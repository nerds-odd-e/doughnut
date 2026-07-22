# Phase 6: User-level defaults - Pattern Map

**Mapped:** 2026-07-22
**Files analyzed:** 12
**Analogs found:** 12 / 12

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `backend/.../db/migration/V300000232__add_health_remove_empty_folders_default.sql` | migration | CRUD (schema) | `V300000231__rename_index_content_to_readme_content.sql` + baseline `tinyint(1)` | role-match |
| `backend/.../entities/User.java` | model | CRUD | same file (`dailyAssimilationCount` / `spaceIntervals`) | exact |
| `backend/.../controllers/dto/UserDTO.java` | model (DTO) | request-response | same file (`dailyAssimilationCount`) | exact |
| `backend/.../controllers/UserController.java` | controller | request-response | same file `updateUser` | exact |
| `packages/generated/doughnut-backend-api/*` | config (generated) | request-response | regen via `pnpm generateTypeScript` | exact |
| `frontend/.../notebook/NotebookHealthPanel.vue` | component | request-response | same file (action bar) + `UserProfileForm.vue` (PATCH) + inject from `DoughnutApp.vue` | exact / role-match |
| `frontend/tests/.../NotebookHealthPanel.spec.ts` | test | request-response | same file + `UserProfileForm.spec.ts` + `RenderingHelper.withCurrentUser` | exact / role-match |
| `backend/.../UserControllerTest.java` | test | request-response | same file `updateUserSuccessfully` | exact |
| `e2e_test/features/notebooks/notebook_health.feature` | test (E2E) | request-response | same feature (Phase 5 scenarios) | exact |
| `e2e_test/start/pageObjects/notebookPage.ts` | utility (page object) | request-response | same file Health methods | exact |
| `e2e_test/step_definitions/notebook.ts` | test (steps) | request-response | same file Health steps | exact |
| `packages/doughnut-test-fixtures/src/UserBuilder.ts` (optional) | utility (fixture) | transform | same file default `User` shape | role-match |

**Do not modify for Health defaults (reference only):** `UserProfileForm.vue`, `DoughnutApp.vue` (already `provide`s `currentUser`; Health mutates the injected ref in place).

## Pattern Assignments

### `V300000232__add_health_remove_empty_folders_default.sql` (migration, CRUD)

**Analog:** `backend/src/main/resources/db/migration/V300000231__rename_index_content_to_readme_content.sql` (file shape) + baseline boolean columns

**Migration file shape** (`V300000231` lines 1-6):
```sql
-- Rename container-owned markdown from index_content to readme_content.
ALTER TABLE `notebook`
  CHANGE COLUMN `index_content` `readme_content` mediumtext COLLATE utf8mb4_unicode_ci;
```

**Boolean column style** (`V100000000__baseline.sql` line 365 / 598 pattern):
```sql
`spelling` tinyint(1) NOT NULL DEFAULT '0',
`is_contested` tinyint(1) NOT NULL DEFAULT '0',
```

**Prescriptive Phase 6 migration:**
```sql
-- V300000232__add_health_remove_empty_folders_default.sql
ALTER TABLE `user`
  ADD COLUMN `health_remove_empty_folders_default` tinyint(1) NOT NULL DEFAULT 0;
```

**Version rule:** Tip is `V300000231`; next must be **`V300000232` or higher**. Never edit applied migrations. After migrate: regenerate `docs/database-erd.md` via `database-erd` skill.

---

### `User.java` (model, CRUD)

**Analog:** `backend/src/main/java/com/odde/doughnut/entities/User.java` preference columns

**Imports / entity header** (lines 1-16):
```java
package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user")
public class User extends EntityIdentifiedByIdOnly {
```

**Preference column pattern** (lines 37-45) — copy for the new boolean:
```java
  @Column(name = "daily_assimilation_count")
  @Getter
  @Setter
  private Integer dailyAssimilationCount = 15;

  @Column(name = "space_intervals")
  @Getter
  @Setter
  private String spaceIntervals = "0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55";
```

