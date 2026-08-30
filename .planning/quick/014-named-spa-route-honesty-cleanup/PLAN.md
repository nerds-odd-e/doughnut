# Named SPA route honesty cleanup

**Status:** in progress (slices 1–3 and 6 done; 4–5 remaining).
**Type:** ad-hoc plan (`.planning/quick/`)
**Depends on:** shipped `.planning/quick/011-named-spa-route-honesty-follow-up/` (PLAN retired; named visit gate, `namedLocationHref`, Proposed [ADR 0005](../../../docs/adrs/0005-web-routes.md) E2E table remain on `main`); shipped `.planning/quick/015-spa-hydrate-after-testability-inject/` (PLAN retired 2026-08-29)

## Goal

Close leftovers from 011: **dead E2E** and the ADR table’s **later-jump = named `push`** (slice 10 still `visitNamed`s most Given shortcuts). Operator STATE no longer points at the retired 011 PLAN (spent-plan cleanup 2026-08-29). Do not reopen unit-wiki HTML pinning, recall remount, or ADR accept.

**015 already shipped** (do not redo): Given-shaped note/notebook identity jumps; catalog listing **page re-enter** (`leaveNotebookCatalogIfAlreadyOpen` then `push('notebooks')`); assimilate menu Model A (no `route.name` → `getMenuData`). 015 left circles / admin `visitNamed` for this plan. Bazaar and settings Given shortcuts now `push` after login (slices 2–3). Named `push` after login was **not slower** on 015’s timer specs or slices 2–3.

## Inspection (011 on `main`)

Scope: 011’s route-honesty commits (unit leftovers + E2E gate + ADR rewrite), not later probe work. Bar: Proposed ADR 0005 E2E intent table, `unit-testing.mdc` focused assertions, post-change-refactor dead/redundant/duplication.

### Sliced (meaningful)

1. **Dead E2E navigation leftovers.** Done in slice 1. Invitation join stays inject + `visitNamed('circleJoin')`. `visitHomePage` kept (`feature_toggle.feature`).

2. **Given shortcuts still remount after first load.** ADR / `e2e-authoring.mdc`: after first SPA load, Given-shaped jumps use named `router.push`; `visitNamed` is first load, inbound URL, or **explicit remount**. Slice 10 converted leftover `cy.visit('/…')` to `visitNamed` and left them there. `push` already falls back to `visitNamed` when `@firstVisited` is not `yes`. Call sites that remount after login:

   | Helper | Today | After |
   |---|---|---|
   | `navigateToBazaar` | `push('bazaar')` (slice 2) | — |
   | `visitManageAccessTokensPage` | `push('settingsAccessTokens')` (slice 3) | — |
   | `visitRecallStatsPage` | `push('settingsRecallStats')` (slice 3) | — |
   | `navigateToCircle` list/show | `visitNamed('circles'` / `'circleShow')` | `push` |
   | Admin tab | `visitNamed('adminDashboard', {}, { tab })` | `push` with query |
   | Join flow `circleShow` | `visitNamed` | `push` |

   **Stay `visitNamed`:** `loginAs` → notebooks (first load); identify; invitation `circleJoin`; `visitRecallPage`; epub `leaveEpubReadingViewAndReturn`; `visitHomePage` (only `feature_toggle.feature`, first SPA load — `push` would only fall back).

   **`push` has no `query`.** Add optional query in the same slice that converts admin tabs — not a standalone unused parameter.

3. **`.planning/STATE.md` 011 pointer** — done (spent-plan cleanup 2026-08-29). Do not resurrect the 011 diary.

### Inspected and not slicing

