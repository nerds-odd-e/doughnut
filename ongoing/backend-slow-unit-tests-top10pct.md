# Backend slow unit test optimization (top 10%)

Status: in-progress

## Profiling baseline (2026-06-03)

Command: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`

- **1163 tests**; top 10% = **117** slowest (≥ 0.014s).
- **Grouping:** per 10 tests (**12 groups**) — fewer than per-file (67 groups).

## Optimization rules

1. Remove or simplify redundant tests first.
2. Strictly no fixed-time waits (`Thread.sleep`, etc.).
3. Flaky = failure; tests must be deterministic.

---

### Phase 1: Optimize slow tests rank 1–10 (max 0.671s)
Status: done

**Result (2026-06-03):** Slimmer fixtures and fewer tests (1163 → 1158): JPEG image builder + parameterized `ImageUtilsTest`; OpenAPI approval uses static true copy + lazy-init (removed custom diff); multipart handlers in plain `ControllerSetupMultipartExceptionTest`; removed redundant audio-instructions and recall-prompt AI reply tests; lighter admin/MVC assertions; merged GCS delete cases and prod deep-link routes test.

**Scope:**
- `com.odde.doughnut.algorithms.ImageUtilsTest#shouldResizeLargeImage()` (0.671s)
- `com.odde.doughnut.integration.OpenApiDocsTests#openApiDocsApprovalTest()` (0.601s)
- `com.odde.doughnut.configs.ControllerSetupTest#returnsPayloadTooLargeForMaxUploadSizeExceeded()` (0.493s)
- `com.odde.doughnut.controllers.AiAudioControllerTests$ConvertAudioToTextTests#shouldWorkWithoutAdditionalInstructions()` (0.235s)
- `com.odde.doughnut.controllers.ConversationMessageControllerAiReplyTests$RecallPromptConversationTests#shouldUseNoteFromRecallPrompt()` (0.189s)
- `com.odde.doughnut.controllers.DuplicateNoteTitleMvcTest#createNoteAtNotebookRootReturns409WhenTitleDuplicatesAtRoot()` (0.123s)
- `com.odde.doughnut.controllers.AdminUserControllerTest$AdminAccessUserListing#canListUsersWithCorrectMemoryTrackerCount()` (0.114s)
- `com.odde.doughnut.controllers.AiControllerTest$GetModelVersions#shouldGetModelVersionsCorrectly()` (0.086s)
- `com.odde.doughnut.controllers.ApplicationControllerProdDeepLinkTests#prodDeepLinkUnderNotebooksReturnsHtmlFromConfiguredSpaOrigin()` (0.086s)
- `com.odde.doughnut.services.book.GcsBookStorageTest#delete_callsStorageDeleteForAllowedRef()` (0.079s)

**Goals:** Remove redundant coverage; slim fixtures/setup; mock heavy IO; no sleep; keep behavior assertions.

**Verify:** run affected test classes, then `CURSOR_DEV=true nix develop -c pnpm backend:test_only` if multiple files touched.

---

### Phase 2: Optimize slow tests rank 11–20 (max 0.074s)
Status: done

**Result (2026-06-03):** Slimmer fixtures and fewer redundant tests (1158 → 1161 invocations, −1 redundant shuffle integration test): GCS get invalid-ref cases parameterized; conversation cap test simplified; admin listing uses one note and smaller page; shuffle test extracted to mock-only `AiQuestionGeneratorShuffleTest`; wiki-link extract test drops duplicate reference assertion; embedding job setup drops unused notes; export-messages asserts parsed messages directly; ControllerSetup “do not record” cases merged into one parameterized test.

