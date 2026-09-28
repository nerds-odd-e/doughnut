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
bar covers it. The owner grouped the defects into the three stories below.

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
- Unverified hypothesis from reading the code:
  - the hint's sticky anchor has zero height, so the hint is drawn over rows
    instead of taking space;
  - the reveal uses `scrollIntoView` with the default `block: 'start'`;
  - the tree scroll area's 0.75rem `scroll-padding-top` is smaller than the
    hint's height.
- Other observations left unselected by the owner:
  - ArrowDown does not move focus within the `role="tree"` sidebar.
  - Far jumps smooth-scroll a long distance (about 25,600px in about 1.5
    seconds in a 1,000-note folder).
  - An uncached deep load builds the tree in 4–5 visible layout shifts.
  - Expanded folders stay open after navigating elsewhere.

## Story Decomposition

Effort bands: S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery.

<a id="story-1"></a>

### Keep the revealed note fully visible below the sidebar path hint

**Identity:** SEED-043#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal**

When the sidebar reveals the current note, users can see and read the whole
selected row, however deep the note sits and however long the list is.

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

**Effort hypothesis:** S, medium confidence.

<a id="story-2"></a>

### Fix clickable rows under the path hint and small shell defects

**Identity:** SEED-043#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal**

Users never meet visible elements that ignore clicks, overlapping rail
decoration, console errors, or wasteful search traffic while navigating.

**Observed defects**

1. **Rows behind the path hint cannot be clicked.**
   - *Expected:* a row that can be seen can be clicked.
   - *Observed:* clicking the visible half of a row under the hint does nothing.
     There is no navigation and no scroll.
   - *Reproduction:* in a long folder, scroll so a note row is half under the
     hint, then click its visible half.
   - *Impact:* users see a row they cannot select until they scroll.
2. **The left-rail count badge overlaps the Assimilate icon.**
   - *Expected:* the badge sits clear of the icon below it.
   - *Observed:* the badge ("15/623", "15/1.6k") covers the top of the
     Assimilate icon.
   - *Reproduction:* log in with notes due, then view a note page at 1280px or
     1440px width.
   - *Impact:* cosmetic. The rail looks unfinished, but the icon stays usable.
3. **Console error at phone width.**
   - *Expected:* no console errors.
   - *Observed:* at 390px, the console logs `<path> attribute d: Expected number,
     "… h --120 …"`, which suggests a malformed SVG path in the mobile layout.
   - *Reproduction:* open any note page with a 390×844 viewport.
   - *Impact:* no visual breakage has been traced to it yet.
4. **Search sends a burst of recent-notes requests.**
   - *Expected:* recent notes load once when search opens.
   - *Observed:* typing one query sent `GET /api/notes/recent` 15 times before
     the single `POST /api/notes/search`.
   - *Reproduction:* open note search, type a query at normal speed, and watch
     network requests.
   - *Impact:* wasted server load and bandwidth; no visible slowness observed
     locally.

**Effort hypothesis:** M, low confidence. These are four independent small
fixes. If one of them grows, split it rather than stretching the story.

<a id="story-3"></a>

### Keep the selected note in view after re-sorting the sidebar

**Identity:** SEED-043#story-3
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal**

After users change the sidebar sort order, the selected note is still where they
can see it.

**Observed defect**

- *Expected:* after the sort order changes, the selected note stays in view.
- *Observed:* with Note 120 selected and revealed in a 150-note folder, choosing
  *Sort sidebar → Title (Z–A)* reorders the list but keeps the old scroll offset.
  The selected row ends up about 2,700px above the visible area.
- *Reproduction:* open a note deep in a long folder, then apply Title (Z–A).
- *Impact:* users lose their place right after reorganising the view.
- It may share the reveal behavior from story 1; it must also keep that story's
  full-row visibility.

**Effort hypothesis:** S, medium confidence.

## Ordering and Scope Reduction

The owner queued these stories in this order after the existing backlog. They are
not refined yet; refine each before planning or execution.

## Open Decisions

- Whether far jumps should reveal instantly or keep smooth scrolling, if story 1 or
  story 3 touches the reveal behavior.

## Breadcrumbs

- Source: the sidebar UX manual UAT findings in
  `.planning/seeds/SEED-042-sidebar-ux-uat.md` at commit `adce721eea`.
