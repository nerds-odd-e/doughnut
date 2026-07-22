---
phase: 02-empty-folder-findings
plan: 02
subsystem: health
tags: [health-lint, notebook-api, openapi, authorization, tdd]

requires:
  - phase: 02-empty-folder-findings
    provides: EmptyFolderHealthRule, NotebookHealthService.lint, HealthRunContext, findings DTOs
provides:
  - Authorized POST /api/notebooks/{notebook}/health/lint (owner write gate)
  - NotebookHealthController thin package-private controller
  - Regenerated TypeScript OpenAPI client with health lint operation
affects:
  - Phase 5 Health tab UI (consumes generated client)
  - Phase 7 empty-folder purge (separate fix endpoint)

tech-stack:
  added: []
  patterns:
    - Thin package-private Notebook*Controller colocated under /api/notebooks with assertAuthorization then service delegate
    - Report-only lint uses @Transactional(readOnly = true) and empty HealthRunContext (no fix body)

key-files:
  created:
    - backend/src/main/java/com/odde/doughnut/controllers/NotebookHealthController.java
    - backend/src/test/java/com/odde/doughnut/controllers/NotebookHealthControllerTest.java
  modified:
    - open_api_docs.yaml
    - packages/generated/doughnut-backend-api/sdk.gen.ts
    - packages/generated/doughnut-backend-api/types.gen.ts
    - packages/generated/doughnut-backend-api/index.ts
    - packages/generated/doughnut-backend-api/api-summary.md

key-decisions:
  - "Dedicated NotebookHealthController (not NotebookController) for cohesion and 250-line discipline"
  - "Owner write auth only via assertAuthorization (never assertReadAuthorization)"
  - "No request DTO / fix options (D-09)"

patterns-established:
  - "Health endpoints live on NotebookHealthController under /api/notebooks/{notebook}/health/*"
  - "Lint is read-only transactional and never mutates notebook structure"

requirements-completed: [EFOL-01, EFOL-02]

coverage:
  - id: D1
    description: Authorized owner POST health/lint receives empty_folders group items for fully empty folders
    requirement: EFOL-02
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/controllers/NotebookHealthControllerTest.java#ownerReceivesEmptyFolderFindingsWithoutMutatingNotebook
        status: pass
    human_judgment: false
  - id: D2
    description: Foreign and anonymous callers are rejected when invoking lint
    requirement: EFOL-02
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/controllers/NotebookHealthControllerTest.java#rejectsForeignUser
        status: pass
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/controllers/NotebookHealthControllerTest.java#rejectsAnonymousUser
        status: pass
    human_judgment: false
  - id: D3
    description: Lint does not delete folders or otherwise mutate notebook data
    requirement: EFOL-02
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/controllers/NotebookHealthControllerTest.java#ownerReceivesEmptyFolderFindingsWithoutMutatingNotebook
        status: pass
    human_judgment: false
  - id: D4
    description: TypeScript OpenAPI client regenerated including health lint operation
    requirement: EFOL-02
    verification:
      - kind: other
        ref: packages/generated/doughnut-backend-api/sdk.gen.ts#lint
        status: pass
    human_judgment: false

duration: 4min
completed: 2026-07-22
status: complete
---

# Phase 2 Plan 02: Empty-folder findings (authorized lint API) Summary

**Authorized `POST /api/notebooks/{notebook}/health/lint` returns empty-folder findings without mutating the notebook, with regenerated OpenAPI TS client**

## Performance

- **Duration:** 4 min
- **Started:** 2026-07-22T10:16:26Z
- **Completed:** 2026-07-22T10:20:09Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments
- Thin package-private `NotebookHealthController` exposes report-only lint behind owner write auth
- Controller tests prove owner success (empty_folders items), foreign/anon rejection, and unchanged folder count
- OpenAPI docs and `packages/generated/doughnut-backend-api` regenerated (D-11)

## Task Commits

Each task was committed atomically:

1. **Task 1: Add NotebookHealthController auth and report tests (RED)** - `993fff4e40` (test)
2. **Task 2: Implement NotebookHealthController and regenerate OpenAPI client (GREEN)** - `7618245402` (feat)

**Plan metadata:** (recorded after this summary)

## Files Created/Modified
- `backend/src/main/java/com/odde/doughnut/controllers/NotebookHealthController.java` - POST `/{notebook}/health/lint`
- `backend/src/test/java/com/odde/doughnut/controllers/NotebookHealthControllerTest.java` - owner/foreign/anon/no-mutate
- `open_api_docs.yaml` - generated OpenAPI path for health lint
- `packages/generated/doughnut-backend-api/**` - TS client including `lint` → `/api/notebooks/{notebook}/health/lint`

## Decisions Made
- Dedicated `NotebookHealthController` rather than extending large `NotebookController`
- Write gate only (`assertAuthorization`); no read-only/bazaar/subscriber entrypoint
- Empty body / empty `HealthRunContext` (no fix options)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Phase 2 Behavior complete (EFOL-01 + EFOL-02)
- Phase 5 can consume generated client for Health UI; Phase 7 adds fix/purge separately
- No Health UI, purge endpoint, or dissolve on lint path

## TDD Gate Compliance
- RED: `993fff4e40` test(02-02)
- GREEN: `7618245402` feat(02-02)

## Self-Check: PASSED

---
*Phase: 02-empty-folder-findings*
*Completed: 2026-07-22*
