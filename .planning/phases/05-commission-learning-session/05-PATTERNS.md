# Phase 05: Commission Learning Session - Pattern Map

**Mapped:** 2026-08-08
**Files analyzed:** 11
**Analogs found:** 10 / 11

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `frontend/src/components/recall/CommissionLearningSessionDialog.vue` | component | request-response | `frontend/src/components/commons/AiRequestExportDialog.vue` | exact |
| `frontend/src/components/recall/RecallProgressBar.vue` | component | event-driven | `frontend/src/components/recall/RecallProgressBar.vue` + `NoteRefinement.vue` | exact |
| `frontend/tests/components/recall/CommissionLearningSessionDialog.spec.ts` | test | request-response | `frontend/tests/components/recall/NoteRefinement.exportBreakdownRequest.spec.ts` | exact |
| `frontend/tests/components/recall/RecallProgressBar.spec.ts` | test | event-driven | `frontend/tests/components/recall/RecallProgressBar.spec.ts` | exact |
| `e2e_test/features/learning_session/commissioned_learning_session.feature` | config | batch | `.planning/phases/01-commissioned-tracker-model/commissioned_learning_session.feature` | exact |
| `e2e_test/step_definitions/learning_session.ts` | route | request-response | `e2e_test/step_definitions/recall.ts` | exact |
| `e2e_test/start/pageObjects/recallPage.ts` | utility | request-response | `e2e_test/start/pageObjects/recallPage.ts` (`expectPotentialLearningSession`) | exact |
| `backend/src/main/java/com/odde/doughnut/services/RecallService.java` | service | transform | `backend/src/main/java/com/odde/doughnut/services/RecallService.java` (`getDueMemoryTrackers`) | exact |
| `backend/src/test/java/com/odde/doughnut/controllers/RecallsControllerTests.java` | test | CRUD | `RecallsControllerTests.shouldListDueCommissionedTrackersSeparatelyFromOrdinaryRecall` | exact |
| `.cursor/rules/frontend-api.mdc` | config | — | `.cursor/rules/frontend-api.mdc` (Intentionally noncancelable table) | exact |
| `frontend/src/composables/useRecallData.ts` | composable | pub-sub | `frontend/src/composables/useRecallPageLoading.ts` (refresh nonce watch) | role-match |

## Pattern Assignments

### `frontend/src/components/recall/CommissionLearningSessionDialog.vue` (component, request-response)

**Analog:** `frontend/src/components/commons/AiRequestExportDialog.vue` (display) + `frontend/src/composables/useGoToNextAssimilation.ts` (API call)

**Imports pattern** (AiRequestExportDialog lines 40-43):

```typescript
import { computed, onMounted, ref } from "vue"
import Modal from "@/components/commons/Modal.vue"
import CopyButton from "@/components/commons/CopyButton.vue"
```

Add commission-specific imports from RESEARCH Pattern 2:

```typescript
import { LearningSessionController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import timezoneParam from "@/managedApi/window/timezoneParam"
```

**Modal + readonly textarea + CopyButton** (AiRequestExportDialog lines 1-37):

```vue
<Modal @close_request="$emit('close')">
  <template #body>
    <div class="daisy-card">
      <div class="daisy-card-body">
        <h3 class="daisy-card-title">{{ title }}</h3>
        <div class="mt-4">
          <textarea
            class="daisy-textarea w-full h-96 bg-base-100 font-mono text-xs"
            readonly
            :value="displayContent"
            data-testid="export-textarea"
          />
          <div class="flex gap-2 justify-end mt-2">
            <CopyButton
              :text="displayContent"
              :disabled="!displayContent"
              test-id="copy-export-btn"
              aria-label="Copy to clipboard"
            />
          </div>
        </div>
      </div>
    </div>
  </template>
</Modal>
```

Adapt for Phase 5: use `data-test="learning-session-request"` (per RESEARCH E2E guidance), `test-id="copy-learning-session-request"`, pre-commission CTA button (`daisy-btn daisy-btn-primary`), post-commission awaiting banner with `data-test="learning-session-awaiting-report"` when `status === 'AWAITING_REPORT'`.

