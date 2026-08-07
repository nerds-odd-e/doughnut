# Phase 3: potential-learning-sessions - Pattern Map

**Mapped:** 2026-08-08
**Files analyzed:** 16
**Analogs found:** 16 / 16

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `backend/.../dto/DueCommissionedMemoryTrackerLite.java` | model (DTO) | request-response | `backend/.../dto/MemoryTrackerLite.java` | exact |
| `backend/.../dto/DueMemoryTrackers.java` | model (DTO) | request-response | itself (`DueMemoryTrackers.java`) | exact |
| `backend/.../repositories/MemoryTrackerRepository.java` | repository | CRUD (native query) | itself — `byUserIdFrom` sibling | exact |
| `backend/.../services/RecallService.java` | service | request-response | itself — `getDueMemoryTrackers` lite map | exact |
| `backend/.../services/UserService.java` (optional sibling method) | service | CRUD | itself — `getMemoryTrackersNeedToRepeat` | exact |
| `backend/.../controllers/RecallsController.java` | controller | request-response | itself — no signature change expected | exact |
| `backend/.../RecallsControllerTests.java` | test | request-response | itself — `shouldExcludeCommissioned…` | exact |
| `frontend/src/composables/useRecallData.ts` | store / hook | transform | itself — `toRepeat` + `toRepeatCount` | exact |
| `frontend/src/composables/useRecallPageLoading.ts` | hook | request-response | itself — `loadMore` / `setToRepeat` | exact |
| `frontend/src/components/toolbars/MainMenu.vue` | component | request-response | itself — `menuData.recallStatus` apply | exact |
| `frontend/src/components/recall/RecallProgressBar.vue` | component | transform | itself + `ProgressBar.vue` layout | exact |
| `frontend/src/pages/RecallPage.vue` | component / page | request-response | itself — props into `RecallProgressBar` | exact |
| `packages/.../DueMemoryTrackersBuilder.ts` | utility / fixture | transform | itself | exact |
| `frontend/tests/pages/recallPageTestSupport.ts` | test | request-response | itself — `createUseRecallDataMock` | exact |
| `frontend/tests/pages/RecallPage.spec.ts` (extend / sibling) | test | request-response | itself + `recallPageTestSupport` | role-match |
| `e2e_test/features/learning_session/commissioned_learning_session.feature` | test (E2E) | request-response | draft under `.planning/phases/01-…/commissioned_learning_session.feature` | exact |
| `e2e_test/step_definitions/recall.ts` (+ assimilation helpers) | test | request-response | itself — `I should see that I have {int} notes to recall` | exact |
| `e2e_test/start/pageObjects/recallPage.ts` | test | request-response | itself — `expectCount` / `navigateToRecallPage` | exact |
| `e2e_test/start/testability.ts` | test utility | request-response | itself — `assimilateNote` | exact |

**Do not modify (keep ordinary-only):** `ProgressBar.vue` progress math, `useNavigationItems.ts` badge (`toRepeatCount`), `byUserIdFrom` exclusion fragment.

**Generated (regenerate, never hand-edit):** `packages/generated/doughnut-backend-api/**` via `pnpm generateTypeScript`.

## Pattern Assignments

### `DueCommissionedMemoryTrackerLite.java` (model / DTO, request-response)

**Analog:** `backend/src/main/java/com/odde/doughnut/controllers/dto/MemoryTrackerLite.java`

**Imports / Lombok + OpenAPI Schema pattern** (lines 1–16):
```java
package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemoryTrackerLite {
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private int memoryTrackerId;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean spelling;

  private String propertyKey;
}
```

**Copy for new companion DTO:** same package, `@Getter`/`@Setter`, `@Schema(REQUIRED)` on `memoryTrackerId`, `notebookId`, `notebookName`. Do **not** reuse `MemoryTrackerLite` or add notebook fields onto it.

---

### `DueMemoryTrackers.java` (model / DTO, request-response)

**Analog:** itself — `backend/src/main/java/com/odde/doughnut/controllers/dto/DueMemoryTrackers.java`

