---
gsd_state_version: 1.0
milestone: v1.3
milestone_name: Commissioned Learning Session MVP
status: in_progress
stopped_at: Phase 3 verification passed
last_updated: "2026-08-07T23:58:06.163Z"
progress:
  total_phases: 7
  completed_phases: 3
  total_plans: 6
  completed_plans: 6
current_phase: 4
current_phase_name: learning-session-request-builder
last_activity: 2026-08-08
last_activity_desc: Phase 3 goal verified passed (03-VERIFICATION.md, 6/6)
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

**Next:** Plan or discuss Phase 4 (Learning Session / Request builder Structure).

## Deferred Items

| Category | Item | Status |
|----------|------|--------|
| seed | SEED-001-mcq-fuzzy-notebook-title-spelling-match | dormant |
| seed | SEED-002-host-mcp-over-https | dormant |
| tech_debt | OpenAPI/`outgoingLinks`/`linkText` glossary rename (ADR 0001) | open |
| deferred | Refine note on answered spelling questions | deferred |
| plan_later | Amend recomputation (snapshot vs compound) | decide in `/gsd-plan-phase 7` |
| known | `pnpm lint:all` / `test:path-routing` fails pre-existing (`render from routing JSON substitutes SHA` 6==7) | unrelated |

## Session Continuity

**Last session:** 2026-08-07T23:58:06.157Z
**Stopped at:** Phase 3 verification passed
**Resume file:** .planning/phases/03-potential-learning-sessions/03-VERIFICATION.md

Phase 3 verified 6/6. Next: Phase 4.

## Performance Metrics

| Plan | Duration | Tasks | Files |
|------|----------|-------|-------|
| Phase 02 P01 | 4 min | 3 tasks | 19 files |
| Phase 02 P02 | 3 min | 2 tasks | 7 files |
| Phase 03 P01 | 7min | 2 tasks | 26 files |
| Phase 03-potential-learning-sessions P02 | 4min | 2 tasks | 5 files |

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
