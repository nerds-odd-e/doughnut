# Plan: New is ungraded — complete FSRS-6 first-rating

**Status:** in-progress

**Goal:** **New** means never received a memory-state grade. Every mapped grade on New uses published FSRS-6 first-rating `S0(G)` / `D0(G)` (own implementation, frozen `Fsrs.W`). Existing graded-New rows are repaired to that invariant. Destination: Proposed [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) Decision, glossary in [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md).

Humans already chose the locks below. This plan only sequences them. Do not accept ADR 0003 in this plan.

**Commit grain:** one git commit per slice. Do not bundle slices. Each unit should be ~5 minutes including its targeted tests (`planning.mdc`).

## Locked for this plan

- **New** = `S = 0`, Difficulty unset = **ungraded** (assimilation is not a grade; confusion is not a grade). Not “never succeeded.”
- First Again / Tutor **0/1** on New: `G=1`, Stability **5**, Difficulty `D0(1)` (Java float), due `lastRecalledAt + I` (**5h**).
- First Tutor **2** on New: Hard first-rating (`G=2`, Stability **31**, `D0(2)`), same bucket as Tutor **3**. Shrink 80% stays the exception **only when `S > 0`**.
- After any of those grades, the tracker is no longer New. The next mapped success uses long-term Good/Hard/Easy, not first-rating initials.
- Elapsed time and thinking time still do **not** change first-rating.
- Backfill only still-New rows that already have a memory-state RecallLog. Recompute due from `I`. Do not touch `S > 0`. Do not replay a fail chain. Incomplete logs stay New.
- Strictly-future **24h** is only non-positive `I`, not a New-fail interval.
- Tutor **2** when `S > 0`, confusion, overlap, and never-graded assimilate are unchanged.

Pin Difficulty to the Java float from `D0(G)`, same as first Good / Hard / Easy.

ADR and glossary are updated **incrementally**: a Structure slice locks only what the **immediate next** Behavior will implement. Do not rewrite the whole destination in slice 1.

## Out of this plan

E3 fuzz / max interval, E4 fitting, card-state enum, learning steps, `DEFAULT_SPACES` (still needed for `V300000260` replay), accepting ADR 0003.

## Slices

### 1. Lock first Again on New in ADR 0003

- **Type:** Structure
- **Status:** done

Proposed ADR 0003 Decision now locks First Again / Tutor **0/1** on New to `S0(1)` / `D0(1)` (Stability **5**, due **5h**). Tutor **2** still stay-New. No Flyway / no backfill. ADR 0001 **New** unchanged. Gap tracker points here.

**Learning:** 24h is named as the non-positive-`I` fallback, not a New-fail interval.

### 2. First Again on New uses S0(Again) / D0(Again)

- **Type:** Behavior
- **Status:** done

Ordinary incorrect / just review No / Tutor **0/1** on New go through `afterRecall` first-rating (`S0(1)` **5**, `D0(1)` **6.4133**, due +5h). `recalledAgain` shares `applyRecall`. Tutor **2** still stay-New. Confusion on New still `S = 0`. E2E: `Memory Tracker shows first Again after just-review No on New`.

**Learning:** Prompted accidental-match is Again (reviewed tracker), not matched-note confusion. `MemoryTrackerAgainRecall` is gone. Slice 3 is unblocked: after Again, `S = 5` so the next Good is long-term.

### 3. Success after New Again is long-term Good

- **Type:** Behavior
- **Status:** done

Pin `correctRecallAfterNewAgainUsesLongTermGoodStability`: New → Again → on-time Good is Stability **21**, not `S0(Good)=55`. Production already took the long-term path. Same-hour Good/Easy/Hard pins moved to `SpacedRepetitionSameHourRecallSchedulingTest` so the correct-recall class stays under 250 lines.

### 4. Memory Tracker shows first tutor 0/1 as Again first-rating

- **Type:** Behavior
- **Status:** done

Example rows **0** and **1** on `commissioned_learning_session.feature` “First tutor score on a new tracker…”: Stability **5**, Difficulty **6.4133**, **5h**. Score **2** not added. Cypress that feature: 14 passing.

