# Backend unit test optimization

Status: in-progress

**Execution:** run via **execute-plan** (commit + push per phase).

## Profiling baseline (2026-07-10)

Command: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`

- **1517 tests**, suite wall ~**36s** (BUILD SUCCESSFUL in 33s)
- Eligible after blacklist: **1517** (Ignored empty; see `ongoing/test-optimization-blacklist.md`)
- Sum of all testcase times ~**12.0s**; top 10% sum ~**6.9s**
- Raw profile: `ongoing/backend-profile-results.json` — **do not commit**

### Top 10% slowest (n = ceil(1517 × 0.10) = 152)

| # | ms | file / spec | test / scenario |
|---|-----|-------------|-----------------|
| 1 | 1049 | `com/odde/doughnut/integration/OpenApiDocsTests.java` | openApiDocsApprovalTest() |
| 2 | 415 | `com/odde/doughnut/configs/ControllerSetupMultipartExceptionTest.java` | returnsPayloadTooLargeForMaxUploadSizeExceeded() |
| 3 | 367 | `com/odde/doughnut/services/ai/NoteConversationAiReplyServiceTest.java` | shouldGenerateStreamingResponseForConversation() |
| 4 | 318 | `com/odde/doughnut/controllers/NoteControllerTests.java` | shouldNotAllowUploadForNoteBelongingToAnotherUser() |
| 5 | 238 | `com/odde/doughnut/controllers/AiAudioControllerTests.java` | shouldNotTruncateSRTWhenComplete() |
| 6 | 214 | `com/odde/doughnut/services/QuestionGenerationBatchMaintenanceServiceOrderTest.java` | shouldPruneTerminalBatchesAfterImport() |
| 7 | 191 | `com/odde/doughnut/controllers/ConversationMessageControllerAiReplyTests.java` | chatWithAIAndGetResponse() |
| 8 | 174 | `com/odde/doughnut/services/QuestionGenerationBatchAdminStatusServiceTest.java` | doesNotReportSchedulerActiveWhenNoMaintenanceTaskIsRegistered() |
| 9 | 164 | `com/odde/doughnut/algorithms/ImageUtilsTest.java` | [2] width = "2001", height = "2", expectedWidth = "2000", expectedHeight = "1" |
| 10 | 155 | `com/odde/doughnut/services/QuestionGenerationBatchUserScheduleTest.java` | returnsNoCandidateTrackersReasonWhenNoTrackersCanBeBatched() |
| 11 | 110 | `com/odde/doughnut/services/PredefinedQuestionServiceRegenerationTest.java` | savesOriginalAsContestedThenRegeneratedQuestion() |
| 12 | 108 | `com/odde/doughnut/services/ai/AiQuestionGeneratorShuffleTest.java` | shouldReturnPostProcessedGeneratedQuestion() |
| 13 | 107 | `com/odde/doughnut/controllers/DuplicateNoteTitleMvcTest.java` | createNoteAtNotebookRootReturns409WhenTitleDuplicatesAtRoot() |
| 14 | 106 | `com/odde/doughnut/services/QuestionGenerationBatchAdminStatusServiceTest.java` | reportsRegisteredMaintenanceSchedulerFromScheduledTaskRegistry() |
| 15 | 85 | `com/odde/doughnut/services/book/GcsBookStorageTest.java` | get_emptyWhenNoBlob() |
| 16 | 83 | `com/odde/doughnut/configs/ControllerSetupTest.java` | [1] exception = org.springframework.web.server.ResponseStatusException: 401 UNAUTHORIZED "xx", expectedType = class org.springframework.web.server.ResponseStatusException |
| 17 | 83 | `com/odde/doughnut/controllers/AiControllerTest.java` | shouldGetModelVersionsCorrectly() |
| 18 | 82 | `com/odde/doughnut/controllers/ApplicationControllerProdDeepLinkTests.java` | prodDeepLinkRoutesReturnHtmlFromConfiguredSpaOrigin() |
| 19 | 77 | `com/odde/doughnut/controllers/AdminQuestionGenerationBatchControllerTest.java` | adminGetsCountsReflectingSeededBatchesAndRequests() |
| 20 | 76 | `com/odde/doughnut/services/book/GcsBookStorageTest.java` | get_returnsBytesWhenBlobExists() |
| 21 | 71 | `com/odde/doughnut/controllers/AiControllerExportExtractRequestTest.java` | shouldExportExtractRequestWithBodyMapReflectingSelection() |
| 22 | 65 | `com/odde/doughnut/controllers/ConversationMessageControllerTest.java` | returnsAtMost50Conversations() |
| 23 | 58 | `com/odde/doughnut/services/EmbeddingMaintenanceJobTests.java` | shouldInvokeUpdateForEveryNotebook() |
| 24 | 53 | `com/odde/doughnut/controllers/AiControllerCreateExtractedNoteTest.java` | shouldPersistNewAndUpdatedNotesFromEditedFields() |
| 25 | 53 | `com/odde/doughnut/controllers/SearchControllerSemanticTests.java` | shouldReturnEmptyListWhenNoMatchingNotes() |
| 26 | 52 | `com/odde/doughnut/algorithms/ImageUtilsTest.java` | [1] width = "300", height = "300", expectedWidth = "300", expectedHeight = "300" |
| 27 | 49 | `com/odde/doughnut/controllers/SoftDeletedTitleConflictMvcTest.java` | undoDeleteRestoresNoteAfterSoftDeletedTitleConflict() |
| 28 | 46 | `com/odde/doughnut/configs/ControllerSetupTest.java` | shouldNotRecordUserInfoWhenNoAuthentication() |
| 29 | 45 | `com/odde/doughnut/services/focusContext/FocusContextRetrievalServiceTest.java` | focusInboundUriListCappedAtTwenty() |
| 30 | 38 | `com/odde/doughnut/services/QuestionGenerationBatchJsonlRendererTest.java` | rendersOneJsonlLinePerRequestWithResponsesApiShape() |
| 31 | 38 | `com/odde/doughnut/services/QuestionGenerationBatchSubmitDueUsersTest.java` | continuesAfterPerUserFailureAndRecordsLatestSubmittedAt() |
| 32 | 36 | `com/odde/doughnut/controllers/ConversationMessageControllerTest.java` | shouldExportConversationWithMessages() |
| 33 | 34 | `com/odde/doughnut/services/focusContext/FocusContextRetrievalServiceTest.java` | [2] seed = 1 |
| 34 | 33 | `com/odde/doughnut/services/QuestionGenerationBatchRowImportServiceAtomicTest.java` | failureAfterQuestionCreationRollsBackAndLeavesRowReimportable() |
| 35 | 32 | `com/odde/doughnut/services/focusContext/FocusContextRetrievalServiceTest.java` | [1] seed = null |
| 36 | 32 | `com/odde/doughnut/controllers/AiControllerCreateExtractedNoteTest.java` | shouldRefreshWikiLinkCacheForOriginalAndNewNoteAfterExtraction() |
| 37 | 30 | `com/odde/doughnut/services/focusContext/FocusContextRetrievalServiceTest.java` | relatedNotesBudgetCapsNumberOfIncludedNotes() |
| 38 | 29 | `com/odde/doughnut/controllers/AdminUserControllerTest.java` | canListUsersWithLastAssimilationTime() |
| 39 | 28 | `com/odde/doughnut/controllers/AssimilationControllerTests.java` | subscriptionNoteBeforeOwnedNote() |
| 40 | 28 | `com/odde/doughnut/services/QuestionGenerationBatchMetricsTest.java` | incrementsFailedBatchCounterWhenSubmissionFails() |
| 41 | 27 | `com/odde/doughnut/services/focusContext/FocusContextRetrievalServiceTest.java` | differentSeedsProduceDifferentInboundSelection() |
| 42 | 26 | `com/odde/doughnut/controllers/ConversationMessageControllerTest.java` | shouldExportConversationWithContextAndHistory() |
| 43 | 26 | `com/odde/doughnut/services/QuestionGenerationBatchMaintenanceConcurrencyTest.java` | jdbcLockProviderPreventsDuplicateMaintenanceExecution() |
| 44 | 26 | `com/odde/doughnut/services/QuestionGenerationBatchOutputCollectionServiceTest.java` | doesNotCollectAlreadyCollectedBatches() |
| 45 | 26 | `com/odde/doughnut/services/QuestionGenerationBatchOutputCollectionServiceTest.java` | ignoresErrorLinesWithMissingCustomId() |
| 46 | 25 | `com/odde/doughnut/controllers/MemoryTrackerControllerTest.java` | shouldReturnMCQRecallPromptForNonSpellingMemoryTracker() |
| 47 | 25 | `com/odde/doughnut/services/QuestionGenerationBatchMetricsTest.java` | incrementsFailedAndExpiredBatchCountersFromPolling() |
| 48 | 23 | `com/odde/doughnut/configs/NullToNotFoundResponseBodyAdviceTest.java` | [1] methodName = "returnsRecallPrompt", expected = "true" |
| 49 | 23 | `com/odde/doughnut/controllers/AdminQuestionGenerationBatchControllerTest.java` | adminCanResumeExistingBatchesAndReceivesRefreshedStatus() |
| 50 | 23 | `com/odde/doughnut/controllers/AdminUserControllerTest.java` | canListUsersWithCorrectNoteCount() |
| 51 | 23 | `com/odde/doughnut/controllers/AssimilationControllerTests.java` | returnsNextNoteIdPastDailyCap() |
| 52 | 23 | `com/odde/doughnut/controllers/NotebookBooksAttachControllerTest.java` | persistsEpubAttachWithFormatAndStorageRef() |
| 53 | 23 | `com/odde/doughnut/services/QuestionGenerationBatchOutputCollectionServiceTest.java` | marksMissingOutputLinesAsFailed() |
| 54 | 23 | `com/odde/doughnut/services/QuestionGenerationBatchOutputCollectionServiceTest.java` | downloadsFromPersistedFileIdsWithoutRetrieveBatch() |
| 55 | 22 | `com/odde/doughnut/controllers/NotebookFolderManagementControllerTest.java` | crossNotebookFolderMove_rewritesOutgoingLinksToOutsideTargetOnly() |
| 56 | 22 | `com/odde/doughnut/controllers/RelationControllerMoveNoteToFolderTests.java` | crossNotebookMoveToFolder_rewritesInboundAndOutgoingLinks() |
| 57 | 22 | `com/odde/doughnut/services/QuestionGenerationBatchOutputCollectionServiceTest.java` | ignoresMalformedOutputLinesWithoutFailingOtherRows() |
| 58 | 22 | `com/odde/doughnut/services/QuestionGenerationBatchOutputCollectionServiceTest.java` | mapsErrorFileRowsToFailedRequests() |
| 59 | 22 | `com/odde/doughnut/services/QuestionGenerationBatchSubmitDueUsersTest.java` | skipsDueUserWithNoCandidateTrackersWithoutSubmittedBatch() |
| 60 | 22 | `com/odde/doughnut/services/QuestionGenerationRequestBuilderTests.java` | shouldUseExplicitViewerForWikiTitlesWithoutSessionCurrentUser() |
| 61 | 21 | `com/odde/doughnut/controllers/AdminUserControllerTest.java` | canListUsersWithCorrectMemoryTrackerCount() |
| 62 | 21 | `com/odde/doughnut/controllers/AiControllerNoteRefinementTest.java` | shouldReturnEmptyLayoutWhenAiReturnsInvalidLayout() |
| 63 | 21 | `com/odde/doughnut/services/AiOpenAiAssistantFactoryWithDBTest.java` | rejected() |
| 64 | 21 | `com/odde/doughnut/services/QuestionGenerationBatchMaintenanceServiceTest.java` | resumesPollingOutputCollectionAndImportFromPersistedState() |
| 65 | 21 | `com/odde/doughnut/services/QuestionGenerationBatchOutputCollectionServiceTest.java` | mapsUnorderedOutputLinesToRequestRows() |
| 66 | 20 | `com/odde/doughnut/controllers/AiControllerExtractNotePreviewTest.java` | shouldReturnExtractionPreviewWithoutPersisting() |
| 67 | 20 | `com/odde/doughnut/controllers/NotebookFolderManagementControllerTest.java` | crossNotebookFolderMove_rewritesInboundLinksFromOutsideReferrerOnly() |
| 68 | 20 | `com/odde/doughnut/services/QuestionGenerationBatchOutputCollectionServiceTest.java` | doesNotCollectNonCompletedBatches() |
| 69 | 19 | `com/odde/doughnut/controllers/NotebookRootNoteCreationWithWikidataTests.java` | [1] WikidataId = "Q706446", Birthday from Wikidata = "+1980-03-31T00:00:00Z", CountryQID = null, Country Name = null, Expected Birthday = "31 March 1980" |
| 70 | 19 | `com/odde/doughnut/services/focusContext/FocusContextRetrievalServiceTest.java` | repeatRetrieveSameSiblingOrderForNullAndFixedSeed() |
| 71 | 19 | `com/odde/doughnut/controllers/AdminUserControllerTest.java` | canListUsersWithLastNoteTime() |
| 72 | 19 | `com/odde/doughnut/controllers/AiControllerCreateExtractedNoteTest.java` | [1] sourceInFolder = false |
| 73 | 19 | `com/odde/doughnut/controllers/NoteControllerTests.java` | shouldUseSuffixedPropertyKeyWhenSourceAlreadyHasPropertyKey() |
| 74 | 19 | `com/odde/doughnut/controllers/NoteControllerTests.java` | shouldRehomeRelationNoteLevelTrackerAsPropertyTrackerOnSource() |
| 75 | 19 | `com/odde/doughnut/services/search/UserModelSearchTest.java` | [3] allMyNotebooksAndSubscriptions = "true", allMyCircle = "true", expectOtherNotebooks = "true", expectSubscription = "true", expectCircleNote = "true", expectedCount = "4" |
| 76 | 18 | `com/odde/doughnut/configs/ControllerSetupTest.java` | [1] githubSucceeds = true, expectedIssueNumber = 123, expectedErrorFragment = null |
| 77 | 18 | `com/odde/doughnut/controllers/AdminUserControllerTest.java` | paginationWorksCorrectly() |
| 78 | 18 | `com/odde/doughnut/controllers/AiControllerCreateExtractedNoteTest.java` | [2] sourceInFolder = true |
| 79 | 18 | `com/odde/doughnut/controllers/AiControllerExtractNotePreviewTest.java` | shouldCallExtractNoteWithStructuredInstructions() |
| 80 | 18 | `com/odde/doughnut/controllers/NoteControllerTests.java` | shouldRehomeTrackerWithSuffixedPropertyKeyWhenSourceAlreadyHasPropertyKey() |
| 81 | 18 | `com/odde/doughnut/controllers/NoteControllerTests.java` | shouldUseExampleOfPropertyKeyWhenReducingExampleOfRelation() |
| 82 | 18 | `com/odde/doughnut/controllers/NoteControllerTests.java` | shouldRemoveDeletedNoteLinksFromReferrerPropertiesOnly() |
| 83 | 18 | `com/odde/doughnut/services/QuestionGenerationBatchJsonlRendererTest.java` | includesHighReasoningEffortForReasoningModel() |
| 84 | 17 | `com/odde/doughnut/algorithms/FrontmatterAliasesTest.java` | fromFrontmatter_returns_valid_yaml_list_items() |
| 85 | 17 | `com/odde/doughnut/configs/ObjectMapperConfigTest.java` | shouldExcludeNullFieldsFromJsonSerialization() |
| 86 | 17 | `com/odde/doughnut/controllers/AiControllerNoteRefinementTest.java` | shouldCallResponsesApiWithStructuredInstructions() |
| 87 | 17 | `com/odde/doughnut/controllers/AiControllerNoteRefinementTest.java` | shouldReturnRegeneratedContentAfterRemovingSelectedLayoutPoints() |
| 88 | 17 | `com/odde/doughnut/controllers/AssimilationControllerTests.java` | returnsOwnedNoteWhenSubscriptionDailyCapReached() |
| 89 | 17 | `com/odde/doughnut/controllers/NotebookFolderManagementControllerTest.java` | crossNotebookFolderMove_keepsCoMovedPeerLinkRelativeWhenDestinationHasSameTitleNote() |
| 90 | 17 | `com/odde/doughnut/services/AssimilationServicePropertyUnitsTest.java` | subscription_daily_budget_caps_note_and_property_units_combined() |
| 91 | 16 | `com/odde/doughnut/services/focusContext/FocusContextRetrievalServiceTest.java` | depth2InboundCappedAtTwo() |
| 92 | 16 | `com/odde/doughnut/controllers/AiControllerExportRefinementLayoutRequestTest.java` | shouldExportRefinementLayoutRequestWithBodyMap() |
| 93 | 16 | `com/odde/doughnut/controllers/AiControllerExtractNotePreviewTest.java` | shouldTrimSurroundingWhitespaceFromExtractionPreviewTitle() |
| 94 | 16 | `com/odde/doughnut/controllers/NotebookBooksAttachControllerTest.java` | persistsNestedOutlineAndReturnsBookWithBlocks() |
| 95 | 16 | `com/odde/doughnut/controllers/RecallPromptControllerTests.java` | shouldPassOldQuestionAndContestResultToOpenAiApi() |
| 96 | 16 | `com/odde/doughnut/controllers/SearchControllerAliasTests.java` | shouldReturnNotesMatchingFrontmatterAliases() |
| 97 | 16 | `com/odde/doughnut/services/NoteQuestionGenerationServiceTests.java` | shouldUseSameRequestShapeAsExportedQuestionGenerationRequest() |
| 98 | 16 | `com/odde/doughnut/services/QuestionGenerationBatchMetricsTest.java` | incrementsCountersForRepresentativeBatchLifecycleOutcomes() |
| 99 | 16 | `com/odde/doughnut/services/book/GcsBookStorageTest.java` | [1] format = "pdf", extension = ".pdf", contentType = "application/pdf" |
| 100 | 16 | `com/odde/doughnut/services/search/NoteSearchServiceExactMatchTest.java` | shouldIncludeExactMatchesEvenWhenMoreThan20PartialMatches() |
| 101 | 15 | `com/odde/doughnut/controllers/AiControllerExtractNoteValidationTest.java` | shouldThrowWhenAiReturnsNull() |
| 102 | 15 | `com/odde/doughnut/controllers/AssimilationControllerTests.java` | returns_next_property_key_for_untracked_example_of() |
| 103 | 15 | `com/odde/doughnut/controllers/NoteControllerTests.java` | shouldAddRelationLabelPropertyToSourceAndSoftDeleteRelationNote() |
| 104 | 15 | `com/odde/doughnut/controllers/NoteControllerTests.java` | shouldSkipUnreadableLowestIdAliasCandidateForReadableTarget() |
| 105 | 15 | `com/odde/doughnut/controllers/PredefinedQuestionRefinementControllerTests.java` | shouldThrowWhenOpenAiNotAvailable() |
| 106 | 15 | `com/odde/doughnut/controllers/RecallPromptControllerTests.java` | rejected() |
| 107 | 15 | `com/odde/doughnut/controllers/RelationControllerMoveNoteToFolderTests.java` | sameNotebookMoveToFolder_doesNotRewriteLinks() |
| 108 | 15 | `com/odde/doughnut/controllers/TextContentControllerTests.java` | updateVisibleText_rewritesWikiLinkInsideYamlFrontmatter() |
| 109 | 15 | `com/odde/doughnut/services/AssimilationServicePropertyWikiLinkGateTest.java` | gates_list_property_until_all_resolved_targets_are_assimilated() |
| 110 | 15 | `com/odde/doughnut/services/focusContext/FocusContextRetrievalServiceTest.java` | differentSeedsProduceDifferentSampleSiblingsSelection() |
| 111 | 15 | `com/odde/doughnut/services/openAiApis/StructuredResponseCreateParamsSerializerTest.java` | rendersResponsesApiBodyShapeWithoutSyntheticValidField() |
| 112 | 14 | `com/odde/doughnut/services/AssimilationServiceDailyCapTest.java` | returnsNextNotePastUserDailyCap() |
| 113 | 14 | `com/odde/doughnut/services/focusContext/FocusContextRetrievalServiceTest.java` | depth1InboundExcludesOutgoingTargetsBeforeCap() |
| 114 | 14 | `com/odde/doughnut/controllers/MemoryTrackerControllerTest.java` | shouldThrowWhenOpenAiNotAvailableAndGeneratingQuestion() |
| 115 | 14 | `com/odde/doughnut/controllers/MemoryTrackerControllerTest.java` | shouldReturnRecentlyRecalledForCurrentUser() |
| 116 | 14 | `com/odde/doughnut/controllers/NoteControllerTests.java` | shouldResolveAmbiguousAliasToLowestNoteId() |
| 117 | 14 | `com/odde/doughnut/controllers/NotebookFolderManagementControllerTest.java` | movesFolderSubtreeToAnotherNotebookRoot() |
| 118 | 14 | `com/odde/doughnut/controllers/NotebookFolderManagementControllerTest.java` | mergesRecursivelyAcrossNotebooksWhenMergeRequested() |
| 119 | 14 | `com/odde/doughnut/controllers/NotebookFolderManagementControllerTest.java` | movesFolderSubtreeIntoFolderInAnotherNotebook() |
| 120 | 14 | `com/odde/doughnut/controllers/RelationControllerMoveNoteToFolderTests.java` | moveNoteIntoFolder_collectsPeersAndIsIdempotent() |
| 121 | 14 | `com/odde/doughnut/services/NoteQuestionGenerationServiceTests.java` | usesMediumReasoningForReasoningModel() |
| 122 | 14 | `com/odde/doughnut/services/QuestionGenerationBatchSubmissionServiceTest.java` | updatesLocalBatchWithSubmittedAt() |
| 123 | 14 | `com/odde/doughnut/services/focusContext/FocusContextRetrievalServiceTest.java` | budgetExhaustedMidRingLeavesLaterDepthOneNotesAndDepthTwoUnreachable() |
| 124 | 13 | `com/odde/doughnut/algorithms/ClozeDescriptionTest.java` | clozeShouldMaskEvictedWhenTitleIsEvict() |
| 125 | 13 | `com/odde/doughnut/controllers/AiControllerExtractNotePreviewTest.java` | shouldSanitizePathSeparatorsInExtractionPreview() |
| 126 | 13 | `com/odde/doughnut/controllers/AiControllerTest.java` | shouldTrimSurroundingWhitespaceFromSuggestedTitle() |
| 127 | 13 | `com/odde/doughnut/controllers/AiControllerTest.java` | shouldSanitizePathSeparatorsInSuggestedTitle() |
| 128 | 13 | `com/odde/doughnut/controllers/NotebookRootNoteCreationWithWikidataTests.java` | shouldBeAbleToSaveNoteWithWikidataIdWhenValid() |
| 129 | 13 | `com/odde/doughnut/controllers/PredefinedQuestionControllerTests.java` | shouldExportQuestionGenerationWithAllNonEmptyFields() |
| 130 | 13 | `com/odde/doughnut/controllers/PredefinedQuestionRefinementControllerTests.java` | givenQuestion_thenReturnPostProcessedRefinedQuestion() |
| 131 | 13 | `com/odde/doughnut/controllers/RecallPromptControllerTests.java` | shouldAcceptThinkingTimeMs() |
| 132 | 13 | `com/odde/doughnut/controllers/RelationControllerTests.java` | crossNotebookMove_rewritesMovedNotesOutgoingUnqualifiedLinks() |
| 133 | 13 | `com/odde/doughnut/controllers/TextContentControllerTests.java` | updateVisibleText_preservesExplicitDisplayTextAndRefreshesInboundMetadata() |
| 134 | 13 | `com/odde/doughnut/services/NoteQuestionGenerationServiceTests.java` | nestedLabeledQuestionInstructionsAppearInFirstUserMessageInOrder() |
| 135 | 13 | `com/odde/doughnut/services/NoteRealmServiceTest.java` | notebook_index_applies_title_pattern_and_question_instruction_to_sibling_notes() |
| 136 | 13 | `com/odde/doughnut/services/focusContext/FocusContextRetrievalServiceTest.java` | shorterPathWinsWhenSameNoteReachableAtDepthOneAndTwo() |
| 137 | 13 | `com/odde/doughnut/services/search/UserModelSearchTest.java` | [2] allMyNotebooksAndSubscriptions = "true", allMyCircle = "false", expectOtherNotebooks = "true", expectSubscription = "true", expectCircleNote = "false", expectedCount = "3" |
| 138 | 12 | `com/odde/doughnut/controllers/ConversationMessageControllerTest.java` | shouldSetRecallPromptAsSubject() |
| 139 | 12 | `com/odde/doughnut/configs/ControllerSetupTest.java` | [2] githubSucceeds = false, expectedIssueNumber = null, expectedErrorFragment = "GitHub issue creation failed" |
| 140 | 12 | `com/odde/doughnut/controllers/BooksControllerTest.java` | returnsEpubZipWhenBookFormatIsEpub() |
| 141 | 12 | `com/odde/doughnut/controllers/ConversationMessageControllerTest.java` | testConversationsOrderedByLastMessageTime() |
| 142 | 12 | `com/odde/doughnut/controllers/NoteControllerRecentNotesTests.java` | shouldReturnEmptyListWhenNoNotes() |
| 143 | 12 | `com/odde/doughnut/controllers/NoteControllerTests.java` | shouldResolveWikiLinkToSingleFrontmatterAliasTarget() |
| 144 | 12 | `com/odde/doughnut/controllers/NotebookBooksAttachControllerTest.java` | persistsFromContentListBuildsBeginningAndChapter() |
| 145 | 12 | `com/odde/doughnut/controllers/NotebookFolderManagementControllerTest.java` | rejectsCrossNotebookMoveWhenSoftDeletedNoteHasSameTitleAtDestination() |
| 146 | 12 | `com/odde/doughnut/controllers/NotebookRootNoteCreationWithWikidataTests.java` | siblingsShareSameFolder() |
| 147 | 12 | `com/odde/doughnut/controllers/PredefinedQuestionRefinementControllerTests.java` | refineQuestionFailedWithGpt35WillNotTryAgain() |
| 148 | 12 | `com/odde/doughnut/controllers/PredefinedQuestionRefinementControllerTests.java` | invalidRefinedQuestionIsRejected() |
| 149 | 12 | `com/odde/doughnut/controllers/RecallPromptControllerTests.java` | acceptTheContest() |
| 150 | 12 | `com/odde/doughnut/controllers/RecallPromptControllerTests.java` | shouldThrowWhenOpenAiNotAvailable() |
| 151 | 12 | `com/odde/doughnut/controllers/RelationControllerTests.java` | [1] before = "[[MyNote]]", after = "[[NewNb:MyNote\|MyNote]]" |
| 152 | 12 | `com/odde/doughnut/controllers/RelationControllerTests.java` | [3] before = "[[OldNb:MyNote\|custom text]]", after = "[[NewNb:MyNote\|custom text]]" |

### Grouping

- By file (outer class): **66** groups
- Batches of 3: **51** groups
- **Chosen:** batches of 3 (fewer groups)

## Optimization rules

1. Remove or simplify redundant tests first.
2. Strictly no fixed-time waits.
3. Flaky = failure.

Hard-to-improve tests: propose under **Candidates** in
`ongoing/test-optimization-blacklist.md` (do not promote to Ignored without
developer review).

Follow `.cursor/rules/backend-testing.mdc` and `.cursor/rules/backend-code.mdc`.

**Verify pattern** (focused class, then full suite if shared helpers change):

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut....ClassName"
```

