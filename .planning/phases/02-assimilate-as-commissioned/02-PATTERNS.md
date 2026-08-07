# Phase 02: assimilate-as-commissioned - Pattern Map

**Mapped:** 2026-08-08
**Files analyzed:** 14
**Analogs found:** 14 / 14

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `backend/.../dto/AssimilationRequestDTO.java` | model (DTO) | request-response | itself (`skipMemoryTracking` Boolean) | exact |
| `backend/.../services/MemoryTrackerAssimilation.java` | service | CRUD | itself (property / note-level early returns) | exact |
| `backend/.../controllers/AssimilationControllerTests.java` | test | request-response | itself (`CreateAssimilationPoint` nested) | exact |
| `frontend/.../recall/AssimilationButtons.vue` | component | request-response | itself + `NotebookCatalogGroupActions.vue` (dropdown) + `CircleShowPage.vue` (`daisy-join`) | role-match |
| `frontend/.../recall/AssimilationSettings.vue` | component | request-response | itself (note-level vs property `AssimilationButtons`) | exact |
| `frontend/.../recall/AssimilationPanel.vue` | component | request-response | itself (`hasNoteLevelMemoryTrackers` / `processAssimilate`) | exact |
| `frontend/.../composables/useAssimilateUnit.ts` | hook | request-response | itself (`assimilateUnit` body + navigate) | exact |
| `frontend/.../notes/NoteInfoMemoryTracker.vue` | component | transform | itself (`trackerTypeLabel`) | exact |
| `packages/.../MemoryTrackerBuilder.ts` | utility | transform | itself + Java `MemoryTrackerBuilder.commissioned()` | exact |
| `frontend/tests/.../AssimilationPanel.spec.ts` | test | request-response | itself (disable + assimilate spy patterns) | exact |
| `frontend/tests/.../NoteInfoMemoryTracker.spec.ts` | test | transform | itself (type label assertions) | exact |
| `e2e_test/features/learning_session/*.feature` | test (E2E) | request-response | draft scenario + `assimilation/assimilate_with_remembering_spelling.feature` | role-match |
| `e2e_test/.../assimilationPage/*` (flow/shared) | utility (page object) | request-response | `assimilationFlow.ts` + `shared.ts` + `propertyMemoryTrackerExpectations.ts` | exact |
| `e2e_test/step_definitions/*` | test | request-response | `assimilation_settings.ts` + `assimilation_memory_tracker.ts` | exact |

**Not hand-edited:** `packages/generated/doughnut-backend-api/**` — regenerate via `pnpm generateTypeScript` after DTO change.  
**Unchanged pass-through:** `AssimilationController.assimilate` already forwards the DTO; no controller logic change expected.

## Pattern Assignments

### `backend/.../dto/AssimilationRequestDTO.java` (model, request-response)

**Analog:** `backend/src/main/java/com/odde/doughnut/controllers/dto/AssimilationRequestDTO.java`

**Core pattern** (lines 3–6) — add optional `Boolean` beside existing wrappers:

```java
public class AssimilationRequestDTO {
  public Integer noteId;
  public Boolean skipMemoryTracking;
  public String propertyKey;
  // ADD: public Boolean assimilateAsCommissioned;
}
```

**Validation posture:** Treat missing/`null`/`false` as ordinary assimilate; only `Boolean.TRUE.equals(...)` enables commissioned path (same as `skipMemoryTracking` usage in `MemoryTrackerAssimilation`).

---

### `backend/.../services/MemoryTrackerAssimilation.java` (service, CRUD)

**Analog:** same file — property early-branch + empty-list idempotency + `createNoteLevelTracker(..., type)`

**Imports pattern** (lines 1–11):

```java
import com.odde.doughnut.controllers.dto.AssimilationRequestDTO;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.MemoryTrackerType;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
// ...
```

**Core early-branch / idempotent empty** (lines 35–48, property path — copy structure for commissioned):

