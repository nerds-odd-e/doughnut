# Phase 04: Learning Session and Request builder - Pattern Map

**Mapped:** 2026-08-08
**Files analyzed:** 14
**Analogs found:** 14 / 14

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `backend/.../db/migration/V300000240__learning_session_and_session_item.sql` | migration | CRUD | `V100000000__baseline.sql` (`question_generation_batch` + `_request`) | exact |
| `backend/.../entities/LearningSession.java` | model | CRUD | `QuestionGenerationBatch.java` | exact |
| `backend/.../entities/LearningSessionStatus.java` | model (enum) | transform | `QuestionGenerationBatchStatus.java` | exact |
| `backend/.../entities/SessionItem.java` | model | CRUD | `QuestionGenerationBatchRequest.java` | exact |
| `backend/.../entities/repositories/LearningSessionRepository.java` | repository | CRUD | `QuestionGenerationBatchRepository.java` | exact |
| `backend/.../entities/repositories/SessionItemRepository.java` | repository | CRUD / transform | `QuestionGenerationBatchRequestRepository.java` + `QuestionGenerationBatchRepository` aggregate query | role-match |
| `backend/.../services/LearningSessionService.java` | service | CRUD / transform | `QuestionGenerationBatchRetentionService.java` (delete) + `UserService.getCommissionedMemoryTrackersNeedToRepeat` | role-match |
| `backend/.../services/LearningSessionRequestMarkdownBuilder.java` | utility / service | transform | `FocusContextMarkdownRenderer.java` | exact |
| `backend/.../controllers/LearningSessionController.java` | controller | request-response | `AssimilationController.java` (POST) + `RecallsController.java` (timezone) | exact |
| `backend/.../controllers/dto/CommissionLearningSessionRequest.java` | model (DTO) | request-response | `AssimilationRequestDTO.java` | exact |
| `backend/.../controllers/dto/LearningSessionCommissionResponse.java` | model (DTO) | request-response | `DueCommissionedMemoryTrackerLite.java` | exact |
| `backend/.../controllers/LearningSessionControllerTests.java` | test | request-response | `RecallsControllerTests.java` + `AssimilationControllerAssimilateTests.java` | exact |
| `backend/.../testability/builders/LearningSessionBuilder.java` | utility / fixture | transform | `QuestionGenerationBatchBuilder.java` + `QuestionGenerationBatchRequestBuilder.java` | exact |
| `backend/.../testability/MakeMe.java` (register builder) | config | transform | `MakeMe.aQuestionGenerationBatch()` | exact |

**Optional (planner discretion):** `LearningSessionRequestMarkdownBuilderTest.java` — pure unit tests without DB; analog `FocusContextMarkdownRendererTest.java`.

**Do not modify (Structure phase):** recall DTOs, `RecallService`, frontend, E2E commission scenario (stay `@wip` until Phase 5).

**Generated (regenerate, never hand-edit):** `packages/generated/doughnut-backend-api/**` via `pnpm generateTypeScript` after controller/DTO add.

## Pattern Assignments

### `V300000240__learning_session_and_session_item.sql` (migration, CRUD)

**Analog:** `backend/src/main/resources/db/migration/V100000000__baseline.sql` lines 615–678 (`question_generation_batch` + `question_generation_batch_request`)

**Parent table pattern** (lines 615–629):
```sql
CREATE TABLE `question_generation_batch` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `planned_at` timestamp(3) NOT NULL,
  ...
  PRIMARY KEY (`id`),
  KEY `idx_question_generation_batch_user_status` (`user_id`,`status`),
  CONSTRAINT `fk_question_generation_batch_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB ... CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Child table pattern** (lines 662–677):
```sql
CREATE TABLE `question_generation_batch_request` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `batch_id` int unsigned NOT NULL,
  `memory_tracker_id` int unsigned NOT NULL,
  ...
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_question_generation_batch_request_batch_tracker` (`batch_id`,`memory_tracker_id`),
  CONSTRAINT `fk_question_generation_batch_request_batch` FOREIGN KEY (`batch_id`) REFERENCES `question_generation_batch` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_question_generation_batch_request_tracker` FOREIGN KEY (`memory_tracker_id`) REFERENCES `memory_tracker` (`id`) ON DELETE CASCADE
) ...
```

