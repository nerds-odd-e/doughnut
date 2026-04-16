# Phase 4 sub-phases: Navigate precisely to any chapter or section

**Parent phase:** Phase 4 of `book-reading-epub-support-plan.md`

**Phase 4 goal:** When user clicks any BookBlock in the EPUB layout, the reader jumps precisely to that section — including sub-chapter sections within the same spine document. Replaces Phase 3's interim spine-document-level navigation with fragment-precise navigation.

---

## Current state (after Phases 1–3)

- `BookBlock.getEpubStartHref()` returns only the `href` key from the first content block's `rawData`, ignoring `fragment`. All blocks in the same spine document navigate to the same position.
- The extractor stores `fragment` on each content block via `fragmentFor(element)` — the element's own `id` attribute. For the first content block of a nav-defined sub-section, this is typically `""` because the paragraph element doesn't have an `id`; the heading that defines the section does.
- The test fixture `epub_valid_minimal.epub` has 3 BookBlocks (`Part One`, `Chapter Alpha`, `Chapter Beta`), each in a separate spine document. There are no sub-sections within a spine document, so precise vs rough navigation is indistinguishable.
- Frontend passes `block.epubStartHref` to `rendition.display(href)` via epub.js. epub.js supports `href#fragment` natively for precise navigation.

## Approach

1. **Update the test fixture** to include sub-sections within a single spine document so that precise navigation is testable.
2. **Set the nav entry's fragment on the first content block** of each section during extraction — so that `getEpubStartHref()` can read it from the existing rawData structure without a DB migration.
3. **Include fragment in `getEpubStartHref()`** — append `fragment` when non-empty (e.g. `OEBPS/chapter3.xhtml#section-id`).
4. **Frontend needs zero code change** — it already passes `epubStartHref` to `rendition.display()`, and epub.js handles `href#fragment`.

---

## Full E2E scenario (target for this phase)

```gherkin
Scenario: Navigate precisely to a sub-section within an EPUB chapter
  Given I am logged in as an existing user
  And I have a notebook with the head note "EPUB Precise Nav Notebook"
  When I open the notebook settings for "EPUB Precise Nav Notebook"
  And I attach the EPUB file "book_reading/epub_valid_minimal.epub"
  When I open the reading view for the attached book "epub_valid_minimal"
  When I click "Section Beta-Two" in the book layout
  Then I should see the text "Unique content in section beta-two." in the EPUB reader
```

The key assertion: clicking a sub-section block navigates to that specific section's content, not the spine document start. "Section Beta-Two" is a sub-section within the same spine document as "Chapter Beta", distinguished only by its fragment anchor.

---

## Sub-phase 4.1 — Structure: Enhance EPUB test fixture with sub-sections in a single spine document

**What:** The current fixture has all BookBlocks in separate spine documents, so there is no case where fragment-level precision matters. Add sub-sections under Chapter Beta in `chapter3.xhtml` with distinct heading IDs and body text.

**Work:**
- Modify `epub_valid_minimal.epub`:
  - In `chapter3.xhtml`, add two sub-sections after the existing table: `<h2 id="section-beta-one">Section Beta-One</h2>` with body text "Unique content in section beta-one." and `<h2 id="section-beta-two">Section Beta-Two</h2>` with body text "Unique content in section beta-two."
  - In the nav document (`toc.xhtml` or `nav.xhtml`), add nested `<li>` entries under Chapter Beta pointing to `chapter3.xhtml#section-beta-one` and `chapter3.xhtml#section-beta-two`.
- Update backend test assertions in `NotebookBooksControllerTest`:
  - Expect 5 BookBlocks instead of 3 (Part One, Chapter Alpha, Chapter Beta, Section Beta-One, Section Beta-Two).
  - Verify the new blocks have appropriate depth, titles, and content blocks.
- Update the E2E layout assertion in "Upload supported EPUB and see book name on reading page":
  - Add the two new sub-section rows to the expected layout table.
