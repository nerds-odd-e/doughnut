# Frontend unit test optimization

Status: done

## Profiling baseline (2026-06-03)

Command: `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run --reporter=json`

- **1369 tests**, full suite wall ~57s.
- Grouping: top 10% = **137 tests**; per-file = 32 groups, per-10 = **14 groups** → use **14 batch groups** (smaller count).

### Top 10 slowest tests (by Vitest `duration`)

| # | ms | file | test |
|---|-----|------|------|
| 1 | 153 | MessageCenterPage.spec.ts | should highlight the selected conversation |
| 2 | 125 | TextContentWrapper.spec.ts | does not discard when focusout has misleading relatedTarget… |
| 3 | 119 | NoteAudioTools.spec.ts | displays error message in fullscreen overlay when errors exist |
| 4 | 117 | NotebookPage.spec.ts | saves notebook index content directly to container on save |
| 5 | 93 | MessageCenterPage.spec.ts | should navigate when conversation clicked |
| 6 | 81 | HorizontalMenu.spec.ts | collapses menu when route changes |
| 7 | 77 | HorizontalMenu.spec.ts | collapses menu when losing focus |
| 8 | 65 | PopButton.spec.ts | blurs button when dialog closes via close_request |
| 9 | 61 | HorizontalMenu.spec.ts | expands menu when expand button is clicked |
| 10 | 59 | BookReadingPage.spec.ts | updates current block while book layout drawer is closed |

**Dominant file in top 10%:** `BookReadingPage.spec.ts` (52 of 137 slow tests).

## Optimization rules

1. Remove or simplify redundant tests first.
2. Strictly no fixed-time waits (`sleep`, arbitrary `setTimeout`, debounce-timeout waits).
3. Flaky = failure; tests must be deterministic.
4. Prefer `getByText` / `getByLabelText` / `querySelector` over role queries (project convention).

---

### Phase 1: Slowest batch — MessageCenter, TextContent, NoteAudio, Notebook, HorizontalMenu, PopButton, BookReading (1 test)
Status: done

**Result (2026-06-03):** Replaced slow `getByRole`/`vi.waitUntil`/rAF waits with `data-testid` queries, `nextTick`/`flushPromises`, and fake timers for debounce/focus handlers. Notebook save uses paste instead of per-key typing. **126 tests** in touched files unchanged; full suite **1369** pass.

**Scope:** `tests/pages/MessageCenterPage.spec.ts`, `tests/notes/TextContentWrapper.spec.ts`, `tests/notes/NoteAudioTools.spec.ts`, `tests/pages/NotebookPage.spec.ts`, `tests/toolbars/HorizontalMenu.spec.ts`, `tests/commons/Popups/PopButton.spec.ts`, `tests/pages/BookReadingPage.spec.ts`

**Tests (10, slowest first):**
- `should highlight the selected conversation` (153ms)
- `does not discard when focusout has a misleading relatedTarget but focus remains inside the wrapper` (125ms)
- `displays error message in fullscreen overlay when errors exist` (119ms)
- `saves notebook index content directly to container on save` (117ms)
- `should navigate when conversation clicked` (93ms)
- `collapses menu when route changes` (81ms)
- `collapses menu when losing focus` (77ms)
- `blurs button when dialog closes via close_request` (65ms)
- `expands menu when expand button is clicked` (61ms)
- `updates current block while book layout drawer is closed` (59ms)

**Verify:** run scoped files then full `CURSOR_DEV=true nix develop -c pnpm frontend:test`

---

### Phase 2: BookReading snap/menu batch
Status: done

**Result (2026-06-03):** Removed redundant HorizontalMenu expand-button-when-expanded test. Replaced `vi.waitFor`/`vi.waitUntil` with sync selection checks, `flushPromises`, and `requestAnimationFrame` for focus. Split RichMarkdownEditor preset-key test into lean `it.each` cases. **134** scoped tests; full suite **1369** pass (~19s wall).

