# Same-hour short-term leftover cohesion

**Status:** in progress (slice 1 done) 
**Policy:** Proposed [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) (do not Accept)

Leftover from shipped same-hour FSRS-6 short-term success (elapsed whole hours 0, S > 0). No new user-facing scheduling rule.

## Inspection (018 implementation)

**Not bugs (formula matches Decision pins):** Good 24→25; Easy 24→43; Hard 24 stays 24; Good 72 stays 72. New still inits 24. Again stays post-lapse. E2E same-hour Good shows 25 (`yesIRemember` ticks 11s, so thinking time rounds to the same whole hour as no thinking time).

**Missed post-change-refactor**

- Shotgun: `elapsedInHours == 0` → `Fsrs.hoursAfterShortTermRecall` is copied in `FsrsGoodRecall`, `FsrsEasyRecall`, and `FsrsHardRecall`.
- Naming: `immediateEarlyCorrectDoesNotGrow` still describes the old “no extra success increment” rule. Same-hour Good *does* grow at S=24; at S=72 it only holds because SInc < 1 is clamped.
- Redundant / weakened test: `thinkingTimeAdjustmentCombinedWithSameHourRecall` also asserts 0ms same-hour > base. Realistic 10s no longer distinguishes (1-hour increment rounds away). Unique remaining claim is on-time > same-hour at base thinking time.

**Keep (not redundant)**

- Unit Good 24→25 (sub-hour remainder) and E2E Stability 25 (do-more-recall path). Different boundaries.
- E2E “25 hours between last and next” (Memory Tracker card).
- Hard 24 clamp vs Good 72 clamp (different grade and S).
- No extra HTTP same-hour Easy pin — `recalledEasily` is the scheduling boundary commissioned score 5 already calls.

## Out of scope (value decision)

Slow thinking time after same-hour Good can round the +1 hour increment back to 24 (`Math.max(current S, …)`). ADR allows thinking time after a correct outcome. Do not floor next S at the short-term result unless a human asks.
