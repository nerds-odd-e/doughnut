# Implementation briefs

The live Type, Status and Proof fields remain in [PLAN.md](PLAN.md).
These bodies retain the detailed instructions for its ordered leaves.

## Establish stored question ownership before changing persistence

Under an owner-authorized production read, count `recall_prompt` grouped by MCQ
with `COUNT(DISTINCT memory_tracker_id) > 1`; also count unreferenced MCQs and
inspect the relevant FK delete rules/version. Reuse matching retained observations
where available. No production writes or private question text. Zero cross-tracker
sharing permits the planned ownership transition; otherwise stop dependent work
and refine preservation of those existing attempts before any cleanup. This is
an early probe because only the credentialed persistent store can settle it.

## Use the current tracker note for prompt context

Given an answered prompt whose tracker moves, opening its history/conversation,
contesting or regenerating uses the tracker's current note. Derive once through
the prompt; remove direct question-note reads in conversation subject/title and
AI context callers, including recall-prompt read authorization in
`AuthorizationService`. Delete the direct-MCQ authorization case when its
exclusive consumers go in slice 6. Preserve property focus and owner/read
authorization.
Keep the old storage write until the compatibility release is deployed.

## Export recent questions from trackers currently on the note

After either move, exporting the current note includes the moved question and
recorded selection. Query through prompts/trackers, including property focus;
do not predicate on the question's stored note. Replace cross-tracker shared-MCQ
fixtures with separate questions, retaining real privacy/access expectations.
Same-tracker repeated-prompt cases remain valid unless naturally redundant.
Convert promptless ordering/key/privacy fixtures to tracker-owned questions.
Delete `questionsWithoutPromptsAreIncluded` and its exclusive setup as bank
behavior, without replacing it with an absence assertion.

## Keep test setup stable while separating its domain responsibilities

Enables the next Behavior, slice 6, together with slice 5: reorganize the 466-line
`e2e_test/start/testability.ts` along its existing domain mixin seams. Extract
note/relationship seeding and conversation setup into cohesive sibling modules;
keep facade names/callers stable and every edited module under 250 lines.
Retain the question helpers only until their deletion in slice 6. Do not add
another fixture framework or change setup semantics.

## Keep Git move and learning-identity proof in cohesive test homes

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

## Keep ordinary recall and note actions through question-bank deletion

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

Execution closure found `NoteQuestionGenerationService.buildQuestionGenerationRequest`
has only test callers after bank export deletion; runtime generation and batch
rendering call `QuestionGenerationRequestBuilder` directly. Delete those wrappers
and the bank-export shape comparison. Before editing the 377-line
`NoteQuestionGenerationRequestTests`, separate general request context/model/scoped
instructions from its property-type instruction scenarios into cohesive sibling
homes under 250 lines. Both use the existing request builder directly; preserve
every surviving input/assertion, reuse existing setup, add no production API or
fixture framework. Update the other request-test callers and run the full backend
suite. This is required consumer closure within slice 6, not new behavior.

Replace `Mcq.note`/`Note.mcqs` with the minimum temporary scalar mapping of
`note_id`, populated from the generation input for original binaries. It must
not hydrate a Note or act as attribution. Retain the FK/physical column here.
Regenerate OpenAPI/client and `components.d.ts`; do not hand-edit generated output.
This cohesive transitive removal has no green partial frontend/backend boundary;
budget about 8–10 minutes editing after slices 4 and 5, with verification wait
recorded separately.
If edits exceed 10 minutes, stop and refine this boundary before continuing.

## Persist each selected question with its prompt

Persist only the final selected question with its prompt in the existing write
transaction; a duplicate recheck returns the existing prompt without persisting
a discarded candidate. Preserve regeneration behavior without saved intermediate
questions. Delete the intermediate-row lookup/assertions in
`McqTest.AutoEvaluateAndRegenerate`; keep the selected question result and the
controller proof for regeneration of an actually asked prompt. Keep the
compatibility scalar write until slice 9.

## Delete learning questions through their tracker lifecycle

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

The 240-line `MemoryTrackerService` keeps its public deletion facade. Move its
existing tracker/prompt deletion and conversation-detachment responsibility
into a focused lifecycle collaborator before adding question cleanup. Keep
grading, assimilation and history in their present homes; the collaborator
owns scalar selection, removal/flush and unreferenced-question cleanup through
one transactional lifecycle. The note boundary invokes that same owner.
This cohesive current-responsibility seam keeps all edited homes under 250
lines and preserves existing controller proof; it is not a new fixture framework.

Checkpoint A follows: publish the complete compatible application through the
[release checkpoint procedure](operational-policy.md#release-checkpoints-and-execution-authority). Stop before slice 9 until every active
instance runs this scalar-write/current-note-reader version.

## Preserve answered questions across tracker moves and old-note deletion

Remove scalar note mapping/writes and remaining compatibility-only dependencies.
Adapt `CommittedUserCleanup` at this boundary: capture question IDs through
prompts/trackers before deleting prompts, then clean those unreferenced questions.
Its current native join through `mcq.note_id` cannot identify the new writes.
Preserve committed-fixture isolation and collect IDs before their ownership links
are destroyed; do not add a parallel cleanup framework.
Also settle G3's `NoteDependentRowsControllerTestBase` consumer: preserve its
ordinary deletion/retry observations using tracker/prompt ownership rather than
the physical question-note field. Keep complete fixture evidence and active
deletion assertions; add no retired-column absence proof.
Ship a compatible migration dropping `fk_mcq_note`, while retaining nullable
physical `note_id` for checkpoint A binaries during overlap. Questions now
survive reduction/reification and old-note removal through one ownership rule.
The retention promise is accepted after the FK-drop migration is confirmed;
ApplicationReadyEvent means startup/health alone is insufficient evidence.
Checkpoint B follows: stop before slice 10 until every active instance has no
question-note mapping/writes and the FK migration is applied.

## Preserve learner evidence through the final schema upgrade

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
