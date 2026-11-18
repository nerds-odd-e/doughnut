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

<a id="source-and-smallest-change-in-corrections"></a>

### Show the smallest change in a note correction

**Identity:** SEED-073#source-and-smallest-change-in-corrections
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/109-show-smallest-change-in-note-correction/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"8efecabf7419b912ae71e8fdf1a63dc8effca4e9b323c5000c15a368ea220df1","plan":"c60a032b827630e98c910da396ee5d5cc4ea1d1732a336cd950f49f85834ffe6"}}
```

- **Goal.** A note author deciding on a
  [reviewed note correction](../../docs/recall-help.md#reviewed-note-corrections)
  to a note's content sees what the correction would change in the current
  note, without comparing two full versions. The author can then accept
  with confidence and notices a change nobody asked for. This serves the
  wider aim of turning recall misses into reviewed note corrections.
- **Why.** In the [validation](source-backed-note-edits-evidence.md#recommendation-2026-11-18)
  the smallest change altered the owner's decision in 4 of 5 cases. One
  correction in production also changed a property nobody asked about,
  which a whole-content review makes easy to miss.
- **Owner decision (2026-11-18).** The story was split. This story shows
  the change. The quoted source is
  [its own story](#assistant-quoted-source-in-corrections).
- **Required behavior.**
  - For a pending content correction that the author may accept, the
    review shows the change between the current note content and the
    wording in the editor: removed text and added text are marked, and
    unchanged text is not.
  - Note properties are part of the content, so a changed, added or
    removed property is marked like any other change.
  - Inside a changed line, only the changed characters are marked, not
    the whole line. This also holds for text without spaces, such as
    Japanese.
  - The change follows the editor. When the author edits the wording, the
    marks update. After **Load current content**, the change is shown
    against the note as just loaded.
  - When the wording equals the current content, the review says there is
    no change.
  - The full current content, the original proposal and the editor stay
    available.
- **Out of scope.**
  - Title corrections. A title is one line, and the current and proposed
    titles are already shown together.
  - Corrections already accepted or declined, and the view of a reader who
    cannot accept. They keep today's display.
  - Accepting or declining each change separately. The author removes an
    unwanted change in the editor.
  - Making the assistant propose smaller edits. The story makes extra
    changes visible; it does not prevent them.
  - A question made from the note before and after the edit. The
    validation said to narrow and retest it first.
  - Carried from the validation: offering a correction without being
    asked; a note that is only a pointer to a source; help for spelling
    attempts; other authors, and learners who cannot edit the note.
    Renewing stored questions after an edit is its
    [own story](#renew-waiting-question-when-note-changes).
- **Key examples.**
  1. Solid OIDC, as proposed on 2026-11-18: the property `a specialization
     of: WebID` becomes `uses: WebID`, `an instance of: OpenID Connect`
     becomes `builds on: OpenID Connect`, and one body sentence is
     reworded → the author sees those two property lines and the reworded
     words marked. `type`, `url` and `a part of` are not marked.
  2. Prisoner's dilemma: the body is one long paragraph on one line, and
     the correction replaces one sentence and adds another → only those
     words are marked. The rest of the paragraph is not.
  3. In example 1 the author did not ask for the `an instance of` change
     and types the old line back into the editor → that mark disappears,
     and Accept publishes the wording without that change.
  4. A Japanese body sentence gains 「最後に「か」がつくと」 in the middle of a
     line → the added characters are marked, not the whole line.
  5. Another edit changes the note while the correction is pending. Accept
     is refused as stale and the author chooses **Load current content** →
     the change is shown against the note as it now reads.
  6. The author edits the wording until it equals the current note → the
     review says there is no change.
- **Interaction.** For a pending content correction the change is what the
  author reads first. The editor, the full current content and the
  original proposal are still there, but the author no longer has to
  compare them by eye.
- **Found during refinement (2026-11-18).**
  - The product already compares two texts line by line, side by side, in
    the refine-note preview and the undo confirmation. Reusing that view
    unchanged is not enough: in the two corrections accepted in production
    (revisions 5ed4e72e and fa5efa4d, read from the notebooks' Git history)
    the body change sat inside one paragraph that is a single line, so a
    line comparison marks the whole paragraph. Property changes sat on
    their own lines.
  - The review already loads the current content and holds the proposal,
    so the change can be worked out in the browser. Nothing new is stored
    and acceptance stays as it is
    ([ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)).
    No Accepted decision is in conflict.
  - Strongest doubt: in the validation an agent wrote the smallest edit
    itself. In the product the assistant may rewrite more than asked, as
    in example 1. Showing the change does not make the edit small; it
    lets the author see and undo the extra. The purpose, an easier and
    safer decision, still holds.
- **Effort hypothesis.** S, medium confidence.

<a id="assistant-quoted-source-in-corrections"></a>

### Quote a source the assistant found in a note correction

**Identity:** SEED-073#assistant-quoted-source-in-corrections
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/110-quote-assistant-found-source-in-note-correction/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"b2baff834499875ab3014a547223b3140f839b6aba1fad2d43d5bdde082e1b17","plan":"48411b248229baccdeb5e5004254a5d7c838a844028408065c73d4d4d4334570"}}
```

