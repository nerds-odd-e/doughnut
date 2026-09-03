# Frontend unit test optimization

Status: in-progress

**Execution:** run via **execute-plan** (commit + push per slice).

## Profiling baseline (2026-09-03)

Command:

`CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run --reporter=json`

- **1,886 tests**, suite wall **122.44s**
- Eligible: **1,886**
- Raw profile: `.planning/quick/frontend-profile-results.json` — **do not commit**
- Total measured test CPU: **5,922.4ms**

### Top 10% slowest (n = ceil(1,886 × 0.10) = 189)

| # | ms | file / spec | test / scenario |
|---|---:|-------------|-----------------|
| 1 | 59.2 | `tests/notes/NoteNewForm.wikidata.spec.ts` | NoteNewForm wikidata and soft-delete > search wikidata entry > replace then append title actions update title for differing wikidata label |
| 2 | 51.3 | `tests/notes/NoteNewForm.wikidata.spec.ts` | NoteNewForm wikidata and soft-delete > search wikidata entry > opens dialog, cancels, then applies matching-title selections |
| 3 | 48.0 | `tests/components/form/RichMarkdownEditor.propertyKeyPresets.listAppend.spec.ts` | RichMarkdownEditor list-capable preset append > appends to exact list-capable keys without folding legacy suffixes |
| 4 | 45.1 | `tests/pages/NoteShowPage.autosaveDelete.spec.ts` | note show autosave before deletion > uses the same barrier for ordinary deletion and reopens it after delete failure |
| 5 | 42.8 | `tests/composables/useThinkingTimeTracker.spec.ts` | useThinkingTimeTracker > excludes a suspend gap reconciled by the watchdog tick, with no pause() called |
| 6 | 42.3 | `tests/notes/NoteToolbar.pinnedToggles.spec.ts` | NoteToolbar pinned on-state toggles > pins 'assimilation' on a narrow toolbar then returns it to overflow when turned off |
| 7 | 39.3 | `tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts` | RichMarkdownEditor aliases property > inserts aliases as a list and blocks scalar aliases on row commit |
| 8 | 38.4 | `tests/composables/useThinkingTimeTracker.idle.spec.ts` | useThinkingTimeTracker idle detection > excludes a silent device suspend gap from idle time |
| 9 | 38.1 | `tests/components/form/RichMarkdownEditor.propertyValueDialogModeSwitch.spec.ts` | RichMarkdownEditor property value dialog mode switch > allows duplicate list items and saves an emptied list from the property value dialog |
| 10 | 37.7 | `tests/pages/MemoryTrackerPageView.deleteUnanswered.spec.ts` | MemoryTrackerPageView delete unanswered > confirmation messages and deletes when confirmed |
| 11 | 37.1 | `tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts` | RichMarkdownEditor overlaps property > inserts overlaps as a list and blocks scalar overlaps on row commit |
| 12 | 36.5 | `tests/notes/NoteToolbar.conversationWikiNewOverflow.spec.ts` | NoteToolbar Conversation, Wiki, and New overflow > keeps a pinned on-toggle then only more options when nothing is pinned |
| 13 | 34.2 | `tests/notes/NoteToolbar.conversationWikiNewOverflow.spec.ts` | NoteToolbar Conversation, Wiki, and New overflow > overflows Conversation before Wiki/New, and shortcuts still open them |
| 14 | 33.8 | `tests/pages/FolderPage.moveDestination.spec.ts` | FolderPage move destinations > move > retries cross-notebook folder move with merge after 409 conflict |
| 15 | 32.9 | `tests/notes/NoteToolbar.moreOptionsOverflow.spec.ts` | NoteToolbar more-options overflow > overflows Export then Edit, and Edit still works from menu and m |
| 16 | 32.7 | `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` | NoteRefinement layout selection > submits checked descendants then parent id when all descendants are selected again |
| 17 | 31.9 | `tests/notes/NoteTextContent.titleEdit.spec.ts` | NoteTextContent title edit > is editable by default and not editable when readonly |
| 18 | 31.7 | `tests/pages/NoteShowPage.autosaveDelete.spec.ts` | note show autosave before deletion > saves pending relationship content before reduction and starts no later patch |
| 19 | 30.3 | `tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts` | RichMarkdownEditor overlaps property > rejects invalid overlaps in the property value dialog then saves a valid list |
| 20 | 27.7 | `tests/components/form/RichMarkdownEditor.propertyValueDialogModeSwitch.spec.ts` | RichMarkdownEditor property value dialog mode switch > switches scalar↔list in the property value dialog, seeds text from list, and saves each mode |
| 21 | 27.7 | `tests/components/form/RichMarkdownEditor.propertyValueDialogReorder.spec.ts` | RichMarkdownEditor property value dialog reorder > disables edge moves, reorders items including duplicates, and saves YAML order |
| 22 | 27.7 | `tests/components/recall/NoteRefinement.extractNote.loading.spec.ts` | NoteRefinement extract note loading > shows LoadingModal while retrying extract preview and while creating note |
| 23 | 27.6 | `tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts` | RichMarkdownEditor aliases property > rejects invalid aliases in the property value dialog then saves a valid list |
| 24 | 27.2 | `tests/pages/NoteShowPage.autosaveDelete.spec.ts` | note show autosave before deletion > does not delete when flushing the content save fails |
| 25 | 26.9 | `tests/notes/NoteEditableContent.spec.ts` | NoteEditableContent > should preserve second edit when first save response arrives after second edit |
| 26 | 26.2 | `tests/wiki-link-or-relationship/SearchDialog.searchKeyHistory.spec.ts` | SearchForm search key history > collapses search key history when clicking the search input or a scope toggle |
| 27 | 25.4 | `tests/notes/NoteTextContent.titleEdit.spec.ts` | NoteTextContent title edit > displays a binding error then clears it after a successful edit |
| 28 | 25.2 | `tests/pages/FolderPage.renameDissolve.spec.ts` | FolderPage rename and dissolve > dissolve > soft-deleted shows inline error; name conflict confirms merge and retries |
| 29 | 24.1 | `tests/notes/NoteTextContent.wikiLinks.spec.ts` | NoteTextContent wiki link display > shows a new wiki link to an existing note as live after content save |
| 30 | 23.9 | `tests/notes/NoteTextContent.titleEdit.spec.ts` | NoteTextContent title edit > keeps newer local edits when API returns an older title |
| 31 | 23.9 | `tests/notes/NoteUnresolvedWikiLinkModal.spec.ts` | NoteUnresolvedWikiLinkModal > shows create-or-retarget choice when reopened after modelValue cleared without close |
| 32 | 23.7 | `tests/notes/NoteToolbar.pinnedToggles.spec.ts` | NoteToolbar pinned on-state toggles > pins 'audio' on a narrow toolbar then returns it to overflow when turned off |
| 33 | 23.7 | `tests/notes/NoteMoreOptionsForm.deleteNote.spec.ts` | NoteMoreOptionsForm delete note > asks how to handle references when the note has inbound references |
| 34 | 23.6 | `tests/notes/NoteMoreOptionsForm.deleteNote.relationship.spec.ts` | NoteMoreOptionsForm delete relationship note > uses confirm when relationship note source does not resolve |
| 35 | 23.6 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | NoteRefinement extract note preview > shows inline error when retry preview API fails |
| 36 | 23.1 | `tests/components/form/RichMarkdownEditor.propertyWikiLinks.spec.ts` | RichMarkdownEditor property wiki links > clicking a resolved property wiki in a property value pushes noteProperty and does not rewrite on blur |
| 37 | 22.6 | `tests/notes/WikidataAssociationDialog.titleActions.spec.ts` | WikidataAssociationDialog title actions and save > emits replace then append when showSaveButton is true |
| 38 | 22.5 | `tests/wiki-link-or-relationship/InsertWikiLink.spec.ts` | InsertWikiLink > calls the insert-wiki-link-as-property inserter with the backend-authored Portable path |
| 39 | 22.3 | `tests/notes/NoteToolbar.conversationWikiNewOverflow.spec.ts` | NoteToolbar Conversation, Wiki, and New overflow > keeps the current property location when starting a conversation from overflow |
| 40 | 22.2 | `tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` | AnsweredSpellingQuestion accidental match > builds a link as a same-Modal step and returns to the match list after success |
| 41 | 21.8 | `tests/pages/NoteShowPageConversation.spec.ts` | note show page conversation > maximizes and restores note content when maximize is toggled |
| 42 | 21.8 | `tests/components/recall/NoteRefinement.removeLayout.loading.spec.ts` | NoteRefinement remove layout loading modal > keeps remove continuous blocker noncancelable while nested layout regenerates |
| 43 | 21.6 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | NoteRefinement extract note preview > extracts multiple selected refinement layout items into one preview |
| 44 | 21.5 | `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` | RichMarkdownEditor property key presets > preset dropdown for 'existing row' shows options and sets key on selection |
| 45 | 21.4 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | NoteRefinement extract note preview > returns to the layout when Back is clicked |
| 46 | 21.3 | `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` | NoteRefinement layout selection > removes non-contiguous selected refinement layout items |
| 47 | 20.9 | `tests/components/form/RichMarkdownEditor.propertyValueDialogModeSwitch.spec.ts` | RichMarkdownEditor property value dialog mode switch > rejects empty list items on save |
| 48 | 19.5 | `tests/wiki-link-or-relationship/SearchDialog.searchKeyHistory.spec.ts` | SearchForm search key history > renders history panel inside the modal dialog and collapses on click elsewhere in that modal |
| 49 | 19.0 | `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` | RichMarkdownEditor property key presets > preset dropdown for 'occupied image preset' shows options and sets key on selection |
| 50 | 18.8 | `tests/notes/WikidataAssociationDialog.titleActions.spec.ts` | WikidataAssociationDialog title actions and save > enables Save and emits empty string when clearing with canSaveEmptyToClear |
| 51 | 18.6 | `tests/components/recall/NoteRefinement.extractionPreview.cancel.spec.ts` | NoteRefinement extraction preview cancel > cancels Ask AI to retry without wiping prior preview |
| 52 | 18.4 | `tests/components/form/RichMarkdownEditor.properties.spec.ts` | RichMarkdownEditor properties > opening one property panel then removing that row leaves the other collapsed |
| 53 | 18.2 | `tests/notes/NoteEditableContent.paste.spec.ts` | NoteEditableContent paste > quill editor > shows options popup based on content after paste, and skips when no links |
| 54 | 17.9 | `tests/components/form/RichMarkdownEditor.propertyMemoryTrackerGuard.spec.ts` | RichMarkdownEditor property memory tracker guard > keeps the property row and does not emit when the user cancels |
| 55 | 17.8 | `tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts` | RichMarkdownEditor overlaps property > shows a new overlaps wiki link as pending until last-saved includes it |
| 56 | 17.8 | `tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts` | RichMarkdownEditor overlaps property > renders overlaps list items as wiki links (resolved and dead) |
| 57 | 17.5 | `tests/components/form/RichMarkdownEditor.propertyValueDialog.spec.ts` | RichMarkdownEditor property value dialog > cancel discards edits; reopen save keeps scalar YAML shape |
| 58 | 17.5 | `tests/components/recall/AssimilationPanel.loadingModal.spec.ts` | AssimilationPanel loading modal > keeps the global modal open from skip through loading the next unit |
| 59 | 17.3 | `tests/recall/RecallPromptCard.spec.ts` | repeat page > loading state when fetching recall prompt > should show ContentLoader, not JustReview, when navigating to a memory tracker that previously failed |
| 60 | 17.0 | `tests/notes/NoteMoreOptionsForm.deleteNote.relationship.spec.ts` | NoteMoreOptionsForm delete relationship note > offers reduce-to-property using the current note after prop change without remount |
| 61 | 16.9 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | NoteRefinement extract note preview > shows inline error when extract preview API fails |
| 62 | 16.4 | `tests/notes/WikidataAssociationDialog.titleActions.spec.ts` | WikidataAssociationDialog title actions and save > defers selected until Save when showSaveButton is true |
| 63 | 16.4 | `tests/commons/Modal.spec.ts` | Modal > closes only topmost modal when ESC is pressed with stacked modals |
| 64 | 16.3 | `tests/notes/NoteEditableContent.debouncedSave.spec.ts` | NoteEditableContent debounced save > clears dirty when save returns wrapped ordinary-note content |
| 65 | 16.3 | `tests/components/form/RichMarkdownEditor.propertyAssimilation.spec.ts` | RichMarkdownEditor property assimilation controls > skips the property from its own property panel after confirming |
| 66 | 16.3 | `tests/components/recall/NoteRefinement.extractionPreview.cancel.edges.spec.ts` | NoteRefinement extraction preview cancel edges > keeps selection after Cancel, ignores a second Cancel, and retries with a fresh cancelable preview |
| 67 | 16.2 | `tests/components/form/RichMarkdownEditor.propertyPanelLocation.spec.ts` | RichMarkdownEditor property panel location > opening the property value dialog from its control leaves the property panel closed |
| 68 | 16.0 | `tests/notes/NoteMoreOptionsForm.spec.ts` | NoteMoreOptionsForm > refine note action > opens the refine note modal when clicked |
| 69 | 15.9 | `tests/notes/NoteNewForm.wikidata.spec.ts` | NoteNewForm wikidata and soft-delete > submit errors > displays reserved title error when api returns binding error for newTitle |
| 70 | 15.7 | `tests/pages/BookReadingPage.readingPosition.spec.ts` | BookReadingPage reading position > PATCH reading position includes selectedBookBlockId after layout click |
| 71 | 15.7 | `tests/components/admin/FailureReportList.spec.ts` | FailureReportList > selecting and deleting reports > deletes selected reports when confirmed |
| 72 | 15.6 | `tests/pages/NoteShowPageConversation.spec.ts` | note show page conversation > restores note content and clears conversation query on close |
| 73 | 15.6 | `tests/components/recall/NoteRefinement.removeLayout.spec.ts` | NoteRefinement remove refinement layout items > selection and confirmation > does not save or emit contentUpdated when removal returns unchanged content |
| 74 | 15.5 | `tests/notes/NoteEditableContent.htmlNormalization.spec.ts` | NoteEditableContent HTML content normalization > 'should not save when only addition is…' |
| 75 | 15.5 | `tests/notes/NoteTextContent.titleEdit.spec.ts` | NoteTextContent title edit > displays reserved title error in the title field |
| 76 | 15.5 | `tests/pages/NotebookCatalogExport.spec.ts` | Notebook catalog export > downloads a zip using 'notebook title when Content-Dispositi…' |
| 77 | 15.4 | `tests/components/form/RichMarkdownEditor.propertyTouchFocus.spec.ts` | RichMarkdownEditor property touch focus > existing property value > focuses primer then value field on touch; skips primer for dead wiki link |
| 78 | 15.3 | `tests/notes/NoteToolbar.assimilationPanel.spec.ts` | NoteToolbar assimilation panel > hides assimilation when audio opens and vice versa |
| 79 | 15.3 | `tests/notes/NoteEditableContent.memoryTracker.spec.ts` | NoteEditableContent property memory tracker guard on markdown > does not save when the user cancels |
| 80 | 15.1 | `tests/notes/NoteEditableContent.spec.ts` | NoteEditableContent > should save edited content to the correct note on blur before navigation |
| 81 | 15.0 | `tests/wiki-link-or-relationship/SearchDialog.actions.spec.ts` | SearchForm actions > Move to notebook root on NOTEBOOK hit > calls moveNoteToNotebookRootInNotebook with notebook id after confirm |
| 82 | 15.0 | `tests/notes/NoteEditableContent.spec.ts` | NoteEditableContent > should preserve unsaved edits if the noteContent prop doesn't actually change |
| 83 | 15.0 | `tests/notes/NoteTextContent.wikiLinks.spec.ts` | NoteTextContent wiki link display > shows a new wiki link as pending until content save confirms it is missing |
| 84 | 15.0 | `tests/pages/NoteShowPageAssimilationPanel.spec.ts` | note show page inline assimilation panel > toggles assimilate button with assimilation settings |
| 85 | 15.0 | `tests/components/form/RichMarkdownEditor.propertyValueDialog.spec.ts` | RichMarkdownEditor property value dialog > hides list mode for scalar-only structural keys |
| 86 | 14.9 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | NoteRefinement extract note preview > shows editable preview, retries without confirm, and confirms when fields were edited |
| 87 | 14.6 | `tests/notes/NoteEditableContent.htmlNormalization.spec.ts` | NoteEditableContent HTML content normalization > should save with trailing empty lines and <p><br></p> removed when change is not only at the end |
| 88 | 14.5 | `tests/notes/NoteEditableContent.htmlNormalization.spec.ts` | NoteEditableContent HTML content normalization > 'should not save when only addition is…' |
| 89 | 14.5 | `tests/notes/TextContentWrapper.spec.ts` | TextContentWrapper beforeSaveContent > blocks save when beforeSaveContent returns false |
| 90 | 14.4 | `tests/components/admin/FailureReportList.spec.ts` | FailureReportList > selecting and deleting reports > closes delete confirmation modal when cancel is clicked |
| 91 | 14.0 | `tests/components/recall/CommissionLearningSessionDialog.spec.ts` | CommissionLearningSessionDialog > keeps report textarea when record fails and shows rejection warning on partial success |
| 92 | 13.9 | `tests/notes/NoteMoreOptionsActions.spec.ts` | NoteMoreOptionsActions keyboard shortcut > opens the export dialog when e is pressed (layout=toolbar) |
| 93 | 13.7 | `tests/components/form/RichMarkdownEditor.propertyRenameLocation.spec.ts` | RichMarkdownEditor focused property rename location > replaces to noteProperty with the new exact key and keeps the property focused |
| 94 | 13.5 | `tests/components/form/RichMarkdownEditor.propertyMemoryTrackerGuard.spec.ts` | RichMarkdownEditor property memory tracker guard > updates the tracker property key and emits renamed frontmatter when the user confirms |
| 95 | 13.4 | `tests/wiki-link-or-relationship/InsertWikiLink.spec.ts` | InsertWikiLink > does not call the inserter when Add a new relationship note is clicked |
| 96 | 13.4 | `tests/components/form/RichMarkdownEditor.properties.spec.ts` | RichMarkdownEditor properties > invalid YAML: hides Properties, shows alert, freezes Quill, ignores body edits |
| 97 | 13.4 | `tests/notes/sidebar/SidebarPeerSort.spec.ts` | Sidebar peer sort > lists folders above notes (A–Z) and reorders root peers when Title (Z–A) is chosen |
| 98 | 13.3 | `tests/notes/NoteNewForm.parentRelationship.spec.ts` | NoteNewForm parent relationship > submits parent frontmatter for Under current and Same parent choices |
| 99 | 13.3 | `tests/components/form/RichMarkdownEditor.properties.spec.ts` | RichMarkdownEditor properties > shows validation and does not emit corrupt duplicate keys when renaming a row |
| 100 | 13.0 | `tests/components/form/RichMarkdownEditor.propertyDeleteLocation.spec.ts` | RichMarkdownEditor focused property delete location > replaces to noteShow and does not show that the property is not found |
| 101 | 13.0 | `tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts` | RichMarkdownEditor property relation and index > readme-only predefined properties > readme-only fields are shown when note already has those keys in frontmatter |
| 102 | 12.9 | `tests/components/form/RichMarkdownEditor.propertyMemoryTrackerGuard.spec.ts` | RichMarkdownEditor property memory tracker guard > reverts the property key and does not emit when the user cancels a rename |
| 103 | 12.8 | `tests/notes/NoteTextContent.titleEdit.spec.ts` | NoteTextContent title edit > keeps unsaved title edits when props change |
| 104 | 12.8 | `tests/notes/NoteTextContent.titleEdit.spec.ts` | NoteTextContent title edit > does not save when title is 'spaces only' |
| 105 | 12.8 | `tests/pages/FolderPage.moveDestination.spec.ts` | FolderPage move destinations > move > sends destinationNotebookId and navigates after cross-notebook root move |
| 106 | 12.7 | `tests/notes/NoteTextContent.titleEdit.spec.ts` | NoteTextContent title edit > does not save when title is 'mixed whitespace' |
| 107 | 12.7 | `tests/notes/NoteTextContent.titleEdit.spec.ts` | NoteTextContent title edit > displays authorization error when save is rejected with 401 |
| 108 | 12.7 | `tests/pages/FolderPage.moveConflict.spec.ts` | FolderPage move conflicts > move > shows inline error without merge prompt when move returns soft-deleted title conflict |
| 109 | 12.6 | `tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` | AnsweredSpellingQuestion accidental match > omits mutating CTAs when reviewed notebook is readonly |
| 110 | 12.6 | `tests/notes/NoteNewForm.spec.ts` | adding new note > search for duplicate |
| 111 | 12.6 | `tests/notes/NoteTextContent.titleEdit.spec.ts` | NoteTextContent title edit > does not save when title is 'newlines only' |
| 112 | 12.6 | `tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts` | RichMarkdownEditor property relation and index > readme-only predefined properties > empty readme-only fields are not included in emitted YAML |
| 113 | 12.5 | `tests/commons/Modal.spec.ts` | Modal > focuses autofocus target and prefers text controls in a marked autofocus container |
| 114 | 12.5 | `tests/notes/NoteMoreOptionsForm.spec.ts` | NoteMoreOptionsForm > assimilation settings toggle > turns assimilation settings on without changing route |
| 115 | 12.5 | `tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts` | RichMarkdownEditor property relation and index > removing every property row emits body-only markdown and shows add-only chrome without Properties heading |
| 116 | 12.4 | `tests/pages/FolderPage.moveConflict.spec.ts` | FolderPage move conflicts > move > shows merge confirm when move returns typed FOLDER_NAME_CONFLICT without status |
| 117 | 12.4 | `tests/components/form/RichMarkdownEditor.propertyValueDialog.spec.ts` | RichMarkdownEditor property value dialog > 'does not show value edit icon on spec…' |
| 118 | 12.4 | `tests/components/recall/RefineNoteModal.extractNote.close.spec.ts` | RefineNoteModal extract note close > closes the refine note modal after creating a note from extraction preview |
| 119 | 12.3 | `tests/components/form/RichMarkdownEditor.properties.spec.ts` | RichMarkdownEditor properties > editing an existing property row emits renamed keys and updated values |
| 120 | 12.3 | `tests/components/form/SeamlessTextEditor.spec.ts` | SeamlessTextEditor > pastes plain text ('append when no selection') |
| 121 | 12.2 | `tests/notes/NoteTextContent.titleEdit.spec.ts` | NoteTextContent title edit > does not save when title is 'empty string' |
| 122 | 12.2 | `tests/notes/NoteUnresolvedWikiLinkModal.spec.ts` | NoteUnresolvedWikiLinkModal > soft keyboard primer > focuses primer then 'create' target on touch device |
| 123 | 12.2 | `tests/notes/WikidataAssociationDialog.titleActions.spec.ts` | WikidataAssociationDialog title actions and save > emits replace then append when showSaveButton is false |
| 124 | 12.2 | `tests/notes/sidebar/SidebarRouteNavigation.spec.ts` | Sidebar route navigation: sticky realm during uncached note load > keeps sidebar chrome for an uncached same-notebook note, then clears active on leave |
| 125 | 12.2 | `tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts` | RichMarkdownEditor property relation and index > readme-only predefined properties > does not show readme-only predefined rows when isReadmeContext is false |
| 126 | 12.2 | `tests/components/recall/NoteRefinement.removeLayout.loading.spec.ts` | NoteRefinement remove layout loading modal > shows LoadingModal while removing refinement layout items and hides on success or failure |
| 127 | 12.1 | `tests/notes/NoteUnresolvedWikiLinkModal.spec.ts` | NoteUnresolvedWikiLinkModal > soft keyboard primer > focuses primer then 'point-at-existing' target on touch device |
| 128 | 12.1 | `tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` | AnsweredSpellingQuestion accidental match > omits mutating CTAs when note realms are not loaded |
| 129 | 12.1 | `tests/components/recall/QuestionDisplay.thinking.spec.ts` | QuestionDisplay thinking time > records a detour when deactivated and reactivated (KeepAlive) |
| 130 | 12.0 | `tests/notes/NoteToolbar.moreOptions.spec.ts` | NoteToolbar more options > toggles the audio tools panel from the inline button and overflow menu |
| 131 | 11.9 | `tests/pages/NotebookCatalogList.spec.ts` | catalog list > sorts title A–Z by default, Z–A when selected, then back to A–Z |
| 132 | 11.8 | `tests/components/form/RichMarkdownEditor.propertyValueDialog.spec.ts` | RichMarkdownEditor property value dialog > 'shows value edit icon on list propert…' |
| 133 | 11.8 | `tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` | AnsweredSpellingQuestion accidental match > dismisses resolve dialog via close button and stays on accidental-match result |
| 134 | 11.6 | `tests/notes/sidebar/SidebarPeerSort.spec.ts` | Sidebar peer sort > keeps Title (Z–A) on a later visit after the tab session is gone |
| 135 | 11.4 | `tests/pages/NoteShowPage.spec.ts` | note show page > loads note by id from route |
| 136 | 11.4 | `tests/notes/NoteNewForm.submit.spec.ts` | NoteNewForm submit > call the api |
| 137 | 11.3 | `tests/storybook/all-stories.spec.ts` | All Storybook Stories > Page Views/NotebooksPageView > renders WithNotebookGroup |
| 138 | 11.3 | `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` | NoteRefinement layout selection > preselects ledToQuestion items only when question context is provided |
| 139 | 11.2 | `tests/pages/BookReadingPage.readingControlPanel.marking.spec.ts` | BookReadingPage reading control panel marking > marking successor via auto-targeted panel advances selection past successor |
| 140 | 11.2 | `tests/notes/WikidataAssociationDialog.search.spec.ts` | WikidataAssociationDialog search and input > emits update:modelValue when user types a Wikidata ID |
| 141 | 11.1 | `tests/components/form/RichMarkdownEditor.propertyPanelLocation.spec.ts` | RichMarkdownEditor property panel location > opening the property panel replaces to noteProperty and opens the panel |
| 142 | 11.1 | `tests/components/form/SeamlessTextEditor.spec.ts` | SeamlessTextEditor > does not handle paste when 'empty clipboard' |
| 143 | 11.0 | `tests/components/recall/AssimilationPanel.loadingModal.spec.ts` | AssimilationPanel loading modal > keeps the global modal open from assimilate through next unit and hides on assimilate error |
| 144 | 10.9 | `tests/pages/RecallPage.spelling.spec.ts` | RecallPage spelling quiz > should handle spelling questions correctly |
| 145 | 10.6 | `tests/notes/NoteEditableContent.spec.ts` | NoteEditableContent > updates displayed content on navigate, including clearing when content is undefined |
| 146 | 10.6 | `tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts` | RichMarkdownEditor property relation and index > relation property in rich mode > opens custom relation dialog prefilled and commits updated frontmatter |
| 147 | 10.6 | `tests/components/recall/NoteRefinement.extractNote.loading.spec.ts` | NoteRefinement extract note loading > shows LoadingModal during extract preview and hides on success or failure |
| 148 | 10.5 | `tests/components/form/RichMarkdownEditor.propertyPanelLocation.spec.ts` | RichMarkdownEditor property panel location > preserves unrelated query values when opening and closing the property panel |
| 149 | 10.4 | `tests/notes/NoteUnresolvedWikiLinkModal.spec.ts` | NoteUnresolvedWikiLinkModal > soft keyboard primer > does not focus primer on create tap when pointer is not coarse |
| 150 | 10.4 | `tests/components/admin/FailureReportList.spec.ts` | FailureReportList > trigger test exception > calls triggerFailure API and refreshes list when clicked |
| 151 | 10.4 | `tests/components/form/SeamlessTextEditor.spec.ts` | SeamlessTextEditor > submits the nearest form on Enter |
| 152 | 10.3 | `tests/pages/BookReadingPage.readingPosition.spec.ts` | BookReadingPage reading position > does not restore reading position when no snapshot exists |
| 153 | 10.2 | `tests/wiki-link-or-relationship/SearchDialog.spec.ts` | SearchForm > Matches / Recent list mode > keeps search key and switches between Matches and Recent |
| 154 | 10.2 | `tests/notes/NoteEditableContent.debouncedSave.spec.ts` | NoteEditableContent debounced save > should not save until debounce when edit adds no new wiki link |
| 155 | 10.2 | `tests/notes/sidebar/SidebarAncestorLoading.spec.ts` | Sidebar gradual ancestor population > loads ancestor branches via folder listings, then shows them from cache on remount |
| 156 | 10.1 | `tests/storybook/all-stories.spec.ts` | All Storybook Stories > Recall/AssimilationPanel > renders SpellingAlreadyAssimilated |
| 157 | 10.1 | `tests/notes/NoteToolbar.moreOptions.spec.ts` | NoteToolbar more options > copies export markdown while keeping the export dialog open |
| 158 | 10.1 | `tests/components/form/RichMarkdownEditor.propertyFocus.spec.ts` | RichMarkdownEditor property focus from noteProperty > visiting noteProperty focuses the row, scrolls it into view, and opens its property panel |
| 159 | 10.1 | `tests/components/form/SeamlessTextEditor.spec.ts` | SeamlessTextEditor > keeps caret offset when modelValue is synced with same-length text |
| 160 | 10.1 | `tests/notes/sidebar/SidebarActiveFolder.spec.ts` | Sidebar active folder > navigates to folderPage when the folder label is clicked |
| 161 | 10.0 | `tests/wiki-link-or-relationship/AddRelationship.spec.ts` | AddRelationshipFinalize > shows LoadingModal while creating relationship note |
| 162 | 9.9 | `tests/notes/NoteEditableContent.relationProperty.spec.ts` | NoteEditableContent relation property row in rich mode > shows relation type picker only when noteContent includes relation frontmatter |
| 163 | 9.9 | `tests/components/form/SeamlessTextEditor.spec.ts` | SeamlessTextEditor > does not handle paste when 'readonly' |
| 164 | 9.7 | `tests/pages/BookReadingPage.readingPosition.spec.ts` | BookReadingPage reading position > restores reading position from stored snapshot on open |
| 165 | 9.7 | `tests/components/recall/AnsweredQuestionComponent.spec.ts` | AnsweredQuestionComponent > refine note > shows Refine note, opens refine modal, and passes MCQ context when present |
| 166 | 9.6 | `tests/components/recall/NoteRefinement.extractNote.spec.ts` | NoteRefinement extract note preview > displays one extract button and no per-item extract buttons |
| 167 | 9.5 | `tests/pages/BookReadingPage.navigationBar.spec.ts` | BookReadingPage navigation bar > shows navigation bar when current block differs from selected block |
| 168 | 9.5 | `tests/pages/RecallPage.viewHistoryThinkingTime.spec.ts` | thinking time while viewing a previously answered question > excludes time spent viewing the last answered question from the current question's thinking time |
| 169 | 9.4 | `tests/pages/NoteShowPageConversation.spec.ts` | note show page conversation > clears conversation query without leaving the property location |
| 170 | 9.3 | `tests/notes/NoteNewForm.spec.ts` | adding new note > does not search for initial default 'Untitled' title |
| 171 | 9.1 | `tests/pages/BookReadingPage.snap.budgets.spec.ts` | BookReadingPage snap budgets > marking READ clears snap reminder: block no longer snaps when re-visited |
| 172 | 9.1 | `tests/components/form/RichMarkdownEditor.propertyTouchFocus.spec.ts` | RichMarkdownEditor property touch focus > Add property on touch focuses primer then property key with 'no existing rows' |
| 173 | 9.1 | `tests/wiki-link-or-relationship/SearchDialog.spec.ts` | SearchForm > keyboard navigation > moves focus through results and back to search input with ArrowDown and ArrowUp |
| 174 | 9.0 | `tests/pages/BookReadingPage.readingPosition.spec.ts` | BookReadingPage reading position > debounces PATCH reading position; keeps last top; skips null viewport |
| 175 | 8.9 | `tests/wiki-link-or-relationship/SearchDialog.searchKeyHistory.spec.ts` | SearchForm search key history > lists cookie keys and fills the input when one is chosen |
| 176 | 8.9 | `tests/components/form/RichMarkdownEditor.properties.spec.ts` | RichMarkdownEditor properties > composes edited body with existing frontmatter when emitting updates |
| 177 | 8.8 | `tests/notes/NoteMoreOptionsForm.spec.ts` | NoteMoreOptionsForm > assimilation settings toggle > emits close-dialog when assimilation settings button is clicked |
| 178 | 8.8 | `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` | NoteRefinement layout selection > cascades parent selection and marks already extracted items without disabling |
| 179 | 8.7 | `tests/components/form/RichMarkdownEditor.spec.ts` | RichMarkdownEditor > preserves nested bullet indentation when pasting ChatGPT-style HTML |
| 180 | 8.7 | `tests/notes/NoteNewForm.spec.ts` | adding new note > searches when user edits title back to 'Untitled' |
| 181 | 8.6 | `tests/notes/NoteExportForm.spec.ts` | NoteExportForm > allows customizing token limit and refreshes graph |
| 182 | 8.6 | `tests/notes/NoteTextContent.titleEdit.saveRace.spec.ts` | NoteTextContent title edit save race > saves the last title after an earlier in-flight save finishes |
| 183 | 8.6 | `tests/pages/BookReadingPage.snap.spec.ts` | BookReadingPage snap > snaps back on first boundary crossing and when landing two+ blocks ahead |
| 184 | 8.6 | `tests/pages/RecallPageOverlap.spec.ts` | overlap try-again stay and retry > stays on the same tracker, skips threshold, and remounts spelling on Try again |
| 185 | 8.6 | `tests/components/recall/AnsweredSpellingQuestionAddAsOverlapped.spec.ts` | AnsweredSpellingQuestion add as overlapped note > adds as overlapped note via wiki-link content update without try-again |
| 186 | 8.4 | `tests/notes/NoteMoreOptionsActions.spec.ts` | NoteMoreOptionsActions keyboard shortcut > opens the export dialog when e is pressed (layout=menu) |
| 187 | 8.4 | `tests/pages/RecallPage.treadmill.spec.ts` | RecallPage treadmill mode > should show treadmill mode toggle in settings |
| 188 | 8.4 | `tests/components/form/RichMarkdownEditor.propertyTouchFocus.spec.ts` | RichMarkdownEditor property touch focus > existing property value > does not focus primer when pointer is not coarse |
| 189 | 8.4 | `tests/pages/settings/RecallStatsSettingsTab.spec.ts` | RecallStatsSettingsTab > renders retention headline, charts, and best/worst hours from the fixture |

