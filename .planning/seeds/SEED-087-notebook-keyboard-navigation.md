---
id: SEED-087
status: dormant
planted: 2026-10-27
planted_during: owner request for keyboard navigation from the notebooks filter
trigger_when: enabling keyboard users to find and open notebooks from the catalog
scope: small
---

# SEED-087: Open notebooks from the filter with the keyboard

## Why This Matters

A notebook reader should be able to filter the notebooks catalog, move to a
result, and open it without reaching for a mouse. The owner requested Up/Down
navigation and Enter activation, plus removal of the redundant visible
"Filter notebooks" title because the input placeholder explains its purpose.

## Alternatives and Decision

Keep the existing catalog and its group previews. Enter on a group should open
its existing group page, matching a click on the group title. Right from a
group header should focus its first visible notebook; Left from a grouped
notebook should return to its header. This provides a shortcut into and out of
a group while Up/Down can traverse the whole visible catalog.

The simpler alternative is Up/Down and Enter alone, leaving Left/Right alone.
Adding collapsible groups would add a new presentation state to this story;
the recommendation uses the previews already displayed. WAI's
[tree keyboard guidance](https://www.w3.org/WAI/ARIA/apg/patterns/treeview/#keyboardinteraction)
supports Right into visible children, Left to a parent, and Enter for an item's
default action. The owner accepted this direction on 2026-10-27 after clarifying
Left/Right behavior on individual notebooks.

## Story

<a id="story-1"></a>

### Open notebooks from the filter with the keyboard

**Identity:** SEED-087#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/001-keyboard-notebook-catalog/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"5e78c15db675f0dd6d596019124c5651d2924599529421d6f0aa790b342fc5ba","plan":"05bf4a8fe72aa05088b55673121d0a49553954907bf13d769d98c1b828c594da"}}
```

**Goal:** A notebook reader on the notebooks page filters the catalog, reaches
the wanted visible notebook or group with the arrow keys, and opens it with
Enter, without leaving the keyboard. The visible "Filter notebooks" title goes;
the placeholder and a programmatic accessible name keep the input understandable.

**Scope:**

Required behavior:

- The notebooks page shows no visible "Filter notebooks" title. The filter
  input keeps its placeholder and has an accessible name for assistive
  technology.
- Keyboard focus moves between the filter input and the catalog's navigation
  targets: each ungrouped notebook, each subscribed notebook, each group header,
  and each visible group member, in the displayed sequence under the current
  filter and sort. The displayed sequence is the reading order: top to bottom
  in the list layout; left to right within each row, then the next row, in the
  grid layout, with a group's header followed by its visible members.
- Down from the filter focuses the first target; Down and Up move forward and
  backward; Up from the first target returns to the filter. Up at the filter and
  Down at the last target keep focus where it is (no wrapping, owner request
  2026-10-27). With no visible target, Down leaves focus in the filter.
- Right on a group header focuses its first visible member; Left on a member
  focuses its group header. Right on a header with no visible member, and Left
  or Right on any other target, keep focus where it is. Groups do not expand or
  collapse.
- The focused target is the entry's own link, so Enter opens it the way a
  click does: a notebook's page for a notebook (owned, subscribed, or a group
  member), the group page for a group header, including a group with more
  members than the preview shows or with no members.
- The focused target shows a visible focus indicator and is scrolled into
  view.
- Arrow handling applies only while focus is on the filter or on a target.
  Inside the filter, Left and Right keep moving the text cursor. Tab and
  Shift+Tab keep reaching the clear-filter button, layout and sort controls,
  and each entry's secondary buttons; arrows pressed on those controls keep
  their ordinary meaning.

Deferred promises (not built or verified here):

- Typing a character or pressing Escape while a target is focused does not
  return to the filter; the reader presses Up or Shift+Tab.
- The Circle page keeps its own visible "Filter notebooks" label; this story
  changes only the notebooks page.
- No Home/End, type-ahead, or wrapping navigation.

Boundary assumptions: the filter keeps its existing autofocus on page load.
Filtering, sorting, layout switching, and group previews behave as today;
navigation reads whatever they display.

**UI:** No new control or layout. The filter block loses its title line and
keeps the search icon, placeholder, and clear button. Keyboard focus shows on
the whole entry link (row text in the list, card body in the grid, header text
for a group) with the ring used for focused links elsewhere in Donut; the
browser scrolls the focused entry into view. Focus on the filter stays visually
as today.

**Architecture:** This adds one responsibility to the frontend: keyboard
traversal over the notebooks page's filter and catalog entry links. Donut
already owns the same input-to-rows rule for search dialogs
(`frontend/src/utils/searchDialogKeyboard.ts` with `focusTarget.ts`): Up/Down
between a text field and rows found in the rendered DOM. The catalog reuses that
rule and adds group Left/Right and the no-wrap boundaries; the ordered targets
come from the rendered catalog, whose DOM order was observed to equal the
reading order in both layouts (2026-10-27, this workspace), so no second model
of layout, sort, or preview limits is built. Activation stays the entry's own
RouterLink to the named `notebookPage` or `notebookGroup` route, as
[ADR 0005](../../docs/adrs/0005-web-routes-accepted.md) requires; no custom
Enter handler or navigation path is added. No other Accepted ADR applies.

**Key examples:**

1. **Filter to notebook:** Notebooks Alpha and Beta are shown; the reader types
   "beta", presses Down, then Enter. Focus moves to Beta's link and Beta's
   notebook page opens.
2. **Walk back:** Notebooks A, B, C are shown and C is focused. Up focuses B,
   then A, then the filter with its text intact. A further Up leaves the filter
   focused.
3. **Bottom boundary:** C is the last visible target and focused. Down keeps C
   focused.
4. **Filter and sort follow the display:** The reader changes the filter or the
   sort, then presses Down from the filter. Focus lands on the first entry now
   displayed.
5. **No matches:** The filter shows "No notebooks match". Down leaves focus in
   the input so the reader can edit or clear the query.
6. **Group journey:** Group G shows members A and B, followed by notebook C.
   From the filter, Down visits G, A, B, C in that order in both layouts. Enter
   on G opens G's group page; Enter on A opens A. Right on G focuses A; Left on
   B focuses G; Left on C keeps C focused.
7. **Preview boundary:** Group H has five members and shows three. Arrows visit
   the three shown members; Enter on H opens the group page with all five. An
   empty group's header is a target and Enter opens its page; Right on it keeps
   the header focused.
8. **Filter remains usable:** The input has an accessible name and the
   placeholder "Search by notebook or group name"; typing filters the catalog;
   Left and Right move the text cursor; Tab reaches the clear-filter button.
9. **Secondary controls unaffected:** With focus on an entry's secondary button
   (reached by Tab), Down does not move focus to another entry.

**Evaluation:** Drive the mounted notebooks page in its component tests (they
run in a real browser): dispatch arrow keys and observe the focused element,
in both layouts, with filters, subscriptions, groups, and preview limits. Enter
is the browser's own link activation, so the proof observes that focus rests on
the entry's link to the notebook or group route. Accept the title deletion with
a one-time reading of the affected markup and references, apart from the
behavior checks; keep the existing filtering and autofocus proof.

**Effort hypothesis:** S–M (about 1–2 hours), medium-high confidence after
observing link order and focus behavior in the rendered catalog.

**Depends on:** none.

**Safe stopping point:** Keyboard filtering, Up/Down traversal, and Enter
activation work in both layouts; group Left/Right follows as its own slice.

## Breadcrumbs

- Owner request, 2026-10-27: add one backlog story; existing autofocus needs no
  change; simplify the filter title; enable Up/Down and Enter without wrapping;
  propose group and Left/Right behavior.
- Owner follow-up, 2026-10-27: accepted the keyboard proposal, moved this story
  to the top of the product backlog, and authorized landing the preparation.
- Refinement with UX/UI and architecture focus, 2026-10-27: confirmed the
  agreed interaction; no interaction choice with architectural consequences
  stays open. Observed in this workspace that the rendered catalog's entry
  links follow the reading order in the grid layout with a group and that
  focusing one makes it the active element. Deferred: return-to-filter on
  typing or Escape, and the Circle page's filter label.
