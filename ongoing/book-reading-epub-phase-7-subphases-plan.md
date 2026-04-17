# EPUB Phase 7 — Mark a block as read/skimmed/skipped — sub-phases

**Parent:** [`book-reading-epub-support-plan.md`](book-reading-epub-support-plan.md) — *Phase 7: Mark an EPUB block as read, skimmed, or skipped*.

**Discipline:** Each sub-phase below is intended to be a single, ~5-minute commit that is independently push-/deploy-safe. Follow [`.cursor/rules/planning.mdc`](../.cursor/rules/planning.mdc): every sub-phase is **Behavior** or **Structure**; structure phases must be justified by the *immediately following* behavior phase; tests for unfinished behavior land as `@wip`.

## Cohesion principles for this slice

These are non-negotiable for keeping EPUB and PDF code from drifting apart:

1. **Reading-records state is one composable.** Both viewers go through `useNotebookBookReadingRecords` (`syncFromServer`, `submitReadingDisposition`, `dispositionForBlock`). EPUB does not get a parallel records hook.
2. **Layout disposition rendering is one component contract.** `BookReadingBookLayout` owns the read/skimmed/skipped border styling via the `dispositionForBlock` prop. EPUB feeds the same prop instead of growing its own UI.
3. **Reading Control Panel is one component.** `ReadingControlPanel.vue` is reused as-is. EPUB drives it with `:anchor-top-px="null"` (bottom-docked) — it is the same component PDF uses, just without content-aware geometry. Phase 8 later upgrades EPUB to anchored placement; PDF already runs anchored. **Do not** fork the panel into an EPUB variant.
4. **"Mark and advance to next block" is one product rule.** Both viewers, after a successful disposition write, should attempt to advance the selection to the next block in preorder. The pure "next-block" computation is shared; the *act* of applying that selection differs (PDF scrolls bbox; EPUB displays href) and stays inside each viewer.
5. **Auto-mark vs explicit mark unchanged.** Phase 3 of reading-record (auto-mark predecessors with no direct content as `READ`) is **PDF-only** until EPUB Phase 8 lands content-aware geometry. Do not add an EPUB auto-mark heuristic in Phase 7.

## What stays out of Phase 7 (explicitly)

- Content-aware (anchored) panel placement for EPUB — that is **Phase 8**.
- EPUB no-direct-content auto-mark — also **Phase 8**.
- Any change to PDF panel anchoring, snap-back, or auto-mark behavior.
- Any new shared "viewer abstraction" layer — see *Deferred architectural improvements* below.

---

## Sub-phase 7.1 — Land EPUB disposition E2E scenarios as `@wip` (Behavior)

**Type:** Behavior (test-first slice that documents the contract before code lands).

**Why first:** Captures the user-visible contract for a future reader; if the phase is paused after this commit, the codebase still records what is intended. `@wip` keeps CI green (skipped on GitHub Actions) while making the scenarios runnable locally.

**Scope:**
- Add scenarios to `e2e_test/features/book_reading/epub_book.feature` tagged `@wip`:
  - "Mark an EPUB block as read advances the selection" — click `Chapter Alpha`, mark as read in the Reading Control Panel, expect `Chapter Alpha` marked as read in the layout and `Chapter Beta` becomes the selected block.
  - "Mark an EPUB block as skimmed shows skimmed in layout" — click a block, mark as skimmed, expect the skimmed border in the layout.
  - "Mark an EPUB block as skipped shows skipped in layout" — click a block, mark as skipped, expect the skipped border in the layout.
- Reuse existing step definitions where they exist:
  - `When I mark the book block {string} as read in the Reading Control Panel` — reusable.
  - `Then I should see that book block {string} is marked as read in the book layout` — reusable.
- Add the missing step definitions for **skimmed** and **skipped** to `e2e_test/step_definitions/book_reading.ts` and the matching page-object methods on `bookReadingPage.ts`:
  - `markBookBlockAsSkimmedInReadingControlPanel(title)` (clicks `[data-testid="book-reading-mark-as-skimmed"]`).
  - `markBookBlockAsSkippedInReadingControlPanel(title)` (clicks `[data-testid="book-reading-mark-as-skipped"]`).
  - `expectBookBlockMarkedAsSkimmedInBookLayout(title)` / `expectBookBlockMarkedAsSkippedInBookLayout(title)` — assert `data-direct-content-skimmed="true"` / `data-direct-content-skipped="true"`.
