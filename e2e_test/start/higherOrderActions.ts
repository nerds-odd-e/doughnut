import { questionGenerationService } from "./questionGenerationService"
import basicActions from "./basicActions"

export const higherOrderActions = {
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
    return basicActions.jumpToNotePage(noteTopic).chatAboutNote().testMe()
  },
}