**Scope:** `tests/pages/BookReadingPage.spec.ts`, `tests/toolbars/HorizontalMenu.spec.ts`, `tests/components/form/RichMarkdownEditor.properties.spec.ts`, `tests/toolbars/GlobalBar.spec.ts`, `tests/notes/NoteNewForm.spec.ts`, `tests/notes/QuestionExportDialog.spec.ts`

**Tests (10):** different unread blocks snap budgets; expand button when expanded (removed as redundant); preset key insert/existing row; GlobalBar touch search focus; READ clears snap; snap second/third crossing; successor panel selection; NoteNewForm append title; QuestionExportDialog fetch; successor block after Read.

**Verify:** scoped files then full suite.

---

### Phase 3: BookReading panel/sidebar batch
Status: done

**Result (2026-06-03):** Removed redundant page-level layout depth test (covered in `BookReadingBookLayout.spec.ts`). Replaced `vi.waitFor`/`vi.waitUntil` with sync selection checks, `flushPromises`, and `mountSidebarFirstGenReady`. **70** scoped tests; full suite **1369** pass (~21s wall).

**Scope:** `tests/pages/BookReadingPage.spec.ts`, `tests/notes/sidebar/SidebarActiveFolder.spec.ts`

**Tests (10):** debounces PATCH; snap fourth crossing; API depth (moved to layout spec); panel unmount after Read; PUT SKIMMED; sidebar folder activate/navigate; Read from here; PATCH selectedBookBlockId; successor panel visibility.

**Verify:** scoped files then full suite.

---

### Phase 4: BookReading snap geometry batch
Status: done

**Result (2026-06-03):** Added `mountFirstBlockBboxScenario` helper; replaced `vi.waitFor`/`vi.waitUntil` with `clickBookBlockAndExpectSelection`, sync selection checks, and `flushPromises`. Zoom test uses plain book mount (no blocks). Questions table test drops body attach + polling. **54** scoped tests; full suite **1369** pass (~22s wall).

**Scope:** `tests/pages/BookReadingPage.spec.ts`, `tests/notes/Questions.spec.ts`

---

### Phase 5: BookReading panel visibility batch
Status: done

**Result (2026-06-03):** Added `mountFirstBlockBboxScenario`, `mountCrossPageBboxScenario`, and `clickBookBlockStartingWithAndExpectSelection`. Replaced `vi.waitFor` selection polls with sync `clickBookBlockAndExpectSelection` / `emitViewportAndSettleCurrentBlock`. MessageCenter default state uses `flushPromises` + `textContent` instead of `expect.element`. **56** scoped tests; full suite **1369** pass (~21s wall).

**Scope:** `tests/pages/BookReadingPage.spec.ts`, `tests/pages/MessageCenterPage.spec.ts`

**Tests (10):** anchors panel; keeps panel visible after geometry false; last-block panel visible; no snap when disposition recorded; cross-page bbox snap; nav bar when current ≠ selected; no PATCH when viewport null; clears snap-animating on animationend; MessageCenter no selection default; PATCH uses last viewport top in debounce window.

**Verify:** scoped files then full suite.

---

### Phase 6: BookReading bootstrap/modal batch
Status: done

**Result (2026-06-03):** `waitForPdfViewer` tries `flushPromises` before polling; PDF loading test waits on viewer mount instead of spinner polling. Replaced selection/nav-bar `vi.waitFor` with `clickBookBlockAndExpectSelection` / `expectCurrentSelection`. Modal autofocus uses `flushPromises` + dialog + `requestAnimationFrame` instead of `activeElement` polling. Wikidata flow uses real timers and leaner dialog helpers. Bootstrap keeps short `vi.waitFor` only where async bootstrap is still in flight. **83** scoped tests; full suite **1369** pass (~20s wall).

**Scope:** `tests/pages/BookReadingPage.spec.ts`, `tests/notes/NoteNewForm.spec.ts`, `tests/commons/Modal.spec.ts`, `tests/composables/useBookReadingBootstrap.spec.ts`, `tests/notes/QuestionExportDialog.spec.ts`

