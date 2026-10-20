# Reach and operate the Donut menu from every screen state

**Source:** [Reach and operate the Donut menu from every screen state](../../seeds/SEED-078-cohesive-note-workspace.md#correction-1)

**Identity:** SEED-078#correction-1

## Source and provenance

Bounded correction handed off by the execution retrospective of
SEED-078#story-1 (`.planning/seeds/SEED-078-cohesive-note-workspace.md#story-1`
at commit `e1b635a22b`), executed through plan 003
(`.planning/slice-plans/003-donut-command-center/PLAN.md` at commit
`e1b635a22b`) in commits
`15b6077`, `9fbfd1f`, `e4227c4`, `448d6fd`, and `0202d4a` (claim `74e9a1b`).
The original promises are that story 1 section, recoverable at `e1b635a22b`. This correction
adds no feature promise.

## Goal and scope

Outcome: the Donut menu is reachable and operable from every signed-in or
visitor screen state that had navigation before story 1, by mouse and
keyboard, with its Account group expanding in place, and its tests identify it
unambiguously and without redundant cost.

Preserved promises and constraints: every story 1 promise (one launcher at
the leading end of the page bar, panel contents and order, counts and Resume,
closing triggers, keyboard focus order, visitor sign-in, shell-owned menu data
and catch-up); deferrals stay deferred (no count or indicator on the launcher,
no shortcut that opens the panel, no search in the panel); named-route
navigation and note-route-family active state under
[ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md). The fix for a
missing launcher gives that state the page bar; it adds no second launcher
location.

Excluded:

- Story key example "choose Assimilate → … reopening shows 2": not a
  correction. `AssimilationController.next` does not change counts; the count
  drops only after assimilating, as before story 1. Reported, not planned.
- `withRoutedPages()` and the Donut menu click in
  `DonutApp.searchHistoryMigration.spec.ts`: dropped. Its test titles promise
  "keeps login available"; since the launcher lives in the routed page bar,
  mounting the routed page and opening the panel is that observation, not an
  unrelated readiness check. Slice 8 gives the helper a second caller.
- The shell before `userLoaded` and the nonproduction-only login page: no
  user state is known yet in the first, and the second is the sign-in page
  itself; neither is a navigation state a user works in.

Preparation checkout:
`/Users/terryyin/git/doughnut/.worktrees/open-a-quiet-donut-command-center-consistently-o`
at `0202d4a1ca`, observed 2026-10-20 (uncommitted `DearDough.md` untouched).
Scratch spec files were added, run, and deleted; one scratch edit to
`DonutMenu.vue` was reverted. Preparation authorizes no implementation or
Take.

## Current findings (re-verified at 0202d4a)

1. **Navigation lost where the page bar is gated.** `RecallPage.vue` renders
   `GlobalBar` inside `DailyProbeGate`'s ordinary-recall slot, so the Daily
   probe and its offer-Retry state have no launcher. `BookReader.vue` renders
   `BookReadingShell` (which holds the bar) only once the book and file are
   loaded, so book loading, a book-file error, and a failed book fetch (book
   stays `null`) are dead ends. Found in addition during this re-verification:
   `NotebooksPage.vue` and `NotebookGroupPage.vue` render the bar only inside
   their loaded views, so a failed catalog fetch (endless loader) and a
   missing notebook group have no launcher. Before story 1 the shell-level
   menu covered all of these.
2. **Account opens as a side flyout tuned for the deleted sidebar.**
   `NavigationItem.vue` puts `lg:daisy-dropdown-top lg:daisy-dropdown-right`
   on the Account dropdown; at ≥1024px `dropdownPortalPlacement.ts` places the
   portaled list to the right, aligned to the anchor's bottom, with no
   vertical clamp. Story UI promise: "Account expands its grouped actions in
   place". E2E masks it with `cy.findByText('Admin Dashboard').click({ force: true })`
   in `e2e_test/start/pageObjects/donutMenu.ts`. Account is the only consumer
   of `hasDropdown` and of the `lg:`-prefixed placement branch (its only test
   is the "account nav" case in `useDropdownPortalPosition.spec.ts`).
3. **Ambiguous panel identity.** `.donut-menu-panel` is a pre-existing shared
   class (also on `RichFrontmatterPropertyKeyPresets.vue`, styled in
   `assets/daisyui.css`), yet E2E `pageBase.ts` (`pressDonutMenuLauncherWhen`,
   `openDonutMenu`, `withinDonutMenuItem`) and unit
   `donutMenuTestSupport.ts` (`panelItemLabels`, `expectPanelClosed`) use it as
   the Donut menu's identity.
