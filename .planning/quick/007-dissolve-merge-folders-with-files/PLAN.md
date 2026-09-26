# Dissolve and merge folders that contain files

Status: **in progress** (Story Branch Mode, branch `story/dissolve-merge-folders-with-files`, claim `fb2b68dc1d`).
Work item: **SEED-035#story-11**.
Source: [refined story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-11)
(owner decisions 2026-09-26).

## Goal and scope

An owner can dissolve or merge a folder that contains files on the web: files
move with the notes. Every web placement shares one set of entry names per
folder, compared without letter case; dissolve and merge check every
destination first and refuse naming the first clash. Folder contents are never
removed by a database cascade.

Excluded (see the story): moves and merges into another notebook (story 10;
their refusal stays), renaming on clash, rewriting file references (the path
wiki-link rewrite on a same-notebook merge move is already delivered),
local-publish acceptance using the shared rule, notebook-level cascades, and
any other health-fix change.

## Architecture

- **One set of names per folder** — see the North Star topic
  [One set of names per folder](../../NORTH-STAR.md#one-set-of-names-per-folder).
  PFE: today's checks are `FolderSiblingNameValidation` (folders only, case
  sensitive), `NoteTitlePlacementRules` (notes only, case insensitive),
  `NotebookGitAcceptedTree.takenPaths` (accepted tree, for picture upload and
  `NotebookRootFreeFilename`), and the database unique keys for note create and
  rename. Evolve `FolderSiblingNameValidation` into the single owner of entry
  names in a folder, reading live `note`, `folder` and `notebook_attachment`
  rows, keeping `NumberedNameSelection` for free names. The other checks and
  `takenPaths` retire as their callers move. The database unique keys stay as
  the last safety net.
- **Error codes:** `FOLDER_NAME_CONFLICT` keeps exactly one meaning — a
  *folder* already holds the name — with its current messages; the frontend
  answers it with the "Merge?" prompt on move and dissolve
  (`folderAdminMutations.ts`) and shows it inline on folder create and rename.
  A name held by a note or a file is `RESOURCE_CONFLICT` with a message naming
  the path (note forms keep their `newTitle` field error), which the frontend
  already shows. It must never be `FOLDER_NAME_CONFLICT`, or the frontend
  would offer to merge into a file. No frontend change.
- **Case-variant folders are the same folder:** when a merge meets `diagrams`
  and `Diagrams`, they merge into the existing destination folder, keeping its
  name; merge lookups use the shared rule instead of the exact-match
  `FolderRepository.findCandidateChildContainers`.
- **Local publish is not a caller:** `NotebookGitProposalFolderPlacement` keeps
  a folders-only check with today's behavior (story exclusion).
- **No cascade on folder contents:** `fk_notebook_attachment_folder`,
  `fk_folder_parent` and `fk_note_folder` lose their `ON DELETE` action.
  Every place that removes a folder first removes or moves its contents in
  code, children first. Files removed in code pass through
  `ProjectionChangeCapture`, so the Git derivation learns of them directly.

## Key examples → proof

