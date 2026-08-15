# Difficulty display and 004 follow-up — context

Inspected commits `4fc1a4a9cf`–`af674bcc4c` (Difficulty persist, FSRS-6 Good next Stability, Difficulty update, first-correct init, E2E day lists). Proposed [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) stays **Proposed**.

**Language:** [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md) names **Stability** and **Retrievability**. FSRS **SInc** (stability increment) is citation jargon only. This plan says **next Stability** after a correct recall.

## Locked for this plan

- Show Difficulty **only** on the Memory Tracker Information card (same page as Stability). Do not add it to note-info stats or recently-recalled tables.
- Presentation matches Stability: the number as returned by the API, or **N/A** when unset (New / assimilate-only).
- New tracker still has Difficulty unset until first correct. Graded trackers should not stay unset after a correct recall.
- ADR Decision line “not part of the learner UI” is updated in the show slice. Do not Accept the ADR.
- Fail / confusion / commissioned stay on the ladder. First Stability = 12h stays parked.

## Findings (004)

### Bug — graded tracker with unset Difficulty stays unset

`ForgettingCurve` treats null Difficulty as 5 when computing next Stability, but `MemoryTracker.recalledSuccessfully` persists next-D only when `getDifficulty() != null` (or Stability = 0). A graded tracker that missed backfill (or a fixture with Stability > 0 and Difficulty null) keeps Difficulty null forever. Locked 004 rule was: unset Difficulty on a graded tracker is treated as 5. Once Difficulty is on the page, that hole shows as N/A on a tracker that already has a schedule.

### Redundant tests

- `RecallServiceWithSpacedRepetitionAlgorithmTest.OnTimeAndEarlyRecall` pins exact 315h / 361h after `markAsRecalled`. That is the same next-Stability rule already locked on `recalledSuccessfully` (canonical 266h at Difficulty 5, Stability 72h) plus early/on-time qualitative tests. `markAsRecalled` only increments `recallCount` then calls `recalledSuccessfully`. Keep the due-day grid in the same class (`whenThereIsOneRecalledNotesForUser`).
- `harderDifficultyGrowsStabilityLessOnCorrectRecall` re-asserts that next Stability is at least current Stability on both trackers. Unique claim is higher Difficulty → smaller next Stability; the growth floor is already locked by the canonical case.

### Missed smells (parked — not this plan)

- Thinking-time tweak still scales by `SpacedRepetitionAlgorithm.LEGACY_INDEX_STEP` (ladder leftover). No user-facing slice here; decoupling would be Structure with no immediate Behavior.
- Good next-D uses raw `w[4]` as Easy-init, not FSRS `D0(4)`. Difference is ~0.005 at Difficulty 8 because `w[7]` is 0.001 — not learner-visible.
- `FsrsGoodRecall.difficultyAfterGoodRecall` computes a Good ΔD that is always 0. Harmless tautology.
- SpringDoc `Mcq` property order (`questionStem` / `responseChoices`) shifts when `MemoryTracker` schema changes; `RobotsTests` can fail under `gradle --parallel`. Handle inside the expose slice (pin order if regen requires it), not as a dangling Structure.

### Not bugs

- Next-Stability math in days, persist whole hours; Good does not apply Hard `w[15]`.
- First success Difficulty 5, Stability 24h; E2E second interval 102h.
- Persist round-trip tests (Difficulty 7, assimilate-only null) stay useful; frontend will mock the API.

## Show Difficulty

`MemoryTracker.difficulty` is `@JsonIgnore`. `MemoryTrackerInformation.vue` already lists Stability. Frontend `makeMe.aMemoryTracker` has no `.difficulty()`. `GET` show returns the entity; no new endpoint.

Drive `MemoryTrackerPageView` (already asserts Type). Optional E2E line on the existing memory-tracker page visit (New → N/A). Do not add Difficulty to `MemoryTrackerLite` / due strip.
