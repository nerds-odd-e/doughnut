# Phase 2: Empty-folder findings - Context

**Gathered:** 2026-07-22
**Status:** Ready for planning

<domain>
## Phase Boundary

**Behavior.** Authorized notebook Health lint reports folders whose entire subtree has no notes, under the `empty_folders` findings group, without mutating the notebook.

Delivers EFOL-01 and EFOL-02 via an authorized lint API that plugs the empty-folder rule into the Phase 1 `HealthRule` / runner / report contract. No Health UI (Phase 5). No readme-only finding type yet (Phase 3). No fix/purge (Phase 7). No dead-link rule (Phase 4).

</domain>

<decisions>
## Implementation Decisions

### Empty-folder predicate (report scope)
- **D-01:** A folder is reported under `empty_folders` only when it is **fully empty**: entire subtree has **no non-deleted notes** (soft-deleted notes do not count as content) **and** the folder’s own `readmeContent` is blank (null, empty, or whitespace-only). Nested empty shells count (recursive).
- **D-02:** Note-empty folders with **non-blank** `readmeContent` are **not** listed under `empty_folders` in this phase — leave them unreported until Phase 3 adds `readme_only_folders`. Do not lump them into `empty_folders` as an interim.
- **D-03:** Soft-deleted notes never make a folder “non-empty.” Only live (`deletedAt IS NULL`) notes occupy a folder for this predicate.
- **D-04:** List **every** matching empty folder as its own `HealthFindingItem` (not only roots of maximal empty subtrees). Each item must set `folderId` and a short `label` (folder name). Optional `message` only if cheap; path-in-label is not required for v1.

### Rule metadata
- **D-05:** Use stable rule id `empty_folders` (`HealthRuleIds.EMPTY_FOLDERS`). Display title: human-readable e.g. “Empty folders”.
- **D-06:** Group severity = `warning`. Group `autoFixable` = `true` (reserved for Phase 7 bulk purge; lint remains report-only and must not delete).
- **D-07:** Implement as a Spring `@Component` / `@Service` `HealthRule` bean (e.g. `EmptyFolderHealthRule`) discovered by existing `HealthRuleRunner` `List<HealthRule>` injection. Runner must still never mutate.

### Authorized lint API
- **D-08:** Expose `POST /api/notebooks/{notebook}/health/lint` that authorizes with **`assertAuthorization(notebook)`** (owner/write — not read-only/bazaar/subscriber), calls `NotebookHealthService.lint`, returns `NotebookHealthLintReport`. Foreign and anonymous callers are rejected.
- **D-09:** Phase 2 lint request body is empty/minimal — no run-option fields required yet (`HealthRunContext` may stay empty). Do not invent fix options or user-defaults fields here.
- **D-10:** Prefer methods on existing `NotebookController` under `/health/lint` **or** a thin dedicated controller colocated with notebook routes — whichever matches existing controller cohesion; capability-named types only (no phase numbers in product code).
- **D-11:** After the endpoint exists, regenerate the TypeScript OpenAPI client (`pnpm generateTypeScript` / generate-api-client skill). No frontend Health UI in this phase.

### Verification
- **D-12:** Prove with focused backend tests: (1) unit/service tests for the empty-folder predicate (recursive emptiness, soft-delete ignored, blank vs non-blank readme excluded from this group); (2) MVC/API tests that authorized lint returns the empty-folder group/items and that unauthorized callers fail. No `@wip` E2E and no Health tab UI in this phase.

### Claude's Discretion
- Exact query strategy (load folders + note folder_ids once vs repository helpers) — follow research O(folders + notes) guidance; avoid N+1.
- Whether label is bare folder name vs light path hint — prefer folder name unless path is trivial from existing APIs.
- Exact controller class split (`NotebookController` vs `NotebookHealthController`) — pick the simplest cohesive option.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope and requirements
- `.planning/ROADMAP.md` — Phase 2 goal, Behavior type, success criteria (EFOL-01/02), stop-safe value
- `.planning/REQUIREMENTS.md` — EFOL-01, EFOL-02 (this phase); EFOL-03 deferred to Phase 3
- `.planning/PROJECT.md` — recursive empty definition; readme naming; lint ≠ fix; Health later

