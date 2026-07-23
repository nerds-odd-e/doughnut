# Phase 1: Extend Answer outcome API - Pattern Map

**Mapped:** 2026-07-23
**Files analyzed:** 13 (new / modified / regenerated / reuse / no-edit-verify)
**Analogs found:** 13 / 13

## Summary

Phase 1 is a pure **Structure** phase: extend the backend->frontend answer **contract** (OpenAPI types/schema) so it can represent a third outcome (accidental-match with a matched-note id) and an overlap flag, then regenerate the TypeScript client. No backend behavior is wired; new states must be representable but not returned.

Locked shape (Option A-full, substituting for absent CONTEXT.md):
- Add `@Transient` nullable `matchedNoteId` + `AnswerOutcome outcome` to the `Answer` entity.
- Add optional `overlap: Boolean` + `matchedNotes: List<NoteTopology>` to the `AnsweredQuestion` DTO.
- Reuse the existing `NoteTopology` DTO (id + title) for `matchedNotes` -- do NOT create a new note-ref type.
- Keep `correct: boolean` required and untouched. No Flyway migration. No new endpoint/service. No UI.

The contract already has the right primitives. Phase 1 is additive fields + regen.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `backend/.../entities/Answer.java` | entity (+contract type) | response-only (serialized; `@Transient` non-persisted) | `entities/NoteEmbedding.java` (`@Transient` field style) + itself | exact (self) + role-match |
| `backend/.../entities/AnswerOutcome.java` (NEW) | enum (contract type) | response-only | `entities/QuestionType.java` | exact |
| `backend/.../controllers/dto/AnsweredQuestion.java` | DTO (response) | response-only | `controllers/dto/RecalledNote.java` + itself | exact (self) |
| `backend/.../controllers/dto/RecallPromptHistoryItem.java` | DTO (response, 2nd surface) | response-only | itself (embeds `Answer`) | exact (self) -- NO edit under Option A |
| `backend/.../controllers/dto/NoteTopology.java` (REUSE) | DTO (response) | response-only | itself; built by `Note.getNoteTopology()` | exact (self) -- NO new file |
| `packages/generated/doughnut-backend-api/types.gen.ts` | generated (TS contract) | response | current `types.gen.ts:282-297` | exact (self, regenerated) |
| `packages/generated/doughnut-backend-api/sdk.gen.ts` | generated (TS SDK) | response | itself | exact (self, regenerated) |
| `open_api_docs.yaml` | generated (OpenAPI schema) | response | current `open_api_docs.yaml:3633-3677` | exact (self, regenerated) |
| `frontend/src/components/recall/AnsweredSpellingQuestion.vue` | frontend-consumer (Vue) | response (reads `answer.correct`) | itself | exact (self) -- NO edit |
| `frontend/src/components/recall/AnsweredQuestionComponent.vue` | frontend-consumer (Vue) | response (passes `answer`) | itself | exact (self) -- NO edit |
| `packages/doughnut-test-fixtures/src/AnsweredQuestionBuilder.ts` | frontend-consumer (fixture) | response (builds `Answer as Answer`) | itself | exact (self) -- NO edit |
| `frontend/tests/components/recall/QuestionDisplay.spec.ts` | test (frontend) | response (`satisfies Answer`) | itself | exact (self) -- NO edit |
| `backend/.../controllers/RecallPromptControllerTests.java` | test (backend controller) | response (asserts `getCorrect()`) | itself (`:519`, `:531-532`, `:670-671`) | exact (self, extend) |
| `backend/.../controllers/RecallsControllerTests.java` | test (backend controller) | response (`previouslyAnswered`) | itself (`:166`, `:189`, `:240`) | exact (self, extend) |

## Pattern Assignments

### `backend/.../entities/Answer.java` (entity + contract type, response-only)

**Analog:** itself (entity pattern) + `entities/NoteEmbedding.java:19` for the `@Transient` field style.

Current entity shape (`entities/Answer.java:12-35`):

```24:35
@Getter
@Entity
@Table(name = "quiz_answer")
public class Answer extends EntityIdentifiedByIdOnly {
  @Column(name = "choice_index")
  Integer choiceIndex;
  @Column(name = "created_at")
  @Setter
  @JsonIgnore
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());
  @Column(name = "correct")
  @Setter
  @NotNull
  private Boolean correct;
  @Column(name = "thinking_time_ms")
  @Setter
  private Integer thinkingTimeMs;
  @Column(name = "spelling_answer")
  @Setter
  private String spellingAnswer;
```

