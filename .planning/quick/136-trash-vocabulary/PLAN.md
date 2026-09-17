# Use trash vocabulary for recoverable note removal

Status: done
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
Status: done
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
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`; diff limited
to `NoteDelete*` -> `NoteTrash*` renames in `packages/generated/donut-backend-api`.

### 3. Rename the frontend production trash surface

Type: Structure
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/store/storedApi.trashNote.spec.ts`
green; frontend typecheck passes (no leftover `NoteDelete*` import).

Rename `StoredApiCollection.ts` type aliases, `NoteDeleteButton.vue` ->
`NoteTrashButton.vue`, `relationNoteReduceOnDelete.ts` ->
`relationNoteReduceOnTrash.ts` (and its exported names), updating every
import site (`useNoteTrashFlow.ts` and the component's parent).

### 4. Rename the frontend trash-flow tests

Type: Structure
Status: done
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
Status: done
Proof: `git grep -rniE "notedelete|soft.?delete"` returns nothing outside
historical migrations and the excluded unrelated concepts named above;
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` and
`CURSOR_DEV=true nix develop -c pnpm frontend:test` full runs green.

## Current decisions

- Owner decision (2026-09-17): execute this story now even though it renames
  `REDUCE_TO_SOURCE_PROPERTY`/`sourcePropertyKey`, which the unexecuted,
  already-taken SEED-024 story 1 (plan 135) will later delete outright. Plan
  135 has been updated to re-target its slice 6 at the renamed identifiers.
- The shared reference-handling enum takes the trash-flavored name because it
  is exposed to API/frontend consumers exclusively through the trash
  endpoint; `permanentlyRemove`'s own method name already carries the
  permanent-deletion meaning, so reusing the renamed type does not mislead.

## Learnings

Execution found more misleading names than slice 1's own scope text listed,
all within the same authorized boundary (recoverable `_trash` journey), and
fixed them alongside:

- The shared reference-handling method `NoteService.applyNoteDeleteReferenceHandling`
  (called by both `trash()` and `permanentlyRemove()`) renamed to the neutral
  `applyNoteReferenceHandling` rather than a trash-flavored name, since it is
  directly invoked from the permanent-removal path too and a trash-flavored
  method name there would newly mislead.
- Five backend test files used "soft-deleted"/`softDelete` for notes moved
  beneath `_trash` (`NoteRealmServiceTest`, `NotebookFolderCrossNotebookMoveMergeControllerTest`,
  `WikiLinkResolverReferenceResolutionTest`, `DeadWikiLinkHealthRuleTest`,
  `OkfIncompatibleTitleHealthRuleTest`); renamed test/method names to "trashed"/`trashNote`.
- The keyboard-shortcut id `"note-delete"` (`keyboardShortcuts.ts`,
  `NoteMoreOptionsActions.vue`) and its test helper
  `dispatchNoteDeleteShortcut` (`NoteMoreOptionsActions.spec.ts`) named the
  same trash action; renamed to `"note-trash"`/`dispatchNoteTrashShortcut`.
- `NoteDeleteButton.vue` turned out to be dead code (no import or template
  usage anywhere, confirmed by search). The post-change refactor pass removed
  it entirely instead of renaming it to `NoteTrashButton.vue`, per this
  project's dead-code check (code with no caller, exposed by tracing this
  change). The generated `frontend/components.d.ts` had already partially
  regenerated from a background watcher (added a `NoteTrashButton` entry
  without removing the stale `NoteDeleteButton` ones); removed both the stale
  and the now-also-unneeded `NoteTrashButton` entries by hand.
- `storedApi.spec.ts` had one test titled "...instead of soft-delete",
  contrasting with the old model; reworded to drop the legacy term entirely
  rather than keep it as a contrast.
- Confirmed as out of scope and left unchanged: `NoteNewForm.wikidata.spec.ts`'s
  describe block "wikidata and soft-delete" (about cancelling a wikidata
  search dialog, unrelated to note removal); `docs/notebook-publication-profiling.md`
  and `SEED-018`'s references to `requireNoSoftDeletedTitleAt` /
  `findSoftDeletedByNotebookFolderAndTitleOrderByIdAsc` (methods no longer
  exist in code; the docs describe a past investigation, i.e. historical
  material).
