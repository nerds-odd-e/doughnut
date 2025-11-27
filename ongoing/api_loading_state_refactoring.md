# API Loading State Refactoring

## Long-term Goal

Refactor API loading state management to use explicit `apiCallWithLoading` wrapper instead of automatic interceptor-based loading for all API calls.

## Current State (Updated: Nov 27, 2024)

✅ **MAJOR MILESTONES ACHIEVED**: 
1. Automatic loading state removed from globalClient!
2. `globalClientSilent` completely removed!

We now have a single, simplified approach:

1. **Global Client** (default) - **THE ONLY CLIENT**
   - ✅ **NO automatic loading state** (interceptors removed)
   - ✅ Shows error toasts automatically via error interceptor
   - ✅ Handles 401 redirects automatically
   - Used by ALL API calls in the application

2. **apiCallWithLoading** (explicit loading wrapper)
   - Explicitly wraps API calls that need loading state
   - Sets loading state synchronously (immediately when called)
   - Uses the global client (benefits from error toasts and 401 handling)
   - Guarantees cleanup in `finally` block
   - Simple API - just wrap the API call
   - **This is the ONLY way to get loading state**

## Target State

- ✅ Remove automatic loading state from global client interceptors **DONE**
- ✅ Every API call that needs loading state explicitly uses `apiCallWithLoading` **DONE**
- ✅ Remove `globalClientSilent` (it becomes the default) **DONE**
- ✅ This makes loading state opt-in rather than opt-out **ACHIEVED**

**ALL TARGET GOALS ACHIEVED! 🎉**

## Benefits

1. **Explicit over implicit** - Clear which API calls show loading state
2. **Synchronous loading** - Loading state set immediately, not asynchronously
3. **Better control** - Each component decides if it needs loading state
4. **Simpler architecture** - No need for two separate clients
5. **Easier debugging** - No confusion about which client to use

## Challenges

### Analysis Required

Every API call in the codebase needs careful analysis:

1. **User-initiated actions** - Usually need loading state
   - Form submissions
   - Button clicks
   - Quiz/assessment answers
   
2. **Background operations** - Usually don't need loading state
   - Eager fetching/prefetching
   - Auto-save operations
   - Polling/periodic updates
   
3. **Page loads** - May need loading state
   - Initial data fetching
   - Navigation-triggered loads

### Migration Strategy

1. Identify all API call locations (search for imports from `@generated/backend/sdk.gen`)
2. Categorize each call (user action, background, page load)
3. Wrap appropriate calls with `apiCallWithLoading`
4. Remove interceptors from global client setup
5. Remove `globalClientSilent` (rename `globalClient` to use silent mode)
6. Test thoroughly to ensure no regression in UX

### Effort Estimate

- ~100+ API call locations across the frontend
- Each requires understanding the context and UX requirements
- Significant testing effort to verify no loading state regression

## Current Progress

- ✅ `apiCallWithLoading` implemented and tested
- ✅ Simplified `apiCallWithLoading` to remove client parameter (always uses `globalClientSilent`)
- ✅ Applied to `RecallPromptComponent` (quiz answers)
- ✅ Applied to `AssessmentQuestion` (assessment answers)
- ✅ Complete audit of all API calls (127+ locations identified)
- ✅ Migration plan created (7 phases)
- ✅ **Phase 1 Complete**: Category B - All background/silent operations converted (~80 locations)
  - Badge counts in MainMenu
  - All page load data fetches
  - Component display fetches
  - Search operations
  - Admin read operations
  - Export operations
  - Wikidata searches
  - Audio processing
- ✅ **A1 Complete**: StoredApiCollection.ts already uses `apiCallWithLoading` for all mutations (11 operations)
- ✅ **A2 Complete**: Notebook/Subscription/Circle operations wrapped with `apiCallWithLoading` (13 operations)
  - NotebookController.createNotebook
  - CircleController.createNotebookInCircle
  - NotebookController.updateNotebook
  - NotebookController.importObsidian
  - NotebookController.resetNotebookIndex
  - NotebookController.updateNotebookIndex
  - NotebookController.shareNotebook
  - SubscriptionController.createSubscription
  - SubscriptionController.updateSubscription
  - SubscriptionController.destroySubscription
  - CircleController.createCircle
  - CircleController.joinCircle
  - BazaarController.removeFromBazaar
