# Semantic answers for AI-generated MCQs

**Status:** in progress

## Goal

An AI-generated MCQ names its correct answer semantically instead of pointing at
an array position. Doughnut owns choice assembly, shuffling, and
`correctAnswerIndex`, so a stale model-authored index cannot mark a distractor
as correct. Existing persisted MCQs keep their current storage and REST shape.

## Requirements

- `GeneratedMcq` remains the one AI structured-response type for generation,
  refinement, regeneration, assistant tools, and batch requests.
- Its answer fields become:
  - one nonblank `correctAnswer`;
  - exactly three nonblank `distractors`;
  - no `responseChoices`, `correctAnswerIndex`, or
    `choicesMayBeShuffled` in the AI schema.
- Doughnut tags the correct answer before combining and shuffling the four
  choices, then derives the persisted index from that tag rather than text or
  an AI-supplied number.
- Invalid semantic answer sets are rejected before an `Mcq` or `RecallPrompt`
  is created: wrong distractor count, blank choice, repeated distractor, or a
  distractor duplicating the correct answer. Compare choice identities after
  stripping surrounding whitespace; preserve the authored text when storing.
- Existing independent-evaluator behavior remains intact: where it is already
  configured or invoked by a contest, disagreement contests/regenerates and
  never rewrites the stored index.
- `testedFocus` and `validationRationale` retain their current meaning and
  persistence.

## Explicitly unchanged

- `Mcq.raw_json_question` remains the JSON persistence of
  `MultipleChoicesQuestion(questionStem, responseChoices)`.
- `Mcq.correct_answer_index`, the database schema, the REST `Mcq` shape, and
  generated frontend API remain unchanged.
- Existing imported MCQs and unanswered recall prompts are not reparsed or
  rewritten.
- There is no compatibility parser for the old OpenAI response: the developer
  confirmed there are no submitted, output-ready, or otherwise outstanding
  old-schema batches.
- Do not parse `validationRationale` to infer the answer.
- `QuestionEvaluation` keeps selecting indices from the already-fixed displayed
  choice array. Unlike generation, it does not author and reorder that array;
  indices also preserve ambiguity detection for older/manual duplicate choices.

## Key design decisions

| Decision | Choice | Rationale |
|---|---|---|
| AI type | Change the existing `GeneratedMcq`; do not add a parallel response DTO | It is already the structured-output boundary. A second near-identical type would add translation without value. |
| Conversion owner | `GeneratedQuestionPostProcessor` is the sole `GeneratedMcq -> Mcq` assembler | Every live, refined, regenerated, and batch question must pass through the same semantic-to-positional conversion. |
| Correct identity | Carry an internal tagged choice through the shuffle | Looking up by answer text is unnecessary and becomes ambiguous if validation ever changes. |
| Choice order | Always shuffle AI-generated correct answer plus distractors | The prompts prohibit order-dependent/meta choices; the model no longer decides whether shuffling is allowed. |
| Evaluator scope | Preserve existing contest/automatic-evaluation behavior and indices; add no new OpenAI calls | Production auto-evaluation currently defaults off and batch import bypasses it. Universal batch evaluation needs a separate two-stage offline workflow, not network calls inside import transactions. |

## Discoveries affecting execution

- Concurrent user-owned changes already move `MultipleChoicesQuestion` from
  `services.ai` to `entities` and remove its AI-schema annotations. Preserve
  and build on those edits; do not revert or duplicate them.
- `question.regeneration.times` is `1` in backend tests/E2E but defaults to `0`
  in production. Enabling it would affect only synchronous fallback generation,
  not batch-created prompts.
- Batch row import is transactional and sequential. Calling the evaluator from
  it would hold database transactions across external requests and discard the
  Batch API's latency/cost benefit.
- `questionRefineAiTool` currently permits two to four choices. It must join the
  common contract of one correct answer plus exactly three reorder-safe
  distractors.
- The captured `live_batch_success_line.json` uses the retired response shape.
  Replace or rename it as a representative semantic-contract fixture; do not
  retain legacy parsing solely for that test.

## Slices

### 1. One generated-question assembly boundary

**Status:** done
**Type:** Structure

`GeneratedQuestionPostProcessor.assembleMcq` is now the sole conversion from
`GeneratedMcq` to a persisted `Mcq`. Generation, refinement, regeneration, and
batch import all use it; non-AI fixtures build `Mcq` directly.

Refactor without changing the current AI schema or observable question
behavior. This structure exists only to localize the immediate semantic-contract
behavior in slice 2.

- Change `GeneratedQuestionPostProcessor` to accept the note/context seed and
  return the completed `Mcq`, including metadata and shuffled choices.
- Remove `GeneratedMcq.toMcq`; a raw AI response must not be directly
  persistable.
- Make `AiQuestionGenerator` return processed `Mcq` objects for generation,
  refinement, and regeneration.
