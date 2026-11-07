---
id: SEED-106
status: active
planted: 2026-11-07
scope: story
---

# Publish Doughnut test execution times in Intent

<a id="publish-ci-test-execution-times"></a>

### Publish Doughnut test execution times in Intent

**Identity:** SEED-106#publish-ci-test-execution-times
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Beneficiary:** A Doughnut developer investigating the cost of the frontend and end-to-end tests.

**Goal:** Intent shows the cumulative test execution time and file breakdown for the Vitest and Cucumber proof from a CI attempt.

**Scope:** Extend the existing Vitest and Cucumber report conversion to retain recorded
test durations and repository-relative file paths. Each of the two Vitest and
six Cucumber shards keeps a timed fragment. The existing CI collector combines
those fragments into the attempt's single `ci-proof-summary-attempt-<n>`
artifact.

Follow the [installed CI proof recipe](../../.agents/skills/dough-test-optimization/references/ci-proof-summary.md).

**Acceptance:** For one completed run of the judged workflow, the downloaded summary and
Intent agree with the runner reports on counts and cumulative time for both
entries and a file from each. A missing shard stays visible and a shard that
reported failed tests still contributes its available timings.
