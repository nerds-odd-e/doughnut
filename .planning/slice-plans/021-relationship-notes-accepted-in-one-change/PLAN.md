# Adding a relationship on the web places its note through the server in one accepted change

## Source

- Story: [SEED-050#story-2](../../seeds/SEED-050-local-ai-notebook-technical-debt.md#story-2)
- **Identity:** SEED-050#story-2
- Found by the closing review of the local AI notebook effort; re-evaluated
  against `d9abdcc2eb` on 2026-09-27, which also found that web relationship
  and Wikidata-assisted creates bypass the accepted change (owner folded that
  in the same day).
- Direction: [North Star — One accepted-change boundary](../../NORTH-STAR.md#one-accepted-change-boundary);
  one name owner per folder ([docs](../../../docs/notebook-git-attachments.md#one-set-of-names-per-folder)).

## Goal and scope

A notebook owner adding a relationship on the web gets the note in the chosen
folder, also when its name differs only in letter case, and the note plus any
new folder reach accepted Git as one commit.

Included: relationship and Wikidata-assisted web creates go through
`AcceptedWebChangeService.apply`; note creation takes an optional child-folder
name resolved by the server in the same change; the relationship dialog stops
listing and creating folders; one 10 MiB new-payload limit.

Excluded: the lock/race bullet and one "first free name" operation (dropped);
AI "create extracted note" bypass; undo removing a created folder; retiring
the exact-match sibling check (`requireNoConflictingSibling` in
`FolderConstructionService.createFolder` and `NotebookGitProposalFolderPlacement`);
audio and Book size limits; a relationship-specific placement enum on the
server.

## Starting facts (checked 2026-09-27 at `d9abdcc2eb`)

- `services/notebookGit/WebNoteCreationService.createRootNote:40-43` calls
  `NoteConstructionService.createRootNoteWithWikidataService` without `apply`
  when a Wikidata id is given or the content is not an ordinary `Note`
  (`NoteConceptType.isOrdinary`, its only caller). Locked in by
  `NotebookGitNoteCreationControllerTest.relationshipNoteKeepsExistingWebCreationAndAcceptedHead`
  and `NotebookRootNoteCreationWithWikidataTests.wikidataAssistedRootCreateDoesNotAdvanceAcceptedHead`;
  `NotebookGitProjectionDriftControllerTest` uses a web relationship create to
  produce drift (`:31`, `:83`).
- The bypass was left out of plan 062's scope (`f8d97a3333`; relationship
  notes and external-data-assisted creation excluded), not for a technical
  reason. The dialog (`AddRelationshipFinalize.vue` → note store
  `createRootNoteAtNotebook`) posts to `NotebookController:123-137`; creation
  makes no other notes or relation rows; accepted Git already holds
  relationship notes from local publish (`cli_notebook_clone.feature:60`).
- Two tests create drift through a web relationship create:
  `NotebookGitProjectionDriftControllerTest` (`:31`, `:83-86`) and
  `NotebookGitProposalFolderPublicationSafetyControllerTest.refusesAComposedProposalWhenLiveProjectionHasDrifted`
  (`:140-149`).
- The Wikidata path only fetches one description string
  (`WikidataIdWithApi.fetchWikidataDescription`) that
  `NoteConstructionService:113-126` prepends to the body; the controller
  method is itself `@Transactional(SERIALIZABLE)` (`NotebookController:124`).
- `NoteCreationDTO` has `folderId` and `content` only.
- `FolderConstructionService.findOrCreateFolder:109-114` (private, used by
  trash) looks only at folders (`folderHolding`), so it would create a folder
  beside a same-named file; it stays as it is. The name owner's
  `FolderSiblingNameValidation.entryHolding` answers for folders, notes and
  files, and `mergeTargetOrRefuse` (`:143-155`) already reuses a folder or
  refuses another holder.
- `FolderCreationRequest.name` carries `@NotBlankDisplayName`,
  `@Size(max=512)` and `@Pattern(DisplayNamePathSeparators.REGEXP)`; note
  titles may contain `|`, so a folder named after a source note needs the
  same validation.
- After creating a folder the dialog refreshes the sidebar
  (`relationshipFolderResolve.ts:42`); `createRootNoteAtNotebook` refreshes it
  only with `skipNavigation` (`noteStore.ts:103-107`).
- `frontend/src/utils/relationshipFolderResolve.ts` lists the folder, matches
  `f.name === childName` case-sensitively, and otherwise calls `createFolder`
  as its own accepted change; placements `relations_subfolder` ("relations")
  and `named_after_source_note` (source title, or " " when blank) use it.
  `AddRelationship.spec.ts:33-40` mocks the listing and `createFolder`.
- 10 MiB appears as `PictureFile.LIMIT_BYTES` (`services/notebookAttachment/PictureFile.java:16`)
  and in `NotebookGitAttachmentSizeAdmission.java:29`; `ValidateMultipartFile:22`
  is the audio default (overridden to 20 MiB), not the same concept.

## Outside-in proof

| Seed example | Slice |
| --- | --- |
| 4. root relationship note → accepted head contains it; publish not refused as drift | 1 |
| (Wikidata-assisted create reaches accepted Git — same bypass) | 2 |
| 1. existing `Relations` reused ignoring case, one commit | 3 |
| 2. no `relations` folder → folder and note in one commit | 3 |
| 3. a file named `relations` → refused naming `Europe/relations`, nothing changes | 4 |
| dialog sends the child-folder name, makes no listing/folder calls, a new folder appears in the sidebar | 5 |
| 10 MiB limit defined once, refusals unchanged | 6 |

## Slices

### 1. Relationship notes created on the web are accepted

Type: Behavior
Status: done
Accepted proof: `relationshipNoteIsAcceptedInOneCommit` failed first (head did
not advance), then passed with parent = old head, message "Add note: Relates",
`Relates.md` holding the relationship; drift tests now seed an unsynchronized
`makeMe` row (7 + 4 + 5 tests green).
Proof: flip `relationshipNoteKeepsExistingWebCreationAndAcceptedHead` to assert
the accepted head advanced with the note's file (fails first, then passes);
re-seed the drift in `NotebookGitProjectionDriftControllerTest` and
`NotebookGitProposalFolderPublicationSafetyControllerTest` with a row made
directly through `makeMe`, and keep their cases green —
`CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests 'com.odde.donut.controllers.NotebookGitNoteCreationControllerTest' --tests 'com.odde.donut.controllers.NotebookGitProjectionDriftControllerTest' --tests 'com.odde.donut.controllers.NotebookGitProposalFolderPublicationSafetyControllerTest'`.

Behavior: a Git-bound notebook → create a `type: Relationship` note on the web
→ one accepted commit "Add note: <title>" holds it.

Change: only a Wikidata id bypasses `apply`; delete `NoteConceptType.isOrdinary`
(its only caller).

### 2. Wikidata-assisted notes created on the web are accepted

Type: Behavior
Status: done
Accepted proof: `wikidataAssistedRootCreateIsAcceptedInOneCommit` failed first,
then passed (parent = old head; `Wikidata Root.md` holds the Wikidata-derived
location); 8 + 7 tests green. `createRootNote` takes the pre-fetched
`Optional<String>` description.
Proof: flip `wikidataAssistedRootCreateDoesNotAdvanceAcceptedHead` (fails first,
then passes); `NotebookRootNoteCreationWithWikidataTests` stays green otherwise —
`CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests 'com.odde.donut.controllers.NotebookRootNoteCreationWithWikidataTests' --tests 'com.odde.donut.controllers.NotebookGitNoteCreationControllerTest'`.

Behavior: create a note with a Wikidata id on the web → the accepted head
advances once with the note, including the Wikidata-derived content.

Change: fetch the Wikidata description before `apply` (before the notebook
lock, still inside the request transaction), then create inside it;
`createRootNoteWithWikidataService` merges into `createRootNote`, and the
branch in `WebNoteCreationService` goes. A title clash is now reported after
the lookup.

### 3. Note creation reuses or creates a named child folder in the same change

Type: Behavior
Status: done
Accepted proof: `childFolderNameReusesTheFolderHoldingItIgnoringCaseInOneCommit`
and `childFolderNameCreatesTheFolderWithTheNoteInOneCommit` failed first, then
passed (8/8); client regenerated, `vue-tsc` clean.
Proof: new cases in `NotebookGitNoteCreationFolderControllerTest` for
examples 1-2 (`relations` reuses `Relations`, head advances once; no such
folder → folder and note in one commit) fail first, then pass —
`CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests 'com.odde.donut.controllers.NotebookGitNoteCreationFolderControllerTest'`.
Then regenerate the client: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`.

Behavior: note creation with `folderId` (or root) and `childFolderName` →
the note lands in the folder of that name under it, found ignoring case or
created in the same accepted change.

Change: `NoteCreationDTO.childFolderName` with `FolderCreationRequest.name`'s
validation; note creation resolves it through a find-folder-or-create method
on the name owner beside `mergeTargetOrRefuse`, using `entryHolding` and
`createFolder`.

### 4. A child folder name held by a file is refused

Type: Behavior
Status: done
Accepted proof: `childFolderNameHeldByAFileIsRefusedWithoutAnyChange` failed
first (folder created beside the file), then passed: `RESOURCE_CONFLICT`
naming `Europe/relations`, binding, folders and notes unchanged (9/9).
Proof: a new case in `NotebookGitNoteCreationFolderControllerTest` for
example 3 (`Europe` holds a file `relations` → `RESOURCE_CONFLICT` naming
`Europe/relations`; head and rows unchanged) fails first (a `relations`
folder is created beside the file), then passes.

Behavior: the name is held by a file (or a note's `Title.md`) → refused
through the name owner's `refuseTaken`; nothing changes.

Change: the find-folder-or-create method refuses a non-folder holder.

### 5. The relationship dialog lets the server place its folder

Type: Behavior
Status: done
Accepted proof: `AddRelationship.spec.ts` "placing the relationship note in a
child folder" failed first (listing called), then passed 6/6: body carries
`folderId` and `childFolderName` ("relations" / source title), no listing or
`createFolder` call, sidebar refresh key rises; `vue-tsc` clean;
`add_relationship.feature` 7/7. `relationshipFolderResolve.ts` is gone; the
placement mapping lives in `AddRelationshipFinalize.vue`.
Proof: `AddRelationship.spec.ts` asserts the create request carries the source
folder and `childFolderName: "relations"` (and the source title for
`named_after_source_note`), that no listing or `createFolder` call happens,
and that the sidebar listing refreshes after the create —
`CURSOR_DEV=true nix develop -c pnpm frontend:test tests/wiki-link-or-relationship/AddRelationship.spec.ts`,
plus the frontend typecheck from the `frontend` skill; then
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/relationships/add_relationship.feature`
stays green (E2E boot makes this leaf longer than 5 minutes; stated reason).

Behavior: add a relationship with the default placement → one create request
places the note; a newly created folder appears in the sidebar.

Change: `relationshipFolderResolve.ts` maps a placement to `{ folderId,
childFolderName }` without calls; delete `findOrCreateChildFolder` and the
dead blank-title fallback; refresh the sidebar structure after the create on
both the navigating and non-navigating paths.

### 6. One new-payload size limit

Type: Structure
Status: done
Accepted proof: `NotebookAttachment.NEW_PAYLOAD_LIMIT_BYTES` replaces both
`LIMIT_BYTES`; 25 size tests green with refusal messages unchanged.
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests 'com.odde.donut.controllers.NoteControllerUploadNoteImageTests' --tests 'com.odde.donut.controllers.NotebookGitAttachmentSizeAdmission*'`
stays green with refusal messages unchanged.

Change: one constant on `NotebookAttachment` for the 10 MiB new-payload
limit, used by `PictureFile` and `NotebookGitAttachmentSizeAdmission`.

## Execution complete

Product advice: no backlog change. The correction SEED-050#story-8
(plan 026, not queued) holds the folder-entry rule once on the name owner,
drops the dialog spec's absence checks and brings the folder creation test
under 250 lines. Consider later, as advice only: one composed folder-name
constraint for `FolderCreationRequest`, `FolderRenameRequest` and
`NoteCreationDTO.childFolderName`. For wrap-up: example 4's "next publish is
not refused as drift" is inferred (relationship notes now share the ordinary
path), not observed; `docs/notebook-git-attachments.md` "One set of names per
folder" can gain a line on note creation with a child-folder name.

## Current decisions

- The server stays relationship-agnostic: a generic child-folder name on note
  creation (keeps the direction of removing `RelationshipNotePlacement`).
- Wikidata lookups happen before the notebook lock.
- Audio and Book limits stay separate concepts.

## Learnings

- The name owner cannot create folders (`FolderConstructionService` already
  depends on it), so it answers `folderToEnter` and
  `FolderConstructionService.folderToEnterOrCreate` creates when empty. Slice 4
  adds the refusal inside `folderToEnter`; the trash path's private
  `findOrCreateFolder` keeps `folderHolding`.
- The note store already refreshes the sidebar on both the navigating path
  (`navigateToFocusedNote`) and `skipNavigation`; no store change was needed.