| Promise (story key example or decision) | Slice | Proof |
| --- | --- | --- |
| `refs/` with only `paper.pdf` is neither reported nor purged as empty | 1 | `NotebookHealthControllerTest` |
| Trashed folder with `paper.pdf` permanently deleted → gone after pull | 2 | `NotebookGitWebFolderPermanentDeleteControllerTest` |
| Folder `force.png` beside `Force.png` refused naming `physics/Force.png` | 3 | `NotebookFolderCreateControllerTest` |
| Moving a folder onto a name a file uses → `RESOURCE_CONFLICT`, no merge offer | 4 | `NotebookFolderMoveControllerTest` |
| Dissolve with `Energy.md`/`energy.md` refused naming `physics/Energy.md` | 5 | `NotebookGitFolderDissolveGuardControllerTest` |
| Case-variant subfolders merge into the existing one | 5 | `NotebookFolderDissolveControllerTest` |
| Dissolve `old` with `sketch.png` → `physics/sketch.png`, same bytes | 6 | `NotebookGitFolderDissolveControllerTest` |
| Dissolve with merge → `physics/diagrams/{a,b}.png` | 6 | `NotebookGitFolderDissolveControllerTest` |
| Move `archive/diagrams` into `physics/` with merge → `physics/diagrams/c.png` | 6 | `NotebookGitWebFolderMoveControllerTest` |
| `force.png` clash (different or identical bytes) refused, nothing changes | 6 | `NotebookGitFolderDissolveGuardControllerTest` |
| Cross-notebook merge of a folder with a file still refused | 6 | `NotebookGitWebFolderCrossNotebookMoveControllerTest` |
| Note moved beside folder `energy.md` as `Energy` refused | 7 | `NotebookGitWebNoteMoveGuardControllerTest` |
| Note created or renamed `Energy` beside folder `energy.md` refused | 8 | `NotebookNoteCreateControllerTest`, `TextContentControllerUpdateNoteTitleTests` |
| Picture `force.png` beside `Force.png` refused; Book name case variant numbered | 9 | `NoteControllerUploadNoteImageTests`, `NotebookBooksAttachNotebookFileControllerTest` |

## Slices

### 1. Health checks count files as occupying a folder
Type: Behavior
Status: done
Accepted proof: `NotebookHealthControllerTest` (13, incl.
`LintHealth.folderHoldingOnlyAFileIsNotReportedEmpty`,
`FixHealth.fixLeavesAFolderHoldingOnlyAFileAndItsFileInPlace`) and
`com.odde.donut.services.health.*` (35) pass; the single source is
`FolderRepository.findOccupiedFolderIdsByNotebookId`.
Proof: `NotebookHealthControllerTest` — a notebook whose `refs/` holds only
`paper.pdf`: the health report no longer lists `refs/` as empty, and fixing
health leaves the folder and its attachment rows in place. Existing
`EmptyFolderBulkPurgeTest` and health-rule tests stay green.

Behavior: one occupied-folders source (folders holding notes or files)
replaces `NoteRepository.findOccupiedFolderIdsByNotebookId` for its three
callers — `EmptyFolderBulkPurge`, `EmptyFolderHealthRule` and
`ReadmeOnlyFolderHealthRule`. The defensive `detachNotesFromFolder` in the
purge goes, since occupied folders are never purged.

### 2. Folder contents are removed in code, never by the database
Type: Structure
Status: done
Accepted proof: `*FolderPermanentDelete*` (5, incl.
`NotebookGitWebFolderPermanentDeleteControllerTest.permanentDeleteOfATrashedFolderAppendsOneAcceptedChildWithoutTheSubtree`)
and the full backend suite (2611, 0 failures) pass after `V300000348`.
Proof: migration `V300000348` (the next free number; the Book storage
removal took `V300000347`) changes the three foreign keys to plain
restricting keys. `NotebookGitWebFolderPermanentDeleteControllerTest`,
`NotebookFolderPermanentDeleteControllerTest` and
`NotebookGitProposalInitialNotebookStructureControllerTest` stay green; then
the full backend suite, because the committed-fixture cleanup every Git
controller test uses changes. The full suite is this slice's stated sizing
exception.

Internal change: permanent delete of a trashed folder removes the subtree's
attachments before its folders. `testability/CommittedUserCleanup` removes
attachments, then folders children first, before notebooks (the notebook
cascades now meet restricting keys). Local-publish acceptance already removes
files and notes before unrepresented folders, children first; a live note can
never sit in an unrepresented folder, because its own path represents the
folder, so it needs no change and a violation there would be a legitimate loud
failure. Unchanged external behavior; it enables slice 6, where a file the code
forgot to move fails loudly instead of disappearing.

