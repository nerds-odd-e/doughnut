# Plan: Thinking time is not a memory-state input

**Status:** in progress (slice 1 done)  
**Type:** Behavior then leftover Structure  
**Seed:** [SEED-004](../../seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md)  
**Research (delete when leftover slice lands):** [THINKING-TIME-DSR.md](../../research/THINKING-TIME-DSR.md)

## Goal

Ordinary Good next Stability is open FSRS-6 Good. Thinking time stays on the answer and on recall stats / Memory Tracker prompt history. No interval fuzz or other shuffle replaces the overlay. Existing Stability rows are not rewritten.

## Decisions

- Thinking time is **not** a DSR input (grade G, elapsed t, D/S/R only). ADR 0003 **Thinking time** locks that; the ADR stays **Proposed** (do not accept).
- Record and display thinking time. Do not change Stability, Difficulty, or due.
- Due stays `lastRecalledAt + I(0.9, S)` in whole hours (clock instant kept). No fuzz.
- Do not backfill or clamp historical S that was computed under the overlay.
- Leave no overlay implementation: no `adjustForThinkingTime`, no `BASE_THINKING_TIME_MS` / `MAX_THINKING_TIME_MS`, no live use of `LEGACY_INDEX_STEP`.

## Slices

### 1. Ordinary Good Stability ignores thinking time — done — Behavior

**Pre:** Graded tracker (`S > 0`).  
**Trigger:** Ordinary correct (just-review Yes / MCQ / spelling) with thinking time missing, 0, or 60s.  
**Post:** Next Stability, Difficulty, and due equal FSRS-6 Good for that elapsed time (same as missing effort). The answer still stores thinking time when the prompt measured it.

`afterGoodRecall` is FSRS Good only. Overlay methods and `ForgettingCurveThinkingTimeTest` are gone. Proposed ADR 0003 **Thinking time** matches. Capture / stats / Memory Tracker display remain.

**Learnings:** Overlay was Good-only; Hard/Easy/Again/first-rating/Tutor already ignored thinking time; no new shuffle. Fast-vs-slow controller tests went with the overlay. Wrap-up deleted unused `succeeded()` and moved maximum-interval tests to `SpacedRepetitionCorrectRecallMaximumIntervalSchedulingTest`.

### 2. No overlay left on the grade path — planned — Structure

No observable schedule change. Remove leftover overlay shape so the grade path maps to the Decision.

- Drop unused `thinkingTimeMs` from `MemoryTracker.recalledSuccessfully` / `markAsRecalled` / `ForgettingCurve.afterGoodRecall`. `MemoryTrackerService` still reads thinking time off the answer to persist it; it does not pass it into the memory update. (`succeeded()` already gone.)
- Delete `BASE_THINKING_TIME_MS` / `MAX_THINKING_TIME_MS`. Tests that used them as a 25s stand-in pass `null` or any recorded ms.
- Delete `thinkingTimeOnOverCapCorrectRecallDoesNotPierceTheCap` in `SpacedRepetitionCorrectRecallMaximumIntervalSchedulingTest` (duplicate of the cap test once RT cannot pierce).
- `firstCorrectRecallIgnoresReviewTiming`: keep elapsed-hours cases; drop the max-thinking-time column.
- `LEGACY_INDEX_STEP` private (Flyway `V300000260` replay only).
- Tracker / seed: overlay closed. Remaining FSRS: **E4** + human accept of ADR 0003.
- `trash` `.planning/research/THINKING-TIME-DSR.md`.
- When this slice is done, this plan is spent: drop `.planning/quick/015-thinking-time-not-memory-state/` (outcomes live in ADR 0003 and code).

**Tests:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Out of scope

- Accepting ADR 0003
- E4 fitting
- Interval fuzz or any new due shuffle
- Rewriting existing Stability
- Dropping thinking-time capture, stats, or Memory Tracker display
- Deleting `SpacedRepetitionAlgorithm` / `DEFAULT_SPACES` (`V300000260` must replay)

## Done when

Ordinary Good next S is FSRS-6 Good regardless of thinking time; overlay code and `THINKING-TIME-DSR.md` are gone; ADR 0003 Thinking time matches.