**Tests (10):** shows panel when last content bottom visible; NoteNewForm search replace; PDF loading indicator; hides nav bar; Modal autofocus; useBookReadingBootstrap pdf; QuestionExportDialog API fail; PDF invalid error; hides panel obstruction; data-snap-animating on snap.

**Verify:** scoped files then full suite.

---

### Phase 7: BookReading load/restore batch
Status: done

**Result (2026-06-03):** Removed redundant default-selection and standalone PDF-load tests (credentials merged into loading-indicator test). Book layout toggle uses plain PDF mount without blocks. Added `mountNoDirectContentBboxScenario`; PathNameEditor autofocus uses `flushPromises` + `requestAnimationFrame`; bootstrap tests try sync read before short `vi.waitFor`. **61** scoped tests; full suite **1368** pass (~22s wall).

**Scope:** `tests/pages/BookReadingPage.spec.ts`, `tests/components/notes/core/PathNameEditor.spec.ts`, `tests/composables/useBookReadingBootstrap.spec.ts`

**Tests (10):** batch 7 from profiling.

**Verify:** scoped files then full suite.

---

### Phase 8: BookReading restore/format batch
Status: done

**Result (2026-06-03):** Added `waitForEpubViewer` and `stubReadingPositionSnapshot` helpers; merged read/skimmed border tests into `it.each`; replaced HorizontalMenu `vi.waitUntil` with sync `nextTick` toggles; converted NoteAddQuestion to `it.each` with `getByLabelText`; split RichMarkdownEditor preset-key visibility into lean `it.each` cases; leaner Wikidata matching-label flow in NoteNewForm. **128** scoped tests; full suite **1370** pass (~23s wall).

**Scope:** `tests/pages/BookReadingPage.spec.ts`, `tests/toolbars/HorizontalMenu.spec.ts`, `tests/notes/NoteAddQuestion.spec.ts`, `tests/notes/NoteNewForm.spec.ts`, `tests/components/form/RichMarkdownEditor.properties.spec.ts`

**Tests (10):** restores selected book block; restores reading position; skimmed border on load; loads EPUB; no restore without snapshot; read border on load; HorizontalMenu collapse expand again; NoteAddQuestion only allow generation; NoteNewForm search dog undefined; RichMarkdownEditor preset keys on focus.

**Verify:** scoped files then full suite.

---

### Phase 9: NoteNewForm / RichMarkdownEditor batch
Status: done

**Result (2026-06-03):** Replaced `waitUntilFocused` polling with single rAF + sync assert; HorizontalMenu drops `vi.waitUntil`/`expect.element` for sync `querySelector`/`nextTick`; NoteAddQuestion uses direct input instead of `userEvent.type`; removed redundant NoteEditableContent multi-scenario normalization test; leaner Wikidata dropdown helpers and relation `it.each`; split RichMarkdownEditor paste readonly case. **105** scoped tests; full suite **1369** pass (~23s wall).

**Scope:** `tests/notes/NoteNewForm.spec.ts`, `tests/notes/NoteEditableContent.spec.ts`, `tests/components/form/RichMarkdownEditor.properties.spec.ts`, `tests/toolbars/HorizontalMenu.spec.ts`, `tests/components/form/RichMarkdownEditor.spec.ts`, `tests/notes/NoteAddQuestion.spec.ts`

**Tests (10):** batch 9 from profiling.

**Verify:** scoped files then full suite.

---

### Phase 10: RichMarkdownEditor properties / toolbar batch
Status: done

**Result (2026-06-03):** Replaced HorizontalMenu `expect.element`/`vi.waitUntil` with sync `ariaLabelEl`/`querySelector` and `nextTick`. Split relation button label into `it.each`; trimmed redundant `flushPromises`. NoteToolbar export uses wide inline toolbar. NoteRefinement loading modal shares deferred-API helper. Quiz ContentLoader uses tracker-scoped pending retry mock. `waitUntilFocused` uses rAF instead of `vi.waitUntil`. **94** scoped tests; full suite **1370** pass (~22s wall).

