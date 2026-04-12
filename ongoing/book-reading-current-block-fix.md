# Book reading: last-block panel fix + caching variable cleanup

## Problem

When the last book block sits low on the last page, the mark-as-read panel never appears because `selectedIndexAndSuccessor` returns null for the last block, so `blockAwaitingConfirmation` is always null. The panel cannot show for the last block regardless of scroll position.

A contributing cause: many values in `BookReadingContent.vue` are stored in mutable refs and imperatively updated from multiple sites (event handlers, watchers), rather than being computed from a single source of truth. This makes data flow hard to follow.

## Approach

Fix the last-block bug by adding a geometry-based code path for the last block in `blockAwaitingConfirmation`. Add bottom padding so the last block's bottom can always be scrolled into view. Clean up caching refs to improve readability.

**The snap-back mechanism is preserved as-is.** It continues to gate `currentBlockId` commits and snap the scroll position when the user scrolls past an unconfirmed block.

**`currentBlockId`** (scroll-based) still drives the `CurrentBlockNavigationBar`, reading position persistence, auto-marking of content-less blocks, and snap-back decisions.

## Caching variable cleanup

Replace imperatively-set caching refs with computed values or debouncer-owned readonly refs, one group per phase. Each refactoring phase is verified by existing tests staying green (no behavior change).

**Variables to address:**

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

### Phase 5 — `currentBlockId` → debouncer-owned readonly ref

**Refactoring (no behavior change)**

Currently `BookReadingContent.vue` declares `currentBlockId` as a mutable ref and passes it to both the debouncer (which writes via `commit`) and `useBookReadingSnapBack` (which reads it for snap-back decisions). The debouncer should own the ref internally and expose it as `Readonly<Ref<number | null>>`.

**What changes:**

- **`createCurrentBlockIdDebouncer`:** Creates `currentBlockId` internally. Returns it as `Readonly<Ref<number | null>>`. The `commit` callback still gates on snap-back (returns false to reject); the debouncer only writes when `commit` returns true.
- **`BookReadingContent.vue`:** Reads `currentBlockId` from the debouncer return value instead of declaring its own ref.

**Verified by:** All existing tests pass.

---

### Phase 6 — Auto-target next block when selected is already marked

**Behavior**

- **Pre-condition:** The selected block is already marked. The next block exists and its content bottom is visible with room for the panel.
- **Trigger:** User scrolls to a position where the next block's bottom is in view.
- **Post-condition:** The panel appears for the next block.

**What changes:**

- Extend panel-target logic in `blockAwaitingConfirmation`: when the selected block is already marked, check the next block.
- Marking via this panel still advances `selectedBlockId`.

**Tests**

- **E2E:** Mark a block → if the next block's bottom is visible, panel immediately shows for it.

**Deploy gate:** Commit after green E2E for this phase.

---

## Remaining after all phases

`readingPanelAnchorTopPx` requires DOM measurements from `PdfBookViewer` and cannot be a pure computed; it is still set from two sites (event handler + watcher). Consolidating to a single update site is a possible future cleanup. `windowWidth` mirrors a DOM measurement via resize listener — standard pattern.

## Order

- **Phase 1** — bottom padding (structural prep).
- **Phase 2** — last-block bug fix (user value: last block reachable).
- **Phases 3–4** — caching variable cleanup (structural improvement, verified by existing tests).
- **Phase 5** — debouncer owns currentBlockId (structural improvement).
- **Phase 6** — auto-advance panel to next visible block (new behavior).

## Out of scope

- Changes to `currentBlockIdFromVisiblePage` logic — stays as-is for the navigation bar and reading position.
- Snap-back mechanism changes — preserved as-is.
- "Mark remaining as read" / bulk operations.

## Status

| Phase | Status  |
|-------|---------|
| 1     | Done    |
| 2     | Done    |
| 3     | Done    |
| 4     | Done    |
| 5     | Done    |
| 6     | Done    |
