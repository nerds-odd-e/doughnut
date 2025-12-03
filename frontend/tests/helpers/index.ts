import RenderingHelper from "./RenderingHelper"
import matchByText from "./matchByText"
import { vi } from "vitest"
import {
  UserController,
  TestabilityRestController,
  SubscriptionController,
  GlobalSettingsController,
  RecallPromptController,
  PredefinedQuestionController,
  NoteCreationController,
  NoteController,
  SearchController,
  NotebookController,
  NotebookCertificateApprovalController,
  MemoryTrackerController,
  McpNoteCreationController,
  LinkController,
  FineTuningDataController,
  ConversationMessageController,
  CircleController,
  CertificateController,
  BazaarController,
  AiAudioController,
  AssimilationController,
  AssessmentController,
  AiController,
  TextContentController,
  WikidataController,
  CurrentUserInfoController,
  RecallsController,
  HealthCheckController,
  FailureReportController,
} from "@generated/backend/sdk.gen"
import type { NoteRealm } from "@generated/backend"

// Mapping of method names to their controller classes
// biome-ignore lint/suspicious/noExplicitAny: Controller classes have different types and need any for dynamic access
const methodToController: Record<string, any> = {
  getUserProfile: UserController,
  createUser: UserController,
  generateToken: UserController,
  updateUser: UserController,
  getTokens: UserController,
  deleteToken: UserController,
  getMenuData: UserController,
  closeAllGithubIssues: TestabilityRestController,
  triggerException: TestabilityRestController,
  timeTravelRelativeToNow: TestabilityRestController,
  timeTravel: TestabilityRestController,
  testabilityUpdateUser: TestabilityRestController,
  shareToBazaar: TestabilityRestController,
  replaceServiceUrl: TestabilityRestController,
  randomizer: TestabilityRestController,
  linkNotes: TestabilityRestController,
  injectSuggestedQuestion: TestabilityRestController,
  injectNotes: TestabilityRestController,
  injectCircle: TestabilityRestController,
  injectPredefinedQuestion: TestabilityRestController,
  getFeatureToggle: TestabilityRestController,
  enableFeatureToggle: TestabilityRestController,
  resetDbAndTestabilitySettings: TestabilityRestController,
  githubIssues: TestabilityRestController,
  updateSubscription: SubscriptionController,
  destroySubscription: SubscriptionController,
  createSubscription: SubscriptionController,
  getCurrentModelVersions: GlobalSettingsController,
  setCurrentModelVersions: GlobalSettingsController,
  regenerate: RecallPromptController,
  contest: RecallPromptController,
  answerQuiz: RecallPromptController,
  answerSpelling: RecallPromptController,
  showQuestion: RecallPromptController,
  askAQuestion: MemoryTrackerController,
  toggleApproval: PredefinedQuestionController,
  suggestQuestionForFineTuning: PredefinedQuestionController,
  refineQuestion: PredefinedQuestionController,
  getAllQuestionByNote: PredefinedQuestionController,
  addQuestionManually: PredefinedQuestionController,
  generateQuestionWithoutSave: PredefinedQuestionController,
  exportQuestionGeneration: PredefinedQuestionController,
  createNoteAfter: NoteCreationController,
  createNoteUnderParent: NoteCreationController,
  updateWikidataId: NoteController,
  updateRecallSetting: NoteController,
  deleteNote: NoteController,
  moveAfter: NoteController,
  showNote: NoteController,
  updateNoteAccessories: NoteController,
  undoDeleteNote: NoteController,
  getNoteInfo: NoteController,
  getGraph: NoteController,
  getDescendants: NoteController,
  showNoteAccessory: NoteController,
  getRecentNotes: NoteController,
  semanticSearchWithin: SearchController,
  searchForLinkTargetWithin: SearchController,
  semanticSearch: SearchController,
  searchForLinkTarget: SearchController,
  get: NotebookController,
  updateNotebook: NotebookController,
  updateNotebookIndex: NotebookController,
  shareNotebook: NotebookController,
  resetNotebookIndex: NotebookController,
  downloadNotebookForObsidian: NotebookController,
  importObsidian: NotebookController,
  createNotebook: NotebookController,
  moveToCircle: NotebookController,
  getAiAssistant: NotebookController,
  updateAiAssistant: NotebookController,
  myNotebooks: NotebookController,
  approve: NotebookCertificateApprovalController,
  requestApprovalForNotebook: NotebookCertificateApprovalController,
  getAllPendingRequest: NotebookCertificateApprovalController,
  getApprovalForNotebook: NotebookCertificateApprovalController,
  selfEvaluate: MemoryTrackerController,
  removeFromRepeating: MemoryTrackerController,
  reEnable: MemoryTrackerController,
  markAsRepeated: MemoryTrackerController,
  showMemoryTracker: MemoryTrackerController,
  getRecentlyReviewed: MemoryTrackerController,
  getRecentMemoryTrackers: MemoryTrackerController,
  getRecallPrompts: MemoryTrackerController,
  deleteUnansweredRecallPrompts: MemoryTrackerController,
  createNoteViaMcp: McpNoteCreationController,
  updateLink: LinkController,
  moveNote: LinkController,
  linkNoteFinalize: LinkController,
  duplicate: FineTuningDataController,
  delete: FineTuningDataController,
  uploadAndTriggerFineTuning: FineTuningDataController,
  updateSuggestedQuestionForFineTuning: FineTuningDataController,
  getAllSuggestedQuestions: FineTuningDataController,
  replyToConversation: ConversationMessageController,
  getAiReply: ConversationMessageController,
  startConversationAboutRecallPrompt: ConversationMessageController,
  getConversationsAboutNote: ConversationMessageController,
  startConversationAboutNote: ConversationMessageController,
  startConversationAboutAssessmentQuestion: ConversationMessageController,
  markConversationAsRead: ConversationMessageController,
  getConversation: ConversationMessageController,
  getConversationMessages: ConversationMessageController,
  exportConversation: ConversationMessageController,
  getUnreadConversations: ConversationMessageController,
  getConversationsOfCurrentUser: ConversationMessageController,
  index: CircleController,
  createCircle: CircleController,
  createNotebookInCircle: CircleController,
  joinCircle: CircleController,
  showCircle: CircleController,
  getCertificate: CertificateController,
  claimCertificate: CertificateController,
  removeFromBazaar: BazaarController,
  bazaar: BazaarController,
  audioToText: AiAudioController,
  assimilate: AssimilationController,
  getAssimilationCount: AssimilationController,
  assimilating: AssimilationController,
  answerQuestion: AssessmentController,
  submitAssessmentResult: AssessmentController,
  generateAssessmentQuestions: AssessmentController,
  getMyAssessments: AssessmentController,
  suggestTitle: AiController,
  generateImage: AiController,
  dummyEntryToGenerateDataTypesThatAreRequiredInEventStream: AiController,
  getAvailableGptModels: AiController,
  updateNoteTitle: TextContentController,
  updateNoteDetails: TextContentController,
  searchWikidata: WikidataController,
  fetchWikidataEntityDataById: WikidataController,
  currentUserInfo: CurrentUserInfoController,
  recalling: RecallsController,
  overview: RecallsController,
  ping: HealthCheckController,
  dataUpgrade: HealthCheckController,
  failureReports: FailureReportController,
  showFailureReport: FailureReportController,
  deleteFailureReports: FailureReportController,
}

