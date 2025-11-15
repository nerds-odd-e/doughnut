// Manually maintained DoughnutApi class
// This provides instance-based access to openapi-ts generated static services
// This file should NOT be in the generated folder to avoid being overwritten
// Now uses generated functions directly instead of httpRequest.request()

import type { CancelablePromise } from "@generated/backend/core/CancelablePromise"
import type * as Types from "@generated/backend/types.gen"
import * as Services from "@generated/backend/services.gen"

// Base class for instance-based service wrappers
// Service instances now call generated functions directly
abstract class ServiceInstance {
  // No longer needs httpRequest - uses generated functions
  // Error handling is done via interceptors in ManagedApi
}

// Instance-based wrapper for RestNoteControllerService
// Now uses generated functions directly
class RestNoteControllerServiceInstance extends ServiceInstance {
  updateWikidataId(
    note: number,
    requestBody: Types.WikidataAssociationCreation
  ): CancelablePromise<Types.UpdateWikidataIdResponse> {
    return Services.updateWikidataId({ note, requestBody })
  }

  updateRecallSetting(
    note: number,
    requestBody: Types.RecallSetting
  ): CancelablePromise<Types.UpdateRecallSettingResponse> {
    return Services.updateRecallSetting({ note, requestBody })
  }

  deleteNote(note: number): CancelablePromise<Types.DeleteNoteResponse> {
    return Services.deleteNote({ note })
  }

  moveAfter(
    note: number,
    targetNote: number,
    dropMode: "after" | "asFirstChild"
  ): CancelablePromise<Types.MoveAfterResponse> {
    return Services.moveAfter({
      note,
      targetNote,
      asFirstChild: dropMode === "asFirstChild" ? "true" : "false",
    })
  }

  show(note: number): CancelablePromise<Types.ShowResponse> {
    return Services.show({ note })
  }

  updateNoteAccessories(
    note: number,
    formData: Types.NoteAccessoriesDTO
  ): CancelablePromise<Types.UpdateNoteAccessoriesResponse> {
    return Services.updateNoteAccessories({ note, formData })
  }

  undoDeleteNote(
    note: number
  ): CancelablePromise<Types.UndoDeleteNoteResponse> {
    return Services.undoDeleteNote({ note })
  }

  getNoteInfo(note: number): CancelablePromise<Types.GetNoteInfoResponse> {
    return Services.getNoteInfo({ note })
  }

  getGraph(
    note: number,
    tokenLimit?: number
  ): CancelablePromise<Types.GetGraphResponse> {
    return Services.getGraph({ note, tokenLimit: tokenLimit ?? 0 })
  }

  getDescendants(
    note: number
  ): CancelablePromise<Types.GetDescendantsResponse> {
    return Services.getDescendants({ note })
  }

  showNoteAccessory(
    note: number
  ): CancelablePromise<Types.ShowNoteAccessoryResponse> {
    return Services.showNoteAccessory({ note })
  }

  getRecentNotes(): CancelablePromise<Types.GetRecentNotesResponse> {
    return Services.getRecentNotes()
  }
}

class RestTextContentControllerServiceInstance extends ServiceInstance {
  updateNoteTitle(
    note: number,
    data: { newTitle: string }
  ): CancelablePromise<Types.UpdateNoteTitleResponse> {
    return Services.updateNoteTitle({ note, requestBody: data })
  }

  updateNoteDetails(
    note: number,
    data: { details: string }
  ): CancelablePromise<Types.UpdateNoteDetailsResponse> {
    return Services.updateNoteDetails({ note, requestBody: data })
  }
}

class RestNoteCreationControllerServiceInstance extends ServiceInstance {
  createNote(
    parentNote: number,
    requestBody: Types.NoteCreationDTO
  ): CancelablePromise<Types.CreateNoteResponse> {
    return Services.createNote({ parentNote, requestBody })
  }

  createNoteAfter(
    referenceNote: number,
    requestBody: Types.NoteCreationDTO
  ): CancelablePromise<Types.CreateNoteAfterResponse> {
    return Services.createNoteAfter({ referenceNote, requestBody })
  }
}