### 3. One set of names per folder for folder create and rename
Type: Behavior
Status: done
Accepted proof: `NotebookFolder*ControllerTest` (83, incl.
`NotebookFolderCreateControllerTest.rejectsAFolderNamedLikeAFileHereIgnoringCase`,
`rejectsAFolderNamedLikeANoteFileHereIgnoringCase`,
`rejectsACaseVariantOfASiblingFolderAsAFolderNameConflict`,
`NotebookFolderRenameControllerTest.rejectsRenamingToAFileNameHereIgnoringCase`),
`*Folder*` (317) and `*NotebookGitProposal*` (148) pass. The owner API is
`FolderSiblingNameValidation.entryHolding` → `TakenEntry(kind, path)` and
`requireFolderNameFree`; the all-entries free-name picker is left to slice 4,
its first user.
Proof: `NotebookFolderCreateControllerTest` — creating folder `force.png` in
`physics/`, which holds `Force.png`, is refused with `RESOURCE_CONFLICT` naming
`physics/Force.png`; a folder `energy.md` beside note `Energy` likewise; a
folder `Physics` beside folder `physics` keeps `FOLDER_NAME_CONFLICT` and its
message. `NotebookFolderRenameControllerTest` — renaming to a file's name is
refused the same way. Existing `rejectsDuplicateSiblingFolderName` and
`rejectsDuplicateSiblingName` stay green.

Behavior: `FolderSiblingNameValidation` evolves into the owner of entry names
in a folder: it answers whether a name is taken across notes (`Title.md`),
folders and files from live rows without case, and picks free names with
`NumberedNameSelection`. Folder create and rename use it. The local-publish
caller keeps a folders-only check.

### 4. Folder move and trash use the same set of names
Type: Behavior
Status: done
Accepted proof: `*Folder*` (319, incl.
`NotebookFolderMoveNameClashControllerTest.refusesMovingOntoAFileNameIgnoringCaseWithoutOfferingMerge`,
`mergesIntoACaseVariantDestinationFolderKeepingItsName` and
`NotebookFolderTrashControllerTest.usesFirstFreeSiblingNameIgnoringCaseWithoutChangingEarlierTrash`),
`*Trash*` (74) and `*NotebookGitProposal*` (148) pass. A within-notebook move
onto a case-variant folder merges into it (top-level lookup only; nested
merge lookups stay exact until slice 5). Cross-notebook moves still use
`mergeTargetOrRejectConflict`.
Proof: `NotebookFolderMoveControllerTest` — moving folder `force.png` into a
folder holding file `Force.png` is refused with `RESOURCE_CONFLICT` (never
`FOLDER_NAME_CONFLICT`) and nothing changes; moving onto a same-named folder
still returns `FOLDER_NAME_CONFLICT` and merges when requested.
`NotebookFolderTrashControllerTest.usesFirstFreeSiblingNameWithExistingCaseRulesWithoutChangingEarlierTrash`
is updated to the case-insensitive rule. Existing move, merge and trash-collision
tests stay green.

Behavior: folder move asks the shared rule; a folder-held name offers merge, any
other taken name refuses. Folder trash takes the first free name from the same
rule.

