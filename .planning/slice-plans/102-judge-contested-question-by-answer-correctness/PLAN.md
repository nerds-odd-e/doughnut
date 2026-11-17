# Judge a contested question by whether its answer is correct

**Identity:** SEED-073#judge-contested-question-by-answer-correctness
**Source:** [selected story](../../seeds/SEED-073-learning-refinement-and-retrieval.md#judge-contested-question-by-answer-correctness)
**Source review:** [source-review.md](source-review.md)

## Outcome and boundaries

A learner who contests a recall question gets a verdict on correctness alone.
The product asks the model only which choices the note supports and decides
itself: refused when the supported choices are exactly the stored answer,
accepted otherwise. An accepted contest still marks the question contested and
leads to a replacement question.

Included: the contest check, its verdict and its advice on the web and in the
CLI (both read the same contest result); deletion of the broader
question-quality evaluation and of checking and replacing a question while it
is generated, with the `question.regeneration.times` setting, their tests and
documentation; the sweep for what only they used; the amended **Contested**
entry in ADR 0001.

Excluded, as the story defers them: a way to replace a correct but poor
question; checking a question before it is first shown; checking stored
questions that are waiting. Kept unchanged: the contest button, its place and
label; replacing a question after an accepted contest; the learner-confirmed
ambiguity exclusion; the administrator's evaluation model setting, which note
conversations, note automation and book layout also use.

## Architecture and reuse

PFE: every part exists; the story narrows one and deletes two.

- `RecallPromptController.contest` → `RecallPromptService.contest` →
  `McqService.contest` is the contest entry and stays. `QuestionContestResult`
  (`advice`, `rejected`) is the API contract and does not change, so no API
  client regeneration is needed and the web and CLI callers keep working.
- `NoteQuestionGenerationService.evaluateQuestion` builds the check request
  through `QuestionGenerationRequestBuilder.openAiResponseRequestForQuestionEvaluation`:
  the notebook's question instructions, the focus context within the shared
  content budget, and the evaluation model. That request construction is
  reused; only the instruction and the response schema change.
- `QuestionEvaluation` holds the model's answer, the feasibility flag, and the
  verdict rule. It is replaced by a response that holds only the supported
  choice positions, with the verdict and advice decided from it and the stored
  answer in one place.
- `McqService.generateAFeasibleQuestion` holds the generation-time loop. It
  becomes plain generation; `RecallPromptService` is its only product caller.
- `AiToolFactory.getAllAssistantTools` offers `QuestionEvaluation` to note
  conversations. No conversation handler reads such a call (searched
  `frontend/src`, `cli/src` and the backend conversation services on
  2026-11-17 at `64e0eb3a3d`: no match), so the offer is deleted with the
  class.

Accepted decisions:

- [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md) defines
  **Contested** as "Marks an MCQ as not feasible". The ADR directs that domain
  terms are amended in place. Slice 3 amends the entry to: marks an MCQ whose
  stored answer a contest found wrong or not the only supported one.
  **Contest** keeps its meaning.
- [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md): the check
  adds no catch. A missing model response keeps today's result (no verdict).
- [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)
  is untouched: a contest happens before answering and changes no grade.

No conflict was found. No North Star topic governs this work and none is added.

No expected edit touches a file over the file-size check (250 lines). The
largest are `QuestionGenerationRequestBuilder.java` (240 lines, one method
rename at most), `MemoryTrackerRecallPromptControllerTest.java` (224 lines,
shrinks) and `AiToolFactory.java` (180 lines, shrinks).

## Current decisions

- The instruction is the one frozen in the comparison, word for word
  (SHA-256 `3591bc73411b7cf0eb14ae8f0e48b959d9c539f93bbbca8255913e9dc42944a5`
  of `integrity/pilot/candidate-prompt.txt` in the default checkout's private
  recall cache; hash confirmed on 2026-11-17). The implementer copies the
  `instructions` text and the `supportedChoices` schema description from that
  file. The request keeps low reasoning effort and 500 maximum output tokens,
  as today and as measured. The question is sent without its stored answer, as
  today (`Mcq.withoutSolution`).
- One request per contest. No majority of several runs.
- Verdict rule, decided by the product: refused when the supported positions
  are exactly the stored answer's position; accepted for every other set,
  including the empty set.
- Advice. Refused: "The note supports exactly one of these choices, so the
  question stands. Please answer it." It names no choice. Accepted: names the
  stored answer and the supported choices in quotes, or says the note supports
  none of the choices, and asks for a question with one correct answer. The
  accepted advice is what the replacement request receives.
- The replacement request no longer calls the earlier question "non-feasible".
  Its opening line becomes "Previously generated question, contested by the
  learner:". The end-to-end stubs that match that line change with it.
- Checking during generation is deleted before the check changes, so the
  check has one caller when its request and response change.
- Removal proof follows the repository rule: surviving behavior is proven by
  retained or replacement tests, and the absence of the removed names is one
  sweep reading recorded here at acceptance, not a test.

## Decisive premises and observations

| Premise | Consuming operation | Observation | Result |
| --- | --- | --- | --- |
| The backend tests that depend on checking during generation are known | Slice 1 backend suite | Scratch in this worktree at `64e0eb3a3d`, 2026-11-17: test `question.regeneration.times` set to 0 (the loop then never runs, the same as its deletion), `CURSOR_DEV=true nix develop -c pnpm backend:test_only`, reverted | 3198 tests, exactly 3 failed: `MemoryTrackerQuestionPersistenceControllerTest.persistsTheSelectedQuestionWithItsCurrentTracker`, `MemoryTrackerRecallPromptControllerTest…shouldGenerateMcqWithoutHoldingATransactionDuringContestAndRegenerateOpenAiCalls`, `McqTest…selectsRegeneratedQuestionWhenEvaluatorDisagrees`. Other tests that enqueue an evaluation for the loop stay green and only lose an unused response |
| A recall feature passes without the "evaluates the question as legitimate" step once no check runs during generation | Slice 2 end-to-end features | Scratch in this worktree at `64e0eb3a3d`, 2026-11-17: `e2e` profile `question.regeneration.times` set to 0, the step removed from `recall_quiz_ai_question.feature`, `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/recall/recall_quiz_ai_question.feature`, reverted | 1 passing. The other four features use the same step for the same single consumer (the generation loop); none of them contests, so the difference cannot change their outcome. Slice 2 runs all five |
| The frozen instruction is available to the implementer unchanged | Slice 3 instruction copy | `shasum -a 256` of the file in the default checkout, 2026-11-17 | Hash matches the evidence report |
| Production does not check during generation | Slice 1 has no production behavior change | Read `application-prod.yml` at `64e0eb3a3d` | `question.regeneration.times: 0` |

## Proof map

| Promise (story) | Slice | Observable proof |
| --- | --- | --- |
| A new recall question is shown as generated, with no check request | 1 | Recall prompt controller test: one generation response stubbed, the question returned is that one, and exactly one model request was made |
| Features that never contest need no evaluation stub | 2 | The five features pass with the step removed |
| Refused when exactly the stored answer is supported; question not marked contested; advice names no choice | 3 | Contest controller test |
| Accepted on a different single choice, on several choices, and on no choice; question marked contested; advice names stored and supported choices | 3 | Contest controller tests, one per example |
| Question quality does not affect the verdict | 3 | The response schema carries only supported positions; request-capture assertion that the instruction is the answer-support one |
| An accepted contest still leads to a replacement that receives the advice | 3 | `question_contest.feature` learner scenario; regenerate controller test |
| Check uses the evaluation model and the tracker's current note | 3 | Existing contest controller assertions, retained |
| **Contested** reads as the correctness meaning | 3 | ADR 0001 entry |

Sweep reading at acceptance (recorded here, not a test): a search of the
product for `QuestionEvaluation`, `feasibleQuestion`, `improvementAdvices`,
`regeneration.times`, `generateAFeasibleQuestion`, `non-feasible`,
`evaluating a memory recall question` and `evaluates the question as` returns
nothing outside `.planning/`.

## Slices

### 1. A new recall question is shown as generated

Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`; then
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/ai_generated_recall_questions/question_contest.feature`.

Behavior: a memory tracker is due and has no waiting question → the learner
opens recall → the generated question is shown, and the product made one
model request for it.

Work: `McqService` generates once (the loop, the `regenerationTimes` value and
its constructor parameter go); `question.regeneration.times` goes from
`application.yml`, `application-prod.yml` and the test `application.properties`.
Tests of the removed loop are deleted (the `AutoEvaluateAndRegenerate` cases
that exist only for it, the contest-and-regenerate transaction case in
`MemoryTrackerRecallPromptControllerTest`); tests of surviving behavior that
enqueue an evaluation for the loop drop that response and keep their own
promise (`MemoryTrackerQuestionPersistenceControllerTest`,
`RecallPromptControllerTestBase`, `McqTest`'s remaining cases). The premise table names the three tests that fail without the loop: the persistence test keeps its promise with one generation response, and the other two exist only for the loop. In
`question_contest.feature` the "Internally contested MCQs are replaced before
recall" outline and its "not legitimate" step go, and the learner scenario's
stub upholds the contest on the first evaluation.
`OpenAiTransactionalRatchetTest` lists only the `contest` and `regenerate`
endpoints, which stay.

### 2. Recall features state only what they use

Type: Structure
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec` for
`e2e_test/features/cli/cli_recall.feature`,
`e2e_test/features/note_topology/export_highlighted_property.feature`,
`e2e_test/features/recall/ambiguous_question_exclusion.feature`,
`e2e_test/features/recall/property_memory_tracker.feature` and
`e2e_test/features/recall/recall_quiz_ai_question.feature`.

Internal change: the step "OpenAI evaluates the question as legitimate" leaves
those five features (eight uses), with its step definition and the accepted
and rejected evaluation stubs in `questionGenerationService.ts`. External
behavior is unchanged. It enables slice 3: the check's request and response
then change with one stub to update.

### 3. A contest is decided by which choices the note supports

Type: Behavior
Status: planned
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`;
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/ai_generated_recall_questions/question_contest.feature`;
the sweep reading above.

Behavior, with stored answer at position 0:

- the model reports `[0]` → contest → refused, advice names no choice,
  question not contested;
- `[1]` → accepted, advice names the stored and the supported choice,
  question contested;
- `[0, 2]` → accepted, advice names both supported choices;
- `[]` → accepted, advice says the note supports none of the choices;
- after an acceptance → the replacement request carries the question and
  that advice, and the learner is asked the new question.

Work: the answer-support instruction and response replace the quality
instruction and `QuestionEvaluation` (class, test, tool offer in
`getAllAssistantTools`, the `evaluateQuestion` names); the verdict and advice
come from the supported positions and the stored answer. Contest tests are
rewritten to the examples above through `RecallPromptController.contest`;
`ListRecallAnswersMovedTrackerMcpEndpointTest` and
`AiOpenAiAssistantFactoryWithDBTest` stub the new response.
`RegenerateQuestionMessage` takes the new opening line, and the end-to-end
stubs match it and the new instruction. ADR 0001's **Contested** entry and
the test-class example in `docs/cloud_agent_backend_testing.md` are amended.
The CLI needs no change: it shows the returned advice.

## Learnings

- Slice 1: with the loop gone, `McqService.generateAFeasibleQuestion` was a
  pass-through; the refactor moved generation into
  `RecallPromptService.generateNewRecallPrompt` and deleted `McqTest`, whose
  remaining cases are proven through
  `MemoryTrackerQuestionPersistenceControllerTest.persistsTheSelectedQuestionWithItsCurrentTracker`
  (single generated question, context seed set, exactly one model request).
  `McqService` now holds only the contest.
- Slice 2: `cli_recall.feature` is `@ignore` (`@interactiveCLI`), and the
  isolated runner refuses it, so its two step removals are proven only by
  `pnpm -s test:gherkin-validation`; the other four features ran green
  (8 passing). After slice 2 the evaluation stubs left in
  `questionGenerationService.ts` serve only "OpenAI will uphold a contest".
- For slice 3: the four-argument `AiQuestionGenerator.regenerateQuestion`
  overload has no callers (unused before this story too); delete it with the
  contest check change.

## Concern evidence

Refinement: not needed. Three slices, each one outcome with one proof loop;
the two removals come before the change so each slice leaves the suite green.

Remaining slice-specific concerns: none identified in this review. Slice 3 is
the largest (one backend rule, its tests, one end-to-end stub, two document
lines); its parts share one proof loop, so it stays one slice.

Known limits carried from the story, not concerns for execution: the measured
detection rate used `gpt-6.1-sol` and the experiment's context, and the
verdict rule, not that rate, is what this plan proves.
