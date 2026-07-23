# Quick Task: Recall Statistics Tab - Research

**Researched:** 2026-07-23
**Domain:** Spaced-repetition analytics (backend aggregation + Vue 3 charts)
**Confidence:** HIGH (competitive survey from official docs; codebase findings verified by grep/read)

## Summary

Popular spaced-repetition apps (Anki, SuperMemo, Mochi, RemNote) converge on a small, high-value stats surface: a **past-activity calendar** (GitHub-style heatmap of daily reviews), **review counts**, **time spent reviewing**, **retention / answer-button accuracy**, and an **hourly breakdown** of success rate. Anki's Stats screen is the gold standard and ships exactly the calendar the user asked for ("Calendar — past card review activity; hover to see revisions that day"). Mochi's Dashboard adds a review heatmap + line charts for review history/time. The user's requested set (calendar, daily avg response time, morning/afternoon, weekday×hour) overlaps heavily with competitor norms; the gaps worth filling cheaply are **total reviews**, **retention/accuracy rate**, **streaks**, and **total time spent** — all derivable from the same `recall_prompt`/`quiz_answer` rows.

**Critical codebase finding:** Doughnut *already* captures a precise, pause-aware response time. `Answer.thinkingTimeMs` (`quiz_answer.thinking_time_ms`) is measured client-side by `useQuestionThinkingTimeTracker`, which pauses on tab-hide / window-blur / page-hide and resumes on focus — i.e. it already excludes "walked away" time. It is also already clamped to `[0, 60000]` ms in `ForgettingCurve` (`MAX_THINKING_TIME_MS = 60000`). This is a strictly better "response time" than the user's proposed `answer.createdAt − recall_prompt.createdAt` (which includes paused/away time). **Recommend `thinkingTimeMs` as the primary metric**, with the timestamp-diff as a fallback only for legacy rows where `thinkingTimeMs` is null. This deviates from the user's stated definition — flagged as Open Question A1 for planner/user confirmation.

**Primary recommendation:** Ship one read-only `GET /api/user/recall-stats?timezone=…` endpoint that reuses `RecallPromptRepository.findAnsweredRecallPromptsInTimeRange` and returns a single `RecallStatsDTO`; render it in a new Settings → "Recall Stats" tab using **hand-rolled SVG/CSS** (no new chart dependency — the repo has none, and the shapes are simple). Add a tight, high-value stat set (calendar + 4 requested charts + total reviews / retention / streak / time-spent), with response-time averages computed as a **trimmed mean (P5–P95)** after dropping sub-second misclicks.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Aggregation of review/answer rows | API / Backend | — | Grouping by user-local day/hour requires server-side `ZoneId` conversion of UTC `answer.createdAt`; reuses existing repo query. |
| Calendar / heatmap / trend rendering | Browser / Client | — | Pure visualization of pre-aggregated DTOs; no client-side data fetching of raw rows. |
| Timezone grouping | API / Backend | Browser | Backend owns grouping (one source of truth); browser only passes `timezone` param like `getMenuData`. |
| Response-time outlier filtering | API / Backend | — | Trimmed-mean / percentile logic belongs with aggregation so the wire shape is final. |
| Auth / access control | API / Backend | — | `authorizationService.getCurrentUser()` scopes all rows to the logged-in user. |

## Part 1 — Competitive Product Research

### What each app ships (verified via official docs)

