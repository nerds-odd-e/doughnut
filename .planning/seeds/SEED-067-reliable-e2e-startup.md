---
id: SEED-067
status: dormant
planted: 2026-10-11
planted_during: owner follow-up to the readiness-race reminder from the SEED-066 spoken-title preparation landed at 7d062d701f
trigger_when: starting an isolated E2E session against a fresh worktree database
scope: small
---

# SEED-067: Start isolated E2E sessions reliably on a fresh database

## Why This Matters

Donut contributors need the first `pnpm e2e:hold` or `pnpm cy:run` in a new
worktree to produce a usable session or trustworthy test result. The reported
startup race can interrupt database migration and leave the disposable database
unusable until the contributor manually drops and recreates it. Reliable E2E
startup supports the current voice-input work and other product development.

## Alternatives and Decision

**Terry's decision, 2026-10-11:** Application readiness belongs in the existing
`/api/healthcheck`: return HTTP 503 while starting, then the existing HTTP 200
response. The E2E runner keeps polling HTTP.

The application owns its startup completion. Exposing that fact through the
existing healthcheck gives E2E, Development, and production traffic checks the
same signal. Runner-only migration waiting would repair E2E while leaving other
consumers able to treat an application still migrating as ready; it would also
make the runner interpret the application's migration lifecycle.

Reuse Spring Boot's application availability. In the installed Boot 4.1.1,
`ReadinessState.ACCEPTING_TRAFFIC` is published after synchronous
`ApplicationReadyEvent` listeners finish, including Donut's Flyway listener.
This avoids another migration-completion flag or schema-history polling loop.

## Story Decomposition

<a id="start-e2e-after-migrations"></a>

### Start E2E sessions after database migration completes

