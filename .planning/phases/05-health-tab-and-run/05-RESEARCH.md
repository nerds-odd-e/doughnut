# Phase 5: Health tab and Run - Research

**Researched:** 2026-07-22
**Domain:** Vue SPA Health tab — consume existing bodyless lint API; expandable findings UI
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Add **Health** as a **third tab** on notebook settings only: existing order **Readme | Settings | Health**. Do **not** invent a new route, dialog, or workspace navigation model.
- **D-02:** Integrate by extending `WorkspaceReadmeSettingsTabs` (live name; research’s `WorkspaceIndexSettingsTabs` is stale). Extend the tab union with `"health"` and render a Health panel from `NotebookPageView` when active.
- **D-03:** Health is **notebook-only**. `FolderPage` also uses `WorkspaceReadmeSettingsTabs` — keep folder tabs as Readme | Settings only (e.g. optional `includeHealth` / equivalent prop defaulting off). Do not show Health on folder settings.
- **D-04:** Default tab remains **`readme`** when opening the notebook page. Opening Health is explicit (user clicks Health). Opening Health alone does not run lint or mutate data.
- **D-05:** Lint runs **only** on explicit **Run** (button label: “Run lint” or “Run”). Merely selecting the Health tab must not call the API or mutate notebook data.
- **D-06:** Call existing `POST /api/notebooks/{notebook}/health/lint` via generated `NotebookHealthController.lint` through `apiCallWithLoading` (loading bar + error toasts). No silent background fetch; no client-side lint.
- **D-07:** Keep the lint request **bodyless** as today. Do **not** add fix-option fields to the lint API in this phase — Run never mutates regardless of UI option state.
- **D-08:** Idle state before first successful Run: show a short prompt + Run control (no fabricated findings). After Run, replace idle with the report view. Re-Run replaces the previous report (ephemeral; no history persistence).
- **D-09:** Render report `groups` as **expandable** rule sections using DaisyUI `daisy-collapse` (same family as `AssimilationSettings`) or equivalent `<details>` — no modal/route for results.
- **D-10:** Nesting follows the wire shape: flat `items` for folder rules (`empty_folders`, `readme_only_folders`); `children` → items for `dead_wiki_links` (note → tokens). Do not re-group on the client.
- **D-11:** Show **all** groups returned by the report (backend always-emit). Include item/child count in the group header. Groups with findings expand by default; empty groups stay collapsed (or show a quiet “No findings” when expanded).
- **D-12:** Display item `label` only for v1 (folder name / wiki-link inner). **No** click-through to note editor or folder (HLTH-11 is v2).
- **D-13:** Severity badges optional and minimal — `warning` is the only tier in use; do not build a severity settings UI.
- **D-14:** Health panel includes an **action bar** with: **Run** control and an optional **“Remove empty folders”** checkbox (bulk fix option, visible for this run).
- **D-15:** Checkbox is **UI-only in Phase 5** (local state). It does not change lint behavior and does **not** enable mutation. **No Fix / Apply button** in this phase — Fix enablement and purge are Phase 7 (AFIX-02–05). User-defaults save/prefill are Phase 6 (DFLT-01/02).
- **D-16:** Prove AFIX-01 with an explicit assertion path: Run (with or without checkbox selected) never deletes folders or mutates notebook data — report-only.
- **D-17:** Prefer capability-named components under `frontend/src/components/notebook/` (e.g. `NotebookHealthPanel` for tab body + action bar; findings list as a focused child if the panel would exceed cohesion limits). No phase numbers in product file/test names.
- **D-18:** Use `data-testid`s for E2E: tab (`notebook-workspace-tab-health`), panel, Run control, findings groups by `ruleId`, fix-option checkbox.
- **D-19:** Frontend unit/component tests for tab presence (notebook only), idle → Run → findings render (including nested dead-link children), and no API call on tab open alone.
- **D-20:** Targeted E2E (capability-named feature, e.g. notebook health) covering: open Health tab → Run lint → see expandable groups for empty folders / readme-only / dead links as present. Tag `@wip` until green; remove `@wip` when scenarios pass. Do not run the full E2E suite unless required.

