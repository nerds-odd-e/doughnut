import { findQuestionWithStem, currentQuestion } from "./QuizQuestionPage"
import { goToLastResult, answeredQuestionPage } from "./AnsweredQuestionPage"
import mock_services from "./mock_services"

const jumpToNotePage = (noteTitle: string) => {
  cy.jumpToNotePage(noteTitle)
  return {
    startChat() {
      cy.clickNotePageMoreOptionsButton(noteTitle, "chat about this note")
      return {
        testMe() {
          cy.findByRole("button", { name: "Test me" }).click()
        },
        findQuestionWithStem,
      }
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
}
export default pageObjects
export { mock_services }
