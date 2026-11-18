# Short lessons after recall misses: evidence

Story: [check short lessons and count later recalls](SEED-073-learning-refinement-and-retrieval.md#check-short-recall-lessons).
Private files (pair list, check items, readers' answers, scripts) stay in the
local recall-history cache and are not committed; this file keeps counts,
digests and short examples.

## Fit check (2026-11-18)

**Result: 20 of 31 groups were kept for the check (24 pairs, 48 check
sentences); 14 of them met the fit-check rule (7 in each half).** Six kept
groups passed only through another accepted word and are
[reported apart](#groups-reported-apart). The plan's stop line of 12 groups
is passed.

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
  blank. A sentence passes when both readers write the intended word, or a
  script or inflected form of it. The scoring script counted any entry of
  the author's accepted list as a pass, and those lists include some
  different words.
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
| 14 | Pass: every sentence of every pair answered with the intended word by both readers (2 groups after a revised sentence) |
| 6 | Kept, reported apart: some sentence was answered only with another accepted word, or with a kanji both words share |
| 9 | Both words fit: an overlap with the other word is already declared (10 pairs) |
| 2 | Both words fit: the readers wrote the other word (整う/調う after revision, 脇/腋) |

In round one, 73 of 80 sentences passed both readers. Of the seven
failures, two were in a group already out for a declared overlap and one
drew the other word; the other four were rewritten and three of those
passed.

<a id="groups-reported-apart"></a>

### Groups reported apart

The author's accepted lists, frozen before any reader answered, include
words other than the intended one. These six groups passed only through
such an entry and stay in the check and the split; the check reports them
apart from the 14.

| Group | Half | Sentence | Readers wrote | Intended word |
| --- | --- | --- | --- | --- |
| G03 経つ/過ぎる/過ごす | comparison | two | 越して (both); 切れた (both) | 過ぎる |
| G22 ただいま/たった今 | lesson | one | さっき (both) | たった今 |
| G26 凄い/恐ろしい | lesson | one | 怖く (both) | 恐ろしい |
| G20 いくらか/いくら | comparison | one | 幾分 (one reader) | いくらか |
| G21 それで/それに | lesson | one | で (one reader) | それで |
| G23 やつ/やっこ | comparison | both | 奴 (both, for both words) | やつ, やっこ |

奴 is the kanji of both やつ and やっこ and is accepted for both, so the
readers did not separate that pair.

The 14 qualified groups (ids as in the private check items):

- Lesson half (7): G10 含める/込める, G17 brine/curing, G18
  caravel/galley, G24 不正/犯行, G25 付く/付ける, G27 悩む/煩う, G29
  砕く/砕ける.
- Comparison half (7): G01 いかにも (four pairs), G07 尚更/更に, G08
  にこやか/和やか, G11 見かける/見つける, G12 meadow/pasture, G13
  下がる/下る, G31 黙り/黙る.

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

- Split: the 20 kept groups, sorted by id, were shuffled with Python
  `random.Random(20261118)`; the first 10 form the lesson half (10 pairs),
  the rest the comparison half (14 pairs, including the four-pair いかにも
  group). Groups, not pairs, were split, so both directions of a pair and
  all pairs of a group share a half. Assignment file SHA-256
  `1339038df0770ce7d4e34f48760598efe0555bb1fc92bed48ceb56464fe95144`,
  written 2026-11-18 00:32:18 SGT (2026-11-17 16:32:18 UTC), before the
  first lesson file (00:34:45 SGT). Of each half, seven groups met the fit
  check and three are [reported apart](#groups-reported-apart).
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

## Lesson and check pages (2026-11-18)

- Two self-contained local pages built from the lesson content and the
  frozen check items (`lesson.html` SHA-256
  `dbf0c1197c805a8411be7a5114a9c4eea18c625e4bbce60b0698e22cb5f865af`,
  `check.html` SHA-256
  `fbc1947144744622f70d0e97ccc046e6df69a42e8acf9bff597eb6be70fa5916`).
  Each saves its answers through a browser download, with start, end and
  per-item times and a skipped flag.
- Dry run in headless Chromium with made-up answers: the lesson page showed
  one pair at a time (1 of 10 to 10 of 10), hid the practice answer and
  reason until an answer was given, and recorded a skipped pair. The check
  page showed one of 48 sentences at a time, in an order shuffled by seed
  `20261118` with no two sentences of a pair adjacent, gave no feedback and
  ended with "Saved." The dry-run answer files were deleted.
- The check page embeds only each item's id, sentence and hint. None of the
  44 pair words occurs in it; four accepted alternative spellings occur by
  chance inside other items' sentences or hints (for example 少し).

## Lessons taken (2026-11-18)

- The owner took the lesson page on 2026-11-18 from 07:22 to 07:31 SGT,
  8.9 minutes for 10 pairs (36 to 79 seconds a pair). No pair was skipped.
  Practice answers: 7 of 10 right on the first answer (wrong: 含める/込める,
  付く/付ける, 悩む/煩う). Answer file SHA-256
  `9b991c988161ef79223adfa8f280cebb6f6d4e8da884e6dc1ade89e86fc464e8`.
- Check window: day 5 to day 9, 2026-11-23 to 2026-11-27, aimed at
  2026-11-25. Answers after 2026-11-27 are reported as late.
