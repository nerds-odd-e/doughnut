# Plan: Difficulty tests and Information-card row

**Status:** in progress (slice 1 done)  
**Index:** not in `.planning/STATE.md` — this plan is ad-hoc only; do not write it into project state.  
**Goal:** Drop the redundant show-JSON omit test; use graded fixtures; lock next Stability on graded-unset persist; put Stability and Difficulty on the same card row.

**Context:** [CONTEXT.md](./CONTEXT.md)

Sequential. Test cleanup first (requested focus). Do not Accept ADR 0003.

---

### 1. Difficulty tests match graded vs unset, without Jackson omit-null

Type: Structure  
Status: done

**Unlocks slice 2** (page tests already describe Difficulty; layout can move the pair without fighting New+D=7 fixtures).

Graded show/page fixtures are Stability > 0 + Difficulty 7. Jackson omit-null show test removed; DB unset + page N/A remain. Persist also locks next Stability to the D=5 sibling.

**Learnings:** After dropping the omit-null sibling, graded show asserts `shown.getDifficulty()` on the controller return (no ObjectMapper roundtrip). `persistsAssignedDifficulty` still uses New+D=7 — that is assigned-Difficulty persistence, not the graded-show precondition. Slice 2 can move the card pair without New+D=7 fixtures.

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
