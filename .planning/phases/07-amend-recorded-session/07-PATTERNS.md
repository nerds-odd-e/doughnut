# Phase 7: Amend Recorded Session - Pattern Map

**Mapped:** 2026-08-08
**Files analyzed:** 18
**Analogs found:** 17 / 18

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `backend/src/main/resources/db/migration/V300000241__session_item_pre_session_snapshot.sql` | migration | transform | `V300000238__add_memory_tracker_type.sql` | exact |
| `backend/src/main/java/com/odde/doughnut/entities/SessionItem.java` | model | CRUD | `SessionItem.java` (`feedbackScore`, `feedbackRecordedAt`) | exact |
| `backend/src/main/java/com/odde/doughnut/services/LearningSessionService.java` | service | request-response | `LearningSessionService.record()` (lines 74–143) | exact |
| `backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java` | model | transform | `MemoryTracker.recordCommissionedFeedback()` (lines 203–209) | role-match |
| `backend/src/main/java/com/odde/doughnut/controllers/dto/RecordedLearningSessionLite.java` | model | transform | `AwaitingReportLearningSessionLite.java` | exact |
| `backend/src/main/java/com/odde/doughnut/controllers/dto/DueMemoryTrackers.java` | model | transform | `DueMemoryTrackers.awaitingReportSessions` | exact |
| `backend/src/main/java/com/odde/doughnut/services/RecallService.java` | service | transform | `RecallService.toAwaitingReportLite()` + `awaitingReportSessions` (lines 108–131) | exact |
| `backend/src/main/java/com/odde/doughnut/controllers/LearningSessionController.java` | controller | request-response | `LearningSessionController.record()` (lines 68–85) | exact |
| `backend/src/test/java/com/odde/doughnut/testability/builders/SessionItemBuilder.java` | config | CRUD | `SessionItemBuilder.feedbackScore()` / `feedbackRecordedAt()` | exact |
| `backend/src/test/java/com/odde/doughnut/controllers/LearningSessionControllerTests.java` | test | request-response | `LearningSessionControllerTests.Record` nested class | exact |
| `backend/src/test/java/com/odde/doughnut/algorithms/CommissionedLearningSessionFeedbackPolicyTest.java` | test | transform | same file (`scoreFiveSchedulesLaterThanScoreOneFromSameStartingState`) | exact |
| `backend/src/test/java/com/odde/doughnut/controllers/RecallsControllerTests.java` | test | CRUD | `returnsAwaitingReportSessionsAfterCommission` | exact |
| `frontend/src/components/recall/CommissionLearningSessionDialog.vue` | component | request-response | same file `mode="record"` branch (lines 66–81, 152–175) | exact |
| `frontend/src/components/recall/RecallProgressBar.vue` | component | event-driven | `awaitingReportSessions` strip (lines 86–129) | exact |
| `frontend/src/composables/useRecallData.ts` | composable | pub-sub | `awaitingReportSessions` ref + setter (lines 15–20, 26–28, 76–80) | exact |
| `frontend/src/composables/useRecallPageLoading.ts` | composable | request-response | `setAwaitingReportSessions(response.awaitingReportSessions ?? [])` (line 64) | exact |
| `e2e_test/step_definitions/learning_session.ts` | route | request-response | `Given I have commissioned a learning session…` (lines 16–25) | exact |
| `e2e_test/features/learning_session/commissioned_learning_session.feature` | config | batch | existing record scenario in same file | exact |
| `MemoryTracker.restorePreSessionSnapshot()` (new method) | model | transform | — | no analog |

## Pattern Assignments

### `backend/src/main/resources/db/migration/V300000241__session_item_pre_session_snapshot.sql` (migration, transform)

**Analog:** `backend/src/main/resources/db/migration/V300000238__add_memory_tracker_type.sql`

**Column-add pattern** (V300000238 lines 1–4):

```sql
ALTER TABLE `memory_tracker`
  ADD COLUMN `type` VARCHAR(32) NOT NULL DEFAULT 'UNDERSTANDING';

UPDATE `memory_tracker` SET `type` = 'SPELLING' WHERE `spelling` = 1;
```