class RestLinkControllerServiceInstance extends ServiceInstance {
  linkNoteFinalize(
    sourceId: number,
    targetId: number,
    data: Types.LinkCreation
  ): CancelablePromise<Types.LinkNoteFinalizeResponse> {
    return Services.linkNoteFinalize({
      sourceNote: sourceId,
      targetNote: targetId,
      requestBody: data,
    })
  }

  updateLink(
    linkId: number,
    data: Types.LinkCreation
  ): CancelablePromise<Types.UpdateLinkResponse> {
    return Services.updateLink({ link: linkId, requestBody: data })
  }

  moveNote(
    sourceId: number,
    targetId: number,
    data: Types.NoteMoveDTO
  ): CancelablePromise<Types.MoveNoteResponse> {
    return Services.moveNote({
      sourceNote: sourceId,
      targetNote: targetId,
      requestBody: data,
    })
  }
}

// Instance-based wrappers for all service classes
// Methods are converted from static methods that take data objects to instance methods with individual parameters

class RestNotebookCertificateApprovalControllerServiceInstance extends ServiceInstance {
  getAllPendingRequest(): CancelablePromise<Types.GetAllPendingRequestResponse> {
    return Services.getAllPendingRequest()
  }

  approve(
    notebookCertificateApproval: number
  ): CancelablePromise<Types.ApproveResponse> {
    return Services.approve({ notebookCertificateApproval })
  }

  getApprovalForNotebook(
    notebook: number
  ): CancelablePromise<Types.GetApprovalForNotebookResponse> {
    return Services.getApprovalForNotebook({ notebook })
  }

  requestApprovalForNotebook(
    notebook: number
  ): CancelablePromise<Types.RequestApprovalForNotebookResponse> {
    return Services.requestApprovalForNotebook({ notebook })
  }
}

class RestFineTuningDataControllerServiceInstance extends ServiceInstance {
  uploadAndTriggerFineTuning(): CancelablePromise<Types.UploadAndTriggerFineTuningResponse> {
    return Services.uploadAndTriggerFineTuning()
  }

  getAllSuggestedQuestions(): CancelablePromise<Types.GetAllSuggestedQuestionsResponse> {
    return Services.getAllSuggestedQuestions()
  }

  updateSuggestedQuestionForFineTuning(
    suggestedQuestion: number,
    requestBody: Types.UpdateSuggestedQuestionForFineTuningData["requestBody"]
  ): CancelablePromise<Types.UpdateSuggestedQuestionForFineTuningResponse> {
    return Services.updateSuggestedQuestionForFineTuning({
      suggestedQuestion,
      requestBody,
    })
  }

  duplicate(
    suggestedQuestion: number
  ): CancelablePromise<Types.DuplicateResponse> {
    return Services.duplicate({ suggestedQuestion })
  }

  delete(suggestedQuestion: number): CancelablePromise<Types.DeleteResponse> {
    return Services.delete_({ suggestedQuestion })
  }
}

class RestBazaarControllerServiceInstance extends ServiceInstance {
  bazaar(): CancelablePromise<Types.BazaarResponse> {
    return Services.bazaar()
  }

  removeFromBazaar(
    notebook: number
  ): CancelablePromise<Types.RemoveFromBazaarResponse> {
    return Services.removeFromBazaar({ bazaarNotebook: notebook })
  }
}

class RestAiControllerServiceInstance extends ServiceInstance {
  getAvailableGptModels(): CancelablePromise<Types.GetAvailableGptModelsResponse> {
    return Services.getAvailableGptModels()
  }

  suggestTitle(note: number): CancelablePromise<Types.SuggestTitleResponse> {
    return Services.suggestTitle({ note })
  }

  generateImage(
    requestBody: Types.GenerateImageData["requestBody"]
  ): CancelablePromise<Types.GenerateImageResponse> {
    return Services.generateImage({ requestBody })
  }
}

class RestGlobalSettingsControllerServiceInstance extends ServiceInstance {
  getCurrentModelVersions(): CancelablePromise<Types.GetCurrentModelVersionsResponse> {
    return Services.getCurrentModelVersions()
  }

