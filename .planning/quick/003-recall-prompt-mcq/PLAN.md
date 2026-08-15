# Plan: Recall prompt / MCQ — minimum translation

**Goal:** Same nouns as ADR 0001 (**recall prompt** HAS_A **MCQ**) in tests, API,
code, and schema, with no `RecallQuestion` / `PredefinedQuestion` / `quiz`
translation types. One slice = one small commit. Stop-safe.

**Prerequisite (done):** ADR 0001 / 0003 glossary.

**Cite:** [CONTEXT.md](./CONTEXT.md)

## Decisions

1. Two nouns only. An MCQ is not a subtype of a recall prompt.
2. Minimum DTO ≠ zero DTO. The unanswered prompt must not leak the solution;
   that projection must not be named **RecallQuestion**.
3. Contest stays on the recall-prompt URL; `contested` stays on the MCQ.
4. Do not feed note-authored MCQs into recall in this plan.
5. After OpenAPI/controller changes, regenerate the client
   (`pnpm generateTypeScript`); never hand-edit generated files. Type/path
   slices that regen the SDK may exceed ~5 min; that regen is the stated
   reason to continue (do not bundle a second outcome into the same commit).
6. New Flyway versions only; never edit committed migrations.
7. Capability-named tests only. Slice numbers stay in this file.
8. One slice, one commit. If a slice overruns ~10 min for any reason other
   than SDK regen or a single Flyway apply, stop, revert, split.
9. Schema table names are operator-observable (ERD + migrated catalog). They
   are Behavior in this plan, not Structure-for-the-next-pixel-change.
10. Do not rename `Quiz.vue` or the note **Questions** menu in this plan
    (leftover identifiers; no user-facing behavior tied to the file name).

## Slices

### 1. Tests say recall prompt

- **Status:** done
- **Type:** Structure
- **Enables:** 2. Ask type is RecallPrompt

Shipped: `When I visit recall for a due recall prompt on day {int}` (step +
call sites). Contest / note-question wording untouched. Feature filenames
and page-object helpers still say quiz/question (later leftover).

### 2. Ask type is RecallPrompt

- **Status:** done
- **Type:** Behavior

Shipped: unanswered-ask OpenAPI/SDK type is **`RecallPrompt`** (DTO
`controllers.dto.RecallPrompt`; entity unchanged). JSON shape and
`GET .../question` / `askAQuestion` unchanged. `makeMe.aRecallPrompt` builds
the ask payload; history is `makeMe.aRecallPromptHistoryItem`.

### 3. Ask path is recall-prompt

- **Status:** done
- **Type:** Behavior

Shipped: `GET /api/memory-trackers/{id}/recall-prompt`, SDK `getRecallPrompt`.
No `askAQuestion` / `GET .../question`. Ask controller test is
`MemoryTrackerRecallPromptControllerTest`.

### 4. CLI recall help is not quiz

- **Status:** done
- **Type:** Behavior

Shipped: `/recall` help is `Recall the next due note (just review when no
recall prompt is pending)`.

### 5. Answer operation is answer

- **Status:** done
- **Type:** Behavior

Shipped: SDK operation **`answer`** (not `answerQuiz`); path still
`POST /api/recall-prompts/{id}/answer`. Frontend `submitAnswer`; test
`RecallPromptAnswerControllerTest`; service method `answer`.

### 6. Answered field is mcq

- **Status:** done
- **Type:** Behavior

Shipped: answered result and history JSON/OpenAPI property is **`mcq`**.
Type remains `PredefinedQuestion`. Builders use `withMcq`.

### 7. Tests say MCQ on the note

- **Status:** done
- **Type:** Structure
- **Enables:** 8. Type is Mcq

Shipped: Gherkin/testability says MCQs / contest an **MCQ**. Feature
filenames and `injectPredefinedQuestionsToNotebook` left for type rename.

### 8. Type is Mcq