### 5. Dissolve and merge check every destination first
Type: Behavior
Status: done
Accepted proof: `*FolderDissolve*ControllerTest` (23, incl.
`NotebookGitFolderDissolveGuardControllerTest.dissolveOntoANoteNameTakenIgnoringCaseIsRefusedNamingItAndChangesNothing`,
`mergeMoveOntoANestedNoteNameTakenIgnoringCaseIsRefusedNamingItAndChangesNothing`,
`NotebookFolderDissolveControllerTest.dissolveMergesACaseVariantSubfolderIntoTheExistingOne`),
`*Folder*` (322), `*Dissolve*` (29) and `*Merge*` (18) pass. The check is
`FolderContentsPlacementCheck.requireContentsFit` (subfolders then notes,
depth-first; slice 6 adds the source's files there); dissolve and merge share
`FolderSubtree.moveContentsInto`, where slice 6 adds carrying files.
Proof: `NotebookGitFolderDissolveGuardControllerTest` — `physics/Energy.md`
against `physics/old/energy.md`: dissolving `old` is refused naming
`physics/Energy.md`; folders, notes and the accepted head are unchanged; the
same through a merge move. `NotebookFolderDissolveControllerTest` — subfolder
`Diagrams` of `old` merges into `physics/diagrams` when merge is requested.
Existing dissolve and merge tests stay green.

Behavior: dissolve and merge first list every destination entry they would
create and check each with the shared rule; same-named folders (ignoring case)
merge, or without merge keep today's `FOLDER_NAME_CONFLICT` prompt; any other
taken name refuses with `RESOURCE_CONFLICT` naming the first one, before any
row changes. Merge lookups use the shared rule. Folders with files are still
refused here (the guard stays until slice 6).

### 6. Dissolve and merge carry files
Type: Behavior
Status: done
Accepted proof: `*NotebookGit*Folder*ControllerTest` (114, incl.
`NotebookGitFolderDissolveControllerTest.dissolveCarriesAFileIntoTheParentWithTheSameBytes`,
`dissolveWithMergeCarriesAFileIntoTheSameNamedFolder`,
`NotebookGitWebFolderMoveControllerTest.mergeMoveCarriesAFileBesideTheDestinationFolderContent`,
`NotebookGitFolderDissolveGuardControllerTest.dissolveOntoATakenFileNameIsRefusedNamingItAndChangesNothing`,
`NotebookGitWebFolderCrossNotebookMoveControllerTest.folderContainingAFileCannotMergeIntoAnotherNotebook`),
`*Folder*` (326), `*Dissolve*` (31), `*Merge*` (19), `*Attachment*` (63)
pass. The cross-notebook refusal now reads "Folders containing files cannot
be moved to another notebook yet." (the seed still quotes the old temporary
wording — for story wrap-up).
Proof: `NotebookGitFolderDissolveControllerTest` — dissolving `old` holding
`sketch.png` leaves a pulled tree with `physics/sketch.png`, same bytes, and no
`old/`; dissolve with merge brings `physics/old/diagrams/b.png` beside
`physics/diagrams/a.png`. `NotebookGitWebFolderMoveControllerTest` — moving
`archive/diagrams` into `physics/` with merge yields `physics/diagrams/c.png`.
`NotebookGitFolderDissolveGuardControllerTest` — different or identical
`force.png` at `physics/` and `physics/old/`: dissolving `old` is refused naming
`physics/force.png` and nothing changes (replacing its two file-refusal cases).
`NotebookGitWebFolderCrossNotebookMoveControllerTest` — a cross-notebook merge
of a folder with a file is still refused.

Behavior: the destination check of slice 5 includes files, and
`FolderSubtree.dissolveInto` / `mergeInto` rehome files exactly as notes (row
and bytes kept; only the folder changes). `requireSubtreeHasNoAttachments`
leaves these operations; the cross-notebook refusal moves into
`FolderMoveRelocation.moveFolderToAnotherNotebook` once, before its
merge-or-reassign branch, covering both.

### 7. Note move, undo and trash use the same set of names
Type: Behavior
Status: planned
Proof: `NotebookGitWebNoteMoveGuardControllerTest` — moving note `Energy` into
`physics/`, which holds folder `energy.md`, is refused naming
`physics/energy.md`, and nothing changes. Existing move-collision, undo
(`NotebookGitWebTrashGuardControllerTest`) and trash free-title tests stay
green.

Behavior: `NoteMotionService` placement asks the shared rule for `Title.md`
(refuse) and for trash (first free title). `NoteTitlePlacementRules` retires
into it, keeping the `newTitle` field error.

### 8. Note create and rename use the same set of names
Type: Behavior
Status: planned
Proof: `NotebookNoteCreateControllerTest` — creating note `Energy` in `physics/`,
which holds folder `energy.md`, is refused with the `newTitle` field error
naming `physics/energy.md`; `TextContentControllerUpdateNoteTitleTests` —
renaming a note to that title likewise. Existing duplicate-title tests stay
green.

Behavior: note create and rename gain the shared check they lacked; the
database unique key stays as the last safety net.

