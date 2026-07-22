---
phase: 02-empty-folder-findings
plan: 01
subsystem: health
tags: [health-rule, empty-folders, spring-service, jpa, tdd]

requires:
  - phase: 01-health-lint-contract
    provides: HealthRule, HealthRuleRunner, NotebookHealthService.lint, findings DTOs, HealthRuleIds.EMPTY_FOLDERS
provides:
  - EmptyFolderHealthRule Spring bean discovered by HealthRuleRunner
  - NoteRepository.findLiveNoteFolderIdsByNotebookId for live note occupancy
  - Predicate tests for recursive empty, soft-delete, blank-readme exclusion
affects:
  - 02-empty-folder-findings (plan 02 HTTP lint surface)
  - Phase 3 readme_only_folders
  - Phase 7 empty-folder purge

tech-stack:
  added: []
  patterns:
    - O(folders+notes) empty-folder detection via one folder load, one live folder-id set, memoized subtree occupancy
    - HealthRule @Service bean auto-discovered by HealthRuleRunner List injection

key-files:
  created:
    - backend/src/main/java/com/odde/doughnut/services/health/EmptyFolderHealthRule.java
    - backend/src/test/java/com/odde/doughnut/services/health/EmptyFolderHealthRuleTest.java
  modified:
    - backend/src/main/java/com/odde/doughnut/entities/repositories/NoteRepository.java

key-decisions:
  - "Label is bare folder name (D-04 v1)"
  - "Blank readme = null or String.isBlank()"
  - "Always emit empty_folders group even when items empty"

patterns-established:
  - "Health rule beans implement HealthRule as @Service and return one HealthFindingGroup always"
  - "Live occupancy uses DISTINCT n.folder.id with deletedAt IS NULL"

requirements-completed: [EFOL-01]

coverage:
  - id: D1
    description: Fully empty folders reported under empty_folders with recursive, soft-delete-aware, blank-readme predicate
    requirement: EFOL-01
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/health/EmptyFolderHealthRuleTest.java#listsEveryNestedFullyEmptyFolder
        status: pass
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/health/EmptyFolderHealthRuleTest.java#liveNoteInDescendantClearsAncestorOccupancy
        status: pass
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/health/EmptyFolderHealthRuleTest.java#softDeletedNoteDoesNotOccupyFolder
        status: pass
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/health/EmptyFolderHealthRuleTest.java#nonBlankReadmeExcludesFolderFromEmptyFolders
        status: pass
    human_judgment: false
  - id: D2
    description: empty_folders group metadata is title Empty folders, severity warning, autoFixable true
    requirement: EFOL-01
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/health/EmptyFolderHealthRuleTest.java#alwaysEmitsEmptyFoldersGroupWithMetadata
        status: pass
    human_judgment: false

duration: 8min
completed: 2026-07-22
status: complete
---

# Phase 2 Plan 01: Empty-folder findings (rule/predicate) Summary

**Spring `EmptyFolderHealthRule` reports fully empty folders under `empty_folders` via O(folders+notes) live occupancy and blank-readme checks**

## Performance

- **Duration:** 8 min
- **Started:** 2026-07-22T10:09:52Z
- **Completed:** 2026-07-22T10:18:00Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- `EmptyFolderHealthRule` bean plugs into `HealthRuleRunner` / `NotebookHealthService.lint` with rule id `empty_folders`
- Recursive emptiness: every blank-readme empty folder listed; live notes clear ancestors; soft-deleted notes ignored
- Non-blank `readmeContent` folders excluded from this group (reserved for Phase 3)

## Task Commits

Each task was committed atomically:

1. **Task 1: Add EmptyFolderHealthRule predicate tests (RED)** - `0170314a75` (test)
2. **Task 2: Implement live folder-id query and EmptyFolderHealthRule (GREEN)** - `a86008f3a8` (feat)

**Plan metadata:** `846115d90f` (docs: complete plan)

## Files Created/Modified
- `backend/src/main/java/com/odde/doughnut/services/health/EmptyFolderHealthRule.java` - HealthRule bean for fully empty folders
- `backend/src/main/java/com/odde/doughnut/entities/repositories/NoteRepository.java` - `findLiveNoteFolderIdsByNotebookId`
- `backend/src/test/java/com/odde/doughnut/services/health/EmptyFolderHealthRuleTest.java` - predicate coverage via `NotebookHealthService.lint`

## Decisions Made
- Label is bare folder name (D-04 v1)
- Blank readme uses `null || String.isBlank()`
- Group always returned (items may be empty)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Rule injectable and proven; plan 02-02 can expose authorized `POST .../health/lint` and regenerate OpenAPI client
- No controller, frontend, or purge code in this plan

## TDD Gate Compliance
- RED: `0170314a75` test(02-01)
- GREEN: `a86008f3a8` feat(02-01)

## Self-Check: PASSED

---
*Phase: 02-empty-folder-findings*
*Completed: 2026-07-22*
