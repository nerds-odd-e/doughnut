# Doughnut Backend API Summary

Generated from `open_api_docs.yaml` by `scripts/generate-api-summary.mjs`.
Use this for endpoint lookup; open `sdk.gen.ts` or `types.gen.ts` only for exact generated signatures.

## Notebook Books Controller

- `putNotebookBookBlockReadingRecord`: PUT `/api/notebooks/{notebook}/book/blocks/{bookBlock}/reading-record` -> `PutNotebookBookBlockReadingRecordResponse` (request: `PutNotebookBookBlockReadingRecordData`; path: notebook, bookBlock; body: BookBlockReadingRecordPutRequest; response body: Array<BookBlockReadingRecordListItem>)
- `changeBookBlockDepth`: PUT `/api/notebooks/{notebook}/book/blocks/{bookBlock}/depth` -> `ChangeBookBlockDepthResponse` (request: `ChangeBookBlockDepthData`; path: notebook, bookBlock; body: BookBlockDepthRequestFull; response body: BookMutationResponseFull)
- `suggestBookLayoutReorganization`: POST `/api/notebooks/{notebook}/book/reorganize-layout/suggest` -> `SuggestBookLayoutReorganizationResponse` (request: `SuggestBookLayoutReorganizationData`; path: notebook; response body: BookLayoutReorganizationSuggestion)
- `applyBookLayoutReorganization`: POST `/api/notebooks/{notebook}/book/reorganize-layout/apply` -> `ApplyBookLayoutReorganizationResponse` (request: `ApplyBookLayoutReorganizationData`; path: notebook; body: BookLayoutReorganizationSuggestionFull; response body: BookMutationResponseFull)
- `createBookBlockFromContent`: POST `/api/notebooks/{notebook}/book/blocks` -> `CreateBookBlockFromContentResponse` (request: `CreateBookBlockFromContentData`; path: notebook; body: CreateBookBlockFromContentRequestFull; response body: BookFull)
- `attachBook`: POST `/api/notebooks/{notebook}/attach-book` -> `AttachBookResponse` (request: `AttachBookData`; path: notebook; body: object; response body: BookFull)
- `getNotebookBookReadingPosition`: GET `/api/notebooks/{notebook}/book/reading-position` -> `GetNotebookBookReadingPositionResponse` (request: `GetNotebookBookReadingPositionData`; path: notebook; response body: BookUserLastReadPosition)
- `patchNotebookBookReadingPosition`: PATCH `/api/notebooks/{notebook}/book/reading-position` -> `PatchNotebookBookReadingPositionResponse` (request: `PatchNotebookBookReadingPositionData`; path: notebook; body: BookLastReadPositionRequest; response body: void)
- `getBook`: GET `/api/notebooks/{notebook}/book` -> `GetBookResponse` (request: `GetBookData`; path: notebook; response body: BookFull)
- `deleteBook`: DELETE `/api/notebooks/{notebook}/book` -> `DeleteBookResponse` (request: `DeleteBookData`; path: notebook; response body: void)
- `getNotebookBookReadingRecords`: GET `/api/notebooks/{notebook}/book/reading-records` -> `GetNotebookBookReadingRecordsResponse` (request: `GetNotebookBookReadingRecordsData`; path: notebook; response body: Array<BookBlockReadingRecordListItem>)
- `getBookFile`: GET `/api/notebooks/{notebook}/book/file` -> `GetBookFileResponse` (request: `GetBookFileData`; path: notebook; response body: string)
- `cancelBookBlock`: DELETE `/api/notebooks/{notebook}/book/blocks/{bookBlock}` -> `CancelBookBlockResponse` (request: `CancelBookBlockData`; path: notebook, bookBlock; response body: BookMutationResponseFull)

## User Controller

