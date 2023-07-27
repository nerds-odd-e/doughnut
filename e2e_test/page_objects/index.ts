import { findQuestionWithStem, currentQuestion } from "./QuizQuestionPage"
import { goToLastResult, answeredQuestionPage } from "./AnsweredQuestionPage"
import mock_services from "./mock_services"

const jumpToNotePage = (noteTitle: string) => {
  cy.jumpToNotePage(noteTitle)
  return {
    askQuestion() {
      cy.clickNotePageMoreOptionsButton(noteTitle, "test me")
      return {
        findQuestionWithStem,
      }
    },
  }
}
const askQuestionForNote = (noteTitle: string) => {
  return jumpToNotePage(noteTitle).askQuestion()
}

const pageObjects = {
  answeredQuestionPage,
  goToLastResult,
  findQuestionWithStem,
  currentQuestion,
  askQuestionForNote,
}
export default pageObjects
export { mock_services }
