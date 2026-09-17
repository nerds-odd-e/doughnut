# Use trash vocabulary for recoverable note removal

Status: planned
Source: [SEED-020 story 1](../../seeds/SEED-020-trash-vocabulary.md#story-1),
refined 2026-09-17. Owner authorized planning and execution in the same
instruction.

## Goal and scope

Donut maintainers and contributors currently meet the recoverable
`_trash`/undo-trash journey described as "delete" and "soft-delete" in the
DTO/enum exposed by the trash endpoint, two production comments, several
frontend production filenames/identifiers, and their tests. Accepted ADR 0001
reserves "delete" for permanent removal. Align every maintained name for the
recoverable journey to trash vocabulary; behavior, routes, and data are
unchanged.

Included (confirmed in code, 2026-09-17):

- Backend: `NoteDeleteDTO` -> `NoteTrashDTO`, `NoteDeleteReferenceHandling` ->
  `NoteTrashReferenceHandling` (both used only by `NoteController.trashNote`
  through the API; `NoteService.permanentlyRemove` takes the same renamed
  enum by direct Java reference — it has no DTO of its own and reference
  handling is one policy shared by trash and permanent removal). Update every
  caller: `NoteController`, `NoteTrashService`, `NoteService`,
  `ControllerTestBase` (`leaveDeadLinks`/`removeFromProperties`/
  `reduceToSourceProperty` helpers keep their names, only their DTO/enum
  types change).
- Backend comments: `AuthoredNoteReferenceInboundFacade` ("soft-deleted
  referrer" -> "trashed referrer") and `WikiLinkResolver` ("excluding
  soft-deleted notes" -> "excluding trashed notes"). Reword
  `NoteReferenceHandling`'s class Javadoc ("note-delete reference policies")
  to name the shared trash/permanent-removal policy without claiming either
  term exclusively.
- Regenerate the frontend API client (`NoteDeleteDto` -> `NoteTrashDto` and
  friends) after the backend rename.
- Frontend production rename: `StoredApiCollection.ts` type aliases
  (`NoteDeleteReferenceHandling` -> `NoteTrashReferenceHandling`,
  `NoteDeleteOptions` -> `NoteTrashOptions`, `noteReferenceHandlingBody` stays);
  `useNoteTrashFlow.ts` imports; `NoteDeleteButton.vue` ->
  `NoteTrashButton.vue` and its import site(s); `relationNoteReduceOnDelete.ts`
  -> `relationNoteReduceOnTrash.ts` (`qualifyRelationNoteForReduceOnDelete` ->
  `qualifyRelationNoteForReduceOnTrash`,
  `RelationNoteReduceOnDeleteQualification` ->
  `RelationNoteReduceOnTrashQualification`) and its one import site in
  `useNoteTrashFlow.ts`.
- Frontend test rename, only where the test is confirmed exclusively about
  the trash flow (button title "Trash note", `trashNote` SDK call):
  `noteMoreOptionsDeleteTestSupport.ts`, `NoteMoreOptionsForm.deleteNote.spec.ts`,
  `NoteMoreOptionsForm.deleteNote.relationship.spec.ts`,
  `NoteShowPage.autosaveDelete.spec.ts`, `relationNoteReduceOnDelete.spec.ts`,
  and every identifier/describe string inside them that says "delete" for
  this journey.

Excluded (confirmed unrelated "delete" concepts that keep their name):
`RichMarkdownEditor.propertyDeleteLocation.spec.ts` (editor property-row
deletion), `MemoryTrackerPageView.deleteUnanswered.spec.ts` (permanent recall
prompt deletion), `NoteService.deleteOrphanImagesForPersistedContent`
(permanent orphan-image cleanup), `NoteService.permanentlyRemove`'s own name,
and any other backend/frontend "delete" naming not touching the recoverable
`_trash` journey. Historical Flyway migrations keep their original wording.

## Outside-in proof

| Promise | Proof |
| --- | --- |
| Trash endpoint behavior unchanged after the rename | `NoteControllerTrashTests` green |
| Generated client matches the renamed backend contract | `pnpm generateTypeScript` succeeds with no leftover `NoteDelete*` in `packages/generated/donut-backend-api` |
| Frontend trash flow unchanged after the rename | `frontend:test` for the renamed spec files green |
| No maintained name still says delete/soft-delete for the recoverable journey | `git grep -rniE "notedelete|soft.?delete"` (case-insensitive) returns nothing outside historical migrations and the excluded unrelated "delete" concepts above |

## Ordered slices

### 1. Rename the shared trash reference-handling contract in the backend

Type: Structure
Status: planned
Proof: focused Gradle run of `*NoteControllerTrashTests*` and
`*NotebookGitWebTrash*` green; `git grep -n "NoteDeleteDTO\|NoteDeleteReferenceHandling"`
returns nothing.

Rename `NoteDeleteDTO` -> `NoteTrashDTO` and `NoteDeleteReferenceHandling` ->
`NoteTrashReferenceHandling` (file renames plus every import/usage:
`NoteController`, `NoteTrashService`, `NoteService`, `ControllerTestBase`).
Fix the two soft-delete comments and the `NoteReferenceHandling` class
Javadoc. No behavior change; `permanentlyRemove`'s signature keeps working
with the renamed type.

### 2. Regenerate the frontend API client

Type: Structure
Status: planned
Proof: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`; diff limited
to `NoteDelete*` -> `NoteTrash*` renames in `packages/generated/donut-backend-api`.

### 3. Rename the frontend production trash surface

Type: Structure
Status: planned
Proof: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/store/storedApi.trashNote.spec.ts`
green; frontend typecheck passes (no leftover `NoteDelete*` import).

Rename `StoredApiCollection.ts` type aliases, `NoteDeleteButton.vue` ->
`NoteTrashButton.vue`, `relationNoteReduceOnDelete.ts` ->
`relationNoteReduceOnTrash.ts` (and its exported names), updating every
import site (`useNoteTrashFlow.ts` and the component's parent).

### 4. Rename the frontend trash-flow tests

Type: Structure
Status: planned
Proof: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteMoreOptionsForm.trashNote.spec.ts tests/notes/NoteMoreOptionsForm.trashNote.relationship.spec.ts tests/pages/NoteShowPage.autosaveTrash.spec.ts tests/utils/relationNoteReduceOnTrash.spec.ts`
green (file names shown post-rename).

Rename `noteMoreOptionsDeleteTestSupport.ts`,
`NoteMoreOptionsForm.deleteNote.spec.ts`,
`NoteMoreOptionsForm.deleteNote.relationship.spec.ts`,
`NoteShowPage.autosaveDelete.spec.ts`, `relationNoteReduceOnDelete.spec.ts`,
and every "delete"-named identifier/describe string inside them for this
journey (`deleteNoteButton`, `clickDeleteNote`, `deleteNoteSpy`,
`qualifyingRelationRealmForDelete`, `mountDeleteFormReady`,
`setupNoteMoreOptionsDeleteFormTests`, `awaitDeleteSideEffects`, `startDelete`,
etc.) to their trash equivalents.

### 5. Confirm the maintained surface agrees with the glossary

Type: Behavior
Status: planned
Proof: `git grep -rniE "notedelete|soft.?delete"` returns nothing outside
historical migrations and the excluded unrelated concepts named above;
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` and
`CURSOR_DEV=true nix develop -c pnpm frontend:test` full runs green.

## Current decisions

- Owner decision (2026-09-17): execute this story now even though it renames
  `REDUCE_TO_SOURCE_PROPERTY`/`sourcePropertyKey`, which the unexecuted,
  already-taken SEED-024 story 1 (plan 135) will later delete outright. Plan
  135 has been updated to re-target its slice 6 at the renamed identifiers.
- Update (2026-09-17, plan 135 wrap-up): SEED-024 story 1 (plan 135) is now
  complete and its history removed. Its slice 6 already deleted
  `REDUCE_TO_SOURCE_PROPERTY`/`sourcePropertyKey` outright — `NoteDeleteDTO`
  and `NoteDeleteReferenceHandling` now only carry `LEAVE_DEAD_LINKS`/
  `REMOVE_FROM_PROPERTIES`, so this story's rename applies to that simplified
  two-value shape with no reduce-to-source-property coordination needed. Two
  scope items above are stale and need re-verification before executing this
  plan's Included frontend-rename slice: `ControllerTestBase.reduceToSourceProperty`
  was deleted (no longer exists to keep its name through the DTO/enum rename);
  `qualifyRelationNoteForReduceOnDelete`/`RelationNoteReduceOnDeleteQualification`
  were replaced by a simplified `isRelationshipNote(noteRealm): boolean` in
  `relationNoteReduceOnDelete.ts` (still that filename) — re-check its current
  export name/shape before renaming it to `relationNoteReduceOnTrash.ts`. The
  dedicated reduce endpoint (`RelationController.reduceToSourceProperty`,
  `RelationReduceService`, `StoredApiCollection.reduceRelationNoteToSourceProperty`)
  is a separate, unrelated, already-shipped feature and keeps its name — it is
  not part of this trash-vocabulary rename.
- The shared reference-handling enum takes the trash-flavored name because it
  is exposed to API/frontend consumers exclusively through the trash
  endpoint; `permanentlyRemove`'s own method name already carries the
  permanent-deletion meaning, so reusing the renamed type does not mislead.

## Learnings

None yet.