`@Transient` field style analog (`entities/NoteEmbedding.java:19`):

```19:19
  @Transient @Getter @Setter private byte[] embedding;
```

**Pattern to replicate:** Add two non-persisted nullable fields. The class-level `@Getter` (line 12) already generates accessors; mirror the `NoteEmbedding` style with field-local `@Getter @Setter` (or rely on the class `@Getter` + add `@Setter`). Keep `correct` `@NotNull` and untouched.

```java
@Transient @Getter @Setter private Long matchedNoteId;
@Transient @Getter @Setter private AnswerOutcome outcome;
```

`import jakarta.persistence.*;` is already present (`Answer.java:6`), so `@Transient` resolves with no new import. `AnswerOutcome` lives in the same `entities` package, so no import is needed.

**Highest-risk assumption (A1):** Jackson does NOT honor `jakarta.persistence.@Transient` (it honors `com.fasterxml.jackson.annotation.@JsonIgnore`), so the fields ARE serialized -> should surface in OpenAPI as optional. `NoteEmbedding.embedding` is `@Transient` and not `@JsonIgnore`, confirming the annotation style -- but `NoteEmbedding` is not a controller return type, so it does NOT prove springdoc surfacing. **Mitigation:** after regen, grep `types.gen.ts` for `matchedNoteId` / `outcome`; if absent, fall back to Option B (explicit DTO).

---

### `backend/.../entities/AnswerOutcome.java` (NEW, enum, response-only)

**Analog:** `entities/QuestionType.java` (simple enum, same `entities` package).

```1:6
package com.odde.doughnut.entities;

public enum QuestionType {
  MCQ,
  SPELLING
}
```

Secondary analog: `controllers/dto/HealthSeverity.java` (simple enum, lowercase values).

**Pattern to replicate:** A plain enum in the `entities` package. Package declaration + enum body, no constructor, no fields.

```java
package com.odde.doughnut.entities;

public enum AnswerOutcome {
  CORRECT,
  WRONG,
  ACCIDENTAL_MATCH,
  OVERLAP
}
```

Naming note (A3): values are cosmetic pre-wire; Phase 2/6 may rename (e.g. `ACCIDENTAL`) cheaply. Place in `entities/` (not `controllers/dto/`) because `Answer` (entity) references it; keeps the entity self-contained.

---

### `backend/.../controllers/dto/AnsweredQuestion.java` (DTO, response-only)

**Analog:** itself + `controllers/dto/RecalledNote.java` (DTO with `@Schema(requiredMode=...)` required fields, optional un-annotated fields, a `List<...>` field, and a static `from()` factory).

Current DTO (`controllers/dto/AnsweredQuestion.java:12-30`):

```12:30
@Data
@NoArgsConstructor
public class AnsweredQuestion {
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) private int id;
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) private QuestionType questionType;
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) private int memoryTrackerId;
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) private RecalledNote recalledNote;
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) private Answer answer;
  private PredefinedQuestion predefinedQuestion;
```

`RecalledNote` analog showing optional `List<...>` field (`controllers/dto/RecalledNote.java:10-21`):

```10:21
@Data
@NoArgsConstructor
public class RecalledNote {
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private NoteTopology noteTopology;
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private int notebookId;
  private List<Folder> ancestorFolders;
  private String propertyKey;
```

**Pattern to replicate:** Add two optional fields with NO `@Schema(requiredMode=...)` annotation -- absence of the annotation makes them optional in OpenAPI (see `predefinedQuestion` line 30 and `RecalledNote.ancestorFolders` line 19). `@Data` (line 12) generates getters/setters. `NoteTopology` is in the same `controllers/dto` package -> no import; add `import java.util.List;`.

```java
private Boolean overlap;
private List<NoteTopology> matchedNotes;
```

**Critical:** `AnsweredQuestion.from(RecallPrompt)` (`:32-45`) stays UNCHANGED -- do not set the new fields. Left null, they are omitted from JSON via `NON_NULL` -> existing frontend stays green. `RecallPromptHistoryItem.from()` likewise must not set them.

