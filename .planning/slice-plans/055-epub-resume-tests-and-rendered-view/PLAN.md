# EPUB resume tests say what the product does, and one lookup finds a locator's rendered view

**Identity:** SEED-059#story-14
**Source:** [correction story](../../seeds/SEED-059-book-reading-uat-fixes.md#story-14), from the
execution retrospective of SEED-059#story-1 (final plan
`52e8d849e7:.planning/slice-plans/049-epub-land-and-track-chosen-place/PLAN.md`, original
contract `e733844d01:.planning/slice-plans/049-epub-land-and-track-chosen-place/PLAN.md`; reviewed
commits `485ed2eb49` slice 1 through `26c34d6649` slice 6 on `story/seed-059-story-1`).

## Finding and provenance

- **Misleading and redundant resume scenarios.** Slice 6 (`26c34d6649`) made the saved EPUB
  reading position the current block's start. `epub_book.feature` "EPUB reading resumes at the
  scrolled fragment, not the inferred block start" now names the opposite of what the product
  does; it passes only because "Cell One" sits right after Chapter Beta's heading in
  `epub_valid_minimal.epub`. "Resume EPUB reading at the last position" only checks text is
  visible after reopening. Slice 6's "Reopening resumes at the last place and shows its block in
  the book layout" proves resume more strongly (heading at the top, block current, layout row in
  view). The steps "I scroll the EPUB reader host to the top" and "I scroll the EPUB reader until
  the text {string} is in the viewport" (`book_reading_epub.ts`), and
  `scrollEpubReaderHostToTop` / `scrollEpubReaderUntilTextInViewport`
  (`bookReadingEpubMethods.ts`), serve only the misleading scenario.
- **Two lookups for a locator's rendered view.** In `useEpubLocatorGeometry.ts`,
  `resolveEpubLocatorElement` (moved from the old viewer; matches views by href, takes the last
  displayed one, falls back to `body`) feeds the reading-panel anchor, while slice 2's
  `startTopPx` (matches by spine index; −∞/+∞ for sections not rendered; falls back to the
  iframe top) feeds the current-block rule. The reading panel and the current block can place
  the same locator differently.

## Goal and scope

Readers of the EPUB tests see only behavior the product has, at lower suite cost, and the EPUB
viewer finds a locator's rendered view one way. Included: the two older resume scenarios and
the steps and page-object methods only they use; `useEpubLocatorGeometry.ts`. Excluded: finer
in-block resume (an owner decision), the current-block rule, auto-marking, PDF, and the
extractor.

Preserved promises: SEED-059#story-1's landing, current block, first open, anchorless blocks,
and resume at the current block with its layout row in view; the reading control panel stays
content-anchored.

## Proof commands

- **E:** `SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/book_reading/epub_book.feature`
- **U:** `CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/lib/book-reading/currentBlockIdFromEpubView.spec.ts`
  and `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`

## Ordered slices

### 1. Remove the resume scenarios the reopen scenario supersedes
Type: Structure
Status: done
Accepted proof: E, 15 passing, including "Reopening resumes at the last place and shows its
block in the book layout"; `git grep` finds no callers of the removed steps or methods.
`epubHostViewportIntersectsMarker` keeps other callers and stays.
Size: about 5 minutes active; E runtime excepted.
Proof: E green without the two scenarios; "Reopening resumes at the last place and shows its
block in the book layout" is the surviving resume proof.

Delete "Resume EPUB reading at the last position" and "EPUB reading resumes at the scrolled
fragment, not the inferred block start", then the two steps and two page-object methods left
without callers (`git grep` to confirm). Leave the other scenarios unchanged.

### 2. One lookup finds a locator's rendered view
Type: Structure
Status: planned
Size: about 5 minutes active; E runtime excepted.
Proof: U, and E including "EPUB reading control panel is content-anchored" and the landing Rule.

Give `useEpubLocatorGeometry` one "rendered view for a locator" helper used by both
`resolveEpubLocatorElement` and `startTopPx`, keeping each caller's observable result. If the
two fallbacks cannot share one helper without changing where the panel or the current block
lands, stop and report the difference instead of choosing one.
