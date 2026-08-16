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
Status: done

Enables slice 3: E2E outline stays the only duplicated 4/5/3 Stability pin across boundaries; unit S pins are one parameterized test.

Learned: 235 → 196 lines. Score 0 added to first-score D-unset/S=0. Score 1 Again is one test (S=8, D=10, due = last + S). On-time/overdue S parameterized; next-D and overdue greaterThan kept unique. Unused String trampoline dropped. Remaining slices unchanged.

---

### 3. One E2E outline for on-time second-score Stability
Type: Structure  
Status: done

Enables slice 4: production cohesion without three Gherkin copies of the same path.

Learned: one Outline with Examples 4→102, 5→169, 3→71; first-score 4 Difficulty and first-score 5 Stability left separate. Cypress commissioned_learning_session.feature 10 passing. Remaining slices unchanged.

---

### 4. Score 2 shrink lives on MemoryTracker
Type: Structure  
Status: done

Enables slice 5: every commissioned grade mutates memory on `MemoryTracker`, so Easy/Hard/Good apply can collapse without a leftover scheduling special case.

Learned: `case 2` routes to `MemoryTracker.shrinkStability` → `MemoryTrackerShrinkStability` (Again-style extract; due via `scheduleNextRecallFromStability`). Existing tests still pin S=19 / D unchanged / New D unset. `MemoryTracker` is 250 lines with a compacted JPQL javadoc — **restore that javadoc when slice 5 frees lines**.

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
- Restore the original `JPA_WHERE_NOTE_LEVEL_TRACKER` javadoc if the collapse frees lines.

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
