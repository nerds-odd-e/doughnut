# Mobile Soft Keyboard on Tap-to-Reveal Inputs

## Goal

On touch devices without a physical keyboard, the soft keyboard only appears when
`.focus()` runs **synchronously inside the user-gesture handler** (the tap), while
the browser's transient activation is still alive. Today, every "tap a button →
input appears" flow focuses the input **after** the gesture (deferred via
`nextTick` + `requestAnimationFrame`, and after an async `v-if` mount), so the
keyboard never opens. Users must tap the field a second time.

We want the keyboard to open automatically for these tap-to-reveal flows.

## Root cause (current behavior)

- `frontend/src/utils/focusTarget.ts` → `scheduleFocusTargetWithin` defers `.focus()`
  into `nextTick` → `requestAnimationFrame`. By then, gesture activation is gone.
- `PopButton.vue` opens dialogs with `@click.prevent="show=true"`, so the input is
  `v-if`-mounted on the **next** render — it does not exist during the tap, so it
  cannot be focused synchronously.
- `Modal.vue` `onMounted` also focuses via `requestAnimationFrame`.

## Chosen approach: keyboard primer + focus relay

Add one always-present, visually-hidden but **focusable** text input (the "primer").
On the originating tap, synchronously call `primer.focus()` — this summons the
keyboard **within the gesture**. When the real field later mounts, the existing
autofocus path (`focusAutofocusTargetWithin` / `v-focus` / `PathNameEditor`
autofocus) calls `.focus()` on it, transferring focus from the primer to the real
input. Moving focus between focusable text controls keeps an already-open keyboard
up, so the deferred autofocus path does **not** need to change.

Why a primer instead of pre-mounting the real input: it is the smallest change and
leaves the `v-if` dialog-mounting architecture intact.

Implementation primitives (introduced in Phase 1, reused after):
- A global, visually-hidden focusable `<input>` rendered once near the app root.
  Must NOT use `display:none` / `visibility:hidden` (those block focus); use an
  off-screen but laid-out element.
- A `primeSoftKeyboard()` helper (in `focusTarget.ts`) that focuses the primer
  synchronously, and a no-op / cheap guard on non-touch devices.

## Testing reality

Real soft-keyboard visibility **cannot** be asserted in Cypress/CI or jsdom — it is
device behavior. The observable, testable contract is instead:

1. On the originating tap (synchronously, before any async mount/`rAF` flush), the
   **primer** receives focus.
2. After the dialog mounts, focus **transfers** to the real field (existing
   autofocus behavior — already covered by `Modal.spec.ts` and friends).

Component/unit tests assert (1) and (2). Actual keyboard appearance is a **manual
device-verification** step per phase (real iOS Safari + Android Chrome).

## Phases (value-ordered, stop-safe — one commit per phase/sub-phase)

Each numbered item below is sized to **one small commit**: a single focused change
plus its test, independently green and stop-safe. Phase 1 cannot be split further
without leaving dead code (an unused primer/helper), so the mechanism + its first
caller ship together.

### Phase 1 — Mechanism + search summons keyboard (Behavior) — 1 commit — **DONE**
Covers all `PopButton`-based flows at once (they share the trigger component).
- **Scenario:** On mobile, tapping the search icon in `GlobalBar` opens the keyboard
  with the search field focused.
- **Change (one commit):**
  - Add the global visually-hidden focusable primer `<input>` near the app root.
  - Add `primeSoftKeyboard()` + a touch/pointer guard to `focusTarget.ts`.
  - Call `primeSoftKeyboard()` synchronously from `PopButton`'s button tap handler.
- **Covered by this single wiring:** Search (`GlobalBar.vue`), Link note
  (`NoteToolbar.vue`), New note/notebook/folder (`NoteNewButton`,
  `NotebookNewButton`, `FolderNewButton`).
- **Tests:** Component test asserting the primer is focused **synchronously** on tap
  (before any `rAF`/`nextTick` flush), and a representative flow (search + new note)
  still lands focus on the real field after mount. Keep existing modal-autofocus
  tests green.
- **Manual:** Verify keyboard opens on a real device for search and new note. This
  phase **de-risks the whole approach** — confirm primer-then-transfer keeps the
  keyboard open across the deferred `rAF` autofocus before building Phases 2+.
