# Batch question generation — functional, catch-up trigger

Make the per-user batch question generation trigger a **function of recall data**, not of saved
submission state, and make a **missed silent-window trigger catch up** on the next hourly run
instead of waiting a full day.

## Current behavior (as built)

Hourly cron `QuestionGenerationBatchMaintenanceJob.runHourlyMaintenance` (minute 0) →
`submitDueUsers` → `QuestionGenerationBatchPlanningService.findUsersEligibleForBatchSubmission`.

A user is submitted when **all** hold:
1. Has an answered recall in the last 7 days.
2. `isUserEligibleForNewBatchSubmission`: no in-flight `SUBMITTED` batch **AND**
   (`isUserPastSubmissionGate` **OR** OpenAI-failure retry path).
3. `isUserEligibleViaOpenAiFailureRetryPath` **OR** `isUserDueInCurrentCronHour`.

Two mechanisms decide "now":
- **Functional (good):** `isUserDueInCurrentCronHour` derives a target time-of-day `T` = one hour
  after the start of the user's longest no-recall window (`RecallSilentPeriodTargetSelector`), then
  checks whether `T` lands inside the **current cron hour** via `CronHourTargetDueSelector`.
- **Saved state (to remove):** `isUserPastSubmissionGate` reads
  `QuestionGenerationBatchUserState.lastSuccessfulSubmittedAt` and enforces a 23h gate
  (`SUBMISSION_GATE_MILLIS`).

The same eligibility predicate also feeds the user-facing schedule
(`getNextBatchQuestionSchedule`, served by `UserController` `/question-generation-batch-schedule`)
and the admin manual path (`findUsersEligibleForManualBatchSubmission`).

## Gaps and bugs

1. **Missed silent-window is never caught up (primary bug).** `isUserDueInCurrentCronHour` is an
   **exact-hour equality** test. If the hourly run for the target hour does not pick the user up
   (deploy/restart, cron skew, the earlier `submitDueUsers` step threw, no candidate trackers that
   hour, or `T` shifts by an hour day-to-day as the rolling 7-day window changes), the user is
   skipped and waits a **full day** for the next match.

2. **The 23h gate compounds the delay.** `getNextBatchQuestionSchedule` test
   `delaysUntilSubmissionGateAndTargetCronHourBothMatch` encodes the buggy interaction: when the
   gate clears *after* today's target hour has passed, the next fire is pushed to the **next day's**
   target hour rather than catching up the same day.

3. **Saved state duplicates the batch records.** `QuestionGenerationBatchUserState`
   `lastSuccessfulSubmittedAt` is written only in `QuestionGenerationBatchSubmissionService` and
   equals `MAX(QuestionGenerationBatch.submittedAt)` for the user. Two representations of one
   concept ("when did this user last get a batch"), against the one-representation rule.

## Target design

Concept: **due instant** = the most recent wall-clock occurrence of the user's silent-period target
time-of-day `T`, at or before `now` (within the last 24h). Computed purely from recall data.

```
T          = targetTimeOfDayFromTimestamps(answeredRecalls within 7d)   // existing
dueInstant = most recent occurrence of T at or before now               // new pure helper
```

A user is **overdue** for a new batch iff they have recent recall activity **and** no batch has been
submitted since `dueInstant` (`lastSubmission == null || lastSubmission < dueInstant`).

Eligibility to submit now = no in-flight `SUBMITTED` batch **AND** (`overdue` **OR**
`openAiFailureRetry`), where `openAiFailureRetry` = a batch submitted since `dueInstant` ended in an
OpenAI failure (reframed `isUserEligibleViaOpenAiFailureRetryPath`).

This single rule:
- is functional over recall data + batch records (no separate user-state row),
- self-heals missed windows (a later hourly run still sees `lastSubmission < dueInstant` and fires),
- prevents same-day double submission (after submitting past `dueInstant`, not overdue until next `T`),
- makes both `SUBMISSION_GATE_MILLIS` (23h gate) and exact-hour `CronHourTargetDueSelector`
  obsolete.

Schedule (`getNextBatchQuestionSchedule`):
- in-flight `SUBMITTED` → `BATCH_IN_PROGRESS`;
- no recent recall → `NO_RECENT_RECALLS`;
- overdue now (and candidates exist) → next top-of-hour;
- otherwise → next occurrence of `T` after `now` where candidates exist.

### Design decisions

1. Deliver the **catch-up bug fix first** (highest user value), keeping the saved
   `lastSuccessfulSubmittedAt` read in Phase 1; remove the saved-state table in Phase 2. Phase 1
   already makes the *trigger timing* a function of recall data; Phase 2 makes *last submission*
   functional too.
2. **Submit, schedule, and manual share one eligibility predicate**, so they migrate together in
   Phase 1 to avoid an interim where generation catches up but the displayed schedule does not.