**Scope:**
- `com.odde.doughnut.services.book.GcsBookStorageTest#get_returnsBytesWhenBlobExists()` (0.074s)
- `com.odde.doughnut.controllers.ConversationMessageControllerTest$ConversationOrderingTests#returnsAtMost50Conversations()` (0.073s)
- `com.odde.doughnut.controllers.AdminUserControllerTest$AdminAccessUserListing#canListUsersWithCorrectNoteCount()` (0.069s)
- `com.odde.doughnut.services.ai.AiQuestionGeneratorTests#shouldShuffleChoicesInSpecificOrder()` (0.069s)
- `com.odde.doughnut.controllers.AiControllerExtractNoteTest$ExtractNote#shouldRefreshWikiLinkCacheForOriginalAndNewNoteAfterExtraction()` (0.068s)
- `com.odde.doughnut.controllers.AdminUserControllerTest$AdminAccessUserListing#canListUsersWithLastAssimilationTime()` (0.065s)
- `com.odde.doughnut.services.EmbeddingMaintenanceJobTests#shouldInvokeUpdateForEveryNotebook()` (0.064s)
- `com.odde.doughnut.controllers.ConversationMessageControllerTest$ExportConversationTests#shouldExportConversationWithMessages()` (0.060s)
- `com.odde.doughnut.configs.ControllerSetupTest#shouldNotRecordUnexpectedNoAccessRightException()` (0.057s)
- `com.odde.doughnut.factoryServices.FailureReportFactoryTest#failureReportIncludesAuthenticatedUserInfo()` (0.055s)

**Goals:** Remove redundant coverage; slim fixtures/setup; mock heavy IO; no sleep; keep behavior assertions.

**Verify:** run affected test classes, then `CURSOR_DEV=true nix develop -c pnpm backend:test_only` if multiple files touched.

---

### Phase 3: Optimize slow tests rank 21–30 (max 0.054s)
Status: done

**Result (2026-06-03):** Fewer and lighter tests: regeneration save path moved to mock-only `PredefinedQuestionServiceRegenerationTest`; failure-report exception details tested in `FailureReportFactoryTest`; `CurrentUserInfoControllerTest` no longer uses Spring/DB; removed redundant semantic-search auth and empty scope stubs; parameterized `OwnershipTest`; slimmer soft-delete undo and focus-context fixtures (merged inbound cap+seed stability, fewer referrers/linked notes).

**Scope:**
- `com.odde.doughnut.entities.PredefinedQuestionTest$AutoEvaluateAndRegenerate#shouldSaveBothOriginalAndRegeneratedQuestions()` (0.054s)
- `com.odde.doughnut.controllers.SearchControllerSemanticTests$SemanticSearch#shouldNotAllowSearchWhenNotLoggedIn()` (0.052s)
- `com.odde.doughnut.entities.OwnershipTest#getOwnerNameReturnsUserNameWhenUserIsNotNull()` (0.052s)
- `com.odde.doughnut.configs.ControllerSetupTest#shouldRecordExceptionDetails()` (0.051s)
- `com.odde.doughnut.entities.OwnershipTest#getOwnerNameReturnsCircleNameWhenCircleIsNotNull()` (0.050s)
- `com.odde.doughnut.controllers.SoftDeletedTitleConflictMvcTest#undoDeleteRestoresNoteAfterSoftDeletedTitleConflict()` (0.049s)
- `com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest$InboundSampling#depth2InboundCappedAtTwo()` (0.047s)
- `com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest$InboundSampling#sameSeedProducesSameInboundSelection()` (0.047s)
- `com.odde.doughnut.controllers.CurrentUserInfoControllerTest#shouldReturnUserInfoIncludingRoleForLearner()` (0.045s)
- `com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest$TokenBudget#relatedNotesBudgetCapsNumberOfIncludedNotes()` (0.045s)

**Goals:** Remove redundant coverage; slim fixtures/setup; mock heavy IO; no sleep; keep behavior assertions.

**Verify:** run affected test classes, then `CURSOR_DEV=true nix develop -c pnpm backend:test_only` if multiple files touched.

---

### Phase 4: Optimize slow tests rank 31–40 (max 0.043s)
Status: done

**Result (2026-06-03):** Slimmer fixtures and fewer tests (1162 invocations): InboundSampling split by concern with minimal referrers (21 for URI cap, 7 for outgoing exclusion); merged null-seed cap into parameterized depth-1 test; BooksController epub download uses direct storage ref instead of attach+fixture; extract-note placement parameterized with separate instruction-shape test; NullToNotFound supports cases merged; removed stray note in MCQ recall test.