  setCurrentModelVersions(
    requestBody: Types.SetCurrentModelVersionsData["requestBody"]
  ): CancelablePromise<Types.SetCurrentModelVersionsResponse> {
    return Services.setCurrentModelVersions({ requestBody })
  }
}

class TestabilityRestControllerServiceInstance extends ServiceInstance {
  randomizer(
    data: Types.Randomization
  ): CancelablePromise<Types.RandomizerResponse> {
    return Services.randomizer({ requestBody: data })
  }

  enableFeatureToggle(
    requestBody: Types.EnableFeatureToggleData["requestBody"]
  ): CancelablePromise<Types.EnableFeatureToggleResponse> {
    return Services.enableFeatureToggle({ requestBody })
  }

  getFeatureToggle(): CancelablePromise<Types.GetFeatureToggleResponse> {
    return Services.getFeatureToggle()
  }

  resetDbAndTestabilitySettings(): CancelablePromise<Types.ResetDbAndTestabilitySettingsResponse> {
    return Services.resetDbAndTestabilitySettings()
  }

  injectNotes(
    requestBody: Types.NotesTestData
  ): CancelablePromise<Types.InjectNotesResponse> {
    return Services.injectNotes({ requestBody })
  }

  injectPredefinedQuestion(
    requestBody: Types.PredefinedQuestionsTestData
  ): CancelablePromise<Types.InjectPredefinedQuestionResponse> {
    return Services.injectPredefinedQuestion({ requestBody })
  }

  linkNotes(
    requestBody: Types.LinkNotesData["requestBody"]
  ): CancelablePromise<Types.LinkNotesResponse> {
    return Services.linkNotes({ requestBody })
  }

  injectSuggestedQuestion(
    requestBody: Types.SuggestedQuestionsData
  ): CancelablePromise<Types.InjectSuggestedQuestionResponse> {
    return Services.injectSuggestedQuestion({ requestBody })
  }

  timeTravel(
    requestBody: Types.TimeTravel
  ): CancelablePromise<Types.TimeTravelResponse> {
    return Services.timeTravel({ requestBody })
  }

  timeTravelRelativeToNow(
    requestBody: Types.TimeTravelRelativeToNow
  ): CancelablePromise<Types.TimeTravelRelativeToNowResponse> {
    return Services.timeTravelRelativeToNow({ requestBody })
  }

  triggerException(): CancelablePromise<Types.TriggerExceptionResponse> {
    return Services.triggerException()
  }

  shareToBazaar(
    requestBody: Types.ShareToBazaarData["requestBody"]
  ): CancelablePromise<Types.ShareToBazaarResponse> {
    return Services.shareToBazaar({ requestBody })
  }

  injectCircle(
    requestBody: Types.InjectCircleData["requestBody"]
  ): CancelablePromise<Types.InjectCircleResponse> {
    return Services.injectCircle({ requestBody })
  }

  updateCurrentUser(
    requestBody: Types.UpdateCurrentUserData["requestBody"]
  ): CancelablePromise<Types.UpdateCurrentUserResponse> {
    return Services.updateCurrentUser({ requestBody })
  }

  replaceServiceUrl(
    requestBody: Types.ReplaceServiceUrlData["requestBody"]
  ): CancelablePromise<Types.ReplaceServiceUrlResponse> {
    return Services.replaceServiceUrl({ requestBody })
  }
}

class RestAiAudioControllerServiceInstance extends ServiceInstance {
  audioToText(
    formData?: Types.AudioToTextData["formData"]
  ): CancelablePromise<Types.AudioToTextResponse> {
    return Services.audioToText({ formData })
  }
}

class RestUserControllerServiceInstance extends ServiceInstance {
  getUserProfile(): CancelablePromise<Types.GetUserProfileResponse> {
    return Services.getUserProfile()
  }

  createUser(
    requestBody: Types.CreateUserData["requestBody"]
  ): CancelablePromise<Types.CreateUserResponse> {
    return Services.createUser({ requestBody })
  }

  generateToken(
    requestBody: Types.GenerateTokenData["requestBody"]
  ): CancelablePromise<Types.GenerateTokenResponse> {
    return Services.generateToken({ requestBody })
  }

