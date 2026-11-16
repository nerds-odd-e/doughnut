---
id: SEED-073
status: dormant
planted: 2026-10-12
planted_during: owner-directed backlog shaping after the OpenKB implementation assessment
trigger_when: improving note quality and retrieval quality through everyday learning and use
scope: large
---

# SEED-073: Improve knowledge through learning, refinement, and retrieval

<a id="epic-outcome"></a>
<a id="why-this-matters"></a>
<a id="alternatives-and-decision"></a>
<a id="shared-constraints"></a>
<a id="recall-proposal-validation"></a>

Read the [outcome, rationale, alternatives, constraints and validation context](learning-refinement-and-retrieval-context.md).

<a id="local-recall-evidence"></a>

[Local recall evidence and validation workflow](learning-refinement-and-retrieval-context.md#local-recall-evidence) apply to each story below.

<a id="explore-reliable-recall-question-checks"></a>

### Explore reliable recall question checks with paid experiments

**Identity:** SEED-073#explore-reliable-recall-question-checks
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/099-explore-reliable-recall-question-checks/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"e176a8d9df4ea7879d87818eaeb1992aea2840f0f6f913391f43c36fe11b0b5d","plan":"61f4d7b7dc12cbcaee65a64c0f2e3cb4de515413595fff3b27f3fac79230b380"}}
```

**Goal.** A learner needs a wrong mark to reflect a wrong answer, rather
than a defective key or another defensible choice. Give the owner a
reproducible comparison of three checks and a recommendation to proceed to
design, narrow and retest, or stop, naming supported failure classes and
routes. Trustworthy wrong marks in the product belong to a later design story.

[Starting evidence and hypothesis](learning-refinement-and-retrieval-context.md#reliable-check-exploration-premise) inform this comparison.

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
- **Historical set.** 41 scoreable questions from the 47 already
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
  whichever is reached first: 246 historical checks, 144 held-out checks,
  24 generations, 48 fresh checks, and 38 for wording the narrower prompt
  and retries (18 wording; 20 retries, fallback and probes). Every paid call counts;
  a batch row is one call. Cost is computed from each response's token usage at
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
- **Report.** Separate wrong-key and multiple-answer detection, sound false
  flags, unresolved labels, repeat agreement, latency, tokens and actual
  cost. Separate historical, held-out and fresh results, with route, question
  age and note context. Relate them to checking before display versus after
  a miss, including the stored route. Publish counts and minimal de-identified
  examples in the [dated evidence](learning-refinement-and-retrieval-evidence.md).
  State remaining uncertainty; mixed or inconclusive results are valid.
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

- *Note at fault.* Row 5347 is note-at-fault and unscored after both fresh
  reviewers agree. Its existing paid probe is diagnostic and counts against
  the 20 retry, fallback and probe calls; it is outside the scored comparison.
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

**Owner amendment, 2026-11-16.** The owner approved the fresh labels and
six calls moved from the reserve. Earlier “240 historical checks” and
“Row 5347's key rejects the unique correct answer” remain dated provenance;
the current allocation and note-at-fault example above govern this run.

**Effort hypothesis.** M, low confidence; sample preparation and labelling
determine the bound.

<a id="validate-spelling-objectives"></a>

### Validate spelling objectives and cues

**Identity:** SEED-073#validate-spelling-objectives
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Goal / hypothesis.** Make the tested written-form, reading or vocabulary
  skill clear and credit answers that satisfy it. Test one stated objective
  and intact cue while preserving spelling and usage distinctions.
- **Validation and decision.** See the [detailed proposal](learning-refinement-validation-approaches.md#validate-spelling-objectives).
- **Effort hypothesis.** S, medium confidence; a small paired review using existing examples.
- **Evidence and cleanup.** Follow the [shared contract](#close-out-temporary-recall-evidence).

<a id="validate-short-recall-lessons"></a>

### Validate short lessons after recall misses

**Identity:** SEED-073#validate-short-recall-lessons
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Goal / hypothesis.** Help learners apply a distinction after a meaningful
  mistake. Test whether an optional contrastive lesson plus practice improves
  delayed, unassisted application over ordinary feedback.
- **Validation and decision.** See the [detailed proposal](learning-refinement-validation-approaches.md#validate-short-recall-lessons).
- **Effort hypothesis.** L, low confidence; preparation can be small, but learner
  participation and an elapsed follow-up are needed.
- **Evidence and cleanup.** Follow the [shared contract](#close-out-temporary-recall-evidence).

<a id="validate-source-backed-note-edits"></a>

### Validate source-backed note edits from recall

**Identity:** SEED-073#validate-source-backed-note-edits
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Goal / hypothesis.** Give authors an evaluable correction when a note
  misleads. Test whether source, attempt and smallest proposed edit help them
  preserve useful distinctions and improve the note and its questions.
- **Validation and decision.** See the [detailed proposal](learning-refinement-validation-approaches.md#validate-source-backed-note-edits).
- **Effort hypothesis.** M, medium confidence; a few authored examples and accessible sources.
- **Evidence and cleanup.** Follow the [shared contract](#close-out-temporary-recall-evidence).

<a id="validate-within-note-objectives"></a>

### Validate follow-up by learning objective

**Identity:** SEED-073#validate-within-note-objectives
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Goal / hypothesis.** Revisit the missed part of a broad note while keeping
  context and related progress. Test whether objective-specific evidence
  gives better follow-up choices than the note’s latest grade alone.
- **Validation and decision.** See the [detailed proposal](learning-refinement-validation-approaches.md#validate-within-note-objectives).
- **Effort hypothesis.** S, medium confidence; a small annotated replay using existing examples.
- **Evidence and cleanup.** Follow the [shared contract](#close-out-temporary-recall-evidence).

<a id="validate-question-generation-purpose"></a>

### Validate when question generation should abstain

**Identity:** SEED-073#validate-question-generation-purpose
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Goal / hypothesis.** Use recall time for a chosen fact, distinction or
  skill, including deliberate catalog practice. Test whether checking learning
  purpose reduces incidental metadata questions while retaining worthwhile
  facts and substantive relationships.
- **Validation and decision.** See the [detailed proposal](learning-refinement-validation-approaches.md#validate-question-generation-purpose).
- **Effort hypothesis.** S, medium confidence; an offline set with and without explicit catalog goals.
- **Evidence and cleanup.** Follow the [shared contract](#close-out-temporary-recall-evidence).

<a id="ordering-and-scope-reduction"></a>
[Owner ordering and scope reduction](learning-refinement-and-retrieval-context.md#ordering-and-scope-reduction) applies across these stories.

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

<a id="open-decisions"></a>
<a id="when-to-surface"></a>
<a id="breadcrumbs"></a>

[Open decisions, surfacing conditions and provenance](learning-refinement-and-retrieval-context.md#open-decisions) remain shared context.

<a id="recall-question-integrity-recommendation"></a>

Read the [dated initial integrity recommendation](learning-refinement-integrity-initial-recommendation.md#recall-question-integrity-recommendation).
