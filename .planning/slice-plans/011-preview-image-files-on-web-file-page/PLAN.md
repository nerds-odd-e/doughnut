# Preview image files on their web file page

## Source

- Story: [SEED-035#story-26](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-26)
- **Identity:** SEED-035#story-26

## Goal and scope

A notebook reader opening an image file's page sees the picture below the
name and size, scaled down to fit and never enlarged. "Image" is the existing
picture rule (`PictureFile`: png, jpg, jpeg, gif, webp by extension, ignoring
case). Every other type, SVG included, is never served inline (script risk;
owner decision 2026-09-27). Download and Delete are unchanged.

Excluded: sidebar or folder thumbnails, zoom, dimensions, which notes use the
picture, other previews, caching headers, and images referenced from note
body text.

## Outside-in proof

| Key example | Proof |
| --- | --- |
| 1 `diagrams/flow.png` → the picture shows; Download and Delete remain | slice 1 (inline bytes), slice 2 (page and E2E) |
| 2 `photo.JPG` in a notebook the reader cannot edit → the picture shows, no Delete | slice 1 (upper-case extension, read rule), slice 2 (page spec with `readonly`) |
| 3 `notes.pdf` or `logo.svg` → the page is unchanged | slice 1 (refused inline), slice 2 (page spec) |

Commands:

- `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test -Dspring.profiles.active=test --tests '*NotebookAttachmentController*' --tests '*NoteAttachmentImageController*'`
- `CURSOR_DEV=true nix develop -c pnpm frontend:test AttachmentPage`
- `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/notebooks/notebook_files.feature`

## Architecture

PFE: the solution already exists for a note's `image:` —
`NoteAttachmentImageController` serves an attachment inline through
`PictureFile.mediaType` and `NotebookAttachmentFile.bytes`. Reuse it; do not
add a second picture rule or byte reader (North Star, "One way out"). The file
page's `/content` download stays a forced `application/octet-stream`
download.

## Slices

### 1. A notebook file is served inline as a picture by the file itself

Type: Behavior
Status: planned
Proof: new tests in `NotebookAttachmentControllerTest`, run red first: a root
`photo.JPG` → `inline` disposition, `image/jpeg`, `nosniff`, exact bytes; a
`logo.svg` → 415 `UNSUPPORTED_MEDIA_TYPE`; a notebook the user cannot read →
`UnexpectedNoAccessRightException`. `NoteAttachmentImageControllerTest` stays
green.

Behavior: a reader of the notebook → `GET
/api/notebooks/{notebook}/attachments/{attachment}/picture` → the file's
bytes inline with the picture's media type when the filename is a picture,
otherwise 415.

Change: add the endpoint beside `downloadAttachment` (same read check and
`requireInNotebook`). Move the inline-picture response (media type from
`PictureFile`, 415 otherwise, inline disposition, `nosniff`, bytes) into one
place both this endpoint and `NoteAttachmentImageController` use, so the two
cannot drift. Regenerate the API client (generate-api-client).

### 2. The file page shows an image file's picture

Type: Behavior
Status: planned
Proof: `AttachmentPage.spec.ts` — a realm with `picture: true` renders an
`<img>` whose path is the slice 1 address and alt is the filename, with
Download (and Delete when not readonly) still present; `picture: false` renders
no `<img>`. `NotebookAttachmentControllerTest` file-page tests assert
`picture` is true for `flow.png` and false for `run.json`. E2E: in the existing
"Delete a file from its page" scenario, after opening `sketch.png`, a new step
"the file page shows its picture" requests the `<img>` source and expects
`image/png`; no new scenario.

Behavior: an image file's page → loaded → the picture shows below the size,
`max-width: 100%` and `height: auto`; a non-image file's page looks as today.

Change: `NotebookAttachmentRealm` gains `@NotNull boolean picture` from
`PictureFile.mediaType(filename).isPresent()`, so the frontend never repeats
the extension rule. Regenerate the API client; `AttachmentPage.vue` renders the
`<img>` when `picture` is true.

## Current decisions

- 2026-09-27: whether a file is a picture is decided only by the backend's
  `PictureFile`; the page follows the realm's `picture` flag.
- Plan 010 (SEED-046#story-9) also changes `NotebookAttachmentRealm`'s folder
  trail; whichever lands second rebases onto the other. No ordering
  dependency.
