---
phase: 02-assimilate-as-commissioned
plan: 01
subsystem: assimilation
tags: [commissioned, assimilate, memory-tracker, daisyui, cypress]

requires:
  - phase: 01-commissioned-tracker-model
    provides: MemoryTrackerType.COMMISSIONED, backend coexistence, Java makeMe.commissioned()
provides:
  - assimilateAsCommissioned create path (COMMISSIONED only)
  - Note-level Assimilate caret menu (Assimilate as commissioned)
  - Commissioned Type label in assimilation settings
  - Graduated learning_session E2E scenario (no @wip)
affects:
  - 02-02 coexistence when ordinary already exists
  - later learning-session phases consuming COMMISSIONED trackers

actuals:
  tokens: 6443
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - Optional Boolean assimilateAsCommissioned on AssimilationRequestDTO
    - DaisyUI daisy-join + AutoCollapseDropdown caret for note-level only
    - Stay-on-note assimilate path (skip goToNextAssimilation + ordinary counts)

key-files:
  created:
    - e2e_test/features/learning_session/commissioned_learning_session.feature
  modified:
    - backend/src/main/java/com/odde/doughnut/controllers/dto/AssimilationRequestDTO.java
    - backend/src/main/java/com/odde/doughnut/services/MemoryTrackerAssimilation.java
    - backend/src/test/java/com/odde/doughnut/controllers/AssimilationControllerTests.java
    - frontend/src/composables/useAssimilateUnit.ts
    - frontend/src/components/recall/AssimilationButtons.vue
    - frontend/src/components/recall/AssimilationSettings.vue
    - frontend/src/components/recall/AssimilationPanel.vue
    - frontend/src/components/notes/NoteInfoMemoryTracker.vue
    - packages/doughnut-test-fixtures/src/MemoryTrackerBuilder.ts
    - packages/generated/doughnut-backend-api/types.gen.ts

key-decisions:
  - "D-01: assimilateAsCommissioned creates only note-level COMMISSIONED"
  - "D-03/D-05: ignore COMMISSIONED for assimilateDisabled; hide caret when COMMISSIONED exists"
  - "D-06: commissioned path skips queue advance and ordinary assimilate counters"
  - "D-07/D-08: Type label Commissioned; one graduated learning_session E2E scenario"

patterns-established:
  - "Commissioned create via existing POST /api/assimilation flag, not a dedicated endpoint"
  - "Note-level showCommissionedOption drives caret; property rows stay caret-free"

requirements-completed: [TRK-01]

coverage:
  - id: D1
    description: User can assimilate as commissioned via note-level caret and see Commissioned type
    requirement: TRK-01
    verification:
      - kind: unit
        ref: backend AssimilationControllerTests#assimilatingAsCommissionedCreatesOnlyCommissionedTracker
        status: pass
      - kind: unit
        ref: frontend/tests/components/recall/AssimilationPanel.spec.ts#posts assimilateAsCommissioned and stays on note without navigating
        status: pass
      - kind: unit
        ref: frontend/tests/components/notes/NoteInfoMemoryTracker.spec.ts#should display commissioned memory tracker type
        status: pass
      - kind: e2e
        ref: e2e_test/features/learning_session/commissioned_learning_session.feature#Assimilating a note with a tutor creates a commissioned memory tracker
        status: pass
    human_judgment: false
  - id: D2
    description: Primary Assimilate stays enabled for commissioned-only; caret hidden when COMMISSIONED exists
    requirement: TRK-01
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/AssimilationPanel.spec.ts#enables assimilate when note has only a commissioned memory tracker
        status: pass
      - kind: unit
        ref: frontend/tests/components/recall/AssimilationPanel.spec.ts#hides commissioned caret when note already has a commissioned tracker
        status: pass
    human_judgment: false

duration: 4min
completed: 2026-08-07
status: complete
---

# Phase 02 Plan 01: Assimilate as commissioned Summary

**Optional `assimilateAsCommissioned` flag creates a note-level COMMISSIONED tracker from a DaisyUI caret menu, stays on the note, and shows Type Commissioned — graduated E2E green without `@wip`.**

## Performance

- **Duration:** 4 min
- **Started:** 2026-08-07T23:11:59Z
- **Completed:** 2026-08-07T23:16:08Z
- **Tasks:** 3/3
- **Files modified:** 19

## Accomplishments

- Backend early-branch creates only COMMISSIONED; refuses propertyKey + commissioned; idempotent when COMMISSIONED exists
- Note-level daisy-join caret posts `assimilateAsCommissioned` and does not advance the assimilation queue
- Assimilation settings Type cell shows **Commissioned**; primary Assimilate remains enabled when only COMMISSIONED exists; caret hidden once COMMISSIONED present
- E2E `learning_session/commissioned_learning_session.feature` passes without `@wip`

## Task Commits

1. **Task 1: End-to-end assimilate as commissioned happy path** - `4a1fa6b14e` (feat)
2. **Task 2: Keep primary Assimilate usable and hide commissioned menu when already commissioned** - `bf3a79501b` (feat)
3. **Task 3: Graduate Phase 2 E2E scenario to green** - `39c4794d26` (test)

## Files Created/Modified

- `AssimilationRequestDTO.java` / `MemoryTrackerAssimilation.java` — commissioned create branch
- `AssimilationButtons.vue` / `AssimilationSettings.vue` / `AssimilationPanel.vue` / `useAssimilateUnit.ts` — caret UX + stay-on-note
- `NoteInfoMemoryTracker.vue` — Commissioned label
- `commissioned_learning_session.feature` + assimilation page objects/steps — E2E

## Decisions Made

Followed locked CONTEXT decisions D-01, D-03–D-08. Discretionary: DaisyUI join + `AutoCollapseDropdown`; data-test ids `assimilate-as-commissioned-caret` / `assimilate-as-commissioned`; extend existing assimilate endpoint rather than a new route.

## Deviations from Plan

### Auto-fixed Issues

None - plan executed as written.

**TDD note:** Tracer shipped as a single vertical-slice feat commit (tests + implementation together) rather than separate RED then GREEN commits, because the slice spanned backend DTO regenerate + UI + E2E scaffolding in one tracer task. Behavior was verified green before commit.

**Total deviations:** 0 auto-fixed. **Impact:** none on delivered behavior.

## Authentication Gates

None.

## Known Stubs

None.

## Threat Flags

None beyond plan threat model (same authenticated assimilate endpoint; mitigations T-02-01–T-02-04 applied).

## Issues Encountered

None.

## Next Phase Readiness

Ready for `02-02` (coexistence when ordinary trackers already exist / TRK-02 remainder). Primary create path and D-03/D-05 hardening are in place.

## Self-Check: PASSED

- `e2e_test/features/learning_session/commissioned_learning_session.feature` FOUND
- Commits `4a1fa6b14e`, `bf3a79501b`, `39c4794d26` FOUND in git log
- AssimilationControllerTests, AssimilationPanel/NoteInfoMemoryTracker Vitest, Cypress feature, `check_wip_tags.sh` all passed during execution
