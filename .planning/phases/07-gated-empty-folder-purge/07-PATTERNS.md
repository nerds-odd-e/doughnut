# Phase 7: Gated empty-folder purge - Pattern Map

**Mapped:** 2026-07-22
**Files analyzed:** 13
**Analogs found:** 13 / 13

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `backend/.../controllers/NotebookHealthController.java` | controller | request-response | same file (`lint`) + `NotebookController.dissolveFolder` (void mutate) | exact |
| `backend/.../dto/NotebookHealthFixRequest.java` | model (DTO) | request-response | `FolderCreationRequest.java` / `UserDTO.healthRemoveEmptyFoldersDefault` | exact |
| `backend/.../services/NotebookHealthService.java` | service | request-response → CRUD | same file (`lint`) + gate pattern in RESEARCH | exact |
| `backend/.../health/EmptyFolderBulkPurge.java` | service | CRUD (hard-delete) | `EmptyFolderHealthRule` (load graph) + `FolderRelocationService` (`entityPersister.remove` only) | role-match |
| `backend/.../health/FolderSubtreeLiveNotes.java` | utility | transform | same file (`noteEmptyFolderItems`, `childrenByParentId`) | exact |
| `backend/.../test/.../NotebookHealthControllerTest.java` | test | request-response | same file (`LintHealth` nested class) | exact |
| `backend/.../test/.../EmptyFolderBulkPurgeTest.java` | test | CRUD | `EmptyFolderHealthRuleTest.java` | exact |
| `frontend/.../NotebookHealthPanel.vue` | component | request-response | same file (action bar, `runLint`, `apiCallWithLoading`) | exact |
| `frontend/tests/.../NotebookHealthPanel.spec.ts` | test | request-response | same file (`mockSdkService`, Fix-absent assertions to replace) | exact |
| `e2e_test/features/notebooks/notebook_health.feature` | test | request-response | same file (lint / checkbox scenarios) | exact |
| `e2e_test/start/pageObjects/notebookPage.ts` | utility (page object) | request-response | same file (`runLint`, `checkRemoveEmptyFolders`) | exact |
| `e2e_test/step_definitions/notebook.ts` | test (steps) | request-response | same file (health step defs) | exact |
| `packages/generated/doughnut-backend-api/**`, `open_api_docs.yaml` | config (generated) | transform | `NotebookHealthController.lint` in `sdk.gen.ts` + generate-api-client skill | exact |

## Pattern Assignments

### `NotebookHealthController.java` (controller, request-response)

**Analog:** `backend/src/main/java/com/odde/doughnut/controllers/NotebookHealthController.java` (auth + delegate) and `NotebookController.dissolveFolder` (void writable mutate).

**Imports / class skeleton** (lines 1–29 of health controller):
```java
package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.NotebookHealthLintReport;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NotebookHealthService;
// ADD: NotebookHealthFixRequest, RequestBody, Valid
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/notebooks")
class NotebookHealthController {
  // constructor injection of AuthorizationService + NotebookHealthService
}
```

**Auth + read-only lint (do not mutate this path)** (lines 31–39):
```java
  @PostMapping("/{notebook}/health/lint")
  @Transactional(readOnly = true)
  public NotebookHealthLintReport lint(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    return notebookHealthService.lint(
        notebook, new HealthRunContext(authorizationService.getCurrentUser()));
  }
```

**Void mutate endpoint pattern** (`NotebookController.java` lines 213–221) — copy shape for fix, **not** dissolve semantics:
```java
  @DeleteMapping("/{notebook}/folders/{folder}")
  @Transactional
  public void dissolveFolder(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("folder") @Schema(type = "integer") Folder folder,
      @RequestParam(name = "merge", defaultValue = "false") boolean merge)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    folderRelocationService.dissolveFolder(notebook, folder, merge);
  }
```

**Copy for Phase 7 fix** (compose lint auth + dissolve void + `@Valid @RequestBody`):
```java
  @PostMapping("/{notebook}/health/fix")
  @Transactional // writable — NOT readOnly
  public void fix(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @Valid @RequestBody NotebookHealthFixRequest request)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    notebookHealthService.fix(notebook, request);
  }
```

