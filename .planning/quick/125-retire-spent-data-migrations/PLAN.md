# Retire spent data migrations

Status: done

Execution identity:

- mode: Story Branch Mode
- originating checkout: `/Users/terryyin/git/doughnut`
- originating branch: `main`
- execution checkout: `/Users/terryyin/git/doughnut-worktrees/125-retire-spent-data-migrations`
- execution branch: `codex/125-retire-spent-data-migrations`
- integration target: `main`

## Source

[SEED-009 Story 39 — Retire the spent portable-trash migration after production success](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-39)

## Goal and scope

Maintainers install and upgrade Donut from a migration chain that contains the
current schema and active runtime responsibilities. Production has completed the
data transformations covered by this plan.

Included work:

- retire applied data-only Flyway versions `V300000306`, `V300000311`,
  `V300000318`, `V300000320`, `V300000326`, and `V300000327`;
- fold the `V300000328` final note schema into `V100000000__baseline.sql` and
  retire that transition;
- retire Java helpers, standalone backfills, migration fixtures, and migration
  tests whose caller graphs terminate inside completed data-conversion work;
- align current schema documentation and capability comments with the remaining
  runtime model; and
- keep `V300000329` as the current schema-transition tip.

Preserved boundaries: Flyway startup and repair, schema-changing migrations,
current operational tasks, Portable notebook tree and trash behavior, and
product-level persistence tests remain with their owning capabilities.

## Architecture and existing-solution decision

- [ADR 0004 — OKF-compatible notebook Markdown profile](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  keeps folder location as the source of trash state and retains learning state
  through ordinary product behavior.
- [ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
  assigns fresh verification data to the isolated Unit Test environment.
- Commit `e434eb26aade8c2b7bccc10ee1dff40c98fe5321` is the existing Flyway cleanup
  precedent. This plan reuses its baseline-alignment and migration-owned cleanup
  approach while retaining the newer `V300000329` schema transition as the tip.

## Key examples and proof ownership

| Promise | Owning slice | Observable proof |
| --- | --- | --- |
| A fresh isolated database installs the current schema and starts the backend | 1 | `CURSOR_DEV=true nix develop -c pnpm backend:verify` completes in the execution worktree |
| Current backend, Portable notebook tree, trash, and Git behavior remains green | 1 | The same backend verification suite completes, including current product-level tests |
| Maintained schema documentation describes the current relational model | 1 | `CURSOR_DEV=true nix develop -c pnpm export:database-erd` completes and the generated ERD matches the migrated isolated schema |

## Ordered slices

### 1. Current installations use the compact migration chain

Type: Behavior
Status: done
Proof: Fresh isolated database migration, full backend verification, and ERD
generation through the commands mapped above.

Behavior: Given production has completed the applied data transformations, when
a maintainer installs or verifies Donut, the backend starts on the current schema
and the current product behavior suite passes using the compact migration chain.

## Current decisions

- Production is the sole persistent data installation requiring the retired
  transformations, and its conversion is complete.
- `V300000329` remains the schema-transition tip.
- Migration artifact retirement is deletion; verification observes current
  installation and product behavior.

## Learnings

- Caller inspection identified two standalone backfill groups and the
  `admin_data_migration_progress` table as completed migration support.
- Refactor inspection found the completed authored-reference progress-table
  create/drop pair, the earlier placeholder, and the empty admin data-upgrade
  endpoint in the same migration-support lifecycle.

## Accepted proof

- Promise: a fresh isolated database installs the current schema and starts the
  backend, while current backend behavior remains green.
  - Boundary: Spring Boot Flyway startup and the full backend test suite.
  - Setup: `V100000000__baseline.sql` plus the remaining migrations in isolated
    database `doughnut_wt_431d41f9477b4906a5750c5d9631033b_test`.
  - Observations: `migrateTestDB` startup and the 2,400-test Gradle result.
  - Command: `unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL; CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
  - Result: pass.
- Promise: the generated API and frontend consumers match the current backend
  surface.
  - Boundary: generated OpenAPI/TypeScript client plus the frontend suite.
  - Observations: generator completion, OpenAPI lint, and 1,894 frontend tests.
  - Commands: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`;
    `CURSOR_DEV=true nix develop -c pnpm openapi:lint`;
    `CURSOR_DEV=true nix develop -c pnpm frontend:test`.
  - Result: pass.
- Promise: maintained schema documentation describes the current relational
  model.
  - Boundary: MySQL `information_schema` to `docs/database-erd.md`.
  - Setup: the isolated database named above.
  - Command: `DONUT_ERD_SCHEMA=doughnut_wt_431d41f9477b4906a5750c5d9631033b_test CURSOR_DEV=true nix develop -c pnpm export:database-erd`.
  - Result: pass.