Top-10% measured CPU: **3,209.1ms**.

### Grouping

- By file: **95 groups**
- Batches of 3: **63 groups**
- **Chosen:** batches of 3 (fewer groups)

## Optimization rules

1. Remove or simplify redundant tests first.
2. Strictly no fixed-time waits.
3. Flaky = failure.

Hard-to-improve tests: propose under **Candidates** in
`.planning/test-optimization-blacklist.md`. Do not add permanent skip tags in optimize mode.

---

### 1. Optimize ranked tests 1–3
Type: Structure
Status: done

Outcome: Removed redundant exact-match/append UI journeys and retained the unique cancel, matching, replace, and list-append behaviors. Focused browser tests passed three consecutive runs.

**Tests:**
- #1 `tests/notes/NoteNewForm.wikidata.spec.ts` — NoteNewForm wikidata and soft-delete > search wikidata entry > replace then append title actions update title for differing wikidata label (~59.2ms)
- #2 `tests/notes/NoteNewForm.wikidata.spec.ts` — NoteNewForm wikidata and soft-delete > search wikidata entry > opens dialog, cancels, then applies matching-title selections (~51.3ms)
- #3 `tests/components/form/RichMarkdownEditor.propertyKeyPresets.listAppend.spec.ts` — RichMarkdownEditor list-capable preset append > appends to exact list-capable keys without folding legacy suffixes (~48.0ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteNewForm.wikidata.spec.ts tests/components/form/RichMarkdownEditor.propertyKeyPresets.listAppend.spec.ts
```

---

### 2. Optimize ranked tests 4–6
Type: Structure
Status: done

Outcome: Replaced a day-scale fake-timer advance with one watchdog tick, simplified duplicated deferred-save setup, and removed a redundant assimilation pinning journey. Focused browser tests passed four consecutive runs.

**Tests:**
- #4 `tests/pages/NoteShowPage.autosaveDelete.spec.ts` — note show autosave before deletion > uses the same barrier for ordinary deletion and reopens it after delete failure (~45.1ms)
- #5 `tests/composables/useThinkingTimeTracker.spec.ts` — useThinkingTimeTracker > excludes a suspend gap reconciled by the watchdog tick, with no pause() called (~42.8ms)
- #6 `tests/notes/NoteToolbar.pinnedToggles.spec.ts` — NoteToolbar pinned on-state toggles > pins 'assimilation' on a narrow toolbar then returns it to overflow when turned off (~42.3ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/NoteShowPage.autosaveDelete.spec.ts tests/composables/useThinkingTimeTracker.spec.ts tests/notes/NoteToolbar.pinnedToggles.spec.ts
```