**`@Valid @RequestBody` companion** (`NotebookController.updateNotebook` lines 224–228):
```java
  @PostMapping(value = "/{notebook}")
  @Transactional
  public Notebook updateNotebook(
      @PathVariable @Schema(type = "integer") Notebook notebook,
      @Valid @RequestBody NotebookUpdateRequest request)
```

---

### `NotebookHealthFixRequest.java` (model / DTO, request-response)

**Analog:** `FolderCreationRequest.java` (Lombok `@Getter/@Setter` + `@Schema`) and `UserDTO` Boolean field for the same flag name.

**Lombok + Schema DTO** (`FolderCreationRequest.java` lines 12–27):
```java
@Schema(
    description =
        "Create a folder under notebook root, nested under an explicit parent folder, or nested"
            + " under the folder of a context note.")
@Getter
@Setter
public class FolderCreationRequest {

  @NotBlank
  @Size(max = 512)
  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "Display name for the new folder")
  private String name;
```

**Boolean preference field naming** (`UserDTO.java` lines 26–26) — same product flag, but fix request must **require true**, not default false for mutation:
```java
  @Getter @Setter private Boolean healthRemoveEmptyFoldersDefault = false;
```

**Target shape for fix request** (from RESEARCH; no folder IDs):
```java
@Getter
@Setter
public class NotebookHealthFixRequest {
  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "Must be true to bulk-purge fully empty folder trees")
  private Boolean removeEmptyFolders;
}
```

Gate with `Boolean.TRUE.equals(request.getRemoveEmptyFolders())` (null-safe; used elsewhere e.g. `NoteController` line 121). Reject with `ResponseStatusException(HttpStatus.BAD_REQUEST, ...)` as in `FolderRelocationService` / `NoteConstructionService`.

---

### `NotebookHealthService.java` (service, request-response → CRUD)

**Analog:** same service’s thin `lint` delegation.

**Current orchestration** (lines 9–19):
```java
@Service
public class NotebookHealthService {
  private final HealthRuleRunner healthRuleRunner;

  public NotebookHealthService(HealthRuleRunner healthRuleRunner) {
    this.healthRuleRunner = healthRuleRunner;
  }

  public NotebookHealthLintReport lint(Notebook notebook, HealthRunContext context) {
    return healthRuleRunner.run(notebook, context);
  }
}
```

**Extend pattern:** inject `EmptyFolderBulkPurge`; add `fix(Notebook, NotebookHealthFixRequest)` that enforces opt-in then delegates:
```java
public void fix(Notebook notebook, NotebookHealthFixRequest request) {
  if (!Boolean.TRUE.equals(request.getRemoveEmptyFolders())) {
    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Fix requires removeEmptyFolders=true");
  }
  emptyFolderBulkPurge.apply(notebook);
}
```

Keep service thin; all delete logic in `EmptyFolderBulkPurge`.

---

### `EmptyFolderBulkPurge.java` (service, CRUD)

**Analog (load graph / predicate):** `EmptyFolderHealthRule.java`  
**Analog (hard-delete only):** `FolderRelocationService.dissolveFolder` final `entityPersister.remove` — **without** promote/reparent.

**Health rule load pattern** (`EmptyFolderHealthRule.java` lines 45–58):
```java
  public HealthFindingGroup evaluate(Notebook notebook, HealthRunContext context) {
    List<Folder> folders = folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId());
    Set<Integer> occupiedFolderIds =
        new HashSet<>(noteRepository.findLiveNoteFolderIdsByNotebookId(notebook.getId()));

    // ...
    group.setItems(
        FolderSubtreeLiveNotes.noteEmptyFolderItems(
            folders, occupiedFolderIds, FolderSubtreeLiveNotes::isBlankReadme));
```

**Constructor injection with repos + EntityPersister** (`FolderRelocationService.java` lines 25–47):
```java
@Service
public class FolderRelocationService {

  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;
  // ...
  private final EntityPersister entityPersister;

  public FolderRelocationService(
      FolderRepository folderRepository,
      NoteRepository noteRepository,
      // ...
      EntityPersister entityPersister,
      // ...
  ) {
```

