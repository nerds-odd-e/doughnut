# Plan: Morning probe and recall analysis

**Source:** [SEED-100#story-2](../../seeds/SEED-100-recall-history-export.md#story-2)
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
  `python3 -I -m pytest .planning/slice-plans/002-morning-probe-and-recall-analysis/analysis`
  and `python3 -I .planning/slice-plans/002-morning-probe-and-recall-analysis/analysis/recall_probe_analysis.py <export.json> <section>`.
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
  (`awayMs + detourMs + idleMs` over the span from first prompt to last
  answer), session length (that span), pace (reviews per active minute,
  active = span minus away/detour/idle). A day enters the relation table only
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
Status: planned
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
Status: planned
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
Status: planned
Proof: `test_gain*`: a fixture where the previous day predicts surprise as
well as the probe → "supported, no gain over the previous day"; a fixture
where only the probe predicts → "gain over both baselines"; then the real run
appends the gain column to the relation table.

Behavior: relation table exists → run the gain comparison → each supported
row gains a verdict against the typical-day baseline and the previous-day
baseline from leave-one-day-out mean absolute error; unsupported rows are
left as they are.

### 4. Recall-session efficiency

Type: Behavior
Status: planned
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
Status: planned
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
Status: planned
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

None yet; slice 1 records the inventory counts here.