---

### `backend/.../controllers/dto/RecallPromptHistoryItem.java` (DTO, 2nd contract surface, response-only)

**Analog:** itself. It embeds `private Answer answer;` (`:29`), so under Option A the new `Answer` fields propagate automatically.

```29:29
  private Answer answer;
```

**Pattern to replicate:** NO source edit required under Option A. The shared `Answer` entity carries `matchedNoteId`/`outcome` into this DTO's schema for free. Only VERIFY after regen that the history path type-checks and the new fields are optional/absent on the existing path (CLI/frontend consumers tolerate them via `NON_NULL`).

If the planner falls back to Option B (separate DTO), then BOTH `AnsweredQuestion` and `RecallPromptHistoryItem` must swap `answer: Answer` -> `answer: AnswerView` and gain a mapper -- broader blast radius (Pitfall 5).

---

### `backend/.../controllers/dto/NoteTopology.java` (REUSE, no new file)

**Analog:** itself. Already the contract's note-pointer shape (`id` + `title`), built by `Note.getNoteTopology()`.

`NoteTopology` (`controllers/dto/NoteTopology.java:9-19`):

```9:19
@NoArgsConstructor
@Data
public class NoteTopology {
  @NonNull private Integer id;
  @NotBlank private String title;
  private Timestamp createdAt;
  private Timestamp updatedAt;
```

Producer (`entities/Note.java:142-151`):

```142:151
  @NonNull
  public NoteTopology getNoteTopology() {
    NoteTopology noteTopology = new NoteTopology();
    noteTopology.setId(getId());
    noteTopology.setTitle(getTitle() != null ? getTitle() : "");
    Objects.requireNonNull(getNotebook());
    noteTopology.setCreatedAt(getCreatedAt());
    noteTopology.setUpdatedAt(getUpdatedAt());
    return noteTopology;
  }
```

**Pattern to replicate:** Do NOT create a new note-ref type. `matchedNotes: List<NoteTopology>` reuses this. Phase 3 (reveal by title) and Phase 4 (`LinkInsertionChoice` pre-select by id) both consume `NoteTopology`. If Phase 4 needs `notebookId`/ancestor context, enrich to a `MatchedNote` DTO THEN -- not in Phase 1 (A4).

---

### `packages/generated/doughnut-backend-api/types.gen.ts` + `sdk.gen.ts` (REGENERATE, never hand-edit)

**Analog:** current generated contract.

Current `Answer` / `AnsweredQuestion` TS (`types.gen.ts:282-297`):

```282:297
export type Answer = {
    id: number;
    choiceIndex?: number;
    correct: boolean;
    thinkingTimeMs?: number;
    spellingAnswer?: string;
};

export type AnsweredQuestion = {
    id: number;
    questionType: 'MCQ' | 'SPELLING';
    memoryTrackerId: number;
    recalledNote: RecalledNote;
    answer: Answer;
    predefinedQuestion?: PredefinedQuestion;
};
```

`NoteTopology` TS (`types.gen.ts:233-238`):

```233:238
export type NoteTopology = {
    id: number;
    title: string;
    createdAt?: string;
    updatedAt?: string;
};
```

**Pattern to replicate:** Do NOT hand-edit. After editing Java, regenerate:

```bash
CURSOR_DEV=true nix develop -c pnpm generateTypeScript
```

Expected post-regen shape (additive optional fields only):

```typescript
export type Answer = {
  id: number; choiceIndex?: number; correct: boolean;
  thinkingTimeMs?: number; spellingAnswer?: string;
  matchedNoteId?: number; outcome?: 'CORRECT' | 'WRONG' | 'ACCIDENTAL_MATCH' | 'OVERLAP';
};
export type AnsweredQuestion = {
  id: number; questionType: 'MCQ' | 'SPELLING'; memoryTrackerId: number;
  recalledNote: RecalledNote; answer: Answer; predefinedQuestion?: PredefinedQuestion;
  overlap?: boolean; matchedNotes?: Array<NoteTopology>;
};
```

**Verification (Wave 0 gap):** grep `types.gen.ts` for `matchedNoteId`, `outcome`, `matchedNotes`, `overlap` -- if any is absent, A1 failed -> fall back to Option B. Whitespace hygiene via `scripts/check_diff_whitespace.sh`, NOT raw `git diff --check`.

