# Phase 3: Readme-only folder findings - Context

**Gathered:** 2026-07-22
**Status:** Ready for planning

<domain>
## Phase Boundary

**Behavior.** Authorized notebook Health lint reports note-empty folders that still have non-blank **readme** (`readmeContent`) under a **separate** finding type/group (`readme_only_folders`), distinct from fully empty folders (`empty_folders`).

Delivers EFOL-03. Lint remains report-only (no deletes or mutations). No Health UI (Phase 5). No dead-link rule (Phase 4). No fix/purge (Phase 7) — but this phase’s finding type is the fix-eligibility boundary Phase 7 must respect.

</domain>

<decisions>
## Implementation Decisions

### Readme-only predicate (EFOL-03)
- **D-01:** A folder is reported under `readme_only_folders` when its **entire subtree has no live notes** (same recursive emptiness as Phase 2: soft-deleted notes do not count) **and** the folder’s **own** `readmeContent` is **non-blank** (not null and not `String.isBlank()`).
- **D-02:** Predicate uses the folder’s **own** readme only — ancestor or descendant readmes do not reclassify this folder. A note-empty child with blank own readme stays under `empty_folders` even if a parent has a readme.
- **D-03:** Mutual exclusion with `empty_folders` must hold: fully empty (note-empty + blank readme) → only `empty_folders`; note-empty + non-blank readme → only `readme_only_folders`; folders with any live note in the subtree → neither group. Do not lump readme-only into `empty_folders`.
- **D-04:** List **every** matching readme-only folder as its own `HealthFindingItem` with `folderId` and `label` = bare folder name (same item shape as Phase 2). No readme preview / path-in-label required for v1.

### Rule metadata
- **D-05:** Use stable rule id `readme_only_folders` (`HealthRuleIds.README_ONLY_FOLDERS`). Display title: “Readme-only folders”.
- **D-06:** Group severity = `warning` (same structural tier as empty folders).
- **D-07:** Group `autoFixable` = **`false`** — readme-only folders must never be deleted by Phase 7 bulk purge; reserve this flag now so the fix-eligibility boundary is visible in the report.
- **D-08:** Always emit the `readme_only_folders` group (metadata + `items`, possibly empty), consistent with Phase 2’s always-emit `empty_folders` group.

### Implementation surface
- **D-09:** Add a Spring `HealthRule` bean (e.g. `ReadmeOnlyFolderHealthRule`) discovered by existing `HealthRuleRunner`. Reuse the existing authorized `POST .../health/lint` — no new endpoint, no request-body options, no frontend.
- **D-10:** Prefer a small shared helper for the shared “subtree has live notes” / folder-load scan so `EmptyFolderHealthRule` and the new rule cannot drift on emptiness. Mutual-exclusion tests are required either way. Avoid speculative structure beyond that shared scan.
- **D-11:** No OpenAPI / TypeScript client regen unless a wire schema change appears (report DTOs already support multiple groups). Verify lint response can include both groups via backend tests.
- **D-12:** Prove with focused backend tests: readme-only inclusion; blank/whitespace readme stays in `empty_folders` not this group; live note in subtree excludes; soft-delete ignored; both groups can appear in one lint report; no mutation. No `@wip` E2E / Health UI in this phase.

### Claude's Discretion
- Exact shared-helper name/package (`services/health/` package-private vs package-visible util) — pick the smallest cohesive option.
- Whether to refactor `EmptyFolderHealthRule` in the same change set vs thin wrapper — either is fine if mutual exclusion stays green and duplication is not left to drift.
- Optional `message` on items — omit unless trivially useful; default is label-only.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope and requirements
- `.planning/ROADMAP.md` — Phase 3 goal, Behavior type, success criteria (EFOL-03), stop-safe value after Phase 3
- `.planning/REQUIREMENTS.md` — EFOL-03 (this phase); AFIX-04 (later: purge must not delete readme-only)
- `.planning/PROJECT.md` — note-empty + non-blank readme = separate finding; domain name `readme` / `readmeContent`

