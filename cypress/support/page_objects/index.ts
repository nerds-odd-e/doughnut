import { questionWithStem } from "./QuizQuestionPage"

const askQuestionForNote = (noteTitle: string) => {
  cy.jumpToNotePage(noteTitle)
  cy.clickNotePageMoreOptionsButton(noteTitle, "test me")
  return {
    questionWithStem,
  }
}

const PageObjects = {
  questionWithStem,
  askQuestionForNote,
}
export default PageObjects