**Add after existing prefs:**
```java
  @Column(name = "health_remove_empty_folders_default")
  @Getter
  @Setter
  private Boolean healthRemoveEmptyFoldersDefault = false;
```

Jackson serializes via Lombok getters on returned `User` — no extra `@JsonProperty` needed (same as other prefs).

---

### `UserDTO.java` (model/DTO, request-response)

**Analog:** `backend/src/main/java/com/odde/doughnut/controllers/dto/UserDTO.java`

**DTO shell + optional fields** (lines 10-24):
```java
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDTO {

  @NotNull
  @Size(min = 1, max = 100)
  @Getter
  @Setter
  private String name;

  @Getter @Setter private Integer dailyAssimilationCount = 15;

  @Pattern(regexp = "^\\d+((,\\s*\\d+){1,1000})*$", message = "must be numbers separated by ','")
  @Getter
  @Setter
  private String spaceIntervals = "0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55";
}
```

**Add (optional Boolean, no `@NotNull` — D discretion):**
```java
  @Getter @Setter private Boolean healthRemoveEmptyFoldersDefault = false;
```

**Pitfall:** `updateUser` always copies mapped fields; omitted JSON properties become DTO Java defaults. PATCH bodies must send full preference set (see UserProfileForm / Health Save).

---

### `UserController.java` (controller, request-response)

**Analog:** same file `updateUser` (lines 84-95)

**Auth + mapping pattern:**
```java
  @PatchMapping("/{user}")
  @Transactional
  public User updateUser(
      @PathVariable @Schema(type = "integer") User user, @Valid @RequestBody UserDTO updates)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(user);
    user.setName(updates.getName());
    user.setSpaceIntervals(updates.getSpaceIntervals());
    user.setDailyAssimilationCount(updates.getDailyAssimilationCount());
    entityPersister.save(user);
    return user;
  }
```

**Extend mapping (null-safe default false):**
```java
    user.setHealthRemoveEmptyFoldersDefault(
        updates.getHealthRemoveEmptyFoldersDefault() != null
            ? updates.getHealthRemoveEmptyFoldersDefault()
            : false);
```

Keep `assertAuthorization(user)` unchanged. `GET /api/user` and `CurrentUserInfo` already return full `User` — no new endpoints.

---

### `packages/generated/doughnut-backend-api/*` (generated client)

**Analog:** generate-api-client skill — never hand-edit

**Current `User` type** (`types.gen.ts` lines 79-87) — will gain field after regen:
```typescript
export type User = {
    id: number;
    name: string;
    externalIdentifier: string;
    ownership?: Ownership;
    dailyAssimilationCount?: number;
    spaceIntervals?: string;
    admin?: boolean;
};
```

**Command:** `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` after backend `User`/`UserDTO` change. Same for `UserDto` PATCH body type.

---

### `NotebookHealthPanel.vue` (component, request-response)

**Analogs:**
1. Same file — action bar + local checkbox (extend)
2. `UserProfileForm.vue` — `apiCallWithLoading` + `UserController.updateUser` full body
3. `DoughnutApp.vue` / inject consumers — `inject<Ref<User | undefined>>("currentUser")`
4. `NotebookButtons.vue` — secondary density `daisy-btn-ghost daisy-btn-sm` beside primary actions

**Existing action bar** (`NotebookHealthPanel.vue` lines 6-22):
```vue
    <div class="flex flex-wrap items-center gap-2 mb-6">
      <button
        type="button"
        class="daisy-btn daisy-btn-primary daisy-btn-sm"
        data-testid="notebook-health-run"
        @click="runLint"
      >
        Run lint
      </button>
      <div data-testid="notebook-health-remove-empty-folders">
        <CheckInput
          scope-name="notebook-health"
          field="removeEmptyFolders"
          title="Remove empty folders"
          v-model="removeEmptyFolders"
        />
      </div>
    </div>
```

**Add Save control** (primary stays on Run lint; Save = ghost/secondary sm):
```vue
      <button
        type="button"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        data-testid="notebook-health-save-defaults"
        @click="saveAsDefaults"
      >
        Save as defaults
      </button>
```

