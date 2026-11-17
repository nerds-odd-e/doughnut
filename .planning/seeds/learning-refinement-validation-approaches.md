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
The product owner approved the [fixed-skill retest](SEED-073-learning-refinement-and-retrieval.md#retest-fixed-skill-spelling-objectives).

<a id="validate-short-recall-lessons"></a>

## Validate short lessons after recall misses

[Story home](SEED-073-learning-refinement-and-retrieval.md#validate-short-recall-lessons).

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
