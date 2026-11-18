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

<a id="validate-source-backed-note-edits"></a>

### Validate source-backed note edits from recall

**Identity:** SEED-073#validate-source-backed-note-edits
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/108-validate-source-backed-note-edits/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"57acaaebc6bf25508d76cb592b170a7e1f44580284ac3761408ba76e15bc7836","plan":"6a2a241bbd00cf285fc083cace193020ba050495d935c2009fb92b066f0e276d"}}
```

- **Goal.** The owner, as author of his notes, learns what to add to
  today's [reviewed note correction](../../docs/recall-help.md#reviewed-note-corrections)
  so that he can decide on a correction when a recall attempt points at a
  faulty note. Three additions are tested: a quoted outside source, the
  edit shown as the smallest change, and a question made from the note
  before and after the edit. The result is a recommendation to design
  named additions, narrow and retest, or stop; it changes no product
  behavior. Findings behind this scope are in the
  [detailed proposal](learning-refinement-validation-approaches.md#validate-source-backed-note-edits).
- **Required behavior.**
  - **Cases.** Six, from the owner's own notes: the four faulty claims
    confirmed in the current notes on 2026-11-18 (存じる tagged 自動詞;
    Solid OIDC as a specialization of WebID; the prisoner's dilemma motive;
    んじゃない as used only by men); one answer that fits only one sense of
    a word, chosen from recurring mix-ups outside the lessons trial; and
    one attempt where the note is right, for which the proposal is "no
    change".
  - **Review page.** A local page shows each case: the recall attempt
    (question, answer given, mark), the passage in the current note, the
    quoted source with its link, the smallest edit as a change against the
    current text, and the question the product's generator makes from the
    note before and after the edit.
  - **Verdict.** Per case the owner records: accept as it is, accept with
    his own wording, or decline with a reason; and for each of the three
    additions whether it changed or eased his decision.
  - **Two real tries.** For two of the faults, one in a note property and
    one in the body text, the owner asks for the fix through today's note
    conversation in production. Recorded: what was proposed, whether the
    rest of the note was kept unchanged, whether he could judge it, the
    minutes spent, and his decision.
  - **Question check.** The product's generator runs locally on the note
    before and after each edit. Reviewers answer with the key hidden.
    Recorded: whether the key after the edit agrees with the source, and
    whether a valid near miss is still told apart.
  - **Recommendation.** Per addition: design it, narrow and retest, or
    stop, with the counts and the owner's reasons, for his review.
    Evidence closeout follows the [shared contract](#close-out-temporary-recall-evidence).
- **Required restrictions.**
  - Production is read only, except an edit the owner himself accepts in
    the two real tries; that edit goes through the existing acceptance and
    Git history ([ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)).
    No attempt, grade or schedule is changed ([ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)).
  - Word pairs of the [lessons trial](#check-short-recall-lessons) are
    neither shown nor edited before 2026-12-16: an extra exposure or a
    changed note would disturb that trial's four-week count.
  - Paid generator calls are sent only after the plan's cost, computed
    from real prices, is shown to the owner.
  - Attempts, note passages and the review page are private learning
    evidence. They stay in the local cache and are not committed; the seed
    keeps counts and a few short examples.
- **Out of scope.** Any product change. Deferred promises: the assistant
  finding and quoting a source itself; offering a correction without being
  asked; a note that is only a pointer to a source; help for spelling
  attempts; [renewing stored questions](#renew-waiting-question-when-note-changes)
  after an edit; other authors, and learners who cannot edit the note.
- **Key examples.**
  1. 存じる is tagged 自動詞; the dictionary entry classes it as 他動詞.
     The page shows the attempt, the tag line, the quoted entry, and an
     edit that changes the tag and adds one example with a noun object →
     the owner records a verdict and whether the quote mattered.
  2. The owner asks today's conversation to correct Solid OIDC → the
     proposal is recorded, including whether the other properties and the
     body came back unchanged.
  3. The note is right and the learner's answer was wrong → the page
     proposes no change and quotes the source that supports the note; the
     owner agrees or objects.
  4. An answer fits one sense of a word only → the edit accepts it for
     that sense; in the question made after the edit, the same answer in
     the other sense is still marked wrong.
  5. A question made from the edited prisoner's dilemma note → reviewers
     solve it with the key hidden, and the key is compared with the source.
  6. A candidate pair turns out to be in the lessons trial → it is left
     out and named in the record.
- **Limits to state with the result.** One author, who is also the
  developer and was told the four faults during refinement on 2026-11-18.
  Cases are chosen, not sampled: the result says nothing about how often a
  recall miss comes from a faulty note. Generator output varies between
  runs. Sources are found by an agent with web access, which today's
  assistant does not have.
- **Effort hypothesis.** M, medium confidence. Owner: about 40 minutes of
  review and two conversations.

<a id="source-and-smallest-change-in-corrections"></a>

### Show a quoted source and the smallest change in a note correction

**Identity:** SEED-073#source-and-smallest-change-in-corrections
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Beneficiary and outcome.** A note author deciding on a
  [reviewed note correction](../../docs/recall-help.md#reviewed-note-corrections)
  sees a quoted outside source for the claim and the edit shown as the
  change against the current note, not only the whole new content.
- **Why.** In the [validation](source-backed-note-edits-evidence.md#recommendation-2026-11-18)
  the owner's decision was changed or eased by the quoted source in 6 of 6
  cases and changed by the smallest change in 4 of 5; one correction in
  production also changed a property nobody asked about, which a
  whole-content review makes easy to miss. The owner agreed to design both.
- **Open.** Today's assistant has no web access, so who finds and quotes
  the source is undecided. A question made from the note before and after
  the edit needs narrowing before a retest: it reached the edit in 2 of 5
  cases and never in a property, and one question came in English for a
  Japanese note.
- **Deferred promises carried from the validation.** The assistant
  finding and quoting a source itself; offering a correction without
  being asked; a note that is only a pointer to a source; help for
  spelling attempts; other authors, and learners who cannot edit the
  note. Renewing stored questions after an edit is its
  [own story](#renew-waiting-question-when-note-changes).
- **Effort hypothesis.** M, low confidence.

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
