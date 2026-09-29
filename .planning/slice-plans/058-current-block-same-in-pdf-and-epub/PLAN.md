# The current block moves the same way in PDF as in EPUB

Work item: **SEED-059#story-16**.
Status: **awaiting story refinement — not ready for slice-plan refinement or
execution.** Resume with `dough-story-refinement` on
[story 16](../../seeds/SEED-059-book-reading-uat-fixes.md#story-16) to clarify
its goal, scope, and key examples, then realign this plan before slice-plan
refinement or execution.

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
- Story 5 removes snap-back and the hold-window gate (`PdfBookViewer.vue:141`)
  from the same pipeline; this work follows it.
- Tests that change: `book_browsing.feature:52-66` (bookmark block about 95/1000
  below "top of page 5") breaks; `reading_record.feature:25-29` needs landing
  fixed; the same-page helper's deltas (`bookReadingPdfMethods.ts:69-89`) need
  recomputing; page specs that emit normalized viewports
  (`BookReadingPage.readingControlPanel.marking.spec.ts`,
  `.readingPosition.spec.ts`, `.notebookSwitch.spec.ts`,
  `bookReadingPageInteractionTestSupport.ts`) need a stubbed block-start source.
- No PDF fixture has two headings sharing a start; a parent and child bookmark
  at one destination would give one. No fixture checked has a "Chapter N" label
  before a title block with introduction text.

## Provisional slices

1. **Structure:** move the rule to a format-neutral module taking a block-start
   function; EPUB unchanged. About 5 min.
2. **Behavior:** a chosen PDF block lands with its start at the top and is
   current (new E2E in `book_browsing.feature`). About 10 min.
3. **Behavior:** PDF follows scrolling with the shared rule; delete the old
   rule; move page specs and the bookmark scenario. About 15 min, over target:
   refinement should look for a cut.
4. **Behavior:** a chosen PDF block keeps a shared start (bookmark fixture).
   About 10 min.
5. **Behavior:** marking goes on after choosing "Chapter 12" in a no-bookmark
   PDF (new fixture shaped like Think Python). About 10 min if the rule and
   `hasNoTextOfItsOwn` suffice.
