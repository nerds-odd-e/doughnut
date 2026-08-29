# Named SPA route honesty follow-up

**Status:** in progress — slices 1–6 done; next is slice 7.
**Type:** ad-hoc plan (`.planning/quick/`)
**Depends on:** shipped `.planning/quick/009-named-spa-route-honesty/` (PLAN retired; code and Proposed [ADR 0005](../../../docs/adrs/0005-web-routes.md) remain)
**Merged from:** this file’s 009 leftovers **and** the former `.planning/quick/011-e2e-named-route-honesty/` (deleted as a duplicate 011).

## Goal

Finish 009’s honesty claim in **unit tests**, then make the **E2E clause** of ADR 0005 true without a second path dialect. Navigation-changing E2E slices must **not get slower** (timed before/after). Stop after any slice.

## Inspection (009 commits `e38912a3b3`..`cd4ba83a65`)

Scope: the eight route-honesty commits only (not later Daily probe work). Bar: Proposed ADR 0005 SPA convention, `unit-testing.mdc` focused assertions, post-change-refactor dead/redundant/duplication.

### Sliced (meaningful) — unit leftovers

1. **ADR still overclaims test routers.** Decision says test routers use production `routes` or `routeMetadata` stubs, not a hand-copied path dialect. Dummy `/` catch-alls (`questionsRouter`, `modalRouter`, `popButtonRouter`) and `useRoute` mocks of `path: "/"` are not a second screen dialect. `useRecallData.spec.ts` now uses `dummyRouteRecordsFromMetadata` (slice 1).

2. **Live wiki HTML is asserted many times.** Canonical live tag lives in `replaceWikiLinksInHtml` “replaces known wikilink text with a note href”. Sibling helper cases and mounted specs now assert only their delta (slice 2). Dummy lockstep is one `it.each` (failureReport + four settings names).

3. **Second live-token `<a>` builder.** `wikiLinkAnchorHtml` owns href / class / `data-wiki-title` / display / `data-note-id` / optional already-escaped inner HTML. `propertyValueField` live and unresolved tokens call it with `wikiLinkBracketedInnerHtml` (slice 3). `parseWikiHtmlFragment` returns the wrap element only.

### Sliced (meaningful) — E2E vs ADR lines 73–75

ADR claims: prefer UI for the trigger; direct location is a Given shortcut; first load may `visit` an href from the named table; later jumps use named `router.push`. `.cursor/rules/e2e-authoring.mdc` already copies that. The suite does not match it.

**Already true:** `e2e_test/start/router.ts` named `push` after `@firstVisited`; note/notebook/book/folder jumps by name **plus a fallback path string**; recall chrome vs remount as two APIs (`navigateToRecallPage` vs `visitRecallPage`); circles list via UI; admin tab as query; `noteShowHref` compile.

**Not true:**

| Call site | Literal |
|---|---|
| `loginActions.ts` | `/notebooks`, `/` |
| `recallPage.ts` | `/recall` (always; ignores `firstVisited`) |
| `bazaarPage.ts` | `/bazaar` |
| `homePage.ts` | `/` |
| `manageAccessTokensPage.ts` | `/settings/access-tokens` |
| `recallStatsPage.ts` | `/settings/recall-stats` |
| `user.ts` | `/users/identify` |
| `conversation.ts` | `/message-center` (duplicates `navigateToMessageCenter`) |
| `circlePage.ts` | `/circles/${id}`, `/circles` |
| `circle.ts` | invitation `url`; after join `/circles/${id}` |
| `adminDashboardPage.ts` | `/admin-dashboard?tab=…` |
| `navigationActions.ts` `forceLoadPage` | `/n${noteId}` |
| `bookReadingEpubMethods.ts` | `cy.visit(readingPath)` from live pathname |
| `testability.ts` invitation | `` `${origin}/circles/join/${code}` `` |

`loginAs` `cy.visit('/notebooks')` never sets `@firstVisited`, so the first `jumpToNotePage` still full-loads via fallback. `router.push` still takes a fallback path and `query: { time: Date.now() }`. Gherkin `When I visit …` is a location shortcut, not UI. Wiki E2E href checks use a regex instead of `noteShowHref`.

### Inspected and not slicing