- Run the new scenarios locally and confirm they fail because **`book-reading-reading-control-panel`** is not present in the EPUB view yet (this is the *right* failure reason; if it fails for any other reason, fix the scenario first).

**Cohesion:** New step definitions deliberately mirror the existing PDF "mark as read" steps so PDF and EPUB share one Gherkin vocabulary.

**Out of scope:** Production code changes.

**Stop-safe value:** Documents the expected EPUB disposition behavior in feature files even if no further phase ships.

---

## Sub-phase 7.2 — Extract `nextBookBlockAfter` helper (Structure)

**Type:** Structure. Justified by sub-phase 7.3, which needs to share this with PDF.

**Why now:** PDF's `markBlockDisposition` and the soon-to-be-added EPUB equivalent both need "give me the block after this id, or null if none." Extracting the pure computation now avoids duplicating it once and then deduplicating later. Mirrors the existing `predecessorBookBlockIdInPreorder` pattern.

**Scope:**
- Add `frontend/src/lib/book-reading/nextBookBlockAfter.ts`:
  ```ts
  export function nextBookBlockAfter<T extends { id: number }>(
    blocks: readonly T[],
    blockId: number,
  ): T | null
  ```
- Replace the inline `findIndex` + `selIdx + 1` logic in `BookReadingContent.markBlockDisposition` with a call to the helper.
- Add a focused unit test `frontend/tests/lib/book-reading/nextBookBlockAfter.spec.ts` covering: middle block returns successor, last block returns null, missing id returns null, empty array returns null.

**Tests still green:** All existing PDF Vitest and Cypress reading-record scenarios pass unchanged.

**Stop-safe value:** Slightly cleaner shared utility; no observable behavior change.

---

## Sub-phase 7.3 — Wire reading records into the EPUB view and unblock the disposition scenarios (Behavior)

**Type:** Behavior. This is the user-value commit.

**Why now:** 7.1 has the failing scenarios; 7.2 has the shared helper. This sub-phase delivers the actual capability and removes the `@wip` tags.

**Scope (frontend only — no backend changes for Phase 7):**

In `frontend/src/components/book-reading/BookReadingEpubView.vue`:

1. **Records state.** Use `useNotebookBookReadingRecords(notebookId)` (the same composable PDF uses). Call `syncFromServer()` from `onMounted` before user interaction matters, exactly as `BookReadingContent.vue` does.
2. **Layout disposition.** Replace the `noDisposition = () => undefined` placeholder with `bookReading.dispositionForBlock`, passed straight into `BookReadingBookLayout`'s `dispositionForBlock` prop. Delete the `noDisposition` placeholder so no dead code remains.
3. **Panel visibility.** Compute a simple `blockAwaitingConfirmation`:
   - When `selectedBlockId` is non-null **and** `bookReading.hasRecordedDisposition(selectedBlockId)` is `false`, surface the corresponding `BookBlockFull` (a single `computed`).
   - Otherwise `null`.
   - Mount `<ReadingControlPanel>` with `:anchor-top-px="null"` (bottom-docked) inside the EPUB main pane. Pass the block title as `selected-block-title`. Do **not** fork the component or duplicate its template.
4. **Mark and advance.** On `@mark-as-read` / `@mark-as-skimmed` / `@mark-as-skipped`:
   - Call `bookReading.submitReadingDisposition(blockId, status)`.
   - On success, find the next block via `nextBookBlockAfter(book.blocks, blockId)` (from 7.2). If present, set `selectedBlockId` to its id, call `epubViewerRef.value?.displayEpubTarget(next.epubStartHref)` when the href exists, and `currentBlockIdDebouncer.commitNow(next.id)` so the layout never flickers through a stale current-block id.
   - Keep the existing reading-position debouncer flush behavior (selection change already triggers `proposeEpubPositionForBlockId` through the existing `watch(selectedBlockId, …)` — verify that still fires after the advance).
5. **Remove `@wip`** from the three scenarios added in 7.1 once they pass.

**Layout positioning detail:** `ReadingControlPanel` already supports both bottom-docked (when `anchorTopPx === null`) and anchored placement — see `frontend/src/components/book-reading/ReadingControlPanel.vue`. Phase 7 only uses bottom-docked; Phase 8 will switch EPUB to anchored placement using DOM-resolved content boundaries. No changes to `ReadingControlPanel.vue` itself in this phase.

