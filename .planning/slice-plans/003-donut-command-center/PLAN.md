# Open a quiet Donut command center consistently on every screen

**Source:** [Open a quiet Donut command center consistently on every screen](../../seeds/SEED-078-cohesive-note-workspace.md#story-1)

**Identity:** SEED-078#story-1

## Goal and scope

Authors and learners open one quiet "Donut menu" launcher at the leading end
of the shared page bar on every screen and at every window width. Its panel
lists the existing destinations with their counts and active marking, Resume
while recall is paused, the grouped account actions, the sign-in action for a
visitor, and the existing attribution link. Choosing closes it; so do the
launcher, an outside tap, Escape, and a route change. The two current
presentations, the wide-window vertical sidebar and the narrow-window
collapsing horizontal menu, are replaced and swept. The seed owns the goal,
scope, key examples, UI, and architecture notes.

Outside this delivery: global search inside the panel (story 2), note actions
in the bar (story 3), any count on the launcher, an always-visible active
indicator, a shortcut that opens the panel, and the home page welcome flow.

Preparation checkout:
`/Users/terryyin/git/doughnut/.worktrees/open-a-quiet-donut-command-center-consistently-o`.
Baseline: `f72443698eaa8246c866a44798045dfff2eaccb8`, observed 2026-10-20.
Execution consumes this landed plan in its selected checkout; repeat an
observation if that checkout changes its relevant boundary. Preparation
authorizes no implementation or Take.

## Existing solutions and decisions

- PFE, reuse unchanged: `useNavigationItems` (destinations, counts, active
  state, Resume availability), `NavigationItem` (item rendering, Resume and
  Assimilate actions), `AccountMenuItem` (grouped account actions),
  `LoginButton` (sign-in action), `useResumeRecall`, and
  `useGoToNextAssimilation`. `AutoCollapseDropdown` is the existing overlay
  container: it closes on outside click through the dropdown portal context
  and exposes `closeDropdown`; it has no Escape handling, which the panel adds.
- PFE, change the owner: `MainMenu.vue` currently both fetches menu data on
  sign-in (with the recall-window catch-up timer and logout) and switches
  presentation at 1024px through `useBreakpoint`. The fetch and timer are
  app-shell responsibilities that must outlive any page bar, so they move to a
  composable `DonutApp.vue` calls. The switcher and both presentations are
  replaced by one launcher component rendered by `GlobalBar.vue` in its leading
  position; `GlobalBar` already carries global search and Undo on every
  signed-in screen.
- Layout: `DonutApp.vue` reserves a 90px side column above 1024px and a fixed
  top-left region below it; `GlobalBar.vue` pads for that region and
  `SidebarDrawer.vue` offsets the drawer by the mobile menu height. With the
  launcher in the bar, the column and region go, and the drawer offset becomes
  the one shared header-height rule the seed contract names.
- Decisions follow the seed: launcher glyph distinct from the Note icon, no
  count on the launcher, same panel on the home page (drops the `isHomePage`
  special case), attribution link kept as the panel footer, sign-in inside the
  panel for visitors. Named-route navigation and note-route-family active
  state stay as `useNavigationItems` provides them under
  [ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md); the launcher is
  the Donut menu, not the domain entity Book of
  [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md). No North Star
  topic is needed: the direction is the seed's approved UI contract.
- The common rule is one launcher and one panel regardless of width, user
  state, or page; width changes the panel's size, not its contents.

## Investigation and decisive premises

Observed in the preparation checkout at the baseline on 2026-10-20. No
scratch edits were made; the worktree held only the seed change.

| Premise and consuming operation | Literal observation | Result and consequence |
| --- | --- | --- |
| The toolbar unit specs are green and cheap, so re-homing and replacing them is bounded | `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars` | 12 files, 81 tests passed in 7.0s (13s wall). `MainMenu.spec`, `MainMenu.recall.spec`, `MainMenu.resume.spec` mount `MainMenu` with a mocked `getMenuData` and observe counts, Resume, Assimilate navigation; `HorizontalMenu.spec` and `VerticalMenu.spec` test the replaced presentations. |
| The E2E menu page objects are driven by named features the runner admits locally | Read `withinMainMenuItem` callers: `messageCenterIndicator`, `recall().expectCount`, `assimilation().expectAssimilationNavBadge`, `mainMenu().userOptions()`, `expectBookLayoutBelowMainMenu`. Step texts map to `messages/message_center_with_unread_message_count.feature`, `recall/property_memory_tracker.feature`, `relationships/reify_property.feature`, `learning_session/commissioned_learning_session.feature`, `users/user_profile.feature`, and book reading. `CURSOR_DEV=true nix develop -c node --input-type=module -e '…assertSupportedIsolatedCypressSpecs(["e2e_test/features/messages/message_center_with_unread_message_count.feature"])'` | `{"requiresPrivateOpenAiMock":false,"requiresPrivateWikidataMock":false}`. The full run starts the stack and mutates disposable data; it is slice 4's early probe. All page objects locate items inside `.main-menu`, visible without opening at the 1200px default viewport; after replacement they must open the panel first. |
| The 1024px switcher is the only consumer of `useBreakpoint`, and the fixed-region padding has no other consumer | Searched `useBreakpoint`, `collapsed-menu-width`, `main-menu-height`, `main-menu-width` across `frontend/src` | One consumer (`MainMenu.vue`); variables consumed by `DonutApp`, `GlobalBar`, `SidebarDrawer`, `HorizontalMenu` only. They go with the replaced presentations, except the drawer offset, which becomes the shared header-height rule. |
| Every signed-in screen renders the shared page bar | Read `GlobalBar`/`ContainerPage`/`NotebookSidebarLayout`/`BookReadingShell` consumers in `frontend/src/pages` | All signed-in screens render it; `RecallPage` shows it while active under KeepAlive. The test-only login page has no bar and no signed-in user. |
| The current narrow menu already closes on route change, outside click, and focus loss | Read `HorizontalMenu.vue` | Confirmed; the panel keeps route-change and outside-click closing and adds Escape. |

Remaining hypothesis settled only by use: a quiet launcher without an
always-visible active item is as easy to find as the current destinations.

## Outside-in proof ownership

| Final promise / affected consumer | Owner and observable proof |
| --- | --- |
| Counts, Resume availability, menu-data fetch at sign-in, and recall catch-up keep their data and timing while no panel is open | Slice 1: the three `MainMenu` specs run through a harness that mounts the shell composable with `MainMenu`, observing the same counts, Resume navigation, and Assimilate navigation. Slice 2 re-homes those observations to the launcher spec. |
| Launcher at the leading end of the page bar opens a panel listing Resume (when paused), Note, Assimilate, Recall, Circles, Bazaar, Messages with counts and active marking, Account group, attribution | Slice 2: launcher unit spec with mocked `getMenuData`: press "Donut menu" → items in order with counts; active route marks its destination; Resume first when recall is paused with trackers and navigates to recall; Assimilate navigates to the next note; Account reveals Admin Dashboard (admin only), Recent, Settings for the user, Manage Access Tokens, Logout; "Donut by" present. `GlobalBar.spec`: the launcher is the first control. |
| Choosing, launcher press, outside tap, Escape, and route change close the panel; keyboard reachable | Slice 2: launcher unit spec for each closing trigger and for the launcher being a focusable button. |
| Same launcher and panel at every width, no side column or fixed region, drawer uses the shared header-height rule, home page behaves the same | Slice 4: `DonutApp`, `GlobalBar`, `SidebarDrawer` layout change; E2E page objects open the panel and scope to it; the five named features plus book reading pass; one new phone-width scenario (`I am on a window 390 * 844`) opens the launcher and chooses a destination; book-reading layout asserts below the page bar. Home page: launcher spec at the home route (`root`) lists the destinations. |
| Signed-out visitor reaches sign-in from the same launcher | Slice 3: launcher spec without a user: press "Donut menu" → the sign-in action is listed and triggers the existing login flow. E2E `I haven't login` journeys continue through the key-shaped sign-in control relocated into the panel. |
| Replaced presentations leave no trace | Slice 4 acceptance reading: a search over the product for `VerticalMenu`, `HorizontalMenu`, `useBreakpoint`, `collapsed-menu-width`, `main-menu-height`, `main-menu-width`, `Toggle menu`, `is-expanded`, and `.main-menu` returns nothing. Recorded as a reading, not a test. |

Frontend proof at each slice: the focused Vitest files, then the `frontend`
skill's typecheck before acceptance. After adding or deleting a component, run
the Vite build so `frontend/components.d.ts` is regenerated and committed.
Hosted CI remains the broader E2E proof after publication.

## Ordered slices

### 1. App shell keeps menu counts and recall catch-up without a menu presentation
Type: Structure
Status: done
Proof: `MainMenu.spec`, `MainMenu.recall.spec`, `MainMenu.resume.spec` green through a harness that mounts the new shell composable beside `MainMenu`; `tests/toolbars` green; typecheck.

Internal change: move the sign-in menu-data fetch, the recall-window
catch-up timer, and logout out of `MainMenu.vue` into one composable that
`DonutApp.vue` calls once per app; `MainMenu` keeps rendering the two current
presentations from the stores. External behavior is unchanged. Enables slice
2, whose launcher lives in the per-page bar and must not own that lifecycle.

Accepted: `useSignedInMenuData(user)` (fetch and catch-up timer) is called
once by `DonutApp.vue`; `logout` became the module function
`managedApi/window/logout.ts`, which the slice 2 launcher imports directly.
`mountMainMenu` in `tests/toolbars/mainMenuTestSupport.ts` mounts the
composable beside `MainMenu`. `env -u NODE_ENV CURSOR_DEV=true nix develop -c
pnpm frontend:test tests/toolbars tests/DonutApp` (repo root): 15 files, 96
tests green; `pnpm -C frontend exec vue-tsc --noEmit` clean. Slice 2 re-homes
these observations onto the launcher harness.

### 2. The Donut menu launcher opens the command panel from the page bar
Type: Behavior
Status: done
Proof: new launcher spec under `frontend/tests/toolbars/` covering the item list with counts and active marking, Resume and Assimilate actions, Account actions, attribution, and every closing trigger; `GlobalBar.spec` for launcher placement; typecheck; Vite build for `components.d.ts`.

Behavior: signed in on any screen with the page bar → press the "Donut
menu" launcher (book glyph distinct from the Note icon) at the bar's leading
end → a panel opens over the workspace listing the items `useNavigationItems`
provides in order with counts and active marking, the Account group, and the
"Donut by" attribution; choosing navigates or acts as today and closes it;
the launcher, an outside tap, Escape, and a route change close it; the
launcher and items are keyboard focusable. Built from `AutoCollapseDropdown`,
`NavigationItem`, and `AccountMenuItem`. Interim: the two existing
presentations stay mounted until slice 4 removes them, so E2E keeps passing
through `.main-menu`; the signed-out bar slot renders nothing until slice 3.

Accepted: `DonutMenu.vue` (BookMarked glyph, panel rendered only while
open) is the first child of `GlobalBar`'s nav; `NavigationItem`'s action
anchor gained `tabindex="0"` and Enter so Resume and Assimilate are keyboard
reachable. Proof: `DonutMenu.spec`, `DonutMenu.counts.spec`,
`DonutMenu.resume.spec` (harness `donutMenuTestSupport.ts`, fixtures
`menuDataMocks.ts`) carry every MainMenu spec observation except the
horizontal-collapse behaviors slice 4 deletes; `GlobalBar.spec` asserts the
launcher first. `tests/toolbars` 15 files/122 tests, full frontend suite
2254 tests, typecheck, and Vite build green.

### 3. Signed-out visitors reach sign-in through the same launcher
Type: Behavior
Status: done
Proof: launcher spec without a user lists the sign-in action and pressing it starts the existing login flow; `GlobalBar.spec` shows the launcher without a user; any E2E feature that signs out and back in through the bar.

Behavior: signed out on any screen with the page bar → press "Donut menu" →
the panel offers the existing sign-in action, which starts the login flow;
`LoginButton` renders inside the panel; until slice 4 removes them the old
presentations also still show it.

Accepted: `GlobalBar` always renders `DonutMenu`; signed out, its panel holds
only `LoginButton` and the attribution (no public destinations).
`DonutMenu.signedOut.spec` observes the sign-in action and its
`/users/identify` redirect; `GlobalBar.spec` observes the launcher without a
user. Full frontend suite 2257 tests and typecheck green. Signed-out screens
with the bar: home and Bazaar. No E2E step selects the sign-in control.

### 4. One navigation arrangement at every width and on every screen
Type: Behavior
Status: planned
Proof: E2E features `messages/message_center_with_unread_message_count`, `recall/property_memory_tracker`, `relationships/reify_property`, `learning_session/commissioned_learning_session`, `users/user_profile`, and the book-reading feature that asserts the layout below the bar, through page objects that open the panel; one new phone-width scenario choosing a destination from the launcher; launcher spec at the home route (`root`); `tests/toolbars` green; typecheck; Vite build; sweep reading.

Behavior: at any window width, including after resizing, and on the home
page → the same launcher sits at the same bar position with no side column
or fixed top-left region, and opening it shows the same contents. Delete
`MainMenu.vue`'s switcher, `VerticalMenu`, `HorizontalMenu`, `useBreakpoint`,
their specs, `mainMenuTestSupport.ts`, and Storybook stories (keep
`menuDataMocks.ts`, which the launcher specs use), the `.main-menu` wrapper
and its layout rules in `DonutApp.vue`, the bar padding in `GlobalBar.vue`,
and the menu-only variables; keep one shared header-height rule for the
drawer and safe-area offsets. Retarget `withinMainMenuItem`,
`mainMenu().userOptions()`, and `expectBookLayoutBelowMainMenu` to open and
scope to the panel. Run the named features first as the early probe; a
failure there changes this slice before the sweep.

## Current decisions

- One launcher component owns the panel; the shell composable owns menu data.
  No per-width component, no second item source, no count on the launcher.
- Slices 2 and 4 replace the presentations in two commits only to isolate the
  component risk from the layout and E2E retarget risk; both run in this
  execution. Slice 4 exceeds the five-minute target because the replacement
  must land atomically to keep one navigation and green CI; no platform or
  layer split is permitted by the seed.
- Signed-out sign-in (slice 3) precedes removing the old presentations
  (slice 4): the old presentations hold the only visitor sign-in control, so
  removing them first would leave visitors without sign-in between commits
  and break the signed-out E2E journeys. Found by the destructive
  later-outcome check before slice 3 delegation; the original order was stale.

## Story obligations

### G1. Logout's real request and redirect stay unobserved
Reported: slice 1 — "No spec runs the real `fetch(\"/logout\")` and redirect, and none did before."
Story clause: "choosing a destination or action does what it does today"
Disposition: no user cost "Authors and learners find Donut's app commands in one place": the logout request and redirect moved verbatim into `managedApi/window/logout.ts` and were never observed before; `AccountMenuItem.spec` still observes Logout calling it.

### G2. Signed-out page bar shows no launcher
Reported: slice 2 — "Signed out, the launcher slot renders nothing (slice 4)."
Story clause: "a signed-out visitor sees the same launcher; the panel offers the existing sign-in action"
Disposition: proved by slice 3: `DonutMenu.signedOut.spec` "offers only the sign-in action and the attribution" and "starts the login flow when the sign-in action is pressed"; `GlobalBar.spec` "puts the Donut menu launcher in the bar when signed out".

### G3. Narrow-window bar keeps the old menu's padding
Reported: slice 2 — "While the old presentations remain, at 1024px and below the bar still has the padding left for HorizontalMenu, so the launcher sits after that region."
Story clause: "The workspace no longer reserves a side column or a fixed top-left region for navigation."
Disposition: receiving slice 4

### G4. Panel sheet and anchoring are not asserted
Reported: slice 2 — "The narrow-window full-width sheet and the wide-window anchoring are CSS only and not asserted in unit tests"
Story clause: "anchored near the launcher on wide windows and as a full-width sheet below the bar on narrow windows"
Disposition: receiving slice 4

### G5. Account list after the panel closes
Reported: slice 2 — "If the Account sub-dropdown is open and the user clicks blank space inside its portaled list, the outer panel closes and that list may stay floating until the next click."
Story clause: "tapping or clicking outside, choosing nothing and pressing Escape, or a route change closes it"
Disposition: no user cost "Authors and learners find Donut's app commands in one place": the Account list is teleported from inside the panel's `v-if` subtree, so closing the panel unmounts it; no floating list remains.
