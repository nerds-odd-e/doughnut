# Plan: Reading record

**User story:** [`ongoing/book-reading-user-stories.md`](book-reading-user-stories.md) — *Story: Reading record*.

**Architecture:** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md).

**UX context:** [`ongoing/book-reading-ux-ui-roadmap.md`](book-reading-ux-ui-roadmap.md) and Story 2 reader work in [`ongoing/book-reading-read-a-block-plan.md`](book-reading-read-a-block-plan.md).

**Planning rule:** `.cursor/rules/planning.mdc` — one user-visible behavior per phase, scenario-first ordering, test-first workflow.

**This document is a delivery plan only** — update phases here before implementation, then trim obsolete detail after each shipped slice.

**Shipped baseline (do not regress):** Reading-record Phases **1–12** — including **Phase 5a** (same-page snap to block start), **Phase 5b** (snap attention animation), **Phase 11** (content-anchored panel), and **Phase 12** (current ≠ selected: "read from here" / "back to selected"). Observable contract: [`e2e_test/features/book_reading/reading_record.feature`](../e2e_test/features/book_reading/reading_record.feature) and mounted reader tests (e.g. `BookReadingPage.spec.ts`). Deeper vocabulary: architecture + UX roadmaps above.

---

## Principles (unchanged)

- **Progress remains per block:** `ReadingRecord` attaches to **`BookBlock`**, not PDF coordinates.
- **Direct-content gating:** explicit disposition only when the block has **direct content** and no recorded disposition; auto-read for **no-direct-content** blocks stays.
- **Reading order:** depth-first preorder for predecessor/successor, **current block**, and direct-content boundaries.
- **Snap-back:** reminder only (two attempts per block); not a persisted lock.
- **Tests:** prefer mounted `BookReadingPage` / reader-flow tests and controller tests when HTTP changes; avoid tests that only pin private counters or watcher internals.

---

## Phase 5a — Snap to block start when block fits on one page (shipped)

**Shipped:** `performSnapBack` checks `parsedStart.pageIndex === lastBbox.pageIndex`; same-page snaps to block start via `scrollToBookNavigationTarget`, cross-page snaps to last content bottom via `snapToContentBottomAndHold`. Tests in `BookReadingPage.spec.ts` (same-page vs cross-page assertions).

---

## Phase 5b — Animated panel on snap (shipped)

**Shipped:** `snapAnimationKey` increments on snap; `ReadingControlPanel` plays `snap-attention` CSS animation. Tests: `data-snap-animating` attribute appears on snap and clears after `animationend`.

---

## Phase 8 — No timed auto-selection of the viewport "current" block (shipped)

**Shipped:** removed dwell timer that promoted **current** → **selected**.

---

## Phase 9 — Always a selected book block; default to the first (shipped)

**Shipped:** `BookReadingContent` watches `book.blocks` (`immediate`); when non-empty, sets **selected** to **`blocks[0].id`** if selection is null or not in the list; empty layout clears selection. Does **not** call `applyBookBlockSelection` (last-read position restore unchanged).

---

## Phase 10 — Persist and restore selected block with last view position (shipped)

**Shipped:** nullable **`selected_book_block_id`** on **`book_user_last_read_position`**, **`GET`/`PATCH` reading-position** wire field **`selectedBookBlockId`**; **`PATCH`** updates the FK only when a non-null id is sent. **`BookReadingPage`** / **`BookReadingContent`** persist selection with debounced position patches and restore from **`getNotebookBookReadingPosition`**.

---

## Phase 11 — Panel below last content bottom when it would float above the obstruction zone (shipped)

**Shipped:** `PdfBookViewer.readingPanelAnchorTopPx` + `BookReadingContent` passes **`anchorTopPx`** into **`ReadingControlPanel`** when **`lastContentBottomVisible`**; sticky / no-geometry path stays **`data-panel-placement="fixed"`**. Optional clamp falls back to bottom-docked when the main pane is too short (skip clamp when main height is 0).

---

