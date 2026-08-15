---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: "5"
current_phase_name: Copy and glossary match the decided concept
status: executing
stopped_at: null
last_updated: "2026-08-15T08:50:00Z"
last_activity: 2026-08-15
last_activity_desc: Phase 4 done — subscribe API rejects Skip Memory Tracking
progress:
  total_phases: 5
  completed_phases: 4
  total_plans: 1
  completed_plans: 0
  percent: 80
---

# Project State

## Project Reference

See: `.planning/PROJECT.md`

**Core value:** Healthy mainline for learning and knowledge work

## Current Position

Ad-hoc plan in progress: [`.planning/quick/001-skip-memory-tracking/PLAN.md`](quick/001-skip-memory-tracking/PLAN.md). Phases 1–4 done: flagged notebooks are out of the assimilation sequence; assimilate-on-note still joins recall; subscribe API returns 400. Next: Phase 5 Settings/ADR copy.

**Ops leftover:** gated dummy-skip conversions. Enable on the deploy that first applies each version, then revert to `1=0`:
- `dummy_note_sequence_skip_convert` (`V300000254`)
- `dummy_property_sequence_skip_convert` (`V300000255`)
JDBC harnesses remain temporary until those production applications.

**Recently shipped:** accidental-match confusion adjustment + cleanup. Assimilation-sequence skip. Note toolbar overflow. Production hard-delete incident response (2026-08-12) — [MILESTONES.md](MILESTONES.md).

**In discussion:** finalize Proposed ADR 0003 using [FSRS-COMPATIBILITY-GAP.md](research/FSRS-COMPATIBILITY-GAP.md). **Recall** (not FSRS **review**) is locked in ADR 0001 and ADR 0003 Decision.

## Operator Next Steps

- Continue execute-plan on remaining phase 5 of `.planning/quick/001-skip-memory-tracking/PLAN.md`
- Enable dummy-skip conversion placeholders on the deploys that first apply V300000254 / V300000255
- Continue ADR 0003: resolve open issues in `.planning/research/FSRS-COMPATIBILITY-GAP.md`
