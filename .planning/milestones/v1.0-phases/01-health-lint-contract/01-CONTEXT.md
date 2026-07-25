# Phase 1: Health lint contract - Context

**Gathered:** 2026-07-22
**Status:** Ready for planning

<domain>
## Phase Boundary

**Structure only.** Deliver the shared Health lint contract — finding DTO shapes, `HealthRule` interface, and `HealthRuleRunner` / `NotebookHealthService` skeleton — so Phase 2 can plug in the empty-folder rule and return a typed lint report without inventing a second model.

No user-facing Health behavior. No HTTP lint/fix endpoints. No concrete rules. Existing product behavior must remain unchanged (existing unit and targeted E2E tests still pass).

</domain>

<decisions>
## Implementation Decisions

### Phase 1 delivery surface
- **D-01:** Backend-only contract in this phase: OpenAPI-ready findings DTOs under `controllers/dto/`, rule interface + runner under `services/health/`, and a thin `NotebookHealthService` orchestration skeleton under `services/`. No controller routes, no frontend, no OpenAPI regen required until Phase 2 exposes an endpoint.
- **D-02:** Runner with zero registered rules returns an empty `NotebookHealthLintReport` (`groups: []`). That is sufficient Phase 1 verification that the contract wires together.
- **D-03:** Fix request/result DTOs and fix applicator are **out of Phase 1** (Phase 7). Lint request DTO may be deferred until Phase 2 if the skeleton does not yet need run options; do not invent option fields beyond what Phase 2 immediately needs.

### Report and finding shapes
- **D-04:** Top-level report is `NotebookHealthLintReport` with `groups: HealthFindingGroup[]` — one group per rule, shaped for expandable UI later (no frontend in this phase).
- **D-05:** `HealthFindingGroup` includes stable `ruleId`, human `title`, reserved `severity`, reserved `autoFixable`, optional `items` (leaf findings), and optional `children` (nested groups — e.g. Phase 4 dead links by note). Prefer this recursive group shape over a flat findings list so Phase 4 does not rework the shared model.
- **D-06:** `HealthFindingItem` carries optional target identity fields sufficient for later navigation: `folderId`, `noteId`, `label`, optional `message`, optional `wikiLinkToken` (for dead links). Reserve `autoFixable` on items only if cheap; group/rule-level `autoFixable` is enough for v1 gating.
- **D-07:** Severity vocabulary is `error` | `warning` | `info` (API enum / string constants). No severity-settings UI. Structural decay defaults to `warning` or `error` at rule level when rules are added later; Phase 1 only reserves the field.
- **D-08:** Findings are ephemeral DTOs only — **no findings table**, no SARIF, no history persistence.

### Rule interface and registry
- **D-09:** `HealthRule` (in `services/health/`) exposes at least: `id()`, display title (method or constant), `severity()`, `autoFixable()`, and `evaluate(Notebook notebook, HealthRunContext ctx) → HealthFindingGroup`. Aligns with LM Wiki / research `LintRule` pattern without importing external lint frameworks.
- **D-10:** Rule ids are stable snake_case strings reserved for later rules: `empty_folders`, `readme_only_folders` (or equivalent for Phase 3), `dead_wiki_links`. Phase 1 does not implement those rules; ids are documented in the contract for planner consistency.
- **D-11:** `HealthRuleRunner` discovers rules via Spring `List<HealthRule>` injection (ordered beans). Phase 1 registers **zero** rule beans. Runner never mutates notebook data.
- **D-12:** `HealthRunContext` is a minimal placeholder for future run-scoped options (e.g. which rules / fix prefs). Keep it empty or trivially extensible; do not implement user defaults or fix options here.

### Package layout and naming
- **D-13:** Package layout follows research: `services/health/` for `HealthRule`, `HealthRuleRunner`, `HealthRunContext`; `NotebookHealthService` at `services/NotebookHealthService.java`; wire DTOs in `controllers/dto/` (`NotebookHealthLintReport`, `HealthFindingGroup`, `HealthFindingItem`, severity type as appropriate).
- **D-14:** Domain language is **readme** / `readmeContent` — never `index` / `indexContent` in new Health code, tests, or docs for this milestone (product rename already landed).

### Verification
- **D-15:** Prove the contract with focused backend unit tests (runner with no rules → empty report; DTO/group construction). Do not add `@wip` E2E. Existing product tests remain green.
- **D-16:** Do not regenerate the TypeScript API client in Phase 1 (no new endpoints). Phase 2 owns controller + OpenAPI exposure + `pnpm generateTypeScript`.