### Claude's Discretion
- Exact split between `NotebookHealthPanel` vs findings subcomponent; collapse vs details markup as long as expandable nested review works.
- Exact empty/idle copy; button label “Run” vs “Run lint” — prefer clear “Run lint”.
- Whether to show severity text at all — omit if it adds noise.
- Whether folder pages need an explicit prop or a separate tabs variant — smallest change that keeps Health off folders.

### Deferred Ideas (OUT OF SCOPE)
- User-level defaults save/prefill — Phase 6 (DFLT-01, DFLT-02)
- Fix button enablement + bulk empty-folder purge — Phase 7 (AFIX-02–05)
- Click-through from dead-link finding to editor — v2 (HLTH-11)
- Health summary chip on notebook chrome — v2 (HLTH-10)
- Lint request body carrying fix options — only if Phase 6/7 need it; not Phase 5
- Health on folder settings — rejected; notebook-scoped only
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| HLTH-01 | User can open a **Health** tab on notebook settings (alongside Index/Settings), without a separate `/health` route or findings dialog | Extend `WorkspaceReadmeSettingsTabs` with optional Health; mount panel only from `NotebookPageView`; no router change |
| HLTH-02 | User can explicitly **Run lint** for the current notebook (on-demand; opening Health does not mutate data) | `NotebookHealthController.lint` via `apiCallWithLoading` on Run only; idle until first success |
| HLTH-03 | User can review **expandable nested findings** on the Health tab (grouped by rule; dead links nested by note) with an action bar for fix options | Render `report.groups` with `daisy-collapse`; wire shape items/children; action bar Run + remove-empty-folders checkbox |
| AFIX-01 | Auto-fix is an **optional** run option; lint never deletes or mutates on Run alone | Checkbox UI-only; bodyless POST; no Fix button; assert folders unchanged after Run with checkbox on |
</phase_requirements>

## Summary

Phase 5 is a **frontend-only Behavior** slice. Backend lint is complete: owner-authorized bodyless `POST /api/notebooks/{notebook}/health/lint` returns a multi-rule `NotebookHealthLintReport` with always-emitted groups `empty_folders`, `readme_only_folders`, and `dead_wiki_links` (nested by note via `children`). The SPA must expose a third **Health** tab on notebook settings only, call lint only on explicit **Run lint**, and render expandable groups in-tab.

Critical integration seams are live and small: `WorkspaceReadmeSettingsTabs` currently is `"readme" | "settings"` with `testIdPrefix` → `notebook-workspace-tab-*`; `NotebookPageView` uses `v-if="readme"` / `v-else` for Settings — that **`v-else` must become an explicit three-way branch** or Health will incorrectly render Settings. Folder pages share the tabs component and must keep Health off via a prop defaulting false.

No new npm packages, no OpenAPI regen, no backend changes. Generated SDK already exposes `NotebookHealthController.lint` with `body?: never` and `path.notebook: number`.

