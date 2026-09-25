# Finish the single attachment representation

**Identity:** quick/031-finish-single-attachment-representation/PLAN.md
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"d6b64813e8f8271baeb867cffbd6a62cb573b7d89a0323c489b0cb0b210eb705"}}
```

## Source

- Kind: bounded retrospective correction; no seed.
- Corrects the execution of SEED-035#story-19 ("Remove the legacy raw file
  storage"), plan `.planning/quick/029-remove-raw-file-storage/PLAN.md`
  (the story and plan stay recoverable from Git once wrapped up). Reviewed
  commits on `story/remove-raw-file-storage`: `0284ea7f52`, `0d84006f7d`,
  `9ad0b9bf9f`, `e9cbbc6547`, `6b9c39cb3d`, `aba0dc2509`.
- Governing direction: North Star
  [one attachment content model](../../NORTH-STAR.md#one-attachment-content-model),
  "Every notebook uses LFS": there is one representation, so no code asks how
  a notebook stores its files. Story 19 removed the representation from the
  code paths; this correction removes what still describes or tests the old
  raw/LFS split.
- Findings re-verified at `aba0dc2509` (2026-09-25):
  1. `docs/notebook-git-attachments.md` still describes two representations:
     lines 12-14 ("database-byte projection … awaiting that transition"),
     22-24 ("the file's bytes in a raw notebook, or its Git LFS pointer in an
     LFS notebook"), 58-61 ("The notebook's attachment representation … a raw
     notebook serves its content unchanged") and 80-91 (admission of "raw-Git
     attachment blobs"; "the intermediate refusal stays in force … not part of
     the current raw-Git admission"). Code: `NotebookAttachmentFile` treats
     every non-empty row as a pointer and fails loudly otherwise;
     `NotebookGitAttachmentSizeAdmission` refuses a non-pointer ("must be a Git
     LFS pointer or empty file") and allows an omitted oversized intermediate
     when the tip is valid (`NotebookGitAttachmentSizeAdmissionHistoryControllerTest.allowsOmittedOversizedIntermediateWhenTipCorrectionIsValid`).
     `docs/notebook-git-lfs.md` already states the current rules.
  2. `NotebookGitCutoverService` keeps a public
     `resetHistory(Notebook, Instant, List<PortableTreeEntry>)` ("when demoting
     a notebook to raw") and its only helper `findOrCreateBinding`; no main or
     test code calls them (all callers use `resetHistory(Notebook, Instant)`).
     Its class Javadoc still says reset preserves "that binding's
     representation". `NotebookGitLfsPointer` Javadoc says "raw bindings keep
     pointer-looking bytes as legacy payload"; `NotebookGitBindingAssertions`
     Javadoc asserts "LFS representation".
  3. Attachment controller tests still split along the old raw/LFS line:
     - `NotebookGitAttachmentCreationControllerTest` holds no creation test:
       `historyResetPreservesLfsAttributesAndRetainedContentObjects` overlaps
       `NotebookGitHistoryResetControllerTest.resetRestartsHistoryCarryingTheRootFilesTheNotebookCurrentlyHolds`
       (it adds the content-store and projection checks), and
       `olderClientRawAttachmentPayloadIntoLfsNotebookIsRefusedWithoutStoringBytes`
       is an admission rule.
     - `NotebookGitAttachmentPublicationControllerTest` (case-distinct root
       files, exact tip, next web save) overlaps
       `NotebookGitAttachmentLfsPublicationControllerTest.lfsPublicationAcceptsRootNestedAndEmptyPointersInGitAndProjection`.
     - `NotebookGitAttachmentLfsPublicationControllerTest` also holds
       `lfsExactLimitTipPointerIsAccepted` and
       `lfsMixedRefusalLeavesAcceptedHeadProjectionAndLearningUnchanged`,
       admission rules kept apart from
       `NotebookGitAttachmentSizeAdmissionControllerTest`.
     - `NotebookGitAttachmentMetadataControllerTest.gitattributesAreReservedMetadataNotAttachmentsAndSurviveWebSaveAndReset`
       never resets and repeats one assertion; no test proves reset keeps an
       authored `.gitattributes` (the service Javadoc promises it).
     - The `Lfs` prefix distinguishes nothing now:
       `NotebookGitAttachmentLfsWebContinuityControllerTest`
       (`lfsWebEditsAndFolderOps…`), `NotebookGitAttachmentLfsRawHistoryControllerTest`,
       `NotebookGitWebContentSaveCostControllerTest.noteSaveInALargeLfsNotebook…`,
       and the helpers `committedOnLfs` (`NotebookGitControllerTestBase`) and
       `proposedOnLfs` (`NotebookGitAttachmentLocalChangeControllerTest`).
  4. The CLI still asks whether a checkout uses LFS:
     `checkoutUsesLfs` (`cli/src/commands/notebook/notebookLfsLocal.ts:27`,
     true only when `.gitattributes` contains `filter=lfs`) gates
     `uploadRequiredLfsObjectsBeforeProposal` (`notebookPublishLfs.ts:18`;
     Javadoc line 10 "Raw checkouts are untouched") and
     `fillInCurrentLfsFilesIfNeeded` (`notebookLfsFillIn.ts:18`, returns
     whether it ran; callers `nonInteractiveCli.ts:163` for pull and
     `notebookAcquisition.ts:141-148` for clone, which removes `origin` again
     only when it ran). Tests pinning the raw path:
     `notebookPublish.lfs.test.ts:50` "raw checkout publishes without Git LFS
     upload" and `notebookPull.lfs.test.ts:196` "a legacy checkout pulls
     without Git LFS". The local raw-payload refusal
     (`notebookPublishLfsSelection.ts:97`, asserted at
     `notebookPublish.lfsFailure.test.ts:61`, test named "… in an LFS
     notebook" at line 42) adds "when the notebook uses LFS", unlike the
     backend's `rawPayloadRefusal`
     (`NotebookGitAttachmentSizeAdmission.java:173-176`: "Attachment \"<path>\"
     must be a Git LFS pointer or empty file."). Docs tie Git LFS to
     attachment work only: `docs/notebook-git-lfs.md:25` ("Require standard
     Git LFS for supported local attachment workflows"), line 53 ("Local
     attachment work requires Git LFS") and
     `docs/notebook-git-synchronization.md:26` ("local attachment workflows
     require Git LFS").

## Goal and scope

Beneficiary: maintainers. Outcome: the documentation, production code, CLI and
attachment tests describe one attachment representation (a Git LFS pointer or
an empty file), so nobody reading them looks for a raw path that no longer
exists. Fewer tests keep the same behavioral coverage.

Backend and web behavior is preserved; no API, schema or E2E change. The one
intended product change is in the CLI (owner decision below): clone, pull and
publish treat every notebook checkout as LFS, so they need Git LFS installed
even when only notes change, and the CLI's raw-payload refusal reads like the
server's.

Included: the four stale passages in `docs/notebook-git-attachments.md`; the
dead cutover-service overload and helper and the three stale comments; the
attachment controller test consolidation and renames listed in the slices; the
CLI raw-checkout gate, its two raw-path tests, the CLI refusal copy, and the
three Git LFS requirement passages in finding 4.

Excluded:
- ADR 0002 ("Local attachment workflows use standard Git LFS") stays as
  written: it does not say notes-only work avoids Git LFS, so it does not
  conflict with the owner decision.
- The CLI's history walk that ignores raw blobs in accepted history
  (`notebookPublishLfsSelection.ts:103`, test "accepted raw history converted
  to LFS does not block publishing a pointer") stays: SEED-035#story-19
  "Keep" — raw blobs in accepted history stay readable and publishable.
- E2E CLI features: every checkout they use is cloned from a server notebook,
  whose tip always carries the initial LFS `.gitattributes`
  (`NotebookGitAttributes`; `cli_notebook_git_history_reset.feature:28-32`),
  so they already take the LFS path and do not change.
- `assertInitialLfsRootCommitBinding` and
  `resetOfANotebookWithoutBindingCreatesABindingWithInitialLfsAttributes` keep
  their names: "LFS" there names the initial LFS `.gitattributes` content, not
  a representation.
- The historical story, plan 029 and North Star text stay as written.
- Other attachment test classes (Independence, LocalChange,
  SizeAdmissionHistory) change only by the helper rename.

Assumptions:
- `NotebookGitHistoryResetControllerTest` extends
  `NotebookGitControllerTestBase`, which has `notebookAttachmentContent` but
  not `notebookAttachmentRepository`; the merged reset test autowires the
  repository (or reads the projection through `NotebookLiveProjectionTestReader`).
- Mixed-refusal proof helpers (`learnedTracker`,
  `assertShownContentAndRetainedLearning`, `countNativeObjectStoreRows`) are
  reachable from `NotebookGitAttachmentSizeAdmissionTestSupport`, since
  `NotebookGitAttachmentLfsPublicationControllerTest` already extends it.

## Outside-in proof (key examples)

Surviving coverage after the correction (each at the controller boundary):

| Example | Owning test after the correction |
| --- | --- |
| A notebook with root files, content objects and an authored `.gitattributes` is reset → one parentless commit holds the same pointers and `.gitattributes`; the content store still holds each payload; the projection still lists each file | `NotebookGitHistoryResetControllerTest.resetRestartsHistoryCarryingTheRootFilesTheNotebookCurrentlyHolds` (slice 3) |
| Publishing notes plus case-distinct, nested and empty files → the accepted tip is exactly that tree on the prior head, the projection lists the files, and the next web note save keeps them | `NotebookGitAttachmentPublicationControllerTest` (slice 5) |
| An attachment exactly at the limit is accepted, and two within-limit files whose sum exceeds it are accepted | `NotebookGitAttachmentSizeAdmissionControllerTest` (slice 4) |
| A proposal with an over-limit pointer, a missing object, a corrupt pointer, or a non-pointer payload is refused with a message naming the path and reason, leaving head, projection, content store and learning unchanged | `NotebookGitAttachmentSizeAdmissionControllerTest` (slice 4) |
| `.gitattributes` is reserved metadata, not an attachment, and survives a web save | `NotebookGitAttachmentMetadataControllerTest` (slice 3) |
| A notes-only publish from a checkout without LFS `.gitattributes` still publishes (after preparing the LFS endpoint, uploading nothing) | existing `notebookPublish.*.suite.ts` runs via `notebookPublish.test.ts`, whose checkouts carry no `.gitattributes` (slice 7) |
| Cloning a checkout without LFS `.gitattributes` still configures the notebook LFS endpoint and fills in the current pointer files; without Git LFS it fails naming "Git LFS is required" and leaves the destination untouched | `notebookAcquisition.lfs.test.ts`, with its fixture no longer writing `.gitattributes` (slice 8) |
| Publishing a raw attachment is refused locally with the server's wording (`must be a Git LFS pointer or empty file.`), nothing uploaded or posted | `notebookPublish.lfsFailure.test.ts` (slice 9) |

Focused backend command (linked worktree isolates the test DB automatically):

```
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests '<pattern>'
```

Focused CLI command (Vitest file filters; the nix shell provides `git-lfs`,
and CI's `pnpm cli:test` already relies on the runner's Git LFS for the
existing LFS tests):

```
CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run <file-filter>...
```

Isolated probe (2026-09-25, Git LFS 3.8.0): in a fresh repo with a Markdown
note and a raw `data.bin`, no `.gitattributes`, `lfs.url` pointing at the
unreachable `http://127.0.0.1:9/...`, running `git lfs install --local`, then
`GIT_TERMINAL_PROMPT=0 GIT_LFS_SKIP_SMUDGE=1 git lfs fetch origin` and
`git lfs checkout` both exit 0 in under 0.1 s with no network request and
leave the worktree clean. So an ungated fill-in on a checkout with no pointers
needs no server, and the existing real-Git pull and clone tests stay offline.

