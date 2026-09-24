# Browse and download notebook files in Web Donut

## Source

- Identity: SEED-035#story-1.
- Source: [refined story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-1)
  (refined in `e92ea9add4`).
- Preceding delivery checked: SEED-035#story-15 (same-checkout receive,
  `2dc0ce9478`..`f6b4578a15`) and its queued correction
  [021](../021-pull-recovery-guidance/PLAN.md). Both change only the CLI's
  pull/publish path. They do not change attachment rows, the LFS content store,
  or the web, so this story's scope is unchanged. Two things carry over: the
  testability LFS tip (`accept_lfs_attachment_tip_for_testability`, obsolete
  payload now optional) seeds the LFS example, and an LFS checkout depends on
  the root `.gitattributes`, which supports keeping it hidden from the web
  (decision below).
- Preparation workspace: `/Users/terryyin/git/doughnut/.claude/worktrees/plan-web-file-browsing`,
  branch `prep/web-file-browsing`, session-created from `3f863850f4`;
  integration checkout `/Users/terryyin/git/doughnut`; publication target
  `origin/main`. Implementation is not authorized by this plan.

## Execution

- Mode: Story Branch. Workspace `/Users/terryyin/git/doughnut/.claude/worktrees/browse-download-notebook-files`
  (created), branch `story/browse-download-notebook-files`, starting revision
  `15ec80eee6`; integration checkout `/Users/terryyin/git/doughnut`; publisher
  `claude-job-dd72230f`.
- Claim published on `origin/main` as `2725f1ecf0`. Increments publish to
  `origin/story/browse-download-notebook-files`.

## Goal and scope

Anyone who can read a notebook sees its non-Markdown files in the sidebar tree
at their folder or the root, opens a file's page (filename, size), and
downloads the exact accepted bytes under the exact filename, for raw and LFS
notebooks alike.

