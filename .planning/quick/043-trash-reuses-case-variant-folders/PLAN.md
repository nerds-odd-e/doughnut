# Trash reuses a case-variant folder in `_trash`

## Source

**Identity:** quick/043-trash-reuses-case-variant-folders/PLAN.md
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"603a0c5c1d23d8f9ef2a79a63426401aa130ac0211c464acc3402237e58f219e"}}
```

- Kind: bounded retrospective correction; no story.
- Provenance: execution retrospective of SEED-035#story-11
  (seed section and plan recoverable at before-cleanup commit `08fec6ef8d`:
  `.planning/seeds/SEED-035-ai-workspace-supporting-files.md#story-11`,
  `.planning/quick/007-dissolve-merge-folders-with-files/PLAN.md`), story branch
  `story/dissolve-merge-folders-with-files`, commits `1a8b7abff7`..`97c1f73ef5`
  (claim `fb2b68dc1d`). Story 11 promised that every web placement asks the one
  set of names per folder and that a name Donut chooses is the first free one
  (note and folder trash).
- Governing direction: North Star
  [one set of names per folder](../../NORTH-STAR.md#one-set-of-names-per-folder).

## Finding (current truth, `97c1f73ef5`)

Note trash (`NoteTrashService.trash`) and folder trash
(`FolderRelocationService`) mirror the item's folder path under `_trash/`
through `FolderConstructionService.ensureTrashParentFor`. Its
`findOrCreateFolder` looks each segment up with
`FolderSiblingNameValidation.findConflictingSibling`
(`FolderRepository.findCandidateChildContainers`, exact `f.name = :name` on a
`utf8mb4_bin` column). When no exact match exists it calls `createFolder`, whose
check is also exact. Story 11 moved the trashed item's own name to the shared
rule, but not these mirrored ancestor folders.

Example: the owner trashes a note from `Physics/`, so `_trash/Physics/` exists.
They rename the folder to `physics` (a case-only rename, which is allowed), then
trash another note from it. Trash creates `_trash/physics/` beside
`_trash/Physics/`, which puts two case-variant folders on one path in the Git
tree. A checkout on a case-insensitive file system cannot show both, so it no
longer matches the web.

## Goal and scope

An owner who trashes a note or folder gets one folder per mirrored path segment
in `_trash`: a segment that a folder already holds, ignoring letter case, reuses
that folder and keeps its name. Otherwise trash behaves as it does today.

Excluded: renaming existing case-variant folders already in `_trash` (existing
content is never judged again); undo-trash path recovery; a mirrored segment
whose name a note file or a file already holds in `_trash` (rare; left for an
owner decision if it ever matters); local-publish folder creation and
cross-notebook merges (story exclusions; story 10); the `_trash` root lookup.

## Outside-in proof

| Key example | Slice | Proof |
| --- | --- | --- |
| `_trash/Physics/` exists; trashing note `Energy` from `physics/` puts it in `_trash/Physics/Energy.md`, and there is no `_trash/physics/` | 1 | `NoteControllerTrashTests` |
| Existing trash ancestry, first-free-name and collision cases stay green | 1 | `*Trash*` |

## Slices

### 1. Trash reuses a case-variant mirrored folder
Type: Behavior
Status: done
Proof: a new note-trash controller test for the key example above fails first,
then passes; `*Trash*` and `NotebookFolderTrashControllerTest` stay green.

Behavior: `_trash/Physics/` exists and note `Energy` is in `physics/` → the owner
trashes `Energy` → the note is at `_trash/Physics/Energy.md`, no `_trash/physics/`
folder exists.
`ensureTrashParentFor`'s segment lookup uses the shared rule's case-insensitive
folder lookup (`FolderSiblingNameValidation.folderHolding`) instead of the exact
`findConflictingSibling`. Folder trash shares the method, so it gets the same
behavior. `findConflictingSibling` keeps its local-publish and cross-notebook
callers.

Accepted proof: `NoteControllerTrashTests.trashReusesACaseVariantFolderAlreadyInTrash`
(setup: `_trash/Physics` plus root `physics/Energy`; asserts the trashed note's
folder is the existing `_trash/Physics`) failed first (a new folder was
created), then passed. `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test -Dspring.profiles.active=test --build-cache --tests '*Trash*' --tests 'com.odde.donut.controllers.NotebookFolderTrashControllerTest'`
passes (40 tests). Refactor made `findConflictingSibling` private; it has no
caller outside `FolderSiblingNameValidation`.

## Current decisions

- Reuse the existing destination folder and keep its name (the same choice as
  story 11's case-variant merge).
