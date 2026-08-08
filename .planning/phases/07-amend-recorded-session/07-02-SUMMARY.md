---
phase: 07-amend-recorded-session
plan: 02
subsystem: ui
tags: [recorded-sessions, recall-progress-bar, amend, cypress, junit, openapi]

requires:
  - phase: 07-amend-recorded-session
    plan: 01
    provides: Snapshot amend API, dialog amend mode, @wip amend E2E tracer
provides:
  - recordedSessions on recalling payload with cross-user isolation
  - Recorded-session strip with Amend report re-open path
  - Session-strip refresh on recall page activation
  - Amend edge JUnit coverage (partial, zero-match, scheduling, tutor feedback)
  - Amend E2E graduated without @wip
affects: []

actuals:
  tokens: 18000
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "RecordedLearningSessionLite mirrors awaiting-report lite on DueMemoryTrackers"
    - "loadSessionStrips on recall onActivated without resetting toRepeat queue"

key-files:
  created:
    - backend/src/main/java/com/odde/doughnut/controllers/dto/RecordedLearningSessionLite.java
  modified:
    - backend/src/main/java/com/odde/doughnut/services/RecallService.java
    - frontend/src/components/recall/RecallProgressBar.vue
    - frontend/src/composables/useRecallData.ts
    - frontend/src/composables/useRecallPageLoading.ts
    - e2e_test/features/learning_session/commissioned_learning_session.feature
    - e2e_test/step_definitions/learning_session.ts
    - e2e_test/start/pageObjects/recallPage.ts

key-decisions:
  - "loadSessionStrips on recall activation refreshes awaiting/recorded strips without queue reset"
  - "E2E When step opens amend strip only when report dialog is not already open"

patterns-established:
  - "Recorded-session strip sibling to awaiting-report with amend-learning-session-report CTA"

requirements-completed: [AMD-01]

coverage:
  - id: D1
    description: recordedSessions on recalling with cross-user isolation
    requirement: AMD-01
    verification:
      - kind: unit
        ref: RecallsControllerTests.returnsRecordedSessionsAfterRecord
        status: pass
      - kind: unit
        ref: RecallsControllerTests.recordedSessionsDoesNotLeakAcrossUsers
        status: pass
    human_judgment: false
  - id: D2
    description: Recorded-session strip re-opens amend dialog with prefilled request
    requirement: AMD-01
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/RecallProgressBar.spec.ts
        status: pass
      - kind: e2e
        ref: e2e_test/features/learning_session/commissioned_learning_session.feature
        status: pass
    human_judgment: false
  - id: D3
    description: Partial and zero-match amend edges with rejectedEntries
    requirement: AMD-01
    verification:
      - kind: unit
        ref: LearningSessionControllerTests.partialAmendUpdatesOnlyMatchedItem
        status: pass
      - kind: unit
        ref: LearningSessionControllerTests.allRejectedAmendLeavesPriorFeedback
        status: pass
    human_judgment: false
  - id: D4
    description: Day-3 empty dueCommissioned after Gracias amend to 4
    requirement: AMD-01
    verification:
      - kind: unit
        ref: RecallsControllerTests.dayThreeDueCommissionedEmptyAfterAmendGraciasToFour
        status: pass
      - kind: e2e
        ref: e2e_test/features/learning_session/commissioned_learning_session.feature
        status: pass
    human_judgment: false
  - id: D5
    description: latestTutorFeedbackScore reflects amended score
    requirement: AMD-01
    verification:
      - kind: unit
        ref: LearningSessionControllerTests.amendSpanishNotebookPartialReport
        status: pass
      - kind: e2e
        ref: e2e_test/features/learning_session/commissioned_learning_session.feature
        status: pass
    human_judgment: false
  - id: D6
    description: Snapshot re-grade guards (high-to-low amend)
    requirement: AMD-01
    verification:
      - kind: unit
        ref: CommissionedLearningSessionFeedbackPolicyTest.highToLowAmendFromSnapshotMatchesFreshScoreOneNotCompoundOnScoreFive
        status: pass
      - kind: unit
        ref: LearningSessionControllerTests.highToLowAmendReschedulesFromSnapshot
        status: pass
    human_judgment: false

duration: 45min
completed: 2026-08-08
status: complete
---

# Phase 7 Plan 02: Amend Expansion Summary

**recordedSessions strip re-opens amend dialog; edge JUnit proofs; amend E2E green without @wip**

## Performance

- **Duration:** 45 min
- **Started:** 2026-08-08T01:40:00Z
- **Completed:** 2026-08-08T02:25:00Z
- **Tasks:** 3
- **Files modified:** 20

## Accomplishments

- Added `recordedSessions` to recalling payload and recorded-session strip with Amend report CTA
- JUnit locks partial amend, zero-match, day-3 scheduling, tutor feedback, and snapshot-vs-compound regression
- Graduated amend E2E scenario (6/6 `commissioned_learning_session.feature` green, no `@wip`)

## Task Commits

1. **Recorded-sessions feed and progress-bar amend strip** — `19e3b482c0` (feat)
2. **Partial amend edges, scheduling proof, tutor feedback** — `e87d25a832` (test)
3. **Graduate amend E2E and full learning_session regression** — `2c632c20ba` (feat)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `RecordedLearningSessionLite.java` — DTO for recorded-session strip payload
- `RecallService.java` — `findByUser_IdAndStatus(RECORDED)` mapping
- `RecallProgressBar.vue` — recorded-session strip + amend dialog
- `useRecallPageLoading.ts` — `loadSessionStrips` on recall activation
- `commissioned_learning_session.feature` — amend scenario without `@wip`
- `learning_session.ts` / `recallPage.ts` — strip re-open E2E steps

## Decisions Made

- **Strip refresh:** `loadSessionStrips` on recall `onActivated` without resetting recall queue
- **E2E When:** Open amend strip only when report textarea not already in open dialog

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Recorded strip missing after page reload**
- **Found during:** Task 3 E2E verify
- **Issue:** `onActivated` only reloaded recalling when recall window expired; awaiting/recorded strips stayed empty after `cy.visit`
- **Fix:** Added `loadSessionStrips()` on every recall activation
- **Files modified:** `frontend/src/composables/useRecallPageLoading.ts`
- **Verification:** Amend E2E passes with `visitRecallPage` in Given
- **Committed in:** `2c632c20ba`

**2. [Rule 3 - Blocking] Test mocks missing recordedSessions**
- **Found during:** Task 1 commit (pre-commit hook)
- **Issue:** `useRecallData` mock helpers lacked `recordedSessions` / `setRecordedSessions`
- **Fix:** Updated recall page and menu test support mocks
- **Files modified:** `recallPageTestSupport.ts`, `mainMenuMocks.ts`, `assimilationPanelTestSupport.ts`
- **Committed in:** `19e3b482c0`

---

**Total deviations:** 2 auto-fixed (1 bug, 1 blocking)
**Impact on plan:** Required for strip re-open E2E path; no scope change.

## Issues Encountered

None beyond strip-refresh and mock fixes above.

## User Setup Required

None.

## Next Phase Readiness

- Phase 7 AMD-01 complete (tracer + expansion)
- Milestone v1.3 commissioned learning session amend flow fully observable

## Self-Check: PASSED

- FOUND: `.planning/phases/07-amend-recorded-session/07-02-SUMMARY.md`
- FOUND: commit `19e3b482c0`
- FOUND: commit `e87d25a832`
- FOUND: commit `2c632c20ba`

---
*Phase: 07-amend-recorded-session*
*Completed: 2026-08-08*
