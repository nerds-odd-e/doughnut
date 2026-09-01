# Question generation: OpenAI FAILED is loud

**Status:** in progress (slices 1–2 done)

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
the next hourly poll gets OpenAI status `FAILED` (file-access). Slice 1
persists that `FAILED` then throws so the hourly job writes a Failure
report.

Intended sequence for that status:

1. Save local `FAILED` (and request `error_detail`) so retry/eligibility
   still work.
2. Throw with OpenAI’s error text (e.g. “Cannot find file …”) so the hourly
   job’s resume `catch` writes a Failure report (already wired in
   `QuestionGenerationBatchMaintenanceJob`). Consecutive similar hourly
   throws stay one report with a count (ADR 0006).

## Remaining gap

- Submit-time OpenAI exceptions still **return false** after the committed
  `FAILED` row. Slice 3 throws instead so the hourly job writes a Failure
  report. The nested persist from slice 2 keeps that row.

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

### 1. Persist OpenAI FAILED then throw from poll — Behavior — done

Poller persists OpenAI `FAILED`/`CANCELLED` as local `FAILED`, then throws
one exception (process-all-then-throw). Message uses OpenAI `errors` text
when present, else `openai batch failed`, plus batch id. `EXPIRED` still
persists without throwing.

**Learning:** `Batch.errors()` stayed a small helper on the poller. Status
tests that grew past 250 lines were split into
`QuestionGenerationBatchPollingScopeTest`.

### 2. Commit submit-time FAILED outside the user tx — Structure — done

Submit-time `FAILED` is persisted in `QuestionGenerationBatchSubmissionFailureTx`
(`REQUIRES_NEW`). Planning is committed first via
`planAndCommitLocalBatchForUser` so the nested failure persist can update
those rows (MySQL lock). `submitPlannedBatch` still returns `false`.

**Learning:** Nested `REQUIRES_NEW` cannot update rows still held by the
user tx; the planned insert must commit before upload/create.

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

