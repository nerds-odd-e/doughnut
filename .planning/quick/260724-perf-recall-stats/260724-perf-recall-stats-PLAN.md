# Plan: Recall Stats endpoint performance (perf/recall-stats)

## Problem
`GET /api/user/recall-stats` times out in production for users with large recall history.

## Root cause
`RecallStatsService.compute` runs two native `SELECT rp.*` queries (1y `recent`, 5y `allTime`, the latter a superset of the former) returning full `RecallPrompt` entities. `RecallPrompt` has three EAGER associations (`answer` @OneToOne, `memoryTracker`/`predefinedQuestion` @ManyToOne). A native `SELECT rp.*` does not join-fetch them, so Hibernate fires a secondary SELECT per row per association — a classic N+1, dominated by the 1:1 `answer` (one extra query per recall prompt). For thousands of rows over 5 years → thousands of round-trips → timeout. Indexes already exist; the cost is post-query hydration, plus double-fetching the last year and aggregating full entities in Java.

## Approach chosen: A — projection + single query
Select only the 4 columns the aggregation needs (`answer.created_at, correct, thinking_time_ms, prompt.created_at`) via a JPQL constructor expression (no entity hydration → no N+1), collapse to a single 5y query, derive the 1y `recent` set in Java.

## Phases (Behavior/Structure, stop-safe)

### Phase 1 — Structure: projection row seam (no behavior change)
- New `RecallAnswerRow` record (4 fields) in `services`.
- `RecallStatsAggregator` core consumes `List<RecallAnswerRow>` (`buildTotals`, `responseTimeMs`, main loop).
- `RecallStatsService.aggregate(List<RecallPrompt>, ...)` becomes a thin adapter mapping each `RecallPrompt` → `RecallAnswerRow`, delegating to a new `aggregateRows(List<RecallAnswerRow>, ...)`.
- `compute` unchanged (still entity query). Existing `RecallStatsServiceTest` + `UserControllerTest` stay green.

### Phase 2 — Behavior: fix the timeout (test-first)
- Add `RecallStatsPerformanceTest` (`@SpringBootTest`): build ~200 answered recall prompts over 5y for one user; enable Hibernate `Statistics`; assert `getRecallStats` executes `< 10` prepared statements (proves no N+1) and returns correct totals. Fails before fix (N+1), passes after.
- Add repository `findAnsweredRecallAnswerRows(userId, start, end)` — JPQL constructor expression returning `List<RecallAnswerRow>`.
- `compute`: one 5y projection query; derive 1y rows in Java; call `aggregateRows`.
- All existing stats tests stay green.

## Out of scope (future, if still slow)
DB-side GROUP BY aggregation; per-user short-TTL cache; bound on allTime window.

## Results (done)
- Phase 1 (structure) committed `4af2f4343e`: `RecallAnswerRow` projection seam; aggregator core consumes rows; `aggregate(List<RecallPrompt>)` adapter keeps unit tests unchanged.
- Phase 2 (behavior) committed `11ce29bab3`: `compute` uses a single JPQL constructor-expression projection over 5y + derives 1y in Java; `RecallStatsPerformanceTest` guards the query count.
- Benchmark (200 answered recalls): prepared statements **403 → 1** (no N+1). Existing `RecallStatsServiceTest` + `UserControllerTest` green; no behavior change.
- Branch `perf/recall-stats` pushed to origin.
