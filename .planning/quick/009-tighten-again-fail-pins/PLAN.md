# Plan: Tighten Again fail pins

**Status:** in progress (slices 1–2 done)  
**Index:** not in `.planning/STATE.md` — ad-hoc only; do not write it into project state.  
**Goal:** Make the 007 Again path’s unique claims unprovable from a no-op or a missing floor. Align Proposed ADR 0003 general Stability bullets with the locked Again Decision. No Accept.

**Context:** [CONTEXT.md](./CONTEXT.md)

Sequential. Each slice is one Behavior or Structure, stop-safe, ~one commit. Do not Accept ADR 0003. Do not delete `DEFAULT_SPACES`. Do not change 12h due or New-fail S=0.

---

### 1. Memory Tracker shows 8-hour Stability after first-success then incorrect

Type: Behavior  
Status: done

**Pre-condition:** Same just-review Yes (S=24h) then due No path as the existing `spaced_repetition.feature` scenario.  
**Trigger:** Open the understanding Memory Tracker.  
**Post-condition:** Stability is **8** (not merely > 0). Difficulty 10 and 12h Last-to-Next stay — do not re-assert.

Pinned Then to rendered `"8"` via `expectStability`. Deleted `expectRemainingStability`. Wrap-up collapsed labeled-field asserts onto `expectLabeledValue`; dropped “remaining” from the scenario title. No production change. `spaced_repetition.feature` green (4 passing).

---

### 2. New incorrect recall leaves Difficulty unset

Type: Behavior  
Status: done

**Pre-condition:** New tracker (S=0, D unset).  
**Trigger:** Ordinary incorrect recall.  
**Post-condition:** Difficulty is still unset. S=0 and 12h already asserted — do not re-assert.

Delta only: `nullValue()` on Difficulty in `newTrackerIncorrectRecallKeepsZeroStabilityAndTwelveHourDue`. No production change. `pnpm backend:test_only` green.

---

### 3. Incorrect from 1-hour Stability persists 1 hour

Type: Behavior  
Status: planned

**Pre-condition:** Graded tracker, Stability = 1 hour, D=5.  
**Trigger:** Ordinary incorrect recall (on-time).  
**Post-condition:** Persisted Stability is **1** (the floored FSRS result for this fixture).

- Change `incorrectRecallFromOneHourStabilityPersistsAtLeastOneHour` from `≥ 1` to `equalTo(1f)`.
- No production change expected. Do not change the floor in `FsrsAgainRecall`.

**Done when:** the 1h pin is `1f`; `pnpm backend:test_only` green.

---

### 4. General Stability Decision matches Again due-time and New-fail

Type: Structure  
Status: planned

Structure change: Proposed ADR 0003 general **Stability** bullets no longer contradict locked **Incorrect recall (Again)**. Immediate next: any later FSRS slice can read one due-time rule per outcome.

- Qualify: after **correct** recall, `nextRecallAt = lastRecalledAt + stability`; after **ordinary incorrect**, due is grade time + 12h (schedule metadata).
- Qualify: persisted S=0 is forbidden after a grade **except** New fail (S=0 + 12h), already in Again Decision.
- Status stays **Proposed**. Do not Accept. Do not rewrite unrelated Working draft.

**Done when:** those general bullets match Again; Status still Proposed.
