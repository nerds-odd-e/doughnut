# Recall-stats FSRS alignment — cleanup + Requested-retention adoption

**Status:** planned
**Scope:** everything surfaced by the FSRS-compatibility-gap analysis of recall
stats (chat 2026-08-18): one correctness bug, one policy/naming adoption
(2 slices), one dead-code cleanup. Forward-looking stats (forecast,
stability/difficulty distribution) are explicitly **out of scope** per
developer direction.

## Context (why these four slices)

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
- Dead DTO weight: `DayRetention.correctCount` / `.answeredCount` /
  `.sampleSize` and `HourRetention.correctCount` / `.answeredCount` are
  computed and shipped but never read by any frontend file (confirmed via
  grep across `frontend/src`) — pure payload bloat.
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
- **Files:**
  - `backend/src/main/java/com/odde/doughnut/services/RecallAnswerRow.java` —
    add `boolean countsAsReview() { return answerOutcome != AnswerOutcome.OVERLAP; }`
  - `backend/src/main/java/com/odde/doughnut/services/RecallStatsService.java` —
    in `aggregateRows`, filter both `recent` and `allTime` through
    `countsAsReview()` before any aggregation.
  - `backend/src/test/java/com/odde/doughnut/services/RecallStatsTestFixtures.java` —
    add an `overlapAnswered(Timestamp answerAt)` builder (outcome `OVERLAP`,
    `productOutcome null`, matching production reality).
  - `backend/src/test/java/com/odde/doughnut/services/RecallStatsServiceTest.java` —
    new nested test class asserting an overlap row contributes to none of
    `totalReviewsAllTime`, `retentionPct365`, or the calendar count for its
    day.
- **TDD:** write the new test first against current code (it should fail —
  today overlap counts as a correct review), confirm it fails for the right
  reason, then implement the filter.
- **Status:** planned

### 2. Structure (docs): Requested retention may be shown read-only in stats

- Amends ADR 0001 and ADR 0003 so slice 3 doesn't contradict written policy.
  No code change; verified by re-reading the two ADRs for internal
  consistency (grep confirms "not in the UI" appears exactly once, in ADR
  0003 line ~54).
- **Files:**
  - `docs/adrs/0001-ubiquitous-language.md` — extend the existing
    **Requested retention** glossary row to note it may be surfaced
    read-only in recall statistics (e.g. the heatmap color anchor).
  - `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — reword "not a
    Settings knob, not in the UI, not persisted" to distinguish
    *configurable* (still no) from *displayed read-only* (now yes, in recall
    stats). Do not touch the other two "not a Settings knob" mentions
    (lines ~178, ~322) — they're about the short-term-success rule and the
    rejected-options list, not the UI-visibility question.
- **Status:** planned — enables slice 3.

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

### 4. Cleanup: drop unread retention-count fields from the stats DTO (Structure)

- Removes `DayRetention.correctCount`, `DayRetention.answeredCount`,
  `DayRetention.sampleSize`, `HourRetention.correctCount`,
  `HourRetention.answeredCount` — confirmed zero references anywhere under
  `frontend/src` (`.correctCount` / `.answeredCount` / `.sampleSize` on
  these two types). `DayAvgResponseTime.sampleSize` is a **different**
  concept (valid response-time sample count) and stays.
- **Files:**
  - `backend/src/main/java/com/odde/doughnut/controllers/dto/RecallStatsDTO.java` —
    trim the two record shapes.
  - `backend/src/main/java/com/odde/doughnut/services/RecallStatsAggregator.java` —
    update `buildRetentionTrend` / `buildHourlyRetention` constructor calls.
  - `backend/src/test/java/com/odde/doughnut/services/RecallStatsServiceTest.java` —
    drop the now-nonexistent-field assertions in
    `perDayRetentionIsCorrectOverAnsweredWithGuard`
    (`getAnsweredCount()`/`getCorrectCount()`); keep the `retentionPct`
    assertions, which are the only externally-observable part.
  - Regenerate the frontend OpenAPI client (`generate-api-client` skill) so
    `DayRetention`/`HourRetention` TS types narrow accordingly; confirm no
    frontend file breaks (none should, per the grep above).
- **No external behavior change** — verified by existing backend tests
  (once trimmed of the removed getters) and the frontend build/tests passing
  with the narrowed generated types.
- **Status:** planned

## Ordering rationale

1 (correctness bug) → 2+3 (Requested-retention adoption arc; 2 unlocks 3) →
4 (zero-value hygiene, safe to drop last if work stops early). Each slice is
independently stop-safe: stopping after any one still leaves a strictly
better, fully-tested state than before it.

## Out of scope (explicit)

- Forecast / due-workload projection, stability/difficulty distribution,
  retrievability-at-risk view — deferred per developer direction ("skip
  forward-looking anything").
- `Answer.correctFrom`'s three-way fallback indirection (outcome → transient
  `correct` → `RecallLog` scan) — flagged in the analysis as an indirection
  smell, but no observed bug beyond the overlap case fixed in slice 1;
  leave as-is.
