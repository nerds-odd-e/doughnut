---
phase: 01-health-lint-contract
plan: 02
subsystem: api
tags: [health, lint, HealthRule, Spring, List-injection, junit]

requires:
  - phase: 01-health-lint-contract/01
    provides: NotebookHealthLintReport, HealthFindingGroup, HealthSeverity DTOs
provides:
  - "HealthRule interface (id, title, severity, autoFixable, evaluate)"
  - "Empty HealthRunContext placeholder"
  - "HealthRuleIds constants (empty_folders, readme_only_folders, dead_wiki_links)"
  - "HealthRuleRunner with List<HealthRule> injection"
  - "Public NotebookHealthService.lint orchestration"
  - "Zero-rules → empty groups unit coverage (SC-3)"
affects:
  - Phase 2 EmptyFolderHealthRule + authorized lint controller

tech-stack:
  added: []
  patterns:
    - "Spring List<HealthRule> multi-bean injection mirrored from AssimilationServiceFactory"
    - "List.copyOf for immutable rule registry in HealthRuleRunner"
    - "Thin NotebookHealthService.lint delegates to HealthRuleRunner"

key-files:
  created:
    - backend/src/main/java/com/odde/doughnut/services/health/HealthRule.java
    - backend/src/main/java/com/odde/doughnut/services/health/HealthRunContext.java
    - backend/src/main/java/com/odde/doughnut/services/health/HealthRuleIds.java
    - backend/src/main/java/com/odde/doughnut/services/health/HealthRuleRunner.java
    - backend/src/main/java/com/odde/doughnut/services/NotebookHealthService.java
  modified:
    - backend/src/test/java/com/odde/doughnut/services/health/HealthRuleRunnerTest.java

key-decisions:
  - "HealthRuleRunner uses constructor List.copyOf; skip @Order until Phase 2+"
  - "NotebookHealthService.lint is public for Phase 2 controller call site"
  - "Zero HealthRule @Service beans in Phase 1 (D-11)"

patterns-established:
  - "Pattern: HealthRule.evaluate → HealthFindingGroup; runner maps injection order to report.groups"
  - "Pattern: zero Spring HealthRule beans → empty List → empty groups (D-02)"

requirements-completed: []

coverage:
  - id: D1
    description: "HealthRule interface + HealthRuleRunner + NotebookHealthService.lint under locked package layout (SC-2)"
    verification:
      - kind: unit
        ref: "backend/src/test/java/com/odde/doughnut/services/health/HealthRuleRunnerTest.java#returnsEmptyGroupsWhenNoRulesRegistered"
        status: pass
    human_judgment: false
  - id: D2
    description: "Runner with zero registered rules returns empty groups (SC-3 / D-02)"
    verification:
      - kind: unit
        ref: "backend/src/test/java/com/odde/doughnut/services/health/HealthRuleRunnerTest.java#returnsEmptyGroupsWhenNoRulesRegistered"
        status: pass
    human_judgment: false
  - id: D3
    description: "Full backend unit suite green with no rule beans (SC-1)"
    verification:
      - kind: unit
        ref: "CURSOR_DEV=true nix develop -c pnpm backend:test_only"
        status: pass
    human_judgment: false

duration: 3min
completed: 2026-07-22
status: complete
---

# Phase 01 Plan 02: HealthRule runner skeleton Summary

**Spring `List<HealthRule>` runner and public `NotebookHealthService.lint` with zero rule beans — empty registry returns `groups: []` (SC-2/SC-3); full backend suite green (SC-1).**

## Performance

- **Duration:** ~3 min
- **Started:** 2026-07-22T09:41:47Z
- **Completed:** 2026-07-22T09:44:24Z
- **Tasks:** 2/2
- **Files modified:** 6

## Accomplishments

- Delivered `HealthRule`, empty `HealthRunContext`, reserved `HealthRuleIds`, `HealthRuleRunner`, and public `NotebookHealthService.lint`
- Verified zero rules → empty `NotebookHealthLintReport.groups` via direct unit construction (D-02)
- No HTTP, no rule beans, no fix applicator, no `generateTypeScript` (D-01, D-11, D-16)

## Task Commits

1. **Task 1: Add zero-rules empty report test (RED)** - `9bca556801` (test)
2. **Task 2: Implement HealthRule runner and NotebookHealthService (GREEN)** - `3db676b2c4` (feat)

**Plan metadata:** see docs commit after this SUMMARY

## Files Created/Modified

- `backend/src/main/java/com/odde/doughnut/services/health/HealthRule.java` — rule contract
- `backend/src/main/java/com/odde/doughnut/services/health/HealthRunContext.java` — empty run context placeholder
- `backend/src/main/java/com/odde/doughnut/services/health/HealthRuleIds.java` — reserved snake_case ids
- `backend/src/main/java/com/odde/doughnut/services/health/HealthRuleRunner.java` — List injection → report
- `backend/src/main/java/com/odde/doughnut/services/NotebookHealthService.java` — public lint orchestration
- `backend/src/test/java/com/odde/doughnut/services/health/HealthRuleRunnerTest.java` — empty-rules assertion

## Decisions Made

- Mirrored `AssimilationServiceFactory` List injection; `List.copyOf` for immutability; no `@Order` yet
- `NotebookHealthService.lint` public so Phase 2 controller can call without package hacks
- Registered zero `HealthRule` implementations (D-11)

## Deviations from Plan

None - plan executed exactly as written.

## TDD Gate Compliance

- RED: `test(01-02): …` (`9bca556801`) — compile failure for missing runner/types
- GREEN: `feat(01-02): …` (`3db676b2c4`) — suite green including empty-rules test

## Threat Flags

None — no new HTTP endpoints, auth paths, or schema changes beyond ephemeral in-process DTOs already in threat model.

## Known Stubs

None — empty `HealthRunContext` and zero rule beans are intentional Phase 1 contract (D-11, D-12).

## Self-Check: PASSED

- FOUND: all five service-layer files + extended test
- FOUND: `9bca556801`, `3db676b2c4`
