# Scheduled job failure-report follow-up

**Status:** in progress (slice 1 done).
**Type:** ad-hoc plan (`.planning/quick/`)

Follow-up to the shipped work in `feat(failure-report): persist reports from
exception and source without HTTP`, `…record hourly question-generation resume
failures`, and `…persist uncaught scheduled job exceptions`.

## Goal

Prod `@Scheduled` jobs still write Failure reports, **and** they still run on
the **3-thread** scheduler that `application-prod.yml` already configures.
Tests for this work assert unique deltas only — no second Spring context just
to persist a row the factory test already covers.

## Inspection (what is and is not in this plan)

### Bug — in scope

`SchedulingConfig` now `@Bean`s a raw `ThreadPoolTaskScheduler` (default pool
size **1**) so it can `setErrorHandler`. That bean replaces Spring Boot’s
auto-configured scheduler. Prod already sets:

```yaml
spring.task.scheduling.pool.size: 3
```

That property no longer applies. Embedding (every 5 min) and hourly
question-generation share one thread again. Failure reports still work; the
pool regression is the miss.

Boot 4 seam: `ThreadPoolTaskSchedulerCustomizer` on the auto-configured
scheduler — attach the ErrorHandler without replacing the bean.

### Redundant tests / missed refactor — in scope

- `ScheduledJobErrorHandlerTest` is `@SpringBootTest` + Mockito and only
  checks `handleError` → one row with `scheduled-job`. Persist shape already
  lives in `FailureReportFactoryTest`. Slice-2 wrap-up already rejected this
  mix on the job tests; slice 3 reintroduced it. The handler is never driven
  through a `TaskScheduler`, so a missing `setErrorHandler` would still pass.
- `FailureReportFactoryTest.recordsFailureReportFromExceptionAndSourceWithoutHttpRequest`
  re-asserts `errorName` and stack file already covered by the HTTP factory
  test. Unique delta: source in `error_detail`, no request/user block.
- `QuestionGenerationBatchMaintenanceRunRepositoryTest` autowires real
  `GithubService` + `FailureReportRepository` only to satisfy the job
  constructor; that test never asserts failure reports.

### Keep (not a problem for this plan)

- Resume catch still reports then continues; submit catch still rethrows
  without a second factory call. Job tests for those deltas stay.
- `fromException` + HTTP constructor sharing `contextPrefix` is the right
  recorder. Do not add a third wrapper type.
- Jobs and `SchedulingConfig` are `@Profile("prod")`. E2E is `e2e`. Skipping
  `TestabilitySettings.getGithubService()` on the scheduled path is not a
  prod/e2e bug.
- `handleError` only records `instanceof Exception` — same bound as HTTP
  `@ExceptionHandler(Exception.class)`.
- Nested prune-order test inside `QuestionGenerationBatchMaintenanceJobTests`
  is pre-existing, not from this work.
- Factory double-`save` (row then GitHub issue number) is pre-existing.

### Out of scope

- Fixing EntityManager prune `remove`
- Deduplicating similar reports
- Changing continue-after-resume-failure
- Per-job source labels on the scheduler ErrorHandler (keep `scheduled-job`)

## Design decisions

- **Customizer, not a replacement scheduler.** A
  `ThreadPoolTaskSchedulerCustomizer` `@Bean` in prod `SchedulingConfig` calls
  `setErrorHandler`. Boot keeps `spring.task.scheduling.pool.size: 3`.
  `@EnableScheduling` stays where it is.
- **Test the scheduler seam, not `new Handler()`. ** Drive a
  `ThreadPoolTaskScheduler` after the customizer (initialize, schedule a
  throwing runnable, wait for `save`). Mockito `FailureReportRepository` +
  mock `GithubService` — no `@SpringBootTest` for this wrapper.
- **Minimum tests for same coverage.** Factory `fromException` keeps only the
  source / no-HTTP delta. Job tests keep resume-reports + submit-does-not.
  Repository test constructs the job with mocks for the unused deps.

## Slices

Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

### 1. Prod scheduler keeps a 3-thread pool and still records uncaught failures — Behavior `[x]`

`SchedulingConfig` exposes `ThreadPoolTaskSchedulerCustomizer` (`setErrorHandler`
only). No `TaskScheduler` `@Bean`. `SchedulingConfigTest` applies customizer to
a pool-size-3 scheduler, schedules a throwing runnable, asserts pool size 3
and `scheduled-job` on `save`. SpringBootTest `ScheduledJobErrorHandlerTest`
removed.

---

### 2. Drop leftover duplicate factory and constructor-only test wiring — Structure `[ ]`

Enables nothing further; stop-safe cleanup of slice 1 leftovers. No product
behavior change.

- `fromException` factory test: assert source in `error_detail` (and that
  request/user blocks are absent). Do not re-assert `errorName` or stack file.
- `QuestionGenerationBatchMaintenanceRunRepositoryTest`: pass mocks (or
  null-safe test doubles) for `GithubService` / `FailureReportRepository`
  instead of autowiring them for a happy-path run that never reports.

**Verify:** `FailureReportFactoryTest` and
`QuestionGenerationBatchMaintenanceRunRepositoryTest` still pass.
`ControllerSetupTest` still passes (HTTP path untouched).

---

## Discoveries

- Prod yaml already chose pool size 3; the ErrorHandler slice replaced the
  Boot scheduler and silently dropped that. Customizer is the Boot 4 API
  (`org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer`).
- Slice-2 wrap-up already treated SpringBootTest+Mockito on the job as
  test-only persist wiring. The handler test repeated that mistake.
- Slice 1: Boot 4 customizer is `org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer`.
  Seam test is `SchedulingConfigTest` (Mockito, `timeout` on `save`).
