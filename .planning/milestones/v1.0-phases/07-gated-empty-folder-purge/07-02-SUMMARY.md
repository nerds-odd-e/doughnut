---
phase: 07-gated-empty-folder-purge
plan: 02
subsystem: ui
tags: [notebook-health, empty-folders, fix-ui, e2e]

requires:
  - phase: 07-gated-empty-folder-purge
    provides: NotebookHealthController.fix + EmptyFolderBulkPurge
  - phase: 05-health-tab-and-run
    provides: NotebookHealthPanel action bar and bodyless lint
provides:
  - Gated Fix control notebook-health-fix with Remove N empty folders label
  - Fix → health/fix then auto re-lint + sidebar structural refresh
  - Targeted notebook_health E2E for gated purge (no @wip)
affects:
  - milestone verification / ship

tech-stack:
  added: []
  patterns:
    - "Fix enablement = checkbox && empty_folders items ≥ 1; primary stays on Run lint"
    - "Post-purge refreshSidebarStructuralListings before re-lint"

key-files:
  created:
    - frontend/tests/components/notebook/NotebookHealthPanel.fix.spec.ts
    - frontend/tests/components/notebook/notebookHealthPanelTestSupport.ts
  modified:
    - frontend/src/components/notebook/NotebookHealthPanel.vue
    - frontend/tests/components/notebook/NotebookHealthPanel.spec.ts
    - e2e_test/features/notebooks/notebook_health.feature
    - e2e_test/start/pageObjects/notebookPage.ts
    - e2e_test/start/pageObjects/noteSidebar.ts
    - e2e_test/step_definitions/notebook.ts
    - e2e_test/step_definitions/note.ts

key-decisions:
  - "Fix secondary/sm after checkbox before Save; no confirm dialog"
  - "refreshSidebarStructuralListings after successful fix so tree matches purge"
  - "Split Fix unit specs + shared test support under 250-line limit"

patterns-established:
  - "Pattern: Health Fix body is removeEmptyFolders true only; client re-lint replaces report"
  - "Pattern: expectSidebarFolderAbsent uses find + should('not.exist') without waiting for presence"

requirements-completed: [AFIX-02, AFIX-03, AFIX-04, AFIX-05]

coverage:
  - id: D1
    description: Single gated Fix control on Health action bar with count label
    requirement: AFIX-02
    verification:
      - kind: unit
        ref: frontend/tests/components/notebook/NotebookHealthPanel.fix.spec.ts#enables Fix with count label
        status: pass
      - kind: e2e
        ref: e2e_test/features/notebooks/notebook_health.feature#Gated fix removes fully empty folders
        status: pass
    human_judgment: false
  - id: D2
    description: Fix enabled only when Remove empty folders checked and empty_folders ≥ 1
    requirement: AFIX-03
    verification:
      - kind: unit
        ref: frontend/tests/components/notebook/NotebookHealthPanel.fix.spec.ts#enablement matrix
        status: pass
    human_judgment: false
  - id: D3
    description: Fix then auto re-lint removes purged empty folders; readme-only remains
    requirement: AFIX-04
    verification:
      - kind: e2e
        ref: e2e_test/features/notebooks/notebook_health.feature#Gated fix removes fully empty folders
        status: pass
    human_judgment: false
  - id: D4
    description: Client calls dedicated health/fix then refreshes tree (no dissolve UX)
    requirement: AFIX-05
    verification:
      - kind: unit
        ref: frontend/tests/components/notebook/NotebookHealthPanel.fix.spec.ts#calls fix then re-lints
        status: pass
      - kind: e2e
        ref: e2e_test/features/notebooks/notebook_health.feature#Gated fix removes fully empty folders
        status: pass
    human_judgment: false

duration: 10min
completed: 2026-07-22
status: complete
---

# Phase 7 Plan 02: Gated Health Fix UI Summary

**Health Fix is a secondary action-bar control that posts `removeEmptyFolders: true`, refreshes the sidebar, auto re-lints, and is proven by targeted `notebook_health` E2E.**

## Performance

- **Duration:** 10 min
- **Started:** 2026-07-22T15:04:42Z
- **Completed:** 2026-07-22T15:14:00Z
- **Tasks:** 2/2
- **Files modified:** 9

## Accomplishments

- Added gated `notebook-health-fix` with enablement from checkbox + `empty_folders` count and label `Remove N empty folders`
- On success: `NotebookHealthController.fix` → `refreshSidebarStructuralListings` → bodyless lint; failures keep prior report
- Targeted E2E proves purge removes fully empty folders, keeps readme-only, updates sidebar; no `@wip`

## Task Commits

| Task | Name | Commit |
|------|------|--------|
| 1 RED | Failing Fix unit tests | b92d4f5720 |
| 1 GREEN | Gated Fix + auto re-lint | 1034d87df8 |
| 2 | E2E + sidebar refresh + spec split | 3557ea020a |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical functionality] Sidebar not refreshed after purge**
- **Found during:** Task 2 (E2E sidebar absence assertion)
- **Issue:** Findings showed Empty folders (0) but sidebar still listed purged folders until refresh
- **Fix:** Call `refreshSidebarStructuralListings()` after successful fix (same seam as dissolve)
- **Files modified:** `NotebookHealthPanel.vue`
- **Commit:** 3557ea020a

**2. [Rule 1 - Bug] Absence helper waited for element to exist**
- **Found during:** Task 2 (after sidebar refresh worked)
- **Issue:** `expectSidebarFolderAbsent` used `folderTreitemByLabel` which retries until the folder appears
- **Fix:** Assert with `aside.find(...).should('not.exist')` without `.last()`
- **Files modified:** `noteSidebar.ts`
- **Commit:** 3557ea020a

**3. [Rule 3 - Blocking] Spec file exceeded 250 lines**
- **Found during:** post-change-refactor
- **Issue:** Combined panel + Fix specs grew past the line limit
- **Fix:** Extract `notebookHealthPanelTestSupport.ts` and `NotebookHealthPanel.fix.spec.ts`
- **Commit:** 3557ea020a

## Self-Check: PASSED

- `notebook-health-fix` present in `NotebookHealthPanel.vue`
- Unit specs green (`NotebookHealthPanel.spec.ts` + `.fix.spec.ts`)
- Targeted `cy:run --spec notebook_health.feature` 4/4 green without `@wip`
- `pnpm lint:all` and `pnpm format:all` green
