# Book reading: panel visibility based on block bottom in viewport

## Problem

When the last book block sits low on the last page, the mark-as-read panel never appears because the scroll-based "current block" logic cannot promote it. The current system uses a complex chain (scroll → `currentBlockId` → snap-back with attempt counters and geometry tracking → `blockAwaitingConfirmation`) that is hard to reason about and still fails for edge layouts.

A contributing cause: many values in `BookReadingContent.vue` are stored in mutable refs and imperatively updated from multiple sites (event handlers, watchers), rather than being computed from a single source of truth. This makes data flow hard to follow and leads to unpredictable behavior.

## New approach

Replace the snap-back mechanism and `currentBlockId`-dependent panel logic with a simpler rule:

**Show the panel when the selected block's bottom is visible in the viewport with room for the panel.**

Core rules:

1. Look at blocks whose bottoms are in the viewport minus panel height.
2. If the **selected** block is among them and **not yet marked** → show the panel anchored at that block's bottom.
3. If the **selected** block is among them but **already marked** → show the panel for the **next** block, if that next block's bottom is also in view with room.
4. **Marking** a block advances `selectedBlockId` to the next block (unchanged).
5. The PDF scroll surface gets **bottom padding** equal to the panel height, so the last block's bottom can always be scrolled into view.

**What this removes:** The snap-back mechanism (`useBookReadingSnapBack` — attempt counters, `geometryEverVisibleForSelection`, `snapToContentBottomAndHold`, `suppressScrollInput`). The panel no longer chases the user; it simply appears when the block bottom is visible.

**What stays unchanged:** `currentBlockId` (scroll-based) still drives the `CurrentBlockNavigationBar`, reading position persistence, and auto-marking of content-less blocks. It is decoupled from panel visibility.

## Caching variable cleanup

Replace imperatively-set caching refs with computed values or debouncer-owned readonly refs, one group per phase. Each refactoring phase is verified by existing tests staying green (no behavior change).

**Variables to address (after snap-back removal):**

| Variable | Problem | Fix |
|----------|---------|-----|
| `currentBlockId` | External ref written by debouncer commit callback | Debouncer owns the ref internally, exposes `Readonly<Ref>` |
| `pdfBarCurrentPage` / `pdfBarPagesTotal` | Imperatively set in event handler | Store viewport payload in one ref; compute from it |
| `lastReadingForPatch` | Cache of viewport reading position | Derive from stored viewport payload |
| `currentBlockLiveText` + `lastAnnouncedCurrentBlockTitle` | Two refs + watcher to track announcement | Single computed from `currentBlockId` + `bookBlocks` |
| `readingPanelAnchorTopPx` | Set from two sites (event handler + watcher) | Consolidate to single update site (DOM measurement — cannot be pure computed) |

---

## Phases

### Phase 1 — Bottom padding on PDF scroll container

**Refactoring (no behavior change)**

Add `bottomPaddingPx` prop (default 0) to `PdfBookViewer.vue`; apply as `padding-bottom` on `.pdf-book-viewer-container` via a dynamic style binding. In `BookReadingContent.vue`, pass `:bottom-padding-px="READING_PANEL_OBSTRUCTION_PX"` (80). This ensures the last block's bottom can always be scrolled into the visible area.

**Verified by:** All existing tests pass.

---

### Phase 2 — Fix panel visibility for the last block

**Behavior (bug fix)**

- **Pre-condition:** The selected block is the last block, has direct content, and has not been marked.
- **Trigger:** User scrolls until the last block's content bottom is visible with room for the panel.
- **Post-condition:** The mark-as-read panel appears.

**Root cause:** `selectedIndexAndSuccessor` returns null when `selIdx >= rows.length - 1`, so `blockAwaitingConfirmation` is always null for the last block.

**What changes:**

- **`useBookReadingSnapBack.ts`:** In `blockAwaitingConfirmation`, after `selectedIndexAndSuccessor` returns null, check if the selected block exists, has direct content, and its bottom is visible. If so, return it. This adds one new code path without changing existing non-last-block behavior.
- **Unit test update:** The existing test "hides the panel when the selected block has no successor" (which asserts the panel is always hidden for the last block) needs updating — mock `isLastContentBottomVisible` to false so the panel correctly stays hidden; add a companion test with geometry true that asserts the panel shows.

**Tests**

- **E2E:** Scroll to the last block "6. Why Refactoring Matters More with AI" → panel appears → mark as read.
- **Unit:** Update "no successor" test; add "last block panel shows when geometry visible" test.

**Deploy gate:** Commit after green E2E + unit tests for this phase.

---

### Phase 3 — Store viewport payload; compute derived values

**Refactoring (no behavior change)**

Currently `onViewportAnchorPage` imperatively sets `pdfBarCurrentPage`, `pdfBarPagesTotal`, and `lastReadingForPatch` from the event payload. These should be computed from a single stored viewport payload ref.

**What changes:**

- **`BookReadingContent.vue`:** Add one reactive ref for the latest viewport event payload. Replace `pdfBarCurrentPage`, `pdfBarPagesTotal`, `lastReadingForPatch` with computed values derived from it.
- **Removes:** 3 caching refs.

**Verified by:** All existing tests pass.

---

### Phase 4 — `currentBlockLiveText` → single computed

**Refactoring (no behavior change)**

Currently two refs (`currentBlockLiveText`, `lastAnnouncedCurrentBlockTitle`) are updated by a `watch(currentBlockId)` to track what was last announced. Replace with a single computed.

