---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: 2
current_phase_name: Unhealthy release fails deploy
status: in_progress
stopped_at: null
last_updated: "2026-08-12T07:20:00Z"
last_activity: 2026-08-12
last_activity_desc: Phase 1 done — removed failed V300000245 migration; awaiting deploy
progress:
  total_phases: 7
  completed_phases: 1
  total_plans: 1
  completed_plans: 0
  percent: 14
---

# Project State

## Project Reference

See: `.planning/PROJECT.md`

**Core value:** Healthy mainline for learning and knowledge work

## Current Position

Phase 1 of [safe hard delete](quick/003-safe-hard-delete/PLAN.md) is committed: the failed
Flyway migration `V300000245` and its ClassPathResource test are gone. Production should recover
once CI deploys this jar (`repair()` clears the failed history row).

**Next:** Phase 2 — probe `/api/healthcheck` after rolling replace before writing
`last-successful-deploy.json`.

Shipped earlier: [remove re-assimilate](quick/001-remove-reassimilate/) (9 phases) — introduced
the failed migration.

## Operator Next Steps

- After deploy: confirm `https://doughnut.odd-e.com/api/healthcheck` returns `OK`
- Decide Phase 3 policy for `conversation.recall_prompt_id` (SET NULL, recommended, vs CASCADE)
- Human review/update of Proposed ADRs 0001 / 0003 / 0005
