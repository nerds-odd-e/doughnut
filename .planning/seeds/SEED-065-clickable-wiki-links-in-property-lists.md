---
id: SEED-065
status: dormant
planted: 2026-10-02
planted_during: owner request to capture clickable wiki links in frontmatter property lists
trigger_when: improving navigation from note properties
scope: small
---

# SEED-065: Navigate wiki links in frontmatter property lists

## Why This Matters

Readers should be able to follow a wiki link stored as a list element in a
note's frontmatter wherever that property list is displayed.

## Story

<a id="story-1"></a>

### Follow wiki links in property lists from both property views

**Identity:** SEED-065#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

- **Goal:** note readers can navigate to a referenced note by clicking a
  wiki-link element in a frontmatter property list.
- **Scope:** render wiki-link list elements as clickable wiki links in both
  the normal first-layer property view and the expanded list view. Apply the
  existing wiki-link navigation behavior in each view, including when a list
  mixes wiki links with plain-text values. Plain-text elements retain their
  normal presentation.
- **Key examples:**
  - A property contains `["[[Grammar]]", "practice"]` → view the property in
    the normal first layer → `Grammar` is a clickable wiki link that opens its
    referenced note; `practice` remains plain text.
  - Expand that property's list → `Grammar` remains a clickable wiki link
    with the same navigation behavior.
  - A property contains several wiki-link elements → view it in either
    presentation → each wiki-link element can be followed independently.
- **Depends on:** none.
- **Effort hypothesis:** S; implementation planning will confirm the shared
  rendering boundary and proof needed for both views.

## Breadcrumbs

- Owner request, 2026-10-02: capture this story at the top of the product
  backlog; every wiki-link list element should be clickable in both views.
