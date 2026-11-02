# Resolve remaining local test optimization candidates

<a id="resolve-candidates"></a>

### Triage the migrated candidates using existing evidence

**Identity:** SEED-097#resolve-candidates
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

**Goal:** Give every currently pending test-optimization candidate an evidenced
disposition and land the resolve-only result.

**Scope:** Read the candidate record, related test families and surviving proof;
preserve accepted costs, existing exclusions, normal test execution and CI.
Record necessary costs, evident existing-mechanism profile exclusions, or a
replacement plan with a missing baseline as a later prerequisite. Retain any
real human trade-off explicitly. Do not profile, optimize, or execute a
replacement experiment in this pass.

**Key examples:** `e2e-batch-cold-start` and `frontend-sibling-spec-setup` are
the two pending entries at the starting revision. Keep their slugs and dated
observations; do not report new timings or a speedup from read-only triage.

**Delivery:** Publish the reviewed result to `origin` `refs/heads/main` through
Dough Land, preserving any unfinished future replacement work.
