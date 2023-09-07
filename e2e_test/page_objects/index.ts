import { findQuestionWithStem, currentQuestion } from "./QuizQuestionPage"
import { goToLastResult, answeredQuestionPage } from "./AnsweredQuestionPage"
import mock_services from "./mock_services"
import { chatAboutNotePage } from "./chatAboutNotePage"

const jumpToNotePage = (noteTopic: string, customModel?: string) => {
  cy.jumpToNotePage(noteTopic)
  return {
    startChat() {
      cy.clickNotePageMoreOptionsButton(noteTopic, "chat about this note")
      const page = chatAboutNotePage()
      if (customModel) {
        page.setCustomModel(customModel)
      }
      return page
    },
  }
}

const chatAboutNote = (noteTopic: string, customModel?: string) => {
  return jumpToNotePage(noteTopic, customModel).startChat()
}

const findCustomModelInput = () => {
  return {
    isNotPresent() {
      cy.get(".custom-model-input input").should("not.exist")
    },
  }
}

const pageObjects = {
  answeredQuestionPage,
  goToLastResult,
  findQuestionWithStem,
  currentQuestion,
  chatAboutNote,
  chatAboutNotePage,
  findCustomModelInput,
}
export default pageObjects
export { mock_services }
