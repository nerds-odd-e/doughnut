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

<a id="check-short-recall-lessons"></a>

### Check short lessons and count later recalls

**Identity:** SEED-073#check-short-recall-lessons
```json dough-story-dependencies
{"schemaVersion":1,"identity":"SEED-073#check-short-recall-lessons","dependencies":[{"supplier":{"identity":"SEED-073#validate-short-recall-lessons","href":"seeds/SEED-073-learning-refinement-and-retrieval.md#validate-short-recall-lessons"},"implementation":"The lessons story's closure lands the evidence file (groups, split, lessons, pages, lesson record) on trunk.","rationale":"This story's plan scores and appends to that evidence file and relies on its recorded digests; started from a trunk without it, the check would have no recorded split or lesson day to compare against.","condition":"SEED-073#validate-short-recall-lessons is wrapped up and its evidence file short-recall-lessons-evidence.md is on trunk.","state":"satisfied","resolution":{"revision":"51a743c6cb63d765188cde77f0d0254e797a43d3","path":".planning/seeds/SEED-073-learning-refinement-and-retrieval.md#validate-short-recall-lessons","summary":"The lessons story closed with slices 1-4 done (plan 105 at the before-cleanup revision) and its closure was fast-forwarded onto main at 8790b5d7e6; .planning/seeds/short-recall-lessons-evidence.md with the fit check, split, lessons, pages and lesson record is on main.\nCondition satisfied: SEED-073#validate-short-recall-lessons is wrapped up and its evidence file short-recall-lessons-evidence.md is on trunk.\nAccepted integration: 8790b5d7e66e76134102b55cc9dcf280593af79c on origin/refs/heads/main."}}]}
```
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/106-check-short-recall-lessons/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"7e0c860c732e66f5db5c4f81cbfe617bcbb54118298db41b5025a523d29cbc17","plan":"07cf75a9f7c715f1db7a6425f8cb1f6e8c013e4d1a058135d31f1315b143dc99"}}
```

- **Goal.** The owner learns whether the short contrast lessons he took on
  2026-11-18 helped him use the right word later without help, compared
  with the feedback he gets today. The result is a recommendation to
  proceed to design, narrow and retest, or stop; it changes no product
  behavior. The groups, split, lessons, pages and lesson record are in the
  [evidence](short-recall-lessons-evidence.md); the counts behind the trial
  are in the [detailed proposal](learning-refinement-validation-approaches.md#validate-short-recall-lessons).
- **Dates.** Lesson day 2026-11-18. Check window 2026-11-23 to 2026-11-27,
  aimed at 2026-11-25. Four-week count due on 2026-12-16.
- **Required behavior.**
  - **Too early.** When this story is started before 2026-11-23, tell the
    owner it is too early for the check and how many days remain, and
    leave it queued, postponed. When the check is done but it is before
    2026-12-16, tell the owner the four-week count is not due yet and
    postpone that part the same way.
  - **Check.** In the window, the owner fills the frozen check sentences
    for every pair of both halves on the existing local check page, typing
    the word without notes or tools.
  - **Record.** Per pair: half, check answers (right, the paired word,
    other), minutes spent on lessons and on the check, any lesson he
    skipped, and any check item missed or late. His spelling recalls of the
    trial's notes are counted for four weeks from the lesson day from a
    fresh read-only export.
  - **Recommendation.** After the check: the counts for both halves, the
    effort, and proceed, narrow and retest, or stop, for the owner's
    review. The four-week recall count is added when it is due and may
    revise the recommendation. Evidence closeout follows the
    [shared contract](#close-out-temporary-recall-evidence).
- **Required restrictions.**
  - Production is read only: no note, alias, overlap, grade or tracker is
    written, so the comparison half's recalls stay ordinary and the
    owner's notebook and schedule are untouched ([ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)).
  - Pages, answers and the pair list hold private learning evidence. They
    stay in the local cache and are not committed; the seed keeps counts
    and a few short examples.
- **Out of scope.** Any product screen, model call or stored lesson state;
  lessons shown at the moment of a miss; mistakes in multiple-choice
  questions about a concept (giving and receiving, 向き and 向け, RDF
  literals), which stay a deferred promise; lessons kept as notebook
  content (a contrast note, or one deciding line in both notes); other
  learners; statistical proof.
- **Key examples.**
  1. The owner opens the check on 2026-11-20 → told it is three days too
     early; the story stays queued.
  2. For 含める and 込める, in the lesson half, he types answers to two
     unseen sentences → each is recorded as right, the paired word, or
     other.
  3. A pair in the comparison half gets the same kind of unseen sentences.
  4. The owner does the check on 2026-11-30 (day 12) → the answers are
     recorded and marked late, and reported apart from on-time answers.
  5. The lesson for a pair was skipped → reported as not taken.
- **Limits to state with the result.** One learner; 14 groups met the
  fit check, 7 a half: a direction, not proof. The comparison rests on
  those 14; the six groups that passed only through another accepted word
  or a kanji both words share are checked and
  [reported apart](short-recall-lessons-evidence.md#groups-reported-apart). The owner knows which pairs
  had lessons. Lessons come days or weeks after the misses, not at the
  miss. A lesson adds one more exposure to the pair, which the comparison
  half does not get.
- **Effort hypothesis.** S. Owner: about 20 minutes of check. Elapsed: to
  2026-11-27 for the recommendation, to 2026-12-16 for the recall count.

<a id="revisit-waiting-questions-after-linked-note-edits"></a>

### Revisit a waiting question after a linked note changes

**Identity:** SEED-073#revisit-waiting-questions-after-linked-note-edits
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Goal / hypothesis.** A learner’s waiting question may depend on information from a linked note. If an edit changes the answer, the learner needs a question and choices that reflect the correction when recall opens.
- **Open question.** Which linked-note edits matter enough to renew a question when the authored focus note still matches? Correct questions that are poor practice have their own [replacement story](#replace-correct-but-poor-question).

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
