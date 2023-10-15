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

const higherOrderActions = () => {
  return {
    stubOpenAIQuestionGenerationAndSeeTheQuestionSimple(questionStem: string) {
      const noteTopic = `A note discussing "${questionStem}"`
      return this.stubOpenAIQuestionGenerationAndSeeTheQuestion(noteTopic, {
        "Question Stem": questionStem,
        "Correct Choice": "True",
        "Incorrect Choice 1": "False",
      })
    },

    stubOpenAIQuestionGenerationAndSeeTheQuestion: (
      noteTopic: string,
      question: Record<string, string>,
    ) => {
      cy.testability().seedNotes([{ topic: noteTopic }])
      questionGenerationService().stubAskSingleAnswerMultipleChoiceQuestion(question)
      chatAboutNote(noteTopic).testMe()
    },
  }
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