**Scope:** `tests/components/form/RichMarkdownEditor.properties.spec.ts`, `tests/notes/NoteToolbar.moreOptions.spec.ts`, `tests/components/recall/NoteRefinement.removeSuggestions.spec.ts`, `tests/toolbars/HorizontalMenu.spec.ts`, `tests/notes/NoteNewForm.spec.ts`, `tests/recall/Quiz.spec.ts`

**Tests (10):** relation button label; copy export markdown; LoadingModal removing suggestions; hides menu icon; LoadingModal API fail; collapses outside click; NoteNewForm folderId; unknown relation dialog; Quiz ContentLoader; remove all property rows.

**Verify:** scoped files then full suite.

---

### Phase 11: FolderPage / SearchDialog / InsertWikiLink batch
Status: done

**Result (2026-06-03):** Added `searchDebounceTestSupport` with exact debounce advance. Merged duplicate FolderPage merge-conflict tests into `it.each`; dropped `attachTo: document.body` where popups do not need it. Replaced `findByRole`/`findByPlaceholderText`/`vi.waitUntil` with `getByText`/`getByTitle`/`getByPlaceholderText` and sync assertions in SearchDialog and InsertWikiLink. RichMarkdownEditor harness uses `attachToBody` only for focus/preset/paste paths; readonly Properties uses scoped `h4`/`dl` queries; property-key focus uses sync `activeElement` after one rAF. **62** scoped tests; full suite **1370** pass (~21s wall).

**Scope:** `tests/pages/FolderPage.spec.ts`, `tests/links/InsertWikiLink.spec.ts`, `tests/links/SearchDialog.spec.ts`, `tests/components/form/RichMarkdownEditor.properties.spec.ts`

**Tests (10):** FolderPage merge confirm 409; InsertWikiLink wiki property; SearchDialog moveNoteToFolder; moveNoteToNotebookRoot; read-only Properties YAML; preset dropdown excludes keys; focus property key after insert; rewrite dead link; soft-deleted move confirm; FolderPage move 409 cancel.

**Verify:** scoped files then full suite.

---

### Phase 12: NoteEditable / refinement / failure batch
Status: done

**Result (2026-06-03):** Added `mountMarkdownTextarea`/`setTextareaValue` helpers; dropped `attachTo: document.body` where unnecessary. QuestionDisplay mask tests use `mountActiveQuestion` + `nextTick` instead of extra `flushPromises`. SeamlessTextEditor paste uses caret/paste helpers without focus churn. FailureReportList gets `data-testid` selectors and shared mount/delete-modal helpers. NoteNewForm folder submits use a dedicated nested describe (no mid-test remount). RichMarkdownEditor compose/pasteComplete tests drop redundant flushes after mount/emit. **121** scoped tests; full suite **1370** pass (~20s wall).

**Scope:** `tests/notes/NoteEditableContent.spec.ts`, `tests/components/recall/QuestionDisplay.spec.ts`, `tests/components/recall/NoteRefinement.removeSuggestions.spec.ts`, `tests/components/form/SeamlessTextEditor.spec.ts`, `tests/components/recall/NoteRefinement.extractNote.spec.ts`, `tests/notes/NoteNewForm.spec.ts`, `tests/components/admin/FailureReportList.spec.ts`, `tests/components/form/RichMarkdownEditor.properties.spec.ts`

**Tests (10):** preserve unsaved edits; QuestionDisplay focus mask; removeSuggestions API confirm; SeamlessTextEditor paste; extractNote API fail; NoteNewForm pre-selected folder; FailureReportList cancel; delete reports; compose body frontmatter; preserve second edit race.

**Verify:** scoped files then full suite.

---

### Phase 13: Wikidata / assimilation batch
Status: done