**Version tip pattern** — follow `V300000239__memory_tracker_unique_on_type_drop_spelling.sql`; next file is **`V300000240`** (version > `300000230` per `db-migration.mdc`).

**Apply for Phase 4:**
- `learning_session`: `id`, `user_id`, `notebook_id`, `status`, `commissioned_at` timestamp(3), `recorded_at` nullable; index `(user_id, notebook_id, status)` for abandon queries
- `session_item`: `id`, `learning_session_id`, `memory_tracker_id`, `note_title`, `feedback_score` nullable, `feedback_recorded_at` nullable
- FK `learning_session` → `user`, `notebook`; FK `session_item` → `learning_session` **ON DELETE CASCADE**, → `memory_tracker` (restrict or cascade per product — research recommends cascade on session abandon only)
- Same charset/collation/`int unsigned`/`timestamp(3)` conventions as baseline

---

### `LearningSession.java` (model, CRUD)

**Analog:** `backend/src/main/java/com/odde/doughnut/entities/QuestionGenerationBatch.java`

**Imports + entity shape** (lines 1–30):
```java
package com.odde.doughnut.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "question_generation_batch")
@Getter
@Setter
public class QuestionGenerationBatch extends EntityIdentifiedByIdOnly {

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private QuestionGenerationBatchStatus status;

  @Column(name = "planned_at", nullable = false)
  private Timestamp plannedAt;
```

**Base class** (`EntityIdentifiedByIdOnly.java` lines 8–14):
```java
@MappedSuperclass
public abstract class EntityIdentifiedByIdOnly {
  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  protected Integer id;
```

**Apply:** extend `EntityIdentifiedByIdOnly`; `@ManyToOne` `User user`, `Notebook notebook`; `LearningSessionStatus status`; `Timestamp commissionedAt`; nullable `Timestamp recordedAt`. Table name `learning_session`.

---

### `LearningSessionStatus.java` (model / enum, transform)

**Analog:** `backend/src/main/java/com/odde/doughnut/entities/QuestionGenerationBatchStatus.java`

```java
public enum QuestionGenerationBatchStatus {
  PLANNED,
  SUBMITTED,
  COMPLETED,
  FAILED,
  EXPIRED;
  // optional helper methods for terminal sets
}
```

**Apply:** `AWAITING_REPORT`, `RECORDED` only for MVP. Optional `isAwaitingReport()` helper if abandon query keys off status.

---

### `SessionItem.java` (model, CRUD)

**Analog:** `backend/src/main/java/com/odde/doughnut/entities/QuestionGenerationBatchRequest.java`

**Parent FK + tracker FK** (lines 17–25):
```java
public class QuestionGenerationBatchRequest extends EntityIdentifiedByIdOnly {

  @ManyToOne(optional = false)
  @JoinColumn(name = "batch_id", nullable = false)
  private QuestionGenerationBatch batch;

  @ManyToOne(optional = false)
  @JoinColumn(name = "memory_tracker_id", nullable = false)
  private MemoryTracker memoryTracker;
```

**Apply:** `@ManyToOne LearningSession learningSession`; `@ManyToOne MemoryTracker memoryTracker`; `String noteTitle` snapshot (protocol identifier per ADR 0005); nullable `Integer feedbackScore` (0–5); nullable `Timestamp feedbackRecordedAt`. Table `session_item`.

---

### `LearningSessionRepository.java` (repository, CRUD)

**Analog:** `backend/src/main/java/com/odde/doughnut/entities/repositories/QuestionGenerationBatchRepository.java`

**Interface + status/user queries** (lines 13–24):
```java
public interface QuestionGenerationBatchRepository
    extends JpaRepository<QuestionGenerationBatch, Integer> {

  List<QuestionGenerationBatch> findByStatus(QuestionGenerationBatchStatus status);

  boolean existsByUser_IdAndStatus(Integer userId, QuestionGenerationBatchStatus status);
```

