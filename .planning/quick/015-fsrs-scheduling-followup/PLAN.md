# Plan: FSRS leftover-close follow-up

**Status:** in progress  
**Goal:** Close the confusion understanding-path test hole and the redundant tests / cohesion leftovers from leftover-score work. Same schedule numbers.

Locked: [CONTEXT.md](./CONTEXT.md). Capability names only.

Tests: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`. When Gherkin changes: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/learning_session/commissioned_learning_session.feature`.

Wrap-up per `planning.mdc`. Do not accept ADR 0003.

---

### 1. Understanding confusion still shrinks Stability
Type: Behavior  
Status: done

**Pre:** Accidental match; matched note has an understanding tracker (S > 0) and no spelling tracker.  
**Trigger:** Grade the spelling answer.  
**Post:** That understanding tracker’s Stability is **strictly less** than before. Still linked as the confusion target.

Learned: production already shrinks S on this path; only the unique `lessThan` claim was missing. Did not re-pin 115. Remaining slices unchanged.

---

### 2. Tutor Feedback tests pin each unique claim once
Type: Structure  
Status: planned

Enables slice 3: E2E outline stays the only duplicated 4/5/3 Stability pin across boundaries; unit S pins become one parameterized test.

- Include New score **0** in `firstScoreLeavesDifficultyUnsetAndStabilityZero` (`0,1,2`).
- Keep **one** strictly-future due test for S=0 (24h fallback). Do not also pin D/S there.
- Collapse on-time score **1** (S=8, D=10, due = last + S) into **one** test.
- Parameterize on-time second S (4→102, 5→169, 3→71) and overdue second S (4→146, 5→253, 3→97). Keep separate next-D tests (unique floats).
- Existing tests still pass; no new schedule numbers.
- Stay under 250 lines in `LearningSessionRecordTutorFeedbackTests`.

---

### 3. One E2E outline for on-time second-score Stability
Type: Structure  
Status: planned

Enables slice 4: production cohesion without three Gherkin copies of the same path.

- Replace the three on-time second Tutor score scenarios (4→102, 5→169, 3→71) with one **Scenario Outline**.
- Same path and assertion focus; only score and Stability vary (`e2e-authoring.mdc`).
- Leave first-score 4 Difficulty / first-score 5 Stability as separate scenarios (different unique claims).

---

### 4. Score 2 shrink lives on MemoryTracker
Type: Structure  
Status: planned

Enables slice 5: every commissioned grade mutates memory on `MemoryTracker`, so Easy/Hard/Good apply can collapse without a leftover scheduling special case.

- Move 80% accumulated shrink (D unchanged, lastRecalledAt, due from new S) onto `MemoryTracker` next to `recalledHard` / `recalledAgain`.
- `recordFeedback` only routes `case 2`.
- Same S=19 / D unchanged / New D unset. `MemoryTracker` must stay ≤ 250 lines (extract if needed, like Again).

---

### 5. One apply path for Good / Hard / Easy memory update
Type: Structure  
Status: planned

No further Behavior in this plan (cohesion of shipped Hard). Existing tests still pass.

- Collapse `recalledSuccessfully` / `recalledHard` / `recalledEasily` duplication and repeated `isNewlyAssimilated()` New-init on `ForgettingCurve`.
- Keep `FsrsGoodRecall` / `FsrsHardRecall` / `FsrsEasyRecall` as own types (w15 / w16 / Good increment).
- Thinking time stays on **Good / correct** only.
- Do not change 102 / 169 / 71 / next-D floats.
- Inline `MemoryTrackerAgainRecall` only if line budget allows after the collapse; otherwise leave it.

---

## Stop-safe

| Stop after | User-visible |
|------------|----------------|
| 1 | Understanding confusion cannot silently skip a Stability shrink |
| 2 | Same schedule; fewer overlapping controller tests |
| 3 | Same E2E coverage; one outline |
| 4–5 | Same schedule; commissioned grades sit on `MemoryTracker` |

## Not this plan

Accept ADR 0003. Deferred knobs. Merge `Fsrs*Recall` classes. E2E for score 1 due. Flyway. Delete `DEFAULT_SPACES`.
