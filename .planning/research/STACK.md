# Stack Research

**Domain:** Notebook / wiki content-health lint + optional auto-fix (PKM)
**Researched:** 2026-07-22
**Confidence:** HIGH
**Scope:** Subsequent milestone on existing Doughnut (Vue SPA + Spring Boot 4.1 + MySQL). Do **not** introduce a new app stack — adopt the 2025–2026 **pattern** for KB lint (rule registry, findings report, gated fix) on Doughnut’s current layers.

## Verdict (one screen)

| Layer | Recommendation | Confidence |
|-------|----------------|------------|
| New frameworks/libs | **None** for v1 | HIGH |
| Lint engine | **In-process Java rule registry** on Spring Boot (LM Wiki / ESLint-style) | HIGH |
| Wiki integrity | **Reuse** `WikiLinkMarkdown`, `NoteContentMarkdown`, `WikiLinkResolver` | HIGH |
| Empty folders | **Backend scan** over `Folder` + note placement; fix via existing `DELETE /api/notebooks/{notebook}/folders/{folder}` (bulk orchestration in one service call) | HIGH |
| Findings UI | **Vue Health tab** on `WorkspaceIndexSettingsTabs`; expandable nested groups (DaisyUI) | HIGH |
| Defaults | **Per-user columns** on `User` (same pattern as `spaceIntervals` / `dailyAssimilationCount`) | HIGH |
| Report vs fix | **Separate API operations**; fix only for rules with `autoFixable` + user opt-in | HIGH |

The ecosystem standard for in-app knowledge-base health is **not** “install kb-lint / SARIF / Obsidian Linter.” It is a **small domain rule registry** (name, severity, `autoFixable`, `run` → findings), a **report-only pass**, and **opt-in mechanical fixes**. LM Wiki (llmwikis.org + `llmwiki.lint` registry), kb-lint’s severity/`auto_fixable` matrix, and Obsidian Linter’s independent rules all share that shape. Doughnut already has the hard parts (wiki parse/resolve, folder delete, settings tabs).

---

## Recommended Stack

### Core Technologies (reuse — no version bumps required)

| Technology | Version (current) | Purpose | Why Recommended |
|------------|-------------------|---------|-----------------|
| Spring Boot | 4.1.0 | Host `NotebookHealthService` + REST | Authoritative notebook graph lives in MySQL; dead-link identity must match backend `WikiLinkResolver`, not a second client-side resolver. |
| Java | 25 | Rule interfaces, pure scans | Keep rules as testable classes in `services/health/` (+ pure helpers in `algorithms/` when no Spring needed), matching existing layout. |
| Spring Data JPA / MySQL 8.4 | existing | Folder tree + notes for empty-folder rule | Empty-folder = recursive “subtree has no notes”; needs server-side joins/queries, not SPA walks of partial listings. |
| Vue 3.5 + Vue Router 5 | existing | Health tab UI | Extend `WorkspaceIndexSettingsTabs` (`index` \| `settings` → add `health`); stay on-tab expandable results (project decision). |
| Generated OpenAPI SDK | `@hey-api/openapi-ts` 0.99 / springdoc 3.0.3 | Typed lint report + fix calls | Controllers define DTOs → `pnpm generateTypeScript` → `apiCallWithLoading` in Health UI. |
| DaisyUI 5 + Tailwind 4 | existing | Nested findings, action bar, option toggles | Matches notebook workspace chrome; no new UI kit. |
| Flyway | existing | User default columns only | Prefer additive columns on `user` for v1 prefs; avoid speculative prefs tables. |

### Domain patterns (the “stack” for this feature)