- `getUserProfile`: GET `/api/user` -> `GetUserProfileResponse` (request: none; response body: User)
- `createUser`: POST `/api/user` -> `CreateUserResponse` (request: `CreateUserData`; body: User; response body: User)
- `generateToken`: POST `/api/user/generate-token` -> `GenerateTokenResponse` (request: `GenerateTokenData`; body: TokenConfigDto; response body: GeneratedTokenDto)
- `updateUser`: PATCH `/api/user/{user}` -> `UpdateUserResponse` (request: `UpdateUserData`; path: user; body: UserDto; response body: User)
- `getTokenInfo`: GET `/api/user/token-info` -> `GetTokenInfoResponse` (request: none; response body: UserToken)
- `revokeToken`: DELETE `/api/user/token-info` -> `RevokeTokenResponse` (request: none; response body: void)
- `getQuestionGenerationBatchSchedule`: GET `/api/user/question-generation-batch-schedule` -> `GetQuestionGenerationBatchScheduleResponse` (request: none; response body: QuestionGenerationBatchUserScheduleDto)
- `getMenuData`: GET `/api/user/menu-data` -> `GetMenuDataResponse` (request: `GetMenuDataData`; query: timezone; response body: MenuDataDto)
- `getTokens`: GET `/api/user/get-tokens` -> `GetTokensResponse` (request: none; response body: Array<UserToken>)
- `deleteToken`: DELETE `/api/user/token/{tokenId}` -> `DeleteTokenResponse` (request: `DeleteTokenData`; path: tokenId; response body: void)

## Testability Rest Controller

- `closeAllGithubIssues`: POST `/api/testability/use_real_sandbox_github_and_close_all_github_issues` -> `CloseAllGithubIssuesResponse` (request: none; response body: string)
- `triggerException`: POST `/api/testability/trigger_exception` -> `TriggerExceptionResponse` (request: none; response body: string)
- `timeTravelRelativeToNow`: POST `/api/testability/time_travel_relative_to_now` -> `TimeTravelRelativeToNowResponse` (request: `TimeTravelRelativeToNowData`; body: TimeTravelRelativeToNow; response body: Array<unknown>)
- `timeTravel`: POST `/api/testability/time_travel` -> `TimeTravelResponse` (request: `TimeTravelData`; body: TimeTravel; response body: Array<unknown>)
- `testabilityUpdateUser`: POST `/api/testability/testability_update_user` -> `TestabilityUpdateUserResponse` (request: `TestabilityUpdateUserData`; query: username; body: Record<string, string>; response body: string)
- `shareToBazaar`: POST `/api/testability/share_to_bazaar` -> `ShareToBazaarResponse` (request: `ShareToBazaarData`; body: ShareToBazaarRequest; response body: string)
- `replaceServiceUrl`: POST `/api/testability/replace_service_url` -> `ReplaceServiceUrlResponse` (request: `ReplaceServiceUrlData`; body: Record<string, string>; response body: void)
- `randomizer`: POST `/api/testability/randomizer` -> `RandomizerResponse` (request: `RandomizerData`; body: Randomization; response body: Array<unknown>)
- `setOpenAiToken`: POST `/api/testability/open_ai_token` -> `SetOpenAiTokenResponse` (request: `SetOpenAiTokenData`; body: Record<string, string>; response body: void)
- `injectNotes`: POST `/api/testability/inject_notes` -> `InjectNotesResponse` (request: `InjectNotesData`; body: NotesTestData; response body: Record<string, number>)
- `injectCircle`: POST `/api/testability/inject_circle` -> `InjectCircleResponse` (request: `InjectCircleData`; body: Record<string, string>; response body: string)
- `injectPredefinedQuestion`: POST `/api/testability/inject-predefined-questions` -> `InjectPredefinedQuestionResponse` (request: `InjectPredefinedQuestionData`; body: PredefinedQuestionsTestData; response body: Array<PredefinedQuestion>)
- `getFeatureToggle`: GET `/api/testability/feature_toggle` -> `GetFeatureToggleResponse` (request: none; response body: boolean)
- `enableFeatureToggle`: POST `/api/testability/feature_toggle` -> `EnableFeatureToggleResponse` (request: `EnableFeatureToggleData`; body: Record<string, string>; response body: Array<unknown>)
- `resetDBAndTestabilitySettings`: POST `/api/testability/clean_db_and_reset_testability_settings` -> `ResetDbAndTestabilitySettingsResponse` (request: none; response body: string)
- `githubIssues`: GET `/api/testability/github_issues` -> `GithubIssuesResponse` (request: none; response body: Array<Record<string, unknown>>)

## Subscription Controller

