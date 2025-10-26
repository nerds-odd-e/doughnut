import mock_services from './mock_services/index'
import { questionGenerationService } from './questionGenerationService'
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
import { routerToMyNotebooksPage } from './pageObjects/myNotebooksPage'
import { notebookCard } from './pageObjects/notebookCard'
import { assumeNotePage } from './pageObjects/notePage'
import { noteSidebar } from './pageObjects/noteSidebar'
import { assumeNoteTargetSearchDialog } from './pageObjects/noteTargetSearchDialog'
import { assumeQuestionPage } from './pageObjects/QuizQuestionPage'
import { recall } from './pageObjects/recallPage'

const start = {
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
  routerToNotebooksPage: routerToMyNotebooksPage,

  // === Services & Utilities ===
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
