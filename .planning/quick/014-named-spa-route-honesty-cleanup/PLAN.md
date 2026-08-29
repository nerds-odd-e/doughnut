# Named SPA route honesty cleanup

**Status:** planned, not started.
**Type:** ad-hoc plan (`.planning/quick/`)
**Do not execute until the developer approves.**
**Depends on:** shipped `.planning/quick/011-named-spa-route-honesty-follow-up/` (PLAN retired; named visit gate, `namedLocationHref`, Proposed [ADR 0005](../../../docs/adrs/0005-web-routes.md) E2E table remain on `main`)

## Goal

Close leftovers from 011: **dead E2E**, the ADR table’s **later-jump = named `push`** (slice 10 still `visitNamed`s most Given shortcuts), and **stale operator STATE**. Do not reopen unit-wiki HTML pinning, recall remount, or ADR accept.

## Inspection (011 on `main`)

Scope: 011’s route-honesty commits (unit leftovers + E2E gate + ADR rewrite), not later probe work. Bar: Proposed ADR 0005 E2E intent table, `unit-testing.mdc` focused assertions, post-change-refactor dead/redundant/duplication.

### Sliced (meaningful)

1. **Dead E2E navigation leftovers.** (a) `When I create a new circle … and copy the invitation code` has no feature caller. `e2e_test/start/pageObjects/myCirclesPage.ts` (`navigateToMyCircles`, `createNewCircle`, `copyInvitationCode`) and `start.navigateToMyCircles` exist only for that step. 011 renamed the copy alias to `@circleInvitationCode`. The UI field is a **full invitation URL** (`origin` + production `resolve(circleJoin)`). If that step were wired, `visitNamed('circleJoin', { invitationCode: url })` would stuff the URL into a path param. Circles scenarios inject a **raw code** and `visitNamed('circleJoin')` — that path is honest. (b) `Given I am re-logged in as {string} and reload the page` / `reloginAndEnsureHomePage` have no feature caller (011 still converted them to `visitNamed('root')`). `visitHomePage` **is** used (`feature_toggle.feature`). Delete the unused clusters; do not invent a UI-copy or reload-home scenario here.

2. **Given shortcuts still remount after first load.** ADR / `e2e-authoring.mdc`: after first SPA load, Given-shaped jumps use named `router.push`; `visitNamed` is first load, inbound URL, or **explicit remount**. Slice 10 converted leftover `cy.visit('/…')` to `visitNamed` and left them there. `push` already falls back to `visitNamed` when `@firstVisited` is not `yes`. Call sites that remount after login:

   | Helper | Today | After |
   |---|---|---|
   | `navigateToBazaar` | `visitNamed('bazaar')` | `push` |
   | `visitManageAccessTokensPage` | `visitNamed('settingsAccessTokens')` | `push` |
   | `visitRecallStatsPage` | `visitNamed('settingsRecallStats')` | `push` |
   | `navigateToCircle` list/show | `visitNamed('circles'` / `'circleShow')` | `push` |
   | Admin tab | `visitNamed('adminDashboard', {}, { tab })` | `push` with query |
   | Join flow `circleShow` | `visitNamed` | `push` |

   **Stay `visitNamed`:** `loginAs` → notebooks (first load); identify; invitation `circleJoin`; `visitRecallPage`; epub `leaveEpubReadingViewAndReturn`; `visitHomePage` (only `feature_toggle.feature`, first SPA load — `push` would only fall back).

   **`push` has no `query`.** Add optional query in the same slice that converts admin tabs — not a standalone unused parameter.

3. **`.planning/STATE.md` still lists 011 as planned** and points at a deleted PLAN. 011’s closeout left operator next steps stale (`execute-plan` must not write `STATE.md`; this slice may).

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
| Convert `visitHomePage` to `push` | Only first-load `feature_toggle.feature`. Dead `reloginAndEnsureHomePage` is slice 1. |

## Design decisions

