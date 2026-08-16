# Plan: Close remaining FSRS scheduling gap

**Status:** in progress  

**Goal:** Leftover Tutor scores and confusion stop walking the spacing ladder; ADR 0003 Decision holds the full Tutor map plus remaining locks/defers. Proposed stays Proposed.

Locked: [CONTEXT.md](./CONTEXT.md). Capability names only. No Flyway.

Tests: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`. When Gherkin changes: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/learning_session/commissioned_learning_session.feature`. Confusion slice: also the accidental-match controller tests.

Each slice: lock its ADR 0003 Decision paragraph first, then TDD, then drop leftover for **that** score only. Wrap-up per `planning.mdc` (Jidoka, refactor, plan update, commit+push). Do not accept the ADR.

---

### 1. Tutor score 3 is FSRS-6 Hard
Type: Behavior  
Status: done

On-time Hard S **71** (< Good 102), Hard next-D **8.0177937**, overdue S **97**. New 3 shares D=5/S=24 with New 5. Record tests split: `LearningSessionRecordTests` vs `LearningSessionRecordTutorFeedbackTests`. Leftover `applyScore` is 2/1/0 (`standardIncrementHours` removed with case 3).

---

### 2. Tutor score 1 is Again memory, due from Stability
Type: Behavior  
Status: done

On-time second score 1 (after 4): S **8**, Again next-D **10**, due = last + new S. New 1 still D unset / S=0. Ordinary incorrect reuses Again memory then **+12h**. Leftover `applyScore` is 2 and 0.

---

### 3. Tutor score 0 matches score 1
Type: Behavior  
Status: done

On-time second score 0 equals score 1 (S, D, due) on one notebook (Hola=1 vs Gracias=0 after both scored 4). New 0 still strictly-future. Leftover `applyScore` is score **2** only.

---

### 4. Tutor score 2 shrinks Stability (not Hard)
Type: Behavior  
Status: planned

**Pre:** Commissioned tracker with S > 0.  
**Trigger:** Record Tutor score **2**.  
**Post:** Next S = rounded 80% of current S (accumulated above assimilate 0). D unchanged. Due from new S.

- ADR: named Doughnut exception. Not FSRS Hard. No elapsed/R. No overdue extra.
- Last leftover `applyScore` case. Delete `CommissionedLearningSessionFeedbackPolicy` when it has no callers.
- Controller: after score 4 (S=24), score 2 → S=19, D unchanged. New score 2 stays S=0, D unset.

---

### 5. Confusion shrinks Stability without the ladder
Type: Behavior  
Status: planned

**Pre:** Accidental match with one eligible secondary tracker, S > 0.  
**Trigger:** Grade the spelling answer.  
**Post:** Secondary S is the Again-midpoint (CONTEXT). D / `lastRecalledAt` / `recallCount` unchanged. Due not later. Strictly weaker than Again on the same state.

- ADR: confusion formula in Decision (replace “current rules”).
- **Jidoka** if midpoint is rejected.
- Update `RecallPromptAccidentalMatchConfusionAdjustmentTests` to the new S; keep non-grade and due-not-later pins. Stop calling `hoursAfterSpacingDelta`.

---

### 6. Strictly-future fallback is 24 hours
Type: Structure  
Status: planned

Enables slice 7: live code must not use `hoursFromSpacingIndex(1)`.

- Replace commissioned fallback with `FIRST_SUCCESS_STABILITY_HOURS` (24). Same observable as today.
- ADR E2: non-positive interval → 24h.
- Existing tests still pass; no new schedule numbers.

---

### 7. Drop live leftover ladder
Type: Structure  
Status: planned

Enables slice 8: no live scheduling on `DEFAULT_SPACES`.

- Remove `hoursAfterSpacingDelta` and leftover policy if still present. Keep `DEFAULT_SPACES` / `hoursFromLegacyIndex` / `StabilityIndexToHoursBackfill` for `V300000260` replay.
- `SpacedRepetitionAlgorithmTest` keeps legacy conversion; drop live ladder-step tests.
- Thinking-time `LEGACY_INDEX_STEP` stays unless unused.

---

### 8. Finish ADR 0003 locks and spent docs
Type: Structure  
Status: planned

No schedule change. Immediate next behavior: none (policy complete; humans still own accept).

- Move remaining Working draft that is locked (commissioned shared rules, C1 map, C3, D2, E1, relearning = 12h only) into Decision. Explicit defer: B2 knob, B4, C4, E3, E4, E6. Shrink Working draft. Do not accept.
- Collapse spent discussion in `FSRS-COMPATIBILITY-GAP.md` / SEED-004 / STATE to the final map + deferred list. Delete leftover “3/2/1/0 still ladder” text.

---

## Stop-safe

| Stop after | User-visible |
|------------|----------------|
| 1 | Score 3 is Hard; 2/1/0 still leftover |
| 2 | Score 1 Again due from S; 0 still resets; 2 still leftover |
| 3 | 0 and 1 schedule the same |
| 4 | Tutor leftover gone; confusion still ladder |
| 5 | Confusion FSRS-aware, not a grade |
| 6–7 | Same schedule; live ladder gone; migration helpers remain |
| 8 | Same schedule; ADR/docs match code |

## Not this plan

Accept ADR 0003. Settings `r`. RecallLog / fitting. Relearning steps. Just-review Hard/Easy. Lapses. Fuzz / max interval. Plan 013. Flyway. Delete `V300000260` helpers.
