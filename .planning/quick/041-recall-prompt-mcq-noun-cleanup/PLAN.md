# Recall-prompt / MCQ noun cleanup

**Status:** planned
**Architecture:** ADR 0001 Ubiquitous language (Accepted 2026-09-01) — **recall prompt** HAS_A **MCQ**
**Source:** `.planning/seeds/SEED-005-leftover-recall-prompt-mcq-nouns.md`
**Goal:** Rename the internal identifiers the seed flagged as still saying "question" after the recall-prompt / MCQ translation shipped, so code search and onboarding land on the ADR's terms. No user-visible copy changes and no public API changes — pure naming cohesion (`general.mdc` principle 1).

## Why these are Structure-only slices

Every slice below renames a file/class/variable/id with zero change to what a
user, an API caller, or an existing test observes — verified by the existing
test suite passing unchanged. There is no following "Behavior" slice: the
value is the naming cohesion itself (searchability, onboarding, matching the
Accepted ADR), not a new capability. Each slice is still independently
stop-safe — doing 1 of 4 and stopping leaves zero waste and zero half-done
state, so the slice grammar's stop-safe requirement holds even without a
downstream Behavior slice.

## Slices

### 1. Structure — rename CLI `RecallQuestionAnswerOutcome` type
Status: done
- `cli/src/commands/recall/recallQuestionAnswerOutcome.ts` → `recallPromptAnswerOutcome.ts`, type `RecallQuestionAnswerOutcome` → `RecallPromptAnswerOutcome`.
- Update importers: `SpellingRecallStage.tsx`, `JustReviewRecallStage.tsx`, `RecallSessionStage.tsx`, `RecallMcqStage.tsx`.
- Verify: `cli` typecheck/build + its existing tests (pure type, no runtime change).
- Verified: `pnpm cli:test` 299/299 passed; no remaining references to the old name anywhere in the repo.

### 2. Structure — rename backend `RecallQuestionService`
Status: done
- `backend/.../services/RecallQuestionService.java` → `RecallPromptService.java`, class renamed to match.
- Update the two callers: `MemoryTrackerController.java`, `RecallPromptController.java`.
- No test file exists for this class today (behavior stays covered through the two controllers' existing tests) — run those targeted tests.
- Verified: `MemoryTrackerControllerTest` + `RecallPromptControllerTest` pass; no remaining references to the old name in code.

### 3. Structure — rename `Quiz.vue`
- `frontend/src/components/recall/Quiz.vue` → `RecallPromptCard.vue` (component name + `QuizProps` interface renamed to match).
- Update the one import site: `frontend/src/pages/RecallPage.vue`.
- Rename its tests: `frontend/tests/recall/Quiz.spec.ts` → `RecallPromptCard.spec.ts`, `frontend/tests/recall/quizTestSupport.ts` → `recallPromptCardTestSupport.ts`.
- Verify: targeted vitest run for the renamed spec.

### 4. Structure — rename the note "Questions" surface's internal identifiers
- `frontend/src/components/notes/Questions.vue` → `Mcqs.vue`; internal names `questionAdded` → `mcqAdded`, `fetchQuestions` → `fetchMcqs`, CSS class `.question-table` → `.mcq-table`.
- Action id `'questions'` → `'mcqs'` in `NoteMoreOptionsActions.vue`, `noteToolbarOverflow.ts`, and the `noteMoreOptionsTitles.ts` key (confirmed not persisted anywhere — driven only by the `only`/`omit` props each caller passes inline, so nothing needs a migration).
- **Keep visible copy unchanged** ("Add Question", "Question Text", "No questions", "Questions for the note" tooltip) — that's a product-copy call, not a naming-cohesion one; out of scope here.
- Rename its tests: `frontend/tests/notes/Questions.spec.ts` → `Mcqs.spec.ts`, `frontend/tests/notes/questionsTestSupport.ts` → `mcqsTestSupport.ts`.
- Verify: targeted vitest run for the renamed spec.

Slices are independent (disjoint files) and can run in any order; listed smallest/lowest-risk first.

## Decided, not sliced

- **`MultipleChoicesQuestion` / `MCQToJsonConverter`** (seed item 2) — **keep as-is**, not folded into `Mcq`. It's a `@JsonIgnore`d internal DTO + JPA `AttributeConverter` pair for the single `raw_json_question` column; both `Mcq`'s public API field names (`questionStem`, `responseChoices` — shipped OpenAPI contract) and the stored JSON shape for existing `mcq` rows are untouched either way. Folding it in would touch persistence/serialization plumbing for a class-name-only win and risks the stored data's read path. Re-open only if `Mcq`'s persistence is being reworked for an unrelated reason.
- **Seed item 3 (optional): use the note's MCQ list as the recall source; infer `QuestionType` from `mcq_id`** — this is a behavior/product change, not a naming cleanup, and the seed itself marks it optional. Left for a future seed if still wanted.

## Discovered: wider "question" naming surface (out of scope here)

The seed's 4 breadcrumbs are a small slice of a much larger surface that still
says "question": `QuestionStem.vue`, `QuestionDisplay.vue`, `QuestionChoices.vue`,
`NoteUnderQuestion.vue`, `ContestableQuestion.vue`, `AnsweredQuestionComponent.vue`,
`SpellingQuestionDisplay.vue`, `QuestionGenerationBatchStatus.vue`,
`QuestionContestResult`, `AiQuestionGenerator`, `QuestionEvaluation`,
`RegenerateQuestionMessage`, plus their tests — dozens of files. Renaming all
of that is a large, separate effort, not "small." This plan intentionally
stays inside the seed's stated scope; if a full sweep is still wanted, plant
a new seed for it rather than growing this one.
