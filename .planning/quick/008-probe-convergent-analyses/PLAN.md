# Cognitive probe convergent-validity and latency-modeling analyses

**Status:** planned, not started, and **not fully scoped**. Extracted from
`.planning/quick/001-morning-cognitive-index/PLAN.md` (slices 32–33 there),
where both were flagged as needing re-scoping because they were written
assuming things that no longer exist: a composite morning index (dropped —
its reliability gate failed decisively on real data) and slice 24's
contribution-bars visualization (dropped along with the index it would have
explained). This extraction re-scopes each slice's *dependency*, below, but
does not add new research, estimation, or review beyond that — treat the
re-scoped text as a starting hypothesis for a proper planning pass, not a
ready-to-execute spec.
**Type:** ad-hoc plan (`.planning/quick/`)
**Depends on:** `.planning/quick/007-daily-cognitive-probe/PLAN.md` (slice 1
needs the probe fully built and shipped — it has no value with zero probe
history); `.planning/quick/001-morning-cognitive-index/PLAN.md` for the
shipped component readouts (pace, accuracy, consistency, lapse count) slice
1 validates against.

## Goal

Two analyses shelved when the plan they were originally written for changed
shape: whether an independent daily probe agrees with what the
recall-derived component readouts say, and whether a rolling
diffusion-model decomposition of MCQ latencies is worth building at all.
Neither slice below should be executed as-is without a fresh planning pass —
this plan preserves the original intent and records what changed, not a
finished design.

## Slices

Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

### 1. Convergent validity is reported against the shipped component readouts — Behavior `[ ]` (re-scoped, needs planning)

**Original text** (`quick/001` slice 32): "Internal diagnostic: index versus
probe on mornings with both, compared against the index-versus-raw-accuracy
correlation. The probe is an independent speeded task with no shared item
structure, which is what makes it a usable criterion."

**Why it needs re-scoping:** "the index" no longer exists.
`quick/001`'s composite morning index was dropped after its split-half
reliability gate failed on real production data (`pairCount: 91`,
`rawCorrelation: 0.076`, `spearmanBrownCorrelation: 0.141` — all far below
the ~0.6 threshold). There is nothing single-valued left to validate the
probe against, and the original comparison term
("index-versus-raw-accuracy correlation") assumed the index existed too.

**Re-scoped direction (decided during this extraction, not yet reviewed or
estimated further):** compare the probe against each of the four shipped
component readouts individually — accuracy (standardized residual), pace
(`pctVsUsual`), lapse count, and consistency (`consistencyZScore`) — on
mornings with both a probe result and qualifying recall data, rather than
against one composite. This is arguably more informative than the original
plan: the composite's own reliability problem came from combining four
components of unknown individual reliability into one number, so per-component
correlations would show which (if any) component the probe actually agrees
with, instead of laundering that question through a discredited composite.

**Open questions a real planning pass must answer before this is a slice:**
- Minimum morning-pair count for a trustworthy correlation. `quick/001`
  used 10 as its floor for one composite correlation; unclear whether that
  transfers to four separate, smaller-sample correlations.
- Whether running four correlations instead of one needs any
  multiple-comparison adjustment at this exploratory-diagnostic (not
  ship-a-claim) stage.
- Where this surfaces: `quick/001` slice 21.4 shipped its diagnostic as an
  internal, current-user-only endpoint with no page — same precedent likely
  applies here, but worth confirming rather than assuming.

### 2. Rolling EZ-diffusion separates caution from capacity — Behavior `[ ]` (re-scoped trigger condition, needs planning)

**Original text** (`quick/001` slice 33): "Drift rate and boundary
separation over a rolling three-morning window, on the MCQ subset, fitted on
residualized latencies... Only worth building if slice 24 shows speed and
accuracy moving against each other often enough to matter."

**Why it needs re-scoping:** slice 24 ("contribution bars explain the
index") was dropped along with the composite index it would have
visualized — it never shipped, so its trigger condition ("shows speed and
accuracy moving against each other") can never fire as written.

**Re-scoped trigger condition (decided during this extraction, not yet
run):** substitute a direct historical check against the two components
that already ship — pace (`pctVsUsual`) and accuracy (standardized
residual) — instead of the dropped visualization. Before committing to this
slice, run a one-off query against a learner's recent history: do mornings
where pace and accuracy move in opposite directions (one better-than-usual,
one worse) occur often enough to justify a model this specialized? This
check is analysis, not a coded deliverable — it gates whether slice 2 is
worth planning further at all.

**Unchanged from the original — still the model's real preconditions once
(if) it's triggered:** EZ-diffusion assumes two-choice symmetric boundaries
and needs 30–50 trials per fit; RT variance is its noisiest input; the
window is rolling over three mornings, never daily, because of that noise;
scoped to the MCQ subset only (spelling has no symmetric two-choice
structure for EZ to fit).

---

## Permanent artifacts (capability-named)

None yet — both slices above are diagnostics without a committed UI or
feature-file surface; a real planning pass should decide this per slice
(see "Open questions" and "Unchanged from the original" notes above).

## Per-slice wrap-up

Per `.cursor/rules/planning.mdc`: test first and confirm it fails for the
right reason → smallest change to green → `post-change-refactor` on the
uncommitted change → update this plan → commit and push before the next
slice. Targeted `cypress run --spec` only, never the full suite. Unfinished
E2E stays `@wip`; never commit on red.

**Before executing either slice above:** run it through `/slice-planning`
again once its open questions are answered — this plan intentionally leaves
both underspecified rather than guessing a resolution that wasn't asked for.
