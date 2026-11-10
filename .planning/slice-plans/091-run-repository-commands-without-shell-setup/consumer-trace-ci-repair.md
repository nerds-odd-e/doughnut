# Consumer trace

## Effect terms searched

| Term | Origin |
| --- | --- |
| `cli/tests/notebookPublish.lfs.test.ts` | changed path |
| `notebookPublish.lfs.test.ts` | basename of `cli/tests/notebookPublish.lfs.test.ts` |
| `GIT_CONFIG_GLOBAL` | supplied |
| `GIT_CONFIG_NOSYSTEM` | supplied |
| `cli/tests/notebookPublish.lfs.test` | import path of `cli/tests/notebookPublish.lfs.test.ts` |
| `notebookPublish.lfs.test` | import path of `cli/tests/notebookPublish.lfs.test.ts` |

## Scope

Whole checkout at `3a5b5bc848bf`, including uncommitted and untracked files: 4215 tracked and untracked non-ignored files searched with fixed-string, case-insensitive matching.

Changed files not searched: `cli/tests/notebookPublish.lfs.test.ts`.

## Located consumers

Kinds are path hints for inspection; each file appears once. Fill every Disposition with `run`, `reused` or `excluded: <reason the effect does not reach its boundary>`.

### Production

none found

### Test

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `cli/tests/notebookPublish.lfsFailure.test.ts` | `notebookPublish.lfs.test` | 17: `} from './notebookPublish.lfs.testHelpers.js'` | excluded: basename-prefix import refers to unchanged shared helper; moved beforeEach belongs only to notebookPublish.lfs.test.ts, not this separate file. |
| `cli/tests/notebookPull.lfs.test.ts` | `GIT_CONFIG_GLOBAL`, `GIT_CONFIG_NOSYSTEM`, `notebookPublish.lfs.test` | 15: `} from './notebookPublish.lfs.testHelpers.js'` | excluded: separate test file owns its own Git environment hooks; unchanged shared-helper import does not consume the moved publish-suite hook. |
| `cli/tests/notebookPull.lfs.testHelpers.ts` | `notebookPublish.lfs.test` | 14: `} from './notebookPublish.lfs.testHelpers.js'` | excluded: unchanged helper import; no callback or environment owner moved out of notebookPublish.lfs.test.ts. |
| `cli/tests/notebookPull.rebase.testHelpers.ts` | `GIT_CONFIG_GLOBAL`, `GIT_CONFIG_NOSYSTEM` | 30: `'GIT_CONFIG_GLOBAL',` | excluded: independent environment-copying helper; this change does not alter shared environment-copying behavior. |

### Test support or stand-in

none found

### Journey or feature

none found

