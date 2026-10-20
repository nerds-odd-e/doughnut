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

## Preparation observations and decisive premises

All observations below were made on 2026-10-20 in
`/Users/terryyin/git/doughnut/.worktrees/navigate-from-a-stable-sidebar-breadcrumb-with-a`
at `944bee0c4a687a30ac3231f172ad7cc0113a756e`, with headless Chromium and mocked
backend HTTP. The source's dated investigation preserves commands, setup,
results and limits. Scratch production edits and specs were reverted before
this plan was written; `git diff --exit-code -- frontend` passed.

| Premise and consuming operation | Observed result and consequence |
| --- | --- |
| Existing realm data supplies notebook and folder destinations across the supported resource types | The four-case `sidebarRealmPreparationProbe.spec.ts` entered folder/attachment pages, followed their parent link to the named folder destination, and resolved circle/Bazaar rendered links. All passed. Root navigation was also clicked in the geometry probe. Reuse the existing realm derivation. No claim that the circle/Bazaar destination page journey was exercised. |
| Relocating chrome can retain the existing command and tree owners | Baseline `NotebookWorkspaceContext`, `NotebookWorkspaceHeader`, the sidebar directory and `Breadcrumb` suites passed 52 tests. A scratch relocation exposed oversized contents-navbar geometry and unstable command clicks. Intrinsic toolbar sizing restored all ten header tests and preserved the sidebar suites; the only remaining old-test failures were two desktop breadcrumb assertions requiring its superseded position. Rewrite those around stable sidebar location. |
| Location can remain outside the tree scroll while retaining navigation | The two-width disposable geometry probe failed on the original location. After scratch relocation/sizing, with a constrained 300 px tree and 2000 px spacer, both cases passed: contents tools inside the bar, tree scroll changed while location stayed fixed, and notebook-link navigation worked. The fixture isolates scroll containment; final populated-tree/pane geometry remains implementation proof. |
| Existing row-reveal tests discriminate reveal behavior | Disabling the actual reveal action made all five `SidebarRowReveal` cases fail; restoration passed all five. Preserve these tests for minimal scrolling and full row visibility, and add workspace-level geometry for the relocated header. |
| Existing location/header consumers need adaptation, not silent weakening | Scratch suite results identify the old two desktop-position expectations. Source tracing also reaches `notePage.expectBreadcrumb`, used by Bazaar browsing and circles, and `.daisy-breadcrumbs` in MessageCenter note context. Keep full realm/path observability and adapt the user journey to open disclosure where necessary. Do not replace navigation proof with aggregate hidden text. |

