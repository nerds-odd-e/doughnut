---
id: SEED-100
status: active
planted: 2026-11-03
planted_during: owner request to export recall history with daily probe data, then analyze it
trigger_when: the owner wants to study recall-session efficiency, repetition efficiency, or how the daily (morning) probe relates to recall results
scope: medium
---

# SEED-100: Export recall history to analyze recall and the morning probe

## Why This Matters

The owner wants to learn from their own recall data: how efficient their
recall sessions are, how efficient the spaced repetition is, and whether the
morning daily probe result says anything about how recall goes that day. The
recall stats page shows a few summary tiles and downloads the full history
as JSON for analysis (`GET /api/user/recall-history-export`). The goal is to find interesting patterns and
ways to make the morning probe more useful.

Existing data, as of main at `8930764691`:

- Recall history: `recall_log` (recorded time, elapsed hours, grade,
  stability and difficulty before, retrievability, tutor feedback), linked to
  `answer` (created time, thinking time, away/detour/idle times and counts,
  outcome, exclusion) and `recall_prompt` (question type, created time),
  through each `memory_tracker`.
- Daily probe: `daily_probe` (completed time, speed, accuracy, lapse count,
  variability, and per-trial JSON). The probe is offered once per local day,
  so a user's timezone matters.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery.
Story 2 reads the recall history export; it is not a product change.

<a id="story-2"></a>

### Find how the morning probe relates to recall, and how to make the probe more useful

