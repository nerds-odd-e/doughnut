<<running within nix env>>
# Consumer trace

## Effect terms searched

| Term | Origin |
| --- | --- |
| `diagnoseNotebookCheckout` | supplied |
| `donut notebook diagnose` | supplied |
| `refresh the checkout before publishing` | supplied |
| `NotebookGitDiagnosisBuilder` | supplied |
| `formatInteractiveHelp` | supplied |
| `#domain-operation-ownership` | supplied |
| `notebook-git-projection` | supplied |
| `aNotebookGitDiagnosis` | supplied |

## Scope

Whole checkout at `f3bb7162effc`, including uncommitted and untracked files: 4181 tracked and untracked non-ignored files searched with fixed-string, case-insensitive matching.

Changed files not searched: none named.

## Located consumers

Kinds are path hints for inspection; each file appears once. Fill every Disposition with `run`, `reused` or `excluded: <reason the effect does not reach its boundary>`.

### Production

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `backend/src/main/java/com/odde/donut/services/notebookGit/NotebookGitProjection.java` | `donut notebook diagnose` | 115: `+ " \"donut notebook diagnose <directory>\" and review server recovery; Pull alone cannot"` |run: full backend:test_only passed3,055tests; existing projection/history-reset/folder safety assertions retain accepted head, content, identities and learning associations |
| `cli/src/commands/help.ts` | `formatInteractiveHelp` | 27: `export function formatInteractiveHelp(` |run: complete cli:test passed442tests /68files,45.25s; diagnosis command assertions and retained rebase/conflict/refusal/continued publication suites |
| `cli/src/commands/notebook/notebookDiagnosis.ts` | `diagnoseNotebookCheckout` | 12: `export async function diagnoseNotebookCheckout(directory: string): Promise<void> {` |run: complete cli:test passed442tests /68files,45.25s; diagnosis command assertions and retained rebase/conflict/refusal/continued publication suites |
| `cli/src/nonInteractiveCli.ts` | `diagnoseNotebookCheckout`, `donut notebook diagnose` | 1: `import { diagnoseNotebookCheckout } from './commands/notebook/notebookDiagnosis.js'` |run: complete cli:test passed442tests /68files,45.25s; diagnosis command assertions and retained rebase/conflict/refusal/continued publication suites |

### Test

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitHistoryResetControllerTest.java` | `donut notebook diagnose` | 69: `assertThat(beforeReset.getReason(), containsString("donut notebook diagnose"));` |run: full backend:test_only passed3,055tests; existing projection/history-reset/folder safety assertions retain accepted head, content, identities and learning associations |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitProjectionDriftControllerTest.java` | `donut notebook diagnose` | 93: `assertThat(exception.getReason(), containsString("donut notebook diagnose"));` |run: full backend:test_only passed3,055tests; existing projection/history-reset/folder safety assertions retain accepted head, content, identities and learning associations |
| `backend/src/test/java/com/odde/donut/controllers/NotebookGitProposalFolderPublicationSafetyControllerTest.java` | `donut notebook diagnose` | 158: `assertThat(exception.getReason(), containsString("donut notebook diagnose"));` |run: full backend:test_only passed3,055tests; existing projection/history-reset/folder safety assertions retain accepted head, content, identities and learning associations |
| `cli/tests/notebookDiagnosis.test.ts` | `aNotebookGitDiagnosis` | 25: `const report = makeMe.aNotebookGitDiagnosis` |run: complete cli:test passed442tests /68files,45.25s; diagnosis command assertions and retained rebase/conflict/refusal/continued publication suites |
| `cli/tests/notebookPublish.rebasedRejection.suite.ts` | `donut notebook diagnose` | 29: `"The notebook's current Portable content differs from accepted main. Run \"donut notebook diagnose <directory>\" and review server recovery; Pull alone cannot …` |run: complete cli:test passed442tests /68files,45.25s; diagnosis command assertions and retained rebase/conflict/refusal/continued publication suites |
| `cli/tests/notebookPublish.resolvedContinuationRejection.suite.ts` | `donut notebook diagnose` | 33: `"The notebook's current Portable content differs from accepted main. Run \"donut notebook diagnose <directory>\" and review server recovery; Pull alone cannot …` |run: complete cli:test passed442tests /68files,45.25s; diagnosis command assertions and retained rebase/conflict/refusal/continued publication suites |