### 9. Picture upload and Book files use the same set of names
Type: Behavior
Status: planned
Proof: `NoteControllerUploadNoteImageTests` — its four refusal cases stay green,
plus `force.png` beside `Force.png` is refused.
`NotebookBooksAttachNotebookFileControllerTest.aTakenNameIsNumberedAndTheExistingFileIsUntouched`
stays green, plus a case-variant name is also numbered.

Behavior: picture upload (`WebNoteImageUploadService`) refuses, and
`BookSourceFilePlacement` picks a free root filename, through the shared rule on
live rows instead of the accepted tree. `NotebookRootFreeFilename` (its only
caller is `BookSourceFilePlacement`), `NotebookGitAcceptedTree.takenPaths` and
its store method go. Update
`docs/notebook-git-attachments.md` for the folder operations and the one name
rule.

## Current decisions

- First clashing path only in the refusal message (owner, 2026-09-26).
- Existing content is never judged again; the rule governs new placements.
- The database unique keys stay unchanged as a last safety net; the
  case-sensitive folder and file keys are not widened.
- Case-variant folders merge into the existing one (planner decision under the
  owner's case-insensitive rule).
- Slices 5 before 6: the file guard stays until dissolve and merge check every
  destination, so a case-variant file clash can never slip past the
  case-sensitive database key.

## Learnings

- Nested merge lookups are shared with cross-notebook merge moves, so those now
  merge case-variant nested folders too (coordinator decision, consistent with
  "case-variant folders are the same folder"). Cross-notebook merges still run
  no destination pre-check: a nested note clash fails loudly on
  `uk_note_notebook_folder_title`, as an exact clash already did — story 10
  territory.
- Git controller tests are non-transactional: use repository `findById`, not
  `makeMe.refresh`.
- Every web placement asking the shared rule now reads attachment filenames;
  `NotebookGitDerivedFolderTreeOracleControllerTest` assertions allow only
  `SELECT a.filename FROM NotebookAttachment a…` queries there.
- `FolderConstructionService.createFolder(notebook, request)` is shared by
  web and local-publish folder creation, and `createFolder(notebook, parent,
  name)` by trash; web-only naming rules go in `WebFolderCreationService`
  (via `parentFolderFor`), not in `FolderConstructionService`.
- `fk_note_folder` was already `ON DELETE RESTRICT` (since `V300000329`);
  `V300000348` restricts only the attachment and parent-folder keys. The
  unique key `uk_folder_notebook_parent_name` rules out "unlink parents, then
  bulk delete" — folder removal must go children first.
- `pnpm backend:test:worktree` takes a single `--tests` pattern; use a
  package wildcard (e.g. `'com.odde.donut.services.health.*'`) for several classes.
- Rechecked on main `7f1bc6d440` (2026-09-26): the only product-code change
  since the previous recheck is `63fdb9134e` (path wiki links survive a
  same-notebook merge move). It turns both `FolderMoveRelocation` moves into one
  merge-or-place branch followed by the wiki-link rewrite; slices 4–6 still fit
  it, and slice 6's cross-notebook refusal now sits before that single branch.
  Every other named class, test and migration number is unchanged.
- Rechecked on main `b36d89a999` (2026-09-26), after the Book storage removal:
  every class and test the slices name still exists, and the folder, naming,
  health, move and dissolve code is unchanged since planning. Only the
  migration number (slice 2) and the Book placement caller (slice 9) moved.
- Parallel with SEED-035#story-2 (web file delete): that plan adds a delete
  endpoint, the Book-source refusal and the file page action, with no
  migration and no folder or naming code. Its only file in common with this
  plan is `folderAdminMutations.ts`, which this plan relies on unchanged.
  Removing one attachment row is unaffected by slice 2's restricting keys.
- Independent plan review (2026-09-26) confirmed the Git derivation already
  handles attachments whose folder changes and attachments removed explicitly
  inside a deleted folder; no derivation change is planned.
- The earlier 14-slice draft and the resplit mapping remain recoverable from
  commit `653f5ce5a7365a4296c3206dcdf554bdccd28fb0`; none of it is carried.
