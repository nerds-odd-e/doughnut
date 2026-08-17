---
id: SEED-004
status: sprouting
planted: 2026-08-15
planted_during: ADR 0003 finalization (gap analysis)
trigger_when: when accepting Proposed ADR 0003, or when adding a relearning step list or RecallLog
scope: large
---

# SEED-004: Close the spaced-repetition scheduling-policy gap with open FSRS

## Why This Matters

Doughnut already schedules recall with elapsed time and outcome. The product contract is Proposed [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) Decision. Remaining work is **accepting** that ADR plus the deferred knobs below.

## When to Surface

**Trigger:** accepting ADR 0003; adding a relearning step list; adding a replayable recall log for fitting (E6).

Also surface when changing success/failure interval math, commissioned score → schedule mapping, or due-work rebuild from history.

## Scope Estimate

**Large** — remaining trigger is policy accept plus deferred product knobs:

1. Humans accept Proposed [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) (`docs/adrs/README.md`).
2. Deferred IDs: **B4** / **C4** / **E3** / **E4** / **E6**. Tracker: [FSRS-COMPATIBILITY-GAP.md](../research/FSRS-COMPATIBILITY-GAP.md).

## Breadcrumbs

- `.planning/research/FSRS-COMPATIBILITY-GAP.md` — pointer + deferred ID list
- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — Proposed policy (Decision; Working draft empty pending accept)
- `docs/adrs/0001-ubiquitous-language.md` — **recall** (not FSRS **review**)
- `docs/adrs/0005-commissioned-learning-session-protocol.md` — Tutor 0–5 meaning
- `backend/src/main/java/com/odde/doughnut/entities/ForgettingCurve.java`
- `backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java`
- `backend/src/main/java/com/odde/doughnut/algorithms/SpacedRepetitionAlgorithm.java`
- https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm

## Notes

`.planning/research/SUMMARY.md` was referenced at capture time but is not in the tree. The gap doc is the research synthesis for this seed.
