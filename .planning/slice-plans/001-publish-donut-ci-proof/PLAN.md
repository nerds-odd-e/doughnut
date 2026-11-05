# Publish Donut CI proof for Intent

## Source

- **Story:** [Publish Donut CI proof for Intent](../../seeds/SEED-102-publish-donut-ci-proof.md#publish-donut-ci-proof)
- **Identity:** SEED-102#publish-donut-ci-proof
- **Recipe:** [CI proof summary](../../../.agents/skills/dough-test-optimization/references/ci-proof-summary.md)
  (Open Dough guidance 0.3.84)

## Goal and scope

In the Open Dough dashboard's **Intent → CI status** tab, the developer sees
`e2e_test/features` and `frontend/tests` totals for each `donut CI` run and
attempt, with any part that did not report shown as a gap. Scope, deferred
locations, and the per-attempt boundary are in the story. This plan adds no
promise.

**Exclusions:** `scripts`, `infra`, backend tests; changes to the shared
collector modules (copied unchanged); the deliberate proof recorders and
committed proof (left untouched); existing CI verdict gaps found during
planning (see Learnings).

## Current decisions

- **Runner reports, not the proof recorders.** Vitest's built-in JSON reporter
  and the Cucumber preprocessor's messages report, both switched on only by a
  workflow-set environment variable that names an absolute output path under
  `${RUNNER_TEMP}`. Local runs, the Cloud VM (`CI=1`), and the proof options are
  unaffected, and no report lands in the checkout.
- **The Vitest report is added in `frontend/vitest.config.ts`**, beside the
  existing reporters, never with a command-line `--reporter` (which replaces
  `orchestratorGcReporter` and the proof recorder). The Cucumber report is
  added through `expose` in `e2e_test/config/ci.ts`.
- **One converter module,** `scripts/ci/ci-proof-outcomes.mjs`, turns either
  report into the recipe's `<status><TAB><repo-relative path>` lines, one per
  test case or scenario, using the final attempt. A Vitest file that is
  `failed` with no test results gives one `failed` line for that file. An
  absent report gives no fragment, so the collector shows that part as missing.
- **The collector modules** go in `scripts/ci/` beside it, copied unchanged.
- **E2E part numbers** come from `strategy.job-index` plus 1, computed in the
  shell, because GitHub expressions have no arithmetic and the matrix values
  are spec globs.
- **The part-count check** goes in `scripts/ci/ci-workflow.test.mjs`, which
  already reads `ci.yml`. It runs in CI through a new `test:ci-proof` package
  script added to `lint:all`, together with the converter's tests.
- **Actions pins:** `actions/upload-artifact@v7` and
  `actions/download-artifact@v8`, as the project already pins. The collector
  job uses `actions/setup-node@v7` with `vars.NODE_VERSION` (26) and installs
  no dependencies.
- **Retention:** summary 30 days, fragments 1 day.
- **Mode:** Story Branch Mode. `donut CI` runs on every branch push, so the
  real-run acceptance happens on the story branch before landing.

## Decisive premises

| Premise | Consuming operation | Observation (form, profile, revision) | Result | Remainder |
| --- | --- | --- | --- | --- |
| A config-level Vitest JSON report under the CI profile gives per-test status with file paths, keeps existing output, and shows a failed-to-load file | Slice 1 converter on the shard report | Scratch edit at 842901fd2f in this workspace: `["json", { outputFile }]` added to `reporters` in `frontend/vitest.config.ts`, then `env -u NODE_ENV CI=true FRONTEND_UT_CONSOLE_OUTPUT_AS_FAILURE=true nix develop -c pnpm exec vitest run --shard=N/2 <two specs> tests/zzScratchBroken.spec.ts` for N = 1, 2 (2026-11-05). Reverted. | Default output kept. `testResults[].name` is an absolute path. `assertionResults[].status` is per test, including the touch instance. The broken file appears as `status: failed` with 0 assertion results, and shard 2 exits 1. | macOS, not the Ubuntu runner; the report format does not depend on the OS. Confirmed on the real run in slice 1. |
| Cucumber messages turned on through `expose` in `ci.ts`, with `RECORD_CUCUMBER_PROOF` unset, give scenario paths and final attempts | Slice 2 converter on the shard report | Scratch `expose` `{ messagesEnabled: true, messagesOutput }` in `e2e_test/config/ci.ts` at 842901fd2f, `SUT_TIMEOUT_MS=360000 nix develop -c pnpm cy:run --spec "e2e_test/features/testability/cypress_retry.feature,e2e_test/features/users/user_profile.feature"` (2026-11-05). Reverted. | NDJSON written. `pickle.uri` is repository-relative (`e2e_test/features/...`). The retry scenario has attempt 0 `willBeRetried: true` with a failed step, then attempt 1 passed. Outline examples are separate pickles (3 test cases in `user_profile.feature`). | The CI form adds `--browser chrome` and the built runtime target. Both are browser- or app-side; the messages are written on the Node side by the preprocessor. Confirmed on the real run in slice 2. |
| `pnpm lint:all` runs its `test:*` scripts in CI | Slice 1 part-count check | CI log of the `Linting & Types Gen for Frontend` job, run 41948058190 at 61c0a9ab7d (2026-11-05) | The `lint:all` chain runs `test:cucumber-proof` and others, with `node --test` totals printed | none |
| The developer's `gh` can list and download this repository's run artifacts | Slice 3 acceptance and the dashboard tab | `gh api "repos/nerds-odd-e/doughnut/actions/runs/41948058190/artifacts?per_page=100"` (2026-11-05) | Lists `frontend-dist`, `donut-0.0.1-SNAPSHOT.jar`, `cli-dist` | The zip download is first exercised in slice 3 |
| `scripts/ci/ci-workflow.test.mjs` passes today and reads `ci.yml` | Slice 1 extends it | `nix develop -c node --test scripts/ci/ci-workflow.test.mjs` at 842901fd2f | 1 test passes | none |

## Outside-in proof

| Promise (story example) | Owner | Proof |
| --- | --- | --- |
| Example 1: one readable summary per run and attempt, with `frontend/tests` 2/2 parts | Slice 1 | Real run on the story branch: the artifact list names `ci-proof-summary-attempt-1`, and its JSON's repository, run id, attempt, and revision equal the run's, with `frontend/tests` checks equal to the two shard logs' Vitest totals |
| Example 1: `e2e_test/features` 6/6 parts | Slice 2 | Same real-run check, with scenario totals equal to the six shard logs' Cypress totals |
| Example 1: the dashboard shows those totals for the same run | Slice 3 | The CI status tab for that run shows both entries' counts |
| Example 2: one failing scenario keeps the other shards' proof | Slices 2 and 3 | Converter test (a failing final attempt gives a `failed` line); workflow test (E2E and frontend matrices do not fail fast); real failing run in slice 3 shows the failed count and 6/6 parts |
| Example 3: a part with no fragment is a gap, not a pass | Slices 1 and 2 | Local: `summarize` over only part 1's fragment reports part 2 missing and counts nothing for it. The workflow's fragment step is skipped when the test step was skipped. |
| Example 4: a failed-to-load Vitest file counts as one failed check | Slice 1 | Converter test with the observed report shape |
| Example 5: rerun gives attempt 2 its own summary | Slice 3 | "Re-run failed jobs" on the slice 3 failing run: an `attempt-2` summary exists, and attempt 1's is unchanged |
| Part counts stay equal to the matrix lengths | Slices 1 and 2 | `scripts/ci/ci-workflow.test.mjs`, run in CI through `lint:all` |
| Retained proof untouched | Slices 1 and 2 | `pnpm test:vitest-proof` and `pnpm test:cucumber-proof` stay green; `git diff` touches no proof file or recorder |

## Slices

### 1. A CI run publishes a summary with frontend unit totals

Type: Behavior
Status: planned
Proof: `node --test scripts/ci/ci-workflow.test.mjs scripts/ci/ci-proof-outcomes.test.mjs`; a local CI-profile shard run converted, turned into a fragment, and summarized; the real run's summary artifact (example 1, frontend).

Behavior: a push to the story branch → `donut CI` runs both Vitest shards,
each writing its report and fragment even when its tests fail → the
`ci-proof` collector job publishes `ci-proof-summary-attempt-<n>`, expecting
`frontend/tests=2`.

Work: copy the four collector modules into `scripts/ci/`; add the
environment-gated JSON reporter; add the `vitest` conversion and its tests
(passed, failed, skipped or todo as other, failed-to-load file);
`CI_PROOF_REVISION` at workflow level; an `id` on the frontend test step plus
the fragment and upload steps; the collector job; the part-count and
no-fail-fast assertions in `ci-workflow.test.mjs`; `test:ci-proof` in
`lint:all`.

Sizing: about 10 minutes of local work plus one external CI wait, the stated
exception to the slice target.

### 2. The summary adds end-to-end totals from all six shards

Type: Behavior
Status: planned
Proof: converter tests (final attempt after a retry, an all-skipped scenario as other); a local `pnpm cy:run` of `cypress_retry.feature` and `user_profile.feature` with the variable set, converted to 4 passed lines and then a fragment; the real run's summary shows 6/6 parts (example 1, E2E).

Behavior: a push → each E2E shard writes its Cucumber messages report and
fragment, even after a failing scenario → the summary lists
`e2e_test/features=6` before `frontend/tests=2`.

Work: environment-gated `messagesEnabled` and `messagesOutput` in
`e2e_test/config/ci.ts`; the `cucumber` conversion and its tests; on the E2E
matrix, `fail-fast: false`, a step `id`, and fragment and upload steps (part
from the job index); add `E2E-tests` to the collector's `needs` and `--expect`;
extend the workflow test.

Sizing: about 8 minutes of local work plus the local E2E run and one CI wait.

### 3. The dashboard shows the totals, including for a failing run and its rerun

Type: Behavior
Status: planned
Proof: the dashboard tab for slice 2's green run (example 1). Then a temporary commit on the story branch that makes `cypress_retry.feature` fail on both attempts and one frontend spec fail, pushed: its summary shows the failed counts with all parts present (example 2). "Re-run failed jobs" on that run gives an `attempt-2` summary (example 5). Then the temporary commit is reverted and the revert pushed, and that run is green.

Behavior: the developer opens Intent → CI status for a run → the tab shows
both entries' totals and gaps for that run and attempt.

Work: no product change. Observe the tab in the dashboard at
`http://127.0.0.1:4173`, through browser tools or, if they cannot reach it,
in one developer round that names the run and the expected counts. The
temporary failing commit and its revert are execution scaffolding and leave
no trace in the final diff.

Sizing: external CI waits dominate (two runs plus one rerun).

## Concern evidence

Slice-plan refinement was not needed: each slice owns one outcome with its own
proof loop, and the sizes are stated with their external-wait exception. This
review found no remaining slice-specific concern.

## Learnings

- A Gherkin syntax error in a selected feature file makes Cypress silently
  skip that spec. The run passes and the messages report has no trace of it
  (observed by appending an `Examples` block without an outline to
  `cypress_retry.feature`, scratch, reverted). The CI summary cannot show this
  as a failure, because no runner report carries it. This is an existing CI
  verdict gap, outside this story.
- The `cypress-screenshots` upload in the E2E matrix uses one artifact name
  for all shards. From version 4 on, `upload-artifact` requires unique names
  within a run, so a second failing shard's upload would fail. This comes from
  the action's documented behavior and was not observed here. It is existing
  behavior that this story does not change. It may show up more often once
  `fail-fast: false` lets several shards fail in one run. Slice 3 fails only
  one E2E shard.