- `updateSubscription`: POST `/api/subscriptions/{subscription}` -> `UpdateSubscriptionResponse` (request: `UpdateSubscriptionData`; path: subscription; body: SubscriptionDto; response body: Subscription)
- `destroySubscription`: POST `/api/subscriptions/{subscription}/delete` -> `DestroySubscriptionResponse` (request: `DestroySubscriptionData`; path: subscription; response body: Array<number>)
- `createSubscription`: POST `/api/subscriptions/notebooks/{notebook}/subscribe` -> `CreateSubscriptionResponse` (request: `CreateSubscriptionData`; path: notebook; body: SubscriptionDto; response body: Subscription)
- `updateSubscriptionGroup`: PATCH `/api/subscriptions/{subscription}/notebook-group` -> `UpdateSubscriptionGroupResponse` (request: `UpdateSubscriptionGroupData`; path: subscription; body: UpdateNotebookGroupRequest; response body: Subscription)

## Global Settings Controller

- `getCurrentModelVersions`: GET `/api/settings/current-model-version` -> `GetCurrentModelVersionsResponse` (request: none; response body: GlobalAiModelSettings)
- `setCurrentModelVersions`: POST `/api/settings/current-model-version` -> `SetCurrentModelVersionsResponse` (request: `SetCurrentModelVersionsData`; body: GlobalAiModelSettings; response body: GlobalAiModelSettings)

## Relation Controller

- `moveNoteToNotebookRoot`: POST `/api/relations/move-to-notebook-root/{sourceNote}` -> `MoveNoteToNotebookRootResponse` (request: `MoveNoteToNotebookRootData`; path: sourceNote; response body: Array<NoteRealm>)
- `moveNoteToNotebookRootInNotebook`: POST `/api/relations/move-to-notebook-root/{sourceNote}/{targetNotebook}` -> `MoveNoteToNotebookRootInNotebookResponse` (request: `MoveNoteToNotebookRootInNotebookData`; path: sourceNote, targetNotebook; response body: Array<NoteRealm>)
- `moveNoteToFolder`: POST `/api/relations/move-to-folder/{sourceNote}/{targetFolder}` -> `MoveNoteToFolderResponse` (request: `MoveNoteToFolderData`; path: sourceNote, targetFolder; response body: Array<NoteRealm>)

## Recall Prompt Controller

- `regenerate`: POST `/api/recall-prompts/{recallPrompt}/regenerate` -> `RegenerateResponse` (request: `RegenerateData`; path: recallPrompt; body: QuestionContestResult; response body: RecallQuestion)
- `contest`: POST `/api/recall-prompts/{recallPrompt}/contest` -> `ContestResponse` (request: `ContestData`; path: recallPrompt; response body: QuestionContestResult)
- `answerQuiz`: POST `/api/recall-prompts/{recallPrompt}/answer` -> `AnswerQuizResponse` (request: `AnswerQuizData`; path: recallPrompt; body: AnswerDto; response body: AnsweredQuestion)
- `answerSpelling`: POST `/api/recall-prompts/{recallPrompt}/answer-spelling` -> `AnswerSpellingResponse` (request: `AnswerSpellingData`; path: recallPrompt; body: AnswerSpellingDto; response body: AnsweredQuestion)

## Predefined Question Controller

- `refineQuestion`: POST `/api/predefined-questions/{note}/refine-question` -> `RefineQuestionResponse` (request: `RefineQuestionData`; path: note; body: PredefinedQuestion; response body: PredefinedQuestion)
- `getAllQuestionByNote`: GET `/api/predefined-questions/{note}/note-questions` -> `GetAllQuestionByNoteResponse` (request: `GetAllQuestionByNoteData`; path: note; response body: Array<PredefinedQuestion>)
- `addQuestionManually`: POST `/api/predefined-questions/{note}/note-questions` -> `AddQuestionManuallyResponse` (request: `AddQuestionManuallyData`; path: note; body: PredefinedQuestion; response body: PredefinedQuestion)
- `generateQuestionWithoutSave`: POST `/api/predefined-questions/generate-question-without-save` -> `GenerateQuestionWithoutSaveResponse` (request: `GenerateQuestionWithoutSaveData`; query: note; response body: PredefinedQuestion)
- `exportQuestionGeneration`: GET `/api/predefined-questions/{note}/export-question-generation` -> `ExportQuestionGenerationResponse` (request: `ExportQuestionGenerationData`; path: note; response body: Record<string, unknown>)

