# Plan: Tutor score 5 Easy cohesion

**Status:** in progress  
**Goal:** Score 5’s Easy path owns New init (same D=5 / S=24h as Good), and tests pin only unique claims.

Locked: CONTEXT.md. No new schedule numbers. Capability names only.

Tests: `pnpm backend:test_only`; if E2E Gherkin changes, `pnpm cypress run --spec e2e_test/features/learning_session/commissioned_learning_session.feature`.

---

### 1. Easy path initializes New like first successful recall
Type: Structure  
Status: done

Easy New init mirrors Good (`FIRST_SUCCESS_STABILITY_HOURS` / `DEFAULT_DIFFICULTY`). All score 5 goes through `recalledEasily`. Special-case scheduling helper removed. First score 5 still D=5, S=24h.

---

---

### 2. Drop overlapping score-5 tests; pin Stability 24 in E2E
Type: Structure  
Status: planned

Structure change: tests only. Immediate next Behavior: none (cleanup of shipped work). Existing tests still pass; unique claims remain.

- E2E “First tutor score 5 on a new tracker sets Difficulty to 5” → assert **Stability 24** (unique vs score 4’s Difficulty 5). Do not also re-assert Difficulty 5.
- Delete `dayThreeDueCommissionedHolaAndGraciasAfterRecordedScores` (E2E recording scenario already lists both notes on day 3).
- Remove unused `RecallsControllerTestBase` leftovers (`HOLA4_GRACIAS1_REPORT`; after the delete, also `HOLA_GRACIAS_REPORT` / `recordRequest` / `learningSessionController` if they have no remaining callers).
- In `firstScoreFiveOnNewPersistsDifficultyFiveAndStability24`, drop the `nextRecallAt = last + 24` assertion; keep D=5 and S=24.

Do not delete on-time 169 / Easy next-D / overdue 253 tests. Do not outline second-score 4 vs 5.

Unlocks nothing further.

---

## Stop-safe

| Stop after | User-visible |
|------------|----------------|
| 1 | Same schedule; New score 5 cannot silently take Easy next-D |
| 2 | Same schedule; fewer overlapping tests |

## Not this plan

Tutor 3/2/1/0, confusion, B2, grade framework, 250-line splits, double `setNextRecallAt` after Good/Easy.
