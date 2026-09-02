---
id: SEED-005
status: planned
planted: 2026-08-18
planted_during: spent-plan cleanup after recall-prompt / MCQ translation shipped
trigger_when: when renaming recall-prompt or MCQ product nouns
scope: small
plan: .planning/quick/041-recall-prompt-mcq-noun-cleanup/PLAN.md
---

# SEED-005: Finish leftover recall-prompt / MCQ nouns

## Why This Matters

The shipped glossary is Accepted [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md): **recall prompt** HAS_A **MCQ**. Public API, OpenAPI `Mcq`, tables `mcq`/`answer`, and nested `/api/mcqs` routes already match. A few internal names and the note **Questions** UI still say question.

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
- `backend/src/main/java/com/odde/donut/services/RecallQuestionService.java`
- `cli/src/commands/recall/recallQuestionAnswerOutcome.ts`
- `backend/src/main/java/com/odde/donut/entities/MultipleChoicesQuestion.java`
- `docs/adrs/0001-ubiquitous-language.md`

## Notes

Parked from the spent recall-prompt / MCQ translation plan (done 2026-08-15). Nested `/api/mcqs` routes are already shipped.

Converted into `.planning/quick/041-recall-prompt-mcq-noun-cleanup/PLAN.md` on 2026-09-02: item 1 (the four breadcrumb renames) became 4 Structure slices; item 2 (`MultipleChoicesQuestion`) was decided — kept as-is, not folded; item 3 (optional MCQ-list recall source) stayed out of scope as a future seed candidate.
