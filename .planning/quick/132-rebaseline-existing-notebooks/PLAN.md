# Rebaseline existing notebook Git bindings

Status: planned
Source: [SEED-009 story 43](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-43),
refined 2026-09-17. The owner authorized planning, not execution.

## Goal and scope

On the first ordinary application startup after this change is released, every
live notebook that already has a Git binding receives one replacement accepted
bundle. Its sole reachable commit is a new parentless root whose tree is the
notebook's current canonical Portable notebook tree. The previous root and all
later accepted commits are deliberately abandoned without a backup, retained
ref, or ancestry bridge; owners must freshly acquire the notebook before making
another local publication.

The migration preserves notebook, folder, note, binding, ownership, authored
content, and learning identities and data. It updates only the existing
binding's accepted object ID, bundle bytes, and update time, atomically per
notebook. A failure propagates and prevents successful startup rather than
persisting a mismatched object ID and bundle.

Included:

- a one-time Java Flyway data migration after current tip `V300000329`;
- automatic, unconditional execution through the existing startup
  `repair()`/`migrate()` path, with no toggle, placeholder gate, or opt-in;
- selection of live notebooks that already have a binding;
- a fresh root snapshot containing current notebook/folder Readmes, ordinary
  notes, empty-folder markers, and location-based trash under the canonical
  Portable-tree rules; and
- migration-owned proof of complete history abandonment, retained application
  data, fleet selection, and binding consistency.

Excluded:

- soft-deleted notebooks and live notebooks without a binding;
- backup, reconciliation, rebasing, ancestry preservation, or repair of a
  previously downloaded repository;
- manual release, deployment, maintenance-mode, or production-confirmation
  work;
- changes to creation-time binding behavior for future notebooks; and
- removal of the spent migration and its dedicated support, which belongs to
  [story 44](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-44)
  after the target environment confirms success.

The owner explicitly overrides the repository's default gated-DML guidance for
this migration. It must run automatically as soon as the application starts
with the pending Flyway version.

## Existing-solution and architecture assessment

Current code and relevant history were inspected at `c49fd41bb0`; no automated
checks were run during planning.

| Responsibility | Existing solution and decision |
| --- | --- |
| Canonical tree construction | Reuse `PortableTreeSnapshot`, `ExportFolderRow`, `ExportNoteRow`, and the current notebook export rules. Accepted [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md) defines the tree, including Readmes, empty-folder `.keep`, and location-based trash. Do not invent a migration-specific format. |
| Git root and bundle construction | Reuse `NotebookGitBundleBuilder` and `NotebookGitBundleWriter`, with the stable Donut system author identity already owned by `NotebookGitCutoverService`. The replacement intentionally has no parents. |
| Migration-time database access | Restore the domain shape of the retired `NotebookGitRows` raw-JDBC reader because Flyway runs before a JPA persistence context is available. Adapt it to the current schema, where note soft deletion is gone and every note—including notes below `_trash`—belongs in the Portable tree. Keep this duplication migration-owned and temporary for story 44 to remove. |
| Atomic binding replacement | Restore the retired `NotebookGitBaselineRebuild` responsibility: build the complete bundle first, then update object ID, bytes, and timestamp in one SQL statement inside one notebook transaction while retaining binding ID and `created_at`. Do not delete and recreate the binding row. |
| Fleet trigger and selection | Add `V300000330__RebaselineExistingNotebookGitBindings` following the retired `V300000327` pattern: join `notebook` to `notebook_git_binding`, require `notebook.deleted_at IS NULL`, order deterministically, and run outside Flyway's single transaction so each notebook owns its atomic replacement. `FlyWayFreeVersionRealMigration` already calls `repair()` then `migrate()` automatically; add no second trigger. |
| Failure policy | Follow Accepted [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md): propagate an unbuildable notebook or database failure. The atomic update prevents a half-replaced binding; no recovery framework or catch-and-continue loop is selected. |

The historical rebuild implementation is an exact Proudly Found Elsewhere
match whose lifecycle ended only because its earlier migration was spent. Reuse
and adapt that responsibility instead of adding a second snapshot model. The
current JPA `NotebookGitCutoverService` remains the creation-time owner and is
not widened into a pre-JPA migration service.

Proposed ADR 0002 is non-binding. Its append-only direction is deliberately
interrupted once by the owner's explicit history-destruction decision; the new
root begins the next append-only sequence. No Accepted ADR conflict or new North
Star topic was found.

## Outside-in proof ownership

| Promise | Owning slice | Observable proof |
| --- | --- | --- |
| Every live bound notebook receives a single-root bundle matching its current Portable tree | 2 | A real-database migration test invokes the Flyway migration over multiple live bound notebooks and reads each resulting JGit bundle back |
| The prior root and all later accepted commits are absent from the replacement bundle | 2 | The canonical live-bound fixture begins with a multi-commit accepted history; after migration its bundle exposes one reachable parentless commit and cannot resolve the captured old commit IDs |
| Donut identities and retained content/learning data survive unchanged | 2 | The same test captures the affected database rows before migration and compares them afterward while asserting binding ID and `created_at` are retained |
| Soft-deleted and unbound notebooks are outside the selected fleet | 2 | A focused sibling migration test observes an existing deleted binding unchanged and a live unbound notebook still unbound |
| Head and bundle remain one consistent replacement | 2 | Read the new head from the stored bundle and assert it equals `accepted_git_object_id`; the production update writes both fields together |
| The migration is discovered without a gate and current backend behavior remains valid | 2 | Source inspection finds no placeholder/toggle branch, and `CURSOR_DEV=true nix develop -c pnpm backend:verify` completes with the new Flyway version |

