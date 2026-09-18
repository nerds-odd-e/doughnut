---
id: SEED-030
status: refined
planted: 2026-09-18
planted_during: Portable-path representation repair after a publication NullPointerException
trigger_when: a publication reparents a folder while also placing or moving content under it
scope: S
---

# SEED-030: One representation of folder ancestry during publication

## Why This Matters

A notebook owner publishing from a local checkout expects every note and
container in the proposal to land under the folder the proposal shows, even
when the same publication reorganizes folders.

Publication holds folder ancestry in two representations: live `Folder`
entities, and flat `ExportFolderRow` snapshot rows. Mixing them across a
mutation point already produced one defect: a note moved into a folder that
the same publication had just created had no row in the snapshot, and path
resolution dereferenced null. That path was repaired by deriving a live note's
Portable path from the note's own folder ancestry.

One instance of the mixing remains, in
`NotebookGitProposalFolderMaterialization.foldersByPath`. It builds a map whose
**key** is a path computed from snapshot rows and whose **value** is the live
`Folder` entity re-loaded by id.

## Resolved: the mixing is latent, not live

Tracing every caller answers the question this seed was planted with. There is
**no currently reachable scenario in which the key is stale**, because the rows
are re-read immediately before each materialization:

- `NotebookGitProposalFolderRelocation` reparents the source folder, then
  returns state carrying `notebookGitStateLoader.foldersOf(...)`.
- `NotebookGitProposalDocumentApplication` re-reads `stateLoader.foldersOf(...)`
  after materializing.
- Between `published.folders()` and `ensureAncestry`, only `applyDeletions`
  runs, and it removes notes, never folders.

So this story fixes no reported or reproducible failure. Its value is that the
guarantee currently rests on every caller remembering to re-read at the right
moment — the same property that produced the original defect — rather than on
the code being unable to express the wrong thing.

The concrete waste is visible: `FolderRepository.findByNotebookIdOrderByIdAsc`
already returns `List<Folder>` with `LEFT JOIN FETCH f.parentFolder`,
`NotebookExportRows.folders` flattens those entities into rows, and
`foldersByPath` then rebuilds an id→row index to recompute ancestry **and
re-finds every `Folder` by id**. Entities become rows and then entities again,
once per folder, to answer a question the original entities already answered.

## Alternatives and Decision

**Recommended direction:** materialization resolves folder ancestry from live
`Folder` entities only. `ensureAncestry` and `materialize` already receive the
`Notebook`; they can load live folders themselves and drop the folder-row
parameter entirely. Path keys come from walking the entity's parent chain, the
same walk `NotebookGitLivePortablePath` already uses for notes.

This is safe precisely because of the resolved finding above: every caller
already passes rows read immediately beforehand, so loading live folders inside
is never less current than what is passed today. It also replaces N `find`
calls with the one query that already exists.

**Strongest simpler alternative:** leave it and rely on callers continuing to
re-read at the right moment. Cheaper, correct today, but keeps a correctness
property that no test states and that a future caller can silently break.

**Rejected:** a general removal of `ExportFolderRow`. The row-based
`folderPath` is correct where its callers compare snapshots against snapshots
(accepted tree against proposed tree), and export/ZIP use of the rows is
unrelated. Only live-entity-against-snapshot mixing is in scope.

## Story Decomposition

### 1. A publication that reparents a folder still places content correctly under it

- **Goal:** for the notebook owner who reorganizes folders and places content
  in one publication, keep content landing under the folder the proposal shows,
  and make that guarantee a property of the code rather than of caller
  ordering. Contributes to the Git-backed authoring goal by removing a way
  publication can silently attach content to ancestry that no longer describes
  it.

- **Scope — required behavior:**
  - Observable publication behavior is unchanged. This is a structural change
    with a characterization proof, not a behavior change.
  - Folder ancestry used to resolve a materialization destination is derived
    from live `Folder` entities, not from `ExportFolderRow` snapshot rows.
  - `NotebookGitProposalFolderMaterialization` no longer receives a folder-row
    list; the row→entity re-find loop in `foldersByPath` is gone.

- **Scope — deferred promises:** no change to `ExportFolderRow` itself, to
  `NotebookGitAcceptedTree.folderPath(row, map)`, to accepted-tree or
  proposed-tree comparison, to ZIP export, or to `LockedNotebookState`'s shape.
  No new owner-facing capability, and no new publication scenario is admitted.

- **Scope — justified rejection:** none. Nothing that publication accepts today
  may stop being accepted; there is no product rule here that forbids a case.
  A scenario failing after this change is a regression, not a new constraint.

- **Boundary assumption:** materialization runs inside the publication
  transaction, so walking a lazy `parentFolder` chain initializes rather than
  throwing. This already holds for the note-path walk delivered earlier. If a
  caller outside a transaction is found, stop and resolve it rather than adding
  a guard.

- **Key examples:**
  1. *Reparent then place under the destination.* Given a notebook with folders
     `Alpha/` and `Beta/`, when one publication both moves `Alpha/` under
     `Beta/` and adds a note at `Beta/Alpha/note.md`, then the published tree
     places that note under the relocated `Alpha`, and the note's folder is the
     moved `Alpha` folder. This is the scenario the mixing could break; it must
     pass before and after.
  2. *Create ancestry mid-publication.* Given a notebook with a note at
     `note.md`, when one publication moves it to `New/Deep/note.md` where
     neither folder exists, then both folders are created and the note sits
     under `Deep`. Covers destination ancestry built entirely during the
     publication.
  3. *No folder work.* Given a publication that only edits note content, then
     it publishes exactly as before, with no folder created and no change to
     the accepted tree beyond the edit. Guards against the parameter removal
     changing the common path.

- **Evaluation:** example 1 is **already covered** by
  `NotebookGitComposedFolderRelocationControllerTest.publishesRelocateThenDescendantEditAndAddRetainingIdentitiesWithEarlierParent`,
  which relocates `Topics/` under `Archive/` and, in the same publication, adds
  `Archive/Topics/Extra.md`, asserting the added note's folder is the relocated
  `Topics`. Run it against current `main` first and confirm it passes; a green
  result is the required starting evidence that this is a refactor. If it
  fails, the finding is a live defect and the story stops for re-refinement
  rather than proceeding as Structure work. Then make the structural change and
  require the full `NotebookGit*` controller suite to stay green.

- **Value / learning:** turns a correctness property maintained by convention
  into one the code enforces, and confirms whether the reparent-then-place
  scenario is covered at all today.

- **Effort hypothesis:** S — one class plus three call sites, mirroring a change
  already made in the same subsystem. Confidence moderate; the sizing risk is
  how `NotebookGitProposalFolderRelocation` obtains live folders.

- **Depends on:** none.

- **Safe stopping point:** if the characterization test passes and the
  structural change proves larger than expected, keep the test and stop. The
  test alone retains value by stating the guarantee that is currently unstated.

## Ordering and Scope Reduction

Preventive work; drop it first if capacity is short, since no owner has
reported a failure. Do not extend it into a general `ExportFolderRow` rewrite.

## Open Decisions

None remaining. The staleness question is resolved above; the sizing question
about `NotebookGitProposalFolderRelocation` is an implementation detail for
planning, not a product decision.

## When to Surface

Selected. When further Git-backed publication work touches folder relocation or
subtree composition, or if an owner reports content landing under an unexpected
folder after a publication that also moved folders.
