---
phase: 01-health-lint-contract
plan: 01
subsystem: api
tags: [health, lint, dto, openapi, lombok, junit]

requires: []
provides:
  - "NotebookHealthLintReport with groups list"
  - "Recursive HealthFindingGroup (items + children)"
  - "HealthFindingItem identity fields (folderId, noteId, label, message, wikiLinkToken)"
  - "HealthSeverity enum (error, warning, info)"
  - "HealthRuleRunnerTest nested construction coverage (SC-3b)"
affects:
  - 01-02 HealthRule runner skeleton
  - Phase 2 empty-folder rule + lint HTTP

tech-stack:
  added: []
  patterns:
    - "Lombok @Getter/@Setter DTOs under controllers/dto with light @Schema REQUIRED"
    - "Lowercase HealthSeverity enum constants mirroring Randomization.RandomStrategy"
    - "Recursive children on HealthFindingGroup like AttachBookLayoutNodeRequest"

key-files:
  created:
    - backend/src/main/java/com/odde/doughnut/controllers/dto/HealthSeverity.java
    - backend/src/main/java/com/odde/doughnut/controllers/dto/HealthFindingItem.java
    - backend/src/main/java/com/odde/doughnut/controllers/dto/HealthFindingGroup.java
    - backend/src/main/java/com/odde/doughnut/controllers/dto/NotebookHealthLintReport.java
    - backend/src/test/java/com/odde/doughnut/services/health/HealthRuleRunnerTest.java
  modified: []

key-decisions:
  - "DTO classes (not records) with Lombok getters/setters per existing controllers/dto patterns"
  - "HealthSeverity as public top-level enum with lowercase error|warning|info constants"
  - "Omit item-level autoFixable; keep group-level autoFixable only (D-06)"

patterns-established:
  - "Pattern: recursive HealthFindingGroup.items + children for nested lint UI later"
  - "Pattern: ephemeral findings DTOs only — no JPA/Flyway for findings"

requirements-completed: []

coverage:
  - id: D1
    description: "OpenAPI-ready findings DTO shapes (report, group, item, severity)"
    verification:
      - kind: unit
        ref: "backend/src/test/java/com/odde/doughnut/services/health/HealthRuleRunnerTest.java#nestedFindingGroupRetainsItemsAndChildren"
        status: pass
    human_judgment: false
  - id: D2
    description: "Nested HealthFindingGroup constructible with items and children (SC-3b)"
    verification:
      - kind: unit
        ref: "backend/src/test/java/com/odde/doughnut/services/health/HealthRuleRunnerTest.java#nestedFindingGroupRetainsItemsAndChildren"
        status: pass
    human_judgment: false

duration: 3min
completed: 2026-07-22
status: complete
---

# Phase 01 Plan 01: Health findings DTO contract Summary

**OpenAPI-ready nested findings DTOs (`NotebookHealthLintReport` → recursive `HealthFindingGroup` → `HealthFindingItem` + `HealthSeverity`) with a unit test proving items+children construction.**

## Performance

- **Duration:** 3 min
- **Started:** 2026-07-22T09:35:58Z
- **Completed:** 2026-07-22T09:39:10Z
- **Tasks:** 2/2
- **Files modified:** 5

## Accomplishments

- Delivered SC-2 shape: report, recursive group, item identity fields, severity enum
- Proved SC-3b nested `items` + `children` construction in `HealthRuleRunnerTest`
- Kept Structure-only: no HTTP routes, Flyway findings table, frontend, or TypeScript regen

## Task Commits

1. **Task 1: Add nested findings DTO construction test (RED)** - `c08488bb47` (test)
2. **Task 2: Implement OpenAPI-ready findings DTOs (GREEN)** - `fd8890da90` (feat)

**Plan metadata:** (pending docs commit)

## Files Created/Modified

- `backend/src/main/java/com/odde/doughnut/controllers/dto/HealthSeverity.java` — severity vocabulary error|warning|info
- `backend/src/main/java/com/odde/doughnut/controllers/dto/HealthFindingItem.java` — optional folderId/noteId/label/message/wikiLinkToken
- `backend/src/main/java/com/odde/doughnut/controllers/dto/HealthFindingGroup.java` — rule metadata + items + recursive children
- `backend/src/main/java/com/odde/doughnut/controllers/dto/NotebookHealthLintReport.java` — top-level groups list
- `backend/src/test/java/com/odde/doughnut/services/health/HealthRuleRunnerTest.java` — nested construction assertion (readme vocabulary)

## Decisions Made

- Followed existing Lombok DTO + light `@Schema(REQUIRED)` style (`FolderCreationRequest`)
- `HealthSeverity` mirrors `Randomization.RandomStrategy` lowercase constants
- Item-level `autoFixable` omitted per D-06; group-level retained

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Plan 01-02 can add `HealthRule` / runner / `NotebookHealthService` and the empty-rules test on the same `HealthRuleRunnerTest` class. DTOs are ready for Phase 2 to return a typed lint report.

## Self-Check: PASSED

- FOUND: all four DTO files and `HealthRuleRunnerTest.java`
- FOUND: commits `c08488bb47`, `fd8890da90`
- Verified: `pnpm backend:test_only` green; recursive `children` present; readme vocabulary; no controller/Flyway/frontend in plan scope

---
*Phase: 01-health-lint-contract*
*Completed: 2026-07-22*
