# A moved note keeps its picture

Work item: **SEED-035#story-23**.
Source: [refined story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-23)
(owner decisions 2026-09-26, option A).

## Goal and scope

When an owner moves a note within its notebook on the web (into a folder or to
the notebook root), the file its `image:` names directly in the note's folder
comes along in the same accepted change, so the picture keeps showing and a
pull finds note and picture together. A taken destination name gives the file
the first free name and rewrites `image:` to it; a file another note in the
source folder also names is copied instead of moved.

Excluded (see the story): moves to another notebook (stories 24 and 10), trash
and undo-trash, `image:` values that are URLs, absolute paths, paths into other
folders or missing files, references from other folders, `..` support on the
web, and any other `image:` rewrite.

The one set of names per folder is on main (checked 2026-09-26 before
execution): `FolderSiblingNameValidation.entryHolding` answers "is this
filename taken in the destination" (live note, folder and file rows, ignoring
letter case) and `NumberedNameSelection.firstAvailableFilename` gives the first
free filename (Book file placement already combines the two). Use them; add no
second name check.

## Architecture

- **PFE:**
  - The move path is `RelationController.webMove` →
    `WebNoteEditService.edit` → `AcceptedWebChangeService.apply` → the
    `NoteMoveService.sameNotebookMoveIntoFolder` and `sameNotebookMoveToRoot`
    steps. Trash (`executeMoveIntoFolderWithAvailableTitle`) and undo
    (`executePlacement`) call `NoteMotionService` directly. So the picture
    carry belongs to the two `NoteMoveService` steps, which keeps trash and
    undo out without a flag.
  - `NoteFolderAttachment.at(note, image)` already finds the file an `image:`
    names. Call it before placement, while the note is still in its old folder.
    Only a plain filename (`NotebookGitPortablePath.isPlainFilename`) qualifies.
  - Folder dissolve and merge already carry a file by changing its row's folder
    and merging it (`FolderSubtree`); the picture carry moves its row the same
    way.
  - `NoteContentMarkdown.withNoteImage` plus
    `AuthoredNoteDocumentPersistence.persist` already rewrite `image:`, as
    picture upload does. Add a reading counterpart beside `withNoteImage`
    (frontmatter `getString("image")`).