---

### 3. Optimize ranked tests 7–9
Type: Structure
Status: done

Outcome: Removed redundant mount/flush/assertion work, replaced a huge suspend timer jump with one watchdog tick, and retained only the unique empty-list dialog behavior. Focused browser tests passed twice.

**Tests:**
- #7 `tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts` — RichMarkdownEditor aliases property > inserts aliases as a list and blocks scalar aliases on row commit (~39.3ms)
- #8 `tests/composables/useThinkingTimeTracker.idle.spec.ts` — useThinkingTimeTracker idle detection > excludes a silent device suspend gap from idle time (~38.4ms)
- #9 `tests/components/form/RichMarkdownEditor.propertyValueDialogModeSwitch.spec.ts` — RichMarkdownEditor property value dialog mode switch > allows duplicate list items and saves an emptied list from the property value dialog (~38.1ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts tests/composables/useThinkingTimeTracker.idle.spec.ts tests/components/form/RichMarkdownEditor.propertyValueDialogModeSwitch.spec.ts
```

---

### 4. Optimize ranked tests 10–12
Type: Structure
Status: done

Outcome: Halved the delete-unanswered journey, removed redundant overlaps setup/assertions, folded overflow assertions into the progressive case, and deleted the duplicate pinned-toggle journey plus two helpers it orphaned. Focused browser coverage passed repeatedly.

**Tests:**
- #10 `tests/pages/MemoryTrackerPageView.deleteUnanswered.spec.ts` — MemoryTrackerPageView delete unanswered > confirmation messages and deletes when confirmed (~37.7ms)
- #11 `tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts` — RichMarkdownEditor overlaps property > inserts overlaps as a list and blocks scalar overlaps on row commit (~37.1ms)
- #12 `tests/notes/NoteToolbar.conversationWikiNewOverflow.spec.ts` — NoteToolbar Conversation, Wiki, and New overflow > keeps a pinned on-toggle then only more options when nothing is pinned (~36.5ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/MemoryTrackerPageView.deleteUnanswered.spec.ts tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts tests/notes/NoteToolbar.conversationWikiNewOverflow.spec.ts
```

