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
import { jumpToNotePage } from "./jumpToNotePage"

const loginAsAdminAndGoToAdminDashboard = () => {
  cy.loginAs("admin")
  cy.reload()
  cy.openSidebar()
  cy.findByText("Admin Dashboard").click()
  return adminDashboardPage()
}

const start = {
  higherOrderActions,
  jumpToNotePage,
  questionGenerationService,
  answeredQuestionPage,
  goToLastResult,
  findQuestionWithStem,
  currentQuestion,
  expectFeedbackRequiredMessage,
  chatAboutNotePage,
  loginAsAdminAndGoToAdminDashboard,
}
export default start
export { mock_services }
