---
id: SEED-047
status: dormant
planted: 2026-09-27
planted_during: owner-requested backlog capture
trigger_when: selected from the product backlog
scope: small
---

# SEED-047: Continue browsing after deleting a node

## Why This Matters

After deleting a node, Donut currently opens its folder page. A person working
through nodes in that folder loses their place. The next useful destination
depends on the node order they selected in their own browser.

## Story Decomposition

<a id="story-1"></a>

### Continue to a neighboring node after deletion

**Identity:** SEED-047#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

**Goal:** A person deleting a node can continue browsing the folder's nodes in
the order currently selected in their browser, without returning to the folder
page while nodes remain.

**Scope:** After a successful deletion, navigate to the deleted node's next
node in the current browser-selected order. If the deleted node was last in
that order, navigate to the last remaining node in the folder. If no nodes
remain, show the folder page. The destination is based on this user's current
UI ordering selection, not a fixed or shared ordering.

**Key examples:**

- With nodes A, B, C in the selected order, deleting B opens C.
- With nodes A, B, C in the selected order, deleting C opens B.
- With only A in the folder, deleting A opens the folder page.
- If the user changes the selected order, the next deletion follows that new
  order.

## When to Surface

Select this story from the product backlog when improving node browsing after
deletion.

## Breadcrumbs

- Owner request, 2026-09-27: after deleting a node, redirect to its next node
  or the folder's last node according to the current user's browser ordering;
  open the folder page only when no node remains.
