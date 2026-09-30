# Book layout keyboard navigation

Work item: **SEED-059#story-9**
([story](../../seeds/SEED-059-book-reading-uat-fixes.md#story-9)).
Status: **story refined; three Behavior slices planned.**

## Goal and scope

A reader using the keyboard can leave the book layout with Tab, move between
rows with ArrowUp/ArrowDown, choose with Enter, and indent or outdent the same
block repeatedly, without Tab ever changing the saved layout.

Excluded (see the story): Backspace/Delete cancel (unchanged), Home/End/PageUp/
PageDown/type-ahead, tree roles and collapsing, Ctrl/Cmd+Z (story 7), drag,
phones, focus after Cancel or AI reorganize, making the depth keys work in EPUB
(story 10), any backend or generated API change.

## Known architecture

- **One place.** All row keys are on the row `<button>` in
  `BookReadingBookLayout.vue`: `@keydown.tab.shift.prevent` → `blockOutdent`,
  `@keydown.tab.exact.prevent` → `blockIndent`, `@keydown.delete.prevent` →
  `blockCancel`. Drag emits the same two events. The parent wiring
  (`useBookLayoutMutations`, `useBookReadingSession`) does not change.
- **Rows are native buttons.** Enter and Space already fire `click`, which emits
  `blockClick` (choose). Nothing binds arrows.
- **Focus today.** `onOpenLayoutRow` watches `selectedBlockId` and focuses the
  selected row after paint. `changeBlockDepth` re-assigns the same
  `selectedBlockId`, so that watcher never fires again after a depth change.
- **Probable cause of defect 14.** `changeBlockDepth` uses
  `apiCallWithLoading(..., { blockUi: true })`; `DonutApp.vue` then shows
  `LoadingModal`, a `<dialog>` that is torn down with `v-if`. Focus is not
  given back. The fix does not depend on the cause: after the update, focus the
  changed block's row explicitly.
- **Roving tab stop.** One row has `tabindex="0"` and the others `-1`. The stop
  is the row last focused, else the selected block, else the current block, else
  the first block. It follows focus events, so mouse and arrow focus keep it
  current.
- **Story 7 is on trunk.** `BookReadingBookLayout.vue` now also has the Undo
  button; `BookReadingShell.vue` holds the window-level Ctrl/Cmd+Z listener. Both
  stay as they are. The Tab bindings and the Tab step names in the E2E feature
  and page object are unchanged by story 7 (rechecked after rebasing onto trunk
  on 2026-09-30).

## Premises observed

- Only `BookReadingBookLayout.vue` handles Tab in the product:
  `grep -rn "keydown.tab\|Shift+Tab"` outside `.planning` finds that file, the
  E2E feature `reorganize_layout.feature` and its step file
  `book_reading_reorganize.ts` (three scenarios use the Tab steps).
- The E2E page object sends synthetic `keydown` events with
  `cy.focused().trigger` (`indentFocusedBookBlockWithTab`,
  `outdentFocusedBookBlockWithShiftTab`, `cancelFocusedBookBlockWithBackspace`).
  Synthetic events do not move focus natively, so E2E cannot prove "Tab leaves the
  layout"; a component spec asserts it (keydown not default-prevented, one tab
  stop).
- `BookReadingBookLayout.spec.ts` covers drag emits, click and AI reorganize; no
  spec covers row keys or focus today.
- Not observed: whether focus really falls to the body after a depth change in
  the current build (UAT saw it on 2026-09-29; the cause above is from reading).
  Slice 3 adds the assertion first; if it already passes, slice 3 shrinks to
  that assertion and the plan reports it.

## Outside-in proof

- Tab does not change the layout, and the layout is one tab stop (component spec).
- Arrow down moves focus without choosing; Enter chooses (E2E, one new scenario
  in `reorganize_layout.feature`, plus a component spec of tab-stop and arrow
  rules).
- Alt+Shift+Right indents and Alt+Shift+Left outdents (the three existing E2E
  scenarios, with renamed steps).
- After a depth change the block stays focused and a second press acts on it
  (extended existing E2E scenario).

## Ordered slices

### 1. Tab no longer changes the layout; Alt+Shift+Arrow does
Type: Behavior
Status: planned
Proof: component spec of `BookReadingBookLayout`: Tab and Shift+Tab keydown
emit nothing and are not default-prevented; Alt+Shift+ArrowRight emits
`blockIndent` and Alt+Shift+ArrowLeft emits `blockOutdent` for the row. E2E:
the three existing scenarios keep their journeys; steps and page-object methods
are renamed to "with Alt+Shift+Right/Left" and send those keys.

Behavior: Tab and Shift+Tab stop hijacking the rows, and the two new key
combinations replace them. About 6 min. Stop-safe: Tab now moves on to the next
row natively (a long walk, but no accidental depth change).

### 2. The layout is one tab stop; arrows move focus
Type: Behavior
Status: planned
Proof: component spec: exactly one row has `tabindex="0"` (last focused, else
selected, else current, else first); ArrowDown/ArrowUp focus the next/previous
row without emitting `blockClick`, and stay put at the ends. E2E new scenario
"Move between book blocks with the arrow keys": choose "3. Refactoring Is Not Only About Changing Production Code", press ArrowDown →
"3.1" is focused and the selection is still that block; Enter → "3.1" is chosen
(existing steps for choose and focused, one new step for the key press and
for the selection assertion).

Behavior: Tab leaves the layout in one press and comes back to the same row;
ArrowUp/ArrowDown walk the rows; Enter chooses. About 8 min.

### 3. The block keeps focus after a depth change
Type: Behavior
Status: planned
Proof: E2E, extending "Indent a block and its children together": after the
indent, the existing step "the book block "Chapter A" should be focused in the
book layout" is added first and watched failing if the premise holds; then the
same block is outdented with Alt+Shift+Left with no click between and is back at
depth 0. Component spec if the fix lives in the layout's focus handling.

Behavior: after Alt+Shift+Arrow the changed block's row is focused once the
updated layout has painted, so the same key acts on it again. About 6 min. If
the first assertion passes without a change, the slice is that assertion only.

## Current decisions

- Keys (owner, 2026-09-30): Alt+Shift+ArrowRight indent, Alt+Shift+ArrowLeft
  outdent; arrows only move focus, Enter/Space choose; Backspace/Delete cancel
  unchanged and its risk recorded in the story.
- Component specs prove key rules; one new E2E scenario proves the flow; the
  existing scenarios are reused.

## Learnings

None yet.
