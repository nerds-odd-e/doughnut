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

<a id="retrieve-my-recall-answer-history-through-mcp"></a>

### Retrieve my recall answer history through MCP

**Identity:** SEED-073#retrieve-my-recall-answer-history-through-mcp
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/093-retrieve-my-recall-answer-history-through-mcp/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"302c7cc90f2ea15d87c7cd13a9debbce9e11160c64b56a5bee0ec91da10de817","plan":"e92931f1a2f3ac1a18d197efa0706555e713068dd4ccaf6e4e084fc947f02f25"}}
```

**Goal.** As a Donut learner, I can ask my connected assistant for my recall
answers so it can use real attempts to investigate what I confuse and where my
notes or questions could improve.

**Scope.**

- A read-only MCP call retrieves every recorded answered recall prompt for the
  connected learner. The lookback defaults to seven days, measured by answer
  time, and the caller can optionally supply a different positive number of
  days. The full window is retrievable even when the results need more than one
  response.
- Each attempt includes its answer time, the full question used, the learner's
  selected choice or typed answer, and the recorded correctness. For multiple
  choice, include all choices, the expected answer, and the stored validation
  rationale when available. Preserve recorded outcomes such as overlap and
  ambiguity exclusion so they can be interpreted correctly.
- The response selects source note evidence automatically from the recorded
  correctness and question type. Apply current notebook read access. Use the
  current title and the notebook-qualified [Portable path](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#links-and-attachments).
- Identify missing historical evidence explicitly. For example, an older
  spelling attempt may have its typed answer even when the original masked
  prompt cannot be recovered after a note edit.

**Clarifications (refinement, 2026-11-12).** These follow from the scope above
and the observations below; they add no restriction of their own.

- One attempt is one answer. Its correctness is the grade recorded with that
  answer, or the Overlap outcome, which counts as correct. An Accidental match
  is an incorrect attempt that also says so; its Confusion record on the other
  note's tracker is part of the same attempt, not a second one.
- An attempt later excluded as ambiguous keeps its recorded correctness and
  adds the exclusion and its reason. Source note evidence follows the recorded
  correctness.
- The lookback is a rolling window of days × 24 hours ending at the call.
  Times are returned as absolute instants, so the call needs no timezone.
- Responses are sized for an assistant to read. When the window does not fit
  one response, the response says so and gives what the next call needs to
  continue; continuing to the end returns each attempt exactly once.
- Source note evidence is addressed the way the other MCP tools address notes:
  by qualified Portable path, with no numeric note ID. The bounded graph for an
  incorrect multiple-choice answer uses a fixed modest budget chosen by Donut;
  `get_note_graph` remains the way to ask for more.
- When the learner can no longer read the source note (access withdrawn, or the
  note is in the trash), the attempt stays: time, answer, correctness, and the
  stored question are the learner's own recall record. The source note evidence
  is reported as unavailable with the reason.
- An answer whose question is no longer stored is still an attempt. It carries
  its time, answer, and correctness, and reports the question as unavailable.

**Scope limits (deferred promises, not rejections).**

- Just review and Tutor Feedback events have no answered recall prompt and are
  not returned.
- Filtering, such as incorrect attempts only or one notebook, and a compact
  index of attempts with details on request. See the size observation below.
- Thinking time and other attention readings, aggregates, and any diagnosis.
  The assistant draws conclusions; the tool supplies evidence.
- Hard-deleting a note deletes its recall prompts today; this story does not
  preserve those attempts.

**Required restrictions.** Only the connected learner's own attempts are
returned (private learning evidence). Source note title, path, content, and
graph need current notebook read access (authorization). The days argument, when
given, is a positive number (a window must have a length). The tool changes
nothing: no schedule, tracker, or note.

**Key examples.**

- With the default lookback, yesterday's correct multiple-choice answer has
  the full question and the note's title and path. The title and path are the
  entire source note payload for a correct answer of either question type.
- Yesterday's incorrect spelling answer includes the note's title, path, and
  full current content so the assistant can inspect the clue in context.
- An incorrect multiple-choice answer from ten days ago appears when the
  caller requests fourteen days. Its source note evidence includes the title,
  path, and bounded graph.
- An answer from eight days ago is absent from the default call.
- A spelling answer that named another note is one incorrect attempt marked
  Accidental match, with the typed answer; it does not appear a second time for
  the other note.
- A spelling answer accepted as an Overlap is correct with that outcome named,
  and has title and path only.
- An incorrect multiple-choice attempt later excluded as ambiguous shows
  incorrect, excluded, and the reason, with the bounded graph.
- A spelling note was edited after it was answered: the typed answer and
  correctness are returned, and the prompt as shown is reported unavailable.
- A week holds more attempts than one response carries: the first response
  says more remain; following it to the end yields every attempt once.
- The learner lost access to a subscribed notebook after answering: the attempt
  and its stored question remain, and the note evidence is reported unavailable.
- Another learner's attempts on the same shared note never appear.

**Observations (2026-11-12, worktree at `7c1b8d3e6c`, production read with the
owner's own token; counts only).**

- The owner answered 729 recall prompts in the last seven days and 99 in the
  last day: 508 multiple choice (49 incorrect), 197 spelling (23 incorrect, 15
  of them Accidental matches), none excluded. A sample of 158 answered
  multiple-choice questions averaged about 200 characters of stem and choices;
  about half had a validation rationale of about 100 characters. A default
  call therefore carries several hundred thousand characters before any note
  evidence, mostly Japanese text. Paging is required, not an edge case.
- 24 of those 729 answers (and 2,189 of all recorded answers) have no recall
  prompt any more, all on understanding trackers, some answered the day
  before. Their trackers list no prompts. No code path that deletes an
  answered prompt was found; the cause is unknown and is the first thing for
  planning to settle, because it decides how often a question is unavailable.
- Correctness is not stored on the answer. It is derived from the recall log
  linked to the answer and from the Overlap outcome
  ([Answer](../../backend/src/main/java/com/odde/donut/entities/Answer.java)).
- The stored multiple-choice question keeps stem, choices, expected answer,
  tested focus, and validation rationale, so it survives note edits. The
  spelling prompt is derived from the current note.
- The existing recall history export returns the whole history in one answer
  (134,670 events, 78 MB for the owner) and carries no question or answer
  text. It cannot serve this goal as it is; a windowed read is needed.
- Every existing read tool uses the `notes:read` grant, and
  `request_learning_session` already serves learning data under it. The
  consent page says "read your notebooks".

**Alternatives considered.** All were judged against the same goal: an
assistant can use real attempts to find what the learner confuses.

- *Everything in the window, paged (recorded scope, selected).* Complete and
  simple to explain; an assistant must page through mostly correct answers to
  reach the misses.
- *Borrowed: flight-data monitoring.* Airlines keep a one-line record of every
  routine flight and a full snapshot only when a limit is exceeded. Here a
  correct answer is the routine flight and a miss is the exceedance. The scope
  already grades source note evidence this way; the adaptation would also
  reduce a correct attempt to a compact line. The analogy breaks where a
  correct answer is itself the evidence (a lucky guess, a leaky question), so
  correct attempts must stay retrievable in full. Deferred as the compact
  index above.
- *Incorrect attempts only.* About a tenth of the volume, but loses the
  comparison between questions answered well and badly on the same note.
  Deferred as a filter.
- *Hand the existing export file to the assistant.* No questions or answers in
  it, and too large. Rejected for this goal.

**Architecture.**

- The MCP surface gains its first tool that serves private learning evidence
  rather than notebook content. It is read from the application database, never
  from Portable content, as
  [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  requires. It reuses the `notes:read` grant; the tool is read-only.
- It is also the first MCP tool whose answer can exceed one response. The
  window is read by answer time, never by loading the whole history.
- Source note evidence reuses the existing full read and bounded graph, and the
  qualified Portable path addressing of
  [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#links-and-attachments).
  Unavailable evidence is a reported outcome, not a swallowed failure
  ([ADR 0006](../../docs/adrs/0006-failure-handling-accepted.md)). Names follow
  [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md): answer, recall
  prompt, Accidental match, Overlap, Confusion.

The existing [recall history export](../../backend/src/main/java/com/odde/donut/controllers/dto/RecallHistoryExport.java)
provides event metadata. [Recall prompts](../../backend/src/main/java/com/odde/donut/entities/RecallPrompt.java)
hold the answered multiple-choice question and derive the spelling prompt from
the note. The [MCP graph tool](../../backend/src/main/java/com/odde/donut/mcp/GetNoteGraphTool.java)
already supplies bounded note context. Research and recommendations can follow
once an assistant can retrieve this evidence.

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
