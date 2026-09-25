# Move existing uploaded note pictures into their notebooks

## Source

- Identity: SEED-035#story-5.
- Source: [refined story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-5)
  (refined 2026-09-25, owner accepted the proposed decisions).
- Governing direction: North Star
  [one attachment content model](../../NORTH-STAR.md#one-attachment-content-model):
  "One way in" (store the verified object first, then accept the pointer with
  the reference that uses it in one accepted change through the existing
  owner; Donut-chosen names take a free name) and "Moving and retiring" (per
  notebook, resumable, keep the old copy until story 18).
- **Start condition:** met. Story 14 (every notebook uses LFS) and story 4
  (web uploads become files, `WebNoteImageUploadService`) are on main; no code
  creates `image` rows any more.

## Goal and scope

Every note whose `image:` names a legacy upload (`/attachments/images/{id}/…`)
owned by a note in the same notebook gets that picture as an LFS file in its
own folder, and `image:` names the file. Each notebook gets one Donut System
commit. The move runs once on its own at startup, is safe to run again, and
isolates a failing notebook.

Excluded (seed): references to another notebook's upload (left unchanged,
counted), references whose row is gone, unreferenced rows, body-text legacy
addresses, remote URLs, Books, deleting legacy rows (story 18), history
rewrite, any UI or owner trigger, the 10 MiB check.

Assumptions (checked on main `42ef5d8ff2`, 2026-09-25):

- `WebNoteImageUploadService.upload` already does "one way in" for one note:
  `NotebookAttachmentContent.storeAsLfsPointer` before the transaction, then
  `WebNoteEditService.edit` → `AcceptedWebChangeService.apply` with a mutation
  (`addFile`, l.109) that saves a `NotebookAttachment` in `note.getFolder()`
  and persists `AuthoredNoteContent.prepareDocumentForSave(NoteContentMarkdown.withNoteImage(content, filename), …)`
  through `AuthoredNoteDocumentPersistence.persist`.
  `NotebookGitTreeEncoder.derive` picks the inserted row up through
  `ProjectionChangeCapture`. `apply` is not reentrant: one `apply` per notebook.
- Every accepted web commit is already authored "Donut System"
  (`NotebookGitCutoverService.SYSTEM_AUTHOR_NAME`). The move calls
  `AcceptedWebChangeService.apply` directly, not `WebNoteEditService.edit`,
  because that one asserts the current user's authorization.
- `AuthoredNoteDocumentPersistence.persist(note, content, updatedAt)` sets
  `updatedAt`; passing the note's own `getUpdatedAt()` keeps last-updated
  unchanged without a second save path. It also runs orphan cleanup, which
  skips a note-relative `image:` (`InvalidPathPresent`), so the legacy row
  stays as the backup (`replacingALegacyPictureKeepsItsMaskAndItsRow`).
- Legacy bytes: `Image` (`name`, `contentType`, lazy `blob.data`); no
  repository, queries go through `entityPersister.createQuery("FROM Image i …")`.
  `NoteContentMarkdown.leadingFrontmatterImageReference` returns
  `Referenced(imageId)` for a legacy value.
- Taken names: `NotebookGitAcceptedTree.hasPath` covers files, notes and
  folders in the accepted tree. `NumberedNameSelection.firstAvailable`
  (package-private, `services`) is the product's numbering rule (" (2)") but
  numbers after the extension.
- Precedent: story 14's startup conversion (added `071d0e0861`…`1e2ed84c35`,
  removed `0284ea7f52`) used a `@Configuration @Profile("!test")`
  `ApplicationReadyEvent` listener guarded only by data, a per-notebook
  `@Transactional` service call in `try/catch RuntimeException` with
  `logger.error`, and `@Order(Ordered.HIGHEST_PRECEDENCE)` on
  `FlyWayFreeVersionRealMigration.actualMigration()` so Flyway runs first.
  Its tests called the service and the listener method directly.

