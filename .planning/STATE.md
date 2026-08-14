---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: null
current_phase_name: null
status: idle
stopped_at: null
last_updated: "2026-08-14T08:15:00Z"
last_activity: 2026-08-14
last_activity_desc: 004-skip-assimilation-cleanup finished; spent planning files removed
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

No active execute-plan. Skip-assimilation cleanup shipped: shared JDBC conversion harness, shared assimilation selector helpers, tracker page shows type instead of the legacy spelling boolean.

**Ops leftover:** gated dummy-skip conversions. Enable on the deploy that first applies each version, then revert to `1=0`:
- `dummy_note_sequence_skip_convert` (`V300000254`)
- `dummy_property_sequence_skip_convert` (`V300000255`)
JDBC harnesses remain temporary until those production applications.

**Recently shipped:** assimilation-sequence skip (table, Skip / Return to sequence / Remove from recall, dummy-tracker conversion gated). Note toolbar keeps on-state Audio/Assimilation on the bar; other actions overflow from the right into More options. Production hard-delete incident response (2026-08-12) — [MILESTONES.md](MILESTONES.md).

**In discussion:** finalize Proposed ADR 0003 using [FSRS-COMPATIBILITY-GAP.md](research/FSRS-COMPATIBILITY-GAP.md). **Recall** (not FSRS **review**) is locked in ADR 0001 and ADR 0003 Decision. FSRS overdue reward is not implemented. `RecallLog` is deferred.

## Operator Next Steps

- Enable dummy-skip conversion placeholders on the deploys that first apply V300000254 / V300000255
- Continue ADR 0003: resolve open issues in `.planning/research/FSRS-COMPATIBILITY-GAP.md`
- Or queued quick plan `.planning/quick/003-accidental-match-confusion-adjustment/` if that workstream is next
