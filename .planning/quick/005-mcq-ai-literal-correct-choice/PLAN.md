# Describe MCQ correct answers as choice text in AI prompts

**Status:** planned — not started.
**Type:** ad-hoc plan (`.planning/quick/`)
**Origin:** Generation already emits `GeneratedMcq` (`correctAnswer` + `distractors`). Contest advice and two related prompts still talk in 0-based indexes.

## Goal

When a model is told what the current correct choice is (contest advice, regenerate the previous question, question-led note refinement), it sees the **choice text**, not `correctAnswerIndex`.

## Value ordering

1. Contest disagreement advice (learner-visible, and regeneration feedback).
2. Regeneration’s dump of the previous question (generating AI reads this).
3. Note-refinement layout prompt (weaker leftover; not MCQ generation).

## Key design decisions

- **Evaluation schema stays indexes.** `QuestionEvaluation.correctChoices` is still `int[]` (“pick from this ordered list”). Do not change that contract, E2E stubs, or comparison to `Mcq.correctAnswerIndex`.
- **Persistence and answering stay indexes.** `Mcq.correctAnswerIndex`, recall UI, CLI number keys, and `NoteRefinementQuestionContextDTO.correctAnswerIndex` stay. Only the **prompt/advice wording** resolves index → text.
- **No new E2E.** `question_contest.feature` already covers contest → replacement and does not assert advice copy. Extend existing unit/controller tests that already pin this text.
- **Advice copy** (slice 1): quote literals only. Example: original `"Paris"`; re-evaluation `"London", "Berlin"`; no `0 ("Paris")` and no `1 ("London")`. Out-of-bounds evaluation indexes contribute no text (same as none). Invalid stored index quotes `"unknown"`.

## Out of scope

- Changing how the evaluation model *returns* correct choices.
- Shuffle / post-process (`GeneratedQuestionPostProcessor` still derives the stored index).

## Jidoka checkpoints — stop for developer judgement

None required. If evaluation should also return choice texts instead of indexes, that is a separate plan — do not fold it in here.

---

## Slices

Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

### 1. Contest disagreement advice quotes choice text — Behavior `[ ]`

**Pre:** An MCQ has one stored correct choice (e.g. `"Paris"`); evaluation does not agree that only that choice is correct.
**Trigger:** `QuestionEvaluation.getQuestionContestResult` builds contest advice (learner contest or auto-regenerate).
**Post:** Advice names original and re-evaluated answers as quoted choice text. It does not mention a 0-based index.

- Extend `QuestionEvaluationTest` (this class is the stable contract for the advice string). Update the no-agreement, multiple-correct, and out-of-bounds cases; do not add a new test class.
- Production: `QuestionEvaluation.java` advice builder only.

### 2. Regeneration describes the previous question as generated shape — Behavior `[ ]`

**Pre:** Slice 1 done. A contested MCQ is regenerated.
**Trigger:** `AiToolFactory.buildRegenerateQuestionMessage` is included in the OpenAI generate request.
**Post:** The previous-question JSON uses `questionStem`, `correctAnswer`, and `distractors`. It does not include `correctAnswerIndex` or `responseChoices`.

- Extend `RecallPromptRegenerateControllerTest.shouldPassOldQuestionAndContestResultToOpenAiApi` (existing payload capture). Assert the generated-shape fields and absence of the persisted-shape fields.
- Production: serialize a `GeneratedMcq` view of the persisted MCQ (correct choice text + other choices as distractors). Keep `testedFocus` / `validationRationale` when present.

### 3. Note refinement layout prompt names the correct choice by text — Behavior `[ ]`

**Pre:** Generate refinement layout with question context (stem, choices, `correctAnswerIndex`).
**Trigger:** `generateNoteRefinementLayoutAiTool` builds instructions.
**Post:** Instructions include `Correct answer: <choice text>` and list choices without 0-based labels. They do not include `Correct answer index`.

- Extend `AiControllerNoteRefinementTest.shouldAppendQuestionLedGuidanceWhenQuestionContextProvided`.
- Production: `NoteRefinementAiToolFactory.questionLedLayoutGuidance` only. DTO field stays `correctAnswerIndex`.