### Guidance reader

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.planning/seeds/SEED-121-publish-large-notebooks.md` | `notebookPublish.lfs.test.ts`, `notebookPublish.lfs.test` | 112: `` recovery. The existing `notebookPublish.lfs.test.ts` cost test does not `` | excluded: source reference to existing cost proof; no executable dependence on hook nesting, no change to that owner or its retained proof. |

## Diagnosis and focused observations

Only `cli/tests/notebookPublish.lfs.test.ts` changes: move the existing Git global/system configuration isolation from the nested cost group to its containing LFS suite. All seven original tests, assertions and 5-second deadline remain. The fixture creates handmade pointers and intercepts external LFS install/version/push; host clean/filter processes add unrelated work to setup and status. The shared fixture's earlier hook only creates directories and spies; its cleanup performs filesystem removal, while the existing suite cleanup restores environment stubs.

A controlled external Git configuration was supplied only to a scratch invocation at `/tmp/seed116-ci-lfs-probe/gitconfig`: filter.lfs.process points to an executable printing controlled-host-lfs-filter and exiting42, required=true. This is a deterministic discrimination of host configuration leakage, not a delay or a reproduction of the exact remote5000ms timing. Before the edit the original raw-history case failed at real `git add .gitattributes`; after the edit all seven original LFS cases passed with the same caller configuration.

- Red: `GIT_CONFIG_GLOBAL=/tmp/seed116-ci-lfs-probe/gitconfig GIT_CONFIG_NOSYSTEM=1 ./scripts/run.sh pnpm -C cli exec vitest run tests/notebookPublish.lfs.test.ts -t 'accepted raw history converted to LFS' --maxWorkers 1` -> exit1, one selected case failed from external host filter. `/tmp/seed116-ci-lfs-red.log`.
- Green: `GIT_CONFIG_GLOBAL=/tmp/seed116-ci-lfs-probe/gitconfig GIT_CONFIG_NOSYSTEM=1 ./scripts/run.sh pnpm -C cli exec vitest run tests/notebookPublish.lfs.test.ts --maxWorkers 1` -> exit0,7/7. `/tmp/seed116-ci-lfs-green.log`. Original raw-history assertion `pushedObjectIds(pushCalls) == [OID_B]` observes the complete CLI publication after a raw accepted commit and LFS-conversion commit; setup does not supply the upload result.
- Historical commit `3d62bfe5dde56519b52829985605a406db208583` records prior CI36729419519 timeout caused by host-wide LFS setup filters and introduced this isolation for the cost block only. Current CI42815149180/1 failure at the raw-history case shows a5000ms timeout, not stage timings or current Git configuration. The workflow uses vars.RUNNER with ordinary pnpm cli:test; its file does not establish the current remote host/config. This repair removes a demonstrated irrelevant fixture dependency; remote latency cause remains bounded by those evidence limits.

## Full proof and resource decision

`./scripts/run.sh pnpm cli:test` terminal exit1:68files442tests,10timeout failures432pass,143seconds. Failures: InteractiveCliApp character-by-character /exit; notebook shell; attach EPUB/HTTP400 body/HTTP400 no body; recall wrongMCQ-next; recall Esc/y; Pull fills after rebase/fills after conflict/download failure-rebase-rerun. All reported5000ms deadlines, no assertion mismatch. Original publish-LFS target passed. `/tmp/seed116-ci-cli-full.log` preserves the strict failed verdict.

Local Territory macOS16CPU48GiB:20:29load16.4 with13.5GiB inactive supported one ordinary full run; during it20:31load22.7, concurrent unrelated Python/Chromium activity, no processes touched. After failed terminal, coordinator authorized one full-breadth calibration at maxWorkers1 plus Python and frozen install, preserving all original assertions/deadlines. No claim that load averages or a successful retry alone establish external load as the sole cause. Current remote capacity was not measured.

Serial full-breadth command `./scripts/run.sh bash -c 'pnpm --frozen-lockfile --silent recursive install && pnpm -C cli exec vitest run --maxWorkers 1 && pnpm -C cli test:mineru-python'` -> exit1,68files442tests,1failed441pass,252.58seconds. The one failure is unchanged `notebookPublish.rebasedRejection.suite.ts` remote advancement before ancestry download rejects without posting (registered via notebookPublish.test.ts),5340ms at5s deadline. All ten prior ordinary-run timeout cases passed in this serial selection. `/tmp/seed116-ci-cli-serial.log` preserves this failed verdict; the Python phase was withheld by &&.

One authorized discrimination used the same controlled external global LFS configuration with GIT_TRACE2_EVENT and the exact remote-advance/rebased-rejection journey: `GIT_CONFIG_GLOBAL=/tmp/seed116-ci-lfs-probe/gitconfig GIT_CONFIG_NOSYSTEM=1 GIT_TRACE2_EVENT=/tmp/seed116-ci-rebased-trace.jsonl ./scripts/run.sh pnpm -C cli exec vitest run tests/notebookPublish.test.ts -t 'rebased unpublished work retained on rejection.*remote advancement before ancestry download' --maxWorkers 1` -> exit0,1selected33skipped,2.96seconds. Trace115Git sessions reported1.137seconds summed Git process duration; only LFS child was explicit product git-lfs install --local --skip-repo, no host-filter invocation. Fixture creates Markdown-only divergent repositories and later accepted bundle, then proves refusal/no POST and retained rebased content. This excludes the original filter-leak cause for the new failure; a passing focused retry is not a repair or disposal of its failed full-suite verdict. No edit/widening was made for it; fullCLI proof remains incomplete with cause uncertainty.

Supplemental `./scripts/run.sh pnpm -C cli test:mineru-python` -> exit0 but selected0tests (NO TESTS RAN), so it supplies no behavioral coverage; `/tmp/seed116-ci-cli-python.log`. No Python behavior changed.

Remaining gap: required full CLI verdict is not green despite sufficient original LFS repair discrimination. Coordinator owns disposition/investigation of the new full-suite timeout and delivery; this return claims no overall verification completion. Original backend/runner/held/Cypress proof boundaries are untouched.

## Coordinator completion of full proof

The original return's full-CLI gap was resolved by one preselected full-breadth run after changed measured headroom. At20:40:41 local CPU58.3%idle,load10.9 versus22.7 during prior failures,~1.6GiBunused and no current swap activity. Selected one serial command, with no assertion/deadline/config changes: `./scripts/run.sh bash -c 'pnpm --frozen-lockfile --silent recursive install && pnpm -C cli exec vitest run --maxWorkers 1 && pnpm -C cli test:mineru-python'` -> terminal exit0,68files442tests allpassed,237.51s. At20:44:36 CPU62.9%idle/load14.95 supported continuing the same run. Log `/tmp/seed116-ci-cli-headroom.log`. All previously timed-out cases were included with their original5s deadlines. Earlier strict failed verdicts remain dated; this validates current behavior under the chosen resource conditions, without claiming localpressure alone explains all earlier latency or remoteCI.

Python discovery exited0 and selected0tests; tracked cli/python has only production mineru_book_outline.py, so no standalone Python behavioral coverage is claimed. Python is unaffected by this test-hook change; existing subprocess CLI cases are included in442. The changed LFS suite's controlled red/green remains the repair discrimination, independent of broader timing calibration. No implementation change beyond moving the existing hook. All required observing assertions for the affected consumer ran; no current proof gap.
