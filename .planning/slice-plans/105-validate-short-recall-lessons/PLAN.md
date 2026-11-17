# Validate short lessons after recall misses

**Identity:** SEED-073#validate-short-recall-lessons
**Source:** [selected story](../../seeds/SEED-073-learning-refinement-and-retrieval.md#validate-short-recall-lessons)
**Source review:** [source-review.md](source-review.md)

## Goal and scope

Give the owner a recommendation (proceed to design, narrow and retest, or
stop) on short contrast lessons for word pairs he keeps confusing in
spelling recall. Half of the testable pair groups get a read-once lesson;
seven days later he fills unseen sentences for every pair without help; his
ordinary spelling recalls of the trial's notes are counted for four weeks.

Included: selecting and fit-checking the pair groups, the random split, the
lesson pages, the check, the recorded effort, the recommendation, the
four-week count, and this story's evidence disposition.

Excluded, as the story defers them: any product screen, model call from the
product or stored lesson state; lessons at the moment of a miss; mistakes
in multiple-choice questions about a concept; lessons kept as notebook
content; other learners; statistical proof.

## Existing solution and architectural fit

PFE: nothing is built in the product. Reuse:

- the November 13 export and its `fetch.py` in the private cache (the MCP
  `list_recall_answers` route, which carries the typed answer);
- the independent-reader method of the earlier validations: fresh reader
  contexts, no web access, intended answers hidden
  ([fixed-skill retest](../../seeds/fixed-skill-spelling-retest-evidence.md#provenance-and-method));
- the [dated counts](../../seeds/learning-refinement-validation-approaches.md#validate-short-recall-lessons)
  for this story.

[ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md):
the trial records no Grade and touches no tracker, so schedules are
unchanged. [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md):
no notebook content is written; private learning evidence stays outside
Portable content and outside this repository. No conflict found. The
[North Star](../../NORTH-STAR.md) governs no choice here.

## Inputs and private files

Read the shared cache at
`/Users/terryyin/git/doughnut/.planning/tmp/recall-history/` without
changing its existing files. This story's private files (pair list, check
items, readers' returns, assignment, lessons, pages, answers, fresh
exports, scripts) live in a new folder `short-lessons/` inside that cache,
so they outlast the execution worktree across the four weeks. Nothing in
it is committed. The committed result is
`.planning/seeds/short-recall-lessons-evidence.md`: counts, dates, digests
and a few short examples.

Run Python as `python3 -I`. Production is read only.

## Decisive premises and observations

Preparation checkout: this workspace at `523d2eba27`, 2026-11-17.

| Premise / consuming operation | Observation, result and limits | Owner |
| --- | --- | --- |
| The export yields the repeated pairs (slice 1 selection) | `python3 -I` over `recall-answers-100d.json` (SHA-256 `3071dac175df47cd172c6bbc54f8fe887f475658f950ca340e9e441dde0ad3df`): 227 rows with outcome `ACCIDENTAL_MATCH`; counting both directions together, 40 pairs with two or more mix-ups (103 answers; 36 Japanese); 31 groups (25 of one pair, 4 of two, 1 of three, 1 of four). Recheck the digest before use. | Slice 1 |
| Enough groups pass the fit check for a split | Not observable before the check sentences exist. meadow/pasture is expected to fail (its cue names grazing). Slice 1 is the probe: fewer than 12 passing groups stops the plan for the owner. | Slice 1 |
| Later spelling answers with the typed word can be read (slice 6 count) | A copy of the cache's `fetch.py` run as `python3 -I fetch.py 1` in a scratch folder on 2026-11-17 23:51 SGT: "Deployed MCP advertised list_recall_answers via Donut CLI", 190 attempts, 34 spelling, each with `typedAnswer`, 2 with outcome `ACCIDENTAL_MATCH`. Scratch copy deleted. The script writes beside itself, so a copy in `short-lessons/` leaves the shared files alone. | Slice 6 |
| `GET /api/user/recall-history-export` could serve the count instead | False for this use: read 2026-11-17 with `?timezone=Asia/Singapore` (200; 10,259 spelling answers), an answer carries the outcome but not the typed word. Not used. Download deleted. | — |
| Independent readers are available | Fresh agent contexts served as readers on 2026-11-14, 11-16 and 11-17 (cited evidence above). Same model family; stated as a limit. | Slices 1–2 |
| The owner's time | Agreed in refinement on 2026-11-17: about 35 minutes once, about 20 minutes a week later. Dates are set in slice 4. | Slices 4–5 |

## Outside-in proof ownership

These are research observations at the owner-facing report. Product
behavior and tests are unchanged, so no product test run is warranted. For
the committed documents use `git diff --check`, a link and anchor reading
of the changed files, and
`node .claude/skills/dough-product-backlog/scripts/product-backlog.mjs read-state --link 'seeds/SEED-073-learning-refinement-and-retrieval.md#validate-short-recall-lessons'`.

| Promise / source example | Owning slice | Observable proof |
| --- | --- | --- |
| Groups enter only when exactly one word fits each sentence; the rest are counted with a reason (examples 1, 2) | 1 | Private list: each pair's two check sentences, both readers' answers, pass or the reason. Evidence file: counts by reason. |
| Words confused with each other form one group (example 3) | 1, 2 | The group list; the assignment file shows whole groups in one half. |
| Random split by a recorded seed before lessons are written | 2 | Assignment file with seed and script; its timestamp precedes the first lesson file. |
| Lesson page content, each deciding feature source-checked | 2, 3 | Per lesson: the source consulted; a blind reader answers the practice sentence with the intended word. Pages open in a browser. |
| Check sentences unseen, fixed before lessons, naming neither word (example 4) | 1, 3 | Check items frozen in slice 1 (digest recorded); no check sentence appears in a lesson; dry run of the check page. |
| Per-pair record: answers, minutes, skipped lesson, late or missed check (examples 5, 6) | 4, 5 | Saved answer files with times; the evidence file reports skipped and late items apart. |
| Recommendation with both halves' counts and effort, for the owner's review | 5 | Evidence file section dated on the check day. |
| Four-week recall count; may revise the recommendation | 6 | Fresh export digest and window; counts per half; the recommendation confirmed or revised. |
| Production read only; private evidence uncommitted | all | Only GET and MCP list calls; `git status` shows no file from `short-lessons/`; published examples are short and de-identified. |
| Evidence disposition | 6 | Closeout paragraph under the [shared contract](../../seeds/SEED-073-learning-refinement-and-retrieval.md#close-out-temporary-recall-evidence). |

## Ordered slices

Slices 4 to 6 wait on dates. Record each date in this plan when it is set.
Between them the story stays in progress; nothing else is done.

### 1. Select the pair groups that can be tested
Type: Behavior
Status: done
Completed: 2026-11-18
Result: 20 of 31 groups pass (24 pairs, 48 check sentences); 9 out for a
declared overlap, 2 because readers wrote the other word (整う/調う after
revision, 脇/腋). Frozen check items `check-items.json` SHA-256
`9db182f94e32085410ca992e7461a43da9a15f5c6d0905aa8edfc4b2d63a8da1`.
Evidence: [fit check](../../seeds/short-recall-lessons-evidence.md#fit-check-2026-11-18).
Learning: meadow/pasture passed (the sentences do not use its cue), unlike
key example 2's expectation; kept by the stated rule.
Proof: the private list holds every one of the 40 pairs with its group,
two check sentences, both readers' answers and pass or reason; the frozen
check-item file's SHA-256 is recorded here; the evidence file is started
with the counts by reason and two short examples.

Behavior: the export and its 40 repeated pairs → group them, write two new
sentences per pair (one where only the first word fits, one where only the
second fits, each with a meaning hint that contains neither word), and give
all sentences to two fresh readers who see no intended word → a group
passes when both readers supply the intended word, or a script or
inflected form of it, for every sentence of every pair in it.

One revision of a failed sentence is allowed, labelled as revised and
re-read by two new readers; no further rewriting. A pair whose reviewed
note already declares an overlap with the other word, or where readers
supply the other word, is counted as "both words fit". Freeze the accepted
written forms per sentence here, before any answer exists.

Stop: fewer than 12 passing groups → record the count and stop for the
owner; slices 2 to 6 wait.

### 2. Split the groups and write the lessons
Type: Structure
Status: done
Completed: 2026-11-18
Result: seed 20261118; lesson half 10 groups (10 pairs), comparison half 10
groups (14 pairs). Assignment 00:32:18 UTC precedes lessons 00:34:45.
Lessons cite sources; ただいま/たった今 reported as several features.
Evidence: [split and lessons](../../seeds/short-recall-lessons-evidence.md#split-and-lessons-2026-11-18).
Learning: open-blank practice sentences drew other near words (5 of 10), so
practice items name the two words to choose from; check items stay open
blanks. Slice 3's lesson page shows the two choices with the practice.
Proof: the assignment file (seed, script, halves) is older than every
lesson file; each lesson names its source; a fresh reader given only the
lesson's practice sentence supplies the intended word.

Internal change enabling slice 3: split the passing groups at random into
a lesson half and a comparison half with a recorded seed. For each pair of
the lesson half write the lesson content: the typed word, the intended
word, one deciding feature, two short sentences that differ in that
feature, one practice sentence with its answer and reason. Check each
feature against a dictionary or usage source and note the source. Where two
words differ in several ways at once and no single feature holds, say so in
the lesson and list the pair for the report. No lesson reuses a check
sentence.

### 3. Open the lesson and check pages locally
Type: Behavior
Status: done
Completed: 2026-11-18
Result: `lesson.html` and `check.html` in `short-lessons/`, rebuilt by
`build_pages.py`; headless Chromium dry run passed and its files were
deleted. Answer files download as `lesson-answers-<date>.json` and
`check-answers-<date>.json` to the Downloads folder.
Evidence: [pages](../../seeds/short-recall-lessons-evidence.md#lesson-and-check-pages-2026-11-18).
Proof: a dry run in a browser with made-up answers: the lesson page shows
one pair at a time and withholds the practice answer until one is typed;
the check page shows one sentence at a time in shuffled order with no
feedback until the end; each saves an answer file with per-item times. The
dry-run files are deleted afterwards.

Behavior: the lesson content and the frozen check items → build two plain
local pages in `short-lessons/` → the owner can open each from a file path
and finish with a saved answer file. The check page keeps the two
sentences of a pair apart and names neither word. Keep the pages as simple
as this needs; they are deleted at closeout.

### 4. The owner takes the lessons
Type: Behavior
Status: done
Completed: 2026-11-18
Lesson day: 2026-11-18 (07:22–07:31 SGT, 8.9 minutes, none skipped).
Check window: 2026-11-23 to 2026-11-27, aimed at 2026-11-25.
Evidence: [lessons taken](../../seeds/short-recall-lessons-evidence.md#lessons-taken-2026-11-18).
Proof: the saved lesson answer file with start and end time; this plan
records the lesson day and the check window (day 5 to 9, aimed at day 7).

Behavior: pages ready → ask the owner once, with everything he needs: the
file path to open, about 35 minutes in one sitting, no notes or tools for
the practice answers, the option to skip a pair, where the answer file is
saved, and the check window dates → the lesson half is taken or marked
skipped per pair. He does not open the check page before the check day.

### 5. The owner takes the check; report the recommendation
Type: Behavior
Status: planned
Proof: the saved check answer file; the evidence file's dated section
gives, per half, answers right, the paired word, and other; minutes spent
on lessons and check; skipped lessons; late or missed items apart; the
limits from the story; and the recommendation.

Behavior: on the check day ask the owner once: the file path, about 20
minutes, no notes or tools, type the word for each sentence → score each
answer against the forms frozen in slice 1 → publish the counts and the
recommendation for the owner's review. Report on-time and late answers
apart. A group the owner did not check is reported as missed, not scored.

The frozen accepted forms include some near synonyms besides readings
and inflections (少し, 幾分 for いくらか; で for それで; しかも for それに;
さっき for たった今; grass for pasture; trireme for galley; 越して, 超して,
切れた for 過ぎる; 割れた for 砕ける; 怖く for 恐ろしい; 沈黙 for 黙り;
着いた for 付く). Score against the frozen forms, and report answers that
match only such a synonym apart from answers that are the intended word.

The recommendation states what the difference between the halves shows and
how little it can show with this many groups; it fixes no pass mark.

### 6. Count four weeks of recalls and close out
Type: Behavior
Status: planned
Proof: the fresh export's window and SHA-256; per half, spelling recalls of
the trial's notes from the lesson day: correct, the paired word typed,
other; the recommendation confirmed or revised with the date; the closeout
paragraph.

Behavior: four weeks after the lesson day → run a copy of `fetch.py` in
`short-lessons/` for the days needed → count per half and add the dated
section; revise the recommendation only if the count changes its reading.
Record this story's evidence disposition: delete `short-lessons/` once the
conclusions are in the evidence file, and release or keep the shared cache
under the shared contract after checking the other queued validations.

## Current decisions

- The primary result is the day-seven check. The four-week count is
  secondary and comes from ordinary recalls only.
- Groups, not pairs, are the unit of the split.
- No pass mark is fixed in advance; the owner reviews the recommendation.
- Fewer than 12 passing groups stops the plan for the owner.
- Private files live in the default checkout's cache folder, not in the
  execution worktree.

## Refinement and concerns

Slice-plan refinement was not needed: six slices, each with one outcome
and its own proof, below the eight-slice limit; slice 2 is the only
Structure and directly feeds slice 3. This review identified no remaining
slice-specific concern. The one unobserved premise, how many groups pass
the fit check, is bounded by slice 1 as an early probe.
