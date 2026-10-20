# Use note actions and folder context from one adaptive top bar

**Source:** [Use note actions and folder context from one adaptive top bar](../../seeds/SEED-078-cohesive-note-workspace.md#story-3)

**Identity:** SEED-078#story-3

## Goal and scope

Give a note reader or author one shared top bar for the Donut menu,
contents toggle, existing note commands, and global Undo. Put ancestry in
the bar on wide windows and immediately below it on narrow ones. Preserve
command behavior, measured overflow and pinning, note-owned editing and
dictation state, Assimilation content, creation context, access rules, and
other pages' controls. The source's key examples and UI contract apply.

Deferred promises: command-center search relocation, property presentation
and disclosure, new commands, and editor/history/learning redesign. No new
rejection policy. Reconcile with story 2 through ordinary shared ownership;
common header files do not establish a blocking dependency.

Preparation is planning only. No Take or implementation is authorized.
This owned checkout was observed at `ad2a592d06ad1dcd2edd61269fa50f097c294a9a`
on 2026-10-20. The accepted Book placement was then observed here through a
disposable application of the published frontend delta described below.
Execution starts from current trunk.

## Existing solutions and architectural decisions

PFE findings (source and consumer reading, 2026-10-20):

- `NotebookSidebarLayout.vue` owns GlobalBar, the contents toggle, sidebar
  state, notebook/folder ancestry, and non-note main-column New note.
  `useNotebookSidebarDrawer` / `useSidebarDrawer` already provide docked vs
  drawer presentation and one open state. Reuse these owners.
- `NoteShow.vue` owns editor mode, title dictation and title-focus intent;
  its focus handling already asks `NoteToolbar.containsFocusTarget`,
  including portaled overflow. `NoteToolbar.vue` owns commands and renders
  the Assimilation panel as a sibling of its navigation. Relocate only
  control DOM; retain this component ancestry and lifecycle.
- `NoteToolbarMoreOptions.vue` measures its actual button row through
  ResizeObserver, caches measured widths, and supplies pinning to
  `noteToolbarOverflow.ts`. Reuse that algorithm and ordered commands;
  change the available space, not the action model.
- `BreadcrumbWithCircle` / `BasicBreadcrumb` already supply notebook,
  parent-folder, Circle/Bazaar links and horizontal scrolling. Reuse them.
  `NoteUndoButton` and `noteUndo` already own global history and navigation.
- Remote trunk advanced to `fa509e45f62967c4081255690af9c4602edcde6f` during
  preparation. Reading its `BookReader.vue`, `BookReadingShell.vue`, and
  `bookReadingBarTarget.ts` found the same placement solution: a typed
  injected DOM-target ref lets page-owned controls render in a persistent
  GlobalBar through Teleport. Modularize that placement key for the current
  notebook use rather than add another header placement protocol. Book
  reading state and note editing state remain with their respective pages.
  The remote reading located the candidate; the owned-checkout observation
  below settled its reuse premise. Slice 1 still owns retained acceptance
  proof after its actual change.
- Cross-file consumers: note toolbar test mounts/width helpers, title/body
  dictation helpers, note page autosave/image/conversation tests, sidebar
  creation helpers, and `e2e_test/start/pageObjects/noteToolbarOverflow.ts`.
  The E2E helper queries `data-note-toolbar` globally; preserve that command
  identity while updating enclosing test mounts to the actual page bar.
  `noteChromeToolbarNavClass` still serves sidebar and main-column creation
  toolbars; it is shared, not deletion residue.

The Accepted ADR index and in-file statuses agree for ADRs 0000–0007;
no supersession or conflicting decision was found. Relevant constraints:

- [ADR 0001 — Ubiquitous language](../../../docs/adrs/0001-ubiquitous-language.md):
  Conversation, Export, note/property context, and Book retain their meanings.
- [ADR 0004 — Notebook Markdown](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md):
  authored titles, body, properties, and links retain their representation.
- [ADR 0005 — Web routes](../../../docs/adrs/0005-web-routes-accepted.md):
  named locations, note/property route-family continuity, conversation query.
- [ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md):
  browser proof uses disposable test state, never Development data.

[North Star: One notebook tree](../../NORTH-STAR.md#one-notebook-tree) supports
the existing folder context; no new architectural direction is required.

## Decisive premises and observations

All runtime observations below ran in this owned preparation checkout on
2026-10-20, with headless Chromium and mocked backend/audio boundaries. The
first four used `ad2a592d06`; the last used that revision plus the exact
published frontend delta from `fa509e45f62967c4081255690af9c4602edcde6f`.
Every scratch product edit and spec was reverted before preparation recording.
No application stack, real microphone, or paid service was used.

| Premise and consuming operation | Literal observation and result | Planning consequence |
| --- | --- | --- |
| Moving only the command DOM can preserve title-focus intent, dictation insertion/save, body targeting, and busy states | Disposable `tests/notes/PreparationNoteHeaderProbe.spec.ts` mounted real NoteShow and GlobalBar together; scratch NoteToolbar teleported only its navigation to a header target. `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/PreparationNoteHeaderProbe.spec.ts` passed 11 tests in 3.28s. Ten reused title-toolbar scenarios, including title save, selected text replacement, delayed startup, conversion lock, and fresh body recording, reached their existing outcomes. One extended title scenario narrowed to 320px during recording and widened to 1280px before Stop. | Use DOM placement while preserving Vue ownership; slice 2 promotes the relevant integrated observations into maintained proof. |
| Overflow placement remains usable and Assimilation content remains note-owned after relocation | The same scratch run opened More at 320px, observed the portaled menu within the viewport, selected Assimilate, observed its pressed header control and real settings inside the note's panel shell. | Teleport the navigation only; retain panel content below the bar. Final long-trail, simultaneous pinned-tool, and actual-layout acceptance belongs to slices 2–3. |
| Header geometry proof distinguishes the proposed placement | With scratch Teleport disabled, `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/PreparationNoteHeaderProbe.spec.ts -t 'inserts at the title caret'` failed: Stop's bottom was 87px, beyond the bar's 55px bottom. | Keep geometric membership in the shared row as positive proof; component presence alone is insufficient. |
| Current toolbar/voice/breadcrumb/history fixtures reach their observable boundaries in this checkout | After reverting scratch files, `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteToolbar tests/notes/NoteShow.toolbarTitleVoiceInput.spec.ts tests/toolbars/NoteUndoButton tests/toolbars/Breadcrumb.spec.ts` passed 12 files / 73 tests in 4.93s. Setup/assertions were read, including named-route Conversation, global Undo API/destination, action overflow, and title save. | This is a baseline, not acceptance of future header integration. Do not assume isolated mounting proves page placement; slice 2 owns integrated proof. |
| The newly accepted Book target can supply the shared placement primitive while page-owned behavior survives | Applied `git diff --binary HEAD..fa509e45f62967c4081255690af9c4602edcde6f -- frontend` as a disposable patch in this owned checkout. `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/BookReadingPage tests/components/book-reading tests/notes/NoteToolbar tests/notes/NoteShow.toolbarTitleVoiceInput.spec.ts tests/toolbars/NoteUndoButton tests/toolbars/Breadcrumb.spec.ts` passed 25 files / 158 tests in 10.18s. Then a scratch generalization of `bookReadingBarTarget` to `pageBarTarget`, including the three import/provider consumers, passed `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/BookReadingPage tests/components/book-reading`: 13 files / 85 tests in 5.63s, reaching Book loading, failure, navigation, marking, and layout outcomes. Reversed the generalization and accepted delta; only planning files remained changed. | Slice 1 reuses this bounded four-file change with its real accepted-trunk basis. No missing checkout observation is carried into readiness. |

The remaining phone/tablet layout is delivery work, not an assumed existing
capability. The probe settles the main ownership risk, not usability on a
physical device. No decisive premise is deferred to an early probe.

## Current decisions

- One typed page-bar target is the placement seam for both current consumers.
  Generalize the Book-specific placement key and update its consumers in
  slice 1. The target holds no command state. No registry, global active-note
  store, or platform-specific command model is needed.
- The notebook layout provides its own target. NoteShow keeps NoteToolbar
  mounted below that provider; only the command navigation renders at the
  target. The Assimilation panel stays in note content. Target readiness and
  cleanup follow the actual parent/child mount lifecycle, including loading,
  minimized Conversation, note switching, and leaving the notebook layout.
- Keep a shrinkable, bounded context region and an independently measured
  tool region. Reserve space for current pinned tools, More, the two leading
  controls, and trailing Undo. Ancestor text yields before these controls.
- Use the existing 768px dock/drawer boundary for the compact ancestry line.
  It belongs below the bar in the content area. Use `assets/page-bar.scss`
  and the current SidebarDrawer safe-area positioning; do not introduce a
  second header-height constant.
- Slice 2's interim context may stay in a bounded horizontally scrolling
  header region at narrow widths; slice 3 moves it below the bar. Commands
  and ancestor destinations must stay usable throughout this interim state.
- Preserve current command labels, shortcuts, authorization, and marker
  identities. Relocation requires retargeting DOM consumers, not changing
  domain operations. A shared CSS change to GlobalBar must preserve all page
  slots, including Recall and Book controls.

## Outside-in proof ownership

| Final promise / affected consumer | Owner and observable proof |
| --- | --- |
| Page-owned Book controls retain loading/error/reading continuity through placement modularization | Slice 1: existing `tests/pages/BookReadingPage*.spec.ts` and Book reading control/layout specs, after updating their real target providers. Observe launcher and reading controls through the page boundary. |
| One shared command row at phone/tablet/desktop widths, order, real overflow width, usable More and active pinning | Slice 2: promote `tests/notes/NoteToolbar.realLayout.spec.ts` to a notebook-layout + NoteShow mount (or a cohesive `tests/layouts/NotebookWorkspaceHeader.spec.ts`). At 320, 390, 768, and 1280px observe control rectangles in the shared row, select an overflow action, resize, and observe the corresponding dialog/editor result. Keep focused overflow policy tests. |
| Draft/editor mode and authored title survive resizing; title/body dictation uses its intended target; startup/conversion/retry controls stay directly reachable; simultaneous voice and Assimilate fit | Slice 2: integrated title/body recording and editor draft scenarios, adapted from `NoteShow.toolbarTitleVoiceInput.spec.ts`, `NoteToolbar.voiceInputOverflow.spec.ts`, and the voice preservation fixtures. Use real NoteShow ownership, mock only API/audio. Assert saved title/body or retained draft, plus visible active controls at the narrow width. |
| Assimilation content remains with the note and its toggle remains active | Slice 2: select Assimilate through overflow, observe settings in the note content region and the pressed header toggle, then resize. Retain `NoteToolbar.panels.spec.ts` command proof. |
| Permissions, shortcuts, Conversation/property route and query, note changes and maximized Conversation lifecycle | Slice 2: current toolbar permission/shortcut and panel tests plus real notebook-layout route transitions; observe allowed Conversation, actively enforced editing restriction, correct named property/query destination, and next note's working commands. Retain `NoteShowPageConversation.spec.ts` and note autosave/image consumers. |
| Latest global history operation, including another note, retains confirmation and destination | Slice 2: header mounted with A open and history for B; click Undo, confirm, observe B's mutation and named-route destination. Reuse `NoteUndoButton.actions.spec.ts` fixtures and retain its focused action/confirmation/visibility proof. |
| Long ancestry stays reachable without consuming tool space; narrow context below bar, wide context inside it | Slice 3: actual notebook layout with long notebook and folder names, scroll to a parent and navigate by its real link; observe named folder/notebook route. Check context and pinned-control geometry across the existing breakpoint. Keep Circle/Bazaar breadcrumb cases. |
| Contents state selects docked pane/drawer; New note retains context; New folder/sort and non-note page controls survive | Slice 3: toggle the actual contents control at narrow/wide widths, invoke New note from the sidebar then header and observe the same parent-folder context; exercise notebook/folder/attachment creation placement. Existing `tests/notes/sidebar`, relevant page specs, and Recall/Book slot checks retain their actual controls. Observe drawer top relative to the shared bar, including its safe-area expression. |

All proof above observes surviving behavior or actively enforced access rules.
Removal acceptance is a separate reading: sweep product code, tests, E2E,
packages, docs, and agent guidance for the replaced Book-specific target names
and any identifiers discovered to belong only to the separate action-bar
presentation. Delete transitive unused consumers. Retain shared toolbar
markers and helpers still used by surviving commands. Record the literal
search and classified result; add no permanent absence assertion. Preserve
dated observation provenance in planning until ordinary wrap-up.

## Ordered slices

### 1. Page-bar placement preserves page-owned control lifecycles
Type: Structure
Status: done
Accepted (2026-10-20): `frontend/src/composables/pageBarTarget.ts` exports
`pageBarTargetKey`; BookReader provides it, BookReadingShell injects it, and
the PDF AI-reorganize fixture provides it. `env -u NODE_ENV CURSOR_DEV=true nix
develop -c pnpm frontend:test tests/pages/BookReadingPage
tests/components/book-reading` passed 13 files / 85 tests; both typechecks
exit 0. Sweep reading: `grep -rn -i bookReadingBarTarget --exclude-dir=node_modules --exclude-dir=.git .`
hits only this plan's dated evidence.
Proof: Book page loading, failure, and reading controls remain operable through
the existing high-level specs after the target key/module and all providers,
injections, and fixture imports are reconciled. Run the affected Book page and
component suites and both frontend typechecks.

Internal change: modularize the existing typed Book bar-target ref as a
page-bar placement primitive. Preserve all Book behavior; this immediately
enables slice 2's note placement. Do not relocate Book state or add a command
framework. This slice starts from the current accepted Book target, not a
reconstruction of pre-correction Book chrome.

Sizing hypothesis: about 5 minutes including focused tests and local cleanup;
the change is one key/module responsibility and its consumers.

### 2. Operate the current note from the shared top bar
Type: Behavior
Status: planned
Proof: integrated header geometry/overflow, editing/dictation, Assimilation,
permission/route, lifecycle, and global Undo observations owned above, plus
the affected toolbar and note-page suites. The geometric assertion fails
before relocation, as observed in preparation. Both frontend typechecks.

Behavior: signed in on a note → use its header commands and resize → the
existing action works in the shared row, overflow remains selectable, active
tools stay directly accessible, and editing/dictation/panel state retains its
note owner. History still performs its own global operation. Leaving or
minimizing the note releases its controls through the existing lifecycle.

Implement the notebook target and control-only placement; replace the separate
action-bar presentation and reconcile test harnesses/selectors in this same
slice. Include removal sweep reading. Preserve the existing shared classes
needed by sidebar and main-column creation. Use a bounded scrollable context
region until slice 3 supplies the compact line.

Sizing hypothesis: 7–9 minutes including focused proof and cleanup. Scrutinized
above the five-minute target: placement, focus, and editor/panel lifetime form
one coupled outcome and share the integrated mount/proof loop; separating
their preservation would deliver a broken note lifecycle. The scratch result
reduces integration uncertainty. Stop and finer-decompose if it exceeds ten
minutes; do not postpone a lost editing or access promise to slice 3.

### 3. Reach folder context while note tools retain their space
Type: Behavior
Status: planned
Proof: the long-ancestry and contents/creation/page-context observations above
at 320/390, either side of 768, and 1280px; current breadcrumb/sidebar and
affected notebook, folder, attachment, property/conversation, Recall, and Book
consumer suites. Both frontend typechecks.

Behavior: a note in a long folder trail → narrow or widen the window and
toggle contents → compact ancestry sits above the note on narrow windows,
wide ancestry sits before the commands, parents remain reachable, the
independent contents pane/drawer follows its toggle, and creation retains its
existing folder context. Pinned commands, More, launcher, and Undo fit while
the trail yields space. Non-note screens keep their appropriate controls.

Use the existing breadcrumbs and drawer state. This replaces slice 2's
bounded-in-header narrow context and completes the approved arrangement.

Sizing hypothesis: 6–8 minutes including focused proof and cleanup. The
context width, position, and drawer/creation interactions share one layout
boundary and proof loop; no separate platform model is justified. Stop and
finer-decompose above ten minutes, or revisit the story if the one-hour
hypothesis fails.

## Verification and delivery

Follow `AGENTS.md` and the frontend, frontend-component, frontend-testing,
and unit-testing skills. Focused browser proof uses
`CURSOR_DEV=true nix develop -c pnpm frontend:test <paths-relative-to-frontend>`.
New layout tests use actual browser geometry, not mocked control widths;
retain synthetic widths only for the overflow policy tests.

Before accepting each frontend change, run both required typechecks on the
same content:

```sh
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
```

Run the whole frontend suite at final acceptance: the change affects the
shared GlobalBar and distributed page/test-harness consumers, so this is
broader local proof justified by the affected boundary. No new E2E capability
or manual-testing session is required; retain E2E command selectors and let
ordinary hosted CI cover those existing journeys. API generation is not
anticipated. If components are added/deleted, regenerate `components.d.ts`
with the frontend skill's Vite command.

For every delivered slice apply required Jidoka and a fresh
dough-post-change-refactor agent, then any needed generation; the coordinator
runs `./scripts/run.sh pnpm format:changed` once, updates this plan without a
second routine formatting pass, commits through the assigned agent entry
point, and pushes under dough-execute-plan. The commit hook is check-only.
These are execution gates, not authorization to execute during preparation.

## Learnings

The ownership risk has a discriminating browser observation. Trunk's newly
landed Book control placement supplies an existing primitive to modularize,
rather than a second header model. Runtime acceptance of that reconciled
Book path and of the final notebook layout remains with its owning slices.
