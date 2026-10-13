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
| The scheduling state held before the excluded attempt is kept, including state no recall log records | 10: controller tests comparing persisted state with the pre-attempt state and an independently graded tracker |

Meaning-specific misconception feedback (the driving example) is tutor
judgement under the slice 5 instruction; it has no automated proof, as the
seed's observation provenance already states.

## Ordered slices

### 1. Exclude an answered attempt as ambiguous through the recall-prompt API
Type: Behavior
Status: done
Accepted proof (2026-10-13): `CURSOR_DEV=true nix develop -c pnpm backend:verify`
→ BUILD SUCCESSFUL, 2786 tests, 0 failures; the five tests of
`RecallPromptExcludeAsAmbiguousControllerTest` observe each promise below
through `RecallPromptController.excludeAsAmbiguous`. The decision lives in
`MemoryTrackerService.excludeAsAmbiguous`; an unanswered prompt is refused with
`QuestionAnswerException`.
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
Status: done
Accepted proof (2026-10-13): `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
→ BUILD SUCCESSFUL, 2788 tests, 0 failures;
`MemoryTrackerRebuildFromRecallHistoryTest` observes both promises below.
`RecallLog.recordStateBefore(tracker)` is the only writer of the cached
columns and reads the log's own `recordedAt`; `MemoryTracker.rebuildFrom`
delegates to `MemoryTrackerReplay`.
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
Status: done
Accepted proof (2026-10-13): `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
→ BUILD SUCCESSFUL, 2791 tests, 0 failures. In
`RecallPromptExcludeAsAmbiguousControllerTest`:
`anAttemptWithNoOtherHistoryLeavesTheSameTrackerNewAndDueAtAssimilation` (a),
`aLaterGradeIsRescheduledAsIfTheExcludedAttemptHadNotHappened` (b, against an
in-memory tracker graded GOOD once), and
`repeatingChangesNeitherTheScheduleNorTheHistory` (c), all read from persisted
state. The valid-event rule is `RecallLog.isValidEvent()`.
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
Status: done
Accepted proof (2026-10-13): `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
→ BUILD SUCCESSFUL, 2796 tests, 0 failures; after test-helper refactoring the
`RecallPrompt*`, `MemoryTracker*`, and `UserRecallStatsControllerTest`
controller classes reran green (186 tests). Observations:
`MemoryTrackerShowControllerTest` `recallCountOmitsAnExcludedAttemptAndKeepsOrdinaryRecalls`,
`MemoryTrackerThresholdControllerTest.anExcludedAgainDoesNotCountTowardThreshold`,
`RecallStatsExcludedAttemptIsNotAReviewTest`, and
`UserRecallStatsControllerTest.anAttemptExcludedAsAmbiguousIsNotAReview`
(the query projects the exclusion).
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
Status: done
Accepted proof (2026-10-13): `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
→ BUILD SUCCESSFUL, 2799 tests, 0 failures; frontend `vue-tsc --noEmit` and
`tsc --noEmit` exit 0 against the regenerated types; after refactoring, the
`RecallConversation*`, `RecallPromptExcludeAsAmbiguous*`, `MemoryTracker*`,
`RecallStats*`, and `RecallPromptAnswer*` classes reran green (312 tests).
Observations in `RecallConversationExclusionProposalControllerTest`:
`offersTheProposalToolForAnMcqAttemptAndSaysOnlyTheLearnerConfirms`,
`doesNotOfferTheProposalToolInANoteConversation`, and
`tellsTheTutorThatTheAttemptIsAlreadyExcludedAndWhy`. The tool is added for
any recall-prompt subject: `ConversationMessageController.getAiReply` already
refuses a subject without an MCQ before the request is built. The attempt
record carries `attemptExcludedAsAmbiguous: {reason}`.
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
Status: done
Accepted proof (2026-10-13): `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall tests/pages/MemoryTrackerPageView.history.spec.ts tests/pages/MemoryTrackerPageView.privateHelp.spec.ts tests/pages/RecallPage.privateHelp.spec.ts tests/pages/RecallPage.practice.spec.ts tests/pages/RecallPage.answering.spec.ts tests/pages/MessageCenterPage.subject.spec.ts tests/components/conversation`
→ 53 files, 241 tests passed; `vue-tsc --noEmit` and `tsc --noEmit` exit 0.
Observations: `QuestionDisplay.spec.ts` "an attempt excluded as an ambiguous
question" (three tests) and `MemoryTrackerPageView.history.spec.ts` "shows an
excluded attempt as excluded beside its AGAIN log". The rule lives in
`QuestionDisplay.vue` (`data-test="excluded-attempt"`); `QuestionChoices.vue`
drops the wrong-answer colour through its `excluded` prop.
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
Status: done
Accepted proof (2026-10-13): `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/conversation`
→ 9 files, 41 tests passed, including the three tests of
`AiResponse.exclusionProposal.spec.ts`; before refactoring the same change
passed `tests/models` and the private-help, practice, and message-center page
specs (23 files, 123 tests); `vue-tsc --noEmit` and `tsc --noEmit` exit 0.
The suggestion shows the title below with the reason in `.exclusion-reason`
and the buttons "Confirm" and "Decline", with no Skip.
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
Status: done
Accepted proof (2026-10-13): `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/conversation tests/components/recall tests/pages/RecallPage tests/pages/MemoryTrackerPage tests/pages/MessageCenterPage tests/pages/NoteShowPageConversation.spec.ts`
→ 67 files, 318 tests passed; after test-support refactoring
`tests/components/conversation` with the two private-help page specs reran
green (12 files, 47 tests); `vue-tsc --noEmit` and `tsc --noEmit` exit 0.
Observations, each starting from a plain wrong attempt:
`ConversationComponent.exclusion.spec.ts`, `RecallPage.privateHelp.spec.ts`
"shows the answered attempt as excluded once the learner confirms the tutor's
proposal in private help", and `MemoryTrackerPage.privateHelp.spec.ts`. The
result travels as one `attempt-excluded` event up the emit chain.
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
Status: done
Accepted proof (2026-10-13): `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/recall/ambiguous_question_exclusion.feature,e2e_test/features/ai_generated_content/note_content_completion.feature,e2e_test/features/messages/conversation_about_a_note.feature`
→ 4 of 4 scenarios passing; the `isolated-cypress*` and `e2e-runner` script
tests → 104 pass. The journey observes the proposal and its reason, Confirm,
the page's excluded notice, the history item excluded beside `AGAIN`, "Next
Recall Time" equal to "Assimilated Time", and the excluded notice in reopened
help. The reopen is an in-app visit to the memory tracker page and its help.
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
overrun rule. Stop-safe: the journey and its documentation are complete.