- **Goal.** A note author deciding on a
  [reviewed note correction](../../docs/recall-help.md#reviewed-note-corrections)
  to a note's content sees a passage quoted word for word from an outside
  source for the corrected claim, with a link that opens the source. The
  assistant searches the web for it when it proposes the correction. The
  author can then accept or decline with evidence that the author did not
  have to find. This serves the wider aim of turning recall misses into
  reviewed note corrections.
- **Why.** In the [validation](source-backed-note-edits-evidence.md#recommendation-2026-11-18)
  the quoted source changed or eased the owner's decision in 6 of 6 cases.
  There an agent with web access found the sources. A source the author
  supplies tells the author nothing new, so the owner chose on 2026-11-18
  to have the assistant find it, as a story after
  [showing the smallest change](#source-and-smallest-change-in-corrections).
- **Owner decisions (2026-11-18).** The quote and link are their own parts
  of the correction, and the link opens the page at the quoted words. A
  correction without a source is still offered and says that no source
  was found. The owner gave the go for the
  [paid try](source-backed-note-edits-evidence.md#assistant-search-try-2026-11-18).
- **Required behavior.**
  - When the assistant proposes a content correction it first searches
    the web for a source for the corrected claim. The correction carries
    one passage copied from the source and the address of the page.
  - The review shows the quote and a link to the source with the
    correction, apart from the reason. They stay with the correction
    after it is accepted or declined, and for a reader who cannot accept.
  - The link opens the source at the quoted passage and the browser marks
    it, where the browser and the page allow. For a PDF, or a page where
    the words cannot be found, the link opens the source at its start.
  - When the assistant found no source, the correction is offered as
    today and the review says that no source was found.
- **Not promised now.**
  - Title corrections.
  - A check by the server that the quote is on the page. In the try no
    quote was invented, and a strict check would have wrongly failed 2 of
    10. It becomes worth a story if invented quotes are seen in use.
  - More than one source for a correction, and a note on how strong the
    source is.
  - Handling the note's own `url` apart. The assistant reads the note's
    properties and opened that address first in the try.
  - Keeping the assistant from searching when it answers without a
    correction. In the try's no-change case it searched and proposed
    nothing, at $0.04 to $0.05 a reply.
- **Out of scope.** Storing the source in the note. The source helps the
  decision and stays with the correction in the conversation.
- **Key examples.**
  1. Solid OIDC says `a specialization of: WebID` and the author asks for
     a correction → the review shows the quote "WebIDs underpin Solid and
     are used as a primary identifier for Users in this specification."
     with a link to `https://solid.github.io/solid-oidc/`, apart from the
     reason.
  2. The author opens that link → the specification opens at the quoted
     sentence, which is marked.
  3. 存じる is tagged 自動詞 and the author asks for a correction → the
     review shows the quote 「〘 他動詞 ザ上一 〙」 with a link to the kotobank
     entry.
  4. The assistant proposes a correction and finds no source → the
     correction is offered as today, and the review says no source was
     found.
  5. The source is a PDF (prisoner's dilemma, a book chapter) → the quote
     and link are shown; the link opens the PDF at its start.
  6. The author accepts a correction with a source and reopens the
     conversation later → the quote and link are still shown with the
     accepted correction.
- **Interaction.** The quote and its link sit with the reason, before the
  change and the editor, so the author reads the evidence first. Without
  a source the same place says that no source was found.
- **Found during refinement (2026-11-18).**
  - Production uses `gpt-6.2-sol` for note conversations (read from
    `/api/settings/current-model-version`). The
    [paid try](source-backed-note-edits-evidence.md#assistant-search-try-2026-11-18)
    sent 12 requests of the product's kind with search added. All
    completed as streams, with search and the correction tools together.
    Every one of the 10 corrections carried a quote and an address. A
    correction with search cost $0.02 to $0.09.
  - A correction stores a reason, which the review shows as formatted
    text. It has no place of its own for a quote or a link, so the story
    adds one. Nothing changes in how a correction is accepted
    ([ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)).
  - A link ending in `#:~:text=` and the quoted words makes the browser
    scroll to those words and mark them
    ([MDN](https://developer.mozilla.org/en-US/docs/Web/URI/Reference/Fragment/Text_fragments)).
    Tried in Chrome on 2026-11-18 by clicking such links: the page opened
    at the quote for the Solid-OIDC sentence, for 「〘 他動詞 ザ上一 〙」 on
    kotobank, and for a long Japanese sentence.
  - Ways compared: the quote written into the reason (the product cannot
    tell whether a source exists); own parts of the correction; own parts
    with a link to the passage (chosen); own parts with a server check;
    a button the author presses to ask for a source (against the
    2026-11-18 decision). The link to the passage is borrowed from the
    page-and-paragraph citation in legal writing, which sends the reader
    to the exact place. It breaks where a page changes, is a PDF, or is
    built by script: the link then opens at the start with nothing
    marked. 4 of the 10 sources in the try were PDFs.
  - Strongest doubt: a reworded quote, or one that does not support the
    claim, makes a wrong correction look checked. The try found no
    invented quote. One quote joined two passages, so the request asks
    for one passage. Two quotes were example sentences that support the
    claim only indirectly. The link shows that the words are there; that
    they support the claim stays the author's judgement.
- **Effort hypothesis.** M, medium confidence.

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