| Pattern | Purpose | Why Recommended |
|---------|---------|-----------------|
| **Rule registry** | Pluggable mechanical checks | LM Wiki `LintRule` + `@register` / `REGISTRY`; kb-lint check matrix; ESLint-style id + severity. v1 registers two rules without a plugin host. |
| **Finding record** | Stable report item | `{ ruleId, severity, message, location, autoFixable }` — enough for UI groups and future rules; **do not** ship SARIF in v1. |
| **Report ≠ Fix** | Safety | LM Wiki: lint finds; deletes/moves need consent. kb-lint: links not auto-fixable. Project: dead links report-only; empty folders fix only when bulk option selected. |
| **Severity enum** | Prioritize display | Use a small fixed set: `blocker` \| `high` \| `medium` \| `low` (LM Wiki) or map v1 to `error` \| `warning` \| `info`. Prefer **`error` / `warning` / `info`** to match Doughnut/API simplicity; map LM “medium” dead links → `warning`, empty folders → `info` or `warning`. |
| **Gated fix application** | Safe bulk structural fix | Fix endpoint accepts explicit fix-option ids (e.g. `remove_empty_folders`); no silent delete. Re-run lint after fix or return applied counts + residual report. |

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| **None new** | — | — | v1 needs zero npm/Maven additions. |
| Existing `WikiLinkMarkdown` / `NoteContentMarkdown` | in-repo | Extract `[[wiki]]` from body + leading frontmatter | Dead-link rule only — do not reimplement regex on the frontend for the Health report. |
| Existing `WikiLinkResolver` | in-repo | Resolve token → note (aliases, notebook-qualified links) | Dead-link = inners where resolve returns empty (match product dead-link semantics, including readability vs any-target — prefer **same definition as render-time dead links**). |
| Existing folder `DELETE` | `NotebookController` | Soft/hard delete path already tested | Health fix should call internal folder delete service in a transaction for eligible empty trees, not invent a second delete path. |
| JUnit 5 | existing | Per-rule unit tests + service integration tests | One test class per rule + report/fix service tests; no new test framework. |
| Vitest / Cypress Cucumber | existing | Health tab + fix E2E | Targeted E2E for run → findings → fix; tag `@wip` until green. |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| `pnpm generateTypeScript` | After Health controller/DTO land | Required before frontend calls. |
| `pnpm backend:test_only` / `backend:verify` | Rule + service tests | Prefer unit tests for empty-folder recursion and dead-link extraction. |
| `pnpm cy:run --spec …` | Health tab E2E | Do not run full E2E suite for this slice. |
| Spotless / Biome | Formatting | No new linters for note content (Biome stays code-only). |

---

## Installation

```bash
# No new packages for v1.
# Implement in existing modules:
#   backend/.../services/health/   (or services/notebookHealth/)
#   backend/.../algorithms/        (pure empty-folder / finding helpers if Spring-free)
#   backend/.../controllers/       (NotebookHealthController or methods on NotebookController)
#   frontend/.../components/notebook/ (Health tab panel)
#   frontend/.../WorkspaceIndexSettingsTabs.vue (add "health" tab)
#
# After API DTOs:
CURSOR_DEV=true nix develop -c pnpm generateTypeScript
```

---

## Doughnut mapping (backend vs frontend)

### Backend (authoritative)

| Responsibility | Where | Notes |
|----------------|-------|-------|
| Rule interface + registry | `services/health/` | e.g. `HealthRule` with `id()`, `severity()`, `autoFixable()`, `findings(NotebookHealthContext ctx)` |
| Runner | `NotebookHealthService.lint(notebook, options)` | Runs selected rules; returns grouped report DTO |
| Empty-folder rule | Backend only | Load folder tree + notes-in-folder counts; mark folders whose **entire subtree has zero non-deleted notes** |
| Dead-link rule | Backend only | For each note in notebook: `NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder` (body + frontmatter via existing helpers) → `WikiLinkResolver`; emit finding per unresolved token (group by note in DTO) |
| Fix: remove empty folders | Backend only | Only if option `remove_empty_folders` enabled; delete deepest-first or parents after children; reuse existing folder delete semantics; authorize via `AuthorizationService` |
| User defaults | `User` + User settings API | Booleans: e.g. `healthAutoFixEmptyFolders` (name TBD); load into Health tab as defaults for “this run” and “save as defaults” |
| OpenAPI DTOs | `controllers/dto/` | `NotebookHealthReport`, `HealthFindingGroup`, `HealthFinding`, `NotebookHealthLintRequest`, `NotebookHealthFixRequest`, `NotebookHealthFixResult` |

**Suggested API shape (pattern, not final paths):**

- `POST /api/notebooks/{notebook}/health/lint` — body: enabled rules / options for this run; response: report
- `POST /api/notebooks/{notebook}/health/fix` — body: must include explicit fix option(s); response: counts + optional refreshed report
- User defaults via existing user update path (or small dedicated fields on `UserDTO`)

### Frontend (presentation + gating)

