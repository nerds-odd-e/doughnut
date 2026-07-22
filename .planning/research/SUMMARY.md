# Project Research Summary

**Project:** Notebook Lint & Auto-Fix (Doughnut Health tab)
**Domain:** Notebook / wiki content-health lint + optional gated auto-fix (PKM, subsequent milestone on existing Doughnut)
**Researched:** 2026-07-22
**Confidence:** HIGH

## Executive Summary

Notebook Health is Doughnut’s **mechanical immune system**: a notebook-scoped, backend-authoritative audit that reports structural decay (recursive empty folders, dead `[[wiki links]]` in body and frontmatter) and offers **one** opt-in fix—bulk removal of empty folder trees. Experts in this space (LM Wiki / llmwiki lint, kb-lint, Obsidian Broken Links / Vault Inspector) share one shape: a small **rule registry**, a **report-only** pass, and **consentful** mechanical fixes. Semantic/LLM lint is a later tier; v1 must not pay for it.

**Do not introduce a new stack.** Implement an in-process Java `HealthRule` registry on Spring Boot, findings DTOs over OpenAPI, and a Vue **Health** tab beside Index/Settings. Reuse `NoteContentMarkdown` + `WikiLinkResolver` for dead links and a **dedicated** empty-folder purge (never folder dissolve). User defaults live as columns on `User`, same pattern as `spaceIntervals`. Lint and fix are **separate** API operations; Fix is disabled until the bulk “remove empty folders” option is selected for this run.

Primary risks are **trust and data loss**: silent deletes, wrong emptiness (leaf-only, soft-delete, or ignoring `indexContent`), dissolve-as-cleanup, a second wiki resolver / cache-as-truth, and auth-skipped Health endpoints. Mitigate by shipping report-only paths first, binding dead links to existing resolve semantics, re-checking emptiness in the fix transaction, and gating all mutation behind explicit opt-in.

## Key Findings

### Recommended Stack

No new npm/Maven packages for v1. The “stack” is the ecosystem **pattern** (registry, severity, `autoFixable`, report ≠ fix) ported into Doughnut’s current layers. Details: [STACK.md](./STACK.md).

**Core technologies:**
- **Spring Boot 4.1 / Java 25** — Host `NotebookHealthService` + rule classes; authoritative notebook graph lives in MySQL
- **Spring Data JPA / MySQL 8.4** — Folder tree + note placement for recursive empty-folder scans
- **Existing wiki plumbing** — `WikiLinkMarkdown`, `NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder`, `WikiLinkResolver` (do not reimplement on the frontend)
- **Vue 3.5 + DaisyUI / Tailwind** — Health tab on `WorkspaceIndexSettingsTabs`; expandable nested findings
- **Generated OpenAPI SDK** — Lint/fix DTOs → `pnpm generateTypeScript` → `apiCallWithLoading`
- **Flyway + User columns** — Per-user health defaults (two booleans); no prefs microservice

**Avoid:** kb-lint/ESLint/markdownlint/SARIF/LLM as product dependencies; client-side full-notebook lint; async job queue for v1; findings history table.

### Expected Features

v1 is a complete Run → review → optional Fix loop for two mechanical rules. Details: [FEATURES.md](./FEATURES.md).

**Must have (table stakes):**
- Health tab on notebook settings (no `/health` route or dialog)
- Explicit **Run lint** (on-demand; never silent mutate on open)
- **Empty folders** — recursive: entire subtree has no non-deleted notes
- **Dead wiki links** — body + frontmatter/properties; report-only; same resolve semantics as editor
- Nested expandable findings (by rule; dead links by note); navigable to note/folder
- **Lint ≠ fix**; Fix active only when bulk “remove empty folders” is selected
- User-level defaults for lint/auto-fix options (across notebooks)

**Should have (competitive):**
- Notebook-scoped Health inside product UI (not a vault plugin/CLI)
- Live dead-link semantics (aliases, qualified `Notebook:Title`, properties)
- Single bulk structural fix—not a fix menu or per-folder multi-select
- Mechanical-only, always-available hygiene (no LLM cost/latency)

**Defer (v1.x / v2+):**
- Dead-link focus/repair assist, health summary chip, empty-note rule, orphan report, per-notebook overrides
- LLM semantic lint, full rule-registry settings UI, scheduled/CLI/MCP health, lint history
- Auto-fix for dead links; silent/scheduled auto-fix — **anti-features for v1**

### Architecture Approach

Lint is a **backend-authoritative audit**; the Vue Health panel is a thin run/review/fix surface. Findings are ephemeral DTOs (no findings table). User defaults seed run options; each request sends run-scoped options explicitly. Details: [ARCHITECTURE.md](./ARCHITECTURE.md).

