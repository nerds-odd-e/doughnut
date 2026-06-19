# Question Generation Cohesion

Status: planned

## Problem

Batch-generated multiple-choice questions preserve the model's choice order when imported, while synchronous generation shuffles choices when `choicesMayBeShuffled` is true. This is a bug, but it also exposes a cohesion problem: post-processing of AI-generated MCQs is owned by the synchronous `AiQuestionGenerator` path instead of by the domain concept "an AI-generated MCQ result is ready for use".

## Current Divergence

- Synchronous generation:
  - `AiQuestionGenerator.getAiGeneratedQuestion(...)` calls `NoteQuestionGenerationService.generateQuestion(...)`.
  - It then shuffles choices inside a private `shuffleChoices(...)` method.
- Batch generation:
  - `QuestionGenerationBatchRowImportService.importRow(...)` parses an `MCQWithAnswer` from a batch output line.
  - It persists `PredefinedQuestion.fromMCQWithAnswer(...)` directly, so choices are not shuffled.
- Refinement:
  - `AiQuestionGeneratorForNote.refineQuestion(...)` returns an `MCQWithAnswer`.
  - `PredefinedQuestionService.refineAIQuestion(...)` persists it directly, so it also bypasses the synchronous shuffle behavior.

## Design Direction

Create a single domain-level post-processing boundary for AI-generated MCQs, for example `GeneratedQuestionPostProcessor` or `MCQWithAnswerNormalizer`.

Responsibilities:

- Validate only where the caller already expects validation, or keep validation separate if that is clearer.
- If `choicesMayBeShuffled` is false, return the question unchanged.
- If `choicesMayBeShuffled` is true, shuffle response choices and recompute `solutionChoiceIndex`.
- Track the correct answer by original choice index, not by answer text, so duplicate choice text cannot corrupt the answer index.
- Preserve stem, `testedFocus`, `validationRationale`, and other non-choice metadata.
- Use the existing testability randomization mechanism so backend and E2E tests can remain deterministic.

Non-goals:

- Do not put MCQ shuffling into `OpenAiApiHandler`; it is transport/parsing infrastructure.
- Do not put randomization into `PredefinedQuestion.fromMCQWithAnswer(...)`; it is an entity mapping boundary and should stay deterministic.
- Do not change batch eligibility, submission, polling, collection, contesting, or regeneration behavior.

## Phase 1: Batch Imported Questions Use The Shared AI MCQ Post-Processor

Status: done

Type: Behavior

Precondition: A question generation batch request has an `OUTPUT_READY` row whose structured output is an `MCQWithAnswer` with `choicesMayBeShuffled = true`.

Trigger: Batch import runs for the completed batch.

Postcondition: The persisted `PredefinedQuestion` has shuffled choices and a corrected `correctAnswerIndex`, matching the same AI MCQ post-processing contract used by synchronous generation.

Implementation notes:

- Extract the existing shuffle behavior out of `AiQuestionGenerator` into a cohesive service such as `GeneratedQuestionPostProcessor`.
- Replace `AiQuestionGenerator.shuffleChoices(...)` with a call to the new service so synchronous behavior remains unchanged through the same contract.
- Inject and use the same service in `QuestionGenerationBatchRowImportService` after parsing and validating the batch output, before `PredefinedQuestion.fromMCQWithAnswer(...)`.
- Fix the current text-based correct-answer tracking while extracting: shuffle indexed choice objects or otherwise preserve original indices, then derive the new correct index from the original `solutionChoiceIndex`.
- Keep the batch import service focused on import orchestration: parse row, post-process generated question, persist prompt, update row status.

Tests:

- Extend the existing batch import behavior test to assert the imported `PredefinedQuestion` choices are shuffled and `correctAnswerIndex` points to the same semantic answer after shuffling.
- Add a small black-box unit test for the post-processor's duplicate-choice edge case, because duplicate text is easier and clearer to cover at the pure domain boundary.
- Keep or adapt the existing synchronous shuffle test so it proves synchronous generation still receives post-processed output without duplicating shuffle assertions across multiple structural tests.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Phase 2: Refined AI Questions Use The Same Post-Processing Contract

Status: done

Type: Behavior

