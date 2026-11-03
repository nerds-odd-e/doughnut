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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/001-recall-history-export/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"c038fe90988b62e7faf9a34d010510a94da85d79a53ddcde05f266c1788c1cd9","plan":"cfaea5559ab514599f7b15c97f58fec96ca20033090484b0292b28f7babdd57d"}}
```

**Goal:** A signed-in learner can get one JSON file that holds every recall
event they have and every daily probe they took, so they, or an agent working
for them, can study recall-session efficiency, repetition efficiency, and how
the morning probe relates to that day's recall. This is the data source for
[story 2](#story-2); the export itself makes no analysis.

**Scope:**

- **Who and how.** The current user's own data only. Two ways in, one
  response: a download control on the recall stats settings tab, and an
  authenticated `GET` under `/api`, described in OpenAPI, that an agent calls
  with the user's existing API token (`Authorization: Bearer`, as generated on
  the settings page). No new authentication, MCP tool, or CLI command.
- **Timezone.** The request names an IANA timezone, as the recall stats
  request already does; the page sends the browser's. The file records that
  timezone and the export time, and every timestamp in it is ISO-8601 with
  that zone's offset, so "local day" and "morning" can be read directly.
- **Recall events.** One flat list, one entry per recall log, oldest first,
  covering the whole history (no time window). Each entry has: recorded time;
  grade (`AGAIN`/`HARD`/`GOOD`/`EASY`) or `CONFUSION`; the kind of event
  (answered recall prompt, just review, or tutor feedback, with the tutor
  feedback text); elapsed hours, stability, difficulty, and retrievability
  before the event; the memory tracker id and type and the note id, so
  repetitions of one item can be grouped. An event from an answered recall
  prompt also has the question type (MCQ or spelling), the prompt's created
  time, and the answer's id, created time, thinking, away, detour and idle
  times, away and detour counts, outcome, and exclusion time and reason. One
  spelling answer can produce two events (a grade on the tracker under recall
  and a confusion on the note it accidentally named); both carry the same
  answer id. Excluded answers, trackers
  removed from recall, and notes in trash are included; analysis decides what
  to leave out.
- **Daily probes.** Every stored probe, oldest first, with completed time,
  speed, accuracy, lapse count, variability, and its trials (stimulus,
  response, response time, correct), whether or not the daily probe is
  currently enabled.
- **Empty parts.** A user with no probes, or no recall events, gets the same
  file with that list empty.
- **Required restriction.** The request is refused for a caller who is not
  signed in or has no valid token: recall history is private learning data
  (ADR 0001).

**Deferred promises (not rejected):** note titles or other note content;
recall prompts that were shown but never answered; a session id or any other
derived value; CSV or other formats; a format-version field (one consumer,
story 2, reads it once).

**Out of scope:** any analysis, charts, or derived statistics; importing data;
exporting other users' data.

**Key examples:**

1. A learner with 3 answered recall prompts, 1 just review, 1 tutor feedback,
   and 2 daily probes, in `Asia/Bangkok` → downloads from the recall stats tab
   → the file holds 5 recall events and 2 probes in time order, timestamps end
   in `+07:00`, and the file names `Asia/Bangkok` and the export time.
2. Two answers on the same note's memory tracker → exported → both events
   carry the same memory tracker id and note id, the second with the stability
   and difficulty the first one left.
3. A learner who never took a daily probe → exports → recall events are
   present and the probe list is empty.
4. A learner who took probes and later turned the daily probe off → exports →
   the earlier probes are in the file.
5. An answer that was excluded as ambiguous → exported → its event is present
   with its exclusion time and reason.
6. An agent with the learner's API token calls the endpoint with
   `timezone=Asia/Bangkok` → gets the same JSON the download gives; without a
   token → refused, and no data is returned.

**UI:** on the recall stats tab, one control labeled as downloading the recall
history, available whenever the tab is shown, including its empty state. It
saves a `.json` file whose name includes the export date. No preview, no
options.

**Architecture:**

- Read the history through a projection query, as the recall stats page does,
  not by loading `RecallPrompt` entities: hydrating them per row caused the
  production N+1 timeout that `RecallStatsService` was rewritten to avoid. The
  owner's whole history is already read into memory by the stats page through
  that projection, so one in-memory response is expected to fit.
- The export is a read of existing data; no schema change. It sits beside the
  recall stats endpoint and shares its timezone parsing.
- ADR 0005: the endpoint is under `/api` and in OpenAPI; the frontend uses the
  generated client. ADR 0003 owns the probe and the recall terms; the export
  uses those names and claims no relation between them.
- Known limit: Donut stores no timezone per event or per probe, so one
  timezone is applied to the whole history. On days the learner was in another
  zone, "local day" and "morning" in the file can be wrong. Storing a
  per-event offset is a separate decision, not part of this story.

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
