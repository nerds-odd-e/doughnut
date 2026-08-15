# Plan: Difficulty on correct recall

**Status:** in progress (slice 1 done; next is 2)  
**Goal:** Persist Difficulty; ordinary correct recall follows FSRS-6 Good (SInc + D-update); New-card first correct inits D. First S = **24h** this plan (12h parked).

**Context:** [CONTEXT.md](./CONTEXT.md)

Each slice is one Behavior or one Structure, stop-safe, one commit. Sequential (same `ForgettingCurve` / `MemoryTracker`). Slice 1 is Structure **only** for slice 2 — do not stop after 1 (unused column).

---

### 1. Persist Difficulty

Type: Structure  
Status: done

**Unlocks slice 2.** No schedule change.

Shipped: nullable `memory_tracker.difficulty` (`V300000261`); graded backfill 5; New/assimilate-only unset; `@JsonIgnore`; `makeMe.difficulty(float)`; persist tests in `MemoryTrackerShowControllerTest.DifficultyPersistence` (assigned 7, not backfill 5). ADR 0003 stays Proposed. ERD unchanged (PK/FK only).

**Learning:** adding a `@JsonIgnore` field on `MemoryTracker` still shifted SpringDoc `Mcq` property order; synced YAML/types via `generateTypeScript`. No API shape change.

---

### 2. On-time correct recall uses FSRS SInc

Type: Behavior  
Status: planned

**Pre-condition:** Tracker with S > 0, D = 5 (backfill or default).  
**Trigger:** Ordinary on-time correct recall.  
**Post-condition:** Next S is FSRS-6 Good SInc (days internally, hours persisted), not `hoursAfterSpacingDelta`. `nextRecallAt = lastRecalledAt + stability`. SInc ≥ 1. Drop the success-path ladder and linear elapsed/S increment.

Put SInc next to `ForgettingCurve` (pass D). Do not add it to `SpacedRepetitionAlgorithm`. Commissioned standard increment stays ladder-based.

Same `succeeded()` path: rewrite Fibonacci-pinned **unit** assertions in this slice so CI stays green (on-time, early, overdue/B3 qualitative, thinking-time qualitative, `RecallServiceWithSpacedRepetitionAlgorithmTest` 60/72). If `spaced_repetition.feature` schedule scenarios go red, tag those scenarios `@wip` here (cap 5); do not rewrite day lists yet (slice 6).

S = 0 still yields **24h** (current first rung).

If on-time growth at D = 5, S ≈ 3d is a 10× jump vs Fibonacci, **Jidoka** (do not retune weights ad hoc).

**Done when:** success path does not walk the ladder; backend unit tests green; fail/confusion/commissioned still ladder; E2E schedule scenarios either still pass or are `@wip`.

---

### 3. Harder Difficulty grows Stability less

Type: Behavior  
Status: planned

**Pre-condition:** Two trackers, same S, same elapsed whole hours, same thinking time; D differs (e.g. 3 vs 8).  
**Trigger:** Same ordinary correct recall.  
**Post-condition:** Higher D → strictly smaller next S. Both SInc ≥ 1.

Delta-only test on `recalledSuccessfully`. No UI.

**Done when:** that delta is locked; slice 2 canonical case is not re-asserted.

---

### 4. Correct recall updates Difficulty

Type: Behavior  
Status: planned

**Pre-condition:** Graded tracker with a known D.  
**Trigger:** Ordinary correct recall (Good).  
**Post-condition:** Persisted D follows FSRS-5/6 (ΔD then mean reversion, clamped `[1, 10]`). For Good, ΔD = 0; D only nudges toward Easy-init.

Delta-only: D after the grade is the FSRS next-D, not left sticky.

**Done when:** that update is locked; SInc from slice 2 still holds.

---

### 5. First correct recall initializes Difficulty

Type: Behavior  
Status: planned

**Pre-condition:** Newly assimilated tracker (S = 0, D unset, due now).  
**Trigger:** First ordinary correct recall.  
**Post-condition:** D = 5, S = 24h, `nextRecallAt` = that instant + 24h, `lastRecalledAt` advances. Assimilate still writes no grade and leaves D unset.

**Done when:** first-success fixtures go through this init. 12h first interval is **not** this slice (parked).

---

### 6. Spaced-repetition E2E follows FSRS success intervals

Type: Behavior  
Status: planned

**Pre-condition:** Slice 2–5 green in unit tests; schedule scenarios `@wip` if tagged.  
**Trigger:** `spaced_repetition.feature` day-at-08:00 assimilate/recall (just-review correct).  
**Post-condition:** Expected note lists match FSRS Good + 24h first interval (not Fibonacci). Remove `@wip`. `bazaar_subscription.feature` day-2 recall of day-1 assimilates still holds (24h later still due).

Targeted: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/spaced_repetition.feature`.

**Done when:** those scenarios pass without `@wip`; seed no longer says “index + user day table”; leftover ladder paths documented as not this plan.

---

## Parked / leftover (not this plan)

- First success **S = 12h** (revisit; E2E first rung stays 24h)
- B2 requested-retention **knob** (`r ≠ 0.9`)
- D1 post-lapse S / incorrect 12h retry
- C2 Tutor 2
- RecallLog / fitting
- Delete `DEFAULT_SPACES` (fail / confusion / commissioned)
