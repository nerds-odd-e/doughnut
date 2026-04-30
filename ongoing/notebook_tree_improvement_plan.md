# Notebook Tree Improvement Plan

## Purpose

Improve the notebook sidebar tree so large notebooks load and navigate smoothly after the removal of notebook head notes.

Desired behavior:

- `GET /api/notebooks/{notebook}/root-notes` returns only top-level notes, without their children.
- Sidebar child-count badges stay as `...` until that branch is loaded.
- Navigating between notes in the same notebook keeps the existing tree visible and incrementally fills missing branches from the active note upward.
- The sidebar remembers the last notebook tree while the next note route resolves, avoiding blank states and flashes.

## Current Understanding

`NotebookController.listNotebookRootNotes` currently maps root `Note` entities through `NoteRealmService.build`. `NoteRealm.getChildren()` delegates to `note.getChildren()`, so root-note responses include each root note's immediate children. That payload is too large for a root listing.

The frontend stores loaded note realms in `NoteStorage`. `SidebarInner` loads root notes into local `rootNotesList`, while expanded branches use `getNoteRealmRefAndLoadWhenNeeded(noteId)` to lazily fetch branch children when `children` is missing.

The sidebar tree is keyed by active note id in `Sidebar.vue`. A same-notebook note navigation remounts `SidebarInner`, clears local root-note state and expanded ids, then reloads from scratch. This is likely the main cause of visible flashes and broken gradual population.

The active note already carries an ancestor chain through `note.noteTopology.parentOrSubjectNoteTopology`. That chain identifies which ancestors must be expanded. Each expanded ancestor still needs its own note realm loaded so sibling lists are available at that level.

## Design Direction

Keep the root-notes API contract as `NoteRealm[]`, but make `children` absent for this endpoint. The generated TypeScript already treats `NoteRealm.children` as optional, and the sidebar already interprets missing `children` as an unknown branch.

Keep one sidebar tree instance per notebook, not per active note. Active note changes should update selection and expanded ids inside the existing tree.

Treat missing `children` as "unknown branch" in the sidebar. Unknown branches show `...`; expanding or auto-expanding a branch loads the note realm and replaces `...` with the real count.

Remember the last sidebar notebook id while a new in-notebook note is loading. Clear or replace the tree only when the route leaves notebook/note context or resolves to a different notebook.

## Phase 1: Shallow Root Notes

**Type:** Behavior

**Pre-condition:** A notebook has multiple top-level notes, and at least one root note has children.

**Trigger:** The frontend or another API client calls `GET /api/notebooks/{notebook}/root-notes`.

**Post-condition:** The response includes only top-level note realms. Returned realms do not include `children`, so the sidebar badge displays `...` until that root branch is loaded later.

Implementation notes:

- Adjust backend root-note serialization so `NoteRealm.children` is omitted for this endpoint.
- Keep `showNote` behavior unchanged: a fully loaded note realm should still include `children`.
- Prefer a cohesive DTO or `NoteRealm` construction option over ad hoc JSON filtering in the controller.
- Do not add a child-count field yet; the requested UI is the existing `...` placeholder.

Tests:

- Extend `NotebookControllerTest.ListNotebookRootNotes` to assert that a returned root with children has no `children` data, while its child note is not returned as a root.
- Add or extend a frontend sidebar test so shallow root notes from `listNotebookRootNotes` render `...` for the badge.
- Run targeted checks:
  - `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.controllers.NotebookControllerTest`
  - `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/Sidebar.spec.ts`

## Phase 2: Stable Sidebar Tree Per Notebook

**Type:** Behavior

**Pre-condition:** A user is viewing a note inside a notebook, and the sidebar tree has already loaded and expanded some branches.

**Trigger:** The user navigates to another note in the same notebook.

**Post-condition:** The sidebar keeps the existing notebook tree visible while the new note resolves. It updates the active selection after the new realm arrives, without remounting the whole tree, losing expanded branches, or reloading root notes.

Implementation notes:

- Change the sidebar tree key so it is stable for a notebook.
- Do not key the tree by active note id.
- Keep active note as reactive input to the existing tree instance.
- Preserve expanded ids across active note changes within the same notebook.
- Keep the tree reset boundary at notebook changes, not note changes.

Tests:

