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

<a id="accept-spelling-answer-as-alias"></a>

### Accept a wrong spelling answer as an alias

**Identity:** SEED-073#accept-spelling-answer-as-alias
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/104-accept-spelling-answer-as-alias/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"75a408c01218b176dffa1931861a26ca5022a52b36865a19326e642c8bf58b60","plan":"08ca2cb726e6d8c55ae49c29d489454061bb3e9683a9912eb9c6e37195f900bb"}}
```

- **Goal.** A learner who reviews their own note and whose spelling answer
  was marked wrong, but is another name or written form of the same note,
  adds that answer to the note's aliases in one step from the answer
  result. Later spelling recalls of the note accept it through the existing
  title-or-alias match, so the learner stops losing recalls to forms the
  note should accept.
- **Why.** In the [fixed-skill retest](fixed-skill-spelling-retest-evidence.md)
  readers could not tell which written forms an author lists (つてを頼る,
  死に掛け); the author is the one who can decide, at the moment the answer
  is in front of them. The [follow-up count](fixed-skill-spelling-retest-evidence.md#follow-up-count-and-decisions-november-17-2026)
  found about 15 same-word and 5 expression-variant misses among 402 wrong
  answers in 100 days.
- **Required behavior.**
  - A wrong, nonblank spelling answer that names no other note shows an
    action to add the typed answer as an alias of the reviewed note. One
    click writes it into the note's `aliases`; no dialog or confirmation.
  - Afterwards the result shows that the answer is now an alias, and the
    action cannot add it twice. The same holds when the result is shown
    again later, for example from the last answered question.
  - The grade of the answer just given stays as recorded (owner decision
    2026-11-17); the alias counts from the next recall.
- **Required restrictions.**
  - A wrong answer gets one offer, never both (owner decision 2026-11-17).
    An answer that names another note (an accidental match, or a declared
    overlap that asks for a retry) keeps its existing flow and offers no
    alias: the name belongs to a different note.
  - The action appears only where the learner may edit the reviewed note; a
    read-only notebook shows none, as it shows no overlap action.
  - An answer that cannot be a valid alias (it contains `/`, `\`, `:`, `#`,
    `^`, `[[`, `]]` or a line break) shows no action, because the note
    would reject that alias.
- **Out of scope.** Changing or excluding the grade of the answer just
  given; offering aliases for multiple-choice answers; merging two notes
  that describe the same thing; a several-answer spelling format; a warning
  when a note fits spelling poorly; removing an alias from the answer
  result (ordinary note editing does that).
- **Key examples.**
  1. Note 伝手を頼る, no aliases. The learner types つてを頼る, which names
     no other note → marked wrong; the result offers "add つてを頼る as an
     alias". Clicking it → the note's `aliases` lists つてを頼る and the
     result shows it is added. The next spelling recall of the note,
     answered つてを頼る → correct.
  2. Note 尚更; the learner types 更に, which is the title of another note →
     the existing "names another note" result and resolve action; no alias
     action.
  3. Note colour declares an overlap with Partner, whose alias is hue; the
     learner types hue → the existing retry feedback; no alias action.
  4. A blank answer, or a correct one → no alias action.
  5. A wrong answer on a note in a notebook the learner can only read → no
     alias action.
  6. The learner types draw/a line → marked wrong; no alias action.
- **UI.** The action sits with the "Your answer `X` is incorrect." message,
  named with the typed answer, for example "Add `つてを頼る` as an alias".
  After the write, it reads as done (for example "`つてを頼る` is now an
  alias") and the note shown under the result lists the new alias.
- **Considered.** Clearing the miss by excluding the attempt from
  scheduling, reusing the existing exclusion rebuild ([ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md#recall-history-and-current-state)):
  declined; the grade stays. Leaving the learner to edit `aliases` by hand
  from the note under the result already works but takes several steps,
  which is the cost this story removes.
- **Effort hypothesis.** S, medium confidence; the frontend already appends
  an alias to note content (used by the Wikidata flows) and the answer
  result already edits the reviewed note for overlaps.

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

<a id="validate-short-recall-lessons"></a>

### Validate short lessons after recall misses

**Identity:** SEED-073#validate-short-recall-lessons
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Goal / hypothesis.** Help learners apply a distinction after a meaningful
  mistake. Test whether an optional contrastive lesson plus practice improves
  delayed, unassisted application over ordinary feedback.
- **Why now.** In 100 days of spelling recall, 227 of 402 wrong answers
  named another of the learner's notes, and 39 word pairs recurred; see
  the count in the detailed proposal.
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
