---
id: SEED-080
status: dormant
planted: 2026-10-20
planted_during: owner review of the deployed cohesive note workspace
trigger_when: restoring navigation continuity and finishing the deployed command bar
scope: medium
---

# SEED-080: Recover navigation continuity in the cohesive command bar

## Why This Matters

The owner’s iPad review shows that the new popup menu hides an important part
of the old collapsed navigation: the current activity and direct Resume
action. The command bar also needs the intended alignment, boundaries, and
menu spacing. These corrections help authors move between notes and learning
without losing their place.

## Alternatives and Decision

Keep the popup menu and the shared responsive command bar. Recover the
historical active navigation presentation and action semantics, reconciling
them with the current implementation rather than rebuilding their rules.
The owner now chooses a search-shaped launcher for the common search dialog
instead of search results embedded in the menu. This supersedes the earlier
embedded-search and fixed-launcher design choices.

The owner explicitly groups the understood command-bar recovery changes into
the first story. After reviewing the alternatives, the owner selected the
“Sidebar header” design on 2026-10-20 for the second story: align the contents
and note toolbars, and put notebook/location above the sidebar tree. The
breadcrumb replaces the separate scroll-dependent ancestor display.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours including delivery.
This seed is non-executable planning input.

<a id="story-1"></a>

### Recover active navigation and finish command-bar interactions

**Identity:** SEED-080#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

**Goal:** A signed-in author can identify the current activity, resume an
interrupted recall directly, and open familiar search from a clear, spacious
command menu, consistently on phones, iPads, and desktop windows.

**Scope — required:**

- Recover the historical collapsed active-item selection and direct-action
  behavior from Git. The top-left control shows the current navigation
  item’s existing icon and applicable badge, with an accessible name; a
  visible caption is optional. Use Note for notebook/note browsing, Recall
  for recall, and Assimilate when actively assimilating. Reconcile that last
  context with today’s note/property assimilation presentation rather than
  reviving an obsolete route. Preserve the other existing navigation
  destinations, fallback, and signed-out entry behavior.
- Preserve the existing Resume precedence, green active background, badge
  semantics, and exact paused/progress eligibility rules. Activating Resume
  invokes the existing resume action directly, including its input-focus
  preparation. Current-context Assimilate retains its existing direct
  assimilation action and destination handling. Ordinary active-page context
  opens the menu. Keep an explicit, accessible way to open the menu while the
  primary control is either direct action, as the historical adjacent
  chevron did.
- Right-align the adaptive note toolbar within its available header space.
  Retain the current action order, measured overflow, pinned busy/open
  actions, and ellipsis behavior as width changes. Add subtle dividers
  between navigation and note-command groups and below the command bar to
  distinguish controls from content.
- Give the popup comfortable padding and row spacing, including additional
  vertical separation before the “Donut by” logo footer. Fit narrow viewports
  and keep all commands reachable when vertical space is limited.
- Present an empty search-shaped field and a search button in the menu.
  Activating either closes the menu and opens the same common global search
  dialog, ready for input. Ctrl/Cmd+F opens that dialog directly. Recover the
  historical popup interaction and touch keyboard preparation while reusing
  the surviving general note/folder search behavior. The apparent field is a
  launcher: it must have truthful accessible semantics and a clear focus
  indicator. Remove the embedded-menu search presentation and transitively
  unused presentation-only code; shared search capabilities remain reusable.