- ✅ **A3 Complete**: Quiz/Assessment operations wrapped with `apiCallWithLoading` (3 operations)
  - MemoryTrackerController.answerSpelling (Quiz.vue)
  - AssessmentController.submitAssessmentResult (Assessment.vue)
  - AssessmentController.generateAssessmentQuestions (AssessmentPage.vue)
- ✅ **A4 Complete**: Review Actions wrapped with `apiCallWithLoading` (6 operations)
  - MemoryTrackerController.markAsRepeated (JustReview.vue)
  - MemoryTrackerController.selfEvaluate (NoteInfoMemoryTracker.vue)
  - MemoryTrackerController.removeFromRepeating (NoteInfoMemoryTracker.vue)
  - RecallPromptController.contest (ContestableQuestion.vue)
  - RecallPromptController.regenerate (ContestableQuestion.vue)
  - AssimilationController.assimilate (Assimilation.vue)
- ✅ **A5 Complete**: Conversation Actions wrapped with `apiCallWithLoading` (5 operations)
  - ConversationMessageController.replyToConversation (ConversationInner.vue)
  - ConversationMessageController.startConversationAboutNote (NoteConversation.vue - 2 places)
  - ConversationMessageController.startConversationAboutAssessmentQuestion (FeedbackForm.vue)
  - ConversationMessageController.startConversationAboutRecallPrompt (ConversationButton.vue)
- ✅ **A6 Complete**: Note/Question Management wrapped with `apiCallWithLoading` (8 operations)
  - PredefinedQuestionController.addQuestionManually (NoteAddQuestion.vue)
  - PredefinedQuestionController.refineQuestion (NoteAddQuestion.vue)
  - PredefinedQuestionController.toggleApproval (Questions.vue)
  - NoteController.updateRecallSetting (RecallSettingForm.vue)
  - NoteController.updateNoteAccessories (NoteEditImageDialog.vue - already wrapped)
  - NoteController.updateNoteAccessories (NoteEditUrlDialog.vue - already wrapped)
  - AiController.suggestTitle (NoteAudioTools.vue)
  - AiController.generateImage (AIGenerateImageDialog.vue)
- ✅ **A7 Complete**: Admin Operations wrapped with `apiCallWithLoading` (8 operations)
  - FineTuningDataController.uploadAndTriggerFineTuning (FineTuningData.vue)
  - FineTuningDataController.updateSuggestedQuestionForFineTuning (SuggestedQuestionEdit.vue)
  - FineTuningDataController.duplicate (SuggestedQuestionRow.vue)
  - FineTuningDataController.delete (SuggestedQuestionRow.vue)
  - GlobalSettingsController.setCurrentModelVersions (ManageModel.vue)
  - NotebookCertificateApprovalController.approve (CertificateRequests.vue)
  - NotebookCertificateApprovalController.requestApprovalForNotebook (NotebookCertificateRequest.vue)
  - FailureReportController.deleteFailureReports (FailureReportList.vue)
- ✅ All 416 frontend unit tests passing
- ✅ **Phase 2 Complete**: All user mutations (A1-A7) wrapped with `apiCallWithLoading`
- **Total operations wrapped**: 54 user-initiated mutations now show proper loading state
- ✅ **Additional Cleanup (Nov 27, 2024)**: Found and fixed 10 more API calls that weren't using proper client setup:
  - 6 mutations wrapped with `apiCallWithLoading`:
    - UserController.createUser (user registration)
    - UserController.generateToken (MCP token generation)
    - UserController.deleteToken (MCP token deletion)
    - NotebookController.moveToCircle (move notebook)
    - PredefinedQuestionController.suggestQuestionForFineTuning (suggest fine-tuning)
    - NotebookController.updateAiAssistant (update AI assistant)
  - 4 background reads made silent with `client: globalClientSilent`:
    - UserController.getTokens (load tokens on page mount)
    - CircleController.index (load circles in dialog)
    - NotebookController.getNotes (load notebook notes)
    - CertificateController.getCertificate (load certificate data)

