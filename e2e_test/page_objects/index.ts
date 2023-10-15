import {
  findQuestionWithStem,
  expectFeedbackRequiredMessage,
  currentQuestion,
} from "./QuizQuestionPage"
import { goToLastResult, answeredQuestionPage } from "./AnsweredQuestionPage"
import { chatAboutNotePage } from "./chatAboutNotePage"
import { adminDashboardPage } from "./adminPages/adminDashboardPage"
import mock_services from "./mock_services"
import { questionGenerationService } from "./questionGenerationService"
import { higherOrderActions } from "./higherOrderActions"

// jumptoNotePage is faster than navigateToNotePage
//    it uses the note id memorized when creating them with testability api
const jumpToNotePage = (noteTopic: string, forceLoadPage = false) => {
  cy.testability()
    .getSeededNoteIdByTitle(noteTopic)
    .then((noteId) => {
      const url = `/notes/${noteId}`
      if (forceLoadPage) cy.visit(url)
      else cy.routerPush(url, "noteShow", { noteId: noteId })
    })
  cy.findNoteTopic(noteTopic)

  return {
    startSearchingAndLinkNote() {
      cy.notePageButtonOnCurrentPage("search and link note").click()
    },
    aiGenerateImage: () => {
      cy.clickNotePageMoreOptionsButton(noteTopic, "Generate Image with DALL-E")
    },
  }
}

const chatAboutNote = (noteTopic: string) => {
  jumpToNotePage(noteTopic)
  cy.clickNotePageMoreOptionsButton(noteTopic, "chat about this note")
  return chatAboutNotePage()
}

const loginAsAdminAndGoToAdminDashboard = () => {
  cy.loginAs("admin")
  cy.reload()
  cy.openSidebar()
  cy.findByText("Admin Dashboard").click()
  return adminDashboardPage()
}

const pageObjects = {
  higherOrderActions,
  jumpToNotePage,
  questionGenerationService,
  answeredQuestionPage,
  goToLastResult,
  findQuestionWithStem,
  currentQuestion,
  expectFeedbackRequiredMessage,
  chatAboutNote,
  chatAboutNotePage,
  loginAsAdminAndGoToAdminDashboard,
}
export default pageObjects
export { mock_services }