**Scope:**
- `com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest$InboundSampling#focusInboundUriListCappedAtTwenty()` (0.043s)
- `com.odde.doughnut.controllers.BooksControllerTest$GetBookFileByBook#returnsEpubZipWhenBookFormatIsEpub()` (0.040s)
- `com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest$InboundSampling#differentSeedsProduceDifferentInboundSelection()` (0.037s)
- `com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest$InboundSampling#depth1InboundCappedAtSix()` (0.029s)
- `com.odde.doughnut.controllers.AiControllerExtractNoteTest$ExtractNote#shouldReturnCreatedNoteAtNotebookRootWhenSourceNoteHasNoFolder()` (0.028s)
- `com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest$InboundSampling#depth1InboundExcludesOutgoingTargetsBeforeCap()` (0.027s)
- `com.odde.doughnut.controllers.AiControllerExtractNoteTest$ExtractNote#shouldPlaceExtractedNoteInSameFolderWhenSourceNoteIsInFolder()` (0.026s)
- `com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest$InboundSampling#noSeedTakesFirstSixInStableOrder()` (0.026s)
- `com.odde.doughnut.configs.NullToNotFoundResponseBodyAdviceTest#doesNotApplyToResponseEntityReturnType()` (0.025s)
- `com.odde.doughnut.controllers.MemoryTrackerControllerTest$AskAQuestion#shouldReturnMCQRecallPromptForNonSpellingMemoryTracker()` (0.025s)

**Goals:** Remove redundant coverage; slim fixtures/setup; mock heavy IO; no sleep; keep behavior assertions.

**Verify:** run affected test classes, then `CURSOR_DEV=true nix develop -c pnpm backend:test_only` if multiple files touched.

---

### Phase 5: Optimize slow tests rank 41–50 (max 0.024s)
Status: done

**Result (2026-06-03):** Fewer redundant tests and lighter fixtures (1156 invocations): merged admin pagination into one test with smaller page fixtures; dropped duplicate AI reply message-count/instructions tests; merged note non-persistence assert into regeneration test and removed redundant controller refinement-suggestions happy path (covered by structured-instructions test + service tests); depth-limit split uses shallow attach with depth set to MAX−1 instead of 63-node tree; dropped extra note from question-generation service setup.

**Scope:**
- `com.odde.doughnut.controllers.AdminUserControllerTest$AdminAccessUserListing#paginationWorksCorrectly()` (0.024s)
- `com.odde.doughnut.controllers.AiControllerNoteRefinementTest$RemoveRefinementSuggestion#shouldNotModifyNoteInDatabase()` (0.024s)
- `com.odde.doughnut.controllers.AssimilationControllerTests$Next#subscriptionNoteBeforeOwnedNote()` (0.024s)
- `com.odde.doughnut.controllers.ConversationMessageControllerAiReplyTests$NewChatTests#chatWithAIAndGetResponse()` (0.024s)
- `com.odde.doughnut.controllers.AdminUserControllerTest$AdminAccessUserListing#canListUsersWithLastNoteTime()` (0.022s)
- `com.odde.doughnut.controllers.AiControllerNoteRefinementTest$GenerateRefinementSuggestions#shouldReturnRefinementSuggestions()` (0.022s)
- `com.odde.doughnut.controllers.AssimilationControllerTests$Next#returnsNextNoteIdPastDailyCap()` (0.022s)
- `com.odde.doughnut.controllers.ConversationMessageControllerAiReplyTests$NewChatTests#shouldAddMessageToConversationWhenMessageCompleted()` (0.022s)
- `com.odde.doughnut.services.NoteQuestionGenerationServiceTests$EvaluateQuestion#shouldReturnEvaluationWhenEvaluationSucceeds()` (0.022s)
- `com.odde.doughnut.controllers.NotebookBooksBlockContentControllerTest$CreateBookBlockFromContent#rejectsWhenChildWouldExceedMaxLayoutDepth()` (0.021s)

**Goals:** Remove redundant coverage; slim fixtures/setup; mock heavy IO; no sleep; keep behavior assertions.

