# Source review: SEED-104#remove-local-proof

## Launch and input

- Host: Cursor Task, fresh general-purpose agent (no conversation inheritance)
- Agent id: 1744af42-0ee0-4a00-b34e-de8165b8aeea
- Input: story Goal, Scope (deletion / survives / limits), Boundary assumptions,
  Key examples, Evaluation, and Boundary from
  `.planning/seeds/SEED-104-remove-local-proof.md#remove-local-proof` only
- Instruction: use only those inputs; read no other project files; change nothing;
  return numbered commitments, qualifications, journey examples, and exclusions

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | A Donut developer maintains a smaller repository without a local proof collection or persistence subsystem. | slice 3 | |
| 2 | Delete that feature and all its artifacts and dedicated support. | slice 3 | |
| 3 | Ordinary tests and CI proof remain usable. | slice 3 | |
| 4 | After the change, ordinary test runs behave as before. | slice 3 | |
| 5 | After the change, the `donut CI` proof summary behaves as before. | slice 3 | |
| 6 | Nothing records, stores, or describes local proof. | slice 3 | |
| 7.1 | Delete committed local proof snapshots at `e2e_test/proof`. | slice 1 | observed: deleted e2e_test/proof/latest.json; path absent |
| 7.2 | Delete committed local proof snapshots at `frontend/tests/proof`. | slice 2 | observed: deleted frontend/tests/proof/latest.json; directory absent |
| 7.3 | Delete committed local proof snapshots at `scripts/proof`. | slice 3 | |
| 7.4 | Delete committed local proof snapshots at `infra/proof`. | slice 3 | |
| 8 | Delete any local-proof-only copies and fixture snapshots. | slice 3 | |
| 9 | Keep no archive or replacement history store. | slice 3 | |
| 10.1 | Delete Cucumber local proof recorders/stores including `e2e_test/config/cucumberProof*.mjs`. | slice 1 | observed: deleted e2e_test/config/cucumberProof*.mjs and tests; no cucumberProof* remains |
| 10.2 | Delete Node test local proof recorders/stores including `e2e_test/config/nodeTestProof*.mjs`. | slice 3 | |
| 10.3 | Delete `e2e_test/config/proofStore.mjs`. | slice 3 | |
| 10.4 | Delete `frontend/vitestProofReporter.ts` implementation. | slice 2 | observed: deleted frontend/vitestProofReporter.ts and frontend/vitestProofReporter.test.mjs |
| 11 | Trace and delete exclusive refresh, locking, serialization and helper dependencies. | slice 3 | |
| 12.1 | Remove `RECORD_CUCUMBER_PROOF` collection paths, commands, configuration, hooks and reporter wiring. | slice 1 | observed: RECORD_CUCUMBER_PROOF removed from agent-map, docs, open-dough gherkin location; common.ts no longer loads cucumberProof |
| 12.2 | Remove `RECORD_VITEST_PROOF` collection paths, commands, configuration, hooks and reporter wiring. | slice 2 | observed: RECORD_VITEST_PROOF removed from vitest.config.ts, open-dough frontend/tests location, agent-map |
| 12.3 | Remove `RECORD_NODE_TEST_PROOF` collection paths, commands, configuration, hooks and reporter wiring. | slice 3 | |
| 13.1 | Remove `test:cucumber-proof` and its place in `lint:all`. | slice 1 | observed: package.json: test:cucumber-proof replaced by test:gherkin-validation in scripts and lint:all |
| 13.2 | Remove `test:vitest-proof` and its place in `lint:all`. | slice 2 | observed: test:vitest-proof removed from package.json scripts and lint:all |
| 13.3 | Remove `test:node-test-proof` and its place in `lint:all`. | slice 3 | |
| 14 | Remove local `proof` and `proofGeneration` settings from `.planning/open-dough.json`. | slice 3 | |
| 15 | Preserve the Intent locations (`path`, `kind`, `include`, `runner`) used for source browsing and CI proof entry identities. | slice 3 | |
| 16 | Delete all tests, fixtures, mocks, helper modules, package entries and dependencies dedicated to local proof. | slice 3 | |
| 17 | Remove local-proof documentation (for example the local proof sections of `.agents/agent-map.md` and `docs/end-to-end-testing.md`). | slice 3 | |
| 18 | Remove local-proof code comments (for example the retry comment in `e2e_test/config/common.ts`). | slice 1 | observed: e2e_test/config/common.ts retries comment describes runMode retry only; cypress_retry.feature proof comment removed |
| 19 | Remove project-owned agent guidance transitively (for local proof). | slice 3 | |
| 20 | Maintained files describe the resulting product directly. | slice 3 | |
| 21.1 | Retain no deprecation switches. | slice 3 | |
| 21.2 | Retain no compatibility readers. | slice 3 | |
| 21.3 | Retain no stubs. | slice 3 | |
| 21.4 | Retain no historical feature notes. | slice 3 | |
| 21.5 | Retain no migration guide. | slice 3 | |
| 22 | Ordinary test selection stays as it is for Cypress/Cucumber, Vitest and `node:test` runs, locally, on the Cloud VM and in CI. | slice 3 | |
| 23 | Ordinary diagnostics stay as they are for those same runners and environments. | slice 3 | |
| 24 | Ordinary retries stay as they are for those same runners and environments. | slice 3 | |
| 25 | Ordinary strict result handling stays as it is for those same runners and environments. | slice 3 | |
| 26 | Existing runner protections stay as they are for those same runners and environments. | slice 3 | |
| 27 | The Gherkin feature-file check (`validateCucumberFeatures`) is a runner protection, not local proof. | slice 1 | observed: e2e_test/config/common.ts calls validateCucumberFeatures then addCucumberPreprocessorPlugin; validateCucumberFeatures.test.mjs maintained |
| 28 | Every Cypress run still fails before running when a selected project's feature file has a parse error or no scenarios. | slice 1 | observed: example 2 acceptance: Invalid Gherkin feature files for note_creation.feature before Running:; validateCucumberFeatures.test.mjs parse/no-scenarios cases |
| 29 | `validateCucumberFeatures` stays wired into ordinary Cypress setup with the plain preprocessor plugin. | slice 1 | observed: e2e_test/config/common.ts: validateCucumberFeatures(config); await addCucumberPreprocessorPlugin(on, config) |
| 30 | `validateCucumberFeatures` keeps a maintained test that does not use proof fixtures. | slice 1 | observed: e2e_test/config/validateCucumberFeatures.test.mjs direct node:test; no proof fixtures |
| 31 | The ordinary CI reports, conversions, fragments, summary artifact and dashboard contract owned by CI proof summary stay, with their maintained tests. | slice 3 | |
| 32 | Shared code remains only where a surviving test or CI responsibility needs it. | slice 3 | |
| 33 | Prove the surviving ordinary test execution using its maintained tests. | slice 3 | |
| 34 | Prove the surviving CI report → fragment → summary behavior using its maintained tests. | slice 3 | |
| 35 | Separately perform a one-time whole-product reading/search for the deleted feature, including configuration, docs, fixtures and exclusive dependencies. | slice 3 | |
| 36 | Follow Donut's removal guidance: delete feature tests outright. | slice 3 | |
| 37 | Create no permanent assertion or checker that the deleted feature is absent. | slice 3 | |
| 38 | Remove spent story/plan records at closure. | landing | |
| 39 | Keep no local-proof archive. | slice 3 | |
| 40 | Complete deletion, not an opt-out default or a disabled recorder. | slice 3 | |
| E1 | A developer runs `pnpm cy:run --spec e2e_test/features/testability/cypress_retry.feature` → the scenario retries once and passes as before, with the same Cypress summary and exit code. | slice 1 | observed: SUT_TIMEOUT_MS=360000 cy:run cypress_retry.feature: Attempt 1 of 2 then pass; All specs passed; exit 0 |
| E2 | A selected Cypress project contains a feature file with a stray line or an `Examples:` block with no rows → the run fails before any spec with `Invalid Gherkin feature files` naming each bad file, as today. | slice 1 | observed: scratch stray line on note_creation.feature; cy:run cypress_retry failed with Invalid Gherkin naming it before Running:; scratch reverted |
| E3 | A developer runs `./scripts/run.sh pnpm -C frontend test <spec>` locally, and CI runs both Vitest shards → output, retries and the CI-only orchestrator GC reporter are unchanged, and the CI shard still writes the JSON report SEED-102 converts. | slice 2 | observed: ./scripts/run.sh pnpm -C frontend test titlePatternRender.spec.ts 3 pass; CI=true CI_PROOF_VITEST_REPORT shard writes JSON; pnpm test:ci-proof 8 pass; orchestratorGcReporter retained in vitest.config.ts |
| E4 | `pnpm lint:all` runs → the remaining `test:*` scripts, including the CI proof tests, pass; no local proof test script is listed. | slice 3 | |
| E5 | A push runs `donut CI` → `ci-proof-summary-attempt-<n>` lists `e2e_test/features` and `frontend/tests` totals, and the Intent → CI status tab shows them against the locations still listed in `.planning/open-dough.json`. | slice 3 | |
| E6 | Boundary: the sweep finds the `frontend` skill's "Frontend proof" rule and the CI proof summary → both stay, because they are not local proof. | slice 3 | |
