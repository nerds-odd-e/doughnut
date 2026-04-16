# Phase 5 sub-phases: Scroll the EPUB and see where I am in the layout

**Parent phase:** Phase 5 of `book-reading-epub-support-plan.md`

**Phase 5 goal:** When the user scrolls through EPUB content, the layout sidebar highlights the current `BookBlock` — distinct from the selected block when applicable.

---

## Current state (after Phases 1–4)

- `EpubBookViewer.vue` renders with epub.js (`flow: "scrolled"`, `manager: "continuous"`). No events are emitted — no `relocated` listener, no scroll tracking.
- `BookReadingEpubView.vue` passes `:current-block-id="null"` and `:selected-block-id="null"` to `BookReadingBookLayout`. The layout never highlights any row for EPUB.
- `BookReadingBookLayout.vue` already has the `data-current-block="true"` + `aria-current="location"` styling and a watcher that scrolls the aside to the current block row. These work for PDF. They will work for EPUB once a non-null `currentBlockId` is provided.
- The PDF pipeline uses `currentBlockIdFromVisiblePage` (page + bbox geometry) which is entirely PDF-specific. EPUB needs its own mapping: epub.js spine `href` → owning `BookBlock.id`.
- Each `BookBlock` has `epubStartHref` (e.g. `OEBPS/chapter3.xhtml#section-beta-two`). The `href` portion (before `#`) identifies the spine document; the fragment identifies the sub-section.
- `createCurrentBlockIdDebouncer` (120 ms, generic) and `currentBlockLiveAnnouncement.ts` are shared infrastructure with no PDF dependency — reusable.
- E2E page object `bookReadingPage.ts` already has `expectBookBlockIsCurrentBlockByTitle` checking `data-current-block="true"`.

## Approach

1. Listen to epub.js `relocated` events in `EpubBookViewer.vue` and emit a location payload.
2. In `BookReadingEpubView.vue`, map the epub.js location (`href` + optional `atEnd`) to the owning `BookBlock.id` using the blocks' `epubStartHref` data.
3. Wire `currentBlockId` (via debouncer) and `selectedBlockId` into `BookReadingBookLayout`.
4. Add E2E coverage showing the layout highlights the current block on scroll and keeps it distinct from the selected block.

---

## Full E2E scenario (target for this phase)

```gherkin
Scenario: Scrolling the EPUB updates the current block in the layout
  Given I am logged in as an existing user
  And I have a notebook with the head note "EPUB Scroll Sync Notebook"
  When I open the notebook settings for "EPUB Scroll Sync Notebook"
  And I attach the EPUB file "book_reading/epub_valid_minimal.epub"
  When I open the reading view for the attached book "epub_valid_minimal"
  Then the book block "Part One" should be the current block in the book reader
  When I click "Section Beta-Two" in the book layout
  Then the book block "Section Beta-Two" should be the current block in the book reader
```

The key assertion: opening the EPUB sets the initial current block, and navigating to a different section updates it.

---

## Sub-phase 5.1 — Structure: EpubBookViewer emits `relocated` location

**Type:** Structure (no observable behavior change)

**What:** Add an epub.js `relocated` listener in `EpubBookViewer.vue` that emits a `relocated` event with the spine href from the current location. This is internal plumbing — `BookReadingEpubView.vue` does not yet consume it, so no layout or UI change.

