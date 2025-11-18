# Wikidata Association Unification - Current Behavior Documentation

## Current Flows

### Creation Flow
**Path**: `NoteNewDialog.vue` → `WikidataSearchByLabel.vue` → `WikidataSearchDialog.vue`

**Behavior**:
1. User enters a note title in `NoteNewDialog`
2. User clicks Wikidata button (via `WikidataSearchByLabel`)
3. `WikidataSearchDialog` opens with:
   - `searchKey` prop = current note title (from `creationData.newTitle`)
   - `currentTitle` prop = current note title
   - `modelValue` = current `wikidataId` (if any)
4. Dialog automatically searches Wikidata using `searchKey` via `managedApi.services.searchWikidata({ search: searchKey })`
5. Search results displayed in a dropdown select
6. User can also manually type a Wikidata ID in the input field
7. When user selects a result:
   - If titles match (case-insensitive): emits `selected` event immediately with entity (no titleAction)
   - If titles differ: shows radio buttons for "Replace" or "Append" title
   - User chooses action, then emits `selected` event with entity and titleAction ("replace" | "append")
8. `NoteNewDialog` handles the `selected` event:
   - Updates `creationData.wikidataId` with selected entity ID
   - If titleAction === "replace": sets `creationData.newTitle = selectedSuggestion.label`
   - If titleAction === "append": sets `creationData.newTitle = "${currentTitle} / ${selectedSuggestion.label}"`
   - If no titleAction (titles match): sets `creationData.newTitle = selectedSuggestion.label` (exact label from Wikidata)
9. Dialog closes after selection
10. User submits form, which calls `createNote` or `createNoteAfter` with `creationData` (including `wikidataId`)

**Key characteristics**:
- Uses note title as `searchKey` for automatic search
- Emits events only, does NOT save
- Provides Replace/Append options for title conflicts
- Modal UI component

### Edit Flow
**Path**: `NoteToolbar.vue` → `WikidataAssociationDialog.vue`

**Behavior**:
1. User clicks "associate wikidata" or "Edit Wikidata ID" button in toolbar
2. `WikidataAssociationDialog` opens with:
   - `note` prop = current note object
   - `storageAccessor` prop = storage accessor for API calls
   - Initial `wikidataId` = `note.wikidataId` (if exists)
3. Dialog shows:
   - Text input for Wikidata ID (no search functionality)
   - Error message display
4. User manually types a Wikidata ID
5. User clicks "Save"
6. Dialog validates by calling `managedApi.services.fetchWikidataEntityDataById({ wikidataId })`
7. If validation succeeds:
   - If `WikidataTitleInEnglish` is empty OR matches note title (case-insensitive): saves immediately via `storageAccessor.storedApi().updateWikidataId(note.id, { wikidataId })`
   - If `WikidataTitleInEnglish` differs from note title: shows confirmation dialog asking "Confirm to associate [note title] with [wikidata title]?"
     - User can Cancel (returns to input form)
     - User can Confirm (saves via `updateWikidataId`)
8. On save success: emits `closeDialog` event
9. On error: displays error message in the input field

**Key characteristics**:
- NO search functionality - only manual input
- Saves directly via API
- Shows simple Confirm/Cancel for title conflicts (not Replace/Append)
- Card UI component (not Modal)

## API Methods Used

1. **`searchWikidata({ search: string })`**: Returns `WikidataSearchEntity[]`
   - Used in: Creation flow only
   - Searches Wikidata by label/text

2. **`fetchWikidataEntityDataById({ wikidataId: string })`**: Returns `WikidataEntityData`
   - Used in: Edit flow only
   - Validates and fetches entity data by ID

3. **`updateWikidataId(noteId: number, { wikidataId: string })`**: Updates note's Wikidata association
   - Used in: Edit flow only
   - Saves the association

## Desired Unified Behavior

After unification, both flows should:
1. **Always support search**: Use note title as `searchKey` in both creation and edit flows
2. **Consistent conflict handling**: Use Replace/Append options in both flows (not Confirm/Cancel)
3. **Unified UI**: Use Modal component consistently
4. **Save semantics**: 
   - Creation: Emit events (parent handles save as part of note creation)
   - Edit: Save directly via API (immediate persistence)

## Test Coverage

### Unit Tests
- `frontend/tests/notes/WikidataSearchDialog.spec.ts`: Tests search dialog behavior (creation flow)
  - ✓ Tests automatic search when `searchKey` changes
  - ✓ Tests title matching (case-insensitive)
  - ✓ Tests Replace/Append options when titles differ
  - ✓ Tests manual ID input
  - **Status**: All 13 tests passing - accurately captures creation flow behavior

- `frontend/tests/notes/WikidataAssociationDialog.spec.ts`: Tests edit dialog behavior
  - ✓ Tests manual ID input (no search)
  - ✓ Tests validation via `fetchWikidataEntityDataById`
  - ✓ Tests Confirm/Cancel flow for title conflicts
  - ✓ Tests direct save via `updateWikidataId`
  - **Status**: All 6 tests passing - accurately captures edit flow behavior

- `frontend/tests/notes/NoteNewDialog.spec.ts`: Tests creation flow with Wikidata integration
  - ✓ Tests search integration
  - ✓ Tests title replacement/append behavior
  - ✓ Tests dialog open/close
  - **Status**: All 11 tests passing - accurately captures integration

### E2E Tests
- `e2e_test/features/wikidata/note_create_with_wikidata_id.feature`: Creation flow
  - Tests search, selection, title updates, validation errors
- `e2e_test/features/wikidata/associate_wikidata.feature`: Edit flow
  - Tests manual input, validation, confirmation flow
- `e2e_test/features/wikidata/associate_wikidata_*.feature`: Various entity types
  - Tests person, location, book entities

**Test Status**: All unit tests passing (382 passed, 3 skipped). Tests accurately capture current behavior differences:
- Creation flow: Has search, uses Replace/Append
- Edit flow: No search, uses Confirm/Cancel

**Note**: E2E tests for edit flow do NOT test search functionality because it doesn't exist in the current implementation. After unification, these tests will need to be updated to include search scenarios.

