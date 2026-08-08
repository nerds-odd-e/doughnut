---
phase: 06-record-report-and-schedule
plan: 01
subsystem: api
tags: [learning-session, commissioned-feedback, cypress, vitest, junit]

requires:
  - phase: 05-commission-learning-session
    provides: Commission dialog, commission API, AWAITING_REPORT sessions
provides:
  - POST /api/learning-sessions/record with partial-success response
  - LearningSessionReportParser and CommissionedLearningSessionFeedbackPolicy
  - Dialog report textarea + recorded banner
  - @wip E2E recording scenario (Hola:5 Gracias:1)
affects:
  - 06-02-record-report-expansion

actuals:
  tokens: 23448
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Commissioned feedback via recordCommissionedFeedback on MemoryTracker"
    - "Notebook-scoped record POST symmetric with commission"

key-files:
  created:
    - backend/src/main/java/com/odde/doughnut/services/LearningSessionReportParser.java
    - backend/src/main/java/com/odde/doughnut/algorithms/CommissionedLearningSessionFeedbackPolicy.java
    - backend/src/main/java/com/odde/doughnut/controllers/dto/RecordLearningSessionRequest.java
    - backend/src/main/java/com/odde/doughnut/controllers/dto/RecordLearningSessionResponse.java
  modified:
    - backend/src/main/java/com/odde/doughnut/services/LearningSessionService.java
    - backend/src/main/java/com/odde/doughnut/controllers/LearningSessionController.java
    - frontend/src/components/recall/CommissionLearningSessionDialog.vue
    - e2e_test/features/learning_session/commissioned_learning_session.feature

key-decisions:
  - "Record uses same notebook-scoped auth pattern as commission (T-06-01)"
  - "Session moves to RECORDED only when at least one line matches (D-06)"
  - "E2E recording scenario tagged @wip until 06-02 graduates tutor feedback and day-3 steps"

patterns-established:
  - "ADR 0003 commissioned feedback applied via dedicated policy + entity method"
  - "Report paste in plain textarea; apiCallWithLoading + timezoneParam for record"

requirements-completed: [REC-01, REC-02, REC-04]

coverage:
  - id: D1
    description: POST /record persists feedback and reschedules matched trackers
    requirement: REC-01
    verification:
      - kind: e2e
        ref: "e2e_test/features/learning_session/commissioned_learning_session.feature#Recording the tutor's report schedules each tracker from its score"
        status: pass
      - kind: unit
        ref: "backend/src/test/java/com/odde/doughnut/controllers/LearningSessionControllerTests.java#recordsSpanishNotebookSessionWithMatchedScores"
        status: pass
    human_judgment: false
  - id: D2
    description: Score 5 schedules longer interval than score 1 from same state
    requirement: REC-02
    verification:
      - kind: unit
        ref: "backend/src/test/java/com/odde/doughnut/algorithms/CommissionedLearningSessionFeedbackPolicyTest.java#scoreFiveSchedulesLaterThanScoreOneFromSameStartingState"
        status: pass
      - kind: unit
        ref: "backend/src/test/java/com/odde/doughnut/controllers/LearningSessionControllerTests.java#highScoreSchedulesLaterThanLowScoreFromSameStartingState"
        status: pass
    human_judgment: false
  - id: D3
    description: Dialog shows recorded banner after successful record
    requirement: REC-04
    verification:
      - kind: unit
        ref: "frontend/tests/components/recall/CommissionLearningSessionDialog.spec.ts#shows report textarea after commission and records report"
        status: pass
      - kind: e2e
        ref: "e2e_test/features/learning_session/commissioned_learning_session.feature#Recording the tutor's report schedules each tracker from its score"
        status: pass
    human_judgment: false

duration: 25min
completed: 2026-08-08
status: complete
---

# Phase 6 Plan 1: Record Report Tracer Summary

**Notebook-scoped POST /record parses ADR 0005 reports, applies ADR 0003 commissioned scheduling, and wires dialog + @wip E2E for Hola:5 Gracias:1.**

## Performance

- **Duration:** ~25 min
- **Tasks:** 3
- **Files modified:** 23
- **Commits:** 3

## Accomplishments

- Added `LearningSessionReportParser`, `CommissionedLearningSessionFeedbackPolicy`, and `MemoryTracker.recordCommissionedFeedback`
- Exposed `POST /api/learning-sessions/record` with structured `recordedItems` / `rejectedEntries` response
- Extended `CommissionLearningSessionDialog` with report textarea, Record report CTA, and recorded banner
- Graduated recording E2E scenario as `@wip` with step definitions and recall-count assertions via `NoteController.getNoteInfo`

## Task Commits

1. **End-to-end record — Hola:5 Gracias:1 happy path** - `9132bccef3` (feat)
2. **JUnit — record controller happy path and policy schedule divergence** - `2f796069bf` (test)
3. **Vitest — dialog report textarea and recorded banner** - `fc3460d1c1` (test)

## Files Created/Modified

- `backend/.../LearningSessionReportParser.java` - ADR 0005 line parser
- `backend/.../CommissionedLearningSessionFeedbackPolicy.java` - ADR 0003 score-to-index mapping
- `backend/.../LearningSessionService.java` - `record` mutation with partial success
- `frontend/.../CommissionLearningSessionDialog.vue` - report UI + record CTA
- `e2e_test/features/learning_session/commissioned_learning_session.feature` - @wip recording scenario

## Decisions Made

- Learning session controller tests use `1, 2, 4, 8` spacing so score-5 vs score-1 schedule divergence is observable
- Recording scenario includes explicit commissioned assimilation on day 1 before commission Given on day 2

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Controller test spacing made schedule divergence unobservable**
- **Found during:** Task 2 (JUnit schedule divergence)
- **Issue:** Default user spacing produced identical `nextRecallAt` for score 5 vs score 1 at initial index
- **Fix:** Set `withSpaceIntervals("1, 2, 4, 8")` in `LearningSessionControllerTestBase`
- **Files modified:** `LearningSessionControllerTestBase.java`
- **Committed in:** `2f796069bf`

None other — plan executed as written.

## Issues Encountered

None

## Next Phase Readiness

- Ready for 06-02: parser reject matrix, awaiting-report strip, tutor feedback visibility, E2E graduation (remove @wip, day-3 recommission)

## Self-Check: PASSED

- FOUND: `.planning/phases/06-record-report-and-schedule/06-01-SUMMARY.md`
- FOUND: `9132bccef3`
- FOUND: `2f796069bf`
- FOUND: `fc3460d1c1`

---
*Phase: 06-record-report-and-schedule*
*Completed: 2026-08-08*
