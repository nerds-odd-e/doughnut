---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: null
current_phase_name: null
status: ready
stopped_at: null
last_updated: "2026-08-17T08:10:00Z"
last_activity: 2026-08-17
last_activity_desc: "016 slice 1: r=0.9 named; due hours I(0.9,S)=S; ordinary-incorrect +12h still in place; ADR 0003 still Proposed"
progress:
  total_phases: 0
  completed_phases: 0
  total_plans: 0
  completed_plans: 0
  percent: 0
---

# Project State

## Project Reference

See: `.planning/PROJECT.md`

**Core value:** Healthy mainline for learning and knowledge work

## Current Position

Difficulty is shown on the Memory Tracker Information card (API number, or **N/A** when unset). Scheduling policy lives in Proposed [ADR 0003](../docs/adrs/0003-spaced-repetition-scheduling-policy.md) Decision.

**Ops leftover:** gated dummy-skip conversions. Enable on the deploy that first applies each version, then revert to `1=0`:
- `dummy_note_sequence_skip_convert` (`V300000254`)
- `dummy_property_sequence_skip_convert` (`V300000255`)
JDBC harnesses remain temporary until those production applications.

**Recently shipped:** Difficulty on the Memory Tracker page (Information card; N/A when unset). FSRS-6 Good next Stability and Difficulty on ordinary correct recall (first Stability 24h; E2E day lists follow FSRS). Ordinary incorrect uses FSRS-6 post-lapse Stability and Again Difficulty (due stays +12h). Stability as whole hours; overdue correct lengthens Stability more than on-time. Unanswered recall-prompt history omits the MCQ solution; nested `/api/mcqs` routes are `/{note}`, `/refine`, `/generate`, `/export` (no `question` segment). Recall prompt / MCQ noun alignment (OpenAPI `Mcq`, `/api/mcqs`, tables `mcq`/`answer`). Skip Memory Tracking leftover cohesion (unused skip-flag tests dropped; unassimilated sequence queries renamed). Skip Memory Tracking sequence opt-out + subscribe API + Settings/ADR copy. Accidental-match confusion adjustment + cleanup. Assimilation-sequence skip. Note toolbar overflow. Production hard-delete incident response (2026-08-12) — [MILESTONES.md](MILESTONES.md).

**Remaining FSRS gap:** B2 planned as locked global `r = 0.9` ([016](quick/016-requested-retention-interval/PLAN.md)); then deferred B4 / C4 / E3 / E4 / E6 plus **accept ADR 0003** (human). Tracker: [FSRS-COMPATIBILITY-GAP.md](research/FSRS-COMPATIBILITY-GAP.md). Seed: [SEED-004](seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md).

## Operator Next Steps

- Confirm production applied Flyway `V300000257` (table `mcq`), `V300000258` (table `answer`), `V300000259` (rename `stability`), `V300000260` (hours conversion + drop `space_intervals`), `V300000261` (difficulty column + graded backfill), and `V300000262` (leftover graded-row difficulty backfill)
- Enable dummy-skip conversion placeholders on the deploys that first apply V300000254 / V300000255
- Next FSRS: [016](quick/016-requested-retention-interval/PLAN.md) (lock `r = 0.9`, fail due from `I(r, S)`). Then humans accept Proposed ADR 0003, or pick a remaining deferred knob (B4 / C4 / RecallLog).