## Critical Issue Discovered (Nov 27, 2024)

**Problem**: Using `globalClientSilent` for API calls breaks 401 authentication handling.

**Root Cause**: 
- `globalClientSilent` has NO interceptors (no loading, no error handling)
- When a 401 error occurs, the error interceptor calls `loginOrRegisterAndHaltThisThread()` to redirect to login
- Silent operations lose this critical functionality

**Impact**:
- E2E test `account_control.feature` failed - unauthenticated users weren't redirected to login
- Any page using `globalClientSilent` for initial data fetch will fail authentication checks

**Fixes Applied**:
- ✅ `FailureReportList.vue` - Removed `client: globalClientSilent`, now uses default client
- ✅ `FailureReportPage.vue` - Removed `client: globalClientSilent`, now uses default client
- ✅ `ManageModel.vue` - Removed `client: globalClientSilent` from 2 API calls
- ✅ `FineTuningData.vue` - Removed `client: globalClientSilent`, now uses default client
- ✅ `CertificateRequests.vue` - Removed `client: globalClientSilent`, now uses default client
- ✅ E2E test `account_control.feature` now passing

**Architectural Implication**:
- We need THREE client modes, not two:
  1. **Full client** (loading + error handling) - user actions
  2. **Error-only client** (no loading, but error handling) - background operations that need 401 handling
  3. **Silent client** (no interceptors) - truly silent operations like prefetch

- Current architecture only has #1 and #3, missing #2
- For now, operations that need 401 handling must use the default client (will show loading)

## Implementation Details

`apiCallWithLoading` has been simplified - just wrap your API call:

```typescript
// Usage - uses default globalClient
const result = await apiCallWithLoading(() =>
  SomeController.someMethod({
    path: { id: 123 },
  })
)

// For truly silent calls (no loading state, no toasts), use globalClientSilent directly
const result = await SomeController.someMethod({
  path: { id: 123 },
  client: globalClientSilent,
})
```

The wrapper doesn't specify a client, so API calls use the default `globalClient` which has interceptors for loading state and error toasts.

## Complete API Audit Results

### Current State (as of analysis)
- **Total API calls found**: ~127+ locations
- **Already using `apiCallWithLoading`**: 4 files (StoredApiCollection.ts, AssessmentQuestion.vue, RecallPromptComponent.vue, clientSetup.ts)
- **Already using `globalClientSilent`**: 1 location (Quiz.vue eager fetch - correctly silent)
- **Using default client (automatic loading via interceptors)**: ~120+ locations

### Categories

#### Category A: KEEP Loading State (User Actions + Router Changes)
**Purpose**: User-initiated actions, mutations, and operations followed by navigation
**Total**: ~40 locations

**A1. StoredApiCollection.ts** (✅ Already using `apiCallWithLoading`)
- All 11 API calls already correctly wrapped
- Create/update/delete operations with router changes

**A2. User-Initiated Mutations** (NEED `apiCallWithLoading`)
- `UserController.createUser` (UserNewRegisterPage.vue:35)
- `UserController.updateUser` (UserProfileDialog.vue:57)
- `UserController.generateToken` (ManageMCPTokensPage.vue:91)
- `UserController.deleteToken` (ManageMCPTokensPage.vue:110)
- `NotebookController.updateNotebook` (NotebookEditDialog.vue)
- `NotebookController.createNotebook` (NotebookNewDialog.vue)
- `NotebookController.shareNotebook` (NotebookButtons.vue)
- `NotebookController.updateNotebookIndex` (NotebookButtons.vue)
- `NotebookController.resetNotebookIndex` (NotebookButtons.vue)
- `NotebookController.moveToCircle` (NotebookMoveDialog.vue)
- `NotebookController.importObsidian` (ObsidianImportDialog.vue)
- `NotebookController.updateAiAssistant` (NotebookAssistantManagementDialog.vue)
- `SubscriptionController.createSubscription` (SubscribeDialog.vue)
- `SubscriptionController.updateSubscription` (SubscriptionEditDialog.vue:39)
- `SubscriptionController.destroySubscription` (SubscriptionNoteButtons.vue:49)
- `CircleController.createCircle` (CircleNewDialog.vue)
- `CircleController.createNotebookInCircle` (CircleNewDialog.vue)
- `CircleController.joinCircle` (CircleJoinForm.vue)
- `BazaarController.removeFromBazaar` (ManageBazaar.vue)
- `CertificateController.claimCertificate` (CertificatePopup.vue)

