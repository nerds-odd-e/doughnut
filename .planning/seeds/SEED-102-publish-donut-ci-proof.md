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

**Outcome:** A Donut developer can see the frontend unit and end-to-end
proof summaries for the shown run in the Open Dough dashboard's
**Intent → CI status** tab.

Use the [installed CI proof recipe](../../.agents/skills/dough-test-optimization/references/ci-proof-summary.md)
with [Donut's CI workflow](../../.github/workflows/ci.yml). Collect test
outcomes from both Vitest shards for `frontend/tests` and Cypress Cucumber
outcomes from the six feature groups for `e2e_test/features`. Preserve CI
test selections and verdicts. Publish one attempt-specific summary after the
producers settle, including available proof when a test fails.

**Acceptance:** A real workflow run publishes one readable summary with
separate totals for both configured locations. The dashboard displays those
totals for the same run and attempt. A missing shard is visible as a gap and
does not count as a pass. Counts for other suites can follow when their CI
jobs produce usable reports.
