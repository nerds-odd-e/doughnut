# Check short lessons and count later recalls

**Identity:** SEED-073#check-short-recall-lessons
**Source:** [selected story](../../seeds/SEED-073-learning-refinement-and-retrieval.md#check-short-recall-lessons)

Split on 2026-11-18 from SEED-073#validate-short-recall-lessons, whose
slices 5 and 6 became slices 1 and 2 here. Its slices 1 to 4 (fit check,
split, lessons, pages, lessons taken) are delivered; their results are in
the [evidence file](../../seeds/short-recall-lessons-evidence.md).

## Goal and scope

Give the owner a recommendation (proceed to design, narrow and retest, or
stop) from the day-seven check of the lessons taken on 2026-11-18, then the
four-week count of his ordinary spelling recalls of the trial's notes.
Included: the check, scoring, the recommendation, the four-week count, and
this story's evidence disposition. Excluded: as the story says.

## Dates and the too-early rule

- Lesson day 2026-11-18. Check window 2026-11-23 to 2026-11-27, aimed at
  2026-11-25. Four-week count due 2026-12-16.
- Before any slice, compare today's date with these. Before 2026-11-23:
  tell the owner the check is too early and how many days remain, change
  nothing, and stop with the story still queued (postponed). After slice 1
  and before 2026-12-16: tell the owner the count is not due and stop the
  same way; slice 2 resumes on or after that date.

## Inputs and private files

All private files are in
`/Users/terryyin/git/doughnut/.planning/tmp/recall-history/short-lessons/`:
`check.html` (the owner's page), `check-items.json` (frozen; SHA-256
`9db182f94e32085410ca992e7461a43da9a15f5c6d0905aa8edfc4b2d63a8da1`),
`assignment.json` (halves; SHA-256
`1339038df0770ce7d4e34f48760598efe0555bb1fc92bed48ceb56464fe95144`),
`lessons.json`, `lesson-answers-2026-11-18.json`, `pairs.json`. The check
page saves `check-answers-<date>.json` to `~/Downloads` (list it with a
bounded command; an unbounded listing there can hang on a macOS permission
prompt). Nothing in the folder is committed. Run Python as `python3 -I`.
Production is read only. ADR 0002 and ADR 0003 hold as in the original
plan: no notebook content, Grade or tracker is written.

## Outside-in proof ownership

Research observations at the owner-facing report; no product test run. For
the committed documents use `git diff --check`, a link and anchor reading
of the changed files, and
`node .claude/skills/dough-product-backlog/scripts/product-backlog.mjs read-state --link 'seeds/SEED-073-learning-refinement-and-retrieval.md#check-short-recall-lessons'`.

## Ordered slices

### 1. The owner takes the check; report the recommendation
Type: Behavior
Status: planned
Proof: the saved check answer file; the evidence file's dated section
gives, per half, answers right, the paired word, and other; minutes spent
on lessons and check; skipped lessons; late or missed items apart; the
limits from the story; and the recommendation.

Behavior: in the check window ask the owner once: the file path, about 20
minutes, no notes or tools, type the word for each sentence → score each
answer against the frozen forms → publish the counts and the
recommendation for the owner's review. Report on-time and late answers
apart. A group the owner did not check is reported as missed, not scored.

The comparison between the halves uses the 14 groups that met the fit
check, 7 a half. The six groups that passed it only through another
accepted word or the shared kanji 奴 (G21, G22, G26 in the lesson half;
G03, G20, G23 in the comparison half; see the
[evidence](../../seeds/short-recall-lessons-evidence.md#groups-reported-apart))
are checked and scored the same way, and their counts are reported apart.

The frozen accepted forms include some near synonyms besides readings
and inflections (少し, 幾分 for いくらか; で for それで; しかも for それに;
さっき for たった今; grass for pasture; trireme for galley; 越して, 超して,
切れた for 過ぎる; 割れた for 砕ける; 怖く for 恐ろしい; 沈黙 for 黙り;
着いた for 付く). Score against the frozen forms, and report answers that
match only such a synonym apart from answers that are the intended word.

The recommendation states what the difference between the halves shows and
how little it can show with this many groups; it fixes no pass mark. It
also reports the pair whose words differ in several ways at once
(ただいま/たった今).

### 2. Count four weeks of recalls and close out
Type: Behavior
Status: planned
Proof: the fresh export's window and SHA-256; per half, spelling recalls of
the trial's notes from the lesson day: correct, the paired word typed,
other; the recommendation confirmed or revised with the date; the closeout
paragraph.

Behavior: on or after 2026-12-16 → run a copy of the cache's `fetch.py` in
`short-lessons/` for the days needed (it writes beside itself; observed
working on 2026-11-17 through the MCP `list_recall_answers` route, which
carries the typed answer) → count per half and add the dated section;
revise the recommendation only if the count changes its reading. Record
this story's evidence disposition: delete `short-lessons/` once the
conclusions are in the evidence file, and release or keep the shared cache
under the [shared contract](../../seeds/SEED-073-learning-refinement-and-retrieval.md#close-out-temporary-recall-evidence)
after checking the other queued validations.

## Current decisions

- The primary result is the day-seven check. The four-week count is
  secondary and comes from ordinary recalls only.
- Groups, not pairs, are the unit of the split (already made).
- No pass mark is fixed in advance; the owner reviews the recommendation.
- Starting before a due date only reports the date and postpones.
