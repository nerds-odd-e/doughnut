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

<a id="replace-correct-but-poor-question"></a>

### Replace a correct but poor recall question

**Identity:** SEED-073#replace-correct-but-poor-question
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Goal / hypothesis.** A learner whose recall question is correct but poor
  (easy to guess, or testing a linked note rather than the focus note) can
  get a better one. A contest is judged on correctness alone, so such a
  contest is refused today and the learner must answer.
- **Open questions.** How many correct questions a learner would want
  replaced is unknown: the [comparison's](reliable-recall-question-checks-evidence.md#comparison-2026-11-17)
  reviewers judged correctness only, and the earlier quality verdict flagged
  30 of 75 sound questions. Whether to check a question before it is first
  shown, or to check stored questions that are waiting, is also undecided.
- **Effort hypothesis.** Unknown until the beneficiary's need is validated.

<a id="retest-fixed-skill-spelling-objectives"></a>

### Retest spelling objectives with one skill held fixed

**Identity:** SEED-073#retest-fixed-skill-spelling-objectives
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/103-retest-fixed-skill-spelling-objectives/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"74e4f65c768b73a12e3214375cae0170fdf82c04e02ebbd54dc37aba41fe8584","plan":"e1ac5bbc94895693bad997ecdde0c6fcd778c36f13f6e72758b45fec2de1892e"}}
```

- **Goal.** Give the product owner evidence to recommend proceeding to design,
  narrowing and retesting, or stopping objective-and-cue review: can a clearer
  cue and explicit answer rule make recall fair while preserving the skill?
  The owner approved this narrowed retest on 2026-11-17 from the
  [prior recommendation](spelling-objectives-validation-evidence.md#comparison-and-decision).

**Scope**

- Reuse the five archived families and response matrices. Before rewriting,
  declare one intended skill per item, supplied information and remaining
  retrieval, script permissions and blank boundaries. Record any departure
  from the archived task; a changed skill is a result, not a successful repair.
- Compare paired objective/cue/rule review with manual note/alias review and
  broader acceptance under the original cue. Use two independent key-hidden
  readers to answer, identify the remaining retrieval and judge response
  variants; retain disagreement and genuine errors alongside agreement.
- Resolve exact 伝手に頼る usage with attributable evidence or retain it as
  unresolved. Keep meadow as an ambiguity control. Deliver a per-item skill
  preservation judgment and bounded recommendation. Live recall changes,
  product design and learner comprehension or retention trials are deferred.

**Key examples**

| Given the held skill | When a cue or rule is reviewed | Required result |
| --- | --- | --- |
| Brine word-family retrieval in the archived slots | A rewrite asks only for the material noun | Record the lost process/inflection demand; keep real spelling errors distinct. |
| Natural whole-expression production for reliance on connections | A rewrite supplies 伝手 and asks for its reading | Record the skill change; judge script variants separately from unresolved usage. |
| Honorific lexical written-form recall | A response varies script or omits small っ | Apply declared script permissions without crediting a genuine form error. |
| Retrieval and formation of the near-death construction | A rewrite supplies ～かける or changes the blank ending | Identify supplied retrieval and changed completion demands rather than count agreement as preservation. |
| Vocabulary retrieval from the meadow description | Readers find grassland or pasture defensible | Preserve the overlap as ambiguity, not an error inferred from a hidden name. |

- **Effort hypothesis.** S, medium confidence; a bounded offline retest.
- **Evidence and cleanup.** Use the [local evidence](#local-recall-evidence) and [shared contract](#close-out-temporary-recall-evidence); learner benefit remains a hypothesis.

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
[Shared temporary evidence closeout](learning-refinement-and-retrieval-context.md#close-out-temporary-recall-evidence) applies to each validation.

<a id="open-decisions"></a>
<a id="when-to-surface"></a>
<a id="breadcrumbs"></a>

[Open decisions, surfacing conditions and provenance](learning-refinement-and-retrieval-context.md#open-decisions) remain shared context.

<a id="recall-question-integrity-recommendation"></a>

Read the [dated initial integrity recommendation](learning-refinement-integrity-initial-recommendation.md#recall-question-integrity-recommendation).
