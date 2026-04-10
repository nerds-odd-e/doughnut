# Plan: Reading record

**User story:** [`ongoing/book-reading-user-stories.md`](book-reading-user-stories.md) — *Story: Reading record*.

**Architecture:** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md).

**UX context:** [`ongoing/book-reading-ux-ui-roadmap.md`](book-reading-ux-ui-roadmap.md) and shipped Story 2 reader work in [`ongoing/book-reading-read-a-block-plan.md`](book-reading-read-a-block-plan.md).

**Planning rule:** `.cursor/rules/planning.mdc` — one user-visible behavior per phase, scenario-first ordering, test-first workflow.

**This document is a delivery plan only** — update phases here before implementation, then trim obsolete detail after each shipped slice.

**Status:** Core reading-record behavior is already shipped:

- **Phase 2:** explicit **Mark as read** from the Reading Control Panel.
- **Phase 3:** auto-mark **READ** for blocks with **no direct content**.
- **Phase 4:** explicit **skimmed / skipped** dispositions.
- **Phase 5:** first snap-back reminder when scrolling past an unread block's end boundary.

---

## Principles for remaining work

- **Progress remains per block:** `ReadingRecord` is still attached to **`BookBlock`**, not arbitrary PDF coordinates.
- **Direct-content gating stays as shipped:** explicit disposition is needed only for blocks that still have **direct content** and no recorded disposition; Phase 3 auto-read behavior remains the first path for **no-direct-content** blocks.
- **Reading order stays linear:** predecessor/successor and reminder logic follow the same **depth-first preorder** already used for **current block** and direct-content boundaries.
- **Snap-back is a reminder, not a lock:** each block may interrupt scrolling at most **twice**; later attempts must scroll normally even if the block remains unread.
- **Snap-back state is local and disposable:** it belongs to the current user/session view of a specific block, clears immediately when that block is marked **READ**, and does not become a new long-lived persisted progress concept unless a later plan explicitly chooses that.
- **Tests stay high-level:** prefer mounted `BookReadingPage` / reader-flow tests that drive the real prompt and scroll-handling contract, plus controller tests only where an observable HTTP contract changes. Avoid low-level tests that only pin private counters or watcher internals.

---

## Existing baseline

These shipped phases are the baseline that later reminder phases build on:

- **Phase 2:** when the viewport reaches the immediate successor of a selected block, the **Reading Control Panel** lets the user mark that selected block **READ**.
- **Phase 3:** when a predecessor block has **no direct content**, entering the successor auto-marks the predecessor **READ** instead of prompting.
- **Phase 4:** the same panel path also supports **SKIMMED** and **SKIPPED**.
- **Phase 5:** on the first crossing of the end boundary of an unread block with direct content, the reader snaps the viewport back so the last content bbox bottom sits above the panel, and keeps the panel visible. No E2E changes — covered by mounted tests.

Keep this section short; detailed shipped implementation notes belong in code/tests and the architecture/UX roadmaps.

---

## Phase 1 — Remember book last read position

**User story scenario:** leave the book, return later, land on the same reading position.

**User outcome:** reopening the book restores the last reading position for the same user and book.

**Test shape:** no E2E required; prove via controller tests for persistence and mounted reader/page tests for save + restore behavior through observable viewer contracts.

**Out of scope:** per-block disposition prompts, snap-back reminder state, read/skim/skip styling.

---

## Phase 5a — Snap to block start when block fits on one page

**User story scenario:** the user scrolls past an unread block whose top and last direct-content bbox are on the same page — snap-back should show the full block from the beginning.

**User outcome:** when the snap fires and the selected block's start anchor and last direct-content bbox are on the **same page**, the reader scrolls to the **block's start position** (same scroll target as when the user clicks the block in the layout), not just to the last content bbox bottom. If they are on **different pages**, the existing Phase 5 behavior (last content bbox bottom above the panel) applies unchanged.

**Depends on:** Phase 5.

**Notes for implementation shape:**

- The condition is: `lastBbox.pageIndex === startAnchor.pageIndex` (start anchor parsed page index equals last content bbox page index).
- The "block start" scroll target is the existing `scrollToPdfOutlineV1Target` path used by `applyBookBlockSelection` — reuse that, not a new coordinate calculation.
- `snapToContentBottomAndHold` in `PdfBookViewer` still handles the suppression window; the scroll target is just chosen differently before the call, or a new exposed method handles both.
- No change to the snap attempt counter or suppression duration.

