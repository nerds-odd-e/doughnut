// Manually maintained DoughnutApi class
// This provides instance-based access to openapi-ts generated static services
// This file should NOT be in the generated folder to avoid being overwritten

import type { BaseHttpRequest } from "./BaseHttpRequest"
import type { OpenAPIConfig } from "@generated/backend/core/OpenAPI"
import { Interceptors } from "@generated/backend/core/OpenAPI"
import { FetchHttpRequest } from "./FetchHttpRequest"
import type { CancelablePromise } from "@generated/backend/core/CancelablePromise"
import type * as Types from "@generated/backend/types.gen"

// Helper type for HttpRequest constructor
type HttpRequestConstructor = new (config: OpenAPIConfig) => BaseHttpRequest

// Base class for instance-based service wrappers
abstract class ServiceInstance {
  constructor(protected readonly httpRequest: BaseHttpRequest) {}
}

// Instance-based wrapper for RestNoteControllerService
class RestNoteControllerServiceInstance extends ServiceInstance {
  updateWikidataId(
    note: number,
    requestBody: Types.WikidataAssociationCreation
  ): CancelablePromise<Types.UpdateWikidataIdResponse> {
    return this.httpRequest.request<Types.UpdateWikidataIdResponse>({
      method: "POST",
      url: "/api/notes/{note}/updateWikidataId",
      path: { note },
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  updateRecallSetting(
    note: number,
    requestBody: Types.RecallSetting
  ): CancelablePromise<Types.UpdateRecallSettingResponse> {
    return this.httpRequest.request<Types.UpdateRecallSettingResponse>({
      method: "POST",
      url: "/api/notes/{note}/review-setting",
      path: { note },
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  deleteNote(note: number): CancelablePromise<Types.DeleteNoteResponse> {
    return this.httpRequest.request<Types.DeleteNoteResponse>({
      method: "POST",
      url: "/api/notes/{note}/delete",
      path: { note },
      errors: { 500: "Internal Server Error" },
    })
  }

  moveAfter(
    note: number,
    targetNote: number,
    dropMode: "after" | "asFirstChild"
  ): CancelablePromise<Types.MoveAfterResponse> {
    return this.httpRequest.request<Types.MoveAfterResponse>({
      method: "POST",
      url: "/api/notes/move_after/{note}/{targetNote}/{asFirstChild}",
      path: {
        note,
        targetNote,
        asFirstChild: dropMode === "asFirstChild",
      },
      errors: { 500: "Internal Server Error" },
    })
  }

  show(note: number): CancelablePromise<Types.ShowResponse> {
    return this.httpRequest.request<Types.ShowResponse>({
      method: "GET",
      url: "/api/notes/{note}",
      path: { note },
      errors: { 500: "Internal Server Error" },
    })
  }

  updateNoteAccessories(
    note: number,
    formData: Types.NoteAccessoriesDTO
  ): CancelablePromise<Types.UpdateNoteAccessoriesResponse> {
    return this.httpRequest.request<Types.UpdateNoteAccessoriesResponse>({
      method: "PATCH",
      url: "/api/notes/{note}",
      path: { note },
      formData: formData as unknown as Record<string, unknown>,
      mediaType: "multipart/form-data",
      errors: { 500: "Internal Server Error" },
    })
  }

  undoDeleteNote(
    note: number
  ): CancelablePromise<Types.UndoDeleteNoteResponse> {
    return this.httpRequest.request<Types.UndoDeleteNoteResponse>({
      method: "PATCH",
      url: "/api/notes/{note}/undo-delete",
      path: { note },
      errors: { 500: "Internal Server Error" },
    })
  }

  getNoteInfo(note: number): CancelablePromise<Types.GetNoteInfoResponse> {
    return this.httpRequest.request<Types.GetNoteInfoResponse>({
      method: "GET",
      url: "/api/notes/{note}/note-info",
      path: { note },
      errors: { 500: "Internal Server Error" },
    })
  }

  getGraph(
    note: number,
    tokenLimit?: number
  ): CancelablePromise<Types.GetGraphResponse> {
    return this.httpRequest.request<Types.GetGraphResponse>({
      method: "GET",
      url: "/api/notes/{note}/graph",
      path: { note },
      query: tokenLimit ? { tokenLimit } : undefined,
      errors: { 500: "Internal Server Error" },
    })
  }

  getDescendants(
    note: number
  ): CancelablePromise<Types.GetDescendantsResponse> {
    return this.httpRequest.request<Types.GetDescendantsResponse>({
      method: "GET",
      url: "/api/notes/{note}/descendants",
      path: { note },
      errors: { 500: "Internal Server Error" },
    })
  }

  showNoteAccessory(
    note: number
  ): CancelablePromise<Types.ShowNoteAccessoryResponse> {
    return this.httpRequest.request<Types.ShowNoteAccessoryResponse>({
      method: "GET",
      url: "/api/notes/{note}/accessory",
      path: { note },
      errors: { 500: "Internal Server Error" },
    })
  }

  getRecentNotes(): CancelablePromise<Types.GetRecentNotesResponse> {
    return this.httpRequest.request<Types.GetRecentNotesResponse>({
      method: "GET",
      url: "/api/notes/recent",
      errors: { 500: "Internal Server Error" },
    })
  }
}

// Placeholder for other service instances - will be expanded
// For now, creating minimal stubs to fix build errors
class RestTextContentControllerServiceInstance extends ServiceInstance {
  updateNoteTitle(
    note: number,
    data: { newTitle: string }
  ): CancelablePromise<Types.UpdateNoteTitleResponse> {
    return this.httpRequest.request<Types.UpdateNoteTitleResponse>({
      method: "PATCH",
      url: "/api/text_content/{note}/title",
      path: { note },
      body: data,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  updateNoteDetails(
    note: number,
    data: { details: string }
  ): CancelablePromise<Types.UpdateNoteDetailsResponse> {
    return this.httpRequest.request<Types.UpdateNoteDetailsResponse>({
      method: "PATCH",
      url: "/api/text_content/{note}/details",
      path: { note },
      body: data,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestNoteCreationControllerServiceInstance extends ServiceInstance {
  createNote(
    parentNote: number,
    requestBody: Types.NoteCreationDTO
  ): CancelablePromise<Types.CreateNoteResponse> {
    return this.httpRequest.request<Types.CreateNoteResponse>({
      method: "POST",
      url: "/api/notes/{parentNote}/create",
      path: { parentNote },
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  createNoteAfter(
    referenceNote: number,
    requestBody: Types.NoteCreationDTO
  ): CancelablePromise<Types.CreateNoteAfterResponse> {
    return this.httpRequest.request<Types.CreateNoteAfterResponse>({
      method: "POST",
      url: "/api/notes/{referenceNote}/create-after",
      path: { referenceNote },
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestLinkControllerServiceInstance extends ServiceInstance {
  linkNoteFinalize(
    sourceId: number,
    targetId: number,
    data: Types.LinkCreation
  ): CancelablePromise<Types.LinkNoteFinalizeResponse> {
    return this.httpRequest.request<Types.LinkNoteFinalizeResponse>({
      method: "POST",
      url: "/api/links/{sourceId}/finalize/{targetId}",
      path: { sourceId, targetId },
      body: data,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  updateLink(
    linkId: number,
    data: Types.LinkCreation
  ): CancelablePromise<Types.UpdateLinkResponse> {
    return this.httpRequest.request<Types.UpdateLinkResponse>({
      method: "POST",
      url: "/api/links/{linkId}",
      path: { linkId },
      body: data,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  moveNote(
    sourceId: number,
    targetId: number,
    data: Types.NoteMoveDTO
  ): CancelablePromise<Types.MoveNoteResponse> {
    return this.httpRequest.request<Types.MoveNoteResponse>({
      method: "POST",
      url: "/api/links/{sourceId}/move/{targetId}",
      path: { sourceId, targetId },
      body: data,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }
}

// Instance-based wrappers for all service classes
// Methods are converted from static methods that take data objects to instance methods with individual parameters

class RestNotebookCertificateApprovalControllerServiceInstance extends ServiceInstance {
  getAllPendingRequest(): CancelablePromise<Types.GetAllPendingRequestResponse> {
    return this.httpRequest.request<Types.GetAllPendingRequestResponse>({
      method: "GET",
      url: "/api/notebook_certificate_approvals/get-all-pending-request",
      errors: { 500: "Internal Server Error" },
    })
  }

  approve(
    notebookCertificateApproval: number
  ): CancelablePromise<Types.ApproveResponse> {
    return this.httpRequest.request<Types.ApproveResponse>({
      method: "POST",
      url: "/api/notebook_certificate_approvals/{notebookCertificateApproval}/approve",
      path: { notebookCertificateApproval },
      errors: { 500: "Internal Server Error" },
    })
  }

  getApprovalForNotebook(
    notebook: number
  ): CancelablePromise<Types.GetApprovalForNotebookResponse> {
    return this.httpRequest.request<Types.GetApprovalForNotebookResponse>({
      method: "GET",
      url: "/api/notebook_certificate_approvals/for-notebook/{notebook}",
      path: { notebook },
      errors: { 500: "Internal Server Error" },
    })
  }

  requestApprovalForNotebook(
    notebook: number
  ): CancelablePromise<Types.RequestApprovalForNotebookResponse> {
    return this.httpRequest.request<Types.RequestApprovalForNotebookResponse>({
      method: "POST",
      url: "/api/notebook_certificate_approvals/request-approval/{notebook}",
      path: { notebook },
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestFineTuningDataControllerServiceInstance extends ServiceInstance {
  uploadAndTriggerFineTuning(): CancelablePromise<Types.UploadAndTriggerFineTuningResponse> {
    return this.httpRequest.request<Types.UploadAndTriggerFineTuningResponse>({
      method: "POST",
      url: "/api/fine-tuning/upload-and-trigger-fine-tuning",
      errors: { 500: "Internal Server Error" },
    })
  }

  getAllSuggestedQuestions(): CancelablePromise<Types.GetAllSuggestedQuestionsResponse> {
    return this.httpRequest.request<Types.GetAllSuggestedQuestionsResponse>({
      method: "GET",
      url: "/api/fine-tuning/all-suggested-questions-for-fine-tuning",
      errors: { 500: "Internal Server Error" },
    })
  }

  updateSuggestedQuestionForFineTuning(
    suggestedQuestion: number,
    requestBody: Types.UpdateSuggestedQuestionForFineTuningData["requestBody"]
  ): CancelablePromise<Types.UpdateSuggestedQuestionForFineTuningResponse> {
    return this.httpRequest.request<Types.UpdateSuggestedQuestionForFineTuningResponse>(
      {
        method: "PATCH",
        url: "/api/fine-tuning/{suggestedQuestion}/update-suggested-question-for-fine-tuning",
        path: { suggestedQuestion },
        body: requestBody,
        mediaType: "application/json",
        errors: { 500: "Internal Server Error" },
      }
    )
  }

  duplicate(
    suggestedQuestion: number
  ): CancelablePromise<Types.DuplicateResponse> {
    return this.httpRequest.request<Types.DuplicateResponse>({
      method: "POST",
      url: "/api/fine-tuning/{suggestedQuestion}/duplicate",
      path: { suggestedQuestion },
      errors: { 500: "Internal Server Error" },
    })
  }

  delete(suggestedQuestion: number): CancelablePromise<Types.DeleteResponse> {
    return this.httpRequest.request<Types.DeleteResponse>({
      method: "POST",
      url: "/api/fine-tuning/{suggestedQuestion}/delete",
      path: { suggestedQuestion },
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestBazaarControllerServiceInstance extends ServiceInstance {
  bazaar(): CancelablePromise<Types.BazaarResponse> {
    return this.httpRequest.request<Types.BazaarResponse>({
      method: "GET",
      url: "/api/bazaar",
      errors: { 500: "Internal Server Error" },
    })
  }

  removeFromBazaar(
    notebook: number
  ): CancelablePromise<Types.RemoveFromBazaarResponse> {
    return this.httpRequest.request<Types.RemoveFromBazaarResponse>({
      method: "POST",
      url: "/api/bazaar/{notebook}/remove",
      path: { notebook },
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestAiControllerServiceInstance extends ServiceInstance {
  getAvailableGptModels(): CancelablePromise<Types.GetAvailableGptModelsResponse> {
    return this.httpRequest.request<Types.GetAvailableGptModelsResponse>({
      method: "GET",
      url: "/api/ai/available-gpt-models",
      errors: { 500: "Internal Server Error" },
    })
  }

  suggestTitle(note: number): CancelablePromise<Types.SuggestTitleResponse> {
    return this.httpRequest.request<Types.SuggestTitleResponse>({
      method: "POST",
      url: "/api/ai/suggest-title/{note}",
      path: { note },
      errors: { 500: "Internal Server Error" },
    })
  }

  generateImage(
    requestBody: Types.GenerateImageData["requestBody"]
  ): CancelablePromise<Types.GenerateImageResponse> {
    return this.httpRequest.request<Types.GenerateImageResponse>({
      method: "POST",
      url: "/api/ai/generate-image",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestGlobalSettingsControllerServiceInstance extends ServiceInstance {
  getCurrentModelVersions(): CancelablePromise<Types.GetCurrentModelVersionsResponse> {
    return this.httpRequest.request<Types.GetCurrentModelVersionsResponse>({
      method: "GET",
      url: "/api/settings/current-model-version",
      errors: { 500: "Internal Server Error" },
    })
  }

  setCurrentModelVersions(
    requestBody: Types.SetCurrentModelVersionsData["requestBody"]
  ): CancelablePromise<Types.SetCurrentModelVersionsResponse> {
    return this.httpRequest.request<Types.SetCurrentModelVersionsResponse>({
      method: "POST",
      url: "/api/settings/current-model-version",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }
}

class TestabilityRestControllerServiceInstance extends ServiceInstance {
  randomizer(
    data: Types.Randomization
  ): CancelablePromise<Types.RandomizerResponse> {
    return this.httpRequest.request<Types.RandomizerResponse>({
      method: "POST",
      url: "/api/testability/randomizer",
      body: data,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  enableFeatureToggle(
    requestBody: Types.EnableFeatureToggleData["requestBody"]
  ): CancelablePromise<Types.EnableFeatureToggleResponse> {
    return this.httpRequest.request<Types.EnableFeatureToggleResponse>({
      method: "POST",
      url: "/api/testability/feature_toggle",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  getFeatureToggle(): CancelablePromise<Types.GetFeatureToggleResponse> {
    return this.httpRequest.request<Types.GetFeatureToggleResponse>({
      method: "GET",
      url: "/api/testability/feature_toggle",
      errors: { 500: "Internal Server Error" },
    })
  }

  resetDbAndTestabilitySettings(): CancelablePromise<Types.ResetDbAndTestabilitySettingsResponse> {
    return this.httpRequest.request<Types.ResetDbAndTestabilitySettingsResponse>(
      {
        method: "POST",
        url: "/api/testability/clean_db_and_reset_testability_settings",
        errors: { 500: "Internal Server Error" },
      }
    )
  }

  injectNotes(
    requestBody: Types.NotesTestData
  ): CancelablePromise<Types.InjectNotesResponse> {
    return this.httpRequest.request<Types.InjectNotesResponse>({
      method: "POST",
      url: "/api/testability/inject_notes",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  injectPredefinedQuestion(
    requestBody: Types.PredefinedQuestionsTestData
  ): CancelablePromise<Types.InjectPredefinedQuestionResponse> {
    return this.httpRequest.request<Types.InjectPredefinedQuestionResponse>({
      method: "POST",
      url: "/api/testability/inject-predefined-questions",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  linkNotes(
    requestBody: Types.LinkNotesData["requestBody"]
  ): CancelablePromise<Types.LinkNotesResponse> {
    return this.httpRequest.request<Types.LinkNotesResponse>({
      method: "POST",
      url: "/api/testability/link_notes",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  injectSuggestedQuestion(
    requestBody: Types.SuggestedQuestionsData
  ): CancelablePromise<Types.InjectSuggestedQuestionResponse> {
    return this.httpRequest.request<Types.InjectSuggestedQuestionResponse>({
      method: "POST",
      url: "/api/testability/inject_suggested_questions",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  timeTravel(
    requestBody: Types.TimeTravel
  ): CancelablePromise<Types.TimeTravelResponse> {
    return this.httpRequest.request<Types.TimeTravelResponse>({
      method: "POST",
      url: "/api/testability/time_travel",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  timeTravelRelativeToNow(
    requestBody: Types.TimeTravelRelativeToNow
  ): CancelablePromise<Types.TimeTravelRelativeToNowResponse> {
    return this.httpRequest.request<Types.TimeTravelRelativeToNowResponse>({
      method: "POST",
      url: "/api/testability/time_travel_relative_to_now",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  triggerException(): CancelablePromise<Types.TriggerExceptionResponse> {
    return this.httpRequest.request<Types.TriggerExceptionResponse>({
      method: "POST",
      url: "/api/testability/trigger_exception",
      errors: { 500: "Internal Server Error" },
    })
  }

  shareToBazaar(
    requestBody: Types.ShareToBazaarData["requestBody"]
  ): CancelablePromise<Types.ShareToBazaarResponse> {
    return this.httpRequest.request<Types.ShareToBazaarResponse>({
      method: "POST",
      url: "/api/testability/share_to_bazaar",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  injectCircle(
    requestBody: Types.InjectCircleData["requestBody"]
  ): CancelablePromise<Types.InjectCircleResponse> {
    return this.httpRequest.request<Types.InjectCircleResponse>({
      method: "POST",
      url: "/api/testability/inject_circle",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  updateCurrentUser(
    requestBody: Types.UpdateCurrentUserData["requestBody"]
  ): CancelablePromise<Types.UpdateCurrentUserResponse> {
    return this.httpRequest.request<Types.UpdateCurrentUserResponse>({
      method: "POST",
      url: "/api/testability/update_current_user",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  replaceServiceUrl(
    requestBody: Types.ReplaceServiceUrlData["requestBody"]
  ): CancelablePromise<Types.ReplaceServiceUrlResponse> {
    return this.httpRequest.request<Types.ReplaceServiceUrlResponse>({
      method: "POST",
      url: "/api/testability/replace_service_url",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestAiAudioControllerServiceInstance extends ServiceInstance {
  audioToText(
    formData?: Types.AudioToTextData["formData"]
  ): CancelablePromise<Types.AudioToTextResponse> {
    return this.httpRequest.request<Types.AudioToTextResponse>({
      method: "POST",
      url: "/api/audio/audio-to-text",
      formData: formData as unknown as Record<string, unknown>,
      mediaType: "multipart/form-data",
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestUserControllerServiceInstance extends ServiceInstance {
  getUserProfile(): CancelablePromise<Types.GetUserProfileResponse> {
    return this.httpRequest.request<Types.GetUserProfileResponse>({
      method: "GET",
      url: "/api/user",
      errors: { 500: "Internal Server Error" },
    })
  }

  createUser(
    requestBody: Types.CreateUserData["requestBody"]
  ): CancelablePromise<Types.CreateUserResponse> {
    return this.httpRequest.request<Types.CreateUserResponse>({
      method: "POST",
      url: "/api/user",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  generateToken(
    requestBody: Types.GenerateTokenData["requestBody"]
  ): CancelablePromise<Types.GenerateTokenResponse> {
    return this.httpRequest.request<Types.GenerateTokenResponse>({
      method: "POST",
      url: "/api/user/generate-token",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  getTokens(): CancelablePromise<Types.GetTokensResponse> {
    return this.httpRequest.request<Types.GetTokensResponse>({
      method: "GET",
      url: "/api/user/get-tokens",
      errors: { 500: "Internal Server Error" },
    })
  }

  deleteToken(tokenId: number): CancelablePromise<Types.DeleteTokenResponse> {
    return this.httpRequest.request<Types.DeleteTokenResponse>({
      method: "DELETE",
      url: "/api/user/token/{tokenId}",
      path: { tokenId },
      errors: { 500: "Internal Server Error" },
    })
  }

  updateUser(
    user: number,
    requestBody: Types.UpdateUserData["requestBody"]
  ): CancelablePromise<Types.UpdateUserResponse> {
    return this.httpRequest.request<Types.UpdateUserResponse>({
      method: "PATCH",
      url: "/api/user/{user}",
      path: { user },
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestSearchControllerServiceInstance extends ServiceInstance {
  semanticSearch(
    requestBody: Types.SemanticSearchData["requestBody"]
  ): CancelablePromise<Types.SemanticSearchResponse> {
    return this.httpRequest.request<Types.SemanticSearchResponse>({
      method: "POST",
      url: "/api/notes/semantic-search",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  semanticSearchWithin(
    note: number,
    requestBody: Types.SemanticSearchWithinData["requestBody"]
  ): CancelablePromise<Types.SemanticSearchWithinResponse> {
    return this.httpRequest.request<Types.SemanticSearchWithinResponse>({
      method: "POST",
      url: "/api/notes/{note}/semantic-search",
      path: { note },
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  searchForLinkTarget(
    requestBody: Types.SearchForLinkTargetData["requestBody"]
  ): CancelablePromise<Types.SearchForLinkTargetResponse> {
    return this.httpRequest.request<Types.SearchForLinkTargetResponse>({
      method: "POST",
      url: "/api/notes/search",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  searchForLinkTargetWithin(
    note: number,
    requestBody: Types.SearchForLinkTargetWithinData["requestBody"]
  ): CancelablePromise<Types.SearchForLinkTargetWithinResponse> {
    return this.httpRequest.request<Types.SearchForLinkTargetWithinResponse>({
      method: "POST",
      url: "/api/notes/{note}/search",
      path: { note },
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestMemoryTrackerControllerServiceInstance extends ServiceInstance {
  selfEvaluate(
    memoryTracker: number,
    requestBody: Types.SelfEvaluateData["requestBody"]
  ): CancelablePromise<Types.SelfEvaluateResponse> {
    return this.httpRequest.request<Types.SelfEvaluateResponse>({
      method: "POST",
      url: "/api/memory-trackers/{memoryTracker}/self-evaluate",
      path: { memoryTracker },
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  removeFromRepeating(
    memoryTracker: number
  ): CancelablePromise<Types.RemoveFromRepeatingResponse> {
    return this.httpRequest.request<Types.RemoveFromRepeatingResponse>({
      method: "POST",
      url: "/api/memory-trackers/{memoryTracker}/remove",
      path: { memoryTracker },
      errors: { 500: "Internal Server Error" },
    })
  }

  answerSpelling(
    memoryTracker: number,
    requestBody: Types.AnswerSpellingData["requestBody"]
  ): CancelablePromise<Types.AnswerSpellingResponse> {
    return this.httpRequest.request<Types.AnswerSpellingResponse>({
      method: "POST",
      url: "/api/memory-trackers/{memoryTracker}/answer-spelling",
      path: { memoryTracker },
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  markAsRepeated(
    memoryTracker: number,
    successful: boolean
  ): CancelablePromise<Types.MarkAsRepeatedResponse> {
    return this.httpRequest.request<Types.MarkAsRepeatedResponse>({
      method: "PATCH",
      url: "/api/memory-trackers/{memoryTracker}/mark-as-repeated",
      path: { memoryTracker },
      query: { successful },
      errors: { 500: "Internal Server Error" },
    })
  }

  show1(memoryTracker: number): CancelablePromise<Types.Show1Response> {
    return this.httpRequest.request<Types.Show1Response>({
      method: "GET",
      url: "/api/memory-trackers/{memoryTracker}",
      path: { memoryTracker },
      errors: { 500: "Internal Server Error" },
    })
  }

  getSpellingQuestion(
    memoryTracker: number
  ): CancelablePromise<Types.GetSpellingQuestionResponse> {
    return this.httpRequest.request<Types.GetSpellingQuestionResponse>({
      method: "GET",
      url: "/api/memory-trackers/{memoryTracker}/spelling-question",
      path: { memoryTracker },
      errors: { 500: "Internal Server Error" },
    })
  }

  getRecentlyReviewed(): CancelablePromise<Types.GetRecentlyReviewedResponse> {
    return this.httpRequest.request<Types.GetRecentlyReviewedResponse>({
      method: "GET",
      url: "/api/memory-trackers/recently-reviewed",
      errors: { 500: "Internal Server Error" },
    })
  }

  getRecentMemoryTrackers(): CancelablePromise<Types.GetRecentMemoryTrackersResponse> {
    return this.httpRequest.request<Types.GetRecentMemoryTrackersResponse>({
      method: "GET",
      url: "/api/memory-trackers/recent",
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestRecallPromptControllerServiceInstance extends ServiceInstance {
  regenerate(
    recallPrompt: number,
    requestBody: Types.RegenerateData["requestBody"]
  ): CancelablePromise<Types.RegenerateResponse> {
    return this.httpRequest.request<Types.RegenerateResponse>({
      method: "POST",
      url: "/api/recall-prompts/{recallPrompt}/regenerate",
      path: { recallPrompt },
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  contest(recallPrompt: number): CancelablePromise<Types.ContestResponse> {
    return this.httpRequest.request<Types.ContestResponse>({
      method: "POST",
      url: "/api/recall-prompts/{recallPrompt}/contest",
      path: { recallPrompt },
      errors: { 500: "Internal Server Error" },
    })
  }

  answerQuiz(
    recallPrompt: number,
    requestBody: Types.AnswerQuizData["requestBody"]
  ): CancelablePromise<Types.AnswerQuizResponse> {
    return this.httpRequest.request<Types.AnswerQuizResponse>({
      method: "POST",
      url: "/api/recall-prompts/{recallPrompt}/answer",
      path: { recallPrompt },
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  showQuestion(
    recallPrompt: number
  ): CancelablePromise<Types.ShowQuestionResponse> {
    return this.httpRequest.request<Types.ShowQuestionResponse>({
      method: "GET",
      url: "/api/recall-prompts/{recallPrompt}",
      path: { recallPrompt },
      errors: { 500: "Internal Server Error" },
    })
  }

  askAQuestion(
    memoryTracker: number
  ): CancelablePromise<Types.AskAquestionResponse> {
    return this.httpRequest.request<Types.AskAquestionResponse>({
      method: "GET",
      url: "/api/recall-prompts/{memoryTracker}/question",
      path: { memoryTracker },
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestRecallsControllerServiceInstance extends ServiceInstance {
  recalling(
    timezone?: string,
    dueindays?: number
  ): CancelablePromise<Types.RecallingResponse> {
    return this.httpRequest.request<Types.RecallingResponse>({
      method: "GET",
      url: "/api/recalls/recalling",
      query: { timezone, dueindays },
      errors: { 500: "Internal Server Error" },
    })
  }

  overview(timezone?: string): CancelablePromise<Types.OverviewResponse> {
    return this.httpRequest.request<Types.OverviewResponse>({
      method: "GET",
      url: "/api/recalls/overview",
      query: { timezone },
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestPredefinedQuestionControllerServiceInstance extends ServiceInstance {
  suggestQuestionForFineTuning(
    predefinedQuestion: number,
    requestBody: Types.SuggestQuestionForFineTuningData["requestBody"]
  ): CancelablePromise<Types.SuggestQuestionForFineTuningResponse> {
    return this.httpRequest.request<Types.SuggestQuestionForFineTuningResponse>(
      {
        method: "POST",
        url: "/api/predefined-questions/{predefinedQuestion}/suggest-fine-tuning",
        path: { predefinedQuestion },
        body: requestBody,
        mediaType: "application/json",
        errors: { 500: "Internal Server Error" },
      }
    )
  }

  addQuestionManually(
    note: number,
    requestBody: Types.AddQuestionManuallyData["requestBody"]
  ): CancelablePromise<Types.AddQuestionManuallyResponse> {
    return this.httpRequest.request<Types.AddQuestionManuallyResponse>({
      method: "POST",
      url: "/api/predefined-questions/{note}/note-questions",
      path: { note },
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  refineQuestion(
    note: number,
    requestBody: Types.RefineQuestionData["requestBody"]
  ): CancelablePromise<Types.RefineQuestionResponse> {
    return this.httpRequest.request<Types.RefineQuestionResponse>({
      method: "POST",
      url: "/api/predefined-questions/{note}/refine-question",
      path: { note },
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  getAllQuestionByNote(
    note: number
  ): CancelablePromise<Types.GetAllQuestionByNoteResponse> {
    return this.httpRequest.request<Types.GetAllQuestionByNoteResponse>({
      method: "GET",
      url: "/api/predefined-questions/{note}/note-questions",
      path: { note },
      errors: { 500: "Internal Server Error" },
    })
  }

  generateQuestionWithoutSave(
    note: number
  ): CancelablePromise<Types.GenerateQuestionWithoutSaveResponse> {
    return this.httpRequest.request<Types.GenerateQuestionWithoutSaveResponse>({
      method: "POST",
      url: "/api/predefined-questions/generate-question-without-save",
      query: { note },
      errors: { 500: "Internal Server Error" },
    })
  }

  toggleApproval(
    predefinedQuestion: number
  ): CancelablePromise<Types.ToggleApprovalResponse> {
    return this.httpRequest.request<Types.ToggleApprovalResponse>({
      method: "POST",
      url: "/api/predefined-questions/{predefinedQuestion}/toggle-approval",
      path: { predefinedQuestion },
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestWikidataControllerServiceInstance extends ServiceInstance {
  searchWikidata(
    search: string
  ): CancelablePromise<Types.SearchWikidataResponse> {
    return this.httpRequest.request<Types.SearchWikidataResponse>({
      method: "GET",
      url: "/api/wikidata/search/{search}",
      path: { search },
      errors: { 500: "Internal Server Error" },
    })
  }

  fetchWikidataEntityDataById(
    wikidataId: string
  ): CancelablePromise<Types.FetchWikidataEntityDataByIdResponse> {
    return this.httpRequest.request<Types.FetchWikidataEntityDataByIdResponse>({
      method: "GET",
      url: "/api/wikidata/entity-data/{wikidataId}",
      path: { wikidataId },
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestConversationMessageControllerServiceInstance extends ServiceInstance {
  getAiReply(
    conversationId: number
  ): CancelablePromise<Types.GetAiReplyResponse> {
    return this.httpRequest.request<Types.GetAiReplyResponse>({
      method: "POST",
      url: "/api/conversation/{conversationId}/ai-reply",
      path: { conversationId },
      errors: { 500: "Internal Server Error" },
    })
  }

  getConversationsAboutNote(
    note: number
  ): CancelablePromise<Types.GetConversationsAboutNoteResponse> {
    return this.httpRequest.request<Types.GetConversationsAboutNoteResponse>({
      method: "GET",
      url: "/api/conversation/note/{note}",
      path: { note },
      errors: { 500: "Internal Server Error" },
    })
  }

  getConversationMessages(
    conversationId: number
  ): CancelablePromise<Types.GetConversationMessagesResponse> {
    return this.httpRequest.request<Types.GetConversationMessagesResponse>({
      method: "GET",
      url: "/api/conversation/{conversationId}/messages",
      path: { conversationId },
      errors: { 500: "Internal Server Error" },
    })
  }

  replyToConversation(
    conversationId: number,
    requestBody: Types.ReplyToConversationData["requestBody"]
  ): CancelablePromise<Types.ReplyToConversationResponse> {
    return this.httpRequest.request<Types.ReplyToConversationResponse>({
      method: "POST",
      url: "/api/conversation/{conversationId}/send",
      path: { conversationId },
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  startConversationAboutNote(
    note: number,
    requestBody?: Types.StartConversationAboutNoteData["requestBody"]
  ): CancelablePromise<Types.StartConversationAboutNoteResponse> {
    return this.httpRequest.request<Types.StartConversationAboutNoteResponse>({
      method: "POST",
      url: "/api/conversation/note/{note}",
      path: { note },
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  startConversationAboutRecallPrompt(
    recallPrompt: number
  ): CancelablePromise<Types.StartConversationAboutRecallPromptResponse> {
    return this.httpRequest.request<Types.StartConversationAboutRecallPromptResponse>(
      {
        method: "POST",
        url: "/api/conversation/recall-prompt/{recallPrompt}",
        path: { recallPrompt },
        errors: { 500: "Internal Server Error" },
      }
    )
  }

  startConversationAboutAssessmentQuestion(
    assessmentQuestion: number,
    requestBody: Types.StartConversationAboutAssessmentQuestionData["requestBody"]
  ): CancelablePromise<Types.StartConversationAboutAssessmentQuestionResponse> {
    return this.httpRequest.request<Types.StartConversationAboutAssessmentQuestionResponse>(
      {
        method: "POST",
        url: "/api/conversation/assessment-question/{assessmentQuestion}",
        path: { assessmentQuestion },
        body: requestBody,
        mediaType: "application/json",
        errors: { 500: "Internal Server Error" },
      }
    )
  }

  markConversationAsRead(
    conversationId: number
  ): CancelablePromise<Types.MarkConversationAsReadResponse> {
    return this.httpRequest.request<Types.MarkConversationAsReadResponse>({
      method: "PATCH",
      url: "/api/conversation/{conversationId}/read",
      path: { conversationId },
      errors: { 500: "Internal Server Error" },
    })
  }

  getConversation(
    conversationId: number
  ): CancelablePromise<Types.GetConversationResponse> {
    return this.httpRequest.request<Types.GetConversationResponse>({
      method: "GET",
      url: "/api/conversation/{conversationId}",
      path: { conversationId },
      errors: { 500: "Internal Server Error" },
    })
  }

  getConversationsOfCurrentUser(): CancelablePromise<Types.GetConversationsOfCurrentUserResponse> {
    return this.httpRequest.request<Types.GetConversationsOfCurrentUserResponse>(
      {
        method: "GET",
        url: "/api/conversation",
        errors: { 500: "Internal Server Error" },
      }
    )
  }

  getUnreadConversations(): CancelablePromise<Types.GetUnreadConversationsResponse> {
    return this.httpRequest.request<Types.GetUnreadConversationsResponse>({
      method: "GET",
      url: "/api/conversation/unread",
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestAssessmentControllerServiceInstance extends ServiceInstance {
  answerQuestion(
    assessmentQuestionInstance: number,
    requestBody: Types.AnswerQuestionData["requestBody"]
  ): CancelablePromise<Types.AnswerQuestionResponse> {
    return this.httpRequest.request<Types.AnswerQuestionResponse>({
      method: "POST",
      url: "/api/assessment/{assessmentQuestionInstance}/answer",
      path: { assessmentQuestionInstance },
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  submitAssessmentResult(
    assessmentAttempt: number
  ): CancelablePromise<Types.SubmitAssessmentResultResponse> {
    return this.httpRequest.request<Types.SubmitAssessmentResultResponse>({
      method: "POST",
      url: "/api/assessment/{assessmentAttempt}",
      path: { assessmentAttempt },
      errors: { 500: "Internal Server Error" },
    })
  }

  generateAssessmentQuestions(
    notebook: number
  ): CancelablePromise<Types.GenerateAssessmentQuestionsResponse> {
    return this.httpRequest.request<Types.GenerateAssessmentQuestionsResponse>({
      method: "POST",
      url: "/api/assessment/questions/{notebook}",
      path: { notebook },
      errors: { 500: "Internal Server Error" },
    })
  }

  getMyAssessments(): CancelablePromise<Types.GetMyAssessmentsResponse> {
    return this.httpRequest.request<Types.GetMyAssessmentsResponse>({
      method: "GET",
      url: "/api/assessment",
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestSubscriptionControllerServiceInstance extends ServiceInstance {
  createSubscription(
    notebook: number,
    requestBody: Types.CreateSubscriptionData["requestBody"]
  ): CancelablePromise<Types.CreateSubscriptionResponse> {
    return this.httpRequest.request<Types.CreateSubscriptionResponse>({
      method: "POST",
      url: "/api/subscriptions/notebooks/{notebook}/subscribe",
      path: { notebook },
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestCertificateControllerServiceInstance extends ServiceInstance {
  getCertificate(
    notebook: number
  ): CancelablePromise<Types.GetCertificateResponse> {
    return this.httpRequest.request<Types.GetCertificateResponse>({
      method: "GET",
      url: "/api/certificate/{notebook}",
      path: { notebook },
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestNotebookControllerServiceInstance extends ServiceInstance {
  updateAiAssistant(
    notebook: number,
    requestBody: Types.UpdateAiAssistantData["requestBody"]
  ): CancelablePromise<Types.UpdateAiAssistantResponse> {
    return this.httpRequest.request<Types.UpdateAiAssistantResponse>({
      method: "POST",
      url: "/api/notebooks/{notebook}/ai-assistant",
      path: { notebook },
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  downloadNotebookDump(
    notebook: number
  ): CancelablePromise<Types.DownloadNotebookDumpResponse> {
    return this.httpRequest.request<Types.DownloadNotebookDumpResponse>({
      method: "GET",
      url: "/api/notebooks/{notebook}/dump",
      path: { notebook },
      errors: { 500: "Internal Server Error" },
    })
  }

  getAiAssistant(
    notebook: number
  ): CancelablePromise<Types.GetAiAssistantResponse> {
    return this.httpRequest.request<Types.GetAiAssistantResponse>({
      method: "GET",
      url: "/api/notebooks/{notebook}/ai-assistant",
      path: { notebook },
      errors: { 500: "Internal Server Error" },
    })
  }

  shareNotebook(
    notebook: number
  ): CancelablePromise<Types.ShareNotebookResponse> {
    return this.httpRequest.request<Types.ShareNotebookResponse>({
      method: "POST",
      url: "/api/notebooks/{notebook}/share",
      path: { notebook },
      errors: { 500: "Internal Server Error" },
    })
  }

  update1(
    notebook: number,
    requestBody: Types.Update1Data["requestBody"]
  ): CancelablePromise<Types.Update1Response> {
    return this.httpRequest.request<Types.Update1Response>({
      method: "POST",
      url: "/api/notebooks/{notebook}",
      path: { notebook },
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  importObsidian(
    notebook: number,
    formData: Types.ImportObsidianData["formData"]
  ): CancelablePromise<Types.ImportObsidianResponse> {
    return this.httpRequest.request<Types.ImportObsidianResponse>({
      method: "POST",
      url: "/api/notebooks/{notebook}/import-obsidian",
      path: { notebook },
      formData: formData as unknown as Record<string, unknown>,
      mediaType: "multipart/form-data",
      errors: { 500: "Internal Server Error" },
    })
  }

  resetNotebookIndex(
    notebook: number
  ): CancelablePromise<Types.ResetNotebookIndexResponse> {
    return this.httpRequest.request<Types.ResetNotebookIndexResponse>({
      method: "POST",
      url: "/api/notebooks/{notebook}/reset-index",
      path: { notebook },
      errors: { 500: "Internal Server Error" },
    })
  }

  updateNotebookIndex(
    notebook: number
  ): CancelablePromise<Types.UpdateNotebookIndexResponse> {
    return this.httpRequest.request<Types.UpdateNotebookIndexResponse>({
      method: "POST",
      url: "/api/notebooks/{notebook}/update-index",
      path: { notebook },
      errors: { 500: "Internal Server Error" },
    })
  }

  moveToCircle(
    notebook: number,
    circle: number
  ): CancelablePromise<Types.MoveToCircleResponse> {
    return this.httpRequest.request<Types.MoveToCircleResponse>({
      method: "PATCH",
      url: "/api/notebooks/{notebook}/move-to-circle/{circle}",
      path: { notebook, circle },
      errors: { 500: "Internal Server Error" },
    })
  }

  createNotebook(
    requestBody: Types.CreateNotebookData["requestBody"]
  ): CancelablePromise<Types.CreateNotebookResponse> {
    return this.httpRequest.request<Types.CreateNotebookResponse>({
      method: "POST",
      url: "/api/notebooks",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  getNotes(notebook: number): CancelablePromise<Types.GetNotesResponse> {
    return this.httpRequest.request<Types.GetNotesResponse>({
      method: "GET",
      url: "/api/notebooks/{notebook}/notes",
      path: { notebook },
      errors: { 500: "Internal Server Error" },
    })
  }

  myNotebooks(): CancelablePromise<Types.MyNotebooksResponse> {
    return this.httpRequest.request<Types.MyNotebooksResponse>({
      method: "GET",
      url: "/api/notebooks",
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestCircleControllerServiceInstance extends ServiceInstance {
  createCircle(
    requestBody: Types.CreateCircleData["requestBody"]
  ): CancelablePromise<Types.CreateCircleResponse> {
    return this.httpRequest.request<Types.CreateCircleResponse>({
      method: "POST",
      url: "/api/circles",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  index(): CancelablePromise<Types.IndexResponse> {
    return this.httpRequest.request<Types.IndexResponse>({
      method: "GET",
      url: "/api/circles",
      errors: { 500: "Internal Server Error" },
    })
  }

  createNotebookInCircle(
    circle: number,
    requestBody: Types.CreateNotebookInCircleData["requestBody"]
  ): CancelablePromise<Types.CreateNotebookInCircleResponse> {
    return this.httpRequest.request<Types.CreateNotebookInCircleResponse>({
      method: "POST",
      url: "/api/circles/{circle}/notebooks",
      path: { circle },
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  showCircle(circle: number): CancelablePromise<Types.ShowCircleResponse> {
    return this.httpRequest.request<Types.ShowCircleResponse>({
      method: "GET",
      url: "/api/circles/{circle}",
      path: { circle },
      errors: { 500: "Internal Server Error" },
    })
  }

  joinCircle(
    requestBody: Types.JoinCircleData["requestBody"]
  ): CancelablePromise<Types.JoinCircleResponse> {
    return this.httpRequest.request<Types.JoinCircleResponse>({
      method: "POST",
      url: "/api/circles/join",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestCurrentUserInfoControllerServiceInstance extends ServiceInstance {
  currentUserInfo(): CancelablePromise<Types.CurrentUserInfoResponse> {
    return this.httpRequest.request<Types.CurrentUserInfoResponse>({
      method: "GET",
      url: "/api/user/current-user-info",
      errors: { 500: "Internal Server Error" },
    })
  }
}

class AssimilationControllerServiceInstance extends ServiceInstance {
  assimilate(
    requestBody: Types.AssimilateData["requestBody"]
  ): CancelablePromise<Types.AssimilateResponse> {
    return this.httpRequest.request<Types.AssimilateResponse>({
      method: "POST",
      url: "/api/assimilation",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }

  getAssimilationCount(
    timezone?: string
  ): CancelablePromise<Types.GetAssimilationCountResponse> {
    return this.httpRequest.request<Types.GetAssimilationCountResponse>({
      method: "GET",
      url: "/api/assimilation/count",
      query: { timezone },
      errors: { 500: "Internal Server Error" },
    })
  }

  assimilating(
    timezone?: string
  ): CancelablePromise<Types.AssimilatingResponse> {
    return this.httpRequest.request<Types.AssimilatingResponse>({
      method: "GET",
      url: "/api/assimilation/assimilating",
      query: { timezone },
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestFailureReportControllerServiceInstance extends ServiceInstance {
  failureReports(): CancelablePromise<Types.FailureReportsResponse> {
    return this.httpRequest.request<Types.FailureReportsResponse>({
      method: "GET",
      url: "/api/failure-reports",
      errors: { 500: "Internal Server Error" },
    })
  }

  show2(failureReport: number): CancelablePromise<Types.Show2Response> {
    return this.httpRequest.request<Types.Show2Response>({
      method: "GET",
      url: "/api/failure-reports/{failureReport}",
      path: { failureReport },
      errors: { 500: "Internal Server Error" },
    })
  }

  deleteFailureReports(
    requestBody: Types.DeleteFailureReportsData["requestBody"]
  ): CancelablePromise<Types.DeleteFailureReportsResponse> {
    return this.httpRequest.request<Types.DeleteFailureReportsResponse>({
      method: "DELETE",
      url: "/api/failure-reports/delete",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }
}

class RestHealthCheckControllerServiceInstance extends ServiceInstance {
  ping(): CancelablePromise<Types.PingResponse> {
    return this.httpRequest.request<Types.PingResponse>({
      method: "GET",
      url: "/api/healthcheck",
      errors: { 500: "Internal Server Error" },
    })
  }
}

class McpNoteCreationControllerServiceInstance extends ServiceInstance {
  createNote1(
    requestBody: Types.McpNoteAddDTO
  ): CancelablePromise<Types.CreateNote1Response> {
    return this.httpRequest.request<Types.CreateNote1Response>({
      method: "POST",
      url: "/api/mcp/notes/create",
      body: requestBody,
      mediaType: "application/json",
      errors: { 500: "Internal Server Error" },
    })
  }
}

export class DoughnutApi {
  public readonly request: BaseHttpRequest
  public readonly restNoteController: RestNoteControllerServiceInstance
  public readonly restTextContentController: RestTextContentControllerServiceInstance
  public readonly restNoteCreationController: RestNoteCreationControllerServiceInstance
  public readonly restLinkController: RestLinkControllerServiceInstance
  public readonly restUserController: RestUserControllerServiceInstance
  public readonly restSearchController: RestSearchControllerServiceInstance
  public readonly restNotebookController: RestNotebookControllerServiceInstance
  public readonly restCircleController: RestCircleControllerServiceInstance
  public readonly restConversationMessageController: RestConversationMessageControllerServiceInstance
  public readonly restMemoryTrackerController: RestMemoryTrackerControllerServiceInstance
  public readonly restRecallPromptController: RestRecallPromptControllerServiceInstance
  public readonly restRecallsController: RestRecallsControllerServiceInstance
  public readonly restPredefinedQuestionController: RestPredefinedQuestionControllerServiceInstance
  public readonly restWikidataController: RestWikidataControllerServiceInstance
  public readonly restAiController: RestAiControllerServiceInstance
  public readonly restAiAudioController: RestAiAudioControllerServiceInstance
  public readonly restAssessmentController: RestAssessmentControllerServiceInstance
  public readonly restBazaarController: RestBazaarControllerServiceInstance
  public readonly restCertificateController: RestCertificateControllerServiceInstance
  public readonly restFailureReportController: RestFailureReportControllerServiceInstance
  public readonly restFineTuningDataController: RestFineTuningDataControllerServiceInstance
  public readonly restGlobalSettingsController: RestGlobalSettingsControllerServiceInstance
  public readonly restHealthCheckController: RestHealthCheckControllerServiceInstance
  public readonly restNotebookCertificateApprovalController: RestNotebookCertificateApprovalControllerServiceInstance
  public readonly restSubscriptionController: RestSubscriptionControllerServiceInstance
  public readonly restCurrentUserInfoController: RestCurrentUserInfoControllerServiceInstance
  public readonly testabilityRestController: TestabilityRestControllerServiceInstance
  public readonly assimilationController: AssimilationControllerServiceInstance
  public readonly mcpNoteCreationController: McpNoteCreationControllerServiceInstance

  constructor(
    config?: Partial<OpenAPIConfig>,
    HttpRequest: HttpRequestConstructor = FetchHttpRequest
  ) {
    const fullConfig: OpenAPIConfig = {
      BASE: config?.BASE ?? "",
      VERSION: config?.VERSION ?? "0",
      WITH_CREDENTIALS: config?.WITH_CREDENTIALS ?? false,
      CREDENTIALS: config?.CREDENTIALS ?? "include",
      TOKEN: config?.TOKEN,
      USERNAME: config?.USERNAME,
      PASSWORD: config?.PASSWORD,
      HEADERS: config?.HEADERS,
      ENCODE_PATH: config?.ENCODE_PATH,
      interceptors: config?.interceptors ?? {
        request: new Interceptors(),
        response: new Interceptors(),
      },
    }
    this.request = new HttpRequest(fullConfig)
    this.restNoteController = new RestNoteControllerServiceInstance(
      this.request
    )
    this.restTextContentController =
      new RestTextContentControllerServiceInstance(this.request)
    this.restNoteCreationController =
      new RestNoteCreationControllerServiceInstance(this.request)
    this.restLinkController = new RestLinkControllerServiceInstance(
      this.request
    )
    this.restUserController = new RestUserControllerServiceInstance(
      this.request
    )
    this.restSearchController = new RestSearchControllerServiceInstance(
      this.request
    )
    this.restNotebookController = new RestNotebookControllerServiceInstance(
      this.request
    )
    this.restCircleController = new RestCircleControllerServiceInstance(
      this.request
    )
    this.restConversationMessageController =
      new RestConversationMessageControllerServiceInstance(this.request)
    this.restMemoryTrackerController =
      new RestMemoryTrackerControllerServiceInstance(this.request)
    this.restRecallPromptController =
      new RestRecallPromptControllerServiceInstance(this.request)
    this.restRecallsController = new RestRecallsControllerServiceInstance(
      this.request
    )
    this.restPredefinedQuestionController =
      new RestPredefinedQuestionControllerServiceInstance(this.request)
    this.restWikidataController = new RestWikidataControllerServiceInstance(
      this.request
    )
    this.restAiController = new RestAiControllerServiceInstance(this.request)
    this.restAiAudioController = new RestAiAudioControllerServiceInstance(
      this.request
    )
    this.restAssessmentController = new RestAssessmentControllerServiceInstance(
      this.request
    )
    this.restBazaarController = new RestBazaarControllerServiceInstance(
      this.request
    )
    this.restCertificateController =
      new RestCertificateControllerServiceInstance(this.request)
    this.restFailureReportController =
      new RestFailureReportControllerServiceInstance(this.request)
    this.restFineTuningDataController =
      new RestFineTuningDataControllerServiceInstance(this.request)
    this.restGlobalSettingsController =
      new RestGlobalSettingsControllerServiceInstance(this.request)
    this.restHealthCheckController =
      new RestHealthCheckControllerServiceInstance(this.request)
    this.restNotebookCertificateApprovalController =
      new RestNotebookCertificateApprovalControllerServiceInstance(this.request)
    this.restSubscriptionController =
      new RestSubscriptionControllerServiceInstance(this.request)
    this.restCurrentUserInfoController =
      new RestCurrentUserInfoControllerServiceInstance(this.request)
    this.testabilityRestController =
      new TestabilityRestControllerServiceInstance(this.request)
    this.assimilationController = new AssimilationControllerServiceInstance(
      this.request
    )
    this.mcpNoteCreationController =
      new McpNoteCreationControllerServiceInstance(this.request)
  }
}
