# Spread out my recall backlog in one click

Work item: **SEED-108#spread-out-recall-backlog**.
Source: [refined story](../../seeds/SEED-108-spread-out-recall-backlog.md#spread-out-recall-backlog).
**Source review:** [source-review.md](source-review.md)

## Goal and scope

A learner returning to a large recall backlog uses one action in Settings to
spread the backlog over today and the next 30 local days. Each day is filled
up to one shared level (water-filling); days already above the level keep
their load. New and low-Stability trackers stay earliest. The learner then
sees the forecast: today's remaining count, the 30-day average, and each
day's count.

**Included:** a backend action that moves only `next_recall_at` of
recall-queue trackers due by the end of today, and returns the forecast; the
Settings action and forecast display; an amendment to ADR 0003 naming
backlog redistribution.

**Excluded (deferred in the seed):** a preview before applying, undo, a
configurable window or target, a recall-page entry point, automatic
redistribution, predicting follow-ups or assimilation, and commissioned
trackers.

## Architecture

- **PFE:** Reuse the recall-queue selection (`MemoryTrackerRepository`
  `byUserIdFrom`: non-commissioned, not removed, available notes), local-day
  helpers `TimestampOperations.getStartOfDay` / `getStartOfNextDay`, the
  browser `timezone` parameter (`TimezoneUtils.parseTimezone`), and
  `testabilitySettings.getCurrentUTCTimestamp()`. Nothing in the product
  redistributes due times or forecasts future load today, so the fill rule
  and forecast are new.
- **Placement:** the action is recall scheduling, so its endpoint belongs on
  `RecallsController` (`/api/recalls/...`), with the rule in a recall service
  rather than in `MemoryTracker` grade transitions. The UI lives on the
  existing Settings **Recall Stats** tab (no new route; ADR 0005 unaffected).
- **ADR 0003 (Accepted):** Terry decided on 2026-11-08 to amend it. A
  learner-initiated backlog redistribution changes only due time, never
  Stability or Difficulty. It is not a RecallLog event, and a rebuild after an
  excluded attempt reschedules from memory state. Calendar-day placement
  belongs only to this action. Slice 1 writes the amendment (Terry remains
  the decision maker; the text records his decision).
- **North Star:** no topic; the change is local to recall scheduling.

## Current decisions

- **Local day bucket:** day D holds trackers with
  `start(D) < next_recall_at ≤ start(D+1)`. This matches the due query's `≤`
  at the half-day window end (see premise P1). Today's bucket is everything
  `≤ start(tomorrow)`, overdue included.
- **Placement:** a moved tracker gets `next_recall_at = start(D) + 1 hour`
  (local), inside D's morning window and after the previous evening's.
- **Level:** the smallest integer level L such that filling today and each
  of the 30 days up to L absorbs the backlog; when it does not divide evenly,
  earlier days take the extra. Days above L receive nothing. If today's
  backlog is ≤ L, nothing moves.
- **Priority:** New (never graded) trackers first, then ascending Stability.
  Today keeps the first items at their existing due times; the rest are dealt
  to days 1…30 in order.
- **Forecast average:** sum of days 1…30 after redistribution ÷ 30 (today
  excluded).

## Key examples → proof

| Promise (seed key example) | Slice | Proof |
| --- | --- | --- |
| 1. Even spread: backlog fills today and the 30 days to one level | 1 | Controller test: due-today trackers plus evenly scheduled future ones → today and every day at the same level; forecast agrees |
| 2. Heavy future day keeps its load, others share the level | 1 | Controller test: a day above the level keeps its count and gets no moved tracker; average reflects it |
| 3. Nothing to spread when today ≤ level | 1 | Controller test: no `next_recall_at` changes; forecast shows the existing counts |
| 5. Moved item: not in tonight's recall, in tomorrow morning's | 1 | Controller test via `recalling(...)` at 18:00 and next 09:00 in Asia/Shanghai |
| Only due time changes (Stability, Difficulty, RecallLog untouched) | 1 | Same controller test asserts the unchanged fields and RecallLog count |
| 4. Low-Stability/New trackers stay earliest | 2 | Controller test: a New tracker and a Stability ≈ 1 day tracker stay today; the Stability 200 days tracker moves |
| Settings action shows the forecast and the recall count drops | 3 | E2E `e2e_test/features/recall/spread_out_recall_backlog.feature`; frontend spec in `frontend/tests/pages/settings/RecallStatsSettingsTab.spec.ts` for the forecast display |

## Decisive premises

| # | Premise | Consuming operation | Observation | Result |
| --- | --- | --- | --- | --- |
| P1 | An item due exactly at local midnight is already in the previous evening's due list; one due at 01:00 is not | Slice 1 bucket and placement decisions; example 5 | Scratch controller test in this workspace at 35f8a63b43 (2026-11-08), `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test -Dspring.profiles.active=test --tests '*ScratchLocalMidnightDueTest'`, using `RecallsController.recalling("Asia/Shanghai", 0)` at 18:00 then 09:00 next day; reverted | `eveningCount=1` (00:00 item), `morningCount=2`: settled, decisions above follow it |
| P2 | The recall queue is `byUserIdFrom` (excludes commissioned, removed, unavailable) and the recall page and menu both read `getDueMemoryTrackers` | Slice 1 selection; slice 3 recall count after the action | Read `MemoryTrackerRepository`, `RecallService.getDueMemoryTrackers`, `UserController` menu data (2026-11-08); slice 1/3 tests observe it end to end | Settled by the same controller path the slices test |
| P3 | `RecallsControllerTestBase` + `makeMe.aMemoryTrackerBy(user).nextRecallAt(...)` build due trackers, and Stability can be set for example 4 | Slices 1–2 proof setup | P1's scratch test used that base and builder (2026-11-08); `MemoryTrackerBuilder.stabilityAndNextRecallAt(float)` exists, followed by `.nextRecallAt(...)` to set the due time | Settled |
| P4 | Local E2E route with the reused steps (`my daily new notes to assimilate is set to`, `On day {int} I self-review … and assimilate new …`, `It's day {int}`, `the menu should show {int} note to recall`) runs in this workspace | Slice 3 E2E | `SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/recall/spaced_repetition.feature` (2026-11-08, this workspace at 35f8a63b43) | 4 passing, 0 failing, exit 0: settled; the reused setup steps run in this workspace |

## Slices

### 1. Spreading the backlog evenly over today and the next 30 local days
Type: Behavior
Status: done
Proof: New `backend/src/test/java/com/odde/donut/controllers/RecallsControllerSpreadBacklogTests.java`
covering examples 1, 2, 3 and 5 plus "only due time changes" (use small
counts that show the same rule); run with
`CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test -Dspring.profiles.active=test --build-cache --tests '*RecallsControllerSpreadBacklogTests' --tests '*RecallsController*'`.

Behavior: Pre — a learner has recall-queue trackers due by the end of today
and some due over the next 30 days. Trigger — `POST /api/recalls/spread-out-backlog?timezone=…`.
Result — backlog trackers beyond today's level move to the earliest days below
the level (placed at local start of day + 1 h); future-scheduled trackers keep
their dates; the response is the forecast (today's count, 30-day average,
30 dated day counts). Only `next_recall_at` changes. Within this slice the
backlog order is the existing due order. Slice 2 replaces it with the
priority order (interim behavior).

Also amend `docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md`
(an `Amended:` line and a short "Backlog redistribution" subsection with the
decision above). If the term becomes UI language, add it to ADR 0001's Recall
glossary.

### 2. Keeping the memories that suffer most from waiting first
Type: Behavior
Status: planned
Proof: Add example 4 to `RecallsControllerSpreadBacklogTests` (New tracker,
Stability ≈ 24 h, Stability ≈ 4800 h; today's level leaves room for two) and
rerun that class.

Behavior: Pre — the backlog mixes New, low- and high-Stability trackers.
Trigger — spread the backlog. Result — New trackers first, then ascending
Stability, decide who stays today and who gets earlier days.

### 3. Spreading the backlog from Settings and seeing the forecast
Type: Behavior
Status: planned
Proof: E2E `e2e_test/features/recall/spread_out_recall_backlog.feature`.
Daily new notes set to 3, Notes 1–3 assimilated on day 1, then day 10: the
learner spreads the backlog from Settings → Recall Stats. The forecast shows
1 today and 1 on each of the next two days, and the menu shows 1 note to
recall. Run with
`SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/recall/spread_out_recall_backlog.feature`.
Add a focused case in `frontend/tests/pages/settings/RecallStatsSettingsTab.spec.ts`
for the forecast rendering, and run
`CURSOR_DEV=true nix develop -c env -u NODE_ENV pnpm -C frontend test RecallStatsSettingsTab`.
Regenerate the API client first (generate-api-client skill).

Behavior: Pre — the learner has a backlog. Trigger — the action on the Recall
Stats tab. Result — the forecast appears (today, average, per-day counts) and
the recall count everywhere reflects today's new load.

## Plan review

Refinement not needed: three slices each own one example group and one proof
loop. Slice 1 bundles the ADR amendment because the amendment is about the
due-time change that slice introduces. The examples exercise one water-filling
rule plus one ordering; no special cases accumulate. No remaining
slice-specific concerns found in this review.

## Learnings

- P4: the local E2E route and the reused recall setup steps pass in this
  workspace (spaced_repetition.feature, 4/4, 2026-11-08).
- P1: the due query includes items at exactly the half-day window end, so
  local-day boundaries must use `(start, next start]` and placement must sit
  after midnight.
- Slice 1 accepted proof (2026-11-08): `RecallsControllerSpreadBacklogTests`
  (6 tests) plus `RecallsController*`, `RobotsTests` after API regeneration;
  rule in `RecallBacklogService.additionsUpToLevel` (fill to L-1, extras one
  each to the earliest days below L, today first). The API client and
  `open_api_docs.yaml` were regenerated in slice 1 because
  `RobotsTests.openApiDocsMatchCommittedYaml` requires it; slice 3 regenerates
  only if the API changes again.

## Story obligations

### G1. Backlog order is the existing due order (interim)
Reported: slice 1 — "Backlog order is the existing due order, as the interim rule says. Slice 2 replaces it with priority order."
Story clause: "backlog items are assigned in order of how much they suffer from waiting: New (never graded) trackers first, then ascending Stability"
Disposition: interim until slices 2
