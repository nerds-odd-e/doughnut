# EPUB support — phased delivery plan

**Goal:** Deliver EPUB support as a sequence of small, user-visible slices that reuse the existing PDF book-reading architecture where the domain is shared, while keeping EPUB-specific behavior in tightly scoped modules and concrete `book.format` branches.

**Companion docs:** [epub-research-quest.md](book-reading-epub-research-quest.md), [architecture-roadmap.md](doughnut-book-reading-architecture-roadmap.md), [ux-ui-roadmap.md](book-reading-ux-ui-roadmap.md), [user-stories.md](book-reading-user-stories.md).

## Delivery principles

- **Risk-first:** Burn down the highest product and architecture risks early, not after a large amount of EPUB-only code has accumulated.
- **One behavior per phase:** Every phase must let the user do one more meaningful thing end to end.
- **No wasted work:** If delivery stops after any phase, the shipped result is still coherent and worth keeping.
- **High cohesion:** Reuse shared book-reading concepts and modules from the PDF path where the behavior is the same: attach orchestration, reader shell, block selection/current-block state, reading-position persistence, reading-record persistence.
- **Low coupling:** Avoid abstract interface layers unless they are clearly paying for themselves. Prefer format-aware branching at the boundary modules and keep PDF/EPUB viewer internals separate.
- **Shared domain vocabulary:** **BookBlock** and **BookContentBlock** remain the core domain concepts for both formats. EPUB should fit those concepts rather than introducing a second tree or second progress model.
- **Primary upload surface:** Frontend upload is the primary EPUB path. CLI upload comes later and sends raw `.epub` bytes only, with no preprocessing and no layout/content payload.
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

## Phase 1: Upload a supported EPUB and see its structure (planned)

**Why first:** This is the highest-risk backend slice and the first user-visible value. It proves we can parse the EPUB package on the server, persist the book, and map it into the existing `BookBlock` concept.

**Behavior:**
- *Pre:* Notebook exists, has no attached book, user has a supported `.epub` file.
- *Trigger:* User uploads the `.epub` in the notebook page.
- *Post:* The notebook shows the attached EPUB, the reading page opens, and the layout sidebar shows the book structure. Unsupported EPUBs fail with a clear message instead of partial import.

**User value after this phase:** "I can upload a supported EPUB and browse its chapter structure in Doughnut."

**Scope:**

*Backend:*
- Generalize attach orchestration from PDF-only to format-aware attach.
- Accept `format: "epub"` with raw file upload and no client-provided `layout` or `contentList`.
- Add server-side EPUB structure extraction: unzip, parse `container.xml`, OPF, manifest, spine, nav/heading fallback, and build the `BookBlock` tree.
- Preserve shared `BookBlock` semantics, including synthetic `*beginning*` when content appears before the first structural heading.
- Detect unsupported/invalid EPUB early and return clear user-visible errors.
- Serve the raw `.epub` file with `application/epub+zip`.

*Frontend:*
- Allow `.epub` in the existing attach flow.
- Make the attach UI and copy format-aware instead of PDF-specific.
- Let the reading page load an EPUB book without crashing and show the structure drawer plus a temporary main-pane placeholder.
- Verify book deletion works for EPUB through the same notebook/book flow.

*Fixture:*
- Commit one small representative `.epub` fixture with a TOC or headings, a few chapters, paragraphs, and at least one image.

**Testing:**
- Backend controller test: attach EPUB and verify stored book format, file serving, and `BookBlock` structure.
- Focused unit tests for EPUB structure extraction edge cases and fail-fast validation.
- E2E: upload EPUB in the frontend, open the reading page, and see the structure in the layout sidebar.

## Phase 2: Open the EPUB and read its content (planned)

**Why now:** This burns down the renderer and security risk without coupling it to navigation behavior. If this phase ships and work stops, users still get meaningful value: the EPUB is attached, visible, and readable in Doughnut.