| Finding | Why not a slice |
|---|---|
| No user-visible navigation bug in the 009 diff | `noteShowHref` lockstep vs production `resolve`; `/n/888` still lands on `/n888`; wiki click still ends at `/n{id}` |
| Dummy settings records are flat; production is nested `SettingsPage` | Same named `path` (`routes.spec.ts` lockstep). Storybook does not need the layout |
| `noteShowHref` instantiates a full dummy table at import | Honest compile against the table; not a defect. Shrinking to a one-route router is speculative |
| `QuillEditor.spec.ts` fixture `href="/n1"` | Inbound HTML (like `quillHtmlToMarkdown` `/n701`), not a navigation assertion. Click uses `data-note-id` |
| Catch-all `/` or `/:pathMatch(.*)*` test routers | Not a competing screen path |
| `useRoute` stubs with `path: "/"` | Not a test router |
| `/users/identify` in `NonproductionOnlyLoginPage.spec.ts` | Separate sign-in path work |
| Convert all `When I visit recall` to sidebar UI | Changes KeepAlive; slower; plan 010 needs remount |
| Production `:to` / `route.name === "home"` vs metadata `root` | Not this follow-up |
| Ban path strings in `cy.url()` / `location` assertions | Classifiers, not a second navigate dialect |
| Full-suite E2E test-optimization profile | This plan only **guards regression** on specs whose navigation mechanism changes. Do not run `cy:run-on-sut` for the whole suite here |
| `notebookSidebarNestedRouteNames` still a name set | Different shape than settings |
| `.planning/STATE.md` still saying 009 is planned | Hygiene in this planning pass; `execute-plan` must not write `STATE.md` |

## Design decisions

### Unit leftovers (009)

- **`useRecallData` router:** `dummyRouteRecordsFromMetadata` (same as other unit stubs that must not import pages). Do not add a recall-only helper.
- **Canonical live HTML once:** `replaceWikiLinksInHtml` “replaces known wikilink text with a note href” keeps the full compiled string. Sibling live cases assert only their delta. Delete one of the two pending→live tests.
- **Mounted tests:** keep display-text / editor-integration; drop `data-note-id` and `noteShowHref` where the helper already pinned that shape.
- **Dummy lockstep:** one `it.each` of named locations (failureReport + the four settings names).
- **`wikiLinkAnchorHtml`:** optional inner HTML (already-escaped). Property-field live and unresolved token tags call it with `wikiLinkBracketedInnerHtml`.
- **`parseWikiHtmlFragment`:** return the wrap element only.

### E2E gate

- **One E2E gate:** `e2e_test/start/router.ts`. Page objects and steps do not call `cy.visit` with SPA path strings.
- **One compile helper:** `namedLocationHref({ name, params, query })` over `dummyRouteRecordsFromMetadata`. `noteShowHref` wraps it. E2E imports that module (Cypress esbuild alias). Do not copy path templates into E2E.
- **`push(name, params, query?)` only** — drop fallback path and empty-name `{ path }`.
- **Drop `time` query** unless a targeted spec proves same-route push needs remount; then explicit `visitNamed`, not a silent cache-buster.
- **Keep** `visitRecallPage` as remount (compiled recall href) and `navigateToRecallPage` as UI. Gherkin `When I visit recall` stays remount.
- **Invitation / identify** stay `visit` of compiled href (`circleJoin` / `nonproductionOnlyLogin`).
- **Epub remount:** prefer `cy.reload()` after UI leave; `visitNamed('bookReading', { notebookId })` only if reload is wrong. Feature is `@ignore` — still change the page object; time that spec with an `@ignore`-including tag filter.
- **Guard:** `cy.visit(` only in `router.ts`; CI next to `check_focus_tags.sh`.
- **ADR stays Proposed.** Last slice rewrites unit-router + E2E bullets so they match the tree.

### Honest ADR E2E split (after the gate)

| Intent | Mechanism |
|---|---|
| Unique trigger **is** in-app navigation | UI |
| Given-shaped shortcut (including Gherkin `When I visit …` when the unique behavior is **on** that screen) | Named `router.push` after first load |
| First SPA load, inbound URL, or **explicit remount** | `cy.visit` of href **compiled from the named table** |

## E2E timing gate (from test-optimization)

Not a full-suite optimize pass. Borrow: JSON durations, **3 consecutive green runs**, no committed profile JSON, flaky = failure, do not “speed up” by dropping remount/coverage.

**When required:** slices marked **Timing: yes**. Frontend-only slices: no Cypress timing.

**Metric:** for each listed `--spec`, run 3 times:

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec <feature> --reporter json \
  | tee /tmp/011-nav-<slice>-<n>.log
