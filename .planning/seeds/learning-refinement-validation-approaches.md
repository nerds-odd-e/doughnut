# Recall validation approaches

Active proposal wording and validation approaches remain with their linked
[canonical story homes](SEED-073-learning-refinement-and-retrieval.md).
Completed study sections link their retained evidence.

<a id="validate-spelling-objectives"></a>

## Validate spelling objectives and cues

The [validation evidence](spelling-objectives-validation-evidence.md) recommends
narrowing and retesting the paired objective-and-cue proposal while retaining
manual note and alias review as the smaller baseline. Its five-family comparison
records the observed answerability, changed skills and remaining uncertainty.
The [fixed-skill retest evidence](fixed-skill-spelling-retest-evidence.md)
recommends proceeding to design the paired objective, cue and rule, with its
open points listed there.

<a id="validate-short-recall-lessons"></a>

## Validate short lessons after recall misses

[Story home](SEED-073-learning-refinement-and-retrieval.md#check-short-recall-lessons).

- **For / why.** A learner who makes a meaningful mistake needs a way to
  understand the distinction and use it in another situation.
- **Proposal hypothesis.** An optional lesson that contrasts the learner's
  answer with the intended idea leads to better delayed, unassisted
  application than ordinary feedback alone. Goal, scope and key examples
  of the trial are in the story home.
- **Spelling mix-up count (2026-11-17).** From the November 13 private
  export (August 5–November 13; 2,602 spelling answers on 784 notes):
  - 402 answers were wrong. In 227 (56%) the typed answer was another of
    the learner's notes; 57 were blank; 118 were other wrong answers.
  - The 227 mix-ups cover 139 notes and 173 distinct pairs of expected and
    typed word. 39 pairs recurred, taking 93 of the 227 answers. Nine pairs
    were confused in both directions (下がる and 下る, 整う and 調う,
    見つける and 見かける, 顧みる and 振り返る, 不備 and 欠如, 軽率 and
    浅はか, にこやか and 和やか, 不快 and 気まずい, 拠点 and 本拠).
  - Most repeated: 尚更 answered 更に (5), 含める answered 込める (4),
    過ぎる answered 経つ (4). いかにも drew nine mix-ups across several
    words (さも, 甚だしい, まるで).
  - 209 of the 227 are in the Japanese notebook, 12 in English (meadow
    answered pasture, 3).
  - 32 further answers named a note already recorded as overlapping, so
    the existing overlap record covers a small part of the pairs.
  - Reading: recurring near-word pairs are the largest group of spelling
    misses and give this validation ready items that isolate one
    distinction, with a natural delayed check at the pair's next recall.
  - Limits: a match with another note shows the words were confused, not
    why; some pairs may be acceptable synonyms for the cue. Counts are
    from one learner. Multiple-choice misses were not counted.
- **After a mix-up today (2026-11-17, same export).**
  - The answer result names the other note and offers a wiki link, a
    relationship or an overlap; it shows no contrast between the two words.
    The reviewed note is graded as failed and the other note comes due
    sooner ([ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md#accidental-matches-confusion-and-overlap)).
  - Of the 227 mix-ups, 210 had a later spelling recall of the same note
    more than an hour afterwards, at a median of 2.5 days (middle half 1.1
    to 4.1 days). That recall was correct in 157 (75%), repeated the same
    wrong word in 18, named a different note in 17, was otherwise wrong in
    14 and blank in 4. The same wrong word came back at some later recall
    in 55 of the 210.
  - Mix-ups arrived at 6 to 29 a week, about 15 on average. Showing a rise
    from 75% to 90% correct at the next recall, at the usual 5% and 80%
    levels, needs about 100 mix-ups in each group: about 13 weeks.
  - Pair counts: 134 pairs once, 28 twice, 8 three times, 2 four times, 1
    five times. Counting both directions together, 40 pairs were confused
    two or more times (103 answers; 36 in the Japanese notebook). Joined
    through shared words they form 31 groups: 25 of one pair, 4 of two, 1
    of three and 1 of four.
  - The Japanese notebook (read-only clone, 2026-11-17) holds 17 notes
    titled as a difference between words. Two match a confused pair
    (含める と 込める, 脇 と 腋); the first pair was confused again on
    November 3 and 8. The clone's history starts on 2026-09-17, so when
    each contrast note was written is not recoverable.
  - Limits: next-recall figures use the same cue, so they show recall of
    the word, not use in a new sentence. Cue text was exported for 82 of
    the 227 mix-ups.
- **Earlier approach (2026-11-14).** Compare ordinary feedback with a
  short explanation and practice on checked concept items (giver and
  recipient roles, the position of literals in RDF), with a different
  application scored after a comparable delay. The offline replay checked
  that those sample tasks are answerable. The owner chose spelling word
  pairs for this validation on 2026-11-17; the concept items are deferred.
- **Evidence and cleanup.** Use the [local recall evidence](SEED-073-learning-refinement-and-retrieval.md#local-recall-evidence)
  and apply the [shared closeout](SEED-073-learning-refinement-and-retrieval.md#close-out-temporary-recall-evidence).

<a id="validate-source-backed-note-edits"></a>

## Validate source-backed note edits from recall

[Story home](SEED-073-learning-refinement-and-retrieval.md#validate-source-backed-note-edits).

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
- **Evidence and cleanup.** Use the [local recall evidence](SEED-073-learning-refinement-and-retrieval.md#local-recall-evidence)
  and apply the [shared closeout](SEED-073-learning-refinement-and-retrieval.md#close-out-temporary-recall-evidence).

<a id="validate-within-note-objectives"></a>

## Validate follow-up by learning objective

[Story home](SEED-073-learning-refinement-and-retrieval.md#validate-within-note-objectives).

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
- **Evidence and cleanup.** Use the [local recall evidence](SEED-073-learning-refinement-and-retrieval.md#local-recall-evidence)
  and apply the [shared closeout](SEED-073-learning-refinement-and-retrieval.md#close-out-temporary-recall-evidence).

<a id="validate-question-generation-purpose"></a>

## Validate when question generation should abstain

[Story home](SEED-073-learning-refinement-and-retrieval.md#validate-question-generation-purpose).

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
- **Evidence and cleanup.** Use the [local recall evidence](SEED-073-learning-refinement-and-retrieval.md#local-recall-evidence)
  and apply the [shared closeout](SEED-073-learning-refinement-and-retrieval.md#close-out-temporary-recall-evidence).
