---
phase: 260723-jlm-add-recall-statistics-for-user-a-new-tab
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - backend/src/main/java/com/odde/doughnut/controllers/UserController.java
  - backend/src/main/java/com/odde/doughnut/controllers/dto/RecallStatsDTO.java
  - backend/src/main/java/com/odde/doughnut/services/RecallStatsService.java
  - backend/src/test/java/com/odde/doughnut/controllers/UserControllerTest.java
  - backend/src/test/java/com/odde/doughnut/services/RecallStatsServiceTest.java
  - frontend/src/pages/SettingsPage.vue
  - frontend/src/pages/settings/RecallStatsSettingsTab.vue
  - frontend/src/components/recallStats/RecallActivityCalendar.vue
  - frontend/src/components/recallStats/ResponseTimeTrendChart.vue
  - frontend/src/components/recallStats/RetentionTrendChart.vue
  - frontend/src/components/recallStats/WeekdayHourHeatmap.vue
  - frontend/src/components/recallStats/RecallStatsTiles.vue
  - frontend/src/components/recallStats/AmPmResponseTimeChart.vue
  - frontend/src/routes/routes.ts
  - frontend/tests/pages/settings/RecallStatsSettingsTab.spec.ts
  - frontend/tests/components/recallStats/RecallActivityCalendar.spec.ts
  - frontend/tests/components/recallStats/WeekdayHourHeatmap.spec.ts
  - frontend/tests/components/recallStats/ResponseTimeTrendChart.spec.ts
  - frontend/tests/components/recallStats/RetentionTrendChart.spec.ts
  - packages/generated/doughnut-backend-api/types.gen.ts
  - packages/generated/doughnut-backend-api/sdk.gen.ts
  - open_api_docs.yaml
autonomous: true
requirements:
  - RS-01
  - RS-02
  - RS-03
  - RS-04
  - RS-05
  - RS-06
  - RS-07
  - RS-08
must_haves:
  truths:
    - "GET /api/user/recall-stats?timezone=... returns aggregated recall stats scoped to the logged-in user, and returns 401 when not logged in (per D-arch, getMenuData pattern)"
    - "Retention is the LEAD statistic: the DTO carries retentionTrend (daily correct/answered, last 90d), hourlyRetention, weekdayHourCorrect + weekdayHourCounts (for the retention heatmap), and totals.retentionPct365 as a prominent headline (per D-retention)"
    - "Retention per bucket = count(correct=true)/count(answered) with a >=3-answered guard (insufficient data otherwise); computed from the SAME fetched rows as response time via answer.getCorrect() — no new SQL query (per D-retention)"
    - "Response-time averages use Answer.thinkingTimeMs as primary metric with quiz_answer.created_at - recall_prompt.created_at fallback only when thinking_time_ms IS NULL (cap 300s); drop <1000ms, cap thinkingTimeMs >120000ms, trimmed mean P5-P95, >=3 samples per bucket else insufficient data (per D-metric, D-avg)"
    - "All day/hour/weekday bucketing happens on the backend using the user ZoneId from the timezone param; DTO sends ISO yyyy-MM-dd date strings and pre-counted arrays (per D-arch, D-tz)"
    - "Streak (current and longest) is computed over user-local consecutive days with >=1 answered review, using testabilitySettings.getCurrentUTCTimestamp() as now (per D-arch)"
    - "Settings page exposes a Recall Stats tab whose component fetches via the generated SDK UserController.getRecallStats with apiCallWithLoading and passes timezoneParam() (per D-arch, D-frontend)"
    - "Five hand-rolled SVG charts render from a fixture DTO: 365-cell calendar, daily response-time trend polyline, daily RETENTION trend polyline (0-100% Y), weekday x hour count heatmap, weekday x hour retention heatmap (per D-frontend, no chart dependency)"
  artifacts:
    - backend/src/main/java/com/odde/doughnut/services/RecallStatsService.java
    - backend/src/main/java/com/odde/doughnut/controllers/dto/RecallStatsDTO.java
    - frontend/src/pages/settings/RecallStatsSettingsTab.vue
    - frontend/src/components/recallStats/RecallActivityCalendar.vue
    - frontend/src/components/recallStats/ResponseTimeTrendChart.vue
    - frontend/src/components/recallStats/RetentionTrendChart.vue
    - frontend/src/components/recallStats/WeekdayHourHeatmap.vue
    - frontend/src/components/recallStats/RecallStatsTiles.vue
    - frontend/src/components/recallStats/AmPmResponseTimeChart.vue
  key_links:
    - "UserController.getRecallStats -> RecallStatsService -> RecallPromptRepository.findAnsweredRecallPromptsInTimeRange (read-only, @Transactional) — one fetch, retention + response time derived from the same rows"
    - "RecallStatsService bucketing -> TimestampOperations.getZonedDateTime(answer.createdAt, zoneId) for UTC->user-local conversion (used by BOTH retention and response-time buckets)"
    - "Backend RecallStatsDTO -> pnpm generateTypeScript -> generated SDK UserController.getRecallStats -> RecallStatsSettingsTab.vue fetch"
    - "SettingsPage.vue tabs[] + routes.ts settingsNestedRoute.children -> RecallStatsSettingsTab route"
