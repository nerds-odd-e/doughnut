# Notebook file browsing reports lost content and lists files cheaply

**Identity:** quick/023-notebook-file-listing-correction
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"2fc789579601414ea5f378cfe462f0a5636cb5dc94dbb1334059611dea2cddc6"}}
```

## Source

- Kind: bounded retrospective correction (no seed required).
- Provenance: execution of SEED-035#story-1 "Browse and download notebook
  files in Web Donut" (plan `15ec80eee6:.planning/quick/022-browse-download-notebook-files/PLAN.md`),
  commits `70b3b67313`, `f41c291823`, `a0ee337e40` on
  `story/browse-download-notebook-files` (claim `2725f1ecf0`). Retrospective
  2026-09-24.
- Beneficiary: notebook readers browsing files in Web Donut, and the
  developers who must learn when accepted attachment content is lost.
- Outcome: a missing LFS object behind a current file is a reported failure,
  the sidebar listing reads only each file's id and filename, and the file
  feature keeps only E2E scenarios that add integration proof.

## Current findings (scope)

1. Missing LFS bytes behind a current row raise
   `ResponseStatusException(NOT_FOUND, "File content unavailable: <filename>")`
   in `services/notebookAttachment/NotebookAttachmentFile.java`.
   `FailureReportFactory.createUnlessAllowed` skips every
   `ResponseStatusException`, so lost content never produces a failure report.
   Tip objects are required (`docs/notebook-git-lfs.md`), so a missing one is
   data loss, which plan 022 meant to fail loudly under
   [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md).
2. `FolderListing.attachments` returns whole `NotebookAttachment` entities from
   `NotebookAttachmentRepository.findByNotebook_IdAndFolderIsNullOrderByIdAsc` /
   `findByFolder_IdOrderByIdAsc` (`controllers/NotebookFolderQuerySupport.java`).
   `content` is an eager `@Lob byte[]` with no bytecode enhancement, so every
   sidebar expand reads every file's bytes in scope (raw notebooks up to
   10 MiB each). The frontend uses only `id` and `filename`.
3. `e2e_test/features/notebooks/notebook_files.feature` scenario "Files appear
   in the sidebar where they live" is covered by
   `NotebookFolderListingControllerTest.listsFilesAtRootAndInsideFolderOnlyInTheirOwnScope`,
   `SidebarPeerSort.spec.ts` "lists files with notes after folders, ordered by
   filename as title", and the other two scenarios, which open files through
   `physics/data` and at the root. The LFS scenario's root-rows step repeats
   what opening the file proves, and it reuses the CLI step with an irrelevant
   `obsolete payload`.

## Preserved promises and constraints

All SEED-035#story-1 promises stay: files at their folder or the root in the
sidebar (sorted with notes by title, not expandable), the file page (name,
size, sidebar expanded to its folder), exact bytes under the exact filename
with `Content-Disposition: attachment`, octet-stream and nosniff, read
authorization (Bazaar reader allowed, non-reader and foreign notebook refused),
LFS real bytes, empty `.keep`, raw pointer-looking content unchanged, and Git
metadata hidden. A file from another notebook stays a 404 business outcome.

Excluded: documentation of web browsing and download (story wrap-up); new file
features; changing the folder page's own payload.

## Current decisions

- Missing LFS object: throw an unhandled exception (for example
  `IllegalStateException`) whose message names the file as unavailable, so it
  becomes a 500 and a failure report. Keep one controller test asserting that
  exception type and message and that no pointer text is served; ADR 0006
  needs no test for the report itself.
- Listing shape: one small record `(id, filename)` for a file in a listing,
  selected by repository projection queries (pattern:
  `services/notebookTree/PortableTreeAttachmentRow`). `NotebookAttachmentRealm`
  (file page) carries the same record plus `size`, so the entity is no longer
  serialized anywhere; remove its `@JsonIgnore` and `@Schema` additions from
  plan 022 once nothing serializes it. The API JSON keeps `id` and `filename`;
  the generated type name changes and the frontend follows it.
- E2E: keep "Open a file and download its exact bytes" and "LFS file downloads
  its real bytes". Trim the Background to the rows those use and add a web
  LFS seeding step phrase without an obsolete payload (the testability API
  already treats it as optional).

## Slices

### 1. Lost LFS content is a reported failure
Type: Behavior
Status: done

Behavior: LFS notebook, `diagram.png` pointer row, store lacks the object →
download fails with an unhandled exception naming `diagram.png` as unavailable
(500 and failure report, not a 404 `ResponseStatusException`); no pointer text
is served. The page still shows the pointer's size.
Proof: `NotebookAttachmentControllerTest.LfsNotebook.missingBytesFailLoudlyNamingTheFile`
updated to the new exception type and message. Command B-focused.
Sizing: ~3 min.
Accepted: B-focused on `NotebookAttachmentControllerTest` passed (14 tests);
`missingBytesFailLoudlyNamingTheFile` asserts `IllegalStateException` with
"File content unavailable: diagram.png"; the pointer-size page test still passes.

### 2. Sidebar listing reads only id and filename
Type: Structure
Status: done

Replace entity loading in the folder listing with `(id, filename)` projection
queries and a listing record; use the same record in the file page payload;
drop the entity's serialization annotations; regenerate the API client and
update the frontend type references (`sidebarStructuralSort.ts`,
`SidebarAttachmentItem.vue`, `AttachmentPage.vue`, test fixtures). Response
JSON fields unchanged.
Proof: `NotebookFolderListingControllerTest` and `NotebookAttachmentControllerTest`
(Command B-focused on both), `tests/notes/sidebar/` and
`tests/pages/AttachmentPage.spec.ts` (Command F), vue-tsc, Command G, and
Command E (the kept scenarios exercise listing, page and download over HTTP).
Sizing: ~7 min (API regeneration and E2E wait).
Accepted: record `controllers/dto/NotebookAttachmentListItem` filled by JPQL
projections `findRootListItemsByNotebookId` / `findListItemsByFolderId`; B-focused
(24 tests), F (23 tests + vue-tsc), G (schema renamed to
`NotebookAttachmentListItem`, fields unchanged), E (3/3) passed.

### 3. File feature keeps only integration-bearing scenarios
Type: Structure
Status: done

Remove "Files appear in the sidebar where they live", trim Background rows
only it used (`force.png`, `paper.pdf`, `results.csv`, and the `Force` note if
unused), drop the LFS scenario's root-rows step, add the web LFS seeding
phrase without obsolete payload, update the feature narrative to the page and
download goal, and remove page-object helpers or steps left unused.
Surviving coverage: the two kept scenarios, the listing controller test and
the sidebar sort spec named in finding 3.
Proof: Command E (2 scenarios passing).
Sizing: ~4 min.
Accepted: E passed 2/2; the web and CLI LFS seeding share
`testability().acceptLfsAttachmentTipForTestability` (optional obsolete
payload), and `cli_notebook_lfs.feature` passed 10/10 with it.

## Proof ownership

| Promise | Slice |
| --- | --- |
| Lost LFS content fails loudly and is reported; no pointer text | 1 |
| Listing and page keep `id`/`filename` without reading file bytes | 2 |
| All story-1 promises still proved at lower E2E cost | 2, 3 |

## Commands

- B-focused: `unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL; CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests '<class>'`.
- F: `CURSOR_DEV=true nix develop -c pnpm frontend:test <spec>` plus
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`.
- G: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`.
- E: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/notebooks/notebook_files.feature`.

All slice proof ran and passed.

## Learnings

- `pnpm cy:run` must run outside the command sandbox; inside it Electron never
  connects and Cypress aborts before any test runs.