  getTokens(): CancelablePromise<Types.GetTokensResponse> {
    return Services.getTokens()
  }

  deleteToken(tokenId: number): CancelablePromise<Types.DeleteTokenResponse> {
    return Services.deleteToken({ tokenId })
  }

  updateUser(
    user: number,
    requestBody: Types.UpdateUserData["requestBody"]
  ): CancelablePromise<Types.UpdateUserResponse> {
    return Services.updateUser({ user, requestBody })
  }
}

class RestSearchControllerServiceInstance extends ServiceInstance {
  semanticSearch(
    requestBody: Types.SemanticSearchData["requestBody"]
  ): CancelablePromise<Types.SemanticSearchResponse> {
    return Services.semanticSearch({ requestBody })
  }

  semanticSearchWithin(
    note: number,
    requestBody: Types.SemanticSearchWithinData["requestBody"]
  ): CancelablePromise<Types.SemanticSearchWithinResponse> {
    return Services.semanticSearchWithin({ note, requestBody })
  }

  searchForLinkTarget(
    requestBody: Types.SearchForLinkTargetData["requestBody"]
  ): CancelablePromise<Types.SearchForLinkTargetResponse> {
    return Services.searchForLinkTarget({ requestBody })
  }

  searchForLinkTargetWithin(
    note: number,
    requestBody: Types.SearchForLinkTargetWithinData["requestBody"]
  ): CancelablePromise<Types.SearchForLinkTargetWithinResponse> {
    return Services.searchForLinkTargetWithin({ note, requestBody })
  }
}

class RestMemoryTrackerControllerServiceInstance extends ServiceInstance {
  selfEvaluate(
    memoryTracker: number,
    requestBody: Types.SelfEvaluateData["requestBody"]
  ): CancelablePromise<Types.SelfEvaluateResponse> {
    return Services.selfEvaluate({ memoryTracker, requestBody })
  }

  removeFromRepeating(
    memoryTracker: number
  ): CancelablePromise<Types.RemoveFromRepeatingResponse> {
    return Services.removeFromRepeating({ memoryTracker })
  }

  answerSpelling(
    memoryTracker: number,
    requestBody: Types.AnswerSpellingData["requestBody"]
  ): CancelablePromise<Types.AnswerSpellingResponse> {
    return Services.answerSpelling({ memoryTracker, requestBody })
  }

  markAsRepeated(
    memoryTracker: number,
    successful: boolean
  ): CancelablePromise<Types.MarkAsRepeatedResponse> {
    return Services.markAsRepeated({ memoryTracker, successful })
  }

  show1(memoryTracker: number): CancelablePromise<Types.Show1Response> {
    return Services.show1({ memoryTracker })
  }

  getSpellingQuestion(
    memoryTracker: number
  ): CancelablePromise<Types.GetSpellingQuestionResponse> {
    return Services.getSpellingQuestion({ memoryTracker })
  }

  getRecentlyReviewed(): CancelablePromise<Types.GetRecentlyReviewedResponse> {
    return Services.getRecentlyReviewed()
  }

  getRecentMemoryTrackers(): CancelablePromise<Types.GetRecentMemoryTrackersResponse> {
    return Services.getRecentMemoryTrackers()
  }
}

class RestRecallPromptControllerServiceInstance extends ServiceInstance {
  regenerate(
    recallPrompt: number,
    requestBody: Types.RegenerateData["requestBody"]
  ): CancelablePromise<Types.RegenerateResponse> {
    return Services.regenerate({ recallPrompt, requestBody })
  }

  contest(recallPrompt: number): CancelablePromise<Types.ContestResponse> {
    return Services.contest({ recallPrompt })
  }

  answerQuiz(
    recallPrompt: number,
    requestBody: Types.AnswerQuizData["requestBody"]
  ): CancelablePromise<Types.AnswerQuizResponse> {
    return Services.answerQuiz({ recallPrompt, requestBody })
  }

  showQuestion(
    recallPrompt: number
  ): CancelablePromise<Types.ShowQuestionResponse> {
    return Services.showQuestion({ recallPrompt })
  }

