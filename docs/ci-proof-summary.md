# CI proof summary

Every `donut CI` run attempt publishes the artifact
`ci-proof-summary-attempt-<n>`. The Open Dough dashboard's **Intent → CI
status** tab reads it to show, for that run and attempt, how many checks in
each Intent location passed, failed or did something else, and which CI parts
did not report.

| Entry | Producer job | Parts | Unit | Runner report |
| --- | --- | --- | --- | --- |
| `e2e_test/features` | `E2E-tests` matrix | 6 | one Cucumber scenario (test case) | Cucumber preprocessor messages (NDJSON) |
| `frontend/tests` | `Frontend-unit-tests` shards | 2 | one Vitest test | Vitest JSON report |

`scripts`, `infra` and backend tests have no entry. They show no CI proof.

## How it is made

1. The test step of each producer part writes its runner's own report to an
   absolute path under `${RUNNER_TEMP}`, named by an environment variable that
   only the workflow sets: `CI_PROOF_VITEST_REPORT` (read in
   `frontend/vitest.config.ts`) and `CI_PROOF_CUCUMBER_MESSAGES` (read in
   `e2e_test/config/ci.ts`). Local runs and the Cloud VM write no such
   report.
2. The composite action `.github/ci_proof_fragments` converts the report with
   `scripts/ci/ci-proof-outcomes.mjs` into one `<status><TAB><repo-relative
   path>` line per test case. It then writes the part's fragment and uploads
   it, kept for 1 day. The part number is `strategy.job-index` plus 1. It runs
   after failing tests too, but not when the test step was skipped, so a part
   whose setup failed shows as missing.
3. The `ci-proof` job runs after both producers, even when they fail. The
   composite action `.github/ci_proof_summary` collects this attempt's
   fragments against `expect` (`e2e_test/features=6 frontend/tests=2`, in
   `intent.locations` order) and uploads the summary, kept for 30 days.

## Rules

- **Final attempt only.** Vitest statuses map `passed` and `failed` directly
  and anything else to other. A Vitest file reported `failed` with no test
  results, such as one that failed to load, counts as one failed check. A
  Cucumber test case is judged by its last attempt, using its worst step
  status: passed is passed; skipped or unknown is other; pending, undefined,
  ambiguous or failed is failed.
- **The Vitest JSON report is configured, never passed on the command line.**
  A command-line `--reporter` replaces the configured reporters, dropping
  `orchestratorGcReporter`, which keeps Chromium from crashing on CI.
- **Part counts equal matrix lengths.** `scripts/ci/ci-workflow.test.mjs`
  checks this. It also checks that every producer matrix has
  `fail-fast: false`, so that one failing part does not cancel the others'
  proof. It runs in CI through `pnpm test:ci-proof` in `lint:all`.
- **Per attempt.** After "re-run failed jobs", the new attempt's summary
  holds only the parts that ran again, and the others show as missing.
  Earlier attempts keep their own summaries.
- **Fail loudly.** A broken conversion fails its producer step. A failed
  collector fails the run but no producer job. A missing or ambiguous part is
  reported as such, never filled in.
- **Copied collector.** `scripts/ci/ci-proof-{contract,summary,fragments,collect}.mjs`
  are unchanged copies of the installed Open Dough `dough-test-optimization`
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