**Primary recommendation:** Extend tabs with `includeHealth` (default `false`), mount `NotebookHealthPanel` from `NotebookPageView` only, bind Run to bodyless `apiCallWithLoading(NotebookHealthController.lint)`, render groups with DaisyUI collapse following the wire shape, and prove no-mutate with unit + targeted `@wip` E2E.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Health tab chrome (Readme \| Settings \| Health) | Browser / Client | — | Pure Vue tab shell; notebook-only visibility |
| Idle / report panel state | Browser / Client | — | Ephemeral last report; no findings persistence |
| Explicit Run lint | Browser / Client | API / Backend | UI triggers; backend authoritative evaluation |
| Rule evaluation & report shape | API / Backend | Database / Storage | Already implemented Phases 2–4; consume only |
| Expandable findings presentation | Browser / Client | — | Map wire DTO to collapses; no client re-group |
| Remove-empty-folders option (UI-only) | Browser / Client | — | Local boolean; no API body; no Fix in Phase 5 |
| Auth for lint | API / Backend | — | `assertAuthorization(notebook)` already on controller |
| Mutation / purge | — (out of scope) | — | Phase 7 only |

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Vue 3 SFC + `<script setup>` | project pin | Health panel + tabs | Existing notebook/folder pages |
| Generated OpenAPI SDK | `packages/generated/doughnut-backend-api` | `NotebookHealthController.lint` | Project rule: never hand-roll API clients |
| `apiCallWithLoading` | `@/managedApi/clientSetup` | Thin loading bar + error toasts | User-initiated actions per `frontend-api.mdc` |
| DaisyUI collapse | project Tailwind/DaisyUI | Expandable finding groups | Live pattern in `AssimilationSettings.vue`, `FailureReportPage.vue` |
| Vitest + `@testing-library/vue` / VTU helpers | frontend package | Unit/component tests | `frontend/tests` + `mockSdkService` |
| Cypress + Cucumber | `e2e_test/` | Targeted E2E | Capability-named features, page objects |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `CheckInput` | `frontend/src/components/form/CheckInput.vue` | Labeled checkbox | Remove-empty-folders option (matches Settings forms) |
| `doughnut-test-fixtures/makeMe` | monorepo package | API-shaped fixtures | Unit tests for report groups |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `daisy-collapse` | Native `<details>` | Both allowed (D-09); collapse matches AssimilationSettings and supports checkbox-driven open state |
| `includeHealth` prop | Duplicate tabs component for notebook | More duplication; prop is smallest change (discretion) |
| Thin-bar `apiCallWithLoading` | `{ blockUi: true }` | Lint is read-only report; thin bar is enough — do not add whole-UI blocker unless UX requires it |

**Installation:** None — no new packages.

**Version verification:** Phase consumes in-repo stack only. Lint API signature verified in generated types: `LintData.body?: never`, path `notebook: number`, response `NotebookHealthLintReport`. [VERIFIED: packages/generated/doughnut-backend-api]

## Package Legitimacy Audit

No external packages to install for this phase.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| — | — | — | — | — | — | N/A |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
User on Notebook page
    │
    ▼
WorkspaceReadmeSettingsTabs  (includeHealth=true → Readme | Settings | Health)
    │ activeTab === "health"
    ▼
NotebookHealthPanel(notebookId)
    │ idle: prompt + action bar
    │ Run lint click
    ▼
apiCallWithLoading(() =>
  NotebookHealthController.lint({ path: { notebook: id } })  // bodyless
)
    │
    ▼
POST /api/notebooks/{notebook}/health/lint
    │ assertAuthorization(notebook)  [already]
    │ NotebookHealthService.lint (readOnly transaction)
    ▼
NotebookHealthLintReport { groups: [
  empty_folders (items[]),
  readme_only_folders (items[]),
  dead_wiki_links (children[] → items[])
]}
    │
    ▼
NotebookHealthFindings (or inline)
    │ daisy-collapse per group (ruleId)
    │ nested collapse per child (note title)
    │ leaf: item.label only
    ▼
Ephemeral report state (replaced on Re-Run)
```

Folder path: same tabs component with `includeHealth` false (default) — no Health tab, no panel.

### Recommended Project Structure

```
frontend/src/
├── components/
│   ├── commons/
│   │   └── WorkspaceReadmeSettingsTabs.vue   # + "health" tab when includeHealth
│   └── notebook/
│       ├── NotebookHealthPanel.vue           # action bar + idle/report shell
│       └── NotebookHealthFindings.vue        # optional: collapse tree from groups
├── pages/
│   ├── NotebookPageView.vue                  # includeHealth; health branch
│   └── FolderPage.vue                        # leave default (no Health)
frontend/tests/
├── pages/NotebookPageView.spec.ts            # extend: Health tab, no call on open
├── pages/FolderPage*.spec.ts or landmarks    # assert no Health tab on folder
└── components/notebook/
    └── NotebookHealthPanel.spec.ts           # idle → Run → findings; checkbox UI-only
