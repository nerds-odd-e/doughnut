---
id: SEED-112
status: active
planted: 2026-11-09
scope: story
---

# See Doughnut test cases and test files in Intent

<a id="publish-explicit-ci-test-counts"></a>

### See Doughnut test cases and test files in Intent

**Identity:** SEED-112#publish-explicit-ci-test-counts
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/086-publish-explicit-ci-test-counts/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"cf7e8ed5bb1d6a40c939431fcde9ac4db1857d6a991a5224bbdae1242924142f","plan":"0fb9382e3551174638149c9c3e6b38fe32d7025ea2a105c9a220657e996d6e63"}}
```

**Goal.** Doughnut contributors comparing CI builds can distinguish changes to the
number of test cases from changes to the number of test files in Intent, with
outcomes and execution times attributed to the unit the runner actually reports.

**Scope.** Adopt the new CI proof format in the existing Doughnut publisher. Publish
individual JUnit, Vitest, Cucumber, and Node results as test cases and the
Bash runner’s whole-file results as test files. Keep the Scripts and
Infrastructure proof location, its file breakdown, outcomes, and recorded
execution times. A file that fails to load before producing any cases reports
one failed test file, with its available timing. Vitest and Node producers
therefore publish separate case and file streams, including zero-result
fragments when a report contains no results in one declared unit. Keep the
existing proved locations and producer commands; adding proof for further
suites or changing Intent itself is outside this story.

Open Dough 0.3.93 publishes these units; the project already has 0.3.94
installed. Use guidance at least that recent and copy all seven `ci-proof-*.mjs`
collector modules into `scripts/ci/` through the CI proof copy recipe
(`ci-proof-evidence.mjs` and `ci-proof-options.mjs` are new). Give
`.github/ci_proof_fragments` a unit input passed as `--unit case|file`, and
declare each `.github/ci_proof_summary` expectation as
`<entry>=<parts>:<unit>`, numbering each unit's parts separately. A part
that wrote no fragment stays a missing part in its unit; do not extract test
cases from the Bash runner's whole-file report. Until both compared builds
declare matching units, Intent reads unit-less results as legacy test
results and shows no count or time difference.

**Key examples.**

- Given Node reports containing individual case outcomes and a Bash report
  containing whole-file outcomes for one attempt, publishing CI proof gives
  the Scripts and Infrastructure location separate totals for test cases and
  test files, each with its own outcomes, file breakdown, and recorded time.
  A Bash file with several internal assertions contributes one file result.
- Given the existing CI producer layout, declare
  `e2e_test/features=6:case frontend/tests=2:case frontend/tests=2:file
  backend/src/test/java=1:case scripts=12:case scripts=13:file`, in location
  order. Each of the twelve `lint:all` Node reports owns parts 1–12 in both
  scripts units; `run_all_script_tests.sh` owns file part 13. Each frontend
  shard owns the same part number in its case and file streams.
- Given a producer that writes no report, collecting the attempt leaves that
  part missing in its declared unit while retaining proof from the other
  parts and units.
- Given reports of individual JUnit, Vitest, and Cucumber cases, publishing
  CI proof retains their case outcomes, source-file attribution, recorded
  execution times, and untimed results.
- Given a file that fails before yielding cases, publishing CI proof keeps
  one failed test file visible at that path, with available timing retained.
  A report with two real case outcomes and one such failure yields two case
  results and one file result. A report with only case outcomes yields a
  present, zero-result file fragment; a missing report leaves both declared
  parts missing.
- Given a comparison with an older unit-less build, Intent retains legacy
  test results and shows no count or time difference until both builds
  declare matching units. This is supplied by the released reader contract;
  the story changes Doughnut's publisher.

**Decision.** On 2026-11-10 the developer delegated the load-failure unit
decision. Publish those failures as test files so case counts represent actual
cases. File streams describe the aggregate outcomes the runners report;
case reports do not create additional successful-file results.
