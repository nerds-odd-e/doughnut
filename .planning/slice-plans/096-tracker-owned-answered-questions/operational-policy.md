# Proof and execution policy

## Outside-in proof ownership

Every row proves surviving behavior or an active access/migration-safety rule.
Removal acceptance is the separate sweep reading below, never an absence test.

| Promise | Owner | Observable boundary and retained/replacement proof |
| --- | --- | --- |
| Questions have one tracker owner without losing existing attempts | 1, 7 | Read-only ownership probe; generation/recovery controller tests exercise the transactional duplicate recheck and selected question/prompt write. |
| History, conversations, contest and regeneration use the current note | 2, 9 | `MemoryTrackerRecallHistoryControllerTest`, `ConversationStartControllerTest`, `ConversationListingControllerTest`, `RecallPromptContestControllerTest`, `RecallPromptRegenerateControllerTest`; inspect emitted title/context and mocked external AI input for a moved tracker. |
| Recent questions follow current note/property, preserving own-choice privacy and ordering | 3, 9 | `NoteExportControllerPropertyHistoryTests`, `NoteExportControllerPropertyPrivacyTests`, `NoteExportControllerLearnerSelectionsTests`, `NoteExportControllerRecentQuestionsTest`, `NoteExportControllerQuestionPrivacyTest`, `NoteExportControllerMovedQuestionsTest`; export moved questions and real recorded choices. |
| Both move/delete journeys retain question, choices, index, grade, outcome, exclusion and schedule | 9 | New focused controller regressions beside `RelationControllerReduceToSourcePropertyTests` and `NoteReifyPropertyTests`; invoke the actual move and permanent-delete operations, flush/clear and read real history plus the `ListRecallAnswersMcpEndpointTest` tool boundary. Canonical payload once; inverse case asserts its delta. |
| Deleting a tracker removes its owned learning questions; other trackers' history and conversation rules survive | 8, 9 | Extend `MemoryTrackerDeleteControllerTest`, unanswered-prompt recovery and note hard-delete boundary tests with complete FK-closure fixtures, including answer/log/conversation/correction rows. This is retained deletion behavior, not proof of feature absence. |
| Ordinary recall and note actions remain usable | 6 | Mounted toolbar/action tests plus existing mocked recall, contest, property recall and conversation E2E journeys; delete bank-only tests rather than negate them. |
| Upgrading schema preserves asked questions and their learner evidence | 10 | Temporary migration safety harness on populated isolated MySQL: default-off gate, enabled selection count, pre-DDL refusal and retained question/prompt/answer/log graph. No permanent test of missing bank rows, endpoints or columns. |

## Release checkpoints and execution authority

This preparation authorizes no implementation or deployment. Future execution
uses Dough Execute Plan. Before each checkpoint's trunk release step in Story
Branch Mode, obtain one human go-ahead covering Dough Land `--keep-worktree`
of the reviewed increment and the exact immutable stable application tag/SHA.
Then follow [release-application](../../../.agents/skills/release-application/SKILL.md)
and its runbook: applicable CI, actual publication job, all active backend
instances and production smoke/schema evidence. Never infer deployment from a
tag or CI success. Preserve the worktree, live plan and Taken assignment across
these checkpoints. A failure stops dependent slices in this same plan.

## Execution gates, sizing and cleanup

Leaves target 5 minutes including proof and cleanup; scrutinize above 5 and
finer-decompose above 10 unless a stated indivisible proof or external wait
accounts for the excess. Probe/release waiting is external and never permission
to silently skip a gate. Reassess this same plan on overrun; a boundary change
to the selected story stays with the owner.

Expected oversized edit: `e2e_test/start/testability.ts` is 466 lines. Slice 4
owns the cohesive seeding/conversation [reorganization in its brief](implementation-briefs.md#keep-test-setup-stable-while-separating-its-domain-responsibilities); slice 6
deletes the bank-only helpers. Preserved callers and proof are the affected
conversation, relationship and recall journeys. The newly observed 409-line
`NotebookGitProposalRenameControllerTest` has the
cohesive split and proof owned by slice 5; the remaining observed Git homes are
79–250 lines. Put new backend regressions in
focused classes; avoid enlarging the 245/248-line export test homes. Recheck
other touched files as they emerge.

Slice 6's wrapper-consumer closure exposed the 377-line
`NoteQuestionGenerationRequestTests` before editing. Its general request and
property-type instruction seams, direct existing-builder contract and preserved
full-suite proof are settled in the slice brief. No size exception is needed.

Backend changes run all unit tests (`CURSOR_DEV=true nix develop -c pnpm
backend:test_only`); schema changes use `pnpm backend:verify` under the same
prefix and the worktree's isolated database. Frontend changes require the full
headless suite, both `vue-tsc --noEmit` and `tsc --noEmit`, and a Vite build for
component declarations. API changes use `pnpm generateTypeScript` and OpenAPI
validation. E2E support changes trace through step wording to affected features,
then run those mocked journeys. These are local gates from the stack skills;
target CI remains execution-owned and is not an extra preparation test gate.

Before test delegation run the shared test-entry selector for actual source
paths; apply MakeMe/page-object conventions only where enabled. Before accepting
each contract/removal change generate the consumer trace and disposition every
listed production/test/helper consumer. Outside-in setup must invoke the real
move/deletion, not supply its desired result.

The full backend proof wait measured in [preparation evidence](preparation-evidence.md#disposable-probe-protocol-and-limits) is indivisible under the backend
skill's all-tests rule. It is the stated good reason for a cohesive leaf to pass
the ten-minute wall-time limit when its edit/cleanup remains bounded; record the
actual wait separately. It does not excuse an edit overrun or an independent
second outcome. The bank deletion still has the stated 8–10 minute edit budget,
after both test-structure steps.

Each slice wraps through Jidoka, a fresh post-change-refactor agent, required
generation, coordinator `./scripts/run.sh pnpm format:changed` once, plan update,
agent commit with independent hook and push. Do not run standalone lint:changed.

One-time final sweep reading, separate from proof: search the whole product,
tests, CLI/MCP, generated files, docs and agent guidance for retired bank symbols,
paths/actions and question-note accessors. Delete exclusive dependencies to
closure. Classify historical immutable migrations and dated retained evidence
by provenance; preserve `.planning/done/` observations as recorded and remove
spent live plan/seed detail at Story Wrap Up. Create no absence assertion,
runtime guard, replacement history note or permanent sweep checker.

## Plan concern review

Slice-plan refinement ran: retained the ownership probe, attribution/export
boundaries and three-release sequence; split the hidden testability reorganization
from feature deletion and split question creation from lifecycle deletion.
Result: ten slices; no story-resplit recommendation or human-owned scope decision.
The cumulative design is one tracker-owned question model; replacement owners
9 and 10 delete the temporary scalar write and physical column.

The new observation pass retains the Behavior boundaries and adds slice 5's
bounded Git test reorganization before slice 6. The two adjacent Structure
steps prepare the same next Behavior and add no separate product outcome.
Consumer failures are explicitly owned: export fixtures in slice 3; bank-only
Git fixtures/assertions, question builders/pure evaluation fixtures and the
atomic-import query in slice 6; intermediate-generation assertions in slice 7;
committed fixture cleanup when writes end in slice 9. Retain their surviving
identity, scheduling, privacy and atomicity proof; delete removed-interface observations without negations.

No remaining local premise concern was identified after the result-bearing
probes and restoration. The production ownership probe and rollout checkpoints
remain bounded by their existing authority and stop conditions. They are not
relabelled as observed, and no slice is marked executed by this preparation.
