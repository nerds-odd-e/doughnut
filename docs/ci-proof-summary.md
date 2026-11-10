# CI proof summary

Every `donut CI` run attempt publishes the artifact
`ci-proof-summary-attempt-<n>`. The Open Dough dashboard's **Intent → CI
status** tab shows counts and recorded test execution time by location and
file for that attempt. It also shows which CI parts did not report.

| Entry | Producer job | Parts | Unit | Runner report |
| --- | --- | --- | --- | --- |
| `e2e_test/features` | `E2E-tests` matrix | 6 | case | Cucumber preprocessor messages (NDJSON) |
| `frontend/tests` | `Frontend-unit-tests` shards | 2 | case | Vitest JSON report |
| `frontend/tests` | The same frontend shards | 2 | file | Vitest failed-file results before assertions |
| `backend/src/test/java` | `Backend-unit-tests` | 1 | case | Gradle JUnit XML under `backend/build/test-results/test/` |
| `scripts` | Wrapped Node commands in `Lint-N-Backend-Generated-Types-For-Frontend`, in `lint:all` order | 13 | case | JSON lines from `scripts/ci/node-test-report.mjs` |
| `scripts` | The same Node commands (parts 1–13) and Bash runner in `Other-unit-tests` (part 14) | 14 | file | Node file-load failures and Bash whole-file outcomes |

`cli/tests` has no entry. It shows no CI proof.

The Lint job's `.github/lint_and_generated_types` action publishes the Node
parts. Existing concern groups run the isolated-browser mocks, local runtime
and development health checks. `test:ci-repair-stash` contributes the reviewed
stash-recovery checks as part 4. `test:worktree-retirement` runs the five
retirement files as part 12, and `test:publication-receiver` runs the profiling
receiver check as part 13. Each Node command publishes both units with the same
part number: actual cases in its case stream and pre-case file-load failures in
its file stream. Bash contributes one file result per invocation of a test file,
regardless of the number of internal assertions. Each unit retains its own
outcomes, file breakdown and recorded times.

## How it is made

1. The test step of each producer part writes its runner's own report. Frontend
   and E2E write to an absolute path under `${RUNNER_TEMP}`, named by an
   environment variable that only the workflow sets: `CI_PROOF_VITEST_REPORT`
   (read in `frontend/vitest.config.ts`), `CI_PROOF_CUCUMBER_MESSAGES`
   (read in `e2e_test/config/ci.ts`) and `CI_PROOF_BASH_REPORT` (read in
   `scripts/test/run_all_script_tests.sh`). `CI_PROOF_NODE_REPORT_DIR`, set
   on the `pnpm lint:all` step and read in `scripts/ci/node-test.mjs`, names a
   directory where each Node test command writes
   `<package script>.ndjson`. Local runs and the Cloud VM write
   no such report. Backend uses Gradle's default JUnit XML directory
   `backend/build/test-results/test/` (one `TEST-*.xml` file per class).
2. The composite action `.github/ci_proof_fragments` converts the report with
   `scripts/ci/ci-proof-outcomes.mjs` into one `<status><TAB><repo-relative
   path>` line per reported result in its declared unit and one `<seconds><TAB><repo-relative path>`
   line per timed check. It passes both to the collector with `--by-file`,
   writes the part's fragment and uploads it, kept for 1 day. Producers
   pass their declared `case` or `file` unit through conversion and collection;
   artifact names are `ci-proof-fragments-<job>-<unit>-<part>-attempt-<n>`, so paired streams stay distinct.
   An existing Node or Vitest report with no file outcomes writes a present zero-result
   file fragment. An absent report leaves both declared parts missing. A producer
   outside a matrix names its part with the action's `part` and `of` inputs;
   otherwise the part number is `strategy.job-index` plus 1, of
   `strategy.job-total`. It runs
   after failing tests too, but not when the test step was skipped, so a part
   whose setup failed shows as missing. Each call stages its fragment afresh,
   so a job with several parts uploads each part once. Within `lint:all`, a
   failing command stops the later ones; their reports are absent, their
   conversions fail, and their parts show as missing while earlier parts
   publish. A command's checks outside `scripts/` (such as `e2e_test/`
   files in `test:browser-worktree-isolation`) still decide its verdict but
   prove no entry. Commands with no Node wrapper, such as
   `test:gherkin-validation` and the linters, publish nothing.