Adapt for `session_item`: add nullable `pre_session_forgetting_curve_index` (FLOAT) and `pre_session_recall_count` (INT) per CONTEXT D-02. Use next version **> 300000240**. One atomic migration file; no change to `V100000000__baseline.sql` until a squash.

---

### `backend/src/main/java/com/odde/doughnut/entities/SessionItem.java` (model, CRUD)

**Analog:** `SessionItem.java` existing feedback columns

**Column mapping pattern** (lines 29–33):

```java
@Column(name = "feedback_score")
private Integer feedbackScore;

@Column(name = "feedback_recorded_at")
private Timestamp feedbackRecordedAt;
```

Add snapshot columns with same `@Column` + Lombok `@Getter/@Setter` style:

```java
@Column(name = "pre_session_forgetting_curve_index")
private Float preSessionForgettingCurveIndex;

@Column(name = "pre_session_recall_count")
private Integer preSessionRecallCount;
```

---

### `backend/src/main/java/com/odde/doughnut/services/LearningSessionService.java` (service, request-response)

**Analog:** `LearningSessionService.record()` — extend in place; same `RecordLearningSessionResponse` shape (CONTEXT D-04).

**Session resolution** (lines 77–86) — branch when no `AWAITING_REPORT`:

```java
List<LearningSession> awaitingSessions =
    learningSessionRepository.findByUser_IdAndNotebook_IdAndStatus(
        user.getId(), notebook.getId(), LearningSessionStatus.AWAITING_REPORT);

if (awaitingSessions.isEmpty()) {
  throw new ResponseStatusException(
      HttpStatus.NOT_FOUND, "No learning session awaiting report for this notebook.");
}

LearningSession session = awaitingSessions.getFirst();
```

**Amend branch:** when `awaitingSessions.isEmpty()`, resolve latest `RECORDED` via existing `findByUser_IdAndNotebook_IdAndStatus(..., RECORDED)` (same repository method as `LearningSessionControllerTests` lines 247–250). Throw 404 only when neither status exists.

**Parse + partial-success loop** (lines 89–130) — reuse unchanged for amend:

```java
ParseResult parseResult =
    learningSessionReportParser.parse(reportMarkdown, sessionItemTitles, ambiguousTitles);
// ...
for (ParsedReportEntry entry : parseResult.entries()) {
    SessionItem matched = sessionItems.stream()
        .filter(item -> item.getNoteTitle().equals(entry.noteTitle()))
        .findFirst().orElse(null);
    // reject or apply...
    matched.setFeedbackScore(entry.score());
    matched.setFeedbackRecordedAt(now);
    MemoryTracker tracker = matched.getMemoryTracker();
    tracker.recordCommissionedFeedback(now, entry.score());
    sessionItemRepository.save(matched);
}
```

**First-record snapshot capture (new):** immediately before `recordCommissionedFeedback` on a matched item whose `preSessionRecallCount == null`, persist:

```java
matched.setPreSessionForgettingCurveIndex(tracker.getForgettingCurveIndex());
matched.setPreSessionRecallCount(tracker.getRecallCount());
```

**Amend path (new):** when session is already `RECORDED` and item has snapshot, call `tracker.restorePreSessionSnapshot(matched)` then `recordCommissionedFeedback` once (no extra `recallCount` bump — CONTEXT D-01).

**Status / recordedAt** (lines 132–140) — amend keeps `RECORDED`; update `recordedAt` when ≥1 match (planner discretion D-03):

```java
if (!response.getRecordedItems().isEmpty()) {
  session.setStatus(LearningSessionStatus.RECORDED);
  session.setRecordedAt(now);
  learningSessionRepository.save(session);
  response.setStatus(LearningSessionStatus.RECORDED);
  response.setRecordedAt(now);
}
```

On amend with zero matches: leave prior feedback unchanged; session stays `RECORDED` (CONTEXT D-05).

---

### `backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java` (model, transform)

**Analog:** `recordCommissionedFeedback()` (lines 203–209)

**Apply feedback pattern:**

```java
public void recordCommissionedFeedback(Timestamp now, int score) {
  setRecallCount(getRecallCount() + 1);
  setLastRecalledAt(now);
  setForgettingCurveIndex(
      CommissionedLearningSessionFeedbackPolicy.applyScore(getForgettingCurveIndex(), score));
  setNextRecallAt(ensureNextRecallStrictlyAfterNow(now));
}
```

