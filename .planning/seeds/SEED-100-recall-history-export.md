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
recall stats page shows a few summary tiles, but there is no way to get the
full history out for analysis. The goal is to find interesting patterns and
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
Story 2 needs story 1's export; it is not a product change.

<a id="story-1"></a>

### Export my entire recall history with my daily probe results

**Identity:** SEED-100#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Beneficiary and outcome:** A signed-in learner can download one file that
holds their entire recall history and all their daily probe results, so they
(or an agent working for them) can analyze recall-session efficiency,
repetition efficiency, and the link between the morning probe and recall.

**Initial scope (to confirm in refinement):**

- One download for the current user only, reachable from the recall stats
  page and by an authenticated API endpoint an agent can call.
- Format: one JSON document, chosen because probe trials and recall events
  are nested data. It carries a format version, export time, and the user's
  timezone; timestamps are ISO-8601 with offset so "morning" and "local day"
  can be worked out.
- Recall events: every recall log with its grade, timings, scheduling state
  before the recall (stability, difficulty, retrievability, elapsed hours),
  answer timings (thinking, away, detour, idle), outcome, exclusion, question
  type, and a stable memory tracker and note id so repetitions of the same
  item can be grouped. No note content beyond what analysis needs (refinement
  decides whether note titles are included).
- Daily probes: every probe with its summary values and its trials.
- A user with no probe data still gets their recall history.

**Out of scope:** any analysis, charts, or derived statistics; importing data;
exporting other users' data.

**Rough effort:** M.

<a id="story-2"></a>

### Find how the morning probe relates to recall, and how to make the probe more useful

**Identity:** SEED-100#story-2
```json dough-story-dependencies
{"schemaVersion":1,"identity":"SEED-100#story-2","dependencies":[{"supplier":{"identity":"SEED-100#story-1","href":"seeds/SEED-100-recall-history-export.md#story-1"},"implementation":"The analysis reads the owner's data through the export that story 1 delivers (endpoint or download).","rationale":"The analysis needs the complete recall history together with daily probe data; no existing product path gives that data out, and querying the production database directly is not the agreed way to get a user's data.","condition":"A signed-in user can export their entire recall history with their daily probe results from production.","state":"waiting"}]}
```
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Beneficiary and outcome:** The owner gets a dated analysis report of their
own data, made by an agent using story 1's export (through the endpoint or the
end-to-end download), that says whether the morning probe result relates to
that day's recall results, lists other interesting findings, and proposes
concrete changes that would make the probe more useful. The owner can then
decide which probe changes to queue as stories.

**Initial scope (to confirm in refinement):**

- Use the owner's real export from production, with the export time recorded.
- Relate each day's probe (speed, accuracy, lapses, variability) to that
  day's recall results (accuracy, thinking time, distraction time, session
  length and pace), and also look at recall-session efficiency and
  repetition efficiency on their own.
- State sample sizes and how strong each relationship is; say plainly when a
  result is not supported by the data.
- Proposals for improving the probe, each tied to a finding.
- The report and the analysis scripts are kept as evidence, outside product
  code.

**Out of scope:** changing the product, including the probe itself; the
proposals become separate stories only if the owner chooses.

**Rough effort:** M–L.
