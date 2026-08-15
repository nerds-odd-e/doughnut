# Recall prompt / MCQ alignment — context

**Status:** executing (slices 1–3 done)  
**Glossary:** Proposed [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md), [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy.md)

## Requirement

Replace the overloaded **Quiz / question** model with two nouns that already
match persistence intent:

- **Recall prompt** — one ask during recall for a memory tracker (`recall_prompt`).
  Kinds: **spelling** (no MCQ) or **MCQ** (the prompt **HAS_A** an MCQ).
- **MCQ** — persisted multiple-choice content on a note (today’s
  `predefined_question`). **Contested** / **contest** belong to the MCQ.
  Origin (AI vs manual) is not a prompt kind.

**Just review** is a method of **recall** (self-evaluate after seeing the note),
not “recall with no prompt.”

End state: **the same nouns in UI tests, OpenAPI, code, and schema**, with
**minimum DTO** — no type that translates a recall prompt into a “question.”

A learner-facing payload may still omit the MCQ solution. That is a
**projection** of an MCQ, not a second domain type (`RecallQuestion`).

## Goal

Stop-safe, incremental alignment so that if work stops after any slice, shipped
code is consistent and CI-green. Value is proportional: each slice removes one
real translation (or one leftover name) rather than adding aliases.

Out of scope here: wiring note-authored MCQs into the recall queue; renaming
**just review**; i18n / exact button microcopy; moving contest off the prompt
URL (contest is triggered while viewing a prompt; the flag lives on the MCQ).

## Current translations (inventory)

| Glossary | Persistence | Code / API | Tests / UI leftovers |
|----------|-------------|------------|----------------------|
| Recall prompt | `recall_prompt` | Entity `RecallPrompt`; DTO **`RecallPrompt`**; `GET .../recall-prompt` `getRecallPrompt`; `RecallQuestionService`; `Quiz.vue` (file name out of scope) | E2E due recall prompt; `answerQuiz` |
| MCQ | `predefined_question` | `PredefinedQuestion`; `/api/predefined-questions`; JSON `predefinedQuestion` | E2E “predefined questions” |
| Answer | `quiz_answer` | Entity `Answer`; `answerQuiz` | `submitQuizAnswer` |
| MCQ stem+choices | JSON in `raw_json_question` | `MultipleChoicesQuestion` | |
| AI generate/refine | (same rows) | `MCQWithAnswer` | |

Plan slices (see PLAN.md): 1 tests → 2 type RecallPrompt → 3 path → 4 CLI help → 5 answer op → 6 field `mcq` → 7 tests MCQ → 8 type Mcq → 9 `/mcqs` → 10 table `mcq` → 11 table `answer` → 12 drop `MultipleChoicesQuestion` → 13 AI uses Mcq.

Contest HTTP is already on `/recall-prompts/{id}/contest` and mutates the MCQ.
That is HAS_A, not a translation to delete.

Recall never selects trainer-authored rows from the note MCQ list; it always
generates a new MCQ. This plan does **not** change that product behavior.
