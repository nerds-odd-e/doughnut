# Phase 5: Health tab and Run - Context

**Gathered:** 2026-07-22
**Status:** Ready for planning

<domain>
## Phase Boundary

**Behavior.** From notebook settings, the user can open a **Health** tab, explicitly **Run lint** for the current notebook, and review **expandable nested findings** on that tab — without a dedicated `/health` route, findings dialog, or any mutation on Run.

Delivers HLTH-01, HLTH-02, HLTH-03, and AFIX-01 (report-only run with optional fix-option UI). No user-defaults persistence (Phase 6). No Fix / purge execution (Phase 7). No click-through to editor (v2 HLTH-11). Backend lint rules and `POST .../health/lint` already exist from Phases 2–4 — this phase consumes them in the Vue SPA.

</domain>

<decisions>
## Implementation Decisions

### Tab shell (HLTH-01)
- **D-01:** Add **Health** as a **third tab** on notebook settings only: existing order **Readme | Settings | Health**. Do **not** invent a new route, dialog, or workspace navigation model.
- **D-02:** Integrate by extending `WorkspaceReadmeSettingsTabs` (live name; research’s `WorkspaceIndexSettingsTabs` is stale). Extend the tab union with `"health"` and render a Health panel from `NotebookPageView` when active.
- **D-03:** Health is **notebook-only**. `FolderPage` also uses `WorkspaceReadmeSettingsTabs` — keep folder tabs as Readme | Settings only (e.g. optional `includeHealth` / equivalent prop defaulting off). Do not show Health on folder settings.
- **D-04:** Default tab remains **`readme`** when opening the notebook page. Opening Health is explicit (user clicks Health). Opening Health alone does not run lint or mutate data.

### Run interaction (HLTH-02, AFIX-01)
- **D-05:** Lint runs **only** on explicit **Run** (button label: “Run lint” or “Run”). Merely selecting the Health tab must not call the API or mutate notebook data.
- **D-06:** Call existing `POST /api/notebooks/{notebook}/health/lint` via generated `NotebookHealthController.lint` through `apiCallWithLoading` (loading bar + error toasts). No silent background fetch; no client-side lint.
- **D-07:** Keep the lint request **bodyless** as today. Do **not** add fix-option fields to the lint API in this phase — Run never mutates regardless of UI option state.
- **D-08:** Idle state before first successful Run: show a short prompt + Run control (no fabricated findings). After Run, replace idle with the report view. Re-Run replaces the previous report (ephemeral; no history persistence).

### Findings presentation (HLTH-03)
- **D-09:** Render report `groups` as **expandable** rule sections using DaisyUI `daisy-collapse` (same family as `AssimilationSettings`) or equivalent `<details>` — no modal/route for results.
- **D-10:** Nesting follows the wire shape: flat `items` for folder rules (`empty_folders`, `readme_only_folders`); `children` → items for `dead_wiki_links` (note → tokens). Do not re-group on the client.
- **D-11:** Show **all** groups returned by the report (backend always-emit). Include item/child count in the group header. Groups with findings expand by default; empty groups stay collapsed (or show a quiet “No findings” when expanded).
- **D-12:** Display item `label` only for v1 (folder name / wiki-link inner). **No** click-through to note editor or folder (HLTH-11 is v2).
- **D-13:** Severity badges optional and minimal — `warning` is the only tier in use; do not build a severity settings UI.

### Action bar and fix options (HLTH-03, AFIX-01)
- **D-14:** Health panel includes an **action bar** with: **Run** control and an optional **“Remove empty folders”** checkbox (bulk fix option, visible for this run).
- **D-15:** Checkbox is **UI-only in Phase 5** (local state). It does not change lint behavior and does **not** enable mutation. **No Fix / Apply button** in this phase — Fix enablement and purge are Phase 7 (AFIX-02–05). User-defaults save/prefill are Phase 6 (DFLT-01/02).
- **D-16:** Prove AFIX-01 with an explicit assertion path: Run (with or without checkbox selected) never deletes folders or mutates notebook data — report-only.

### Component layout and naming
- **D-17:** Prefer capability-named components under `frontend/src/components/notebook/` (e.g. `NotebookHealthPanel` for tab body + action bar; findings list as a focused child if the panel would exceed cohesion limits). No phase numbers in product file/test names.
- **D-18:** Use `data-testid`s for E2E: tab (`notebook-workspace-tab-health`), panel, Run control, findings groups by `ruleId`, fix-option checkbox.

### Verification
- **D-19:** Frontend unit/component tests for tab presence (notebook only), idle → Run → findings render (including nested dead-link children), and no API call on tab open alone.
- **D-20:** Targeted E2E (capability-named feature, e.g. notebook health) covering: open Health tab → Run lint → see expandable groups for empty folders / readme-only / dead links as present. Tag `@wip` until green; remove `@wip` when scenarios pass. Do not run the full E2E suite unless required.

