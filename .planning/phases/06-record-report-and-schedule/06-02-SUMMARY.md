---
phase: 06-record-report-and-schedule
plan: 02
subsystem: api
tags: [learning-session, awaiting-report, tutor-feedback, parser, cypress, vitest, junit]

requires:
  - phase: 06-record-report-and-schedule
    plan: 01
    provides: POST /record, dialog report UI, @wip recording tracer
provides:
  - awaitingReportSessions on recalling payload + progress-bar strip re-open path
  - latestTutorFeedbackScore on commissioned trackers in assimilation settings
  - LearningSessionReportParser REC-05 rejection matrix + dialog rejection UX
  - Recording E2E graduated without @wip including day-3 Gracias-only recommission
affects:
  - 07-amend-recorded-session

actuals:
  tokens: 42000
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "awaitingReportSessions sibling strip mirrors potential-session pattern"
    - "Record-mode dialog opens with initialRequestMarkdown from recalling payload"
    - "latestTutorFeedbackScore transient on MemoryTracker via SessionItemRepository"

key-files:
  created:
    - backend/src/main/java/com/odde/doughnut/controllers/dto/AwaitingReportLearningSessionLite.java
    - backend/src/test/java/com/odde/doughnut/services/LearningSessionReportParserTest.java
  modified:
    - backend/src/main/java/com/odde/doughnut/services/RecallService.java
    - frontend/src/components/recall/RecallProgressBar.vue
    - frontend/src/components/notes/NoteInfoMemoryTracker.vue
    - e2e_test/features/learning_session/commissioned_learning_session.feature

key-decisions:
  - "Record dialog clicks scoped in E2E to avoid homonym strip button data-test"
  - "visitRecallPage after time travel forces fresh dueCommissioned load on cached RecallPage"
  - "latestTutorFeedbackScore is @Transient JSON-only, not a DB column"

patterns-established:
  - "Re-open awaiting session via awaiting-report strip + record-mode CommissionLearningSessionDialog"
  - "Parser session context rejects unknown titles, ambiguous duplicates, and malformed scores"

requirements-completed: [REC-01, REC-03, REC-04, REC-05]

coverage:
  - id: D1
    description: Awaiting-report strip re-opens record dialog with request prefill
    requirement: REC-01
    verification:
      - kind: unit
        ref: "backend/src/test/java/com/odde/doughnut/controllers/RecallsControllerTests.java#returnsAwaitingReportSessionsAfterCommission"
        status: pass
      - kind: unit
        ref: "frontend/tests/components/recall/RecallProgressBar.spec.ts#opens record dialog with request prefilled"
        status: pass
    human_judgment: false
  - id: D2
    description: Tutor feedback score visible on commissioned tracker in assimilation settings
    requirement: REC-03
    verification:
      - kind: unit
        ref: "frontend/tests/components/notes/NoteInfoMemoryTracker.spec.ts#shows tutor feedback score for commissioned tracker"
        status: pass
      - kind: e2e
        ref: "e2e_test/features/learning_session/commissioned_learning_session.feature#Recording the tutor's report schedules each tracker from its score"
        status: pass
    human_judgment: false
  - id: D3
    description: Recorded session clears awaiting strip after refresh
    requirement: REC-04
    verification:
      - kind: e2e
        ref: "e2e_test/features/learning_session/commissioned_learning_session.feature#Recording the tutor's report schedules each tracker from its score"
        status: pass
    human_judgment: false
  - id: D4
    description: Parser rejection matrix and partial-reject UX
    requirement: REC-05
    verification:
      - kind: unit
        ref: "backend/src/test/java/com/odde/doughnut/services/LearningSessionReportParserTest.java"
        status: pass
      - kind: unit
        ref: "backend/src/test/java/com/odde/doughnut/controllers/LearningSessionControllerTests.java#allLinesRejectedStaysAwaitingReport"
        status: pass
    human_judgment: false
  - id: D5
    description: Day-3 recommission lists only Gracias after Hola score 5 vs Gracias score 1
    requirement: REC-02
    verification:
      - kind: e2e
        ref: "e2e_test/features/learning_session/commissioned_learning_session.feature#Recording the tutor's report schedules each tracker from its score"
        status: pass
      - kind: unit
        ref: "backend/src/test/java/com/odde/doughnut/controllers/RecallsControllerTests.java#dayThreeDueCommissionedOnlyGraciasAfterRecordedScores"
        status: pass
    human_judgment: false

