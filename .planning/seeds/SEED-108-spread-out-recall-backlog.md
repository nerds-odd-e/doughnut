---
id: SEED-108
status: active
planted: 2026-11-08
scope: story
---

# Spread out my recall backlog in one click

<a id="spread-out-recall-backlog"></a>

### Spread out my recall backlog in one click

**Identity:** SEED-108#spread-out-recall-backlog
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/084-spread-out-recall-backlog/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"3880d890f92e122240d84a0c459502a0d9ba3948043bfbb2ac3ea37e0fddbf19","plan":"cd0605f23e889b0568f1c894fcb2cb41c6e8c9b7f8ba91211bda580d45fac06e"}}
```

**Goal** A Donut learner returning after a busy period to hundreds of due recall items can, with one click in Settings, get back to a manageable recall workload today and see what is due over the next 30 days. This turns a demotivating backlog into a daily load they can resume, while the items that suffer most from waiting stay first in line.

**Scope**

Required behavior:

- One action in Settings redistributes the learner's recall backlog immediately (no preview step) and then shows the resulting forecast.
- **Backlog** = the learner's recall-queue items (the same items the recall page shows, spelling included) whose due time is before the end of today in the learner's local time zone. Items due later today count as backlog too, so "today" means the whole local day, not the current half-day recall window.
- **Window** = today plus the next 30 local calendar days. Items already due on those 30 days keep their dates and count toward each day's load.
- **Fill rule (water-filling):** choose one daily level so that today keeps up to that level and each of the next 30 days is filled up to it from the backlog; a day already above the level keeps what it has and receives nothing. When the backlog does not divide evenly, the earlier days take the extra item. When today's backlog is already at or below the level, nothing moves.
- **Priority:** backlog items are assigned in order of how much they suffer from waiting: New (never graded) trackers first, then ascending Stability. Today keeps the highest-priority items at their existing due times; the rest are assigned to days in priority order.
- A moved item becomes due just after local midnight starting its assigned day, so it appears in that day's first recall window and not in the evening window before it. An item due exactly at local midnight already shows in the previous evening's recall, so each local day counts items due after its start, up to and including the next midnight.
- Only due time changes. Stability, Difficulty, and RecallLog are untouched, and a later Grade schedules from the actual elapsed time as usual (an overdue success may gain more Stability).

Forecast (shown after the action): today's remaining count, the average per day over the next 30 days, and the count for each of those 30 days. Counts reflect the schedule at the moment of redistribution; follow-ups from recalls, new assimilation, and changes made after it are not predicted.

Deferred promises (not undertaken now; not rejected):

- Showing the forecast before redistributing, or a preview/confirm step.
- Undoing a redistribution.
- A choice of window length or target load.
- An entry point on the recall page, or automatic redistribution.
- A forecast that predicts follow-ups or new assimilation.
- Redistributing commissioned memory trackers (they are not in the daily recall queue).

**Key examples**

1. Even spread (from the original request): 520 items due today, 100 already scheduled evenly over the next 30 days, no day above 20 → after the action, 20 remain due today and the forecast shows 20 on each of the next 30 days (average 20).
2. A heavy future day keeps its load: 360 due today, 50 already due on day 3, nothing else scheduled → today keeps 12, each of the other 29 days gets 12, day 3 stays at 50; the forecast's average is 13.3.
3. Nothing to spread: 15 due today, each of the next 30 days already has 20 → nothing moves; the forecast shows 15 today and 20 on each day.
4. Priority: the backlog holds a tracker graded yesterday with Stability of about 1 day and one with Stability of 200 days, and only one more item fits today → the low-Stability tracker stays due today; the 200-day one moves to a later day.
5. Local days: a learner in UTC+8 clicks at 09:00 local; an item moved to tomorrow is not in this evening's recall list and is in tomorrow morning's.

**Investigation notes** (facts with sources)

- Due work is `next_recall_at` ≤ end of the current local half-day; observed 2026-11-08 at 35f8a63b43 with a scratch controller test (Asia/Shanghai, 18:00): an item due at 00:00 tomorrow was in the evening list, one due at 01:00 was not (`UserService.getMemoryTrackersNeedToRepeat`, `TimestampOperations.alignByHalfADay`); the browser sends the time zone with each request.
- The recall queue excludes commissioned and removed trackers and unavailable notes (`MemoryTrackerRepository.byUserIdFrom`).
- Under FSRS, for the same delay a lower-Stability memory loses more Retrievability, and an overdue success may gain more Stability (ADR 0003, `Fsrs`) — this supports the priority rule.
- Excluding a past attempt rebuilds the tracker's due time from RecallLog (`MemoryTrackerReplay`), so a redistributed due time is not reproducible from RecallLog.
- Settings already has a Recall Stats tab; nothing in the product redistributes due times today.

**Architecture**

- **Scheduling policy (decided by Terry, 2026-11-08):** amend Accepted ADR 0003 as part of this story to name a learner-initiated **backlog redistribution**. It changes only due time, never Stability or Difficulty; it is not a RecallLog event; and a rebuild after an excluded attempt reschedules the tracker from its memory state, not keeping the redistributed date. Calendar-day placement belongs to this action only; Grade scheduling stays free of calendar-day boundaries. Add the term to ADR 0001 if it becomes product language.
- The backlog boundary is the end of today in local time, the same time zone the recall page uses.