e2e_test/
├── features/notebooks/notebook_health.feature
├── start/pageObjects/notebookPage.ts         # openHealthTab, runLint, expect groups
└── start/pageObjects/workspaceSurfaceLandmarks.ts  # notebook Health present; folder absent
```

### Pattern 1: Notebook-only Health tab prop

**What:** Extend tab union to `"readme" | "settings" | "health"`. Add `includeHealth?: boolean` default `false`. Render Health `<a>` only when true. `NotebookPageView` passes `include-health` (or `:include-health="true"`); `FolderPage` unchanged.

**When to use:** Shared tabs component used by notebook and folder.

**Example:**

```vue
<!-- WorkspaceReadmeSettingsTabs.vue -->
<a
  v-if="includeHealth"
  :class="tabClass('health')"
  role="button"
  href="#"
  :data-testid="`${testIdPrefix}-tab-health`"
  @click.prevent="model = 'health'"
>Health</a>
```

```ts
export type WorkspaceReadmeSettingsTab = "readme" | "settings" | "health"
// FolderPage keeps activeTab typed the same but never sets/sees health in UI
```

### Pattern 2: Explicit three-way panel switch (required)

**What:** Replace Settings `v-else` with explicit branches.

**When to use:** Always — current `NotebookPageView` uses `v-else` for Settings.

**Example:**

```vue
<div v-if="activeTab === 'readme'" data-testid="notebook-workspace-readme">…</div>
<NotebookWorkspaceSettings v-else-if="activeTab === 'settings'" … />
<NotebookHealthPanel
  v-else-if="activeTab === 'health'"
  :notebook-id="notebook.id"
  data-testid="notebook-workspace-health"
/>
```

### Pattern 3: Bodyless lint via generated SDK

**What:** User-initiated Run only; check `error` before assigning report.

**When to use:** HLTH-02 / AFIX-01.

**Example:**

```typescript
// Source: .cursor/rules/frontend-api.mdc + packages/generated/doughnut-backend-api/sdk.gen.ts
import { NotebookHealthController } from "@generated/doughnut-backend-api/sdk.gen"
import type { NotebookHealthLintReport } from "@generated/doughnut-backend-api"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

const report = ref<NotebookHealthLintReport | null>(null)
const removeEmptyFolders = ref(false) // UI-only; never sent

async function runLint(notebookId: number) {
  const { data, error } = await apiCallWithLoading(() =>
    NotebookHealthController.lint({
      path: { notebook: notebookId },
    })
  )
  if (!error) {
    report.value = data
  }
}
```

### Pattern 4: Expandable groups from wire shape

**What:** Iterate `report.groups ?? []`. Header: `title` + finding count. Open by default when group has findings (`items.length > 0` or `children.length > 0`). Nested: for each child group, another collapse; leaves render `item.label`.

**When to use:** HLTH-03.

**Example (collapse family):**

```vue
<!-- Source: frontend/src/components/recall/AssimilationSettings.vue + FailureReportPage.vue -->
<div
  v-for="group in groups"
  :key="group.ruleId"
  class="daisy-collapse daisy-collapse-arrow border border-base-300 bg-base-200/50 rounded-lg"
  :data-testid="`notebook-health-group-${group.ruleId}`"
>
  <input type="checkbox" :checked="groupHasFindings(group)" />
  <div class="daisy-collapse-title min-h-0 py-3 text-sm font-medium">
    {{ group.title }} ({{ findingCount(group) }})
  </div>
  <div class="daisy-collapse-content">
    <!-- items: flat labels; children: nested collapses -->
  </div>