## Note Controller

- `verifySpelling`: POST `/api/notes/{note}/verify-spelling` -> `VerifySpellingResponse` (request: `VerifySpellingData`; path: note; body: AnswerSpellingDto; response body: SpellingVerificationResult)
- `updateNoteRecallSetting`: POST `/api/notes/{note}/recall-setting` -> `UpdateNoteRecallSettingResponse` (request: `UpdateNoteRecallSettingData`; path: note; body: NoteRecallSetting; response body: RedirectToNoteResponse)
- `uploadNoteImage`: POST `/api/notes/{note}/images` -> `UploadNoteImageResponse` (request: `UploadNoteImageData`; path: note; body: NoteImageUploadDto; response body: NoteImageUploadResult)
- `deleteNote`: POST `/api/notes/{note}/delete` -> `DeleteNoteResponse` (request: `DeleteNoteData`; path: note; body: NoteDeleteDto; response body: Array<NoteRealm>)
- `undoDeleteNote`: PATCH `/api/notes/{note}/undo-delete` -> `UndoDeleteNoteResponse` (request: `UndoDeleteNoteData`; path: note; response body: NoteRealm)
- `showNote`: GET `/api/notes/{note}` -> `ShowNoteResponse` (request: `ShowNoteData`; path: note; response body: NoteRealm)
- `getNoteInfo`: GET `/api/notes/{note}/note-info` -> `GetNoteInfoResponse` (request: `GetNoteInfoData`; path: note; response body: NoteRecallInfo)
- `getGraph`: GET `/api/notes/{note}/graph` -> `GetGraphResponse` (request: `GetGraphData`; path: note; query: tokenLimit; response body: FocusContextResult)
- `getAiContextMarkdown`: GET `/api/notes/{note}/ai-context-markdown` -> `GetAiContextMarkdownResponse` (request: `GetAiContextMarkdownData`; path: note; query: tokenLimit; response body: NoteAiContextMarkdown)
- `getRecentNotes`: GET `/api/notes/recent` -> `GetRecentNotesResponse` (request: none; response body: Array<NoteSearchResult>)

## Search Controller

- `semanticSearchWithin`: POST `/api/notes/{note}/semantic-search` -> `SemanticSearchWithinResponse` (request: `SemanticSearchWithinData`; path: note; body: SearchTerm; response body: Array<NoteSearchResult>)
- `searchForRelationshipTargetWithin`: POST `/api/notes/{note}/search` -> `SearchForRelationshipTargetWithinResponse` (request: `SearchForRelationshipTargetWithinData`; path: note; body: SearchTerm; response body: Array<RelationshipLiteralSearchHit>)
- `semanticSearch`: POST `/api/notes/semantic-search` -> `SemanticSearchResponse` (request: `SemanticSearchData`; body: SearchTerm; response body: Array<NoteSearchResult>)
- `searchForRelationshipTarget`: POST `/api/notes/search` -> `SearchForRelationshipTargetResponse` (request: `SearchForRelationshipTargetData`; body: SearchTerm; response body: Array<RelationshipLiteralSearchHit>)

## Notebook Controller

