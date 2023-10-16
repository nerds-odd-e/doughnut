import {
  findQuestionWithStem,
  expectFeedbackRequiredMessage,
  currentQuestion,
} from "./pageObjects/QuizQuestionPage"
import { goToLastResult, answeredQuestionPage } from "./pageObjects/AnsweredQuestionPage"
import { adminDashboardPage } from "./pageObjects/adminPages/adminDashboardPage"
import mock_services from "./mock_services"
import { questionGenerationService } from "./questionGenerationService"
import { higherOrderActions } from "./higherOrderActions"
import basicActions from "./basicActions"

const loginAsAdminAndGoToAdminDashboard = () => {
  cy.loginAs("admin")
  cy.reload()
  cy.openSidebar()
  cy.findByText("Admin Dashboard").click()
  return adminDashboardPage()
}

const start = {
  ...basicActions,
  higherOrderActions,
  questionGenerationService,
  answeredQuestionPage,
  goToLastResult,
  findQuestionWithStem,
  currentQuestion,
  expectFeedbackRequiredMessage,
  loginAsAdminAndGoToAdminDashboard,
}
export default start
export { mock_services }