**Tests:**
- All three scenarios from 7.1 must pass with the `@wip` tag removed.
- All existing EPUB scenarios (browse, navigate, scroll-sync, resume) and all PDF reading-record scenarios still pass.
- If a small, focused mounted Vitest is helpful to lock the panel-visibility computed (selected vs. unrecorded), add it under `frontend/tests/components/book-reading/` next to existing EPUB view specs. Skip if Cypress already proves it.

**Cohesion check at end of phase:**
- `dispositionForBlock` prop has exactly one production consumer per format, both routed through the same composable.
- The "mark and advance" rule is implemented in two callers but both use the same `nextBookBlockAfter` and the same `submitReadingDisposition` API. The differing piece is intentionally local to each viewer (PDF scroll vs EPUB `displayEpubTarget`).
- No `noDisposition`-style placeholder remains in EPUB.

**Stop-safe value:** Full Phase 7 user value shipped. Phase 8 can land later without rework.

---

## Sub-phase 7.4 — Plan fold-back and doc refresh (Structure / housekeeping)

**Type:** Structure (no code change). Optional but recommended to keep the plan honest before Phase 8 starts.

**Scope:**
- Mark Phase 7 as done in [`book-reading-epub-support-plan.md`](book-reading-epub-support-plan.md), folding in the actual interim-panel placement decision (bottom-docked).
- Update [`book-reading-user-stories.md`](book-reading-user-stories.md) Gherkin for the EPUB read/skim/skip story so it matches the shipped DOM hooks (`book-reading-mark-as-skimmed` / `-skipped`, `data-direct-content-*`).
- Note in [`doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) that EPUB now goes through the same `useNotebookBookReadingRecords` + `BookReadingBookLayout.dispositionForBlock` seam as PDF.
- Delete this sub-phases doc (or trim it to a one-line "shipped" pointer) since `ongoing/` is for active work.

**Stop-safe value:** Plan documents reflect reality; the next reader is not misled.

---

## Suggested commit order and reason

| # | Type | Commit message hint | Stop-safe outcome |
|---|------|---------------------|-------------------|
| 7.1 | Behavior (`@wip`) | "epub: add @wip scenarios for read/skim/skip dispositions" | Contract is captured even if nothing else ships. |
| 7.2 | Structure | "book-reading: extract nextBookBlockAfter helper" | No behavior change; small cohesion win. |
| 7.3 | Behavior | "epub: mark block as read/skimmed/skipped via Reading Control Panel" | Full Phase 7 user value live. |
| 7.4 | Structure (docs) | "epub: fold Phase 7 into plan, refresh user stories" | Plan and reality match. |

---

## Deferred architectural improvements (intentionally not in Phase 7)

These were considered while writing this plan and **rejected for now** to keep sub-phases small and to avoid speculative abstractions. They are recorded so a future plan can pick them up when there is real demand from a *next* behavior phase:

1. **Shared "book reading shell" composable** — `BookReadingContent.vue` (≈700 lines) and `BookReadingEpubView.vue` (≈180 lines) both juggle window resize, layout breakpoint, `selectedBlockId` defaulting, current-block debouncer wiring, and reading-position persistence. A future phase that adds a third concern to both viewers (e.g. annotations, highlights) should consider extracting `useBookReadingShell`. Premature today: only two consumers, and PDF carries snap-back logic that does not generalize.
2. **Viewer-agnostic "apply selection" interface** — both viewers currently expose imperative methods on `pdfViewerRef` / `epubViewerRef` whose surfaces differ. Once a third format or a richer "navigate to block" need appears, define a small `BookViewerHandle` interface and let `BookReadingPage` orchestrate. Premature today.
3. **Auto-default `selectedBlockId` to the first block on load for EPUB** — PDF does this via a `watch(bookBlocks, …, { immediate: true })` block. Doing the same in EPUB would cause the Reading Control Panel to show immediately on first load, which is a UX call (currently EPUB stays unselected until the user clicks). Decide as part of Phase 8 UX, not Phase 7.
4. **Promote `useReadingDispositionPanelState` composable** — bundling `selectedBlockId` + records + simple `blockAwaitingConfirmation` + mark/advance into one composable would reduce duplication between PDF and EPUB call sites *if and only if* PDF can drop snap-back from its own `blockAwaitingConfirmation`. That is a Phase 8+ conversation (PDF and EPUB sharing content-aware anchoring), not Phase 7 work.

Each deferred item is justified only by a concrete future behavior phase. Pulling any of them into Phase 7 would violate the "structure phases only justify themselves through the next behavior phase" rule.