**New `restorePreSessionSnapshot(SessionItem item)`** — no existing analog; mirror field writes in reverse using snapshot columns only (`forgettingCurveIndex`, `recallCount`; optionally `lastRecalledAt` if planner adds `preSessionLastRecalledAt`). Do **not** increment `recallCount` on restore. Unit-test primary per CONTEXT D-13.

---

### `backend/src/main/java/com/odde/doughnut/controllers/dto/RecordedLearningSessionLite.java` (model, transform)

**Analog:** `AwaitingReportLearningSessionLite.java`

**DTO shape** (lines 7–21):

```java
@Getter
@Setter
public class AwaitingReportLearningSessionLite {
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private int notebookId;
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String notebookName;
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private int learningSessionId;
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String requestMarkdown;
}
```

Clone as `RecordedLearningSessionLite` with identical fields (CONTEXT D-07). Add to `DueMemoryTrackers` beside `awaitingReportSessions`.

---

### `backend/src/main/java/com/odde/doughnut/services/RecallService.java` (service, transform)

**Analog:** `awaitingReportSessions` population (lines 108–131)

**List population pattern:**

```java
dueMemoryTrackers.setAwaitingReportSessions(
    learningSessionRepository
        .findByUser_IdAndStatus(user.getId(), LearningSessionStatus.AWAITING_REPORT)
        .stream()
        .map(session -> toAwaitingReportLite(session, timeZone))
        .toList());
```

**Lite mapper** (lines 123–131):

```java
private AwaitingReportLearningSessionLite toAwaitingReportLite(
    LearningSession session, ZoneId zoneId) {
  AwaitingReportLearningSessionLite lite = new AwaitingReportLearningSessionLite();
  lite.setNotebookId(session.getNotebook().getId());
  lite.setNotebookName(session.getNotebook().getName());
  lite.setLearningSessionId(session.getId());
  lite.setRequestMarkdown(learningSessionRequestMarkdownBuilder.build(session, zoneId));
  return lite;
}
```

Add `recordedSessions` via `findByUser_IdAndStatus(..., RECORDED)` + `toRecordedLite` (same mapper body). No change to `dueCommissioned` exclusion logic — recorded trackers remain scheduled normally.

---

### `backend/src/main/java/com/odde/doughnut/controllers/LearningSessionController.java` (controller, request-response)

**Analog:** `record()` endpoint (lines 68–85) — **no signature change**; amend is service-internal.

```java
@PostMapping("/record")
@Transactional
public RecordLearningSessionResponse record(
    @RequestBody RecordLearningSessionRequest body,
    @RequestParam(value = "timezone") String timezone)
    throws UnexpectedNoAccessRightException {
  authorizationService.assertLoggedIn();
  Notebook notebook = notebookRepository.findById(body.notebookId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notebook not found."));
  authorizationService.assertAuthorization(notebook);
  ZoneId zoneId = TimezoneUtils.parseTimezone(timezone);
  Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
  return learningSessionService.record(
      authorizationService.getCurrentUser(), notebook, body.reportMarkdown, now, zoneId);
}
```

Regenerate OpenAPI after DTO change only (`DueMemoryTrackers.recordedSessions`).

---

### `backend/src/test/java/com/odde/doughnut/testability/builders/SessionItemBuilder.java` (config, CRUD)

**Analog:** `feedbackScore` / `feedbackRecordedAt` (lines 31–38)

```java
public SessionItemBuilder feedbackScore(Integer feedbackScore) {
  entity.setFeedbackScore(feedbackScore);
  return this;
}

public SessionItemBuilder feedbackRecordedAt(Timestamp feedbackRecordedAt) {
  entity.setFeedbackRecordedAt(feedbackRecordedAt);
  return this;
}
```

Add `preSessionForgettingCurveIndex(Float)` and `preSessionRecallCount(Integer)` with same fluent style. Extend `LearningSessionControllerTestBase.addRecordedFeedback` or add `recordedLearningSessionWithScores` helper that runs through real `record()` or sets snapshot + feedback for amend Given steps (CONTEXT D-12).