```java
if (request.propertyKey != null && !request.propertyKey.isEmpty()) {
  boolean propertyTrackerExists =
      existingTrackers.stream().anyMatch(mt -> request.propertyKey.equals(mt.getPropertyKey()));
  if (propertyTrackerExists) {
    return List.of();
  }
  return List.of(
      initializeNewTracker(
          MemoryTracker.buildMemoryTrackerForProperty(note, request.propertyKey),
          currentUser,
          currentTime,
          skipMemoryTracking,
          MemoryTrackerType.UNDERSTANDING));
}
```

**COMMISSIONED exclusion already used for ordinary existence** (lines 50–54) — mirror for disable/create checks:

```java
List<MemoryTracker> existingNoteLevelTrackers =
    existingTrackers.stream()
        .filter(MemoryTracker::isNoteLevelTracker)
        .filter(mt -> mt.getType() != MemoryTrackerType.COMMISSIONED)
        .toList();
```

**Create helper for typed note-level tracker** (lines 83–95):

```java
private MemoryTracker createNoteLevelTracker(
    Note note,
    User currentUser,
    Timestamp currentTime,
    boolean skipMemoryTracking,
    MemoryTrackerType type) {
  return initializeNewTracker(
      MemoryTracker.buildMemoryTrackerForNote(note),
      currentUser,
      currentTime,
      skipMemoryTracking,
      type);
}
```

**Recommended commissioned branch placement:** immediately after loading `existingTrackers` / computing `skipMemoryTracking`, **before** property and ordinary note-level logic; if `assimilateAsCommissioned` and non-empty `propertyKey` → `List.of()`; if note-level COMMISSIONED already exists → `List.of()`; else `createNoteLevelTracker(..., MemoryTrackerType.COMMISSIONED)`.

**Error handling:** Do not catch; duplicate/illegal combo returns empty list (existing assimilate posture). UK enforces uniqueness if pre-check missed.

---

### `backend/.../controllers/AssimilationControllerTests.java` (test, request-response)

**Analog:** same file — `CreateAssimilationPoint` nested class

**Controller boundary + makeMe** (lines 20–44, 127–173):

```java
class AssimilationControllerTests extends ControllerTestBase {
  @Autowired AssimilationController controller;

  AssimilationRequestDTO assimilateRequest(Note note) {
    AssimilationRequestDTO request = new AssimilationRequestDTO();
    request.noteId = note.getId();
    return request;
  }

  @Nested
  class CreateAssimilationPoint {
    @Test
    void assimilatingCommissionedOnlyNoteCreatesUnderstandingAndLeavesCommissioned() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      makeMe.aMemoryTrackerFor(note).commissioned().please();
      List<MemoryTracker> result = controller.assimilate(assimilateRequest(note));
      assertThat(result.get(0).getType(), equalTo(MemoryTrackerType.UNDERSTANDING));
      // ... coexistence assert
    }
  }
}
```

**Patterns to copy for new tests:**
- Helper `assimilateCommissionedRequest(Note)` setting `assimilateAsCommissioned = true`
- One behavior per test; assert type `COMMISSIONED` and coexistence delta only
- Empty-list assertions like `shouldReturnEmptyWhenPropertyTrackerAlreadyExists` (lines 232–241)
- Prefer controller tests over direct `MemoryTrackerAssimilation` tests

**Auth pattern already covered:** `notLoggedIn` throws `ResponseStatusException` (lines 129–134).

---

### `frontend/.../recall/AssimilationButtons.vue` (component, request-response)

**Analog (primary control):** same file — primary Assimilate / Skip / Revive emits  
**Analog (dropdown):** `NotebookCatalogGroupActions.vue`  
**Analog (join layout):** `CircleShowPage.vue` / `UserListing.vue` `daisy-join`

**Existing assimilate control** (lines 1–10, 59):

