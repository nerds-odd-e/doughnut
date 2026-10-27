# Open notebooks from the filter with the keyboard

## Source

- Story: [Open notebooks from the filter with the keyboard](../../seeds/SEED-087-notebook-keyboard-navigation.md#story-1)
- **Identity:** SEED-087#story-1

## Goal and scope

A notebook reader on the notebooks page filters the catalog, reaches a visible
notebook or group header with Up/Down (and Right/Left into and out of a group),
and opens it with Enter. The visible "Filter notebooks" title is deleted; the
input keeps its placeholder and gains an accessible name. Up at the filter and
Down at the last entry stay put (no wrapping). Arrow handling applies only
while focus is on the filter or on an entry link; Tab order and secondary
buttons keep their ordinary behavior.

Exclusions (from the seed): returning to the filter on typing or Escape; the
Circle page's own filter label; Home/End, type-ahead, wrapping; any group
expand/collapse. Frontend only; no API change.

## Outside-in proof

Component tests mount `NotebooksPageView` attached to the document and run in
real Chromium (Vitest browser mode), so the focused element is observable.
Enter is the browser's own activation of the focused link, so proof observes
that focus rests on the entry's RouterLink to the notebook or group route.

| Key example | Observable signal |
| --- | --- |
| 8 Filter remains usable, no title | `NotebookCatalogFilter.spec.ts`: the input has the accessible name and placeholder, filters, and is autofocused (existing cases kept); title deletion accepted by the one-time reading in slice 1 |
| 1 Filter to notebook; 2 Walk back; 3 Bottom boundary; 4 Filter and sort follow the display; 5 No matches; 9 Secondary controls unaffected | new `frontend/tests/pages/NotebookCatalogKeyboard.spec.ts`: after each arrow key dispatched on the active element, `document.activeElement` is the expected entry link (identified by the entry's heading inside it and the RouterLink `to` naming `notebookPage` with its id), or the filter input; cases in list and grid layout |
| 6 Group journey; 7 Preview boundary | same spec: Down visits header, visible members, then the next entry; Right/Left between header and first member; Right on an empty group's header and Left on an ungrouped entry keep focus; the header link's `to` names `notebookGroup` |
| Visible focus indicator | reading at slice 2 acceptance: every entry link (list row, grid card, group header) carries the focus-visible ring style; scroll-into-view is the browser's default on `focus()` |

Frontend proof acceptance also requires the typechecks from the frontend skill
(`vue-tsc --noEmit` and `tsc --noEmit` in `frontend/`) on the same content, or
equivalent `lint`/`build` evidence.

## Ordered slices

### 1. The filter needs no visible title
Type: Behavior
Status: done
Proof: `NotebookCatalogFilter.spec.ts` (add the accessible-name assertion to
the autofocus or filtering case) run with
`env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/NotebookCatalogFilter.spec.ts`;
plus the one-time sweep reading recorded here at acceptance: a search for
"Filter notebooks" over `frontend/` finds only the Circle page's own label.

Behavior: the notebooks page has notebooks → it renders → the filter block
shows the search icon, the input with placeholder "Search by notebook or group
name" and an accessible name, and the clear button; the `<label>` title line
and its markup are deleted (`frontend/src/pages/NotebooksPageView.vue`).

### 2. Up and Down move between the filter and the displayed entries
Type: Behavior
Status: done
Proof: new `NotebookCatalogKeyboard.spec.ts` for examples 1–5 and 9 in both
layouts (set `donut.notebooksPage.layout` in localStorage before mounting;
dispatch keys with `dispatchArrowKey` from
`tests/helpers/searchDialogKeyboardTestSupport.ts`), run with the single-file
command; then the search-dialog consumers of the shared rule:
`tests/wiki-link-or-relationship/SearchDialog.spec.ts`,
`tests/notes/FolderSelector.spec.ts`, `tests/toolbars/DonutMenu.search.spec.ts`.

Behavior: the filter is focused → Down → the first displayed entry link is
focused; Down/Up step forward/backward through the entry links in DOM order;
Up on the first returns to the filter (text intact); Up at the filter, Down at
the last entry, and Down with no displayed entry change nothing; after the
filter text or sort changes, Down from the filter lands on the first entry now
displayed; arrows on an entry's secondary button, on the clear button, or on
the toolbar are not handled.

Design: one keydown handler on the page's filter-and-catalog section. Targets
are the entry links (list row link, grid card link, group header link) marked
with one shared attribute or class and read from the rendered catalog in DOM
order, which is the reading order in both layouts. The input↔rows vertical rule
already lives in `frontend/src/utils/searchDialogKeyboard.ts`
(`handleSearchDialogListKeydown`); generalize its core to serve both the search
dialogs and the catalog (input, ordered targets, no wrap, focus the target
itself) instead of a second copy. Add the focus-visible ring to the three entry
links so the focused entry is visible; `focus()` scrolls it into view.

### 3. Right and Left step into and out of a group
Type: Behavior
Status: done
Proof: `NotebookCatalogGroupKeyboard.spec.ts`, examples 6 and 7, both layouts;
shared `notebookCatalogKeyboardTestSupport.ts` retains the mounted-page boundary.

Behavior: a group header link is focused → Right → its first displayed member
link is focused; a member link is focused → Left → its group header link is
focused; Right on a header whose group shows no member, and Left/Right on any
other entry, keep focus. The group relation comes from the rendered group panel
containing the header link and its member links. Enter is the header's own
RouterLink to `notebookGroup`.

## Current decisions

- Ordered targets and group membership derive from the rendered DOM, not from
  a parallel model of filter, sort, layout, and preview limits.
- No custom Enter handler: activation is the entry's existing RouterLink to the
  named route (ADR 0005). In component tests RouterLink renders `href="#"`
  (observed), so tests identify the focused link by its entry and the
  RouterLink `to` props, not by href.
- Proof stays at component level; no new E2E scenario. Whether Cypress
  `type('{enter}')` activates a focused anchor is unobserved and not needed,
  since Enter on a link is browser behavior, not Donut's.
- Arrow handling only when `document.activeElement` is the filter input or an
  entry link; the search-dialog rule's "focus anywhere within a row" variant is
  not adopted for the catalog.

## Decisive premises

- **Rendered catalog order equals the reading order with a group in the grid
  layout.** Consuming operation: listing `a[href]` inside
  `.notebook-catalog-section` of the mounted `NotebooksPageView`. Observation
  (scratch spec in this workspace, 2026-10-27, reverted): layout `grid`, catalog
  group G(A, B, Extra1, Extra2) + notebook C + subscribed S gave links
  `["C","G","A","B","Extra1","S"]` — ungrouped entry per the default sort, then
  header, then the three previewed members, then the subscribed notebook; the
  section held 6 anchors and 0 buttons.
- **Focusing an entry link makes it the active element in the test runner.**
  Same observation: mounted with `attachTo: document.body`, the filter input
  was `document.activeElement` after mount (existing autofocus), and after
  `links[1].focus()` the second link was `document.activeElement`.
- **Arrow keys dispatched in tests move focus through the existing rule.**
  Matching evidence in the same runner: `SearchDialog.spec.ts` lines 118–135
  (`dispatchArrowKey` then `document.activeElement` within the next row).
- **Consumers of the shared rule.** `searchDialogKeyboard.ts` is imported by
  `SearchForNoteAndFolder.vue`, `FolderSearchForm.vue`, `SearchResultList.vue`,
  `SearchResultListItem.vue`; their specs are `SearchDialog.spec.ts`,
  `FolderSelector.spec.ts`, and `DonutMenu.search.spec.ts` (run in slice 2).

## Plan concerns

Slice-plan refinement: not needed — three independent outcomes with separate
proof loops; the only structural change (sharing the vertical rule) sits inside
the Behavior that needs it, with its consumers' specs named. No remaining
slice-specific concern identified in this review.

## Execution context

- Execution authority: owner invoked dough-execute-plan for SEED-087#story-1;
  planned Story Branch Mode; existing planning authority permits in-scope replanning.
- Owned workspace (created for this story, creation ref verified):
  `/Users/terryyin/git/doughnut/.worktrees/open-notebooks-from-the-filter-with-the-keyboard`;
  branch `codex/open-notebooks-from-the-filter-with-the-keyboard`.
- Integration checkout: `/Users/terryyin/git/doughnut`; authorized publication target
  `origin/main`; slice delivery target is the story branch on `origin`.
- Starting revision: `faced863b730f077dcde757eb7d41db5e76631a8`;
  accepted claim and initial published base: `1e8fa25b2948788b7daedacfc8d31786e887aaae`.
  Publisher `dashboard-territory.local-doughnut`; agent Yui-chan.
- Checkout setup: `./scripts/run.sh bash scripts/worktree_setup.sh` succeeded
  with the checkout's current dependency fingerprint; the 7 filter component
  tests then passed using the plan's literal focused command.
- Delivery formatter: `./scripts/run.sh pnpm format:changed` once per delivery;
  installed pre-commit hook independently runs check-only `pnpm lint:changed`.
- CI: GitHub Actions, `nerds-odd-e/doughnut`, push workflow `ci.yml` (`donut CI`).
  Story-branch observer coordinator `yui-keyboard`, directory
  `/tmp/dough-ci-501/watch-6uAevK`, PID 27237, stream session 28824, yielded cell 11.
  The trunk claim predates this story-branch observer and is unobserved.

## Accepted proof

### Slice 1

- Source-only review: fresh isolated agent `source_review_one` received the
  story's goal, scope, examples and exclusions, no product/plan/return, and
  returned individual commitments. A corrected exact example 8 confirms the
  accessible-name wording is unspecified. Source comparison: non-empty catalog
  renders → title deleted, placeholder/name/icon/clear retained → markup reading
  and existing component assertions agree; keyboard journeys remain owned by
  slices 2 and 3. Circle label remains explicitly excluded.
- `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/NotebookCatalogFilter.spec.ts`:
  7 passed. Real mounted `NotebooksPageView` with `makeMe` catalog; existing
  autofocus case observes focused input id, accessible name and placeholder;
  existing filtering/group-preview/clear/no-match cases observe the retained behavior.
- `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/NotebookCatalogList.spec.ts tests/pages/NotebookCatalogSubscriptions.spec.ts`:
  6 passed; existing catalog fixtures observe sort, group route/preview, dropdown
  dismissal and subscribed member actions through the mounted view.
- `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit` and
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit`: passed.
- One-time removal reading, 2026-10-27: `rg -n 'Filter notebooks' frontend`
  finds only `CircleShowPage.vue:63`, the retained Circle label.
- Consumer trace: `node .agents/skills/dough-execute-plan/scripts/consumer-trace.mjs --checkout /Users/terryyin/git/doughnut/.worktrees/open-notebooks-from-the-filter-with-the-keyboard --base 1e8fa25b2948788b7daedacfc8d31786e887aaae --term 'Filter notebooks' --term notebook-filter-input --term NotebooksPageView`.
  View/page/list/subscription callers covered by the component checks; declarative
  story fixtures typechecked. Circle independent label, route declarations,
  incidental backend fixture and planning/guidance matches do not consume changed
  filter markup. No affected consumer lacks proof.
- Independent refactor `refactor_filter`: none, already clean; no edits or
  invalidated proof. Slice delivered as `8c5915bcbef51aa3cb6610542c15f00a8623a11f`
  to the story branch, suffix base `1e8fa25b2948788b7daedacfc8d31786e887aaae`,
  attached to the retained observer.

### Slice 2

Fresh isolated `source_review_two` received only original source commitments,
qualifications, examples and exclusions. Its individual commitment inventory
agrees with the source; coordinator inspected product, router-link test stub,
fixtures and observing assertions before accepting the following mapping:

| Original trigger/setup | Expected result | Actual observation / remaining owner |
| --- | --- | --- |
| Input focused; list or grid with owned, group, member, subscribed entries | Down reaches each displayed link in reading order; no wrap | `walks the displayed links…` visits owned → group → three displayed members of five → subscribed, exact named-route ids, initial Up and final Down stay |
| C focused, matching query retained | Up visits B, A, then filter with text intact | `walks back…` observes each focus and input value |
| Query or sort changes | Down reaches first currently displayed result | `uses the current filter and sort order` observes Beta's exact route after query and title-desc sort |
| No matches | Down stays in editable filter | `keeps focus…` observes active input |
| Filter or secondary/clear/layout/sort control focused | Left/Right/caret and Tab access retained; secondary arrows keep ordinary behavior | `leaves the filter cursor and secondary controls…` observes unprevented input keys and unchanged control focus; markup has no tabindex changes, handler recognizes only exact filter/link focus |
| Link receives focus | Visible whole-link ring, browser scroll and native Enter destination | Reading: row/header ring, block card link with inset ring; default `focus()` scrolls; existing RouterLinks retain notebookPage/notebookGroup names, ADR 0005; exact route assertions above |
| Group header/member horizontal journeys, including empty group | Right enters, Left returns, unrelated entry stays | Legitimate remaining owner: slice 3 |
| Filter UI and ordinary catalog operations | Name/placeholder/autofocus/filter/sort/layout/previews retained | Slice 1 proof retained; current catalog/page/Circle/Bazaar suites pass; no new control or layout; Circle title and excluded Home/End/type-ahead/return-on-typing/Escape unchanged |

- `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/NotebookCatalogKeyboard.spec.ts tests/pages/NotebookCatalogFilter.spec.ts tests/pages/NotebookCatalogList.spec.ts tests/pages/NotebookCatalogSubscriptions.spec.ts tests/pages/NotebooksPage.spec.ts tests/pages/CircleShowPage.spec.ts tests/pages/BazaarPage.spec.ts`:
  34 passed across 7 files. New keyboard cases attach real page to document,
  select list/grid in localStorage, build real `makeMe` catalogs, dispatch arrows
  on active elements, and assert focused entry links and RouterLink `to` props.
- `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/NotebookCatalogKeyboard.spec.ts tests/wiki-link-or-relationship/SearchDialog.spec.ts tests/notes/FolderSelector.spec.ts tests/toolbars/DonutMenu.search.spec.ts tests/pages/NotebookCatalogFilter.spec.ts tests/pages/NotebookCatalogList.spec.ts tests/pages/NotebookCatalogSubscriptions.spec.ts tests/pages/NotebooksPage.spec.ts tests/pages/CircleShowPage.spec.ts tests/pages/BazaarPage.spec.ts`:
  shared consumers passed 7/4/12 tests. Batch initially failed only new catalog
  test setup (static RouterLink lookup and dropdown flush), corrected and green
  in the 34-test run; shared code unchanged, so those 23 passes are retained.
  Search/Folder mounted tests observe focus traversing results and returning to
  input; DonutMenu also exercises Enter with the production router.
- Both final typechecks passed: `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`
  and `CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit`.
- Consumer trace command: `node .agents/skills/dough-execute-plan/scripts/consumer-trace.mjs --checkout /Users/terryyin/git/doughnut/.worktrees/open-notebooks-from-the-filter-with-the-keyboard --base 8c5915bcbef51aa3cb6610542c15f00a8623a11f --term handleSearchDialogListKeydown --term handleInputListKeydown --term searchDialogKeyboard --term NotebookCard --term NotebookListRow --term NotebookCatalogGroupHeader --term data-notebook-catalog-target`.
  Catalog section/group/card wrappers, page/Circle/Bazaar, search forms/results
  and their tests covered above; component declarations and story fixtures
  typechecked. E2E card/title/click selectors inspected unchanged, no affected
  selector contract; route declarations and incidental backend/CLI/property-list
  or planning matches excluded. No affected consumer lacks proof.
- Independent `refactor_vertical` review: none, already clean; no edits or
  invalidated proof. Active implementation about 5 minutes; no overrun.

### Slice 3

- Slice 2 delivered as `9e8a01a6e1184003b922a529b753c1c6c3748d95`, suffix base `8c5915bcbef51aa3cb6610542c15f00a8623a11f`, retained story-branch observer reused.
- Fresh isolated `source_review_three` received only original source and returned individual commitments, qualifications, journeys and exclusions; source comparison agrees with the earlier mapping, with horizontal promises now applicable.
- Trigger → expected → observation: header Right → first rendered member → `steps into a group…` observes Alpha; Beta Left → header → same journey observes header, then Down visits Alpha/Beta/outside; header Left/member Right/outside Left/Right stay. Filtered group Right reaches Beta in `enters the first currently displayed member after filtering`.
- Empty/preview-limited header → existing group page and Right boundary → `keeps empty and preview-limited groups available…` observes exact group ids, empty header stays, preview enters first displayed member. Slice 2's retained vertical case visits only three of five displayed members and exact owned/member/subscribed notebook routes. No expand/collapse or custom Enter path exists.
- Filter caret, clear/secondary/layout/sort controls → browser handling → boundary case keeps input keys unprevented and control focus unchanged for vertical/horizontal arrows. Default link focus supplies scrolling; retained whole-link rings and native route activation remain. Existing Tab order and scope exclusions remain unchanged by inspection.
- `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/NotebookCatalogKeyboard.spec.ts tests/pages/NotebookCatalogFilter.spec.ts tests/pages/NotebookCatalogList.spec.ts tests/pages/NotebookCatalogSubscriptions.spec.ts tests/pages/NotebooksPage.spec.ts`: 36 passed across 5 files, document-attached real components with makeMe catalogs and list/grid localStorage setup.
- `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit` and `CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit`: final implementation passed.
- Consumer trace: `node .agents/skills/dough-execute-plan/scripts/consumer-trace.mjs --checkout /Users/terryyin/git/doughnut/.worktrees/open-notebooks-from-the-filter-with-the-keyboard --base 9e8a01a6e1184003b922a529b753c1c6c3748d95 --term handleCatalogKeydown --term catalogTargetSelector --term notebook-catalog-group --term data-notebook-catalog-target`. Group/header/row/card/page and catalog/view tests run; story fixture typechecked; route declarations, local targets collection, planning matches excluded. No affected consumer lacks proof. Active implementation about 3 minutes.
- Independent `refactor_groups`: extracted complete handler into `components/notebook/notebookCatalogKeyboard.ts`, shared modifier/composition guard, split group spec with one browser harness to keep files below 250 lines. Coordinator inspected moved product/setup/assertions; behavior and named routes preserved.
- Refactor proof: `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/NotebookCatalogKeyboard.spec.ts tests/pages/NotebookCatalogGroupKeyboard.spec.ts tests/pages/NotebookCatalogFilter.spec.ts tests/pages/NotebookCatalogList.spec.ts tests/pages/NotebookCatalogSubscriptions.spec.ts tests/pages/NotebooksPage.spec.ts tests/wiki-link-or-relationship/SearchDialog.spec.ts tests/notes/FolderSelector.spec.ts tests/toolbars/DonutMenu.search.spec.ts`: 59 passed in 9 files; both typechecks above passed, whitespace check passed. Moved group/vertical observations retain their setups; fresh shared-search proof covers the guard extraction; prior Circle/Bazaar markup proof remains valid.
- Refactor consumer trace: same script/checkout/base with `--term handleInputListKeydown --term isVerticalListNavigationKey --term isUnmodifiedKeydown --term handleCatalogKeydown --term handleNotebookCatalogKeydown --term mountCatalog --term expectFocusedEntry --term setupCatalogKeyboardTest`. Catalog/page/search and moved tests run; story fixtures typechecked; layout storage reused; unchanged type/import/storage/route/planning and backend matches excluded. Active refactor about 4 minutes; no gaps or deferred proof.
