# Retrieve my recall answer history through MCP

**Identity:** SEED-073#retrieve-my-recall-answer-history-through-mcp
**Source:** [refined story](../../seeds/SEED-073-learning-refinement-and-retrieval.md#retrieve-my-recall-answer-history-through-mcp)
**Source review:** [source-review.md](source-review.md)

## Goal and boundaries

A connected assistant can read the learner's own recent recall attempts through
one read-only MCP call: every answer in a rolling window (seven days unless the
caller gives another positive number of days), each with its time, question,
answer, recorded correctness and outcome, and source note evidence chosen from
correctness and question type. This is preparation only; all slices remain
unexecuted.

Outside this plan, as the story records: Just review and Tutor Feedback events,
filters and a compact index, attention readings and aggregates, any diagnosis,
and preserving attempts of hard-deleted notes. The tool changes no schedule,
tracker, or note. No new OAuth scope and no consent wording change.

## Current decisions and existing solutions

- One new tool, `list_recall_answers`, registered beside the existing tools
  with `notes:read` metadata and the read-only annotations. Arguments: optional
  `days` (positive number, default 7) and optional `cursor` (opaque, from the
  previous response). Result: `attempts`, the window's `from` and `to`
  instants, and `nextCursor` only when more remain.
- Attempts are ordered newest first by answer time, then answer ID. The first
  call fixes the window's end; the cursor carries that end and the last
  position, so answers recorded while paging neither repeat nor shift a page.
- Initial sizes, to be checked against the owner's real history after delivery:
  50 attempts per response, and a 1,000-token budget for each bounded graph.
  The story's measured volume (729 attempts and 72 misses in seven days) gives
  about fifteen responses for a default call.
- The read starts from answers linked to the learner's recall logs, not from
  recall prompts, so an answer whose prompt is gone is still an attempt. This
  follows the existing projection in `RecallLogRepository.findRecallEventRows`,
  bounded by answer time (`idx_answer_created_at` exists). The Confusion log
  an Accidental match writes on another tracker shares the answer and is
  folded into the same attempt.
- Reuse, do not duplicate: `Answer.getCorrect` and `Answer.correctFrom` for
  recorded correctness; the stored `Mcq` for stem, choices, expected answer and
  validation rationale; `RecallPrompt.getSpellingQuestion` for the masked
  prompt; `AttributedNoteContentService` for full content;
  `NoteGraphService` with `RetrievalConfig.forGraphApi` for the bounded graph;
  `PortablePathAuthoring` for the qualified Portable path; existing notebook
  read authorization for note evidence.
- One new service owns "recall answer history for a learner in a window"; the
  MCP tool is a thin projection of it, like the other tools. The web export
  (`RecallHistoryExportService`) keeps its own whole-history contract and is
  not changed.
- [SEED-125](../../seeds/SEED-125-read-notes-through-mcp-by-portable-path.md#read-notes-through-mcp-by-portable-path)
  is Taken and changes MCP addressing and the tool-list assertions in
  `McpEndpointTest`. Execute this plan on a trunk that contains it, and use
  its MCP address authoring. If it has not landed, this plan still starts:
  `PortablePathAuthoring` already exists; reconcile the tool-list test on
  rebase. No blocking dependency is recorded.
- Follows [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md) (answer,
  recall prompt, Accidental match, Overlap, Confusion),
  [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  (private learning data stays out of Portable content),
  [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  (Portable path), and
  [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) (unavailable
  evidence is a reported outcome). No [North Star](../../NORTH-STAR.md) topic
  governs this work and none is added.
- No file this plan expects to edit exceeds the 250-line check; the largest,
  `McpServerConfiguration.java`, has 142 lines.

## Outside-in proof ownership

All proof drives the real MCP HTTP endpoint (`McpEndpointTestBase`) with
database fixtures and current authorization; no service mocks. Attempts are
arranged so that their correctness is recorded the way the product records it
(see the first premise). One end-to-end journey proves the real answering path
feeds the tool.

| Promise | Owning slice | Observable proof |
| --- | --- | --- |
| Tool is listed, read-only, under `notes:read`; default window returns own multiple-choice attempts with full question, selected choice, expected answer, rationale, correctness, title and path | 2 | New `ListRecallAnswersMcpEndpointTest` cases; `McpEndpointTest` tool list; yesterday's answer present, eight-day-old absent, another learner's absent |
| Caller-chosen lookback; a non-positive or non-numeric value is refused with usable feedback | 3 | Ten-day-old miss appears with `days` 14; `0`, `-1`, and text are refused |
| Spelling attempts carry the typed answer, correctness and the prompt; an edited note reports the prompt unavailable | 4 | Spelling cases before and after a note edit |
| Overlap is correct with its outcome; Accidental match is one incorrect attempt | 5 | Overlap case; accidental-match case with a Confusion log on a second tracker yields one attempt |
| Ambiguity exclusion keeps recorded correctness and shows the reason | 6 | Excluded incorrect multiple-choice case |
| Evidence follows correctness and type: title and path; plus full content; plus bounded graph | 7 | Correct of both types, incorrect spelling, incorrect multiple choice; graph respects its budget |
| Lost access or trashed note keeps the attempt and reports note evidence unavailable; a missing question is reported unavailable | 8 | Withdrawn subscription, trashed note, and an answer without a prompt |
| A window larger than one response is retrievable to the end, each attempt once | 9 | More attempts than one response; follow `nextCursor` to the end; an answer added mid-way does not repeat or shift |
| A hosted assistant with `notes:read` can call it; a real wrong spelling answer reaches the assistant | 10 | `AssistantMcpAccessTest` case; one `mcp_services.feature` scenario |
| Tool and its use are documented where the tools are listed | 10 | `.agents/skills/mcp-server/SKILL.md` and `docs/assistant-oauth.md` read correctly |

Commands, from the execution checkout, with Nix:

- Every backend slice: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
  (the backend skill requires the full suite).
- Slice 10: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature,e2e_test/features/mcp/assistant_account_linking.feature`.
- No controller or shared DTO changes are planned, so no API generation. If
  execution changes one, regenerate and run the frontend checks the
  `generate-api-client` skill names.

## Decisive premises and preparation observations

Observed on 2026-11-12 in this preparation worktree at
`7c1b8d3e6c91c39f2e2aecbe03998cbe8f081cb2`. The scratch test was deleted before
planning writes; the product diff is empty.

| Premise | Consuming operation and observed form/profile | Result and limit |
| --- | --- | --- |
| A fixture-built answered prompt carries recorded correctness | Scratch test in the MCP test package: `makeMe.aRecallPrompt().forMemoryTracker(..).answerChoiceIndex(0).please()`, flush and clear, then `RecallPromptRepository.findAnsweredRecallPromptsInTimeRange` and `Answer.getCorrect`; `CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests 'com.odde.donut.mcp.*'`, this worktree's isolated test database | False: the windowed query found the prompt, and the reloaded answer's correctness was `null`. The builder sets only the transient flag and writes no recall log. Slice 2 arranges attempts through the product's answering path, or teaches the builder to record the grade; its sizing includes that. |
| MCP HTTP tests run in this checkout | Same command and profile | The 33 existing MCP tests passed (34 run, the scratch test the only failure by design). A focused run, not the full suite. |
| Full backend suite is green before this work | `CURSOR_DEV=true nix develop -c pnpm backend:test_only`, isolated `test` profile, recorded in [plan 092](../092-read-notes-through-mcp-by-portable-path/PLAN.md) at `b481551f17` on 2026-11-12 | Green: 3,118 tests. `git diff b481551f17 7c1b8d3e6c -- backend e2e_test` is empty, so the result carries over; it was taken in another worktree of the same content. Slice 2 re-establishes it in the execution checkout. |
| Answered prompts can lose their stored question | Owner's production history through `GET /api/user/recall-history-export` and `GET /api/memory-trackers/{id}/recall-prompts`, read-only with the owner's token | 2,189 answers, 24 in the last seven days, have no recall prompt; two such trackers answered on 2026-11-11 and 2026-11-12 list no prompts. Reading the code found no path that deletes an answered prompt. Cause unknown: slice 1 settles it. |
| The window can be read by answer time without loading the whole history | Code reading only: `RecallPromptRepository.findAnsweredRecallAnswerRows` already serves recall statistics in production with the same range predicate; `idx_answer_created_at` exists | Presence, not a measurement. Bounded by slice 9, whose proof pages a window larger than one response. |
| A spelling scenario in `mcp_services.feature` runs without paid AI mocks | Not observed | Bounded by slice 10; plan 092 observed that the feature is admitted by the isolated runner, without running a journey. |

## Ordered slices

### 1. Explain answers that have no stored question
Type: Structure
Status: planned
Proof: A recorded explanation, from production data, of how answers
`246327`, `246452`, and `246496` lost their recall prompts, with the code path
or data event named.

An early probe; it changes no product code. With the production database
(read-only, through the owner's `gcloud` access), read those answers' recall
log, tracker, note, and any surviving prompt or question rows, and compare
their times with question generation and note changes. It enables slice 8,
which reports a missing question. If the cause is a defect that deletes
answered prompts, stop slice 8, record the finding under Learnings, and report
it to the owner as its own work; slices 2 to 7, 9, and 10 do not wait on it.
The observation is owner-held and credentialed: when `gcloud` is not signed in,
ask the owner to sign in rather than guessing.
Sizing: about five active minutes.

### 2. List recent multiple-choice attempts
Type: Behavior
Status: done
Proof: New MCP endpoint cases and the tool-list assertion; full backend suite.
Accepted proof: `ListRecallAnswersMcpEndpointTest`
(`returnsTheFullRecordedMultipleChoiceAttempt`; the window case
`returnsOnlyTheLearnersOwnAnswersOfTheLastSevenDaysNewestFirst` now lives in
`ListRecallAnswersWindowMcpEndpointTest`) and the
`McpEndpointTest` tool list, via
`CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests 'com.odde.donut.mcp.*'`
after refactoring; full suite `pnpm backend:test_only` green before the
MCP-only refactor.

Behavior: A learner answered multiple-choice recall prompts in the last seven
days → an assistant calls `list_recall_answers` with no arguments → each of
that learner's attempts is returned newest first with answer time, stem, all
choices, selected choice, expected answer, validation rationale when stored,
recorded correctness, and the note's title and qualified Portable path. Older
attempts and other learners' attempts are absent. The tool is listed as
read-only under `notes:read`.
Sizing: about ten active minutes, the largest slice, because it introduces the
tool, the windowed read, and fixtures that record correctness. It is one
outcome with one proof loop; splitting the registration from the first result
would leave a tool that returns nothing.

### 3. Choose the lookback
Type: Behavior
Status: done
Proof: MCP endpoint cases; full backend suite.

Accepted proof: `ListRecallAnswersWindowMcpEndpointTest` cases
`looksBackTheGivenNumberOfDays`, `refusesDaysThatAreNotPositive`,
`refusesDaysThatAreNotANumber`; full suite green before the MCP-only refactor,
`com.odde.donut.mcp.*` green after it.

Behavior: An incorrect answer is ten days old → the call gives `days` 14 → the
attempt is returned; with no `days` it is not. A `days` that is zero, negative,
or not a number → the call is refused with a message saying a positive number
of days is required.
Sizing: about five active minutes.

### 4. Include spelling attempts
Type: Behavior
Status: done
Proof: MCP endpoint cases; full backend suite.

Accepted proof: `ListRecallAnswersMcpEndpointTest` cases
`returnsTheSpellingAttemptWithItsTypedAnswerAndPrompt` and
`reportsTheSpellingPromptUnavailableWhenTheNoteWasEditedAfterTheAnswer`; full
suite green after refactoring.

Behavior: A learner answered a spelling prompt → the attempt carries the typed
answer, recorded correctness, and the prompt as derived from the note. The note
was edited after the answer → the prompt is reported unavailable, with the
typed answer and correctness kept.
Sizing: about five active minutes.

### 5. Name Overlap and Accidental match outcomes
Type: Behavior
Status: done
Proof: MCP endpoint cases; full backend suite.

Accepted proof: `ListRecallAnswersMcpEndpointTest` cases
`returnsAnOverlapAsACorrectAttemptNamingTheOutcome` and
`returnsAnAccidentalMatchAsOneIncorrectAttemptNamingTheOutcome`; full suite green
before the refactor, `com.odde.donut.mcp.*` green after it.

Behavior: A spelling answer was accepted as an Overlap → the attempt is correct
and names that outcome. A spelling answer named another note and adjusted that
note's tracker → one incorrect attempt names Accidental match; no second
attempt appears for the other note.
Sizing: about five active minutes.

### 6. Show ambiguity exclusions
Type: Behavior
Status: planned
Proof: MCP endpoint case; full backend suite.

Behavior: An incorrect multiple-choice attempt was excluded as ambiguous → the
attempt still shows incorrect, and adds that it was excluded and why.
Sizing: under five active minutes.

### 7. Attach source note evidence for misses
Type: Behavior
Status: planned
Proof: MCP endpoint cases for the four combinations; full backend suite.

Behavior: Correct attempts of either type → title and path only. An incorrect
spelling attempt → also the note's full current content. An incorrect
multiple-choice attempt → also the bounded graph within its budget, with notes
addressed by qualified Portable path.
Sizing: about five active minutes.

### 8. Report unavailable evidence
Type: Behavior
Status: planned
Proof: MCP endpoint cases; full backend suite.

Behavior: The learner can no longer read the source note (subscription
withdrawn, or the note is in the trash) → the attempt keeps its time, answer,
correctness, and stored question, and reports the note evidence as unavailable
with the reason. An answer has no stored question → the attempt keeps its time,
answer, and correctness, and reports the question as unavailable.
Depends on slice 1 for the missing-question case's real shape.
Sizing: about five active minutes.

### 9. Continue a window across responses
Type: Behavior
Status: planned
Proof: MCP endpoint cases; full backend suite.

Behavior: The window holds more attempts than one response carries → the
response gives a `nextCursor` → calling again with it, to the end, returns
every attempt exactly once and then no cursor. An answer recorded between two
calls does not repeat or displace an attempt. A cursor that cannot be read is
refused with a message saying to start again without it.
Sizing: about five active minutes.

### 10. Serve hosted assistants and document the tool
Type: Behavior
Status: planned
Proof: `AssistantMcpAccessTest` case; one new `mcp_services.feature` scenario;
both MCP features green; documentation read.

Behavior: A hosted assistant connected with `notes:read` → calls the tool →
receives the learner's attempts. A learner answers a spelling prompt wrongly in
the browser → an agent calls the tool → the attempt appears with the typed
answer and the note's content. The tool is described in the MCP skill's tool
list and the hosted assistant guide, including that it serves recall answers
and that a default call can take several responses.
Sizing: about ten active minutes plus the E2E wait, a stated external-wait
exception.

## Concern evidence

Slice-plan refinement was not needed: each slice has one outcome and one proof
loop, and the examples extend one model (an attempt with its question, outcome,
and evidence) rather than adding special cases.

Remaining concerns: none beyond the premises above, each bounded by the slice
named in its row. The page size and graph budget are initial values the owner
checks against real history after delivery; that check is not a slice.

## Learnings

- Execution started 2026-11-12 in Story Branch Mode on
  `claude/retrieve-my-recall-answer-history-through-mcp`, claim `e5ee25233c`.
- Slice 1 is deferred, not skipped: the local `gcloud` login had expired
  (reauthentication required) and the owner was asked to sign in. Only slice 8's
  missing-question case waits on it.
- Slice 2 (accepted): attempts are arranged with
  `makeMe.aRecallPrompt().withMcq(..).forMemoryTracker(t).answerChoiceIndex(i).answerTimestamp(ts).recorded().please()`
  then `makeMe.entityPersister.flushAndClear()`; `.recorded()` writes the recall
  log with the answer's grade, as answering does, so `Answer.getCorrect` reads
  the recorded grade. For an Accidental match, add
  `aRecallLogFor(other).confusion().answer(answer)` on the second tracker.
- `RecallAnswerHistoryService.recentAttempts` reads answers by time through
  `AnswerRepository.findRecordedAnswersOfLearnerBetween`, batches prompts with
  `RecallPromptRepository.findAllByAnswerIn`, and takes the source note from the
  prompt's tracker. It keeps only answers that have a prompt; slice 8 lifts that
  and must take the source note from the answer's non-Confusion recall log when
  the prompt is gone. Open-in-view is off, so every lazy walk (logs, note, content,
  graph) stays inside that read-only transaction and the service returns records.
- MCP tools that return JSON share `McpJsonToolResult.of`.
- Full backend suite after slice 2: 3,173 tests, 0 failures.
- Slice 3 (accepted): `days` is a JSON-schema `number` (fractions allowed),
  default 7 in `ListRecallAnswersTool`; the service takes a `Duration`. The MCP
  SDK validates argument types against the input schema before the handler, so a
  text `days` is refused by the SDK naming `/days` and the expected type; the
  handler refuses only non-positive values. In-band refusals share
  `McpToolRefusal.because` (also used by `McpNoteLookup`); slice 9's unreadable
  cursor should use it.
- Slice 4 (accepted): a spelling attempt carries `spelling.typedAnswer` and the
  masked `prompt` from `RecallPrompt.getSpellingQuestion`; when
  `RecallPrompt.isNoteEditedSinceAnswer()` (the prompt's note `updatedAt` after
  the answer time) it carries `promptUnavailable` instead. Any note edit counts,
  even one that leaves the masked text the same. `RecallAttempt` omits null
  parts. Fixtures: `answerSpellingIncorrectly(text)`, and set the note's
  `updatedAt` before the test clock's `now`, since `NoteBuilder` uses real time.
  Full suite: 3,179 tests green.
- Slice 5 (accepted): an Overlap records no recall log (ADR 0003), so
  `AnswerRepository.findRecordedAnswersOfLearnerBetween` finds an answer through
  a recall log of the learner OR a recall prompt on the learner's tracker.
  `RecallAttempt.outcome` carries `AnswerOutcome` names (`OVERLAP`,
  `ACCIDENTAL_MATCH`), as `RecallHistoryExport` does. Fixtures: Overlap without
  `.recorded()`; Accidental match with `.accidentalMatch().recorded()` plus
  `aRecallLogFor(other).confusion().answer(..)`. Tests are split:
  `ListRecallAnswersMcpEndpointTest` (what an attempt carries; slices 6–8),
  `ListRecallAnswersWindowMcpEndpointTest` (which attempts; slice 9), sharing
  `ListRecallAnswersMcpEndpointTestBase`. Full suite: 3,181 tests green.
