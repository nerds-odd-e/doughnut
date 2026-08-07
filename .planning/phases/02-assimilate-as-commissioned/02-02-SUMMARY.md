---
phase: 02-assimilate-as-commissioned
plan: 02
subsystem: assimilation
tags: [commissioned, coexistence, assimilate, memory-tracker, cypress]

requires:
  - phase: 02-assimilate-as-commissioned
    provides: assimilateAsCommissioned create path, caret UX, Commissioned label (02-01)
provides:
  - Ordinary-then-commissioned coexistence locked at AssimilationController
  - D-02 caret usable when ordinary trackers exist without COMMISSIONED
  - Graduated E2E asserts ordinary + Commissioned rows together
affects:
  - Phase 2 verification / ship
  - later learning-session phases consuming coexisting trackers

actuals:
  tokens: 1524
  tasks: 2
  commits: 3

tech-stack:
  added: []
  patterns:
    - Commissioned caret ignores assimilateDisabled (only general disabled)
    - E2E coexistence via Given ordinary already assimilated + dual-type Then

key-files:
  created: []
  modified:
    - backend/src/test/java/com/odde/doughnut/controllers/AssimilationControllerAssimilateTests.java
    - frontend/src/components/recall/AssimilationButtons.vue
    - frontend/tests/components/recall/AssimilationPanel.commissioned.spec.ts
    - frontend/tests/components/notes/NoteInfoComponent.spec.ts
    - e2e_test/features/learning_session/commissioned_learning_session.feature
    - e2e_test/start/pageObjects/assimilationPage/assimilationFlow.ts
    - e2e_test/step_definitions/assimilation_memory_tracker.ts

key-decisions:
  - "D-02: caret/menu enabled independently of assimilateDisabled when COMMISSIONED absent"
  - "TRK-02: ordinary-then-commissioned proven at controller + settings/E2E"
  - "Keep single graduated scenario name; enrich Given/Then for coexistence"

patterns-established:
  - "Assimilate primary can be disabled while Assimilate as commissioned remains available"
  - "Coexistence E2E uses testability ordinary assimilate then commissioned caret"

requirements-completed: [TRK-02]

coverage:
  - id: D1
    description: Ordinary-then-commissioned create leaves UNDERSTANDING and adds COMMISSIONED
    requirement: TRK-02
    verification:
      - kind: unit
        ref: backend AssimilationControllerAssimilateTests#assimilatingAsCommissionedWhenUnderstandingExistsCreatesCommissionedAndLeavesUnderstanding
        status: pass
    human_judgment: false
  - id: D2
    description: Assimilate as commissioned caret usable when ordinary trackers exist; settings/E2E show both types
    requirement: TRK-02
    verification:
      - kind: unit
        ref: frontend/tests/components/recall/AssimilationPanel.commissioned.spec.ts#keeps commissioned caret usable when ordinary trackers already exist
        status: pass
      - kind: unit
        ref: frontend/tests/components/notes/NoteInfoComponent.spec.ts#should show ordinary and commissioned memory trackers together
        status: pass
      - kind: e2e
        ref: e2e_test/features/learning_session/commissioned_learning_session.feature#Assimilating a note with a tutor creates a commissioned memory tracker
        status: pass
    human_judgment: false

duration: 3min
completed: 2026-08-08
status: complete
---

# Phase 02 Plan 02: TRK-02 Coexistence Summary

**Ordinary-then-commissioned assimilation leaves UNDERSTANDING in place, keeps the Assimilate-as-commissioned caret usable when only ordinary trackers exist, and shows both Type normal and Commissioned in settings/E2E.**

## Performance

- **Duration:** 3 min
- **Started:** 2026-08-07T23:20:02Z
- **Completed:** 2026-08-07T23:23:19Z
- **Tasks:** 2/2
- **Files modified:** 7

## Accomplishments

- Controller proof: assimilateAsCommissioned with existing UNDERSTANDING returns COMMISSIONED and leaves both types in the repository
- Fixed D-02 regression: commissioned caret/menu no longer inherit `assimilateDisabled` from ordinary trackers
- Graduated E2E Given ordinary already assimilated; Then asserts normal + Commissioned rows

## Task Commits

1. **Task 1: Controller coexistence** - `b4e6ca7b06` (test)
2. **Task 2 RED+GREEN: caret usable with ordinary trackers** - `959995d582` (test + feat mashed; see deviations)
3. **Task 2: Settings/E2E coexistence** - `c7971b4686` (feat)

## Files Created/Modified

- `AssimilationControllerAssimilateTests.java` — ordinary-then-commissioned create
- `AssimilationButtons.vue` — caret/menu ignore assimilateDisabled
- `AssimilationPanel.commissioned.spec.ts` — D-02 caret usable with ordinary
- `NoteInfoComponent.spec.ts` — both type labels in table
- `commissioned_learning_session.feature` + page object/steps — dual-type assertion

## Decisions Made

Honored D-01 create-only COMMISSIONED (no delete of ordinary). D-02 menu available whenever COMMISSIONED absent even if UNDERSTANDING exists. Kept single D-08 scenario name per RESEARCH Open Question 1.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Commissioned caret disabled when ordinary trackers exist**
- **Found during:** Task 2 Vitest (D-02)
- **Issue:** `AssimilationButtons` applied `assimilateDisabled` to caret `pointer-events-none` and menu `:disabled`, blocking assimilate-as-commissioned after ordinary intake
- **Fix:** Caret/menu respect only general `disabled` (loading); primary Assimilate still uses `assimilateDisabled`
- **Files modified:** `AssimilationButtons.vue`, `AssimilationPanel.commissioned.spec.ts`
- **Commit:** `959995d582`

**2. [Rule 3 - Blocking] Create tests live in AssimilationControllerAssimilateTests**
- **Found during:** Task 1
- **Issue:** Plan named `AssimilationControllerTests`; wave-1 refactor split create coverage into `AssimilationControllerAssimilateTests`
- **Fix:** Extended the assimilate test class (capability boundary unchanged)
- **Files modified:** `AssimilationControllerAssimilateTests.java`
- **Commit:** `b4e6ca7b06`

**TDD note:** Task 1 production path already green from 02-01 — tests locked the observable (test-only commit). Task 2 RED+GREEN landed in one commit when the caret fix raced into the failing-test commit staging.

**Total deviations:** 2 auto-fixed. **Impact:** D-02 now correct; no scope change.

## Authentication Gates

None.

## Known Stubs

None.

## Threat Flags

None beyond plan threat model (T-02-06 create does not delete ordinary; T-02-03 idempotent empty list still covered by prior test).

## Issues Encountered

None.

## Next Phase Readiness

Phase 2 both plans complete for TRK-01/TRK-02 create+coexistence. Ready for phase verification / ship gate.

## Self-Check: PASSED

- `AssimilationControllerAssimilateTests.java` FOUND
- Commits `b4e6ca7b06`, `959995d582`, `c7971b4686` FOUND in git log
- Backend assimilate tests, Vitest commissioned/NoteInfo specs, Cypress learning_session feature all passed during execution
