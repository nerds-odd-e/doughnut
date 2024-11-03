import { assumeAnsweredQuestionPage } from './pageObjects/AnsweredQuestionPage'
import { assumeAssessmentPage } from './pageObjects/AssessmentPage'
import { assumeQuestionPage } from './pageObjects/QuizQuestionPage'
import { assumeAdminDashboardPage } from './pageObjects/adminPages/adminDashboardPage'
import { assumeMessageCenterPage } from './pageObjects/messageCenterPage'
import { navigateToBazaar } from './pageObjects/bazaarPage'
import { assumeConversationAboutNotePage } from './pageObjects/conversationAboutNotePage'
import { navigateToCircle } from './pageObjects/circlePage'
import { assumeNotePage } from './pageObjects/notePage'
import { routerToNotebooksPage } from './pageObjects/notebooksPage'
import { noteSidebar } from './pageObjects/noteSidebar'
import { systemSidebar } from './pageObjects/systemSidebar'
import { navigateToMessageCenter } from './pageObjects/messageCenterPage'
import testability from './testability'
import { logins } from './logins'
import { messageCenterIndicator } from './pageObjects/messageCenterIndicator'
import { assumeAudioTools } from './pageObjects/audioToolsPage'

export default {
  navigateToBazaar,
  noteSidebar,
  systemSidebar,
  assumeNotePage,
  assumeAudioTools,
  assumeAssessmentPage,
  assumeAnsweredQuestionPage,
  assumeConversationAboutNotePage,
  assumeQuestionPage,
  assumeAdminDashboardPage,
  assumeMessageCenterPage,
  routerToNotebooksPage,
  navigateToCircle,
  navigateToMessageCenter,
  ...logins,

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
  navigateToAssessmentAndCertificatePage() {
    return systemSidebar().userOptions().myAssessmentAndCertificateHistory()
  },
  loginAsAdminAndGoToAdminDashboard() {
    this.reloginAsAdmin()
    return this.goToAdminDashboard()
  },
  messageCenterIndicator,
}
