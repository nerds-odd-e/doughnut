# Give the frontend note store a cohesive architecture

## Source

- Story: [SEED-049#story-1](../../seeds/SEED-049-note-store-architecture.md#story-1)
- **Identity:** SEED-049#story-1
- Assumes SEED-047#story-2 ([015-removal-landing-listing-load](../015-removal-landing-listing-load/PLAN.md))
  is executed first: removal landing already loads its listing through the
  shared user-action load and builds rows through the one sidebar-rows function.
  If it is not done when this plan starts, stop and run it first.

## Goal and scope

One outcome: the web frontend's note store is small and domain-named. Note
commands are in one place, undo (records and their reversal) in another, both
reached through `useNoteStore()`. Every store file is within 250 lines. Product
behavior is unchanged, except that creating a note or relationship sends fewer
requests.

Preserved: the story's key examples 1, 2, 4, 5 and 7 (typing autosave
re-renders only that note and skips the sidebar refresh; title rename refreshes
the sidebar; trash/create/move undo); the error toast on failed requests
(`apiCallWithLoading`); ADR 0006 (fail loudly).

Excluded (story deferrals): in-flight load de-duplication, optimistic updates,
Pinia or any new state library, and any backend change.

## Architectural decisions

- PFE: no state library exists. The project's idiom for shared frontend state
  is a module-level singleton behind a composable (`useFeatureToggle`,
  `useRecallData`); `useNoteStore()` follows it. The per-note ref cache
  (`NoteStorage.ts`) is reused as-is; it is what keeps re-rendering
  fine-grained.
- No Accepted ADR covers frontend state. ADR 0006 governs the failure style.
  The North Star has no frontend topic, and this plan adds none (the design
  is local to the store and recorded in the seed).
- Target files (names may be adjusted during execution, but not the
  boundaries): `store/noteStore.ts` (entry point and note commands),
  `store/noteUndo.ts` (history, record types, reversal), `store/NoteStorage.ts`
  (cache, unchanged), `store/noteRequests.ts` (SDK calls, one failure style).
  Removed: `StoredApiCollection.ts`, `createNoteStorage.ts`,
  `NoteEditingHistory.ts`, `composables/useStorageAccessor.ts`.

## Outside-in proof

| Promise | Owning slice and observation |
| --- | --- |
| Note and relationship creation unchanged for the person | 1: `NoteNewForm`, `NoteUnresolvedWikiLinkModal` and `AddRelationshipFinalize` specs green with the re-save expectations removed; E2E `note_topology/wiki_link.feature`, `note_topology/property_wiki_link.feature`, `relationships/add_relationship.feature` |
| Failed requests show the toast and stop the command | 2: existing refused-request specs (for example the refused upload in `tests/store/storedApi.spec.ts`) adjusted to expect rejection; trash, delete and reduce specs green |
| Move, read and edit commands unchanged | 3: `SearchForm` move specs, `NoteRealmLoader` consumers, `TextContentWrapper` / `NoteEditableContent` specs green; E2E `note_topology/note_move.feature` |
| Removal landing unchanged | 4: `tests/store/storedApi.noteRemovalLanding.spec.ts` examples re-homed to the removal flow (`NoteMoreOptionsForm` trash / permanent-delete entry) and green; E2E `note_creation_and_update/note_deletion.feature` |
| Undo unchanged, label reactive | 5: `NoteUndoButton` specs, `NoteTextContentUndo.spec.ts`, `storeUndoCommand.spec.ts` examples re-homed or kept where no component spec proves them; E2E `note_creation_and_update/note_edit.feature`, `note_creation.feature` |
| Typing does not refresh the sidebar; title rename does | 6: one assertion added to the existing content-save component spec (`NoteEditableContent.saveResponse.spec.ts`) on `sidebarStructuralRefreshKey`; existing specs for the rest |
| Store tests observe through high-level entry points | 7: `tests/store/` holds only what no component spec proves |