**Ghost sm density analog** (`NotebookButtons.vue` lines 3-7):
```vue
    <button
      type="button"
      class="daisy-btn daisy-btn-ghost daisy-btn-sm"
```

**Provide/inject session user** (`DoughnutApp.vue` lines 29-30, 53-58):
```typescript
const user = ref<User | undefined>()
provide("currentUser", user)
// ...
user.value = userInfo!.user
```

**Inject consumer pattern** (`Sidebar.vue` line 104 / `HomePage.vue` line 45):
```typescript
const currentUser = inject<Ref<User | undefined>>("currentUser")
```

**PATCH + loading pattern** (`UserProfileForm.vue` lines 86-103):
```typescript
  const { data: updatedUser, error } = await apiCallWithLoading(() =>
    UserController.updateUser({
      path: { user: userData.id },
      body: userData,
    })
  )
  if (error) {
    const errorObj = toOpenApiError(error)
    errors.value = errorObj.errors || {}
  } else {
    errors.value = {}
    emits("user-updated", updatedUser)
  }
```

**Health Save (prescriptive — mutate inject in place, no emit to MainMenu):**
```typescript
import { inject, onMounted, ref, type Ref } from "vue"
import type { User } from "@generated/doughnut-backend-api"
import { UserController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

const currentUser = inject<Ref<User | undefined>>("currentUser")
const removeEmptyFolders = ref(false)

onMounted(() => {
  removeEmptyFolders.value =
    currentUser?.value?.healthRemoveEmptyFoldersDefault ?? false
})

async function saveAsDefaults() {
  const user = currentUser?.value
  if (!user) return
  const { data, error } = await apiCallWithLoading(() =>
    UserController.updateUser({
      path: { user: user.id },
      body: {
        name: user.name,
        dailyAssimilationCount: user.dailyAssimilationCount,
        spaceIntervals: user.spaceIntervals,
        healthRemoveEmptyFoldersDefault: removeEmptyFolders.value,
      },
    })
  )
  if (!error && currentUser) {
    currentUser.value = data!
  }
}
```

**Remount trigger** (`NotebookPageView.vue` lines 44-47) — `onMounted` is enough when Health tab is shown:
```vue
    <NotebookHealthPanel
      v-else-if="activeTab === 'health'"
      :notebook-id="notebook.id"
    />
```

**Do not:** call lint on mount/save; auto-save on checkbox toggle; add fields to `UserProfileForm`; send defaults on lint body.

---

### `NotebookHealthPanel.spec.ts` (test, request-response)

**Analogs:** same file + `UserProfileForm.spec.ts` + `RenderingHelper.withCurrentUser`

**Existing mount / no-lint-on-mount** (lines 46-74):
```typescript
  beforeEach(() => {
    vi.restoreAllMocks()
    lintSpy = mockSdkService(NotebookHealthController, "lint", reportFixture)
  })

  function mountPanel() {
    return helper
      .component(NotebookHealthPanel)
      .withProps({ notebookId })
      .mount()
  }

  it("shows idle prompt and action bar without calling lint on mount", async () => {
    const wrapper = mountPanel()
    await flushPromises()
    expect(lintSpy).not.toHaveBeenCalled()
  })
```

**Inject helper** (`RenderingHelper.ts` lines 70-73):
```typescript
  withCurrentUser(user: User) {
    this.global.provide.currentUser = ref(user)
    return this
  }
```

**updateUser mock + emit assertion** (`UserProfileForm.spec.ts` lines 48-62):
```typescript
    mockSdkService(UserController, "updateUser", updatedUser)
    // ...
    expect(wrapper.emitted("user-updated")).toEqual([[updatedUser]])
```

**Prefill / Save cases to add:**
```typescript
  it("prefills Remove empty folders from currentUser without calling lint", async () => {
    const wrapper = helper
      .component(NotebookHealthPanel)
      .withProps({ notebookId })
      .withCurrentUser({
        ...makeMe.aUser.please(),
        healthRemoveEmptyFoldersDefault: true,
      })
      .mount()
    await flushPromises()
    const checkbox = wrapper.get(
      '[data-testid="notebook-health-remove-empty-folders"] input[type="checkbox"]'
    )
    expect((checkbox.element as HTMLInputElement).checked).toBe(true)
    expect(lintSpy).not.toHaveBeenCalled()
  })

  // Save: mockSdkService(UserController, "updateUser", ...); click save-defaults;
  // assert body includes name + prefs + healthRemoveEmptyFoldersDefault;
  // assert lintSpy not called; assert injected currentUser.value updated
```

