# Doughnut ↔ open FSRS gap (toward ADR 0003)

**Status:** Interval fuzz is **closed** (not used; due follows S). Thinking-time overlay is **closed** (RT is not a DSR input). New last recall is **closed** (`lastRecalledAt` is the last mapped grade; New has none). RecallLog elapsed hours is **closed** (`elapsed_hours` required; [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) RecallLog). Proposed ADR 0003/0005 Decision locks commissioned Tutor scores as **1–4**, identical to FSRS G (`1` Again, `2` Hard, `3` Good, `4` Easy). Short-term window is **live** in `Fsrs` (elapsed whole hours **< 24** all four G; **≥ 24** long-term, Again = post-lapse; New → Again **5h** → Good at 5h → **6h**) and locked in Proposed ADR 0003 Decision. Remaining deferred: **E4** fitting, plus **accept ADR 0003** (human). Shipped FSRS-6 locks live in ADR 0003 Decision and code.

**Updated:** 2026-08-19

**Feeds:** Proposed [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy.md)

**Does not:** approve the ADR (humans own announce → discuss → approve)

Product policy lives in ADR 0003 Decision. This tracker is a pointer plus the deferred ID list.

## Current code vs FSRS-6

Doughnut persists **Stability** in whole hours and **Difficulty** (nullable; shown on the Memory Tracker). Retrievability is computed (FSRS-6 power curve), not stored. Frozen default FSRS-6 weights live in `Fsrs`. Requested retention is locked global `r = 0.9` (`Fsrs.REQUESTED_RETENTION`). There is **no lapse count**. Maximum interval is locked global **36500 days** / **876000 hours** (`Fsrs.MAXIMUM_INTERVAL_HOURS`); clamp after next-S, due from that S. There is **no interval fuzz** (due is `lastRecalledAt + I(0.9, S)`). Thinking time is recorded on answers for display and stats; it is not a memory-state input. There is no FSRS card-state machine (Learning / Review / Relearning); Doughnut **New** is ungraded. Assimilate does not write `lastRecalledAt`; New due is `assimilatedAt`. `lastRecalledAt` is the last mapped grade. First-rating on New uses published FSRS-6 `S0(G)` / `D0(G)` for all four G (Tutor **2** on New is Hard). RecallLog `elapsed_hours` is required (ADR 0003 RecallLog). When Stability is greater than 0, elapsed whole hours **< 24** use published FSRS-6 short-term next Stability for all four G; elapsed **≥ 24** uses long-term next Stability (Hard/Good/Easy Stability increase; Again post-lapse).

Live scheduling does not walk a spacing-index ladder. Legacy index conversion (`hoursFromLegacyIndex`) lives only on `db.migration.StabilityIndexToHoursBackfill` so committed `V300000260` can replay on fresh DBs.

## Deferred (see ADR 0003 Decision)

- **E4** fitting / per-user weights

Humans still own accept / reject / supersede of ADR 0003 (`docs/adrs/README.md`).

## References

- [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) (Proposed)
- [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md) — **recall** vs FSRS **review**; **New** = ungraded
- [ADR 0005](../../docs/adrs/0005-commissioned-learning-session-protocol.md) — Tutor 1–4 meaning (`score = G`)
- Seed: [SEED-004](../seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md)
- Code: `Fsrs`, `MemoryTracker`; Flyway replay only: `db.migration.StabilityIndexToHoursBackfill` / `V300000260`
- [FSRS-6 algorithm](https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm)
- [ts-fsrs](https://open-spaced-repetition.github.io/ts-fsrs/)
