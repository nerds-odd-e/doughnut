# Picture upload explains refusals and shows the new file

## Source

- Story: [SEED-046#story-4](../../seeds/SEED-046-notebook-files-and-git-findings.md#story-4)
- **Identity:** SEED-046#story-4

## Goal and scope

A request refused by field validation carries the field's message as its
message, so the web toast shows the reason instead of "binding error"
(app-wide). Picture upload admits a file by its extension from the one list
picture display uses, ignoring the declared content type, and refuses any
other name with a message naming the allowed types. An accepted upload appears
in the sidebar without a reload.

Excluded (seed): checking the bytes, SVG/HEIC, restricting the file picker, a
browser-side size check, the 100 MB request limit, `image:` values from local
publish, Book upload. Preserved: the 10 MiB inclusive limit and its message,
the single accepted change, the kept previous picture, name-clash and dot-name
refusals.

## Outside-in proof

| Key example | Proof |
| --- | --- |
| 1 over-limit PNG → limit message, note unchanged | slice 2, `NoteControllerUploadNoteImageTests` |
| 2 `drawing.svg` → names png, jpg, jpeg, gif, webp | slice 2, same |
| 3 PNG bytes named `notes.txt` → refused, `image:` unchanged | slice 2, same |
| 4 `photo.png` sent as `application/octet-stream` → accepted | slice 2, same |
| 5 `Blue.PNG` → sidebar lists it without reload | slice 3, `frontend/tests/store/storedApi.spec.ts` |
| 6 a form field failing request validation → toast shows its message | slice 1, `CustomRestExceptionHandler` test |
| 7 exact-limit PNG accepted; SVG bytes named `fake.png` accepted | slice 2, same upload tests; display unchanged in `NoteAttachmentImageControllerTest` |

Commands:
`CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests '*CustomRestExceptionHandler*' --tests '*NoteControllerUploadNoteImageTests' --tests '*NoteAttachmentImageControllerTest'`;
`CURSOR_DEV=true nix develop -c pnpm frontend:test storedApi.spec`.

## Slices

### 1. A request refused by field validation says why

Type: Behavior
Status: planned
Proof: a test beside `CustomRestExceptionHandlerDuplicateTitleTest` feeds a
`BindException` with one field error (and a `MethodArgumentNotValidException`
with two) to the handler → 400, `BINDING_ERROR`, the message is the field
message (several joined with "; "), and the per-field `errors` stay as today.

Behavior: any `@Valid` refusal (for example today's picture DTO type or size
check) → the response message is the field message, which the existing toast
shows. Forms that attach field errors keep them.

Change: in `CustomRestExceptionHandler`, `handleBindException` and
`handleMethodArgumentNotValid` build one `ApiError` through one shared private
method whose message is the collected field and global messages, replacing the
literal "binding error". No frontend change.

### 2. Upload admits a picture by its name, from the list display uses

Type: Behavior
Status: planned
Proof: in `NoteControllerUploadNoteImageTests`, through the controller `upload`
helper: `notes.txt` (PNG bytes, `image/png`) and `drawing.svg` are refused with
an `ApiException` whose message names png, jpg, jpeg, gif and webp, and nothing
is stored or committed (as `assertUploadRefusedWithNothingChanged` checks);
`photo.png` sent as `application/octet-stream` is accepted; a
10,485,761-byte PNG is refused with the limit message and nothing changes; a
10,485,760-byte PNG is accepted. The three bean-validation DTO tests are
replaced by these. `NoteAttachmentImageControllerTest` stays green unchanged.

Behavior: the examples above; display still serves png, jpg, jpeg, gif and
webp by extension, ignoring case, and answers 415 otherwise.

Change: one small owner of picture file names (for example `PictureFile` in
`services/notebookAttachment`) holds the extension → media
type map, the 10 MiB limit, and the admission check with its two messages.
`NoteAttachmentImageController.rasterType` uses it; `WebNoteImageUploadService`
calls the admission before storing bytes, beside `requireFreePlainFilename`.
`NoteImageUploadDTO` keeps only `@NotNull`; `MultipartFileValidator` stays for
audio. The `produces` list stays as the endpoint's HTTP declaration. Regenerate
the API client if the DTO schema changes.

### 3. An accepted upload appears in the sidebar at once

Type: Behavior
Status: planned
Proof: `frontend/tests/store/storedApi.spec.ts` — `uploadNoteImage` with a
mocked `uploadNoteImage` response advances `sidebarStructuralRefreshKey` by
one, like the existing move tests; a refused upload (no realm) does not.

Behavior: uploading `Blue.PNG` from the note page → the sidebar lists it
without a reload.

Change: `StoredApiCollection.uploadNoteImage` calls
`refreshSidebarStructuralListings()` after refreshing the realm.

## Current decisions

- Owner, 2026-09-26: app-wide message fix; admission by extension, ignoring the
  declared type; the file picker stays `image/*`.
- Planning, 2026-09-26: picture admission moves from the shared multipart
  annotation into the picture owner because annotation values cannot share a
  runtime list, and the annotation matches declared content types, which this
  story stops trusting. With the admission as an `ApiException`, upload
  refusals no longer depend on slice 1; slice 1 still serves every other form.
- Planning, 2026-09-26: the web 10 MiB constant is not merged with local
  publish's `NotebookGitAttachmentSizeAdmission.LIMIT_BYTES` here, because the
  Taken story SEED-046#story-1 is reworking that class; merging is a candidate
  follow-up once it lands.
