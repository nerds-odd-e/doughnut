---
phase: 03-potential-learning-sessions
plan: 02
subsystem: recall
tags: [commissioned, potential-learning-session, multi-notebook, POT-02, e2e]

requires:
  - phase: 03-potential-learning-sessions
    provides: dueCommissioned + FE group-by-notebook + one-notebook strip (03-01)
provides:
  - Graduated multi-notebook potential learning session E2E (POT-02)
  - Long-title break-words backstop on potential-session rows
  - Both Phase 3 D-06 scenarios green without @wip
affects:
  - Phase 4–5 commission dialog / Learning Session create

actuals:
  tokens: 1278
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - cy.contains(rowSelector, exactCopy) for multi-row potential-session assertions
    - break-words on display-only strip so full notebookName stays in DOM

key-files:
  created: []
  modified:
    - frontend/src/components/recall/RecallProgressBar.vue
    - frontend/tests/components/recall/RecallProgressBar.spec.ts
    - e2e_test/features/learning_session/commissioned_learning_session.feature
    - e2e_test/start/pageObjects/recallPage.ts
    - e2e_test/step_definitions/assimilation.ts

key-decisions:
  - "expectPotentialLearningSession uses cy.contains on a specific row (multi-row safe)"
  - "Notebook-scoped assimilate Given reuses title-based assimilateNoteAsCommissioned"
  - "Ordinary recall 0 asserted in multi-notebook scenario (TRK-03 / D-05)"

patterns-established:
  - "Multi-notebook potential sessions: one row per notebookId; page object matches by full copy string"

requirements-completed: [POT-02, TRK-03, POT-01]

coverage:
  - id: D1
    description: Two notebooks with due COMMISSIONED trackers yield two potential-learning-session rows with distinct titles
    requirement: POT-02
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/RecallProgressBar.spec.ts#renders one row per notebook with distinct titles
        status: pass
      - kind: e2e
        ref: e2e_test/features/learning_session/commissioned_learning_session.feature#Notes from different notebooks are commissioned as separate learning sessions
        status: pass
    human_judgment: false
  - id: D2
    description: Multi-notebook case keeps ordinary recall count at 0 when only COMMISSIONED work is due
    requirement: TRK-03
    verification:
      - kind: e2e
        ref: e2e_test/features/learning_session/commissioned_learning_session.feature#0 notes to recall (multi-notebook)
        status: pass
    human_judgment: false
  - id: D3
    description: Long notebook titles wrap with break-words; full title remains in row text for E2E match
    requirement: POT-02
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/RecallProgressBar.spec.ts#keeps the full long notebook title in the row text
        status: pass
    human_judgment: false

duration: 4min
completed: 2026-08-08
status: complete
---

# Phase 3 Plan 02: Multi-notebook potential sessions Summary

**Two notebooks with due COMMISSIONED trackers show two potential learning session rows; long titles wrap without truncation; both Phase 3 E2E scenarios are green without `@wip`.**

## Performance

- **Duration:** 4 min
- **Started:** 2026-08-07T23:51:58Z
- **Completed:** 2026-08-08T00:00:00Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments

- Vitest multi-notebook delta + long-title `break-words` backstop
- Graduated E2E “Notes from different notebooks…” with ordinary recall 0
- Page object multi-row-safe `cy.contains` for potential-session copy

## Task Commits

1. **Task 1: Multi-notebook potential sessions E2E + unit delta** - `6851d1f6a1` (feat)
2. **Task 2: Polish wrap backstop and graduate multi-notebook E2E** - `bdefc59964` (feat)

## Files Created/Modified

- `RecallProgressBar.vue` — `break-words` on potential-session rows
- `RecallProgressBar.spec.ts` — two-notebook + long-title fixtures
- `commissioned_learning_session.feature` — graduated multi-notebook scenario
- `recallPage.ts` — row-specific potential-session assertion
- `assimilation.ts` — notebook-scoped assimilate-as-commissioned Given

## Decisions Made

- Honor 03-01: session copy stays `1` per notebook; `expectCount(0)` = absent badge
- Multi-row page object must match by full expected string, not assert all rows contain one title
- Draft notebook-scoped Given kept as alias over title-based assimilate (titles unique in fixture)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Multi-row potential-session page object**
- **Found during:** Task 1
- **Issue:** `cy.get(...).should('contain', title)` fails when multiple rows exist (assertion applies to all)
- **Fix:** `cy.contains('[data-test="potential-learning-session"]', expected)`
- **Files modified:** `e2e_test/start/pageObjects/recallPage.ts`
- **Commit:** `6851d1f6a1`

## Post-change refactor

Reviewed Task 2 delta: `break-words` + long-title unit + `@wip` removal only. No dead code; gap-2 stack already present; no Phase 4–5 commission wiring.

## REFACTOR COMPLETE

## Threat Flags

None beyond plan threat model (mustache `notebookName`, user-scoped fixtures).

## Self-Check: PASSED

- Commits `6851d1f6a1` and `bdefc59964` present
- Feature file multi-notebook scenario present without `@wip`
- Targeted Vitest + Cypress green; `check_wip_tags.sh` OK (0 WIP)
