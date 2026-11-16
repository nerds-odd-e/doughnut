# Recall question integrity: initial recommendation

Retained dated evidence from `SEED-073-learning-refinement-and-retrieval.md` at revision
`1baf636d9a14d853de81cd67081e47c3e2ad96be`; dates, inputs, results and limitations
below are preserved. The [canonical seed](SEED-073-learning-refinement-and-retrieval.md) owns current story decisions.

<a id="recall-question-integrity-recommendation"></a>

**Recall question integrity: initial recommendation (validated 2026-11-16).**
A wrong mark on a multiple-choice recall question
should mean the learner chose a wrong answer. The [validation results](learning-refinement-and-retrieval-evidence.md#recall-question-integrity-results)
hold the counts, commands and limits. A clean baseline holds for repeated
choices on both generating routes; the other classes still reach a learner.

**Owner disposition, 2026-11-16.** The semantic-checking result is inconclusive.
Queue [further exploration with paid experiments](https://github.com/nerds-odd-e/doughnut/blob/5e91080a952b5bd2d5b3aa6cb06ddf9c0f27a475/.planning/seeds/SEED-073-learning-refinement-and-retrieval.md#explore-reliable-recall-question-checks)
at the top of the product backlog. The initial recommendation below remains
dated evidence; this disposition selects further investigation and leaves
the product design and stored-question policy undecided. The follow-up owns
continued use of the private integrity evidence.

| Failure class | On demand | Batch | Stored |
| --- | --- | --- | --- |
| Repeated choices | **Stop.** Rejected at generation since 2026-08-22. | **Stop.** Rejected; a maintained test holds it. | **Proceed to design**, within the stored-route decision below: 10 stored questions from 2023–24, 3 next in line. |
| Unique correct answer rejected by the key | **Narrow and retest.** None seen after the change in the reviewed set; the generator is shared with batch. | **Narrow and retest.** One case (row 5347) among 46 reviewed misses; the verifier upheld it and no check without the key can see it. | **Proceed to design**, within the stored-route decision. |
| More than one defensible answer | **Narrow and retest.** Row 7893; the verifier flagged it on feasibility only. | **Narrow and retest.** Rows 4917 and 5037; the verifier caught 4917 and missed 5037. | **Proceed to design**, within the stored-route decision; row 3748 was shown 20 months after generation. |

- **Stored route.** Every check added since a question was stored is
  bypassed. Of the 2,650 questions next in line, 1,969 were generated
  before 2026-05-16 and 641 fail a check today's generation applies without
  the key. The owner chooses between checking a stored question when it is
  shown and discarding stored questions older than a protection; this
  evidence does not separate them.
- **Generating routes, key and two-answer defects.** The verifier as
  deployed would replace about half of sound questions: it flagged 12 of 26
  sound questions and 10 of 15 the learner answered correctly, almost all
  on feasibility. Judged on its selected choices alone, it flagged 4 of 8
  defects and 1 sound question. Retest the choices-only rule on a larger
  sample of misses after 2026-08-22 before choosing "verify before a
  question is shown" or "verify after a miss only". Each retest needs its
  own paid-call authorization.
- **Hypothesis.** Option uniqueness already stops repeated choices at
  generation. Agreement with the verifier finds two-answer questions by its
  choices, but its feasibility judgment costs too many sound questions, and
  it rarely finds a wrong key.
- **Regression examples.** A route-level test is justified only with an
  approved change. For a stored-route change: stored choices ["alpha",
  "beta", "beta", "gamma"] with the key at the second position; answering
  the third is marked wrong today. The on-demand route has no route-level
  repeated-choice test (its proof is the shared
  `GeneratedMcqTest.rejectsInvalidSemanticAnswerSets`); for example,
  answer "alpha" with distractors "beta", "gamma" and " alpha " stores
  nothing.
- **Temporary evidence.** Until the owner decides, keep the cache's
  `integrity/` folder: a retest reuses its census, labels and verifier
  route. A decision to stop releases this story's use.

| Choice | Evidence now |
| --- | --- |
| Verify before a question is shown | Detects 5 of 8 defects but flags about half of sound questions as deployed; choices alone 4 of 8 with 1 false flag. Needs the retest. |
| Verify after a miss only | Same verifier figures on missed questions (5 of 8 detected, 11 of 25 sound flagged); 963 of 8,489 attempts were misses. |
| Keep showing stored older questions | 8,254 unanswered stored questions, 7,573 generated before 2026-05-16; repeated choices and row 3748 show stored defects reach the learner. |
| Check a stored question when it is shown | Today's keyless checks would reject 641 of the 2,650 next in line; key defects need the verifier. |
| Discard stored questions older than a protection | 1,969 of 2,650 next in line predate 2026-05-16, 2,119 predate 2026-08-22. |
