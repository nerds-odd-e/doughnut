---
phase: 04-dead-link-findings
plan: 01
subsystem: backend-health
tags: [health-lint, wiki-links, HealthRunContext, WikiLinkResolver]

requires:
  - phase: 03-readme-only-folder-findings
    provides: HealthRule runner, always-emit groups, NotebookHealthController lint
provides:
  - HealthRunContext.viewer for authorized lint caller
  - WikiLinkResolver.unresolvedWikiLinkTokens (viewer-readable)
  - NoteRepository.findLiveNotesByNotebookIdOrderByIdAsc
affects: [04-02 DeadWikiLinkHealthRule, Phase 5 Health UI]

tech-stack:
  added: []
  patterns:
    - "HealthRunContext carries required User viewer"
    - "Invert resolveWikiLinksForCache loop for unresolved tokens"

key-files:
  created: []
  modified:
    - backend/src/main/java/com/odde/doughnut/services/health/HealthRunContext.java
    - backend/src/main/java/com/odde/doughnut/controllers/NotebookHealthController.java
    - backend/src/main/java/com/odde/doughnut/services/WikiLinkResolver.java
    - backend/src/main/java/com/odde/doughnut/entities/repositories/NoteRepository.java
    - backend/src/test/java/com/odde/doughnut/services/health/EmptyFolderHealthRuleTest.java
    - backend/src/test/java/com/odde/doughnut/services/health/ReadmeOnlyFolderHealthRuleTest.java
    - backend/src/test/java/com/odde/doughnut/services/health/HealthRuleRunnerTest.java

key-decisions:
  - "HealthRunContext requires User viewer; no no-arg constructor"
  - "unresolvedWikiLinkTokens uses resolveToken only (not any-target)"
  - "Live notes query: deletedAt IS NULL, ORDER BY id ASC"

patterns-established:
  - "Viewer on HealthRunContext for viewer-readable health rules"
  - "Dead-token helper lives on WikiLinkResolver, not health/"

requirements-completed: [DLNK-01, DLNK-02]

coverage:
  - id: D1
    description: HealthRunContext carries authorized lint caller as viewer
    requirement: DLNK-01
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/health/EmptyFolderHealthRuleTest.java
        status: pass
    human_judgment: false
  - id: D2
    description: unresolvedWikiLinkTokens shares cache extract/dedupe/resolveToken path
    requirement: DLNK-01
    verification:
      - kind: unit
        ref: CURSOR_DEV=true nix develop -c pnpm backend:test_only
        status: pass
    human_judgment: false
  - id: D3
    description: Live notes query loads non-deleted notebook notes by id order
    requirement: DLNK-02
    verification:
      - kind: unit
        ref: CURSOR_DEV=true nix develop -c pnpm backend:test_only
        status: pass
    human_judgment: false

duration: 5min
completed: 2026-07-22
status: complete
---

# Phase 4 Plan 01: HealthRunContext viewer & resolve helpers Summary

**Lint caller is available as `HealthRunContext.viewer`; `unresolvedWikiLinkTokens` and live-notes query ready for dead-link rule (no new finding group yet).**

## Performance

- **Duration:** ~5 min
- **Started:** 2026-07-22T11:48:06Z
- **Completed:** 2026-07-22T11:52:00Z
- **Tasks:** 2/2
- **Files modified:** 7

## Accomplishments

- Required `User viewer` on `HealthRunContext`; controller passes `authorizationService.getCurrentUser()` after write-auth
- `WikiLinkResolver.unresolvedWikiLinkTokens` mirrors cache extract/dedupe/`resolveToken` (viewer-readable only; full inner strings)
- `NoteRepository.findLiveNotesByNotebookIdOrderByIdAsc` loads live notes only (`deletedAt IS NULL`, `ORDER BY id ASC`)
- Existing empty_folders / readme_only_folders tests green

## Task Commits

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1+2 | Viewer context + unresolved tokens + live-notes query | 1d9fb30752 | HealthRunContext, NotebookHealthController, WikiLinkResolver, NoteRepository, folder/runner tests |

Pre-commit formatting co-staged Task 2 files into the Task 1 commit; message amended to cover both.

## Deviations from Plan

None - plan executed as written (single commit for both tasks due to pre-commit auto-stage).

## Known Stubs

None.

## Self-Check: PASSED

- FOUND: HealthRunContext.viewer, unresolvedWikiLinkTokens, findLiveNotesByNotebookIdOrderByIdAsc
- FOUND: commit 1d9fb30752
- Backend suite: BUILD SUCCESSFUL
