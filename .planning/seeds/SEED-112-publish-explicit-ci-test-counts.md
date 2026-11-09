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

Adopt once Open Dough publishes the explicit test case and test file units
and Intent can read the new format. See the [Open Dough protocol story](https://github.com/terryyin/open-dough/blob/b8fdb3a361899488e12828383afdea02cb545fda/.planning/seeds/SEED-281-distinguish-test-cases-and-files-in-ci-proof.md#distinguish-test-cases-and-files-in-ci-proof).

**Key examples.**

- For one CI attempt, the Scripts and Infrastructure location shows separate
  totals for the Node test cases and the Bash test files. A Bash file with
  several internal assertions contributes one file result.
- The published proof and Intent agree with the case reports for the other
  suites. A file that fails before yielding cases remains visible as a file
  failure, with available timing retained.