**Verify:** run affected test classes, then `CURSOR_DEV=true nix develop -c pnpm backend:test_only` if multiple files touched.

---

### Phase 6: Optimize slow tests rank 51–60 (max 0.021s)
Status: done

**Result (2026-06-03):** Fewer redundant tests and lighter fixtures (1151 invocations): budget-exhaustion focus test uses four spend notes with smaller bodies; bona-fide cloze folded into parameterized cases; merged suggest-title and export-question-generation assertions; removed weak notebook-assistant AI reply test; folder append-order uses two creates; assimilation cap test drops extra subscription note; epub delete uses direct storage ref; trimmed duplicate request-shape assertions.

**Scope:**
- `com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest$BreadthFirstDepth2#budgetExhaustedMidRingLeavesLaterDepthOneNotesAndDepthTwoUnreachable()` (0.021s)
- `com.odde.doughnut.algorithms.ClozeDescriptionTest#clozeShouldMaskBonaFideWithFidesVariantAndMarkdownEmphasis()` (0.020s)
- `com.odde.doughnut.controllers.AiControllerTest$SuggestNoteTitle#shouldReturnSuggestedTitle()` (0.020s)
- `com.odde.doughnut.controllers.ConversationMessageControllerAiReplyTests$NewChatTests#shouldIncludeNotebookAiAssistantInstructionsInRun()` (0.020s)
- `com.odde.doughnut.controllers.NotebookNotesFolderControllerTest$CreateNoteAtNotebookRoot#createsNotesInFolderInAppendLastOrder()` (0.020s)
- `com.odde.doughnut.services.NoteQuestionGenerationServiceTests$GenerateQuestion#shouldUseSameRequestShapeAsExportedQuestionGenerationRequest()` (0.020s)
- `com.odde.doughnut.controllers.AiControllerTest$SuggestNoteTitle#shouldCallResponsesApiWithStructuredInstructions()` (0.019s)
- `com.odde.doughnut.controllers.AssimilationControllerTests$Next#returnsOwnedNoteWhenSubscriptionDailyCapReached()` (0.019s)
- `com.odde.doughnut.controllers.NotebookBooksRetrievalControllerTest$DeleteBook#removesEpubBookRowAndStoredBytes()` (0.019s)
- `com.odde.doughnut.controllers.PredefinedQuestionControllerTests$ExportQuestionGeneration#shouldNotContainValidFieldsInExportedQuestionGeneration()` (0.019s)

**Goals:** Remove redundant coverage; slim fixtures/setup; mock heavy IO; no sleep; keep behavior assertions.

**Verify:** run affected test classes, then `CURSOR_DEV=true nix develop -c pnpm backend:test_only` if multiple files touched.

---

### Phase 7: Optimize slow tests rank 61–70 (max 0.019s)
Status: done

**Result (2026-06-03):** Fewer redundant tests and lighter fixtures (1145 invocations): merged folder peer-collection and idempotent move into one two-note test; removed redundant contest null-response DB test (service handles null); `NoteConversationAiReplyServiceTest` mock-only; parameterized frontmatter image updates; merged export context/history assertions; slimmer OpenAI-unavailable memory-tracker fixture; dropped duplicate wikidata human country case; conversation-instructions AI reply test already removed in phase 6.

