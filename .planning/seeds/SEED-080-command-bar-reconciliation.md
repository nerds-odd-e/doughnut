---
id: SEED-080
status: dormant
planted: 2026-10-20
planted_during: owner review of the deployed cohesive note workspace
trigger_when: restoring navigation continuity and finishing the deployed command bar
scope: medium
---

# SEED-080: Stable sidebar location and aligned tools

## Why This Matters

Authors need a stable notebook and folder location while browsing, with
contents tools aligned with note commands and a breadcrumb that stays visible
when the contents tree scrolls.

## Alternatives and Decision

The owner selected the “Sidebar header” design on 2026-10-20: align the
contents and note toolbars, and put notebook/location above the sidebar tree.
The stable breadcrumb replaces the separate scroll-dependent ancestor display.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours including delivery.
This seed is non-executable planning input.

<a id="story-2"></a>

### Navigate from a stable sidebar breadcrumb with aligned tools

**Identity:** SEED-080#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/002-sidebar-location/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"f64b98d359548151fc48d82e302f047c080f91401fce4937bb3a024e6cf9183a","plan":"b0f501bedd6fb5e03b04f02ce2ef38dca7fca23dd5c1a909ff9eb5636e4a1fc9"}}
```

**Goal:** An author can see where the current item belongs and navigate its
notebook and ancestors without a crowded note header or a path that changes
when the contents tree scrolls.

**Scope — required:**

- Implement the approved “Sidebar header” design. With the sidebar docked,
  align its New note, New folder, and Sort controls with the note toolbar in
  the same top-bar row. Keep the pane divider aligned with the sidebar edge,
  retain the independent sidebar toggle, and preserve the active navigation
  control. Note tools remain right-aligned and use their
  existing adaptive overflow.
- Put the notebook name and a compact navigable folder breadcrumb immediately
  above the sidebar tree, outside its scrollport. This location header stays
  visible while the tree scrolls; it takes the place of the breadcrumb sharing
  the note-command row. Show notebook identity above the folder trail as in
  the approved design. Keep complete ancestry reachable for long names and
  deep nesting through an accessible location disclosure.
- Preserve notebook-root and ancestor-folder navigation, including circle or
  Bazaar context where applicable. Use the current notebook/location on note,
  folder, notebook-root, and attachment routes. At the root, notebook identity
  remains useful with an empty folder trail.
- With the sidebar hidden, expose the same compact location above the main
  content and make the full navigable ancestry reachable. On phones, keep the
  familiar main/contents controls and note-command bar; place contents
  commands and the same location header in the opened drawer. Avoid a second
  location row in the main pane while the docked sidebar already presents it.
- Adopt the owner’s deletion decision for the separate scroll-dependent
  collapsed-ancestor display. Its component, exclusive measurement/listener
  logic, tests, styles, and generated declarations are deletion scope. The
  current base already satisfies this removal, as the dated reading below
  establishes; there is no new removal implementation to perform on that
  base. The replacement is the stable breadcrumb above the tree.
- Preserve normal ancestor expansion, manual folder toggling, selected-item
  highlighting, and revealing the active row with the smallest necessary
  scroll after navigation or sorting. Preserve tree loading/cache behavior,
  notebook-root scroll reset, and current context during pending route loads.
  Creation/sort controls keep their existing destination scope and access
  rules when their presentation moves.

**Scope — deferred:** Changes to command-menu/search/Resume behavior, new
tree navigation capabilities, note content, title editing, reading width,
and details disclosure are deferred.

**Key examples:**

| Context and trigger | Observable result |
| --- | --- |
| Open a note in a nested folder on desktop or iPad with contents docked | Contents and note commands share a baseline; the sidebar header identifies the notebook and parent trail, and the note toolbar has its own available space. |
| Scroll until the active item’s parent rows leave the visible tree | The header breadcrumb remains readable and its ancestors remain navigable while the tree scrolls beneath it. |
| Navigate to another note/folder, or sort the contents | The location follows the selected item; normal ancestor expansion and active-row reveal keep that item discoverable. |
| Close contents, then open the compact location control above the note | The complete path and its notebook/ancestor navigation remain reachable. |
| Open contents on a phone | The drawer presents its contents commands, stable location header, and independently scrolling tree; the main header retains familiar navigation and note actions. |
| Open a notebook root, an attachment, a circle notebook, or a read-only Bazaar notebook | Location reflects the appropriate realm, retaining root/circle/Bazaar navigation and existing permissions. An empty folder trail still presents notebook identity. |
| Use a long notebook name or deeply nested path at a narrow width | The compact header fits without squeezing note commands; the complete named path is available through the location disclosure. |

**Architecture:** Reuse the existing notebook realm and folder
trail derived by the workspace layout for breadcrumb, tree context, and
creation scope. Treat location as one derived domain concept presented in the
sidebar header or compact fallback, rather than copying route classification
or ancestry state into each surface. Keep sidebar command ownership distinct
from note command ownership inside the shared layout. Preserve pending-load
context; its existing sticky-realm helper serves loading continuity, not the
deleted scroll-dependent display. No new persisted model is needed.

Follow [ADR 0001 — Ubiquitous language](../../docs/adrs/0001-ubiquitous-language.md#notebook--note-structure)
and the North Star's **One notebook tree**: folder ancestry is notebook
location, not note ancestry or a second navigation hierarchy. Preserve named
locations and the shared note/property route family under
[ADR 0005 — Web routes](../../docs/adrs/0005-web-routes-accepted.md).
Existing loading/error ownership stays under
[ADR 0006 — Failure handling](../../docs/adrs/0006-failure-handling-accepted.md#usage).
No Accepted-ADR conflict was found. Index and in-file statuses agree; ADR 0001's
missing `-accepted` filename suffix is a hygiene mismatch, not a status change.
The existing breadcrumb also serves recall and read-only note context; the
workspace's compact presentation must reuse its navigation meaning without
imposing workspace layout on those consumers.

**UI:** Notebook identity sits above the compact folder trail. A named,
keyboard- and touch-operable location disclosure makes the complete names and
all notebook/ancestor links available, including circle or Bazaar context.
Opening it preserves the current page; selecting a destination navigates there.
Closing it returns to the same location control. At notebook root, identity
and realm navigation remain meaningful without folder segments. The same
location is presented above main content when contents are hidden, and in the
opened phone drawer. These interactions implement the already approved design;
no new layout alternative or scope decision is introduced.

**Evaluation:** Outside-in evidence for stable navigable location during tree
scroll, ancestor navigation, hidden-sidebar/phone access, and current context
across the supported route families. Reuse retained tree reveal/loading
coverage. Visually check toolbar baselines, pane boundaries, long paths, and
adaptive overflow on phone, iPad, and desktop. Record the one-time removal
sweep as a reading separate from these behavioral proofs; add no permanent
absence test or checker for the deleted display.

**Effort hypothesis:** S — approximately 45–60 minutes, medium confidence,
assuming reuse of existing location derivation, sidebar commands, and
breadcrumb navigation. Header allocation at intermediate widths is the main
sizing uncertainty.

**Depends on:** None. Sharing header components is not a blocking prerequisite.

**Safe stopping point:** The sidebar location and aligned controls are useful
independently, with normal tree navigation and closed-sidebar access intact.

[Dated investigation and recovery evidence](SEED-080-command-bar-context.md).


## Open Decisions

None. The owner selected the Sidebar header design and deletion of the
separate scroll-dependent ancestor display.