PFE: reuse the upload's add-file mutation, `AcceptedWebChangeService`,
`storeAsLfsPointer`, `NoteContentMarkdown`, `NumberedNameSelection`, and story
14's startup shape. Gap: numbering before a file extension and choosing names
for several pictures in one change.

## Outside-in proof (key examples)

| Promise (seed example) | Slice | Observable proof |
| --- | --- | --- |
| Own picture becomes a file beside its note in one Donut System commit; mask, learning and last-updated unchanged; legacy row kept (1) | 2 | `LegacyNotePictureMoveControllerTest`: one new commit by Donut System; tip has `physics/example.png` as a pointer whose stored object is the blob's bytes; note content `image: example.png` with the same `image_mask`; `updatedAt` unchanged; `legacyImageCount` unchanged; `assertAcceptedTreeMatchesTheFullAssembly` |
| Run again / no legacy references → no commit (5, 6) | 2 | same test class: second move adds no commit; a notebook without legacy references gains none |
| Taken or non-plain name → next free numbered name (2) | 3 | same class: folder holding `example.png` → `example (2).png`; two notes in one folder both uploading `example.png` → `example.png` and `example (2).png`; stored name `.png` → `picture.png` |
| Other note's upload in the same notebook → own copy; another notebook's → unchanged and counted; missing row → unchanged (3, 4) | 4 | same class, one test per case; the move's result counts the skipped other-notebook reference |
| Every notebook at startup; a failing notebook doesn't stop the others; rerun resumes (5) | 5 | `LegacyNotePictureMoveOnStartup` method called directly: healthy notebook moved, broken one unchanged, second run moves nothing more |
| Pull/clone gives the file beside the note (1) | 6 | E2E `cli_notebook_lfs.feature`: legacy picture from fixture `moon.jpg` on note `force` in `physics`, move, CLI clone has `physics/moon.jpg` equal to the fixture and `physics/force.md` containing `image: moon.jpg` |
| Unpublished local frontmatter edit → ordinary rebase conflict (7) | — | Not proven separately: ordinary Git rebase of a Donut System commit, as for every web change |

Focused backend command (a linked worktree isolates the test DB automatically):

```
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests '<pattern>'
```

Focused E2E command:

```
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_lfs.feature
```

## Slices

### 1. The upload and the move share one "attach a picture file to a note" step
Type: Structure
Status: done — `NoteImageFileAttachment.attach(note, filename, pointer, updatedAt)`;
accepted proof: `NoteControllerUploadNoteImageTests` (15) and
`NotebookGitDerivedTreeOracleControllerTest` (7) green, unchanged.
Proof: `NoteControllerUploadNoteImageTests` and
`NotebookGitDerivedTreeOracleControllerTest` green, unchanged.

Extract `WebNoteImageUploadService`'s `addFile` mutation (save the
`NotebookAttachment` in the note's folder with the pointer, then persist the
prepared content with `image:` naming the file) into one step that takes the
note, filename, pointer and the `updatedAt` to persist with. The upload keeps
calling it with its own timestamp. Enables slice 2; no behavior change.

### 2. A notebook's own legacy pictures become files beside their notes
Type: Behavior
Status: done — `LegacyNotePictureMove.move(notebookId)`; accepted proof:
`LegacyNotePictureMoveControllerTest` (3: own picture moved in one Donut System
commit, rerun adds no commit, notebook without legacy references gains none).
Proof: new `LegacyNotePictureMoveControllerTest` (extends
`NotebookGitWebContentControllerTestBase`): the slice 2 rows of the proof
table.

Pre-condition: LFS notebook, note `force` in folder `physics` with a legacy
`Image` (`makeMe.anImage().forNote(note)`), `image:
/attachments/images/{id}/example.png` and an `image_mask`. Trigger: the move
for that notebook. Add a `LegacyNotePictureMove` service whose per-notebook
method finds the notebook's notes with a `Referenced` legacy value whose
`Image` belongs to a note in the same notebook, stores each picture's bytes with
`storeAsLfsPointer` first, then runs one `AcceptedWebChangeService.apply`
("Move uploaded pictures into the notebook") that re-checks each note's
`image:` and applies slice 1's step with the note's own `updatedAt`. In this
slice the stored name is used as is.

