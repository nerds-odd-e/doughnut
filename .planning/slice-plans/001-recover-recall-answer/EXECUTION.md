## Execution context

- Execution resumed 2026-10-21 in the established owned worktree
  `/Users/terryyin/git/doughnut/.worktrees/recover-a-recall-answer-after-a-failed-or-lost-s`,
  branch `codex/recover-a-recall-answer-after-a-failed-or-lost-s`, Story Branch
  Mode, origin/main integration target. Publisher
  `dashboard-territory.local-doughnut`, agent Airi-chan; starting revision
  `a9b42ad6f55242ab4df205b1ae672b913381e3e2`, published claim
  `9de0b75a02cf7b8d5b92c0d80bfd2a753f41f095`. Existing planning authority
  permits in-scope refinement. Checkout setup completed with
  `./scripts/run.sh bash scripts/worktree_setup.sh`; applicable command
  `CURSOR_DEV=true nix develop -c pnpm --version` returned 12.14.0.

## Slice 1 accepted evidence

2026-10-21, claim revision 9de0b75a02cf7b8d5b92c0d80bfd2a753f41f095 plus
temporary probe. Independent source-only reviewer `/root/source_review_1` received
only the original Goal, Scope, examples, exclusions and UI clauses; its launch
and complete returned commitments are retained in this execution conversation.
Acceptance compared every commitment: accepted-answer identity, original content,
mark/outcome, creation time, timing, repeated/changed/overlapping submissions and
Grade/Confusion/log/schedule effects remain owned by backend slices 2 and 4;
retained prompt/input/session position, uncertainty feedback, deliberate repeatable
Retry, pending controls, navigation and stale-result protection by slices 3 and 5;
normal advance/history by 3 and 5; overlap's same-tracker new attempt and
accidental-match reveal/resolution under current visibility by 4 and 5;
ownership and ordinary grading by 2 and 4. No product recovery is accepted in this
structure slice. The listed story deferrals remain excluded.

Inspected scratch `RecallPromptSerializationProbeTest.preloadedAnswerIsFreshAfterRefreshWithLock`:
the reader binds a request-owned EntityManager, loads before acceptance
transaction entry, and asserts that the same prompt remains managed inside it.
The writer locks, answers through the real controller, flushes, and waits for an
explicit release. The reader starts the existing locking query and is observed
queued before release. On writer commit, lock-only still returns the same entity
with null answer; `refresh(prompt, PESSIMISTIC_WRITE)` reads the committed answer
ID. On writer rollback, refreshed answer is null and the real controller accepts
a subsequent answer, confirmed from another transaction.

Literal observations: `PROBE rollback=false lock-only=null refresh=1`;
`PROBE rollback=true lock-only=null refresh=null subsequent-accept=3`.
Command: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
Final result: 2,857 tests, zero failures/errors, two skipped; Gradle build 55s
plus 10s migration. The failed stronger snapshot variant and its interpretation
are recorded above. Acceptance inspected both XMLs and the actual source before
deletion. Local disposable evidence remains at
`/tmp/recall-serialization-before-transaction.xml`,
`/tmp/recall-serialization-inside-transaction.xml`, and
`/tmp/RecallPromptSerializationProbeTest.java`. Scratch source was removed from
the repository. Active work was approximately 5–6 minutes, with full-suite waits
excluded under the explicit exception.

Dependent acceptance must lock/refresh before nonlocking transactional reads.
Authorization currently compares loaded owner IDs; AnsweredQuestion projects
tracker identity/property focus, not scheduling state. Fresh-context controller
tests in slices 2 and 4 still own complete response/effect proof, including
retained concurrent and rollback observations. No schema/API wire change.

## Slice 2 refactor reading

2026-10-21: swept the retired MCQ construction parameter and refusal text across
backend, frontend, CLI, MCP, generated packages, E2E, docs and agent guidance.
No live references remain. The original rejection observations in
`OBSERVATIONS.md` and DD-317 retain their dates, revisions and limitations.
This is a one-time acceptance reading, separate from surviving-behavior tests.

## Slice 2 accepted evidence

Independent source-only review `/root/source_review_2` received only the original
story clauses and returned forty individual commitments plus qualifications and
journeys (actual launch/return retained in this conversation). Comparison with
the complete source accepts the MCQ server promises below. G1 explicitly leaves
retained-input feedback/Retry, unchanged session position, pending controls,
navigation/stale completion, and ordinary once-only advance/history to slice 3.
Spelling outcome/reveal/current-access/Confusion and overlap's distinct new attempt
remain owned by slices 4 and 5; the original exclusions remain unchanged.

Inspected `RecallPromptService.answer`: after existing controller ownership
checks, lock and refresh precede ordinary reads; an existing accepted answer is
returned before grading. Same and changed retry content cannot revise it.
`RecallPromptAnswerRecoveryControllerTest` exercises correct, wrong and property
MCQ acceptance in committed transactions; after fresh-context reload, repeated
same/different choices return original answer JSON, identity, creation time and
all timing metrics. Immutable persisted log JSON and tracker state are exactly
unchanged; the initial log is linked to the accepted answer with Good/Again and
one recall transition. Independent concurrent controller calls use a managed
pre-transaction preload and explicit barriers; both recover one answer after
commit, while rollback allows the later caller's ordinary acceptance. Both
paths leave one Grade/log/recall transition. That observing case now lives in
`RecallPromptConcurrentAnswerRecoveryControllerTest.overlappingPreloadedRequestRecoversCommitOrAcceptsAfterRollback`,
with shared committed fixture/snapshot support in `RecallPromptAnswerRecoveryTestBase`.
Anonymous and other-owner recovery
refuse access and preserve original evidence. Existing controller timing and
normal-grading tests establish original capture/validation; the new tests own
preservation through retry.

Command: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
Terminal result: 2,861 tests, zero failures/errors, two skipped; six new cases
passed. Active implementation was approximately 8–9 minutes; suite waits excepted.
Two failing proof fixtures were repaired before that final run: a denial caught
inside a wrapping transaction caused rollback-only commit failure; assertions now
surround the controller's own transaction. An unrefreshed Java float differed from
MySQL stored precision; both exact snapshots now read persisted values.

Consumer trace inspected controller → RecallPromptService → AnswerService →
Answer, plus AnsweredQuestion.from, ConversationSubject, memory-tracker history
and previous-answer retrieval. Their response contract and purposes are unchanged;
the full backend suite includes those existing observations. No API wire/schema
change or generation trigger.

Refactoring deleted the unreachable duplicate-answer guard and its parameter from
Answer.buildAnswer, updating AnswerService and RecallPromptBuilder. The acceptance
sweep found no live references to the removed literal/parameter; the original
dated DD-317 rejection observation and dated preparation reproduction in
OBSERVATIONS.md remain with their provenance. This is a reading,
not a permanent absence test. After a capacity interruption before structural
edits, coordinator checked worktree/index/local and remote HEAD, found no new
commits, and reestablished the all-backend pass before refactoring resumed.
