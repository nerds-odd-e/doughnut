---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: "2"
current_phase_name: Name unassimilated sequence queries as unassimilated
status: executing
stopped_at: null
last_updated: "2026-08-15T08:50:00Z"
last_activity: 2026-08-15
last_activity_desc: Phase 1 done — dropped skip-flag tests that did not hit a production branch
progress:
  total_phases: 2
  completed_phases: 1
  total_plans: 1
  completed_plans: 0
  percent: 50
---

# Project State

## Project Reference

See: `.planning/PROJECT.md`

**Core value:** Healthy mainline for learning and knowledge work

## Current Position

Ad-hoc plan in progress: [`.planning/quick/003-skip-memory-tracking-cleanup/PLAN.md`](quick/003-skip-memory-tracking-cleanup/PLAN.md). Phase 1 done (dropped unused skip-flag tests). Next: Phase 2 rename unassimilated sequence repository methods.

**Ops leftover:** gated dummy-skip conversions. Enable on the deploy that first applies each version, then revert to `1=0`:
- `dummy_note_sequence_skip_convert` (`V300000254`)
- `dummy_property_sequence_skip_convert` (`V300000255`)
JDBC harnesses remain temporary until those production applications.

**Recently shipped:** Skip Memory Tracking sequence opt-out + subscribe API + Settings/ADR copy. Accidental-match confusion adjustment + cleanup. Assimilation-sequence skip. Note toolbar overflow. Production hard-delete incident response (2026-08-12) — [MILESTONES.md](MILESTONES.md).

**In discussion:** finalize Proposed ADR 0003 using [FSRS-COMPATIBILITY-GAP.md](research/FSRS-COMPATIBILITY-GAP.md). **Recall** (not FSRS **review**) is locked in ADR 0001 and ADR 0003 Decision.

## Operator Next Steps

- Continue execute-plan Phase 2 of `.planning/quick/003-skip-memory-tracking-cleanup/PLAN.md`
- Enable dummy-skip conversion placeholders on the deploys that first apply V300000254 / V300000255
- Continue ADR 0003: resolve open issues in `.planning/research/FSRS-COMPATIBILITY-GAP.md`