### 10. Exclusion keeps the state the tracker held before the excluded attempt
Type: Behavior
Status: planned
Proof: controller tests on the exclusion operation in
`RecallPromptExcludeAsAmbiguousControllerTest`: (a) a tracker whose stability,
difficulty, and last recall were set without recall logs, then AGAIN at t1,
then exclude → stability, difficulty, `lastRecalledAt`, and `nextRecallAt`
equal what the tracker held before t1, read from persisted state; (b) the
same tracker with AGAIN at t1 and GOOD at t2, then exclude t1 → the state
equals an independent tracker that started from the pre-t1 state and received
only GOOD at t2, and the t2 log's cached columns equal that tracker's; (c) the
existing tests for a New tracker, a later Grade, and repeating stay green;
(d) an excluded attempt whose log has no cached state rebuilds from a New
tracker over all valid logs. `docs/recall-help.md` describes the rule. The
E2E journey stays green.

Behavior: the attempt is excluded → replay starts from the memory state
cached on that attempt's own recall log (`stabilityBefore`,
`difficultyBefore`, and the last recall it implies) and applies the valid
events recorded after it, at their original times → state earned before the
attempt, logged or not, is kept.

Give `MemoryTracker.rebuildFrom` (or `MemoryTrackerReplay`) the starting
state instead of always resetting to New; the last recall before the attempt
is the latest earlier valid Grade log's time, or the log's time minus its
`elapsedHours` when no such log exists, or none for a tracker that was New;
the due time before the attempt follows from that state through
`calculateNextRecallAt`. Keep one replay owner and the one valid-event rule.

Sizing: about 8 minutes, scrutinized: one rule change with its proof and one
documentation paragraph. Stop-safe: the story is complete.