## Slices

### 1. Attachment docs describe one pointer representation
Type: Structure
Status: planned
Proof: `grep -nE "raw notebook|raw-Git|attachment representation|awaiting that transition" docs/notebook-git-attachments.md`
returns nothing; each rewritten passage matches the code named in finding 1.

Rewrite the four passages in `docs/notebook-git-attachments.md` for the single
representation: a row holds the file's Git LFS pointer, or empty content for an
empty file; a download serves the stored object named by the pointer (link to
[notebook-git-lfs.md](../../../docs/notebook-git-lfs.md) for the missing-object
and empty-file rules instead of restating them); publication admission follows
the LFS acceptance rule there (pointer or empty only, 10 MiB inclusive, tip and
within-limit intermediates required, oversized intermediate-only payloads may be
absent, same-notebook accepted digests grandfathered). Drop the "database-byte
projection … awaiting that transition" sentence. Keep every other statement.

### 2. History reset has one entry point and no raw comments remain
Type: Structure
Status: planned
Proof: backend compiles;
`NotebookGitCutoverServiceTest`, `NotebookGitHistoryResetControllerTest`,
`NotebookGitLfsPointerTest`, `NotebookCrudControllerTest` and
`CircleControllerTest` green.

Delete `NotebookGitCutoverService.resetHistory(Notebook, Instant,
List<PortableTreeEntry>)` and `findOrCreateBinding`; state in the class Javadoc
that reset preserves the binding's accepted metadata (no "representation").
Correct `NotebookGitLfsPointer`'s Javadoc (drop the raw-bindings sentence) and
`NotebookGitBindingAssertions`'s ("initial LFS `.gitattributes`", not "LFS
representation"). No behavior change; weakness removed: dead demotion API.