---

<objective>
Add a read-only `GET /api/user/recall-stats?timezone=...` endpoint that aggregates the logged-in user's answered recall prompts into a single `RecallStatsDTO` where **correct answer rate (retention) is the lead, first-class statistic**: a daily retention trend (parallel to the daily response-time trend), an overall retention % headline, retention-by-hour, a weekday x hour retention heatmap, and best/worst review hour by retention — alongside the previously-locked response-time views (daily avg response-time trend, morning/afternoon, weekday x hour count heatmap, calendar, streak/totals tiles). Response-time averages use a trimmed mean P5-P95 after dropping sub-second misclicks and capping long values; retention buckets use the same >=3-sample guard. Then expose it in a new Settings -> "Recall Stats" tab rendered with hand-rolled SVG/CSS charts (no new chart dependency).

Purpose: Give the user insight into their recall accuracy and habits (Anki/Mochi-style stats, retention-first) from data Doughnut already captures — `answer.correct` and `answer.thinkingTimeMs` are already on every fetched answered-prompt row — without a database migration, a new SQL query, or a new frontend chart library.
Output: `RecallStatsService` + `RecallStatsDTO` (retention-first shape) + `UserController.getRecallStats` + backend tests; regenerated API client; `RecallStatsSettingsTab.vue` + five SVG chart sub-components (incl. the new `RetentionTrendChart`) + AM/PM bars + tiles + `SettingsPage.vue`/`routes.ts` wiring + frontend tests.
</objective>