### Claude's Discretion
- Exact split between `NotebookHealthPanel` vs findings subcomponent; collapse vs details markup as long as expandable nested review works.
- Exact empty/idle copy; button label “Run” vs “Run lint” — prefer clear “Run lint”.
- Whether to show severity text at all — omit if it adds noise.
- Whether folder pages need an explicit prop or a separate tabs variant — smallest change that keeps Health off folders.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope and requirements
- `.planning/ROADMAP.md` — Phase 5 goal, Behavior type, success criteria (HLTH-01/02/03, AFIX-01), UI hint, stop-safe value after Phase 5
- `.planning/REQUIREMENTS.md` — HLTH-01, HLTH-02, HLTH-03, AFIX-01 (this phase); DFLT-* Phase 6; AFIX-02–05 Phase 7; out-of-scope `/health` route/dialog
- `.planning/PROJECT.md` — Health tab entry, expandable results, lint ≠ fix, action bar, no dedicated route/dialog

### Prior phase decisions (backend contract to consume)
- `.planning/phases/01-health-lint-contract/01-CONTEXT.md` — report/group/item DTO shapes, `children`, reserved rule ids
- `.planning/phases/02-empty-folder-findings/02-CONTEXT.md` — `empty_folders`, write-auth lint API, always-emit group
- `.planning/phases/03-readme-only-folder-findings/03-CONTEXT.md` — `readme_only_folders`, `autoFixable=false`
- `.planning/phases/04-dead-link-findings/04-CONTEXT.md` — `dead_wiki_links` nested by note via `children`

### Research (UI and pitfalls)
- `.planning/research/ARCHITECTURE.md` — thin Health tab; `NotebookHealthPanel` / findings; extend tabs; no findings persistence
- `.planning/research/FEATURES.md` — Health tab entry, explicit Run, expandable on-tab results
- `.planning/research/PITFALLS.md` — no mutate on open/Run alone; no second client linker; auth already on backend
- `.planning/research/SUMMARY.md` — milestone ordering; report before fix

### Frontend / API conventions
- `.planning/codebase/CONVENTIONS.md` — `data-testid`, capability naming, `apiCallWithLoading`
- `.cursor/rules/frontend-api.mdc` — generated SDK + `apiCallWithLoading` + error handling
- `.cursor/rules/e2e-authoring.mdc` — capability-named features, `@wip`, page objects

### Implemented surfaces to extend (must not replace)
- `frontend/src/components/commons/WorkspaceReadmeSettingsTabs.vue` — Readme | Settings tabs (shared with folder page)
- `frontend/src/pages/NotebookPageView.vue` — notebook settings shell; switch on `activeTab`
- `frontend/src/pages/FolderPage.vue` — same tabs component; must **not** gain Health
- `frontend/src/components/notebook/NotebookWorkspaceSettings.vue` — Settings panel sibling pattern
- `packages/generated/doughnut-backend-api/sdk.gen.ts` — `NotebookHealthController.lint`
- `packages/generated/doughnut-backend-api/types.gen.ts` — `NotebookHealthLintReport`, `HealthFindingGroup`, `HealthFindingItem`
- `backend/src/main/java/com/odde/doughnut/controllers/NotebookHealthController.java` — existing bodyless lint endpoint

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `WorkspaceReadmeSettingsTabs` — DaisyUI `daisy-tabs-box`; `v-model` tab union `"readme" | "settings"`; `testIdPrefix` → `notebook-workspace-tab-*`
- `NotebookPageView` — switches Readme vs Settings panels; add Health branch
- Generated `NotebookHealthController.lint` — `POST /api/notebooks/{notebook}/health/lint` returns full multi-rule report
- `apiCallWithLoading` — standard user-initiated API loading/errors
- DaisyUI `daisy-collapse` pattern in `AssimilationSettings.vue` for expandable sections
- Backend always emits `empty_folders`, `readme_only_folders`, `dead_wiki_links` groups (items/children may be empty)

### Established Patterns
- Notebook settings live on the notebook page tabs — no separate settings route for Health
- Frontend API: generated SDK + check `error` before using `data`
- E2E: capability-named features; page objects; `@wip` while red
- Domain language: **readme** / `readmeContent` (tabs already say Readme, not Index)

### Integration Points
- Extend tab type + optional Health visibility for notebook only
- `NotebookPageView` mounts Health panel with `notebook.id`
- Panel calls lint and binds `report.groups` into expandable findings UI
- Phase 6 will prefill/save the fix-option checkbox from user defaults
- Phase 7 will add Fix enablement/purge using the same checkbox semantics

</code_context>

<specifics>
## Specific Ideas

- `--auto` discussion locked: third notebook-only Health tab; explicit Run via bodyless lint + `apiCallWithLoading`; expandable groups from wire shape; action bar with Run + remove-empty-folders checkbox (UI-only); no Fix button; frontend tests + targeted E2E `@wip` until green.
- Research package layout names (`NotebookHealthPanel`, `NotebookHealthFindings`) are guidance — capability names, not phase numbers.
- Stale research name `WorkspaceIndexSettingsTabs` maps to live `WorkspaceReadmeSettingsTabs`.

</specifics>

<deferred>
## Deferred Ideas

- User-level defaults save/prefill — Phase 6 (DFLT-01, DFLT-02)
- Fix button enablement + bulk empty-folder purge — Phase 7 (AFIX-02–05)
- Click-through from dead-link finding to editor — v2 (HLTH-11)
- Health summary chip on notebook chrome — v2 (HLTH-10)
- Lint request body carrying fix options — only if Phase 6/7 need it; not Phase 5
- Health on folder settings — rejected; notebook-scoped only

None — discussion stayed within phase scope

</deferred>

---

*Phase: 5-Health tab and Run*
*Context gathered: 2026-07-22*
