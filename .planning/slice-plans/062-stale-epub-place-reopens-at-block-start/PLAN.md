# A stale exact EPUB place reopens at the block start

Work item: **SEED-059#story-20** (retrospective correction).
Source: [story 20](../../seeds/SEED-059-book-reading-uat-fixes.md#story-20).

## Provenance

- Corrects SEED-059#story-17, closed. Its story and plan are recoverable at
  `e505a489f9:.planning/seeds/SEED-059-book-reading-uat-fixes.md` and
  `e505a489f9:.planning/slice-plans/059-reopen-epub-at-exact-paragraph/PLAN.md`.
- Reviewed commits on `story/seed-059-story-17`: 8d3d821f28 (probe record),
  946dccd30f (API keeps `cfi`), b47d6eb4c8 (viewer, debouncer, E2E).
- Story 17's boundary assumption: "an exact place that no longer resolves
  reopens at its block start, through the existing fallback, with no new
  message." Its plan softened this to "falls back through the existing display
  chain" and gave it no proof.

## Current findings (observed 2026-09-29 at b47d6eb4c8)

- `EpubBookViewer.epubDisplayTarget` returns the CFI alone when one is stored,
  so the block start is never tried. `openEpub` then runs
  `r.display(target).catch(() => r.display())`, and `r.display()` shows the
  first linear spine item: the cover.
- epub.js 0.3.93 `Spine.get` takes the CFI's spine position.
  `Rendition._display` rejects with "No Section Found" when no item exists
  there.
  - Node check with `epubjs/lib/epubcfi.js`: `epubcfi(/6/200!/4/2/1:0)` parses
    to spinePos 99.
  - `epub_long_chapter_before_target.epub` has 6 spine items: cover, contents,
    c1, c2, short, end. So that CFI cannot be displayed there.
- `useBookReadingBootstrap.epubInitialLocatorFromSaved` still drops a saved
  locator whose href#fragment string is empty (via `epubDisplayHref`). That is
  a second, href-only displayability rule beside the viewer's. It is not
  observable today, because `href` is required, but it diverges from the
  viewer's rule.
- A stored exact place can go stale: a Book reads its notebook-root file by
  filename (`BookSourceFile`), so the file can change while the reading
  position is kept.

## Preserved promises

- Paragraph-precise reopening at another width (story 17 scenario) stays green.
- Choosing a block lands at its start. Block locators never carry `cfi`.
- A position without `cfi` reopens at its block start.
- A position whose block start cannot be displayed either still reopens at
  the book start.
- No new message.

## Outside-in proof

| Promise | Owning slice | Proof |
| --- | --- | --- |
| Saved exact place can't be displayed → reopen at the saved block's start | 1 | New E2E scenario in `epub_book.feature`, rule "Landing on the chosen place" |
| Story 17 promises above | 1 | Existing `epub_book.feature` scenarios stay green |

## Ordered slices

### 1. Reopening with an exact place that no longer resolves lands at the block start

Type: Behavior
Status: done
Proof: `SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/book_reading/epub_book.feature`
(the new scenario fails first, landing at the cover), plus frontend
`vue-tsc --noEmit`, and `pnpm frontend:test tests/composables/useBookReadingBootstrap.spec.ts tests/pages/BookReadingPage.spec.ts`.

Behavior: at 1280×560, open `epub_long_chapter_before_target` and choose
"Licence Section One", then leave the reading view. While away, the stored
position keeps its href and fragment, but its `cfi` is replaced by one with no
spine item, for example `epubcfi(/6/200!/4/2/1:0)`. Do this with a GET, then a
PATCH of `/api/notebooks/{notebook}/book/reading-position`. On return, the
heading "Licence Section One" is at the top of the EPUB reader, and it is the
current block.

- **E2E:** Seeding must happen after the view's own PATCH flushes, and before
  the remount. Shape it as a variant of the existing leave-and-return page
  object, for example an optional step run while away. Do not copy that
  method.
- **Viewer:** Make one ordered display chain for a stored EPUB locator: the
  exact place, then the block start (href#fragment). Both opening and
  `displayLocator` try its targets in order until one displays. Opening ends
  with the book start.
- **Bootstrap:** Pass the saved EPUB locator (`asEpubLocator`), so the viewer
  owns displayability. Drop `epubInitialLocatorFromSaved`'s href check.
  Inline `epubDisplayHref` into `blockStartEpubDisplayHref` if that becomes its
  only caller.
- About 10 min, most of it the E2E step.

Accepted proof (2026-09-29): the new scenario "Reopening when the saved exact
place no longer resolves resumes at its block start" failed before the fix (reader
at the cover) and passes after it. `epub_book.feature` is 19 passing,
`vue-tsc --noEmit` passes, and the bootstrap, reading-page, and book-layout specs
pass. The viewer alone decides EPUB displayability (`epubDisplayTargets` →
`displayFirst`). Bootstrap passes the saved locator unchanged. Both failing →
book start remains the unchanged `r.display()` fallback, with no test.

## Current decisions

- "No longer resolves" means epub.js rejects the display. A CFI that displays
  somewhere else is out of scope.
