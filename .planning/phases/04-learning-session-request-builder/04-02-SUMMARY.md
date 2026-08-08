---
phase: 04-learning-session-request-builder
plan: 02
subsystem: api
tags: [spring-boot, jpa, learning-session, adr-0005, makeMe]

requires:
  - phase: 04-learning-session-request-builder
    provides: commission API tracer, learning_session/session_item persistence, markdown builder
provides:
  - abandon-on-recommission lifecycle (hard-delete AWAITING_REPORT sessions)
  - learning status aggregation in Request markdown from recorded session history
  - makeMe.aLearningSession() and makeMe.aSessionItem() builders
  - refreshed database ERD with learning_session and session_item tables
affects:
  - phase-5-commission-ui
  - phase-6-record-amend

actuals:
  tokens: 42000
  tasks: 3
  commits: 0

tech-stack:
  added: []
  patterns:
    - "Abandon unfinished sessions before commission (delete items then sessions)"
    - "JPQL aggregates for prior recorded feedback per memory tracker"
    - "LearningSessionBuilder/SessionItemBuilder parent-child fixture pattern"

key-files:
  created:
    - backend/src/test/java/com/odde/doughnut/testability/builders/LearningSessionBuilder.java
    - backend/src/test/java/com/odde/doughnut/testability/builders/SessionItemBuilder.java
  modified:
    - backend/src/main/java/com/odde/doughnut/services/LearningSessionService.java
    - backend/src/main/java/com/odde/doughnut/entities/repositories/LearningSessionRepository.java
    - backend/src/main/java/com/odde/doughnut/entities/repositories/SessionItemRepository.java
    - backend/src/main/java/com/odde/doughnut/services/LearningSessionRequestMarkdownBuilder.java
    - backend/src/test/java/com/odde/doughnut/controllers/LearningSessionControllerTests.java
    - backend/src/test/java/com/odde/doughnut/testability/MakeMe.java
    - docs/database-erd.md

key-decisions:
  - "Abandon only after due-tracker validation passes (empty due commission does not delete awaiting sessions)"
  - "Explicit deleteByLearningSession_Id before session delete (no JPA cascade on LearningSession)"
  - "Learning status pluralizes sessions when count > 1 per RESEARCH A1"

patterns-established:
  - "countRecordedByMemoryTrackerId / findLastRecordedFeedbackAtByMemoryTrackerId on RECORDED sessions only"
  - "ISO_LOCAL_DATE in user zone for learning status last-on date"

requirements-completed: [COM-01, COM-02, COM-03]

coverage:
  - id: D1
    description: Re-commission hard-deletes prior AWAITING_REPORT session and items
    requirement: COM-01
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/controllers/LearningSessionControllerTests.java#recommissionSameNotebookAbandonsPriorAwaitingReportSession
        status: pass
    human_judgment: false
  - id: D2
    description: RECORDED sessions survive re-commission for same notebook
    requirement: COM-01
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/controllers/LearningSessionControllerTests.java#recommissionPreservesRecordedSessionsForSameNotebook
        status: pass
    human_judgment: false
  - id: D3
    description: Request markdown learning status reflects prior recorded feedback per tracker
    requirement: COM-02
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/controllers/LearningSessionControllerTests.java#requestMarkdownReflectsPriorRecordedFeedbackPerTracker
        status: pass
    human_judgment: false
  - id: D4
    description: Structure regression — backend verify and existing Cypress spec green
    requirement: COM-03
    verification:
      - kind: integration
        ref: pnpm backend:verify
        status: pass
      - kind: e2e
        ref: e2e_test/features/learning_session/commissioned_learning_session.feature
        status: pass
    human_judgment: false

duration: 18min
completed: 2026-08-08
status: complete
---

# Phase 4 Plan 02: Abandon Lifecycle + Learning Status Summary

**Re-commission abandons awaiting-report sessions; Request markdown shows tutoring history from recorded feedback; MakeMe builders seed sessions for Phase 6.**

## Performance

- **Duration:** 18 min
- **Tasks:** 3/3
- **Commits:** 0 (coordinator wrap-up)

## Accomplishments

- `LearningSessionService.abandonUnfinishedSessions` deletes prior `AWAITING_REPORT` sessions (and items) after due-tracker validation, before creating a new session
- `LearningSessionRequestMarkdownBuilder` emits `not yet tutored` or `N previous session(s), last on YYYY-MM-DD` per ADR 0005
- `makeMe.aLearningSession()` / `makeMe.aSessionItem()` registered with required-field guards
- `docs/database-erd.md` refreshed with `learning_session` and `session_item` tables
- `pnpm backend:verify` and `commissioned_learning_session.feature` Cypress (3 scenarios) green

## Deviations from Plan

None — plan executed as written.

## Self-Check: PASSED

- FOUND: backend/src/test/java/com/odde/doughnut/testability/builders/LearningSessionBuilder.java
- FOUND: backend/src/test/java/com/odde/doughnut/testability/builders/SessionItemBuilder.java
- FOUND: docs/database-erd.md (learning_session, session_item)
