# Stable sidebar location and aligned contents tools

**Identity:** SEED-080#story-2
**Source:** [Navigate from a stable sidebar breadcrumb with aligned tools](../../seeds/SEED-080-command-bar-reconciliation.md#story-2)

## Goal and scope

An author can identify and navigate the current notebook and folder ancestry
from a stable location above the contents tree, with contents and note tools
aligned. Implement the owner's Sidebar header choice, including full-name
location disclosure, docked/hidden/phone presentations, existing realm links,
creation scope/access, adaptive note actions, and retained tree behavior.

The source owns complete scope and examples. Command-menu/search/Resume recovery
belongs to story 1; consume its changes if already landed without reimplementing
its classifier or action rules. Neither story is a blocking dependency. Reading
width, note content/editing, new tree capabilities, persistence and new routes
are deferred. The historical scroll-dependent display was already deleted;
only a one-time acceptance reading remains, separate from behavioral proof.

Preparation is planning only. No product implementation is authorized by this
plan's creation or readiness assessment.

## Design and reuse

PFE inspected the real workspace layout, shared breadcrumbs, Sidebar and its
commands/tree, note header, route loaders, recall/note-context consumers and
Cypress breadcrumb assertions. The smallest coherent design is:

- Keep notebook realm and folder trail derived in `NotebookSidebarLayout` from
  existing route realms. Present that same concept in the active workspace
  location surface and its disclosure. Do not copy route classifiers, ancestry
  arrays or scroll position into a new store. `useStickyActiveNoteRealmForRoute`
  owns pending note continuity; it is not part of the historical deletion.
- Reuse named notebook/folder/circle/Bazaar links. `BreadcrumbWithCircle`,
  `Breadcrumb` and `BasicBreadcrumb` also serve `NoteContextReader` and
  `AccidentalMatchResolveRow`; keep their general navigation reusable. Add the
  workspace presentation at its actual owner, extracting only shared navigation
  needed by the current callers. A workspace-only layout need not change those
  read-only/recall presentations.
- Keep contents actions in `SidebarToolbar` and their existing creation,
  permission and sort owners. Relocate their presentation through the workspace
  layout; no duplicate mounted action owners or second readonly policy. The
  current full-width navbar sizing must adapt to its available row allocation.
- Preserve `Sidebar`'s separate tree scrollport, expansion state, listing cache,
  root reset and `useSidebarRowReveal`. The stable location lives outside that
  scrollport. Preserve note toolbar ownership/teleport, measurement, action
  order, busy/open action pinning, and Undo.

This follows North Star **One notebook tree** and
[ADR 0001 — Ubiquitous language](../../../docs/adrs/0001-ubiquitous-language.md#notebook--note-structure),
[ADR 0005 — Web routes](../../../docs/adrs/0005-web-routes-accepted.md),
and [ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md#usage).
No new cross-cutting decision or North Star update is warranted. Investigations
use isolated test surfaces under [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md).
The seed records the ADR status check and filename hygiene observation.

Dated preparation premises: [retained observations](EVIDENCE.md#preparation-observations-and-decisive-premises).

## Proof ownership

Use mounted real workspace/pages and production router with crafted `makeMe`
data and backend-only mocks. Assert visible names, geometry and resulting named
location, not implementation class presence alone. New proof must discriminate
the promised behavior, including keyboard focus and usable targets.

| Final promise | Owning slice | Observable proof |
| --- | --- | --- |
| Notebook identity above compact folder trail; complete long/deep names and navigable ancestry through accessible disclosure | 1 | Extend `NotebookWorkspaceContext`: open by keyboard and pointer, inspect complete names and fitting geometry, follow notebook/ancestor destinations, close and restore focus. Root has meaningful notebook identity with empty trail. |
| Note/property, folder, attachment and notebook-root realm; circle/Bazaar links and permissions | 1 | Routed workspace cases fed by their actual realm endpoint responses; assert destination changes, read-only action eligibility, and property-family context. Retain Breadcrumb/context-reader consumers. |
| Stable docked location and same location above main content when hidden; active item discoverable after navigation/sort | 2 | Real workspace at 768/1280 px with populated overflowing tree: scroll, observe fixed header and full active-row reveal, follow an ancestor, toggle contents and use fallback. Retain discriminating row-reveal, loading/cache, expansion and root-reset suites. |
| Pending route-load context and normal manual folder toggling/selection | 2 | Retain `SidebarRouteNavigation`, `SidebarActiveFolder`, `SidebarAncestorLoading`, `SidebarFolderListingReload`, `SidebarNotebookShell`, `SidebarFirstGeneration` and row tests; assert stable displayed location during a deferred same-notebook note response in workspace proof. |
| Familiar phone controls, location and contents tools in opened drawer, independent tree scroll, full-path access | 3 | 320/390 px routed workspace: open drawer, scroll populated tree, open location disclosure and navigate, close/reopen contents, verify header and note commands remain reachable. Resize across 767/768 with an open note action. |
| Docked contents/note toolbar baseline, pane divider at sidebar edge, independent sidebar toggle, active navigation and right-aligned adaptive note commands | 4 | Workspace geometry at 768/1024/1280: toolbar action rectangles share row, divider matches pane edge, last note command fits right allocation, menus open without clipping; busy/open actions survive narrowing and toggling. Run `NotebookWorkspaceHeader` and `NoteToolbar.overflow` suites plus story-1 navigation tests present on execution base. |
| Contents creation destinations and sort behavior/access survive relocation | 4 | Drive New note/New folder from docked bar and phone drawer with notebook/folder/note contexts; assert existing destination scope. Sort and inspect order/active-row visibility. Retain read-only/signed-out refusal proof in sidebar toolbar tests. |
| Actual visual hierarchy, bounded long text and usable overflow | 1–4 at their affected surfaces | Inspect browser-rendered screenshots at phone, iPad and desktop sizes; assert text/target bounds with production CSS. Include root, long notebook name, deep trail, narrow note allocation and open disclosure. |

One-time removal reading at acceptance: recover the deleted component, helper,
attribute and display-text identifiers from
`56505b78dd934aa2efe8dc32ddd5564e913ffeae`; search the whole product, generated
declarations, docs and guidance. The preparation reading returned no matches.
Record the result separately from tests; add no absence assertion or checker.
Preserve dated evidence as evidence, never rename its observed action to clear
a sweep.

## Ordered slices

### 1. Reach complete named location from a compact header
Type: Behavior
Status: done

Behavior: Given a current notebook resource with a short, long or deep path,
the author sees notebook identity above a compact folder trail, opens its
location disclosure, and navigates to any named ancestor or notebook/realm
entry. At root the identity remains meaningful. Closing the disclosure keeps
the page and restores a usable location control.

Keep this presentation in the existing workspace location positions for this
slice; slice 2 replaces desktop placement and slice 3 replaces phone placement.
Use existing route-derived data and named destinations for note/property,
folder, attachment and root. Keep read-only/recall breadcrumbs coherent. Adapt
Cypress breadcrumb observation to the visible user journey if disclosure changes
its enclosing observation; do not weaken the realm assertion.

Proof: Proof-table rows 1–2 and visual hierarchy. Extend the real workspace
context suite; run its full suite, Breadcrumb and affected note-context/recall
consumers. Run the Bazaar/circle E2E consumer scenarios if their enclosing
rendered observation or page object changes. Accessible disclosure includes
keyboard opening, full names, destination selection and dismissal focus.

Sizing: 5–8 minutes of implementation, focused verification and cleanup; this
is one disclosure/navigation loop across data variations, not separate route
handlers. Scrutinized above the 5-minute target: splitting out UI scaffolding
would leave an unused component. E2E startup/runtime is a stated external-wait
exception; never waive its acceptance result.

Accepted proof: [slice 1 observations](EVIDENCE.md#slice-1).

### 2. Keep location stable with docked or hidden contents
Type: Behavior
Status: done

Behavior: With contents docked, scrolling the populated tree leaves the location
header visible above it; navigating or sorting keeps the active row discoverable.
Hide contents and the same location becomes usable above main content. Show
contents again and its stable location belongs to the sidebar.

Move presentation within the existing layout, outside the tree scrollport.
Preserve root reset, expansion/toggling, listing cache, active selection and
pending note context. Replace the old desktop-position tests with geometry and
navigation of the surviving location. Existing phone position is usable interim
behavior, replaced by slice 3. Contents tools stay at their existing position
until slice 4; that later alignment does not invalidate location navigation.

Proof: Proof-table rows 3–4; full context suite and sidebar directory. Use actual
overflowing rows at the workspace boundary, not the preparation probe's spacer,
and verify minimal reveal as well as the location rectangle during scrolling.

Sizing: 5–8 minutes including proof/cleanup. One responsive ownership rule covers
docked and hidden states; splitting them would temporarily strand location when
the sidebar closes. The independent toolbar alignment belongs to slice 4.

Accepted proof: [slice 2 observations](EVIDENCE.md#slice-2).

### 3. Navigate from location inside the phone contents drawer
Type: Behavior
Status: done

Behavior: On a phone, open contents from the familiar main controls, use contents
commands and the stable location above its independently scrolling tree, and
navigate from its full ancestry. With contents hidden, location remains reachable
above the main content. Note commands and their active operation stay usable
through drawer toggles and a resize across the dock breakpoint.

Reuse the same location/command owners and drawer state. Let existing note-route
drawer-close behavior govern navigation; this adds no separate close policy.
Do not mount duplicate hidden action owners merely to position controls.

Proof: Proof-table row 5, plus phone visual/keyboard/touch targets. Context and
header suites at 320/390/767/768 px with long names, an empty root trail and an
active note action. Retain the existing mobile route-navigation check.

Sizing: About 5 minutes including focused proof and cleanup; a single
open/navigate/close drawer journey.

Accepted proof: [slice 3 observations](EVIDENCE.md#slice-3).

### 4. Align contents actions with the adaptive note toolbar
Type: Behavior
Status: done

Behavior: With contents docked, New note, New folder and Sort occupy the same
row as right-aligned note actions, with the pane divider at the sidebar edge.
Toggle or resize contents and use creation/sort/note actions with the same
scope, permissions and active state. On phones contents commands remain in
the drawer and familiar main navigation/note controls remain usable.

Adapt existing toolbar row allocation rather than duplicating commands or route
state. The scratch observation showed the contents navbar needs intrinsic sizing
inside the shared row. Preserve active navigation from story 1 if present,
its independent menu entry, the independent sidebar toggle, Undo, note action
order, overflow and busy/open pinning. Incorporate any required generation of
component declarations through Vite, not manual edits.

Proof: Proof-table rows 6–7 and final visual review. Run full workspace header/
context, sidebar, `NoteToolbar.overflow`, and affected navigation tests. Exercise
both creation controls and sorting through their relocated surface, including
read-only/signed-out access. Verify final divider, baselines, clipping and
right-edge alignment on phone, iPad and desktop. Complete final realm-link
integration by activating circle and Bazaar destinations through the disclosure
and observing the resulting named routes (G2). Perform the separate removal
reading after final code/refactor.

Sizing: 5–8 minutes including focused proof/cleanup. The retained action owners
bound the change; geometry and adaptive action retention are one row-allocation
proof loop. A >10-minute implementation overrun requires finer decomposition
unless only the documented test/external wait remains.

Accepted proof: [slice 4 observations](EVIDENCE.md#slice-4).

## Execution complete

Product advice: No additional product work recommended; retain the existing backlog order.

## Verification and delivery gates

Apply the [focused commands and delivery gates](EVIDENCE.md#verification-and-delivery-gates) to affected surfaces.

## Current decisions and review

Execution: Story Branch Mode, agent Aino-chan, publisher `dashboard-territory.local-doughnut`;
`origin`, trunk `main`; owned checkout `/Users/terryyin/git/doughnut/.worktrees/navigate-from-a-stable-sidebar-breadcrumb-with-a`,
branch `codex/navigate-from-a-stable-sidebar-breadcrumb-with-a`.
Observer: `nerds-odd-e/doughnut`, `ci.yml`, coordinator `aino-sidebar-44097ed9`,
`/tmp/dough-ci-501/watch-gMU34E`, PID 27009, stream session 56944, yielded cell 11.
Setup, revisions and readiness indication: [execution context](EVIDENCE.md#execution-context).

The approved design, source scope and direction remain unchanged; [preparation decisions](EVIDENCE.md#preparation-decisions) are retained.

## Story obligations

### G1. Compact notebook identity at the dock breakpoint
Reported: slice 1 — "At 768 compact in current old bar allocation shrinks to icon + 'Frui…', hiding notebook text."
Story clause: "Show notebook identity above the folder trail as in the approved design."
Disposition: proved by slice 2: NotebookWorkspaceContext.spec.ts "shows notebook identity" asserts rendered text width above 180px at 768/1280; inspected sidebar-location-768-active-tree.png shows readable notebook identity above the tree.

### G2. Activate retained realm destinations through the disclosure
Reported: slice 1 — "Activating circle/Bazaar realm links was not newly exercised by this delegated unit-test command."
Story clause: "Preserve notebook-root and ancestor-folder navigation, including circle or Bazaar context where applicable."
Disposition: proved by slice 4: NotebookWorkspaceRealmNavigation.spec.ts clicks disclosed circle/Bazaar links and observes named routes plus rendered destination headings.

### G3. Shared mount helper assimilation consumer
Reported: slice 2 — "NotebookWorkspaceAssimilation.spec.ts consumes the modified mount helper and observes navigation/property-panel visibility; it was outside the supplied command."
Story clause: "retain the independent sidebar toggle, and preserve the active navigation control."
Disposition: proved by slice 2: NotebookWorkspaceAssimilation.spec.ts passed seven tests with the changed viewport-bounded mount helper; active navigation and property-panel visibility observations remain valid.

### G4. Circle creation journey follows relocated toolbar
Reported: slice 4 — "Circle member adds a note under a circle notebook could not find aside [data-testid=note-creation-new-button] button."
Story clause: "Creation/sort controls keep their existing destination scope and access rules when their presentation moves."
Disposition: proved by slice 4: corrected toolbar-owner selectors passed all 18 final isolated Cypress scenarios, including circle creation, root/nested folder creation and folder-page note creation.

### G5. Optional standalone E2E typecheck
Reported: slice 4 — "The optional broad E2E TypeScript check exited 2 with unresolved SDK imports and Cypress chain typing diagnostics."
Story clause: "An author can see where the current item belongs"
Disposition: no user cost "An author can see where the current item belongs": this standalone command is outside required gates and the product execution path. No passing or prior-baseline claim is made. The selector-only correction changes no callable contract; required frontend types and actual Cypress consumer journeys passed.
