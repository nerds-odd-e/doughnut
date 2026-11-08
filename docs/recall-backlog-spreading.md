# Spreading out the recall backlog

On the Settings **Recall Stats** tab, **Spread out my recall backlog** moves
the learner's recall backlog over today and the next 30 local days in one
click, with no preview, and then shows the **recall forecast**. The policy is
**Backlog redistribution** in
[ADR 0003](adrs/0003-spaced-repetition-scheduling-policy-accepted.md#backlog-redistribution).

## What moves

- **Backlog:** the learner's recall-queue memory trackers (the same trackers
  the recall page shows, spelling included; not commissioned or removed ones,
  and not trackers of unavailable notes) due by the end of today in the
  browser's time zone. Overdue trackers and trackers due later today both
  count.
- **Local days:** day D holds trackers with `start(D) < next_recall_at ≤
  start(D + 1)`. A tracker due exactly at local midnight already shows in the
  previous evening's recall, so it counts on the previous day.
- **Window:** today plus the next 30 local days. Trackers already due on those
  days keep their due times and count toward each day's load.

## Fill rule

The level is the smallest whole number L such that filling today and each of
the 30 days up to L absorbs the backlog. Every day is filled to L − 1 first;
the remaining trackers go one each to the earliest days still below L,
starting with today. A day already at or above L receives nothing. When the
backlog is already within today's share, nothing moves.

Backlog trackers are taken in priority order: New (Stability 0) first, then
ascending Stability, with ties in due order. Today keeps the first trackers at
their existing due times; the rest are dealt to days 1…30 in that order. A
moved tracker becomes due at local start of its day plus one hour, so it
appears in that day's first recall window and not the evening before.

Only `next_recall_at` changes. The action records no RecallLog event.

## Forecast and recall count

The forecast shows today's remaining count, the average per day over days
1…30 (today excluded), and each of those days' counts, as scheduled at the
moment of the action. It does not predict follow-ups or new assimilation.
After the action, the frontend replaces today's due queue
(`useRecallCatchUp.replaceDueRecalls`) so the menu's recall count reflects the
new load even when the recall page is not mounted.
