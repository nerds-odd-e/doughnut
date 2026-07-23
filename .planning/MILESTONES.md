# Milestones

## v1.0 Notebook Lint & Auto-Fix (Shipped: 2026-07-23)

**Phases completed:** 7 phases, 13 plans, 26 tasks

**Key accomplishments:**

- OpenAPI-ready nested findings DTOs (`NotebookHealthLintReport` → recursive `HealthFindingGroup` → `HealthFindingItem` + `HealthSeverity`) with a unit test proving items+children construction.
- Spring `List<HealthRule>` runner and public `NotebookHealthService.lint` with zero rule beans — empty registry returns `groups: []` (SC-2/SC-3); full backend suite green (SC-1).
- Spring `EmptyFolderHealthRule` reports fully empty folders under `empty_folders` via O(folders+notes) live occupancy and blank-readme checks
- Authorized `POST /api/notebooks/{notebook}/health/lint` returns empty-folder findings without mutating the notebook, with regenerated OpenAPI TS client
- `readme_only_folders` HealthRule reports note-empty folders with non-blank own readme, mutually exclusive from `empty_folders`, with `autoFixable=false`
- Lint caller is available as `HealthRunContext.viewer`; `unresolvedWikiLinkTokens` and live-notes query ready for dead-link rule (no new finding group yet).
- Lint reports nested `dead_wiki_links` for body and frontmatter unresolved tokens (viewer-readable resolve), report-only with `autoFixable=false`.
- Notebook settings gain a Health tab with idle → Run lint → on-tab report groups; folder settings stay Readme|Settings only.
- Extracted wire-shape NotebookHealthFindings plus green capability-named notebook_health E2E (and Health landmarks), completing Phase 5 Behavior.
- Flyway + User/UserDTO/updateUser persist `healthRemoveEmptyFoldersDefault` (default false) with green controller round-trip and regenerated OpenAPI client.
- Health panel prefills Remove empty folders from injected currentUser and explicit Save as defaults persists full UserDTO preferences across notebooks without lint or mutation.
- Authorized `POST .../health/fix` hard-deletes CASCADE-safe fully empty folder trees when `removeEmptyFolders` is true, never dissolving or wiping readme-only descendants.
- Health Fix is a secondary action-bar control that posts `removeEmptyFolders: true`, refreshes the sidebar, auto re-lints, and is proven by targeted `notebook_health` E2E.

**Closeout type:** override_closeout
**Known verification overrides:** 6 (see STATE.md Deferred Items) — phases 1, 3, 4, 5, 6, 7 shipped (implementation complete + `notebook_health` E2E) without a formal GSD `/gsd-verify-work` report; only phase 2 was formally verified. Accepted as override at archive time to retire the milestone cleanly before starting a separate project.

---
