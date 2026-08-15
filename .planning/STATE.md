---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: null
current_phase_name: null
status: executing
stopped_at: null
last_updated: "2026-08-15T10:20:00Z"
last_activity: 2026-08-15
last_activity_desc: 005-overdue-correct-stability slice 3 done (memory tracker field is stability)
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

Executing [`.planning/quick/005-overdue-correct-stability/PLAN.md`](quick/005-overdue-correct-stability/PLAN.md). Slices 1–3 done. Next: slice 4 drop Settings/User API day list.

**Ops leftover:** gated dummy-skip conversions. Enable on the deploy that first applies each version, then revert to `1=0`:
- `dummy_note_sequence_skip_convert` (`V300000254`)
- `dummy_property_sequence_skip_convert` (`V300000255`)
JDBC harnesses remain temporary until those production applications.

**Recently shipped:** Unanswered recall-prompt history omits the MCQ solution; nested `/api/mcqs` routes are `/{note}`, `/refine`, `/generate`, `/export` (no `question` segment). Recall prompt / MCQ noun alignment (OpenAPI `Mcq`, `/api/mcqs`, tables `mcq`/`answer`). Skip Memory Tracking leftover cohesion (unused skip-flag tests dropped; unassimilated sequence queries renamed). Skip Memory Tracking sequence opt-out + subscribe API + Settings/ADR copy. Accidental-match confusion adjustment + cleanup. Assimilation-sequence skip. Note toolbar overflow. Production hard-delete incident response (2026-08-12) — [MILESTONES.md](MILESTONES.md).

**In progress:** [`.planning/quick/005-overdue-correct-stability/PLAN.md`](quick/005-overdue-correct-stability/PLAN.md) — slices 1–3 done. Next: slice 4 drop Settings/User API day list. Tracker: [FSRS-COMPATIBILITY-GAP.md](research/FSRS-COMPATIBILITY-GAP.md).

## Operator Next Steps

- Confirm production applied Flyway `V300000257` (table `mcq`) and `V300000258` (table `answer`)
- Enable dummy-skip conversion placeholders on the deploys that first apply V300000254 / V300000255
- Continue [`.planning/quick/005-overdue-correct-stability/PLAN.md`](quick/005-overdue-correct-stability/PLAN.md) from slice 4 (no day list in Settings or User API)
