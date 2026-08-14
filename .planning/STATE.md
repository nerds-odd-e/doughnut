---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: 3
current_phase_name: Weaken a unique matched spelling tracker
status: executing
stopped_at: null
last_updated: "2026-08-14T16:39:00Z"
last_activity: 2026-08-14
last_activity_desc: 003 Phase 2 done — quiz_answer FK to confusion-adjusted tracker
progress:
  total_phases: 6
  completed_phases: 2
  total_plans: 1
  completed_plans: 0
  percent: 33
---

# Project State

## Project Reference

See: `.planning/PROJECT.md`

**Core value:** Healthy mainline for learning and knowledge work

## Current Position

Executing `.planning/quick/003-accidental-match-confusion-adjustment/` Phase 3: weaken a unique matched spelling tracker without recall credit. Phase 2 shipped: nullable `quiz_answer.confusion_adjusted_memory_tracker_id`.

**Ops leftover:** gated dummy-skip conversions. Enable on the deploy that first applies each version, then revert to `1=0`:
- `dummy_note_sequence_skip_convert` (`V300000254`)
- `dummy_property_sequence_skip_convert` (`V300000255`)
JDBC harnesses remain temporary until those production applications.

**Recently shipped:** assimilation-sequence skip (table, Skip / Return to sequence / Remove from recall, dummy-tracker conversion gated). Note toolbar keeps on-state Audio/Assimilation on the bar; other actions overflow from the right into More options. Production hard-delete incident response (2026-08-12) — [MILESTONES.md](MILESTONES.md).

**In discussion:** finalize Proposed ADR 0003 using [FSRS-COMPATIBILITY-GAP.md](research/FSRS-COMPATIBILITY-GAP.md). **Recall** (not FSRS **review**) is locked in ADR 0001 and ADR 0003 Decision. FSRS overdue reward is not implemented. `RecallLog` is deferred.

## Operator Next Steps

- Enable dummy-skip conversion placeholders on the deploys that first apply V300000254 / V300000255
- Continue ADR 0003: resolve open issues in `.planning/research/FSRS-COMPATIBILITY-GAP.md`
- Continue execute-plan `.planning/quick/003-accidental-match-confusion-adjustment/` (Phase 3 next)