**What changes:**

- **`BookReadingContent.vue`:** Replace the two refs and the watcher with one computed.
- **Removes:** 2 caching refs and 1 watcher.

**Verified by:** All existing tests pass.

---

### Phase 5 — Simplify panel visibility to geometry-only rule

**Behavior**

Change `blockAwaitingConfirmation` from the current multi-path logic (geometry + successor-as-current + geometryEverVisible) to the simple rule: selected block is not marked AND has direct content AND bottom is currently visible. This removes the `geometryEverVisibleForSelection` "sticky" behavior and the successor-based fallback for blocks without direct content.

**What changes:**

- **`useBookReadingSnapBack.ts`:** Replace `blockAwaitingConfirmation` computed body with the simple rule. Remove `geometryEverVisibleForSelection` ref and its updates.
- **Unit test updates:** Tests asserting old behavior (e.g. "keeps panel visible after geometry becomes false while successor is not yet current", "shows the panel when the selected block's successor is the viewport current block") need updating to reflect the new geometry-only rule.

**Deploy gate:** Commit after green tests.

---

### Phase 6 — Remove snap-back mechanism + dead code cleanup

**Refactoring**

With panel visibility now geometry-only (Phase 5), the snap-back mechanism is dead code. Remove it and all its dependents.

**What changes:**

- **`useBookReadingSnapBack.ts`:** Remove `shouldSnapBack`, `performSnapBack`, `snapbackAttempts`, `snapAnimationKey`, `clearSnapbackAttemptsForBlock`. Remove `currentBlockId` and `snapHoldMs` from options.
- **`BookReadingContent.vue`:** Simplify debouncer `commit` (always commit, no snap-back gate). Remove `clearSnapbackAttemptsForBlock` call from `markSelectedDisposition`. Remove `:snap-animation-key` from `ReadingControlPanel`.
- **`PdfBookViewer.vue`:** Remove `snapToContentBottomAndHold`, `suppressScrollInput`, `contentFitsFromBlockTop` from expose and implementation. Remove `createIntervalScrollSuppression` import and `scrollSuppression` instance. Remove `checkEvent()` calls from wheel/touch handlers.
- **`ReadingControlPanel.vue`:** Remove `snapAnimationKey` prop, `isAnimating` ref, watcher, `data-snap-animating` attribute, and `snap-attention` CSS.
- **`BookReadingPdfViewerRef` type:** Remove `snapToContentBottomAndHold`, `suppressScrollInput`, `contentFitsFromBlockTop`.
- **Delete:** `frontend/src/lib/book-reading/intervalScrollSuppression.ts`, `frontend/tests/lib/book-reading/intervalScrollSuppression.spec.ts`.
- **Unit tests:** Remove all snap-back–specific tests (snap budgets, snap animation, cross-page snap, same-page-too-tall snap, snap state reset, etc.).

**Verified by:** All remaining tests pass.

---

### Phase 7 — `currentBlockId` → debouncer-owned readonly ref

**Refactoring (no behavior change)**

With snap-back removed (Phase 6), the debouncer `commit` callback is unconditional. The debouncer should own `currentBlockId` internally and expose it as `Readonly<Ref<number | null>>`.

**What changes:**

- **`createCurrentBlockIdDebouncer`:** Returns an additional `currentBlockId: Readonly<Ref<number | null>>`. The `commit` option is removed; the debouncer writes to its own ref.
- **`BookReadingContent.vue`:** Reads `currentBlockId` from the debouncer return value instead of declaring its own ref.

**Verified by:** All existing tests pass.

---

### Phase 8 — Auto-target next block when selected is already marked

**Behavior**

- **Pre-condition:** The selected block is already marked. The next block exists and its content bottom is visible with room for the panel.
- **Trigger:** User scrolls to a position where the next block's bottom is in view.
- **Post-condition:** The panel appears for the next block.

**What changes:**

- Extend panel-target logic: when selected block is marked, check next block.
- Marking via this panel still advances `selectedBlockId`.

**Tests**

- **E2E:** Mark a block → if the next block's bottom is visible, panel immediately shows for it.

**Deploy gate:** Commit after green E2E for this phase.

---

## Remaining after all phases

`readingPanelAnchorTopPx` requires DOM measurements from `PdfBookViewer` and cannot be a pure computed. After Phase 5 it will have a single update site (the viewport event handler), which is acceptable. `windowWidth` mirrors a DOM measurement via resize listener — standard pattern.

## Order

- **Phase 1** — bottom padding (structural prep).
- **Phase 2** — last-block bug fix (user value: last block reachable).
- **Phases 3–4** — caching variable cleanup (structural improvement, verified by existing tests).
- **Phase 5** — simplify panel to geometry-only (behavior change, breaks old tests).
- **Phase 6** — remove snap-back + dead code (structural cleanup, removes old tests).
- **Phase 7** — debouncer owns currentBlockId (structural improvement).
- **Phase 8** — auto-advance panel to next visible block (new behavior).

## Out of scope

- Changes to `currentBlockIdFromVisiblePage` logic — stays as-is for the navigation bar and reading position.
- Tap-to-set-current — no longer needed since the panel follows selected-block visibility.
- "Mark remaining as read" / bulk operations.

## Status

| Phase | Status  |
|-------|---------|
| 1     | Done    |
| 2     | Done    |
| 3     | Done    |
| 4     | Done    |
| 5     | Planned |
| 6     | Planned |
| 7     | Planned |
| 8     | Planned |