- **Note:** Keyboard shortcuts (`Ctrl+F`, `Ctrl+Shift+F`) use physical keyboards and
  need no primer; the pointer/touch guard keeps the primer path off that route.

### Phase 2 — Dead wiki link create/link summons keyboard (Behavior) — 1 commit
- **Scenario:** Tapping a dead `[[wiki link]]` opens the chooser; tapping
  "Create a new note named …" or "Link to an existing note" opens the keyboard with
  the title / search field focused.
- **Why separate:** The field is revealed by a **second** in-modal button tap inside
  `NoteDeadLinkCreateModal.vue` (`showCreateForm` / `linkingToExisting`), not a
  `PopButton`. Wire `primeSoftKeyboard()` into those two buttons.
- **Tests:** Component test on `NoteDeadLinkCreateModal` asserting primer focus on
  each button tap; existing focus-transfer to `NoteNewForm` / `SearchForm` green.
- **Manual:** Verify on device for both branches.

### Phase 3 — Folder search/picker summons keyboard (Behavior) — 1 commit
- **Scenario:** Opening the folder search/move/picker dialog
  (`FolderSelector.vue` → `FolderSearchForm.vue`) opens the keyboard with the
  folder search field focused.
- **Why separate:** `FolderSelector` uses its own `@click="searchOpen = true"` + raw
  `Modal` (not `PopButton`), and `FolderSearchForm` focuses its `<input>` only
  **after** an async folder-index load (`watch(foldersLoading)`) — the furthest
  off-gesture case. Wire `primeSoftKeyboard()` into the "Search folders" button tap;
  the primer bridges the async gap and focus transfers when loading resolves.
- **Tests:** Component test asserting primer focus on the opening tap and search
  input focus after loading resolves.
- **Manual:** Verify on device.

### Phase 4 — Remaining tap-to-reveal inputs (Behavior, optional / lower value)
Independent spots; each is its own small commit. Do each only if the UX gap is real
on mobile. No shared dependency between them, so order is by observed value.

- **4a — Notebooks page filter — DROPPED.** `NotebooksPageView.vue` has no
  tap-to-reveal filter toggle: the input is always in the DOM when the catalog is
  non-empty, so a direct tap opens the keyboard without a primer. `onMounted`
  `filterInputRef.focus()` is page-load autofocus (out of scope below). Same
  always-visible pattern on `CircleShowPage.vue` if revisited.
- **4b — Wikidata association dialog — 1 commit.** `WikidataAssociationDialogBody.vue`
  (`input.focus()`). Prime on the tap that opens the dialog/field.
- **4c — Frontmatter "add property" — 1 commit.** Wire the add-property tap that
  reveals the key input (`RichFrontmatterInsertForm.vue` /
  `RichFrontmatterProperties.vue`). Confirm a single tap reveals the input.
- **4d — Frontmatter "edit property value" — 1 commit — **DONE**.** `primeSoftKeyboard()`
  on `pointerdown` in `RichFrontmatterEditablePropertyRow.vue` (skips anchor taps);
  tests in `RichMarkdownEditor.properties.spec.ts`.
- **4e — Spelling recall answer — 1 commit.** `SpellingQuestionDisplay.vue`
  (`v-focus`). Verify first whether the answer field is revealed by a user tap (e.g.
  "next question") vs. an automatic transition — if there is no originating gesture,
  no primer is possible and this item is **dropped**.

- **Out of scope:** Full-page-load autofocus (login/register/profile/circle-join,
  conversation template, access tokens, notebook page name editor) — not
  tap-to-reveal; native first-load autofocus is acceptable.

## Open questions
- Should the primer be a single global element (preferred — one DOM node, shared
  helper) or per-component? Plan assumes global.
- Phase 1 doubles as the device de-risk gate: confirm on a real iOS device that
  primer-then-transfer keeps the keyboard open across the deferred `rAF` autofocus
  before committing to Phases 2+ on the same mechanism.
- Phase 4e (spelling) is conditional on the answer field being revealed by a user
  tap; resolve during that phase, drop the item if the transition is automatic.