4. **Keyboard gaps** against "the launcher is a focusable button and the
   panel's items are focusable in order": `LoginButton.vue` is an SVG with
   `role="button"` and no tabindex, so a visitor cannot Tab to sign-in.
   `NavigationItem.vue` action items are `<a>` without href given tabindex and
   Enter only; Space does nothing, and `DonutMenu.vue`'s `<li @click>` closes
   only on click, so Enter on Assimilate that does not navigate (no more
   notes, error) leaves the panel open.
5. **Residue and test cost.** `data-auto-collapse-dropdown` on
   `AutoCollapseDropdown.vue` lost its last product reader in `448d6fd` and is
   read only by `NoteToolbar.overflow.spec.ts`. `useSignedInMenuData(user)` in
   `DonutApp.vue` has no unit observation (the `DonutMenuInAppShell` harness
   rebuilds it). Count, Resume, and placement assertions overlap across
   `DonutMenu.spec`, `DonutMenu.counts.spec`, `DonutMenu.resume.spec`, and
   `GlobalBar.spec`. Two layout-only E2E scenarios sit in
   `messages/message_center_with_unread_message_count.feature`, with a
   redundant `When I open the Donut menu` step before a Then that reopens.

## Decisive premises

| Premise and consuming operation | Literal observation | Result and consequence |
| --- | --- | --- |
| The gated states render no launcher (symptom of finding 1) | Scratch `tests/pages/zzScratchBar.spec.ts`: RecallPage with Daily probe on, with probe check failing, BookReadingPage with book file 404, with `getBook` error; control: ordinary recall. `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/zzScratchBar.spec.ts` | Control passed; the four states failed exactly at `wrapper.find('[aria-label="Donut menu"]').exists()` after their state preconditions passed. Slices 2 and 3 reuse these page mounts. |
| Catalog failure and a missing group render no launcher | Scratch `tests/pages/zzScratchNb.spec.ts`: NotebooksPage with `myNotebooks` error; NotebookGroupPage with an empty catalog (text "could not be found"); control: loaded NotebooksPage | Control passed; both failed at the launcher check. Adds slice 4. |
| Account's list escapes the panel at wide width (finding 2) | Scratch `tests/toolbars/zzScratchMenu.spec.ts` at `page.viewport(1200, 800)`, admin user, Account clicked | Admin Dashboard top −60.6px while the panel spans 40–184px: the list goes off the top of the window. Slice 6's in-panel bounds proof discriminates. |
| Visitor cannot Tab to sign-in; Enter on Assimilate that does not navigate leaves the panel open; Space on Assimilate does nothing (finding 4) | Same scratch spec: Tab from the launcher with no user; Enter on focused Assimilate with `next` returning no `nextUnit`; Space on focused Assimilate | `activeElement` aria-label was `null`; the panel stayed in the DOM; `AssimilationController.next` was not called. Slice 7's proofs discriminate. |
| Browser-mode Vitest observes the sheet and anchoring with real CSS (slice 5 replaces E2E layout checks) | Scratch `tests/toolbars/zzScratchGeo.spec.ts` mounting `GlobalBar` with a user, at 390 and 1200 widths; then scratch edit of `DonutMenu.vue` media query to `max-width: 1px` and rerun of the phone case; edit reverted | Phone: panel top ≥ bar bottom, left ≤ 1, right ≥ width − 1; wide: top ≥ launcher bottom, left within 4px of the launcher, narrower than the window: all passed. With the media query disabled the phone case failed. Mounting `DonutMenu` without `GlobalBar` mis-positions the sheet (top 800), so the spec mounts `GlobalBar`. |
| A DonutApp mount observes the shell's menu-data wiring (slice 8) | Scratch `tests/zzScratchShell.spec.ts`: `currentUserInfo` returns a user, `getMenuData` returns 12 to repeat, `DonutApp` with `withRealRouter(root)` and `withRoutedPages()`, launcher pressed, Recall count read inside the panel; rerun with `useSignedInMenuData(user)` replaced by a comment in `DonutApp.vue`, then restored | Passed with the wiring; failed without it. The home page also lists "Recall", so the query must be scoped to the panel (slice 1's identity). |
| E2E route for the admin Account journey and retargeted page objects is available locally | `CURSOR_DEV=true nix develop -c node --input-type=module -e '…assertSupportedIsolatedCypressSpecs([show_failure_report, user_profile, message_center_with_unread_message_count])'` | `{"requiresPrivateOpenAiMock":false,"requiresPrivateWikidataMock":false}`; plan 003 ran these features in this checkout. The full run starts the disposable stack. |

