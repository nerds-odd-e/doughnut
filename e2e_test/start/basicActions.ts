import { assumeAdminDashboardPage } from "./pageObjects/adminPages/adminDashboardPage"
import { assumeChatAboutNotePage } from "./pageObjects/chatAboutNotePage"
import { assumeNotePage } from "./pageObjects/notePage"
import { assumeAnsweredQuestionPage } from "./pageObjects/AnsweredQuestionPage"
import { assumeQuestionPage } from "./pageObjects/QuizQuestionPage"
import { assumeClarifyingQuestionDialog } from "./pageObjects/clarifyingQuestionDialog"
import testability from "./testability"

export default {
  assumeAnsweredQuestionPage,
  assumeChatAboutNotePage,
  assumeQuestionPage,
  assumeAdminDashboardPage,
  assumeClarifyingQuestionDialog,
  // jumptoNotePage is faster than navigateToNotePage
  //    it uses the note id memorized when creating them with testability api
  jumpToNotePage: (noteTopic: string, forceLoadPage = false) => {
    testability()
      .getSeededNoteIdByTitle(noteTopic)
      .then((noteId) => {
        const url = `/notes/${noteId}`
        if (forceLoadPage) cy.visit(url)
        else cy.routerPush(url, "noteShow", { noteId: noteId })
      })
    cy.findNoteTopic(noteTopic)

    return assumeNotePage()
  },

  loginAsAdmin: () => {
    cy.loginAs("admin")
  },

  goToAdminDashboard: () => {
    cy.reload()
    cy.openSidebar()
    cy.findByText("Admin Dashboard").click()
    return assumeAdminDashboardPage()
  },

  loginAsAdminAndGoToAdminDashboard() {
    this.loginAsAdmin()
    return this.goToAdminDashboard()
  },
}
