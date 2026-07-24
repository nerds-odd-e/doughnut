# Quick Task 260723-jlm: Recall Statistics Tab - Context

**Gathered:** 2026-07-23
**Status:** Ready for planning (research complete; decisions locked)

<domain>
## Task Boundary

Add a new "Recall Stats" tab under Settings showing recall statistics for the user, with a reasonable averaging algorithm that removes too-short and too-long outliers.

</domain>

<decisions>
## Implementation Decisions (locked — do not revisit)

### Response-time metric
- **PRIMARY:** `Answer.thinkingTimeMs` (`quiz_answer.thinking_time_ms`) — already captured client-side, pause/blur/visibility-aware, clamped to 60s in `ForgettingCurve`. Strictly better than wall-clock diff (excludes "walked away" time).
- **FALLBACK:** `quiz_answer.created_at − recall_prompt.created_at` (ms), used ONLY when `thinking_time_ms IS NULL` (legacy rows). Cap diff at 300s.

### Scope (one tab) — retention is the LEAD, first-class statistic
- **Daily correct-answer-rate (retention) trend** — line chart, one point per day, 0–100% Y-axis, last 90d default + 30/90/all toggle (parallel to the response-time trend). FIRST-CLASS view.
- **Prominent overall retention % headline** — `count(correct)/count(answered)` over the window, shown as a leading headline tile.
- Calendar (GitHub-style, 365-day daily review counts) — *requested*
- Daily average response-time trend (line, last 90d default + 30/90/all toggle) — *requested*
- Morning vs afternoon response-time comparison (AM [06,12), PM [12,18); evening/night as extra bars if cheap) — *requested*
- Weekday×hour heatmap of review counts (7×24) — *requested*; PLUS a weekday×hour RETENTION heatmap (correct/answered per cell) reframed as a retention view
- Headline tiles: Total reviews (all-time / last-365 / today), **Retention % (last-365) [prominent]**, Current streak & Longest streak, Total time spent, Reviews today
- Best/worst review hour by retention (top-3 / bottom-3, min-N-reviews guard) — reframed as a retention view

### Retention metric + guard (locked)
- Retention = `count(quiz_answer.correct = true) / count(answered)` per bucket. `Answer.correct` is `@NotNull` non-null (verified).
- Minimum-sample guard: show a bucket's retention % only if it has `≥ 3` answered reviews; else "insufficient data" (matches the response-time guard).

### Averaging algorithm (response-time averages)
1. Drop too-short: discard `< 1000 ms` (misclicks).
2. Cap too-long (winsorize): clamp `thinkingTimeMs > 120000 ms` to 120s; clamp diff-fallback `> 300000 ms` to 300s.
3. Average = trimmed mean P5–P95 per bucket (drop bottom 5% and top 5% of remaining values).
4. Minimum-sample guard: render a bucket only if it has `≥ 3` valid responses; else "insufficient data".

### Architecture (from research)
- Backend: one read-only `GET /api/user/recall-stats?timezone=…` endpoint on `UserController`, following the `getMenuData` pattern (`assertLoggedIn` → `getCurrentUser` → `TimezoneUtils.parseTimezone` → `testabilitySettings.getCurrentUTCTimestamp()`), `@Transactional(readOnly = true)`.
- Reuse `RecallPromptRepository.findAnsweredRecallPromptsInTimeRange(userId, start, end)`. New `RecallStatsService` + `RecallStatsDTO` (aggregation in Java, not SQL).
- All day/hour/weekday bucketing on the backend using the user's `ZoneId`; send ISO date strings + pre-counted arrays.
- Frontend: new `settingsRecallStats` tab (SettingsPage.vue `tabs[]` + routes.ts child + `RecallStatsSettingsTab.vue`), fetch via generated SDK + `apiCallWithLoading`, pass `timezoneParam()`. Hand-rolled SVG/CSS charts (NO new chart dependency — repo has none; shapes are simple).
- After backend DTO changes: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`. Never hand-edit `packages/generated/**`.

### Claude's Discretion
- Exact DTO field names / nesting (mirror `MenuDataDTO` Lombok style).
- Calendar color-scale steps and SVG structure.
- 365-day vs YTD calendar window (recommend 365 trailing days).
- All-time vs 5-year cap for totals/streak (recommend 5-year cap; revisit if slow).

</decisions>

<specifics>
## Specific Ideas

- Open Question A5 (all-time vs capped window): default to 5-year cap for totals/streak.
- Defer (out of scope): Future-Due forecast, card-counts by maturity band, ease distribution, FSRS stability/difficulty.

</specifics>

<canonical_refs>
## Canonical References

- `.planning/quick/260723-jlm-add-recall-statistics-for-user-a-new-tab/260723-jlm-RESEARCH.md` (competitive survey + verified codebase findings + metric definitions + pitfalls)
- `.cursor/rules/backend-code.mdc`, `.cursor/rules/frontend-api.mdc`, `.cursor/rules/linting_formating.mdc`
- `.cursor/agent-map.md` (generated API + commands)

</canonical_refs>