**Commission API call with blockUi** (useGoToNextAssimilation lines 19-30):

```typescript
const { data, error } = await apiCallWithLoading(
  () =>
    AssimilationController.next({
      query: { timezone: timezoneParam() },
    }),
  { blockUi: true, message: "Loading next note..." }
)

if (error || !data) {
  return false
}
```

Commission variant (from RESEARCH Pattern 2):

```typescript
const { data, error } = await apiCallWithLoading(
  () =>
    LearningSessionController.commission({
      body: { notebookId },
      query: { timezone: timezoneParam() },
    }),
  { blockUi: true, message: "Commissioning learning session…" }
)
if (!error && data) {
  requestMarkdown.value = data.requestMarkdown
  status.value = data.status
}
```

**Response types** (`packages/generated/doughnut-backend-api/types.gen.ts` lines 619-627):

```typescript
export type CommissionLearningSessionRequest = {
    notebookId: number;
};

export type LearningSessionCommissionResponse = {
    learningSessionId: number;
    requestMarkdown: string;
    status: 'AWAITING_REPORT' | 'RECORDED';
};
```

**Refresh after success:** emit `commissioned` → parent calls `requestDueRecallsRefresh()` from `useRecallData` (lines 100-102, 127).

**Anti-pattern:** Do not fetch-on-mount like `AiRequestExportDialog` — commission is user-triggered; do not re-implement markdown in FE.

---

### `frontend/src/components/recall/RecallProgressBar.vue` (component, event-driven)

**Analog:** current `RecallProgressBar.vue` (potential-session strip) + `NoteRefinement.vue` (dialog v-if wiring) + `RecallSessionOptionsDialog.vue` (recall-context Modal)

**Current potential-session row** (RecallProgressBar lines 60-72):

```vue
<div
  v-for="session in potentialLearningSessions"
  :key="session.notebookId"
  data-test="potential-learning-session"
  role="status"
  class="text-base font-normal text-base-content break-words"
>
  1 potential learning session to commission for notebook "{{ session.notebookName }}"
</div>
```

**Promotion pattern:** Keep row + `data-test="potential-learning-session"` and glossary copy; add explicit `daisy-btn daisy-btn-primary` Commission button per row; on click set `selectedSession` ref and show dialog.

**Dialog v-if pattern** (NoteRefinement lines 108-120):

```vue
<AiRequestExportDialog
  v-if="showExportExtractDialog"
  title="Export Extract Request for ChatGPT"
  :fetch-export="fetchExtractRequestExport"
  @close="showExportExtractDialog = false"
/>
```

Adapt:

```vue
<CommissionLearningSessionDialog
  v-if="commissionDialogSession"
  :notebook-id="commissionDialogSession.notebookId"
  :notebook-name="commissionDialogSession.notebookName"
  @close="commissionDialogSession = undefined"
  @commissioned="onCommissioned"
/>
```

**Recall-context Modal reference** (RecallSessionOptionsDialog lines 1-3, 48):

```vue
<Modal :isPopup="true" @close_request="closeDialog">
```

Use `Modal` without `:isPopup` for commission dialog (full card like export dialogs) unless UI-SPEC dictates popup.

**useRecallData refresh** (import pattern from RecallSessionOptionsDialog line 49):

```typescript
import { useRecallData } from "@/composables/useRecallData"
const { requestDueRecallsRefresh } = useRecallData()

const onCommissioned = () => {
  requestDueRecallsRefresh()
}
```

**Anti-pattern:** Do not inject commission into `ProgressBar` fill or ordinary `#buttons` — violates Phase 3 D-05.

---

### `frontend/tests/components/recall/CommissionLearningSessionDialog.spec.ts` (test, request-response)

**Analog:** `frontend/tests/components/recall/NoteRefinement.exportBreakdownRequest.spec.ts`

**mockSdkService + click + textarea assertion** (exportBreakdownRequest spec lines 39-74):

