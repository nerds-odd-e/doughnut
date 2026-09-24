# Test file journeys on LFS notebooks as production has them

## Source

- Identity: SEED-035#story-20.
- Source: [refined story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-20)
  (refined 2026-09-24, owner accepted).
- Governing direction: North Star
  [one attachment content model](../../NORTH-STAR.md#one-attachment-content-model),
  "Every notebook uses LFS": one representation, so tests should not choose one.
- Depends on: story 14 (done; its plan is recoverable at `c8654d4b80:.planning/quick/025-convert-raw-notebooks-to-lfs/PLAN.md`).
  Checked on main (2026-09-24): the `NotebookGitBinding` default is LFS;
  `NotebookGitLfsConversionControllerTest` gets its raw notebooks from
  `createGitBackedNotebook`, so slice 5 moves it to `createLegacyRawNotebook`;
  the E2E conversion scenario (`cli_notebook_lfs`) seeds its raw files by
  local publish after the demotion, so slice 10's seeding change does not
  touch it.

## Goal and scope

Backend Git tests and E2E scenarios run on notebooks as the product creates
them (LFS with the standard `.gitattributes`), instead of demoting them to raw.
Scenarios that only repeat covered LFS journeys are deleted, not converted.
Afterwards only tests of raw-only behaviour use a raw notebook: the backend raw
size admission and publication tests, the raw `NotebookAttachmentFile` path,
and story 14's conversion tests.

Excluded:
- Removing raw production code, the demotion endpoint, or the no-binding raw
  path in `NotebookAttachmentFile` (story 19). Tests that use `makeMe.aNotebook()`
  without a binding are left as they are; story 19 deals with them.
- E2E speed work. LFS fill-in on every clone and pull makes the
  `@publicationProfile*` measurements slower; record it, do not tune it.
- The oversized-attachment refusal text. It advises that "a later tip
  deletion alone does not clear" the refusal, which is not true on LFS, where
  intermediate-only oversized objects are omitted. Reported to the owner as a
  finding; not changed here.
- New coverage.

## Outside-in proof (key examples)

1. A web note move carries a picture into another folder on an LFS notebook;
   the pulled checkout has the picture with identical bytes
   (`cli_notebook_publish_to_clean_clone` "Nested attachments follow a web
   folder rename into a clean clone", slice 9).
2. A scenario that only repeats a covered LFS journey is deleted:
   "Published root files reach another checkout byte for byte" (covered by
   `cli_notebook_lfs` publish → web save → fresh clone, and pull with a local
   image), and `notebook_files` "LFS file downloads its real bytes" (covered by
   the nested file scenario once it runs on LFS).
3. The oversized tip refusal runs on LFS and keeps its recovery by amending
   (`cli_notebook_attachment_size_admission`, slice 9).
4. Backend: a grep shows the raw fixture used only by raw-subject tests, and
   the backend suite is green with the default fixture on LFS (slice 5).

## Current decisions

- **Reserved Git metadata is not notebook content in listings.** Content
  listings used by notes-only assertions (backend `GitBundleTestReader`
  `tipPaths` / `pathsIn`, E2E `listCheckoutFilesRecursively` behind "…contains
  exactly") skip `.gitattributes`, using the product rule
  `NotebookGitAttributes.isMetadataPath` on the backend. Reason: the North
  Star calls it reserved Git metadata, and adding it to about 37 backend
  assertions and 30 E2E tables would repeat one fact everywhere. Tests whose
  subject is the metadata (`NotebookGitAttachmentMetadataControllerTest`,
  `NotebookGitBindingAssertions`, story 14's conversion tests) keep a reader
  that includes it. Considered and rejected: adding `.gitattributes` rows.
- **Proposals keep the accepted metadata.** `NotebookGitCommitFixtureTestSupport.proposalBundleBytes`
  carries the accepted tip's `.gitattributes` unless the test lists that path
  itself, as a local checkout does. Without it every LFS proposal silently drops
  its attributes and changes the "empty accepted notebook" check.
- **One LFS fixture name.** `createGitBackedNotebook` becomes the product LFS
  notebook; the demoting variant is renamed `createLegacyRawNotebook`;
  `createProductLfsNotebook` and `enableLfs` go.
- **LFS seeding follows the product.** Test seeding of file bytes stores the
  payload in the content store (the in-memory store outside `prod`) and writes
  the pointer, like `NotebookGitAttachmentLfsPublicationTestSupport.pointerFor`.
  No seeding helper branches on the representation.
- The hidden `resetHistory` inside the E2E demotion disappears with it. Every
  CLI scenario already resnapshots after it, except `cli_notebook_git_history_reset`
  (whose subject is the reset) and the two web features (their seeding
  resnapshots).

## Slices

Slice target about 5 minutes, hard limit 10, excluding E2E run time (an
external wait). Each slice ends green.

### 1. Proposal fixtures keep the accepted Git metadata

Type: Structure
Status: planned
Proof: backend Git controller tests green; `withAttributes`
(`NotebookGitAttachmentSizeAdmissionLfsHistoryTestSupport`) removed.

Change: `proposalBundleBytes` adds the accepted tip's `.gitattributes` entries
when the listed files do not name them. Raw notebooks have none, so their
behaviour is unchanged. Enables slice 5.

### 2. Tip listings compare notebook content, not reserved Git metadata

Type: Structure
Status: planned
Proof: backend Git controller tests green, including the metadata tests that
switch to the including reader.

Change: `GitBundleTestReader.pathsIn` / `readTreeEntries` / `tipPaths` skip
`NotebookGitAttributes.isMetadataPath`; metadata tests use an explicit
including variant. Enables slice 5.

### 3. Folder-attachment seeding stores LFS files

Type: Structure
Status: planned
Proof: the users of
`NotebookGitWebContentControllerTestBase.storeFolderAttachmentAndSnapshot`
green: DerivedFolderTreeOracle, DerivedTreeOracle, FolderDissolveGuard,
FolderRename, WebFolderCrossNotebookMove, WebFolderPermanentDelete,
WebFolderTrash, WebContentSaveCost support and representative experiment.

Change: promote `pointerFor` to the shared base; `storeFolderAttachmentAndSnapshot`
stores the payload and the pointer row; its users create the product LFS
notebook and assert pointers where they asserted raw bytes. Enables slice 5.

### 4. Proposals carrying attachments send LFS pointers

Type: Structure
Status: planned
Proof: ProposalFolderRelocationAttachment, PublicationAtomic,
AttachmentIndependence and AttachmentLocalChange green.

Change: these classes create the product LFS notebook and propose pointers
(payload stored through the shared `pointerFor`) instead of raw bytes. Enables
slice 5.

### 5. The default backend Git fixture is the product LFS notebook

Type: Structure
Status: planned
Proof: full backend test suite green; `createLegacyRawNotebook` is used only by
NotebookGitAttachmentSizeAdmission, …SizeAdmissionHistory,
…AttachmentPublication, …AttachmentMetadata, the contrast assertion in
…AttachmentCreation, and story 14's `NotebookGitLfsConversionControllerTest`.

Change: rename the demoting fixture to `createLegacyRawNotebook` for those
users; `createGitBackedNotebook` returns the product LFS notebook (about 128
test files); the LFS-subject tests (LfsPublication, SizeAdmissionLfsHistory,
LfsWebContinuity, WebContentSaveCost) drop `enableLfs` and hand-seeded
attributes. Slices 1–4 remove the known failure families. If other failures
remain after the first run, stop at 10 minutes, record their families here,
and replan them as separate slices before this one.

### 6. E2E checkout listings skip reserved Git metadata

Type: Structure
Status: planned
Proof: `cli_notebook_clone.feature` green (still raw, so unchanged).

Change: `listCheckoutFilesRecursively` (`e2e_test/config/cliE2eNotebookCloneGit.ts`)
skips `.gitattributes` beside `.git`. Enables slices 7–9.

### 7. Web note journeys pull into LFS checkouts

Type: Behavior
Status: planned
Proof: `cli_notebook_web_note_renames`, `cli_notebook_existing_note_edits`,
`cli_notebook_web_created_note` (including the publication profiles),
`cli_notebook_web_local_reconciliation`, `cli_notebook_web_note_moves` green.

Behavior: an owner's checkout of a notebook created by the product → web note
edits, creations, renames and moves → pull and local publishing work as before.
Remove the demotion step from these files.

### 8. Folder, trash, clone and reset journeys run on LFS notebooks

Type: Behavior
Status: planned
Proof: `cli_notebook_web_folder_moves`, `cli_notebook_web_trash`,
`cli_notebook_folder_relocation`, `cli_notebook_clone`,
`cli_notebook_git_history_reset` green.

Behavior: the same journeys on product LFS notebooks; the history reset
scenario now really needs its reset, and its single-commit clone keeps
`.gitattributes`. Remove the demotion step from these files.

### 9. Locally published files travel on LFS notebooks

Type: Behavior
Status: planned
Proof: `cli_notebook_publish_to_clean_clone` and
`cli_notebook_attachment_size_admission` green (key examples 1–3).

Behavior: remove the demotion; delete "Published root files reach another
checkout byte for byte"; keep the nested web folder rename scenario and the
oversized tip refusal with recovery by amending, now on LFS.

### 10. Web pictures and file pages are served from LFS files

Type: Behavior
Status: planned
Proof: `note_frontmatter_image.feature` and `notebooks/notebook_files.feature`
green.

Behavior: a note's picture in its own folder, and a nested file's page and
exact download, work on a product LFS notebook. `put_notebook_file_for_testability`
(`NotebookGitTestabilityController`) stores the payload in the content store
and writes the pointer, for any folder path and binary content. Remove the
demotion from both features and delete "LFS file downloads its real bytes",
which the nested file scenario now covers. Afterwards grep shows the demotion
step used only by story 14's conversion scenario, which seeds by local publish
and needs no raw branch in the seeding.

## Remaining concerns

None blocking. Slice 5's residual failures beyond the three known families are
unknown; its stop-and-replan rule bounds that risk.