**Apply:** extend `JpaRepository<LearningSession, Integer>`; add `List<LearningSession> findByUser_IdAndNotebook_IdAndStatus(...)` or `deleteByUser_IdAndNotebook_IdAndRecordedAtIsNull(...)` for abandon lifecycle; prefer Spring Data derived names matching column paths (`user.id`, `notebook.id`).

---

### `SessionItemRepository.java` (repository, CRUD / transform)

**Analog:** `QuestionGenerationBatchRequestRepository.java` + aggregate query from `QuestionGenerationBatchRepository`

**Delete by parent** (`QuestionGenerationBatchRequestRepository.java` lines 11–13):
```java
  List<QuestionGenerationBatchRequest> findByBatch_Id(Integer batchId);

  void deleteByBatch_Id(Integer batchId);
```

**Aggregate date query analog** (`QuestionGenerationBatchRepository.java` lines 46–51):
```java
  @Query(
      """
      SELECT MAX(b.submittedAt) FROM QuestionGenerationBatch b
      WHERE b.user.id = :userId AND b.submittedAt IS NOT NULL
      """)
  Optional<Timestamp> findLatestSubmittedAtByUser_Id(@Param("userId") Integer userId);
```

**Apply:** `deleteByLearningSession_Id`; JPQL for `countRecordedByMemoryTrackerId` (items with non-null `feedbackScore` / `feedbackRecordedAt` on **recorded** sessions); `findLastRecordedDate(memoryTrackerId, zoneId)` for learning status line. Scope queries by `learningSession.user.id` when loading by tracker.

---

### `LearningSessionService.java` (service, CRUD / transform)

**Analogs:** abandon delete — `QuestionGenerationBatchRetentionService.java`; due selection — `UserService.java` lines 72–77; note-level filter — `MemoryTracker.isNoteLevelTracker()`

**Abandon delete loop** (`QuestionGenerationBatchRetentionService.java` lines 50–53):
```java
    for (QuestionGenerationBatch batch : prunableBatches) {
      batchRequestRepository.deleteByBatch_Id(batch.getId());
    }
    batchRepository.deleteAll(prunableBatches);
```

**Due commissioned stream** (`UserService.java` lines 72–77):
```java
  public Stream<MemoryTracker> getCommissionedMemoryTrackersNeedToRepeat(
      User user, Timestamp currentUTCTimestamp, ZoneId timeZone) {
    final Timestamp timestamp = TimestampOperations.alignByHalfADay(currentUTCTimestamp, timeZone);
    return memoryTrackerRepository
        .findAllCommissionedByUserAndNextRecallAtLessThanEqualOrderByNextRecallAt(
            user.getId(), timestamp);
  }
```

**Note-level filter** (`MemoryTracker.java` lines 199–203):
```java
  public boolean isNoteLevelTracker() {
    String key = getPropertyKey();
    return key == null || key.isEmpty();
  }
```

**Commission flow shape:**
1. `abandonUnfinishedSessions(user, notebook)` — delete prior `AWAITING_REPORT` / `recorded_at IS NULL` sessions (+ cascade items)
2. Filter `userService.getCommissionedMemoryTrackersNeedToRepeat(...)` by `notebook.getId()` and `isNoteLevelTracker()`
3. If empty → `ResponseStatusException(HttpStatus.BAD_REQUEST, "...")` (see NotebookController not-found pattern below); do not create orphan session
4. Persist `LearningSession` (`AWAITING_REPORT`, `commissionedAt = now`)
5. Persist `SessionItem` per tracker with `noteTitle` snapshot from `note.getTitle()`
6. Return DTO with `learningSessionRequestMarkdownBuilder.build(session, zoneId)`

**Expected content helper:** `NoteContentMarkdown.bodyWithoutLeadingFrontmatter(note.getContent()).trim()` (`NoteContentMarkdown.java` lines 116–117).

**Date in learning status:** `DateTimeFormatter.ISO_LOCAL_DATE` in user zone (`RecallStatsAggregator.java` line 26).

`@Service` + `@Transactional` on `commission` method (controller may also be `@Transactional` like `AssimilationController`).

---