```vue
<input
  type="submit"
  value="Assimilate"
  :class="['daisy-btn daisy-btn-primary', sizeClass]"
  data-test="assimilate"
  :disabled="disabled || assimilateDisabled"
  @click="$emit('assimilate', false)"
/>
```

**Dropdown primitives** (`NotebookCatalogGroupActions.vue` lines 1–26, 50–60):

```vue
<AutoCollapseDropdown
  v-slot="{ closeDropdown }"
  class="daisy-dropdown daisy-dropdown-end daisy-dropdown-bottom shrink-0"
>
  <summary ... class="daisy-btn ... list-none cursor-pointer">...</summary>
  <DropdownMenu>
    <DropdownMenuItem>
      <button type="button" :class="dropdownMenuButtonClass" @click="...">
        ...
      </button>
    </DropdownMenuItem>
  </DropdownMenu>
</AutoCollapseDropdown>
```

```typescript
import AutoCollapseDropdown from "@/components/commons/AutoCollapseDropdown.vue"
import DropdownMenu from "@/components/commons/DropdownMenu.vue"
import DropdownMenuItem from "@/components/commons/DropdownMenuItem.vue"
import { dropdownMenuButtonClass } from "@/components/commons/dropdownMenuClasses"
```

**Join grouping** (`CircleShowPage.vue` lines 17–41):

```vue
<div class="daisy-join">
  <button type="button" class="daisy-btn ... daisy-join-item" ...>...</button>
  <button type="button" class="daisy-btn ... daisy-join-item" ...>...</button>
</div>
```

**Discretionary wiring (from RESEARCH):** When `showCommissionedOption`, wrap primary Assimilate + caret in `daisy-join`; caret `data-test="assimilate-as-commissioned-caret"`; menu item `data-test="assimilate-as-commissioned"` with copy **Assimilate as commissioned**; emit a distinct event (e.g. `assimilateAsCommissioned`) that never goes through Skip/`assimilate(true)`. Prefer Lucide `ChevronDown` like `RichFrontmatterPropertyValueDialog.vue` for the caret icon.

---

### `frontend/.../recall/AssimilationSettings.vue` (component, request-response)

**Analog:** same file — note-level vs property button instances

**Note-level buttons** (lines 108–116) — pass commissioned option only here:

```vue
<AssimilationButtons
  :disabled="!noteInfoLoaded"
  :assimilate-disabled="assimilateDisabled"
  :skipped-for-recall="isSkippedForRecall(noteRecallInfo)"
  @assimilate="(skip) => emit('assimilate', { skipMemoryTracking: skip })"
  @revive="emit('revive', {})"
/>
```

**Property rows keep caret-free buttons** (lines 67–84) — do **not** pass `showCommissionedOption` (D-04).

**Reload after create** (lines 186–192) — stay on note via `reloadNoteInfo` (already exposed):

```typescript
const reloadNoteInfo = async () => {
  await noteInfoBarRef.value?.reload()
  noteRecallInfo.value =
    noteInfoBarRef.value?.noteRecallInfo ?? noteRecallInfo.value
}
defineExpose({ reloadNoteInfo })
```

**Commissioned menu visibility (D-05):** derive from `noteRecallInfo.memoryTrackers` — hide caret when any note-level `type === 'COMMISSIONED'`.

---

### `frontend/.../recall/AssimilationPanel.vue` (component, request-response)

**Analog:** same file — disable logic + assimilate pipeline

**Ordinary note-level existence (must ignore COMMISSIONED — D-03)** (lines 80–93):

```typescript
const hasNoteLevelMemoryTrackers = computed(
  () =>
    noteRecallInfo.value?.memoryTrackers?.some((mt) => !mt.propertyKey) ?? false
)
const assimilateDisabled = computed(
  () =>
    hasNoteLevelMemoryTrackers.value &&
    !(rememberSpelling.value && !hasSpellingMemoryTracker.value)
)
```

**Change to:** exclude `mt.type === 'COMMISSIONED'` in the `some(...)` for ordinary existence (keep spelling-only exception).

