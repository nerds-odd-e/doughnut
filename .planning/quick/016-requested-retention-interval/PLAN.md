# Plan: Requested retention interval (r = 0.9)

**Status:** in-progress  
**Goal:** Lock B2 as global `r = 0.9`. Graded due is `I(r, S)`. Drop the ordinary-incorrect +12h retry. No UI, no varying `r`.

Locked: [CONTEXT.md](./CONTEXT.md). Capability names only.

Tests: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`. When Gherkin changes: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/spaced_repetition.feature`.

Wrap-up per `planning.mdc`. Do not accept ADR 0003. Do not run in parallel with 015.

---

### 1. Requested retention is 0.9; due hours are I(0.9, S)
Type: Structure  
Status: done

`Fsrs.REQUESTED_RETENTION` / `intervalHours` (`I(0.9, S) = S`). `calculateNextRecallAt` uses it. ADR 0001 glossary + ADR 0003 Decision lock `r = 0.9`; B2 dropped from Deferred; +12h still in running code and ADR until slice 2.

Learning: wrapping `calculateNextRecallAt` pushed `MemoryTracker` over 250 lines; confusion due projection extracted to `MemoryTrackerConfusionAdjustment` (same helper pattern as Again/Shrink).

---

### 2. Incorrect recall with Stability > 0 is due from I
Type: Behavior  
Status: done

**Pre:** Graded tracker, Stability > 0, due for recall.  
**Trigger:** Ordinary incorrect (just-review, MCQ, or spelling).  
**Post:** `nextRecallAt = lastRecalledAt + I(0.9,` post-lapse `S)` — wait equals new Stability hours, not 12.

Just-review E2E unique due claim is 8 hours (`{int} hours between last and next recall`). Canonical unit pin S=17 → last+17. MCQ/spelling `shouldRepeatInTwelveHours` dropped. New fail still +12h via `recallFailed` when `isNewlyAssimilated()`. ADR 0003 still Proposed (S>0 due from `I`; New fail +12h interim).

Learning: New-tracker check for the +12h override shares `ForgettingCurve.isNewlyAssimilated()` with Again recall.

---

### 3. New incorrect recall uses the 24-hour fallback
Type: Behavior  
Status: planned

**Pre:** New tracker (Stability 0, Difficulty unset).  
**Trigger:** Ordinary incorrect.  
**Post:** Stability 0, Difficulty unset, due **24 hours** after the grade (`I` non-positive → existing strictly-future fallback). Not +12h.

- Change `newTrackerIncorrectRecallKeepsZeroStabilityAndTwelveHourDue` to 24h; keep D unset / S=0 as the canonical New-fail shape (due is the unique delta).
- Remove the S=0 +12h branch. Put strictly-future **24h** on `scheduleNextRecallFromStability` so ordinary fail and commissioned scores share it. `CommissionedLearningSessionFeedbackScheduling.ensureNextRecallStrictlyAfterNow` should not stay a second copy.
- ADR 0003: delete every ordinary-incorrect **+12h** / “schedule metadata” sentence. After **any** grade, due is `last + I(0.9, S)`; non-positive `I` → 24h. New fail matches commissioned New 0/1/2 fallback. Confusion projection `last + I`, never later.
- No new E2E (New fail 24h vs 12h is the unit-test edge of the same rule slice 2 already showed on the Memory Tracker).

---

### 4. One due path; drop 12h leftover
Type: Structure  
Status: planned

No further Behavior. Existing tests still pass.

- Delete `recallFailed` if it is only a 12h wrapper; `markAsRecalled` fail goes through Again + shared schedule.
- Rename leftover 12h test/step names. No `TwelveHour` helpers.
- Gap tracker + SEED-004: B2 is locked `r = 0.9`, not a knob. Remaining deferred: B4 / C4 / E3 / E4 / E6 plus **accept ADR 0003**.
- Still no `r` argument, no UI, no Flyway, no tests at other `r`.

---

## Stop-safe

| Stop after | User-visible |
|------------|----------------|
| 1 | Same schedule; `r` named in ADR and `Fsrs` |
| 2 | Failed recall (S > 0) waits Stability hours, not 12 |
| 3 | New fail waits 24h, not 12; no +12h policy left |
| 4 | Same schedule as 3; one due path |

## Not this plan

Accept ADR 0003. Settings / UI for `r`. Backfill in-flight 12h rows. Varying-`r` tests or `I(r, S)` with an `r` parameter. 015 apply-path collapse. Delete `DEFAULT_SPACES`.
