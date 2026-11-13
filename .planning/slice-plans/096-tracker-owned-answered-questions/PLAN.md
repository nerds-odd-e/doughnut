# Keep answered questions with their tracker

**Identity:** SEED-073#keep-answered-questions-with-their-tracker
**Source:** [selected story](../../seeds/SEED-073-learning-refinement-and-retrieval.md#keep-answered-questions-with-their-tracker)
**Source review:** [source-only commitments](source-review.md)

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

## Execution resume context

[Retained workspace, revisions, setup and observer identity](execution-context.md#execution-resume-context).

## Decisive premises and observation

[Preparation observations, provenance, results and limits](preparation-evidence.md#decisive-premises-and-observation).

### Disposable probe protocol and limits

[Literal protocol, fixture observations and restoration](preparation-evidence.md#disposable-probe-protocol-and-limits).

## Outside-in proof ownership

[The complete promise/proof-owner table](operational-policy.md#outside-in-proof-ownership) applies to every slice.

## Ordered slices

### 1. Establish stored question ownership before changing persistence
Type: Behavior
Status: done
Proof: early read-only probe of actual prompt ownership, retained with time/revision/profile and aggregate counts only.

The read-only production probe must establish zero cross-tracker question sharing
before the ownership transition; nonzero sharing stops dependent work.

[Detailed implementation brief](implementation-briefs.md#establish-stored-question-ownership-before-changing-persistence). [Accepted production observation](ownership-observation.md).

### 2. Use the current tracker note for prompt context
Type: Behavior
Status: done
Proof: history/conversation and contest/regenerate controller boundaries in the proof table.

Derive prompt history, conversation and AI context/read authorization through
the tracker’s current note while retaining the compatibility storage write.

[Detailed implementation brief](implementation-briefs.md#use-the-current-tracker-note-for-prompt-context). [Accepted implementation proof](current-note-verification.md).

### 3. Export recent questions from trackers currently on the note
Type: Behavior
Status: done
Proof: export controller cases for note/property membership, newest ordering and the learner's own choice/privacy.

Export through prompts/current trackers and property focus, retaining recent
ordering, the learner’s own choice and privacy.

[Detailed implementation brief](implementation-briefs.md#export-recent-questions-from-trackers-currently-on-the-note).

[Accepted export proof and consumer dispositions](recent-question-verification.md).

### 4. Keep test setup stable while separating its domain responsibilities
Type: Structure
Status: done
Proof: unchanged testability facade through the affected conversation, relationship and recall feature journeys.

Split note/relationship seeding and conversation setup along existing domain
seams; keep the facade stable and every edited module under 250 lines.

[Detailed implementation brief](implementation-briefs.md#keep-test-setup-stable-while-separating-its-domain-responsibilities).

[Accepted setup-seam proof](testability-verification.md).

### 5. Keep Git move and learning-identity proof in cohesive test homes
Type: Structure
Status: done
Proof: unchanged notebook Git proposal controller tests in the full backend suite.

Split the two private-learning identity scenarios and their support into a
focused sibling, preserving all existing move/identity proof under 250 lines.

[Detailed implementation brief](implementation-briefs.md#keep-git-move-and-learning-identity-proof-in-cohesive-test-homes).

[Accepted Git identity proof](git-identity-verification.md).

### 6. Keep ordinary recall and note actions through question-bank deletion
Type: Behavior
Status: done
Proof: surviving mounted action tests, full frontend suite/types, API regeneration and affected mocked E2E journeys.

Delete the bank transitively while retaining ordinary recall/note actions. Use
the minimum scalar compatibility write, keeping its FK and physical column.
Stop and refine before continuing if editing exceeds ten minutes.

[Detailed implementation brief](implementation-briefs.md#keep-ordinary-recall-and-note-actions-through-question-bank-deletion).

[Accepted recall/action proof, compatibility observation and removal reading](question-bank-verification.md).

### 7. Persist each selected question with its prompt
Type: Behavior
Status: done
Proof: generation and recovery controller boundaries exercise the transactional duplicate recheck, reload the returned prompt and observe the selected question.

Persist only the final selected question atomically with its prompt; retain
duplicate-recheck and actual-prompt regeneration behavior.

[Detailed implementation brief](implementation-briefs.md#persist-each-selected-question-with-its-prompt).

[Selected-write and application-owned atomicity proof](selected-question-verification.md).

### 8. Delete learning questions through their tracker lifecycle
Type: Behavior
Status: done
Proof: direct tracker, unanswered-prompt and note-deletion controller tests with complete dependent fixtures, preserving other trackers' history and conversation behavior.

Use the shared tracker lifecycle to clean owned questions across all destruction
paths, preserving same-tracker shared questions until their last prompt ends.
Checkpoint A must confirm every active instance before slice 9.

[Detailed implementation brief](implementation-briefs.md#delete-learning-questions-through-their-tracker-lifecycle).

[Accepted local lifecycle proof and retained rollout obligation](tracker-lifecycle-verification.md).
[Checkpoint A preparation and dated production baseline](release-checkpoints.md).

Compatible publication and all-active/schema/public probes are observed;
the developer's manual login confirmation settles G2 and checkpoint A.

### 9. Keep recall history proof in cohesive test homes
Type: Structure
Status: done
Proof: all 14 existing recall-history export scenarios retain their bodies/assertions and pass in the full backend suite.

Separate recall-event content/state from export envelope/access/daily probes,
sharing only existing setup and `answerMcqAtDay`. Enables Behavior 12.

[Detailed implementation brief](implementation-briefs.md#keep-recall-history-proof-in-cohesive-test-homes).

[Accepted scenario-preservation and full-suite proof](recall-history-test-homes-verification.md).

### 10. Create test questions through their tracker and prompt
Type: Structure
Status: done
Proof: all creation-fixture consumers retain current ownership, authorization, payload and committed-isolation observations in the full backend suite.

Retire note-only builder/helper inputs while keeping production compatibility
mapping/writes and the prompt builder's scalar fallback/save. Enables Behavior 12.

[Detailed implementation brief](implementation-briefs.md#create-test-questions-through-their-tracker-and-prompt).

[Accepted fixture-preservation and full-suite proof](tracker-creation-fixtures-verification.md).

### 11. Keep destruction proof tied to the selected questions
Type: Structure
Status: done
Proof: capture question IDs before destruction and observe those same IDs across deletion, retry, rollback and committed fixture cleanup; full backend suite stays green.

Settle G3 and fixture cleanup through tracker/prompt ownership while production
mapping/writes/FK remain unchanged. Enables Behavior 12.

[Detailed implementation brief](implementation-briefs.md#keep-destruction-proof-tied-to-the-selected-questions).

[Accepted selected-ID and committed-graph proof](selected-question-destruction-proof.md).

### 12. Preserve answered questions across tracker moves and old-note deletion
Type: Behavior
Status: planned
Proof: reproduce both move/delete/read journeys red, then green through history, MCP and export; preserve grading/schedule/exclusion payload and ordinary deletion proof.

Remove compatibility writes/mapping and drop the question-note FK, retaining
the nullable physical column. Prove both real move/delete/read journeys.
Checkpoint B must confirm all binaries and applied migration before slice 13.

[Detailed implementation brief](implementation-briefs.md#preserve-answered-questions-across-tracker-moves-and-old-note-deletion).

### 13. Preserve learner evidence through the final schema upgrade
Type: Behavior
Status: planned
Proof: populated isolated-engine migration safety and retained move/history/MCP/export/deletion behavior; checkpoint C confirms application of the contract migration.

Use the default-off gated cleanup and pre-DDL refusal, then retire the physical
column/index. Confirm checkpoint C, regenerate the ERD and perform the final sweep.

[Detailed implementation brief](implementation-briefs.md#preserve-learner-evidence-through-the-final-schema-upgrade).

## Release checkpoints and execution authority

[Human go-ahead for the reviewed retained landing and exact stable tag/SHA, actual publication/all-instance/schema evidence, and stop rules](operational-policy.md#release-checkpoints-and-execution-authority).

## Execution gates, sizing and cleanup

[Full local proof commands, overrun limits, required reorganization, consumer trace, delivery order and final sweep](operational-policy.md#execution-gates-sizing-and-cleanup).

## Plan concern review

[Retained refinement decisions, consumer ownership and unobserved production gates](operational-policy.md#plan-concern-review).


## Story obligations

### G1. Observe application-owned question and prompt atomicity
Reported: slice 7 — "The new payload/cardinality cases use the inherited outer test transaction; application-owned rollback on prompt-write failure has not been observed."
Story clause: "Generation must persist the selected question with its prompt, rather than leave discarded candidates behind."
Disposition: proved by slice 7: selected-question-verification.md#application-owned-atomicity; NOT_SUPPORTED controller call reaches a real SQL fault and a separate committed count retains its baseline, 1/1 in the final full suite
Commitment: C80


### G2. Observe checkpoint A compatibility rollout
Reported: slice 8 — "Checkpoint A still requires the reviewed retained landing and exact stable-tag go-ahead, then actual publication and all-active-instance compatibility evidence."
Story clause: "Use a compatibility release that derives attribution through the tracker while retaining a scalar note ID write for original readers."
Disposition: proved by slice 8: release-checkpoints.md#checkpoint-a-publication-and-observation; approved exact retained landing, main CI/artifacts, actual publication/all-active/schema/public observations and developer manual login confirmation
Commitment: C84
### G3. Retain note deletion proof through compatibility retirement
Reported: slice 8 — "NoteDependentRowsControllerTestBase still counts MCQ rows through physical note_id; that compatibility consumer must change when mapping and writes retire."
Story clause: "A question's note is its tracker's note."
Disposition: proved by slice 11: selected-question-destruction-proof.md; pre-action distinct IDs remain the question observations through deletion, retry and rollback, and full red/green committed-graph cleanup preserves unrelated learning
