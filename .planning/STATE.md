---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: null
current_phase_name: null
status: ready
stopped_at: null
last_updated: "2026-08-30T13:34:00Z"
last_activity: 2026-08-30
last_activity_desc: "013 noteProperty shipped; 017 property wiki integrity planned (not started)"
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

Scheduling follows Accepted [ADR 0003](../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md). Difficulty is shown on the Memory Tracker Information card. `last_recalled_at` / `next_recall_at` are DATETIME; remaining TIMESTAMP columns are [SEED-006](seeds/SEED-006-remove-mysql-timestamp-2038.md).

**Flyway:** every applied migration is squashed into `V100000000__baseline.sql`; `V300000300__db_migration_placeholder.sql` is the tip. New migrations use a greater version.

Daily probe measurement (not an ADR): [daily-probe-protocol.md](notes/daily-probe-protocol.md).

## Operator Next Steps

- Canonical property path (`noteProperty`, **property panel**, `#prop:` wiki) shipped: `.planning/quick/013-note-property-canonical-path/PLAN.md`. Follow-up (OS-invalid `#prop:` suffix, case-distinct keys, dialog-closed test leftover): `.planning/quick/017-property-wiki-integrity/PLAN.md` — planned, do not execute until asked. Policy in ADR 0001 / ADR 0004 / Proposed 0005.
- Named SPA route honesty leftovers (Given shortcuts as named `push`) shipped; 011 / 014 / 015 PLANs retired. Recall/epub/identify/`circleJoin`/`loginAs`/`visitHomePage` stay `visitNamed`.
- After deploying the Flyway squash, confirm startup succeeded and `flyway_schema_history` shows the baseline repaired, the collapsed versions marked `DELETE`, and `V300000300` applied.

Parked work: SEED-001, SEED-002, SEED-005, SEED-006, SEED-007, SEED-008; ADR 0002 Level 1. See [ROADMAP.md](ROADMAP.md).
