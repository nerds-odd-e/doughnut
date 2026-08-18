---
id: SEED-006
status: dormant
planted: 2026-08-18
planted_during: FSRS maximum-interval DATETIME
trigger_when: when altering remaining MySQL TIMESTAMP columns, or when a persisted instant must live past 2038
scope: large
---

# SEED-006: Remove TIMESTAMP from the system to avoid the 2038 problem

## Why This Matters

MySQL `TIMESTAMP` cannot store instants after `2038-01-19 03:14:07` UTC. Open FSRS `S_MAX` is 36500 days (876000 whole hours). `last_recalled_at + I(S)` at that cap is ~100 years later and cannot persist on `TIMESTAMP`. JDBC already writes UTC (`connectionTimeZone=UTC`). `DATETIME` is timezone-naive and can hold that due if the session TZ stays UTC.

`V300000273` converted only `memory_tracker.last_recalled_at` and `next_recall_at`. Other `TIMESTAMP` columns (including `assimilated_at`, created/updated/deleted, job locks) still hit 2038.

## When to Surface

**Trigger:** altering remaining MySQL `TIMESTAMP` columns, or persisting an instant past 2038.

Also surface when adding scheduling columns, changing JDBC timezone, or revisiting the 8h TZ skew repairs.

## Scope Estimate

**Large** — full-schema type change across many tables, plus confirmation that Hibernate/`java.sql.Timestamp` and UTC session TZ still round-trip. Not a Settings knob.

## Breadcrumbs

- `backend/src/main/resources/db/migration/V300000273__convert_memory_tracker_recall_due_to_datetime.sql` — DATETIME for the two recall-due columns only
- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — maximum interval; due from capped S
- `backend/src/main/resources/db/migration/V100000000__baseline.sql` — `memory_tracker.last_recalled_at` / `next_recall_at` / `assimilated_at` as `timestamp`
- `backend/src/main/resources/db/migration/V300000234__repair_tz_skewed_memory_tracker_scheduling.sql` — prior TZ incident on these columns
- `backend/src/main/resources/application-prod.yml` (and `db-dev.properties`, `db-test.properties`) — `connectionTimeZone=UTC`
- `backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java`

## Notes

Captured during maximum-interval execution when slice 3 could not write `last + 876000 hours` on `TIMESTAMP NOT NULL`. `last_recalled_at` is also `NOT NULL`; a null-last clamp case is unrepresentable.
