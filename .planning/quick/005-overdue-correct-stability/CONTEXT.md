# Overdue correct recall — Stability hours

**Status:** in progress (slices 1–5 done)  
**ADR:** Proposed [0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) B3  
**Seed:** [SEED-004](../../seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md)

## Requirement (closing B3)

A **correct** recall with elapsed whole hours **greater than Stability** gets a **strictly longer** next interval than the same correct recall at elapsed **equals** Stability (same thinking time). Extra **converges**. Input is elapsed vs Stability.

Commissioned Tutor scores stay score-driven.

## Domain mapping

Same noun **Stability** (whole hours) on ADR, glossary, DB, entity, OpenAPI, UI, tests. Retrievability is computed, not stored. No `forgettingCurveIndex` alias. No Difficulty / lapse / retention / RecallLog fields.

The Settings day list and `space_intervals` are **removed in this plan** (not deferred to B2). B2 (requested retention) is out of scope.

## Migration order

Convert `forgetting_curve_index` through each user’s `space_intervals` **before** dropping `space_intervals`.

## ADR hygiene

Present-tense domain only. No index history. No negation for replaced machinery.
