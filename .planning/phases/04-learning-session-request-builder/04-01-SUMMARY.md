---
phase: 04-learning-session-request-builder
plan: 01
subsystem: api
tags: [spring-boot, flyway, jpa, learning-session, adr-0005]

requires:
  - phase: 03-potential-learning-sessions
    provides: dueCommissioned recall path and commissioned tracker fixtures
provides:
  - POST /api/learning-sessions/commission endpoint
  - learning_session and session_item persistence
  - ADR 0005 Request markdown builder
  - Controller-boundary tests for commission happy path and guardrails
affects:
  - 04-02-learning-session-lifecycle
  - phase-5-commission-ui

actuals:
  tokens: 28000
  tasks: 3
  commits: 0

tech-stack:
  added: []
  patterns:
    - "Parent/child batch pattern (QuestionGenerationBatch → LearningSession/SessionItem)"
    - "StringBuilder markdown builder (FocusContextMarkdownRenderer analog)"
    - "Controller commission with auth + notebook authorization"

key-files:
  created:
    - backend/src/main/resources/db/migration/V300000240__learning_session_and_session_item.sql
    - backend/src/main/java/com/odde/doughnut/entities/LearningSession.java
    - backend/src/main/java/com/odde/doughnut/entities/LearningSessionStatus.java
    - backend/src/main/java/com/odde/doughnut/entities/SessionItem.java
    - backend/src/main/java/com/odde/doughnut/entities/repositories/LearningSessionRepository.java
    - backend/src/main/java/com/odde/doughnut/entities/repositories/SessionItemRepository.java
    - backend/src/main/java/com/odde/doughnut/services/LearningSessionService.java
    - backend/src/main/java/com/odde/doughnut/services/LearningSessionRequestMarkdownBuilder.java
    - backend/src/main/java/com/odde/doughnut/controllers/LearningSessionController.java
    - backend/src/main/java/com/odde/doughnut/controllers/dto/CommissionLearningSessionRequest.java
    - backend/src/main/java/com/odde/doughnut/controllers/dto/LearningSessionCommissionResponse.java
    - backend/src/test/java/com/odde/doughnut/controllers/LearningSessionControllerTests.java
  modified:
    - open_api_docs.yaml
    - packages/generated/doughnut-backend-api/**

key-decisions:
  - "Learning status hard-coded to not yet tutored in tracer (aggregation deferred to 04-02)"
  - "Abandon-on-recommission deferred to 04-02 per plan"
  - "OpenAPI regen run early to satisfy backend:verify (plan noted deferral to 04-02)"

patterns-established:
  - "Commission filters UserService.getCommissionedMemoryTrackersNeedToRepeat by notebook + isNoteLevelTracker"
  - "ADR 0005 rubric copied verbatim in LearningSessionRequestMarkdownBuilder"

requirements-unlocked: [COM-01, COM-02, COM-03]

coverage:
  - id: D1
    description: POST commission creates LearningSession + SessionItems for due commissioned trackers
    requirement: COM-01
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/controllers/LearningSessionControllerTests.java#commissionsSpanishNotebookWithDueCommissionedTrackers
        status: pass
    human_judgment: false
  - id: D2
    description: Request markdown matches ADR 0005 verbatim rubric and Spanish fixture
    requirement: COM-02
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/controllers/LearningSessionControllerTests.java#commissionsSpanishNotebookWithDueCommissionedTrackers
        status: pass
    human_judgment: false
  - id: D3
    description: Session status AWAITING_REPORT persisted and retrievable
    requirement: COM-03
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/controllers/LearningSessionControllerTests.java#commissionsSpanishNotebookWithDueCommissionedTrackers
        status: pass
    human_judgment: false
  - id: D4
    description: Auth, notebook access, and empty-due guardrails
    requirement: COM-01
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/controllers/LearningSessionControllerTests.java
        status: pass
    human_judgment: false

duration: 12min
completed: 2026-08-08
status: complete
---

# Phase 4 Plan 01: Learning Session Commission Tracer Summary

**POST commission API with learning_session/session_item persistence and ADR 0005 Request markdown for the Spanish conversation fixture**

## Performance

- **Duration:** 12 min
- **Started:** 2026-08-08T00:07:00Z
- **Completed:** 2026-08-08T00:19:00Z
- **Tasks:** 3
- **Files modified:** 14 product + generated OpenAPI

## Accomplishments

- Flyway migration `V300000240` adds `learning_session` and `session_item` tables with parent/child FK cascade
- `LearningSessionController` exposes `POST /api/learning-sessions/commission` with auth, notebook authorization, and timezone
- `LearningSessionService.commission` selects due commissioned note-level trackers for one notebook, persists session + items, returns ADR 0005 markdown
- Five controller tests: Spanish notebook happy path (rubric + items), not logged in, notebook not found, unauthorized, empty due

## Task Commits

Uncommitted per coordinator instruction — coordinator commits after post-change-refactor.

1. **Task 1: End-to-end commission — Spanish notebook happy path** — (pending coordinator)
2. **Task 2: ADR 0005 markdown fidelity assertions** — (pending coordinator, merged into canonical test)
3. **Task 3: Commission guardrails** — (pending coordinator)

## Files Created/Modified

- `backend/src/main/resources/db/migration/V300000240__learning_session_and_session_item.sql` — schema
- `backend/src/main/java/com/odde/doughnut/entities/LearningSession*.java` — domain model
- `backend/src/main/java/com/odde/doughnut/services/LearningSession*.java` — commission + markdown
- `backend/src/main/java/com/odde/doughnut/controllers/LearningSessionController.java` — REST endpoint
- `backend/src/test/java/com/odde/doughnut/controllers/LearningSessionControllerTests.java` — boundary tests
- `open_api_docs.yaml` + `packages/generated/doughnut-backend-api/**` — regenerated for verify gate

## Decisions Made

- Learning status line hard-coded `not yet tutored` for tracer; aggregation in 04-02
- OpenAPI regenerated during execution so `backend:verify` passes (plan had noted deferral to 04-02)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added `throws UnexpectedNoAccessRightException` on controller commission**
- **Found during:** Task 1 compile
- **Issue:** `assertAuthorization(notebook)` is a checked exception
- **Fix:** Declared on controller method; happy-path test declares throws
- **Files modified:** `LearningSessionController.java`, `LearningSessionControllerTests.java`

**2. [Rule 3 - Blocking] Regenerated OpenAPI docs**
- **Found during:** Plan verification (`RobotsTests.openApiDocsMatchCommittedYaml` failed)
- **Fix:** Ran `pnpm generateTypeScript`
- **Files modified:** `open_api_docs.yaml`, `packages/generated/doughnut-backend-api/**`

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** Required for compile and CI verify; no scope creep beyond new endpoint surface.

## Issues Encountered

None beyond compile/OpenAPI sync fixes above.

## User Setup Required

None.

## Next Phase Readiness

- Commission API ready for 04-02 lifecycle (abandon-on-recommission, learning status aggregation)
- OpenAPI client already includes commission endpoint for Phase 5 UI

## Self-Check: PASSED

- FOUND: backend/src/test/java/com/odde/doughnut/controllers/LearningSessionControllerTests.java
- FOUND: backend/src/main/java/com/odde/doughnut/controllers/LearningSessionController.java
- Tests: `LearningSessionControllerTests` 5/5 pass; `pnpm backend:verify` green

---
*Phase: 04-learning-session-request-builder*
*Completed: 2026-08-08*
