# EPUB support — phased delivery plan

**Goal:** Deliver EPUB support as a sequence of small, user-visible slices that reuse the existing PDF book-reading architecture where the domain is shared, while keeping EPUB-specific behavior in tightly scoped modules and concrete `book.format` branches.

**Companion docs:** [epub-research-quest.md](book-reading-epub-research-quest.md), [architecture-roadmap.md](doughnut-book-reading-architecture-roadmap.md), [ux-ui-roadmap.md](book-reading-ux-ui-roadmap.md), [user-stories.md](book-reading-user-stories.md).

## Delivery principles

- **Risk-first:** Burn down the highest product and architecture risks early, not after a large amount of EPUB-only code has accumulated.
- **One behavior per phase:** Every phase must let the user do one more meaningful thing end to end.
- **No wasted work:** If delivery stops after any phase, the shipped result is still coherent and worth keeping.
- **High cohesion:** Reuse shared book-reading concepts and modules from the PDF path where the behavior is the same: attach orchestration, reader shell, block selection/current-block state, reading-position persistence, reading-record persistence. Code that belongs to the same parsing pass should stay in the same phase.
- **Low coupling:** Avoid abstract interface layers unless they are clearly paying for themselves. Prefer format-aware branching at the boundary modules and keep PDF/EPUB viewer internals separate.
- **Shared domain vocabulary:** **BookBlock** and **BookContentBlock** remain the core domain concepts for both formats. EPUB should fit those concepts rather than introducing a second tree or second progress model.
- **Upload vs reading:** **EPUB** attach in the browser is the primary EPUB path (raw upload; no client layout). **PDF** books are **read** in the web app like today, but **PDF attach is CLI-only:** the CLI runs **MinerU** (or equivalent) to produce `layout` / `contentList` for `attach-book`, which the browser does not do. Roadmap **Phase 9** adds CLI attach for **`.epub`** (no MinerU, no preprocessing).
- **E2E strategy:** Shared reader-shell behaviors do not need a full EPUB duplicate matrix. EPUB still gets representative end-to-end proof in each shipped phase, and EPUB-specific behavior gets its own dedicated E2E coverage.

## Supported EPUB contract for v1

The first shipped EPUB slices should support a clearly defined subset and fail fast outside it.

- **Supported first:** reflow EPUB, single-rendition package, non-DRM, normal XHTML spine content, chapter structure available from nav document or heading fallback.
- **Fail fast with clear error:** DRM/encrypted EPUB, invalid container/package structure, unsupported package shapes we intentionally defer, and other cases where Doughnut cannot reliably build a `BookBlock` tree or render safely.
- **Deferred, not silently half-supported:** fixed-layout EPUB, vertical writing, complex multi-rendition selection, and other special cases called out in the research doc.

## Shared seams to keep cohesive

These are the places where EPUB should reuse the existing book-reading flow instead of creating parallel feature stacks.

- **Attach orchestration:** one attach entry point with format dispatch; PDF and EPUB differ in extraction path, not in the notebook-level user concept.
- **Stored book file:** one file-serving concept for `GET …/book/file`; media type and downstream viewer differ by format.
- **Reader shell:** one page-level flow for loading book metadata, loading file bytes, showing the drawer, handling selected block vs current block, and rendering the format-specific viewer.
- **Reading position:** one user-facing API surface with a format-specific payload branch.
- **Reading records:** one reading-record concept and persistence flow; only geometry/content-boundary detection differs by format.

## Phase 1: Upload a supported EPUB and see it attached (done)

**Why first:** This burns down the API-generalization and storage risk — the backbone that every later phase depends on. Today the backend is hardcoded PDF-only (`attachBookWithPdf`, `getBookPdfFile`, `APPLICATION_PDF`, `validateAttachRequest` rejects non-pdf). **PDF** attach already goes through the **CLI** (MinerU-backed layout). There is no EPUB attach in the browser yet — EPUB-related attach today is via testability / future CLI. Making the system format-aware and adding **browser EPUB attach** is foundational before EPUB parsing is layered on.

**Behavior:**
- *Pre:* Notebook exists, has no attached book, user has a supported `.epub` file.
- *Trigger:* User uploads the `.epub` in the notebook page.
- *Post:* The notebook shows the attached EPUB book. The reading page opens and shows the book name. Unsupported or invalid EPUBs (DRM, bad container) fail with a clear user-visible error instead of partial import. **PDF** notebooks attached earlier (via CLI) still **open and read** in the browser unchanged.

