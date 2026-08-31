---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: null
current_phase_name: null
status: ready
stopped_at: null
last_updated: "2026-08-31T02:19:00Z"
last_activity: 2026-08-31
last_activity_desc: "Retired all shipped quick plans (013–017); product + ADRs are the record"
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

- After deploying the Flyway squash, confirm startup succeeded and `flyway_schema_history` shows the baseline repaired, the collapsed versions marked `DELETE`, and `V300000300` applied.
- Start the next milestone with `/gsd-new-milestone` when ready.

Parked work: SEED-001, SEED-002, SEED-005, SEED-006, SEED-007, SEED-008; ADR 0002 Level 1. See [ROADMAP.md](ROADMAP.md).

Recent ad-hoc work (no active plans): `noteProperty` / **property panel** / `#prop:` wiki (ADR 0001 / ADR 0004 / Proposed ADR 0005); E2E named-route honesty and SPA hydrate protocol (E2E helpers, `MainMenu.vue`); daily probe side tap ([daily-probe-protocol.md](notes/daily-probe-protocol.md)).
