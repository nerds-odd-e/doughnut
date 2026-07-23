# Quick Task: Recall Statistics Tab - Research (Retention-First)

**Researched:** 2026-07-23 (revised — centered on correct answer rate / retention)
**Domain:** Spaced-repetition analytics (backend aggregation + Vue 3 charts)
**Confidence:** HIGH (competitive survey from official docs; codebase findings verified by grep/read in the prior session and reused here verbatim)

## Summary

The single most important spaced-repetition statistic is **correct answer rate (retention / accuracy)** — every major SR app treats it as first-class, not secondary. Anki gives it **three** dedicated surfaces: **Answer Buttons** (% correct per card type), the **True Retention Table** (retention by interval band, mature = interval ≥21d, first review per day only), and the **Hourly Breakdown** (a shaded success-rate-% region overlaid on the review-count bars per hour) `[CITED: docs.ankiweb.net/stats.html]`. SuperMemo's whole tuning loop is the **forgetting index** (requested vs measured; FI≈10% ⇒ retention≈95%) `[CITED: help.supermemo.org/wiki/Forgetting_index_in_SuperMemo]`. FSRS defines memory stability as the time for recall to fall to **90%** and treats desired retention (~90%) as the user's primary knob `[CITED: expertium.github.io/Retention.html]`. Mochi records remembered/forgotten per review and charts retention over time `[CITED: mochi.cards/docs/cards]`. The user is right: retention is the headline SR health signal.