---

### Phase 1: Optimize batch ranks 1–3
Status: done

**Tests (baseline → after):**
- `OpenApiDocsTests.openApiDocsApprovalTest()` (~1049ms) → merged into `RobotsTests.openApiDocsMatchCommittedYaml()` (~806ms); shares `@SpringBootTest` context with `robotsTxt()` via `@Order`
- `ControllerSetupMultipartExceptionTest.returnsPayloadTooLargeForMaxUploadSizeExceeded()` (~415ms) → parameterized `mapsMultipartExceptionToApiError` (~14ms); dropped Mockito (handler uses no deps)
- `NoteConversationAiReplyServiceTest.shouldGenerateStreamingResponseForConversation()` (~367ms) → **deleted** (redundant with `ConversationMessageControllerAiReplyTests.chatWithAIAndGetResponse()`)

**Learnings:** Mockito self-attach dominated the multipart test; a separate `@SpringBootTest` with custom `lazy-initialization` blocked context reuse for OpenAPI approval.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.integration.RobotsTests" --tests "com.odde.doughnut.configs.ControllerSetupMultipartExceptionTest" --tests "com.odde.doughnut.controllers.ConversationMessageControllerAiReplyTests"
```

---

### Phase 2: Optimize batch ranks 4–6
Status: done

**Tests (baseline → after):**
- `NoteControllerTests.shouldNotAllowUploadForNoteBelongingToAnotherUser()` (~318ms) → ~4ms; dropped `anUploadedImage()` fixture — `assertAuthorization` throws before image processing
- `AiAudioControllerTests.shouldNotTruncateSRTWhenComplete()` (~238ms) → **deleted** (redundant with `SRTProcessorTests`); also removed `shouldTruncateSRTWhenIncomplete()` (~same cost)
- `QuestionGenerationBatchMaintenanceServiceOrderTest.shouldPruneTerminalBatchesAfterImport()` (~214ms) → merged into `QuestionGenerationBatchMaintenanceJobTests.ResumeExistingBatchesInvocationOrder` (~118ms); deleted standalone test class

**Learnings:** PNG generation in `UploadedImageBuilder` dominated the upload access-denial test; SRT truncation is already covered at `SRTProcessor` unit level.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.NoteControllerTests" --tests "com.odde.doughnut.controllers.AiAudioControllerTests" --tests "com.odde.doughnut.services.QuestionGenerationBatchMaintenanceServiceOrderTest"
```

