# Book reading: panel visibility based on block bottom in viewport

## Problem

When the last book block sits low on the last page, the mark-as-read panel never appears because the scroll-based "current block" logic cannot promote it. The current system uses a complex chain (scroll → `currentBlockId` → snap-back with attempt counters and geometry tracking → `blockAwaitingConfirmation`) that is hard to reason about and still fails for edge layouts.

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

---

## Phase 1 — Panel shows when selected block's bottom is visible + bottom padding

**Behavior**

- **Pre-condition:** Book with blocks; the selected block has not been marked.
- **Trigger:** User scrolls until the selected block's content bottom is in the viewport with at least panel-height room below it.
- **Post-condition:** The mark-as-read panel appears anchored at the selected block's content bottom. Scrolling the block bottom out of view hides the panel. For the last block, bottom padding on the scroll surface ensures it can always be scrolled far enough.

**What changes:**

- **`useBookReadingSnapBack` (or replacement):** Simplify `blockAwaitingConfirmation` to: selected block is not yet marked AND its content bottom is visible in the viewport with room for the panel. Remove snap-back logic (attempt counters, `geometryEverVisibleForSelection`, `performSnapBack`, `shouldSnapBack`, `snapToContentBottomAndHold`, `suppressScrollInput`).
- **`PdfBookViewer.vue`:** Add bottom padding to the scroll container equal to the panel height, so the last block's bottom can be scrolled into view.
- **`BookReadingContent.vue`:** Remove snap-back wiring (`snapAnimationKey`, `shouldSnapBack`, `performSnapBack`, `clearSnapbackAttemptsForBlock`). The `updateReadingPanelAnchor` logic can be simplified since the panel visibility and anchor are now one concern.

**Tests**

- **E2E:** Book where the last block's content is near the bottom of the last page. Scroll to bottom → panel appears for the last block. (This is the scenario that fails today.)

**Deploy gate:** Commit after green E2E for this phase.

---

## Phase 2 — Auto-target next block when selected is already marked

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

## Order

- **Phase 1 first** — delivers the core fix (last block reachable, snap-back removed).
- **Phase 2** — adds the flow convenience (auto-advance panel to next visible block).

## Out of scope

- Changes to `currentBlockId` logic or `currentBlockIdFromVisiblePage` — those stay as-is for the navigation bar and reading position.
- Tap-to-set-current (the old Phase 2) — no longer needed since the panel follows selected-block visibility.
- "Mark remaining as read" / bulk operations.

## Status

| Phase | Status  |
|-------|---------|
| 1     | Planned |
| 2     | Planned |
