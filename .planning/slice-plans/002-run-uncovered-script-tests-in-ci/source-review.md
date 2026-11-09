# Source-only review

Reviewer: `/root/source_review`, launched by `/root` on 2026-11-09 through
`collaboration.spawn_agent` with `fork_turns: "none"`. The launch supplied only
the original selected story's Goal, required behavior, restrictions, scope
limits, boundary assumption, retirement gate, key examples, and complete list
of 18 scoped files. The reviewer was instructed to use only that text, read no
project files, change nothing, and derive individual source commitments,
qualifications, trigger/journey examples, and exclusions without assigning
slices or acceptance. It returned the 80 ids below in its final answer. The
source commitment cells preserve that returned text; the coordinator's
retention agent allocated the slice ownership separately.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| C01 | Every `donut CI` attempt runs `scripts/dev-healthcheck.test.mjs` on the CI runner. | slice 2 |  |
| C02 | Every attempt runs `scripts/isolated-cypress-openai-mock-cancel.test.mjs`. | slice 2 |  |
| C03 | Every attempt runs `scripts/isolated-cypress-openai-mock-failure.test.mjs`. | slice 2 |  |
| C04 | Every attempt runs `scripts/isolated-cypress-openai-mock-ownership.test.mjs`. | slice 2 |  |
| C05 | Every attempt runs `scripts/isolated-cypress-openai-mock.test.mjs`. | slice 2 |  |
| C06 | Every attempt runs `scripts/isolated-cypress-owning-health.test.mjs`. | slice 2 |  |
| C07 | Every attempt runs `scripts/isolated-openai-mock.test.mjs`. | slice 2 |  |
| C08 | Every attempt runs `scripts/isolated-service-mocks.test.mjs`. | slice 2 |  |
| C09 | Every attempt runs `scripts/local-runtime-target.test.mjs`. | slice 2 |  |
| C10 | Every attempt runs `scripts/profiling/verify-publication-receiver.test.mjs`. | slice 2 |  |
| C11 | Every attempt runs `scripts/sut-owner-application-group.test.mjs`. | slice 2 |  |
| C12 | Every attempt runs `scripts/worktree-reset-isolation-harness-openai-mock.test.mjs`. | slice 2 |  |
| C13 | Every attempt runs `scripts/worktree-reset-isolation-harness-wikidata-mock.test.mjs`. | slice 2 |  |
| C14 | Every attempt runs `scripts/worktree-retirement-checkout-processes.test.mjs`, subject to the retirement gate. | slice 2 |  |
| C15 | Every attempt runs `scripts/worktree-retirement-e2e-mutation.test.mjs`, subject to the retirement gate. | slice 2 |  |
| C16 | Every attempt runs `scripts/worktree-retirement-evidence.test.mjs`, subject to the retirement gate. | slice 2 |  |
| C17 | Every attempt runs `scripts/worktree-retirement-mutation.test.mjs`, subject to the retirement gate. | slice 2 |  |
| C18 | Every attempt runs `scripts/worktree-retirement.test.mjs`, subject to the retirement gate. | slice 2 |  |
| C19 | A failing test in the required files fails the attempt. | slice 2 |  |
| C20 | The attempt’s CI proof summary publishes each scoped file’s test outcomes under `scripts`. | slice 2 |  |
| C21 | The summary publishes each scoped file’s cumulative recorded test time. | slice 2 |  |
| C22 | Intent shows those same per-file outcomes for the selected attempt. | slice 3 |  |
| C23 | Intent shows those same per-file recorded times for the selected attempt. | slice 3 |  |
| C24 | Intent exposes these results in its Scripts and Infrastructure group. | slice 3 |  |
| C25 | Intent shows the group’s count and recorded time for the selected attempt. | slice 3 |  |
| C26 | Failed tests contribute their failed outcomes to the published results. | slice 2 |  |
| C27 | Failed tests contribute whatever durations the runner recorded. | slice 2 |  |
| C28 | A file that fails to load counts as one failed check. | slice 2 |  |
| C29 | A part that did not run appears as missing. | slice 2 |  |
| C30 | The CI runner provides the capabilities the scoped files need. |  | G1 |
| C31 | No scoped test is skipped on CI to make an attempt pass. | slice 2 |  |
| C32 | No scoped test is left out on CI to make an attempt pass. | slice 2 |  |
| C33 | The retirement tests become required only after the retirement gate is satisfied. |  | G1 |
| C34 | The already-recurring disposable MySQL cleanup failure is resolved before CI repetitions begin. | slice 1 | observed: SEED-114 dependency resolution at 37253a42cc, integrated 5d70136ac833: disposable helper stop awaits terminateOwnedProcessTree before rmSync. Inspected current helper and fast-forwarded supplier regression 03fc16245a before probe publication; hosted series still owes G1. |
| C35 | Before retirement tests become required, the named idle test is exercised repeatedly on the intended CI runner. |  | G1 |
| C36 | Those repetitions run the full group of all 18 files together. |  | G1 |
| C37 | Those repetitions follow the same setup as the job that will run the group. |  | G1 |
| C38 | The CI attempts used for the repetitions are recorded. |  | G1 |
| C39 | The outcomes of those attempts are recorded. |  | G1 |
| C40 | Any recurring failure in the group is resolved before its tests become required. |  | G1 |
| C41 | After a failure requiring resolution, the repetitions run again before retirement tests become required. |  | G1 |
| Q01 | “Required” means that the tests run on the CI runner and their failures fail the attempt. | slice 2 |  |
| Q02 | The full-group repetitions exercise `verified idle fixture reports idle snapshot not a reservation` from `scripts/worktree-retirement-checkout-processes.test.mjs`. |  | G1 |
| Q03 | Any failure of that idle test during CI repetitions counts as recurring because local evidence already contains one failure. |  | G1 |
| Q04 | The summary’s existing rules remain unchanged. | slice 2 |  |
| Q05 | Reported time is cumulative recorded test time; the source does not promise unrecorded durations. | slice 2 |  |
| Q06 | The intended runner is assumed to be GitHub’s `ubuntu-latest`. |  | G1 |
| Q07 | That runner assumption is grounded in the repository variable `RUNNER` and the lint job as observed on 2026-11-09. | slice 1 | observed: Selected seed Boundary assumption and decisive premise dated 2026-11-09: gh variable list named ubuntu-latest, as does the Lint job. Current probe explicitly names ubuntu-latest; no new variable read claimed. |
| Q08 | If a file cannot run on the CI runner without changing what it checks, the story owner decides its disposition. |  | G1 |
| Q09 | The MySQL mutation file starts its own MySQL 8.4 server from local binaries and checks its version. |  | G1 |
| Q10 | Those binaries may be available through `mysqld` on `PATH` **or** through `MYSQL_BASEDIR`; both routes are not required. |  | G1 |
| Q11 | The checkout-processes file starts a Java process. |  | G1 |
| Q12 | That file inspects processes using `ps`. |  | G1 |
| Q13 | That file inspects processes using `lsof`. |  | G1 |
| J01 | For an all-passing candidate revision, the `donut CI` attempt runs the complete 18-file group and all files pass. | slice 2 |  |
| J02 | Its `ci-proof-summary-attempt-<n>` lists each scoped file under `scripts` with its passed count. | slice 2 |  |
| J03 | That artifact lists each file’s recorded time. | slice 2 |  |
| J04 | The MySQL mutation file’s passing results come from tests against a MySQL 8.4 server started on the runner. | slice 2 |  |
| J05 | Intent’s CI status for that same attempt shows matching file counts. | slice 3 |  |
| J06 | Intent shows matching file times. | slice 3 |  |
| J07 | Intent also shows the group’s count. | slice 3 |  |
| J08 | Intent also shows the group’s time. | slice 3 |  |
| J09 | If one test in `scripts/isolated-service-mocks.test.mjs` fails on the runner, the attempt fails. | slice 2 |  |
| J10 | The summary counts that test as failed for its file. | slice 2 |  |
| J11 | Intent counts that test as failed for its file. | slice 3 |  |
| J12 | The failed test’s duration is included when the runner reported one. | slice 2 |  |
| J13 | The file’s other tests retain their own results. | slice 2 |  |
| J14 | During the retirement gate, the plan records each CI repetition’s attempt. |  | G1 |
| J15 | The plan records each repetition’s outcomes. |  | G1 |
| J16 | If the idle test fails in any repetition, its cause is resolved before the tests become required. |  | G1 |
| J17 | If another test fails recurrently, its cause is resolved before the tests become required. |  | G1 |
| J18 | After either qualifying failure is resolved, the full-group repetitions run again. |  | G1 |
| E01 | The 13 `scripts/ci/application-release-*.test.mjs` files retain their execution inside the Bash `scripts/test/application-release.test` check. | slice 2 |  |
| E02 | Those 13 files continue to count as one check. | slice 2 |  |
| E03 | Node test files outside `scripts/` are outside this story. | slice 2 |  |
| E04 | CLI CI proof is outside this story. | slice 2 |  |
| E05 | The test counts of SEED-112 are outside this story. | slice 2 |  |
| E06 | Removing any of the 18 scoped files is outside this story. | slice 2 |  |
| E07 | Merging any of the 18 scoped files is outside this story. | slice 2 |  |
| E08 | The named examples do not restrict the promise to those example files, failures, or candidate revisions. | slice 3 |  |
