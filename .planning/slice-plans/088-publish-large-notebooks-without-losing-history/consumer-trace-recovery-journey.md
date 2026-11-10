# Consumer trace

## Effect terms searched

| Term | Origin |
| --- | --- |
| `e2e_test/features/cli/cli_notebook_recovery.feature` | changed path |
| `cli_notebook_recovery.feature` | basename of `e2e_test/features/cli/cli_notebook_recovery.feature` |
| `e2e_test/start/pageObjects/cli/index.ts` | changed path |
| `index.ts` | basename of `e2e_test/start/pageObjects/cli/index.ts` |
| `e2e_test/start/pageObjects/cli/notebookRecovery.ts` | changed path |
| `notebookRecovery.ts` | basename of `e2e_test/start/pageObjects/cli/notebookRecovery.ts` |
| `e2e_test/step_definitions/cli_notebook_recovery.ts` | changed path |
| `cli_notebook_recovery.ts` | basename of `e2e_test/step_definitions/cli_notebook_recovery.ts` |
| `scripts/isolated-cypress-active-specs.mjs` | changed path |
| `isolated-cypress-active-specs.mjs` | basename of `scripts/isolated-cypress-active-specs.mjs` |
| `notebookRecovery` | supplied |
| `runInstalledCli` | supplied |
| `e2e_test/start/pageObjects/cli` | import path of `e2e_test/start/pageObjects/cli/index.ts` |
| `cli` | import path of `e2e_test/start/pageObjects/cli/index.ts`; in 1239 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `scripts/isolated-cypress-active-specs` | import path of `scripts/isolated-cypress-active-specs.mjs` |
| `isolated-cypress-active-specs` | import path of `scripts/isolated-cypress-active-specs.mjs` |
| `e2e_test/features/cli/cli_notebook_recovery` | import path of `e2e_test/features/cli/cli_notebook_recovery.feature` |
| `cli_notebook_recovery` | import path of `e2e_test/features/cli/cli_notebook_recovery.feature` |
| `e2e_test/start/pageObjects/cli/notebookRecovery` | import path of `e2e_test/start/pageObjects/cli/notebookRecovery.ts` |
| `e2e_test/step_definitions/cli_notebook_recovery` | import path of `e2e_test/step_definitions/cli_notebook_recovery.ts` |
| `Feature` | declared in `e2e_test/features/cli/cli_notebook_recovery.feature`; in 219 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `Scenario` | declared in `e2e_test/features/cli/cli_notebook_recovery.feature`; in 168 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `type` | declared in `e2e_test/features/cli/cli_notebook_recovery.feature`; in 2137 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `runOnPublisher` | declared in `e2e_test/start/pageObjects/cli/notebookRecovery.ts` |
| `args` | declared in `e2e_test/start/pageObjects/cli/notebookRecovery.ts`; in 224 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `env` | declared in `e2e_test/start/pageObjects/cli/notebookRecovery.ts`; in 431 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `diagnosis` | declared in `e2e_test/start/pageObjects/cli/notebookRecovery.ts` |

## Scope

Whole checkout at `78cd0f8c6b18`, including uncommitted and untracked files: 4193 tracked and untracked non-ignored files searched with fixed-string, case-insensitive matching.

Changed files not searched: `e2e_test/features/cli/cli_notebook_recovery.feature`, `e2e_test/start/pageObjects/cli/index.ts`, `e2e_test/start/pageObjects/cli/notebookRecovery.ts`, `e2e_test/step_definitions/cli_notebook_recovery.ts`, `scripts/isolated-cypress-active-specs.mjs`.

## Located consumers

Kinds are path hints for inspection; each file appears once. Fill every Disposition with `run`, `reused` or `excluded: <reason the effect does not reach its boundary>`.