**Behavior:**
- *Pre:* EPUB book is attached to the notebook.
- *Trigger:* User opens the reading page.
- *Post:* EPUB content is visible in the main pane in a safe, readable scrolled view alongside the structure drawer.

**User value after this phase:** "I can open and read my EPUB in Doughnut."

**Scope:**

*Frontend:*
- Add `EpubBookViewer.vue` using Readium in scrolled mode.
- Replace the Phase 1 placeholder with real EPUB rendering.
- Keep shared reader-shell logic together: book/file loading, file fetch, drawer behavior, and format-aware viewer selection.

*Security and ops:*
- Validate CSP behavior required for EPUB rendering.
- Confirm user-uploaded EPUB scripting stays disabled.

**Testing:**
- E2E: open the EPUB reading page and assert known fixture text is visible in the main pane.
- Focused frontend test for viewer mount/unmount lifecycle.

## Phase 3: Click a chapter and jump to that place in the EPUB (planned)

**Why here:** Navigation is valuable on its own and should not be bundled with basic rendering. This phase is also where the canonical EPUB navigation contract should be fixed, before more EPUB reader behavior accumulates.

**Behavior:**
- *Pre:* EPUB is open in the reader and the structure drawer is visible.
- *Trigger:* User clicks a `BookBlock` in the layout.
- *Post:* The reader jumps to the corresponding chapter or section in the EPUB.

**User value after this phase:** "I can use the structure to jump to where I want in the EPUB."

**Scope:**

*Contract and architecture:*
- Adopt the EPUB block-start contract here: expose an EPUB-native block locator aligned with Readium `Locator` semantics.
- Keep PDF navigation data unchanged and branch on `book.format` at the reader boundary.

*Backend:*
- Persist the minimal EPUB locator data needed for layout-to-content navigation.
- Extend `GET …/book` to expose that locator data for EPUB blocks.

*Frontend:*
- Wire `BookReadingContent.vue` so clicking a block uses the EPUB locator to navigate.
- Reuse the existing selected-block flow rather than inventing an EPUB-specific selection model.

**Testing:**
- E2E: click a chapter in the EPUB layout and verify the target section becomes visible.

## Phase 4: Scroll the EPUB and see where I am in the layout (planned)

**Why here:** This completes the core structure-to-reader sync after basic rendering and explicit navigation already work.

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

## Phase 5: Leave and return to the same EPUB position (planned)

**Why here:** Once basic reading and navigation work, resume becomes a clean, valuable standalone slice with moderate technical risk.

**Behavior:**
- *Pre:* User has opened an EPUB and scrolled to a meaningful position.
- *Trigger:* User leaves the reading page and later opens the same book again.
- *Post:* The reader returns to the saved EPUB position and restores the selected block when applicable.

**User value after this phase:** "I can continue my EPUB where I left off."

**Scope:**
- Extend reading-position persistence with an EPUB-specific locator payload while keeping the existing PDF shape intact.
- Reuse the same `GET/PATCH …/reading-position` user-facing flow.
- Debounce EPUB position updates from reader relocation events.
- Restore from the saved locator on load.

**Testing:**
- Backend test for EPUB reading-position round-trip.
- E2E: navigate to a known EPUB section, leave, return, and verify the same section is shown again.

## Phase 6: Mark an EPUB block as read, skimmed, or skipped (planned)

**Why separate from direct-content automation:** Manual progress recording is meaningful on its own. It should not wait for the harder EPUB-specific direct-content geometry work.

**Behavior:**
- *Pre:* EPUB is open and a block is selected.
- *Trigger:* User marks the selected block as read, skimmed, or skipped in the Reading Control Panel.
- *Post:* The block shows the chosen disposition in the layout.

**User value after this phase:** "I can record my reading progress in an EPUB the same way I do for a PDF."

