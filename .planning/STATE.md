---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: null
current_phase_name: null
status: ready
stopped_at: null
last_updated: "2026-08-15T16:40:00Z"
last_activity: 2026-08-15
last_activity_desc: "quick/004 done: Difficulty on correct recall (FSRS Good SInc + D-update + 24h first S); E2E day lists follow FSRS"
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

Difficulty on correct recall is **done**. Ordinary correct uses FSRS-6 Good SInc and next-D (hidden Difficulty; first success D=5, S=24h). Proposed ADR 0003 stays Proposed. Fail / confusion / commissioned still use the Fibonacci ladder.

**Ops leftover:** gated dummy-skip conversions. Enable on the deploy that first applies each version, then revert to `1=0`:
- `dummy_note_sequence_skip_convert` (`V300000254`)
- `dummy_property_sequence_skip_convert` (`V300000255`)
JDBC harnesses remain temporary until those production applications.

**Recently shipped:** FSRS-6 Good SInc and Difficulty on ordinary correct recall (first S=24h; E2E day lists follow FSRS). Stability as whole hours; overdue correct lengthens Stability more than on-time. Unanswered recall-prompt history omits the MCQ solution; nested `/api/mcqs` routes are `/{note}`, `/refine`, `/generate`, `/export` (no `question` segment). Recall prompt / MCQ noun alignment (OpenAPI `Mcq`, `/api/mcqs`, tables `mcq`/`answer`). Skip Memory Tracking leftover cohesion (unused skip-flag tests dropped; unassimilated sequence queries renamed). Skip Memory Tracking sequence opt-out + subscribe API + Settings/ADR copy. Accidental-match confusion adjustment + cleanup. Assimilation-sequence skip. Note toolbar overflow. Production hard-delete incident response (2026-08-12) — [MILESTONES.md](MILESTONES.md).

**Remaining FSRS gap:** requested retention (B2), relearning, optional RecallLog, leftover ladder on fail/confusion/commissioned. Tracker: [FSRS-COMPATIBILITY-GAP.md](research/FSRS-COMPATIBILITY-GAP.md). Seed: [SEED-004](seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md).

## Operator Next Steps

- Confirm production applied Flyway `V300000257` (table `mcq`), `V300000258` (table `answer`), `V300000259` (rename `stability`), `V300000260` (hours conversion + drop `space_intervals`), and `V300000261` (difficulty column + graded backfill)
- Enable dummy-skip conversion placeholders on the deploys that first apply V300000254 / V300000255
- Next FSRS: B2 / relearning / RecallLog ([FSRS-COMPATIBILITY-GAP.md](research/FSRS-COMPATIBILITY-GAP.md))