### `LearningSessionRequestMarkdownBuilder.java` (utility / service, transform)

**Analog:** `backend/src/main/java/com/odde/doughnut/services/focusContext/FocusContextMarkdownRenderer.java`

**StringBuilder section assembly** (lines 8–22):
```java
@Service
public class FocusContextMarkdownRenderer {

  public String render(FocusContextResult result, RetrievalConfig config) {
    StringBuilder sb = new StringBuilder();

    sb.append("# Focus Context\n\n");
    sb.append("Purpose: Context around the focus note for AI use.\n");
    // ...
    return sb.toString();
  }
```

**Per-item private append** (lines 25–38):
```java
  private void appendFocusNote(StringBuilder sb, FocusContextFocusNote focusNote) {
    sb.append("\n## Focus Note\n\n");
    sb.append("Title: ")
        .append(focusNote.getTitle() != null ? focusNote.getTitle() : "")
        .append("\n");
```

**Apply:** `@Service` class with `build(LearningSession session, ZoneId zoneId)`; private methods `appendRubric`, `appendSessionItem`; **copy ADR 0005 rubric lines verbatim** (no paraphrase). Structure:

```markdown
# Learning Session Request

Notebook: {notebook.title}

## How to report
…(verbatim 0–5 bullets)…

## Session Items

### {noteTitle}
- Expected learning content: {bodyWithoutLeadingFrontmatter trimmed}
- Learning status: {not yet tutored | N previous session(s), last on ISO date}
```

Order session items by due `next_recall_at` (same as commissioned due query). No template engine — `StringBuilder` only per RESEARCH.

**Test analog** (`FocusContextMarkdownRendererTest.java` lines 48–57):
```java
      assertThat(output, containsString("## Focus Note"));
      assertThat(output, containsString("Title: My Title"));
      assertThat(output, containsString("Notebook: My Notebook"));
```

Use one canonical snapshot test with Spanish notebook / Hola / Gracias fixture from `commissioned_learning_session.feature`.

---

### `LearningSessionController.java` (controller, request-response)

**Analog:** `AssimilationController.java` (POST + auth) + `RecallsController.java` (timezone param)

**POST + transactional** (`AssimilationController.java` lines 59–67):
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

**Timezone + testability clock** (`RecallsController.java` lines 39–51):
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

**Notebook resolve + auth** (`NotebookController.java` lines 457–463):
```java
    Notebook destinationNotebook =
        notebookRepository
            .findById(request.getDestinationNotebookId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notebook not found."));
    authorizationService.assertAuthorization(destinationNotebook);
```

**Apply:**
```java
@RestController
@SessionScope
@RequestMapping("/api/learning-sessions")
class LearningSessionController {
  @PostMapping("/commission")
  @Transactional
  public LearningSessionCommissionResponse commission(
      @RequestBody CommissionLearningSessionRequest body,
      @RequestParam String timezone) {
    authorizationService.assertLoggedIn();
    Notebook notebook = notebookRepository.findById(body.getNotebookId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notebook not found."));
    authorizationService.assertAuthorization(notebook);
    ZoneId zoneId = TimezoneUtils.parseTimezone(timezone);
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    return learningSessionService.commission(
        authorizationService.getCurrentUser(), notebook, now, zoneId);
  }
}
```

---

### `CommissionLearningSessionRequest.java` (DTO, request-response)

**Analog:** `AssimilationRequestDTO.java`

```java
public class AssimilationRequestDTO {
  public Integer noteId;
  public Boolean skipMemoryTracking;
  public String propertyKey;
  public Boolean assimilateAsCommissioned;
}
```

**Apply:** public field or `@Getter/@Setter` `Integer notebookId` only — server sets status/timestamps (mass-assignment guard per RESEARCH security table). Optional `@Schema(requiredMode = REQUIRED)` if using OpenAPI annotations like `DueCommissionedMemoryTrackerLite`.

---

### `LearningSessionCommissionResponse.java` (DTO, request-response)

**Analog:** `DueCommissionedMemoryTrackerLite.java`

