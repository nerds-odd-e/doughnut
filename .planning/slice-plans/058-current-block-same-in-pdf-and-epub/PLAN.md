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
3. **Behavior:** PDF follows scrolling with the shared rule; delete the old
   rule (`currentBlockIdFromVisiblePage` and its cases); move the bookmark
   scenario to scroll to the block's start; move the page specs to a stubbed
   block-start source. About 10–12 min. Exception: the spec migration is
   mechanical and cannot be split off without leaving red specs or a dead old
   rule.
4. **Behavior:** a chosen PDF block keeps a shared start (bookmark fixture).
   About 10 min.
5. **Behavior:** marking goes on after choosing "Chapter 12" in a no-bookmark
   PDF (new fixture shaped like Think Python). About 10 min if the rule and
   `hasNoTextOfItsOwn` suffice.