- `get`: GET `/api/notebooks/{notebook}` -> `GetResponse` (request: `GetData`; path: notebook; response body: NotebookRealm)
- `updateNotebook`: POST `/api/notebooks/{notebook}` -> `UpdateNotebookResponse` (request: `UpdateNotebookData`; path: notebook; body: NotebookUpdateRequest; response body: Notebook)
- `updateNotebookIndex`: POST `/api/notebooks/{notebook}/update-index` -> `UpdateNotebookIndexResponse` (request: `UpdateNotebookIndexData`; path: notebook; response body: void)
- `shareNotebook`: POST `/api/notebooks/{notebook}/share` -> `ShareNotebookResponse` (request: `ShareNotebookData`; path: notebook; response body: Notebook)
- `resetNotebookIndex`: POST `/api/notebooks/{notebook}/reset-index` -> `ResetNotebookIndexResponse` (request: `ResetNotebookIndexData`; path: notebook; response body: void)
- `createFolder`: POST `/api/notebooks/{notebook}/folders` -> `CreateFolderResponse` (request: `CreateFolderData`; path: notebook; body: FolderCreationRequest; response body: Folder)
- `moveFolder`: POST `/api/notebooks/{notebook}/folders/{folder}/move` -> `MoveFolderResponse` (request: `MoveFolderData`; path: notebook, folder; body: FolderMoveRequest; response body: Folder)
- `createNoteAtNotebookRoot`: POST `/api/notebooks/{notebook}/create-note` -> `CreateNoteAtNotebookRootResponse` (request: `CreateNoteAtNotebookRootData`; path: notebook; body: NoteCreationDto; response body: NoteRealm)
- `createNotebook`: POST `/api/notebooks/create` -> `CreateNotebookResponse` (request: `CreateNotebookData`; body: NotebookCreationRequest; response body: NotebookRealm)
- `updateNotebookReadmeContent`: PATCH `/api/notebooks/{notebook}/readme-content` -> `UpdateNotebookReadmeContentResponse` (request: `UpdateNotebookReadmeContentData`; path: notebook; body: NoteUpdateContentDto; response body: NotebookRealm)
- `updateNotebookGroup`: PATCH `/api/notebooks/{notebook}/notebook-group` -> `UpdateNotebookGroupResponse` (request: `UpdateNotebookGroupData`; path: notebook; body: UpdateNotebookGroupRequest; response body: Notebook)
- `moveToCircle`: PATCH `/api/notebooks/{notebook}/move-to-circle/{circle}` -> `MoveToCircleResponse` (request: `MoveToCircleData`; path: notebook, circle; response body: Notebook)
- `getFolderPage`: GET `/api/notebooks/{notebook}/folders/{folder}` -> `GetFolderPageResponse` (request: `GetFolderPageData`; path: notebook, folder; response body: FolderRealm)
- `dissolveFolder`: DELETE `/api/notebooks/{notebook}/folders/{folder}` -> `DissolveFolderResponse` (request: `DissolveFolderData`; path: notebook, folder; query: merge; response body: void)
- `renameFolder`: PATCH `/api/notebooks/{notebook}/folders/{folder}` -> `RenameFolderResponse` (request: `RenameFolderData`; path: notebook, folder; body: FolderRenameRequest; response body: Folder)
- `updateFolderReadmeContent`: PATCH `/api/notebooks/{notebook}/folders/{folder}/readme-content` -> `UpdateFolderReadmeContentResponse` (request: `UpdateFolderReadmeContentData`; path: notebook, folder; body: NoteUpdateContentDto; response body: FolderRealm)
- `myNotebooks`: GET `/api/notebooks` -> `MyNotebooksResponse` (request: none; response body: NotebooksViewedByUser)
- `listNotebookFolderIndex`: GET `/api/notebooks/{notebook}/folders/index` -> `ListNotebookFolderIndexResponse` (request: `ListNotebookFolderIndexData`; path: notebook; response body: Array<Folder>)
- `listNotebookFolderListing`: GET `/api/notebooks/{notebook}/folder-listing` -> `ListNotebookFolderListingResponse` (request: `ListNotebookFolderListingData`; path: notebook; query: parent; response body: FolderListing)

## Notebook Health Controller

- `lint`: POST `/api/notebooks/{notebook}/health/lint` -> `LintResponse` (request: `LintData`; path: notebook; response body: NotebookHealthLintReport)
- `fix`: POST `/api/notebooks/{notebook}/health/fix` -> `FixResponse` (request: `FixData`; path: notebook; body: NotebookHealthFixRequest; response body: void)

## Notebook Group Controller

- `createGroup`: POST `/api/notebook-groups` -> `CreateGroupResponse` (request: `CreateGroupData`; body: CreateNotebookGroupRequest; response body: NotebookGroup)

## Memory Tracker Controller

