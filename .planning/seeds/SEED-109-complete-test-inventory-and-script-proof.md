---
id: SEED-109
status: active
planted: 2026-11-08
scope: story
---

# See CLI tests and timed scripts and infrastructure results in Intent

<a id="complete-test-inventory-and-script-proof"></a>

### See CLI tests and timed scripts and infrastructure results in Intent

**Identity:** SEED-109#complete-test-inventory-and-script-proof
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Beneficiary** A Doughnut developer checking which tests Intent discovers and
what a CI attempt proved for scripts and infrastructure.

**Current behavior** `intent.locations` omits the CLI and lists `scripts` and
`infra` separately as Node.js test locations. CI already runs the CLI Vitest
suite, the Bash checks in `scripts/test/*.test`, and infrastructure Node.js
tests. Neither scripts nor infrastructure has an entry in the CI proof summary.

**Goal** Intent discovers the CLI suite and shows one Scripts and Infrastructure
group with CI results and recorded test execution time for Bash and Node.js.

**Scope** Register the existing CLI Vitest suite in `.planning/open-dough.json`.
Combine `scripts` and `infra` into one logical test group in Intent and CI
proof, preserving repository-relative file paths under both trees. Include the
Bash tests in discovery and publish the group's Bash and Node.js outcomes and
recorded durations through the [existing CI proof summary](../../docs/ci-proof-summary.md).

**Acceptance** Intent lists CLI Vitest files and shows one Scripts and
Infrastructure group containing Bash tests and Node.js tests from both trees.
For a CI attempt that runs those checks, the proof artifact and Intent show
matching counts and cumulative recorded time for the group and its files.
Failed checks contribute their available results and timings, and a missing
report is visible as missing proof.
