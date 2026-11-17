# Source review

Launch: fresh general-purpose agent on 2026-11-18 after the owner split the
story, no conversation inheritance, told to use only the story text and read
no other file. Input: the narrowed story section of
[SEED-073#validate-short-recall-lessons](../../seeds/SEED-073-learning-refinement-and-retrieval.md#validate-short-recall-lessons)
from its heading to the next story anchor, without the state block. It
replaces the review of the original story taken at claim `c653f1232b`;
returned ids are retained as returned.

Unnumbered context (as returned): purpose and owner; the 100-day counts;
out-of-scope list, including the check, scoring, recommendation and
four-week count moved to SEED-073#check-short-recall-lessons; considered
alternatives; the effort hypothesis.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | Pairs: "Start from the pairs confused two or more times in the November 13 export." | slice 1 | observed: short-recall-lessons-evidence.md#fit-check-2026-11-18 provenance (40 pairs, digest rechecked) |
| 2 | Grouping: "Words that were confused with each other, in either direction or through a shared word (いかにも with さも and 甚だしい), form one group." Also Key example 3: "整う and 調う were confused in both directions → one group; both directions fall in the same half." | slice 2 | observed: short-recall-lessons-evidence.md#split-and-lessons-2026-11-18 (groups split as units) and private pairs.json groups |
| 3 | Qualification: "A group enters the trial only when, for each of its pairs, sentences exist in which exactly one of the two words fits, confirmed by two independent readers who do not see the intended word." Also Key example 1: 含める/込める, "Readers given two new sentences each supply only one of the two words → the pair qualifies." | slice 1 | observed: short-recall-lessons-evidence.md#fit-check-2026-11-18 counts and 含める/込める example; private fit-status.json and reader answers |
| 4 | Non-qualifying groups: "The other groups are counted with the reason (both words fit the note's cue, or an overlap is already declared) and take no further part." Also Key example 2: meadow/pasture, "counted as 'both words fit' and gets no lesson and no check." | slice 1 | observed: short-recall-lessons-evidence.md#fit-check-2026-11-18 Counts by reason (11 groups); meadow/pasture did not meet the example's condition (readers separated the words), see its Limits |
| 5 | Assignment: "Qualifying groups are split at random into a lesson half and a comparison half, by a recorded seed, before any lesson is written." (Key example 3: whole group falls in the same half.) | slice 2 | observed: short-recall-lessons-evidence.md#split-and-lessons-2026-11-18 seed, digest and timestamps |
| 6 | Lesson page: "For each pair in the lesson half, one read-once local page: the word the owner typed, the intended word, the one feature that decides between them, two short sentences that differ in that feature, and one practice sentence he answers, followed by the answer and its reason." Also Goal ("takes short contrast lessons for half of the word pairs") and Key example 1 ("Drawn into the lesson half → the owner reads its page and answers the practice sentence"). | slice 4 | observed: short-recall-lessons-evidence.md#lesson-and-check-pages-2026-11-18 dry run and short-recall-lessons-evidence.md#lessons-taken-2026-11-18 (10 pairs answered) |
| 7 | Feature check: "Each stated feature is checked against a dictionary or usage source." | slice 2 | observed: private lessons.json source per pair, summarized in short-recall-lessons-evidence.md#split-and-lessons-2026-11-18 |
| 8 | Comparison half: "The comparison half gets nothing new; both notes stay available to him as they are today." Also Key example 4: "A pair drawn into the comparison half → no page." | slice 2 | observed: private assignment.json and lessons.json (lesson half only); short-recall-lessons-evidence.md#split-and-lessons-2026-11-18 |
| 9 | Check page content: "New sentences for every pair of both halves, not used in any lesson, ... naming neither word; ready for the owner on the check day." | slice 3 | observed: short-recall-lessons-evidence.md#lesson-and-check-pages-2026-11-18 (48 items, none of the 44 words) and no-reuse comparison in short-recall-lessons-evidence.md#split-and-lessons-2026-11-18 |
| 10 | Check fixed beforehand: check sentences "fixed before the lessons were shown" (Goal: "with an unseen check for both halves fixed beforehand"). | slice 1 | observed: short-recall-lessons-evidence.md#fit-check-2026-11-18 frozen check-item digest, before the split and lessons |
| 11 | Record: "The lesson day, minutes spent on lessons, and any lesson he skipped, in the evidence." Also Key example 5: "The owner skips a lesson page → the pair stays in the lesson half and is recorded as not taken." | slice 4 | observed: short-recall-lessons-evidence.md#lessons-taken-2026-11-18 (2026-11-18, 8.9 minutes, none skipped); skip recording seen in the pages dry run |
| 12 | Multi-feature pairs reported: the single deciding feature "breaks where two words differ in several ways at once, and such pairs are reported." (Considered) | slice 2 | observed: short-recall-lessons-evidence.md#split-and-lessons-2026-11-18 (ただいま/たった今) |
| 13 | No product change: "It changes no product behavior." (Goal); "UI. Local pages only." | slice 4 | observed: git diff c653f1232b..HEAD changes only .planning records; pages are local files |
| 14 | Restriction, production read only: "no note, alias, overlap, grade or tracker is written, so the comparison half's recalls stay ordinary and the owner's notebook and schedule are untouched (ADR 0003)." | slice 4 | observed: this execution made no production call; inputs were the cached export only |
| 15 | Restriction, private evidence: "Pages, answers and the pair list hold private learning evidence. They stay in the local cache and are not committed." | slice 4 | observed: short-lessons/ is ignored by .planning/tmp/recall-history/.gitignore; git diff c653f1232b..HEAD has no file from it |
| 16 | Seed content: "the seed keeps counts and a few short examples." | slice 4 | observed: short-recall-lessons-evidence.md holds counts, digests and short examples only |
| 17 | Lesson page UI: "A lesson page shows one pair at a time and asks for the practice answer before showing it." | slice 3 | observed: short-recall-lessons-evidence.md#lesson-and-check-pages-2026-11-18 dry run |
| 18 | Check page UI: "The check page shows one sentence at a time with a meaning hint and a blank, gives no feedback until all are answered." | slice 3 | observed: short-recall-lessons-evidence.md#lesson-and-check-pages-2026-11-18 dry run |
| 19 | Check answers saved: the check page "saves the answers to a local file." | slice 3 | observed: short-recall-lessons-evidence.md#lesson-and-check-pages-2026-11-18 dry run download with per-item times |
