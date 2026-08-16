# Plan: Difficulty on the Memory Tracker page (004 follow-up)

**Status:** in progress (slices 1–3 done; next is slice 4)  
**Goal:** Drop redundant exact next-Stability hour pins; persist Difficulty when a graded tracker still has it unset; show Difficulty on the Memory Tracker page.

**Context:** [CONTEXT.md](./CONTEXT.md)

Say **next Stability** after a correct recall — not FSRS **SInc**. Each slice is one Behavior or Structure, stop-safe, one commit. Sequential. Slice 3 is Structure **only** for slice 4 — do not stop after 3 (JSON field with no UI).

---

### 1. Correct-recall Stability hours have one canonical test

Type: Structure  
Status: done

Deleted `OnTimeAndEarlyRecall` (24/315/361 pins); kept the due-day grid. `harderDifficultyGrowsStabilityLessOnCorrectRecall` now only asserts higher D → smaller next Stability.

---

### 2. Correct recall fills unset Difficulty on a graded tracker

Type: Behavior  
Status: done

`recalledSuccessfully` always persists `difficultyAfterSuccessfulRecall()`. Graded + unset Difficulty now gets FSRS Good next-D from 5 (same as a tracker that already had D=5). New (Stability 0) still inits Difficulty 5. `ForgettingCurve.isNewlyAssimilated()` is private.

---

### 3. MemoryTracker JSON includes Difficulty

Type: Structure  
Status: done

Removed `@JsonIgnore` on `MemoryTracker.difficulty`. Generated type has `difficulty?: number`. Show JSON: graded Difficulty round-trips; assimilate-only omits the field. TS `makeMe.aMemoryTracker.difficulty(...)`. `MemoryTrackerLite` unchanged. Mcq order did not need a re-pin.

---

### 4. Memory Tracker page shows Difficulty

Type: Behavior  
Status: planned

**Pre-condition:** Learner opens a memory tracker (graded with Difficulty, or New with Difficulty unset).  
**Trigger:** Memory Tracker page / Information card.  
**Post-condition:** Graded tracker shows Difficulty next to Stability. New / unset shows **N/A** (same pattern as missing timestamps).

- `MemoryTrackerInformation.vue` only. ADR 0003 Decision: Difficulty is shown on this page (still Proposed).
- Tests: `MemoryTrackerPageView.spec.ts` (or the information card via the page). Optional E2E: existing “memory tracker page” visit asserts N/A for a new tracker.
- Targeted: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/MemoryTrackerPageView.spec.ts`

**Done when:** Type/Stability row has a Difficulty sibling; unset is N/A; no Difficulty on note-info or recently-recalled.

---

## Parked (not this plan)

- Thinking-time tweak vs `LEGACY_INDEX_STEP`
- FSRS `D0(4)` vs raw `w[4]` Easy-init
- Difficulty on `NoteInfoMemoryTracker` / recently recalled
- First success Stability = 12h
- Delete `DEFAULT_SPACES` (fail / confusion / commissioned)
