# Overdue correct recall — Stability hours

**Status:** in progress  
**Source:** ADR 0003 B3; vertical slice; no day-list leftover; small stop-safe commits.

## Design

- **Stability** = current interval in whole hours. After a grade, `nextRecallAt = lastRecalledAt + stability`. Assimilate may use `stability = 0` (due now).
- **Retrievability** is computed from elapsed hours and Stability.
- Day list is used only to **convert existing rows**, then it is gone (scheduler, Settings, API, DB).
- Until the overdue slice, overdue correct still equals on-time.
- Policy tests assert **next interval hours**.

**B2 is not this plan** (no retention Settings).

## Slices

| # | Type | Status | Capability |
|---|------|--------|------------|
| 1 | Structure | done | ADR 0003 states Stability hours |
| 2 | Structure | done | Glossary names Stability |
| 3 | Behavior | done | Memory tracker field is `stability` |
| 4 | Behavior | planned | No spaced-repetition day list in Settings or User API |
| 5 | Behavior | planned | Stability is whole hours; day-list column dropped |
| 6 | Behavior | planned | Overdue correct lengthens Stability more than on-time |

Stop after any slice: product still schedules. After 4, learners cannot edit a day list; stored tables still apply until 5. After 5, spacing is hours only.

---

### 1. ADR 0003 states Stability hours

**Type:** Structure  
**Status:** done

Proposed ADR 0003 is present-tense domain: persisted **Stability** (whole hours), Retrievability computed, overdue extra locked, Doughnut-owned shape, remaining gaps one behavior at a time. Status still Proposed. Accidental-match, overlap, whole-hour Decisions kept.

**Learning:** leftover “memory strength” in accidental-match is now Stability. File trimmed to 250 lines. Slice 2 still owns glossary 0001.

---

### 2. Glossary names Stability

**Type:** Structure  
**Status:** done

ADR 0001 glossary: **Stability** (hours) and **Retrievability** (computed). Removed **spaced-repetition schedule** as user interval list.

**Learning:** keep glossary meanings in domain language (no code identifiers). Slice 3 owns the field rename.

---

### 3. Memory tracker field is `stability`

**Type:** Behavior  
**Status:** done

Field/column/JSON/UI is **`stability` / Stability**. Numbers still the old index scale. Migration `V300000259`. `ForgettingCurve` algorithm class unchanged. No E2E for the old label.

**Learning:** unused duplicate `ReviewPointBuilder.ts` deleted. Slice 5 still converts values and may replace `ForgettingCurve`.

---

### 4. No spaced-repetition day list in Settings or User API

**Type:** Behavior  
**Status:** planned

**Pre-condition:** General Settings and User JSON still expose `spaceIntervals`.

**Trigger:** Open Settings / read current user.

**Post-condition:** No day-list field in Settings or User API. Learners cannot customize a table.

**Change:** remove Settings control, `UserDTO.spaceIntervals`, OpenAPI user field, related validation/UI tests. `User.spaceIntervals` stays on the entity for the scheduler (and slice 5 migration). `@JsonIgnore` is the temporary API hide — special case until slice 5 drops the column.

**Stop-safe:** recall still uses each user’s stored table. Testability `space_intervals` may remain until slice 5.

**Tests:** Settings / UserDTO tests; regen SDK if the user type changes.

---

### 5. Stability is whole hours; day-list column dropped

**Type:** Behavior  
**Status:** planned

**Pre-condition:** Field is already `stability` (index scale). `space_intervals` still on `user` for conversion.

**Trigger:** Migrate, then any grade or assimilate.

**Post-condition:** `stability` is whole hours (the current interval). `nextRecallAt = lastRecalledAt + stability` after a grade. No `space_intervals` column, no testability day list, no `SpacedRepetitionAlgorithm` table lookup. `ForgettingCurve` (or its replacement) updates hours. Assimilate still due now (`stability = 0`). Incorrect still 12h retry. Early correct still grows less than on-time. Overdue correct still **equals** on-time. Thinking time and commissioned scores adjust **hours** with the same qualitative rules.

**Change:** Java Flyway: `stability = repeatInHours(oldValue, user.space_intervals)` then drop `space_intervals`. Algorithm in hours. Backend scheduling tests assert hours. Frontend shows the new numbers (field name already `stability`).

No SDK field rename in this slice. No Settings UI work (slice 4).

**Tests:** `SpacedRepetitionRecallSchedulingTest`, early-recall tests, commissioned score tests, confusion/overlap — next interval hours, not index.

---

### 6. Overdue correct recall lengthens Stability more than on-time

**Type:** Behavior  
**Status:** planned

**Pre-condition:** Two equal trackers, same Stability and thinking time; one graded at elapsed = Stability, one at elapsed > Stability.

**Trigger:** Correct recall.

**Post-condition:** Overdue next interval (and Stability) is **strictly longer**. Extra converges (not linear unbounded). Same elapsed hours + different `nextRecallAt` still match.

**Tests:** add beside existing MemoryTracker scheduling tests. E2E only if the testability clock can show the longer gap without a new harness.

**Production:** success update when `elapsedHours > stability`. No new columns.

---

## Jidoka

- Slice 4 then 5: do not leave a JsonIgnored `space_intervals` in production longer than needed; 5 drops it.
- Slice 3 then 5: do not convert hours before the field is named `stability`.
- Commissioned 0–5 → hour adjustments land in slice 5.
- Slice 3 is the generated-SDK rename; slice 5 is the value migration. Keep them separate commits.