**Result (2026-06-03):** Shared Wikidata DOM click helpers; NoteNewForm wikidata/cancel/semantic paths use modal queries instead of `findComponent`/vm; trimmed redundant `flushPromises` on property remove and removal cancel; AssimilationPanel spelling-close helper; FolderPage move uses `nextTick` after parent select. **105** scoped tests; full suite **1370** pass (~24s wall).

**Scope:** `tests/components/form/RichMarkdownEditor.properties.spec.ts`, `tests/components/recall/NoteRefinement.removeSuggestions.spec.ts`, `tests/notes/WikidataAssociationDialog.spec.ts`, `tests/components/recall/AssimilationPanel.spec.ts`, `tests/notes/NoteNewForm.spec.ts`, `tests/pages/FolderPage.spec.ts`

**Tests (10):** remove property row; removeSuggestions cancel; Wikidata save from list; AssimilationPanel close popup; pasteComplete frontmatter; semantic search toggle; Wikidata save empty clear; NoteNewForm cancel dialog; FolderPage re-enable organize; Wikidata append action.

**Verify:** scoped files then full suite.

---

### Phase 14: Final slow batch
Status: done

**Result (2026-06-03):** Added `folderPageTestSupport` and `noteContentDebounceTestSupport` (fake-timer debounce advance). Consolidated FolderPage move/dissolve describes; RichMarkdownEditor dead-link/linkify tests drop redundant flushes; NoteEditableContent debounce/normalization use shared mount helpers without `attachTo: document.body`; PdfBookViewer pinch asserts scale synchronously. **74** scoped tests; full suite **1370** pass.

**Scope:** `tests/pages/FolderPage.spec.ts`, `tests/components/form/RichMarkdownEditor.spec.ts`, `tests/notes/NoteEditableContent.spec.ts`, `tests/components/book-reading/PdfBookViewer.gestureZoom.spec.ts`, `tests/components/form/RichMarkdownEditor.properties.spec.ts`

**Tests (7):** FolderPage soft-deleted inline error; RichMarkdownEditor dead link echo; NoteEditableContent auto-save debounce; RichMarkdownEditor linkify wikilinks; PdfBookViewer pinch zoom; FolderPage merge navigate; RichMarkdownEditor image upload path.

**Verify:** scoped files then full suite.

---

### Phase 15: Re-profile and close
Status: done

**After (2026-06-03):** `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run --reporter=json`

| Metric | Before | After |
|--------|--------|-------|
| Test count | 1369 | **1370** (+1 from `it.each` splits) |
| Full suite wall | ~57s | **~52s** |
| Top 10 slowest total CPU | 949ms | **597ms** (−37%) |
| Top 10% (137 tests) total CPU | 5911ms | **4443ms** (−25%) |

#### Top 10 slowest (post-optimization)

| # | ms | file | test |
|---|-----|------|------|
| 1 | 81 | HorizontalMenu.spec.ts | expands menu when clicking menu icon |
| 2 | 66 | BookReadingPage.spec.ts | updates current block while book layout drawer is closed |
| 3 | 62 | PopButton.spec.ts | blurs button when dialog closes via close_request |
| 4–12 | 54–57 | BookReadingPage.spec.ts | reading control panel / PATCH / geometry tests |

**Dominant file in top 10% after:** `BookReadingPage.spec.ts` (49 of 137, down from 52).

#### Summary

- Removed or merged redundant tests (HorizontalMenu expand-when-expanded, BookReading default-selection/PDF-load duplicates, NoteEditableContent multi-scenario normalization, etc.).
- Replaced `getByRole`/`findByRole`, `vi.waitUntil`, and long `vi.waitFor` polls with `data-testid`, sync DOM queries, `flushPromises`/`nextTick`, and fake timers for debounce/snap-hold.
- Added shared helpers: BookReading selection/viewport, sidebar mount, search debounce, folder page, note content debounce, Wikidata dialog clicks.

**Commits:** `0e82333346` (batch 1) through `ae58276268` (batch 14); plan close in this commit.
