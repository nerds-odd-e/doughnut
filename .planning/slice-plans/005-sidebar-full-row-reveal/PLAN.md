# Keep the revealed note fully visible in the sidebar

## Source

- Story: [Keep the revealed note fully visible in the sidebar](../../seeds/SEED-043-sidebar-uat-defects.md#story-1)
- **Identity:** SEED-043#story-1

## Goal and scope

When the sidebar reveals the selected note, including after the sidebar sort
order changes, the whole row ends up inside the tree's visible area and nothing
is drawn over it. The "Ancestor folders scrolled out of view" hint is removed.

Excluded (see the seed): instant reveal (smooth scrolling stays), any promise
about where the row lands beyond "whole row visible", changes to conversation
scrolling, re-sorting with no selected note, inside a collapsed folder, or from
outside the sidebar, and the other UAT observations.

## Approach

One rule, one owner: **an active sidebar row reveals itself by scrolling the
least amount that shows the whole row** (`scrollIntoView({ block: "nearest",
behavior: "smooth" })` on the row). The browser already makes that rule do
nothing for a fully visible row, move a cut-off row just enough, and bring a
far row to the nearest edge. The existing `scroll-padding` on
`.sidebar-tree-scroll` keeps a small gap at either edge.

- The hint (`SidebarNotebookTreeScrollportPathHint.vue`), its helper
  `sidebarActivePathRowsAboveScrollport.ts`, its spec, its only-for-the-hint
  `data-sidebar-folder-row` attribute, and its `components.d.ts` entry are
  deleted. The note toolbar breadcrumb already shows the same folders.
- Sidebar note and folder rows stop using the generic `ScrollTo` marker (a
  zero-size element above the row, checked against the window, scrolled with
  `block: "start"`). One small sidebar composable owns the reveal: it runs when
  the row becomes active and, while active, after the shared peer-sort spec
  changes (post-flush, so rows are already reordered). `ScrollTo` then has only
  the conversation caller and stays as it is.
- No North Star topic or ADR governs sidebar scrolling; the change stays
  inside the sidebar components.

Expected complexity delta: about −200 lines of product code and one spec file
removed with the hint; one composable of roughly 20 lines added.

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| The existing sidebar, hint-helper, and conversation specs are green on trunk | `CURSOR_DEV=true nix develop -c bash -c 'cd frontend && pnpm vitest run tests/notes/sidebar tests/components/notes/sidebarActivePathRowsAboveScrollport.spec.ts tests/components/conversation/ConversationInner.spec.ts'` | 11 files, 38 tests passed (real Chromium, browser mode) |
| A Sidebar spec without `installSidebarDomMeasurementStubs` gets real layout: a scrollable tree and measurable row positions | Temporary spec (deleted): 40 root notes, style rule `.sidebar-tree-scroll{height:300px;flex:none}`, active Note 30, wait 1.5s | Tree `clientHeight` 300, `scrollHeight` 1280, `scrollTop` 916; row at 76–108px in a tree at 64–364px (today's reveal lands it 12px, the scroll padding, below the top) |
| `block: "nearest"` gives the story's whole-row rule in this layout | Same temporary spec: set `scrollTop` 644 so the row spans 348–380px against the tree's 364px bottom, call `row.scrollIntoView({ block: "nearest" })` twice | First call moves `scrollTop` to 668 (row 324–356px, 8px scroll padding from the bottom); second call does not move it |
| `ScrollTo` callers | `grep -rn "import ScrollTo" frontend/src` | `SidebarNoteItem.vue`, `SidebarFolderItem.vue`, `ConversationInner.vue` |
| Tests tied to today's sidebar reveal | `grep -rn "scrollIntoView\|IntersectionObserver" frontend/tests/notes/sidebar` | `SidebarFolderItem.spec.ts` (active folder scrolls when not intersecting, not when intersecting), `SidebarFirstGeneration.spec.ts:33` (no scroll when intersecting), both via `stubIntersectionObserver` |
| The hint has no other users or E2E coverage | `grep -rn "ScrollportPathHint\|scrollport-path-hint\|ActivePathRowsAboveScrollport\|data-sidebar-folder-row" frontend/src frontend/tests e2e_test` | Only the hint, `Sidebar.vue`, `SidebarFolderItem.vue:16` (the attribute), `components.d.ts`, and the helper spec |
| Re-sorting reorders existing rows and the sort spec is one shared value | `SidebarInner.vue:104` sorts rows from `usePeerSort().peerSortSpec`; `usePeerSort.ts:44` is a module-level `ref` with no storage-event listener | A post-flush watch on that spec sees every sidebar sort change after the rows move |

## Proof entry point

A new real-layout spec, `frontend/tests/notes/sidebar/SidebarRowReveal.spec.ts`,
mounts `Sidebar` through `mountSidebarSignedIn` without the DOM measurement
stubs. It fixes the tree height with a style rule, as in the probe. It
asserts the active row's rectangle is inside `.sidebar-tree-scroll`'s
rectangle, polling with `vi.waitFor` because scrolling stays smooth. The
fixture is one folder of 40 notes ("Note 01"… "Note 40") under the notebook
root, with the active note's `ancestorFolders` passed as breadcrumbs, so the
old hint would have appeared.

## Slices

### 1. The sidebar no longer draws a path hint over the revealed note
Type: Behavior
Status: planned
Proof: `SidebarRowReveal.spec.ts` (new), plus the existing
`tests/notes/sidebar` specs, run as in the premises table.

Behavior:
- Note 30 in the 40-note folder is opened with a 300px tree → the whole row is
  inside the tree area, `document.elementFromPoint` at the row's top-left
  inner point hits the row, and there is no
  `[aria-label="Ancestor folders scrolled out of view"]` element.
- Delete the hint, its helper and spec, the `data-sidebar-folder-row`
  attribute, the `Sidebar.vue` usage, and the `components.d.ts` entry.

### 2. A partly visible selected row scrolls just enough to show all of it
Type: Behavior
Status: planned
Proof: extend `SidebarRowReveal.spec.ts`; run the `tests/notes/sidebar` and
`tests/components/conversation` specs.

Behavior:
- Note 30 is active and revealed; the tree is scrolled so Note 31's row is cut
  off at the bottom edge; Note 31 becomes the active note → the whole Note 31
  row is visible, and `scrollTop` moved by less than one row height.
- The same with the row cut off at the top edge → the whole row is visible.
- The tree is scrolled so Note 25's row is fully visible; Note 25 becomes
  active → `scrollTop` does not change.
- Replace `ScrollTo` in `SidebarNoteItem.vue` and `SidebarFolderItem.vue` with
  the sidebar reveal composable. The active folder row shares it and gets no
  separate check (seed). Remove the `stubIntersectionObserver` scroll
  assertions from `SidebarFolderItem.spec.ts` (keep its expansion assertion)
  and `SidebarFirstGeneration.spec.ts:33`, and the stub helper if unused;
  the real-layout cases above replace them. `ConversationInner.spec.ts` stays
  green unchanged.

### 3. Re-sorting the sidebar reveals the selected note
Type: Behavior
Status: planned
Proof: extend `SidebarRowReveal.spec.ts`, choosing the sort through the
toolbar menu as `SidebarPeerSort.spec.ts` does
(`[data-note-sidebar-sort] summary`, then the Title (Z–A) row); run with
`SidebarPeerSort.spec.ts`.

Behavior:
- Note 30 is active and revealed in the 40-note folder; choose Title (Z–A) →
  the rows are reordered and the whole Note 30 row is visible.
- A short folder whose rows all fit in the tree, with its note active; choose
  Title (Z–A) → `scrollTop` does not change.

## Considered and excluded

- An E2E scenario: the defect and the fix live entirely inside the `Sidebar`
  component, and the real-layout spec runs it in real Chromium with its real
  styles. An E2E scenario would need new steps for bulk notes, sorting, and
  row visibility for no extra observation.
- Changing `ScrollTo` itself: its marker is zero-size and conversations want
  "scroll to the end" semantics, so the whole-row rule would not fit there.
