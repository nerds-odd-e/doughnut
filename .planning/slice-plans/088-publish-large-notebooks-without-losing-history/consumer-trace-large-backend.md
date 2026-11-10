# Consumer trace

## Effect terms searched

| Term | Origin |
| --- | --- |
| `e2e_test/config/cliE2eNotebookListingTasks.ts` | changed path |
| `cliE2eNotebookListingTasks.ts` | basename of `e2e_test/config/cliE2eNotebookListingTasks.ts` |
| `e2e_test/config/cliE2ePluginTasks.ts` | changed path |
| `cliE2ePluginTasks.ts` | basename of `e2e_test/config/cliE2ePluginTasks.ts` |
| `e2e_test/features/cli/cli_notebook_large_publication.feature` | changed path |
| `cli_notebook_large_publication.feature` | basename of `e2e_test/features/cli/cli_notebook_large_publication.feature` |
| `e2e_test/start/pageObjects/cli/index.ts` | changed path |
| `index.ts` | basename of `e2e_test/start/pageObjects/cli/index.ts` |
| `e2e_test/start/pageObjects/cli/notebookLargePublication.ts` | changed path |
| `notebookLargePublication.ts` | basename of `e2e_test/start/pageObjects/cli/notebookLargePublication.ts` |
| `e2e_test/step_definitions/cli_notebook_large_publication.ts` | changed path |
| `cli_notebook_large_publication.ts` | basename of `e2e_test/step_definitions/cli_notebook_large_publication.ts` |
| `scripts/isolated-cypress-active-specs.mjs` | changed path |
| `isolated-cypress-active-specs.mjs` | basename of `scripts/isolated-cypress-active-specs.mjs` |
| `notebookLargePublication` | supplied |
| `measureCliNotebookListing` | supplied |
| `cli_notebook_large_publication` | supplied |
| `createCliE2eNotebookListingTasks` | supplied |
| `e2e_test/config/cliE2ePluginTasks` | import path of `e2e_test/config/cliE2ePluginTasks.ts` |
| `cliE2ePluginTasks` | import path of `e2e_test/config/cliE2ePluginTasks.ts` |
| `e2e_test/start/pageObjects/cli` | import path of `e2e_test/start/pageObjects/cli/index.ts` |
| `cli` | import path of `e2e_test/start/pageObjects/cli/index.ts`; in 1245 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `scripts/isolated-cypress-active-specs` | import path of `scripts/isolated-cypress-active-specs.mjs` |
| `isolated-cypress-active-specs` | import path of `scripts/isolated-cypress-active-specs.mjs` |
| `e2e_test/config/cliE2eNotebookListingTasks` | import path of `e2e_test/config/cliE2eNotebookListingTasks.ts` |
| `cliE2eNotebookListingTasks` | import path of `e2e_test/config/cliE2eNotebookListingTasks.ts` |
| `e2e_test/features/cli/cli_notebook_large_publication` | import path of `e2e_test/features/cli/cli_notebook_large_publication.feature` |
| `e2e_test/start/pageObjects/cli/notebookLargePublication` | import path of `e2e_test/start/pageObjects/cli/notebookLargePublication.ts` |
| `e2e_test/step_definitions/cli_notebook_large_publication` | import path of `e2e_test/step_definitions/cli_notebook_large_publication.ts` |
| `maxBuffer` | declared in `e2e_test/config/cliE2eNotebookListingTasks.ts` |
| `Feature` | declared in `e2e_test/features/cli/cli_notebook_large_publication.feature`; in 222 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `Scenario` | declared in `e2e_test/features/cli/cli_notebook_large_publication.feature`; in 170 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `noteCount` | declared in `e2e_test/start/pageObjects/cli/notebookLargePublication.ts` |
| `batchSize` | declared in `e2e_test/start/pageObjects/cli/notebookLargePublication.ts` |
| `title` | declared in `e2e_test/start/pageObjects/cli/notebookLargePublication.ts`; in 1164 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `body` | declared in `e2e_test/start/pageObjects/cli/notebookLargePublication.ts`; in 928 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `relativePath` | declared in `e2e_test/start/pageObjects/cli/notebookLargePublication.ts` |

