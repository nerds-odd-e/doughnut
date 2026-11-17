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

<a id="validate-short-recall-lessons"></a>

### Validate short lessons after recall misses

**Identity:** SEED-073#validate-short-recall-lessons
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/105-validate-short-recall-lessons/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"3409590cd01dd54943ccace868fdd875d4571f8c2b89ff59554f85ce036fa502","plan":"4d262221eaa5bc9d283c10dbfa2111d28423613be0d826b97b081e455fea6e13"}}
```

- **Goal.** The owner, the only learner with recall evidence, takes short
  contrast lessons for half of the word pairs he keeps confusing in
  spelling recall, with an unseen check for both halves fixed beforehand,
  so that [checking the lessons](#check-short-recall-lessons) can compare
  the halves. It changes no product behavior.
- **Why.** In 100 days of spelling recall, 227 of 402 wrong answers named
  another of the learner's notes, and 39 word pairs recurred. After a
  mix-up the next recall of the note is already correct 157 of 210 times,
  so a comparison on every new miss would take about 13 weeks; the repeated
  pairs are where today's feedback has not been enough. Counts and
  arithmetic are in the [detailed proposal](learning-refinement-validation-approaches.md#validate-short-recall-lessons).
- **Required behavior.**
  - **Pairs.** Start from the pairs confused two or more times in the
    November 13 export. Words that were confused with each other, in
    either direction or through a shared word (いかにも with さも and
    甚だしい), form one group. A group enters the trial only when, for each
    of its pairs, sentences exist in which exactly one of the two words
    fits, confirmed by two independent readers who do not see the intended
    word. The other groups are counted with the reason (both words fit the
    note's cue, or an overlap is already declared) and take no further part.
  - **Assignment.** Qualifying groups are split at random into a lesson
    half and a comparison half, by a recorded seed, before any lesson is
    written.
  - **Lesson.** For each pair in the lesson half, one read-once local page:
    the word the owner typed, the intended word, the one feature that
    decides between them, two short sentences that differ in that feature,
    and one practice sentence he answers, followed by the answer and its
    reason. Each stated feature is checked against a dictionary or usage
    source. The comparison half gets nothing new; both notes stay available
    to him as they are today.
  - **Check page.** New sentences for every pair of both halves, not used
    in any lesson, fixed before the lessons were shown, naming neither
    word; ready for the owner on the check day.
  - **Record.** The lesson day, minutes spent on lessons, and any lesson he
    skipped, in the [evidence](short-recall-lessons-evidence.md).
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
  learners; statistical proof. The check, its scoring, the recommendation
  and the four-week count belong to [checking the lessons](#check-short-recall-lessons).
- **Key examples.**
  1. 含める answered 込める four times, and 込める is a note. Readers given
     two new sentences each supply only one of the two words → the pair
     qualifies. Drawn into the lesson half → the owner reads its page and
     answers the practice sentence.
  2. meadow answered pasture three times, but the note's cue says "used
     for hay production or grazing", and readers accept pasture for the
     sentences tried → the pair is counted as "both words fit" and gets no
     lesson and no check.
  3. 整う and 調う were confused in both directions → one group; both
     directions fall in the same half.
  4. A pair drawn into the comparison half → no page.
  5. The owner skips a lesson page → the pair stays in the lesson half and
     is recorded as not taken.
- **UI.** Local pages only. A lesson page shows one pair at a time and
  asks for the practice answer before showing it. The check page shows one
  sentence at a time with a meaning hint and a blank, gives no feedback
  until all are answered, and saves the answers to a local file.
- **Considered.** A lesson or no lesson on every new mix-up, checked at
  the next recall: about 100 misses a half, 13 weeks, and the check reuses
  the same cue. Reading the answer from history: the notebook holds 17
  hand-written contrast notes, two match a confused pair, and their
  creation dates are not recoverable, so history cannot answer it. Closing
  on the counts without a trial: declined by the owner on 2026-11-17. The
  lesson's single deciding feature borrows the "similar species" entry of
  a field guide, which separates two look-alikes by one mark; it breaks
  where two words differ in several ways at once, and such pairs are
  reported. On 2026-11-18, after the lessons were taken, the owner split
  the check and the four-week count into [their own story](#check-short-recall-lessons)
  so this one could close while they wait for their dates.
- **Effort hypothesis.** S, delivered: about 35 minutes of owner time
  planned, 9 taken.

<a id="check-short-recall-lessons"></a>

### Check short lessons and count later recalls

**Identity:** SEED-073#check-short-recall-lessons
```json dough-story-dependencies
{"schemaVersion":1,"identity":"SEED-073#check-short-recall-lessons","dependencies":[{"supplier":{"identity":"SEED-073#validate-short-recall-lessons","href":"seeds/SEED-073-learning-refinement-and-retrieval.md#validate-short-recall-lessons"},"implementation":"The lessons story's closure lands the evidence file (groups, split, lessons, pages, lesson record) on trunk.","rationale":"This story's plan scores and appends to that evidence file and relies on its recorded digests; started from a trunk without it, the check would have no recorded split or lesson day to compare against.","condition":"SEED-073#validate-short-recall-lessons is wrapped up and its evidence file short-recall-lessons-evidence.md is on trunk.","state":"waiting"}]}
```
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/106-check-short-recall-lessons/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"810ebe5b31dc4c33904a83ca471ac87de24f87e7f6f96263638df42d26a4c222","plan":"95ed9550fb94f3af728c2429513e632b09b9142279b102a49cbd83098928bb31"}}
```

