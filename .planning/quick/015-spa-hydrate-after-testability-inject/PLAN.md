# SPA hydrate after testability inject

**Status:** in progress (slices 1–5 done; slice 6 timers done, CI not green yet).
**Type:** ad-hoc plan (`.planning/quick/`)
**Depends on:** Proposed [ADR 0005](../../../docs/adrs/0005-web-routes.md) E2E intent table; shipped identity-jump interims (`jumpToNotebookPage` for skip-tracking and note creation).

## Goal

One protocol for **login → testability inject → named `push`**: Given-shaped jumps use **identity**, not a stale catalog; menu chrome follows **assimilation domain events**, not Vue route names; true catalog listing **re-enters** the catalog with a current fetch. Prove with a **speed gate** (test-optimization timing) that the protocol is not slower. Close with **CI green**.

## Problem (one cause, two lifetimes)

`loginAs` does `visitNamed('notebooks')` against an empty world. Background then injects notebooks/notes/assimilation via testability. Named `push` does not remount layout or a page you are already on.

| Snapshot | Lifetime | Intended writers | What broke |
|---|---|---|---|
| Notebook catalog | **Page** (`onMounted` fetch) | Entering the catalog; in-app create/update/refresh | `push('notebooks')` while already there is a no-op → empty listing |
| Menu assimilation counts | **Session chrome** | First load `getMenuData`; in-app assimilate / `next()` DTO | Testability assimilate + jump to a note left login counts (`0/0`) |

Vue is coherent: **page bodies refetch on mount; layout chrome is session-scoped.** Testability writes do not invalidate either. Slice 3 dropped the `route.name` → `getMenuData` navigation proxy (commit `37897066dc`). Catalog listing after inject is still stale until slice 5.

## Already shipped (do not redo)

| What | Why it is interim / keep |
|---|---|
| Skip Memory Tracking → `jumpToNotebookPage` | **Keep.** Domain is notebook settings, not catalog browsing (ADR 0001). |
| Note creation under folder → `jumpToNotebookPage` | **Keep.** Same Given-shaped identity jump. |
| Tree-view Gherkin `{notepath}` | **Restored (slice 4).** Helper identity-jumps the leaf; path still documents tree position. |
| MainMenu refetch on `route.name` | **Reverted (slice 3).** Login hydrate + in-app assimilate / `next()` DTO only. |

## Design decisions

- **Given-shaped “I am on this note / notebook”** → identity `push` (`jumpToNotePage` / `jumpToNotebookPage`). Proposed ADR 0005: later jumps are named `push`, not remount.
- **Unique trigger is the catalog listing** (`I open the notebook … from the notebook catalog`) → must **enter** the catalog with current server data. Do not skip the listing with `jumpToNotebookPage`.
- **Menu counts stay Model A:** session store, hydrate on login, mutate on in-app assimilate / `next()`. Observe the progress bar after an in-app assimilate (or explicit remount if the claim is inbound hydrate). Do **not** refetch `getMenuData` on `route.name`.
- **Catalog already on `/notebooks` after inject:** do **not** default to `visitNamed('notebooks')` (full remount, fights 014 / ADR 0005, slower). Prefer **page re-enter** (leave notebooks via identity jump, then open catalog) **or** a small product “Notes while already on catalog refetches `myNotebooks`” if listing steps still fail without remount. Choose the faster option that still hits the listing UI; **timing gate decides**.
- **Do not** change `loginAs` to delay first visit in this plan (high blast radius). Reconsider only if listing slices fail the speed gate even after re-enter/refetch.
- **Do not** overlap 014 bazaar/settings/circles `visitNamed` → `push`. This plan does not edit `bazaarPage.ts`.
- **Speed gate (from test-optimization / 014):** median of **3** Cypress JSON spec scores. Pass if after ≤ before + max(15% of before, 3s). Identity jump should be **faster or equal**. If a slice fails the gate, **revert that slice**, do not keep a slower “correct” remount-everywhere.
- **Timer specs** (not full E2E suite):
  - `e2e_test/features/note_topology/note_tree_view.feature`
  - `e2e_test/features/assimilation/assimilation_walkthrough.feature`
  - `e2e_test/features/notebooks/notebook_catalog_navigation.feature`
  - After slice 4: also `e2e_test/features/note_topology/wiki_link.feature` (owned `{notepath}` callers)
- Raw JSON: tee locally (e.g. `/tmp/hydrate-profile/`); **do not commit**.
- **CI close:** watch GitHub Actions workflow `donut CI` on `main` after the last behavior push until green. Do not run full local `pnpm verify` unless CI is red and needs a local repro.
- Do not add `@skipOptimizationDueToKnownNecessarySlowness` without developer Jidoka.