### Claude's Discretion
- Exact Java type styles (record vs class vs enum) and OpenAPI annotation density — follow existing `controllers/dto/` patterns.
- Whether `NotebookHealthService.lint` is public in Phase 1 or package-visible via runner tests — either is fine if Phase 2 can call it without rework.
- Stable ordering mechanism (`@Order` vs list order) — pick the simplest Spring-idiomatic approach.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope and requirements
- `.planning/ROADMAP.md` — Phase 1 goal, Structure type, success criteria; enables Phase 2 only
- `.planning/REQUIREMENTS.md` — v1 requirement set (Phase 1 maps to none directly; contract must support EFOL-*, DLNK-*, HLTH-*, AFIX-*)
- `.planning/PROJECT.md` — Key decisions: Health tab later, report≠fix, readme naming, per-user defaults later

### Research (contract shape)
- `.planning/research/SUMMARY.md` — Phase 1 deliverables: `HealthRule`, `HealthRuleRunner`, OpenAPI findings DTOs, `services/health/`
- `.planning/research/ARCHITECTURE.md` — Nested report DTO example, `HealthRule` interface sketch, package layout
- `.planning/research/STACK.md` — Rule registry / severity / `autoFixable` pattern (no kb-lint/SARIF dependencies)
- `.planning/research/PITFALLS.md` — No findings persistence; reserve severity without settings console; do not use dissolve for fix (later)

### Codebase maps
- `.planning/codebase/ARCHITECTURE.md` — Controllers → services → entities; DTOs in `controllers/dto/`
- `.planning/codebase/STRUCTURE.md` — Backend layout under `com.odde.doughnut`
- `.planning/codebase/CONVENTIONS.md` — Capability naming, Java test naming, no phase numbers in product code

### External inspiration (pattern only — do not depend on)
- https://llmwikis.org/operations/lint/ — structural lint tier, severity, propose removals separately
- https://github.com/Pratiyush/llm-wiki — `LintRule` registry pattern (`auto_fixable`, `run` → issues)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `controllers/dto/` — Existing Jackson/OpenAPI DTO patterns for new `NotebookHealthLintReport` / group / item types
- `services/book/`, `services/focusContext/`, `services/search/` — Subpackage cohesion precedent for `services/health/`
- Spring `@Service` + constructor injection — Standard for `HealthRuleRunner` and `NotebookHealthService`

### Established Patterns
- Controllers stay thin and authorize via `AuthorizationService` — **not introduced in Phase 1**; Phase 2 adds authorized lint path
- Prefer entities/DTOs as API bodies; introduce DTOs when wire shape differs from persistence — Health findings are DTO-only (ephemeral)
- Capability-named types (`NotebookHealth*`, `HealthRule`) — never phase-numbered product artifacts

### Integration Points
- Phase 2 will add empty-folder rule bean + authorized controller method calling `NotebookHealthService.lint`
- Phase 4 will use `children` on groups for dead links by note and `wikiLinkToken` on items
- Phase 5 will consume the same report shape in Vue via generated SDK
- Future fix path (Phase 7) stays separate from lint; do not couple runner to mutation

</code_context>

<specifics>
## Specific Ideas

- Follow research conceptual types closely: `NotebookHealthLintReport.groups[]`, `HealthFindingGroup` with `items` and `children`, item fields `folderId` / `noteId` / `label` / `wikiLinkToken`.
- Rule metadata mirrors LM Wiki: `id`, `severity`, `autoFixable`, `evaluate` → group.
- Domain rename: always `readme` / `readmeContent` (research docs may still say `indexContent` in places — treat those as stale).

</specifics>

<deferred>
## Deferred Ideas

- Concrete `EmptyFolderHealthRule` and authorized lint API — Phase 2
- Readme-only folder finding type — Phase 3
- Dead wiki-link rule and note-nested groups — Phase 4
- Health tab UI / Run — Phase 5
- User-level defaults — Phase 6
- Fix DTOs, purge applicator, controller fix endpoint — Phase 7
- Severity-settings console, SARIF, findings history, LLM lint — out of milestone

None — discussion stayed within phase scope

</deferred>

---

*Phase: 1-Health lint contract*
*Context gathered: 2026-07-22*
