# Cognitive probe convergent-validity and latency-modeling analyses

**Status:** planned. Slice 1 is scoped and ready to execute. Slice 2 is
gated on a developer-run precondition check (see slice 2) and should not be
decomposed further until that resolves.
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

### 1. Convergent validity is reported per component readout — Behavior `[ ]`

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

### 2. Rolling EZ-diffusion separates caution from capacity — Behavior `[ ]` (gated, not ready)

**Status:** blocked on a precondition check below. Do not decompose into
real Behavior/Structure slices until it resolves.

**Precondition check (analysis only, not a coded deliverable):** query a
learner's recent recall history for how often pace (`pctVsUsual`) and
accuracy (standardized residual) move in opposite directions on the same
morning (one better-than-usual, one worse). **Not run yet** — the local dev
database (`doughnut_development`) currently has no tables/data, so this
needs to be run by the developer against a real (dev-with-data or
production) instance, or by an agent with access to one.

**Trigger threshold (decided in this planning pass):** slice 2 is worth
decomposing only if opposite-direction mornings occur in **≥15–20%** of
qualifying morning-pairs. Below that, the divergence is more plausibly noise
than a caution/capacity signal worth EZ-diffusion's cost.

**Next step once the rate is known:**
- rate ≥ threshold → run `/slice-planning` again on slice 2 alone to
  decompose it into real Behavior/Structure slices.
- rate < threshold → drop slice 2 from this plan.

**Unchanged from the original — still the model's real preconditions once
(if) it's triggered:** EZ-diffusion assumes two-choice symmetric boundaries
and needs 30–50 trials per fit; RT variance is its noisiest input; the
window is rolling over three mornings, never daily, because of that noise;
scoped to the MCQ subset only (spelling has no symmetric two-choice
structure for EZ to fit).

---

## Permanent artifacts (capability-named)

| Artifact (once slice 1 ships) | Notes |
|---|---|
| `UserController` endpoint `GET /api/user/daily-probe-convergent-validity` | internal, current-user-only, no page |
| convergent-validity service (name TBD at implementation time, e.g. `DailyProbeConvergentValidity`) | mirrors the retired split-half diagnostic's shape |

Slice 2 has no permanent artifacts yet — still gated (see above).

## Per-slice wrap-up

Per `.cursor/rules/planning.mdc`: test first and confirm it fails for the
right reason → smallest change to green → `post-change-refactor` on the
uncommitted change → update this plan → commit and push before the next
slice. Targeted `cypress run --spec` only, never the full suite. Unfinished
E2E stays `@wip`; never commit on red.

**Before executing slice 2:** run its precondition check first (see above)
and re-run `/slice-planning` on it once the rate is known — it intentionally
stays underspecified until then.
