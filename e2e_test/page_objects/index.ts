import { findQuestionWithStem, currentQuestion } from "./QuizQuestionPage"
import { goToLastResult, answeredQuestionPage } from "./AnsweredQuestionPage"
import mock_services from "./mock_services"
import { chatAboutNotePage } from "./chatAboutNotePage"

const jumpToNotePage = (noteTopic: string) => {
  cy.jumpToNotePage(noteTopic)
  return {
    startChat() {
      cy.clickNotePageMoreOptionsButton(noteTopic, "chat about this note")
      return chatAboutNotePage()
    },
  }
}

const chatAboutNote = (noteTopic: string) => {
  return jumpToNotePage(noteTopic).startChat()
}

const pageObjects = {
  answeredQuestionPage,
  goToLastResult,
  findQuestionWithStem,
  currentQuestion,
  chatAboutNote,
  chatAboutNotePage,
}
export default pageObjects
export { mock_services }
