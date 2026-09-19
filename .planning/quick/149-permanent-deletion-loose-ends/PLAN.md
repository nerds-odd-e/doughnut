# Close the loose ends left by permanent deletion of trashed content

Status: planned
Source: [SEED-036 story 1](../../seeds/SEED-036-permanent-deletion-loose-ends.md#story-1).
Provenance: follow-ups grouped by the owner during the execution retrospective
of SEED-034 story 1 (plan 146, recoverable at commit `8b8b83e962`).
Authority: 2026-09-19 owner instruction to evaluate each item, choose an
approach, and write a slice plan for the items worth doing. Later the same day
the owner accepted the recommendations and asked for a refinement that keeps
the scope narrow and the design simple. Planning only. This plan does not
authorize execution by itself.

Baseline: `c70ad55a6a` on `main`.

## Goal and scope

A notebook owner no longer meets an Undo that fails after permanently deleting
a note. Donut's maintainers get one wording for the folder Trash offer, one
home and one status for the "Folder not in notebook." refusal, and the three
oversized files made smaller by removing real duplication and by giving each
mixed responsibility its own home.

The owner's direction for this work: simple, highly cohesive, directly mapped
to the domain model, with as little duplication as possible. Every slice below
must leave the code smaller or more cohesive. No slice cuts a file only to
reach a line count.

## Evaluation of the four items

| Item | Value | Cost | Verdict |
| --- | --- | --- | --- |
| (a) Undo after permanent deletion | High. User-visible. The failed "trash note" entry is never popped, so Undo stays broken for every older entry until the page reloads. | Very small | Do first (slice 1) |
| (b) One wording for the folder Trash offer | Low to medium. Two texts say the same thing in different words on one screen. | Very small | Do (slice 2) |
| (c) One home and one status for "Folder not in notebook." | Medium. Seven copies, one of them answers 400 where six answer 404. Some checks run twice in one call. | Small | Do (slices 3 and 4) |
| (d) `StoredApiCollection.ts` (613 lines) | Medium to high. About 140 lines are true duplication: the same move request is written five times, one branch can never run, and every public method is declared twice. After that, most of each method is request and error handling that needs no state. | Small | Do (slices 5, 6, 8, 9) |
| (d) `NoteMoreOptionsActions.vue` (272 lines) | Medium-low. The note actions are declared twice, once per layout. Four of the six are plain buttons that differ only in data. Every new action pays twice; the permanent-deletion story already did. | Small | Do (slice 7) |
| (d) `NotebookController.java` (562 lines) | Medium. Ten folder endpoints live in the notebook controller. The tests already treat them as their own concept (`NotebookFolder*ControllerTest`), and sibling controllers under `/api/notebooks` are an existing pattern (`NotebookBooksController`, `NotebookHealthController`, `NotebookGroupController`). | Medium. About 50 client call sites and 125 backend test call sites are renamed mechanically. No duplication is removed. URLs do not change. | Do last (slices 10 and 11), first to drop |

Considered and declined, with reasons:

- **Moving the three notebook Git endpoints out of `NotebookController`.** It
  would remove about 65 lines but touch 89 backend test files. The cost is far
  above the value. After slices 10 and 11 the controller is about 350 lines and
  holds one concept, the notebook itself (catalog, settings, sharing, readme,
  indexing, export, Git). Cutting further would be arbitrary. The owner accepted
  this decline on 2026-09-19.
- **A new layout-aware button component for the note actions, and a new
  cache-aware request layer under the stored API.** Both were in the first
  version of this plan. The refinement replaced them with a plain list (slice 7)
  and stateless request functions (slices 8 and 9), which need no new concept.
- **Forgetting undo entries of notes removed by permanent deletion of a
  folder.** The client does not learn which notes the server removed. The seed
  limits item (a) to the note case. Left out.
- **Unifying "Parent folder not in notebook." and "Note not in notebook."**
  They are different refusals with their own pinned test. Left out.
- **Removing the backend endpoint `moveNoteToNotebookRoot` without a notebook**
  once slice 5 removes its only client caller. It is API surface, outside this
  story. Reported as a follow-up candidate only.
- **Merging the note and folder permanent-delete warning texts.** Not one of
  the four items.

## Owner decisions

Confirmed by the owner on 2026-09-19.

1. **Surviving Trash wording (slice 2).** The description's impersonal wording.
   - Subject: `Trash folder "<name>" with its complete subtree`
   - Consequence: `Its contents leave active use. References remain authored, but may no longer resolve until the folder is recovered with Move.`
   - Description is `<subject>. <consequence>`; confirmation is `<subject>? <consequence>`.
2. **Status for "Folder not in notebook." (slice 3).** 404 everywhere.
3. **`NotebookController` beyond its folder endpoints** is not cut further.

## Architectural notes

Existing solutions were searched before choosing each approach:

- (a) `permanentlyDeleteNote` and `reduceRelationNoteToSourceProperty` both end
  by dropping the note from the client cache. That shared step is where "this
  note no longer exists" is known, so the undo entries are forgotten there.
- (b) The permanent-delete offer in the same file already composes both texts
  from one subject and one consequence. The Trash offer reuses that shape.
- (c) `FolderRelocationService.requireFolderInNotebook` is the model. The rule
  "a folder belongs to one notebook" is a fact about `Folder`, so the home is
  `Folder.requireInNotebook(Notebook)`, beside `Folder.isTrashed()`. It throws
  `ResponseStatusException` like the copies it replaces. `Folder` already
  carries web annotations (Jackson, OpenAPI), so this adds no new kind of
  dependency.
- (d) Sibling controllers under `/api/notebooks` are the established pattern.

No Accepted ADR is affected. ADR 0001 keeps Trash and permanent deletion
distinct; slices 1 and 2 respect that. The North Star topics concern Git
consistency owners and are not touched: slices 10 and 11 move endpoints without
changing which service owns a change. No new North Star topic is needed.

## Outside-in proof

| Promise from the seed | Owning slice | Observable proof |
| --- | --- | --- |
| Trash a note, permanently delete it, press Undo: nothing fails and nothing claims to restore it | 1 | Mounted `NoteUndoButton`: after trash then permanent deletion through `storedApi()`, the undo button for that note is gone and an older entry of another note is offered instead |
| Folder Settings states the Trash consequences once, in one wording | 2 | `FolderPage.trash.spec.ts`: description and confirmation contain the identical consequence sentence |
| One status for "Folder not in notebook." | 3 | `NotebookNoteCreateControllerTest`: creating a note in another notebook's folder answers 404 with that reason |
| One home for "Folder not in notebook." | 4 | The text exists once in `backend/src/main`; the four tests that pin it stay green |
| The three oversized files are resolved | 5 to 11 | Existing specs stay green; each slice reports the resulting line count; the accepted decline above |

Focused commands:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/path/to/File.spec.ts
CURSOR_DEV=true nix develop -c pnpm backend:test_only
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/path/to.feature
```

## Ordered slices

Order: the user-visible item first, then cheapest maintenance value first. The
story may stop safely after any slice. First to drop: 10 and 11, then 8 and 9.
Slices 1 to 9 are each one small change with one proof loop.

### 1. Undo offers nothing for a note that no longer exists
Type: Behavior
Status: done
Proof: new cases in `frontend/tests/toolbars/NoteUndoButton.visibility.spec.ts`
(`pnpm frontend:test tests/toolbars/NoteUndoButton.visibility.spec.ts`, 6
tests pass); `frontend/tests/store/storedApi.reduceRelationNoteToSourceProperty.spec.ts`
stays green unchanged, as does the rest of `frontend/tests/store`.

Behavior: a note has an "edit content" entry, another note has an older entry,
and the first note is then trashed through `storedApi().trashNote` → the owner
permanently deletes it through `storedApi().permanentlyDeleteNote` → the undo
button no longer names the deleted note and offers the other note's entry. With
no other entry, the button is absent.

Approach: `NoteEditingHistory` gains one operation that drops every entry of a
given note. `StoredApiCollection` gets one private step, "this note no longer
exists", that removes the cached realm and forgets its entries.
`permanentlyDeleteNote` and `reduceRelationNoteToSourceProperty` both use it,
because both permanently remove a note. Relationship reduction is covered by the
same rule and needs no extra test. `undoCreateNote` keeps its plain cache
removal: that note is only trashed.
Complexity: about +10 lines, no new concept on the screen.

### 2. The folder Trash offer states its consequences in one wording
Type: Behavior
Status: done
Proof: `frontend/tests/pages/FolderPage.trash.spec.ts` pins the identical
consequence sentence in both description and confirmation
(`pnpm frontend:test tests/pages/FolderPage.trash.spec.ts`, 4 tests pass).
The refactor pass introduced `removalMessages(subject, consequence)` in
`folderRemovalOffer.ts` so the permanent-delete and trash pairs share one
composition instead of repeating it; `FolderPage.permanentlyDelete.spec.ts`
stays green (4 tests) proving the permanent-delete strings are unchanged.
Net line count: `folderRemovalOffer.ts` 55 → 61 lines — one wording became
one, but the file gained the shared `removalMessages` helper it lacked
before, which the plan's "-2 lines" estimate did not anticipate.

Behavior: an active folder's Settings tab is open → the owner reads the
description and then presses Trash → the confirmation shows the same subject and
the same consequence sentence as the description, differing only in "." and "?".

Approach: in `folderRemovalOffer.ts`, replace `trashDescription` and
`trashConfirmation` with one subject and one consequence, composed exactly like
the permanent-delete pair. Update the pinned phrase in the spec.
Complexity: about −2 lines, two wordings become one.

### 3. Creating a note in another notebook's folder answers 404
Type: Behavior
Status: done
Proof: `NotebookNoteCreateControllerTest.rejectsFolderIdFromAnotherNotebook`
updated in place (it already covered this exact case, previously pinning
400) to assert 404, the exact reason text, and that no note is created.
`./backend/gradlew -p backend test -Dspring.profiles.active=test --tests
"com.odde.donut.controllers.NotebookNoteCreateControllerTest"`: BUILD
SUCCESSFUL. `Folder.requireInNotebook(Notebook)` is the new home, used only
by `NoteConstructionService.buildNote` so far; slice 4 replaces the
remaining five copies with it.

Behavior: folder F belongs to notebook B → the owner creates a note in notebook
A with `folderId` = F → 404 "Folder not in notebook.", and no note is created.

Approach: add `Folder.requireInNotebook(Notebook)` as the home of the refusal
and use it in `NoteConstructionService.buildNote`.

### 4. Every "Folder not in notebook." refusal comes from the one home
Type: Structure (owns retrospective item c directly)
Status: done
Proof accepted: full backend suite (`pnpm backend:test_only`) BUILD
SUCCESSFUL, including the four named controller tests; `grep -rn "Folder not
in notebook" backend/src/main` returns exactly one line, inside
`Folder.requireInNotebook`. `NotebookController.java` 556→548 lines,
`FolderRelocationService.java` 236→229, `FolderMoveRelocation.java`
211→204, `FolderConstructionService.java` 118→111.
Weakness removed: the same rule and message are written in
`NotebookController`, `FolderRelocationService`, `FolderMoveRelocation` and
twice in `FolderConstructionService`, and some calls check twice.
Proof: `NotebookFolderMoveControllerTest`, `NotebookFolderRenameControllerTest`,
`NotebookGitFolderDissolveGuardControllerTest` and
`NotebookGitFolderRenameGuardControllerTest` stay green;
`grep -rn "Folder not in notebook" backend/src/main` finds one line.

Internal change: replace each copy with `folder.requireInNotebook(notebook)` and
delete the private helpers. Remove a check only where the very next callee
repeats it on the same objects: the request-level check in
`FolderConstructionService.createFolder(notebook, request)`, whose delegate
checks again. Keep the live check under the lock in
`FolderRelocationService.applyLiveFolderChange`; it guards re-read state.
Complexity: about −20 lines, seven copies become one.

### 5. Moving a note and undoing a move use one placement request
Type: Structure (owns retrospective item d directly)
Status: done
Weakness removed: `StoredApiCollection` writes the "move note to folder" request
twice and the "move note to notebook root" request three times. The third root
variant, without a notebook id, can never run, because a recorded move always
carries `originalNotebookId`.
Proof: `frontend/tests/store/storedApi.spec.ts` and
`frontend/tests/store/storeUndoCommand.spec.ts` stay green (plus
`storedApi.trashNote.spec.ts`, `storedApi.reduceRelationNoteToSourceProperty.spec.ts`
and the `NoteUndoButton` specs — `pnpm frontend:test tests/store tests/toolbars`,
17 files/108 tests pass). New private `placeNoteAt` in `StoredApiCollection.ts`
sends the one request; `undoMoveNote`'s unreachable third branch is deleted,
and its `originalNotebookId` parameter is now a required `number`. Undo-of-move
failures now use the richer `throwStoredApiError` shape instead of a plain
`Error`, matching ordinary move failures; no test pinned the old shape.
`StoredApiCollection.ts`: 619 → 583 lines.

Internal change: one private "place note at (folder or notebook root)" step,
used by `moveNoteToFolder`, `moveNoteToNotebookRoot` and undo of a move. It keeps
the richer error (message and status) that the move callers rely on. Delete the
unreachable branch.
Complexity: about −55 lines.

### 6. The stored API's public surface is declared once
Type: Structure (owns retrospective item d directly)
Status: done
Weakness removed: the `StoredApi` interface repeats every public method
signature of its only implementation, so each new action is declared twice.
Proof: `vue-tsc --noEmit` clean; `pnpm frontend:test tests/store
tests/toolbars` (17 files/108 tests) plus three more `storedApi()` consumer
specs, all pass. `StoredApi` interface deleted; `createNoteStorage.ts` types
`storedApi()` as `StoredApiCollection` directly; the class's
`noteEditingHistory` and `storage` are now constructor-private.
`StoredApiCollection.ts`: 583 → 516 lines; `createNoteStorage.ts`: 51 → 49.

Internal change: `createNoteStorage.ts` is the only user of the interface and no
test stubs it. Remove the interface, return the class type, move the three doc
comments onto the methods, and make `noteEditingHistory` and `storage` private.
Complexity: about −80 lines.

### 7. The plain note actions are declared once as a list
Type: Structure (owns retrospective item d directly)
Status: done
Weakness removed: `NoteMoreOptionsActions.vue` declares refine, audio,
assimilation and delete twice, once for the menu and once for the toolbar.
Proof: `pnpm frontend:test tests/notes/NoteMoreOptionsActions.spec.ts
tests/notes/NoteToolbar.moreOptions.spec.ts tests/notes/NoteMoreOptionsForm.spec.ts
tests/notes/NoteMoreOptionsForm.trashNote.spec.ts
tests/notes/NoteMoreOptionsForm.trashNote.relationship.spec.ts
tests/notes/NoteMoreOptionsForm.permanentlyDeleteNote.spec.ts` (6 files, 24
tests) plus the full frontend suite (339 files, 1903 tests) and a clean
`vue-tsc --noEmit`, all pass. `NoteToolbar.moreOptions.spec.ts` pins the
exact `aria-pressed="true"/"false"` values on the toolbar's audio button;
still green.

Internal change: one computed list holds the four plain actions: id, title,
icon, what it runs, whether it is available, and whether it is switched on.
The menu loops over it with `DropdownMenuActionButton`; the toolbar loops
over it with the existing button markup, rendering `aria-pressed` only for
the two toggleable actions (audio, assimilation), matching the pinned test.
Export and questions stay as they are, because their pop-up content differs
per action. The refactor pass extracted the list's construction into a
sibling pure-data module, `noteMoreOptionsPlainActions.ts` (68 lines),
following the same colocated-module pattern as the existing
`noteMoreOptionsTitles.ts`, once the component's own line count crossed 250
after the slice's edit.
Complexity: `NoteMoreOptionsActions.vue` 272 → 225 lines;
`noteMoreOptionsPlainActions.ts` new, 68 lines.

### 8. Text, loading and creation requests are plain functions
Type: Structure (owns retrospective item d directly)
Status: done
Weakness removed: in `StoredApiCollection`, most of each method is the server
request and its error handling. That part needs neither the cache, the undo
history nor the router, yet it sits between them and makes every method long.
Proof: `pnpm frontend:test tests/store tests/toolbars` (17 files/108 tests)
and `vue-tsc --noEmit` (clean) both pass, unchanged — the mocked generated
client meant no test needed editing. `StoredApiCollection.ts`: 516 → 427
lines; new `noteRequests.ts`: 122 lines, holding `toErrorMessage`,
`throwStoredApiError`, `updateTextContentRequest`, `loadNoteRequest`, and
`createNoteRequest`.

Internal change: a new file `frontend/src/store/noteRequests.ts` holds plain
functions. Each one sends one request and returns the data or throws the same
error as today. It holds no state. Move the two error helpers, the title and
content update, note loading and note creation. `StoredApiCollection` keeps
everything that touches the cache, the undo history and the router.

### 9. Placement, trash, permanent deletion and reduction requests are plain functions
Type: Structure (owns retrospective item d directly)
Status: done
Weakness removed: same as slice 8, for the remaining requests.
Proof: `pnpm frontend:test tests/store tests/toolbars` (17 files/108 tests)
and `vue-tsc --noEmit` (clean), both pass. `StoredApiCollection.ts`: 427 →
387 lines. `noteRequests.ts`: 126 → 204 lines.

Internal change: moved the placement request from slice 5
(`placeNoteRequest`), trash (`trashNoteRequest`), undo trash
(`undoTrashNoteRequest`), permanent deletion
(`permanentlyDeleteNoteRequest`) and relationship reduction
(`reduceRelationNoteToSourcePropertyRequest`) into `noteRequests.ts`, each
preserving its call site's exact error shape (throw vs. silent bail). The
refactor pass also collapsed `undoCreateNote`'s own inline
`NoteController.trashNote` call — a pre-existing duplicate of the same
request `trashNoteRequest` now names — onto `trashNoteRequest`, removing the
now-unused `NoteController`, `apiCallWithLoading` and `toErrorMessage`
imports from `StoredApiCollection.ts`. This changes `undoCreateNote`'s
thrown error message from the server-supplied detail to a fixed "Failed to
undo create note"; no test or UI reads that message (verified by grep), so
the behavior is unaffected. `StoredApiCollection.ts` is still 387 lines,
above 250, per this plan's "Current decisions": no slice adds work to reach
250 lines, so this is reported here for wrap-up rather than answered with
another slice.

### 10. Reading a notebook's folders has its own controller
Type: Structure (owns retrospective item d directly)
Status: done
Weakness removed: `NotebookController` mixes the notebook with its folders and
needs 21 collaborators.
Proof: full backend suite (13 affected test classes, including every
`NotebookFolderManagementControllerTestBase` subclass) BUILD SUCCESSFUL;
full frontend suite (339 files/1903 tests) pass; `vue-tsc --noEmit` clean;
`pnpm cy:run --spec e2e_test/features/folder_organization/folder_trash.feature`
3 passing. URLs unchanged; `grep` confirmed no CLI or MCP reference to the
moved methods. `NotebookController.java`: 556 → 479 lines; new
`NotebookFolderController.java`: 113 lines.

Internal change: new `NotebookFolderController` on `/api/notebooks`, following
the sibling-controller pattern. Move `listNotebookFolderListing`,
`listNotebookFolderIndex` and `getFolderPage` unchanged. `NotebookControllerTestBase`
is the only place tests obtain the controller, so it gains one field for the new
controller. Regenerate the TypeScript client, then rename call sites and test
mocks by search and replace.
Sizing exception: the edit is mechanical, but client regeneration and the
backend test run are waits that decomposition cannot shorten.

### 11. Changing a notebook's folders uses the folder controller
Type: Structure (owns retrospective item d directly)
Status: done
Weakness removed: same as slice 10, for the mutating endpoints.
Proof: `pnpm backend:test_only` (full suite) BUILD SUCCESSFUL; full frontend
suite (339 files/1903 tests) pass; `vue-tsc --noEmit` clean;
`e2e_test/features/folder_organization/folder_trash.feature` 3/3 passing.
`NotebookController.java`: 479 → 319 lines (close to the plan's "about
350" estimate); `NotebookFolderController.java`: 113 → 287 lines. Both are
over 250 lines; per decision 3 above and the "Cutting further would be
arbitrary" note, neither is cut further — each holds one concept
(the notebook itself; a notebook's folders).

Internal change: move `createFolder`, `moveFolder`, `trashFolder`,
`permanentlyDeleteFolder`, `renameFolder`, `dissolveFolder`,
`updateFolderReadmeContent` and `resolveDestinationNotebookForFolderMove`
unchanged. Regenerate the client and rename callers the same way.
`NotebookController` drops to about 350 lines and loses the folder-only
collaborators.
Sizing exception: same as slice 10.

## Current decisions

- While slices 1, 5 and 6 edit `StoredApiCollection.ts`, the post-change
  refactor pass does **not** split the file for size. Slices 8 and 9 own that.
  The same holds for `NotebookController.java` until slices 10 and 11.
- No slice adds work to reach 250 lines. Each slice from 5 on reports the
  resulting line count. If a file is still above 250 after its last slice, that
  is reported to the owner at wrap-up, not answered with more slices here.
- Slices 8 to 11 move code unchanged. They do not rename methods, change error
  messages, or redesign what they move.
- Slice 4 changes no status code. Slice 3 owns the only behavior change of
  item (c).
- Report a complexity delta (lines and concepts added or removed) with every
  slice.

## Refinement record

2026-09-19, on the owner's request for narrow scope and simple design:

- Slices 1 to 6: assessed ready, unchanged. Decisions 1 and 2 are confirmed, so
  slices 2 and 3 no longer wait.
- Slice 7: replaced. A plain list replaces the planned new component.
- Former slices 10 and 11 (a cache-aware request layer): replaced by slices 8
  and 9, stateless request functions in one new file.
- Former slices 8 and 9 (folder controller): kept, moved last as slices 10 and
  11, because they are the widest change and remove no duplication. Evidence for
  their size: all 33 affected backend test files get the controller from one
  base class, with 125 call sites.
- Result: 11 slices. No story resplit is needed.

## Learnings

- Slice 1 added a private `noteNoLongerExists(noteId)` step in
  `StoredApiCollection.ts` that only touches `this.storage` and
  `this.noteEditingHistory` (no server request, no router). Slice 9 moves the
  *request* portions of `permanentlyDeleteNote` and
  `reduceRelationNoteToSourceProperty` into `noteRequests.ts`, but this
  cache/undo-history step has no request in it, so it stays in
  `StoredApiCollection` as the cache/undo-owning collaborator that calls into
  the moved request functions after they return.