- Route `McqController`, `McqService`, `RecallQuestionService`, and
  `QuestionGenerationBatchRowImportService` through that one conversion
  boundary.
- Decouple internal `Mcq` fixtures and evaluator tests from `GeneratedMcq` when
  they do not exercise the AI boundary.
- Preserve the concurrent `MultipleChoicesQuestion` relocation.

Verification:

- Existing controller, recall-generation, refinement/regeneration, contest,
  and batch-import behavior remains green.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

Stop-safe outcome: all generated-question paths still behave as before, but no
caller can bypass the one conversion boundary used by slice 2.

### 2. Valid semantic AI answer becomes the indexed choice

**Status:** planned
**Type:** Behavior

**Precondition:** OpenAI returns a valid stem, one correct answer, and three
distinct distractors.
**Trigger:** Doughnut consumes that response through live generation,
refinement/regeneration, or batch import.
**Postcondition:** The resulting `Mcq` contains four shuffled choices and its
derived `correctAnswerIndex` selects the declared correct answer.

Test first:

- In `QuestionGenerationBatchRowImportServiceTest`, reproduce the reported
  Japanese case with correct answer `しない` and distractors `しなかった`,
  `しなくて`, and `しないで`; deterministically reorder the choices and assert
  that the indexed stored text is still `しない`.
- Adapt `McqRefinementControllerTests` to prove the same result through the live
  controller boundary without duplicating the full batch assertion.
- Update `McqControllerTests.ExportMcq` to assert the exported structured schema
  contains `correctAnswer` and `distractors` and excludes
  `responseChoices`, `correctAnswerIndex`, and `choicesMayBeShuffled`.

Implementation:

- Change `GeneratedMcq` to the semantic fields and keep its non-answer metadata.
- Keep an interim `isValid` check for a nonblank stem/correct answer and a
  nonempty distractor list so all raw-response boundaries remain safe and
  compilable. Exact count, blank distractors, and duplicates are tightened in
  slice 3; accepting those edge shapes temporarily is no worse than the current
  validator.
- Assemble tagged choices, shuffle them through `TestabilitySettings`, derive
  the index from the tag, and build `Mcq` in `GeneratedQuestionPostProcessor`.
- Update the base and refinement instructions to request exactly three
  independent distractors and remove shuffle/index instructions.
- Update `GeneratedMcqBuilder`, raw-response fixtures, assistant-tool schema
  expectations, and mechanical test call sites.
- Adapt the existing `McqTest.AutoEvaluateAndRegenerate` regression so it still
  proves evaluator disagreement regenerates rather than rewriting an answer
  index after the new assembly boundary.
- Update `e2e_test/start/questionGenerationService.ts` and every generated-MCQ
  feature table to provide `Incorrect Choice 3`; do not hide the four-choice
  contract behind a test-helper default.

Verification:

- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
- Run the relevant generation and contest specs:
  `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/recall_quiz_ai_question.feature,e2e_test/features/ai_generated_recall_questions/question_contest.feature`.

Stop-safe outcome: the stale positional-pointer failure is unrepresentable for
every newly generated MCQ, including batch imports.

### 3. Invalid semantic answer sets create no question

**Status:** planned
**Type:** Behavior

**Precondition:** OpenAI returns an otherwise parseable MCQ with the wrong
number of distractors, a blank distractor, or duplicate displayed choice text.
**Trigger:** Doughnut validates the structured response.
**Postcondition:** The response is rejected and no invalid `Mcq` or
`RecallPrompt` is created; a batch row follows its existing failed-import path.

Test first:

- Add a parameterized pure-contract `GeneratedMcqTest` for the invalid semantic
  shapes; keep one canonical valid case.
- Update the existing invalid refinement controller test to use one semantic
  validation failure.
- Add one batch-import boundary assertion that an invalid semantic answer set
  marks the request failed and creates no recall prompt; do not repeat every
  pure validation case at this boundary.

Implementation:

- Tighten `GeneratedMcq.isValid` around the agreed semantic invariants.
- Preserve the existing visible failure behavior in live generation and batch
  metrics/status handling.

Verification:

- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

Stop-safe outcome: malformed-but-schema-parseable answers cannot enter recall.

## Follow-up deliberately deferred

If every future nightly batch question must receive independent AI evaluation,
plan a separate two-stage offline workflow:

```text
generation batch -> evaluation batch -> import accepted questions / regenerate rejected questions
```

That follow-up needs explicit persisted states, retry policy, cost/latency
policy, and scheduler behavior. It must not be approximated by synchronous
OpenAI calls inside `QuestionGenerationBatchRowImportService` or recall GETs.

## Slice wrap-up contract

For every executed slice: run its red-to-green cycle, run
`post-change-refactor`, update this plan, format as needed, run the listed full
backend test command, then commit and push before starting the next slice.
After the last slice, run `CURSOR_DEV=true nix develop -c pnpm backend:verify`
and clean up spent planning history according to `planning.mdc`.
