---
id: SEED-043
status: dormant
planted: 2026-09-28
planted_during: owner selection of defects found by the sidebar UX manual UAT
trigger_when: fixing sidebar navigation defects observed in the manual UAT
scope: small
---

# SEED-043: Fix sidebar navigation defects found in manual UAT

## Why This Matters

A one-hour manual UAT of the sidebar (2026-09-28, commit `ee8859e4fc`) found
defects that make navigation feel unstable. The most visible one is the owner's
reported glitch: the sidebar brings the visited note into view, and then a sticky
bar covers it. The owner grouped the defects into the stories below.

The UAT ran in visible headless Chromium on a local E2E stack with seeded notebooks:
- a tree with folders 7 levels deep;
- a 150-note folder between six small folders;
- 40 folders of 10 notes each;
- a 1,000-note folder;
- long titles.

Cached, uncached, and partially cached frontend data behaved the same for every
defect below.

## Context

- Measured rendering cost was small (150 rows expanded in about 6ms; 1,000 rows
  in about 140ms with one long task of about 60ms). The friction is where
  notes land after scrolling, not rendering speed. Progressive loading is not
  indicated by current evidence.
- The sticky bar is the "Ancestor folders scrolled out of view" hint at the top
  of the tree scroll area. It shows a chevron-up and the names of ancestor
  folders scrolled above the top, so it looks like a folder row.
- Confirmed in the code during story 1 refinement:
  - the hint's sticky anchor has zero height on purpose (to avoid show/hide
    flicker), so the hint is drawn over rows instead of taking space;
  - the hint appears only once ancestor folder rows scroll above the top, which
    is exactly what revealing a deep note does, so it lands on the revealed row;
  - the reveal is the generic `ScrollTo` marker, shared with conversations. It
    calls `scrollIntoView` with the default `block: 'start'`, and only when the
    zero-size marker is fully out of view;
  - the tree scroll area's 0.75rem `scroll-padding-top` is smaller than the
    hint's height;
  - the note toolbar breadcrumb already shows the same ancestor folders.
- Other observations left unselected by the owner:
  - ArrowDown does not move focus within the `role="tree"` sidebar.
  - Far jumps smooth-scroll a long distance (about 25,600px in about 1.5
    seconds in a 1,000-note folder).
  - An uncached deep load builds the tree in 4–5 visible layout shifts.
  - Expanded folders stay open after navigating elsewhere.

## Story Decomposition

Effort bands: S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery.

<a id="story-1"></a>

### Keep the revealed note fully visible in the sidebar

