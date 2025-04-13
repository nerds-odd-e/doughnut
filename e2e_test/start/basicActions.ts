import { assumeAnsweredQuestionPage } from './pageObjects/AnsweredQuestionPage'
import { assumeAssessmentPage } from './pageObjects/AssessmentPage'
import { assumeQuestionPage } from './pageObjects/QuizQuestionPage'
import { assumeAdminDashboardPage } from './pageObjects/adminPages/adminDashboardPage'
import { assumeMessageCenterPage } from './pageObjects/messageCenterPage'
import { navigateToBazaar } from './pageObjects/bazaarPage'
import { assumeConversationAboutNotePage } from './pageObjects/conversationAboutNotePage'
import { navigateToCircle } from './pageObjects/circlePage'
import { assumeNotePage } from './pageObjects/notePage'
import { routerToMyNotebooksPage } from './pageObjects/myNotebooksPage'
import { noteSidebar } from './pageObjects/noteSidebar'
import { mainMenu } from './pageObjects/mainMenu'
import { navigateToMessageCenter } from './pageObjects/messageCenterPage'
import testability from './testability'
import { logins } from './logins'
import { messageCenterIndicator } from './pageObjects/messageCenterIndicator'
import { assumeAudioTools } from './pageObjects/audioToolsPage'
import { assumeNoteTargetSearchDialog } from './pageObjects/noteTargetSearchDialog'
import { navigateToMyCircles } from './pageObjects/myCirclesPage'

export default {
  navigateToBazaar,
  noteSidebar,
  mainMenu,
  assumeNotePage,
  assumeAudioTools,
  assumeAssessmentPage,
  assumeAnsweredQuestionPage,
  assumeConversationAboutNotePage,
  assumeQuestionPage,
  assumeAdminDashboardPage,
  assumeMessageCenterPage,
  assumeNoteTargetSearchDialog,
  routerToNotebooksPage: routerToMyNotebooksPage,
  navigateToCircle,
  navigateToMessageCenter,
  ...logins,

  // jumptoNotePage is faster than navigateToPage
  //    it uses the note id memorized when creating them with testability api
  jumpToNotePage: (noteTopology: string, forceLoadPage = false) => {
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
  loginAsAdminAndGoToAdminDashboard() {
    this.reloginAsAdmin()
    return this.goToAdminDashboard()
  },
  messageCenterIndicator,
  navigateToMyCircles,
  importFromJson(filePath: string) {
    cy.get('input[type="file"]').selectFile(filePath, { force: true })
    cy.get('button').contains('Import').click()
  },
  importAllFromJson(filePath: string) {
    cy.get('input[type="file"]').selectFile(filePath, { force: true })
    cy.get('button').contains('Import All').click()
  }
}
