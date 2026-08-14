---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: 2
current_phase_name: Durable causal link to one adjusted tracker
status: executing
stopped_at: null
last_updated: "2026-08-14T16:27:00Z"
last_activity: 2026-08-14
last_activity_desc: 003 Phase 1 done — accidental match fully fails prompted spelling tracker
progress:
  total_phases: 6
  completed_phases: 1
  total_plans: 1
  completed_plans: 0
  percent: 17
---

# Project State

## Project Reference

See: `.planning/PROJECT.md`

**Core value:** Healthy mainline for learning and knowledge work

## Current Position

Executing `.planning/quick/003-accidental-match-confusion-adjustment/` Phase 2: nullable `quiz_answer` FK to the confusion-adjusted tracker. Phase 1 shipped: accidental match fully fails prompted tracker A.

**Ops leftover:** gated dummy-skip conversions. Enable on the deploy that first applies each version, then revert to `1=0`:
- `dummy_note_sequence_skip_convert` (`V300000254`)
- `dummy_property_sequence_skip_convert` (`V300000255`)
JDBC harnesses remain temporary until those production applications.

**Recently shipped:** assimilation-sequence skip (table, Skip / Return to sequence / Remove from recall, dummy-tracker conversion gated). Note toolbar keeps on-state Audio/Assimilation on the bar; other actions overflow from the right into More options. Production hard-delete incident response (2026-08-12) — [MILESTONES.md](MILESTONES.md).

**In discussion:** finalize Proposed ADR 0003 using [FSRS-COMPATIBILITY-GAP.md](research/FSRS-COMPATIBILITY-GAP.md). **Recall** (not FSRS **review**) is locked in ADR 0001 and ADR 0003 Decision. FSRS overdue reward is not implemented. `RecallLog` is deferred.

## Operator Next Steps

- Enable dummy-skip conversion placeholders on the deploys that first apply V300000254 / V300000255
- Continue ADR 0003: resolve open issues in `.planning/research/FSRS-COMPATIBILITY-GAP.md`
- Continue execute-plan `.planning/quick/003-accidental-match-confusion-adjustment/` (Phase 2 next)