### Production

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.agents/skills/dough-execute-plan/scripts/agent-assignments.mjs` | `diagnosis` | 135: `// occupied assignment for diagnosis.` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.agents/skills/dough-execute-plan/scripts/ci-mailbox-complete.mjs` | `diagnosis` | 131: `// an interrupted wait retain the observer for diagnosis or an explicit stop.` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.cursorindexingignore` | `index.ts` | 6: `packages/generated/donut-backend-api/index.ts` |excluded: generic index/diagnosis or stable module/package reference; no changed production/export contract reaches this boundary |
| `backend/src/main/java/com/odde/donut/controllers/NotebookController.java` | `diagnosis` | 25: `import com.odde.donut.services.notebookGit.NotebookGitDiagnosis;` |reused: delivered diagnosis/recovery contract and CLI proof from slices6/7 are unchanged; new installed recovery feature exercises their actual HTTP continuation |
| `backend/src/main/java/com/odde/donut/controllers/NotebookGitHttpSupport.java` | `diagnosis` | 7: `import com.odde.donut.services.notebookGit.NotebookGitDiagnosis;` |reused: delivered diagnosis/recovery contract and CLI proof from slices6/7 are unchanged; new installed recovery feature exercises their actual HTTP continuation |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitDiagnosis.java` | `diagnosis` | 20: `public class NotebookGitDiagnosis {` |reused: delivered diagnosis/recovery contract and CLI proof from slices6/7 are unchanged; new installed recovery feature exercises their actual HTTP continuation |
| `cli/package.json` | `index.ts` | 19: `"bundle": "([ -f .env.local ] && set -a && . ./.env.local && set +a) \|\| true; OUTFILE=\"${CLI_BUNDLE_OUTFILE:-./dist/donut-cli.bundle.mjs}\"; pnpm exec esbuild…` |reused: delivered diagnosis/recovery contract and CLI proof from slices6/7 are unchanged; new installed recovery feature exercises their actual HTTP continuation |
| `cli/src/commands/help.ts` | `notebookRecovery`, `diagnosis` | 7: `import { notebookDiagnosisDoc } from './notebook/notebookDiagnosis.js'` |reused: delivered diagnosis/recovery contract and CLI proof from slices6/7 are unchanged; new installed recovery feature exercises their actual HTTP continuation |
| `cli/src/commands/notebook/nonInteractiveNotebook.ts` | `notebookRecovery`, `diagnosis` | 3: `notebookDiagnosisDoc,` |reused: delivered diagnosis/recovery contract and CLI proof from slices6/7 are unchanged; new installed recovery feature exercises their actual HTTP continuation |
| `cli/src/commands/notebook/notebookDiagnosis.ts` | `diagnosis` | 13: `export const notebookDiagnosisDoc = {` |reused: delivered diagnosis/recovery contract and CLI proof from slices6/7 are unchanged; new installed recovery feature exercises their actual HTTP continuation |
| `cli/src/commands/notebook/notebookRecovery.ts` | `notebookRecovery` | 8: `export const notebookRecoveryDoc = {` |reused: delivered diagnosis/recovery contract and CLI proof from slices6/7 are unchanged; new installed recovery feature exercises their actual HTTP continuation |
| `open_api_docs.yaml` | `diagnosis` | 3076: `/api/notebooks/{notebook}/git-diagnosis:` |reused: delivered diagnosis/recovery contract and CLI proof from slices6/7 are unchanged; new installed recovery feature exercises their actual HTTP continuation |
| `package.json` | `index.ts` | 18: `"cli": "pnpm --frozen-lockfile --silent recursive install && DONUT_API_BASE_URL=http://localhost:8081 DONUT_CONFIG_DIR=$HOME/.config/donut-dev pnpm -C cli exec…` |excluded: generic index/diagnosis or stable module/package reference; no changed production/export contract reaches this boundary |
| `packages/donut-api/package.json` | `index.ts` | 10: `".": "./src/index.ts"` |excluded: generic index/diagnosis or stable module/package reference; no changed production/export contract reaches this boundary |
| `packages/generated/donut-backend-api/package.json` | `index.ts` | 7: `".": "./index.ts",` |excluded: generic index/diagnosis or stable module/package reference; no changed production/export contract reaches this boundary |
| `packages/generated/donut-backend-api/sdk.gen.ts` | `diagnosis` | 808: `return (options.client ?? client).get<DiagnoseNotebookGitResponses, unknown, ThrowOnError>({ url: '/api/notebooks/{notebook}/git-diagnosis', ...options });` |reused: delivered diagnosis/recovery contract and CLI proof from slices6/7 are unchanged; new installed recovery feature exercises their actual HTTP continuation |
| `packages/generated/donut-backend-api/types.gen.ts` | `diagnosis` | 4129: `url: '/api/notebooks/{notebook}/git-diagnosis';` |reused: delivered diagnosis/recovery contract and CLI proof from slices6/7 are unchanged; new installed recovery feature exercises their actual HTTP continuation |
| `scripts/add-generated-package-json.mjs` | `index.ts` | 12: `'.': './index.ts',` |excluded: generic index/diagnosis or stable module/package reference; no changed production/export contract reaches this boundary |
| `scripts/isolated-cypress-spec-selection.mjs` | `isolated-cypress-active-specs.mjs`, `isolated-cypress-active-specs` | 7: `} from './isolated-cypress-active-specs.mjs'` |run: focused isolated-Cypress CLI admission tests (3 pass) and the actual new feature wrapper admission/origin selection |

### Test

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitDiagnosisControllerTest.java` | `diagnosis` | 14: `import com.odde.donut.services.notebookGit.NotebookGitDiagnosis;` |reused: delivered diagnosis/recovery contract and CLI proof from slices6/7 are unchanged; new installed recovery feature exercises their actual HTTP continuation |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitWebContentExtractedNoteDatingControllerTest.java` | `diagnosis` | 60: `var diagnosis = controller.diagnoseNotebookGit(notebook.getId());` |reused: delivered diagnosis/recovery contract and CLI proof from slices6/7 are unchanged; new installed recovery feature exercises their actual HTTP continuation |
| `cli/tests/notebookDiagnosis.test.ts` | `diagnosis` | 17: `describe('notebook diagnosis', () => {` |reused: delivered diagnosis/recovery contract and CLI proof from slices6/7 are unchanged; new installed recovery feature exercises their actual HTTP continuation |

### Test support or stand-in

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `packages/donut-test-fixtures/src/makeMe.ts` | `diagnosis` | 33: `import NotebookGitDiagnosisBuilder from './NotebookGitDiagnosisBuilder'` |excluded: generic index/diagnosis or stable module/package reference; no changed production/export contract reaches this boundary |
| `packages/donut-test-fixtures/src/NotebookGitDiagnosisBuilder.ts` | `diagnosis` | 4: `export default class NotebookGitDiagnosisBuilder extends Builder<Report> {` |excluded: generic index/diagnosis or stable module/package reference; no changed production/export contract reaches this boundary |

### Journey or feature

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.agents/skills/e2e-authoring/SKILL.md` | `scripts/isolated-cypress-active-specs.mjs`, `isolated-cypress-active-specs.mjs`, `e2e_test/start/pageObjects/cli`, `scripts/isolated-cypress-active-specs`, `isolated-cypress-active-specs` | 28: `` `APPLICATION_ONLY_ACTIVE_SPECS` in `scripts/isolated-cypress-active-specs.mjs`) before an `` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `e2e_test/config/cliE2eInstalledCli.ts` | `runInstalledCli` | 12: `export type RunInstalledCliTask = {` |run: focused recovery feature uses the existing installed CLI/managed PTY task path; these shared task implementations are unchanged |
| `e2e_test/config/cliE2eManagedPty.ts` | `runInstalledCli` | 124: `` `${caller}: no managed CLI PTY session. Start interactive (runInstalledCliInteractive / runRepoCliInteractive) or run a one-shot command (runInstalledCli) firs… `` |run: focused recovery feature uses the existing installed CLI/managed PTY task path; these shared task implementations are unchanged |
| `e2e_test/config/cliE2ePluginTasks.ts` | `runInstalledCli` | 11: `runInstalledCliExpectingExit,` |run: focused recovery feature uses the existing installed CLI/managed PTY task path; these shared task implementations are unchanged |
| `e2e_test/config/cliE2eRepo.test.ts` | `index.ts` | 34: `const source = join(root, 'cli/src/index.ts')` |run: focused recovery feature uses the existing installed CLI/managed PTY task path; these shared task implementations are unchanged |
| `e2e_test/config/cliE2eRepo.ts` | `index.ts` | 72: `` /** Env: set to `1` to spawn `tsx src/index.ts` instead of the bundle (debug only). */ `` |run: focused recovery feature uses the existing installed CLI/managed PTY task path; these shared task implementations are unchanged |
| `e2e_test/start/pageObjects/cli/execution.ts` | `runInstalledCli` | 26: `cy.task<null>('runInstalledCli', {` |excluded: additive recovery page-object member and feature do not change existing step, page-object or feature behavior; shared helpers remain unchanged |
| `e2e_test/start/pageObjects/cli/notebookClone.ts` | `runInstalledCli` | 3: `` * (non-interactive, installed-binary invocation — same `runInstalledCli` mechanism as `` |excluded: additive recovery page-object member and feature do not change existing step, page-object or feature behavior; shared helpers remain unchanged |
| `e2e_test/start/pageObjects/cli/notebookCloneCheckout.ts` | `runInstalledCli` | 61: `task: 'runInstalledCli' \| 'runInstalledCliExpectingRejection'` |excluded: additive recovery page-object member and feature do not change existing step, page-object or feature behavior; shared helpers remain unchanged |
| `e2e_test/start/pageObjects/cli/notebookCloneCheckoutDestination.ts` | `runInstalledCli` | 26: `task: 'runInstalledCli' \| 'runInstalledCliExpectingRejection'` |excluded: additive recovery page-object member and feature do not change existing step, page-object or feature behavior; shared helpers remain unchanged |
| `e2e_test/start/pageObjects/cli/notebookCloneCheckoutReceiver.ts` | `runInstalledCli` | 36: `'runInstalledCli'` |excluded: additive recovery page-object member and feature do not change existing step, page-object or feature behavior; shared helpers remain unchanged |
| `e2e_test/start/pageObjects/cli/ttyAssertTerminal.ts` | `runInstalledCli` | 22: `type RunInstalledCliInteractiveArg = {` |excluded: additive recovery page-object member and feature do not change existing step, page-object or feature behavior; shared helpers remain unchanged |
| `e2e_test/step_definitions/cli_notebook_clone_commits.ts` | `e2e_test/start/pageObjects/cli` | 3: `` * `e2e_test/start/pageObjects/cli`. Clone, publish, and pull stay in `` |excluded: additive recovery page-object member and feature do not change existing step, page-object or feature behavior; shared helpers remain unchanged |
| `e2e_test/step_definitions/cli_notebook_clone.ts` | `e2e_test/start/pageObjects/cli` | 3: `` * `e2e_test/start/pageObjects/cli`. Behavior and assertions belong in page objects, not here. `` |excluded: additive recovery page-object member and feature do not change existing step, page-object or feature behavior; shared helpers remain unchanged |
| `e2e_test/step_definitions/cli.ts` | `e2e_test/start/pageObjects/cli` | 2: `` * CLI scenarios: keep steps as one-line glue to `e2e_test/start/pageObjects/cli`. `` |excluded: additive recovery page-object member and feature do not change existing step, page-object or feature behavior; shared helpers remain unchanged |

### Guidance reader

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.agents/skills/cli/SKILL.md` | `index.ts`, `runInstalledCli`, `e2e_test/start/pageObjects/cli` | 85: `` Vocabulary for Cucumber steps and page objects (`e2e_test/start/pageObjects/cli/`). **Exact TTY behavior** (past messages, user input history, cursor, renderin… `` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.agents/skills/dough-execute-plan/manuals/custom-ci.md` | `diagnosis` | 247: `falls back to GitHub. Failed diagnosis for a known failure still reports with` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.agents/skills/dough-execute-plan/references/admit-accepted-work.md` | `diagnosis` | 17: `diagnosis or repair, test profiling or optimization, exploratory or manual` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.agents/skills/dough-execute-plan/references/ci-completion-wait.md` | `diagnosis` | 55: `waiving the failure and keeps the observer available for diagnosis and` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.agents/skills/dough-execute-plan/references/ci-monitor.md` | `diagnosis` | 203: `ownership boundaries. Assign every failure the diagnosis lists for this` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.agents/skills/dough-execute-plan/references/delegation.md` | `diagnosis` | 79: `report"; pass a known failure's attribution as diagnosis evidence, separate` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.agents/skills/dough-execute-plan/references/execution-decisions.md` | `diagnosis` | 140: `For other failures, use focused diagnosis. Attribute a failure as pre-existing,` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.agents/skills/dough-execute-plan/references/finish-or-stop.md` | `diagnosis` | 156: `and the observer remains available for diagnosis. Emit no completion marker or` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.agents/skills/dough-execute-plan/references/one-shot.md` | `diagnosis` | 25: `diagnosis may fit. A queued story whose plan has more than one slice is not` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.agents/skills/dough-manual-testing/SKILL.md` | `diagnosis` | 14: `No diagnosis, product repair, or unrequested tooling.` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.agents/skills/dough-story-refinement/references/executable-proof.md` | `diagnosis` | 72: `record that a known failure predates the story; that attribution is diagnosis` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.planning/seeds/SEED-073-learning-refinement-and-retrieval.md` | `diagnosis` | 38: `events. Neither alone records the diagnosis, proposal decision, and learning` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.planning/seeds/SEED-121-publish-large-notebooks.md` | `diagnosis` | 49: `- Correct the confirmed cause and provide supported diagnosis and recovery` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/consumer-trace-admission.md` | `diagnosis` | 92: `` \| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/consumer-trace-diagnosis.md` \| `.planning/seeds/SEED-121-publish-large-notebooks.md… `` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/consumer-trace-cli-recovery.md` | `index.ts`, `notebookRecovery`, `e2e_test/start/pageObjects/cli`, `diagnosis` | 3: `Installed consumer-trace.mjs ran across the whole checkout at e5f8d8841d5dc2fe9ea5c032c29c7beadfb10ebd, including all eight changed/untracked paths. Supplied e…` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/consumer-trace-cli.md` | `diagnosis` | 11: `` \| `NotebookGitDiagnosisBuilder` \| supplied \| `` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/consumer-trace-diagnosis.md` | `index.ts`, `diagnosis` | 9: `` \| `NotebookGitDiagnosis` \| supplied \| `` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/consumer-trace-extraction.md` | `diagnosis` | 57: `- Complete-tree observation: lines 61–63 use the native notebook diagnosis boundary to compare both accepted and live tree IDs with the accepted bundle's tip t…` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/consumer-trace-recovery.md` | `index.ts`, `diagnosis` | 33: `` \| `packages/generated/donut-backend-api/index.ts` \| `recoverNotebookGit`, `RecoverNotebookGitData`, `RecoverNotebookGitResponse` \| 4: `export type { AcceptCorr… `` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/execution-evidence.md` | `diagnosis` | 38: `At trigger owner requests a diagnostic of the bound notebook, the source expects differing paths and current accepted revision, including live-only/accepted-on…` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/PLAN.md` | `diagnosis` | 62: `publication with diagnosis: A/tree/bytes positively describe one complete committed state. Non-owner denied.` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/source-review.md` | `notebookRecovery`, `diagnosis` | 24: `\| C14 \| Identify differing paths. \| slice 1 \| observed: backend/src/test/java/com/odde/donut/controllers/NotebookGitDiagnosisControllerTest.java#reportsSortedE…` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `docs/end-to-end-testing.md` | `index.ts` | 48: `` \| Cypress plugins \| `e2e_test/plugins/index.ts` \| `` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `docs/notebook-git-synchronization.md` | `diagnosis` | 166: `cannot repair server projection drift. Diagnosis preserves local work. A repair` |excluded: generic diagnosis/index or retained planning/guidance reference; no executable dependency on the added scenario or page-object member |
| `DonutRetrospectiveFindings.md` | `diagnosis` | 65: `- Observed effect: one refused setup command and a diagnosis at startup; two E2E features failed and were rerun; every later brief had to carry both exports.` |excluded: generic index/diagnosis or stable module/package reference; no changed production/export contract reaches this boundary |
| `packages/generated/donut-backend-api/api-summary.md` | `diagnosis` | 142: `` - `diagnoseNotebookGit`: GET `/api/notebooks/{notebook}/git-diagnosis` -> `DiagnoseNotebookGitResponse` (request: `DiagnoseNotebookGitData`; path: notebook; re… `` |excluded: generic index/diagnosis or stable module/package reference; no changed production/export contract reaches this boundary |

## Selected proof

The new feature owns installed diagnose/recover through the real isolated backend, exact reviewed-tree equality, unchanged pending checkout, actual conflict/continue/Publish, clean receiver bytes/ancestry and same Note/tracker linkage. Existing detailed identity/refusal/atomic cases remain slice6 proof; CLI argument and dirty-work variations remain slice7. Shared installed/Pull/receiver helpers are unchanged. Registry proof selects3 passing tests; the runner itself admits this new feature to its private worktree origin/database. The focused recovery feature completed successfully; terminal observations follow.

## Terminal observations

On Territory.local, CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_recovery.feature completed exit0 (owned session52187, terminal observed2026-11-10T10:01:26Z). Cypress selected exactly1 scenario,1passed,0failed/pending/skipped,16seconds. Log: /tmp/seed121-slice8-e2e.log. Its wrapper provisioned doughnut_e2e_wt_91a387d2611349a3850318e6f5d14dd1 and selected http://127.0.0.1:49725; SUT healthy after30polls. This was the installed binary and real HTTP/backend/Git path; no backend mock or test-only production endpoint.

CURSOR_DEV=true nix develop -c node scripts/ci/node-test.mjs scripts/isolated-cypress-cli-spec.test.mjs completed exit0 (session63109),3tests/3pass,0failed,562.66ms. Before expensive launch load18.64/24.67/19.19 and618012 inactive16KiB pages (~9.4GiB) supported one owned E2E batch. No other owned heavy proof ran. Required E2E runtime was excepted; active fixture/page-object/trace work was approximately5minutes, below the ten-minute stop. No formatting/lint/commit/push or production action.

Setup: accepted Overview/README baseline plus a real Understanding tracker; publisher and receiver clone it. One pending commit changes Overview and adds LocalA/LocalB. A sanctioned web save advances accepted history, then existing note injection creates genuine live-only Recovered text without resnapshot. The installed diagnosis prints exact A/L and live-only path; recover sends that pair. Afterward diagnosis has exact accepted/live tree equality with reviewedL and the publisher's complete checkout state is unchanged. Native Pull leaves a real Overview conflict; explicit combined resolution retains author/message, Publish is accepted, receiver Pull retains original accepted ancestor and exact recovered/local/combined bytes. Original Note lookup and same tracker id/note id/count/schedule assertions observe identity/learning preservation after the whole sequence.

# Independent refactor trace

# Consumer trace

## Effect terms searched

| Term | Origin |
| --- | --- |
| `runInstalledOn` | supplied |
| `runOnPublisher` | supplied |
| `notebookCloneCheckoutExecution` | supplied |

## Scope

Whole checkout at `78cd0f8c6b18`, including uncommitted and untracked files: 4199 tracked and untracked non-ignored files searched with fixed-string, case-insensitive matching.

Changed files not searched: none named.

## Located consumers

Kinds are path hints for inspection; each file appears once. Fill every Disposition with `run`, `reused` or `excluded: <reason the effect does not reach its boundary>`.

### Production

none found

### Test

none found

### Test support or stand-in

none found

### Journey or feature

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `e2e_test/start/pageObjects/cli/notebookCloneCheckout.ts` | `runInstalledOn`, `notebookCloneCheckoutExecution` | 14: `import { runInstalledOn } from './notebookCloneCheckoutExecution'` | run: focused installed recovery journey exercises shared primary Publish/Pull/refusal, receiver Pull and explicit diagnose/recover arguments through the real isolated backend; unchanged default-argument callers inspected |
| `e2e_test/start/pageObjects/cli/notebookCloneCheckoutExecution.ts` | `runInstalledOn` | 3: `export function runInstalledOn(` | run: focused installed recovery journey exercises shared primary Publish/Pull/refusal, receiver Pull and explicit diagnose/recover arguments through the real isolated backend; unchanged default-argument callers inspected |
| `e2e_test/start/pageObjects/cli/notebookCloneCheckoutReceiver.ts` | `runInstalledOn`, `notebookCloneCheckoutExecution` | 16: `import { runInstalledOn } from './notebookCloneCheckoutExecution'` | run: focused installed recovery journey exercises shared primary Publish/Pull/refusal, receiver Pull and explicit diagnose/recover arguments through the real isolated backend; unchanged default-argument callers inspected |
| `e2e_test/start/pageObjects/cli/notebookRecovery.ts` | `runInstalledOn`, `notebookCloneCheckoutExecution` | 6: `import { runInstalledOn } from './notebookCloneCheckoutExecution'` | run: focused installed recovery journey exercises shared primary Publish/Pull/refusal, receiver Pull and explicit diagnose/recover arguments through the real isolated backend; unchanged default-argument callers inspected |

### Guidance reader

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/execution-evidence.md` | `runInstalledOn`, `runOnPublisher` | 191: `PFE reading at slice8 acceptance found that new runOnPublisher repeats the existing runInstalledOn alias/binary/config/task invocation. The existing checkout e…` | excluded: root-owned recorded PFE/reorganization decision, not an executable caller; its dated input remains intact |

## Refactor scope and sufficient proof

Extracted runInstalledOn from the 256-line destination operations/observations module into notebookCloneCheckoutExecution.ts. Primary and receiver callers now import the same execution owner; recovery uses it with its real diagnose/recover arguments instead of a second launcher. Destination aliases stay shared with existing observations; no compatibility re-export or generic harness. The remaining destination module is237lines, new executor20, recovery72; primary199 and receiver175. No production/API/generated contract or test setup/assertion changed.

The incoming recovery E2E proof was invalidated only by its execution-helper dependency move, then renewed with the same feature and observing assertions. Other controller/CLI production proof remains unchanged. Registry proof (3tests, parent-accepted session63109) remains valid because feature admission and registry contents were untouched by refactoring. Receiver Publish retains the exact same default argument list/config/task construction as primary Publish; receiver Pull and primary Publish/Pull/refusal are observed in this one journey, so no separate unchanged feature batch is needed.

## Terminal proof

CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_recovery.feature exited0 (owned session34776). Cypress selected exactly1scenario,1passed,0failed/pending/skipped; scenario6685ms (report6seconds, 1passing7s). Terminal result observed2026-11-10T10:09:23Z. Log/tmp/seed121-refactor-slice8-e2e.log. All existing exact reviewed-tree, pending checkout, real conflict/continue/Publish, receiver content/ancestry and Note/tracker identity/count/schedule assertions remained and executed. No resnapshot or backend mock supplied the outcome.

Before the single expensive batch, Territory.local load22.14/25.71/22.72 and746470inactive16KiB pages (~11.4GiB) supported one owned isolated E2E run; root reported no other owned heavy proof. The wrapper retained this checkout's private environment and terminal ownership/cleanup. Required runtime was excepted. ./scripts/run.sh bash scripts/check_diff_whitespace.sh exited0(session25513). Consumer-trace script exited0(session77575); every5locatedconsumer is disposed. No formatting/standalone lint/commit/push/generation or production action ran.
