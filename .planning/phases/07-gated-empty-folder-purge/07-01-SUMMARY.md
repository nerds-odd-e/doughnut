---
phase: 07-gated-empty-folder-purge
plan: 01
subsystem: api
tags: [notebook-health, empty-folders, bulk-purge, openapi]

requires:
  - phase: 02-empty-folder-findings
    provides: FolderSubtreeLiveNotes fully-empty predicate and empty_folders rule
  - phase: 05-health-tab-and-run
    provides: NotebookHealthController lint endpoint and auth pattern
provides:
  - POST /api/notebooks/{notebook}/health/fix with removeEmptyFolders opt-in
  - EmptyFolderBulkPurge CASCADE-safe hard-delete of fully empty folder trees
  - Regenerated OpenAPI client NotebookHealthController.fix
affects:
  - 07-02 Fix UI and E2E

tech-stack:
  added: []
  patterns:
    - "Lint ≠ fix: bodyless readOnly lint vs writable fix with removeEmptyFolders gate"
    - "D ⊆ S CASCADE-safe deletable filter before EntityPersister.remove deepest-first"

key-files:
  created:
    - backend/src/main/java/com/odde/doughnut/controllers/dto/NotebookHealthFixRequest.java
    - backend/src/main/java/com/odde/doughnut/services/health/EmptyFolderBulkPurge.java
    - backend/src/test/java/com/odde/doughnut/services/health/EmptyFolderBulkPurgeTest.java
  modified:
    - backend/src/main/java/com/odde/doughnut/services/health/FolderSubtreeLiveNotes.java
    - backend/src/main/java/com/odde/doughnut/services/NotebookHealthService.java
    - backend/src/main/java/com/odde/doughnut/controllers/NotebookHealthController.java
    - backend/src/test/java/com/odde/doughnut/controllers/NotebookHealthControllerTest.java
    - packages/generated/doughnut-backend-api
    - open_api_docs.yaml

key-decisions:
  - "EmptyFolderBulkPurge collaborator in services/health; void fix response"
  - "D ⊆ S requires all real descendants in empty set S before hard-delete"
  - "Detach soft-deleted notes (folder=null) before remove for Hibernate session coherence"

patterns-established:
  - "Pattern: Health mutation only via dedicated fix path with Boolean.TRUE.equals gate"
  - "Pattern: cascadeSafeFullyEmptyFolders reuses note-empty ∩ blank readme then subtree-safe filter"

requirements-completed: [AFIX-02, AFIX-03, AFIX-04, AFIX-05]

coverage:
  - id: D1
    description: Authorized POST health/fix with removeEmptyFolders true purges CASCADE-safe fully empty trees
    requirement: AFIX-02
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/controllers/NotebookHealthControllerTest.java#authorizedOwnerFixSucceeds
        status: pass
    human_judgment: false
  - id: D2
    description: Fix rejected when removeEmptyFolders is missing, null, or false
    requirement: AFIX-03
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/health/EmptyFolderBulkPurgeTest.java#fixRejectsWithoutOptIn
        status: pass
    human_judgment: false
  - id: D3
    description: Readme-only folders and blank parent over readme-only child are not CASCADE-deleted
    requirement: AFIX-04
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/health/EmptyFolderBulkPurgeTest.java#blankParentOverReadmeOnlyChildNotCascaded
        status: pass
    human_judgment: false
  - id: D4
    description: Dedicated purge uses EntityPersister.remove deepest-first; never dissolve
    requirement: AFIX-05
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/health/EmptyFolderBulkPurgeTest.java#nestedFullyEmptyTreePurgedDeepestFirst
        status: pass
    human_judgment: false

duration: 6min
completed: 2026-07-22
status: complete
---

# Phase 7 Plan 01: Gated empty-folder purge API Summary

**Authorized `POST .../health/fix` hard-deletes CASCADE-safe fully empty folder trees when `removeEmptyFolders` is true, never dissolving or wiping readme-only descendants.**

## Performance

- **Duration:** 6 min
- **Started:** 2026-07-22T14:55:51Z
- **Completed:** 2026-07-22T15:02:10Z
- **Tasks:** 2/2
- **Files modified:** 11

## Accomplishments

- Added `EmptyFolderBulkPurge` that recomputes fully-empty set S via `FolderSubtreeLiveNotes`, builds deletable D ⊆ S (all descendants in S), removes deepest-first with `EntityPersister.remove`
- Wired `NotebookHealthService.fix` opt-in gate (`Boolean.TRUE.equals`) and writable `POST /{notebook}/health/fix` with `assertAuthorization`
- Detach soft-deleted notes (`folder=null`) before remove so Hibernate matches `ON DELETE SET NULL`
- Regenerated OpenAPI TypeScript client exposing `NotebookHealthController.fix`

## Task Commits

| Task | Name | Commit |
|------|------|--------|
| 1 | RED purge and fix-endpoint tests | 07f779c807 |
| 2 | GREEN EmptyFolderBulkPurge, fix API, OpenAPI regen | 563808c977 |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Soft-deleted notes blocked folder remove in Hibernate session**
- **Found during:** Task 2 (softDeletedOnlyEmptyFolderPurged)
- **Issue:** Notes still referencing the folder in-session caused `TransientPropertyValueException` on `EntityPersister.remove`
- **Fix:** Before remove, null `folder` on all notes in that folder (including soft-deleted), flush, then remove
- **Files modified:** `EmptyFolderBulkPurge.java`

## Self-Check: PASSED

- `EmptyFolderBulkPurge.java` present
- `NotebookHealthFixRequest.java` present
- Generated SDK exposes `/api/notebooks/{notebook}/health/fix`
- Backend tests green (`pnpm backend:test_only`)
- `pnpm lint:all` and `pnpm format:all` green
