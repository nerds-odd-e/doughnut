---
phase: 03-potential-learning-sessions
plan: 01
subsystem: recall
tags: [commissioned, potential-learning-session, dueCommissioned, recall, e2e]

requires:
  - phase: 02-assimilate-as-commissioned
    provides: assimilateAsCommissioned COMMISSIONED create path + makeMe.commissioned()
  - phase: 01-commissioned-tracker-model
    provides: byUserIdFrom COMMISSIONED exclusion from ordinary due recall
provides:
  - Additive DueMemoryTrackers.dueCommissioned with notebook identity
  - Sibling byUserIdCommissionedFrom due query (no Flyway)
  - FE potentialLearningSessions group-by-notebook + display-only progress-bar strip
  - Graduated E2E: due commissioned await Tutor / ordinary empty
affects:
  - 03-02 multi-notebook potential sessions
  - Phase 4–5 commission dialog / Learning Session create

actuals:
  tokens: 17559
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - Sibling native query fragment for COMMISSIONED due (leave byUserIdFrom intact)
    - FE-derived potential learning session via group-by notebookId
    - setDueCommissioned only on successful recalling / recallStatus payloads

key-files:
  created:
    - backend/src/main/java/com/odde/doughnut/controllers/dto/DueCommissionedMemoryTrackerLite.java
    - frontend/tests/components/recall/RecallProgressBar.spec.ts
  modified:
    - backend/src/main/java/com/odde/doughnut/controllers/dto/DueMemoryTrackers.java
    - backend/src/main/java/com/odde/doughnut/entities/repositories/MemoryTrackerRepository.java
    - backend/src/main/java/com/odde/doughnut/services/RecallService.java
    - backend/src/main/java/com/odde/doughnut/services/UserService.java
    - frontend/src/composables/useRecallData.ts
    - frontend/src/composables/useRecallPageLoading.ts
    - frontend/src/components/recall/RecallProgressBar.vue
    - frontend/src/pages/RecallPage.vue
    - e2e_test/features/learning_session/commissioned_learning_session.feature

key-decisions:
  - "Row copy uses 1 potential learning session per notebook row (session count), not trackerIds.length"
  - "expectCount(0) asserts absent .recall-count badge (v-if hides zero)"

patterns-established:
  - "Ordinary due stays on byUserIdFrom; commissioned due is a sibling fragment + mapping onto dueCommissioned"
  - "Potential learning sessions are FE-only derived; no PLS persistence"

requirements-completed: [TRK-03, POT-01]

coverage:
  - id: D1
    description: Only-due-COMMISSIONED recalling returns empty toRepeat and populated dueCommissioned with notebook identity
    requirement: TRK-03
    verification:
      - kind: unit
        ref: backend/.../RecallsControllerTests#shouldListDueCommissionedTrackersSeparatelyFromOrdinaryRecall
        status: pass
    human_judgment: false
  - id: D2
    description: Recall progress bar shows one display-only potential learning session row with glossary copy for a notebook
    requirement: POT-01
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/RecallProgressBar.spec.ts#renders glossary copy for one notebook session
        status: pass
      - kind: e2e
        ref: e2e_test/features/learning_session/commissioned_learning_session.feature#Due commissioned trackers await a Tutor rather than ordinary recall
        status: pass
    human_judgment: false
  - id: D3
    description: Ordinary recall badge stays empty when only commissioned trackers are due
    requirement: TRK-03
    verification:
      - kind: e2e
        ref: e2e_test/features/learning_session/commissioned_learning_session.feature#0 notes to recall
        status: pass
    human_judgment: false

duration: 7min
completed: 2026-08-08
status: complete
---

# Phase 3 Plan 01: Potential learning sessions tracer Summary

**Due COMMISSIONED trackers feed `dueCommissioned` on recalling; the recall progress bar shows a display-only potential learning session per notebook while ordinary recall stays empty.**

## Performance

- **Duration:** 7 min
- **Started:** 2026-08-07T23:43:29Z
- **Completed:** 2026-08-07T23:50:31Z
- **Tasks:** 2
- **Files modified:** 26

## Accomplishments

- Additive `dueCommissioned` DTO + sibling COMMISSIONED due query without Flyway / PLS table
- FE `potentialLearningSessions` group-by-notebook and UI-SPEC sibling strip (`data-test="potential-learning-session"`)
- First Phase 3 E2E scenario green without `@wip` (ordinary 0 + one Spanish conversation session)

## Task Commits

1. **Task 1: End-to-end one-notebook potential learning session** - `42ca7eb5f0` (feat)
2. **Task 2: Graduate first Phase 3 E2E scenario to green** - `da19019064` (feat)

## Files Created/Modified

- `DueCommissionedMemoryTrackerLite.java` — lite with memoryTrackerId, notebookId, notebookName
- `MemoryTrackerRepository.java` — `byUserIdCommissionedFrom` + due stream (ordinary fragment unchanged)
- `RecallService` / `UserService` — map due commissioned onto DTO
- `useRecallData.ts` — dueCommissioned + potentialLearningSessions computed
- `RecallProgressBar.vue` — display-only sibling strip under ProgressBar
- `commissioned_learning_session.feature` — graduated Tutor-await scenario

## Decisions Made

- Row copy is **one session per notebook row** (`1 potential learning session…`), matching E2E/glossary — not `trackerIds.length`
- `expectCount(0)` treats zero as **absent** `.recall-count` (badge uses `v-if="badge"`)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Zero ordinary recall badge assertion**
- **Found during:** Task 2
- **Issue:** `expectCount(0)` looked for text `0` in `.recall-count`, but NavigationItem hides badge when falsy
- **Fix:** When count is 0, assert `.recall-count` does not exist
- **Files modified:** `e2e_test/start/pageObjects/recallPage.ts`
- **Commit:** `da19019064`

**2. [Rule 1 - Bug] Session count vs tracker count in strip copy**
- **Found during:** Task 2
- **Issue:** PATTERNS sample used `trackerIds.length` (showed "2 …" for two notes); E2E/UI-SPEC expect one session per notebook ("1 …")
- **Fix:** Render fixed `1` per notebook row
- **Files modified:** `RecallProgressBar.vue`, Vitest expectations
- **Commit:** `da19019064`

## Post-change refactor

Reviewed uncommitted Task 2 delta before commit: no cohesion/dead-code issues beyond the copy/assert fixes above. No speculative Phase 4–5 commission wiring.

## REFACTOR COMPLETE

## Threat Flags

None beyond plan threat model (user-scoped query, Vue text interpolation for notebookName, existing assertLoggedIn).

## Self-Check: PASSED

- Created DTO and RecallProgressBar.spec present
- Commits `42ca7eb5f0` and `da19019064` present in git log
- Targeted Cypress feature green; `check_wip_tags.sh` OK (0 WIP)