**Anki** `[CITED: docs.ankiweb.net/stats.html]` — the gold standard. Stats screen (press T) shows:
- **Today** block: reviews done, Again count, correct %, Learn/Review/Relearn/Filtered counts.
- **Calendar**: "past card review activity; hover to see revisions that day; click a weekday to set week start" — *this is exactly the GitHub-style calendar the user wants*.
- **Reviews** graph (count by day/week/month, stacked by mature/young/relearning/learning).
- **Review Time** graph (time spent per card, same shape as Reviews).
- **Future Due** forecast; **Card Counts** pie (mature/unseen/young/suspended); **Review Intervals**; **Card Ease** (avg ease); **Card Stability/Difficulty/Retrievability** (FSRS only); **Hourly Breakdown** (reviews per hour + success-rate % per hour — *directly maps to the user's weekday×hour + morning/afternoon ask*); **Answer Buttons** (% correct per card type); **True Retention Table** (retention by interval band, mature = interval ≥21d). "Average answer time" is a headline stat `[CITED: github.com/ankitects/anki …/statistics.ftl]`. Revlog `time` field = ms spent on question+answer before pressing ease.

**SuperMemo** `[CITED: help.supermemo.org/wiki/Statistics + /wiki/Analysis]` — Statistics window (F5) + Calendar of daily/monthly repetitions. Metrics: forgetting index, retention, daily repetition workload, time spent learning, new items learned, repetitions completed, scheduled vs overdue. Analysis graphs track daily changes (memorized items, measured forgetting index, outstanding items). R-Metric compares algorithm versions (niche, not user-facing value).

**Mochi** `[CITED: mochi.cards/changelog + /docs/cards]` — Dashboard ships: **review heatmap** (browsable by year — GitHub-style), **line charts** for review history, **review-time chart**, **review-forecast chart**, **review-intervals chart**, deck progress, daily stats with **total review time**, "all time" toggle, per-deck + date-picker scoping. Per-card review history records timestamp, remembered/forgotten, interval, duration.

**RemNote** `[ASSUMED]` — spaced-reetition stats in the queue (daily review count, retention %, cards mature vs. learning). Less public docs; treat as lower confidence.

**Quizlet** `[ASSUMED]` — study streaks, mastery/progress tracking, set-level score; no true SR algorithm stats. Lower relevance (not a strict SR app).

### Ranked shortlist by user value → Doughnut data model

| Rank | Statistic | User value | Doughnut data source | Effort |
|------|-----------|------------|----------------------|--------|
| 1 | **Activity calendar** (365-day daily review count, GitHub-style) | Habit motivation; Anki+Mochi both ship it | `quiz_answer.created_at` grouped by user-local day | Low |
| 2 | **Retention / accuracy rate** (% correct) | Core SR health signal (Anki Answer Buttons / True Retention) | `quiz_answer.correct` | Low |
| 3 | **Total reviews** (all-time + last-365 + today) | Headline "how much have I done" | count of answered prompts | Low |
| 4 | **Daily avg response-time trend** (line, last 30/90d) | Self-awareness of focus/fatigue | `quiz_answer.thinking_time_ms` | Low |
| 5 | **Hourly breakdown** (reviews + success % per hour) | Find best study time (Anki Hourly Breakdown) | `correct` + hour-of-day | Low |
| 6 | **Weekday×hour heatmap** (most-frequent review time) | When do I actually study? | count by weekday×hour | Low |
| 7 | **Streak** (current + longest consecutive days with ≥1 review) | Habit hook (Quizlet-style) | distinct review days | Low |
| 8 | **Total time spent reviewing** | "Time on task" (Mochi/SuperMemo ship it) | sum `thinking_time_ms` | Low |
| 9 | Morning vs afternoon response-time trend | User-requested slice of #5/#6 | `thinking_time_ms` by AM/PM | Low |

### Overlap with the user's request + recommended additions

The user's four requested views map 1:1 to competitor norms: **calendar** = Anki Calendar / Mochi heatmap; **daily avg response time** = Anki Review Time + "average answer time"; **morning/afternoon** = slice of Anki Hourly Breakdown; **weekday×hour** = Anki Hourly Breakdown extended to 2D.

**Recommended tight set for one tab** (not a kitchen sink — every item is low-effort from the same query and high user value):
- Calendar (rank 1) — *requested*
- Daily avg response-time trend (rank 4) — *requested*
- Weekday×hour heatmap (rank 6) — *requested*
- Morning vs afternoon response-time comparison (rank 9) — *requested*
- **Plus** headline tiles: Total reviews (rank 3), Retention % (rank 2), Current/longest streak (rank 7), Total time spent (rank 8), Reviews today.
- **Plus** one bonus chart that's nearly free from the hourly data: **best/worst review hour by retention** (rank 5) — surfaces "you recall best at 10am".

Defer (higher effort, lower v1 value): Future-Due forecast (needs scheduler math), Card Counts by maturity band (needs interval semantics), Ease distribution (Doughnut uses a forgetting-curve index, not Anki ease — different concept), FSRS stability/difficulty (not applicable).

## Part 2 — Doughnut-Specific Recommendations

### Exact set of statistics to implement now

1. **Activity calendar** — 365 days (or YTD) of daily answered-review counts, GitHub-style grid (weeks as columns, weekdays as rows), color scale by count, hover tooltip = date + count.
2. **Daily average response-time trend** — line chart, one point per day, last 90 days (default) with a 30/90/all toggle. Y-axis in seconds.
3. **Morning vs afternoon response-time** — two summary stats (avg ms AM vs PM) + optionally a small bar. AM = local hours [06,12), PM = [12,18); (evening [18,24) and night [00,06) can be shown as extra bars cheaply).
4. **Weekday×hour heatmap** — 7×24 grid of review counts (most-frequent review time); color scale. Optional second heatmap for retention % per weekday×hour (cheap once data is grouped).
5. **Headline tiles** — Total reviews (all-time / last-365 / today), Retention % (correct / answered, last-365), Current streak & Longest streak (consecutive local days with ≥1 answered review), Total time spent (sum of valid `thinkingTimeMs`, formatted h/m), Reviews today.
6. **Best/worst review hour by retention** — small ranked list (top-3 / bottom-3 hours by retention %, min-N-reviews guard).

### Precise metric definitions

- **Review (answer)** = one answered `recall_prompt` (i.e. `quiz_answer_id IS NOT NULL`). Counted at `quiz_answer.created_at`.
- **Response time (PRIMARY)** = `quiz_answer.thinking_time_ms` (the client-measured, pause-aware value already on `Answer`). `[VERIFIED: backend/.../entities/Answer.java + frontend/.../composables/useThinkingTimeTracker.ts]`
- **Response time (FALLBACK)** = `quiz_answer.created_at − recall_prompt.created_at` (ms), used **only** when `thinking_time_ms IS NULL` (legacy rows). `[VERIFIED: RecallPrompt.getAnswerTime() returns answer.getCreatedAt()]`
- **Retention / accuracy** = `count(correct = true) / count(answered)` over the window. `Answer.correct` is non-null `[VERIFIED: Answer.java @NotNull correct]`.
- **Day / hour bucketing** = convert `quiz_answer.created_at` (UTC) to the user's `ZoneId` (from `timezone` param), then take local `LocalDate` / `LocalTime.getHour()`. Weekday = `DayOfWeek` (Mon=1…Sun=7) of the user's local date.
- **Streak** = consecutive user-local dates with ≥1 answered review up to today (current) and longest run (all-time). "Today" boundary uses `testabilitySettings.getCurrentUTCTimestamp()` + user `ZoneId` (matches `getMenuData` testability pattern).
- **Total time spent** = sum of valid response times (after outlier filtering) across the window.

### Outlier removal for response-time averages

`thinkingTimeMs` is already pause/blur/visibility-aware, so the dominant outlier is **too-short** (misclicks / accidental instant answers). The timestamp-diff fallback additionally suffers **too-long** (user walked away before submitting, since the diff includes wall-clock but the client timer self-pauses — note the diff can even be *less* than `thinkingTimeMs` is not possible; the diff is always ≥ `thinkingTimeMs`).

**Recommended approach — trimmed mean with fixed guards:**

1. **Drop too-short:** discard response times `< 1000 ms` (1 s). Rationale: a real recall answer to a MCQ/spelling prompt takes ≥1s; sub-second values are misclicks or double-clicks. `[ASSUMED threshold — standard practice; Anki's revlog `time` is similarly noisy at the low end]`
2. **Cap too-long (winsorize):** for `thinkingTimeMs`, clamp values `> 120000 ms` (120 s) down to 120 s. Rationale: the existing curve already clamps at 60s for scheduling; for *stats* a 120s ceiling keeps "long but genuine" deliberation while killing absurd values. For the **timestamp-diff fallback**, clamp `> 300000 ms` (300 s / 5 min) — the diff includes away time, so it needs a looser ceiling. `[ASSUMED thresholds — tune after seeing the distribution]`
3. **Average = trimmed mean P5–P95:** after the guards above, drop the bottom 5% and top 5% of the remaining values in each bucket (per day / per hour / per AM-PM), then take the mean. This is robust to the long right tail of response times and more stable day-to-day than a raw mean. `[ASSUMED — robust statistics norm]`
4. **Minimum sample guard:** show a bucket's average only if it has `≥ 3` valid responses; otherwise render the bucket as "insufficient data" (no point / greyed cell). Prevents a single fast answer from skewing a day.
5. **Alternative (simpler):** if the team prefers one number, use the **median** per bucket — fully robust to both tails, no threshold tuning. Slightly less familiar to users than "average". Recommend trimmed mean as primary, median as a fallback if P5–P95 feels heavy.

**Why not raw mean:** response-time distributions are right-skewed; a single 10-minute "walked away" answer (which survives even the 120s clamp for the diff) can dominate a day's mean. Trimmed mean + min-sample guard is the standard fix and matches the user's "reasonable averaging, remove too-short and too-long outliers" intent.

## Part 3 — Implementation Notes

### Charting approach (frontend, Vue 3 + DaisyUI 5.7 + Tailwind 4)

**Recommendation: hand-rolled SVG/CSS — no new dependency.** `[VERIFIED: frontend/package.json has NO chart library (no chart.js/echarts/d3/apex/unovis/plotly); root package.json likewise none]`. Rationale:
- The three visualizations are simple shapes: a fixed 53×7 cell grid (calendar), a 7×24 cell grid (heatmap), and a single-line path (trend). All trivial in inline SVG with Tailwind/DaisyUI classes for theming.
- Zero bundle cost, no version/syncpack churn, full control of DaisyUI color tokens (`base-100/200/300`, `primary`), accessible tooltips via `<title>` or a small popover.
- The repo already uses `gsap` (animation) and `@lucide/vue` (icons) if any polish is needed; `vue-content-loader` exists for skeletons.
- A GitHub-style calendar maps cleanly to an SVG `<rect>` per day with `fill` from a 5-step scale; weekday×hour heatmap is the same pattern; trend is one `<polyline>` + axis labels.

**Lightweight library candidates (only if the team prefers a lib over SVG) — all `[ASSUMED]`, planner MUST run the Package Legitimacy Gate before installing:**
- `chart.js` + `vue-chartjs` — most common Vue chart lib; good line/bar, weak for heatmaps (you'd still hand-roll the calendar).
- `unovis` (`@unovis/ts` + `@unovis/vue`) — has a calendar/heatmap first-class; modern, framework-agnostic.
- `apexcharts` + `vue3-apexcharts` — heatmaps and line out of the box; heavier bundle.

Given the calendar/heatmap are hand-rolled regardless, a lib only helps the trend line — not worth a dependency. **Default to SVG/CSS.**

### Backend aggregation

- **New endpoint:** `GET /api/user/recall-stats?timezone=…` on `UserController` (`/api/user` already mapped) `[VERIFIED: UserController.java]`. Follow `getMenuData` exactly: `authorizationService.assertLoggedIn()` → `getCurrentUser()` → `TimezoneUtils.parseTimezone(timezone)` → `testabilitySettings.getCurrentUTCTimestamp()` as the "now" upper bound. Mark `@Transactional(readOnly = true)` so the lazy `RecallPrompt.answer` OneToOne loads within the transaction.
- **Reuse the existing query:** `RecallPromptRepository.findAnsweredRecallPromptsInTimeRange(userId, startTime, endTime)` already returns answered prompts ordered by answer time and is the documented data source `[VERIFIED: RecallPromptRepository.java; used by RecallService + QuestionGenerationBatchPlanningService]`. Call it with:
  - a **365-day window** (`now − 365d` … `now`) for the calendar, hourly breakdown, weekday×hour, and 90-day trend;
  - an **all-time window** (`epoch` … `now`, or `now − 5y` if performance matters) for total reviews, retention, streak, total time. If a single all-time call is cheap enough, one call covers everything; otherwise two calls.
- **New service:** `RecallStatsService` (under `services/`) takes the list of `RecallPrompt`s + `ZoneId` + "now" and computes all buckets. Keep aggregation in Java (not SQL) — the row count per user over 365 days is modest, and Java gives easy access to `answer.getThinkingTimeMs()`, `answer.getCorrect()`, and `ZoneId` conversion via `Timestamp.toInstant().atZone(zoneId)`. This avoids new native queries.
- **DTO shape (new `RecallStatsDTO`):** introduce a response DTO because the wire shape aggregates many sources and differs from the entity `[per .cursor/rules/backend-code.mdc: "Introduce a response DTO only when necessary, such as … aggregating multiple sources"]`. Suggested shape:

```java
record RecallStatsDTO(
    List<DayCount> calendar,          // [{date: "2026-07-23", count: 12}, ...]  (365 entries, 0-filled)
    List<DayAvgResponseTime> trend,   // [{date, avgMs, sampleSize}, ...] last 90d
    AmPmResponseTime amPm,             // {morningMs, afternoonMs, eveningMs, nightMs, ...samples}
    int[][] weekdayHourCounts,         // [7][24] review counts
    int[][] weekdayHourCorrect,        // [7][24] correct counts (for retention heatmap)
    HeadlineStats totals               // {totalReviewsAllTime, totalReviews365, reviewsToday,
                                       //  retentionPct365, currentStreak, longestStreak,
                                       //  totalTimeSpentMs, bestHour, worstHour}
) {}
```
  (Use Lombok `@Data` + nested records/`@AllArgsConstructor` to match `MenuDataDTO` style `[VERIFIED: MenuDataDTO.java]`. Send dates as ISO `yyyy-MM-dd` strings to avoid TZ ambiguity on the client; the client renders them as-is.)

- **After backend changes:** regenerate the API client — `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`. Never hand-edit `packages/generated/**` `[VERIFIED: agent-map.md]`.

### Timezone handling

- Pass the browser IANA timezone via `timezoneParam()` from `@/managedApi/window/timezoneParam` as the `timezone` query param — identical to `getMenuData` usage `[VERIFIED: timezoneParam.ts uses Intl.DateTimeFormat().resolvedOptions().timeZone]`.
- **All day/hour/weekday grouping happens on the backend** using `TimezoneUtils.parseTimezone(timezone)` → `ZoneId`, converting each `quiz_answer.created_at` (UTC) to user-local before bucketing. This keeps one source of truth and matches the `getMenuData` precedent (server owns TZ-sensitive grouping).
- "Today" / streak boundaries use `testabilitySettings.getCurrentUTCTimestamp()` + user `ZoneId` (so tests can freeze time) `[VERIFIED: getMenuData uses testabilitySettings.getCurrentUTCTimestamp()]`.
- The client never re-derives day/hour from raw timestamps; it only renders pre-bucketed DTOs. This avoids double-conversion bugs.

### Frontend wiring

- **New tab:** add `{ name: "settingsRecallStats", label: "Recall Stats" }` to the `tabs[]` array in `frontend/src/pages/SettingsPage.vue` `[VERIFIED]`.
- **New route child:** add to `settingsNestedRoute.children` in `frontend/src/routes/routes.ts` (`path: "recall-stats"`, name `settingsRecallStats`, component `RecallStatsSettingsTab`) and import the new component `[VERIFIED]`.
- **New component:** `frontend/src/pages/settings/RecallStatsSettingsTab.vue` (mirror `GeneralSettingsTab.vue` / `AccessTokensSettingsTab.vue`): `onMounted` fetch via `UserController.getRecallStats({ query: { timezone: timezoneParam() } })` using `apiCallWithLoading` (read-only, no `blockUi` — keep the page interactive; show `ContentLoader` skeleton until data arrives) `[VERIFIED: frontend-api.mdc + GeneralSettingsTab.vue pattern]`.
- **Chart sub-components** under `frontend/src/components/recallStats/`: `RecallActivityCalendar.vue`, `ResponseTimeTrendChart.vue`, `WeekdayHourHeatmap.vue`, `RecallStatsTiles.vue` — each pure-SVG, props-driven, testable in isolation.
- **Tests:** `frontend/tests/` with `mockSdkService()` + `makeMe` fixtures; add a `RecallStatsSettingsTab.spec.ts` and per-chart specs asserting rendered SVG cells/points from a fixture DTO `[VERIFIED: agent-map.md frontend test pattern]`.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Vue 3 | 3.5.40 | UI framework | Already in repo `[VERIFIED: frontend/package.json]` |
| DaisyUI | 5.7.0 | Component theming | Already in repo; use `base-*`/`primary` tokens for chart fills |
| Tailwind CSS | 4.3.3 | Styling | Already in repo |
| Spring Boot (Java) | existing | Backend API + aggregation | Already in repo; reuse `RecallPromptRepository` |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Inline SVG + CSS | — | Calendar / heatmap / trend | **Primary** — zero deps, simple shapes |
| `vue-content-loader` | 2.0.1 | Loading skeleton | Already in repo; use while stats load |
| `gsap` | ^3.15.0 | Optional chart entrance animation | Already in repo; only if polish desired |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Hand-rolled SVG calendar/heatmap | `unovis` / `apexcharts` | Lib adds bundle weight + a legitimacy gate; shapes are simple → SVG wins |
| Hand-rolled trend line | `chart.js` + `vue-chartjs` | Only helps one line; not worth a dependency for a single `<polyline>` |
| Java-side aggregation | SQL `GROUP BY` native query | Java reuses `ZoneId` + lazy `answer` easily; avoids new native queries; row volume modest |

**Installation:** No new packages required for the recommended path. If a chart lib is chosen, run the Package Legitimacy Gate first (see below).

## Package Legitimacy Audit

> The recommended path installs **no** external packages (hand-rolled SVG/CSS). The chart-lib candidates below are listed only as alternatives the team *may* choose; none are recommended for install. Per protocol, packages discovered via training data are tagged `[ASSUMED]` — the planner MUST run `gsd-tools query package-legitimacy check --ecosystem npm <pkg>` and add a `checkpoint:human-verify` task before installing any of them.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| `chart.js` | npm | `[ASSUMED]` ~10y | `[ASSUMED]` very high | `[ASSUMED]` chartjs/chart.js | `[ASSUMED]` OK | Not recommended; gate if chosen |
| `vue-chartjs` | npm | `[ASSUMED]` ~7y | `[ASSUMED]` high | `[ASSUMED]` chartjs/vue-chartjs | `[ASSUMED]` OK | Not recommended; gate if chosen |
| `@unovis/ts` | npm | `[ASSUMED]` ~3y | `[ASSUMED]` low-mid | `[ASSUMED]` felixmos/unovis | `[ASSUMED]` SUS (newer, lower downloads) | Flag — verify before using |
| `apexcharts` | npm | `[ASSUMED]` ~8y | `[ASSUMED]` high | `[ASSUMED]` apexcharts/apexcharts | `[ASSUMED]` OK | Not recommended; gate if chosen |
| `vue3-apexcharts` | npm | `[ASSUMED]` ~4y | `[ASSUMED]` mid | `[ASSUMED]` | `[ASSUMED]` | Not recommended; gate if chosen |

**Packages removed due to [SLOP] verdict:** none (no SLOP detected; all are well-known libs, but versions/signals not verified this session).
**Packages flagged as suspicious [SUS]:** `@unovis/ts` — newer with lower download volume; if chosen, planner must `checkpoint:human-verify` and confirm the source repo before install.

*No package install is needed for the recommended SVG/CSS path.*

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Response-time measurement | A new "time shown → answered" timer | `Answer.thinkingTimeMs` (already captured, pause-aware) | Already exists, tested, blur/visibility-aware `[VERIFIED]` |
| Answered-prompt query | A new SQL query | `RecallPromptRepository.findAnsweredRecallPromptsInTimeRange` | Already exists, already used by RecallService + batch planner `[VERIFIED]` |
| Auth + current user | Custom auth | `authorizationService.assertLoggedIn()` + `getCurrentUser()` | Existing pattern `[VERIFIED: UserController.getMenuData]` |
| Timezone parsing | Custom TZ handling | `TimezoneUtils.parseTimezone` + `ZoneId` | Existing; falls back to UTC `[VERIFIED]` |
| "Now" for testability | `new Date()` / `System.currentTimeMillis()` | `testabilitySettings.getCurrentUTCTimestamp()` | Tests can freeze time `[VERIFIED: getMenuData]` |
| Browser IANA timezone | Custom detection | `timezoneParam()` | Existing; uses `Intl` `[VERIFIED]` |
| API client | Hand-written fetch | `@generated/…/sdk.gen` + `apiCallWithLoading` | Existing pattern; regenerate after DTO change `[VERIFIED: frontend-api.mdc]` |

**Key insight:** Everything *around* the statistics already exists in Doughnut — the response-time field, the answered-prompt query, auth, timezone, testability clock, and the API-client pipeline. The only net-new work is one aggregation service + DTO + one endpoint + one Settings tab + SVG charts. Do **not** reinvent the response-time metric; `thinkingTimeMs` is the right source.

## Common Pitfalls

### Pitfall 1: Using wall-clock diff instead of `thinkingTimeMs`
**What goes wrong:** Averages look huge and noisy because the diff includes paused/away time.
**Why:** `answer.createdAt − recall_prompt.createdAt` is wall-clock; the client timer self-pauses on blur/hidden.
**How to avoid:** Use `thinkingTimeMs` as primary; diff only as a null-fallback with a looser (300s) cap.
**Warning signs:** daily averages swing 10×+ on days the user left a tab open.

### Pitfall 2: Forgetting `@Transactional(readOnly = true)`
**What goes wrong:** `LazyInitializationException` when reading `recallPrompt.getAnswer()…`.
**Why:** `findAnsweredRecallPromptsInTimeRange` is a native query returning `rp.*`; the `answer` OneToOne loads lazily.
**How to avoid:** Annotate the endpoint `@Transactional(readOnly = true)` (mirror `getMenuData`).
**Warning signs:** 500s only when accessing answer fields, in tests without a session.

### Pitfall 3: Client-side timezone re-bucketing
**What goes wrong:** Calendar/heatmap days shift by one vs. the user's view; "today" mismatch.
**Why:** Sending raw UTC timestamps and bucketing on the client double-converts.
**How to avoid:** Bucket on the backend with the `timezone` param; send ISO date strings + pre-counted arrays in the DTO.
**Warning signs:** "Reviews today" tile disagrees with the calendar's today cell.

### Pitfall 4: Single fast answer skews a day's average
**What goes wrong:** A day with one 300ms answer shows a tiny "average" that looks great but is meaningless.
**Why:** No minimum-sample guard.
**How to avoid:** Require `≥ 3` valid responses per bucket; render "insufficient data" otherwise.
**Warning signs:** jagged trend line driven by low-sample days.

### Pitfall 5: OpenAPI `operationId` / path collision
**What goes wrong:** `pnpm generateTypeScript` fails or lints fail on duplicate operationIds.
**Why:** New endpoint under `/api/user` must have a unique operationId/method.
**How to avoid:** Give the controller method a distinct name (`getRecallStats`) and unique `@Operation` if annotated; run `pnpm openapi:lint` after generating `[CITED: .cursor/rules/linting_formating.mdc]`.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `thinkingTimeMs` (not the timestamp diff) should be the primary response-time metric — deviates from user's stated definition | Part 2, Metric definitions | User may specifically want wall-clock diff; confirm before locking. Low risk — `thinkingTimeMs` is strictly more accurate. |
| A2 | Sub-second (<1000ms) responses are misclicks to drop | Part 2, Outliers | Some legit fast recalls could be lost; threshold tunable. |
| A3 | 120s cap for `thinkingTimeMs`, 300s cap for diff fallback | Part 2, Outliers | May over/under-trim; tune after seeing real distribution. |
| A4 | Trimmed mean P5–P95 is the right average | Part 2, Outliers | Median is a valid simpler alternative; user may prefer one. |
| A5 | Per-user 365-day answered-prompt row count is small enough for Java-side aggregation | Part 3, Backend | If a power user has 100k+ rows, consider SQL `GROUP BY` or a shorter window. |
| A6 | `RecallSessionOptionsDialog` "Average thinking time" precedent generalizes to cross-day stats | Part 2 | Confirms `thinkingTimeMs` averaging is an established pattern `[VERIFIED]`. |
| A7 | Chart-lib candidates (`chart.js`, `unovis`, `apexcharts`) are legitimate | Standard Stack, Package Audit | Not verified this session; gate before install. |
| A8 | RemNote/Quizlet stats features as described | Part 1 | Lower confidence; not central to recommendations. |

## Open Questions

1. **Response-time metric choice (A1)** — `thinkingTimeMs` vs `answer.createdAt − recall_prompt.createdAt`.
   - What we know: `thinkingTimeMs` is already captured, pause-aware, clamped at 60s; the diff includes away time.
   - What's unclear: whether the user specifically wants wall-clock "shown→answered" semantics regardless.
   - Recommendation: confirm with user; default to `thinkingTimeMs` (primary) + diff (null-fallback).

2. **All-time vs capped window for totals/streak (A5)** — performance of a multi-year all-time fetch.
   - What we know: query is indexed on `qa.created_at` + `mt.user_id`.
   - What's unclear: max rows per user.
   - Recommendation: start with a 5-year cap; revisit if slow.

3. **Average type (A4)** — trimmed mean vs median.
   - Recommendation: trimmed mean P5–P95; offer median if the team wants simpler.

## Environment Availability

Step 2.6: minimal — this phase adds no external runtime dependency. All required tooling already exists in the repo/Nix shell.

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Spring Boot / Java | Backend endpoint + service | ✓ | existing (repo) | — |
| Vue 3 + DaisyUI + Tailwind | Frontend tab + SVG charts | ✓ | 3.5.40 / 5.7.0 / 4.3.3 `[VERIFIED]` | — |
| `pnpm generateTypeScript` | API client regen | ✓ | via Nix | — |
| MySQL | `recall_prompt`/`quiz_answer` reads | ✓ | `pnpm sut` assumed running | — |

**Missing dependencies with no fallback:** none.
**Missing dependencies with fallback:** none.

## Validation Architecture

> `workflow.nyquist_validation: true` in `.planning/config.json` `[VERIFIED]` → section included.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Backend: JUnit/Gradle via `pnpm backend:test_only`; Frontend: Vitest 4.1.10 (browser=chromium) `[VERIFIED: frontend/package.json]` |
| Config file | Frontend: `frontend/vitest.config.ts`; Backend: `backend/build.gradle` |
| Quick run command | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` ; `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/path/to/spec.ts` |
| Full suite command | `CURSOR_DEV=true nix develop -c pnpm backend:verify` ; `CURSOR_DEV=true nix develop -c pnpm frontend:test` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| RS-01 | `GET /api/user/recall-stats` returns scoped-to-user aggregated stats for a 365-day window | integration (controller) | `pnpm backend:test_only` (controller test) | ❌ Wave 0 |
| RS-02 | Response-time average uses `thinkingTimeMs`, drops <1s, caps long, trimmed P5–P95, min-3 guard | unit (service) | `pnpm backend:test_only` (RecallStatsServiceTest) | ❌ Wave 0 |
| RS-03 | Day/hour/weekday bucketing respects the `timezone` param (UTC→user-local) | unit (service) | `pnpm backend:test_only` | ❌ Wave 0 |
| RS-04 | Streak computed over user-local consecutive days using testability clock | unit (service) | `pnpm backend:test_only` | ❌ Wave 0 |
| RS-05 | Settings "Recall Stats" tab renders calendar/heatmap/trend/tiles from a fixture DTO | unit (component) | `pnpm frontend:test tests/pages/settings/RecallStatsSettingsTab.spec.ts` | ❌ Wave 0 |
| RS-06 | SVG calendar renders 365 cells + correct fill scale from fixture | unit (component) | `pnpm frontend:test tests/components/recallStats/RecallActivityCalendar.spec.ts` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `pnpm backend:test_only` (touched) + `pnpm frontend:test <touched spec>`
- **Per wave merge:** `pnpm backend:verify` + `pnpm frontend:test`
- **Phase gate:** Full touched suites green before `/gsd-verify-work`; do **not** run full E2E unless required.

### Wave 0 Gaps
- [ ] `backend/src/test/java/.../controllers/UserControllerRecallStatsTest.java` — covers RS-01
- [ ] `backend/src/test/java/.../services/RecallStatsServiceTest.java` — covers RS-02/03/04
- [ ] `frontend/tests/pages/settings/RecallStatsSettingsTab.spec.ts` — covers RS-05
- [ ] `frontend/tests/components/recallStats/RecallActivityCalendar.spec.ts` (+ heatmap/trend specs) — covers RS-06
- [ ] Framework install: none (Vitest + JUnit already present)

## Security Domain

> `security_enforcement: true`, `security_asvs_level: 1`, `security_block_on: high` in `.planning/config.json` `[VERIFIED]`. Surface is minimal: one read-only endpoint serving the authenticated user's own data.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes (inherit) | Existing session auth via `authorizationService.assertLoggedIn()` |
| V3 Session Management | yes (inherit) | Existing session mechanism |
| V4 Access Control | yes | `authorizationService.getCurrentUser()` — stats scoped to logged-in user's `id`; repo query filters `mt.user_id = :userId`. Never accept a `userId` from the client. |
| V5 Input Validation | yes | `timezone` param validated via `TimezoneUtils.parseTimezone` (falls back to UTC on bad input); no other client-controlled inputs. |
| V6 Cryptography | no | No secrets/PII beyond existing user data |

### Known Threat Patterns for Spring Boot + user-scoped read endpoint

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| IDOR / accessing another user's stats | Info disclosure / Elevation | Server forces `userId = currentUser().getId()`; no client-supplied user id `[VERIFIED: getMenuData pattern]` |
| Timezone injection / TZ DB bombing | Tampering / DoS | `ZoneId.of` wrapped in try/catch → UTC fallback `[VERIFIED: TimezoneUtils]` |
| Large result-set exhaustion (DoS) | DoS | Cap windows (365d / 5y); Java-side aggregation over bounded rows |
| PII exposure (review timestamps) | Info disclosure | Endpoint is user-scoped (own data only); no cross-user data |

## Sources

### Primary (HIGH confidence)
- Anki Manual — Statistics: https://docs.ankiweb.net/stats.html (Calendar, Hourly Breakdown, Review Time, Answer Buttons, True Retention, revlog `time` field) `[CITED]`
- Anki `statistics.ftl` — "average answer time", reviews-per-day labels: https://github.com/ankitects/anki/blob/…/ftl/core/statistics.ftl `[CITED]`
- SuperMemo Help — Statistics / Analysis: https://help.supermemo.org/wiki/Statistics , /wiki/Analysis `[CITED]`
- Mochi changelog + docs — Dashboard heatmap, line charts, review-time/forecast/intervals, total review time, all-time toggle: https://mochi.cards/changelog , https://mochi.cards/docs/cards/ `[CITED]`
- Codebase (verified by Read/Grep this session): `RecallPrompt.java`, `Answer.java` (`thinking_time_ms`), `ForgettingCurve.java` (`MAX_THINKING_TIME_MS=60000`), `RecallPromptRepository.java`, `UserController.java` (`getMenuData`), `RecallService.java`, `TimezoneUtils.java`, `MenuDataDTO.java`, `useThinkingTimeTracker.ts` (pause/blur/visibility-aware), `QuestionDisplay.vue`, `RecallSessionOptionsDialog.vue` (avg-thinking-time precedent), `SettingsPage.vue`, `routes.ts`, `frontend/package.json` (no chart lib), `timezoneParam.ts`, `.cursor/rules/{backend-code,frontend-api,linting_formating}.mdc`, `.planning/config.json`.

### Secondary (MEDIUM confidence)
- WebSearch syntheses for Anki/SuperMemo/Mochi (cross-checked against the official pages above).

### Tertiary (LOW confidence)
- RemNote / Quizlet stats descriptions `[ASSUMED]` — not central to recommendations; flagged A8.

## Metadata

**Confidence breakdown:**
- Competitive survey: HIGH — grounded in official docs for Anki/SuperMemo/Mochi.
- Data model & implementation: HIGH — every claim verified by reading the actual source this session.
- Outlier thresholds (A2/A3/A4): LOW/MEDIUM — standard practice but unverified against Doughnut's real distribution; tune after first data.

**Research date:** 2026-07-23
**Valid until:** 2026-08-22 (30 days; stable stack, no fast-moving deps)




