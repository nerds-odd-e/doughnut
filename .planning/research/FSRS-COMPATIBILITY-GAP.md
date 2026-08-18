# Doughnut ↔ open FSRS gap (toward ADR 0003)

**Status:** First-rating initials on New (all four G, including Tutor **2** as Hard) are **closed** in product. **New** = ungraded is locked in ADR 0001. Still-New graded-row backfill is gated Flyway: Again `V300000271` (`still_new_again_first_rating_backfill`), Hard/SHRINK `V300000272` (`still_new_hard_first_rating_backfill`), both default `1=0`. Remaining work in [`.planning/quick/011-new-ungraded-first-rating/PLAN.md`](../quick/011-new-ungraded-first-rating/PLAN.md) is leftover stay-New cleanup. After that: **deferred** knobs (**E3** / **E4**) plus **accept ADR 0003** (human). Other shipped FSRS-6 locks, including same-hour success short-term next Stability (elapsed 0, S > 0), live in ADR 0003 Decision and code.

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

First-rating on New is closed in product: all four G use published FSRS-6 `S0(G)` / `D0(G)` (ADR 0003 **First rating on New**), including Tutor **2** as Hard (`S0(2)` / `D0(2)`). Shrink 80% remains the exception only when `S > 0`. `w[0]` is used for Again `S0`.

- **E3** fuzz / max interval
- **E4** fitting / per-user weights

Humans still own accept / reject / supersede of ADR 0003 (`docs/adrs/README.md`).

## References

- [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) (Proposed)
- [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md) — **recall** vs FSRS **review**; **New** = ungraded
- [ADR 0005](../../docs/adrs/0005-commissioned-learning-session-protocol.md) — Tutor score meaning
- Seed: [SEED-004](../seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md)
- Code: `ForgettingCurve`, `MemoryTracker`, `SpacedRepetitionAlgorithm`
- [FSRS-6 algorithm](https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm)
- [ts-fsrs](https://open-spaced-repetition.github.io/ts-fsrs/)
