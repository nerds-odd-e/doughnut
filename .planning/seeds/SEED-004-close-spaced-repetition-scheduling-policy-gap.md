---
id: SEED-004
status: sprouting
planted: 2026-08-15
planted_during: ADR 0003 finalization (gap analysis)
trigger_when: when closing Doughnut’s spaced-repetition scheduling gap vs open FSRS, or when accepting Proposed ADR 0003
scope: large
---

# SEED-004: Close the spaced-repetition scheduling-policy gap with open FSRS

## Why This Matters

Doughnut already schedules recall with elapsed time and outcome, but the **memory model** is still a single strength index plus a user day table. Open FSRS (FSRS-6) is the DSR scheduler we expect to stay **mostly compatible** with: Difficulty, Stability, computed Retrievability, grades, requested retention.

Until Proposed [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) states that target shape, implementation work will either freeze today’s index or invent a private model. The ADR should hold the product contract. **A1 locked:** Doughnut owns an FSRS-compatible implementation (no FSRS library). **B3 locked:** overdue correct gets bounded extra growth. Remaining gaps close by **vertical slice** (one observable behavior; structure only when that behavior needs it).

## When to Surface

**Trigger:** finalizing or accepting ADR 0003; starting a milestone to replace or wrap the forgetting-curve index; exposing retention / FSRS-like grades; adding a replayable recall log for fitting.

Also surface when changing success/failure interval math, commissioned score → schedule mapping, or due-work rebuild from history.

## Scope Estimate

**Large** — policy first, then stop-safe behavior slices:

1. Finalize ADR 0003 from the gap + open issues in `.planning/research/FSRS-COMPATIBILITY-GAP.md` (human advice process). Remaining gaps: one observable behavior at a time; no unused D/S/lapse/retention/RecallLog structure.
2. Next implementation: B3 overdue extra on `ForgettingCurve.succeeded` (current index + interval). Then later: D/S when a behavior needs them, retention-target intervals, relearning, optional RecallLog.

## Breadcrumbs

- `.planning/research/FSRS-COMPATIBILITY-GAP.md` — analysis + open issues (tracker)
- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — Proposed policy (Decision + Working draft)
- `docs/adrs/0001-ubiquitous-language.md` — **recall** (not FSRS **review**)
- `docs/adrs/0005-commissioned-learning-session-protocol.md` — Tutor 0–5 meaning
- `backend/src/main/java/com/odde/doughnut/entities/ForgettingCurve.java`
- `backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java`
- `backend/src/main/java/com/odde/doughnut/algorithms/SpacedRepetitionAlgorithm.java`
- `backend/src/main/java/com/odde/doughnut/algorithms/CommissionedLearningSessionFeedbackPolicy.java`
- https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm

## Notes

`.planning/research/SUMMARY.md` was referenced at capture time but is not in the tree. The gap doc is the research synthesis for this seed.
