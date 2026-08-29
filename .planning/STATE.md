---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: null
current_phase_name: null
status: ready
stopped_at: null
last_updated: "2026-08-29T10:00:00Z"
last_activity: 2026-08-29
last_activity_desc: "Quick plan 013 note-property canonical path (planned, not started)"
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

- Memory-tracker stability investigation closed: live `applyGrade` persists; `RecallLogMemoryStateBackfill` ran 2026-08-29 (125,570 rows); the 21.9/21.4 gate re-run still fails (`r=0.076`) but on complete data. See `.planning/quick/001-morning-cognitive-index/PLAN.md` slices 21.9/21.4.
- Canonical property path (`noteProperty`, `#prop:` wiki): `.planning/quick/013-note-property-canonical-path/PLAN.md` (planned, not started). Policy already in ADR 0001 / 0004 / Proposed 0005.
- Finish Proposed [ADR 0005](../docs/adrs/0005-web-routes.md) honesty (009 unit leftovers + E2E named-route gate, with navigation timing): `.planning/quick/011-named-spa-route-honesty-follow-up/PLAN.md` (planned, not started)
- Compile path-Markdown HTML `href` (live `noteShowHref`, unresolved `#`): `.planning/quick/012-wiki-anchor-compiled-href/PLAN.md` (complete)
- Split SPA `/users/identify` from production OAuth continue via `.planning/quick/010-sign-in-path-split/PLAN.md` (planned, not started)
- Infer pending vs dead wiki links and show a third style via `.planning/quick/006-wiki-link-pending-style/PLAN.md` (planned, not executed)
- Complete remaining doughnut → donut naming via `.planning/quick/002-complete-donut-naming/PLAN.md`
- Nested Report shape is in `docs/commissioned-learning-session-protocol.md`; implement via `.planning/quick/003-nested-session-item-report/PLAN.md`
- Opt-in daily cognitive probe, extracted from the closed morning-index plan: `.planning/quick/007-daily-cognitive-probe/PLAN.md` (planned, not started)
- Probe convergent-validity and EZ-diffusion analyses, extracted and re-scoped but not yet planned: `.planning/quick/008-probe-convergent-analyses/PLAN.md` (needs a `/slice-planning` pass before execution)
- After deploying the Flyway squash, confirm startup succeeded and `flyway_schema_history` shows the baseline repaired, the collapsed versions marked `DELETE`, and `V300000300` applied