**Recommendation for Doughnut:** elevate retention to a first-class part of the new Settings → "Recall Stats" tab by adding (1) a **prominent overall retention % headline tile**, (2) a **daily correct-answer-rate (retention) trend line chart** — parallel to the daily response-time trend, same 30/90/all toggle — (3) **retention by hour** (Anki's Hourly Breakdown success-rate region), (4) the **weekday×hour retention heatmap** (alongside the existing count heatmap), and (5) the **best/worst review hour by retention** list. Everything else the user requested (calendar, daily response-time trend, morning/afternoon response-time, weekday×hour count heatmap, the other headline tiles) stays exactly as previously locked in CONTEXT.md. Elevating retention adds **one** new chart (the retention trend line) and one new headline number; the rest reuses data already grouped for the count heatmap.

**Critical codebase finding (reused from prior research, still `[VERIFIED]`):** Doughnut already captures a precise, pause-aware response time — `Answer.thinkingTimeMs` (`quiz_answer.thinking_time_ms`), measured client-side by `useQuestionThinkingTimeTracker`, which pauses on tab-hide / window-blur / page-hide and resumes on focus, and is already clamped to `[0, 60000]` ms in `ForgettingCurve` (`MAX_THINKING_TIME_MS = 60000`). This is strictly better than the wall-clock diff `answer.createdAt − recall_prompt.createdAt` (which includes away time). Use `thinkingTimeMs` as the **primary** response-time metric; the diff is a **fallback only** for legacy rows where `thinkingTimeMs` is null. `Answer.correct` is `@NotNull` non-null, so retention is a clean `count(correct=true)/count(answered)` over any window `[VERIFIED: backend/.../entities/Answer.java]`.

**Primary recommendation:** Ship one read-only `GET /api/user/recall-stats?timezone=…` endpoint that reuses `RecallPromptRepository.findAnsweredRecallPromptsInTimeRange` and returns a single `RecallStatsDTO`; render it in a new Settings → "Recall Stats" tab using **hand-rolled SVG/CSS** (no new chart dependency). **Retention is the lead statistic**: overall retention % headline + daily retention trend line + retention-by-hour + weekday×hour retention heatmap + best/worst hour by retention, alongside the previously-locked response-time views. Response-time averages use a **trimmed mean (P5–P95)** after dropping sub-second misclicks and capping long values; retention buckets use the same **≥3-sample guard** as response time.

## User Constraints (from CONTEXT.md)

### Locked Decisions (do not revisit)
- **Response-time metric — PRIMARY:** `Answer.thinkingTimeMs` (`quiz_answer.thinking_time_ms`), already captured client-side, pause/blur/visibility-aware, clamped to 60s in `ForgettingCurve`. Strictly better than wall-clock diff.
- **Response-time metric — FALLBACK:** `quiz_answer.created_at − recall_prompt.created_at` (ms), used ONLY when `thinking_time_ms IS NULL` (legacy rows). Cap diff at 300s.
- **Scope (one tab):** Calendar (GitHub-style, 365-day daily review counts); Daily average response-time trend (line, last 90d default + 30/90/all toggle); Morning vs afternoon response-time comparison (AM [06,12), PM [12,18); evening/night as extra bars if cheap); Weekday×hour heatmap of review counts (7×24); Headline tiles (Total reviews all-time/last-365/today, Retention % last-365, Current & Longest streak, Total time spent, Reviews today); Best/worst review hour by retention (top-3/bottom-3, min-N-reviews guard).
- **Averaging algorithm (response-time averages):** (1) drop `< 1000 ms`; (2) winsorize — clamp `thinkingTimeMs > 120000 ms` to 120s, clamp diff-fallback `> 300000 ms` to 300s; (3) trimmed mean P5–P95 per bucket; (4) min-sample guard — render a bucket only if `≥ 3` valid responses, else "insufficient data".
- **Architecture:** one read-only `GET /api/user/recall-stats?timezone=…` on `UserController` following the `getMenuData` pattern (`assertLoggedIn` → `getCurrentUser` → `TimezoneUtils.parseTimezone` → `testabilitySettings.getCurrentUTCTimestamp()`), `@Transactional(readOnly = true)`. Reuse `RecallPromptRepository.findAnsweredRecallPromptsInTimeRange`. New `RecallStatsService` + `RecallStatsDTO` (aggregation in Java, not SQL). All day/hour/weekday bucketing on the backend using the user's `ZoneId`; send ISO date strings + pre-counted arrays. Frontend: new `settingsRecallStats` tab + `RecallStatsSettingsTab.vue`, fetch via generated SDK + `apiCallWithLoading`, pass `timezoneParam()`. Hand-rolled SVG/CSS charts (NO new chart dependency). After backend DTO changes: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`; never hand-edit `packages/generated/**`.

### Claude's Discretion
- Exact DTO field names / nesting (mirror `MenuDataDTO` Lombok style).
- Calendar color-scale steps and SVG structure.
- 365-day vs YTD calendar window (recommend 365 trailing days).
- All-time vs 5-year cap for totals/streak (recommend 5-year cap; revisit if slow).

### Deferred Ideas (OUT OF SCOPE)
- Future-Due forecast; card-counts by maturity band; ease distribution; FSRS stability/difficulty.
- Open Question A5 (all-time vs capped window): default to 5-year cap for totals/streak.

> **Note on this revision:** CONTEXT.md predates the user's "retention is the most important stat" emphasis. This research elevates retention to first-class **within the already-locked scope** — it adds a daily retention trend chart and a retention headline (both derive from the same `answer.correct` data already grouped for the count heatmap), and reframes the existing weekday×hour heatmap + best/worst-hour list as **retention** views. No locked decision is reversed; the response-time metric, averaging algorithm, architecture, and tab scope are unchanged. Planner should treat "daily retention trend chart" + "overall retention % headline" as **new first-class requirements** flowing from the user's latest instruction.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Aggregation of review/answer rows (counts, retention, response time) | API / Backend | — | Grouping by user-local day/hour requires server-side `ZoneId` conversion of UTC `answer.createdAt`; reuses existing repo query. Each row already carries `answer.correct` and `answer.thinkingTimeMs`, so retention + response time are computed from the **same** fetched rows. |
| Retention % computation | API / Backend | — | `count(correct=true)/count(answered)` per bucket; min-sample guard applied server-side so the wire shape is final. |
| Calendar / heatmap / trend rendering (incl. retention trend + retention heatmap) | Browser / Client | — | Pure visualization of pre-aggregated DTOs; no client-side raw-row fetching or re-bucketing. |
| Timezone grouping | API / Backend | Browser | Backend owns grouping (one source of truth); browser only passes `timezone` param like `getMenuData`. |
| Outlier filtering + trimmed mean | API / Backend | — | Percentile/trim logic belongs with aggregation so the wire shape is final. |
| Auth / access control | API / Backend | — | `authorizationService.getCurrentUser()` scopes all rows to the logged-in user. |

## Part 1 — Competitive Product Research (Retention-First)

### The retention-first consensus (verified via official docs)

**Anki** `[CITED: docs.ankiweb.net/stats.html]` — the gold standard — surfaces correct-answer rate as **three** first-class statistics, more than any other metric:
- **Answer Buttons** graph: "% correct" per card type (learning / young / mature) — the headline accuracy view.
- **True Retention Table**: retention by interval band; a card is "mature" if its interval ≥21 days; **only the first review per day counts**; Again = Fail, Hard/Good/Easy = Pass. Used to check how well the SR algorithm is working. FSRS users expect true retention ≈ desired retention (~90%); "data for a single day is noisy, so it's better to look at monthly data."
- **Hourly Breakdown**: blue bars = review count per hour, **gray shaded region = success-rate % per hour** — i.e. retention-by-hour is plotted directly on the chart, so the user can see "I retain best at 11am, worst at 7pm." This is the canonical retention-vs-time-of-day view.
- "Today" block also shows a **correct %** alongside review/Again counts.

**SuperMemo** `[CITED: help.supermemo.org/wiki/Forgetting_index_in_SuperMemo + /wiki/Forgetting_index_FAQ]` — the entire tuning loop is built on retention, expressed as the **forgetting index (FI)**: requested FI (target) vs **measured FI** (displayed in the Statistics window). FI≈10% ⇒ retention≈95% (retention = 1 − FI/2 roughly, because retention decays from 100% post-review to the target at the next repetition). Measured FI is the primary "is my learning healthy?" number.

**FSRS** `[CITED: expertium.github.io/Retention.html + /Algorithm.html]` — defines memory stability S as the time for recall R to fall from 100% to **90%**; **desired retention** (~90%) is the user's main algorithm knob. Distinction worth borrowing for Doughnut's UI: **desired retention** ("recall % when due") vs **measured retention** ("recall % of all cards today") — measured is usually a few points higher. Doughnut's stat is the *measured* kind (correct/answered over a window), which is exactly what users want to see trending.

**Mochi** `[CITED: mochi.cards/docs/cards + /changelog]` — per-card review history records remembered/forgotten, interval, duration; Dashboard charts **retention over time** alongside the review heatmap and review-time line charts.

**RemNote / Quizlet** `[ASSUMED]` — RemNote shows retention % and mature-vs-learning counts in the queue; Quizlet has streaks/mastery but no true SR retention. Lower confidence, not central.

### Distilled guidance for Doughnut (retention as the lead statistic)

1. **Overall retention % headline tile** — the single most-glanced number; Anki's "Today correct %" / SuperMemo's measured-retention equivalent. Compute over the last-365 window (matches the locked "Retention % (last-365)" tile). Show `count(correct=true)/count(answered)` as a percentage; show the answered-count denominator small underneath so users trust it.
2. **Daily correct-answer-rate (retention) trend line** — *new first-class chart*. Parallel to the daily response-time trend, same 30/90/all toggle, one point per day = that day's retention %. This is the "is my recall getting better or worse over time?" view that Anki's True Retention table approximates monthly; a daily line gives finer signal while still being noise-manageable via the min-sample guard. Y-axis 0–100%.
3. **Retention by hour** — Anki's Hourly Breakdown success-rate region. One retention % per hour-of-day (0–23), with the review count shown so significance is visible. Cheap: same grouping as the count heatmap, just swap numerator.
4. **Weekday×hour retention heatmap** — the locked 7×24 heatmap, but add a **second** heatmap (or a toggle) for retention % per weekday×hour. Same denominator data, different fill scale (red→green for low→high retention).
5. **Best/worst review hour by retention** — the locked top-3/bottom-3 list, now explicitly framed as *retention* ranking (not response time), with the min-N-reviews guard.

**Does elevating retention change the locked tight set?** It **adds** the daily retention trend chart (one new SVG line) and makes the overall retention % a prominent headline. Everything else stays. The retention-by-hour, weekday×hour retention heatmap, and best/worst-hour list are all derived from data already grouped for the count heatmap (each row has `answer.correct`), so they are nearly free. The response-time views (daily trend, morning/afternoon, total-time tile) remain as locked — retention and response time are **complementary** signals (Anki ships both), not competing ones.

## Part 2 — Doughnut-Specific Recommendations

### Exact set of statistics to implement now (retention lead, response-time second)

**Retention (first-class — the lead statistic):**
1. **Overall retention % headline tile** — `count(correct=true)/count(answered)` over last-365d, with answered-count denominator.
2. **Daily retention trend line** — one point per day = day's retention %, last 90d default + 30/90/all toggle. Y-axis 0–100%. Min-3-answered guard per day, else "insufficient data" gap.
3. **Retention by hour** — retention % per hour-of-day (0–23), with review-count shown for significance.
4. **Weekday×hour retention heatmap** — 7×24 grid of retention % (red→green), alongside the count heatmap.
5. **Best/worst review hour by retention** — top-3 / bottom-3 hours by retention %, min-N-reviews guard.

**Activity & response-time (locked, unchanged):**
6. Activity calendar — 365-day daily answered-review counts, GitHub-style grid.
7. Daily average response-time trend — line, last 90d default + 30/90/all toggle, seconds.
8. Morning vs afternoon response-time — AM [06,12) vs PM [12,18) avg; evening/night extra bars if cheap.
9. Weekday×hour count heatmap — 7×24 review counts (most-frequent review time).
10. Headline tiles — Total reviews (all-time/last-365/today), Current & Longest streak, Total time spent, Reviews today (retention % tile is #1 above).

### Precise metric definitions

- **Review (answer)** = one answered `recall_prompt` (`quiz_answer_id IS NOT NULL`), counted at `quiz_answer.created_at`.
- **Retention / accuracy (PRIMARY statistic)** = `count(answer.correct = true) / count(answered)` over the window. `Answer.correct` is non-null `[VERIFIED: Answer.java @NotNull correct]`. Per-bucket retention uses the **same ≥3-answered guard** as response time: show a bucket's retention % only if it has `≥ 3` answered reviews, else render "insufficient data" (no point / greyed cell). This prevents a 1/1 = 100% day from looking like perfect recall. `[ASSUMED guard threshold — matches the locked response-time guard]`
- **Response time (PRIMARY)** = `quiz_answer.thinking_time_ms` (client-measured, pause-aware) `[VERIFIED: Answer.java + useThinkingTimeTracker.ts]`.
- **Response time (FALLBACK)** = `quiz_answer.created_at − recall_prompt.created_at` (ms), only when `thinking_time_ms IS NULL` `[VERIFIED: RecallPrompt.getAnswerTime()]`.
- **Day / hour / weekday bucketing** = convert `quiz_answer.created_at` (UTC) to the user's `ZoneId` (from `timezone` param), then local `LocalDate` / `LocalTime.getHour()` / `DayOfWeek` (Mon=1…Sun=7).
- **Streak** = consecutive user-local dates with ≥1 answered review up to today (current) and longest run (all-time); "today" boundary uses `testabilitySettings.getCurrentUTCTimestamp()` + user `ZoneId` `[VERIFIED: getMenuData testability pattern]`.
- **Total time spent** = sum of valid response times (after outlier filtering) across the window.

### Outlier removal for response-time averages (reaffirmed, unchanged)

`thinkingTimeMs` is already pause/blur/visibility-aware, so the dominant outlier is **too-short** (misclicks). The timestamp-diff fallback additionally suffers **too-long** (away time). Use the locked approach: (1) drop `< 1000 ms`; (2) winsorize — clamp `thinkingTimeMs > 120000 ms` to 120s, diff-fallback `> 300000 ms` to 300s; (3) trimmed mean P5–P95 per bucket; (4) min-3 guard. (Median is a valid simpler fallback if P5–P95 feels heavy.) Retention buckets need **no** outlier removal (correct is boolean) — only the ≥3-answered guard.

### Updated DTO shape (retention elevated)

```java
record RecallStatsDTO(
    List<DayCount> calendar,            // [{date, count}, ...] 365 entries, 0-filled
    List<DayAvgResponseTime> trend,     // [{date, avgMs, sampleSize}, ...] last 90d  (response time)
    List<DayRetention> retentionTrend,  // [{date, retentionPct, correctCount, answeredCount, sampleSize}, ...] last 90d  (NEW — retention)
    AmPmResponseTime amPm,               // {morningMs, afternoonMs, eveningMs, nightMs, ...samples}
    int[][] weekdayHourCounts,           // [7][24] review counts (denominators for retention)
    int[][] weekdayHourCorrect,          // [7][24] correct counts  -> retention % = correct/counts (for retention heatmap + best/worst hour)
    HourRetention[] hourlyRetention,     // [{hour: 0..23, retentionPct, correctCount, answeredCount}, ...] (retention by hour)
    HeadlineStats totals                 // {totalReviewsAllTime, totalReviews365, reviewsToday,
                                         //  retentionPct365, currentStreak, longestStreak,
                                         //  totalTimeSpentMs, bestHour, worstHour,
                                         //  bestHourRetentionPct, worstHourRetentionPct}
) {}
```
- `DayRetention` carries `correctCount` + `answeredCount` + `sampleSize` so the client can show the denominator and the planner can later switch to a different smoothing without a DTO change.
- `weekdayHourCorrect` + `weekdayHourCounts` together give the weekday×hour retention heatmap and the best/worst-hour-by-retention list (no separate query).
- `hourlyRetention` is the retention-by-hour series (Anki's Hourly Breakdown success-rate region).
- `totals.retentionPct365` is the prominent overall retention headline; `bestHour`/`worstHour` + their retention % are the best/worst-hour list.
- Use Lombok `@Data` + nested records/`@AllArgsConstructor` to match `MenuDataDTO` style `[VERIFIED: MenuDataDTO.java]`. Dates as ISO `yyyy-MM-dd` strings to avoid TZ ambiguity on the client.

## Part 3 — Implementation Notes (updated)

### Charting approach (frontend, Vue 3 + DaisyUI 5.7 + Tailwind 4)

**Hand-rolled SVG/CSS — no new dependency** `[VERIFIED: frontend/package.json + root package.json have NO chart library]`. The retention trend is a **single `<polyline>`** — the same hand-rolled SVG/CSS approach as the response-time trend, just a different Y-axis (0–100% instead of seconds) and a different color (e.g. `success`/green token for retention vs `primary` for response time). The retention heatmap reuses the exact 7×24 `<rect>` grid of the count heatmap with a red→green fill scale. Zero new dependencies, zero bundle cost.

### Backend aggregation (updated — retention is nearly free)

- **New endpoint:** `GET /api/user/recall-stats?timezone=…` on `UserController` (`/api/user` already mapped) `[VERIFIED: UserController.java]`. Follow `getMenuData` exactly: `assertLoggedIn()` → `getCurrentUser()` → `TimezoneUtils.parseTimezone(timezone)` → `testabilitySettings.getCurrentUTCTimestamp()` as the "now" upper bound. `@Transactional(readOnly = true)` so the lazy `RecallPrompt.answer` OneToOne loads within the transaction.
- **Reuse the existing query — no new query for retention:** `RecallPromptRepository.findAnsweredRecallPromptsInTimeRange(userId, start, end)` returns answered prompts ordered by answer time `[VERIFIED]`. **Each row already carries `answer.correct` and `answer.thinkingTimeMs`**, so the retention trend, retention-by-hour, weekday×hour retention heatmap, and best/worst-hour-by-retention are all computed from the **same** rows as the response-time trend and count heatmap — just additional aggregation in `RecallStatsService`. No new SQL, no new repository method.
- **Windows:** 365-day window (`now−365d … now`) for calendar, hourly breakdown, weekday×hour, retention trend, and 90-day response-time trend; all-time (or `now−5y` cap) for total reviews, retention headline, streak, total time. One call covers everything if cheap; two calls otherwise.
- **New service:** `RecallStatsService` (under `services/`) takes the `List<RecallPrompt>` + `ZoneId` + "now" and computes all buckets in Java (not SQL) — easy access to `answer.getCorrect()`, `answer.getThinkingTimeMs()`, and `ZoneId` conversion via `Timestamp.toInstant().atZone(zoneId)`. Per-day retention = `sum(correct)/count(answered)`; per-hour and per-weekday×hour retention likewise.
- **After backend changes:** regenerate the API client — `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`. Never hand-edit `packages/generated/**` `[VERIFIED: agent-map.md]`. Run `pnpm openapi:lint` after generating (unique operationId `getRecallStats`) `[CITED: .cursor/rules/linting_formating.mdc]`.

### Timezone handling (unchanged)

- Pass the browser IANA timezone via `timezoneParam()` from `@/managedApi/window/timezoneParam` as the `timezone` query param — identical to `getMenuData` `[VERIFIED: timezoneParam.ts uses Intl.DateTimeFormat().resolvedOptions().timeZone]`.
- **All day/hour/weekday grouping on the backend** using `TimezoneUtils.parseTimezone(timezone)` → `ZoneId`, converting each `quiz_answer.created_at` (UTC) to user-local before bucketing. One source of truth; matches `getMenuData` precedent.
- "Today"/streak boundaries use `testabilitySettings.getCurrentUTCTimestamp()` + user `ZoneId` (tests can freeze time) `[VERIFIED]`. Client never re-derives day/hour from raw timestamps; only renders pre-bucketed DTOs.

### Frontend wiring (updated)

- **New tab:** add `{ name: "settingsRecallStats", label: "Recall Stats" }` to `tabs[]` in `frontend/src/pages/SettingsPage.vue` `[VERIFIED]`.
- **New route child:** add to `settingsNestedRoute.children` in `frontend/src/routes/routes.ts` (`path: "recall-stats"`, name `settingsRecallStats`, component `RecallStatsSettingsTab`) `[VERIFIED]`.
- **New component:** `frontend/src/pages/settings/RecallStatsSettingsTab.vue` (mirror `GeneralSettingsTab.vue`): `onMounted` fetch via `UserController.getRecallStats({ query: { timezone: timezoneParam() } })` using `apiCallWithLoading` (read-only, **no** `blockUi` — keep page interactive; `ContentLoader` skeleton until data arrives) `[VERIFIED: frontend-api.mdc + GeneralSettingsTab.vue pattern]`.
- **Chart sub-components** under `frontend/src/components/recallStats/`: `RecallActivityCalendar.vue`, `ResponseTimeTrendChart.vue`, **`RetentionTrendChart.vue` (NEW)**, `WeekdayHourHeatmap.vue` (count + retention variants), `RecallStatsTiles.vue` (with the retention % headline prominent), `HourlyRetentionChart.vue`. Each pure-SVG, props-driven, testable in isolation.
- **Tests:** `frontend/tests/` with `mockSdkService()` + `makeMe` fixtures; add `RecallStatsSettingsTab.spec.ts` and per-chart specs asserting rendered SVG cells/points from a fixture DTO `[VERIFIED: agent-map.md frontend test pattern]`.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Vue 3 | 3.5.40 | UI framework | Already in repo `[VERIFIED: frontend/package.json]` |
| DaisyUI | 5.7.0 | Component theming | Already in repo; `base-*`/`primary`/`success` tokens for chart fills |
| Tailwind CSS | 4.3.3 | Styling | Already in repo |
| Spring Boot (Java) | existing | Backend API + aggregation | Already in repo; reuse `RecallPromptRepository` |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Inline SVG + CSS | — | Calendar / heatmaps / both trend lines | **Primary** — zero deps, simple shapes |
| `vue-content-loader` | 2.0.1 | Loading skeleton | Already in repo; use while stats load |
| `gsap` | ^3.15.0 | Optional chart entrance animation | Already in repo; only if polish desired |

**Installation:** No new packages required. (Chart-lib alternatives like `chart.js`/`unovis`/`apexcharts` are NOT recommended; if ever chosen, the planner MUST run `gsd-tools query package-legitimacy check --ecosystem npm <pkg>` and add `checkpoint:human-verify` first — all such candidates are `[ASSUMED]` and unverified this session.)

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Response-time measurement | A new "shown→answered" timer | `Answer.thinkingTimeMs` (pause-aware) | Already exists, tested, blur/visibility-aware `[VERIFIED]` |
| Answered-prompt query | A new SQL query | `RecallPromptRepository.findAnsweredRecallPromptsInTimeRange` | Already exists; each row carries `answer.correct` so retention is free `[VERIFIED]` |
| Retention numerator | A new query for correct counts | `answer.getCorrect()` on the same fetched rows | Same rows as response time; no extra DB hit `[VERIFIED]` |
| Auth + current user | Custom auth | `authorizationService.assertLoggedIn()` + `getCurrentUser()` | Existing pattern `[VERIFIED: UserController.getMenuData]` |
| Timezone parsing | Custom TZ handling | `TimezoneUtils.parseTimezone` + `ZoneId` | Existing; falls back to UTC `[VERIFIED]` |
| "Now" for testability | `new Date()` / `System.currentTimeMillis()` | `testabilitySettings.getCurrentUTCTimestamp()` | Tests can freeze time `[VERIFIED]` |
| Browser IANA timezone | Custom detection | `timezoneParam()` | Existing; uses `Intl` `[VERIFIED]` |
| API client | Hand-written fetch | `@generated/…/sdk.gen` + `apiCallWithLoading` | Existing; regenerate after DTO change `[VERIFIED: frontend-api.mdc]` |

**Key insight:** Everything around the statistics already exists in Doughnut — the response-time field, the answered-prompt query, the per-row `correct` flag, auth, timezone, testability clock, and the API-client pipeline. Net-new work is one aggregation service + DTO + one endpoint + one Settings tab + SVG charts (one extra line for retention). Do **not** reinvent the response-time metric; `thinkingTimeMs` is the right source. Do **not** add a query for retention — `answer.correct` is already on every fetched row.

## Common Pitfalls

### Pitfall 1: Treating retention as a secondary tile
**What goes wrong:** Retention % is buried in a corner; users miss the most important SR signal.
**Why:** It's easy to default retention to a small tile since the response-time trend was specified first.
**How to avoid:** Make the overall retention % a **prominent headline tile** and give retention its **own daily trend line chart** (parallel to the response-time trend). Anki gives retention three surfaces; Doughnut should give it at least the headline + trend + by-hour.
**Warning signs:** the tab reads as "response-time analytics with a retention afterthought."

### Pitfall 2: A 1/1 day shows 100% retention
**What goes wrong:** A day with a single correct answer shows a perfect 100% retention point, making the trend look great but meaningless.
**Why:** No minimum-sample guard on retention buckets.
**How to avoid:** Apply the **same ≥3-answered guard** as response time: render a day's retention % only if it has ≥3 answered reviews, else "insufficient data" (gap in the line).
**Warning signs:** jagged retention trend driven by low-sample days at 0% or 100%.

### Pitfall 3: Using wall-clock diff instead of `thinkingTimeMs`
**What goes wrong:** Response-time averages look huge and noisy (diff includes paused/away time).
**How to avoid:** `thinkingTimeMs` primary; diff only as a null-fallback with a 300s cap.
**Warning signs:** daily response-time averages swing 10×+ on days a tab was left open.

### Pitfall 4: Forgetting `@Transactional(readOnly = true)`
**What goes wrong:** `LazyInitializationException` reading `recallPrompt.getAnswer()…`.
**How to avoid:** Annotate the endpoint `@Transactional(readOnly = true)` (mirror `getMenuData`).
**Warning signs:** 500s only when accessing answer fields, in tests without a session.

### Pitfall 5: Client-side timezone re-bucketing
**What goes wrong:** Calendar/heatmap days shift by one vs. the user's view; "today" mismatch.
**How to avoid:** Bucket on the backend with the `timezone` param; send ISO date strings + pre-counted arrays.
**Warning signs:** "Reviews today" tile disagrees with the calendar's today cell.

### Pitfall 6: OpenAPI `operationId` / path collision
**What goes wrong:** `pnpm generateTypeScript` or lint fails on duplicate operationIds.
**How to avoid:** Distinct method name (`getRecallStats`) + unique `@Operation`; run `pnpm openapi:lint` after generating `[CITED: .cursor/rules/linting_formating.mdc]`.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `thinkingTimeMs` (not the timestamp diff) is the primary response-time metric — deviates from user's stated definition | Part 2, Metric definitions | User may specifically want wall-clock diff; confirm before locking. Low risk — `thinkingTimeMs` is strictly more accurate. |
| A2 | Sub-second (<1000ms) responses are misclicks to drop | Part 2, Outliers | Some legit fast recalls could be lost; threshold tunable. |
| A3 | 120s cap for `thinkingTimeMs`, 300s cap for diff fallback | Part 2, Outliers | May over/under-trim; tune after seeing real distribution. |
| A4 | Trimmed mean P5–P95 is the right response-time average | Part 2, Outliers | Median is a valid simpler alternative; user may prefer one. |
| A5 | Per-user 365-day answered-prompt row count is small enough for Java-side aggregation | Part 3, Backend | If a power user has 100k+ rows, consider SQL `GROUP BY` or a shorter window. |
| A6 | ≥3-answered guard is the right min-sample threshold for retention buckets (matches response-time guard) | Part 2, Retention metric | Could be too low (noisy) or too high (sparse); tune after first data. |
| A7 | Retention trend should use the same 30/90/all toggle as the response-time trend | Part 2, Retention trend | User may want a different default window; minor. |
| A8 | RemNote/Quizlet stats features as described | Part 1 | Lower confidence; not central to recommendations. |

## Open Questions

1. **Response-time metric choice (A1)** — `thinkingTimeMs` vs `answer.createdAt − recall_prompt.createdAt`. Recommendation: confirm with user; default to `thinkingTimeMs` (primary) + diff (null-fallback).
2. **All-time vs capped window for totals/streak (A5)** — performance of a multi-year fetch. Recommendation: start with a 5-year cap; revisit if slow.
3. **Average type for response time (A4)** — trimmed mean vs median. Recommendation: trimmed mean P5–P95; offer median if the team wants simpler.
4. **Retention min-sample guard (A6)** — ≥3 answered per bucket. Recommendation: match the response-time guard; revisit after first real data.

## Environment Availability

Step 2.6: minimal — this phase adds no external runtime dependency. All required tooling already exists in the repo/Nix shell.

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Spring Boot / Java | Backend endpoint + service | ✓ | existing (repo) | — |
| Vue 3 + DaisyUI + Tailwind | Frontend tab + SVG charts | ✓ | 3.5.40 / 5.7.0 / 4.3.3 `[VERIFIED]` | — |
| `pnpm generateTypeScript` | API client regen | ✓ | via Nix | — |
| MySQL | `recall_prompt`/`quiz_answer` reads | ✓ | `pnpm sut` assumed running | — |

**Missing dependencies with no fallback:** none. **Missing dependencies with fallback:** none.

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
| RS-03 | **Retention % = correct/answered per bucket with ≥3-answered guard** (daily trend, hourly, weekday×hour, best/worst hour) | unit (service) | `pnpm backend:test_only` (RecallStatsServiceTest) | ❌ Wave 0 |
| RS-04 | Day/hour/weekday bucketing respects the `timezone` param (UTC→user-local) | unit (service) | `pnpm backend:test_only` | ❌ Wave 0 |
| RS-05 | Streak computed over user-local consecutive days using testability clock | unit (service) | `pnpm backend:test_only` | ❌ Wave 0 |
| RS-06 | Settings "Recall Stats" tab renders calendar/heatmaps/both trends/tiles from a fixture DTO | unit (component) | `pnpm frontend:test tests/pages/settings/RecallStatsSettingsTab.spec.ts` | ❌ Wave 0 |
| RS-07 | SVG **retention trend** renders points (0–100% Y) with min-3 gaps from fixture | unit (component) | `pnpm frontend:test tests/components/recallStats/RetentionTrendChart.spec.ts` | ❌ Wave 0 |
| RS-08 | SVG calendar renders 365 cells + correct fill scale from fixture | unit (component) | `pnpm frontend:test tests/components/recallStats/RecallActivityCalendar.spec.ts` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `pnpm backend:test_only` (touched) + `pnpm frontend:test <touched spec>`
- **Per wave merge:** `pnpm backend:verify` + `pnpm frontend:test`
- **Phase gate:** Full touched suites green before `/gsd-verify-work`; do **not** run full E2E unless required.

### Wave 0 Gaps
- [ ] `backend/src/test/java/.../controllers/UserControllerRecallStatsTest.java` — covers RS-01
- [ ] `backend/src/test/java/.../services/RecallStatsServiceTest.java` — covers RS-02/03/04/05
- [ ] `frontend/tests/pages/settings/RecallStatsSettingsTab.spec.ts` — covers RS-06
- [ ] `frontend/tests/components/recallStats/RetentionTrendChart.spec.ts` — covers RS-07
- [ ] `frontend/tests/components/recallStats/RecallActivityCalendar.spec.ts` (+ heatmap/response-time-trend specs) — covers RS-08
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
| PII exposure (review timestamps + correctness) | Info disclosure | Endpoint is user-scoped (own data only); no cross-user data |

## Sources

### Primary (HIGH confidence)
- Anki Manual — Statistics: https://docs.ankiweb.net/stats.html (Calendar, **Hourly Breakdown success-rate region**, **Answer Buttons % correct**, **True Retention Table** (mature≥21d, first review/day), Review Time) `[CITED]`
- Anki `stats.md` (manual source): https://github.com/ankitects/anki-manual/blob/main/src/stats.md `[CITED]`
- SuperMemo — Forgetting index: https://www.supermemo.guru/wiki/Forgetting_index_in_SuperMemo , /wiki/Forgetting_index_FAQ (requested vs measured FI; FI≈10% ⇒ retention≈95%) `[CITED]`
- FSRS retention — Expertium: https://expertium.github.io/Retention.html (desired vs measured retention), /Algorithm.html (S = time for R to fall to 90%) `[CITED]`
- Mochi docs + changelog: https://mochi.cards/docs/cards , https://mochi.cards/changelog (remembered/forgotten per review, retention over time, review heatmap, line charts, total review time) `[CITED]`
- Codebase (verified by Read/Grep in the prior session, reused verbatim): `RecallPrompt.java`, `Answer.java` (`thinking_time_ms`, `@NotNull correct`), `ForgettingCurve.java` (`MAX_THINKING_TIME_MS=60000`), `RecallPromptRepository.java` (`findAnsweredRecallPromptsInTimeRange`), `UserController.java` (`getMenuData`), `RecallService.java`, `TimezoneUtils.java`, `MenuDataDTO.java`, `useThinkingTimeTracker.ts` (pause/blur/visibility-aware), `RecallSessionOptionsDialog.vue` (avg-thinking-time precedent), `SettingsPage.vue`, `routes.ts`, `frontend/package.json` (no chart lib), `timezoneParam.ts`, `.cursor/rules/{backend-code,frontend-api,linting_formating}.mdc`, `.planning/config.json`.

### Secondary (MEDIUM confidence)
- WebSearch syntheses for Anki/SuperMemo/Mochi/FSRS (cross-checked against the official pages above).

### Tertiary (LOW confidence)
- RemNote / Quizlet stats descriptions `[ASSUMED]` — not central to recommendations; flagged A8.

## Metadata

**Confidence breakdown:**
- Competitive survey (retention-first): HIGH — grounded in official docs for Anki/SuperMemo/Mochi/FSRS; retention-as-first-class is the consensus.
- Data model & implementation: HIGH — every codebase claim verified by reading the actual source in the prior session; retention reuses the same fetched rows (`answer.correct`), so no new query.
- Outlier/guard thresholds (A2/A3/A4/A6): LOW/MEDIUM — standard practice but unverified against Doughnut's real distribution; tune after first data.

**Research date:** 2026-07-23 (revised — retention-first)
**Valid until:** 2026-08-22 (30 days; stable stack, no fast-moving deps)

