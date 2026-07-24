---
status: complete
plan: 260723-jlm-add-recall-statistics-for-user-a-new-tab
---

# SUMMARY — Recall Statistics settings tab

## What was delivered

A new **Recall Stats** settings tab that surfaces a **retention-first** view of a
user's recall activity, backed by a read-only aggregation endpoint.

### Backend (Task 1)
- `GET /api/user/recall-stats?timezone=...` on `UserController`, following the
  `getMenuData` pattern (assert logged in, user-scoped, testability clock,
  `@Transactional(readOnly = true)`).
- `RecallStatsService` aggregates answered recall prompts into a
  `RecallStatsDTO` (retention-first):
  - **Retention is the lead statistic**: daily retention trend (last 90d) with a
    `>=3 answered` guard, overall `retentionPct365` headline, and best/worst
    review hour by retention.
  - Response-time averages use `thinkingTimeMs` (diff fallback when null), drop
    `<1s` misclicks, cap long thinking/diff, **trimmed mean P5–P95**, `>=3` sample
    guard.
  - Calendar (365d), response-time trend (90d), weekday×hour counts/correct,
    hourly retention, streaks (current + longest), total time spent.
- `RecallStatsDTO` mirrors `MenuDataDTO`'s Lombok style; aggregation is split
  into a repo-fetching `compute` and a pure static `aggregate` for unit testing.

### Frontend (Task 2)
- `RecallStatsSettingsTab` fetches via `apiCallWithLoading` (read-only, no
  `blockUi`) with the browser `timezoneParam()`.
- **Retention % headline tile is prominent** (with answered denominator),
  followed by total reviews / today / streaks / time-spent / best-worst hour tiles.
- Hand-rolled **SVG charts** (no new chart dependency):
  - `RecallActivityCalendar` — 365-cell 5-step fill scale.
  - `ResponseTimeTrendChart` — polyline; insufficient-data days greyed.
  - `RetentionTrendChart` — fixed 0–100% Y-axis; insufficient days greyed
    (never shown as 0% or 100%).
  - `WeekdayHourHeatmap` — `count` + `retention` modes (168 cells); retention
    greys cells with `<3` answered.
  - `AmPmResponseTimeChart` — 4 bars.
- 30/90/all trend window toggle (client-side slice of the 90-day arrays).
- Wired into `SettingsPage` tabs and `routes.ts` (`settingsRecallStats`).

## Commits
- `dac57e127c` — feat(recall-stats): backend aggregation service, retention-first
  DTO, read-only endpoint (+ regenerated `open_api_docs.yaml` and frontend API
  client; unique operationId `getRecallStats`).
- `41c7945d42` — feat(recall-stats): Recall Stats settings tab + retention-first
  SVG charts + specs.

## Verification
- Backend: `pnpm backend:test_only` green (full suite, incl.
  `RobotsTests.openApiDocsMatchCommittedYaml`).
- Targeted: `RecallStatsServiceTest` + `UserControllerTest.GetRecallStats`.
- Frontend: 5 specs (6 tests) green — tab + 4 chart component specs.
- `pnpm openapi:lint` clean; `pnpm frontend:format` (incl. `vue-tsc --noEmit`)
  clean.

## Deviations from plan
- API client regeneration was pulled forward into Task 1's commit (instead of
  Task 2) because the backend `RobotsTests.openApiDocsMatchCommittedYaml` test
  validates the committed `open_api_docs.yaml` against the new controller and
  must be green at Task 1's `backend:test_only` gate. Task 2's regeneration step
  was a no-op (confirmed clean).

## Out of scope (deferred)
- E2E coverage for the new tab (not in this plan's scope).
- Bigger retention windows beyond 90d (backend window is 90d; the "all" toggle
  shows the full 90).