**Base helper pattern** (`LearningSessionControllerTestBase` lines 39–59):

```java
protected LearningSession recordedLearningSession(Notebook notebook, Timestamp at) {
  return makeMe.aLearningSession()
      .forNotebook(notebook)
      .by(currentUser.getUser())
      .status(LearningSessionStatus.RECORDED)
      .commissionedAt(at)
      .recordedAt(at)
      .please();
}

protected void addRecordedFeedback(
    LearningSession session, MemoryTracker tracker, int score, Timestamp at) {
  makeMe.aSessionItem()
      .learningSession(session)
      .memoryTracker(tracker)
      .feedbackScore(score)
      .feedbackRecordedAt(at)
      .please();
}
```

For amend unit tests, prefer driving `controller.record()` twice (first record, then amend paste) like `Record.recordsSpanishNotebookSessionWithMatchedScores` (lines 231–258).

---

### `backend/src/test/java/com/odde/doughnut/controllers/LearningSessionControllerTests.java` (test, request-response)

**Analog:** `Record` nested class (lines 220–362)

**Happy-path record test** (lines 231–258):

```java
@Test
void recordsSpanishNotebookSessionWithMatchedScores() throws UnexpectedNoAccessRightException {
  Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
  testabilitySettings.timeTravelTo(dayTwo);
  Notebook notebook = spanishNotebook(dayTwo);
  controller.commission(commissionRequest(notebook), "Asia/Shanghai");
  RecordLearningSessionResponse response =
      controller.record(recordRequest(notebook, HOLA_GRACIAS_REPORT), "Asia/Shanghai");
  assertThat(response.getStatus(), equalTo(LearningSessionStatus.RECORDED));
  // ...
  for (SessionItem item : sessionItemRepository.findByLearningSession_Id(session.getId())) {
    assertThat(item.getMemoryTracker().getRecallCount(), equalTo(1));
  }
}
```

**Amend tests to add:** (1) second `record()` with partial report updates only matched item; (2) `recallCount` stays 1 after amend; (3) `preSession*` populated on first record; (4) zero-match amend leaves scores unchanged; (5) high→low amend reschedules per policy (day-3 `dueCommissioned` empty — mirror `RecallsControllerTests.dayThreeDueCommissionedOnlyGraciasAfterRecordedScores`).

---

### `backend/src/test/java/com/odde/doughnut/algorithms/CommissionedLearningSessionFeedbackPolicyTest.java` (test, transform)

**Analog:** `scoreFiveSchedulesLaterThanScoreOneFromSameStartingState` (lines 34–43)

```java
highScoreTracker.recordCommissionedFeedback(recordedAt, 5);
lowScoreTracker.recordCommissionedFeedback(recordedAt, 1);
assertThat(highScoreTracker.getNextRecallAt(), greaterThan(lowScoreTracker.getNextRecallAt()));
```

**Amend regression test (new):** record score 1, restore snapshot, `recordCommissionedFeedback` with score 4 — assert `recallCount == 1` and schedule matches fresh score-4 from initial state (not compound on post-score-1 state). CONTEXT D-01 / D-13.

---

### `backend/src/test/java/com/odde/doughnut/controllers/RecallsControllerTests.java` (test, CRUD)

**Analog:** `returnsAwaitingReportSessionsAfterCommission` (lines 219–246)

```java
DueMemoryTrackers afterCommission = controller.recalling("Asia/Shanghai", 0);
assertThat(afterCommission.getAwaitingReportSessions(), hasSize(1));
assertEquals("Spanish conversation",
    afterCommission.getAwaitingReportSessions().get(0).getNotebookName());
assertThat(
    afterCommission.getAwaitingReportSessions().get(0).getRequestMarkdown(),
    org.hamcrest.Matchers.containsString("### Hola"));
```

Add `returnsRecordedSessionsAfterRecord` asserting `getRecordedSessions()` size, notebook name, and `requestMarkdown` after full record flow.

---

### `frontend/src/components/recall/CommissionLearningSessionDialog.vue` (component, request-response)

**Analog:** `mode="record"` implementation (lines 101–175)

**Props + initial state** (lines 101–120):

