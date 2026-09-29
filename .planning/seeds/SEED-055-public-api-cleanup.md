---
id: SEED-055
status: dormant
planted: 2026-09-29
planted_during: owner request to review and clean up the current backend public API
trigger_when: prioritizing removal of unused public APIs and reconsideration of redundant services
scope: medium
---

# SEED-055: Keep the public API aligned with supported product features

## Why This Matters

For Donut maintainers, the backend public API should expose capabilities that
serve real frontend or external features. Dead endpoints add maintenance work,
and endpoints that provide duplicated services deserve reconsideration.
The owner wants unused APIs found and removed, and actionable improvements
proposed for redundancies, while preserving supported feature behavior.

## Alternatives and Decision

The owner selected a review of the current API using the generated TypeScript
client or OpenAPI YAML as the starting inventory. Keeping an endpoint because
it has tests does not establish product use. An inventory-only report would
leave confirmed dead APIs in place, so this story includes their cleanup.
Redundant APIs require comparison of their actual services and consumers;
this story proposes improvements without presuming every overlap should merge.

## Story Decomposition

Effort bands: S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery.
This seed captures one cleanup outcome; it does not authorize an audit or
implementation now.

<a id="story-1"></a>

### Remove unused public APIs and propose improvements for redundant services

**Identity:** SEED-055#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/012-public-api-cleanup/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"11ac7526dbf86e0d8a04314f733e2cae230570ee05b533ddd7a607d20a3bd40d","plan":"35244fb9fd161058946a269429558c9676478656f20256a86cc203077b7482bd"}}
```

**Goal**

Donut maintainers and their coding agents work from a public API that
advertises only endpoints serving real features. The generated API summary is
the agents' default endpoint lookup, so every dead or duplicated endpoint is a
wrong option an agent can pick and build on. This story removes the confirmed
dead endpoints with everything only they kept alive, and gives the owner one
evidence-backed improvement proposal for the overlapping file-serving endpoints
left behind by the attachment, Book, and LFS migration. Supported frontend and
external feature journeys keep working.

**Scope**

- An endpoint is used when a frontend, CLI, MCP, or supported external feature
  calls it, through the generated client or direct HTTP (for example Git and
  Git LFS transfer, page image and file URLs, event streams, install).
  Endpoints used by E2E tests, such as the testability controllers, are exempt
  from removal as test infrastructure. Other test calls, fixtures, and mocks do
  not count as use.
- Remove the endpoints with no such use. A usage review on 2026-09-29 found
  these five; the implementer confirms each before removal:
  - `DELETE /api/user/token-info` (revoke token)
  - `GET /api/user/recall-ez-diffusion`
  - `GET /api/user/daily-probe-convergent-validity`
  - `GET /api/memory-trackers/{memoryTracker}/recall-logs`
  - `GET /api/notebooks/{notebook}/book/file`, found during slice planning: the
    frontend reads Book bytes only through `GET /api/books/{book}/file`
- Removal is transitive: also remove every implementation piece whose only
  dependent was a removed endpoint (services, queries, DTOs, entities or
  columns, helpers, tests, fixtures), repeating until nothing orphaned
  remains. Then clean up across the whole product scope (backend, frontend,
  CLI, MCP, documentation, agent guidance) so the result reads as though the
  removed things never existed. Regenerate the API artifacts through the
  repository's generation workflow.
- Removal leaves no trace: no negative tests, absence checks, or historical
  notes replace what was removed.
- Compare the remaining file-serving endpoints — `GET /api/books/{book}/file`,
  `GET /api/notebooks/{notebook}/attachments/{attachment}/image`,
  `GET /api/notebooks/{notebook}/attachments/{attachment}/content`, and
  `GET /api/notes/{note}/image` — by consumer, authorization, inputs, outputs,
  and side effects, then deliver one concrete proposal naming affected
  endpoints, intended benefit, consumer impact, and migration work.
  Implementing it is a later product decision.
- Deferred: redundancy review of the rest of the API, implementing any
  consolidation, the code-generation workaround endpoint
  (`AiController.dummyEntryToGenerateDataTypesThatAreRequiredInEventStream`),
  unused DTO fields or parameters, dead code unrelated to removed endpoints,
  and new API capabilities.

**Key examples**

- The recall logs endpoint has backend tests but no frontend or external
  caller → remove it, the implementation only it used, and its tests;
  regenerate the API artifacts; nothing asserts it is gone.
- A testability endpoint is called only by E2E setup → keep it.
- `POST /api/notebooks/{notebook}/attach-book` has no generated-client caller
  but the CLI calls it over HTTP → keep it as used by the CLI.
- A service method was used only by a removed diagnostic endpoint → remove it
  too, and any query or DTO only that method used.
- The Book file and attachment content endpoints return the same bytes, but
  only the Book endpoint sets caching headers → the proposal explains who calls each and what differs, then
  recommends consolidating or keeping the distinction, with its migration impact.

**Output and evaluation**

The owner can review the confirmed usage evidence for each removed endpoint,
the removal diff, and the file-serving proposal. The public API artifacts no
longer advertise the removed endpoints, and the relevant automated checks
show that supported feature journeys still work.

- **For / why:** Maintainers and agents stop reading, maintaining, and
  building on endpoints no feature needs.
- **Value / learning:** Whether the migration's overlapping file-serving
  endpoints are a real distinction or duplication worth consolidating.
- **Effort hypothesis:** S–M. The main usage review is done; the rest is
  confirming it, transitive removal, and one focused comparison.
- **Depends on:** Nothing queued.
- **Safe stopping point:** After the removals land with behavior preserved;
  the proposal is useful independently.

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

## Ordering and Scope Reduction

The owner requested this story at the top of the product backlog, ahead of
SEED-054#story-1. Establish feature-use evidence before removal. Redundancy
implementation is deferred; the requested recommendations remain in scope.

## Open Decisions

- Whether to implement the file-serving proposal is decided after the owner
  reviews it.

## When to Surface

Next, as the highest-priority queued story requested by the owner.

## Breadcrumbs

- Owner request on 2026-09-29: check the current backend public API through the
  generated TypeScript code or YAML; unused means no frontend or external
  feature use, and tests do not justify keeping an API. Find and clean up dead
  APIs and propose improvements for duplicated services.
- Owner refinement on 2026-09-29: accepted the four removals, the E2E
  exemption, and limiting redundancy review to the file-serving endpoints;
  removal is transitive and followed by whole-product cleanup, with no
  negative tests.
- `.agents/agent-map.md` identifies the generated API summary, TypeScript
  client, OpenAPI YAML, backend routes, frontend, CLI, and MCP entry points.
