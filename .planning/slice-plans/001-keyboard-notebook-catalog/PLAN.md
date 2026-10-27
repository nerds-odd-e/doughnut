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
Status: planned
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
Status: planned
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
Status: planned
Proof: same spec, examples 6 and 7, both layouts.

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