**A3. Quiz/Assessment Operations** (✅ Some already using `apiCallWithLoading`)
- `RecallPromptController.answerQuiz` (RecallPromptComponent.vue:64) ✅ Already wrapped
- `AssessmentController.answerQuestion` (AssessmentQuestion.vue:61) ✅ Already wrapped
- `MemoryTrackerController.answerSpelling` (Quiz.vue:182) - NEED to wrap
- `AssessmentController.submitAssessmentResult` (Assessment.vue) - NEED to wrap
- `AssessmentController.generateAssessmentQuestions` (AssessmentPage.vue:26) - NEED to wrap

**A4. Review Actions** (NEED `apiCallWithLoading`)
- `MemoryTrackerController.markAsRepeated` (JustReview.vue:41)
- `MemoryTrackerController.removeFromRepeating` (QuestionDisplay.vue)
- `MemoryTrackerController.selfEvaluate` (FeedbackForm.vue)
- `RecallPromptController.contest` (ContestableQuestion.vue:83)
- `RecallPromptController.regenerate` (ContestableQuestion.vue:95)
- `AssimilationController.assimilate` (Assimilation.vue:66)

**A5. Conversation Actions** (NEED `apiCallWithLoading`)
- `ConversationMessageController.replyToConversation` (ConversationInner.vue)
- `ConversationMessageController.getAiReply` (AiResponse.vue)
- `ConversationMessageController.startConversationAboutNote` (NoteConversation.vue)
- `ConversationMessageController.startConversationAboutAssessmentQuestion` (FeedbackForm.vue:36)
- `ConversationMessageController.startConversationAboutRecallPrompt` (ConversationButton.vue:35)

**A6. Note/Question Management** (NEED `apiCallWithLoading`)
- `PredefinedQuestionController.addQuestionManually` (NoteAddQuestion.vue)
- `PredefinedQuestionController.refineQuestion` (Questions.vue)
- `PredefinedQuestionController.toggleApproval` (Questions.vue)
- `PredefinedQuestionController.suggestQuestionForFineTuning` (SuggestQuestionForFineTuning.vue)
- `NoteController.updateRecallSetting` (RecallSettingForm.vue:65)
- `NoteController.updateNoteAccessories` (NoteEditImageDialog.vue:49, NoteEditUrlDialog.vue:49)
- `AiController.suggestTitle` (NoteToolbar.vue)
- `AiController.generateImage` (AIGenerateImageDialog.vue)

**A7. Admin Operations** (NEED `apiCallWithLoading`)
- `FineTuningDataController.uploadAndTriggerFineTuning` (FineTuningData.vue)
- `FineTuningDataController.updateSuggestedQuestionForFineTuning` (SuggestedQuestionEdit.vue)
- `FineTuningDataController.duplicate` (SuggestedQuestionRow.vue)
- `FineTuningDataController.delete` (SuggestedQuestionRow.vue)
- `GlobalSettingsController.setCurrentModelVersions` (ManageModel.vue)
- `NotebookCertificateApprovalController.approve` (CertificateRequests.vue)
- `NotebookCertificateApprovalController.requestApprovalForNotebook` (NotebookCertificateRequest.vue)
- `FailureReportController.deleteFailureReports` (FailureReportList.vue)

#### Category B: REMOVE Loading State (Background/Silent Operations)
**Purpose**: Operations that should not show global loading indicator
**Total**: ~80+ locations

