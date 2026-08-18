# Plan: Cap Stability at the open-FSRS maximum interval

**Status:** done

Persisted Stability (and due, because `I(0.9, S) = S`) never exceeds open FSRS `S_MAX` (**36500 days** / **876000 hours**). Existing over-cap rows were clamped. ADR 0003 stays **Proposed**.

Product policy: [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy.md). Tracker: [FSRS-COMPATIBILITY-GAP.md](../../research/FSRS-COMPATIBILITY-GAP.md). Remaining deferred: **E3** fuzz / **E4** fitting, plus accept ADR 0003. Remaining MySQL `TIMESTAMP` columns: [SEED-006](../../seeds/SEED-006-remove-mysql-timestamp-2038.md).