---

### 5. Optimize ranked tests 13–15
Type: Structure
Status: done

Outcome: Removed redundant toolbar ordering and shortcut journeys, renamed the remaining conversation-overflow spec, and deleted three orphaned helpers. The cross-notebook 409 retry remained uniquely valuable and was recorded as a Candidate. Focused browser tests passed three consecutive runs.

**Tests:**
- #13 `tests/notes/NoteToolbar.conversationWikiNewOverflow.spec.ts` — NoteToolbar Conversation, Wiki, and New overflow > overflows Conversation before Wiki/New, and shortcuts still open them (~34.2ms)
- #14 `tests/pages/FolderPage.moveDestination.spec.ts` — FolderPage move destinations > move > retries cross-notebook folder move with merge after 409 conflict (~33.8ms)
- #15 `tests/notes/NoteToolbar.moreOptionsOverflow.spec.ts` — NoteToolbar more-options overflow > overflows Export then Edit, and Edit still works from menu and m (~32.9ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteToolbar.conversationWikiNewOverflow.spec.ts tests/pages/FolderPage.moveDestination.spec.ts tests/notes/NoteToolbar.moreOptionsOverflow.spec.ts
```

---

### 6. Optimize ranked tests 16–18
Type: Structure
Status: done

Outcome: Removed redundant refinement and title-edit journeys, consolidated autosave deletion barriers onto one mount, and refactored repeated textarea mutation. Focused Chromium tests passed three consecutive runs.

**Tests:**
- #16 `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` — NoteRefinement layout selection > submits checked descendants then parent id when all descendants are selected again (~32.7ms)
- #17 `tests/notes/NoteTextContent.titleEdit.spec.ts` — NoteTextContent title edit > is editable by default and not editable when readonly (~31.9ms)
- #18 `tests/pages/NoteShowPage.autosaveDelete.spec.ts` — note show autosave before deletion > saves pending relationship content before reduction and starts no later patch (~31.7ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.layoutSelection.spec.ts tests/notes/NoteTextContent.titleEdit.spec.ts tests/pages/NoteShowPage.autosaveDelete.spec.ts
```

---

### 7. Optimize ranked tests 19–21
Type: Structure
Status: done

Outcome: Removed redundant validation/item journeys, reduced mode switching to one mount, and combined edge-state, duplicate, reorder, and YAML checks into one save. Ranked cases measured about 32.5% faster; focused Chromium passed twice.

**Tests:**
- #19 `tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts` — RichMarkdownEditor overlaps property > rejects invalid overlaps in the property value dialog then saves a valid list (~30.3ms)
- #20 `tests/components/form/RichMarkdownEditor.propertyValueDialogModeSwitch.spec.ts` — RichMarkdownEditor property value dialog mode switch > switches scalar↔list in the property value dialog, seeds text from list, and saves each mode (~27.7ms)
- #21 `tests/components/form/RichMarkdownEditor.propertyValueDialogReorder.spec.ts` — RichMarkdownEditor property value dialog reorder > disables edge moves, reorders items including duplicates, and saves YAML order (~27.7ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts tests/components/form/RichMarkdownEditor.propertyValueDialogModeSwitch.spec.ts tests/components/form/RichMarkdownEditor.propertyValueDialogReorder.spec.ts
```

---

### 8. Optimize ranked tests 22–24
Type: Structure
Status: done

Outcome: Reused mounts across refinement retry, aliases validation, and deletion failure paths while retaining each unique observable; clarified async gate naming. Focused Chromium passed three consecutive runs.

**Tests:**
- #22 `tests/components/recall/NoteRefinement.extractNote.loading.spec.ts` — NoteRefinement extract note loading > shows LoadingModal while retrying extract preview and while creating note (~27.7ms)
- #23 `tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts` — RichMarkdownEditor aliases property > rejects invalid aliases in the property value dialog then saves a valid list (~27.6ms)
- #24 `tests/pages/NoteShowPage.autosaveDelete.spec.ts` — note show autosave before deletion > does not delete when flushing the content save fails (~27.2ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.extractNote.loading.spec.ts tests/components/form/RichMarkdownEditor.aliasesProperty.spec.ts tests/pages/NoteShowPage.autosaveDelete.spec.ts
```

---

### 9. Optimize ranked tests 25–27
Type: Structure
Status: done

Outcome: Removed broad flush/setup paths while preserving edit-race, search-history collapse, and binding-error recovery behavior. Ranked cases fell from 84.0ms to 26.6ms; focused Chromium passed three consecutive runs.

**Tests:**
- #25 `tests/notes/NoteEditableContent.spec.ts` — NoteEditableContent > should preserve second edit when first save response arrives after second edit (~26.9ms)
- #26 `tests/wiki-link-or-relationship/SearchDialog.searchKeyHistory.spec.ts` — SearchForm search key history > collapses search key history when clicking the search input or a scope toggle (~26.2ms)
- #27 `tests/notes/NoteTextContent.titleEdit.spec.ts` — NoteTextContent title edit > displays a binding error then clears it after a successful edit (~25.4ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableContent.spec.ts tests/wiki-link-or-relationship/SearchDialog.searchKeyHistory.spec.ts tests/notes/NoteTextContent.titleEdit.spec.ts
```

---

### 10. Optimize ranked tests 28–30
Type: Structure
Status: planned

**Tests:**
- #28 `tests/pages/FolderPage.renameDissolve.spec.ts` — FolderPage rename and dissolve > dissolve > soft-deleted shows inline error; name conflict confirms merge and retries (~25.2ms)
- #29 `tests/notes/NoteTextContent.wikiLinks.spec.ts` — NoteTextContent wiki link display > shows a new wiki link to an existing note as live after content save (~24.1ms)
- #30 `tests/notes/NoteTextContent.titleEdit.spec.ts` — NoteTextContent title edit > keeps newer local edits when API returns an older title (~23.9ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/FolderPage.renameDissolve.spec.ts tests/notes/NoteTextContent.wikiLinks.spec.ts tests/notes/NoteTextContent.titleEdit.spec.ts
```