---

### `UserControllerTest.java` (test, request-response)

**Analog:** same file `updateUserSuccessfully` (lines 45-55)

```java
  @Test
  void updateUserSuccessfully() throws UnexpectedNoAccessRightException {
    UserDTO dto = new UserDTO();
    dto.setName("new name");
    dto.setSpaceIntervals("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15");
    dto.setDailyAssimilationCount(12);
    User response = controller.updateUser(currentUser.getUser(), dto);
    assertThat(response.getName(), equalTo(dto.getName()));
    assertThat(response.getSpaceIntervals(), equalTo(dto.getSpaceIntervals()));
    assertThat(response.getDailyAssimilationCount(), equalTo(dto.getDailyAssimilationCount()));
  }
```

**Extend / add:**
```java
  @Test
  void updateUserPersistsHealthRemoveEmptyFoldersDefault() throws UnexpectedNoAccessRightException {
    UserDTO dto = new UserDTO();
    dto.setName(currentUser.getUser().getName());
    dto.setSpaceIntervals(currentUser.getUser().getSpaceIntervals());
    dto.setDailyAssimilationCount(currentUser.getUser().getDailyAssimilationCount());
    dto.setHealthRemoveEmptyFoldersDefault(true);

    User response = controller.updateUser(currentUser.getUser(), dto);
    assertThat(response.getHealthRemoveEmptyFoldersDefault(), equalTo(true));
    assertThat(controller.getUserProfile().getHealthRemoveEmptyFoldersDefault(), equalTo(true));
  }

  @Test
  void newUserHealthRemoveEmptyFoldersDefaultIsFalse() {
    assertThat(controller.getUserProfile().getHealthRemoveEmptyFoldersDefault(), equalTo(false));
  }
```

Keep `updateOtherUserProfile` auth regression unchanged.

---

### `notebook_health.feature` + page object + steps (E2E)

**Analogs:** existing Phase 5 Health feature / `notebookPage.ts` / `notebook.ts` steps

**Feature scenario style** (`notebook_health.feature` lines 24-32):
```gherkin
  Scenario: Run lint with Remove empty folders checked does not delete folders
    Given I have a notebook "Health no-mutate suite" with a note "Anchor"
    ...
    And I check Remove empty folders on the notebook health panel
    And I run notebook health lint
    Then I should see sidebar folder "Keep Empty"
```

**Add `@wip` until green:**
```gherkin
  @wip
  Scenario: Save Remove empty folders default applies on another notebook
    Given I have a notebook "Defaults A" with a note "A1"
    And I have a notebook "Defaults B" with a note "B1"
    When I open the notebook "Defaults A" from my notebooks catalog
    And I open the notebook workspace Health tab
    And I check Remove empty folders on the notebook health panel
    And I save notebook health options as defaults
    And I open the notebook "Defaults B" from my notebooks catalog
    And I open the notebook workspace Health tab
    Then Remove empty folders on the notebook health panel is checked
```

**Page object Health methods** (`notebookPage.ts` lines 38-58) — extend:
```typescript
    checkRemoveEmptyFolders() {
      cy.get(
        '[data-testid="notebook-health-remove-empty-folders"] input[type="checkbox"]'
      ).check({ force: true })
      return this
    },

    // Add:
    saveAsDefaults() {
      cy.get('[data-testid="notebook-health-save-defaults"]').click()
      pageIsNotLoading()
      return this
    },

    expectRemoveEmptyFoldersChecked() {
      cy.get(
        '[data-testid="notebook-health-remove-empty-folders"] input[type="checkbox"]'
      ).should('be.checked')
      return this
    },
```