## Current decisions

- Each page renders its `GlobalBar` outside its loading, probe, and error
  gates; only page-specific bar contents (recall progress, book title and
  layout toggle, notebook titles) stay conditional. No shell-level fallback
  launcher.
- Account becomes an in-panel disclosure: a button with `aria-expanded` whose
  actions render inside the panel. `NavigationItem` loses its dropdown branch
  and `hasDropdown`; `dropdownPortalPlacement.ts` loses the `lg:`-only branch
  and its account-nav test case, which only Account used.
- `NavigationItem` action items and `LoginButton` become native buttons, so
  Enter and Space activate them and the existing `<li @click>` close fires for
  keyboard choices too. `aria-label`s stay, so label-based tests and E2E keep
  working.
- The panel gets its own accessible identity: `role="region"` with
  `aria-label="Donut menu panel"`. Unit and E2E support find it by role and
  name. A `nav` was not chosen because `pageBar()` and `GlobalBar.spec` find
  the bar by `nav`. The shared `.donut-menu-panel` class stays with its form
  consumer, and the Donut menu stops using it.
- Wide anchoring and the phone sheet are pure layout, so browser-mode Vitest
  proves them. The two E2E layout scenarios and everything only they use are
  deleted once that unit proof is green. Plan 003's story-obligation G4 text
  stays as dated story 1 evidence.

## Outside-in proof ownership

| Promise / affected consumer | Owner and observable proof |
| --- | --- |
| Tests identify the Donut menu panel unambiguously | Slice 1: `donutMenuTestSupport.ts` and `e2e_test/start/pageBase.ts` locate the panel by role `region` and name "Donut menu panel"; `tests/toolbars` green; E2E `message_center_with_unread_message_count`, `user_profile`, `show_failure_report` green through the retargeted page objects. |
| The Daily probe and its retry show the launcher | Slice 2: `RecallPage.spec` Daily probe tests assert the "Donut menu" button beside the probe and beside Retry. |
| Book loading, file error, and failed book fetch show the launcher | Slice 3: `BookReadingPage.spec` loading, file-error, and book-fetch-error tests assert the "Donut menu" button. |
| Catalog failure and a missing notebook group show the launcher | Slice 4: `NotebooksPage.spec` (fetch error) and a NotebookGroupPage not-found test assert the "Donut menu" button. |
| Phone sheet below the bar; wide panel anchored below the launcher | Slice 5: `tests/toolbars/DonutMenu.layout.spec.ts` mounting `GlobalBar` at 390 and 1200 widths. |
| Account expands in place at every width; admin reaches Admin Dashboard | Slice 6: `DonutMenu.layout.spec.ts` shows every Account action inside the panel's bounds at 390 and 1200 for an admin; `DonutMenu.spec` account tests through the disclosure; E2E `show_failure_report.feature` with `{ force: true }` removed; `user_profile.feature`. |
| Every panel item is keyboard operable and closes the panel | Slice 7: `DonutMenu.signedOut.spec` Tab reaches and Enter starts sign-in; `DonutMenu.spec` Enter and Space on Assimilate act, and the panel closes when nothing navigates (no-more-notes toast); the existing focus-order test stays green; the home welcome's Assimilate item, which shares `NavigationItem`, still goes to the next note (regression test added before the change). |
| Menu data is fetched by the app shell at sign-in | Slice 8: a DonutApp spec signs in through `currentUserInfo`, renders the routed home page, opens the launcher, and sees the menu-data counts. |
| No residue and no overlapping assertions | Slice 9: `NoteToolbar.overflow.spec` selects the overflow `details` without the marker; each count, Resume, and placement observation lives in one spec. Sweep reading: `data-auto-collapse-dropdown` returns nothing. |
| Removed Account flyout and E2E layout steps leave no trace | Slices 5 and 6 acceptance sweep readings (below), recorded as readings, not tests. |

