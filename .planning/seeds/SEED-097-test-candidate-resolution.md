# Resolve remaining local test optimization candidates

<a id="resolve-candidates"></a>

### Triage the migrated candidates using existing evidence

**Identity:** SEED-097#resolve-candidates
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/080-remaining-local-test-costs/PLAN.md","assessment":"not-ready","reasons":["Cold-start stage attribution and current comparable ordinary-command baselines must be established by a later authorized optimization pass before replacements are selected or accepted."],"basis":{"document":"fe20ac0cfa8f5238e823779fdb3692379f16a97a3d9ab802438c206f1e7963fd","plan":"221fe2b8272c7a95c11f1aee32ddd09c8a4cd445be7e794b92432750130fbecf"}}
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

**Planned follow-up:** The replacement plan owns the missing cold-start
baseline and two small AI-reply sibling-spec experiments. Its intended later
outcome is less ordinary local feedback time with the same observable
protection, or an evidenced no-gain result. This request authorizes writing
that plan, not running its measurements or implementing its experiments.
