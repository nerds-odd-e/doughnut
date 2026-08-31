# Scheduled job failure reports

**Status:** in progress (slices 1–2 done).
**Type:** ad-hoc plan (`.planning/quick/`)

## Goal

When a **backend scheduled job** fails, the same **Failure report** list
(and GitHub issue attempt) that HTTP 500s already use is written
**synchronously**, so this class of failure is not only a log line or the
Batch Questions maintenance-run `error` field.

Out of scope (explicit):

- Fixing the EntityManager / missing-transaction `remove` on prune
- Deduplicating bursts of similar reports
- Changing whether resume **continues** to due-user submission after a
  resume failure

## Why this gap exists

Failure reports are created only from `ControllerSetup` (`@ControllerAdvice`)
via `FailureReportFactory`, which requires `HttpServletRequest` and
`CurrentUserFetcher`. `@Scheduled` methods are not HTTP.

Prod jobs today:

| Job | Uncaught? | What happens |
|-----|-----------|----------------|
| `QuestionGenerationBatchMaintenanceJob` resume | **Caught** | `recordError` on the maintenance run, log, **continue** to submit |
| Same job, submit | Rethrown | `recordError`, then throw to the scheduler (log only) |
| `EmbeddingMaintenanceJob` | Uncaught | Scheduler log only |

The EntityManager prune failure is on the **resume** path, so a
scheduler-only error handler would **miss** the failure we actually saw.

## Design decisions

- **Synchronous write is correct.** The job is already blocked on the
  work that failed. `FailureReportFactory` already `save`s through the
  repository because a transaction may be absent. Jobs use that same
  persist + GitHub-issue attempt, in-process. No queue.
- **One recorder, two call sites.** Keep HTTP `FailureReportFactory` /
  `ControllerSetup` behavior. Add a **background** entry that takes
  exception + source label (job name), no request/user. Same skip list
  (`ResponseStatusException`, `ApiException`, `UnexpectedNoAccessRightException`)
  and same GitHub attempt.
- **Report the swallowed resume failure.** Do **not** change
  continue-after-resume-failure. Report inside that catch. Uncaught
  scheduled throws (embedding, submit rethrow) go through a scheduler
  `ErrorHandler` so new jobs are covered without a catch in every method.
- **Do not report twice** on submit failure: that path rethrows, so only
  the scheduler handler records it (not also the job catch).

## Slices

Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

### 1. Record a failure report without an HTTP request — Structure `[x]`

`FailureReportFactory.fromException(exception, source, githubService, failureReportRepository)`
persists with exception class as `error_name` and `# source: …` plus stack
in `error_detail`. HTTP constructor still writes user + URI (shared
`contextPrefix` built at construction). Same skip list and GitHub attempt.

---

### 2. Hourly question-generation resume failure appears as a failure report — Behavior `[x]`

Resume catch calls `fromException` with `getClass().getSimpleName()`, then
continues to due-user submit. Maintenance-run `error` still recorded.
Submit catch still only `recordError` + rethrow (no second report).

---

### 3. Uncaught scheduled job exceptions appear as a failure report — Behavior `[ ]`

**Pre:** a `@Scheduled` method has no local catch (embedding job, or
question-generation **submit** after it rethrows). **Trigger:** the method
throws. **Post:** a Failure report exists with the exception and a
scheduled-job source. No second report from the submit `catch` that
already `recordError`s and rethrows.

**Verify:** scheduler `ErrorHandler` (on prod `SchedulingConfig`) — test
the handler with a real repository: `handleError` persists one report.
Embedding job needs no per-method catch.

---

## Discoveries

- Resume **does not stop** the hourly job today; only submit rethrow
  stops the rest of that run. This plan reports resume anyway and leaves
  that control flow alone.
- Two prod `@Scheduled` jobs only. The ErrorHandler is the generalization
  for the second job and any future one.
- Manual admin **Resume** already rethrows into `ControllerSetup`, so it
  can already create a Failure report. This plan is for **scheduled**
  execution.
- Slice 1: HTTP and background share `contextPrefix` on the factory
  record. Remaining slices call `fromException`; do not add nullable
  request/source fields.
- Slice 2: job tests stay Mockito-only (verify repository `save`); real-row
  persist is the factory test. Source label is `getClass().getSimpleName()`.
