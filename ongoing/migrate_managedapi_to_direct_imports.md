# Migration Plan: ManagedApi to Direct Service Imports (Option 3)

## Step 1: Create Global Client Setup Infrastructure ✅

**Goal:** Set up global client with interceptors without breaking existing functionality.

**Changes:**

- Create `frontend/src/managedApi/clientSetup.ts`:
- Import `createClient` from `@generated/backend/client`
- Import `client` as `globalClient` from `@generated/backend/client.gen`
- Create `globalClientSilent` instance (no interceptors)
- Create `setupGlobalClient(apiStatus: ApiStatus)` function that:
  - Creates `ApiStatusHandler` instance
  - Sets up `globalClient` config (baseUrl, credentials, responseStyle: 'fields', throwOnError: false)
  - Adds request/response/error interceptors to `globalClient` for loading states and error handling
  - Moves error handling logic from `ManagedApi.handleApiError` to interceptors
- Export both `globalClient` and `globalClientSilent`

**Verification:** All frontend unit tests pass.

---

## Step 2: Update ManagedApi to Use Separate Client Instance ✅

**Goal:** Prevent duplicate interceptors when ManagedApi and direct imports coexist.

**Changes:**

- Update `frontend/src/managedApi/ManagedApi.ts`:
- Import `createClient` from `@generated/backend/client`
- Import `globalClientSilent` from `./clientSetup`
- Create `private managedApiClient = createClient({...})` instance (no interceptors, responseStyle: 'fields', throwOnError: false)
- Create `private managedApiClientSilent = globalClientSilent` (reuse silent client)
- Update `wrapServices()` to pass appropriate client to each service call:
  - Normal: `{ ...options, client: this.managedApiClient }`
  - Silent: `{ ...options, client: this.managedApiClientSilent }`
- Keep all existing `wrapServiceCall`, `extractData`, and `handleApiError` logic unchanged

**Verification:** All frontend unit tests pass. ManagedApi tests verify no duplicate loading states.

---

## Step 3: Initialize Global Client in DoughnutApp ✅

**Goal:** Set up global client interceptors for future direct imports.

**Changes:**

- Update `frontend/src/DoughnutApp.vue`:
- Import `setupGlobalClient` from `@/managedApi/clientSetup`
- Call `setupGlobalClient(apiStatus.value)` after creating `apiStatus` but before creating `managedApi`
- This ensures global client interceptors are active

**Verification:** All frontend unit tests pass. Global client interceptors work but don't interfere with ManagedApi.

---

## Step 4: Migrate Simple Leaf Components (No Dependencies, Non-Silent) ✅

**Goal:** Migrate components that don't depend on other migrated components and don't use silent mode.

**Strategy:** Start with components that:

- Only use `managedApi.services.*` directly (not `.silent.services.*`)
- Don't use `storageAccessor` or `StoredApiCollection`
- Are not dependencies of other components

**Example candidates:** `ManageMCPTokensPage.vue`, `UserProfileDialog.vue`, simple admin components

**Changes per component:**

- Replace `useLoadingApi()` with direct service imports
- Import services directly from `@generated/backend/sdk.gen`
- Replace `managedApi.services.getUserProfile()` with `getUserProfile()`
- Remove `managedApi` from component if no longer needed
- Update component tests to use direct services (build test helpers as needed)

**Verification:** After each component migration, run frontend unit tests. All must pass.

**Migrated Components:**
- ✅ `ManageMCPTokensPage.vue`
- ✅ `UserProfileDialog.vue`
- ✅ `AnsweredQuestionPage.vue`
- ✅ `RecentlyAddedNotes.vue`
- ✅ `NotebooksPage.vue`
- ✅ `AssessmentPage.vue`
- ✅ `MemoryTrackerAsync.vue`
- ✅ `NoteAccessoryAsync.vue`
- ✅ `DoughnutApp.vue` (partial - direct service calls for `getFeatureToggle` and `currentUserInfo`)
- ✅ `WikidataAssociationDialog.vue` (both `searchWikidata` and `fetchWikidataEntityDataById`)

---

## Step 5: Migrate Silent Service Calls ✅

**Goal:** Migrate components that use `managedApi.silent.services.*` to direct imports with silent client.

**Strategy:** Find all usages of `managedApi.silent.services.*` and migrate them.

**Changes per component:**

- Replace `managedApi.silent.services.askAQuestion()` with `askAQuestion({ client: globalClientSilent })`
- Import `globalClientSilent` from `@/managedApi/clientSetup`
- Import services directly from `@generated/backend/sdk.gen`
- Update component tests accordingly (build test helpers as needed)

**Verification:** After each component migration, run frontend unit tests. All must pass.