## Current decisions and delivery

- Exclusion lives on the attempt (`answer`), the RecallLog row stays, and one
  "valid scheduling event" rule serves replay and every readout. No RecallLog
  flag, no second versioning, no undo.
- Owner decision (2026-10-13): replay starts from the memory state cached on
  the excluded attempt's own recall log and applies the later valid events;
  a log without cached state keeps the rebuild from a New tracker. ADR 0003
  calls the cached columns a cache and "not a new source of truth"; whether
  the ADR needs a note for this use is the owner's to decide.
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

## Execution

Story Branch Mode in
`/Users/terryyin/git/doughnut/.worktrees/resolve-a-disputed-wrong-mark-after-the-learner`
on `claude/resolve-a-disputed-wrong-mark-after-the-learner`, publishing to
`origin` (trunk `main`). The claim `d3343096807098127dd596c0596b9ad66ae89c4e`
was accepted on `main`.

## Learnings

- Controller tests for this operation reuse `recallPrompt.requireMemoryTracker()`:
  a second tracker for the same user and note violates
  `uq_memory_tracker_user_note_type_property`, and clearing the persistence
  context in setup detaches the current user.
- A migration slice proves with `pnpm backend:verify` (test-DB migration plus
  the whole suite) in place of `backend:test_only`.
- `MemoryTracker.rebuildFrom` trusts the caller's order and realigns the passed
  logs in place, so the caller supplies ascending `recordedAt` (id as the
  tie-break) and saves tracker and logs. The existing ordered query,
  `findAllByMemoryTracker_IdOrderByRecordedAtDescIdDesc`, is descending.
  `RecallLog.getAnswer()` reaches the attempt and its `excludedAt`.
- `MemoryTracker.java` is at 247 of 250 lines; additions there need room.
- Persisted `FLOAT` stability and difficulty come back at about six
  significant digits; tests compare them to in-memory values with a tolerance.
- Observation behind the 2026-10-13 owner decision: a replay from New keeps
  only state that RecallLog holds. RecallLog writing began 2026-08-17 (`0843cbd3ee`, with backfills of
  prompt answers and Tutor scores in `6ddf35275e` and `59e3e4a282`), so a
  tracker whose stability was earned through earlier unlogged grades would be
  rebuilt from a partial history when one of its attempts is excluded. ADR 0003
  states the state "can be rebuilt from RecallLog"; whether production
  trackers satisfy that is unobserved. Slice 10 owns obligation G4.
- `scripts/isolated-cypress-openai-mock.test.mjs` asserts the size of the
  OpenAI-mock feature inventory and no package script runs it.
- The recall page holds more than one `dialog` while help is open; E2E page
  objects select the help dialog by its title.
- The stats query's end bound is exclusive, so an attempt answered at exactly
  the current clock enters stats only after the clock advances.
- `ControllerTestBase` holds the shared request builders `choiceAnswer` and
  `ambiguousExclusion`; `MemoryTrackerControllerTestBase.answerWronglyThenExcludeAsAmbiguous`
  produces an excluded attempt through the real operations.
- `Answer.isExcludedAsAmbiguous()` is the Java question for the exclusion fact;
  `RecallLog.isValidEvent()` and the tutor's attempt record read it.
- The frontend type is `AmbiguousQuestionExclusionProposal` (`{ reason: string }`)
  in the generated API types; the function name on the wire is the same.
- `ConversationMessageControllerTestBase.lastRequestSentToAi()` captures the
  request sent to the mocked AI client.
- `makeMe.anAnsweredQuestion.excludedAsAmbiguous(reason, excludedAtIso)` builds
  the excluded shape; mounting with `attachTo: document.body` gives real
  computed styles.
- `AnsweredQuestionPage.expectMCQAnswerToBeIncorrect` asserts classes that an
  excluded attempt keeps, so the E2E journey needs its own excluded assertion.
- The confirmation's returned `AnsweredQuestion` is the `data` of the
  `apiCallWithLoading` call in the `exclusion` case of `handleAccept` in
  `ToolCallHandler.vue`; the path upward is `ToolCallHandler` → `AiResponse` →
  `ConversationInner` → `ConversationComponent` → `ConversationButton`.
