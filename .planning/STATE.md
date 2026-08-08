---
gsd_state_version: 1.0
milestone: v1.3
milestone_name: Commissioned Learning Session MVP
status: in_progress
stopped_at: Completed 07-01-PLAN.md
last_updated: "2026-08-08T01:40:08.510Z"
progress:
  total_phases: 7
  completed_phases: 5
  total_plans: 14
  completed_plans: 11
current_phase: 7
current_phase_name: Amend recorded session (Behavior)
last_activity: 2026-08-08
last_activity_desc: Phase 7 Plan 07-01 executed — snapshot amend tracer, @wip E2E green, JUnit amend contract
---

# Project State

## Project Reference

See: `.planning/PROJECT.md`

## Current Position

Quick plan **006 memory-tracker type** is **done** (see slim PLAN under
`.planning/quick/006-memory-tracker-type/`).

**Phase 1** — both plans executed; goal **verified passed**
(`.planning/phases/01-commissioned-tracker-model/01-VERIFICATION.md`, score
6/6). Due-recall / assimilation join / batch candidates exclude COMMISSIONED;
coexistence green; `pnpm backend:verify` green; no user-visible create path.

**Phase 2** — both plans executed; goal **verified passed**
(`.planning/phases/02-assimilate-as-commissioned/02-VERIFICATION.md`, score
6/6). Caret → COMMISSIONED create, Commissioned label, stay-on-note, primary
Assimilate with commissioned-only, ordinary+commissioned coexistence (TRK-01/02).

**Phase 3** — both plans executed; goal **verified passed**
(`.planning/phases/03-potential-learning-sessions/03-VERIFICATION.md`, score
6/6). Due COMMISSIONED → `dueCommissioned` + FE notebook-grouped potential
sessions on recall progress bar; ordinary recall stays 0 (TRK-03, POT-01/02).

**Phase 4 Plan 04-02** — executed; abandon-on-recommission + learning status aggregation green
(`.planning/phases/04-learning-session-request-builder/04-02-SUMMARY.md`).
Re-commission deletes prior `AWAITING_REPORT` sessions; Request markdown reflects recorded
history; MakeMe builders; `pnpm backend:verify` + Cypress regression green.

**Phase 5** — both plans executed (COM-01–03). Progress bar Commission button →
`CommissionLearningSessionDialog` → copyable ADR 0005 Request + awaiting-report banner;
`dueCommissioned` excludes trackers in `AWAITING_REPORT` sessions; full
`commissioned_learning_session.feature` green (4 scenarios).

**Phase 6 Plan 06-02** — executed
(`.planning/phases/06-record-report-and-schedule/06-02-SUMMARY.md`). `awaitingReportSessions`
strip re-opens record dialog; `latestTutorFeedbackScore` on assimilation settings; REC-05
parser matrix + rejection UX; recording E2E green without `@wip` including day-3 Gracias-only
recommission (REC-01, REC-03–REC-05).

**Phase 7 Plan 07-01** — executed
(`.planning/phases/07-amend-recorded-session/07-01-SUMMARY.md`). Snapshot columns on
`session_item`; POST record amends latest RECORDED session; dialog amend state; amend E2E
`@wip` green (Gracias 1→4, day-3 zero potential sessions); JUnit snapshot re-grade contract.

**Next:** Phase 7 Plan 07-02 — recorded-session strip, E2E graduation, edge unit tests.

## Deferred Items

| Category | Item | Status |
|----------|------|--------|
| seed | SEED-001-mcq-fuzzy-notebook-title-spelling-match | dormant |
| seed | SEED-002-host-mcp-over-https | dormant |
| tech_debt | OpenAPI/`outgoingLinks`/`linkText` glossary rename (ADR 0001) | open |
| deferred | Refine note on answered spelling questions | deferred |
| plan_later | Amend recomputation (snapshot vs compound) | resolved in Phase 7 CONTEXT (option-a) |
| known | `pnpm lint:all` / `test:path-routing` fails pre-existing (`render from routing JSON substitutes SHA` 6==7) | unrelated |

## Session Continuity

**Last session:** 2026-08-08T01:40:08.500Z
**Stopped at:** Completed 07-01-PLAN.md
**Resume file:** 07-02-PLAN.md

Phase 7 Plan 07-01 complete. Next: 07-02 expansion.

## Performance Metrics

| Plan | Duration | Tasks | Files |
|------|----------|-------|-------|
| Phase 02 P01 | 4 min | 3 tasks | 19 files |
| Phase 02 P02 | 3 min | 2 tasks | 7 files |
| Phase 03 P01 | 7min | 2 tasks | 26 files |
| Phase 03-potential-learning-sessions P02 | 4min | 2 tasks | 5 files |
| Phase 04-learning-session-request-builder P01 | 12 | 3 tasks | 14 files |
| Phase 04-learning-session-request-builder P02 | 18 | 3 tasks | 9 files |
| Phase 06 P01 | 25 | 3 tasks | 23 files |
| Phase 06-record-report-and-schedule P02 | 20 | 3 tasks | 32 files |
| Phase 07-amend-recorded-session P01 | 25 | 2 tasks | 11 files |

## Decisions

- [Phase 02]: assimilateAsCommissioned creates only note-level COMMISSIONED (D-01)
- [Phase 02]: Ignore COMMISSIONED for assimilateDisabled; hide caret when COMMISSIONED exists (D-03/D-05)
- [Phase 02]: Commissioned assimilate stays on note; no ordinary count increment (D-06)
- [Phase 02]: D-02: commissioned caret ignores assimilateDisabled when COMMISSIONED absent
- [Phase 02]: TRK-02: ordinary-then-commissioned coexistence locked at controller + E2E
- [Phase 03]: D-01 dueCommissioned on recalling path; D-02 FE group-by-notebook; no PLS table
- [Phase 03]: D-03/D-04 display-only progress-bar strip; D-05 ordinary-only counts; D-06 two E2E scenarios
- [Phase 03]: Row copy is 1 potential learning session per notebook (not tracker count)
- [Phase 03]: expectCount(0) asserts absent recall-count badge
- [Phase 03]: Multi-row potential-session page object matches by full copy via cy.contains
- [Phase 03]: Long notebook titles use break-words; full title stays in DOM for E2E
- [Phase 04-01]: learning status `not yet tutored` hard-coded in tracer; aggregation in 04-02
- [Phase 04-02]: abandon after due-tracker validation; explicit item delete before session delete
- [Phase 04-02]: learning status pluralizes sessions when N > 1
- [Phase 06-01]: Record uses notebook-scoped auth symmetric with commission
- [Phase 06-01]: Session RECORDED only when at least one report line matches
- [Phase 06-01]: E2E recording scenario @wip until 06-02 graduates tutor feedback steps
- [Phase 06-02]: E2E record scoped to dialog; visitRecallPage after time travel for fresh due data
- [Phase 06-02]: latestTutorFeedbackScore transient JSON field from latest recorded SessionItem
- [Phase 07-01]: Option-a: two snapshot columns on session_item (preSessionForgettingCurveIndex + preSessionRecallCount)
- [Phase 07-01]: Amend: restorePreSessionSnapshot then recordCommissionedFeedback for single recallCount