<execution_context>
@$HOME/.cursor/gsd-core/workflows/execute-plan.md
@$HOME/.cursor/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/STATE.md
@.planning/quick/260723-jlm-add-recall-statistics-for-user-a-new-tab/260723-jlm-CONTEXT.md
@.planning/quick/260723-jlm-add-recall-statistics-for-user-a-new-tab/260723-jlm-RESEARCH.md
@.cursor/agent-map.md
@.cursor/rules/backend-code.mdc
@.cursor/rules/backend-testing.mdc
@.cursor/rules/frontend-api.mdc
@.cursor/rules/frontend-testing.mdc
@.cursor/rules/linting_formating.mdc
@backend/src/main/java/com/odde/doughnut/controllers/UserController.java
@backend/src/main/java/com/odde/doughnut/controllers/dto/MenuDataDTO.java
@backend/src/main/java/com/odde/doughnut/entities/repositories/RecallPromptRepository.java
@backend/src/main/java/com/odde/doughnut/entities/RecallPrompt.java
@backend/src/main/java/com/odde/doughnut/entities/Answer.java
@backend/src/main/java/com/odde/doughnut/utils/TimezoneUtils.java
@backend/src/main/java/com/odde/doughnut/utils/TimestampOperations.java
@backend/src/test/java/com/odde/doughnut/controllers/UserControllerTest.java
@backend/src/test/java/com/odde/doughnut/controllers/ControllerTestBase.java
@backend/src/test/java/com/odde/doughnut/testability/builders/RecallPromptBuilder.java
@frontend/src/pages/SettingsPage.vue
@frontend/src/pages/settings/GeneralSettingsTab.vue
@frontend/src/pages/settings/AccessTokensSettingsTab.vue
@frontend/src/routes/routes.ts
@frontend/src/managedApi/window/timezoneParam.ts
@frontend/tests/pages/settings/AccessTokensSettingsTab.spec.ts
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Backend recall-stats aggregation service, retention-first DTO, and read-only endpoint (per D-arch, D-metric, D-avg, D-tz, D-retention)</name>
  <files>backend/src/main/java/com/odde/doughnut/services/RecallStatsService.java, backend/src/main/java/com/odde/doughnut/controllers/dto/RecallStatsDTO.java, backend/src/main/java/com/odde/doughnut/controllers/UserController.java, backend/src/test/java/com/odde/doughnut/services/RecallStatsServiceTest.java, backend/src/test/java/com/odde/doughnut/controllers/UserControllerTest.java</files>
  <behavior>
    - RecallStatsServiceTest (pure algorithm, @SpringBootTest @ActiveProfiles("test") @Transactional, @Autowired MakeMe): trimmed-mean P5-P95 drops <1000ms, caps thinkingTimeMs >120000ms and diff-fallback >300000ms, returns null/insufficient for buckets with <3 valid samples; uses thinkingTimeMs when non-null, falls back to (answer.createdAt - recallPrompt.createdAt) only when thinkingTimeMs is null; bucketing via TimestampOperations.getZonedDateTime(answer.createdAt, zoneId) respects the timezone param (a Shanghai 10am answer and a UTC 10am answer land in different local-day buckets).
    - RecallStatsServiceTest retention: per-day retention = correct/answered with a >=3-answered guard (a day with 1 correct / 1 answered is insufficient, NOT 100%); per-hour retention over 365d; weekday x hour correct/answered; overall retentionPct365 = totalCorrect/totalAnswered over the window; best/worst hour by retention ranks hours with >=N (use N=5) answered reviews, best = highest retention, worst = lowest; retention is computed from the same fetched rows as response time (no second repository call).
    - RecallStatsServiceTest streak: streak counts consecutive user-local days with >=1 answered review up to "now" using testabilitySettings.getCurrentUTCTimestamp(); current = run ending at today's local date; longest = max run.
    - UserControllerTest @Nested GetRecallStats: getRecallStats("Asia/Shanghai") returns stats scoped to currentUser (another user's answered prompts are excluded); returns 401 (ResponseStatusException) when currentUser is null (mirror GetMenuData.forLoginUserOnly); calendar has 365 entries 0-filled; reviewsToday uses the testability clock + user ZoneId "today" boundary; retentionTrend has up to 90 entries and respects the >=3 guard; totals.retentionPct365 is present.
  </behavior>
  <action>Create RecallStatsDTO in controllers/dto mirroring MenuDataDTO Lombok style (@Data @AllArgsConstructor, nested records/@Data classes). RETENTION-FIRST shape: calendar (List of {String date "yyyy-MM-dd", int count}, 365 entries 0-filled trailing days); trend (List of {String date, Long avgMs, Integer sampleSize} for last 90d, avgMs null when insufficient — response time); retentionTrend (List of {String date, Double retentionPct, Integer correctCount, Integer answeredCount, Integer sampleSize} for last 90d, retentionPct null when answeredCount < 3 — NEW retention trend); amPm ({Long morningMs, Integer morningSamples, Long afternoonMs, Integer afternoonSamples, Long eveningMs, Integer eveningSamples, Long nightMs, Integer nightSamples} with AM=[06,12), PM=[12,18), evening=[18,24), night=[00,06)); weekdayHourCounts (int[7][24] review counts — denominators); weekdayHourCorrect (int[7][24] correct counts — retention % = correct/counts per cell); hourlyRetention (List of {Integer hour 0..23, Double retentionPct, Integer correctCount, Integer answeredCount} — retention by hour, feeds best/worst hour list); totals ({int totalReviewsAllTime, int totalReviews365, int reviewsToday, Double retentionPct365, int currentStreak, int longestStreak, long totalTimeSpentMs, Integer bestHour, Double bestHourRetentionPct, Integer worstHour, Double worstHourRetentionPct}). Send dates as ISO strings; weekday index 0=Monday..6=Sunday (DayOfWeek.getValue()-1). Create RecallStatsService (@Service) injecting RecallPromptRepository; constructor-inject like RecallService. Compute "now" from a passed-in Timestamp (do not call System.currentTimeMillis inside the service — the controller passes testabilitySettings.getCurrentUTCTimestamp() so tests can freeze time). Reuse recallPromptRepository.findAnsweredRecallPromptsInTimeRange(userId, start, end) — ONE 365-day call (now-365d .. now) for calendar/trend/retentionTrend/amPm/weekdayHour/hourlyRetention/retention365/reviewsToday, and ONE all-time call (epoch .. now, or now-5y cap) for totalReviewsAllTime/longestStreak/totalTimeSpent per D-A5 (5-year cap default). For each answered RecallPrompt read answer=getAnswer() (lazy OneToOne — @Transactional(readOnly=true) keeps the session open), correct=answer.getCorrect(), primary=answer.getThinkingTimeMs(); fallback=(answer.getCreatedAt().getTime() - recallPrompt.getCreatedAt().getTime()) only when thinkingTimeMs is null, capped at 300000ms; drop <1000ms; cap primary >120000ms to 120000. Bucket each answer via TimestampOperations.getZonedDateTime(answer.getCreatedAt(), zoneId) -> localDate (calendar/trend/retentionTrend/streak) and dayOfWeek.getValue()-1 (weekday, Mon=1->0) and getHour (0..23). RETENTION aggregation (same rows): per-day correctCount/answeredCount; per-hour correctCount/answeredCount (sum across weekdays); weekdayHourCorrect[wd][hr]++; weekdayHourCounts[wd][hr]++; overall retentionPct365 = totalCorrect/totalAnswered. Trimmed mean P5-P95 per bucket for response time: sort valid values, drop bottom 5% and top 5% (floor/ceil index), mean the rest; require >=3 valid samples else avgMs=null. Retention guard: require >=3 answered per bucket else retentionPct=null (insufficient). Streak: distinct local dates with >=1 answered review sorted; current = consecutive run ending at today's local date (today boundary from now+zoneId); longest = max run. Best/worst hour: from hourlyRetention, rank hours with >=N (use N=5) answered reviews; best=top retention, worst=lowest. Add UserController.getRecallStats(@RequestParam("timezone") String timezone) annotated @GetMapping("/recall-stats") @Transactional(readOnly = true), following getMenuData exactly: authorizationService.assertLoggedIn() -> getCurrentUser() -> TimezoneUtils.parseTimezone(timezone) -> testabilitySettings.getCurrentUTCTimestamp() -> recallStatsService.compute(user, zoneId, now) -> return RecallStatsDTO. Give the method a unique name (getRecallStats) to avoid OpenAPI operationId collision (Pitfall 5). Use top-of-file imports (no inline fully-qualified names) per backend-code.mdc. Do NOT add a userId request param (IDOR guard). Do NOT write a new SQL query — reuse the existing repository method; retention reuses the same fetched rows.</action>
  <verify>
    <automated>CURSOR_DEV=true nix develop -c pnpm backend:test_only</automated>
  </verify>
  <done>RecallStatsServiceTest passes (trimmed-mean outlier rules, timezone bucketing, streak, retention trend + retention guard + best/worst hour by retention, retention reuses same rows) and UserControllerTest.GetRecallStats passes (user-scoped, 401 when logged out, 365 0-filled calendar entries, retentionTrend present with >=3 guard, reviewsToday via testability clock). Backend compiles and `pnpm backend:test_only` is green.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Regenerate API client, then add Settings Recall Stats tab with five hand-rolled SVG charts (retention trend first-class) + tiles (per D-arch, D-frontend, D-retention)</name>
  <files>packages/generated/doughnut-backend-api/types.gen.ts, packages/generated/doughnut-backend-api/sdk.gen.ts, open_api_docs.yaml, frontend/src/pages/SettingsPage.vue, frontend/src/routes/routes.ts, frontend/src/pages/settings/RecallStatsSettingsTab.vue, frontend/src/components/recallStats/RecallActivityCalendar.vue, frontend/src/components/recallStats/ResponseTimeTrendChart.vue, frontend/src/components/recallStats/RetentionTrendChart.vue, frontend/src/components/recallStats/WeekdayHourHeatmap.vue, frontend/src/components/recallStats/RecallStatsTiles.vue, frontend/src/components/recallStats/AmPmResponseTimeChart.vue, frontend/tests/pages/settings/RecallStatsSettingsTab.spec.ts, frontend/tests/components/recallStats/RecallActivityCalendar.spec.ts, frontend/tests/components/recallStats/WeekdayHourHeatmap.spec.ts, frontend/tests/components/recallStats/ResponseTimeTrendChart.spec.ts, frontend/tests/components/recallStats/RetentionTrendChart.spec.ts</files>
  <behavior>
    - RecallStatsSettingsTab.spec.ts: mocks UserController.getRecallStats with a fixture RecallStatsDTO (mockSdkService), mounts via helper.component(RecallStatsSettingsTab).withRouter(), and after flushPromises asserts the prominent retention % headline tile renders totals.retentionPct365, the other headline tiles render totalReviews365/reviewsToday/currentStreak, and the five chart components + AM/PM bars + best/worst-hour list are present (data-testid or text). Asserts the fetch is called with { query: { timezone: timezoneParam() } }.
    - RecallActivityCalendar.spec.ts: given a fixture with 365 day-counts (including a max-count day and zero days), mounts and asserts 365 SVG <rect> cells render and the max-count day gets the darkest fill class while zero days get the empty class.
    - WeekdayHourHeatmap.spec.ts: given a fixture 7x24 counts array with a known peak (mode="count") asserts 168 cells render and the peak cell has the darkest fill class; given a fixture 7x24 correct/answered with a known high-retention cell (mode="retention") asserts that cell gets the greenest fill and an insufficient (answered<3) cell is greyed.
    - ResponseTimeTrendChart.spec.ts: given a fixture trend with a 30-day window, asserts a <polyline> renders with one point per sufficient-data day and insufficient-data days are omitted/greyed.
    - RetentionTrendChart.spec.ts: given a fixture retentionTrend with a 30-day window (some days >=3 answered, some <3 insufficient), asserts a <polyline> renders with one point per sufficient-data day on a 0-100% Y-axis and insufficient-data days are omitted/greyed (NOT shown as 0% or 100%).
  </behavior>
  <action>STEP 1 (regen, prerequisite): run `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` to regenerate open_api_docs.yaml and packages/generated/doughnut-backend-api/** from the new controller method + retention-first DTO, then `CURSOR_DEV=true nix develop -c pnpm openapi:lint` to confirm no duplicate operationId/path collision (Pitfall 5). Never hand-edit packages/generated/** or open_api_docs.yaml. STEP 2 (wiring): add `{ name: "settingsRecallStats", label: "Recall Stats" }` to the tabs[] array in frontend/src/pages/SettingsPage.vue; add the import `import RecallStatsSettingsTab from "@/pages/settings/RecallStatsSettingsTab.vue"` and a child `{ path: "recall-stats", name: "settingsRecallStats", component: RecallStatsSettingsTab }` to settingsNestedRoute.children in frontend/src/routes/routes.ts (mirror the AccessTokens entry). STEP 3 (tab component): create frontend/src/pages/settings/RecallStatsSettingsTab.vue mirroring GeneralSettingsTab.vue — `onMounted` fetch via `const { data, error } = await apiCallWithLoading(() => UserController.getRecallStats({ query: { timezone: timezoneParam() } }))` (read-only, NO blockUi — keep page interactive; show ContentLoader skeleton until data arrives), import timezoneParam from "@/managedApi/window/timezoneParam", store the RecallStatsDTO in a ref, check `!error` before using data (per frontend-api.mdc). Render RecallStatsTiles FIRST (retention % headline prominent at the top), then RecallActivityCalendar, then ResponseTimeTrendChart and RetentionTrendChart side by side (retention trend uses a success/green token, 0-100% Y-axis; response-time trend uses primary token, seconds Y-axis), then WeekdayHourHeatmap(mode="count") and WeekdayHourHeatmap(mode="retention") side by side, then AmPmResponseTimeChart, passing the relevant DTO slices as props. Add a 30/90/all toggle (local ref, default 90) that re-derives the visible trend + retentionTrend slice client-side from the 90-day arrays the backend returns (for "all" show the full 90 since the backend window is 90d — keep the toggle UI but note the backend window is 90d). STEP 4 (SVG chart sub-components, no chart dependency): create frontend/src/components/recallStats/RecallActivityCalendar.vue (53-week x 7-day SVG <rect> grid, 5-step fill scale via Tailwind/DaisyUI classes keyed off count, <title> tooltip = date + count); ResponseTimeTrendChart.vue (one <polyline> over sufficient-data days, y-axis in seconds, x-axis date labels, insufficient-data days rendered as a gap/grey marker); RetentionTrendChart.vue (NEW — one <polyline> over sufficient-data days, y-axis 0-100%, success/green token, x-axis date labels, insufficient-data days (answeredCount<3) rendered as a gap/grey marker, NEVER as 0% or 100%); WeekdayHourHeatmap.vue (7x24 <rect> grid with a `mode: "count" | "retention"` prop — count mode fills by count, retention mode fills red->green by correct/answered and greys cells with answered<3; <title> tooltip); RecallStatsTiles.vue (headline tiles: PROMINENT retention % 365 with answered-count denominator small underneath, total reviews all-time/365/today, current & longest streak, total time spent formatted h/m, reviews today; plus best/worst review hour by retention list from totals.bestHour/worstHour + retention %); AmPmResponseTimeChart.vue (minor 4-bar SVG: morning/afternoon/evening/night avg response time, props from amPm). All chart components are pure-SVG, props-driven, no fetches. Use data-testid on key elements for tests. Follow frontend-api.mdc (destructure { data, error }, check !error before using data, no runtime property checks on typed props) and frontend-testing.mdc (mockSdkService, helper.component(...).withRouter().mount(), flushPromises, avoid role queries). Run `CURSOR_DEV=true nix develop -c pnpm format:all` after writing the Vue files. Do NOT add any chart library to frontend/package.json.</action>
  <verify>
    <automated>CURSOR_DEV=true nix develop -c pnpm openapi:lint &amp;&amp; CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/settings/RecallStatsSettingsTab.spec.ts tests/components/recallStats/RecallActivityCalendar.spec.ts tests/components/recallStats/WeekdayHourHeatmap.spec.ts tests/components/recallStats/ResponseTimeTrendChart.spec.ts tests/components/recallStats/RetentionTrendChart.spec.ts</automated>
  </verify>
  <done>API client regenerated (UserController.getRecallStats + retention-first RecallStatsDTO present in sdk.gen.ts/types.gen.ts) and openapi:lint clean; Settings page shows a "Recall Stats" tab; RecallStatsSettingsTab fetches via the generated SDK with timezoneParam() and renders the prominent retention % headline + tiles + 365-cell calendar + response-time trend polyline + RETENTION trend polyline + weekday x hour count heatmap + weekday x hour retention heatmap + AM/PM bars + best/worst hour list from a fixture; all five frontend specs green; no new chart dependency added to frontend/package.json.</done>
</task>

</tasks>
<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| client -> API | Untrusted `timezone` query param crosses into `GET /api/user/recall-stats` |
| API -> DB | Read-only scoped reads of `recall_prompt`/`quiz_answer` via `currentUser().getId()` |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-rs-01 | Info disclosure / Elevation (IDOR) | UserController.getRecallStats | high | mitigate | Server forces `userId = currentUser().getId()`; never accept a `userId` from the client (per D-arch getMenuData pattern). Repo query already filters `mt.user_id = :userId`. |
| T-rs-02 | Tampering / DoS (timezone injection) | TimezoneUtils.parseTimezone(timezone) | medium | mitigate | `ZoneId.of` is wrapped in try/catch with UTC fallback (verified TimezoneUtils); reject any client-supplied user id; cap windows at 365d / 5y to bound row volume. |
| T-rs-03 | DoS (large result-set exhaustion) | RecallStatsService aggregation | medium | mitigate | Java-side aggregation over bounded windows (365d / 5y); retention reuses the same fetched rows as response time (no second query); per-user answered-prompt row count is modest (A5). |
| T-rs-04 | Info disclosure (PII - review timestamps + correctness) | RecallStatsDTO response | low | accept | Endpoint is user-scoped (own data only); no cross-user data is returned. |
| T-rs-SC | Tampering (supply chain) | npm/pip/cargo installs | high | mitigate | No package install in this plan (hand-rolled SVG, no chart lib). If a chart lib is later chosen, run the Package Legitimacy Gate and add a blocking human checkpoint before install. |
</threat_model>

<verification>
- Backend: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` green (RecallStatsServiceTest incl. retention trend + retention guard + best/worst hour by retention + UserControllerTest.GetRecallStats).
- OpenAPI: `CURSOR_DEV=true nix develop -c pnpm openapi:lint` clean (no duplicate operationId for getRecallStats; retention-first DTO fields present in generated types).
- Frontend: the five touched specs green via `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/settings/RecallStatsSettingsTab.spec.ts tests/components/recallStats/*.spec.ts`.
- Do NOT run the full E2E suite; run targeted specs only.
- Manual (end-of-phase human verify, optional): open Settings -> Recall Stats in a running dev app and confirm the prominent retention % headline, calendar, both trend lines (response-time + retention), both heatmaps (count + retention), AM/PM bars, and best/worst-hour list render for a user with review history.
</verification>

<success_criteria>
- `GET /api/user/recall-stats?timezone=Asia/Shanghai` returns a retention-first `RecallStatsDTO` scoped to the logged-in user; 401 when logged out.
- Retention is the lead statistic: `retentionTrend` (daily, last 90d, >=3-answered guard), `hourlyRetention`, `weekdayHourCorrect`+`weekdayHourCounts` (retention heatmap), `totals.retentionPct365` (prominent headline), and `totals.bestHour`/`worstHour` + retention % are all present.
- Retention per bucket = `count(correct=true)/count(answered)` with a >=3-answered guard (insufficient data otherwise); computed from the SAME fetched rows as response time — no new SQL query.
- Response-time averages use `thinkingTimeMs` (diff fallback only when null), drop <1000ms, cap long values, trimmed mean P5-P95, >=3-sample guard with "insufficient data" otherwise.
- Day/hour/weekday bucketing uses the `timezone` param on the backend; DTO carries ISO date strings + pre-counted arrays.
- Streak (current + longest) computed over user-local consecutive days using the testability clock.
- Settings exposes a "Recall Stats" tab that fetches via the generated SDK with `apiCallWithLoading` + `timezoneParam()` and renders the prominent retention % headline + tiles + five hand-rolled SVG charts (calendar, response-time trend, RETENTION trend, weekday x hour count heatmap, weekday x hour retention heatmap) + AM/PM bars + best/worst hour by retention list.
- No new chart dependency; no DB migration; no new SQL query; generated API client regenerated, not hand-edited.
- All targeted backend and frontend tests green.
</success_criteria>

<output>
Create `.planning/quick/260723-jlm-add-recall-statistics-for-user-a-new-tab/260723-jlm-SUMMARY.md` when done.
</output>



