# Save pronunciations as note properties

**Identity:** SEED-124#save-pronunciations-as-note-properties
**Source:** [Save pronunciations as note properties](../../seeds/SEED-124-save-pronunciations-as-note-properties.md#save-pronunciations-as-note-properties)
**Source review:** [source review](source-review.md)

## Goal and scope

A language learner records how a note's title is read in a `pronunciation`
property, and Spelling review hides that reading wherever the note body
repeats it. The owner's existing `/…/` readings in the 日本語学習 notebook
(notebook 15 in production) move into that property through the owner's own
checkout and `donut notebook publish`, and the converted notes keep their
memory trackers and recall history.

Included: `pronunciation` among the key choices of **Add property**; Spelling
review hiding every value of the property; the one-time conversion of
日本語学習 with a preview, publication in parts, and a check in production.

Not included: any change to which properties are indexed (`pronunciation` is
an ordinary property and is offered in the assimilation sequence); any change
to accepted answers; any change to how `/…/` readings in a note body are
hidden; conversion of other notebooks; conversion code in this repository.

Assumption: the owner's CLI login on the executing machine can clone, pull and
publish notebook 15, as it does today.

## Current decisions and existing solutions

- **Review owner.** `Note.createMaskedContentForRecall` builds the Spelling
  prompt from the body without frontmatter through `ClozedString` and
  `ClozeReplacement`, and already passes the title and `aliases`. Change that
  owner: pass the `pronunciation` values as one more list of hidden texts.
  Hide them with the existing word rule (`TitleFragment.replaceLiteralWords`)
  and the existing hidden-pronunciation mark, before titles are hidden. Add no
  second recognizer.
- **Reading the property.** Follow the existing `Frontmatter…` readers beside
  `FrontmatterAliases` (`FrontmatterNoteLevel`, `FrontmatterOverlaps`): a
  small reader returns the values of `pronunciation` from note content, a
  scalar as one value and a list as its items, through
  `Frontmatter.getPropertyValue`. `Note.java` has 234 lines; keep the reading
  out of it so that it gains one call.
- **Answers.** `Note.matchAnswer` accepts title forms and aliases. It does not
  change.
- **Key choices.** `frontend/src/utils/noteContentPropertyKeyPresets.ts` owns
  the list. Add `pronunciation` to the general list after `example of`. The
  existing list-capable rule then keeps it offered when the note already has
  it, and adding it again appends a value. Container readme editors list it
  too, where it has no effect; limiting it to notes would need a second kind
  of note-only key, which this story does not add.
- **No end-to-end scenario is added.** The review rule is proved at the
  controller and the key choice in the mounted editor. The two share only the
  YAML text `pronunciation: …`, which each proof states literally. The check
  in production in slices 7 and 8 observes real notes.
- **Conversion runs outside this repository.** It works on a clone of
  notebook 15 made with `donut notebook clone 15 <directory>`, in a directory
  outside this repository. Its script and its preview report live outside both
  repositories and are deleted after slice 8. Nothing of it is committed here.
  This plan retains only counts, commit IDs and results.
- **Conversion commits stay on a local branch.** In the clone, work on a
  branch named `conversion`. Local `main` moves only in slices 7 and 8, so
  nothing can be published before the owner's go-ahead.
- **Publish after production hides the property.** Until production runs
  slice 1, a converted note whose body repeats its reading would show that
  repeat in its Spelling prompt. Slice 7 therefore releases the application
  before it publishes.
- **Execution mode.** Story Branch Mode. Slice 7 carries the human checkpoint
  and names the Dough Land `--keep-worktree` landing that precedes the
  release.
- [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  requires every accepted content change to go through Git publication; the
  conversion uses its local path. [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  preserves author-owned frontmatter keys such as `pronunciation`. Both are
  Accepted; no conflict was found.
- `.planning/NORTH-STAR.md` was reviewed. Its accepted-change topic already
  covers local publication. This work needs no new short-term direction.

## Outside-in proof ownership

| Promise or example | Slice | Observable proof |
| --- | --- | --- |
| The Spelling prompt of 多大 with `pronunciation: ただい` hides the title and the reading in `「多大（ただい）なるご迷惑」`, with the hidden-pronunciation mark for the reading | 1 | `RecallPromptSpellingStemMaskingControllerTest`: the stem without mark tags equals `数や量が非常に大きいこと。「[...]（/.../）なるご迷惑」` and contains the `Hidden pronunciation` mark. |
| Two values, `かおいろ` and `がんしょく`, are both hidden; a YAML list authored by hand behaves like a value added in the editor | 1 | Same test class, content with a two-item YAML list: both repeats are `/.../`. |
| Answering 多大 is correct and answering ただい is incorrect | 1 | `RecallPromptAnswerSpellingControllerTest`, one case on a note with the property. |
| A `/…/` reading in the body is hidden as today | 1 | The existing `ClozeDescriptionTest` and masking controller cases stay green in the backend suite. |
| **Add property** lists `pronunciation`, none preselected; choosing it and entering ただい stores `pronunciation: ただい` | 2 | `RichMarkdownEditor.propertyPresets.spec.ts`: the opened draft has an empty key and the listed choices include `pronunciation`; one added case selects it, adds the value, and reads the property from the emitted Markdown. |
| `pronunciation` stays an ordinary property, offered in the assimilation sequence | 1, 2 | No key-specific rule exists to test. Reading at acceptance: the story's diff leaves `PropertyKeyNaming` and the frontend structural-key lists unchanged. Existing property features keep proving the general rule. |
| The lasting rule is documented | 2 | `docs/note-content-saving.md` names `pronunciation` among the list-capable key choices and says that Spelling review hides its values in the note body. |
| The preview lists the notes that contain `/…/` but stay unchanged, each with the reason (音: several readings; コツ: not a reading; 足利義満: the reading is the sentence's subject; a note that already has the property) | 3 | The listing, with counts that match the measurement at `cc485736`. |
| The preview shows each changed note's title, reading and changed text, grouped by kind of change | 4, 5, 6 | One preview section per kind, listing every note changed in that slice's commits. |
| A converted body loses only the marked reading and the wrapper that held it; `「多大　/ただい/」とは…` becomes `「多大」とは…` and `水引（/みずひき/）は、…` becomes `水引は、…`; `甚大（じんだい）` stays | 4, 5, 6 | The mechanical check of each commit, defined in slice 4, plus these two notes shown in the preview with exactly this result. |
| Nothing changes in production before the owner's go-ahead; publication uses `donut notebook publish` | 3 to 8 | The clone's local `main` equals accepted `main` until slice 7; the publish output names the accepted head. |
| Converted notes keep their memory trackers and recall history | 7, 8 | Production `note-info` and recall history of the chosen notes, read before and after publication, are equal. |
| No conversion code enters this repository | 8 | Reading at acceptance: the story's diff holds backend, frontend, documentation and planning files only. |

## Ordered slices

### 1. Spelling review hides saved pronunciations
Type: Behavior
Status: done
Proof: The three controller observations in the table and a green backend suite.

Behavior: Given a note whose frontmatter has `pronunciation` with one value or
a list, and a spelling memory tracker, when its Spelling prompt is produced,
every place where the body repeats a value shows the hidden-pronunciation
mark, the title and aliases are hidden as before, and only title forms and
aliases are accepted as answers.

Implementation brief:

- Extend the existing chain named in Current decisions. Hide longer values
  first, as the existing fragment ordering does for titles.
- Add the cases to `RecallPromptSpellingStemMaskingControllerTest` (151 lines)
  and `RecallPromptAnswerSpellingControllerTest` (188 lines). Do not grow
  `ClozeDescriptionTest` (244 lines).
- The answer case passes before the change. It stays as proof that a
  pronunciation is not an accepted answer.

Sizing: the scratch change was about 25 production lines in three files and
four probe cases; the suite took 117 seconds. About five minutes of edits plus
the suite run.

Accepted proof, 2026-11-11: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
on the implementation diff gave 3,118 tests, none failed (10 minutes 52 seconds
with the host under heavy unrelated load). After the refactor pass, the six
classes that reach the changed chain were run again: 128 tests, none failed.
The observing cases are named per commitment in the
[source review](source-review.md).

Learnings:

- A blank `pronunciation` value is ignored. Without that, an empty value
  matched at every word boundary and the whole prompt became `/.../` marks.
  `spellingQuestionIgnoresABlankSavedPronunciation` observes it.
- A one-kana value is hidden inside other words, as the story's UI note says:
  `手を挙げて。` with `pronunciation: て` gives `[...]を挙げ/.../。`.
- Where a note has both the property and a `/…/` reading of the same text, a
  bare repeat of that text shows the hidden-pronunciation mark.

### 2. Add property offers pronunciation
Type: Behavior
Status: done
Proof: The mounted-editor observation in the table, the updated expected lists,
both frontend typechecks, and the property layout feature.

Behavior: Given a learner editing an ordinary note, when they open **Add
property**, the key choices include `pronunciation` with the key field empty,
and choosing it and adding `ただい` stores `pronunciation: ただい`.

Implementation brief:

- Add the key to the general list after `example of`.
- Update the expected lists in the four spec files the scratch change failed:
  `RichMarkdownEditor.propertyPresets.spec.ts` (6 tests),
  `RichMarkdownEditor.propertySlotPresets.spec.ts` (5),
  `RichMarkdownEditor.propertyRowEditing.spec.ts` (1) and
  `tests/utils/noteContentPropertyKeyPresets.spec.ts` (2).
  `propertyPresets.spec.ts` has 230 lines and states the full list six times;
  keep it under 250 lines, sharing that list if the additions would cross it.
- Update the key-choice paragraphs of `docs/note-content-saving.md`.

Sizing: one production line, list updates in four spec files, one added case
and a short documentation edit. About five minutes plus a 31-second focused
run, two typechecks and one feature run of about two and a half minutes; that
feature run is the focused-test wait.

Accepted proof, 2026-11-11, on the implementation diff: the focused frontend
run gave 150 files and 1,028 tests, none failed; both typechecks reported
nothing; `note_property_layout.feature` gave 12 passing. The observing cases
are named per commitment in the [source review](source-review.md).

Learning: **Add property** on a note without properties emits
`---\npronunciation: ただい\n---\n` directly followed by the body, with no
blank line between the closing fence and the body.

### 3. List what the conversion would change
Type: Behavior
Status: done
Proof: Run on the clone at commit `cc485736`, the listing reports 750 notes
with a match, 641 with exactly one match in plain kana, 56 with one match that
is not plain kana, 53 with several matches and none with a `pronunciation`
property. Run on the clone's current accepted head, it gives the candidates
and the unchanged notes with their reasons.

Behavior: Given a clone of notebook 15, when the listing runs, the owner can
read which notes are candidates and which notes with `/…/` stay unchanged and
why.

Implementation brief:

- Clone into a directory outside this repository and create the `conversion`
  branch there. Keep the script and the report outside both repositories.
- Recognize readings as Spelling review does today: take the body without
  frontmatter; treat Markdown link URLs as protected; look only at text
  outside HTML tags; match `/([^\s/][^/\n]*)/(?![a-zA-Z0-9_])` with Unicode
  whitespace; skip a match that overlaps a `[[…]]` link. Skip everything under
  `_trash/`.
- Plain kana means hiragana, katakana, the long-vowel mark, the middle dot and
  spaces.
- Reasons for staying unchanged: several matches; the match is not plain kana;
  the note already has `pronunciation`; the reading leads a sentence with no
  title before it and a particle after it, so removing it leaves the sentence
  without its subject (about 11 notes at `cc485736`).

Sizing: one small script and one report; about five minutes. The counts above
make a wrong recognizer visible at once.

Accepted proof, 2026-11-12: `python3 tools/conversion.py list notebook --rev cc485736`
printed 750, 641, 56, 53 and 0 on its first run. The clone's accepted head is
`cc485736fa2cab24e8776bd33a18558a3515e23d`, the commit of the measurement, so
both runs read the same tree. The listing has 630 candidates and 120 unchanged
notes: 53 with several matches, 56 not plain kana, 0 with the property, and 11
whose reading is the subject of its sentence.

Working directory, outside both repositories:
`/Users/terryyin/doughnut-pronunciation-conversion`, with the clone in
`notebook` (branch `conversion`, local `main` at the accepted head), the script
in `tools` and the listing in `preview/01-listing.md`.

Rule for "the reading is the subject of its sentence": before the reading,
back to the start of the body, a line break or a sentence end, there is only
spacing, Markdown marks and opening quotes or brackets; after it, past closing
marks and spaces, comes a particle (とは, って, は, が, も, を, に, の, で, と).
It selects 三叉路, さっぱり, 熟字訓, 薬味, 魚卵, 厳島, 松島, 土下座, 足利義満,
飛鳥時代 and 捉える.

Learnings for slices 4 to 8:

- The clone's Git configuration holds the owner's access token. Do not print
  it.
- A note is `<folders>/<title>.md`. 744 of the 750 notes have no final line
  break, so a conversion must not add one. In 553 the body starts with a blank
  line after the closing fence. There is no CRLF.
- The clone has no remote named `origin`; publication goes through the CLI.
- In 45 candidates the reading text also appears elsewhere in the body.

### 4. Convert readings that stand alone on a line or lead the body
Type: Behavior
Status: done
Proof: The mechanical check passes on both commits of this slice, and the
preview section for this kind lists every changed note.

Behavior: Given the candidates whose reading is alone on its line (about 190)
or leads the body with text after it (about 105 after the sentence-subject
notes are set aside), when the conversion runs, each note gains
`pronunciation: <reading>` in its existing frontmatter and its body loses the
reading with the emptied line or the spacing after it.

Implementation brief:

- First commit: ten of these notes that have memory trackers in production,
  found with the read-only lookup in the premises table. This is the pilot
  that slice 7 publishes. Second commit: the rest of this kind.
- The mechanical check, reused by slices 5, 6 and 7: for every changed file,
  the diff removes only the marked reading with its slashes and the wrapper
  that existed only to hold it (surrounding spacing, an emptied bracket, quote
  or bold pair, or an emptied line) and adds only one `pronunciation` line
  inside the existing frontmatter; the added value equals the removed reading;
  the frontmatter still parses as YAML with its `type` unchanged; the
  recognizer of slice 3 finds no reading left in the note; and the notes
  converted plus the notes listed as unchanged equal the notes with a match.

Sizing: one rule family for two positions, with the check written here. About
ten minutes; the check is the reason this slice is larger than the next two.

Accepted proof, 2026-11-12, in the clone on branch `conversion`:

- Pilot commit `06e5adbf8f704f7a2eb6cf0a24627901420abe65`: 10 notes, 5 of
  each position. Second commit `1b9fc990874754b557a74e15b0ef49bd89ee202c`:
  288 notes. In all 191 notes had the reading alone on its line and 107 had
  it at the start of the body.
- `python3 tools/conversion.py check notebook main..conversion` ends with
  "298 converted so far + 332 candidates still to convert + 120 unchanged =
  750" and "CHECK PASSED". It also passes on each commit alone. Run against a
  deliberately wrong, uncommitted edit of 算術 and コツ it failed with five
  named violations.
- `preview/02-changes.md` lists the 298 notes, the ten pilot notes marked.
- Local `main` is still `cc485736`; the clone's tree is clean.

Pilot notes and their production note IDs, kept with their tracker IDs in
`tools/pilot-notes.json`: 連濁 1486, 算術 9085, 名字 1523, 今度 2409, 体言 2010,
定義 1237, 三十路 6554, 合羽 6533, 穏やか 1725, 了見 9011. Each has a tracker
with a recall count above zero, and its production content equals the clone's
file at `cc485736`.

Decisions and learnings:

- Blank-line rule: when the removed line stood between two blank lines, or
  between the start of the body and a blank line, the one blank line after it
  is removed too. No other blank line changes.
- The check also accepts a comma and the spacing after a reading that was the
  first item in a bracket that holds more. Slice 5 needs it; no commit of
  slice 4 uses it.
- The check counts against the fixed commit `cc485736`, so it stays right
  after local `main` moves in slice 7. From then on the preview needs
  `--range cc485736..conversion`.
- 相当 is left for slice 6: text follows its leading reading with no spacing.
- The YAML parser of the check is the system Ruby. Whether Doughnut reads the
  values back the same is observed in production in slice 7.

### 5. Convert readings in brackets after a word
Type: Behavior
Status: done
Proof: The mechanical check passes on this slice's commit, the preview section
lists every changed note, and 水引 shows `水引は、…` with
`pronunciation: みずひき`.

Behavior: Given the candidates whose reading sits in brackets after a word,
alone (about 214) or as the first item (about 12), when the conversion runs,
the bracket pair that held only the reading is removed, or the reading and its
following comma are removed from a bracket that holds more.

Sizing: one rule for full-width and half-width brackets; about five minutes.

Accepted proof, 2026-11-12, in the clone on branch `conversion`:

- Commit `1551a951d9086a4140131f26e15e9ad6523dac1c`: 226 notes. In 215 the
  brackets held only the reading and the pair is removed; in 11 the reading
  was the first item and the reading with its comma is removed.
- `python3 tools/conversion.py check notebook main..conversion` ends with
  "524 converted so far + 106 candidates still to convert + 120 unchanged =
  750" and "CHECK PASSED". The check's rules were not changed.
- `preview/02-changes.md` has the second kind with its 226 notes beside the
  298 of the first. 水引 shows `水引は、…` with `pronunciation: みずひき`.
- Local `main` is still `cc485736`; the clone's tree is clean.

Learnings for slice 6:

- 貧困 is left: `**貧困**（ /ひんこん/ 、[英](…` has a space after the opening
  bracket and a space before the comma. The check accepts a comma only
  directly after the reading. Convert it as the other first-item notes are,
  with the check's comma rule also accepting that spacing around the reading.
- The 106 remaining candidates by position, as counted once by hand: 54 in a
  bold pair, 17 next to an HTML tag, 13 after a word with spacing on both
  sides, 7 after a colon, 5 after a word and spacing with text directly
  after (多大 is one), 5 directly after text, 3 alone in another pair, 相当
  and 貧困.

### 6. Convert the remaining candidates and complete the preview
Type: Behavior
Status: done
Proof: The mechanical check passes on this slice's commit; 多大 shows
`「多大」とは…` with `pronunciation: ただい` and keeps `甚大（じんだい）`; the
complete preview accounts for every note with a match as converted, by kind,
or unchanged, with its reason.

Behavior: Given the candidates not yet converted (reading directly after the
title, inside quotes, in bold, and other positions; about 100 notes), when the
conversion runs, each is converted by a rule for its position, or edited one
by one where no rule fits, and the preview is complete for the owner.

Sizing: about ten minutes. The notes left for one-by-one edits share no rule;
each is one small edit under the same check, so cutting this list further
would only divide it arbitrarily.

Accepted proof, 2026-11-12, in the clone on branch `conversion`:

- Commit `81f06fc829000d3736f4af87954a1f0fd54378c9`: 92 notes. 54 had the
  reading alone on its line in bold, 20 after a word without brackets, 15
  directly after the HTML tag `<p>`; 貧困 is converted as a first item in
  brackets; 相当 and 復讐 were edited one by one.
- Commit `4d8ecf5fd0af367d6ecfc8c56bbfb63de35cd0c6`: the nine notes with a
  label before the reading (一区切り, 学ぶ, 一斉, 日にち, 理不尽, 示唆, 経過,
  考慮, 都市), with the label removed together with the reading.
- `python3 tools/conversion.py check notebook main..conversion` ends with
  "625 converted so far + 5 candidates still to convert + 120 unchanged =
  750" and "CHECK PASSED". 多大 shows `「多大」とは…` with
  `pronunciation: ただい` and keeps `甚大（じんだい）`.
- The preview is `preview/00-start-here.md`, `01-listing.md` (125 unchanged
  notes with reasons) and `02-changes.md` (625 changed notes by kind).
  `tools/verify_preview.py` finds the two lists disjoint and together equal to
  the 750 notes with a match.
- Converted: 298 of the first kind, 227 of the second, 100 of the third.
  Unchanged: 53 several matches, 56 not plain kana, 0 with the property, 11
  subject of the sentence by the rule, 2 subject of the sentence found by
  reading (通訳 and 緒の「いとぐち」の意味の広がり), and 3 whose sentence says
  how the word is read (冷奴, 角立つの読み方, 省く).
- Local `main` is still `cc485736`; the clone's tree is clean.

Owner's decision, 2026-11-12. The checkpoint message asked about the twelve
candidates of G5 and recommended removing the label together with the reading
in the nine label notes and leaving the three "と読みます" notes unchanged. The
owner answered "go ahead". The coordinator read that as agreement with both
recommendations, as the owner's request for these notes one by one. The same
answer is the go-ahead of slice 7's checkpoint.

Decisions and learnings:

- The check's comma rule also accepts spacing around a reading that is the
  first item in a bracket; 貧困 needed it.
- A defect in the check was corrected: a hyphen had counted as spacing, so a
  line `--------` counted as blank. The check is now stricter, passes on all
  four commits, and the listing still prints 750, 641, 56, 53 and 0.
- `preview/00-start-here.md` is written by hand. Its numbers need an update
  when a later commit changes them.
- For the nine label notes alone, the check accepts the loss of the listed
  label directly before the reading. Any other note that loses a label fails.

### 7. Publish a ten-note pilot and confirm learning history
Type: Behavior
Status: done
Proof: The publish output names the pilot commit as the accepted head, and for
each pilot note the production `note-info` and each tracker's recall history,
read before and after, are equal.

Human checkpoint, before any step below: the owner has read the complete
preview. One go-ahead covers a [Dough Land](../../../.agents/skills/dough-land/SKILL.md)
`--keep-worktree` landing of this story branch on `main`, the application
release from that landed revision under the `release-application` skill, and
the publications of slices 7 and 8. An owner who prefers to release or publish
themselves says so here, and the slice waits for their word that it is done.

Behavior: Given production running the release that contains slice 1, when
the pilot commit is published, the ten notes show their converted content and
keep the same memory trackers and recall history.

Implementation brief:

- Land with `--keep-worktree`, then release. The release keeps its own CI and
  smoke gates; waiting for them is this slice's external wait.
- Read and keep, for the ten pilot notes, `note-info` and each tracker's
  recall history.
- Run `donut notebook pull` in the clone. If it rebased the conversion
  commits, run the mechanical check again.
- Move local `main` to the pilot commit and run `donut notebook publish`.
- Read the same facts again and compare tracker IDs, types, `recallCount`,
  `lastRecalledAt`, `nextRecallAt`, `assimilatedAt`, `removedFromTracking`
  and the recall history entries.

Progress, 2026-11-12:

- The owner gave the go-ahead ("go ahead") after the checkpoint message.
- The story branch was landed on `main` with the worktree kept: `main` is at
  `12288107bd2a2dbbfc2eabc2d00f44e0d4ed4e74`. Trunk CI run 43126110847 passed
  for that commit in every job, with its packaging job.
- The owner then authorized the release tag and the publication. Release
  `v1.3.72` of `12288107bd`: Application Release run 43127613078 succeeded in
  admission and in "GCP deploy (GCS + MIG + health probe)"; the production
  health check reports commit `12288107bd2a2dbbfc2eabc2d00f44e0d4ed4e74`.
- `donut notebook pull` received a new accepted head, `a8de2940`, one web
  edit by the owner that touches no converted note. The five conversion
  commits were rebased onto it: pilot `418fc3e6fb8339194ab2c40182179d0f4e4ffb40`,
  then `d42ff734`, `1dee2ce5`, `db796694` and the tip
  `2787ae83b5fd09b225b0fc1b496e6ef44f30b203`. The listing prints the same
  counts on the new head, and the check with `--base main` passes on the range
  and on the pilot commit.
- `donut notebook publish notebook` printed "Published notebook. Accepted
  head: 418fc3e6fb8339194ab2c40182179d0f4e4ffb40." It took 23 seconds.
- Comparison of the ten pilot notes, read 13 seconds before the publish and
  about a minute after it: the same note IDs, the same 23 memory trackers with
  equal fields, and the same 59 recall history entries. Each note's production
  content equals its file in the pilot commit. Accepted proof of this slice.

Learnings for slice 8:

- Pull and publish need `main` checked out in the clone.
- The publish created no memory tracker for the new property. Whether the
  property is offered at assimilation time was not observed.
- `tools/pilot_snapshot.py` and `tools/pilot_compare.py` serve the twenty-note
  comparison as well.

A refused publication changes nothing; correct the named file and publish
again. Any difference in the comparison stops slice 8 and returns to planning
with the difference as evidence.

### 8. Publish the remaining conversion and confirm
Type: Behavior
Status: planned
Proof: The publish output names the last conversion commit as the accepted
head; a fresh download of the notebook shows the converted content; for twenty
converted notes spread over the three kinds, `note-info` read before and after
is equal as in slice 7; the story's diff holds no conversion code.

Behavior: Given an equal pilot comparison, when the remaining conversion
commits are published, every converted note shows its `pronunciation` property
and converted body in production and keeps its learning history.

Implementation brief:

- Read `note-info` for the twenty notes before publishing.
- Move local `main` to the tip of `conversion` and publish. If the publication
  is refused for its size or ends without a clear answer, run publish again; a
  repeated publication of an accepted head changes nothing. If it keeps
  failing, publish the commits one at a time, and split a commit that is
  still too large into smaller commits published in order.
- Tell the owner to run `donut notebook pull` in their own checkout.
- Delete the script and the report. Record in this plan the counts by kind,
  the commit IDs, the accepted heads and the comparison results.

## Execution record

Story Branch Mode. Execution checkout
`.worktrees/save-pronunciations-as-note-properties`, branch
`claude/save-pronunciations-as-note-properties`, remote `origin`, trunk
`main`. The claim `15e3e95d5e` was accepted on `main` on 2026-11-11 and is the
published base of the first increment. Replanning permission: the existing
planning authority; no option was given.

Published on `origin/claude/save-pronunciations-as-note-properties`: slice 1
as `e9f109c3c2`, slice 2 as `3f43ec8853`, slice 3 as `588f51ccc2`, slice 4 as
`550401a5dd`, slice 5 as `4e88d3d2ad`, the slice 6 evidence as `86f30bee22`,
the CI record as `aa17aab044`.
CI observer directory:
`/tmp/dough-ci-501/watch-gNNSef`.

CI on the story branch, 2026-11-11:

- Run 43049319481 for `e9f109c3c2` and run 43053073660, attempt 1, for
  `3f43ec8853` each had eleven failed jobs. Every one stopped in its
  dependency installation with `Temporary failure resolving
  'azure.archive.ubuntu.com'` from `apt-get`; no test ran in them. The backend
  unit test job, which does not install those packages, passed in both runs.
  Trunk CI had passed at `5d974f8eae` earlier the same day with the same
  setup. This was a runner network failure, so no repair was made.
- Run 43053073660 was run again once the two runs had ended. In attempt 2 ten
  of the eleven jobs passed. The job for
  `e2e_test/features/note_creation_and_update/**` ended with "The hosted
  runner lost communication with the server" during its dependency setup,
  before any test, and left no log.
- That one job was run again. Attempt 3 passed, so run 43053073660 is
  successful for `3f43ec8853`, the latest revision that changes product files.
  The later revisions on the branch change planning records only and start no
  CI run.
- The CI observer ended by itself while attempt 2 was running. Attempt 3 was
  read directly with `gh run view`.

## Story obligations

### G1. About 27 readings with extra characters stay unchanged
Reported: slice 3 — "about 27 real readings with something extra inside the slashes. They stay unchanged under the rule as written."
Story clause: "when its match is not plain kana"
Disposition: excluded "The owner may edit such notes by hand in the checkout, or ask for them one by one, before publishing."

### G2. 音 has three matches, not the two the story names
Reported: slice 3 — "音 has three matches, not the two the story names. Same reason, same outcome."
Story clause: "**音** has `/ね/` and `/おん/` (several readings)"
Disposition: no user cost "The owner's existing readings in the 日本語学習 notebook move into the same property": 音 is listed unchanged under several matches with two or with three matches, and no conversion touches it.

### G3. Two agent-guidance files are among the 750 notes
Reported: slice 3 — "Two agent-guidance files are among the 750: AGENTS.md and .agents/donut-format.md."
Story clause: "750 notes have at least one `/…/` match"
Disposition: no user cost "The owner's existing readings in the 日本語学習 notebook move into the same property": both files are listed unchanged because their matches are not plain kana, so the conversion leaves them as they are.

### G4. 乗り越す has two line-break tags in a row after conversion
Reported: slice 4 — "Its reading was alone on a line between two `<br>` lines, so it was converted by the rule and the two `<br>` now follow each other. Removing a `<br>` would touch other text, so the rule cannot do better."
Story clause: "The body loses the marked reading together with the spacing or the bracket pair that existed only to hold it."
Disposition: excluded "All other text stays, including readings of other words."

### G5. Twelve candidates stay unchanged for reasons the story does not name
Reported: slice 6 — "12 candidates are unconverted for two reasons the story does not name"
Story clause: "A note is converted when its body has exactly one `/…/` reading as Spelling review recognizes it today, that reading is written only in kana, and the note has no `pronunciation` property."
Disposition: no user cost "Spelling review still never shows that reading while it asks for the title": the owner agreed on 2026-11-12 to convert the nine label notes with their labels removed and to leave 冷奴, 角立つの読み方 and 省く unchanged; those three keep their `/…/` reading, which stays hidden as today, and are listed with their reason in the preview.

### G6. Some converted notes look odd where the reading stood
Reported: slice 6 — "Changed notes where the result may look odd"
Story clause: "The body loses the marked reading together with the spacing or the bracket pair that existed only to hold it."
Disposition: excluded "All other text stays, including readings of other words."

## Decisive premises and observations

Observations were local and unpaid, or read-only against production with the
owner's CLI login, on 2026-11-11. The worktree was this preparation's owned
workspace at `abfcfc6dde`; every scratch edit was reverted before this plan
was written, and `git status --short` then showed only the seed.

| Premise and consuming operation | Observed form, result and limits |
| --- | --- |
| Passing `pronunciation` values through the existing chain hides them at the controller, as slice 1 needs | Scratch edit of `ClozeReplacement`, `ClozedString` and `Note`, with four probe cases. `CURSOR_DEV=true nix develop -c pnpm backend:test_only` in this worktree's isolated test database: 3,115 tests, none failed. The probes passed: scalar value in brackets, two-item list, a `/…/` reading without the property, and reading refused while the title is accepted. The refusal probe passes without the change. |
| No other backend behavior reads the changed chain differently | The same full run: every existing case passed with the scratch change. |
| Adding the key to the general list reaches only the list consumers, as slice 2 needs | Scratch edit adding `pronunciation` after `example of`. `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/utils tests/components/form tests/notes`: 14 tests failed in the four spec files named in slice 2, each on a stated list or its length; 1,013 passed; 31 seconds. |
| The longer list keeps the key panel layout | Same scratch edit. `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_topology/note_property_layout.feature` in this worktree's own stack: 12 passing; 140 seconds in all. |
| Today's reading rule can be reproduced outside the product, as slice 3 needs | Copies of `HtmlOrMarkdown` and the pattern, wiki-link and URL guards from `abfcfc6dde`, compiled alone with JDK 24 and run over a clone of notebook 15 at `cc485736`: 750, 641, 56 and 53, the counts in slice 3. These classes work on text only, so running them outside the build cannot change the counts. A first count that ignored the HTML rule gave 797, which is why the rule is spelled out in slice 3. |
| A locally published content edit keeps a note's memory trackers, as slices 7 and 8 need | `NotebookGitExistingNoteBatchPublicationControllerTest` publishes an edits-only revision onto learned notes and keeps their trackers; it ran in the full backend run above and in trunk CI at `4e328b86ea` (success, 2026-11-11). This is the test profile, not production. The remainder is the pilot in slice 7, which is owner-held and changes production. |
| Production can show a note's memory trackers before and after, read-only | With the owner's CLI login: `POST /api/notes/search` for the title 多大 returned its note in notebook 15, and `GET /api/notes/{note}/note-info` returned three memory trackers with `id`, `type`, `recallCount`, `lastRecalledAt`, `nextRecallAt`, `assimilatedAt` and `removedFromTracking`. `GET /api/memory-trackers/{memoryTracker}/recall-history` returned that tracker's nine entries. |
| The installed CLI can clone notebook 15 on this machine, as slice 3 needs | `donut notebook clone 15 <directory>` with CLI 0.5.28: 31 seconds, 60 MB, head `cc485736`, with the notebook binding recorded in the clone's Git configuration. |
| How many changed notes one publication accepts | Not observed, and not decisive: slice 8 publishes in smaller parts when a publication is refused or unclear, and a refused publication changes nothing. Existing proof covers three small edits in a large notebook (`cli_notebook_large_publication.feature`); the notebook's history holds an 828-file Donut System commit from 2026-10-02 made on the server path. |

## Consumer trace and verification

The scratch runs above located the consumers: the backend suite found none
beyond the edited chain, and the frontend run found the four spec files. At
execution, run the installed consumer trace on each actual diff and give every
listed consumer a disposition:

```sh
node .agents/skills/dough-execute-plan/scripts/consumer-trace.mjs --checkout <checkout> --base HEAD --term 'pronunciation' --term 'Hidden pronunciation'
```

Execution verification:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/utils tests/components/form tests/notes
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_topology/note_property_layout.feature
```

The backend suite is the backend skills' local check. The frontend run and
both typechecks follow the frontend proof rule. The feature run is included
because the key panel's layout observes the list this story lengthens. No API
contract changes, so no client generation is needed. Slices 3 to 8 change no
product file; their proof is the listing, the mechanical check and the
production reads named in each slice.

Host for the preparation runs: `Territory.local`, load about 5 to 7 with
unrelated work running. The backend, frontend and feature runs were run one
after another.

## Plan review

Slices 1 and 2 extend one existing rule each. Slices 4 to 6 are one conversion
rule applied by position, split by kind of change because the story previews
and reviews changes by kind; slice 7 is the probe that bounds the two premises
only production can settle. Slice-plan refinement was not needed: each slice
has one outcome and one proof loop, and no slice-specific concern remained
after the scratch runs. This preparation records planned work and grants no
execution authority.