**Identity:** SEED-043#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/005-sidebar-full-row-reveal/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"81c538f5dfee464c58713a34918c076f185062232739cdfc949c5f8103e64459","plan":"c60b48ad0127e7878033208c5f24901adf4398f02e62e6d20e324e2ddef6c13a"}}
```

**Goal**

When the sidebar reveals the current note, users can see and read the whole
selected row, however deep the note sits and however long the list is. Changing
the sidebar sort order reveals the selected note the same way, so users do not
lose their place after reordering the list.

**Observed defect**

- *Expected:* after the sidebar brings the visited note into view, the whole row
  is visible and readable.
- *Observed:* the revealed row sits at the top of the tree, under the sticky
  path hint.
  - At 1440×900 in the 150-note folder (Note 120), the row spans 140–172px and
    the hint 128–157px, covering about the top half.
  - At 1280×560 in the 7-level tree (Deep Target Note), the hint wraps to two
    lines (128–177px) and covers the row completely.
- *Reproduction:*
  - Open `/n/<id>` for any note far enough down to need scrolling, or reach it
    in-app through search, Back, or creating a note.
  - Or scroll manually until a note's row is half under the hint, open another
    note, and press Back. The row stays half covered, because a partly visible
    row is not scrolled again.
- *Conditions:* the same for uncached, cached, and partially cached data, in dark
  and light themes, and in the 390px drawer.
- *Impact:* on every far jump, users cannot read which note is selected at the
  moment the sidebar is supposed to show it.

**Observed defect: re-sorting**

- *Expected:* after the sort order changes, the selected note stays in view.
- *Observed:* with Note 120 selected and revealed in the 150-note folder,
  choosing *Sort sidebar → Title (Z–A)* reorders the list but keeps the old
  scroll offset. The selected row ends up about 2,700px above the visible area.
- *Cause:* the reveal runs only when the selected row is first rendered;
  re-sorting moves existing rows without re-rendering them, so nothing reveals
  again.

**Scope**

Decided by the owner: remove the sidebar path hint rather than make room for it.
The note toolbar breadcrumb already shows the same ancestor folders, and the
hint is what covers the revealed row.

- The sidebar no longer shows the "Ancestor folders scrolled out of view" hint.
  Nothing is drawn over the tree rows.
- When the sidebar reveals the selected note, the whole row ends up inside the
  tree's visible area. A row that is only partly visible, cut off at the top or
  bottom edge, counts as not visible and is scrolled fully into view. A row that
  is already fully visible does not move the tree.
- This applies on every way of reaching a note (direct URL, search, Back,
  creating a note), for deep and long folders, at narrow heights, and in the
  390px drawer.
- Changing the sort order from the sidebar reveals the selected note by the
  same rule. The selected note, not the top of the list, is the target: it is
  the sidebar's "you are here", and the old scroll offset means nothing after
  the list is reordered.
- The whole-row rule is the only promise about where the row lands. The row
  may end up at the top edge, the bottom edge, or anywhere in between, as long
  as all of it is visible.
- On a folder page, the active folder row is revealed the same way. It shares
  the reveal and needs no separate check.

Not promised by this story:

- Changing how conversations scroll. The reveal may change for conversations
  too, or stay as it is, whichever is simpler.
- Losing the hint's shortcut to scroll the tree to an ancestor folder row is
  accepted. The breadcrumb still links to each folder's page.
- Losing the only list of ancestor folders in the 390px drawer is accepted.
  The drawer covers the breadcrumb, but the ancestor folder rows are still in
  the tree above the revealed row.
- Instant reveal. Far jumps, including after a re-sort, keep the current
  smooth scrolling.
- Re-sorting when no note is selected (for example on a folder or notebook
  page), when the selected note sits inside a folder the user collapsed, or
  when the sort order is changed outside the sidebar (the Notebooks page or
  another tab).
- Keeping the previously visible rows in place instead of revealing the
  selected note.
- Story 2's defects and the other unselected UAT observations. Removing the
  hint also removes the UAT defect that rows under the hint cannot be clicked;
  story 2 already dropped it.

**Key examples**

- Note 120 in the 150-note folder at 1440×900, opened at `/n/<id>`: the whole
  row is inside the tree's visible area, and no bar covers it.
- Deep Target Note in the 7-level tree at 1280×560: the whole row is visible.
- The tree is scrolled by hand so a note's row is half cut off at the bottom
  edge. Opening that note (or returning to it with Back) scrolls the tree just
  enough to show the whole row.
- A note whose row is already fully visible is opened: the tree does not scroll.
- Note 120 is selected and revealed in the 150-note folder; choosing *Sort
  sidebar → Title (Z–A)* reorders the list and the whole Note 120 row is
  visible.
- A selected note whose row stays fully visible after a re-sort (every row of a
  short folder fits): the tree does not scroll.
- Any note page with ancestor folders: the breadcrumb still shows those folders;
  the sidebar shows no path hint.

**Effort hypothesis:** M, medium confidence. Removing the hint is mostly
deletion; the whole-row rule and revealing again after a re-sort are two small
behaviors on the same reveal.

## Ordering and Scope Reduction

The owner queued these stories in this order after the existing backlog. Refine
each before planning or execution. Story 1's removal of the path hint also
removed story 2's original first defect. The re-sorting defect was folded into
story 1 because it is the same reveal rule triggered by reordering.

## Breadcrumbs

- Source: the sidebar UX manual UAT findings in
  `.planning/seeds/SEED-042-sidebar-ux-uat.md` at commit `adce721eea`.
