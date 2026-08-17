# CONTEXT: Requested retention r = 0.9; due is I(r, S)

**Scope:** Close B2 as a **locked global constant**, not a Settings knob. Map requested retention in ADR 0003 and in code. Every **graded** due uses `I(r, S)`. No UI. No persisted `r`. No parameter, setter, or test that varies `r`.

Do not accept ADR 0003. Do not parallel [015](../015-fsrs-scheduling-followup/PLAN.md) (both touch `MemoryTracker`).

## Locked this session

| Decision | Lock |
|----------|------|
| Requested retention `r` | **0.9 globally**. Not Settings. Not UI. Not a DB column. Not a method argument. |
| Interval | After a **grade**, `nextRecallAt = lastRecalledAt + I(r, S)`. Open FSRS: `I(0.9, S) = S` (whole hours). Implementation is a named identity, not a general `I(r, S)` that takes `r`. |
| Ordinary incorrect / New fail **+12h** | **Removed.** Fail due is `I` of the updated Stability. New fail (S=0) → `I` non-positive → existing **24h** strictly-future fallback. |
| Confusion | Still not a grade; due never later. Projection is `last + I` (same hours as `last + S` at r=0.9). |
| Assimilate / overlap | Unchanged (not grades). Assimilate S=0 stays due now. |
| In-flight 12h retries | **No backfill.** Next grade applies the new due rule. No Flyway. |
| Varying `r` | Forbidden in this plan: no knob, no tests at 0.85 / 0.95, no dead `r` parameter. |

## What is already true at r = 0.9

Success, Tutor 3/4/5, Tutor 0/1/2, and confusion projection already set due from updated Stability (`last + S` = `last + I(0.9, S)`).

The leftover is `MemoryTracker.recallFailed`: Again memory then **+12h**, including New fail.

## Concept in code (no flexibility)

`Fsrs.REQUESTED_RETENTION` (0.9) and `Fsrs.intervalHours(stabilityHours)` returning `Math.round(stabilityHours)` because `I(0.9, S) = S`. `calculateNextRecallAt` uses that. Comment the identity; do not compute the general invert-R formula.

## Out of scope

Accept ADR 0003. B4 / C4 / E3 / E4 / E6. Settings / UI for `r`. First-rating initials. Same-day w17–w19. Delete `DEFAULT_SPACES`. 015 leftover apply-path collapse.