</div>
```

**Finding count recommendation:** leaf count = `(items?.length ?? 0) + sum(children.items.length)`. For dead_wiki_links top group, items are empty and children hold tokens — leaf count equals total dead tokens; also acceptable to show `children.length` (notes). Prefer leaf count so headers stay meaningful.

### Anti-Patterns to Avoid

- **`v-else` for Settings after adding Health:** Health would show Settings panel. Use explicit `v-else-if`.
- **Calling lint on tab select / `onMounted` of panel:** Violates D-04/D-05 and AFIX-01 spirit.
- **Sending checkbox in lint body:** Lint is bodyless (`LintData.body?: never`); would require API change (deferred).
- **Adding Fix / Apply button:** Phase 7 only.
- **Client-side wiki-link or empty-folder scan:** Backend is authoritative (PITFALLS).
- **New route `/health` or modal:** Out of scope.
- **Phase numbers in file/test names:** Capability names only (`notebook_health`, `NotebookHealthPanel`).
- **Hand-editing generated SDK:** No regen needed; endpoint already present.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| HTTP client for lint | `fetch` / axios wrapper | `NotebookHealthController.lint` | Generated types, session client, consistent errors |
| Loading / error UX | Local LoadingModal + toast glue | `apiCallWithLoading` | Global bar + toasts per frontend-api |
| Expandable sections | Custom accordion from scratch | `daisy-collapse` (or `<details>`) | Matches product UI |
| Checkbox control | Raw unlabeled input | `CheckInput` or labeled daisy-checkbox | Consistent Settings forms |
| Report re-grouping | Client maps by rule inventing structure | Render `groups` / `children` as returned | D-10; stable E2E `ruleId`s |
| Auth checks in UI | Client-only “is owner” gates as sole protection | Backend already rejects foreign/anon | Defense in depth is server-side |

**Key insight:** Phase 5 is a thin consumer of an already-complete report contract — risk is UI wiring mistakes (auto-run, wrong panel branch, mutation assumptions), not rule logic.

## Common Pitfalls

### Pitfall 1: Settings panel captures Health via `v-else`

**What goes wrong:** Clicking Health shows Notebook Settings sections.
**Why it happens:** Current `NotebookPageView` uses `v-else` for Settings.
**How to avoid:** Explicit three-way `v-if` / `v-else-if` branches; unit test asserts settings absent when health active.
**Warning signs:** Settings testids visible on Health tab.

### Pitfall 2: Lint on tab open

**What goes wrong:** Opening Health triggers API and/or feels like mutation.
**Why it happens:** `onMounted` / `watch(activeTab)` habits.
**How to avoid:** Call lint only from Run handler; unit test spy not called until Run.
**Warning signs:** Network call without button click; idle prompt never shown.

### Pitfall 3: Treating checkbox as behavioral

**What goes wrong:** Checkbox changes request shape or enables Fix.
**Why it happens:** Anticipating Phase 6/7.
**How to avoid:** Local `ref(false)` only; assert lint called with path-only options; no Fix button in DOM.
**Warning signs:** `body` in lint options; Fix control in template.

### Pitfall 4: Health appears on folder settings

**What goes wrong:** Folder page gains Health tab.
**Why it happens:** Shared tabs component without a gate.
**How to avoid:** `includeHealth` default false; FolderPage does not set it; unit/E2E assert `folder-workspace-tab-health` absent.
**Warning signs:** Landmark E2E fails or folder shows Health.

### Pitfall 5: Client re-group or hide empty groups

**What goes wrong:** Missing always-emit groups; wrong nesting for dead links.
**Why it happens:** Filtering empty groups or flattening children.
**How to avoid:** Render all `groups`; nest `children` as returned; empty groups collapsed with count `0`.
**Warning signs:** E2E cannot find `notebook-health-group-readme_only_folders` on a clean notebook.

### Pitfall 6: Landmark E2E drift

**What goes wrong:** `workspace_surface_landmarks.feature` still expects only Readme | Settings on notebook.
**Why it happens:** Existing steps assert two tabs.
**How to avoid:** Update `workspaceSurfaceLandmarks.expectNotebookWorkspaceTabsPresent` to include Health; keep folder expectations at two tabs only.
**Warning signs:** Landmark feature fails after Health lands.

### Pitfall 7: Assuming OpenAPI regen is required

**What goes wrong:** Unnecessary `pnpm generateTypeScript` churn.
**Why it happens:** Prior phases regenerated client when adding endpoints.
**How to avoid:** Endpoint and types already present — no backend/OpenAPI change in Phase 5.

## Code Examples

### Run lint (bodyless)

```typescript
// Source: packages/generated/doughnut-backend-api/sdk.gen.ts (NotebookHealthController.lint)
// LintData: body?: never; path: { notebook: number }
const { data: report, error } = await apiCallWithLoading(() =>
  NotebookHealthController.lint({
    path: { notebook: props.notebookId },
  })
)
if (!error) {
  lastReport.value = report
}
```

### Mock lint in unit tests

```typescript
// Source: frontend/tests/helpers/index.ts — mockSdkService
import { NotebookHealthController } from "@generated/doughnut-backend-api/sdk.gen"
import { mockSdkService } from "@tests/helpers"