```typescript
const props = defineProps<{
  notebookId: number
  notebookName: string
  mode?: "commission" | "record"
  initialRequestMarkdown?: string
}>()

const commissioned = ref(props.mode === "record")
const status = ref<LearningSessionCommissionResponse["status"] | "">(
  props.mode === "record" ? "AWAITING_REPORT" : ""
)
```

Extend `mode` to `"commission" | "record" | "amend"`. For amend: `commissioned = true`, `status = "RECORDED"`, show recorded banner + report textarea (CONTEXT D-08).

**Recorded banner** (lines 49–51):

```vue
<div v-if="status === 'RECORDED'" class="daisy-alert daisy-alert-info mt-4" data-test="learning-session-recorded">
  <span>This learning session is recorded.</span>
</div>
```

**Report textarea + CTA** (lines 66–81) — show when `status === 'AWAITING_REPORT' || status === 'RECORDED'` (amend):

```vue
<template v-if="status === 'AWAITING_REPORT'">
  <p class="text-sm mt-4">Learning session report</p>
  <textarea v-model="reportMarkdown" data-test="learning-session-report" ... />
  <button data-test="record-learning-session-report" @click="recordReport">Record report</button>
</template>
```

**API call** (lines 152–175) — reuse same `LearningSessionController.record` + `apiCallWithLoading`; amend uses identical POST body.

---

### `frontend/src/components/recall/RecallProgressBar.vue` (component, event-driven)

**Analog:** `awaitingReportSessions` strip (lines 86–129)

**Strip row pattern:**

```vue
<div v-if="awaitingReportSessions.length > 0" class="flex flex-col gap-2 px-4">
  <div
    v-for="session in awaitingReportSessions"
    :key="session.learningSessionId"
    data-test="awaiting-report-learning-session"
    role="status"
    class="flex gap-2 items-start text-base font-normal text-base-content"
  >
    <span class="flex-1 break-words">
      1 learning session awaiting the tutor's report for notebook "{{ session.notebookName }}"
    </span>
    <button
      type="button"
      class="daisy-btn daisy-btn-primary shrink-0"
      data-test="record-learning-session-report"
      @click="openRecordDialog(session)"
    >
      Record report
    </button>
  </div>
</div>
```

Clone for `recordedSessions`: `data-test="recorded-learning-session"`, copy `1 recorded learning session for notebook "{name}"`, CTA **`Amend report`** (new `data-test="amend-learning-session-report"` or reuse strip button pattern), `openAmendDialog(session)` opening dialog with `mode="amend"`.

**Second dialog instance** (lines 120–129):

```vue
<CommissionLearningSessionDialog
  v-if="recordDialogSession"
  mode="record"
  :notebook-id="recordDialogSession.notebookId"
  :notebook-name="recordDialogSession.notebookName"
  :initial-request-markdown="recordDialogSession.requestMarkdown"
  @close="recordDialogSession = undefined"
  @commissioned="onCommissioned"
  @recorded="onRecorded"
/>
```

Add `amendDialogSession` ref + sibling dialog with `mode="amend"` and `status` starting at `RECORDED`.

---

### `frontend/src/composables/useRecallData.ts` (composable, pub-sub)

**Analog:** `AwaitingReportSession` + `awaitingReportSessions` (lines 15–28, 76–80, 124)

```typescript
export type AwaitingReportSession = {
  notebookId: number
  notebookName: string
  learningSessionId: number
  requestMarkdown: string
}

const awaitingReportSessions = ref<AwaitingReportSession[] | undefined>(undefined)

const setAwaitingReportSessions = (sessions: AwaitingReportSession[] | undefined) => {
  awaitingReportSessions.value = sessions
}
```

Add `RecordedSession` type (same shape), `recordedSessions` ref, `setRecordedSessions`, export in return object. Mirror naming from generated `RecordedLearningSessionLite` after OpenAPI regen.

---

### `frontend/src/composables/useRecallPageLoading.ts` (composable, request-response)

**Analog:** line 64

```typescript
setAwaitingReportSessions(response.awaitingReportSessions ?? [])
```

Add `setRecordedSessions` to options + call `setRecordedSessions(response.recordedSessions ?? [])` beside awaiting line. Wire through `RecallPage.vue` like `awaitingReportSessions` (lines 21, 109).

---

