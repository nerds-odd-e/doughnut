# Plan: Difficulty on the Memory Tracker page (004 follow-up)

**Status:** in progress (slice 1 done; next is slice 2)  
**Goal:** Drop redundant exact next-Stability hour pins; persist Difficulty when a graded tracker still has it unset; show Difficulty on the Memory Tracker page.

**Context:** [CONTEXT.md](./CONTEXT.md)

Say **next Stability** after a correct recall — not FSRS **SInc**. Each slice is one Behavior or Structure, stop-safe, one commit. Sequential. Slice 3 is Structure **only** for slice 4 — do not stop after 3 (JSON field with no UI).

---

### 1. Correct-recall Stability hours have one canonical test

Type: Structure  
Status: done

Deleted `OnTimeAndEarlyRecall` (24/315/361 pins); kept the due-day grid. `harderDifficultyGrowsStabilityLessOnCorrectRecall` now only asserts higher D → smaller next Stability.

**Learning:** `RobotsTests.openApiDocsMatchCommittedYaml` is already red on HEAD/main CI (`Mcq` property order). Slice 1 wrapped up as-is per developer (local commit, no push). Slice 3 still owns the `@JsonPropertyOrder` pin. Do not treat full `backend:test_only` green as a slice-2 gate.

---

---

### 2. Correct recall fills unset Difficulty on a graded tracker

Type: Behavior  
Status: planned

**Pre-condition:** Tracker with Stability > 0 and Difficulty unset.  
**Trigger:** Ordinary correct recall.  
**Post-condition:** Difficulty is persisted as the FSRS Good next-D from 5 (not left null). Next Stability for that recall still uses Difficulty 5.

Delta-only on `recalledSuccessfully`. Do not re-assert 266h.

**Done when:** that persist is locked; New (Stability 0) init from 004 still holds.

---

### 3. MemoryTracker JSON includes Difficulty

Type: Structure  
Status: planned

**Unlocks slice 4.** No page change yet.

- Remove `@JsonIgnore` on `MemoryTracker.difficulty`.
- `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`. If `RobotsTests.openApiDocsMatchCommittedYaml` fails on `Mcq` property order, pin `@JsonPropertyOrder` on `Mcq` to declaration order and regen — do not hand-edit YAML.
- TS `makeMe.aMemoryTracker.difficulty(...)`.
- Controller/show: graded Difficulty round-trips on the shown entity (delta). Assimilate-only still null.

**Done when:** generated `MemoryTracker` type has `difficulty?: number`; show payload can carry Difficulty; due `MemoryTrackerLite` unchanged.

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
