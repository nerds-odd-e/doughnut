---
id: SEED-104
status: active
planted: 2026-11-05
planted_during: Terry Yin's request to remove local proof across maintained projects
trigger_when: A Donut developer maintains local proof recording or test execution
scope: story
---

# SEED-104: Remove local proof from Donut

<a id="remove-local-proof"></a>

### Remove local proof from Donut

**Identity:** SEED-104#remove-local-proof
```json dough-story-dependencies
{"schemaVersion":1,"identity":"SEED-104#remove-local-proof","dependencies":[{"supplier":{"identity":"SEED-102#publish-donut-ci-proof","href":"seeds/SEED-102-publish-donut-ci-proof.md#publish-donut-ci-proof"},"implementation":"SEED-102 adds the CI runner reports, converter, fragments, collector and summary job, wired beside the local proof recorders in frontend/vitest.config.ts, e2e_test/config/ci.ts, package.json lint:all and .github/workflows/ci.yml.","rationale":"This story must preserve and prove the CI report to fragment to summary behavior with its maintained tests, which exist only after SEED-102 lands; SEED-102's in-flight plan also relies on the local proof recorder tests staying green, so deleting them first would break that plan's proof. Reconciling two concurrent rewrites of the same wiring cannot supply the CI tests this story's evaluation needs.","condition":"SEED-102's CI proof summary, with its converter and workflow tests, is published on trunk.","state":"waiting"}]}
```
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/002-remove-local-proof/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"389471da1563b2d21658974676e3f10e9ad6f46ca924b106ec6fb33716828c18","plan":"e62ca425b0214173ae71ce00728d659dbbcd26c66c57c85d452bdc4d33334e1b"}}
```

**Goal:** A Donut developer maintains a smaller repository without a local
proof collection or persistence subsystem. Delete that feature and all its
artifacts and dedicated support while ordinary tests and CI proof remain usable.
The observable outcome is a repository where ordinary test runs and the
`donut CI` proof summary behave as before and nothing records, stores or
describes local proof.

**Scope — required behavior (deletion):**

- Delete all committed local proof and historical snapshots, including
  `e2e_test/proof`, `frontend/tests/proof`, `scripts/proof` and `infra/proof`.
  Delete any local-proof-only copies and fixture snapshots; keep no archive or
  replacement history store.
- Delete Cucumber, Vitest and Node test local proof recorders and stores,
  including the relevant `e2e_test/config/cucumberProof*.mjs`,
  `e2e_test/config/nodeTestProof*.mjs`, `e2e_test/config/proofStore.mjs` and
  `frontend/vitestProofReporter.ts` implementation. Trace and delete exclusive
  refresh, locking, serialization and helper dependencies.
- Remove the `RECORD_CUCUMBER_PROOF`, `RECORD_VITEST_PROOF` and
  `RECORD_NODE_TEST_PROOF` collection paths, commands, configuration, hooks and
  reporter wiring, including the `test:cucumber-proof`, `test:vitest-proof` and
  `test:node-test-proof` package scripts and their place in `lint:all`. Remove
  local `proof` and `proofGeneration` settings from `.planning/open-dough.json`,
  preserving the Intent locations (`path`, `kind`, `include`, `runner`) used for
  source browsing and CI proof entry identities.
- Delete all tests, fixtures, mocks, helper modules, package entries and
  dependencies dedicated to local proof. Remove local-proof documentation
  (for example the local proof sections of `.agents/agent-map.md` and
  `docs/end-to-end-testing.md`), code comments (for example the retry comment
  in `e2e_test/config/common.ts`) and project-owned agent guidance
  transitively. Maintained files describe the resulting product directly;
  retain no deprecation switches, compatibility readers, stubs, historical
  feature notes or migration guide.

**Scope — required behavior (survives):**

- Ordinary test selection, diagnostics, retries, strict result handling and
  existing runner protections stay as they are for Cypress/Cucumber, Vitest and
  `node:test` runs, locally, on the Cloud VM and in CI.
- The Gherkin feature-file check (`validateCucumberFeatures`) is a runner
  protection, not local proof: every Cypress run still fails before running
  when a selected project's feature file has a parse error or no scenarios.
  Today it is called only from the proof wrapper `addCucumberPreprocessorWithProof`
  and tested only through proof test fixtures. It stays wired into ordinary
  Cypress setup with the plain preprocessor plugin, and keeps a maintained test
  that does not use proof fixtures.
- The ordinary CI reports, conversions, fragments, summary artifact and
  dashboard contract owned by
  [CI proof summary](../../docs/ci-proof-summary.md)
  stay, with their maintained tests. Shared code remains only where a
  surviving test or CI responsibility needs it.

**Scope limits:**

- Receive retired shared guidance (for example
  `.agents/skills/dough-test-optimization/references/*-proof.md`) through Open
  Dough's released update rather than hand-editing its installed managed
  copies. Open Dough's own removal story owns that published guidance and
  payload cleanup.
- CI adoption remains SEED-102's work; adding richer CI proof or retaining a
  local fallback is not part of this story.

**Boundary assumptions:**

- Other meanings of "proof" are not this feature and stay: acceptance evidence
  in planning and skills (for example the `frontend` skill's "Frontend proof"
  typecheck rule), the CI proof summary, and the MCP isolation helper
  `scripts/mcp-isolation-proof.mjs`.
- This story starts after SEED-102 lands: the CI proof it must preserve and
  prove does not exist on trunk yet, and SEED-102's in-flight plan relies on the
  proof recorders' tests staying green (recorded as a dependency below).

**Key examples:**

1. A developer runs `pnpm cy:run --spec e2e_test/features/testability/cypress_retry.feature`
   → the scenario retries once and passes as before, with the same Cypress
   summary and exit code.
2. A selected Cypress project contains a feature file with a stray line or an
   `Examples:` block with no rows → the run fails before any spec with
   `Invalid Gherkin feature files` naming each bad file, as today.
3. A developer runs `./scripts/run.sh pnpm -C frontend test <spec>` locally,
   and CI runs both Vitest shards → output, retries and the CI-only
   orchestrator GC reporter are unchanged, and the CI shard still writes the
   JSON report SEED-102 converts.
4. `pnpm lint:all` runs → the remaining `test:*` scripts, including the CI proof
   tests, pass; no local proof test script is listed.
5. A push runs `donut CI` → `ci-proof-summary-attempt-<n>` lists
   `e2e_test/features` and `frontend/tests` totals, and the Intent → CI status
   tab shows them against the locations still listed in
   `.planning/open-dough.json`.
6. Boundary: the sweep finds the `frontend` skill's "Frontend proof" rule and
   the CI proof summary → both stay, because they are not local proof.

**Evaluation:** Prove the surviving ordinary test execution and CI report →
fragment → summary behavior using their maintained tests. Separately perform
a one-time whole-product reading/search for the deleted feature, including
configuration, docs, fixtures and exclusive dependencies. Follow Donut's
removal guidance in `AGENTS.md` and the `unit-testing` skill: delete feature
tests outright and create no permanent assertion or checker that it is absent.
Remove spent story/plan records at closure, keeping no local-proof archive.

**Boundary:** Complete deletion, not an opt-out default or a disabled recorder.
