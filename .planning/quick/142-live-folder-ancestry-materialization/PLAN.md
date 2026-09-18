# Folder ancestry stays inside one notebook, with one live representation

Status: planned
Source: [SEED-030 story 1](../../seeds/SEED-030-folder-ancestry-single-representation.md#1-publishing-notebook-4s-proposal-succeeds-and-folder-ancestry-has-one-representation).
Authority: 2026-09-18 owner instruction, after the production diagnosis: follow
containment, deliver the repair as an ungated SQL migration applied on release,
update this plan, refine if needed, then execute it.

Baseline: `7259de0e50179c1a9e2d8065510dc2a8ce7d34ac` on `main` for the
structural delta. The repair slice starts from current `main`.

## Repair goal and acceptance

Production holds 16 legacy rows whose folder ancestry crosses notebooks (see the
seed's Diagnosis). Any notebook owning a stray **folder** cannot publish at all:
every publication walks every folder row bottom-up and dereferences a parent
that is not in the notebook's rows.

After the repair migration, every folder carries its root ancestor's
`notebook_id` and every note carries its folder's. Notebooks 4, 12 and 86 stop
crashing on publication.

**Owner decisions, 2026-09-18:**

- Direction: follow containment. The row joins its container's notebook. The
  owner accepts the visibility change for pairs 12→4 and 4→26.
- Mechanism: a plain SQL Flyway migration, **not gated**, applied by the next
  release. This overrides the `db-migration` skill's placeholder-gate default
  for one-off DML. It is safe ungated because it is a no-op on any database
  without stray rows and idempotent where they exist.

**Known consequence, accepted with the mechanism and not solved here.** A
notebook that *gains* rows (4, 26, 191, 309 in production) will hold live
content its accepted Git tree lacks. `requireMatchingAcceptedTree` then refuses
publication with 409 projection drift, and `AcceptedWebChangeService` commits
nothing for a notebook that did not match before a change. No recovery path for
drift exists today, and NORTH-STAR says drift is not silently adopted. So this
plan ends notebook 4's crash but does not by itself make notebook 4 publish.
Adoption of the repaired content into those accepted heads is queued as SEED-030
story 2 for an owner decision. Do not add adoption to this plan.

### Storage proof already run (planning, 2026-09-18)

Assumption: MySQL accepts `WITH RECURSIVE … UPDATE folder JOIN <cte>` on the
table the CTE reads, repairs nested stray folders in one statement, and leaves
`updated_at` alone when it is assigned to itself.

Result on local MySQL 8.4.11, scratch database with `CREATE TABLE … LIKE` copies
of `folder` and `note`, then dropped: a stray folder, a consistent folder nested
under it, a note inside the nested folder and a directly stray note all took the
container's notebook. A healthy folder, its note and a root note were unchanged.
Every `updated_at` was preserved. A second run changed 0 rows. Production runs
MySQL 8.4.10.

```sql
WITH RECURSIVE rooted AS (
  SELECT id, notebook_id AS root_notebook_id FROM folder WHERE parent_folder_id IS NULL
  UNION ALL
  SELECT child.id, rooted.root_notebook_id
  FROM folder child JOIN rooted ON child.parent_folder_id = rooted.id
)
UPDATE folder JOIN rooted ON rooted.id = folder.id
SET folder.notebook_id = rooted.root_notebook_id, folder.updated_at = folder.updated_at
WHERE folder.notebook_id <> rooted.root_notebook_id;

UPDATE note JOIN folder ON folder.id = note.folder_id
SET note.notebook_id = folder.notebook_id, note.updated_at = note.updated_at
WHERE note.notebook_id <> folder.notebook_id;
```

`note` and `folder` are the only tables that store a notebook id per row of the
tree. Production showed no title or folder-name collision for this direction.

## Structural goal and acceptance

Folder ancestry used to resolve a materialization destination is derived from
live `Folder` entities instead of `ExportFolderRow` snapshot rows, so a
structural change made earlier in the same publication cannot leave a later
destination resolving against ancestry that no longer describes it.

Observable publication behavior does not change. This is a Structure outcome
with a characterization proof, explicitly selected by the owner as a structural
improvement. No new user-facing Behavior is invented to justify it.

Completion requires all three:

- **Single representation:** `NotebookGitProposalFolderMaterialization` no
  longer accepts a `List<ExportFolderRow>`; it resolves live folders itself and
  computes destination path keys by walking the entity parent chain.
- **Less code:** the row→entity round trip in `foldersByPath` is deleted — the
  `indexFoldersById` build, the per-row `entityPersister.find(Folder.class, …)`
  loop, and the folder-row parameter at all three call sites. Aggregate
  handwritten production additions minus deletions against the baseline must be
  negative after formatting. Do not delete useful tests to reach it.
- **No regression:** the full `NotebookGit*` controller suite stays green, and
  the new characterization test passes both before and after the structural
  change.

Production delta evidence:
`git diff --numstat 7259de0e50179c1a9e2d8065510dc2a8ce7d34ac -- backend/src/main/java`

## Existing solutions and selected design

| Responsibility | Existing owner / evidence | Choice |
| --- | --- | --- |
| Live folder ancestry of one entity | `NotebookGitLivePortablePath.folderPath(Folder)` walks `getParentFolder()`; delivered in `83a7434798` for note paths | Reuse the same walk for folder destination keys. Do not write a second walk. |
| Loading a notebook's folders with parents | `FolderRepository.findByNotebookIdOrderByIdAsc` — `SELECT f FROM Folder f LEFT JOIN FETCH f.parentFolder` returns entities with ancestry already fetched | Call it directly from materialization. It is the same query the rows are already built from, so this adds no round trip. |
| Snapshot ancestry for tree comparison | `NotebookGitAcceptedTree.folderPath(ExportFolderRow, Map)` and `indexFoldersById`, used by projection and proposal acceptance | Keep unchanged. Those callers compare snapshots against snapshots, which is correct. |
| Folder creation | `FolderConstructionService.createFolder`, called by `ensureAncestry` | Unchanged; it remains the creation owner. |

Selected shape: inject `FolderRepository` into
`NotebookGitProposalFolderMaterialization` and drop the `List<ExportFolderRow>`
parameter from both `materialize` and `ensureAncestry`. Both already receive the
`Notebook`, so they can load live folders themselves. `foldersByPath` then maps
each live `Folder` to its path key using the entity walk, with no id index and
no re-find.

This is safe because every current caller already passes rows read immediately
beforehand — `NotebookGitProposalFolderRelocation` returns state carrying
`foldersOf(...)` after reparenting, `NotebookGitProposalDocumentApplication`
re-reads after materializing, and only `applyDeletions` (notes only) runs in
between. Loading inside is therefore never less current than what is passed
today, and the existing relocation test below already demonstrates it.

`folders` locals at the call sites are **not** all dead: in
`NotebookGitProposalFolderRelocation` the same local still feeds
`requireNoUnrepresentedEmptySourceDescendants` and
`requireMatchingAcceptedTree`. Only the argument passed to `ensureAncestry` is
removed. Remove a local only where it genuinely becomes unused.

### Accepted decisions carried

ADR 0002 — Git-native Portable notebook tree synchronization, *Apply one final
projection atomically*: the single final application is mutating, and Portable
paths it resolves must come from live entity state. This plan brings the last
known materialization path into line with that constraint. The ADR text
stating it explicitly is drafted but unaccepted, so treat the constraint as the
direction already delivered for note paths in `83a7434798`, not as an accepted
rule.

## Outside-in proof

Key examples from the seed, with their observable signals at the controller
boundary (`controller.publishNotebookGitProposal`):

1. **Reparent then place under the destination** — **already covered and green
   on the baseline.**
   `NotebookGitComposedFolderRelocationControllerTest.publishesRelocateThenDescendantEditAndAddRetainingIdentitiesWithEarlierParent`
   moves `Topics/` under `Archive/` and, in the same publication, edits
   `Archive/Topics/A.md` and adds `Archive/Topics/Extra.md`. It asserts
   `folders.get(topics.getId()).getParentFolderId() == archive.getId()` and
   `extra.getFolder().getId() == topics.getId()` — the added note lands in the
   relocated folder. This is the decisive signal for this plan; reuse it, do
   not duplicate it.
2. **Create ancestry mid-publication** — a note moves to `New/Deep/note.md`
   where neither folder exists. Signal: both folders exist afterwards and the
   note's folder is `Deep`. Already covered by
   `NotebookGitComposedMoveEditControllerTest.publishesMoveIntoNewlyCreatedFolderAlongsideAnotherNoteEdit`
   and the folder-materialization tests; reuse, do not duplicate.
3. **No folder work** — a content-only edit publishes unchanged. Already
   covered across the `NotebookGit*` suite; reuse.

## Ordered slices

### Slice 1 — Stray folder ancestry follows its containing notebook

Type: Behavior
Status: planned

Behavior: a bound notebook owns a folder whose parent belongs to another
notebook, a note inside that folder, and a note placed directly in a folder of
the other notebook → the repair migration runs → each row carries its
container's notebook, and publishing a plain note edit to the first notebook
succeeds instead of throwing `NullPointerException`.

Add `backend/src/main/resources/db/migration/V300000333__notebook_follows_folder_containment.sql`
holding exactly the two statements proven above. Highest version ever used is
`300000332` (files and git history checked). No placeholder, no profile change.

Proof: one temporary migration test extending `NotebookGitBundleControllerTestBase`.
Build the rows with `makeMe` and corrupt them with native SQL inside
`inCommittedTransaction`, because the builders refuse inconsistent folders.
Snapshot the notebook, confirm the publish throws the production
`NullPointerException` before the migration, execute the migration file's
statements through JDBC, then assert the three rows' notebook ids and that the
same publish returns the proposed head. Follow the retired
`QuestionGenerationBatchFailedRequestPurgeMigrationTest` for loading and running
a migration resource (`git show 9948260f55^:backend/src/test/java/com/odde/donut/services/QuestionGenerationBatchFailedRequestPurgeMigrationTest.java`).
The test is migration-only and is removed after production applies the
migration, per the `db-migration` skill.

Command:
`unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL && CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests '*NotebookFollowsFolderContainmentMigrationTest*'`

Safe stopping point: yes. The migration ships alone.

### Slice 2 — Materialize folder ancestry from live folders

Planning refinement: an earlier draft opened with a slice to write a
characterization test for example 1. Inspecting the suite showed that test
already exists and already asserts the decisive placement, so that slice was
removed rather than duplicating coverage.

Type: Structure
Status: planned

Change `NotebookGitProposalFolderMaterialization`:

- inject `FolderRepository`;
- `materialize(Notebook, List<String> documentPaths, ImportedProposal)` and
  `ensureAncestry(Notebook, List<String> destinationPaths)` — folder-row
  parameter removed;
- `foldersByPath(Notebook)` loads `findByNotebookIdOrderByIdAsc` and keys each
  live `Folder` by its entity-walked path, dropping `indexFoldersById` and the
  `entityPersister.find` loop.

Update the three call sites: `NotebookGitProposalFolderRelocation:96`,
`NotebookGitProposalDocumentApplication:64`,
`NotebookGitProposalOrdinaryNoteApplication:86`. Remove a now-unused local only
where it is genuinely unused; see the warning above.

Reuse `NotebookGitLivePortablePath` for the walk rather than writing a second
one. If reuse needs a folder-shaped entry point there, add it to that class —
it is the existing owner of live-entity path derivation.

Proof: run the decisive existing test **before** editing, to confirm it is
green on the baseline and that this story really is a refactor — if it already
fails, stop and return the story for re-refinement as a live defect:
`unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL && CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests 'com.odde.donut.controllers.NotebookGitComposedFolderRelocationControllerTest'`

Then, after the change, that test again plus the full feature suite
`unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL && CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests 'com.odde.donut.controllers.NotebookGit*'`
— expect 98 classes / 318 tests, 0 failures, matching the baseline. Also record
the production `--numstat` delta and require it negative.

Safe stopping point: yes, once green and committed.

## Current decisions

- Do not touch `ExportFolderRow`, `NotebookGitAcceptedTree.folderPath(row, map)`,
  `indexFoldersById`'s remaining callers, ZIP export, or `LockedNotebookState`'s
  shape. Only live-entity-against-snapshot mixing is in scope.
- Materialization must stay inside the publication transaction so the lazy
  `parentFolder` chain initializes. If a caller outside a transaction is found,
  stop for human judgment rather than adding a guard (ADR 0006: fail loudly).
- Nothing publication accepts today may stop being accepted. A scenario failing
  after the structural slice is a regression, not a new constraint.

## Learnings

None yet.
