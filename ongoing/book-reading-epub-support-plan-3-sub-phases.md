# Phase 3 sub-phases: Open the EPUB and read its content

**Parent phase:** Phase 3 of `book-reading-epub-support-plan.md`

**Phase 3 goal:** EPUB content is visible in the main pane in a safe, readable scrolled view. User can click a chapter in the layout and the reader scrolls to that chapter's spine document (interim navigation, upgraded to precise locators in Phase 4).

**Fixture text for E2E assertions (from `epub_valid_minimal.epub`):**
- Part One / chapter1.xhtml → "Opening paragraph for part one."
- Chapter Alpha / chapter2.xhtml → "Body text with an illustration."
- Chapter Beta / chapter3.xhtml → table with "Cell One"

---

## Full E2E scenario (target for this phase)

```gherkin
Scenario: Read EPUB content and navigate to a chapter
  Given I am logged in as an existing user
  And I have a notebook with the head note "EPUB Reading Notebook"
  When I open the notebook settings for "EPUB Reading Notebook"
  And I attach the EPUB file "book_reading/epub_valid_minimal.epub"
  When I open the reading view for the attached book "epub_valid_minimal"
  Then I should see the text "Opening paragraph for part one." in the EPUB reader
  When I click "Chapter Beta" in the book layout
  Then I should see the text "Cell One" in the EPUB reader
```

Steps are enabled incrementally using `@wip` tagging: add the full scenario with `@wip`, comment out later steps, uncomment one at a time, remove `@wip` once all steps pass. At the end of every sub-phase, CI stays green (`@wip` scenarios are skipped in CI).

---

## Sub-phase 3.1 — Behavior (Red): E2E — expect EPUB content text visible on reading page (planned)

**What:** Add the `@wip`-tagged E2E scenario with only the content-visibility assertion enabled. Chapter-navigation steps stay commented out.

**Work:**
- Add the new scenario to `epub_book.feature` tagged `@wip`, with the first `Then` step enabled and the `When I click` / `Then ... "Cell One"` steps commented out.
- Add page object helper (e.g. `expectEpubContentTextVisible(text)`) that asserts the text is visible in the EPUB reader area.
- Add step definition wiring.
- Run E2E locally, confirm failure: placeholder text "EPUB reading view is not available yet." is shown instead of book content.

**Done when:** E2E fails locally for the right reason (no EPUB content rendered). CI stays green because the scenario is `@wip`.

---

## Sub-phase 3.2 — Behavior (Green): Render EPUB content with epub.js on reading page (planned)

**What:** Add epub.js, create `EpubBookViewer.vue`, wire it into the reading page, remove the placeholder. Remove `@wip` from the scenario.

**Work:**
- Install `epub.js` in `frontend/`.
- Create `frontend/src/components/book-reading/EpubBookViewer.vue`:
  - Props: EPUB `ArrayBuffer` bytes and `book` metadata.
  - Renders using epub.js in continuous scrolled mode (`flow: "scrolled"`, `manager: "continuous"`).
  - Scripting disabled (`allowScriptedContent: false`).
  - Container gets `data-testid="epub-book-viewer"`.
- `BookReadingPage.vue`: remove the early return for `epub` format. Fetch EPUB bytes the same way as PDF (same `/api/notebooks/{id}/book/file` endpoint). Pass bytes to `EpubBookViewer` instead of placeholder.
- Delete `BookReadingEpubPlaceholder.vue` (dead code after wiring).
- Update existing E2E scenario "Upload supported EPUB and see book name on reading page": replace the placeholder assertion (`book-reading-epub-placeholder`) with a content or reader assertion that still proves attach succeeded (e.g. assert the EPUB viewer element exists, or assert fixture text is visible).
- Update `bookReadingPage.ts` page object: replace `expectEpubReadingViewShowsBookName` to not depend on the deleted placeholder testid.
- Remove `@wip` tag from the new scenario now that it passes.
- Run E2E, confirm the new scenario passes (fixture text visible).

**Decisions:**
- **epub.js over Readium:** epub.js is simpler to integrate for initial rendering. Readium (or a switch) can be evaluated later if scrolled-mode rendering, performance, or locator fidelity requires it.
- **CSP:** epub.js renders into an iframe with blob URLs. No backend CSP headers exist today. Validate that EPUB content renders without CSP violations in the dev server. If Vite's dev CSP blocks blob/inline styles, configure the dev proxy or relax the policy for the book file path only.
- **Shadow DOM / Cypress:** epub.js uses an iframe, not shadow DOM. E2E assertions need to enter the iframe context (`cy.get('iframe').its('0.contentDocument.body').find(...)` or equivalent). Document this in the step definition.

**Done when:** E2E green — fixture text "Opening paragraph for part one." visible in the EPUB reader. `@wip` removed. No failing tests locally or in CI.

**Stop-safe value:** If delivery stops here, the user can open and read EPUB content in a scrolled view (no chapter navigation yet, but the full text is visible).

---

## Sub-phase 3.3 — Behavior (Red): E2E — click chapter and see its content (planned)

**What:** Uncomment the chapter-click steps in the E2E scenario and re-tag `@wip`.

**Work:**
- Uncomment `When I click "Chapter Beta" in the book layout` and `Then I should see the text "Cell One" in the EPUB reader`.
- Tag the scenario `@wip` again since the newly enabled steps will not pass yet.
- Run E2E locally, confirm failure: clicking a chapter does nothing (block click not wired for EPUB viewer).

**Done when:** E2E fails locally for the right reason (chapter content not scrolled into view after click). CI stays green because the scenario is `@wip`.

---

## Sub-phase 3.4 — Behavior (Green): Wire chapter click navigation via spine href (planned)

**What:** Expose the EPUB spine href in the API and wire block clicks to navigate the epub.js renderer. Remove `@wip`.

**Backend:**
- Add a computed field on `BookBlock` (e.g. `epubStartHref`) that extracts the `href` from the first content block's `rawData` JSON when the owning book's format is EPUB. Return `null` for PDF books. Annotate with `@JsonView(BookViews.Full.class)` so it appears in the book API response.
- Backend test: verify `epubStartHref` is present and correct for EPUB blocks in the controller test.
- This is **interim** — Phase 4 replaces it with the canonical EPUB locator contract.

**Frontend:**
- Regenerate TypeScript types (`pnpm generateTypeScript`).
- Wire `@block-click` in the EPUB reading flow: when a block is clicked, read its `epubStartHref` and use epub.js navigation to display that spine document (e.g. `rendition.display(href)`).
- Remove `@wip` tag from the scenario now that all steps pass.
- E2E passes: clicking "Chapter Beta" scrolls to content containing "Cell One".

**Done when:** E2E green — chapter navigation works at spine-document granularity. `@wip` removed. No failing tests locally or in CI.

---

## Key design notes

- **Interim navigation:** Phase 3 navigation is spine-document level (intentionally imprecise). Phase 4 upgrades to locator-precise navigation and removes the interim `epubStartHref` field.
- **Shared reader shell:** `BookReadingBookLayout` is already shared between PDF and EPUB. The EPUB viewer plugs into the same layout slot and same `@block-click` event pattern.
- **PDF unchanged:** All changes branch on `book.format`. PDF reading path is untouched.
