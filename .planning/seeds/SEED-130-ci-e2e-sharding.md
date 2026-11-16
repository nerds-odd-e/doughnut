---
id: SEED-130
status: dormant
planted: 2026-11-16
planted_during: owner-directed CI backlog prioritization
trigger_when: the team takes up end-to-end CI coverage and shard balance
scope: medium
---

# SEED-130: Keep end-to-end CI coverage complete and workloads balanced

## Why This Matters

Donut contributors need every eligible end-to-end spec to run in CI and want
faster feedback from the last shard to finish. All six shards currently depend
on explicit folder patterns. A spec outside those patterns can be missed, and
folder counts alone cannot tell maintainers how to balance the workload.

## Alternatives and Decision

Manually updating folder lists and inspecting the latest CI run can rebalance
today's suite, but it can miss new folders and makes each rebalance a one-off.
An extra catch-all job would raise the shard count. Keep the existing six slots
and replace one explicit group with the remainder, then make rebalancing
repeatable with a private Donut agent skill.

## Story Decomposition

<a id="rebalance-ci-e2e-shards"></a>

### Keep all E2E specs covered and rebalance the six CI shards

**Identity:** SEED-130#rebalance-ci-e2e-shards
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Donut contributors get complete E2E feedback and maintainers
  can address the slowest shard as the suite changes.
- **Coverage:** Keep five explicit folder-pattern groups. Replace one existing
  group with a catch-all that runs exactly the eligible E2E specs not selected
  by the other five. Keep the matrix and CI proof at six parts. Show that every
  eligible spec runs exactly once, including a new eligible spec outside the
  named folders, while existing runner exclusions still apply.
- **Rebalancing:** Add a private, project-local Donut agent skill for on-demand
  rebalancing. It reads recent comparable completed CI proof on each invocation.
  Use the controlling Open Dough dashboard's **Intent → CI status** when its
  location is known and it supplies the needed evidence; otherwise read the
  CI proof artifacts directly from GitHub Actions. Read evidence in place or use
  temporary storage outside the Donut checkout. Keep no evidence cache in the
  repository and publish no raw proof there. This skill belongs to Donut,
  separate from the shared Open Dough skill distribution.
- **Evaluation:** Use the skill for the initial balance. Identify the runs,
  attempts and revisions used, reconcile proof with their shard selections, and
  compare the current and proposed workloads across all six groups. Use CI proof's
  recorded per-file time to estimate moves; use comparable job wall times to
  check whether the last shard finishes sooner. Report coverage and timing gaps,
  and apply only changes the evidence supports. If recent proof is incomplete or
  inaccessible, say what is missing and leave timing-based assignments alone.
- **Effort hypothesis:** M, assuming the existing CI proof provides usable
  timings and the current runner can express the full partition.
- **Depends on:** No other queued story.
- **Safe stopping point:** Deliver the six-shard coverage guarantee on its own
  if timing evidence is unavailable; the skill can re-evaluate once it is.

## Breadcrumbs

- [Current GitHub Actions workflow](../../.github/workflows/ci.yml) defines
  the E2E matrix and the expected six proof parts.
- [CI proof summary](../../docs/ci-proof-summary.md) describes the dashboard,
  the GitHub artifact, timing units, and how missing parts are reported.
