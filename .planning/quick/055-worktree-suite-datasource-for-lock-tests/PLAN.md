# Worktree backend tests inherit the suite datasource

Status: planned; not executed.
Source: [SEED-015 Story 1a](../../seeds/SEED-015-concurrent-worktree-environments.md#story-1a)
and execution-retrospective of
[quick/054-configured-worktree-backend-tests](../054-configured-worktree-backend-tests/PLAN.md)
(commits `c937031a00`…`1089be2573`, merge `c764beb7ce`).

## Goal and scope

When a developer runs `pnpm backend:test:worktree` against a selected
`doughnut_<id>_test` database, Spring tests that start a JDBC context use that
database. They must not reopen the shared `doughnut_test` schema by hardcoding
its JDBC URL.

Leaf 8 of plan 054 proved the hole: a worktree-only Flyway script on checkout A
also applied to `doughnut_test` (history `flyway_schema_history.installed_rank`
23, version `300000321`) while A’s selected DB was `doughnut_wt_054a_test`.
`QuestionGenerationBatchMaintenanceConcurrencyTest` and `ShedLockConfigProdTest`
still set `spring.datasource.url=jdbc:mysql://127.0.0.1:3309/doughnut_test`.

This is a bounded repair of 1a’s selected-DB isolation, not a new concurrency
story.

**Exclude:** launcher/Gradle overrides of arbitrary `@SpringBootTest`
properties (not a sandbox); other tests that might pin a database; stories 1b
and 1c; raising `max_connections` further; deleting leftover proof databases.

**Assumption:** a test-profile `@SpringBootTest` without an explicit datasource
URL uses `db-test.properties` plus `SPRING_DATASOURCE_URL` from the worktree
launcher. If the prod-profile ShedLock slice cannot start without an explicit
URL, pass the suite URL through the existing env/property chain rather than
the literal database name `doughnut_test`.

## Outside-in proof

| Promise | Leaf | Signal |
|---|---|---|
| Test-profile lock/concurrency context uses the suite database | 1 | No `doughnut_test` JDBC URL on that class; focused tests still pass; worktree launcher tests remain green |
| Prod-profile ShedLock JDBC context uses the suite database | 2 | Same file no longer names `doughnut_test`; focused prod-profile tests still pass |

## Ordered slices

### 1. Test-profile lock tests use the suite datasource
Type: Behavior
Status: planned
Proof: `QuestionGenerationBatchMaintenanceConcurrencyTest` does not set a
`doughnut_test` JDBC URL. Focused backend tests for that class stay green.
`node --test scripts/backend-test-worktree.test.mjs` stays green.

Behavior: A worktree launcher has exported `SPRING_DATASOURCE_URL` for
`doughnut_<id>_test`. Running the test-profile Spring lock/concurrency tests
opens that database instead of a hardcoded `doughnut_test`.

### 2. Prod-profile ShedLock tests use the suite datasource
Type: Behavior
Status: planned
Proof: `ShedLockConfigProdTest` does not set a `doughnut_test` JDBC URL.
Focused tests for that class stay green. The worktree guide no longer treats
hardcoded test datasources as an accepted exception.

Behavior: The same worktree run’s prod-profile ShedLock JDBC context uses the
suite-selected database rather than `doughnut_test`.

## Current decisions

- Plan 054’s “custom Spring configuration injection is outside the workflow”
  still means the launcher does not fight `@TestPropertySource`. This repair
  removes the pins in the tests that actually broke isolation.
- Backend package tests normally run as a complete suite; use focused classes
  while red/green, then the complete backend unit-test command before wrap-up.
