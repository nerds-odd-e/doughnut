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

<a id="story-1"></a>

### Continue to a neighboring note after deletion

**Identity:** SEED-047#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/014-continue-to-neighboring-note-after-deletion/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"5938f250db05851f800526e01b2c2f9f020e3fe96818a44f2026c402724ba67c","plan":"e57e809e6ebe31931856ca22c3f5e31288b1c743196c5ab282a97a1b7f6301bf"}}
```

**Goal:** A person deleting notes one after another in a folder keeps their
place: after each deletion they land on the neighboring note in the order they
chose for the sidebar, instead of on the folder page while notes remain.

**Scope:**

- Applies to both steps of the note delete action: moving a note to trash and
  permanently deleting a note that is already in trash.
- The neighbors are the other notes in the same folder (or the notebook root
  for a note outside folders), ordered by this browser's current sidebar sort
  choice (title, created, or updated; ascending or descending), the same order
  the sidebar shows.
- After a successful deletion, open the note that followed the deleted note in
  that order. If the deleted note was last, open the note that is now last.
- If no other note remains, keep today's destination: the folder page, or the
  notebook page for a note at the notebook root.
- Subfolders and files listed beside the notes are not neighbors; they are
  skipped when choosing the destination.
- Assumption: "node" in the owner request means note; the product has no node
  concept (ADR 0001).
- Deferred: deleting folders or files, moving notes, undo, and reducing a
  relationship note into a property keep their current navigation.

**Key examples:**

- Notes A, B, C in the selected order: deleting B opens C.
- Notes A, B, C in the selected order: deleting C opens B.
- Only note A, beside a subfolder or file: deleting A opens the folder page.
- With the sort switched to Title (Z–A) — C, B, A — deleting B opens A.
- In trash, permanently deleting B among A, B, C opens C.

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
[SEED-047#story-1](#story-1); adds no feature promise.

**Scope:** frontend only; the landing destinations of story 1 stay unchanged.
Plan: [015-removal-landing-listing-load](../slice-plans/015-removal-landing-listing-load/PLAN.md).

## When to Surface

Select this story from the product backlog when improving note browsing after
deletion.

## Breadcrumbs

- Owner request, 2026-09-27: after deleting a node, redirect to its next node
  or the folder's last node according to the current user's browser ordering;
  open the folder page only when no node remains.
