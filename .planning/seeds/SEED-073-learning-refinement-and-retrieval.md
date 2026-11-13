---
id: SEED-073
status: dormant
planted: 2026-10-12
planted_during: owner-directed backlog shaping after the OpenKB implementation assessment
trigger_when: improving note quality and retrieval quality through everyday learning and use
scope: large
---

# SEED-073: Improve knowledge through learning, refinement, and retrieval

## Epic Outcome

For people learning from deliberately authored Donut notes, a confusing recall answer or conversation should lead to better understanding or a reviewed note correction, and later questions should find and use the relevant evidence. Preserve the learner's history and the author's control over knowledge changes.

This epic brings together learning and refinement, chat, and evidence discovery.

## Why This Matters

The owner wants to borrow OpenKB ideas for improving and using accumulated knowledge. Donut's starting point remains a person noticing and capturing something worth learning; bulk document compilation is not the priority.

The 2026-10-12 assessment read local main at `e851a7682d`, including code, tests, and Accepted ADRs. It did not run the app or measure model quality:

- Answered MCQs already open refinement with the question and tested focus,
  but that refinement input omits the learner's selected answer.
- Refinement already decomposes content and previews extraction; contesting a
  question and conversing about a recall prompt are existing actions.
- Chat already proposes title/content updates for acceptance. The recall-chat
  proposal handoff needs verification: frontend acceptance reads the direct
  note subject, while a recall conversation has a recall-prompt subject.
- A shared bounded focus context already serves question generation, chat,
  automation, exports, and the graph API. Task information does not guide
  candidate selection. Related-note bodies are truncated to short prefixes.
- Search matches titles and aliases. MCP lists those matches as candidates
  with notebook and folder, reads a chosen note in full with its notebook's
  accepted revision, and offers graph context; it does not search note bodies.
- Git preserves accepted notebook content; recall history preserves learner
  events. Neither alone records the diagnosis, proposal decision, and learning
  follow-up as one resolved difficulty.

These are starting observations, not promises that the current UI works in every cited journey. Verify the selected story's decisive premises when refining it.

## Alternatives and Decision

- **Defer everything:** Keeps the present disconnected actions and gives no
  feedback about whether a difficulty is caused by knowledge, understanding,
  the question, or retrieval.
- **Existing tools and manual editing:** The strongest smaller alternative is
  to discuss the question, manually edit the note, and continue recall. Reuse
  this workflow, but reduce the lost answer context and manual handoff, then
  make an accepted correction and its origin inspectable.
- **Build a general review queue or new chat system:** Premature. Start from
  an explicit confusing MCQ and the existing conversation/refinement surfaces.
- **Selected direction:** Diagnose learning difficulties before changing
  knowledge.

## Shared Constraints

- Assimilation creates a new memory tracker. Revisiting understanding uses
  the existing tracker and history; it does not silently reset or recreate it.
  Grading follows [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md).
- A wrong answer is evidence of a difficulty, not an established diagnosis.
  Learners can correct a suggested diagnosis. Ordinary forgetting does not
  automatically create a task or modify a note.
- Accepted content changes use the existing publication boundary and Git
  history under [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md).
  Use existing identities and revisions for attribution, not another content
  versioning system. Private learning evidence stays outside Portable content.
- Existing ownership applies to corrections. A learner who can read a shared
  note does not gain permission to edit it through these flows.
- Retrieved content is evidence, distinct from application instructions.
  Search, expansion, citations, and later reads respect the viewer's access.
- Bulk ingestion, embeddings, a universal review inbox, and automatic knowledge
  rewrites are outside this epic. Direct MCP and ChatGPT access follows the
  [hosted assistant setup guide](../../docs/assistant-oauth.md).

## Story Decomposition

<a id="keep-answered-questions-with-their-tracker"></a>

### Keep answered questions with their tracker