| Responsibility | Where | Notes |
|----------------|-------|-------|
| Health tab | Extend `WorkspaceIndexSettingsTab` | Third tab beside Index / Settings on notebook (and folder page only if project later requires — v1 is notebook settings) |
| Run lint | `apiCallWithLoading` + generated SDK | User-initiated; block UI optional for large notebooks |
| Nested findings | Expandable sections | Group by `ruleId`: “Empty folders” → list; “Dead links” → by note → tokens |
| Options | Checkboxes for this run | Mirror LM/kb-lint: auto-fix path visible but Fix disabled until `remove empty folders` selected |
| Save defaults | User update call | Per-user, not per-notebook |
| Dead-link display in editor | **Unchanged** | Frontend `dead-link` CSS remains editor UX; Health report is the notebook-scoped audit, not a replacement for inline dead links |

### What stays frontend-only (do not)

- Do **not** implement empty-folder scan by walking SPA folder listings (incomplete pagination / cache risk).
- Do **not** implement Health dead-link report by reusing Quill HTML / `replaceWikiLinksInHtml` alone (misses authoritative alias/notebook resolution and frontmatter parity).

---

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| In-process Java rule registry | Python **kb-lint** / **mdlint** CLI as sidecar | Never inside Doughnut product path — filesystem wiki tools, not JPA graph; would duplicate wiki semantics. |
| Custom findings DTO | **SARIF 2.1** report format | Only if exporting to GitHub Code Scanning / IDE problem panels becomes a product goal. Overkill for in-app Health tab. |
| Backend scan | Frontend-only lint | Never for authoritative health; OK only for ephemeral editor hints (already have `dead-link`). |
| Custom registry | Embed **ESLint** / **markdownlint** | Wrong artifact model (files on disk, different link grammar); no folder entities. |
| Custom registry | Vendor **Obsidian Linter** patterns as a plugin host | Formatting-oriented (YAML/spacing); not folder trees or Doughnut resolution. Steal **ideas** (independent rules, settings toggles), not the plugin. |
| Reuse folder DELETE in one fix service | Call N DELETE from the SPA | Worse UX/atomicity; prefer single fix transaction that deletes all eligible empty folders. |
| User columns for defaults | `localStorage` only | Loses cross-device; project requires per-user defaults. |
| User columns | Full prefs JSON document / new prefs service | Premature for two booleans; escalate if rule config explodes post-v1. |

---

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| New lint frameworks (ESLint, markdownlint, Vale, Spectral for notes) | Target source/docs files, not Doughnut `Note`/`Folder` graph | Java `HealthRule` registry |
| **kb-lint**, **mdlint**, **llmwiki** as a dependency | Different runtime and data model; link resolution would diverge | Port the **pattern** (severity, `auto_fixable`, registry) into Java |
| **SARIF** UI / SARIF-js viewers | Heavy interchange format; no CI consumer in-app | Nested JSON groups for Vue |
| **LLM / OpenAI** for v1 Health | Project out of scope; LM Wiki semantic tier is later | Mechanical rules only |
| Auto-fix for dead wiki links | Needs human judgment (create note vs retarget vs delete text); LM/kb-lint treat links as report-only or “create page” proposals | Report-only findings; user follows existing dead-link UX |
| Per-folder multi-select delete UI | Project explicitly out of scope | Single bulk option + Fix |
| Dedicated `/health` route or modal | Project: stay on Health tab | Tab panel only |
| Second wiki-link parser on frontend for Health | Drift from `WikiLinkResolver` / aliases | Backend report only |
| Async job queue / Redis for v1 lint | Notebook sizes fit request-scoped scan; adds failure modes | Synchronous POST; revisit if timeouts appear |
| Storing full lint history in DB | Not required for v1 value | Ephemeral report in response; optional future |

---

## Stack Patterns by Variant

**If only empty-folder cleanup mattered:**
- Backend query + existing folder delete might suffice without a registry.
- **Still use a registry** so dead-link rule and future rules share one report shape (stop-safe extensibility without rewrite).

**If dead links only mattered:**
- Could surface counts from existing wiki cache — but Health needs **notebook-wide** scan including notes not currently open; still backend batch resolve.

**If semantic / LLM lint is added later:**
- Same registry with `requiresLlm = true` (LM Wiki / llmwiki pattern); runner skips unless opted in.
- Keep structural rules free of OpenAI dependency.

**If CLI / MCP should run health later:**
- Same backend endpoints via generated SDK (`doughnut-api`); no separate engine.
- Defer CLI/MCP until web Health is stable.