**Tests (no new E2E):**

- **Mounted test — same-page block:** block whose start anchor and last content bbox are on the same page; assert that `scrollToPdfOutlineV1Target` (not `snapToContentBottomAndHold`'s raw scrollTop) is called when snap fires.
- **Mounted test — cross-page block:** block whose start anchor and last content bbox are on different pages; assert that the existing last-bbox-bottom target is used.

---

## Phase 5b — Animated panel on snap

**User story scenario:** the snap fires but the user did not notice the Reading Control Panel was already visible; the animation draws their eye to it.

**User outcome:** when a snap-back fires, the Reading Control Panel plays a brief attention animation (e.g. a pulse, shake, or highlight flash) so the user clearly sees the prompt they need to answer.

**Depends on:** Phase 5.

**Notes for implementation shape:**

- The animation is applied to the `ReadingControlPanel` component root element at the moment the snap fires, not on every render.
- Use a CSS keyframe animation class that is added on snap and removed after it plays (one-shot via `animationend` listener or a short timer matching the animation duration).
- The panel's layout and hit targets must not shift during the animation; prefer transforms or opacity/shadow rather than position changes.
- If the panel is already visible when snap fires, the animation still replays (re-triggers).

**Tests (no new E2E):**

- **Mounted test:** after snap fires, assert that the panel root element has the animation class (or a `data-*` attribute used to trigger it via CSS); assert that the class is removed after the animation completes or the timer elapses.

---

## Phase 6 — One more reminder, then release scrolling

**User story scenario:** after the first snap-back, the user again tries to scroll past the same unread block without marking it as read.

**User outcome:** the reader applies the same snap-back behavior **one more time** on the **second** attempt, then allows **normal scrolling** on the **third and later** attempts for that same block.

**Depends on:** Phase 5.

**Notes for implementation shape:**

- Count attempts **per block** in the active reader state.
- "Allow normal scrolling" means the prompt may still be present or reappear by the usual rules, but the forced scroll restoration must stop after attempt two.
- Keep the counting rule simple: at most **two** snap-backs for one block before normal scrolling resumes.

**Tests (no new E2E):**

- **Mounted reader/page sequence test:** simulate first, second, and third boundary crossings for the same unread block and assert: snap-back on attempt 1, snap-back on attempt 2, no snap-back on attempt 3.
- **Mounted continuation test:** assert that attempt 4+ behaves the same as attempt 3.

---

## Phase 7 — Clear snap-back on read; scope reminders per block

**User story scenario:** the user marks a block as read after one or two reminders, then continues reading and later reaches another unread block.

**User outcome:**

- marking the block **READ** clears that block's snap-back reminder state immediately, so later scrolling for that block proceeds normally, and
- a different unread block gets its **own** reminder budget of up to two snap-backs.

**Depends on:** Phase 6.

**Notes for implementation shape:**

- Clearing is immediate on successful **Mark as read** completion; do not wait for navigation away and back.
- The per-block scope should not accidentally share attempt counts across siblings, parent/child neighbors, or repeated visits to a different block.
- This phase is only about **READ** because the requested behavior says "marked as read"; if product later wants skim/skip to clear the reminder too, that should be an explicit follow-up decision.

**Tests (no new E2E):**

- **Mounted reader/page test:** after one or two snap-backs, complete **Mark as read** and assert that subsequent scrolling no longer restores position for that block.
- **Mounted per-block test:** exhaust reminders for block A, move to a different unread block B, and assert that B still gets its own first and second snap-backs.
- **Controller coverage only if needed:** add or extend HTTP tests only if the implementation introduces a new observable API contract. Otherwise keep coverage in mounted reader-flow tests.

---

## Phase discipline

After each phase:

1. Add or update the focused high-level test first, and confirm the failure is for the right reason.
2. Implement the smallest change that makes that phase green while preserving the existing `reading_record.feature` path.
3. Remove temporary state or dead branches that were only useful during the phase.
4. Update this plan to reflect what shipped and trim notes that no longer matter for later phases.
5. If the work changes a long-lived default, update the architecture or UX roadmap in the same delivery stream.

---

## Document maintenance

Keep this file forward-looking. Detailed shipped implementation notes should move to code, tests, or the architecture/UX roadmaps once they stop helping with upcoming phases.
