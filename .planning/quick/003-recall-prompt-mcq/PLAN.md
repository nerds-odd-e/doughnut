# Plan: Recall prompt / MCQ — minimum translation

**Status:** done (2026-08-15)

**Goal (shipped):** Same nouns as ADR 0001 — **recall prompt** HAS_A **MCQ** —
in tests, API, code, and schema. No `RecallQuestion` / `PredefinedQuestion` /
`MultipleChoicesQuestion` / `MCQWithAnswer` product types.

## Shipped shape

- Unanswered ask: DTO/SDK **`RecallPrompt`** at `GET /api/memory-trackers/{id}/recall-prompt`
- Answer: SDK **`answer`** at `POST /api/recall-prompts/{id}/answer`
- Note MCQs: type **`Mcq`** (`questionStem` / `responseChoices`), HTTP `/api/mcqs`
- Unanswered prompt carries a solution-omitted **`mcq`** (`Mcq.withoutSolution()`)
- Tables: `mcq` (`V300000257`), `answer` (`V300000258`); FKs `recall_prompt.mcq_id`, `recall_prompt.answer_id`
- AI generate/refine/contest-regenerate: Mcq field names; **`GeneratedMcq`** is AI-only parse/shuffle (`choicesMayBeShuffled` not persisted)

## Leftovers (out of this plan)

- `Quiz.vue`, note **Questions** menu, nested HTTP segments (`note-questions`, `refine-question`, …)
- `RecallQuestionService`, CLI `RecallQuestionAnswerOutcome`
- Internal `MultipleChoicesQuestion` (DB `raw_json_question` converter only)
- Using the note’s MCQ list as the recall source
- Inferring `QuestionType` from `mcq_id`

## Ops

Deploy must apply Flyway **V300000257** and **V300000258**.
