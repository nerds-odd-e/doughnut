import { questionGenerationService } from "./questionGenerationService"
import { chatAboutNotePage } from "./chatAboutNotePage"

const chatAboutNote = (noteTopic: string) => {
  cy.jumpToNotePage(noteTopic)
  cy.clickNotePageMoreOptionsButton(noteTopic, "chat about this note")
  return chatAboutNotePage()
}

export const higherOrderActions = () => {
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
