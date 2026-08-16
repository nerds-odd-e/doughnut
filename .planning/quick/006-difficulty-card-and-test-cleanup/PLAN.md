# Plan: Difficulty tests and Information-card row

**Status:** planned (not started)  
**Index:** not in `.planning/STATE.md` — this plan is ad-hoc only; do not write it into project state.  
**Goal:** Drop the redundant show-JSON omit test; use graded fixtures; lock next Stability on graded-unset persist; put Stability and Difficulty on the same card row.

**Context:** [CONTEXT.md](./CONTEXT.md)

Sequential. Test cleanup first (requested focus). Do not Accept ADR 0003.

---

### 1. Difficulty tests match graded vs unset, without Jackson omit-null

Type: Structure  
Status: planned

**Unlocks slice 2** (page tests already describe Difficulty; layout can move the pair without fighting New+D=7 fixtures).

- Delete `assimilateOnlyTrackerShowLeavesDifficultyNull`. Keep `leavesDifficultyUnsetForAssimilateOnlyTracker` and page N/A.
- `gradedTrackerShowIncludesDifficulty`: Stability > 0 and Difficulty 7 (not New+D=7).
- Page `shows difficulty`: graded-shaped tracker (Stability set, Difficulty 7). Keep unset → N/A as the sibling.
- `correctRecallFillsUnsetDifficultyOnGradedTracker`: also assert next Stability equals the D=5 sibling. Do not re-assert 266h.

**Done when:** `pnpm backend:test_only` and `pnpm frontend:test tests/pages/MemoryTrackerPageView.spec.ts` green; no show test named graded on a Stability-0 tracker.

---

### 2. Stability and Difficulty share a row on the Information card

Type: Behavior  
Status: planned

**Pre-condition:** Learner opens a memory tracker (graded or New).  
**Trigger:** Memory Tracker Information card.  
**Post-condition:** Stability and Difficulty are adjacent fields on the same 2-col row. Times stay grouped; Type stays last. Unset Difficulty is still N/A; graded still shows the API number.

- `MemoryTrackerInformation.vue` only. Suggested order: Assimilated | Last, Next | Recall Count, Stability | Difficulty, Type.
- Drive `MemoryTrackerPageView.spec.ts`: field labels `Stability:` then `Difficulty:` immediately after, and they share a row (`getBoundingClientRect().top` equal) in the 2-col grid.
- No note-info / recently-recalled. No ADR Accept.

**Done when:** targeted frontend test green; card row pairing matches ADR “next to Stability”.