**Scope:**
- `com.odde.doughnut.controllers.RelationControllerTests$MoveNoteToFolderTest#moveNoteToSameFolder_isIdempotent()` (0.019s)
- `com.odde.doughnut.controllers.ConversationMessageControllerAiReplyTests$NewChatTests#shouldSetConversationInstructionsForRun()` (0.018s)
- `com.odde.doughnut.controllers.RelationControllerTests$MoveNoteToFolderTest#moveNoteIntoFolder_collectsPeersInFolder()` (0.018s)
- `com.odde.doughnut.services.AiOpenAiAssistantFactoryWithDBTest$ContestQuestion#noFunctionCallInvoked()` (0.018s)
- `com.odde.doughnut.services.ai.NoteConversationAiReplyServiceTest#shouldGenerateStreamingResponseForConversation()` (0.018s)
- `com.odde.doughnut.controllers.NotebookRootNoteCreationWithWikidataTests$createNoteInFolderTest$AddingNoteWithHumanWikidataId#[1] WikidataId = "Q706446", Birthday from Wikidata = "+1980-03-31T00:00:00Z", CountryQID = null, Country Name = null, Expected Birthday = "31 March 1980"` (0.017s)
- `com.odde.doughnut.algorithms.NoteContentMarkdownTest#mergeNoteImageScalarsIntoContent_replaces_existing_image_lines()` (0.017s)
- `com.odde.doughnut.controllers.ConversationMessageControllerTest$ExportConversationTests#shouldExportConversationWithHistory()` (0.017s)
- `com.odde.doughnut.controllers.ConversationMessageControllerTest$ExportConversationTests#shouldExportConversationWithContext()` (0.017s)
- `com.odde.doughnut.controllers.MemoryTrackerControllerTest$AskAQuestion#shouldThrowWhenOpenAiNotAvailableAndGeneratingQuestion()` (0.017s)

**Goals:** Remove redundant coverage; slim fixtures/setup; mock heavy IO; no sleep; keep behavior assertions.

**Verify:** run affected test classes, then `CURSOR_DEV=true nix develop -c pnpm backend:test_only` if multiple files touched.

---

### Phase 8: Optimize slow tests rank 71–80 (max 0.017s)
Status: done

**Result (2026-06-03):** Fewer redundant tests and lighter fixtures (1143 invocations): delete-referrer property test moved out of heavy `DeleteNoteTest` setup; merged PDF full-view JSON checks into nested-outline attach test; slimmed EPUB attach to layout persistence (locators covered by retrieval test); dropped recursive-merge folder listing round-trip; parameterized GCS put pdf/epub; fewer folder peers for sibling seed test; parameterized GitHub issue success/failure in `ControllerSetupTest`; refinement suggestions test asserts return value in API-shape test. `ImageUtilsTest#shouldNotTouchSmallImage` already covered by Phase 1 `resizeImageDimensions` parameter.

**Scope:**
- `com.odde.doughnut.controllers.NoteControllerTests$DeleteNoteTest#shouldRemoveDeletedNoteLinksFromReferrerPropertiesOnly()` (0.017s)
- `com.odde.doughnut.controllers.NotebookBooksAttachControllerTest$AttachBook#getBookFullViewJsonExposesDepthAndPreorderMatchesLayoutSequence()` (0.017s)
- `com.odde.doughnut.controllers.NotebookBooksAttachControllerTest$AttachBook#persistsEpubAttachWithFormatAndStorageRef()` (0.017s)
- `com.odde.doughnut.controllers.NotebookFolderManagementControllerTest$MoveFolder#mergesRecursivelyOnNestedNameClash()` (0.017s)
- `com.odde.doughnut.services.book.GcsBookStorageTest#put_epubUploadsWithEpubExtensionAndContentType()` (0.017s)
- `com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest$FolderSiblings#differentSeedsProduceDifferentSampleSiblingsSelection()` (0.017s)
- `com.odde.doughnut.algorithms.ImageUtilsTest#shouldNotTouchSmallImage()` (0.016s)
- `com.odde.doughnut.configs.ControllerSetupTest#shouldRecordGithubErrorInFailureReportWhenGithubFails()` (0.016s)
- `com.odde.doughnut.controllers.AiControllerExtractNoteTest$ExtractNote#shouldThrowWhenAiReturnsNull()` (0.016s)
- `com.odde.doughnut.controllers.AiControllerNoteRefinementTest$GenerateRefinementSuggestions#shouldCallResponsesApiWithStructuredInstructions()` (0.016s)

**Goals:** Remove redundant coverage; slim fixtures/setup; mock heavy IO; no sleep; keep behavior assertions.

**Verify:** run affected test classes, then `CURSOR_DEV=true nix develop -c pnpm backend:test_only` if multiple files touched.

---

### Phase 9: Optimize slow tests rank 81–90 (max 0.016s)
Status: planned