Frontend proof at each slice: the focused Vitest files, then the `frontend`
skill's typecheck (`pnpm -C frontend exec vue-tsc --noEmit`) before acceptance.
E2E slices run their named features with
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec <feature>`. Hosted CI
remains the broader E2E proof after publication.

## Ordered slices

### 1. The Donut menu panel has its own test identity
Type: Structure
Status: done
Accepted: `tests/toolbars` 79/79 (failed first at `panelItemLabels` before the product change); vue-tsc clean; E2E `message_center_with_unread_message_count` 6/6, `user_profile` 3/3, `show_failure_report` 3/3. Scoped rules now use `.donut-menu > .daisy-dropdown-content`; E2E `pageBase.ts` uses one `[role="region"][aria-label="Donut menu panel"]` selector, so the Recall and Assimilate page objects reach the panel through the same proven helpers.
Proof: `tests/toolbars` green with `donutMenuTestSupport.ts` finding the panel by role `region` and name "Donut menu panel"; E2E `messages/message_center_with_unread_message_count.feature`, `users/user_profile.feature`, `testability/show_failure_report.feature` green through `pageBase.ts` using the same identity; typecheck.

Internal change: the panel element gets `role="region"` and
`aria-label="Donut menu panel"` and drops the shared `.donut-menu-panel`
class (its scoped width and sheet rules move to a Donut-menu-only selector).
`panelItemLabels`, `expectPanelClosed`, `pressDonutMenuLauncherWhen`, and
`openDonutMenu` locate it by role and name. External behavior is unchanged.
Removes the ambiguous identity (finding 3) that every later slice's proof
relies on.

### 2. Recall keeps the Donut menu during the Daily probe and its retry
Type: Behavior
Status: done
Accepted: `RecallPage.spec.ts` probe and Retry tests assert the launcher (failed with the product change reversed); `tests/pages/RecallPage* tests/components/recall tests/toolbars` green; vue-tsc clean. `DailyProbeGate` exposes a `bar` slot with `ordinary-recall`; the bar stays gated only by the KeepAlive-active flag (`isGlobalBarVisible`), which keeps a hidden cached page's search shortcut unregistered.
Proof: `tests/pages/RecallPage.spec.ts` "RecallPage Daily probe entry" tests assert the "Donut menu" button while the probe shows and while Retry shows (fail first); the existing ordinary-recall and KeepAlive tests stay green; typecheck.

Behavior: Daily probe opted in, or today's probe check failed → open the
recall page → the page bar with the launcher is present above the probe or
Retry; ordinary recall still shows the progress bar in that bar.

### 3. Book reading keeps the Donut menu while loading and on failure
Type: Behavior
Status: done
Accepted: `BookReadingPage.spec.ts` asserts one launcher while loading, on a file 404, when `getBook` fails (all failed first), and exactly one once the PDF loads; `tests/components/book-reading tests/pages/BookReading tests/pages/RecallPage tests/toolbars` green; vue-tsc clean. `BookReader` renders a plain `GlobalBar` until the bootstrap exists, then `BookReadingShell`'s bar takes over (one bar at a time, no lifting of the shell's layout-drawer state). Page specs share `donutMenuLaunchersIn(wrapper)` from `donutMenuTestSupport.ts`.
Proof: `tests/pages/BookReadingPage.spec.ts` "loading the book file" tests assert the "Donut menu" button while loading, on a book-file 404, and when `getBook` fails (fail first); loaded EPUB/PDF tests stay green; typecheck.

Behavior: opening a notebook's book → while it loads, when the file fails,
or when the book cannot be fetched → the page bar with the launcher is
present; once loaded the bar still carries the layout toggle, Notebook link,
and book title.

### 4. Notebook catalog failure and a missing group keep the Donut menu
Type: Behavior
Status: done
Accepted: `NotebooksPage.spec.ts` (loading, fetch error, loaded heading) and new `NotebookGroupPage.spec.ts` (loaded group, not found) each assert exactly one launcher; loading, failure, and missing tests failed first; `tests/pages/Notebook` green; vue-tsc clean. CI repair (run 39360839469, `assimilation_walkthrough` failed at the panel after the launcher click): the plain-bar-until-loaded shape of slices 3–4 remounted the bar when loading finished, closing a panel opened during loading. Repaired so each page keeps one bar instance: the notebooks and group views render their bar always with the loader, error, and not-found states in their body; `BookReader` owns the bar and `BookReadingShell` teleports its book controls into it (`bookReadingBarTargetKey`). Proof: "keeps the Donut menu open when … finishes loading" in `NotebooksPage.spec`, `NotebookGroupPage.spec`, `BookReadingPage.spec` (failed first); unit suites green; E2E `assimilation_walkthrough`, `message_center_with_unread_message_count`, `user_profile`, `book_reading/epub_book`, `phone_reading`, `book_browsing` green.
Proof: `tests/pages/NotebooksPage.spec.ts` asserts the "Donut menu" button when `myNotebooks` fails and while loading; a NotebookGroupPage test asserts it beside "This notebook group could not be found." (fail first); typecheck.

Behavior: the notebooks list or a notebook group page whose catalog is
loading, fails, or lacks the group → the page bar with the launcher is
present; the loaded views keep their titles and controls in the bar.

### 5. Panel sheet and anchoring are proven in browser-mode unit tests
Type: Structure
Status: done
Accepted: `DonutMenu.layout.spec.ts` 2/2 (phone case failed with the media query disabled in a reverted scratch edit); `tests/toolbars` 81/81; vue-tsc clean; E2E `message_center_with_unread_message_count` 4/4 after the removal. Sweep reading (2026-10-20, before the slice 5 commit): matches only surviving names — the `donutMenuPanel` selector constant in `pageBase.ts` and the unit helpers `donutMenuPanel`/`donutMenuPanelIn` for the existing panel, and `.agents/skills/e2e-authoring/SKILL.md:151` ("page objects that start from the Donut menu use" `withinDonutMenuItem`); none refers to the removed page object, steps, or scenarios.
Proof: new `frontend/tests/toolbars/DonutMenu.layout.spec.ts` (mounts `GlobalBar` with a user; `page.viewport` 390: panel top ≥ bar bottom and spans the window width; 1200: panel top ≥ launcher bottom, left within 4px of the launcher, narrower than the window) green; then the two E2E scenarios removed and `message_center_with_unread_message_count.feature` green; sweep reading.

Internal change: replaces the E2E layout checks with the unit proof first,
then deletes the scenarios "On a phone, the Donut menu opens as a sheet below
the page bar…" and "On a wide window, the Donut menu opens anchored below its
launcher", `e2e_test/step_definitions/donut_menu.ts`, `donutMenuPanel()` with
its two methods and `index.ts` exports, and anything only they used (for
example `messageCenterIndicator().go()`), repeated until nothing is orphaned.
Removes the E2E cost and the redundant open step (finding 5). Acceptance
sweep reading: `git grep -n -E "donutMenuPanel|expectFullWidthSheetBelowPageBar|expectAnchoredBelowLauncher|I open the Donut menu|from the Donut menu|anchored below its launcher|sheet below the page bar" -- . ':!.planning'` returns nothing.

### 6. Account expands its grouped actions in place
Type: Behavior
Status: done
Accepted: `DonutMenu.layout.spec.ts` admin Account actions within the panel at 390 and 1200 (failed first: list past the panel bottom at 390, −53px at 1200); `AccountMenuItem.spec` `aria-expanded` toggle; `DonutMenu.spec` "closes when the account destination already shown is chosen" (failed before the account list emitted `choose`); `tests/toolbars tests/composables/useDropdownPortalPosition.spec.ts` green; vue-tsc clean; E2E `show_failure_report` 3/3 without `{ force: true }`, `user_profile` 3/3. Account's actions render in a full-width row inside the panel and any choice closes the panel through `@choose`. Sweep reading (2026-10-20, before the slice 6 commit): `hasDropdown|has-dropdown|lg:daisy-dropdown|lgOnly|LG_MEDIA_QUERY|account nav` returns nothing.
Proof: `DonutMenu.layout.spec.ts` for an admin at 390 and 1200: after pressing Account, Admin Dashboard, Recent, Settings, Manage Access Tokens, and Logout all lie within the panel's bounds (fails first: −60px at 1200); `DonutMenu.spec` account-group tests and `AccountMenuItem.spec` green through the disclosure (`aria-expanded` toggles); `useDropdownPortalPosition.spec` green after deleting its account-nav case; E2E `testability/show_failure_report.feature` with `{ force: true }` removed from `donutMenu.ts`, and `users/user_profile.feature`; typecheck; sweep reading.

Behavior: signed in at any width with the panel open → press Account → its
grouped actions appear inside the panel below it and stay inside the window;
choosing one acts and closes the panel as today. Delete the `hasDropdown`
branch and prop from `NavigationItem`, the `lg:`-only placement branch from
`dropdownPortalPlacement.ts`, and its account-nav test. Acceptance sweep
reading: `git grep -n -E "hasDropdown|has-dropdown|lg:daisy-dropdown|lgOnly|LG_MEDIA_QUERY|account nav" -- . ':!.planning'` returns nothing.

### 7. Every panel item is keyboard operable and closes the panel
Type: Behavior
Status: done
Accepted: `DonutMenu.signedOut.spec` Tab reaches "Login via Github" and Enter starts login; `DonutMenu.keyboard.spec` (split from `DonutMenu.spec`) Space on Assimilate goes to the next note and Enter on Assimilate with no more notes closes the panel (all three failed first); `HomePage.welcome.spec` welcome Assimilate reaches the next note (green before the element change); `tests/toolbars tests/pages/HomePage tests/DonutApp.searchHistoryMigration.spec.ts` 104/104; vue-tsc clean. `NavigationItem` action and disclosure items share one native button branch; `LoginButton` is a native button. CI run 39363989869 on 29333c5 failed only `record_live_audio_with_real_open_ai` ("Append live recording transcription with real OpenAI"): the real OpenAI transcription returned "請安靜" for the English "Please be quiet.". No change in this execution touches audio or transcription, and the same feature passed on 25ec462; disposition: external model output variation, not owned or repaired here.
Proof: `DonutMenu.signedOut.spec` Tab from the launcher reaches "Login via Github" and Enter starts the login flow (fails first); `DonutMenu.spec` Space on Assimilate calls `next` (fails first), Enter on Assimilate with no next note closes the panel (fails first), and the existing focus-order and Resume tests stay green; a home-page test, added and green before the element change, presses the welcome section's Assimilate item and reaches the next note (`HomeWelcomeSection` shares `NavigationItem` and has no such test today); typecheck.

Behavior: panel open → Tab through its items, then press Enter or Space on
any of them (signed in or visitor) → the item acts as a click does and the
panel closes, including when the action does not navigate.

### 8. The app shell's menu-data fetch is observed through DonutApp
Type: Structure
Status: done
Accepted: `frontend/tests/DonutApp.menuData.spec.ts` sees the Recall count 12 inside the panel after signing in through `currentUserInfo` (failed with `useSignedInMenuData(user)` commented out, reverted); `tests/toolbars` green; vue-tsc clean. `navItemOf` in `donutMenuTestSupport.ts` now looks inside the panel.
Proof: a DonutApp spec (in `frontend/tests/`, beside the existing DonutApp specs) returns a user from `currentUserInfo`, mocks `getMenuData` with counts, renders the routed home page, presses the launcher, and sees the Recall count; it fails with the `useSignedInMenuData` call removed from `DonutApp.vue` in a scratch edit; `tests/toolbars` green.

Internal change: test-only. Adds the missing observation of the shell wiring
that the `DonutMenuInAppShell` harness rebuilds (finding 5), so the harness
specs no longer stand alone for it.

### 9. Leftover marker and overlapping Donut menu assertions are removed
Type: Structure
Status: done
Accepted: `tests/toolbars tests/notes` 479/479; vue-tsc clean; `NoteToolbar.overflow.spec` finds the overflow `details` through its "more options" control (fails with the note change removed). Moved: Assimilate due title and count, and Messages unread count, from the `DonutMenu.spec` list test to `DonutMenu.counts.spec`; `resume-recall-active` from the `DonutMenu.spec` Resume test to `DonutMenu.resume.spec`. Dropped as duplicates: the list test's Recall count (still in `DonutMenu.counts.spec` "shows the to-repeat count on Recall" and "decreases the count as recall progresses"); the Resume test's "Resume first" (still in `DonutMenu.resume.spec` first test); `GlobalBar.spec` signed-out presence (now the stronger "launcher first" `it.each` case). Sweep reading (2026-10-20, before the slice 9 commit): `git grep -n "data-auto-collapse-dropdown" -- frontend` returns nothing.
Proof: `NoteToolbar.overflow.spec` green selecting the overflow `details` without the marker; `tests/toolbars` and `tests/notes` green; surviving coverage named below; typecheck; sweep reading `git grep -n "data-auto-collapse-dropdown" -- frontend` returns nothing.

Internal change: delete `data-auto-collapse-dropdown` from
`AutoCollapseDropdown.vue`. Give each Donut menu observation one home:
`DonutMenu.spec` keeps item order, attribution, active marking, actions,
account group, closing, and keyboard; `DonutMenu.counts.spec` owns badge
rendering (move the Assimilate due title, Recall, and Messages unread checks
from the list test, dropping the ones it already has); `DonutMenu.resume.spec`
owns Resume availability and first position, while `DonutMenu.spec` keeps
choosing Resume; `GlobalBar.spec` keeps launcher placement as one test over
signed-in and signed-out. No observation is lost; list any assertion dropped
as a duplicate with the test that still covers it in the slice's acceptance.