## Scope

Whole checkout at `c8fc4073ac74`, including uncommitted and untracked files: 4199 tracked and untracked non-ignored files searched with fixed-string, case-insensitive matching.

Changed files not searched: `e2e_test/config/cliE2eNotebookListingTasks.ts`, `e2e_test/config/cliE2ePluginTasks.ts`, `e2e_test/features/cli/cli_notebook_large_publication.feature`, `e2e_test/start/pageObjects/cli/index.ts`, `e2e_test/start/pageObjects/cli/notebookLargePublication.ts`, `e2e_test/step_definitions/cli_notebook_large_publication.ts`, `scripts/isolated-cypress-active-specs.mjs`.

## Located consumers

Kinds are path hints for inspection; each file appears once. Fill every Disposition with `run`, `reused` or `excluded: <reason the effect does not reach its boundary>`.

### Production

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.agents/skills/dough-execute-plan/scripts/ci-command-run.mjs` | `maxBuffer` | 33: `maxBuffer: answerBytes,` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `.agents/skills/dough-execute-plan/scripts/ci-github-actions.mjs` | `maxBuffer` | 18: `maxBufferBytes: 16 * 1024 * 1024,` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `.agents/skills/dough-execute-plan/scripts/ci-reuse-custom-example.mjs` | `maxBuffer` | 38: `maxBuffer: 16 * 1024,` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `.agents/skills/dough-execute-plan/scripts/consumer-trace.mjs` | `maxBuffer` | 32: `maxBuffer: 256 * 1024 * 1024,` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `.agents/skills/dough-execute-plan/scripts/execution-worktree-preparation-readiness-gate.mjs` | `maxBuffer` | 32: `maxBuffer: 2_000_000,` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `.agents/skills/dough-execute-plan/scripts/github-request-answer.mjs` | `maxBuffer` | 204: `if (error.code === "ERR_CHILD_PROCESS_STDIO_MAXBUFFER") {` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `.agents/skills/dough-execute-plan/scripts/github-request-calls.mjs` | `maxBuffer` | 26: `*   readonly maxBufferBytes: number;` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `.agents/skills/dough-execute-plan/scripts/github-request-owner-link.mjs` | `maxBuffer` | 103: `* @param {{ readonly args: readonly string[]; readonly boundMs: number; readonly maxBufferBytes: number }} request` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `.agents/skills/dough-execute-plan/scripts/github-request-owner-wire.mjs` | `maxBuffer` | 149: `const { args, boundMs, maxBufferBytes, scope } = request ?? {};` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `.agents/skills/dough-execute-plan/scripts/github-requests.mjs` | `maxBuffer` | 52: `` // process starts. `maxBufferBytes` is the most one answer may print before `` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `.agents/skills/dough-product-backlog/scripts/product-backlog-story-state-home.mjs` | `relativePath` | 7: `import { dirname, relative as relativePath, resolve, sep } from "node:path";` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `.agents/skills/dough-test-optimization/scripts/ci-proof-read.mjs` | `maxBuffer` | 204: `const isMaxBuffer = (error) =>` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `.cursorindexingignore` | `index.ts` | 6: `packages/generated/donut-backend-api/index.ts` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `backend/src/main/java/com/odde/donut/controllers/AdminUserController.java` | `noteCount` | 67: `dto.setNoteCount(noteRepository.countByCreator(user.getId()));` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `backend/src/main/java/com/odde/donut/controllers/dto/UserForListing.java` | `noteCount` | 12: `private long noteCount;` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `backend/src/main/java/com/odde/donut/services/AssimilationService.java` | `noteCount` | 112: `return subscriptionService.getUnassimilatedNoteCount(subscription)` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NoteFolderAttachment.java` | `relativePath` | 28: `public Optional<NotebookAttachment> at(Note note, String relativePath) {` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `backend/src/main/java/com/odde/donut/services/SubscriptionService.java` | `noteCount` | 17: `public int getUnassimilatedNoteCount(Subscription subscription) {` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `backend/src/main/java/com/odde/donut/services/UserService.java` | `noteCount` | 46: `public int getUnassimilatedNoteCount(User user) {` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `cli/package.json` | `index.ts` | 19: `"bundle": "([ -f .env.local ] && set -a && . ./.env.local && set +a) \|\| true; OUTFILE=\"${CLI_BUNDLE_OUTFILE:-./dist/donut-cli.bundle.mjs}\"; pnpm exec esbuild…` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `cli/src/commands/notebook/notebookPublishLfsSelection.ts` | `maxBuffer` | 51: `maxBuffer: Number.POSITIVE_INFINITY,` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `cli/src/commands/notebook/notebookPullRebase.ts` | `relativePath` | 57: `function quoteForGitCommand(relativePath: string): string {` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `frontend/src/components/admin/UserListing.vue` | `noteCount` | 17: `<td>{{ user.noteCount }}</td>` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `frontend/src/routes/routeRecordsFromMetadata.ts` | `relativePath` | 14: `export function relativePathUnder(` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `frontend/src/routes/routes.ts` | `relativePath` | 31: `relativePathUnder,` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `open_api_docs.yaml` | `noteCount` | 6944: `noteCount:` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `package.json` | `index.ts` | 18: `"cli": "pnpm --frozen-lockfile --silent recursive install && DONUT_API_BASE_URL=http://localhost:8081 DONUT_CONFIG_DIR=$HOME/.config/donut-dev pnpm -C cli exec…` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `packages/donut-api/package.json` | `index.ts` | 10: `".": "./src/index.ts"` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `packages/generated/donut-backend-api/package.json` | `index.ts` | 7: `".": "./index.ts",` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `packages/generated/donut-backend-api/types.gen.ts` | `noteCount` | 1500: `noteCount?: number;` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `scripts/add-generated-package-json.mjs` | `index.ts` | 12: `'.': './index.ts',` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `scripts/isolated-cypress-spec-selection.mjs` | `isolated-cypress-active-specs.mjs`, `isolated-cypress-active-specs` | 7: `} from './isolated-cypress-active-specs.mjs'` |run: focused E2E admits the added feature through active-spec selection; retained registry suite passes three cases. |
| `scripts/profiling/verify-publication-receiver.mjs` | `relativePath` | 10: `* @typedef {{ relativePath: string, content: string }} ExpectedPublicationFile` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `scripts/sut-isolated-target.mjs` | `maxBuffer` | 84: `{ encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 }` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |

### Test

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `backend/src/test/java/com/odde/donut/controllers/AdminUserControllerTest.java` | `noteCount` | 69: `void listsNoteCountAndLastNoteTime() throws UnexpectedNoAccessRightException {` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `backend/src/test/java/com/odde/donut/controllers/AiControllerCreateExtractedNoteTest.java` | `noteCount` | 37: `long noteCountBefore = noteRepository.count();` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `backend/src/test/java/com/odde/donut/controllers/AiControllerExtractNotePreviewTest.java` | `noteCount` | 52: `long noteCountBefore = noteRepository.count();` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitNoteCreationAtomicControllerTest.java` | `noteCount` | 105: `long originalNoteCount =` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `backend/src/test/java/com/odde/donut/controllers/NotebookNoteCreateControllerTest.java` | `noteCount` | 131: `long noteCountBefore = noteRepository.count();` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `backend/src/test/java/com/odde/donut/controllers/NoteReifyPropertyRefusalTests.java` | `noteCount` | 26: `long originalNoteCount;` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `backend/src/test/java/com/odde/donut/services/health/DeadWikiLinkHealthRuleTest.java` | `noteCount` | 209: `int storedNoteCountBefore =` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `cli/tests/notebookPublish.ancestry.suite.ts` | `relativePath` | 24: `files: { relativePath: string; contents: string }[],` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `cli/tests/notebookPublish.largeTree.suite.ts` | `maxBuffer` | 65: `maxBuffer: 4 * 1024 * 1024,` |reused: accepted slice9 proof at cb85e22b, 447 CLI tests plus historical-selector ENOBUFS observation; this E2E addition changes no CLI implementation. |
| `cli/tests/notebookPublish.lfs.testHelpers.ts` | `relativePath` | 66: `relativePath: string,` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `cli/tests/notebookPull.conflict.suite.ts` | `relativePath` | 33: `relativePath: string` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `cli/tests/notebookPull.conflict.testHelpers.ts` | `relativePath` | 33: `relativePath: string,` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `cli/tests/notebookPull.testHelpers.ts` | `relativePath` | 147: `relativePath: string,` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `frontend/tests/components/admin/UserListing.spec.ts` | `noteCount` | 35: `.withNoteCount(42)` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `frontend/tests/layouts/NotebookWorkspaceSequenceSkip.spec.ts` | `noteCount` | 114: `const showNoteCount = showNoteCallCount()` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `frontend/tests/layouts/NotebookWorkspaceUnderstandingResult.spec.ts` | `noteCount` | 65: `const showNoteCount = showNoteCallCount()` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `scripts/profiling/verify-publication-receiver.test.mjs` | `relativePath` | 14: `function write(checkoutDir, relativePath, content) {` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `scripts/workspace-syncpack-lifecycle.test.mjs` | `relativePath` | 68: `for (const relativePath of packagePaths) {` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |

### Test support or stand-in

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `packages/donut-test-fixtures/src/UserForListingBuilder.ts` | `noteCount` | 14: `noteCount: 0,` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |

### Journey or feature

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.agents/skills/e2e-authoring/SKILL.md` | `scripts/isolated-cypress-active-specs.mjs`, `isolated-cypress-active-specs.mjs`, `e2e_test/start/pageObjects/cli`, `scripts/isolated-cypress-active-specs`, `isolated-cypress-active-specs` | 28: `` `APPLICATION_ONLY_ACTIVE_SPECS` in `scripts/isolated-cypress-active-specs.mjs`) before an `` |excluded: existing task/page-object guidance remains valid; this adds one feature through its documented registry. |
| `e2e_test/config/cliE2eNotebookCloneCommitTasks.ts` | `relativePath` | 25: `files: { relativePath: string; content: string }[]` |run: one commit stages the three typed Markdown edits through the existing task. |
| `e2e_test/config/cliE2eNotebookCloneGit.ts` | `relativePath` | 74: `files: { relativePath: string; content: string }[]` |run: real Git stage/commit supplies submitted C; baseline parent and receiver observations retain IDs. |
| `e2e_test/config/cliE2eNotebookCloneTasks.ts` | `relativePath` | 92: `` /** Blob id of `relativePath` in commit `treeish`, or '' when absent there. */ `` |run: real checkout-state/ancestry tasks observe parent A, exact C and clean receiver. |
| `e2e_test/config/cliE2eNotebookLfsHistoricalFetchTasks.ts` | `relativePath` | 84: `relativePath,` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `e2e_test/config/cliE2eNotebookLfsPublishTasks.ts` | `relativePath` | 33: `relativePath: string,` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `e2e_test/config/cliE2eNotebookLfsTasks.ts` | `relativePath` | 167: `for (const relativePath of tracked) {` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `e2e_test/config/cliE2eRepo.test.ts` | `index.ts` | 34: `const source = join(root, 'cli/src/index.ts')` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `e2e_test/config/cliE2eRepo.ts` | `index.ts`, `relativePath` | 68: `function cliE2eInstallBundleCacheRelativePath(version: string): string {` |run: isolated installation builds/serves the installed CLI and owns temporary checkout/config disposal. |
| `e2e_test/config/common.ts` | `cliE2ePluginTasks` | 22: `import { createCliE2ePluginTasks } from './cliE2ePluginTasks'` |run: focused E2E registers the measurement task through createCliE2ePluginTasks. |
| `e2e_test/config/notebookPublicationProfile.ts` | `maxBuffer`, `relativePath` | 85: `maxBuffer: 64 * 1024 * 1024,` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `e2e_test/config/validateCucumberFeatures.test.mjs` | `relativePath` | 15: `function writeFeature(relativePath, contents) {` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `e2e_test/start/pageObjects/cli/notebookCloneCheckout.ts` | `relativePath` | 21: `type RetainedProposalTextFile = { relativePath: string; content: string }` |run: installed Publish plus accepted-head message assertion observe submitted C. |
| `e2e_test/start/pageObjects/cli/notebookCloneCheckoutCommits.ts` | `relativePath` | 19: `files: { relativePath: string; content: string }[]` |run: commitRelatedNoteChanges records the submitted commit ID from the three changes. |
| `e2e_test/start/pageObjects/cli/notebookCloneCheckoutDestination.ts` | `relativePath` | 38: `relativePath: string,` |run: commitNoteChangesAt captures exact proposed bytes and expectCleanAcceptedHeadAt checks clean C. |
| `e2e_test/start/pageObjects/cli/notebookCloneCheckoutFresh.ts` | `relativePath` | 22: `relativePath: string,` |run: fresh clone checks clean checkout at submitted C. |
| `e2e_test/start/pageObjects/cli/notebookCloneCheckoutObservations.ts` | `relativePath` | 31: `relativePath: string` |excluded: unchanged receiver/rebase/observation variants are not altered by adding the separate large-publication page object. |
| `e2e_test/start/pageObjects/cli/notebookCloneCheckoutRebase.ts` | `relativePath` | 66: `relativePath: string` |excluded: unchanged receiver/rebase/observation variants are not altered by adding the separate large-publication page object. |
| `e2e_test/start/pageObjects/cli/notebookCloneCheckoutReceiver.ts` | `relativePath` | 25: `relativePath: string,` |excluded: unchanged receiver/rebase/observation variants are not altered by adding the separate large-publication page object. |
| `e2e_test/start/pageObjects/cli/notebookLfs.ts` | `relativePath` | 90: `expectClonedCheckoutFileExact(relativePath: string, text: string) {` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `e2e_test/start/pageObjects/cli/notebookLfsHistoricalFetch.ts` | `relativePath` | 103: `commitLfsAttachmentRemoval(relativePath: string) {` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `e2e_test/start/pageObjects/cli/notebookLfsPublish.ts` | `relativePath` | 12: `relativePath: string,` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `e2e_test/start/pageObjects/cli/notebookPublicationProfile.ts` | `relativePath` | 39: `` relativePath: `${folder(i % folders)}/${title(pathKind, i)}.md`, `` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `e2e_test/start/pageObjects/notebookPage.ts` | `relativePath` | 135: `attachEpubFixture(relativePath: string) {` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `e2e_test/start/pageObjects/noteRichPropertyMethods.ts` | `relativePath` | 70: `uploadRichNoteImagePropertyFromFixture(fixtureRelativePath: string) {` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `e2e_test/step_definitions/book_reading.ts` | `relativePath` | 85: `(relativePath: string) => {` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `e2e_test/step_definitions/cli_notebook_checkout_observations.ts` | `relativePath` | 14: `(relativePath: string) =>` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `e2e_test/step_definitions/cli_notebook_clone_commits.ts` | `e2e_test/start/pageObjects/cli`, `relativePath` | 3: `` * `e2e_test/start/pageObjects/cli`. Clone, publish, and pull stay in `` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `e2e_test/step_definitions/cli_notebook_clone.ts` | `e2e_test/start/pageObjects/cli`, `relativePath` | 3: `` * `e2e_test/start/pageObjects/cli`. Behavior and assertions belong in page objects, not here. `` |run: existing installed Clone, Publish and accepted-head steps participate in the selected scenario. |
| `e2e_test/step_definitions/cli_notebook_lfs_historical_fetch.ts` | `relativePath` | 17: `(relativePath: string) => {` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `e2e_test/step_definitions/cli_notebook_lfs.ts` | `relativePath` | 27: `(relativePath: string, content: string) => {` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |
| `e2e_test/step_definitions/cli.ts` | `e2e_test/start/pageObjects/cli` | 2: `` * CLI scenarios: keep steps as one-line glue to `e2e_test/start/pageObjects/cli`. `` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |

### Guidance reader

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.agents/skills/cli/SKILL.md` | `e2e_test/config/cliE2ePluginTasks.ts`, `cliE2ePluginTasks.ts`, `index.ts`, `e2e_test/config/cliE2ePluginTasks`, `cliE2ePluginTasks`, `e2e_test/start/pageObjects/cli` | 85: `` Vocabulary for Cucumber steps and page objects (`e2e_test/start/pageObjects/cli/`). **Exact TTY behavior** (past messages, user input history, cursor, renderin… `` |excluded: existing task/page-object guidance remains valid; this adds one feature through its documented registry. |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/consumer-trace-cli-recovery.md` | `index.ts`, `e2e_test/start/pageObjects/cli` | 53: `` \| `packages/generated/donut-backend-api/index.ts` \| `diagnose` \| 4: `export type { AcceptCorrectionData, AcceptCorrectionResponse, AcceptCorrectionResponses, A… `` |excluded: dated retained source/proof record only; its historical reference is preserved and consumes no new E2E task or step. |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/consumer-trace-diagnosis.md` | `index.ts` | 37: `` \| `packages/generated/donut-backend-api/index.ts` \| `diagnoseNotebookGit` \| 4: `export type { AcceptCorrectionData, AcceptCorrectionResponse, AcceptCorrectionR… `` |excluded: dated retained source/proof record only; its historical reference is preserved and consumes no new E2E task or step. |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/consumer-trace-recovery-journey.md` | `e2e_test/config/cliE2ePluginTasks.ts`, `cliE2ePluginTasks.ts`, `e2e_test/start/pageObjects/cli/index.ts`, `index.ts`, `scripts/isolated-cypress-active-specs.mjs`, `isolated-cypress-active-specs.mjs`, `e2e_test/config/cliE2ePluginTasks`, `cliE2ePluginTasks`, `e2e_test/start/pageObjects/cli`, `scripts/isolated-cypress-active-specs`, `isolated-cypress-active-specs` | 9: `` \| `e2e_test/start/pageObjects/cli/index.ts` \| changed path \| `` |excluded: dated retained source/proof record only; its historical reference is preserved and consumes no new E2E task or step. |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/consumer-trace-recovery.md` | `index.ts` | 33: `` \| `packages/generated/donut-backend-api/index.ts` \| `recoverNotebookGit`, `RecoverNotebookGitData`, `RecoverNotebookGitResponse` \| 4: `export type { AcceptCorr… `` |excluded: dated retained source/proof record only; its historical reference is preserved and consumes no new E2E task or step. |
| `docs/end-to-end-testing.md` | `index.ts` | 48: `` \| Cypress plugins \| `e2e_test/plugins/index.ts` \| `` |excluded: lexical basename or local-symbol match only; new task and page-object bindings do not change this separate implementation or its callers. |

## Slice 10 continuation observation

- Base: `c8fc4073ac748a62a604597af4ff9c6e33fe6903`; seven original files restored only after exact SHA-256 verification against the parked attempt manifest. Every authored file remains below 250 lines.
- Changed effects: one independent complete-listing measurement task, one large-publication page object/step vocabulary, and active feature registration. No CLI/backend production implementation changed. Supplied terms: `notebookLargePublication`, `measureCliNotebookListing`, `cli_notebook_large_publication`, `createCliE2eNotebookListingTasks`.
- Scoped diagnostic wait: `cy.then({ timeout: 60_000 }, ...)` retains accepted-head, accepted/live-tree and empty-difference health assertions. The generic six-second wait failed the prior attempt; no global timeout was changed.
- First continuation E2E command: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_large_publication.feature`; terminal exit1, both automatic attempts reached initial listing, installed Publish and fresh receiver. Both stopped at the new callback implicitly returning the fluent Object{3} from `donutNotebookContent.expectNoteByIdAt`, which queues Cypress commands. Error and matching source are `/tmp/seed121-slice10-continuation-e2e.log:58` and `e2e_test/start/donutNotebookContent.ts:118`. Corrected only the new callback to a block with no synchronous return.
- Initial earlier attempt's retry injection timeout remains unexplained; it did not recur in the two continuation attempts. Passing injection does not establish the earlier timeout's cause.
- Expensive proof: Territory.local,48GiB; prelaunch load3.82/5.75/8.58,877154 inactive16KiBpages. Prior owned backend log showed completed graceful shutdown at18:46:57 local and ports49723–25 had no listeners. Root confirmed no competing owned heavy proof. One fresh focused batch; one bounded rerun tests the exact callback correction after its completed19:10:21 shutdown, load7.32/6.75/8.60. Owned DB `doughnut_e2e_wt_91a387d2611349a3850318e6f5d14dd1`, origin `http://127.0.0.1:49725`.
- Final rerun: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_large_publication.feature`; owned session16546 terminal exit0 observed2026-11-10T11:11:51Z. One selected scenario, one pass in16.768s, zero failures/skips/retries. Log `/tmp/seed121-slice10-continuation-e2e-fixed.log`. Actual A=`d4dbef89a293092d3fd631343ccf632eb35463c2`; submitted/received C=`61f893db8c35c497cd32597113d6b001bd0ec2c4`; both complete listings1760068bytes/11001entries.
- Registry command: `CURSOR_DEV=true nix develop -c node scripts/ci/node-test.mjs scripts/isolated-cypress-cli-spec.test.mjs`; terminal exit0, three passing retained registry/isolation cases. The focused E2E itself observes admission of the newly registered feature. Log `/tmp/seed121-slice10-continuation-registry.log`.

## Promise observations and fixture boundary

Setup is the Given sequence in `cli_notebook_large_publication.feature`: actual isolated serving/installation, existing-user login, owned notebook and token; `notebookLargePublication.seed` injects11000 unique valid typed root notes in44 existing250-note batches, then creates one accepted Git snapshot via existing testability. Setup supplies only initial accepted A; the three edits, Publish and receiver are actual product operations. No backend response is mocked.

| Promise | Actual observing location |
| --- | --- |
| J02.1 healthy starting notebook | `notebookLargePublication.expectHealthyLargeAcceptedTree`: actual diagnostic report acceptedHead==native A, acceptedTree==liveTree, differences==[]. |
| J02.2 oversized complete listing; C08 workload qualification | `createCliE2eNotebookListingTasks.measureCliNotebookListing` invokes native `git ls-tree -r -z HEAD` with test-only8MiB buffer; scenario asserts bytes>1048576 and entries>11000. |
| J02.3 three Markdown edits; J02.4 based on A | `commitThreeEdits` calls existing `commitRelatedNoteChanges` for three distinct typed Markdown paths, then actual Git checkout parent==baseline A. `commitNoteChangesAt` records real submitted C. |
| C08/J02.5 Publish succeeds; J02.6 submitted IDs retained | Existing installed-CLI Publish step and `expectCommittedHeadAccepted` require stdout containing submitted C; `expectFreshCloneAtAcceptedHead` reads fresh receiver Git HEAD==C and status==empty. |
| J02.7 unchanged accepted ancestry retained | `expectFreshReceiver` invokes actual `git merge-base --is-ancestor` task for baseline A against receiver HEAD and requires true. Exact C retains its original parent A. |
| J02.8 edits applied | `expectFreshReceiver` reads all three receiver paths and compares exact submitted text bytes; original injected Note IDs are read via actual API and `expectNoteByIdAt` asserts full path and edited body for each. |
| J02.9 size alone imposes no edit limit | Same installed Publish/fresh-receiver pass qualifies listing1760068bytes; receiver entry count equals initial11001. |
| Q11 deferred promises are limits, not rejection rules | Reading: changes add only E2E fixture/task/vocabulary/registry; no admission policy or production restriction changed. |

No mapped slice10 observation remains uncovered. Original10active-minute attempt stays incomplete in its dated record; this continuation used approximately4active minutes including setup, diagnosis, code and trace, plus excepted E2E startup/runtime waits. No production incident action was performed.
