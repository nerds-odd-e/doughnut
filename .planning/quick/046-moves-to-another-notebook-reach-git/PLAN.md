# Moves to another notebook reach both notebooks' Git

Work item: **SEED-035#story-24**.
Source: [refined story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-24)
(owner decisions 2026-09-26).

## Goal and scope

When an owner moves a note or a folder to another notebook on the web, the
source and the destination notebook each get one accepted commit in the same
transaction. A later local publish of either notebook then passes the
accepted-tree check instead of being refused for good. The link rewrites the
move already makes, and undo (which sends the same move again), land the same
way.

Excluded (see the story): commits for linking notes in a third notebook
(story 25); carrying a picture file or a folder's files to the other notebook
(story 10; folders with files stay refused, a picture note's file stays in the
source); repairing notebooks already out of step; moving several folders at
once.

Start execution after story 23 (plan 042, recoverable at `77ceb41056:.planning/quick/042-moved-note-keeps-its-picture/PLAN.md`)
lands on main: both change the note move code, and the decision below depends
on its result.

## Architecture

- **PFE:**
  - Owner: `AcceptedWebChangeService.apply(Set<Integer>, …)` already locks
    several notebooks in ascending id order and appends one commit per changed
    notebook. `RelationReduceService` uses it this way. No new owner or
    transaction path is needed
    ([synchronization contract](../../../docs/notebook-git-synchronization.md#domain-operation-ownership)).
  - Today the cross-notebook paths bypass it:
    `NoteMoveService.moveCrossNotebookToFolder` and
    `moveCrossNotebookToNotebookRoot` (plain `@Transactional`, called from
    `RelationController`), and `FolderRelocationService.moveFolder` →
    `FolderMoveRelocation.moveFolderToAnotherNotebook`. The same-notebook
    paths already go through the owner, via `WebNoteEditService.edit` and
    `FolderRelocationService.applyLiveFolderChange`, each re-checking the
    live row inside the operation.
  - **Gap in change capture:** `ProjectionChangeCapture.onFlushDirty` files an
    updated row only under its *previous* notebook. `NotebookGitChangedFiles`
    then removes the old path and writes the row at its current path into
    that same tree, which would put the arrived note into the source's tree
    and leave the destination unchanged. The one rule to add: **a row whose
    notebook changes leaves the old notebook (recorded as deleted there, at
    its previous path) and arrives in the new one (recorded as inserted
    there).** Derivation already handles deleted and inserted notes and
    folders. A deleted folder removes its previous subtree
    (`NotebookGitChangedFolders.relocate`), and an inserted folder gets its
    directory and readme. So `NotebookGitTreeEncoder` needs no change.
  - Link rewrites already run inside the move (`WikiLinkRelocationRewrite`).
    Inside the owner, their rows are captured under their notebook. Source
    and destination referrers therefore land in those notebooks' commits, and
    third-notebook referrers get no commit because those notebooks are not
    opened (story 25).
- **Keep the cross-notebook steps separate from the same-notebook steps.**
  After story 23 the same-notebook steps carry the picture file.
  Carrying it to another notebook is story 10, because LFS objects are scoped
  to one notebook and would have to be copied. Share only the transaction
  owner, not the move steps.
- **Notebook set:** {source notebook, destination notebook}, taken from the
  request's rows before locking. Inside the operation, re-read the live rows
  and refuse when the note or folder is no longer in the source notebook or
  the target folder is no longer in the destination. Use the same live
  re-check the same-notebook paths use (`WebNoteEditService.edit`'s
  notebook check, `Folder.requireInNotebook`). Only a concurrent move reaches
  this, so it gets no test of its own.
- **Complexity:** this removes the two plain `@Transactional` cross-notebook
  entry points and the `NoteMoveService` javadoc sentence saying they are not
  Git-synchronized. It adds one capture rule and no new classes. Report the
  net line delta at the end.

## Key examples → proof

| Promise (story key example) | Slice | Proof |
| --- | --- | --- |
| 1. `Science/physics/Force` moved into `Engineering/mechanics` → gone from `Science`, present in `Engineering`, one commit each, both trees match the full assembly (so publish passes), same note id | 2 | `NotebookGitWebNoteCrossNotebookMoveControllerTest` |
| Same, to the other notebook's root | 2 | `NotebookGitWebNoteCrossNotebookMoveControllerTest` |
| 2. `Energy`'s `[[Force]]` → `[[Engineering:Force\|Force]]` in `Science`'s tip; `Force`'s `[[Energy]]` → `[[Science:Energy\|Energy]]` in `Engineering`'s tip | 2 | same test, tip file content |
| 4. `Force` has `image: force.png` → `image:` unchanged, `physics/force.png` stays in `Science`'s tip | 2 | same test |
| 6. Undo = moving `Force` back into `Science/physics` → back in `Science`, gone from `Engineering`, both match the full assembly | 2 | same test |
| 3. Folder `physics/` with `waves/Sound.md` moved to `Engineering`'s root → `Science` has no `physics/`, `Engineering` has `physics/waves/Sound.md`; merge into an existing `Engineering/physics/` puts `Sound` there | 3 | `NotebookGitWebFolderCrossNotebookMoveControllerTest` |
| 5. Folder with `paper.pdf` refused, neither head changes | 3 | existing `NotebookGitWebFolderCrossNotebookMoveControllerTest` cases stay green |

## Slices

### 1. Change capture records a move to another notebook as leaving and arriving
Type: Structure
Status: planned
Proof: existing behavior stays green:
`NotebookGitDerivedTreeOracleControllerTest`,
`NotebookGitDerivedFolderTreeOracleControllerTest`,
`NotebookGitWebRelationReduceControllerTest`, `NotebookGitTreeEncoderTest`.

Change: in `ProjectionChangeCapture.onFlushDirty`, when a note, folder or
attachment row's `notebook` differs between previous and current state,
record it as deleted under the previous notebook, with its previous path, and
as inserted under the current notebook. Otherwise record it as an update, as
today. A row that left a notebook is not also an update there. If an earlier
flush in the same window already recorded it as updated under the old
notebook, that record becomes the deletion and keeps its first-seen previous
path. This matters for the folder merge, which may flush a row's folder
change before its notebook change. No operation inside a capture window changes a row's notebook yet, so
behavior is unchanged.
Enables slice 2.

### 2. A note moved to another notebook reaches both notebooks' Git
Type: Behavior
Status: planned
Proof: new `NotebookGitWebNoteCrossNotebookMoveControllerTest` (on
`NotebookGitWebNoteMoveTestBase` or `NotebookGitWebContentControllerTestBase`,
using `createGitBackedNotebook`, `storeFolderAttachmentAndSnapshot`,
`acceptedHistory(...).tipPaths()` and `assertAcceptedTreeMatchesTheFullAssembly`),
through `RelationController`:
- The move into a folder and the move to the other notebook's root each
  append exactly one commit to both notebooks. Check the tip paths and the
  moved note's id, and check that both trees match the full assembly.
- In the folder-move case, assert the rewritten links in both tips, that
  `image: force.png` is unchanged, and that `physics/force.png` stays in the
  source tip.
- Moving `Force` back into `Science/physics` (what undo sends) restores it in
  `Science`, removes it from `Engineering`, and both trees match the full
  assembly.

Behavior: `RelationController`'s two cross-notebook branches run
`NoteMoveService`'s cross-notebook steps inside
`AcceptedWebChangeService.apply({source, destination}, …)` with the live
re-check. The commit message is "Move note: <title>", as in the same-notebook
path. Remove the plain `@Transactional` and the "not Git-synchronized"
javadoc.

### 3. A folder moved to another notebook reaches both notebooks' Git
Type: Behavior
Status: planned
Proof: `NotebookGitWebFolderCrossNotebookMoveControllerTest` (existing
refusal cases stay green), new cases through `NotebookFolderController.moveFolder`:
- `physics/` holding `waves/Sound.md` is moved to `Engineering`'s root. Each
  notebook gets one commit, `Science`'s tip has no `physics/` path,
  `Engineering`'s tip has `physics/waves/Sound.md`, and both trees match the
  full assembly.
- With merge into an existing `Engineering/physics/`, `Sound` lands in that
  folder and both trees match the full assembly.

Focused regression, since the moves now run `SERIALIZABLE` under the owner:
`pnpm cy:run --spec e2e_test/features/note_topology/note_move.feature,e2e_test/features/note_topology/wiki_link_move.feature,e2e_test/features/folder_organization/folder_organization.feature`
(scenarios unchanged).

Behavior: the cross-notebook branch of `FolderRelocationService.moveFolder`
goes through the owner over {source, destination} with the live-folder
re-check (extend `applyLiveFolderChange` to a notebook set rather than adding
a second wrapper). Remove its plain `@Transactional`. In
`docs/notebook-git-synchronization.md` (domain operation ownership), change
"Cross-notebook move and cross-notebook referrer rewrites remain outside this
owner" so that cross-notebook moves are covered and only rewrites of links in
notebooks outside the changed set remain outside.

## Current decisions

- No new end-to-end scenario. The frontend is unchanged, and pull fidelity
  follows from the accepted tree, as the existing CLI move scenario shows.
  `assertAcceptedTreeMatchesTheFullAssembly` makes the same comparison as the
  local publish check (`NotebookGitProjection.requireMatchingAcceptedTree`),
  so the controller tests are the outside-in proof that publishing is no
  longer blocked.
- A notebook without a Git binding is skipped by the owner, as today.

## Learnings

None yet.
