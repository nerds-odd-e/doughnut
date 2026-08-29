# Daily probe

**Status:** shipped.
**Type:** ad-hoc plan (`.planning/quick/`)
**Measurement spec:** [daily-probe-protocol.md](../../notes/daily-probe-protocol.md)
(not in an ADR).

Opt-in ~60s two-choice Daily probe before recall. Same protocol every day.
Offer is consumed only on completion; an abandoned run writes nothing. Results
are stored as completed rows and shown as a trend on Recall Stats (existing
30/90/All window).

Canonical name: **Daily probe** / `daily_probe` / `DailyProbe` (ADR 0001 /
0003). Follow-on analyses: `.planning/quick/008-probe-convergent-analyses/PLAN.md`.
Follow-up (KeepAlive, PATCH, save-before-continue, offer retry, trend
when off): shipped `.planning/quick/010-daily-probe-follow-up/PLAN.md`.