- **Status:** planned
- **Type:** Behavior
- **Pre:** Slice 6–7 done.
- **Trigger:** Client reads or writes note MCQs / answered `mcq`.
- **Post:** Java/OpenAPI type is **`Mcq`**, not `PredefinedQuestion`. HTTP
  paths still `/api/predefined-questions/...`. `@Table` may still be
  `predefined_question`.

Regen client; rename builders (`aPredefinedQuestion` → `anMcq` or equivalent).

**Tests:** `PredefinedQuestionControllerTests` (rename with the type);
note MCQ E2E; regen compile.

### 9. Note MCQ routes are /mcqs

- **Status:** planned
- **Type:** Behavior
- **Pre:** Type is already `Mcq`.
- **Trigger:** List / add / generate / refine on a note.
- **Post:** HTTP is `/api/mcqs/...`. No `/api/predefined-questions`.

**Tests:** controller tests for those routes; note MCQ E2E; regen client.

### 10. Table is mcq

- **Status:** planned
- **Type:** Behavior
- **Pre:** API type is `Mcq`.
- **Trigger:** Flyway migrate.
- **Post:** Table `mcq`, FK `recall_prompt.mcq_id`; ERD matches; create/load
  MCQ and recall still work. No JSON change.

**Tests:** backend tests that persist MCQs / recall prompts;
`pnpm export:database-erd`.

### 11. Table is answer

- **Status:** planned
- **Type:** Behavior
- **Pre:** Slice 10 done (or at least app on current schema).
- **Trigger:** Flyway migrate.
- **Post:** Table `answer`, FK `recall_prompt.answer_id`; ERD matches;
  answering still works. Entity was already `Answer`.

**Tests:** backend answer / recall-prompt tests; ERD export.

### 12. Unanswered prompt has no MultipleChoicesQuestion type

- **Status:** planned
- **Type:** Behavior
- **Pre:** Learner is shown an unanswered MCQ prompt.
- **Trigger:** Client fetches the recall prompt.
- **Post:** OpenAPI has no **`MultipleChoicesQuestion`**. Stem+choices live
  on the prompt or on `Mcq` without a second type name. UI pixels unchanged.

**Tests:** MCQ recall E2E; regen client; OpenAPI/types have no that name.

### 13. AI generate uses Mcq

- **Status:** planned
- **Type:** Behavior
- **Pre:** Trainer or recall generation asks the model for an MCQ.
- **Trigger:** Generate / refine / contest-regenerate.
- **Post:** API/Java have no **`MCQWithAnswer`** (or refine subclass) as a
  product type. `choicesMayBeShuffled` is a field on `Mcq` or AI-only — not
  a glossary noun.

**Tests:** generate/refine/contest/regenerate controller tests;
`isMCQWithAnswerValid` follows the type.

## Out of scope

- Using the note’s MCQ list as the recall source
- Inferring `QuestionType` from `mcq_id` and dropping the enum
- Renaming **just review**, `Quiz.vue`, or the note **Questions** menu
- Dual JSON aliases for old field names

## Jidoka

- Confirm no external API consumers that need dual JSON field names.
- Slices 10–11 need a deploy that applies the new Flyway version.
- Do not add compatibility DTOs that reintroduce translation.

## Learnings

- Slice 1: leftover `quiz` in feature filenames and `visitRecallPageAndWaitForQuestion` stay until a later wording slice; not this step.
- Slice 2: `makeMe.aRecallPrompt` is the unanswered-ask DTO; history items are `makeMe.aRecallPromptHistoryItem`. `RecallQuestionService` and CLI `RecallQuestionAnswerOutcome` wait for later slices.
- Slice 3: SDK operation is `getRecallPrompt` (history list remains `getRecallPrompts`). Fetch extracted to `useRecallPromptFetching` / `recallMcqCardLoad`.
- Slice 5: CLI `RecallQuestionAnswerOutcome` still deferred; JSON `predefinedQuestion` is slice 6.
- Slice 6: answered/history property is `mcq`; Java type still `PredefinedQuestion`.
- Slice 7: leftover `injectPredefinedQuestionsToNotebook` / feature filenames wait for type `Mcq`.
