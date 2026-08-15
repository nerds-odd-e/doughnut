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

- **Status:** planned
- **Type:** Behavior
- **Pre:** A due memory tracker.
- **Trigger:** Client asks for the next ask payload (`GET .../question` still).
- **Post:** OpenAPI/SDK type is **`RecallPrompt`**, not `RecallQuestion`. JSON
  shape and path are unchanged (stem+choices or spelling stem; no solution).

Delete `RecallQuestion` / `.from`. Regen client; fix compile. Leave
`askAQuestion` and `/question`.

**Tests:** `MemoryTrackerAskQuestionControllerTest`; frontend/CLI compile
against the new type.

### 3. Ask path is recall-prompt

- **Status:** planned
- **Type:** Behavior
- **Pre:** Slice 2 done.
- **Trigger:** Learner starts recall (web or CLI).
- **Post:** `GET /api/memory-trackers/{id}/recall-prompt`. No `.../question`
  and no `askAQuestion`.

**Tests:** same ask tests + one MCQ recall E2E + one spelling recall E2E;
CLI interactive ask mocks.

### 4. CLI recall help is not quiz

- **Status:** planned
- **Type:** Behavior
- **Pre:** `/recall` help is shown.
- **Trigger:** User reads CLI command docs / help.
- **Post:** Copy does not say **quiz**; it matches just review / recall prompt.

**Tests:** existing CLI help or `/recall` doc assertion.

### 5. Answer operation is answer

- **Status:** planned
- **Type:** Behavior
- **Pre:** An unanswered recall prompt is on screen.
- **Trigger:** Learner submits an MCQ choice or spelling.
- **Post:** SDK operation is **`answer`**, not `answerQuiz`. Path stays
  `POST /api/recall-prompts/{id}/answer`.

Rename `submitQuizAnswer` with the SDK. Regen client.

**Tests:** `RecallPromptAnswerQuizControllerTest` (rename with the operation);
`RecallPromptComponent` spec; existing answer E2E.

### 6. Answered field is mcq

- **Status:** planned
- **Type:** Behavior
- **Pre:** Learner has answered an MCQ recall prompt.
- **Trigger:** Result or history is shown.
- **Post:** JSON field is **`mcq`**, not `predefinedQuestion`. Java/OpenAPI
  type may still be `PredefinedQuestion`.

**Tests:** answered-question component spec; memory-tracker history tests;
regen client.

### 7. Tests say MCQ on the note

- **Status:** planned
- **Type:** Structure
- **Enables:** 8. Type is Mcq

E2E/testability wording: “predefined questions in the notebook” → MCQs;
contest feature says contest an **MCQ**. No product change.

**Verify:** `predefined_questions_management.feature` and
`question_contest.feature` still pass.

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
