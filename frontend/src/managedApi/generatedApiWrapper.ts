// Manually maintained wrapper that provides instance-based DoughnutApi
// This bridges the gap between openapi-ts static services and the frontend's instance-based expectations
// This file should NOT be in the generated folder to avoid being overwritten

export * from "@generated/backend"

// Re-export types that were in models/ folder
export type {
  User,
  AnsweredQuestion,
  SpellingResultDTO,
  NoteTopology,
  DummyForGeneratingTypes,
} from "@generated/backend/types.gen"

// Re-export service classes from services.gen
export {
  AssimilationControllerService,
  McpNoteCreationControllerService,
  RestAiAudioControllerService,
  RestAiControllerService,
  RestAssessmentControllerService,
  RestBazaarControllerService,
  RestCertificateControllerService,
  RestCircleControllerService,
  RestConversationMessageControllerService,
  RestCurrentUserInfoControllerService,
  RestFailureReportControllerService,
  RestFineTuningDataControllerService,
  RestGlobalSettingsControllerService,
  RestHealthCheckControllerService,
  RestLinkControllerService,
  RestMemoryTrackerControllerService,
  RestNotebookCertificateApprovalControllerService,
  RestNotebookControllerService,
  RestNoteControllerService,
  RestNoteCreationControllerService,
  RestPredefinedQuestionControllerService,
  RestRecallPromptControllerService,
  RestRecallsControllerService,
  RestSearchControllerService,
  RestSubscriptionControllerService,
  RestTextContentControllerService,
  RestUserControllerService,
  RestWikidataControllerService,
  TestabilityRestControllerService,
} from "@generated/backend/services.gen"
