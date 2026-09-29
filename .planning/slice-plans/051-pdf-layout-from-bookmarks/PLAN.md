# Get a PDF book layout from the book's bookmarks

Work item: **SEED-059#story-3**.
Source: [refined story](../../seeds/SEED-059-book-reading-uat-fixes.md#story-3)
(owner decisions 2026-09-29).

## Goal and scope

A reader who attaches a PDF book with bookmarks gets one block per bookmark,
in the same order and nesting, titled as the bookmark, landing where it
points, with the book's text split between blocks by position. MinerU headings
and page headers become text inside blocks, not blocks.

Excluded (owner decisions): PDFs without bookmarks keep today's MinerU-heading
layout; books already attached keep their layout (no migration or
re-extraction); headings deeper than the bookmarks are not blocks; web PDF
attach, EPUB, AI Reorganize, and MinerU speed. The `bookLayout` roots path of
attach (the CLI's middle.json fallback, used only when MinerU finds no
headings) is unchanged.

## Architecture

- **PFE:**
  - Layout building: `AttachBookLayoutValidator.validatePdfAttachRequest` turns
    a `contentList` into layout with `MineruContentListLayoutBuilder`. Evolve
    that builder; do not add a second builder that repeats its nesting stack
    and `*beginning*` handling. The common rule for both sources: an ordered
    list of outline entries (level, title, position) splits the content
    items, and each item belongs to the last entry at or before it. MinerU
    headings are one source of entries, bookmarks the other.
  - Landing without own text: reuse the synthetic `beginning_anchor` payload
    idea (already used by the PDF `*beginning*` block and by EPUB) for a
    bookmark's start. Do not add a second locator representation.
  - Reading the outline: the backend has no PDF library; the frontend uses
    pdf.js and the CLI's MinerU environment has pypdf only for real MinerU
    (the E2E stub environment has neither). Add Apache PDFBox 3 to the backend
    and read the outline from the uploaded file in the attach flow
    (`AttachBookService.attach` already holds `fileBytes`). This mirrors EPUB,
    whose structure the backend reads from the uploaded file
    (`EpubStructureExtractor`). No CLI change.
- **Direct content:** `BookBlockDirectContentPredicate` already treats MinerU
  headings (`text_level` 1–3), headers, and footers as not direct content, so
  running headers inside a block change no reading-record behavior. Keep it
  unchanged.
- No North Star topic or ADR is affected: the Book stays private reading
  structure over its source attachment (NORTH-STAR "One attachment content
  model").

## Current decisions

- **An unreadable PDF is refused at attach** with a clear binding error
  ("not a readable PDF"), because its outline cannot be read and the reader
  could not show it either (ADR 0006: fail loudly, catch only for a clearer
  message). Tests that attach fake PDF bytes switch to real PDFs (slice 1).
- **Position mapping:** MinerU `content_list` bboxes are per page, 0–1000 from
  the top-left. A bookmark at PDF page `p` with `/XYZ` top `t` on a page of
  height `h` sits at `page_idx = p`, `y = (h − t) / h × 1000`. A bookmark
  without a top (for example `/Fit`) sits at the page top.
- **Assignment:** walk the content items in list order; move to the next
  bookmark while its position is at or above the item's top (`page_idx`, then
  `bbox` y0). Items before the first bookmark go to `*beginning*`.

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| The UAT PDFs carry bookmarks that match the book | pypdf 6.9.2 outline dump of `thinkpython2.pdf` (greenteapress.com) and arXiv `1706.03762` | Confirmed: Think Python 240 (22 top-level, 218 sections); Attention 22, nested to 3 levels (Model Architecture → Attention → Scaled Dot-Product Attention) |
| PDFBox 3 reads these outlines with page and top | One-off `java -cp pdfbox-app-3.0.5.jar Outline.java` over both UAT PDFs, `refactoring.pdf`, `blank_5_pages.pdf` | Confirmed. LaTeX PDFs use **named destinations** (all 240 and 22), resolved with `findNamedDestinationPage`; `refactoring.pdf` uses direct `/XYZ` destinations (6, top-level only); `blank_5_pages.pdf` has none. Page numbers are 0-based, like MinerU `page_idx` |
| Bookmark tops map onto MinerU bboxes with the formula above | `refactoring.pdf` page height 841.89; bookmark tops 357.2/785.2/762.2/565.2 vs headings in `mineru_output_for_refactoring.json` | Confirmed: 576/67/95/329 vs heading y0 577/68/96/331. Bookmark 1 (y 217) sits above its heading (y0 252) and below "Code Refactoring" (y0 72) |
| Most book scenarios are unaffected | Read `testabilityBook.attachBookToNotebook` and `book_reading.ts` | Confirmed: E2E attaches `blank_5_pages.pdf` (no bookmarks) with a MinerU fixture, so the existing layout path serves all current scenarios. The CLI attach step is defined but used by no feature |
| Backend tests attach fake PDF bytes | `grep -rn "0x25, 0x50" backend/src/test` | Confirmed: `NotebookBooksAttachControllerTest`, `NotebookBooksAttachNotebookFileControllerTest`, `BooksControllerTest`, `NotebookGitWebAttachmentDeleteControllerTest`, `NotebookGitBookSourceFileProtectionControllerTest`, via `NotebookBooksControllerTestBase.pdfFile` |
| Real MinerU can run locally for the acceptance slice | `ls .venv-mineru/bin` | **False today**: the venv has no `python` executable. Slice 6 repairs it or records the gap |

## Key examples → proof

| Promise | Slice | Proof |
| --- | --- | --- |
| One block per bookmark, titled as the bookmark; headings and page headers are text inside blocks; text before the first bookmark is `*beginning*` | 3 | Controller test: attach an in-code PDF with top-level bookmarks and a content list holding headings, a page-header copy, a "Chapter N" label, and paragraphs → layout and each block's content |
| Choosing a block lands where its bookmark points, also with no own text before the next bookmark | 3 | Same test: each block's first locator is at its bookmark position; E2E in slice 5 |
| Nesting follows the bookmarks, at any depth the layout allows | 4 | Controller test: three-level bookmarks → three-level layout |
| Named destinations and bookmarks without a top | 4 | Controller test: an in-code PDF using a named destination and a `/Fit` bookmark → blocks at the target page, the latter at its top |
| A PDF without bookmarks keeps today's layout | 2, 3 | Existing `MineruContentListLayoutBuilderTest` and book controller tests green; E2E `book_browsing.feature` green |
| An unreadable PDF is refused | 1 | Controller test: fake bytes → binding error |
| Real use: layout, landing, current block in the browser | 5 | E2E: attach `refactoring.pdf` (6 bookmarks) with its MinerU output → layout shows the bookmark titles; choose "4. Two Different Kinds of Refactoring" → page 4, and it is the current selection; scroll to page 5 → "5. Refactoring in Team Development" is current |
| Think Python and Attention key examples | 6 | Manual acceptance with real MinerU through the CLI |

Commands: backend
`CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test -Dspring.profiles.active=test --tests 'com.odde.donut.controllers.NotebookBooks*' --tests 'com.odde.donut.services.book.*'`;
E2E
`SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/book_reading/book_browsing.feature`.

## Slices

### 1. Attaching an unreadable PDF is refused
Type: Behavior
Status: done
Proof: controller test in `NotebookBooksAttachControllerTest`; all book and
notebook-file controller tests green.

Add PDFBox 3 to `backend/build.gradle`. PDF attach loads the uploaded file;
failure to load gives the binding error "not a readable PDF". Replace fake PDF
bytes in the tests listed under *Decisive premises* with a real one-page PDF
from one shared test helper (built with PDFBox in code, next to
`NotebookBooksControllerTestBase.pdfFile`). Nothing yet reads the outline.
Sizing: more than 5 minutes because of the test call sites; they are one
mechanical change with one proof loop.

### 2. The layout builder splits content by an ordered list of outline entries
Type: Structure
Status: done
Proof: existing `MineruContentListLayoutBuilderTest`, book controller tests,
and `book_browsing.feature` stay green unchanged.

Evolve `MineruContentListLayoutBuilder` to the common rule under
*Architecture*: it takes outline entries (level, title, position) and assigns
each content item to the last entry at or before it, with `*beginning*` for
items before the first. MinerU headings supply the entries, so layouts are
unchanged. Enables slice 3.

### 3. A PDF with bookmarks gets one block per bookmark
Type: Behavior
Status: done
Proof: controller test (attach, then read the book view) with an in-code PDF
holding top-level `/XYZ` bookmarks, plus the unchanged no-bookmark tests.

Read the outline in the attach flow; when it has entries, they replace the
MinerU headings as the builder's entries. Each bookmark block starts with an
anchor at its bookmark position, then holds the content items assigned to it.
Examples in the test: text before the first bookmark → `*beginning*`; a
MinerU heading, a page-header copy of it, and a "Chapter 3" label → content of
the bookmark block, not blocks; two bookmarks on the same page with nothing
between → both blocks exist and each lands at its own position.

### 4. Bookmarks nest and point anywhere a PDF can point
Type: Behavior
Status: done
Proof: controller tests with in-code PDFs: three-level bookmarks → three-level
layout; a named destination → the named page; a `/Fit` bookmark → its page top.

### 5. The browser shows and follows a bookmark layout
Type: Behavior
Status: done
Proof: new scenario in `book_browsing.feature`.

Let the E2E attach step send a real fixture PDF instead of the blank one
(`refactoring.pdf` with `mineru_output_for_refactoring.json`; both have 5
pages). Scenario: the layout shows `*beginning*` and the six bookmark titles
(including "2. The Usual Definition Is Not Enough", which MinerU reads as
"Defi nition"); choosing "4. Two Different Kinds of Refactoring" puts page 4
in view and makes it the current selection; scrolling to page 5 makes "5. Refactoring
in Team Development" current. Existing scenarios keep the blank PDF.

### 6. Real books match their contents (manual acceptance)
Type: Behavior (manual observation, no product change expected)
Status: done
Proof: observations recorded in *Learnings*.

Repair `.venv-mineru` (`python3 -m venv --clear` then
`pip install 'mineru[pipeline]'`). With the local dev stack, `/attach`
Think Python and the Attention paper through the CLI and check the story's
key examples: 22 top-level blocks with 218 sections under them, the Attention
nesting, landing on "The way of the program", and the current block through
chapter 14. If MinerU cannot be run, record that the real-book examples are
unproved and report it; do not claim them from slices 3–5.

## Learnings

- Slice 1: the PDF readability check is `BookFormat.validateAttachableFile`
  (EPUB delegates to `EpubAttachValidator`). Test PDFs come from
  `NotebookBooksControllerTestBase.onePagePdf(padding)` / `ONE_PAGE_PDF`;
  tests that store books through `makeMe` (not attach) keep fake bytes.
  Accepted proof: `NotebookBooksAttachControllerTest$AttachBook.rejectsAnUnreadablePdf`.
- Slice 2: `MineruContentListLayoutBuilder.buildLayout(entries, items)` (package
  private) holds the common rule; `OutlineEntry(level, title, startBlock,
  firstItem)` — `startBlock` is the entry's locator block, `firstItem` the index
  of its first item in `items`. The MinerU entry point removes headings from
  `items`; bookmark entries (slice 3) keep the full content list as `items` and
  use a `beginning_anchor`-style `startBlock`.
- Slice 3: `PdfBookmarkReader.read` loads the PDF once (readability check +
  bookmarks as `Bookmark(level, title, pageIdx, y)`); it reads only top-level
  `/XYZ` destinations and casts, so other destinations fail loudly until slice
  4 (recurse `children()` with level + 1; resolve named destinations; top only
  for `/XYZ` with top ≠ −1, else y = 0; mind `MAX_LAYOUT_DEPTH`).
  `buildLayoutFromBookmarks` falls back to headings without bookmarks. One
  `beginningAnchor(pageIdx, bbox)` builds both anchors; a bookmark anchor's bbox
  is `[0, y, 1000, y + 1]` (check highlight/current block in slice 5). Test PDFs
  live in `controllers/TestPdfs` (`pdfWithBookmarks`). A content item without
  `page_idx` in a bookmarked PDF fails loudly (MinerU always supplies it).
  Accepted proof: `NotebookBooksAttachPdfBookmarksControllerTest$AttachPdfWithBookmarks`.
- Slice 4: `PdfBookmarkReader` recurses children (level + 1) and resolves
  `/Dest`, GoTo actions, and named destinations (LaTeX uses GoTo → named);
  y only for `/XYZ` with a top, else page top. Deeper than `MAX_LAYOUT_DEPTH`
  fails with the existing depth binding error. A throwaway read of
  `thinkpython2.pdf` (240 bookmarks: 22/218), arXiv 1706.03762 (22: 7/12/3),
  and `refactoring.pdf` (6; y 217/576/67.6/67.6/94.9/328.9) threw nothing.
  Accepted proof: `NotebookBooksAttachPdfBookmarksControllerTest$AttachPdfWithNestedAndIndirectBookmarks`.
- Slice 5: `book_browsing.feature` Rule "A PDF with bookmarks is laid out by its
  bookmarks" attaches the real `refactoring.pdf` with its MinerU output
  (`attachBookToNotebook` takes the PDF fixture; blank path is
  `attachBlankPdfBookToNotebook`). The thin anchor bbox needed no product change.
  Accepted proof: whole `book_browsing.feature` 6/6 and `reorganize_layout.feature`
  8/8. Observed, out of scope: opening a second notebook's book in-app kept the
  first book's layout and PDF (and patched the first notebook's reading
  position), so each Rule has its own Background — a candidate for the backlog.
