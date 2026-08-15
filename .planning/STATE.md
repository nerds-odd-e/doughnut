---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: null
current_phase_name: null
status: idle
stopped_at: null
last_updated: "2026-08-15T09:20:00Z"
last_activity: 2026-08-15
last_activity_desc: 004-recall-mcq-followup done (unanswered history omits solution; nested /api/mcqs paths have no question segment)
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

004-recall-mcq-followup is **done** — unanswered recall-prompt history omits the MCQ solution; nested `/api/mcqs` routes have no `question` segment (`/{note}`, `/refine`, `/generate`, `/export`).

**Ops leftover:** gated dummy-skip conversions. Enable on the deploy that first applies each version, then revert to `1=0`:
- `dummy_note_sequence_skip_convert` (`V300000254`)
- `dummy_property_sequence_skip_convert` (`V300000255`)
JDBC harnesses remain temporary until those production applications.

**Recently shipped:** Unanswered recall-prompt history omits the MCQ solution; nested `/api/mcqs` routes are `/{note}`, `/refine`, `/generate`, `/export` (no `question` segment). Recall prompt / MCQ noun alignment (OpenAPI `Mcq`, `/api/mcqs`, tables `mcq`/`answer`). Skip Memory Tracking leftover cohesion (unused skip-flag tests dropped; unassimilated sequence queries renamed). Skip Memory Tracking sequence opt-out + subscribe API + Settings/ADR copy. Accidental-match confusion adjustment + cleanup. Assimilation-sequence skip. Note toolbar overflow. Production hard-delete incident response (2026-08-12) — [MILESTONES.md](MILESTONES.md).

**In discussion:** finalize Proposed ADR 0003. **A1 locked** (own FSRS-compatible implementation). **B3 locked** (overdue correct: bounded extra growth). Remaining gaps close by vertical slice. Tracker: [FSRS-COMPATIBILITY-GAP.md](research/FSRS-COMPATIBILITY-GAP.md). Seed: [SEED-004](seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md).

## Operator Next Steps

- Confirm production applied Flyway `V300000257` (table `mcq`) and `V300000258` (table `answer`)
- Enable dummy-skip conversion placeholders on the deploys that first apply V300000254 / V300000255
- Continue ADR 0003: inspect B3 Decision, then implement overdue extra on current index (`ForgettingCurve.succeeded`). Remaining discussion: B2, C–E in `.planning/research/FSRS-COMPATIBILITY-GAP.md`
