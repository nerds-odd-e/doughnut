# Plan: Same-hour short-term leftover cohesion

**Status:** in progress (slice 1 done)  
**Index:** ad-hoc under `.planning/quick/` — leftover from shipped same-hour FSRS-6 short-term. Do not Accept ADR 0003.

**Goal:** One production dispatch for elapsed-0 success short-term, and same-hour tests that name the clamp (not “does not grow”) without a weakened 0ms thinking-time extra assert.

**Context:** [CONTEXT.md](./CONTEXT.md)

Sequential. Each slice is one Structure, stop-safe, **one commit**. No new scheduling behavior. Existing unit/E2E pins stay 25 / 43 / 24 / 72.

---

### 1. Same-hour tests drop the weakened extra assert and name the clamp

Type: Structure  
Status: done

Same-hour tests name grow-at-24 / clamp-at-72. EarlyRecall extends `SpacedRepetitionRecallSchedulingTestBase`; elapsed-0 pin is `sameHourCorrectRecallDoesNotShrinkThreeDayStability` next to 24/43/24. Thinking-time same-hour test keeps only on-time > same-hour at base thinking time.

---

### 2. Elapsed-0 success uses one short-term dispatch

Type: Structure  
Status: planned

Structure change: one representation of “elapsed 0 and S > 0 → FSRS-6 short-term next Stability.” Immediate next: none in this plan (shipped pins already cover Good/Easy/Hard).

**Commit bound:** `Fsrs` plus `FsrsGoodRecall` / `FsrsEasyRecall` / `FsrsHardRecall` (and `ForgettingCurve` only if the dispatch lives there). Do not change pins, E2E, ADR, gap, or seed.

- Collapse the three copied `if (elapsedInHours == 0) return hoursAfterShortTermRecall(...)` branches onto one helper used by Good / Easy / Hard.
- Keep file-per-grade long-term SInc (Good term, Easy × W[16], Hard × W[15]). Do not invent a parameterized success-grade framework beyond that dispatch.
- Thinking time still wraps Good after the helper. New (S = 0) still inits 24 via `afterGoodHardOrEasyRecall`.
- Existing tests still pass with no assertion changes.

**Done when:** elapsed-0 short-term routing has one production site; Good/Easy/Hard long-term paths unchanged; `pnpm backend:test_only` green.
