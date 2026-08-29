# SPA hydrate after testability inject

**Status:** in progress (slice 1 done; 2–6 remaining). Do not execute remaining slices until the developer says so.
**Type:** ad-hoc plan (`.planning/quick/`)
**Depends on:** Proposed [ADR 0005](../../../docs/adrs/0005-web-routes.md) E2E intent table; shipped identity-jump interims (`jumpToNotebookPage` for skip-tracking and note creation; tree-view `I route to the note`).

## Goal

One protocol for **login → testability inject → named `push`**: Given-shaped jumps use **identity**, not a stale catalog; menu chrome follows **assimilation domain events**, not Vue route names; true catalog listing **re-enters** the catalog with a current fetch. Prove with a **speed gate** (test-optimization timing) that the protocol is not slower. Close with **CI green**.

## Problem (one cause, two lifetimes)

`loginAs` does `visitNamed('notebooks')` against an empty world. Background then injects notebooks/notes/assimilation via testability. Named `push` does not remount layout or a page you are already on.

| Snapshot | Lifetime | Intended writers | What broke |
|---|---|---|---|
| Notebook catalog | **Page** (`onMounted` fetch) | Entering the catalog; in-app create/update/refresh | `push('notebooks')` while already there is a no-op → empty listing |
| Menu assimilation counts | **Session chrome** | First load `getMenuData`; in-app assimilate / `next()` DTO | Testability assimilate + jump to a note left login counts (`0/0`) |

Vue is coherent: **page bodies refetch on mount; layout chrome is session-scoped.** Testability writes do not invalidate either. Interim `route.name` → `getMenuData` (commit `37897066dc`) is a **navigation proxy**, not an assimilation event. It will miss same-name jumps and over-fetch unrelated screens.

## Already shipped (do not redo)

| What | Why it is interim / keep |
|---|---|
| Skip Memory Tracking → `jumpToNotebookPage` | **Keep.** Domain is notebook settings, not catalog browsing (ADR 0001). |
| Note creation under folder → `jumpToNotebookPage` | **Keep.** Same Given-shaped identity jump. |
| Tree-view Gherkin → `I route to the note` (slice 1) | **Interim wording.** Unique behavior is the sidebar tree. Restore `{notepath}` after the helper jumps by leaf (slice 3). |
| MainMenu refetch on `route.name` | **Revert in slice 2.** Not a domain invalidation. |

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
  - After slice 3: also `e2e_test/features/note_topology/wiki_link.feature` (owned `{notepath}` callers)
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

### 2. Record timing baseline — Structure `[ ]`

**Timing:** yes (this slice *is* the baseline).

Run the timer command 3× on tree-view, assimilation walkthrough, and catalog navigation. Write medians into this PLAN. No product change.

**Verify:** three JSON logs exist locally; PLAN table filled.

---

### 3. Menu bar after in-app assimilate — Behavior `[ ]`

**Pre:** day 1, notes 1–5, daily cap 2; SPA session already loaded. **Trigger:** assimilate via the walkthrough (menu / panel), not testability-only. **Post:** assimilate menu progress bar visible (`assimilated > 0 && due > 0`).

- Revert `route.name` `getMenuData` watch and the “refetches when the route name changes” unit test.
- Rewrite `Menu shows assimilation progress midway through daily plan` so the bar is asserted after in-app assimilate (or after explicit remount if you keep a hydrate-from-server claim — prefer in-app; it is the domain path).
- Keep skip-tracking `jumpToNotebookPage`.

**Verify:** `MainMenu.assimilate.spec.ts`; `pnpm cypress run --spec e2e_test/features/assimilation/assimilation_walkthrough.feature` (3 consecutive green). Timing: walkthrough median vs slice-2 baseline (gate above).

---

### 4. Owned `{notepath}` jumps to the leaf note — Behavior `[ ]`

**Pre:** login + inject; unique behavior is **being on that note**, not browsing cards. **Trigger:** `I navigate to "{notebook}/…/{title}" note`. **Post:** that note’s screen (wiki_link path steps, tree-view if paths restored).

- Non-bazaar `navigateToNoteFromPath`: `jumpToNotePage(last segment)`. Keep Bazaar root on bazaar catalog.
- Restore tree-view Gherkin to `{notepath}` (same observable as slice 1; path documents tree position).

**Verify:** `note_tree_view.feature` and `wiki_link.feature` (3 green on tree-view; wiki_link once unless red). Timing: tree-view vs slice-2; wiki_link median vs a **new** 3-run baseline taken at the start of this slice (file was not in slice 2).

---

### 5. Catalog listing after inject shows current notebooks — Behavior `[ ]`

**Pre:** logged in on `/notebooks` (empty hydrate); scenario injects a notebook. **Trigger:** `I open the notebook "…" from the notebook catalog`. **Post:** that notebook page (rename/health/readme flows as today).

This step **is** catalog navigation. Identity jump is the wrong shortcut.

Implement the **faster** of:

1. **Page re-enter:** if already on `notebooks`, leave via a named `push` (e.g. jump to an injected notebook’s note, or a cheap named route that is not a full `cy.visit`), then `push('notebooks')` so `NotebooksPage` mounts and fetches; then click the card; or
2. **Product:** choosing Notes while already on the catalog refetches `myNotebooks`; listing steps go through that UI.

Do **not** ship full-document `visitNamed('notebooks')` unless both (1) and (2) fail the timing gate and Jidoka agrees remount is worth it.

**Verify:** `notebook_catalog_navigation.feature`, and the catalog-open scenarios in `notebook_creation.feature` / `notebook_health.feature` (targeted `--spec`). Timing: catalog navigation vs slice-2 baseline.

---

### 6. Re-profile timers and CI green — Structure `[ ]`

Re-run the same 3× JSON timers as slice 2 (plus wiki_link if slice 4 added it). Fill after-table. If any spec fails the gate, stop and revert the offending slice — do not “accept slower.”

Then watch `donut CI` on `main` for the branch tip until green (lint, backend unit, frontend, E2E jobs as the workflow defines). If red, fix in a follow-up slice on this plan; do not declare done.

| Metric | Before (slice 2) | After |
|--------|------------------|-------|
| note_tree_view median | | |
| assimilation_walkthrough median | | |
| notebook_catalog_navigation median | | |
| wiki_link median (if timed) | | |

---

## Jidoka

- Do not convert `When I visit recall` to sidebar (014 / KeepAlive).
- Do not refetch menu on every route name change (slice 3 reverts that).
- Timing fail → revert that slice.
- Human owns ADR 0005 accept.
- If catalog listing **must** remount the whole document, stop and ask; do not silently make every `navigateToNotebooksPage` a `visitNamed`.
