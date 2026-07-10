# Frontend UT test optimization

Status: in-progress

**Execution:** run via **execute-plan** (commit + push per phase).

## Profiling baseline (2026-07-10)

Command: `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run --reporter=json`

- **1574 tests**, all passed; suite wall ~**61s** (process wall ~70s incl. Nix)
- Eligible after blacklist: **1574** (no frontend entries under Skip test optimization in `ongoing/test-optimization-blacklist.md`)
- Top 10% total CPU (sum of durations): **~5086ms**
- Raw profile: `ongoing/frontend-profile-results.json` — **do not commit**

### Top 10% slowest (n = ceil(1574 × 0.10) = 158)

| # | ms | file / spec | test / scenario |
|---|-----|-------------|-----------------|
| 1 | 66 | `tests/commons/Popups/PopButton.spec.ts` | blurs button when dialog closes via close_request |
| 2 | 57 | `tests/pages/BookReadingPage.spec.ts` | marking READ clears snap reminder: block no longer snaps when re-visited |
| 3 | 57 | `tests/pages/BookReadingPage.spec.ts` | different unread blocks get independent snap budgets |
| 4 | 57 | `tests/pages/BookReadingPage.spec.ts` | snaps back on second crossing, then allows normal scrolling on third |
| 5 | 56 | `tests/notes/Questions.spec.ts` | shows export dialog when export button is clicked |
| 6 | 55 | `tests/pages/BookReadingPage.spec.ts` | zoom buttons exist with accessible names and page indicator shows via PdfControl |
| 7 | 55 | `tests/pages/BookReadingPage.spec.ts` | does not snap on fourth and later crossings after budget exhausted |
| 8 | 55 | `tests/commons/Modal.spec.ts` | adds top alignment class when content requests stable modal top |
| 9 | 55 | `tests/pages/BookReadingPage.spec.ts` | calls PUT with SKIMMED when Skim is used |
| 10 | 55 | `tests/pages/BookReadingPage.spec.ts` | snaps back and keeps panel visible on first boundary crossing (same-page: scrolls to block start) |
| 11 | 54 | `tests/composables/useBookReadingBootstrap.spec.ts` | sets pdf bootstrap with initial last-read when position includes a PDF locator |
| 12 | 54 | `tests/pages/BookReadingPage.spec.ts` | snap state resets when selection changes to a different block |
| 13 | 54 | `tests/pages/BookReadingPage.spec.ts` | sets data-snap-animating on panel when snap fires |
| 14 | 54 | `tests/pages/BookReadingPage.spec.ts` | marking successor via auto-targeted panel advances selection past successor |
| 15 | 54 | `tests/pages/BookReadingPage.spec.ts` | snaps to last bbox bottom when start anchor and last content bbox are on different pages |
| 16 | 54 | `tests/pages/BookReadingPage.spec.ts` | Read from here makes current block the selected block and hides nav bar |
| 17 | 54 | `tests/pages/BookReadingPage.spec.ts` | shows panel for successor when selected block is already marked and successor bottom is visible |
| 18 | 54 | `tests/pages/BookReadingPage.spec.ts` | clears data-snap-animating after animationend on the inner card |
| 19 | 54 | `tests/pages/BookReadingPage.spec.ts` | does not snap when block already has a recorded disposition |
| 20 | 54 | `tests/pages/BookReadingPage.spec.ts` | Back to selected scrolls to selected block and hides nav bar |
| 21 | 54 | `tests/pages/BookReadingPage.spec.ts` | shows the panel for the last block when content bottom is visible |
| 22 | 54 | `tests/pages/BookReadingPage.spec.ts` | hides the panel when the last block has direct content but bottom is not visible |
| 23 | 54 | `tests/pages/BookReadingPage.spec.ts` | same-page-too-tall: snaps to last content bottom when content does not fit with panel |
| 24 | 54 | `tests/pages/BookReadingPage.spec.ts` | unmounts the reading control panel after Read once it was shown |
| 25 | 54 | `tests/pages/BookReadingPage.spec.ts` | keeps panel visible after geometry becomes false while successor is not yet current |
| 26 | 54 | `tests/pages/BookReadingPage.spec.ts` | does not snap when block has no recorded direct-content bbox |
| 27 | 53 | `tests/pages/BookReadingPage.spec.ts` | PATCH reading position includes selectedBookBlockId after layout click |
| 28 | 53 | `tests/pages/BookReadingPage.spec.ts` | shows the panel when last content bottom is visible and above obstruction |
| 29 | 53 | `tests/pages/BookReadingPage.spec.ts` | anchors panel when last content bottom is visible and anchor px is returned |
| 30 | 53 | `tests/pages/BookReadingPage.spec.ts` | hides the panel when last content bottom is not yet above obstruction |
| 31 | 53 | `tests/pages/BookReadingPage.spec.ts` | hides the panel when the current block is not the immediate successor of the selection |
| 32 | 53 | `tests/pages/BookReadingPage.spec.ts` | does not snap when geometry was never visible for the selection |
| 33 | 53 | `tests/pages/BookReadingPage.spec.ts` | shows navigation bar when current block differs from selected block |
| 34 | 53 | `tests/pages/BookReadingPage.spec.ts` | PATCH reading position uses last viewport top within debounce window |
| 35 | 53 | `tests/pages/BookReadingPage.spec.ts` | shows the panel when the selected block’s successor is the viewport current block |
| 36 | 53 | `tests/pages/BookReadingPage.spec.ts` | hides navigation bar when current block equals selected block |
| 37 | 53 | `tests/pages/BookReadingPage.spec.ts` | updates current block while book layout drawer is closed |
| 38 | 53 | `tests/pages/BookReadingPage.spec.ts` | debounces PATCH reading position on rapid viewport updates |
| 39 | 53 | `tests/pages/BookReadingPage.spec.ts` | shows error when PDF viewer reports invalid PDF |
| 40 | 53 | `tests/pages/BookReadingPage.spec.ts` | book layout toggle exposes aria-expanded and aria-controls |
| 41 | 53 | `tests/pages/BookReadingPage.spec.ts` | moves book layout selection to the successor block after Read |
| 42 | 53 | `tests/pages/BookReadingPage.spec.ts` | snaps back when scrolling lands two or more blocks ahead (not just immediate successor) |
| 43 | 53 | `tests/pages/BookReadingPage.spec.ts` | does not auto-mark when predecessor has no direct content but is already SKIMMED |
| 44 | 53 | `tests/pages/BookReadingPage.spec.ts` | auto-marks predecessor with READ body when it has no direct content and no record |
| 45 | 53 | `tests/pages/BookReadingPage.spec.ts` | does not PATCH reading position when viewport is null |
| 46 | 53 | `tests/pages/BookReadingPage.spec.ts` | does not restore reading position when no snapshot exists |
| 47 | 53 | `tests/pages/BookReadingPage.spec.ts` | hides the Reading Control Panel when the default-selected first block is viewport current |
| 48 | 52 | `tests/pages/BookReadingPage.spec.ts` | shows 'READ' border for blocks returned as 'READ' from reading-records on load |
| 49 | 52 | `tests/pages/BookReadingPage.spec.ts` | loads EPUB into viewer with book title in bar, no PDF viewer |
| 50 | 52 | `tests/pages/BookReadingPage.spec.ts` | restores reading position from stored snapshot on open |
| 51 | 52 | `tests/pages/BookReadingPage.spec.ts` | shows loading indicator while PDF is loading, hides it after render |
| 52 | 52 | `tests/pages/BookReadingPage.spec.ts` | restores selected book block from stored reading snapshot |
| 53 | 52 | `tests/pages/BookReadingPage.spec.ts` | shows 'SKIMMED' border for blocks returned as 'SKIMMED' from reading-records on load |
| 54 | 51 | `tests/composables/useBookReadingBootstrap.spec.ts` | sets epub bootstrap with null initial locator when no reading position |
| 55 | 49 | `tests/toolbars/HorizontalMenu.spec.ts` | expands menu when clicking menu icon |
| 56 | 49 | `tests/toolbars/HorizontalMenu.spec.ts` | collapses menu when expand button is clicked again |
| 57 | 35 | `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` | submits only checked descendants when parent is indeterminate (remove) |
| 58 | 34 | `tests/toolbars/HorizontalMenu.spec.ts` | expands menu when expand button is clicked |
| 59 | 33 | `tests/pages/FolderPage.spec.ts` | retries cross-notebook folder move with merge after 409 conflict |
| 60 | 32 | `tests/components/recall/AssimilationSettings.spec.ts` | renders a property row and Assimilate control per frontmatter key |
| 61 | 31 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | extracts multiple selected layout points into one preview |
| 62 | 30 | `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` | saves an empty list from popup |
| 63 | 29 | `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` | rejects empty list items on save |
| 64 | 28 | `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` | allows duplicate list items in popup save |
| 65 | 28 | `tests/pages/FolderPage.spec.ts` | sends destinationNotebookId and newParentFolderId for cross-notebook folder move |
| 66 | 28 | `tests/toolbars/HorizontalMenu.spec.ts` | shows all menu items when expanded |
| 67 | 26 | `tests/notes/NoteNewForm.spec.ts` | search 'dog' get 'Canine' with action 'append' updates title as 'dog' |
| 68 | 26 | `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` | includes parent id when all descendants are selected again |
| 69 | 26 | `tests/notes/NoteEditableContent.spec.ts` | should preserve second edit when first save response arrives after second edit |
| 70 | 26 | `tests/links/SearchDialog.spec.ts` | shows relationship form when Add a new relationship note is clicked |
| 71 | 25 | `tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts` | emits valid aliases list edits from popup |
| 72 | 25 | `tests/notes/NoteNewForm.spec.ts` | search 'dog' get 'Canine' with action 'replace' updates title as 'Canine' |
| 73 | 24 | `tests/toolbars/HorizontalMenu.spec.ts` | collapses menu when route changes |
| 74 | 24 | `tests/toolbars/HorizontalMenu.spec.ts` | collapses menu when losing focus |
| 75 | 24 | `tests/links/SearchDialog.spec.ts` | shows confirm when move is blocked by soft-deleted title at destination |
| 76 | 24 | `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` | saves list as scalar when user switches to text mode in popup |
| 77 | 24 | `tests/links/SearchDialog.spec.ts` | calls moveNoteToFolder with folder id after confirm |
| 78 | 24 | `tests/pages/NotebookPage.spec.ts` | saves notebook index content directly to container on save |
| 79 | 23 | `tests/components/form/RichMarkdownEditor.propertyValuePopup.spec.ts` | cancel closes popup without emitting property changes |
| 80 | 23 | `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` | selecting a resolved preset inserts the suffixed key when base is taken ('list-capable preset') |
| 81 | 23 | `tests/toolbars/HorizontalMenu.spec.ts` | collapses menu when clicking outside |
| 82 | 23 | `tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts` | inserts the first alias as a list when adding a new aliases property |
| 83 | 23 | `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` | selecting a resolved preset inserts the suffixed key when base is taken ('scalar-only preset') |
| 84 | 22 | `tests/links/SearchDialog.spec.ts` | calls moveNoteToNotebookRootInNotebook with notebook id after confirm |
| 85 | 22 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | shows LoadingModal while creating note from preview |
| 86 | 22 | `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` | submits only checked descendants when parent is indeterminate (extract) |
| 87 | 22 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | keeps edited preview fields when retry is cancelled |
| 88 | 22 | `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` | removes non-contiguous selected layout points |
| 89 | 21 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | displays one extract button and no per-item extract buttons |
| 90 | 21 | `tests/links/InsertWikiLink.spec.ts` | calls the wiki-property inserter when Add wiki link as a new property is clicked |
| 91 | 21 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | shows confirmation before retry when preview fields were edited |
| 92 | 21 | `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` | seeds text mode from populated list when switching from list mode |
| 93 | 21 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | shows inline error when retry preview API fails |
| 94 | 21 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | creates a note from edited preview fields |
| 95 | 21 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | disables Create note when new note title is blank |
| 96 | 21 | `tests/notes/NoteMoreOptionsForm.deleteNote.relationship.spec.ts` | uses the current note id when note prop changes without remount |
| 97 | 21 | `tests/components/recall/NoteRefinement.removeLayout.loading.spec.ts` | hides LoadingModal when remove API fails |
| 98 | 20 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | returns to the layout when Back is clicked |
| 99 | 20 | `tests/links/SearchDialog.spec.ts` | rewrites note content when linking dead link to existing note |
| 100 | 20 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | discards edited preview fields when retry is confirmed |
| 101 | 20 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | shows LoadingModal while retrying extract preview |
| 102 | 20 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | replaces preview fields when Ask AI to retry is clicked |
| 103 | 20 | `tests/recall/Quiz.spec.ts` | should show ContentLoader, not JustReview, when navigating to a memory tracker that previously failed |
| 104 | 20 | `tests/components/form/RichMarkdownEditor.propertyTouchFocus.spec.ts` | does not focus primer when pointer is not coarse |
| 105 | 19 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | shows create errors in the preview |
| 106 | 19 | `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` | sets property key when a preset is chosen ('insert') |
| 107 | 19 | `tests/links/SearchDialog.spec.ts` | collapses search key history inside a modal when clicking elsewhere in that modal |
| 108 | 19 | `tests/notes/NoteNewForm.spec.ts` | search 'dog' get 'dog' with action undefined updates title as 'dog' |
| 109 | 19 | `tests/notes/NoteNewForm.spec.ts` | search 'dog' get 'Dog' with action undefined updates title as 'Dog' |
| 110 | 19 | `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` | sets property key when a preset is chosen ('existing row') |
| 111 | 19 | `tests/notes/WikidataAssociationDialog.spec.ts` | enables Save and emits save with empty string when clearing and canSaveEmptyToClear |
| 112 | 19 | `tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts` | commits custom relationship text from the dialog and emits updated frontmatter |
| 113 | 19 | `tests/components/form/SeamlessTextEditor.spec.ts` | does not handle paste when readonly |
| 114 | 18 | `tests/notes/NoteNewForm.spec.ts` | closes dialog when cancel is clicked |
| 115 | 18 | `tests/recall/Quiz.spec.ts` | fetch the first 1 question when mount |
| 116 | 18 | `tests/notes/WikidataAssociationDialog.spec.ts` | emits selected with add alias action |
| 117 | 18 | `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` | property key preset dropdown resolves occupied presets to suffixed keys |
| 118 | 18 | `tests/pages/FolderPage.spec.ts` | navigates to the moved folder in the destination notebook after a cross-notebook root move |
| 119 | 18 | `tests/components/form/RichMarkdownEditor.properties.spec.ts` | shows validation and does not emit corrupt duplicate keys when renaming a row |
| 120 | 17 | `tests/pages/FolderPage.spec.ts` | shows merge confirm when dissolve returns 409 and retries with merge=true |
| 121 | 17 | `tests/notes/WikidataAssociationDialog.spec.ts` | emits selected with replace action |
| 122 | 17 | `tests/pages/FolderPage.spec.ts` | sends destinationNotebookId when moving to another notebook root |
| 123 | 17 | `tests/components/form/RichMarkdownEditor.propertyValuePopup.spec.ts` | hides list mode for scalar-only structural keys |
| 124 | 17 | `tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts` | blocks commit when parsed aliases row is scalar |
| 125 | 17 | `tests/pages/FolderPage.spec.ts` | re-enables organize controls after moving into a neighbour folder |
| 126 | 17 | `tests/components/form/RichMarkdownEditor.properties.spec.ts` | emits deadLinkClick when a dead wiki link in a property value is clicked |
| 127 | 17 | `tests/pages/FolderPage.spec.ts` | shows inline conflict error when rename returns 409 FOLDER_NAME_CONFLICT |
| 128 | 17 | `tests/components/admin/FailureReportList.spec.ts` | closes modal when cancel is clicked |
| 129 | 17 | `tests/links/InsertWikiLink.spec.ts` | does not call the inserter when Add a new relationship note is clicked |
| 130 | 17 | `tests/links/SearchDialog.spec.ts` | collapses search key history when clicking a search scope toggle |
| 131 | 17 | `tests/components/recall/AssimilationPanel.spec.ts` | closes popup and returns to original state when user closes it |
| 132 | 17 | `tests/components/recall/NoteRefinement.exportExtractRequest.spec.ts` | opens export dialog with extract request JSON for the selection |
| 133 | 17 | `tests/notes/NoteNewForm.spec.ts` | displays reserved title error when api returns binding error for newTitle |
| 134 | 17 | `tests/notes/WikidataAssociationDialog.spec.ts` | saves when clicking Save button after selecting from result list |
| 135 | 17 | `tests/links/SearchDialog.spec.ts` | shows 'Link ... to this note' button when dead link payload is provided |
| 136 | 17 | `tests/notes/WikidataAssociationDialog.spec.ts` | saves with add alias action immediately when user selects Add as alias |
| 137 | 17 | `tests/components/book-reading/PdfBookViewer.gestureZoom.spec.ts` | meta+wheel on the viewer prevents default and updates pdf scale |
| 138 | 17 | `tests/components/admin/QuestionGenerationBatchStatus.spec.ts` | disables the manual generation button while submitting |
| 139 | 17 | `tests/notes/NoteEditableContent.spec.ts` | should preserve unsaved edits if the noteContent prop doesn't actually change |
| 140 | 16 | `tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts` | opens dialog with custom text prefilled for an unknown relation |
| 141 | 16 | `tests/notes/NoteEditableContent.spec.ts` | updates the tracker property key and saves when the user confirms renaming |
| 142 | 16 | `tests/components/admin/FailureReportList.spec.ts` | deletes selected reports when confirmed |
| 143 | 16 | `tests/components/form/RichMarkdownEditor.properties.spec.ts` | emits deadLinkClick with target token when a property wiki link uses display text |
| 144 | 16 | `tests/pages/MemoryTrackerPageView.spec.ts` | excludes contested prompts from unanswered count in confirmation message |
| 145 | 16 | `tests/notes/NoteNewForm.spec.ts` | opens dialog when clicking search button |
| 146 | 16 | `tests/notes/WikidataAssociationDialog.spec.ts` | saves with replace action immediately when user selects Replace |
| 147 | 16 | `tests/components/recall/AssimilationSettings.spec.ts` | emits assimilate with skipMemoryTracking and propertyKey when skip recall is clicked |
| 148 | 16 | `tests/notes/TextContentWrapper.spec.ts` | keeps the draft when choosing a save option (click does not discard before save) |
| 149 | 16 | `tests/components/recall/AssimilationSettings.spec.ts` | emits revive with propertyKey when Revive is clicked on a skipped property |
| 150 | 16 | `tests/components/recall/NoteRefinement.exportExtractRequest.spec.ts` | does not show export button on the extraction preview screen |
| 151 | 16 | `tests/components/recall/NoteRefinement.removeLayout.spec.ts` | shows confirmation dialog when remove button is clicked |
| 152 | 16 | `tests/pages/MemoryTrackerPageView.spec.ts` | calls delete endpoint and emits refresh when confirmed |
| 153 | 16 | `tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts` | empty index-only fields are not included in emitted YAML |
| 154 | 16 | `tests/notes/NoteEditableContent.spec.ts` | should auto-save edited content after debounce timeout without blur |
| 155 | 16 | `tests/notes/NoteEditableContent.spec.ts` | soft-deletes the tracker and saves when the user confirms removing a tracked property |
| 156 | 16 | `tests/notes/NoteEditableContent.spec.ts` | should save with trailing empty lines and <p><br></p> removed when change is not only at the end |
| 157 | 16 | `tests/pages/MemoryTrackerPageView.spec.ts` | shows correct count in confirmation message for multiple prompts |
| 158 | 16 | `tests/toolbars/NoteUndoButton.spec.ts` | discards edit content item and shows next item |

