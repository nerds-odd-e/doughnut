# Learner-confirmed ambiguous-question exclusion

**Identity:** SEED-073#story-7
**Source:** [Resolve a disputed wrong mark after the learner confirms question ambiguity](../../seeds/SEED-073-learning-refinement-and-retrieval.md#story-7)

## Goal and scope

A learner whose MCQ answer was marked wrong resolves that result from private
recall help: the tutor proposes that the wording did not support a unique
answer, the learner confirms, and the attempt reads as excluded. The excluded
attempt leaves evaluation and derived readouts, the memory tracker's schedule
is rebuilt from the remaining valid history at the original event times, and
the original question, selected answer, mark, Grade, reason, and time stay
inspectable. Confirmation adds no Grade and never resets or recreates the
tracker.

Included: one new recall-prompt operation and its persisted facts on the
learner's attempt, schedule recomputation by replay, readout exclusion, the
tutor's proposal tool and instruction in private recall help, the confirm or
decline interaction, excluded presentation wherever the attempt is shown, one
mocked end-to-end journey, and the recall-help documentation.

Excluded (deferred promises from the seed, no runtime rejection added): shared
MCQ invalidation for other learners, regenerating or editing the question,
undoing a confirmed exclusion, excluding several attempts together, spelling
disputes, authored-note changes, and a formal contest verdict (story 6).

Assumptions: the private help conversation for a recall prompt is the only
surface that proposes exclusion, so the originating exchange is that
conversation (found today by prompt and initiator) and needs no extra column;
the confirming learner is the attempt owner that authorization already
enforces; the current FSRS profile is the replay policy.

## Existing owners and observed premises

Preparation checkout:
`/Users/terryyin/git/doughnut/.worktrees/resolve-a-disputed-wrong-mark-after-the-learner`,
base `e4ddaa13ab041c78e9b4dfddeeb7d89eb85358e9` (fetched `origin/main`), read
2026-10-13. Execution uses the checkout its own authorized start establishes;
these readings are reusable while the named files are unchanged.

- PFE: the attempt is `Answer` (owned through `RecallPrompt.answer`), its
  scheduling event is the `RecallLog` row linked by `answer_id`, and the
  tracker's current state lives on `MemoryTracker` (`applyGrade`,
  `adjustForConfusion`, `MemoryTrackerRecallDue`). ADR 0003 states that the
  current state "can be rebuilt from RecallLog" and that cached
  `stability_before`, `difficulty_before`, `retrievability` "must never diverge
  from what replay would produce"; no replay exists yet. Decision: record the
  exclusion on the attempt (`answer.excluded_at`, `answer.exclusion_reason`),
  keep the RecallLog row as evidence, and define one rule, "valid scheduling
  event = a RecallLog whose answer is absent or not excluded", used by replay,
  recall count, the frequent-failure count, and the stats projection. The
  RecallLog is not marked, because the exclusion is the learner's decision
  about the attempt, not a scheduling event.
- `MemoryTrackerService.persistRecallLog` is the one place that caches memory
  state before an event; `MemoryTracker.applyGrade(now, grade)` and
  `adjustForConfusion(now)` depend only on tracker state and the event time, so
  replaying the remaining logs from a New state at their `recordedAt` times
  reproduces incremental grading, whole-hour elapsed time, and the
  strictly-future fallback. `memory_tracker.assimilated_at` is `NOT NULL`
  (baseline DDL line 401), so a tracker with no remaining valid event is due at
  its assimilation time through `calculateNextRecallAt`.
- Authorization: `RecallPromptController` mutations call
  `assertReadAuthorization(memoryTracker)`, which is `assertAuthorizationUser`
  against the tracker's user (`AuthorizationService` lines 80–82): owner only.
  `MemoryTrackerThresholdControllerTest.shouldNotBeAbleToGetForOthersMemoryTracker`
  observes that refusal in the suite run below.
- Readout consumers of correctness and grades: `MemoryTracker.getRecallCount`
  (counts non-confusion logs), `RecallLogRepository.countAgainOutcomesSinceForMemoryTracker`
  (frequent-failure warning), `RecallPromptRepository.findAnsweredRecallAnswerRows`
  (JPQL constructor projection into `RecallAnswerRow`; its only other
  constructor callers are two sites in `RecallStatsTestFixtures`), and
  `Answer.getCorrect` (answer time, spelling grading, `RecallPrompt.getQuestionDetails`).
  `getCorrect` stays the original mark; exclusion is an added fact.
