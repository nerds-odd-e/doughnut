---
id: SEED-004
status: sprouting
planted: 2026-08-15
planted_during: ADR 0003 finalization (gap analysis)
trigger_when: when accepting Proposed ADR 0003, or when changing fitting
scope: large
---

# SEED-004: Close the spaced-repetition scheduling-policy gap with open FSRS

## Why This Matters

Doughnut already schedules recall with elapsed time and outcome. The product contract is Proposed [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) Decision. First-rating (all four G, Tutor **2** as Hard) is **closed**; `w[0]` is used for Again `S0`. Maximum interval is **closed** (36500 days / 876000 hours). Interval fuzz is **closed** (not used). Thinking-time overlay is **closed** (RT is not a DSR input). New last recall is **closed** (`lastRecalledAt` is the last mapped grade; New has none; due for New is `assimilatedAt`). Commissioned scores are **1–4** identical to FSRS G (`score = G`). RecallLog elapsed hours is **closed** (`elapsed_hours` required; ADR 0003 RecallLog). Short-term success window is locked in that Decision (elapsed **< 24** Hard/Good/Easy; **≥ 24** long-term; New → Again **5h** → Good at 5h → **6h**). Remaining deferred is **accepting** ADR 0003 plus **E4** fitting.

## When to Surface

**Trigger:** accepting ADR 0003; changing fitting.

Also surface when changing success/failure interval math, commissioned score → schedule mapping, or due-work rebuild from history.

## Scope Estimate

**Large** — remaining work is policy accept plus deferred product knobs:

1. Humans accept Proposed [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) (`docs/adrs/README.md`).
2. Deferred ID: **E4** fitting. Tracker: [FSRS-COMPATIBILITY-GAP.md](../research/FSRS-COMPATIBILITY-GAP.md).

## Breadcrumbs

- `.planning/research/FSRS-COMPATIBILITY-GAP.md` — pointer + deferred ID list
- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — Proposed policy (Decision; Working draft empty pending accept)
- `docs/adrs/0001-ubiquitous-language.md` — **recall** (not FSRS **review**); **New** = ungraded
- `docs/adrs/0005-commissioned-learning-session-protocol.md` — Tutor 1–4 meaning (`score = G`)
- `backend/src/main/java/com/odde/doughnut/entities/Fsrs.java`
- `backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java`
- `backend/src/main/java/db/migration/StabilityIndexToHoursBackfill.java` (Flyway replay of `V300000260` only; not live scheduling)
- https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm
