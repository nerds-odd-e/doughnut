# Question generation batch: latest-only OpenAI retry + failed-request purge

**Status:** complete
**Goal:** Stop hourly re-submit when an older OpenAI `FAILED`/`EXPIRED` batch still exists, then drop current `FAILED` request rows in production so twice-failed trackers can be queued again without waiting 30 days.

## Outcome (shipped)

- `QuestionGenerationBatchPlanningService.isUserEligibleViaOpenAiFailureRetryPath` now consults only the user's latest-accepted batch (`QuestionGenerationBatchRepository.findFirstByUser_IdAndSubmittedAtIsNotNullOrderBySubmittedAtDescIdDesc`), replacing the old "any historical batch with a terminal OpenAI status" check that kept unlocking hourly retry for up to 30 days after a later submission had already succeeded. `isUserOverdueForBatch` was consolidated onto the same lookup, removing a duplicate query path.
- Flyway migration `V300000318__purge_failed_question_generation_batch_requests.sql` unconditionally deletes `question_generation_batch_request` rows with `status = 'FAILED'` (parent batch rows and non-`FAILED` request rows untouched). Started as a gated migration (placeholder `question_generation_batch_failed_request_purge`, default `1=0`, production-only `1=1`); at explicit developer instruction the gate was dropped entirely — the placeholder was removed from the SQL and from all `spring.flyway.placeholders` blocks (`application.yml` ×3, `application-prod.yml`), and the migration-only test `QuestionGenerationBatchFailedRequestPurgeMigrationTest` was deleted.
- The sibling migration `V300000306__purge_incomplete_question_generation_batches.sql` (pre-existing, from before this plan) got the same treatment at explicit developer instruction: its `question_generation_batch_incomplete_purge` gate was removed from the SQL and from every placeholder block (including dropping the `QUESTION_GENERATION_BATCH_INCOMPLETE_PURGE` env-var override in `application-prod.yml`), and its migration-only test `QuestionGenerationBatchIncompletePurgeMigrationTest` was deleted. This was explicitly out of scope in this plan's original design but was pulled in by direct instruction.
- Editing already-committed migration SQL is normally against repo policy, but is safe here specifically because both test and non-test startup call `flyway.repair()` before `migrate()` (`FlyWayTestMigrationStrategyConfig`, `FlyWayFreeVersionRealMigration`) — repair realigns the stored checksum to the new resolved SQL on next startup without re-running an already-applied migration; a fresh/empty database just runs the (now unconditional) delete once, which is a no-op there.
- Docs retry paragraph in `docs/question-generation-batch-operations.md` updated to describe the latest-accepted-batch semantics.

## Not done / follow-up

- Docs one-time-purge note beside `V300000306` was not added — not asked for.
