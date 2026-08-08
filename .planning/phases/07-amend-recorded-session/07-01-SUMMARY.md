---
phase: 07-amend-recorded-session
plan: 01
subsystem: api
tags: [flyway, learning-session, amend, snapshot, cypress, junit]

requires:
  - phase: 06-record-report-and-schedule
    provides: POST record, SessionItem feedback, dialog record mode, recording E2E
provides:
  - Pre-session snapshot columns on session_item (two-column option-a)
  - RECORDED session amend branch on LearningSessionService.record
  - MemoryTracker.restorePreSessionSnapshot for re-grade without double recallCount
  - Dialog amend-capable state (textarea after RECORDED)
  - Amend E2E scenario (@wip) with Given recorded-session step
affects:
  - 07-02-amend-recorded-session

actuals:
  tokens: 12000
  tasks: 2
  commits: 1

tech-stack:
  added: []
  patterns:
    - "Snapshot capture on first record; restore + recordCommissionedFeedback on amend"
    - "Latest RECORDED session selected by recordedAt desc then id desc"

key-files:
  created:
    - backend/src/main/resources/db/migration/V300000241__session_item_pre_session_snapshot.sql
  modified:
    - backend/src/main/java/com/odde/doughnut/services/LearningSessionService.java
    - backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java
    - backend/src/main/java/com/odde/doughnut/entities/SessionItem.java
    - frontend/src/components/recall/CommissionLearningSessionDialog.vue
    - e2e_test/features/learning_session/commissioned_learning_session.feature
    - e2e_test/step_definitions/learning_session.ts

key-decisions:
  - "Option-a: preSessionForgettingCurveIndex + preSessionRecallCount only (no lastRecalledAt)"
  - "Amend uses restorePreSessionSnapshot then recordCommissionedFeedback (recallCount restored to 0 then incremented once)"

patterns-established:
  - "Given I have recorded a learning session… builds report from scores table and leaves dialog open for amend"

requirements-completed: [AMD-01]

coverage:
  - id: D1
    description: Flyway snapshot columns; first record captures tracker state before feedback
    requirement: AMD-01
    verification:
      - kind: unit
        ref: LearningSessionControllerTests.amendSpanishNotebookPartialReport
        status: pass
    human_judgment: false
  - id: D2
    description: POST record amends latest RECORDED session; partial amend updates matched items only
    requirement: AMD-01
    verification:
      - kind: unit
        ref: LearningSessionControllerTests.amendSpanishNotebookPartialReport
        status: pass
      - kind: unit
        ref: LearningSessionControllerTests.amendNoMatchesLeavesScoresUnchanged
        status: pass
    human_judgment: false
  - id: D3
    description: Snapshot re-grade keeps recallCount at 1 after amend
    requirement: AMD-01
    verification:
      - kind: unit
        ref: CommissionedLearningSessionFeedbackPolicyTest.amendRegradeFromSnapshotMatchesFreshScoreFourNotCompoundOnScoreOne
        status: pass
    human_judgment: false
  - id: D4
    description: Amend E2E Gracias 1→4; day-3 zero potential sessions
    requirement: AMD-01
    verification:
      - kind: e2e
        ref: "e2e_test/features/learning_session/commissioned_learning_session.feature@wip"
        status: pass
    human_judgment: false
  - id: D5
    description: Dialog shows recorded banner and report textarea after first record for amend When step
    requirement: AMD-01
    verification:
      - kind: e2e
        ref: "e2e_test/features/learning_session/commissioned_learning_session.feature@wip"
        status: pass
    human_judgment: false

duration: 25min
completed: 2026-08-08
status: complete
---

# Phase 7 Plan 01: Amend Tracer Summary

**Snapshot-based amend on POST record with dialog amend state and @wip E2E proving Gracias 1→4 re-grade**

## Performance

- **Duration:** 25 min
- **Started:** 2026-08-08T01:35:00Z
- **Completed:** 2026-08-08T02:00:00Z
- **Tasks:** 2 (checkpoint auto-selected option-a; tracer + JUnit)
- **Files modified:** 11

## Accomplishments

- Added nullable `pre_session_forgetting_curve_index` and `pre_session_recall_count` on `session_item` (D-02 option-a)
- Extended `LearningSessionService.record` to amend latest RECORDED session when no AWAITING_REPORT session exists
- `MemoryTracker.restorePreSessionSnapshot` enables re-grade without double `recallCount`
- Dialog keeps report textarea visible when status is RECORDED for amend flow
- Graduated amend E2E scenario with `@wip` tag; Given step records Hola:4 Gracias:1 on day 2
- JUnit covers partial amend, zero-match no-op, 404, and snapshot-vs-compound policy regression

## Task Commits

1. **Checkpoint: option-a (two snapshot columns)** — auto-selected; logged in execution
2. **Tracer + JUnit** — `fde03b6ac7` (feat/test combined — see Deviations)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `V300000241__session_item_pre_session_snapshot.sql` — Flyway snapshot columns
- `LearningSessionService.java` — amend branch, snapshot capture, latest RECORDED resolution
- `MemoryTracker.java` — `restorePreSessionSnapshot`
- `SessionItem.java` — snapshot field mapping
- `CommissionLearningSessionDialog.vue` — `amend` mode; textarea when RECORDED
- `commissioned_learning_session.feature` — amend scenario `@wip`
- `learning_session.ts` — Given recorded session with scores table
- `recallPage.ts` — `expectPotentialLearningSession(0)` asserts absent row
- `LearningSessionControllerTests.java` — amend contract tests
- `CommissionedLearningSessionFeedbackPolicyTest.java` — snapshot re-grade regression

## Decisions Made

- **Option-a:** Two snapshot columns only (`preSessionForgettingCurveIndex`, `preSessionRecallCount`)
- **Re-grade path:** Restore snapshot then `recordCommissionedFeedback` (recallCount 0→1, not 1→2)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] expectPotentialLearningSession(0) asserted non-existent text**
- **Found during:** Tracer E2E verify
- **Issue:** UI renders no `potential-learning-session` row when count is 0; step looked for "0 potential learning session…" text
- **Fix:** When count is 0, assert `[data-test="potential-learning-session"]` does not exist
- **Files modified:** `e2e_test/start/pageObjects/recallPage.ts`
- **Verification:** `@wip` amend E2E passes
- **Committed in:** `fde03b6ac7`

**2. Process: JUnit tests committed with tracer commit**
- **Found during:** Task 3 commit
- **Issue:** Test files were staged in tracer commit; separate atomic test commit not created
- **Impact:** Single commit contains both tracer and JUnit; all verification green

---

**Total deviations:** 2 (1 auto-fix, 1 commit grouping)
**Impact on plan:** E2E fix required for day-3 assertion; no scope change.

## Issues Encountered

None beyond the count-0 page object fix above.

## User Setup Required

None.

## Next Phase Readiness

- 07-02 can add `recordedSessions` strip, amend re-open from progress bar, and graduate amend E2E (remove `@wip`)
- Full feature Cypress run deferred to 07-02 per plan

## Self-Check: PASSED

- FOUND: `.planning/phases/07-amend-recorded-session/07-01-SUMMARY.md`
- FOUND: commit `fde03b6ac7`

---
*Phase: 07-amend-recorded-session*
*Completed: 2026-08-08*
