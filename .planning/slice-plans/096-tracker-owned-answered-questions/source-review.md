# Source-only review

Reviewer launch: `/root/source_review`, a fresh reviewer spawned with
`fork_turns: "none"`. The reviewer was instructed to use only the original
source text supplied in its launch, read no project files and change nothing.
Its input supplied the selected story’s Goal, owner direction, Scope, scope
limits, key examples, ownership and identity qualifications, rollout sequence,
and removal instructions. No plan, implementation return, product, proof,
suspected omission or current-slice scope was supplied.

The actual launch/input and numbered reviewer return are inspectable in this
execution conversation at the `/root/source_review` task launch and its final
return. The commitments below retain every individual returned commitment,
qualification, trigger/journey example and exclusion. List numbering and bold
formatting are represented by the Id column and ordinary table text; the
reviewer’s wording is retained. Allocations identify the output that owes each
commitment; empty observation cells claim no proof. Slice 1’s read-only premise
probe has no source commitment allocated to it.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| C01 | C01 — Defect characterization: Losing an answered question while retaining its tracker is a defect. | slice 9 | |
| C02 | C02 — Ownership: Every question belongs only to the tracker through which it was asked. | slice 9 | |
| C03 | C03 — Movement: An answered question stays with its tracker wherever that tracker moves. | slice 9 | |
| C04 | C04 — Origin-note reduction: Reducing the note that generated the question preserves the attempt’s question while its tracker survives. | slice 9 | |
| C05 | C05 — Origin-note removal: Removing the note that generated the question preserves the attempt’s question while its tracker survives. | slice 9 | |
| C06 | C06 — Prompt lifetime: An answered prompt lasts as long as its tracker. | slice 9 | |
| C07 | C07 — Question lifetime: The prompt’s question lasts as long as its tracker. | slice 9 | |
| C08 | C08 — Choices: The question’s choices remain with the answered prompt. | slice 9 | |
| C09 | C09 — Recorded answer: The recorded answer remains with the answered prompt. | slice 9 | |
| C10 | C10 — Grade: The grade remains with the answered prompt. | slice 9 | |
| C11 | C11 — Outcome: The outcome remains with the answered prompt. | slice 9 | |
| C12 | C12 — Ambiguity exclusion: The ambiguity exclusion remains with the answered prompt. | slice 9 | |
| C13 | C13 — Tracker deletion: Removing a tracker removes its prompts. | slice 8 | |
| C14 | C14 — Question deletion: Removing a tracker removes its prompts’ questions. | slice 8 | |
| C15 | C15 — Deletion ownership: Deletion remains application-owned. | slice 8 | |
| C16 | C16 — Note attribution: A question’s note is its tracker’s current note. | slice 2 | observed: RecallPrompt.getNote derives requireMemoryTracker().getNote(); RecallPromptAnswerControllerTest.attributesTheAnswerToTheTrackersCurrentNote observes current topology ID after real reification; full backend pass 2026-11-13 01:57 UTC. |
| C17 | C17 — Recall history: Recall history uses the tracker’s current note. | slice 2 | observed: MemoryTrackerRecallHistoryControllerTest.readsAnsweredHistoryAlongsideTheTrackersCurrentNote observes current note through showMemoryTracker plus retained answered MCQ in history after real reification; final full backend pass. |
| C18 | C18 — Connected assistant answers: Connected assistant recall answers use the tracker’s current note. | slice 9 | |
| C19 | C19 — Prompt conversations: Conversations about a prompt use the tracker’s current note. | slice 2 | observed: ConversationStartControllerTest.usesTheTrackersCurrentNoteAsSubject; ConversationListingControllerTest.titlesRecallConversationsWithTheTrackersCurrentNote; ConversationAiReplyControllerTest.usesTheTrackersCurrentNoteForAiContextAndCorrections observes exact focus-note title and correction note ID after real reification; final full backend pass. |
| C20 | C20 — Contest context: Contest context uses the tracker’s current note. | slice 2 | observed: RecallPromptContestControllerTest.contestsAgainstTheTrackersCurrentNote observes exact focus_note title in mocked external AI request after real reification; final full backend pass. |
| C21 | C21 — Regeneration context: Regeneration context uses the tracker’s current note. | slice 2 | observed: RecallPromptRegenerateControllerTest.shouldPassOldQuestionAndContestResultToOpenAiApi observes exact current focus_note title and retained regeneration inputs after real reification; final full backend pass. |
| C22 | C22 — Recent-question attribution: A note export’s recent questions use the tracker’s current note. | slice 3 | observed: NoteExportControllerMovedQuestionsTest.exportsTheMovedTrackersQuestionAndOwnChoiceOnItsCurrentNote: real reification then numbered question and recorded selection from current-note export; full backend pass. |
| C23 | C23 — Export selection: Recent-question export lists questions asked through trackers currently on the exported note. | slice 3 | observed: McqRepository.findRecentForNote uses prompt/current-tracker EXISTS; RecentQuestionsTest.anotherNotesQuestionsAreExcluded and PropertyHistoryTests observe note/focus membership through export; full backend pass. |
| Q01 | Q01 — Property focus: Recent-question export keeps property focus unchanged. | slice 3 | observed: NoteExportControllerPropertyHistoryTests exact key/value binary matching and distinct-newest-ten plus PropertyTests duplicate-occurrence/shared-history HTTP observations; full backend pass. |
| C24 | C24 — Bank removal: Delete the note question bank transitively. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C25 | C25 — Add Question: Delete the bank’s Add Question capability. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C26 | C26 — Questions list: Delete the note’s Questions list. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| Q02 | Q02 — Owner’s stated rationale: No learner is ever asked the note-bank questions. | slice 6 | observed: RecallPromptService.generateNewRecallPrompt plus question-bank-verification.md#browser-and-compatibility-observation: ordinary recall generates and asks its tracker question; owner rationale retained in selected source |
| Q03 | Q03 — Resulting representation: With the bank removed, no question needs its own note. | slice 6 | observed: Mcq scalar compatibility mapping and RecallPrompt.getNote current-tracker attribution; question-bank-verification.md#browser-and-compatibility-observation; later storage retirement remains slices 9/10 |
| C27 | C27 — Questions entry: Delete the frontend Questions entry in `NoteMoreOptionsActions.vue`. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C28 | C28 — MCQ page: Delete `Mcqs.vue`. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C29 | C29 — Add-question component: Delete `NoteAddQuestion.vue`. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C30 | C30 — Export dialog: Delete `QuestionExportDialog.vue`. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C31 | C31 — Generation-request export: Delete the generation-request export that lives only on that page. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C32 | C32 — Removed frontend specifications: Delete the removed bank frontend’s specifications. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C33 | C33 — MCQ test support: Delete `mcqsTestSupport.ts`. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C34 | C34 — Component declarations: Delete the corresponding `components.d.ts` entries. | slice 6 | observed: question-bank-verification.md#frontend-proof: Vite-generated declarations, full mounted suite and final typechecks |
| C35 | C35 — Controller: Delete `McqController`. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C36 | C36 — List endpoint: Delete its list operation. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C37 | C37 — Add endpoint: Delete its add operation. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C38 | C38 — Refine endpoint: Delete its refine operation. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C39 | C39 — Generate endpoint: Delete its generate operation. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C40 | C40 — Export endpoint: Delete its export operation. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C41 | C41 — Exclusive dependencies: Delete dependencies used exclusively by the removed bank. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C42 | C42 — Add service method: Delete `McqService.addQuestion`. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C43 | C43 — Refinement service method: Delete `McqService.refineAIQuestion`. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C44 | C44 — AI refinement method: Delete `AiQuestionGenerator.getAiGeneratedRefineQuestion`. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C45 | C45 — Refinement generation method: Delete `NoteQuestionGenerationService.refineQuestion`. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C46 | C46 — Refinement prompt: Delete its prompt. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C47 | C47 — Unused generation overloads: Delete unused `getAiGeneratedQuestion` overloads. | slice 6 | observed: AiQuestionGenerator.java: retained four-argument generation entry; retired overload consumer conversion inspected with full backend proof in question-bank-verification.md |
| C48 | C48 — Note collection: Delete `Note.mcqs`. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C49 | C49 — Note-based repository query: Delete `McqRepository.findTop10ByNote_Id…`. | slice 3 | observed: Retained current-tracker query/controller selection and ordering proof; consumer-trace directed inspection and separate retirement reading recorded in recent-question-verification.md. |
| C50 | C50 — Controller tests: Delete `McqControllerTests`. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C51 | C51 — Refinement controller tests: Delete `McqRefinementControllerTests`. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C52 | C52 — Transactional ratchet entries: Delete `McqController` entries in `OpenAiTransactionalRatchetTest`. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C53 | C53 — Stored note mapping: Delete `Mcq.note`. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C54 | C54 — Stored note column: Delete `mcq.note_id` through the stated rollout sequence. | slice 10 | |
| C55 | C55 — Stored note foreign key: Delete its foreign key through the stated rollout sequence. | slice 9 | |
| C56 | C56 — Unreferenced-question migration: Before dropping the column, migrate by deleting questions no prompt uses. | slice 10 | |
| C57 | C57 — Generation input: Generation takes the tracker’s note as input. | slice 7 | |
| C58 | C58 — Question representation: Generation does not store its own note on the question. | slice 9 | |
| C59 | C59 — MCQ injection: Delete testability/E2E `injectMcq`. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C60 | C60 — MCQ test data: Delete `McqsTestData`. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C61 | C61 — Notebook injection: Delete `injectMcqsToNotebook`. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C62 | C62 — Number-note injection: Delete unused `injectYesNoQuestionsForNumberNotes`. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C63 | C63 — Management feature: Delete `mcq_management.feature`. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C64 | C64 — Add-question page object: Delete `addQuestionPage.ts`. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C65 | C65 — Related steps: Delete related steps. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C66 | C66 — Related page methods: Delete related page methods. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C67 | C67 — Isolated Cypress selection: Delete related isolated-Cypress spec selection. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C68 | C68 — Mock references: Delete related mock references. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C69 | C69 — Optimization candidate: Delete `mcq-creation-journey` from `TEST-OPTIMIZATION-CANDIDATES.md`. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C70 | C70 — Product documentation: Remove documentation describing the bank. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C71 | C71 — Agent guidance: Remove agent guidance describing the bank. | slice 6 | observed: question-bank-verification.md#removal-reading: inspected transitive deletion and classified whole-product reading, with retained behavior proof in the same record |
| C72 | C72 — OpenAPI document: Regenerate `open_api_docs.yaml`. | slice 6 | observed: question-bank-verification.md#accepted-backend-and-generator-proof: source-generated OpenAPI/client and passing validation/backend consumer proof |
| C73 | C73 — Generated API package: Regenerate `packages/generated/donut-backend-api`. | slice 6 | observed: question-bank-verification.md#accepted-backend-and-generator-proof: source-generated OpenAPI/client and passing validation/backend consumer proof |
| Q04 | Q04 — Ownership cardinality: One question belongs to one tracker. | slice 9 | |
| Q05 | Q05 — Prompt sharing qualification: That ownership rule does not independently forbid multiple prompts on the same tracker from referring to that question. | slice 8 | |
| C74 | C74 — Cross-learner export fixtures: Fixtures that share a question across learners must instead use separate tracker-owned questions. | slice 3 | observed: QuestionPrivacyTest.newerOtherLearnerAttemptDoesNotSubstitute and PropertyPrivacyTests.focusedRequestUsesLatestOwnAnswerAndKeepsCompleteAppendixOutsideBudget arrange distinct MCQs for each learner tracker; full backend pass. |
| C75 | C75 — Fixture privacy: Those fixtures preserve privacy. | slice 3 | observed: QuestionPrivacyTest and PropertyPrivacyTests observe own selections, other-answer non-disclosure, guest behavior and access refusal through real export; full backend pass. |
| C76 | C76 — Tracker identities: Keep existing tracker identities. | slice 9 | |
| C77 | C77 — Prompt identities: Keep existing prompt identities. | slice 9 | |
| C78 | C78 — Answer identities: Keep existing answer identities. | slice 9 | |
| C79 | C79 — Log identities: Keep existing log identities. | slice 9 | |
| C80 | C80 — Prompt boundary: Reuse the existing prompt persistence boundary. | slice 7 | |
| C81 | C81 — Note boundary: Reuse the existing note deletion boundary. | slice 8 | |
| C82 | C82 — Tracker boundary: Reuse the existing tracker deletion boundary. | slice 8 | |
| E01 | E01 — Relationship reduction journey: A learner answers MCQ index 2 incorrectly on relationship note R’s note-level tracker; the owner reduces R to a property of source S; R is permanently removed. | slice 9 | |
| E02 | E02 — History result for that journey: Recall history still shows the question under S. | slice 9 | |
| E03 | E03 — Assistant result for that journey: `list_recall_answers` still shows the question under S. | slice 9 | |
| E04 | E04 — Choices in that journey: The choices remain visible. | slice 9 | |
| E05 | E05 — Answer in that journey: The recorded index 2 remains visible. | slice 9 | |
| E06 | E06 — Reverse movement journey: A property tracker on S with an answered question moves onto a new relationship note R; S is subsequently permanently removed. | slice 9 | |
| E07 | E07 — Reverse movement result: The answered question remains under R. | slice 9 | |
| E08 | E08 — Contest journey: Following E01, contesting the moved tracker’s question uses S. | slice 9 | |
| E09 | E09 — Conversation journey: Following E01, opening a conversation about the moved tracker’s question uses S. | slice 9 | |
| E10 | E10 — Export journey: Following E01, exporting S with recent questions includes the moved tracker’s answered question. | slice 9 | |
| E11 | E11 — Exported choice: That export includes the learner’s choice. | slice 9 | |
| E12 | E12 — Unmoved tracker journey: Removing a note whose tracker never moved removes that tracker. | slice 8 | |
| E13 | E13 — Unmoved tracker prompts: That removal removes its prompts. | slice 8 | |
| E14 | E14 — Unmoved tracker questions: That removal removes its questions. | slice 8 | |
| X01 | X01 — Lost production questions: Restoring the 2,197 lost questions is outside scope; production has no copy. | slice 10 | |
| X02 | X02 — Merged tracker history: Merging relationship-note tracker history that reduction drops because the learner already tracks that focus is outside scope. | slice 10 | |
| X03 | X03 — Scheduling changes: Changing scheduling is outside scope. | slice 10 | |
| X04 | X04 — Statistics changes: Changing statistics is outside scope. | slice 10 | |
| X05 | X05 — Archive: Add no archive. | slice 10 | |
| X06 | X06 — Content revisions: Add no content revision system. | slice 10 | |
| X07 | X07 — Database cascades: Add no new database cascade. | slice 10 | |
| C83 | C83 — Instance compatibility: Preserve compatibility between two app instances during rollout. | slice 10 | |
| C84 | C84 — Compatibility attribution: The compatibility release derives attribution through the tracker. | slice 8 |  |
| C85 | C85 — Compatibility write: That release retains a scalar note-ID write. | slice 6 | observed: question-bank-verification.md#browser-and-compatibility-observation: current fresh read and matching persisted scalar/input/joined note; original-binary launch explicitly unobserved, checkpoint A remains slice 8 |
| Q06 | Q06 — Compatibility consumer: The retained write serves the original eager ORM readers. | slice 6 | observed: question-bank-verification.md#browser-and-compatibility-observation: current fresh read and matching persisted scalar/input/joined note; original-binary launch explicitly unobserved, checkpoint A remains slice 8 |
| C86 | C86 — Mapping removal timing: After the compatibility release is fully deployed, remove the mapping. | slice 9 | |
| C87 | C87 — Write removal timing: At that stage, remove the scalar write. | slice 9 | |
| C88 | C88 — Foreign-key removal timing: At that stage, drop the foreign key. | slice 9 | |
| C89 | C89 — Interim physical column: Retain the physical nullable column until all mapped binaries have been replaced. | slice 9 | |
| C90 | C90 — Final question cleanup: Then clean unreferenced questions. | slice 10 | |
| C91 | C91 — Final column cleanup: Then drop the column. | slice 10 | |
| Q07 | Q07 — Source’s retention qualification: The source explicitly makes full retention acceptance dependent on confirmed foreign-key migration. | slice 9 | |
| Q08 | Q08 — Source’s final cleanup stage: The source identifies column drop as the final cleanup stage. | slice 10 | |
| C92 | C92 — Temporary representation: Delete the temporary compatibility representation in that sequence. | slice 10 | |
| X08 | X08 — Final compatibility guidance: Retain no final product guidance about that temporary representation. | slice 10 | |
| X09 | X09 — Absence tests for compatibility: Retain no final tests asserting that representation’s absence. | slice 10 | |
| C93 | C93 — Transitive cleanup: Perform removal transitively. | slice 10 | |
| C94 | C94 — Product sweep: Sweep the whole product. | slice 10 | |
| X10 | X10 — Absence tests: Retain no tests asserting removed things are absent. | slice 10 | |
| X11 | X11 — Runtime guards: Retain no runtime guards against removed things. | slice 10 | |
| X12 | X12 — Replacement history notes: Retain no replacement history notes about removed things. | slice 10 | |
| C95 | C95 — Surviving-behavior proof: Use proof of surviving behavior. | slice 10 | |
| C96 | C96 — Sweep reading: Use one acceptance sweep reading alongside that proof. | slice 10 | |
| C97 | C97 — Dated evidence: Preserve dated evidence in `.planning/done` as recorded. | slice 10 | |
