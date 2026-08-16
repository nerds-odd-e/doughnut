# Doughnut ↔ open FSRS gap (toward ADR 0003)

**Status:** Remaining work is **deferred** knobs plus **accept ADR 0003** (human).

**Updated:** 2026-08-16

**Feeds:** Proposed [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy.md)

**Does not:** approve the ADR (humans own announce → discuss → approve)

Product policy lives in ADR 0003 Decision. This tracker is a pointer plus the deferred ID list.

## Current code vs FSRS-6

Doughnut persists **Stability** in whole hours and **Difficulty** (nullable; shown on the Memory Tracker). Retrievability is computed (FSRS-6 power curve), not stored. Frozen default FSRS-6 weights live in `Fsrs`. There is **no** lapse count, requested-retention knob, card state (`New` / `Learning` / `Review` / `Relearning`), fuzz, max interval, or RecallLog.

Live scheduling does not walk a spacing-index ladder. `DEFAULT_SPACES` / `hoursFromLegacyIndex` remain only so committed `V300000260` can replay on fresh DBs.

Tutor map, confusion, ordinary correct/incorrect, and locked IDs: ADR 0003 Decision.

## Deferred (see ADR 0003 Decision)

- **B2** requested-retention knob
- **B4** lapses
- **C4** just-review Hard / Easy
- **E3** fuzz / max interval
- **E4** fitting / per-user weights
- **E6** RecallLog

Humans still own accept / reject / supersede of ADR 0003 (`docs/adrs/README.md`).

## References

- [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) (Proposed)
- [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md) — **recall** vs FSRS **review**
- [ADR 0005](../../docs/adrs/0005-commissioned-learning-session-protocol.md) — Tutor score meaning
- Seed: [SEED-004](../seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md)
- Code: `ForgettingCurve`, `MemoryTracker`, `SpacedRepetitionAlgorithm`
- [FSRS-6 algorithm](https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm)
- [ts-fsrs](https://open-spaced-repetition.github.io/ts-fsrs/)
