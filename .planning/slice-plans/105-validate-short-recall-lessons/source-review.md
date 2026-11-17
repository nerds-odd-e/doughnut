# Source review

Launch: fresh general-purpose agent on 2026-11-18, no conversation
inheritance, told to use only the story text and read no other file. Input:
the story section of
[SEED-073#validate-short-recall-lessons](../../seeds/SEED-073-learning-refinement-and-retrieval.md#validate-short-recall-lessons)
from its heading to the next story anchor, without the state block, at
claim `c653f1232b`. Returned ids are retained as returned.

Unnumbered context (as returned): goal framing; the 100-day counts; "Local
pages only"; the out-of-scope list; considered alternatives; the effort
hypothesis; key-example details as illustrations.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | Pairs: "Start from the pairs confused two or more times in the November 13 export." | slice 1 | observed: short-recall-lessons-evidence.md#fit-check-2026-11-18 provenance: 40 pairs ≥2 mix-ups from the Nov 13 export (digest rechecked); private pairs.json |
| 2 | Grouping: "Words that were confused with each other, in either direction or through a shared word (いかにも with さも and 甚だしい), form one group." KE3: 整う/調う "confused in both directions → one group; both directions fall in the same half and are checked together." | slice 2 | observed: short-recall-lessons-evidence.md#split-and-lessons-2026-11-18: groups split as units; private pairs.json groups and assignment.json |
| 3 | Qualification: "A group enters the trial only when, for each of its pairs, sentences exist in which exactly one of the two words fits, confirmed by two independent readers who do not see the intended word." KE1: 含める/込める readers "each supply only one of the two words → the pair qualifies." | slice 1 | observed: private fit-status.json and round1-scores.json/answers-A..D.json: 20 groups pass with both blind readers writing the intended word for every sentence; evidence Counts by reason and 含める/込める example |
| 4 | Non-qualifying groups: "The other groups are counted with the reason (both words fit the note's cue, or an overlap is already declared) and take no further part." KE2: meadow/pasture "counted as 'both words fit' and gets no lesson and no check." | slice 1 | observed: short-recall-lessons-evidence.md Counts by reason: 11 groups counted out (9 declared overlap, 2 readers wrote the other word) with private fit-status.json; KE2 antecedent (readers accept pasture) did not occur, see Limits |
| 5 | Assignment: "Qualifying groups are split at random into a lesson half and a comparison half, by a recorded seed, before any lesson is written." | slice 2 | observed: short-recall-lessons-evidence.md#split-and-lessons-2026-11-18: seed 20261118, assignment digest and 00:32:18 timestamp before lessons 00:34:45 |
| 6 | Lesson page: "For each pair in the lesson half, one read-once local page: the word the owner typed, the intended word, the one feature that decides between them, two short sentences that differ in that feature, and one practice sentence he answers, followed by the answer and its reason." UI: "A lesson page shows one pair at a time and asks for the practice answer before showing it." KE1: "the owner reads its page and answers the practice sentence." | slice 3 | observed: short-recall-lessons-evidence.md#lesson-and-check-pages-2026-11-18: lesson.html dry run — one pair at a time, typed/intended/feature/two examples, practice answer and reason withheld until answered |
| 7 | "Each stated feature is checked against a dictionary or usage source." | slice 2 | observed: private lessons.json source field per pair (Weblio/デジタル大辞泉, Merriam-Webster, Wikipedia), summarized in evidence Split and lessons |
| 8 | Comparison half: "The comparison half gets nothing new; both notes stay available to him as they are today." KE4: "A pair drawn into the comparison half → no page; the same kind of unseen sentences at the check." | slice 2 | observed: private assignment.json comparison list: 10 groups get no lesson content in lessons.json (lesson half only); evidence Split and lessons |
| 9 | Check timing and coverage: "Seven days later (five to nine accepted), the owner fills new sentences for every pair of both halves, typing the word without notes or tools." KE1: "two unseen sentences for the pair." | slice 5 | |
| 10 | Check sentences: "were not used in any lesson, were fixed before the lessons were shown, and do not name the two words." | slice 3 | observed: evidence: check items frozen in fit check (digest 9db182f9…) before the split and lessons; no lesson sentence reuses one (Split and lessons); check.html holds none of the 44 pair words (Pages) |
| 11 | Check page UI: "shows one sentence at a time with a meaning hint and a blank, gives no feedback until all are answered, and saves the answers to a local file." | slice 3 | observed: short-recall-lessons-evidence.md#lesson-and-check-pages-2026-11-18: check.html dry run — one sentence at a time with hint and blank, no feedback, saves answer file |
| 12 | Record per pair: "half, check answers (right, the paired word, other), minutes spent on lessons and on the check, any lesson he skipped, and any check item missed or late." KE1: "each is recorded as right, the paired word, or other." | slice 5 | |
| 12.1 | Late check (KE5): "The owner does the check on day 12 → the answers are recorded and marked late, and reported apart from on-time answers." | slice 5 | |
| 12.2 | Skipped lesson (KE6): "The owner skips a lesson page → the pair stays in the lesson half and is reported as not taken." | slice 5 | |
| 13 | Four-week recall count: "His spelling recalls of the trial's notes are counted for four weeks from the lesson day from a fresh read-only export." | slice 6 | |
| 14 | Recommendation: "After the check: the counts for both halves, the effort, and proceed, narrow and retest, or stop, for the owner's review." (Goal: "a recommendation to proceed to design, narrow and retest, or stop.") | slice 5 | |
| 15 | "The four-week recall count is added when it is due and may revise the recommendation." | slice 6 | |
| 16 | "Evidence closeout follows the shared contract (#close-out-temporary-recall-evidence)." | slice 6 | |
| 17 | Restriction: "Production is read only: no note, alias, overlap, grade or tracker is written, so the comparison half's recalls stay ordinary and the owner's notebook and schedule are untouched (ADR 0003)." Goal: "it changes no product behavior." | slice 6 | |
| 18 | Restriction: "Pages, answers and the pair list hold private learning evidence. They stay in the local cache and are not committed; the seed keeps counts and a few short examples." | slice 6 | |
| 19 | Limits stated with the result: "One learner; 31 groups before the fit check, so about 15 a half at most: a direction, not proof. The owner knows which pairs had lessons. Lessons come days or weeks after the misses, not at the miss. A lesson adds one more exposure to the pair, which the comparison half does not get." | slice 5 | |
| 20 | Considered: the single deciding feature "breaks where two words differ in several ways at once, and such pairs are reported." | slice 5 | |