duration: 20min
completed: 2026-08-08
status: complete
---

# Phase 6 Plan 2: Record Report Expansion Summary

**Awaiting-report strip re-opens record flow; tutor feedback on assimilation settings; REC-05 parser matrix; recording E2E green without @wip including day-3 Gracias-only recommission.**

## Performance

- **Duration:** ~20 min
- **Tasks:** 3
- **Files modified:** 32
- **Commits:** 3

## Accomplishments

- Added `awaitingReportSessions` to recalling payload and awaiting-report strip with record-mode dialog prefill
- Extended report parser with title/score rejection matrix; surfaced rejections in dialog warning alert
- Exposed `latestTutorFeedbackScore` on commissioned trackers in assimilation settings
- Graduated recording E2E scenario (tutor feedback + day-3 Gracias-only recommission)

## Task Commits

1. **Awaiting-report sessions feed and progress-bar strip** - `7af8a14a25` (feat)
2. **REC-05 parser matrix, rejection UX, tutor feedback on assimilation settings** - `4151d1f962` (feat)
3. **Graduate recording E2E and full learning_session regression** - `4fe17710d9` (feat)

## Files Created/Modified

- `backend/.../AwaitingReportLearningSessionLite.java` - recalling DTO for awaiting sessions
- `backend/.../LearningSessionReportParser.java` - session-aware parse with duplicate-title reject
- `backend/.../LearningSessionReportParserTest.java` - REC-05 matrix
- `frontend/.../RecallProgressBar.vue` - awaiting-report strip + record dialog
- `frontend/.../NoteInfoMemoryTracker.vue` - tutor feedback score row
- `e2e_test/.../commissioned_learning_session.feature` - recording scenario without @wip

## Decisions Made

- E2E record actions scoped to commission dialog to disambiguate strip vs dialog `record-learning-session-report` buttons
- `visitRecallPage` after day-3 time travel avoids stale `dueCommissioned` on cached RecallPage
- `latestTutorFeedbackScore` populated in `NoteController.getNoteInfo` via latest recorded SessionItem query

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Hibernate mapped latestTutorFeedbackScore as DB column**
- **Found during:** Task 2 (backend tests)
- **Issue:** `@JsonProperty` field without `@Transient` caused SQL unknown column errors
- **Fix:** Added `@Transient` on `MemoryTracker.latestTutorFeedbackScore`
- **Files modified:** `MemoryTracker.java`
- **Committed in:** `4151d1f962`

**2. [Rule 3 - Blocking] Frontend test mocks missing awaitingReportSessions**
- **Found during:** Task 1 commit pre-hook
- **Issue:** `useRecallData` mock types missing new fields broke vue-tsc
- **Fix:** Extended recall page and menu test support mocks
- **Files modified:** `recallPageTestSupport.ts`, `mainMenuMocks.ts`, `assimilationPanelTestSupport.ts`
- **Committed in:** `7af8a14a25`

**3. [Rule 1 - Bug] E2E duplicate record button and stale recall cache**
- **Found during:** Task 3 (Cypress)
- **Issue:** Strip + dialog shared `data-test`; day-3 commission saw zero due sessions on cached page
- **Fix:** Dialog-scoped record clicks; `visitRecallPage` for post-time-travel commission
- **Files modified:** `recallPage.ts`, `learning_session.ts`
- **Committed in:** `4fe17710d9`

## Issues Encountered

None beyond deviations above.

## Next Phase Readiness

- Phase 6 record-report-and-schedule behavior complete; ready for Phase 7 amend-recorded-session
- ROADMAP SC2/SC3/SC4 satisfied via unit + E2E verification

## Self-Check: PASSED

- FOUND: `.planning/phases/06-record-report-and-schedule/06-02-SUMMARY.md`
- FOUND: `7af8a14a25`
- FOUND: `4151d1f962`
- FOUND: `4fe17710d9`

---
*Phase: 06-record-report-and-schedule*
*Completed: 2026-08-08*