### `e2e_test/step_definitions/learning_session.ts` (route, request-response)

**Analog:** Given commissioned session (lines 16–25) + When record (lines 27–35)

**Given commissioned:**

```typescript
Given(
  'I have commissioned a learning session for notebook {string} on day {int} with session items for notes {string}',
  (notebookTitle: string, day: number, _noteTitles: string) => {
    start.testability().timeTravelTo(day, 9)
    start.recall().navigateToRecallPage().commissionLearningSession(notebookTitle)
  }
)
```

**New Given** `I have recorded a learning session for notebook … on day {n} with scores:` — time travel, commission, paste full report via `recordLearningSessionReport`, or testability API if faster. Reuse When `I record the learning session report…` for amend step (lines 27–35).

**Then tutor feedback** (lines 121–127) — unchanged; asserts amended score after amend.

---

### `e2e_test/features/learning_session/commissioned_learning_session.feature` (config, batch)

**Analog:** amend scenario draft (`.planning/phases/01-commissioned-tracker-model/commissioned_learning_session.feature` lines 65–78)

```gherkin
Scenario: A later report amends the feedback of a recorded learning session
  Given I have recorded a learning session for notebook "Spanish conversation" on day 2 with scores:
    | Note    | Score |
    | Hola    | 4     |
    | Gracias | 1     |
  When I record the learning session report for the learning session of notebook "Spanish conversation":
    """
    # Learning Session Report

    Gracias: 4
    """
  Then I should see tutor feedback score 4 from a learning session for the memory tracker of note "Gracias"
  When It's day 3, 9 hour
  Then I should see 0 potential learning session to commission for notebook "Spanish conversation"
```

Tag `@wip` until green (CONTEXT D-11). Reuse `recallPage.recordLearningSessionReport` — scopes to dialog (lines 198–209).

---

## Shared Patterns

### Partial report acceptance (ADR 0005)
**Source:** `LearningSessionService.record()` lines 89–130
**Apply to:** first record and amend — same parser, same `rejectedEntries` accumulation, no rollback of matched items.

### Progress-bar strip affordances
**Source:** `RecallProgressBar.vue` potential (60–85) + awaiting (86–111) strips
**Apply to:** recorded-session strip — same `flex flex-col gap-2 px-4`, `role="status"`, `daisy-btn-primary`, one row per notebook.

### Recalling payload extension
**Source:** `RecallService.getDueMemoryTrackers` + `DueMemoryTrackers`
**Apply to:** add `recordedSessions` list without new endpoint; frontend refresh via `requestDueRecallsRefresh()` after amend (`RecallProgressBar` lines 194–196).

### API mutation + loading
**Source:** `CommissionLearningSessionDialog.recordReport()` lines 152–163
**Apply to:** amend uses same `LearningSessionController.record` + `apiCallWithLoading` + `timezoneParam()`.

### Latest tutor feedback display
**Source:** `SessionItemRepository.findLatestFeedbackScoreByMemoryTrackerId` + `NoteInfoMemoryTracker.vue` lines 56–61
**Apply to:** no UI change — amend updates `feedbackRecordedAt` so latest score query returns amended value (CONTEXT D-09).

### E2E dialog scoping
**Source:** `e2e_test/start/pageObjects/recallPage.ts` lines 198–209
**Apply to:** always scope report paste to `[data-test="commission-learning-session-dialog"]` to avoid strip/button homonyms.

### Unit-test primary for policy math
**Source:** `CommissionedLearningSessionFeedbackPolicyTest` + `LearningSessionControllerTests.Record`
**Apply to:** snapshot restore, no double `recallCount`, compound-vs-snapshot — not E2E (CONTEXT D-13).

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `MemoryTracker.restorePreSessionSnapshot()` | model | transform | No undo/restore pattern on trackers; new domain method required for amend re-grade |

## Metadata

**Analog search scope:** Phase 5–6 learning-session stack (`LearningSessionService`, `RecallProgressBar`, `CommissionLearningSessionDialog`, `RecallsController`/`RecallService`, `SessionItem`, Flyway `V300000240`, E2E `learning_session.ts` / `recallPage.ts`, controller + policy tests)
**Files scanned:** ~35
**Pattern extraction date:** 2026-08-08
