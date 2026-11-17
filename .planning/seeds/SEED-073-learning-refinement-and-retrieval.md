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

<a id="judge-contested-question-by-answer-correctness"></a>

### Judge a contested question by whether its answer is correct

**Identity:** SEED-073#judge-contested-question-by-answer-correctness
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Beneficiary and outcome.** A learner who contests a recall question gets
  a verdict on correctness alone: the contest is accepted when the stored
  answer is wrong or more than one choice is defensible from the note, and
  refused when exactly the stored answer is supported.
- **Why.** Contesting asks "is this question correct and answerable?", while
  the shared evaluation also asks "is this a good question under the
  generation rules?". In the [comparison evidence](reliable-recall-question-checks-evidence.md#cohort-results)
  that quality verdict flagged 30 of 75 sound questions; deciding from the
  chosen answers alone flagged none and caught 12 of 14 defects.
- **Scope.** The contest decision and its advice to the learner, asked
  through one narrow instruction: name every choice the note supports. The
  story deletes the broader question-quality evaluation and the checking and
  replacing of a question while it is being generated, with their setting,
  tests and documentation, then sweeps the product for what only they used.
  Replacing a question after the learner's accepted contest stays.
- **Known context.** Production already sends the contest check the focus
  note and its retrieved related notes within the shared content budget; the
  experiment's deployed baseline used the focus note only. The narrower
  frozen instruction caught 14 of 14 defects and flagged no sound question.
- **Effort hypothesis.** S to M, medium confidence.

<a id="validate-spelling-objectives"></a>

### Validate spelling objectives and cues

**Identity:** SEED-073#validate-spelling-objectives
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/101-validate-spelling-objectives/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"34dfe13ee64abc35b42078bf3b028b9cbce69f8e93fd8c4672aa4acce99d82d2","plan":"3ac2d61ee64fe755f0f06abffc281fa1c8beb5d376857b7d4fadeeff2348b224"}}
```

- **Goal.** Give the product owner evidence for whether one stated objective
  and one intact cue can make written-form, reading, and vocabulary recall
  interpretable and fairly scored while preserving intended distinctions.
  The beneficiary is the learner; this story delivers a validation decision,
  not a change to live recall.
- **Scope.** Compare recoverable historical cues and proposed rewrites with
  keys hidden, then replay legitimate variants, genuine form errors, and close
  semantic neighbors against an explicit answer rule for each objective.
  Cover the five already-reviewed case families: brine, 伝手, おっしゃる,
  the ～かける inflection, and meadow. Report cue clarity, scoring changes,
  remaining alternatives, and whether each rewrite preserves the skill.
  Distinguish observed judgments from hypotheses about learner benefit.
- **Alternatives and recommendation.** Compare the existing manual workflow
  (edit the note and review title/aliases), an explicit objective with one
  intact cue and its answer rule, and broader semantic acceptance under the
  existing cue. Recommend the paired objective-and-cue review: manual edits
  remain the smaller baseline, while broader acceptance alone cannot resolve
  what a mixed cue asks. This selects an experiment, not a production grader.
- **Borrowed mechanism.** Use calibration's positive and negative controls:
  the declared objective is the quantity being assessed, the cue elicits the
  response, valid variants check credit, and real form or meaning errors
  check discrimination. Hold the objective fixed when attributing an
  improvement to wording or scoring. Unlike a calibrated physical instrument,
  language can have several defensible answers; reviewer agreement does not
  establish a unique key or a learning gain.
- **Challenge.** A clearer rewrite can succeed by testing a different skill
  or supplying the answer-bearing form. The earlier 伝手 rewrite asks for
  a reading instead of whole-expression production; the ～かける rewrite
  supplies the construction. Treat these as changed or guided tasks, not
  evidence that the original task was graded fairly. A broad meadow cue
  remains a control against assuming one short definition has one answer.
- **Key examples.**
  - Given a cue mixing a solution, process, and inflected forms, asking for
    the culinary solution → review *brine* and *curing* → distinguish the
    material from the process and record the narrowed task; do not infer
    that the historical response satisfied every original blank.
  - Given a whole-expression vocabulary goal → review つてをたよる and
    伝手に頼る → judge script and particle separately against that goal.
    Asking instead for the reading of 伝手 → つて is a different task.
  - Given an explicitly hiragana, dictionary-form objective for the special
    honorific of 言う → compare おっしゃる and おしゃる → retain the small
    っ distinction. Any kanji answer is judged under its stated script goal.
  - Given one complete inflected blank using ～かける → compare the complete
    answer, an allowed script variant, and a different construction → preserve
    the requested formation, recording that the construction was supplied.
  - Given a broad field-and-wildflowers cue → review meadow, grassland, and
    pasture → expose residual sense overlap; a guided use comparison does
    not establish unaided vocabulary retrieval.
- **Deferred promises.** Product UX/UI, learner or author workflows, automated
  generation/grading, new objective storage, live history or schedule changes,
  paid experiments, and measured comprehension or retention gains. Unsettled
  linguistic alternatives are findings for review, not automatic rejections.
  The validation recommends proceed to design, narrow and retest, or stop;
  the product owner owns adoption and any follow-up scope.
- **Evidence.** On 2026-11-17 at af84555ebf, the local November 13 export's
  digest matched the November 14 replay manifest. All ten selected attempts
  have recoverable cues; all five primary original prompts match the manifest
  exactly. The retained key-hidden review supports answerability on selected
  examples, not deployed error rates, causal improvement, or learner benefit.
- **Effort hypothesis.** S, medium confidence; reuse the existing paired
  review and obtain only missing judgments within these case families.
- **Evidence and cleanup.** Follow the [shared contract](#close-out-temporary-recall-evidence).
  Retain minimal de-identified conclusions; the other queued validations
  still own use of the shared private cache.

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

<a id="renew-waiting-question-when-note-changes"></a>

### Renew a waiting question when its note changes

**Identity:** SEED-073#renew-waiting-question-when-note-changes
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Beneficiary and outcome.** A learner who has improved a note is asked
  about the note as it now reads: a stored question still waiting to be
  shown, generated before its note changed, is replaced by one made from
  the current note.
- **Why.** Refining notes through recall makes stored questions stale. In
  the [reviewed samples](reliable-recall-question-checks-evidence.md#route-age-and-context)
  18 of 47 and 22 of 24 focus notes had changed after their question was
  generated, and [8,254 stored questions were waiting](learning-refinement-integrity-initial-recommendation.md#recall-question-integrity-recommendation).
- **Open.** Which note changes make a question stale is untested; the
  evidence supports no rule based on a question's age alone.
- **Effort hypothesis.** S to M, low confidence.

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