Precondition: A user asks to refine an existing predefined question, and OpenAI returns an `MCQWithAnswer` with `choicesMayBeShuffled = true`.

Trigger: `PredefinedQuestionController.refineQuestion(...)` completes successfully.

Postcondition: The returned refined `PredefinedQuestion` has choices post-processed by the same shared contract as synchronous and batch generation.

Implementation notes:

- Apply the shared post-processor in the refinement path.
- Prefer doing this in `AiQuestionGenerator.getAiGeneratedRefineQuestion(...)` or another generation-output boundary, not inside the controller or entity factory.
- Review `AiQuestionGeneratorForNote`; if it remains, keep it as an OpenAI request execution helper, not the owner of MCQ domain normalization.
- Avoid creating a second "refinement shuffle" concept.

Tests:

- Extend the existing controller refinement test so the OpenAI mock returns known ordered choices and the controller returns the post-processed order with the adjusted correct index.
- Avoid a separate implementation-shaped test for the same behavior unless a pure edge case needs it.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Phase 3: Clarify Question Generation Service Boundaries

Status: done

Type: Structure

Precondition: All current AI MCQ output paths use the shared post-processing contract.

Trigger: Refactor service names/dependencies around the now-shared boundary.

Postcondition: Existing behavior is unchanged, but service responsibilities are easier to understand and no class owns duplicate conceptual pieces of question-generation output handling.

Why structure: Phases 1 and 2 reveal the intended domain boundary by using it. This phase removes leftover ambiguity without speculating about new behavior.

Implementation notes:

- Make class responsibilities explicit:
  - `QuestionGenerationRequestBuilder`: request/prompt construction only.
  - `NoteQuestionGenerationService`: synchronous OpenAI request execution for question/evaluation flows.
  - `AiQuestionGenerator`: high-level orchestration for generation/refinement/regeneration, delegating output post-processing.
  - `GeneratedQuestionPostProcessor`: MCQ output normalization only.
  - `QuestionGenerationBatchRowImportService`: batch row import orchestration only.
- Remove unused or misleading fields from `AiQuestionGenerator` if extraction makes them unnecessary.
- Consider whether `AiQuestionGeneratorForNote` still pulls its weight or whether its behavior is clearer folded into `AiQuestionGenerator`; do this only if it reduces indirection without widening behavior.
- Do not move request construction or OpenAI transport behavior into the post-processor.

Discovery:

- `QuestionGenerationRequestBuilder.openAiResponseRequestForQuestionGeneration(...)` already applies `globalSettingQuestionGeneration()`, so folding refinement request execution into `NoteQuestionGenerationService` keeps the selected model behavior unchanged without `AiQuestionGenerator` depending on `GlobalSettingsService`.

Tests:

- No new behavior test should be required if Phases 1 and 2 already cover the observable behavior.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only` to verify external behavior remains unchanged.

## Phase 4: Reduce Request-Building Ownership Leakage

Status: planned

Type: Structure

Precondition: Generated-question output handling is cohesive.

Trigger: Refactor the export-question-generation controller path.

Postcondition: Existing export behavior is unchanged, but `PredefinedQuestionController` no longer manually constructs `QuestionGenerationRequestBuilder` and duplicates dependency wiring that Spring already owns.

Why structure: This is a nearby cohesion issue in the same capability: the controller currently leaks request-builder construction details, which makes future question-generation changes easier to miss.

Implementation notes:

- Inject `QuestionGenerationRequestBuilder` or delegate export construction through an existing service.
- Keep the response body identical.
- Avoid touching generated API files unless a controller signature changes, which should not be necessary.

Tests:

- Existing export-question-generation controller tests should pass unchanged.
- If current tests do not pin the exported body enough, add one assertion at the controller boundary rather than testing builder internals again.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

## Open Questions

- Should `choicesMayBeShuffled = false` be preserved on the returned `MCQWithAnswer`, or should post-processed questions always normalize this field to the actual behavior? The safest initial choice is to preserve it unless shuffling happens.
- Should manually added questions ever be shuffled? Initial answer: no. Manual entry is not AI-generated output and should remain deterministic.
- Should old already-imported batch questions be backfilled? Initial answer: no unless there is a user-visible data cleanup requirement; the bug fix should apply to future imports.
