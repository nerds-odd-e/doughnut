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