  askAQuestion(
    memoryTracker: number
  ): CancelablePromise<Types.AskAquestionResponse> {
    return Services.askAquestion({ memoryTracker })
  }
}

class RestRecallsControllerServiceInstance extends ServiceInstance {
  recalling(
    timezone?: string,
    dueindays?: number
  ): CancelablePromise<Types.RecallingResponse> {
    return Services.recalling({ timezone: timezone ?? "", dueindays })
  }

  overview(timezone?: string): CancelablePromise<Types.OverviewResponse> {
    return Services.overview({ timezone: timezone ?? "" })
  }
}

class RestPredefinedQuestionControllerServiceInstance extends ServiceInstance {
  suggestQuestionForFineTuning(
    predefinedQuestion: number,
    requestBody: Types.SuggestQuestionForFineTuningData["requestBody"]
  ): CancelablePromise<Types.SuggestQuestionForFineTuningResponse> {
    return Services.suggestQuestionForFineTuning({
      predefinedQuestion,
      requestBody,
    })
  }

  addQuestionManually(
    note: number,
    requestBody: Types.AddQuestionManuallyData["requestBody"]
  ): CancelablePromise<Types.AddQuestionManuallyResponse> {
    return Services.addQuestionManually({ note, requestBody })
  }

  refineQuestion(
    note: number,
    requestBody: Types.RefineQuestionData["requestBody"]
  ): CancelablePromise<Types.RefineQuestionResponse> {
    return Services.refineQuestion({ note, requestBody })
  }

  getAllQuestionByNote(
    note: number
  ): CancelablePromise<Types.GetAllQuestionByNoteResponse> {
    return Services.getAllQuestionByNote({ note })
  }

  generateQuestionWithoutSave(
    note: number
  ): CancelablePromise<Types.GenerateQuestionWithoutSaveResponse> {
    return Services.generateQuestionWithoutSave({ note })
  }

  toggleApproval(
    predefinedQuestion: number
  ): CancelablePromise<Types.ToggleApprovalResponse> {
    return Services.toggleApproval({ predefinedQuestion })
  }
}

class RestWikidataControllerServiceInstance extends ServiceInstance {
  searchWikidata(
    search: string
  ): CancelablePromise<Types.SearchWikidataResponse> {
    return Services.searchWikidata({ search })
  }

  fetchWikidataEntityDataById(
    wikidataId: string
  ): CancelablePromise<Types.FetchWikidataEntityDataByIdResponse> {
    return Services.fetchWikidataEntityDataById({ wikidataId })
  }
}

class RestConversationMessageControllerServiceInstance extends ServiceInstance {
  getAiReply(
    conversationId: number
  ): CancelablePromise<Types.GetAiReplyResponse> {
    return Services.getAiReply({ conversationId })
  }

  getConversationsAboutNote(
    note: number
  ): CancelablePromise<Types.GetConversationsAboutNoteResponse> {
    return Services.getConversationsAboutNote({ note })
  }

  getConversationMessages(
    conversationId: number
  ): CancelablePromise<Types.GetConversationMessagesResponse> {
    return Services.getConversationMessages({ conversationId })
  }

  replyToConversation(
    conversationId: number,
    requestBody: Types.ReplyToConversationData["requestBody"]
  ): CancelablePromise<Types.ReplyToConversationResponse> {
    return Services.replyToConversation({ conversationId, requestBody })
  }

  startConversationAboutNote(
    note: number,
    requestBody?: Types.StartConversationAboutNoteData["requestBody"]
  ): CancelablePromise<Types.StartConversationAboutNoteResponse> {
    return Services.startConversationAboutNote({
      note,
      requestBody: requestBody ?? "",
    })
  }

  startConversationAboutRecallPrompt(
    recallPrompt: number
  ): CancelablePromise<Types.StartConversationAboutRecallPromptResponse> {
    return Services.startConversationAboutRecallPrompt({ recallPrompt })
  }

  startConversationAboutAssessmentQuestion(
    assessmentQuestion: number,
    requestBody: Types.StartConversationAboutAssessmentQuestionData["requestBody"]
  ): CancelablePromise<Types.StartConversationAboutAssessmentQuestionResponse> {
    return Services.startConversationAboutAssessmentQuestion({
      assessmentQuestion,
      requestBody,
    })
  }

