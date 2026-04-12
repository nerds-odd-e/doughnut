# Book reading: current block at scroll end + tap to set current

## Problem

When the last book block sits low on the last page (content above), scroll-based “current block” logic may never promote it, so the mark-as-read panel never appears. The second-to-last block can fail similarly if the title never crosses the viewport threshold.

## Outcome

1. **Scroll-end (automatic):** When the reader reaches the **end** of the reading scroll surface, the **last** book block (in reading order) can become **current** so mark-as-read can appear even if the title never satisfied the normal visibility rule.

2. **Tap to set current (manual):** The user can **tap a block** (or an explicit control on it) to **set that block as current**, independent of scroll, so awkward layouts stay completable.

## Phase 1 — Scroll-end: last block becomes eligible as current

**Behavior**

- **Pre-condition:** Book has multiple blocks; last block is low on the page with content above (reproduces the bug).
- **Trigger:** User scrolls to **effective bottom** of the reading surface (one rule: e.g. `scrollTop + clientHeight >= scrollHeight - ε`, small ε for subpixel).
- **Post-condition:** **Current block** updates to the **last** block (or a single documented rule for “last in-view at bottom”), and the mark-as-read UI can show for that block like other current blocks.

**Implementation notes**

- Likely touch: `currentBlockIdFromVisiblePage` (or equivalent), scroll handling, `BookReadingContent.vue` / composable that proposes `currentBlockId` via the existing debouncer.
- **Debouncing:** At bottom, consider **immediate commit** or a narrow exception so end-of-doc does not feel laggy.
- **Priority:** When “at bottom” conflicts with the visible-page heuristic, **bottom wins** only while the bottom condition is true.

**Tests**

- **E2E:** Layout where the last block never wins under the old rule; scroll to bottom → assert **current block** is the last (or assert mark-as-read panel for last block—pick the most stable observable).

**Deploy gate:** Commit after green E2E for this phase.

## Phase 2 — Tap (or explicit control) to set current block

**Behavior**

- **Pre-condition:** Any book with blocks; optional: same awkward layout as Phase 1.
- **Trigger:** User **activates** a block (tap on block chrome / row / “set as current”—exact control TBD).
- **Post-condition:** **That block** becomes **current**; mark-as-read reflects it; scroll need not move (optional scroll-into-view deferred unless needed).

**Implementation notes**

- Wire from block list / row component to the same **`currentBlockId`** pipeline (e.g. `commitNow` / equivalent).
- **Accessibility:** Keyboard/focus if not pointer-only.
- **vs scroll:** Explicit user selection should **override** scroll-based proposals until the next meaningful scroll (define a minimal rule to avoid oscillation).

**Tests**

- **E2E:** Tap a chosen block → assert **current** (or panel) matches **that** block without requiring special scroll.

**Deploy gate:** Commit after green E2E for this phase.

## Order

- **Phase 1 before Phase 2** for the smallest slice that fixes “last block never current.”
- **Phase 2** still adds value after Phase 1 (penultimate blocks, user override).

## Optional sub-phases

If a phase is still large, use **E2E-led sub-phases**: one Gherkin step at a time (red → green → uncomment next step).

## Out of scope (for this plan)

- “Mark remaining as read” / full unread block picker (not part of the two mechanisms above).
- Large refactors of the visibility algorithm beyond what Phase 1 needs.

## Status

| Phase | Status   |
|-------|----------|
| 1     | Planned  |
| 2     | Planned  |