**Migrated Components:**
- ✅ `Quiz.vue` - migrated from `managedApi.silent.services.askAQuestion` to `askAQuestion({ client: globalClientSilent })`

---

## Step 6: Migrate Components Using StorageAccessor (Keep ManagedApi for Storage)

**Goal:** Migrate components that use storageAccessor but don't need ManagedApi directly.

**Strategy:** These components use `storageAccessor.storedApi()` which still uses ManagedApi internally. We can migrate the component's direct API calls while keeping storageAccessor unchanged.

**Changes per component:**

- Migrate direct `managedApi.services.*` calls to direct imports
- Keep `storageAccessor` usage unchanged (it still uses ManagedApi internally)
- Example: Components that fetch data directly but also use storageAccessor for note operations
- Update component tests (build test helpers as needed)

**Verification:** After each component migration, run frontend unit tests. All must pass.

**Components to Migrate:**
- `NoteConversation.vue` - uses `managedApi.services.getConversationsAboutNote`, `startConversationAboutNote`
- `NoteAudioTools.vue` - uses `managedApi.services.suggestTitle`, `audioToText`
- `ConversationInner.vue` - uses `managedApi.services.getConversationMessages`, `replyToConversation`
- `ConversationComponent.vue` - uses `managedApi.services.getConversationsAboutNote`

---

## Step 7: Migrate StoredApiCollection to Use Direct Services

**Goal:** Update StoredApiCollection to use direct services instead of ManagedApi.

**Changes:**

- Update `frontend/src/store/StoredApiCollection.ts`:
- Remove `managedApi: ManagedApi` property
- Import services directly from `@generated/backend/sdk.gen`
- Replace all `this.managedApi.services.*` calls with direct service calls
- Update constructor to remove `managedApi` parameter
- Update `frontend/src/store/createNoteStorage.ts`:
- Remove `managedApi: ManagedApi` parameter and property
- Update `StoredApiCollection` instantiation to not pass managedApi
- Update `frontend/src/DoughnutApp.vue`:
- Update `createNoteStorage(managedApi)` call to `createNoteStorage()`
- Update all test files that create `StoredApiCollection` or `createNoteStorage`:
- Remove `managedApi` parameter from constructors/calls
- Build test helpers as needed

**Verification:** All frontend unit tests pass. Storage operations work correctly.

---

## Step 8: Migrate Remaining Components

**Goal:** Migrate all remaining components that still use ManagedApi.

**Changes:**

- Migrate remaining components following Step 4 pattern
- Update components that use `managedApi.silent` following Step 5 pattern
- Remove `useLoadingApi()` imports where no longer needed
- Update component tests (build test helpers as needed)

**Verification:** After each component migration, run frontend unit tests. All must pass.

---

## Step 9: Remove ManagedApi (Final Cleanup)

**Goal:** Remove ManagedApi entirely once all usages are migrated.

**Prerequisites:** Verify no files import or use ManagedApi anymore.

**Changes:**

- Remove `frontend/src/managedApi/ManagedApi.ts`
- Remove `frontend/src/managedApi/useLoadingApi.ts` (or keep as compatibility shim if needed)
- Update `frontend/src/DoughnutApp.vue`:
- Remove ManagedApi import and instantiation
- Remove `provide("managedApi", managedApi)`
- Keep `setupGlobalClient(apiStatus.value)` call
- Update test helpers:
- Remove ManagedApi from RenderingHelper if no longer needed
- Update tests to use direct services only
- Remove ManagedApi-specific tests or convert them to test global client interceptors

**Verification:** All frontend unit tests pass. No references to ManagedApi remain.

---

## Step 10: Update Documentation

**Goal:** Update all documentation to reflect the new direct service import pattern.

**Changes:**

- Update `.cursor/rules/generated-backend-api-code-for-frontend.mdc`:
- Remove references to ManagedApi wrapper
- Document direct service import pattern
- Update examples to show direct imports from `@generated/backend/sdk.gen`
- Document `globalClientSilent` usage for silent calls
- Document `setupGlobalClient` initialization
- Update any other relevant documentation files that mention ManagedApi or the old API pattern
- Update README or developer guides if they reference the API calling pattern

**Verification:** All frontend unit tests pass. Documentation accurately reflects the new pattern.

---

## Migration Order Summary

1. Infrastructure setup (Steps 1-3): ✅ Complete
2. Component migration (Steps 4-6): Step 4 ✅ Complete, Step 5 ✅ Complete, Step 6 pending
3. Storage migration (Step 7): Pending
4. Remaining migration (Step 8): Pending
5. Cleanup (Step 9): Pending
6. Documentation (Step 10): Pending

Each step maintains backward compatibility until the final cleanup. Test helpers are built incrementally as components are migrated.