- Private recall help: `ConversationAiRequestBuilder` adds every class from
  `AiToolFactory.getAllAssistantTools()` as a tool and a recall-prompt
  developer instruction; `RecallPrompt.getQuestionDetails` is the attempt
  record the tutor reads. `ResponseStreamToLegacyChatChunkMapper` forwards any
  function name (lines 50, 95); `frontend/src/models/aiReplyState.ts`
  dispatches unknown names to an `unknown` suggestion, and
  `ToolCallHandler.vue` renders suggestions with accept, cancel, and skip.
  `ConversationComponent.vue` shows the subject through
  `conversation.subject.recallPrompt` (`AnsweredQuestion`); `ConversationButton.vue`
  opens the modal from `AnsweredQuestionComponent.vue` (recall page) and
  `RecallHistory.vue` (memory tracker page). `QuestionChoices.vue` colors the
  selected non-key choice red (`.is-selected:not(.is-correct)`).
- Tool-call frontend proof route: `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/conversation/AiResponse.titleAndUnknown.spec.ts`
  in this checkout, 2026-10-13: 7 passed in 4.9 s. `createToolCallChunk` and
  `submitMessageAndSimulateToolCall` in `aiResponseTestSupport.ts` drive a tool
  call by name.
- Backend proof route: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
  in this checkout, 2026-10-13: BUILD SUCCESSFUL in 59 s, 2781 tests, 0
  failures, 0 errors, 2 skipped (from `backend/build/test-results`).
  `RecallConversationReasoningControllerTest.sentInput` captures the request
  sent to the mocked `OpenAIClient`, so tools and instructions are observable.
