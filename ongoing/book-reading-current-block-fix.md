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

### Phase 1 — Panel shows when selected block's bottom is visible + bottom padding

**Behavior**

- **Pre-condition:** Book with blocks; the selected block has not been marked.
- **Trigger:** User scrolls until the selected block's content bottom is in the viewport with at least panel-height room below it.
- **Post-condition:** The mark-as-read panel appears anchored at the selected block's content bottom. Scrolling the block bottom out of view hides the panel. For the last block, bottom padding on the scroll surface ensures it can always be scrolled far enough.

**What changes:**

- **`useBookReadingSnapBack` (or replacement):** Simplify `blockAwaitingConfirmation` to: selected block is not yet marked AND its content bottom is visible in the viewport with room for the panel. Remove snap-back logic (attempt counters, `geometryEverVisibleForSelection`, `performSnapBack`, `shouldSnapBack`, `snapToContentBottomAndHold`, `suppressScrollInput`).
- **`PdfBookViewer.vue`:** Add bottom padding to the scroll container equal to the panel height, so the last block's bottom can be scrolled into view.
- **`BookReadingContent.vue`:** Remove snap-back wiring (`snapAnimationKey`, `shouldSnapBack`, `performSnapBack`, `clearSnapbackAttemptsForBlock`). The `updateReadingPanelAnchor` logic can be simplified since the panel visibility and anchor are now one concern.

**Caching variables removed:** `lastContentBottomVisible`, `geometryEverVisibleForSelection`, `snapbackAttempts`, `snapAnimationKey` (all part of snap-back).

**Tests**

- **E2E:** Book where the last block's content is near the bottom of the last page. Scroll to bottom → panel appears for the last block. (This is the scenario that fails today.)

**Deploy gate:** Commit after green E2E for this phase.

---

### Phase 2 — `currentBlockId` → debouncer-owned readonly ref

**Refactoring (no behavior change)**

Currently `currentBlockId` is a `ref<number | null>` in `BookReadingContent.vue`, imperatively set by the debouncer's `commit` callback. The debouncer should own the ref internally and expose it as `Readonly<Ref<number | null>>`.

**What changes:**

- **`createCurrentBlockIdDebouncer`:** Returns an additional `currentBlockId: Readonly<Ref<number | null>>` that it owns. The `commit` option is removed; the debouncer writes to its own ref. Snap-back intercept logic (already removed in Phase 1) is gone, so the commit is unconditional.
- **`BookReadingContent.vue`:** Reads `currentBlockId` from the debouncer return value instead of declaring its own ref. No external writes to `currentBlockId`.

**Verified by:** All existing tests pass (no behavior change). Debouncer internals (`lastCommitted`, the internal ref) are not accessible by outsiders.

---

### Phase 3 — Store viewport payload; compute derived values from it

**Refactoring (no behavior change)**

Currently `onViewportAnchorPage` imperatively sets `pdfBarCurrentPage`, `pdfBarPagesTotal`, and `lastReadingForPatch` from the event payload. These should be computed from a single stored viewport payload ref.

**What changes:**

- **`BookReadingContent.vue`:** Add one reactive ref for the latest viewport event payload. Replace `pdfBarCurrentPage`, `pdfBarPagesTotal`, `lastReadingForPatch` with computed values derived from it. The `watch(selectedBlockId)` that reads `lastReadingForPatch` reads from the computed instead.
- **Removes:** 3 caching refs (`pdfBarCurrentPage`, `pdfBarPagesTotal`, `lastReadingForPatch`).

**Verified by:** All existing tests pass.

---

### Phase 4 — `currentBlockLiveText` → single computed

**Refactoring (no behavior change)**

Currently two refs (`currentBlockLiveText`, `lastAnnouncedCurrentBlockTitle`) are updated by a `watch(currentBlockId)` to track what was last announced. Since both end up with the same value, they can be replaced with a single computed: `structuralTitleForBlockId(currentBlockId, bookBlocks)`. Vue's reactivity already avoids re-rendering when the computed value hasn't changed, so the "changed" guard is unnecessary.

**What changes:**

- **`BookReadingContent.vue`:** Replace the two refs and the watcher with one computed.
- **Removes:** 2 caching refs and 1 watcher.

**Verified by:** All existing tests pass.

---

### Phase 5 — Auto-target next block when selected is already marked

**Behavior**

- **Pre-condition:** The selected block is already marked (read/skimmed/skipped). The next block exists and its content bottom is also visible in the viewport with room for the panel.
- **Trigger:** User scrolls to a position where both the selected (marked) block's bottom and the next block's bottom are in view.
- **Post-condition:** The panel appears anchored at the **next** block's content bottom, offering to mark that block.

**What changes:**

- Extend the panel-target logic: when the selected block is marked, check the next block in reading order. If its bottom is in the viewport with room, show the panel for it.
- Marking via this panel still advances `selectedBlockId` to the block after the one just marked (same `markSelectedDisposition` behavior).

**Tests**

- **E2E:** Mark a block → if the next block's bottom is visible, panel immediately shows for it without requiring further scrolling.

**Deploy gate:** Commit after green E2E for this phase.

---

## Remaining after all phases

`readingPanelAnchorTopPx` requires DOM measurements from `PdfBookViewer` and cannot be a pure computed. After Phase 1 it will have a single update site (the viewport event handler), which is acceptable. `windowWidth` mirrors a DOM measurement via resize listener — standard pattern.

## Order

- **Phase 1** — core behavior fix (user value: last block reachable, snap-back removed).
- **Phases 2–4** — caching variable cleanup (structural improvement, one group per phase, verified by existing tests).
- **Phase 5** — new behavior (auto-advance panel to next visible block).

## Out of scope

- Changes to `currentBlockIdFromVisiblePage` logic — stays as-is for the navigation bar and reading position.
- Tap-to-set-current — no longer needed since the panel follows selected-block visibility.
- "Mark remaining as read" / bulk operations.

## Status

| Phase | Status  |
|-------|---------|
| 1     | Planned |
| 2     | Planned |
| 3     | Planned |
| 4     | Planned |
| 5     | Planned |
