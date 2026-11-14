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

The [2026-10-12 assessment](learning-refinement-and-retrieval-evidence.md#learning-and-retrieval-assessment) records the starting observations and their limits.

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

## Recall Proposal Validation

Donut learners and note authors need useful next steps when recall exposes a
bad question, an unclear source, or difficulty applying an idea. Today a
learner can contest a question, discuss it, or edit the note manually. Those
actions provide a baseline for testing whether each proposal adds enough
value to justify a product change.

The November 13, 2026 MCP export covers 11,091 attempts from one learner over
100 days. A November 14 offline replay compared 24 selected historical cases
against candidate tasks with the answer keys hidden from two AI reviewers.
It shows which changes address specific examples. Current generator behavior,
learner benefit, and how well the findings generalize remain questions for
validation. Missing prompts from intentionally reduced relationship nodes
were outside the review. Visible relationship questions can serve as controls.
While the private evidence is available, use `offline-replay/results.md` and
`offline-replay/question-pairs.md` in the analysis cache described below.

Each story below is a **proposal with a hypothesis**. Complete it by recording
the evidence and recommending whether to proceed to design, narrow and
retest, or stop. The product owner reviews that recommendation. If the
evidence is strong enough to try the idea, scope the learner or author
journey and review the UX/UI design before implementing the approved
behavior. Create a separate follow-up
story when that design and implementation warrant their own backlog item.
The six entries below own validation decisions. Each can be evaluated on
its own using the baseline and examples.

<a id="validate-recall-question-integrity"></a>

### Validate recall question integrity

**Identity:** SEED-073#validate-recall-question-integrity
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why.** A learner needs an incorrect result to reflect what they
  answered. The product owner needs to know which failures still reach a
  learner today and which protections already work.
- **Proposal hypothesis.** Checking option uniqueness and agreement with an
  independently verified answer would catch defects that still reach recall,
  while keeping useful single-answer items.
- **Validate first.** Observe the current recall-generation path with fresh
  output and replay archived payloads through the applicable boundaries.
  Record the revision, configuration and provenance. Trace any discrepancy
  from generated options and proposed key through verification, the item
  shown, and its recorded grade. Compare duplicate choices, a unique correct
  answer rejected by its stored key, and multiple defensible answers with
  useful difficult controls. In the historical cases, the apology item
  (row 8719) combined a duplicate with a wrong key, the kendō cloze
  (rows 9229 and 9350) needed a key correction, and the location-particle
  question (row 3748) needed different choices.
- **Decision.** Show which failure can still be reproduced, which checks
  detect it, and which sound questions those checks would flag. Keep the
  smallest de-identified replay examples for any justified regression proof.
  A confirmed bounded defect can be scoped as a repair. If a broader
  verification or recovery experience looks promising, design the UX/UI for
  handling a flagged question before implementation and scope a follow-up
  story if needed. A clean baseline is a valid result.
- **Effort hypothesis.** M, medium confidence. A focused baseline and replay
  are bounded if the current generation and grading paths can be observed.
- **Cleanup reminder.** Apply the [shared evidence closeout](#close-out-temporary-recall-evidence).

<a id="validate-spelling-objectives"></a>

### Validate spelling objectives and cues

**Identity:** SEED-073#validate-spelling-objectives
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why.** A learner practicing written form, reading, or vocabulary
  needs to know which skill a prompt asks for and receive credit for answers
  that satisfy it.
- **Proposal hypothesis.** Stating one objective and using one intact cue
  makes spelling tasks easier to interpret and grade fairly, while
  preserving intentional distinctions in spelling and usage.
- **Validate first.** Compare recoverable original cues and rewrites with
  keys hidden. Replay legitimate alternatives, actual form errors, and close
  semantic neighbors under each stated objective. Use examples such as the
  solution versus process for *brine*, the reading of 伝手, and the small っ
  in おっしゃる. Include a broad cue such as *meadow* where the answer rule
  may still need review. Keep unavailable historical cues out of wording
  judgments.
- **Decision.** Show where the rewrite resolves the ambiguity, which valid
  variants it accepts, and which intended errors it still distinguishes.
  Identify any cases where the rewrite changes the skill tested. If
  promising, design how learners see the objective and how authors review
  alternatives before implementation. Scope a follow-up story if needed.
- **Effort hypothesis.** S, medium confidence for a small paired review using
  existing examples.
- **Cleanup reminder.** Apply the [shared evidence closeout](#close-out-temporary-recall-evidence).

<a id="validate-short-recall-lessons"></a>

### Validate short lessons after recall misses

**Identity:** SEED-073#validate-short-recall-lessons
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why.** A learner who makes a meaningful mistake needs a way to
  understand the distinction and use it in another situation.
- **Proposal hypothesis.** An optional lesson that contrasts the learner's
  answer with the intended idea leads to better delayed, unassisted
  application than ordinary feedback alone.
- **Validate first.** Use independently checked items that isolate a real
  misunderstanding, such as explicit giver and recipient roles or the
  position of literals in RDF. Compare ordinary feedback with a short
  explanation and practice, with the same key facts available to each group.
  Score a different application after a comparable delay without assistance.
  Record uptake, effort, and missed follow-ups alongside the answers. The
  offline replay already checks that sample tasks are answerable.
- **Decision.** Show whether a learner applies the distinction in a fresh
  setting, with enough evidence and acceptable effort to justify another
  trial. If promising, design the entry point, the learner's choice to
  continue, and the follow-up experience before implementation. Scope a
  follow-up story if needed.
- **Effort hypothesis.** L, low confidence. Preparation can be small, but
  the validation needs learner participation and an elapsed follow-up.
- **Cleanup reminder.** Apply the [shared evidence closeout](#close-out-temporary-recall-evidence).

<a id="validate-source-backed-note-edits"></a>

### Validate source-backed note edits from recall

**Identity:** SEED-073#validate-source-backed-note-edits
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why.** A note author needs a concrete correction when the note
  caused a misleading judgment and a way to preserve useful distinctions.
- **Proposal hypothesis.** Showing the attempt, relevant source, and
  smallest proposed edit makes it easier for authors to improve a note and
  the questions that rely on it.
- **Validate first.** Verify that the relevant claim or answer rule remains
  in the current note. Compare a factual correction, a scoped usage edit,
  and an alternative that only fits one sense with the author's current
  workflow. Ask the author whether they would accept each edit. Replay a
  question from the proposed revision and a valid near miss. Use source
  checks such as the classification of 存じる and the distinct roles of
  OpenID Connect and WebID.
- **Decision.** Record whether the author can evaluate the change, whether
  it fixes the source or question, and whether the intended distinctions
  survive. If promising, design review, revision context, and author
  approval before implementation. Scope a follow-up story if needed.
- **Effort hypothesis.** M, medium confidence. A few authored examples and
  accessible source material are enough for the first review.
- **Cleanup reminder.** Apply the [shared evidence closeout](#close-out-temporary-recall-evidence).

<a id="validate-within-note-objectives"></a>

### Validate follow-up by learning objective

**Identity:** SEED-073#validate-within-note-objectives
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why.** A learner reviewing a broad note should be able to revisit
  the part they missed while preserving the note's context and progress on
  related material.
- **Proposal hypothesis.** Using the evidence for the specific objective
  selects more useful follow-up questions than using the note's latest
  grade alone.
- **Validate first.** Identify the objective an original question actually
  tested and compare note-wide versus objective-specific follow-up choices.
  Include a question with independently correct and incorrect components, a
  success on a different objective, and a later success on the same one.
  Compare handling a call with the grammatical frame of 対応する and the
  wartime use of Senate House with its original building plan.
- **Decision.** Show whether the proposed follow-up targets an unresolved
  objective and whether recent evidence prevents needless practice. If
  promising, design how a learner sees or chooses the objective and how it
  stays connected to the note before implementation. Scope a follow-up
  story if needed.
- **Effort hypothesis.** S, medium confidence for a small annotated replay.
- **Cleanup reminder.** Apply the [shared evidence closeout](#close-out-temporary-recall-evidence).

<a id="validate-question-generation-purpose"></a>

### Validate when question generation should abstain

**Identity:** SEED-073#validate-question-generation-purpose
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why.** A learner should spend recall time on a useful fact,
  distinction, or skill, including catalog details they deliberately chose
  to remember.
- **Proposal hypothesis.** Checking the learning purpose before generating
  reduces incidental metadata questions while retaining useful facts and
  substantive relationship questions.
- **Validate first.** Compare generation decisions given explicit learner
  goals for a note with substantive content, a sparse file or URL pointer,
  a meaningful relationship, and intentional catalog practice. Review
  questions the approach would show or withhold against each goal. Include
  the Pitest URL and mutation mechanism from the same note, the
  attachment-filename case, and the ことだから／ことから contrast.
- **Decision.** Show which low-value questions are avoided, which worthwhile
  questions would be lost, and when asking about intent changes the outcome.
  If promising, design how the learner expresses a goal and what happens
  when no question fits before implementation. Scope a follow-up story
  if needed.
- **Effort hypothesis.** S, medium confidence for an offline set with and
  without explicit catalog goals.
- **Cleanup reminder.** Apply the [shared evidence closeout](#close-out-temporary-recall-evidence).

### Ordering and scope reduction

Question integrity goes first because it determines whether a recorded miss
is trustworthy evidence. Spelling is next because the replay shows distinct
cue and scoring failures that can be checked cheaply. The lesson tests
learning directly. Source edits, objective routing, and generation purpose
follow the observed proposal ranking. Each can start independently with
checked examples. Finishing any one leaves a product decision and supporting
evidence that remain useful if the others are deferred. If capacity shrinks,
defer the lowest-ranked validations first.

<a id="close-out-temporary-recall-evidence"></a>

### Close out temporary recall evidence

Each validation closeout records whether any approved follow-up still needs
the temporary evidence. The owner of the last story still using that
evidence checks the other dispositions and any approved follow-up before
removing the shared cache. A decision to stop releases that story's use.

Once the validation stories are settled and remaining approved use is
finished or covered by a minimal de-identified handoff, remove
`.planning/tmp/recall-history/` from the recall-history analysis worktree
and any temporary copies created for this investigation. This includes raw
MCP exports, derived datasets, reports, reviewer packets, and scratch scripts.
Keep only the non-sensitive conclusions and minimal de-identified examples
needed for approved work or maintained regression tests. Verify the temporary
files are gone and record the closeout. The product's notes and recall
history are outside this cleanup.

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
