# Cognitive probe convergent-validity and latency-modeling analyses

**Status:** slice 1 shipped. Slice 2's production gate passed on 2026-08-29;
slices 2–3 below are the EZ-diffusion decomposition (not yet executed).
**Type:** ad-hoc plan (`.planning/quick/`)
**Depends on:** `.planning/quick/007-daily-cognitive-probe/PLAN.md` (the probe
has shipped — slice 1 needs probe history); `.planning/quick/001-morning-cognitive-index/PLAN.md`
for the shipped component readouts (pace, accuracy, consistency, lapse count)
slice 1 validates against.

## Goal

Two analyses shelved when the plan they were originally written for changed
shape: whether an independent daily probe agrees with what the
recall-derived component readouts say, and whether a rolling
diffusion-model decomposition of MCQ latencies is worth building at all.
This plan was re-scoped in a `/slice-planning` pass on 2026-08-29 after the
composite morning index (the original comparison target) was dropped — see
`.planning/quick/001-morning-cognitive-index/PLAN.md`'s Outcome section.

## Slices

Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

### 1. Convergent validity is reported per component readout — Behavior `[x]`

**Shipped as:** `RecallProbeConvergentValidity` (service),
`DailyProbeConvergentValidityDTO`, `RecallStatsService.computeConvergentValidity`,
and `UserController.getDailyProbeConvergentValidity` — see Permanent
artifacts below. `DailyProbeDaySeries` gained a shared `latestByLocalDay`
helper during refactor (deduplicated against this slice's own day-grouping
logic).

**Precondition:** current user has at least one daily probe result and
qualifying recall data on the same mornings. `RecallPaceAggregator.compute`
and `RecallAccuracyAggregator.compute` already accept an arbitrary historical
`today: LocalDate`, so this is computable per historical morning the same way
the (now-retired) split-half reliability diagnostic looped over historical
days — no new aggregation capability is needed, only a new caller.

**Trigger:** `GET /api/user/daily-probe-convergent-validity` — internal,
current-user-only, no frontend page. Same precedent as the retired
`GET /api/user/recall-split-half-reliability` (`UserController`, deleted in
`8ca3115dd5`).

**Postcondition:** over a trailing 90-day window (same window precedent as
the retired split-half diagnostic), for each of four matched pairs below,
returns `pairCount` and `rawCorrelation` (Pearson r; `null` if
`pairCount < 10`, reusing `RecallDayBaseline.MIN_BASELINE_DAYS` /
the retired split-half gate's `MIN_PAIRS_FOR_CORRELATION` — same floor,
applied independently per pair, not raised just because there are four).
No significance testing and no multiple-comparison correction: this matches
the retired diagnostic's own precedent of reporting raw `r` + `pairCount`
only, gated by minimum sample size rather than a p-value.

Matched pairs (one-to-one by construct, not a full cross matrix — keeps
results interpretable and avoids reintroducing a multiple-comparison
problem):

| Recall component | Probe metric |
|---|---|
| pace (`pctVsUsual`) | speed |
| accuracy (standardized residual) | accuracy |
| lapse count | lapse count |
| consistency (`consistencyZScore`) | variability |

### 2. EZ-diffusion recovers drift and boundary from accuracy and RT moments — Structure `[ ]`

**Unlocks slice 3.** No user-visible behavior. A package-local pure function
(Wagenmakers, van der Maas & Grasman 2007 EZ) maps `(P, MRT, VRT, n)` to
`(v, a, Ter)` so slice 3 does not invent the algebra inside a controller.

**Contract (capability-named, not a GSD number):**

- `s = 0.1` (conventional diffusion coefficient).
- `P` is proportion correct; `MRT` / `VRT` are mean and variance of
  response times **in seconds**, over the same responded trials (correct and
  error). Variance uses `n−1` (sample).
- Edge correction: `P = 0` → `1/(2n)`, `P = 1` → `1 − 1/(2n)`. `P = 0.5`
  uses the paper's `L = 0` special case (do not divide by `logit(0.5)`).
- Return `null` parameters when `n < 2` or `VRT ≤ 0` (the equations are
  undefined). Do not clamp or invent a default fit.
- Tests are the published-equation contract: crafted `(P, MRT, VRT, n)` at
  the function boundary (`unit-testing.mdc` pure-contract exception). No
  Spring, no HTTP.

### 3. Trailing three-morning MCQ EZ-diffusion is reported — Behavior `[ ]`

**Precondition:** current user has MCQ recall answers in the request
timezone's local dates `[today−2, today]` (three calendar mornings, empty
days contribute zero trials). Qualifying trial: `QuestionType.MCQ`, counts
as a review, has on-task RT, and is not implausibly fast — reuse
`RecallPaceAggregator`'s exclusion set and `RecallAnswerRow.rawElapsedMs`
(same on-task definition as pace). Spelling is out of scope: no symmetric
two-choice structure for EZ.

**Trigger:** `GET /api/user/recall-ez-diffusion?timezone=` — internal,
current-user-only, no frontend page. Same precedent as slice 1.

**Postcondition:** JSON `{ driftRate, boundarySeparation, nondecisionTimeMs,
trialCount, morningCount }`. `morningCount` is how many of the three local
dates had ≥1 qualifying MCQ. When `trialCount < 30`, the three fit fields
are `null` and `trialCount` / `morningCount` still report (EZ needs 30–50
trials; 30 is the floor to emit a fit). Do not fit a single morning even
when that morning alone has ≥30 trials — the window is rolling three
mornings because RT variance is noisy.

MCQ is scored as two-boundary correct-vs-error (not n-choice). That is the
EZ assumption; 3–4 option stems still collapse to correct vs error.

---

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

## Design decisions for slices 2–3

- **Internal diagnostic first**, same as slice 1. This plan is analyses, not
  Recall Stats tiles. A user-facing caution/capacity readout is a later
  plan if this diagnostic is worth showing.
- **Wagenmakers 2007 EZ**, not a numerical diffusion fit. Closed form matches
  the “is it worth it” bar; RT variance stays the noisy input, hence three
  mornings never daily.
- **MCQ only**, correct-vs-error boundaries. Spelling stays out.

## Permanent artifacts (capability-named)

| Artifact | Notes |
|---|---|
| `UserController.getDailyProbeConvergentValidity` — `GET /api/user/daily-probe-convergent-validity` | internal, current-user-only, no page |
| `RecallProbeConvergentValidity` | correlates the 4 matched pairs; mirrors the retired split-half diagnostic's shape |
| `DailyProbeConvergentValidityDTO` | `{ pairs: [{ pair, pairCount, rawCorrelation }] }` |
| `DailyProbeDaySeries.latestByLocalDay` | shared "latest probe per local day" grouping, extracted during this slice's refactor |
| `UserController` EZ-diffusion GET (slice 3, not shipped) | `GET /api/user/recall-ez-diffusion` |
| EZ closed-form helper (slice 2, not shipped) | Wagenmakers 2007; consumed only by slice 3 |

## Per-slice wrap-up

Per `.cursor/rules/planning.mdc`: test first and confirm it fails for the
right reason → smallest change to green → `post-change-refactor` on the
uncommitted change → update this plan → commit and push before the next
slice. Targeted `cypress run --spec` only, never the full suite. Unfinished
E2E stays `@wip`; never commit on red. Slices 2–3 are internal diagnostics:
controller/unit tests, no E2E page.
