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
  higherOrderActions,
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
