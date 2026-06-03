# Frontend unit test optimization

Status: in-progress

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
Status: planned

**Scope:** `tests/pages/BookReadingPage.spec.ts`, `tests/notes/Questions.spec.ts`

**Tests (10):** batch 4 from profiling.

**Verify:** scoped files then full suite.

---

### Phase 5: BookReading panel visibility batch
Status: planned

**Scope:** `tests/pages/BookReadingPage.spec.ts`, `tests/pages/MessageCenterPage.spec.ts`

**Tests (10):** batch 5 from profiling.

**Verify:** scoped files then full suite.

---

### Phase 6: BookReading bootstrap/modal batch
Status: planned

**Scope:** `tests/pages/BookReadingPage.spec.ts`, `tests/notes/NoteNewForm.spec.ts`, `tests/commons/Modal.spec.ts`, `tests/composables/useBookReadingBootstrap.spec.ts`, `tests/notes/QuestionExportDialog.spec.ts`

**Tests (10):** batch 6 from profiling.

**Verify:** scoped files then full suite.

---

### Phase 7: BookReading load/restore batch
Status: planned

**Scope:** `tests/pages/BookReadingPage.spec.ts`, `tests/components/notes/core/PathNameEditor.spec.ts`, `tests/composables/useBookReadingBootstrap.spec.ts`

**Tests (10):** batch 7 from profiling.

**Verify:** scoped files then full suite.

---

### Phase 8: BookReading restore/format batch
Status: planned

**Scope:** `tests/pages/BookReadingPage.spec.ts`, `tests/toolbars/HorizontalMenu.spec.ts`, `tests/notes/NoteAddQuestion.spec.ts`, `tests/notes/NoteNewForm.spec.ts`, `tests/components/form/RichMarkdownEditor.properties.spec.ts`

**Tests (10):** batch 8 from profiling.

**Verify:** scoped files then full suite.

---

### Phase 9: NoteNewForm / RichMarkdownEditor batch
Status: planned

**Scope:** `tests/notes/NoteNewForm.spec.ts`, `tests/notes/NoteEditableContent.spec.ts`, `tests/components/form/RichMarkdownEditor.properties.spec.ts`, `tests/toolbars/HorizontalMenu.spec.ts`, `tests/components/form/RichMarkdownEditor.spec.ts`, `tests/notes/NoteAddQuestion.spec.ts`

**Tests (10):** batch 9 from profiling.

**Verify:** scoped files then full suite.

---

### Phase 10: RichMarkdownEditor properties / toolbar batch
Status: planned

**Scope:** `tests/components/form/RichMarkdownEditor.properties.spec.ts`, `tests/notes/NoteToolbar.moreOptions.spec.ts`, `tests/components/recall/NoteRefinement.removeSuggestions.spec.ts`, `tests/toolbars/HorizontalMenu.spec.ts`, `tests/notes/NoteNewForm.spec.ts`, `tests/recall/Quiz.spec.ts`

**Tests (10):** batch 10 from profiling.

**Verify:** scoped files then full suite.

---

### Phase 11: FolderPage / SearchDialog / InsertWikiLink batch
Status: planned

**Scope:** `tests/pages/FolderPage.spec.ts`, `tests/links/InsertWikiLink.spec.ts`, `tests/links/SearchDialog.spec.ts`, `tests/components/form/RichMarkdownEditor.properties.spec.ts`

**Tests (10):** batch 11 from profiling.

**Verify:** scoped files then full suite.

---

### Phase 12: NoteEditable / refinement / failure batch
Status: planned

**Scope:** `tests/notes/NoteEditableContent.spec.ts`, `tests/components/recall/QuestionDisplay.spec.ts`, `tests/components/recall/NoteRefinement.removeSuggestions.spec.ts`, `tests/components/form/SeamlessTextEditor.spec.ts`, `tests/components/recall/NoteRefinement.extractNote.spec.ts`, `tests/notes/NoteNewForm.spec.ts`, `tests/components/admin/FailureReportList.spec.ts`, `tests/components/form/RichMarkdownEditor.properties.spec.ts`

**Tests (10):** batch 12 from profiling.

**Verify:** scoped files then full suite.

---

### Phase 13: Wikidata / assimilation batch
Status: planned

**Scope:** `tests/components/form/RichMarkdownEditor.properties.spec.ts`, `tests/components/recall/NoteRefinement.removeSuggestions.spec.ts`, `tests/notes/WikidataAssociationDialog.spec.ts`, `tests/components/recall/AssimilationPanel.spec.ts`, `tests/notes/NoteNewForm.spec.ts`, `tests/pages/FolderPage.spec.ts`

**Tests (10):** batch 13 from profiling.

**Verify:** scoped files then full suite.

---

### Phase 14: Final slow batch
Status: planned

**Scope:** `tests/pages/FolderPage.spec.ts`, `tests/components/form/RichMarkdownEditor.spec.ts`, `tests/notes/NoteEditableContent.spec.ts`, `tests/components/book-reading/PdfBookViewer.gestureZoom.spec.ts`, `tests/components/form/RichMarkdownEditor.properties.spec.ts`

**Tests (7):** batch 14 from profiling.

**Verify:** scoped files then full suite.

---

### Phase 15: Re-profile and close
Status: planned

Re-run JSON reporter, compare top 10% and total wall time, mark plan done.
