# Plan: Recall prompt / MCQ — minimum translation

**Goal:** Learners, APIs, code, and schema use **recall prompt** and **MCQ** as
in ADR 0001, with no `RecallQuestion` / `PredefinedQuestion` / `quiz` translation
types. Stop-safe; one Behavior or Structure per slice.

**Prerequisite (done):** ADR 0001 / 0003 glossary (recall prompt HAS_A MCQ; just
review is a recall method).

**Cite:** [CONTEXT.md](./CONTEXT.md)

## Decisions

1. Two nouns only. An MCQ is not a subtype of a recall prompt.
2. Minimum DTO ≠ zero DTO. The unanswered prompt must not leak the solution;
   that projection must not be named **RecallQuestion**.
3. Contest stays on the recall-prompt URL; `contested` stays on the MCQ.
4. Do not feed note-authored MCQs into recall in this plan.
5. After OpenAPI/controller changes, regenerate the client
   (`pnpm generateTypeScript`); never hand-edit generated files.
6. New Flyway versions only; never edit committed migrations.
7. Capability-named tests only (existing recall / note-question features).
8. If a slice overruns ~10 min, stop, revert that attempt, split that slice.

## Slices

### 1. Tests say recall prompt

- **Status:** planned
- **Type:** Structure
- **Enables:** Ask yields a recall prompt

Rename Gherkin/steps/feature titles that say **quiz question** to **recall
prompt** (e.g. `When I visit recall for a due quiz question on day {int}`).
Rename contest feature language to contest an **MCQ**. No product change;
existing scenarios still pass.

**Verify:** targeted `cypress run --spec` for touched recall features.

### 2. Ask yields a recall prompt

- **Status:** planned
- **Type:** Behavior
- **Pre:** A due memory tracker (spelling or understanding).
- **Trigger:** Learner starts recall (web or CLI).
- **Post:** The client loads a **recall prompt** from
  `GET /api/memory-trackers/{id}/recall-prompt`. There is no `RecallQuestion`
  type and no `.../question` / `askAQuestion` path.

Drop `RecallQuestion` / `RecallQuestion.from`. Return a learner view of
`RecallPrompt` (stem+choices or spelling stem; no solution). Rename `Quiz.vue`
to a recall-session name in this slice so the session is not a “quiz.” CLI
help that says “no quiz” follows the glossary (just review / no prompt).

**Tests:** existing recall E2E (MCQ + spelling); frontend `Quiz`/`RecallPage`
specs; CLI interactive recall; controller tests. Then `generateTypeScript`.

### 3. Answer a recall prompt

- **Status:** planned
- **Type:** Behavior
- **Pre:** An unanswered recall prompt is on screen.
- **Trigger:** Learner submits an MCQ choice or spelling.
- **Post:** Submit uses `POST /api/recall-prompts/{id}/answer` as **answer**,
  not `answerQuiz`. Frontend has no `submitQuizAnswer` name.

Path is already `/answer`; this slice is the operation and client names.

**Tests:** existing answer E2E + `RecallPromptComponent` / controller answer tests.

### 4. Answered MCQ is named MCQ

- **Status:** planned
- **Type:** Behavior
- **Pre:** Learner has answered an MCQ recall prompt.
- **Trigger:** Result / history is shown.
- **Post:** Wire field is `mcq`, not `predefinedQuestion` (`AnsweredQuestion`,
  history items). Type may still be `PredefinedQuestion` until the next slice.

**Tests:** memory-tracker prompt history unit tests; answered-question component
tests; regenerate client.

### 5. Trainer manages MCQs on a note

- **Status:** planned
- **Type:** Behavior
- **Pre:** Note owner opens note MCQ management.
- **Trigger:** List / add / AI-generate / refine.
- **Post:** API is `/api/mcqs/...`; Java/OpenAPI type is **Mcq** (not
  `PredefinedQuestion`). Table name may still be `predefined_question`.
  Button microcopy may still say “question” (ADR out of scope).

**Tests:** `predefined_questions_management.feature` scenarios (keep
capability-named file; update wording inside); controller tests; regenerate
client.

### 6. Schema table is mcq

- **Status:** planned
- **Type:** Structure
- **Enables:** Schema table matches Answer (and any later MCQ work)

Flyway rename `predefined_question` → `mcq`; `recall_prompt.predefined_question_id`
→ `mcq_id`. Entity `@Table` matches. No API JSON change.

**Verify:** backend tests that persist MCQs / recall prompts.

### 7. Schema table matches Answer

- **Status:** planned
- **Type:** Structure
- **Enables:** Learner MCQ view is not a named type

Flyway rename `quiz_answer` → `answer`; `recall_prompt.quiz_answer_id` →
`answer_id`. Entity is already `Answer`.

**Verify:** backend answer / recall-prompt tests.

### 8. Learner MCQ view is not a named OpenAPI type

- **Status:** planned
- **Type:** Structure
- **Enables:** AI generates an Mcq

Remove `MultipleChoicesQuestion` from OpenAPI. Unanswered prompt exposes
stem+choices as fields on the prompt or on `Mcq` without a second type name.
`generateTypeScript`; existing MCQ recall E2E still pass (same pixels).

### 9. AI generates an Mcq

- **Status:** planned
- **Type:** Structure

`MCQWithAnswer` (and refine subclass) become `Mcq` or a factory onto `Mcq`.
`choicesMayBeShuffled` is either stored on `Mcq` or kept as an AI-only field —
do not invent a glossary noun for it.

**Verify:** question-generation / contest / regenerate controller tests.

## Out of scope

- Using the note’s MCQ list as the recall source
- Inferring `QuestionType` from `mcq_id` and dropping the enum (no next
  behavior in this plan)
- Renaming **just review**
- Mass UI microcopy (“Questions” menu)

## Jidoka

- Confirm no external API consumers that need dual JSON field names.
- Table renames need a real Flyway version and a deploy that applies it.
- Do not add compatibility DTOs that reintroduce translation.
