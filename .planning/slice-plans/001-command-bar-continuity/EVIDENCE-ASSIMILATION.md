# Assimilation presentation evidence

**Identity:** SEED-080#story-1, slice 4  
**Date:** 2026-10-20  
**Base:** b3cd0c7e273378f715c39f239b2ffe1d0be24cd2  
**Candidate:** uncommitted changes in the authorized story workspace/branch.

## Responsibility and caller trace

`useAssimilationView` retains note-panel intent and adds scope-owned reactive
presentation registrations. A registration derives its presented note from the
actual consumer's note identity and visibility; disposal removes that source.
Navigation uses the existing `noteRouteFamilyNoteId` and current registrations.
It does not inspect property contents, infer presentation from a route, or
remember sequence entry.

`NoteToolbar` still owns opening/closing the note panel, read-only refusal,
loading and reload; its actual mounted `AssimilationPanel` registers that
presentation. `RichFrontmatterPropertyRow` still mounts its panel only for a
focused editable stored row. `RichFrontmatterPropertyPanel` shares its existing
AssimilationModes eligibility computation with presentation registration,
additionally requiring a saved note and the injected details disclosure to be
revealed for that same note. This accounts for mounted `v-show` details.
Unsaved list editing still renders its existing list UI without claiming a
learning presentation. `NoteShow`/`NoteTextContent` retain page, minimization,
editor, note and disclosure lifetime ownership.

`useNavigationItems` makes this current presentation Assimilate context and
keeps its existing badge/count model. Ordinary Note yields to it; the existing
Resume branch stays first. `DonutMenu` and `NavigationItem` consume this model
and retain their existing compact direct next action and independent menu opener.
`useGoToNextAssimilation` remains the unchanged owner of backend next-unit
loading, count updates, completion messages and note/property destinations.
No component file was added or deleted and no API contract changed, so no
generation trigger applies.

## Observations and proof ownership

`frontend/tests/layouts/NotebookWorkspaceAssimilation.spec.ts` mounts real
routed workspace components with real presentation, router and next-unit code.
Backend-only mocks provide notes, recall info and next units. Its assertions
observe the visible compact button's accessible identity; next-unit assertions
also observe its due count and the returned route/visible property learning UI.
No test supplies assimilation presentation as its expected result through store
setup.

- `identifies an opened note panel...`: actual toolbar clicks open and close it;
  route switching gives the second note ordinary Note context.
- `follows direct property learning...`: starts at a real eligible property;
  actual details toggle hides/reveals it, the row toggle closes/reopens it,
  switching notes and leaving for Bazaar restore their context.
- Parameterized ordinary browsing uses missing property, canonical `note_level`
  (non-learning), and read-only `topic` as distinct input boundaries.
- `keeps eligible Resume...`: starts with visible property learning and supplies
  existing recall queue/progress preconditions; visible Resume takes precedence.
- `follows next-unit note to property...`: opens the menu and starts actual next
  assimilation, receives note/count 7, activates the compact direct action, then
  observes backend-returned property/count 6. Neither outcome uses intent setup.

The first isolated run passed six cases and exposed an incorrect test fixture
assumption: `image` is eligible under the existing property-learning policy.
Reading `isNoteLevelPropertyKey` established `note_level` as the intended
non-learning fixture. Correcting only that fixture preserved product policy;
the subsequent affected run passed all seven routed cases.

## Terminal verification

Commands ran with checkout-bound Nix tooling, headless Chromium, no held stack.
The complete affected run passed **189 tests in 28 files**, exit 0:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars tests/layouts/NotebookWorkspaceAssimilation.spec.ts tests/layouts/NotebookWorkspaceHeader.spec.ts tests/layouts/NotebookWorkspaceContext.spec.ts tests/notes/NoteToolbar.panels.spec.ts tests/notes/NoteTextContent.detailsDisclosure.spec.ts tests/components/form/RichMarkdownEditor.listPropertyMemoryTracking.spec.ts tests/components/form/RichMarkdownEditor.listPropertyLayout.spec.ts tests/composables/useGoToNextAssimilation.spec.ts tests/composables/useAssimilationView.spec.ts tests/components/recall/AssimilationPanel.spec.ts
```

Retained scalar-property memory/location/read-only/focus suites additionally
passed **25 tests in 4 files**, exit 0:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyMemoryTracking.spec.ts tests/components/form/RichMarkdownEditor.propertyLocation.spec.ts tests/components/form/RichMarkdownEditor.readOnlyProperties.spec.ts tests/components/form/RichMarkdownEditor.propertyFocusAndPresets.spec.ts
```

Both required frontend typechecks passed against final frontend content, exit 0:

```bash
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
```

Independent refactoring replaced the ordinary-browsing fixture's direct
read-only mutation with the existing `NoteRealmBuilder.readonly` input.
Production code, backend setup and all observations stayed unchanged. The
affected routed suite passed all **7 tests**, exit 0, and both typechecks above
passed again after this fixture cleanup:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/layouts/NotebookWorkspaceAssimilation.spec.ts
```

Native iPad software-keyboard behavior remains unverified under the owner's
previous acceptance update. This slice changes no input/focus ownership.