```typescript
const exportBreakdownRequestSpy = mockSdkService(
  AiController,
  "exportRefinementLayoutRequest",
  sampleExportData
)
// ...
await wrapper
  .find(`button[title="${exportBreakdownRequestButtonTitle}"]`)
  .trigger("click")
await flushPromises()

expect(exportBreakdownRequestSpy).toHaveBeenCalledWith({
  path: { note: note.id },
})

const textarea = document.body.querySelector(
  '[data-testid="export-textarea"]'
) as HTMLTextAreaElement
expect(textarea).toBeTruthy()
expect(textarea.value).toContain('"model"')
```

Commission variant:

```typescript
import { LearningSessionController } from "@generated/doughnut-backend-api/sdk.gen"
import { mockSdkService } from "@tests/helpers"

mockSdkService(LearningSessionController, "commission", {
  learningSessionId: 42,
  requestMarkdown: "# Learning Session Request\n\n### Hola\n",
  status: "AWAITING_REPORT",
})
// click Commission CTA → assert [data-test="learning-session-request"] value
// assert [data-test="learning-session-awaiting-report"] visible
```

**Mount pattern:** `helper.component(CommissionLearningSessionDialog).withProps({ notebookId, notebookName }).mount()` — mirror RecallProgressBar.spec.ts `mountBar` helper style.

---

### `frontend/tests/components/recall/RecallProgressBar.spec.ts` (test, event-driven)

**Analog:** existing `RecallProgressBar.spec.ts`

**mountBar helper** (lines 6-23):

```typescript
const mountBar = (
  potentialLearningSessions: {
    notebookId: number
    notebookName: string
    trackerIds: number[]
  }[]
) =>
  helper
    .component(RecallProgressBar)
    .withProps({
      finished: 0,
      toRepeatCount: 0,
      canMoveToEnd: false,
      currentIndex: 0,
      previousAnsweredQuestions: [],
      potentialLearningSessions,
    })
    .mount()
```

**Extend with:** Commission button visible on row; click opens dialog (query `document.body` for dialog controls); preserve existing glossary/role assertions — update `role="status"` expectation if row becomes interactive (may move status to text span, button as control).

---

### `e2e_test/features/learning_session/commissioned_learning_session.feature` (config, batch)

**Analog:** `.planning/phases/01-commissioned-tracker-model/commissioned_learning_session.feature` lines 28-36

```gherkin
Scenario: Commissioning a learning session produces a request for the tutor
  Given the notes "Hola, Gracias" are assimilated as commissioned on day 1
  And It's day 2, 9 hour
  When I commission a learning session for notebook "Spanish conversation"
  Then the learning session request should list session items for notes "Hola, Gracias"
  And the learning session request should include the learning status of "Hola"
  And the learning session request should include the expected learning content "Hello"
  And the learning session request should instruct the tutor to report one score per session item
  And the learning session should be awaiting the tutor's report
```

Add to `e2e_test/features/learning_session/commissioned_learning_session.feature` with `@wip` until green; do **not** graduate Phase 6/7 scenarios (record/amend).

---

### `e2e_test/step_definitions/learning_session.ts` (route, request-response)

**Analog:** `e2e_test/step_definitions/recall.ts`

**Step definition structure** (recall.ts lines 1-7, 60-65):

```typescript
import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Then(
  'I should see {int} potential learning session to commission for notebook {string}',
  (count: number, notebookTitle: string) => {
    start.recall().expectPotentialLearningSession(count, notebookTitle)
  }
)
```

**New steps delegate to page object:**

```typescript
When(
  'I commission a learning session for notebook {string}',
  (notebookTitle: string) => {
    start.recall().assumeRecallPage().commissionLearningSession(notebookTitle)
  }
)

Then(
  'the learning session request should list session items for notes {string}',
  (noteTitles: string) => {
    start.recall().assumeRecallPage().expectLearningSessionRequestListsNotes(noteTitles)
  }
)
```

Assert backend markdown substrings (`### Hola`, `Expected learning content: Hello`, `score from 0 to 5 per item`) — do not paraphrase rubric in FE.

---

### `e2e_test/start/pageObjects/recallPage.ts` (utility, request-response)

**Analog:** existing `expectPotentialLearningSession` (lines 154-160)

