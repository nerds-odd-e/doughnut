# Recall-stats FSRS alignment — cleanup + Requested-retention adoption

**Status:** in progress (slices 1–2 done)
**Scope:** everything surfaced by the FSRS-compatibility-gap analysis of recall
stats (chat 2026-08-18), refined into small-commit-size slices: one
correctness bug (1), one Requested-retention adoption arc (2 docs → 3 code),
two independent dead-wire cleanups (4, 5). Forward-looking stats (forecast,
stability/difficulty distribution) are explicitly **out of scope** per
developer direction.

## Context (why these five slices)

Full analysis is in the chat transcript, not duplicated here. Short version:

- `RecallStatsService`/`Aggregator` already read cleanly off the current
  domain model (`RecallPrompt` + `Answer` + `RecallLog`); no stats-only
  domain structure needs removing.
- One real bug: declared-**overlap** spelling answers get no `RecallLog`
  (ADR 0003: "neither tracker receives recall credit... schedule fields stay
  unchanged") but `Answer.correctFrom` still reports them as `correct=true`,
  so `RecallStatsService` currently counts them as successful reviews —
  inflating `retentionPct365`/`totalReviews`/calendar/heatmap.
- **Requested retention** is already the canonical ADR 0001 term
  (`Fsrs.REQUESTED_RETENTION = 0.9`, package-private in
  `entities.Fsrs`, wrapped by public `entities.ForgettingCurve`). No new term
  is needed — "target retention" should **not** be introduced as a synonym.
  ADR 0003's Decision currently says requested retention is "not in the UI,"
  which the heatmap-anchor change below will make untrue; that line needs
  amending before (or as part of) making the change.
- Dead DTO weight, two independent findings: `DayRetention.correctCount` /
  `.answeredCount` / `.sampleSize` are computed and shipped but never read
  by any frontend file (confirmed via grep across `frontend/src`); and the
  entire `hourlyRetention` list (all of `HourRetention`) is dead on the
  wire — the frontend only ever reads the server-computed
  `bestHourRetentionPct` / `worstHourRetentionPct` derived from it, never
  the raw per-hour list.
- Design decision: **do not** add a new API field for the requested-retention
  constant. It is explicitly "a global constant... frozen weights" (ADR
  0003) with no other runtime knob anywhere (Retrievability itself is
  computed server-side and never shipped either). Plumbing it through
  `RecallStatsDTO` → OpenAPI → generated TS client for a value that cannot
  vary per user or request is more indirection, not less. Instead: change
  the frontend literal from 85 to 90, with a comment pointing at
  `Fsrs.REQUESTED_RETENTION` / ADR 0003, and let the ADR text (once amended)
  be the single documented source of truth for humans keeping the two in
  sync if it ever changes.

## Slices

### 1. Fix: overlap answers no longer counted as a recall-stats review (Behavior)

- **Pre-condition:** a learner has a declared-overlap spelling answer (no
  `RecallLog` written, per ADR 0003 "declared overlap" rule).
- **Trigger:** recall stats are computed for that user.
- **Post-condition:** that answer is excluded entirely from
  `totalReviewsAllTime` / `totalReviews365` / `retentionPct365` / calendar /
  weekday-hour heatmap / streaks — it was never a graded recall.
- **Status:** done
- **Learning:** filter is `RecallAnswerRow.countsAsReview()` applied once in
  `aggregateRows` (`recentReviews` / `allTimeReviews`). Overlap test lives in
  `RecallStatsOverlapIsNotAReviewTest` so `RecallStatsServiceTest` stays under
  250 lines. `Answer.correctFrom` still treats overlap as correct — that is
  unchanged and out of scope.

### 2. Structure (docs): Requested retention may be shown read-only in stats

- Amends ADR 0001 glossary and Proposed ADR 0003 Decision so requested
  retention may be shown read-only in recall statistics (e.g. heatmap color
  anchor) while remaining a locked 0.9 constant — not a Settings knob, not
  persisted. Enables slice 3.
- **Status:** done
- **Learning:** dual glossary/policy wording is the same pattern as
  Stability; other two "not a Settings knob" mentions in ADR 0003 (short-term
  success, rejected options) were left alone. ADR 0003 stays Proposed.

### 3. Heatmap retention color anchor uses Requested Retention, 90% not 85% (Behavior)

- **Pre-condition:** a user has retention-heatmap data with day/hour cells
  spanning both sides of the old 85% anchor (e.g. the common 85–92% band).
- **Trigger:** the user opens the recall-stats "Retention %" heatmap.
- **Post-condition:** cell color is anchored at the product's actual
  Requested Retention (90%), not an arbitrary UI-chosen 85% — a cell at 87%
  now renders red-leaning (below target) instead of green-leaning.
- **Files:**
  - `frontend/src/components/recallStats/WeekdayHourHeatmap.vue` — rename
    `RETENTION_TARGET_PCT` value `85` → `90`; update the adjacent comment to
    reference `Fsrs.REQUESTED_RETENTION` / ADR 0003 as the source of truth
    instead of "real-world retention clusters in the 85-92% band."
  - `frontend/tests/components/recallStats/recallStatsTheme.spec.ts` —
    update the retention-heatmap test's description ("anchored at 85%") and
    inline comments ("above/below 85% target") to 90%. Existing 100/90/80/60
    sample data still exercises both sides of a 90% anchor (90% lands as the
    lightest green level, not "mild green above the target" — reword
    accordingly); assertions (distinct shades, hue direction) are expected to
    keep passing unchanged — confirm, don't assume.
- **Status:** planned

### 4. Cleanup: drop unread retention-count fields from `DayRetention` (Structure)

- Removes `DayRetention.correctCount`, `DayRetention.answeredCount`,
  `DayRetention.sampleSize` (three fields, one record) — confirmed zero
  references anywhere under `frontend/src`. `DayAvgResponseTime.sampleSize`
  is a **different** concept (valid response-time sample count) and stays
  untouched.
- **Files:**
  - `backend/src/main/java/com/odde/doughnut/controllers/dto/RecallStatsDTO.java` —
    trim the `DayRetention` record to `(date, retentionPct)`.
  - `backend/src/main/java/com/odde/doughnut/services/RecallStatsAggregator.java` —
    update the `buildRetentionTrend` constructor call.
  - `backend/src/test/java/com/odde/doughnut/services/RecallStatsServiceTest.java` —
    drop the three now-nonexistent-field assertions in
    `perDayRetentionIsCorrectOverAnsweredWithGuard` (lines ~113/115/116:
    `getAnsweredCount()` × 2, `getCorrectCount()` × 1); keep the
    `retentionPct` assertions, the only externally-observable part.
  - Regenerate the frontend OpenAPI client (`generate-api-client` skill) so
    the `DayRetention` TS type narrows accordingly; confirm no frontend file
    breaks (none should, per the grep above).
- **No external behavior change** — verified by the trimmed backend test
  and the frontend build/tests passing against the narrowed generated type.
- **Status:** planned

### 5. Cleanup: stop exposing `hourlyRetention` on the wire — it's server-internal (Structure)

- Bigger finding than a field trim: the **entire** `RecallStatsDTO.hourlyRetention`
  list is dead on the wire. Grepped `frontend/src` for `HourRetention` /
  `hourlyRetention` — the only hit is `RecallStatsTiles.vue` reading
  `totals.bestHourRetentionPct` / `worstHourRetentionPct`, which are
  computed server-side by `RecallStatsAggregator.buildTotals` *from*
  `hourlyRetention` — the raw per-hour list itself is never read once it
  reaches the browser. It's currently public API surface purely to satisfy
  an internal computation step.
- Also, `HourRetention.correctCount` is unused even **internally**:
  `buildTotals`'s best/worst-hour loop only reads `getAnsweredCount()` (the
  `BEST_WORST_MIN_ANSWERED` guard) and `getRetentionPct()`, never
  `getCorrectCount()`.