**User value after this phase:** "I can upload an EPUB into Doughnut and see it attached to my notebook, with clear feedback when the file is not supported."

**Scope:**

*Backend:*
- Generalize attach orchestration from PDF-only to format-aware: rename/refactor `attachBookWithPdf` → format-dispatched attach, accept `format: "epub"` with raw file upload and no client-provided `layout` or `contentList`.
- Detect unsupported/invalid EPUB early (DRM via `encryption.xml`, invalid container/package structure) and return clear user-visible errors.
- Serve the raw `.epub` file with `application/epub+zip` and the correct filename from `GET …/book/file`.
- Book deletion works for EPUB through the same storage flow.

*Frontend:*
- Build the frontend attach UI for **`.epub` only** (multipart to `attach-book`). Do **not** offer PDF upload in the browser — PDF attach stays on the **CLI** because **MinerU** (or the same pipeline) is required to supply layout / `contentList`.
- Make the attach UI and copy format-aware instead of PDF-only wording where it matters (EPUB vs “use CLI for PDF”).
- Let the reading page load an EPUB book without crashing: show the book name and a temporary main-pane placeholder. The structure drawer is empty or shows just the book title (no chapter tree yet).

*Fixture:*
- Commit one small representative `.epub` fixture with a TOC or headings, a few chapters, paragraphs, and at least one image.
- Commit one intentionally unsupported `.epub` fixture (e.g. DRM-flagged or invalid container) for error-path testing.

**Testing:**
- Backend controller test: attach EPUB and verify stored book format, file serving with correct media type, and book record persistence. Attach unsupported EPUB and verify clear error response.
- E2E: upload EPUB in the frontend and see it attached. Upload unsupported EPUB and see a clear error message.

## Phase 2: See the EPUB chapter structure (done)

**Why now:** This is the core EPUB parsing risk. It proves we can extract meaningful structure from an EPUB package and map it into the existing `BookBlock` and `BookContentBlock` domain concepts. The BookBlock tree and BookContentBlock stream come from the same parsing pass (ZIP → OPF → spine → XHTML), so extracting both together is higher cohesion than revisiting the parser later.

**Behavior:**
- *Pre:* EPUB book is attached to the notebook (from Phase 1).
- *Trigger:* User opens the reading page.
- *Post:* The layout sidebar shows the book's chapter structure. Blocks with and without direct content are correctly classified.

**User value after this phase:** "I can upload an EPUB and browse its chapter structure in Doughnut."

**Scope:**