- **Goal.** The owner learns whether the short contrast lessons he took on
  2026-11-18 helped him use the right word later without help, compared
  with the feedback he gets today. The result is a recommendation to
  proceed to design, narrow and retest, or stop; it changes no product
  behavior. Split on 2026-11-18 from
  [the lessons](#validate-short-recall-lessons) (SEED-073#validate-short-recall-lessons),
  whose groups, split, lessons, pages and lesson record are in the
  [evidence](short-recall-lessons-evidence.md).
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
- **Required restrictions.** As in [the lessons](#validate-short-recall-lessons):
  production is read only, and private learning evidence stays in the
  local cache.
- **Out of scope.** As in [the lessons](#validate-short-recall-lessons).
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
- **Limits to state with the result.** One learner; 20 groups passed the
  fit check, 10 a half: a direction, not proof. The owner knows which pairs
  had lessons. Lessons come days or weeks after the misses, not at the
  miss. A lesson adds one more exposure to the pair, which the comparison
  half does not get.
- **Effort hypothesis.** S. Owner: about 20 minutes of check. Elapsed: to
  2026-11-27 for the recommendation, to 2026-12-16 for the recall count.

<a id="correct-short-lessons-fit-check"></a>

### Correct the short lessons' fit-check record

**Identity:** SEED-073#correct-short-lessons-fit-check
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/107-correct-short-lessons-fit-check/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"6e30f6a927011dfc8b509c1bb7b9acd031b312339fd55800173048b744657ecd","plan":"aa9461c257d32e5a46e84a591c667893b5b591fea0cc45a77e2e61ae22540dbf"}}
```

- **Goal.** Before the owner takes the day-seven check, his recommendation
  rests on an accurate record of which lesson groups the fit check really
  qualified: the evidence and [the check's](#check-short-recall-lessons)
  plan name the groups that passed only through accepted near synonyms or
  a kanji shared by both words, and report them apart.
- **Scope.** Records only: the fit-check and split sections of the
  [evidence](short-recall-lessons-evidence.md) and the check story's limits
  and plan. The frozen check items, the split and the pages stay as they are.
  Plan: [107](../slice-plans/107-correct-short-lessons-fit-check/PLAN.md).

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
