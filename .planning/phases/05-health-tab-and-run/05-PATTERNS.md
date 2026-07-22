# Phase 5: Health tab and Run - Pattern Map

**Mapped:** 2026-07-22
**Files analyzed:** 12
**Analogs found:** 12 / 12

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `frontend/src/components/commons/WorkspaceReadmeSettingsTabs.vue` | component | request-response | self (extend in place) | exact |
| `frontend/src/pages/NotebookPageView.vue` | page | request-response | self + `NotebookWorkspaceSettings.vue` mount pattern | exact |
| `frontend/src/pages/FolderPage.vue` | page | request-response | self (leave default; no Health) | exact |
| `frontend/src/components/notebook/NotebookHealthPanel.vue` | component | request-response | `NotebookWorkspaceSettings.vue` | role-match |
| `frontend/src/components/notebook/NotebookHealthFindings.vue` | component | transform | `AssimilationSettings.vue` + `FailureReportPage.vue` | role-match |
| `frontend/tests/pages/NotebookPageView.spec.ts` | test | request-response | self (tab click + panel assertions) | exact |
| `frontend/tests/components/notebook/NotebookHealthPanel.spec.ts` | test | request-response | `NotebookAttachedBookSection.spec.ts` + `mockSdkService` | role-match |
| Folder Health-absent assertion (extend existing Folder/Notebook page specs) | test | request-response | `NotebookPageView.spec.ts` tab assertions | role-match |
| `e2e_test/features/notebooks/notebook_health.feature` | test | request-response | `notebook_workspace_readme.feature` | role-match |
| `e2e_test/start/pageObjects/notebookPage.ts` | utility | request-response | self (`openSettingsTab`) | exact |
| `e2e_test/start/pageObjects/workspaceSurfaceLandmarks.ts` | utility | request-response | self (`expectNotebookWorkspaceTabsPresent`) | exact |
| `e2e_test/features/note_view/workspace_surface_landmarks.feature` + step defs | test | request-response | self (landmark steps) | exact |

**Consume only (do not modify):** `packages/generated/doughnut-backend-api/sdk.gen.ts` (`NotebookHealthController.lint`), `types.gen.ts` (`NotebookHealthLintReport`, `HealthFindingGroup`, `HealthFindingItem`), `CheckInput.vue`.

## Pattern Assignments

### `frontend/src/components/commons/WorkspaceReadmeSettingsTabs.vue` (component, request-response)

**Analog:** self — extend tab union and optional third tab.

**Current tab shell** (lines 1–39):
```vue
<a
  :class="tabClass('settings')"
  role="button"
  href="#"
  :data-testid="`${testIdPrefix}-tab-settings`"
  @click.prevent="model = 'settings'"
>Settings</a>
```

```typescript
export type WorkspaceReadmeSettingsTab = "readme" | "settings"

const model = defineModel<WorkspaceReadmeSettingsTab>({ required: true })

withDefaults(
  defineProps<{
    testIdPrefix?: string
  }>(),
  {
    testIdPrefix: "workspace",
  }
)
```

**Copy this pattern for Health:**
- Extend type: `"readme" | "settings" | "health"`.
- Add prop `includeHealth?: boolean` default `false` via `withDefaults`.
- Render Health `<a>` only when `includeHealth` is true, same `tabClass` / `role="button"` / `href="#"` / `@click.prevent` / `data-testid` shape:
  - testid: `` `${testIdPrefix}-tab-health` `` → `notebook-workspace-tab-health` when prefix is `notebook-workspace`.
- Do **not** change FolderPage usage (default keeps Health off).

---

### `frontend/src/pages/NotebookPageView.vue` (page, request-response)

**Analog:** self — tabs mount + panel switch; must replace Settings `v-else`.

**Current tabs + two-way branch** (lines 21–41) — **anti-pattern for Health if left as `v-else`:**
```vue
<WorkspaceReadmeSettingsTabs
  v-model="activeTab"
  test-id-prefix="notebook-workspace"
/>

<div v-if="activeTab === 'readme'" data-testid="notebook-workspace-readme">
  <ScopedReadmeEditor … />
</div>

<NotebookWorkspaceSettings
  v-else
  :notebook="notebook"
  …
/>
```