---

### 11. Optimize ranked tests 31–33
Type: Structure
Status: done

Outcome: Removed redundant modal focus/flush work, styling assertions, and router settling while retaining both modal choices, the complete generic pin lifecycle, and the inbound-reference payload behavior. Focused browser checks passed.

**Tests:**
- #31 `tests/notes/NoteUnresolvedWikiLinkModal.spec.ts` — NoteUnresolvedWikiLinkModal > shows create-or-retarget choice when reopened after modelValue cleared without close (~23.9ms)
- #32 `tests/notes/NoteToolbar.pinnedToggles.spec.ts` — NoteToolbar pinned on-state toggles > pins 'audio' on a narrow toolbar then returns it to overflow when turned off (~23.7ms)
- #33 `tests/notes/NoteMoreOptionsForm.deleteNote.spec.ts` — NoteMoreOptionsForm delete note > asks how to handle references when the note has inbound references (~23.7ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteUnresolvedWikiLinkModal.spec.ts tests/notes/NoteToolbar.pinnedToggles.spec.ts tests/notes/NoteMoreOptionsForm.deleteNote.spec.ts
```

---

### 12. Optimize ranked tests 34–36
Type: Structure
Status: done

Outcome: Removed a redundant relationship-delete UI case, folded retry failure into the edited-preview journey, and dropped unnecessary focus/flush work while asserting blur does not rewrite. Focused browser verification passed three consecutive runs.

**Tests:**
- #34 `tests/notes/NoteMoreOptionsForm.deleteNote.relationship.spec.ts` — NoteMoreOptionsForm delete relationship note > uses confirm when relationship note source does not resolve (~23.6ms)
- #35 `tests/components/recall/NoteRefinement.extractNote.spec.ts` — NoteRefinement extract note preview > shows inline error when retry preview API fails (~23.6ms)
- #36 `tests/components/form/RichMarkdownEditor.propertyWikiLinks.spec.ts` — RichMarkdownEditor property wiki links > clicking a resolved property wiki in a property value pushes noteProperty and does not rewrite on blur (~23.1ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteMoreOptionsForm.deleteNote.relationship.spec.ts tests/components/recall/NoteRefinement.extractNote.spec.ts tests/components/form/RichMarkdownEditor.propertyWikiLinks.spec.ts
```

---

### 13. Optimize ranked tests 37–39
Type: Structure
Status: done

Outcome: Removed a duplicate Wikidata mount, drove property insertion at the selected-note boundary, and replaced full toolbar layout setup with the real overflow menu while preserving property-route behavior. Focused Chromium passed.

**Tests:**
- #37 `tests/notes/WikidataAssociationDialog.titleActions.spec.ts` — WikidataAssociationDialog title actions and save > emits replace then append when showSaveButton is true (~22.6ms)
- #38 `tests/wiki-link-or-relationship/InsertWikiLink.spec.ts` — InsertWikiLink > calls the insert-wiki-link-as-property inserter with the backend-authored Portable path (~22.5ms)
- #39 `tests/notes/NoteToolbar.conversationWikiNewOverflow.spec.ts` — NoteToolbar Conversation, Wiki, and New overflow > keeps the current property location when starting a conversation from overflow (~22.3ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/WikidataAssociationDialog.titleActions.spec.ts tests/wiki-link-or-relationship/InsertWikiLink.spec.ts tests/notes/NoteToolbar.conversationWikiNewOverflow.spec.ts
```

---

### 14. Optimize ranked tests 40–42
Type: Structure
Status: planned

**Tests:**
- #40 `tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` — AnsweredSpellingQuestion accidental match > builds a link as a same-Modal step and returns to the match list after success (~22.2ms)
- #41 `tests/pages/NoteShowPageConversation.spec.ts` — note show page conversation > maximizes and restores note content when maximize is toggled (~21.8ms)
- #42 `tests/components/recall/NoteRefinement.removeLayout.loading.spec.ts` — NoteRefinement remove layout loading modal > keeps remove continuous blocker noncancelable while nested layout regenerates (~21.8ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts tests/pages/NoteShowPageConversation.spec.ts tests/components/recall/NoteRefinement.removeLayout.loading.spec.ts
```

---

### 15. Optimize ranked tests 43–45
Type: Structure
Status: planned

**Tests:**
- #43 `tests/components/recall/NoteRefinement.extractNote.spec.ts` — NoteRefinement extract note preview > extracts multiple selected refinement layout items into one preview (~21.6ms)
- #44 `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` — RichMarkdownEditor property key presets > preset dropdown for 'existing row' shows options and sets key on selection (~21.5ms)
- #45 `tests/components/recall/NoteRefinement.extractNote.spec.ts` — NoteRefinement extract note preview > returns to the layout when Back is clicked (~21.4ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.extractNote.spec.ts tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts
```

---

### 16. Optimize ranked tests 46–48
Type: Structure
Status: planned

**Tests:**
- #46 `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` — NoteRefinement layout selection > removes non-contiguous selected refinement layout items (~21.3ms)
- #47 `tests/components/form/RichMarkdownEditor.propertyValueDialogModeSwitch.spec.ts` — RichMarkdownEditor property value dialog mode switch > rejects empty list items on save (~20.9ms)
- #48 `tests/wiki-link-or-relationship/SearchDialog.searchKeyHistory.spec.ts` — SearchForm search key history > renders history panel inside the modal dialog and collapses on click elsewhere in that modal (~19.5ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.layoutSelection.spec.ts tests/components/form/RichMarkdownEditor.propertyValueDialogModeSwitch.spec.ts tests/wiki-link-or-relationship/SearchDialog.searchKeyHistory.spec.ts
```

---

### 17. Optimize ranked tests 49–51
Type: Structure
Status: planned

**Tests:**
- #49 `tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts` — RichMarkdownEditor property key presets > preset dropdown for 'occupied image preset' shows options and sets key on selection (~19.0ms)
- #50 `tests/notes/WikidataAssociationDialog.titleActions.spec.ts` — WikidataAssociationDialog title actions and save > enables Save and emits empty string when clearing with canSaveEmptyToClear (~18.8ms)
- #51 `tests/components/recall/NoteRefinement.extractionPreview.cancel.spec.ts` — NoteRefinement extraction preview cancel > cancels Ask AI to retry without wiping prior preview (~18.6ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyKeyPresets.spec.ts tests/notes/WikidataAssociationDialog.titleActions.spec.ts tests/components/recall/NoteRefinement.extractionPreview.cancel.spec.ts
```

---

### 18. Optimize ranked tests 52–54
Type: Structure
Status: planned

**Tests:**
- #52 `tests/components/form/RichMarkdownEditor.properties.spec.ts` — RichMarkdownEditor properties > opening one property panel then removing that row leaves the other collapsed (~18.4ms)
- #53 `tests/notes/NoteEditableContent.paste.spec.ts` — NoteEditableContent paste > quill editor > shows options popup based on content after paste, and skips when no links (~18.2ms)
- #54 `tests/components/form/RichMarkdownEditor.propertyMemoryTrackerGuard.spec.ts` — RichMarkdownEditor property memory tracker guard > keeps the property row and does not emit when the user cancels (~17.9ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.properties.spec.ts tests/notes/NoteEditableContent.paste.spec.ts tests/components/form/RichMarkdownEditor.propertyMemoryTrackerGuard.spec.ts
```

---

### 19. Optimize ranked tests 55–57
Type: Structure
Status: planned

**Tests:**
- #55 `tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts` — RichMarkdownEditor overlaps property > shows a new overlaps wiki link as pending until last-saved includes it (~17.8ms)
- #56 `tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts` — RichMarkdownEditor overlaps property > renders overlaps list items as wiki links (resolved and dead) (~17.8ms)
- #57 `tests/components/form/RichMarkdownEditor.propertyValueDialog.spec.ts` — RichMarkdownEditor property value dialog > cancel discards edits; reopen save keeps scalar YAML shape (~17.5ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.overlapsProperty.spec.ts tests/components/form/RichMarkdownEditor.propertyValueDialog.spec.ts
```

---

### 20. Optimize ranked tests 58–60
Type: Structure
Status: planned

**Tests:**
- #58 `tests/components/recall/AssimilationPanel.loadingModal.spec.ts` — AssimilationPanel loading modal > keeps the global modal open from skip through loading the next unit (~17.5ms)
- #59 `tests/recall/RecallPromptCard.spec.ts` — repeat page > loading state when fetching recall prompt > should show ContentLoader, not JustReview, when navigating to a memory tracker that previously failed (~17.3ms)
- #60 `tests/notes/NoteMoreOptionsForm.deleteNote.relationship.spec.ts` — NoteMoreOptionsForm delete relationship note > offers reduce-to-property using the current note after prop change without remount (~17.0ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/AssimilationPanel.loadingModal.spec.ts tests/recall/RecallPromptCard.spec.ts tests/notes/NoteMoreOptionsForm.deleteNote.relationship.spec.ts
```

---

### 21. Optimize ranked tests 61–63
Type: Structure
Status: planned

**Tests:**
- #61 `tests/components/recall/NoteRefinement.extractNote.spec.ts` — NoteRefinement extract note preview > shows inline error when extract preview API fails (~16.9ms)
- #62 `tests/notes/WikidataAssociationDialog.titleActions.spec.ts` — WikidataAssociationDialog title actions and save > defers selected until Save when showSaveButton is true (~16.4ms)
- #63 `tests/commons/Modal.spec.ts` — Modal > closes only topmost modal when ESC is pressed with stacked modals (~16.4ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.extractNote.spec.ts tests/notes/WikidataAssociationDialog.titleActions.spec.ts tests/commons/Modal.spec.ts
```

---

### 22. Optimize ranked tests 64–66
Type: Structure
Status: planned

**Tests:**
- #64 `tests/notes/NoteEditableContent.debouncedSave.spec.ts` — NoteEditableContent debounced save > clears dirty when save returns wrapped ordinary-note content (~16.3ms)
- #65 `tests/components/form/RichMarkdownEditor.propertyAssimilation.spec.ts` — RichMarkdownEditor property assimilation controls > skips the property from its own property panel after confirming (~16.3ms)
- #66 `tests/components/recall/NoteRefinement.extractionPreview.cancel.edges.spec.ts` — NoteRefinement extraction preview cancel edges > keeps selection after Cancel, ignores a second Cancel, and retries with a fresh cancelable preview (~16.3ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableContent.debouncedSave.spec.ts tests/components/form/RichMarkdownEditor.propertyAssimilation.spec.ts tests/components/recall/NoteRefinement.extractionPreview.cancel.edges.spec.ts
```

