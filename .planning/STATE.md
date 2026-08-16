---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: null
current_phase_name: null
status: ready
stopped_at: null
last_updated: "2026-08-16T13:30:00Z"
last_activity: 2026-08-16
last_activity_desc: "Tutor score 3 is FSRS-6 Hard; leftover ladder 2/1/0 + confusion"
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

Difficulty is shown on the Memory Tracker Information card (API number, or **N/A** when unset). Ordinary correct persists FSRS-6 Good next-D; ordinary incorrect persists Again next-D (due stays +12h). Commissioned Tutor score 4 uses the same `recalledSuccessfully` Good update as ordinary correct. Score **5** is FSRS-6 Easy; score **3** is FSRS-6 Hard (New D=5/S=24h; on-time S=71). Proposed ADR 0003 stays Proposed.

**Ops leftover:** gated dummy-skip conversions. Enable on the deploy that first applies each version, then revert to `1=0`:
- `dummy_note_sequence_skip_convert` (`V300000254`)
- `dummy_property_sequence_skip_convert` (`V300000255`)
JDBC harnesses remain temporary until those production applications.

**Recently shipped:** Difficulty on the Memory Tracker page (Information card; N/A when unset). FSRS-6 Good next Stability and Difficulty on ordinary correct recall (first Stability 24h; E2E day lists follow FSRS). Ordinary incorrect uses FSRS-6 post-lapse Stability and Again Difficulty (due stays +12h). Stability as whole hours; overdue correct lengthens Stability more than on-time. Unanswered recall-prompt history omits the MCQ solution; nested `/api/mcqs` routes are `/{note}`, `/refine`, `/generate`, `/export` (no `question` segment). Recall prompt / MCQ noun alignment (OpenAPI `Mcq`, `/api/mcqs`, tables `mcq`/`answer`). Skip Memory Tracking leftover cohesion (unused skip-flag tests dropped; unassimilated sequence queries renamed). Skip Memory Tracking sequence opt-out + subscribe API + Settings/ADR copy. Accidental-match confusion adjustment + cleanup. Assimilation-sequence skip. Note toolbar overflow. Production hard-delete incident response (2026-08-12) — [MILESTONES.md](MILESTONES.md).

**Remaining FSRS gap:** planned in [014](quick/014-close-fsrs-scheduling-gap/PLAN.md) slice 2+. Leftover ladder on confusion + Tutor **2/1/0**. Tracker: [FSRS-COMPATIBILITY-GAP.md](research/FSRS-COMPATIBILITY-GAP.md). Seed: [SEED-004](seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md).

## Operator Next Steps

- Confirm production applied Flyway `V300000257` (table `mcq`), `V300000258` (table `answer`), `V300000259` (rename `stability`), `V300000260` (hours conversion + drop `space_intervals`), `V300000261` (difficulty column + graded backfill), and `V300000262` (leftover graded-row difficulty backfill)
- Enable dummy-skip conversion placeholders on the deploys that first apply V300000254 / V300000255
- Next FSRS: continue [014](quick/014-close-fsrs-scheduling-gap/PLAN.md) (Tutor 1/0/2, confusion, ADR locks)