**Required three-way branch** (from RESEARCH; mirrors Settings mount style):
```vue
<WorkspaceReadmeSettingsTabs
  v-model="activeTab"
  test-id-prefix="notebook-workspace"
  include-health
/>

<div v-if="activeTab === 'readme'" data-testid="notebook-workspace-readme">…</div>
<NotebookWorkspaceSettings
  v-else-if="activeTab === 'settings'"
  :notebook="notebook"
  …
/>
<NotebookHealthPanel
  v-else-if="activeTab === 'health'"
  :notebook-id="notebook.id"
  data-testid="notebook-workspace-health"
/>
```

**Imports pattern** (lines 46–54) — add Health panel import alongside existing notebook components:
```typescript
import NotebookWorkspaceSettings from "@/components/notebook/NotebookWorkspaceSettings.vue"
import WorkspaceReadmeSettingsTabs, {
  type WorkspaceReadmeSettingsTab,
} from "@/components/commons/WorkspaceReadmeSettingsTabs.vue"
```

**Default tab** (line 75) — keep `"readme"`:
```typescript
const activeTab = ref<WorkspaceReadmeSettingsTab>("readme")
```

---

### `frontend/src/pages/FolderPage.vue` (page, request-response)

**Analog:** self — **no product change**. Shared tabs without `include-health`.

**Current usage** (lines 21–24) — leave as-is (Health stays off via prop default):
```vue
<WorkspaceReadmeSettingsTabs
  v-model="activeTab"
  test-id-prefix="folder-workspace"
/>
```

**Assert in tests/E2E:** `folder-workspace-tab-health` must not exist.

---

### `frontend/src/components/notebook/NotebookHealthPanel.vue` (component, request-response)

**Analog:** `frontend/src/components/notebook/NotebookWorkspaceSettings.vue` — notebook-scoped panel sibling: `data-testid`, section layout, `CheckInput`, bodyless/path-only SDK calls via `apiCallWithLoading`.

**Panel shell + testid** (lines 1–5):
```vue
<div
  class="notebook-workspace-settings"
  data-testid="notebook-workspace-settings"
>
```
→ Health: `data-testid="notebook-workspace-health"` (panel root) and action controls per D-18.

**CheckInput for fix option** (lines 69–75) — UI-only checkbox in Phase 5:
```vue
<CheckInput
  scope-name="notebook"
  field="skipMemoryTrackingEntirely"
  title="Skip Memory Tracking"
  v-model="settingsBody.skipMemoryTrackingEntirely"
  :error-message="errors.skipMemoryTrackingEntirely"
/>
```
→ Health: local `ref(false)` for “Remove empty folders”; `data-testid="notebook-health-remove-empty-folders"` (on input or wrapper). Do **not** send to API. No Fix/Apply button.

**Imports + path-only SDK call + `!error` guard** (lines 121–129, 160–169, 173–180):
```typescript
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

const { error } = await apiCallWithLoading(() =>
  NotebookController.shareNotebook({
    path: { notebook: props.notebook.id },
  })
)
if (!error) {
  // use result / update UI
}

const { data: updatedNotebook, error } = await apiCallWithLoading(() =>
  NotebookController.updateNotebook({
    path: { notebook: props.notebook.id },
    body: props.settingsBody,
  })
)
if (!error) {
  emit("notebook-updated", updatedNotebook!)
}
```

**Health Run (bodyless lint)** — same wrapper, **no `body`**, thin bar only (no `blockUi`):
```typescript
import { NotebookHealthController } from "@generated/doughnut-backend-api/sdk.gen"
import type { NotebookHealthLintReport } from "@generated/doughnut-backend-api"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

const report = ref<NotebookHealthLintReport | null>(null)
const removeEmptyFolders = ref(false) // UI-only; never sent

async function runLint() {
  const { data, error } = await apiCallWithLoading(() =>
    NotebookHealthController.lint({
      path: { notebook: props.notebookId },
    })
  )
  if (!error) {
    report.value = data
  }
}
```