**Spelling popup gate** (lines 108–111) — commissioned path must never enter:

```typescript
if (!propertyKey && !skipMemoryTracking && rememberSpelling.value) {
  showSpellingPopup.value = true
  return
}
```

**Post-success reload when navigation skipped** (lines 128–152) — commissioned path should land here (`!result.navigated` → `reloadNoteInfo` already called):

```typescript
const result = await assimilateUnit({ noteId: note.id, skipMemoryTracking, propertyKey })
await settingsRef.value?.reloadNoteInfo()
if (!result.navigated) {
  emit("reloadNeeded")
}
```

Extend `AssimilateEvent` / `doAssimilate` with `assimilateAsCommissioned?: boolean` and a dedicated handler that skips spelling confirmation.

---

### `frontend/.../composables/useAssimilateUnit.ts` (hook, request-response)

**Analog:** same file

**Request body construction** (lines 8–12, 36–51):

```typescript
export type AssimilateUnitRequest = {
  noteId: number
  skipMemoryTracking: boolean
  propertyKey?: string
  // ADD: assimilateAsCommissioned?: boolean
}

AssimilationController.assimilate({
  body: {
    noteId: request.noteId,
    ...(request.propertyKey ? { propertyKey: request.propertyKey } : {}),
    ...(request.skipMemoryTracking ? { skipMemoryTracking: true } : {}),
    // ADD: ...(request.assimilateAsCommissioned ? { assimilateAsCommissioned: true } : {}),
  },
})
```

**Count + navigate (skip both on commissioned — D-06 / Pitfall 4)** (lines 57–67):

```typescript
const newTrackerCount = memoryTrackers.filter(
  (tracker) => !tracker.removedFromTracking
).length
if (totalAssimilatedCount.value !== undefined) {
  totalAssimilatedCount.value += newTrackerCount
}
incrementAssimilatedCount(newTrackerCount)
requestDueRecallsRefresh()

const navigated = await goToNextAssimilation()
```

**Commissioned branch:** still call API + `requestDueRecallsRefresh` if desired; **do not** increment ordinary assimilate counters; **do not** call `goToNextAssimilation`; return `{ success: true, navigated: false, memoryTrackers }`.

**Loading / error:** keep `apiCallWithLoading` + propagate failure as `{ success: false }` (no swallow).

---

### `frontend/.../notes/NoteInfoMemoryTracker.vue` (component, transform)

**Analog:** same file — `trackerTypeLabel`

**Current label** (lines 38–44):

```typescript
const trackerTypeLabel = computed(() => {
  const { propertyKey, spelling } = localMemoryTracker.value
  if (propertyKey) {
    return `property: ${propertyKey}`
  }
  return spelling ? "spelling" : "normal"
})
```

**Extend (D-07):** check `type === 'COMMISSIONED'` → `"Commissioned"` **before** property/spelling/normal fallthrough so E2E can assert Type cell text.

---

### `packages/.../MemoryTrackerBuilder.ts` (utility, transform)

**Analog (TS):** same file `spelling()`  
**Analog (Java):** `backend/.../testability/builders/MemoryTrackerBuilder.java` `commissioned()`

**TS spelling helper** (lines 66–69) — add sibling:

```typescript
spelling(value = true): MemoryTrackerBuilder {
  this.data.spelling = value
  return this
}
```

**Java commissioned** (lines 58–60):

```java
public MemoryTrackerBuilder commissioned() {
  entity.setType(MemoryTrackerType.COMMISSIONED);
  return this;
}
```

**TS commissioned:** set `this.data.type = "COMMISSIONED"` and `this.data.spelling = false` (OpenAPI already has `type?: 'UNDERSTANDING' | 'SPELLING' | 'COMMISSIONED'`).

---

### `frontend/tests/.../AssimilationPanel.spec.ts` (test, request-response)

**Analog:** same file + `assimilationPanelTestSupport`