---

### `open_api_docs.yaml` (REGENERATE, never hand-edit)

**Analog:** current schema (`open_api_docs.yaml:3633-3677`).

```3633:3677
    Answer:
      type: object
      properties:
        id: { type: integer, format: int32 }
        choiceIndex: { type: integer, format: int32 }
        correct: { type: boolean }
        thinkingTimeMs: { type: integer, format: int32 }
        spellingAnswer: { type: string }
      required: [correct, id]
    AnsweredQuestion:
      type: object
      properties:
        id: { type: integer, format: int32 }
        questionType: { type: string, enum: [MCQ, SPELLING] }
        memoryTrackerId: { type: integer, format: int32 }
        recalledNote: { $ref: "#/components/schemas/RecalledNote" }
        answer: { $ref: "#/components/schemas/Answer" }
        predefinedQuestion: { $ref: "#/components/schemas/PredefinedQuestion" }
      required: [answer, id, memoryTrackerId, questionType, recalledNote]
```

**Pattern to replicate:** Regenerated by the same `pnpm generateTypeScript` (runs Gradle `generateOpenAPIDocs` -> `open_api_docs.yaml` -> `@hey-api/openapi-ts`). Never hand-edit; if `pnpm openapi:lint` fails, fix Java controllers then regenerate. After regen, `Answer` gains optional `matchedNoteId`/`outcome` and stays `required: [correct, id]`; `AnsweredQuestion` gains optional `overlap`/`matchedNotes` and keeps its existing `required` list.

---

### `frontend/src/components/recall/AnsweredSpellingQuestion.vue` (frontend-consumer, NO edit)

**Analog:** itself. Reads `answeredQuestion.answer.correct` (required boolean) and `answeredQuestion.answer.spellingAnswer` (optional).

```2:4
  <div class="daisy-alert" :class="{ 'daisy-alert-success': answeredQuestion.answer.correct, 'daisy-alert-error': !answeredQuestion.answer.correct }">
    <strong>
      {{ answeredQuestion.answer.correct ? 'Correct!' : `Your answer \`${answeredQuestion.answer.spellingAnswer}\` is incorrect.` }}
```

Type import (`AnsweredSpellingQuestion.vue:21`):

```21:21
import type { AnsweredQuestion } from "@generated/doughnut-backend-api"
```

**Pattern to replicate:** NO source edit. Because `correct` stays required and `matchedNoteId`/`outcome`/`overlap`/`matchedNotes` are optional, this component keeps type-checking unchanged. Only RUN `pnpm frontend:test` to confirm green (Pitfall 1).

---

### `frontend/src/components/recall/AnsweredQuestionComponent.vue` (frontend-consumer, NO edit)

**Analog:** itself. Passes `answeredQuestion.answer` to `QuestionDisplay` (`:15`).

```10:19
  <QuestionDisplay
    v-if="answeredQuestion.predefinedQuestion"
    v-bind="{
      multipleChoicesQuestion: answeredQuestion.predefinedQuestion.multipleChoicesQuestion,
      correctChoiceIndex: answeredQuestion.predefinedQuestion.correctAnswerIndex,
      answer: answeredQuestion.answer,
      testedFocus: answeredQuestion.predefinedQuestion.testedFocus,
      validationRationale: answeredQuestion.predefinedQuestion.validationRationale,
    }"
  />
```

**Pattern to replicate:** NO source edit. Same reasoning -- new optional fields do not affect existing required-field reads. Confirm via `pnpm frontend:test`.

---

### `packages/doughnut-test-fixtures/src/AnsweredQuestionBuilder.ts` (fixture, NO edit)

**Analog:** itself. Builds an `Answer`-shaped literal cast `as Answer`.

```82:90
    const answer =
      this.answerToUse ??
      ({
        id: generateId(),
        correct: this.isCorrect,
        ...(this.choiceIndexToUse !== undefined && {
          choiceIndex: this.choiceIndexToUse,
        }),
      } as Answer)
