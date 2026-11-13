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

Retained production evidence and source readings remain at `1e331f5c5c` as
recorded in the seed. New disposable observations were made on 2026-11-13 in
this owned workspace at `0ec6e1dafa97ef0946fbc2afd0039649318d1c72`, profile
`test`, MySQL **8.4.11**, database
`doughnut_wt_3ede975a67f44093a759a2739bd92420_test`. The local engine version,
FK metadata and actual controller consumers were observed, not inferred from
nullable-column definitions.

| Premise and consuming operation | Result-bearing observation | Remainder/owner |
| --- | --- | --- |
| Reduction can erase a moved tracker's prompt while leaving its answer/log | The seed's two production binlog traces remain applicable. In the scalar compatibility prototype with the FK retained, the real reduction controller followed by a fresh history read returned zero prompts for the surviving graded log. The tracker ID and due time survived. | Slice 9 retains a maintained regression; this is no claim about every historical loss. |
| The inverse move has the same retention requirement | The real `NoteController.reifyProperty`, source trash and permanent-delete controllers, with fresh persistence contexts between requests, also returned zero history prompts while the FK remained. With the tracker-owned prototype and FK dropped, both journeys passed history, authenticated MCP and export reads. | Slice 9 owns lasting canonical payload proof once, with the inverse case asserting its delta. |
| Scalar compatibility writes can be read after fresh ORM hydration | With `Mcq.note` replaced by an Integer `note_id` column and a transient generation input, a saved prompt was flushed/cleared and freshly loaded with its question. SQL read the original input note ID from `mcq.note_id`. The initial original-mapping run also freshly loaded that physical shape successfully. | Slice 6 implements the compatibility mapping. Every original binary must be replaced at checkpoint A before retiring that write. |
| New writes and fresh prompt loads work without a question-note mapping | The final prototype removed the scalar mapping/write. The actual recall-generation controller (external OpenAI mocked) persisted a question and a fresh ORM prompt read returned its stem and current tracker note. The old-note deletion journeys also freshly loaded the retained question after the FK drop. | Slice 9 implements this after checkpoint A; these local observations do not establish production deployment. |
| Existing data never shares one question across different trackers | Current callers allocate fresh questions, but the schema and legacy fixtures permit sharing. No production ownership-count observation is retained. The grouping query and applicable FK shape are available locally. | Authorized early credentialed probe in slice 1; nonzero sharing stops dependent work and preserves every attempt. |
| Application-owned cleanup covers direct tracker, unanswered-prompt and permanent-note deletion | The tracker-owned prototype passed all three real controller paths with populated FK-closure fixtures and fresh reads. Other-tracker answered questions, the answered prompt on the same tracker, and a question still used by another prompt on that tracker survived. Existing conversation/deletion tests ran in the same full suite. | Slice 8 implements the shared lifecycle rule. Bank-only consumers and fixture cleanup are assigned below. |
| Release checkpoints replace every earlier binary and apply the intended schema | Source still runs migrations at ApplicationReadyEvent and rolls two instances. Local DDL is not production deployment evidence. | Owner-authorized checkpoint A/B/C observations must reach actual publication jobs, all active binaries and applied schema before dependent slices proceed. |

### Disposable probe protocol and limits

All three runs used the literal command:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

The wrapper selected the isolated database above, migrated it and ran the whole
backend suite. No filtered backend run, production access or paid model call was
made. `TrackerQuestionPreparationProbeTest` was temporary and is deleted.

- Initial original-mapping run: **3,200 tests, 5 failures, 2 skipped**. The fresh
  prompt read passed. Two held-question fixtures reached Hibernate's
  transient-note reference error; three deletion fixtures lacked the required
  batch-owner builder input. These five are not retention/deletion proof.
  The request boundaries and batch fixtures were corrected for the later runs.
- Scalar compatibility run: **3,201 tests, 19 failures, 2 skipped**. The fresh
  scalar write/reload and same-tracker shared-question case passed. Both
  move/delete/history cases failed with zero prompts, and direct tracker and
  unanswered deletion left the question row. Permanent deletion still held its
  fixture tracker in the request context; the final run corrects that boundary.
  The other 14 failures identify seven Git stored-note assertions, six pure
  `QuestionEvaluationTest` fixtures using `forNote(null)`, and the atomic-import
  query through `Mcq.note`; slice 6 owns their consumer conversion/removal.