- The suggestion flow reports nothing back to the tutor within a stream; a
  later request sees a confirmed exclusion through the attempt record.
- After Confirm the help dialog stays open, and the page behind it shows its
  own `[data-test="excluded-attempt"]` notice; the dialog is teleported to
  `body`, so an E2E selector scopes to the dialog or to the page.
- `confirmTutorExclusionProposal` in `aiResponseTestSupport.ts` drives the
  proposal and the confirmation in mounted specs.

## Story obligations

### G1. Exclusion recorded without schedule recomputation
Reported: slice 1 — "the exclusion is recorded without schedule recomputation or readout changes (slices 3 and 4), as the plan states."
Story clause: "Recompute the existing tracker's current scheduling state from the remaining valid history, using the original event times and the current FSRS policy."
Disposition: proved by slice 3: `RecallPromptExcludeAsAmbiguousControllerTest` tests `anAttemptWithNoOtherHistoryLeavesTheSameTrackerNewAndDueAtAssimilation` and `aLaterGradeIsRescheduledAsIfTheExcludedAttemptHadNotHappened`

### G2. Excluded attempt still counted in readouts
Reported: slice 1 — "the exclusion is recorded without schedule recomputation or readout changes (slices 3 and 4), as the plan states."
Story clause: "Exclude that invalid attempt from evaluation and derived recall readouts."
Disposition: proved by slice 4: `MemoryTrackerShowControllerTest` `recallCountOmitsAnExcludedAttemptAndKeepsOrdinaryRecalls`, `MemoryTrackerThresholdControllerTest.anExcludedAgainDoesNotCountTowardThreshold`, `UserRecallStatsControllerTest.anAttemptExcludedAsAmbiguousIsNotAReview`

### G3. Route, request body, and serialized fields unobserved over HTTP
Reported: slice 1 — "HTTP request binding of the `{reason}` JSON body and the route string: the controller is called directly, as in the neighbouring tests."
Story clause: "The learner confirms → the attempt reads “Excluded — ambiguous question,” is not counted as failed or successful recall"
Disposition: proved by slice 9: `ambiguous_question_exclusion.feature`, where the Confirm click sends the real request and the reason and original mark are read back from the recall page and the memory tracker page

### G4. Replay discards tracker state that RecallLog does not hold
Reported: slice 3 — "A tracker whose current stability did not come from RecallLog rows (for example test fixtures built with `stabilityAndNextRecallAt(200)` and no logs) becomes New on exclusion."
Story clause: "Confirmation is not a new Grade, does not award GOOD, and does not reset or recreate the tracker."
Disposition: receiving slice 10

### G5. An excluded attempt still counts as recall activity
Reported: slice 4 — "an excluded attempt still counts as recall activity for the silent-window due time. I treated that as activity, not evaluation, and did not change it."
Story clause: "Exclude that invalid attempt from evaluation and derived recall readouts."
Disposition: no user cost "the attempt is visibly excluded from evaluation": the learner did answer at that time, and the activity readers (`findAnsweredRecallPromptsInTimeRange`, `findUserIdsWithAnsweredRecallsInTimeRange`) use only answer times, never correctness or Grades

### G6. The proposal shows as an unknown suggestion
Reported: slice 5 — "Until slice 7 the proposal shows as an \"unknown\" suggestion in the UI."
Story clause: "Let the learner review and confirm or decline the proposal."
Disposition: proved by slice 7: `AiResponse.exclusionProposal.spec.ts` tests "shows the proposed correction with its reason", "excludes the attempt when the learner confirms", and "leaves the attempt as it was when the learner declines"

### G7. Tutor judgement is unmeasured
Reported: slice 5 — "Tutor judgement is unmeasured: whether the model proposes only for genuine ambiguity, emits a single call, or respects \"do not propose again\" rests on wording alone."
Story clause: "This is agent-simulated learner evidence, not measured human comprehension, retention, or representative model reliability."
Disposition: no user cost "private recall help identifies an ambiguous question": a proposal changes nothing until the learner confirms it, so an unsuitable proposal costs the learner one declined suggestion

