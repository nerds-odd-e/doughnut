---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: 17
current_phase_name: Notebook Skip Memory Tracking language
status: in_progress
stopped_at: "Paused after Phase 16 wrap-up; resume execute-plan at Phase 17"
last_updated: "2026-08-14T08:55:00Z"
last_activity: 2026-08-14
last_activity_desc: Phases 1–16 done; next is notebook Skip Memory Tracking E2E language
progress:
  total_phases: 17
  completed_phases: 16
  total_plans: 1
  completed_plans: 0
  percent: 94
---

# Project State

## Project Reference

See: `.planning/PROJECT.md`

**Core value:** Healthy mainline for learning and knowledge work

## Current Position

Ad-hoc plan in progress: [`.planning/quick/001-skip-assimilation/PLAN.md`](quick/001-skip-assimilation/PLAN.md). **Phases 1–16 done.** Next: Phase 17 (E2E/steps call the notebook setting Skip Memory Tracking, not skip recall).

**Ops leftover:** gated dummy-skip conversions. Enable on the deploy that first applies each version, then revert to `1=0`:
- `dummy_note_sequence_skip_convert` (`V300000254`)
- `dummy_property_sequence_skip_convert` (`V300000255`)
JDBC harnesses are temporary until those production applications.

**Recently shipped:** note toolbar keeps on-state Audio/Assimilation on the bar; other actions overflow from the right into More options. Production hard-delete incident response (2026-08-12) — [MILESTONES.md](MILESTONES.md).

**In discussion:** finalize Proposed ADR 0003 using [FSRS-COMPATIBILITY-GAP.md](research/FSRS-COMPATIBILITY-GAP.md). **Recall** (not FSRS **review**) is locked in ADR 0001 and ADR 0003 Decision. FSRS overdue reward is not implemented. `RecallLog` is deferred.

## Operator Next Steps

- Continue execute-plan: Phase 17 of `.planning/quick/001-skip-assimilation/PLAN.md`
- Enable dummy-skip conversion placeholders on the deploys that first apply V300000254 / V300000255
- Or continue ADR 0003: resolve open issues in `.planning/research/FSRS-COMPATIBILITY-GAP.md`