```java
@Getter
@Setter
public class DueCommissionedMemoryTrackerLite {
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private int memoryTrackerId;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private int notebookId;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String notebookName;
}
```

**Apply:** `@Schema(REQUIRED)` on `learningSessionId` (int), `requestMarkdown` (String), `status` (`LearningSessionStatus` or String enum wire value).

---

### `LearningSessionControllerTests.java` (test, request-response)

**Analog:** `RecallsControllerTests.java` (Spanish notebook commissioned fixture) + `AssimilationControllerAssimilateTests.java` (auth + `ControllerTestBase`)

**Test base** (`ControllerTestBase.java` lines 17–26):
```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class ControllerTestBase {
  @Autowired protected MakeMe makeMe;
  @Autowired protected AuthorizationService authorizationService;
  @Autowired protected TestabilitySettings testabilitySettings;

  @TestBean protected CurrentUser currentUser;
```

**Spanish notebook + commissioned due fixture** (`RecallsControllerTests.java` lines 151–177):
```java
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
```

**Not logged in** (`AssimilationControllerAssimilateTests.java` lines 29–34):
```java
    void notLoggedIn() {
      currentUser.setUser(null);
      assertThrows(
          ResponseStatusException.class, () -> controller.assimilate(new AssimilationRequestDTO()));
    }
```

**Apply (small-test style):**
- `@BeforeEach`: `currentUser.setUser(makeMe.aUser().please())`
- Canonical commission test: notebook "Spanish conversation", notes Hola/Gracias with content Hello/Thank you, commissioned + due on day 2; drive `controller.commission(request, "Asia/Shanghai")`
- Assert delta: response `status == AWAITING_REPORT`; `requestMarkdown` contains `# Learning Session Request`, `### Hola`, `Expected learning content: Hello`, rubric `0 to 5`, `not yet tutored`
- Second commission same notebook: only one `AWAITING_REPORT` row (abandon lifecycle)
- Empty due: assert `ResponseStatusException` / 400 when no due commissioned trackers for notebook
- Do **not** re-assert full `RecallsController` exclusion shape — keep Phase 3 tests canonical for recall payload

**E2E fixture reference** (`.planning/phases/01-commissioned-tracker-model/commissioned_learning_session.feature` lines 7–14, 28–35) — Phase 4 does not graduate commission scenario.

---

### `LearningSessionBuilder.java` + `MakeMe` registration (fixture, transform)

**Analog:** `QuestionGenerationBatchBuilder.java` + `QuestionGenerationBatchRequestBuilder.java` + `MakeMe.java` lines 145–150

**Parent builder** (`QuestionGenerationBatchBuilder.java` lines 10–31, 61–72):
```java
public class QuestionGenerationBatchBuilder extends EntityBuilder<QuestionGenerationBatch> {

  public QuestionGenerationBatchBuilder forUser(User user) {
    entity.setUser(user);
    return this;
  }

  public QuestionGenerationBatchBuilder status(QuestionGenerationBatchStatus status) {
    entity.setStatus(status);
    return this;
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getUser() == null) {
      throw new IllegalStateException("call forUser() before please()");
    }
    if (entity.getStatus() == null) {
      entity.setStatus(QuestionGenerationBatchStatus.PLANNED);
    }
```

**Child builder** (`QuestionGenerationBatchRequestBuilder.java` lines 18–26, 38–50):
```java
  public QuestionGenerationBatchRequestBuilder batch(QuestionGenerationBatch batch) {
    entity.setBatch(batch);
    return this;
  }

  public QuestionGenerationBatchRequestBuilder memoryTracker(MemoryTracker memoryTracker) {
    entity.setMemoryTracker(memoryTracker);
    return this;
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getBatch() == null) {
      throw new IllegalStateException("call batch() before please()");
    }
```

**MakeMe registration** (`MakeMe.java` lines 145–150):
```java
  public QuestionGenerationBatchBuilder aQuestionGenerationBatch() {
    return new QuestionGenerationBatchBuilder(this);
  }

  public QuestionGenerationBatchRequestBuilder aQuestionGenerationBatchRequest() {
    return new QuestionGenerationBatchRequestBuilder(this);
  }
```

