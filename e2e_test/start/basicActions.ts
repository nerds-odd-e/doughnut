import { assumeAnsweredQuestionPage } from './pageObjects/AnsweredQuestionPage'
import {
  assumeAssessmentPage,
  assumeAssessmentResultPage,
} from './pageObjects/AssessmentPage'
import { assumeQuestionPage } from './pageObjects/QuizQuestionPage'
import { assumeAdminDashboardPage } from './pageObjects/adminPages/adminDashboardPage'
import { navigateToBazaar } from './pageObjects/bazaarPage'
import { assumeChatAboutNotePage } from './pageObjects/chatAboutNotePage'
import { navigateToCircle } from './pageObjects/circlePage'
import { navigateToAssessmentHistory } from './pageObjects/AssessmentHistoryPage'
import { assumeClarifyingQuestionDialog } from './pageObjects/clarifyingQuestionDialog'
import { assumeNotePage } from './pageObjects/notePage'
import { routerToNotebooksPage } from './pageObjects/notebooksPage'
import { sidebar } from './pageObjects/sidebar'
import testability from './testability'

export default {
  navigateToBazaar,
  navigateToAssessmentHistory,
  sidebar,
  assumeNotePage,
  assumeAssessmentPage,
  assumeAssessmentResultPage,
  assumeAnsweredQuestionPage,
  assumeChatAboutNotePage,
  assumeQuestionPage,
  assumeAdminDashboardPage,
  assumeClarifyingQuestionDialog,
  routerToNotebooksPage,
  navigateToCircle,
  // jumptoNotePage is faster than navigateToPage
  //    it uses the note id memorized when creating them with testability api
  jumpToNotePage: (noteTopic: string, forceLoadPage = false) => {
    testability()
      .getInjectedNoteIdByTitle(noteTopic)
      .then((noteId) => {
        const url = `/n${noteId}`
        if (forceLoadPage) cy.visit(url)
        else cy.routerPush(url, 'noteShow', { noteId: noteId })
      })

    return assumeNotePage(noteTopic)
  },

  loginAsAdmin: () => {
    cy.logout()
    cy.loginAs('admin')
  },

  goToAdminDashboard: () => {
    cy.reload()
    cy.openSidebar()
    cy.findByText('Admin Dashboard').click()
    return assumeAdminDashboardPage()
  },

  loginAsAdminAndGoToAdminDashboard() {
    this.loginAsAdmin()
    return this.goToAdminDashboard()
  },
}
