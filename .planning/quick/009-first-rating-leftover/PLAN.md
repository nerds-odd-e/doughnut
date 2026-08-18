# Plan: First-rating leftover cohesion

**Status:** in-progress

**Goal:** E2E that pretends to be “after first success” actually goes through New first-rating (`S0`/`D0`), and the testability seed stack this plan introduced is gone. No product schedule change.

Inspected range: `f323ce565e`..`4ccfd5a624` (quick 008). Proposed ADR 0003 stays Proposed.

## Locked

- First Good stays **55** / **`2.118104f`**. Same-hour Good after that uses short-term from **S=55** (clamp → Stability stays **55**), not the old 24→25 story.
- 24→25 short-term stays a **unit** pin on a graded S=24 tracker (`sameHourCorrectRecallGrowsFirstIntervalStabilityToTwentyFive`). Do not delete it.
- Incorrect just-review after first Good uses FSRS-6 Again from **S0(Good)/D0(Good)**, not Again from seeded S=24 / D=5.
- Do not accept ADR 0003. No Flyway. No New Again init.

## Out of this plan

E3 / E4, accepting ADR 0003, collapsing `FIRST_GOOD_*` constants across Java test classes, splitting files at the 250-line cliff (`MemoryTracker` 248, scheduling test 246).

## Not doing (inspected, not meaningful)

- Entity first-Good pin vs commissioned controller first score **4**: different user paths (ordinary recall vs Tutor 4). Keep both.
- Controller first 3/5 exact floats vs E2E display rounding: complementary. Keep the controller pins.
- `firstCorrectRecallIgnoresReviewTiming`: unique claim (thinking time / elapsed). Keep.
- `Map<String, Object>` on other testability endpoints: pre-existing pattern; goes away with the seed endpoint.

## Slices

### 1. Same-hour Good after first Good stays S0(Good)

- **Type:** Behavior
- **Status:** done

E2E in `spaced_repetition.feature` now assimilates → just-review Yes → more recall Yes and pins Stability **55** / **55h**. Seed still only used by the incorrect-just-review scenario. Unit 24→25 pin still passes.

**Learning:** product schedule already clamped; this slice was Gherkin-only. `backend:test_only` can make E2E DevTools restart on a truncated classpath — if `/api/healthcheck` 404s, wait for a full class rebuild then touch a `.class` to reload.

### 2. Incorrect just-review after first Good uses Again from S0(Good)

- **Type:** Behavior
- **Status:** done

Unit pin `onTimeIncorrectRecallAfterFirstGoodUsesFsrsAgainFromS0AndD0Good`: Stability **15**, Difficulty **7.3945026f**, due **15h**. E2E: first Good then `It's day 3, 15 hour` + `I visit recall` (must not reset to hour 8) → No; display Difficulty **7.3945**. Seed has no remaining feature callers. Graded D=5 Again pin 8.341763f kept.

### 3. Drop graded-tracker seed and dead mark-successfully helper

- **Type:** Structure
- **Status:** planned

Enables nothing further — leftover from slices 1–2. Stop-safe: unused testability only.

Remove `POST /api/testability/seed_graded_memory_tracker`, `SeedGradedMemoryTrackerWorker`, E2E `seedGradedUnderstandingTracker` / “graded at stability {int}” step, generated client for that operation (`pnpm generateTypeScript`). Revert `MemoryTrackerService.persistRecallLog` to package-private (it was widened only for the worker’s unused GOOD log).

Delete unused `markUnderstandingTrackerRecalledSuccessfully` and the “I marked the understanding tracker … as recalled successfully” step (no feature callers after 008).

**Done when:** no seed endpoint; `persistRecallLog` not public; targeted backend tests and `spaced_repetition.feature` still pass.

### 4. One commissioned first-score E2E outline

- **Type:** Structure
- **Status:** planned

Three copy-paste first-score scenarios (4 / 5 / 3) share the same path. Collapse to a **Scenario Outline**. Include Stability, Difficulty, and hours for Good as well as Hard/Easy (score 4 today only asserts Difficulty 2.1181).

Do not drop `LearningSessionRecordTutorFeedbackTests` first-score float pins.

**Done when:** one outline covers first 3/4/5; `commissioned_learning_session.feature` still passes.

## Tests (capability-owned)

| Capability | Where |
|---|---|
| Same-hour after first Good | `spaced_repetition.feature`; unit 24→25 stays in `SpacedRepetitionCorrectRecallSchedulingTest` |
| Again after first Good | `SpacedRepetitionIncorrectRecallSchedulingTest`, `spaced_repetition.feature` |
| Commissioned first 3/4/5 | `commissioned_learning_session.feature` outline; controller float pins unchanged |

No product types named after this plan number.