---

### 23. Optimize ranked tests 67–69
Type: Structure
Status: planned

**Tests:**
- #67 `tests/components/form/RichMarkdownEditor.propertyPanelLocation.spec.ts` — RichMarkdownEditor property panel location > opening the property value dialog from its control leaves the property panel closed (~16.2ms)
- #68 `tests/notes/NoteMoreOptionsForm.spec.ts` — NoteMoreOptionsForm > refine note action > opens the refine note modal when clicked (~16.0ms)
- #69 `tests/notes/NoteNewForm.wikidata.spec.ts` — NoteNewForm wikidata and soft-delete > submit errors > displays reserved title error when api returns binding error for newTitle (~15.9ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyPanelLocation.spec.ts tests/notes/NoteMoreOptionsForm.spec.ts tests/notes/NoteNewForm.wikidata.spec.ts
```

---

### 24. Optimize ranked tests 70–72
Type: Structure
Status: planned

**Tests:**
- #70 `tests/pages/BookReadingPage.readingPosition.spec.ts` — BookReadingPage reading position > PATCH reading position includes selectedBookBlockId after layout click (~15.7ms)
- #71 `tests/components/admin/FailureReportList.spec.ts` — FailureReportList > selecting and deleting reports > deletes selected reports when confirmed (~15.7ms)
- #72 `tests/pages/NoteShowPageConversation.spec.ts` — note show page conversation > restores note content and clears conversation query on close (~15.6ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/BookReadingPage.readingPosition.spec.ts tests/components/admin/FailureReportList.spec.ts tests/pages/NoteShowPageConversation.spec.ts
```

---

### 25. Optimize ranked tests 73–75
Type: Structure
Status: planned

**Tests:**
- #73 `tests/components/recall/NoteRefinement.removeLayout.spec.ts` — NoteRefinement remove refinement layout items > selection and confirmation > does not save or emit contentUpdated when removal returns unchanged content (~15.6ms)
- #74 `tests/notes/NoteEditableContent.htmlNormalization.spec.ts` — NoteEditableContent HTML content normalization > 'should not save when only addition is…' (~15.5ms)
- #75 `tests/notes/NoteTextContent.titleEdit.spec.ts` — NoteTextContent title edit > displays reserved title error in the title field (~15.5ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.removeLayout.spec.ts tests/notes/NoteEditableContent.htmlNormalization.spec.ts tests/notes/NoteTextContent.titleEdit.spec.ts
```

---

### 26. Optimize ranked tests 76–78
Type: Structure
Status: planned

**Tests:**
- #76 `tests/pages/NotebookCatalogExport.spec.ts` — Notebook catalog export > downloads a zip using 'notebook title when Content-Dispositi…' (~15.5ms)
- #77 `tests/components/form/RichMarkdownEditor.propertyTouchFocus.spec.ts` — RichMarkdownEditor property touch focus > existing property value > focuses primer then value field on touch; skips primer for dead wiki link (~15.4ms)
- #78 `tests/notes/NoteToolbar.assimilationPanel.spec.ts` — NoteToolbar assimilation panel > hides assimilation when audio opens and vice versa (~15.3ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/NotebookCatalogExport.spec.ts tests/components/form/RichMarkdownEditor.propertyTouchFocus.spec.ts tests/notes/NoteToolbar.assimilationPanel.spec.ts
```

---

### 27. Optimize ranked tests 79–81
Type: Structure
Status: planned

**Tests:**
- #79 `tests/notes/NoteEditableContent.memoryTracker.spec.ts` — NoteEditableContent property memory tracker guard on markdown > does not save when the user cancels (~15.3ms)
- #80 `tests/notes/NoteEditableContent.spec.ts` — NoteEditableContent > should save edited content to the correct note on blur before navigation (~15.1ms)
- #81 `tests/wiki-link-or-relationship/SearchDialog.actions.spec.ts` — SearchForm actions > Move to notebook root on NOTEBOOK hit > calls moveNoteToNotebookRootInNotebook with notebook id after confirm (~15.0ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableContent.memoryTracker.spec.ts tests/notes/NoteEditableContent.spec.ts tests/wiki-link-or-relationship/SearchDialog.actions.spec.ts
```

---

### 28. Optimize ranked tests 82–84
Type: Structure
Status: planned

**Tests:**
- #82 `tests/notes/NoteEditableContent.spec.ts` — NoteEditableContent > should preserve unsaved edits if the noteContent prop doesn't actually change (~15.0ms)
- #83 `tests/notes/NoteTextContent.wikiLinks.spec.ts` — NoteTextContent wiki link display > shows a new wiki link as pending until content save confirms it is missing (~15.0ms)
- #84 `tests/pages/NoteShowPageAssimilationPanel.spec.ts` — note show page inline assimilation panel > toggles assimilate button with assimilation settings (~15.0ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableContent.spec.ts tests/notes/NoteTextContent.wikiLinks.spec.ts tests/pages/NoteShowPageAssimilationPanel.spec.ts
```

---

### 29. Optimize ranked tests 85–87
Type: Structure
Status: planned

**Tests:**
- #85 `tests/components/form/RichMarkdownEditor.propertyValueDialog.spec.ts` — RichMarkdownEditor property value dialog > hides list mode for scalar-only structural keys (~15.0ms)
- #86 `tests/components/recall/NoteRefinement.extractNote.spec.ts` — NoteRefinement extract note preview > shows editable preview, retries without confirm, and confirms when fields were edited (~14.9ms)
- #87 `tests/notes/NoteEditableContent.htmlNormalization.spec.ts` — NoteEditableContent HTML content normalization > should save with trailing empty lines and <p><br></p> removed when change is not only at the end (~14.6ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyValueDialog.spec.ts tests/components/recall/NoteRefinement.extractNote.spec.ts tests/notes/NoteEditableContent.htmlNormalization.spec.ts
```

---

### 30. Optimize ranked tests 88–90
Type: Structure
Status: planned

**Tests:**
- #88 `tests/notes/NoteEditableContent.htmlNormalization.spec.ts` — NoteEditableContent HTML content normalization > 'should not save when only addition is…' (~14.5ms)
- #89 `tests/notes/TextContentWrapper.spec.ts` — TextContentWrapper beforeSaveContent > blocks save when beforeSaveContent returns false (~14.5ms)
- #90 `tests/components/admin/FailureReportList.spec.ts` — FailureReportList > selecting and deleting reports > closes delete confirmation modal when cancel is clicked (~14.4ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableContent.htmlNormalization.spec.ts tests/notes/TextContentWrapper.spec.ts tests/components/admin/FailureReportList.spec.ts
```

---

### 31. Optimize ranked tests 91–93
Type: Structure
Status: planned

**Tests:**
- #91 `tests/components/recall/CommissionLearningSessionDialog.spec.ts` — CommissionLearningSessionDialog > keeps report textarea when record fails and shows rejection warning on partial success (~14.0ms)
- #92 `tests/notes/NoteMoreOptionsActions.spec.ts` — NoteMoreOptionsActions keyboard shortcut > opens the export dialog when e is pressed (layout=toolbar) (~13.9ms)
- #93 `tests/components/form/RichMarkdownEditor.propertyRenameLocation.spec.ts` — RichMarkdownEditor focused property rename location > replaces to noteProperty with the new exact key and keeps the property focused (~13.7ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/CommissionLearningSessionDialog.spec.ts tests/notes/NoteMoreOptionsActions.spec.ts tests/components/form/RichMarkdownEditor.propertyRenameLocation.spec.ts
```

---

### 32. Optimize ranked tests 94–96
Type: Structure
Status: planned

**Tests:**
- #94 `tests/components/form/RichMarkdownEditor.propertyMemoryTrackerGuard.spec.ts` — RichMarkdownEditor property memory tracker guard > updates the tracker property key and emits renamed frontmatter when the user confirms (~13.5ms)
- #95 `tests/wiki-link-or-relationship/InsertWikiLink.spec.ts` — InsertWikiLink > does not call the inserter when Add a new relationship note is clicked (~13.4ms)
- #96 `tests/components/form/RichMarkdownEditor.properties.spec.ts` — RichMarkdownEditor properties > invalid YAML: hides Properties, shows alert, freezes Quill, ignores body edits (~13.4ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyMemoryTrackerGuard.spec.ts tests/wiki-link-or-relationship/InsertWikiLink.spec.ts tests/components/form/RichMarkdownEditor.properties.spec.ts
```

---

### 33. Optimize ranked tests 97–99
Type: Structure
Status: planned

**Tests:**
- #97 `tests/notes/sidebar/SidebarPeerSort.spec.ts` — Sidebar peer sort > lists folders above notes (A–Z) and reorders root peers when Title (Z–A) is chosen (~13.4ms)
- #98 `tests/notes/NoteNewForm.parentRelationship.spec.ts` — NoteNewForm parent relationship > submits parent frontmatter for Under current and Same parent choices (~13.3ms)
- #99 `tests/components/form/RichMarkdownEditor.properties.spec.ts` — RichMarkdownEditor properties > shows validation and does not emit corrupt duplicate keys when renaming a row (~13.3ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/sidebar/SidebarPeerSort.spec.ts tests/notes/NoteNewForm.parentRelationship.spec.ts tests/components/form/RichMarkdownEditor.properties.spec.ts
```

---

### 34. Optimize ranked tests 100–102
Type: Structure
Status: planned

**Tests:**
- #100 `tests/components/form/RichMarkdownEditor.propertyDeleteLocation.spec.ts` — RichMarkdownEditor focused property delete location > replaces to noteShow and does not show that the property is not found (~13.0ms)
- #101 `tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts` — RichMarkdownEditor property relation and index > readme-only predefined properties > readme-only fields are shown when note already has those keys in frontmatter (~13.0ms)
- #102 `tests/components/form/RichMarkdownEditor.propertyMemoryTrackerGuard.spec.ts` — RichMarkdownEditor property memory tracker guard > reverts the property key and does not emit when the user cancels a rename (~12.9ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyDeleteLocation.spec.ts tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts tests/components/form/RichMarkdownEditor.propertyMemoryTrackerGuard.spec.ts
```

---

### 35. Optimize ranked tests 103–105
Type: Structure
Status: planned

**Tests:**
- #103 `tests/notes/NoteTextContent.titleEdit.spec.ts` — NoteTextContent title edit > keeps unsaved title edits when props change (~12.8ms)
- #104 `tests/notes/NoteTextContent.titleEdit.spec.ts` — NoteTextContent title edit > does not save when title is 'spaces only' (~12.8ms)
- #105 `tests/pages/FolderPage.moveDestination.spec.ts` — FolderPage move destinations > move > sends destinationNotebookId and navigates after cross-notebook root move (~12.8ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteTextContent.titleEdit.spec.ts tests/pages/FolderPage.moveDestination.spec.ts
```

---

### 36. Optimize ranked tests 106–108
Type: Structure
Status: planned

**Tests:**
- #106 `tests/notes/NoteTextContent.titleEdit.spec.ts` — NoteTextContent title edit > does not save when title is 'mixed whitespace' (~12.7ms)
- #107 `tests/notes/NoteTextContent.titleEdit.spec.ts` — NoteTextContent title edit > displays authorization error when save is rejected with 401 (~12.7ms)
- #108 `tests/pages/FolderPage.moveConflict.spec.ts` — FolderPage move conflicts > move > shows inline error without merge prompt when move returns soft-deleted title conflict (~12.7ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteTextContent.titleEdit.spec.ts tests/pages/FolderPage.moveConflict.spec.ts
```

