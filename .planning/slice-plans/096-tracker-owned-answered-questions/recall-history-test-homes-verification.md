# Recall history export test homes

Structure 9 separates the existing 358-line export test into seven recall-event
content/state cases and seven envelope/access/daily-probe cases. Existing setup,
`ownedNote` and `answerMcqAtDay` have one owner in the shared base. The new
event home is 192 lines, envelope home 155 and base 35 before formatting.
Product code, fixture semantics, compatibility writers and schema are unchanged.

## Preserved observations

Against accepted base `e5b8aca1c605f59775f8cc607ea5f08095a9348e`, all 14 test
names, signatures, bodies and assertions are byte-identical. Existing setup and
five relevant helper bodies survive; only the two shared private helpers gain
package visibility. All six injected declarations have one owner, with no new
Spring annotation mix. Root and a separate read-only reviewer inspected this
reading: `/tmp/donut-slice9-preservation-reading.txt`.

The event cases retain ordered event kinds/times/grades, answer identity and
timers, repeated-tracker state, exclusion time/reason, withdrawn tracker and
trashed-note history, and accidental spelling-match outcomes. The envelope
cases retain login/access refusal, timezone/export time, bearer JSON shape,
ordered daily probes/trials, disabled-probe history and the empty probe list.
Both concrete suites ran all seven cases with no skip. Selected conventions
are MakeMe true/pageObjects false for both concrete tests, false/false for the
base; existing base practice is preserved.

## Full backend proof

`CURSOR_DEV=true nix develop -c pnpm backend:test_only > /tmp/donut-slice9-export-homes-backend.log 2>&1`

Native session 68838 reached terminal 0. Start:
`2026-11-13T07:59:28.317663Z`; final log write `08:04:35.341345Z`; terminal
observed by `08:05:36.513453Z`. The command log interval was 307.024 seconds:
migration reported 45s and tests 4m11s. Fresh XML at `08:04:34Z` contains
640 suites, 3,195 tests, zero failures/errors and two skips. The additional
suite preserves the accepted baseline's total cases. Root independently
counted XML and inspected the three homes. Exact case IDs/timings and freshness:
`/tmp/donut-slice9-backend-proof.json`; complete handoff:
`/tmp/donut-slice9-handoff.md`.

Before the serial run, the 16-CPU/48-GiB host had load 17.68/19.49/19.78 and
41% free memory. No other owned backend run was active. Unowned fseventsd and
Open Dough Playwright work remained untouched. Conservative edit/cleanup time
was about 61s (`07:58:27Z`–proof start); the required full-suite wait is the
plan's separate indivisible proof exception. No edits followed proof start.

## Consumer and independent review

The explicit ownership trace reached terminal 0 with these inputs:

```sh
node .agents/skills/dough-execute-plan/scripts/consumer-trace.mjs \
  --checkout /Users/terryyin/git/doughnut/.worktrees/keep-answered-questions-with-their-tracker \
  --path backend/src/test/java/com/odde/donut/controllers/UserRecallHistoryExportControllerTest.java \
  --path backend/src/test/java/com/odde/donut/controllers/UserRecallHistoryEventsExportControllerTest.java \
  --path backend/src/test/java/com/odde/donut/controllers/UserRecallHistoryExportControllerTestBase.java \
  --term UserRecallHistoryExportControllerTest \
  --term UserRecallHistoryEventsExportControllerTest \
  --term UserRecallHistoryExportControllerTestBase --term answerMcqAtDay
```

`/tmp/donut-slice9-owned-consumer-trace.md` has three read/reused planning
readers and no external product/test/helper consumers, zero pending. The same
command with `--base e5b8aca1c605f59775f8cc607ea5f08095a9348e` also includes
root-owned planning/evidence edits; all 130 scope-derived matches have explicit
exclusions in `/tmp/donut-slice9-consumer-trace.md`. Their content movement is
not represented as runtime proof. The fresh independent refactor separately
dispositioned planning readers in
`/tmp/donut-slice9-independent-refactor-planning-trace.md`, zero pending.

Source freshness settled after full proof: terminal 0, unchanged selected
story/plan at fetched authorized main
`4059ac2353c590f33c971f5d531cf3853dacd4a8`. No named sibling was supplied.
Fresh independent refactor returned `none — already clean` and
`REFACTOR COMPLETE`; no edits or redundant test run. Accepted setup, boundary
and assertions remain valid. No generator input changed.

The same increment carries the reviewed remaining-plan refinement: 13 leaves,
completed 1–8 and all 131 source commitment texts preserved, G3 owned by 11,
retention/final schema owned by 12/13. The actual preparation reassessment is
ready with no concerns; delivery proof/status updates do not renew it. The
canonical seed's dated observations moved intact to its linked evidence home;
only a terminal separator was trimmed. All changed handwritten homes are under
250 lines. Checkpoints B/C and their human go-aheads remain future work.
