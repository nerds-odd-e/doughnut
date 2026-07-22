---
phase: 04-dead-link-findings
plan: 02
subsystem: backend-health
tags: [health-lint, dead-wiki-links, DeadWikiLinkHealthRule, tdd]

requires:
  - phase: 04-dead-link-findings
    provides: HealthRunContext.viewer, unresolvedWikiLinkTokens, findLiveNotesByNotebookIdOrderByIdAsc
provides:
  - DeadWikiLinkHealthRule Spring HealthRule bean
  - Nested dead_wiki_links findings via NotebookHealthService.lint
  - DLNK-01, DLNK-02, DLNK-03 proven with backend tests
affects: [Phase 5 Health UI, Phase 7 must never auto-fix dead links]

tech-stack:
  added: []
  patterns:
    - "HealthRule bean with children-by-note nesting, autoFixable=false"
    - "Unresolved tokens via WikiLinkResolver.unresolvedWikiLinkTokens + live notes query"

key-files:
  created:
    - backend/src/main/java/com/odde/doughnut/services/health/DeadWikiLinkHealthRule.java
    - backend/src/test/java/com/odde/doughnut/services/health/DeadWikiLinkHealthRuleTest.java
  modified: []

key-decisions:
  - "Top group items empty list; children per note with dead tokens"
  - "Leaf wikiLinkToken and label are full inner strings"
  - "Report-only: autoFixable false on top and children"

patterns-established:
  - "Nested HealthFindingGroup children for per-note dead links"
  - "D-13 scenarios proven via NotebookHealthService.lint entry only"

requirements-completed: [DLNK-01, DLNK-02, DLNK-03]

coverage:
  - id: D1
    description: Body dead wiki links reported nested by note
    requirement: DLNK-01
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/health/DeadWikiLinkHealthRuleTest.java#reportsBodyDeadWikiLinkNestedUnderSourceNote
        status: pass
    human_judgment: false
  - id: D2
    description: Frontmatter dead wiki links reported via same extract path
    requirement: DLNK-02
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/health/DeadWikiLinkHealthRuleTest.java#reportsFrontmatterDeadWikiLinkWithSameTokenShape
        status: pass
    human_judgment: false
  - id: D3
    description: Report-only — autoFixable false and no mutation
    requirement: DLNK-03
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/health/DeadWikiLinkHealthRuleTest.java#lintDoesNotMutateNoteContentOrCount
        status: pass
    human_judgment: false
  - id: D4
    description: Live alias/qualified not dead; soft-deleted source excluded; always emit
    verification:
      - kind: unit
        ref: CURSOR_DEV=true nix develop -c pnpm backend:test_only
        status: pass
    human_judgment: false

duration: 6min
completed: 2026-07-22
status: complete
---

# Phase 4 Plan 02: DeadWikiLinkHealthRule Summary

**Lint reports nested `dead_wiki_links` for body and frontmatter unresolved tokens (viewer-readable resolve), report-only with `autoFixable=false`.**

## Performance

- **Duration:** ~6 min
- **Started:** 2026-07-22T11:53:00Z
- **Completed:** 2026-07-22T11:56:00Z
- **Tasks:** 2/2
- **Files modified:** 2

## Accomplishments

- TDD RED: `DeadWikiLinkHealthRuleTest` fails for missing `dead_wiki_links` group
- GREEN: `@Service DeadWikiLinkHealthRule` loads live notes, resolves via `unresolvedWikiLinkTokens(note, viewer)`, nests children by note
- Always emits top group; leaf items use `noteId` + full-inner `wikiLinkToken`/`label`; no `folderId`
- Coexists with `empty_folders` and `readme_only_folders`; no content/count mutation

## Task Commits

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 (RED) | Add DeadWikiLinkHealthRule tests | 188a6a4b1b | DeadWikiLinkHealthRuleTest.java |
| 2 (GREEN) | Implement DeadWikiLinkHealthRule | 3ddd7ed0bc | DeadWikiLinkHealthRule.java |

## TDD Gate Compliance

- RED commit `test(04-02): …` present
- GREEN commit `feat(04-02): …` present after RED

## Deviations from Plan

None - plan executed as written.

## Known Stubs

None.

## Self-Check: PASSED

- FOUND: DeadWikiLinkHealthRule.java, DeadWikiLinkHealthRuleTest.java
- FOUND: commits 188a6a4b1b, 3ddd7ed0bc
- Backend suite: BUILD SUCCESSFUL
