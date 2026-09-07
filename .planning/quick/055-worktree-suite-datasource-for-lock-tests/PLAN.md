# Worktree backend tests inherit the suite datasource

Status: complete.
Source: [SEED-015 Story 1a](../../seeds/SEED-015-concurrent-worktree-environments.md#story-1a)
and execution-retrospective of
[quick/054-configured-worktree-backend-tests](../054-configured-worktree-backend-tests/PLAN.md)
(commits `c937031a00`…`1089be2573`, merge `c764beb7ce`).

## Goal and scope

When a developer runs `pnpm backend:test:worktree` against a selected
`doughnut_<id>_test` database, Spring tests that start a JDBC context use that
database. They must not reopen the shared `doughnut_test` schema by hardcoding
its JDBC URL.

This is a bounded repair of 1a’s selected-DB isolation, not a new concurrency
story.

**Exclude:** launcher/Gradle overrides of arbitrary `@SpringBootTest`
properties (not a sandbox); other tests that might pin a database; stories 1b
and 1c; raising `max_connections` further; deleting leftover proof databases.

## Outside-in proof

| Promise | Leaf | Signal |
|---|---|---|
| Test-profile lock/concurrency context uses the suite database | 1 | No `doughnut_test` JDBC URL on that class; focused tests still pass; worktree launcher tests remain green |
| Prod-profile ShedLock JDBC context uses the suite database | 2 | Same file no longer names `doughnut_test`; focused prod-profile tests still pass |

## Ordered slices

### 1. Test-profile lock tests use the suite datasource
Type: Behavior
Status: done
Proof: `QuestionGenerationBatchMaintenanceConcurrencyTest` does not set a
`doughnut_test` JDBC URL. Focused backend tests for that class stay green.
`node --test scripts/backend-test-worktree.test.mjs` stays green.

Behavior: A worktree launcher has exported `SPRING_DATASOURCE_URL` for
`doughnut_<id>_test`. Running the test-profile Spring lock/concurrency tests
opens that database instead of a hardcoded `doughnut_test`.

### 2. Prod-profile ShedLock tests use the suite datasource
Type: Behavior
Status: done
Proof: `ShedLockConfigProdTest` does not set a `doughnut_test` JDBC URL.
Focused tests for that class stay green. The worktree guide no longer treats
hardcoded test datasources as an accepted exception.

Behavior: The same worktree run’s prod-profile ShedLock JDBC context uses the
suite-selected database rather than `doughnut_test`.

## Current decisions

- The launcher still does not fight `@TestPropertySource` / `@SpringBootTest`
  properties. Isolation is restored by removing the pins in the tests that
  broke it.
- Test-profile lock tests inherit `db-test.properties` /
  `SPRING_DATASOURCE_URL` with no explicit datasource properties.
- Prod-profile ShedLock tests pass the suite URL as
  `${SPRING_DATASOURCE_URL:${db.url}}` (and `db.user` / `db.password`) so
  `application-prod.yml` does not send JDBC to `db-server`.