- **Fix:** remove `hourlyRetention` from `RecallStatsDTO`'s public shape;
  relocate `HourRetention` from `controllers.dto.RecallStatsDTO` into
  `services` (package-private, alongside `RecallStatsAggregator`) as a
  purely internal computation type, and drop its dead `correctCount` field
  (keep `hour`, `retentionPct`, `answeredCount` — the guard needs the last
  one).
- **Files:**
  - `backend/src/main/java/com/odde/doughnut/controllers/dto/RecallStatsDTO.java` —
    remove the `hourlyRetention` field and the nested `HourRetention` class.
  - `backend/src/main/java/com/odde/doughnut/services/RecallStatsAggregator.java` —
    define `HourRetention` as a package-private record here (3 fields, no
    `correctCount`); `buildHourlyRetention` stays, used only as an internal
    input to `buildTotals`.
  - `backend/src/main/java/com/odde/doughnut/services/RecallStatsService.java` —
    drop `hourlyRetention` from the `new RecallStatsDTO(...)` call (7 args,
    not 8); the local `hourlyRetention` variable stays, just isn't returned.
  - `frontend/tests/pages/settings/RecallStatsSettingsTab.spec.ts` — drop
    the `hourlyRetention: buildHourlyRetention()` key (and the now-unused
    `buildHourlyRetention` mock helper) from the DTO test fixture.
  - Regenerate the frontend OpenAPI client so the generated
    `RecallStatsDTO` TS type drops `hourlyRetention`/`HourRetention`.
- **No external behavior change** — `bestHourRetentionPct`/`worstHourRetentionPct`
  on `totals` are computed identically; verified by existing backend tests
  (`bestAndWorstHourByRetentionWithMin5Guard`, unchanged) and the frontend
  build/tests against the narrowed type.
- **Status:** planned

## Ordering rationale

1 (correctness bug) → 2+3 (Requested-retention adoption arc; 2 unlocks 3) →
4, 5 (zero-value hygiene, independent of each other and of 1–3; safe to drop
either or both last if work stops early). Slices 4 and 5 are retroactive
dead-code removal (already-proven-unused fields), not speculative structure
prep — they don't need a following behavior slice to justify them, per the
"clean up dead code" phase-discipline rule rather than the
prep-for-next-behavior rule. Each slice is independently stop-safe: stopping
after any one still leaves a strictly better, fully-tested state than
before it.

## Out of scope (explicit)

- Forecast / due-workload projection, stability/difficulty distribution,
  retrievability-at-risk view — deferred per developer direction ("skip
  forward-looking anything").
- `Answer.correctFrom`'s three-way fallback indirection (outcome → transient
  `correct` → `RecallLog` scan) — flagged in the analysis as an indirection
  smell, but no observed bug beyond the overlap case fixed in slice 1;
  leave as-is.
