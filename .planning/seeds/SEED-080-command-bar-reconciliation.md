---
id: SEED-080
status: dormant
planted: 2026-10-20
planted_during: owner review of the deployed cohesive note workspace
trigger_when: restoring navigation continuity and finishing the deployed command bar
scope: small
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

The owner explicitly groups these understood changes into one story. Sidebar
toolbar alignment and breadcrumb placement need design review first and are
separate work; this story does not choose their layout.

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

**Scope — deferred:** Sidebar toolbar relocation/alignment, breadcrumb
placement, and changes to the note reading/details layout. Those need the
separate design decision requested by the owner. Note titles remain authored
text, including any dates, and title/body editing stays inline.
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

## Ordering and Scope Reduction

Place this single recovery story first in the product backlog, as requested.
Keep the later sidebar/breadcrumb design discussion outside its commitments.

## Open Decisions

None for this story. The sidebar/breadcrumb alternatives await owner review.

## When to Surface

Now, following the owner’s deployed-interface review.
