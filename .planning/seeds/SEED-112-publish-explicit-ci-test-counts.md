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
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal.** Doughnut contributors comparing CI builds can distinguish changes to the
number of test cases from changes to the number of test files.

**Scope.** Adopt the new CI proof format in the existing Doughnut publisher. Publish
individual JUnit, Vitest, Cucumber, and Node results as test cases and the
Bash runner’s whole-file results as test files. Keep the Scripts and
Infrastructure proof location, its file breakdown, outcomes, and recorded
execution times.

Open Dough 0.3.93 publishes these units. Update Open Dough guidance to
0.3.93 or later with `dough-update`, then copy all seven `ci-proof-*.mjs`
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

- For one CI attempt, the Scripts and Infrastructure location shows separate
  totals for the Node test cases and the Bash test files. A Bash file with
  several internal assertions contributes one file result.
- The `scripts` expectation becomes `scripts=12:case scripts=1:file`: the
  twelve `lint:all` Node reports are case parts 1–12 and
  `run_all_script_tests.sh` is file part 1. The other entries declare
  `e2e_test/features=6:case`, `frontend/tests=2:case` and
  `backend/src/test/java=1:case`.
- The published proof and Intent agree with the case reports for the other
  suites. A file that fails before yielding cases remains visible as a file
  failure, with available timing retained.