**Additive field pattern** (lines 9–14):
```java
public class DueMemoryTrackers {
  @NotNull public int totalAssimilatedCount;
  @Getter @Setter private Timestamp currentRecallWindowEndAt;
  @Getter @Setter private List<MemoryTrackerLite> toRepeat;
  @Getter @Setter private Integer dueInDays;
}
```

**Apply:** add `@Getter @Setter private List<DueCommissionedMemoryTrackerLite> dueCommissioned;` — leave `toRepeat` / `totalAssimilatedCount` semantics unchanged (D-01, D-05).

---

### `MemoryTrackerRepository.java` (repository, CRUD native query)

**Analog:** itself — `byUserIdFrom` + due stream query

**Ordinary fragment to leave intact** (lines 64–69):
```java
  String byUserIdFrom =
      " FROM memory_tracker rp "
          + " WHERE rp.user_id = :userId "
          + "   AND rp.removed_from_tracking IS FALSE "
          + "   AND rp.deleted_at IS NULL "
          + "   AND rp.type <> 'COMMISSIONED' ";
```

**Due stream query to mirror** (lines 27–34):
```java
  @Query(
      value =
          "SELECT rp.* "
              + byUserIdFrom
              + " AND rp.next_recall_at <= :nextRecallAt ORDER BY rp.next_recall_at, (rp.type = 'SPELLING') DESC",
      nativeQuery = true)
  Stream<MemoryTracker> findAllByUserAndNextRecallAtLessThanEqualOrderByNextRecallAt(
      @Param("userId") Integer userId, @Param("nextRecallAt") Timestamp nextRecallAt);
```

**Apply:** add sibling string `byUserIdCommissionedFrom` with `AND rp.type = 'COMMISSIONED'` (not `<>`), plus `findAllCommissionedByUserAndNextRecallAtLessThanEqual…` using the same `next_recall_at` cutoff. **Never** edit `byUserIdFrom`.

---

### `RecallService.java` (service, request-response)

**Analog:** itself — lite mapping in `getDueMemoryTrackers`

**Core map-to-lite + DTO fill** (lines 47–71):
```java
  public DueMemoryTrackers getDueMemoryTrackers(
      User user, Timestamp currentUTCTimestamp, ZoneId timeZone, int dueInDays) {
    List<MemoryTrackerLite> toRepeat =
        getMemoryTrackersNeedToRepeat(user, currentUTCTimestamp, timeZone, dueInDays)
            .map(
                mt -> {
                  MemoryTrackerLite lite = new MemoryTrackerLite();
                  lite.setMemoryTrackerId(mt.getId());
                  lite.setSpelling(mt.isSpelling());
                  String propertyKey = mt.getPropertyKey();
                  lite.setPropertyKey(
                      propertyKey == null || propertyKey.isEmpty() ? null : propertyKey);
                  return lite;
                })
            .toList();
    DueMemoryTrackers dueMemoryTrackers = new DueMemoryTrackers();
    dueMemoryTrackers.setDueInDays(dueInDays);
    dueMemoryTrackers.setToRepeat(toRepeat);
    dueMemoryTrackers.totalAssimilatedCount = totalAssimilatedCount(user);
    dueMemoryTrackers.setCurrentRecallWindowEndAt(
        TimestampOperations.alignByHalfADay(currentUTCTimestamp, timeZone));
    return dueMemoryTrackers;
  }
```

**Cutoff helper to reuse** (lines 39–45):
```java
  private Stream<MemoryTracker> getMemoryTrackersNeedToRepeat(
      User user, Timestamp currentUTCTimestamp, ZoneId timeZone, int dueInDays) {
    return userService.getMemoryTrackersNeedToRepeat(
        user,
        TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, dueInDays * 24),
        timeZone);
  }
```

**Apply:** parallel stream → map `DueCommissionedMemoryTrackerLite` with `mt.getNote().getNotebook()` id/name **inside** this method (controller `@Transactional` keeps session open). `setDueCommissioned(...)` on the same DTO. Optional: mirror cutoff via new `UserService` method calling the commissioned repository query (same half-day align as below).

