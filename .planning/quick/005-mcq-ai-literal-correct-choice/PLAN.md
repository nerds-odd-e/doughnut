# Describe MCQ correct answers as choice text in AI prompts

**Status:** in progress — slice 1 done.
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
- **Advice copy** (slice 1, done): quote literals only. Original `"Paris"`; re-evaluation `"London", "Berlin"`; OOB evaluation indexes contribute no text (`none`); invalid stored index quotes `"unknown"`.

## Out of scope

- Changing how the evaluation model *returns* correct choices.
- Shuffle / post-process (`GeneratedQuestionPostProcessor` still derives the stored index).

## Jidoka checkpoints — stop for developer judgement

None required. If evaluation should also return choice texts instead of indexes, that is a separate plan — do not fold it in here.

---

## Slices

Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

### 1. Contest disagreement advice quotes choice text — Behavior `[x]`

Advice from `QuestionEvaluation.getQuestionContestResult` quotes original and re-evaluated choice text (`quotedOriginalChoice` / `quotedCorrectChoices`). `correctChoices` stays `int[]`. Covered in `QuestionEvaluationTest`.

**Learning:** quoting is advice-only; do not extract a shared “index → text” helper for slice 2 (`GeneratedMcq` is a different shape).

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