**Steps** (`notebook.ts` lines 120-130) — mirror existing When/Then wiring:
```typescript
When('I check Remove empty folders on the notebook health panel', () => {
  notebookPage().checkRemoveEmptyFolders()
})
// Add When save + Then checked → notebookPage().saveAsDefaults() / expectRemoveEmptyFoldersChecked()
```

**Targeted run only:** `pnpm cypress run --spec e2e_test/features/notebooks/notebook_health.feature`

---

### `packages/doughnut-test-fixtures/src/UserBuilder.ts` (optional fixture)

**Analog:** same file default User shape (lines 10-18)

```typescript
    this.data = {
      id: generateId(),
      name: 'a name',
      externalIdentifier: `user ${generateId()}`,
      ownership: { id: 0 },
      dailyAssimilationCount: 5,
      spaceIntervals: '',
      admin: false,
    }
```

After OpenAPI regen, optionally default `healthRemoveEmptyFoldersDefault: false` so `makeMe.aUser.please()` stays type-complete. Backend `UserBuilder.java` already inherits entity field defaults — optional fluent setter only if tests need it.

## Shared Patterns

### User preference persistence
**Source:** `User` + `UserDTO` + `UserController.updateUser`  
**Apply to:** migration, entity, DTO, controller, Health Save  
- Column on `user` table ↔ `@Column` on entity ↔ field on `UserDTO` ↔ one setter in `updateUser`  
- Do **not** use `NotebookSettings` for cross-notebook defaults

### Session user inject
**Source:** `DoughnutApp.vue` provide + inject consumers  
**Apply to:** `NotebookHealthPanel` prefill + post-Save refresh  
```typescript
provide("currentUser", user)           // DoughnutApp
const currentUser = inject<Ref<User | undefined>>("currentUser")
currentUser.value = data!              // after successful PATCH
```

### Frontend API calls
**Source:** `frontend-api.mdc` + `UserProfileForm.vue` / existing Health lint  
**Apply to:** Save as defaults  
```typescript
const { data, error } = await apiCallWithLoading(() => UserController.updateUser(...))
if (!error) { /* use data */ }
```
Generated SDK only; check `error` before using `data`.

### Authorization on PATCH
**Source:** `UserController.updateUser` line 89  
**Apply to:** unchanged — keep `authorizationService.assertAuthorization(user)`

### Full UserDTO body on PATCH
**Source:** `UserProfileForm` sending full `userData`; Research Pitfall 2  
**Apply to:** Health Save — always include `name`, `dailyAssimilationCount`, `spaceIntervals`, and the new boolean

### E2E capability naming + `@wip`
**Source:** `e2e-authoring.mdc` + existing `notebook_health.feature`  
**Apply to:** cross-notebook defaults scenario — extend capability-named feature; tag `@wip` until green; page objects over raw selectors in steps

### Action-bar visual hierarchy
**Source:** Phase 5 Health UI-SPEC + `NotebookHealthPanel` primary Run lint  
**Apply to:** Save as defaults — `daisy-btn-ghost daisy-btn-sm` (or secondary), not primary

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | All Phase 6 files have close in-repo analogs |

Optional / out of scope (no required new file):
- New prefs service / HealthUserDefaultsService — rejected; map in `updateUser`
- Fix/purge endpoint — Phase 7
- Health fields in `UserProfileForm` — deferred D-08

## Metadata

**Analog search scope:**  
`backend/src/main/java/com/odde/doughnut/{entities,controllers,controllers/dto}`,  
`backend/src/main/resources/db/migration`,  
`backend/src/test/java/com/odde/doughnut/controllers`,  
`frontend/src/components/{notebook,toolbars}`, `frontend/src/DoughnutApp.vue`,  
`frontend/src/pages/NotebookPageView.vue`,  
`frontend/tests/{components/notebook,toolbars,helpers}`,  
`e2e_test/{features/notebooks,start/pageObjects,step_definitions}`,  
`packages/{generated/doughnut-backend-api,doughnut-test-fixtures}`

**Files scanned:** ~25 primary + grep hits  
**Pattern extraction date:** 2026-07-22