## Timer command

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec <feature> --reporter json
```

Median wall of 3 runs per spec. Record in the PLAN (not STATE.md).

## Slices

Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

### 1. Tree view Given-shaped jump — Behavior `[x]`

**Pre:** login + inject “LeSS training”; SPA still on empty `/notebooks`. **Trigger:** open a note to see the sidebar tree. **Post:** tree as today.

Shipped: `I route to the note` (identity) instead of `{notepath}` catalog walk. Spec 6/6 green (~10s). Commit `1aea295ccc`.

---

### 2. Record timing baseline — Structure `[x]`

**Timing:** yes (this slice *is* the baseline).

Score field: Cypress mocha JSON `stats.duration` (ms → s). Logs under `/tmp/hydrate-profile/` (not committed). All 9 runs green.

| Spec | run1 (s) | run2 (s) | run3 (s) | median (s) |
|------|----------|----------|----------|------------|
| note_tree_view | 7.542 | 7.403 | 7.291 | 7.403 |
| assimilation_walkthrough | 15.321 | 15.093 | 15.100 | 15.100 |
| notebook_catalog_navigation | 2.845 | 2.854 | 2.891 | 2.854 |

---

### 3. Menu bar after in-app assimilate — Behavior `[x]`

**Pre:** day 1, notes 1–5, daily cap 2; SPA session already loaded. **Trigger:** assimilate via the walkthrough (menu / panel), not testability-only. **Post:** assimilate menu progress bar visible (`assimilated > 0 && due > 0`).

Shipped: dropped `route.name` `getMenuData` watch and its unit test. Walkthrough asserts the bar after menu + in-app panel assimilate. Skip-tracking `jumpToNotebookPage` unchanged. In-app path was enough (no remount).

Timing (`stats.duration`): 15.536 / 15.307 / 15.281 → median **15.307s** vs 15.100s (gate ≤ 18.100s) — pass.

---

### 4. Owned `{notepath}` jumps to the leaf note — Behavior `[x]`

**Pre:** login + inject; unique behavior is **being on that note**, not browsing cards. **Trigger:** `I navigate to "{notebook}/…/{title}" note`. **Post:** that note’s screen.

Shipped: owned `{notepath}` → `jumpToNotePage(last segment)`; Bazaar root still catalog-walks. Tree-view Gherkin restored to `{notepath}`. Helpers: `jumpToOwnedNoteFromPath` / `navigateToBazaarNoteFromPath` in `navigateNotePath.ts`.

| Spec | Before median | After median | Gate | Result |
|------|---------------|--------------|------|--------|
| note_tree_view | 7.403s | 7.389s | ≤ 10.403s | pass |
| wiki_link | 27.059s (new at slice start) | 26.318s | ≤ 31.118s | pass |

---

### 5. Catalog listing after inject shows current notebooks — Behavior `[x]`

**Pre:** logged in on `/notebooks` (empty hydrate); scenario injects a notebook. **Trigger:** `I open the notebook "…" from the notebook catalog`. **Post:** that notebook page.

Shipped: **page re-enter** (not product Notes refetch, not `visitNamed('notebooks')`). If already on the catalog, `leaveNotebookCatalogIfAlreadyOpen` named-`push`es `root`, then `push('notebooks')` so the listing remounts and fetches. Card click still goes through the catalog UI.

Timing (`stats.duration`): 2.925 / 2.813 / 2.867 → median **2.867s** vs 2.854s (gate ≤ 5.854s) — pass. Also green: `notebook_creation.feature`, `notebook_health.feature`.

---

### 6. Re-profile timers and CI green — Structure `[~]`

Timers re-run (mocha `stats.duration`, 3× each). All gates pass. First `donut CI` on `d14e4c189e` ([run 33263336257](https://github.com/nerds-odd-e/doughnut/actions/runs/33263336257)) **red**: `record_live_audio_with_real_open_ai_service.feature` — stop button stayed disabled; backend `OpenAIInvalidDataException`. Lint / backend unit / frontend unit green; other E2E shards cancelled. Same `note_creation_and_update` shard was **green** on slice 4 (`7c227f5056`). Not a hydrate-spec failure; watching CI on the next push.

| Metric | Before (slice 2) | After |
|--------|------------------|-------|
| note_tree_view median | 7.403s | 7.448s |
| assimilation_walkthrough median | 15.100s | 15.333s |
| notebook_catalog_navigation median | 2.854s | 2.813s |
| wiki_link median (if timed) | 27.059s | 26.564s |

---

## Jidoka

- Do not convert `When I visit recall` to sidebar (014 / KeepAlive).
- Do not refetch menu on every route name change (slice 3 reverted that).
- Timing fail → revert that slice.
- Human owns ADR 0005 accept.
- If catalog listing **must** remount the whole document, stop and ask; do not silently make every `navigateToNotebooksPage` a `visitNamed`.
