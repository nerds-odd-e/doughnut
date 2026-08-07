---
phase: 01-commissioned-tracker-model
plan: 02
subsystem: api
tags: [memory-tracker, commissioned, assimilation, batch-question-gen]

requires:
  - phase: 01-01
    provides: byUserIdFrom excludes COMMISSIONED; SC3 recall proof
provides:
  - Ordinary assimilation join ignores COMMISSIONED (commissioned-only notes stay in queue)
  - Property target-note gate ignores COMMISSIONED
  - Batch question-gen candidates exclude COMMISSIONED
  - SC1 backend:verify green
affects:
  - Phase 2 assimilate-as-commissioned Behavior

actuals:
  tokens: ~800
  tasks: 2
  commits: 0

tech-stack:
  added: []
  patterns:
    - JPQL fully-qualified enum filter on joinMemoryTracker / target gate (mirror SPELLING)
    - Native SQL literal type filter beside SPELLING in batch candidates

key-files:
  created: []
  modified:
    - backend/src/main/java/com/odde/doughnut/entities/repositories/NoteRepository.java
    - backend/src/main/java/com/odde/doughnut/entities/repositories/NotePropertyIndexRepository.java
    - backend/src/main/java/com/odde/doughnut/entities/repositories/MemoryTrackerRepository.java
    - backend/src/test/java/com/odde/doughnut/controllers/AssimilationControllerTests.java
    - backend/src/test/java/com/odde/doughnut/services/QuestionGenerationBatchCandidateMemoryTrackersTest.java

key-decisions:
  - "Append COMMISSIONED exclusion on joinMemoryTracker only; leave JPA_WHERE_NOTE_LEVEL_TRACKER aligned with isNoteLevelTracker()"
  - "Target gate tmtBlock gets same COMMISSIONED exclusion (RESEARCH A2)"
  - "No MemoryTrackerAssimilation / assimilate UI / Flyway / OpenAPI"

patterns-established:
  - "Ordinary assimilation detection = note-level join excluding COMMISSIONED so commissioned-only notes remain unassimilated"

requirements-completed: []

coverage:
  - id: SC1
    description: Existing assimilation and recall backend unit suites still pass
    verification:
      - kind: unit
        ref: pnpm backend:verify
        status: pass
    human_judgment: false
  - id: queue-proof
    description: Note with only commissioned tracker still in ordinary assimilation next/queue
    verification:
      - kind: unit
        ref: AssimilationControllerTests$Next#commissionedOnlyNoteStillAppearsInOrdinaryAssimilationQueue
        status: pass
    human_judgment: false
  - id: batch-proof
    description: Due COMMISSIONED trackers absent from batch question-gen candidates
    verification:
      - kind: unit
        ref: QuestionGenerationBatchCandidateMemoryTrackersTest#excludesCommissionedTracker
        status: pass
    human_judgment: false

duration: ~5min
completed: 2026-08-07
status: ready-for-wrap-up
---

# Plan 01-02: Ordinary assimilation + batch ignore COMMISSIONED

**Assimilation queue and batch question-gen candidates never treat COMMISSIONED as ordinary work; SC1 `backend:verify` is green.**

## Accomplishments

- Appended JPQL `AND rp.type <> …MemoryTrackerType.COMMISSIONED` to `NoteRepository.joinMemoryTracker`.
- Appended the same exclusion on `NotePropertyIndexRepository` target-note gate (`tmtBlock`).
- Appended native `AND mt.type <> 'COMMISSIONED'` beside SPELLING in `findBatchQuestionGenerationCandidatesByUser`.
- Added capability-named proofs: `commissionedOnlyNoteStillAppearsInOrdinaryAssimilationQueue`, `excludesCommissionedTracker`.
- Left `MemoryTrackerAssimilation`, assimilate UI/DTO, Flyway, and OpenAPI untouched.

## Tests run (green)

- `pnpm backend:test_only -- --tests com.odde.doughnut.controllers.AssimilationControllerTests`
- `pnpm backend:test_only -- --tests com.odde.doughnut.services.QuestionGenerationBatchCandidateMemoryTrackersTest`
- `pnpm backend:verify` (SC1)

## Deviations

None material. Did not extend `MemoryTracker.JPA_WHERE_*` (keeps note-level predicate aligned with `isNoteLevelTracker()`); exclusion lives on the ordinary joins only.

## Ready for coordinator wrap-up

Uncommitted working tree; do not mark PLAN done / commit / push / post-change-refactor from this implementer.
