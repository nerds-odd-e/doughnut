# Plan: FSRS-6 first-rating initials

**Status:** in progress

**Goal:** First mapped **success** on a New tracker uses published FSRS-6 `S0(G)` / `D0(G)` (own implementation, frozen `Fsrs.W`). Destination: Proposed [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) Decision.

Humans already chose the lock below. This plan only sequences it. Do not accept ADR 0003 in this plan.

## Locked for this plan

- First Good / Hard / Easy on New use `S0(G) = w[G−1]` days → whole hours, and `D0(G)` clamped to `[1, 10]`.
- `D0(Easy)` stays **unclamped** as the later mean-reversion target; persisted first Easy Difficulty is **1**.
- Elapsed time and thinking time do **not** change first-rating. Overdue extra does not apply.
- New Again / Tutor **0/1/2** stay New (S=0, D unset, due +24h). Out of this plan.
- Already-graded rows are not backfilled. No Flyway.
- Strictly-future fallback stays **24 hours**, named separately from first-success Stability.
- Difficulty **5** remains only as the fallback when S>0 and D is null.

Rounded hours (frozen W): Good **55**, Hard **31**, Easy **199**. Pin Difficulty to the Java float from `D0(G)`.

## Out of this plan

E3 fuzz / max interval, E4 fitting, New Again init, card states, `DEFAULT_SPACES` (needed for `V300000260` replay).

## Slices

### 1. Lock first-rating in ADR 0003

- **Type:** Structure
- **Status:** done

Lock lives in Proposed ADR 0003 **First rating on New**. Gap tracker points here as in-progress (not a second policy map). First Good and Easy are S0/D0 in code; Hard New still D=5/S=24 until slice 4.

**Learning:** ADR cross-refs (commissioned New 3/4/5, elapsed-0, thinking time, mapped-grade D) now point at that section; product behavior is unchanged.

### 2. First Good on New uses S0(Good) / D0(Good)

- **Type:** Behavior
- **Status:** done

Ordinary correct / just review Yes / Tutor **4** on New: Stability **55**, Difficulty **`2.118104f`** (`D0(3)`), due +55h.

**Learning:** Init is `Fsrs.initialDifficulty` / `initialStabilityHours` from `ForgettingCurve.afterGoodRecall`. Same-hour short-term E2E seeds a graded S=24 tracker (`/api/testability/seed_graded_memory_tracker`). On-time second score waits +55h.

### 3. First Easy on New uses S0(Easy) / D0(Easy)

- **Type:** Behavior
- **Status:** done

Tutor **5** on New: Stability **199**, Difficulty **1**, due +199h. New Hard still D=5 / S=24. Good path unchanged.

**Learning:** New Good/Easy share `ForgettingCurve.firstRating(grade)`. Hard still uses leftover 24/5 until slice 4.

### 4. First Hard on New uses S0(Hard) / D0(Hard)

- **Type:** Behavior
- **Status:** planned

**Pre:** New commissioned tracker.  
**Trigger:** Tutor **3**.  
**Post:** Stability **31**, Difficulty `D0(2)` (API number), due +31h.

Extend the first-score **3** unit pin. Add one commissioned E2E scenario for first score 3 (feature already owns first 4/5).

Then remove the leftover New-success D=5/S=24 branch. Keep `FIRST_SUCCESS_STABILITY_HOURS` only if it is still the 24h strictly-future fallback — rename so it is not “first success.” `DEFAULT_DIFFICULTY` stays as null-D fallback on S>0.

Update the gap tracker / SEED-004 / STATE: first-rating closed; remaining FSRS knobs still E3/E4 plus human accept of ADR 0003.

**Done when:** all three first-success initials are FSRS-6; no D=5/S=24 success init; fallback is not named as first success; no new dead weights path (`w[0]` still unused — New Again is later).

## Tests (capability-owned)

| Capability | Where |
|---|---|
| Ordinary / just-review first Good | `SpacedRepetitionCorrectRecallSchedulingTest`, `spaced_repetition.feature` |
| Commissioned first 3/4/5 | `LearningSessionRecordTutorFeedbackTests`, `commissioned_learning_session.feature` |
| Due-queue after n strict Goods | `RecallServiceWithSpacedRepetitionAlgorithmTest` (retiming only) |

No product types named after this plan number.
