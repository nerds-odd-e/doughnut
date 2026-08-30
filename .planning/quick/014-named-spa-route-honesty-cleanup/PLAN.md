# Named SPA route honesty cleanup

**Status:** shipped (2026-08-30). Plan retired — Given-shaped later jumps use named `push` in E2E helpers.
**Type:** ad-hoc plan (`.planning/quick/`)
**Depends on:** shipped 011 (named visit gate, `namedLocationHref`, Proposed [ADR 0005](../../../docs/adrs/0005-web-routes.md) E2E table); shipped 015 (identity jumps, catalog page re-enter, assimilate menu Model A)

## Protocol (after first SPA load)

| Need | Mechanism |
|------|-----------|
| Logged-in Given shortcut to bazaar / settings / circles / admin tab | Named `push` (`navigateToBazaar`, `visitManageAccessTokensPage`, `visitRecallStatsPage`, `navigateToCircle` list/show, post-join `circleShow`, `visitAdminDashboardTab` with `{ tab }` query) |
| First SPA load, inbound URL, or **explicit remount** | `visitNamed` |

**Stay `visitNamed`:** `loginAs` → notebooks (first load); identify; invitation `circleJoin`; `visitRecallPage` (KeepAlive); epub `leaveEpubReadingViewAndReturn`; `visitHomePage` (`feature_toggle.feature` only). Do not convert `When I visit recall` to sidebar UI. Human owns ADR 0005 accept.

Bazaar proving observable: `__donutSpaDocumentMarker` on `window` inside `navigateToBazaar` after login (no Gherkin about `window`). Logged-out bazaar still `push` → `visitNamed` fallback.

## Speed gate

Median of 3 Cypress mocha `stats.duration` scores. Pass if after ≤ before + max(15% of before, 3s). Raw JSON was local-only (`/tmp/route-honesty-profile/`).

| Spec | Before | After | Gate | Result |
|------|--------|-------|------|--------|
| bazaar_subscription | 4.835s | 4.145s | ≤ 7.835s | pass |
| user_access_token | 2.568s | 2.013s | ≤ 5.568s | pass |
| daily_probe (recall stats helper; `recall_stats.feature` is `@wip`) | 16.339s | 15.263s | ≤ 19.339s | pass |
| notebooks_in_circles | 7.274s | 5.726s | ≤ 10.274s | pass |
| manage_bazaar | 1.454s | 1.203s | ≤ 4.454s | pass |
