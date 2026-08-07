---
phase: 01-commissioned-tracker-model
plan: 01
subsystem: api
tags: [memory-tracker, commissioned, due-recall, native-sql]

requires:
  - phase: quick-006-memory-tracker-type
    provides: MemoryTrackerType.COMMISSIONED, type column, UK on type, makeMe .commissioned()
provides:
  - Ordinary due-recall and countByUserNotRemoved exclude COMMISSIONED via byUserIdFrom
  - SC3 controller-boundary proof at RecallsController.recalling
affects:
  - 01-02 (assimilation join + batch filters)
  - Phase 2 assimilate-as-commissioned Behavior

actuals:
  tokens: ~800
  tasks: 2
  commits: 0

tech-stack:
  added: []
  patterns:
    - Native SQL literal type filter on shared byUserIdFrom fragment (mirror SPELLING style)

key-files:
  created: []
  modified:
    - backend/src/main/java/com/odde/doughnut/entities/repositories/MemoryTrackerRepository.java
    - backend/src/test/java/com/odde/doughnut/controllers/RecallsControllerTests.java

key-decisions:
  - "Filter byUserIdFrom only (due + count); leave byUserIdWhere / findByUserAndNote / findLast100 unfiltered"
  - "Literal AND rp.type <> 'COMMISSIONED'; no boolean column; tip stays V300000239"

patterns-established:
  - "Ordinary selection excludes COMMISSIONED at shared native FROM fragment for consistent due list + totalAssimilatedCount"

requirements-completed: []

coverage:
  - id: SC3
    description: Due-recall never returns commissioned trackers when ordinary + commissioned are due; totalAssimilatedCount ordinary-only
    verification:
      - kind: unit
        ref: RecallsControllerTests#shouldExcludeCommissionedMemoryTrackersFromOrdinaryRecallLists
        status: pass
    human_judgment: false
  - id: SC2
    description: Ordinary and commissioned memory trackers coexist on the same note
    verification:
      - kind: unit
        ref: AssimilationControllerTests#understandingAndCommissionedTrackersCanCoexistOnSameNote
        status: pass
    human_judgment: false

duration: ~5min
completed: 2026-08-07
status: ready-for-wrap-up
---

# Plan 01-01: Ordinary due-recall excludes COMMISSIONED

**Due-recall and ordinary assimilated counts never select `type=COMMISSIONED`; coexistence on the same note remains green.**

## Accomplishments

- Appended literal `AND rp.type <> 'COMMISSIONED'` to `MemoryTrackerRepository.byUserIdFrom` (due stream + `countByUserNotRemoved`).
- Added capability-named SC3 test `shouldExcludeCommissionedMemoryTrackersFromOrdinaryRecallLists` at `RecallsController.recalling` (TDD: failed with size 2, then passed).
- Left `byUserIdWhere`, `findByUserAndNote`, `findLast100ByUser` unchanged; no Flyway; tip `V300000239`.

## Tests run (green)

- `pnpm backend:test_only -- --tests com.odde.doughnut.controllers.RecallsControllerTests`
- `pnpm backend:test_only -- --tests com.odde.doughnut.controllers.AssimilationControllerTests`

## Deviations

None. Wave 2 (01-02) still owns assimilation join + batch candidate filters.

## Ready for coordinator wrap-up

Uncommitted working tree; do not mark PLAN done / commit / push / post-change-refactor from this implementer.
