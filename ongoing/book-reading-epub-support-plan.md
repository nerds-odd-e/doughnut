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

## Phase 3: Open the EPUB and read its content (planned)

**Why now:** This burns down the Readium renderer integration risk and the security/CSP risk — both are significant unknowns. Readium + Vue 3 integration, CSP for untrusted EPUB HTML, shadow DOM implications for testing, scroll behavior on mobile Safari, and bundle size all need to be validated. A time-boxed Readium spike should precede or be part of this phase.

**Behavior:**
- *Pre:* EPUB book is attached and has a chapter structure (from Phases 1–2).
- *Trigger:* User opens the reading page.
- *Post:* EPUB content is visible in the main pane in a safe, readable scrolled view alongside the structure drawer. User can click a chapter in the layout and the reader scrolls roughly to that chapter's spine document (interim navigation, upgraded to precise locators in Phase 4).

**User value after this phase:** "I can open and read my EPUB in Doughnut and use the chapter list to jump around."

**Scope:**

*Frontend:*
- Add `EpubBookViewer.vue` using Readium in scrolled mode.
- Replace the Phase 1 placeholder with real EPUB rendering.
- Keep shared reader-shell logic together: book/file loading, file fetch, drawer behavior, and format-aware viewer selection.
- Wire rough chapter navigation as interim behavior: clicking a block navigates to the start of the corresponding spine document using the `href` persisted in Phase 2. This is intentionally imprecise (spine-document level, not locator-precise); Phase 4 upgrades to the canonical locator contract.

*Security and ops:*
- Validate CSP behavior required for EPUB rendering (inline styles, blob URLs, embedded fonts).
- Confirm user-uploaded EPUB scripting stays disabled.
- If Readium uses shadow DOM, document the Cypress configuration needed (e.g. `includeShadowDom`).

**Testing:**
- E2E: open the EPUB reading page and assert known fixture text is visible in the main pane. Click a chapter and verify content from that chapter becomes visible.
- Focused frontend test for viewer mount/unmount lifecycle.

## Phase 4: Navigate precisely to any chapter or section (planned)

**Why here:** Rough navigation from Phase 3 uses spine-document-level jumps. This phase establishes the canonical EPUB locator contract aligned with Readium `Locator` semantics, enabling precise navigation to sections within a spine document. This contract is needed before more reader behavior (scroll sync, resume) accumulates.

**Behavior:**
- *Pre:* EPUB is open in the reader and the structure drawer is visible.
- *Trigger:* User clicks a `BookBlock` in the layout.
- *Post:* The reader jumps precisely to the corresponding chapter or section in the EPUB, including sub-chapter sections within a spine document.

**User value after this phase:** "I can use the structure to jump precisely to any section in my EPUB."

**Scope:**

*Contract and architecture:*
- Adopt the EPUB block-start contract: expose an EPUB-native block locator aligned with Readium `Locator` semantics (`href`, `type`, `locations` with `partialCfi` and/or `progression`, optional `text`).
- Keep PDF navigation data unchanged and branch on `book.format` at the reader boundary.
- Remove the Phase 3 interim rough navigation and replace with locator-based navigation.

*Backend:*
- Persist the EPUB locator data needed for layout-to-content navigation (from the extraction pass — extend the `rawData` or add locator fields on `BookBlock` for EPUB).
- Extend `GET …/book` to expose that locator data for EPUB blocks.

*Frontend:*
- Wire `BookReadingContent.vue` so clicking a block uses the EPUB locator to navigate via Readium.
- Reuse the existing selected-block flow rather than inventing an EPUB-specific selection model.

**Testing:**
- E2E: click a sub-chapter section in the EPUB layout and verify the target section becomes visible (not just the spine document start).

## Phase 5: Scroll the EPUB and see where I am in the layout (planned)

**Why here:** This completes the core structure-to-reader sync after rendering and explicit navigation already work.