---

### Phase 3: Optimize batch ranks 7–9
Status: done

**Tests (baseline → after):**
- `ConversationMessageControllerAiReplyTests.chatWithAIAndGetResponse()` (~191ms) → merged into `ConversationMessageControllerTest.GetAiReplyTests` (~170ms); dropped separate `@SpringBootTest` context
- `QuestionGenerationBatchAdminStatusServiceTest.doesNotReportSchedulerActiveWhenNoMaintenanceTaskIsRegistered()` (~174ms) + `reportsRegisteredMaintenanceSchedulerFromScheduledTaskRegistry()` (~106ms) → merged `reportsSchedulerActiveBasedOnRegisteredMaintenanceTasks()` (~159ms); `StandardEnvironment` + mocked `ScheduledTask.toString()` instead of reflection
- `ImageUtilsTest` resize `[2] 2001×2` (~164ms) → `scaledDimensions` parameterized cases (&lt;1ms each); one small integration resize test (~149ms, no longer top-10%)

**Learnings:** Image resize slowness was JVM warmup on first `Graphics2D` path, not pixel count; dimension math belongs in a pure unit test.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.ConversationMessageControllerTest\$GetAiReplyTests" --tests "com.odde.doughnut.services.QuestionGenerationBatchAdminStatusServiceTest" --tests "com.odde.doughnut.algorithms.ImageUtilsTest"
```

---

### Phase 4: Optimize batch ranks 10–12
Status: done

**Tests (baseline → after):**
- `QuestionGenerationBatchUserScheduleTest.returnsNoCandidateTrackersReasonWhenNoTrackersCanBeBatched()` (~155ms) → `QuestionGenerationBatchUserScheduleNoCandidateTrackersTest` mock-only (~&lt;5ms when Mockito warm; avoids 168× DB scan)
- `PredefinedQuestionServiceRegenerationTest.savesOriginalAsContestedThenRegeneratedQuestion()` (~110ms) → **deleted**; contested-flag assertions merged into `PredefinedQuestionTest.shouldRegenerateQuestionWhenEvaluationShowsNotFeasible()`
- `AiQuestionGeneratorShuffleTest.shouldReturnPostProcessedGeneratedQuestion()` (~108ms) → **deleted** (redundant with `GeneratedQuestionPostProcessorTest` + `AiQuestionGeneratorTests`)

**Learnings:** `getNextBatchQuestionSchedule` scans up to 168 hours with DB queries per hour when no candidates exist; mock-only test covers the reason path without integration cost.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.services.QuestionGenerationBatchUserScheduleTest" --tests "com.odde.doughnut.services.QuestionGenerationBatchUserScheduleNoCandidateTrackersTest" --tests "com.odde.doughnut.entities.PredefinedQuestionTest\$AutoEvaluateAndRegenerate" --tests "com.odde.doughnut.services.ai.AiQuestionGeneratorTests" --tests "com.odde.doughnut.services.ai.GeneratedQuestionPostProcessorTest"
```

