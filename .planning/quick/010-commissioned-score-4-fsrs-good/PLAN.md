# Plan: Commissioned Tutor score 4 is FSRS-6 Good

**Status:** in progress (slices 1–6 done)  
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
Status: done

**Learnings:** Production already used elapsed-based Good SInc — no formula change. HTTP comparison: Gracias at +48h (146) > Hola at +24h (102). Slice 6 is Flyway null-D backfill only.

---

### 6. Graded trackers with null Difficulty backfill to 5

Type: Behavior  
Status: done

**Learnings:** `V300000262__backfill_memory_tracker_difficulty_for_graded_rows.sql` updates NULL D where S>0 OR recall_count>0; `difficulty IS NULL` preserves already-written next-D. New rows stay unset. Slice 7 cleans leftover score-4 ladder docs.

---

### 7. Spent score-4 ladder and tracker docs

Type: Structure  
Status: planned

Structure change: repo state matches score 4 = Good. Immediate next: later Tutor-score slices start from 5/3/2/1/0 + confusion with no leftover score-4 ladder claims.

**Commit bound:** drop unused score-4 `applyScore` table rows / dead comments; update gap doc, seed, and STATE remaining gap. Keep `DEFAULT_SPACES`. Do not Accept ADR 0003.

**Done when:** no score-4 ladder pins or docs remain; `DEFAULT_SPACES` still present for other paths.
