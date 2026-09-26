# Remove the separate Book storage

## Source

- Identity: SEED-035#story-21.
- Source: [refined story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-21)
  (refined 2026-09-26; owner decisions: drop `attachment_blob` outright with
  no backup; remove the `/attachments/` address; delete the Book GCS bucket
  within this story; backlog order kept, since story 2 is independent).
- Governing direction: North Star
  [moving and retiring](../../NORTH-STAR.md#moving-and-retiring) ("delete old
  stores and their code in a later release, after the move is confirmed in
  production") and [one store for file bytes](../../NORTH-STAR.md#one-store-for-file-bytes)
  (the separate Book storage and `attachment_blob` retire).

## Execution gate

Do not start slice 2 until both hold (slice 1 changes nothing observable and may run earlier):

1. The release carrying story 17's Book move (merge `ecf5c531a1`; not in
   v1.3.26) is deployed.
2. A read-only production query returns 0:
   `SELECT COUNT(*) FROM book WHERE source_file_path IS NULL`.

Record the release tag, query and result under Learnings. If the count is not
0, stop and report; do not retry the move from this plan.

## Goal and scope

Maintainers keep one attachment implementation and one place for file bytes.
Every Book reads only from its notebook file; the old Book storage, the old
picture bytes, the `/attachments/` address and the Book bucket are gone.
Owners notice nothing: Books keep reading with their progress.

Excluded (seed): any change to how Books are attached, read, laid out or
limited (the 100 MB Book upload limit stays); any backup or export of the
removed bytes; the duplication between `BooksControllerTest` and
`NotebookBooksBookFileControllerTest` beyond removing their old-storage cases.

Assumptions (checked on main `450bd49f21`, 2026-09-26):

- Old storage: `BookStorage`, `GcsBookStorage`, `DbBookStorage`,
  `BookStorageConfiguration`, `AttachmentBlob`, `AttachmentBlobRepository`,
  `LegacyBookSourceFileMove`, `LegacyBookSourceFileMoveOnStartup`, the
  `path == null` branch in `BookSourceFile.read`, and `Book.sourceFileRef`
  (already nullable since `V300000344`). Nothing in the API exposes
  `sourceFileRef` (`@JsonIgnore`).
- `BookStorageConfiguration` also defines the prod GCS `Storage` bean
  (`bookGcsClient`) that `NotebookAttachmentContentConfiguration` injects for
  the notebook LFS store. It must move there, not disappear. No test loads the
  prod profile's full context (`ShedLockConfigProdTest` is the narrow-context
  pattern).
- Test fixtures build Books through the old storage: `BookBuilder` calls
  `makeMe.bookStorage.put`, and `NotebookBuilder.withBook` uses it.
  `NotebookAttachmentBuilder` already stores a notebook file as the product
  does; `NotebookBooksBookFileControllerTest.returnsTheNotebookFileWhenSourcePathNamesARootAttachment`
  already covers reading from a notebook file.
- E2E attaches Books through the real CLI/web path, which already writes
  notebook files; the Book E2E features are unchanged preserved-behavior proof.
- Latest migration is `V300000346__drop_image.sql`.
- `/attachments/` appears in `frontend/vite.config.ts`,
  `infra/gcp/path-routing/doughnut-routing.json`,
  `infra/gcp/path-routing/rendered-url-map-routing.test.mjs` (`pnpm test:path-routing`,
  `pnpm validate:path-routing`) and `docs/gcp/prod-frontend-static-lb.md`.
  The routing is published by the application release. The SPA route
  `/notebooks/:notebookId/attachments/:attachmentId` does not match the
  root-prefix `/attachments/`.
- The bucket's access bindings are bucket-level (`docs/gcp/prod_env.md` §7),
  so deleting the bucket removes them.

PFE: reuse `NotebookAttachmentBuilder` for Book fixtures, the existing
notebook-file read in `BookSourceFile`, and `NotebookAttachmentContentConfiguration`
as the home of the prod storage client. Gap: none; this is removal.

## Outside-in proof

| Promise (seed example) | Slice | Observable proof |
| --- | --- | --- |
| A Book attached before story 17 opens reading from its notebook file and resumes its position (1) | 2 | Execution gate query (every production Book has a path); backend Book controller tests build Books on notebook files and pass with the old read branch gone; Book E2E features pass |
| Production still starts with the notebook LFS store after the Book config is gone | 1 | narrow prod-profile context test in the `ShedLockConfigProdTest` shape: `NotebookAttachmentContentConfiguration` yields `GcsNotebookAttachmentContent`. If creating the GCS client needs credentials in CI, drop the test, record that, and rely on the release smoke check |
| Attaching a new Book in development or test works with no `attachment_blob` table (2) | 3 | migration applied to the test database; attach and read Book controller tests and Book E2E pass; the schema has no `attachment_blob` or `book.source_file_ref`, and `book.source_file_path` is NOT NULL |
| Cloning a notebook with a Book still delivers its source file (3) | 2–3 | existing clone E2E for the Book's source file stays green |
| A request to `/attachments/...` is no longer sent to the backend (4) | 4 | `pnpm test:path-routing` and `pnpm validate:path-routing` with `/attachments/` gone from the backend hints and the test's backend list |
| The Book bucket no longer exists; pictures and Book files still download (5) | 5 | `gcloud storage buckets describe gs://doughnut-book-pdf-carbon-syntax-298809` reports not found; the notebook LFS bucket still describes; the owner opens one Book in production |

## Slices

