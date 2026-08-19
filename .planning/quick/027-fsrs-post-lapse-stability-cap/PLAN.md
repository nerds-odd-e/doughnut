# Plan: Post-lapse After-Again Stability cap

**Status:** in-progress  
**Goal:** Long-term Again (elapsed **≥ 24**) uses open FSRS `S' = min(Sf, S)` so a fail cannot lengthen Stability. Going forward only.

## Decisions (accepted 2026-08-19)

- Elapsed **≥ 24**, S > 0: compute post-lapse `Sf`, then **`S' = min(current S, max(1, round(Sf)))`** in whole hours.
- Cap **long-term only**. Short-term After-Again (`elapsed < 24`) unchanged.
- Overdue Again may still leave **more** remaining S than on-time, as long as `S' ≤ S` (keep `e^{w14(1-R)}`).
- Confusion inherits that Again S (no special case). When the cap binds, midpoint is current S (no Stability change).
- **No Flyway.** RecallLog does not store pre/post S.
- Lock in Proposed ADR 0003 **Decision**. Reject: allow post-lapse `Sf` to exceed current S.
- Out of this plan: E4, accept ADR 0003, `S_MIN`, `INIT_S_MAX`.

## Discovery

026 leftover cohesion is **shipped and deleted** (`8571c5e264`): short-term Again 0h/23h live with other G; elapsed **24** on 72h / D=5 is post-lapse **15** (ADR + `twentyFourHourAgainRecallUsesPostLapseStability`). Post-lapse `Sf` is still **uncapped** (`FsrsAgainRecall.hoursAfterPostLapse`).

With frozen default FSRS-6 weights, `Sf > S` is rare. Usual pins stay under the cap (72h / D=5 elapsed **24** → **15**; on-time → **17**; overdue 2×S → **18**; first Good **55h** → **15**). The distinguishing fixture is small S, low D, long elapsed: **S=5h, D=1, elapsed 8760** (365 days) → uncapped **6**, capped **5**. Product assimilate/grade cannot reach that edge in a short E2E; pin it on `MemoryTracker` (same Again boundary as the other incorrect-recall tests). Existing E2E already covers ordinary Again shrink.

## Slices

### 1. Lock post-lapse cap in ADR 0003

**Type:** Structure  
**Status:** done

Proposed ADR 0003 Decision: elapsed **≥ 24** post-lapse is `S' = min(current S, max(1, round(Sf)))` (formula in **Incorrect recall**; commissioned **1** and elapsed-time pins point at it). Cap pin **5h** / D=**1** / elapsed **8760** stays **5h**. Options reject uncapped `Sf` and capping short-term After-Again. Pointer docs wait for slice 3.

**Learning:** formula lives once in Incorrect recall; do not restate it in every Decision heading.

### 2. Year-overdue Again on 5h / D=1 stays Stability 5

**Type:** Behavior  
**Status:** planned

**Pre:** Graded tracker, Stability **5**, Difficulty **1**.  
**Trigger:** ordinary incorrect (Again) at elapsed **8760** hours.  
**Post:** Stability **5** (capped), not uncapped **6**.

TDD on `MemoryTrackerIncorrectRecallSchedulingTest` (existing `markAsRecalled` / Again helper). Unique assertion is Stability **5**. Do not re-assert on-time **17** / **15**, overdue-more-than-on-time, or Difficulty. No new E2E (edge path; ordinary Again shrink already in `spaced_repetition.feature`).

Implement only in the post-lapse hour path (`FsrsAgainRecall` / `hoursAfterPostLapse`): after round + 1h floor, `min` with current Stability. Short-term branch untouched. Tutor **1** / just review No / confusion pick up the same Again S.

Keep `overdueIncorrectRecallLeavesMoreRemainingStabilityThanOnTime` and the elapsed-**24** → **15** pin (both stay under the cap).

**Done when:** the new unit is green; on-time / short-term Again pins still green; `pnpm backend:test_only` green.

### 3. Close post-lapse cap in the FSRS tracker

**Type:** Structure  
**Status:** planned

No schedule change.

- [FSRS-COMPATIBILITY-GAP.md](../../research/FSRS-COMPATIBILITY-GAP.md), [SEED-004](../../seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md), [STATE.md](../../STATE.md): post-lapse cap is **closed**. Remaining deferred is still **E4** plus **accept ADR 0003**. Trackers today omit the cap (they still jump to E4); this slice names it closed, not newly invented.

**Done when:** trackers match the shipped rule; they do not claim E4 or ADR accept closed.

## Not this plan

- 026 leftover cohesion (done: window pins + elapsed-**24** → **15**)
- Tutor same-hour score **1** or a confusion midpoint pin
- Flyway backfill or RecallLog replay
- Accept ADR 0003; E4 fitting; `S_MIN`; `INIT_S_MAX`
- Flyway squash / `StabilityIndexToHoursBackfill`