**UserService ordinary due path** (`UserService.java` lines 65–69):
```java
  public Stream<MemoryTracker> getMemoryTrackersNeedToRepeat(
      User user, Timestamp currentUTCTimestamp, ZoneId timeZone) {
    final Timestamp timestamp = TimestampOperations.alignByHalfADay(currentUTCTimestamp, timeZone);
    return memoryTrackerRepository.findAllByUserAndNextRecallAtLessThanEqualOrderByNextRecallAt(
        user.getId(), timestamp);
  }
```

---

### `RecallsController.java` (controller, request-response)

**Analog:** itself — no new endpoint

**Auth + transactional recalling** (lines 39–51):
```java
  @GetMapping(value = {"/recalling"})
  @Transactional
  public DueMemoryTrackers recalling(
      @RequestParam(value = "timezone") String timezone,
      @RequestParam(value = "dueindays", required = false) Integer dueInDays) {
    authorizationService.assertLoggedIn();
    ZoneId timeZone = TimezoneUtils.parseTimezone(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    return recallService.getDueMemoryTrackers(
        authorizationService.getCurrentUser(),
        currentUTCTimestamp,
        timeZone,
        dueInDays == null ? 0 : dueInDays);
  }
```

**Apply:** keep as-is; OpenAPI picks up new DTO field automatically. Menu `recallStatus` already uses `getDueMemoryTrackers` — same payload.

---

### `RecallsControllerTests.java` (test, request-response)

**Analog:** itself — exclusion test + helpers

**Helpers + exclusion canonical case** (lines 34–40, 136–148):
```java
  private Note ownedNote() {
    return makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
  }

  private MemoryTracker dueTracker(Note note, Timestamp nextRecallAt) {
    return makeMe.aMemoryTrackerFor(note).nextRecallAt(nextRecallAt).please();
  }

  @Test
  void shouldExcludeCommissionedMemoryTrackersFromOrdinaryRecallLists() {
    Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
    testabilitySettings.timeTravelTo(currentTime);
    Note note = ownedNote();
    dueTracker(note, currentTime);
    makeMe.aMemoryTrackerFor(note).commissioned().nextRecallAt(currentTime).please();

    DueMemoryTrackers dueMemoryTrackers = controller.recalling("Asia/Shanghai", 0);

    assertThat(dueMemoryTrackers.getToRepeat(), hasSize(1));
    assertEquals(1, dueMemoryTrackers.totalAssimilatedCount);
  }
```

**Apply (small-test style):** new test drives `controller.recalling` — only-commissioned-due → `toRepeat` empty **and** `dueCommissioned` contains `notebookName` / `notebookId`. Do not re-assert full exclusion shape in every sibling; keep Phase 1 exclusion test as canonical for ordinary lists. Prefer `makeMe.aMemoryTrackerFor(note).commissioned().nextRecallAt(...).please()`.

---

### `useRecallData.ts` (store / hook, transform)

**Analog:** itself — module-level refs + ordinary-only count

**Ordinary-only count (must stay)** (lines 6–21):
```typescript
const toRepeat = ref<MemoryTrackerLite[] | undefined>(undefined)
// ...
const toRepeatCount = computed(() => {
  const length = toRepeat.value?.length ?? 0
  const index = currentIndex.value
  return Math.max(0, length - index)
})
```

**Setter export pattern** (lines 26–28, 68–89):
```typescript
  const setToRepeat = (trackers: MemoryTrackerLite[] | undefined) => {
    toRepeat.value = trackers
  }
  return {
    toRepeatCount,
    toRepeat,
    setToRepeat,
    // ...
  }
```

**Grouping analog** (`useBookLayoutAiReorganize.ts` lines 19–23 — Map inside `computed`):
```typescript
  const previewRows = computed(() => {
    if (suggestion.value === null) return []
    const idToDepth = new Map(
      suggestion.value.blocks.map((e) => [e.id, e.depth] as const)
    )
```

**Apply:** add `dueCommissioned` ref + `setDueCommissioned`; add `potentialLearningSessions` `computed` grouping by `notebookId` (D-02). **Never** fold commissioned into `toRepeat` / `toRepeatCount` (D-05 — badge via `useNavigationItems` uses `toRepeatCount` only).

