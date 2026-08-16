# Difficulty card and test cleanup — 005 follow-up

Inspected 005 commits `42456426e1`–`7347c8753c` (canonical next-Stability pin, persist unset D, show JSON, Information card). Proposed [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) stays **Proposed**.

## Not bugs

- `recalledSuccessfully` snapshots Difficulty on a `ForgettingCurve` before writing next-D, so next Stability still uses the pre-recall value (null → 5).
- Jackson `NON_NULL` omits unset Difficulty; Vue `?? "N/A"` matches. Due `MemoryTrackerLite` has no Difficulty. Note-info and recently-recalled tables do not show it.
- No production dead code from 005 (`isNewlyAssimilated` still used; `afterNthStrictRecall` still used by the due-day grid).

## Meaningful leftovers

1. **Redundant test** — `assimilateOnlyTrackerShowLeavesDifficultyNull` pins global omit-null, not a Difficulty-specific claim. DB unset is `leavesDifficultyUnsetForAssimilateOnlyTracker`; page N/A is the frontend spec.
2. **Wrong precondition** — `gradedTrackerShowIncludesDifficulty` (and the page “shows difficulty” case) set Difficulty 7 on a New tracker (Stability 0). Domain graded means Stability > 0.
3. **Incomplete persist assertion** — `correctRecallFillsUnsetDifficultyOnGradedTracker` locks next-D vs a D=5 sibling, not next Stability (005 post-condition).
4. **Card layout** — 2-col grid is Next | Stability, then Difficulty | Recall Count. ADR: Difficulty is **next to** Stability. Consecutive DOM order is not the same row.

## Parked (not this plan)

- Rounding Difficulty for display (ADR still says the API number)
- E2E N/A on a new tracker (page unit test covers the card)
- FSRS Good ΔD tautology, thinking-time `LEGACY_INDEX_STEP`, Difficulty on note-info / recently-recalled
- Extracting a generic Information-card row component (file is 67 lines)