- Run backend tests and E2E. All pass — no behavior change, only fixture enrichment.

**Done when:** All existing tests pass with the updated fixture. The layout now shows 5 blocks.

---

## Sub-phase 4.2 — Behavior (Red): E2E scenario for precise sub-section navigation

**What:** Add a `@wip` scenario to `epub_book.feature` that clicks a sub-section and asserts its specific content is visible.

**Work:**
- Add the scenario shown above, tagged `@wip`.
- Add step definitions if not already covered by existing steps (the existing `When I click "..." in the book layout` and `Then I should see the text "..." in the EPUB reader` steps should work).
- Run E2E locally. Confirm failure: `epubStartHref` returns just `OEBPS/chapter3.xhtml` for all blocks within that spine document, so clicking "Section Beta-Two" navigates to the start of chapter3.xhtml, not to the sub-section. The assertion for "Unique content in section beta-two." fails because the reader shows the start of the chapter (or the text is scrolled out of view).

**Done when:** E2E fails locally for the right reason (not navigating to the sub-section). CI stays green (`@wip`).

---

## Sub-phase 4.3 — Behavior (Green): Precise navigation via section fragment in epubStartHref

**What:** Two small changes make precise navigation work end-to-end:

1. **Extraction:** In `EpubStructureExtractor.extractContentForSpineFile`, after content is assigned to sections, set the first content block's `fragment` to the nav entry's `fragmentId` (with `#` prefix) when present. This ensures `getEpubStartHref()` can read the section's start anchor from the existing rawData structure. Only the first content block is modified; later content blocks keep their element IDs.

2. **Getter:** In `BookBlock.getEpubStartHref()`, read the `fragment` field from rawData and append it to `href` when non-empty (e.g. `OEBPS/chapter3.xhtml` + `#section-beta-two` → `OEBPS/chapter3.xhtml#section-beta-two`). For blocks with `fragment=""` (top-level sections at spine doc start), behavior is unchanged.

**Frontend:** No change needed. `rendition.display("OEBPS/chapter3.xhtml#section-beta-two")` navigates precisely via epub.js.

**Work:**
- Modify `EpubStructureExtractor`: after populating `perBlock` for each spine file, iterate over sections — for any section whose nav row has a non-empty `fragmentId`, overwrite the first content block's `"fragment"` value with `"#" + fragmentId`.
- Modify `BookBlock.getEpubStartHref()`: read `fragment` from the first content block's rawData; if non-empty, append it to `href`.
- Update `@Schema` description: remove "Interim" / "rough" wording; describe as the EPUB block-start locator for layout navigation.
- Update backend test assertions: sub-section blocks now have `epubStartHref` values that include fragments (e.g. `OEBPS/chapter3.xhtml#section-beta-one`).
- Remove `@wip` from the E2E scenario.
- Run all backend tests and E2E. All pass.

**Optional cleanup:** Rename `displaySpineHref` → `displayEpubTarget` in `EpubBookViewer.vue` and `BookReadingEpubView.vue` for naming accuracy (no longer spine-level only). Small rename, no behavior change.

**Done when:** E2E green — clicking "Section Beta-Two" navigates precisely and shows "Unique content in section beta-two." `@wip` removed. No failing tests. Interim labeling cleaned up.

---

## Key design notes

- **No DB migration needed.** The nav entry's fragment is stored in the existing `rawData` JSON of the first content block. `getEpubStartHref()` reads it from there.
- **`epubStartHref` remains a string field.** It evolves from `href`-only to `href#fragment` — a compatible change. No TypeScript regeneration needed (still `string | undefined`).
- **epub.js handles `href#fragment` natively.** `rendition.display("path/file.xhtml#id")` scrolls to the element with that ID.
- **PDF path unchanged.** All changes branch on `book.format`. PDF reading is untouched.
- **Shared reader shell.** The same `@block-click` → `displaySpineHref` → `rendition.display()` path is used; only the data flowing through it becomes more precise.