---

### 37. Optimize ranked tests 109–111
Type: Structure
Status: planned

**Tests:**
- #109 `tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` — AnsweredSpellingQuestion accidental match > omits mutating CTAs when reviewed notebook is readonly (~12.6ms)
- #110 `tests/notes/NoteNewForm.spec.ts` — adding new note > search for duplicate (~12.6ms)
- #111 `tests/notes/NoteTextContent.titleEdit.spec.ts` — NoteTextContent title edit > does not save when title is 'newlines only' (~12.6ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts tests/notes/NoteNewForm.spec.ts tests/notes/NoteTextContent.titleEdit.spec.ts
```

---

### 38. Optimize ranked tests 112–114
Type: Structure
Status: planned

**Tests:**
- #112 `tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts` — RichMarkdownEditor property relation and index > readme-only predefined properties > empty readme-only fields are not included in emitted YAML (~12.6ms)
- #113 `tests/commons/Modal.spec.ts` — Modal > focuses autofocus target and prefers text controls in a marked autofocus container (~12.5ms)
- #114 `tests/notes/NoteMoreOptionsForm.spec.ts` — NoteMoreOptionsForm > assimilation settings toggle > turns assimilation settings on without changing route (~12.5ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts tests/commons/Modal.spec.ts tests/notes/NoteMoreOptionsForm.spec.ts
```

---

### 39. Optimize ranked tests 115–117
Type: Structure
Status: planned

**Tests:**
- #115 `tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts` — RichMarkdownEditor property relation and index > removing every property row emits body-only markdown and shows add-only chrome without Properties heading (~12.5ms)
- #116 `tests/pages/FolderPage.moveConflict.spec.ts` — FolderPage move conflicts > move > shows merge confirm when move returns typed FOLDER_NAME_CONFLICT without status (~12.4ms)
- #117 `tests/components/form/RichMarkdownEditor.propertyValueDialog.spec.ts` — RichMarkdownEditor property value dialog > 'does not show value edit icon on spec…' (~12.4ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts tests/pages/FolderPage.moveConflict.spec.ts tests/components/form/RichMarkdownEditor.propertyValueDialog.spec.ts
```

---

### 40. Optimize ranked tests 118–120
Type: Structure
Status: planned

**Tests:**
- #118 `tests/components/recall/RefineNoteModal.extractNote.close.spec.ts` — RefineNoteModal extract note close > closes the refine note modal after creating a note from extraction preview (~12.4ms)
- #119 `tests/components/form/RichMarkdownEditor.properties.spec.ts` — RichMarkdownEditor properties > editing an existing property row emits renamed keys and updated values (~12.3ms)
- #120 `tests/components/form/SeamlessTextEditor.spec.ts` — SeamlessTextEditor > pastes plain text ('append when no selection') (~12.3ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/RefineNoteModal.extractNote.close.spec.ts tests/components/form/RichMarkdownEditor.properties.spec.ts tests/components/form/SeamlessTextEditor.spec.ts
```

---

### 41. Optimize ranked tests 121–123
Type: Structure
Status: planned

**Tests:**
- #121 `tests/notes/NoteTextContent.titleEdit.spec.ts` — NoteTextContent title edit > does not save when title is 'empty string' (~12.2ms)
- #122 `tests/notes/NoteUnresolvedWikiLinkModal.spec.ts` — NoteUnresolvedWikiLinkModal > soft keyboard primer > focuses primer then 'create' target on touch device (~12.2ms)
- #123 `tests/notes/WikidataAssociationDialog.titleActions.spec.ts` — WikidataAssociationDialog title actions and save > emits replace then append when showSaveButton is false (~12.2ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteTextContent.titleEdit.spec.ts tests/notes/NoteUnresolvedWikiLinkModal.spec.ts tests/notes/WikidataAssociationDialog.titleActions.spec.ts
```

---

### 42. Optimize ranked tests 124–126
Type: Structure
Status: planned

**Tests:**
- #124 `tests/notes/sidebar/SidebarRouteNavigation.spec.ts` — Sidebar route navigation: sticky realm during uncached note load > keeps sidebar chrome for an uncached same-notebook note, then clears active on leave (~12.2ms)
- #125 `tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts` — RichMarkdownEditor property relation and index > readme-only predefined properties > does not show readme-only predefined rows when isReadmeContext is false (~12.2ms)
- #126 `tests/components/recall/NoteRefinement.removeLayout.loading.spec.ts` — NoteRefinement remove layout loading modal > shows LoadingModal while removing refinement layout items and hides on success or failure (~12.2ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/sidebar/SidebarRouteNavigation.spec.ts tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts tests/components/recall/NoteRefinement.removeLayout.loading.spec.ts
```

---

### 43. Optimize ranked tests 127–129
Type: Structure
Status: planned

**Tests:**
- #127 `tests/notes/NoteUnresolvedWikiLinkModal.spec.ts` — NoteUnresolvedWikiLinkModal > soft keyboard primer > focuses primer then 'point-at-existing' target on touch device (~12.1ms)
- #128 `tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` — AnsweredSpellingQuestion accidental match > omits mutating CTAs when note realms are not loaded (~12.1ms)
- #129 `tests/components/recall/QuestionDisplay.thinking.spec.ts` — QuestionDisplay thinking time > records a detour when deactivated and reactivated (KeepAlive) (~12.1ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteUnresolvedWikiLinkModal.spec.ts tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts tests/components/recall/QuestionDisplay.thinking.spec.ts
```

---

### 44. Optimize ranked tests 130–132
Type: Structure
Status: planned

**Tests:**
- #130 `tests/notes/NoteToolbar.moreOptions.spec.ts` — NoteToolbar more options > toggles the audio tools panel from the inline button and overflow menu (~12.0ms)
- #131 `tests/pages/NotebookCatalogList.spec.ts` — catalog list > sorts title A–Z by default, Z–A when selected, then back to A–Z (~11.9ms)
- #132 `tests/components/form/RichMarkdownEditor.propertyValueDialog.spec.ts` — RichMarkdownEditor property value dialog > 'shows value edit icon on list propert…' (~11.8ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteToolbar.moreOptions.spec.ts tests/pages/NotebookCatalogList.spec.ts tests/components/form/RichMarkdownEditor.propertyValueDialog.spec.ts
```

---

### 45. Optimize ranked tests 133–135
Type: Structure
Status: planned

**Tests:**
- #133 `tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` — AnsweredSpellingQuestion accidental match > dismisses resolve dialog via close button and stays on accidental-match result (~11.8ms)
- #134 `tests/notes/sidebar/SidebarPeerSort.spec.ts` — Sidebar peer sort > keeps Title (Z–A) on a later visit after the tab session is gone (~11.6ms)
- #135 `tests/pages/NoteShowPage.spec.ts` — note show page > loads note by id from route (~11.4ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts tests/notes/sidebar/SidebarPeerSort.spec.ts tests/pages/NoteShowPage.spec.ts
```

---

### 46. Optimize ranked tests 136–138
Type: Structure
Status: planned

**Tests:**
- #136 `tests/notes/NoteNewForm.submit.spec.ts` — NoteNewForm submit > call the api (~11.4ms)
- #137 `tests/storybook/all-stories.spec.ts` — All Storybook Stories > Page Views/NotebooksPageView > renders WithNotebookGroup (~11.3ms)
- #138 `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` — NoteRefinement layout selection > preselects ledToQuestion items only when question context is provided (~11.3ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteNewForm.submit.spec.ts tests/storybook/all-stories.spec.ts tests/components/recall/NoteRefinement.layoutSelection.spec.ts
```

---

### 47. Optimize ranked tests 139–141
Type: Structure
Status: planned

**Tests:**
- #139 `tests/pages/BookReadingPage.readingControlPanel.marking.spec.ts` — BookReadingPage reading control panel marking > marking successor via auto-targeted panel advances selection past successor (~11.2ms)
- #140 `tests/notes/WikidataAssociationDialog.search.spec.ts` — WikidataAssociationDialog search and input > emits update:modelValue when user types a Wikidata ID (~11.2ms)
- #141 `tests/components/form/RichMarkdownEditor.propertyPanelLocation.spec.ts` — RichMarkdownEditor property panel location > opening the property panel replaces to noteProperty and opens the panel (~11.1ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/BookReadingPage.readingControlPanel.marking.spec.ts tests/notes/WikidataAssociationDialog.search.spec.ts tests/components/form/RichMarkdownEditor.propertyPanelLocation.spec.ts
```

---

### 48. Optimize ranked tests 142–144
Type: Structure
Status: planned

**Tests:**
- #142 `tests/components/form/SeamlessTextEditor.spec.ts` — SeamlessTextEditor > does not handle paste when 'empty clipboard' (~11.1ms)
- #143 `tests/components/recall/AssimilationPanel.loadingModal.spec.ts` — AssimilationPanel loading modal > keeps the global modal open from assimilate through next unit and hides on assimilate error (~11.0ms)
- #144 `tests/pages/RecallPage.spelling.spec.ts` — RecallPage spelling quiz > should handle spelling questions correctly (~10.9ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/SeamlessTextEditor.spec.ts tests/components/recall/AssimilationPanel.loadingModal.spec.ts tests/pages/RecallPage.spelling.spec.ts
```

---

### 49. Optimize ranked tests 145–147
Type: Structure
Status: planned

**Tests:**
- #145 `tests/notes/NoteEditableContent.spec.ts` — NoteEditableContent > updates displayed content on navigate, including clearing when content is undefined (~10.6ms)
- #146 `tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts` — RichMarkdownEditor property relation and index > relation property in rich mode > opens custom relation dialog prefilled and commits updated frontmatter (~10.6ms)
- #147 `tests/components/recall/NoteRefinement.extractNote.loading.spec.ts` — NoteRefinement extract note loading > shows LoadingModal during extract preview and hides on success or failure (~10.6ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableContent.spec.ts tests/components/form/RichMarkdownEditor.propertyRelationImageIndex.spec.ts tests/components/recall/NoteRefinement.extractNote.loading.spec.ts
```

---

### 50. Optimize ranked tests 148–150
Type: Structure
Status: planned

**Tests:**
- #148 `tests/components/form/RichMarkdownEditor.propertyPanelLocation.spec.ts` — RichMarkdownEditor property panel location > preserves unrelated query values when opening and closing the property panel (~10.5ms)
- #149 `tests/notes/NoteUnresolvedWikiLinkModal.spec.ts` — NoteUnresolvedWikiLinkModal > soft keyboard primer > does not focus primer on create tap when pointer is not coarse (~10.4ms)
- #150 `tests/components/admin/FailureReportList.spec.ts` — FailureReportList > trigger test exception > calls triggerFailure API and refreshes list when clicked (~10.4ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyPanelLocation.spec.ts tests/notes/NoteUnresolvedWikiLinkModal.spec.ts tests/components/admin/FailureReportList.spec.ts
```

---

### 51. Optimize ranked tests 151–153
Type: Structure
Status: planned

