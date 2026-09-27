# The notebook tree has one model in code

## Source

- Story: [SEED-050#story-4](../../seeds/SEED-050-local-ai-notebook-technical-debt.md#story-4)
- **Identity:** SEED-050#story-4
- Found by the closing review of the local AI notebook effort; re-evaluated
  against `d9abdcc2eb` on 2026-09-27 (Book source bullet dropped).
- Direction: [North Star — One notebook tree](../../NORTH-STAR.md#one-notebook-tree)
  ("the notebook root can contain the same content").

## Goal and scope

Developers read a folder's ancestry from one method on `Folder` and what a
container (root or folder) holds through one query per kind, so root and
folder paths stop diverging, with no performance loss.

Included: `Folder.trailFromRoot()`; the descendant check and the health rule's
depth derive from it; services and DTOs read the trail from the domain and
`FolderTrail` goes; one container query each for folders, files and notes,
taking the notebook id and the folder id or null; the paired queries, the
listing's branch and `MovedNotePicture`'s root/folder branch go.

Excluded: `Folder.isTrashed` and `NotebookGitPortablePath.folderPath` (short
recursions on hot paths); the Git projection's prefix walk and downward child
maps (not ancestry); the Book source path (dropped); API or DTO changes.

Constraints: same number of queries per request; ancestry compared by id
(`EntityIdentifiedByIdOnly.equals` compares classes, so a Hibernate proxy
never equals the entity); listing responses and move messages unchanged.

## Starting facts (checked 2026-09-27 at `d9abdcc2eb`)

Paths under `backend/src/main/java/com/odde/donut/`.

- `controllers/dto/FolderTrail.fromRootToFolder` (`:31-38`) is the trail
  (outermost first, inclusive, empty for the root); `FolderTrail` has five
  helpers, called at 15 sites in 11 services (`NoteRealmService:52,61,80,105`,
  `NoteTrashService:60`, `FolderRelocationService:111`,
  `NotebookCatalogService:74`, `PortablePathAuthoring:52`,
  `WikiLinkRelocationRewrite:72`, `WikiLinkRewriteSupport:245`,
  `WikiLinkNoteCandidates:65`, `FocusContextRetrievalService:114`,
  `FocusContextRelatedNoteMaterializer:29`), by DTOs `RecalledNote:30` and
  `NotebookAttachmentRealm:25`, and by `FolderConstructionServiceTest:32,35`.
- Hand-written repeats: `services/FolderMoveDestinationRules.folderIsStrictDescendantOf`
  (`:26-35`) and `services/health/FolderSubtreeOccupancy.folderDepth`
  (`:124-132`, used for deepest-first sorting at `:89`).
- Paired queries: `FolderRepository.findRootFoldersByNotebookIdOrderByIdAsc` /
  `findChildFoldersByParentFolderIdOrderByIdAsc`;
  `NotebookAttachmentRepository.findRootListItemsByNotebookId` /
  `findListItemsByFolderId`; `NoteRepository.findNotesInNotebookRootFolderScopeByNotebookId`
  (adds a not-trashed filter that is a no-op at the root) /
  `findNotesInFolderOrderByIdAsc`, wrapped by `NoteService:56-62`.
- Callers: `controllers/NotebookFolderQuerySupport:59-88` (branch),
  `services/FolderConstructionService:79`, `services/MovedNotePicture:74-77`
  (ternary), `services/FolderSubtree:49,79,92,141,156`,
  `services/FolderContentsPlacementCheck:71,83`, and `NoteService`'s callers
  (including tests `CircleControllerTest`, `NotebookCrudControllerTest`,
  `NotebookNoteCreateControllerTest`, `NotebookFolderPermanentDeleteControllerTest`,
  `RelationControllerMoveNoteToFolderTests`).
- The single `(:parentFolderId IS NULL AND … IS NULL) OR …` form already
  exists in `FolderRepository.findCandidateChildContainers` and
  `NotebookAttachmentRepository.findFilenamesNamedIgnoringCase`.
- Hibernate 7's MySQL dialect renders `is not distinct from` as MySQL's
  null-safe `<=>`, which MySQL can use for an index lookup whether the value
  is NULL or a number; no JDBC URL sets `useServerPrepStmts`.
- Indexes: `folder` has single-column `idx_folder_notebook_id` and
  `idx_folder_parent_folder_id` (no combined one); `notebook_attachment` has
  `uk_notebook_attachment_notebook_folder_filename` and
  `fk_notebook_attachment_folder`; `note` has
  `idx_note_structural_peer (notebook_id, folder_id, id)`. Assumption to
  confirm with `EXPLAIN` in slices 3 and 4: the combined forms keep using
  these (root folders by notebook id, child folders by parent, notes by the
  structural-peer index).

## Outside-in proof

All Structure: existing behavior stays green.

| Seed example | Slice | Existing proof |
| --- | --- | --- |
| 1. move `A` into its descendant `C` refused | 1, 2 | `NotebookFolderMoveControllerTest`, `NotebookGitProposalFolderRelocationPlacementControllerTest` |
| 2. empty `X/Y/Z` purged deepest first | 1 | `EmptyFolderBulkPurgeTest` |
| 3. root and folder listings show only their own entries | 3, 4 | `NotebookFolderListingControllerTest` |
| 4. a shared root picture is copied, not moved | 4 | `NotebookGitWebNoteMovePictureControllerTest` |

## Slices

### 1. A folder answers its own trail from the root

Type: Structure
Status: done — proof passed 26/26 (`rejectsMoveIntoDescendant`,
`rejectsAnExactFolderRelocationIntoADescendantFolder`,
`nestedFullyEmptyTreePurgedDeepestFirst`; the purge order is observed only
indirectly, over two levels, as before)
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests 'com.odde.donut.controllers.NotebookFolderMoveControllerTest' --tests 'com.odde.donut.controllers.NotebookGitProposalFolderRelocationPlacementControllerTest' --tests 'com.odde.donut.controllers.NotebookFolderCrossNotebookMoveControllerTest' --tests 'com.odde.donut.services.health.EmptyFolderBulkPurgeTest' --tests 'com.odde.donut.controllers.NotebookGitWebTrashControllerTest'`
stays green.

Change: add `Folder.trailFromRoot()` with the loop from
`FolderTrail.fromRootToFolder`; `FolderTrail` delegates for now (null still
means the root). The descendant check becomes "the new parent's trail contains the
moved folder's id"; `folderIsStrictDescendantOf` goes. The purge sorts by
trail length; `folderDepth` goes.

### 2. Services read a folder's trail from the domain

Type: Structure
Status: done — full backend suite 2708 tests green; after refactor, focused
rerun of focus context, attachment, folder, wiki-link and portable-path tests
(449) green. Trail lives in `Folder.trailFromRootTo`/`trailFromRoot`/
`ancestorsFromRoot` and `Note.folderTrailFromRoot`/`folderNamesFromRoot`
Proof: the backend compiles and the full backend suite stays green —
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` (the change reaches 11
services across notes, wiki links, recall and focus context, so a focused
selection would miss callers; this is the stated reason for a leaf longer
than 5 minutes).

Change: the 15 service call sites, the two DTOs and the test read the trail
from `Folder` (and a `Note` helper for its containing folder's trail or names
where several callers need it); `FolderTrail`'s helpers move to their owners
and the class goes.

### 3. Folder and file listings read one container query each

Type: Structure
Status: done — `FolderRepository.findFoldersInContainer` and
`NotebookAttachmentRepository.findListItemsInContainer` (`<=>` in SQL). EXPLAIN
on a scratch copy seeded with 6,000 folders / 12,300 files: root folders
index-merge notebook+parent (as before), child folders index-merge
parent+notebook (1 row), root and folder files ref `fk_notebook_attachment_folder`
(root plan identical to the old query). Planned tests plus the other
`FolderSubtree`/`FolderContentsPlacementCheck` callers: 110 green.
Proof: one `EXPLAIN` of the root and folder forms of each query on the test
database, recorded here; then
`CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests 'com.odde.donut.controllers.NotebookFolderListingControllerTest' --tests 'com.odde.donut.controllers.NotebookFolderMoveControllerTest' --tests 'com.odde.donut.controllers.NotebookFolderMoveNameClashControllerTest' --tests 'com.odde.donut.controllers.NotebookGitWebFolderTrashGuardControllerTest' --tests 'com.odde.donut.services.FolderConstructionServiceTest'`
stays green.

Change: one folders-in-container and one file-list-items-in-container query
(`notebook id = :notebookId and parent is not distinct from :folderId`,
ordered by id) replace the two pairs; update
the listing, `FolderConstructionService`, `FolderSubtree` and
`FolderContentsPlacementCheck` callers. If `EXPLAIN` shows a lost index, stop
and record it as a learning.

### 4. Note listings read one container query and the listing stops branching

Type: Structure
Status: done — `NoteRepository.findNotesInContainer`; the root query's
not-trashed filter was a no-op (null `folder_id` never matches a trashed
folder). EXPLAIN on 9,000 seeded notes: root `idx_note_structural_peer`,
folder `idx_note_folder_id`, same as before. Planned proof 82 green plus the
other callers (123) green; new
`aRootPictureAnotherRootNoteUsesIsCopiedNotMoved` observes example 4 at the
root (fails when the root answer is removed). Refactor split
`RelationControllerMoveNoteTrashTests` out of
`RelationControllerMoveNoteToFolderTests`.
Proof: one `EXPLAIN` of the note query's root and folder forms, recorded
here; then `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests 'com.odde.donut.controllers.NotebookFolderListingControllerTest' --tests 'com.odde.donut.controllers.NotebookGitWebNoteMovePictureControllerTest' --tests 'com.odde.donut.controllers.NotebookFolderCrossNotebookMoveControllerTest' --tests 'com.odde.donut.controllers.NotebookFolderPermanentDeleteControllerTest' --tests 'com.odde.donut.controllers.RelationControllerMoveNoteToFolderTests' --tests 'com.odde.donut.controllers.NotebookNoteCreateControllerTest' --tests 'com.odde.donut.controllers.CircleControllerTest' --tests 'com.odde.donut.controllers.NotebookCrudControllerTest'`
stays green.

Change: one notes-in-container query replaces the pair (dropping the no-op
trashed filter) and the two `NoteService` pass-throughs, including the
callers in `FolderSubtree:49,92,156` and `FolderContentsPlacementCheck:83`; `MovedNotePicture`
loses its ternary; the listing resolves the folder or root once and makes
three calls without a branch; update the test callers. If it overruns 10
minutes, split the listing's branch removal into its own slice.

## Current decisions

- `isTrashed` and `folderPath` stay recursive.
- Ancestry is compared by id.
- Slices 1-2 are independent of 3-4; slices 3 and 4 may run in either order.
- Container queries use `is not distinct from`, not an `IS NULL … OR` pair.

## Learnings

- On empty tables MySQL picks the notebook-id prefix for `<=>` forms; with
  realistic data it uses the parent/folder indexes, so verify container query
  plans on seeded data, not the empty test database.
- The name-filtered sibling queries (`findCandidateChildContainers`,
  `findChildFoldersNamedIgnoringCase`, `findFilenamesNamedIgnoringCase`) keep
  the `IS NULL … OR` form; out of this story's scope.
- A folder's notes are now found by (notebook, folder), so a cross-notebook
  move reads them before changing the folder's notebook.
- Still branching outside this story: `NoteService.findStructuralPeerNotesSample`
  (root vs folder structural-peer queries), and the "folder in this notebook
  or 404" lookup written in the listing, `FolderRelocationService` and
  `NoteConstructionService`.