**Stable boundary mount + SDK spy** (lines 36–57, 104–154):

```typescript
it("advances via next assimilation and increments counts when assimilating", async () => {
  assimilateSpy.mockResolvedValue(wrapSdkResponse([...]))
  const wrapper = await mountAssimilationPanelReady()
  await clickAssimilate(wrapper)
  expect(assimilateSpy).toHaveBeenCalledWith({ body: { noteId: note.id } })
  expect(mockedGoToNextAssimilation).toHaveBeenCalled()
})

it("disables assimilate when note has memory trackers and no add-spelling-only mode", async () => {
  mockSdkService(NoteController, "getNoteInfo", {
    memoryTrackers: [makeMe.aMemoryTracker.id(1).spelling(false).please()],
  })
  // ...
})
```

**Add scenarios (delta-only asserts):**
- Commissioned-only fixture → Assimilate **enabled** (D-03)
- Click commissioned menu → body includes `assimilateAsCommissioned: true`; `goToNextAssimilation` **not** called; counts unchanged
- Property rows: no caret / no commissioned control

---

### `frontend/tests/.../NoteInfoMemoryTracker.spec.ts` (test, transform)

**Analog:** same file

**Type label assertions** (lines 15–65):

```typescript
expect(wrapper.text()).toContain("normal")
// property:
expect(wrapper.text()).toContain("property: topic")
// spelling:
expect(wrapper.text()).toContain("spelling")
```

**Add:** mount with `makeMe.aMemoryTracker.commissioned().please()` → `toContain("Commissioned")`.

---

### `e2e_test/features/learning_session/*.feature` (test E2E, request-response)

**Analog (scenario text):** `.planning/phases/01-commissioned-tracker-model/commissioned_learning_session.feature` lines 16–20  
**Analog (feature file shape):** `e2e_test/features/assimilation/assimilate_with_remembering_spelling.feature`

**Graduate only Phase 2 scenario** (D-08), tag `@wip` until green:

```gherkin
@disableOpenAiService @mockBrowserTime @wip
Feature: Commissioned learning session
  ...
  Scenario: Assimilating a note with a tutor creates a commissioned memory tracker
    When I am assimilating the note "Hola"
    And I assimilate it as commissioned
    And I open assimilation settings
    Then I should see a commissioned memory tracker for "Hola"
```

Create directory `e2e_test/features/learning_session/` (absent today). Do not graduate later scenarios from the draft.

---

### `e2e_test/.../assimilationPage/*` (page object, request-response)

**Analog:** `shared.ts` note-level filter + `assimilationFlow.ts` click helpers + `propertyMemoryTrackerExpectations.ts` row asserts

**Note-level control filter** (`shared.ts` lines 12–18):

```typescript
export const isNoteLevelAssimilationControl = (el: Element) =>
  el.closest('[data-test="assimilation-property-row"]') === null

export const assimilateButton = (options?: { timeout?: number }) =>
  cy.get(assimilateButtonSelector, options ?? {})
    .filter((_, el) => isNoteLevelAssimilationControl(el))
```

**Click + busy wait** (`assimilationFlow.ts` lines 27–48):

```typescript
clickAssimilate() {
  assimilateButton().click()
  return this
},
assimilateOnPanel() {
  this.clickAssimilate()
  waitUntilAppIsNotBusy()
  return this
},
```

**Tracker Type assert** (`propertyMemoryTrackerExpectations.ts` lines 28–38):

```typescript
expectMemoryTrackerInfo(expected: { [key: string]: string }[]) {
  for (const k in expected) {
    cy.contains('tr', expected[k]?.type ?? '').within(() => {
      for (const attr in expected[k]) {
        if (expected[k][attr] !== undefined) {
          cy.contains('td', expected[k][attr])
        }
      }
    })
  }
}
```

