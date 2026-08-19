# Plan: After-Again leftover test cohesion

**Status:** in-progress  
**Goal:** Drop the duplicate same-hour After-Again pin, put the short-term window tests with the other G pins, and lock elapsed **24** as post-lapse. No schedule change.

## Inspection (025 After-Again, `af6bed1fc3..98b9ac80eb`)

No production bug found. `elapsed < 24` matches Proposed ADR 0003. Confusion still inherits Again S (on-time accidental-match pin **115** is elapsed **200**, still post-lapse). Tutor on-time score **1** is still **15**.

Meaningful leftover:

- **Fencepost gap.** Good has `twentyFourHourCorrectRecallGrowsThreeDayStability`. After-Again pins elapsed **0**/**23** → **24** on `MemoryTrackerSameHourRecallSchedulingTest` (`nextStabilityHoursAfterAgain`) and on-time 72h → **17**, but not elapsed **24**. A `<= 24` slip would not fail those tests.

## Decisions

- Delete the duplicate first-Good same-hour **unit** test. Keep the E2E; trim it to the delta (Stability **18** / **18h**).
- Keep `incorrectRecallFromOneHourStabilityPersistsOneHour` (unique 1h floor inside the window). Keep on-time after first Good **15** (unit + day-3 E2E existed before 025).
- Do not add Tutor same-hour score **1** or a same-hour confusion pin (same Again S; no separate rule).
- Do not lazy-eval `longTermHours`. Do not split ADR 0003.

## Slices

### 1. Drop duplicate same-hour After-Again after first Good

**Type:** Structure  
**Status:** done

Deleted `sameHourIncorrectRecallAfterFirstGoodUsesShortTermStability`. E2E `Same-hour Again after first Good uses short-term Stability 18` now pins only Stability **18** / **18h**. Difficulty / AGAIN RecallLog stay on the day-3 scenario and on-time unit **15**. 72h / 23h pins left for slice 2.

### 2. House After-Again window pins with the other short-term grades

**Type:** Structure  
**Status:** done

One parameterized pin `againRecallOnThreeDayStabilityUsesShortTermNotPostLapse` (elapsed **0**, **23** → Stability **24**) on `MemoryTrackerSameHourRecallSchedulingTest`, via `nextStabilityHoursAfterAgain` (`recalledAgain`). 1h-floor and on-time **15** / **17** stay on IncorrectRecall.

### 3. Elapsed 24 After-Again uses post-lapse Stability 17

**Type:** Behavior  
**Status:** planned

**Pre:** Graded tracker S=**72h**, D=5.  
**Trigger:** Again at elapsed **24**.  
**Post:** Stability **17** (post-lapse), not short-term **24**.

Add next to `twentyFourHourCorrectRecallGrowsThreeDayStability`, using `nextStabilityHoursAfterAgain(24)`. Unit only (same boundary as that Good pin; no new E2E). Do not fold elapsed **24** into the short-term parameterized pin (different expected S).

**Done when:** elapsed **23** → **24** and elapsed **24** → **17** on the same 72h tracker.

## Not this plan

- Tutor same-hour score **1**
- Same-hour confusion midpoint
- Splitting Proposed ADR 0003
- Lazy `longTermHours` / `Supplier`
- Deleting on-time After-Again **15** (pre-existing unit + day-3 E2E)
- Long-term After-Again cap; accept ADR 0003; E4