- **Delete unused my-circles and reload-home E2E**, including `myCirclesPage.ts` if nothing remains. Circles list/show stay on `circlePage.ts`. Keep `visitHomePage`. Do not add “create circle in the UI” or “reload home” scenarios in this plan.
- **One proving observable for push vs remount:** after `loginAs`, set a marker on `window` in the existing logged-in bazaar step path (do not add a Gherkin phrase about `window`). `cy.visit` remount clears it; named `push` does not. Logged-out `browsing.feature` is first load (`push` → `visitNamed` fallback) — not the proof.
- **Optional `query` on `push`** only when converting admin tabs in the same slice.
- **Timing:** only slice 2. Median of 3 Cypress JSON spec scores; pass if after ≤ before + max(15% of before, 3s). Time `e2e_test/features/bazaar/bazaar_subscription.feature` (login then bazaar). Do not use logged-out `browsing.feature` as the push timer.
- **STATE:** mark 011 complete / drop the deleted PLAN pointer. Do not add a permanent 011 diary. Mention this cleanup plan only while it is active.
- **Do not** convert recall remount, epub remount, identify, invitation `circleJoin`, `loginAs` notebooks, or `visitHomePage`.

## Slices

Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

### 1. Remove unused my-circles and reload-home E2E — Structure `[ ]`

Delete the uncalled create-and-copy Gherkin step, `myCirclesPage.ts` (or every helper that only it uses), and `start.navigateToMyCircles`. Delete `Given I am re-logged in as {string} and reload the page` and `reloginAndEnsureHomePage`. Keep `visitHomePage`. Existing inject + `visitNamed('circleJoin')` circles scenarios still pass.

**Verify:** `pnpm cypress run --spec e2e_test/features/circles/creating_circles.feature`. Confirm no remaining imports of `myCirclesPage` or `reloginAndEnsureHomePage`. Once: `e2e_test/features/testability/feature_toggle.feature` (still uses `visitHomePage`).

---

### 2. Bazaar after login uses named push — Behavior `[ ]`

**Timing:** yes.

**Pre:** logged-in SPA (`loginAs` already `visitNamed`'d notebooks); `window` marker set. **Trigger:** visit the Bazaar (existing Given/When). **Post:** bazaar UI as today **and** the marker remains (no document remount). `navigateToBazaar` uses `push('bazaar')`. Logged-out bazaar still full-loads via `push` → `visitNamed` fallback.

**Verify:** `e2e_test/features/bazaar/bazaar_subscription.feature` (marker + subscribe). Once: `e2e_test/features/bazaar/browsing.feature` for logged-out remount fallback. Timing gate on `bazaar_subscription.feature`.

---

### 3. Settings Given shortcuts use named push — Behavior `[ ]`

**Timing:** no (same mechanism as slice 2; settings specs are not the bazaar timer).

**Pre:** logged-in SPA. **Trigger:** open access tokens or recall stats (existing Gherkin). **Post:** those screens as today; `visitManageAccessTokensPage` / `visitRecallStatsPage` use `push`.

**Verify:** `e2e_test/features/users/user_access_token.feature` and `e2e_test/features/recall/recall_stats.feature`.

---

### 4. Circles list and show after first load use named push — Behavior `[ ]`

**Pre:** logged-in SPA. **Trigger:** open a circle (existing `navigateToCircle` / post-join `circleShow`). **Post:** circle UI as today; those jumps `push`. Invitation **join** URL stays `visitNamed('circleJoin')`.

**Verify:** `e2e_test/features/circles/creating_circles.feature` and `e2e_test/features/circles/notebooks_in_circles.feature`.

---

### 5. Admin dashboard tabs use named push with query — Behavior `[ ]`

**Pre:** admin session, SPA already loaded. **Trigger:** open an admin tab. **Post:** that tab as today. `push(name, params, query?)`; `visitAdminDashboardTab` passes `{ tab }`. No other `push` caller required to pass query.

**Verify:** `e2e_test/features/user_admin/manage_bazaar.feature`.

---

### 6. Operator STATE matches shipped 011 — Structure `[ ]`

Update `.planning/STATE.md`: 011 is done (PLAN retired); drop “planned, not started” for the deleted path. Optionally list this cleanup plan as planned until it finishes. Do not resurrect 011 diary files.

**Verify:** `STATE.md` has no link to `.planning/quick/011-named-spa-route-honesty-follow-up/PLAN.md`.

## Jidoka

- Do not convert `When I visit recall` to sidebar UI.
- Do not restore a silent `time` query cache-buster.
- Timing fail on slice 2 → revert that slice; do not accept slower bazaar for honesty.
- If a Given shortcut **must** remount (KeepAlive / stale view), keep explicit `visitNamed` and note it next to recall/epub — do not silently remount the rest.
- Human owns ADR accept. Do not rewrite ADR 0005 unless a converted call site contradicts the intent table (it should not).