- Slice 6 (real MinerU 3.4.5 pipeline, CLI `/attach` against a disposable E2E
  stack, browser via Claude in Chrome), all observed:
  - Think Python: 241 blocks = `*beginning*` + 22 top-level (Preface, 18
    chapters, 3 appendices) + 218 sections; all 240 (depth, title, page) match
    a pypdf bookmark dump in order; no "Chapter N" or page-header blocks.
  - Attention: Model Architecture → Attention → Scaled Dot-Product Attention.
  - "The way of the program" lands on page 23/244 with the chapter heading at
    the top and becomes the current selection; "What is a program?" starts on
    the same page.
  - Wheel-scrolling chapter 14 moves the current block through all 12 sections
    in order, no copies.
  - Owner question: the story's Think Python example says "Index" once, but the
    PDF has no Index bookmark, so there is no Index block (its text is inside
    "Analysis of Algorithms"). Consistent with "bookmarks decide the layout";
    the example's wording may need correcting at wrap-up.
  - Environment: `.venv-mineru` (main checkout, gitignored) rebuilt on Homebrew
    Python 3.12 with `mineru[pipeline]==3.4.5` and `six`. MinerU 4.x has no
    `pipeline` extra or `mineru.cli.common`, so the unpinned
    `pip install 'mineru[pipeline]'` in `cli/python/mineru_book_outline.py` and
    `regenerate_mineru_output_for_refactoring.sh` no longer works — follow-up.

## Execution complete

Product advice:
- Queue a bug story first: in slice 5's E2E, opening a second notebook's book
  in-app kept the first book's layout and PDF and sent
  `PATCH /api/notebooks/1/book/reading-position` from `/notebooks/2/book`, so a
  reader can write reading position to the wrong book.
- Pin MinerU (`mineru[pipeline]==3.4.5` plus `six`) where the CLI docstring and
  `regenerate_mineru_output_for_refactoring.sh` install it; the unpinned install
  now gets MinerU 4.x, which the CLI cannot import (DD-161).
- At wrap-up, correct the Think Python key example's "'Index' once": the PDF has
  no Index bookmark, so the delivered rule (bookmarks decide the layout) gives
  no Index block.
- No change to the order of the remaining SEED-059 stories.
