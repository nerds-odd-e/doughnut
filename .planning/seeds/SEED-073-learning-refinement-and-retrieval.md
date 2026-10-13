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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/003-reviewed-conversation-note-corrections/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"d65e4c951598e94aa39cc2bf8a2a732a23a7f7b1f0a1fcd07779c56934ddd081","plan":"ee67fe1576c09e75136b72be1b4b68c41a4f22f95f539a445b4fc79f5f77b0b5"}}
```

**Goal:** A note author who has found a useful clarification while discussing
a note or an answered MCQ can review it into the existing subject note without
copying and reconstructing it, then inspect what was decided in that conversation.
This closes the conversation-to-knowledge gap while preserving author control.

**Scope:**

- Reuse chat's note-content and title suggestions. Show an editable correction
  within the conversation, with the subject note, reason, and originating
  exchange. The author explicitly accepts or declines it; discussion and
  proposal generation alone do not change the note.
- Retain the proposal and its decision with the conversation. Reopening shows
  pending, declined, or accepted review state; acceptance records the actual
  reviewed text and the accepted Git revision, with a link to the subject note.
  Later note edits do not relabel an earlier decision or its change reference.
- Acceptance uses ordinary note validation, edit rights, and Git publication.
  Resolve the subject on the server for both direct-note and MCQ conversations.
  The saved correction and its accepted decision must agree. Retrying the same
  completed decision returns that result instead of applying another correction.
- Review against the note version the author saw. If publication has advanced,
  refuse the stale acceptance, keep the author's current input available, and
  let them load current content and explicitly review again. Do not merge or
  overwrite unseen accepted work. Failed saves leave the review retryable;
  show success only after a confirmed decision.
- Keep private review evidence under the conversation's existing audience and
  outside Portable content. Publication still requires note edit rights, even
  for a participant who can read and discuss the note. The existing recall
  attempt, tracker, history, and schedule retain their meaning.
- This delivery corrects one existing subject note. Discovery-note creation,
  coordinated corrections to several notes, a general review queue, automatic
  rewrites, a history browser, undo, and autosaving unsubmitted editor keystrokes
  are deferred promises. Ordinary title-validation and reference-handling rules
  still apply; this story introduces no separate rename policy.

**Key examples:**

1. An author asks in an MCQ conversation to retain the distinction between
   correlation and causation. The proposal adds a common-cause example. The
   author edits the wording and accepts; the subject note contains that reviewed
   wording. Reopening the conversation shows the reason, originating exchange,
   accepted wording, decision, and resulting Git revision, even after another
   ordinary note edit.
2. The same review works in a direct-note conversation, including a suggested
   title correction. The author can inspect the subject and proposed value
   before saving, under ordinary note validation.
3. The author declines an alternative. Reopening shows the declined proposal
   with its reason and exchange; authored content remains unchanged. Closing
   the conversation without deciding leaves the proposal pending.
4. Another edit is published while a proposal is being reviewed. Accepting
   reports that the review is stale; the author's input remains available.
   Loading current content and reviewing again lets the author decide against
   that version. A repeated acceptance after a lost response returns the
   retained accepted result, including its original revision.
5. A reader of a shared note can discuss the distinction but cannot accept a
   correction without edit rights. Someone outside the conversation's audience
   cannot read its private review evidence. A correction does not regrade or
   reassimilate the original recall attempt.

**UI:** Keep the review in the conversation, adjacent to its originating
exchange. Identify the note and changed field, show the current and proposed
values and reason, and allow editing before Accept or Decline. Display the
server-confirmed outcome where the proposal appeared; accepted reviews offer
an Open note action and their revision reference. Distinguish closing from
Decline. Make stale-review recovery explicit and preserve current input on a
failed save. Layout and component choices belong to implementation.

**Alternatives and decision (2026-10-14):** Terry accepted review within the
conversation. Manual editing is smaller but loses the promised handoff and
retained decision. Carrying the clarification into existing refinement reuses
its handoff, but that destination focuses on extraction/removal and would need
a correction outcome plus a return of its decision to the conversation.
The chosen flow extends the existing suggestion responsibility with durable
review state and a server-owned acceptance operation.

The borrowed mechanism is a reviewed change request: the proposed correction
is the submitted change, the author its reviewer, the exchange and reason its
rationale, and the Git revision the applied result. This keeps revision before
acceptance and later inspection together. Its consequential limit is that an AI
explanation provides no independent validation of correctness; the author still
judges it. It adds no separate reviewer, branch workflow, or general inbox.

**Architecture:** Existing conversation ownership governs review evidence;
existing note mutation and Git publication owners govern accepted content.
Follow [ADR 0002 — Git-native Portable notebook tree
synchronization](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
and [One accepted-change boundary](../NORTH-STAR.md#one-accepted-change-boundary).
Use existing note identities and Git revisions; proposal text is review evidence,
not a second content authority or synchronization protocol. Commit acceptance
and its decision coherently under the existing publication transaction, rather
than relying on two independent browser writes. The accepted revision must be
captured at that boundary. Keep the private transcript out of published note
content except for the exact correction the author reviewed. No ADR exception
is proposed.

**Preparation evidence:** Source inspection on 2026-10-14 in
`save-a-useful-chat-clarification-as-a-reviewed-n` at
`3aceb37f36dfe3b1e1979026b78f30b658bb0c28` found that `AiResponse.vue` passes
only `conversation.subject?.note` to suggestion acceptance, while
`Conversation.getSubjectNote()` resolves direct-note and MCQ subjects. The
server stream persists ordinary assistant text; browser tool-call state does
not persist proposal decisions. `useNoteRefinement` provides removal/extraction,
and `ConversationComponent.refinement.spec.ts` describes the reviewed
clarification handoff. These were source/test readings, not browser observations.
Subsequent executable observations and their limitations belong in the plan.

- **Effort hypothesis:** L (2–4 hours), medium confidence; durable stream
  capture and atomic attribution are the principal implementation risks.
  Production delivery also needs the existing schema-first release checkpoint.
- **Depends on:** No new product story. Reuse the existing
  [reviewed refinement handoff](../../docs/recall-help.md#reviewed-refinement)
  knowledge without routing this correction through extraction.
- **Safe stopping point:** A reviewed, attributable correction is useful
  independently of adaptive retrieval or a general review system. Execution
  increments keep existing editing usable and do not enable unsafe acceptance.

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
  Grade; a learner-confirmed ambiguity exclusion repairs grade and schedule
  ([recall help](../../docs/recall-help.md#learner-confirmed-ambiguity-exclusion)).
  Learner reasoning remains private
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

<a id="story-9"></a>

### Keep private recall help controls in view for long conversations

**Identity:** SEED-073#story-9
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/002-private-recall-help-controls/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"0e6415f9dc7ded9530810683151d5182b3f8f1b7f6d1adb3c2e79d6eec3b236b","plan":"29ac14dfa92ce95b91e7343aa1f328901c3fcfb7fa457d5bb81646b2abd3131e"}}
```

