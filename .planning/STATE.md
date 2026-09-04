---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: null
current_phase_name: null
status: ready
stopped_at: null
last_updated: "2026-09-04T00:30:33Z"
last_activity: 2026-09-04
last_activity_desc: "reconstructed Proposed ADR 0002 as Git-native Portable notebook synchronization"
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

**Flyway:** every applied migration is squashed into `V100000000__baseline.sql`; `V300000318__purge_failed_question_generation_batch_requests.sql` is the tip. New migrations use a greater version.

Daily probe measurement (not an ADR): [daily-probe-protocol.md](notes/daily-probe-protocol.md). Authoritative authored note references (live resolution, no notebook-wide cache) and question-generation batch latest-only retry + `FAILED` request purge are shipped.

## Operator Next Steps

- Start the next milestone with `/gsd-new-milestone` when ready.

Parked work: SEED-001, SEED-002, SEED-006, SEED-007, SEED-008; Git-native
Portable notebook tree synchronization proposed in ADR 0002. See
[ROADMAP.md](ROADMAP.md).

Recent ad-hoc work: `noteProperty` / **property panel** / `#prop:` wiki (ADR 0001 / ADR 0004 / ADR 0005); E2E named-route honesty and SPA hydrate protocol (E2E helpers, `MainMenu.vue`); daily probe side tap ([daily-probe-protocol.md](notes/daily-probe-protocol.md)); live authored-note-reference resolution; question-generation batch latest-only retry + failed-request purge; recall E2E suite cut (~78% wall time).
