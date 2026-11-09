# Source-only review

Reviewer `/root/amended_source_review` was launched by `/root` on 2026-11-09
through `collaboration.spawn_agent` with `fork_turns: "none"`. Its sole input
was the current story contract text at `/tmp/seed111-amended-source-input.txt`,
SHA256 `a3a0539a57c34d6d15f9af363a1b9eeb44620256b8a29aebdfa8c1d33a98f1d3`.
That input contains the original Goal, required Scope and restrictions, key
examples, and complete file list after the human amendment on trunk
`9a33d32751e905a00624b0fb28b1ef5a42eabd23`. The reviewer read only that input,
read no project files, changed nothing, and derived source promises without
slice ownership or acceptance. It then split compound observations in a
follow-up using the same source-only context. The table preserves its latest
complete final return. The prior review remains recoverable at `a84e1ee292`.
Allocation and reused observations below were applied separately by the
coordinator's retention agent.

The reviewer's file-list qualification applies to C27–C44 verbatim.

Each following file individually carries the applicable execution, failure, reporting, timing, runner-support, and test-inclusion commitments above. Retirement files also carry the gate.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| C01 | Every `donut CI` attempt runs all 18 in-scope files on the CI runner. | slice 2 |  |
| C02 | A failing test in these files fails the attempt, subject to the retirement gate in Q01. | slice 2 |  |
| C03 | Each attempt’s CI proof summary publishes every in-scope file’s test outcomes under `scripts`. | slice 2 |  |
| C04 | Each attempt’s CI proof summary publishes every in-scope file’s cumulative recorded test time under `scripts`. | slice 2 |  |
| C05 | Intent’s Scripts and Infrastructure group shows the same per-file outcomes for the selected attempt. | slice 3 |  |
| C06 | Intent shows the same per-file recorded times for that attempt. | slice 3 |  |
| C07 | Intent shows the group’s test counts for that attempt. | slice 3 |  |
| C08 | Intent shows the group’s recorded time for that attempt. | slice 3 |  |
| C09 | Failed tests contribute their failed outcomes. | slice 2 |  |
| C10 | Failed tests contribute whatever durations the runner recorded. | slice 2 |  |
| C11 | A file that fails to load counts as one failed check under the existing summary rules. | slice 2 |  |
| C12 | A part that did not run appears as missing under the existing summary rules. | slice 2 |  |
| C13 | The CI runner provides the prerequisites the files need. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| C14 | No test in these files is skipped on CI to make an attempt pass. | slice 2 |  |
| C15 | No test in these files is left out on CI to make an attempt pass. | slice 2 |  |
| C16 | If a file cannot run on the CI runner without changing what it checks, the story owner decides its disposition. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| C17 | Before retirement tests become required, repeatedly run `verified idle fixture reports idle snapshot not a reservation` with all 18 files together on the intended CI runner. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| C18 | Those repetitions use the same setup as the job that will run the group. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| C19 | Record each repetition’s CI attempt in the plan. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| C20 | Record each repetition’s outcomes in the plan. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| C21 | Resolve any recurring failure in the group before making its tests required. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| C22 | Treat any idle-test failure during the CI repetitions as recurring, because the stated local evidence already includes one failure. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| C23 | Resolve the disposable MySQL cleanup failure before starting the CI repetitions, because the stated evidence already establishes recurrence. | slice 1 | observed: SEED-114 dependency resolution at 37253a42cc, integrated 5d70136ac833: disposable helper stop awaits terminateOwnedProcessTree before rmSync. Inspected current helper and fast-forwarded supplier regression 03fc16245a before probe publication; hosted series still owes G1. |
| C24 | After resolving an idle-test failure or another recurrent failure during the repetitions, run the repetitions again before making the tests required. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| C25 | Keep the 13 `scripts/ci/application-release-*.test.mjs` files running inside the Bash `scripts/test/application-release.test` check. | slice 2 |  |
| C26 | Continue counting that Bash check as one check. | slice 2 |  |
| C27 | `scripts/dev-healthcheck.test.mjs` | slice 2 |  |
| C28 | `scripts/isolated-cypress-openai-mock-cancel.test.mjs` | slice 2 |  |
| C29 | `scripts/isolated-cypress-openai-mock-failure.test.mjs` | slice 2 |  |
| C30 | `scripts/isolated-cypress-openai-mock-ownership.test.mjs` | slice 2 |  |
| C31 | `scripts/isolated-cypress-openai-mock.test.mjs` | slice 2 |  |
| C32 | `scripts/isolated-cypress-owning-health.test.mjs` | slice 2 |  |
| C33 | `scripts/isolated-openai-mock.test.mjs` | slice 2 |  |
| C34 | `scripts/isolated-service-mocks.test.mjs` | slice 2 |  |
| C35 | `scripts/local-runtime-target.test.mjs` | slice 2 |  |
| C36 | `scripts/profiling/verify-publication-receiver.test.mjs` | slice 2 |  |
| C37 | `scripts/sut-owner-application-group.test.mjs` | slice 2 |  |
| C38 | `scripts/worktree-reset-isolation-harness-openai-mock.test.mjs` | slice 2 |  |
| C39 | `scripts/worktree-reset-isolation-harness-wikidata-mock.test.mjs` | slice 2 |  |
| C40 | `scripts/worktree-retirement-checkout-processes.test.mjs` | slice 2 |  |
| C41 | `scripts/worktree-retirement-e2e-mutation.test.mjs` | slice 2 |  |
| C42 | `scripts/worktree-retirement-evidence.test.mjs` | slice 2 |  |
| C43 | `scripts/worktree-retirement-mutation.test.mjs` | slice 2 |  |
| C44 | `scripts/worktree-retirement.test.mjs` | slice 2 |  |
| Q01 | Retirement tests become required only after the specified gate. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| Q02a | “Required” means the files run on the runner. | slice 2 |  |
| Q02b | “Required” also means a failing test fails the attempt. | slice 2 |  |
| Q03 | Durations reflect what the runner recorded, including for failures. | slice 2 |  |
| Q04 | Existing CI proof summary rules continue to apply unchanged. | slice 2 |  |
| Q05a | The intended runner is assumed to be GitHub’s `ubuntu-latest`. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| Q05b | The repository variable `RUNNER` named that runner on 2026-11-09. | slice 1 | observed: Selected seed Boundary assumption and decisive premise dated 2026-11-09: gh variable list named ubuntu-latest, as does the Lint job. Current probe explicitly names ubuntu-latest; no new variable read claimed. |
| Q05c | The lint job named that runner on 2026-11-09. | slice 1 | observed: Selected seed Boundary assumption and decisive premise dated 2026-11-09: gh variable list named ubuntu-latest, as does the Lint job. Current probe explicitly names ubuntu-latest; no new variable read claimed. |
| Q06 | The source requires repeated CI runs without specifying a numerical repetition count. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| J01a | A candidate revision’s attempt runs all 18 files. | slice 2 |  |
| J01b | All 18 files pass in that example. | slice 2 |  |
| J02a | Its `ci-proof-summary-attempt-<n>` lists every file under `scripts` with its passed count. | slice 2 |  |
| J02b | That summary lists every file’s recorded time under `scripts`. | slice 2 |  |
| J03 | The passing mutation-test example uses a MySQL 8.4 server started on the runner. | slice 2 |  |
| J04a | Intent shows the same counts for each file. | slice 3 |  |
| J04b | Intent shows the same recorded times for each file. | slice 3 |  |
| J04c | Intent shows the same counts for the group. | slice 3 |  |
| J04d | Intent shows the same recorded time for the group. | slice 3 |  |
| J05 | A test in `scripts/isolated-service-mocks.test.mjs` fails on the runner, causing the attempt to fail. | slice 2 |  |
| J06a | The summary attributes that failed test to its file. | slice 2 |  |
| J06b | The summary attributes that test’s recorded duration to its file when the runner reported one. | slice 2 |  |
| J06c | Intent attributes that failed test to its file. | slice 3 |  |
| J06d | Intent attributes that test’s recorded duration to its file when the runner reported one. | slice 3 |  |
| J07 | Other tests in that file retain their own results. | slice 2 |  |
| J08a | Before retirement tests become required, the full group runs repeatedly on the intended runner. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| J08b | Each repetition’s CI attempt is recorded. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| J08c | Each repetition’s outcomes are recorded. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| J09a | An idle-test failure in any repetition triggers resolution of its cause before the tests become required. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| J09b | After that cause is resolved, the repetitions run again before the tests become required. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| J10a | Another test’s recurrent failure triggers resolution of its cause before the tests become required. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| J10b | After that cause is resolved, the repetitions run again before the tests become required. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| J11a | As a runner-prerequisite example, `scripts/worktree-retirement-e2e-mutation.test.mjs` starts its own MySQL server from local binaries. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| J11b | That test checks that the server is version 8.4. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| J12 | That MySQL example allows either `mysqld` on `PATH` or `MYSQL_BASEDIR`. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| J13 | As another runner-prerequisite example, `scripts/worktree-retirement-checkout-processes.test.mjs` starts a Java process. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| J14a | That checkout-process example inspects processes using `ps`. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| J14b | That checkout-process example inspects processes using `lsof`. | slice 1 | observed: runner-series-clean.md and exact successful run42583951600 attempt1 at a84e1ee292. Root independently read all30 artifact result sets and exact18-file selection. Each repetition passed78 raw tests with zero fail/cancel/skip/todo. Named idle and actual MySQL8.4.3 cases passed30/30 after the two documented recurring failures were repaired. Shared Lint setup and native await-revision success establish runner execution. |
| E01 | Node test files outside `scripts/` are outside scope. | slice 2 |  |
| E02 | CLI CI proof is outside scope. | slice 2 |  |
| E03 | Removing any of the 18 files is outside scope. | slice 2 |  |
| E04 | Merging any of the 18 files is outside scope. | slice 2 |  |