const lintSpy = mockSdkService(NotebookHealthController, "lint", {
  groups: [
    {
      ruleId: "empty_folders",
      title: "Empty folders",
      severity: "warning",
      autoFixable: true,
      items: [{ folderId: 1, label: "Empty Shell" }],
    },
    {
      ruleId: "readme_only_folders",
      title: "Readme-only folders",
      severity: "warning",
      autoFixable: false,
      items: [],
    },
    {
      ruleId: "dead_wiki_links",
      title: "Dead wiki links",
      severity: "warning",
      autoFixable: false,
      items: [],
      children: [
        {
          ruleId: "dead_wiki_links",
          title: "Source",
          severity: "warning",
          autoFixable: false,
          items: [{ noteId: 9, label: "Missing", wikiLinkToken: "Missing" }],
        },
      ],
    },
  ],
})
// open Health tab → expect(lintSpy).not.toHaveBeenCalled()
// click Run → expect(lintSpy).toHaveBeenCalledOnce()
// expect call path: { notebook: <id> } and no body
```

### Recommended data-testids (D-18)

| Element | testid |
|---------|--------|
| Health tab | `notebook-workspace-tab-health` (via prefix) |
| Health panel | `notebook-workspace-health` |
| Run control | `notebook-health-run` |
| Remove empty folders checkbox | `notebook-health-remove-empty-folders` |
| Idle prompt | `notebook-health-idle` |
| Findings root | `notebook-health-findings` |
| Group | `notebook-health-group-{ruleId}` |
| Nested note group (optional) | `notebook-health-group-child` + note title text |

### Wire rule ids and titles (backend — do not invent)

| ruleId | title | Shape | autoFixable |
|--------|-------|-------|-------------|
| `empty_folders` | Empty folders | `items[]` (label = folder name) | true |
| `readme_only_folders` | Readme-only folders | `items[]` | false |
| `dead_wiki_links` | Dead wiki links | `items: []`, `children[]` (title = note title, items.label = token) | false |

[VERIFIED: backend HealthRuleIds + rule title() methods]

## State of the Art

| Old Approach (stale research names) | Current Approach | When Changed | Impact |
|-------------------------------------|------------------|--------------|--------|
| `WorkspaceIndexSettingsTabs` | `WorkspaceReadmeSettingsTabs` | Product readme rename | Use live component name |
| `indexContent` | `readmeContent` | Product rename | No index language in Health UI |
| Backend-only lint (Phases 2–4) | SPA consumes report | Phase 5 | First user-visible Health surface |

**Deprecated/outdated:**
- Milestone research diagrams that still say Index tab or future fix API in the Phase 5 path — Fix is Phase 7; defaults Phase 6.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Leaf finding count (items + child items) is the preferred header count | Pattern 4 | Minor UX; planner/discretion can use children.length for dead links |
| A2 | Thin-bar `apiCallWithLoading` without `blockUi` is sufficient for Run | Standard Stack | If lint is very slow, UX may need blockUi — not required by CONTEXT |

No other ASSUMED claims: stack and API verified in-repo.

## Open Questions

1. **Should landmark E2E wording change from “Readme and Settings tabs” to include Health?**
   - What we know: `workspace_surface_landmarks` asserts two notebook tabs today.
   - What's unclear: Whether to rename steps vs extend assertions only.
   - Recommendation: Extend assertions to require Health on notebook; keep step text sensible (“notebook workspace tabs include Health”) or update existing Then steps in the same feature change.

2. **E2E fixture strategy for all three finding types in one scenario vs three**
   - What we know: Empty folder = create folder with no notes; readme-only = folder with readme and no notes; dead link = note body `[[Missing]]`.
   - What's unclear: Minimal scenario count for D-20.
   - Recommendation: One primary scenario seeding all three (create empty folder, set folder readme on another empty folder, note with dead link) plus a dedicated AFIX-01 scenario (Run with checkbox checked → folder still in sidebar).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Nix + `pnpm sut` (frontend HMR, backend) | Dev / E2E | Assumed running per agent-map | — | `pnpm sut:healthcheck` |
| Generated SDK `NotebookHealthController` | Run lint | ✓ | in-repo | — |
| DaisyUI collapse styles | Findings UI | ✓ | project Tailwind | `<details>` |
| Cypress | Targeted E2E | ✓ | e2e_test | — |

**Missing dependencies with no fallback:** none for code/config-only Phase 5.

**Missing dependencies with fallback:** none.

Step 2.6: External tools are existing project toolchain only; no new installs.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Vitest (frontend) + Cypress/Cucumber (E2E) |
| Config file | `frontend` vitest config; `e2e_test/config/ci.ts` (`not @wip` in CI) |
| Quick run command | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/NotebookPageView.spec.ts` (and Health panel specs) |
| Full suite command | `CURSOR_DEV=true nix develop -c pnpm frontend:test` (unit); targeted E2E only for phase |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| HLTH-01 | Health tab on notebook; not on folder; no route | unit | `pnpm frontend:test tests/pages/NotebookPageView.spec.ts` (+ FolderPage assertion) | ❌ Wave 0 extend existing |
| HLTH-02 | Run only on button; idle first; no call on tab open | unit | `pnpm frontend:test tests/components/notebook/NotebookHealthPanel.spec.ts` | ❌ Wave 0 |
| HLTH-03 | Expandable groups; nested dead links; action bar | unit | same panel/findings specs | ❌ Wave 0 |
| AFIX-01 | Checkbox on does not mutate; no Fix button | unit + E2E | unit assert no Fix + lint bodyless; E2E folder still present | ❌ Wave 0 |
| HLTH-* | Open Health → Run → see groups | E2E `@wip` | `pnpm cypress run --spec e2e_test/features/notebooks/notebook_health.feature` | ❌ Wave 0 |