Focused checks per slice: `CURSOR_DEV=true nix develop -c pnpm frontend:test <affected spec dirs>`
and the frontend skill's typecheck. E2E per the table:
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec <feature>`.

## Ordered slices

### 1. Creating a note or relationship sends no wiki-link re-save

Type: Structure
Status: done
Proof: slice 1 row above.

Accepted proof (2026-09-27): `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteNewForm.spec.ts tests/notes/NoteNewForm.submit.spec.ts tests/notes/NoteUnresolvedWikiLinkModal.spec.ts tests/wiki-link-or-relationship/AddRelationship.spec.ts tests/store/storedApi.spec.ts`
passed 40 tests; `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`
passed. `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_topology/wiki_link.feature,e2e_test/features/note_topology/property_wiki_link.feature,e2e_test/features/relationships/add_relationship.feature`
passed all 26 scenarios. Mounted creation forms observe request payload and
navigation; wiki-link features observe missing/piped target creation and live
links; relationship feature observes creation and undo. Independent refactor:
no edits. The intermediate store size is owned by slices 4–7 as planned.

Removes a leftover workaround. The backend dropped its resolved wiki-link cache
(`V300000316__drop_resolved_wiki_link.sql`) and resolves links live. Delete
`refreshWikiLinkCacheForNote` and the `refreshWikiLinkCacheForNoteIds` option
through `createRootNoteAtNotebook`, `noteNewFormSubmit.ts`, `NoteNewForm.vue`
(`wikiLinkCacheRefreshSourceNoteId`), `AddRelationshipFinalize.vue`, the
`sourceNoteId` prop of `NoteUnresolvedWikiLinkModal` and its `NoteShow`
binding, and the `refreshWikiLinkCacheForNote` store spec. Remove any test
expectation of the re-save. Add no reload and no absence test. If an E2E
scenario fails because it relied on the re-save, stop and report it rather than
adding a replacement.

### 2. Note requests fail in one way

Type: Structure
Status: done
Proof: slice 2 row above.

Accepted proof: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/store tests/notes/NoteMoreOptionsForm.trashNote.spec.ts tests/notes/NoteMoreOptionsForm.permanentlyDeleteNote.spec.ts tests/notes/NoteMoreOptionsForm.trashNote.relationship.spec.ts tests/pages/NoteShowPage.imageUpload.spec.ts tests/managedApi/clientSetup.spec.ts tests/managedApi/clientSetup.loading.spec.ts`
passed 69 tests across 11 files; `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`
passed. Existing refused-upload assertion observes rejection and unchanged
sidebar key; mounted removal/reduction/upload specs observe successful flows;
clientSetup specs observe error toast and rejection loading cleanup. Request
error and missing-payload checks preserved. Independent refactor: no edits.

Removes four failure styles. `trashNoteRequest`, `permanentlyDeleteNoteRequest`,
`reduceRelationNoteToSourcePropertyRequest` and `uploadNoteImageRequest` throw
on error like the others. The commands drop their `if (!result) return`
branches. Keep field-error enrichment (title, creation). Keep
`undoCreateNote`'s trash call, which already throws.

### 3. One move, one read, no leftovers in note commands

Type: Structure
Status: done
Proof: slice 3 row above.

