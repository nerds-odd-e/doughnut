# Command-bar reconciliation context

Supporting constraints and dated evidence for [SEED-080](SEED-080-command-bar-reconciliation.md).

## Story 1 architecture and UI

**Architecture:** Keep navigation identity, activity precedence,
counts, and action classification in the existing shared navigation model.
Reconcile its presentation in the compact launcher and popup; do not create a
second route/state classifier or parallel recall/assimilation flags. Preserve
the ownership of note actions and their state when changing header alignment.
Use one common search-dialog entry path for both launcher targets and the
keyboard shortcut. Follow the existing vocabulary and accepted architectural
decisions; this story adds no new persisted domain model.

Use the named note/property locations and shared note route family from
[ADR 0005](../../docs/adrs/0005-web-routes-accepted.md). Opening a property
is already a resource location, not a new assimilation route.
[ADR 0001](../../docs/adrs/0001-ubiquitous-language.md#assimilation)
distinguishes the assimilation sequence from assimilating on a note; that
distinction does not itself decide the navigation label. Reuse the existing
next-assimilation and resume actions, including their loading, completion,
and focus behavior, under
[ADR 0006](../../docs/adrs/0006-failure-handling-accepted.md#usage).
No conflict with these Accepted decisions was found. The ADR index and
in-file statuses agree; ADR 0001 has a filename hygiene mismatch (no
`-accepted` suffix), which does not change its Accepted status.

**UI:** The primary control identifies the current activity and performs its
existing action where applicable. Its independent menu opener remains
keyboard and touch reachable. In the popup, both the empty search-shaped
target and its adjacent search button announce that they open search; input
begins in the dialog. Reuse the common dialog's close/Escape behavior and
search-result navigation, including selection of the note already open.
These are interaction requirements, not a new layout choice.


## Story 1 investigation

**Investigation evidence — 2026-10-20:** Observed in the established
preparation workspace
`/Users/terryyin/git/doughnut/.worktrees/recover-active-navigation-and-finish-command-bar`,
at `c64063489126d137d32fb7f4ab3027a26756dd1f`, using headless Chromium and
the real frontend with mocked backend HTTP responses. These are preparation
observations, not implementation acceptance.

- Current source/history confirms the historical first-active-item selection
  and action classification in `HorizontalMenu.vue` at
  `e4227c46a0fb8fcc2185769bb0ab9974588288aa`. Current `DonutMenu.vue` shows
  a fixed icon, while `useNavigationItems` retains Resume eligibility and
  precedence. The existing model is the reuse owner.
- `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars/DonutMenu.resume.spec.ts tests/toolbars/DonutMenu.search.spec.ts tests/layouts/NotebookWorkspaceHeader.spec.ts tests/composables/useGoToNextAssimilation.spec.ts`
  passed 34 tests. The inspected setups exercise paused/progress eligibility,
  search history/scopes/result navigation, adaptive commands, and next-unit
  note/property destinations. They do not prove the proposed compact
  controls or an iPad software keyboard.
- A disposable `commandBarPreparationProbe.spec.ts`, run with
  `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars/commandBarPreparationProbe.spec.ts`,
  reproduced the missing compact Resume: with a paused recall and five
  loaded trackers, the closed menu rendered only the fixed launcher.
  A second case mounted `SearchForNoteAndFolder` in the real `Modal`,
  supplied a recent note through `NoteController.getRecentNotes`, opened
  the dialog, observed focus on Search, and selected the current note.
  It passed, including dialog closure without changing the current note.
  This settles reuse of the general search component in that dialog; it
  does not prove the future launcher's wiring or touch keyboard behavior.
- `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/layouts/commandBarGeometryPreparationProbe.spec.ts tests/components/form/RichMarkdownEditor.propertyLocation.spec.ts tests/components/form/RichMarkdownEditor.propertyMemoryTracking.spec.ts`
  passed the 16 existing property tests. They exercise direct property
  entry, existing/read-only/specialized/missing properties, and assimilation
  from the property panel. The disposable geometry case mounted the real
  notebook workspace at 1280 × 700 with a note and measured 352.5 px
  between the last visible command and the available toolbar's right edge,
  failing its right-alignment assertion.
- A scratch `justify-end` on the existing note-command row made that
  geometry assertion pass. The same run,
  `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/layouts/commandBarGeometryPreparationProbe.spec.ts tests/layouts/NotebookWorkspaceHeader.spec.ts tests/notes/NoteToolbar.overflow.spec.ts`,
  passed all 16 cases, including resizing and active-panel retention.
  The header suite emitted a ResizeObserver-loop notification in both the
  original and scratch runs. This observation supports changing alignment
  within the existing measured allocation; it does not settle the width
  taken by the future context control or visual spacing acceptance.
- Both disposable probes and the scratch production edit were removed.
  `git diff --exit-code -- frontend` passed before this evidence write.
  No product changes remain. At the end of this observation, native iPad
  keyboard behavior and final menu spacing/dividers were unverified, and
  the property-context choice awaited the owner. The [source story's property-context decision](SEED-080-command-bar-reconciliation.md#story-1) settles
  that choice; code presence and Chromium focus do not settle native iPad
  keyboard behavior.


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

