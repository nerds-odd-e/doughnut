# Source review: Save pronunciations as note properties

**Launch.** 2026-11-11, before the first implementation delegation. A fresh
general-purpose agent without conversation inheritance, fork or resume, told
to use no tools, read no project files and change nothing.

**Input.** Only the story section
[Save pronunciations as note properties](../../seeds/SEED-124-save-pronunciations-as-note-properties.md#save-pronunciations-as-note-properties)
at `15e3e95d5e`, from **Goal** to the end of **Architecture**, with the ADR
links reduced to their names. No plan, product, proof or slice scope was
given. No human amendment existed.

**Return.** The reviewer returned the 137 ids below with their kind and source
text. The Slice and last columns are the coordinator's allocation and
acceptance record.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | commitment: Goal: a language learner records how a note's title is read in a `pronunciation` property instead of marking it inside the note text. | slice 2 | observed: RichMarkdownEditor.propertyPresets.spec.ts “adds a pronunciation chosen from the presets as an ordinary property”: the emitted Markdown is `---\npronunciation: ただい\n---\n数や量が非常に大きいこと。` |
| 2 | commitment: Goal: Spelling review still never shows that reading while it asks for the title. | slice 1 | observed: RecallPromptSpellingStemMaskingControllerTest.spellingQuestionHidesSavedPronunciationWhereTheBodyRepeatsIt: the stem shows `[...]（/.../）` for 多大（ただい） |
| 3 | commitment: Goal: the owner's existing readings in the 日本語学習 notebook move into the same property. | slice 8 | |
| 4 | commitment: Goal: those notes keep their learning history. | slice 8 | |
| 5 | qualification: This serves the near-future direction of better note quality: the reading becomes named data, where today it is a `/…/` marking in prose that Spelling review has to recognize by pattern. | slice 8 | |
| 6a | commitment: When a learner edits an ordinary note, Add property lists `pronunciation` among its key choices. | slice 2 | observed: RichMarkdownEditor.propertyPresets.spec.ts “adds a pronunciation chosen from the presets as an ordinary property”: the listed choices on an ordinary note include `pronunciation` |
| 6b | commitment: No choice is preselected. | slice 2 | observed: RichMarkdownEditor.propertyPresets.spec.ts “adds a pronunciation chosen from the presets as an ordinary property”: the draft key input is empty when Add property opens |
| 6c | commitment: No note receives the property unless someone adds it. | slice 2 | observed: Reading of the story diff: `pronunciation` occurs in frontend/src only in the key choice list; nothing writes it without the learner's Add |
| 7a | commitment: Choosing it adds an ordinary YAML property, for example `pronunciation: ただい`. | slice 2 | observed: RichMarkdownEditor.propertyPresets.spec.ts “adds a pronunciation chosen from the presets as an ordinary property”: choosing the key and adding ただい emits `pronunciation: ただい` inside the frontmatter |
| 7b | commitment: The value is plain text. | slice 2 | observed: RichMarkdownEditor.propertyPresets.spec.ts “adds a pronunciation chosen from the presets as an ordinary property”: the value is stored as typed, unquoted |
| 7c | commitment: As with other ordinary properties, a YAML list holds several readings. | slice 1 | observed: RecallPromptSpellingStemMaskingControllerTest.spellingQuestionHidesEveryValueOfAHandAuthoredPronunciationList and …HidesALongerSavedPronunciationBeforeAShorterOneItStartsWith: both list values hidden |
| 7d.1 | commitment: A property typed by hand behaves the same as one added through Add property. | slice 1 | observed: RecallPromptSpellingStemMaskingControllerTest.spellingQuestionHidesEveryValueOfAHandAuthoredPronunciationList: a block list typed as YAML text is hidden like a scalar |
| 7d.2 | commitment: A property received through Git publication behaves the same as one added through Add property. | slice 1 | observed: Note.createMaskedContentForRecall passes only getContent() to FrontmatterPronunciation.fromNoteContent; no code path sees where the content came from |
| 8a | commitment: In Spelling review of that note, every place where the note body repeats a `pronunciation` value is hidden. | slice 1 | observed: RecallPromptSpellingStemMaskingControllerTest.spellingQuestionHidesSavedPronunciationWhereTheBodyRepeatsIt and …HidesEveryValueOfAHandAuthoredPronunciationList: every repeat is `/.../` |
| 8b | commitment: The prompt is the note body without its properties, so the property itself is not shown either. | slice 1 | observed: RecallPromptSpellingStemMaskingControllerTest.spellingQuestionHidesSavedPronunciationWhereTheBodyRepeatsIt: the exact stem equality holds no property text |
| 9a | commitment: The expected answer stays the note title, in the forms accepted today. | slice 1 | observed: Note.matchAnswer is not in the slice diff; RecallPromptAnswerSpellingControllerTest.shouldValidateTheAnswer, …answerOneOfTheFrontmatterAliases and NoteTitleTest stay green in the full backend run |
| 9a.1 | commitment: Accepted form: the title. | slice 1 | observed: RecallPromptAnswerSpellingControllerTest.savedPronunciationIsNotAnAcceptedAnswer (多大 is correct) and …shouldValidateTheAnswer |
| 9a.2 | commitment: Accepted form: its part before trailing brackets. | slice 1 | observed: NoteTitleTest.matchesForRecall_acceptsTitleWithoutTrailingParenthesizedContent, unchanged and green |
| 9a.3 | commitment: Accepted form: its aliases. | slice 1 | observed: RecallPromptAnswerSpellingControllerTest.answerOneOfTheFrontmatterAliases, unchanged and green |
| 9b | commitment: Being a `pronunciation` value does not make a text an accepted answer. | slice 1 | observed: RecallPromptAnswerSpellingControllerTest.savedPronunciationIsNotAnAcceptedAnswer: ただい is incorrect on a note with the property |
| 9c | qualification: Spelling review trains the written form, and accepting the reading would pass a learner who did not recall it. | slice 1 | observed: Reason for 9b; observed with 9b in RecallPromptAnswerSpellingControllerTest.savedPronunciationIsNotAnAcceptedAnswer |
| 10a | commitment: `pronunciation` is otherwise an ordinary property. | slice 2 | observed: Reading of the story diff: PropertyKeyNaming and frontend/src/utils/noteContentPropertyKeys.ts are unchanged; no key-specific rule exists |
| 10a.1 | commitment: It is shown with the note's other properties. | slice 2 | observed: No key-specific display code exists; RichMarkdownEditor.readOnlyProperties.spec.ts and propertyEntry.spec.ts hold for every ordinary key and pass |
| 10a.2 | commitment: It is edited with the note's other properties. | slice 2 | observed: No key-specific editing code exists; RichMarkdownEditor.propertyRowEditing.spec.ts holds for every ordinary key and passes; its row key field lists `pronunciation` |
| 10a.3 | commitment: It is published in the note's frontmatter. | slice 2 | observed: RichMarkdownEditor.propertyPresets.spec.ts “adds a pronunciation chosen from the presets as an ordinary property”: the property is emitted inside the note's frontmatter fence, the content that Git publication carries |
| 10a.4 | commitment: It is offered in the assimilation sequence as its own unit, as every ordinary property is. | slice 2 | observed: Reading of the story diff: PropertyKeyNaming and the frontend structural-key lists are unchanged, so the key is an ordinary property unit |
| 10b | qualification: The owner confirmed this on 2026-11-11. | slice 2 | observed: Reason for 10a; the owner's confirmation is recorded in the story |
| 11a | commitment: Scope limit: Spelling review keeps hiding `/…/` readings written in the note body exactly as it does today. | slice 1 | observed: ClozeDescriptionTest (82 unchanged cases) and the 8 earlier RecallPromptSpellingStemMaskingControllerTest cases green; RecallPromptSpellingStemMaskingControllerTest.spellingQuestionHidesBothASavedPronunciationAndASlashReadingInTheBody |
| 11b | qualification: Notes that are not converted depend on it. | slice 1 | observed: Reason for 11a; the `/…/` pass in ClozeReplacement.maskPronunciationsAndTitles is unchanged |
| 12a | commitment: The conversion runs in the owner's own checkout of the notebook. | slice 3 | observed: `donut notebook clone 15` with the owner's CLI login on the owner's machine printed “Cloned notebook 15”; the clone is on branch `conversion` at `cc485736` |
| 12b | commitment: An agent, or a script that is not kept, edits the checkout (stated as alternatives). | slice 3 | observed: The script is `conversion.py` in a tools directory outside both repositories; `git status --short` is empty in the clone and in this worktree |
| 12c | exclusion: No conversion code enters this repository. | slice 8 | |
| 12d | qualification: The owner chose this on 2026-11-11. | slice 3 | observed: Reason for 12a and 12b; the owner's choice is recorded in the story |
| 13a | commitment: Before anything is published, the owner gets a preview. | slice 6 | observed: Preview files `00-start-here.md`, `01-listing.md` and `02-changes.md` exist; the clone's local `main` is still `cc485736` and nothing was published |
| 13b.1 | commitment: Preview, for each note that would change: its title. | slice 6 | observed: `02-changes.md`: each of the 616 entries starts with the note's title and path; `verify_preview.py` finds its paths equal to the 616 changed files |
| 13b.2 | commitment: Preview, for each note that would change: the reading. | slice 6 | observed: `02-changes.md`: each entry gives the reading and the new property line |
| 13b.3 | commitment: Preview, for each note that would change: the changed text before and after. | slice 6 | observed: `02-changes.md`: each entry gives the changed text before and after |
| 13c | commitment: The changing notes are grouped by the kind of change so that similar changes can be checked together. | slice 6 | observed: `02-changes.md`: three kinds of change with nine sub-groups (298, 227 and 91 notes) |
| 13d.1 | commitment: The preview includes a list of the notes that contain `/…/` but would stay unchanged. | slice 3 | observed: listing `01-listing.md`, section “Notes that stay unchanged”: 120 notes |
| 13d.2 | commitment: Each note in that list is given with the reason. | slice 3 | observed: listing `01-listing.md`: the unchanged notes are grouped under four reason headings (53, 56, 0 and 11 notes) |
| 13e | commitment: Git's diff of the checkout shows the exact changes. | slice 6 | observed: `git diff --shortstat cc485736 conversion` in the clone: 616 files changed, 987 insertions, 802 deletions |
| 14a | commitment: A note is converted when all three conditions 14a.1 to 14a.3 hold. |  | G5 |
| 14a.1 | qualification: Condition: its body has exactly one `/…/` reading as Spelling review recognizes it today. | slice 3 | observed: listing `01-listing.md` at `cc485736`: 750 notes with a match, 53 with several; the recognizer reproduces the story's measurement |
| 14a.2 | qualification: Condition: that reading is written only in kana. | slice 3 | observed: listing `01-listing.md`: 641 with exactly one match in plain kana, 56 with one match that is not plain kana |
| 14a.3 | qualification: Condition: the note has no `pronunciation` property. | slice 3 | observed: listing `01-listing.md`: 0 notes with a `pronunciation` property; the fixture note 見本 with the property is listed unchanged |
| 14b | commitment: The reading becomes the `pronunciation` value. | slice 4 | observed: Mechanical check on `06e5adbf` and `1b9fc990`: for all 298 changed notes the YAML parser reads `pronunciation` back as exactly the removed reading |
| 14c.1 | commitment: The body loses the marked reading. | slice 4 | observed: Mechanical check on both commits: the recognizer finds no reading left in any of the 298 notes |
| 14c.2 | commitment: Together with the reading, the body loses the spacing that existed only to hold it (alternative to 14c.3). | slice 4 | observed: Clone diff of 算術 in `06e5adbf`: `/さんじゅつ/ 算术` becomes `算术`; preview `02-changes.md`: 連濁 loses the reading's line and the empty line after it |
| 14c.3 | commitment: Or, together with the reading, the body loses the bracket pair that existed only to hold it (alternative to 14c.2). | slice 5 | observed: Mechanical check on `1551a951`: all 226 notes lost only the reading and the wrapper that held it; preview `02-changes.md`: 求める `「求める(/もとめる/)」とは、` becomes `「求める」とは、` |
| 14d | commitment: All other text stays. | slice 6 | observed: Mechanical check on `cc485736..conversion`: all 616 notes lost only the reading and the wrapper that held it; `verify_preview.py` finds no other removed text |
| 14d.1 | commitment: The text that stays includes readings of other words. | slice 6 | observed: Clone file 漢字/多/多大.md at `81f06fc8` still holds `甚大（じんだい）` |
| 15a | commitment: A note stays unchanged and is listed when any one of the cases 15a.1 to 15a.4 applies. | slice 3 | observed: listing `01-listing.md`: 630 candidates and 120 unchanged notes add up to the 750 notes with a match |
| 15a.1 | commitment: Case: it has several `/…/` matches. | slice 3 | observed: listing `01-listing.md`, reason “several matches”: 53 notes |
| 15a.2 | commitment: Case: its match is not plain kana. | slice 3 | observed: listing `01-listing.md`, reason “the match is not plain kana”: 56 notes |
| 15a.3 | commitment: Case: it already has a `pronunciation` property. | slice 3 | observed: listing `01-listing.md`, reason “the note already has a pronunciation property”: 0 notes; fixture note 見本 is listed with this reason |
| 15a.4 | commitment: Case: removing the reading would leave a sentence without its subject. | slice 3 | observed: listing `01-listing.md`, reason “the reading is the subject of its sentence”: 11 notes, with the rule stated in the plan |
| 15b.1 | commitment: The owner may edit such notes by hand in the checkout before publishing (alternative to 15b.2). | slice 7 | observed: The preview's start-here file told the owner the unchanged notes can be edited by hand in the copy; the owner asked for no hand edit before the go-ahead |
| 15b.2 | commitment: Or the owner may ask for them one by one, before publishing (alternative to 15b.1). | slice 7 | observed: The owner's go-ahead of 2026-11-12 took the nine label notes one by one; they are commit `4d8ecf5f`, later `2787ae83` |
| 16a | commitment: The owner publishes with `donut notebook publish`, the normal local publication. | slice 7 | observed: `donut notebook publish notebook` printed “Published notebook. Accepted head: 418fc3e6fb8339194ab2c40182179d0f4e4ffb40.” |
| 16b | commitment: Nothing changes in production until that publication is accepted. | slice 7 | observed: The before snapshot of the ten pilot notes, taken 13 seconds before the publish, still showed the `/reading/` text and no property; the clone's local `main` had not left the accepted head until then |
| 17a | commitment: Converted notes are the same notes. | slice 8 | |
| 17a.1 | commitment: Their memory trackers stay attached. | slice 8 | |
| 17a.2 | commitment: Their recall history stays attached. | slice 8 | |
| 17b | commitment: This is checked in production on converted notes after publishing. | slice 8 | |
| 18a | qualification: Measurement basis: measured on 2026-11-11 from 日本語学習's accepted Git history at commit `cc485736`, with the text rule Spelling review applies today, notes outside trash. | slice 3 | observed: Listing run with `--rev cc485736`, notes under `_trash/` skipped, printed “Read from: cc485736fa2c…” |
| 18b | qualification: 750 notes have at least one `/…/` match. | slice 3 | observed: Same run: 750 notes with at least one match |
| 18c | qualification: 641 have exactly one match in plain kana, which makes them the candidates for conversion. | slice 3 | observed: Same run: 641 with exactly one match in plain kana |
| 18d | qualification: 56 have one match that is not plain kana. | slice 3 | observed: Same run: 56 with one match that is not plain kana |
| 18e | qualification: 53 have several matches. | slice 3 | observed: Same run: 53 with several matches |
| 18f | qualification: No note has a `pronunciation` property. | slice 3 | observed: Same run: 0 with a `pronunciation` property |
| 18g | qualification: As ordinary properties, the converted readings add up to 641 units to the owner's assimilation sequence. | slice 8 | |
| 19 | qualification: Deferred items are not delivered here, and not prevented. | slice 8 | |
| 20a | exclusion: Deferred: converting other notebooks. | slice 8 | |
| 20b | qualification: The owner's English notebook has 57 notes with `/…/` matches, mostly IPA; Spanish and 中文 have at most one each. | slice 8 | |
| 20c | exclusion: Notebooks of other owners are not converted and were not counted, by the owner's decision. | slice 8 | |
| 20d | commitment: Their `/…/` readings stay hidden as today. | slice 1 | observed: The `/…/` pass in ClozeReplacement.maskPronunciationsAndTitles is unchanged and applies to every note; ClozeDescriptionTest green |
| 21a | exclusion: Deferred: removing Spelling review's handling of `/…/` in the note body. | slice 1 | observed: ClozeReplacement keeps its PRONUNCIATION matcher; ClozeDescriptionTest unchanged and green |
| 21b | qualification: In 日本語学習 about 90 of the 750 notes have a match that is not a plain kana reading. | slice 3 | observed: Same run: 91 notes with at least one match that is not plain kana |
| 21c | qualification: The sampled ones are parts of web addresses written as plain text and slash-separated alternatives or translation pairs, which today's rule hides as if they were readings. | slice 3 | observed: listing `01-listing.md`, reason “the match is not plain kana”: about 17 web address parts, 2 file path parts, about 10 alternatives or translation pairs, and about 27 readings with extra characters (G1) |
| 21d | qualification: Removal needs every notebook that relies on the rule to be converted first. | slice 1 | observed: Reason for 21a; nothing was removed |
| 22a | exclusion: Deferred: rewriting by rule a sentence whose subject is the reading itself, for example `/あしかが よしみつ/は、室町時代…`, which about 11 of the candidates do. | slice 3 | observed: listing `01-listing.md`: the 11 notes whose reading is the subject of its sentence are listed unchanged; no rule rewrites them |
| 22b | qualification: Putting the title in its place was considered. It adds text the author did not write, so the rule leaves those notes to the owner. | slice 3 | observed: Reason for 22a; those 11 notes are left to the owner |
| 23a | exclusion: Deferred: accepting the reading as an answer. | slice 1 | observed: RecallPromptAnswerSpellingControllerTest.savedPronunciationIsNotAnAcceptedAnswer; Note.matchAnswer is not in the slice diff |
| 23b | exclusion: Deferred: a review that asks for the reading. | slice 1 | observed: Reading of the slice diff: ClozeReplacement, ClozedString, Note and FrontmatterPronunciation only; no question type added |
| 24 | exclusion: Deferred: recording readings of other words that appear in a note body. | slice 6 | observed: `verify_preview.py`: all 616 converted notes gained exactly one line, `pronunciation` with the note's own reading |
| 25a | exclusion: Considered and not chosen for the conversion: a tested temporary script kept in this repository. | slice 8 | |
| 25b | exclusion: Considered and not chosen for the conversion: a temporary startup migration in production over every notebook, as the numbered-property consolidation did on 2026-10-02. | slice 8 | |
| 25c | qualification: The second (startup migration) would change other owners' prose without their review. | slice 8 | |
| 26a | example: Trigger: a learner edits the note 多大 and opens Add property. | slice 2 | observed: RichMarkdownEditor.propertyPresets.spec.ts “adds a pronunciation chosen from the presets as an ordinary property”: Add property opened on an ordinary note shows an empty key and `pronunciation` among the choices |
| 26b | example: Expected: they see `pronunciation` among the key choices. | slice 2 | observed: RichMarkdownEditor.propertyPresets.spec.ts “adds a pronunciation chosen from the presets as an ordinary property”: Add property opened on an ordinary note shows an empty key and `pronunciation` among the choices |
| 26c | example: Expected: none is preselected. | slice 2 | observed: RichMarkdownEditor.propertyPresets.spec.ts “adds a pronunciation chosen from the presets as an ordinary property”: Add property opened on an ordinary note shows an empty key and `pronunciation` among the choices |
| 26d | example: Trigger: they choose it, enter `ただい`, and add it. | slice 2 | observed: RichMarkdownEditor.propertyPresets.spec.ts “adds a pronunciation chosen from the presets as an ordinary property”: selecting it, entering ただい and clicking Add emits `pronunciation: ただい` |
| 26e | example: Expected: the note now stores `pronunciation: ただい`. | slice 2 | observed: RichMarkdownEditor.propertyPresets.spec.ts “adds a pronunciation chosen from the presets as an ordinary property”: selecting it, entering ただい and clicking Add emits `pronunciation: ただい` |
| 27a | example: Setup: 多大 has `pronunciation: ただい` and the body `数や量が非常に大きいこと。「多大（ただい）なるご迷惑」`. | slice 1 | observed: RecallPromptSpellingStemMaskingControllerTest.spellingQuestionHidesSavedPronunciationWhereTheBodyRepeatsIt: this content, stem `数や量が非常に大きいこと。「[...]（/.../）なるご迷惑」` |
| 27b | example: Expected: its Spelling prompt hides 多大. | slice 1 | observed: RecallPromptSpellingStemMaskingControllerTest.spellingQuestionHidesSavedPronunciationWhereTheBodyRepeatsIt: this content, stem `数や量が非常に大きいこと。「[...]（/.../）なるご迷惑」` |
| 27c | example: Expected: its Spelling prompt hides ただい. | slice 1 | observed: RecallPromptSpellingStemMaskingControllerTest.spellingQuestionHidesSavedPronunciationWhereTheBodyRepeatsIt: this content, stem `数や量が非常に大きいこと。「[...]（/.../）なるご迷惑」` |
| 27d | example: Expected: answering `多大` is correct. | slice 1 | observed: RecallPromptAnswerSpellingControllerTest.savedPronunciationIsNotAnAcceptedAnswer: 多大 is correct |
| 27e | example: Expected: answering `ただい` is incorrect. | slice 1 | observed: RecallPromptAnswerSpellingControllerTest.savedPronunciationIsNotAnAcceptedAnswer: ただい is incorrect |
| 28a | example: Setup: 顔色 has `pronunciation` with the two values `かおいろ` and `がんしょく`. | slice 1 | observed: RecallPromptSpellingStemMaskingControllerTest.spellingQuestionHidesEveryValueOfAHandAuthoredPronunciationList: stem `[...]（/.../）が悪い。[...]（/.../）を失う。` |
| 28b | example: Expected: its Spelling prompt hides `かおいろ` wherever the body repeats it. | slice 1 | observed: RecallPromptSpellingStemMaskingControllerTest.spellingQuestionHidesEveryValueOfAHandAuthoredPronunciationList: stem `[...]（/.../）が悪い。[...]（/.../）を失う。` |
| 28c | example: Expected: its Spelling prompt hides `がんしょく` wherever the body repeats it. | slice 1 | observed: RecallPromptSpellingStemMaskingControllerTest.spellingQuestionHidesEveryValueOfAHandAuthoredPronunciationList: stem `[...]（/.../）が悪い。[...]（/.../）を失う。` |
| 29a | example: Setup: a note has `/じゅわ/` in its body and no `pronunciation` property. | slice 1 | observed: ClozeDescriptionTest `/…/` cases, unchanged and green in the full backend run |
| 29b | example: Expected: its Spelling prompt hides that reading as it does today. | slice 1 | observed: ClozeDescriptionTest `/…/` cases, unchanged and green in the full backend run |
| 30a | example: Setup (conversion, reading after the title): the body has `「多大　/ただい/」とは…`. | slice 6 | observed: Clone diff of 漢字/多/多大.md: the removed line starts `「多大　/ただい/」とは、` |
| 30b | example: Expected: it becomes `「多大」とは…`. | slice 6 | observed: Clone diff of 漢字/多/多大.md: the added line starts `「多大」とは、` |
| 30c | example: Expected: the note has `pronunciation: ただい`. | slice 6 | observed: Clone diff of 漢字/多/多大.md: the frontmatter gains `pronunciation: ただい` |
| 30d | example: Expected: the inline `甚大（じんだい）` stays with its explanation. | slice 6 | observed: Clone file 漢字/多/多大.md at `81f06fc8`: the line `甚大（じんだい）` with its explanation is unchanged |
| 30e | example: Expected: the note keeps its memory trackers. | slice 8 | |
| 30f | example: Expected: the note keeps its recall history. | slice 8 | |
| 31a | example: Setup (conversion, reading in brackets): the body has `水引（/みずひき/）は、…`. | slice 5 | observed: Clone diff of 文化/水引.md in `1551a951`: the removed line starts `水引（/みずひき/）は、` |
| 31b | example: Expected: it becomes `水引は、…`. | slice 5 | observed: Clone diff of 文化/水引.md in `1551a951`: the added line starts `水引は、` |
| 31c | example: Expected: the note has `pronunciation: みずひき`. | slice 5 | observed: Clone diff of 文化/水引.md in `1551a951`: the frontmatter gains `pronunciation: みずひき` |
| 32a | example: Left unchanged and listed: 音 has `/ね/` and `/おん/` (several readings). | slice 3 | observed: listing `01-listing.md`: 音 is under “several matches” with `/おと/`, `/ね/` and `/おん/` (G2) |
| 32b | example: Left unchanged and listed: コツ has `釣りのコツ/钓鱼的秘诀。 コツを覚える/学会窍门` (not a reading). | slice 3 | observed: listing `01-listing.md`: コツ is under “the match is not plain kana” with `/钓鱼的秘诀。 　 コツを覚える/` |
| 32c | example: Left unchanged and listed: 足利義満 begins `/あしかが よしみつ/は、…` (removing the reading would leave the sentence without its subject). | slice 3 | observed: listing `01-listing.md`: 足利義満 is under “the reading is the subject of its sentence”, sentence start `/あしかが よしみつ/は、` |
| 32d | example: Left unchanged and listed: a note that already has a `pronunciation` property. | slice 3 | observed: Listing run on the fixture note 見本 with `pronunciation: みほん`: “1 unchanged: the note already has a pronunciation property” |
| 33a | commitment: UI: `pronunciation` appears in the existing key choice list of Add property. | slice 2 | observed: RichMarkdownEditor.propertyPresets.spec.ts “adds a pronunciation chosen from the presets as an ordinary property” and “add form lists every available preset once the typed text is cleared”: the Add property list includes `pronunciation` |
| 33b | commitment: UI: `pronunciation` appears in the existing key choice list of a property row's key field. | slice 2 | observed: RichMarkdownEditor.propertyPresets.spec.ts “existing row lists every available preset on focus and narrows on typing” and propertyRowEditing.spec.ts “omits another row's list preset while retaining the current row's own preset” |
| 33c | exclusion: UI: it adds no new control. | slice 2 | observed: Reading of the slice diff: one line in frontend/src/utils/noteContentPropertyKeyPresets.ts; no component changed |
| 34a | commitment: UI: in the Spelling prompt, a hidden `pronunciation` value uses the existing hidden-pronunciation mark `/.../`, the one the learner already sees for a `/…/` reading. | slice 1 | observed: RecallPromptSpellingStemMaskingControllerTest.spellingQuestionHidesSavedPronunciationWhereTheBodyRepeatsIt: the stem contains `<mark title='Hidden pronunciation'>/.../</mark>` |
| 34b | qualification: The mark tells the learner that a reading is hidden there, not the answer. | slice 1 | observed: Reason for 34a; the same mark is asserted there |
| 35a | commitment: UI: a one-kana reading such as `て` is also hidden inside other words, as in `挙げて`, because the existing matching rule accepts any kana as a word boundary. | slice 1 | observed: RecallPromptSpellingStemMaskingControllerTest.spellingQuestionHidesAOneKanaSavedPronunciationInsideOtherWords: stem `[...]を挙げ/.../。` |
| 35b | qualification: A `/て/` marking in the body does the same today. | slice 1 | observed: Saved values and repeats of a `/…/` reading both go through TitleFragment.replaceLiteralWords in ClozeReplacement.maskPronunciationsAndTitles |
| 36a | qualification: Architecture, publication: under ADR 0002, every accepted change to notebook content goes through Git publication. | slice 7 | observed: Reason for 36d; the pilot changed production content only through `donut notebook publish` |
| 36b | qualification: A SQL update of note content would change content outside that boundary: live notes would differ from accepted `main`, which local publication refuses until a recovery, and the property index and reference rows derived on a content write would be skipped. | slice 7 | observed: Reason for 36d; no SQL update was made |
| 36c | qualification: The original condition for pure SQL, that it can prepare the changes safely, is not met. | slice 7 | observed: Reason for 36d; no SQL update was made |
| 36d | commitment: The conversion uses that boundary's local path, which the owner already uses for this notebook. | slice 7 | observed: The pilot commit was published from the local clone with `donut notebook publish`, the local publication path |
| 36e | exclusion: Production runs no conversion code. | slice 8 | |
| 37a | commitment: Architecture, format: under ADR 0004, `pronunciation` is author-owned YAML frontmatter that is preserved as authored. | slice 2 | observed: RichMarkdownEditor.propertyPresets.spec.ts “adds a pronunciation chosen from the presets as an ordinary property”: the emitted YAML is the authored key and value; no format code changed |
| 37b | exclusion: It needs no format rule of its own. | slice 2 | observed: Reading of the slice diff: no format rule added |
| 38a | commitment: Architecture, one concept in two places for now: readings can be a `/…/` marking in the body or a `pronunciation` property, and Spelling review reads both. | slice 1 | observed: RecallPromptSpellingStemMaskingControllerTest.spellingQuestionHidesBothASavedPronunciationAndASlashReadingInTheBody: stem `[...] /.../ は「/.../」と読む。` |
| 38b | qualification: This is deliberate while unconverted notes exist. | slice 1 | observed: Reason for 38a; both passes exist in ClozeReplacement.maskPronunciationsAndTitles |
| 38c | qualification: Removing the body marking is the deferred item above. | slice 1 | observed: Refers to 21a; the body marking is kept |
| 39a | commitment: Spelling review reads `pronunciation` from the same note content it already reads `aliases` from. | slice 1 | observed: Note.createMaskedContentForRecall passes getContent() to FrontmatterAliases.fromNoteContent and FrontmatterPronunciation.fromNoteContent |
| 39b.1 | exclusion: It needs no new stored data. | slice 1 | observed: Reading of the slice diff: no migration, entity field, index, DTO or generated API file |
| 39b.2 | exclusion: It needs no new index. | slice 1 | observed: Reading of the slice diff: no migration, entity field, index, DTO or generated API file |
| 39b.3 | exclusion: It needs no new API field. | slice 1 | observed: Reading of the slice diff: no migration, entity field, index, DTO or generated API file |
