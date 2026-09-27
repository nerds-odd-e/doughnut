---
id: SEED-047
status: dormant
planted: 2026-09-27
planted_during: owner-requested backlog capture
trigger_when: selected from the product backlog
scope: small
---

# SEED-047: Continue browsing after deleting a note

## Why This Matters

After deleting a note, Donut currently opens its folder page. A person working
through notes in that folder loses their place. The next useful destination
depends on the note order they selected in their own browser.

## Story Decomposition

<a id="story-2"></a>

### Removal landing loads the folder listing like other user actions

**Identity:** SEED-047#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/015-removal-landing-listing-load/PLAN.md"}
```

**Goal:** A person trashing or permanently deleting a note sees the app busy
from confirmation until they land, as for other user-triggered folder
listings, and the "load a folder listing" and "listing to sidebar rows" rules
each live in one place. Bounded retrospective correction of
SEED-047#story-1 (`edfd7ba92a:.planning/seeds/SEED-047-deleted-node-navigation.md`); adds no feature promise.

**Scope:** frontend only; the landing destinations of story 1 stay unchanged.
Plan: [015-removal-landing-listing-load](../slice-plans/015-removal-landing-listing-load/PLAN.md).

## When to Surface

Select this story from the product backlog when improving note browsing after
deletion.

## Breadcrumbs

- Owner request, 2026-09-27: after deleting a node, redirect to its next node
  or the folder's last node according to the current user's browser ordering;
  open the folder page only when no node remains.
