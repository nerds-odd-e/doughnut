# Proof and execution policy

## Architecture and reuse

PFE: extend `RecallPrompt`'s existing tracker relationship for attribution;
`RecallPromptPersister` for atomic selected-question persistence;
`MemoryTrackerService` and `NoteService.permanentlyRemove` for lifecycle cleanup;
the existing recent-question repository/export boundary for projection. Reuse
the existing pre-DDL check pattern in `V300000338__DropNotebookGitBindingBundleBytes`.
Do not add an archive, second ownership model or alternative deletion owner.

Relevant Accepted decisions: [0001, domain vocabulary](../../../docs/adrs/0001-ubiquitous-language.md),
[0002, private learning data and content identity](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md),
[0003, grading and durable attempts](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md),
[0006, failure handling](../../../docs/adrs/0006-failure-handling-accepted.md),
and [0007, isolated test data](../../../docs/adrs/0007-environments-and-isolation-accepted.md).
The index and record statuses agree; no conflict or exception was found.
ADR 0001's Accepted filename lacks the suffix used by the catalog convention;
this existing naming mismatch changes no decision. The North Star's
[accepted-change boundary](../../NORTH-STAR.md#one-accepted-change-boundary)
keeps private learning data outside Portable notebook content. No new direction is needed.

## Outside-in proof ownership

Every row proves surviving behavior or an active access/migration-safety rule.
Removal acceptance is the separate sweep reading below, never an absence test.

| Promise | Owner | Observable boundary and retained/replacement proof |
| --- | --- | --- |
| Questions have one tracker owner without losing existing attempts | 1, 7 | Read-only ownership probe; generation/recovery controller tests exercise the transactional duplicate recheck and selected question/prompt write. |
| History, conversations, contest and regeneration use the current note | 2, 12 | `MemoryTrackerRecallHistoryControllerTest`, `ConversationStartControllerTest`, `ConversationListingControllerTest`, `RecallPromptContestControllerTest`, `RecallPromptRegenerateControllerTest`; inspect emitted title/context and mocked external AI input for a moved tracker. |
| Recent questions follow current note/property, preserving own-choice privacy and ordering | 3, 12 | `NoteExportControllerPropertyHistoryTests`, `NoteExportControllerPropertyPrivacyTests`, `NoteExportControllerLearnerSelectionsTests`, `NoteExportControllerRecentQuestionsTest`, `NoteExportControllerQuestionPrivacyTest`, `NoteExportControllerMovedQuestionsTest`; export moved questions and real recorded choices. |
| Both move/delete journeys retain identities, question, choices, index, grade, outcome, exclusion and schedule | 12 | Maintained regressions in a focused MCP sibling reusing `ListRecallAnswersMcpEndpointTestBase`: actual reduction/reification then old-note permanent deletion, flush/clear, history and export through existing MockMvc/bearer access. The existing controller move suites retain their own mechanics proof. Canonical index-2 wrong-answer payload once; inverse delta separately. Observe a positive populated legacy asked row through move/delete/read on the migrated schema too. |
| Deleting a tracker removes its owned learning questions; other trackers' history and conversation rules survive | 8, 11, 12 | Extend `MemoryTrackerDeleteControllerTest`, unanswered-prompt recovery and note hard-delete boundary tests with complete FK-closure fixtures, including answer/log/conversation/correction rows. Slice 11 carries pre-destruction selected question IDs into deletion/retry/rollback; 12 retains that proof through compatibility retirement. |
| Ordinary recall and note actions remain usable | 6 | Mounted toolbar/action tests plus existing mocked recall, contest, property recall and conversation E2E journeys; delete bank-only tests rather than negate them. |
| Upgrading schema preserves asked questions and their learner evidence | 13–16 | Isolated-catalog resource proof precedes the temporary populated-engine safety harness proves default-off readiness, enabled retry/selection, pre-DDL refusal and exact asked graph; C observes the complete upgrade; post-application retirement preserves fresh/current operation and neutral startup. No permanent absence assertions. |
| Recall export content/state, envelope/access and daily-probe behavior stays unchanged | 9 | All 14 existing history-export scenario bodies/assertions retained across cohesive homes; full backend proof. |
| Creation fixtures retain current-note access, asked payload and isolation | 10 | Every builder/helper consumer retains its observations, including both opposite-ownership authorization cases with temporary local scalar setup and committed fixture isolation; full backend proof. |
| Destruction and committed cleanup preserve selected-question observations | 11 | Preselect distinct question IDs through trackers/prompts; carry the same IDs through delete/retry/rollback. Cleanup shares its matching tracker-user/owned-note prompt scope, then deletes only selected unreferenced questions. One graph case proves both roots and an unrelated survivor; full backend red/green proof. |

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

The current preflight also found 358 lines in
`UserRecallHistoryExportControllerTest`; slice 9 owns its content/state versus
export envelope/access/daily-probe split before fixture adaptation. All 14 test
bodies/assertions and existing common setup survive; all edited homes stay under
250 lines. The [briefs](implementation-briefs.md) own the bounded seams.

Slice 6's wrapper-consumer closure exposed the 377-line
`NoteQuestionGenerationRequestTests` before editing. Its general request and
property-type instruction seams, direct existing-builder contract and preserved
full-suite proof are settled in the slice brief. No size exception is needed.

Backend changes run all unit tests (`CURSOR_DEV=true nix develop -c pnpm
backend:test_only`); schema changes use `pnpm backend:verify` under the same
prefix and the worktree's isolated database. Each schema slice explicitly
regenerates the ERD with `CURSOR_DEV=true nix develop -c env
DONUT_ERD_SCHEMA=<this-worktree-test-schema> pnpm export:database-erd`; use the
isolated migrated catalog, not auto-selected development. Frontend changes require the full
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
second outcome. Current remaining edit/cleanup budgets are 3–5 minutes for 9,
5–8 each for 10/11 and 8–10 for 12. All require a green full-backend boundary.
Slice 12's red/green, schema verification and ERD form one retention proof loop;
13 owns isolated-engine setup and actual CI capability; 14's cleanup, 15's
complete upgrade/C and 16's lifecycle each own a cohesive verification loop.
Those indivisible proof waits and human/release waiting are stated exceptions;
editing/cleanup beyond ten minutes must stop and refine, never consume the wait
exception. Remaining editing estimates are 5–8 minutes for Structure 13,
8–10 for cleanup 14, 5–8 for contract 15 and 4–6 for lifecycle 16; concrete setup/teardown and stopping boundaries are in the
[final schema briefs](final-schema-briefs.md).

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

The 2026-11-13 preflight at published base
`e5b8aca1c605f59775f8cc607ea5f08095a9348e` found 73 hidden consumer files and
the oversized history-export test. The prior remaining-leaf sizing assumed
that preparation fit inside the retention transition; that assumption failed
before product edits. The workspace was clean, with no attempt-owned work to
park. The smallest authorized result remains retention across both real moves
and old-note deletion; leaving the FK in place cannot deliver it.

Decision: **Split** old 9's preparation into Structures 9 (cohesive history proof),
10 (creation fixture inputs) and 11 (pre-destruction question selection), each
with unchanged external behavior and full backend proof, immediately preparing
Behavior 12. **Retain** old Behavior 9 as 12 and old Behavior 10 as 13; completed
1–8 remain unchanged. This old-9→12 / old-10→13 mapping also resolves forward
pointers in dated records without renaming their observed actions. G3 receives
11. There are **13 slices** including completed work; no resplit recommendation.

The cumulative design remains one tracker-owned question model, using the same
prompt persistence and tracker/note lifecycle boundaries. No story, ADR, PFE or
subsystem direction changes. The strongest smaller choice is to adapt fixtures
inside 12; three independent green preparation boundaries isolate that closure
and protect its 8–10 minute edit budget. Shared fixtures get no new framework.
Source-only commitment wording and settled evidence stay intact; no unproved
source promise solely names fixture preparation, so their final Behavior owners
remain 12/13. Structure proof owners 9–11 are explicit in the table and briefs.

Checkpoint A is observed and G2 proved. B/C remain future authorized-checkpoint
operations with human go-aheads, all-active and applied-migration stop conditions.
Fresh unmapped NULL note IDs cannot stand in for legacy retention: 12 requires
a positive populated legacy asked-row observation on the migrated schema, then
retires that temporary physical fixture after B and before 13. Local planning
neither observes these future gates nor marks any new slice executed.

Before 11, a narrow cleanup reading exposed both existing deletion roots:
matching learner trackers and matching users' owned notes, whose FK cascade
also removes other learners' trackers. Nullable note/circle ownership paths
need optional joins. The shared selection/deletion predicate and one focused
graph case clarify 11's existing fixture-isolation proof; boundaries and story
scope stay unchanged. Full-suite red/green waits remain separately owned.

## Final schema refinement after observed B

After B and slice 12 closure `0f1d1f5b...`, local source inspection exposed the
omitted populated-engine and production-override lifecycle work. The
[final schema briefs](final-schema-briefs.md#readiness-and-boundary-decision)
record the initial split and subsequent actual shared-catalog failure.
Structure 13 now proves owned-catalog MakeMe isolation and CI administration
before cleanup 14, contract/C 15 and lifecycle 16. The cumulative model/source
outcome stay unchanged; 16 slices remain. Story resplit is recommended under
the refinement skill; the optional developer choice remains pending.
Completed slices and dated observations above retain their original provenance.