**Generated contract** (`packages/generated/doughnut-backend-api/types.gen.ts` lines 1954–1968):
```typescript
export type LintData = {
    body?: never;
    path: {
        notebook: number;
    };
    query?: never;
    url: '/api/notebooks/{notebook}/health/lint';
};
```

**Core panel behavior (implement against):**
- Idle (`report === null`): prompt + action bar (Run lint + checkbox); `data-testid="notebook-health-idle"`.
- After successful Run: replace idle with findings; Re-Run replaces report (ephemeral).
- **Never** call lint in `onMounted` / `watch(activeTab)`.

---

### `frontend/src/components/notebook/NotebookHealthFindings.vue` (component, transform)

**Analog:** `frontend/src/components/recall/AssimilationSettings.vue` (collapse + checkbox open state) and `frontend/src/pages/FailureReportPage.vue` (default-open collapse).

**Expandable section** (`AssimilationSettings.vue` lines 33–44):
```vue
<div
  class="daisy-collapse daisy-collapse-arrow border border-base-300 bg-base-200/50 rounded-lg"
>
  <input
    v-model="propertiesSectionOpen"
    type="checkbox"
    data-test="assimilation-properties-toggle"
  />
  <div class="daisy-collapse-title min-h-0 py-3 text-sm font-medium">
    Properties
  </div>
  <div class="daisy-collapse-content">
    …
  </div>
</div>
```

**Default-open via checked input** (`FailureReportPage.vue` lines 125–147):
```vue
<div class="daisy-collapse daisy-collapse-arrow bg-base-200">
  <input type="checkbox" checked />
  <div class="daisy-collapse-title font-medium flex items-center gap-2">
    Error Details
  </div>
  <div class="daisy-collapse-content">
    …
  </div>
</div>
```

**Wire-shape render** (do not re-group):
```vue
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
    <!-- flat: group.items → item.label -->
    <!-- nested: group.children → collapse per child (title = note); items → label -->
  </div>
</div>
```

**DTO types** (`types.gen.ts` lines 391–446):
```typescript
export type HealthFindingGroup = {
    ruleId: string;
    title: string;
    severity: 'error' | 'warning' | 'info';
    autoFixable: boolean;
    items?: Array<HealthFindingItem>;
    children?: Array<HealthFindingGroup>;
};

export type HealthFindingItem = {
    folderId?: number;
    noteId?: number;
    label?: string;
    message?: string;
    wikiLinkToken?: string;
};

export type NotebookHealthLintReport = {
    groups?: Array<HealthFindingGroup>;
};
```

**Rules:** render all `groups`; open when findings exist; empty groups collapsed (count `0`); leaf labels only (no click-through); prefer leaf count = items + sum(children.items).

Optional: inline findings in `NotebookHealthPanel` if panel stays small — same collapse pattern either way.

---

### `frontend/tests/pages/NotebookPageView.spec.ts` (test, request-response)

**Analog:** self — tab click, panel presence, settings absent on first paint.

**Tab click → panel** (lines 122–149):
```typescript
await wrapper
  .get('[data-testid="notebook-workspace-tab-settings"]')
  .trigger("click")
await flushPromises()

const settings = wrapper.find('[data-testid="notebook-workspace-settings"]')
expect(settings.exists()).toBe(true)
```

**Extend with:**
- Health tab present: `notebook-workspace-tab-health`.
- Click Health → `notebook-workspace-health` exists; `notebook-workspace-settings` **absent** (guards `v-else` trap).
- Default tab remains readme; Health not active on first paint.
- Optional: `mockSdkService(NotebookHealthController, "lint", …)` and assert **not** called when only opening Health (if panel mounts on tab open).

**Mount helper** (lines 37–41):
```typescript
const wrapper = helper
  .component(NotebookPageView)
  .withRouter()
  .withProps({ notebook: nb, fetchNotebookPage: noopFetchNotebookPage })
  .mount()
```

---

### `frontend/tests/components/notebook/NotebookHealthPanel.spec.ts` (test, request-response)

**Analog:** `frontend/tests/helpers/index.ts` `mockSdkService` + notebook component specs that assert idle vs after action.