| Finding | Why not a slice |
|---|---|
| `jumpToFolderPage` waits outside the innermost `.then` | Cypress enqueues the wait at call time; nested `.then` commands insert **before** it. Same pattern as pre-existing `jumpToNotebookPage`. Not a race vs the old in-then wait. |
| Wiki helper pending→live siblings only assert class | 011 slice 2: canonical live tag lives in `replaceWikiLinksInHtml` “replaces known wikilink text”. Intentional. |
| Two Gherkin Thens share `followWikiLinkToNote` | Both phrases are used (`should open` vs `following`). Not duplicate scenarios. |
| `routes.spec.ts` notebooks + adminDashboard query compiles | Plan asked one extra name compile; query compile is the only `namedLocationHref` + query pin. Dummy lockstep `it.each` is dummy vs production **path**, a different claim. |
| Visit-gate `grep cy.visit(` misses `cy.visit (` | No caller; not worth a slice. |
| EPUB `@ignore` flake (`chooseBookBlockByTitle`) | Pre-existing; 011 remount helper is not the flake. |
| Convert `When I visit recall` to sidebar UI | KeepAlive; 011 jidoka. |
| Ban path strings in `cy.url()` classifiers | Inbound classifiers; ADR allows them. |
| Dummy catch-all `/` test routers | Not a second screen dialect. |
| Accept ADR 0005 | Human. |
| `wikiLinkMarkup.ts` at 248 lines | Under 250. |
| Duplicate ADR vs `e2e-authoring.mdc` intent table | Each artifact stands alone; 011 kept both. |
| Invitation does not visit a UI-copied URL | 011 slice 11 dropped `@savedInvitationCode` on purpose; inject + `visitNamed('circleJoin')` matches that decision. |
| Convert `visitHomePage` to `push` | Only first-load `feature_toggle.feature`. |
| Identity `jumpToNotePage` / `jumpToNotebookPage` / owned `{notepath}` | 015. Bazaar-rooted `{notepath}` still walks the bazaar catalog after `navigateToBazaar`. |
| Catalog listing after inject | 015 page re-enter. Do **not** make `navigateToNotebooksPage` a `visitNamed`. |
| Assimilate menu `route.name` refetch | 015 reverted. Do not bring it back. |

## Design decisions

