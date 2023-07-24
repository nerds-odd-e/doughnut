import { findQuestionWithStem, currentQuestion } from "./QuizQuestionPage"
import { goToLastResult, answeredQuestionPage } from "./AnsweredQuestionPage"

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

const PageObjects = {
  answeredQuestionPage,
  goToLastResult,
  findQuestionWithStem,
  currentQuestion,
  askQuestionForNote,
}
export default PageObjects
