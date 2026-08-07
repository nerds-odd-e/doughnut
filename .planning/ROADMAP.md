# Roadmap: Doughnut

## Milestones

- ✅ **v1.0 Notebook Lint & Auto-Fix** — shipped 2026-07-23
- ✅ **v1.1 Spelling Answer Match & Link** — shipped 2026-07-25
- ✅ **v1.2 Accidental Match Resolve UX** — shipped 2026-08-06

Shipped phase detail is not retained here (product + ADRs are the record). Next: `/gsd-new-milestone`.

## Backlog

### Phase 999.1: Commissioned learning session (BACKLOG — MVP scope agreed)

**Goal:** The Learning Orchestrator commissions a Tutor to conduct an appropriate Learning Session, then records the resulting Learning Session. Commissioned memory trackers (per-note, coexisting with ordinary trackers) group by notebook into a Learning Session; Doughnut emits a Learning Session Request in markdown, the Tutor returns a Learning Session Report, and recording it applies Feedback per Session Item — score only for the MVP — rescheduling each tracker and keeping a feedback log. Copy-paste protocol needs its own ADR; score-to-schedule mapping needs research and an ADR 0003 revision.

**Requirements:** TBD — behavioral scope drafted; promote with `/gsd-new-milestone` or `/gsd-review-backlog`.

Context and agreed decisions: `.planning/phases/999.1-commissioned-learning-session/CONTEXT.md`

---
*Last updated: 2026-08-07 — pruned shipped phase diaries*
