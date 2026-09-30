# Show EPUB readers only the layout controls that work

Work item: **SEED-059#story-10**
([story](../../seeds/SEED-059-book-reading-uat-fixes.md#story-10)).
Status: **both slices done.**
Nothing blocks execution.

## Goal and scope

An EPUB reader is not offered layout controls that do nothing: no *AI
Reorganize* button, and no indent, outdent, cancel, or drag on block rows. PDF
behaves as today.

Excluded (see the story; no follow-up story is queued): reorganizing an EPUB
layout, the "Now reading" bar and EPUB pane layout, creating a block from EPUB
text, the panel keeping the old selection after scrolling on; any backend,
generated API, or CLI change.

## Known architecture

- **The capability already exists.** `useBookReadingSession` returns `reorganize`
  only when the format view's surface offers it; `BookReadingPdf` does,
  `BookReadingEpub` does not. `BookReadingShell` already follows it for the
  undo control (`:can-undo-depth-change`), the Ctrl/Cmd+Z shortcut, the "Now
  reading" bar, and the AI preview dialog. It also attaches the layout's
  listeners with `v-on="reorganize?.layoutListeners ?? {}"`, so with no
  capability the events go nowhere while the layout still draws the button and
  binds the row keys and drag. That leftover is the whole defect.
- **PFE result: reuse and complete.** No new mechanism. Give
  `BookReadingBookLayout` a `canReorganize` prop that the shell sets from
  `reorganize !== null`, and have the layout draw or bind only what that
  capability provides. This finishes the shell's existing "capability, not
  format" rule for the last three controls instead of adding an EPUB case.
- **No ADR or North Star topic applies.** `docs/adrs` covers notebook content,
  sync, and language; `.planning/NORTH-STAR.md` mentions a Book only as notebook
  content. Nothing there governs the reading surface.

## Premises observed

- **Only the shell mounts the layout.** `grep -rln BookReadingBookLayout
  frontend/src frontend/tests e2e_test` finds `BookReadingShell.vue` and
  `BookReadingBookLayout.spec.ts` only.
- **EPUB has no `reorganize`; PDF has it.** `BookReadingEpub.vue`'s surface sets
  `showBlock`, `readingPositionLocator`, `viewer`, `reanchorPanelAfterSyncAndShow`
  and `flushPositionOnLeave`; `BookReadingPdf.vue` line 96 sets `reorganize`.
  Read both files.
- **The button and row bindings are unconditional today.**
  `BookReadingBookLayout.vue` draws `book-reading-ai-reorganize-layout` with no
  `v-if`, and each row binds `@keydown.alt.shift.right`, `.left`, `.delete`
  (which calls `preventDefault`) and `@pointerdown="blockDrag.onPointerDown"`.
  Read the file.
- **Layout proof lives at the layout's own spec.**
  `frontend/tests/components/book-reading/BookReadingBookLayout.spec.ts` covers
  drag indent/outdent, click-sized movement, Alt+Shift+Arrow keys, Tab not
  prevented, and the AI button emitting `requestAiReorganize`. Its `mountLayout`
  passes no reorganize prop, so it needs `canReorganize: true` by default, and
  that is the only change to existing tests. Read the spec.
- **PDF end-to-end proof exists and is unaffected in intent.**
  `reorganize_layout.feature` and `ai_reorganize_layout.feature` attach a fake
  PDF and use these controls; `bookReadingAiReorganizeMethods.ts` clicks
  `book-reading-ai-reorganize-layout`.
- **An EPUB absence check can pass vacuously** if the layout has not rendered.
  The scenario therefore first asserts the layout lists blocks, as the existing
  step "I should see the book layout in the browser" does in
  `epub_book.feature`.
- **Not run:** no test or E2E command was run to plan this; the before-change
  failures below follow from the code read above and are confirmed by running
  each new check first, per execution.

## Outside-in proof

- E2E, `epub_book.feature`, new scenario in the "Supported minimal EPUB" rule:
  the layout lists "Part One" and the other blocks, and offers no AI
  reorganization. Expected to fail before slice 1 (the button is drawn) and pass
  after.
- Layout spec (the layout's own boundary), for the keys and drag, which cannot be
  observed end to end because a dead key sends nothing: with reorganizing not
  offered, Alt+Shift+Right, Alt+Shift+Left, Delete and a horizontal drag emit
  nothing, Delete is not default-prevented, and clicking a row still emits
  `blockClick`. Expected to fail before slice 2.
- Preserved: every existing case in `BookReadingBookLayout.spec.ts` (with
  `canReorganize: true`), `reorganize_layout.feature` and
  `ai_reorganize_layout.feature` (PDF), and `epub_book.feature` (EPUB selection,
  navigation, marks).

## Ordered slices

### 1. An EPUB layout offers no AI Reorganize
Type: Behavior
Status: done
Proof: new E2E scenario "EPUB book layout offers no AI reorganization" in
`epub_book.feature`, with a step that asserts the button does not exist after
the layout has listed its blocks; and a layout spec case that the button is
drawn only when `canReorganize` is true. Both written first and seen failing.
Focused check: `epub_book.feature` and `ai_reorganize_layout.feature` through
the project's E2E runner, and `BookReadingBookLayout.spec.ts`.

Behavior: an EPUB is open → its book layout lists the blocks and has no *AI
Reorganize* button. A PDF's layout still has it and it still works.

Add the `canReorganize` prop to `BookReadingBookLayout`, `v-if` the button on
it, and set it from `reorganize !== null` in `BookReadingShell`. Add
`canReorganize: true` to the spec's `mountLayout`. Reuse the existing
`Given`-style steps for opening the EPUB reading view; add one step for the
absence. About 5 min. Stated reason it is separate from slice 2: it is the only
end-to-end-observable outcome, and it introduces the prop slice 2 reuses.

### 2. An EPUB layout's rows offer no reorganize keys or drag
Type: Behavior
Status: done
Proof: layout spec cases described under Outside-in proof, written first and
seen failing. Focused check: `BookReadingBookLayout.spec.ts`; PDF preserved by
its existing indent, outdent and drag cases and `reorganize_layout.feature`.

Behavior: an EPUB layout row is focused → Alt+Shift+Right, Alt+Shift+Left,
Delete and a horizontal drag send nothing, Delete is left to the browser, and
clicking the row selects it as before. A PDF layout's rows behave as today.

Bind the row keys and pointer drag only while `canReorganize` is true, so an
unsupported key or drag is not offered rather than handled by an empty
listener. Keep the drag-click guard tied to the same condition so a click is
never swallowed. About 5 min.

## Current decisions

- Hide by capability rather than enable reorganizing for EPUB (owner decision,
  2026-09-30: rarely used feature; the requirement returns when readers need
  it).
- The keys and drag are proved at the layout's spec, not end to end, because a
  dead control has no observable end-to-end effect to assert; the button's
  absence is the end-to-end promise.
- No unit test of the shell's `reorganize !== null` mapping on its own: the
  EPUB scenario reaches it through a real EPUB, and the existing PDF scenarios
  cover the other value.

## Learnings

Slice 1 accepted proof: `BookReadingBookLayout.spec.ts` (16/16, incl. "draws AI Reorganize only when reorganizing is offered"); E2E `epub_book.feature` (new scenario failed before, passes after) and `ai_reorganize_layout.feature` (PDF), 22/22. `reorganize_layout.feature` not run.

Slice 2 accepted proof: `BookReadingBookLayout.spec.ts` "without reorganizing" cases (3 failed before; 19/19 after); E2E `reorganize_layout.feature` 10/10 and `epub_book.feature` 21/21. Vue's `.delete` modifier also matches Backspace, so the row handler keeps both. `BookReadingBookLayout.vue` (314 lines) and its spec (409) were already over the 250-line guide before this story.
