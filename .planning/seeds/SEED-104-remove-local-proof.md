---
id: SEED-104
status: active
planted: 2026-11-05
planted_during: Terry Yin's request to remove local proof across maintained projects
trigger_when: A Donut developer maintains local proof recording or test execution
scope: story
---

# SEED-104: Remove local proof from Donut

<a id="clear-ci-docs-of-local-proof"></a>

### Clear CI proof docs of local-proof wording

**Identity:** SEED-104#clear-ci-docs-of-local-proof
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/003-ci-proof-summary-without-local-proof-wording/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"d4894b642ce120c3aa72ae44de8a6fe10949aa244a168273895814cf60847115","plan":"21d2b86dca1362f2d90987678c92923c9845c5d5b4cc3e397bc1ac3273dc69fd"}}
```

**Goal:** A Donut developer reading the maintained CI proof summary documentation
sees only surviving report producers and environments, with no wording that
names or implies the removed local proof recording feature.

**Scope:** In `docs/ci-proof-summary.md`, remove the historical "proof recording"
clause so the sentence states that local runs and the Cloud VM write no CI
runner report. Do not change CI wiring, Intent locations, converters, or tests.
Do not add a permanent absence checker.

**Key examples:**

1. A developer opens `docs/ci-proof-summary.md` → the "How it is made" step about
   runner reports names only local runs and the Cloud VM as surfaces that write
   no such report; it does not name proof recording or other removed local-proof
   surfaces.
2. Boundary: `pnpm test:ci-proof` still passes; CI proof summary behavior is
   unchanged.

**Plan:** [003-ci-proof-summary-without-local-proof-wording](../slice-plans/003-ci-proof-summary-without-local-proof-wording/PLAN.md)

**Provenance:** Retrospective of SEED-104#remove-local-proof (before-cleanup
`3cb5feabc82bf023905b04c00abbf4dc2c58f5b6`); product commits `fe9092acc6`,
`e96ef120f3`, `07044faff7`; plan evidence `a7896672b2`.