**Behavior:**
- *Pre:* EPUB is open in the reader.
- *Trigger:* User scrolls through the EPUB content.
- *Post:* The layout highlights the current `BookBlock`, distinct from the selected block when applicable.

**User value after this phase:** "As I read, Doughnut keeps the structure synced with where I am."

**Scope:**
- Use Readium relocation events and EPUB locator progression to derive the current block.
- Reuse shared shell behavior from the PDF reader where the behavior is format-agnostic: debounce, current-block state, selected-vs-current distinction, live region announcement, layout scroll-into-view.
- Keep EPUB-specific logic limited to mapping the current Readium location to the owning `BookBlock`.

**Testing:**
- E2E: scroll the EPUB and verify the expected block becomes the current block in the layout.
- Keep layout-side assertions the same style as PDF where possible.

## Phase 6: Leave and return to the same EPUB position (planned)

**Why here:** Once basic reading and navigation work, resume becomes a clean, valuable standalone slice with moderate technical risk.

**Behavior:**
- *Pre:* User has opened an EPUB and scrolled to a meaningful position.
- *Trigger:* User leaves the reading page and later opens the same book again.
- *Post:* The reader returns to the saved EPUB position and restores the selected block when applicable.

**User value after this phase:** "I can continue my EPUB where I left off."

**Scope:**
- Extend reading-position persistence with an EPUB-specific locator payload while keeping the existing PDF shape (`pageIndex` + `normalizedY`) intact. This touches the API schema — `BookLastReadPosition` needs a format-specific branch or additional fields for the EPUB locator.
- Reuse the same `GET/PATCH …/reading-position` user-facing flow.
- Debounce EPUB position updates from reader relocation events.
- Restore from the saved locator on load.

**Testing:**
- Backend test for EPUB reading-position round-trip.
- E2E: navigate to a known EPUB section, leave, return, and verify the same section is shown again.

## Phase 7: Mark an EPUB block as read, skimmed, or skipped (planned)

**Why separate from direct-content automation:** Manual progress recording is meaningful on its own. It should not wait for the harder EPUB-specific direct-content geometry work.

**Behavior:**
- *Pre:* EPUB is open and a block is selected.
- *Trigger:* User marks the selected block as read, skimmed, or skipped in the Reading Control Panel.
- *Post:* The block shows the chosen disposition in the layout.

**User value after this phase:** "I can record my reading progress in an EPUB the same way I do for a PDF."

**Scope:**
- Reuse the existing reading-record API and reader-shell panel flow.
- Support EPUB in the Reading Control Panel with bottom-docked placement (interim — upgraded to content-aware geometry in Phase 8).
- Reuse shared reading-record state management, snap-back rules, and layout presentation where they are format-agnostic.
- Do not yet depend on EPUB `BookContentBlock` direct-content geometry for panel anchoring or auto-mark heuristics.

**Testing:**
- E2E: mark an EPUB block as read and verify the layout shows the updated disposition.
- Extend the same behavior with skimmed/skipped if the phase lands those together.

## Phase 8: EPUB direct-content boundaries and no-direct-content automation (planned)

**Why here:** `BookContentBlock` rows are already persisted from Phase 2 and `hasDirectContent` is already correct on the backend. This phase is purely frontend: resolving those content boundaries in the live DOM for panel geometry and auto-mark behavior.

**Behavior:**
- *Pre:* EPUB is open and the book has blocks with and without direct content.
- *Trigger:* User reads forward through the book, including blocks that have no direct content.
- *Post:* No-direct-content blocks are auto-marked, and the Reading Control Panel uses EPUB-aware content boundaries for anchoring rather than the Phase 7 bottom-docked fallback.

**User value after this phase:** "EPUB reading progress behaves intelligently, including the blocks that are only structure."

**Scope:**