**Add helpers:** open note-level caret → click `[data-test="assimilate-as-commissioned"]` → `waitUntilAppIsNotBusy`; expect Type `"Commissioned"` via `expectMemoryTrackerInfo([{ type: 'Commissioned', ... }])`.

---

### `e2e_test/step_definitions/*` (test, request-response)

**Analog:** `assimilation_settings.ts` (open settings / assimilate) + `assimilation_memory_tracker.ts` (see tracker)

**Existing assimilating step** (`assimilation_settings.ts` lines 15–21):

```typescript
When('I am assimilating the note {string}', (noteTitle: string) => {
  start.jumpToNotePage(noteTitle).moreOptions().openAssimilationSettings()
})

When('I open assimilation settings', () => {
  start.assumeNotePage().moreOptions().openAssimilationSettings()
})
```

**Property tracker Then** (`assimilation_memory_tracker.ts` lines 29–33):

```typescript
Then('I should see a property memory tracker for {string}', (propertyKey: string) => {
  start.assumeAssimilationPage().expectPropertyMemoryTracker(propertyKey)
})
```

**Add:**
- `When('I assimilate it as commissioned', ...)` → page-object caret+menu
- `Then('I should see a commissioned memory tracker for {string}', ...)` → Type `Commissioned` (title may only need settings open; draft uses note title for orientation)

## Shared Patterns

### Authentication (backend assimilate)

**Source:** `AssimilationController.java` lines 59–66  
**Apply to:** No new endpoints — commissioned uses same POST

```java
@PostMapping(path = "")
@Transactional
public List<MemoryTracker> assimilate(@RequestBody AssimilationRequestDTO request) {
  authorizationService.assertLoggedIn();
  return memoryTrackerService.assimilate(
      request,
      authorizationService.getCurrentUser(),
      testabilitySettings.getCurrentUTCTimestamp());
}
```

### Idempotent empty list (create duplicates / illegal combo)

**Source:** `MemoryTrackerAssimilation.java` property + ordinary existing-tracker returns  
**Apply to:** Second COMMISSIONED create; `propertyKey` + `assimilateAsCommissioned`

```java
if (propertyTrackerExists) {
  return List.of();
}
```

### Frontend API loading

**Source:** `useAssimilateUnit.ts` `apiCallWithLoading`  
**Apply to:** Commissioned assimilate call (same spinner / busy attribute for E2E)

```typescript
const { data: memoryTrackers, error } = await apiCallWithLoading(
  () => AssimilationController.assimilate({ body: { ... } }),
  { blockUi: true, message: "Assimilating..." }
)
```

### E2E note-level vs property control scoping

**Source:** `shared.ts` `isNoteLevelAssimilationControl`  
**Apply to:** Caret and commissioned menu selectors must filter property rows the same way as Assimilate

### OpenAPI client regeneration

**Source:** project `generate-api-client` skill / RESEARCH Don’t Hand-Roll  
**Apply to:** After DTO field added — never edit `packages/generated/doughnut-backend-api/**` by hand

### makeMe fixtures

**Source:** Java `MemoryTrackerBuilder.commissioned()`; extend TS builder  
**Apply to:** Backend coexistence tests + Vitest disable/label tests

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| *(none)* | — | — | Split button is a **composition** of existing `daisy-join` + `AutoCollapseDropdown` (no single prior Assimilate+caret file); treat as role-match composition above |

## Metadata

**Analog search scope:** `backend/src/main/java/com/odde/doughnut/{controllers,services,dto}`, `backend/src/test/.../AssimilationControllerTests`, `frontend/src/components/{recall,notes,notebook,commons}`, `frontend/src/composables/useAssimilateUnit.ts`, `frontend/tests/components/{recall,notes}`, `packages/doughnut-test-fixtures`, `e2e_test/{features,start/pageObjects/assimilationPage,step_definitions}`, `.planning/phases/01-commissioned-tracker-model/`
**Files scanned:** ~25 primary + grep hits for `daisy-join` / assimilate steps
**Pattern extraction date:** 2026-08-08