---

### Phase 5: Optimize batch ranks 13–15
Status: done

**Tests (baseline → after):**
- `DuplicateNoteTitleMvcTest.createNoteAtNotebookRootReturns409WhenTitleDuplicatesAtRoot()` (~107ms) → **deleted** separate `@SpringBootTest` class; duplicate-title constraint + reserved-index create moved into `NotebookNotesFolderControllerTest` (direct controller, shared context); HTTP 409 mapping covered by new `CustomRestExceptionHandlerDuplicateTitleTest` (~&lt;1ms); rename-to-index MVC test dropped (covered by `NoteUpdateTitleDTOTest`)
- `QuestionGenerationBatchAdminStatusServiceTest.reportsRegisteredMaintenanceSchedulerFromScheduledTaskRegistry()` (~106ms) → **already merged in Phase 3** as `reportsSchedulerActiveBasedOnRegisteredMaintenanceTasks()`
- `GcsBookStorageTest.get_emptyWhenNoBlob()` (~85ms) → merged with `get_returnsBytesWhenBlobExists()` into parameterized `get_returnsBytesOrEmptyWhenBlobPresentOrMissing()`

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.NotebookNotesFolderControllerTest" --tests "com.odde.doughnut.configs.CustomRestExceptionHandlerDuplicateTitleTest" --tests "com.odde.doughnut.services.QuestionGenerationBatchAdminStatusServiceTest" --tests "com.odde.doughnut.services.book.GcsBookStorageTest"
```

---

### Phase 6: Optimize batch ranks 16–18
Status: done

**Tests (baseline → after):**
- `ControllerSetupTest.shouldNotRecordExcludedExceptions()` `[1] ResponseStatusException` (~83ms) → `ControllerSetupExcludedExceptionsTest` mock-only (~&lt;5ms); dropped `@SpringBootTest` + DB `count()` for excluded-exception path
- `AiControllerTest.shouldGetModelVersionsCorrectly()` (~83ms) → `OtherAiServicesGetModelsTest` mock-only (~&lt;1ms); controller test keeps only `shouldThrowWhenOpenAiNotAvailable()`
- `ApplicationControllerProdDeepLinkTests.prodDeepLinkRoutesReturnHtmlFromConfiguredSpaOrigin()` (~82ms) → direct `spaDeepLink()` call (~&lt;5ms); dropped MockMvc + duplicate path/upstream expectations

**Learnings:** Excluded-exception and model-list filtering are pure handler/service behavior; MockMvc added cost without extra coverage for prod deep-link shell fetch.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.configs.ControllerSetupTest" --tests "com.odde.doughnut.controllers.AiControllerTest" --tests "com.odde.doughnut.controllers.ApplicationControllerProdDeepLinkTests"
```

---

### Phase 7: Optimize batch ranks 19–21
Status: done

**Tests (baseline → after):**
- `AdminQuestionGenerationBatchControllerTest.adminGetsCountsReflectingSeededBatchesAndRequests()` (~77ms) → **deleted**; count mapping moved to mock-only `QuestionGenerationBatchAdminStatusServiceTest.reportsBatchAndRequestCountsFromRepositories()` (~&lt;5ms)
- `GcsBookStorageTest.get_returnsBytesWhenBlobExists()` (~76ms) → already parameterized in Phase 5; merged `get_emptyWhenWrongPrefix` into `get_emptyWhenInvalidRefOrWrongPrefix`; `@ExtendWith(MockitoExtension.class)` with shared `@Mock Storage`
- `AiControllerExportExtractRequestTest.shouldExportExtractRequestWithBodyMapReflectingSelection()` (~71ms) → **deleted** separate `@SpringBootTest` class; body-map assertions in mock-only `AiNoteAutomationServiceExtractRequestTest.buildExtractNoteRequestBodyReflectsSelectedLayoutItems()` (~&lt;5ms)

**Learnings:** Admin batch counts and extract-request export are service/serializer behavior; planning-service seeding and full controller context added cost without extra coverage beyond mock-only tests.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.AdminQuestionGenerationBatchControllerTest" --tests "com.odde.doughnut.services.book.GcsBookStorageTest" --tests "com.odde.doughnut.services.ai.AiNoteAutomationServiceExtractRequestTest" --tests "com.odde.doughnut.services.QuestionGenerationBatchAdminStatusServiceTest"
```

---

### Phase 8: Optimize batch ranks 22–24
Status: done

**Tests (baseline → after):**
- `ConversationMessageControllerTest.returnsAtMost50Conversations()` (~65ms) → **deleted**; 50-item cap covered by mock-only `ConversationServiceConversationListLimitTest.shouldRequestAtMostFiftyConversations()` (~&lt;5ms)
- `EmbeddingMaintenanceJobTests.shouldInvokeUpdateForEveryNotebook()` (~58ms) → mock-only (~&lt;5ms); dropped `@SpringBootTest` + `makeMe` notebook seeding
- `AiControllerCreateExtractedNoteTest.shouldPersistNewAndUpdatedNotesFromEditedFields()` (~53ms) → **deleted**; persistence assertions merged into parameterized `shouldCreateExtractedNoteFromSourceNote(false)`

**Learnings:** Conversation list cap is `PageRequest.of(0, 50)` in `ConversationService`; embedding maintenance is a pure repository→indexing loop; root-note extraction persistence duplicated the parameterized placement test.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.ConversationMessageControllerTest" --tests "com.odde.doughnut.services.EmbeddingMaintenanceJobTests" --tests "com.odde.doughnut.controllers.AiControllerCreateExtractedNoteTest"
```

