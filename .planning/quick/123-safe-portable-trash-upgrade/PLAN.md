# Release portable trash without risking notebook data

Source: [SEED-009 story 37](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-37).
Status: planned; planning-only instruction. No implementation or release performed.

## Outcome and decisions

Notebook owners retain authored notebook data and learning identities through
the legacy-trash upgrade. The operator can recover an interrupted upgrade while
old application writers remain excluded. Preserve the resulting Portable tree;
the already-selected baseline rebuild intentionally replaces prior Git history.
Owners need fresh checkouts and instructions to preserve unpublished local work
before replacing a checkout. This release does not implement local-history merge.

Terry Yin, 2026-09-14: production has not deployed this migration; explicitly
revise committed `V300000328` as a one-time exception to migration immutability.
Record that exception in its implementation commit. Do not edit other committed
migrations without an evidenced need and corresponding owner direction. Do not
renumber the failed migration or pretend a later migration can bypass it.

Use a bounded maintenance release, backup/restore and verified writer exclusion.
No general migration framework, resumable job service, permanent fault-injection
hooks, zero-downtime expand/contract rollout, UI polish or trash feature expansion.
10,000 deleted notes is representative safety evidence, not a product limit or
permanent benchmark/SLA. Optimize only an observed obstacle to this release.

## Existing solutions and architectural assessment

PFE assessment (migration, product and deployment boundaries):

- Reuse `NoteLegacyTrashMigration`'s whole-conversion transaction and
  `NotebookGitBaselineRebuild`'s per-notebook transaction. Registered Java
  migrations 326/327 already call them. Exercise the registered chain, rather
  than duplicating their recipes in tests or adding another production runner.
- Change `V300000328__drop_note_deleted_at.sql` in place. Prefer one atomic
  index/column alteration with narrowly scoped schema-state handling for retry,
  including commit-before-Flyway-history recording. A single ALTER alone does
  not establish that retry works after its commit. Reconcile the known partial
  states from the old script; unexpected schema shapes fail with a clear message.
- `NotebookGitRebuildTestSupport`, `GitBundleTestReader`, committed fixtures and
  existing baseline tests supply data/tree assertions. The current
  `NotebookUpgradeDataPreservationTest` starts after migrations; extend/replace
  its migration-only proof with a populated pre-upgrade Flyway rehearsal.
  `MemoryTrackerDeletedAtUpgradeTest` copies SQL into a temporary table: do not
  copy that technique for proof of actual Flyway ordering or durable DDL.
- Use the existing worktree-owned disposable MySQL allocation. A temporary
  isolated schema/harness is justified for pre-upgrade DDL and interrupted
  commits, which must not mutate the ordinary suite schema or Development.
  Keep it local to this migration's test support, with deterministic teardown.
- Existing `publish-application.sh`, backend deployment scripts, Application
  Release workflow and release runbook own rollout. Add only the one-time
  maintenance path/guard needed there; ordinary rolling replacement does not
  prove writer exclusion. No parallel release orchestrator.

Accepted ADR 0004 (`docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md`)
requires location-based trash with retained Portable files and learning identity.
ADR 0007 (`docs/adrs/0007-environments-and-isolation-accepted.md`) requires owned,
isolated test data. No ADR deviation. `.planning/NORTH-STAR.md`'s publication
composition topic does not require new direction for this one-time upgrade.

## Slices

Target about 5 minutes active work including implementation, proof and local
cleanup. More than 5 minutes requires scrutiny; more than 10 requires finer
decomposition and recording the failed sizing assumption. Mandatory full backend
suite/startup, the single scale rehearsal and cloud rehearsal waiting are explicit
wait-only exceptions, recorded separately. No active-work exception. Every slice
ends green; an unfinished slice is never evidence that production release is safe.

