# Retire spent data migrations

Status: planned

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
Status: planned
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