**Major components:**
1. **NotebookHealthPanel + Health tab** — Run options, expandable findings, Fix gating, save defaults
2. **NotebookHealthController** — Authorized `POST .../health/lint` and `POST .../health/fix`
3. **NotebookHealthService + HealthRuleRunner** — Orchestrate rules; never mutate on lint
4. **EmptyFolderHealthRule** — Recursive “no notes in subtree” (+ fix eligibility: no meaningful `indexContent`)
5. **DeadWikiLinkHealthRule** — Extract inners → `WikiLinkResolver`; group by note
6. **EmptyFolderBulkFixApplicator** — Dedicated empty-tree purge (not dissolve); TOCTOU re-check
7. **User / UserDTO defaults** — Prefill options; PATCH via existing user update path

**Critical architecture correction vs naive reuse:** Existing `DELETE .../folders/{folder}` is **dissolve** (promotes children). Health fix must **purge** empty trees only—new applicator path.

### Critical Pitfalls

Top risks from [PITFALLS.md](./PITFALLS.md):

1. **Silent or one-click destructive deletes** — Lint never mutates; Fix only when auto-fix path + bulk remove option selected; list targets before apply
2. **Wrong empty-folder definition** — Must be recursive subtree (not leaf-only / direct-notes-only); non-deleted notes only; fix must not leave parent shells
3. **Ignoring `indexContent`** — Note-empty folders with index markdown must not be auto-deleted; report separately or exclude from fix eligibility
4. **Dissolve as cleanup** — Promotes children; leaves empty shells at parent—dedicated purge only
5. **Second resolver / cache-as-truth** — Use `NoteContentMarkdown` + `WikiLinkResolver`; do not treat `note_wiki_title_cache` absence as dead without live resolve
6. **Missing authorization** — Every Health endpoint `assertAuthorization`; fix requires edit/owner rights; controller tests for foreign/anon users

## Implications for Roadmap

Suggested phase structure is **stop-safe**: report-only value lands early; mutation last; Structure only when it enables the immediate next Behavior.

### Phase 1: Health lint contract (Structure)
**Rationale:** Shared finding shape and rule interface unblock both rules and the UI without inventing a second model later.
**Delivers:** `HealthRule` interface, `HealthRuleRunner` skeleton, OpenAPI findings DTOs (`NotebookHealthLintReport`, groups/items), package layout under `services/health/`.
**Addresses:** Research-informed registry model (ids, severity fields reserved, `autoFixable`) without settings UI.
**Avoids:** Overbuilt rule-registry console; SARIF; findings persistence.

### Phase 2: Empty-folder findings (Behavior)
**Rationale:** Highest-signal Doughnut-specific structural rot; fix depends on a trustworthy predicate—prove it report-only first.
**Delivers:** Recursive empty-folder rule (subtree has no non-deleted notes); note-empty-with-`indexContent` not fix-eligible (report as non-fixable or separate cue); exposed via authorized lint path (at least service + controller tests).
**Addresses:** Empty folders (table stakes); recursive definition.
**Avoids:** Leaf-only emptiness; soft-delete miscount; premature delete.

### Phase 3: Dead-link findings (Behavior)
**Rationale:** Completes the single lint pass users expect; independent of empty folders but same report shape.
**Delivers:** Dead wiki-link rule (body + frontmatter) via shared extract + `WikiLinkResolver`; findings grouped by note; aliases, qualified links, display `|` targets match editor.
**Addresses:** Dead wiki links (table stakes); live resolve semantics differentiator.
**Avoids:** Second regex resolver; cache-only phantoms; auto-fix for links.

### Phase 4: Health tab + Run (Behavior)
**Rationale:** First full user-visible loop: open Health → Run → expandable nested findings (no route/dialog).
**Delivers:** Extend `WorkspaceIndexSettingsTabs` with `health`; `NotebookHealthPanel` / findings UI; `POST .../health/lint` wired through generated SDK + `apiCallWithLoading`; empty state / “no issues”; navigate to note/folder from findings.
**Addresses:** Health surface, Run lint, navigable nested findings.
**Avoids:** Dedicated `/health` route; client-side scan of loaded notes only; auth footgun on lint API.

### Phase 5: User-level defaults (Behavior)
**Rationale:** Options exist on the panel; defaults only prefill—do not enable silent fix.
**Delivers:** Flyway columns on `User` + `UserDTO` / profile PATCH; Health panel loads defaults into run options; save-as-defaults.
**Addresses:** Persist preferences across notebooks (table stakes).
**Avoids:** Per-notebook defaults; defaults that auto-apply deletes on load.

### Phase 6: Gated bulk remove empty folders (Behavior)
**Rationale:** Mutation only after report, predicate, preview, and UI gate are proven.
**Delivers:** `POST .../health/fix` requiring `removeEmptyFolders: true`; `EmptyFolderBulkFixApplicator` (purge empty trees, children-before-parents or set delete, TOCTOU re-check); Fix button enabled only when option selected; refresh lint after fix.
**Addresses:** Optional auto-fix; Fix gated on bulk option.
**Avoids:** Silent deletes; dissolve misuse; deleting index-bearing folders; fix without edit authorization.