**Goal:** A learner reading a long private recall-help conversation can identify
and close help without scrolling back to find its controls, then continue the
same recall attempt.

**Scope:**

- Keep the help heading and a clearly named Close control visible within the
  available viewport while its question, selected answer, exclusion reason,
  and conversation remain fully readable by scrolling.
- Preserve this result when opening or reopening help, receiving a long reply,
  scrolling either way, and confirming an ambiguity exclusion.
- Closing by the visible control or Escape returns to the same attempt with
  recall progress preserved and focus on its help opener. Existing nested
  refinement returns to help before Escape closes help.
- Preserve existing conversation identity, access, exclusion, and scheduling
  behavior. This is a presentation change, with no new storage or API contract.
- Composer pinning, scroll-position restoration, conversation navigation
  redesign, and a product-wide modal redesign are deferred promises.

**Key examples:**

- Open the same attempt's help with a long reason and several exchanges at
  1728×963 → its heading and Close are visible; scrolling reaches the full
  question, reason, and transcript without losing those controls.
- Read help, receive a long reply, then confirm an exclusion with a long reason
  → the updated attempt and conversation remain readable and Close remains
  usable. The existing exclusion outcome is preserved.
- Close and reopen that conversation → controls remain visible and the learner
  can continue reading. Close or Escape returns focus to Get private help with
  the same attempt and recall progress.
