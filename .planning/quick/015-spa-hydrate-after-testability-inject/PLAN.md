# SPA hydrate after testability inject

**Status:** shipped (2026-08-29). Plan retired — protocol lives in E2E helpers and `MainMenu.vue`.
**Type:** ad-hoc plan (`.planning/quick/`)

## Protocol (login → testability inject → named `push`)

| Need | Mechanism |
|------|-----------|
| Given-shaped “I am on this note / notebook” | Identity `push`: `jumpToNotePage` / `jumpToNotebookPage`. Owned `{notepath}` → `jumpToOwnedNoteFromPath` (leaf). Bazaar root still walks the bazaar catalog. |
| Unique trigger **is** the catalog listing (`I open the notebook … from the notebook catalog`) | **Page re-enter:** if already on notebooks, `leaveNotebookCatalogIfAlreadyOpen` named-`push`es `root`, then `push('notebooks')` so the listing remounts and fetches. Card click stays on the catalog UI. Not `jumpToNotebookPage`. Not default `visitNamed('notebooks')`. |
| Assimilate menu counts (session chrome) | Model A: hydrate on login (`getMenuData`); mutate on in-app assimilate / `next()` DTO. Walkthrough asserts the bar after menu + panel assimilate. **Do not** refetch `getMenuData` on `route.name`. |

Keep: skip-tracking and note-creation `jumpToNotebookPage`. Do not delay `loginAs` first visit. Do not convert `When I visit recall` to sidebar (014 / KeepAlive). Human owns Proposed [ADR 0005](../../../docs/adrs/0005-web-routes.md) accept.

## Speed gate

Median of 3 Cypress mocha `stats.duration` scores. Pass if after ≤ before + max(15% of before, 3s). Raw JSON was local-only (`/tmp/hydrate-profile/`).

| Spec | Before | After | Gate | Result |
|------|--------|-------|------|--------|
| note_tree_view | 7.403s | 7.448s | ≤ 10.403s | pass |
| assimilation_walkthrough | 15.100s | 15.333s | ≤ 18.100s | pass |
| notebook_catalog_navigation | 2.854s | 2.813s | ≤ 5.854s | pass |
| wiki_link | 27.059s | 26.564s | ≤ 31.118s | pass |

## CI

`donut CI` on `188aa1fab1` [green](https://github.com/nerds-odd-e/doughnut/actions/runs/33263865146). Earlier tip `d14e4c189e` failed once on live OpenAI recording (flake; same shard green on slice 4).
