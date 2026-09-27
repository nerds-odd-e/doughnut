# The name owner holds the folder-entry rule once

## Source

- Story: [SEED-050#story-8](../../seeds/SEED-050-local-ai-notebook-technical-debt.md#story-8)
- **Identity:** SEED-050#story-8
- Correction of [SEED-050#story-2](../../seeds/SEED-050-local-ai-notebook-technical-debt.md#story-2)
  ("adding a relationship on the web places its note through the server in
  one accepted change"); provenance
  `aef1e27b77:.planning/slice-plans/021-relationship-notes-accepted-in-one-change/PLAN.md`,
  claim fe66a7224b, commits f8087d5845 (slice 1), c0a9d8fa0e (2),
  aaedc57987 (3), d2fb2c7934 (4), 06ff81b916 (5), aef1e27b77 (6).

### Current findings (rechecked 2026-09-27 at aef1e27b77)

1. The name owner holds "enter the folder holding this name (ignoring case),
   else refuse a note or file holding it" twice.
   `services/FolderSiblingNameValidation.folderToEnter` (`:162-169`, added in
   slices 3-4) and the merge branch of `mergeTargetOrRefuse` (`:143-155`)
   give the same answer; they differ only in query order and in excluding
   the moved folder's id. The non-merge branch of `mergeTargetOrRefuse` is
   exactly `requireFolderNameFree` (`:128-136`) with the moved folder
   excluded. Callers: `FolderConstructionService.folderToEnterOrCreate`
   (`:113-117`, note creation) and `FolderMoveRelocation` (`:104`, `:141`,
   same- and cross-notebook moves).
2. `frontend/tests/wiki-link-or-relationship/AddRelationship.spec.ts`
   (`:148-152`, `:166-167`) spies on `NotebookFolderController.listNotebookFolderListing`
   and `createFolder` only to assert they are not called — tests of the
   absence of behavior slice 5 removed. The import on `:1` serves only them.
   The sidebar refresh assertion (`:164`, `:168-170`) runs in both cases.
3. `backend/src/test/java/com/odde/donut/controllers/NotebookGitNoteCreationFolderControllerTest.java`
   is 252 lines, over the 250-line file check
   ([refactor checks](../../../.agents/skills/dough-post-change-refactor/references/refactor-checks.md)).
   Seven cases repeat `titleOnly(...)` then `setFolderId(...)`.

## Goal and scope

One folder-entry rule on the name owner, used by note creation and folder
move/merge; the relationship dialog spec asserts only observable behavior;
the note-creation folder test file passes the file-size check. Net fewer
lines.

Preserved: SEED-050#story-2 key examples 1-4 and their tests; merge onto a
same-named folder, merge refused by a note or file holding the name, and the
no-merge clash on folder move; every refusal message and error type
(`FOLDER_NAME_CONFLICT` "A folder with this name already exists here.",
`RESOURCE_CONFLICT` "This name is already used here by <path>").

Excluded: the trash path's private `FolderConstructionService.findOrCreateFolder`
(plan 021 decision: stays on `folderHolding`); one composed folder-name
validation constraint across `FolderCreationRequest`, `FolderRenameRequest`
and `NoteCreationDTO` (advice only).

## Outside-in proof

Both slices are Structure: existing tests stay green unchanged in intent.

| Preserved promise | Guarding tests |
| --- | --- |
| story 2 ex. 1-2: reuse ignoring case / create folder with the note, one commit | `NotebookGitNoteCreationFolderControllerTest` `childFolderNameReuses…`, `childFolderNameCreates…` |
| story 2 ex. 3: file-held name refused naming `Europe/relations`, nothing changes | `NotebookGitNoteCreationFolderControllerTest.childFolderNameHeldByAFileIsRefusedWithoutAnyChange` |
| story 2 ex. 4: relationship note accepted | `NotebookGitNoteCreationControllerTest` |
| merge onto a folder; merge refused by a note; no-merge clash | `NotebookGitWebFolderMoveControllerTest`, `NotebookGitFolderDissolveGuardControllerTest`, `NotebookFolderMoveNameClashControllerTest`, `NotebookFolderCrossNotebookMoveMergeControllerTest`, `NotebookFolderMoveWikiLinkRewriteControllerTest` |
| dialog sends `folderId` + `childFolderName`; sidebar refreshes | `AddRelationship.spec.ts` "placing the relationship note in a child folder" |

## Slices

### 1. One folder-entry rule on the name owner

Type: Structure
Status: planned
Proof: all green, no test intent changed —
`CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests 'com.odde.donut.controllers.NotebookGitNoteCreationFolderControllerTest' --tests 'com.odde.donut.controllers.NotebookGitNoteCreationControllerTest' --tests 'com.odde.donut.controllers.NotebookGitWebFolderMoveControllerTest' --tests 'com.odde.donut.controllers.NotebookGitFolderDissolveGuardControllerTest' --tests 'com.odde.donut.controllers.NotebookFolderMoveNameClashControllerTest' --tests 'com.odde.donut.controllers.NotebookFolderCrossNotebookMoveMergeControllerTest' --tests 'com.odde.donut.controllers.NotebookFolderMoveWikiLinkRewriteControllerTest'`;
`wc -l` on `NotebookGitNoteCreationFolderControllerTest.java` ≤ 250;
`git diff --stat` shows net fewer lines in `FolderSiblingNameValidation.java`.

Change:

- `folderToEnter(notebook, parentOrNull, name, excludedFolderIds)`;
  `folderToEnterOrCreate` passes `Set.of()`.
- `mergeTargetOrRefuse`: with `merge`, return `folderToEnter(…, name,
  Set.of(folder.getId()))`; without, `requireFolderNameFree(…, name,
  Set.of(folder.getId()))` and return empty. Fold `requireHeldByAFolder`
  into its one remaining caller if that is simpler; update the Javadoc.
- `NotebookGitNoteCreationFolderControllerTest`: trim repetition in place
  (for example a local `titleIn(title, folder)` for the repeated
  `titleOnly` + `setFolderId` pair); no new helper file.

### 2. The relationship dialog spec asserts only what the dialog does

Type: Structure
Status: planned
Proof: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/wiki-link-or-relationship/AddRelationship.spec.ts`
green (same case count), then
`CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit` clean.

Change: in "placing the relationship note in a child folder", delete the
`listingSpy`/`createFolderSpy` spies, their `not.toHaveBeenCalled()`
assertions and the `NotebookFolderController` import. Keep both request-body
assertions; assert the sidebar refresh key rises once, in the default-placement
case only.

## Current decisions

- 2026-09-27: `folderToEnter` is the one rule; `mergeTargetOrRefuse` keeps its
  name and callers and only chooses between entering and requiring the name
  free.
- 2026-09-27: `findOrCreateFolder` (trash) stays on `folderHolding`.
