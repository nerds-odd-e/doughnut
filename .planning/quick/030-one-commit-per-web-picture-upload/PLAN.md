# One commit per web picture upload

**Identity:** quick/030-one-commit-per-web-picture-upload/PLAN.md
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"PLAN.md","assessment":"not-ready","reasons":["Slice 1 sizing: how the editor flushes pending text before an upload is unverified, and the slice spans backend, API client and frontend","Slice 2: reading whether a note-relative path exists in the notebook's current portable tree is an assumed seam; an existing reader is not yet identified"],"basis":{"document":"c86a452fd1bd87d2c45fc03dc42224ea545e4ef16922c8a00638934d662301f9"}}
```

## Source

- Kind: bounded retrospective correction; no seed.
- Corrects the execution of SEED-035#story-4
  ([story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-4),
  plan `quick/027-web-uploaded-pictures-as-notebook-files`). Reviewed commits on
  `main`: `4250de93e1` (no resize), `a061808c28` (web commits carry files),
  `682259779a` (upload becomes an LFS folder file), `0e760d4933` (CLI clone
  proof), `f4d1ec8d5c` (taken/non-plain name refusal). Retrospective 2026-09-24.
- Governing direction: North Star
  [one attachment content model](../../NORTH-STAR.md#one-attachment-content-model),
  "One way in": the stored object's pointer and the reference that uses it are
  accepted together in one change.

## Beneficiary and outcome

The notebook owner, who reads the notebook's history in a local checkout. After
this correction, one web picture upload is exactly one accepted commit (file and
`image:` together), and a name already used in the note's folder by anything —
file, note or folder — is refused.

## Findings (current truth at `f4d1ec8d5c`)

1. **Second commit after each upload.** `WebNoteImageUploadService.upload` writes
   `NoteContentMarkdown.withNoteImage(content)` without the ordinary save
   preparation (`AuthoredNoteContent.prepareDocumentForSave`, which adds a
   missing `type:` first). The frontend (`RichFrontmatterImagePropertyValue.vue`
   `onImageFileSelected`) then sets `image:` and emits `commit`; the editor
   rebuilds frontmatter from its rows and autosaves, so the content differs and
   `AcceptedWebChangeService` makes a second "Edit note content" commit.
   Evidence: the root-note controller test expects `---\nimage: my.png\n---\n…`
   (no `type`), while `cli_notebook_lfs.feature` "A picture uploaded on the web
   arrives in the owner's clone beside its note" expects `type: Note` — only the
   follow-up save can add it. The plan-027 decision "follow-up save is a no-change
   save" is disproved.
2. **Two rules set `image:`.** `NoteContentMarkdown.mergeNoteImageScalarsIntoContent`
   is now used only by `testability/model/NotesTestData.buildNote`, beside
   `withNoteImage` / `setLeadingFrontmatterProperty`.
3. **Taken check covers files only.** `requireFreePlainFilename` asks
   `NoteFolderAttachment.at`; a name equal to a note file (`force.md`) or a
   subfolder in the same folder is accepted and would put two entries on one path.
4. **Stale frontend proof.** `frontend/tests/components/form/RichMarkdownEditor.propertyImage.spec.ts`
   still mocks and expects `imagePath: "/attachments/images/99/e2e.png"`.
5. **Resize-era test.** `NoteControllerUploadNoteImageTests.shouldKeepTheOriginalBytesOfAPictureWiderThan2000Pixels`
   repeats the first upload test's exact stored-bytes check
   (`lfsPointerStoredFor`); only its serving path is extra, which the web E2E
   already shows after reload.

## Preserved promises and constraints

- Story 4 key examples 1–6 stay true: file under the uploaded name in the note's
  folder as an LFS pointer, `image:` names it, `image_mask:` unchanged, legacy row
  kept, original bytes, non-LFS notebook fails loudly, taken or non-plain name
  refused with a message naming the path and nothing changed.
- Unsaved editor text is not lost by an upload.
- Refuse, never rename or overwrite. LFS only.

Excluded: the plain-name rule of the Git publish check
(`NotebookGitProposalTreeInspection.assertPathIsSafe`); the three places that
build a `NotebookAttachment` row; image placeholder wording; legacy pictures
(stories 5, 18).

## Outside-in proof

| Promise | Slice | Observable proof |
| --- | --- | --- |
| Upload is one commit end to end | 1 | Frontend `RichMarkdownEditor.propertyImage.spec.ts`: after upload the editor shows the returned content and no content save is sent (mock expects a filename, finding 4); pending edits are saved before the upload request. Controller test: uploaded content is prepared like any save (`type:` present for a note without one). E2E `cli_notebook_lfs.feature` scenario keeps its exact text. |
| Name used by a note or folder is refused | 2 | `NoteControllerUploadNoteImageTests`: `force.md` beside note `force`, and `sub` beside folder `physics/sub`, refused naming the path, nothing changed |
| One rule sets `image:`; no redundant test | 3 | Backend tests seeding `imageUrl`/`imageMask` through `NotesTestData` and E2E `note_frontmatter_image.feature` stay green; `mergeNoteImageScalarsIntoContent`, its tests, the resize-era test and unused `UploadedImageBuilder.metrics` are gone |

## Slices

### 1. A web picture upload is one commit
Type: Behavior
Status: planned
Proof: see table row 1. Frontend proof also runs the frontend typecheck
(`frontend` skill "Frontend proof").

Behavior: a note open in the editor (with or without unsaved text) → the owner
uploads a picture → pending text is saved first, the upload accepts file and
`image:` in one commit with the ordinary save preparation, the response carries
the note's resulting content, and the editor adopts it without another save.
Choose the smallest response change (for example the note's content beside
`imagePath`, or the existing note realm) and regenerate the API client.

### 2. A name used by a note or folder in the folder is refused
Type: Behavior
Status: planned
Proof: see table row 2.

Behavior: note `force` in `physics` → upload named `force.md` (or the name of a
subfolder) → refused like a taken file, nothing stored or accepted. Prefer one
check of whether the note-relative path exists in the notebook's current
portable tree over adding note and folder lookups beside `NoteFolderAttachment.at`.

### 3. One rule sets a note's `image:`
Type: Structure
Status: planned
Proof: see table row 3.

Internal change: `NotesTestData.buildNote` sets `image` and `image_mask` through
`setLeadingFrontmatterProperty`; delete `mergeNoteImageScalarsIntoContent` and its
`NoteContentMarkdownTest` cases; delete the resize-era upload test and
`UploadedImageBuilder.metrics` if unused. Removes a duplicated domain rule and a
redundant test (retrospective correction; no new behavior).

## Current decisions

- The upload response, not a follow-up save, brings the editor up to date
  (retrospective, 2026-09-24; replaces plan 027's follow-up-save decision).

## Learnings

None yet.