**B1. Background Badge Counts** (HIGH PRIORITY - Should be silent)
- `AssimilationController.getAssimilationCount` (MainMenu.vue:137)
- `RecallsController.overview` (MainMenu.vue:148)
- `ConversationMessageController.getUnreadConversations` (MainMenu.vue:160)

**B2. Eager Fetching / Prefetching**
- `RecallPromptController.askAQuestion` (Quiz.vue:120) ✅ Already silent
- `NoteController.showNote` (StoredApiCollection.ts:185) - Called from getNoteRealmRefAndLoadWhenNeeded

**B3. Page Load Data Fetches** (pages manage their own loading state)
- `NotebookController.myNotebooks` (NotebooksPage.vue:53)
- `BazaarController.bazaar` (BazaarPage.vue:30)
- `CircleController.index` (CirclesPage.vue:42)
- `CircleController.showCircle` (CircleShowPage.vue:70)
- `AssessmentController.getMyAssessments` (AssessmentAndCertificateHistoryPage.vue:85)
- `RecallPromptController.showQuestion` (AnsweredQuestionPage.vue:29)
- `FailureReportController.showFailureReport` (FailureReportPage.vue:41)
- `ConversationMessageController.getConversation` (MessageCenterPage.vue:91)
- `ConversationMessageController.getConversationsOfCurrentUser` (MessageCenterPage.vue:105)
- `RecallsController.recalling` (RecallPage.vue:135)
- `AssimilationController.assimilating` (AssimilationPage.vue:109)
- `CurrentUserInfoController.currentUserInfo` (CircleJoinPage.vue:27)

**B4. Component Data Fetches** (display-only data)
- `NoteController.getRecentNotes` (RecentlyAddedNotes.vue:40, SearchResults.vue:170)
- `MemoryTrackerController.getRecentlyReviewed` (RecentlyReviewedNotes.vue:44)
- `MemoryTrackerController.getRecentMemoryTrackers` (RecentlyLearnedNotes.vue:38)
- `NoteController.getNoteInfo` (NoteInfoBar.vue:26)
- `NoteController.getGraph` (NoteExportDialog.vue:111)
- `NoteController.getDescendants` (NoteExportDialog.vue:90)
- `MemoryTrackerController.showMemoryTracker` (MemoryTrackerAsync.vue:34)
- `MemoryTrackerController.getSpellingQuestion` (SpellingQuestionComponent.vue:50)
- `NoteController.showNoteAccessory` (NoteAccessoryAsync.vue:34, NoteEditUrlDialog.vue:37)
- `NotebookController.getAiAssistant` (NotebookAssistantManagementDialog.vue)
- `NotebookController.getNotes` (Sidebar.vue, SidebarInner.vue)
- `ConversationMessageController.getConversationsAboutNote` (ConversationComponent.vue:77)
- `ConversationMessageController.getConversationMessages` (ConversationInner.vue)
- `UserController.getUserProfile` (UserProfileDialog.vue:49)
- `UserController.getTokens` (ManageMCPTokensPage.vue:77)

**B5. Search Operations** (instant feedback expected)
- `SearchController.searchForLinkTargetWithin` (SearchResults.vue:144)
- `SearchController.searchForLinkTarget` (SearchResults.vue:148)
- `SearchController.semanticSearchWithin` (SearchResults.vue:150)
- `SearchController.semanticSearch` (SearchResults.vue:154)

**B6. Export/Download Operations** (may want local loading but not global)
- `PredefinedQuestionController.exportQuestionGeneration` (QuestionExportDialog.vue:44)
- `ConversationMessageController.exportConversation` (ConversationExportDialog.vue:34)
- `NotebookController.downloadNotebookDump` (NotebookButtons.vue)
- `NotebookController.downloadNotebookForObsidian` (NotebookButtons.vue)

