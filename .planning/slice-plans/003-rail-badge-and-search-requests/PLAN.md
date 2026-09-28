# Stop the Assimilate badge covering its icon and search re-requesting recent notes

## Source

- Story: [Stop the Assimilate badge covering its icon and search re-requesting recent notes](../../seeds/SEED-043-sidebar-uat-defects.md#story-2)
- **Identity:** SEED-043#story-2

## Goal and scope

The left rail's Assimilate badge no longer covers its icon, note search asks for
recent notes at most once per search view, and the home page stops drawing its
hidden flow line at phone width (the `h --120` console error).

Excluded (see the seed): the original "rows under the path hint" defect (story 1
removes the hint), any other place showing the total unassimilated count, a
general "no console errors" guarantee, and any rail redesign.

## Approach

Three independent fixes, each with a known cause:

- **Badge.** Reuse the Recall item's existing badge pattern: the Assimilate
  badge carries only the abbreviated due count (`abbreviateCount`), and is
  absent when nothing is due. `formatAssimilationBadge` has no other caller
  and is deleted with its test. The tooltip (`assimilationBadgeTitle`) keeps both
  numbers. No CSS change is needed: the overlap came from a 5–7 character
  label in a badge built for 1–3 characters (`NavigationItem.vue:121-140`).
- **Recent notes.** `useSearchExecution.ts` decides "already fetched" by
  `recentNotes.length === 0`, so it re-requests while a request is in flight
  and forever when the answer is empty. Record that the request was made
  (before awaiting) and fetch only when it was not. The display rules in
  `searchDisplayState.ts` stay unchanged.
- **Home flow line.** `useHomeWelcomePath.ts` measures `.flow-background`,
  which CSS hides at `max-width: 768px`, and builds `h -${width - 120}` from a
  0×0 box. Do not draw when the background has no size.

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| The existing proof entry points run green on trunk | `CURSOR_DEV=true nix develop -c bash -c 'cd frontend && pnpm vitest run tests/toolbars/MainMenu.spec.ts tests/composables/useAssimilationCount.spec.ts tests/components/search/SearchResults.recentNotes.spec.ts tests/pages/HomePage.welcome.spec.ts'` | 4 files, 28 tests passed (real Chromium, browser mode) |
| `formatAssimilationBadge` is used only by the rail | `grep -rn formatAssimilationBadge frontend/src` | Only `useNavigationItems.ts:51` |
| The badge text is asserted in E2E only through one step | `grep -rn "should see assimilation progress" e2e_test/features`; `e2e_test/start/pageObjects/assimilationPage/assimilationMenu.ts:22` | 5 uses in `assimilation/assimilation_walkthrough.feature`, all via `expectAssimilationNavBadge(dueOverTotal)`. The Skip Memory Tracking scenario relies on the total (5, not 6) |
| The recent-notes repeat is reproducible at the component entry point | Temporary spec (deleted): `mountSearchResults({ inputSearchKey: "" })`, then `setProps` to `a`, `ab`, `abc`, `abcd` with `flushPromises` between, `getRecentNotes` returning `[]` | `getRecentNotes` called 5 times |
| The malformed path is reproducible in the unit-test browser | Temporary spec (deleted): `HomePage` mounted with `attachTo: document.body`, wait 200ms | Viewport 414px; `.flow-background` 0×0; `.flow-path` `d` contains `h --120` and `v --360.6` |
| The home flow line is the only dynamically built SVG path | `grep -rn 'h -\${\|"d",\|:d="' frontend/src` | Only `useHomeWelcomePath.ts` |
| Leaving the home page cannot produce the error | `useHomeWelcomePath.ts:5-10,21`: the update looks elements up with `document.querySelector` and returns when they are gone | No timer cleanup needed |

## Slices

### 1. The Assimilate badge shows the due count at the icon's corner
Type: Behavior
Status: done
Proof: `tests/toolbars/MainMenu.spec.ts` and `tests/composables/useAssimilationCount.spec.ts` (17 passed),
and `pnpm cy:run --spec e2e_test/features/assimilation/assimilation_walkthrough.feature` (9 passed).

Behavior:
- 5 due, 128 unassimilated → the badge reads `5` with class `due-count` and
  title `5 due today, 128 total unassimilated` (replaces the `5/128` test).
- 0 due, 128 unassimilated → no Assimilate badge (the existing "nothing due or
  backlogged" test changes to this case).
- E2E: `I should see assimilation progress "2/5"` keeps its feature wording;
  the page object checks the badge text `2` and its title
  `2 due today, 5 total unassimilated`, so the Skip Memory Tracking scenario
  still proves the total.
- Manual: the badge sits at the icon's top-right corner like the Recall badge
  and the icon's top is visible.

### 2. Search asks for recent notes at most once per search view
Type: Behavior
Status: done
Proof: extended `tests/components/search/SearchResults.recentNotes.spec.ts` (10 passed); all search and SearchDialog specs passed (6 files, 68 passed).

Behavior:
- Recent notes answer `[]`; mount with an empty key and type `a`, `ab`, `abc`
  (flushing between) → `getRecentNotes` called once.
- Recent notes request still pending; type several keys → called once, and the
  notes appear once it resolves while the first search is still running.
- Existing tests (loads once on mount, shows recent notes for an empty key,
  excludes the current note) stay green unchanged.

### 3. The home page does not draw its flow line while it is hidden
Type: Behavior
Status: done
Proof: added case to `tests/pages/HomePage.welcome.spec.ts` (3 passed).

## Execution complete

Product advice: No backlog change. Promised maintenance delivered across all three slices (due-only Assimilate badge, recent-notes requested at most once per view, and home flow line not drawn while hidden). Optional hardening if desired: pin viewport in home welcome unit test to ≤768px and assert desktop still sets d.

## Current decisions

- Badge option (b): due count only; total stays in the tooltip (owner, during
  refinement).
