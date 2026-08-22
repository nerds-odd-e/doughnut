---
id: SEED-005
status: dormant
planted: 2026-08-18
planted_during: spent-plan cleanup after recall-prompt / MCQ translation shipped
trigger_when: when renaming recall-prompt or MCQ product nouns
scope: small
---

# SEED-005: Finish leftover recall-prompt / MCQ nouns

## Why This Matters

The shipped glossary is Proposed [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md): **recall prompt** HAS_A **MCQ**. Public API, OpenAPI `Mcq`, tables `mcq`/`answer`, and nested `/api/mcqs` routes already match. A few internal names and the note **Questions** UI still say question.

## When to Surface

**Trigger:** renaming recall-prompt or MCQ product nouns; touching `Quiz.vue`, the note Questions menu, `RecallQuestionService`, or CLI `RecallQuestionAnswerOutcome`.

## Scope Estimate

**Small** — leftover cohesion, plus two optional product follow-ons:

1. Rename remaining question-shaped names (`Quiz.vue`, note **Questions** menu, `RecallQuestionService`, CLI `RecallQuestionAnswerOutcome`).
2. Keep `MultipleChoicesQuestion` as the `raw_json_question` converter type, or fold it into `Mcq` if that stays one concept.
3. Optional: use the note’s MCQ list as the recall source; infer `QuestionType` from `mcq_id`.

## Breadcrumbs

- `frontend/src/components/recall/Quiz.vue`
- `frontend/src/components/notes/Questions.vue`
- `backend/src/main/java/com/odde/doughnut/services/RecallQuestionService.java`
- `cli/src/commands/recall/recallQuestionAnswerOutcome.ts`
- `backend/src/main/java/com/odde/doughnut/entities/MultipleChoicesQuestion.java`
- `docs/adrs/0001-ubiquitous-language.md`

## Notes

Parked from the spent recall-prompt / MCQ translation plan (done 2026-08-15). Nested `/api/mcqs` routes are already shipped.