```typescript
expectPotentialLearningSession(count: number, notebookTitle: string) {
  this.navigateToRecallPage()
  const expected = `${count} potential learning session to commission for notebook "${notebookTitle}"`
  cy.contains('[data-test="potential-learning-session"]', expected).should(
    'be.visible'
  )
  return this
},
```

**Add `commissionLearningSession(notebookTitle)`:**

```typescript
commissionLearningSession(notebookTitle: string) {
  cy.contains('[data-test="potential-learning-session"]', notebookTitle)
    .find('[data-test="commission-learning-session"]') // or button text
    .click()
  waitUntilAppIsNotBusy()
  return this
},
```

**waitUntilAppIsNotBusy** (`e2e_test/start/pageBase.ts` lines 4-7):

```typescript
export const waitUntilAppIsNotBusy = () => {
  cy.get('[data-app-busy]', { timeout: 30000 }).should('not.exist')
}
```

Call after commission CTA — required for `blockUi: true` mutations (`frontend-api.mdc`).

**Request assertions:** read `[data-test="learning-session-request"]` textarea `.invoke('val')` or `.should('contain', '### Hola')`.

---

### `backend/src/main/java/com/odde/doughnut/services/RecallService.java` (service, transform) — recommended

**Analog:** existing `getDueMemoryTrackers` dueCommissioned mapping (lines 72-87)

```java
List<DueCommissionedMemoryTrackerLite> dueCommissioned =
    getCommissionedMemoryTrackersNeedToRepeat(user, currentUTCTimestamp, timeZone, dueInDays)
        .map(
            mt -> {
              DueCommissionedMemoryTrackerLite lite = new DueCommissionedMemoryTrackerLite();
              lite.setMemoryTrackerId(mt.getId());
              Notebook notebook = mt.getNote().getNotebook();
              lite.setNotebookId(notebook.getId());
              lite.setNotebookName(notebook.getName());
              return lite;
            })
        .toList();
```

**Repository @Query pattern** (SessionItemRepository lines 15-25):

```java
@Query(
    """
    SELECT new com.odde.doughnut.entities.repositories.RecordedFeedbackSummary(
      COUNT(si), MAX(si.feedbackRecordedAt))
    FROM SessionItem si
    WHERE si.memoryTracker.id = :memoryTrackerId
    ...
    """)
RecordedFeedbackSummary summarizeRecordedFeedbackByMemoryTrackerId(
    @Param("memoryTrackerId") Integer memoryTrackerId);
```

Add analogous query: `findMemoryTrackerIdsInAwaitingReportSessions(userId)` → filter stream before `.map(...)`. Inject `SessionItemRepository` into `RecallService` (constructor pattern lines 27-35).

**No exact analog** for awaiting-report exclusion — partial match only; planner should add JPQL method on `SessionItemRepository` rather than in-memory filter for scale.

---

### `backend/src/test/java/com/odde/doughnut/controllers/RecallsControllerTests.java` (test, CRUD) — if exclusion added

**Analog:** `shouldListDueCommissionedTrackersSeparatelyFromOrdinaryRecall` (lines 150-178)

```java
@Test
void shouldListDueCommissionedTrackersSeparatelyFromOrdinaryRecall() {
  Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
  testabilitySettings.timeTravelTo(currentTime);
  Note note =
      makeMe
          .aNote()
          .notebook(
              makeMe
                  .aNotebook()
                  .creatorAndOwner(currentUser.getUser())
                  .name("Spanish conversation")
                  .please())
          .please();
  MemoryTracker commissioned =
      makeMe.aMemoryTrackerFor(note).commissioned().nextRecallAt(currentTime).please();

  DueMemoryTrackers dueMemoryTrackers = controller.recalling("Asia/Shanghai", 0);

  assertThat(dueMemoryTrackers.getToRepeat(), hasSize(0));
  assertThat(dueMemoryTrackers.getDueCommissioned(), hasSize(1));
  assertEquals(
      commissioned.getId(), dueMemoryTrackers.getDueCommissioned().get(0).getMemoryTrackerId());
}
```

**New test sketch:** commission via `LearningSessionController` (or `makeMe.aLearningSession()`), then assert `getDueCommissioned()` excludes trackers in `AWAITING_REPORT` session. Commission assertions reference `LearningSessionControllerTests` lines 42-75.

