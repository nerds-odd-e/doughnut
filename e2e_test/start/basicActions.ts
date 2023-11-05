import { assumeAdminDashboardPage } from "./pageObjects/adminPages/adminDashboardPage"
import { assumeChatAboutNotePage } from "./pageObjects/chatAboutNotePage"
import { assumeNotePage } from "./pageObjects/notePage"
import { assumeAnsweredQuestionPage } from "./pageObjects/AnsweredQuestionPage"
import { assumeQuestionPage } from "./pageObjects/QuizQuestionPage"

export default {
  assumeAnsweredQuestionPage,
  assumeChatAboutNotePage,
  assumeQuestionPage,
  assumeAdminDashboardPage,
  // jumptoNotePage is faster than navigateToNotePage
  //    it uses the note id memorized when creating them with testability api
  jumpToNotePage: (noteTopic: string, forceLoadPage = false) => {
    start
      .testability()
      .getSeededNoteIdByTitle(noteTopic)
      .then((noteId) => {
        const url = `/notes/${noteId}`
        if (forceLoadPage) cy.visit(url)
        else cy.routerPush(url, "noteShow", { noteId: noteId })
      })
    cy.findNoteTopic(noteTopic)

    return assumeNotePage()
  },
  loginAsAdminAndGoToAdminDashboard: () => {
    cy.loginAs("admin")
    cy.reload()
    cy.openSidebar()
    cy.findByText("Admin Dashboard").click()
    return assumeAdminDashboardPage()
  },
}
