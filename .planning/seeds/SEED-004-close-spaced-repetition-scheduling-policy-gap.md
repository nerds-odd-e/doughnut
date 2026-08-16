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

Doughnut already schedules recall with elapsed time and outcome. Ordinary **correct** recall uses FSRS-6 Good SInc and Difficulty (own implementation; first success D=5, S=24h). Ordinary **incorrect** uses FSRS-6 post-lapse Stability and Again next-D; due stays +12h. Commissioned Tutor score **4** is that same Good path. Confusion / remaining commissioned scores (5/3/2/1/0) still walk the Fibonacci ladder (`DEFAULT_SPACES`) — leftover. Open FSRS (FSRS-6) remains the DSR target: Difficulty, Stability, computed Retrievability, grades, requested retention.

Until Proposed [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) states that target shape, remaining work will either freeze today’s success-path SInc or invent a private model. The ADR should hold the product contract. **A1 locked:** Doughnut owns an FSRS-compatible implementation (no FSRS library). **B3 locked:** overdue correct gets bounded extra growth. Remaining gaps close by **vertical slice** (one observable behavior; structure only when that behavior needs it).

## When to Surface

**Trigger:** finalizing or accepting ADR 0003; exposing a requested-retention knob; moving confusion / remaining commissioned scores (5/3/2/1/0) off the leftover ladder; adding a replayable recall log for fitting.

Also surface when changing success/failure interval math, commissioned score → schedule mapping, or due-work rebuild from history.

## Scope Estimate

**Large** — policy first, then stop-safe behavior slices:

1. Finalize ADR 0003 from the gap + open issues in `.planning/research/FSRS-COMPATIBILITY-GAP.md` (human advice process). Remaining gaps: one observable behavior at a time; no unused lapse/retention/RecallLog structure.
2. **Success path, ordinary incorrect Again, and commissioned Tutor score 4 (Good) are in code** (details in the gap doc). Remaining: B2 `r ≠ 0.9`, relearning steps, RecallLog / fitting, leftover ladder on confusion + remaining commissioned scores (5/3/2/1/0).

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