### Phase Ordering Rationale

- **Report before mutate** — Ecosystem invariant and stop-safe: stopping after Phase 4 still delivers full audit value.
- **Empty-folder rule before fix** — Fix reuses the same emptiness (+ `indexContent`) predicate.
- **Both rules before UI polish is optional** — One Run should return both groups; implement rules before (or as part of) the full lint API, then UI.
- **Defaults after options exist** — Prefill only; never conflate defaults with apply.
- **Auth on first API-bearing phase** — Lint endpoint must not ship permit-all.
- **No LLM / no severity-settings console in this milestone** — Internal enums/ruleIds only.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 2 (empty folders):** Exact report shape for note-empty-but-has-`indexContent` (separate group vs non-fixable items); delete order and whether to list every empty folder vs empty-tree roots only.
- **Phase 6 (fix applicator):** Dedicated purge vs leaf-dissolve loop; interaction with soft-deleted title conflicts—confirm against live `Folder` / dissolve tests.

Phases with standard patterns (skip research-phase):
- **Phase 1 (contract):** ESLint/LM Wiki registry pattern is well documented; map to Java interfaces.
- **Phase 3 (dead links):** Bind to existing resolver tests and editor `dead-link` semantics.
- **Phase 4 (Health tab):** Existing tab shell + DaisyUI expandables + OpenAPI client regen.
- **Phase 5 (user defaults):** Copy `spaceIntervals` / `dailyAssimilationCount` column pattern.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Ecosystem tools are disk-wiki CLIs; Doughnut mapping validated against in-repo stack/wiki/folder code |
| Features | HIGH | Locked PROJECT.md intent aligns with LM Wiki structural tier + kb-lint report/fix split |
| Architecture | HIGH | Clear controller/service/UI placement; dissolve vs purge distinction confirmed in codebase |
| Pitfalls | HIGH | Auth, cache coherence, and folder lifecycle are known Doughnut footguns plus ecosystem delete-safety patterns |

**Overall confidence:** HIGH

### Gaps to Address

- **Large-notebook latency:** Sync request-scoped lint is right for v1; unmeasured at 10k+ notes—design DTOs as lists (paginatable later); add loading UX; do not add jobs until timeouts appear.
- **Severity vocabulary:** Prefer `error` / `warning` / `info` in API; UI can group by rule without a full severity taxonomy.
- **`indexContent` threshold:** Define “meaningful” index (blank/null vs whitespace-only) in Phase 2 planning with fixtures.
- **STACK vs ARCHITECTURE on folder DELETE:** Prefer ARCHITECTURE/PITFALLS—**do not** call dissolve for Health fix; implement dedicated empty-tree purge.
- **Cross-notebook / unreadable targets:** Match editor: unresolved (including auth-omitted) reports as dead; optional `forbidden` severity is out of v1.

## Sources

### Primary (HIGH confidence)
- [LM Wiki — Lint: Keep the Knowledge Graph Healthy](https://llmwikis.org/operations/lint/) — two-tier lint, severity, propose removals separately
- [llmwiki lint registry](https://github.com/Pratiyush/llm-wiki/blob/06c30e2b0c9018b11463b4fa37de0d75248cde5c/llmwiki/lint/__init__.py) — `LintRule`, `auto_fixable`, registry
- [kb-lint](https://github.com/SingggggYee/kb-lint) — severity / auto-fixable matrix; links not auto-fixable
- Doughnut `.planning/PROJECT.md` — locked v1 scope and decisions
- Doughnut codebase — `WikiLinkResolver`, `NoteContentMarkdown`, `WikiTitleCacheService`, `Folder`/`indexContent`, `dissolveFolder`, `WorkspaceIndexSettingsTabs`, `User` preference columns
- Doughnut `.planning/codebase/` — STACK, ARCHITECTURE, CONCERNS (auth, cache coherence)

### Secondary (MEDIUM confidence)
- [Obsidian Broken Links](https://www.obsidianstats.com/plugins/broken-links) / [Vault Inspector](https://github.com/rogerdigital/vault-inspector) — nested reports, navigate-to-note, read-only default
- Obsidian File Cleaner Redux patterns — preview before delete; recursive empty folders
- [Karpathy llm-wiki gist](https://gist.github.com/karpathy/442a6bf555914893e9891c11519de94f) — lint as immune system (semantic-leaning; use for inspiration, not v1 scope)
- [SARIF 2.1](https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html) — deferred interchange format

### Tertiary (LOW confidence)
- Community wiki-lint false-positive reports (qualified paths, `\|` escapes) — inform resolver-binding tests, not product scope

---
*Research completed: 2026-07-22*
*Ready for roadmap: yes*
