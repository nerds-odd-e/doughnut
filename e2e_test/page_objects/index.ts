import { findQuestionWithStem, currentQuestion } from "./QuizQuestionPage"
import { goToLastResult, answeredQuestionPage } from "./AnsweredQuestionPage"
import mock_services from "./mock_services"
import { chatAboutNotePage } from "./chatAboutNotePage"

const jumpToNotePage = (noteTitle: string) => {
  cy.jumpToNotePage(noteTitle)
  return {
    startChat() {
      cy.clickNotePageMoreOptionsButton(noteTitle, "chat about this note")
      return chatAboutNotePage()
    },
  }
}

const chatAboutNote = (noteTitle: string) => {
  return jumpToNotePage(noteTitle).startChat()
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