- `softDelete`: POST `/api/memory-trackers/{memoryTracker}/soft-delete` -> `SoftDeleteResponse` (request: `SoftDeleteData`; path: memoryTracker; response body: void)
- `removeFromRepeating`: POST `/api/memory-trackers/{memoryTracker}/remove` -> `RemoveFromRepeatingResponse` (request: `RemoveFromRepeatingData`; path: memoryTracker; response body: MemoryTracker)
- `reEnable`: POST `/api/memory-trackers/{memoryTracker}/re-enable` -> `ReEnableResponse` (request: `ReEnableData`; path: memoryTracker; response body: MemoryTracker)
- `updatePropertyKey`: PATCH `/api/memory-trackers/{memoryTracker}/property-key` -> `UpdatePropertyKeyResponse` (request: `UpdatePropertyKeyData`; path: memoryTracker; body: UpdateMemoryTrackerPropertyKeyDto; response body: MemoryTracker)
- `markAsRecalled`: PATCH `/api/memory-trackers/{memoryTracker}/mark-as-recalled` -> `MarkAsRecalledResponse` (request: `MarkAsRecalledData`; path: memoryTracker; query: successful; response body: MemoryTracker)
- `showMemoryTracker`: GET `/api/memory-trackers/{memoryTracker}` -> `ShowMemoryTrackerResponse` (request: `ShowMemoryTrackerData`; path: memoryTracker; response body: MemoryTracker)
- `getThresholdExceeded`: GET `/api/memory-trackers/{memoryTracker}/threshold-exceeded` -> `GetThresholdExceededResponse` (request: `GetThresholdExceededData`; path: memoryTracker; response body: ThresholdExceededResult)
- `getRecallPrompts`: GET `/api/memory-trackers/{memoryTracker}/recall-prompts` -> `GetRecallPromptsResponse` (request: `GetRecallPromptsData`; path: memoryTracker; response body: Array<RecallPromptHistoryItem>)
- `askAQuestion`: GET `/api/memory-trackers/{memoryTracker}/question` -> `AskAQuestionResponse` (request: `AskAQuestionData`; path: memoryTracker; response body: RecallQuestion)
- `getRecentlyRecalled`: GET `/api/memory-trackers/recently-recalled` -> `GetRecentlyRecalledResponse` (request: none; response body: Array<MemoryTracker>)
- `getRecentMemoryTrackers`: GET `/api/memory-trackers/recent` -> `GetRecentMemoryTrackersResponse` (request: none; response body: Array<MemoryTracker>)
- `deleteUnansweredRecallPrompts`: DELETE `/api/memory-trackers/{memoryTracker}/recall-prompts/unanswered` -> `DeleteUnansweredRecallPromptsResponse` (request: `DeleteUnansweredRecallPromptsData`; path: memoryTracker; response body: void)

## Failure Report Controller

- `triggerFailure`: POST `/api/failure-reports/trigger` -> `TriggerFailureResponse` (request: none; response body: string)
- `failureReports`: GET `/api/failure-reports` -> `FailureReportsResponse` (request: none; response body: object)
- `showFailureReport`: GET `/api/failure-reports/{failureReport}` -> `ShowFailureReportResponse` (request: `ShowFailureReportData`; path: failureReport; response body: FailureReportForView)
- `deleteFailureReports`: DELETE `/api/failure-reports/delete` -> `DeleteFailureReportsResponse` (request: `DeleteFailureReportsData`; body: Array<number>; response body: void)

## Conversation Message Controller

