---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: null
current_phase_name: null
status: executing
stopped_at: null
last_updated: "2026-08-15T09:05:00Z"
last_activity: 2026-08-15
last_activity_desc: 004-recall-mcq-followup slice 2 done (annotation-only Mcq path tests dropped)
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

[`.planning/quick/004-recall-mcq-followup/PLAN.md`](quick/004-recall-mcq-followup/PLAN.md) in progress — slices 1–2 done. Next: nested `/api/mcqs` routes with no `question` segment.

**Ops leftover:** gated dummy-skip conversions. Enable on the deploy that first applies each version, then revert to `1=0`:
- `dummy_note_sequence_skip_convert` (`V300000254`)
- `dummy_property_sequence_skip_convert` (`V300000255`)
JDBC harnesses remain temporary until those production applications.

**Recently shipped:** Recall prompt / MCQ noun alignment (OpenAPI `Mcq`, `/api/mcqs`, tables `mcq`/`answer`). Skip Memory Tracking leftover cohesion (unused skip-flag tests dropped; unassimilated sequence queries renamed). Skip Memory Tracking sequence opt-out + subscribe API + Settings/ADR copy. Accidental-match confusion adjustment + cleanup. Assimilation-sequence skip. Note toolbar overflow. Production hard-delete incident response (2026-08-12) — [MILESTONES.md](MILESTONES.md).

**In discussion:** finalize Proposed ADR 0003 using [FSRS-COMPATIBILITY-GAP.md](research/FSRS-COMPATIBILITY-GAP.md). **Recall** (not FSRS **review**) is locked in ADR 0001 and ADR 0003 Decision.

## Operator Next Steps

- Continue [`.planning/quick/004-recall-mcq-followup/PLAN.md`](quick/004-recall-mcq-followup/PLAN.md) — next: nested `/api/mcqs` routes with no `question` segment
- Confirm production applied Flyway `V300000257` (table `mcq`) and `V300000258` (table `answer`)
- Enable dummy-skip conversion placeholders on the deploys that first apply V300000254 / V300000255
- Continue ADR 0003: resolve open issues in `.planning/research/FSRS-COMPATIBILITY-GAP.md`