**Apply:** `LearningSessionBuilder` with `.forNotebook(notebook)`, `.by(user)`, `.status(AWAITING_REPORT)`, `.withSessionItems(tracker1, tracker2)` creating child `SessionItem` rows; register `makeMe.aLearningSession()` and optionally `makeMe.aSessionItem()` for Phase 6 prep.

---

### `LearningSessionRequestMarkdownBuilderTest.java` (optional test, transform)

**Analog:** `FocusContextMarkdownRendererTest.java` — no `@SpringBootTest`; instantiate builder directly; `containsString` assertions on ADR verbatim sections.

---

## Shared Patterns

### Authentication / notebook authorization
**Source:** `LearningSessionController` sketch + `AuthorizationService.java` lines 26–33, 142–145  
**Apply to:** Controller before service call
```java
authorizationService.assertLoggedIn();
authorizationService.assertAuthorization(notebook);
```

### Parent/child persistence with cascade abandon
**Source:** `V100000000__baseline.sql` FK `ON DELETE CASCADE`; `QuestionGenerationBatchRetentionService` delete loop  
**Apply to:** Migration + `LearningSessionService.abandonUnfinishedSessions` — hard delete prior awaiting-report sessions for same `user_id` + `notebook_id`

### Due commissioned selection (do not duplicate SQL)
**Source:** `UserService.getCommissionedMemoryTrackersNeedToRepeat`  
**Apply to:** `LearningSessionService.commission` — filter by notebook id + `isNoteLevelTracker()` only

### Fail visibly on empty commission
**Source:** `NotebookController.resolveDestinationNotebookForFolderMove` (`ResponseStatusException` NOT_FOUND); use `HttpStatus.BAD_REQUEST` for empty due set  
**Apply to:** Service or controller — no silent empty session

### Markdown verbatim ADR contract
**Source:** `docs/adrs/0005-commissioned-learning-session-protocol.md` lines 59–85  
**Apply to:** `LearningSessionRequestMarkdownBuilder` — snapshot test is canonical guard against rubric drift

### OpenAPI client sync
**Source:** repo `generate-api-client` skill  
**Apply to:** After adding controller + DTOs — `pnpm generateTypeScript`; never hand-edit `packages/generated/`

### Structure phase — no user-visible change
**Source:** `planning.mdc` / RESEARCH user constraints  
**Apply to:** No frontend changes; run existing `e2e_test/features/learning_session/commissioned_learning_session.feature` for regression only; do not remove `@wip` from commission scenario

### Controller-boundary small tests
**Source:** `unit-testing.mdc`, `RecallsControllerTests`, `ControllerTestBase`  
**Apply to:** `LearningSessionControllerTests` — real DB `@Transactional`, `makeMe` fixtures, assert markdown delta not full recall DTO

### ERD refresh after migration
**Source:** `database-erd` skill — `pnpm export:database-erd` after `V300000240` lands

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | All Phase 4 files map to parent/child batch, markdown renderer, or recall/assimilation controller patterns |

**Explicit non-goals (do not copy as targets):**

| Obsolete / wrong analog | Why not |
|-------------------------|---------|
| Phase 3 FE `useRecallData` / potential sessions | Unchanged this phase |
| `QuestionGenerationBatchSubmissionService` OpenAI path | External API; only delete/save shape relevant |
| Hand-editing generated TypeScript client | Regenerate only |
| E2E commission scenario graduation | Phase 5 Behavior |

## Metadata

**Analog search scope:** `backend/.../entities` (`QuestionGenerationBatch*`), `entities/repositories`, `services` (`FocusContextMarkdownRenderer`, `UserService`, `QuestionGenerationBatchRetentionService`), `controllers` (`AssimilationController`, `RecallsController`, `NotebookController`), `controllers/dto`, `algorithms/NoteContentMarkdown`, `db/migration`, `testability/builders`, `test/.../controllers`, `test/.../services/focusContext`, `.planning/phases/01-.../commissioned_learning_session.feature`, `docs/adrs/0005-*.md`  
**Files scanned:** ~40 (targeted)  
**Pattern extraction date:** 2026-08-08
