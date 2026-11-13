# Learning, refinement and retrieval: dated evidence

Extracted verbatim on 2026-11-13 SGT from
`.planning/seeds/SEED-073-learning-refinement-and-retrieval.md` at source revision
`e5b8aca1c605f59775f8cc607ea5f08095a9348e`. The original dates, analyzed/deployed
revisions, inputs, private aggregate/numeric evidence, actions, results and
limitations below remain as recorded. This extraction changes no observation.
The forward contract remains in the [canonical seed](SEED-073-learning-refinement-and-retrieval.md#keep-answered-questions-with-their-tracker).

## Tracker ownership and production loss

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

## Learning and retrieval assessment

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