**Scope — deferred:** Sidebar toolbar relocation/alignment and breadcrumb
placement belong to [the sidebar location story](#story-2). Changes to the
note reading/details layout are deferred. Note titles remain authored text,
including any dates, and title/body editing stays inline.
This delivery promises current-context Assimilate identification; extending
unfinished assimilation priority across unrelated routes is deferred.

**Key examples:**

| Context and trigger | Observable result |
| --- | --- |
| Browse a notebook or note, then activate the Note context control | The existing Note icon identifies the context; the popup opens with the normal destinations. |
| Enter the current assimilation presentation or recall page | The context identifies Assimilate or Recall using the existing icon/count semantics; an eligible Resume still takes precedence. |
| Pause on an answered recall question with loaded trackers, then browse a note | A green Resume control takes priority over Note. Activate it to continue the existing recall session at its preserved cursor. |
| Leave recall after progressing beyond the first item with remaining work | Resume remains directly actionable; the adjacent menu opener independently opens navigation. |
| Leave the first item unpaused, finish the unpaused queue, or pause without loaded trackers | The ordinary active-page context is shown according to the historical eligibility rules. A paused cursor with loaded trackers follows its existing rule even when the remaining-count branch differs. |
| Activate either part of the search-shaped launcher, or use Ctrl/Cmd+F | The common global search dialog receives input focus and provides the existing note/folder search journey. Touch activation supports entering text with the software keyboard. |
| Resize a note from desktop to iPad to phone with an active toolbar operation | Commands stay right-aligned and accessible through existing adaptive overflow, with busy/open controls retained and visible group/content boundaries. |
| Open the menu on an iPad or short phone viewport | Navigation, search launcher, and footer have comfortable separation; overflow remains reachable. |

**Implementation cohesion:** Keep navigation identity, activity precedence,
counts, and action classification in the existing shared navigation model.
Reconcile its presentation in the compact launcher and popup; do not create a
second route/state classifier or parallel recall/assimilation flags. Preserve
the ownership of note actions and their state when changing header alignment.
Use one common search-dialog entry path for both launcher targets and the
keyboard shortcut. Follow the existing vocabulary and accepted architectural
decisions; this story adds no new persisted domain model.

**Evaluation:** Outside-in component/interaction evidence for active context,
direct Resume and independent menu opening, and the shared search journey;
bounded visual checks at phone, iPad, and desktop widths for alignment,
overflow, separators, and menu spacing. Reuse relevant historical/current
tests. Record a one-time product-wide sweep for the removed embedded-menu
presentation and its exclusively used names, separately from behavioral proof.

**Effort hypothesis:** S — approximately 45–60 minutes, medium confidence,
assuming reuse of the surviving navigation/search helpers and focused tests.
Assimilation context reconciliation is the main sizing uncertainty.

**Depends on:** None.

**Safe stopping point:** Active navigation, direct recall continuity, and
common search work within the deployed popup/header layout independently of
any later sidebar or breadcrumb redesign.

<a id="story-2"></a>

### Navigate from a stable sidebar breadcrumb with aligned tools

**Identity:** SEED-080#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

**Goal:** An author can see where the current item belongs and navigate its
notebook and ancestors without a crowded note header or a path that changes
when the contents tree scrolls.

**Scope — required:**

- Implement the approved “Sidebar header” design. With the sidebar docked,
  align its New note, New folder, and Sort controls with the note toolbar in
  the same top-bar row. Keep the pane divider aligned with the sidebar edge,
  retain the independent sidebar toggle, and preserve the active navigation
  control from the first story. Note tools remain right-aligned and use their
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

**Scope — deferred:** Command-menu/search/Resume recovery remains in
[the first story](#story-1). New tree navigation capabilities and changes to
note content, title editing, reading width, or details disclosure are deferred.

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

**Implementation cohesion:** Reuse the existing notebook realm and folder
trail derived by the workspace layout for breadcrumb, tree context, and
creation scope. Treat location as one derived domain concept presented in the
sidebar header or compact fallback, rather than copying route classification
or ancestry state into each surface. Keep sidebar command ownership distinct
from note command ownership inside the shared layout. Preserve pending-load
context; its existing sticky-realm helper serves loading continuity, not the
deleted scroll-dependent display. No new persisted model is needed.

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

**Depends on:** None. Its position after the recovery story is the owner’s
priority; sharing header components is not a blocking prerequisite.

**Safe stopping point:** The sidebar location and aligned controls are useful
independently, with normal tree navigation and closed-sidebar access intact.

## Recovery Evidence

Inspected on 2026-10-20 against main `21d19fc9b08b4ad486769c427e51c6cf8dd3b0a4`.
The owner supplied two screenshots of the deployed iPad interface. This is
source/history inspection, not a runtime reproduction or a test execution.

- Before the quiet launcher: `15b6077ccd313eb0ef17deb0fa638b5f25f06200`.
  Final historical collapsed-menu snapshot:
  `e4227c46a0fb8fcc2185769bb0ab9974588288aa`; removed in `448d6fdc2f`.
  `HorizontalMenu.vue` selects the first active navigation item, displays it,
  distinguishes direct actions from context, and supplies a separate chevron.
  `MainMenu.resume.spec.ts` proves direct collapsed Resume and contains the
  progress/paused boundaries. Recover the behavior, not the old wrapper DOM.
- `useNavigationItems`, `NavigationItem`, `navigationActionItems`,
  `useResumeRecall`, and `useGoToNextAssimilation` survive in the current
  frontend. Their existing counts, diligent-mode styling, resume intent,
  keyboard preparation, and destination handling are reconciliation assets.
- Historical limit: Assimilate was `isActive: false` immediately before the
  launcher change. An older route-active condition was removed in
  `d249786830`. Showing the current assimilation context is the owner’s
  explicit reconciliation requirement; history does not establish a new
  off-route assimilation-priority policy.
- Historical common search popup: `a90233a446`,
  `frontend/src/components/toolbars/GlobalBar.vue`, using `PopButton` and
  `note-search`. Reconcile with today’s global search component contracts;
  the present relationship `SearchForm` requires a note and is not itself a
  drop-in global dialog.

Sidebar follow-up inspected on 2026-10-20 at
`428dfdf574f687ec83178f15ab4f07b36f315483`, after the owner selected the
“Sidebar header” alternative. This is source/history inspection, not a
runtime layout observation.

- `NotebookSidebarLayout.vue` currently derives notebook and folder location
  for notes, folders, and attachments. It puts desktop breadcrumbs beside the
  note toolbar and phone breadcrumbs in a separate row. `Sidebar.vue` renders
  its commands below that global bar. These are the two layout responsibilities
  to reconcile, using the same domain context.
- The scroll-dependent ancestor display and its exclusive support were
  deleted by `56505b78dd934aa2efe8dc32ddd5564e913ffeae` on 2026-09-28.
  A one-time reading at the inspected revision found no surviving exact-name
  or display-text references across frontend, E2E, docs, agent guidance, and
  planning. Recover the deleted identifiers from that commit when repeating
  the acceptance sweep, rather than introducing a permanent checker.
- `SidebarFolderItem`, `useSidebarRowReveal`, and the existing sidebar
  reveal/loading/route tests own the retained expansion, selection, row
  visibility, and pending-load behavior. They are reconciliation assets.

## Ordering and Scope Reduction

Keep command-bar recovery first and insert sidebar location immediately after
it, as requested. Keep each story’s own scope independently evaluable.

## Open Decisions

None. The owner selected the Sidebar header design and deletion of the
separate scroll-dependent ancestor display.

## When to Surface

Now, following the owner’s deployed-interface review.
