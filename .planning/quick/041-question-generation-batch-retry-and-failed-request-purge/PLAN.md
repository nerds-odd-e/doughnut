# Question generation batch: latest-only OpenAI retry + failed-request purge

**Status:** complete
**Goal:** Stop hourly re-submit when an older OpenAI `FAILED`/`EXPIRED` batch still exists, then drop current `FAILED` request rows in production so twice-failed trackers can be queued again without waiting 30 days.

## Outcome (shipped)

- `QuestionGenerationBatchPlanningService.isUserEligibleViaOpenAiFailureRetryPath` now consults only the user's latest-accepted batch (`QuestionGenerationBatchRepository.findFirstByUser_IdAndSubmittedAtIsNotNullOrderBySubmittedAtDescIdDesc`), replacing the old "any historical batch with a terminal OpenAI status" check that kept unlocking hourly retry for up to 30 days after a later submission had already succeeded. `isUserOverdueForBatch` was consolidated onto the same lookup, removing a duplicate query path.
- Gated Flyway migration `V300000318__purge_failed_question_generation_batch_requests.sql` deletes `question_generation_batch_request` rows with `status = 'FAILED'` (parent batch rows and non-`FAILED` request rows untouched), gated by placeholder `question_generation_batch_failed_request_purge` in all `spring.flyway.placeholders` blocks.
- Production gate enabled directly (`application-prod.yml`: `1=1`, no env-var toggle, by explicit developer instruction) so the next production deploy purges the stuck `FAILED` rows and unblocks twice-failed trackers for regeneration. Left permanently on since versioned Flyway migrations run at most once per database — no revert-after-apply step needed for this one, unlike the `question_generation_batch_incomplete_purge` pattern it followed.
- Docs retry paragraph in `docs/question-generation-batch-operations.md` updated to describe the latest-accepted-batch semantics.

## Not done / follow-up

- Docs one-time-purge note beside `V300000306`, and removing the migration-only test `QuestionGenerationBatchFailedRequestPurgeMigrationTest` (SQL stays), were left for after the production deploy has actually run and the drop is confirmed — not done as part of enabling the gate.
