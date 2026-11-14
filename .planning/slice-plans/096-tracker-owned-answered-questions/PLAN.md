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

## Supporting context

- [Reuse and Accepted architecture](operational-policy.md#architecture-and-reuse).
- [Execution identity, setup and observation](execution-context.md#execution-resume-context).
- [Decisive premises and dated observations](preparation-evidence.md#decisive-premises-and-observation).
- [Disposable protocol and limits](preparation-evidence.md#disposable-probe-protocol-and-limits).
- [Outside-in promise/proof owners](operational-policy.md#outside-in-proof-ownership), applying to every slice.

## Ordered slices

### 1. Establish stored question ownership before changing persistence
Type: Behavior
Status: done
Proof: early read-only probe of actual prompt ownership, retained with time/revision/profile and aggregate counts only.

[Detailed implementation brief](implementation-briefs.md#establish-stored-question-ownership-before-changing-persistence). [Accepted production observation](ownership-observation.md).

### 2. Use the current tracker note for prompt context
Type: Behavior
Status: done
Proof: history/conversation and contest/regenerate controller boundaries in the proof table.

[Detailed implementation brief](implementation-briefs.md#use-the-current-tracker-note-for-prompt-context). [Accepted implementation proof](current-note-verification.md).

### 3. Export recent questions from trackers currently on the note
Type: Behavior
Status: done
Proof: export controller cases for note/property membership, newest ordering and the learner's own choice/privacy.

[Detailed implementation brief](implementation-briefs.md#export-recent-questions-from-trackers-currently-on-the-note).

[Accepted export proof and consumer dispositions](recent-question-verification.md).

### 4. Keep test setup stable while separating its domain responsibilities
Type: Structure
Status: done
Proof: unchanged testability facade through the affected conversation, relationship and recall feature journeys.

[Detailed implementation brief](implementation-briefs.md#keep-test-setup-stable-while-separating-its-domain-responsibilities).

[Accepted setup-seam proof](testability-verification.md).

### 5. Keep Git move and learning-identity proof in cohesive test homes
Type: Structure
Status: done
Proof: unchanged notebook Git proposal controller tests in the full backend suite.

[Detailed implementation brief](implementation-briefs.md#keep-git-move-and-learning-identity-proof-in-cohesive-test-homes).

[Accepted Git identity proof](git-identity-verification.md).

### 6. Keep ordinary recall and note actions through question-bank deletion
Type: Behavior
Status: done
Proof: surviving mounted action tests, full frontend suite/types, API regeneration and affected mocked E2E journeys.

[Detailed implementation brief](implementation-briefs.md#keep-ordinary-recall-and-note-actions-through-question-bank-deletion).

[Accepted recall/action proof, compatibility observation and removal reading](question-bank-verification.md).

### 7. Persist each selected question with its prompt
Type: Behavior
Status: done
Proof: generation and recovery controller boundaries exercise the transactional duplicate recheck, reload the returned prompt and observe the selected question.

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

[Detailed implementation brief](implementation-briefs.md#keep-recall-history-proof-in-cohesive-test-homes).

[Accepted scenario-preservation and full-suite proof](recall-history-test-homes-verification.md).

### 10. Create test questions through their tracker and prompt
Type: Structure
Status: done
Proof: all creation-fixture consumers retain current ownership, authorization, payload and committed-isolation observations in the full backend suite.

[Detailed implementation brief](implementation-briefs.md#create-test-questions-through-their-tracker-and-prompt).

[Accepted fixture-preservation and full-suite proof](tracker-creation-fixtures-verification.md).

### 11. Keep destruction proof tied to the selected questions
Type: Structure
Status: done
Proof: capture question IDs before destruction and observe those same IDs across deletion, retry, rollback and committed fixture cleanup; full backend suite stays green.

[Detailed implementation brief](implementation-briefs.md#keep-destruction-proof-tied-to-the-selected-questions).

[Accepted selected-ID and committed-graph proof](selected-question-destruction-proof.md).

### 12. Preserve answered questions across tracker moves and old-note deletion
Type: Behavior
Status: done
Proof: reproduce both move/delete/read journeys red, then green through history, MCP and export; preserve grading/schedule/exclusion payload and ordinary deletion proof.

Remove compatibility writes/mapping and drop the question-note FK, retaining
the nullable physical column. Prove both real move/delete/read journeys.
Checkpoint B must confirm all binaries and applied migration before slice 13.

[Detailed implementation brief](implementation-briefs.md#preserve-answered-questions-across-tracker-moves-and-old-note-deletion).

[Local retention/schema proof](tracker-retention-verification.md).
[Accepted checkpoint B publication and observation](checkpoint-b-verification.md).

### 13. Isolate the populated migration proof from shared test fixtures
Type: Structure
Status: done
Proof: canonical pre-C engine replay and committed MakeMe graph in an owned catalog, unchanged backend behavior, actual backend CI resource capability.

Reuse the test datasource/transaction seams with owned catalog allocation and
cleanup. Preserve shared rows/history. Enables Behavior 14.

[Detailed isolation brief and failed-premise evidence](final-schema-briefs.md#isolate-the-populated-migration-proof-from-shared-test-fixtures).

### 14. Require deliberate cleanup readiness while preserving asked evidence
Type: Behavior
Status: done
Proof: real populated MySQL default-off refusal/no-op, enabled retry and exact asked-graph preservation; full backend verification.

Guard cleanup readiness before the separate gated DML records success. Keep
all profile defaults off and preserve every prompt-referenced question.
Stop safely after branch publication; no intermediate production release.

[Detailed implementation brief and settled isolated-engine setup](final-schema-briefs.md#require-deliberate-cleanup-readiness-while-preserving-asked-evidence).

[Accepted populated cleanup proof](cleanup-readiness-verification.md).

### 15. Preserve learner evidence through guarded physical schema retirement
Type: Behavior
Status: done
Proof: complete canonical populated upgrade, pre-ALTER refusal and retained graph/product behavior; actual checkpoint C publication/all-active/schema/smokes.

Guard the physical column/index drop, extend the same pending harness, and
regenerate the ERD. Review exact C landing/tag plus deliberate production gate
enablement before live action. Retain pending tooling through actual C.

[Implementation brief](final-schema-briefs.md#preserve-learner-evidence-through-guarded-physical-schema-retirement). [Local proof](contract-retirement-verification.md). [C rollout](checkpoint-c-verification.md).

### 16. Keep normal startup and fresh upgrades after cleanup
Type: Behavior
Status: done
Proof: current local and fresh hosted canonical replay with retained product behavior; observed neutral-startup publication and separate final sweep reading.

After C, retire the spent cleanup guard/DML, harness and profile placeholder;
restore normal production startup. Keep the immutable DDL migration, verify
fresh upgrades and observe the reviewed cleanup publication before closure.

[Detailed implementation brief and lifecycle](final-schema-briefs.md#keep-normal-startup-and-fresh-upgrades-after-cleanup); [prepared local proof and remaining observations](neutral-startup-verification.md).

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

### G4. Observe checkpoint B retention rollout
Reported: slice 12 — "B remains ROOT-OWNED/unobserved: human-reviewed exact retained landing/tag/SHA, actual publication/every active unmapped binary, production applied FK migration and smokes."
Story clause: "Full retention acceptance includes confirmation of the FK-drop migration"
Disposition: proved by slice 12: checkpoint-b-verification.md; approved exact retained landing/tag, actual publication, every active unmapped backend, applied migration 357 and public/manual login smokes

### G5. Observe checkpoint C guarded schema rollout
Reported: slice 13 — "Acceptance reading: physical retirement, deliberate production enablement and actual checkpoint C are not observed by the local cleanup-readiness slice."
Story clause: "Then clean unreferenced questions and drop the column."
Disposition: proved by slice 15: checkpoint-c-verification.md; exact approved retained landing/tag, actual publication/all-active enabled JVM/applied358–360/public observations and developer-confirmed fresh sign-in/notebook list

### G6. Establish populated cleanup proof after the shared-catalog failure
Reported: slice 13 — "Full-suite native 34545 stopped both new cases before DML because unrelated promptless rows existed; populated cleanup preservation remains unobserved."
Story clause: "Then clean unreferenced questions and drop the column."
Disposition: proved by slice 14: cleanup-readiness-verification.md; real canonical default-off no-op/refusal, enabled repair/retry, exact selected IDs and complete asked graph passed in full backend native 6621

### G7. Observe hosted isolated-catalog capability
Reported: slice 13 — "The local owned-catalog proof and YAML checks do not observe GitHub root authentication, named-account bootstrap, credential transfer and catalog grant/revoke/drop."
Story clause: "Then clean unreferenced questions and drop the column."
Disposition: proved by slice 13: upgrade-isolation-verification.md#hosted-resource-proof; exact run 43429706691/1 backend job 131899854654 and its uploaded isolation-file case prove the hosted setup and target lifecycle


### G8. Retire applied cleanup tooling and observe neutral startup
Reported: slice 15 — "Prepared C uses an enabled JVM gate; spent guard, DML, harness, profiles, exclusive CI capability and the running override still need post-application retirement."
Story clause: "Then clean unreferenced questions and drop the column."
Disposition: proved by slice 16: neutral-startup-verification.md; prepared local/current and actual hosted fresh replay, exact approved neutral landing/tag, actual publication/every-active neutral JVM/schema/public observations and developer-confirmed fresh sign-in/notebook list
Commitment: C93
