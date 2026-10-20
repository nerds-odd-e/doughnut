# Sidebar location context

Supporting dated evidence for [SEED-080](SEED-080-command-bar-reconciliation.md#story-2).

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

## Sidebar preparation observation

**Investigation evidence — 2026-10-20:** Observed in the established
preparation workspace
`/Users/terryyin/git/doughnut/.worktrees/navigate-from-a-stable-sidebar-breadcrumb-with-a`
at `944bee0c4a687a30ac3231f172ad7cc0113a756e`. Headless Chromium ran the real
frontend with mocked backend HTTP responses; these are preparation observations,
not implementation acceptance.

- `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/layouts/NotebookWorkspaceContext.spec.ts tests/layouts/NotebookWorkspaceHeader.spec.ts tests/notes/sidebar tests/toolbars/Breadcrumb.spec.ts`
  passed 52 tests. The inspected setups reach named folder navigation, scoped
  creation, drawer toggling, same-notebook pending note continuity, expansion,
  cache/loading, sorting, root scroll reset, and row reveal. The baseline emitted
  a ResizeObserver-loop notification in the header suite.
- Disposable `sidebarLocationPreparationProbe.spec.ts`, run with
  `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/layouts/sidebarLocationPreparationProbe.spec.ts`,
  failed at 768 and 1280 px because the breadcrumb was outside the sidebar.
  A scratch relocation of the existing breadcrumb above the tree and the
  existing contents toolbar into the shared bar exposed a real sizing issue:
  the toolbar's full-width navbar styling exceeded the row and destabilized
  note-command interactions. Giving that toolbar intrinsic width, zero minimum
  height/padding and no flex shrinking restored all ten header tests. In the
  combined rerun of the baseline command plus the probe, 50 passed and four
  failed: the two intentionally superseded desktop breadcrumb-position tests,
  and two probe scroll checks whose fixture lacked a constrained scrollport.
- With a 300 px scrollport and 2000 px disposable spacer, the final probe passed
  both widths: contents tools fit within the shared bar, scrolling changed
  the tree scroll position without moving the location header, and activating
  the notebook link reached `notebookPage`. This observes scroll containment
  and reuse, not populated-tree behavior, final pane-divider alignment, or
  the future compact disclosure. The retained row tests cover populated rows.
- `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/layouts/sidebarRealmPreparationProbe.spec.ts tests/notes/sidebar/SidebarRowReveal.spec.ts`
  passed nine tests after restoration. Four disposable realm cases entered
  folder/attachment routes and followed a parent link to `folderPage`, or
  resolved rendered circle/Bazaar links to their named destination. Circle
  and Bazaar destination pages themselves were not exercised.
- Discrimination check:
  `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/sidebar/SidebarRowReveal.spec.ts`
  failed all five tests with the reveal action disabled in a scratch edit;
  the restored run above passed all five. Their setup/assertions observe full
  row visibility, minimal scroll and sorting at the Sidebar boundary.
- A one-time exact-name/display-text reading, recovering identifiers and the
  exclusive row attribute from deletion commit
  `56505b78dd934aa2efe8dc32ddd5564e913ffeae`, found zero matches in frontend,
  backend, CLI, MCP, packages, E2E, docs, agent guidance and planning. The
  removal remains satisfied; this is a reading, not a permanent checker.
- All disposable specs and production edits were removed; `git diff --exit-code -- frontend`
  passed before recording. Final disclosure accessibility, long-name fit,
  phone presentation and exact pane boundaries remain execution proof
  obligations. No product-choice hypothesis remains unresolved; no native
  iPad runtime observation or final-design acceptance is claimed.