Literal baseline command:

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/layouts/NotebookWorkspaceContext.spec.ts tests/layouts/NotebookWorkspaceHeader.spec.ts tests/notes/sidebar tests/toolbars/Breadcrumb.spec.ts
```

The source records the disposable probe commands and the failed/passing reveal
mutation. No native iPad runtime result or final visual acceptance is implied.
The new compact disclosure, its accessibility, exact pane alignment, long-name
fit and phone placement are work to implement and prove, not existing
capabilities assumed to work. Baseline ResizeObserver notifications are recorded;
the final interaction assertions must still pass.

The isolated Cypress selector accepted the two consumer features, with both
private-mock requirements false, through this side-effect-free runner prefix:

```sh
CURSOR_DEV=true nix develop -c node --input-type=module -e 'import {selectedCypressSpecs,assertSupportedIsolatedCypressSpecs} from "./scripts/isolated-cypress-spec-selection.mjs"; const specs=selectedCypressSpecs({argv:["--spec","e2e_test/features/bazaar/browsing.feature,e2e_test/features/circles/notebooks_in_circles.feature"],checkoutRoot:process.cwd()}); console.log(JSON.stringify({specs,...assertSupportedIsolatedCypressSpecs(specs)}));'
```

This confirms selection only. Running those scenarios allocates/reset disposable
E2E state and remains execution verification; use the worktree-owned wrapper.
No paid service is needed.

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

Accepted 2026-10-21: `NotebookLocation` uses the existing shared breadcrumb
inside the labeled standard modal. `NotebookWorkspaceLocation.spec.ts` observes
parent/root destinations, note-property/attachment realms, keyboard opening,
full-name bounds and 44px targets, Escape/Close focus return at 320/768/1280.
`NotebookWorkspaceContext.spec.ts` retains layout/creation proof. Initial full
focused command selected context, Breadcrumb, NoteContextReader,
AnsweredSpellingQuestionAccidentalMatchNoteContext, MessageCenterPage.subject
and Modal: 35 tests passed. After the independent test-only split, both workspace
specs passed 16 tests; unchanged consumer proof remains applicable. Both
`pnpm -C frontend exec vue-tsc --noEmit` and `tsc --noEmit` passed through Nix.
The plan's exact isolated Bazaar/circle command passed all seven scenarios,
observing visible realm context and permissions through disclosure. Final
realm destination activation remains G2. Inspected ignored screenshots
`frontend/vitest-test-results/sidebar-location-*.png`: full disclosed names
fit at all three widths; G1 owns the compact 768px limit. Initial screenshot
output failed Vite's allowed-directory check; the corrected ignored path passed.
Independent refactoring split the overlength test into context/location/shared
fixture files; production boundaries stayed unchanged. Vite generated the new
declarations; its first exit was pending Biome formatting, resolved by the
coordinator's required selective formatter.

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

Accepted 2026-10-21: the workspace supplies location through Sidebar's slot
outside its scrollport, or above main when hidden. `NotebookWorkspaceContentsLocation`
uses 40 real rows at 768/1280 and observes fixed location geometry, full active
row after navigation/sorting, less-than-one-row reveal for a half-clipped row,
ancestor navigation through the main fallback and pending same-notebook context.
Context's notebook-text width assertion and inspected `sidebar-location-*-active-tree.png`
screenshots resolve G1. The focused command passed 68 tests in 16 files:
`CURSOR_DEV=true nix develop -c pnpm frontend:test tests/layouts/NotebookWorkspaceContext.spec.ts tests/layouts/NotebookWorkspaceLocation.spec.ts tests/layouts/NotebookWorkspaceContentsLocation.spec.ts tests/layouts/NotebookWorkspaceHeader.spec.ts tests/layouts/NotebookWorkspaceHeader.dictation.spec.ts tests/notes/NoteToolbar.overflow.spec.ts tests/notes/sidebar`.
Both required typechecks passed. Independent refactoring made no edits and
identified the shared fixture's assimilation consumer; the additional command
`CURSOR_DEV=true nix develop -c pnpm frontend:test tests/layouts/NotebookWorkspaceAssimilation.spec.ts`
passed seven tests before delivery. Coordinator selective formatting passed.

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

Accepted 2026-10-21: location follows the existing drawer's opened state at
all widths; the hidden phone fallback lives above main content. Phone workspace
tests at 320/390 observe populated independent tree scrolling, fixed location,
contents command access and actual descending sort, keyboard full-name ancestry
navigation, and existing drawer-close behavior. Entered note-editor text survives
drawer toggles and 767/768/767 resizing. Root navigation/disclosure is parameterized
at 320/768. Inspected ignored phone-drawer and phone-root screenshots match these
observations. Command through Nix `pnpm frontend:test` with Context, Location,
ContentsLocation, PhoneLocation, Header, Header.dictation, NoteToolbar.overflow
and the full sidebar directory passed 72 tests in 17 files. Both required
typechecks passed. Independent refactoring shared the populated notebook fixture;
the ContentsLocation/PhoneLocation rerun passed seven tests and both typechecks
passed afterward. Coordinator formatting passed. Initial title-selector and
note-to-folder drawer-close expectations were corrected to match actual product
controls and the retained route policy; no product workaround was added.

### 4. Align contents actions with the adaptive note toolbar
Type: Behavior
Status: planned

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

## Verification and delivery gates

The repository's `AGENTS.md`, `frontend`, `frontend-testing`, `unit-testing`
and `e2e-authoring` skills govern proof. Run repo commands through Nix. Focused
frontend commands use `pnpm frontend:test <spec paths relative to frontend>`;
select the whole affected boundary suite rather than a few assertions.

Before accepting each changed frontend state, also pass:

```sh
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
```

Reuse identical-content typecheck evidence. If a component is added or deleted,
generate declarations with `pnpm -C frontend exec vite build --emptyOutDir`.
No backend/API generation is expected; reassess if signatures actually change.
The two identified E2E consumer scenarios run through the isolated wrapper:

```sh
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/bazaar/browsing.feature,e2e_test/features/circles/notebooks_in_circles.feature
```

Their allocation/DB reset occurs only during authorized execution. Shared
breadcrumb changes also reach `MessageCenterPage.subject` through
`NoteContextReader`, and accidental-match recall through `AccidentalMatchResolveRow`;
inspect and include those suites when changed behavior reaches them. Unchanged
inner breadcrumbs may reuse their observed baseline while workspace proof owns
new presentation. Do not invent unrelated backend/full-E2E gates from CI config.

During execution use Jidoka, a fresh dough-post-change-refactor agent, necessary
generation, coordinator `./scripts/run.sh pnpm format:changed` once, plan update,
agent commit with check-only lint hook, and publication/CI ownership under
dough-execute-plan. Preparation does not run that execution pipeline.

## Current decisions and review

Execution resumed 2026-10-21 in Story Branch Mode for `SEED-080#story-2`.
Owned execution/originating checkout:
`/Users/terryyin/git/doughnut/.worktrees/navigate-from-a-stable-sidebar-breadcrumb-with-a`,
branch `codex/navigate-from-a-stable-sidebar-breadcrumb-with-a`; default integration
checkout `/Users/terryyin/git/doughnut`; remote `origin`, trunk `main`.
Publisher `dashboard-territory.local-doughnut`, agent Aino-chan; accepted claim
`f2a1dbf31d687376e64f9e1568e477d8ed230669`, starting revision
`4d44b2eb279eeff338be0331c02322aad173dd80`. Established start reused without
another Take. Changed-since-readiness indication retained; scope still aligns.
Existing planned refinement authority applies to overruns. Checkout preparation
`./scripts/run.sh bash scripts/worktree_setup.sh` and
`CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit` passed.
The installed pre-commit hook is check-only `pnpm lint:changed`.