## Phase 12 — Current ≠ selected: "read from here" and "back to selected" (shipped)

**Shipped:** `CurrentBlockNavigationBar.vue` shown at bottom of main pane when `currentBlockId ≠ selectedBlockId`; **Read from here** calls `applyBookBlockSelection(currentBlock)` making current the new selected; **Back to selected** calls `applyBookBlockSelection(selectedBlock)` scrolling back. Computed `currentBlockForNavBar` in `BookReadingContent.vue`. Tests: two E2E scenarios in `reading_record.feature`; mounted coverage in `BookReadingPage.spec.ts`.

---

## Phase 13 — Snap-back target: content-fits-with-panel condition

**User story scenario:** the user scrolls past an unread block whose content is short enough to fit on one screen together with the panel — snap-back should show the whole block from the top. When content is longer, snap-back should show the last content area instead.

**User outcome:** when snap fires:

- **Same page + content fits with panel:** if the selected block's first bbox and last content bbox are on the **same page**, AND viewing from the block's top would place the last content bbox bottom **above** the panel anchor zone (i.e. the full block content + panel fit in the viewport) → scroll to the **block start** (same as clicking the block in the book layout). The panel anchors right below the last content bottom.
- **Otherwise** (cross-page, or same page but content too tall for block-top + panel to coexist): scroll so the **last content area** of the selected block is fully visible, with the last content bbox bottom sitting just above the panel. The panel anchors right below the last content bottom.

In both cases the panel uses Phase 11 anchoring (right below the last content block bottom).

**Depends on:** shipped Phases 5a, 11.

**What changes from Phase 5a:** the same-page path gains an additional check — "last content bottom is above the panel zone when viewed from block top." Without this, a same-page block with very tall content would snap to the block start but the panel would be off-screen below. With the new condition, that case falls through to the "show last content" path instead.

**Tests (no new E2E):** mounted — same-page-fits → scroll-to-block-start; same-page-too-tall → scroll-to-last-content-bottom; cross-page → scroll-to-last-content-bottom. Extend or replace existing same-page snap assertions.

---

## Phase 14 — Interval-based scroll suppression after snap

**User story scenario:** after snap-back fires the user's momentum scroll continues; the viewport should absorb the remaining scroll gesture without undoing the snap, but release promptly when the user pauses or initiates a new deliberate scroll.

**User outcome:** after snap fires:

- Scroll events arriving **< 100ms** after the previous event are **ignored** (momentum tail).
- The first event with a **≥ 100ms gap** from the previous event **releases** the suppression (deliberate new scroll).
- If cumulative suppression time exceeds **`SNAP_HOLD_MS`**, suppression releases regardless of interval (safety cap).

This replaces the current hard `suppressScrollInput(holdMs)` timer that blocks all wheel/touch-move for a fixed duration.

**Depends on:** shipped Phase 5 scroll suppression in `PdfBookViewer`.

**What changes:** `suppressScrollInput` in `PdfBookViewer` becomes interval-aware: instead of a single `setTimeout` that flips `scrollSuppressed = false`, it tracks the timestamp of the last suppressed event, checks the gap on each new event, and releases when the gap exceeds 100ms or cumulative time exceeds `SNAP_HOLD_MS`. The composable (`useBookReadingSnapBack.performSnapBack`) still calls the same API surface; the behavior change is inside the viewer.

**Tests (no new E2E):** mounted or unit — rapid events (< 100ms apart) stay suppressed; event after ≥ 100ms gap releases; release after `SNAP_HOLD_MS` cumulative regardless of gaps.

---

## Phase discipline

After each phase:

1. Add or update the focused high-level test first, and confirm the failure is for the right reason.
2. Implement the smallest change that makes that phase green while preserving existing `reading_record.feature` paths.
3. Remove temporary state or dead branches that were only useful during the phase.
4. Update this plan and, when behavior changes defaults, the architecture or UX roadmap in the same delivery stream.

---

## Document maintenance

Keep this file forward-looking. Shipped implementation detail lives in code, tests, and the architecture/UX roadmaps.