- Use a narrower, shorter viewport (390×600 is the investigated example) →
  content still scrolls within the available space while the heading and Close
  remain reachable. These sizes are examples, not supported-size restrictions.

**UI:** Keep help's identity and its dismissal available independently of the
reading position. Long content is read by scrolling, not truncated or collapsed
as a substitute for access. Reuse the existing keyboard and return journey;
this refinement prescribes no particular layout or new interaction.

**Investigation, 2026-10-14:** At `2534af9c3a13b1712d719b2fb42a6fac5b3dedbc`
in this story's owned preparation workspace, real mounted frontend components
in headless Chromium reproduced the outer modal scrolling its controls above
the viewport. A disposable bounded-body experiment kept the heading and modal
Close visible through confirmation, a long reply, scrolling, and reopening at
both example sizes; the same geometry assertions failed on the original code.
All 350 frontend test files (2,189 tests) and both frontend typechecks passed
with the experiment. All scratch changes were reverted. This settles local
layout feasibility, not real-model quality or mobile-device/keyboard behavior.
The executable plan records the setup, commands, results, and limitations.

The architecture check found no consequential new responsibility or Accepted
ADR conflict: the existing modal owns dismissal and the conversation retains
its data and learning semantics. No seed Architecture section or North Star
change is needed.

- **Evidence, 2026-10-14:** Bounded manual testing of learner-confirmed ambiguity
  exclusion observed Close at y=-157 after confirmation and y=-793 when
  reopening a longer exchange at 1728×963. Scrolling upward recovered it at
  y=74; Escape closed help and restored focus to Get private help. This was a
  usability opportunity, with no established root cause. The observation used
  product base `2587fb27cb95699d70ed053af1aacdfa32095194`; the held app reported
  revision `458b2ea204ff8fe11c9a037e71c46582f42c5fc0`.
  [Dated inputs, actions, results, and limitations](https://github.com/nerds-odd-e/doughnut/blob/2537f7aca24505f4e4b63e4bb62f2be5888b9911/.planning/seeds/SEED-073-learning-refinement-and-retrieval.md#manual-ambiguity-exclusion-evaluation)
  retain the full provenance. In that revision, `confirmed-help.jpg` under
  `.planning/manual-testing-evidence/manual-ambiguity-exclusion-20261014/`
  shows clipping; `reopened-help-clipped.jpg` shows the recovered view after
  upward scrolling. This is agent-simulated learner evidence from one desktop
  viewport; other viewport behavior remains unobserved.
- **Effort hypothesis:** S (30–60 minutes including delivery), high confidence
  after the bounded browser experiment; one cohesive layout/proof change.
- **Depends on:** No new story; the existing private recall-help dialog supplies
  the journey to improve.
- **Safe stopping point:** Learners can read and close long help conversations
  through visible controls.

## Ordering and Scope Reduction

The backlog owns global priority. Within this epic: resolve an actual learning
difficulty, retain a useful note correction, improve question evidence through
SEED-007, then improve body search, external-agent evidence access, and cited
comparison. This order favors immediate learning value before broader query
machinery; it does not impose serial technical dependencies.

Drop or defer broader comparison first if capacity is tight, then expanded
external-agent access. Keep the existing workflows usable at every stopping
point. The separately queued capability experiment can use a current notebook
checkout and is neither a supplier nor a consumer blocking this epic.

## Open Decisions

- Select real learner cases during evaluation; human comprehension,
  retention, and retrieval-quality gains remain unmeasured.
- Define how long proposal decisions and their private conversation evidence
  should remain available, using existing deletion/ownership rules.
- Agree practical latency and cost expectations for retrieval/query evaluation.
- Agent-reported gaps, automatic repeat-failure triggers, and claim-level
  provenance remain later decisions. Detailed contest and invalid-question
  correction policies are owned by story 6.

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
