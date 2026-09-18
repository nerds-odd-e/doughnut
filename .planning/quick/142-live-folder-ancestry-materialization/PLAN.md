# Live folder ancestry during materialization

Status: planned
Source: [SEED-030 story 1](../../seeds/SEED-030-folder-ancestry-single-representation.md#1-a-publication-that-reparents-a-folder-still-places-content-correctly-under-it).
Authority: 2026-09-18 owner request to refine the first backlog item and, if
scope is clear, write a slice plan. Planning only — this plan does not
authorize execution.

Baseline: `7259de0e50179c1a9e2d8065510dc2a8ce7d34ac` on `main`.
Note: an unrelated uncommitted edit to
`docs/adrs/0002-git-native-portable-notebook-synchronization.md` is awaiting
owner review in the working tree. It is not part of this plan; preserve it.

## Goal and acceptance

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
today. Slice 1 establishes that claim as a test before slice 2 relies on it.

`folders` locals at the call sites are **not** all dead: in
`NotebookGitProposalFolderRelocation` the same local still feeds
`requireNoUnrepresentedEmptySourceDescendants` and
`requireMatchingAcceptedTree`. Only the argument passed to `ensureAncestry` is
removed. Remove a local only where it genuinely becomes unused.

### Accepted decisions carried

ADR 0002 — Git-native Portable notebook tree synchronization, *Apply one final
projection atomically*: the single final application is mutating, and Portable
paths it resolves must come from live entity state. This plan brings the last
known materialization path into line with that constraint. (The ADR text
stating this explicitly is the uncommitted edit awaiting owner review; the
constraint is treated here as the direction already delivered for note paths,
not as an accepted rule.)

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

Planning refinement: an earlier draft opened with a slice to write a
characterization test for example 1. Inspecting the suite showed that test
already exists and already asserts the decisive placement, so that slice was
removed rather than duplicating coverage. One slice remains.

### Slice 1 — Materialize folder ancestry from live folders

Type: Structure
Status: not started

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
  after slice 2 is a regression, not a new constraint.

## Learnings

None yet.
