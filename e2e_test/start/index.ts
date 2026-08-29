import './clientConfig'
import { UserController } from '@generated/donut-backend-api/sdk.gen'
import mock_services from './mock_services/index'
import { waitUntilAppIsNotBusy as waitForAppNotBusy } from './pageBase'
import { questionGenerationService } from './questionGenerationService'
import type NotePath from '../support/NotePath'
import { navigateToNoteFromPath as openNoteFromPath } from './navigateNotePath'
import testability from './testability'
import mcpApi from './mcpApi'

// === Actions ===
import { loginActions } from './actions/loginActions'
import { navigationActions } from './actions/navigationActions'

// === Page Objects - Alphabetically Organized ===
import { assumeAdminDashboardPage } from './pageObjects/adminPages/adminDashboardPage'
import { assumeAnsweredQuestionPage } from './pageObjects/AnsweredQuestionPage'
import {
  assimilation,
  assumeAssimilationPage,
} from './pageObjects/assimilationPage'
import { assumeAudioTools } from './pageObjects/audioToolsPage'
import { assumeBazaarPage, navigateToBazaar } from './pageObjects/bazaarPage'
import { assumeCirclePage, navigateToCircle } from './pageObjects/circlePage'
import { assumeHomePage, visitHomePage } from './pageObjects/homePage'
import { assumeConversationAboutNotePage } from './pageObjects/conversationAboutNotePage'
import { assumeUserSettingsPage, mainMenu } from './pageObjects/mainMenu'
import { mcpAgentActions } from './pageObjects/mcpAgentActions'
import { messageCenterIndicator } from './pageObjects/messageCenterIndicator'
import {
  assumeMessageCenterPage,
  navigateToMessageCenter,
} from './pageObjects/messageCenterPage'
import {
  navigateToNotebookPage,
  navigateToNotebooksPage,
} from './pageObjects/notebooksPage'
import { notebookCard } from './pageObjects/notebookCard'
import { assumeNotePage } from './pageObjects/notePage'
import { noteSidebar } from './pageObjects/noteSidebar'
import { assumeNoteTargetSearchDialog } from './pageObjects/noteTargetSearchDialog'
import { assumeQuestionPage } from './pageObjects/QuizQuestionPage'
import { recall } from './pageObjects/recallPage'
import {
  recallStatsPage,
  visitRecallStatsPage,
} from './pageObjects/recallStatsPage'
import { form } from './forms'

const start = {
  // === Page Base ===
  waitUntilAppIsNotBusy() {
    waitForAppNotBusy()
    return this
  },

  // === Page Objects ===
  assimilation,
  assumeAdminDashboardPage,
  assumeAnsweredQuestionPage,
  assumeAssimilationPage,
  assumeAudioTools,
  assumeCirclePage,
  assumeHomePage,
  assumeUserSettingsPage,
  visitHomePage,
  assumeConversationAboutNotePage,
  assumeMessageCenterPage,
  assumeNotePage,
  assumeNoteTargetSearchDialog,
  assumeQuestionPage,
  mainMenu,
  mcpAgentActions,
  messageCenterIndicator,
  assumeBazaarPage,
  navigateToBazaar,
  navigateToCircle,
  navigateToMessageCenter,
  notebookCard,
  noteSidebar,
  recall,
  recallStatsPage,
  visitRecallStatsPage,
  navigateToNotebookPage,
  navigateToNotebooksPage,

  navigateToNoteFromPath(notePath: NotePath) {
    openNoteFromPath(notePath)
    return this
  },

  // === Services & Utilities ===
  form,
  questionGenerationService,
  testability,
  mcpApi,
  userController: () => UserController,

  // === Actions ===
  ...loginActions,
  ...navigationActions,
}

export default start
export { mock_services }
