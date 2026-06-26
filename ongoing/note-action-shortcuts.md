# Note Action Keyboard Shortcuts

## Goal

Add global single-key shortcuts that trigger note-action buttons, sharing one
cohesive mechanism so future shortcuts cannot drift into duplicated or
inconsistent key handling.

| Key | Action                         | Button it mirrors |
|-----|--------------------------------|-------------------|
| `n` | New note                       | `NoteNewButton` (toolbar or sidebar, whichever is visible) |
| `m` | Toggle edit mode markdown/rich | `NoteToolbar` edit-as-markdown button |
| `e` | Export note                    | `NoteMoreOptionsActions` export button |
| `d` | Delete note                    | `NoteMoreOptionsActions` delete button |

Each shortcut: fires only while its button is mounted/available, runs the exact
same handler as a click, and is suppressed while typing in an editable field.

## Cohesive design (introduced incrementally, not up front)

- `frontend/src/utils/keyboardShortcuts.ts`
  - `ShortcutAction` union — the single source of truth, grows one entry per phase.
  - Central binding table mapping each action to a key matcher (plain letters for
    new actions; later `Ctrl/Cmd+F` and `Ctrl/Cmd+Shift+F` fold in here too).
  - `isEditableTarget(target)` guard so plain-letter shortcuts never fire in
    `input` / `textarea` / `select` / `contenteditable`.
  - Per-action handler **stack** (`Map<ShortcutAction, Array<() => void>>`);
    top-of-stack wins so the most-recently-mounted button is targeted (this is how
    `n` follows whichever new-note button is visible). Mirrors today's opener stack.
  - One lazily-installed `document` keydown capture listener; on a match with a
    non-empty stack it calls `preventDefault()` + `stopImmediatePropagation()` and
    runs the top handler.
- `frontend/src/composables/useKeyboardShortcut.ts`
  - `useKeyboardShortcut(action, handler, isActive?)` registers while active and
    cleans up on unmount/inactive (same watch pattern GlobalBar uses today).

Each component registers an action by **reusing its own button handler**, so there
is no duplicated behavior:
- new -> `NoteNewButton.vue` -> its `PopButton.openDialog()`
- toggle -> `NoteToolbar.vue` -> emit `edit-as-markdown` with `!asMarkdown`
- export -> `NoteMoreOptionsActions.vue` -> export `PopButton.openDialog()`
- delete -> `NoteMoreOptionsActions.vue` -> `deleteNote` from `useNoteDeleteFlow`

## Phases (stop-safe, value-first)

Phases 1-4 each deliver one usable shortcut. Phase 5 removes the pre-existing
duplicated shortcut code by folding it into the same registry. The minimal
registry created in Phase 1 is used immediately by Phase 1 (not speculative); it
only grows by one enum + one binding per later phase.

### Phase 1 — Press `n` to open New note (Behavior) — DONE

- **Scenario:** With a note shown and a new-note button available, pressing `n`
  (not while typing in a field) opens the new-note dialog, same as clicking it.
- **Work:**
  - Create `keyboardShortcuts.ts` with `ShortcutAction = "note-new"`, the binding
    `note-new` -> plain `n`, `isEditableTarget` guard, handler stack, lazy global
    listener, `registerShortcut`.
  - Create `useKeyboardShortcut` composable.
  - Register `note-new` in `NoteNewButton.vue` (add a ref to its `PopButton`).
- **Tests** (`frontend/tests/...`, capability-named, e.g. note new-button spec):
  - `n` opens the new-note dialog when the button is mounted.
  - `n` is ignored while focus is in an input/textarea.
- **Verify:** `CURSOR_DEV=true nix develop -c pnpm frontend:test <touched spec(s)>`.

### Phase 2 — Press `m` to toggle markdown/rich edit mode (Behavior) — DONE

- **Scenario:** On an editable note, pressing `m` toggles between markdown and rich
  editing, same as the toolbar button.
- **Work:** add `note-toggle-edit-mode` -> plain `m`; register in `NoteToolbar.vue`
  (handler emits `edit-as-markdown` with `!props.asMarkdown`, active when `!readonly`).
- **Tests:** `m` toggles edit mode when not readonly; no toggle when readonly.

### Phase 3 — Press `e` to open Export (Behavior) — DONE

- **Scenario:** With note actions available, pressing `e` opens the export dialog.
- **Work:** add `note-export` -> plain `e`; in `NoteMoreOptionsActions.vue` add a
  ref to the export `PopButton`, register handler = `openDialog()`. Component is
  mounted in both inline and collapsed-dropdown layouts, so the shortcut is
  available in both.
- **Tests:** `e` opens the export dialog.

### Phase 4 — Press `d` to Delete (Behavior) — PLANNED

- **Scenario:** With note actions available, pressing `d` starts the delete flow
  (confirmation/options), same as the delete button.
- **Work:** add `note-delete` -> plain `d`; register in `NoteMoreOptionsActions.vue`
  with handler = `deleteNote` from the existing `useNoteDeleteFlow` (no logic
  duplicated).
- **Tests:** `d` invokes the delete flow.

### Phase 5 — Consolidate existing search/link shortcuts (Structure) — PLANNED

- **Goal:** remove the two pre-existing ad-hoc implementations so all shortcuts use
  one mechanism (the explicit "no duplicate / no inconsistency" requirement).
- **Work:**
  - Add `note-search` -> `Ctrl/Cmd+F` and `note-link` -> `Ctrl/Cmd+Shift+F` to the
    central binding table.
  - `GlobalBar.vue`: register `note-search` (handler `openNoteSearch`, active when
    `user` set) instead of the opener stack.
  - `NoteToolbar.vue`: register `note-link` (handler `linkPopButtonRef.openDialog()`,
    active when `!readonly`); remove its `window` keydown listener + `isLinkToolbarShortcut`.
  - Delete `frontend/src/utils/globalNoteSearchShortcut.ts` and
    `frontend/src/composables/useGlobalNoteSearchKeyboardShortcut.ts`.
  - Remove the `useGlobalNoteSearchKeyboardShortcut(user)` call/import in
    `frontend/src/DoughnutApp.vue` (registry self-installs its listener).
- **Verify (behavior unchanged):** existing `frontend/tests/toolbars/GlobalBar.spec.ts`
  (`Ctrl+F`) and `frontend/tests/notes/NoteToolbar.spec.ts` (`Ctrl+Shift+F`) stay green.

## Discipline notes

- Tests named by capability, never by phase number.
- One intentionally failing test at a time (TDD).
- Commit + push + let CD deploy at each phase boundary (deploy gate).
- Run targeted frontend specs for touched files, not the whole suite.
