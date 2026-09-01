# Question generation: OpenAI FAILED is loud

**Status:** planned (do not execute until asked)

## Goal

When OpenAI marks a question-generation batch `FAILED`, Donut **persists**
local `FAILED` (batch and pending requests), **then throws** with an
understandable message so the existing scheduled-job path writes a
**Failure report**.

No ADR change. This follows Accepted
[ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md): persist is
the handled business outcome; wrapping the OpenAI error and throwing is
enrich + propagate so developers see it.

## ADR 0006 vs this intent

The ADR is already the right rule. It does **not** say “never catch.” It
says catch only for a deliberate outcome, then prefer persist/prevent,
then propagate, and wrap when the raw failure would be unclear.

This outage is not an HTTP/SDK exception. `createResponsesBatch` succeeds;
the next hourly poll gets OpenAI status `FAILED` (file-access). Today that
path saves `FAILED` and **returns normally**, so no Failure report.

Intended sequence for that status:

1. Save local `FAILED` (and request `error_detail`) so retry/eligibility
   still work.
2. Throw with OpenAI’s error text (e.g. “Cannot find file …”) so the hourly
   job’s resume `catch` writes a Failure report (already wired in
   `QuestionGenerationBatchMaintenanceJob`). Consecutive similar hourly
   throws stay one report with a count (ADR 0006).

## Current gap (do not implement in planning)

- `QuestionGenerationBatchPollingService`: OpenAI `FAILED` / `CANCELLED` →
  persist, no throw.
- Throw today only if `retrieveBatch` **throws** (`57f1849c24`).
- Submit-time OpenAI exceptions save `FAILED` then **return false** inside
  `REQUIRES_NEW` (`QuestionGenerationBatchUserSubmissionTx`). Throwing from
  that same transaction would **roll back** the `FAILED` row. Out of this
  plan’s first slice (this outage never hits that path).

## Design

- After polling **all** `SUBMITTED` batches, if any mapped to local
  `FAILED`, throw **one** exception (same “process all, then throw”
  shape as retrieve failures). Persist every `FAILED` **before** the throw.
- Exception message includes OpenAI `errors` text when present, plus batch
  id; keep a generic fallback (`openai batch failed`) if OpenAI sent none.
- Hourly job already: resume throw → Failure report → still
  `submitDueUsers`. Do not change that order.
- `EXPIRED` stays persist-without-throw (not this outage; different
  meaning). `CANCELLED` already maps to local `FAILED` — same throw path.

## Slices

### 1. Persist OpenAI FAILED then throw from poll — Behavior — planned

**Pre-condition:** A local batch is `SUBMITTED`. OpenAI retrieve returns
status `FAILED` (optional `errors` with a human message, as in the file
access outage).

**Trigger:** Hourly resume polls submitted batches
(`QuestionGenerationBatchPollingService.pollSubmittedBatches`).

**Post-condition:**

- Local batch is `FAILED`; pending requests are `FAILED` (still saved if
  the poller then throws).
- `pollSubmittedBatches` throws; message contains the OpenAI error text
  when provided (otherwise the existing `openai batch failed` wording).
- Hourly job therefore writes a Failure report on resume (existing
  behavior; do not re-assert the job in a second test). Due-user submit
  still runs after resume error.

**Test:** Extend `QuestionGenerationBatchPollingServiceTest`
`failedUpdatesLocalBatch` (and add OpenAI `errors` on the stubbed `Batch`
when asserting the message). Drive the poller; mock only `OpenAiApiHandler`.
Existing retrieve-throw test stays.

**Docs:** One line in `docs/question-generation-batch-operations.md` that
OpenAI `FAILED` persists then fails loudly.

Stop-safe: this is the outage signal. Collect/import/prune in the **same**
resume call stay skipped when poll throws — same as today’s retrieve-throw
path. Next hour with no new `FAILED` continues collect.

### 2. Commit submit-time FAILED outside the user tx — Structure — planned

**What it changes:** Local `FAILED` from upload/create exceptions is
committed in a nested transaction that **survives** a later throw from
`processDueUser` / `submitPlannedBatch`. No new user-visible throw yet.

**What it enables:** Slice 3.

Existing submission tests stay green (still `FAILED` + `return false`).

### 3. Submit-time OpenAI exception throws after FAILED is committed — Behavior — planned

**Pre-condition:** Due-user submit; OpenAI upload or `createResponsesBatch`
throws.

**Trigger:** `submitPlannedBatch` / hourly due-user submit.

**Post-condition:** Local `FAILED` remains after the throw; the exception
reaches the hourly job (Failure report via scheduler `ErrorHandler`, which
already persists uncaught submit throws).

**Test:** Extend `QuestionGenerationBatchSubmissionServiceTest` (persist
survives the throw). Only then stop returning `false` as the sole outcome.

## Out of scope

- Changing ADR 0006.
- OpenAI platform file-access bug / new API key / project rotation.
- `EXPIRED` loud failure.
- Starving collect during an ongoing FAILED outage (accepted; same as
  retrieve-throw).
- New E2E (no OpenAI Batch in Cypress).

## Jidoka

If extracting `Batch.errors()` needs more than a small helper on the
existing poller/test stub, stop and split that helper as Structure before
retrying slice 1.
