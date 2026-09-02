---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: null
current_phase_name: null
status: ready
stopped_at: null
last_updated: "2026-09-02T03:31:00Z"
last_activity: 2026-09-02
last_activity_desc: "040: note-reference index hardening planned (do not execute until asked); 039 complete"
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

Daily probe measurement (not an ADR): [daily-probe-protocol.md](notes/daily-probe-protocol.md). Authoritative authored note references: `.planning/quick/039-authoritative-authored-note-references/PLAN.md` — complete. Follow-up: `.planning/quick/040-note-reference-index-hardening/PLAN.md`.

## Operator Next Steps

- `.planning/quick/040-note-reference-index-hardening/PLAN.md` — planned. Slice 11 (drop the one-time backfill) is gated: stop after slice 10 and confirm `authored_note_reference_backfill_progress.completed_at` on production before continuing. Do not execute until asked.
- After deploying the Flyway squash, confirm startup succeeded and `flyway_schema_history` shows the baseline repaired, the collapsed versions marked `DELETE`, and `V300000300` applied.
- Title/create slowness probe (skip in-request `refreshNotebookScope`): [issue note](notes/notebook-scope-wiki-refresh-on-title-and-create.md), plan `.planning/quick/038-skip-notebook-scope-refresh-on-title-and-create/PLAN.md` — planned, do not execute until asked.
- Daily probe tap affordance (visible panels, stable board, press flash): `.planning/quick/033-daily-probe-tap-affordance/PLAN.md` — planned, do not execute until asked.
- OpenAI transaction-boundary follow-up cleanup (test coverage gap, duplicated test helper, comment trim, dead code): `.planning/quick/037-openai-transaction-boundary-followup/PLAN.md` — planned, do not execute until asked.
- Start the next milestone with `/gsd-new-milestone` when ready.

Parked work: SEED-001, SEED-002, SEED-005, SEED-006, SEED-007, SEED-008; ADR 0002 Level 1. See [ROADMAP.md](ROADMAP.md).

Recent ad-hoc work: `noteProperty` / **property panel** / `#prop:` wiki (ADR 0001 / ADR 0004 / ADR 0005); E2E named-route honesty and SPA hydrate protocol (E2E helpers, `MainMenu.vue`); daily probe side tap ([daily-probe-protocol.md](notes/daily-probe-protocol.md)). Active plan: [040](quick/040-note-reference-index-hardening/PLAN.md) (planned).