```

Parse Cypress JSON `tests[].duration` (ms) as in test-optimization `parse_e2e_profile` (brace-balanced `"stats"` blocks). **Score** = sum of scenario durations for that spec. Record the **median** of the 3 scores in this PLAN. Do not commit logs/JSON (tee under `/tmp/` or gitignored `.planning/quick/*-profile-results.json`).

Default tags stay CI-like (`not @ignore`). For `epub_book.feature` only, add `--expose tags='@ignore'` (or equivalent) so the ignored feature actually runs.

**Pass:** after median ≤ before median + **max(15% of before, 3s)**.

**Recall remount extra:** if after median **< 70%** of before on a remount spec (`question_contest` or whichever recall spec is timed), **stop** — likely `visitRecallPage` became `push` (KeepAlive change). Confirm remount still `cy.visit` of compiled href.

**Fail:** revert that slice’s WIP; do not land a slower (or accidentally non-remount) navigation. Cypress noise: if one of 3 runs is an outlier (infra), re-run the trio once; do not lower the gate.

**3+ green:** after timing passes, the same spec must be green 3 consecutive times (test-optimization E2E verify).

Fill the empty timing tables during execute. Baseline slice writes the **before** numbers; later slices copy them into **after**.

### Timing spec set

| Alias | Spec | Why |
|---|---|---|
| **note_edit** | `e2e_test/features/note_creation_and_update/note_edit.feature` | Login then many `jumpToNotePage` — `@firstVisited` / named `push` |
| **new_user** | `e2e_test/features/users/new_user.feature` | Identify + home |
| **bazaar** | `e2e_test/features/bazaar/browsing.feature` | `visit` bazaar as first load |
| **recall_remount** | `e2e_test/features/ai_generated_recall_questions/question_contest.feature` | `visitRecallPage` must stay remount-class cost |
| **circles** | `e2e_test/features/circles/creating_circles.feature` | Invitation inbound URL (one scenario is skip-tagged for *optimization profiles*; it still runs in CI) |
| **epub** | `e2e_test/features/book_reading/epub_book.feature` | Remount helper; `@ignore` — local timing only |

## Slices

Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

Frontend slices: `CURSOR_DEV=true nix develop -c pnpm frontend:test` on the specs named. No E2E until slice 6.

### 1. Recall test router comes from the named table — Structure `[x]`

`useRecallData.spec.ts` builds the memory-history router from `dummyRouteRecordsFromMetadata` (shared factory, including potential-session mount). Resume click asserts `currentRoute.name === "recall"`. In-browser suite ~35ms; dummy table is not seconds.

**Verify:** `pnpm frontend:test tests/composables/useRecallData.spec.ts` — 6 passed.

---

### 2. Live wiki tests pin the compiled HTML shape once — Structure `[x]`

Canonical live HTML stays in the known-`[[MyNote]]` helper spec. Siblings assert class/live-vs-pending/dead only. Dropped duplicate in-flight pending→live. Dummy lockstep is one `it.each`. Mounted wiki specs drop `noteShowHref` / `data-note-id` except path-markdown `data-note-id`. Multi-occurrence asserts both tokens replaced, not the full tag twice.

**Verify:** 6 listed specs — 65 passed.

---

### 3. Property-field token anchors go through `wikiLinkAnchorHtml` — Structure `[x]`

Optional already-escaped `innerHtml` on `wikiLinkAnchorHtml`. Property-field live/unresolved tokens call it with `wikiLinkBracketedInnerHtml`. Canonical property HTML shape is the dead well-formed case; live sibling keeps href/`data-note-id` delta. `parseWikiHtmlFragment` returns the wrap only. Serialize round-trip still `[[N]]`.

**Verify:** `propertyValueField.spec.ts` + `wikiLinkMarkup.spec.ts` — 26 passed after refactor; `replaceWikiLinksInHtml.spec.ts` also green during implementation.

---

### 4. Compile any named location to href — Structure `[x]`

`namedLocationHref({ name, params, query })` in `frontend/src/routes/namedLocationHref.ts` over `dummyRouteRecordsFromMetadata` (no page imports). `noteShowHref` wraps it. Extra compile example in `routes.spec.ts`: notebooks vs production `router.resolve`.

**Verify:** `pnpm frontend:test tests/routes/routes.spec.ts` — 13 passed.

---

### 5. E2E bundles the compile helper — Structure `[x]`

Cypress esbuild and `e2e_test/tsconfig.json` alias `@/routes` → `frontend/src/routes` (not `@/` → pages). `e2e_test/start/router.ts` re-exports `namedLocationHref`. Navigation still uses fallback paths. `rootDir: ".."` + `noEmit: true` so the frontend href module is a legal TS input.

**Verify:** esbuild bundle of the router re-export includes href modules, not `.vue` pages. Runtime in slice 6.

**Learning for slice 6:** import `noteShowHref` from `@/routes/noteShowLocation` (covered by the `@/routes` prefix).

---

### 6. Wiki live href equals compiled note-show href — Behavior `[x]`

Live wiki `href` equals imported `noteShowHref(id)` (`expectHrefPointsToNote`). Classifier regex on the live href removed. `noteShowPathInUrl` still classifies `cy.url()` after click. Title→id lookup is not used on “should open” steps (two notes can share a title).

**Verify:** `wiki_link.feature` — 14 passed.

---

### 7. E2E navigation timing baseline — Structure `[ ]`

**Timing:** this **is** the before. No product change.

Run the timing protocol on **note_edit**, **new_user**, **bazaar**, **recall_remount**, **circles**. Skip **epub** here if `@ignore` setup is painful; capture epub **before** in slice 11 immediately before changing the remount helper.

Fill:

| Spec | Run1 ms | Run2 ms | Run3 ms | Median before |
|---|---|---|---|---|
| note_edit | | | | |
| new_user | | | | |
| bazaar | | | | |
| recall_remount | | | | |
| circles | | | | |

**Verify:** all three runs green per spec.

---

### 8. Named push / visitNamed gate; drop fallback paths — Structure `[ ]`

**Timing:** yes — **note_edit** after vs slice 7 median.

`router.ts`: `visitNamed` + named `push` only. Update `navigationActions`, `toRoot`, `toMessageCenter`, `notebooksPage`, `folder_page`. `forceLoadPage` → `visitNamed('noteShow', { noteId })`. Drop fallback strings and `time` query.

| Spec | Median before (7) | Median after | Gate |
|---|---|---|---|
| note_edit | | | |

**Verify:** `note_edit.feature` 3× green + timing gate.

---

### 9. Login and home go through the gate — Behavior `[ ]`

**Timing:** yes — **note_edit** and **new_user**. This is the `@firstVisited` fix (later jumps should `push`, **faster or same**, not slower).

**Pre:** Session established. **Trigger:** land on notebooks or home. **Post:** same screens; later jumps named `push`.

**Change:** `loginActions` / `visitHomePage` use `visitNamed` (`notebooks`, `root`).

| Spec | Median before (7) | Median after | Gate |
|---|---|---|---|
| note_edit | | | |
| new_user | | | |

**Verify:** `new_user.feature` and `note_edit.feature` 3× green + timing gate. `feature_toggle.feature` if home is not covered by new_user.

---

### 10. Remaining screen visits use visitNamed — Structure `[ ]`

**Timing:** yes — **bazaar** and **recall_remount** (remount lower-bound applies).

Replace leftover `cy.visit('/…')` in bazaar, recall remount, recall stats, access tokens, message-center, circles list/show, identify, admin dashboard (`name: adminDashboard`, `query: { tab }`). `conversation.ts` uses the message-center helper.

| Spec | Median before (7) | Median after | Gate |
|---|---|---|---|
| bazaar | | | |
| recall_remount | | | (also ≥ 70% of before) |

**Verify:** those two specs 3× green + gates. If the slice blows the time-box, split by area; keep the same timing specs on the recall/bazaar half.

---

### 11. Inbound invitation URL and epub remount — Behavior `[ ]`

**Timing:** yes — **circles** vs slice 7; **epub** before/after in this slice (3× with `@ignore` tags).

Invitation alias from `namedLocationHref({ name: 'circleJoin', params: { invitationCode } })` + origin. Join-then-show: `visitNamed('circleShow', { circleId })`. Epub: `cy.reload()` or `visitNamed('bookReading', …)`.

| Spec | Median before | Median after | Gate |
|---|---|---|---|
| circles | (from 7) | | |
| epub | (this slice) | | |

**Verify:** `creating_circles.feature` 3×; epub feature 3× with ignore tags.

---

### 12. Guard: `cy.visit` only in the gate — Behavior `[ ]`

**Timing:** no.

**Pre:** Planted `cy.visit('/recall')` in a page object. **Trigger:** check. **Post:** fails with file/line; clean tree passes.

`scripts/check_e2e_spa_visit_gate.sh`; CI next to `check_focus_tags.sh`.

---

### 13. Honest stricter ADR (unit routers + E2E) — Structure `[ ]`

**Timing:** no.

Rewrite ADR 0005: test routers (slice 1 done); E2E bullets = the intent table above; point at `e2e_test/start/router.ts`. Update `.cursor/rules/e2e-authoring.mdc`. Status stays **Proposed**.

Do not start until slice 12 is green and timing tables for slices 8–11 pass.

## Timing summary (fill at close)

| Spec | Baseline median | Final median | Verdict |
|---|---|---|---|
| note_edit | | | |
| new_user | | | |
| bazaar | | | |
| recall_remount | | | |
| circles | | | |
| epub | | | |

If a spec was not worse but Cypress JSON was unusable, say so and use wall-clock of the 3 `cypress run` invocations with the same gate — do not invent durations.

## Jidoka

- Do not convert `When I visit recall` to sidebar UI.
- If dropping `time` query breaks a spec, use explicit `visitNamed` remount; do not restore a silent cache-buster without an ADR note.
- Cypress must not import page components via the href module.
- Timing gate fail → revert that slice, do not “accept slower for honesty.”
- Recall remount suspiciously faster → confirm `visitRecallPage` still full-loads.
- Human owns ADR accept.