**If notebooks become huge (timeouts):**
- Add pagination of findings or background job + poll — **not** in v1 stack choice; design DTOs so findings are a list (paginatable later).

---

## Version Compatibility

| Piece | Compatible With | Notes |
|-------|-----------------|-------|
| Health DTOs / OpenAPI | `@hey-api/openapi-ts` 0.99 + springdoc 3.0.3 | Regenerate SDK after controller changes; never hand-edit `sdk.gen.ts`. |
| `WikiLinkResolver` | `NoteContentMarkdown` / `WikiLinkMarkdown` | Dead-link rule must use the same inner-extraction path as cache/render. |
| Folder delete | Existing notebook authorization | Fix path must `assertAuthorization` on notebook; only delete folders in that notebook that are still empty at apply time (TOCTOU: re-check emptiness in fix). |
| User defaults columns | Flyway + `User` / `UserDTO` | Follow `db-migration` rule; additive only. |
| Health tab | `WorkspaceIndexSettingsTabs` | Type becomes `"index" \| "settings" \| "health"`; update notebook (and folder pages only if they share the component and should show Health — default: notebook workspace). |

---

## Recommended rule model (concrete)

```text
HealthRule
  id: "empty_folders" | "dead_wiki_links" | …
  severity: error | warning | info
  autoFixable: boolean          // empty_folders=true, dead_wiki_links=false
  findings(ctx) -> List<Finding>

Finding
  ruleId
  severity
  message
  // location discriminators (OpenAPI oneOf or optional fields):
  folderId?, folderPath?
  noteId?, noteTitle?, linkToken?, occurrenceHint?

HealthReport
  groups: [{ ruleId, title, findings[] }]
  summary: { byRule: counts }

HealthLintOptions (this run)
  // v1: both rules always on; options focus on fix gate
  removeEmptyFolders: boolean   // enables Fix for that rule

HealthFixRequest
  removeEmptyFolders: boolean   // must be true to apply; reject otherwise
```

Aligns with llmwiki `LintRule` (`name`, `severity`, `auto_fixable`, `run` → issues) and kb-lint’s auto-fixable matrix without importing either.

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| No new frameworks | HIGH | Ecosystem tools are for markdown-on-disk wikis; Doughnut is DB-backed. |
| Backend rule registry | HIGH | Matches LM Wiki / llmwiki / kb-lint patterns and Doughnut architecture. |
| Reuse wiki plumbing | HIGH | `WikiLinkMarkdown`, `NoteContentMarkdown`, `WikiLinkResolver` already implement parse + resolve. |
| Reuse folder delete | HIGH | `DELETE .../folders/{folder}` exists; Health fix orchestrates bulk empty-only deletes. |
| User column defaults | HIGH | Same pattern as `spaceIntervals`; two booleans do not need a prefs subsystem. |
| Severity naming (`error`/`warning`/`info` vs LM blocker/high/medium/low) | MEDIUM | Either works; prefer shorter API enums unless product wants LM vocabulary in UI copy. |
| Sync vs async lint | MEDIUM | Sync is right for v1; large-notebook behavior unmeasured. |

---

## Sources

- [Lint: Keep the Knowledge Graph Healthy – LlmWikis.org](https://llmwikis.org/operations/lint/) — two-tier structural vs semantic; severity; human review for deletes/moves (HIGH)
- [llmwiki lint registry (`LintRule`, `auto_fixable`, `run_all`)](https://github.com/Pratiyush/llm-wiki/blob/06c30e2b0c9018b11463b4fa37de0d75248cde5c/llmwiki/lint/__init__.py) — rule registry pattern (HIGH)
- [kb-lint](https://github.com/SingggggYee/kb-lint) — severity + auto-fixable matrix; links not auto-fixable (HIGH)
- [Obsidian Linter](https://github.com/platers/obsidian-linter/) — independent configurable rules; formatting-focused (MEDIUM — inspirational only)
- [SARIF 2.1](https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html) — interchange format; deferred (HIGH that it is overkill for in-app Health)
- Doughnut codebase: `.planning/codebase/STACK.md`, `ARCHITECTURE.md`; `WikiLinkResolver`, `WikiLinkMarkdown`, `NoteContentMarkdown`, `NotebookController` folder DELETE, `WorkspaceIndexSettingsTabs`, `User` preference columns (HIGH)

---
*Stack research for: Doughnut notebook Health lint + optional auto-fix*
*Researched: 2026-07-22*