### Sampling Rate

- **Per task commit:** targeted frontend specs for touched files
- **Per wave merge:** all new/updated Health frontend specs green
- **Phase gate:** frontend Health specs green; E2E `@wip` removed only when scenarios pass; do not run full E2E suite

### Wave 0 Gaps

- [ ] `frontend/tests/components/notebook/NotebookHealthPanel.spec.ts` — idle, Run, findings, no API on mount, checkbox UI-only, no Fix
- [ ] Extend `frontend/tests/pages/NotebookPageView.spec.ts` — Health tab present; panel on Health; settings not shown
- [ ] Folder assertion — Health tab absent (`folder-workspace-tab-health` not exist)
- [ ] `e2e_test/features/notebooks/notebook_health.feature` — `@wip` until green
- [ ] Page object methods on `notebookPage` + landmark updates for Health tab
- [ ] Existing `workspace_surface_landmarks.feature` / steps updated for notebook Health presence

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes (session already required) | Existing session cookie / SPA client |
| V3 Session Management | no new session surface | — |
| V4 Access Control | yes | Backend `authorizationService.assertAuthorization(notebook)` on lint; foreign/anon rejected (controller tests) |
| V5 Input Validation | minimal | Bodyless POST; notebook id from trusted `notebook.id` prop (path param) |
| V6 Cryptography | no | — |

