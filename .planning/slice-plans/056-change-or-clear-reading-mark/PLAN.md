# Let readers change or clear a reading mark

Work item: **SEED-059#story-6**.
Source: [refined story](../../seeds/SEED-059-book-reading-uat-fixes.md#story-6)
(owner decisions 2026-09-29: correct the mark on the block in the layout;
marking after an empty block moved to SEED-059#story-15).

## Goal and scope

A reader can change or clear a wrong reading mark: clicking the mark of the
chosen, marked block in the book layout offers Read, Skimmed, Skipped, and
Clear mark. The choice saves at once and persists, in PDF and EPUB alike.

Excluded (story): a legend, tooltips, dark-theme colours, and chapter progress
(story 12); changing several marks at once; undoing a change of mark; marking
after choosing a block with no text of its own (story 15).

Must keep working: Read/Skim/Skip on the Reading Control Panel, the panel
moving on after marking, the panel offering the next block when a marked block
is chosen, resume, and cancelling a PDF block clearing its record.

## Architecture

- **PFE:** both formats render the layout with one component,
  `BookReadingBookLayout.vue`, and read and write records through one
  composable, `useNotebookBookReadingRecords.ts` (`dispositionForBlock`,
  `submitReadingDisposition`). Changing a mark reuses the existing
  `PUT …/book/blocks/{bookBlock}/reading-record`, which already overwrites an
  existing record's status (`BookReadingProgress.upsertReadingRecord`).
  Clearing needs the one missing operation: `DELETE` on the same resource,
  returning the record list like `PUT`, in `NotebookBooksController` →
  `BookService` → `BookReadingProgress`.
- The mark control lives in `BookReadingBookLayout.vue` and emits to its two
  hosts (`BookReadingContent.vue` for PDF, `BookReadingEpubView.vue` for EPUB),
  which call the composable. A layout row is a `<button>`, so the control is a
  sibling of the chosen row, not nested in it. It does not go through
  `markSelectedBlockDisposition`, so it neither advances the selection nor
  touches the panel's flow.
- A cleared block simply has no record; the panel's existing "unmarked" rules
  apply unchanged. No new state.
- No ADR or North Star topic is affected.

## Decisive premises (observed 2026-09-29)

- **PUT overwrites an existing status.** Read
  `BookReadingProgress.upsertReadingRecord`: an existing row gets
  `setStatus(status)`. Confirmed.
- **No clear operation exists.** `grep -n Mapping NotebookBooksController.java`:
  only `PUT …/reading-record`; `BookBlockReadingRecordRepository` has no delete
  method. Confirmed.
- **One layout component and one records composable serve both formats.**
  `grep -rn dispositionForBlock frontend/src`: rendered only in
  `BookReadingBookLayout.vue`, supplied by `useNotebookBookReadingRecords`
  (EPUB `BookReadingEpubView.vue:112`; PDF `BookReadingContent.vue`).
  Confirmed.
- **The layout row is a `<button>`** (`BookReadingBookLayout.vue`, the
  `v-for="block in blocks"` element), and the mark is only a CSS border plus a
  screen-reader "Marked as …" span. Confirmed.
- **E2E setup and steps exist.** `reading_record.feature` has
  "I choose the book block …", "I scroll the PDF book reader until the Reading
  Control Panel shows for …", "I mark the book block … as skimmed in the Reading
  Control Panel", "I should see that book block … is marked as read/skimmed in
  the book layout", "no book block should be marked in the book layout", and
  "I open the book attached to notebook …" (usable to reopen).
  `epub_book.feature` has "Mark an EPUB block as skimmed advances the
  selection" (Chapter Alpha) and "I leave the EPUB reading view and return to
  it". Confirmed by grep of features and `e2e_test/step_definitions/`.
- **Controller test home exists:**
  `NotebookBooksReadingRecordControllerTest` (PUT persistence, 404 for another
  notebook's block, access denial). Confirmed.

## Outside-in proof

| Promise | Owner | Signal |
| --- | --- | --- |
| Change a mark on the chosen block; it persists | Slice 1 | New E2E scenario in `reading_record.feature`: choose 2.1, mark it skimmed on the panel, choose 2.1, change its mark to Read in the layout → marked read; open the book again → still marked read |
| Clear a mark on the chosen block; it persists; works in EPUB | Slice 2 | New E2E scenario in `epub_book.feature`: choose Chapter Alpha, mark skimmed, choose Chapter Alpha, clear its mark in the layout → no block marked; leave and return → still none. Controller tests: DELETE removes only the current user's record for that block and returns the list; another notebook's block → 404 |
| The panel still offers the next block when a marked block is chosen | Slices 1–2 | Existing "Panel auto-targets next block when selected is already marked" stays green |
| Must-keep-working behavior | Slices 1–2 | `reading_record.feature`, `epub_book.feature`, `reorganize_layout.feature` and `frontend/tests/pages/BookReadingPage*.spec.ts` green |

## Slices

### 1. Change the mark of the chosen block in the book layout

Type: Behavior
Status: done
Proof: new E2E scenario red → green; `reading_record.feature` and
book-reading page specs green.
Accepted proof: `reading_record.feature` 7/7 (new "Change the mark of a book
block in the book layout" red first, then green; rerun after refactor);
`epub_book.feature` 17/17 and `reorganize_layout.feature` 8/8;
`pnpm frontend:test tests/pages/BookReadingPage
tests/components/book-reading/BookReadingBookLayout` green; `vue-tsc` clean.
The control is `BookBlockMarkControl.vue`; the layout emits
`changeMark(blockId, status)` and both hosts bind
`bookReading.submitReadingDisposition` directly.

Behavior: in the refactoring PDF, "2.1 Easier to Change—and Harder to Misuse"
is marked Skimmed and chosen → the reader clicks its mark in the book layout and
chooses Read → the layout shows it read, and after opening the book again it is
still read.

Change: a mark control beside the chosen row when it has a mark (Read,
Skimmed, Skipped), emitting the chosen status; both hosts call
`submitReadingDisposition`. New step and page-object method to change a
block's mark in the layout.

### 2. Clear the mark of the chosen block in the book layout

Type: Behavior
Status: done
Proof: controller tests red → green; API client regenerated; new EPUB E2E
scenario red → green; `epub_book.feature` green.
Accepted proof: `NotebookBooksReadingRecordControllerTest` `DeleteBlockReadingRecord`
red (missing endpoint) → green, with the reading-position, attach, and
access-denial controller tests green after the refactor; `pnpm
generateTypeScript` output unchanged by the refactor; `epub_book.feature`
18/18 (new "Clear the mark of an EPUB block in the book layout" red first) and
`reading_record.feature` 7/7; book-reading page, layout, and
`useBookReadingSelection` specs green; `vue-tsc` clean. The reading-progress
endpoints now live in `NotebookBookReadingController` (same API tag).

Behavior: in the minimal EPUB, "Chapter Alpha" is marked Skimmed and chosen →
the reader clicks its mark and chooses Clear mark → "Chapter Alpha" is no
longer marked, and after leaving and returning it still is not. (Choosing
Chapter Alpha auto-marks the structural "Part One" read, so "no block is
marked" does not hold in this fixture.)

Change: `DELETE /api/notebooks/{notebook}/book/blocks/{bookBlock}/reading-record`
(same ownership checks as `PUT`, returns the list); regenerate the frontend
client; a `clearReadingDisposition` beside `submitReadingDisposition` in the
composable; Clear mark in the control.

## Current decisions

- The mark control sits on the chosen block in the layout, not on the panel
  (owner, 2026-09-29).
- Marking after choosing a block with no text of its own belongs to story 15
  (owner, 2026-09-29).

## Learnings

- "I open the book attached to notebook …" does not remount an already open
  book, so it cannot prove persistence. Slice 1 added "I open the book again"
  (a full visit); reuse it for reopen checks.
- In the minimal EPUB, choosing "Chapter Alpha" always marks "Part One" read;
  assert on the cleared block, not on "no block marked".
- `BookReadingContent.vue` (455 lines) stays over the file-size limit; both
  slices added one binding line each. Splitting it is outside this story.
