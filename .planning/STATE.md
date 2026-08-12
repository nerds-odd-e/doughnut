---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: 3
current_phase_name: Separate deploy workflow
status: in_progress
stopped_at: null
last_updated: "2026-08-12T07:50:00Z"
last_activity: 2026-08-12
last_activity_desc: Phase 2 done — deploy health gate before success record
progress:
  total_phases: 8
  completed_phases: 2
  total_plans: 1
  completed_plans: 0
  percent: 25
---

# Project State

## Project Reference

See: `.planning/PROJECT.md`

**Core value:** Healthy mainline for learning and knowledge work

## Current Position

[safe hard delete](quick/003-safe-hard-delete/PLAN.md): Phases 1–2 done. Phase 2 adds post-rollout
health probe before `last-successful-deploy.json` (interim in CI Deploy job).

**Next:** Phase 3 — separate deploy workflow so CI duration excludes health wait.

## Operator Next Steps

- Decide Phase 5 policy for `conversation.recall_prompt_id` (SET NULL, recommended, vs CASCADE)