**Tests:**
- #151 `tests/components/form/SeamlessTextEditor.spec.ts` — SeamlessTextEditor > submits the nearest form on Enter (~10.4ms)
- #152 `tests/pages/BookReadingPage.readingPosition.spec.ts` — BookReadingPage reading position > does not restore reading position when no snapshot exists (~10.3ms)
- #153 `tests/wiki-link-or-relationship/SearchDialog.spec.ts` — SearchForm > Matches / Recent list mode > keeps search key and switches between Matches and Recent (~10.2ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/SeamlessTextEditor.spec.ts tests/pages/BookReadingPage.readingPosition.spec.ts tests/wiki-link-or-relationship/SearchDialog.spec.ts
```

---

### 52. Optimize ranked tests 154–156
Type: Structure
Status: planned

**Tests:**
- #154 `tests/notes/NoteEditableContent.debouncedSave.spec.ts` — NoteEditableContent debounced save > should not save until debounce when edit adds no new wiki link (~10.2ms)
- #155 `tests/notes/sidebar/SidebarAncestorLoading.spec.ts` — Sidebar gradual ancestor population > loads ancestor branches via folder listings, then shows them from cache on remount (~10.2ms)
- #156 `tests/storybook/all-stories.spec.ts` — All Storybook Stories > Recall/AssimilationPanel > renders SpellingAlreadyAssimilated (~10.1ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableContent.debouncedSave.spec.ts tests/notes/sidebar/SidebarAncestorLoading.spec.ts tests/storybook/all-stories.spec.ts
```

---

### 53. Optimize ranked tests 157–159
Type: Structure
Status: planned

**Tests:**
- #157 `tests/notes/NoteToolbar.moreOptions.spec.ts` — NoteToolbar more options > copies export markdown while keeping the export dialog open (~10.1ms)
- #158 `tests/components/form/RichMarkdownEditor.propertyFocus.spec.ts` — RichMarkdownEditor property focus from noteProperty > visiting noteProperty focuses the row, scrolls it into view, and opens its property panel (~10.1ms)
- #159 `tests/components/form/SeamlessTextEditor.spec.ts` — SeamlessTextEditor > keeps caret offset when modelValue is synced with same-length text (~10.1ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteToolbar.moreOptions.spec.ts tests/components/form/RichMarkdownEditor.propertyFocus.spec.ts tests/components/form/SeamlessTextEditor.spec.ts
```

---

### 54. Optimize ranked tests 160–162
Type: Structure
Status: planned

**Tests:**
- #160 `tests/notes/sidebar/SidebarActiveFolder.spec.ts` — Sidebar active folder > navigates to folderPage when the folder label is clicked (~10.1ms)
- #161 `tests/wiki-link-or-relationship/AddRelationship.spec.ts` — AddRelationshipFinalize > shows LoadingModal while creating relationship note (~10.0ms)
- #162 `tests/notes/NoteEditableContent.relationProperty.spec.ts` — NoteEditableContent relation property row in rich mode > shows relation type picker only when noteContent includes relation frontmatter (~9.9ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/sidebar/SidebarActiveFolder.spec.ts tests/wiki-link-or-relationship/AddRelationship.spec.ts tests/notes/NoteEditableContent.relationProperty.spec.ts
```

---

### 55. Optimize ranked tests 163–165
Type: Structure
Status: planned

**Tests:**
- #163 `tests/components/form/SeamlessTextEditor.spec.ts` — SeamlessTextEditor > does not handle paste when 'readonly' (~9.9ms)
- #164 `tests/pages/BookReadingPage.readingPosition.spec.ts` — BookReadingPage reading position > restores reading position from stored snapshot on open (~9.7ms)
- #165 `tests/components/recall/AnsweredQuestionComponent.spec.ts` — AnsweredQuestionComponent > refine note > shows Refine note, opens refine modal, and passes MCQ context when present (~9.7ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/SeamlessTextEditor.spec.ts tests/pages/BookReadingPage.readingPosition.spec.ts tests/components/recall/AnsweredQuestionComponent.spec.ts
```

---

### 56. Optimize ranked tests 166–168
Type: Structure
Status: planned

**Tests:**
- #166 `tests/components/recall/NoteRefinement.extractNote.spec.ts` — NoteRefinement extract note preview > displays one extract button and no per-item extract buttons (~9.6ms)
- #167 `tests/pages/BookReadingPage.navigationBar.spec.ts` — BookReadingPage navigation bar > shows navigation bar when current block differs from selected block (~9.5ms)
- #168 `tests/pages/RecallPage.viewHistoryThinkingTime.spec.ts` — thinking time while viewing a previously answered question > excludes time spent viewing the last answered question from the current question's thinking time (~9.5ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.extractNote.spec.ts tests/pages/BookReadingPage.navigationBar.spec.ts tests/pages/RecallPage.viewHistoryThinkingTime.spec.ts
```

---

### 57. Optimize ranked tests 169–171
Type: Structure
Status: planned

**Tests:**
- #169 `tests/pages/NoteShowPageConversation.spec.ts` — note show page conversation > clears conversation query without leaving the property location (~9.4ms)
- #170 `tests/notes/NoteNewForm.spec.ts` — adding new note > does not search for initial default 'Untitled' title (~9.3ms)
- #171 `tests/pages/BookReadingPage.snap.budgets.spec.ts` — BookReadingPage snap budgets > marking READ clears snap reminder: block no longer snaps when re-visited (~9.1ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/NoteShowPageConversation.spec.ts tests/notes/NoteNewForm.spec.ts tests/pages/BookReadingPage.snap.budgets.spec.ts
```

---

### 58. Optimize ranked tests 172–174
Type: Structure
Status: planned

**Tests:**
- #172 `tests/components/form/RichMarkdownEditor.propertyTouchFocus.spec.ts` — RichMarkdownEditor property touch focus > Add property on touch focuses primer then property key with 'no existing rows' (~9.1ms)
- #173 `tests/wiki-link-or-relationship/SearchDialog.spec.ts` — SearchForm > keyboard navigation > moves focus through results and back to search input with ArrowDown and ArrowUp (~9.1ms)
- #174 `tests/pages/BookReadingPage.readingPosition.spec.ts` — BookReadingPage reading position > debounces PATCH reading position; keeps last top; skips null viewport (~9.0ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyTouchFocus.spec.ts tests/wiki-link-or-relationship/SearchDialog.spec.ts tests/pages/BookReadingPage.readingPosition.spec.ts
```

---

### 59. Optimize ranked tests 175–177
Type: Structure
Status: planned

**Tests:**
- #175 `tests/wiki-link-or-relationship/SearchDialog.searchKeyHistory.spec.ts` — SearchForm search key history > lists cookie keys and fills the input when one is chosen (~8.9ms)
- #176 `tests/components/form/RichMarkdownEditor.properties.spec.ts` — RichMarkdownEditor properties > composes edited body with existing frontmatter when emitting updates (~8.9ms)
- #177 `tests/notes/NoteMoreOptionsForm.spec.ts` — NoteMoreOptionsForm > assimilation settings toggle > emits close-dialog when assimilation settings button is clicked (~8.8ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/wiki-link-or-relationship/SearchDialog.searchKeyHistory.spec.ts tests/components/form/RichMarkdownEditor.properties.spec.ts tests/notes/NoteMoreOptionsForm.spec.ts
```

---

### 60. Optimize ranked tests 178–180
Type: Structure
Status: planned

**Tests:**
- #178 `tests/components/recall/NoteRefinement.layoutSelection.spec.ts` — NoteRefinement layout selection > cascades parent selection and marks already extracted items without disabling (~8.8ms)
- #179 `tests/components/form/RichMarkdownEditor.spec.ts` — RichMarkdownEditor > preserves nested bullet indentation when pasting ChatGPT-style HTML (~8.7ms)
- #180 `tests/notes/NoteNewForm.spec.ts` — adding new note > searches when user edits title back to 'Untitled' (~8.7ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.layoutSelection.spec.ts tests/components/form/RichMarkdownEditor.spec.ts tests/notes/NoteNewForm.spec.ts
```

---

### 61. Optimize ranked tests 181–183
Type: Structure
Status: planned

**Tests:**
- #181 `tests/notes/NoteExportForm.spec.ts` — NoteExportForm > allows customizing token limit and refreshes graph (~8.6ms)
- #182 `tests/notes/NoteTextContent.titleEdit.saveRace.spec.ts` — NoteTextContent title edit save race > saves the last title after an earlier in-flight save finishes (~8.6ms)
- #183 `tests/pages/BookReadingPage.snap.spec.ts` — BookReadingPage snap > snaps back on first boundary crossing and when landing two+ blocks ahead (~8.6ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteExportForm.spec.ts tests/notes/NoteTextContent.titleEdit.saveRace.spec.ts tests/pages/BookReadingPage.snap.spec.ts
```

---

### 62. Optimize ranked tests 184–186
Type: Structure
Status: planned

**Tests:**
- #184 `tests/pages/RecallPageOverlap.spec.ts` — overlap try-again stay and retry > stays on the same tracker, skips threshold, and remounts spelling on Try again (~8.6ms)
- #185 `tests/components/recall/AnsweredSpellingQuestionAddAsOverlapped.spec.ts` — AnsweredSpellingQuestion add as overlapped note > adds as overlapped note via wiki-link content update without try-again (~8.6ms)
- #186 `tests/notes/NoteMoreOptionsActions.spec.ts` — NoteMoreOptionsActions keyboard shortcut > opens the export dialog when e is pressed (layout=menu) (~8.4ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/RecallPageOverlap.spec.ts tests/components/recall/AnsweredSpellingQuestionAddAsOverlapped.spec.ts tests/notes/NoteMoreOptionsActions.spec.ts
```

---

### 63. Optimize ranked tests 187–189
Type: Structure
Status: planned

**Tests:**
- #187 `tests/pages/RecallPage.treadmill.spec.ts` — RecallPage treadmill mode > should show treadmill mode toggle in settings (~8.4ms)
- #188 `tests/components/form/RichMarkdownEditor.propertyTouchFocus.spec.ts` — RichMarkdownEditor property touch focus > existing property value > does not focus primer when pointer is not coarse (~8.4ms)
- #189 `tests/pages/settings/RecallStatsSettingsTab.spec.ts` — RecallStatsSettingsTab > renders retention headline, charts, and best/worst hours from the fixture (~8.4ms)

**Goals:**

- Preserve the tests’ observable behavior while reducing redundant scenarios, repeated setup, expensive queries, or avoidable async work.
- Prefer deletion/merging of redundant coverage before narrower optimizations.
- If serious investigation finds no meaningful safe speedup, add a dated Candidate entry instead of forcing a weak change.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/RecallPage.treadmill.spec.ts tests/components/form/RichMarkdownEditor.propertyTouchFocus.spec.ts tests/pages/settings/RecallStatsSettingsTab.spec.ts
```

---

### 64. Re-profile and close
Type: Structure
Status: planned

Run the same full-suite JSON profile, record the after metrics and new top-10% table, mark the plan done, then perform spent-plan cleanup without committing the raw profile.

| Metric | Before | After |
|--------|--------|-------|
| Test count | 1,886 | |
| Suite wall | 122.44s | |
| Top 10% total CPU | 3,209.1ms | |

**Candidates proposed this run:** pending

**Commits:** pending
