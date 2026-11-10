---
id: SEED-119
status: active
planted: 2026-11-10
scope: story
---

# Highlight and link to a specific property item

<a id="highlight-and-link-to-a-specific-property-item"></a>

### Highlight and link to a specific property item

**Identity:** SEED-119#highlight-and-link-to-a-specific-property-item
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal.** A learner can focus a particular item in a note's list property,
return to it from a direct link, and remove an individual item while
keeping the other values.

**Scope.** Opening a list property with its chevron on the note page
highlights the first item in the displayed order. Clicking empty space in
an item row moves the highlight to that row. The value link and the existing
row actions keep their own click behavior. A highlighted list item has a
direct note URL. Opening or reloading an item URL expands the property and
highlights the addressed item. Assimilation opens the property with the
item it targets highlighted. Other surfaces that know the exact item can
link directly to it. Scalar properties retain their existing focus behavior,
and links that identify only a property still open that property.

Each expanded list item has its own minus button. Pressing it removes only
that item. The other values and their order are preserved.

**UI decision.** Use the **Highlighted row** design in the existing note
page. Show the tint and leading edge on the item row within the expanded
property. The property header and sibling items use their normal styling.
Keep the existing chevron, property edit control, value links, and
assimilation or tracker actions. Place a minus button at the start of each
item row. The highlight is the only visible focus cue.
[Interactive mockup](../notes/property-item-highlight-in-note.html)

**Key examples.**

- A note has `[[walk]]`, `[[run]]`, and `[[jog]]` under `example of` in that
  order. Expanding the property highlights `[[walk]]`. Clicking empty space
  in the `[[run]]` row moves the highlight to `[[run]]` and updates the URL.
- Opening or reloading the URL for `[[run]]`, or starting assimilation for
  that item, opens the list with `[[run]]` highlighted.
- Pressing minus beside `[[run]]` leaves `[[walk]]` and `[[jog]]` in the
  same order under `example of`.

**For refinement.** Settle focus after removing the highlighted item and
what happens to the property when its final item is removed. The mockup
leaves the panel open and clears a removed item's highlight.

**Routing constraint.** Use named navigation within the note route family
and preserve the authored property key. Settle the item selector and URL
shape during refinement. [ADR 0005 — Web routes](../../docs/adrs/0005-web-routes-accepted.md)