- Tracker-owned run: **3,201 tests, 21 failures, 2 skipped**. All **seven** temporary probes passed,
  including actual generation and fresh reload, both move/delete/history/MCP/
  export journeys, the three deletion paths, and shared-question retention.
  The 21 failures are seven export fixtures (slice 3), nine Git stored-note
  observations and three bank controller tests (slice 6), the atomic-import
  query (slice 6), and one intermediate-generation row lookup (slice 7).
  `CommittedUserCleanup` is an additional read-through consumer for slice 9.
  These are the observed consumers of the pending feature deletion and query
  conversion; they are not a claim that
  the story implementation or its required full-suite acceptance is complete.

The move fixtures create a wrong MCQ answer at index `1` from choices
`key/picked/third`, record Grade AGAIN, and exclude it with reason
`two interpretations`. After the real move and original-note deletion, the
fresh controller history, authenticated `/api/mcp` `list_recall_answers` call
and current-note Markdown export retain the stem, choices, index, recorded
correctness, grade, exclusion/reason, due time, current note and own selected
answer. Deletion fixtures additionally create a batch request and a prompt
conversation with message and accepted correction, following every incoming FK
in the observed learning subtree. The inverse journey separately invokes trash
and permanent deletion; setup does not simulate its resulting tracker move.

The prototype uses scalar question IDs before tracker deletion and an
application-issued selected-ID DELETE with `NOT EXISTS` on remaining prompts.
Unanswered cleanup uses its existing conversation-detachment boundary. Permanent
note deletion calls that same tracker deletion owner. Querying recent questions
through prompts/current trackers reaches the moved question and own choice.
Surviving authorization/context consumers derive through `RecallPrompt`.
The prototype retained transient bank scaffolding only to compile the untouched
feature; it is not a proposed final representation.

DDL was limited to this disposable database, through
`./scripts/run.sh mysql -h127.0.0.1 -P3309 -uroot --batch --raw
<isolated-database> -e ...`: `ALTER TABLE mcq DROP FOREIGN KEY fk_mcq_note` before
the final run, then `ALTER TABLE mcq ADD CONSTRAINT fk_mcq_note FOREIGN KEY
(note_id) REFERENCES note(id) ON DELETE CASCADE` after it. The restoration query observed zero invalid note
references and the restored `fk_mcq_note` rule as CASCADE.
All scratch Java/test edits were reverted before this record write; the product
checkout matched the starting revision. Raw inputs/logs and summaries were kept
outside the checkout under `/tmp/donut-tracker-preparation-324fe884/`.

Fresh host evidence was taken before the serial runs (load 23.95/37.21/48.32,
about 116 MiB free pages). Each invocation owned only its isolated test database;
no peer process was stopped. The mandatory test tasks took 6m01s and 6m10s in
the first two runs, and 4m01s in the final run, plus migration/startup. Execution
must recheck current headroom rather than reuse that host snapshot.

## Outside-in proof ownership

Every row proves surviving behavior or an active access/migration-safety rule.
Removal acceptance is the separate sweep reading below, never an absence test.

