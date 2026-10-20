# Find and open notes from search inside the Donut menu

**Source:** [Find and open notes from search inside the Donut menu](../../seeds/SEED-078-cohesive-note-workspace.md#story-2)

**Identity:** SEED-078#story-2

## Goal and scope

Outcome: a signed-in learner finds and opens the next note from the global
search that now lives inside the Donut menu panel, at every width, and the
page bar no longer carries its separate search control.

Included: the existing search surface (field, history control, scope
controls, Recent and Matches results with their switch) as the panel's last
section; a quiet open that leaves focus on the launcher and shows results
only once search is entered; Ctrl/Cmd+F opening the panel into search with
the field focused; a result opening its named-route destination and closing
the panel, including the note already open; history picks, scope toggles,
and Matches/Recent switches keeping the panel open; the keyboard journey
(Tab to the field after the items, ArrowDown into the results, ArrowUp back,
Enter opens); the panel scrolling within the space below the bar on a short
window; deletion of the bar's search control with everything only it used.

Excluded (deferred, not rejected, as the story records): a Search item or
count on the launcher; any route or query for search being open; a
phone-specific search layout; search for visitors; changes to the note-local
search, folder search, or wiki-link flows, which share the search components.

Boundary assumptions from the story: while a modal dialog is open the Donut
menu is unreachable, so Ctrl/Cmd+F then leaves the dialog alone; correction-1
(plan 004) is executing on the same panel file and test support, and
execution of this plan starts from the trunk of that moment and reconciles
ordinarily.

Key examples are the story's; the slices below cite them.

Preparation checkout: this worktree at `2c6a1ef05d`, observed 2026-10-20.
Two scratch specs were added, run, and deleted; no product file was edited;
no product runtime was started. Preparation authorizes no implementation or
Take.

## Current structure (read at 2c6a1ef05d)

- `GlobalBar.vue` renders `DonutMenu` first, page slots, then a `PopButton`
  ("search note", title "Search note (Ctrl+F / Cmd+F)", signed-in only)
  whose modal hosts `SearchForm` with no note, and registers the
  `note-search` shortcut (`keyboardShortcuts.ts`: Ctrl/Cmd+F without Shift,
  not guarded by editable targets) to open that modal while a user is
  signed in. `PopButton.onTriggerClick` calls `primeSoftKeyboard()` before
  opening, so a coarse-pointer tap keeps the phone keyboard for the field's
  autofocus. `primeSoftKeyboard` has twelve other callers and stays.
- `SearchForm.vue` with no note renders only `SearchForNoteAndFolder.vue`:
  `TextInput` with `v-focus` (unconditional autofocus on mount), the
  history `AutoCollapseDropdown` whose `DropdownMenu` list is portaled to
  `body` or the enclosing `dialog`, the "All notebooks" (disabled with no
  note) and "All My Circles" toggles, an X when `modalCloser` is given, and
  `SearchResults`, which fetches Recent notes on mount and emits `navigate`
  on a plain result-title click (`SearchResultListItem.onTitleNavigation`
  skips modified clicks). `useStableModalTop` is a no-op without a `Modal`.
- `DonutMenu.vue` is an `AutoCollapseDropdown` (`details`) with the launcher
  `summary`, the item grid, `AccountMenuItem`, `LoginButton` for visitors,
  and the "Donut by" footer; it closes on route change, Escape, launcher
  press, and outside click (`useAutoCollapseDetails`: a click is inside only
  when the details contains it, it is in this dropdown's own portaled
  panel, or it is in another modal).
- Tests: `GlobalBar.spec.ts` owns the Ctrl/Cmd+F, Shift, signed-out, and
  touch-primer cases; `donutMenuTestSupport.ts` owns panel helpers;
  `SearchDialog.*.spec.ts` prove the form, history, and navigation through
  `SearchForm`/`PopButton` and keep doing so for the toolbar flows;
  `DonutApp.searchHistoryMigration.spec.ts` renders `SearchForm` directly
  and opens the Donut menu only to see sign-in, so it is unaffected.
- E2E: `step_definitions/search.ts` "I start searching notes" and "I search
  from the current note" click `toolbarButton('search note')`; "the note
  search dialog should be closed" checks `dialog[open]` through
  `noteTargetSearchDialog.expectClosed`, its only caller. The other search
  steps and page-object methods are shared with the toolbar flows.
- Plan 004 (in progress in a sibling worktree) gives the panel
  `role="region"` and `aria-label="Donut menu panel"`, makes Account an
  in-panel disclosure, and retargets `donutMenuTestSupport.ts` and
  `pageBase.ts`. This plan's specs locate the panel through those support
  helpers, whichever identity trunk has at execution.

## Decisive premises

| Premise and consuming operation | Literal observation | Result and consequence |
| --- | --- | --- |
| Picking a history key inside a details-based panel closes it (slice 5's symptom) | Scratch `tests/zzScratchNested.spec.ts` mounting `SearchForNoteAndFolder` inside an `AutoCollapseDropdown`, launcher clicked, history trigger clicked, item 0 clicked; `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/zzScratchNested.spec.ts`; spec deleted | History item rendered outside the panel; after the click the details was closed and the panel gone. Slice 5 changes the shared closing rule and its proof fails first. |
| Recent notes render as soon as the form mounts (slice 1's disclosure) | Same scratch | "Recent one" was listed right after the launcher click. Slice 1 hides the results region until search is entered. |
| The field's autofocus takes focus when the panel opens (slice 1's quiet open) | Scratch `tests/zzScratchFocus.spec.ts` with the production `focus` directive, launcher focused then clicked, two animation frames | `document.activeElement` was the "Search" field. Slice 1 suppresses autofocus in the panel; its quiet-open proof fails first. |
| Setting `details.open` programmatically renders the panel (slice 3's open path) | Same scratch: `details.open = false` then `true` | Panel absent, then present. The shortcut can open the dropdown without a click. |
| The E2E search feature is admitted by the local runner (slice 4) | `CURSOR_DEV=true nix develop -c node --input-type=module -e 'import { assertSupportedIsolatedCypressSpecs } from "./scripts/isolated-cypress-spec-selection.mjs"; …(["e2e_test/features/note_view/search_note.feature"])'` | `{"requiresPrivateOpenAiMock":false,"requiresPrivateWikidataMock":false}`. The full run starts the disposable stack. |
| Consumers of the bar's search control | Read `GlobalBar.spec.ts`, `step_definitions/search.ts`, `noteTargetSearchDialog.ts`, `DonutApp.searchHistoryMigration.spec.ts`; `git grep` for `search note`, `searchNotePopButtonRef`, `openNoteSearch`, `expectClosed` | Only the bar spec and the two E2E steps reach the control; `expectClosed` has one caller. Slices 3 and 4 retarget them. |

## Current decisions

- The panel renders `SearchForNoteAndFolder` directly (no note, no
  notebook), not `SearchForm`: without a note the wrapper adds nothing. The
  form gains an opt-out of autofocus and passes `SearchResults`' `navigate`
  on as its own emit; each container closes itself on it (`DonutMenu` via
  `close`, `SearchForm` via `modalCloser`), and `modalCloser` inside the form
  only controls its Close button. Toolbar, folder, and wiki-link flows keep
  their behavior.
- "Search entered" is state for one open, held by `SearchForNoteAndFolder`
  behind its `quiet` prop (the panel content remounts on every open, so it
  clears when the panel closes): set by field focus or a history pick; the
  shortcut enters it by focusing the field. The results region renders only
  while it is set. `quiet` also opts out of autofocus through
  `v-focus="!quiet"`; the single `focusDirective` in `utils/focusTarget.ts`
  honours `false` and is what tests mount. No other search state moves.
- The `note-search` registration moves from `GlobalBar` to `DonutMenu`,
  guarded by the signed-in user as today. It opens the dropdown
  (`AutoCollapseDropdown.openDropdown`) and schedules focus into the field.
  The shortcut registry now carries `guardModal` separately from
  `guardEditable`; `note-search` sets `guardModal`, so behind a modal dialog
  the key is left to the dialog untouched (story boundary assumption).
- The shared outside-click rule in `useAutoCollapseDetails` treats a click
  inside a portaled panel whose owning dropdown is inside this details as
  inside. Implemented once in the composable, not as a Donut-menu case: each
  `AutoCollapseDropdown` marks its details `data-dropdown-portal-owner=<portalId>`
  beside the panel's `data-dropdown-portal-for`, both read through
  `dropdownPortalContext.ts`.
- The panel gets a max height of the window below the bar with vertical
  scrolling; the phone sheet and wide anchoring rules stay. "Below the bar"
  is `$page-bar-bottom` in `assets/page-bar.scss` (bar height plus the top
  safe-area inset), shared with `SidebarDrawer`.
- The bar's `PopButton`, its `SearchForm` import, `searchNotePopButtonRef`,
  `openNoteSearch`, the `useKeyboardShortcut` call, and the `Search` icon
  import leave `GlobalBar.vue`; `GlobalBar.spec` keeps only launcher and
  Undo cases, with its `SearchController`/`NoteController` mocks and
  `SoftKeyboardPrimer` harness deleted when nothing left uses them.
- E2E: "I start searching notes" and "I search from the current note" open
  the Donut menu and focus the field; "the note search dialog should be
  closed" is renamed to the panel ("the Donut menu should be closed") with
  `expectClosed` deleted from the dialog page object and the check placed
  with the Donut menu page support. Feature wording changes with it.

## Outside-in proof ownership

| Promise / affected consumer | Owner and observable proof |
| --- | --- |
| Panel holds the search surface after Account, before the footer; quiet open; results only once entered; scope toggle and list switch keep it open | Slice 1: `tests/toolbars/DonutMenu.search.spec.ts` |
| A result opens its destination and closes the panel, including the current note; modified click keeps it open; ArrowDown/ArrowUp/Enter and Tab order | Slice 2: same spec |
| Ctrl/Cmd+F opens into search focused; focuses the field when already open; Shift and signed-out cases; no-op behind a modal | Slice 3: `tests/toolbars/DonutMenu.searchShortcut.spec.ts`, moved from `GlobalBar.spec` |
| The bar's search control is gone and E2E search journeys run through the panel | Slice 4: `GlobalBar.spec` green without search cases; E2E `note_view/search_note.feature` green; sweep reading |
| Picking a history key keeps the panel open | Slice 5: `tests/composables/useAutoCollapseDetails.spec.ts` nested-portal case and `DonutMenu.search.spec.ts` history case |
| Panel fits a short window; phone sheet and wide anchoring unchanged | Slice 6: browser-mode layout spec at 390×500 and the existing 390/1200 cases |
| Toolbar, folder, and wiki-link search flows unchanged | Slices 1 and 4: `tests/wiki-link-or-relationship` and `tests/components/search` green |
| Removed control leaves no trace | Slice 4 acceptance sweep reading, recorded as a reading, not a test |

Frontend proof at each slice: the focused Vitest files with
`env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test <spec>`,
then the `frontend` skill's typecheck
`CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`
before acceptance. Slice 4 runs its feature with
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_view/search_note.feature`
(external-wait exception: the disposable stack boot). Hosted CI remains the
broader E2E proof after publication.

## CI repairs

- Run 39361802870 (slice 2 revision `6a5765ab`): `scripts/sut-isolated-health.test.mjs`
  failed when sequential `allocateFreePort()` calls returned a repeated port.
  Repaired with `allocateFreePorts(count)` in `scripts/sut-isolated-fixtures.mjs`,
  used by every multi-port script test; slice 3 stayed paused meanwhile.

## Ordered slices

### 1. The Donut menu panel holds the global search and opens quietly
Type: Behavior
Status: done
Proof: new `frontend/tests/toolbars/DonutMenu.search.spec.ts` (mounting through `donutMenuTestSupport`): after the launcher click the field is in the panel after the Account item and before "Donut by", `document.activeElement` is not the field, and no result section is rendered (fails first: focus lands in the field and Recent shows); clicking the field shows Recent notes; typing "Sed" with the search mock shows Matches; toggling "All My Circles" and switching to Recent keep the panel open; a visitor sees no field. `tests/wiki-link-or-relationship` and `tests/components/search` stay green; typecheck.

Behavior: signed in, panel closed → press the launcher at any width → the
panel lists the items, Account, and the search row with focus left on the
launcher and no results; focus the field or pick a history key → Recent
notes appear, then Matches as a key is typed, and both stay until the panel
closes. Story examples: phone open, scope toggle.

### 2. A result opens its destination and closes the panel
Type: Behavior
Status: done
Proof: `DonutMenu.search.spec.ts`: a plain click on a Matches title routes to `noteShowLocation` and the panel is closed (fails first: the panel stays open only via route watch, so prove the current-note case too); Enter on the current note's Recent entry leaves the route unchanged and closes the panel (fails first); a Ctrl-click keeps the panel open; from the field, ArrowDown focuses the first result, ArrowUp returns, Enter opens; Tab from the last panel item reaches the field. Typecheck.

Behavior: search entered with results → choose a result by click or Enter →
its note, folder, or notebook route opens and the panel closes; the note
already open closes the panel without a route change. Story examples:
desktop Ctrl+F journey's ending, current note, Tab/Arrow keys.

### 3. Ctrl/Cmd+F opens the Donut menu into search
Type: Behavior
Status: done
Proof: `DonutMenu.searchShortcut.spec.ts` (split from `DonutMenu.search.spec.ts` for size): dispatching Ctrl+F and Cmd+F keydown with the panel closed opens it with the field focused and Recent listed (fails first); with the panel open and focus on the launcher it focuses the field; Ctrl+Shift+F and a signed-out mount do nothing; with a `Modal` mounted the panel stays closed. `GlobalBar.spec` shortcut and touch-primer cases deleted as this spec takes their observations; `GlobalBar.spec` green; typecheck.

Behavior: signed in, panel closed → Ctrl/Cmd+F → the panel is open into
search with the field focused and the browser's find suppressed; already
open → the field is focused; Shift variant, visitor, or an open modal
dialog → nothing changes. The bar's own registration moves here, so the
bar button no longer answers the shortcut. Story examples: desktop Ctrl+F,
Escape then Ctrl+F, visitor, Ctrl/Cmd+Shift+F.

### 4. The page bar's search control is removed and E2E search runs through the panel
Type: Behavior
Status: done
Accepted (2026-10-20, uncommitted candidate on `c6256e03`): `search_note.feature`
6/6 green; frontend toolbars/search suites 178 green; typecheck clean. Sweep
reading: `git grep -n -E "search note|Search note \(Ctrl|searchNotePopButtonRef|openNoteSearch|note search dialog should be closed" -- . ':!.planning'`
printed nothing; the planned `dialog\[open\]` term was too broad (it matches
the toolbar popup specs and the refine-note modal, which stay). `SearchForm`'s
no-note path went with the bar control: `note` is required and the no-note
"All notebooks fixed on" observation lives in `DonutMenu.search.spec.ts`.
Proof: `GlobalBar.spec` green with the search button, `SearchForm` import, and primer harness gone; `tests/toolbars` green; E2E `e2e_test/features/note_view/search_note.feature` green through the retargeted steps (the two opening steps open the Donut menu and focus the field; the closed check observes the panel); typecheck; sweep reading `git grep -n -E "search note|Search note \(Ctrl|searchNotePopButtonRef|openNoteSearch|note search dialog should be closed|dialog\[open\]" -- . ':!.planning'` returns nothing.

Behavior: signed in on any screen → the page bar shows the launcher, page
controls, and Undo, with no search button; the E2E search journeys (open
from the notebooks page, open from the current note, Recent and Matches
opening the current note, title matching, exact-match ordering) pass
through the panel. Deletes the `PopButton` and everything only it used,
repeated until nothing is orphaned.

### 5. Picking a history key keeps the panel open
Type: Behavior
Status: done
Proof: `tests/composables/useAutoCollapseDetails.spec.ts` new case: a click inside a portaled panel owned by a dropdown nested in the details does not close it, while a click in an unrelated portaled panel still does (fails first); `DonutMenu.search.spec.ts`: open the history control, pick a seeded key → the field reads it, the panel stays open, and Matches for it appear (fails first). `tests/wiki-link-or-relationship/SearchDialog.searchKeyHistory.spec.ts` green; typecheck.

Behavior: search entered → press the history control and pick a key → the
field takes the key, the panel stays open, and results follow. Story
example: history pick.

### 6. The panel fits a short window
Type: Behavior
Status: done
Proof: browser-mode layout spec (extend `DonutMenu.layout.spec.ts` when plan 004 has landed it, else create it) at `page.viewport(390, 500)` with search entered and results: the panel's bottom is within the window and its content scrolls (`scrollHeight > clientHeight`) with the field reachable by `scrollIntoView` (fails first); the 390 phone-sheet and 1200 anchored cases stay green. Typecheck.

Behavior: phone with the keyboard open, or any short window, panel open into
search → the panel ends within the window and scrolls, keeping the field and
results reachable; wide windows stay anchored below the launcher. Story
scope: panel fits the window.

## Execution complete

Product advice: no change to backlog order; SEED-078 story-3 and story-4 stay
queued. The learning hypothesis (a search field at the bottom of the panel is
as easy to find as the bar's magnifier was) awaits the owner's observation in
use, not new work. Wrap-up carries forward that plan 004 (SEED-078#correction-1)
edits the same panel and test support: whichever lands second reruns the Tab
order case in `DonutMenu.search.spec.ts` and `DonutMenu.layout.spec.ts`.

## Story obligations

### G1. History pick entering search is untested
Reported: slice 1 — "The history-pick route to entering search has no test in this slice."
Story clause: "Picking a history key, toggling a scope control, or switching Matches/Recent keeps the panel open"
Disposition: proved by slice 5: `DonutMenu.search.spec.ts` "enters search from a picked history key, keeping the panel open" opens the panel without focusing the field, picks "older", and observes the field value, the open panel, the search call for "older", and the Matches hit

### G3. Touch primer on the bar's search button is untested
Reported: slice 3 — "The deleted touch-primer case checked that tapping the bar's search button focuses the soft-keyboard primer straight away, then the field. No new test takes this over"
Story clause: "its touch primer handling"
Disposition: no user cost "so the page bar no longer carries a second, persistent search control competing for space": the bar button's primer code is unchanged until slice 4 deletes the button with it, and the panel needs no primer because the learner's own tap focuses the field

### G2. Real Tab order into the field is untested
Reported: slice 1 — "Tab-into-field is only covered through the focus event; real Tab order is slice 2's proof."
Story clause: "Tab reaches the field after the panel items"
Disposition: proved by slice 2: `DonutMenu.search.spec.ts` "reaches the field by Tab after the panel items" presses real `userEvent.tab()` from the launcher through the panel items and observes the field focused

### G4. A non-zero top safe-area inset is untested
Reported: slice 6 — "Nothing tests a non-zero safe-area inset. Browser tests and E2E cannot simulate `env(safe-area-inset-top)`, so the notch fix itself is not observed by any test."
Story clause: "on a phone with the keyboard open or on a short window the panel scrolls within the space below the bar"
Disposition: no user cost "living inside the Donut menu panel at every width": the panel's max height uses the same `$page-bar-bottom` expression that already places `SidebarDrawer` below the bar on notched phones, and the 390×500 layout spec proves the scroll-within-window behavior with the inset at zero
