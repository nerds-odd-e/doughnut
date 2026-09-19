# Close the loose ends left by permanent deletion of trashed content

Status: planned
Source: [SEED-036 story 1](../../seeds/SEED-036-permanent-deletion-loose-ends.md#story-1).
Provenance: follow-ups grouped by the owner during the execution retrospective
of SEED-034 story 1 (plan 146, recoverable at commit `8b8b83e962`).
Authority: 2026-09-19 owner instruction to evaluate each item, choose an
approach, and write a slice plan for the items worth doing. Planning only. This
plan does not authorize execution by itself.

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
| (b) One wording for the folder Trash offer | Low to medium. Two texts say the same thing in different words on one screen. | Very small | Do (slice 2). Surviving wording is a proposed decision. |
| (c) One home and one status for "Folder not in notebook." | Medium. Seven copies, one of them answers 400 where six answer 404. Some checks run twice in one call. | Small | Do (slices 3 and 4). The status is a proposed decision. |
| (d) `StoredApiCollection.ts` (613 lines) | Medium to high. About 140 lines are true duplication: the same move request is written five times, one branch can never run, and every public method is declared twice. The rest mixes three responsibilities. | Small for the duplication, medium for the separation | Do (slices 5, 6, 10, 11) |
| (d) `NoteMoreOptionsActions.vue` (272 lines) | Medium-low. Each of the six note actions is declared twice, once per layout. Every new action pays twice; the permanent-deletion story already did. | Small | Do (slice 7) |
| (d) `NotebookController.java` (562 lines) | Medium. Ten folder endpoints live in the notebook controller. The tests already treat them as their own concept (`NotebookFolder*ControllerTest`), and sibling controllers under `/api/notebooks` are an existing pattern (`NotebookBooksController`, `NotebookHealthController`, `NotebookGroupController`). | Medium. About 50 client call sites and 33 backend test files are renamed mechanically. URLs do not change. | Do (slices 8 and 9) |

Considered and declined, with reasons:

- **Moving the three notebook Git endpoints out of `NotebookController`.** It
  would remove about 65 lines but touch 89 backend test files. The cost is far
  above the value. After slices 8 and 9 the controller is about 350 lines and
  holds one concept, the notebook itself (catalog, settings, sharing, readme,
  indexing, export, Git). Cutting further would be arbitrary. This plan treats
  that remainder as resolved by explicit decline; the owner may overrule.
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

## Proposed decisions for the owner

The seed leaves two decisions open. Each has a recommended answer. The slice
that depends on it executes only after the owner confirms or revises it. All
other slices are independent of both.

1. **Surviving Trash wording (slice 2).** Recommended: keep the description's
   impersonal wording, because the permanent-delete text beside it is also
   impersonal ("this cannot be undone").
   - Subject: `Trash folder "<name>" with its complete subtree`
   - Consequence: `Its contents leave active use. References remain authored, but may no longer resolve until the folder is recovered with Move.`
   - Description is `<subject>. <consequence>`; confirmation is `<subject>? <consequence>`.
2. **Status for "Folder not in notebook." (slice 3).** Recommended: **404**
   everywhere. Six of seven copies already answer 404 and four tests pin it. No
   test pins the 400, and no client reads this status or message. In the same
   method, a missing folder id already answers 404.

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
consistency owners and are not touched: slices 8 and 9 move endpoints without
changing which service owns a change. No new North Star topic is needed.

## Outside-in proof

| Promise from the seed | Owning slice | Observable proof |
| --- | --- | --- |
| Trash a note, permanently delete it, press Undo: nothing fails and nothing claims to restore it | 1 | Mounted `NoteUndoButton`: after trash then permanent deletion through `storedApi()`, the undo button for that note is gone and an older entry of another note is offered instead |
| Folder Settings states the Trash consequences once, in one wording | 2 | `FolderPage.trash.spec.ts`: description and confirmation contain the identical consequence sentence |
| One status for "Folder not in notebook." | 3 | `NotebookNoteCreateControllerTest`: creating a note in another notebook's folder answers 404 with that reason |
| One home for "Folder not in notebook." | 4 | The text exists once in `backend/src/main`; the four tests that pin it stay green |
| The three oversized files are resolved | 5 to 11 | Existing specs stay green; line counts and the recorded decline above |

Focused commands:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/path/to/File.spec.ts
CURSOR_DEV=true nix develop -c pnpm backend:test_only
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/path/to.feature
```

## Ordered slices

Order: the user-visible item first, then cheapest maintenance value first. The
story may stop safely after any slice. First to drop: 10 and 11, then 8 and 9.

### 1. Undo offers nothing for a note that no longer exists
Type: Behavior
Status: planned
Proof: new case in `frontend/tests/toolbars/NoteUndoButton.visibility.spec.ts`;
`frontend/tests/store/storedApi.reduceRelationNoteToSourceProperty.spec.ts`
stays green.

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
Status: planned, waits for proposed decision 1
Proof: `frontend/tests/pages/FolderPage.trash.spec.ts`.

Behavior: an active folder's Settings tab is open → the owner reads the
description and then presses Trash → the confirmation shows the same subject and
the same consequence sentence as the description, differing only in "." and "?".

Approach: in `folderRemovalOffer.ts`, replace `trashDescription` and
`trashConfirmation` with one subject and one consequence, composed exactly like
the permanent-delete pair. Update the pinned phrase in the spec.
Complexity: about −2 lines, two wordings become one.

### 3. Creating a note in another notebook's folder answers 404
Type: Behavior
Status: planned, waits for proposed decision 2
Proof: new case in
`backend/src/test/java/com/odde/donut/controllers/NotebookNoteCreateControllerTest.java`.

Behavior: folder F belongs to notebook B → the owner creates a note in notebook
A with `folderId` = F → 404 "Folder not in notebook.", and no note is created.

Approach: add `Folder.requireInNotebook(Notebook)` as the home of the refusal
and use it in `NoteConstructionService.buildNote`.

### 4. Every "Folder not in notebook." refusal comes from the one home
Type: Structure (owns retrospective item c directly)
Status: planned
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
Status: planned
Weakness removed: `StoredApiCollection` writes the "move note to folder" request
twice and the "move note to notebook root" request three times. The third root
variant, without a notebook id, can never run, because a recorded move always
carries `originalNotebookId`.
Proof: `frontend/tests/store/storedApi.spec.ts` and
`frontend/tests/store/storeUndoCommand.spec.ts` stay green.

Internal change: one private "place note at (folder or notebook root)" step,
used by `moveNoteToFolder`, `moveNoteToNotebookRoot` and undo of a move. It keeps
the richer error (message and status) that the move callers rely on. Delete the
unreachable branch.
Complexity: about −55 lines.

### 6. The stored API's public surface is declared once
Type: Structure (owns retrospective item d directly)
Status: planned
Weakness removed: the `StoredApi` interface repeats every public method
signature of its only implementation, so each new action is declared twice.
Proof: frontend type check and `frontend/tests/store/*.spec.ts` stay green.

Internal change: `createNoteStorage.ts` is the only user of the interface and no
test stubs it. Remove the interface, return the class type, move the three doc
comments onto the methods, and make `noteEditingHistory` and `storage` private.
Complexity: about −80 lines.

### 7. Each note action is declared once for both layouts
Type: Structure (owns retrospective item d directly)
Status: planned
Weakness removed: `NoteMoreOptionsActions.vue` declares export, questions,
refine, audio, assimilation and delete twice, once for the menu and once for the
toolbar.
Proof: `frontend/tests/notes/NoteMoreOptionsActions.spec.ts` (runs every case in
both layouts) and the `NoteMoreOptionsForm.*.spec.ts` files stay green.

Internal change: one small layout-aware button component renders an action as a
menu item or as a toolbar button. One shared rule covers the two toggles: an
action that is switched on shows as pressed in the toolbar and is absent from
the menu. The two pop-up actions (export, questions) get the same treatment.
Complexity: about −60 lines in the component, one small new component.

### 8. Reading a notebook's folders has its own controller
Type: Structure (owns retrospective item d directly)
Status: planned
Weakness removed: `NotebookController` mixes the notebook with its folders and
needs 21 collaborators.
Proof: the backend tests of the moved endpoints, the frontend specs that mock
them, and `e2e_test/features/folder_organization/folder_trash.feature` stay
green. URLs are unchanged, so CLI and MCP are unaffected.

Internal change: new `NotebookFolderController` on `/api/notebooks`, following
the sibling-controller pattern. Move `listNotebookFolderListing`,
`listNotebookFolderIndex` and `getFolderPage`. Regenerate the TypeScript client
and rename the client call sites and test mocks. Backend test base classes gain
a field for the new controller.

### 9. Changing a notebook's folders uses the folder controller
Type: Structure (owns retrospective item d directly)
Status: planned
Weakness removed: same as slice 8, for the mutating endpoints.
Proof: `NotebookFolder*ControllerTest`, `NotebookGit*Folder*ControllerTest`,
frontend folder specs, and `folder_trash.feature` stay green.

Internal change: move `createFolder`, `moveFolder`, `trashFolder`,
`permanentlyDeleteFolder`, `renameFolder`, `dissolveFolder`,
`updateFolderReadmeContent` and `resolveDestinationNotebookForFolderMove`.
Regenerate the client and rename callers. `NotebookController` drops to about
350 lines and loses the folder-only collaborators.

### 10. Server requests that keep cached notes current have their own home: text, loading, creation
Type: Structure (owns retrospective item d directly)
Status: planned
Weakness removed: `StoredApiCollection` mixes three responsibilities: asking the
server and refreshing the cache, recording and running undo, and navigation.
Undo already needs the first without the second ("update text without undo").
Proof: `frontend/tests/store/*.spec.ts` and
`frontend/tests/toolbars/NoteUndoButton.*.spec.ts` stay green.

Internal change: a new module in `frontend/src/store/` owns "ask the server,
refresh the cached note realms, raise a clear error". It knows nothing about undo
or the router. Move the text update, note loading and note creation requests and
the two error helpers there. `StoredApiCollection` keeps user actions: record
history, call the request, navigate, undo.

### 11. The same home serves placement, trash, permanent deletion and reduction
Type: Structure (owns retrospective item d directly)
Status: planned
Weakness removed: same as slice 10, for the remaining requests.
Proof: same specs as slice 10 stay green; both files are under 250 lines.

Internal change: move the placement request from slice 5, the trash and undo
trash requests, permanent deletion and relationship reduction.

## Current decisions

- While slices 1, 5 and 6 edit `StoredApiCollection.ts`, the post-change
  refactor pass does **not** split the file for size. Slices 10 and 11 own that.
  The same holds for `NotebookController.java` until slices 8 and 9.
- Slice 4 changes no status code. Slice 3 owns the only behavior change of
  item (c).
- The remainder of `NotebookController` after slice 9 is declined as described
  under "Considered and declined".
- Report a complexity delta (lines and concepts added or removed) with every
  slice.

## Learnings

None yet.