| Promise | Owner | Observable boundary and retained/replacement proof |
| --- | --- | --- |
| Questions have one tracker owner without losing existing attempts | 1, 7 | Read-only ownership probe; generation/recovery controller tests exercise the transactional duplicate recheck and selected question/prompt write. |
| History, conversations, contest and regeneration use the current note | 2, 9 | `MemoryTrackerRecallHistoryControllerTest`, `ConversationStartControllerTest`, `ConversationListingControllerTest`, `RecallPromptContestControllerTest`, `RecallPromptRegenerateControllerTest`; inspect emitted title/context and mocked external AI input for a moved tracker. |
| Recent questions follow current note/property, preserving own-choice privacy and ordering | 3, 9 | `NoteExportControllerPropertyHistoryTests`, `NoteExportControllerPropertyPrivacyTests`, `NoteExportControllerLearnerSelectionsTests`, `NoteExportControllerAiContextMarkdownTests`; export moved questions and real recorded choices. |
| Both move/delete journeys retain question, choices, index, grade, outcome, exclusion and schedule | 9 | New focused controller regressions beside `RelationControllerReduceToSourcePropertyTests` and `NoteReifyPropertyTests`; invoke the actual move and permanent-delete operations, flush/clear and read real history plus the `ListRecallAnswersMcpEndpointTest` tool boundary. Canonical payload once; inverse case asserts its delta. |
| Deleting a tracker removes its owned learning questions; other trackers' history and conversation rules survive | 8, 9 | Extend `MemoryTrackerDeleteControllerTest`, unanswered-prompt recovery and note hard-delete boundary tests with complete FK-closure fixtures, including answer/log/conversation/correction rows. This is retained deletion behavior, not proof of feature absence. |
| Ordinary recall and note actions remain usable | 6 | Mounted toolbar/action tests plus existing mocked recall, contest, property recall and conversation E2E journeys; delete bank-only tests rather than negate them. |
| Upgrading schema preserves asked questions and their learner evidence | 10 | Temporary migration safety harness on populated isolated MySQL: default-off gate, enabled selection count, pre-DDL refusal and retained question/prompt/answer/log graph. No permanent test of missing bank rows, endpoints or columns. |

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
AI context callers, including recall-prompt read authorization in
`AuthorizationService`. Delete the direct-MCQ authorization case when its
exclusive consumers go in slice 6. Preserve property focus and owner/read
authorization.
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
Convert promptless ordering/key/privacy fixtures to tracker-owned questions.
Delete `questionsWithoutPromptsAreIncluded` and its exclusive setup as bank
behavior, without replacing it with an absence assertion.

### 4. Keep test setup stable while separating its domain responsibilities
Type: Structure
Status: planned
Proof: unchanged testability facade through the affected conversation, relationship and recall feature journeys.

Enables the next Behavior, slice 6, together with slice 5: reorganize the 466-line
`e2e_test/start/testability.ts` along its existing domain mixin seams. Extract
note/relationship seeding and conversation setup into cohesive sibling modules;
keep facade names/callers stable and every edited module under 250 lines.
Retain the question helpers only until their deletion in slice 6. Do not add
another fixture framework or change setup semantics.

### 5. Keep Git move and learning-identity proof in cohesive test homes
Type: Structure
Status: planned
Proof: unchanged notebook Git proposal controller tests in the full backend suite.

Immediately enables slice 6 alongside slice 4's E2E setup preparation. The
observed question-note consumers include the 409-line
`NotebookGitProposalRenameControllerTest`. Move its two private-learning
identity scenarios (same-parent edited rename and renamed/untouched-note
associations), their committed conversation cleanup and association fixture into
a focused sibling. Keep ordinary move and companion-change admission in the
original home. Reuse `NotebookGitControllerTestBase` and shared scoring bodies;
both homes must be under 250 lines, with every existing observable preserved.
Other observed Git test homes are at most 250 lines. This is a bounded test
responsibility split, not new Git behavior or another fixture framework.

### 6. Keep ordinary recall and note actions through question-bank deletion
Type: Behavior
Status: planned
Proof: surviving mounted action tests, full frontend suite/types, API regeneration and affected mocked E2E journeys.

Delete the bank transitively using the source's inventory: controller/service/AI
refinement-only paths, Vue pages/dialogs/validation, testability endpoint/data,
page objects/steps/mocks/isolated spec selection, optimization candidate and
guidance. Include `isMcqValid`, question toolbar action/title entries,
`questionListPage`, question-only methods in shared conversation support,
`injectNumbersNotebookWithQuestions` and the refinement-only AI tool/prompt.
The scratch suite also identified stored-note consumers in
`NotebookGitComposedAccumulatedRenameControllerTest`,
`NotebookGitComposedMoveEditControllerTest`, `NotebookGitCopyIdentityControllerTest`,
`NotebookGitProposal{FolderRelocation,Relocation}PrivateAssociationControllerTest`
and the rename scenarios separated in slice 5. Delete their bank-only MCQ
fixtures, association fields, lookup helpers and stored-note assertions; retain
note/tracker/conversation identity and scheduling proof. Where a retained
scenario actually needs a question, seed it through its tracker and prompt.
Update `McqBuilder`, `RecallPromptBuilder` and `QuestionEvaluationTest` so pure
question content requires no note, while compatibility persistence gets the
scalar ID from the tracker/generation input. Replace the removed-note query in
`QuestionGenerationBatchRowImportServiceAtomicTest` with proof of the surviving
atomic import/retry outcome. These are consumer closure, not absence tests.
Keep shared conversation, assimilation, recall generation and export behavior.