### 1. The production storage client belongs to notebook files
Type: Structure
Status: done
Accepted proof: `NotebookAttachmentContentConfigurationProdTest`
(prod profile, only `NotebookAttachmentContentConfiguration` loaded →
`GcsNotebookAttachmentContent`); passes without local gcloud config, so the
test stays. `*Book*` / `*NotebookAttachment*` backend tests pass (159).
Proof: the prod-profile context test (second row above).

Internal change: move the prod GCS `Storage` bean from
`BookStorageConfiguration` into `NotebookAttachmentContentConfiguration`, the
notebook LFS store that also uses it; the Book storage keeps receiving it by
type. External behavior unchanged. Enables slice 2, which deletes
`BookStorageConfiguration` without leaving the notebook LFS store without a
client in production.

### 2. Every Book reads only from its notebook file
Type: Behavior
Status: done
Accepted proof: `BookSourceFile.read` has one path (notebook-root file or
404); `NotebookBooksBookFileControllerTest$GetBookFile` and
`BooksControllerTest$GetBookFileByBook` PDF/EPUB/304 cases rebuilt on
notebook files via `BookBuilder`; full backend suite 2608 pass. The clone
promise rests on `cli_notebook_lfs.feature` in CI (`cli_notebook_clone.feature`
does not name Books).
Proof: slice 2 rows above.

Behavior: a Book whose source path names a notebook-root file → it is read →
the bytes come from that file; there is no other place a Book's bytes come
from.

Change: `BookBuilder` places the Book's source file at the notebook root
through `NotebookAttachmentBuilder` and sets the path (no `bookStorage`).
Delete `BookStorage`, `GcsBookStorage`, `DbBookStorage`,
`BookStorageConfiguration`, `LegacyBookSourceFileMove`,
`LegacyBookSourceFileMoveOnStartup`, the `path == null` branch in
`BookSourceFile`, `Book.sourceFileRef`, `AttachmentBlob` and its repository.
Remove `donut.book-pdf.*` and the Book storage comments from
`application.yml` / `application-prod.yml`. Tests: delete
`GcsBookStorageTest`, `LegacyBookSourceFileMoveControllerTest`, and the
old-storage cases (`…SourceFileRef…`, the EPUB and 304 cases rebuilt on a
notebook file); change `NotebookBooksRetrievalControllerTest`'s ref assertion
to the path, and drop the `getSourceFileRef()` assertion in
`NotebookBooksAttachNotebookFileControllerTest`.

### 3. The database keeps no Book or picture bytes
Type: Behavior
Status: planned
Proof: slice 3 row above.

Behavior: a fresh or migrated database → attach and read a Book → works with
no `attachment_blob` table; every Book row has a path.

Change: one Flyway migration (follow the `db-migration` skill): drop
`attachment_blob`, drop `book.source_file_ref`, make `book.source_file_path`
NOT NULL. The gate guarantees production rows satisfy it; if one does not,
the migration fails loudly (ADR 0006). Regenerate `docs/database-erd.md`
(`database-erd` skill).

### 4. The old picture address is gone
Type: Behavior
Status: done
Accepted proof: `pnpm test:path-routing` (catch-all test now asserts
`/attachments/x` reaches the static bucket) and `pnpm validate:path-routing`
pass; `docs/gcp/prod_env.md` §6 also lost `/attachments`.
Proof: slice 4 row above.

Behavior: a request to `/attachments/x` → it is no longer classified as a
backend path; the Vite dev server no longer proxies it.

Change: remove `/attachments/` from `doughnut-routing.json` and from the
backend list in `rendered-url-map-routing.test.mjs`; remove the `/attachments`
proxy in `frontend/vite.config.ts`; remove the `/attachments/*` row and the
"attachments" wording (lines 24 and 194) in `docs/gcp/prod-frontend-static-lb.md`.

### 5. The Book bucket is deleted
Type: Behavior
Status: planned
Proof: slice 5 row above.

Behavior: the production Book bucket → deleted → it no longer exists; the
notebook LFS bucket and Book reading are unaffected.

Change: remove `docs/gcp/prod_env.md` §7 (the bucket's setup, IAM and orphan
notes; keep the 100 MB upload limit sentence where Book attach is described,
or move it next to the notebook LFS section). Then, as the last step and only
after the owner confirms the exact command in the session:
`gcloud storage rm --recursive gs://doughnut-book-pdf-carbon-syntax-298809`.
It cannot be undone. Code that still names the bucket in the deployed release
reads it only for a Book without a path, which the gate ruled out.

## Current decisions

- Slices 2–5 start only after the gate; merging slices 2–3 before it would let
  a release drop the old copy of an unmoved Book.
- Drop `attachment_blob` with no backup (owner, 2026-09-26).
- The bucket is deleted inside this story, confirmed by the owner at the
  moment of deletion.

## Learnings

- Execution gate met 2026-09-26: Application Release `v1.3.27` (run
  36202449200, success) is deployed — production health reports commit
  `450bd49f21`, the tag's SHA, which contains `ecf5c531a1`. The owner ran
  `SELECT COUNT(*) AS unmoved FROM book WHERE source_file_path IS NULL` on
  production (from the app VM against `db-server`); result `0`.
- Production facts for later slices: the Cloud SQL instance is `doughnut-db`
  (not `doughnut-db-instance` as `docs/gcp/prod_env.md` §4 says); the app VM
  is recreated by the MIG under the same instance ID, so SSH may first need
  its stale host key removed.
