# Consumer trace

## Effect terms searched

| Term | Origin |
| --- | --- |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/execution-evidence.md` | changed path |
| `execution-evidence.md` | basename of `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/execution-evidence.md` |
| `cli/tests/notebookPublish.largeTree.suite.ts` | changed path |
| `notebookPublish.largeTree.suite.ts` | basename of `cli/tests/notebookPublish.largeTree.suite.ts` |
| `cli/tests/notebookPublish.test.ts` | changed path |
| `notebookPublish.test.ts` | basename of `cli/tests/notebookPublish.test.ts` |
| `describeNotebookPublishLargeTree` | supplied |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/execution-evidence` | import path of `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/execution-evidence.md` |
| `execution-evidence` | import path of `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/execution-evidence.md` |
| `cli/tests/notebookPublish.test` | import path of `cli/tests/notebookPublish.test.ts` |
| `notebookPublish.test` | import path of `cli/tests/notebookPublish.test.ts` |
| `cli/tests/notebookPublish.largeTree.suite` | import path of `cli/tests/notebookPublish.largeTree.suite.ts` |
| `notebookPublish.largeTree.suite` | import path of `cli/tests/notebookPublish.largeTree.suite.ts` |
| `acceptedContent` | declared in `cli/tests/notebookPublish.largeTree.suite.ts` |
| `editedContent` | declared in `cli/tests/notebookPublish.largeTree.suite.ts` |
| `appendNotes` | declared in `cli/tests/notebookPublish.largeTree.suite.ts` |

## Scope

Whole checkout at `9b6dd33f4fb6`, including uncommitted and untracked files: 4198 tracked and untracked non-ignored files searched with fixed-string, case-insensitive matching.

Changed files not searched: `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/execution-evidence.md`, `cli/tests/notebookPublish.largeTree.suite.ts`, `cli/tests/notebookPublish.test.ts`.

## Located consumers

Kinds are path hints for inspection; each file appears once. Fill every Disposition with `run`, `reused` or `excluded: <reason the effect does not reach its boundary>`.