**Scope:**
- `com.odde.doughnut.controllers.AiControllerNoteRefinementTest$RemoveRefinementSuggestion#shouldLimitRemovalOutputToTwoThousandTokens()` (0.016s)
- `com.odde.doughnut.controllers.AiControllerNoteRefinementTest$RemoveRefinementSuggestion#shouldReturnRegeneratedContentAfterRemovingSuggestions()` (0.016s)
- `com.odde.doughnut.controllers.NoteControllerTests$UpdateNoteRecallSetting#shouldPutNoteBackToAssimilationListWhenRememberSpellingIsAddedLater()` (0.016s)
- `com.odde.doughnut.controllers.PredefinedQuestionControllerTests$RefineQuestion#shouldThrowWhenOpenAiNotAvailable()` (0.016s)
- `com.odde.doughnut.controllers.SearchControllerTests$SearchForRelationshipTarget#shouldReturnFolderHitsAlongsideNoteHits()` (0.016s)
- `com.odde.doughnut.controllers.TextContentControllerTests$updateNoteTitleInboundWikiReferences#allowsRenameWithExplicitReferenceHandlingWhenInboundWikiLinksExist()` (0.016s)
- `com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest$BreadthFirstDepth2#shorterPathWinsWhenSameNoteReachableAtDepthOneAndTwo()` (0.016s)
- `com.odde.doughnut.controllers.NoteControllerTests$DeleteNoteTest$MemoryTrackerExclusionWhenNoteDeleted#shouldRestoreMemoryTrackersWhenNoteIsRestored()` (0.015s)
- `com.odde.doughnut.controllers.ConversationMessageControllerTest$ExportConversationTests#shouldExportConversationWithRequest()` (0.015s)
- `com.odde.doughnut.controllers.NotebookBooksRetrievalControllerTest$GetBook#epubContentLocatorsMatchAnchorAndDirectContentOnFixture()` (0.015s)

**Goals:** Remove redundant coverage; slim fixtures/setup; mock heavy IO; no sleep; keep behavior assertions.

**Verify:** run affected test classes, then `CURSOR_DEV=true nix develop -c pnpm backend:test_only` if multiple files touched.

---

### Phase 10: Optimize slow tests rank 91–100 (max 0.015s)
Status: planned

**Scope:**
- `com.odde.doughnut.controllers.NotebookFolderManagementControllerTest$DissolveFolder#dissolveMergesClashingSubfolderWhenMergeRequested()` (0.015s)
- `com.odde.doughnut.controllers.RecallPromptControllerTests$Contest#useTheRightModel()` (0.015s)
- `com.odde.doughnut.controllers.RecallPromptControllerTests$ContestQuestion#shouldNotMarkQuestionAsContestedWhenContestIsRejected()` (0.015s)
- `com.odde.doughnut.controllers.RecallPromptControllerTests$RegenerateQuestion#createQuizQuestion()` (0.015s)
- `com.odde.doughnut.controllers.RecallPromptControllerTests$RegenerateQuestion#shouldPassOldQuestionAndContestResultToOpenAiApi()` (0.015s)
- `com.odde.doughnut.controllers.RelationControllerTests$MoveNoteToNotebookRootInNotebookTest#crossNotebookMove_preservesExplicitDisplayText()` (0.015s)
- `com.odde.doughnut.controllers.RelationControllerTests$MoveNoteToNotebookRootInNotebookTest#crossNotebookMove_requalifiesQualifiedReferrerLink()` (0.015s)
- `com.odde.doughnut.controllers.RelationControllerTests$MoveNoteToNotebookRootInNotebookTest#crossNotebookMove_qualifiesUnqualifiedReferrerLink()` (0.015s)
- `com.odde.doughnut.controllers.TextContentControllerTests$updateNoteTitleInboundWikiReferences#keepVisibleText_preservesExplicitDisplayWhileRetargetingTitle()` (0.015s)
- `com.odde.doughnut.controllers.TextContentControllerTests$updateNoteTitleInboundWikiReferences#updateVisibleText_rewritesWikiLinkInsideYamlFrontmatter()` (0.015s)

**Goals:** Remove redundant coverage; slim fixtures/setup; mock heavy IO; no sleep; keep behavior assertions.