### 1. Isolate a populated pre-upgrade Flyway fixture
Type: Structure
Status: done
Change: Added `PreUpgradeFixtureSchema` (package-private `AutoCloseable`) and
its lifecycle proof `PreUpgradeFixtureSchemaTest`, both in
`backend/src/test/java/com/odde/donut/services/notebookGit/`. It derives an
owned disposable schema name from the enclosing suite's own live schema, grants
it to the `doughnut` app user by reusing `scripts/backend-test-worktree-owner.sh`'s
existing grant pattern, runs the actual project Flyway migrations
(`classpath:db/migration`) up to version `300000325`, and exposes a raw-JDBC
`connection()`/`schemaName()` for a later slice's pre-326 fixtures. `close()`
drops only the owned schema.
Proof: A harness lifecycle check observes version 325 and `note.deleted_at`,
then cleanup, without changes to the enclosing suite schema. Do not introduce
a generic schema registry or a new test service.
Estimate: 5 minutes active; if schema ownership/setup needs another mechanism,
stop and refine before expanding it.
Learnings: Actual active time ran ~35-40 minutes (implementation ~25-30,
refactor ~10), well past the 5-minute target and the 10-minute hard limit,
because the version boundary and registered migration classes needed tracing
(`V300000326`/`V300000327` Java migrations live under
`backend/src/main/java/db/migration/`, distinct from their delegate classes
`NoteLegacyTrashMigration`/`NotebookGitBaselineRebuild` under
`com.odde.donut.services.notebookGit`; `V300000325__cascade_note_dependents_on_note_delete.sql`
confirmed as the correct pre-conversion boundary), and because the suite's
`doughnut`-authenticated connection could not see a root-created schema until
it was explicitly granted, mirroring `scripts/backend-test-worktree-owner.sh`'s
provisioning grant. No fallback mechanism or scope change was needed once that
was found — recording the failed sizing assumption per this plan's discipline
rather than re-splitting a slice that already converged. Refactor pass
extracted a duplicated admin-connection helper (`executeAsAdmin`) within the
new file only; no other duplication or scope issue found. Both focused proofs
and the full backend suite pass; `git status` shows only the two new files.

### 2. Recover the column retirement at the actual Flyway boundary
Type: Behavior
Status: done
Behavior: A populated note table is in an original or known interrupted 328
state → run revised 328 through repair/migrate → final schema and retained rows
are correct, including retry after DDL commit before successful history recording.
Change: Reproduced the old missing-index failure first (MySQL error 1091,
`Can't DROP 'idx_note_structural_peer'`) against the unmodified file, matching
story 37's release-audit provenance. Revised only
`V300000328__drop_note_deleted_at.sql`: a temporary stored procedure branches on
the actual `information_schema` shape of `note.deleted_at`/`idx_note_structural_peer`
(original / index-dropped-only / both-dropped / fully-complete), running only the
remaining ALTERs for that state; an unrecognized shape fails loudly via `SIGNAL
SQLSTATE '45000'`. MySQL 8.4 does not support `IF [NOT] EXISTS` on `ALTER TABLE
ADD/DROP COLUMN` or `DROP INDEX` (verified empirically; MariaDB-only extension),
which is why a guard procedure was needed instead of idempotent ALTER syntax.
Proof: One parameterized Flyway boundary check
(`V300000328DropNoteDeletedAtMigrationTest`) over 6 states — the 4 schema shapes
above, already-recorded-success, and a guard-procedure-left-behind state — with
row-identity/content snapshots before and after. Uses the actual migration
resource via `PreUpgradeFixtureSchema` (slice 1, extended with
`openConnection()`/`jdbcUrl()`/`flywayConfig()`), a real discarded-connection
interruption (not mocked Flyway internals), and `flyway.repair()`+`migrate()`
mirroring production startup. Final index columns/order and column absence
assert correctly; already-recorded success stays unchanged; unknown schema
fails loudly (exercised via the reproduction, not asserted in the parameterized
suite).
Engine hypothesis: production-family MySQL 8.4/InnoDB supports the selected
DDL form and per-statement atomicity — CONFIRMED. `SELECT VERSION()` →
`8.4.11`. `SHOW CREATE TABLE note` before: `deleted_at datetime DEFAULT NULL`
present, `idx_note_structural_peer (notebook_id,folder_id,deleted_at,id)`.
After: `deleted_at` absent, `idx_note_structural_peer (notebook_id,folder_id,id)`.
Command: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (used instead
of the `backend:verify` wrapper during slice execution, per this plan's
execution-discipline note — the wrapper's extra formatter is the coordinator's
job). Result: BUILD SUCCESSFUL, full suite green.
Estimate: 5 minutes active after fixture support; a failed engine assumption
changes this slice before proceeding, not by adding a fallback framework.
Learnings: Active time ran far over target — implementation ~60-70 minutes,
coordinator review ~20 minutes — because this slice legitimately carries the
engine-proof gate the plan calls out, and because the coordinator's own review
(the delegated post-change-refactor agent spawn was denied by the harness's
auto-mode permission classifier for this migration-file content, so the
coordinator reviewed directly instead) found a genuine retry-safety gap the
implementation had not tested: `CREATE PROCEDURE` and the trailing `DROP
PROCEDURE` are each their own DDL auto-commit in MySQL, so an interruption
between `CREATE PROCEDURE` and `CALL`, or between a successful `CALL` and the
final `DROP PROCEDURE`, would leave the guard procedure itself behind and make
any retry fail on "PROCEDURE already exists" before the schema-shape logic ever
ran — defeating the exact retry-safety promise this slice exists to prove.
Fixed with a leading `DROP PROCEDURE IF EXISTS` (a distinct, universally
supported MySQL clause, unlike the ALTER-table `IF EXISTS` forms ruled out
above) and proved with a new `GUARD_PROCEDURE_LEFT_BEHIND_FROM_INTERRUPTED_ATTEMPT`
case. Recording this as a real defect caught during required scrutiny, not a
sizing-only overrun — the original implementation's parameterized states only
replayed raw ALTER statements directly and never exercised the guard
procedure's own commit boundaries.