Replace `Mcq.note`/`Note.mcqs` with the minimum temporary scalar mapping of
`note_id`, populated from the generation input for original binaries. It must
not hydrate a Note or act as attribution. Retain the FK/physical column here.
Regenerate OpenAPI/client and `components.d.ts`; do not hand-edit generated output.
This cohesive transitive removal has no green partial frontend/backend boundary;
budget about 8–10 minutes editing after slices 4 and 5, with verification wait
recorded separately.
If edits exceed 10 minutes, stop and refine this boundary before continuing.

### 7. Persist each selected question with its prompt
Type: Behavior
Status: planned
Proof: generation and recovery controller boundaries exercise the transactional duplicate recheck, reload the returned prompt and observe the selected question.

Persist only the final selected question with its prompt in the existing write
transaction; a duplicate recheck returns the existing prompt without persisting
a discarded candidate. Preserve regeneration behavior without saved intermediate
questions. Delete the intermediate-row lookup/assertions in
`McqTest.AutoEvaluateAndRegenerate`; keep the selected question result and the
controller proof for regeneration of an actually asked prompt. Keep the
compatibility scalar write until slice 9.

### 8. Delete learning questions through their tracker lifecycle
Type: Behavior
Status: planned
Proof: direct tracker, unanswered-prompt and note-deletion controller tests with complete dependent fixtures, preserving other trackers' history and conversation behavior.

Application cleanup covers all tracker/prompt destruction paths, including note
hard deletion and reduction's discarded duplicate tracker. Keep shared
same-tracker question references alive until their last prompt ends. Do not add
database cascades or alter existing answer/log/conversation semantics.
The observed implementation seam is scalar question-ID selection before tracker
cascade deletion, followed by deletion of only selected, unreferenced questions.
It avoids loading prompts merely to discover IDs. Permanent note deletion must
invoke the same tracker lifecycle owner; unanswered cleanup already detaches
conversation subjects before removing prompts. The representative fixtures
populate answers/logs, batch requests, conversations/messages and corrections.

Checkpoint A follows: publish the complete compatible application through the
release checkpoint procedure below. Stop before slice 9 until every active
instance runs this scalar-write/current-note-reader version.

### 9. Preserve answered questions across tracker moves and old-note deletion
Type: Behavior
Status: planned
Proof: reproduce both move/delete/read journeys red, then green through history, MCP and export; preserve grading/schedule/exclusion payload and ordinary deletion proof.

Remove scalar note mapping/writes and remaining compatibility-only dependencies.
Adapt `CommittedUserCleanup` at this boundary: capture question IDs through
prompts/trackers before deleting prompts, then clean those unreferenced questions.
Its current native join through `mcq.note_id` cannot identify the new writes.
Preserve committed-fixture isolation and collect IDs before their ownership links
are destroyed; do not add a parallel cleanup framework.
Ship a compatible migration dropping `fk_mcq_note`, while retaining nullable
physical `note_id` for checkpoint A binaries during overlap. Questions now
survive reduction/reification and old-note removal through one ownership rule.
The retention promise is accepted after the FK-drop migration is confirmed;
ApplicationReadyEvent means startup/health alone is insufficient evidence.
Checkpoint B follows: stop before slice 10 until every active instance has no
question-note mapping/writes and the FK migration is applied.

### 10. Preserve learner evidence through the final schema upgrade
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
finer-decompose above 10 unless a stated indivisible proof or external wait
accounts for the excess. Probe/release waiting is external and never permission
to silently skip a gate. Reassess this same plan on overrun; a boundary change
to the selected story stays with the owner.

Expected oversized edit: `e2e_test/start/testability.ts` is 466 lines. Slice 4
owns the cohesive seeding/conversation reorganization described above; slice 6
deletes the bank-only helpers. Preserved callers and proof are the affected
conversation, relationship and recall journeys. The newly observed 409-line
`NotebookGitProposalRenameControllerTest` has the
cohesive split and proof owned by slice 5; the remaining observed Git homes are
79–250 lines. Put new backend regressions in
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

The full backend proof wait measured above is indivisible under the backend
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
