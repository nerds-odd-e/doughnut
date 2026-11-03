# Plan: Morning probe and recall analysis

**Source:** [SEED-100#story-2](../../seeds/SEED-100-recall-history-export.md#story-2)
**Source review:** [source-review.md](source-review.md)
**Identity:** SEED-100#story-2

## Goal and scope

The owner gets one dated report, `.planning/notes/recall-and-morning-probe-analysis.md`,
written from their production recall history export, that states with sample
sizes and strength whether the daily probe relates to that day's recall, how
efficient the owner's recall sessions and spaced repetition are, and which
probe changes each finding implies. No product code, test, or document
changes. Scope, definitions, required restriction, scope limits, and key
examples are in the source story and are not repeated here.

**Exclusions (from the story):** product changes of any kind, an in-product
analytics tile, a prospective probe experiment, other users' data, note ids,
note titles, tutor feedback text, and the raw export itself in the repository.

**Assumptions:** the owner's export is at most a few tens of thousands of
events and loads into pandas in memory; the export's one timezone is the
owner's usual one (the report states it as a limitation either way).

## Current decisions

- **Input handover.** Execution starts when the owner's export JSON is at a
  path outside the repository (for example `~/Downloads/recall-history-<date>.json`
  from the recall stats tab's "Download recall history" button), or when a
  personal access token lets the agent fetch
  `https://doughnut.odd-e.com/api/user/recall-history-export?timezone=<zone>`
  with `Authorization: Bearer <token>` and save it there. Every script takes
  that path as its argument. Nothing copies the export into the repository.
- **Tooling.** System `python3` (`/usr/bin/python3`, pandas 2.3.3, scipy 1.13.1,
  numpy 2.0.2, pytest 8.4.1, all observed 2026-11-03). Scripts and their
  tests live in this plan folder under `analysis/`, run as
  `cd /tmp && /usr/bin/python3 -E -B -m pytest -p no:cacheprovider -q <checkout>/.planning/slice-plans/002-morning-probe-and-recall-analysis/analysis`
  and `/usr/bin/python3 -E -B <checkout>/.planning/slice-plans/002-morning-probe-and-recall-analysis/analysis/recall_probe_analysis.py <export.json> <section>`.
  `-I` cannot be used: pandas, numpy and pytest are in the user site
  (`~/Library/Python/3.9`), which `-I` disables, and Python 3.9 has no `-P`;
  `-E -B -p no:cacheprovider` keeps the export an argument only and writes no
  cache into the repository.
  They are outside every product build, lint, and format step (`pnpm
  format:changed` touches no `.py` file). No product dependency is added.
- **Definitions reused from the product** (PFE: `RecallAnswerRow`,
  `RecallStatsAggregator.responseTimeMs`, `RecallAccuracyAggregator`,
  `Answer.correctFrom`, all at `51f844a1d5`), re-expressed in the script, not
  called:
  - an event is an answered prompt when `kind == ANSWERED_RECALL_PROMPT`;
  - it counts as a review when `answer.outcome != OVERLAP` and
    `answer.excludedAt` is null;
  - correct = grade `GOOD` or outcome `OVERLAP`; wrong = grade `AGAIN`;
    `HARD`, `EASY`, `CONFUSION` are neither and leave correctness measures;
  - response time = `answer.thinkingTimeMs`, else
    `answer.createdAt - answer.promptCreatedAt` when positive; capped at
    120 s (thinking time) or 300 s (timestamp difference); dropped below 1 s;
  - surprise for a set of reviews with non-null `retrievability` =
    `sum(y - p) / sqrt(sum(p (1 - p)))`, `y` ∈ {0, 1}, `p` = raw
    retrievability (the tiles additionally recalibrate `p` per question type;
    the report states this difference);
  - local day = calendar date of the timestamp in the export's `timezone`;
    the probe's day is the local day of `completedAt`; "after the probe" =
    `answer.createdAt > completedAt` on the same local day.
- **Day-level measures.** Per day: reviews after the probe, correct share,
  surprise, median response time, distraction share
  (`awayMs + detourMs + idleMs` over session length), session length, pace
  (reviews per active minute, active = session length minus
  away/detour/idle). Session length comes from answers, not prompts
  (prompts are pre-generated, see Learnings): a day's answered prompts and
  `JUST_REVIEW` events split into sessions at gaps over 30 minutes; an answer
  occupies `thinkingTimeMs + awayMs + detourMs` before its `createdAt` (away
  and detour pause the product's thinking clock, idle stays inside it); the
  day's length is the sum of its session spans. Slice 4 reuses the same
  helpers in `analysis/recall_export.py`. A day enters the relation table only
  with at least 10 reviews after the probe (the inventory reports how many
  days the threshold removes).
- **Strength and verdict.** Spearman rho with a 95% bootstrap interval over
  days. Verdict: "supported" when the interval excludes 0; "not supported"
  when it includes 0 and there are at least 30 days; "too few days to tell"
  below 30 days, with the number of days needed to detect |rho| = 0.3 at 80%
  power. Baselines: a leave-one-day-out linear prediction of each recall
  measure from (a) the owner's mean by weekday and by probe hour band, (b)
  the previous paired day's value, and (c) each alone plus the probe measure;
  the probe "gains" when (c) lowers the out-of-sample mean absolute error
  over (a) and (b). Verdicts and gains are reported per row; nothing is
  aggregated into an index.
- **Report.** One Markdown file with the sections Inventory, Probe and
  same-day recall, Recall-session efficiency, Repetition efficiency,
  Findings, Proposals, Method and limits. The Method section records
  `exportedAt`, `timezone`, the definitions above, and that the scripts live
  in this plan folder and are recoverable from Git history after wrap-up
  (`git log --all -- .planning/slice-plans/002-morning-probe-and-recall-analysis`).
  Each slice appends or replaces its own section; the scripts print the
  section's Markdown so the report is regenerated, not hand-edited, except
  Findings and Proposals, which the agent writes from the numbers.
- **ADR 0003.** A proposal that would make the probe a scheduling input or a
  validator of Pace, Accuracy, or Consistency is listed under Proposals with
  the conflict named and no story recommendation.

## Decisive premises

| Premise | Consuming operation | Observation | Result |
| --- | --- | --- | --- |
| Production serves the export with the `RecallHistoryExport` fields the definitions above read (`kind`, `grade`, `retrievability`, `answer.outcome`, `answer.excludedAt`, timing and distraction fields, probe measures). | The script reads the owner's export. | `https://doughnut.odd-e.com/api/healthcheck` → `OK. Active Profile: prod. Commit: 51f844a1d5ec2dec604f1ed1da17cbe3b5916a89` (2026-11-03); Application Release v1.3.66 jobs "Admit selected application release" and "GCP deploy (GCS + MIG + health probe)" both success; `UserRecallHistoryExportControllerTest` (14 cases, including grade values `GOOD`/`HARD`/`AGAIN`/`CONFUSION`, exclusion fields, and probe trials) passed in "donut CI" on main at 51f844a1d5 (run created 2026-11-03T03:37:55Z). | Settled for the shape. Whether the owner's own file parses is observed in slice 1, which stops the plan if a field is missing. |
| Correctness is derivable from the exported `grade` and `answer.outcome`. | Day-level correct share and surprise. | `Answer.correctFrom(outcome, grade)` at `51f844a1d5`: `OVERLAP` → true, `GOOD` → true, `AGAIN` → false, else null; the export carries both fields (test above). | Settled. |
| System Python has pandas, scipy, numpy, pytest. | Every script and test run. | `python3 -c "import pandas, scipy"` → `pandas 2.3.3 scipy 1.13.1`; `python3 -m pytest --version` → `pytest 8.4.1`; numpy 2.0.2 (2026-11-03, this machine). The Nix shell has none of them. | Settled; scripts run outside the Nix shell. |
| Enough probe days with reviews after the probe exist to answer the relation question (H1), probes mostly precede that day's reviews (H2), and day-level recall varies beyond item mix (H3). | Slices 2–3 verdicts; slice 6 proposals. | Owner-held: needs the owner's export. | Early probe slice 1 settles all three and records the counts; a low count changes verdicts and proposals, not the plan. |

## Outside-in proof

| Story example | Owning slice | Proof |
| --- | --- | --- |
| 1. 60 probe days, 48 paired → relation rows say 48 days with strength, interval, verdict | 2 | Fixture export with 60 probes, 48 paired days → rows carry `n = 48`; the real run's section |
| 2. Probe day without later reviews → in inventory, out of relation | 1, 2 | Fixture: such a day appears in the inventory counts and not in the relation `n` |
| 3. Probe at 15:10 → still that day's probe; time-of-day distribution shown | 1 | Fixture: afternoon probe counted on its local day; distribution table has an afternoon row |
| 4. Excluded answer → out of correctness and time measures; counted with its reason | 1, 2 | Fixture: exclusion listed by reason; measures unchanged by the excluded row |
| 5. `JUST_REVIEW` and `TUTOR_FEEDBACK` → not reviews; `JUST_REVIEW` counts in length and pace | 1, 4 | Fixture: counts by kind; session span extends to a trailing `JUST_REVIEW` |
| 6. Supported, but no gain over yesterday → verdict says so | 3 | Fixture with a planted relation that persistence predicts equally → "supported, no gain over the previous day" |
| 7. 12 usable days → "too few days to tell" and days-needed | 2, 6 | Fixture with 12 paired days → every row says too few days and names the days needed; proposals section carries the collection proposal |
| 8. `AGAIN` clustering below retrievability 0.7 → calibration band reported | 5 | Fixture with planted misses at low retrievability → band table shows it |
| 9. After wrap-up: notes file remains, plan folder gone, product untouched | wrap-up (not a slice) | `git status` after each slice shows changes only under this plan folder and `.planning/notes/`; wrap-up deletes the plan folder |

## Slices

### 1. Data inventory from the owner's export

Type: Behavior
Status: done
Proof: `analysis/test_recall_probe_analysis.py::test_inventory*` on a small
synthetic export (events of all three kinds, an excluded answer, an
`OVERLAP`, a morning and an afternoon probe, a probe day without later
reviews); `python3 -I -m pytest .planning/slice-plans/002-morning-probe-and-recall-analysis/analysis`;
then the real run writes the report's Inventory and Method sections.

Behavior: the owner's export file is at the handed-over path → run the
inventory → the Inventory section states events by kind, reviews with
excluded answers by reason, `OVERLAP` answers, probe days, probe completion
time-of-day distribution (hour bands), days with a probe and at least one
review after it, and days with at least 10 such reviews; the Method section
records `exportedAt`, `timezone`, and the definitions. A missing or renamed
field stops the run with the field named. This slice settles H1–H3: the
counts, the time-of-day table, and the spread of day-level correct share and
surprise are recorded in the plan under Learnings before slice 2 starts.

### 2. Probe-to-recall relation with strength and verdict

Type: Behavior
Status: done
Proof: `test_relation*`: a fixture with a planted monotone relation between
probe speed and surprise → "supported" with an interval excluding 0; an
independent fixture with 40 paired days → "not supported"; a 12-day fixture
→ "too few days to tell" with the days needed; then the real run writes the
"Probe and same-day recall" section.

Behavior: inventory done → run the relation → one table with a row per
(probe measure × recall measure): rho, 95% interval, `n` days, verdict; the
all-day sensitivity variant as a second table; days below the 10-review
threshold excluded and their number stated.

### 3. Probe gain over what the owner already knows

Type: Behavior
Status: done
Proof: `test_gain*`: a fixture where the previous day predicts surprise as
well as the probe → "supported, no gain over the previous day"; a fixture
where only the probe predicts → "gain over both baselines"; an unsupported
row still carries its baseline errors; then the real run writes the gain
comparison into the "Probe and same-day recall" section.

Behavior: relation table exists → run the gain comparison → every row gets
leave-one-day-out mean absolute errors for the typical-day baseline, the
previous-day baseline, and each with the probe added; a supported row's
verdict names its gain or lack of gain over each baseline, and other rows
show the comparison as information. (Corrected during execution: the story
compares the probe against both baselines without limiting it to supported
rows, and slice 2 found no supported row, so a supported-only comparison
would leave the report without one.)

### 4. Recall-session efficiency

Type: Behavior
Status: done
Proof: `test_session*`: a fixture with a trailing `JUST_REVIEW` extends the
session span; distraction share and pace computed from known `awayMs`,
`detourMs`, `idleMs`; then the real run writes the "Recall-session
efficiency" section.

Behavior: inventory done → run the session analysis → median response time,
distraction share, session length, and pace by day, summarized overall, by
weekday, by start-hour band, and by position in the session (first ten
reviews versus the rest), each with its `n`.

### 5. Repetition efficiency

Type: Behavior
Status: done
Proof: `test_repetition*`: a fixture with planted `AGAIN` grades at
retrievability below 0.7 → the calibration band table shows observed correct
share below predicted in that band; too-early and too-late shares computed
from known rows; then the real run writes the "Repetition efficiency"
section.

Behavior: inventory done → run the repetition analysis → calibration table by
retrievability band (predicted mean, observed correct share, `n`); share of
reviews that look too early (retrievability ≥ 0.95 and correct) or too late
(`AGAIN` at retrievability < 0.7); `elapsedHours` by grade.

### 6. Findings and proposals

Type: Behavior
Status: done
Proof: the coordinator reads the Findings and Proposals sections against the
story: every finding cites a table and its `n`; every proposal names its
finding; an inconclusive relation carries the days-needed proposal
(`test_days_needed` covers the number); any proposal in ADR 0003 territory
names the conflict. `git status` shows changes only under this plan folder
and `.planning/notes/`.

Behavior: sections 1–5 exist → the agent writes Findings (numbered, each
tied to a table and sample) and Proposals (each tied to a finding, stating
the probe change: offer timing, length, shown number, pairing with recall
feedback, or keep collecting for a stated number of days) → the report is
complete and dated; the story's example 9 is left to wrap-up.

## Concern evidence

Slice-plan refinement was not needed: each slice is one script section, one
fixture test, and one real run on the same input, with no product code, no
shared structure between slices beyond the parsing done in slice 1, and no
integration beyond reading one file. Slices run longer than the ~5-minute
execution-leaf target because each contains a real run and a report section;
splitting further by probe measure or recall measure would fragment one
table, so the overrun is accepted with that reason. No slice-specific concern
remains: the only unsettled premises are owner-held (H1–H3) and slice 1 is
the early probe that settles them before dependent slices.

## Learnings

- **Input (2026-11-03).** The owner supplied a personal access token; the
  export was fetched with `timezone=Asia/Jakarta` (this machine's zone) to
  `~/Downloads/recall-export/recall-history-2026-11-03.json`: 133,814 events
  (2023-07-24 to 2026-11-03), 66 probes on 66 days (2026-08-29 to
  2026-11-03), `exportedAt` 2026-11-03T21:27:06.183+07:00. The export is
  larger than the plan's assumption but loads in about 2 s.
- **H1 settled: enough paired days.** 66 of 66 probe days have a review after
  the probe; 65 have at least 10 (median 115.5 reviews after the probe per
  day). Verdicts can reach "supported" or "not supported" (≥ 30 days).
- **H2 settled: probes precede recall.** The probe precedes the day's first
  review on 64 of 66 days (97%). Probe times: 61 of 66 before 09:00
  (27 before 06:00), 3 of 66 at 12:00 or later.
- **H3 settled: day-level recall varies.** Over the 65 usable days, correct
  share ranges 0.769–0.935 (SD 0.040) and surprise ranges −2.588 to 3.657
  (SD 1.262, median +0.912: raw retrievability under-predicts correctness).
- **No `OVERLAP` answers exist** in the export (outcomes are null or
  `ACCIDENTAL_MATCH`, which the product counts as a review), and only one
  answer is excluded. The `OVERLAP` and exclusion rules are exercised only by
  the fixtures.
- **Exclusion reasons are free text that quotes note content**, so the report
  labels them ("reason 1") with counts and never quotes them.
- **Session span (slice 4).** `answer.promptCreatedAt` cannot mark a session
  start: prompts are pre-generated (31% of prompt-to-answer gaps exceed
  300 s, 90th percentile about 5 days, 30,486 negative). Slice 4 takes a
  session's start as the first answer's `createdAt` minus its
  `thinkingTimeMs`, and splits sessions by gaps between answers rather than
  using one span per day.
- **Timing fields.** Since 2026-08-29 `thinkingTimeMs` and the
  away/detour/idle fields are present on every answer (missing on 68% of
  all-time answers); `retrievability` is missing on 0.5%. `recordedAt`
  matches `answer.createdAt` to within 0.4 s.
- **Relation (slice 2): nothing supported at 65 days.** All 24 after-probe
  rows and all 24 all-day rows are "not supported"; intervals are about
  ±0.25 wide. Largest: accuracy × surprise −0.23 [−0.45, 0.02],
  lapseCount × pace −0.23 [−0.48, 0.03] (all-day −0.25 [−0.50, 0.00]).
  85 days are needed to detect |rho| = 0.3. Slice 3's gain comparison
  applies only to supported rows, so it has none on the real data; its
  fixture proof still stands. Slice 6 carries the days-needed proposal and
  the near-boundary rows.
- **Answer span.** Starting an answer `thinkingTimeMs` before `createdAt`
  clipped distraction share on 4 of 65 days; adding `awayMs + detourMs`
  (product's `useThinkingTimeTracker` pauses on away/detour) clips none.
- **Gain (slice 3): no meaningful gain.** All 24 rows, n = 64. The probe
  lowers a baseline's leave-one-day-out error on 5 rows, by at most about
  3% (for example variability × distraction share 0.133 → 0.129); the
  comparison has no gain threshold or uncertainty, so slice 6 treats these
  as noise. Weekday-and-hour adjustment predicts worse than the overall mean
  on 5 of 6 measures (overfitting at 64 days); yesterday's value helps only
  for median response time (1.69 s against 2.07 s).
- **Module layout.** Markdown helpers live in `analysis/markdown.py`,
  `HOUR_BANDS` in `recall_export.py`; `recall_probe_analysis.py` is near
  250 lines, so slices 4 and 5 add their sections in their own modules.
  Method lines live in `method()` and print with the `inventory` section.
- **Sessions (slice 4).** Probe period 2026-08-29 to 2026-11-03: 67 days
  with reviews (one without a probe), 349 sessions, 7,317 reviews. Per day:
  median response time 17.9 s, distraction share 0.141, 140 min, pace 1.03
  reviews per active minute, 5 sessions. Response time barely moves with
  weekday (16.9–19.9 s) or position (18.4 s first ten, 17.9 s later);
  session length and distraction vary more by weekday (Tuesday 175 min and
  0.201; Monday 0.064) and start hour (06:00–08:59 sessions longest, n 91).
  The first-ten distraction ratio is driven by a few long absences.
  Each section module owns its Method text (`session.METHOD`).
- **Repetition (slice 5).** Probe period: 7,135 reviews in calibration.
  Raw retrievability under-predicts below 0.8 (observed about 0.80 at
  predicted 0.44–0.60) and over-predicts at ≥ 0.95 (0.855 against 0.983);
  the 0.8–0.9 band (5,139 reviews) matches. Story example 8's premise does
  not hold: only 17% of `AGAIN` reviews sit below 0.7 (13% all time). Too
  early 4% (all time 44%), too late 2%. No review is graded `EASY` or
  `HARD`; `GOOD` stands in for the easy grade. MCQ is under-predicted and
  SPELLING over-predicted. The judgment interval ignores repeated notes, so
  it is somewhat too narrow. The section reports the probe period and all
  time (from 2023-07-24) separately.
- **Findings and proposals (slice 6).** The report answers the three
  questions: no probe–recall relation at 65 days; session and repetition
  efficiency described (Findings 4–8); Proposals 1–3 ready to queue (keep
  collecting to 85 usable days, around 2026-11-23, rerun with the owner's
  usual timezone; probe reliability by split halves, which the export's
  per-trial data supports), Proposals 4–6 conflict with ADR 0003. Open
  owner questions in the report: the usual timezone, and whether the
  calibration gaps become a separate story. The report's Evidence line names
  commit e82f6044 for the scripts, which leave with this plan folder at
  wrap-up.