- `replyToConversation`: POST `/api/conversation/{conversationId}/send` -> `ReplyToConversationResponse` (request: `ReplyToConversationData`; path: conversationId; body: string; response body: ConversationMessage)
- `getAiReply`: POST `/api/conversation/{conversationId}/ai-reply` -> `GetAiReplyResponse` (request: `GetAiReplyData`; path: conversationId; response body: SseEmitter)
- `startConversationAboutRecallPrompt`: POST `/api/conversation/recall-prompt/{recallPrompt}` -> `StartConversationAboutRecallPromptResponse` (request: `StartConversationAboutRecallPromptData`; path: recallPrompt; response body: Conversation)
- `getConversationsAboutNote`: GET `/api/conversation/note/{note}` -> `GetConversationsAboutNoteResponse` (request: `GetConversationsAboutNoteData`; path: note; response body: Array<Conversation>)
- `startConversationAboutNote`: POST `/api/conversation/note/{note}` -> `StartConversationAboutNoteResponse` (request: `StartConversationAboutNoteData`; path: note; body: string; response body: Conversation)
- `markConversationAsRead`: PATCH `/api/conversation/{conversationId}/read` -> `MarkConversationAsReadResponse` (request: `MarkConversationAsReadData`; path: conversationId; response body: Array<ConversationMessage>)
- `getConversation`: GET `/api/conversation/{conversationId}` -> `GetConversationResponse` (request: `GetConversationData`; path: conversationId; response body: Conversation)
- `getConversationMessages`: GET `/api/conversation/{conversationId}/messages` -> `GetConversationMessagesResponse` (request: `GetConversationMessagesData`; path: conversationId; response body: Array<ConversationMessage>)
- `exportConversation`: GET `/api/conversation/{conversationId}/export` -> `ExportConversationResponse` (request: `ExportConversationData`; path: conversationId; response body: Record<string, unknown>)
- `getConversationsOfCurrentUser`: GET `/api/conversation/all` -> `GetConversationsOfCurrentUserResponse` (request: none; response body: Array<ConversationListItem>)

## Circle Controller

- `index`: GET `/api/circles` -> `IndexResponse` (request: none; response body: Array<Circle>)
- `createCircle`: POST `/api/circles` -> `CreateCircleResponse` (request: `CreateCircleData`; body: Circle; response body: Circle)
- `createNotebookInCircle`: POST `/api/circles/{circle}/notebooks` -> `CreateNotebookInCircleResponse` (request: `CreateNotebookInCircleData`; path: circle; body: NotebookCreationRequest; response body: NotebookRealm)
- `joinCircle`: POST `/api/circles/join` -> `JoinCircleResponse` (request: `JoinCircleData`; body: CircleJoiningByInvitation; response body: Circle)
- `showCircle`: GET `/api/circles/{circle}` -> `ShowCircleResponse` (request: `ShowCircleData`; path: circle; response body: CircleForUserView)

## Bazaar Controller

- `removeFromBazaar`: POST `/api/bazaar/{bazaarNotebook}/remove` -> `RemoveFromBazaarResponse` (request: `RemoveFromBazaarData`; path: bazaarNotebook; response body: Array<BazaarNotebook>)
- `bazaar`: GET `/api/bazaar` -> `BazaarResponse` (request: none; response body: Array<BazaarNotebook>)

## Ai Audio Controller

- `audioToText`: POST `/api/audio/audio-to-text` -> `AudioToTextResponse` (request: `AudioToTextData`; body: AudioUploadDto; response body: TextFromAudioWithCallInfo)

## Assimilation Controller

- `assimilate`: POST `/api/assimilation` -> `AssimilateResponse` (request: `AssimilateData`; body: AssimilationRequestDto; response body: Array<MemoryTracker>)
- `next`: GET `/api/assimilation/next` -> `NextResponse` (request: `NextData`; query: timezone; response body: AssimilationNextDto)

## Ai Controller

- `suggestTitle`: POST `/api/ai/suggest-title/{note}` -> `SuggestTitleResponse` (request: `SuggestTitleData`; path: note; response body: SuggestedTitleDto)
- `removeRefinementSuggestion`: POST `/api/ai/remove-refinement-suggestion/{note}` -> `RemoveRefinementSuggestionResponse` (request: `RemoveRefinementSuggestionData`; path: note; body: NoteRefinementLayoutSelectionRequestDto; response body: RefinedContentResponseDto)
- `generateRefinementSuggestions`: POST `/api/ai/generate-refinement-suggestions/{note}` -> `GenerateRefinementSuggestionsResponse` (request: `GenerateRefinementSuggestionsData`; path: note; response body: NoteRefinementLayoutDto)
- `extractNotePreview`: POST `/api/ai/extract-note-preview/{note}` -> `ExtractNotePreviewResponse` (request: `ExtractNotePreviewData`; path: note; body: NoteRefinementLayoutSelectionRequestDto; response body: NoteExtractionResult)
- `exportExtractRequest`: POST `/api/ai/export-extract-request/{note}` -> `ExportExtractRequestResponse` (request: `ExportExtractRequestData`; path: note; body: NoteRefinementLayoutSelectionRequestDto; response body: Record<string, unknown>)
- `createExtractedNote`: POST `/api/ai/create-extracted-note/{note}` -> `CreateExtractedNoteResponse` (request: `CreateExtractedNoteData`; path: note; body: NoteExtractionResult; response body: NoteRealm)
- `exportRefinementLayoutRequest`: GET `/api/ai/export-refinement-layout-request/{note}` -> `ExportRefinementLayoutRequestResponse` (request: `ExportRefinementLayoutRequestData`; path: note; response body: Record<string, unknown>)
- `dummyEntryToGenerateDataTypesThatAreRequiredInEventStream`: GET `/api/ai/dummy` -> `DummyEntryToGenerateDataTypesThatAreRequiredInEventStreamResponse` (request: none; response body: DummyForGeneratingTypes)
- `getAvailableGptModels`: GET `/api/ai/available-gpt-models` -> `GetAvailableGptModelsResponse` (request: none; response body: Array<string>)

