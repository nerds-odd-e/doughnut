# Plan: Commissioned Tutor score 4 is FSRS-6 Good

**Status:** in progress (slice 1 done)  
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
Status: planned

**Pre-condition:** Commissioned tracker already at S=24h after a prior score 4. On-time: elapsed whole hours = 24. D unset or 5 (treat unset as 5).  
**Trigger:** Record Tutor Feedback score **4**.  
**Post-condition:** Persisted Stability is **102** (FSRS-6 Good SInc). Not 48 (ladder +1).

**Commit bound:** SInc for score 4 when S>0; one HTTP assertion of 102; one E2E scenario that opens the Commissioned Memory Tracker and `expectStability(102)`. Do not write Difficulty. Do not assert Difficulty, overdue, or New-card D. Do not edit `applyScore` tables, gap docs, or ADR.

- HTTP: `LearningSessionRecordTests` — second `record` of score 4 (or fixture S=24h, elapsed=24). Gracias score 1 stays ladder if the report includes it.
- E2E: `commissioned_learning_session.feature` (`@mockBrowserTime`, `@disableOpenAiService`). Extend `openNoteLevelMemoryTracker` to the **Commissioned** row; reuse `expectStability`.
- Production: for score 4 and S>0, set Stability from Good hours (`ForgettingCurve.succeeded(elapsed, null)` or `FsrsGoodRecall` via an entities collaborator). Compute elapsed **before** advancing `lastRecalledAt`. Other scores still `applyScore`. Do **not** call `recalledSuccessfully` (it writes D).

**Done when:** HTTP pin is 102; E2E shows 102; Difficulty unchanged vs before this slice; `pnpm backend:test_only` and `pnpm cypress run --spec e2e_test/features/learning_session/commissioned_learning_session.feature` green.

---

### 3. First score 4 on New persists Difficulty 5

Type: Behavior  
Status: planned

**Pre-condition:** New commissioned tracker (S=0, D unset).  
**Trigger:** Record Tutor Feedback score **4**.  
**Post-condition:** Difficulty is **5**. Stability 24 already holds — do not re-assert.

**Commit bound:** New score 4 writes D=5; HTTP `nullValue()` → `5f`; E2E `expectDifficulty(5)` after the **first** Hola: 4. Do not write subsequent next-D. Do not change Stability math.

- Production likely: `setDifficulty(DEFAULT_DIFFICULTY)` when score 4 and S=0. Leave S on `applyScore` (already 24).

**Done when:** New score 4 leaves D=5; E2E/unit green.

---

### 4. Subsequent score 4 persists Good next-D

Type: Behavior  
Status: planned

**Pre-condition:** Commissioned tracker with S>0, D=5 (or unset treated as 5).  
**Trigger:** Record Tutor Feedback score **4**.  
**Post-condition:** Difficulty is **5.001413f** (Good next-D from 5). Do not re-assert Stability 102.

**Commit bound:** HTTP/unit pin of next-D only. No E2E (float; New D=5 already showed Difficulty on the page). Do not change Stability math.

- Production: on score 4 with S>0, also `setDifficulty(difficultyAfterSuccessfulRecall())`. Still do not call `recalledSuccessfully` unless that is now the smaller change (SInc already shipped).

**Done when:** subsequent score 4 persists `5.001413f`; 102 not re-asserted as the unique claim.

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