- **Delete unused my-circles and reload-home E2E**, including `myCirclesPage.ts` if nothing remains. Circles list/show stay on `circlePage.ts`. Keep `visitHomePage`. Do not add “create circle in the UI” or “reload home” scenarios in this plan.
- **One proving observable for push vs remount:** after `loginAs`, set a marker on `window` in the existing logged-in bazaar step path (do not add a Gherkin phrase about `window`). `cy.visit` remount clears it; named `push` does not. Logged-out `browsing.feature` is first load (`push` → `visitNamed` fallback) — not the proof.
- **Optional `query` on `push`** only when converting admin tabs in the same slice.
- **Speed gate (same as 015 / test-optimization):** every remaining slice that converts a logged-in Given shortcut from `visitNamed` to named `push` (slices 2–5). Navigation wall-clock can change; bazaar proving `push` does not cover settings/circles/admin specs.
  - Score: Cypress mocha JSON `stats.duration` (ms → s). Median of **3** runs.
  - Pass if after ≤ before + max(15% of before, 3s). Named `push` should be **faster or equal**.
  - **Baseline at slice start** (current `visitNamed`), then reuse 3 consecutive greens as the after sample.
  - Tee JSON locally (e.g. `/tmp/route-honesty-profile/`); **do not commit**.
  - If a slice fails the gate, **revert that slice**; do not keep a slower remount-everywhere “honesty.”
  - Timer command: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec <feature> --reporter json`
  - Do not use logged-out `browsing.feature` as a push timer (`push` → `visitNamed` fallback).
- **STATE:** 011 pointer already dropped (spent-plan cleanup). Do not add a permanent 011 diary. Mention this cleanup plan only while it is active.
- **Do not** convert recall remount, epub remount, identify, invitation `circleJoin`, `loginAs` notebooks, or `visitHomePage`.
- **Do not** redo 015 catalog re-enter, identity jumps, or menu hydrate.

## Slices

Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

### 1. Remove unused my-circles and reload-home E2E — Structure `[x]`

Deleted unused create-and-copy step, `myCirclesPage.ts`, `start.navigateToMyCircles`, reload-home Given, and `reloginAndEnsureHomePage`. Kept `visitHomePage`. `creating_circles.feature` and `feature_toggle.feature` passed.

---

### 2. Bazaar after login uses named push — Behavior `[x]`

`navigateToBazaar` uses `push('bazaar')`. After login, `__donutSpaDocumentMarker` is set on `window` in that helper and asserted to survive (no Gherkin about `window`). First load skips the marker (`push` → `visitNamed` fallback). Logged-out `browsing.feature` passed once.

**Timing** (`bazaar_subscription.feature`, `stats.duration` median of 3): before 4.835s (6030, 4835, 4560 ms) → after 4.145s (4313, 3936, 4145 ms). Threshold 7.835s. **Pass** (faster). Marker stays local to bazaar — slices 3–5 do not copy it unless a remount is suspected.

**Learning:** no KeepAlive remount needed for bazaar; do not extract a shared marker helper for settings/circles/admin.

---

### 3. Settings Given shortcuts use named push — Behavior `[x]`

`visitManageAccessTokensPage` → `push('settingsAccessTokens')`. `visitRecallStatsPage` → `push('settingsRecallStats')`. No remount marker (bazaar’s stays local). Settings tabs are not KeepAlive.

**Timing** (`stats.duration` median of 3):
- `user_access_token.feature`: 2.568s → 2.013s (threshold 5.568s). **Pass** (faster).
- Plan-named `recall_stats.feature` is entirely `@wip` (unrelated SDK sequencing race; out of scope). Live timer: `daily_probe.feature` (same `I visit my recall stats` step): 16.339s → 15.263s (threshold 19.339s). **Pass** (faster).

**Learning:** do not un-`@wip` `recall_stats.feature` in this plan. Later slices should pick a CI-runnable spec that actually hits the helper.

---

### 4. Circles list and show after first load use named push — Behavior `[ ]`

**Timing:** yes — `e2e_test/features/circles/notebooks_in_circles.feature` (list/show after login). 3-run baseline at start, then 3 greens.

**Pre:** logged-in SPA. **Trigger:** open a circle (existing `navigateToCircle` / post-join `circleShow`). **Post:** circle UI as today; those jumps `push`. Invitation **join** URL stays `visitNamed('circleJoin')`.

**Verify:** `notebooks_in_circles.feature` (timer). Once: `e2e_test/features/circles/creating_circles.feature` (join stays remount). Record before/after medians in this PLAN.

---

### 5. Admin dashboard tabs use named push with query — Behavior `[ ]`

**Timing:** yes — `e2e_test/features/user_admin/manage_bazaar.feature`. 3-run baseline at start, then 3 greens.

**Pre:** admin session, SPA already loaded. **Trigger:** open an admin tab. **Post:** that tab as today. `push(name, params, query?)`; `visitAdminDashboardTab` passes `{ tab }`. No other `push` caller required to pass query.

**Verify:** `manage_bazaar.feature`. Record before/after medians in this PLAN.

---

### 6. Operator STATE matches shipped 011 — Structure `[x]`

Done in spent-plan cleanup 2026-08-29: `.planning/STATE.md` no longer links the retired 011 PLAN; this cleanup plan is listed while it is active. Do not resurrect 011 diary files.

## Jidoka

- Do not convert `When I visit recall` to sidebar UI.
- Do not restore a silent `time` query cache-buster.
- Timing fail on slices 2–5 → revert **that** slice; do not accept slower E2E for honesty.
- If a Given shortcut **must** remount (KeepAlive / stale view), keep explicit `visitNamed` and note it next to recall/epub — do not silently remount the rest.
- Do not “fix” catalog hydrate or identity jumps here (015). Do not overlap 015’s `navigateToNotebooksPage` re-enter.
- Human owns ADR accept. Do not rewrite ADR 0005 unless a converted call site contradicts the intent table (it should not).
- Live OpenAI recording E2E can flake on `main` (015); do not treat that as a 014 timing or honesty failure.
