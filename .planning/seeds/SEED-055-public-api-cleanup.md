---
id: SEED-055
status: dormant
planted: 2026-09-29
planted_during: owner request to review and clean up the current backend public API
trigger_when: deciding whether to consolidate the file-serving endpoints
scope: small
---

# SEED-055: Keep the public API aligned with supported product features

## Why This Matters

The generated API summary is the default endpoint lookup for maintainers and
their coding agents, so every duplicated endpoint is a wrong option an agent can
pick and build on. The attachment, Book, and LFS migration left overlapping
endpoints that serve notebook file bytes. This seed holds an evidence-backed
proposal for them; implementing it is a product decision that has not been made.

## File-serving proposal

Four read-only GET endpoints serve notebook file bytes. All four read through
`NotebookAttachmentFile.bytes` (`services/notebookAttachment/NotebookAttachmentFile.java:26-37`),
which returns the LFS store bytes, or the content stored in Git when the file has no LFS
pointer. None has a side effect. Paths below are
under `backend/src/main/java/com/odde/donut/` unless they start with `frontend/` or `e2e_test/`.

| | `GET /api/books/{book}/file` | `GET …/attachments/{attachment}/content` | `GET …/attachments/{attachment}/image` | `GET /api/notes/{note}/image?path=` |
| --- | --- | --- | --- | --- |
| Handler | `controllers/BooksController.java:37-55` | `controllers/NotebookAttachmentController.java:64-86` | `NotebookAttachmentController.java:88-106` | `controllers/NoteImageController.java:36-57` |
| Consumer | Book reader: `fetch` of a hand-built URL, then `arrayBuffer()` (`frontend/src/composables/useBookReadingBootstrap.ts:31-33,88,108`) | File page Download link (`frontend/src/pages/AttachmentPage.vue:68-73`); E2E checks the bytes and header (`e2e_test/start/pageObjects/attachmentPage.ts:45-56`) | File page `<img>` for image files (`AttachmentPage.vue:12-17,75-80`); E2E checks its content type (`attachmentPage.ts:59-65`) | Note header image from the `image:` property, path relative to the note's folder (`frontend/src/components/notes/widgets/ShowImage.vue:45-52`) |
| Authorization | Read access to the Book's notebook | Read access to the notebook, plus `requireInNotebook` (`entities/NotebookAttachment.java:66-70`) | Same as `/content` | Read access to the note, which is read access to its notebook (`services/AuthorizationService.java:105-108`) |
| Lookup | Book → notebook-root file named by `Book.sourceFilePath` (`services/book/BookSourceFile.java:24-32`) | Attachment id | Attachment id | Note + relative path (`services/notebookGit/NoteFolderAttachment.java:28-39`) |
| Content type | `application/pdf` or `application/epub+zip` from `Book.format` | `application/octet-stream` | PNG/JPEG/GIF/WebP by file name, else 415 (`controllers/InlineImage.java:17-30`) | Same as attachment image (shared `InlineImage.of`) |
| Disposition (`nosniff` stops the browser guessing another type) | `inline; filename="<book name>.<ext>"` (`services/book/BookFormat.java:139-149`) | `attachment` under the file name; `nosniff` | `inline`; `nosniff` | `inline`; `nosniff` |
| ETag / 304 / Cache-Control | Yes: MD5 of the stored Git content (`services/book/NotebookBookFile.java:18-41`); `private, max-age=365d, must-revalidate` in prod, `no-store` elsewhere (`configs/BookFileDownloadCacheControl.java:14-20`) | None | None | None |

**What is real distinction and what is duplication.**

- The lookups are real distinctions. Each consumer holds a different key: the reader holds a
  Book id, the file page holds an attachment id, and a note holds only a relative path. The two
  image endpoints already share their whole response through `InlineImage.of`.
- The Book file and `/content` are duplication. Both return the exact bytes of one notebook
  attachment. The reader ignores the response content type and disposition: it takes
  `arrayBuffer()` and picks PDF or EPUB from `book.format` (`useBookReadingBootstrap.ts:108-109`).
  The only behavior the Book endpoint adds is conditional caching, and that caching is not
  Book-specific: its ETag is computed from the attachment row's stored Git content.
- The Book endpoint owns a private code path used by nothing else: `BooksController`,
  `BookFileDownloadCacheControl`, `BookService.bookFile`/`streamBookFile`
  (`services/book/BookService.java:169-175`), `BookSourceFile`, `NotebookBookFile`, and
  `BookFormat.streamFile`/`bookFileMediaType` (`bookFileExtension` stays; `BookSourceFilePlacement`
  uses it).

**Recommendation: retire `GET /api/books/{book}/file` and serve Book bytes through
`/attachments/{attachment}/content`, which takes over the ETag/304/Cache-Control handling.
Keep both image endpoints as they are.**

- **Intended benefit:** one endpoint and one response path for "a notebook file's exact bytes";
  the generated API summary loses an option that duplicates another. The Book-only classes listed
  above go away, so the backend gets smaller. Caching becomes a property of notebook files
  instead of Books, so the file page Download also gains 304 responses.
- **Consumer impact:** the Book reader is the only consumer that changes. It needs the Book's
  source file attachment id, which it does not have today (`Book.sourceFilePath` is
  `@JsonIgnore`, `entities/Book.java:49-57`). No CLI, MCP, or E2E code calls the Book endpoint;
  E2E book-reading features reach it only through the reader UI. `/content` keeps its
  `attachment` disposition and `octet-stream` type, which `fetch` does not care about.
- **Migration work (about one story, S–M):**
  1. Move the ETag (MD5 of stored Git content) and the prod/non-prod `Cache-Control` choice into
     the `/content` handler, with a 304 on a matching `If-None-Match`; move the caching
     assertions from `BooksControllerTest` to `NotebookAttachmentControllerTest`.
  2. Expose the Book's source file attachment id on the Book payload the reader already loads
     (`NotebookBooksController.getBook`), and have `useBookReadingBootstrap` build the `/content`
     URL with `client.buildUrl` instead of a hand-written string.
  3. Remove `GET /api/books/{book}/file` and the Book-only classes listed above. Rewrite
     `NotebookBooksAttachControllerTest` and `NotebookBooksAttachNotebookFileControllerTest`,
     which read attached Book bytes through `getBookFile`, to read them through `/content`.
     Regenerate the API artifacts.
- **Not recommended:** merging the two image endpoints (a note has no attachment id, so the
  path lookup must stay), or merging `/image` into `/content` (they differ on purpose: `inline`
  image type versus forced download).

**Owner should know:** adding conditional caching to the image endpoints too would help note
header images, which are fetched again on every note view, but no evidence was gathered that
this is slow, so it is left out. Whether an attachment row keeps its id when its file is
replaced at the same path was not checked; the ETag with `must-revalidate` keeps responses
correct either way.

## Open Decisions

- Whether to adopt the file-serving proposal. If adopted, refine it into one
  story before slice planning.

## When to Surface

When the owner next reviews API redundancy or the Book reader's file loading.
