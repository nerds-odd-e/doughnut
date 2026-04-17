# Phase 6 sub-phases: Leave and return to the same EPUB position

**Parent phase:** Phase 6 of `book-reading-epub-support-plan.md`

**Phase 6 goal:** When the user opens an EPUB, navigates to a section, leaves the reading page, and later reopens the same book, the reader returns to the saved EPUB position.

Each sub-phase below is designed to be a single closed, commit-ready slice that can be completed in roughly 5 minutes, with all tests green at the end of the sub-phase. Structure phases must not change observable behavior (existing PDF and EPUB tests keep passing). Behavior phases add user-visible value and are paired with the minimum E2E proof.

---

## Current state (after Phases 1–5)

- **Backend — entity:** `BookUserLastReadPosition` has `page_index` (int, NOT NULL), `normalized_y` (int, NOT NULL), `selected_book_block_id` (nullable FK). Entity fields are Java `int` — PDF-only shape.
- **Backend — DTO:** `BookLastReadPositionRequest` requires `pageIndex` and `normalizedY` (`@NotNull`) plus optional `selectedBookBlockId`. No EPUB locator field yet.
- **Backend — service:** `BookService.upsertLastReadPosition` writes `pageIndex` and `normalizedY` unconditionally.
- **Backend — API:** `GET/PATCH /api/notebooks/{notebook}/book/reading-position` exists and is exercised by PDF tests in `NotebookBooksControllerTest`.
- **Frontend — page load:** `BookReadingPage.vue` fetches `getNotebookBookReadingPosition` **only for PDF** (early-returns before the fetch when `data.format === "epub"`). The EPUB view receives nothing about saved position.
- **Frontend — EPUB viewer:** `EpubBookViewer.vue` always calls `r.display()` with no argument on first open. It emits `relocated` events carrying `{ href }`.
- **Frontend — EPUB shell:** `BookReadingEpubView.vue` consumes `relocated` for current-block tracking (Phase 5) but does **not** PATCH reading-position.
- **Frontend — debouncer:** `createLastReadPositionPatchDebouncer` is hard-coded to the PDF `{ pageIndex, normalizedY, selectedBookBlockId? }` body shape.
- **PDF path:** `BookReadingContent.vue` PATCHes on every `viewportAnchorPage` via the debouncer, and `PdfBookViewer.scrollToStoredReadingPosition` restores on `pagesReady`. No change required to this path.

---

## Approach

1. Widen the **persistence schema and API** to carry an optional EPUB locator (`epubLocator`) alongside the existing PDF fields.
2. Widen the **frontend write path** (debouncer) and **read path** (page fetch + props) to carry an `epubLocator`.
3. **Wire** save-on-relocate and restore-on-reopen inside the EPUB view, proved end-to-end by a single round-trip E2E scenario.

The **locator format for v1** is the epub.js spine href (same shape as `epubStartHref`, e.g. `OEBPS/chapter2.xhtml#section-beta-two`). This is coarser than a Readium CFI but is enough to resume at the last visited section and keeps the schema aligned with what Phase 4 already exposes. A later enhancement can extend the string to a full CFI without another schema change.

---

## Full E2E scenario (target for this phase)

```gherkin
Scenario: Resume EPUB at the last read position after leaving
  Given I am logged in as an existing user
  And I have a notebook with the head note "EPUB Resume Notebook"
  When I open the notebook settings for "EPUB Resume Notebook"
  And I attach the EPUB file "book_reading/epub_valid_minimal.epub"
  When I open the reading view for the attached book "epub_valid_minimal"
  And I click "Section Beta-Two" in the book layout
  Then I should see the text "Unique content in section beta-two." in the EPUB reader
  When I leave the EPUB reading view and return to it
  Then I should see the text "Unique content in section beta-two." in the EPUB reader
  And the book block "Section Beta-Two" should be the current block in the book reader
```

The key assertions: after the reader persists the position, leaving and returning lands the user on the same section, and the layout current-block wiring from Phase 5 re-syncs accordingly.

---

## Sub-phase 6.1 — Structure: Persist `epub_locator` on the reading-position row (done)

**Type:** Structure (no observable behavior change; PDF round-trip unchanged)

