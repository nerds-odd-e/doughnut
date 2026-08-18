# Plan: New first-rating leftover cohesion

**Status:** in-progress

**Goal:** After New = ungraded first-rating shipped, remove leftover duplicate pins and put Hard-on-New on the outcome map (not inside shrink). Do not change live first-rating numbers, E2E outlines, or Flyway `V300000271` / `V300000272`.

Inspected the shipped first-rating commits (ADR lock → Again/Tutor 2 live path → gated backfill → stay-New copy cleanup). No live scheduling bug: first Again is `S0(1)` / `D0(1)`, Tutor **2** on New is Hard, shrink 80% only when `S > 0`, confusion on New still `S = 0`.

## Locked for this plan

- Keep E2E `spaced_repetition.feature` first-Again scenario and commissioned first-score outline rows **0–5**.
- Keep `newTrackerIncorrectRecallUsesS0AndD0Again` as the ordinary incorrect unit pin.
- Keep `V300000271` byte-stable (`StillNewAgainFirstRatingBackfill.run` facade stays).
- Do not accept ADR 0003. Do not squash Flyway. Do not retarget `D0` Java floats.

## Out of this plan

- Collapsing `FIRST_AGAIN_*` / `FIRST_HARD_*` constants across test classes (same pattern as first Good).
- Skipping `deleted_at` in backfill (deleted trackers are not a user-visible queue).
- Parameterizing all first-score unit tests into one table (E2E already owns that outline; leftover here is duplicate *assertions*, not missing coverage).
- `ForgettingCurve.succeeded` (thinking-time tests; predates this work).

## Slices

### 1. Drop leftover duplicate first-Again pins

- **Type:** Structure
- **Status:** done

Removed leftover first-Again pins. `firstScoreZeroOrOneOnNewPersistsD0Again` is D+S only. Ordinary first-Again pin and commissioned due shape unchanged.

**Learning:** no product change; slice 2 still needs the `runHard` Again-only negative.

### 2. Hard backfill does not select Again-only New

- **Type:** Behavior
- **Status:** done

`runHard_leavesAgainOnlyTrackersNew` pins Again-only New stays New. Production `runHard` already selects SHRINK only — missing pin, not a selection bug.

### 3. Tutor 2 on New first-rates at the outcome map, not inside shrink

- **Type:** Structure
- **Status:** planned

Enables slice 4 (New check only remains on the forgetting curve).

`MemoryTracker.shrinkStability` / `MemoryTrackerShrinkStability` currently first-rates Hard when `isNewlyAssimilated()`. The method name is shrink; Hard first-rating is Tutor **2** on New. Put that branch on `CommissionedLearningSessionFeedbackScheduling.recordFeedback` (`SHRINK` + New → `recalledHard`; `S > 0` → `shrinkStability`). Shrink helper only does 80% and does not call `recalledHard`.

No product change: `firstScoreTwoOrThreeOnNewPersistsD0Hard` and `onTimeSecondScoreTwoShrinksStabilityAndLeavesDifficultyUnchanged` still pass. Do not change RecallLog `SHRINK` vs `HARD`.

**Done when:** shrink helper has no New/Hard branch; commissioned first **2** and graded shrink tests still pass.

### 4. Call New what the glossary calls New

- **Type:** Structure
- **Status:** planned

Justified by slice 3 leaving `ForgettingCurve.isNewlyAssimilated()` as the only New check for first-rating and confusion-on-New.

Rename that predicate to match ADR 0001 **New** (ungraded: `S = 0`). Keep the implementation (`stabilityHours <= ASSIMILATE_STABILITY_HOURS`). Confusion on New still returns `S = 0`. Do not change `DEFAULT_DIFFICULTY` fallback for `S > 0` with null D.

**Done when:** production Java says New, not “newly assimilated,” for that check; targeted scheduling tests pass.

## Tests (capability-owned)

| Capability | Where |
|---|---|
| Ordinary first Again | `SpacedRepetitionIncorrectRecallSchedulingTest`, `spaced_repetition.feature` |
| Commissioned first 0–5 / shrink when `S > 0` | `LearningSessionRecordTutorFeedbackTests`, `commissioned_learning_session.feature` |
| Hard backfill selection | `StillNewFirstRatingBackfillTest` |

No product types named after this plan number.
