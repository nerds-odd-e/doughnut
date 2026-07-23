---
phase: 05-health-tab-and-run
plan: 02
subsystem: ui
tags: [health, notebook, lint, vue, daisyui, cypress, vitest]

requires:
  - phase: 05-health-tab-and-run
    provides: Health tab + Run shell + report groups from 05-01
provides:
  - NotebookHealthFindings wire-shape expandable collapse tree
  - Capability-named notebook_health E2E (findings + AFIX-01 no-mutate)
  - Notebook landmarks include Health; folder landmarks exclude Health
affects: [06 user defaults, 07 fix/purge]

tech-stack:
  added: []
  patterns:
    - NotebookHealthFindings child for daisy-collapse wire-shape render
    - Default-open via checkbox :checked when leaf findingCount > 0
    - Capability-named E2E with data-testid Health page-object helpers

key-files:
  created:
    - frontend/src/components/notebook/NotebookHealthFindings.vue
    - e2e_test/features/notebooks/notebook_health.feature
  modified:
    - frontend/src/components/notebook/NotebookHealthPanel.vue
    - frontend/tests/components/notebook/NotebookHealthPanel.spec.ts
    - frontend/components.d.ts
    - e2e_test/start/pageObjects/notebookPage.ts
    - e2e_test/start/pageObjects/workspaceSurfaceLandmarks.ts
    - e2e_test/features/note_view/workspace_surface_landmarks.feature
    - e2e_test/step_definitions/notebook.ts

key-decisions:
  - "Extract findings into NotebookHealthFindings; panel keeps action bar + idle/Run"
  - "Leaf findingCount = items + sum(children.items); empty groups show No findings when expanded"
  - "Primary E2E seeds empty folder, readme-only folder, and [[Missing]]; separate AFIX-01 scenario"

patterns-established:
  - "Pattern: Health findings render report.groups only — no client re-group"
  - "Pattern: notebook_health.feature + notebookPage openHealthTab/runLint for Health E2E"

requirements-completed: [HLTH-03, AFIX-01]

coverage:
  - id: D1
    description: Expandable nested findings match wire shape (default expand/collapse, No findings, nested dead links)
    requirement: HLTH-03
    verification:
      - kind: unit
        ref: frontend/tests/components/notebook/NotebookHealthPanel.spec.ts#expands groups with findings and collapses empty groups by default
        status: pass
      - kind: unit
        ref: frontend/tests/components/notebook/NotebookHealthPanel.spec.ts#shows No findings when an empty group is expanded
        status: pass
      - kind: unit
        ref: frontend/tests/components/notebook/NotebookHealthPanel.spec.ts#nests dead wiki links by note title with token leaf labels
        status: pass
    human_judgment: false
  - id: D2
    description: Open Health → Run lint → expandable groups for seeded empty / readme-only / dead links
    requirement: HLTH-03
    verification:
      - kind: e2e
        ref: e2e_test/features/notebooks/notebook_health.feature#Run lint shows expandable findings for seeded health issues
        status: pass
    human_judgment: false
  - id: D3
    description: Run with Remove empty folders checked does not delete folders
    requirement: AFIX-01
    verification:
      - kind: e2e
        ref: e2e_test/features/notebooks/notebook_health.feature#Run lint with Remove empty folders checked does not delete folders
        status: pass
    human_judgment: false
  - id: D4
    description: Notebook landmarks include Health; folder landmarks assert Health absent
    requirement: HLTH-03
    verification:
      - kind: e2e
        ref: e2e_test/features/note_view/workspace_surface_landmarks.feature#Notebook, folder, and note main columns use different landmarks
        status: pass
    human_judgment: false
  - id: D5
    description: Long finding lists remain reachable via page scroll; long labels wrap without horizontal overflow
    verification: []
    human_judgment: true
    rationale: UI overflow/long-text backstop requires visual judgment with many/long findings

# Metrics
duration: 5min
completed: 2026-07-22
status: complete
---

# Phase 5 Plan 02: Expandable findings and Health E2E Summary

**Extracted wire-shape NotebookHealthFindings plus green capability-named notebook_health E2E (and Health landmarks), completing Phase 5 Behavior.**

## Performance

- **Duration:** ~5 min
- **Started:** 2026-07-22T12:27:53Z
- **Completed:** 2026-07-22T12:32:31Z
- **Tasks:** 2/2
- **Files modified:** 9

## Accomplishments

- Extracted `NotebookHealthFindings` with daisy-collapse per group, leaf counts, default open/closed, nested dead-wiki children, and muted "No findings"
- Strengthened unit coverage for expand/collapse, empty copy, and nested structure
- Shipped `notebook_health.feature` (findings + AFIX-01 no-mutate) without `@wip`
- Updated workspace landmarks so notebook expects Health and folder asserts Health absent

## Task Commits

1. **Task 1: Expandable findings from wire shape** - `26e79568f0` (feat)
2. **Task 2: Targeted notebook_health E2E and landmarks** - `eb5b8b1ff3` (feat)

**Plan metadata:** `218a291739` (docs: complete plan)

## Files Created/Modified

- `frontend/src/components/notebook/NotebookHealthFindings.vue` - Wire-shape expandable findings tree
- `frontend/src/components/notebook/NotebookHealthPanel.vue` - Wires findings child after Run; keeps action bar
- `frontend/tests/components/notebook/NotebookHealthPanel.spec.ts` - Expand/collapse, No findings, nested dead links
- `e2e_test/features/notebooks/notebook_health.feature` - Capability-named Health E2E
- `e2e_test/start/pageObjects/notebookPage.ts` - openHealthTab, runLint, group expectations
- `e2e_test/start/pageObjects/workspaceSurfaceLandmarks.ts` - Notebook Health present; folder Health absent
- `e2e_test/features/note_view/workspace_surface_landmarks.feature` - Step text includes Health
- `e2e_test/step_definitions/notebook.ts` - Health steps + updated landmark steps

## Decisions Made

- Keep findings as a focused child component rather than growing the panel further
- One primary E2E scenario seeds all three finding types; separate scenario proves AFIX-01
- Landmark step wording updated to "Readme, Settings, and Health" for notebook only

## Deviations from Plan

### Auto-fixed Issues

None - plan executed as written.

### TDD Gate Note

New expand/collapse unit assertions passed immediately because 05-01 already rendered inline collapses. Task 1 still extracted `NotebookHealthFindings` and kept those assertions as the contract.

## Known Stubs

None — findings render live report groups; no placeholder copy or Fix controls.

## Threat Flags

None beyond plan threat model (T-05-01 mitigated by AFIX-01 E2E; T-05-02 by landmarks; T-05-03 text-only labels).

## Verification Evidence

- `pnpm frontend:test` Health panel + NotebookPageView + FolderPage.healthTab — pass
- `pnpm cy:run --spec e2e_test/features/notebooks/notebook_health.feature` — 2 passing
- `pnpm cy:run --spec e2e_test/features/note_view/workspace_surface_landmarks.feature` — 1 passing

## Self-Check: PASSED

- FOUND: `frontend/src/components/notebook/NotebookHealthFindings.vue`
- FOUND: `e2e_test/features/notebooks/notebook_health.feature`
- FOUND: commit `26e79568f0`
- FOUND: commit `eb5b8b1ff3`