**What:** Add backend storage for an EPUB locator string on `book_user_last_read_position` and expose it on the entity's JSON response, without changing the API request shape yet.

**Work:**
- New Flyway migration `V300000XXX__book_user_last_read_epub_locator.sql`:
  - Add `epub_locator VARCHAR(512) NULL` column.
  - Relax `page_index` and `normalized_y` from `NOT NULL` to nullable (EPUB rows will not carry them).
- `BookUserLastReadPosition` entity:
  - Change `pageIndex` and `normalizedY` fields from `int` to `Integer`.
  - Add `String epubLocator` field mapped to `epub_locator`.
  - Expose `epubLocator` in the JSON response (nullable, same pattern as `selectedBookBlockId`).
- `BookService.upsertLastReadPosition` continues to write `pageIndex` / `normalizedY` for PDF and never touches `epubLocator`.
- Existing PDF controller tests (`NotebookBooksControllerTest` → `PatchReadingPosition` / `GetReadingPosition` nests) stay unchanged and still pass.

**Done when:** `pnpm backend:verify` (or equivalent) green. No frontend change. PDF GET response still has `pageIndex`/`normalizedY`/`selectedBookBlockId`; new `epubLocator` field appears as `null` for PDF rows. No E2E change.

---

## Sub-phase 6.2 — Structure: Accept and return `epubLocator` on the reading-position API (done)

**Type:** Structure (existing PDF clients keep sending the same shape; new optional field for EPUB)

**What:** Extend `BookLastReadPositionRequest` and `BookService.upsertLastReadPosition` to accept an EPUB locator payload. The frontend is not wired yet.

**Work:**
- `BookLastReadPositionRequest`:
  - Remove `@NotNull` from `pageIndex` and `normalizedY` (become optional `Integer`).
  - Add optional `String epubLocator` with schema description.
- `BookService.upsertLastReadPosition`:
  - Branch on request shape:
    - If `epubLocator` is present: persist it and leave `pageIndex`/`normalizedY` as `null` (or clear them on update).
    - Else if `pageIndex`+`normalizedY` are both present: persist them (existing behavior) and leave `epubLocator` unchanged if unspecified.
    - Else: reject with `BINDING_ERROR` ("reading-position payload requires either pageIndex+normalizedY or epubLocator").
  - `selectedBookBlockId` handling stays as-is for both branches.
- New controller tests in `NotebookBooksControllerTest`:
  - PATCH with `{ epubLocator: "OEBPS/chapter2.xhtml#section-beta-two" }` → GET returns that locator and null `pageIndex`/`normalizedY`.
  - PATCH with neither PDF fields nor `epubLocator` → `400 BINDING_ERROR`.
  - Existing PDF round-trip test (PATCH pageIndex+normalizedY → GET returns them) still passes unchanged.
- Regenerate the frontend TS client: `pnpm generateTypeScript`. Commit the regenerated `packages/generated/doughnut-backend-api/**` changes.

**Done when:** backend tests green including the new EPUB round-trip; `types.gen.ts` reflects the optional `epubLocator`. Still no UI change and no E2E scenario change.

---

## Sub-phase 6.3 — Structure: Widen frontend reading-position plumbing for EPUB (done)

**Type:** Structure (new props/arguments wired through but unused by the EPUB viewer UI)

**What:** Make the frontend able to carry an EPUB locator in the debouncer body and down to `EpubBookViewer` via props, without changing observable behavior yet.

**Work:**
- `frontend/src/lib/book-reading/debounceLastReadPositionPatch.ts`:
  - Widen `LastReadPositionPatchBody` to a discriminated shape supporting either `{ pageIndex, normalizedY, selectedBookBlockId? }` or `{ epubLocator, selectedBookBlockId? }`.
  - Expose a second `proposeEpubLocator(epubLocator: string, selectedBookBlockId?: number)` entry point (or a single widened `propose` — whichever keeps the existing PDF call site in `BookReadingContent.vue` untouched).
  - Keep `same()` equivalence stable: two bodies are equal only if they're the same variant with equal fields.
