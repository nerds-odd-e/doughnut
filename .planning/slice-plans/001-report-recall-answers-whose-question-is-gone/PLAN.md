# Report recall answers whose question is gone

**Identity:** SEED-073#report-recall-answers-whose-question-is-gone
**Source:** [Selected story](../../seeds/SEED-073-learning-refinement-and-retrieval.md#report-recall-answers-whose-question-is-gone)

## Outcome and boundary

The connected learner receives each attributable answer in the requested window,
including an answer whose recall prompt has disappeared. The response retains
what is stored, explicitly explains unavailable question material, and applies
the existing source-note evidence and privacy rules.

This plan does not repair the separately reported relationship-reduction defect,
restore deleted questions, introduce question snapshots, change recall scheduling
or statistics, or recover answers whose learner attribution has itself been
hard-deleted. The production investigation is retained in the source story;
implementation requires a separate execution instruction.

## Existing solutions and decisions

- PFE: extend `RecallAnswerHistoryService` and its `RecallAttempt` projection.
  `AnswerRepository.findRecordedAnswersOfLearnerBefore` already selects answers
  through recall logs even when their prompts are gone. Keep its keyset window
  and page position. Use the answer's graded (non-Confusion) recall log to find
  the primary tracker when there is no prompt; Confusion is a secondary event.
- Reuse `Answer.getCorrect`, outcome and exclusion fields. Determine the stored
  answer form from its choice index or spelling text rather than the tracker's
  current focus or its newest question. A later question on the same tracker
  does not reconstruct the original question or its choices.
- Keep the current note-availability check, Portable path authoring, and bounded
  graph retrieval in their existing owners. A correct attempt carries title and
  path; a missed MCQ carries the current bounded graph and a spelling miss the
  current content. An unreadable or trashed note carries its unavailable reason.
- A suitable additive response is a missing-question MCQ carrying the original
  zero-based `selectedChoiceIndex` and an explicit `questionUnavailable` reason
  in `multipleChoice`; its stem, choices and answer text cannot be recovered.
  Reuse spelling's `typedAnswer` and `promptUnavailable` for a missing spelling
  prompt. Preserve the existing available-question response fields.
- [ADR 0002 — Git-native Portable notebook synchronization](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  keeps private learning history in MySQL. [ADR 0003 — Spaced-repetition scheduling policy](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)
  distinguishes Grades from Confusion. [ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md)
  supplies the existing loud-failure policy; the production probe found no
  reachable ungraded answer requiring a new product decision here.
- `.planning/NORTH-STAR.md` adds no consequential choice for this read projection;
  reuse the existing private-history and MCP boundaries. No new direction or
  schema is needed. `RecallAttempt` has only MCP consumers, so no OpenAPI
  regeneration is expected.

## Decisive premises

All observations use the owned preparation checkout at `b3f4ce10bd0d` unless
another profile is explicitly named. Scratch edits are removed before records
are written.

| Premise / consuming operation | Observation and result | Remainder |
| --- | --- | --- |
| Production really retains graded answers after question loss | Read-only transaction in production `doughnut` on MySQL `8.4.11-google`, 2026-11-12 23:22:30 UTC; 2,197 promptless answers reachable through understanding trackers. Samples `246327`, `246452`, `246496` retain AGAIN logs, choice indexes and their primary trackers. No reachable answer lacked correctness derivable by the existing GOOD/AGAIN/OVERLAP rule. | Counts are a dated snapshot, not a prevalence or completeness guarantee. |
| Relationship reduction can leave a tracker while losing its prompt | Production binary log `mysql-bin.000048`, UTC replay, and live FK metadata; GTIDs ending `54749` and `55341` move trackers `5943` and `3227` and delete their former notes in the same transactions. The source story retains positions, times and code correspondence. | The two witnessed cases do not establish the cause of all historical losses. The preservation repair is separate work. |
| The MCP boundary exposes the current omission, and a real graded fixture survives loss of only its prompt | 2026-11-13 SGT: `CURSOR_DEV=true JAVA_TOOL_OPTIONS=-XX:ActiveProcessorCount=2 GRADLE_OPTS=-Dorg.gradle.workers.max=1 nix develop -c pnpm backend:test_only`, isolated MySQL `doughnut_wt_ce28d809be7c48699a21758e99882166_test`. A scratch `ListRecallAnswersMissingQuestionMcpEndpointTest` built a recorded MCQ using MakeMe, observed one MCP attempt, deleted only its prompt with native SQL, flushed/cleared, and observed zero attempts (expected one). The suite completed 3,195 tests: this sole expected failure, 3,192 passes, two skips. All 22 existing recall-answer tests passed (6 base / 8 evidence / 8 window). Test phase 5m48s, preceding migration/build 1m35s. Scratch source removed before this plan write; product and maintained test sources are unchanged. | Final fields and missing-question variants remain implementation proof in slice 1. |

## Proof ownership and consumers

Every row belongs to slice 1. Inspect setup and actual JSON assertions; command
success alone is not the observation.

| Promise | Observable proof |
| --- | --- |
| A promptless MCQ remains the learner's attempt | Add `ListRecallAnswersMissingQuestionMcpEndpointTest`: real Answer and graded RecallLog, unavailable prompt, MCP call returns the original time, recorded correctness, stored zero-based index and explicit question-unavailable reason. Include a later question on the same tracker so the original attempt stays distinguishable. |
| Other stored attempt metadata remains meaningful | Missing-prompt fixtures prove the exclusion reason and recorded result survive; a spelling fixture proves its typed answer and explicit unavailable prompt. Reuse the ordinary MCQ/spelling/outcome cases for retained available-question behavior. |
| Evidence identifies the primary recalled note and respects access | At the same MCP boundary, a missed MCQ has its primary note's graph; a correct answer has title/path. Parameterize inaccessible and trashed source-note cases to observe `note.unavailable`. A spelling Accidental match with a secondary Confusion log identifies the graded tracker, once. |
| A complete window includes each attempt exactly once and stays private | Extend the existing window proof with more than 50 promptless/mixed attempts, including equal-time answers at a boundary; follow every cursor and compare the concatenated original times/answer values. Include another learner's record and an out-of-window record to prove active privacy and window filtering. |
| Connected assistants understand the result | Read the tool DESCRIPTION, `.agents/skills/mcp-server/SKILL.md`, and `docs/assistant-oauth.md` together against the emitted missing-question JSON and its zero-based index. |

Consumer trace: `RecallAttempt` flows only through `ListRecallAnswersTool` and
`McpJsonToolResult`, whose text and structured content derive from the same
serialization. Existing `ListRecallAnswers{,Evidence,Window}McpEndpointTest` and
`AssistantMcpAccessTest.assistantReadsTheLearnersRecallAnswers` remain applicable
and run in the full backend suite. The browser MCP scenario in
`e2e_test/features/mcp/mcp_services.feature` uses
`mcpAgentActions.expectSpellingRecallAnswer`: it finds an ordinary spelling
attempt by typed text and reads correctness/content. Those fields and that
available-prompt path remain covered by the existing backend spelling case;
this plan adds no browser interaction or E2E fixture. No frontend or CLI code
consumes this projection. During execution run the installed consumer-trace
script on the actual diff and disposition any additional consumers it finds.

## Ordered slices

### 1. Report surviving answers with unavailable questions
Type: Behavior
Status: planned
Proof: the MCP response observations in every row above, plus all retained
backend checks via `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

Behavior: given the learner's stored answer and primary graded log, when its
question is unavailable and the assistant requests the window, return the
attempt with stored answer data, recorded grading/exclusion, explicit question
unavailability and authorized note evidence; continuing pages returns every
attributable attempt once.

Extend the existing projection and history assembly, keeping one owner for note
availability/evidence and for attempt metadata shared by both prompt states.
Use the existing MakeMe builders and real persistence; native SQL may establish
an already-missing prompt without testing or altering product deletion. Test
entry selection for the intended new `...MissingQuestionMcpEndpointTest.java`
resolved `makeMe: true`, `pageObjects: false`; recheck new paths before authoring.
Update the three assistant-facing descriptions in this same slice.

Sizing: about 5–8 minutes of active implementation, proof authoring and cleanup;
the required full backend run and independent refactor review are the explicit
verification/wait exception if total elapsed exceeds ten minutes. Scrutinized
against the five-minute target: all variants exercise one answer-projection
rule and one MCP proof loop; splitting tests, docs, or note evidence from that
rule would fragment its outcome. Stop and refine this same plan if active work
exceeds ten minutes or reveals a second outcome.

Safe stopping point: all promised response variants and retained checks are
green, documentation agrees with the response, and no product state is changed
by listing. No intermediate response contract is planned.

## Delivery and proof conditions

Execution follows AGENTS.md and Dough Execute Plan: Jidoka, fresh
`dough-post-change-refactor` agent, API generation only if an actual controller
contract changes, coordinator `./scripts/run.sh pnpm format:changed` once,
plan update, agent commit with the independent check-only hook, and push.
Do not implement or Take from this preparation.

Before another full backend run, recheck local CPU/memory and competing work.
The preparation host had 16 CPUs, 48 GiB RAM, load about 36 and substantial
compression at 2026-11-13 07:27 SGT. Its observation command limited JVM active
processors to two and Gradle workers to one, retaining the full suite. This is
host-specific evidence; execution owns a fresh launch decision if conditions
change. No paid service or production write is needed for implementation proof.

## Plan review

Slice-plan refinement was not needed: one coherent Behavior, mapped proof, no
Structure preparation, and no independent second outcome. This review found no
remaining slice-specific concern after the observations above. The separate
preservation defect is reported in the seed; it is not a prerequisite for
truthfully reporting already-surviving answers.
