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
note's frontmatter from its property row, whether the property panel is closed
or expanded.

## Story

<a id="story-1"></a>

### Follow wiki links in property lists from both property views

**Identity:** SEED-065#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/001-follow-property-list-wiki-links/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"8f62adfcadcb76a14f898a89b4e0a6780387d2a8217fe73c793500e4de75653f","plan":"ff1021340906170cca6d26784a428d78caf247e2c9a93d5cb6ec760bb9543f14"}}
```

- **Goal:** note readers can navigate to a referenced note by clicking a
  wiki-link element in a frontmatter property list, without copying its target
  into search or switching to Markdown to find it.
- **Scope:** render wiki-link list elements as clickable wiki links in both
  the normal first-layer property view and the property view with its chevron
  panel expanded. Apply the existing wiki-link navigation behavior in each
  view, including when a list mixes wiki links with plain-text values.
  Plain-text elements retain their normal presentation. The pencil value
  dialog is for editing; clickable links there are not a delivery commitment.
- **Key examples:**
  - A property contains `["[[Grammar]]", "practice"]` → view the property in
    the normal first layer → `Grammar` is a clickable wiki link that opens its
    referenced note; `practice` remains plain text.
  - Open that property's chevron panel → `Grammar` remains a clickable wiki
    link in the property row with the same navigation behavior.
  - A property contains several wiki-link elements → view it in either
    presentation → each wiki-link element can be followed independently.
- **UI:** in the normal property row, readers can distinguish each wiki-link
  element from plain text and activate the individual link by pointer, touch,
  or keyboard using the existing wiki-link interaction. Existing authored
  display text and target semantics apply. Following a link is a navigation
  action; editing its value and opening property actions remain distinct tasks.
  Existing wiki-link status and recovery behavior apply where the view already
  provides them; this story does not define a new target-resolution workflow.
- **Deferred promises:** clickable links in the pencil value dialog,
  redesigning property editing or wiki-link syntax, and introducing new
  target-resolution workflows. These are not rejection rules for values
  already supported by the product.
- **Depends on:** none.
- **Effort hypothesis:** S; implementation planning will confirm the shared
  rendering boundary and proof needed for both views.

## Breadcrumbs

- Owner request, 2026-10-02: capture this story at the top of the product
  backlog; every wiki-link list element should be clickable in both views.
- Owner clarification, 2026-10-02: the pencil dialog is for editing; its list
  items need not be clickable links.