**Hard-delete only (anti-pattern for Health: everything above this loop)** (`FolderRelocationService.java` lines 299–333) — purge must **skip** promote loops; keep only remove/flush:
```java
    // DO NOT copy promote of subfolders / live notes (lines 299–330)
    entityPersister.flush();
    entityPersister.remove(folder);
```

**Core purge algorithm** (RESEARCH Pattern 2 — implement using `FolderSubtreeLiveNotes` helpers):
```java
// 1. S = note-empty ∩ blank own readme (same as empty_folders)
// 2. D = { f ∈ S | all real descendants of f are also ∈ S }  // CASCADE-safe
// 3. Sort D deepest-first; entityPersister.remove each
// Never call dissolveFolder; never accept client folder IDs
```

**Package:** `com.odde.doughnut.services.health`, `@Service`, capability name `EmptyFolderBulkPurge`.

---

### `FolderSubtreeLiveNotes.java` (utility, transform)

**Analog:** same package-private helper.

**Existing reusable pieces** (lines 15–71):
```java
final class FolderSubtreeLiveNotes {
  private FolderSubtreeLiveNotes() {}

  private static Map<Integer, List<Folder>> childrenByParentId(List<Folder> folders) { ... }

  private static boolean subtreeHasLiveNotes(...) { ... }

  static boolean isBlankReadme(String readmeContent) {
    return readmeContent == null || readmeContent.isBlank();
  }

  static List<HealthFindingItem> noteEmptyFolderItems(
      List<Folder> folders,
      Set<Integer> occupiedFolderIds,
      Predicate<String> readmeContentMatches) {
    Map<Integer, List<Folder>> childrenByParentId = childrenByParentId(folders);
    // ...
  }
}
```

**Extend (package-private):** expose or add helpers purge needs without duplicating occupancy DFS:
- fully-empty `Folder` list / id set (note-empty + `isBlankReadme`)
- `allDescendantsInEmptySet(folder, emptyIds, childrenByParentId)` for subtree-safe `D`
- optionally depth / deepest-first ordering using the same children map

Keep package-private (`final class`, no `public`) unless tests need package access (same package tests under `services/health`).

**Rule id constant** when counting empty folders on client/tests: `HealthRuleIds.EMPTY_FOLDERS` (`"empty_folders"`).

---

### `NotebookHealthControllerTest.java` (test, request-response)

**Analog:** same file’s `LintHealth` nested class.

**Base setup** (lines 21–37):
```java
class NotebookHealthControllerTest extends ControllerTestBase {

  @Autowired NotebookHealthController controller;
  @Autowired FolderRepository folderRepository;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  private Notebook myNotebook() {
    return makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
  }

  private Notebook otherUsersNotebook() {
    return makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
  }
```

**Auth rejection pattern** (lines 72–85) — mirror for `fix`:
```java
    @Test
    void rejectsForeignUser() {
      Notebook otherNotebook = otherUsersNotebook();
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.lint(otherNotebook));
    }

    @Test
    void rejectsAnonymousUser() {
      Notebook notebook = myNotebook();
      currentUser.setUser(null);
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.lint(notebook));
    }
```

**No-mutation proof for lint** (lines 50–69) — invert for fix success (folder count drops):
```java
      int folderCountBefore =
          folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).size();
      // fix(...)
      List<Folder> foldersAfter = folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId());
      assertThat(foldersAfter, hasSize(/* fewer */));
```

**Add `@Nested class FixHealth`:** owner success with `removeEmptyFolders=true`; reject null/false body; foreign/anon reject; assert readme-only folder still present after purge.

---

### `EmptyFolderBulkPurgeTest.java` (test, CRUD)

**Analog:** `EmptyFolderHealthRuleTest.java` (SpringBootTest + MakeMe + health boundary).

**Test class skeleton** (lines 30–44):
```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmptyFolderHealthRuleTest {
  @Autowired NotebookHealthService notebookHealthService;
  @Autowired MakeMe makeMe;

  private User owner;
  private Notebook notebook;

  @BeforeEach
  void setup() {
    owner = makeMe.aUser().please();
    notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
  }
```

