# Cognitive probe convergent-validity and latency-modeling analyses

**Status:** closed.
**Type:** ad-hoc plan (`.planning/quick/`)
**Depends on:** `.planning/quick/007-daily-cognitive-probe/PLAN.md` (the probe
shipped — the convergent-validity diagnostic needs probe history);
`.planning/quick/001-morning-cognitive-index/PLAN.md` for the shipped
component readouts (pace, accuracy, consistency, lapse count) it validates
against.

## Goal

Two analyses shelved when the plan they were originally written for changed
shape: whether an independent daily probe agrees with what the
recall-derived component readouts say, and whether a rolling
diffusion-model decomposition of MCQ latencies is worth building at all.
Re-scoped in a `/slice-planning` pass on 2026-08-29 after the composite
morning index (the original comparison target) was dropped — see
`.planning/quick/001-morning-cognitive-index/PLAN.md`'s Outcome section.

## Outcome

**Shipped**, all three slices, internal diagnostics only (no frontend page,
no Recall Stats tile):

- **Convergent validity** (`GET /api/user/daily-probe-convergent-validity`):
  Pearson `r` + `pairCount` over a trailing 90-day window for four matched
  pairs (pace↔speed, accuracy↔accuracy, lapse count↔lapse count,
  consistency↔variability), gated `null` below 10 pairs per pair — same
  precedent as the retired split-half diagnostic.
- **EZ-diffusion decomposition**
  (`GET /api/user/recall-ez-diffusion?timezone=`): a package-local
  Wagenmakers 2007 EZ closed-form (`EzDiffusion.recover`) consumed by a
  trailing-three-local-morning MCQ aggregator (`RecallEzDiffusion`), gated
  `null` fit fields below 30 qualifying trials. Decomposition was justified
  by a production gate (below) before slices 2–3 were built.

**Learning — EZ-diffusion exclusion-set simplification:** `RecallEzDiffusion`
reuses only `RecallPaceAggregator`'s fixed absolute floor (300ms on-task) and
hard-drop ceiling (300,000ms) for excluding implausible RTs, not
`RecallPaceAggregator`'s per-item EWMA-baseline-relative floor (that requires
a chronological, stateful per-item walk over all-time history to build).
Judged out of slice budget; accepted as a budget-appropriate simplification,
not a correctness gap. If a future user-facing readout is built on this
diagnostic, revisit whether the baseline-relative floor is worth wiring in.

## Gate result (2026-08-29, production)

Read-only against Cloud SQL `doughnut-db-instance` / database `doughnut`.
Learner: Terry (`user.id = 1`), timezone `Asia/Shanghai`. Rows fed through
the shipped `RecallPaceAggregator` / `RecallAccuracyAggregator` (not a SQL
proxy for those formulas). Trailing 90 local mornings with both
`pctVsUsual` and accuracy `standardizedResidual` non-null: **91**.

| Definition of “opposite” (one better-than-usual, one worse) | Rate |
|---|---|
| Raw sign | **54/91 (59.3%)** |
| Exclude pace `\|pctVsUsual\| < 1` (UI “about usual”) | **53/86 (61.6%)** |
| `\|pct\| ≥ 1` and `\|A\| ≥ 0.5` | **28/50 (56.0%)** |
| `\|pct\| ≥ 1` and `\|A\| ≥ 1.0` | **16/29 (55.2%)** |

Threshold decided in the earlier planning pass: decompose only if
opposite-direction mornings are **≥15–20%**. All four cuts clear it.
Of the 54 raw-sign opposites, **48** are faster + worse accuracy and **6**
are slower + better accuracy (both-better 30, both-worse 7). The signal is
a speed–accuracy tradeoff, mostly rushing, which is what `v` vs `a` is for.

Yeong Sheng had only 2 recall days in 90d — not a second powered learner.

## Key design decisions

- **Internal diagnostic first.** This plan is analyses, not Recall Stats
  tiles. A user-facing caution/capacity readout is a later plan if this
  diagnostic is worth showing.
- **Wagenmakers 2007 EZ**, not a numerical diffusion fit. Closed form matches
  the “is it worth it” bar; RT variance stays the noisy input, hence three
  mornings never daily.
- **MCQ only**, correct-vs-error boundaries. Spelling stays out — no
  symmetric two-choice structure for EZ.
- **No significance testing, no multiple-comparison correction** on the
  convergent-validity pairs: matches the retired split-half diagnostic's own
  precedent of raw `r` + `pairCount`, gated by minimum sample size.

## Permanent artifacts (capability-named)

| Artifact | Notes |
|---|---|
| `UserController.getDailyProbeConvergentValidity` — `GET /api/user/daily-probe-convergent-validity` | internal, current-user-only, no page |
| `RecallProbeConvergentValidity` | correlates the 4 matched pairs; mirrors the retired split-half diagnostic's shape |
| `DailyProbeConvergentValidityDTO` | `{ pairs: [{ pair, pairCount, rawCorrelation }] }` |
| `DailyProbeDaySeries.latestByLocalDay` | shared "latest probe per local day" grouping |
| `UserController.getRecallEzDiffusion` — `GET /api/user/recall-ez-diffusion` | internal, current-user-only, no page |
| `RecallEzDiffusion` | trailing-3-morning MCQ aggregator; delegates to `EzDiffusion` |
| `RecallEzDiffusionDTO` | `{ driftRate, boundarySeparation, nondecisionTimeMs, trialCount, morningCount }` |
| `EzDiffusion` | Wagenmakers 2007 closed-form `(P, MRT, VRT, n) → (v, a, Ter)`; consumed by `RecallEzDiffusion` |