### Test support or stand-in

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `packages/donut-test-fixtures/src/makeMe.ts` | `NotebookGitDiagnosisBuilder`, `aNotebookGitDiagnosis` | 33: `import NotebookGitDiagnosisBuilder from './NotebookGitDiagnosisBuilder'` |run: cli:test invokes the new diagnosis builder through run; other getters unchanged and the new constructor is lazy |
| `packages/donut-test-fixtures/src/NotebookGitDiagnosisBuilder.ts` | `NotebookGitDiagnosisBuilder` | 4: `export default class NotebookGitDiagnosisBuilder extends Builder<Report> {` |run: cli:test invokes the new diagnosis builder through run; other getters unchanged and the new constructor is lazy |

### Journey or feature

none found

### Guidance reader

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.planning/NORTH-STAR.md` | `#domain-operation-ownership`, `notebook-git-projection` | 34: `and [domain operation ownership](../docs/notebook-git-projection.md#domain-operation-ownership).` |reused: architecture text moved verbatim; current diagnosis guidance inspected and incoming ownership anchor search returned no stale target |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/PLAN.md` | `notebook-git-projection` | 78: `` Documentation structure: before editing the 328-line synchronization contract, move its domain-operation ownership and identity-correspondence sections to `doc… `` |excluded: plan names remain accurate; execution semantics unchanged |
| `docs/notebook-git-synchronization.md` | `donut notebook diagnose`, `notebook-git-projection` | 164: `` `main` before any change and refuses a drifted projection, ahead of shape and placement refusals. Run `donut notebook diagnose <directory>` `` |reused: architecture text moved verbatim; current diagnosis guidance inspected and incoming ownership anchor search returned no stale target |

## Observation locations and setup

`cli/tests/notebookDiagnosis.test.ts#reviews exact server differences while retaining pending commits and dirty work`: real bound Git checkout, pending commit, staged and unstaged note edits, untracked file. Only NotebookController diagnosis is mocked. Assertions observe A/tree IDs, README unified bytes including trailing spaces, no-final-newline marker, added/removed paths, empty structural marker, exact pointer lines and binary base64; localGitObservation and commit count preserve all local state. Matching canonical trees have a separate positive observation.

CLI publish/rebasedRejection and resolvedContinuationRejection suites retain the real Pull/rebase/conflict journeys and expected stale-base/refusal messages. Backend drift tests assert rejected publication leaves binding/content/learning state unchanged, while history-reset and folder-publication safety cases retain their existing outcomes. Both queued diagnostic/publication orders still pass the complete backend suite.

Docs reorganization reading: domain-operation and identity sections copied exactly to notebook-git-projection.md. Current synchronization contract227lines, projection contract113lines; North Star ownership anchor updated without changing direction. No remaining old ownership/identity deep-link target was found in docs, planning or skills.

## Verification results and limits

`CURSOR_DEV=true nix develop -c pnpm cli:test` passed after final text/fixture changes:68files,442tests,45.25s; /tmp/seed121-slice2-cli-final.log. Existing Python discovery ran0tests with exit0; no Python path changed. `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed3,055tests,0failures/errors,2skipped; BUILD SUCCESSFUL1m52s. Whitespace check passed. No API signature changed, so slice1 generation remains compatible.

Extra diagnostic `CURSOR_DEV=true nix develop -c pnpm -C cli exec tsc --noEmit` remains failed with113error lines in /tmp/seed121-slice2-tsc-diagnostic.log. Its original new SDK-envelope error was corrected with existing runDefaultBackendJson<Report>; the bounded diagnostic retry contains no error at the new diagnosis production/test/builder files. Remaining errors identify unchanged Node16 CLI configuration versus extensionless generated/shared imports, inferred cli rootDir versus existing imports of packages/generated, and unchanged Ink Key/stream/turndown typings. Baseline source at f3bb7162 shows those exact imports/configuration/Key.name accesses. This extra command is absent from the package test/lint contract; no tsconfig, generated code or unrelated CLI behavior was changed. Do not report this diagnostic as passing or let its classification waive required product proof. Coordinator owns disposition of the unresolved optional static check.

Local host proof basis:Mac48GiB,56%memory free, load11.33/9.15/7.21 at14:28+08 during CLI workload. Complete CLI tests then complete backend then final CLI were sequenced; no overlapping owned heavy proof. Backend isolation selected doughnut_wt_91a387d2611349a3850318e6f5d14dd1_test. Approx9minactive, with2min11s explicit backend runtime excluded; first explicit local timestamp14:28:22 CLI launch after edits, diagnosis file creation14:27:32, final CLI14:33:34–14:34:19. Initial source reading preceded those observations, so active estimate is approximate. No commands remain running.
