---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: "2"
current_phase_name: Walkthrough does not offer a flagged notebook
status: executing
stopped_at: null
last_updated: "2026-08-15T07:55:00Z"
last_activity: 2026-08-15
last_activity_desc: Phase 1 done — flagged notebooks omitted from assimilation sequence
progress:
  total_phases: 5
  completed_phases: 1
  total_plans: 1
  completed_plans: 0
  percent: 20
---

# Project State

## Project Reference

See: `.planning/PROJECT.md`

**Core value:** Healthy mainline for learning and knowledge work

## Current Position

Ad-hoc plan in progress: [`.planning/quick/001-skip-memory-tracking/PLAN.md`](quick/001-skip-memory-tracking/PLAN.md). Phase 1 done: notebooks with **Skip Memory Tracking** are absent from `GET /api/assimilation/next` and unassimilated counts (owned notes, property units, subscribed notebooks). Next: Phase 2 E2E walkthrough.

**Ops leftover:** gated dummy-skip conversions. Enable on the deploy that first applies each version, then revert to `1=0`:
- `dummy_note_sequence_skip_convert` (`V300000254`)
- `dummy_property_sequence_skip_convert` (`V300000255`)
JDBC harnesses remain temporary until those production applications.

**Recently shipped:** accidental-match confusion adjustment + cleanup. Assimilation-sequence skip. Note toolbar overflow. Production hard-delete incident response (2026-08-12) — [MILESTONES.md](MILESTONES.md).

**In discussion:** finalize Proposed ADR 0003 using [FSRS-COMPATIBILITY-GAP.md](research/FSRS-COMPATIBILITY-GAP.md). **Recall** (not FSRS **review**) is locked in ADR 0001 and ADR 0003 Decision.

## Operator Next Steps

- Continue execute-plan on remaining phases 2–5 of `.planning/quick/001-skip-memory-tracking/PLAN.md`
- Enable dummy-skip conversion placeholders on the deploys that first apply V300000254 / V300000255
- Continue ADR 0003: resolve open issues in `.planning/research/FSRS-COMPATIBILITY-GAP.md`