**Identity:** SEED-100#story-2
```json dough-story-dependencies
{"schemaVersion":1,"identity":"SEED-100#story-2","dependencies":[{"supplier":{"identity":"SEED-100#story-1","href":"seeds/SEED-100-recall-history-export.md#story-1"},"implementation":"The analysis reads the owner's data through the export that story 1 delivers (endpoint or download).","rationale":"The analysis needs the complete recall history together with daily probe data; no existing product path gives that data out, and querying the production database directly is not the agreed way to get a user's data.","condition":"A signed-in user can export their entire recall history with their daily probe results from production.","state":"satisfied","resolution":{"revision":"1e5c2b8e1b4d054c05799f81ff235d9519e4c968","path":".planning/seeds/SEED-100-recall-history-export.md#story-1","summary":"Satisfied on 2026-11-03: the Application Release workflow for tag v1.3.66 (b5e061cc3d, which contains the accepted integration 51f844a1d5) completed with 'Admit selected application release: success' and 'GCP deploy (GCS + MIG + health probe): success' (run created 2026-11-03T03:49:35Z), and https://doughnut.odd-e.com/api/healthcheck answered 'OK. Active Profile: prod. Commit: 51f844a1d5ec2dec604f1ed1da17cbe3b5916a89' when checked during SEED-100#story-2 refinement. That build serves GET /api/user/recall-history-export?timezone=<zone> and the recall stats tab download, so a signed-in user can export their entire recall history with their daily probe results from production.\nAccepted integration: 51f844a1d5ec2dec604f1ed1da17cbe3b5916a89 on origin/refs/heads/main.\nCondition satisfied: A signed-in user can export their entire recall history with their daily probe results from production.\nAccepted integration: 51f844a1d5ec2dec604f1ed1da17cbe3b5916a89 on origin/refs/heads/main."}}]}
```
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/002-morning-probe-and-recall-analysis/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"0982c5ecb877f56cd722126da43e5774d4633ee8643c01bd69f18b8d77e5c726","plan":"9bedfb49e4a3b9771a89e19dd01b61384aed575b7ce5ac9258f8215539845ae9"}}
```

**Goal:** The owner (the only analysed user) gets one dated analysis report of
their own production recall history and daily probe results, written by an
agent, that answers three questions with stated evidence: does the daily probe
result relate to that day's recall results; how efficient are the owner's
recall sessions and spaced repetition; and which concrete probe changes, each
tied to a finding, would make the probe more useful. The owner then decides
which probe changes to queue as stories. This story changes no product
behavior; its value is the report.

**Scope:**

- Input: one export of the owner's production data from
  `GET /api/user/recall-history-export?timezone=<zone>` (the recall stats tab's
  "Download recall history" button saves the same JSON). The owner hands the
  JSON file over at execution start, or supplies a personal access token with
  which the agent fetches it; execution cannot start without one of these.
  The report records the export's `exportedAt` and `timezone`. The whole
  export is in that one timezone (Donut stores no per-event zone), and the
  report says so as a limitation.
- Data inventory first: number of recall events by kind
  (`ANSWERED_RECALL_PROMPT`, `JUST_REVIEW`, `TUTOR_FEEDBACK`), answered prompts
  with excluded answers (by reason), number of probe days, probe completion
  time-of-day distribution, and the number of days with both a probe and
  answered prompts after it. Every later result states the sample it rests on.
- Probe-to-recall relation: for each probe measure (speed, accuracy, lapse
  count, variability) and each same-day recall measure, report the strength
  with an uncertainty interval, the number of days, and a plain verdict:
  supported, not supported, or too few days to tell. "That day's recall" means
  answered prompts recorded after the probe's completion on the same local
  day; the all-day variant is a sensitivity check. Recall measures: correct
  share, surprise against the scheduler's prediction (actual correctness minus
  `retrievability`, so days with easy or hard item mixes compare fairly),
  median thinking time, distraction share (away, detour, idle time over the
  session span), session length, and pace (answered prompts per active
  minute). Definitions follow the product's own rules so the report matches
  the recall stats tiles (ADR 0003 readouts): an answer counts as correct on
  grade `GOOD` or outcome `OVERLAP`, wrong on `AGAIN`, and is not counted
  otherwise (`Answer.correctFrom`); `OVERLAP` and excluded answers are not
  reviews; surprise is the product's Accuracy residual
  `sum(y - p) / sqrt(sum(p (1 - p)))` over raw `retrievability`, without the
  tiles' per-type recalibration, and the report says so. Compare the probe against two baselines: the owner's typical day
  (overall average, and by weekday and time of day) and the previous day's
  recall, so "useful" means better than what the owner already knows.
- Recall-session efficiency on its own: thinking time, distraction share,
  session length and pace, and how these move with time of day, weekday, and
  session position (first prompts versus later ones).
- Repetition efficiency on its own: calibration of `retrievability` against
  actual correctness by retrievability band, the share of reviews that look
  too early (high retrievability, easy grade) or too late (`AGAIN` at low
  retrievability), and how `elapsedHours` relates to grade.
- Proposals: each names the finding it rests on and the probe change it
  implies (for example offer timing, length, which number to show, pairing
  with recall feedback, or keep collecting with a stated number of days).
  An inconclusive relation still yields proposals: at least the number of days
  needed to detect the smallest effect worth acting on, given the observed
  variance.
- Report home: `.planning/notes/recall-and-morning-probe-analysis.md`, beside
  the probe protocol note, with the date and method inside. It holds aggregates
  only: no note ids, note titles, or tutor feedback text. The raw export is
  never committed.
- Analysis scripts live in the story's plan folder while the story is open and
  leave with it at wrap-up; the report names the commit that holds them, so
  Git history is the evidence. (The owner asked on 2026-11-03 that anything
  built to support the analysis be cleaned up; the earlier "kept as evidence"
  wording is met through that commit.)
- Tooling: the system Python 3 has pandas 2.3.3 and scipy 1.13.1 (checked
  2026-11-03); the Nix shell has neither. The scripts run with system Python
  and are not part of any product build or lint.

**Required restriction:** ADR 0003 (Accepted) says the daily probe is not a
scheduling input and does not validate Pace, Accuracy, or Consistency. A
proposal that would make it one is reported with that conflict named and
left to the owner's ADR decision; the report does not present it as a
ready-to-queue story.

**Scope limits (deferred, not rejected):** no product change, including the
probe itself and the recall stats page; no in-product analytics tile; no
prospective experiment with changed probe settings. Each may become a story
from the proposals. Other users' data is not analysed.

**Key examples:**

1. Export with 60 probe days, 48 of which have answered prompts after the
   probe → analysis runs → the report's relation table says "48 days" for
   those rows and gives, per probe measure, the strength, its interval, and
   the verdict.
2. A day with a probe but no recall after it → the day is counted in the
   inventory and left out of the relation.
3. A probe completed at 15:10 local time → it is still that local day's probe;
   the report's time-of-day distribution shows how often the probe was not a
   morning probe, instead of assuming "morning".
4. An answered prompt whose answer has `excludedAt` → left out of correctness,
   surprise, and thinking-time measures; counted in the inventory with its
   `exclusionReason`.
5. `JUST_REVIEW` and `TUTOR_FEEDBACK` events → not answered prompts, so they do
   not enter correctness; `JUST_REVIEW` still counts as session activity for
   length and pace.
6. Probe speed correlates with same-day surprise, but no better than the
   previous day's recall does → the verdict for that row is "supported, but
   no gain over yesterday's recall", and no proposal rests on it alone.
7. Only 12 usable days → every relation row says "too few days to tell", and
   the proposals include the number of days to keep collecting before the
   question can be answered.
8. Reviews graded `AGAIN` cluster at retrievability below 0.7 → the
   repetition-efficiency section reports the calibration band and whether the
   scheduler's prediction matched.
9. After the report lands and the story wraps up → the notes file remains, the
   plan folder with the scripts is gone, and `git grep` over product code,
   tests, and docs finds nothing added by this story.

**Refinement findings (2026-11-03):**

- *Explore.* Four ways compared against the goal: (A) offline scripts over
  the export, (B) an in-product analytics tile on the recall stats tab, (C)
  reading the JSON with a language model and no scripts, (D) a prospective
  experiment varying probe settings for several weeks. Recommended A: no
  product change, reproducible, and the only one that can state sample sizes
  and strength honestly before anyone knows whether a relation exists. B is a
  likely later proposal if a relation holds; C cannot support the required
  sample-size and strength statements; D answers "more useful" more directly
  but needs product changes and weeks, so it is a proposal candidate.
- *Borrow.* Forecast verification from weather forecasting: a forecast is
  judged by its skill over reference forecasts (climatology, persistence),
  not by whether it correlates with the outcome. Mapping: probe measures are
  the forecast; that day's recall surprise is the outcome; the owner's typical
  day is climatology; yesterday's recall is persistence. Adapted into scope as
  the two baselines. Where it breaks: a weather forecast has a fixed lead time
  and the outcome does not depend on the forecaster's schedule, whereas the
  probe's lead time varies by day and the day's recall outcome depends on
  which items happened to be due. Surprise against `retrievability` reduces
  the second problem but does not remove it.
- *Challenge.* The purpose rests on three assumptions: the probe measures a
  state that affects recall that day; enough probe days with recall after
  them exist; day-level recall varies beyond its item mix. The strongest
  doubt is sample size: the probe has existed since 2026-08-29, so at most
  about 66 probe days exist, and a weak relation at that size will come out
  as "not supported" whether or not it is real. The report cannot be allowed
  to meet its wording by saying "no relation found" and stopping; hence the
  inventory, the "too few days" verdict, and the data-collection proposal are
  required scope, not options. A second doubt, that "morning" is assumed, is
  checked by the time-of-day distribution (example 3).
- *Investigate.* Facts: the export is in production (healthcheck reported
  commit `51f844a1d5` on 2026-11-03 after Application Release v1.3.66
  succeeded; the story-1 dependency is recorded satisfied); the export's
  fields are those of `RecallHistoryExport` (events with grade, kind,
  elapsed hours, stability and difficulty before, retrievability, answer
  timing and distraction fields, outcome, exclusion; probes with speed,
  accuracy, lapses, variability, trials); the probe is offered once per local
  day, inside the recall page, and only completed runs are stored
  (`.planning/notes/daily-probe-protocol.md`); the daily probe table dates
  from 2026-08-29 (`V300000305__create_daily_probe.sql`). Hypotheses the data
  inventory settles at execution start: enough probe days with recall after
  them (H1), probes mostly before that day's recall (H2), day-level recall
  varying beyond item mix (H3). No local observation can settle them before
  the owner's export is in hand.

**Rough effort:** M–L.