**Identity:** SEED-067#start-e2e-after-migrations
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/066-migration-aware-http-readiness/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"fef9b99bca7afa2076510dbc178f1b3e6cf7ddcb2a59cdf405c36a3d4aecee0b","plan":"bef93a384cda1ff158f5321ef76b7c94fef81ee45085f3fca9f059be924f88ae"}}
```

**Goal:** For Donut contributors using a new worktree, the application's
healthcheck reports readiness only after startup and database migration finish,
so the first E2E invocation resets and seeds a complete schema without manual
database recreation after an otherwise successful startup.

**Slice plan:** [Migration-aware HTTP readiness](../slice-plans/066-migration-aware-http-readiness/PLAN.md).

**Scope:**

- **Application readiness:** In the web application's `e2e`, `dev`, and `prod`
  profiles, `GET /api/healthcheck` returns HTTP 503 with a clear starting/not-ready
  response while application startup is incomplete. This includes the interval
  when HTTP is already reachable but the Flyway ready listener is still running.
- **Ready response compatibility:** Once startup completes successfully, return
  HTTP 200 with the existing text exactly: `OK. Active Profile: <profiles>.
  Commit: <commit>`. Preserve profile and deployed-commit reporting and the
  endpoint's existing access policy. The pending response must clearly represent
  startup rather than advertise success: the current deployment probe recognizes
  the `OK` body marker independently of the HTTP status.
- **Existing consumers:** The local load balancer's `/__lb__/ready` and the E2E
  runner consume the application's readiness signal. `pnpm e2e:hold` waits before
  its testability reset and held-session announcement; `pnpm cy:run --spec
  <feature>` waits before starting Cypress, whose setup resets test data. A
  successful startup within the configured timeout needs no database recreation.
- **Failure:** Let a startup/migration failure propagate visibly. The invocation
  ends unsuccessfully using the runner's existing startup-failure and service-exit
  handling, with its existing log diagnostics and owned-service cleanup.
- **Environment ownership:** Preserve the E2E runner's disposable database,
  service ownership, isolation, and shutdown. The shared healthcheck behavior
  does not authorize testability operations on Production or Development data.
- **Deferred promises:** Automatic repair of already partially migrated
  databases, reclamation of other retired databases, changes to migration timing
  or SQL, new operational probes, and startup/autohealing timeout changes.

**Architecture and consumer implications:**

- Use the application's existing Spring Boot availability state as the source
  of readiness. The current Flyway listener is synchronous, so Boot does not
  publish accepting-traffic readiness until migration returns successfully.
- Keep the current HTTP readiness chain: application healthcheck → local load
  balancer → runner. Preserve the runner's existing configurable timeout,
  cancellation, and failure handling.
- Production's load balancer, deployment verification, and VM autohealing also
  use `/api/healthcheck`. Terry approved making these consumers wait for startup.
  Autohealing's tracked configuration has a 300-second initial delay, then
  30-second checks with three failures before recreation. This story retains
  those limits; an unusually long startup is still bounded by operator policy.
- Follow [ADR 0006 — Failure handling](../../docs/adrs/0006-failure-handling-accepted.md):
  prevent premature success and propagate migration failure, without adding
  recovery or retry merely to conceal startup failure.
- Follow [ADR 0007 — Environments and isolation](../../docs/adrs/0007-environments-and-isolation-accepted.md):
  readiness proof uses isolated disposable E2E or Unit Test resources; persistent
  Production and Development data stay under their owners.

**Key examples:**

- A new worktree has an empty isolated database; the HTTP listener is reachable
  while migration is still running → `/api/healthcheck` reports HTTP 503 and a
  starting response. The local load balancer also reports not-ready, so the E2E
  invocation remains in its readiness wait.
- That migration finishes successfully within the configured startup timeout
  → `/api/healthcheck` returns HTTP 200 with the existing profile/commit text;
  the local load balancer reports ready. `pnpm e2e:hold` then resets and seeds
  the complete database and announces a usable held browser session.
- Under the same delayed-migration setup → `pnpm cy:run --spec <feature>` waits
  for readiness, then runs the selected feature successfully against the fully
  migrated, seeded database without a manual drop/recreate.
- A ready `dev` or `prod` application is checked → HTTP 200 still carries that
  application's active profile and deployed commit in the current format.
- Migration fails during startup → the invocation reports startup/service failure
  and exits unsuccessfully with diagnostics; the stack never reaches the
  successful readiness/session outcome.

**Evidence and remaining verification:**

- Original observation: the owner-supplied reminder after spoken-title preparation
  landed at `7d062d701f32fe487152d6414792f6b9df095039`; it describes the fresh
  database reset/migration race and manual recovery. No original failure log or
  command transcript was supplied.
- Refinement source reading at `846ddcb46e` (unchanged application/runner code in
  the refinement workspace at preparation announcement `b833c8cdb5`):
  `FlyWayFreeVersionIgnoreMigrationStrategyConfig` defers non-test migration;
  `FlyWayFreeVersionRealMigration.actualMigration` repairs/migrates on the
  highest-precedence synchronous ready listener. `HealthCheckController.ping`
  currently returns its success text unconditionally. The local load balancer
  accepts that endpoint's 2xx status, and `runOwnedE2eInvocation` waits on the
  resulting readiness before entering the hold or Cypress session.
- Framework source reading: the installed Spring Boot 4.1.1 sources archive's
  `EventPublishingRunListener.ready` publishes `ApplicationReadyEvent` first,
  then `ReadinessState.ACCEPTING_TRAFFIC`. This matches the
  [official lifecycle documentation](https://docs.spring.io/spring-boot/reference/features/spring-application.html#features.spring-application.application-events-and-listeners).
  No asynchronous application-event multicaster or async migration listener is
  configured in the inspected application source.
- Current proof: `HealthCheckControllerTest` checks the ready body's commit text,
  not HTTP startup readiness. `NotebookGitStartupServicesProbeTest` exercises
  the real migration ready listener and its ordering against a later consumer;
  it does not observe concurrent HTTP requests during startup. Existing runner
  tests cover HTTP 503 recognition, startup failure, cancellation, service exit,
  and reset-before-held-announcement ordering.
- Execution must first reproduce the early success through HTTP during a
  controlled non-test-profile migration interval and retain regression proof of
  the pending → ready response. The ordinary `test` profile migrates earlier,
  so its ready-body test alone cannot establish the reported timing boundary.
  A fresh isolated held session and selected Cypress feature still need
  observation; this refinement did not start the application or run tests.

- **Evaluation:** HTTP readiness stays pending until the real migration/startup
  boundary completes; the held session and selected Cypress invocation then
  reach their promised outcomes. Preserve ready-body compatibility and reuse
  existing runner failure/ownership proof rather than duplicating it.
- **Value / learning:** One truthful application readiness signal removes manual
  recovery from ordinary first E2E startup and prevents other readiness consumers
  from declaring startup complete during migration.
- **Effort hypothesis:** S (30–60 minutes), medium confidence for the readiness
  change; a deterministic non-test startup reproduction is the remaining sizing
  uncertainty to resolve during slice planning.
- **Depends on:** No blocking story dependency.
- **Safe stopping point:** The corrected healthcheck and its current consumers
  deliver the outcome independently of broader runner or database recovery work.

## Ordering and Scope Reduction

Keep this single bounded defect story first in the backlog. Preserve unrelated
queue, Taken, and done records. Broader recovery or probe separation
can be considered independently if later operational evidence warrants it.

## Open Decisions

No remaining product choice. The slice plan selects the execution approach and
proof ownership; its first probe must reproduce HTTP success during the real
migration interval before the dependent change. Planning does not authorize
implementation.

## When to Surface

Before the next fresh-worktree E2E startup or when planning this queued defect.

## Breadcrumbs

- Owner-supplied readiness-race reminder associated with
  [spoken-title preparation at its observed revision](https://github.com/nerds-odd-e/doughnut/blob/7d062d701f32fe487152d6414792f6b9df095039/.planning/seeds/SEED-066-voice-input.md#unobtrusive-selection-aware-spoken-title).
- [Isolated browser testing](../../docs/worktree-browser-tests.md).
- [Disposable database retirement](../../docs/worktree-retire-databases.md).
- Application readiness consumers: `scripts/local-lb.mjs`,
  `scripts/dev-healthcheck.mjs`, `infra/gcp/scripts/create-lb-healthcheck.sh`,
  `infra/gcp/scripts/app-instance-healthcheck.sh`, and
  `infra/gcp/scripts/add-mig-autohealing.sh`.