---

### `useRecallPageLoading.ts` (hook, request-response)

**Analog:** itself — wrapped SDK + set from recalling response

**SDK call + apply pattern** (lines 42–64):
```typescript
      const { data: response, error } = await RecallsController.recalling({
        query: {
          timezone: timezoneParam(),
          dueindays: dueInDays,
        },
      })
      if (!error && response) {
        let trackers = response.toRepeat
        currentIndex.value = 0
        setTotalAssimilatedCount(response.totalAssimilatedCount)
        setDiligentMode((dueInDays ?? 0) > 0)
        if (trackers?.length === 0) {
          setToRepeat(trackers)
          return response
        }
        if (getEnvironment() !== "testing" && trackers) {
          trackers = shuffle(trackers)
        }
        setToRepeat(trackers)
        return response
      }
```

**Apply:** always `setDueCommissioned(response.dueCommissioned)` from the same response (including early return when `toRepeat` is empty). Do **not** shuffle commissioned into the quiz queue. Extend options with `setDueCommissioned` callback.

---

### `MainMenu.vue` (component, request-response)

**Analog:** itself — menu recallStatus hydrate

**Apply recallStatus fields** (lines 45–59):
```typescript
const { setToRepeat, setCurrentRecallWindowEndAt, setTotalAssimilatedCount } =
  useRecallData()
// ...
    if (menuData.recallStatus) {
      setToRepeat(menuData.recallStatus.toRepeat)
      setCurrentRecallWindowEndAt(
        menuData.recallStatus.currentRecallWindowEndAt
      )
      setTotalAssimilatedCount(menuData.recallStatus.totalAssimilatedCount)
    }
```

**Apply:** also `setDueCommissioned(menuData.recallStatus.dueCommissioned)`. Badge still derives from `toRepeat` only via `useNavigationItems`.

---

### `RecallProgressBar.vue` (component, transform / display)

**Analog:** itself (slots) + `ProgressBar.vue` (ordinary math unchanged) + UI-SPEC sibling strip

**ProgressBar slot host** (`RecallProgressBar.vue` lines 1–56): wrap `ProgressBar` with `#buttons` / `#cogIcon`; ordinary props `finished`, `toRepeatCount`, `diligentMode` unchanged.

**ProgressBar ordinary math — do not change** (`ProgressBar.vue` lines 8–16):
```vue
    <div
      :class="['progress-bar', { thin : $slots.default !== undefined, 'diligent-mode': diligentMode }]"
      v-if="toRepeatCount !== null"
    >
      <span
        class="progress"
        :style="`width: ${(finished * 100) / (finished + toRepeatCount)}%`"
      >
```

**UI-SPEC display-only row (copy / testid):**
```vue
<div
  v-for="session in potentialLearningSessions"
  :key="session.notebookId"
  data-test="potential-learning-session"
  role="status"
>
  {{ session.trackerIds.length }} potential learning session to commission for notebook "{{ session.notebookName }}"
</div>
```

**Apply:** sibling strip **below** `ProgressBar` (not inside fill / not clickable `daisy-btn`). Vue text interpolation only — no `v-html`. Empty → render nothing.

**GlobalBar layout host** (`RecallPage.vue` lines 3–25): `GlobalBar` → `RecallProgressBar` — pass potential-session props here; keep `toRepeatCount` ordinary-only.

---

### `RecallPage.vue` (page, request-response)

**Analog:** itself — wire `useRecallData` into progress bar

**Props bind pattern** (lines 10–20):
```vue
      <RecallProgressBar
        v-bind="{
          finished,
          toRepeatCount,
          previousAnsweredQuestionCursor,
          canMoveToEnd: toRepeatCount > 0 && currentIndex < (toRepeat?.length ?? 0) - 1,
          currentIndex,
          totalAssimilatedCount: totalAssimilatedCount ?? 0,
          diligentMode: diligentMode,
          previousAnsweredQuestions,
        }"
```