  markConversationAsRead(
    conversationId: number
  ): CancelablePromise<Types.MarkConversationAsReadResponse> {
    return Services.markConversationAsRead({ conversationId })
  }

  getConversation(
    conversationId: number
  ): CancelablePromise<Types.GetConversationResponse> {
    return Services.getConversation({ conversationId })
  }

  getConversationsOfCurrentUser(): CancelablePromise<Types.GetConversationsOfCurrentUserResponse> {
    return Services.getConversationsOfCurrentUser()
  }

  getUnreadConversations(): CancelablePromise<Types.GetUnreadConversationsResponse> {
    return Services.getUnreadConversations()
  }
}

class RestAssessmentControllerServiceInstance extends ServiceInstance {
  answerQuestion(
    assessmentQuestionInstance: number,
    requestBody: Types.AnswerQuestionData["requestBody"]
  ): CancelablePromise<Types.AnswerQuestionResponse> {
    return Services.answerQuestion({ assessmentQuestionInstance, requestBody })
  }

  submitAssessmentResult(
    assessmentAttempt: number
  ): CancelablePromise<Types.SubmitAssessmentResultResponse> {
    return Services.submitAssessmentResult({ assessmentAttempt })
  }

  generateAssessmentQuestions(
    notebook: number
  ): CancelablePromise<Types.GenerateAssessmentQuestionsResponse> {
    return Services.generateAssessmentQuestions({ notebook })
  }

  getMyAssessments(): CancelablePromise<Types.GetMyAssessmentsResponse> {
    return Services.getMyAssessments()
  }
}

class RestSubscriptionControllerServiceInstance extends ServiceInstance {
  createSubscription(
    notebook: number,
    requestBody: Types.CreateSubscriptionData["requestBody"]
  ): CancelablePromise<Types.CreateSubscriptionResponse> {
    return Services.createSubscription({ notebook, requestBody })
  }

  update(
    subscription: number,
    requestBody: Types.UpdateData["requestBody"]
  ): CancelablePromise<Types.UpdateResponse> {
    return Services.update({ subscription, requestBody })
  }

  delete(
    _subscription: number
  ): CancelablePromise<Types.DestroySubscriptionResponse> {
    // Note: destroySubscription doesn't take parameters in generated code
    // but the instance method takes subscription for consistency
    // The subscription parameter is ignored for now
    return Services.destroySubscription()
  }
}

class RestCertificateControllerServiceInstance extends ServiceInstance {
  getCertificate(
    notebook: number
  ): CancelablePromise<Types.GetCertificateResponse> {
    return Services.getCertificate({ notebook })
  }

  claimCertificate(
    notebook: number
  ): CancelablePromise<Types.ClaimCertificateResponse> {
    return Services.claimCertificate({ notebook })
  }
}

class RestNotebookControllerServiceInstance extends ServiceInstance {
  updateAiAssistant(
    notebook: number,
    requestBody: Types.UpdateAiAssistantData["requestBody"]
  ): CancelablePromise<Types.UpdateAiAssistantResponse> {
    return Services.updateAiAssistant({ notebook, requestBody })
  }

  downloadNotebookDump(
    notebook: number
  ): CancelablePromise<Types.DownloadNotebookDumpResponse> {
    return Services.downloadNotebookDump({ notebook })
  }

  getAiAssistant(
    notebook: number
  ): CancelablePromise<Types.GetAiAssistantResponse> {
    return Services.getAiAssistant({ notebook })
  }

  shareNotebook(
    notebook: number
  ): CancelablePromise<Types.ShareNotebookResponse> {
    return Services.shareNotebook({ notebook })
  }

  update1(
    notebook: number,
    requestBody: Types.Update1Data["requestBody"]
  ): CancelablePromise<Types.Update1Response> {
    return Services.update1({ notebook, requestBody })
  }

  importObsidian(
    notebook: number,
    formData: Types.ImportObsidianData["formData"]
  ): CancelablePromise<Types.ImportObsidianResponse> {
    return Services.importObsidian({ notebookId: notebook, formData })
  }