---

### `.cursor/rules/frontend-api.mdc` (config)

**Analog:** Intentionally noncancelable table (lines 114-124)

```markdown
| Assimilate unit | `Assimilating...` | `useAssimilateUnit.ts` | `{ blockUi: true }` | Mutation |
| Load next assimilation note | `Loading next note...` | `useGoToNextAssimilation.ts` | `{ blockUi: true }` | View transition |
```

Add row:

```markdown
| Commission learning session | `Commissioning learning session…` | `CommissionLearningSessionDialog.vue` | `{ blockUi: true }` | Mutation |
```

---

### `frontend/src/composables/useRecallData.ts` (composable, pub-sub) — reference only

**Analog:** `useRecallPageLoading.ts` watch on refresh nonce (lines 101-103)

```typescript
watch(dueRecallsRefreshNonce, async () => {
  await loadCurrentDueRecalls()
})
```

`requestDueRecallsRefresh` (useRecallData lines 100-102):

```typescript
const requestDueRecallsRefresh = () => {
  dueRecallsRefreshNonce.value += 1
}
```

No file change required unless planner adds commission-specific state — dialog should call existing `requestDueRecallsRefresh()` after successful commission.

## Shared Patterns

### Authentication (backend — already Phase 4)

**Source:** `backend/src/main/java/com/odde/doughnut/controllers/LearningSessionController.java`
**Apply to:** Commission API (no new endpoint)

```java
authorizationService.assertLoggedIn();
// ...
authorizationService.assertAuthorization(notebook);
```

### API client + timezone

**Source:** `frontend/src/composables/useGoToNextAssimilation.ts`
**Apply to:** `CommissionLearningSessionDialog.vue`

```typescript
import { LearningSessionController } from "@generated/doughnut-backend-api/sdk.gen"
import timezoneParam from "@/managedApi/window/timezoneParam"
```

Always pass `query: { timezone: timezoneParam() }` on commission.

### Whole-UI blocking + E2E pairing

**Source:** `.cursor/rules/frontend-api.mdc` + `e2e_test/start/pageBase.ts`
**Apply to:** Commission mutation + Cypress steps

- Use `{ blockUi: true, message: "Commissioning learning session…" }` — noncancelable mutation.
- E2E must call `waitUntilAppIsNotBusy()` after commission click before textarea assertions.

### Copyable protocol document

**Source:** `frontend/src/components/commons/AiRequestExportDialog.vue` + `CopyButton.vue`
**Apply to:** Request display

```vue
<CopyButton :text="requestMarkdown" test-id="copy-learning-session-request" />
```

### Vitest SDK mocking

**Source:** `frontend/tests/components/recall/NoteRefinement.exportBreakdownRequest.spec.ts`
**Apply to:** All commission component tests

```typescript
import { mockSdkService } from "@tests/helpers"
mockSdkService(LearningSessionController, "commission", { ... })
```

### Backend markdown contract (display only)

**Source:** `backend/src/test/java/com/odde/doughnut/controllers/LearningSessionControllerTests.java` lines 48-70
**Apply to:** Vitest/E2E substring assertions

```java
assertThat(markdown, containsString("# Learning Session Request"));
assertThat(markdown, containsString("### Hola"));
assertThat(markdown, containsString("Expected learning content: Hello"));
assertThat(markdown, containsString("score from 0 to 5 per item"));
```

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `SessionItemRepository.findMemoryTrackerIdsInAwaitingReportSessions` (new method) | repository | transform | No existing query for awaiting-report tracker exclusion; follow `summarizeRecordedFeedbackByMemoryTrackerId` JPQL style |

## Metadata

**Analog search scope:** `frontend/src/components/recall/`, `frontend/src/components/commons/`, `frontend/tests/components/recall/`, `e2e_test/step_definitions/`, `e2e_test/start/pageObjects/`, `backend/src/main/java/com/odde/doughnut/services/`, `backend/src/test/java/com/odde/doughnut/controllers/`
**Files scanned:** ~45
**Pattern extraction date:** 2026-08-08
