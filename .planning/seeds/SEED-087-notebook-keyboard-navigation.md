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
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** A notebook reader can filter the notebooks page, reach the desired
visible notebook with the keyboard, and open it with Enter.

**Requested scope:**

- Remove the redundant visible "Filter notebooks" title and its presentation
  markup. Keep the descriptive placeholder and a programmatic accessible name
  for the filter input.
- From the filter input, Down focuses the first visible catalog entry. Further
  Down presses move forward; Up moves backward. Navigation uses the current
  filter and displayed sort order.
- From the first visible entry, Up returns focus to the filter. Repeated Up
  presses from the last entry therefore walk back through the entries to the
  filter. Up at the filter stays there; Down at the last entry stays there.
- Enter on a focused notebook opens that notebook through its normal link,
  including a subscribed notebook or a visible group member.
- Show a clear focus indicator and scroll the focused entry into view.

**Agreed UX:**

- Treat each group header as an entry, followed by its visible notebook
  previews, then the next catalog entry. Enter on the header opens the full
  group page, including members beyond the preview limit.
- Right on a group header focuses its first visible member. Left on a grouped
  notebook focuses its group header. With no visible members, Right keeps focus
  on the header. Other Left/Right presses on catalog entries keep focus where
  it is. This does not introduce group expansion or collapse.
- Use the same logical reading sequence in list and grid layouts: left to
  right within each grid row, then the next row. Up/Down follow that sequence;
  Left/Right retain their group-entry and parent-return meaning.
- Within the filter, Left/Right retain normal text-cursor movement. Tab and
  Shift+Tab retain access to links and secondary controls; arrow shortcuts act
  on catalog navigation targets rather than menus or editing controls.

**Key examples:**

1. **Filter to notebook:** With a matching notebook visible, type its name in
   the filter, press Down, then Enter. Focus moves to that result and its
   notebook opens.
2. **Walk back:** With visible notebooks A, B, C and focus on C, successive Up
   presses focus B, A, then the filter. A further Up leaves the filter focused.
3. **Bottom boundary:** With C as the last visible entry and focused, Down
   keeps C focused.
4. **Filter and sort:** Change the filter or catalog sort, then press Down
   from the filter. Focus follows the first result in the current display.
5. **No matches:** With a filter producing zero results, Down leaves focus in
   the input so the reader can edit the query or clear it.
6. **Group journey:** With group G showing A and B, Down traverses
   G, A, B, and the next catalog entry. Enter on G opens G's page; Enter on A
   opens A. Right on G focuses A, and Left on B returns to G.
7. **Preview boundary:** A group has more notebooks than its preview
   shows. Arrows visit its displayed members; Enter on its header opens the
   page containing all members. An empty group's header can also be opened.
8. **Filter remains usable:** The filter has an accessible name, accepts text,
   filters the catalog, and supports ordinary cursor movement with Left/Right.

**Evaluation:** Observe focus and navigation through the mounted notebooks
page, including both layouts, filtered results, subscriptions, and group
previews under the agreed UX. Accept the visible-title deletion with a one-time
reading of affected markup and dependent references, separately from behavior
checks; retain proof of filtering and accessible input use.

**Effort hypothesis:** M (1–2 hours), medium confidence; confirm focus ownership
across catalog rows, cards, and group previews during implementation planning.

**Depends on:** none.

**Safe stopping point:** Keyboard filtering and notebook activation work in the
existing catalog with the agreed group navigation.

## Breadcrumbs

- Owner request, 2026-10-27: add one backlog story; existing autofocus needs no
  change; simplify the filter title; enable Up/Down and Enter without wrapping;
  propose group and Left/Right behavior.
- Owner follow-up, 2026-10-27: accepted the keyboard proposal, moved this story
  to the top of the product backlog, and authorized landing the preparation.