```

**Pattern to replicate:** NO source edit. The literal sets only `id` + `correct` (+ optional `choiceIndex`); the new `Answer` fields are optional, so this still satisfies `Answer`. The `as Answer` cast (not `satisfies`) keeps it permissive. Confirm `pnpm frontend:test` stays green.

---

### `frontend/tests/components/recall/QuestionDisplay.spec.ts` (frontend test, NO edit)

**Analog:** itself. Direct `Answer` type import + `satisfies Answer` literal.

```8:8
import type { Answer } from "@generated/doughnut-backend-api"
```

```254:258
    const answer = {
      id: 1,
      correct: true,
      choiceIndex: 0,
    } satisfies Answer
```

**Pattern to replicate:** NO source edit. `satisfies Answer` checks the literal is assignable to `Answer`; new optional fields do not break assignability (only `id` + `correct` are required). Confirm via `pnpm frontend:test tests/components/recall/QuestionDisplay.spec.ts`.

---

### `backend/.../controllers/RecallPromptControllerTests.java` (backend test, EXTEND)

**Analog:** itself. Existing spelling-answer tests assert `getAnswer().getCorrect()` -- they read individual fields, not whole-object equality, so adding nullable fields is safe.

Test scaffolding (`RecallPromptControllerTests.java:34-42`):

```34:42
class RecallPromptControllerTests extends ControllerTestBase {
  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;
  @Autowired MakeMe makeMe;
  @Autowired RecallPromptController controller;
  @Autowired GlobalSettingsService globalSettingsService;
```

Base class annotations (`ControllerTestBase.java:14-17`):

```14:17
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class ControllerTestBase {
```

Existing happy-path assertion style (`:531-532`):

```531:532
      AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);
      assertTrue(answerResult.getAnswer().getCorrect());
```

Existing wrong-path assertion (`:670-671`):

```670:671
        AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);
        assertFalse(answerResult.getAnswer().getCorrect());
```

**Pattern to replicate:** Add a no-behavior test asserting the new fields are null/absent on the existing spelling path (proves "representable but not returned"). Mirror the `@Nested` + `@Test` + `assertThat`/`assertNull` style. Do NOT assert `outcome == ACCIDENTAL_MATCH` -- no code path returns it yet.

```java
@Test
void shouldNotPopulateAccidentalMatchFieldsOnNormalSpellingAnswer()
    throws UnexpectedNoAccessRightException {
  AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);
  assertNull(answerResult.getAnswer().getMatchedNoteId());
  assertNull(answerResult.getAnswer().getOutcome());
  assertNull(answerResult.getOverlap());
  assertNull(answerResult.getMatchedNotes());
}
```

Place inside the existing spelling `@Nested` class that already sets up `recallPrompt` + `answerDTO` (the group containing `:531`/`:670`). Run via `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (no migration -> `test_only` suffices).

---

### `backend/.../controllers/RecallsControllerTests.java` (backend test, EXTEND)

**Analog:** itself. `previouslyAnswered` returns `List<AnsweredQuestion>`; existing tests assert `hasSize` + `getQuestionType`.

Scaffolding (`RecallsControllerTests.java:24-31`):

```24:31
class RecallsControllerTests extends ControllerTestBase {
  @Autowired RecallsController controller;
  @Autowired NoteService noteService;
  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }
```

Existing assertion (`:189-192`):

```189:192
      List<AnsweredQuestion> results = controller.previouslyAnswered("Asia/Shanghai");
      assertThat(results, hasSize(1));
      assertEquals(com.odde.doughnut.entities.QuestionType.MCQ, results.get(0).getQuestionType());
```

**Pattern to replicate:** Add a no-behavior assertion that the second contract surface (`RecallPromptHistoryItem`-backed history) leaves the new fields null/absent. Extend an existing `previouslyAnswered` test (e.g. the `:189` MCQ case or the `:240` spelling case) with:

```java
assertNull(results.get(0).getOverlap());
assertNull(results.get(0).getMatchedNotes());
assertNull(results.get(0).getAnswer().getMatchedNoteId());
assertNull(results.get(0).getAnswer().getOutcome());
```

This pins the "no behavior" guarantee on BOTH contract surfaces (Pitfall 5). Run via `pnpm backend:test_only`.

---

## Shared Patterns

### Jackson `NON_NULL` serialization (the core safety mechanism)
**Source:** `configs/ObjectMapperConfig.java:25`
**Apply to:** every new optional field on `Answer` / `AnsweredQuestion`.