### 3. Preserve a populated notebook through all three migrations
Type: Behavior
Status: done
Behavior: Pre-326 populated schema → actual Flyway 326–328 upgrade → retained
notebook identities/data and final Portable tree match the intended conversion.
Proof: Rewrote `NotebookUpgradeDataPreservationTest` (its prior body only
replayed V300000327's rebuild directly against the already-fully-migrated live
suite schema, so it never exercised the real 326→328 chain) to seed a canonical
fixture via raw JDBC into slice 1's `PreUpgradeFixtureSchema` at version 325,
across three notebooks/owners: Notebook A (bound, live) with nested folders
carrying folder readme content, an existing `_trash` subtree occupying the
exact destination a legacy-deleted note converts to (forcing collision
suffixing to "Old Pasta (2)"), a legacy-deleted note already directly under
`_trash` (deleted_at-clear only, no relocation), authored wiki-link references,
recall history, an independent `removed_from_tracking` preference, and a
pre-existing `notebook_git_binding`; Notebook B (unbound, live); Notebook C
(bound, soft-deleted notebook). Runs the actual registered Flyway chain for
real (326→327, then 328) and asserts full row snapshots against an explicit
documented allowlist (folder_id/title only for legacy-deleted rows, deleted_at
clearing, updated_at bump, new `_trash` roots only for notebooks that lacked
one, rebuilt binding payload for the live bound notebook, untouched binding
for the soft-deleted notebook) — everything else (every memory_tracker row,
every authored_note_reference row, every pre-existing folder including its
readme_content, every untouched note) byte-for-byte unchanged. The rebuilt
notebook's Git tree is independently checked against a hardcoded expected
path/content list (including two `README.md` entries computed via the real
`ExportReadmeMarkdown.assemble(...)`, not hand-derived frontmatter) and
cross-checked against `NotebookGitRebuildTestSupport.currentPortableTreeFromDb`.
Preserve existing folders and binding identity/ownership: confirmed (ids
unchanged, non-touched folders equal before/after). Bound/unbound/deleted-
notebook variations included; the migration's retained-data promise is not
narrowed to live bound notes.
Estimate: 5 minutes active using existing fixture/tree assertions.
Learnings: Active time ran well over target — implementation ~70-80 minutes,
coordinator review ~20 minutes — consistent with the plan's own expectation
that this is "a substantial proof." Coordinator review (delegated
post-change-refactor was not attempted here given the migration-adjacent
content pattern from slice 2; reviewed directly instead) found a real, if
narrower, coverage gap: the initial fixture set every folder's `readme_content`
to NULL, so the before/after row-equality check that is supposed to prove
folder readmes survive the upgrade was trivially true (NULL equals NULL) and
proved nothing — silently narrowing the plan's explicit "nested folders/readmes"
fixture requirement. Fixed by giving two folders real readme content and adding
the corresponding `README.md` tree entries (computed via the real export code
rather than hand-typed frontmatter, to avoid a second, possibly-wrong,
reimplementation of the frontmatter format). Full backend suite green after
the fix.

