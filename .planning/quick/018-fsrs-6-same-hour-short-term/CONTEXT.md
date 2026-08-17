# Same-hour FSRS-6 short-term next Stability

**Status:** in progress (slices 1–2 done)  
**Policy:** Proposed [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) (do not Accept)

Locked for this plan (human, 2026-08-17):

- Apply only when elapsed **whole hours == 0** and **S > 0**. Not elapsed 1–23h, not calendar same-day.
- **Success grades only:** Good / Hard / Easy (ordinary correct, Tutor **4 / 3 / 5**). Again stays post-lapse.
- Clamp **SInc ≥ 1** for Hard / Good / Easy. No Settings knob. No schema/data migration.
- Tutor **2** and confusion unchanged. New (S = 0) stays first-success init `D = 5`, `S = 24h`.

Open FSRS-6 (S in days, persist whole hours):

`S' = S · e^{w17 · (G − 3 + w18)} · S^{-w19}`

With frozen `Fsrs.W` and rounding: Good 24→**25**; Easy 24→**43**; Hard 24 stays **24** (clamp); Good at 72h stays **72** (clamp).