---

### Phase 9: Optimize batch ranks 25–27
Status: done

**Tests (baseline → after):**
- `SearchControllerSemanticTests.shouldReturnEmptyListWhenNoMatchingNotes()` (~53ms) → **deleted**; empty-embedding path covered by mock-only `SemanticNoteSearchServiceEmptyEmbeddingTest.returnsEmptyListWhenQueryEmbeddingIsEmpty()` (~&lt;5ms)
- `ImageUtilsTest.passesThroughImagesWithinMaxDimensions()` (~52ms) → **deleted**; pass-through branch is trivial byte return when within bounds; dimension math covered by `scaledDimensions` parameterized cases
- `SoftDeletedTitleConflictMvcTest.undoDeleteRestoresNoteAfterSoftDeletedTitleConflict()` (~49ms) → **deleted**; note restore covered by `NoteServiceTest.Restore` and `NoteControllerTests` undo-delete paths

**Learnings:** Empty semantic search is an early-return in `SemanticNoteSearchService` when embedding is empty; ImageUtils pass-through avoids Graphics2D but still paid integration setup cost for trivial logic; undo-delete after soft-delete is not title-conflict-specific behavior.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.SearchControllerSemanticTests" --tests "com.odde.doughnut.algorithms.ImageUtilsTest" --tests "com.odde.doughnut.controllers.SoftDeletedTitleConflictMvcTest"
```

---

### Phase 10: Optimize batch ranks 28–30
Status: done

**Tests (baseline → after):**
- `ControllerSetupTest.shouldNotRecordUserInfoWhenNoAuthentication()` (~46ms) → **deleted**; null external-id assertion merged into mock-only `FailureReportFactoryTest.recordsExceptionClassNameAndStackTrace()` (~&lt;5ms)
- `FocusContextRetrievalServiceTest.focusInboundUriListCappedAtTwenty()` (~45ms) → merged into `Depth1CapAndSeed` shared setup (~&lt;5ms per test); eliminated duplicate 21-referrer fixture
- `QuestionGenerationBatchJsonlRendererTest.rendersOneJsonlLinePerRequestWithResponsesApiShape()` (~38ms) → direct batch/request seed via `savePlannedBatch`/`saveBatchRequest` (~&lt;15ms); dropped `planningService` candidate scan and second note/tracker

**Learnings:** Unauthenticated user info is `FailureReportFactory` behavior; inbound URI cap shares the same referrer graph as depth-1 sampling; JSONL shape tests only need persisted batch rows, not full planning.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.configs.ControllerSetupTest" --tests "com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest" --tests "com.odde.doughnut.services.QuestionGenerationBatchJsonlRendererTest"
```

---

### Phase 11: Optimize batch ranks 31–33
Status: done

**Tests (baseline → after):**
- `QuestionGenerationBatchSubmitDueUsersTest.continuesAfterPerUserFailureAndRecordsLatestSubmittedAt()` (~38ms) → **deleted**; loop continuation + summary counts in mock-only `QuestionGenerationBatchSubmitDueUsersServiceLoopTest` (~&lt;5ms); first-time failure `submittedAt` path in `QuestionGenerationBatchSubmissionServiceTest.FirstTimeFailedSubmission`
- `ConversationMessageControllerTest.shouldExportConversationWithMessages()` (~36ms) → **deleted**; user/assistant role assertions merged into `shouldExportConversationWithContextAndHistory()`
- `FocusContextRetrievalServiceTest` `[2] seed = 1` (~34ms) → removed from parameterized cap test; seed=1 cap/stability/URI assertions consolidated in `fixedSeedInboundSamplingCapAndUriList` (2 retrieves vs 5)

**Learnings:** Per-user submit loop is pure orchestration over `UserSubmissionTx`; export message roles are already covered at builder level — one export test suffices; seed=1 duplicated retrieves across parameterized and URI-cap tests.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.services.QuestionGenerationBatchSubmitDueUsersTest" --tests "com.odde.doughnut.controllers.ConversationMessageControllerTest" --tests "com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest"
```

---

### Phase 12: Optimize batch ranks 34–36
Status: done

**Tests (baseline → after):**
- `QuestionGenerationBatchRowImportServiceAtomicTest.failureAfterQuestionCreationRollsBackAndLeavesRowReimportable()` (~33ms) → dropped redundant `@BeforeEach` cleanup (~9 native deletes); kept committed-transaction rollback test as-is
- `FocusContextRetrievalServiceTest` `[1] seed = null` (~32ms) → merged into `inboundSamplingCapStabilityAndUriList` with seed `1L` (~&lt;5ms per seed); deleted parameterized `depth1InboundCappedAtSixWithStableSeed`
- `AiControllerCreateExtractedNoteTest.shouldRefreshWikiLinkCacheForOriginalAndNewNoteAfterExtraction()` (~32ms) → **deleted**; wiki-link cache assertion merged into `shouldCreateExtractedNoteFromSourceNote(true)`

**Learnings:** Atomic import test pays per-test committed cleanup; null-seed inbound cap duplicated fixed-seed stability checks; wiki cache refresh is exercised by folder extraction path without a separate controller test.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.services.QuestionGenerationBatchRowImportServiceAtomicTest" --tests "com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest" --tests "com.odde.doughnut.controllers.AiControllerCreateExtractedNoteTest"
```

---

### Phase 13: Optimize batch ranks 37–39
Status: done

**Tests (baseline → after):**
- `FocusContextRetrievalServiceTest.relatedNotesBudgetCapsNumberOfIncludedNotes()` (~30ms) → **deleted**; budget cap on included notes covered by `budgetExhaustedMidRingLeavesLaterDepthOneNotesAndDepthTwoUnreachable()`
- `AdminUserControllerTest.canListUsersWithLastAssimilationTime()` (~29ms) → merged into `canListUsersWithCorrectMemoryTrackerCountAndLastAssimilationTime()` (~same cost, one fewer test method)
- `AssimilationControllerTests.subscriptionNoteBeforeOwnedNote()` (~28ms) → **deleted**; subscription-before-owned ordering covered by `AssimilationServiceQueueOrderingTest.owned_and_subscribed_notes_interleave_by_created_at_when_levels_equal()`

**Learnings:** Related-notes budget cap duplicated the mid-ring budget exhaustion test; admin last-assimilation time shares the same memory-tracker fixture as tracker count; subscription queue ordering belongs at service level.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest" --tests "com.odde.doughnut.controllers.AdminUserControllerTest" --tests "com.odde.doughnut.controllers.AssimilationControllerTests"
```

---

### Phase 14: Optimize batch ranks 40–42
Status: done

**Tests (baseline → after):**
- `QuestionGenerationBatchMetricsTest.incrementsFailedBatchCounterWhenSubmissionFails()` (~28ms) → **deleted**; failed-batch counter assertions merged into `QuestionGenerationBatchSubmissionServiceTest.uploadFailureMarksBatchFailedWithoutUpdatingLatestSubmittedAt()`
- `FocusContextRetrievalServiceTest.differentSeedsProduceDifferentInboundSelection()` (~27ms) → merged into `inboundSamplingCapStabilityAndUriList()`; seed scan narrowed 2–10 → 2–5
- `ConversationMessageControllerTest.shouldExportConversationWithContextAndHistory()` (~26ms) → **deleted**; export payload assertions merged into `ConversationAiRequestBuilderTest.shouldIncludeUserAndAssistantMessages()`; dropped export-format helpers and unused export fixture setup

**Learnings:** Submission-failure metrics duplicate submission-service failure tests; inbound seed diversity shares the same 21-referrer fixture as cap/stability; export JSON shape is builder behavior — controller serialization adds cost without extra coverage beyond access control.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.services.QuestionGenerationBatchMetricsTest" --tests "com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest" --tests "com.odde.doughnut.controllers.ConversationMessageControllerTest"
```

---

### Phase 15: Optimize batch ranks 43–45
Status: done

**Tests (baseline → after):**
- `QuestionGenerationBatchMaintenanceConcurrencyTest.jdbcLockProviderPreventsDuplicateMaintenanceExecution()` (~26ms) → slim `DataSourceAutoConfiguration` context (~&lt;5ms); reflection test moved to `QuestionGenerationBatchMaintenanceJobSchedulerLockTest`; deleted redundant `shedlockTableExistsAfterMigration()`
- `QuestionGenerationBatchOutputCollectionServiceTest.doesNotCollectAlreadyCollectedBatches()` (~26ms) → `QuestionGenerationBatchOutputCollectionScopeTest` with direct batch seed (~&lt;5ms); no planning/submission fixture
- `QuestionGenerationBatchOutputCollectionServiceTest.ignoresErrorLinesWithMissingCustomId()` (~26ms) → `QuestionGenerationBatchOutputCollectionMalformedLineTest` with direct batch/request seed (~&lt;10ms); malformed output case moved with shared slim setup