3. The `ci-proof` job runs after the producers, even when they fail. The
   composite action `.github/ci_proof_summary` collects this attempt's
   fragments against `expect` (`e2e_test/features=6:case frontend/tests=2:case frontend/tests=2:file
   backend/src/test/java=1:case scripts=13:case scripts=14:file`, in `intent.locations` order among proved
   entries) and uploads the summary, kept for 30 days.

## Rules

- **Final attempt only.** Vitest statuses map `passed` and `failed` directly
  and anything else to other. A Vitest file reported `failed` with no test
  results, such as one that failed to load, counts as one failed file result. A
  Cucumber test case is judged by its last attempt, using its worst step
  status: passed is passed; skipped or unknown is other; pending, undefined,
  ambiguous or failed is failed. A JUnit `testcase` with a `failure` or
  `error` child is failed; with `skipped` is other; otherwise passed. Nested
  `$` classnames map to the outer class file under `backend/src/test/java`.
  A Bash test file run is passed or failed by its exit status; the whole
  invocation, including work it delegates, is one check. Each report line is
  `<passed|failed><TAB><seconds><TAB><repo-relative path>`. A Node test case
  (leaf test) is one check on its selected file's path: skip and todo are
  other. A suite or parent test is a check only when it fails for its own
  reason, and is untimed because its duration covers its subtests. A file
  that fails to load is one failed file result, with its reported duration. Case
  results do not imply an additional successful-file result. Tests launched by a test's own
  child run stay out of the report.
- **Recorded execution time.** Vitest contributes the duration of each
  assertion that reports one. Cucumber contributes the recorded step
  durations from each scenario's final attempt. JUnit contributes each
  testcase's `time` attribute (seconds). Bash contributes each test file's
  elapsed run time. Node contributes each case or file failure's reported
  duration. A Vitest pre-case file failure contributes its reported
  `endTime - startTime`, including zero; missing timestamps leave it untimed.
  Failed results contribute their recorded time too. Results without recorded
  time still count and are marked untimed. Each unit's `durationMs` is the sum
  of recorded durations across its present parts. It can
  exceed elapsed CI time when shards run in parallel.
- **The Vitest JSON report is configured, never passed on the command line.**
  A command-line `--reporter` replaces the configured reporters, dropping
  `orchestratorGcReporter`, which keeps Chromium from crashing on CI.
- **Part counts equal producer parts.** `scripts/ci/ci-workflow.test.mjs`
  checks this: a matrix producer's matrix length, or an explicit producer's
  `part` inputs numbered 1 to `of`, and that each Node test command in
  `lint:all` has its own `scripts` part in `lint:all` order. It also checks that every producer
  matrix has `fail-fast: false`, so that one failing part does not cancel
  the others' proof, and that `ci-proof` needs every producer job. It runs
  in CI through `pnpm test:ci-proof` in `lint:all`.
- **Reader comparison.** The released Intent reader treats unit-less builds as
  legacy test results. Count and time differences appear only when both builds
  declare matching units.
- **Per attempt.** After "re-run failed jobs", the new attempt's summary
  holds only the parts that ran again, and the others show as missing.
  Earlier attempts keep their own summaries.
- **Fail loudly.** A broken conversion fails its producer step. A failed
  collector fails the run but no producer job. A missing or ambiguous part is
  reported as such, never filled in.
- **Copied collector.** `scripts/ci/ci-proof-{contract,summary,fragments,collect,figures,options,evidence}.mjs`
  are unchanged copies of Open Dough v0.3.94's `dough-test-optimization`
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
