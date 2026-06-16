# OpenAI Batch Question Generation

Status: planned

## Refined Requirement

Use the OpenAI Batch API to pre-generate recall questions at lower cost. The backend runs a prod-only hourly cron, but for any one user it submits at most one successfully accepted OpenAI batch every 23 hours. Each accepted batch should generate questions for active memory trackers due in the next 48 hours. Existing synchronous on-demand generation remains unchanged and continues to serve as fallback when no pre-generated question exists.

Eligibility and timing:

- A user is eligible only when they have at least one answered recall prompt in the last 7 days, using `quiz_answer.created_at`.
- Do not rely on user timezone. Treat `quiz_answer.created_at` as instants on one rolling 24-hour clock.
- The "silent period of the day" is the longest circular time-of-day span in that rolling 24-hour clock with no answered recalls.
- The target submission time is one hour after the beginning of that longest silent span.
- Because the cron runs hourly, the user is due when the current time-of-day falls in the target hour. The implementation may choose an inclusive/exclusive boundary, but it should be deterministic and covered by tests.
- A user with only one answered recall in the last 7 days is still eligible.
- The 23-hour gate is based on the last successful OpenAI batch submission, meaning OpenAI accepted the batch and returned a batch id. Failed local attempts and failed/expired OpenAI batches do not update this gate.
- Failed or expired batches may be retried on the next hourly cron if the user is otherwise due.

Candidate memory trackers:

- Include active trackers with `next_recall_at <= now + 48h`.
- Exclude deleted trackers, removed trackers, spelling trackers, and trackers that already have an unanswered non-contested prompt at submission time.
- Do not do an import-time duplicate check. Extra prompts are acceptable and can be used later.

Batch generation:

- Use OpenAI Batch API JSONL requests targeting Responses API.
- Persist enough state to survive backend restart: local batch rows, request rows, OpenAI batch id, custom ids, memory tracker ids, context seeds, statuses, timestamps, and row-level errors.
- Use `custom_id` to correlate results; batch output order is not guaranteed.
- Skip contest/evaluation/regeneration for batch-generated questions.
- No per-user/per-run cap for now.
- Do not change note-content-update behavior.
- Do not change synchronous recall/question generation behavior.

## Existing Paths

- Synchronous question generation: `backend/src/main/java/com/odde/doughnut/services/RecallQuestionService.java`
- Current feasible question path and contest loop to bypass for batch: `backend/src/main/java/com/odde/doughnut/services/PredefinedQuestionService.java`
- Responses request construction: `backend/src/main/java/com/odde/doughnut/services/QuestionGenerationRequestBuilder.java`
- OpenAI SDK wrapper: `backend/src/main/java/com/odde/doughnut/services/openAiApis/OpenAiApiHandler.java`
- Due tracker selection: `backend/src/main/java/com/odde/doughnut/entities/repositories/MemoryTrackerRepository.java`
- Answered recall records and unanswered prompt query: `backend/src/main/java/com/odde/doughnut/entities/repositories/RecallPromptRepository.java`
- Existing prod scheduled job example: `backend/src/main/java/com/odde/doughnut/services/EmbeddingMaintenanceJob.java`

## Phase 1: Store Successful Submission Gate

Status: done

Type: Structure

Precondition: no durable per-user batch-submission marker exists.

Trigger: add schema and persistence support for a user's last successful batch submission.

Postcondition: backend can record and query the last accepted OpenAI batch submission time for a user, with no change to runtime behavior.

Why structure: this is the smallest durable piece needed immediately by Phase 2's user eligibility behavior.

Implementation notes:

- Add a Flyway migration for a capability-named table such as `question_generation_batch_user_state`, or include an equivalent durable per-user state table.
- Store user id and `last_successful_submitted_at`.
- Prefer a unique user id constraint.

Tests:

- Repository/database test can create/update/read the user's last successful submission time.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Phase 2: Enforce 23-Hour User Submission Gate

Status: done

Type: Behavior

