# Keep answered questions with their tracker

**Identity:** SEED-073#keep-answered-questions-with-their-tracker
**Source:** [selected story](../../seeds/SEED-073-learning-refinement-and-retrieval.md#keep-answered-questions-with-their-tracker)

## Outcome and boundaries

Keep each answered question, its choices, answer, grade, outcome and ambiguity
exclusion with its existing tracker across both tracker moves and subsequent
old-note deletion. All prompt context and recent-question exports use that
tracker's current note. Questions follow their tracker through application-owned
creation/deletion. Delete the note question bank and its exclusive dependencies,
then the question's stored note, transitively. Add no database cascade.

Keep tracker/prompt/answer/log identities, schedule, property focus, access rules,
and surviving conversation behavior. Do not recover already-lost questions,
merge histories of trackers reduction discards, or change grading/statistics.
One tracker per question does not impose one prompt per question on that tracker.
Do not preserve export fixtures that share a question across different trackers.

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

## Decisive premises and observation

Observations below are at `1e331f5c5c` on 2026-11-13 unless otherwise stated.
They are readings/retained evidence, not claims that new regression tests passed.

| Premise and consuming operation | Observed form/profile and result | Remainder/owner |
| --- | --- | --- |
| Reduction can erase a moved tracker's prompt while leaving its answer/log | The seed's dated production MySQL 8.4 binlog replay reaches both tracker UPDATE and old-note DELETE for answers 246452/246496; subsequent observed missing prompts retain answer indexes and logs. `git diff 4ede0b87d1 HEAD` on tracker-move, Mcq, RecallPrompt and V300000325 paths is empty. This retained result settles the reported cause, not every historical loss. | New regression in slice 8 must reproduce the full controller move/delete/read journey before its repair. |
| Both moves change tracker note without changing question note | `PropertyMemoryTrackerService` move/rehome operations; the production replay observes reduction's effect. Reification's move code is read, but its answered-question retention is not claimed as existing proof. | Slice 8 adds the inverse move/delete/read regression. |
| Physical note column accepts compatibility writes and later omitted writes | Baseline `mcq.note_id` is nullable; no subsequent NOT NULL alteration. Original binary `RecallPrompt.getNote`, conversation, contest and regenerate dereference the mapped note; removing only a getter call does not eliminate EAGER hydration. | Slice 5 must exercise persistence with scalar compatibility writes and freshly reload a prompt. Slice 8 must exercise writes with no mapped column. |
| Existing data never shares one question across different trackers | Current creation paths allocate fresh MCQs; ManyToOne schema permits sharing and tests construct it. No production observation establishes the data premise. | Permitted early credentialed probe in slice 1; nonzero result stops dependent migration planning, preserving every attempt. |
| Existing deletion boundaries cover full lifecycle | Direct tracker deletion, unanswered-prompt replacement, and note hard deletion are distinct. Permanent note removals converge on `NoteService.permanentlyRemove` through reduction, trash/folder deletion and Git proposal application. Current code does not independently clean MCQs. | Slice 7 owns full FK-closure fixtures and external-boundary deletion proof; ORM-only cleanup on one path is insufficient. |
| Release checkpoints have actually replaced earlier binaries | App migrations run at ApplicationReadyEvent; deployment rolls two instances. This is a compatibility constraint, not evidence of future deployment. | Owner-authorized release probes at checkpoints A/B/C must reach actual publication jobs, every active binary and applied schema before dependent slices proceed. |

No new production access, paid model call or product test was performed during
preparation. Local load was about 63 with roughly 50 MiB free pages at inspection;
do not launch a broad suite on that stale basis. Execution rechecks headroom,
coordinates its own heavy work and uses the worktree's disposable databases.

## Outside-in proof ownership

Every row proves surviving behavior or an active access/migration-safety rule.
Removal acceptance is the separate sweep reading below, never an absence test.

| Promise | Owner | Observable boundary and retained/replacement proof |
| --- | --- | --- |
| Questions have one tracker owner without losing existing attempts | 1, 6 | Read-only ownership probe; generation/recovery controller tests exercise the transactional duplicate recheck and selected question/prompt write. |
| History, conversations, contest and regeneration use the current note | 2, 8 | `MemoryTrackerRecallHistoryControllerTest`, `ConversationStartControllerTest`, `ConversationListingControllerTest`, `RecallPromptContestControllerTest`, `RecallPromptRegenerateControllerTest`; inspect emitted title/context and mocked external AI input for a moved tracker. |
| Recent questions follow current note/property, preserving own-choice privacy and ordering | 3, 8 | `NoteExportControllerPropertyHistoryTests`, `NoteExportControllerPropertyPrivacyTests`, `NoteExportControllerLearnerSelectionsTests`, `NoteExportControllerAiContextMarkdownTests`; export moved questions and real recorded choices. |
| Both move/delete journeys retain question, choices, index, grade, outcome, exclusion and schedule | 8 | New focused controller regressions beside `RelationControllerReduceToSourcePropertyTests` and `NoteReifyPropertyTests`; invoke the actual move and permanent-delete operations, flush/clear and read real history plus the `ListRecallAnswersMcpEndpointTest` tool boundary. Canonical payload once; inverse case asserts its delta. |
| Deleting a tracker removes its owned learning questions; other trackers' history and conversation rules survive | 7, 8 | Extend `MemoryTrackerDeleteControllerTest`, unanswered-prompt recovery and note hard-delete boundary tests with complete FK-closure fixtures, including answer/log/conversation/correction rows. This is retained deletion behavior, not proof of feature absence. |
| Ordinary recall and note actions remain usable | 5 | Mounted toolbar/action tests plus existing mocked recall, contest, property recall and conversation E2E journeys; delete bank-only tests rather than negate them. |
| Upgrading schema preserves asked questions and their learner evidence | 9 | Temporary migration safety harness on populated isolated MySQL: default-off gate, enabled selection count, pre-DDL refusal and retained question/prompt/answer/log graph. No permanent test of missing bank rows, endpoints or columns. |

## Ordered slices

### 1. Establish stored question ownership before changing persistence
Type: Behavior
Status: planned
Proof: early read-only probe of actual prompt ownership, retained with time/revision/profile and aggregate counts only.

Under an owner-authorized production read, count `recall_prompt` grouped by MCQ
with `COUNT(DISTINCT memory_tracker_id) > 1`; also count unreferenced MCQs and
inspect the relevant FK delete rules/version. Reuse matching retained observations
where available. No production writes or private question text. Zero cross-tracker
sharing permits the planned ownership transition; otherwise stop dependent work
and refine preservation of those existing attempts before any cleanup. This is
an early probe because only the credentialed persistent store can settle it.

### 2. Use the current tracker note for prompt context
Type: Behavior
Status: planned
Proof: history/conversation and contest/regenerate controller boundaries in the proof table.

Given an answered prompt whose tracker moves, opening its history/conversation,
contesting or regenerating uses the tracker's current note. Derive once through
the prompt; remove direct question-note reads in conversation subject/title and
AI context callers. Preserve property focus and owner/read authorization.
Keep the old storage write until the compatibility release is deployed.

### 3. Export recent questions from trackers currently on the note
Type: Behavior
Status: planned
Proof: export controller cases for note/property membership, newest ordering and the learner's own choice/privacy.

After either move, exporting the current note includes the moved question and
recorded selection. Query through prompts/trackers, including property focus;
do not predicate on the question's stored note. Replace cross-tracker shared-MCQ
fixtures with separate questions, retaining real privacy/access expectations.
Same-tracker repeated-prompt cases remain valid unless naturally redundant.

### 4. Keep test setup stable while separating its domain responsibilities
Type: Structure
Status: planned
Proof: unchanged testability facade through the affected conversation, relationship and recall feature journeys.

Immediately enables slice 5: reorganize the 466-line
`e2e_test/start/testability.ts` along its existing domain mixin seams. Extract
note/relationship seeding and conversation setup into cohesive sibling modules;
keep facade names/callers stable and every edited module under 250 lines.
Retain the question helpers only until their deletion in slice 5. Do not add
another fixture framework or change setup semantics.

### 5. Keep ordinary recall and note actions through question-bank deletion
Type: Behavior
Status: planned
Proof: surviving mounted action tests, full frontend suite/types, API regeneration and affected mocked E2E journeys.

Delete the bank transitively using the source's inventory: controller/service/AI
refinement-only paths, Vue pages/dialogs/validation, testability endpoint/data,
page objects/steps/mocks/isolated spec selection, optimization candidate and
guidance. Include `isMcqValid`, question toolbar action/title entries,
`questionListPage`, question-only methods in shared conversation support,
`injectNumbersNotebookWithQuestions` and the refinement-only AI tool/prompt.
Keep shared conversation, assimilation, recall generation and export behavior.

Replace `Mcq.note`/`Note.mcqs` with the minimum temporary scalar mapping of
`note_id`, populated from the generation input for original binaries. It must
not hydrate a Note or act as attribution. Retain the FK/physical column here.
Regenerate OpenAPI/client and `components.d.ts`; do not hand-edit generated output.
This cohesive transitive removal has no green partial frontend/backend boundary;
budget about 8–10 minutes editing after slice 4, with focused verification wait recorded separately.
If edits exceed 10 minutes, stop and refine this boundary before continuing.

### 6. Persist each selected question with its prompt
Type: Behavior
Status: planned
Proof: generation and recovery controller boundaries exercise the transactional duplicate recheck, reload the returned prompt and observe the selected question.

Persist only the final selected question with its prompt in the existing write
transaction; a duplicate recheck returns the existing prompt without persisting
a discarded candidate. Preserve regeneration behavior without saved intermediate
questions. Keep the compatibility scalar write until slice 8.

### 7. Delete learning questions through their tracker lifecycle
Type: Behavior
Status: planned
Proof: direct tracker, unanswered-prompt and note-deletion controller tests with complete dependent fixtures, preserving other trackers' history and conversation behavior.

Application cleanup covers all tracker/prompt destruction paths, including note
hard deletion and reduction's discarded duplicate tracker. Keep shared
same-tracker question references alive until their last prompt ends. Do not add
database cascades or alter existing answer/log/conversation semantics.

Checkpoint A follows: publish the complete compatible application through the
release checkpoint procedure below. Stop before slice 8 until every active
instance runs this scalar-write/current-note-reader version.

### 8. Preserve answered questions across tracker moves and old-note deletion
Type: Behavior
Status: planned
Proof: reproduce both move/delete/read journeys red, then green through history, MCP and export; preserve grading/schedule/exclusion payload and ordinary deletion proof.

Remove scalar note mapping/writes and remaining compatibility-only dependencies.
Ship a compatible migration dropping `fk_mcq_note`, while retaining nullable
physical `note_id` for checkpoint A binaries during overlap. Questions now
survive reduction/reification and old-note removal through one ownership rule.
The retention promise is accepted after the FK-drop migration is confirmed;
ApplicationReadyEvent means startup/health alone is insufficient evidence.
Checkpoint B follows: stop before slice 9 until every active instance has no
question-note mapping/writes and the FK migration is applied.

### 9. Preserve learner evidence through the final schema upgrade
Type: Behavior
Status: planned
Proof: populated isolated-engine migration safety and retained move/history/MCP/export/deletion behavior; checkpoint C confirms application of the contract migration.

Clean MCQs unreferenced by any prompt, then drop physical `note_id` and its
exclusive index. Use new migration versions above the current tip at execution.
Follow the migration skill's default-off DML placeholder in every profile,
production selection count, full FK-closure reading and temporary safety harness.
Check cleanup readiness before DDL; gate-off with remaining cleanup work fails
before DDL rather than silently accepting incomplete conversion. Obtain the
operator's deliberate gate enablement for this release and remove its override
after applied-migration confirmation. Retain the pending migration safety harness
until that confirmation, then delete the spent harness/placeholder configuration
under the migration lifecycle. Regenerate the ERD. Checkpoint C and the sweep
complete the story; do not call an intermediate release final acceptance.

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
finer-decompose above 10 unless a stated indivisible external verification wait
accounts for the excess. Probe/release waiting is external and never permission
to silently skip a gate. Reassess this same plan on overrun; a boundary change
to the selected story stays with the owner.

Expected oversized edit: `e2e_test/start/testability.ts` is 466 lines. Slice 4
owns the cohesive seeding/conversation reorganization described above; slice 5
deletes the bank-only helpers. Preserved callers and proof are the affected
conversation, relationship and recall journeys. Put new backend regressions in
focused classes; avoid enlarging the 245/248-line export test homes. Recheck
other touched files as they emerge.

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
Result: nine slices; no story-resplit recommendation or human-owned scope decision.
The cumulative design is one tracker-owned question model; replacement owners
8 and 9 delete the temporary scalar write and physical column.

Remaining concerns block readiness: slice 8's inverse move/delete/read outcome,
slice 5's scalar persistence/reload compatibility and slice 7's lifecycle consumer
effects have only source readings, not local result-bearing observations.
The credentialed probes remain permitted, but they do not cover these local gaps.
Before a ready reassessment, use disposable representative fixtures in this
owned workspace, observe the actual consuming operations, and revert every
scratch edit. Use the backend full-suite policy and fresh host-headroom evidence;
record literal commands, revision/profile and observations in the premise rows.
Preparation can land this complete not-ready result without starting execution.