### Production

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitDiagnosis.java` | `acceptedContent` | 90: `byte[] acceptedContent,` | excluded: acceptedContent/editedContent is an unrelated local variable or stable diagnosis field; this test-only fixture adds no production/API contract change |
| `cli/src/commands/notebook/notebookDiagnosis.ts` | `acceptedContent` | 49: `const before = Buffer.from(difference.acceptedContent ?? '', 'base64')` | excluded: acceptedContent/editedContent is an unrelated local variable or stable diagnosis field; this test-only fixture adds no production/API contract change |
| `open_api_docs.yaml` | `acceptedContent` | 6583: `acceptedContent:` | excluded: acceptedContent/editedContent is an unrelated local variable or stable diagnosis field; this test-only fixture adds no production/API contract change |
| `packages/generated/donut-backend-api/types.gen.ts` | `acceptedContent` | 1324: `acceptedContent?: string \| null;` | excluded: acceptedContent/editedContent is an unrelated local variable or stable diagnosis field; this test-only fixture adds no production/API contract change |

### Test

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitDiagnosisControllerTest.java` | `acceptedContent` | 200: `assertThat(difference.acceptedContent(), equalTo(before));` | excluded: acceptedContent/editedContent is an unrelated local variable or stable diagnosis field; this test-only fixture adds no production/API contract change |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitMixedEditingControllerTest.java` | `editedContent` | 37: `String editedContent = CREATED_PREFIX + "first web edit";` | excluded: acceptedContent/editedContent is an unrelated local variable or stable diagnosis field; this test-only fixture adds no production/API contract change |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitNoteCreationControllerTest.java` | `acceptedContent` | 101: `void matchingAcceptedContentAppendsOnlyTheNewTitleOnlyRootFile() throws Exception {` | excluded: acceptedContent/editedContent is an unrelated local variable or stable diagnosis field; this test-only fixture adds no production/API contract change |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitProposalFolderReadmeEditControllerTest.java` | `acceptedContent` | 125: `void rejectsAnInvalidFolderReadmeEditWithoutMutatingAcceptedContent() throws Exception {` | excluded: acceptedContent/editedContent is an unrelated local variable or stable diagnosis field; this test-only fixture adds no production/API contract change |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitProposalRelocationReferrerControllerTest.java` | `editedContent` | 79: `void leavesPathQualifiedReferringBodyAuthoredWhenPublishingAnInferredRelocationWithEditedContent()` | excluded: acceptedContent/editedContent is an unrelated local variable or stable diagnosis field; this test-only fixture adds no production/API contract change |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitProposalRenameControllerTest.java` | `editedContent` | 128: `void acceptsASameParentRenameWithEditedContentRetainingNoteAndLearningIdentities()` | excluded: acceptedContent/editedContent is an unrelated local variable or stable diagnosis field; this test-only fixture adds no production/API contract change |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitProposalRenameReferrerControllerTest.java` | `editedContent` | 69: `void leavesReferringBodyAuthoredWhenPublishingAnInferredRenameWithEditedContent()` | excluded: acceptedContent/editedContent is an unrelated local variable or stable diagnosis field; this test-only fixture adds no production/API contract change |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitProposalTreeShapeControllerTest.java` | `acceptedContent` | 109: `void rejectsAnInvalidRootReadmeEditWithACompanionNoteEditWithoutMutatingAcceptedContent()` | excluded: acceptedContent/editedContent is an unrelated local variable or stable diagnosis field; this test-only fixture adds no production/API contract change |
| `backend/src/test/java/com/odde/donut/services/notebookGit/NotebookGitTreeEncoderTest.java` | `acceptedContent` | 152: `void derivingAChangeThatInsertsAnAttachmentPutsItsAcceptedContentAtItsPath() throws Exception {` | excluded: acceptedContent/editedContent is an unrelated local variable or stable diagnosis field; this test-only fixture adds no production/API contract change |
| `cli/tests/notebookDiagnosis.test.ts` | `notebookPublish.test` | 15: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPublish.ancestry.suite.ts` | `notebookPublish.test` | 19: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPublish.ancestryTip.suite.ts` | `notebookPublish.test` | 21: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPublish.binding.suite.ts` | `notebookPublish.test` | 11: `import { stubFetchWithAcceptedBundleFrom } from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPublish.lfs.test.ts` | `notebookPublish.test` | 31: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPublish.lfs.testHelpers.ts` | `notebookPublish.test` | 15: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPublish.lfsFailure.test.ts` | `notebookPublish.test` | 22: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPublish.readiness.suite.ts` | `notebookPublish.test` | 19: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPublish.rebasedRejection.suite.ts` | `notebookPublish.test` | 19: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPublish.resolvedContinuation.suite.ts` | `notebookPublish.test` | 24: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPublish.resolvedContinuationRejection.suite.ts` | `notebookPublish.test` | 30: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPublish.submission.suite.ts` | `notebookPublish.test` | 21: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPull.acceptedChanges.suite.ts` | `notebookPublish.test` | 13: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPull.acceptedHistory.suite.ts` | `notebookPublish.test` | 13: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPull.concurrentChange.suite.ts` | `notebookPublish.test` | 12: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPull.contentBatch.suite.ts` | `notebookPublish.test` | 21: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPull.contentBatch.testHelpers.ts` | `notebookPublish.test` | 6: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPull.fastForward.pathReceipt.ts` | `notebookPublish.test` | 12: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPull.fastForward.suite.ts` | `notebookPublish.test` | 12: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPull.lfs.test.ts` | `notebookPublish.test` | 16: `import { cloneAsBoundCheckout } from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPull.lfs.testHelpers.ts` | `notebookPublish.test` | 15: `import { cloneAsBoundCheckout } from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPull.localCandidate.suite.ts` | `notebookPublish.test` | 10: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPull.localCandidate.testHelpers.ts` | `notebookPublish.test` | 6: `import { cloneAsBoundCheckout } from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPull.pathOverlap.suite.ts` | `notebookPublish.test` | 10: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPull.rebase.suite.ts` | `notebookPublish.test` | 13: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPull.rebase.testHelpers.ts` | `notebookPublish.test` | 8: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookPull.testHelpers.ts` | `notebookPublish.test` | 15: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `cli/tests/notebookRecovery.test.ts` | `notebookPublish.test` | 15: `} from './notebookPublish.testHelpers.js'` | run: complete CLI suite preserves shared Publish entry, ancestry/readiness/Pull and external-backend fixture behavior; no helper implementation changed |
| `frontend/tests/pages/NoteShowPage.imageUpload.spec.ts` | `editedContent` | 26: `const editedContent = "---\nimage: old.png\n---\n\nHi there"` | excluded: acceptedContent/editedContent is an unrelated local variable or stable diagnosis field; this test-only fixture adds no production/API contract change |

### Test support or stand-in

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `packages/donut-test-fixtures/src/NotebookGitDiagnosisBuilder.ts` | `acceptedContent` | 26: `acceptedContent:` | excluded: acceptedContent/editedContent is an unrelated local variable or stable diagnosis field; this test-only fixture adds no production/API contract change |

### Journey or feature

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `e2e_test/start/pageObjects/conversationAboutNotePage.ts` | `acceptedContent` | 116: `expectAcceptedContentCorrection(content: string) {` | excluded: acceptedContent/editedContent is an unrelated local variable or stable diagnosis field; this test-only fixture adds no production/API contract change |
| `e2e_test/step_definitions/ai.ts` | `acceptedContent` | 181: `.expectAcceptedContentCorrection(content)` | excluded: acceptedContent/editedContent is an unrelated local variable or stable diagnosis field; this test-only fixture adds no production/API contract change |

### Guidance reader

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/consumer-trace-admission.md` | `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/execution-evidence.md`, `execution-evidence.md`, `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/execution-evidence`, `execution-evidence` | 11: `` \| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/execution-evidence.md` \| changed path \| `` | excluded: parent-owned planning/evidence reference, not an executable consumer of the added test suite |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/consumer-trace-cli-recovery.md` | `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/execution-evidence.md`, `execution-evidence.md`, `cli/tests/notebookPublish.test.ts`, `notebookPublish.test.ts`, `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/execution-evidence`, `execution-evidence`, `cli/tests/notebookPublish.test`, `notebookPublish.test` | 76: `` \| `cli/tests/notebookPublish.test.ts` \| `rebased` \| 6: `import { describeNotebookPublishRebasedRejection } from './notebookPublish.rebasedRejection.suite.js'` … `` | excluded: parent-owned planning/evidence reference, not an executable consumer of the added test suite |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/consumer-trace-recovery-journey.md` | `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/execution-evidence.md`, `execution-evidence.md`, `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/execution-evidence`, `execution-evidence` | 127: `` \| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/execution-evidence.md` \| `diagnosis` \| 38: `At trigger owner requests a diagnostic … `` | excluded: parent-owned planning/evidence reference, not an executable consumer of the added test suite |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/PLAN.md` | `execution-evidence.md`, `execution-evidence` | 53: `[Retained observations and unverified remainders](execution-evidence.md#decisive-premises-and-limits).` | excluded: parent-owned planning/evidence reference, not an executable consumer of the added test suite |
| `DearDough.md` | `execution-evidence` | 292: `Former local code: DD-274. Consumer discovery treated execution-evidence Identity lines and non-story anchors as canonical work, producing duplicate-home and u…` | excluded: parent-owned planning/evidence reference, not an executable consumer of the added test suite |

## Sufficient proof and fixture qualification

The added suite is registered in the existing notebookPublish.test.ts entry because its ancestry leak-detection fixtures require shared staging-dir tests to remain in one worker entry. The CLI run uses real Git over a measured >1MiB accepted ls-tree listing, exactly three committed typed Markdown edits, and only backend fetch mocked. It observes submitted native head, accepted parent, local main/clean work and exact changed bodies. Existing Publish/Pull suites remain real consumers of the shared entry and are selected by the complete CLI proof. No production code, helper contract, API or generated file changes. The original11000-file fixture exceeded5000ms; a smaller count of longer valid filenames preserves the actual full-listing threshold without a timeout/config change.

## Terminal proof and scratch discrimination

VITEST_MAX_WORKERS=2 CURSOR_DEV=true nix develop -c pnpm cli:test passed69files/447tests,116.22s (session12099), log/tmp/seed121-slice9-cli-test-calibrated.log. The initial11000-file fixture failed only its5000ms deadline; all446prior cases passed. The calibrated4200valid218-byte filenames produce a measured1142400-byte native full listing and the proposal still changes exactly3typed Markdown files. Timeout/config/product behavior was unchanged. Python discovery selected0; no Python coverage claimed.

Exact final test bytes were copied to a disposable detached worktree at9b6dd33f4fb664897c34386b3206143c49f34662. Replacing only the selector with exact historical7c96542978^ source made the same success test fail with real spawnSync git ENOBUFS and measured1142400bytes (session16483, exit1; log/tmp/seed121-slice9-old-scan-negative.log). Restoring the current selector in that same scratch made the selected test pass (session19774, exit0;1pass/34skipped,2.78s; log/tmp/seed121-slice9-scratch-restored.log). The fault was reverted, input/old-source diff/logs/results retained privately at /Users/terryyin/.codex/tmp/seed121-oldscan-1ny3jqtn, and the clean scratch worktree was removed normally without force. Original shared checkout never held the fault.

Scratch input hashes identify the observed pre-coordinator-format source. Routine formatting changes layout only; fixture inputs, action and observing assertions remain unchanged.
