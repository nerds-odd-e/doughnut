# CONTEXT: FSRS leftover-close follow-up

**Scope:** Inspection of commits `a73c312190`–`d359b59298` (Tutor 3/2/1/0, confusion midpoint, live-ladder drop, ADR 0003 locks). Fix test holes and cohesion leftovers from that work. Do not change schedule numbers. Do not accept ADR 0003.

## What is already correct

- Tutor **5/4/3/2/1/0** match Proposed ADR 0003 Decision (Easy / Good / Hard / 80% shrink / Again-from-S / same as 1).
- Ordinary incorrect still Again memory then **+12h**.
- Confusion on-time unique spelling match pins **115** (midpoint of 200 and Again 30); D / lastRecalledAt / recallCount unchanged; due not later.
- Live `hoursAfterSpacingDelta` is gone; `hoursFromLegacyIndex` remains for `V300000260`.
- Parser rejects scores outside 0–5 before `recordFeedback`.

## Locked this session

| Finding | Kind | Fix in this plan |
|---------|------|------------------|
| Understanding-when-spelling-absent no longer asserts the secondary tracker **weakens** (only that it is linked) | Test hole | Yes — unique claim: S decreases; do not re-pin 115 |
| New score **0** is not in the D-unset / S=0 parameterized test | Test hole | Yes — add 0 next to 1 and 2 |
| On-time score **1** is three tests (S, D, due) of one scenario | Redundant tests | Yes — one test, three unique claims |
| On-time / overdue S pins for 3/4/5 are copy-pasted tests | Redundant tests | Yes — parameterize S; keep per-grade next-D tests |
| E2E second score 4/5/3 are near-identical scenarios | Redundant E2E | Yes — Scenario Outline |
| Score **2** shrink mutates the tracker in `CommissionedLearningSessionFeedbackScheduling`; other grades go through `MemoryTracker` | Cohesion | Yes — move shrink onto `MemoryTracker` |
| `recalledEasily` / `recalledHard` / `recalledSuccessfully` plus repeated New-init guards on `ForgettingCurve` | Duplication from adding Hard | Yes — one apply path; keep `FsrsHardRecall` / `FsrsEasyRecall` / `FsrsGoodRecall` as own types |

## Out of scope

Accept ADR 0003. Deferred B2/B4/C4/E3/E4/E6. Merging `Fsrs*Recall` into one class. New E2E for score 1 due (controller already pins last+S). Changing 71 / 8 / 19 / 115. Flyway. Deleting `DEFAULT_SPACES`.