type SdkServiceName = keyof typeof methodToController
type SdkController<K extends SdkServiceName> = (typeof methodToController)[K]
type SdkService<K extends SdkServiceName> = SdkController<K>[K]
type SdkServiceReturnType<K extends SdkServiceName> = ReturnType<SdkService<K>>
type SdkServiceData<K extends SdkServiceName> =
  Awaited<SdkServiceReturnType<K>> extends {
    data: infer D
  }
    ? D
    : never
type SdkServiceOptions<K extends SdkServiceName> = Parameters<SdkService<K>>[0]

class StoredComponentTestHelper {
  component<T>(comp: T) {
    return new RenderingHelper(comp)
  }
}

/**
 * Mocks showNoteAccessory service to prevent unhandled promise rejections
 * in tests that use NoteAccessoryAsync component.
 */
export function mockShowNoteAccessory() {
  // biome-ignore lint/suspicious/noExplicitAny: showNoteAccessory returns undefined which requires special handling
  return mockSdkService("showNoteAccessory", undefined as any)
}

/**
 * Mocks showNote service to prevent unhandled promise rejections
 * in tests that use StoredApiCollection.loadNote (via storageAccessor).
 */
export function mockShowNote(noteRealm?: NoteRealm) {
  const defaultNote =
    noteRealm ||
    ({
      id: 1,
      note: { id: 1 },
      children: [],
    } as unknown as NoteRealm)
  return mockSdkService("showNote", defaultNote)
}

