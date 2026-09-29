# Give the PDF reading view a designed structure, not just a smaller file

Work item: **SEED-059#story-19**.
Source: [story 19](../../seeds/SEED-059-book-reading-uat-fixes.md#story-19).
Evidence: DD-160 in `DearDough.md` (the file-size check waived for
`BookReadingContent.vue`).

## Goal and scope

Each reading concept has one owner, and what PDF and EPUB share is wired once,
so later book-reading stories (7, 9, 10, 16, 17, 18) change one place. Readers
see no change in either format.

- **Included:** the architecture in the story (one format-neutral reading view,
  one reading surface per format with explicit capabilities); every file of the
  reading view within 250 lines, ending the DD-160 waiver.
- **Preserved differences:** PDF only — layout changes (indent, outdent,
  cancel, drag), AI reorganize requests, the "Now reading" bar, creating a
  block from content, snap-back, page and zoom control, repairing the selection
  when blocks change. EPUB keeps the book layout's reorganizing controls shown
  but inert (defect 11, story 10). Two current-block rules (story 16). EPUB
  sends a pending reading position on leave; PDF drops it. EPUB updates the
  panel anchor on window resize.
- **Excluded:** changes to `PdfBookViewer.vue` and `EpubBookViewer.vue` beyond
  the surface boundary; `useBookReadingBootstrap`; any user-visible change.
- **Architecture:** confirmed by the owner on 2026-09-29.

## Architecture

- **Reading session** (`useBookReadingSession`, a composable): owns the
  reading records, selection and the block awaiting confirmation, the current
  block and its live announcement, the last-read position debouncer, and, when
  the surface allows it, layout changes and AI reorganize. It takes a
  **reading surface**: a plain object the format view builds, with
  - `showBlock(block)` — land on a chosen block (today's `onAdvance` bodies);
  - `readingPositionLocator()` — the locator saved as the last-read position;
  - `viewer` — the existing `BookReaderViewerRef` geometry for the panel
    anchor;
  - optional hooks only PDF supplies today: `commitCurrentBlock` and
    `blockAwaitingConfirmation` from snap-back, `onMarkedRead`;
  - capabilities: `reorganize` (PDF on, EPUB off), `repairSelection` (PDF on),
    `flushPositionOnLeave` (EPUB on).
  The format view proposes current-block candidates to the session from its own
  viewer events; the rule that produces them stays in the format view until
  story 16.
- **Reading shell** (`BookReadingShell.vue`): the global bar (book name, a
  trailing slot for PDF's page and zoom control), the book layout panel bound
  to the session once, the main slot inside a positioned pane with the Reading
  Control Panel and the "Now reading" bar, the live announcement, and the AI
  reorganize preview dialog. Reorganize listeners and the bar render only when
  the session's `reorganize` capability is on.
- **Format views**: `BookReadingPdf.vue` (renamed from `BookReadingContent.vue`)
  and `BookReadingEpub.vue` (renamed from `BookReadingEpubView.vue`) own their
  viewer, build the surface, and own format-only extras: PDF snap-back wiring,
  create-block-from-content with `NewBookBlockTitleDialog`, the load error;
  EPUB the resize listener and initial href.
- **PFE:** reuse `useBookReadingSelection`, `useBookReadingCurrentBlock`,
  `useReadingPanelAnchor`, `useBookReadingSnapBack`, `useBookLayoutMutations`,
  `useBookLayoutAiReorganize`, `useNotebookBookReadingRecords` under the
  session. `useBookReadingSelection`'s options `overrideBlockAwaitingConfirmation`
  and `repairSelectionWhenBlocksChange` and `useBookReadingCurrentBlock`'s
  `flushLastReadPositionPatchOnUnmount` are set from the surface in one place;
  fold them into the session where that removes an option rather than moving
  it.
- No North Star topic: this is feature-local structure, and no Accepted ADR
  covers the frontend reading view.

## Outside-in proof

The story adds no behavior, so every slice proves preserved behavior at the
same entry points:

- **Frontend (page level):** `CURSOR_DEV=true nix develop -c pnpm frontend:test
  tests/pages/BookReadingPage tests/components/book-reading
  tests/composables/useBookReadingSelection` — baseline 12 files, 71 tests
  green (observed below). These mount `BookReadingPage` and find
  `PdfBookViewer` / `ReadingControlPanel` / `CurrentBlockNavigationBar`, which
  this story keeps.
- **E2E (both formats):** `CURSOR_DEV=true nix develop -c pnpm cy:run --spec
  <feature>` for the features named on each slice. EPUB behavior is proven
  mainly here (`epub_book.feature`, 16 scenarios; `phone_reading.feature`),
  because EPUB page specs only check mounting.
- **Structure outcome:** `wc -l frontend/src/components/book-reading/*.vue
  frontend/src/composables/useBookReadingSession.ts` shows no reading-view file
  over 250 lines at the end.

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| Book-reading frontend tests are green before the change and go through the page, not the view files | Ran the frontend command above in this worktree on 2026-09-29; `grep` of `frontend/tests` for view imports | 12 files, 71 tests passed. Only `bookReadingContentAiReorganizeTestSupport.ts` imports `BookReadingContent.vue` directly (stubs `GlobalBar`, `PdfBookViewer`, `ReadingControlPanel`, `CurrentBlockNavigationBar`); it follows the rename in slice 5. No E2E step or script names the view files. |
| EPUB has no reorganizing wiring today | `BookReadingEpubView.vue` binds only `block-click`, `change-mark`, `clear-mark` on `BookReadingBookLayout` | Confirmed; the `reorganize` capability off must keep exactly these bindings. |
| Snap-back and selection depend on each other through the view | `BookReadingContent.vue`: `commitCurrentBlockId` calls `shouldSnapBack`, `useBookReadingSelection` takes snap-back's `blockAwaitingConfirmation`, `onMarkedRead` clears snap-back attempts | Confirmed; the session must take these as lazily called surface hooks, so snap-back can be created after the session's `currentBlockId` exists. |
| E2E runs from a linked worktree | `.agents/agent-map.md`: `pnpm cy:run` uses the disposable E2E stack; only the persistent Development stack is refused in worktrees | Confirmed by documentation; not run during planning. |

## Slices

### 1. One reading session owns records, current block, and reading position
Type: Structure
Status: done
Accepted proof: frontend command 12 files / 71 tests; `vue-tsc --noEmit` clean;
`reading_record.feature` 7/7 and `epub_book.feature` 17/17. EPUB's send-on-leave
is passed through unchanged (`flushPositionOnLeave` → the existing
`flushLastReadPositionPatchOnUnmount`); no test observes it directly.
Proof: frontend command; `cy:run --spec e2e_test/features/book_reading/reading_record.feature,e2e_test/features/book_reading/epub_book.feature`.

Create `useBookReadingSession` with the surface object and move into it, for
both views: `useNotebookBookReadingRecords` and its server sync on mount, the
`useBookReadingCurrentBlock` wiring, the reading-position proposal (from
`readingPositionLocator()`), and the live announcement text. Both views call
the session instead. Unchanged: PDF drops a pending position on leave, EPUB
sends it. Enables slice 2 (selection needs the session's current block).
About 10 min.

### 2. The session owns selection and the Reading Control Panel
Type: Structure
Status: done
Accepted proof: frontend command 12 files / 71 tests; `vue-tsc --noEmit` clean;
`reading_record` 7/7, `epub_book` 17/17, `phone_reading` 7/7 (EPUB and phone
rerun after the refactor, 24/24). The refactor removed
`useBookReadingSelection`'s `initialSelectedBlockId` and `afterAdvance` options.
Proof: frontend command (marking, visibility, snap specs); `cy:run --spec e2e_test/features/book_reading/reading_record.feature,e2e_test/features/book_reading/epub_book.feature,e2e_test/features/book_reading/phone_reading.feature`.

Move `useBookReadingSelection` and `useReadingPanelAnchor` into the session,
with `showBlock` replacing both `onAdvance` bodies and PDF's snap-back supplied
through the surface hooks. The selection-change reset of the panel anchor is
wired once. Enables slice 3 (the shell binds one session). About 10 min.

### 3. One reading shell for both formats
Type: Structure
Status: planned
Proof: frontend command; `cy:run --spec e2e_test/features/book_reading/epub_book.feature,e2e_test/features/book_reading/phone_reading.feature,e2e_test/features/book_reading/book_browsing.feature`.

Add `BookReadingShell.vue` with the global bar, book layout panel (block click
and mark bindings), main pane with the Reading Control Panel, and the live
announcement, all bound to the session. Both views render their viewer in its
slot; PDF puts `PdfControl` in the bar slot. The live announcement region
follows Current decisions. Enables slice 4. About 10 min.

### 4. Reorganizing and the "Now reading" bar are a PDF capability of the shell
Type: Structure
Status: planned
Proof: `pnpm frontend:test tests/components/book-reading/BookReadingContentAiReorganize.spec.ts` plus the frontend command; `cy:run --spec e2e_test/features/book_reading/reorganize_layout.feature,e2e_test/features/book_reading/ai_reorganize_layout.feature,e2e_test/features/book_reading/reading_record.feature,e2e_test/features/book_reading/epub_book.feature`.

Move `useBookLayoutMutations`, `useBookLayoutAiReorganize`, the reorganize
preview dialog, and `CurrentBlockNavigationBar` with Read from here / Back to
selected into the session and shell, active only when `reorganize` is on. The
session emits the updated book through the view. EPUB keeps its current
bindings. Enables story 10 turning `reorganize` on for EPUB. About 10 min.

### 5. Format views own only their format; the file-size waiver ends
Type: Structure
Status: planned
Proof: frontend command; `wc -l` check above; `cy:run --spec e2e_test/features/book_reading/book_browsing.feature,e2e_test/features/book_reading/phone_reading.feature`.

Rename the views to `BookReadingPdf.vue` and `BookReadingEpub.vue` (update
`BookReader.vue` and the AI-reorganize test support), keep create-block-from-
content and its title dialog in the PDF view, remove options the surface
capabilities replaced, and confirm no reading-view file is over 250 lines.
About 5–10 min.

## Current decisions

- The shell's live announcement region: today only PDF renders
  `book-reading-current-block-live`. Keep that difference unless an execution
  observation shows EPUB can render it with no visible or E2E change; do not
  add it to EPUB as a feature.
- Current-block candidates stay computed in each format view
  (`currentBlockIdFromVisiblePage`, `currentBlockIdFromEpubView`); story 16
  unifies them in the session.

## Learnings

- The surface carries more than the Architecture lists, all needed for exact
  preservation: `mainPane` (each view keeps its pane ref, because vue-tsc
  reports a template ref bound to a destructured value as unused; slice 3's
  shell decides how the pane reaches the session), PDF's `canAnchorPanel`
  (panel shown only while the block's last content is visible), and EPUB's
  `reanchorPanelAfterSyncAndShow` (anchor refresh after the records sync and
  after showing a block). Slice 1's `onRecordsSynced` hook is gone.
- Slice 5 candidates in `useBookReadingSelection.ts`:
  `overrideBlockAwaitingConfirmation` and `repairSelectionWhenBlocksChange`,
  now set only by the session.
- Slice 5 candidates in `useBookReadingCurrentBlock.ts`: the unused
  `lastReadPositionPatchDebouncer` return and the factory-style
  `proposeReadingPosition` option, now with one caller (the session).
- `PdfBookViewer.vue` (400 lines) is excluded from this story, so slice 5's
  `wc -l` check covers the reading-view files, not the viewers.

- Plans 053 (story 5) and 058 (story 16) name `BookReadingContent.vue` and its
  wiring; after this story they need their file references realigned when they
  are next refined. This plan does not edit them.