**Apply:** pass `potentialLearningSessions` (or raw `dueCommissioned` if grouping stays in child). No commission click handlers (D-04).

---

### `DueMemoryTrackersBuilder.ts` (fixture utility, transform)

**Analog:** itself

**Builder pattern** (lines 7–30):
```typescript
class DueMemoryTrackersBuilder extends Builder<DueMemoryTrackers> {
  memoryTrackersToRepeat: MemoryTrackerLite[] = []
  private totalAssimilatedCountToUse = 100

  toRepeat(memoryTrackers: MemoryTrackerLite[]) {
    this.memoryTrackersToRepeat = memoryTrackers
    return this
  }

  do(): DueMemoryTrackers {
    return {
      toRepeat: this.memoryTrackersToRepeat,
      dueInDays: 0,
      totalAssimilatedCount: this.totalAssimilatedCountToUse,
      currentRecallWindowEndAt: new Date().toISOString(),
    }
  }
}
```

**Apply:** fluent `dueCommissioned([...])` defaulting to `[]` in `do()`. Exposed as `makeMe.aDueMemoryTrackersList` — keep capability name.

---

### Frontend unit tests (`RecallPage.spec.ts` / sibling + `recallPageTestSupport.ts`)

**Analog:** `frontend/tests/pages/recallPageTestSupport.ts` + `RecallPage.spec.ts`

**Mock factory** (`recallPageTestSupport.ts` lines 16–40):
```typescript
export function createUseRecallDataMock(overrides?: {
  toRepeat?: MemoryTrackerLite[]
  // ...
}) {
  const toRepeatRef = ref<MemoryTrackerLite[] | undefined>(overrides?.toRepeat)
  return {
    toRepeatCount: computed(() => toRepeatRef.value?.length ?? 0),
    toRepeat: toRepeatRef,
    setToRepeat: (trackers: MemoryTrackerLite[] | undefined) => {
      toRepeatRef.value = trackers
    },
    // ...
  }
}
```

**Page mount + makeMe** (`RecallPage.spec.ts` lines 70–77):
```typescript
  it("redirect to recall page if nothing to repeat", async () => {
    const repetition = makeMe.aDueMemoryTrackersList.please()
    vi.mocked(useRecallData).mockReturnValue(
      createUseRecallDataMock({ toRepeat: repetition.toRepeat })
    )
    await ctx.mountPage()
    expect(ctx.recallingSpy).not.toHaveBeenCalled()
  })
```

**Apply:** extend mock with `dueCommissioned` / `potentialLearningSessions`; assert progress-bar `data-test="potential-learning-session"` text includes notebook title; assert `toRepeatCount` / badge path unchanged when only commissioned due. Prefer mounted page/progress UI over isolated helper tests.

---

### E2E feature / steps / page object / testability

**Analog feature (scenarios to graduate):** `.planning/phases/01-commissioned-tracker-model/commissioned_learning_session.feature` lines 22–26, 38–46

```gherkin
  Scenario: Due commissioned trackers await a Tutor rather than ordinary recall
    Given the notes "Hola, Gracias" are assimilated as commissioned on day 1
    When It's day 2, 9 hour
    Then I should see that I have 0 notes to recall
    And I should see 1 potential learning session to commission for notebook "Spanish conversation"

  Scenario: Notes from different notebooks are commissioned as separate learning sessions
    ...
    Then I should see 1 potential learning session to commission for notebook "Spanish conversation"
    And I should see 1 potential learning session to commission for notebook "Kanji"
```

**Existing graduated feature host:** `e2e_test/features/learning_session/commissioned_learning_session.feature` — append only the two Phase 3 scenarios with `@wip` until green (D-06).

**Ordinary recall step analog** (`recall.ts` lines 52–57):
```typescript
Then(
  'I should see that I have {int} notes to recall',
  (numberOfNotes: number) => {
    cy.reload()
    start.recall().expectCount(numberOfNotes)
  }
)
```

**Sidebar count page object** (`recallPage.ts` lines 144–148):
```typescript
    expectCount(numberOfNotes: number) {
      getRecallListItemInSidebar(($el) => {
        $el.findByText(`${numberOfNotes}`, { selector: '.recall-count' })
      })
```

