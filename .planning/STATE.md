---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: null
current_phase_name: null
status: ready
stopped_at: null
last_updated: "2026-08-21T17:20:00Z"
last_activity: 2026-08-21
last_activity_desc: "Quick plan 003 slice 1 done (ADR blank-line); slice 2 MergedRelatedNotes test remaining"
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

## Operator Next Steps

- After deploying the Flyway squash, confirm startup succeeded and `flyway_schema_history` shows the baseline repaired, the collapsed versions marked `DELETE`, and `V300000300` applied
- Continue `.planning/quick/003-learning-session-request-review-followups/PLAN.md` — slice 2 (`MergedRelatedNotes` direct unit test) remaining; open question #3 (per-session-item token budget) still needs developer judgment if pursued