### Prior phase decisions
- `.planning/phases/01-health-lint-contract/01-CONTEXT.md` — DTO shapes, `HealthRule` / runner, rule ids, no findings persistence, Phase 2 owns controller + OpenAPI regen

### Research (predicate and pitfalls)
- `.planning/research/ARCHITECTURE.md` — backend-authoritative lint; `POST .../health/lint`; `EmptyFolderHealthRule`; empty detection data flow
- `.planning/research/PITFALLS.md` — recursive emptiness (not leaf-only); soft-deleted notes; do not use dissolve; auth on health endpoints; separate readme-bearing folders from fully empty
- `.planning/research/SUMMARY.md` — milestone deliverables and ordering

### Codebase maps / contracts
- `.planning/codebase/ARCHITECTURE.md` — thin controllers + `AuthorizationService`
- `.planning/codebase/STRUCTURE.md` — package layout under `com.odde.doughnut`
- `.planning/codebase/CONVENTIONS.md` — capability naming; no phase numbers in product artifacts

### Implemented Phase 1 contract (must extend, not replace)
- `backend/src/main/java/com/odde/doughnut/services/health/HealthRule.java`
- `backend/src/main/java/com/odde/doughnut/services/health/HealthRuleRunner.java`
- `backend/src/main/java/com/odde/doughnut/services/health/HealthRuleIds.java`
- `backend/src/main/java/com/odde/doughnut/services/NotebookHealthService.java`
- `backend/src/main/java/com/odde/doughnut/controllers/dto/NotebookHealthLintReport.java`
- `backend/src/main/java/com/odde/doughnut/controllers/dto/HealthFindingGroup.java`
- `backend/src/main/java/com/odde/doughnut/controllers/dto/HealthFindingItem.java`

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- Phase 1 `HealthRule` / `HealthRuleRunner` / `NotebookHealthService.lint` — plug in one rule bean; no second runner
- `HealthRuleIds.EMPTY_FOLDERS` — already reserved
- `HealthFindingGroup` / `HealthFindingItem` — set `folderId` + `label` for each empty folder; flat `items` (no `children` needed for this rule)
- `Folder.readmeContent`, folder tree via `parentFolder` — predicate inputs
- `AuthorizationService.assertAuthorization(notebook)` — same write gate as folder mutate endpoints
- `NotebookController` patterns for `/{notebook}/...` path-variable binding

### Established Patterns
- Controllers authorize then delegate to services; return OpenAPI DTOs when wire shape ≠ entity
- Soft-delete awareness via `deletedAt` on notes elsewhere in repos/queries
- Domain language: **readme** / `readmeContent` only (never `index` / `indexContent` in new Health code)

### Integration Points
- New `EmptyFolderHealthRule` bean → injected into `HealthRuleRunner`
- New `POST .../health/lint` → `NotebookHealthService.lint(notebook, context)`
- OpenAPI regen → generated SDK for later Phase 5 UI (no UI consumers required this phase)
- Phase 3 will add a second rule for readme-only; Phase 7 will purge only folders matching this phase’s fully-empty predicate

</code_context>

<specifics>
## Specific Ideas

- Research empty detection: load all folders for notebook → parent→children map → for each folder, if no live note has `folder_id` in that subtree **and** folder readme is blank → include in findings.
- Endpoint path from research: `POST /api/notebooks/{notebook}/health/lint`.
- `--auto` discussion locked: fully empty only (exclude readme-bearing); list every empty folder; write auth; `warning` + `autoFixable=true` on the group; regenerate TS client; backend tests only.

</specifics>

<deferred>
## Deferred Ideas

- Readme-only finding type / `readme_only_folders` rule — Phase 3
- Dead wiki-link rule — Phase 4
- Health tab UI / Run button — Phase 5
- User-level lint defaults — Phase 6
- Bulk empty-folder purge / fix endpoint — Phase 7 (must reuse this phase’s fully-empty predicate; never dissolve)
- Listing only maximal empty roots (instead of every empty folder) — rejected for v1; revisit only if UI noise becomes a problem after Phase 5
- Read-authorization for lint (bazaar/subscriber audit) — out of v1; Health is owner write tool

None — discussion stayed within phase scope

</deferred>

---

*Phase: 2-Empty-folder findings*
*Context gathered: 2026-07-22*