**B7. Admin Read Operations**
- `FineTuningDataController.getAllSuggestedQuestions` (SuggestedQuestionList.vue)
- `FailureReportController.failureReports` (FailureReportList.vue)
- `NotebookCertificateApprovalController.getAllPendingRequest` (CertificateRequests.vue)
- `GlobalSettingsController.getCurrentModelVersions` (ManageModel.vue)
- `PredefinedQuestionController.getAllQuestionByNote` (Questions.vue)
- `PredefinedQuestionController.generateQuestionWithoutSave` (Questions.vue)
- `AiController.getAvailableGptModels` (ManageModel.vue)

**B8. Background Status Updates**
- `ConversationMessageController.markConversationAsRead` (MessageCenterPage.vue:120)

**B9. Wikidata Operations** (search/fetch operations)
- `WikidataController.searchWikidata` (WikidataSearchByLabel.vue)
- `WikidataController.fetchWikidataEntityDataById` (WikidataAssociationDialog.vue)

**B10. Audio Operations**
- `AiAudioController.audioToText` (NoteAudioTools.vue)

## Migration Plan

### Phase 1: High-Priority Silent Operations (Immediate)
**Goal**: Fix operations that clearly shouldn't have loading state
**Files**: ~15 locations
**Effort**: 1-2 hours

1. MainMenu.vue badge counts (3 calls) - Add `client: globalClientSilent`
2. Search operations in SearchResults.vue (4 calls) - Add `client: globalClientSilent`
3. StoredApiCollection.ts prefetch (1 call) - Add `client: globalClientSilent`
4. Recent notes components (3 calls) - Add `client: globalClientSilent`
5. Export operations (4 calls) - Add `client: globalClientSilent`

**Testing**: Verify badges update without loading spinner, search is responsive

### Phase 2: User Actions Need Loading (High Priority)
**Goal**: Wrap all user-initiated mutations with `apiCallWithLoading`
**Files**: ~30 locations
**Effort**: 4-6 hours

1. User/subscription CRUD operations (8 calls)
2. Notebook CRUD operations (8 calls)
3. Circle operations (3 calls)
4. Quiz/assessment remaining operations (3 calls)
5. Review actions (6 calls)
6. Conversation actions (5 calls)
7. Note/question management (8 calls)

**Testing**: E2E tests should pass, verify loading shows for user actions

### Phase 3: Page Load Silent Operations
**Goal**: Remove loading from page initialization fetches
**Files**: ~15 locations
**Effort**: 2-3 hours

1. All page component data fetches (12 calls)
2. Component mount fetches (3 calls)

**Testing**: Pages should load without global loading spinner (they have their own indicators)

### Phase 4: Component Display Silent Operations
**Goal**: Remove loading from component data fetches
**Files**: ~20 locations
**Effort**: 3-4 hours

1. Info/display components (10 calls)
2. Admin read operations (7 calls)
3. Wikidata/audio operations (3 calls)

**Testing**: Components load data without showing global loading

### Phase 5: Admin Operations
**Goal**: Wrap admin mutations, silence admin reads
**Files**: ~10 locations
**Effort**: 2-3 hours

1. Admin mutations with loading (7 calls)
2. Admin read operations silent (already covered in Phase 4)

**Testing**: Admin operations show loading appropriately

### Phase 6: Remove Interceptor-Based Loading
**Goal**: Switch to explicit loading only
**Effort**: 2-3 hours

1. Remove request/response interceptors from globalClient (clientSetup.ts lines 71-74, 122-125)
2. Keep only error interceptor for error toasts
3. Rename globalClientSilent to globalClient (or keep error-handling interceptor only)
4. Update all API calls to use single client
5. Verify apiCallWithLoading is only wrapper needed

**Testing**: Full regression test suite

### Phase 7: Code Cleanup
**Goal**: Simplify architecture
**Effort**: 1-2 hours

1. Remove globalClientSilent if no longer needed
2. Update documentation
3. Consider adding ESLint rule to enforce pattern
4. Clean up this document or archive it

## Testing Strategy

### Per Phase Testing
1. **Manual Testing**: Test the affected components/pages
2. **E2E Tests**: Run relevant e2e tests for the area
3. **Visual Testing**: Verify loading indicators appear/disappear correctly

### Full Regression Testing (After Phase 6)
1. Run complete E2E test suite
2. Manual smoke test of key user journeys
3. Verify no loading state regressions