```25:25
        .serializationInclusion(JsonInclude.Include.NON_NULL)
```

Nullable fields no code path sets are OMITTED from JSON -> optional in generated TS -> existing frontend stays green. This is what makes a purely additive, zero-behavior contract extension safe. Do NOT add `@JsonIgnore` to the new `@Transient` fields (that would hide them from the contract and break SC#1).

### OpenAPI required vs optional (springdoc)
**Source:** `controllers/dto/AnsweredQuestion.java:15-30` + `controllers/dto/RecalledNote.java:13-21`
**Apply to:** `AnsweredQuestion.overlap`, `AnsweredQuestion.matchedNotes`, `Answer.matchedNoteId`, `Answer.outcome`.

- `@Schema(requiredMode = Schema.RequiredMode.REQUIRED)` -> required in OpenAPI (used for `correct`, `id`, etc.).
- NO `@Schema` annotation -> optional (see `AnsweredQuestion.predefinedQuestion` line 30, `RecalledNote.ancestorFolders` line 19).

The new fields MUST be optional (no `@Schema(requiredMode=...)`). Keep `correct`'s existing `@NotNull` + required schema untouched (Pitfall 1).

### Lombok entity/DTO conventions
**Source:** `entities/Answer.java:12` (`@Getter`), `controllers/dto/AnsweredQuestion.java:12` (`@Data @NoArgsConstructor`), `entities/NoteEmbedding.java:19` (`@Transient @Getter @Setter`)
**Apply to:** `Answer.java` (entity, class-level `@Getter` already present), `AnsweredQuestion.java` (DTO, `@Data` generates getters/setters), `AnswerOutcome.java` (enum, no Lombok needed).

### Generated-artifact rule (never hand-edit)
**Source:** `.cursor/agent-map.md`, `.cursor/skills/generate-api-client/SKILL.md`, `.cursor/rules/linting_formating.mdc`
**Apply to:** `types.gen.ts`, `sdk.gen.ts`, `open_api_docs.yaml`.

Edit Java only -> `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` -> verify with `pnpm openapi:lint`. Use `scripts/check_diff_whitespace.sh` (not raw `git diff --check`) so generated artifacts are not manually "fixed".

### Backend test scaffolding
**Source:** `ControllerTestBase.java:14-17` (`@SpringBootTest @ActiveProfiles("test") @Transactional`), `makeMe` builders, `@Nested` groups, `assertThat`/`assertTrue`/`assertNull`.
**Apply to:** the no-behavior assertions added to `RecallPromptControllerTests` and `RecallsControllerTests`. Prefer controller-level behavior tests; one behavior per `@Test`; descriptive method names (`should...`).

## No Analog Found

No file in this phase lacks a close analog -- every change is an additive extension of an existing entity/DTO/enum/test, or a regeneration of an existing generated artifact. The only novel element is the `@Transient`-surfaces-in-OpenAPI assumption (A1), which is a behavior to VERIFY via regen+grep, not a pattern to copy from (no existing `@Transient` field is a controller return type today; `NoteEmbedding.embedding` is the annotation-style analog but not a serialization proof).

## Metadata

**Analog search scope:** `backend/src/main/java/com/odde/doughnut/{entities,controllers,controllers/dto,services,configs}`, `backend/src/test/java/com/odde/doughnut/controllers`, `packages/generated/doughnut-backend-api`, `packages/doughnut-test-fixtures/src`, `frontend/src/components/recall`, `frontend/tests/components/recall`, `open_api_docs.yaml`.
**Files scanned:** 13 target files + 6 analog files read in full/targeted (`Answer`, `AnsweredQuestion`, `RecallPromptHistoryItem`, `NoteTopology`, `RecalledNote`, `QuestionType`, `HealthSeverity`, `NoteEmbedding`, `ObjectMapperConfig`, `RecallPromptController`, `MemoryTrackerService`, `Note`, `AnsweredSpellingQuestion.vue`, `AnsweredQuestionComponent.vue`, `AnsweredQuestionBuilder.ts`, `QuestionDisplay.spec.ts`, `RecallPromptControllerTests`, `RecallsControllerTests`, `ControllerTestBase`).
**Pattern extraction date:** 2026-07-23.




