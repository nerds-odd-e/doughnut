# Remove local proof from Donut

## Source

- **Story:** [Remove local proof from Donut](../../seeds/SEED-104-remove-local-proof.md#remove-local-proof)
- **Identity:** SEED-104#remove-local-proof
- **Depends on:** [Publish Donut CI proof for Intent](../../seeds/SEED-102-publish-donut-ci-proof.md#publish-donut-ci-proof)
  (recorded as a waiting dependency in the story). Execution starts on a trunk
  that already has SEED-102's CI reports, converter, collector and tests.
**Source review:** [source-review.md](source-review.md)

## Goal and scope

Ordinary Cypress/Cucumber, Vitest and `node:test` runs and the `donut CI` proof
summary work as before, and the repository has no local proof recorders,
stores, committed snapshots, settings, scripts or documentation. Scope,
required survivors and scope limits are in the story. This plan adds no
promise.

**Exclusions:** Open Dough's managed guidance copies under
`.agents/skills/dough-*` (for example
`dough-test-optimization/references/*-proof.md`), which arrive through a
released Open Dough update; dated findings in `DearDough.md` and
`DonutRetrospectiveFindings.md`, which keep their provenance; other meanings of
"proof" (acceptance evidence, the CI proof summary,
`scripts/mcp-isolation-proof.mjs`, the `frontend` skill's "Frontend proof"
typecheck rule).

## Current decisions

- **One slice per runner,** each deleting its recorder, tests, fixtures,
  committed snapshot, `.planning/open-dough.json` `proof`/`proofGeneration`
  keys, package script, ignore and formatter entries, and its documentation
  paragraph together, so every commit leaves a consistent repository.
  `e2e_test/config/proofStore.mjs` goes in the last slice, with its last
  consumer.
- **Gherkin validation stays.** `e2e_test/config/common.ts` calls
  `validateCucumberFeatures(config)` and then the preprocessor's
  `addCucumberPreprocessorPlugin(on, config)` directly: the exact branch the
  proof wrapper takes today whenever recording is off.
- **Its maintained test is a direct node:test test** of
  `validateCucumberFeatures` in `e2e_test/config/validateCucumberFeatures.test.mjs`
  (parse error with line and text, a feature with no scenarios, a basename-only
  `excludeSpecPattern`, `node_modules` ignored, and a valid set passing). It
  replaces the real-Cypress validation case, which depends on the proof test
  project and runner fixture. The one-line wiring in `common.ts` is proven by
  the slice's acceptance run on the Donut Cypress config. A `test:gherkin-validation`
  package script runs it and takes `test:cucumber-proof`'s place in `lint:all`.
- **Local verification** is the affected runner's ordinary command plus the
  remaining `test:*` scripts the slice touches. Hosted CI runs the full
  `lint:all`, E2E and frontend shards after publication.
- **Mode:** Story Branch Mode. `donut CI` runs on every branch push, so the CI
  proof summary check (example 5) happens on the story branch before landing.

## Decisive premises

| Premise | Consuming operation | Observation (form, profile, revision) | Result | Remainder |
| --- | --- | --- | --- | --- |
| With `RECORD_CUCUMBER_PROOF` unset, the proof wrapper already runs only `validateCucumberFeatures` then the plain `addCucumberPreprocessorPlugin(on, config)` | Slice 1 rewiring in `common.ts` | `env -u RECORD_CUCUMBER_PROOF CURSOR_DEV=true nix develop -c node --input-type=module -e` importing `createCucumberProofRecorder` from `e2e_test/config/cucumberProofRecorder.mjs` and printing `.configured`, at 8e09f10175 (2026-11-05); the wrapper returns the plain plugin when `!(config.isTextTerminal && recorder.configured)` | `configured false`, so ordinary runs take the plain branch | none |
| `validateCucumberFeatures` can be tested directly, outside Cypress, with the config fields it reads | Slice 1 replacement test | Scratch project in the job temp directory with `broken.feature` (stray line), `empty.feature` (`Examples:` with no rows), `ok.feature`, `skip.feature` (invalid, excluded by basename) and `node_modules/x.feature` (invalid); `nix develop -c node --input-type=module -e` calling it with `{ projectRoot, specPattern: <root>/**/*.feature, excludeSpecPattern: 'skip.feature' }` at 8e09f10175 (2026-11-05). Deleted. | Throws `Invalid Gherkin feature files` listing `features/broken.feature: (4:5) ... got 'stray text'` and `features/empty.feature: no scenarios` only | none |
| The Vitest proof reporter adds nothing to ordinary output, so removing it leaves runs unchanged | Slice 2 config edit | Scratch removal of the import, comment and `new VitestProofReporter()` from `frontend/vitest.config.ts`, then `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run tests/utils/titlePatternRender.spec.ts` at 8e09f10175 (2026-11-05). Reverted. | `1 passed`, `3 passed`, ordinary default output | SEED-102 adds a CI-gated JSON reporter to the same `reporters` list; slice 2 runs the CI-profile shard command to confirm it after the dependency lands |
| No ordinary command loads the Node test proof reporter | Slice 3 deletion | `git grep -l -E "nodeTestProof|proofStore"` outside the recorder files at 8e09f10175 | Only `.planning/open-dough.json`, `.agents/agent-map.md`, `package.json` (`test:node-test-proof`) and the proof modules themselves | none |
| `lint:all` runs its `test:*` scripts in CI | Slice 1 and 3 package script changes | Cited from [plan 001](../001-publish-donut-ci-proof/PLAN.md) premise row: CI log of `Linting & Types Gen for Frontend`, run 41948058190 at 61c0a9ab7d (2026-11-05) | `test:cucumber-proof` and the other scripts run with `node --test` totals | none |

## Outside-in proof

| Promise (story example) | Owner | Proof |
| --- | --- | --- |
| Example 1: an ordinary Cypress run keeps results and retries | Slice 1 | `SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/testability/cypress_retry.feature` passes with the retry |
| Example 2: invalid feature files fail the run before any spec | Slice 1 | `validateCucumberFeatures.test.mjs`; acceptance: a scratch stray line in one feature file, then `pnpm cy:run --spec` of a different feature fails with `Invalid Gherkin feature files` naming it before `Running:`; scratch reverted |
| Example 3: Vitest output, retries and CI reporters unchanged | Slice 2 | `./scripts/run.sh pnpm -C frontend test tests/utils/titlePatternRender.spec.ts`; the CI-profile shard command from plan 001 still writes the JSON report |
| Example 4: `lint:all`'s remaining `test:*` scripts pass | Slices 1–3 | Each slice runs the scripts it touches (`test:gherkin-validation`, `test:ci-proof`, `test:log-utils`, `test:path-routing`); CI runs `lint:all` on the story branch |
| Example 5: CI summary and Intent tab unchanged | Slice 3 | Story-branch `donut CI` run: `ci-proof-summary-attempt-1` lists `e2e_test/features` and `frontend/tests` with all parts; the Intent → CI status tab shows that run |
| Example 6: unrelated "proof" stays | Slice 3 | Part of the sweep reading below |

**Removal sweep (one-time reading at acceptance, not a test):** after slice 3,
`git grep -n -i -E "RECORD_(CUCUMBER|VITEST|NODE_TEST)_PROOF|proofGeneration|proofStore|ProofReporter|cucumberProof|nodeTestProof|e2e_test/proof|tests/proof|scripts/proof|infra/proof|proof\.lock|test:(cucumber|vitest|node-test)-proof|retained proof|deliberate proof"`
over the product, excluding `.planning/`, Open Dough's managed
`.agents/skills/dough-*` copies, and dated findings files, returns nothing.
Record the reading and its revision in this plan.

## Slices

### 1. Cypress runs validate features without the Cucumber proof recorder

Type: Behavior
Status: done
Proof: Accepted — `CURSOR_DEV=true nix develop -c node --test e2e_test/config/validateCucumberFeatures.test.mjs` (5 pass); `pnpm test:gherkin-validation`; example 1 `SUT_TIMEOUT_MS=360000 … pnpm cy:run --spec e2e_test/features/testability/cypress_retry.feature` (Attempt 1 of 2 then pass); example 2 scratch invalid feature → `Invalid Gherkin feature files` before `Running:`, scratch reverted. Refactor: agent-map Vitest/Node wording no longer names the Cucumber recorder; biome proof JSON overrides consolidated for remaining stores.

Behavior: a developer runs `pnpm cy:run --spec <feature>` → feature files are
validated before any spec and scenarios run, retry and report as before, with
the plain preprocessor plugin.

Work: wire `validateCucumberFeatures` and `addCucumberPreprocessorPlugin` in
`e2e_test/config/common.ts`; rewrite the retry comment there to describe the
retry alone; delete `cucumberProof.mjs`, `cucumberProofCases.mjs`,
`cucumberProofRecorder.mjs`, `cucumberProofStore.mjs`,
`cucumberProofTestProject.mjs`, `cucumberProofRunnerFixture.mjs`,
`cucumberProofRunnerValidation.cases.mjs` and every `cucumberProof*.test.mjs`;
add `validateCucumberFeatures.test.mjs`; delete `e2e_test/proof`; drop the
proof comment in `cypress_retry.feature`; remove the gherkin location's `proof`
and `proofGeneration` keys, the `e2e_test/proof` Biome override, the
`e2e_test/proof.lock` ignore entry and its comment; replace
`test:cucumber-proof` with `test:gherkin-validation` in `package.json` and
`lint:all`; delete the Cucumber proof paragraph from `.agents/agent-map.md` and
the "Retained Cucumber proof" section from `docs/end-to-end-testing.md`.

Sizing: about 8 minutes of edits plus one SUT boot for the two acceptance runs.

### 2. Frontend Vitest runs without the proof reporter

Type: Behavior
Status: done
Proof: Accepted — `./scripts/run.sh pnpm -C frontend test tests/utils/titlePatternRender.spec.ts` (3 pass); `env -u NODE_ENV CI=true CI_PROOF_VITEST_REPORT=… ./scripts/run.sh pnpm -C frontend test tests/utils/titlePatternRender.spec.ts --shard=1/1` writes JSON; `./scripts/run.sh pnpm test:ci-proof` (8 pass). Refactor: none — already clean.

Behavior: a developer or a CI shard runs Vitest → the same output, retries,
orchestrator GC reporter and CI JSON report, with no proof reporter configured.

Work: remove the reporter import, comment and entry from
`frontend/vitest.config.ts`; delete `frontend/vitestProofReporter.ts`,
`frontend/vitestProofReporter.test.mjs` and `frontend/tests/proof`; remove the
`frontend/tests` location's `proof` and `proofGeneration` keys; remove
`test:vitest-proof` from `package.json` and `lint:all`; delete the Vitest
proof paragraph from `.agents/agent-map.md`.

Sizing: about 4 minutes.

### 3. Node test runs and CI proof stand without any local proof store

Type: Behavior
Status: planned
Proof: `./scripts/run.sh pnpm test:log-utils` and `./scripts/run.sh pnpm test:path-routing`; `./scripts/run.sh pnpm test:ci-proof`; the story-branch CI run and Intent tab (example 5); the removal sweep reading.

Behavior: `pnpm test:*` scripts and `donut CI` run → the same results and CI
proof summary; nothing in the repository records or stores local proof.

Work: delete `nodeTestProofReporter.mjs`, `nodeTestProofSession.mjs`,
`nodeTestProofReporterFixture.mjs`, every `nodeTestProofReporter.*.test.mjs`
and `e2e_test/config/proofStore.mjs`; delete `scripts/proof` and `infra/proof`;
remove the `scripts` and `infra` locations' `proof` and `proofGeneration` keys,
leaving each location's `path`, `kind`, `include` and `runner`; remove their
Biome overrides; remove `test:node-test-proof` from `package.json` and
`lint:all`; delete the Node test proof paragraph from `.agents/agent-map.md`.
Then run the sweep reading and fix anything it finds.

Sizing: about 5 minutes plus one CI wait for example 5.

## Concern evidence

Slice-plan refinement was not needed: each slice owns one runner's surviving
behavior with its own proof loop, the shared store goes with its last consumer,
and sizes are within the target apart from the stated SUT boot and CI wait.
This review found no remaining slice-specific concern.

## Learnings

- Gherkin feature validation runs on every Cypress run but is reached only
  through the proof wrapper and tested only through proof fixtures; slice 1
  keeps it as an ordinary runner protection with its own test.
