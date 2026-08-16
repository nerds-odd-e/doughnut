# Plan: Commissioned Tutor score 5 as FSRS-6 Easy

**Status:** in progress  
**Goal:** Tutor Feedback score **5** schedules as open-FSRS-6 Easy (own implementation), matching the locked ADR 0003 Decision for this slice.

Locked choices (CONTEXT.md): Easy not +20% ladder; New D=5/S=24h; overdue extra inherits. No new DB column.

Tests: `LearningSessionRecordTests` (HTTP record boundary) plus targeted E2E in `commissioned_learning_session.feature`. Run `pnpm backend:test_only` and `pnpm cypress run --spec e2e_test/features/learning_session/commissioned_learning_session.feature`. Capability names only — no plan numbers in product files.

---

### 1. Lock Tutor score 5 as FSRS-6 Easy in ADR 0003
Type: Structure  
Status: done

Locked in Proposed ADR 0003: **Tutor Feedback score 5 (Easy)** next to score 4. New D=5/S=24h; S>0 Easy increment + Easy next-D; overdue extra inherited by 4 and 5. Working draft leftover is 3/2/1/0. ADR not accepted.

Learning: no surprises; New score 5 now matches first correct (slice 2).

---

### 2. First score 5 on New initializes Difficulty 5 and Stability 24h
Type: Behavior  
Status: done

New score 5 (`S <= assimilate hours`) uses `recalledSuccessfully`: D=5, S=24h, due +24h. Subsequent score 5 still `applyScore` until slice 3.

29h retargets: score 1 leaves D unset; day-3 due list is both Hola and Gracias. Exclusive “only notes” E2E helper removed.

---

### 3. On-time subsequent score 5 grows Stability more than score 4
Type: Behavior  
Status: planned

**Pre:** Commissioned tracker after first score 4 (S=24, D=5).  
**Trigger:** On-time second session (elapsed 24h) records score 5.  
**Post:** Stability **169** (frozen `w`; strictly greater than score 4’s 102). Due from new S. D may still be unchanged this slice. Effort stays neutral (no thinking-time tweak).

Controller test + E2E pin Stability 169 (mirror “On-time second tutor score 4 grows Stability to 102”).

Impl: FSRS-6 Easy SInc — Good increment term × `w16`, persist whole hours. Do not send subsequent score 5 through `recalledSuccessfully` (that is Good). Sibling to `FsrsGoodRecall` is enough; do not generalize a grade framework.

---

### 4. Subsequent score 5 persists Easy next Difficulty
Type: Behavior  
Status: planned

**Pre:** Same as slice 3 (S=24, D=5, on-time score 5).  
**Trigger:** Record that score 5.  
**Post:** Difficulty is Easy next-D (`Fsrs.nextDifficulty(d, 4)`), about **1.985**, not Good’s 5.0014133 and not left unchanged.

Controller test only (score 4 did the same). Unset D on S>0 still counts as 5.

---

### 5. Overdue subsequent score 5 grows Stability more than on-time
Type: Behavior  
Status: planned

**Pre:** Two commissioned trackers after the same first score 4.  
**Trigger:** One gets score 5 at elapsed=S; the other at elapsed=2S.  
**Post:** Overdue next S is strictly greater than on-time (pin **253** vs **169** with frozen `w` if the same first state as the score-4 overdue test). Extra from elapsed vs Stability, not `nextRecallAt`.

Controller test next to `overdueSecondScoreFourGrowsStabilityMoreThanOnTime`. No E2E unless that spec is already open.

---

### 6. Drop leftover score-5 ladder and record the closed gap
Type: Structure  
Status: planned

`applyScore` no longer receives 5. Remove case 5 and score-5 rows from `CommissionedLearningSessionFeedbackPolicyTest`. Keep 3/2/1/0. Do not delete `DEFAULT_SPACES`.

Update gap doc, SEED-004, STATE.md: leftover ladder is Tutor **3/2/1/0** + confusion; score 5 is Easy. Next FSRS work is not B2-by-default — remaining leftover scores first.

Unlocks nothing further in this plan.

---

## Stop-safe

| Stop after | User-visible |
|------------|----------------|
| 1 | Policy written; app still +20% ladder |
| 2 | First score 5 matches first correct (D=5, S=24h) |
| 3 | Later score 5 lengthens S like Easy |
| 4 | Later score 5 also eases Difficulty |
| 5 | Overdue score 5 gets bounded extra |
| 6 | Ladder helper no longer lies about 5 |

## Not this plan

Tutor 3=Hard, Tutor 2 (C2), scores 1/0, confusion, retention knob, relearning, RecallLog.
