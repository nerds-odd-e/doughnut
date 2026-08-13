# Doughnut ↔ open FSRS compatibility decisions

**Status:** Open for discussion; feeds Proposed
[ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy.md)
**Updated:** 2026-08-13
**Goal:** Track only unresolved choices required to finalize ADR 0003.

## Current baseline

Doughnut uses a single memory-strength index plus a user-configured interval
table. It does not calculate FSRS Difficulty, Stability, or Retrievability.

The scheduler already has the FSRS-compatible transition foundation:

- memory updates consume the recall outcome and elapsed whole hours;
- due time is projection metadata, not a memory-state input;
- correct, incorrect, accidental-match, and Tutor outcomes advance the recall
  anchor; overlap and missing Tutor feedback do not;
- overdue success is never penalized relative to on-time success;
- ordinary incorrect recall uses a separate 12-hour retry projection;
- `nextRecallAt` remains materialized because history is incomplete.

Doughnut does not yet grant the FSRS overdue reward: a successful recall after
longer elapsed time receives the on-time increment, not a bounded additional
Stability increase.

## Compatibility map

| Open FSRS | Doughnut today | Open question |
|-----------|----------------|---------------|
| Difficulty (D) | None | Whether ADR 0003 names D as a target concept |
| Stability (S) | Strength index + interval table | Whether S replaces the index later |
| Retrievability (R) | Not calculated | Whether a future engine uses R |
| Again / Hard / Good / Easy | Incorrect, correct, accidental, overlap, Tutor 0–5 | How product outcomes map to FSRS grades |
| Requested retention | User interval table | Whether retention-driven intervals replace the table later |
| Learning/relearning states | Fixed 12-hour ordinary-recall retry | Whether retry steps become configurable |
| Recall log / optimizer | Partial Answer and Tutor history | Deferred until history is complete enough to consume |

## Open decisions

### O1. Meaning of “FSRS-compatible”

- Semantic compatibility only; formulas remain Doughnut-specific.
- Name Difficulty/Stability/Retrievability as the target vocabulary while the
  implementation remains transitional.
- Commit to an eventual open-FSRS engine.

**Recommendation to discuss:** target vocabulary and semantics without forcing
an engine migration.

### O2. Persisted memory state

- Keep an opaque strength index and interval table.
- Later replace the index with Stability and Difficulty.
- Require Difficulty in the first FSRS-shaped implementation.

**Already decided:** do not add unused fields. Introduce Stability, Difficulty,
or lapses only with behavior that consumes them.

### O3. Outcome-to-grade mapping

- Collapse Doughnut outcomes onto Again/Hard/Good/Easy.
- Keep Doughnut outcomes first-class and publish a compatibility map.
- Replace Tutor 0–5 at the product surface.

**Recommendation to discuss:** retain product-specific outcomes. Overlap is no
recall event; accidental match is a Doughnut extension; Tutor scores remain
ordered grades.

### O4. Thinking time

- Keep bounded continuous adjustment within correct recall.
- Map thinking-time bands to Hard/Good/Easy.
- Remove thinking-time adjustment once explicit grades exist.

**Recommendation to discuss:** keep it bounded within correct recall so effort
cannot invert the outcome; Tutor scores remain the grade.

### O5. Overdue success reward

- Keep the current minimum: overdue success is no worse than on-time.
- Add a converging, bounded FSRS-like reward for longer elapsed time.
- Add a linear lateness bonus.

**Recommendation to discuss:** if a reward is added, base it on elapsed time
and Stability/Retrievability, never queue deviation.

### O6. Interval source

- Keep the user interval table indefinitely.
- Keep it until a later FSRS migration.
- Prefer retention-target intervals in the ADR now.

### O7. Incorrect-recall retry

- Keep the fixed 12-hour ordinary-recall retry.
- Make relearning steps configurable, retaining 12 hours as the default.
- Use post-lapse Stability without a forced short retry.

Commissioned learning remains cadence-driven and does not inherit the 12-hour
ordinary-recall rule.

### O8. Accidental match in frequent-failure reporting

- Count incorrect only.
- Count incorrect plus accidental match.
- Use separate thresholds.

**Recommendation to discuss:** incorrect only, matching the current ADR draft.

### O9. Manual and administrative schedule paths

- Apply recall-transition rules whenever a grade is recorded.
- Document manual paths as explicit policy escape hatches.
- Remove or gate administrative bypasses.

Pure schedule edits should not fabricate a recall or a due-deviation penalty.

### O10. ADR commitment sentence

- “This ADR does not require FSRS.”
- “The target shape is open-FSRS-compatible; shipping open FSRS is optional.”
- “Doughnut will migrate to open FSRS.”

### O11. Short-term recalls

Whole-hour elapsed precision is decided. Open choices:

- keep one duration-based transition until Stability/Difficulty exist;
- later adopt an explicit short-term transition and learning/relearning states;
- if special handling is added, bound it by elapsed duration/state rather than
  user-local calendar date.

### O12. Monitoring

Choose durable scheduler observables such as interval distribution, success
rate, and immediately-due-after-grade incidence. The completed anchor repair
did not replay history or reinterpret due dates.

### O13. Strictly-future fallback

The invariant `nextRecallAt > recalledAt` is decided for every state-changing
recall. Confirm the fallback when configured spacing is non-positive:

- first positive configured spacing;
- 24 hours only when no positive spacing exists.

### O14. Engine and parameter ownership

- library versus maintained implementation;
- pinned FSRS algorithm/version and upgrade policy;
- global versus per-user retention and fitted parameters;
- minimum history and fallback parameters for fitting.

### O15. Lapses

Do not add an unused counter. Before adding lapses, decide:

- which outcomes increment it;
- whether the first consumer is scheduling, learner-visible reporting, or
  parameter fitting.

## ADR 0003 finalization checklist

- Decide O1–O15 or explicitly defer each item.
- Fold decided product constraints into ADR 0003.
- Keep implementation freedom where no user-visible contract is needed.
- Preserve Doughnut vocabulary: a learning **recall**, not an FSRS “review.”
- Humans own accepting, rejecting, or superseding the ADR.

## References

- [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy.md)
- [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md)
- [ADR 0005](../../docs/adrs/0005-commissioned-learning-session-protocol.md)
- [FSRS algorithm wiki](https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm)
