# Keep a Book's source file as an ordinary notebook file

## Source

- Identity: SEED-035#story-17.
- Source: [refined story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-17)
  (refined 2026-09-25; owner decisions: priority kept, Book refers to its file
  by path, removing a Book leaves the file, local changes to a Book's file are
  refused, three or four production Books).
- Governing direction: North Star
  [one attachment content model](../../NORTH-STAR.md#one-attachment-content-model):
  "One way in" (store the verified object first, then accept the pointer with
  the reference that uses it in one accepted change; Donut-chosen names take a
  free name), the Book size exception, and "Moving and retiring" (resumable
  per-notebook move, old copy kept until the separate Book storage is removed).
  The Book referring to a path follows "A Book is private reading structure
  over one source Attachment": the Attachment is the file at that path.
- **Start condition:** the picture move
  ([033](../033-move-legacy-note-pictures/PLAN.md), SEED-035#story-5) is on
  main. This plan reuses what it adds: public `NumberedNameSelection` with
  `firstAvailableFilename` (numbers before the extension),
  `NotebookGitAcceptedTree.takenPaths`, `NotebookGitPortablePath.isPlainFilename`,
  the `LegacyNotePictureMoveOnStartup` startup shape (after Flyway), and the
  CLI E2E step comparing a cloned file with a fixture. Recheck these names on
  main before slice 1.

## Goal and scope

Attaching a Book (CLI PDF or web EPUB) puts its source file at the notebook
root as an LFS file, in the same accepted change as the Book, and the Book
reads from that file by path. The three or four existing Books move the same
way once, at startup. Removing a Book leaves the file. A local publish that
deletes, renames or changes a Book's file is refused.

Excluded (seed): following a local rename, replacing a Book in place, making
an existing notebook PDF into a Book, CLI `/attach` reusing a checkout file,
PDF attach on the web, streaming or range requests, history rewrite, removing
the separate Book storage or its code (story 21), any CLI or frontend change.

Assumptions (checked on origin/main `4ee86d987a`, 2026-09-25, plus the
unmerged picture-move branch):

- `POST /api/notebooks/{notebook}/attach-book`
  (`NotebookBooksController.attachBook`, `@Transactional`) calls
  `BookService.attachBook`, which writes `bookStorage.put` inside the DB
  transaction and persists through `BookFormat.persistNewBook` /
  `AttachBookPersistence`. EPUB bytes are read only at attach time
  (`EpubStructureExtractor`).
- `WebNoteImageUploadService.upload` is the "one way in" precedent:
  `NotebookAttachmentContent.storeAsLfsPointer` first, then
  `AcceptedWebChangeService.apply` (SERIALIZABLE, no current user needed,
  Donut System author, no commit when the notebook has no Git binding) with a
  mutation that saves a `NotebookAttachment`. A null `folder` is the notebook
  root. The web path has no 10 MiB check; the attach multipart limit is 100MB
  (`application.yml`), so the Book exception needs no new code.
  Check during slice 2 whether the attach controller's own `@Transactional`
  must go so `apply` owns the transaction, as in the picture upload.
- Reading: `NotebookBookFile.fromBook(book, bookStorage)` (etag = md5 of
  `sourceFileRef`) serves both `GET /api/notebooks/{nb}/book/file` and
  `GET /api/books/{book}/file`. `NotebookAttachmentFile.bytes(attachment)`
  reads an attachment; `NotebookAttachmentRepository.findPlacementsByNotebookIdAndFilename`
  finds rows by filename.
- `BookService.deleteBookForNotebook` deletes reading positions, the Book, and
  `bookStorage.delete(ref)`.
- Publish: `NotebookGitProposalPublisher.publish` already obtains
  `NotebookGitProposalTreeShape.inspectRegularFiles` → `InspectedRegularFile(path,
  acceptedBlobId, proposedBlobId)` before acceptance; refusals are
  `ResponseStatusException` with a path-naming message (for example the
  oversized refusal). `projectAttachments` would otherwise delete or update the
  row to match the tip.
- `book.source_file_ref` is NOT NULL; the next migration is
  `V300000344__…` (latest `V300000343`).
- The CLI reads only `bookName` and `blocks` from the attach response; keeping
  the new column out of the JSON views means no API regeneration.

PFE: reuse the picture upload's "store then apply" sequence,
`AcceptedWebChangeService`, `NotebookAttachment` at root,
`NotebookAttachmentFile`, the picture move's naming helpers and startup shape,
and the publisher's inspected-file list. Gap: a Book referring to an
attachment path, and a publish rule that protects it.

## Outside-in proof (key examples)

| Promise (seed example) | Slice | Observable proof |
| --- | --- | --- |
| A Book naming a notebook file reads that file; Books without one still read from the old store (safe stopping point) | 1 | `NotebookBooksBookFileControllerTest`: Book with a source path at a root attachment → `GET …/book/file` returns the attachment's bytes; existing `returnsPdfWhenSourceFileRefPointsAtBlob` and EPUB tests stay green |
| Attach puts `<book name>.pdf` at root in one accepted change with the Book; reading unchanged; file listed at root (1) | 2 | `NotebookBooksAttachControllerTest`: one new accepted commit; tip has `Physics Primer.pdf` as a pointer whose stored object has the uploaded bytes; a root `NotebookAttachment` of that name; `…/book/file` returns the bytes; `assertAcceptedTreeMatchesTheFullAssembly`; EPUB attach gives `<name>.epub` |
| A file over 10 MiB is accepted through attach (3) | 2 | same class: an 11 MiB PDF attaches and reads back |
| Taken or non-plain name → next free name (2) | 3 | same class: root holding `Physics Primer.pdf` → `Physics Primer (2).pdf`, existing file untouched; book name `a/b` → Donut-chosen plain name |
| Removing a Book leaves its file (5) | 4 | `NotebookBooksBookFileControllerTest` delete test: after `DELETE …/book`, no Book, reading data gone, root `Physics Primer.pdf` row and tip entry remain, no new commit |
| Local delete, rename or change of a Book's file is refused (6); 10 MiB still applies to local publish (3) | 5 | publisher/controller test beside the oversized-refusal tests: proposal deleting, renaming, or changing `Physics Primer.pdf` → refused naming it, accepted head unchanged; an unrelated change publishes; the existing oversized-refusal test covers the local 10 MiB limit |
| Existing Books move once at startup, layout and reading data kept; rerun adds nothing; a failing notebook doesn't stop others (4) | 6 | move test (controller-level, like `LegacyNotePictureMoveControllerTest`): legacy Book → one Donut System commit adding the root file with the stored bytes, source path set, blocks/reading record/last-read position and `updatedAt` unchanged, old store still holds the bytes; second run adds no commit; startup listener method called directly: healthy notebook moved, broken one unchanged |
| Pull gives the file (1) | 7 | E2E `cli_notebook_lfs.feature`: attach a PDF fixture to a notebook, CLI clone has `<name>.pdf` equal to the fixture; existing `book_reading/*.feature` stay green |

## Slices

### 1. A Book can read from a file in its notebook
Type: Structure
Status: done
Proof: slice 1 row above. Accepted: `pnpm backend:test:worktree --tests '*Book*'`
(134 pass); `NotebookBooksBookFileControllerTest.GetBookFile.returnsTheNotebookFileWhenSourcePathNamesARootAttachment`
stores different bytes in the old store and the root attachment and reads the
attachment's.

Change: migration `V300000344__book_source_file_path.sql` adds nullable
`book.source_file_path` and makes `source_file_ref` nullable. `Book` gets
`sourceFilePath` (not in JSON views). `NotebookBookFile` reads the root
attachment at that path through `NotebookAttachmentFile` when set (etag from
the attachment's pointer content), otherwise the old store. Enables slice 2.

### 2. Attaching a Book adds its file at the notebook root
Type: Behavior
Status: done
Proof: slice 2 rows above. Accepted: `--tests '*Book*'` (137 pass) and
`--tests '*NotebookGit*'` (423 pass); observations in
`NotebookBooksAttachNotebookFileControllerTest` (Git-backed base: one commit,
tip pointer, root row, path set and ref null, bytes read back, full-assembly
match; EPUB `.epub`; 11 MiB PDF).

Behavior: notebook without a Book → attach a PDF or EPUB → one accepted
change holds the root LFS file `<book name>.<format>` and the Book, whose
`sourceFilePath` names it; reading returns the same bytes; nothing is written
to the old Book store. Store the object with `storeAsLfsPointer` before
`AcceptedWebChangeService.apply`; the operation saves the root attachment and
persists the Book. Validation (format, one Book per notebook, EPUB checks)
still refuses before anything is stored.

### 3. A Book's file takes a free, plain name
Type: Behavior
Status: done
Proof: slice 3 row above. Accepted: `--tests '*Book*'` (139 pass),
`'*NotebookGit*'`, `'*LegacyNotePictureMove*'`, `'*UploadNoteImage*'`;
`NotebookBooksAttachNotebookFileControllerTest.aTakenNameIsNumberedAndTheExistingFileIsUntouched`
and `aBookNameThatIsNotAPlainFilenameGetsADonutChosenName`.

Behavior: root name taken or book name not a plain filename → attach →
`firstAvailableFilename` over `takenPaths` gives the name; a non-plain book
name falls back to `book.<format>` before numbering.

### 4. Removing a Book leaves its file
Type: Behavior
Status: done
Proof: slice 4 row above. Accepted: `--tests '*Book*'` (136 pass),
`'*NotebookBooks*ControllerTest'` (98 pass);
`NotebookBooksAttachNotebookFileControllerTest.removingTheBookLeavesItsFileInTheNotebook`.
`BookStorage.delete` is gone (no other caller).

Behavior: Book with a notebook file → `DELETE …/book` → Book and reading data
removed, file and tree unchanged. `deleteBookForNotebook` stops deleting from
the old store (unmoved bytes are dropped with that store in story 21).

### 5. A local publish cannot remove or change a Book's file
Type: Behavior
Status: done
Proof: slice 5 row above. Accepted: `--tests '*NotebookGitBookSourceFileProtection*'`
(2 pass, red-checked), `'*NotebookGit*'` (425 pass, includes the oversized
refusals), `'*Book*'` (137 pass). Rule: `NotebookGitBookSourceFileProtection.refuseChanging`,
called from `NotebookGitProposalPublisher.publish` after size admission.

Behavior: notebook whose Book names `Physics Primer.pdf` → publish a proposal
whose inspected files show that path removed or with a different blob →
`ResponseStatusException` (CONFLICT): "\"Physics Primer.pdf\" is the source
file of the Book \"Physics Primer\". Remove the Book on the web before
deleting, renaming or changing it." Nothing is accepted. A rename is a removal
of the old path, so it needs no separate rule.

### 6. Existing Books move into their notebooks at startup
Type: Behavior
Status: done
Proof: slice 6 row above. Accepted: `--tests '*LegacyBookSourceFileMove*'`
(2 pass), `'*Book*'` (139 pass); `LegacyBookSourceFileMoveControllerTest`.
Code: `services/book/LegacyBookSourceFileMove`, `configs/LegacyBookSourceFileMoveOnStartup`
(story 21 deletes both with their test).

Behavior: Book without `sourceFilePath` → the move runs → bytes from the old
store are stored as an LFS object, then one `apply` per notebook adds the root
file (slice 3 naming) and sets the path; `updatedAt`, blocks and reading data
unchanged; the old copy stays. Runs from an `ApplicationReadyEvent` listener
in the picture move's shape, one notebook per try/catch with `logger.error`;
reruns select only Books still without a path. Separate class from the
picture move so story 21 can delete it on its own.

### 7. A cloned notebook has its Book's file
Type: Behavior
Status: done
Proof: slice 7 row above. Accepted: `pnpm cy:run --spec` over
`cli/cli_notebook_lfs.feature` and `book_reading/{ai_reorganize_layout,book_browsing,reading_record,reorganize_layout}.feature`
(33/33); scenario "A Book attached to a notebook arrives in the owner's clone
as its source file". `epub_book.feature` is not runnable in an isolated
worktree and does not use the changed steps; CI covers it.

Behavior: notebook with an attached PDF Book → `donut notebook clone` → the
checkout has the Book's file with the fixture's bytes. Reuses the testability
attach-book call and the picture move's fixture comparison step.

## Current decisions

- Book → file link is a root-relative path column, not a foreign key to the
  attachment row: publish projection deletes and recreates rows, and the owner
  chose a path.
- One operation places a Book's file: given the Book and verified pointer, it
  chooses the free name (slice 3), saves the root attachment and sets the
  path inside the caller's `apply`. Attach (slice 2) and the move (slice 6)
  both use it; neither has its own naming or placement rule.
- The old store and `source_file_ref` stay, read only for Books not yet moved,
  until story 21.

## Learnings

- `BookSourceFile.read(book)` owns where a Book's bytes come from (root
  attachment by `sourceFilePath`, else the old store); `NotebookBookFile` only
  shapes bytes. Root lookup is
  `NotebookAttachmentRepository.findByNotebook_IdAndFolderIsNullAndFilename`.
- `backend:test:worktree` takes one `--tests` pattern; `'*Book*'` covers the
  Book controller and service tests.
- Attach lives in `AttachBookService.attach` (store first, then `apply` that
  persists the Book and calls `BookSourceFilePlacement.place`, both in
  `services/book`). The file extension comes from `BookFormat.bookFileExtension()`.
  Naming is `notebookGit/NotebookRootFreeFilename.choose(notebookId, preferred,
  fallback)` (public; the helpers stay package-private). A notebook without a
  Git binding has no taken names. Occupancy of the accepted tree is
  `NotebookGitAcceptedRepositoryStore.takenPaths(binding)`, shared with the
  picture upload and the picture move.
- Git-backed proof helpers (`createGitBackedNotebook`, `acceptedHistory`,
  `lfsPointerStoredFor`, `assertAcceptedTreeMatchesTheFullAssembly`) live in
  `NotebookGitWebContentControllerTestBase`; `CommittedUserCleanup` now clears
  `book_content_block` rows for committed EPUB Books.
- Slice 2 took about 8 minutes: the Git-backed test base and the committed
  cleanup cost more than planned.
- The E2E Book attach and OpenAI layout steps now take the real notebook
  name (`testabilityBook.ts`, `getNotebookIdByName`) instead of a note title.
