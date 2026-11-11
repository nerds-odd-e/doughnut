---
id: SEED-124
status: active
planted: 2026-11-11
scope: story
---

# Save pronunciations as note properties

<a id="save-pronunciations-as-note-properties"></a>

### Save pronunciations as note properties

**Identity:** SEED-124#save-pronunciations-as-note-properties
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/001-save-pronunciations-as-note-properties/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"a3087c11d42e7b8d014d7726f4a608b5119c4806921e43558e9b42863d639ca5","plan":"9f40445b7380997edab7904776d9e81d689570a45bad74cd377bb299f613d406"}}
```

**Goal.** A language learner records how a note's title is read in a
`pronunciation` property instead of marking it inside the note text, and
Spelling review still never shows that reading while it asks for the title.
The owner's existing readings in the 日本語学習 notebook move into the same
property, and those notes keep their learning history. This serves the
near-future direction of better note quality: the reading becomes named data,
where today it is a `/…/` marking in prose that Spelling review has to
recognize by pattern.

**Scope: the property and Spelling review.**

- When a learner edits an ordinary note, **Add property** lists `pronunciation`
  among its key choices. No choice is preselected, and no note receives the
  property unless someone adds it.
- Choosing it adds an ordinary YAML property, for example
  `pronunciation: ただい`. The value is plain text. As with other ordinary
  properties, a YAML list holds several readings. A property typed by hand or
  received through Git publication behaves the same as one added through
  **Add property**.
- In Spelling review of that note, every place where the note body repeats a
  `pronunciation` value is hidden. The prompt is the note body without its
  properties, so the property itself is not shown either.
- The expected answer stays the note title, in the forms accepted today: the
  title, its part before trailing brackets, and its aliases. Being a
  `pronunciation` value does not make a text an accepted answer. Spelling
  review trains the written form, and accepting the reading would pass a
  learner who did not recall it.
- `pronunciation` is otherwise an ordinary property. It is shown and edited
  with the note's other properties, is published in the note's frontmatter,
  and is offered in the assimilation sequence as its own unit, as every
  ordinary property is. The owner confirmed this on 2026-11-11.
- Scope limit: Spelling review keeps hiding `/…/` readings written in the note
  body exactly as it does today. Notes that are not converted depend on it.

**Scope: converting 日本語学習, one time.**

- The conversion runs in the owner's own checkout of the notebook. An agent,
  or a script that is not kept, edits the checkout. No conversion code enters
  this repository. The owner chose this on 2026-11-11.
- Before anything is published, the owner gets a preview: for each note that
  would change, its title, the reading, and the changed text before and after,
  grouped by the kind of change so that similar changes can be checked
  together; and a list of the notes that contain `/…/` but would stay
  unchanged, each with the reason. Git's diff of the checkout shows the exact
  changes.
- A note is converted when its body has exactly one `/…/` reading as Spelling
  review recognizes it today, that reading is written only in kana, and the
  note has no `pronunciation` property. The reading becomes the
  `pronunciation` value. The body loses the marked reading together with the
  spacing or the bracket pair that existed only to hold it. All other text
  stays, including readings of other words.
- A note stays unchanged and is listed when it has several `/…/` matches, when
  its match is not plain kana, when it already has a `pronunciation` property,
  or when removing the reading would leave a sentence without its subject. The
  owner may edit such notes by hand in the checkout, or ask for them one by
  one, before publishing.
- The owner publishes with `donut notebook publish`, the normal local
  publication. Nothing changes in production until that publication is
  accepted.
- Converted notes are the same notes: their memory trackers and recall history
  stay attached. This is checked in production on converted notes after
  publishing.

Measured on 2026-11-11 from 日本語学習's accepted Git history at commit
`cc485736`, with the text rule Spelling review applies today, notes outside
trash: 750 notes have at least one `/…/` match; 641 have exactly one match in
plain kana, which makes them the candidates for conversion; 56 have one match
that is not plain kana; 53 have several matches. No note has a `pronunciation`
property. As ordinary properties, the converted readings add up to 641 units
to the owner's assimilation sequence.

**Deferred.** Not delivered here, and not prevented.

- Converting other notebooks. The owner's English notebook has 57 notes with
  `/…/` matches, mostly IPA; Spanish and 中文 have at most one each. Notebooks
  of other owners are not converted and were not counted, by the owner's
  decision. Their `/…/` readings stay hidden as today.
- Removing Spelling review's handling of `/…/` in the note body. In 日本語学習
  about 90 of the 750 notes have a match that is not a plain kana reading. The
  sampled ones are parts of web addresses written as plain text and
  slash-separated alternatives or translation pairs, which today's rule hides
  as if they were readings. Removal needs every notebook that relies on the
  rule to be converted first.
- Rewriting by rule a sentence whose subject is the reading itself, for
  example `/あしかが よしみつ/は、室町時代…`, which about 11 of the candidates
  do. Putting the title in its place was considered. It adds text the author
  did not write, so the rule leaves those notes to the owner.
- Accepting the reading as an answer, or a review that asks for the reading.
- Recording readings of other words that appear in a note body.

Considered and not chosen for the conversion: a tested temporary script kept
in this repository, and a temporary startup migration in production over every
notebook, as the numbered-property consolidation did on 2026-10-02. The second
would change other owners' prose without their review.

**Key examples.**

- A learner edits the note **多大**, opens **Add property**, and sees
  `pronunciation` among the key choices with none preselected. They choose it,
  enter `ただい`, and add it. The note now stores `pronunciation: ただい`.
- **多大** has `pronunciation: ただい` and the body
  `数や量が非常に大きいこと。「多大（ただい）なるご迷惑」`. Its Spelling prompt
  hides both 多大 and ただい. Answering `多大` is correct. Answering `ただい` is
  incorrect.
- **顔色** has `pronunciation` with the two values `かおいろ` and `がんしょく`.
  Its Spelling prompt hides both wherever the body repeats them.
- A note has `/じゅわ/` in its body and no `pronunciation` property. Its
  Spelling prompt hides that reading as it does today.
- Conversion, reading after the title: `「多大　/ただい/」とは…` becomes
  `「多大」とは…` with `pronunciation: ただい`. The inline `甚大（じんだい）`
  stays with its explanation. The note keeps its memory trackers and recall
  history.
- Conversion, reading in brackets: `水引（/みずひき/）は、…` becomes
  `水引は、…` with `pronunciation: みずひき`.
- Left unchanged and listed: **音** has `/ね/` and `/おん/` (several readings);
  **コツ** has `釣りのコツ/钓鱼的秘诀。 コツを覚える/学会窍门` (not a reading);
  **足利義満** begins `/あしかが よしみつ/は、…` (removing the reading would
  leave the sentence without its subject); a note that already has a
  `pronunciation` property.

**UI.**

- `pronunciation` appears in the existing key choice list of **Add property**
  and of a property row's key field. It adds no new control.
- In the Spelling prompt, a hidden `pronunciation` value uses the existing
  hidden-pronunciation mark `/.../`, the one the learner already sees for a
  `/…/` reading. It tells the learner that a reading is hidden there, not the
  answer.
- A one-kana reading such as `て` is also hidden inside other words, as in
  `挙げて`, because the existing matching rule accepts any kana as a word
  boundary. A `/て/` marking in the body does the same today.

**Architecture.**

- Publication. Under [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md),
  every accepted change to notebook content goes through Git publication. A
  SQL update of note content would change content outside that boundary: the
  live notes would differ from accepted `main`, which local publication
  refuses until a recovery, and the property index and reference rows that the
  application derives on a content write would be skipped. The original
  condition for pure SQL, that it can prepare the changes safely, is not met.
  The conversion uses that boundary's local path, which the owner already uses
  for this notebook. Production runs no conversion code.
- Format. Under [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md),
  `pronunciation` is author-owned YAML frontmatter that is preserved as
  authored. It needs no format rule of its own.
- One concept in two places, for now. Readings can be a `/…/` marking in the
  body or a `pronunciation` property, and Spelling review reads both. This is
  deliberate while unconverted notes exist. Removing the body marking is the
  deferred item above.
- Spelling review reads `pronunciation` from the same note content it already
  reads `aliases` from. It needs no new stored data, index, or API field.