**Seed patterns to reuse:**
- Nested empty both listed (`listsEveryNestedFullyEmptyFolder`, lines 47–59) → both deleted by purge
- Readme-only excluded (`nonBlankReadmeExcludesFolderFromEmptyFolders`, lines 85–100) → never deleted
- Soft-deleted only (`softDeletedNoteDoesNotOccupyFolder`, lines 73–81) → purgeable
- Parent blank + child readme: parent may be in `empty_folders` findings but **must not** CASCADE-delete child (subtree-safe `D`)

Drive tests through `NotebookHealthService.fix` (or inject purge) and assert folder repository state — not dissolve promote.

---

### `NotebookHealthPanel.vue` (component, request-response)

**Analog:** same component action bar + `apiCallWithLoading`.

**Action bar layout** (lines 6–30) — insert Fix **after checkbox, before Save as defaults**:
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
        <CheckInput ... v-model="removeEmptyFolders" />
      </div>
      <!-- ADD Fix here: daisy-btn-secondary daisy-btn-sm, data-testid="notebook-health-fix" -->
      <button
        type="button"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        data-testid="notebook-health-save-defaults"
        @click="saveAsDefaults"
      >
        Save as defaults
      </button>
    </div>
```

**Imports + SDK call pattern** (lines 49–85):
```typescript
import {
  NotebookHealthController,
  UserController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

async function runLint() {
  const { data, error } = await apiCallWithLoading(() =>
    NotebookHealthController.lint({
      path: { notebook: props.notebookId },
    })
  )
  if (!error) {
    report.value = data!
  }
}
```

**Fix enablement + post-fix re-lint** (RESEARCH, extend panel):
```typescript
import { computed, inject, onMounted, ref, type Ref } from "vue"

const emptyFolderCount = computed(() => {
  const group = report.value?.groups?.find((g) => g.ruleId === "empty_folders")
  return group?.items?.length ?? 0
})
const fixEnabled = computed(
  () => removeEmptyFolders.value && emptyFolderCount.value > 0
)
const fixLabel = computed(() =>
  emptyFolderCount.value > 0
    ? `Remove ${emptyFolderCount.value} empty folders`
    : "Remove empty folders"
)

async function applyFix() {
  const { error } = await apiCallWithLoading(() =>
    NotebookHealthController.fix({
      path: { notebook: props.notebookId },
      body: { removeEmptyFolders: true },
    })
  )
  if (!error) {
    await runLint()
  }
  // on error: leave report.value unchanged (D-14)
}
```

Button: always visible; `:disabled="!fixEnabled"`; primary stays on Run lint.

---

### `NotebookHealthPanel.spec.ts` (test, request-response)

**Analog:** same file — **replace** “no Fix control” assertions.

**Mock / mount / checkbox helpers** (lines 48–81):
```typescript
describe("NotebookHealthPanel", () => {
  const notebookId = 42
  let lintSpy: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    vi.restoreAllMocks()
    lintSpy = mockSdkService(NotebookHealthController, "lint", reportFixture)
  })

  function mountPanel(user?: User) {
    const builder = helper
      .component(NotebookHealthPanel)
      .withProps({ notebookId })
    if (user) {
      builder.withCurrentUser(user)
    }
    return builder.mount()
  }
```

**Assertions to replace** (lines 142–171 currently require Fix absent):
```typescript
    expect(wrapper.text()).not.toMatch(/\bFix\b/)
    expect(wrapper.find('[data-testid="notebook-health-fix"]').exists()).toBe(
      false
    )
```

**New behaviors to assert (data-testid, not roles):**
- Fix button exists with `data-testid="notebook-health-fix"` but `disabled` when checkbox off or no report / empty `empty_folders`
- Enabled when checkbox on **and** report has ≥1 `empty_folders` item; label `Remove 1 empty folders`
- Click fix → `NotebookHealthController.fix` with `{ path: { notebook }, body: { removeEmptyFolders: true } }` then `lint` again (order)
- On fix error: prior findings remain (`mockSdkService` / `wrapSdkResponse` for error path if needed)

Keep existing bodyless lint assertions (checkbox must not send body on lint).

---

### `notebook_health.feature` + `notebookPage.ts` + `notebook.ts` (E2E)

**Analog:** existing capability-named health E2E stack.

**Feature scenarios** (`notebook_health.feature` lines 24–32) — extend with purge scenario (`@wip` until green):
```gherkin
  Scenario: Run lint with Remove empty folders checked does not delete folders
    ...
    Then I should see sidebar folder "Keep Empty"
```

**New scenario sketch:**
```gherkin
  @wip
  Scenario: Gated fix removes fully empty folders and keeps readme-only
    Given I have a notebook "Health purge suite" with a note "Anchor"
    # seed fully empty + readme-only (same pattern as findings suite)
    When I open the notebook workspace Health tab
    And I check Remove empty folders on the notebook health panel
    And I run notebook health lint
    And the notebook health empty folders finding includes "Empty Shell"
    When I apply notebook health empty folder fix
    Then the notebook health empty folders finding does not include "Empty Shell"
    And the notebook health readme-only folders finding includes "Readme Only Shell"
    And I should not see sidebar folder "Empty Shell"
    # no promote-to-parent shells
```

**Page object fluent methods** (`notebookPage.ts` lines 43–72):
```typescript
    runLint() {
      cy.get('[data-testid="notebook-health-run"]').click()
      pageIsNotLoading()
      return this
    },

    checkRemoveEmptyFolders() {
      cy.get(
        '[data-testid="notebook-health-remove-empty-folders"] input[type="checkbox"]'
      ).check({ force: true })
      return this
    },
```

**Add:**
```typescript
    applyFix() {
      cy.get('[data-testid="notebook-health-fix"]').click()
      pageIsNotLoading()
      return this
    },

    expectFixDisabled() {
      cy.get('[data-testid="notebook-health-fix"]').should('be.disabled')
      return this
    },
```

**Step defs** (`notebook.ts` lines 124–156) — thin wrappers calling page object (same style for apply fix / finding absence).

**Sidebar absence:** extend `noteSidebar` or steps; positive visibility is `expectSidebarFolderVisible` / `I should see sidebar folder {string}` in `note.ts` — add negative counterpart if missing.

---

### Generated OpenAPI / TypeScript client (config, transform)

**Analog:** existing `NotebookHealthController.lint` in `sdk.gen.ts` (lines 803–807):

```typescript
export class NotebookHealthController {
    public static lint<ThrowOnError extends boolean = false>(options: Options<LintData, ThrowOnError>): RequestResult<LintResponses, unknown, ThrowOnError> {
        return (options.client ?? client).post<LintResponses, unknown, ThrowOnError>({ url: '/api/notebooks/{notebook}/health/lint', ...options });
    }
}
```

**Regen process** (`.cursor/skills/generate-api-client/SKILL.md`):
1. Land Java controller + DTO first (source of truth).
2. `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`
3. Never hand-edit `packages/generated/doughnut-backend-api/**` or `open_api_docs.yaml`.
4. Expect generated `fix` with `body: { removeEmptyFolders: true }` and void/empty response (same as other void POSTs).
5. Run frontend tests that use the new method.

Body+JSON header pattern for POSTs with body appears on sibling controllers (e.g. `NotebookGroupController.createGroup` lines 810–818).

## Shared Patterns

### Authentication (owner/write)
**Source:** `NotebookHealthController.lint` lines 36–37  
**Apply to:** `fix` endpoint only (same as lint)

```java
authorizationService.assertAuthorization(notebook);
```

Foreign/anon → `UnexpectedNoAccessRightException` (controller tests).

### Lint ≠ Fix
**Source:** health controller + RESEARCH D-05  
**Apply to:** all layers

| Path | Transaction | Body | Mutates |
|------|-------------|------|---------|
| `POST .../health/lint` | `readOnly=true` | none | no |
| `POST .../health/fix` | writable | `{ removeEmptyFolders: true }` | yes |

Defaults (`healthRemoveEmptyFoldersDefault`) only prefill checkbox — never sent on fix.

### Error handling / loading (frontend)
**Source:** `NotebookHealthPanel.vue` + `.cursor/rules/frontend-api.mdc`  
**Apply to:** Fix and post-fix lint

```typescript
const { error } = await apiCallWithLoading(() => NotebookHealthController.fix(...))
if (!error) {
  await runLint()
}
```

Do not clear `report` before success.

### Validation (server opt-in gate)
**Source:** RESEARCH + `Boolean.TRUE.equals` idiom (`NoteController` line 121)  
**Apply to:** `NotebookHealthService.fix` (and/or controller)

```java
if (!Boolean.TRUE.equals(request.getRemoveEmptyFolders())) {
  throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fix requires removeEmptyFolders=true");
}
```

### Fully-empty predicate (shared with lint)
**Source:** `EmptyFolderHealthRule` + `FolderSubtreeLiveNotes`  
**Apply to:** purge recompute (TOCTOU)

- Occupied = `noteRepository.findLiveNoteFolderIdsByNotebookId` (soft-deleted ignored)
- Empty = no live notes in subtree + `FolderSubtreeLiveNotes.isBlankReadme(readmeContent)`
- Readme-only (`non-blank readme`) never in deletable set

### CASCADE-safe deletable subset
**Source:** RESEARCH Pitfall 1 / Phase 2 Pitfall 6  
**Apply to:** `EmptyFolderBulkPurge` only

`D ⊆ S` where every real descendant of `f` is also in `S`. Prevents deleting blank parent over readme-only child via `ON DELETE CASCADE`.

### Hard-delete without dissolve
**Source:** `EntityPersister.remove` in `FolderRelocationService` (lines 332–333)  
**Apply to:** purge only

- Children-before-parents (or deepest-first) on `D`
- **Never** call `FolderRelocationService.dissolveFolder` / `NotebookController.dissolveFolder`

### Frontend selectors
**Source:** Phase 5/6 panel + specs expecting `notebook-health-fix`  
**Apply to:** panel, Vitest, Cypress

| Control | `data-testid` |
|---------|----------------|
| Run lint | `notebook-health-run` |
| Checkbox | `notebook-health-remove-empty-folders` |
| Fix | `notebook-health-fix` |
| Save defaults | `notebook-health-save-defaults` |

### OpenAPI regen
**Source:** `.cursor/skills/generate-api-client/SKILL.md`  
**Apply to:** after controller signature lands

```bash
CURSOR_DEV=true nix develop -c pnpm generateTypeScript
```

## No Analog Found

None — every planned file has an in-repo analog (Health stack + dissolve’s remove step as negative/partial pattern).

| File | Role | Data Flow | Notes |
|------|------|-----------|-------|
| — | — | — | Subtree-safe `D` filter is **new logic** inside existing `FolderSubtreeLiveNotes` / purge collaborator; no prior CASCADE-safe purge, but building blocks exist |

## Anti-Patterns (do not copy)

| Anti-pattern | Source | Why |
|--------------|--------|-----|
| Full `dissolveFolder` promote loop | `FolderRelocationService` lines 299–330 | Promotes children; empty shells appear at parent (D-10, Pitfall 4) |
| Mutate on `lint` or send defaults on fix | Phase 5/6 panel | Lint stays report-only; defaults only prefill UI |
| Client-supplied folder ID list | — | TOCTOU / spoofing (D-08) |
| Hand-edit `sdk.gen.ts` / `open_api_docs.yaml` | generate-api-client skill | Regen only |
| Generic button label “Fix” / “Apply” alone | Phase 5 negative specs | Prefer “Remove N empty folders” (D-03) |

## Metadata

**Analog search scope:** `backend/.../controllers`, `controllers/dto`, `services`, `services/health`, `frontend/src/components/notebook`, `frontend/tests/components/notebook`, `e2e_test/features/notebooks`, `e2e_test/start/pageObjects`, `e2e_test/step_definitions`, `packages/generated/doughnut-backend-api`, `.cursor/skills/generate-api-client`

**Files scanned:** ~25 primary analogs (health stack, dissolve, DTOs, E2E, SDK, skill)

**Pattern extraction date:** 2026-07-22
