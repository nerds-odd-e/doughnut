# Frontend unit test optimization

Status: in-progress

**Execution:** run via **execute-plan** (commit + push per slice).
**Cloud VM:** no Nix prefix; use `pnpm frontend:test <path>` to verify (browser Chromium). Profile used `CI=true pnpm -C frontend exec vitest run --browser=chromium --reporter=json`.

## Profiling baseline (2026-08-20)

Command:

```bash
CI=true pnpm -C frontend exec vitest run --browser=chromium --reporter=json
```

- **1785 tests**, suite wall ~**138s** (file start→end); assertion CPU sum ~**9.5s**
- Eligible: **1785** (all profiled frontend unit tests)
- Raw profile: `.planning/quick/frontend-profile-results.json` — **do not commit**
- Top 10% assertion CPU sum: **3722ms**

### Top 10% slowest (n = ceil(1785 × 0.10) = 179)

| # | ms | file / spec | test / scenario |
|---|-----|-------------|-----------------|
| 1 | 47.4 | `tests/pages/NoteShowPageConversation.spec.ts` | note show page conversation maximizes and restores note content when maximize is toggled |
| 2 | 42.5 | `tests/pages/FolderPage.moveDestination.spec.ts` | FolderPage move destinations move retries cross-notebook folder move with merge after 409 conflict |
| 3 | 38.8 | `tests/pages/FolderPage.moveDestination.spec.ts` | FolderPage move destinations move sends destinationNotebookId and newParentFolderId for cross-notebook folder move |
| 4 | 38.7 | `tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` | AnsweredSpellingQuestion accidental match builds a link as a same-Modal step and returns to the match list after success |
| 5 | 38.1 | `tests/components/commons/LoadingModal.topLayer.spec.ts` | LoadingModal top layer paints the spinner above an already-open native modal dialog |
| 6 | 35.1 | `tests/notes/TextContentWrapper.spec.ts` | TextContentWrapper referenced title rename discards dirty title and hides save actions when focus leaves the wrapper |
| 7 | 34.8 | `tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts` | RichMarkdownEditor property relation and index relation property in rich mode commits custom relationship text from the dialog and emits updated frontmatter |
| 8 | 33.2 | `tests/components/book-reading/PdfBookViewer.gestureZoom.spec.ts` | PdfBookViewer gesture zoom (mocked pdf.js) 'meta'+wheel on the viewer prevents default and updates pdf scale |
| 9 | 32.6 | `tests/pages/NoteShowPageConversation.spec.ts` | note show page conversation restores note content and clears conversation query on close |
| 10 | 31.1 | `tests/components/recall/NoteRefinement.removeLayout.loading.spec.ts` | NoteRefinement remove layout loading modal shows LoadingModal while removing refinement layout items and hides on success or failure |
| 11 | 30.2 | `tests/components/book-reading/PdfBookViewer.gestureZoom.spec.ts` | PdfBookViewer gesture zoom (mocked pdf.js) 'ctrl'+wheel on the viewer prevents default and updates pdf scale |
| 12 | 30.1 | `tests/pages/NoteShowPageAssimilationPanel.spec.ts` | note show page inline assimilation panel keeps assimilation settings in the shared toolbar panel when sidebar is open |
| 13 | 29.9 | `tests/notes/TextContentWrapper.spec.ts` | TextContentWrapper referenced title rename does not discard when focusout has a misleading relatedTarget but focus remains inside the wrapper |
| 14 | 28.3 | `tests/notes/NoteToolbar.pinnedToggles.spec.ts` | NoteToolbar pinned on-state toggles returns 'assimilation' to the overflow menu when the pinned toolbar toggle is turned off |
| 15 | 28.3 | `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` | RichMarkdownEditor property value popup mode switch rejects empty list items on save |
| 16 | 28.3 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | NoteRefinement extract note preview replaces preview fields when Ask AI to retry is clicked |
| 17 | 28.3 | `tests/notes/NoteToolbar.pinnedToggles.spec.ts` | NoteToolbar pinned on-state toggles returns 'audio' to the overflow menu when the pinned toolbar toggle is turned off |
| 18 | 27.8 | `tests/wiki-link-or-relationship/AddRelationship.spec.ts` | AddRelationshipFinalize emits success without navigating when navigateOnSuccess is false |
| 19 | 27.4 | `tests/components/commons/LoadingModal.spec.ts` | LoadingModal keeps a fitting long-message stack centered and a narrow one scrollable |
| 20 | 27.4 | `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` | RichMarkdownEditor property value popup mode switch allows duplicate list items in popup save |
| 21 | 27.1 | `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` | RichMarkdownEditor property value popup mode switch saves an empty list from popup |
| 22 | 26.7 | `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` | NoteRefinement layout selection submits only checked descendants when parent is indeterminate ('remove') |
| 23 | 26.6 | `tests/notes/NoteNewForm.wikidata.spec.ts` | NoteNewForm wikidata and soft-delete search wikidata entry search 'dog' get 'Canine' with action 'append' updates title as 'dog' |
| 24 | 26.6 | `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` | RichMarkdownEditor property key presets preset dropdown for 'insert row' shows options and sets key on selection |
| 25 | 26.1 | `tests/notes/NoteNewForm.wikidata.spec.ts` | NoteNewForm wikidata and soft-delete search wikidata entry search 'dog' get 'Canine' with action 'replace' updates title as 'Canine' |
| 26 | 25.9 | `tests/notes/NoteToolbar.moreOptionsOverflow.spec.ts` | NoteToolbar more-options overflow emits edit-as-markdown from the overflow Edit row |
| 27 | 25.5 | `tests/notes/NoteToolbar.conversationWikiNewOverflow.spec.ts` | NoteToolbar Conversation, Wiki, and New overflow keeps only the on-toggle and more options on an extremely narrow bar |
| 28 | 25.4 | `tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts` | RichMarkdownEditor overlaps property inserts the first overlap as a list when adding a new overlaps property |
| 29 | 25.1 | `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` | RichMarkdownEditor property value popup mode switch saves list as scalar when user switches to text mode in popup |
| 30 | 25.0 | `tests/notes/NoteMoreOptionsForm.deleteNote.relationship.spec.ts` | NoteMoreOptionsForm delete relationship note uses the current note id when note prop changes without remount |
| 31 | 24.8 | `tests/pages/NoteShowPage.spec.ts` | note show page loads note by id from route |
| 32 | 24.8 | `tests/wiki-link-or-relationship/SearchDialog.actions.spec.ts` | SearchForm actions Use this note choice step shows link choice buttons and relationship form when Add a new relationship note is clicked |
| 33 | 24.8 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | NoteRefinement extract note preview confirms retry when preview fields were edited, keeping edits on cancel and replacing on confirm |
| 34 | 24.7 | `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` | RichMarkdownEditor property value popup mode switch saves scalar as list when user switches to list mode in popup |
| 35 | 24.6 | `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` | NoteRefinement layout selection includes parent id when all descendants are selected again |
| 36 | 24.5 | `tests/notes/NoteToolbar.conversationWikiNewOverflow.spec.ts` | NoteToolbar Conversation, Wiki, and New overflow still opens new note when New is in more options |
| 37 | 24.3 | `tests/components/form/RichMarkdownEditor.properties.spec.ts` | RichMarkdownEditor properties shows read-only Properties above Quill when content includes supported YAML frontmatter |
| 38 | 24.2 | `tests/notes/NoteToolbar.pinnedToggles.spec.ts` | NoteToolbar pinned on-state toggles pins 'assimilation' on a narrow toolbar and omits it from overflow |
| 39 | 24.2 | `tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts` | RichMarkdownEditor overlaps property emits valid overlaps list edits from popup |
| 40 | 24.2 | `tests/components/form/RichMarkdownEditor.propertyValuePopupReorder.spec.ts` | RichMarkdownEditor property value popup reorder preserves reordered list items in composed YAML when saved from popup |
| 41 | 24.1 | `tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts` | RichMarkdownEditor aliases property emits valid aliases list edits from popup |
| 42 | 23.8 | `tests/notes/NoteToolbar.pinnedToggles.spec.ts` | NoteToolbar pinned on-state toggles pins 'audio' on a narrow toolbar and omits it from overflow |
| 43 | 23.5 | `tests/pages/NoteShowPageAssimilationPanel.spec.ts` | note show page inline assimilation panel renders assimilate button when assimilation settings are on |
| 44 | 23.3 | `tests/components/form/RichMarkdownEditor.propertyKeyPresets.listAppend.spec.ts` | RichMarkdownEditor list-capable preset append appends another value to exact list-capable key as a list item |
| 45 | 23.2 | `tests/components/recall/NoteRefinement.removeLayout.spec.ts` | NoteRefinement remove refinement layout items selection and confirmation calls API and emits contentUpdated when removal is confirmed |
| 46 | 23.1 | `tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts` | RichMarkdownEditor overlaps property shows overlaps constraint for 'scalar text in popup' |
| 47 | 23.1 | `tests/wiki-link-or-relationship/AddRelationship.spec.ts` | AddRelationshipFinalize shows LoadingModal while creating relationship note |
| 48 | 23.0 | `tests/wiki-link-or-relationship/AddRelationship.spec.ts` | AddRelationshipFinalize shows placement options with relations subfolder selected by default |
| 49 | 22.9 | `tests/wiki-link-or-relationship/AddRelationship.spec.ts` | AddRelationshipFinalize creates relationship note, navigates, and emits success |
| 50 | 22.9 | `tests/notes/NoteEditableContent.spec.ts` | NoteEditableContent should preserve second edit when first save response arrives after second edit |
| 51 | 22.9 | `tests/notes/NoteTextContent.titleEdit.spec.ts` | NoteTextContent title edit when save fails with a binding error clears the error after a successful edit |
| 52 | 22.8 | `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` | RichMarkdownEditor property value popup mode switch seeds text mode from populated list when switching from list mode |
| 53 | 22.7 | `tests/notes/NoteTextContent.titleEdit.spec.ts` | NoteTextContent title edit keeps newer local edits when API returns an older title |
| 54 | 22.7 | `tests/pages/FolderPage.moveDestination.spec.ts` | FolderPage move destinations move sends destinationNotebookId and navigates after cross-notebook root move |
| 55 | 22.6 | `tests/components/form/RichMarkdownEditor.propertyValuePopup.spec.ts` | RichMarkdownEditor property value popup saves edited scalar value from popup without changing YAML shape to a list |
| 56 | 22.6 | `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` | NoteRefinement layout selection removes non-contiguous selected refinement layout items |
| 57 | 22.5 | `tests/components/recall/NoteRefinement.removeLayout.loading.spec.ts` | NoteRefinement remove layout loading modal keeps remove continuous blocker noncancelable while nested layout regenerates |
| 58 | 22.3 | `tests/components/recall/RefineNoteModal.extractNote.close.spec.ts` | RefineNoteModal extract note close closes the refine note modal after creating a note from extraction preview |
| 59 | 21.8 | `tests/wiki-link-or-relationship/InsertWikiLink.spec.ts` | InsertWikiLink calls the insert-wiki-link-as-property inserter when Add wiki link as a new property is clicked |
| 60 | 21.6 | `tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts` | RichMarkdownEditor aliases property shows alias constraint for 'scalar text in popup' |
| 61 | 21.6 | `tests/components/form/RichMarkdownEditor.propertyValuePopupReorder.spec.ts` | RichMarkdownEditor property value popup reorder disables move up on first item and move down on last item |
| 62 | 21.5 | `tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts` | RichMarkdownEditor aliases property inserts the first alias as a list when adding a new aliases property |
| 63 | 21.4 | `tests/notes/NoteNewForm.wikidata.spec.ts` | NoteNewForm wikidata and soft-delete search wikidata entry search 'dog' get 'dog' with action undefined updates title as 'dog' |
| 64 | 21.4 | `tests/notes/WikidataAssociationDialog.titleActions.spec.ts` | WikidataAssociationDialog title actions and save soft keyboard primer transfers focus to wikidata ID input after mount when showSaveButton |
| 65 | 21.2 | `tests/notes/sidebar/SidebarFirstGeneration.spec.ts` | Sidebar first generation should scroll to active note |
| 66 | 21.1 | `tests/wiki-link-or-relationship/InsertWikiLink.spec.ts` | InsertWikiLink does not call the inserter when Add a new relationship note is clicked |
| 67 | 21.0 | `tests/notes/NoteToolbar.moreOptions.spec.ts` | NoteToolbar more options copies export markdown while keeping the export dialog open |
| 68 | 20.9 | `tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts` | RichMarkdownEditor property relation and index relation property in rich mode opens dialog with custom text prefilled for an unknown relation |
| 69 | 20.8 | `tests/notes/NoteNewForm.wikidata.spec.ts` | NoteNewForm wikidata and soft-delete search wikidata entry opens wikidata dialog on search and closes on cancel |
| 70 | 20.7 | `tests/notes/NoteTextContent.titleEdit.spec.ts` | NoteTextContent title edit displays an editable title by default |
| 71 | 20.7 | `tests/components/form/SeamlessTextEditor.spec.ts` | SeamlessTextEditor keeps caret offset when modelValue is synced with same-length text |
| 72 | 20.6 | `tests/components/recall/NoteRefinement.extractNote.create.spec.ts` | NoteRefinement extract note create shows create errors in the preview |
| 73 | 20.6 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | NoteRefinement extract note preview shows inline error when retry preview API fails |
| 74 | 20.5 | `tests/components/recall/NoteRefinement.extractNote.loading.spec.ts` | NoteRefinement extract note loading shows LoadingModal while creating note from preview |
| 75 | 20.5 | `tests/components/recall/NoteRefinement.extractNote.loading.spec.ts` | NoteRefinement extract note loading shows LoadingModal while retrying extract preview |
| 76 | 20.4 | `tests/notes/NoteMoreOptionsForm.deleteNote.relationship.spec.ts` | NoteMoreOptionsForm delete relationship note uses confirm when relationship note source does not resolve |
| 77 | 20.4 | `tests/components/recall/NoteRefinement.extractionPreview.cancel.edges.spec.ts` | NoteRefinement extraction preview cancel edges create-note pending shows creating message without Cancel |
| 78 | 20.3 | `tests/pages/BookReadingPage.snap.spec.ts` | BookReadingPage snap snaps back and keeps panel visible on first boundary crossing (same-page: scrolls to block start) |
| 79 | 20.3 | `tests/wiki-link-or-relationship/SearchDialog.actions.spec.ts` | SearchForm actions Move Under folder hit shows confirm when move is blocked by soft-deleted title at destination |
| 80 | 20.3 | `tests/wiki-link-or-relationship/SearchDialog.deadWikiLink.spec.ts` | SearchForm dead wiki link actions Dead link - link to existing note rewrites path Markdown dead link '/Folder/Missing' keeping Markdown spelling |
| 81 | 20.3 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | NoteRefinement extract note preview returns to the layout when Back is clicked |
| 82 | 20.2 | `tests/wiki-link-or-relationship/SearchDialog.deadWikiLink.spec.ts` | SearchForm dead wiki link actions Dead link - link to existing note rewrites every matching path Markdown dead link token |
| 83 | 20.0 | `tests/notes/NoteNewForm.wikidata.spec.ts` | NoteNewForm wikidata and soft-delete search wikidata entry search 'dog' get 'Dog' with action undefined updates title as 'Dog' |
| 84 | 20.0 | `tests/wiki-link-or-relationship/SearchDialog.actions.spec.ts` | SearchForm actions Move to notebook root on NOTEBOOK hit calls moveNoteToNotebookRootInNotebook with notebook id after confirm |
| 85 | 19.9 | `tests/wiki-link-or-relationship/SearchDialog.deadWikiLink.spec.ts` | SearchForm dead wiki link actions Dead link - link to existing note rewrites path Markdown dead link '/Folder/Missing.md' keeping Markdown spelling |
| 86 | 19.9 | `tests/components/recall/NoteRefinement.extractNote.create.spec.ts` | NoteRefinement extract note create toggles Create note disabled state from new note title |
| 87 | 19.8 | `tests/wiki-link-or-relationship/SearchDialog.searchKeyHistory.spec.ts` | SearchForm search key history collapses search key history inside a modal when clicking elsewhere in that modal |
| 88 | 19.8 | `tests/notes/FolderSelector.spec.ts` | FolderSelector soft keyboard primer transfers focus to search input after folder index loads |
| 89 | 19.7 | `tests/notes/NoteMoreOptionsForm.deleteNote.spec.ts` | NoteMoreOptionsForm delete note does not call deleteNote when confirmation is cancelled |
| 90 | 19.7 | `tests/components/recall/AssimilationPanel.loadingModal.spec.ts` | AssimilationPanel loading modal keeps the global modal open from assimilate through loading the next unit |
| 91 | 19.5 | `tests/notes/NoteToolbar.assimilationPanel.spec.ts` | NoteToolbar assimilation panel hides assimilation when audio opens and vice versa |
| 92 | 19.4 | `tests/components/form/RichMarkdownEditor.propertyValuePopupReorder.spec.ts` | RichMarkdownEditor property value popup reorder reorders duplicate list items as distinct rows in popup |
| 93 | 19.2 | `tests/pages/NotebookCatalogList.spec.ts` | catalog list returns to title A–Z after title Z–A |
| 94 | 19.2 | `tests/components/recall/NoteRefinement.extractionPreview.cancel.spec.ts` | NoteRefinement extraction preview cancel cancels Ask AI to retry without wiping prior preview |
| 95 | 19.1 | `tests/notes/NoteToolbar.conversationWikiNewOverflow.spec.ts` | NoteToolbar Conversation, Wiki, and New overflow still opens wiki search when Wiki is in more options |
| 96 | 18.9 | `tests/notes/NoteNewForm.spec.ts` | adding new note selects all text when the default Untitled title is shown |
| 97 | 18.8 | `tests/components/form/RichMarkdownEditor.propertyValuePopup.spec.ts` | RichMarkdownEditor property value popup cancel closes popup without emitting property changes |
| 98 | 18.6 | `tests/components/recall/NoteRefinement.removeLayout.spec.ts` | NoteRefinement remove refinement layout items selection and confirmation does not save or emit contentUpdated when removal returns unchanged content |
| 99 | 18.5 | `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` | RichMarkdownEditor property key presets preset dropdown for 'occupied url preset' shows options and sets key on selection |
| 100 | 18.5 | `tests/components/form/TextInput.spec.ts` | TextInput.vue does not select text when initialSelectAll is false |
| 101 | 18.4 | `tests/components/form/RichMarkdownEditor.propertyValuePopup.spec.ts` | RichMarkdownEditor property value popup 'shows value edit icon on list propert…' |
| 102 | 18.4 | `tests/components/recallStats/recallStatsTheme.spec.ts` | recall stats charts use theme tokens (dark-mode safe) calendar empty and filled cells are not hardcoded GitHub hex and adapt to dark theme |
| 103 | 18.3 | `tests/notes/NoteDeadWikiLinkCreateModal.spec.ts` | NoteDeadWikiLinkCreateModal soft keyboard primer transfers focus to note title after create form mounts |
| 104 | 18.2 | `tests/wiki-link-or-relationship/SearchDialog.spec.ts` | SearchForm Matches / Recent list mode keeps search key and switches between Matches and Recent |
| 105 | 18.2 | `tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` | AnsweredSpellingQuestion accidental match omits mutating CTAs when reviewed notebook is readonly |
| 106 | 18.2 | `tests/components/recall/NoteRefinement.removeLayout.spec.ts` | NoteRefinement remove refinement layout items selection and confirmation clears selection and reloads layout after confirmed removal |
| 107 | 18.0 | `tests/pages/NoteShowPageConversation.spec.ts` | note show page conversation opens conversation when URL has conversation=true |
| 108 | 18.0 | `tests/components/recall/NoteRefinement.extractNote.loading.spec.ts` | NoteRefinement extract note loading shows LoadingModal during extract preview and hides on success or failure |
| 109 | 17.9 | `tests/notes/NoteNewForm.parentRelationship.spec.ts` | NoteNewForm parent relationship submits parent frontmatter when Under current is selected |
| 110 | 17.8 | `tests/notes/NoteToolbar.conversationWikiNewOverflow.spec.ts` | NoteToolbar Conversation, Wiki, and New overflow moves Conversation into more options before Wiki or New |
| 111 | 17.6 | `tests/pages/BookReadingPage.snap.budgets.spec.ts` | BookReadingPage snap budgets marking READ clears snap reminder: block no longer snaps when re-visited |
| 112 | 17.6 | `tests/notes/WikidataAssociationDialog.titleActions.spec.ts` | WikidataAssociationDialog title actions and save emits selected with replace action when showSaveButton is true |
| 113 | 17.6 | `tests/components/recall/AssimilationPanel.loadingModal.spec.ts` | AssimilationPanel loading modal hides global modal when assimilate API returns an error |
| 114 | 17.5 | `tests/components/form/RichMarkdownEditor.properties.spec.ts` | RichMarkdownEditor properties editing an existing property row emits renamed keys and updated values |
| 115 | 17.4 | `tests/notes/WikidataAssociationDialog.titleActions.spec.ts` | WikidataAssociationDialog title actions and save emits selected with replace action when showSaveButton is false |
| 116 | 17.4 | `tests/notes/WikidataAssociationDialog.titleActions.spec.ts` | WikidataAssociationDialog title actions and save emits selected with add alias action when showSaveButton is true |
| 117 | 17.4 | `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` | RichMarkdownEditor property key presets preset dropdown for 'existing row' shows options and sets key on selection |
| 118 | 17.3 | `tests/notes/NoteDeadWikiLinkCreateModal.spec.ts` | NoteDeadWikiLinkCreateModal soft keyboard primer transfers focus to search input after point-at-existing form mounts |
| 119 | 17.2 | `tests/notes/FolderSelector.spec.ts` | FolderSelector soft keyboard primer does not focus primer when pointer is not coarse |
| 120 | 17.1 | `tests/pages/FolderPage.renameDissolve.spec.ts` | FolderPage rename and dissolve dissolve shows merge confirm when dissolve returns 409 and retries with merge=true |
| 121 | 17.1 | `tests/notes/NoteNewForm.spec.ts` | adding new note places the caret after a trailing space when initialTitle comes from a template |
| 122 | 17.1 | `tests/notes/sidebar/SidebarFolderItem.spec.ts` | SidebarFolderItem scrolls folder row into view when active folder row is not intersecting |
| 123 | 17.1 | `tests/components/recall/AnsweredQuestionComponent.spec.ts` | AnsweredQuestionComponent refine note passes MCQ context when opening Refine note |
| 124 | 17.0 | `tests/notes/WikidataAssociationDialog.titleActions.spec.ts` | WikidataAssociationDialog title actions and save emits selected with add alias action when showSaveButton is false |
| 125 | 16.9 | `tests/components/form/RichMarkdownEditor.propertyTouchFocus.spec.ts` | RichMarkdownEditor property touch focus does not focus primer when pointer is not coarse |
| 126 | 16.9 | `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` | NoteRefinement layout selection submits only checked descendants when parent is indeterminate ('extract') |
| 127 | 16.8 | `tests/components/book-reading/PdfBookViewer.gestureZoom.spec.ts` | PdfBookViewer gesture zoom (mocked pdf.js) wheel without ctrl/meta does not cancel (no browser-zoom block path) |
| 128 | 16.8 | `tests/notes/NoteNewForm.wikidata.spec.ts` | NoteNewForm wikidata and soft-delete submit errors displays reserved title error when api returns binding error for newTitle |
| 129 | 16.8 | `tests/notes/WikidataAssociationDialog.titleActions.spec.ts` | WikidataAssociationDialog title actions and save defers selected until Save when showSaveButton is true |
| 130 | 16.8 | `tests/notes/WikidataAssociationDialog.titleActions.spec.ts` | WikidataAssociationDialog title actions and save enables Save and emits empty string when clearing with canSaveEmptyToClear |
| 131 | 16.8 | `tests/commons/Modal.spec.ts` | Modal prefers text controls inside a marked autofocus container |
| 132 | 16.8 | `tests/components/book-reading/PdfBookViewer.gestureZoom.spec.ts` | PdfBookViewer gesture zoom (mocked pdf.js) two-finger pinch touchmove updates scale around the midpoint |
| 133 | 16.8 | `tests/notes/sidebar/SidebarPeerSort.spec.ts` | Sidebar peer sort keeps Title (Z–A) on a later visit after the tab session is gone |
| 134 | 16.7 | `tests/notes/NoteEditableContent.relationProperty.spec.ts` | NoteEditableContent relation property row in rich mode shows relation type picker when noteContent includes relation frontmatter |
| 135 | 16.6 | `tests/notes/NoteMoreOptionsForm.deleteNote.relationship.spec.ts` | NoteMoreOptionsForm delete relationship note offers reduce-to-property when deleting a qualifying relationship note |
| 136 | 16.6 | `tests/notes/sidebar/SidebarFolderItem.spec.ts` | SidebarFolderItem does not scroll folder row when active folder row is already intersecting |
| 137 | 16.6 | `tests/notes/sidebar/SidebarRouteNavigation.spec.ts` | Sidebar route navigation: sticky realm during uncached note load keeps sidebar chrome when navigating to an uncached note in the same notebook |
| 138 | 16.6 | `tests/notes/NoteDeadWikiLinkCreateModal.spec.ts` | NoteDeadWikiLinkCreateModal soft keyboard primer does not focus primer on create tap when pointer is not coarse |
| 139 | 16.6 | `tests/pages/NoteShowPageAssimilationPanel.spec.ts` | note show page inline assimilation panel does not render assimilation panel when settings are off |
| 140 | 16.5 | `tests/pages/FolderPage.moveConflict.spec.ts` | FolderPage move conflicts move shows error message when move 409 and user cancels merge |
| 141 | 16.5 | `tests/pages/FolderPage.renameDissolve.spec.ts` | FolderPage rename and dissolve dissolve shows inline error when dissolve returns soft-deleted title conflict |
| 142 | 16.5 | `tests/pages/RecallPageOverlap.spec.ts` | overlap try-again stay and retry stays on the same tracker, skips threshold, and remounts spelling on Try again |
| 143 | 16.5 | `tests/components/form/RichMarkdownEditor.propertyTouchFocus.spec.ts` | RichMarkdownEditor property touch focus transfers focus to property key after insert form mounts with 'existing rows' |
| 144 | 16.4 | `tests/notes/NoteToolbar.moreOptionsOverflow.spec.ts` | NoteToolbar more-options overflow moves Edit into more options when the bar is tighter than Export overflow |
| 145 | 16.4 | `tests/components/recall/AssimilationPanel.property.spec.ts` | AssimilationPanel property assimilation removes a property tracker from recall and shows Revive |
| 146 | 16.3 | `tests/notes/sidebar/SidebarFirstGeneration.spec.ts` | Sidebar first generation should not scroll if already visible |
| 147 | 16.2 | `tests/components/form/RichMarkdownEditor.propertyValuePopup.spec.ts` | RichMarkdownEditor property value popup hides list mode for scalar-only structural keys |
| 148 | 16.2 | `tests/components/recall/AssimilationPanel.property.spec.ts` | AssimilationPanel property assimilation advances to the next unit and reloads note info when assimilating a property |
| 149 | 16.1 | `tests/notes/NoteTextContent.titleEdit.saveRace.spec.ts` | NoteTextContent title edit save race saves the last title after an earlier in-flight save finishes |
| 150 | 16.1 | `tests/components/recall/CommissionLearningSessionDialog.spec.ts` | CommissionLearningSessionDialog keeps report textarea when record fails |
| 151 | 16.1 | `tests/wiki-link-or-relationship/SearchDialog.searchKeyHistory.spec.ts` | SearchForm search key history collapses search key history when 'clicking a search scope toggle' |
| 152 | 16.0 | `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` | RichMarkdownEditor property key presets preset dropdown for 'occupied image preset' shows options and sets key on selection |
| 153 | 15.9 | `tests/wiki-link-or-relationship/SearchDialog.searchKeyHistory.spec.ts` | SearchForm search key history collapses search key history when 'clicking the search input' |
| 154 | 15.9 | `tests/notes/NoteToolbar.moreOptionsOverflow.spec.ts` | NoteToolbar more-options overflow still toggles edit mode with m when Edit is in more options |
| 155 | 15.8 | `tests/notes/NoteToolbar.assimilationPanel.spec.ts` | NoteToolbar assimilation panel shows assimilation settings in the shared panel shell when opened |
| 156 | 15.7 | `tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts` | RichMarkdownEditor overlaps property shows overlaps constraint for 'plain list item in popup' |
| 157 | 15.7 | `tests/components/recall/AssimilationPanel.property.spec.ts` | AssimilationPanel property assimilation returns a skipped property to the sequence without creating a tracker or reviving |
| 158 | 15.6 | `tests/notes/NoteNewButton.spec.ts` | NoteNewButton keyboard shortcut opens the new-note dialog when n is pressed and the button is mounted |
| 159 | 15.6 | `tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts` | RichMarkdownEditor aliases property shows alias constraint for 'invalid list item in popup' |
| 160 | 15.5 | `tests/pages/MemoryTrackerPageView.deleteUnanswered.spec.ts` | MemoryTrackerPageView delete unanswered confirmation message for 'multiple prompts' |
| 161 | 15.4 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | NoteRefinement extract note preview shows inline error when extract preview API fails |
| 162 | 15.3 | `tests/toolbars/MainMenu.resume.spec.ts` | MainMenu resume recall shows highlighted Resume before Note when recall is paused |
| 163 | 15.3 | `tests/wiki-link-or-relationship/SearchDialog.actions.spec.ts` | SearchForm actions Move Under folder hit calls moveNoteToFolder with folder id after confirm |
| 164 | 15.3 | `tests/notes/NoteEditableContent.paste.spec.ts` | NoteEditableContent paste quill editor shows options popup based on content after paste |
| 165 | 15.2 | `tests/pages/MemoryTrackerPageView.deleteUnanswered.spec.ts` | MemoryTrackerPageView delete unanswered confirmation message for 'contested prompts excluded from count' |
| 166 | 15.2 | `tests/components/recall/NoteRefinement.exportExtractRequest.spec.ts` | NoteRefinement export extract request does not show export button on the extraction preview screen |
| 167 | 15.1 | `tests/notes/sidebar/SidebarAncestorLoading.spec.ts` | Sidebar gradual ancestor population loads ancestor branches for a deep note through folder listings without showNote |
| 168 | 15.0 | `tests/components/notes/NoteTextContentUndo.spec.ts` | undo editing should call addEditingToUndoHistory on submitChange |
| 169 | 14.9 | `tests/notes/FolderSelector.spec.ts` | FolderSelector keyboard navigation moves focus through folder results and back to search input with ArrowDown and ArrowUp |
| 170 | 14.8 | `tests/pages/settings/AccessTokensSettingsTab.spec.ts` | AccessTokensSettingsTab displays "No Label" when token label is empty |
| 171 | 14.8 | `tests/pages/MemoryTrackerPageView.deleteUnanswered.spec.ts` | MemoryTrackerPageView delete unanswered confirmation message for 'single prompt' |
| 172 | 14.8 | `tests/pages/NotebookCatalogExport.spec.ts` | Notebook catalog export falls back to the notebook name when Content-Disposition is not printable ASCII |
| 173 | 14.8 | `tests/pages/RecallPage.spelling.spec.ts` | RecallPage spelling quiz focuses the spelling answer input when resuming recall |
| 174 | 14.7 | `tests/pages/BookReadingPage.readingPosition.spec.ts` | BookReadingPage reading position PATCH reading position includes selectedBookBlockId after layout click |
| 175 | 14.7 | `tests/notes/NoteToolbar.moreOptions.spec.ts` | NoteToolbar more options toggles the audio tools panel from the overflow menu |
| 176 | 14.7 | `tests/components/admin/FailureReportList.spec.ts` | FailureReportList selecting and deleting reports deletes selected reports when confirmed |
| 177 | 14.6 | `tests/notes/NoteNewForm.spec.ts` | adding new note searches when user edits title back to 'Untitled' |
| 178 | 14.6 | `tests/components/recall/NoteRefinement.extractNote.create.spec.ts` | NoteRefinement extract note create creates a note from the preview and navigates to the new note |
| 179 | 14.6 | `tests/components/recall/NoteRefinement.extractNote.create.spec.ts` | NoteRefinement extract note create creates a note from edited preview fields |

