# CONTEXT: Commissioned Tutor score 5 as FSRS-6 Easy

**Seed:** SEED-004  
**Policy:** Proposed ADR 0003 (lock only the score-5 Decision; do not accept the ADR)  
**Gap tracker:** `.planning/research/FSRS-COMPATIBILITY-GAP.md`

## Locked for this plan (do not reopen)

1. Replace Working-draft “+20% above the ladder increment” with open-FSRS-6 **Easy** (own implementation). Not Good-plus-20% hours.
2. **New** (S=0, D unset) + score 5 initializes **D=5, S=24h** — same as first ordinary correct and score 4. Not FSRS Easy first-rating `S0(G=4)≈w3` days / `D0(Easy)`.
3. Score 5 **inherits** overdue bounded extra growth (low R vs Stability, not `nextRecallAt`).

Also inherit: no FSRS library; whole hours; effort neutral; no 12-hour ordinary-incorrect retry; `nextRecallAt = lastRecalledAt + stability`; queue lateness is not an input; formulas stay out of the ADR.

## Out of scope

Tutor 3/2/1/0, confusion, B2 retention knob, relearning steps, RecallLog, dropping `DEFAULT_SPACES`, Flyway, rewriting historical score-5 rows.

## Code today

- Score **4** → `MemoryTracker.recalledSuccessfully` (Good SInc + Good next-D + New D=5/S=24).
- Score **5** → `CommissionedLearningSessionFeedbackPolicy.applyScore` (ladder `+1.2` step). New: S=29h, D unset. From S=24/D=5 on-time: S=53h, D unchanged.
- Frozen weights: `Fsrs.W`. Easy SInc multiplies the Good increment term by `w16`. Easy next-D is `Fsrs.nextDifficulty(d, 4)`.

## Pins (frozen `w`, S=24h, D=5, on-time elapsed=24h)

| After | Stability | Difficulty |
|-------|-----------|------------|
| On-time score 4 (already) | 102 | 5.0014133 |
| On-time score 5 | **169** | **~1.985** |
| Overdue score 5 (elapsed=48h) | **253** | (D slice may already have run; compare S only) |