**Work:**
- In `EpubBookViewer.vue`, after `rendition.display()`:
  - Register `rendition.on("relocated", callback)`.
  - The callback extracts `location.start.href` (the spine XHTML path from epub.js's relocation payload).
  - `defineEmits` with a `relocated` event carrying `{ href: string }`.
  - Emit on each relocation.
- Clean up the listener in `destroyEpub`.

**Done when:** `EpubBookViewer` emits `relocated` events. No test changes — existing tests still pass unchanged (no observable difference).

---

## Sub-phase 5.2 — Structure: Add `currentBlockIdFromEpubLocation` mapping function

**Type:** Structure (pure function, unit-tested, not yet wired)

**What:** Create a pure function `currentBlockIdFromEpubLocation(blocks, href)` that maps an epub.js spine href to the owning `BookBlock.id`. This is the EPUB equivalent of `currentBlockIdFromVisiblePage`.

**Logic:**
- Walk blocks in reverse preorder. For each block with a non-null `epubStartHref`, extract the `href` portion (before `#`) and the `fragment` portion (after `#`).
- Match the block whose `href` matches the relocated `href` and whose fragment anchor appears at or before the current location. Specifically: the last block (in preorder) whose href-without-fragment matches and whose fragment is empty or present in the current href.
- Simpler initial rule: last block in preorder whose `epubStartHref` path portion (before `#`) matches the relocated href, preferring a block with a matching fragment if available.

**Work:**
- Create `frontend/src/lib/book-reading/currentBlockIdFromEpubLocation.ts`.
- Unit test in `frontend/tests/lib/book-reading/currentBlockIdFromEpubLocation.spec.ts` with cases:
  - Exact href match → returns that block's id.
  - href matches multiple blocks (same spine doc, different fragments) → returns the last matching block in preorder (deepest section).
  - No match → returns null.
  - Blocks without `epubStartHref` are skipped.

**Done when:** Unit tests pass. No integration or E2E change.

---

## Sub-phase 5.3 — Behavior: Wire current block tracking into EPUB reader layout

**Type:** Behavior

**What:** Connect the `relocated` event from `EpubBookViewer` through the debouncer and mapping function to `BookReadingBookLayout`'s `:current-block-id`. The layout will highlight the current block as the user navigates (via click or initial load).

**Work:**
- In `BookReadingEpubView.vue`:
  - Import `createCurrentBlockIdDebouncer` and `currentBlockIdFromEpubLocation`.
  - Create a debouncer (reuse the same 120 ms delay as PDF).
  - Handle `@relocated` from `EpubBookViewer`: call the mapping function with the blocks and the href, then `propose` the result to the debouncer.
  - Pass `currentBlockId` from the debouncer to `BookReadingBookLayout` `:current-block-id` (replacing `null`).
- Also wire `:selected-block-id` — set it when the user clicks a block in the layout (the `onBookBlockClick` handler already exists; track the clicked block's id in a ref and pass it to the layout).
- Add `@wip` E2E scenario to `epub_book.feature`:

```gherkin
@wip
Scenario: Scrolling the EPUB updates the current block in the layout
  Given I am logged in as an existing user
  And I have a notebook with the head note "EPUB Scroll Sync Notebook"
  When I open the notebook settings for "EPUB Scroll Sync Notebook"
  And I attach the EPUB file "book_reading/epub_valid_minimal.epub"
  When I open the reading view for the attached book "epub_valid_minimal"
  Then the book block "Part One" should be the current block in the book reader
  When I click "Section Beta-Two" in the book layout
  Then the book block "Section Beta-Two" should be the current block in the book reader
```

- Update `bookReadingPage.ts`: the existing `expectBookBlockIsCurrentBlockByTitle` should work as-is (it checks `data-current-block="true"`). The EPUB `clickBookLayoutBlockByTitle` method may need updating — currently it does not assert `data-current-block` after click. Update it or use the same `clickBookBlockByTitle` if applicable (check if page indicator guard is needed).
- Run E2E. Make the scenario pass. Remove `@wip`.

**Done when:** E2E green — opening the EPUB highlights the initial current block; clicking a section updates the current block. `@wip` removed.

---

## Sub-phase 5.4 — Behavior: Selected block stays distinct from current block on navigation

**Type:** Behavior

**What:** When the user clicks a block and then navigates elsewhere (e.g. clicks another block), the selected block and current block can differ. This sub-phase verifies and extends the E2E to cover the distinction — matching the PDF scenario pattern from `book_browsing.feature`.

**Work:**
- Extend the E2E scenario (or add a focused one) to assert the selected-vs-current distinction:

```gherkin
Scenario: EPUB current block updates on navigation; selection stays on explicit choice
  Given I am logged in as an existing user
  And I have a notebook with the head note "EPUB Current vs Selected Notebook"
  When I open the notebook settings for "EPUB Current vs Selected Notebook"
  And I attach the EPUB file "book_reading/epub_valid_minimal.epub"
  When I open the reading view for the attached book "epub_valid_minimal"
  When I click "Chapter Alpha" in the book layout
  Then the book block "Chapter Alpha" should be the current selection in the book reader
  And the book block "Chapter Alpha" should be the current block in the book reader
  When I click "Section Beta-Two" in the book layout
  Then the book block "Section Beta-Two" should be the current selection in the book reader
  And the book block "Section Beta-Two" should be the current block in the book reader
```

- Clean up the `bookReadingPage.ts` comment "EPUB layout has no page indicator and no current-block wiring yet; click only." — this is no longer accurate after this phase.
- Verify the live region announcement works for EPUB (the `BookReadingBookLayout` watcher should trigger it if wired, or note it as deferred to a later phase if it depends on `BookReadingContent`-specific code).
- Run all EPUB E2E scenarios. All pass.

**Done when:** E2E green — selected and current block are correctly tracked and distinguished. Outdated comments removed.

---

## Key design notes

- **epub.js `relocated` event** fires on display, scroll, and resize. It provides `location.start.href` (spine path like `OEBPS/chapter3.xhtml`) — this is the primary input for block mapping.
- **No backend change.** All data needed (`epubStartHref` on each block) is already served from Phase 4.
- **Reuse shared infrastructure:** `createCurrentBlockIdDebouncer`, `BookReadingBookLayout` current-block styling, aside scroll-into-view watcher, `expectBookBlockIsCurrentBlockByTitle` E2E assertion.
- **New EPUB-specific code:** `currentBlockIdFromEpubLocation` (mapping function) and the `relocated` listener + emit in `EpubBookViewer`.
- **PDF path unchanged.** All changes are in EPUB-specific files or format-branched in the EPUB shell.
- **Scroll-based E2E testing:** epub.js `relocated` fires on `display()` calls (navigation), which is more reliable for E2E than trying to trigger and assert on raw scroll events. The initial load and click-navigation are sufficient to prove the current-block pipeline works. Scroll-triggered relocation is covered by the same event path.
