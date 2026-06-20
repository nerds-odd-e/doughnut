# Question Generation Batch Operations

Production-only hourly job that pre-generates recall questions via the OpenAI Batch API. Synchronous on-demand generation is unchanged and still serves as fallback when no pre-generated prompt exists.

## Scheduler

`QuestionGenerationBatchMaintenanceJob` runs every hour at minute 0 (`@Profile("prod")`, cron `0 0 * * * *`). Each run:

1. **Resume existing work** — poll submitted batches, collect output for completed batches, import rows into recall prompts.
2. **Submit new batches** — for users due in the current cron hour, plan and submit at most one OpenAI batch.

Resume runs before submission so completed work becomes visible as soon as possible.

### Multi-instance safety

Production may run up to two backend instances (autoscaler `max=2`). The hourly job uses [ShedLock](https://github.com/lukas-krecan/ShedLock) (`@SchedulerLock` on `runHourlyMaintenance`, JDBC lock table `shedlock`) so only one instance executes maintenance per hour. Manual admin **"Resume existing batches"** is unchanged and not ShedLock-guarded.

## State Tables

See `docs/database-erd.md` for foreign keys and column types.

| Table | Purpose |
|-------|---------|
| `question_generation_batch` | One local batch per submission attempt. Tracks OpenAI file/batch ids, batch-level status, lifecycle timestamps, and the per-user **23-hour gate** via `submitted_at` (set when OpenAI accepts a batch). |
| `question_generation_batch_request` | One row per memory tracker in a batch. Correlates OpenAI results via `custom_id` (`qgb-{batchId}-mt-{trackerId}`). Stores raw success/error payloads and row-level `error_detail`. |
| `question_generation_batch_maintenance_run` | Durable record of each scheduled or manual maintenance run (start/finish timestamps, trigger type). Survives backend restart. |

### Batch status (`question_generation_batch.status`)

| Status | Meaning |
|--------|---------|
| `PLANNED` | Local rows created; not yet accepted by OpenAI. |
| `SUBMITTED` | OpenAI accepted the batch; polling continues until terminal. |
| `COMPLETED` | OpenAI finished successfully; output collection and import follow. |
| `FAILED` | OpenAI reported failed/cancelled, or local upload/create failed before acceptance. |
| `EXPIRED` | OpenAI batch expired (24h completion window). |

Batch-level terminal states are `COMPLETED`, `FAILED`, and `EXPIRED`. A completed batch with `imported_at` set has finished the full pipeline.

### Request status (`question_generation_batch_request.status`)

| Status | Meaning |
|--------|---------|
| `PENDING` | Awaiting OpenAI output lines. |
| `OUTPUT_READY` | Success payload stored; ready for import. |
| `FAILED` | OpenAI or import error; see `error_detail` and raw payloads. |
| `IMPORTED` | `PredefinedQuestion` and `RecallPrompt` created for this tracker. |

## Retry Behavior

- **23-hour gate:** A user gets at most one *accepted* OpenAI submission per 23 hours. The gate is derived from the latest `question_generation_batch.submitted_at` for that user, not batch terminal status.
- **Failed local submission** (`PLANNED` → `FAILED` before OpenAI acceptance): gate is **not** updated; user can retry on the next **target cron hour** (same once-per-day gate as first-time submissions).
- **OpenAI `FAILED` / `EXPIRED`:** Gate **was** updated at acceptance. User is blocked until 23 hours pass **unless** they have a batch with `openai_batch_id` set and status `FAILED` or `EXPIRED` — then they may submit again even inside the gate on the **next hourly cron**, bypassing the target cron-hour gate (retry path in `QuestionGenerationBatchPlanningService`).
- **In-flight work:** User is not eligible for a new submission while any batch has status `SUBMITTED`.
- **Row import:** Failed rows stay `FAILED`; other rows in the same batch can still import. Re-running maintenance is safe: already `IMPORTED` rows are skipped.

## Restart Behavior

No in-memory queues. After backend restart, the next hourly run (or any direct call to `QuestionGenerationBatchMaintenanceService.resumeExistingBatches`) continues from persisted state:

1. Poll batches with status `SUBMITTED` using stored `openai_batch_id`.
2. Download output/error files for `COMPLETED` batches where `output_collected_at` is null.
3. Import batches where status is `COMPLETED`, output is collected, and `imported_at` is null.

Per-user submission uses `REQUIRES_NEW` transactions so one user's failure does not roll back another's work.

## Inspecting Stuck Batches

Use production MySQL read access. Useful log prefixes: `QuestionGenerationBatchPollingService`, `QuestionGenerationBatchOutputCollectionService`, `QuestionGenerationBatchImportService`, `QuestionGenerationBatchSubmissionService`.

### Batches not finishing OpenAI processing

```sql
SELECT id, user_id, status, openai_batch_id, submitted_at
FROM question_generation_batch
WHERE status = 'SUBMITTED'
ORDER BY submitted_at;
```

If `submitted_at` is older than ~24 hours, check OpenAI batch status with the stored `openai_batch_id`. Polling errors appear in backend logs; the batch stays `SUBMITTED` until a poll maps a terminal OpenAI status.

### Completed but not imported

```sql
SELECT id, user_id, status, output_collected_at, imported_at
FROM question_generation_batch
WHERE status = 'COMPLETED'
  AND (output_collected_at IS NULL OR imported_at IS NULL);
```

### Row-level failures

```sql
SELECT r.id, r.batch_id, r.memory_tracker_id, r.custom_id, r.status, r.error_detail
FROM question_generation_batch_request r
JOIN question_generation_batch b ON b.id = r.batch_id
WHERE r.status = 'FAILED'
   OR (r.status = 'PENDING' AND b.output_collected_at IS NOT NULL)
ORDER BY r.batch_id, r.id;
```

`PENDING` after output collection means a missing output line (`error_detail`: `missing batch output line`). Inspect `raw_error_payload` / `raw_success_payload` for OpenAI response details.

### User blocked from new submissions

```sql
SELECT u.id AS user_id,
       latest.submitted_at AS last_submitted_at,
       (SELECT status FROM question_generation_batch
        WHERE user_id = u.id AND status = 'SUBMITTED' LIMIT 1) AS in_flight_status
FROM (
  SELECT user_id, MAX(submitted_at) AS submitted_at
  FROM question_generation_batch
  WHERE submitted_at IS NOT NULL
  GROUP BY user_id
) latest
JOIN user u ON u.id = latest.user_id
WHERE latest.submitted_at > NOW() - INTERVAL 23 HOUR;
```

## OpenAI Batch Request Compatibility

Batch JSONL uses a separate request shape from synchronous question generation (`buildQuestionGenerationResponseRequestForBatch`):

- **`reasoning.effort`** — included at **HIGH** when the configured question-generation model supports reasoning (`OpenAiModelCapabilities.supportsReasoningEffort`: `o1`, `o3`, `o4`, `gpt-5` prefixes); omitted for non-reasoning models such as `gpt-4.1-mini`.
- **Sync generation** uses the same model detection with **MEDIUM** effort when supported; no `reasoning` key otherwise.
- **`text.verbosity: medium`** — batch requests always use medium verbosity (non-reasoning batch models reject `low`); sync generation uses `low`.

Captured success JSONL from manual verification is at `backend/src/test/resources/openai-batch-fixtures/live_batch_success_line.json`, covered by `QuestionGenerationBatchOutputFixtureTest`.

### Timezone

Production JVM runs in **UTC** (`-Duser.timezone=UTC` and `TZ=UTC` in the instance startup script). Logs and scheduler timestamps use UTC.

The hourly cron (`0 0 * * * *`) and silent-window due-instant logic are **timezone-invariant** for whole-hour offsets: shifting the JVM timezone does not change which cron hours fire or how hourly due windows align.

Silent-period target time-of-day is computed from recall answer timestamps in the JVM timezone (`RecallTimeOfDay.fromTimestamp`); per-user timezones are intentionally ignored.

## Code Entry Points

| Concern | Class |
|---------|-------|
| Hourly prod job | `QuestionGenerationBatchMaintenanceJob` |
| Resume poll/collect/import | `QuestionGenerationBatchMaintenanceService` |
| Due-user selection and planning | `QuestionGenerationBatchPlanningService` |
| OpenAI submit | `QuestionGenerationBatchSubmissionService` |
