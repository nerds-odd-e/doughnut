# Code, API and docs say "image", and the UI says "File"

## Source

- Story: [SEED-050#story-7](../../seeds/SEED-050-local-ai-notebook-technical-debt.md#story-7)
- **Identity:** SEED-050#story-7
- Split from SEED-050#story-5 on 2026-09-27 after re-evaluation against
  `d9abdcc2eb` (owner decision: keep ADR 0001's Attachment and Image, rename
  "picture" to "image", add "Short UI: **File**" to Attachment; do not rename
  Attachment; keep the two image endpoints as separate resources).
- Vocabulary: [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md) (UI,
  API and schema use the same nouns).

## Goal and scope

New work copies one name per concept: Attachment and Image in code, API and
docs, and File in the UI; the crossed image endpoint names stop misleading
developers.

Included: rename picture identifiers, the file page realm field, the two
image-serving endpoints and their operations, user messages, docs, North Star
and E2E phrases to "image"; add the ADR line.

Excluded: renaming Attachment; merging the endpoints; the upload endpoint
`POST /api/notes/{note}/images`.

SEED-050#story-2 (plan 021) and SEED-050#story-4 (plan 023) have landed;
plan 024 (SEED-050#story-5) is finishing and also edits
`docs/notebook-git-attachments.md` and `docs/note-content-saving.md`, so
slice 4 rebases onto it.

## Starting facts (rechecked 2026-09-27 at `3d4d6055c0`)

Paths under `backend/src/main/java/com/odde/donut/`.

- Picture identifiers: `services/notebookAttachment/PictureFile`,
  `controllers/InlinePicture`, `services/MovedNotePicture` (and
  `placeWithPicture` in `NoteMoveService`), `controllers/dto/NotebookAttachmentRealm.picture`,
  `services/notebookGit/WebNoteImageUploadService`. `NoteMoveService` also
  names picture in a field, constructor parameter and javadoc; `PictureFile`
  now uses `NotebookAttachment.NEW_PAYLOAD_LIMIT_BYTES`.
- Endpoints: `GET /api/notes/{note}/attachment-image?path=` →
  `NoteAttachmentImageController.showAttachmentImage` (the note's authored
  `image:` path, used by `frontend/src/components/notes/widgets/ShowImage.vue`);
  `GET /api/notebooks/{n}/attachments/{a}/picture` →
  `NotebookAttachmentController.showAttachmentPicture` (used by
  `frontend/src/pages/AttachmentPage.vue`). Only the frontend calls them.
- Messages: "Cannot upload <name>: a picture must be a png, jpg, jpeg, gif or
  webp file." (`PictureFile:38`), "… already exists; rename the picture and
  upload it again" (`WebNoteImageUploadService:104`), "Not a raster picture."
  (`InlinePicture:24`).
- No test asserts the three messages: `NoteControllerUploadNoteImageTests:181`
  checks only "png, jpg, jpeg, gif or webp", its name-clash cases (`:93` …)
  only the path, `NoteAttachmentImageControllerTest:83` only the 415 status.
- API text: OpenAPI summaries and descriptions at
  `NoteAttachmentImageController:39,42`, `NotebookAttachmentController:86-87`,
  `NotebookAttachmentRealm:14`, repeated in the generated `open_api_docs.yaml`. Frontend: `pictureSrc` and the CSS class
  `attachment-picture` (`AttachmentPage.vue:14,73,107`). The generated
  `showAttachmentImage` / `ShowAttachmentImageData` is today the note
  endpoint (`ShowImage.vue:35`); URL assertions at `NoteShow.spec.ts:144` and
  `AttachmentPage.spec.ts:58`.
- Test names saying picture: nested `class Picture` and
  `aPictureFileIsMarkedAsAPicture` in `NotebookAttachmentControllerTest`
  (`:142`, `:84`), methods in `NoteControllerUploadNoteImageTests`,
  `choosePicture` in `NoteShowPage.imageUpload.spec.ts:47`. Also:
  `servesAPictureFileInline…` and `nonReaderGetsNoPageDownloadOrPicture`
  (`NotebookAttachmentControllerTest`), `servesAPictureInTheNotesFolder…`
  (`NoteAttachmentImageControllerTest:50`), the move test's picture names
  (including the new `aRootPictureAnotherRootNoteUsesIsCopiedNotMoved`),
  `rawHistoryStaysReadableWhileANewPictureIsPublished`
  (`NotebookGitAttachmentRawHistoryControllerTest`),
  `uploadingANotePictureAddsItsFile…` (`NotebookGitDerivedTreeOracleControllerTest`),
  a `picture` variable in `NotebookGitWebAttachmentDeleteControllerTest`, and
  test titles in `NoteShow.spec.ts:140` and `AttachmentPage.spec.ts`.
- Kept: historical Flyway migrations (`picture_url`,
  `V300000347__drop_book_and_picture_bytes`), fixture data such as a
  "Pictures" folder or "picture.bin", and the recall picture quiz.
- Docs and plans: `docs/notebook-git-attachments.md`,
  `docs/note-content-saving.md:62,91`, `.planning/NORTH-STAR.md`
  (`:38-39`, `:56`, `:99`). E2E page object `expectPicture`
  (`e2e_test/start/pageObjects/attachmentPage.ts:59`, used at
  `notebook_files.ts:72`). E2E: `note_frontmatter_image.feature`,
  `notebook_files.feature`, `cli_notebook_lfs.feature`, step definitions
  `notebook_files.ts`, `note_editing.ts`.
- ADR 0001 Attachment entry at `docs/adrs/0001-ubiquitous-language.md:23-26`
  has no Short UI line; other entries use "Short UI: **…**".

## Outside-in proof

| Seed example | Slice |
| --- | --- |
| 1. file page shows `diagram.png` through the attachment's image endpoint | 2 |
| 2. note with `image: moon.jpg` loads through the note's image endpoint | 2 |
| 3. uploading `a.txt` refused "…: an image must be a png, …" | 3 |
| UI "File" legitimate in the vocabulary; docs say image | 4 |

## Slices

### 1. Image rules and serving use the image name

Type: Structure
Status: done — proof (plus the renamed `NotebookGitAttachmentRawHistoryControllerTest`,
`NotebookGitDerivedTreeOracleControllerTest`, `NotebookGitWebAttachmentDeleteControllerTest`)
61 tests green; refactor pass made no edits
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests 'com.odde.donut.controllers.NoteAttachmentImageControllerTest' --tests 'com.odde.donut.controllers.NotebookAttachmentControllerTest' --tests 'com.odde.donut.controllers.NotebookGitWebNoteMoveImageControllerTest' --tests 'com.odde.donut.controllers.NoteControllerUploadNoteImageTests'`
stays green.

Change: `PictureFile` → `ImageFile`, `InlinePicture` → `InlineImage`,
`MovedNotePicture` → `MovedNoteImage`, `placeWithPicture` → `placeWithImage`,
and the move test class `NotebookGitWebNoteMovePictureControllerTest` →
`NotebookGitWebNoteMoveImageControllerTest`; picture-named backend tests,
variables and javadoc listed in the starting facts follow the rename.

### 2. One image endpoint family

Type: Structure (API names change; product behavior does not)
Status: done — backend focused tests, `generateTypeScript`, frontend 18 tests
(`AttachmentPage.spec.ts`, `NoteShow.spec.ts`, `tests/pages/NoteShowPage.imageUpload.spec.ts`)
and `pnpm -C frontend exec vue-tsc --noEmit` green; refactor pass made no edits
Proof: the backend tests above, then
`CURSOR_DEV=true nix develop -c pnpm generateTypeScript` and
`CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/AttachmentPage.spec.ts tests/notes/NoteShow.spec.ts`
plus the frontend typecheck.

Change: rename both controller methods before generating —
`GET /api/notes/{note}/image?path=` as `showNoteImage` (controller renamed
`NoteImageController`) and `GET /api/notebooks/{n}/attachments/{a}/image` as
`showAttachmentImage`; the realm field `picture` becomes `image`; OpenAPI
summaries and descriptions say image; `ShowImage.vue` switches to
`ShowNoteImageData`; `AttachmentPage.vue`'s `pictureSrc` and
`attachment-picture` follow; update the URL assertions, `choosePicture` and the frontend test
titles.

### 3. Users read "image" in refusals

Type: Behavior (message wording)
Status: planned
Proof: first tighten the tests to the new wording (fail):
`NoteControllerUploadNoteImageTests:181` expects
`"Cannot upload " + name + ": an image must be a png, jpg, jpeg, gif or webp file."`,
its name-clash case at `:93` also expects "rename the image and upload it
again", and the svg case in `NoteImageControllerTest:83` expects
"Not a raster image."; then change the messages (pass) —
`CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests 'com.odde.donut.controllers.NoteControllerUploadNoteImageTests' --tests 'com.odde.donut.controllers.NoteImageControllerTest'`.

Behavior: uploading `a.txt` → "Cannot upload a.txt: an image must be a png,
jpg, jpeg, gif or webp file."; a taken name → "… rename the image and upload
it again"; a non-raster file → "Not a raster image."

Change: the three messages.

### 4. Docs and the vocabulary say "image" and "File"

Type: Structure (docs)
Status: planned
Proof: `grep -rni "picture" docs/*.md .planning/NORTH-STAR.md` finds only
sentences about pictures in general, none naming the concept; ADR 0001's
Attachment entry ends with "Short UI: **File**."

Change: "picture" → "image" in `docs/notebook-git-attachments.md`,
`docs/note-content-saving.md` and `.planning/NORTH-STAR.md`; add "Short UI:
**File**." to ADR 0001's Attachment entry (owner decision 2026-09-27).

### 5. E2E phrases say image

Type: Structure
Status: planned
Proof: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_view/note_frontmatter_image.feature,e2e_test/features/notebooks/notebook_files.feature,e2e_test/features/cli/cli_notebook_lfs.feature`
stays green (E2E boot makes this leaf longer than 5 minutes; stated reason).

Change: rename "picture" phrases in the three features, in
`notebook_files.ts` / `note_editing.ts`, and the page object's
`expectPicture`.

## Current decisions

- Attachment stays the code, API and schema name; File is its UI word.
- The note's image (by authored path) and an attachment's image (by id) stay
  two resources with matching names.

## Learnings

- Replacing `Picture` inside identifiers breaks articles ("aImageFile"); rename
  "a Picture" to "an Image" deliberately in later slices.
- Slice 2 renamed `NoteAttachmentImageController(Test)` to `NoteImageController(Test)`;
  the starting facts keep the old names as they were found.