- Add a frontend test around the layout/sidebar path that starts on one note, delays the next note load, changes to another note in the same notebook, and asserts the previous tree remains visible before the delayed request resolves.
- Assert `listNotebookRootNotes` is not called again for a same-notebook active note change.
- Keep timing-sensitive no-flash behavior in frontend tests; use E2E for the user-visible final tree.
- Run:
  - `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/Sidebar.spec.ts tests/pages/NoteShowPage.spec.ts`
  - `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_topology/note_tree_view.feature`

## Phase 3: Gradual Ancestor Population

**Type:** Behavior

**Pre-condition:** A user opens a deep note directly, or navigates to a deep note whose ancestor branches are not loaded in the sidebar cache. Root notes are shallow and branch children are unknown.

**Trigger:** The active note realm is loaded.

**Post-condition:** The sidebar expands the active note's ancestor chain and loads only missing ancestor branches. As each branch loads, the tree fills from the root toward the active note. It does not reload the whole root tree.

Implementation notes:

- Use `activeNoteRealm.note.noteTopology.parentOrSubjectNoteTopology` as the source of ancestor ids.
- For each ancestor that must be expanded, invoke `getNoteRealmRefAndLoadWhenNeeded(ancestorId)` only if that ancestor's `children` are unknown.
- Avoid replacing existing cached realms with thinner topology-only ancestor data.
- Preserve lazy branch loading for manual expand/collapse.
- Keep active-path loading logic close to sidebar tree state rather than spreading it across page components.

Tests:

- Add a frontend sidebar test where root notes are shallow, the active note is deep, and only needed ancestors are loaded.
- Assert the final visible order includes the root, ancestor siblings, the active branch, and no unrelated deep branches.
- Assert root notes are fetched once and ancestor `showNote` calls are scoped to missing expanded ancestors.
- Extend `e2e_test/features/note_topology/note_tree_view.feature` with a direct deep-note scenario if the existing deep-note scenario does not catch the regression.
- Run:
  - `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/Sidebar.spec.ts`
  - `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_topology/note_tree_view.feature`

## Phase 4: Remember Last Notebook Tree While Resolving Routes

**Type:** Behavior

**Pre-condition:** A user is navigating inside a notebook and clicks a note link, sidebar link, or browser route whose note realm has not resolved yet.

**Trigger:** The route changes and the next note load begins.

**Post-condition:** The sidebar continues to show the last known notebook tree until the new route resolves. If the note resolves inside the same notebook, the tree updates gradually from the active note to its ancestors. If it resolves to a different notebook, the sidebar switches to that notebook tree once the new notebook id is known.

Implementation notes:

- Separate "currently resolving active note" from "notebook tree to display".
- For notebook slug routes, `notebookId` is available immediately and can become the displayed tree id before the note realm resolves.
- For ambiguous basename routes, keep the previous notebook tree during loading, then replace it only after the resolved realm identifies its notebook.
- Clear remembered sidebar state only when leaving notebook/note surfaces.
- Prefer in-memory sidebar state first. Use `localStorage` only if the product should remember the last tree across page reloads.

Tests:

- Add frontend layout/page tests for notebook slug routes and ambiguous note routes with delayed note resolution.
- Assert the previous sidebar remains visible during the delay and updates after resolution.
- Add an E2E scenario to `note_tree_view.feature` for navigating from one visible sidebar note to another in the same notebook and seeing the tree remain populated after navigation.
- Run:
  - `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/NoteShowPage.spec.ts tests/notes/Sidebar.spec.ts`
  - `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_topology/note_tree_view.feature`

## Phase 5: Cleanup And Regression Coverage

**Type:** Structure

Run this only after Phase 4 behavior is green.

Goals:

- Remove duplicated sidebar tree-loading logic introduced while making behavior pass.
- Keep root-note loading, branch loading, active-path expansion, and remembered notebook state as one cohesive sidebar capability.
- Rename tests or helpers if needed so permanent names describe notebook tree behavior, not delivery phases.
- Remove obsolete comments about the former notebook head-note model if they no longer describe current behavior.

Verification:

- Run targeted frontend and E2E checks from earlier phases.
- If shared sidebar state helpers changed broadly, run:
  - `CURSOR_DEV=true nix develop -c pnpm frontend:test`

## Open Questions

- Should "remember the last notebook" survive browser refresh, or is route-to-route memory enough? This plan assumes route-to-route memory only.
- Should root notes eventually expose a cheap child count from the backend, or should the badge remain `...` until a branch is loaded? The requested behavior is `...`.
- Should the notebook overview page always show shallow root notes even when an `index` note exists, or should `index` remain the initial active note for toolbar/edit behavior? This plan keeps existing notebook page behavior except for tree smoothness.
