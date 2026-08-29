# Daily probe follow-up

**Status:** shipped.
**Type:** ad-hoc plan (`.planning/quick/`)
**Depends on:** shipped `.planning/quick/007-daily-cognitive-probe/PLAN.md`
**Measurement spec:** [daily-probe-protocol.md](../../notes/daily-probe-protocol.md)

Canonical name: **Daily probe** / `daily_probe` / `DailyProbe` (ADR 0001 / 0003).

KeepAlive abandon writes nothing; PATCH without `dailyProbeEnabled` leaves the
opt-in unchanged; Continue waits for Saved (failed persist shows Retry);
failed GET `/today` shows Retry instead of a blank recall page; turning the
probe off omits its Recall Stats series (history rows stay). Overlapping
persist/summary tests and the test-only `aggregateRows` overload are gone.

Left for later (not this plan): Java-side scoring from `trials_json` if 008
needs trustworthy history; unique local-day constraint; probe-only “Daily
trends” heading. Analyses: `.planning/quick/008-probe-convergent-analyses/`.