**Learnings:** JDBC lock duplicate prevention only needs `JdbcTemplate`; collection scope and malformed-line handling do not need full planning/submission — direct persisted batch rows suffice.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.services.QuestionGenerationBatchMaintenanceConcurrencyTest" --tests "com.odde.doughnut.services.QuestionGenerationBatchOutputCollectionServiceTest"
```

---

### Phase 16: Optimize batch ranks 46–48
Status: done

**Tests (baseline → after):**
- `MemoryTrackerControllerTest.shouldReturnMCQRecallPromptForNonSpellingMemoryTracker()` (~25ms) → **deleted**; MCQ generation moved to `RecallQuestionServiceTest.shouldGenerateMcqRecallPromptWhenNoUnansweredPromptExists()` (~same cost, service-level entry)
- `QuestionGenerationBatchMetricsTest.incrementsFailedAndExpiredBatchCountersFromPolling()` (~25ms) → **deleted**; failed/expired counter assertions merged into `QuestionGenerationBatchPollingServiceTest.failedUpdatesLocalBatch()` / `expiredUpdatesLocalBatch()` (avoids duplicate user/planning fixture)
- `NullToNotFoundResponseBodyAdviceTest` `[1] returnsRecallPrompt` (~23ms) → single `supportsAppliesOnlyToNonResponseEntityReturnTypes()` `@Test` (drops parameterized invocation overhead)

**Learnings:** MCQ recall generation is `RecallQuestionService` behavior; polling metrics duplicate polling-service status tests; advice `supports` cases need no `@ParameterizedTest`.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.MemoryTrackerControllerTest" --tests "com.odde.doughnut.services.QuestionGenerationBatchMetricsTest" --tests "com.odde.doughnut.configs.NullToNotFoundResponseBodyAdviceTest"
```

---

### Phase 17: Optimize batch ranks 49–51
Status: done

**Tests (baseline → after):**
- `AdminQuestionGenerationBatchControllerTest.adminCanResumeExistingBatchesAndReceivesRefreshedStatus()` (~23ms) → **deleted**; resume orchestration in mock-only `AdminQuestionGenerationBatchControllerResumeTest` (~&lt;5ms); MANUAL_RESUME persistence in `QuestionGenerationBatchMaintenanceRunRepositoryTest`
- `AdminUserControllerTest.canListUsersWithCorrectNoteCount()` (~23ms) → merged into `canListUsersWithCorrectNoteCountAndLastNoteTime()` (one fewer test method, shared user+note fixture)
- `AssimilationControllerTests.returnsNextNoteIdPastDailyCap()` (~23ms) → **deleted**; daily-cap next-note + dueCount=0 in `AssimilationServiceDailyCapTest.theDailyCountShouldNotBeResetOnSameDayDifferentHour()` and merged `returnsNextNoteWithZeroDueCountWhenUserDailyPlanComplete()`

