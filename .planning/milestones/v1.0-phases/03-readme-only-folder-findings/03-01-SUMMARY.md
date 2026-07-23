---
phase: 03-readme-only-folder-findings
plan: 01
subsystem: health
tags: [health-rule, readme-only-folders, empty-folders, spring-service, tdd]

requires:
  - phase: 02-empty-folder-findings
    provides: EmptyFolderHealthRule, NotebookHealthService.lint, HealthRuleIds.README_ONLY_FOLDERS reserved, blank-readme threshold
provides:
  - ReadmeOnlyFolderHealthRule Spring bean (autoFixable false)
  - FolderSubtreeLiveNotes shared note-empty subtree scan
  - Mutual-exclusion proofs vs empty_folders via ReadmeOnlyFolderHealthRuleTest
affects:
  - Phase 5 Health tab UI (both groups in one report)
  - Phase 7 empty-folder purge (must never delete readme_only_folders)

tech-stack:
  added: []
  patterns:
    - Complementary blank/non-blank own-readme gates on shared FolderSubtreeLiveNotes.noteEmptyFolderItems
    - autoFixable=false reserves fix-eligibility boundary before Phase 7 purge

key-files:
  created:
    - backend/src/main/java/com/odde/doughnut/services/health/FolderSubtreeLiveNotes.java
    - backend/src/main/java/com/odde/doughnut/services/health/ReadmeOnlyFolderHealthRule.java
    - backend/src/test/java/com/odde/doughnut/services/health/ReadmeOnlyFolderHealthRuleTest.java
  modified:
    - backend/src/main/java/com/odde/doughnut/services/health/EmptyFolderHealthRule.java

key-decisions:
  - "Own non-blank readme only — no ancestor inheritance (D-02)"
  - "readme_only_folders autoFixable=false (D-07)"
  - "Shared FolderSubtreeLiveNotes.noteEmptyFolderItems for complementary gates (D-10)"

patterns-established:
  - "Note-empty folder rules share FolderSubtreeLiveNotes and differ only by readme predicate and group metadata"
  - "Mutual exclusion proven by looking up groups by ruleId in one lint report"

requirements-completed: [EFOL-03]

coverage:
  - id: D1
    description: Note-empty folders with non-blank own readmeContent reported under readme_only_folders
    requirement: EFOL-03
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/health/ReadmeOnlyFolderHealthRuleTest.java#listsEveryNestedNoteEmptyFolderWithNonBlankOwnReadme
        status: pass
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/health/ReadmeOnlyFolderHealthRuleTest.java#softDeletedNoteDoesNotOccupyReadmeOnlyFolder
        status: pass
    human_judgment: false
  - id: D2
    description: Fully empty folders only under empty_folders; mutual exclusion and own-readme partition
    requirement: EFOL-03
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/health/ReadmeOnlyFolderHealthRuleTest.java#partitionsByOwnReadmeBlanknessWithMutualExclusion
        status: pass
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/health/ReadmeOnlyFolderHealthRuleTest.java#ownReadmeOnlyDoesNotInheritFromParent
        status: pass
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/health/ReadmeOnlyFolderHealthRuleTest.java#liveNoteInDescendantClearsOccupancyFromBothGroups
        status: pass
    human_judgment: false
  - id: D3
    description: readme_only_folders always emitted with autoFixable false; lint is report-only
    requirement: EFOL-03
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/health/ReadmeOnlyFolderHealthRuleTest.java#alwaysEmitsReadmeOnlyFoldersGroupWithMetadata
        status: pass
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/health/ReadmeOnlyFolderHealthRuleTest.java#lintDoesNotChangeFolderCount
        status: pass
    human_judgment: false

duration: 6min
completed: 2026-07-22
status: complete
---

# Phase 3 Plan 01: Readme-only folder findings Summary

**`readme_only_folders` HealthRule reports note-empty folders with non-blank own readme, mutually exclusive from `empty_folders`, with `autoFixable=false`**

## Performance

- **Duration:** 6 min
- **Started:** 2026-07-22T10:38:35Z
- **Completed:** 2026-07-22T10:44:16Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- `ReadmeOnlyFolderHealthRule` bean plugs into existing `NotebookHealthService.lint` (no new HTTP surface)
- Shared `FolderSubtreeLiveNotes` keeps emptiness scan identical for empty vs readme-only rules
- Mutual exclusion, own-readme-only partition, soft-delete, always-emit metadata (`autoFixable=false`), and no-mutation proven in backend tests
- Existing `EmptyFolderHealthRuleTest` remains green after extract

## Task Commits

Each task was committed atomically:

1. **Task 1: Add ReadmeOnlyFolderHealthRule tests (RED)** - `2cf382cefc` (test)
2. **Task 2: Shared scan helper + ReadmeOnlyFolderHealthRule (GREEN)** - `353be4d47f` (feat)

## Files Created/Modified
- `backend/src/main/java/com/odde/doughnut/services/health/FolderSubtreeLiveNotes.java` - package-private shared note-empty scan + item builder
- `backend/src/main/java/com/odde/doughnut/services/health/ReadmeOnlyFolderHealthRule.java` - HealthRule bean for readme-only folders
- `backend/src/main/java/com/odde/doughnut/services/health/EmptyFolderHealthRule.java` - refactored onto shared helper (blank-readme gate unchanged)
- `backend/src/test/java/com/odde/doughnut/services/health/ReadmeOnlyFolderHealthRuleTest.java` - EFOL-03 / D-01..D-12 coverage via `NotebookHealthService.lint`

## Decisions Made
- Own non-blank readme only — no ancestor inheritance (D-02)
- Group `autoFixable=false` so Phase 7 cannot treat readme-only as purge-eligible by metadata alone (D-07)
- Shared `FolderSubtreeLiveNotes.noteEmptyFolderItems(Predicate)` for complementary gates (D-10)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Critical] Collapsed evaluate-loop duplication into `noteEmptyFolderItems`**
- **Found during:** post-change-refactor after Task 2
- **Issue:** Both rules duplicated children-map / memo / item-build after extracting only low-level scan helpers
- **Fix:** Added `FolderSubtreeLiveNotes.noteEmptyFolderItems` with a readme `Predicate`; rules pass `isBlankReadme` vs `!isBlankReadme`
- **Files modified:** `FolderSubtreeLiveNotes.java`, `EmptyFolderHealthRule.java`, `ReadmeOnlyFolderHealthRule.java`
- **Commit:** `353be4d47f`

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Known Stubs
None

## Threat Flags
None — no new endpoints, auth paths, or schema changes at trust boundaries.

## TDD Gate Compliance
- RED: `2cf382cefc` test(03-01)
- GREEN: `353be4d47f` feat(03-01)

## Next Phase Readiness
- Phase 3 Behavior complete (EFOL-03)
- Phase 4 dead-link rule can plug into the same runner; Phase 5 UI will show both empty and readme-only groups
- Phase 7 purge must respect `autoFixable=false` on `readme_only_folders`

## Self-Check: PASSED
- Files: FolderSubtreeLiveNotes, ReadmeOnlyFolderHealthRule, EmptyFolderHealthRule, ReadmeOnlyFolderHealthRuleTest, 03-01-SUMMARY.md
- Commits: `2cf382cefc` (test), `353be4d47f` (feat)