Excluded (story's deferred promises): previews, editing, upload, rename, move,
delete, earlier versions, folder ZIP, search, files outside the sidebar and
attachment page.

## Current decisions

- **One source for files:** `notebook_attachment` rows (`NotebookAttachment`:
  notebook, folder or root, filename, accepted Git content). The sidebar lists
  rows in the listed scope, and the page and download read one row. Trashed
  folders are ordinary `_trash` folders, so their files follow with no extra
  rule. A root `.keep` is an ordinary row. Git metadata stays hidden like
  `.git` (owner decision 2026-09-24): `.gitattributes` exists only in the
  accepted Git tree and nested `.keep` files are generated empty-folder
  markers, so neither is a row and neither is listed. No second file source
  from Git is added.
- **Listing:** `FolderListing` (`GET /api/notebooks/{notebook}/folder-listing`)
  gains `attachments` (id, filename) for the same root or parent scope, under
  its existing `assertReadAuthorization`. The frontend adds an `attachment`
  kind to `SidebarStructuralRow`. It sorts by filename as its title, has no
  dates (date sorts put it last, as the existing NaN rule does), and is not
  expandable. `sidebarFolderListingCache` needs no new invalidation: files
  change only through publication or folder operations that already refresh.
  Other `FolderListing` users (`useFolderSelectorNeighbourListing`,
  `relationshipFolderResolve`) ignore the new field.
- **Addresses ([ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md)):**
  the page route is `attachmentPage`, `/notebooks/:notebookId/attachments/:attachmentId`.
  It is registered in `routeMetadata.ts`, sits in the sidebar layout, and is
  keyed by row id, not by path. The API has
  `GET /api/notebooks/{notebook}/attachments/{attachment}` (page payload:
  filename, size, and the same sidebar chrome/ancestry the folder page gets)
  and `GET …/attachments/{attachment}/content` (bytes). Both use
  `assertReadAuthorization` and refuse a row from another notebook.
- **Download response:** always
  `Content-Disposition: attachment` built with Spring `ContentDisposition`
  (UTF-8 filename, not string concatenation), `application/octet-stream`, and
  `X-Content-Type-Options: nosniff`. An SVG or HTML file can therefore never
  render in Donut's origin.
- **Representation:** the notebook's `NotebookGitBinding.attachmentRepresentation`
  selects the representation; the bytes are never sniffed, because a raw
  notebook's pointer-looking bytes are ordinary content. For RAW, the bytes
  and size are the row content. For LFS, a zero-byte file
  (`NotebookGitLfsPointer.isEmptyFile`) is served empty. Otherwise
  `NotebookGitLfsPointer.parse` gives the oid and size, and the bytes come from
  `NotebookAttachmentContent.get`. If they are missing, the download raises an
  error that names the file as unavailable
  ([ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md)). It never
  serves the pointer text. One small owner (for example on the
  `notebookAttachment` service package) answers "size and bytes of this row",
  so the page and the download do not each branch on the representation.
- **E2E seeding:** add one testability step that puts a raw file at a
  notebook path (creating folders) and then resnapshots the binding. The
  existing full assembly already encodes rows into the accepted tree. The LFS
  example reuses `accept_lfs_attachment_tip_for_testability`, whose root
  placement matches `diagram.png`. Download observations request the page's
  download link and check the body and `Content-Disposition`, rather than
  reading the browser downloads folder.

## Slices

### 1. Files appear in the sidebar at their place
Type: Behavior
Status: done

Behavior: raw notebook with note `physics/Force`, files `physics/force.png`,
`physics/data/run.json`, and `refs/paper.pdf` only → open the notebook sidebar,
expand `physics` → it shows the note, `force.png` and folder `data`; expanding
`data` shows `run.json`; `refs` shows `paper.pdf` instead of looking empty. A
root file appears at the root.

Proof: backend controller test for `folder-listing` (root and nested scopes
list their own files only); frontend sidebar test (attachment rows rendered and
sorted with notes/folders by title); new E2E feature
`e2e_test/features/notebooks/notebook_files.feature` scenario "Files appear in
the sidebar where they live" with the new testability step. Regenerate the API
client. Commands B, F, E.
Sizing: ~8 min (one listing field end to end plus E2E wait).

### 2. Open a file's page and download its exact bytes
Type: Behavior
Status: planned

Behavior: slice 1's notebook → select `run.json` in the sidebar → its page
shows `run.json` and its size in bytes, with the sidebar expanded to `data`; the
page's download link returns exactly the accepted bytes as an attachment named
`run.json`. `drawing.svg` downloads with `Content-Disposition: attachment` and
never renders inline. A root zero-byte `.keep` downloads as an empty file named
`.keep`. A Bazaar reader can see the page and download the file. A user who
cannot read the notebook gets neither, and a file from another notebook is
refused.

Proof: backend controller tests for the page and content endpoints (exact
bytes, UTF-8 filename header including a non-ASCII name, SVG disposition and
nosniff, empty `.keep`, Bazaar reader allowed, non-reader refused, foreign
notebook refused); frontend page test (name, size, link compiled from the
route/API owner); E2E scenario "Open a file and download its exact bytes" in
the same feature. Regenerate the API client. Commands B, F, E.
Sizing: ~10 min, above the ~5 min target, because it covers two endpoints,
a route and a page, and E2E wait (external-wait exception). Split into page
and download if it overruns.

### 3. LFS notebook files download their real bytes
Type: Behavior
Status: planned

Behavior: a new LFS notebook has `diagram.png` at its root (testability LFS
tip) → the sidebar lists it, its page shows the pointer's size, and download
returns the stored bytes, whose SHA-256 matches the pointer, not the pointer
text. When the store lacks those bytes, the page still shows the file, but the
download fails with a clear "file content unavailable" error and serves no
pointer text. A zero-byte `.keep` in an LFS notebook downloads empty. A raw
notebook whose file content looks like a pointer still downloads that content
unchanged.

Proof: backend controller tests with the in-memory content store (LFS bytes,
pointer size on the page, missing bytes raise the error, empty file, raw
pointer-looking content served as-is); E2E scenario "LFS file downloads its
real bytes" in the same feature. Commands B, E.
Sizing: ~6 min.

## Proof ownership

| Story promise / key example | Slice |
| --- | --- |
| Files at folder and root in the sidebar; `physics`/`data` example | 1 |
| Folder holding only `paper.pdf` is not empty | 1 |
| Files in a trashed folder behave the same | 1 (no special rule; `_trash` is a folder) |
| Page shows filename and size; exact bytes under exact filename | 2, 3 |
| Read authorization, Bazaar reader, no access for others | 2 |
| SVG/HTML always downloaded, never rendered inline | 2 |
| Root zero-byte `.keep` appears and downloads | 1, 2, 3 |
| LFS download returns real bytes matching the pointer | 3 |
| Unavailable LFS bytes fail loudly, never pointer text | 3 |
| Git metadata (`.gitattributes`, nested `.keep`) hidden | 1 (not rows; owner decision 2026-09-24) |

## Commands

- B: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (linked worktree
  isolates it; see `docs/worktree-backend-tests.md`).
- F: `CURSOR_DEV=true nix develop -c pnpm frontend:test <spec>` plus the
  frontend typecheck required by the `frontend` skill.
- G: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` after DTO or
  controller changes.
- E: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/notebooks/notebook_files.feature`.

## Learnings

- Seeding: testability step `the notebook "…" has files:` (`put_notebook_file_for_testability`,
  path + content) creates folders and resnapshots. Its helper chains SDK calls with
  `cy.then` because parallel `cy.wrap(promise)` puts race on the accepted tree.
  Slices 2 and 3 reuse it.
- A new feature file must be listed in `scripts/isolated-cypress-active-specs.mjs`
  before a worktree `cy:run` accepts it.
- `FolderListing.attachments` returns `NotebookAttachment` (id, filename); notebook,
  folder and content are `@JsonIgnore`. Sidebar rows use `SidebarAttachmentItem.vue`
  (no link yet; slice 2 adds the page link).
- Slice 1 proof: `NotebookFolderListingControllerTest.listsFilesAtRootAndInsideFolderOnlyInTheirOwnScope`,
  `SidebarPeerSort.spec.ts` "lists files with notes after folders…", E2E
  "Files appear in the sidebar where they live".