**Verify:** run affected test classes, then `CURSOR_DEV=true nix develop -c pnpm backend:test_only` if multiple files touched.

---

### Phase 11: Optimize slow tests rank 101–110 (max 0.015s)
Status: planned

**Scope:**
- `com.odde.doughnut.services.NoteQuestionGenerationServiceTests$BuildQuestionGenerationRequest#shouldStillIncludeFocusContextAfterDeductingInstructionTokensFromBudget()` (0.015s)
- `com.odde.doughnut.services.NoteQuestionGenerationServiceTests$BuildQuestionGenerationRequest#shouldOrderUserMessagesScopedInstructionThenFocusThenAdditional()` (0.015s)
- `com.odde.doughnut.services.NoteQuestionGenerationServiceTests$GenerateQuestion#shouldPassScopedQuestionGenerationInstructionAsFirstUserMessage()` (0.015s)
- `com.odde.doughnut.services.RecallQuestionServiceTest#shouldReturnMostRecentUnansweredRecallPromptWhenMultipleExist()` (0.015s)
- `com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest$BreadthFirstDepth2#depthTwoInboundFromExpandedNote()` (0.015s)
- `com.odde.doughnut.services.search.UserModelSearchTest$ThereIsANoteBookInMyCircle#[3] allMyNotebooksAndSubscriptions = "true", allMyCircle = "true", expectOtherNotebooks = "true", expectSubscription = "true", expectCircleNote = "true", expectedCount = "4"` (0.015s)
- `com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest$FolderSiblings$SamplingStability#fixedSeedRepeatRetrieveSameSiblingOrder()` (0.014s)
- `com.odde.doughnut.controllers.CircleControllerTest$ShowCircle#notebooksViewIncludesCatalogWithCircleOwnedGroups()` (0.014s)
- `com.odde.doughnut.controllers.ConversationMessageControllerTest$ConversationOrderingTests#testConversationsOrderedByLastMessageTime()` (0.014s)
- `com.odde.doughnut.controllers.NotebookBooksRetrievalControllerTest$GetBookFile#returnsEpubZipWhenBookFormatIsEpub()` (0.014s)

**Goals:** Remove redundant coverage; slim fixtures/setup; mock heavy IO; no sleep; keep behavior assertions.

**Verify:** run affected test classes, then `CURSOR_DEV=true nix develop -c pnpm backend:test_only` if multiple files touched.

---

### Phase 12: Optimize slow tests rank 111–117 (max 0.014s)
Status: planned

**Scope:**
- `com.odde.doughnut.controllers.NotebookRootNoteCreationWithWikidataTests$createNoteInFolderTest#shouldBeAbleToSaveNoteWithWikidataIdWhenValid()` (0.014s)
- `com.odde.doughnut.controllers.PredefinedQuestionControllerTests$RefineQuestion#refineQuestionFailedWithGpt35WillNotTryAgain()` (0.014s)
- `com.odde.doughnut.controllers.PredefinedQuestionControllerTests$RefineQuestion#givenQuestion_thenReturnRefineQuestion()` (0.014s)
- `com.odde.doughnut.controllers.RecallPromptControllerTests$Contest#rejected()` (0.014s)
- `com.odde.doughnut.controllers.RecallPromptControllerTests$ContestQuestion#shouldMarkQuestionAsContestedWhenContestIsAccepted()` (0.014s)
- `com.odde.doughnut.controllers.RelationControllerTests$MoveNoteToNotebookRootInNotebookTest#sameNotebookMove_doesNotRewriteReferrer()` (0.014s)
- `com.odde.doughnut.controllers.TextContentControllerTests$updateNoteContentTest#replacingWikiLinkUpdatesNoteRealmWikiTitlesAndPersistedCache()` (0.014s)

**Goals:** Remove redundant coverage; slim fixtures/setup; mock heavy IO; no sleep; keep behavior assertions.

**Verify:** run affected test classes, then `CURSOR_DEV=true nix develop -c pnpm backend:test_only` if multiple files touched.

---

### Phase 13: Re-profile and close
Status: planned

Re-run full suite, document before/after top 10%, mark plan done.