3. **Manual admin path** (`submitRecentRecallUsers`) keeps its "force now regardless of daily
   timing" intent: eligibility = recent recall + no in-flight `SUBMITTED` batch. The 23h dedup is
   dropped for manual (the in-flight guard suffices; admin-initiated). Update
   `QuestionGenerationBatchManualEligibilityTest` accordingly.
4. The "most recent occurrence of `T` at or before `now`" is **pure date logic** — implement as a
   small pure helper with direct unit tests, in the same time basis as `RecallTimeOfDay` (no new
   timezone handling).

## Phases

### Phase 1 — Behavior: overdue catch-up triggering

Status: done

Behavior: When a user's silent-window target time has passed and no batch has been submitted since
that time, the next hourly run generates a batch — even if the exact target hour was missed — and
the user-facing schedule reflects the same catch-up. A user who already received a batch since the
last target time is not re-triggered until the next target time.

Implementation:
- Add a pure helper (e.g. `RecallSilentWindowDueInstant.lastDueInstantAtOrBefore(LocalTime target,
  LocalDateTime now)`) returning the most recent `T` occurrence at or before `now`.
- In `QuestionGenerationBatchPlanningService`, replace the exact-hour `isUserDueInCurrentCronHour`
  usage and the 23h `isUserPastSubmissionGate` gate with the **overdue** rule (still reading
  `lastSuccessfulSubmittedAt` as the "last submission" source this phase).
- Reframe `isUserEligibleViaOpenAiFailureRetryPath` in terms of "submitted since `dueInstant` and
  failed at OpenAI".
- Update `getNextBatchQuestionSchedule` to the schedule rule above.
- Update `findUsersEligibleForManualBatchSubmission` to: recent recall + no in-flight `SUBMITTED`.
- Remove now-unused `CronHourTargetDueSelector`, `SUBMISSION_GATE_MILLIS`, and the exact-hour
  branch; keep `RecallSilentPeriodTargetSelector`.

Tests:
- New direct unit tests for the due-instant helper (today vs yesterday boundary, midnight wrap,
  exactly-now).
- Rename/repurpose `QuestionGenerationBatchCronHourEligibilityTest` →
  overdue eligibility: add "target passed 2h ago, no submission since, run now (not the exact hour)
  → due", and "batch submitted after dueInstant → not due".
- Update `QuestionGenerationBatchUserScheduleTest`: `delaysUntilSubmissionGateAndTargetCronHourBothMatch`
  now expects same-day catch-up (next top-of-hour) instead of next-day.
- Update `QuestionGenerationBatchManualEligibilityTest` for the dropped 23h dedup.
- Keep `QuestionGenerationBatchSubmitDueUsersTest` green (still asserts the saved marker this phase).

Targeted checks:
- `CURSOR_DEV=true nix develop -c pnpm backend:test_only`

Notes:
- No Cypress: this is a server-side cron/scheduling contract; the stable boundary is
  `QuestionGenerationBatchPlanningService` (`findUsersEligibleForBatchSubmission`,
  `getNextBatchQuestionSchedule`) plus the `submitDueUsers` integration test, matching existing
  test structure.

### Phase 2 — Structure: derive last submission functionally; remove the saved-state table

Status: planned

Behavior: Externally unchanged (the derived "last submission" equals the previously saved value).
Existing Phase 1 tests still pass.

Implementation:
- Add `QuestionGenerationBatchRepository` query for the user's latest submitted-at
  (`MAX(b.submittedAt)`), returning empty when none.
- Replace all reads of `lastSuccessfulSubmittedAt` with that query in the overdue / retry logic.
- Delete `QuestionGenerationBatchUserState`, its repository, and the write in
  `QuestionGenerationBatchSubmissionService.recordSuccessfulSubmission`.
- Add a Flyway migration dropping `question_generation_batch_user_state` (see `db-migration.mdc`).
- Update tests that referenced the user-state entity/repository
  (`QuestionGenerationBatchSubmitDueUsersTest`, `...UserScheduleTest`,
  `...UserStateRepositoryTest`, `...SubmissionServiceTest`, `...RetryEligibilityTest`,
  `QuestionGenerationBatchSubmitDueUsersTestBase`) to assert against batch records instead.

Targeted checks:
- `CURSOR_DEV=true nix develop -c pnpm backend:verify` (migration involved)

## Out of scope

- Changing the silent-window target-time algorithm itself (`RecallSilentPeriodTargetSelector`).
- Changing candidate-tracker selection (48h `findBatchQuestionGenerationCandidatesByUser`).
- Timezone reform of `RecallTimeOfDay`/`Timestamp` handling.
- Frontend changes beyond what the schedule DTO already drives.
