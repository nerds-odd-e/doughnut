---
id: SEED-102
status: active
planted: 2026-11-05
planted_during: Terry Yin's request to prioritize CI proof adoption
trigger_when: A Donut developer reviews the proof produced by the sharded CI workflow
scope: story
---

# SEED-102: Publish Donut CI proof for Intent

<a id="publish-donut-ci-proof"></a>

### Publish Donut CI proof for Intent

**Identity:** SEED-102#publish-donut-ci-proof
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/001-publish-donut-ci-proof/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"f9a9e5bbfc83f45c12aca4d55517a1533c7a1b5f93a1c7257cc5d2b9e53e0c5c","plan":"0719dc8034f73c65c344bdc21635d44ec08b0695fa1fc0ac521c21270be37133"}}
```

**Goal:** A Donut developer looking at a `donut CI` run in the Open Dough
dashboard's **Intent → CI status** tab sees, for that run and attempt, how many
frontend unit checks (`frontend/tests`) and end-to-end scenarios
(`e2e_test/features`) passed, failed, or did something else, and which CI parts
did not report. The developer can then tell from the dashboard what an ordinary
CI run actually proved, instead of reading only the run's red or green verdict
or relying on deliberately retained proof.

**Scope:**

- Follow the [installed CI proof recipe](../../.agents/skills/dough-test-optimization/references/ci-proof-summary.md)
  in [Donut's CI workflow](../../.github/workflows/ci.yml): copy its four
  collector modules into `scripts/ci/`, write one fragment per producer part,
  and add one collector job that publishes `ci-proof-summary-attempt-<n>`.
- Entries and parts, in `intent.locations` order: `e2e_test/features` with
  the six E2E matrix parts, and `frontend/tests` with the two Vitest shard
  parts. Each part's fragment holds one outcome line per test case (Vitest)
  or scenario (Cucumber), using its final attempt after retries and a
  repository-relative file path.
- The reports come from each runner's own report output in ordinary CI runs
  (Vitest's JSON report and the Cucumber preprocessor's messages), converted
  by small scripts in `scripts/ci/`. The deliberate proof recorders, their
  opt-in options, and committed proof stay untouched.
- Test selections, sharding, retries, and each job's test verdict stay as
  they are. The E2E matrix gets `fail-fast: false`, so that one failing shard
  no longer cancels the others and their proof stays available. A failing run
  stays failing. This costs more runner time on failing runs.
- A test file that the runner reports as failed without any per-test result
  (for example, a Vitest file that failed to load) counts as one failed check
  for that file, not as nothing.
- The expected part counts stay equal to the matrix lengths, checked by a test
  that reads the workflow and runs in CI.
- Summary retention is 30 days. Fragments are kept for 1 day.
- Deferred: counts for `scripts` and `infra` (they run in the Other Unit
  Tests job without a per-test report) and for backend tests, which have no
  Intent location. These locations show no CI proof until a later story adds
  them.
- Boundary: the summary is per attempt, as the recipe defines it. After
  "re-run failed jobs", the parts that were not re-run show as missing in the
  new attempt's summary. Changing that would change the shared recipe, not
  Donut.

**Key examples:**

1. **All green.** A push to `main` runs `donut CI` and every test passes →
   the run has one `ci-proof-summary-attempt-1` artifact naming that run, its
   attempt, and the pushed revision → the CI status tab for that run shows
   `e2e_test/features` with 6 of 6 parts and its scenario counts, and
   `frontend/tests` with 2 of 2 parts and its check counts, all passed.
2. **One failing scenario.** A scenario in the `note_creation_and_update`
   shard fails on both of its attempts → that shard's job fails and the run
   is red. The other five E2E shards still finish. The summary shows
   `e2e_test/features` with 1 failed and the passes of all six parts.
3. **A part that never ran its tests.** An E2E shard's MySQL setup step
   fails before Cypress starts → that part writes no fragment → the tab shows
   `e2e_test/features` with that part missing, and does not count its
   scenarios as passed.
4. **A file that fails to load.** A Vitest spec file fails to import in shard
   2 → the shard fails → `frontend/tests` shows one failed check for that
   file, not 0 failed.
5. **Rerun.** The developer re-runs the failed E2E shard from example 2, and
   it passes → attempt 2 has its own summary. The re-run shard and the
   collector report in it. The five shards that were not re-run show as
   missing in attempt 2. Attempt 1's summary is unchanged.

**Architecture:**

- *Observation is separate from retained proof.* CI proof fragments come from
  the runners' own reports. The project-owned proof recorders
  (`frontend/vitestProofReporter.ts`, `e2e_test/config/cucumberProof*.mjs`)
  keep only deliberate retained proof and gain no CI duty. This keeps one
  concept in one place.
- *The Vitest report goes in the configuration, not on the command line.* A
  command-line `--reporter` replaces the configured reporters. On CI that
  would drop `orchestratorGcReporter`, which keeps Chromium from crashing, and
  the proof recorder. So the JSON report is added beside them in
  `frontend/vitest.config.ts`, and only on CI.
- *Failure handling (ADR 0006).* A broken conversion or collector step fails
  loudly. A failed collector fails the run but does not change any producer
  job's result. A missing or ambiguous part is reported as such, never filled
  in.
- *Environments (ADR 0007).* This only adds observation to CI as an execution
  location. It adds no environment.
- *Workflow tests are not wired into CI today.* `scripts/ci/*.test.mjs` is not
  run by any package script or CI job. The part-count check needs a runner
  that CI actually invokes.