  resetNotebookIndex(
    notebook: number
  ): CancelablePromise<Types.ResetNotebookIndexResponse> {
    return Services.resetNotebookIndex({ notebook })
  }

  updateNotebookIndex(
    notebook: number
  ): CancelablePromise<Types.UpdateNotebookIndexResponse> {
    return Services.updateNotebookIndex({ notebook })
  }

  moveToCircle(
    notebook: number,
    circle: number
  ): CancelablePromise<Types.MoveToCircleResponse> {
    return Services.moveToCircle({ notebook, circle })
  }

  createNotebook(
    requestBody: Types.CreateNotebookData["requestBody"]
  ): CancelablePromise<Types.CreateNotebookResponse> {
    return Services.createNotebook({ requestBody })
  }

  getNotes(notebook: number): CancelablePromise<Types.GetNotesResponse> {
    return Services.getNotes({ notebook })
  }

  myNotebooks(): CancelablePromise<Types.MyNotebooksResponse> {
    return Services.myNotebooks()
  }
}

class RestCircleControllerServiceInstance extends ServiceInstance {
  createCircle(
    requestBody: Types.CreateCircleData["requestBody"]
  ): CancelablePromise<Types.CreateCircleResponse> {
    return Services.createCircle({ requestBody })
  }

  index(): CancelablePromise<Types.IndexResponse> {
    return Services.index()
  }

  createNotebookInCircle(
    circle: number,
    requestBody: Types.CreateNotebookInCircleData["requestBody"]
  ): CancelablePromise<Types.CreateNotebookInCircleResponse> {
    return Services.createNotebookInCircle({ circle, requestBody })
  }

  showCircle(circle: number): CancelablePromise<Types.ShowCircleResponse> {
    return Services.showCircle({ circle })
  }

  joinCircle(
    requestBody: Types.JoinCircleData["requestBody"]
  ): CancelablePromise<Types.JoinCircleResponse> {
    return Services.joinCircle({ requestBody })
  }
}

class RestCurrentUserInfoControllerServiceInstance extends ServiceInstance {
  currentUserInfo(): CancelablePromise<Types.CurrentUserInfoResponse> {
    return Services.currentUserInfo()
  }
}

class AssimilationControllerServiceInstance extends ServiceInstance {
  assimilate(
    requestBody: Types.AssimilateData["requestBody"]
  ): CancelablePromise<Types.AssimilateResponse> {
    return Services.assimilate({ requestBody })
  }

  getAssimilationCount(
    timezone?: string
  ): CancelablePromise<Types.GetAssimilationCountResponse> {
    return Services.getAssimilationCount({ timezone: timezone ?? "" })
  }

  assimilating(
    timezone?: string
  ): CancelablePromise<Types.AssimilatingResponse> {
    return Services.assimilating({ timezone: timezone ?? "" })
  }
}

class RestFailureReportControllerServiceInstance extends ServiceInstance {
  failureReports(): CancelablePromise<Types.FailureReportsResponse> {
    return Services.failureReports()
  }

  show2(failureReport: number): CancelablePromise<Types.Show2Response> {
    return Services.show2({ failureReport })
  }

  deleteFailureReports(
    requestBody: Types.DeleteFailureReportsData["requestBody"]
  ): CancelablePromise<Types.DeleteFailureReportsResponse> {
    return Services.deleteFailureReports({ requestBody })
  }
}

class RestHealthCheckControllerServiceInstance extends ServiceInstance {
  ping(): CancelablePromise<Types.PingResponse> {
    return Services.ping()
  }
}

class McpNoteCreationControllerServiceInstance extends ServiceInstance {
  createNote1(
    requestBody: Types.McpNoteAddDTO
  ): CancelablePromise<Types.CreateNote1Response> {
    return Services.createNote1({ requestBody })
  }
}