### 4. Retry an interrupted legacy-trash conversion
Type: Behavior
Status: done
Behavior: Migration connection is interrupted during conversion or after its
commit before Flyway success recording → retry → same retained notes appear
exactly once at their valid trash destinations, without extra suffixes/folders.
Proof: New `NoteLegacyTrashMigrationInterruptionTest` (a focused file, not a
`PreUpgradeFixtureSchema`-alternative harness — reuses slice 1's fixture
schema; separate from `NotebookUpgradeDataPreservationTest` because this
proof's parameterized-interruption-state shape mirrors slice 2's test rather
than that file's single-pass snapshot style). Verified against the actual
source (`NoteLegacyTrashMigration.run`, `V300000326__...canExecuteInTransaction()
== false`) that the whole conversion runs inside one explicit transaction with
exactly one `commit()` at the end and rollback on any exception — so unlike
328's multi-statement DDL, 326 has no reachable "partially committed" state;
it is either entirely committed or entirely rolled back. Two scenarios proved
instead of an unreachable partial-commit case: (1) a second connection
performs writes of the same shape the real conversion makes, with autocommit
disabled, then is discarded without `commit()` — proves MySQL/InnoDB rolls the
whole thing back (no orphaned trash folder, no partial note update) and a real
`repair()+migrate()` retry then completes correctly; (2) the real
`NoteLegacyTrashMigration.run` is invoked directly outside Flyway so the
conversion fully commits but `flyway_schema_history` has no row — mirroring a
process loss after commit but before Flyway's bookkeeping (the 326 analogue of
slice 2's history-unrecorded case) — proving retry is a safe no-op by the
migration's own `deleted_at IS NOT NULL` candidate-selection idempotency. Both
scenarios assert exactly one `_trash` root/child folder (no duplication by
retry), the pre-existing collision note untouched, the collision-suffixed note
titled exactly once ("Draft (2)"), and exactly one successful history row.
Compared against slice 3-style exact-destination assertions. An uncommitted
conversion rolls back: proved directly (not merely asserted).
Command: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
Estimate: 5 minutes active; extend the single fixture, not a second harness.
Learnings: The delegated implementation agent produced complete, correct code
(fixture, both scenarios, assertions) but stalled before running the full
suite or reporting — the harness's own safety classifier was also reported
unavailable for this run, so its output could not be pre-screened. The
coordinator independently verified the transaction-boundary claim against the
actual `NoteLegacyTrashMigration`/`V300000326__...` source before trusting it,
then ran the focused test and full suite directly (both green), and fixed one
cosmetic style issue (a fully-qualified `Matchers.not` instead of a static
import). No functional defect found. Active time: implementation ~40 minutes
before stalling (per its background transcript), coordinator verification and
delivery ~20 minutes.

### 5. Retry a partially rebuilt notebook fleet
Type: Behavior
Status: done
Behavior: At least one notebook rebuild commits and a later rebuild is
interrupted → retry registered 327 and finish 328 → every selected binding
contains a complete tree and matching head with retained notebook data.
Proof: New `NotebookGitBaselineRebuildFleetInterruptionTest`. Verified against
actual source (`V300000327__RebuildNotebookGitBaselines`,
`NotebookGitBaselineRebuild.rebuildNotebook`) that the migration queries every
live bound notebook ordered by `id ASC` and rebuilds each in a plain loop on
one connection, with `canExecuteInTransaction() == false`; each notebook's
rebuild is its own `setAutoCommit(false)` → single `UPDATE
notebook_git_binding` → `commit()`, rollback on exception. So a fleet of N
notebooks is genuinely N independent commits, and the migration keeps no
per-notebook checkpoint — a retry unconditionally re-rebuilds every live bound
notebook (confirmed idempotent by the class's own javadoc and existing
`NotebookGitBaselineRebuildTest` coverage). Three notebooks seeded in
id-ascending fleet order: one whose rebuild is invoked directly outside
Flyway so it genuinely commits before the interruption; one interrupted via a
discarded second connection performing the same single-UPDATE shape with no
commit (InnoDB rolls it back); one left completely untouched
("between notebook baseline rebuilds", the story's key example #2). A real
`repair()+migrate()` retry through 328 then leaves every one of the three with
a complete tree, a recorded head matching the bundle's actual advertised head,
exactly one parentless root commit, and content matching current DB state —
compared via `NotebookGitRebuildTestSupport`/`GitBundleTestReader`, never
against the pre-retry Git SHA or timestamp (a correctly-retried notebook is
expected to get a new head regardless of which state it started from).
Command: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
Estimate: 5 minutes active; no fleet checkpoint table or new retry service —
none was built; the migration's own unconditional-reprocessing design already
satisfies the retry contract.
Learnings: Active time ~30-35 minutes (implementation) plus coordinator
independent source verification and full-suite re-run (~10 minutes) —
consistent with slice 4's investigation-then-proof pattern. No defect found;
the plan's PFE claim about per-notebook transactions was confirmed exactly
against source rather than assumed. This implementation agent, unlike slice
4's, ran and reported the full backend suite itself before ending its turn.

### 6. Complete the upgrade with 10,000 deleted notes
Type: Behavior
Status: done
Behavior: One large notebook with 10,000 legacy-deleted notes plus active content,
and smaller neighboring notebooks → actual full upgrade and one interrupted/retry
run → complete retained content/identities and correct bound Portable trees.
Proof: Batch seed deterministic raw-JDBC data into the same pre-upgrade fixture;
include shared nested parents, existing trash, bounded occupied-name collisions,
long-title suffixing and representative linked learning/reference rows. Compare
all 10,000 note identities/content, all dependent rows seeded, and complete trees;
counts alone are insufficient. Reuse small cases for error permutations instead
of repeating the full matrix at scale. Record rows, elapsed time per migration,
database version, host resources and observed memory/transaction trouble.
Estimate: 5 minutes active plus measured run waiting. Unexpected cost or failure
triggers a bounded reassessment; no invented production latency threshold.
Learnings: New `NotebookUpgradeAtScaleTest` (2 tests) seeds one "Bulk Notebook"
with 10 sibling `Docs/Bucket0..9` folders (1,000 legacy-deleted notes each,
10,000 total), a pre-existing `_trash` subtree with real occupants at 5 of the
10 buckets, 7 ordinary-length title collisions plus one 150-char
(`Note.MAX_TITLE_LENGTH`) title collision forcing the truncate-then-suffix
branch, one note already parked under `_trash` pre-upgrade, a 6-note sample
each carrying one `memory_tracker` and one `authored_note_reference` row, and
two smaller neighboring notebooks (bound-live, bound-soft-deleted) — run
against the actual registered 326→327→328 chain once clean, and once via a
real direct-outside-Flyway `NoteLegacyTrashMigration.run` interruption (full
commit, no Flyway history row) followed by real `repair()+migrate()` retry.
Verified against actual `NoteLegacyTrashMigration` source (not assumed):
`TRASH_ROOT_NAME`, folder-trail reuse across shared nested parents,
`availableTitleAt`'s " (2)" suffix and truncate-at-`MAX_TITLE_LENGTH` logic all
confirmed to match the fixture's expectations before writing assertions.
Every one of the 10,002 seeded legacy-deleted notes (10,000 bulk + 1 per small
notebook) is compared by id/content/identity before and after, not counted;
`memory_tracker`/`authored_note_reference` rows compared byte-for-byte;
Portable trees verified via `currentPortableTreeFromDb`/`readTreeEntries`
against real Git bundle content, not a hand-typed expected list (impractical
at 10,000+ entries). Measured (coordinator-verified by an independent re-run,
not just the implementer's report): MySQL 8.4.11; clean run 326≈3.8s,
327≈220ms, 328≈170ms (total ≈4.2s); interrupted-retry direct 326≈3.9s,
retry repair()+migrate() through 327≈310ms then 328≈170-200ms (total retry
≈500ms); JVM heap ~150-160MB used of 512MB max before and after, no growth,
no transaction/lock trouble on either run. Actual throughput was far better
than the implementer's own pre-measurement estimate (single-digit seconds,
not tens of seconds to minutes) — local MySQL round-trip cost for the
per-note query pattern was lower than expected. Post-change-refactor found
and collapsed a genuine near-duplicate: the new test had reintroduced 9
raw-JDBC fixture-row builder methods byte-for-byte identical to
`NotebookUpgradeDataPreservationTest`'s (slice 3) existing copies; extracted
into a new shared `PreUpgradeFixtureRows` used by both, leaving the
differently-shaped small fixture helpers in the slice 4/5 interruption tests
untouched (distinct signatures, not the same duplication). Coordinator
independently re-verified: cross-checked the fixture's claims against
`NoteLegacyTrashMigration.java` source directly, re-ran the focused test
(matching the implementer's and the refactor agent's reported timings), and
ran the full backend suite twice (before and after the refactor pass) —
both green. Active time ~35-40 minutes across implementation, refactor and
coordinator verification; measured run waiting was on the order of seconds,
not minutes, so no bounded reassessment was triggered.

### 7. Establish a quiescent maintenance state
Type: Behavior
Status: planned
Behavior: Old application processes are serving → enter the one-time maintenance
path → all schema-dependent writers are stopped and verified absent before any
migration is permitted. Account for background writers, not just incoming HTTP.
Change: Add the minimum bounded maintenance operation to existing deployment
ownership; no migration runs merely because a stop request was submitted.
Proof: Existing publication/deployment test fixtures with fake cloud commands
observe stop, wait and quiescence verification in order. Failed verification
prevents migration. This path remains opt-in and cannot release by itself yet.
Estimate: 5 minutes active, reusing existing shell orchestration and test fixtures.

### 8. Keep old writers stopped after replacement or process failure
Type: Behavior
Status: planned
Behavior: Maintenance has been entered and migration is interrupted → lifecycle
replacement, autohealing or an operator retry occurs → old binaries cannot
resume writing. The maintenance state survives loss of the invoking process.
Change: Use existing cloud lifecycle controls for the bounded maintenance window;
do not rely on a shell trap or an HTTP gate to stop background writes.
Proof: Extend the same boundary fixture with replacement/retry variations;
observe that only the selected compatible artifact may subsequently start.
No new durable state service; record the cloud-owned controls in the runbook.
Estimate: 5 minutes active. If the existing lifecycle controls cannot establish
this invariant, stop and reassess the bounded release approach.

### 9. Route normal publication through the protected upgrade
Type: Behavior
Status: planned
Behavior: Application Release selects the portable-trash artifact → publication
→ maintenance protections precede every schema-dependent rollout and routing
cannot reopen early. Ordinary rolling replacement cannot bypass the protection.
Change: Wire slices 7–8 into existing publication ownership for this one release;
preserve selected SHA/artifacts, frontend/CLI publication and release records.
Proof: Extend `scripts/ci/application-release-publication.test.mjs` at its actual
publication entry point to assert the complete order and guard against the normal
rolling route, including retries and conditional backend-deploy skip behavior.
Command: `CURSOR_DEV=true nix develop -c node --test scripts/ci/application-release-publication.test.mjs`.
Estimate: 5 minutes active after 7–8. Local fake-cloud proof does not prove GCP isolation.

### 10. Recover a failed release without reopening an unsafe application
Type: Behavior
Status: planned
Behavior: Backup/preflight, migration or verification fails in the maintenance
path → operator follows recovery → writes remain excluded until either the
verified new state or a verified backup restoration with compatible app exists.
Change: Add a short one-time section to the canonical release runbook with
backup identification and restore rehearsal, pre/post identity/content checks,
actual Flyway/schema preflight, retry states and fresh-checkout communication
that protects unpublished local work. No automatic schema rollback.
Proof: Same publication boundary tests observe failure stays closed and only
successful verification permits reopening; walk its commands against the owned
fixture including restoring backup. Document exact operator inputs and evidence.
Estimate: 5 minutes active plus restore waiting; reuse slices 2–9 observations.

### 11. Rehearse the supported release path on an isolated populated copy
Type: Behavior
Status: planned
Behavior: A production-shaped pre-upgrade copy and old/new release processes →
run the documented maintenance sequence with interruption/retry → only the
compatible app serves the verified retained notebook data after reopening.
Proof: Use the real release/migration entry points on an explicitly disposable
environment. Observe writer exclusion including replacement/autohealing, full
upgrade/restore recovery and final application/Portable-tree checks; retain
sanitized exact commands, versions, row comparisons and timing in this plan.
Use an authorized isolated pre-upgrade production copy when available; if only
synthetic data or fake cloud exists, label that limitation and leave the missing
release proof pending. Do not access or mutate production under this plan.
Estimate: 5 minutes active orchestration after slices 7–10, external waiting
separate. Missing disposable cloud/copy access blocks only this proof; do not
build a general staging platform to get around it.

## Proof ownership and release boundary

| Promise | Owner |
| --- | --- |
| Safe isolated pre-upgrade fixture and actual engine evidence | 1–2 |
| Column/index retry and Flyway history correctness | 2 |
| Authored data, folders, references, learning/preferences and bindings retained | 3 |
| Interruption recovery for conversion and per-notebook rebuild | 4–5 |
| Thousands of deleted notes, complete trees, measured operating cost | 6 |
| Old writers excluded; failures remain in maintenance | 7–10; real observation 11 |
| Backup/restore, row/content comparisons, fresh-checkout guidance | 10–11 |
| Remove all migration-only additions after production success | Story 39; inventory below |

Production release is separate from this implementation plan: use the existing
release-application workflow with an authorized immutable tag/exact tested SHA.
Before release verify owner-confirmed pending migration state in actual Flyway
history/schema and require the missing release proof above. After release record
successful production migration and retained-data/application checks before
activating cleanup. Local green CI is neither deployment nor cleanup permission.

## Temporary removal inventory

Removal owner: [SEED-009 story 39](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-39).
Update exact paths as each slice lands; this is a deletion list, not a framework.

- Existing 326/327/328 versioned migration files; migration-exclusive
  `NoteLegacyTrashMigration` and `NotebookGitBaselineRebuild` implementations.
- Migration-only portions of `NotebookUpgradeDataPreservationTest`,
  `NotebookGitBaselineRebuildTest` and `NotebookGitRebuildTestSupport`; inspect
  callers before removing helpers used by other still-required tests.
- New fixture schema support, historical fixtures, scale case, interruption
  instrumentation and any dedicated task/command (slices 1–6).
- Temporary maintenance guard/path, its migration-only publication tests and
  one-time runbook instructions (slices 7–11); remove after safe retirement.

Story 39 must safely fold final DDL into the existing baseline and deploy/confirm
a new tip above all ever-applied versions before removing old migrations. Fresh
install and already-upgraded startup must both pass afterward. Keep ordinary
trash, learning and Git runtime behavior/tests. Do not remove evidence before
production success or retain dead conversion helpers after safe retirement.

## Execution discipline and assessment

Execute only under a later execution instruction using dough-execute-plan:
claim backlog then establish the story worktree; Jidoka → fresh independent
post-change-refactor agent → API generation only if triggered → coordinator's
one `./scripts/run.sh pnpm format:changed` → plan update → commit/check-only hook
→ push and asynchronous CI observation. Preserve pre-existing planning edits.

Backend migration changes require migration plus the full backend suite. The
documented `pnpm backend:verify` wrapper currently runs a formatter; during slice
execution use its equivalent without that extra formatter: in the story's
isolated worktree run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
(the worktree test task owns migration). If executing in an explicitly selected
unconfigured primary, first run `CURSOR_DEV=true nix develop -c backend/gradlew -p backend migrateTestDB -Dspring.profiles.active=test`, then
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`. Record which actual
command runs slice 2's engine proof. Regenerate the ERD per database-erd guidance
if schema migrations change. No frontend/API change is expected.

Planning inspected code only; all proof results remain pending. The common model
is the existing conversion transaction, per-notebook rebuild transaction, one
schema reconciliation and one maintenance release owner. No per-case production
pipeline.

Refinement: the original rollout slice 7 combined establishing quiescence,
lifecycle protection and publication wiring. Replaced it with slices 7–9 and
renumbered recovery/rehearsal to 10–11, preserving all proof owners. Result:
11 slices, no completed work replaced, no story resplit recommended. The smaller
manual alternative remains preferred where existing cloud controls suffice;
these slices do not require a product maintenance UI or permanent infrastructure.

Slices 1–10 have one bounded proof loop after this refinement and are ready for
direct execution under a separate execution instruction. Slice 2 still carries
the explicit engine-proof gate before broader implementation. Slice 11 is
conditional on a proven disposable environment/copy; its access has not been
established here. Execution has not started and the complete release cannot be
certified safe until all applicable real-environment observations are present.
Sizing exceptions cover test/rehearsal waiting only; active integration overruns
remain subject to the 10-minute refinement limit.
