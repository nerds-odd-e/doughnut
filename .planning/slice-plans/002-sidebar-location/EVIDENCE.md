# Sidebar location observations

Retained evidence for [the active plan](PLAN.md), SEED-080#story-2.

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

## Slice 1

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

## Slice 2

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

## Slice 3

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

## Slice 4

Accepted 2026-10-21: one SidebarToolbar owner teleports into docked GlobalBar;
intrinsic 44px controls fit the pane. One layout-owned CSS variable supplies
sidebar and divider width. Geometry at 768/1024/1280 observes pane/divider equality,
control bounds, common centers, right edge and usable sorting menu. Creation's
12 routed phone/docked cases submit both controls in root/folder/note contexts,
observing exact backend destinations. RealmNavigation activates circle/Bazaar
links, named routes and rendered pages (G2). Circle navigation exposed the prior
string-ID/Number-prop warning; bounded route conversion fixed it. Open Assimilate
and recording survive toggles/narrowing; transcription completes title/body.
The broad focused command passed 166 tests/25 files; split realm/geometry/location/
creation passed 26 tests/4 files; final toggle consumers passed 42 tests/7 files.
Independent width refactoring passed 80 tests/11 files and both required types.
Concrete setup/assertions and literal commands are retained in execution handoffs.
Inspected final phone/iPad/desktop screenshots. Coordinator formatter passed.

Final Cypress initially failed circle creation on an aside-bound toolbar selector.
Both creation selectors now use the existing toolbar owner; consumer tracing
reached note creation and folder-page journeys. Final command:

```sh
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/bazaar/browsing.feature,e2e_test/features/circles/notebooks_in_circles.feature,e2e_test/features/note_creation_and_update/note_creation.feature,e2e_test/features/folder_organization/folder_page_readme.feature
```

All 18 scenarios passed, exit 0, wrapper stack shutdown complete. CLI/Wikidata/audio
callers reuse the proven web-creation selector purpose; external boundaries stayed
unchanged. Optional standalone `pnpm exec tsc -p e2e_test/tsconfig.json --noEmit`
failed on broad SDK/Cypress/project diagnostics; no passing or baseline claim.
Required frontend types and executable consumers passed independently.

## Final removal reading

On 2026-10-21 recovered the deleted display's component/helper/attribute/test
identifiers and display text from `56505b78dd934aa2efe8dc32ddd5564e913ffeae`.
A one-time reading across frontend (including declarations), backend, CLI, MCP,
packages, E2E, scripts, docs, guidance and planning returned no matches.
This reading is separate from retained behavior tests; no absence checker was added.

## Execution context

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

## Preparation decisions

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

