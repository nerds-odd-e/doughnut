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

<a id="story-1"></a>

### Resolve a confusing recall answer with an explanation and a follow-up check

**Identity:** SEED-073#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/001-resolve-confusing-recall-answer/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"03132ee9b2502f336784ad1a011c739ea7609ae35ec679ecc3b1e9752dfc5843","plan":"8fabeda451d55a8114ab4344c8fa86f93d5d5ef61877015ed3d28e201146a6c9"}}
```

#### Goal

A learner confused by an answered MCQ can understand the disputed distinction,
try applying it, and return to recall without having to reconstruct the attempt
or immediately reorganize the note. The learner can also identify that the
question or source knowledge needs attention instead of being told that every
wrong answer means failed understanding.

This directly advances the backlog's **Near-future direction**: make deliberately
captured knowledge useful through learning and refinement. The immediate outcome
is better understanding; a useful clarification can subsequently become a
reviewed correction under [story 2](#story-2). The conversation can also supply
concrete question-quality cases for SEED-007. Neither connection requires a new
review inbox, retrieval system, or automatic knowledge rewrite here.

#### Scope

On 2026-10-12 the owner accepted learner-private help and separate contest/grade
repair work. Story 1 owns the private explanation/check loop; the follow-up
boundaries are recorded as [story 6](#story-6) and [story 7](#story-7).

- An explicit help request retains the original question, choices, selected
  answer, marked result, and tested focus in the recall conversation. The
  learner can explain why a choice seemed right and correct the assistant's
  interpretation of the difficulty.
- Explain the relevant distinction, addressing the supplied reasoning. Treat
  understanding, a questionable MCQ, and incomplete note content as possible
  explanations, not a compulsory taxonomy or an established diagnosis.
- Offer a follow-up that checks the same distinction. Keep the question,
  learner response, and feedback in the conversation. This is practice, not
  another scheduling Grade or evidence that the original answer was correct.
- Retain the explanation and practice exchange for later reading. Preserve
  the existing memory tracker and the original attempt's history. Help is
  private to the learner who owns the attempt; notebook ownership or membership
  alone does not give access. Reopen a compatible private discussion for the
  same attempt instead of creating another empty thread. Existing shared
  discussions retain their audience and are not silently reused as private help.
  Returning to recall preserves the learner's progress.
- Offer existing authorized refinement when that is the useful next step,
  preserving relevant context for the learner. Retain a question objection in
  the discussion; formal post-answer contest, reason-aware adjudication and its
  inspectable decision belong to story 6, and Grade repair belongs to story 7.
  Repairing recall-chat proposal acceptance and adding durable
  correction attribution belong to story 2; its current handoff defect is not
  treated as an already working capability. Ordinary authorized refinement
  remains available.

**Genuine constraints:** respect the learner's and notebook's access rules;
authorship permission is not granted by a help request. Private learning data
stays outside Portable notebook content. A Grade has ADR 0003's scheduling
meaning; practice must not masquerade as one.

**Deferred promises:** automatic diagnosis after every wrong answer, mastery
certification, compulsory remediation, a persistent task queue, broader search,
and automatic note changes. Author sharing is an explicit later handoff, not
automatic access to private reasoning. Invalid-attempt scoring correction is
separate work, not a product prohibition. These boundaries prescribe no rejection
of naturally supported conversation.

#### Key examples

These are illustrative design cases, not observed learner sessions. A real
confusing MCQ, chosen answer, and learner reasoning have been requested.

1. **A plausible distractor:** A question asks what an observational correlation
   establishes. The learner selected causation because both quantities rose
   together. Asking for help with that reasoning explains the missing causal
   evidence and acknowledges why the distractor seemed plausible. When the
   learner requests a check, a different correlation example tests that same
   distinction; feedback follows their answer rather than revealing it in the
   question.
2. **The suggested cause is wrong:** The assistant treats the first answer as
   forgetting. The learner says both choices satisfy the stated wording. The
   discussion examines and retains that objection;
   disagreement does not become a claim that the learner or note is defective.
   It does not represent conversational agreement as a formal contest verdict.
   The formal post-answer action and its consequences follow stories 6 and 7.
3. **Immediate success:** After the explanation, the learner answers the
   related check well. The conversation records what the response demonstrated
   on this occasion. The existing recall Grade remains the original attempt's
   Grade; the practice response is an ordinary conversational learning event.
4. **Useful content clarification:** The exchange exposes a missing qualification
   in the note. The learner can retain and revisit the explanation and take
   relevant context into refinement. A reviewed, attributable note update is
   story 2's outcome, subject to ordinary author permission.
5. **Return and revisit:** The learner leaves
   after an explanation, continues recall, and later returns to that attempt.
   The associated discussion is discoverable and can continue with its context.
   This makes the seed's retention promise usable without a separate difficulty
   database. On a shared notebook, the author or another member can still use
   their authorized notebook conversations but cannot read this learner's
   private help. An older shared discussion keeps its existing audience.

#### Investigation and research

**Provenance:** repository source and existing tests read on 2026-10-12 in this
preparation workspace at `e1bbd1d2727fced534f473bcba709407a46d91c9`.
At this initial source-reading stage, no app journey, automated suite, or real
model call was run. These observations
establish present responsibilities and gaps; they do not demonstrate explanation
quality, usability, or learning improvement. The earlier assessment above retains
its separate date/revision and limitations. Later preparation probes and their
results are recorded in the associated executable plan.

| Observation from this checkout | Consequence for the story |
| --- | --- |
| `RecallPrompt.getQuestionDetails()` supplies the selected answer and marked result; serialized MCQ includes tested focus and validation rationale. `ConversationAiRequestBuilder` supplies it with the conversation. | Recall chat is already answer-aware. The earlier missing-answer finding concerns the refinement input, not chat. Reuse the existing attempt/context owner. |
| `ConversationInner` already offers explanation, distractor, and question-disagreement starter prompts. `QuestionDisplay` shows question rationale after answering. | Another generic explanation button alone adds little. The new value is reasoning-aware clarification, a useful check, and continuity. |
| Ordinary user and assistant text is persisted and can be reopened. `ConversationButton` nevertheless starts a fresh thread each time; recall-chat close has no recall return. History renders the same fixed-position help control for each item. | Reuse messages, but connect help visibly to the particular attempt and support continuation. The positioning issue is inferred from source, not a rendered observation. |
| `Conversation.setRecallPrompt`, `ConversationRepository`, and `AuthorizationService` give notebook owner/circle members and the initiator access. Recall-chat creation does not check prompt ownership as answer/contest do. | Current conversation reuse is not learner-private. Resolve audience; ensure the help entry authorizes access to the learner's attempt as well as later message reads. |
| Contest accepts an answered prompt at the API boundary but takes no learner reason. `McqService.contest` only marks the MCQ contested; returned advice is not durable adjudication. Rejection wording asks the learner to answer it. | Post-answer contest is a partial mechanism, not a complete post-answer experience. Reuse its evaluator if extending it; distinguish chat disagreement from formal contest. |
| Answer submission immediately writes an AGAIN/GOOD RecallLog and updates the tracker. Correctness, readouts, and frequent-failure warnings consume that event. Regeneration creates an ordinary graded prompt. | Grade repair has wider effects than changing a displayed score. A regenerated recall is not the ungraded practice mechanism. No runtime invalidate-and-rebuild operation was found in the product search. |
| Recall chat has a server-resolved subject note, but `AiResponse` reads only `subject.note` and `ToolCallHandler` returns when that is missing. Tool-call accept/reject state is not retained like ordinary text. | Story 2 has a concrete subject-resolution and durable-decision gap. Do not quietly fold its correction-acceptance promise into story 1. |
| Conversation focus context reads the current note, while the MCQ and chosen answer identify the original attempt. | Explain the attempted question using available evidence without claiming that current note text is a historical source snapshot. No new versioning system is justified. |

Source entry points:
[attempt context](../../backend/src/main/java/com/odde/donut/entities/RecallPrompt.java),
[conversation](../../backend/src/main/java/com/odde/donut/entities/Conversation.java),
[conversation service](../../backend/src/main/java/com/odde/donut/services/ConversationService.java),
[AI request](../../backend/src/main/java/com/odde/donut/services/ai/ConversationAiRequestBuilder.java),
[contest](../../backend/src/main/java/com/odde/donut/services/McqService.java), and
[grading](../../backend/src/main/java/com/odde/donut/services/MemoryTrackerService.java).

External evidence was inspected on 2026-10-12. It informs design hypotheses;
none evaluates Donut's model, learners, or proposed flow.

| Source | Finding and bounded design implication |
| --- | --- |
| [Butler & Roediger (2008)](https://link.springer.com/article/10.3758/MC.36.3.604) | In passage/MCQ experiments, corrective feedback improved later recall and reduced intrusion of incorrect alternatives. Address the selected distractor and relevant distinction; this does not establish that AI chat is superior to a concise explanation. |
| [Chi et al. (1994)](https://onlinelibrary.wiley.com/doi/10.1207/s15516709cog1803_3) | A small circulatory-system text-learning study found gains from elicited self-explanations. Inviting the learner's reasoning is plausible; mandatory reflection after every recall answer is not established by this study. |
| [Asher, Gold & Carvalho (2026)](https://osf.io/preprints/psyarxiv/p3m2k_v1/) | A randomized Python-learning experiment found that structured reflection added time and reduced practice without improving measured performance. The [primary abstract](https://api.osf.io/v2/preprints/p3m2k_v1/) was accessible; full text was not. This short procedural-learning setting supports caution about compulsory gates, not a general conclusion that reflection is useless. |
| [Butler (2010)](https://pubmed.ncbi.nlm.nih.gov/20804289/) | Repeated retrieval improved later retention and transfer relative to repeated study in four experiments. A changed-example follow-up is a reasonable design inference; one generated check immediately after an explanation has not thereby been validated. |
| [Soderstrom & Bjork (2015)](https://bjorklab.psych.ucla.edu/wp-content/uploads/sites/13/2016/11/soderstorm_ra_learningvsperformance.pdf) | This review distinguishes immediate performance from durable learning. Describe practice feedback as what the learner demonstrated now, not mastery or grounds to erase an earlier Grade. |
| [Anki filtered-deck documentation](https://docs.ankiweb.net/manual/filtered-decks#rescheduling) | Preview with rescheduling disabled preserves scheduling state. This is a product precedent for extra practice outside scheduling, not causal learning evidence or a reason to copy Anki's scheduler. |

#### Alternatives, borrowed mechanism, and recommendation

All candidates are judged by resolving the actual confusion, limiting disruption
to recall, preserving trustworthy learning history, and reaching the later
reviewed-correction outcome without forcing it.

| Candidate | Learner outcome and system consequence | Assessment |
| --- | --- | --- |
| Defer / use current chat and manual actions | Existing answer-aware chat and rationale can already explain an answer. The learner manages follow-up, finds the thread again, and reconstructs context for repair. | Strongest smaller alternative; useful today but does not own the follow-up/continuation outcome. |
| Focused conversational explanation and optional check | The learner can supply reasoning, receive a targeted explanation, request a related check, and continue recall. Existing attempt context and persisted messages own the evidence. | Recommended core; bounded changes to the existing conversation, without a new chat system. |
| Compulsory diagnosis and remediation | The learner classifies the problem and completes exercises to a stopping criterion. This needs reliable diagnosis, progress state, and a new interruption policy. | Greater burden and uncertain benefit; defer pending evidence that voluntary help is insufficient. |
| Anchored review with explicit correction | The learner can understand the explanation, challenge the question, or request a content correction as distinct decisions. Formal correction must have a durable result and deliberate grading effects. | Borrow its separation of discussion and mutation for the core; refine contest/grade repair as separate value. |

The last candidate borrows a mechanism from
[code review discussions](https://docs.github.com/en/pull-requests/how-tos/review-pull-requests/commenting-on-a-pull-request):
an anchored concern can be discussed and resolved separately from applying a
change. The reviewed location maps to the particular MCQ/answer; the concern
maps to the learner's reasoning; a reply maps to an explanation; resolving the
discussion maps to deciding that the explanation is understood; applying a
change maps to a deliberate question, grade, or note correction. A separate
concern can remain linked to later work.

**Consequential limit:** understanding is not objectively established by closing
a conversation, and a model is not an authoritative reviewer of every subject.
The analogy justifies separating decisions, not a resolution-state database,
approval workflow, or automatic scoring. Understanding an explanation does not
make the original answer correct; an invalid question does not prove successful
recall.

#### Interaction and architectural choices

Describe actions, not a committed layout or component design:

1. From the answered MCQ, request help with the original attempt visible and
   the conversation audience clear. Offer reasoning as an optional contribution;
   asking for help should not depend on completing a diagnostic form.
2. Give a concise explanation of the disputed distinction. Invite correction
   of the assistant's interpretation, and make the relevant note/rationale
   available as evidence. Insufficient or conflicting evidence warrants an
   explicit uncertainty statement, not an invented diagnosis.
3. Offer a related practice question. Wait for the learner's response before
   giving feedback. Allow more discussion or a return to recall at any point;
   do not turn repeated difficulty into compulsory remediation.
4. Keep this discussion connected to the attempt for continuation and later
   reading. A content problem can hand off to story 2; formal post-answer
   contest and repair are separate outcomes in stories 6 and 7. Involving the
   author requires a deliberate later sharing action, not
   access to the private transcript by virtue of notebook ownership.

| Consequential choice | Effect on the learner and system | Recommendation / status |
| --- | --- | --- |
| Audience of help | Existing chat exposes reasoning to notebook owner/circle. Learner-private help changes conversation access/listing and requires a deliberate later handoff if involving the author. Existing shared threads must not silently change audience. | Owner accepted learner-private help on 2026-10-12. This is necessary story scope, not an independent technical story. |
| Free-form practice versus a structured assessment | Ordinary conversation can retain a related question, response, and feedback. Dedicated scored cards, mastery state, or adaptive exercises need new state and stronger evaluation. | Recommend ordinary conversational practice for this outcome; the original ungraded-check promise remains. |
| Contest and scoring | Invalidating MCQ content currently leaves the original Grade and schedule intact. Neutral invalidation of an attempt needs coherent history/readout/scheduling correction; accepting a selected answer as correct has different semantics. | Owner accepted separate work and the direction of excluding an invalid attempt from evaluation while retaining evidence, not automatically awarding GOOD. Detailed authority and historical reach stay with story 7. |

**Architecture:** reuse RecallPrompt/Answer, Conversation/Message and the existing
focus-context owner. Story 6 reuses the existing contest evaluator. Add no general
difficulty/diagnosis store or second note-version system for a transcript-level
outcome. The existing personal Ownership is a candidate for the conversation's
audience, independently of its recall-prompt subject. Keep old ownership values
and shared conversations intact. The executable plan owns verification of this
reuse and entry authorization; generated-feedback quality needs its own proof.

Relevant Accepted decisions were checked against the ADR index and in-file
status on 2026-10-12; both agree and no successor was identified. ADR 0001's
filename omits the accepted suffix but its authoritative status is unambiguous.

- [ADR 0001 — Ubiquitous language](../../docs/adrs/0001-ubiquitous-language.md):
  preserve the distinction between MCQ content, recall prompt, Answer, contest,
  and Grade. Conversation already covers a note or recall prompt.
- [ADR 0003 — Spaced-repetition scheduling policy](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md):
  each Grade appends an event and changes memory state; history and current state
  must remain coherent and reconstructable. Ordinary practice can stay in the
  conversation. Appending GOOD to compensate for AGAIN would be a second recall,
  not a correction. Retroactive invalidation requires a human-owned policy for
  later events and historical caches; the ADR does not already supply it.
- [ADR 0002 — Git-native Portable notebook tree synchronization](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md):
  learning evidence remains server-side; accepted note edits use the existing
  Git publication boundary. This also follows NORTH-STAR's one accepted-change
  boundary. It does not establish learner-only visibility by itself.
- [ADR 0006 — Failure handling](../../docs/adrs/0006-failure-handling-accepted.md):
  preserve visible failures; do not treat a missing reply or interrupted check
  as successful explanation or practice. Any promised recovery behavior needs
  an actual business outcome, not blanket exception swallowing.

No conflict was found for explanation and ungraded conversational practice.
Directly editing a displayed result while leaving its Grade authoritative, or
using a compensating Grade as undo, is not a supported scoring design.

#### Accepted boundary and effort

- Keep **SEED-073#story-1** as explanation plus follow-up practice and usable
  continuation. **L (2–4 hours), medium confidence**, using existing personal
  Ownership for the new conversation audience and ordinary transcript evidence.
  Creation authorization, listing/access, and reopening are cohesive parts of
  this outcome. Dedicated practice resources, mastery state, and author sharing
  are separate work; adding them would require reassessing this estimate.
  Its evaluator is the learner: the explanation addresses the actual reasoning
  and the related check tests the disputed distinction. It remains useful if
  all further work stops; original history and authorship remain intact.
- Keep **story 2** as the existing independently useful reviewed note correction;
  its identity, scope, and queue position are unchanged.
- [Story 6](#story-6) owns reason-aware post-answer contest and an inspectable
  decision. [Story 7](#story-7) owns repair of an invalid attempt's learning
  effects and needs further policy refinement/decomposition before planning.

The owner accepted separating the expanded scope. The follow-ups are canonical
unqueued candidates; the existing backlog order remains unchanged. Deliver
explanation/practice first; prioritize the follow-ups when selecting later work.
Drop compulsory remediation and broad diagnosis before shrinking the useful
explanation/check loop. Grade repair can be deferred without making practice
graded; deferral must not be described as already repairing an unfair result.

#### Open decisions and unverified premises

1. **Research: representative confusion and model quality.** A real owner-supplied
   question would improve evaluation but is not required to discuss the scope.
   Source reading cannot settle whether generated explanations address reasoning
   or whether a follow-up checks the right distinction. The cheapest next quality
   observation is an owner-reviewed transcript on an agreed case through the
   eventual consuming AI path; mocked tests only prove context and interaction
   contracts. Paid calls are not authorized by this preparation.
2. **Research: learning and usability.** A successful immediate check does not
   establish retention. After delivery, observe whether learners can explain
   the distinction, complete or skip practice, and resume recall; measure delayed
   learning separately if making an efficacy claim. No efficacy claim or
   telemetry project is part of the present scope.

The audience and story boundary decisions are settled. The associated
[execution plan](../slice-plans/001-resolve-confusing-recall-answer/PLAN.md)
owns this story's implementation sequence; the follow-ups' open policy questions
do not block the explanation/practice outcome.

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
  Reuse story 1's handoff when available rather than introducing a second flow.
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
- **Depends on:** Existing contest and recall identities. Story 1's help can
  supply context when present but is not a hard implementation prerequisite.
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

### Repair an invalid recall attempt's learning effects while retaining its history

**Identity:** SEED-073#story-7
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A learner should not carry misleading recall readouts or a
  schedule caused by a question accepted as invalid.
- **Scope:** The owner accepted the direction of excluding an invalid attempt
  from evaluation while retaining its evidence, rather than automatically
  awarding a successful Grade. Make the correction attributable and apply it
  coherently to affected readouts and current scheduling state under ADR 0003.
  Do not reset/recreate the memory tracker or append a compensating GOOD.
- **Evaluation:** An incorrect answer has already produced AGAIN. After the
  question is accepted as invalid and correction is authorized, the learner
  can inspect the original attempt and its correction, and the next recall and
  readouts agree with the chosen correction policy. Repeating the same correction
  does not become another learning event. A later Grade is a decisive case for
  policy refinement, not grounds for a guessed implementation restriction.
- **Value / learning:** Preserve trust in learning history and scheduling when
  question quality, rather than demonstrated recall, caused the result.
- **Effort hypothesis:** Larger than L until authority and historical reach are
  settled; low confidence. This is an unselected follow-up requiring further
  decomposition before any executable plan, not a commitment to one large build.
- **Depends on:** An attributable accepted invalidity decision. Story 6 is the
  preferred supplier, but refinement must establish any genuine blocking
  implementation dependency rather than deriving it from order alone.
- **Safe stopping point:** Corrected scheduling and readouts remain explainable
  from retained history; deferred adaptive tutoring or retrieval adds no dependency.
- **Open decisions:** Who authorizes correction; latest and historical attempts;
  subsequent Grades, Confusion, and cached readouts; personal versus other
  attempts on the same MCQ; and the durable ADR 0003 policy interpretation.
  The accepted direction does not silently answer these. This candidate is not queued.

## Ordering and Scope Reduction

The backlog owns global priority. Within this epic: resolve an actual learning
difficulty, retain a useful correction, improve question evidence through
SEED-007, then improve body search, external-agent evidence access, and cited
comparison. This order favors immediate learning value before broader query
machinery; it does not impose serial technical dependencies.

Drop or defer broader comparison first if capacity is tight, then expanded
external-agent access. Keep the existing workflows usable at every stopping
point. The separately queued capability experiment can use a current notebook
checkout and is neither a supplier nor a consumer blocking this epic.

## Open Decisions

- Select real confusing-question cases with the owner during refinement;
  no failure diagnosis or retrieval-quality gain has yet been measured.
- Define how long proposal decisions and their private conversation evidence
  should remain available, using existing deletion/ownership rules.
- Agree practical latency and cost expectations for retrieval/query evaluation.
- Agent-reported gaps, automatic repeat-failure triggers, and claim-level
  provenance remain later decisions. Detailed contest and invalid-question
  correction policies are owned by stories 6 and 7, not by story 1.

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