### 5. Lock Tutor 2 on New as Hard first-rating in ADR 0003

- **Type:** Structure
- **Status:** done

Proposed ADR 0003: New Tutor **2** uses Hard `S0(2)` / `D0(2)` (due **31h**), same first bucket as Tutor **3**. Shrink 80% only when `S > 0`. No Flyway / no backfill. ADR 0001 unchanged.

### 6. First Tutor 2 on New uses S0(Hard) / D0(Hard)

- **Type:** Behavior
- **Status:** done

Tutor **2** on New is Hard first-rating (Stability **31**, `D0(2)`, due +31h); `S > 0` still shrinks 80%. `MemoryTrackerShrinkStability` branches on `isNewlyAssimilated()`. Outline row **2** added. Cypress `commissioned_learning_session.feature`: 15 passing.

### 7. Lock New = ungraded and Again-row backfill in the ADRs

- **Type:** Structure
- **Status:** done

ADR 0001 **New**: ungraded (`S = 0`, Difficulty unset / **N/A**). ADR 0003 (Proposed): going-forward New has no memory-state grade; still-New `AGAIN` / `AGAIN_ZERO` rows **will** be backfilled to `S0(1)` / `D0(1)`, due from `I`. No SHRINK backfill mention. First-success `S > 0` unrestored.

### 8. Backfill still-New Again rows

- **Type:** Behavior
- **Status:** done

`V300000271` + `StillNewAgainFirstRatingBackfill`: still-New + `AGAIN`/`AGAIN_ZERO` → live `S0(1)`/`D0(1)`, due `last_recalled_at + 5h`. Gate `still_new_again_first_rating_backfill` default `1=0`. SHRINK-only New not selected.

### 9. Lock SHRINK-on-New backfill in ADR 0003

- **Type:** Structure
- **Status:** planned

Enables slice 10 only. Still-New rows with `SHRINK` RecallLogs **will** be backfilled to Hard first-rating (`S0(2)` / `D0(2)`, due +31h).

**Done when:** Decision matches slice 10; tests unchanged.

### 10. Backfill still-New SHRINK rows

- **Type:** Behavior
- **Status:** planned

**Pre:** `S = 0`, Difficulty unset, RecallLog has `SHRINK` (no Again-only requirement).  
**Trigger:** Flyway apply (version after slice 8).  
**Post:** `S0(2)` / `D0(2)`, due +31h. Slice 8 rows and `S > 0` unchanged.

Extend the same backfill service + test (positive `SHRINK`; negative Again-already-migrated). New migration file; do not edit slice 8’s committed migration.

**Done when:** graded-New rows in fixtures match New = ungraded; `S > 0` corpus untouched.

### 11. Drop stay-New leftover

- **Type:** Structure
- **Status:** planned

Justified by slices 2–10 already having replaced stay-New. Remove dead New-fail / New-shrink branches if any remain, dual meaning of 24h as “New fail interval,” and tracker/seed copy that says first-rating is closed except E3/E4.

Update [FSRS-COMPATIBILITY-GAP.md](../../research/FSRS-COMPATIBILITY-GAP.md) and [SEED-004](../../seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md): first-rating (all four G, Tutor **2** on New as Hard) is closed; remaining knobs still E3/E4 plus human accept of ADR 0003. `w[0]` is used.

Keep `DEFAULT_DIFFICULTY` only if `S > 0` with null D still exists as a runtime fallback. Do not squash Flyway.

**Done when:** New in code, glossary, ADR, and repaired rows mean the same thing.

## Tests (capability-owned)

| Capability | Where |
|---|---|
| Ordinary / just-review first Again | `SpacedRepetitionIncorrectRecallSchedulingTest`, `spaced_repetition.feature` |
| Success after New Again | `SpacedRepetitionCorrectRecallSchedulingTest` (or incorrect class, one delta) |
| Commissioned first 0/1/2 | `LearningSessionRecordTutorFeedbackTests`, `commissioned_learning_session.feature` |
| Graded-New backfill | backfill service test (`makeMe` + JDBC) |

No product types named after this plan number.
