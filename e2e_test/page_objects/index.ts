import { findQuestionWithStem, currentQuestion } from "./QuizQuestionPage"
import { goToLastResult, answeredQuestionPage } from "./AnsweredQuestionPage"
import mock_services from "./mock_services"
import { chatAboutNotePage } from "./chatAboutNotePage"
import { adminDashboardPage } from "./adminDashboardPage"

const jumpToNotePage = (noteTopic: string, customModel?: string, temperature?: number) => {
  cy.jumpToNotePage(noteTopic)
  return {
    startChat() {
      cy.clickNotePageMoreOptionsButton(noteTopic, "chat about this note")
      const page = chatAboutNotePage()
      if (customModel) {
        page.setCustomModel(customModel)
      }
      if (temperature !== undefined) {
        page.setTemperature(temperature)
      }
      return page
    },
  }
}

const chatAboutNote = (noteTopic: string, customModel?: string, temperature?: number) => {
  return jumpToNotePage(noteTopic, customModel, temperature).startChat()
}

const findCustomModelInput = () => {
  return {
    isNotPresent() {
      cy.get(".custom-model-input input").should("not.exist")
    },
  }
}

const findErrorMessage = (errorMessage: string) => {
  return cy.findByText(errorMessage).should("be.visible")
}

const loginAsAdminAndGoToAdminDashboard = () => {
  cy.visit("/dev-training-data")
  return adminDashboardPage()
}

const pageObjects = {
  answeredQuestionPage,
  goToLastResult,
  findQuestionWithStem,
  currentQuestion,
  chatAboutNote,
  chatAboutNotePage,
  findCustomModelInput,
  findErrorMessage,
  loginAsAdminAndGoToAdminDashboard,
}
export default pageObjects
export { mock_services }
