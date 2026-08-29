# Morning cognitive index from recall history

**Status:** closed.
**Type:** ad-hoc plan (`.planning/quick/`)
**Research memo:** https://claude.ai/code/artifact/9e13f954-fc5e-48e5-868f-f75d03f811c1

## Goal

Give the learner a daily readout of how their morning recall compared with what
the scheduler predicted for exactly the items that came due — a cognitive-state
signal rather than a restatement of what FSRS scheduled.

Every readout is a **residual**: observed outcome minus the expectation derived
from retrievability, difficulty and per-item time intensity, standardized
against that learner's own recent history.

## Outcome

**Shipped:** timer/interruption tracking on Recall History (away, detour, idle,
suspend-safe thinking time); Recall Stats' Pace tile (per-item EWMA
time-intensity residual, cold-start-weighted, winsorized against slow
outliers, plus a retrieval-lapse count); Accuracy tile (retrievability-based
standardized residual, with live 2PL recalibration and a fitted 3PL guessing
floor per question type); and a Consistency badge (weighted MAD of residuals
against a 60-day personal baseline). Vocabulary (*pace*, *retrieval lapse*,
*detour*, *away*, *idle*, *daily probe*, *cognitive index*) and the decision to
persist `stability_before`/`difficulty_before`/`retrievability` as a
materialized replay cache (not a new source of truth) are recorded in ADR 0001
and ADR 0003 (amended commit `5a8b19c085`).

**Not shipped — the composite morning index** (originally the "22–25" slices):
gated on split-half reliability ≥ ~0.6 across odd/even attempts within the same
morning. Re-run on 2026-08-29 against complete production data —
`pairCount: 91`, `rawCorrelation: 0.076`, `spearmanBrownCorrelation: 0.141` —
failed decisively on a fully-powered measurement, not a data-starved one. Per
the gate's own rule, weights were not tuned to rescue the number; the
composite is permanently dropped. The diagnostic code built solely to run that
gate (day-level pace/lapse baselines, odd/even half-index scoring, the
composite arithmetic, and the split-half reliability computation plus its
diagnostic-only endpoint) had no remaining consumer once the verdict was
recorded and has since been **deleted**, per
`.planning/quick/009-retire-split-half-reliability-gate/` (that plan is itself
closed and its directory removed). The shipped component readouts (pace,
accuracy, consistency, lapse count) never depended on any of the deleted code.

Relatedly, correcting the pace expectation for retrievability/difficulty (the
"17.1" slice) was **dropped, not implemented**: building that correction on
top of what was then thought to be a mostly-null retrievability signal
contradicted "keep it simple." That premise turned out to be a rollout timing
artifact rather than a real data gap, but the drop stands — the per-item EWMA
baseline ships uncorrected as the permanent pace expectation.

**Not started — the daily probe and its dependent analyses** (originally the
"26–33" slices): extracted to `.planning/quick/007-daily-cognitive-probe/PLAN.md`
(self-contained, does not depend on the dropped composite) and
`.planning/quick/008-probe-convergent-analyses/PLAN.md` (re-scoped around the
dropped composite; not ready to execute without a fresh planning pass).

## Key design decisions

- **Residual, not raw.** Daily accuracy and mean response time mostly measure
  which cards came due. Every metric here is scored against a per-attempt
  expectation.
- **Counts over means for the slow tail.** Implausibly fast attempts are dropped
  whole (they invalidate the accuracy observation too, not just the timing);
  slow attempts are winsorized rather than deleted, because genuine effortful
  retrieval is the signal, not the noise. The lapse count is robust by
  construction and carries the most weight.
- **Pause types are derived, never asked.** No pause button: compliance with a
  manual pause would fail exactly when the learner is tired or distracted,
  putting measurement error in step with the measurement target. "View last
  answered question" is not reused as a pause signal either — it already fires
  automatically on every wrong answer (`useRecallAnswerHandling.ts`), which is
  what sets `isRecallPaused`; that flag cannot be repurposed as a general
  interruption signal.
- **Wall-clock reconciliation is the guarantee; lifecycle events are an
  optimization.** `freeze`/`resume` listeners are deliberately *not* added — the
  gap detector already makes the total correct, and two mechanisms for one
  guarantee is defensive programming.
- **Choice count and stem length are not collected.** The guessing floor is
  fitted (3PL γ) instead of assumed as 1/k — a better model, since distractors
  are never equally plausible. Reading time lives inside per-item time intensity
  after a few exposures; the cold-start cost is handled by a confidence weight.
- **No self-report.** This removes the known-groups validation check and bounds
  the product claim: the index may say *unusual for you*, never *because you
  slept badly*. Copy must respect that.
- **Extend the existing projection.** `RecallStatsService` documents that its
  single projection query is what avoids the N+1 that once timed the endpoint
  out. Fields were added to `RecallAnswerRow` rather than a second query or
  entity hydration.
- **Pace and the AM/PM trend charts deliberately use different time-handling
  policies.** The trend charts (`RecallStatsAggregator.responseTimeMs`) keep
  their original 1s-drop/120s-cap; Pace/retention use uncapped on-task time
  (`RecallAnswerRow.rawElapsedMs()`) with their own item-relative floor and
  5-minute hard-drop. This is intentional, not an inconsistency to converge.

## Known open item

`recall_stats.feature`'s pace scenario stays `@wip`: its `answerSlowlyOnDay`
step fires generated-SDK calls whose underlying `fetch` dispatches eagerly at
call time rather than deferred to Cypress's command queue, so time-travel +
answer requests for different simulated days race against the backend's
shared testability clock. A fix needs the E2E step helpers to sequence
requests (await each before firing the next); backend/frontend unit tests are
the readout's primary coverage in the meantime.

## Permanent artifacts (capability-named)

| Artifact | Notes |
|----------|--------|
| `e2e_test/features/recall/recall_timing.feature` | away/detour/idle |
| `e2e_test/features/recall/recall_stats.feature` | pace/accuracy; pace scenario `@wip`, see above |
| `e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature` | view-last-answered-question clock pause |
| `scripts/` backfill script | historical `recall_log` memory-state backfill |

Confirmed: the deleted split-half-reliability diagnostic cluster
(`RecallSplitHalfReliability`/`RecallMorningHalfIndex`/`RecallCognitiveIndex`
and its endpoint) was never listed in this table — it was correctly treated as
diagnostic-only, never a permanent artifact.

`recall/daily_cognitive_probe.feature` and the extension to
`users/user_profile.feature` moved to
`.planning/quick/007-daily-cognitive-probe/PLAN.md` with the slices that
create them.
