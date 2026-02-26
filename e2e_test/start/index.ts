import './clientConfig'
import mock_services from './mock_services/index'
import { questionGenerationService } from './questionGenerationService'
import router from './router'
import testability from './testability'
import mcpApi from './mcpApi'

// === Actions ===
import { loginActions } from './actions/loginActions'
import { navigationActions } from './actions/navigationActions'
import { downloadActions, type ExpectedFile } from './actions/downloadActions'

// === Page Objects - Alphabetically Organized ===
import { assumeAdminDashboardPage } from './pageObjects/adminPages/adminDashboardPage'
import { assumeAnsweredQuestionPage } from './pageObjects/AnsweredQuestionPage'
import {
  assimilation,
  assumeAssimilationPage,
} from './pageObjects/assimilationPage'
import { assumeAssessmentPage } from './pageObjects/AssessmentPage'
import { assumeAudioTools } from './pageObjects/audioToolsPage'
import { navigateToBazaar } from './pageObjects/bazaarPage'
import { assumeCirclePage, navigateToCircle } from './pageObjects/circlePage'
import { assumeConversationAboutNotePage } from './pageObjects/conversationAboutNotePage'
import { mainMenu } from './pageObjects/mainMenu'
import { mcpAgentActions } from './pageObjects/mcpAgentActions'
import { messageCenterIndicator } from './pageObjects/messageCenterIndicator'
import {
  assumeMessageCenterPage,
  navigateToMessageCenter,
} from './pageObjects/messageCenterPage'
import { navigateToMyCircles } from './pageObjects/myCirclesPage'
import { navigateToNotebooksPage } from './pageObjects/myNotebooksPage'
import { notebookCard } from './pageObjects/notebookCard'
import { assumeNotePage } from './pageObjects/notePage'
import { noteSidebar } from './pageObjects/noteSidebar'
import { assumeNoteTargetSearchDialog } from './pageObjects/noteTargetSearchDialog'
import { assumeQuestionPage } from './pageObjects/QuizQuestionPage'
import { recall } from './pageObjects/recallPage'
import { form } from './forms'

const start = {
  // === Router ===
  toRoot: () => router().toRoot(),
  routerPush: (
    fallback: string,
    name: string,
    params: Record<string, string | number>
  ) => router().push(fallback, name, params),

  // === Page Objects ===
  assimilation,
  assumeAdminDashboardPage,
  assumeAnsweredQuestionPage,
  assumeAssessmentPage,
  assumeAssimilationPage,
  assumeAudioTools,
  assumeCirclePage,
  assumeConversationAboutNotePage,
  assumeMessageCenterPage,
  assumeNotePage,
  assumeNoteTargetSearchDialog,
  assumeQuestionPage,
  mainMenu,
  mcpAgentActions,
  messageCenterIndicator,
  navigateToBazaar,
  navigateToCircle,
  navigateToMessageCenter,
  navigateToMyCircles,
  notebookCard,
  noteSidebar,
  recall,
  navigateToNotebooksPage,

  // === Services & Utilities ===
  form,
  questionGenerationService,
  testability,
  mcpApi,

  // === Actions ===
  ...loginActions,
  ...navigationActions,
  ...downloadActions,
}

export default start
export { mock_services }
export type { ExpectedFile }
