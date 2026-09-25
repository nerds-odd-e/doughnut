# One commit per web picture upload

**Identity:** quick/030-one-commit-per-web-picture-upload/PLAN.md
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"0aff2a0bc468f98f968785c5350b097493ef066b5f8e2e89cdc24c207f0005a7"}}
```

## Source

- Kind: bounded retrospective correction; no seed.
- Corrects the execution of SEED-035#story-4 (story and plan recoverable at
  `87ec6e1f36:.planning/seeds/SEED-035-ai-workspace-supporting-files.md` and
  `87ec6e1f36:.planning/quick/027-web-uploaded-pictures-as-notebook-files/PLAN.md`;
  the delivered behavior is in `docs/notebook-git-attachments.md`). Reviewed commits on
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
| Upload response carries the prepared note | 1 | `NoteControllerUploadNoteImageTests`: uploaded content is prepared like any save (`type:` present for a note without one) and the response carries that note realm; still exactly one accepted commit |
| Upload is one commit end to end | 2 | Frontend `RichMarkdownEditor.propertyImage.spec.ts`: pending edits are saved before the upload request; after upload the editor shows the returned content and no content save is sent (mock returns a note realm with a filename `image:`, finding 4). Frontend typecheck. E2E `cli_notebook_lfs.feature` scenario keeps its exact text. |
| Name used by a note or folder is refused | 3 | `NoteControllerUploadNoteImageTests`: `force.md` beside note `force`, and `sub` beside folder `physics/sub`, refused naming the path, nothing changed |
| One rule sets `image:`; no redundant test | 4 | Backend tests seeding `imageUrl`/`imageMask` through `NotesTestData` and E2E `note_frontmatter_image.feature` stay green; `mergeNoteImageScalarsIntoContent`, its tests, the resize-era test and unused `UploadedImageBuilder.metrics` are gone |

## Slices

### 1. The upload response carries the prepared note
Type: Behavior
Status: done
Accepted proof: `NoteControllerUploadNoteImageTests.aRootNotesPictureIsAFileAtTheNotebookRootAndItsContentIsPreparedLikeAnySave`
(realm content and tip `Moon.md` are `type: Note` + `image:`; one new commit); the
legacy-mask test now expects `type: Note` first. The frontend still reads `image:`
from the returned realm and commits (slice 2 replaces this).
Proof: see table row 1.

Behavior: note without `type:` → owner uploads a picture → the one accepted commit
holds the file and content prepared like any save
(`AuthoredNoteContent.prepareDocumentForSave` on `withNoteImage`'s result), and the
response is the note's `NoteRealm` instead of `NoteImageUploadResult` (delete that DTO
if unused). Regenerate the API client; keep the frontend compiling by reading
`image:` from the returned realm's content only where `imagePath` was read (behavior
there changes in slice 2).

### 2. A web picture upload is one commit
Type: Behavior
Status: done
Accepted proof: `frontend/tests/pages/NoteShowPage.imageUpload.spec.ts` (request order
save → upload; after upload the image row shows `e2e.png` and no content save);
`cli_notebook_lfs.feature` and `note_frontmatter_image.feature` pass unchanged. The
editor-level spec cannot show save ordering, so upload proof lives at the note page.
Proof: see table row 2.

Behavior: a note open in the editor (with or without unsaved text) → the owner
uploads a picture → `closeAndFlushNoteContentMutations(noteId)`
(`composables/noteContentMutationBarrier.ts`, as `useNoteRemovalFlow` uses it) saves
pending text before the upload request, then reopens admission; the returned realm
goes through `NoteStorage.refreshNoteRealm`, whose content the autosave adopts via
`syncFromExternal` without saving. `RichFrontmatterImagePropertyValue` no longer
emits `update:modelValue` and `commit` after upload.

### 3. A name used by a note or folder in the folder is refused
Type: Behavior
Status: done
Accepted proof: `NoteControllerUploadNoteImageTests.aNameUsedByANoteInTheNotesFolderIsRefusedAndNothingChanges`
and `aNameUsedByAFolderInTheNotesFolderIsRefusedAndNothingChanges`; the check is
`NotebookGitAcceptedTree.hasPath` (an empty folder is not in the accepted tree, so
it does not block a name).
Proof: see table row 3.

Behavior: note `force` in `physics` → upload named `force.md` (or the name of a
subfolder) → refused like a taken file, nothing stored or accepted. Replace the
`NoteFolderAttachment.at` check in `requireFreePlainFilename` with one check of the
notebook's accepted tree: the note-relative path
(`NotebookGitPortablePath.folderPath(note.getFolder()) + filename`) is taken when it
is a key of `NotebookGitAcceptedTree.blobIds` at the accepted head or a folder
prefix of one (the `representedInTree` test).

### 4. One rule sets a note's `image:`
Type: Structure
Status: done
Accepted proof: `NoteContentMarkdownTest`, `NoteControllerUploadNoteImageTests`,
`com.odde.donut.testability.*`; E2E `note_frontmatter_image`, `assimilation_page_types`
and five features seeding frontmatter without an image stay green. Seeds now set
`image:` / `image_mask:` through `withNoteImage` / `withNoteImageMask` and are
otherwise stored as written.
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

- Refinement 2026-09-25: the flush seam (`closeAndFlushNoteContentMutations`), the
  adopt-without-save seam (`refreshNoteRealm` + `syncFromExternal`), and the path
  reader (`NotebookGitAcceptedTree.blobIds`) exist; old slice 1 split into a backend
  and a frontend proof loop.
