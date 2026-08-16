# CONTEXT: Tutor score 5 Easy cohesion

**Scope:** Cleanup of the shipped score-5 Easy work (`acbee22e0a..67fbae9693`). No new schedule behavior.

## Inspection (what is in / out)

**Latent bug (not user-facing today):** `recalledEasily` / `ForgettingCurve` Easy methods have no New-init guards. `succeeded` / `difficultyAfterSuccessfulRecall` return D=5, S=24h when S=0. Easy instead runs FSRS SInc on S=0 (`0^(-w9)` → Inf/NaN → rounded S=0) and Easy next-D (~1.985). `recordFeedback` hides this by sending New score 5 through `recalledSuccessfully`. If that branch is dropped or another caller uses `recalledEasily` on New, first score 5 would persist D≈1.985, S=0, due from the 24h strictly-future fallback.

**Redundant tests**
- E2E “First tutor score 5 … Difficulty 5” is the same observable as score 4’s Difficulty-5 scenario. Unique New score-5 claim vs the old 29h ladder is **S=24h**, already pinned in `firstScoreFiveOnNewPersistsDifficultyFiveAndStability24`.
- `dayThreeDueCommissionedHolaAndGraciasAfterRecordedScores` no longer pins a unique schedule (both +24h). Same day-3 both-due claim as E2E “Recording the tutor's report…”.
- `firstScoreFive…` also asserts `nextRecallAt = last + 24`; that follows from S=24 via `calculateNextRecallAt` (score 4 does not re-assert due).
- `RecallsControllerTestBase.HOLA4_GRACIAS1_REPORT` has no callers.

**Keep (not redundant)**
- Controller New score 5 D=5 **and** S=24 — regression that Easy New is not next-D / S=0.
- `firstScoreOneLeavesDifficultyUnset`.
- On-time S=169 / D=1.9850327 and overdue S=253 vs score-4 siblings.
- E2E on-time second score 5 → Stability 169.

**Not this plan:** leftover Tutor 3/2/1/0 + confusion; grade enum / switch-on-G; splitting files still under 250 lines (`MemoryTracker` 243, `LearningSessionRecordTests` 246); double `setNextRecallAt` after Good/Easy (pre-existing score-4 pattern); Scenario Outline for second-score 4 vs 5.

## Locked

1. New score 5 stays **D=5, S=24h** (ADR 0003). Not FSRS Easy first-rating S0/D0.
2. Subsequent score 5 stays Easy SInc + Easy next-D. No thinking-time tweak.
3. All score 5 goes through `recalledEasily` once Easy has the same New init as Good. Do not keep a scheduling special case.
4. Do not accept ADR 0003.