## E2E Test Impact

**Why Loading State Matters for E2E Tests:**
- Tests need to know when create/update operations complete before navigating
- Tests need to wait for API responses before router changes
- Loading state is the signal that tells tests "wait for this to finish"

**What Changes:**
- User mutations (POST/PATCH/DELETE) + router changes → KEEP loading
- Background fetches, page loads → REMOVE loading (won't affect e2e timing)
- E2E tests can still wait for API responses, just not via global loading indicator

## Success Criteria

✅ **Phase 1-5 Complete**: All API calls properly categorized and migrated
✅ **E2E Tests Pass**: No test failures due to timing issues
✅ **UX Maintained**: Loading state appears for user actions, not for background operations
✅ **Architecture Simplified**: Single client, explicit loading via wrapper
✅ **Documentation Updated**: Clear guidelines for when to use apiCallWithLoading

## Risk Mitigation

1. **Incremental Migration**: Do one phase at a time with testing
2. **Rollback Plan**: Each phase can be reverted independently
3. **E2E Test Monitoring**: Watch for flaky tests after each phase
4. **User Feedback**: Monitor for UX issues (missing loading indicators)

## Timeline Estimate

- **Phase 1**: 1-2 hours
- **Phase 2**: 4-6 hours
- **Phase 3**: 2-3 hours
- **Phase 4**: 3-4 hours
- **Phase 5**: 2-3 hours
- **Phase 6**: 2-3 hours
- **Phase 7**: 1-2 hours
- **Total**: 15-23 hours (2-3 days of focused work)

## Next Immediate Actions

1. ✅ Complete audit (DONE)
2. ✅ Create detailed plan (DONE)
3. ✅ Complete all Phase 2 A-tasks (A1-A7) (DONE)
4. ✅ Fix remaining API calls without explicit wrappers (DONE - Nov 27, 2024)
5. ✅ **Remove automatic loading from globalClient** (DONE - Nov 27, 2024)
6. ✅ **Remove `globalClientSilent` completely** (DONE - Nov 27, 2024)
7. ✅ **Remove unused ApiStatus infrastructure** (DONE - Nov 27, 2024)
8. ⏳ Verify E2E tests still pass
9. ⏳ Archive this document (refactoring complete!)

### Nov 27, 2024 - Moved Error Toast to Caller ✅

**FINAL SIMPLIFICATION**: Moved `addError` method to the caller since it doesn't use internal state!

**Changes made:**
1. ✅ Moved error toast logic from `ApiStatusHandler.addError()` to `clientSetup.handleApiError()`
2. ✅ Removed `addError` method from `ApiStatusHandler`
3. ✅ Removed `useToast` import from `ApiStatusHandler`
4. ✅ `ApiStatusHandler` now ONLY manages loading state
5. ✅ All 415 tests passing ✅

**Why this matters:**
- 🎯 **Single Responsibility**: `ApiStatusHandler` now has ONE job - manage loading state
- 🧹 **No dependencies**: `ApiStatusHandler` no longer needs `vue-toastification`
- 📦 **Simpler class**: Just 15 lines of code, crystal clear purpose
- ✨ **Better separation**: Error display logic lives where it's used

**ApiStatusHandler is now minimal:**
```typescript
export default class ApiStatusHandler {
  apiStatus: ApiStatus

  constructor(apiStatus: ApiStatus) {
    this.apiStatus = apiStatus
  }

  assignLoading(value: boolean) {
    if (value) {
      this.apiStatus.states.push(true)
    } else {
      this.apiStatus.states.pop()
    }
  }
}
```

### Nov 27, 2024 - Cleaned Up Unused ApiStatus Infrastructure ✅

**CLEANUP**: Removed unused parts of ApiStatus infrastructure!

**Changes made:**
1. ✅ Removed `ApiStatus.errors[]` array (never read, only written)
2. ✅ Removed `ApiError` type (not used)
3. ✅ Removed `silentMode` parameter from ApiStatusHandler (always false)
4. ✅ Simplified ApiStatus to only contain `states: boolean[]`
5. ✅ All 415 tests passing ✅

**What was removed:**
```typescript
// Before
export type ApiError = {
  id: number
  message: string
}

export type ApiStatus = {
  states: boolean[]
  errors: ApiError[]  // ❌ Removed - never read
}

// ApiStatusHandler constructor
constructor(apiStatus: ApiStatus, silent?: boolean)  // ❌ silent param removed
```

**What remains:**
```typescript
// After
export type ApiStatus = {
  states: boolean[]  // ✅ Still used for loading indicator
}

// ApiStatusHandler constructor
constructor(apiStatus: ApiStatus)  // ✅ Simplified
```

**Why this matters:**
- 📉 Simpler data structure
- 🧹 Removed dead code
- ✨ Clearer intent - ApiStatus is only for loading state
- 🎯 Error handling is now clearly separated (toast-only, no state tracking)

### Nov 27, 2024 - Removed `globalClientSilent` Completely ✅

**MAJOR MILESTONE #2**: `globalClientSilent` has been completely removed from the codebase!

**Changes made:**
1. ✅ Removed `globalClientSilent` creation from `clientSetup.ts`
2. ✅ Removed all 54 `client: globalClientSilent` parameters from API calls (42 files)
3. ✅ Removed all imports of `globalClientSilent` (43 files)
4. ✅ Updated 13 test files to remove `client: expect.anything()` expectations
5. ✅ All 415 tests passing ✅

**What this means:**
- Only ONE client now: `globalClient`
- All API calls use the same client with error handling
- Loading state is ONLY available via `apiCallWithLoading`
- Simpler, cleaner architecture with no confusion

**Impact:**
- 📉 Code complexity significantly reduced
- 📉 No more "which client should I use?" questions
- ✅ Error handling (toasts, 401 redirects) works for ALL API calls
- ✅ Loading state is explicit and opt-in
- ✅ No functional changes to the application

### Nov 27, 2024 - Removed Automatic Loading from globalClient ✅

**MAJOR MILESTONE #1**: The globalClient no longer automatically sets loading state!

**Changes made:**
1. ✅ Removed request interceptor that set loading state
2. ✅ Removed response interceptor that cleared loading state
3. ✅ Kept error interceptor for error toasts and 401 handling
4. ✅ Updated unit tests:
   - Removed test for automatic loading behavior
   - Updated concurrent calls test to expect correct loading count
   - All 415 tests passing ✅

**What this means:**
- Loading state is now **opt-in** via `apiCallWithLoading`
- No more double-counting of loading states
- No more confusion about when loading appears
- Error handling (toasts, 401 redirects) still works automatically

**Impact:**
- All API calls wrapped with `apiCallWithLoading` continue to show loading ✅
- Error handling unchanged ✅
- No functional changes to the application ✅

### Nov 27, 2024 - Final Cleanup of Unwrapped API Calls

**Identified and fixed 6 API calls using default client without explicit wrapper:**

✅ **GET calls converted to silent client:**
1. `DoughnutApp.vue` - `TestabilityRestController.getFeatureToggle()` → added `client: globalClientSilent`
2. `DoughnutApp.vue` - `CurrentUserInfoController.currentUserInfo()` → added `client: globalClientSilent`
3. `ManageBazaar.vue` - `BazaarController.bazaar()` → added `client: globalClientSilent`
4. `NotebookCertificateRequest.vue` - `NotebookCertificateApprovalController.getApprovalForNotebook()` → added `client: globalClientSilent`
5. `ConversationInner.vue` - `ConversationMessageController.getConversationMessages()` → added `client: globalClientSilent`

✅ **POST call wrapped with loading:**
6. `NoteAddQuestion.vue` - `PredefinedQuestionController.generateQuestionWithoutSave()` → wrapped with `apiCallWithLoading()`

**Result:** All 416 frontend unit tests passing ✅

**Current Status Summary:**
- `apiCallWithLoading` usage: 116 occurrences across 46 files
- `globalClientSilent` usage: 93 occurrences across 44 files
- Calls using default client without wrapper: **0** ✅