- E2E proof route: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/ai_generated_content/note_content_completion.feature`
  in this checkout, 2026-10-13: 2 passing, all specs passed. The mock composer
  `e2e_test/start/mock_services/openAiMessageComposer.ts` hard-codes the
  function name `NoteContentCompletion` for `requires action` rows, and
  `scripts/isolated-cypress-spec-selection.mjs` admits OpenAI-mock features
  through `ACTIVE_OPEN_AI_MOCK_SPECS`; slice 9 changes both. No E2E or page
  object exercises private recall help today.
- Migration: `V300000352__add_property_value_to_trackers_and_property_index.sql`
  is the highest file; the next version is `V300000353`.
- Architecture: ADR 0003 governs Grade transitions, elapsed-time semantics, and
  cached readouts; this plan rebuilds state by replaying RecallLog under the
  frozen profile and realigns cached columns, which the ADR already describes.
  ADR 0002 keeps private learning data server-side; the exclusion is private
  learning data. No North Star topic governs recall; none is added.

Premises outside these observations: tutor judgement quality (whether the
model proposes exclusion only for genuinely ambiguous wording) is model
behavior the seed leaves unmeasured; slice 5 owns the instruction wording and
slice 9 proves the journey with a mocked proposal.

## Outside-in proof

| Promise | Owning slice and observation |
| --- | --- |
| An answered MCQ attempt can be excluded as ambiguous by its owner only; repeating gives the same decision; another learner's attempt on the same MCQ is untouched | 1: controller test on the new recall-prompt operation |
| The exclusion keeps the original mark and Grade and records reason and time | 1 (API shape), 6 (presentation), 9 (journey) |
| Replay of RecallLog reproduces incremental scheduling, including Confusion, whole-hour elapsed time, and cached columns | 2: entity-level replay test against incremental application |
| Confirmation rebuilds the schedule from the remaining valid history at original times, preserves a later Grade, realigns later cached columns, and leaves a tracker with no valid event due at assimilation | 3: controller tests comparing persisted state with an independently graded tracker |
| Confirmation adds no RecallLog, awards no GOOD, keeps the same tracker; repeating adds nothing | 3: same tests assert log count and tracker identity |
| The excluded attempt leaves recall count, the frequent-failure count, and recall stats | 4: controller tests for show and threshold-exceeded, stats aggregation test |
| The tutor can propose exclusion for an MCQ attempt, identifying that attempt, and not for a note conversation; an already excluded attempt is presented as resolved to the tutor | 5: request-capture test on tools, instruction, and attempt context |
| The attempt reads "Excluded — ambiguous question" with its original mark wherever it is shown | 6: QuestionDisplay and RecallHistory specs |
| The learner confirms or declines in help; only confirmation calls the operation | 7: AiResponse spec for the proposal suggestion |
| After confirmation the help subject, the recall page entry, and the memory tracker page reflect the exclusion without reopening | 8: ConversationComponent spec; 9 for the pages |
| Wrong answer → help → proposal → confirm → excluded label, corrected next recall time, recall history shows the decision | 9: mocked-OpenAI E2E scenario |
| Declined proposal leaves result and schedule intact | 7 (no call), 5 (instruction: never imply a correction happened) |

Meaning-specific misconception feedback (the driving example) is tutor
judgement under the slice 5 instruction; it has no automated proof, as the
seed's observation provenance already states.

## Ordered slices

### 1. Exclude an answered attempt as ambiguous through the recall-prompt API
Type: Behavior
Status: planned
Proof: `RecallPromptExcludeAsAmbiguousControllerTest`: an owner excludes an
answered MCQ prompt with a reason → the returned `AnsweredQuestion.answer`
carries `excludedAt`, `exclusionReason`, and the original `correct=false`;
repeating returns the first decision unchanged; another user's attempt on the
same `Mcq` is untouched; a non-owner is refused; an unanswered prompt is
refused.

Behavior: a learner owns an answered recall prompt → `POST
/api/recall-prompts/{recallPrompt}/exclude-as-ambiguous` with `{reason}` →
the attempt records the exclusion time and reason and the response is the
updated `AnsweredQuestion`.

Add migration `V300000353` with nullable `answer.excluded_at` and
`answer.exclusion_reason` (TEXT). Add the fields to `Answer` (serialized like
its other fields), the operation to `RecallPromptController` with the existing
`assertCanMutateRecallPrompt`, and the decision in `MemoryTrackerService` (the
owner of attempt grading) or `RecallPromptService`, whichever keeps one owner
of attempt outcomes. Schedule rebuild comes in slice 3; until then the
exclusion is recorded without recomputation, an interim state slice 3 replaces.
Run `pnpm generateTypeScript` at this slice's wrap-up.

Sizing: about 8 minutes, scrutinized: migration, entity, endpoint, and tests
form one API contract. Stop-safe: the API is usable by clients; nothing else
reads the new fields yet.

### 2. Rebuild a memory tracker's state by replaying its recall history
Type: Structure
Status: planned
Proof: an entity-level test in `backend/src/test/java/com/odde/donut/entities`
builds a tracker, applies AGAIN, GOOD, Confusion, and HARD incrementally at
chosen hours (persisting logs as `MemoryTrackerService.persistRecallLog`
does), then rebuilds a second tracker from those logs → stability,
difficulty, `lastRecalledAt`, `nextRecallAt`, and every log's `elapsedHours`,
`stabilityBefore`, `difficultyBefore`, and `retrievability` are equal. A
tracker rebuilt from no logs is New and due at its assimilation time.
Existing scheduling tests stay green.

Internal change: move the "cache memory state before an event" write from
`MemoryTrackerService.persistRecallLog` into one owner on the entity side
(for example `RecallLog.recordStateBefore(tracker, recordedAt)`), and add
`MemoryTracker.rebuildFrom(List<RecallLog> chronological)` that resets to New
(stability `NEW_STABILITY_HOURS`, null difficulty and last recall, due at
assimilation) and replays each log at its `recordedAt`: `applyGrade` for a
Grade, `adjustForConfusion` for Confusion, caching state before each. External
behavior is unchanged; this enables slice 3.

Sizing: about 6 minutes. Stop-safe: no caller yet.

### 3. Confirmation recomputes the schedule from the remaining valid history
Type: Behavior
Status: planned
Proof: controller tests on the slice 1 operation: (a) AGAIN at t1 only, then
exclude → the tracker is New and due at its assimilation time, the AGAIN log
still exists, the log count is unchanged, and the tracker id is the same;
(b) AGAIN at t1 then GOOD at t2 (via `mark-as-recalled`), then exclude the
t1 attempt → stability, difficulty, `lastRecalledAt`, and `nextRecallAt`
equal those of an independent tracker that received only GOOD at t2 from New,
and the t2 log's cached columns and elapsed hours equal that tracker's;
(c) excluding twice leaves state and logs identical.

Behavior: the attempt is excluded → the tracker's current state is rebuilt
from its RecallLog rows whose answer is absent or not excluded, in
`recordedAt` order, under the current FSRS policy → due work and the memory
tracker page read the corrected schedule, which may already be overdue.

Apply the one valid-event rule in the service that performs the exclusion,
using slice 2's rebuild and persisting tracker and realigned logs in the same
transaction.

Sizing: about 6 minutes. Stop-safe: the exclusion now has its full learning
effect.

### 4. Excluded attempts leave evaluation readouts
Type: Behavior
Status: planned
Proof: `MemoryTrackerShowControllerTest` (or the recall-history test base):
`recallCount` omits a log whose answer is excluded;
`MemoryTrackerThresholdControllerTest`: an excluded AGAIN does not count toward
the frequent-failure warning; `RecallStatsService` aggregation test: an
answered row with an exclusion is not a review (not in retention, accuracy,
or calibration), while an ordinary row still is. The two
`RecallStatsTestFixtures` constructor sites gain the new field.

Behavior: an excluded attempt exists → recall count, the frequent-failure
count, and recall stats are computed as if that attempt had not been
evaluated, while the RecallLog row remains in history.

Extend `RecallAnswerRow` with the exclusion fact selected in
`findAnsweredRecallAnswerRows`, and `countsAsReview` with the same rule; add
the rule to `getRecallCount` and the AGAIN count query. Reuse the slice 3 rule;
do not add a second definition.

Sizing: about 6 minutes. Stop-safe: readouts agree with the corrected
history.

### 5. The tutor can propose excluding an ambiguous MCQ attempt in private help
Type: Behavior
Status: planned
Proof: a request-capture test beside `RecallConversationReasoningControllerTest`:
for an MCQ recall-prompt conversation the sent `ResponseCreateParams` tools
include `AmbiguousQuestionExclusionProposal`, the recall-prompt developer
instruction names when to propose it and that a proposal changes nothing until
the learner confirms; for a note conversation the tool is absent; for an
already excluded attempt the attempt context JSON carries the exclusion
(`attemptExcludedAsAmbiguous` with the reason). The existing reasoning tests
stay green.

Behavior: the learner discusses a marked-wrong MCQ attempt in private help →
the assistant may emit one `AmbiguousQuestionExclusionProposal` tool call
whose `reason` explains the defensible interpretation of that attempt → the
proposal reaches the learner as a tool call; a tutor statement alone changes
nothing.

Add the tool class in `services/ai` with a `reason` property and a class
description that restricts it to wording that does not support a unique
answer, offered only when the conversation subject has a recall prompt with an
MCQ (not spelling, not note chats). Extend the recall-prompt instruction:
propose only for genuine ambiguity, never for meaning-specific misconceptions
such as a grammatical but wrong-meaning option; say that confirmation is the
learner's and that nothing has changed until then; do not propose again for an
excluded attempt. Add the exclusion to `RecallPrompt.getQuestionDetails` and
the tool type to `DummyForGeneratingTypes`; run `pnpm generateTypeScript` at
wrap-up.

Sizing: about 5 minutes. Stop-safe: the proposal arrives as an unknown
suggestion in the current UI until slice 7, an interim state slice 7
replaces.

### 6. An excluded attempt reads as excluded wherever it is shown
Type: Behavior
Status: planned
Proof: `QuestionDisplay` spec: an answer with `excludedAt` and
`exclusionReason` renders "Excluded — ambiguous question", the reason, the
time, and "originally marked incorrect", with the selected choice not styled
as incorrect and the key still visible; a `RecallHistory` spec: the history
item for that prompt shows the same label beside the AGAIN log. Existing
answered-question specs stay green. Typecheck per the `frontend` skill.

Behavior: an `AnsweredQuestion` or `RecallHistoryItem` whose answer is
excluded is displayed → the result reads as excluded with its retained
evidence instead of as a wrong mark, on the recall page, in the help subject
panel, and on the memory tracker page.

Keep the presentation rule in one place (`QuestionDisplay` or a small
answered-result component that `RecallHistory` reuses); extend the `makeMe`
answered-question fixture for the excluded shape.

Sizing: about 5 minutes. Stop-safe: data written by slice 1 is readable.

### 7. The learner confirms or declines the proposal in private help
Type: Behavior
Status: planned
Proof: `AiResponse.exclusionProposal.spec.ts` beside the title and completion
specs: a mounted help conversation for a recall prompt receives an
`AmbiguousQuestionExclusionProposal` tool call → the suggestion shows the
reason and a confirm action; confirming calls
`RecallPromptController.excludeAsAmbiguous` with that prompt id and reason
and the suggestion closes; declining calls nothing and closes.

Behavior: the tutor's proposal arrives in the open help conversation → the
learner confirms → the slice 1 operation runs for that attempt; the learner
declines → the result and schedule stay as they were.

Map the function name in `aiReplyState.ts` to an `exclusion` suggestion;
render and act on it in `ToolCallHandler.vue`, which needs the recall prompt id
from `AiResponse.vue` (the conversation's subject). Wording: "Proposed
correction: exclude this attempt as an ambiguous question".

Sizing: about 6 minutes. Stop-safe: confirmation works; open surfaces refresh
only on reopen until slice 8, an interim state slice 8 replaces.

### 8. Confirmation updates the open surfaces
Type: Behavior
Status: planned
Proof: `ConversationComponent` spec with a recall-prompt subject: after the
proposal is confirmed the subject panel shows the excluded result without
refetching the conversation. The recall page entry and the memory tracker page
are proved in slice 9.

Behavior: the learner confirms in help → the help subject panel, the recall
page's answered question for that attempt, and the memory tracker page's
history and schedule show the excluded result and corrected due time without
reopening.

Carry the updated `AnsweredQuestion` from the confirmation to its owners with
the smallest path: one event from `ConversationButton` upward
(`attempt-excluded`), with the modal's inner components passing the result
through provide/inject or a short emit chain; the recall page replaces the
entry at its cursor; the memory tracker page reloads tracker and history.

Sizing: about 6 minutes. Stop-safe: all surfaces coherent.

### 9. The whole journey runs end to end, and recall help documents it
Type: Behavior
Status: planned
Proof: new `e2e_test/features/recall/ambiguous_question_exclusion.feature`
(mocked OpenAI): assimilated note, generated question "Choose the
grammatically correct completion: I ___ in Tokyo" with key "used to live",
the learner answers "am used to living" on day 2, opens private help, asks
"Why is my answer wrong?", the mocked tutor replies with an
`AmbiguousQuestionExclusionProposal`, the learner confirms → the answered
question reads "Excluded — ambiguous question"; viewing the memory tracker
shows that history item excluded and the next recall time equal to the
assimilation time. Run with `pnpm cy:run --spec` on that feature.

Behavior: the seed's Tokyo example from answer to corrected schedule, through
the real browser, backend, and mocked tutor.

Extend `openAiMessageComposer.buildResponsesStreamEvent` and the
`OpenAI will reply below for user messages` rows with an optional tool name
(default `NoteContentCompletion`, keeping the isolation-proof rewrite to that
tool only); add a private-help page object (open help, send a message, confirm
the proposal); register the feature in `ACTIVE_OPEN_AI_MOCK_SPECS`. Update
`docs/recall-help.md` with a "Learner-confirmed ambiguity exclusion" section:
proposal, confirmation authority, retained evidence, replay under ADR 0003,
readouts, and deferrals.

Sizing: about 10 minutes, scrutinized: mock, page object, scenario, and
documentation are one journey proof; splitting leaves either an unprovable
scenario or an undocumented behavior. At 10 minutes apply the repository
overrun rule. Stop-safe: the story is complete.

## Current decisions and delivery

- Exclusion lives on the attempt (`answer`), the RecallLog row stays, and one
  "valid scheduling event" rule serves replay and every readout. No RecallLog
  flag, no second versioning, no undo.
- Replay follows ADR 0003: original event times, whole-hour elapsed time, the
  current FSRS profile, cached columns realigned to what replay produces.
  Confusion events are replayed in order because `adjustForConfusion` reads
  the tracker's due time at that moment.
- The proposal tool is offered only for MCQ recall-prompt conversations.
  Confirmation is the only path that calls the operation; the API itself
  accepts any owner call, as the seed's deferrals prescribe no rejection.
- Reopening the help conversation or the attempt shows the same decision from
  the persisted fields; no new event, message, or Grade is written on repeat.
- Follow `CLAUDE.md` execution wrap-up: Jidoka, a fresh
  `dough-post-change-refactor` agent, `pnpm generateTypeScript` after slices
  1 and 5 (and any later DTO change), coordinator
  `./scripts/run.sh pnpm format:changed` once, plan evidence update, commit
  with the check-only hook, publication through `dough-execute-plan`.
- Local proof per slice: the named backend suite run (`pnpm backend:test_only`,
  always the whole suite), the named frontend spec plus the `frontend` skill
  typechecks, and the single E2E feature in slice 9. Backend-only slices need
  no frontend run; frontend slices rerun the affected spec directory.
- Spelling attempts, other learners' prompts, and note content are untouched
  by design; no guard code is added for them.

## Learnings

None beyond the preparation observations above.