## Admin Question Generation Batch Controller

- `submitRecentRecallUsersForQuestionGenerationBatch`: POST `/api/admin/question-generation-batch/submit-recent-recall-users` -> `SubmitRecentRecallUsersForQuestionGenerationBatchResponse` (request: none; response body: QuestionGenerationBatchSubmissionSummaryDto)
- `resumeExistingQuestionGenerationBatches`: POST `/api/admin/question-generation-batch/resume-existing-batches` -> `ResumeExistingQuestionGenerationBatchesResponse` (request: none; response body: QuestionGenerationBatchAdminStatusDto)
- `getQuestionGenerationBatchStatus`: GET `/api/admin/question-generation-batch/status` -> `GetQuestionGenerationBatchStatusResponse` (request: none; response body: QuestionGenerationBatchAdminStatusDto)

## Text Content Controller

- `updateNoteTitle`: PATCH `/api/text_content/{note}/title` -> `UpdateNoteTitleResponse` (request: `UpdateNoteTitleData`; path: note; body: NoteUpdateTitleDto; response body: NoteRealm)
- `updateNoteContent`: PATCH `/api/text_content/{note}/content` -> `UpdateNoteContentResponse` (request: `UpdateNoteContentData`; path: note; body: NoteUpdateContentDto; response body: NoteRealm)

## Install Controller

- `install`: GET `/install` -> `InstallResponse` (request: `InstallData`; query: win32; response body: string)

## Wikidata Controller

- `searchWikidata`: GET `/api/wikidata/search` -> `SearchWikidataResponse` (request: `SearchWikidataData`; query: search; response body: Array<WikidataSearchEntity>)
- `fetchWikidataEntityDataByID`: GET `/api/wikidata/entity-data/{wikidataId}` -> `FetchWikidataEntityDataByIdResponse` (request: `FetchWikidataEntityDataByIdData`; path: wikidataId; response body: WikidataEntityData)

## Current User Info Controller

- `currentUserInfo`: GET `/api/user/current-user-info` -> `CurrentUserInfoResponse` (request: none; response body: CurrentUserInfo)

## Recalls Controller

- `recalling`: GET `/api/recalls/recalling` -> `RecallingResponse` (request: `RecallingData`; query: timezone, dueindays; response body: DueMemoryTrackers)
- `previouslyAnswered`: GET `/api/recalls/previously-answered` -> `PreviouslyAnsweredResponse` (request: `PreviouslyAnsweredData`; query: timezone; response body: Array<AnsweredQuestion>)

## Health Check Controller

- `ping`: GET `/api/healthcheck` -> `PingResponse` (request: none; response body: string)
- `dataUpgrade`: GET `/api/data_upgrade` -> `DataUpgradeResponse` (request: none; response body: Array<unknown>)

## Books Controller

- `getBookFileByBook`: GET `/api/books/{book}/file` -> `GetBookFileByBookResponse` (request: `GetBookFileByBookData`; path: book; response body: string)

## Admin User Controller

- `listUsers`: GET `/api/admin/users` -> `ListUsersResponse` (request: `ListUsersData`; query: pageIndex, pageSize; response body: UserListingPage)
