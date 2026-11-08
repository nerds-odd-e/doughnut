---
id: SEED-111
status: active
planted: 2026-11-09
scope: story
---

# See CI results and timings for the remaining script tests in Intent

<a id="run-uncovered-script-tests-in-ci"></a>

### See CI results and timings for the remaining script tests in Intent

**Identity:** SEED-111#run-uncovered-script-tests-in-ci
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal** Doughnut contributors can see whether all 18 currently uncovered Node
test files pass on a selected CI attempt and how much execution time each
file contributed in Intent’s Scripts and Infrastructure group.

**Scope** Run the files listed below in CI and publish their per-file outcomes
and recorded durations through the existing [CI proof summary](../../docs/ci-proof-summary.md).

**Acceptance** A completed CI attempt runs all 18 files. For the same attempt,
Intent and the published summary agree on each file’s results and cumulative
recorded test time. Failed tests contribute their outcomes and available timings.

**Before making the retirement test required** Run `verified idle fixture
reports idle snapshot not a reservation` from
[`scripts/worktree-retirement-checkout-processes.test.mjs`](../../scripts/worktree-retirement-checkout-processes.test.mjs#L177)
repeatedly with the full test group on the intended CI runner. Record the CI
attempts and outcomes, and resolve any recurring failure before making the
test required.

**Evidence** On 2026-11-09, a local Nix run at `afe8472ca0` passed 74 of 75 tests.
The idle retirement case failed when the check could not determine a JVM’s
working directory. Its file passed on its own, and the full rerun passed all
75 tests in 6.6 seconds. The original coverage gap was recorded during
[SEED-109 completion](https://github.com/nerds-odd-e/doughnut/commit/10ac6ef354e15643409df4e7e9e9da3524e7a2a1).

#### Files in scope

- `scripts/dev-healthcheck.test.mjs`
- `scripts/isolated-cypress-openai-mock-cancel.test.mjs`
- `scripts/isolated-cypress-openai-mock-failure.test.mjs`
- `scripts/isolated-cypress-openai-mock-ownership.test.mjs`
- `scripts/isolated-cypress-openai-mock.test.mjs`
- `scripts/isolated-cypress-owning-health.test.mjs`
- `scripts/isolated-openai-mock.test.mjs`
- `scripts/isolated-service-mocks.test.mjs`
- `scripts/local-runtime-target.test.mjs`
- `scripts/profiling/verify-publication-receiver.test.mjs`
- `scripts/sut-owner-application-group.test.mjs`
- `scripts/worktree-reset-isolation-harness-openai-mock.test.mjs`
- `scripts/worktree-reset-isolation-harness-wikidata-mock.test.mjs`
- `scripts/worktree-retirement-checkout-processes.test.mjs`
- `scripts/worktree-retirement-e2e-mutation.test.mjs`
- `scripts/worktree-retirement-evidence.test.mjs`
- `scripts/worktree-retirement-mutation.test.mjs`
- `scripts/worktree-retirement.test.mjs`