CI source: GitHub Actions `nerds-odd-e/doughnut`, verified selector `ci.yml`
(display name `donut CI`), target `codex/navigate-from-a-stable-sidebar-breadcrumb-with-a`.
Observer: coordinator `aino-sidebar-44097ed9`, directory
`/tmp/dough-ci-501/watch-gMU34E`, PID 27009, stream session 56944, yielded cell 11.
The startup claim's trunk CI is unobserved by this branch observer.
Slice 1 accepted on the remote story branch at
`9ee121fdfad67f4aeab513c8bc09e960c17939c0`, suffix base
`f2a1dbf31d687376e64f9e1568e477d8ed230669`; observation reused this stream.
The post-format slice-1 Vite build passed with generated declarations intact.
Slice 2 accepted on the remote story branch at
`bef45f8b52e5bf92af3a84155468f6dcb42c11af`, suffix base
`9ee121fdfad67f4aeab513c8bc09e960c17939c0`; observation reused this stream.

- The owner already selected the Sidebar header design; no new product decision
  is pending. Complete-name disclosure and responsive location are required.
- Four Behavior slices evolve one location model and retain existing action/tree
  owners. No speculative Structure slice, parallel navigation state, persistence,
  or prerequisite on story 1 is needed.
- The sizing checks found no further independent outcome inside a leaf. An
  additional slice-plan-refinement pass is not needed. No remaining slice-specific
  concern was found in this bounded review. Final implementation proof remains
  mandatory; preparation observations do not mark any slice done.
- Execution must reconcile any shared-header changes landed by story 1 while
  preserving both stories' outcomes. The scratch implementation is not retained
  or prescribed as final code.

## Story obligations

### G1. Compact notebook identity at the dock breakpoint
Reported: slice 1 — "At 768 compact in current old bar allocation shrinks to icon + 'Frui…', hiding notebook text."
Story clause: "Show notebook identity above the folder trail as in the approved design."
Disposition: proved by slice 2: NotebookWorkspaceContext.spec.ts "shows notebook identity" asserts rendered text width above 180px at 768/1280; inspected sidebar-location-768-active-tree.png shows readable notebook identity above the tree.

### G2. Activate retained realm destinations through the disclosure
Reported: slice 1 — "Activating circle/Bazaar realm links was not newly exercised by this delegated unit-test command."
Story clause: "Preserve notebook-root and ancestor-folder navigation, including circle or Bazaar context where applicable."
Disposition: receiving slice 4

### G3. Shared mount helper assimilation consumer
Reported: slice 2 — "NotebookWorkspaceAssimilation.spec.ts consumes the modified mount helper and observes navigation/property-panel visibility; it was outside the supplied command."
Story clause: "retain the independent sidebar toggle, and preserve the active navigation control."
Disposition: proved by slice 2: NotebookWorkspaceAssimilation.spec.ts passed seven tests with the changed viewport-bounded mount helper; active navigation and property-panel visibility observations remain valid.