**Learnings:** Admin resume is controller glue over maintenance run recording + service resume; user note-count listing shares fixture with last-note-time; daily-cap past-limit behavior belongs at `AssimilationService` level.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.AdminQuestionGenerationBatchControllerTest" --tests "com.odde.doughnut.controllers.AdminUserControllerTest" --tests "com.odde.doughnut.controllers.AssimilationControllerTests"
```

---

### Phase 18: Optimize batch ranks 52–54
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/controllers/NotebookBooksAttachControllerTest.java` — "persistsEpubAttachWithFormatAndStorageRef()" (~23ms)
- `backend/src/test/java/com/odde/doughnut/services/QuestionGenerationBatchOutputCollectionServiceTest.java` — "marksMissingOutputLinesAsFailed()" (~23ms)
- `backend/src/test/java/com/odde/doughnut/services/QuestionGenerationBatchOutputCollectionServiceTest.java` — "downloadsFromPersistedFileIdsWithoutRetrieveBatch()" (~23ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.NotebookBooksAttachControllerTest" --tests "com.odde.doughnut.services.QuestionGenerationBatchOutputCollectionServiceTest"
```

---

### Phase 19: Optimize batch ranks 55–57
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/controllers/NotebookFolderManagementControllerTest.java` — "crossNotebookFolderMove_rewritesOutgoingLinksToOutsideTargetOnly()" (~22ms)
- `backend/src/test/java/com/odde/doughnut/controllers/RelationControllerMoveNoteToFolderTests.java` — "crossNotebookMoveToFolder_rewritesInboundAndOutgoingLinks()" (~22ms)
- `backend/src/test/java/com/odde/doughnut/services/QuestionGenerationBatchOutputCollectionServiceTest.java` — "ignoresMalformedOutputLinesWithoutFailingOtherRows()" (~22ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.NotebookFolderManagementControllerTest" --tests "com.odde.doughnut.controllers.RelationControllerMoveNoteToFolderTests" --tests "com.odde.doughnut.services.QuestionGenerationBatchOutputCollectionServiceTest"
```

---

### Phase 20: Optimize batch ranks 58–60
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/services/QuestionGenerationBatchOutputCollectionServiceTest.java` — "mapsErrorFileRowsToFailedRequests()" (~22ms)
- `backend/src/test/java/com/odde/doughnut/services/QuestionGenerationBatchSubmitDueUsersTest.java` — "skipsDueUserWithNoCandidateTrackersWithoutSubmittedBatch()" (~22ms)
- `backend/src/test/java/com/odde/doughnut/services/QuestionGenerationRequestBuilderTests.java` — "shouldUseExplicitViewerForWikiTitlesWithoutSessionCurrentUser()" (~22ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.services.QuestionGenerationBatchOutputCollectionServiceTest" --tests "com.odde.doughnut.services.QuestionGenerationBatchSubmitDueUsersTest" --tests "com.odde.doughnut.services.QuestionGenerationRequestBuilderTests"
```

---

### Phase 21: Optimize batch ranks 61–63
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/controllers/AdminUserControllerTest.java` — "canListUsersWithCorrectMemoryTrackerCount()" (~21ms)
- `backend/src/test/java/com/odde/doughnut/controllers/AiControllerNoteRefinementTest.java` — "shouldReturnEmptyLayoutWhenAiReturnsInvalidLayout()" (~21ms)
- `backend/src/test/java/com/odde/doughnut/services/AiOpenAiAssistantFactoryWithDBTest.java` — "rejected()" (~21ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.AdminUserControllerTest" --tests "com.odde.doughnut.controllers.AiControllerNoteRefinementTest" --tests "com.odde.doughnut.services.AiOpenAiAssistantFactoryWithDBTest"
```

---

### Phase 22: Optimize batch ranks 64–66
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/services/QuestionGenerationBatchMaintenanceServiceTest.java` — "resumesPollingOutputCollectionAndImportFromPersistedState()" (~21ms)
- `backend/src/test/java/com/odde/doughnut/services/QuestionGenerationBatchOutputCollectionServiceTest.java` — "mapsUnorderedOutputLinesToRequestRows()" (~21ms)
- `backend/src/test/java/com/odde/doughnut/controllers/AiControllerExtractNotePreviewTest.java` — "shouldReturnExtractionPreviewWithoutPersisting()" (~20ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.services.QuestionGenerationBatchMaintenanceServiceTest" --tests "com.odde.doughnut.services.QuestionGenerationBatchOutputCollectionServiceTest" --tests "com.odde.doughnut.controllers.AiControllerExtractNotePreviewTest"
```

---

### Phase 23: Optimize batch ranks 67–69
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/controllers/NotebookFolderManagementControllerTest.java` — "crossNotebookFolderMove_rewritesInboundLinksFromOutsideReferrerOnly()" (~20ms)
- `backend/src/test/java/com/odde/doughnut/services/QuestionGenerationBatchOutputCollectionServiceTest.java` — "doesNotCollectNonCompletedBatches()" (~20ms)
- `backend/src/test/java/com/odde/doughnut/controllers/NotebookRootNoteCreationWithWikidataTests.java` — "[1] WikidataId = "Q706446", Birthday from Wikidata = "+1980-03-31T00:00:00Z", CountryQID = null, Country Name = null, Expected Birthday = "31 March 1980"" (~19ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.NotebookFolderManagementControllerTest" --tests "com.odde.doughnut.services.QuestionGenerationBatchOutputCollectionServiceTest" --tests "com.odde.doughnut.controllers.NotebookRootNoteCreationWithWikidataTests"
```

---

### Phase 24: Optimize batch ranks 70–72
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/services/focusContext/FocusContextRetrievalServiceTest.java` — "repeatRetrieveSameSiblingOrderForNullAndFixedSeed()" (~19ms)
- `backend/src/test/java/com/odde/doughnut/controllers/AdminUserControllerTest.java` — "canListUsersWithLastNoteTime()" (~19ms)
- `backend/src/test/java/com/odde/doughnut/controllers/AiControllerCreateExtractedNoteTest.java` — "[1] sourceInFolder = false" (~19ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest" --tests "com.odde.doughnut.controllers.AdminUserControllerTest" --tests "com.odde.doughnut.controllers.AiControllerCreateExtractedNoteTest"
```

---

### Phase 25: Optimize batch ranks 73–75
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/controllers/NoteControllerTests.java` — "shouldUseSuffixedPropertyKeyWhenSourceAlreadyHasPropertyKey()" (~19ms)
- `backend/src/test/java/com/odde/doughnut/controllers/NoteControllerTests.java` — "shouldRehomeRelationNoteLevelTrackerAsPropertyTrackerOnSource()" (~19ms)
- `backend/src/test/java/com/odde/doughnut/services/search/UserModelSearchTest.java` — "[3] allMyNotebooksAndSubscriptions = "true", allMyCircle = "true", expectOtherNotebooks = "true", expectSubscription = "true", expectCircleNote = "true", expectedCount = "4"" (~19ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.NoteControllerTests" --tests "com.odde.doughnut.services.search.UserModelSearchTest"
```

---

### Phase 26: Optimize batch ranks 76–78
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/configs/ControllerSetupTest.java` — "[1] githubSucceeds = true, expectedIssueNumber = 123, expectedErrorFragment = null" (~18ms)
- `backend/src/test/java/com/odde/doughnut/controllers/AdminUserControllerTest.java` — "paginationWorksCorrectly()" (~18ms)
- `backend/src/test/java/com/odde/doughnut/controllers/AiControllerCreateExtractedNoteTest.java` — "[2] sourceInFolder = true" (~18ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.configs.ControllerSetupTest" --tests "com.odde.doughnut.controllers.AdminUserControllerTest" --tests "com.odde.doughnut.controllers.AiControllerCreateExtractedNoteTest"
```

---

### Phase 27: Optimize batch ranks 79–81
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/controllers/AiControllerExtractNotePreviewTest.java` — "shouldCallExtractNoteWithStructuredInstructions()" (~18ms)
- `backend/src/test/java/com/odde/doughnut/controllers/NoteControllerTests.java` — "shouldRehomeTrackerWithSuffixedPropertyKeyWhenSourceAlreadyHasPropertyKey()" (~18ms)
- `backend/src/test/java/com/odde/doughnut/controllers/NoteControllerTests.java` — "shouldUseExampleOfPropertyKeyWhenReducingExampleOfRelation()" (~18ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.AiControllerExtractNotePreviewTest" --tests "com.odde.doughnut.controllers.NoteControllerTests"
```

---

### Phase 28: Optimize batch ranks 82–84
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/controllers/NoteControllerTests.java` — "shouldRemoveDeletedNoteLinksFromReferrerPropertiesOnly()" (~18ms)
- `backend/src/test/java/com/odde/doughnut/services/QuestionGenerationBatchJsonlRendererTest.java` — "includesHighReasoningEffortForReasoningModel()" (~18ms)
- `backend/src/test/java/com/odde/doughnut/algorithms/FrontmatterAliasesTest.java` — "fromFrontmatter_returns_valid_yaml_list_items()" (~17ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.NoteControllerTests" --tests "com.odde.doughnut.services.QuestionGenerationBatchJsonlRendererTest" --tests "com.odde.doughnut.algorithms.FrontmatterAliasesTest"
```

---

### Phase 29: Optimize batch ranks 85–87
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/configs/ObjectMapperConfigTest.java` — "shouldExcludeNullFieldsFromJsonSerialization()" (~17ms)
- `backend/src/test/java/com/odde/doughnut/controllers/AiControllerNoteRefinementTest.java` — "shouldCallResponsesApiWithStructuredInstructions()" (~17ms)
- `backend/src/test/java/com/odde/doughnut/controllers/AiControllerNoteRefinementTest.java` — "shouldReturnRegeneratedContentAfterRemovingSelectedLayoutPoints()" (~17ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.configs.ObjectMapperConfigTest" --tests "com.odde.doughnut.controllers.AiControllerNoteRefinementTest"
```

---

### Phase 30: Optimize batch ranks 88–90
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/controllers/AssimilationControllerTests.java` — "returnsOwnedNoteWhenSubscriptionDailyCapReached()" (~17ms)
- `backend/src/test/java/com/odde/doughnut/controllers/NotebookFolderManagementControllerTest.java` — "crossNotebookFolderMove_keepsCoMovedPeerLinkRelativeWhenDestinationHasSameTitleNote()" (~17ms)
- `backend/src/test/java/com/odde/doughnut/services/AssimilationServicePropertyUnitsTest.java` — "subscription_daily_budget_caps_note_and_property_units_combined()" (~17ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.AssimilationControllerTests" --tests "com.odde.doughnut.controllers.NotebookFolderManagementControllerTest" --tests "com.odde.doughnut.services.AssimilationServicePropertyUnitsTest"
```

---

### Phase 31: Optimize batch ranks 91–93
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/services/focusContext/FocusContextRetrievalServiceTest.java` — "depth2InboundCappedAtTwo()" (~16ms)
- `backend/src/test/java/com/odde/doughnut/controllers/AiControllerExportRefinementLayoutRequestTest.java` — "shouldExportRefinementLayoutRequestWithBodyMap()" (~16ms)
- `backend/src/test/java/com/odde/doughnut/controllers/AiControllerExtractNotePreviewTest.java` — "shouldTrimSurroundingWhitespaceFromExtractionPreviewTitle()" (~16ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest" --tests "com.odde.doughnut.controllers.AiControllerExportRefinementLayoutRequestTest" --tests "com.odde.doughnut.controllers.AiControllerExtractNotePreviewTest"
```

---

### Phase 32: Optimize batch ranks 94–96
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/controllers/NotebookBooksAttachControllerTest.java` — "persistsNestedOutlineAndReturnsBookWithBlocks()" (~16ms)
- `backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java` — "shouldPassOldQuestionAndContestResultToOpenAiApi()" (~16ms)
- `backend/src/test/java/com/odde/doughnut/controllers/SearchControllerAliasTests.java` — "shouldReturnNotesMatchingFrontmatterAliases()" (~16ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.NotebookBooksAttachControllerTest" --tests "com.odde.doughnut.controllers.RecallPromptControllerTests" --tests "com.odde.doughnut.controllers.SearchControllerAliasTests"
```

---

### Phase 33: Optimize batch ranks 97–99
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/services/NoteQuestionGenerationServiceTests.java` — "shouldUseSameRequestShapeAsExportedQuestionGenerationRequest()" (~16ms)
- `backend/src/test/java/com/odde/doughnut/services/QuestionGenerationBatchMetricsTest.java` — "incrementsCountersForRepresentativeBatchLifecycleOutcomes()" (~16ms)
- `backend/src/test/java/com/odde/doughnut/services/book/GcsBookStorageTest.java` — "[1] format = "pdf", extension = ".pdf", contentType = "application/pdf"" (~16ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.services.NoteQuestionGenerationServiceTests" --tests "com.odde.doughnut.services.QuestionGenerationBatchMetricsTest" --tests "com.odde.doughnut.services.book.GcsBookStorageTest"
```

---

### Phase 34: Optimize batch ranks 100–102
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/services/search/NoteSearchServiceExactMatchTest.java` — "shouldIncludeExactMatchesEvenWhenMoreThan20PartialMatches()" (~16ms)
- `backend/src/test/java/com/odde/doughnut/controllers/AiControllerExtractNoteValidationTest.java` — "shouldThrowWhenAiReturnsNull()" (~15ms)
- `backend/src/test/java/com/odde/doughnut/controllers/AssimilationControllerTests.java` — "returns_next_property_key_for_untracked_example_of()" (~15ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.services.search.NoteSearchServiceExactMatchTest" --tests "com.odde.doughnut.controllers.AiControllerExtractNoteValidationTest" --tests "com.odde.doughnut.controllers.AssimilationControllerTests"
```

---

### Phase 35: Optimize batch ranks 103–105
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/controllers/NoteControllerTests.java` — "shouldAddRelationLabelPropertyToSourceAndSoftDeleteRelationNote()" (~15ms)
- `backend/src/test/java/com/odde/doughnut/controllers/NoteControllerTests.java` — "shouldSkipUnreadableLowestIdAliasCandidateForReadableTarget()" (~15ms)
- `backend/src/test/java/com/odde/doughnut/controllers/PredefinedQuestionRefinementControllerTests.java` — "shouldThrowWhenOpenAiNotAvailable()" (~15ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.NoteControllerTests" --tests "com.odde.doughnut.controllers.PredefinedQuestionRefinementControllerTests"
```

---

### Phase 36: Optimize batch ranks 106–108
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java` — "rejected()" (~15ms)
- `backend/src/test/java/com/odde/doughnut/controllers/RelationControllerMoveNoteToFolderTests.java` — "sameNotebookMoveToFolder_doesNotRewriteLinks()" (~15ms)
- `backend/src/test/java/com/odde/doughnut/controllers/TextContentControllerTests.java` — "updateVisibleText_rewritesWikiLinkInsideYamlFrontmatter()" (~15ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.RecallPromptControllerTests" --tests "com.odde.doughnut.controllers.RelationControllerMoveNoteToFolderTests" --tests "com.odde.doughnut.controllers.TextContentControllerTests"
```

---

### Phase 37: Optimize batch ranks 109–111
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/services/AssimilationServicePropertyWikiLinkGateTest.java` — "gates_list_property_until_all_resolved_targets_are_assimilated()" (~15ms)
- `backend/src/test/java/com/odde/doughnut/services/focusContext/FocusContextRetrievalServiceTest.java` — "differentSeedsProduceDifferentSampleSiblingsSelection()" (~15ms)
- `backend/src/test/java/com/odde/doughnut/services/openAiApis/StructuredResponseCreateParamsSerializerTest.java` — "rendersResponsesApiBodyShapeWithoutSyntheticValidField()" (~15ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.services.AssimilationServicePropertyWikiLinkGateTest" --tests "com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest" --tests "com.odde.doughnut.services.openAiApis.StructuredResponseCreateParamsSerializerTest"
```

---

### Phase 38: Optimize batch ranks 112–114
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/services/AssimilationServiceDailyCapTest.java` — "returnsNextNotePastUserDailyCap()" (~14ms)
- `backend/src/test/java/com/odde/doughnut/services/focusContext/FocusContextRetrievalServiceTest.java` — "depth1InboundExcludesOutgoingTargetsBeforeCap()" (~14ms)
- `backend/src/test/java/com/odde/doughnut/controllers/MemoryTrackerControllerTest.java` — "shouldThrowWhenOpenAiNotAvailableAndGeneratingQuestion()" (~14ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.services.AssimilationServiceDailyCapTest" --tests "com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest" --tests "com.odde.doughnut.controllers.MemoryTrackerControllerTest"
```

---

### Phase 39: Optimize batch ranks 115–117
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/controllers/MemoryTrackerControllerTest.java` — "shouldReturnRecentlyRecalledForCurrentUser()" (~14ms)
- `backend/src/test/java/com/odde/doughnut/controllers/NoteControllerTests.java` — "shouldResolveAmbiguousAliasToLowestNoteId()" (~14ms)
- `backend/src/test/java/com/odde/doughnut/controllers/NotebookFolderManagementControllerTest.java` — "movesFolderSubtreeToAnotherNotebookRoot()" (~14ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.MemoryTrackerControllerTest" --tests "com.odde.doughnut.controllers.NoteControllerTests" --tests "com.odde.doughnut.controllers.NotebookFolderManagementControllerTest"
```

---

### Phase 40: Optimize batch ranks 118–120
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/controllers/NotebookFolderManagementControllerTest.java` — "mergesRecursivelyAcrossNotebooksWhenMergeRequested()" (~14ms)
- `backend/src/test/java/com/odde/doughnut/controllers/NotebookFolderManagementControllerTest.java` — "movesFolderSubtreeIntoFolderInAnotherNotebook()" (~14ms)
- `backend/src/test/java/com/odde/doughnut/controllers/RelationControllerMoveNoteToFolderTests.java` — "moveNoteIntoFolder_collectsPeersAndIsIdempotent()" (~14ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.NotebookFolderManagementControllerTest" --tests "com.odde.doughnut.controllers.RelationControllerMoveNoteToFolderTests"
```

---

### Phase 41: Optimize batch ranks 121–123
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/services/NoteQuestionGenerationServiceTests.java` — "usesMediumReasoningForReasoningModel()" (~14ms)
- `backend/src/test/java/com/odde/doughnut/services/QuestionGenerationBatchSubmissionServiceTest.java` — "updatesLocalBatchWithSubmittedAt()" (~14ms)
- `backend/src/test/java/com/odde/doughnut/services/focusContext/FocusContextRetrievalServiceTest.java` — "budgetExhaustedMidRingLeavesLaterDepthOneNotesAndDepthTwoUnreachable()" (~14ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.services.NoteQuestionGenerationServiceTests" --tests "com.odde.doughnut.services.QuestionGenerationBatchSubmissionServiceTest" --tests "com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest"
```

---

### Phase 42: Optimize batch ranks 124–126
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/algorithms/ClozeDescriptionTest.java` — "clozeShouldMaskEvictedWhenTitleIsEvict()" (~13ms)
- `backend/src/test/java/com/odde/doughnut/controllers/AiControllerExtractNotePreviewTest.java` — "shouldSanitizePathSeparatorsInExtractionPreview()" (~13ms)
- `backend/src/test/java/com/odde/doughnut/controllers/AiControllerTest.java` — "shouldTrimSurroundingWhitespaceFromSuggestedTitle()" (~13ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.algorithms.ClozeDescriptionTest" --tests "com.odde.doughnut.controllers.AiControllerExtractNotePreviewTest" --tests "com.odde.doughnut.controllers.AiControllerTest"
```

---

### Phase 43: Optimize batch ranks 127–129
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/controllers/AiControllerTest.java` — "shouldSanitizePathSeparatorsInSuggestedTitle()" (~13ms)
- `backend/src/test/java/com/odde/doughnut/controllers/NotebookRootNoteCreationWithWikidataTests.java` — "shouldBeAbleToSaveNoteWithWikidataIdWhenValid()" (~13ms)
- `backend/src/test/java/com/odde/doughnut/controllers/PredefinedQuestionControllerTests.java` — "shouldExportQuestionGenerationWithAllNonEmptyFields()" (~13ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.AiControllerTest" --tests "com.odde.doughnut.controllers.NotebookRootNoteCreationWithWikidataTests" --tests "com.odde.doughnut.controllers.PredefinedQuestionControllerTests"
```

---

### Phase 44: Optimize batch ranks 130–132
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/controllers/PredefinedQuestionRefinementControllerTests.java` — "givenQuestion_thenReturnPostProcessedRefinedQuestion()" (~13ms)
- `backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java` — "shouldAcceptThinkingTimeMs()" (~13ms)
- `backend/src/test/java/com/odde/doughnut/controllers/RelationControllerTests.java` — "crossNotebookMove_rewritesMovedNotesOutgoingUnqualifiedLinks()" (~13ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.PredefinedQuestionRefinementControllerTests" --tests "com.odde.doughnut.controllers.RecallPromptControllerTests" --tests "com.odde.doughnut.controllers.RelationControllerTests"
```

---

### Phase 45: Optimize batch ranks 133–135
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/controllers/TextContentControllerTests.java` — "updateVisibleText_preservesExplicitDisplayTextAndRefreshesInboundMetadata()" (~13ms)
- `backend/src/test/java/com/odde/doughnut/services/NoteQuestionGenerationServiceTests.java` — "nestedLabeledQuestionInstructionsAppearInFirstUserMessageInOrder()" (~13ms)
- `backend/src/test/java/com/odde/doughnut/services/NoteRealmServiceTest.java` — "notebook_index_applies_title_pattern_and_question_instruction_to_sibling_notes()" (~13ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.TextContentControllerTests" --tests "com.odde.doughnut.services.NoteQuestionGenerationServiceTests" --tests "com.odde.doughnut.services.NoteRealmServiceTest"
```

---

### Phase 46: Optimize batch ranks 136–138
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/services/focusContext/FocusContextRetrievalServiceTest.java` — "shorterPathWinsWhenSameNoteReachableAtDepthOneAndTwo()" (~13ms)
- `backend/src/test/java/com/odde/doughnut/services/search/UserModelSearchTest.java` — "[2] allMyNotebooksAndSubscriptions = "true", allMyCircle = "false", expectOtherNotebooks = "true", expectSubscription = "true", expectCircleNote = "false", expectedCount = "3"" (~13ms)
- `backend/src/test/java/com/odde/doughnut/controllers/ConversationMessageControllerTest.java` — "shouldSetRecallPromptAsSubject()" (~12ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.services.focusContext.FocusContextRetrievalServiceTest" --tests "com.odde.doughnut.services.search.UserModelSearchTest" --tests "com.odde.doughnut.controllers.ConversationMessageControllerTest"
```

---

### Phase 47: Optimize batch ranks 139–141
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/configs/ControllerSetupTest.java` — "[2] githubSucceeds = false, expectedIssueNumber = null, expectedErrorFragment = "GitHub issue creation failed"" (~12ms)
- `backend/src/test/java/com/odde/doughnut/controllers/BooksControllerTest.java` — "returnsEpubZipWhenBookFormatIsEpub()" (~12ms)
- `backend/src/test/java/com/odde/doughnut/controllers/ConversationMessageControllerTest.java` — "testConversationsOrderedByLastMessageTime()" (~12ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.configs.ControllerSetupTest" --tests "com.odde.doughnut.controllers.BooksControllerTest" --tests "com.odde.doughnut.controllers.ConversationMessageControllerTest"
```

---

### Phase 48: Optimize batch ranks 142–144
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/controllers/NoteControllerRecentNotesTests.java` — "shouldReturnEmptyListWhenNoNotes()" (~12ms)
- `backend/src/test/java/com/odde/doughnut/controllers/NoteControllerTests.java` — "shouldResolveWikiLinkToSingleFrontmatterAliasTarget()" (~12ms)
- `backend/src/test/java/com/odde/doughnut/controllers/NotebookBooksAttachControllerTest.java` — "persistsFromContentListBuildsBeginningAndChapter()" (~12ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.NoteControllerRecentNotesTests" --tests "com.odde.doughnut.controllers.NoteControllerTests" --tests "com.odde.doughnut.controllers.NotebookBooksAttachControllerTest"
```

---

### Phase 49: Optimize batch ranks 145–147
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/controllers/NotebookFolderManagementControllerTest.java` — "rejectsCrossNotebookMoveWhenSoftDeletedNoteHasSameTitleAtDestination()" (~12ms)
- `backend/src/test/java/com/odde/doughnut/controllers/NotebookRootNoteCreationWithWikidataTests.java` — "siblingsShareSameFolder()" (~12ms)
- `backend/src/test/java/com/odde/doughnut/controllers/PredefinedQuestionRefinementControllerTests.java` — "refineQuestionFailedWithGpt35WillNotTryAgain()" (~12ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.NotebookFolderManagementControllerTest" --tests "com.odde.doughnut.controllers.NotebookRootNoteCreationWithWikidataTests" --tests "com.odde.doughnut.controllers.PredefinedQuestionRefinementControllerTests"
```

---

### Phase 50: Optimize batch ranks 148–150
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/controllers/PredefinedQuestionRefinementControllerTests.java` — "invalidRefinedQuestionIsRejected()" (~12ms)
- `backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java` — "acceptTheContest()" (~12ms)
- `backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java` — "shouldThrowWhenOpenAiNotAvailable()" (~12ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.PredefinedQuestionRefinementControllerTests" --tests "com.odde.doughnut.controllers.RecallPromptControllerTests"
```

---

### Phase 51: Optimize batch ranks 151–152
Status: planned

**Tests:**
- `backend/src/test/java/com/odde/doughnut/controllers/RelationControllerTests.java` — "[1] before = "[[MyNote]]", after = "[[NewNb:MyNote\|MyNote]]"" (~12ms)
- `backend/src/test/java/com/odde/doughnut/controllers/RelationControllerTests.java` — "[3] before = "[[OldNb:MyNote\|custom text]]", after = "[[NewNb:MyNote\|custom text]]"" (~12ms)

**Goals:** Speed up only these tests (merge/delete redundant cases, slim `makeMe`/fixtures, parameterize duplicates, avoid full-stack when a narrower entry suffices). If no meaningful win after a serious attempt, append **Candidates** in the blacklist and mark done.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.controllers.RelationControllerTests"
```

---

### Phase 52: Re-profile and close
Status: planned

Re-run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`, update metrics, set Status: done, archive summary to `ongoing/archive/backend-test-optimization-history.md`, delete this working plan. Keep the blacklist.

| Metric | Before | After |
|--------|--------|-------|
| Test count | 1517 | |
| Suite wall | ~36s | |
| Top 10% total time | ~6942ms | |

**Candidates proposed this run:** (none / list)

**Commits:**
