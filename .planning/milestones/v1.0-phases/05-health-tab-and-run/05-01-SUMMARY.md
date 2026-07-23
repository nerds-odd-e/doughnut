---
phase: 05-health-tab-and-run
plan: 01
subsystem: ui
tags: [health, notebook, lint, vue, daisyui, vitest]

requires:
  - phase: 04-dead-link-findings
    provides: NotebookHealthController.lint multi-rule report (empty_folders, readme_only_folders, dead_wiki_links)
provides:
  - Notebook-only Health tab (Readme | Settings | Health)
  - NotebookHealthPanel with idle → Run lint → report groups shell
  - AFIX-01 UI-only Remove empty folders checkbox (no Fix/Apply)
affects: [05-02 findings expand/E2E, 06 user defaults, 07 fix/purge]

tech-stack:
  added: []
  patterns:
    - includeHealth prop default false on WorkspaceReadmeSettingsTabs
    - Explicit three-way v-if / v-else-if tab panels (no bare v-else)
    - Bodyless lint via apiCallWithLoading + NotebookHealthController.lint

key-files:
  created:
    - frontend/src/components/notebook/NotebookHealthPanel.vue
    - frontend/tests/components/notebook/NotebookHealthPanel.spec.ts
    - frontend/tests/pages/FolderPage.healthTab.spec.ts
  modified:
    - frontend/src/components/commons/WorkspaceReadmeSettingsTabs.vue
    - frontend/src/pages/NotebookPageView.vue
    - frontend/tests/pages/NotebookPageView.spec.ts
    - frontend/components.d.ts

key-decisions:
  - "Findings render inline in NotebookHealthPanel (collapse polish deferred to 05-02)"
  - "Remove empty folders checkbox wrapped for notebook-health-remove-empty-folders testid"
  - "Panel owns notebook-workspace-health testid like NotebookWorkspaceSettings"

patterns-established:
  - "Pattern: includeHealth opt-in keeps FolderPage at Readme|Settings"
  - "Pattern: Health Run is path-only lint; checkbox never in request body"

requirements-completed: [HLTH-01, HLTH-02, AFIX-01]

coverage:
  - id: D1
    description: Notebook owner sees Health tab; default remains readme; Health shows panel without Settings
    requirement: HLTH-01
    verification:
      - kind: unit
        ref: frontend/tests/pages/NotebookPageView.spec.ts#shows home landmarks and hides admin sections on first paint
        status: pass
      - kind: unit
        ref: frontend/tests/pages/NotebookPageView.spec.ts#shows Health panel and hides Settings after opening Health tab
        status: pass
    human_judgment: false
  - id: D2
    description: Folder settings has no Health tab
    requirement: HLTH-01
    verification:
      - kind: unit
        ref: frontend/tests/pages/FolderPage.healthTab.spec.ts#shows Readme and Settings tabs but not Health
        status: pass
    human_judgment: false
  - id: D3
    description: Idle panel does not call lint; Run uses bodyless path-only lint and shows report groups
    requirement: HLTH-02
    verification:
      - kind: unit
        ref: frontend/tests/components/notebook/NotebookHealthPanel.spec.ts#shows idle prompt and action bar without calling lint on mount
        status: pass
      - kind: unit
        ref: frontend/tests/components/notebook/NotebookHealthPanel.spec.ts#runs bodyless path-only lint and shows report groups
        status: pass
    human_judgment: false
  - id: D4
    description: Remove empty folders checkbox is UI-only; Run stays path-only; no Fix/Apply
    requirement: AFIX-01
    verification:
      - kind: unit
        ref: frontend/tests/components/notebook/NotebookHealthPanel.spec.ts#keeps lint path-only when Remove empty folders is checked and has no Fix control
        status: pass
    human_judgment: false

duration: 8min
completed: 2026-07-22
status: complete
---

# Phase 5 Plan 01: Health tab and Run Summary

**Notebook settings gain a Health tab with idle → Run lint → on-tab report groups; folder settings stay Readme|Settings only.**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-07-22T12:22:00Z
- **Completed:** 2026-07-22T12:26:00Z
- **Tasks:** 2/2
- **Files modified:** 7 (product + tests + components.d.ts)

## Accomplishments

- Extended `WorkspaceReadmeSettingsTabs` with optional Health (`includeHealth` default false)
- Wired `NotebookPageView` three-way branch + `NotebookHealthPanel` (Run lint via `apiCallWithLoading`, bodyless path-only)
- Green unit coverage for notebook Health tab, folder Health-absent, idle/Run/AFIX-01

## Task Commits

Single wrap-up commit (local execute-plan): product + tests + this SUMMARY.

1. **Task 1: RED unit tests** — `7147033873` (feat)
2. **Task 2: GREEN Health tab shell, panel, and Run** — `7147033873` (feat)

**Plan metadata:** included in `7147033873`

## Files Created/Modified

- `frontend/src/components/commons/WorkspaceReadmeSettingsTabs.vue` — `health` tab + `includeHealth`
- `frontend/src/pages/NotebookPageView.vue` — `include-health` + three-way panel branch
- `frontend/src/components/notebook/NotebookHealthPanel.vue` — action bar, idle, Run, report groups shell
- `frontend/tests/pages/NotebookPageView.spec.ts` — Health tab presence / default readme / Health click
- `frontend/tests/components/notebook/NotebookHealthPanel.spec.ts` — idle, Run, nested labels, AFIX-01
- `frontend/tests/pages/FolderPage.healthTab.spec.ts` — folder Health tab absent
- `frontend/components.d.ts` — auto component registration

## Decisions Made

- Findings list kept inline in the panel (05-02 owns expand/collapse polish + E2E)
- Checkbox testid on wrapper around `CheckInput` (component has no testid prop)
- No backend / OpenAPI / generated package changes

## Deviations from Plan

None - plan executed exactly as written.

## TDD Gate Compliance

- RED: NotebookPageView Health assertions failed (tab absent); NotebookHealthPanel suite failed to import missing component; FolderPage Health-absent already green (documents D-03)
- GREEN: all three targeted specs pass (12 tests)

## Known Stubs

None — idle before Run is intentional; findings render from live lint response after Run.

## Self-Check: PASSED

All key files present; 12 targeted unit tests green; no backend/OpenAPI/generated diffs.
