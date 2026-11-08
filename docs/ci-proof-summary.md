# CI proof summary

Every `donut CI` run attempt publishes the artifact
`ci-proof-summary-attempt-<n>`. The Open Dough dashboard's **Intent → CI
status** tab shows counts and recorded test execution time by location and
file for that attempt. It also shows which CI parts did not report.

| Entry | Producer job | Parts | Unit | Runner report |
| --- | --- | --- | --- | --- |
| `e2e_test/features` | `E2E-tests` matrix | 6 | one Cucumber scenario (test case) | Cucumber preprocessor messages (NDJSON) |
| `frontend/tests` | `Frontend-unit-tests` shards | 2 | one Vitest test | Vitest JSON report |
| `backend/src/test/java` | `Backend-unit-tests` | 1 | one JUnit test method | Gradle JUnit XML under `backend/build/test-results/test/` |
| `scripts` | `Other-unit-tests` (part 1: Bash script runner) | 1 | one `scripts/test/*.test` file run | Bash runner's tab-separated report |

`cli/tests` has no entry. It shows no CI proof.

## How it is made

1. The test step of each producer part writes its runner's own report. Frontend
   and E2E write to an absolute path under `${RUNNER_TEMP}`, named by an
   environment variable that only the workflow sets: `CI_PROOF_VITEST_REPORT`
   (read in `frontend/vitest.config.ts`), `CI_PROOF_CUCUMBER_MESSAGES`
   (read in `e2e_test/config/ci.ts`) and `CI_PROOF_BASH_REPORT` (read in
   `scripts/test/run_all_script_tests.sh`). Local runs and the Cloud VM write
   no such report. Backend uses Gradle's default JUnit XML directory
   `backend/build/test-results/test/` (one `TEST-*.xml` file per class).
2. The composite action `.github/ci_proof_fragments` converts the report with
   `scripts/ci/ci-proof-outcomes.mjs` into one `<status><TAB><repo-relative
   path>` line per test case and one `<seconds><TAB><repo-relative path>`
   line per timed check. It passes both to the collector with `--by-file`,
   writes the part's fragment and uploads it, kept for 1 day. A producer
   outside a matrix names its part with the action's `part` and `of` inputs;
   otherwise the part number is `strategy.job-index` plus 1, of
   `strategy.job-total`. It runs
   after failing tests too, but not when the test step was skipped, so a part
   whose setup failed shows as missing.
3. The `ci-proof` job runs after the producers, even when they fail. The
   composite action `.github/ci_proof_summary` collects this attempt's
   fragments against `expect` (`e2e_test/features=6 frontend/tests=2
   backend/src/test/java=1 scripts=1`, in `intent.locations` order among proved
   entries) and uploads the summary, kept for 30 days.

## Rules

- **Final attempt only.** Vitest statuses map `passed` and `failed` directly
  and anything else to other. A Vitest file reported `failed` with no test
  results, such as one that failed to load, counts as one failed check. A
  Cucumber test case is judged by its last attempt, using its worst step
  status: passed is passed; skipped or unknown is other; pending, undefined,
  ambiguous or failed is failed. A JUnit `testcase` with a `failure` or
  `error` child is failed; with `skipped` is other; otherwise passed. Nested
  `$` classnames map to the outer class file under `backend/src/test/java`.
  A Bash test file run is passed or failed by its exit status; the whole
  invocation, including work it delegates, is one check. Each report line is
  `<passed|failed><TAB><seconds><TAB><repo-relative path>`.
- **Recorded execution time.** Vitest contributes the duration of each
  assertion that reports one. Cucumber contributes the recorded step
  durations from each scenario's final attempt. JUnit contributes each
  testcase's `time` attribute (seconds). Bash contributes each test file's
  elapsed run time. Failed checks contribute
  their recorded time too. Checks without recorded time, including Vitest
  load failures, still count and are marked untimed. The summary's
  `durationMs` is the sum of recorded durations across present parts. It can
  exceed elapsed CI time when shards run in parallel.
- **The Vitest JSON report is configured, never passed on the command line.**
  A command-line `--reporter` replaces the configured reporters, dropping
  `orchestratorGcReporter`, which keeps Chromium from crashing on CI.
- **Part counts equal producer parts.** `scripts/ci/ci-workflow.test.mjs`
  checks this: a matrix producer's matrix length, or an explicit producer's
  `part` inputs numbered 1 to `of`. It also checks that every producer
  matrix has `fail-fast: false`, so that one failing part does not cancel
  the others' proof, and that `ci-proof` needs every producer job. It runs
  in CI through `pnpm test:ci-proof` in `lint:all`.
- **Per attempt.** After "re-run failed jobs", the new attempt's summary
  holds only the parts that ran again, and the others show as missing.
  Earlier attempts keep their own summaries.
- **Fail loudly.** A broken conversion fails its producer step. A failed
  collector fails the run but no producer job. A missing or ambiguous part is
  reported as such, never filled in.
- **Copied collector.** `scripts/ci/ci-proof-{contract,summary,fragments,collect,figures}.mjs`
  are unchanged copies of Open Dough v0.3.89's `dough-test-optimization`
  scripts, excluded from Biome. Copy them again when a guidance update
  changes them.

## Check a run

```sh
gh api "repos/nerds-odd-e/doughnut/actions/runs/<run-id>/artifacts?per_page=100" \
  --jq '.artifacts[] | "\(.id) \(.name) expired=\(.expired)"'
gh api repos/nerds-odd-e/doughnut/actions/artifacts/<artifact-id>/zip > summary.zip
```

The archive holds only `ci-proof-summary.json`, which names the repository,
run id, attempt and revision. The collector's log prints one line per entry
with its counts and gaps.