Precondition: a user's last successful batch submission can be persisted.

Trigger: invoke the batch planning service for a user at a current timestamp.

Postcondition: the service reports the user eligible when no successful submission exists or it is at least 23 hours old, and suppressed when the last successful submission is newer than 23 hours.

Implementation notes:

- Add a narrow service method that answers whether a user is past the 23-hour gate.
- Do not submit to OpenAI yet.
- Failed/expired batch statuses are intentionally irrelevant here; only the successful submission marker matters.

Tests:

- Backend service test for no marker, 22h59m old marker, exactly/just over 23h marker.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Phase 3: Find Users With Recent Recall Activity

Status: done

Type: Behavior

Precondition: the 23-hour gate exists.

Trigger: invoke batch user selection with a current timestamp.

Postcondition: only users with at least one `quiz_answer.created_at` in the last 7 days are returned as candidates, subject to the 23-hour gate.

Implementation notes:

- Add repository query rooted in answered recall prompts joined through `memory_tracker.user_id`.
- Use `[now - 7d, now)` or another clearly tested boundary.
- A user with one recall in the last 7 days qualifies.

Tests:

- Backend repository/service tests for no recall, one recent recall, old recall, and suppressed by recent successful submission.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Phase 4: Compute Rolling 24-Hour Silent Target

Type: Behavior

Precondition: users with recent recall activity can be selected.

Trigger: pass a user's recent answer timestamps into a pure silent-period selector.

Postcondition: the selector returns the target time-of-day one hour after the beginning of the longest circular no-recall span, with deterministic tie behavior.

Implementation notes:

- Keep this as a pure algorithm with timestamp/time-of-day inputs and outputs.
- No timezone conversion.
- Define tie behavior before coding, for example choose the earliest target time-of-day.
- For one recall timestamp, the longest silent span begins immediately after that recall's time-of-day; the target is one hour later.

Tests:

- Unit tests for one recall, multiple recalls, span crossing midnight, evenly spaced recalls, boundary times, and ties.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Phase 5: Select Users Due In The Current Cron Hour

Type: Behavior

Precondition: candidate users and target time-of-day can be computed.

Trigger: invoke batch user selection for an hourly cron timestamp.

Postcondition: the service returns users whose target time-of-day falls in the current cron hour and excludes users outside that hour.

Implementation notes:

- Use the Phase 4 algorithm over each user's last 7 days of `quiz_answer.created_at`.
- Keep the cron-hour boundary deterministic and tested.
- Continue applying the 23-hour gate.

Tests:

- Backend service tests for target within current hour, target in adjacent hour, and target crossing midnight.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Phase 6: Select Candidate Memory Trackers

Type: Behavior

Precondition: a user can be selected as due for batch generation.

Trigger: ask for that user's trackers at a current timestamp.

Postcondition: the backend returns active non-spelling memory trackers due by `now + 48h`, excluding trackers with existing unanswered non-contested prompts.

Implementation notes:

- Add or extend repository query for candidate trackers.
- Reuse the existing unanswered non-contested semantics from `RecallPromptRepository.findUnansweredByMemoryTracker`.
- Keep the query readable even if it needs a native SQL `NOT EXISTS`.

Tests:

- Backend repository/service tests for due/not due, removed, deleted, spelling, answered prompt, unanswered contested prompt, and unanswered non-contested prompt.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Phase 7: Make Question Request Building Work For Background User

Type: Structure

Precondition: candidate trackers can be selected, but request construction currently relies on `AuthorizationService.getCurrentUser()` in focus-context/wiki-title paths.

Trigger: refactor request construction so the caller can pass the viewer user explicitly for background generation.

Postcondition: existing synchronous request construction behaves the same, and a background caller can build the same structured Responses request for a note/tracker without a session current user.

Why structure: this directly enables Phase 8's batch JSONL request creation.

Implementation notes:

- Keep existing public methods for synchronous callers.
- Add overloads or a small context parameter that carries the viewer user.
- Avoid broad authorization redesign.