**Scope:**
- Reuse the existing reading-record API and reader-shell panel flow.
- Support EPUB in the Reading Control Panel with a simple, safe placement strategy first, such as bottom-docked behavior.
- Reuse shared reading-record state management, snap-back rules, and layout presentation where they are format-agnostic.
- Do not yet depend on EPUB `BookContentBlock` direct-content geometry for panel anchoring or auto-mark heuristics.

**Testing:**
- E2E: mark an EPUB block as read and verify the layout shows the updated disposition.
- Extend the same behavior with skimmed/skipped if the phase lands those together.

## Phase 7: EPUB direct content and no-direct-content automation (planned)

**Why later:** This is important shared-domain work, but it is not necessary to deliver the earlier reading slices. Shipping it later avoids overloading Phase 1 and keeps the workload more even.

**Behavior:**
- *Pre:* EPUB is open and the book has blocks with and without direct content.
- *Trigger:* User reads forward through the book, including blocks that have no direct content.
- *Post:* No-direct-content blocks are auto-marked, and the Reading Control Panel can use EPUB-aware content boundaries rather than a permanent fallback placement.

**User value after this phase:** "EPUB reading progress behaves intelligently, including the blocks that are only structure."

**Scope:**

*Backend/domain:*
- Reuse the existing `BookContentBlock` persistence concept for EPUB instead of introducing a parallel import model.
- Add EPUB content-stream extraction for the types needed by direct-content logic, with EPUB-specific data in `rawData`.
- Extend `BookBlockDirectContentPredicate` for EPUB-specific included and excluded content types while preserving the shared domain rule.

*Frontend/viewer:*
- Resolve EPUB direct-content boundaries in the live DOM.
- Upgrade the panel from simple fallback placement to content-aware geometry where possible.
- Add no-direct-content auto-marking using the shared reading-order rule, fed by EPUB-specific boundary detection.

**Testing:**
- Backend tests for EPUB `BookContentBlock` extraction and direct-content classification.
- E2E: representative no-direct-content scenario for EPUB.
- Keep this focused on EPUB-specific progress behavior rather than re-testing the whole reading stack.

## Phase 8: Attach an EPUB from the CLI with no preprocessing (planned)

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
- Reuse the same backend attach flow already exercised by the frontend.

**Testing:**
- CLI test for `.epub` routing and multipart payload shape.
- One representative end-to-end proof from CLI attach to browser-visible result, without duplicating the full frontend EPUB matrix.

## Cross-cutting concerns

### E2E fixture and assertions

- Reuse one small committed `.epub` fixture across phases.
- Prefer DOM-text assertions for EPUB content instead of OCR.
- Reuse the same layout-side assertion hooks as PDF where the shell behavior is shared.

### OpenAPI and generated types

- Regenerate TypeScript only in the phases that actually change the API surface.
- Likely API-touching phases: 1, 2, and 4; phase 6 only if EPUB direct-content data needs new wire fields.

### Phase discipline

- Each phase should be implemented and tested as its own completed slice.
- Remove temporary fallback behavior as soon as the replacement phase lands.
- Keep PDF behavior fully working after every phase.

## Phase summary

| Phase | User value | Primary risk | Size |
|-------|-----------|-------------|------|
| 1 | Upload EPUB and see structure | Server-side EPUB parsing and clear support boundary | Medium-large |
| 2 | Open EPUB and read content | Readium integration, rendering, and safety model | Medium |
| 3 | Jump from layout to EPUB section | Canonical EPUB locator contract | Medium |
| 4 | Scroll EPUB and see current block | Viewport-to-block mapping without PDF bbox geometry | Medium |
| 5 | Resume EPUB reading position | Locator persistence and restore flow | Medium |
| 6 | Mark EPUB blocks read/skimmed/skipped | Reusing reading-record UI and state without over-coupling to PDF | Medium |
| 7 | Intelligent EPUB direct-content progress | `BookContentBlock` extraction and DOM-based boundary/geometry logic | Medium-large |
| 8 | CLI EPUB attach | Raw upload transport only, no second extraction path | Small-medium |