*Frontend/viewer:*
- Resolve EPUB direct-content boundaries in the live DOM: map each `BookBlock`'s content range to elements/ranges using the locator data from Phase 4 and the `BookContentBlock` import data from Phase 2.
- Upgrade the Reading Control Panel from bottom-docked fallback to content-aware geometry where possible (same product rule as PDF: panel anchors below the last direct-content bottom when it's above the obstruction zone).
- Add no-direct-content auto-marking using the shared reading-order rule, fed by EPUB-specific boundary detection.
- Remove the Phase 7 bottom-docked interim behavior where the content-aware path now applies.

**Testing:**
- E2E: representative no-direct-content auto-mark scenario for EPUB.
- Keep this focused on EPUB-specific progress behavior rather than re-testing the whole reading stack.

## Phase 9: Attach an EPUB from the CLI with no preprocessing (planned)

**Why last:** CLI EPUB attach is valuable but lower risk and lower reach than the frontend path. It should reuse the server-side EPUB flow rather than create a second extraction path.

**Behavior:**
- *Pre:* User has selected a notebook in the CLI and has an `.epub` file.
- *Trigger:* User runs `/attach <file.epub>`.
- *Post:* The EPUB is attached successfully, with raw bytes sent to the backend and no CLI-side preprocessing or layout payload.

**User value after this phase:** "I can attach an EPUB from the CLI without Python or MinerU."

**Scope:**
- Extend `/attach` to accept `.epub`.
- Route EPUB attach as raw file upload with `format: "epub"` only.
- Keep PDF CLI behavior unchanged.
- Reuse the same backend **EPUB** attach path already exercised by the frontend upload.

**Testing:**
- CLI test for `.epub` routing and multipart payload shape.
- One representative end-to-end proof from CLI attach to browser-visible result, without duplicating the full frontend EPUB matrix.

## Cross-cutting concerns

### E2E fixture and assertions

- Reuse one small committed `.epub` fixture across phases (happy path). Keep a second fixture for error-path testing (unsupported EPUB).
- Prefer DOM-text assertions for EPUB content instead of OCR — EPUB renders to HTML, not canvas.
- If Readium uses shadow DOM, configure Cypress with `includeShadowDom` or custom commands to pierce the shadow tree.
- Reuse the same layout-side assertion hooks as PDF where the shell behavior is shared.

### OpenAPI and generated types

- Regenerate TypeScript only in the phases that actually change the API surface.
- API-touching phases: 1 (format-aware attach + file serving), 2 (BookBlock structure with EPUB locator data), 4 (locator fields on blocks), 6 (reading-position schema extension for EPUB locator).

### Phase discipline

- Each phase should be implemented and tested as its own completed slice.
- Remove temporary fallback/interim behavior as soon as the replacement phase lands (Phase 3 rough navigation → Phase 4 precise locators; Phase 7 bottom-docked panel → Phase 8 content-aware geometry).
- Keep PDF behavior fully working after every phase.

## Phase summary

| Phase | User value | Primary risk | Size |
|-------|-----------|-------------|------|
| 1 | Upload EPUB and see it attached | API generalization from PDF-only; frontend EPUB attach (PDF attach stays CLI + MinerU) | Medium |
| 2 | Browse EPUB chapter structure | Server-side EPUB parsing, BookBlock + BookContentBlock extraction | Medium |
| 3 | Open EPUB and read content, jump to chapters | Readium integration, rendering, CSP/security, shadow DOM | Medium |
| 4 | Precise navigation to any section | Canonical EPUB locator contract, Readium locator round-trip | Medium |
| 5 | Scroll EPUB and see current block | Viewport-to-block mapping without PDF bbox geometry | Medium |
| 6 | Resume EPUB reading position | Locator persistence, reading-position schema extension | Medium |
| 7 | Mark EPUB blocks read/skimmed/skipped | Reusing reading-record UI and state without over-coupling to PDF | Small-medium |
| 8 | Intelligent EPUB direct-content progress | DOM-based boundary resolution and content-aware panel geometry | Medium |
| 9 | CLI EPUB attach | Raw upload transport only, no second extraction path | Small-medium |