**Identity:** SEED-073#keep-answered-questions-with-their-tracker
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/096-tracker-owned-answered-questions/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"fa22a11e3f2c5e7fa64bcb54da4d1ef9997b8b0c51199206af8dc055ceed17c3","plan":"6bcca7d056f55d140c1e4c1864f97ffffb28ad5030a648794c4d37e9ab98e1d4"}}
```

**Goal.** As a Donut learner, every question I answered stays with the memory
tracker that recorded my answer, wherever that tracker moves, so reducing or
removing the note the question was generated from never erases my attempt's
question while its grade and schedule survive. Every question belongs only to
the tracker it was asked through.

**Owner direction (2026-11-13).** Losing the question while keeping the
tracker is a defect, not intended behavior. A question should belong only to
its tracker. The owner chose to delete the note question bank (Add Question
and a note's Questions list), because no learner is ever asked those
questions; with it gone, no question needs its own note.

**Current design (analysis, local main `4ede0b87d1`).**

- Current recall-generation, batch-import and contest-and-regenerate paths
  create a fresh question for a prompt on one tracker. This is a caller
  property, not a schema uniqueness guarantee: `RecallPrompt.mcq` is
  many-to-one. Its stored note (`mcq.note_id`) copies the tracker's note at
  creation and goes stale when the tracker moves.
- `mcq.note_id` cascades on note delete, and `recall_prompt.mcq_id` cascades
  on MCQ delete (`V300000325`), so a prompt dies with the question's stored
  note, not with its tracker. Both tracker moves leave questions behind:
  `rehomeNoteLevelTrackersToProperty` (reduction, then permanent removal of
  the relationship note, the confirmed production cause) and
  `movePropertyTrackersOntoRelationshipNote`. Production held 3,538 prompts
  whose question's note differs from their tracker's note.
- Questions without a prompt include trainer-added ones from a note's
  Questions page (`McqController.add`, `NoteAddQuestion.vue`, typed or
  AI-drafted and refined). They came from the note question bank that
  notebook assessments used; approval and assessments were deleted on
  2026-05-07 (`545681f2f6`, `d460611a64`). Today the trainer-added questions
  are only listed on the Questions page and included in recent-question
  exports. Generation also saves before the transactional duplicate-prompt
  recheck, and configurable regeneration saves intermediate results; either
  can leave unreferenced questions. Production's
  `question.regeneration.times: 0` addresses only the latter path.
- Code reads the stored note as the prompt's note: `RecallPrompt.getNote`,
  `Conversation` subject note, `ConversationService` title, contest and
  regenerate context (`RecallPromptController`, `McqService.contest`), and the
  recent-questions export (`McqRepository.findTop10ByNote_Id…`,
  `findRecentForProperty`).

**Production evidence (2026-11-13 SGT).** Read-only MySQL transactions and
binary-log replay used the owner's signed-in `gcloud` access, through
`doughnut-app-group-qk27` to Cloud SQL `doughnut-db`, database `doughnut`,
MySQL `8.4.11-google`. The deployed app reported revision
`12288107bd2a2dbbfc2eabc2d00f44e0d4ed4e74`; the relevant tracker-rehoming code
matched revision `b3f4ce10bd0d`. No production data was changed.

- At `2026-11-12 23:22:30 UTC`, a distinct-answer count joining `answer`
  through `recall_log`/`memory_tracker`, with `NOT EXISTS` on
  `recall_prompt.answer_id`, found **2,197** answers without a prompt, all on
  UNDERSTANDING trackers. Samples `246327`, `246452`, `246496` keep choice
  indexes `2`, `1`, `3` and AGAIN logs on trackers `19414`, `5943`, `3227`.
- Live FK metadata confirmed `note → mcq → recall_prompt` CASCADE deletion,
  and **3,538** stored prompts whose MCQ's note differed from their tracker's
  current note. Counts describe that snapshot, not every cause.
- UTC replay used `mysqlbinlog --read-from-remote-server
  --base64-output=DECODE-ROWS -vv --start-datetime='2026-11-12 01:12:00'
  --stop-datetime='2026-11-12 02:41:00' mysql-bin.000048` over an
  authenticated DB tunnel; only numeric row identities and transaction
  metadata were retained. Answer `246452` is attached to prompt `264841`, MCQ
  `258176`, tracker `5943` at `01:13:32 UTC` (position `7385375`, GTID suffix
  `54733`). At `01:13:43`, transaction `54749` moves that tracker from note
  `14696` to `2116` (position `7400392`) and deletes note `14696`
  (`7401416`). Answer `246496` is attached to prompt `264504`, MCQ `257912`,
  tracker `3227` at `02:38:22 UTC` (`8149412`, transaction `55305`). At
  `02:39:29`, transaction `55341` moves the tracker from note `13313` to
  `1657` (`8224209`) and deletes `13313` (`8225492`). These GTIDs share UUID
  `b5b67510-b8d5-11f1-b37b-42010a6f101b`.
- Limitation: the two traces support the reduction cause; they do not explain
  all 2,197 losses, and the loss of sample `246327` was not replayed.

**Removal rule — read before planning.** This story deletes a whole feature
and a lot of code with it. Follow the "Removals leave no trace" principle in
`CLAUDE.md`: delete each removed thing outright and transitively, and sweep
backend, frontend, CLI, MCP, docs, code comments, agent guidance and generated
artifacts so they read as if the question bank never existed. Keep no
historical record of it in the product: no "removed" or "no longer" notes, no
replacement documentation describing its absence. Do not replace deleted tests
or docs with negations: no test or assertion that Add Question, the Questions
list, its endpoints or the note column are gone, and no runtime guard against
their return. Just delete them. Git history is the record. Proof of the
removal is the surviving behavior's tests plus one sweep reading at
acceptance, recorded in the plan as a reading, not as a test. Dated evidence
in `.planning/done/` stays as recorded.

**Scope.**

- **Keep answered questions with their tracker.** An answered prompt, with its
  question, choices, recorded answer, grade, outcome and ambiguity exclusion,
  lasts as long as its tracker, whichever note the tracker moves to and
  whichever note is later removed. Removing a tracker still removes its
  prompts and their questions. Deletion stays application-owned; add no new
  database cascade.
- **A question's note is its tracker's note.** Recall history, connected
  assistant recall answers, conversations about a prompt, contest and
  regenerate context, and the note export's recent questions all use the
  tracker's current note. The recent-questions export lists questions asked
  through trackers now on the exported note (property focus unchanged).
- **Delete the note question bank, transitively,** including at least:
  - frontend: the Questions entry in `NoteMoreOptionsActions.vue`,
    `Mcqs.vue`, `NoteAddQuestion.vue`, `QuestionExportDialog.vue` (its
    generation-request export lives only on that page), their specs and
    `mcqsTestSupport.ts`, and `components.d.ts` entries;
  - backend: `McqController` (list, add, refine, generate, export) and what
    only it uses, such as `McqService.addQuestion`/`refineAIQuestion`,
    `AiQuestionGenerator.getAiGeneratedRefineQuestion`,
    `NoteQuestionGenerationService.refineQuestion` and its prompt, unused
    `getAiGeneratedQuestion` overloads, `Note.mcqs`,
    `McqRepository.findTop10ByNote_Id…`, `McqControllerTests`,
    `McqRefinementControllerTests` and `McqController`'s entries in
    `OpenAiTransactionalRatchetTest`;
  - the stored note on a question: `Mcq.note`, the `mcq.note_id` column and
    its foreign key, with a migration that deletes questions no prompt uses
    before dropping the column; question generation
    takes the tracker's note as input instead of storing it;
  - testability and E2E: `injectMcq`, `McqsTestData`,
    `injectMcqsToNotebook`/`injectYesNoQuestionsForNumberNotes` (already
    unused), `mcq_management.feature`, `addQuestionPage.ts`, the related
    step definitions and page-object methods, and the isolated-Cypress spec
    selection and mock references;
  - planning and guidance: the `mcq-creation-journey` entry in
    `.planning/TEST-OPTIMIZATION-CANDIDATES.md`, any docs or agent guidance
    describing the question bank, and regenerated `open_api_docs.yaml` and
    `packages/generated/donut-backend-api`.

**Scope limits.** Restoring the 2,197 questions already lost (production has
no copy), merging the history of a relationship-note tracker that reduction
drops because the learner already tracks that focus, and changing scheduling
or statistics are outside this story.

**Key examples.**

1. A learner answered an MCQ (index `2`, wrong) on relationship note R's
   note-level tracker. The owner reduces R into a property of source S, and R
   is permanently removed. The learner's recall history and
   `list_recall_answers` still show that question, its choices and index `2`,
   now under S.
2. A property tracker on S, with an answered question, moves onto a new
   relationship note R. S is later permanently removed. The tracker's answered
   question remains under R.
3. After example 1, contesting or opening a conversation about a question on
   the moved tracker uses S, not the removed R, as its note.
4. After example 1, exporting S with recent questions includes the moved
   tracker's answered question and the learner's choice.
5. Removing a note whose tracker never moved still removes the tracker with
   its prompts and questions.

**Refinement findings (2026-11-13, checkout `1e331f5c5c`).** The recorded
production replay remains applicable: the implicated tracker-move, question,
prompt and cascade definitions have not changed since the analyzed revision.
No new production access or model call was made. The strongest challenge is
that deleting the note association alone does not establish tracker ownership:
question cleanup must also follow direct tracker deletion, unanswered-prompt
replacement and permanent note deletion. Generation must persist the selected
question with its prompt, rather than leave discarded candidates behind.
These are lifecycle consequences of the existing goal, not additional features.

The owner-selected scope remains one outcome. A question belongs to one
tracker; that does not independently require forbidding several prompts on the
same tracker from referring to it. Export fixtures sharing a question across
learners must use separate tracker-owned questions while preserving their
privacy assertions. The scope limits above remain unchanged.

**Architecture.** The tracker is the single source of current note attribution;
the prompt carries its historical question and answer. Keep existing tracker,
prompt, answer and log identities. Reuse the current prompt-persistence and
note/tracker deletion boundaries for application-owned lifecycle management.
Do not add a parallel question archive, content revision system or database
cascade. This follows [ADR 0001, domain vocabulary](../../docs/adrs/0001-ubiquitous-language.md),
[ADR 0002, private learning data and content identity](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md),
[ADR 0003, scheduling and recorded attempts](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md),
and [ADR 0006, failure handling](../../docs/adrs/0006-failure-handling-accepted.md).
The current Accepted index and records agree; no conflict or exception is needed.

Rollout must preserve compatibility between the two application instances.
Original binaries read the question's stored note, including eager ORM
hydration; a nullable column alone does not make new questions readable by
those binaries. Use a compatibility release that derives attribution through
the tracker while retaining a scalar note ID write for original readers. After
that release is fully deployed, remove that mapping/write and drop its FK,
retaining the physical nullable column until all mapped binaries are replaced.
Then clean unreferenced questions and drop the column. This is three compatible
release stages under the [migration release-safety rule](../../.agents/skills/db-migration/SKILL.md#release-safety),
not authority to deploy them in this preparation. Full retention acceptance
includes confirmation of the FK-drop migration; final cleanup includes the
column-drop stage. The temporary compatibility representation is deleted in
that sequence and leaves no final product guidance or tests about its absence.

**Preparation follow-through (2026-11-13, checkout `0ec6e1dafa`).** Disposable
MySQL test-profile observations reached both real move/delete/history/MCP/export
journeys, scalar compatibility persistence with a fresh ORM load, and all three
question-deletion consumers with dependent rows. Inputs, results, failed legacy
consumers, restoration and limits are recorded in the
[active plan](../slice-plans/096-tracker-owned-answered-questions/PLAN.md#decisive-premises-and-observation).
The plan includes the newly observed Git test reorganization and consumer
closure. The selected goal, scope, examples and production rollout checkpoints
remain the preparation input.

**Effort hypothesis:** L for one story — several hours, mostly deletion;
confidence medium. Planning may split the tracker-ownership repair (examples
1–5) from the question-bank deletion if slices grow too large; the repair
comes first because it is the defect.

## Open Decisions

- Select real learner cases during evaluation; human comprehension,
  retention, and retrieval-quality gains remain unmeasured.
- Define how long proposal decisions and their private conversation evidence
  should remain available, using existing deletion/ownership rules.
- Automatic repeat-failure triggers remain a later decision.

**Learner-confirmed ambiguity exclusion (owner decisions, deferred):**

- Whether an attempt marked correct can be excluded. The operation accepts
  one today; only the tutor instruction keeps proposals to wrong marks.
- Whether a recall-prompt conversation with an audience beyond its learner
  should show the exclusion reason or offer the proposal. It is unchecked
  whether any such conversation exists.
- Whether a day whose only answer was excluded keeps its place in the review
  calendar and streak. Recall statistics leave an excluded attempt out
  entirely today.

## When to Surface

When choosing work for the new learning-to-refinement and knowledge-quality
direction, or when repeated question, search, or chat difficulties provide
concrete evaluation cases.

## Breadcrumbs

- Owner's 2026-10-12 assessment and backlog request in this chat: combine the
  learning and chat work, reuse seeds, group the remaining work as an epic, and
  commit/sync planning on main only.
- [Shared OpenKB discussion](https://chatgpt.com/share/6acc428e-cd14-83ec-8412-3b5e7d6826c6).
  Its review loop is a proposal for Donut, not a verified OpenKB feature.
- [Focus-context design](../../docs/focus-context/focus_context_retrieval_design.md)
  and [domain vocabulary](../../docs/adrs/0001-ubiquitous-language.md).
- [Hosted assistant setup guide](../../docs/assistant-oauth.md).
