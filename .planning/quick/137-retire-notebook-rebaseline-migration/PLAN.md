# Retire the spent notebook rebaseline migration

Status: done
Source: [SEED-009 story 44](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-44),
refined 2026-09-17. The owner confirmed on 2026-09-17 that production
successfully applied version `300000330`, which satisfies the story's
prerequisite. The owner authorized planning and plan refinement, not execution.

## Goal and scope

Maintainers no longer carry the one-time notebook Git rebaseline: its
migration, raw-JDBC rebuild and row readers, and migration-only test are gone.
Migration guidance keeps version `300000330` reserved. Owners see no change.

Included:

- delete `backend/src/main/java/db/migration/V300000330__RebaselineExistingNotebookGitBindings.java`;
- delete `NotebookGitBaselineRebuild` and `NotebookGitRows`
  (`backend/src/main/java/com/odde/donut/services/notebookGit/`), whose only
  production caller is that migration;
- delete `RebaselineExistingNotebookGitBindingsMigrationTest`;
- update `.agents/skills/db-migration/SKILL.md` so that
  `V300000329__ReplaceNoteTitleFunctionalIndex.java` is the newest remaining
  file, new versions must exceed `300000330` (a retired version still recorded
  in long-lived `flyway_schema_history`), and the example file name no longer
  uses a taken version.

Excluded: squashing the baseline, adding a placeholder, cleanup toggles,
`ignoreMigrationPatterns` changes, any generic retirement mechanism,
re-verifying the production rebaseline, and changes to shared notebook-Git or
`notebookExport` owners (`NotebookGitCutoverService`, `NotebookGitBundleBuilder`,
`ExportFolderRow`, `ExportNoteRow`, `PortableTreeSnapshot`, and so on).

Assumption: existing startup runs `flyway.repair()` before `migrate()`
(`FlyWayTestMigrationStrategyConfig`, `FlyWayFreeVersionRealMigration`,
`DonutTaskRunner.migrateTestDB`). The earlier "Retire spent data migrations"
cleanup (`ee1f2d254a`) deleted applied Java migrations this way.

Key examples (from the seed):

1. A database that recorded `300000330` as successful → cleaned-up build
   starts, Flyway succeeds, no migration runs.
2. Empty database → migrates through `300000329`; backend suite green.
3. Next migration → the guidance says to use a version above `300000330`.
4. Notebook creation and later Git history → existing notebook-Git tests green.
5. Product tree → no references to the rebaseline migration,
   `NotebookGitBaselineRebuild`, or `NotebookGitRows`.

## Existing-solution and architecture assessment

| Responsibility | Existing solution and decision |
| --- | --- |
| Running without a deleted applied migration | Startup `repair()` then `migrate()`; the `ee1f2d254a` precedent. Reuse; add nothing. |
| Notebook creation root and Portable tree | `NotebookGitCutoverService`, `NotebookGitBundleBuilder`, and `notebookExport` types are live runtime owners. Keep them. |
| Migration version guidance | `.agents/skills/db-migration/SKILL.md` (lines about the current tip and the minimum version). Correct in place. |

Caller inspection on 2026-09-17: `NotebookGitBaselineRebuild` is referenced only
by the migration and its test; `NotebookGitRows` only by
`NotebookGitBaselineRebuild`. No docs, E2E, frontend, or CLI references. No
ADR or North Star change is warranted.

## Outside-in proof ownership

| Promise | Slice | Observable proof |
| --- | --- | --- |
| Example 1: an upgraded database starts without the migration | 1 | Before deleting, the worktree's isolated test database records `300000330` (run the focused migration test once). After deleting, `pnpm backend:test:worktree` re-migrates that same database: `migrateTestDB` succeeds and applies nothing. |
| Example 4: notebook-Git behavior unchanged | 1 | Full backend suite green in the same run. |
| Example 5: no leftovers | 1 | `rg -n "Rebaseline\|NotebookGitBaselineRebuild\|NotebookGitRows" backend docs e2e_test frontend cli .agents` finds nothing. |
| Example 2: fresh install | 1 | Pushed `donut CI` run green. Its `migrateTestDB` runs against an empty database. |
| Example 3: version guidance | 2 | The skill names `300000330` as the minimum to exceed and `V300000329__…` as the newest remaining file; `rg -n "300000329\|300000330" .agents/skills/db-migration/SKILL.md` shows only consistent statements. |

## Current decisions

- Delete, don't squash: Flyway's recorded row for `300000330` stays in every
  long-lived database, and the next new migration uses a version above it.
- No new or changed tests. The deleted test covered only the migration;
  existing notebook-Git tests carry the preserved behavior.

## Ordered slices

### 1. Current installations run without the rebaseline migration

Type: Behavior
Status: done
Proof: with `unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL`, first
`CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests '*RebaselineExistingNotebookGitBindingsMigrationTest*'`
green **before** deletion (records `300000330` in `doughnut_<id>_test`); then,
after deletion, `CURSOR_DEV=true nix develop -c pnpm backend:test:worktree`
green against that same database, the leftover search above empty, and the
pushed CI run green.

Given a database that recorded `300000330` as successful, when the build
without the migration starts, then Flyway succeeds with nothing to apply, and
all current backend behavior passes. Delete the migration, both helpers, and
the migration test. Re-check callers with the search above before deleting.
If a shared owner has comments that mention the rebaseline, reword them. Do
not change behavior. No API generation is needed (no controller changes).

### 2. Migration guidance reserves the retired version

Type: Behavior
Status: done
Proof: skill text check above.

Given a maintainer reading the db-migration skill after slice 1, when they
choose the next migration version, then the skill directs them above
`300000330`, names `V300000329__ReplaceNoteTitleFunctionalIndex.java` as the
newest remaining file and explains that retired versions keep their numbers,
and its example file name uses an unused version (for example `V300000331__…`).
