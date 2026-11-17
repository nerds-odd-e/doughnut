# Short lessons after recall misses: evidence

Story: [SEED-073#validate-short-recall-lessons](SEED-073-learning-refinement-and-retrieval.md#validate-short-recall-lessons).
Private files (pair list, check items, readers' answers, scripts) stay in the
local recall-history cache and are not committed; this file keeps counts,
digests and short examples.

## Fit check (2026-11-18)

**Result: 20 of 31 groups can be tested (24 pairs, 48 check sentences).**
The plan's stop line of 12 groups is passed.

### Provenance and method

- Input: the November 13 private export (SHA-256
  `3071dac175df47cd172c6bbc54f8fe887f475658f950ca340e9e441dde0ad3df`,
  rechecked before and after use). Mix-ups are answers whose outcome is
  `ACCIDENTAL_MATCH`; a pair is the expected note title and the typed word,
  both directions together, kept at two or more mix-ups.
- Pairs and groups: 40 pairs (103 answers; 36 Japanese, 4 English), 31
  groups joined through shared words: 25 of one pair, 4 of two, 1 of three,
  1 of four. Same counts as the plan's premise.
- Sentences: an AI author wrote two new sentences per pair, one where only
  each word fits, each with a meaning hint naming neither word of the
  group, and the accepted written forms per sentence (including needed
  inflections), before any reader saw them. 80 sentences, shuffled by seed
  `20261118` so a pair's two sentences are not adjacent.
- Readers: two fresh AI contexts each saw only the 80 shuffled sentences and
  hints, with no words, no pair, no web access, and wrote one word per
  blank. A sentence passes when both readers write an accepted form.
- Revision: the four failed sentences of groups not already out for a
  declared overlap were rewritten once, labelled revised, and read by two
  further fresh contexts.
  The 脇/腋 sentence, where both readers wrote the other word, was not
  revised; the plan counts that as both words fit.
- Declared overlaps: a pair is out when either note's front matter already
  lists the other word as an overlap (exact link-target match).
- Frozen check items: SHA-256
  `9db182f94e32085410ca992e7461a43da9a15f5c6d0905aa8edfc4b2d63a8da1`;
  fit status per group:
  `3669d5c7c566e96acdaeb545066e9ff4f5294e69dd5ae830211aecc89dd3de2e`.

### Counts by reason

| Groups | Reason |
| --- | --- |
| 20 | Pass: every sentence of every pair answered with the intended word by both readers (2 groups after a revised sentence) |
| 9 | Both words fit: an overlap with the other word is already declared (10 pairs) |
| 2 | Both words fit: the readers wrote the other word (整う/調う after revision, 脇/腋) |

In round one, 73 of 80 sentences passed both readers. Of the seven
failures, two were in a group already out for a declared overlap and one
drew the other word; the other four were rewritten and three of those
passed.

### Examples

- 含める/込める (key example 1): both readers wrote 含めた and 込めて in
  the two sentences → the group passes.
- 整う/調う (key example 3): the revised sentence about a concluded
  marriage talk drew 整った from both new readers, the other kanji of the
  same reading → both words fit; the group takes no further part.

### Limits

- Readers are the same AI model family as the sentence author; their
  agreement shows the sentences are clear to a fluent reader, not that the
  owner will find them so.
- meadow/pasture passes. Key example 2 expected it to fail because its
  note cue named grazing, but the check sentences do not use the cue and
  both readers separated the two words. The pair is kept by the stated rule.
- A declared overlap removes a group even when readers separated its words
  (9 groups); the owner had already accepted both words for those notes.

## Split and lessons (2026-11-18)

- Split: the 20 passing groups, sorted by id, were shuffled with Python
  `random.Random(20261118)`; the first 10 form the lesson half (10 pairs),
  the rest the comparison half (14 pairs, including the four-pair いかにも
  group). Groups, not pairs, were split, so both directions of a pair and
  all pairs of a group share a half. Assignment file SHA-256
  `1339038df0770ce7d4e34f48760598efe0555bb1fc92bed48ceb56464fe95144`,
  written 00:32:18 UTC, before the first lesson file (00:34:45).
- Lessons: one per lesson-half pair, from the word the owner typed to the
  intended word, with one deciding feature, two contrasting sentences and
  a practice sentence. Every pair was confused in one direction only. Each
  feature cites a dictionary or encyclopedia page (デジタル大辞泉 through
  Weblio for the Japanese pairs; Merriam-Webster and Wikipedia for the
  English ones). For 付く/付ける and 砕く/砕ける the transitive and
  intransitive reading comes from the dictionary's definitions and
  examples, not from a label. Lesson content SHA-256
  `db5e7ffd6fdfc2b25c2749ff8849f95b253578eefc7b42b2e525338170a272d4`.
- Several features: ただいま/たった今 differ in tone and in ただいま's
  greeting use more than in meaning; the lesson teaches greeting or formal
  word versus casual time word and the pair is reported here.
- Practice check: with an open blank and a meaning hint, a fresh reader
  wrote the intended word for 5 of 10 practice sentences, choosing other
  near words (割れて, 見事な, あぐねて, 今, だから). Each practice item now
  names the two words to choose from, as a contrast lesson does; a second
  fresh reader then chose the intended word in the needed form for 10 of 10.
  The check sentences stay open blanks and do not name the words.
- No lesson sentence reuses a check sentence: the 30 lesson sentences were
  compared with the 48 frozen check sentences; the closest text similarity
  was 0.48, and shared set phrases were avoided (for example 言い煩う in
  the lesson against 思い煩う in the check).