Accepted proof: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/store tests/wiki-link-or-relationship/SearchDialog.spec.ts tests/notes/TextContentWrapper.spec.ts tests/notes/NoteEditableContent tests/pages/NoteShowPage tests/components/notes/NoteTextContentUndo.spec.ts tests/notes/NoteNewForm tests/wiki-link-or-relationship/AddRelationship.spec.ts`
passed 124 tests across 21 files; `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`
passed. `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_topology/note_move.feature`
passed the move-and-undo scenario. SearchDialog observes confirmed folder/root
placement; store completion observes missing-note loading; page/editor tests
observe loading and edits. NoteRealmLoader retains its explicit route-triggered
load and reads the cache directly, avoiding duplicate requests. Only the cache
absence guard remains in placement undo (required by its type). Refactor: no edits.

CI repair included: run `36294751826`, attempt 1, frontend job `108551410141`
failed at slice 2 on NoteShowPage.autosaveTrash's unhandled Vue warning. The
same failure was found locally by slice 3 before notification; its existing
implementation agent owned the test repair. The spec now observes the expected
propagated error with a restored test-only Vue handler; no production catch.
The final focused run includes the repaired case and passes. Nix startup and
focused proof waits accounted separately from bounded implementation.

Removes duplicate commands. Make these changes:
- Replace `moveNoteToFolder` and `moveNoteToNotebookRoot` with one
  `moveNote(noteId, target)` using `placeNoteAt`'s target shape.
- Keep one read path, `getNoteRealmRefAndLoadWhenNeeded` (may be renamed),
  plus `loadNoteRealm`. Remove `getNoteRealmRef` and the private `loadNote`
  duplicate.
- Drop `noteReferenceHandlingBody`, and replace the `NoteTrashOptions` alias
  with `NoteTrashDto`.
- Drop the `placementUndoForNote` guards the types do not require, and the
  `completeContent` unused variable. It must keep loading a missing note,
  because undo needs the old text.
- Drop return values no caller uses (`trashNote`,
  `reduceRelationNoteToSourceProperty`, `undo`, `focusNoteRealm`,
  `createRootNoteAtNotebook`), unless a spec observes them through a component.

### 4. The removal flow owns where to land

Type: Structure
Status: planned
Proof: slice 4 row above.

Removes the store's dependence on sidebar code. Move `locationAfterRemoving`
beside the sidebar order it uses (next to `neighborNoteAfterRemoval`).
`useNoteRemovalFlow` reads the destination before removal and navigates after
it. `trashNote` and `permanentlyDeleteNote` take no router. The store no longer
imports `sidebarStructuralSort`, `usePeerSort`, `useNoteSidebarTree` or the
listing request. Keep the order: read the listing, remove, navigate, refresh
the cache and sidebar.

### 5. Undo lives in one module

Type: Structure
Status: planned
Proof: slice 5 row above.

Removes scattered undo. `store/noteUndo.ts` holds the history (explicitly
reactive state), the record types, recording helpers, and one `undoLast()`.
`undoLast()` reverses the latest record and returns the route to open (note,
notebook page, or notebooks). `undo(router)` becomes
`router.push(await undoLast())`. Remove `undoInner`, `undoMoveNote`,
`undoCreateNote` (folded in) and `NoteEditingHistory.ts`. `peekUndo` and
`discardUndo` move here.

### 6. One entry point to the note store

Type: Structure
Status: planned
Proof: slice 6 row above.

Removes the hidden deep-reactive wrapper and the three-layer access path. Make
these changes:
- Create `useNoteStore()`, which returns one module singleton: the cache
  reads, the note commands, and undo.
- Rewrite `storageAccessor.value.storedApi().x(...)` and
  `storageAccessor.value.x(...)` in about 20 source files as
  `noteStore.x(...)`.
- Replace test resets (`storageAccessor.value = createNoteStorage(...)`) in
  `RenderingHelper` and specs with one reset helper.
- Delete `StoredApiCollection.ts` (its commands now live in `noteStore.ts`),
  `createNoteStorage.ts` and `useStorageAccessor.ts`.
- Add the sidebar-refresh assertion from the proof table.

Sizing exception: about 45 mechanical call-site edits, driven by typecheck. If
it overruns, split source call sites from test call sites. Keep the old
accessor delegating to the new store until the second part.

### 7. Store tests prove only what components do not

Type: Structure
Status: planned
Proof: slice 7 row above; `wc -l frontend/src/store/*.ts` shows each file
≤ 250 lines.

Removes duplicate low-level tests (unit-testing skill: observe through
high-level entry points). Remove each remaining `tests/store/` example that a
component spec already proves. Keep examples with no component entry, such as
the note realm cache rules. Prefer fewer tests; move nothing just to keep a
count.

## Current decisions

- Execution: Story Branch Mode in
  `/Users/terryyin/.codex/worktrees/note-store-architecture/doughnut`, branch
  `codex/note-store-architecture`; originating and integration checkout
  `/Users/terryyin/git/doughnut`. Managed worktree created by this chat.
  Starting revision: `30b470c4251f69bb5eba9b4928836210ed750dc9`.
  Publisher: `codex-01a0e0fe-e37c-7512-a6b3-b8d4c1318365` (Yua-chan).
  Claim accepted on `origin/main` and execution branch:
  `ae374ab6da9299dc9364454dbbf8d734ba6feb0c`.
  Increment target: `origin/refs/heads/codex/note-store-architecture`.
  Plan 015 is complete and integrated (`c2fefdc293`).
  Setup: `./scripts/run.sh bash scripts/worktree_setup.sh` and
  `./scripts/run.sh pnpm -C frontend exec vue-tsc --version` passed.
  Existing planned-execution refinement authority retained; no scope change.
  The claim's trunk CI is unobserved; branch delivery owns subsequent coverage.
- CI: GitHub Actions `ci.yml` (`donut CI`), branch
  `codex/note-store-architecture`; Codex yielded observer cell 29, session 33954,
  PID 11700, mailbox `/tmp/dough-ci-501/watch-Fd6Y7J`, owned by the publisher
  above and bound to the execution checkout. Workflow selector verified.
- Accepted execution-branch increment: `3821dbd9c79c7ab25e68cd1ff96aa44061d6544f`
  (slice 1); observer reused and registration confirmed by managed delivery.
- Accepted execution-branch increment: `a08a318db1165bbfac47cbf9675c8619615dc833`
  (slice 2); observer reused and registration confirmed by managed delivery.

- Owner, 2026-09-27: delete the wiki-link re-save with nothing left behind: no
  replacement reload, no absence test, no historical note.
- `NoteStorage.ts` (one reactive ref per note) is not redesigned; its behavior
  is the responsiveness baseline.
