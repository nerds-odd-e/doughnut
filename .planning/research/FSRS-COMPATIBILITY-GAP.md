# Doughnut ↔ open FSRS gap (toward ADR 0003)

**Status:** First-rating initials (`S0`/`D0` on New success) are **in progress** in [`.planning/quick/008-fsrs-first-rating/PLAN.md`](../quick/008-fsrs-first-rating/PLAN.md). This tracker is not a second policy map. Remaining work after that is **deferred** knobs plus **accept ADR 0003** (human). Other shipped FSRS-6 locks, including same-hour success short-term next Stability (elapsed 0, S > 0), live in ADR 0003 Decision and code.

**Updated:** 2026-08-18

**Feeds:** Proposed [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy.md)

**Does not:** approve the ADR (humans own announce → discuss → approve)

Product policy lives in ADR 0003 Decision. This tracker is a pointer plus the deferred ID list.

## Current code vs FSRS-6

Doughnut persists **Stability** in whole hours and **Difficulty** (nullable; shown on the Memory Tracker). Retrievability is computed (FSRS-6 power curve), not stored. Frozen default FSRS-6 weights live in `Fsrs`. Requested retention is locked global `r = 0.9` (`Fsrs.REQUESTED_RETENTION`); it is not a product knob. There is **no** lapse count (locked: not memory state). There is no card state (`New` / `Learning` / `Review` / `Relearning`), fuzz, or max interval.

Live scheduling does not walk a spacing-index ladder. `DEFAULT_SPACES` / `hoursFromLegacyIndex` remain only so committed `V300000260` can replay on fresh DBs.

## Deferred (see ADR 0003 Decision)

**B2** is closed: requested retention is locked global `r = 0.9` (`Fsrs.REQUESTED_RETENTION`), not a knob.

**C4** is closed: just review stays two buttons (Yes = Tutor **4** / Good, No = Tutor **1** / Again). Hard / Easy stay commissioned-only.

**B4** is closed: no lapse count. Memory state is Difficulty, Stability, computed Retrievability. Again history is RecallLog. Frequent-failure warning is the product signal. FSRS-6 After-Again Stability does not consume a count.

- **E3** fuzz / max interval
- **E4** fitting / per-user weights

Humans still own accept / reject / supersede of ADR 0003 (`docs/adrs/README.md`).

## References

- In progress: [`.planning/quick/008-fsrs-first-rating/PLAN.md`](../quick/008-fsrs-first-rating/PLAN.md) — first-rating initials
- [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) (Proposed)
- [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md) — **recall** vs FSRS **review**
- [ADR 0005](../../docs/adrs/0005-commissioned-learning-session-protocol.md) — Tutor score meaning
- Seed: [SEED-004](../seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md)
- Code: `ForgettingCurve`, `MemoryTracker`, `SpacedRepetitionAlgorithm`
- [FSRS-6 algorithm](https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm)
- [ts-fsrs](https://open-spaced-repetition.github.io/ts-fsrs/)
