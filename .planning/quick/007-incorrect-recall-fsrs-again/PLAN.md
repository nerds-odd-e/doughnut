# Plan: Ordinary incorrect recall uses FSRS-6 Again

**Status:** in progress (slices 1–6 done)  
**Goal:** S > 0 incorrect recall persists FSRS-6 post-lapse Stability (D, S, R) and Again Difficulty; due time stays +12h. Lock D1 in ADR 0003 Decision. No Accept.

**Context:** [CONTEXT.md](./CONTEXT.md)

Sequential. Each slice is one Behavior, stop-safe, ~one commit. Do not Accept ADR 0003. Do not delete `DEFAULT_SPACES`.

Canonical post-conditions (12h due, lastRecalledAt / recallCount) are asserted **once** in slice 1. Later slices assert only their delta.

**Learnings:** On-time S pin **17h** (S=72, D=5). Again D pin **5 → 10**. Frozen `W`/R/next-D live in `Fsrs`. Slice 2/7 E2E: first-success S=24 then fail; open understanding Memory Tracker from the note (just-review has no answered-question page). Incorrect unit tests: `SpacedRepetitionIncorrectRecallSchedulingTest`.

---

### 1. On-time incorrect (S > 0) persists post-lapse Stability

Type: Behavior  
Status: done

On-time incorrect at S=72h/D=5 persists **17h** (FSRS-6 post-lapse), not −2 ladder. `nextRecallAt` stays +12h. New fail stays S=0 + 12h. ADR 0003 Decision **Incorrect recall (Again)** locked; Status still Proposed. Floor and next-D not locked.

---

### 2. Memory Tracker shows post-lapse Stability after incorrect

Type: Behavior  
Status: done

Assimilate → just-review Yes (day 1) → due No (day 2) → understanding Memory Tracker: remaining Stability > 0 and 12h Last-to-Next. Scenario in `spaced_repetition.feature` (`@mockBrowserTime`). No production change.

---

### 3. Harder Difficulty leaves less remaining Stability on incorrect

Type: Behavior  
Status: done

D=3 vs D=8 siblings, same S, on-time incorrect: harder remaining Stability is strictly less. Production unchanged (post-lapse already uses D). Test in `SpacedRepetitionIncorrectRecallSchedulingTest`.

---

### 4. Overdue incorrect leaves more remaining Stability than on-time

Type: Behavior  
Status: done

Elapsed = 2S leaves strictly more remaining Stability than elapsed = S (same D). Production unchanged (R already in post-lapse). Shared `overdueGradeTime` helper.

---

### 5. Incorrect from 1-hour Stability persists at least 1 hour

Type: Behavior  
Status: done

Graded S=1h fail persists Stability ≥ 1 (`FsrsAgainRecall` floors at 1h). New fail stays 0.

---

### 6. Incorrect recall persists Again Difficulty

Type: Behavior  
Status: done

Graded S>0 fail persists Again next-D: **5 → 10**. Unset D matches a D=5 sibling. New fail leaves D unset. ADR 0003 Again next-D locked; Status still Proposed.

---

### 7. Memory Tracker shows Again Difficulty after incorrect

Type: Behavior  
Status: planned

**Pre-condition:** Same success-then-fail path as slice 2.  
**Trigger:** Open Memory Tracker after that incorrect.  
**Post-condition:** Difficulty is the Again number (5 → 10). Stability/12h already covered — do not re-assert.

- Extend the slice 2 scenario (one extra Then).
- Wrap-up: fail path must not call `hoursAfterSpacingDelta`; D1 resolved in the gap doc; shrink ADR Working draft; SEED-004 remaining = confusion/commissioned leftover ladder. Do not Accept ADR 0003.

**Done when:** E2E Difficulty green; fail off the ladder; D1 closed in ADR/gap/seed.