No E2E or API-client generation is selected: this is a startup data conversion
with no HTTP contract or UI behavior. The migration changes data but not schema,
so database-ERD regeneration is not selected.

## Ordered slices

### 1. A migration-capable owner can build one current replacement root

Type: Structure
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:verify` keeps current
backend and Flyway behavior green while the immediate fleet Behavior is added
next.

Delivered: restored `NotebookGitRows` and `NotebookGitBaselineRebuild` under
`backend/src/main/java/com/odde/donut/services/notebookGit/`, adapted from the
code retired at `ee1f2d254aa270d1b830b945754fcc2479ba23bb`. Every query, column
mapping, and collaborator call site (`PortableTreeSnapshot.build`,
`NotebookGitBundleBuilder.build`, `NotebookGitBundleWriter.write`,
`NotebookGitCutoverService`'s system-author/message constants) already matched
current schema and current APIs byte-for-byte; only Javadoc was reworded to
drop a reference to the no-longer-existing `NotebookGitFleetCutoverBackfill`
sibling and to state the no-soft-deletion/trash-inclusion rule explicitly.
Neither class is wired into any production or migration path yet. Proof:
`CURSOR_DEV=true nix develop -c pnpm backend:verify` — BUILD SUCCESSFUL, full
backend/Flyway suite green, both classes compile cleanly. Post-change refactor
pass: `none — already clean`.

Restore the retired raw-JDBC Portable-tree reader and atomic single-notebook
baseline replacement as migration-owned support. Adapt queries and comments to
the current schema and reuse current export rows, Portable-tree construction,
Git builder/writer, and system author identity. Build the complete bundle before
the transaction's one binding update; retain binding ID and `created_at`.

Do not register a migration, add a toggle, or change runtime notebook creation
in this slice. This Structure exists only to enable slice 2 and is owned for
removal by story 44.

Sizing hypothesis: about five to ten minutes of active change because the
retired implementation supplies the cohesive shape; current-schema adaptation
and compile verification are the only expected differences. Full verification
time is an external wait.

### 2. Startup replaces every live bound notebook's complete old history

Type: Behavior
Status: done
Proof: the real-database migration examples and
`CURSOR_DEV=true nix develop -c pnpm backend:verify` satisfy the mappings above.

Delivered: registered `V300000330__RebaselineExistingNotebookGitBindings` and
added `RebaselineExistingNotebookGitBindingsMigrationTest`, a real-database
migration-boundary test with a canonical multi-commit fixture (nested folders,
an ordinary note, a trashed note, retained memory-tracker/recall-log data) plus
a second notebook proving fleet iteration, and a sibling test proving a
soft-deleted notebook's binding stays untouched and an unbound notebook stays
unbound. Every row of the plan's outside-in proof table is covered (verified by
the coordinator against the actual assertions, not just the report). Proof:
`CURSOR_DEV=true nix develop -c pnpm backend:verify` — BUILD SUCCESSFUL,
`migrateTestDB` applied `V300000330` for real, full suite green (independently
rerun twice by the coordinator: once before and once after the refactor pass
below). Post-change refactor: extracted a shared
`GitBundleTestReader.readTreeEntries(repository, commit)` helper (mirroring the
already-existing `pathsIn` helper's style) and switched
`NotebookGitCutoverServiceTest`'s two pre-existing inline tree-read loops and
the new migration test's loop onto it, removing now-duplicated inline
`TreeWalk`/`ObjectLoader` blocks — proof rerun focused on both affected test
classes (4 tests, 0 failures) plus a full `backend:verify` rerun, both green.

Behavior: Given several notebooks with current data and accepted histories,
including later commits after their original roots, when the pending Flyway
migration runs during startup, every live bound notebook receives one fresh
parentless accepted commit matching its current Portable tree, while deleted
and unbound notebooks remain outside the migration.

Add the next Flyway Java migration, with no placeholder or configuration gate,
and the smallest migration-boundary test fixture that proves the complete
fleet result. Use multiple live bindings to prove iteration; make one canonical
fixture carry a multi-commit old graph and representative Readmes, nested
folders, ordinary notes, trash, and retained learning data. Keep the exclusion
case focused on population selection rather than repeating the canonical tree
assertions.

This slice keeps migration registration, selection, and its populated-database
proof together because a committed Flyway migration is immutable and must land
with its final selection contract. The larger active-change estimate is a
stated exception to the five-minute target: splitting the test from the
migration or adding the population constraint in a later migration would create
an unsafe delivery boundary. Historical fixtures and helpers reduce the
expected work to roughly fifteen to thirty minutes plus the mandatory full
backend verification wait.

## Current decisions

- Resolved execution identity (Story Branch Mode): originating checkout/branch
  `/Users/terryyin/git/doughnut` on `main` (claim commit `ea9c1a899f`);
  execution checkout/branch
  `/Users/terryyin/git/doughnut-worktrees/132-rebaseline-existing-notebooks`
  on `132-rebaseline-existing-notebooks`; integration checkout/branch and
  authorized remote target: originating checkout, `main`, `origin`.
- Destroy the complete prior accepted Git graph without backup; do not preserve
  even the original root.
- Select only notebooks with `deleted_at IS NULL` and an existing binding.
- Run once through ordinary automatic Flyway startup with no toggle, gate, or
  opt-in; manual release work is outside this plan.
- Preserve all non-binding application rows and the binding row identity; only
  replace accepted head, bundle bytes, and update time.
- Reuse the historical migration shape and current Portable-tree/Git owners;
  keep migration-only JDBC projection code temporary.
- Leave cleanup queued as story 44 until the target environment confirms this
  migration completed.
- Execution authorized and underway via `/dough-execute-plan 132`
  (Story Branch Mode, branch `132-rebaseline-existing-notebooks`); production
  confirmation, cleanup, retrospective, and story wrap-up still require their
  applicable later workflows.

## Learnings

- Git history contains no migration version above `300000329`; the next
  available Flyway version is `300000330`.
- The retired `V300000327`, `NotebookGitBaselineRebuild`, and `NotebookGitRows`
  already express the selected lifecycle and were removed only after their
  previous target data crossed the migration.
- Current application startup already owns `repair()` followed by `migrate()`;
  no scheduling or release mechanism belongs in this story.
- Current note persistence no longer has note soft deletion. Raw migration
  reads include every note row, while notebook `deleted_at` still defines the
  selected live fleet.
- Slice 1 found zero current-schema drift from the retired implementation:
  every query, column name, and collaborator API (`PortableTreeSnapshot`,
  `NotebookGitBundleBuilder`, `NotebookGitBundleWriter`,
  `NotebookGitCutoverService`) still matches exactly.
- Reused `NotebookGitCutoverService.CUTOVER_COMMIT_MESSAGE` as-is for the
  rebaseline replacement commit rather than inventing a distinct message.
  Slice 2 should decide deliberately whether a rebaselined notebook's new root
  should carry different wording than an ordinary creation-time cutover
  commit, since right now the two are textually indistinguishable.
- `notebook_git_binding` also has `amendment_head`, `amendment_note_id`, and
  `amendment_last_changed_at` columns (added in `V300000321`, after the
  retired implementation). Neither the historical nor the restored
  `NotebookGitBaselineRebuild` touches them, so a rebaseline leaves any stale
  amendment state referencing the abandoned history untouched. The plan's
  scope does not mention amendment columns; slice 2 should confirm whether
  this is an intended gap or needs explicit handling before registering the
  migration.
- Slice 2 resolved the commit-message decision: added
  `NotebookGitBaselineRebuild.REBASELINE_COMMIT_MESSAGE` ("Rebaseline: replace
  notebook history with a fresh snapshot of current content") and used it
  instead of `NotebookGitCutoverService.CUTOVER_COMMIT_MESSAGE` for the
  replacement root. A rebaselined notebook's history destruction is a
  meaningfully different event from an ordinary creation-time cutover and
  deserves distinct wording in the Git log.
- Slice 2 confirmed the amendment-columns gap above is deliberate, not an
  oversight: the plan's scope explicitly states the migration "updates only
  the existing binding's accepted object ID, bundle bytes, and update time",
  so leaving `amendment_head`/`amendment_note_id`/`amendment_last_changed_at`
  untouched matches the stated scope. No change made; flagged here for
  whoever eventually reconciles amendment state with abandoned history.
- Registered `V300000330__RebaselineExistingNotebookGitBindings`
  (`backend/src/main/java/db/migration/`), an exact structural copy of the
  retired `V300000327` pattern pointed at the restored
  `NotebookGitBaselineRebuild.rebuildNotebook`. No toggle or gate.
- The migration-boundary test
  (`RebaselineExistingNotebookGitBindingsMigrationTest`) initially failed with
  `jakarta.persistence.EntityExistsException: Detached entity passed to
  persist` when building the canonical fixture (memory tracker/recall log)
  directly under `@Transactional(propagation = Propagation.NOT_SUPPORTED)`,
  because each `makeMe...please()` call auto-commits its own transaction and
  previously-saved entities become detached across calls, breaking
  cross-entity cascades (e.g. `MemoryTracker.user`). Fixed by building all JPA
  fixture state for one notebook inside a single committed transaction via
  the existing `CommittedTransactionTestSupport.inCommittedTransaction`
  helper (already used by other concurrent/committed-transaction tests), then
  doing the raw-JDBC history rewrite and the migration run afterward against
  the now-committed rows. Worth remembering for any future migration-boundary
  test that needs richer JPA fixtures (not just Notebook/Folder/Note) under a
  non-transactional test method.
