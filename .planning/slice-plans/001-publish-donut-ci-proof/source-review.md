# Source review: Publish Donut CI proof for Intent

**Launch:** a fresh general-purpose reviewer, with no inherited conversation,
started by the execution coordinator on 2026-11-05 during slice 1 acceptance.
**Input:** only the story text of SEED-102#publish-donut-ci-proof (Goal,
Scope, Key examples, Architecture) from
`.planning/seeds/SEED-102-publish-donut-ci-proof.md`, with "No human
amendments". The reviewer was told to read no files and change nothing. The
returned qualifications (2q, 9q, 14q, 17r, 20c), the example note (11e) and
the exclusion 15x stay with their commitments and have no rows.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | A developer viewing a `donut CI` run in Intent → CI status sees results for that run and attempt | slice 3 | |
| 1a.1 | `frontend/tests` checks that passed | slice 3 | |
| 1a.2 | `frontend/tests` checks that failed | slice 3 | |
| 1a.3 | `frontend/tests` checks that did something else | slice 3 | |
| 1a.4 | `e2e_test/features` scenarios that passed | slice 3 | |
| 1a.5 | `e2e_test/features` scenarios that failed | slice 3 | |
| 1a.6 | `e2e_test/features` scenarios that did something else | slice 3 | |
| 1b | The developer sees which CI parts did not report | slice 3 | |
| 2 | The developer can tell from the dashboard what an ordinary CI run actually proved, instead of only the verdict or retained proof | slice 3 | |
| 3.1 | Copy the recipe's four collector modules into `scripts/ci/` | slice 1 | observed: cmp: scripts/ci/ci-proof-{contract,summary,fragments,collect}.mjs identical to .agents/skills/dough-test-optimization/scripts/ (0.3.86) |
| 3.2 | Write one fragment per producer part | slice 2 | |
| 3.3 | Add one collector job that publishes `ci-proof-summary-attempt-<n>` | slice 1 | |
| 4 | Entries and parts are listed in `intent.locations` order | slice 2 | |
| 4.1 | `e2e_test/features` has the six E2E matrix parts | slice 2 | |
| 4.2 | `frontend/tests` has the two Vitest shard parts | slice 1 | observed: ci.yml ci-proof job: ./.github/ci_proof_summary with expect: frontend/tests=2; scripts/ci/ci-workflow.test.mjs 'CI proof expects each entry in as many parts as its producer matrix runs' |
| 5.1 | One outcome line per test case (Vitest) | slice 1 | observed: scripts/ci/ci-proof-outcomes.test.mjs 'a Vitest report gives one repo-relative line per test'; local 2-shard run summarized 9+11=20 passed checks |
| 5.2 | One outcome line per scenario (Cucumber) | slice 2 | |
| 5.3 | Each line uses the final attempt after retries | slice 2 | |
| 5.4 | Each line uses a repository-relative file path | slice 2 | |
| 6.1 | Reports come from each runner's own report output in ordinary CI runs | slice 2 | |
| 6.2 | For Vitest, its JSON report | slice 1 | observed: frontend/vitest.config.ts json reporter with outputFile CI_PROOF_VITEST_REPORT; local CI-profile shard runs wrote the JSON report |
| 6.3 | For Cucumber, the Cucumber preprocessor's messages | slice 2 | |
| 6.4 | Small scripts in `scripts/ci/` convert the reports | slice 2 | |
| 7.1 | The deliberate proof recorders stay untouched | slice 2 | |
| 7.2 | Their opt-in options stay untouched | slice 2 | |
| 7.3 | Committed proof stays untouched | slice 2 | |
| 8.1 | Test selections stay as they are | slice 2 | |
| 8.2 | Sharding stays as it is | slice 2 | |
| 8.3 | Retries stay as they are | slice 2 | |
| 8.4 | Each job's test verdict stays as it is | slice 2 | |
| 9 | The E2E matrix gets `fail-fast: false` | slice 2 | |
| 9.1 | One failing shard no longer cancels the others | slice 3 | |
| 9.2 | The other shards' proof stays available | slice 3 | |
| 10 | A failing run stays failing | slice 3 | |
| 11 | A file reported failed without per-test results counts as one failed check for that file, not as nothing | slice 1 | observed: scripts/ci/ci-proof-outcomes.test.mjs 'a Vitest file that failed to load counts as one failed check' |
| 12.1 | The expected part counts stay equal to the matrix lengths | slice 2 | |
| 12.2 | A test that reads the workflow checks this | slice 2 | |
| 12.3 | That test runs in CI | slice 2 | |
| 13.1 | Summary retention is 30 days | slice 1 | observed: .github/ci_proof_summary/action.yml summary upload retention-days: 30 |
| 13.2 | Fragments are kept for 1 day | slice 2 | |
| 14.1 | Deferred: counts for `scripts` | slice 2 | |
| 14.2 | Deferred: counts for `infra` | slice 2 | |
| 14.3 | Deferred: counts for backend tests | slice 2 | |
| 15 | The summary is per attempt, as the recipe defines it | slice 3 | |
| 15.1 | After "re-run failed jobs", parts not re-run show as missing in the new attempt's summary | slice 3 | |
| E1.1 | All green: the run has one `ci-proof-summary-attempt-1` artifact naming that run, its attempt, and the pushed revision | slice 2 | |
| E1.2 | All green: the tab shows `e2e_test/features` with 6 of 6 parts and its scenario counts | slice 3 | |
| E1.3 | All green: the tab shows `frontend/tests` with 2 of 2 parts and its check counts | slice 3 | |
| E1.4 | All green: all passed | slice 3 | |
| E2.1 | One failing scenario: that shard's job fails and the run is red | slice 3 | |
| E2.2 | One failing scenario: the other five E2E shards still finish | slice 3 | |
| E2.3 | One failing scenario: the summary shows `e2e_test/features` with 1 failed and the passes of all six parts | slice 3 | |
| E3.1 | Part that never ran its tests: that part writes no fragment | slice 2 | |
| E3.2 | Part that never ran its tests: the tab shows that part missing | slice 2 | |
| E3.3 | Part that never ran its tests: its scenarios are not counted as passed | slice 2 | |
| E4.1 | File that fails to load in shard 2: the shard fails | slice 1 | observed: Decisive premise row 1: broken spec in shard 2 gives status failed with 0 assertion results and shard 2 exits 1 |
| E4.2 | File that fails to load: `frontend/tests` shows one failed check for that file, not 0 failed | slice 1 | observed: scripts/ci/ci-proof-outcomes.test.mjs 'a Vitest file that failed to load counts as one failed check' (observed report shape) |
| E5.1 | Rerun: attempt 2 has its own summary | slice 3 | |
| E5.2 | Rerun: the re-run shard and the collector report in it | slice 3 | |
| E5.3 | Rerun: the shards not re-run show as missing in attempt 2 | slice 3 | |
| E5.4 | Rerun: attempt 1's summary is unchanged | slice 3 | |
| 16.1 | CI proof fragments come from the runners' own reports | slice 2 | |
| 16.2 | The proof recorders keep only deliberate retained proof | slice 2 | |
| 16.3 | The proof recorders gain no CI duty | slice 2 | |
| 17.1 | The Vitest JSON report is added beside the existing reporters in `frontend/vitest.config.ts` | slice 1 | observed: frontend/vitest.config.ts reporters: json added after orchestratorGcReporter and VitestProofReporter |
| 17.2 | It is added only on CI | slice 1 | observed: ci.yml Frontend unit tests step sets CI_PROOF_VITEST_REPORT; local run without it wrote no report and kept default output |
| 17.3 | `orchestratorGcReporter` and the proof recorder stay configured on CI | slice 1 | observed: frontend/vitest.config.ts keeps orchestratorGcReporter (isCI) and new VitestProofReporter(); pnpm test:vitest-proof 12/12 pass |
| 18.1 | A broken conversion or collector step fails loudly | slice 1 | observed: scripts/ci/ci-proof-outcomes.mjs reads and parses the report with no catch; .github/ci_proof_fragments/action.yml fragment step has no continue-on-error |
| 18.2 | A failed collector fails the run | slice 3 | |
| 18.3 | A failed collector does not change any producer job's result | slice 1 | observed: ci.yml: ci-proof is its own job that needs Frontend-unit-tests; no producer step depends on it |
| 18.4 | A missing or ambiguous part is reported as such | slice 1 | observed: local summarize over part 1's fragment only printed 'part 2 of 2 missing' with {part:2,status:missing} |
| 18.5 | Such a part is never filled in | slice 1 | observed: local summarize over part 1's fragment only: 9 passed counted, part 2 shown missing with no counts |
| 19.1 | This only adds observation to CI as an execution location | slice 2 | |
| 19.2 | It adds no environment | slice 2 | |
| 20.1 | The part-count check needs a runner that CI actually invokes | slice 2 | |
