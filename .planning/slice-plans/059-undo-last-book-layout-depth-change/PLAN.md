# Undo the last book layout depth change

Work item: **SEED-059#story-7**
([story](../../seeds/SEED-059-book-reading-uat-fixes.md#story-7)).
Status: **both slices done.**

## Goal and scope

A reader who indents or outdents a block by mistake gets the previous layout
back in one step (button or Ctrl/Cmd+Z). Only the last depth change is undone,
and only while the book's depths are still what that change produced.

Excluded (see the story): undoing Cancel, AI reorganize or block creation;
redo and multi-step undo; "make the following blocks children of this one";
focus and keyboard movement (story 9); EPUB (story 10 turns reorganizing on).

## Known architecture

- **Reuse the apply-depths request.** `NotebookBooksController.applyBookLayoutReorganization`
  (`BookLayoutReorganizer.apply`) takes every block's `{id, depth}`, requires the
  same block ids as the book and a valid preorder outline, then saves the depths.
  Undo sends the depths from before the change. No backend or OpenAPI change.
  A depth change only saves depths, so restoring them restores the layout, and
  reading records are untouched.
- **Why not the inverse Indent/Outdent:** outdenting "3.1" re-parents "3.2"
  without changing its depth; indenting "3.1" moves "3.2" with it, so the
  layout differs. Only a depth restore is exact.
- **Where the memory lives.** `useBookLayoutMutations` already owns
  `changeBlockDepth` and reads the current blocks through `bookBlocks`. It keeps
  `{before, after}`: block-id → depth maps for the whole book around the last
  successful depth change. `canUndoDepthChange` is true while the current
  blocks' ids and depths equal `after`. Cancel, new block and AI apply change
  ids or depths, so undo disappears without those paths knowing about it. A
  reload creates a new composable, so it is empty. Undo clears the memory.
- **Reuse for the request.** The apply call sits in `useBookLayoutAiReorganize.confirmSuggest`,
  bound to the AI suggestion. The undo needs the same request with given
  depths; move the request into one shared function both call, instead of
  duplicating the API call and loading message.
- **Wiring.** `useBookReadingSession`'s `useReorganize` already exposes
  `onBlockIndent/onBlockOutdent/onBlockCancel` from the mutations composable
  and applies mutation responses through `bookFullAfterLayoutMutation`; the
  undo goes out the same way. `BookReadingBookLayout.vue` renders the button
  above the block list next to *AI Reorganize* and emits an event like its other
  actions. Reorganizing is a PDF-only surface today, so undo is PDF-only with no
  extra code.
- **Shortcut listener.** After Tab or Shift+Tab, focus falls to the page body
  (story 9's defect 14), so a listener on the layout would miss Ctrl/Cmd+Z right
  after a depth change. The reorganize-capable reading surface registers a
  `keydown` listener on the window while mounted, removed on unmount, that
  ignores events from text inputs, textareas and editable content.

## Premises observed

- Apply validation: `BookLayoutReorganizer.validateSuggestedLayout` requires equal
  id sets and preorder-valid depths (first 0, each ≤ predecessor + 1). A
  restored earlier depth list is valid when the ids are unchanged.
- `refactoring` fixture: MinerU levels nest by a stack
  (`MineruContentListLayoutBuilder`), so "3.1" and "3.2" (both level 2 after
  the level-1 "3.") are depth-1 siblings; outdenting "3.1" makes "3.2" its
  child. `reorganize_layout.feature` already outdents "3.1" from depth 1 to 0.
- `subtree_indent` fixture: "Chapter A" with children "A.1", "A.2" then
  "Chapter B"; existing scenario indents "Chapter A" with Tab.
- Existing steps to reuse: "the book layout shows block … at depth …",
  "I choose the book block …", "I outdent/indent the focused book block …",
  "the book block … should be at depth … in the book layout"
  (`e2e_test/step_definitions/book_reading_reorganize.ts`).
- No Ctrl/Cmd+Z handler or Undo control exists in
  `BookReadingBookLayout.vue`, `useBookLayoutMutations.ts` or the reading session.
- Existing spec homes: `frontend/tests/composables/book-reading/useBookLayoutMutations.spec.ts`
  (API stubbed, harness composable), `frontend/tests/components/book-reading/BookReadingBookLayout.spec.ts`
  (emits) and `BookReadingPdfAiReorganize.spec.ts` (page level).

## Outside-in proof

- Wrong outdent, then Undo: in *Code Refactoring*, outdent "3.1"; Undo → "3.1"
  and "3.2" are back at depth 1 (E2E, `reorganize_layout.feature`).
- Indent "Chapter A" (with children), then Ctrl/Cmd+Z → "Chapter A" at depth 0
  and "A.1", "A.2" at depth 1 again (E2E).
- Outdent a block, cancel another → Undo not offered; reload → Undo not
  offered (frontend specs).

## Ordered slices

### 1. A reader undoes a wrong indent or outdent with an Undo button
Type: Behavior
Status: done
Proof: E2E scenario "Undo a wrong outdent" in `reorganize_layout.feature`
(refactoring fixture, new step "I undo the last book layout change" clicking the
button, existing depth-assertion steps). Composable spec: after an outdent the
mutations composable can undo and the book's depths return to their earlier
values; after a cancel undo is unavailable, and after a second depth change undo
offers that newer change; undo of an indent restores too. Layout component
spec: the button is shown only while undo is available and emits its event.

Behavior: a PDF book is open and the reader has just outdented "3.1" (Shift+Tab)
→ the reader clicks *Undo* → "3.1" and "3.2" are siblings under "3." again,
the saved layout is the earlier one, and *Undo* is no longer shown.

Includes the shared apply-depths request, the `{before, after}` memory with
`canUndoDepthChange`, the button and the session wiring. About 10 min. Stated
reason it stays one slice: the memory rule, the request and the button share one
proof loop and none of them is observable alone.

### 2. A reader undoes with Ctrl/Cmd+Z anywhere on the reading page
Type: Behavior
Status: done
Proof: E2E scenario "Undo an indent with the keyboard" (subtree_indent fixture,
new step "I press Ctrl+Z" sending the key to the page after the existing Tab
step, so it covers focus having fallen to the body). Spec: the shortcut runs
undo when available, does nothing when unavailable, and does not run from a
text input.

Behavior: the reader has just indented "Chapter A" with Tab (focus is on the
page body) → presses Ctrl (Cmd on Mac) + Z → "Chapter A" is at depth 0 and its
children at depth 1 again. With no undo available, or typing in a text field,
the key does its normal thing.

About 5 min: one window listener on the surface from slice 1.

## Current decisions

- Undo is depths only. Nothing outside the mutations composable clears it: it
  is unavailable whenever the current depths differ from what the last change
  produced.
- The shortcut is page-wide, not layout-scoped, because focus is lost after
  Tab (story 9 owns the focus fix; do not fix it here).
- Considered and excluded: a server-side "undo last change" (needs persisted
  history and new API), inverse Indent/Outdent (not exact), undoing Cancel
  (marks are deleted with the block).

## Learnings

- Slice 1 done: E2E "Undo a wrong outdent" (`reorganize_layout.feature`, 9/9) and
  the composable, layout and AI-reorganize specs pass. A second depth change
  replaces the memory, so Undo then reverses the newest change; only Cancel (or
  any other change of ids or depths) makes it unavailable.
- Not directly observed: E2E does not assert Undo disappears afterwards (the
  composable and layout specs do); no spec for reload or the apply failure path.
- Slice 2 done: E2E "Undo an indent with the keyboard" (10/10) and
  `BookReadingPdfUndoShortcut.spec.ts` (undo runs, nothing to undo, text input
  left alone, listener removed on unmount). The listener lives in
  `BookReadingShell.vue`, registered only when reorganizing is available.
  Untested: the Cmd/Meta path, contenteditable, redo and Alt combinations.
  Page-level specs must feed `update:book` back (`setProps`); synthetic Cypress
  keydown needs `getModifierState`.
