# Tidy LFS test support into one vocabulary

**Identity:** quick/028-tidy-lfs-test-support
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"039fdcb9e5009b2cfc7157fa6e24d6d4720a8f1c60cdc30ec7cc7f9074119461"}}
```

## Source

- Kind: bounded retrospective correction (no backlog identity yet; no seed).
- Provenance: execution retrospective of story SEED-035#story-20
  (plan 026, recoverable at
  `4e0e684f1a:.planning/quick/026-test-file-journeys-on-lfs/PLAN.md`), reviewed commits
  `5859e6d663..47a3bdd0ab` on main. Findings F3, F4, F6, F7; evidence
  locations below are at HEAD `47a3bdd0ab`.
- Governing direction: North Star
  [one attachment content model](../../NORTH-STAR.md#one-attachment-content-model),
  "Every notebook uses LFS". Test support should not keep raw-era names or
  choose a representation the product already chooses.
- Beneficiary: developers reading and changing backend and E2E notebook Git
  tests. Story 19 (removing raw) must be able to delete the "legacy raw"
  helpers without breaking LFS seeding.

## Goal and scope

Backend and E2E test support for LFS notebooks uses one clear set of names,
with no leftovers from when notebooks were raw. Behaviour coverage stays the
same; the test code gets smaller and clearer.

Findings in scope:

- **F3 Seeding helper names point the wrong way.**
  `NotebookGitWebContentControllerTestBase` (~46-62): the product-LFS
  `storeFolderAttachmentAndSnapshot` calls `storeLegacyRawAttachmentAndSnapshot`,
  which only writes the given accepted bytes. Story 19 deleting "legacy raw"
  helpers would break LFS seeding.
- **F4 Reader names do not say whether metadata is included.**
  `backend/src/test/java/com/odde/donut/testability/GitBundleTestReader.java`:
  static `readTreeEntries` leaves out metadata, while `readTreeEntriesWithMetadata`
  keeps it; `AcceptedTip.entries()` and `AcceptedHistory.tipContent()` keep it,
  `.content()` leaves it out; `fetchTipTreeEntries` keeps it.
- **F6 Leftover and overlapping tests.**
  `NotebookGitWebContentSaveCostControllerTest` (~61-64) still sets LFS by hand
  (LFS is now the default) and overlaps the first test (~32), which already
  seeds LFS attachments; the javadoc on it ("Observes JDBC executions …")
  describes a different test. `NotebookGitAttachmentMetadataControllerTest`
  (~98) `lfsRepresentationFixtureProjectsPointerBytesWithoutPublication` only
  checks the seeding fixture, which dozens of tests now use.
- **F7 Overlapping E2E seeding.**
  `backend/src/main/java/com/odde/donut/testability/NotebookLfsTestabilityController.java`
  `acceptLfsAttachmentTipForTestability` still forces the binding to LFS and
  resets history with `NotebookGitAttributes.initialMetadata()`. Both are
  redundant now, and they could hide a raw notebook by silently converting it.
  Used by `e2e_test/features/cli/cli_notebook_lfs.feature` through
  `e2e_test/start/pageObjects/cli/notebookLfs.ts`.

Preserved promises and constraints:

- No behaviour or product change.
- Every current assertion keeps its coverage (moved or merged, not lost).
- Raw-subject tests stay raw; story 19 removes raw.
- No new coverage. `makeMe.aNotebook()` users are untouched.

Excluded (owner decisions):

- F1 CLI `isRepresentedFolder` non-ASCII quoting (`notebookAcceptedAdditionInterval.ts`)
  goes to bug-fixing.
- F2 moving representation-independent tests out of the raw
  `NotebookGitAttachmentPublicationControllerTest` and the AttachmentMetadata
  authored-`.gitattributes` test.
- F5 sharing the product's package-private `NotebookGitProposalTreeShape.isAttachment`.
- The oversized refusal text.
- `NotebookGitAttachmentCreationControllerTest` ~48 (story 19).

Assumptions: the metadata-preserving `NotebookGitCutoverService.resetHistory(notebook, time)`
keeps the accepted `.gitattributes` of an existing binding and installs the
initial metadata for a new one (checked at `NotebookGitCutoverService` ~76-87),
so the E2E seeding no longer needs to pass the metadata.

## Outside-in proof (key examples)

1. A test that seeds an LFS attachment reads as "store accepted bytes (a
   pointer)". No LFS seeding path goes through a name containing "LegacyRaw".
   Grep: `storeLegacyRawAttachmentAndSnapshot` returns nothing (slice 1).
2. Every `GitBundleTestReader` name that returns entries follows one rule:
   `content` leaves out reserved Git metadata, and `exactTree` includes it.
   Grep: `readTreeEntries`, `readTreeEntriesWithMetadata`, `fetchTipTreeEntries`,
   `tipContent` and the `AcceptedTip.entries` accessor are gone (slice 2).
3. The save-cost suite has one LFS-attachment test. It proves that a note-only save
   does not query attachment rows, does not touch the content store, and
   leaves the attachment pointer unchanged. No test sets the representation
   by hand, and the fixture-only metadata test is gone (slice 3).
4. The E2E "accepted LFS tip" seeding does not change the representation
   or install metadata, and `cli_notebook_lfs.feature` stays green (slice 4).
5. Backend `NotebookGit*` tests green after every backend slice. The full
   backend suite is green at the end.

## Current decisions

- **Naming rule:** `content` = notebook content without reserved Git metadata;
  `exactTree` = every blob, including metadata. Renames:
  `readTreeEntries` → `readContent`, `readTreeEntriesWithMetadata` →
  `readExactTree`, `fetchTipTreeEntries` → `fetchTipExactTree`,
  `AcceptedTip.entries` → `exactTree`, `AcceptedHistory.tipContent` →
  `exactTree`. `content()`, `pathsIn` and `tipPaths` already follow the rule
  and keep their names. This is a rename only: every call site keeps the
  semantics it has now (include or exclude), even where the other one might
  seem better. Switching semantics is not in scope.
- **Seeding primitive:** `storeLegacyRawAttachmentAndSnapshot` →
  `storeAcceptedAttachmentAndSnapshot`, documented as "seeds the given accepted
  bytes". `storeFolderAttachmentAndSnapshot` (payload → pointer) keeps its name.
  Raw-subject tests pass raw bytes to the primitive.
- **F7 takes the smaller option:** keep `accept_lfs_attachment_tip_for_testability`
  (it alone supplies the obsolete payload and the returned oid that
  `expectCheckoutLfsCacheHoldsOnlyTip` compares). Drop the forced
  representation and the binding lookup, and call the metadata-preserving
  `resetHistory(notebook, time)`. Folding it into `put_notebook_file_for_testability`
  was considered and rejected. `put` would need a new obsolete-payload field
  and an oid response, or the E2E would have to compute a SHA-256, which
  means more code in total. The request/response schema does not change,
  so no API regeneration is expected. Regenerate only if the OpenAPI output
  differs.

## Slices

Slice target about 5 minutes, hard limit 10, excluding test run time (an
external wait). Each slice ends green. Backend proof command (one `--tests`
pattern per run):
`unset SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL; CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests 'com.odde.donut.controllers.NotebookGit*'`

### 1. LFS seeding no longer goes through a raw-named helper

Type: Structure (retrospective correction F3)
Status: done
Proof: backend `NotebookGit*` green; grep for `storeLegacyRawAttachmentAndSnapshot`
under `backend/` returns nothing.
Accepted: `NotebookGit*` 428 tests, 0 failures; grep empty. Only the base,
SizeAdmission (4) and AttachmentMetadata (1) called the primitive.

Change: rename the primitive in `NotebookGitWebContentControllerTestBase`
to `storeAcceptedAttachmentAndSnapshot` and update its javadoc. Update the
callers: the base (1), `NotebookGitAttachmentSizeAdmissionControllerTest`
(4) and `NotebookGitAttachmentMetadataControllerTest` (1). Those raw-subject
callers still create `createLegacyRawNotebook` and pass raw bytes.
Weakness removed: story 19 can no longer break LFS seeding by deleting
"legacy raw" helpers.

### 2. Test Git readers say whether they include reserved metadata

Type: Structure (retrospective correction F4)
Status: planned
Proof: backend `NotebookGit*` green, then `--tests 'com.odde.donut.services.notebookGit.*'`
green; grep of the backend tests for `readTreeEntries`,
`fetchTipTreeEntries` and `tipContent` returns nothing. `AcceptedTip` has no
`entries` component.

Change: apply the naming rule in `GitBundleTestReader` (renames only;
javadoc says the rule once, on the class). About 55 call sites across about
12 test files: `readTreeEntries` 13/9 files, `readTreeEntriesWithMetadata`
11/6, `fetchTipTreeEntries` 4/3, `AcceptedTip.entries()` about 11 (in
`NotebookGitAttachment{LfsPublication,LocalChange,Publication}ControllerTest`),
`tipContent()` 16/8. Scope `.entries()` edits to `AcceptedTip` receivers. The
`LearningSession*` tests call an unrelated `.entries()`. The retrospective's
count of about 70 included those calls.
Weakness removed: one name meant "with metadata" in one place and "without"
in another.

### 3. One LFS save-cost test, and no fixture-only test

Type: Structure (retrospective correction F6)
Status: planned
Proof: `--tests 'com.odde.donut.controllers.NotebookGitWebContentSaveCost*'`
and `--tests 'com.odde.donut.controllers.NotebookGitAttachmentMetadata*'`
green; grep shows no `setAttachmentRepresentation` in
`NotebookGitWebContentSaveCostControllerTest` and no
`lfsRepresentationFixture` under `backend/`.

Change: fold `noteOnlySaveOnLfsNotebookDoesNotReadPayloadsOrRewriteObjects`
into `savingContentInALargeNotebookWithAttachmentsDoesNotQueryAttachmentsOrPortableTreeRows`
(rename it to cover both, e.g. `noteSaveInALargeLfsNotebookLeavesAttachmentsUntouched`).
Before the save, reset the in-memory content store access counts. After
the save, keep the existing query and parent assertions, and add
`getCalls() == 0`, `storeCalls() == 0`, and that a seeded attachment's
exact-tree entry still equals `pointerFor(large, payload)`. Delete the
second test, its stale javadoc, the hand-set representation, and imports
that become unused. Delete
`NotebookGitAttachmentMetadataControllerTest.lfsRepresentationFixtureProjectsPointerBytesWithoutPublication`.
Coverage check: "no NotebookAttachment query", "content store not read or
written", "pointer unchanged" and "history advanced by one" are all still
asserted. The fixture's pointer projection is covered by every
`storeFolderAttachmentAndSnapshot` user that asserts pointers (slice 3 of
plan 026).

### 4. E2E accepted-LFS-tip seeding keeps the notebook's own representation

Type: Structure (retrospective correction F7)
Status: planned
Proof: `SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_lfs.feature`
green (11/11 per plan 026). `NotebookLfsTestabilityController` has no
`setAttachmentRepresentation` and no `initialMetadata`. Backend compiles
(full suite at the end).

Change: in `acceptLfsAttachmentTipForTestability`, remove the binding lookup
and the forced LFS representation. Call `notebookGitCutoverService.resetHistory(notebook, time)`.
Update its javadoc and the E2E wrapper's comment in
`e2e_test/start/testabilityNotebookGit.ts`, which still says "switching the
notebook to LFS". Remove unused imports and autowired fields.
Weakness removed: seeding that could silently convert a raw notebook and
duplicate the product's default metadata.
Wrap-up: full backend suite green (`pnpm backend:test:worktree` with no
pattern).

## Remaining concerns

- Slice 2 is the largest (about 55 mechanical edits). It fits the target only
  if the edits are scripted per file and scoped as noted. If it runs past
  10 minutes, split it by reader (static readers, then record accessors).
- Slice 3's merged test switches the attachment's source from proposal
  publication to seeding. Plan 026's decision says the seeding follows the
  product. Content-store counts are reset after seeding, so the seeding's
  own store calls are not counted.