/**
 * Wraps data in the standard SDK response format.
 * Useful for updating mocks that need to return different values.
 */
export function wrapSdkResponse<T>(data: T) {
  return {
    data,
    error: undefined,
    request: {} as Request,
    response: {} as Response,
  } as {
    data: T
    error: undefined
    request: Request
    response: Response
  }
}

/**
 * Wraps an error in the standard SDK response format.
 * Useful for mocking error responses in tests.
 */
export function wrapSdkError(error: string | Record<string, unknown>) {
  return {
    data: undefined,
    error,
    request: {} as Request,
    response: {} as Response,
    // biome-ignore lint/suspicious/noExplicitAny: SDK response types are complex unions that require any for proper mocking
  } as any
}

/**
 * Type-safe helper to mock SDK service calls with resolved data.
 * Automatically wraps the response in the standard format.
 * Returns a spy that can be reconfigured with `.mockResolvedValue(wrapSdkResponse(newData))`.
 *
 * @param serviceName - The name of the SDK service to mock (type-safe)
 * @param data - The data value to return (type-safe based on service)
 * @returns A Vitest Mock that can be further configured
 *
 * @example
 * ```ts
 * // Simple usage
 * mockSdkService("getRecentNotes", [])
 *
 * // Reconfiguring the mock in tests
 * const spy = mockSdkService("showNote", makeMe.aNoteRealm.please())
 * spy.mockResolvedValue(wrapSdkResponse(differentNote))
 * ```
 */
export function mockSdkService<K extends SdkServiceName>(
  serviceName: K,
  data: SdkServiceData<K>
) {
  const controller = methodToController[serviceName]
  if (!controller) {
    throw new Error(`Unknown service: ${serviceName}`)
  }
  return (
    vi
      .spyOn(controller, serviceName)
      // biome-ignore lint/suspicious/noExplicitAny: SDK response types are complex unions that require any for proper mocking
      .mockResolvedValue(wrapSdkResponse(data) as any)
  )
}

/**
 * Type-safe helper to mock SDK service calls with a custom async implementation.
 * Automatically wraps the result in the standard format.
 *
 * @param serviceName - The name of the SDK service to mock (type-safe)
 * @param implementation - An async function that receives options and returns data
 * @returns A Vitest Mock that can be further configured
 *
 * @example
 * ```ts
 * const mockedCall = vi.fn()
 * mockSdkServiceWithImplementation("updateNoteDetails", async (options) => {
 *   const result = await mockedCall(options)
 *   return result
 * })
 * ```
 */
export function mockSdkServiceWithImplementation<K extends SdkServiceName>(
  serviceName: K,
  implementation: (
    options: SdkServiceOptions<K>
  ) => Promise<SdkServiceData<K>> | SdkServiceData<K>
) {
  const controller = methodToController[serviceName]
  if (!controller) {
    throw new Error(`Unknown service: ${serviceName}`)
  }
  // biome-ignore lint/suspicious/noExplicitAny: Vitest spy types are complex and require any for proper typing
  const spy = vi.spyOn(controller, serviceName) as any
  spy.mockImplementation(async (options: SdkServiceOptions<K>) => {
    const result = await implementation(options)
    return {
      data: result,
      error: undefined,
      request: {} as Request,
      response: {} as Response,
      // biome-ignore lint/suspicious/noExplicitAny: SDK response types are complex unions that require any for proper mocking
    } as any
  })
  return spy
}

export default new StoredComponentTestHelper()
export { matchByText }
