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
compatibility scalar write until slice 12.

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

## Keep recall history proof in cohesive test homes

Enables Behavior 12 with unchanged export behavior. Split the 358-line
`UserRecallHistoryExportControllerTest` into recall-event content/state and
export envelope/access/daily-probe homes. Put existing common setup and
`answerMcqAtDay` in one focused shared base; retain all 14 existing test bodies
and assertions: seven event cases in `UserRecallHistoryEventsExportControllerTest`,
seven envelope/access/probe cases in the existing class, and common setup in
`UserRecallHistoryExportControllerTestBase`. Keep every home under 250 lines. Add no fixture framework or
product behavior. Budget 3–5 minutes editing/cleanup plus full backend proof.

## Create test questions through their tracker and prompt

Enables Behavior 12 with unchanged behavior. Remove `McqBuilder.forNote`,
`RecallPromptBuilder.withMcqForNote` and exclusive Note parameters in
`MemoryTrackerControllerTestBase` prompt helpers and
`RecallPromptControllerTestBase.mcqPrompt`; adapt every consumer (41
`withMcqForNote` homes and 14 `forNote` homes overlap). Keep production
mapping/writer/FK/column unchanged. Retain `RecallPromptBuilder`'s compatibility
scalar fallback **and save** while mapping exists; prompts created before actual
moves naturally keep their old input. Preserve the two opposite-ownership
fixtures in `AuthorizationServiceTest:52–68` with local explicit compatibility
scalar setup until Behavior 12. Do not erase their current-note access proof.
Check committed raw-MCQ fixtures stay isolated or have owning prompts; introduce
no orphan. Budget 5–8 minutes editing/cleanup plus full backend proof.

## Keep destruction proof tied to the selected questions

Enables Behavior 12; settles G3 while production stays unchanged. Adapt the
70-line `NoteDependentRowsControllerTestBase`, its 12 external Git/note/folder/
retry/atomic readers and `NotebookGitDeletionPublicationControllerTest:207`.
Capture specific question IDs through trackers/prompts **before** destruction;
carry the same IDs into after-delete/retry/rollback observations. Discovery after
deletion is vacuous. Use explicit inputs, with no lazy cache or snapshot that
silently captures after deletion. Preserve complete dependent fixture proof.
`CommittedUserCleanup` captures distinct selected IDs before prompt deletion,
then deletes only those with no surviving prompt. Its affected graph includes
matching tracker users **or** trackers on matching users' owned notes: later
note deletion cascades other learners' trackers too. Share one native FROM/
predicate between question preselection and prompt deletion; left-join optional
note/ownership paths so circle-owned or nullable-note trackers keep coverage.
Reuse `McqRepository.deleteUnreferencedByIds` at the existing question step;
inject it through `SpringTestBase` and thread it into the twelve cleanup callers.
Keep the remaining cleanup order/conversation semantics. Add one focused cleanup
graph case (red/green full suite): target learner on an external circle note,
another learner on a target-owned note, and an unrelated surviving question.
Give the second question a surviving input-note scalar to expose cleanup without
FK masking; retire that physical fixture in 12. Preserve committed isolation;
add no absence guard or fixture framework. Budget 5–8 minutes editing/cleanup;
full-suite red/green waits are the stated indivisible proof exception.

## Preserve answered questions across tracker moves and old-note deletion

Before production transition, add maintained red/green regressions for both
journeys and obtain the red full-suite result with compatibility mapping/FK
still in place; then change production/migration and obtain green. Exercise the
real journeys: reduce answered relationship R to source S's property (R is
deleted), and reify answered S property to relationship R, then really trash/
permanently delete S. Flush/clear before real history, MCP and export reads.
Observe original tracker/prompt/answer/log IDs, question/choices/index `2`, wrong
grade/outcome, ambiguity exclusion and schedule; canonical payload once, inverse
delta separately. Retain current-note authorization/context and ordinary deletion
proof. A focused MCP class reuses `ListRecallAnswersMcpEndpointTestBase` (91
lines). `NoteExportController` stays package-private: use existing MockMvc and
bearer GET `/api/notes/{id}/ai-context-markdown`, without widening visibility.

Remove scalar mapping/writer and remaining exclusive Note input in
`GeneratedQuestionPostProcessor`, `AiQuestionGenerator` and batch callers.
Retire fixture fallback/save, explicit authorization/cleanup physical setup and selected-
write getter assertions while retaining surviving payload/access proof. Drop
**only** `fk_mcq_note` in a collision-checked migration above actual tip
`300000356`; keep nullable physical column/index. Check both SQL/Java migration
tips and collision sources at execution. Fresh unmapped questions get
NULL `note_id`: green fresh rows alone cannot prove the legacy FK transition.
Observe a positive populated legacy asked row with nonnull original `note_id`
through real move/delete/read on the migrated schema, using a disposable
migration observation or pending fixture. Retire that physical fixture after
checkpoint B and before 13; create no permanent column/FK absence assertion.
Run full backend red/green, `backend:verify` and explicit worktree-schema ERD
regeneration. Budget 8–10 minutes editing/cleanup; stop/refine on edit overrun.

Checkpoint B follows the human-reviewed retained landing and exact new stable
tag/SHA. Stop before 13 until actual publication, every active unmapped binary,
applied FK drop and production smokes are observed. ApplicationReadyEvent makes
health/startup insufficient. Retention acceptance waits for that migration.

## Preserve learner evidence through the final schema upgrade

[Refined cleanup, guarded retirement and post-application lifecycle briefs](final-schema-briefs.md).
