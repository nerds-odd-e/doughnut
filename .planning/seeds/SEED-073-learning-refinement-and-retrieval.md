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

For people learning from deliberately authored Donut notes, a confusing recall
answer or conversation should lead to better understanding or a reviewed note
correction, and later questions should find and use the relevant evidence.
Preserve the learner's history and the author's control over knowledge changes.

This is the parent epic for the combined learning/refinement and chat work,
question-context quality, evidence discovery, and cited answers. It is not one
executable story and is not queued in addition to its children. The independent
[knowledge-to-capability experiment](SEED-074-knowledge-to-capability.md#story-1)
is outside this epic. Its outcome is useful even if this epic stops.

## Why This Matters

The owner wants to borrow OpenKB ideas for improving and using accumulated
knowledge. Donut's starting point remains a person noticing and capturing
something worth learning; bulk document compilation is not the priority.

The 2026-10-12 assessment read local main at `e851a7682d`, including code,
tests, and Accepted ADRs. It did not run the app or measure model quality:

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
- Search matches titles and aliases. MCP returns one matching note's metadata,
  then offers graph context; it does not expose a candidate list or body search.
- Git preserves accepted notebook content; recall history preserves learner
  events. Neither alone records the diagnosis, proposal decision, and learning
  follow-up as one resolved difficulty.

These are starting observations, not promises that the current UI works in
every cited journey. Verify the selected story's decisive premises when refining it.

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
- **Selected direction:** Deliver the useful child outcomes below. Diagnose
  before changing knowledge; use measured question-quality cases to choose
  retrieval improvements. Preserve simple search alongside richer answering.

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
- Bulk ingestion, embeddings, a universal review inbox, automatic knowledge
  rewrites, skill factories, and remote MCP hosting are deferred promises.
  These deferrals do not prescribe runtime rejection of naturally supported behavior.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery, following
[SEED-039](SEED-039-faster-ci-feedback.md#story-decomposition). Estimates are
hypotheses. Refine or resplit work likely to exceed L before execution.
Each story records its preparation state below.

<a id="story-2"></a>

### Save a useful chat clarification as a reviewed note correction

**Identity:** SEED-073#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A note author discussing a note or recall question can retain
  the useful clarification in the note without copying and reconstructing it.
- **Scope:** Reuse current chat suggestions and refinement previews to show
  an editable correction to the existing subject note, its reason, and its
  originating exchange. Acceptance saves through ordinary note publication;
  rejection leaves authored content unchanged. Retain the proposal decision
  and the accepted change's reference with the conversation, including after
  reopening it. Resolve the subject note consistently for note and MCQ chats.
  This story corrects one existing note; creating discovery notes and changing
  several notes together remain later candidates.
- **Evaluation:** A recall conversation reveals that a note omits a useful
  distinction. Its author previews and edits an added example, accepts it,
  and can reopen both the improved note and the conversation showing what was
  accepted. Declining an alternative remains visible as a decision. A reader
  without edit rights can discuss the distinction without publishing a change.
- **Value / learning:** Close the concrete conversation-to-knowledge gap shared
  by the original learning-loop and stronger-chat proposals.
- **Effort hypothesis:** L, medium confidence; current acceptance and Git
  publication exist, but durable proposal attribution needs refinement.
- **Depends on:** No new story; it can start from an existing conversation.
  Reuse the existing [reviewed refinement handoff](../../docs/recall-help.md#reviewed-refinement).
- **Safe stopping point:** A reviewed, attributable note correction remains
  useful without adaptive retrieval or any general review system.

### Generate focus-note questions with relevant supporting passages

The canonical story is [SEED-007#story-1](SEED-007-per-concept-prompt-splitting-single-note-qg.md#story-1).
It is the next epic increment after the two learning-loop stories. Keep its
scope, examples, preparation state, and identity in that existing seed only.

<a id="story-3"></a>

### Find a note from wording remembered in its body

**Identity:** SEED-073#story-3
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A learner who remembers a useful passage but not its note
  title can find the note through ordinary Donut search.
- **Scope:** Extend the existing search journey to body-text evidence, with
  a matching excerpt and a link to the note. Preserve fast title/alias search
  and its scope controls. Search remains evidence discovery, with no generated
  answer required. The existing search API serves the same behavior.
- **Evaluation:** A phrase occurs only in a note's body. Searching it shows
  that note and the matching passage; opening the result reaches the note.
  Exact title/alias searches remain useful, and unavailable notes do not appear.
- **Value / learning:** Discover how much missing knowledge is actually a
  title-only search limitation before investing in richer query orchestration.
- **Effort hypothesis:** L, medium confidence; ranking, representative corpus
  size, and response-time expectations need refinement.
- **Depends on:** No new story. Search and graph expansion have different
  responsibilities, so question-context changes do not block this outcome.
- **Safe stopping point:** Learners can find and inspect additional evidence
  with the existing search interaction even if synthesized answers are deferred.

<a id="story-4"></a>

### Let an external agent inspect several candidate notes and cite its evidence

**Identity:** SEED-073#story-4
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A person using an external agent can have it compare plausible
  matches and read the supporting knowledge rather than trust the first hit.
- **Scope:** Extend the current MCP evidence journey to return multiple
  candidates and directly read the chosen notes or bounded context. Results
  provide unambiguous navigation and source revision attribution so the agent
  can cite the notes actually read, including equal titles in different places.
  Reuse HTTP read/search/context capabilities and notebook Git authority.
  Tool descriptions must accurately describe the content they return.
- **Evaluation:** Two notes match a term; the useful evidence is in the second.
  An agent lists the candidates, reads both, and returns an answer identifying
  the passage and revision it used. A later notebook edit does not silently
  relabel the old evidence as current. Reads remain within the user's access.
- **Value / learning:** Enable evidence-driven external skills without forcing
  them to call another answer-generating model inside Donut.
- **Effort hypothesis:** L, medium confidence; reuse stdio MCP and existing
  reads, with no hosting/transport change.
- **Depends on:** No new story; today's multi-hit title/alias HTTP search can
  supply candidates. Consume body matches when available without waiting for
  them or duplicating search. Remote hosting remains in SEED-002.
- **Safe stopping point:** Existing agents can discover, read, and attribute
  evidence independently of app query or the capability experiment.

<a id="story-5"></a>

### Answer a comparison across notes with inspectable supporting evidence

**Identity:** SEED-073#story-5
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A learner comparing two concepts can obtain an answer grounded
  in their notes and examine what supports it.
- **Scope:** Start with a comparison naming two concepts in one accessible
  notebook. Find candidate notes through existing search, read relevant
  evidence with bounded expansion, and answer with attributable references.
  State uncertainty when the notebook does not support a conclusion. A
  conversational follow-up can request another relevant note as the question
  changes; reuse the current conversation surface and common evidence access.
  This is the first richer query journey, not a commitment to arbitrary
  research across the entire catalog.
- **Evaluation:** Two notes separately explain concepts and a third supplies
  a qualification. A comparison cites the supporting passages, and a follow-up
  about the qualification retrieves that evidence. If the qualification is
  missing or sources disagree, the answer exposes that limit instead of inventing
  agreement. Simple search remains available.
- **Value / learning:** Test whether bounded synthesis helps users apply their
  knowledge and whether conversationally chosen context improves that answer.
- **Effort hypothesis:** L, low confidence; refine the entry interaction and
  comparison cases, and resplit if one end-to-end comparison exceeds L.
- **Depends on:** No hard story block established. Current HTTP search and
  focus context support a bounded comparison; reuse earlier evidence improvements
  when delivered. Shared code and preferred delivery order alone impose no wait.
- **Safe stopping point:** One useful, cited comparison journey with honest
  evidence limits; broader chat, catalog-wide research, and new-note proposals
  can remain deferred.

<a id="story-6"></a>

### Contest an answered question with a reason and an inspectable decision

**Identity:** SEED-073#story-6
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A learner who believes an answered MCQ was ambiguous or wrong
  can have the actual objection considered and later see what was decided.
- **Scope:** Extend the existing contest responsibility with the original
  attempt and learner's reason, an explanation of the verdict, and durable
  attribution to the attempt. Distinguish a conversational hypothesis from a
  formal upheld contest. State the actual effect on the question and recorded
  Grade; grade/schedule repair is story 7. Learner reasoning remains private
  unless deliberately shared. Existing author permission still governs notes.
- **Evaluation:** After choosing an option defensible under the wording, the
  learner contests with that interpretation. The result addresses the objection,
  can be reopened, and states whether the question was invalidated and whether
  the attempt's learning effects were corrected. A declined contest gives an
  inspectable reason, not instructions to answer an already answered question.
- **Value / learning:** Restore trust through a meaningful challenge and learn
  whether disagreements concern wording, evidence, or the marked answer.
- **Effort hypothesis:** M–L, low confidence until verdict authority, persistence,
  and shared-MCQ effects are refined. Separate from explanation/practice by
  the owner's accepted 2026-10-12 decision.
- **Depends on:** Existing contest and recall identities. Private recall help
  can supply context when present but is not a hard implementation prerequisite.
- **Safe stopping point:** The learner can inspect the decision and its actual
  consequences without any implied repair of the recorded Grade.
- **Open decisions:** Who finalizes a verdict, what an accepted alternative
  answer means, and whether one MCQ's invalidation affects other learners.
  Refine before execution planning; this candidate is not queued.

**Preparation observation:** On 2026-10-12 at `e1bbd1d2727fced534f473bcba709407a46d91c9`
in story 1's owned workspace, a disposable controller probe answered an MCQ
incorrectly, reloaded persisted state, then contested with a mocked infeasibility
result. Reload showed a contested MCQ, the original incorrect Answer and AGAIN
event, and unchanged Stability, Difficulty, last recall and due time.
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed with 2738 tests,
zero failures/errors and two skipped in 51s. An initial timestamp comparison
mixed in-memory milliseconds with MySQL rounding; comparing persisted before/after
values corrected the probe without changing product code. Scratch files were
removed. This establishes the existing API mechanism, not a working user-facing
post-answer journey, model adjudication quality, or scoring repair.

<a id="story-7"></a>

### Resolve a disputed wrong mark after the learner confirms question ambiguity

**Identity:** SEED-073#story-7
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/070-learner-confirmed-ambiguous-exclusion/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"caaf46c5660855e30e3bc41bdebf5bf287fbaf26e9a70ca948fd786d15006df8","plan":"708aaac6dbe4b7f90641514a0c15fd01056656e669e2057c8a1553c965d6352c"}}
```

**Goal:** A learner whose answer was marked wrong can resolve that result when
private recall help identifies an ambiguous question. After the learner confirms
the proposed correction, the attempt is visibly excluded from evaluation, its
learning effects are corrected, and the original evidence remains inspectable.
This restores trust without claiming a successful recall that was not demonstrated.

**Scope:**

- Start from one answered MCQ and its private help conversation. The tutor can
  propose that the wording did not support a unique answer, explaining the
  defensible interpretation using the original question, selected answer, and
  available evidence. The proposal identifies that particular attempt.
- Let the learner review and confirm or decline the proposal. The learner's
  confirmation authorizes the personal correction; conversational agreement or
  a tutor statement alone changes no result. A separate AI contest verdict is
  not required for this personal decision.
- On confirmation, replace the unresolved wrong-mark presentation with an
  explicit excluded-as-ambiguous result. Retain the original question, selected
  answer, marked outcome and Grade, together with the correction's reason,
  confirming learner, time, and originating exchange. Reopening the attempt
  or its help shows the same decision and explains its actual consequences.
- Exclude that invalid attempt from evaluation and derived recall readouts.
  Recompute the existing tracker's current scheduling state from the remaining
  valid history, using the original event times and the current FSRS policy.
  Keep the scheduling state the tracker held just before the excluded attempt,
  including state earned through grades that no recall event records.
  Preserve subsequent Grades and Confusion; align affected cached readouts with
  the corrected history. With no remaining valid scheduling event, the same
  tracker is due as an ungraded tracker. Corrected due work can already be overdue.
- Preserve the original event as evidence. Confirmation is not a new Grade,
  does not award GOOD, and does not reset or recreate the tracker. Repeating
  confirmation or reopening the decision does not add another learning event.
- Confirmation authorizes only this learner's attempt. It grants no authority
  over another learner's history, shared-question invalidation, or authored
  note content. A declined proposal leaves the result and schedule intact and
  remains distinguishable from an accepted correction.

**Key examples:**

- A learner answers “Choose the grammatically correct completion: I ___ in
  Tokyo” with “am used to living”; the key is “used to live,” so the app records
  AGAIN. Private help explains that both are grammatical and proposes exclusion
  with that reason. The learner confirms → the attempt reads “Excluded —
  ambiguous question,” is not counted as failed or successful recall, and the
  due time and recall readouts reflect the remaining valid history. Reopening
  shows both the original mark and the learner-confirmed correction.
- The same proposal is declined, or the learner only reads the explanation →
  the original wrong mark and scheduling effects remain. The tutor does not
  imply that a correction has already happened.
- Another valid Grade is recorded before the learner confirms the ambiguity
  proposal → correction excludes the disputed attempt and preserves the later
  Grade at its original time. The current schedule and affected readouts agree
  with that corrected history, rather than restoring an old snapshot over the
  later recall. Repeating confirmation gives the same resolved result.
- The source clearly says that driving on the left feels normal now, and the
  learner selects “used to drive” instead of “is used to driving” → ordinary
  misconception feedback explains the different meanings; a grammatical option
  alone is not evidence that this meaning-specific question is ambiguous.
- Two learners answered the same MCQ → one learner's confirmed correction
  changes only their attempt and derived learning state. Their private reasoning
  is not exposed to the other learner or copied into the note.

**Value and decomposition:**

- **Valuable:** The learner can settle an unfair recorded failure, rather than
  receive reassurance beside an unchanged penalty. The correction remains useful
  even if note refinement and every later epic story are deferred.
- **Visible:** The learner evaluates the proposal, resolved mark, retained
  explanation, recall history, and next due time through the product.
- **Vertical:** One confirmed decision connects private help to the original
  attempt, persisted history, derived readouts, and scheduling. A label change
  that leaves an invalid failure effective is not this story's stopping point.
- **Strongest smaller alternative:** Tutor reassurance already explains the
  ambiguity but leaves the wrong mark and its learning effects unresolved.
  Manually recording GOOD would invent successful recall and another event.
  A personal, confirmed exclusion resolves the observed difficulty without a
  general appeals queue or a note correction.
- **Learning question:** Can learners understand and trust an excluded result
  when they can inspect why it changed and how it affects future recall?

**Boundaries and preparation:**

- Reuse private recall help, attempt ownership, and the existing tracker and
  history. Story 6 retains the broader formal-contest responsibility; its
  verdict workflow is not a blocking prerequisite for learner-confirmed personal
  exclusion. Story 2 remains the separate authored-note correction outcome.
- Shared-MCQ adjudication, regenerating or editing questions, undoing a
  confirmed correction, correcting several attempts together, spelling
  disputes, and authoring note changes are deferred promises. These examples
  do not impose a latest-attempt-only gate or another historical cutoff.
- Preserve [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md):
  original Grade evidence, whole-hour elapsed-time semantics, retained event
  attribution, and coherent current state and cached readouts. Slice planning
  must settle the durable invalid-attempt interpretation against this ADR before
  dependent execution; this refinement does not amend an Accepted ADR.
- **Effort hypothesis:** L (2–4 hours), low confidence. Authority and the
  correction rule are now explicit; recomputation and readout coverage still
  need sizing during slice planning. Resplit if that coherent outcome exceeds L,
  retaining an independently useful correction rather than technical-layer stories.
- **Safe stopping point:** One confirmed personal ambiguity correction is
  inspectable and consistently excluded from evaluation and scheduling, while
  the original learning evidence and every remaining valid event survive.
- **Refinement decision, 2026-10-13:** The owner prioritized this outcome ahead
  of note refinement and chose learner confirmation of the tutor's proposal.
  The same day's preparation re-read the story against the code and kept its
  goal, scope, and examples unchanged; the exclusion is recorded on the
  learner's attempt, and the schedule is rebuilt by replaying the remaining
  RecallLog events, as the slice plan settles. Implementation is later work.
- **Owner decision, 2026-10-13:** Execution observed that recall events have
  been recorded only since 2026-08-17, so a rebuild from a new tracker would
  drop stability earned earlier. The owner chose to start the rebuild from the
  memory state cached on the excluded attempt's own recall event and to apply
  the later valid events to it. An event without cached state keeps the
  rebuild from a new tracker.

**Observation provenance:** The 2026-10-13 manual session used ten real
`gpt-6.1-sol` replies in an isolated E2E browser at product revision
`871aea594bff4aedf46e26a86eb1f6e8166d5f66`. In the Tokyo example, the tutor
recognized the ambiguity from “Why is my answer wrong?” without a leading hint,
but the answer remained red and persisted grading and scheduling state were
unchanged. The full dated inputs, actions, dialogue, and limitations are retained
in [the before-cleanup observation revision](https://github.com/nerds-odd-e/doughnut/blob/d88a5237d6dbd5f3f67493e781bb5ee6c46e0e3c/.planning/seeds/SEED-073-learning-refinement-and-retrieval.md#manual-language-learner-evaluation).
This is agent-simulated learner evidence, not measured human comprehension,
retention, or representative model reliability.

## Ordering and Scope Reduction

The backlog owns global priority. Within this epic: resolve an actual learning
difficulty, resolve a learner-confirmed ambiguous wrong mark, retain a useful
note correction, improve question evidence through
SEED-007, then improve body search, external-agent evidence access, and cited
comparison. This order favors immediate learning value before broader query
machinery; it does not impose serial technical dependencies.

Drop or defer broader comparison first if capacity is tight, then expanded
external-agent access. Keep the existing workflows usable at every stopping
point. The separately queued capability experiment can use a current notebook
checkout and is neither a supplier nor a consumer blocking this epic.

## Open Decisions

- The private-help manual session supplies a concrete ambiguous-question case
  for story 7. Select further real learner cases during evaluation; human
  comprehension, retention, and retrieval-quality gains remain unmeasured.
- Define how long proposal decisions and their private conversation evidence
  should remain available, using existing deletion/ownership rules.
- Agree practical latency and cost expectations for retrieval/query evaluation.
- Agent-reported gaps, automatic repeat-failure triggers, and claim-level
  provenance remain later decisions. Detailed contest and invalid-question
  correction policies are owned by stories 6 and 7.

## When to Surface

When choosing work for the new learning-to-refinement and knowledge-quality
direction, or when repeated question, search, or chat difficulties provide
concrete evaluation cases.

## Breadcrumbs

- Owner's 2026-10-12 assessment and backlog request in this chat: combine the
  learning and chat work, reuse seeds, keep the capability experiment independent,
  group the remaining work as an epic, and commit/sync planning on main only.
- [Shared OpenKB discussion](https://chatgpt.com/share/6acc428e-cd14-83ec-8412-3b5e7d6826c6).
  Its review loop is a proposal for Donut, not a verified OpenKB feature.
- [Focus-context design](../../docs/focus-context/focus_context_retrieval_design.md)
  and [domain vocabulary](../../docs/adrs/0001-ubiquitous-language.md).
- [Existing question-context hypothesis](SEED-007-per-concept-prompt-splitting-single-note-qg.md)
  and [remote MCP hosting seed](SEED-002-host-mcp-over-https.md).
- SEED-055 concerns file-serving API cleanup and SEED-001 concerns a different
  fuzzy spelling-match behavior; neither is duplicated or reprioritized here.
