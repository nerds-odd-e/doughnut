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

**Goal.** A learner can focus a particular item in a note's list property
and return to that same item from a direct link.

**Scope.** Let the learner highlight an individual list item and give that
item its own note URL. Opening the URL directly or reloading it opens the
note with the same item highlighted. When assimilation targets an item in a
list property, highlight that item in the note shown for assimilation. Other
surfaces that know the exact item can link directly to it. Keep scalar
properties and links that identify only a property at property-level focus.

**Key examples.**

- A note has `[[walk]]` and `[[run]]` under `example of`. Assimilating
  `[[run]]` highlights the `[[run]]` item. Selecting `[[walk]]` highlights
  that item and gives it a distinct URL.
- Opening the URL for `[[run]]` from elsewhere, or reloading it, lands on the
  same note and highlights `[[run]]`.

**Routing constraint.** Use named navigation within the note route family
and preserve the authored property key. Settle the item selector and URL
shape during refinement. [ADR 0005 — Web routes](../../docs/adrs/0005-web-routes-accepted.md)
