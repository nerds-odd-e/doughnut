# Keep the mobile note toolbar on one row with an on-screen options menu

**Identity:** SEED-075#story-1

**Source:** [refined story](../../seeds/SEED-075-mobile-note-toolbar.md#story-1)

## Goal and scope

A note author on a phone, or in any narrow note column, sees every toolbar
control on one row and can read and select each action that moved into More
options.

Included: the toolbar shows only the actions that fit on one row together with
More options, at every toolbar width; the opened More options menu lies inside
the screen's left and right edges.

Excluded: any change to which actions exist, their overflow order, the actions
that stay on the bar while active, or the toolbar when everything fits. By
owner decision on 2026-10-12 the toolbar keeps the page's side margins; no
shared notebook page layout changes. A menu taller than the screen is deferred.
Other menus that use the shared placement gain the on-screen behavior
naturally; this plan verifies it only for More options and keeps their
existing tests green.

No execution is authorized by this preparation.

## Architectural approach and existing solutions

Both rules already have one owner each, and the fix stays inside them:

- **Which actions fit** is owned by `computeNoteToolbarOverflow`
  (`frontend/src/composables/noteToolbarOverflow.ts`), fed by the measurement
  in `NoteToolbarMoreOptions.vue`. The calculation is correct; its input is
  not. The measurement passes the toolbar's inner width including its 8px side
  padding, 16px more than the buttons have. Correct the measured room so it is
  the room the button row really has, and make the row a single non-wrapping
  line so a miscount can never again show as a second row. No second fit rule
  and no phone-specific branch: the same rule serves every width.
- **Where a menu opens** is owned by `computeDropdownPortalStyle`
  (`frontend/src/composables/dropdownPortalPlacement.ts`), shared by every
  `AutoCollapseDropdown` menu. Keep a menu that opens below or above its
  button inside the screen's left and right edges there, not in the toolbar.
  Menus that open to the side of their button are not part of this story.

No Accepted ADR in `docs/adrs/` governs frontend layout, and no North Star
topic in `.planning/NORTH-STAR.md` applies. Nothing consequential is added or
moved.

## Outside-in proof

| Promise (story key example) | Slice | Observable proof |
| --- | --- | --- |
| 390px screen: all controls on one row ending in More options | 1 | Real-layout toolbar test at 390px: every visible control has the same top |
| 390px ↔ 414px: Questions for the note moves between bar and menu, row stays single | 1 | Same test resized to 414px and back |
| 640px: all ten actions on the bar, no More options | 1 | Same test at 640px; existing `NoteToolbar.overflow.spec.ts` stays green |
| Overflow order and active-action pinning unchanged | 1 | Existing `NoteToolbar.overflow.spec.ts`, `NoteToolbar.voiceInputOverflow.spec.ts`, `noteToolbarOverflow.spec.ts` stay green |
| 390px and 320px: opened menu inside the screen, an action selectable | 2 | Real-layout toolbar test: the menu's left and right edges are within the viewport and a listed action receives the click |
| Other menus keep working | 2 | Frontend unit suite stays green |

"Real layout" means the toolbar mounted with the application styles inside a
column like the notebook page's main column (`px-4 container mx-auto`), the
browser viewport set to the stated width, real `ResizeObserver`, and no
replaced element sizes. The existing toolbar tests replace measured sizes with
fixed numbers (`tests/helpers/mockNoteToolbarNavWidth.ts`), so they prove the
overflow order but cannot show wrapping or menu position.

Frontend proof for each slice also requires both typechecks from the
`frontend` skill's "Frontend proof" rule:
`CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit` and
`CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit`.
Run frontend tests with `env -u NODE_ENV` in front.

## Settled premises

All observations were made on 2026-10-12 in this plan's workspace at
`c2e8ffaff2`, with a temporary real-layout browser test run by
`env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test <spec>`.
The temporary test and edits were removed afterwards.

- **The symptom reproduces in a real-layout component test.** Unchanged code
  at 390px: More options at top 40px, the other six controls at top 8px;
  opened menu at left −206px. At 320px: one row, menu at left −5px.
- **Correcting the measured room cures the wrap.** With a temporary edit
  subtracting the toolbar's side padding from the available width: one row at
  every even width from 300px to 560px; 6 controls at 390px, 7 at 414px, all
  ten and no More options at 560px.
- **Keeping the menu's left edge on screen in the shared placement cures the
  menu.** With a temporary edit limiting the below-the-button position to the
  viewport: menu at 8px to 288px at 320px, and no off-screen menu at any even
  width from 300px to 560px.
- **Consumers of the two changes.** With both temporary edits, running
  `tests/notes/` and `tests/composables/useDropdownPortalPosition.spec.ts`
  (386 tests) failed exactly four existing tests:
  - three in `tests/notes/NoteToolbar.overflow.spec.ts` ("keeps all
    more-options on the bar when they fit", "overflows delete first…",
    "overflows Export then Edit…"), because
    `tests/helpers/mockNoteToolbarNavWidth.ts` supplies a fixed toolbar inner
    width while the real side padding was then subtracted from it. The helper
    must supply the room the changed measurement reads.
  - "places panel below and right-aligned for bottom end" in
    `tests/composables/useDropdownPortalPosition.spec.ts`, whose input places
    the panel outside the test viewport.
  Tests outside those paths that mount the toolbar or another menu were not
  run with the temporary edits; slice 2 runs the whole frontend unit suite for
  that reason.

## Ordered slices

### 1. Toolbar controls stay on one row

Type: Behavior
Status: planned
Proof: A new real-layout toolbar test at 390px asserts every visible toolbar
control shares one top and the last is More options; at 414px Questions for
the note is on the bar, and back at 390px it is not; at 640px all ten actions
are on the bar with no More options. It fails on unchanged code at 390px (two
rows, observed). `tests/notes/NoteToolbar*.spec.ts` and
`tests/composables/noteToolbarOverflow.spec.ts` are green, with
`mockNoteToolbarNavWidth.ts` aligned to the changed measurement. Both
typechecks pass.

Behavior: 390px-wide screen, sidebar closed, editable note → the note toolbar
renders → all controls, ending in More options, are on one row; widening to
414px moves Questions for the note onto the bar and narrowing moves it back,
with one row throughout.

### 2. More options menu opens inside the screen

Type: Behavior
Status: planned
Proof: The real-layout toolbar test at 320px and 390px opens More options and
asserts the menu's left edge is at or right of the screen's left edge, its
right edge at or left of the screen's right edge, and that clicking Trash note
in the menu starts the trash action. It fails on unchanged code at 320px (left
−5px, observed). A focused example in
`tests/composables/useDropdownPortalPosition.spec.ts` covers a panel wider
than the space to the left of its button; the existing bottom-end example
there is given an input that fits its viewport. The whole frontend unit suite
(`env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test`) is
green, because the placement is shared by every menu. Both typechecks pass.

Behavior: 320px-wide screen with actions in More options → the author opens
More options → the whole menu is inside the screen's left and right edges and
selecting Trash note starts the same action as the toolbar button.

## Current decisions

- The toolbar keeps the page's side margins (owner, 2026-10-12).
- The on-screen rule lives in the shared menu placement, for menus that open
  below or above their button.

## Learnings

None yet.
