# Land on the containing folder from one place

**Identity:** quick/044-one-containing-location/PLAN.md
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"b5be9428a5728c85c45fce30e0102d61a751e6942848be4842cf35080cf08b0e"}}
```

## Source

- Kind: bounded retrospective correction; no seed.
- Corrects the execution of SEED-035#story-2 ("Delete unwanted supporting
  files from Web Donut"), plan
  `.planning/quick/035-delete-notebook-file-on-web/PLAN.md` at `c35f7d3956`
  (removed at wrap-up). Reviewed commits
  on `story/delete-notebook-file-on-web`: `20968e7810`, `e5e4057848`
  (claim `d2cf105398`).
- Finding (re-verified at `e5e4057848`, 2026-09-26): slice 1 moved the folder
  pages' landing rule into `frontend/src/routes/containingLocation.ts`
  (`containingLocationOf(realm)`: the last ancestor folder's page, otherwise
  the notebook page) and used it for folder removal, dissolve and the file
  delete. `frontend/src/store/StoredApiCollection.ts` keeps a private
  `containingLocation(notebookId, folderId)` that makes the same decision for
  trashing and permanently deleting a note, fed by `realmLeafFolder(realm)`,
  which is the same "last ancestor folder" lookup. The slice 1 refactor noted
  it as a later merge. Two homes for one rule mean a route change has to be
  made twice.

## Goal and scope

Beneficiary: maintainers of the web pages. "Where the reader lands once the
note, folder or file they were viewing is gone" has one home.

Included: `StoredApiCollection.trashNote` and `permanentlyDeleteNote` call
`containingLocationOf(cachedRealm)` (a `NoteRealm` has `notebookRealm` and
`ancestorFolders`); delete the private `containingLocation` helper. Keep
`realmLeafFolder` where it is still needed (the trash history records the
original folder id).

Excluded: the notebook-fallback navigation in the undo path of
`StoredApiCollection` (a different decision: where to go when a restored note
cannot be shown); any change to which page the reader lands on.

Preserved promises: trashing or permanently deleting a note lands on its
folder page, or on the notebook page for a root note; folder removal,
dissolve and file delete keep landing where they do now.

## Outside-in proof

| Promise | Slice | Observable proof |
| --- | --- | --- |
| Note trash and permanent delete land as before | 1 | `frontend/tests/store/storedApi.trashNote.spec.ts` (folder case now expects string params), `frontend/tests/notes/NoteMoreOptionsForm.permanentlyDeleteNote.spec.ts` green |
| Folder and file removals land as before | 1 | `frontend/tests/pages/FolderPage.removal.spec.ts`, `FolderPage.spec.ts`, `frontend/tests/pages/AttachmentPage.spec.ts` green, unchanged |
| One home | 1 | `git grep -n "function containingLocation" frontend/src` finds only `routes/containingLocation.ts`; `vue-tsc` clean |

## Slices

### 1. Note removal lands through the shared containing location
Type: Structure
Status: done
Proof: all rows above. Command: `CURSOR_DEV=true nix develop -c pnpm
frontend:test tests/store/storedApi.trashNote.spec.ts
tests/notes/NoteMoreOptionsForm.permanentlyDeleteNote.spec.ts
tests/pages/FolderPage.removal.spec.ts tests/pages/FolderPage.spec.ts
tests/pages/AttachmentPage.spec.ts` — 5 files, 30 tests pass; `vue-tsc`
clean; the grep finds only `routes/containingLocation.ts`.

Learnings: the planned `FolderPage.trash`/`FolderPage.permanentlyDelete`
spec paths do not exist; folder removal landing is observed in
`FolderPage.removal.spec.ts`. The trash spec's folder case asserted numeric
params; it now expects the string params the shared rule produces.

Change: replace the two `containingLocation(...)` calls in
`StoredApiCollection.ts` with `containingLocationOf(cachedRealm)` and delete
the private helper. No test edits are expected; if a test asserts numeric
versus string route params, adjust the test to the observable page rather than
the param type.

Sizing: one file, two call sites and one deletion; well under five minutes.

## Current decisions

- This correction is optional cleanup, not a defect. The owner may decline
  it; then close it without execution.
