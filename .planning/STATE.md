---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: 15
current_phase_name: Convert property-level dummy skipped trackers
status: in_progress
stopped_at: "Paused after Phase 14 wrap-up; resume execute-plan at Phase 15"
last_updated: "2026-08-14T08:10:00Z"
last_activity: 2026-08-14
last_activity_desc: Phases 1–14 done; next is migrate property-level dummy skips
progress:
  total_phases: 17
  completed_phases: 14
  total_plans: 1
  completed_plans: 0
  percent: 82
---

# Project State

## Project Reference

See: `.planning/PROJECT.md`

**Core value:** Healthy mainline for learning and knowledge work

## Current Position

Ad-hoc plan in progress: [`.planning/quick/001-skip-assimilation/PLAN.md`](quick/001-skip-assimilation/PLAN.md). **Phases 1–14 done.** Next: Phase 15 (migrate property-level dummy skipped trackers with `recall_count = 0`).

**Ops leftover:** production dummy-skip conversion is gated. Enable `spring.flyway.placeholders.dummy_note_sequence_skip_convert=1=1` on a deliberate deploy, then revert to `1=0`. JDBC harness `NoteLevelDummySequenceSkipConversionTest` is temporary until that application. Phase 15 adds the property-level conversion.

**Recently shipped:** note toolbar keeps on-state Audio/Assimilation on the bar; other actions overflow from the right into More options. Production hard-delete incident response (2026-08-12) — [MILESTONES.md](MILESTONES.md).

**In discussion:** finalize Proposed ADR 0003 using [FSRS-COMPATIBILITY-GAP.md](research/FSRS-COMPATIBILITY-GAP.md). **Recall** (not FSRS **review**) is locked in ADR 0001 and ADR 0003 Decision. FSRS overdue reward is not implemented. `RecallLog` is deferred.

## Operator Next Steps

- Continue execute-plan: Phase 15 of `.planning/quick/001-skip-assimilation/PLAN.md`
- Enable production dummy-skip conversion (`dummy_note_sequence_skip_convert=1=1`) on a deliberate deploy when ready
- Or continue ADR 0003: resolve open issues in `.planning/research/FSRS-COMPATIBILITY-GAP.md`
