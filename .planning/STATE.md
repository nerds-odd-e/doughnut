---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: 10
current_phase_name: Remove from recall on assimilation settings (note)
status: in_progress
stopped_at: "Paused after Phase 9 wrap-up; resume execute-plan at Phase 10"
last_updated: "2026-08-14T05:45:00Z"
last_activity: 2026-08-14
last_activity_desc: Phases 1–9 done; next is Remove from recall on assimilation settings
progress:
  total_phases: 17
  completed_phases: 9
  total_plans: 1
  completed_plans: 0
  percent: 53
---

# Project State

## Project Reference

See: `.planning/PROJECT.md`

**Core value:** Healthy mainline for learning and knowledge work

## Current Position

Ad-hoc plan in progress: [`.planning/quick/001-skip-assimilation/PLAN.md`](quick/001-skip-assimilation/PLAN.md). **Phases 1–9 done.** Next: Phase 10 (Remove from recall on assimilation settings for an assimilated note).

**Ops leftover:** production dummy-skip conversion is gated. Enable `spring.flyway.placeholders.dummy_note_sequence_skip_convert=1=1` on a deliberate deploy, then revert to `1=0`. JDBC harness `NoteLevelDummySequenceSkipConversionTest` is temporary until that application.

**Recently shipped:** note toolbar keeps on-state Audio/Assimilation on the bar; other actions overflow from the right into More options. Production hard-delete incident response (2026-08-12) — [MILESTONES.md](MILESTONES.md).

**In discussion:** finalize Proposed ADR 0003 using [FSRS-COMPATIBILITY-GAP.md](research/FSRS-COMPATIBILITY-GAP.md). **Recall** (not FSRS **review**) is locked in ADR 0001 and ADR 0003 Decision. FSRS overdue reward is not implemented. `RecallLog` is deferred.

## Operator Next Steps

- Continue execute-plan: Phase 10 of `.planning/quick/001-skip-assimilation/PLAN.md`
- Enable production dummy-skip conversion (`dummy_note_sequence_skip_convert=1=1`) on a deliberate deploy when ready
- Or continue ADR 0003: resolve open issues in `.planning/research/FSRS-COMPATIBILITY-GAP.md`
