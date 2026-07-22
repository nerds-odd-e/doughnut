---
phase: 06-user-level-defaults
plan: 01
subsystem: api
tags: [flyway, user-preferences, openapi, health-defaults]

requires:
  - phase: 05-health-tab-and-run
    provides: Health action bar Remove empty folders checkbox (UI-only local state)
provides:
  - "User.healthRemoveEmptyFoldersDefault persisted via Flyway + UserDTO + updateUser"
  - "Generated OpenAPI User / UserDto include healthRemoveEmptyFoldersDefault"
affects:
  - 06-02 Health UI Save/prefill
  - Phase 7 Fix gated on same checkbox

tech-stack:
  added: []
  patterns:
    - "User preference column + UserDTO + null-safe updateUser map (same as spaceIntervals / dailyAssimilationCount)"

key-files:
  created:
    - backend/src/main/resources/db/migration/V300000232__add_health_remove_empty_folders_default.sql
  modified:
    - backend/src/main/java/com/odde/doughnut/entities/User.java
    - backend/src/main/java/com/odde/doughnut/controllers/dto/UserDTO.java
    - backend/src/main/java/com/odde/doughnut/controllers/UserController.java
    - backend/src/test/java/com/odde/doughnut/controllers/UserControllerTest.java
    - open_api_docs.yaml
    - packages/generated/doughnut-backend-api/types.gen.ts
    - packages/doughnut-test-fixtures/src/UserBuilder.ts

key-decisions:
  - "Null-safe map with Objects.requireNonNullElse(..., false) for PATCH body omission"
  - "ERD exporter only lists PK/UK/FK columns — regenerated with no content change for non-key preference column"

patterns-established:
  - "Health user defaults live on User, not NotebookSettings"

requirements-completed: [DFLT-01]

coverage:
  - id: D1
    description: New/unset users expose healthRemoveEmptyFoldersDefault as false on getUserProfile
    requirement: DFLT-01
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/controllers/UserControllerTest.java#newUserHealthRemoveEmptyFoldersDefaultIsFalse
        status: pass
    human_judgment: false
  - id: D2
    description: Authorized updateUser persists healthRemoveEmptyFoldersDefault true and round-trips on getUserProfile
    requirement: DFLT-01
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/controllers/UserControllerTest.java#updateUserPersistsHealthRemoveEmptyFoldersDefault
        status: pass
    human_judgment: false
  - id: D3
    description: updateOtherUserProfile still denies foreign-user PATCH (T-06-01)
    requirement: DFLT-01
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/controllers/UserControllerTest.java#updateOtherUserProfile
        status: pass
    human_judgment: false
  - id: D4
    description: Generated OpenAPI User / UserDto include healthRemoveEmptyFoldersDefault
    requirement: DFLT-01
    verification:
      - kind: other
        ref: packages/generated/doughnut-backend-api/types.gen.ts#healthRemoveEmptyFoldersDefault
        status: pass
    human_judgment: false

duration: 6min
completed: 2026-07-22
status: complete
---

# Phase 6 Plan 01: User-level defaults (persistence) Summary

**Flyway + User/UserDTO/updateUser persist `healthRemoveEmptyFoldersDefault` (default false) with green controller round-trip and regenerated OpenAPI client.**

## Performance

- **Duration:** 6 min
- **Started:** 2026-07-22T13:20:02Z
- **Completed:** 2026-07-22T13:25:30Z
- **Tasks:** 2/2
- **Files modified:** 8

## Accomplishments

- Added `user.health_remove_empty_folders_default` (tinyint NOT NULL DEFAULT 0) via Flyway V300000232
- Mapped optional `UserDTO.healthRemoveEmptyFoldersDefault` through `updateUser` without weakening `assertAuthorization`
- Regenerated OpenAPI TypeScript types; UserBuilder defaults the new boolean to false

## Task Commits

1. **Task 1: RED UserController tests for Health default** - `585166cb07` (test)
2. **Task 2: GREEN User column, DTO, updateUser, OpenAPI regen** - `0d740a4346` (feat)

## Files Created/Modified

- `backend/src/main/resources/db/migration/V300000232__add_health_remove_empty_folders_default.sql` — preference column
- `backend/src/main/java/com/odde/doughnut/entities/User.java` — Boolean field default false
- `backend/src/main/java/com/odde/doughnut/controllers/dto/UserDTO.java` — optional Boolean for PATCH
- `backend/src/main/java/com/odde/doughnut/controllers/UserController.java` — null-safe map in updateUser
- `backend/src/test/java/com/odde/doughnut/controllers/UserControllerTest.java` — default false + round-trip
- `open_api_docs.yaml` / `packages/generated/doughnut-backend-api/types.gen.ts` — regen only
- `packages/doughnut-test-fixtures/src/UserBuilder.ts` — fixture default false

## Decisions Made

- Used `Objects.requireNonNullElse(..., false)` for null-safe PATCH mapping (plan-required)
- Ran `pnpm export:database-erd` after migration; Mermaid ERD lists only PK/UK/FK columns so the new non-key column does not appear (exporter design — no hand-edit)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] OpenAPI committed YAML lagged entity change**
- **Found during:** Task 2 (`pnpm backend:test` first run)
- **Issue:** `RobotsTests.openApiDocsMatchCommittedYaml` failed until client regen
- **Fix:** Ran `pnpm generateTypeScript` (required by plan; first full test ran before regen completed)
- **Files modified:** `open_api_docs.yaml`, `packages/generated/doughnut-backend-api/types.gen.ts`
- **Verification:** `pnpm backend:test` green
- **Committed in:** `0d740a4346`

## TDD Gate Compliance

- RED: `585166cb07` — compile failed on missing `getHealthRemoveEmptyFoldersDefault` / DTO setter (correct reason)
- GREEN: `0d740a4346` — `backend:test` green including new controller cases

## Known Stubs

None.

## Threat Flags

None beyond plan threat model (T-06-01 mitigated; no new endpoints).

## Self-Check: PASSED

- FOUND: migration, User, UserDTO, UserController, UserControllerTest, types.gen.ts
- FOUND: commits `585166cb07`, `0d740a4346`
- FOUND: `healthRemoveEmptyFoldersDefault` in generated types
- FOUND: column in `doughnut_test.user` (`tinyint(1)` default `0`)
