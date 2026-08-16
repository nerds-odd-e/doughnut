# Plan: Commissioned Tutor score 4 is FSRS-6 Good

**Status:** in progress (slices 1–4 done)  
**Index:** ad-hoc under `.planning/quick/` — on last-slice wrap-up, update `.planning/STATE.md` remaining FSRS gap (score 4 done; leftover 5/3/2/1/0 + confusion). Do not Accept ADR 0003.

**Goal:** Tutor Feedback score 4 uses the same Good-equivalent memory update as ordinary correct (SInc + next-D, including New init and overdue extra). Migrate leftover null Difficulty on already-graded trackers. Cleanup spent score-4 ladder pins.

**Context:** [CONTEXT.md](./CONTEXT.md)

Sequential. Each slice is one Behavior or Structure, stop-safe, **one commit**. Commit bound is part of the slice: do not include the next slice’s files or assertions.

---

### 1. ADR: Tutor 4 is FSRS Good

Type: Structure  
Status: done

**Learnings:** Decision heading `Tutor Feedback score 4 (Good)`; overdue extra lifted for 4 only; Working-draft table dropped the score-4 row. Status still Proposed. Slice 2 can implement Good SInc without further ADR wording.

---

### 2. On-time second score 4 persists Stability 102

Type: Behavior  
Status: done

**Learnings:** Elapsed is computed before advancing `lastRecalledAt`. Score 4 with S>0 uses `MemoryTracker.stabilityHoursAfterSuccessfulRecall` (Good SInc, no Difficulty write). HTTP pin 102; E2E opens the commissioned tracker and sees Stability 102. Existing day-3 request after Hola: 5 / Gracias: 1 lists only Gracias (score 5 → 29h) — E2E aligned with HTTP `dayThreeDueCommissionedOnlyGraciasAfterRecordedScores`. Slice 3 still needs New score 4 to write D=5.

---

### 3. First score 4 on New persists Difficulty 5

Type: Behavior  
Status: done

**Learnings:** New score 4 sets `DEFAULT_DIFFICULTY` when still assimilate (S=0) before `setStability`. Subsequent next-D not written. HTTP `nullValue()` → `5f`; E2E first Hola: 4 shows Difficulty 5. Slice 4 still needs Good next-D for S>0.

---

### 4. Subsequent score 4 persists Good next-D

Type: Behavior  
Status: done

**Learnings:** Score 4 always `setDifficulty(difficultyAfterSuccessfulRecall())` before `setStability` (New still D=5 via ForgettingCurve). HTTP pin is `5.0014133f` (Java float of documented 5.001413f). Shared `afterOnTimeSecondScoreFour()` helper. Slice 5 should already follow from elapsed-based SInc.

---

### 5. Overdue score 4 grows more than on-time

Type: Behavior  
Status: planned

**Pre-condition:** Same commissioned tracker at S=24h, D=5; one grade at elapsed=24h, sibling at elapsed=48h (no thinking time).  
**Trigger:** Record Tutor Feedback score **4**.  
**Post-condition:** Overdue Stability is **strictly greater** than on-time (146 vs 102). Queue lateness vs `nextRecallAt` is not an input.

**Commit bound:** one unit/HTTP comparison test. No E2E. No formula fork.

- Production should already follow from slice 2’s elapsed-based SInc. If elapsed is ignored, this slice fails for the right reason — fix SInc to use elapsed, do not add a commissioned-only overdue bonus.

**Done when:** overdue > on-time pin is green.

---

### 6. Graded trackers with null Difficulty backfill to 5

Type: Behavior  
Status: planned

**Pre-condition:** `memory_tracker` row with `difficulty` NULL and (`stability > 0` OR `recall_count > 0`).  
**Trigger:** Apply Flyway `V300000262`.  
**Post-condition:** Those rows have Difficulty **5**. New / assimilate-only rows stay NULL.

**Commit bound:** `V300000262` SQL only (plus a focused test if the repo already tests Flyway backfills that way). Do not rewrite Stability. Do not change score-4 runtime.

**Done when:** graded null-D rows are 5; New rows remain unset.

---

### 7. Spent score-4 ladder and tracker docs

Type: Structure  
Status: planned

Structure change: repo state matches score 4 = Good. Immediate next: later Tutor-score slices start from 5/3/2/1/0 + confusion with no leftover score-4 ladder claims.

**Commit bound:** drop unused score-4 `applyScore` table rows / dead comments; update gap doc, seed, and STATE remaining gap. Keep `DEFAULT_SPACES`. Do not Accept ADR 0003.

**Done when:** no score-4 ladder pins or docs remain; `DEFAULT_SPACES` still present for other paths.