### Grouping

- By file: **81** groups
- Batches of 3: **60** groups
- **Chosen:** batches of 3 (fewer groups)

## Optimization rules

1. Remove or simplify redundant tests first.
2. Strictly no fixed-time waits.
3. Flaky = failure.

Frontend tactics: avoid `ByRole` (already rare), prefer `data-testid` / `getByText` / querySelector; replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises` / `nextTick` / fake timers; merge overlapping scenarios; hoist shared mount/setup; narrower fixtures.

Hard-to-improve tests: propose under **Candidates** in
`ongoing/test-optimization-blacklist.md`. Permanent skip (developer Jidoka only):
not applicable to Vitest unit tests (E2E tag only).

---

### Optimize batch 1 (ranks 1–3)
Type: Structure
Status: done

**Done:** Merged redundant FolderPage cross-notebook move case into 409-merge test; lighter NoteShow maximize mount (no sidebar); shared helpers. 6 tests green ×3.

---

### Optimize batch 2 (ranks 4–6)
Type: Structure
Status: done

**Done:** Slim AccidentalMatch fixture; single-host LoadingModal.topLayer; fake rAF for TextContentWrapper discard. 12 tests green ×3.

---

### Optimize batch 3 (ranks 7–9)
Type: Structure
Status: done

**Done:** Merged relation open+commit; merged PDF ctrl+meta zoom into one mount + fake rAF; dropped waitFor on conversation close. 13 tests green ×3.

---

### Optimize batch 4 (ranks 10–12)
Type: Structure
Status: planned

**Tests:**
- `tests/components/recall/NoteRefinement.removeLayout.loading.spec.ts` — "NoteRefinement remove layout loading modal shows LoadingModal while removing refinement layout items and hides on success or failure" (~31ms)
- `tests/pages/NoteShowPageAssimilationPanel.spec.ts` — "note show page inline assimilation panel keeps assimilation settings in the shared toolbar panel when sidebar is open" (~30ms)

**Note:** PDF ctrl+wheel already optimized via batch 3 merge — skip that rank.

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/recall/NoteRefinement.removeLayout.loading.spec.ts tests/pages/NoteShowPageAssimilationPanel.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 5 (ranks 13–15)
Type: Structure
Status: planned

**Tests:**
- `tests/notes/TextContentWrapper.spec.ts` — "TextContentWrapper referenced title rename does not discard when focusout has a misleading relatedTarget but focus remains inside the wrapper" (~30ms)
- `tests/notes/NoteToolbar.pinnedToggles.spec.ts` — "NoteToolbar pinned on-state toggles returns 'assimilation' to the overflow menu when the pinned toolbar toggle is turned off" (~28ms)
- `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` — "RichMarkdownEditor property value popup mode switch rejects empty list items on save" (~28ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/notes/TextContentWrapper.spec.ts tests/notes/NoteToolbar.pinnedToggles.spec.ts tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 6 (ranks 16–18)
Type: Structure
Status: planned

**Tests:**
- `tests/components/recall/NoteRefinement.extractNote.spec.ts` — "NoteRefinement extract note preview replaces preview fields when Ask AI to retry is clicked" (~28ms)
- `tests/notes/NoteToolbar.pinnedToggles.spec.ts` — "NoteToolbar pinned on-state toggles returns 'audio' to the overflow menu when the pinned toolbar toggle is turned off" (~28ms)
- `tests/wiki-link-or-relationship/AddRelationship.spec.ts` — "AddRelationshipFinalize emits success without navigating when navigateOnSuccess is false" (~28ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/recall/NoteRefinement.extractNote.spec.ts tests/notes/NoteToolbar.pinnedToggles.spec.ts tests/wiki-link-or-relationship/AddRelationship.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 7 (ranks 19–21)
Type: Structure
Status: planned

**Tests:**
- `tests/components/commons/LoadingModal.spec.ts` — "LoadingModal keeps a fitting long-message stack centered and a narrow one scrollable" (~27ms)
- `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` — "RichMarkdownEditor property value popup mode switch allows duplicate list items in popup save" (~27ms)
- `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` — "RichMarkdownEditor property value popup mode switch saves an empty list from popup" (~27ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/commons/LoadingModal.spec.ts tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 8 (ranks 22–24)
Type: Structure
Status: planned

**Tests:**
- `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` — "NoteRefinement layout selection submits only checked descendants when parent is indeterminate ('remove')" (~27ms)
- `tests/notes/NoteNewForm.wikidata.spec.ts` — "NoteNewForm wikidata and soft-delete search wikidata entry search 'dog' get 'Canine' with action 'append' updates title as 'dog'" (~27ms)
- `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` — "RichMarkdownEditor property key presets preset dropdown for 'insert row' shows options and sets key on selection" (~27ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/recall/NoteRefinement.layoutSelection.spec.ts tests/notes/NoteNewForm.wikidata.spec.ts tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 9 (ranks 25–27)
Type: Structure
Status: planned

**Tests:**
- `tests/notes/NoteNewForm.wikidata.spec.ts` — "NoteNewForm wikidata and soft-delete search wikidata entry search 'dog' get 'Canine' with action 'replace' updates title as 'Canine'" (~26ms)
- `tests/notes/NoteToolbar.moreOptionsOverflow.spec.ts` — "NoteToolbar more-options overflow emits edit-as-markdown from the overflow Edit row" (~26ms)
- `tests/notes/NoteToolbar.conversationWikiNewOverflow.spec.ts` — "NoteToolbar Conversation, Wiki, and New overflow keeps only the on-toggle and more options on an extremely narrow bar" (~26ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/notes/NoteNewForm.wikidata.spec.ts tests/notes/NoteToolbar.moreOptionsOverflow.spec.ts tests/notes/NoteToolbar.conversationWikiNewOverflow.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 10 (ranks 28–30)
Type: Structure
Status: planned

**Tests:**
- `tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts` — "RichMarkdownEditor overlaps property inserts the first overlap as a list when adding a new overlaps property" (~25ms)
- `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` — "RichMarkdownEditor property value popup mode switch saves list as scalar when user switches to text mode in popup" (~25ms)
- `tests/notes/NoteMoreOptionsForm.deleteNote.relationship.spec.ts` — "NoteMoreOptionsForm delete relationship note uses the current note id when note prop changes without remount" (~25ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts tests/notes/NoteMoreOptionsForm.deleteNote.relationship.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 11 (ranks 31–33)
Type: Structure
Status: planned

**Tests:**
- `tests/pages/NoteShowPage.spec.ts` — "note show page loads note by id from route" (~25ms)
- `tests/wiki-link-or-relationship/SearchDialog.actions.spec.ts` — "SearchForm actions Use this note choice step shows link choice buttons and relationship form when Add a new relationship note is clicked" (~25ms)
- `tests/components/recall/NoteRefinement.extractNote.spec.ts` — "NoteRefinement extract note preview confirms retry when preview fields were edited, keeping edits on cancel and replacing on confirm" (~25ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/pages/NoteShowPage.spec.ts tests/wiki-link-or-relationship/SearchDialog.actions.spec.ts tests/components/recall/NoteRefinement.extractNote.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 12 (ranks 34–36)
Type: Structure
Status: planned

**Tests:**
- `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` — "RichMarkdownEditor property value popup mode switch saves scalar as list when user switches to list mode in popup" (~25ms)
- `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` — "NoteRefinement layout selection includes parent id when all descendants are selected again" (~25ms)
- `tests/notes/NoteToolbar.conversationWikiNewOverflow.spec.ts` — "NoteToolbar Conversation, Wiki, and New overflow still opens new note when New is in more options" (~25ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts tests/components/recall/NoteRefinement.layoutSelection.spec.ts tests/notes/NoteToolbar.conversationWikiNewOverflow.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 13 (ranks 37–39)
Type: Structure
Status: planned

**Tests:**
- `tests/components/form/RichMarkdownEditor.properties.spec.ts` — "RichMarkdownEditor properties shows read-only Properties above Quill when content includes supported YAML frontmatter" (~24ms)
- `tests/notes/NoteToolbar.pinnedToggles.spec.ts` — "NoteToolbar pinned on-state toggles pins 'assimilation' on a narrow toolbar and omits it from overflow" (~24ms)
- `tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts` — "RichMarkdownEditor overlaps property emits valid overlaps list edits from popup" (~24ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/form/RichMarkdownEditor.properties.spec.ts tests/notes/NoteToolbar.pinnedToggles.spec.ts tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 14 (ranks 40–42)
Type: Structure
Status: planned

**Tests:**
- `tests/components/form/RichMarkdownEditor.propertyValuePopupReorder.spec.ts` — "RichMarkdownEditor property value popup reorder preserves reordered list items in composed YAML when saved from popup" (~24ms)
- `tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts` — "RichMarkdownEditor aliases property emits valid aliases list edits from popup" (~24ms)
- `tests/notes/NoteToolbar.pinnedToggles.spec.ts` — "NoteToolbar pinned on-state toggles pins 'audio' on a narrow toolbar and omits it from overflow" (~24ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyValuePopupReorder.spec.ts tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts tests/notes/NoteToolbar.pinnedToggles.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 15 (ranks 43–45)
Type: Structure
Status: planned

**Tests:**
- `tests/pages/NoteShowPageAssimilationPanel.spec.ts` — "note show page inline assimilation panel renders assimilate button when assimilation settings are on" (~24ms)
- `tests/components/form/RichMarkdownEditor.propertyKeyPresets.listAppend.spec.ts` — "RichMarkdownEditor list-capable preset append appends another value to exact list-capable key as a list item" (~23ms)
- `tests/components/recall/NoteRefinement.removeLayout.spec.ts` — "NoteRefinement remove refinement layout items selection and confirmation calls API and emits contentUpdated when removal is confirmed" (~23ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/pages/NoteShowPageAssimilationPanel.spec.ts tests/components/form/RichMarkdownEditor.propertyKeyPresets.listAppend.spec.ts tests/components/recall/NoteRefinement.removeLayout.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 16 (ranks 46–48)
Type: Structure
Status: planned

**Tests:**
- `tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts` — "RichMarkdownEditor overlaps property shows overlaps constraint for 'scalar text in popup'" (~23ms)
- `tests/wiki-link-or-relationship/AddRelationship.spec.ts` — "AddRelationshipFinalize shows LoadingModal while creating relationship note" (~23ms)
- `tests/wiki-link-or-relationship/AddRelationship.spec.ts` — "AddRelationshipFinalize shows placement options with relations subfolder selected by default" (~23ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts tests/wiki-link-or-relationship/AddRelationship.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 17 (ranks 49–51)
Type: Structure
Status: planned

**Tests:**
- `tests/wiki-link-or-relationship/AddRelationship.spec.ts` — "AddRelationshipFinalize creates relationship note, navigates, and emits success" (~23ms)
- `tests/notes/NoteEditableContent.spec.ts` — "NoteEditableContent should preserve second edit when first save response arrives after second edit" (~23ms)
- `tests/notes/NoteTextContent.titleEdit.spec.ts` — "NoteTextContent title edit when save fails with a binding error clears the error after a successful edit" (~23ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/wiki-link-or-relationship/AddRelationship.spec.ts tests/notes/NoteEditableContent.spec.ts tests/notes/NoteTextContent.titleEdit.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 18 (ranks 52–54)
Type: Structure
Status: planned

**Tests:**
- `tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts` — "RichMarkdownEditor property value popup mode switch seeds text mode from populated list when switching from list mode" (~23ms)
- `tests/notes/NoteTextContent.titleEdit.spec.ts` — "NoteTextContent title edit keeps newer local edits when API returns an older title" (~23ms)
- `tests/pages/FolderPage.moveDestination.spec.ts` — "FolderPage move destinations move sends destinationNotebookId and navigates after cross-notebook root move" (~23ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyValuePopupModeSwitch.spec.ts tests/notes/NoteTextContent.titleEdit.spec.ts tests/pages/FolderPage.moveDestination.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 19 (ranks 55–57)
Type: Structure
Status: planned

**Tests:**
- `tests/components/form/RichMarkdownEditor.propertyValuePopup.spec.ts` — "RichMarkdownEditor property value popup saves edited scalar value from popup without changing YAML shape to a list" (~23ms)
- `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` — "NoteRefinement layout selection removes non-contiguous selected refinement layout items" (~23ms)
- `tests/components/recall/NoteRefinement.removeLayout.loading.spec.ts` — "NoteRefinement remove layout loading modal keeps remove continuous blocker noncancelable while nested layout regenerates" (~23ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyValuePopup.spec.ts tests/components/recall/NoteRefinement.layoutSelection.spec.ts tests/components/recall/NoteRefinement.removeLayout.loading.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 20 (ranks 58–60)
Type: Structure
Status: planned

**Tests:**
- `tests/components/recall/RefineNoteModal.extractNote.close.spec.ts` — "RefineNoteModal extract note close closes the refine note modal after creating a note from extraction preview" (~22ms)
- `tests/wiki-link-or-relationship/InsertWikiLink.spec.ts` — "InsertWikiLink calls the insert-wiki-link-as-property inserter when Add wiki link as a new property is clicked" (~22ms)
- `tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts` — "RichMarkdownEditor aliases property shows alias constraint for 'scalar text in popup'" (~22ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/recall/RefineNoteModal.extractNote.close.spec.ts tests/wiki-link-or-relationship/InsertWikiLink.spec.ts tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 21 (ranks 61–63)
Type: Structure
Status: planned

**Tests:**
- `tests/components/form/RichMarkdownEditor.propertyValuePopupReorder.spec.ts` — "RichMarkdownEditor property value popup reorder disables move up on first item and move down on last item" (~22ms)
- `tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts` — "RichMarkdownEditor aliases property inserts the first alias as a list when adding a new aliases property" (~22ms)
- `tests/notes/NoteNewForm.wikidata.spec.ts` — "NoteNewForm wikidata and soft-delete search wikidata entry search 'dog' get 'dog' with action undefined updates title as 'dog'" (~21ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyValuePopupReorder.spec.ts tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts tests/notes/NoteNewForm.wikidata.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 22 (ranks 64–66)
Type: Structure
Status: planned

**Tests:**
- `tests/notes/WikidataAssociationDialog.titleActions.spec.ts` — "WikidataAssociationDialog title actions and save soft keyboard primer transfers focus to wikidata ID input after mount when showSaveButton" (~21ms)
- `tests/notes/sidebar/SidebarFirstGeneration.spec.ts` — "Sidebar first generation should scroll to active note" (~21ms)
- `tests/wiki-link-or-relationship/InsertWikiLink.spec.ts` — "InsertWikiLink does not call the inserter when Add a new relationship note is clicked" (~21ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/notes/WikidataAssociationDialog.titleActions.spec.ts tests/notes/sidebar/SidebarFirstGeneration.spec.ts tests/wiki-link-or-relationship/InsertWikiLink.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 23 (ranks 67–69)
Type: Structure
Status: planned

**Tests:**
- `tests/notes/NoteToolbar.moreOptions.spec.ts` — "NoteToolbar more options copies export markdown while keeping the export dialog open" (~21ms)
- `tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts` — "RichMarkdownEditor property relation and index relation property in rich mode opens dialog with custom text prefilled for an unknown relation" (~21ms)
- `tests/notes/NoteNewForm.wikidata.spec.ts` — "NoteNewForm wikidata and soft-delete search wikidata entry opens wikidata dialog on search and closes on cancel" (~21ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/notes/NoteToolbar.moreOptions.spec.ts tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts tests/notes/NoteNewForm.wikidata.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 24 (ranks 70–72)
Type: Structure
Status: planned

**Tests:**
- `tests/notes/NoteTextContent.titleEdit.spec.ts` — "NoteTextContent title edit displays an editable title by default" (~21ms)
- `tests/components/form/SeamlessTextEditor.spec.ts` — "SeamlessTextEditor keeps caret offset when modelValue is synced with same-length text" (~21ms)
- `tests/components/recall/NoteRefinement.extractNote.create.spec.ts` — "NoteRefinement extract note create shows create errors in the preview" (~21ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/notes/NoteTextContent.titleEdit.spec.ts tests/components/form/SeamlessTextEditor.spec.ts tests/components/recall/NoteRefinement.extractNote.create.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 25 (ranks 73–75)
Type: Structure
Status: planned

**Tests:**
- `tests/components/recall/NoteRefinement.extractNote.spec.ts` — "NoteRefinement extract note preview shows inline error when retry preview API fails" (~21ms)
- `tests/components/recall/NoteRefinement.extractNote.loading.spec.ts` — "NoteRefinement extract note loading shows LoadingModal while creating note from preview" (~21ms)
- `tests/components/recall/NoteRefinement.extractNote.loading.spec.ts` — "NoteRefinement extract note loading shows LoadingModal while retrying extract preview" (~21ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/recall/NoteRefinement.extractNote.spec.ts tests/components/recall/NoteRefinement.extractNote.loading.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 26 (ranks 76–78)
Type: Structure
Status: planned

**Tests:**
- `tests/notes/NoteMoreOptionsForm.deleteNote.relationship.spec.ts` — "NoteMoreOptionsForm delete relationship note uses confirm when relationship note source does not resolve" (~20ms)
- `tests/components/recall/NoteRefinement.extractionPreview.cancel.edges.spec.ts` — "NoteRefinement extraction preview cancel edges create-note pending shows creating message without Cancel" (~20ms)
- `tests/pages/BookReadingPage.snap.spec.ts` — "BookReadingPage snap snaps back and keeps panel visible on first boundary crossing (same-page: scrolls to block start)" (~20ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/notes/NoteMoreOptionsForm.deleteNote.relationship.spec.ts tests/components/recall/NoteRefinement.extractionPreview.cancel.edges.spec.ts tests/pages/BookReadingPage.snap.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 27 (ranks 79–81)
Type: Structure
Status: planned

**Tests:**
- `tests/wiki-link-or-relationship/SearchDialog.actions.spec.ts` — "SearchForm actions Move Under folder hit shows confirm when move is blocked by soft-deleted title at destination" (~20ms)
- `tests/wiki-link-or-relationship/SearchDialog.deadWikiLink.spec.ts` — "SearchForm dead wiki link actions Dead link - link to existing note rewrites path Markdown dead link '/Folder/Missing' keeping Markdown spelling" (~20ms)
- `tests/components/recall/NoteRefinement.extractNote.spec.ts` — "NoteRefinement extract note preview returns to the layout when Back is clicked" (~20ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/wiki-link-or-relationship/SearchDialog.actions.spec.ts tests/wiki-link-or-relationship/SearchDialog.deadWikiLink.spec.ts tests/components/recall/NoteRefinement.extractNote.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 28 (ranks 82–84)
Type: Structure
Status: planned

**Tests:**
- `tests/wiki-link-or-relationship/SearchDialog.deadWikiLink.spec.ts` — "SearchForm dead wiki link actions Dead link - link to existing note rewrites every matching path Markdown dead link token" (~20ms)
- `tests/notes/NoteNewForm.wikidata.spec.ts` — "NoteNewForm wikidata and soft-delete search wikidata entry search 'dog' get 'Dog' with action undefined updates title as 'Dog'" (~20ms)
- `tests/wiki-link-or-relationship/SearchDialog.actions.spec.ts` — "SearchForm actions Move to notebook root on NOTEBOOK hit calls moveNoteToNotebookRootInNotebook with notebook id after confirm" (~20ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/wiki-link-or-relationship/SearchDialog.deadWikiLink.spec.ts tests/notes/NoteNewForm.wikidata.spec.ts tests/wiki-link-or-relationship/SearchDialog.actions.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 29 (ranks 85–87)
Type: Structure
Status: planned

**Tests:**
- `tests/wiki-link-or-relationship/SearchDialog.deadWikiLink.spec.ts` — "SearchForm dead wiki link actions Dead link - link to existing note rewrites path Markdown dead link '/Folder/Missing.md' keeping Markdown spelling" (~20ms)
- `tests/components/recall/NoteRefinement.extractNote.create.spec.ts` — "NoteRefinement extract note create toggles Create note disabled state from new note title" (~20ms)
- `tests/wiki-link-or-relationship/SearchDialog.searchKeyHistory.spec.ts` — "SearchForm search key history collapses search key history inside a modal when clicking elsewhere in that modal" (~20ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/wiki-link-or-relationship/SearchDialog.deadWikiLink.spec.ts tests/components/recall/NoteRefinement.extractNote.create.spec.ts tests/wiki-link-or-relationship/SearchDialog.searchKeyHistory.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 30 (ranks 88–90)
Type: Structure
Status: planned

**Tests:**
- `tests/notes/FolderSelector.spec.ts` — "FolderSelector soft keyboard primer transfers focus to search input after folder index loads" (~20ms)
- `tests/notes/NoteMoreOptionsForm.deleteNote.spec.ts` — "NoteMoreOptionsForm delete note does not call deleteNote when confirmation is cancelled" (~20ms)
- `tests/components/recall/AssimilationPanel.loadingModal.spec.ts` — "AssimilationPanel loading modal keeps the global modal open from assimilate through loading the next unit" (~20ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/notes/FolderSelector.spec.ts tests/notes/NoteMoreOptionsForm.deleteNote.spec.ts tests/components/recall/AssimilationPanel.loadingModal.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 31 (ranks 91–93)
Type: Structure
Status: planned

**Tests:**
- `tests/notes/NoteToolbar.assimilationPanel.spec.ts` — "NoteToolbar assimilation panel hides assimilation when audio opens and vice versa" (~20ms)
- `tests/components/form/RichMarkdownEditor.propertyValuePopupReorder.spec.ts` — "RichMarkdownEditor property value popup reorder reorders duplicate list items as distinct rows in popup" (~19ms)
- `tests/pages/NotebookCatalogList.spec.ts` — "catalog list returns to title A–Z after title Z–A" (~19ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/notes/NoteToolbar.assimilationPanel.spec.ts tests/components/form/RichMarkdownEditor.propertyValuePopupReorder.spec.ts tests/pages/NotebookCatalogList.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 32 (ranks 94–96)
Type: Structure
Status: planned

**Tests:**
- `tests/components/recall/NoteRefinement.extractionPreview.cancel.spec.ts` — "NoteRefinement extraction preview cancel cancels Ask AI to retry without wiping prior preview" (~19ms)
- `tests/notes/NoteToolbar.conversationWikiNewOverflow.spec.ts` — "NoteToolbar Conversation, Wiki, and New overflow still opens wiki search when Wiki is in more options" (~19ms)
- `tests/notes/NoteNewForm.spec.ts` — "adding new note selects all text when the default Untitled title is shown" (~19ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/recall/NoteRefinement.extractionPreview.cancel.spec.ts tests/notes/NoteToolbar.conversationWikiNewOverflow.spec.ts tests/notes/NoteNewForm.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 33 (ranks 97–99)
Type: Structure
Status: planned

**Tests:**
- `tests/components/form/RichMarkdownEditor.propertyValuePopup.spec.ts` — "RichMarkdownEditor property value popup cancel closes popup without emitting property changes" (~19ms)
- `tests/components/recall/NoteRefinement.removeLayout.spec.ts` — "NoteRefinement remove refinement layout items selection and confirmation does not save or emit contentUpdated when removal returns unchanged content" (~19ms)
- `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` — "RichMarkdownEditor property key presets preset dropdown for 'occupied url preset' shows options and sets key on selection" (~19ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyValuePopup.spec.ts tests/components/recall/NoteRefinement.removeLayout.spec.ts tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 34 (ranks 100–102)
Type: Structure
Status: planned

**Tests:**
- `tests/components/form/TextInput.spec.ts` — "TextInput.vue does not select text when initialSelectAll is false" (~19ms)
- `tests/components/form/RichMarkdownEditor.propertyValuePopup.spec.ts` — "RichMarkdownEditor property value popup 'shows value edit icon on list propert…'" (~18ms)
- `tests/components/recallStats/recallStatsTheme.spec.ts` — "recall stats charts use theme tokens (dark-mode safe) calendar empty and filled cells are not hardcoded GitHub hex and adapt to dark theme" (~18ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/form/TextInput.spec.ts tests/components/form/RichMarkdownEditor.propertyValuePopup.spec.ts tests/components/recallStats/recallStatsTheme.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 35 (ranks 103–105)
Type: Structure
Status: planned

**Tests:**
- `tests/notes/NoteDeadWikiLinkCreateModal.spec.ts` — "NoteDeadWikiLinkCreateModal soft keyboard primer transfers focus to note title after create form mounts" (~18ms)
- `tests/wiki-link-or-relationship/SearchDialog.spec.ts` — "SearchForm Matches / Recent list mode keeps search key and switches between Matches and Recent" (~18ms)
- `tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` — "AnsweredSpellingQuestion accidental match omits mutating CTAs when reviewed notebook is readonly" (~18ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/notes/NoteDeadWikiLinkCreateModal.spec.ts tests/wiki-link-or-relationship/SearchDialog.spec.ts tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 36 (ranks 106–108)
Type: Structure
Status: planned

**Tests:**
- `tests/components/recall/NoteRefinement.removeLayout.spec.ts` — "NoteRefinement remove refinement layout items selection and confirmation clears selection and reloads layout after confirmed removal" (~18ms)
- `tests/pages/NoteShowPageConversation.spec.ts` — "note show page conversation opens conversation when URL has conversation=true" (~18ms)
- `tests/components/recall/NoteRefinement.extractNote.loading.spec.ts` — "NoteRefinement extract note loading shows LoadingModal during extract preview and hides on success or failure" (~18ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/recall/NoteRefinement.removeLayout.spec.ts tests/pages/NoteShowPageConversation.spec.ts tests/components/recall/NoteRefinement.extractNote.loading.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 37 (ranks 109–111)
Type: Structure
Status: planned

**Tests:**
- `tests/notes/NoteNewForm.parentRelationship.spec.ts` — "NoteNewForm parent relationship submits parent frontmatter when Under current is selected" (~18ms)
- `tests/notes/NoteToolbar.conversationWikiNewOverflow.spec.ts` — "NoteToolbar Conversation, Wiki, and New overflow moves Conversation into more options before Wiki or New" (~18ms)
- `tests/pages/BookReadingPage.snap.budgets.spec.ts` — "BookReadingPage snap budgets marking READ clears snap reminder: block no longer snaps when re-visited" (~18ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/notes/NoteNewForm.parentRelationship.spec.ts tests/notes/NoteToolbar.conversationWikiNewOverflow.spec.ts tests/pages/BookReadingPage.snap.budgets.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 38 (ranks 112–114)
Type: Structure
Status: planned

**Tests:**
- `tests/notes/WikidataAssociationDialog.titleActions.spec.ts` — "WikidataAssociationDialog title actions and save emits selected with replace action when showSaveButton is true" (~18ms)
- `tests/components/recall/AssimilationPanel.loadingModal.spec.ts` — "AssimilationPanel loading modal hides global modal when assimilate API returns an error" (~18ms)
- `tests/components/form/RichMarkdownEditor.properties.spec.ts` — "RichMarkdownEditor properties editing an existing property row emits renamed keys and updated values" (~18ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/notes/WikidataAssociationDialog.titleActions.spec.ts tests/components/recall/AssimilationPanel.loadingModal.spec.ts tests/components/form/RichMarkdownEditor.properties.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 39 (ranks 115–117)
Type: Structure
Status: planned

**Tests:**
- `tests/notes/WikidataAssociationDialog.titleActions.spec.ts` — "WikidataAssociationDialog title actions and save emits selected with replace action when showSaveButton is false" (~17ms)
- `tests/notes/WikidataAssociationDialog.titleActions.spec.ts` — "WikidataAssociationDialog title actions and save emits selected with add alias action when showSaveButton is true" (~17ms)
- `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` — "RichMarkdownEditor property key presets preset dropdown for 'existing row' shows options and sets key on selection" (~17ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/notes/WikidataAssociationDialog.titleActions.spec.ts tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 40 (ranks 118–120)
Type: Structure
Status: planned

**Tests:**
- `tests/notes/NoteDeadWikiLinkCreateModal.spec.ts` — "NoteDeadWikiLinkCreateModal soft keyboard primer transfers focus to search input after point-at-existing form mounts" (~17ms)
- `tests/notes/FolderSelector.spec.ts` — "FolderSelector soft keyboard primer does not focus primer when pointer is not coarse" (~17ms)
- `tests/pages/FolderPage.renameDissolve.spec.ts` — "FolderPage rename and dissolve dissolve shows merge confirm when dissolve returns 409 and retries with merge=true" (~17ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/notes/NoteDeadWikiLinkCreateModal.spec.ts tests/notes/FolderSelector.spec.ts tests/pages/FolderPage.renameDissolve.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 41 (ranks 121–123)
Type: Structure
Status: planned

**Tests:**
- `tests/notes/NoteNewForm.spec.ts` — "adding new note places the caret after a trailing space when initialTitle comes from a template" (~17ms)
- `tests/notes/sidebar/SidebarFolderItem.spec.ts` — "SidebarFolderItem scrolls folder row into view when active folder row is not intersecting" (~17ms)
- `tests/components/recall/AnsweredQuestionComponent.spec.ts` — "AnsweredQuestionComponent refine note passes MCQ context when opening Refine note" (~17ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/notes/NoteNewForm.spec.ts tests/notes/sidebar/SidebarFolderItem.spec.ts tests/components/recall/AnsweredQuestionComponent.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 42 (ranks 124–126)
Type: Structure
Status: planned

**Tests:**
- `tests/notes/WikidataAssociationDialog.titleActions.spec.ts` — "WikidataAssociationDialog title actions and save emits selected with add alias action when showSaveButton is false" (~17ms)
- `tests/components/form/RichMarkdownEditor.propertyTouchFocus.spec.ts` — "RichMarkdownEditor property touch focus does not focus primer when pointer is not coarse" (~17ms)
- `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` — "NoteRefinement layout selection submits only checked descendants when parent is indeterminate ('extract')" (~17ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/notes/WikidataAssociationDialog.titleActions.spec.ts tests/components/form/RichMarkdownEditor.propertyTouchFocus.spec.ts tests/components/recall/NoteRefinement.layoutSelection.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 43 (ranks 127–129)
Type: Structure
Status: planned

**Tests:**
- `tests/components/book-reading/PdfBookViewer.gestureZoom.spec.ts` — "PdfBookViewer gesture zoom (mocked pdf.js) wheel without ctrl/meta does not cancel (no browser-zoom block path)" (~17ms)
- `tests/notes/NoteNewForm.wikidata.spec.ts` — "NoteNewForm wikidata and soft-delete submit errors displays reserved title error when api returns binding error for newTitle" (~17ms)
- `tests/notes/WikidataAssociationDialog.titleActions.spec.ts` — "WikidataAssociationDialog title actions and save defers selected until Save when showSaveButton is true" (~17ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/book-reading/PdfBookViewer.gestureZoom.spec.ts tests/notes/NoteNewForm.wikidata.spec.ts tests/notes/WikidataAssociationDialog.titleActions.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 44 (ranks 130–132)
Type: Structure
Status: planned

**Tests:**
- `tests/notes/WikidataAssociationDialog.titleActions.spec.ts` — "WikidataAssociationDialog title actions and save enables Save and emits empty string when clearing with canSaveEmptyToClear" (~17ms)
- `tests/commons/Modal.spec.ts` — "Modal prefers text controls inside a marked autofocus container" (~17ms)
- `tests/components/book-reading/PdfBookViewer.gestureZoom.spec.ts` — "PdfBookViewer gesture zoom (mocked pdf.js) two-finger pinch touchmove updates scale around the midpoint" (~17ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/notes/WikidataAssociationDialog.titleActions.spec.ts tests/commons/Modal.spec.ts tests/components/book-reading/PdfBookViewer.gestureZoom.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 45 (ranks 133–135)
Type: Structure
Status: planned

**Tests:**
- `tests/notes/sidebar/SidebarPeerSort.spec.ts` — "Sidebar peer sort keeps Title (Z–A) on a later visit after the tab session is gone" (~17ms)
- `tests/notes/NoteEditableContent.relationProperty.spec.ts` — "NoteEditableContent relation property row in rich mode shows relation type picker when noteContent includes relation frontmatter" (~17ms)
- `tests/notes/NoteMoreOptionsForm.deleteNote.relationship.spec.ts` — "NoteMoreOptionsForm delete relationship note offers reduce-to-property when deleting a qualifying relationship note" (~17ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/notes/sidebar/SidebarPeerSort.spec.ts tests/notes/NoteEditableContent.relationProperty.spec.ts tests/notes/NoteMoreOptionsForm.deleteNote.relationship.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 46 (ranks 136–138)
Type: Structure
Status: planned

**Tests:**
- `tests/notes/sidebar/SidebarFolderItem.spec.ts` — "SidebarFolderItem does not scroll folder row when active folder row is already intersecting" (~17ms)
- `tests/notes/sidebar/SidebarRouteNavigation.spec.ts` — "Sidebar route navigation: sticky realm during uncached note load keeps sidebar chrome when navigating to an uncached note in the same notebook" (~17ms)
- `tests/notes/NoteDeadWikiLinkCreateModal.spec.ts` — "NoteDeadWikiLinkCreateModal soft keyboard primer does not focus primer on create tap when pointer is not coarse" (~17ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/notes/sidebar/SidebarFolderItem.spec.ts tests/notes/sidebar/SidebarRouteNavigation.spec.ts tests/notes/NoteDeadWikiLinkCreateModal.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 47 (ranks 139–141)
Type: Structure
Status: planned

**Tests:**
- `tests/pages/NoteShowPageAssimilationPanel.spec.ts` — "note show page inline assimilation panel does not render assimilation panel when settings are off" (~17ms)
- `tests/pages/FolderPage.moveConflict.spec.ts` — "FolderPage move conflicts move shows error message when move 409 and user cancels merge" (~17ms)
- `tests/pages/FolderPage.renameDissolve.spec.ts` — "FolderPage rename and dissolve dissolve shows inline error when dissolve returns soft-deleted title conflict" (~17ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/pages/NoteShowPageAssimilationPanel.spec.ts tests/pages/FolderPage.moveConflict.spec.ts tests/pages/FolderPage.renameDissolve.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 48 (ranks 142–144)
Type: Structure
Status: planned

**Tests:**
- `tests/pages/RecallPageOverlap.spec.ts` — "overlap try-again stay and retry stays on the same tracker, skips threshold, and remounts spelling on Try again" (~17ms)
- `tests/components/form/RichMarkdownEditor.propertyTouchFocus.spec.ts` — "RichMarkdownEditor property touch focus transfers focus to property key after insert form mounts with 'existing rows'" (~17ms)
- `tests/notes/NoteToolbar.moreOptionsOverflow.spec.ts` — "NoteToolbar more-options overflow moves Edit into more options when the bar is tighter than Export overflow" (~16ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/pages/RecallPageOverlap.spec.ts tests/components/form/RichMarkdownEditor.propertyTouchFocus.spec.ts tests/notes/NoteToolbar.moreOptionsOverflow.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 49 (ranks 145–147)
Type: Structure
Status: planned

**Tests:**
- `tests/components/recall/AssimilationPanel.property.spec.ts` — "AssimilationPanel property assimilation removes a property tracker from recall and shows Revive" (~16ms)
- `tests/notes/sidebar/SidebarFirstGeneration.spec.ts` — "Sidebar first generation should not scroll if already visible" (~16ms)
- `tests/components/form/RichMarkdownEditor.propertyValuePopup.spec.ts` — "RichMarkdownEditor property value popup hides list mode for scalar-only structural keys" (~16ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/recall/AssimilationPanel.property.spec.ts tests/notes/sidebar/SidebarFirstGeneration.spec.ts tests/components/form/RichMarkdownEditor.propertyValuePopup.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 50 (ranks 148–150)
Type: Structure
Status: planned

**Tests:**
- `tests/components/recall/AssimilationPanel.property.spec.ts` — "AssimilationPanel property assimilation advances to the next unit and reloads note info when assimilating a property" (~16ms)
- `tests/notes/NoteTextContent.titleEdit.saveRace.spec.ts` — "NoteTextContent title edit save race saves the last title after an earlier in-flight save finishes" (~16ms)
- `tests/components/recall/CommissionLearningSessionDialog.spec.ts` — "CommissionLearningSessionDialog keeps report textarea when record fails" (~16ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/recall/AssimilationPanel.property.spec.ts tests/notes/NoteTextContent.titleEdit.saveRace.spec.ts tests/components/recall/CommissionLearningSessionDialog.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 51 (ranks 151–153)
Type: Structure
Status: planned

**Tests:**
- `tests/wiki-link-or-relationship/SearchDialog.searchKeyHistory.spec.ts` — "SearchForm search key history collapses search key history when 'clicking a search scope toggle'" (~16ms)
- `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` — "RichMarkdownEditor property key presets preset dropdown for 'occupied image preset' shows options and sets key on selection" (~16ms)
- `tests/wiki-link-or-relationship/SearchDialog.searchKeyHistory.spec.ts` — "SearchForm search key history collapses search key history when 'clicking the search input'" (~16ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/wiki-link-or-relationship/SearchDialog.searchKeyHistory.spec.ts tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 52 (ranks 154–156)
Type: Structure
Status: planned

**Tests:**
- `tests/notes/NoteToolbar.moreOptionsOverflow.spec.ts` — "NoteToolbar more-options overflow still toggles edit mode with m when Edit is in more options" (~16ms)
- `tests/notes/NoteToolbar.assimilationPanel.spec.ts` — "NoteToolbar assimilation panel shows assimilation settings in the shared panel shell when opened" (~16ms)
- `tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts` — "RichMarkdownEditor overlaps property shows overlaps constraint for 'plain list item in popup'" (~16ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/notes/NoteToolbar.moreOptionsOverflow.spec.ts tests/notes/NoteToolbar.assimilationPanel.spec.ts tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 53 (ranks 157–159)
Type: Structure
Status: planned

**Tests:**
- `tests/components/recall/AssimilationPanel.property.spec.ts` — "AssimilationPanel property assimilation returns a skipped property to the sequence without creating a tracker or reviving" (~16ms)
- `tests/notes/NoteNewButton.spec.ts` — "NoteNewButton keyboard shortcut opens the new-note dialog when n is pressed and the button is mounted" (~16ms)
- `tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts` — "RichMarkdownEditor aliases property shows alias constraint for 'invalid list item in popup'" (~16ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/recall/AssimilationPanel.property.spec.ts tests/notes/NoteNewButton.spec.ts tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 54 (ranks 160–162)
Type: Structure
Status: planned

**Tests:**
- `tests/pages/MemoryTrackerPageView.deleteUnanswered.spec.ts` — "MemoryTrackerPageView delete unanswered confirmation message for 'multiple prompts'" (~16ms)
- `tests/components/recall/NoteRefinement.extractNote.spec.ts` — "NoteRefinement extract note preview shows inline error when extract preview API fails" (~15ms)
- `tests/toolbars/MainMenu.resume.spec.ts` — "MainMenu resume recall shows highlighted Resume before Note when recall is paused" (~15ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/pages/MemoryTrackerPageView.deleteUnanswered.spec.ts tests/components/recall/NoteRefinement.extractNote.spec.ts tests/toolbars/MainMenu.resume.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 55 (ranks 163–165)
Type: Structure
Status: planned

**Tests:**
- `tests/wiki-link-or-relationship/SearchDialog.actions.spec.ts` — "SearchForm actions Move Under folder hit calls moveNoteToFolder with folder id after confirm" (~15ms)
- `tests/notes/NoteEditableContent.paste.spec.ts` — "NoteEditableContent paste quill editor shows options popup based on content after paste" (~15ms)
- `tests/pages/MemoryTrackerPageView.deleteUnanswered.spec.ts` — "MemoryTrackerPageView delete unanswered confirmation message for 'contested prompts excluded from count'" (~15ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/wiki-link-or-relationship/SearchDialog.actions.spec.ts tests/notes/NoteEditableContent.paste.spec.ts tests/pages/MemoryTrackerPageView.deleteUnanswered.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 56 (ranks 166–168)
Type: Structure
Status: planned

**Tests:**
- `tests/components/recall/NoteRefinement.exportExtractRequest.spec.ts` — "NoteRefinement export extract request does not show export button on the extraction preview screen" (~15ms)
- `tests/notes/sidebar/SidebarAncestorLoading.spec.ts` — "Sidebar gradual ancestor population loads ancestor branches for a deep note through folder listings without showNote" (~15ms)
- `tests/components/notes/NoteTextContentUndo.spec.ts` — "undo editing should call addEditingToUndoHistory on submitChange" (~15ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/recall/NoteRefinement.exportExtractRequest.spec.ts tests/notes/sidebar/SidebarAncestorLoading.spec.ts tests/components/notes/NoteTextContentUndo.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 57 (ranks 169–171)
Type: Structure
Status: planned

**Tests:**
- `tests/notes/FolderSelector.spec.ts` — "FolderSelector keyboard navigation moves focus through folder results and back to search input with ArrowDown and ArrowUp" (~15ms)
- `tests/pages/settings/AccessTokensSettingsTab.spec.ts` — "AccessTokensSettingsTab displays "No Label" when token label is empty" (~15ms)
- `tests/pages/MemoryTrackerPageView.deleteUnanswered.spec.ts` — "MemoryTrackerPageView delete unanswered confirmation message for 'single prompt'" (~15ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/notes/FolderSelector.spec.ts tests/pages/settings/AccessTokensSettingsTab.spec.ts tests/pages/MemoryTrackerPageView.deleteUnanswered.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 58 (ranks 172–174)
Type: Structure
Status: planned

**Tests:**
- `tests/pages/NotebookCatalogExport.spec.ts` — "Notebook catalog export falls back to the notebook name when Content-Disposition is not printable ASCII" (~15ms)
- `tests/pages/RecallPage.spelling.spec.ts` — "RecallPage spelling quiz focuses the spelling answer input when resuming recall" (~15ms)
- `tests/pages/BookReadingPage.readingPosition.spec.ts` — "BookReadingPage reading position PATCH reading position includes selectedBookBlockId after layout click" (~15ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/pages/NotebookCatalogExport.spec.ts tests/pages/RecallPage.spelling.spec.ts tests/pages/BookReadingPage.readingPosition.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 59 (ranks 175–177)
Type: Structure
Status: planned

**Tests:**
- `tests/notes/NoteToolbar.moreOptions.spec.ts` — "NoteToolbar more options toggles the audio tools panel from the overflow menu" (~15ms)
- `tests/components/admin/FailureReportList.spec.ts` — "FailureReportList selecting and deleting reports deletes selected reports when confirmed" (~15ms)
- `tests/notes/NoteNewForm.spec.ts` — "adding new note searches when user edits title back to 'Untitled'" (~15ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/notes/NoteToolbar.moreOptions.spec.ts tests/components/admin/FailureReportList.spec.ts tests/notes/NoteNewForm.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Optimize batch 60 (ranks 178–179)
Type: Structure
Status: planned

**Tests:**
- `tests/components/recall/NoteRefinement.extractNote.create.spec.ts` — "NoteRefinement extract note create creates a note from the preview and navigates to the new note" (~15ms)
- `tests/components/recall/NoteRefinement.extractNote.create.spec.ts` — "NoteRefinement extract note create creates a note from edited preview fields" (~15ms)

**Goals:** Speed up only the listed tests (delete/merge redundant coverage first; then setup/selectors/waits). If no meaningful speedup after a serious attempt, append Candidate(s) to `ongoing/test-optimization-blacklist.md` and mark done.

**Verify:**

```bash
pnpm frontend:test tests/components/recall/NoteRefinement.extractNote.create.spec.ts
```

(Paths are relative to `frontend/` as accepted by `pnpm frontend:test`.)

---

### Re-profile and close
Type: Structure
Status: planned

| Metric | Before | After |
|--------|--------|-------|
| Test count | 1785 | |
| Suite wall | ~138s | |
| Top 10% total assertion time | 3722ms | |

**Candidates proposed this run:** (none / list)

**Commits:**

**Planning cleanup:** prune spent slice detail from this PLAN (or delete if fully spent); never commit profile JSON.