- **No new Git work:** `ProjectionChangeCapture` records a `NotebookAttachment`
  whose folder changed (the old path is removed and the new one written) and an
  inserted row. `NotebookAttachment` holds only the LFS pointer bytes, so a
  copy is a new row with the same `acceptedGitContent`, and no bytes are
  stored again ([North Star: one store for file bytes](../../NORTH-STAR.md#one-store-for-file-bytes)).
  Each slice proves this with `assertAcceptedTreeMatchesTheFullAssembly`.
- **Naming:** Donut chooses the name and writes the only reference in the same
  change, so a taken name gets the first free name
  ([North Star: one way in](../../NORTH-STAR.md#one-way-in)).
- **One rule:** "the moved note's picture file lands in the destination under a
  free name; the source file stays only while a sibling note still names it."
  Slices 2 and 3 widen this one rule; they are not separate handlers.

## Key examples → proof

| Promise (story key example) | Slice | Proof |
| --- | --- | --- |
| 1. Move `Force` (`image: force.png`) into `mechanics/` → `mechanics/force.png`, same pointer, no `physics/force.png` | 1 | `NotebookGitWebNoteMovePictureControllerTest` |
| Same, moved to the notebook root | 1 | `NotebookGitWebNoteMovePictureControllerTest` |
| 5. Trashing `Force` leaves `physics/force.png` | 1 | `NotebookGitWebTrashControllerTest` |
| 2. `mechanics/Force.png` exists → `mechanics/force (2).png`, `image: force (2).png`, `Force.png` untouched | 2 | `NotebookGitWebNoteMovePictureControllerTest` |
| 3. `Energy` also names `force.png` → both files, both notes show their picture | 3 | `NotebookGitWebNoteMovePictureControllerTest` |

Key example 4 (URL `image:`) needs no proof of its own: `NoteFolderAttachment`
finds no file, so the existing move tests cover it.

## Slices

### 1. A moved note's picture moves with it
Type: Behavior
Status: done (accepted proof: `backend:test:worktree --tests
'*NotebookGitWebNoteMove*'`, `'*NotebookGitWebTrashControllerTest*'`)
Proof: new `NotebookGitWebNoteMovePictureControllerTest` (on
`NotebookGitWebNoteMoveTestBase`, seeding the file with
`storeFolderAttachmentAndSnapshot`):
- Moving `physics/Force` with `image: force.png` into `mechanics/` leaves an
  accepted tree with `mechanics/Force.md` still saying `image: force.png`,
  `mechanics/force.png` holding the same pointer blob, and no
  `physics/force.png`, in one accepted commit, and the tree matches the full
  assembly.
- A move to the notebook root does the same.
- When `mechanics/` already has an entry named `force.png` (any letter case),
  the move is refused with the existing `RESOURCE_CONFLICT` naming that path
  (`FolderSiblingNameValidation.refuseTaken`), and nothing changes. This is an
  interim refusal from the shared name rule; slice 2 replaces it.

`NotebookGitWebTrashControllerTest`: trashing `Force` keeps
`physics/force.png`.

Behavior: both `NoteMoveService` same-notebook steps find the picture file
before placement and move its row to the note's new folder after placement.
Update `docs/notebook-git-attachments.md` ("Web rename, move within the
notebook…") to say a moved note carries its picture file.

### 2. A taken name gives the picture the first free name
Type: Behavior
Status: done (accepted proof: `backend:test:worktree --tests
'*NotebookGitWebNoteMove*'`)
Proof: `NotebookGitWebNoteMovePictureControllerTest`: `mechanics/` holds
`Force.png`. Moving `Force` gives `mechanics/force (2).png` with the same
pointer and `mechanics/Force.md` saying `image: force (2).png`;
`mechanics/Force.png` is unchanged, in one accepted commit matching the full
assembly. This replaces slice 1's refusal case.

Behavior: the carry asks `NumberedNameSelection.firstAvailableFilename` over
`FolderSiblingNameValidation.entryHolding` for the first free filename and,
when it differs, rewrites `image:` through `withNoteImage` and the authored
document persistence in the same change.

### 3. A picture another note uses is copied, not moved
Type: Behavior
Status: done (accepted proof: `backend:test:worktree --tests
'*NotebookGitWebNoteMove*'`, `'*NotebookGitWebTrashControllerTest*'`)
Proof: `NotebookGitWebNoteMovePictureControllerTest`: `physics/Energy.md` also
says `image: force.png`. Moving `Force` into `mechanics/` keeps
`physics/force.png` and adds `mechanics/force.png` with the same pointer, and
both notes' `image:` values are unchanged, in one accepted commit matching the
full assembly.

Behavior: when another note in the source folder names the same file, a new
file row with the same pointer bytes is placed instead of moving the row.

## Execution complete

Product advice: no backlog change. Story 24 (moves to another notebook reach
Git) keeps its open decision on a picture note moved to another notebook;
`MovedNotePicture` is where story 10 would extend picture carrying.

## Current decisions

- Only notes in the source folder count as other users of the file. Trashed
  notes live under `_trash`, so they do not count. Locally written references
  from other folders are not searched (story exclusion).
- No end-to-end scenario: the frontend is unchanged (it resolves `image:`
  relative to the note's current folder), and pull fidelity follows from the
  accepted tree, as the existing CLI move scenario already shows. The
  controller tests above are the outside-in proof.

## Learnings

- A move into the note's own folder also runs these steps; `pictureToCarry`
  skips it (comparing folder ids, since a lazy proxy may not equal the loaded
  folder), or the name check would find the picture itself
  (`aSamePlaceMoveKeepsThePicture`).
- The `image:` rewrite uses `AuthoredNoteDocument.fromContent` (as the
  wiki-link rewrite on move does), not upload's save preparation, so only
  `image:` changes and no validation newly refuses a move.
- The rule now lives in `MovedNotePicture.placeWithPicture` (look before
  placing, carry after), which both same-notebook `NoteMoveService` steps call.
- The folder query for other notes does not filter trashed notes while the
  root query does; this is harmless, since a note is trashed exactly when its
  folder is, so all notes in one source folder share that state.
- `backend:test:worktree` takes one `--tests` pattern per run.