- Unit tests in `frontend/tests/lib/book-reading/` covering the new EPUB branch (dedupe, debounce delay, error swallow).
- `BookReadingPage.vue`:
  - Remove the `data.format === "epub"` early return so reading-position is fetched for EPUB too.
  - Add local state `initialEpubLocator: string | null` populated from `posResult.data.epubLocator` when the book format is EPUB.
  - Pass `:initial-epub-locator="initialEpubLocator"` to `BookReadingEpubView`.
- `BookReadingEpubView.vue`:
  - Accept optional `initialEpubLocator: string | null` prop (default `null`). **Do not use it yet.**
  - Pass it through as `:initial-locator="initialEpubLocator"` to `EpubBookViewer`.
- `EpubBookViewer.vue`:
  - Accept optional `initialLocator: string | null` prop (default `null`). **Do not use it in `rendition.display` yet** — the viewer still calls `r.display()` with no argument.

**Done when:** existing frontend unit tests plus the new debouncer tests pass. No visible UI change (confirmed by existing EPUB E2E scenarios still passing). No E2E changes.

---

## Sub-phase 6.4 — Behavior: Save and resume the EPUB reading position

**Type:** Behavior

**What:** Wire the write path (save on `relocated`) and the restore path (display `initialLocator` on open) inside the EPUB viewer, delivering the full leave-and-return user value.

**Work:**
- `EpubBookViewer.vue`:
  - On first `openEpub()`, if `props.initialLocator` is a non-empty string, pass it to `r.display(initialLocator)` instead of the default no-arg call. Keep a safe fallback to the default when the call rejects.
- `BookReadingEpubView.vue`:
  - Import `createLastReadPositionPatchDebouncer` and construct it with the same 400 ms delay constant used by `BookReadingContent.vue`, wired to `NotebookBooksController.patchNotebookBookReadingPosition`.
  - In `onEpubRelocated(payload)`, propose `{ epubLocator: payload.href, selectedBookBlockId: selectedBlockId.value ?? undefined }` to the debouncer.
  - Also propose when `selectedBlockId` changes (mirrors the PDF view's `watch(selectedBlockId)` so a user click persists without waiting for the next relocation).
  - Cancel the debouncer in `onBeforeUnmount`.
- Add the E2E scenario above to `e2e_test/features/book_reading/epub_book.feature` tagged `@wip` first.
- Add any missing step definitions (e.g. `When I leave the EPUB reading view and return to it`) — simplest implementation: navigate to the notebook edit page and back into the reading view through the existing page-object helpers.
- Run the scenario locally with `pnpm cypress run --spec e2e_test/features/book_reading/epub_book.feature`. Once it passes, remove the `@wip` tag.
- Confirm the other EPUB scenarios (attach, read content, navigate, scroll sync, precise nav, DRM error) still pass. Confirm the PDF reading-position PATCH/GET still works (backend tests remain green).

**Done when:** the round-trip E2E scenario passes without `@wip`, the rest of `epub_book.feature` still passes, no PDF or backend regressions.

---

## Key design notes

- **Locator format:** v1 stores and replays the epub.js spine href (including any `#fragment`), i.e. the same string format already produced by `BookBlock.getEpubStartHref`. No CFI yet. The schema uses a plain `VARCHAR(512)` so a later CFI upgrade does not need another migration.
- **PDF path untouched:** PDF writes, PDF restore, PDF controller tests, and the debouncer PDF entry point all keep the existing shape. The only PDF-visible change is the entity's JSON response gaining a nullable `epubLocator` field (always `null` for PDF rows).
- **Stop-safe sequencing:**
  - After 6.1 the DB knows how to store EPUB positions. No user value yet.
  - After 6.2 the API knows how to accept/return them. Still no UI.
  - After 6.3 the frontend props and debouncer can carry them. Still no UI.
  - After 6.4 the end-to-end resume behavior is live and tested. Full user value delivered.
- **No speculative prep:** 6.1–6.3 each prepare exactly what 6.4 needs. Nothing extra is added that only a later phase would use.
- **Reuse over new abstractions:** The debouncer is widened rather than duplicated; the page-level fetch is unified rather than branched off. EPUB-specific logic stays inside the EPUB view and viewer.