export class DoughnutApi {
  public restNoteController: RestNoteControllerServiceInstance
  public restTextContentController: RestTextContentControllerServiceInstance
  public restNoteCreationController: RestNoteCreationControllerServiceInstance
  public restLinkController: RestLinkControllerServiceInstance
  public restUserController: RestUserControllerServiceInstance
  public restSearchController: RestSearchControllerServiceInstance
  public restNotebookController: RestNotebookControllerServiceInstance
  public restCircleController: RestCircleControllerServiceInstance
  public restConversationMessageController: RestConversationMessageControllerServiceInstance
  public restMemoryTrackerController: RestMemoryTrackerControllerServiceInstance
  public restRecallPromptController: RestRecallPromptControllerServiceInstance
  public restRecallsController: RestRecallsControllerServiceInstance
  public restPredefinedQuestionController: RestPredefinedQuestionControllerServiceInstance
  public restWikidataController: RestWikidataControllerServiceInstance
  public restAiController: RestAiControllerServiceInstance
  public restAiAudioController: RestAiAudioControllerServiceInstance
  public restAssessmentController: RestAssessmentControllerServiceInstance
  public restBazaarController: RestBazaarControllerServiceInstance
  public restCertificateController: RestCertificateControllerServiceInstance
  public restFailureReportController: RestFailureReportControllerServiceInstance
  public restFineTuningDataController: RestFineTuningDataControllerServiceInstance
  public restGlobalSettingsController: RestGlobalSettingsControllerServiceInstance
  public restHealthCheckController: RestHealthCheckControllerServiceInstance
  public restNotebookCertificateApprovalController: RestNotebookCertificateApprovalControllerServiceInstance
  public restSubscriptionController: RestSubscriptionControllerServiceInstance
  public restCurrentUserInfoController: RestCurrentUserInfoControllerServiceInstance
  public testabilityRestController: TestabilityRestControllerServiceInstance
  public restAssimilationController: AssimilationControllerServiceInstance
  public mcpNoteCreationController: McpNoteCreationControllerServiceInstance

  constructor() {
    // Service instances no longer need httpRequest - they use generated functions directly
    this.restNoteController = new RestNoteControllerServiceInstance()
    this.restTextContentController =
      new RestTextContentControllerServiceInstance()
    this.restNoteCreationController =
      new RestNoteCreationControllerServiceInstance()
    this.restLinkController = new RestLinkControllerServiceInstance()
    this.restUserController = new RestUserControllerServiceInstance()
    this.restSearchController = new RestSearchControllerServiceInstance()
    this.restNotebookController = new RestNotebookControllerServiceInstance()
    this.restCircleController = new RestCircleControllerServiceInstance()
    this.restConversationMessageController =
      new RestConversationMessageControllerServiceInstance()
    this.restMemoryTrackerController =
      new RestMemoryTrackerControllerServiceInstance()
    this.restRecallPromptController =
      new RestRecallPromptControllerServiceInstance()
    this.restRecallsController = new RestRecallsControllerServiceInstance()
    this.restPredefinedQuestionController =
      new RestPredefinedQuestionControllerServiceInstance()
    this.restWikidataController = new RestWikidataControllerServiceInstance()
    this.restAiController = new RestAiControllerServiceInstance()
    this.restAiAudioController = new RestAiAudioControllerServiceInstance()
    this.restAssessmentController =
      new RestAssessmentControllerServiceInstance()
    this.restBazaarController = new RestBazaarControllerServiceInstance()
    this.restCertificateController =
      new RestCertificateControllerServiceInstance()
    this.restFailureReportController =
      new RestFailureReportControllerServiceInstance()
    this.restFineTuningDataController =
      new RestFineTuningDataControllerServiceInstance()
    this.restGlobalSettingsController =
      new RestGlobalSettingsControllerServiceInstance()
    this.restHealthCheckController =
      new RestHealthCheckControllerServiceInstance()
    this.restNotebookCertificateApprovalController =
      new RestNotebookCertificateApprovalControllerServiceInstance()
    this.restSubscriptionController =
      new RestSubscriptionControllerServiceInstance()
    this.restCurrentUserInfoController =
      new RestCurrentUserInfoControllerServiceInstance()
    this.testabilityRestController =
      new TestabilityRestControllerServiceInstance()
    this.restAssimilationController =
      new AssimilationControllerServiceInstance()
    this.mcpNoteCreationController =
      new McpNoteCreationControllerServiceInstance()
  }
}
