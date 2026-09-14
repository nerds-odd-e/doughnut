# Recover production note-title uniqueness

## Source

- User-authorized recovery of the 2026-09-14 production outage described in the incident debrief at `/Users/terryyin/.codex/attachments/1e59d483-f060-4030-b9fc-83078a5fccb4/pasted-text.txt`.
- Original instance `doughnut-db-instance` crash-loops while rolling back transaction `117411499` through functional index `uk_note_notebook_folder_title`.

## Goal and scope

Restore production to the latest safe committed state before the first observed crash, while preserving the product rule that note titles are unique case-insensitively within a notebook folder or notebook root. Prevent a reboot from routing the application back to the unsafe original database.

Included:

- Isolated PITR at `2026-09-14T05:08:30Z`, before the first observed crash at `05:08:59Z`.
- Data-preserving cleanup of historical soft-deleted memory trackers before the already-pending plain uniqueness migration.
- Replacement of functional note-title index key parts with stored normalized columns under the existing constraint name.
- Persistent production database routing, release, and public smoke verification.

Excluded:

- Repairing transaction `117411499` in the original instance.
- Deleting either existing database instance.
- Redesigning other functional indexes without incident evidence.
- Independent CLI release mechanics, which remain owned by the existing release request.

Assumptions:

- Cloud SQL PITR replays committed binary-log transactions and therefore omits the uncommitted crash transaction.
- Stored generated columns preserve the current `IFNULL(notebook_id, 0)`, `IFNULL(folder_id, 0)`, and binary-collated `LOWER(title)` uniqueness semantics without functional index key parts.
- The isolated target remains outside production traffic until every validation succeeds, following ADR 0007, `docs/adrs/0007-environments-and-isolation-accepted.md`.

## Existing-solution assessment

The schema contains no stored generated-column precedent for this responsibility. The current functional index is the failed representation. `note_alias_index.alias_lookup_key` demonstrates an explicit normalized persistence key, but its lifecycle is application-managed and does not fit note-title atomicity. Add database-managed stored normalized columns so one database transaction continues to own title uniqueness.

## Outside-in proof

| Promise | Owner | Proof |
| --- | --- | --- |
| Historical soft-deleted tracker duplicates do not block migration 323 | Slice 1 | A populated upgrade test first reproduces the production duplicate and then proves its recall history and batch-request dependents are reparented to the active tracker before only the obsolete soft-deleted copy is removed |
| Duplicate note titles remain rejected at root and within a folder, including case variants | Slice 2 | Existing `NotebookNoteCreateControllerTest` behavior plus a schema guard proving an ordinary-column unique index |
| The recovered database excludes the failed undo transaction and contains committed pre-crash state | Slice 3 | Cloud SQL health/log checks, Flyway tip, row counts, duplicate-key query, and application read/write rehearsal while isolated |
| VM reboot cannot route production back to the unsafe database | Slice 3 | Committed Salt/startup configuration resolves `db-server` to the validated target, followed by MIG rollout and public smoke checks |

## Ordered slices

### 1. Repair historical memory-tracker twins before the pending migration

Type: Behavior
Status: done
Proof: On the isolated PITR target, inventory the exact twin and child-row counts, run one transactional repair that reparents all three child tables and deletes only a soft-deleted tracker with an active twin, then verify zero duplicate logical keys, unchanged child counts/IDs, and successful unchanged migrations 323–328.

Behavior: A pre-release production snapshot contains 27 logical tracker keys with one active and one historical soft-deleted row each → move recall logs, recall prompts, and batch requests from the historical twin to the active tracker, then remove only the historical row before version 323 drops `deleted_at` → the active tracker and all dependent learning history survive under the intended plain unique constraint.

### 2. Preserve title uniqueness without functional index key parts

Type: Structure
Status: done
Proof: A persistence test first fails against the current expression index, then passes after a new idempotent Flyway migration replaces it with stored normalized columns; all backend tests remain green.

Internal change: keep `uk_note_notebook_folder_title` and its observable conflict behavior, but make its indexed parts ordinary stored columns so rollback does not evaluate hidden functional key parts. This directly corrects the evidenced persistence hazard while preserving controller behavior.

### 3. Recover and persist production on the safe database

Type: Behavior
Status: in progress
Proof: Given the original database crash loop and an isolated pre-crash PITR target, applying the tested index correction and routing configuration results in a healthy database, a stable MIG, and HTTP 200 production health/read smoke checks after rollout.

Behavior: Production is unavailable because instances resolve the unsafe original database → validate the PITR target, apply the tested index replacement before application migrations, persist its private IP in infrastructure, release the tested correction, and roll out → production remains healthy through a fresh VM startup and retains case-insensitive duplicate-title rejection.

## Current decisions

- Use PITR timestamp `2026-09-14T05:08:30Z`; preserve the partially migrated first target as evidence and cut over only to the clean replacement target `doughnut-db-pitr-050830b` at private IP `10.111.16.24` after validation.
- Preserve the constraint name because API error mapping depends on it.
- Replace `fk_note_folder` `ON DELETE SET NULL` with fail-loud `ON DELETE RESTRICT`; every application folder-deletion path already relocates or detaches notes before deletion, and MySQL 8.4 prohibits a generated column that depends on a foreign-key column with `SET NULL`.
- On the clean target, repair the tracker twins first, run unchanged migrations 323–328, then apply the exact tested version 329 schema correction while the database remains isolated. The originally preferred index-first order was not used on the clean target because the available migration-only v1.3.3 jar does not contain version 329; the intervening migrations completed, the index replacement completed, and subsequent table/rollback/log checks were clean.
- Keep committed Flyway version 323 immutable. The repository's migration policy classifies this historical destructive cleanup as a one-time operation: on the isolated target, transactionally reparent dependents and delete only a soft-deleted tracker that has an active twin with the same logical key, verify the selection and child preservation, then run unchanged version 323.
- Do not route traffic or delete databases until isolated validation passes.