**Assimilation fixture analog** (`testability.ts` lines 571–579):
```typescript
    assimilateNote(noteTitle: string) {
      return this.getInjectedNoteIdByTitle(noteTitle).then((noteId) => {
        return cy.wrap(
          AssimilationController.assimilate({
            body: { noteId, skipMemoryTracking: false },
          }),
          { log: false }
        )
      })
    },
```

**Given ordinary assimilate day** (`assimilation.ts` lines 38–43):
```typescript
Given(
  'the note {string} was assimilated on day {int}',
  (noteTitle: string, day: number) => {
    start.testability().backendTimeTravelTo(day, 8)
    start.testability().assimilateNote(noteTitle)
  }
)
```

**Apply:**
- `assimilateNoteAsCommissioned` via `assimilateAsCommissioned: true` on same AssimilationController call
- Bulk Given: `the notes "{csv}" are assimilated as commissioned on day {n}` (+ notebook-scoped variant from draft)
- Then: navigate to recall page (progress bar), assert `[data-test=potential-learning-session]` contains glossary copy + notebook title — **not** sidebar-only
- Page-object method e.g. `expectPotentialLearningSession(count, notebookTitle)`
- Keep ordinary `expectCount` for TRK-03 zero badge

## Shared Patterns

### Authentication / session scope
**Source:** `RecallsController.java` lines 44, 39–40  
**Apply to:** No new endpoints — reuse `authorizationService.assertLoggedIn()` and `@Transactional` on recalling.

```java
authorizationService.assertLoggedIn();
// ...
return recallService.getDueMemoryTrackers(...);
```

### Ordinary vs commissioned dual feed
**Source:** `MemoryTrackerRepository.byUserIdFrom` + new sibling; `RecallService.getDueMemoryTrackers`  
**Apply to:** Repository + RecallService (+ optional UserService)  
- Ordinary: `type <> 'COMMISSIONED'` (unchanged)  
- Commissioned: `type = 'COMMISSIONED'` + same `next_recall_at` cutoff  
- Wire: both on `DueMemoryTrackers` one round-trip

### Frontend ordinary-only counts
**Source:** `useRecallData.ts` `toRepeatCount`; `useNavigationItems.ts` badge  
**Apply to:** All FE touch points — store `dueCommissioned` separately; never inflate badge / progress finished ratio.

### OpenAPI client sync
**Source:** repo `generate-api-client` / `pnpm generateTypeScript`  
**Apply to:** After Java DTO change — regenerate before FE/fixture compile; never hand-edit `packages/generated/`.

### API error handling (frontend)
**Source:** `useRecallPageLoading.ts` / `MainMenu.vue` — `{ data, error }` unwrap  
**Apply to:** Only set `dueCommissioned` when `!error && response`; do not treat load failure as empty potential sessions (UI-SPEC).

### E2E `@wip` + page objects
**Source:** planning.mdc / existing learning_session feature  
**Apply to:** Tag only the two graduated scenarios `@wip` until green; thin steps; assertions in `recallPage` page object; targeted `cypress run --spec e2e_test/features/learning_session/commissioned_learning_session.feature`.

### XSS / notebook title
**Source:** UI-SPEC + Vue default escaping  
**Apply to:** Interpolate `notebookName` as text only in potential-session rows.

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | All Phase 3 touch points have close in-repo analogs. Potential Learning Session is FE-derived (no new entity/repo). |

## Metadata

**Analog search scope:** `backend/.../dto`, `MemoryTrackerRepository`, `RecallService`, `UserService`, `RecallsController(+Tests)`, `frontend/src/composables`, `frontend/src/components/recall`, `frontend/src/pages/RecallPage.vue`, `MainMenu.vue`, `ProgressBar.vue`, `GlobalBar.vue`, `packages/doughnut-test-fixtures`, `frontend/tests/pages`, `e2e_test/{features,step_definitions,start}`  
**Files scanned:** ~35 (targeted)  
**Pattern extraction date:** 2026-08-08