### Grouping

- By file: **36** groups
- Batches of 3: **53** groups (`ceil(158 / 3)`)
- **Chosen:** by file (fewer groups; also preferred on tie)

## Optimization rules

1. Remove or simplify redundant tests first.
2. Strictly no fixed-time waits.
3. Flaky = failure.

Hard-to-improve tests: propose under **Candidates** in
`ongoing/test-optimization-blacklist.md` (do not move to Skip test optimization
without developer review).

Verify changes with `pnpm frontend:test` (browser mode), not only plain
`vitest run` used for profiling.

---

### Phase 1: pages/BookReadingPage
Status: done

**Learnings:** Replacing `vi.waitFor` (8s timeout) with a tight `flushPromises`/`nextTick` poll cut per-test PDF wait cost sharply. Merging snap-budget and snap-animating cases plus shared geometry/nav helpers removed 3 tests without losing coverage. Suite CPU ~2709ms → ~131ms (47 tests).

**Tests:** (49 in top 10%, ~2627ms combined — pre-optimization baseline)
- `tests/pages/BookReadingPage.spec.ts` — "marking READ clears snap reminder: block no longer snaps when re-visited" (~57ms)
- `tests/pages/BookReadingPage.spec.ts` — "different unread blocks get independent snap budgets" (~57ms)
- `tests/pages/BookReadingPage.spec.ts` — "snaps back on second crossing, then allows normal scrolling on third" (~57ms)
- `tests/pages/BookReadingPage.spec.ts` — "zoom buttons exist with accessible names and page indicator shows via PdfControl" (~55ms)
- `tests/pages/BookReadingPage.spec.ts` — "does not snap on fourth and later crossings after budget exhausted" (~55ms)
- `tests/pages/BookReadingPage.spec.ts` — "calls PUT with SKIMMED when Skim is used" (~55ms)
- `tests/pages/BookReadingPage.spec.ts` — "snaps back and keeps panel visible on first boundary crossing (same-page: scrolls to block start)" (~55ms)
- `tests/pages/BookReadingPage.spec.ts` — "snap state resets when selection changes to a different block" (~54ms)
- `tests/pages/BookReadingPage.spec.ts` — "sets data-snap-animating on panel when snap fires" (~54ms)
- `tests/pages/BookReadingPage.spec.ts` — "marking successor via auto-targeted panel advances selection past successor" (~54ms)
- `tests/pages/BookReadingPage.spec.ts` — "snaps to last bbox bottom when start anchor and last content bbox are on different pages" (~54ms)
- `tests/pages/BookReadingPage.spec.ts` — "Read from here makes current block the selected block and hides nav bar" (~54ms)
- `tests/pages/BookReadingPage.spec.ts` — "shows panel for successor when selected block is already marked and successor bottom is visible" (~54ms)
- `tests/pages/BookReadingPage.spec.ts` — "clears data-snap-animating after animationend on the inner card" (~54ms)
- `tests/pages/BookReadingPage.spec.ts` — "does not snap when block already has a recorded disposition" (~54ms)
- `tests/pages/BookReadingPage.spec.ts` — "Back to selected scrolls to selected block and hides nav bar" (~54ms)
- `tests/pages/BookReadingPage.spec.ts` — "shows the panel for the last block when content bottom is visible" (~54ms)
- `tests/pages/BookReadingPage.spec.ts` — "hides the panel when the last block has direct content but bottom is not visible" (~54ms)
- `tests/pages/BookReadingPage.spec.ts` — "same-page-too-tall: snaps to last content bottom when content does not fit with panel" (~54ms)
- `tests/pages/BookReadingPage.spec.ts` — "unmounts the reading control panel after Read once it was shown" (~54ms)
- `tests/pages/BookReadingPage.spec.ts` — "keeps panel visible after geometry becomes false while successor is not yet current" (~54ms)
- `tests/pages/BookReadingPage.spec.ts` — "does not snap when block has no recorded direct-content bbox" (~54ms)
- `tests/pages/BookReadingPage.spec.ts` — "PATCH reading position includes selectedBookBlockId after layout click" (~53ms)
- `tests/pages/BookReadingPage.spec.ts` — "shows the panel when last content bottom is visible and above obstruction" (~53ms)
- `tests/pages/BookReadingPage.spec.ts` — "anchors panel when last content bottom is visible and anchor px is returned" (~53ms)
- `tests/pages/BookReadingPage.spec.ts` — "hides the panel when last content bottom is not yet above obstruction" (~53ms)
- `tests/pages/BookReadingPage.spec.ts` — "hides the panel when the current block is not the immediate successor of the selection" (~53ms)
- `tests/pages/BookReadingPage.spec.ts` — "does not snap when geometry was never visible for the selection" (~53ms)
- `tests/pages/BookReadingPage.spec.ts` — "shows navigation bar when current block differs from selected block" (~53ms)
- `tests/pages/BookReadingPage.spec.ts` — "PATCH reading position uses last viewport top within debounce window" (~53ms)
- `tests/pages/BookReadingPage.spec.ts` — "shows the panel when the selected block’s successor is the viewport current block" (~53ms)
- `tests/pages/BookReadingPage.spec.ts` — "hides navigation bar when current block equals selected block" (~53ms)
- `tests/pages/BookReadingPage.spec.ts` — "updates current block while book layout drawer is closed" (~53ms)
- `tests/pages/BookReadingPage.spec.ts` — "debounces PATCH reading position on rapid viewport updates" (~53ms)
- `tests/pages/BookReadingPage.spec.ts` — "shows error when PDF viewer reports invalid PDF" (~53ms)
- `tests/pages/BookReadingPage.spec.ts` — "book layout toggle exposes aria-expanded and aria-controls" (~53ms)
- `tests/pages/BookReadingPage.spec.ts` — "moves book layout selection to the successor block after Read" (~53ms)
- `tests/pages/BookReadingPage.spec.ts` — "snaps back when scrolling lands two or more blocks ahead (not just immediate successor)" (~53ms)
- `tests/pages/BookReadingPage.spec.ts` — "does not auto-mark when predecessor has no direct content but is already SKIMMED" (~53ms)
- `tests/pages/BookReadingPage.spec.ts` — "auto-marks predecessor with READ body when it has no direct content and no record" (~53ms)
- `tests/pages/BookReadingPage.spec.ts` — "does not PATCH reading position when viewport is null" (~53ms)
- `tests/pages/BookReadingPage.spec.ts` — "does not restore reading position when no snapshot exists" (~53ms)
- `tests/pages/BookReadingPage.spec.ts` — "hides the Reading Control Panel when the default-selected first block is viewport current" (~53ms)
- `tests/pages/BookReadingPage.spec.ts` — "shows 'READ' border for blocks returned as 'READ' from reading-records on load" (~52ms)
- `tests/pages/BookReadingPage.spec.ts` — "loads EPUB into viewer with book title in bar, no PDF viewer" (~52ms)
- `tests/pages/BookReadingPage.spec.ts` — "restores reading position from stored snapshot on open" (~52ms)
- `tests/pages/BookReadingPage.spec.ts` — "shows loading indicator while PDF is loading, hides it after render" (~52ms)
- `tests/pages/BookReadingPage.spec.ts` — "restores selected book block from stored reading snapshot" (~52ms)
- `tests/pages/BookReadingPage.spec.ts` — "shows 'SKIMMED' border for blocks returned as 'SKIMMED' from reading-records on load" (~52ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/BookReadingPage.spec.ts
```

---

### Phase 2: components/recall/NoteRefinement.extractNote
Status: done

**Learnings:** Merged 4 redundant cases (create-button toggle, retry confirm/cancel, paired loading-modal success+failure) and hoisted shared mount/preview helpers into `noteRefinementTestSupport.ts` (`data-test-id` extract button, `mountNoteRefinementReady`, `expectPreviewFields`, `createDeferredGate`). Suite CPU ~370ms → ~326ms (19 → 15 tests).

**Tests:** (13 in top 10%, ~278ms combined — pre-optimization baseline)
- `tests/components/recall/NoteRefinement.extractNote.spec.ts` — "extracts multiple selected layout points into one preview" (~31ms)
- `tests/components/recall/NoteRefinement.extractNote.spec.ts` — "shows LoadingModal while creating note from preview" (~22ms)
- `tests/components/recall/NoteRefinement.extractNote.spec.ts` — "keeps edited preview fields when retry is cancelled" (~22ms)
- `tests/components/recall/NoteRefinement.extractNote.spec.ts` — "displays one extract button and no per-item extract buttons" (~21ms)
- `tests/components/recall/NoteRefinement.extractNote.spec.ts` — "shows confirmation before retry when preview fields were edited" (~21ms)
- `tests/components/recall/NoteRefinement.extractNote.spec.ts` — "shows inline error when retry preview API fails" (~21ms)
- `tests/components/recall/NoteRefinement.extractNote.spec.ts` — "creates a note from edited preview fields" (~21ms)
- `tests/components/recall/NoteRefinement.extractNote.spec.ts` — "disables Create note when new note title is blank" (~21ms)
- `tests/components/recall/NoteRefinement.extractNote.spec.ts` — "returns to the layout when Back is clicked" (~20ms)
- `tests/components/recall/NoteRefinement.extractNote.spec.ts` — "discards edited preview fields when retry is confirmed" (~20ms)
- `tests/components/recall/NoteRefinement.extractNote.spec.ts` — "shows LoadingModal while retrying extract preview" (~20ms)
- `tests/components/recall/NoteRefinement.extractNote.spec.ts` — "replaces preview fields when Ask AI to retry is clicked" (~20ms)
- `tests/components/recall/NoteRefinement.extractNote.spec.ts` — "shows create errors in the preview" (~19ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.extractNote.spec.ts
```

---

### Phase 3: toolbars/HorizontalMenu
Status: done

**Learnings:** Hoisted mount/mock/selectors into `horizontalMenuTestSupport.ts`, replaced all `page.getByLabelText` with `querySelector` via `ariaLabelEl`, merged initial-state and toggle+show-all-items cases, removed redundant route-already-collapsed test. Suite CPU ~294ms → ~42ms (21 → 15 tests).

**Tests:** (7 in top 10%, ~231ms combined — pre-optimization baseline)
- `tests/toolbars/HorizontalMenu.spec.ts` — "expands menu when clicking menu icon" (~49ms)
- `tests/toolbars/HorizontalMenu.spec.ts` — "collapses menu when expand button is clicked again" (~49ms)
- `tests/toolbars/HorizontalMenu.spec.ts` — "expands menu when expand button is clicked" (~34ms)
- `tests/toolbars/HorizontalMenu.spec.ts` — "shows all menu items when expanded" (~28ms)
- `tests/toolbars/HorizontalMenu.spec.ts` — "collapses menu when route changes" (~24ms)
- `tests/toolbars/HorizontalMenu.spec.ts` — "collapses menu when losing focus" (~24ms)
- `tests/toolbars/HorizontalMenu.spec.ts` — "collapses menu when clicking outside" (~23ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars/HorizontalMenu.spec.ts
```

---

### Phase 4: links/SearchDialog
Status: done

**Learnings:** Hoisted mount/mocks/selectors into `searchDialogTestSupport.ts` (`titleEl`, `renderSearchWithKeyHistory`, `openSearchKeyHistoryDropdown`). Replaced `findByPlaceholderText` with `getByPlaceholderText` after `flushPromises`. Merged add-link choice + relationship form, dead-link button + rewrite, and paired history-collapse cases (`it.each`). Suite CPU ~274ms → ~260ms (17 → 15 tests).

**Tests:** (8 in top 10%, ~168ms combined — pre-optimization baseline)
- `tests/links/SearchDialog.spec.ts` — "shows relationship form when Add a new relationship note is clicked" (~26ms)
- `tests/links/SearchDialog.spec.ts` — "shows confirm when move is blocked by soft-deleted title at destination" (~24ms)
- `tests/links/SearchDialog.spec.ts` — "calls moveNoteToFolder with folder id after confirm" (~24ms)
- `tests/links/SearchDialog.spec.ts` — "calls moveNoteToNotebookRootInNotebook with notebook id after confirm" (~22ms)
- `tests/links/SearchDialog.spec.ts` — "rewrites note content when linking dead link to existing note" (~20ms)
- `tests/links/SearchDialog.spec.ts` — "collapses search key history inside a modal when clicking elsewhere in that modal" (~19ms)
- `tests/links/SearchDialog.spec.ts` — "collapses search key history when clicking a search scope toggle" (~17ms)
- `tests/links/SearchDialog.spec.ts` — "shows 'Link ... to this note' button when dead link payload is provided" (~17ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/links/SearchDialog.spec.ts
```

---

### Phase 5: pages/FolderPage
Status: done

**Learnings:** Hoisted cross-notebook mount/select helpers into `folderPageTestSupport.ts`, merged cross-notebook root move API + navigation cases, folded same-notebook merge navigation into the `it.each` conflict-retry cases. Suite CPU ~234ms → ~192ms (13 → 11 tests).

**Tests:** (7 in top 10%, ~148ms combined — pre-optimization baseline)
- `tests/pages/FolderPage.spec.ts` — "retries cross-notebook folder move with merge after 409 conflict" (~33ms)
- `tests/pages/FolderPage.spec.ts` — "sends destinationNotebookId and newParentFolderId for cross-notebook folder move" (~28ms)
- `tests/pages/FolderPage.spec.ts` — "navigates to the moved folder in the destination notebook after a cross-notebook root move" (~18ms)
- `tests/pages/FolderPage.spec.ts` — "shows merge confirm when dissolve returns 409 and retries with merge=true" (~17ms)
- `tests/pages/FolderPage.spec.ts` — "sends destinationNotebookId when moving to another notebook root" (~17ms)
- `tests/pages/FolderPage.spec.ts` — "re-enables organize controls after moving into a neighbour folder" (~17ms)
- `tests/pages/FolderPage.spec.ts` — "shows inline conflict error when rename returns 409 FOLDER_NAME_CONFLICT" (~17ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/FolderPage.spec.ts
```

---

### Phase 6: notes/NoteNewForm
Status: done

**Learnings:** Hoisted mount/mocks/selectors into `noteNewFormTestSupport.ts`; merged open+close wikidata dialog cases; replaced `findComponent(WikidataAssociationDialog)` with `wikidataDialogIsOpen()` querySelector; dropped redundant `searchWikidataSpy` assertion from `it.each` title cases; use `data-testid` wikidata result items. Suite CPU ~296ms → ~271ms (18 → 17 tests); top-7 target ~163ms → ~160ms.

**Tests:** (7 in top 10%, ~140ms combined)
- `tests/notes/NoteNewForm.spec.ts` — "search 'dog' get 'Canine' with action 'append' updates title as 'dog'" (~26ms)
- `tests/notes/NoteNewForm.spec.ts` — "search 'dog' get 'Canine' with action 'replace' updates title as 'Canine'" (~25ms)
- `tests/notes/NoteNewForm.spec.ts` — "search 'dog' get 'dog' with action undefined updates title as 'dog'" (~19ms)
- `tests/notes/NoteNewForm.spec.ts` — "search 'dog' get 'Dog' with action undefined updates title as 'Dog'" (~19ms)
- `tests/notes/NoteNewForm.spec.ts` — "closes dialog when cancel is clicked" (~18ms)
- `tests/notes/NoteNewForm.spec.ts` — "displays reserved title error when api returns binding error for newTitle" (~17ms)
- `tests/notes/NoteNewForm.spec.ts` — "opens dialog when clicking search button" (~16ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteNewForm.spec.ts
```

---

### Phase 7: components/form/RichMarkdownEditor.propertyValuePopupModeSwitch
Status: done

**Learnings:** Hoisted mount/markdown/mode-switch helpers into `propertyValuePopupModeSwitchTestSupport.ts`; added shared `dialogEl`, `isListModeTabActive`, `popupValidationText`, `savePopup` to `propertyValuePopupTestDom.ts`; replaced inline `querySelector` with helpers. Suite CPU ~194ms → ~184ms (6 tests; no redundant cases to merge).

**Tests:** (5 in top 10%, ~133ms combined)
- `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` — "saves an empty list from popup" (~30ms)
- `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` — "rejects empty list items on save" (~29ms)
- `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` — "allows duplicate list items in popup save" (~28ms)
- `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` — "saves list as scalar when user switches to text mode in popup" (~24ms)
- `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` — "seeds text mode from populated list when switching from list mode" (~21ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts
```

---

### Phase 8: composables/useBookReadingBootstrap
Status: done

**Learnings:** Hoisted mount/mocks into `useBookReadingBootstrapTestSupport.ts`; replaced real PDF/EPUB fixture fetches with minimal fake bytes (composable only stores bytes); replaced `vi.waitFor` with tight `flushPromises`/`nextTick` poll. Suite CPU ~106ms → ~7ms (3 tests).

**Tests:** (2 in top 10%, ~106ms combined — pre-optimization baseline)
- `tests/composables/useBookReadingBootstrap.spec.ts` — "sets pdf bootstrap with initial last-read when position includes a PDF locator" (~54ms)
- `tests/composables/useBookReadingBootstrap.spec.ts` — "sets epub bootstrap with null initial locator when no reading position" (~51ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/composables/useBookReadingBootstrap.spec.ts
```

---

### Phase 9: notes/NoteEditableContent
Status: done

**Learnings:** Hoisted mount/mocks/selectors into `noteEditableContentTestSupport.ts`; split 858-line spec into 6 cohesive files (all under 250 lines); merged paste popup yes/no, relation property show/omit, and HTML normalization no-save cases with `it.each`. Suite CPU ~373ms → ~260ms (25 tests).

**Tests:** (6 in top 10%, ~106ms combined — pre-optimization baseline)
- `tests/notes/NoteEditableContent.spec.ts` — "should preserve second edit when first save response arrives after second edit" (~26ms)
- `tests/notes/NoteEditableContent.spec.ts` — "should preserve unsaved edits if the noteContent prop doesn't actually change" (~17ms)
- `tests/notes/NoteEditableContent.spec.ts` — "updates the tracker property key and saves when the user confirms renaming" (~16ms)
- `tests/notes/NoteEditableContent.spec.ts` — "should auto-save edited content after debounce timeout without blur" (~16ms)
- `tests/notes/NoteEditableContent.spec.ts` — "soft-deletes the tracker and saves when the user confirms removing a tracked property" (~16ms)
- `tests/notes/NoteEditableContent.spec.ts` — "should save with trailing empty lines and <p><br></p> removed when change is not only at the end" (~16ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableContent.spec.ts
```

---

### Phase 10: components/recall/NoteRefinement.layoutSelection
Status: done

**Learnings:** Merged parent cascade + indeterminate checkbox cases; folded extract/remove indeterminate API submission into `it.each`; hoisted `mountNestedLayoutWithIndeterminateParentSelection` and `clickRemoveRefinementLayout` into `noteRefinementTestSupport.ts`; use `mountNoteRefinementWithLayoutReady` and `data-test-id` action helpers. Suite CPU ~104ms → ~107ms wall (7 → 6 tests).

**Tests:** (4 in top 10%, ~104ms combined — pre-optimization baseline)
- `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` — "submits only checked descendants when parent is indeterminate (remove)" (~35ms)
- `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` — "includes parent id when all descendants are selected again" (~26ms)
- `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` — "submits only checked descendants when parent is indeterminate (extract)" (~22ms)
- `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` — "removes non-contiguous selected layout points" (~22ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.layoutSelection.spec.ts
```

---

### Phase 11: notes/WikidataAssociationDialog
Status: planned

**Tests:** (6 in top 10%, ~104ms combined)
- `tests/notes/WikidataAssociationDialog.spec.ts` — "enables Save and emits save with empty string when clearing and canSaveEmptyToClear" (~19ms)
- `tests/notes/WikidataAssociationDialog.spec.ts` — "emits selected with add alias action" (~18ms)
- `tests/notes/WikidataAssociationDialog.spec.ts` — "emits selected with replace action" (~17ms)
- `tests/notes/WikidataAssociationDialog.spec.ts` — "saves when clicking Save button after selecting from result list" (~17ms)
- `tests/notes/WikidataAssociationDialog.spec.ts` — "saves with add alias action immediately when user selects Add as alias" (~17ms)
- `tests/notes/WikidataAssociationDialog.spec.ts` — "saves with replace action immediately when user selects Replace" (~16ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/WikidataAssociationDialog.spec.ts
```

---

### Phase 12: components/form/RichMarkdownEditor.propertyKeyPresets
Status: done

**Learnings:** Hoisted mount/focus/select helpers into `propertyKeyPresetsTestSupport.ts` and fast `querySelector` DOM helpers into `propertyKeyPresetsTestDom.ts`; merged show-options, set-key, occupied-suffix, and suffixed-selection cases into one `it.each` (4 scenarios); dropped standalone occupied-preset test covered by unit tests + merged UI case. Suite CPU ~102ms → ~106ms wall (8 → 5 tests).

**Tests:** (5 in top 10%, ~102ms combined)
- `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` — "selecting a resolved preset inserts the suffixed key when base is taken ('list-capable preset')" (~23ms)
- `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` — "selecting a resolved preset inserts the suffixed key when base is taken ('scalar-only preset')" (~23ms)
- `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` — "sets property key when a preset is chosen ('insert')" (~19ms)
- `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` — "sets property key when a preset is chosen ('existing row')" (~19ms)
- `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` — "property key preset dropdown resolves occupied presets to suffixed keys" (~18ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts
```

---

### Phase 13: commons/Popups/PopButton
Status: done

**Learnings:** Hoisted mount/selectors into `popButtonTestSupport.ts`; replaced full `routes` import with minimal memory router; dropped `page.getByText` and `vi.waitUntil` in favor of `wrapper.find("button")` and `flushPromises`/`nextTick` poll; merged close-button + ESC blur cases with `it.each`; primer mount only for soft-keyboard tests. Suite CPU ~66ms → ~27ms (5 tests).

**Tests:** (1 in top 10%, ~66ms combined)
- `tests/commons/Popups/PopButton.spec.ts` — "blurs button when dialog closes via close_request" (~66ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/commons/Popups/PopButton.spec.ts
```

---

### Phase 14: components/recall/AssimilationSettings
Status: done

**Learnings:** Hoisted mount/mocks/selectors into `assimilationSettingsTestSupport.ts`; merged rendering+assimilate emit and skipped-tracker Revive UI+emit cases; use `input[name="skip"]` and `data-test="revive"` instead of `[value="…"]`. Suite CPU ~65ms → ~48ms (6 → 4 tests).

**Tests:** (3 in top 10%, ~65ms combined — pre-optimization baseline)
- `tests/components/recall/AssimilationSettings.spec.ts` — "renders a property row and Assimilate control per frontmatter key" (~32ms)
- `tests/components/recall/AssimilationSettings.spec.ts` — "emits assimilate with skipMemoryTracking and propertyKey when skip recall is clicked" (~16ms)
- `tests/components/recall/AssimilationSettings.spec.ts` — "emits revive with propertyKey when Revive is clicked on a skipped property" (~16ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/AssimilationSettings.spec.ts
```

---

### Phase 15: components/form/RichMarkdownEditor.aliasesProperty
Status: planned

**Tests:** (3 in top 10%, ~65ms combined)
- `tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts` — "emits valid aliases list edits from popup" (~25ms)
- `tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts` — "inserts the first alias as a list when adding a new aliases property" (~23ms)
- `tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts` — "blocks commit when parsed aliases row is scalar" (~17ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts
```

---

### Phase 16: notes/Questions
Status: planned

**Tests:** (1 in top 10%, ~56ms combined)
- `tests/notes/Questions.spec.ts` — "shows export dialog when export button is clicked" (~56ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/Questions.spec.ts
```

---

### Phase 17: commons/Modal
Status: planned

**Tests:** (1 in top 10%, ~55ms combined)
- `tests/commons/Modal.spec.ts` — "adds top alignment class when content requests stable modal top" (~55ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/commons/Modal.spec.ts
```

---

### Phase 18: components/form/RichMarkdownEditor.propertyRelationImageIndex
Status: planned

**Tests:** (3 in top 10%, ~51ms combined)
- `tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts` — "commits custom relationship text from the dialog and emits updated frontmatter" (~19ms)
- `tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts` — "opens dialog with custom text prefilled for an unknown relation" (~16ms)
- `tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts` — "empty index-only fields are not included in emitted YAML" (~16ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts
```

---

### Phase 19: components/form/RichMarkdownEditor.properties
Status: planned

**Tests:** (3 in top 10%, ~51ms combined)
- `tests/components/form/RichMarkdownEditor.properties.spec.ts` — "shows validation and does not emit corrupt duplicate keys when renaming a row" (~18ms)
- `tests/components/form/RichMarkdownEditor.properties.spec.ts` — "emits deadLinkClick when a dead wiki link in a property value is clicked" (~17ms)
- `tests/components/form/RichMarkdownEditor.properties.spec.ts` — "emits deadLinkClick with target token when a property wiki link uses display text" (~16ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.properties.spec.ts
```

---

### Phase 20: pages/MemoryTrackerPageView
Status: planned

**Tests:** (3 in top 10%, ~48ms combined)
- `tests/pages/MemoryTrackerPageView.spec.ts` — "excludes contested prompts from unanswered count in confirmation message" (~16ms)
- `tests/pages/MemoryTrackerPageView.spec.ts` — "calls delete endpoint and emits refresh when confirmed" (~16ms)
- `tests/pages/MemoryTrackerPageView.spec.ts` — "shows correct count in confirmation message for multiple prompts" (~16ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/MemoryTrackerPageView.spec.ts
```

---

### Phase 21: components/form/RichMarkdownEditor.propertyValuePopup
Status: planned

**Tests:** (2 in top 10%, ~41ms combined)
- `tests/components/form/RichMarkdownEditor.propertyValuePopup.spec.ts` — "cancel closes popup without emitting property changes" (~23ms)
- `tests/components/form/RichMarkdownEditor.propertyValuePopup.spec.ts` — "hides list mode for scalar-only structural keys" (~17ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyValuePopup.spec.ts
```

---

### Phase 22: links/InsertWikiLink
Status: planned

**Tests:** (2 in top 10%, ~38ms combined)
- `tests/links/InsertWikiLink.spec.ts` — "calls the wiki-property inserter when Add wiki link as a new property is clicked" (~21ms)
- `tests/links/InsertWikiLink.spec.ts` — "does not call the inserter when Add a new relationship note is clicked" (~17ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/links/InsertWikiLink.spec.ts
```

---

### Phase 23: recall/Quiz
Status: planned

**Tests:** (2 in top 10%, ~38ms combined)
- `tests/recall/Quiz.spec.ts` — "should show ContentLoader, not JustReview, when navigating to a memory tracker that previously failed" (~20ms)
- `tests/recall/Quiz.spec.ts` — "fetch the first 1 question when mount" (~18ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/recall/Quiz.spec.ts
```

---

### Phase 24: components/admin/FailureReportList
Status: planned

**Tests:** (2 in top 10%, ~33ms combined)
- `tests/components/admin/FailureReportList.spec.ts` — "closes modal when cancel is clicked" (~17ms)
- `tests/components/admin/FailureReportList.spec.ts` — "deletes selected reports when confirmed" (~16ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/admin/FailureReportList.spec.ts
```

---

### Phase 25: components/recall/NoteRefinement.exportExtractRequest
Status: planned

**Tests:** (2 in top 10%, ~33ms combined)
- `tests/components/recall/NoteRefinement.exportExtractRequest.spec.ts` — "opens export dialog with extract request JSON for the selection" (~17ms)
- `tests/components/recall/NoteRefinement.exportExtractRequest.spec.ts` — "does not show export button on the extraction preview screen" (~16ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.exportExtractRequest.spec.ts
```

---

### Phase 26: pages/NotebookPage
Status: planned

**Tests:** (1 in top 10%, ~24ms combined)
- `tests/pages/NotebookPage.spec.ts` — "saves notebook index content directly to container on save" (~24ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/NotebookPage.spec.ts
```

---

### Phase 27: notes/NoteMoreOptionsForm.deleteNote.relationship
Status: planned

**Tests:** (1 in top 10%, ~21ms combined)
- `tests/notes/NoteMoreOptionsForm.deleteNote.relationship.spec.ts` — "uses the current note id when note prop changes without remount" (~21ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteMoreOptionsForm.deleteNote.relationship.spec.ts
```

---

### Phase 28: components/recall/NoteRefinement.removeLayout.loading
Status: planned

**Tests:** (1 in top 10%, ~21ms combined)
- `tests/components/recall/NoteRefinement.removeLayout.loading.spec.ts` — "hides LoadingModal when remove API fails" (~21ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.removeLayout.loading.spec.ts
```

---

### Phase 29: components/form/RichMarkdownEditor.propertyTouchFocus
Status: planned

**Tests:** (1 in top 10%, ~20ms combined)
- `tests/components/form/RichMarkdownEditor.propertyTouchFocus.spec.ts` — "does not focus primer when pointer is not coarse" (~20ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyTouchFocus.spec.ts
```

---

### Phase 30: components/form/SeamlessTextEditor
Status: planned

**Tests:** (1 in top 10%, ~19ms combined)
- `tests/components/form/SeamlessTextEditor.spec.ts` — "does not handle paste when readonly" (~19ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/SeamlessTextEditor.spec.ts
```

---

### Phase 31: components/recall/AssimilationPanel
Status: planned

**Tests:** (1 in top 10%, ~17ms combined)
- `tests/components/recall/AssimilationPanel.spec.ts` — "closes popup and returns to original state when user closes it" (~17ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/AssimilationPanel.spec.ts
```

---

### Phase 32: components/book-reading/PdfBookViewer.gestureZoom
Status: planned

**Tests:** (1 in top 10%, ~17ms combined)
- `tests/components/book-reading/PdfBookViewer.gestureZoom.spec.ts` — "meta+wheel on the viewer prevents default and updates pdf scale" (~17ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/book-reading/PdfBookViewer.gestureZoom.spec.ts
```

---

### Phase 33: components/admin/QuestionGenerationBatchStatus
Status: planned

**Tests:** (1 in top 10%, ~17ms combined)
- `tests/components/admin/QuestionGenerationBatchStatus.spec.ts` — "disables the manual generation button while submitting" (~17ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/admin/QuestionGenerationBatchStatus.spec.ts
```

---

### Phase 34: notes/TextContentWrapper
Status: planned

**Tests:** (1 in top 10%, ~16ms combined)
- `tests/notes/TextContentWrapper.spec.ts` — "keeps the draft when choosing a save option (click does not discard before save)" (~16ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/TextContentWrapper.spec.ts
```

---

### Phase 35: components/recall/NoteRefinement.removeLayout
Status: planned

**Tests:** (1 in top 10%, ~16ms combined)
- `tests/components/recall/NoteRefinement.removeLayout.spec.ts` — "shows confirmation dialog when remove button is clicked" (~16ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.removeLayout.spec.ts
```

---

### Phase 36: toolbars/NoteUndoButton
Status: planned

**Tests:** (1 in top 10%, ~16ms combined)
- `tests/toolbars/NoteUndoButton.spec.ts` — "discards edit content item and shows next item" (~16ms)

**Goals:**
- Remove/merge redundant cases; hoist shared mount/setup.
- Prefer `data-testid` / `getByText` / `querySelector` over `getByRole` / `findByRole`.
- Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`, `nextTick`, or fake timers.
- If no meaningful win after a serious attempt: propose under Candidates in blacklist.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars/NoteUndoButton.spec.ts
```

---

### Phase 37: Re-profile and close
Status: planned

Re-run:

```bash
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run --reporter=json
```

| Metric | Before | After |
|--------|--------|-------|
| Test count | 1574 | |
| Suite wall | ~61s | |
| Top 10% total time (n=158) | ~5086ms | |

**Candidates proposed this run:** (none / list)

Archive summary to `ongoing/archive/frontend-test-optimization-history.md`, delete
this working plan, keep the blacklist. Do not commit profile JSON.

**Commits:**
