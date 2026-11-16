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

<a id="local-recall-evidence"></a>

### Local recall evidence

Refine and execute these stories on the machine that holds the private
evidence. The cache is in `.planning/tmp/recall-history/` in the default
checkout at `~/git/doughnut`. From any linked worktree, use the default
checkout's cache. The folder is local and Git-ignored. Start with
`offline-replay/results.md` and `offline-replay/question-pairs.md` for the
reviewed cases, and use `recall-answers-100d.json` to resolve the original
attempt rows.

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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/098-validate-recall-question-integrity/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"6f0bc26983d1573fc73c24624386523bbb84255a630a13e32c46449f207541a0","plan":"413b3720fb4da710eb4c2860483bcd6c3b0125750160c06d44499f3afd25d57c"}}
```

**Goal.** A learner needs a wrong mark on a multiple-choice recall question
to mean that they chose a wrong answer. The product owner needs to know
which question defects can still reach a learner today, by which route, and
which existing protections stop them. This story delivers that evidence
with a recommendation for each defect: proceed to design, narrow and
retest, or stop. It contributes to the epic by settling whether a recorded
miss is trustworthy evidence before later stories build on misses. It
changes no product behavior.

**Proposal hypothesis.** Checking option uniqueness and agreement with an
independently verified answer would catch defects that still reach recall,
while keeping useful single-answer items.

**What refinement found.** The
[2026-11-16 observations](learning-refinement-and-retrieval-evidence.md#recall-question-integrity-observations)
record the inputs, results, and limits.

- A question reaches a learner by one of three routes: generated on demand,
  generated ahead by the hourly batch, or stored earlier and shown when its
  tracker next comes due. A stored unanswered question has no age limit and
  is not checked again.
- Since 2026-08-22 both generating routes reject repeated choice text and
  take the key from the answer the generator names as correct. The apology
  and kendō questions were generated before that change. In attempts
  answered since, no choice text repeats and no rationale opens by endorsing
  a different choice.
- The location-particle question was generated on 2025-02-06 and shown on
  2026-10-06. Of the 6,659 multiple-choice answers from 2026-08-23, 336
  were to questions generated before the change. In a sample of 60
  trackers, 48 stored unanswered questions were waiting, 46 of them
  generated before 2026-05-16.
- The independent verifier already exists. It answers the question without
  the key and upholds it only when it selects exactly the stored key. It
  runs when a learner contests a question before answering. It does not run
  when Production generates a question.
- Both recovery actions are in use but rare: 4 of 585 sampled questions
  were contested, and one attempt has ever been excluded. The memory
  tracker page also lets a learner delete that tracker's unanswered stored
  questions.
- A question with two defensible answers generated after the change
  (row 4917) reached recall.

**Validation result and recommendation (2026-11-16, for the owner's
review).** The [validation results](learning-refinement-and-retrieval-evidence.md#recall-question-integrity-results)
hold the counts, commands and limits. A clean baseline holds for repeated
choices on both generating routes; the other classes still reach a learner.

| Failure class | On demand | Batch | Stored |
| --- | --- | --- | --- |
| Repeated choices | **Stop.** Rejected at generation since 2026-08-22. | **Stop.** Rejected; a maintained test holds it. | **Proceed to design**, within the stored-route decision below: 10 stored questions from 2023–24, 3 next in line. |
| Unique correct answer rejected by the key | **Narrow and retest.** None seen after the change in the reviewed set; the generator is shared with batch. | **Narrow and retest.** One case (row 5347) among 46 reviewed misses; the verifier upheld it and no check without the key can see it. | **Proceed to design**, within the stored-route decision. |
| More than one defensible answer | **Narrow and retest.** Row 7893; the verifier flagged it on feasibility only. | **Narrow and retest.** Rows 4917 and 5037; the verifier caught 4917 and missed 5037. | **Proceed to design**, within the stored-route decision; row 3748 was shown 20 months after generation. |

- **Stored route.** Every check added since a question was stored is
  bypassed. Of the 2,650 questions next in line, 1,969 were generated
  before 2026-05-16 and 641 fail a check today's generation applies without
  the key. The owner chooses between checking a stored question when it is
  shown and discarding stored questions older than a protection; this
  evidence does not separate them.
- **Generating routes, key and two-answer defects.** The verifier as
  deployed would replace about half of sound questions: it flagged 12 of 26
  sound questions and 10 of 15 the learner answered correctly, almost all
  on feasibility. Judged on its selected choices alone, it flagged 4 of 8
  defects and 1 sound question. Retest the choices-only rule on a larger
  sample of misses after 2026-08-22 before choosing "verify before a
  question is shown" or "verify after a miss only". Each retest needs its
  own paid-call authorization.
- **Hypothesis.** Option uniqueness already stops repeated choices at
  generation. Agreement with the verifier finds two-answer questions by its
  choices, but its feasibility judgment costs too many sound questions, and
  it rarely finds a wrong key.
- **Regression examples.** A route-level test is justified only with an
  approved change. For a stored-route change: stored choices ["alpha",
  "beta", "beta", "gamma"] with the key at the second position; answering
  the third is marked wrong today. The on-demand route has no route-level
  repeated-choice test (its proof is the shared
  `GeneratedMcqTest.rejectsInvalidSemanticAnswerSets`); for example,
  answer "alpha" with distractors "beta", "gamma" and " alpha " stores
  nothing.
- **Temporary evidence.** Until the owner decides, keep the cache's
  `integrity/` folder: a retest reuses its census, labels and verifier
  route. A decision to stop releases this story's use.

| Choice | Evidence now |
| --- | --- |
| Verify before a question is shown | Detects 5 of 8 defects but flags about half of sound questions as deployed; choices alone 4 of 8 with 1 false flag. Needs the retest. |
| Verify after a miss only | Same verifier figures on missed questions (5 of 8 detected, 11 of 25 sound flagged); 963 of 8,489 attempts were misses. |
| Keep showing stored older questions | 8,254 unanswered stored questions, 7,573 generated before 2026-05-16; repeated choices and row 3748 show stored defects reach the learner. |
| Check a stored question when it is shown | Today's keyless checks would reject 641 of the 2,650 next in line; key defects need the verifier. |
| Discard stored questions older than a protection | 1,969 of 2,650 next in line predate 2026-05-16, 2,119 predate 2026-08-22. |

**Scope.**

Required results:

1. For each failure class (repeated choices, a unique correct answer
   rejected by the stored key, more than one defensible answer) and each
   route, record whether the failure can still reach a learner today and
   the evidence for it.
2. Count the stored unanswered questions by generation period and how many
   of them today's generation checks would reject.
3. For each check, record which failures it detects and which sound
   questions it flags. The checks are the repeated-choice comparison, the
   key taken from the named answer, the verifier, a comparison of a wrong
   selection with answers credited for the same note, the learner's
   contest, the exclusion after an answer, and the deletion of a tracker's
   unanswered questions. Include how often the learner used contest and
   exclusion in the same period.
4. Measure the verifier with the key hidden on the named cases and the
   controls, separately for questions the learner missed and questions the
   learner answered correctly.
5. For each failure that still reaches a learner, record what the learner
   saw and could do at each step: the question, the result, and any
   recovery offered.
6. Keep the smallest de-identified examples for each regression proof the
   evidence justifies.
7. Recommend a decision for each failure class and route. A clean baseline
   is a valid result.

Required restrictions:

- Write nothing to Production. A contest marks the question contested and
  replaces it, so every replay runs outside Production
  ([ADR 0007](../../docs/adrs/0007-environments-and-isolation-accepted.md)).
- Keep private recall evidence in the local cache. Planning files hold
  counts, dates, row numbers, and de-identified examples only, as the
  shared constraints and the closeout require.
- Make paid model calls only under the owner's authorization for a stated
  maximum, as the agent map requires for paid OpenAI use.

Deferred promises:

- Any product change: a repair, an age limit for stored questions, running
  the verifier when a question is generated, or an experience for a flagged
  question. Each follows the owner's review as its own story.
- An error rate for the deployed generator. This story shows whether a
  failure still occurs; observed counts are reported as counts.
- Spelling questions, owned by
  [Validate spelling objectives and cues](#validate-spelling-objectives).
- A key that is wrong because the note is wrong, owned by
  [Validate source-backed note edits from recall](#validate-source-backed-note-edits).
  The verifier reads the same note as the generator, so their agreement
  cannot show this defect.

**Key examples.**

- **Repeated choices, newly generated.** The apology question (row 8719)
  has two identical choices → it is replayed as generator output at the
  current revision → generation rejects it and stores nothing. The record
  says this failure stops at generation since 2026-08-22.
- **Repeated choices, stored.** A question with two identical choices was
  stored before 2026-08-22 → its tracker comes due → it is shown, and the
  learner's selection is marked wrong although its text equals the key's
  (row 8518). The count of such stored questions decides whether this still
  reaches a learner.
- **Key against rationale.** Among attempts answered from 2026-08-23, 23
  marked wrong have a rationale that names another choice and not the key
  → each is reviewed with the key hidden → it is recorded as a real
  disagreement or a wording difference. No real disagreement is a clean
  result for this class.
- **Old stored question.** The location-particle question (row 3748) was
  generated on 2025-02-06 with three choices, two of them defensible → its
  tracker came due on 2026-10-06 → it was shown and graded with no check
  applied. The record names the stored route and the verifier's outcome.
- **Two defensible answers from the current generator.** Row 4917 was
  generated on 2026-09-20 → the verifier answers it with the key hidden →
  selecting both answers counts as detected; selecting only the key counts
  as missed, with the verifier's stated reason.
- **Sound difficult control.** The PIT bytecode question (row 1943), the
  ことだから and ことから contrast (row 4032), and the credited kendō
  question (row 8739) are sound → the verifier answers each with the key
  hidden → upholding is correct; a flag is counted as a false flag with its
  stated reason.
- **Answer credited elsewhere.** The kendō selection marked wrong in rows
  9229 and 9350 is the key of row 8739 with the same stem → the comparison
  flags both. Row 4917 has no such twin and is not flagged. The record
  states how many attempts the comparison can reach.
- **No authorization for paid calls.** The owner does not authorize the
  verifier replay → the verifier rows read "not measured" and the
  recommendation covers only what the unpaid evidence supports.

**Choices this validation informs.** Each choice is the owner's to make
after the evidence; the story favors none.

| Choice | For the learner | For the system |
| --- | --- | --- |
| Verify before a question is shown | Never sees a flagged question. A question generated on demand takes one more model call before it appears. | A verification step on both generating routes; the batch route has none today. About one more model call per question. |
| Verify after a miss only | Sees and answers the defective question, then has the mark reviewed. No added wait. | Verification for missed questions only (963 of 8,489). The mark is undone through the existing excluded attempt. |
| Keep showing stored older questions | Keeps receiving questions from earlier generators. | No change. |
| Check a stored question when it is shown | A failing stored question is replaced, with a wait at that moment. | Checks move from generation to the point of showing and cover every route. |
| Discard stored questions older than a protection | Receives a newly generated question. | One removal of unanswered stored questions; generation refills them. |

**Architecture.**

- Checks apply only when a question is generated. A stored question
  bypasses every check added after it was stored.
- The verifier and the generator read the same note context. They can agree
  on a wrong key when the note is wrong or covers one sense only.
- [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)
  lists trustworthy Grades as a prerequisite. This story measures that
  prerequisite for multiple-choice questions and conflicts with no Accepted
  decision. Recovery after a miss stays the learner-confirmed excluded
  attempt; an automatic exclusion is not examined.

**Ways considered.**

- **Replay the named cases only.** It answers whether today's code lets
  those four through. It misses failures outside the list and the stored
  route.
- **Census of the archive by route.** It uses data already held, needs no
  paid call, and covers every route. It cannot judge by itself whether a
  question has two defensible answers.
- **Compare a wrong selection with answers credited for the same note.**
  Borrowed from exam item analysis, where an item is flagged when able
  candidates agree on an answer the key rejects. Here the learner's own
  credited answers stand in for the candidates. The analogy breaks because
  most questions are asked once: 231 of 8,177 stems repeat, so most
  defective questions have no second observation.
- **Replay the verifier on labelled cases and controls.** It is the only
  way to measure the existing verifier. It needs paid calls.
- **Switch the verifier on in Production and watch.** This is a product
  change, so it is a possible outcome and not a validation.
- **Selected direction.** Run the census first, measure the comparison
  inside it, then replay the verifier on what the census cannot decide.

- **Effort hypothesis.** M, medium confidence. The census and the replay
  through the generation checks are small. The stored-question count needs
  a read-only production query, and the verifier replay needs the owner's
  authorization.
- **Evidence and cleanup.** Use the [local recall evidence](#local-recall-evidence)
  and apply the [shared closeout](#close-out-temporary-recall-evidence).

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
- **Evidence and cleanup.** Use the [local recall evidence](#local-recall-evidence)
  and apply the [shared closeout](#close-out-temporary-recall-evidence).

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
- **Evidence and cleanup.** Use the [local recall evidence](#local-recall-evidence)
  and apply the [shared closeout](#close-out-temporary-recall-evidence).

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
- **Evidence and cleanup.** Use the [local recall evidence](#local-recall-evidence)
  and apply the [shared closeout](#close-out-temporary-recall-evidence).

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
- **Evidence and cleanup.** Use the [local recall evidence](#local-recall-evidence)
  and apply the [shared closeout](#close-out-temporary-recall-evidence).

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
- **Evidence and cleanup.** Use the [local recall evidence](#local-recall-evidence)
  and apply the [shared closeout](#close-out-temporary-recall-evidence).

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
`.planning/tmp/recall-history/` from the default checkout at
`~/git/doughnut` and any temporary copies created for this investigation.
This includes raw MCP exports, derived datasets, reports, reviewer packets,
and scratch scripts.
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
