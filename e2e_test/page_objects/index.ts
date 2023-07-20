import { findQuestionWithStem, currentQuestion } from "./QuizQuestionPage"

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
  findQuestionWithStem,
  currentQuestion,
  askQuestionForNote,
}
export default PageObjects