## Learnings

- PITR operation `4fd1535b-792c-4d40-8d57-9a2b00000026` started at `2026-09-14T11:18:27Z`.
- The source retains binary logs for the selected timestamp.
- MySQL 8.4 rejects a stored generated key derived from `note.folder_id` while `fk_note_folder` uses `ON DELETE SET NULL` (error 1215). `FolderRelocationService.dissolveFolder` and `EmptyFolderBulkPurge` explicitly move or detach notes before deleting folders, so `RESTRICT` preserves application behavior and makes an unexpected direct delete fail loudly.
- PITR completed at `2026-09-14T11:28:56Z`; target `doughnut-db-pitr-050830` is MySQL `8.4.10-google` at private IP `10.111.16.21`. It has Flyway tip `300000322`, 26,744 notes, 7,820 folders, no duplicate normalized note keys, `CHECK TABLE note` reports OK, and no crash signature.
- The isolated Flyway run failed immediately at version 323, not from duration: 27 logical `memory_tracker` keys each have exactly one active and one soft-deleted row. The existing migration's claim that this state was unreachable is contradicted by the production snapshot. Because MySQL committed its first two DDL statements before the unique-index failure, a fresh PITR target is required after correcting the migration path.
- The historical tracker twins are not empty residue: their soft-deleted rows own 44 `recall_log` and 47 `recall_prompt` rows. Those foreign keys cascade on tracker deletion, so the correction must reparent the dependents to each active twin before removing the historical tracker. No `question_generation_batch_request` rows currently point at the historical twins, but the migration covers that dependent too and lets a conflicting batch/tracker uniqueness state fail loudly.
- A second clean restore, `doughnut-db-pitr-050830b`, completed operation `60e10d96-c544-4d8f-9b62-c6e900000026` at `2026-09-14T12:04:16Z`; it is RUNNABLE at private IP `10.111.16.24`.
- The one-time tracker repair selected exactly 27 active/historical pairs, found zero batch conflicts, reparented 44 recall logs and 47 recall prompts, reparented zero batch requests, and deleted exactly 27 historical trackers. Total child counts and ID sums were unchanged and zero duplicate logical tracker keys remained.
- The unchanged v1.3.3 migration-only runner completed versions 323–328 on the clean target even though the autohealer closed the SSH session immediately afterward; `flyway_schema_history` independently confirmed all six successful rows. Version 326 created the expected trash-folder structures, changing folder count from 7,820 to 8,838 while note count remained 26,744.
- The tested version 329 DDL then replaced all three functional index expressions with the three ordinary stored generated columns, preserved the unique constraint name, changed `fk_note_folder` to `RESTRICT`, and left zero duplicate note keys. `CHECK TABLE note EXTENDED` returned OK. A real note update/rollback probe restored both folder and title, leaked zero probe titles, and Cloud SQL logs contained no error/crash signature.
- Metadata-driven routing tests passed: `production-database-routing.test`, `mig-startup-java-command.test`, and `deploy-backend-jar-to-gcp-mig.sh.test`. A fresh post-refactor `CURSOR_DEV=true nix develop -c pnpm backend:verify` passed migration plus all 2,418 backend tests.
- The database ERD exporter used `doughnut_development`; `docs/database-erd.md` was already current and did not change. `./scripts/run.sh pnpm format:changed` completed successfully once after refactoring/generation.
- Production public health returned HTTP 200 after routing the current instance to the repaired database. MIG template `doughnut-app-debian12-zulu25-openai-mig-template-1789390278` is configured with `DATABASE_PRIVATE_IP=10.111.16.24`; durable source delivery, exact-SHA CI, and the forward application release remain.

## Accepted proof

- Tracker repair boundary: production `memory_tracker` plus its three direct child tables on isolated `doughnut-db-pitr-050830b`. Setup was the read-only exact-pair/FK inventory; observation was guarded affected counts `27/0/44/47/0/27`, preserved child counts and ID sums, and zero duplicate groups after commit.
- Note-index boundary: `V300000329__ReplaceNoteTitleFunctionalIndex` and `NoteTitlePersistenceTest`. The schema test inspects generated-column expressions, ordinary unique-index parts, and the `RESTRICT` FK. Literal proof: `CURSOR_DEV=true nix develop -c pnpm backend:verify` passed all 2,418 tests after refactor.
- Routing boundary: the single IP config, reader, template creators, startup `/etc/hosts` replacement, and absence of the Salt-baked route. Literal proofs: `CURSOR_DEV=true nix develop -c bash scripts/test/production-database-routing.test`, `... mig-startup-java-command.test`, and `... deploy-backend-jar-to-gcp-mig.sh.test`, all passed.

## Execution identity

- Originating checkout/branch: `/Users/terryyin/git/doughnut`, `main`.
- Execution checkout/branch: `/Users/terryyin/git/doughnut-recover-production-note-title-index`, `codex/recover-production-note-title-index`.
- Integration target: `main`.
- CI observer: repository `nerds-odd-e/doughnut`, destination branch `main`, workflow `ci.yml` / `donut CI`, mailbox `/tmp/dough-ci-501/watch-ppHCEN`, PID `10423`, yielded cell `292`, session `52932`.