**Mock pattern** (`frontend/tests/helpers/index.ts` lines 99–110):
```typescript
export function mockSdkService<…>(controller, methodName, data) {
  return vi.spyOn(controller, methodName as any)
    .mockResolvedValue(wrapSdkResponse(data) as any)
}
```

**Health-specific mock usage:**
```typescript
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

// mount panel → expect(lintSpy).not.toHaveBeenCalled()
// click [data-testid="notebook-health-run"] → expect(lintSpy).toHaveBeenCalledOnce()
// expect call options: path { notebook: id }, no body
// assert groups + nested dead-link labels
// checkbox on → still path-only; no Fix control in DOM
```

Mount with `helper.component(NotebookHealthPanel).withProps({ notebookId }).mount()` (no router unless needed).

---

### Folder Health-absent assertion (test, request-response)

**Analog:** `NotebookPageView.spec.ts` negative assertions (lines 110–112):
```typescript
expect(
  wrapper.find('[data-testid="notebook-workspace-settings"]').exists()
).toBe(false)
```

**Apply to FolderPage mount:** assert `folder-workspace-tab-health` does not exist; `folder-workspace-tab-readme` / `folder-workspace-tab-settings` remain. Can live in an existing Folder page spec or a small tabs-focused assertion — smallest addition that covers D-03.

---

### `e2e_test/features/notebooks/notebook_health.feature` (test, request-response)

**Analog:** `e2e_test/features/notebooks/notebook_workspace_readme.feature` — capability-named notebook feature, Background login, open notebook from catalog, tab interactions.

**Structure pattern** (lines 1–29):
```gherkin
Feature: Notebook workspace readme
  As a learner, I want …

  Background:
    Given I am logged in as an existing user

  Scenario: Settings tab reveals notebook admin sections
    Given I have a notebook "Workspace settings suite"
    When I open the notebook "Workspace settings suite" from my notebooks catalog
    And I open the notebook workspace Settings tab
    Then notebook admin settings sections are visible
```

**Health feature:** tag `@wip` until green; scenarios: open Health → idle → Run lint → expandable groups for seeded empty folder / readme-only / dead links; AFIX-01: checkbox on + Run leaves folder present. Capability name `notebook_health` — no phase numbers.

---

### `e2e_test/start/pageObjects/notebookPage.ts` (utility, request-response)

**Analog:** self — `openSettingsTab` pattern.

**Tab open pattern** (lines 10–13, 27–30):
```typescript
const openSettingsTab = () => {
  cy.get('[data-testid="notebook-workspace-tab-settings"]').click()
  cy.get('[data-testid="notebook-workspace-settings"]').should('be.visible')
}

return {
  openSettingsTab() {
    openSettingsTab()
    return this
  },
```

**Add:**
```typescript
openHealthTab() {
  cy.get('[data-testid="notebook-workspace-tab-health"]').click()
  cy.get('[data-testid="notebook-workspace-health"]').should('be.visible')
  return this
},
runLint() {
  cy.get('[data-testid="notebook-health-run"]').click()
  return this
},
// expectFindingGroup(ruleId), expectIdle(), etc.
```

Use `data-testid` selectors (project prefers testids; avoid new `findByRole` for Health controls).

---

### `e2e_test/start/pageObjects/workspaceSurfaceLandmarks.ts` (utility, request-response)

**Analog:** self — notebook tab presence assertions.

**Current notebook tabs** (lines 7–17) — extend to require Health; keep folder at two tabs only:
```typescript
expectNotebookWorkspaceTabsPresent() {
  cy.get('[data-testid="notebook-page-kind-label"]').should(
    'contain.text',
    'Notebook'
  )
  cy.get('[data-testid="notebook-workspace-tabs"]').should('be.visible')
  cy.get('[data-testid="notebook-workspace-tab-readme"]').should('be.visible')
  cy.get('[data-testid="notebook-workspace-tab-settings"]').should(
    'be.visible'
  )
  // ADD: notebook-workspace-tab-health visible
  return this
},

expectFolderWorkspaceTabsPresent() {
  // ADD assertion: folder-workspace-tab-health should('not.exist')
  …
}
```

**Absent helpers** (lines 20–27): also assert Health tab absent when notebook tabs are absent.

---