### 3. A taken or non-plain name gets the next free numbered name
Type: Behavior
Status: planned
Proof: slice 3 rows of the proof table, in `LegacyNotePictureMoveControllerTest`;
existing `NumberedNameSelection` callers' tests (folder and note title
numbering) green.

The name is the stored name's last `/` segment, prefixed with `picture` when
it is empty or starts with `.`. When it is taken in the note's folder (in the
accepted tree, or by a name this move already chose), number it before the
extension with the existing rule: `example (2).png`. Extend
`NumberedNameSelection` for the extension; its current callers keep their
behavior.

### 4. Only a picture from the same notebook is moved, and skipped references are reported
Type: Behavior
Status: planned
Proof: slice 4 row of the proof table.

Slice 2's rule already gives each referring note in the notebook its own file
(same stored object); this slice proves that case, proves that a reference to
another notebook's `Image` or to a missing row is left unchanged, and makes the
per-notebook result report how many other-notebook references were skipped.
Rejection justified by the owner decision (2026-09-25) recorded in the seed.

### 5. The move runs once at startup for every notebook and isolates failures
Type: Behavior
Status: planned
Proof: slice 5 row of the proof table; `docs/notebook-git-attachments.md`
describes the move.

Add `LegacyNotePictureMoveOnStartup` (`@Configuration @Profile("!test")`,
`ApplicationReadyEvent`), guarded only by data: the ids of notebooks with a
note whose content contains `/attachments/images/`. Each notebook runs in its
own transaction inside `try/catch RuntimeException`, logging an error for a
failure and a warning for skipped other-notebook references; restore
`@Order(Ordered.HIGHEST_PRECEDENCE)` on Flyway's listener. Break one notebook
in the test the way story 14 did (for example, delete its object-store row).
Replace the legacy-picture paragraph of `docs/notebook-git-attachments.md` with
the move's rules. Story 18 deletes this listener with the rest of the legacy
picture code.

### 6. The owner's clone receives a moved picture beside its note
Type: Behavior
Status: planned
Proof: slice 6 row of the proof table.

Add a scenario to `cli_notebook_lfs.feature` next to "A picture uploaded on
the web arrives in the owner's clone beside its note", reusing its clone steps.
It needs two testability endpoints: seed a legacy picture on a note from a
fixture, and run the startup move's method (the E2E backend starts before
seeding, so its own startup finds nothing). Sizing exception: the E2E run is
an external wait.

## Current decisions

- One domain rule from slice 2 on: a note's legacy `image:` owned in the same
  notebook becomes a file in the note's folder under a free name. Slice 3
  extends the naming and slice 4 adds the skip report; neither adds a second
  recognizer.
- The moved note's content is prepared exactly like an upload
  (`prepareDocumentForSave`), so it may gain the same normalization a web save
  gives it; `image_mask:` and body are unchanged.
- Bytes are stored before the accepted change. If the note changed in
  between, the unused object stays (no object GC, per the North Star).
- Legacy rows and bytes are not touched; story 18 removes them.

## Learnings

- Slice 2 calls `NoteImageFileAttachment.attach` (package `services.notebookGit`)
  inside its own `AcceptedWebChangeService.apply` mutation.
- `assertAcceptedTreeMatchesTheFullAssembly` resets accepted history
  (`snapshotCurrentPortableTree`), so "run again adds no commit" is its own test.
- `ownLegacyPictures` reads `image.getNote().getNotebook()`; slice 4 must decide
  the case of a legacy row without a note (treat as not in this notebook).
- Shared test helpers (`lfsPointerStoredFor`, `tipContent`, `tipText`,
  `legacyImageCount`) live in `NotebookGitWebContentControllerTestBase`.
