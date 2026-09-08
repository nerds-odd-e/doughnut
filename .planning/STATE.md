---
gsd_state_version: 1.0
milestone: null
milestone_name: null
current_phase: null
current_phase_name: null
status: ready
stopped_at: null
last_updated: "2026-09-08T09:35:00Z"
last_activity: 2026-09-08
last_activity_desc: "SEED-009 story 10a and SEED-015 story 2b delivered; backlog leads with SEED-015 story 6"
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

**Flyway:** every applied migration is squashed into `V100000000__baseline.sql`; `V300000322__widen_amendment_last_changed_at_precision.sql` is the tip. New migrations use a greater version.

Daily probe measurement (not an ADR): [daily-probe-protocol.md](notes/daily-probe-protocol.md). Authoritative authored note references (live resolution, no notebook-wide cache) and question-generation batch latest-only retry + `FAILED` request purge are shipped.

## Operator Next Steps

- Continue from the ordered queue in [PRODUCT-BACKLOG.md](PRODUCT-BACKLOG.md).
- Start the next milestone with `/gsd-new-milestone` when ready.

Parked undecomposed seeds: SEED-001, SEED-002, SEED-006, SEED-007, SEED-008.
Git-native Portable notebook tree synchronization is proposed in ADR 0002;
selected SEED-009 v1 stories through 18a (including 10/10a) are delivered.
Further SEED-009 candidates stay unselected after the worktree queue.
SEED-015 1a–1c, 2, 2a, 2b, and 3 are delivered. Next queued story is 6
(reclaim databases; in refinement), then 4 and 5. Quick/073 remains an
outstanding proof correction for story 3.
See [ROADMAP.md](ROADMAP.md) and [PRODUCT-BACKLOG.md](PRODUCT-BACKLOG.md).

Recent ad-hoc work: `noteProperty` / **property panel** / `#prop:` wiki (ADR 0001 / ADR 0004 / ADR 0005); E2E named-route honesty and SPA hydrate protocol (E2E helpers, `MainMenu.vue`); daily probe side tap ([daily-probe-protocol.md](notes/daily-probe-protocol.md)); live authored-note-reference resolution; question-generation batch latest-only retry + failed-request purge; recall E2E suite cut (~78% wall time).