### Landmark feature / step wording (test, request-response)

**Analog:** `e2e_test/features/note_view/workspace_surface_landmarks.feature` + `e2e_test/step_definitions/notebook.ts`.

**Current step** (landmarks feature line 14; `notebook.ts` ~124):
```gherkin
And notebook Readme and Settings tabs are present
```

```typescript
Then('notebook Readme and Settings tabs are present', () => {
  workspaceSurfaceLandmarks().expectNotebookWorkspaceTabsPresent()
})
```

Update expectations so notebook includes Health; folder remains Readme | Settings only. Prefer extending assertions (and step text if needed) over inventing a second landmark path.

## Shared Patterns

### Frontend API (user-initiated Run)

**Source:** `.cursor/rules/frontend-api.mdc` + `NotebookWorkspaceSettings.vue`  
**Apply to:** `NotebookHealthPanel.vue`

- Import controllers from `@generated/doughnut-backend-api/sdk.gen`.
- Wrap Run with `apiCallWithLoading` (thin bar; **no** `blockUi`).
- Destructure `{ data, error }`; only assign report when `!error`.
- Errors: global toasts from wrapper — no local LoadingModal.

```typescript
const { data, error } = await apiCallWithLoading(() =>
  NotebookHealthController.lint({
    path: { notebook: props.notebookId },
  })
)
if (!error) {
  report.value = data
}
```

### Panel sibling layout

**Source:** `NotebookWorkspaceSettings.vue`  
**Apply to:** Health panel mount from `NotebookPageView`

- Capability-named under `frontend/src/components/notebook/`.
- Root `data-testid` for E2E (`notebook-workspace-health`).
- Section styling family: `bg-base-100 border border-base-300 rounded-lg p-6` when matching Settings chrome is useful; action bar can be simpler flex row.

### Expandable findings (DaisyUI collapse)

**Source:** `AssimilationSettings.vue`, `FailureReportPage.vue`  
**Apply to:** `NotebookHealthFindings.vue` (or inline in panel)

- `daisy-collapse daisy-collapse-arrow` + checkbox input for open state.
- Open by default when group has findings (`:checked` or `v-model`).
- Nest `children` as secondary collapses; leaves show `item.label` as text (Vue default escaping — no `v-html`).

### Form checkbox

**Source:** `CheckInput.vue` / Settings usage in `NotebookWorkspaceSettings.vue`  
**Apply to:** remove-empty-folders option

- Prefer `CheckInput` with `scope-name` / `field` / `title` + `v-model` local ref.
- Phase 5: local state only; never in lint options; no Fix button.

### Unit testing

**Source:** `frontend/tests/helpers/index.ts`, `NotebookPageView.spec.ts`  
**Apply to:** all Health unit tests

- `helper.component(…).withRouter().withProps(…).mount()` for page; `withProps` only for panel.
- `mockSdkService(NotebookHealthController, "lint", reportFixture)`.
- Selectors: `data-testid` via `wrapper.get` / `wrapper.find`.
- Assert **no** API on mount / tab open; assert path-only call options on Run.

### E2E authoring

**Source:** `notebook_workspace_readme.feature`, `notebookPage.ts`, `workspaceSurfaceLandmarks.ts`  
**Apply to:** Health E2E

- Capability-named feature under `e2e_test/features/notebooks/`.
- Page-object methods; `@wip` until green; targeted `--spec` only.
- Update landmarks so notebook expects Health; folder must not.

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | All Phase 5 files have in-repo analogs |

Health UI is a thin consumer of existing tabs, settings panels, collapse UI, and SDK+loading patterns. No greenfield architecture.

## Metadata

**Analog search scope:** `frontend/src/components/commons`, `frontend/src/components/notebook`, `frontend/src/components/recall`, `frontend/src/pages`, `frontend/src/components/form`, `frontend/tests`, `e2e_test`, `packages/generated/doughnut-backend-api`, `.cursor/rules/frontend-api.mdc`

**Files scanned:** ~25 primary sources (tabs, pages, settings panel, collapse UIs, CheckInput, SDK types, Vitest helpers/specs, E2E page objects/features)

**Pattern extraction date:** 2026-07-22
