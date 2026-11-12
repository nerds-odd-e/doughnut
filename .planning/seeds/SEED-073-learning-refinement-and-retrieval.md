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

<a id="report-recall-answers-whose-question-is-gone"></a>

### Report recall answers whose question is gone

**Identity:** SEED-073#report-recall-answers-whose-question-is-gone
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/001-report-recall-answers-whose-question-is-gone/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"66e0bda6d342fa201fc378aee36c2107800000c24c98e79bdcebc8273da233c7","plan":"7b9d83bc8ff1bdf76f71e09299d6f205d9e8ca0d6e8c6912a3a8fe854d6f0fe0"}}
```

**Goal.** As a Donut learner, my connected assistant sees every recall answer
I gave in the window, including answers whose question Donut no longer stores,
so its investigation of what I confuse is not silently missing attempts.

**Scope.**

- `list_recall_answers` includes an answer whose recall prompt is gone when
  the learner's surviving graded recall log still attributes that attempt.
  Keep its original time, recorded correctness, outcome and ambiguity exclusion
  when recorded, and explicitly explain that its question is unavailable.
- Return what the answer actually stores: its zero-based choice index for an
  MCQ or its typed spelling answer. A choice index cannot recover the selected
  choice's text without the original choices. Another question on the same
  tracker does not supply that missing history.
- Identify the recalled source note through the primary graded log, rather
  than a secondary Confusion log. Apply the existing evidence rule: title and
  Portable path for a correct answer, bounded graph for an MCQ miss, full
  current content for a spelling miss. If the note is trashed or unreadable,
  return its unavailable reason while retaining the learner's attempt.
- Preserve the existing lookback, newest-first ordering and cursor contract:
  reading every response returns each attributable attempt exactly once.
  Only the connected learner's attempts are returned, because learning history
  is private; current notebook access governs source-note evidence.
- Explain missing questions and stored answer indexes consistently in the tool
  description, MCP skill and `docs/assistant-oauth.md`.

**Scope limits (deferred work).** Repairing the separately reported deletion
defect below, restoring deleted question text, adding historical snapshots,
changing scheduling/statistics, and recovering attribution after its tracker
and recall logs are hard-deleted are separate work. Reporting surviving records
does not depend on repairing future question retention.

**Plan:** [Report surviving recall answers](../slice-plans/001-report-recall-answers-whose-question-is-gone/PLAN.md)

**Key examples.**

1. An MCQ answer has index `2`, an AGAIN log and no stored prompt. Listing its
   window returns its original time, `correct: false`, index `2`, an explicit
   question-unavailable reason and the readable source note's bounded graph.
2. The same tracker has since received another question. Listing the earlier
   answer still reports its original question unavailable; the later prompt
   cannot supply the earlier answer's choice text.
3. A promptless spelling miss retains its typed answer. A linked Confusion log
   for another tracker does not change the recalled note or duplicate the
   attempt. A trashed or unreadable source note produces an explicit note
   unavailable reason alongside the retained answer.
4. A window spans several pages of ordinary and promptless answers, including
   equal answer times at a page boundary. Following all cursors returns every
   in-window attempt once; another learner's attempts remain private.

**Observations (2026-11-12).**

- Read with the owner's own token: 24 of the owner's 729 answers in the seven
  days before 2026-11-12 (and 2,189 of all recorded answers) have no recall
  prompt any more, all on understanding trackers, some answered the day
  before. Their trackers list no prompts. The cause is unknown and is the
  first thing to settle, because it decides how often a question is
  unavailable.
- Answers to look at first: `246327`, `246452`, `246496`.
- Lead from code reading only:
  `V300000325__cascade_note_dependents_on_note_delete.sql` made
  `recall_prompt.mcq_id` cascade on MCQ delete, and `mcq.note_id` cascades on
  note delete. If an understanding tracker's prompt can use an MCQ owned by
  another note, hard-deleting that note deletes the answered prompt while the
  tracker stays. No application code deletes MCQs or answered prompts
  directly.
- `RecallAnswerHistoryService` finds answers through the learner's recall logs
  or recall prompts and currently keeps only answers that still have a prompt;
  the source note then comes from the prompt's tracker. Without a prompt it
  would come from the answer's non-Confusion recall log.
- Unverified, from code reading: an answer reachable through its recall prompt
  with no recorded grade, no recall log and no outcome would make
  `list_recall_answers` fail the whole call, because its correctness is null.
  While reading production, check whether such answers exist.

**Production investigation (2026-11-13 SGT).**

Read-only MySQL transactions and binary-log replay used the owner's signed-in
`gcloud` access, through `doughnut-app-group-qk27` to Cloud SQL `doughnut-db`,
database `doughnut`, MySQL `8.4.11-google`. The deployed app reported revision
`12288107bd2a2dbbfc2eabc2d00f44e0d4ed4e74`; the relevant tracker-rehoming code
matches preparation revision `b3f4ce10bd0d`. No production data was changed.

- At `2026-11-12 23:22:30 UTC`, a distinct-answer count joining `answer` through
  `recall_log`/`memory_tracker`, with `NOT EXISTS` on `recall_prompt.answer_id`,
  found **2,197** promptless answers, all on UNDERSTANDING trackers. The three
  original samples retain choice indexes `2`, `1`, `3` and AGAIN logs on
  trackers `19414`, `5943`, `3227`. Tracker `3227` now has a new unanswered
  prompt, `265138`; it is not answer `246496`'s question.
- A query of all answers reachable through a prompt or recall log found **0**
  without correctness derivable from GOOD/AGAIN or OVERLAP. No new unknown-grade
  policy is needed for the observed population.
- Live FK metadata confirms `note → mcq → recall_prompt` CASCADE deletion.
  There are **3,538** stored prompts whose MCQ's note differs from their
  tracker's current note. Counts describe this snapshot, not every cause.
- Successful UTC replay used `mysqlbinlog --read-from-remote-server
  --base64-output=DECODE-ROWS -vv --start-datetime='2026-11-12 01:12:00'
  --stop-datetime='2026-11-12 02:41:00' mysql-bin.000048` over an authenticated
  DB tunnel. Only numeric row identities and transaction metadata were retained.
  Answer `246452` is attached to prompt `264841`, MCQ `258176`, tracker `5943`
  at `01:13:32 UTC` (position `7385375`, GTID suffix `54733`). At `01:13:43`,
  transaction `54749` moves that tracker from note `14696` to `2116`
  (position `7400392`) and deletes note `14696` (`7401416`).
- Answer `246496` is attached to prompt `264504`, MCQ `257912`, tracker `3227`
  at `02:38:22 UTC` (`8149412`, transaction `55305`). At `02:39:29`,
  transaction `55341` moves the tracker from note `13313` to `1657`
  (`8224209`) and deletes `13313` (`8225492`). These GTIDs share UUID
  `b5b67510-b8d5-11f1-b37b-42010a6f101b`.

**Separate defect report — relationship reduction loses answered questions.**
Reported to the owner during refinement, before implementation planning.
`RelationReduceService.reduceToSourceProperty` rehomes understanding trackers
through `PropertyMemoryTrackerService.rehomeNoteLevelTrackersToProperty`, then
permanently removes the relationship note. Its MCQs remain owned by that note;
the cascade deletes their answered prompts while graded logs survive on the
rehomed trackers. The two production transaction traces above support this
cause; they do not explain all 2,197 losses, and the loss of sample `246327`
was not replayed. Preserving answered questions during reduction needs its own
follow-up and owner prioritization; this story reports the surviving evidence.
The owner prioritized that repair as
[Keep answered questions with their tracker](#keep-answered-questions-with-their-tracker).

<a id="keep-answered-questions-with-their-tracker"></a>

### Keep answered questions with their tracker

**Identity:** SEED-073#keep-answered-questions-with-their-tracker
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

**Goal.** As a Donut learner, every question I answered stays with the memory
tracker that recorded my answer, wherever that tracker moves, so reducing or
removing the note the question was generated from never erases my attempt's
question while its grade and schedule survive.

**Owner direction (2026-11-13).** Losing the question while keeping the
tracker is a defect, not intended behavior. The owner prefers that a recall
prompt's question belongs only to its tracker; if it does not, the question
must at least follow the tracker to the right note.

**Current design (analysis, local main `a595aec4e2`).** The question does not
belong only to the tracker. `recall_prompt` points at both its tracker and an
`mcq`; the MCQ separately records the note it was generated from
(`mcq.note_id`, `ON DELETE CASCADE`), and `recall_prompt.mcq_id` cascades on
MCQ delete (`V300000325`). So a prompt dies with the MCQ's note, not with its
tracker. Every tracker move leaves its MCQs behind:
`rehomeNoteLevelTrackersToProperty` (reduction, then permanent removal of the
relationship note — the confirmed production cause) and
`movePropertyTrackersOntoRelationshipNote` (later removal of the source note
would do the same). Production held 3,538 prompts whose MCQ note differs from
their tracker's note. Code still reads the MCQ's note as the prompt's note:
`RecallPrompt.getNote`, `Conversation` subject note, `ConversationService`
title, and contest/regenerate context in `RecallPromptController`. The note's
**Questions** list (`McqController.list` via `Note.mcqs`), directly added
questions (`NoteAddQuestion`), and the recent-questions AI context also read
`mcq.note_id`.

**Recommended direction.** Make the tracker the single owner of an asked
question, as the owner prefers: an asked question's lifetime and its note come
from its prompt's tracker, never from the note it was generated from. This
removes the divided source of truth instead of adding a repointing step to
every tracker move, and it makes the existing mismatched prompts harmless
without a data migration. Repointing `mcq.note_id` on each move is the simpler
alternative; it keeps the duplicate note reference, must be remembered by
every current and future move, and would still leave the 3,538 mismatches to
repair. Deletion stays application-owned, not a new database cascade.

**Scope.**

- An answered prompt, with its question, choices, recorded answer, grade,
  outcome and ambiguity exclusion, lasts as long as its tracker, whichever
  note the tracker moves to and whichever note is later removed.
- Everywhere a recall prompt's note is used — recall history, connected
  assistant recall answers, conversations about a prompt, contest and
  regeneration context — it is the tracker's current note.
- A note's Questions list and recent-questions AI context show questions asked
  through trackers now on that note; questions added directly to a note stay
  on that note.
- Removing a tracker still removes its prompts and answered questions, as
  today.

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
4. After example 1, S's Questions list includes the moved tracker's questions;
   a question added directly to S remains listed there.
5. Deleting the tracker itself (for example its note is removed while it has
   not moved) still removes its prompts.

**Effort hypothesis:** M — a few hours; confidence medium. Assumes each asked
MCQ belongs to exactly one prompt (true for generation, regeneration and batch
import today).

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
