# Reopen an EPUB at the exact paragraph

Work item: **SEED-059#story-17**.
Status: **awaiting story refinement — not ready for slice-plan refinement or
execution.** Resume with `dough-story-refinement` on
[story 17](../../seeds/SEED-059-book-reading-uat-fixes.md#story-17) to clarify
its goal, scope, and key examples, then realign this plan before slice-plan
refinement or execution.

Source: resplit from the original story 15 plan (committed at `8e5d358124`,
part D). The slices below are provisional planning input.

## Known architecture

- The saved position is JSON in `book_user_last_read_position.reading_position_locator_json`;
  an optional `cfi` on `EpubLocator` needs no migration. Add
  `@JsonInclude(NON_NULL)` (like `PdfLocator`), since `EpubLocator` is also the
  block content locator.
- `BookFormat.java:95-116` rebuilds `new EpubLocator(href, frag)` on write and
  would drop the CFI; pass it through. Constructor callers:
  `BookBlockEpubContentLocators.java:61` and three controller test helpers.
- Regenerate the client with `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`,
  then `pnpm openapi:lint`.
- Frontend: `EpubBookViewer` emits `relocated` without a payload today; emit
  `rendition.location.start.cfi`. `BookReadingEpubView.proposeReadingPosition`
  sends the current block's start; add the CFI. `debounceLastReadPositionPatch`
  compares href and fragment only; include the CFI. The initial display tries
  the CFI, then href#fragment (the existing catch chain).
- epub.js 0.3.93 reports `location.start.cfi` and `display(cfi)` accepts it
  (`rendition.js:305-322,689-800`); precision in `flow: "scrolled"`,
  `manager: "continuous"` is unverified.
- Story 14 (delivered) removed the old resume scenarios and their scroll steps;
  this story adds its own.

## Provisional slices

1. **Probe:** measure `location.start.cfi` after scrolling mid-chapter at two
   widths and where `display(cfi)` lands. Stop and replan with the owner if it
   is not precise. About 10 min.
2. **Behavior:** a PATCH with a CFI is saved and returned
   (`NotebookBooksReadingPositionControllerTest`,
   `NotebookBooksGetReadingPositionControllerTest`). About 10 min.
3. **Behavior:** reopening an EPUB at another width shows the same paragraph at
   the top (new E2E in `epub_book.feature`). About 10 min.