### Prior phase decisions
- `.planning/phases/01-health-lint-contract/01-CONTEXT.md` — DTO shapes, `HealthRule` / runner, reserved rule id `readme_only_folders`
- `.planning/phases/02-empty-folder-findings/02-CONTEXT.md` — fully-empty predicate, blank-readme definition, write-auth lint API, list-every-folder, always-emit group

### Research (predicate and pitfalls)
- `.planning/research/SUMMARY.md` — note-empty-with-readme not fix-eligible; blank/null vs whitespace threshold
- `.planning/research/ARCHITECTURE.md` — backend-authoritative lint; empty-folder detection data flow
- `.planning/research/PITFALLS.md` — recursive emptiness; soft-deleted notes; separate readme-bearing folders from fully empty; do not use dissolve

### Implemented contract to extend (must not replace)
- `backend/src/main/java/com/odde/doughnut/services/health/HealthRule.java`
- `backend/src/main/java/com/odde/doughnut/services/health/HealthRuleRunner.java`
- `backend/src/main/java/com/odde/doughnut/services/health/HealthRuleIds.java` — `README_ONLY_FOLDERS` already reserved
- `backend/src/main/java/com/odde/doughnut/services/health/EmptyFolderHealthRule.java` — blank-readme gate + recursive emptiness to mirror/share
- `backend/src/main/java/com/odde/doughnut/services/NotebookHealthService.java`
- `backend/src/main/java/com/odde/doughnut/controllers/NotebookHealthController.java` — existing lint endpoint
- `backend/src/main/java/com/odde/doughnut/controllers/dto/HealthFindingGroup.java`
- `backend/src/main/java/com/odde/doughnut/controllers/dto/HealthFindingItem.java`

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `EmptyFolderHealthRule` — recursive `subtreeHasLiveNotes` + `isBlankReadme`; invert blank check for readme-only items
- `HealthRuleIds.README_ONLY_FOLDERS` — already reserved (`readme_only_folders`)
- `NotebookHealthController` + `NotebookHealthService.lint` — no new HTTP surface
- `HealthFindingGroup` / `HealthFindingItem` — flat `items` with `folderId` + `label` (no `children` for this rule)
- `FolderRepository.findByNotebookIdOrderByIdAsc` + `NoteRepository.findLiveNoteFolderIdsByNotebookId` — same O(folders+notes) inputs

### Established Patterns
- Spring `@Service` `HealthRule` beans discovered via `List<HealthRule>` injection
- Always emit group with `ruleId`, `title`, `severity`, `autoFixable`, `items`
- Domain language: **readme** / `readmeContent` only (never `index` / `indexContent`)
- Lint is `@Transactional(readOnly = true)` / report-only

### Integration Points
- New `ReadmeOnlyFolderHealthRule` bean → injected beside `EmptyFolderHealthRule`
- Existing `POST /api/notebooks/{notebook}/health/lint` returns both groups in one report
- Phase 7 purge must delete only `empty_folders` matches (`autoFixable=true`); never `readme_only_folders`
- Phase 5 UI will show both expandable groups from the same report shape

</code_context>

<specifics>
## Specific Ideas

- `--auto` discussion locked: own non-blank readme + recursive note-empty; `autoFixable=false`; always emit group; folder name label only; share emptiness scan when practical; no OpenAPI regen unless schema changes; backend tests only.
- Blank threshold already locked in Phase 2: `null` or `String.isBlank()` (whitespace-only → empty_folders, not readme-only).
- Research still says `indexContent` in places — treat as stale; use `readmeContent`.

</specifics>

<deferred>
## Deferred Ideas

- Health tab UI / Run — Phase 5
- Dead wiki-link rule — Phase 4
- User-level lint defaults — Phase 6
- Bulk empty-folder purge — Phase 7 (must never delete `readme_only_folders` items)
- Readme preview in finding `message` — rejected for v1; revisit only if UI needs a cue after Phase 5
- Ancestor-readme inheritance for classification — rejected; own-readme only

None — discussion stayed within phase scope

</deferred>

---

*Phase: 3-Readme-only folder findings*
*Context gathered: 2026-07-22*