### Known Threat Patterns for Health UI + lint API

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Subscriber / bazaar reader runs lint | Information Disclosure | Backend write-gate only (not read auth) — already implemented |
| UI implies Fix while checkbox checked | Elevation / Tampering | No Fix button; no purge API call in Phase 5; AFIX-01 tests |
| Client-only “authorization” | Spoofing | Do not add fake owner checks as sole control; rely on API errors/toasts |
| XSS via finding labels | Tampering | Render labels as text (Vue default escaping); no `v-html` on labels |

## Project Constraints (from .cursor/rules/)

| Directive | Implication for Phase 5 |
|-----------|-------------------------|
| `CURSOR_DEV=true nix develop -c …` for tooling | All frontend/E2E commands use Nix prefix |
| Capability naming; no phase numbers in product/tests | `NotebookHealthPanel`, `notebook_health.feature` — not `phase5_*` |
| Frontend API: generated SDK + `apiCallWithLoading`; check `!error` before `data` | Mandatory for Run |
| No `blockUi` cancelable unless allowlisted | Use thin-bar only for lint |
| E2E: capability-named features; `@wip` until green; targeted `--spec` only | D-20 |
| Planning: Behavior vs Structure; one observable behavior; stop-safe | Single Behavior phase delivering Health+Run+report |
| Do not hand-edit generated API packages | No regen unless backend changes (none) |
| Prefer `trash` over `rm -rf` when deleting files | If cleaning drafts |
| post-change-refactor before commit (execute-plan wrap-up) | Planner should leave time for cohesion pass |

## Sources

### Primary (HIGH confidence)

- `frontend/src/components/commons/WorkspaceReadmeSettingsTabs.vue` — live tab union and testids
- `frontend/src/pages/NotebookPageView.vue` — `v-else` Settings branch
- `frontend/src/pages/FolderPage.vue` — shared tabs without Health
- `packages/generated/doughnut-backend-api/sdk.gen.ts` + `types.gen.ts` — `NotebookHealthController.lint`, `LintData`, report types
- `backend/.../NotebookHealthController.java` — bodyless, `@Transactional(readOnly = true)`, write auth
- `backend/.../health/HealthRuleIds.java` + rule `title()` methods — stable ids/titles
- `frontend/src/components/recall/AssimilationSettings.vue`, `FailureReportPage.vue` — daisy-collapse patterns
- `.cursor/rules/frontend-api.mdc`, `frontend-testing.mdc`, `e2e-authoring.mdc`
- `.planning/phases/05-health-tab-and-run/05-CONTEXT.md` — locked decisions
- `.planning/research/ARCHITECTURE.md`, `PITFALLS.md` — UI thin surface; lint ≠ fix

### Secondary (MEDIUM confidence)

- E2E folder creation steps (`I create a folder named …`) for fixtures
- Landmark feature update approach (step wording discretion)

### Tertiary (LOW confidence)

- None material

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — fully in-repo; no new packages
- Architecture: HIGH — live components and API verified
- Pitfalls: HIGH — `v-else` trap, auto-run, folder tab leak, landmark drift identified in code

**Research date:** 2026-07-22
**Valid until:** 2026-08-22 (stable Vue/API surface; re-check if tabs component is refactored)
