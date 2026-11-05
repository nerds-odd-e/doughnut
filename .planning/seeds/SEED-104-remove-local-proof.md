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
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** A Donut developer maintains a smaller repository without a local
proof collection or persistence subsystem. Delete that feature and all its
artifacts and dedicated support while ordinary tests and CI proof remain usable.

**Requirements:**

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
  reporter wiring. Remove local `proof` and `proofGeneration` settings from
  `.planning/open-dough.json`, preserving the Intent locations used for source
  browsing and CI proof entry identities.
- Delete all tests, fixtures, mocks, helper modules, package entries and
  dependencies dedicated to local proof. Remove local-proof documentation,
  code comments and project-owned agent guidance transitively. Maintained files
  describe the resulting product directly; retain no deprecation switches,
  compatibility readers, stubs, historical feature notes or migration guide.
- Preserve ordinary test selection, diagnostics, retries, strict result
  handling and existing runner protections. Preserve the ordinary CI reports,
  conversions, fragments, summary artifact and dashboard contract owned by
  [CI proof summary](../../docs/ci-proof-summary.md).
  Shared code remains only where a surviving test or CI responsibility needs
  it; the local-proof collection and persistence entry points are deleted.
- Receive retired shared guidance through Open Dough's released update rather
  than hand-editing its installed managed copies. Open Dough's own removal
  story owns that published guidance and payload cleanup.

**Evaluation:** Prove the surviving ordinary test execution and CI report →
fragment → summary behavior using their maintained tests. Separately perform
a one-time whole-product reading/search for the deleted feature, including
configuration, docs, fixtures and exclusive dependencies. Follow Donut's
removal guidance in `AGENTS.md` and the `unit-testing` skill: delete feature
tests outright and create no permanent assertion or checker that it is absent.
Remove spent story/plan records at closure, keeping no local-proof archive.

**Boundary:** Complete deletion, not an opt-out default or a disabled recorder.
CI adoption remains its existing work; adding richer CI proof or retaining a
local fallback is not required by this story.
