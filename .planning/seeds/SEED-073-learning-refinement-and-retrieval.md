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
The entries below own validation decisions. Each can be evaluated on
its own using the baseline and examples.

<a id="explore-reliable-recall-question-checks"></a>

### Explore reliable recall question checks with paid experiments

**Identity:** SEED-073#explore-reliable-recall-question-checks
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/099-explore-reliable-recall-question-checks/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"0be3f81589944b2744b2e65b26f54d850f0af62b511a74091badeab6b2f70185","plan":"26622ddb9c3d2be9250a4c32e6377ede02da9821abd36f2ad6bb7540b1bb1049"}}
```

**Goal.** A learner needs a wrong mark to reflect a wrong answer, rather
than a defective key or another defensible choice. This exploration gives
the owner a reproducible comparison of three checks and a recommendation
to proceed to design, narrow and retest, or stop, naming the failure
classes and routes the evidence supports. The broader ambition, a
trustworthy wrong mark in the product, stays with a later design story.

**Starting evidence.** The completed integrity validation established the
stored-question exposure and existing repeated-choice protection, but no
reliable semantic check. The deployed verifier caught 5 of 8 labelled
defects and flagged 12 of 26 sound controls; its choices alone caught 4 of
8 and flagged 1 of 26. See the
[experience summary](learning-refinement-and-retrieval-evidence.md#recall-question-integrity-experience).

**Hypothesis.** A check focused on the supported answers, with sufficient
note evidence, distinguishes wrong keys and multiple defensible answers
more reliably than the verifier's broad feasibility judgment, and the
improvement survives unseen notes and run-to-run variation.

**Scope.**

- **Compared checks.** (1) The deployed verifier: the product's own
  request and decision rule, focus note only. (2) Choices-only: the same
  responses, flagged when the selected choices differ from the stored key;
  it costs no calls. (3) One narrower answer-checking prompt, which may
  carry the linked evidence a question requires. All paid calls use
  `gpt-6.1-sol` with the product's settings, so only the prompt differs.
- **Labels before spending.** Reuse the private census, labels, requests
  and responses. Two fresh session reviewers judge each question against
  the note and its required linked evidence with the stored key hidden.
  They separate an ambiguous or incorrect note from a bad question. A
  disagreement stays unresolved and unscored. Record whether the note
  changed after the question was generated and any missing historical
  content. Labelling makes no paid call.
- **Historical set.** About 40 scored questions from the 47 already
  labelled, relabelled as above. The narrower prompt is worded against
  this set only.
- **Held-out set.** About 24 scored questions from notes outside the
  historical set, labelled before any call and holding every labelled
  defect found plus sound controls. The narrower prompt is frozen, with
  its text recorded, before the first held-out call.
- **Fresh generation.** 12 questions through the current on-demand
  request and 12 through the current batch request, built by the product
  code in a unit-test environment for notes outside the historical set,
  labelled the same way, then checked once by each paid check.
- **Repeats.** Each historical and held-out question goes 3 times to the
  deployed verifier and 3 times to the narrower prompt. A check flags a
  question when at least 2 of its 3 runs flag it.
- **Budget (owner, 2026-11-16).** At most 500 paid calls or USD 5,
  whichever is reached first: 240 historical checks, 144 held-out checks,
  24 generations, 48 fresh checks, and 44 for wording the narrower prompt
  and retries. Every generation, check, repeat and retry counts; a batch
  row is one call. Cost is computed from each response's token usage at
  USD 2 per million input and USD 10 per million output tokens (list price
  as found on 2026-11-16, batch at half). Credentials come from the
  execution shell.
- **Pass mark (owner, 2026-11-16), fixed before held-out results.**
  Recommend "proceed to design" only when, on the held-out set, the
  narrower prompt flags at most 1 in 10 sound questions, catches at least
  two thirds of labelled defects, and gives the same verdict in all 3 runs
  for at least 9 in 10 questions. Catching fewer defects or flagging more
  sound questions than choices-only means "stop". Anything else, including
  fewer than 6 scored held-out defects or 15 scored sound questions, means
  "narrow and retest".
- **Report.** Defect detection separately for wrong keys and multiple
  answers, sound-question false flags, unresolved labels, agreement across
  repeats, latency, tokens and actual cost. Historical, held-out and
  freshly generated questions are reported separately, with route,
  question age and note context, and related to checking before display
  versus after a miss, including the stored route. Publish counts and
  minimal de-identified examples in the
  [dated evidence](learning-refinement-and-retrieval-evidence.md); a mixed
  or inconclusive result is a valid outcome with its remaining uncertainty
  stated.
- **Scope limits.** No product behavior, Production data change or
  stored-question policy. No population error rate from a selected pilot
  and no age-based discard policy from verifier scores. A second model, a
  second candidate prompt and any spending beyond the budget need a
  separately bounded decision.
- **Evidence and cleanup.** Keep new private packets and run outputs in
  `.planning/tmp/recall-history/integrity/` in the default checkout, beside
  the original inputs and paid responses. Apply the
  [shared closeout](#close-out-temporary-recall-evidence) when the
  follow-up is settled or stopped.

**Key examples.**

- *Wrong key.* Row 5347's key rejects the unique correct answer and the
  deployed verifier upheld it once → after the pilot, the report shows for
  each check how many of its 3 runs flagged it, counted under wrong keys.
- *Sound control.* Row 8739 is sound; the deployed verifier chose the key
  and flagged it on feasibility → it counts as a false flag for the
  deployed verifier and as upheld for choices-only; the narrower prompt
  is scored on the same question.
- *Reviewers disagree.* One reviewer finds two defensible answers and the
  other finds one → the question is listed as unresolved and enters no
  detection or false-flag count.
- *Note changed since generation.* A question's note was edited after the
  question was generated → the report marks it, and it is scored only
  when both reviewers can judge it from the content available.
- *Pass.* The held-out set holds 8 labelled defects and 16 sound
  questions; the narrower prompt catches 6, flags 1 sound question and
  gives one verdict in all 3 runs on 22 of 24 → "proceed to design".
- *Too few defects.* Labelling finds 4 held-out defects → the held-out
  figures are reported and the recommendation is at most "narrow and
  retest".
- *Budget reached.* The 500th call or USD 5 is reached before the fresh
  checks finish → sending stops, and the report names the unfinished part
  as unmeasured.

**Effort hypothesis.** M, low confidence; sample preparation and labelling
determine the bound.

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

The owner placed [the integrity follow-up](#explore-reliable-recall-question-checks)
first on 2026-11-16 because semantic checking remains inconclusive after the
initial validation. It informs when a recorded miss is trustworthy evidence.
Spelling follows because the replay shows distinct cue and scoring failures
that can be checked cheaply. The lesson tests
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

<a id="recall-question-integrity-recommendation"></a>

**Recall question integrity: initial recommendation (validated 2026-11-16).**
A wrong mark on a multiple-choice recall question
should mean the learner chose a wrong answer. The [validation results](learning-refinement-and-retrieval-evidence.md#recall-question-integrity-results)
hold the counts, commands and limits. A clean baseline holds for repeated
choices on both generating routes; the other classes still reach a learner.

**Owner disposition, 2026-11-16.** The semantic-checking result is inconclusive.
Queue [further exploration with paid experiments](#explore-reliable-recall-question-checks)
at the top of the product backlog. The initial recommendation below remains
dated evidence; this disposition selects further investigation and leaves
the product design and stored-question policy undecided. The follow-up owns
continued use of the private integrity evidence.

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
