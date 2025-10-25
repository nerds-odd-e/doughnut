import mock_services from './mock_services/index'
import { questionGenerationService } from './questionGenerationService'
import testability from './testability'
import mcpApi from './mcpApi'

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

// === Type Exports ===
export interface ExpectedFile {
  Filename: string
  Format: string
  Content: string
  validateMetadata?: boolean
}

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

  // === Login Methods ===
  logout() {
    cy.pageIsNotLoading()
    cy.request({
      method: 'POST',
      url: '/logout',
    }).then((response) => {
      expect(response.status).to.equal(204)
    })
    cy.pageIsNotLoading()
    return this
  },

  loginAs(username: string) {
    if (username === 'none') {
      this.logout()
      return start
    }

    const password = 'password'
    const token = btoa(`${username}:${password}`)
    cy.request({
      method: 'GET',
      url: '/api/healthcheck',
      headers: {
        Authorization: `Basic ${token}`,
      },
    }).then((response) => {
      expect(response.status).to.equal(200)
    })

    return start
  },

  reloginAs(username: string) {
    return this.logout().loginAs(username)
  },

  reloginAndEnsureHomePage(username: string) {
    const result = this.reloginAs(username)
    cy.visit('/')
    return result
  },

  loginAsAdmin() {
    return this.loginAs('admin')
  },

  reloginAsAdmin() {
    return this.logout().loginAsAdmin()
  },

  goToAdminDashboard() {
    cy.reload()
    return mainMenu().adminDashboard()
  },

  loginAsAdminAndGoToAdminDashboard() {
    this.reloginAsAdmin()
    return this.goToAdminDashboard()
  },

  // === Navigation & Actions ===
  // jumpToNotePage is faster than navigateToPage
  //   it uses the note id memorized when creating them with testability api
  jumpToNotePage(noteTopology: string, forceLoadPage = false) {
    testability()
      .getInjectedNoteIdByTitle(noteTopology)
      .then((noteId) => {
        const url = `/n${noteId}`
        if (forceLoadPage) cy.visit(url)
        else cy.routerPush(url, 'noteShow', { noteId: noteId })
      })

    return assumeNotePage(noteTopology)
  },

  navigateToAssessmentAndCertificatePage() {
    return mainMenu().userOptions().myAssessmentAndCertificateHistory()
  },

  // === Download Utilities ===
  checkDownloadFiles() {
    return {
      hasZipFileWith(files: ExpectedFile[]) {
        return cy.task('checkDownloadedZipContent', files)
      },
    }
  },
}

export default start
export { mock_services }
