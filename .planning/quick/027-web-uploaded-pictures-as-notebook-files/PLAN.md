# Web-uploaded pictures become notebook files

## Source

- Identity: SEED-035#story-4.
- Source: [refined story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-4)
  (refined 2026-09-24, owner accepted).
- Governing direction: North Star
  [one attachment content model](../../NORTH-STAR.md#one-attachment-content-model),
  "One way in": store the verified object first, then accept the pointer with
  the reference that uses it in one accepted change, through the existing
  accepted-change owner. A web upload keeps the user's filename and refuses
  when it is taken.
- **Start condition:** met. Story 20 was delivered (`dc09d60f0e`) with its
  test-support correction (`115b281976`): backend fixtures and the upload
  scenario's notebooks are LFS notebooks.

## Goal and scope

A picture uploaded with the note's `image` property becomes an LFS file in the
note's folder under its uploaded name, and `image:` names it, in one accepted
commit. A local checkout receives it on clone or pull. Uploads keep their
original bytes. A taken or non-plain filename is refused with a clear message.

Excluded: body images, other file types, SVG, automatic free names, reuse of an
identical file, deleting a replaced file (story 2), moving legacy pictures
(story 5), removing `/attachments/images` and the `image` table (story 18), any
raw-notebook write path (story 19 removes raw).

Assumptions (rechecked on main `115b281976`, 2026-09-24):
- The upload today is `NoteController.uploadNoteImage` → `NoteService.uploadNoteImage`
  → `utils/ImageBuilder` (resize via `algorithms/ImageUtils`, JDK ImageIO only,
  no build dependency) → `image` + `attachment_blob` rows. It returns
  `/attachments/images/{id}/{name}`. The frontend
  (`RichFrontmatterImagePropertyValue.vue`) then sets `image:` to the returned
  path and saves the whole content. The content save has no version check.
- Keep that frontend flow. The server returns the filename it wrote into
  `image:`, so the follow-up save normally changes nothing (no second commit),
  and unsaved editor text still saves. No frontend code change is expected.
- A web commit is derived by `NotebookGitTreeEncoder.derive`.
  `NotebookGitChangedFiles.currentNoteEntries` puts only notes, so an inserted
  attachment row would be missing from the commit today.
- Reuse: `WebNoteEditService.edit` (accepted change), `NoteFolderAttachment.at`
  (the file a note-relative path names — also the clash check),
  `NotebookAttachmentContent.storeAsLfsPointer` (stores the verified bytes and
  returns the pointer; used by `NotebookGitLfsConversionService` and the LFS test
  seeding), `AuthoredNoteDocumentPersistence.persist` for the new content.
  `NoteContentMarkdown.mergeNoteImageScalarsIntoContent` exists (testability
  only); use it or `Frontmatter.set`, keeping `image_mask:` as it was.
- API errors carrying a `message` already show as a toast
  (`managedApi/clientSetup.ts`), so the refusal needs no frontend change.

## Outside-in proof (key examples)

| Promise (seed example) | Slice | Observable proof |
| --- | --- | --- |
| Folder note upload reaches a checkout (1) | 4 | E2E `cli_notebook_lfs.feature` (its Background has the token): upload `moon.jpg` on the web to a note in folder `physics`, CLI clone has `physics/moon.jpg` equal to the fixture and the note file containing `image: moon.jpg` |
| Web shows the uploaded picture (1) | 3 | E2E `note_frontmatter_image.feature` upload scenario: after reload the note shows its picture and the source contains `image: moon.jpg` |
| One accepted commit with file + reference, no `image` row, derived tree complete (1) | 3 | `NoteControllerUploadNoteImageTests`: one new commit whose tip has the pointer and the note's `image:`; content store holds the bytes; no `image` row; `NotebookGitDerivedTreeOracleControllerTest` upload case |
| Root note (2) | 3 | controller test: file at root, `image: <name>` |
| `image_mask:` unchanged; legacy row stays when replaced (5) | 3 | controller test assertions |
| Non-LFS notebook fails loudly | 3 | controller test on `createLegacyRawNotebook` (removed with raw by story 19) |
| Name taken → refused, nothing changes (3); non-plain name refused | 5 | controller test: message names the path; accepted head, note and rows unchanged |
| Original bytes, no resize (4) | 1 | controller test: an image over 2000 px is served byte-identical |
| Unpublished local edit rebases as usual (6) | — | No new behaviour; existing pull/rebase coverage (`cli_notebook_lfs` same-checkout pull) |

## Slices

### 1. Uploaded pictures keep their original bytes
Type: Behavior
Status: planned
Proof: `NoteControllerUploadNoteImageTests` — uploading a PNG wider than 2000 px
stores exactly the uploaded bytes.

Behavior: an owner uploads a large picture → it is kept byte-for-byte. Delete
`algorithms/ImageUtils`, `ImageUtilsTest`, and the resize call in
`utils/ImageBuilder` (the class itself goes in slice 3). No other caller exists.

### 2. Web commits carry the files a web change adds
Type: Structure
Status: planned
Proof: `NotebookGitTreeEncoderTest` — deriving a change with an inserted
attachment row puts its path with the row's accepted content; existing derived
tree oracle tests stay green.

Internal change: `NotebookGitChangedFiles` gives the current file of every
inserted or updated note **and attachment** (one rule, replacing
`currentNoteEntries`). The attachment's accepted content (the LFS pointer) is
the blob. Unchanged external behaviour: no web operation inserts an attachment
yet. Enables slice 3.

### 3. A web-uploaded picture is a file in the note's folder
Type: Behavior
Status: planned
Proof: `NoteControllerUploadNoteImageTests` and the
`NotebookGitDerivedTreeOracleControllerTest` upload case (table above); E2E
`note_frontmatter_image.feature` "Uploaded image sets attachment path on the
image property" now asserts the note shows its picture after reload and the
source contains `image: moon.jpg`.

Behavior: note in `physics` (or at the root) of an LFS notebook → the owner
uploads `moon.jpg` → one accepted commit adds `physics/moon.jpg` as an LFS
pointer and sets `image: moon.jpg` (mask unchanged); the bytes are in the
notebook's content store; no `image` row; the web shows the picture. A notebook
without an LFS binding fails loudly.

Cleanup in this slice: `NoteService.uploadNoteImage` and `utils/ImageBuilder`
go (the upload now lives beside the web note edit). Keep `Image`,
`AttachmentController` and orphan cleanup for legacy pictures (story 18).
Update the `NoteImageUploadResult` description and regenerate the API client.
Update `docs/notebook-git-attachments.md`: web uploads become folder files;
`/attachments/images/...` is legacy only.

Sizing note: the backend switch and the web scenario change together because
that scenario asserts the legacy path; splitting them would leave CI red.

### 4. The owner's checkout receives the web-uploaded picture
Type: Behavior
Status: planned
Proof: one new `cli_notebook_lfs.feature` scenario (its Background has the
token): upload `moon.jpg` on the web to a note in folder `physics`, clone with
the installed CLI → `physics/moon.jpg` equals the fixture's bytes and the note
file contains `image: moon.jpg`. Add one step comparing a cloned checkout file
with a fixture's bytes.

Behavior: the story's promise at the owner's boundary. Expected to need no
product change after slice 3 (clone fills every current attachment); kept
separate because it is a different proof loop (CLI build and checkout
harness), so a harness problem cannot hold back slice 3.

### 5. A taken or unusable filename is refused
Type: Behavior
Status: planned
Proof: `NoteControllerUploadNoteImageTests` — with `physics/diagram.png`
present, uploading `diagram.png` to a note in `physics` is refused with a
message naming `physics/diagram.png`; accepted head, note content, attachment
rows and content store are unchanged. Names `""`, `.`, `..`, `a/b.png`,
`.keep.png` are refused the same way.

Behavior: the check runs before the object is stored, using
`NoteFolderAttachment.at` for the clash. Client error with an `ApiError`
message, shown by the existing toast.

## Current decisions

- Refuse a taken name; never rename or overwrite (owner, 2026-09-24).
- No resize; original bytes (owner, 2026-09-24).
- LFS only; no raw write path.
- Keep the frontend's follow-up content save; the server's `image:` value makes
  it a no-change save.

## Learnings

None yet.