*Backend:*
- Add server-side EPUB structure extraction: unzip, parse `container.xml`, OPF, manifest, spine, nav document / heading fallback, and build the `BookBlock` tree.
- In the same parsing pass, extract `BookContentBlock` rows from spine XHTML (paragraphs → `text`, images → `image`, tables → `table`, with EPUB-specific data in `rawData` including `href` and fragment `#id` for each block's start anchor).
- Preserve shared `BookBlock` semantics, including synthetic `*beginning*` when content appears before the first structural heading.
- Extend `BookBlockDirectContentPredicate` for EPUB-specific types (e.g. exclude `note`/`sidebar` asides from direct content, parallel to PDF's `header`/`footer`/`page_*` exclusions).

*Frontend:*
- Show the populated structure drawer on the reading page for EPUB books (replace the Phase 1 empty/title-only drawer).

**Testing:**
- Backend controller test: attach EPUB and verify `BookBlock` tree structure, `BookContentBlock` extraction, and `hasDirectContent` classification.
- Focused unit tests for EPUB extraction edge cases: nav-only vs heading-fallback, `*beginning*` synthetic block, multi-level headings, EPUB-specific direct-content predicate rules.
- E2E: upload EPUB, open reading page, see chapter list in the layout sidebar.

## Phase 3: Open the EPUB and read its content (done)

**Why now:** This burns down the Readium renderer integration risk and the security/CSP risk — both are significant unknowns. Readium + Vue 3 integration, CSP for untrusted EPUB HTML, shadow DOM implications for testing, scroll behavior on mobile Safari, and bundle size all need to be validated. A time-boxed Readium spike should precede or be part of this phase.

**Behavior:**
- *Pre:* EPUB book is attached and has a chapter structure (from Phases 1–2).
- *Trigger:* User opens the reading page.
- *Post:* EPUB content is visible in the main pane in a safe, readable scrolled view alongside the structure drawer. User can click a chapter in the layout and the reader jumps using the block's spine `href` (Phase 4 adds `#fragment` for sub-sections in the same spine file).

**User value after this phase:** "I can open and read my EPUB in Doughnut and use the chapter list to jump around."

**Scope:**

*Frontend:*
- Add `EpubBookViewer.vue` using Readium in scrolled mode.
- Replace the Phase 1 placeholder with real EPUB rendering.
- Keep shared reader-shell logic together: book/file loading, file fetch, drawer behavior, and format-aware viewer selection.
- Wire chapter navigation: clicking a block calls epub.js `rendition.display` with `epubStartHref` (spine path from Phase 2; Phase 4 extends this to `path#id` for nav fragments).

*Security and ops:*
- Validate CSP behavior required for EPUB rendering (inline styles, blob URLs, embedded fonts).
- Confirm user-uploaded EPUB scripting stays disabled.
- If Readium uses shadow DOM, document the Cypress configuration needed (e.g. `includeShadowDom`).

**Testing:**
- E2E: open the EPUB reading page and assert known fixture text is visible in the main pane. Click a chapter and verify content from that chapter becomes visible.
- Focused frontend test for viewer mount/unmount lifecycle.

## Phase 4: Navigate precisely to any chapter or section (done)

**Why here:** Spine-only `href` jumps every block in the same XHTML file to the same scroll position. This phase carries the nav fragment through extraction and API so layout clicks resolve to the correct in-file anchor.

**Behavior:**
- *Pre:* EPUB is open in the reader and the structure drawer is visible.
- *Trigger:* User clicks a `BookBlock` in the layout.
- *Post:* The reader jumps precisely to the corresponding chapter or section in the EPUB, including sub-sections that share a spine item with their parent.

**User value after this phase:** "I can use the structure to jump precisely to any section in my EPUB."

**Scope (shipped):**

*Backend:*
- During extraction, set the first content block's `fragment` in `rawData` from the nav row's fragment id (`#anchor`) when present.
- `BookBlock.getEpubStartHref()` returns `href` + `fragment` for EPUB blocks (`GET …/book` exposes `epubStartHref` as today). No separate locator DTO or DB migration.

*Frontend:*
- `BookReadingEpubView` passes `block.epubStartHref` to `EpubBookViewer.displayEpubTarget` → epub.js `rendition.display(href#fragment)`.

*E2E:*
- Assert subsection text is visible; viewport checks use epub.js's `.epub-container` (the real scroll viewport), not only the Vue host wrapper.

**Later phases:** Full Readium-style locator objects (CFI, progression), reading-position resume, and scroll-sync still build on this `epubStartHref` string or extend the API as needed.

## Phase 5: Scroll the EPUB and see where I am in the layout (done)

**Why here:** This completes the core structure-to-reader sync after rendering and explicit navigation already work.

**Behavior:**
- *Pre:* EPUB is open in the reader.
- *Trigger:* User scrolls through the EPUB content.
- *Post:* The layout highlights the current `BookBlock`, distinct from the selected block when applicable.

**User value after this phase:** "As I read, Doughnut keeps the structure synced with where I am."

**Scope (shipped):**
- `EpubBookViewer.vue` listens to epub.js `relocated` and `displayed` events and emits a single `relocated` payload `{ href }`. `displayed` is kept because epub.js does not reliably fire `relocated` on the initial `display()` call in continuous/scrolled mode.
- `currentBlockIdFromEpubLocation(blocks, href)` maps the spine href (plus optional `#fragment`) to the owning `BookBlock.id`, preferring the last in-preorder match with a matching fragment, falling back to the last path-only match. It tolerates manifest-relative vs package-root path differences.
- `BookReadingEpubView.vue` feeds each relocation through the shared `createCurrentBlockIdDebouncer` (120 ms) and passes the resulting `currentBlockId` to `BookReadingBookLayout`. Clicks `commitNow` immediately with the clicked block id so the layout never flickers through a transient scroll-derived id.
- `selectedBlockId` is tracked independently of `currentBlockId` so the layout distinguishes the two after a scroll moves past the selection.

**Testing (shipped):**
- `frontend/tests/lib/book-reading/currentBlockIdFromEpubLocation.spec.ts` covers exact/relative path matching, preorder tie-breaking, fragment matching, and fallback cases.
- `e2e_test/features/book_reading/epub_book.feature` scenarios "Scrolling the EPUB updates the current block in the layout" and "EPUB current block updates on scroll; selection stays on explicit choice" exercise initial load, click-nav, and scroll-triggered updates along with the selected-vs-current distinction.

## Phase 6: Leave and return to the same EPUB position (done)

**Why here:** Once basic reading and navigation work, resume becomes a clean, valuable standalone slice with moderate technical risk.

**Behavior:**
- *Pre:* User has opened an EPUB and scrolled to a meaningful position.
- *Trigger:* User leaves the reading page and later opens the same book again.
- *Post:* The reader returns to the saved EPUB position and restores the selected block when applicable.

**User value after this phase:** "I can continue my EPUB where I left off."

**Scope (shipped):**
- Reading-position storage widened with a nullable `epub_locator VARCHAR(512)` column (Flyway `V300000143`); PDF `page_index`/`normalized_y` became nullable so EPUB rows omit them.
- `BookLastReadPositionRequest` accepts either `pageIndex`+`normalizedY` or `epubLocator`; `BookService.upsertLastReadPosition` rejects payloads with neither and clears the inactive variant on update.
- `createLastReadPositionPatchDebouncer` widened to a discriminated union with a dedicated `proposeEpubLocator` entry point so the PDF call site is unchanged.
- `BookReadingPage.vue` fetches the reading position for both formats and passes `initialEpubLocator` / `initialSelectedBlockId` into `BookReadingEpubView`.
- `BookReadingEpubView.vue` saves the current block's `epubStartHref` on relocate and on selection changes, and flushes the debouncer on unmount so leaving the page persists the latest position. The viewer seeds the current-block debouncer from the saved locator so the layout is in sync before the first scroll-driven event.
- `EpubBookViewer.vue` calls `r.display(initialLocator)` on first open when a saved locator is present, with a safe fallback to the default no-arg `display()`.

**Testing (shipped):**
- Backend `NotebookBooksControllerTest` covers EPUB round-trip, the missing-payload rejection, and that PDF fields are cleared when an `epubLocator` arrives.
- `frontend/tests/lib/book-reading/debounceLastReadPositionPatch.spec.ts` covers EPUB and PDF dedupe and the variant switch.
- E2E scenario "Resume EPUB at the last read position after leaving" in `epub_book.feature` proves the round-trip; `bookReadingPage.leaveEpubReadingViewAndReturn()` waits for the pending PATCH to flush before reload.

**v1 locator format:** the epub.js spine href (e.g. `OEBPS/chapter2.xhtml#section-beta-two`) — same shape as `epubStartHref`. The schema is a plain `VARCHAR(512)` so a future CFI upgrade does not require another migration.

## Phase 7: Mark an EPUB block as read (done)

**Why separate from direct-content automation:** Manual progress recording is meaningful on its own. It should not wait for the harder EPUB-specific direct-content geometry work.

**Behavior:**
- *Pre:* EPUB is open and a block is selected.
- *Trigger:* User marks the selected block as read in the Reading Control Panel.
- *Post:* The block shows the read disposition in the layout, and the selection advances to the next block in preorder.

**User value after this phase:** "I can record my reading progress in an EPUB the same way I do for a PDF."

**Scope (shipped):**
- `BookReadingEpubView.vue` uses the same `useNotebookBookReadingRecords(notebookId)` composable as the PDF view, calls `syncFromServer()` on mount, and feeds `bookReading.dispositionForBlock` straight into `BookReadingBookLayout`'s `dispositionForBlock` prop — no parallel records state or UI contract.
- `ReadingControlPanel.vue` is reused. EPUB drives it with `:anchor-top-px="null"` (bottom-docked) and `:show-skim-and-skip="false"` so only the "Read" button is rendered; PDF keeps the full Read/Skim/Skip panel via the default. Phase 8 upgrades EPUB to anchored placement and decides whether to surface Skim/Skip.
- "Mark and advance": on a successful `submitReadingDisposition(blockId, "READ")`, the shared `nextBookBlockAfter(blocks, blockId)` helper (`frontend/src/lib/book-reading/nextBookBlockAfter.ts`) picks the successor; EPUB applies it by updating `selectedBlockId`, calling `epubViewerRef.displayEpubTarget(next.epubStartHref)`, and committing the current-block debouncer.

**Testing (shipped):**
- `frontend/tests/lib/book-reading/nextBookBlockAfter.spec.ts` covers the shared helper (middle/last/missing/empty cases).
- E2E scenario "Mark an EPUB block as read advances the selection" in `e2e_test/features/book_reading/epub_book.feature` proves the full user path: click a block, mark as read in the Reading Control Panel, see the row marked as read in the layout, and the next block selected.

**Out of scope (deferred to Phase 8):** EPUB-specific content-aware panel anchoring, no-direct-content auto-mark, and any EPUB UI for skimmed/skipped.

## Phase 8: EPUB direct-content boundaries and no-direct-content automation (done)

**Why here:** `BookContentBlock` rows are already persisted from Phase 2 and `BookBlockDirectContentPredicate` is already correct on the backend. This phase resolves those content boundaries in the live DOM for panel geometry and auto-mark behavior, and at the same time collapses PDF- and EPUB-specific content-location concepts onto one shared model.

**Behavior:**
- *Pre:* EPUB is open and the book has blocks with and without direct content.
- *Trigger:* User reads forward through the book, including blocks that have no direct content.
- *Post:* No-direct-content blocks are auto-marked, and the Reading Control Panel anchors below the last direct-content locator instead of the Phase 7 bottom-docked fallback. EPUB also supports Skim / Skip dispositions alongside Read.

**User value after this phase:** "EPUB reading progress behaves intelligently, including the blocks that are only structure, and supports Skim / Skip like PDF."

**Scope (shipped):**

*Backend:*
- Generalized `PageBbox` + `allBboxes` and `epubStartHref` into a discriminated `ContentLocator` (`PdfLocator` + `EpubLocator` variants) exposed as `BookBlock.contentLocators` on `GET …/book`. Derived rules like `hasDirectContent := contentLocators.length > 1` and block-start / last-direct-content locators are now format-agnostic.
- EPUB extraction emits `EpubLocator` entries for each `BookContentBlock` that `contributesDirectContent` alongside the anchor entry.

*Frontend/viewer:*
- Unified viewer contract `BookReaderViewerRef` (`displayLocator`, `resolveLocatorRect`, `isLocatorBottomVisible`, `readingPanelAnchorTopPx`) implemented by both `PdfBookViewer` and `EpubBookViewer`. PDF-only geometry (snap-back, zoom, stored-position scroll) stays on a PDF sub-interface.
- `EpubBookViewer` resolves EPUB locators to the section iframe's DOM element by spine href + `#fragment`, returning rects in the rendition host's coordinate space so the panel-anchor consumer code is identical to PDF.
- `BookReadingEpubView` anchors `ReadingControlPanel` below the last direct-content locator via the unified API, falling back to bottom-dock only when the locator cannot be resolved, and wires Skim / Skip through the same mark-and-advance path as Read.
- Auto-mark-no-direct-content-predecessor is a shared composable (`useAutoMarkNoDirectContentPredecessor`) driven by `contentLocators.length === 1`; both `BookReadingContent` (PDF) and `BookReadingEpubView` (EPUB) install it.
- `useBookReadingSnapBack` reads `block.contentLocators[last]` through the viewer's `readingPanelAnchorTopPx(locator, obstructionPx)` — no PDF-only panel-anchor reads remain.

**Testing (shipped):**
- Backend controller tests cover `contentLocators` shape for both PDF (anchor + direct entries from `BookContentBlock` rawData) and EPUB (anchor + per-direct-content-block `EpubLocator` on the minimal fixture, including `#fragment` carry-through).
- Frontend unit tests cover the shared auto-mark composable and the PDF / EPUB locator helpers.
- E2E `epub_book.feature` scenarios prove "structural-only predecessor auto-marks on entry", "Reading Control Panel anchors below direct content for the selected block", and "Skim advances the selection". PDF `reading_record.feature` + `book_browsing.feature` remain green unchanged.

## Phase 9: Attach an EPUB from the CLI with no preprocessing (done)

**Why last:** CLI EPUB attach is valuable but lower risk and lower reach than the frontend path. It reuses the server-side EPUB flow rather than a second extraction path.

**Behavior:**
- *Pre:* User has selected a notebook in the CLI and has an `.epub` file.
- *Trigger:* User runs `/attach <file.epub>`.
- *Post:* The EPUB is attached successfully, with raw bytes sent to the backend and no CLI-side preprocessing or layout payload.

**User value after this phase:** "I can attach an EPUB from the CLI without Python or MinerU."

**Scope (shipped):**
- `/attach` detects format from the file extension (`detectBookAttachFormat` in `cli/src/commands/notebook/notebookAttachSlashCommand.tsx`) and rejects anything other than `.pdf` or `.epub` with a clear error.
- One shared upload helper `attachNotebookBookFile(notebookId, metadata, absolutePath)` in `cli/src/backendApi/doughnutBackendClient.ts` derives the multipart Blob `Content-Type` from `metadata.format` (`application/pdf` vs `application/epub+zip`). No second attach endpoint or extraction path.
- EPUB branch (`runEpubAttach`) sends raw bytes with `{ bookName, format: 'epub' }` and no `layout`/`contentList`; MinerU is not invoked. PDF branch (`runPdfAttach`) keeps the existing MinerU outline pipeline unchanged.
- User-facing strings are format-agnostic where the path is shared: CommandDoc usage is `/attach <path to .pdf or .epub>`, spinner label is "Attaching book…", HTTP 413 reads as a generic upload-too-large message.

**Testing (shipped):**
- `cli/tests/InteractiveCliApp.useNotebook.test.tsx` exercises the full flow through `InteractiveCliApp`: EPUB happy path and structure excerpt, EPUB call to `attachNotebookBookFile` with epub metadata and the resolved absolute path, "no MinerU on EPUB", unknown-extension rejection, and missing-file rejection for both `.pdf` and `.epub`. PDF scenarios remain green unchanged.
- No dedicated Cypress scenario for CLI EPUB attach — browser EPUB reading and structure are already covered by `e2e_test/features/book_reading/epub_book.feature` (upload path). Rationale: [book-reading-epub-phase-9-sub-phases.md](book-reading-epub-phase-9-sub-phases.md) §9.4–9.5.

## Cross-cutting concerns

### E2E fixture and assertions

- Reuse one small committed `.epub` fixture across phases (happy path). Keep a second fixture for error-path testing (unsupported EPUB).
- Prefer DOM-text assertions for EPUB content instead of OCR — EPUB renders to HTML, not canvas.
- If Readium uses shadow DOM, configure Cypress with `includeShadowDom` or custom commands to pierce the shadow tree.
- Reuse the same layout-side assertion hooks as PDF where the shell behavior is shared.

### OpenAPI and generated types

- Regenerate TypeScript only in the phases that actually change the API surface.
- API-touching phases: 1 (format-aware attach + file serving), 2 (BookBlock structure + `epubStartHref` spine path), 4 (`epubStartHref` may include `#fragment`; still a single string field), 6 (reading-position schema extension for EPUB, if beyond `epubStartHref`), 8 (generalize `allBboxes` / `PageBbox` / `epubStartHref` into `contentLocators` / `ContentLocator` — see the Phase 8 sub-phases plan for the migration order).

### Phase discipline

- Each phase should be implemented and tested as its own completed slice.
- Remove temporary fallback/interim behavior as soon as the replacement phase lands (Phase 3 rough navigation → Phase 4 precise locators; Phase 7 bottom-docked panel → Phase 8 content-aware geometry).
- Keep PDF behavior fully working after every phase.

## Phase summary

| Phase | User value | Primary risk | Size |
|-------|-----------|-------------|------|
| 1 | Upload EPUB and see it attached | API generalization from PDF-only; frontend EPUB attach (PDF attach stays CLI + MinerU) | Medium |
| 2 | Browse EPUB chapter structure | Server-side EPUB parsing, BookBlock + BookContentBlock extraction | Medium |
| ~~3~~ | ~~Open EPUB and read content, jump to chapters~~ | ~~Done~~ | ~~Done~~ |
| 4 | Precise navigation to any section | `epubStartHref` path#fragment, epub.js `display` | Done |
| 5 | Scroll EPUB and see current block | Viewport-to-block mapping without PDF bbox geometry | Done |
| 6 | Resume EPUB reading position | Locator persistence, reading-position schema extension | Done |
| 7 | Mark EPUB block as read | Reusing reading-record UI and state without over-coupling to PDF | Done |
| 8 | Intelligent EPUB direct-content progress; EPUB Skim / Skip | DOM-based boundary resolution and content-aware panel geometry | Done |
| 9 | CLI EPUB attach | Raw upload transport only; Vitest CLI coverage, no CLI EPUB E2E | Done |
