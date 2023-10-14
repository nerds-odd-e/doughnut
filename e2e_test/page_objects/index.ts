import {
  findQuestionWithStem,
  expectFeedbackRequiredMessage,
  expectSuccessMessageToBeShown,
  expectFeedbackAlreadyExistMessage,
  currentQuestion,
} from "./QuizQuestionPage"
import { goToLastResult, answeredQuestionPage } from "./AnsweredQuestionPage"
import { chatAboutNotePage } from "./chatAboutNotePage"
import { adminDashboardPage } from "./adminPages/adminDashboardPage"
import { openAiService } from "./openAiService"
import mock_services from "./mock_services"

const chatAboutNote = (noteTopic: string) => {
  cy.jumpToNotePage(noteTopic)
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
  openAiService,
  answeredQuestionPage,
  goToLastResult,
  findQuestionWithStem,
  currentQuestion,
  expectSuccessMessageToBeShown,
  expectFeedbackAlreadyExistMessage,
  expectFeedbackRequiredMessage,
  chatAboutNote,
  chatAboutNotePage,
  loginAsAdminAndGoToAdminDashboard,
}
export default pageObjects
export { mock_services }
