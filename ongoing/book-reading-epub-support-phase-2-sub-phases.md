# Phase 2 sub-phases: See the EPUB chapter structure

**Parent plan:** [book-reading-epub-support-plan.md](book-reading-epub-support-plan.md)

**Goal:** After uploading an EPUB, the user opens the reading page and sees the book's chapter structure in the layout sidebar. Blocks with and without direct content are correctly classified.

## Current state (after Phase 1)

- **Backend:** `persistNewEpubBook` saves `Book` row only — no `BookBlock` or `BookContentBlock` rows. `EpubAttachValidator` reads ZIP → container.xml → OPF (validates manifest + spine presence) but does not parse nav/spine content.
- **Frontend:** `BookReadingEpubPlaceholder.vue` passes `emptyBlocks` and `sideDrawerMode="titleOnly"` to `BookReadingBookLayout`. No chapter list.
- **Fixture:** `epub_valid_minimal.epub` has one spine item (`chapter.xhtml`), no nav document, no headings — too minimal for chapter-structure testing.
- **Tests:** Backend controller test asserts `getBlocks()` is `empty()` after EPUB attach. E2E asserts placeholder text and book name.

## Sub-phase plan

### 2.1 — Upgrade EPUB fixture + write failing tests (red)

- Replace `epub_valid_minimal.epub` with a richer fixture: nav document (`<nav epub:type="toc">`), 2–3 chapters with titles at different heading levels, paragraphs, and at least one image reference. Keep the fixture small but structurally representative.
- Update backend controller test (`NotebookBooksControllerTest`): change the `getBlocks() empty()` assertion to expect the correct BookBlock tree (titles, depths, count).
- Add E2E scenario in `epub_book.feature`: after uploading the EPUB and opening the reading page, assert chapter titles appear in the layout sidebar (reuse `expectBookLayoutRows`).
- Run both tests, confirm they fail for the right reason (empty blocks / titleOnly drawer).

**Files touched:** `epub_valid_minimal.epub`, `NotebookBooksControllerTest.java`, `epub_book.feature`, `book_reading.ts` (step defs), `bookReadingPage.ts` (page object if needed).

### 2.2 — Backend EPUB structure extraction → BookBlock tree (green)

- Extract shared ZIP/XML utilities from `EpubAttachValidator` (make `readEntryBytes`, `parseXmlSecure`, `parseContainerRootfileFullPath` package-private or move to a shared helper) so the new extractor can reuse them.
- Create `EpubStructureExtractor` (in `services/book/`) that: reads OPF manifest and spine → finds nav document href → reads nav XHTML → walks `<ol>/<li>/<a>` tree → builds `BookBlock` entities with correct `depth`, `layoutSequence`, `structuralTitle`.
- Wire into `persistNewEpubBook` in `BookService`: call the extractor, add blocks to book, save.
- Backend controller test goes green.

**Files touched:** `EpubAttachValidator.java` (refactor utilities), new `EpubStructureExtractor.java`, `BookService.java`.

### 2.3 — Frontend shows EPUB chapter blocks in drawer (green)

- Update `BookReadingEpubPlaceholder.vue`: pass `book.blocks` instead of `emptyBlocks`, remove `side-drawer-mode="titleOnly"` so the default blocks mode renders the chapter list.
- E2E goes green.

**Files touched:** `BookReadingEpubPlaceholder.vue`.

### 2.4 — BookContentBlock extraction from spine XHTML

- Extend `EpubStructureExtractor` to walk each spine XHTML document in the same parsing pass and extract content elements: paragraphs → `text`, images → `image`, tables → `table`.
- Store EPUB-specific data in `rawData` JSON: `href` (spine document), `fragment` (`#id` of the element's start anchor), and the element's text or src.
- Assign content blocks to the owning `BookBlock` based on the document structure (content under a heading belongs to that heading's block).
- Add backend controller test verifying `BookContentBlock` rows: types, count, rawData shape.

**Files touched:** `EpubStructureExtractor.java`, `NotebookBooksControllerTest.java`.

### 2.5 — Synthetic `*beginning*` block and heading fallback

- In `EpubStructureExtractor`: when content appears before the first structural heading in a spine document, create a synthetic `*beginning*` BookBlock (same semantics as PDF's synthetic beginning).
- When no nav document exists in the manifest, fall back to walking spine XHTML for `h1`–`h6` headings to build the BookBlock tree.
- Add backend unit tests for both edge cases (e.g. a fixture-less programmatic EPUB or a second minimal fixture with no nav).

**Files touched:** `EpubStructureExtractor.java`, test file(s) for edge cases.

### 2.6 — EPUB direct-content predicate

- Extend `BookBlockDirectContentPredicate.contributesDirectContent` to handle EPUB content block types. For EPUB: exclude `note`/`sidebar`-type asides from direct content (parallel to PDF's `header`/`footer`/`page_*` exclusions). Default EPUB types (`text`, `image`, `table`) count as direct content.
- Add unit test to `BookBlockDirectContentPredicateTest` covering EPUB-specific rules.

**Files touched:** `BookBlockDirectContentPredicate.java`, `BookBlockDirectContentPredicateTest.java`.

### 2.7 — Clean up and update plan

- Run full backend tests + the EPUB E2E spec to verify everything passes.
- Remove any dead code or temporary scaffolding.
- Update `book-reading-epub-support-plan.md`: mark Phase 2 as done.
- Delete this sub-phases file.

## Key design decisions

- **Reuse `EpubAttachValidator` utilities** rather than duplicating ZIP/XML parsing. The validator already handles secure XML parsing, entry reading with size caps, and container → OPF path resolution.
- **BookBlock tree from nav document** is the primary path (EPUB 3 spec). Heading fallback is the secondary path for EPUBs without a nav document.
- **Content blocks extracted in the same parsing pass** as the block tree for cohesion — both come from the same ZIP → OPF → spine → XHTML pipeline.
- **`rawData` shape for EPUB** includes `href` and `fragment` (not PDF's `bbox`/`page_idx` geometry) since EPUB content is addressed by document + fragment, not page coordinates.
