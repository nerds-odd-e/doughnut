# The current block moves the same way in PDF as in EPUB

Work item: **SEED-059#story-16**.
Status: **story refined and slices refined; ready for execution.** Slice 3
was cut by moving the landing-dependent test repairs into slice 2; its remaining
size is a stated exception.

Source: resplit from the original story 15 plan (committed at `8e5d358124`,
parts B and C). The slices below are provisional planning input.

## Known architecture

- One rule: evolve `currentBlockIdFromEpubView` into a format-neutral rule over
  "block start offset from the view top in px". PDF supplies starts from the
  page div's position plus `bbox[1]` (close to
  `usePdfLocatorGeometry.resolveLocatorRect`, which today skips page-only
  locators). Delete `currentBlockIdFromVisiblePage` and its 19 cases; keep
  `pdfViewerViewportTopYDown` / `pdfViewerReadingPositionTopEdge` for the page
  bar and reading position.
- PDF landing: `usePdfNavigation.applyNavigationTarget` uses
  `normalizedBboxToPdfJsXyzDestArray` with `SCROLL_TOP_PADDING_PDF = 40`
  points, leaving the start about 53–75 CSS px low, beyond the 24 px tolerance.
  Land at the start and derive the current block from the view after landing,
  as EPUB does.
- Story 5 has landed: snap-back and the hold-window gate are gone, so nothing
  else competes with the view's scroll position.
- Tests that change: the `book_browsing.feature` bookmark scenario "Bookmark
  blocks show, land where they point, and follow scrolling" ("5. Refactoring in
  Team Development" about 95/1000 below "top of page 5") breaks: scroll to the
  block's start instead; `reading_record.feature:25-29` needs landing
  fixed; the same-page helper's deltas (`bookReadingPdfMethods.ts:69-89`) need
  recomputing; page specs that emit normalized viewports
  (`BookReadingPage.readingControlPanel.marking.spec.ts`,
  `.readingPosition.spec.ts`, `.notebookSwitch.spec.ts`,
  `bookReadingPageInteractionTestSupport.ts`) need a stubbed block-start source.
- `useReadingPanelTarget.ts` `hasDirectContent` moves to `hasNoTextOfItsOwn`
  (slice 5); its `lastDirectContentLocator` anchoring stays with story 18.
- Where the start cannot reach the top (first or last page), the chosen block
  wins through the EPUB rule's landing limit; the PDF view supplies its own
  limit (slice 2 proves it on page 1).
- No PDF fixture has two headings sharing a start; a parent and child bookmark
  at one destination would give one. No fixture checked has a "Chapter N" label
  before a title block with introduction text.

## Provisional slices

1. **Structure:** move the rule to a format-neutral module taking a block-start
   function; EPUB unchanged. About 5 min.
   Status: done. The rule is `currentBlockIdFromViewStarts.ts` (generic over a
   block-keyed `startTopPx`); the EPUB adapter sits in `BookReadingEpub.vue`;
   `EpubViewBlockStarts` moved to `useEpubLocatorGeometry.ts`. Proof: vitest
   `Epub ViewStarts` (20) and `BookReadingPage` (33) pass; `vue-tsc --noEmit`
   passes.
2. **Behavior:** a chosen PDF block lands with its start at the top and is
   current, including on the first page (new E2E in `book_browsing.feature`).
   Also repairs what landing changes: `reading_record.feature:25-29` and the
   same-page helper's deltas (`bookReadingPdfMethods.ts:69-89`). About 10 min.
   Status: done. `reading_record.feature` needed no edit; the same-page helper
   delta went from 0.42 to 0.378 of the page height. Landing is bbox top
   (padding removed); `usePdfLocatorGeometry.viewBlockStarts()` supplies starts
   and a landing limit (0, or the whole view at the document end); `showBlock`
   derives the current block after landing (falls back to the chosen block when
   the viewer has no document yet). New E2E outline in `book_browsing.feature`
   (page 1 y=252, page 2 y=89, first block y=72). Learning: pdf.js lands a
   start up to about 11 px above the canvas position (page div borders), so
   the E2E tolerance is 15 px. Not exercised: a block whose start is at or
   above the first page's top edge. Proof: vitest `pdfOutlineV1Anchor
   BookReadingPage currentBlock ViewStarts` (90), vue-tsc, `pnpm cy:run --spec`
   `book_browsing.feature` 9/9 and `reading_record.feature` 9/9.
3. **Behavior:** PDF follows scrolling with the shared rule; delete the old
   rule (`currentBlockIdFromVisiblePage` and its cases); move the bookmark
   scenario to scroll to the block's start; move the page specs to a stubbed
   block-start source. About 10–12 min. Exception: the spec migration is
   mechanical and cannot be split off without leaving red specs or a dead old
   rule.
   Status: done. `onViewportAnchorPage` proposes `currentBlockIdInView()` (same
   as `showBlock`); `currentBlockIdFromVisiblePage` and its spec are deleted and
   `usePdfViewportPosition()` takes no blocks. The bookmark scenario scrolls to
   `0 of 1000` (block 4 still current) then `95 of 1000` (block 5 current) on
   page 5. Page specs stub the view through `stubViewTopAt`. No start-of-document
   landing limit was needed. Learning: the page indicator follows the reader's
   midpoint, so after landing without padding a "viewport should be on page N"
   assertion can sit within pixels of a page boundary; CI run 36647846005
   failed on this in `phone_reading.feature` and was repaired by asserting the
   start position (`5206b08370`). `book_browsing.feature` and
   `reading_record.feature` still use the indicator step after choosing a block
   and could be similarly fragile. Proof: vitest book-reading pages, components,
   lib and composables (253), vue-tsc, `cy:run` for `book_browsing` 9/9,
   `reading_record` 9/9, `reorganize_layout` 8/8, `ai_reorganize_layout` 1/1,
   `phone_reading` 7/7.
4. **Behavior:** a chosen PDF block keeps a shared start (bookmark fixture).
   About 10 min.
   Status: done, proof only (no product change). The rule's chosen-wins case
   already covered it; the E2E PDF fixture would need a binary edit to get two
   headings at one destination, so the proof is a page spec: `it.each` Sections
   4, 5, 6 (page-only starts sharing one start in the top-maths-like fixture)
   in `BookReadingPage.spec.ts` stays current when chosen and moves on to
   "Section 3" when scrolled past. Mutation check: passing `null` for the chosen
   id fails Sections 4 and 5. Proof: vitest `tests/pages` (236), vue-tsc. Not
   covered: real pdf.js geometry with bbox-carrying shared starts.
5. **Behavior:** marking goes on after choosing "Chapter 12" in a no-bookmark
   PDF (new fixture shaped like Think Python). About 10 min if the rule and
   `hasNoTextOfItsOwn` suffice.