Tests:

- Existing question-generation request builder tests still pass.
- Add a backend test proving explicit viewer user is used for wiki-title/focus-context needs without setting session current user.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Phase 8: Persist Batch And Request Rows Before Submission

Type: Behavior

Precondition: due users and candidate trackers can be found, and background request building works.

Trigger: plan a local batch for one due user.

Postcondition: the backend creates one local batch row and one request row per candidate tracker, including custom id and context seed, but does not call OpenAI yet.

Implementation notes:

- Add batch/request tables if not already created in Phase 1; keep names capability-based, for example `question_generation_batch` and `question_generation_batch_request`.
- Store local batch status such as `PLANNED`.
- Use custom ids stable enough for OpenAI result correlation and local idempotency.
- It is acceptable for this phase to stop with planned local work only.

Tests:

- Backend service test verifies planned batch/request rows for selected trackers.
- Test no local batch is created when the user has no candidate trackers.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Phase 9: Build Batch JSONL Payload

Type: Behavior

Precondition: local batch/request rows exist for candidate trackers.

Trigger: ask the service to render the OpenAI Batch JSONL input for a local planned batch.

Postcondition: each request row becomes one valid JSONL line targeting Responses API with the row's `custom_id` and a body equivalent to synchronous structured question generation, without contest/evaluation requests.

Implementation notes:

- Keep JSONL rendering deterministic and unit-testable.
- Use the Responses endpoint expected by the current OpenAI Batch docs.
- Do not include note content in logs or test failure messages beyond controlled fixtures.

Tests:

- Unit or backend service test parses rendered JSONL and verifies one line per request, matching custom ids, endpoint/method/body shape, model, and structured output configuration.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Phase 10: Submit Planned Batch To OpenAI

Type: Behavior

Precondition: JSONL can be rendered for a planned local batch.

Trigger: submit a planned local batch.

Postcondition: the backend uploads the JSONL file, creates an OpenAI batch, saves the OpenAI batch id and submitted status, and updates the user's last successful submission time only after OpenAI accepts the batch.

Implementation notes:

- Extend `OpenAiApiHandler` with the minimum Batch API calls: upload file with purpose `batch`, create batch with endpoint `/v1/responses` and `completion_window` `24h`.
- Persist OpenAI ids/statuses before returning success.
- If upload or batch creation fails, mark local batch failed but do not update the successful-submission marker.

Tests:

- Backend test with mocked OpenAI handler verifies accepted submission updates local batch and 23-hour marker.
- Failure test verifies local failure state and no marker update.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Phase 11: Submit Due Users End To End

Type: Behavior

Precondition: due users can be selected and planned local batches can be submitted.

Trigger: invoke the submission service with a current timestamp.

Postcondition: for each due user, the backend selects candidate trackers, plans rows, submits one OpenAI batch when there are candidates, skips users without candidates, and continues after per-user failures.

Implementation notes:

- Keep this callable directly; do not wire the scheduler yet.
- Use transactional boundaries so a failure for one user does not corrupt another user's work.
- Log counts and batch ids without note content.

Tests:

- Backend service test with two users: one succeeds, one fails, and the successful user's marker is updated.
- Test due user with no candidate trackers does not submit and does not update marker.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Phase 12: Poll OpenAI Batch Status

Type: Behavior

Precondition: submitted local batches with OpenAI batch ids exist.

Trigger: invoke batch polling.

Postcondition: local batch rows update from OpenAI status, including terminal failed/expired states, without importing output yet.

Implementation notes:

- Extend `OpenAiApiHandler` with retrieve-batch support.
- Only poll local batches in nonterminal statuses.
- Failed/expired statuses should leave the user eligible for a later retry because they do not update the successful-submission marker beyond the original accepted submission gate. If this conflicts with "retry next hourly cron", prefer a separate retry eligibility check that allows failed/expired batches to be retried once the current hour is due, without creating duplicate in-progress work.

Tests:

- Backend tests for in-progress, completed, failed, and expired status updates.
- Test already terminal local batches are not polled again.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Phase 13: Download And Store Batch Output Metadata

Type: Behavior

Precondition: a local batch is marked completed and OpenAI exposes output/error files.

Trigger: invoke output collection for completed batches.

Postcondition: the backend downloads output/error JSONL content or stores enough local metadata for import, and marks request rows with raw success/error payloads by `custom_id`.

Implementation notes:

- Extend `OpenAiApiHandler` with file-content download.
- Parse by `custom_id`; output order is not guaranteed.
- Do not create `PredefinedQuestion` or `RecallPrompt` yet.
- Row-level parse failures should mark only the affected row failed.

Tests:

- Backend tests for unordered output lines mapping to request rows.
- Tests for error-file rows and malformed/missing `custom_id`.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Phase 14: Import One Successful Batch Result Row

Type: Behavior

Precondition: a request row has a successful raw Responses result payload.

Trigger: invoke row import.

Postcondition: the backend creates one `PredefinedQuestion` and one MCQ `RecallPrompt` for the row's memory tracker, marks the row imported, and skips contest/evaluation/regeneration.

Implementation notes:

- Convert structured Responses output into `MCQWithAnswer`.
- Save the same domain entities synchronous generation would use.
- Keep row import idempotent: already imported rows are skipped.

Tests:

- Backend test verifies created `PredefinedQuestion` and `RecallPrompt` contents.
- Test idempotent re-import does not create duplicates for the same request row.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Phase 15: Import Completed Batches End To End

Type: Behavior

Precondition: completed local batches have output/error rows available.

Trigger: invoke completed-batch import.

Postcondition: all importable rows create prompts, failed rows are marked failed, one bad row does not block the rest, and the local batch becomes imported when every row is terminal.

Implementation notes:

- Keep retry/re-run safe.
- Import-time duplicate prompt checks are intentionally omitted.
- Preserve enough failure detail to inspect stuck rows.

Tests:

- Backend service test with mixed successful, failed, malformed, and already-imported rows.
- Test batch-level imported status after all rows are terminal.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Phase 16: Resume Work After Backend Restart

Type: Behavior

Precondition: persisted submitted/completed/importable batch rows exist from before service startup.

Trigger: invoke the maintenance service after constructing fresh service instances.

Postcondition: existing nonterminal batches are polled and completed batches are imported using only persisted state and OpenAI ids/files.

Implementation notes:

- This may be mostly an integration test over Phases 12-15.
- Avoid relying on in-memory queues.

Tests:

- Backend test seeds persisted batch/request rows directly, invokes maintenance, and verifies polling/import completes.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Phase 17: Wire The Hourly Prod Job

Type: Behavior

Precondition: direct service invocation can submit due users and resume/import existing batches.

Trigger: prod scheduled job runs hourly.

Postcondition: production backend automatically polls/imports existing work and submits new due-user batches without changing synchronous recall behavior.

Implementation notes:

- Add a prod-only scheduled service similar to `EmbeddingMaintenanceJob`.
- Prefer running resume/import before new submissions so completed work is visible as soon as possible.
- Keep per-user/per-batch failures isolated.

Tests:

- Backend test verifies scheduled job delegates to maintenance service.
- Existing recall controller/service tests continue passing.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Phase 18: Operational Notes And Verification

Type: Behavior

Precondition: hourly job is wired.

Trigger: final cleanup and documentation.

Postcondition: operational notes explain state tables, retry behavior, restart behavior, and stuck-batch inspection; backend verification is clean.

Implementation notes:

- Regenerate database ERD if migrations changed schema.
- Keep generated API docs untouched unless an endpoint was added.
- Remove dead/interim code.

Tests:

- Run `CURSOR_DEV=true nix develop -c pnpm backend:verify`.
- If migrations changed schema, run `CURSOR_DEV=true nix develop -c pnpm export:database-erd`.

## Open Items

None blocking.