### 3. One reset test proves files, content objects and authored metadata survive
Type: Structure
Status: planned
Proof: `NotebookGitHistoryResetControllerTest` and
`NotebookGitAttachmentMetadataControllerTest` green; the reset example in the
proof table is asserted in one test.

Extend `resetRestartsHistoryCarryingTheRootFilesTheNotebookCurrentlyHolds`:
publish an authored `.gitattributes` with the root files, then after reset
assert the exact tree (authored `.gitattributes`, pointers, note), the
content store still returning each payload, and the projection listing each
file (taken from Creation's reset test). Delete
`NotebookGitAttachmentCreationControllerTest.historyResetPreservesLfsAttributesAndRetainedContentObjects`.
Rename the metadata test to
`gitattributesAreReservedMetadataNotAttachmentsAndSurviveWebSave` and drop
its repeated `.gitattributes` assertion (keep `assertAcceptedTreeMatchesTheFullAssembly`).

### 4. Attachment admission rules live in one test class
Type: Structure
Status: planned
Proof: `NotebookGitAttachmentSizeAdmissionControllerTest` green, asserting the
over-limit (`assertOversizedRefusal` or path + size), missing, corrupt and
"must be a Git LFS pointer" refusals and the exact-limit acceptance.

Into `NotebookGitAttachmentSizeAdmissionControllerTest`:
- make one file in `acceptsTwoWithinLimitAttachmentsWhoseCombinedSizeExceedsLimit`
  exactly `LIMIT` bytes (renamed to say the limit is inclusive and per file),
  replacing `lfsExactLimitTipPointerIsAccepted`;
- move `lfsMixedRefusalLeavesAcceptedHeadProjectionAndLearningUnchanged`
  (renamed without `lfs`), adding the non-pointer payload as a fourth refused
  proposal with its message assertion, replacing Creation's raw-payload test.
Delete `NotebookGitAttachmentCreationControllerTest` (now empty) and the two
moved tests from `NotebookGitAttachmentLfsPublicationControllerTest`.

### 5. One publication test proves the exact accepted tree
Type: Structure
Status: planned
Proof: `NotebookGitAttachmentPublicationControllerTest` green; the publication
example in the proof table is asserted in one test.

Merge `lfsPublicationAcceptsRootNestedAndEmptyPointersInGitAndProjection` into
`NotebookGitAttachmentPublicationControllerTest`'s single test: publish a note,
case-distinct `Diagram.png`/`diagram.png`, `tools/cache/nested.bin` and
`empty.bin`; assert the exact tip on the prior head, the projection
(`NotebookLiveProjectionTestReader.attachmentTree`), and the exact tree after
the next web note save. Drop the redundant `reference.json`. Delete
`NotebookGitAttachmentLfsPublicationControllerTest`.

### 6. Test names no longer mention a representation choice
Type: Structure
Status: planned
Proof: all `NotebookGitAttachment*ControllerTest`,
`NotebookGitHistoryResetControllerTest` and
`NotebookGitWebContentSaveCostControllerTest` green;
`grep -rn "OnLfs\|void lfs\|AttachmentLfs\|LargeLfsNotebook" backend/src/test`
returns nothing.

Rename `NotebookGitAttachmentLfsWebContinuityControllerTest` →
`NotebookGitAttachmentWebContinuityControllerTest` (method `webEditsAndFolderOps…`),
`NotebookGitAttachmentLfsRawHistoryControllerTest` →
`NotebookGitAttachmentRawHistoryControllerTest`,
`noteSaveInALargeLfsNotebookLeavesAttachmentsUntouched` →
`noteSaveInANotebookWithManyFilesLeavesAttachmentsUntouched` (or equivalent),
`committedOnLfs` → `asCommitted`, `proposedOnLfs` → `asProposed`. Mechanical
IDE-style renames only.

### 7. Publishing from any checkout takes the Git LFS path
Type: Behavior
Status: planned
Pre-condition: a bound checkout whose tip has no LFS `.gitattributes`.
Trigger: `donut notebook publish`.
Postcondition: publish prepares the authenticated LFS checkout (Git LFS
required, notebook `lfs.url` recorded) and uploads the required objects before
submitting; with only notes changed it uploads nothing and publishes as
before.
Proof:
`CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPublish tests/notebookPull.test.ts`
green (the publish suites and the pull rebase/conflict suites that publish all
use checkouts without `.gitattributes`, so they now run the LFS preparation).

Drop the `checkoutUsesLfs` check from
`uploadRequiredLfsObjectsBeforeProposal` (`notebookPublishLfs.ts`) and correct
its Javadoc (no "For an LFS checkout", no "Raw checkouts are untouched").
Delete `notebookPublish.lfs.test.ts` "raw checkout publishes without Git LFS
upload"; keep "accepted raw history converted to LFS does not block publishing
a pointer". `checkoutUsesLfs` stays for fill-in until slice 8.

### 8. Clone and pull fill in every checkout through Git LFS
Type: Behavior
Status: planned
Pre-condition: a clone staging checkout or bound checkout whose tip has no
LFS `.gitattributes`.
Trigger: `donut notebook clone` or `donut notebook pull`.
Postcondition: the command prepares the authenticated LFS checkout and fills
in the current files; without Git LFS it fails with "Git LFS is required to
receive this notebook's attachments…" and a clone leaves the destination
untouched. The docs state that the CLI needs Git LFS for every notebook
clone, pull and publish, including notes-only work.
Proof:
`CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookAcquisition tests/notebookClone tests/notebookPull`
green, with `writeLfsCheckoutPointer` (`notebookAcquisition.testHelpers.ts`)
no longer writing `.gitattributes`, so the three `notebookAcquisition.lfs.test.ts`
tests (endpoint and fill-in, missing Git LFS, failed fill-in) prove the path
without LFS attributes; `grep -rn "checkoutUsesLfs\|IfNeeded" cli/src` returns
nothing.

Drop the check from the fill-in, rename it `fillInCurrentLfsFiles` returning
nothing, and update both callers: pull (`nonInteractiveCli.ts`) and clone
(`notebookAcquisition.ts`, which now always removes the placeholder `origin`
after fill-in; correct its Javadoc "When the tip enables Git LFS"). Delete
`checkoutUsesLfs` and its now-unused `fs`/`path` imports, remove
`LFS_ATTRIBUTES` from the acquisition test helpers, and correct
`prepareAuthenticatedLfsCheckout`'s Javadoc ("Prepares the checkout", not "an
LFS checkout"). Delete `notebookPull.lfs.test.ts` "a legacy checkout pulls
without Git LFS". Docs: `docs/notebook-git-lfs.md` Clients bullet (line 25)
and Consequences (line 53) say the CLI requires standard Git LFS for every
notebook clone, pull and publish, including notes-only work (owner decision
2026-09-25); `docs/notebook-git-synchronization.md:26` says local notebook
workflows require Git LFS.

### 9. The CLI refuses a raw attachment in the server's words
Type: Behavior
Status: planned
Pre-condition: a checkout with an unpublished commit adding a raw (non-pointer)
attachment.
Trigger: `donut notebook publish`.
Postcondition: it fails before upload or submission with `Attachment "<path>"
must be a Git LFS pointer or empty file.`, the same text as the backend's
`rawPayloadRefusal`; nothing is uploaded or posted.
Proof:
`CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPublish.lfsFailure.test.ts`
green, asserting the full sentence ending `…or empty file.`

Drop "when the notebook uses LFS" from `requireLfsPointer`
(`notebookPublishLfsSelection.ts:97`); rename the test to "rejects publishing
a raw attachment" and assert the new sentence.

## Current decisions

- Consolidation merges assertions into surviving tests rather than adding
  tests; the only new assertion is reset keeping an authored `.gitattributes`,
  which the existing service contract already promises.
- **Owner decision (Terry Yin, 2026-09-25):** remove the CLI's raw-checkout
  path in this correction. The CLI always treats a notebook checkout as LFS,
  so clone, pull and publish need Git LFS installed even when only notes
  change; this is accepted. Every server notebook's tip already carries the
  LFS `.gitattributes`, so only a checkout whose attributes were removed
  locally, or a test fixture, behaves differently.
- The CLI slices come after the backend slices because they are independent;
  publish (7) and fill-in (8) are separate proof loops, and `checkoutUsesLfs`
  is deleted in slice 8 once its last caller goes.
- No new CLI test: slice 8 proves the ungated path by removing
  `.gitattributes` from the existing LFS acquisition fixture, and the existing
  publish, pull and clone suites already use checkouts without it.

## Learnings

None yet.