### G8. Spelling attempts have no excluded reading
Reported: slice 6 — "`RecallHistory.vue` spelling branch: shows a \"Result: Incorrect/Correct\" badge with no excluded reading."
Story clause: "Start from one answered MCQ and its private help conversation."
Disposition: excluded "Shared-MCQ adjudication, regenerating or editing questions, undoing a confirmed correction, correcting several attempts together, spelling disputes, and authoring note changes are deferred promises."

### G9. Recall page and help panel unobserved for the excluded shape
Reported: slice 6 — "Recall page (`AnsweredQuestionComponent`): not observed directly for the excluded shape."
Story clause: "Reopening the attempt or its help shows the same decision and explains its actual consequences."
Disposition: proved by slice 8: `ConversationComponent.exclusion.spec.ts` (help subject panel) and `RecallPage.privateHelp.spec.ts` "shows the answered attempt as excluded once the learner confirms the tutor's proposal in private help"

### G10. The notice does not link to the originating exchange
Reported: slice 6 — "It does not link to the originating exchange."
Story clause: "Retain the original question, selected answer, marked outcome and Grade, together with the correction's reason, confirming learner, time, and originating exchange."
Disposition: no user cost "the original evidence remains inspectable": the exchange is the attempt's persisted private help conversation, which the same answered question and history item already open through their help button

### G11. A history item without its prompt would show a bare AGAIN
Reported: slice 6 — "A log-only history item (no paired prompt) for an excluded answer would show bare AGAIN."
Story clause: "Reopening shows both the original mark and the learner-confirmed correction."
Disposition: proved by slice 9: `ambiguous_question_exclusion.feature` step "the recall history should show the "AGAIN" attempt as excluded" (`memoryTrackerRecallHistory.expectExcludedAttemptWithOutcome`)

### G12. A declined proposal leaves no record
Reported: slice 7 — "A decline leaves no persisted or displayed mark; it differs from an accepted correction only by the absence of the excluded state."
Story clause: "A declined proposal leaves the result and schedule intact and remains distinguishable from an accepted correction."
Disposition: no user cost "the attempt is visibly excluded from evaluation": after a decline the attempt keeps its original wrong mark and after a confirmation it reads as excluded, so the two outcomes read differently wherever the attempt is shown

### G13. A failed confirmation closes the proposal
Reported: slice 7 — "The learner cannot retry from the same proposal."
Story clause: "On confirmation, replace the unresolved wrong-mark presentation with an explicit excluded-as-ambiguous result."
Disposition: no user cost "After the learner confirms the proposed correction": a failed call changes nothing, shows the error, and leaves the wrong mark in place, as a failed title or completion suggestion does; the learner can ask the tutor again

### G14. Open surfaces refresh only on reopen
Reported: slice 7 — "after Confirm, the help subject panel, recall page and memory tracker page still show the old result until reopened."
Story clause: "On confirmation, replace the unresolved wrong-mark presentation with an explicit excluded-as-ambiguous result."
Disposition: proved by slice 8: `ConversationComponent.exclusion.spec.ts`, `RecallPage.privateHelp.spec.ts`, and `MemoryTrackerPage.privateHelp.spec.ts`

### G15. Reopening an already excluded attempt is unobserved in the browser
Reported: slice 8 — "Reopening an already-excluded attempt on the recall page / help panel from persisted data: untested here, left to slice 9."
Story clause: "Reopening the attempt or its help shows the same decision and explains its actual consequences."
Disposition: proved by slice 9: `ambiguous_question_exclusion.feature` step "private recall help for that attempt should still show it as excluded", after a fresh load of the memory tracker page

### G16. The open recall session keeps its due queue
Reported: slice 8 — "The recall page only replaces the displayed entry; it does not reload the due queue or progress counts after an exclusion."
Story clause: "Corrected due work can already be overdue."
Disposition: no user cost "its learning effects are corrected": the corrected due time is persisted when the learner confirms, and the recall page reads due work from the server on its next load, as it does after any other schedule change made outside the queue

### G17. The Message Center has no confirmation spec
Reported: slice 8 — "Message Center: untested. It updates by construction through `ConversationComponent`; no Message Center spec drives a confirmation."
Story clause: "Reopening the attempt or its help shows the same decision and explains its actual consequences."
Disposition: no user cost "the attempt is visibly excluded from evaluation": the Message Center renders the subject through the same `ConversationComponent` whose update `ConversationComponent.exclusion.spec.ts` observes
