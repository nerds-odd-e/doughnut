# Permanently delete trashed notes and folders

Status: in progress
Source: [SEED-034 story 1](../../seeds/SEED-034-permanently-delete-trashed-content.md#story-1).
Authority: 2026-09-18 owner instruction to refine the story, write a slice plan
and refine it if needed. Planning only. This plan does not authorize execution
by itself.

Baseline: `eedaf172eb` on `main`.

## Execution identity

- Mode: Story Branch Mode.
- Originating checkout: `/Users/terryyin/git/doughnut`, integration branch `main`.
  The **Taken** claim is commit `c619aba43a` there.
- Execution checkout: `/Users/terryyin/git/doughnut/.worktrees/146-permanently-delete-trashed-content`,
  branch `146-permanently-delete-trashed-content`, created from that claim.
- Integration checkout and branch for later wrap-up: `/Users/terryyin/git/doughnut`, `main`.
- Authorized push destination: `origin` `146-permanently-delete-trashed-content`.
- CI observer: GitHub Actions, workflow `ci.yml` (display name `donut CI`),
  repository `nerds-odd-e/doughnut`, target branch `146-permanently-delete-trashed-content`,
  observer directory `/tmp/dough-ci-501/watch-u2JuvM`.

## Goal and scope

A notebook owner who opens a note or folder that is in trash finds
**Permanently delete** where active content offers Trash. After a confirmation,
the content and everything it owns leave Donut. The value is a consistent
interface and a trash that can be kept clean. Removing sensitive content is not
the purpose: earlier Git history keeps the text, and the confirmation says so.

Included:

- A trashed note's action, in the toolbar, the menu and the `d` shortcut,
  permanently deletes the note with its memory trackers, recall prompts,
  questions, images and conversations.
- A trashed folder's settings offer permanent deletion of the folder, its
  README, every nested folder and note, and each removed note's dependent data.
  Empty folders and the `_trash` folder itself follow the same rule.
- Always confirm first. Cancelling changes nothing and sends no request.
- Authored links in surviving notes stay as they are and no longer resolve.
- Active notes and folders keep today's Trash behavior.

Rejection constraint: the server refuses permanent deletion of content that is
not in trash. The product rule is trash first, delete second, so that learning
history is never lost in one step. It mirrors the existing "Folder is already
in trash" refusal in `FolderRelocationService.trashFolderWithinNotebook`.

Excluded: a dedicated Empty trash feature, retention timers, a CLI or MCP
command, erasing earlier Git commits or clones, a reference-handling prompt at
deletion time (the choice was made at trash time), the "reduce to property"
option for trashed relationship notes, cleanup of mirror folders left empty
under `_trash`, removing the stale "undo trash note" entry of a note that was
later permanently deleted, and any change to what Dissolve offers on a trashed
folder.

Assumptions:

- Every notebook has a Git binding, so one confirmation wording that mentions
  Git history serves all notebooks. If execution finds notebooks without a
  binding, keep the single wording and record the finding here.
- Permanent deletion removes every learner's trackers for the note, not only
  the owner's. Git publication of a file deletion already behaves this way.
- In a notebook whose live content already differs from accepted Git history,
  the deletion still succeeds in the database and appends no commit. That is
  the existing per-notebook drift policy of `AcceptedWebChangeService`; this
  story does not change it.

## Existing solutions and selected design

| Responsibility | Existing owner / evidence | Choice |
| --- | --- | --- |
| Remove one note with its complete dependent data, leaving dead links | `NoteService.permanentlyRemove`, used by Git publication and by `RelationReduceService` | Reuse unchanged. |
| Web change to one note inside the accepted-change boundary | `WebNoteEditService.edit`, already used by `NoteTrashService.trash`; `RelationReduceService` shows a note can be removed inside `AcceptedWebChangeService.apply` | Add the operation to `NoteTrashService`, beside `trash`. Do not write a second lock/commit path. |
| Web change to one folder inside the accepted-change boundary | `FolderRelocationService.applyLiveFolderChange`, used by `trashFolderWithinNotebook` | Add the folder operation beside `trashFolderWithinNotebook` and reuse the private helper. |
| Walk a folder subtree | `FolderSubtree.collectFolders` and `collectNoteIdsInSubtree` | Reuse. |
| Safe removal order for folders | Git publication removes notes first, then folders deepest first (`NotebookGitProposalOrdinaryNoteApplication.applyDeletions`, `NotebookGitProposalAcceptance.reconcileUnrepresentedFolders`) | Same order on the web. `fk_note_folder` is `ON DELETE SET NULL`, so a folder row removed while it still holds notes would leave those notes active at the notebook root. |
| Know whether content is in trash | Backend `Note.isTrashed()` and `Folder.isTrashed()`; frontend `isLocationInTrash` in `utils/folderTrash.ts`, used by `NoteShow.vue`, `FolderPage.vue` and `FolderSettings.vue` | Reuse. Move the note-realm form of the check from `NoteShow.vue` into `folderTrash.ts` so the toolbar and the page share one rule. |
| Frontend handling of a note that no longer exists after the call | `StoredApiCollection.reduceRelationNoteToSourceProperty`: `removeNoteRealm`, route away, `refreshSidebarStructuralListings` | Same pattern. Land on the note's containing folder page, as Trash does. No undo entry. |
| Frontend handling after a folder is gone | `routeAfterFolderRemoval` in `folderAdminMutations.ts`, used by trash and dissolve | Reuse. |
| Confirmation | `popups.confirm` in `useNoteTrashFlow.ts` and `useFolderAdmin.ts` | Reuse. No new component. |

Routes: `DELETE /api/notebooks/{notebook}/folders/{folder}` is already Dissolve,
so use explicit routes beside the trash routes, for example
`POST /api/notes/{note}/permanently-delete` and
`POST /api/notebooks/{notebook}/folders/{folder}/permanently-delete`. Do not make
the existing trash endpoints state-dependent: a repeated trash request must
never become an irreversible deletion.

Common rule across all slices: *content located under `_trash` is removed notes
first, then folders deepest first, inside one accepted web change.* A single
note is the one-note case of that rule. Do not add a second removal routine for
the `_trash` root, for empty folders, or for notes that are in trash only
because their folder was trashed; location under `_trash` is the only form of
trash membership.

Expected production shape: two controller methods, one method in
`NoteTrashService`, one in `FolderRelocationService`, a state-dependent branch
in `useNoteTrashFlow.ts` with one store method, and a state-dependent block in
`FolderSettings.vue` with one mutation function.

### Direction and decisions carried

- [NORTH-STAR, One complete accepted web change](../../NORTH-STAR.md#one-complete-accepted-web-change):
  both deletions lock, apply the complete domain operation, and append exactly
  one accepted commit in the same transaction. A folder deletion is one commit,
  not one per note. No new topic is needed.
- ADR 0004: `_trash` matching stays case-insensitive through the existing
  checks.
- ADR 0006: no catch around the removal. A failure rolls back and surfaces.

## Outside-in proof

| # | Promise (seed example) | Owning slice | Proof |
| --- | --- | --- | --- |
| 1 | A trashed note with learning history and a conversation is removed with its dependent records and can no longer be opened | 1, 2 | Controller test; E2E |
| 2 | Deleting one note in a trashed folder leaves the other content of that folder | 1, 2 | Controller test; E2E |
| 3 | A surviving note that links to the deleted note keeps its authored text | 1 | Controller test |
| 4 | A note outside trash still offers Trash; the server refuses to permanently delete it | 1, 2 | Controller test; existing `NoteMoreOptionsForm.trashNote.spec.ts` stays green |
| 5 | The deletion appends exactly one accepted commit whose tree lacks the removed paths | 1, 3 | Git controller tests |
| 6 | A trashed folder with a README, a note, and a nested folder holding another note is removed entirely with dependent data; sibling folders and notes remain; no removed note reappears at the notebook root | 3, 4 | Controller test; E2E |
| 7 | An empty trashed folder, and the `_trash` folder itself, can be permanently deleted | 3 | Controller test |
| 8 | A folder outside trash still offers Trash; the server refuses to permanently delete it | 3, 4 | Controller test; existing `FolderPage.trash.spec.ts` stays green |
| 9 | Confirmation names the item, warns that dependent data is lost and that earlier Git history keeps the text; cancelling sends nothing | 2, 4 | Mounted-component tests |

Commands, run from the execution worktree:

- Backend:
  `unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL && CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests '<pattern>'`
- Frontend: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/path/to/File.spec.ts`
- E2E: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/path/to.feature`

## Ordered slices

### Slice 1 — Permanently deleting a trashed note removes it with its dependent data in one accepted change

Type: Behavior
Status: done

Behavior: a note located under `_trash` has a memory tracker and a
conversation, a peer note shares its folder, and a surviving note links to it →
someone who can edit the notebook calls the permanent-delete endpoint for the
note → the note, its tracker and its conversation no longer exist, the peer and
the referrer's authored text are unchanged, and the notebook's accepted Git
history gained exactly one commit whose tree lacks the note's path. Rejection
constraints on the same trigger: an active note answers 400 and nothing
changes; a user who cannot edit the note is refused.

Add the endpoint beside `trashNote` in `NoteController` and the operation
beside `trash` in `NoteTrashService`, through `WebNoteEditService.edit` with
`NoteService.permanentlyRemove(note, LEAVE_DEAD_LINKS, viewer)` as the
mutation and a commit message such as `Permanently delete note: <title>`. The
endpoint returns no body.

Proof: promises 1–5. Extend `NoteControllerTrashTests` or add a sibling for the
data and rejection assertions, reusing the tracker fixture idiom of
`NotebookGitDeletionPublicationControllerTest`. Add one Git controller test next
to `NotebookGitWebTrashControllerTest`, reusing its base class, for the single
commit and the tree. Pattern: `--tests '*PermanentDelet*'`, then
`--tests 'com.odde.donut.controllers.NotebookGitWeb*'` and
`--tests '*NoteControllerTrash*'` to show trash behavior is preserved.

Wrap-up for this slice regenerates the TypeScript API client
(`generate-api-client` skill).

Sizing note: the Git proof stays in this slice because the accepted commit is
produced by the same single production change; a separate slice would be tests
only.

Safe stopping point: yes. The endpoint works without the button.

### Slice 2 — A trashed note offers Permanently delete behind a confirmation

Type: Behavior
Status: planned

Behavior: the owner opens a note that is in trash → the toolbar button, the
menu item and the `d` shortcut read "Permanently delete note (d)" → confirming
the warning deletes the note, lands on its containing folder page, and the
sidebar no longer lists it, while a peer note in the same trashed folder stays.
Cancelling sends no request. No undo entry is recorded. An active note still
runs today's trash flow.

Move the note-realm trash check into `utils/folderTrash.ts`. In
`useNoteTrashFlow.ts`, branch once at the top on that check: trashed →
`popups.confirm` then a new `StoredApiCollection` method following
`reduceRelationNoteToSourceProperty`; otherwise the existing flow untouched.
Title comes from `noteMoreOptionsTitles.ts`. Confirmation wording, to keep or
tighten:

> Permanently delete "<title>"? Its learning history, questions, conversations
> and images are deleted too, and this cannot be undone. Earlier Git history of
> this notebook still contains its text.

Proof, E2E first and kept explicitly unfinished until green: one scenario in
`e2e_test/features/note_creation_and_update/note_deletion.feature` seeded with
two notes under `_trash/Biology` (the existing `Folder | _trash/Biology`
fixture form), reusing the sidebar and "is in trash" steps; add one step for
the action. Then a mounted-component test beside
`frontend/tests/notes/NoteMoreOptionsForm.trashNote.spec.ts` for the label, the
confirmation text and the cancel path (promise 9). Follow the `e2e-authoring`,
`frontend` and `unit-testing` skills.

Safe stopping point: yes. Notes are complete; folders still only offer Move and
Dissolve in trash, as today.

### Slice 3 — Permanently deleting a trashed folder removes its whole subtree in one accepted change

Type: Behavior
Status: planned

Behavior: a folder under `_trash` holds a README, a note with a memory tracker,
and a nested folder with another note; a sibling folder with a note sits beside
it → someone who can edit the notebook calls the folder permanent-delete
endpoint → the folder, its README, the nested folder, both notes and their
trackers no longer exist, the notebook has no note without a folder that it did
not have before, the sibling is unchanged, and accepted Git history gained
exactly one commit whose tree lacks the subtree. The same call succeeds for an
empty trashed folder and for the `_trash` folder itself. Rejection constraint:
an active folder answers 400 and nothing changes.

Add the endpoint beside `trashFolder` in `NotebookController` and the operation
beside `trashFolderWithinNotebook` in `FolderRelocationService` through
`applyLiveFolderChange`: collect the subtree with `FolderSubtree`, remove every
note with `NoteService.permanentlyRemove`, flush, then remove folders deepest
first. Commit message such as `Permanently delete folder: <name>`.

Proof: promises 5–8. One controller test class next to
`NotebookFolderTrashControllerTest` for data, `_trash` root, empty folder and
rejection; one Git controller test next to
`NotebookGitWebFolderTrashControllerTest` for the single commit and tree.
Pattern: `--tests '*PermanentDelet*'`, then
`--tests 'com.odde.donut.controllers.Notebook*FolderTrash*'`.

Wrap-up regenerates the TypeScript API client.

Safe stopping point: yes.

### Slice 4 — A trashed folder's settings offer Permanently delete behind a confirmation

Type: Behavior
Status: planned

Behavior: the owner opens the settings of a folder that is in trash → where an
active folder shows "Trash folder", a "Permanently delete folder" button is
shown → confirming the warning removes the subtree, lands on the parent folder
page (or the notebook page for `_trash` itself), and the sidebar no longer
lists the folder, while a sibling trashed folder stays. Cancelling sends no
request. An active folder still shows Trash.

In `FolderSettings.vue`, fill the trashed branch of the existing
`folderIsTrashed` condition. Add the action to `useFolderAdmin.ts` and the
mutation to `folderAdminMutations.ts`, following `trash` / `trashFolderOnPage`
and reusing `routeAfterFolderRemoval`. Confirmation wording, to keep or
tighten:

> Permanently delete folder "<name>" and everything inside it? All its notes
> are deleted with their learning history, questions, conversations and images,
> and this cannot be undone. Earlier Git history of this notebook still
> contains the text.

Proof, E2E first and kept unfinished until green: one scenario in
`e2e_test/features/folder_organization/folder_trash.feature` seeded directly
under `_trash/...` as its second scenario already does, reusing the folder page
and sidebar steps. Then a mounted-component test beside
`frontend/tests/pages/FolderPage.trash.spec.ts` for the button, the
confirmation text and the cancel path (promise 9).

Safe stopping point: yes. Story complete.

## Refinement assessment

Four slices, each one trigger with one observable result and one proof loop,
ordered so every stop leaves a usable product. The examples exercise one rule
(remove notes first, then folders deepest first, in one accepted change) rather
than accumulating special cases: the empty folder, the `_trash` root and the
note trashed through its folder need no code of their own. Backend and frontend
are separate slices because each endpoint is independently observable and safe
to stop after, not because of layers.

Considered and excluded: see Goal and scope. The most visible exclusion is the
stale "undo trash note" entry: trash a note, then permanently delete it, then
press Undo, and the undo request fails because the note is gone.

Remaining concerns:

- Slice 3: the flush between removing the notes and removing the folders is
  what keeps `fk_note_folder ... ON DELETE SET NULL` from firing. Consequence:
  promise 6's "no note reappears at the notebook root" assertion is the guard;
  do not weaken it.
- Slices 1 and 3: the accepted-commit proof rides in the same slice as the data
  proof. Consequence: these two slices are the largest; each still has a single
  production change.
- Slices 2 and 4: E2E plus component test around one control. Consequence: if
  E2E setup overruns, stop and refine instead of dropping the scenario.

Resolved during refinement: removing a whole subtree inside
`AcceptedWebChangeService.apply` needs no second snapshot path. After the
operation, `apply` flushes and `commitIfChanged` re-reads folders and stored
notes from the database (`notebookGitStateLoader.foldersOf` / `storedNotesOf`)
before building `PortableTreeSnapshot`, so removed rows are simply absent.
`apply` also runs the operation when a notebook has no Git binding
(`ifPresent`), so the deletion works there and appends nothing.

A separate slice-plan refinement pass was judged unnecessary: the four slices
are already single-outcome and the one integration uncertainty is resolved
above.

## Current decisions

- Always confirm; the confirmation states that earlier Git history keeps the
  text.
- The `_trash` folder itself may be permanently deleted by the ordinary folder
  rule.
- Links in surviving notes are left dead; no prompt at deletion time.
- Separate explicit endpoints; the trash endpoints stay as they are.

## Learnings

### Slice 1

- Delivered: `NoteTrashService.permanentlyDelete(noteId, notebookId)` beside `trash`
  on the same `WebNoteEditService.edit` accepted-change boundary, and
  `POST /api/notes/{note}/permanently-delete` in `NoteController` returning no body.
  The refusal `400 "Note is not in trash."` is evaluated inside the `edit` mutation,
  on the locked re-resolved note rather than the controller's instance — the same
  position as `"Folder is already in trash."` in
  `FolderRelocationService.trashFolderWithinNotebook`.
- Accepted proof: `NoteControllerPermanentDeleteTests` (three tests) and
  `NotebookGitWebPermanentDeleteControllerTest` (one test). Promises 1–3 are proved
  together by `removesTheTrashedNoteWithItsDependentDataAndKeepsTheRestOfTheNotebook`,
  which shares one heavy fixture; promise 4 by
  `refusesToPermanentlyDeleteANoteThatIsNotInTrash` and
  `rejectsPermanentDeleteOfAnotherOwnersTrashedNote`; promise 5 by the Git test's
  parent count of 1 and its tree of exactly `Atoms.md` + `_trash/Biology/.keep`.
  Commands: `--tests '*PermanentDelet*'`, `--tests 'com.odde.donut.controllers.NotebookGitWeb*'`,
  `--tests '*NoteControllerTrash*'` — all green.
- Fixture idiom for destructive paths: with a note's dependents managed in the same
  persistence context, Hibernate's flush after removing the note raises
  `TransientPropertyValueException` from a managed `MemoryTracker` still pointing at
  it. Real requests never hold those rows in session, so this is a test artifact. The
  repo's existing remedy is `makeMe.entityPersister.flushAndClear()` after seeding and
  reloading by id, as `MemoryTrackerDeleteControllerTest` does. **Slice 3 will hit the
  same thing** when it removes a folder subtree; use the same idiom rather than
  production-side defensiveness.
- `FolderBuilder.inTrashOf(Notebook)` now owns the fixture knowledge "a folder is in
  trash when it sits under `_trash`". Slices 3 and 4 should reuse it instead of
  re-spelling the parent-folder chain.
- The TypeScript API client was regenerated: `NoteController.permanentlyDeleteNote`
  posts to `/api/notes/{note}/permanently-delete`. Slice 2 consumes it.
- No evidence contradicted the North Star topic "One complete accepted web change".
