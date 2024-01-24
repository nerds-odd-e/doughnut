import { questionGenerationService } from "./questionGenerationService"
import basicActions from "./basicActions"
import testability from "./testability"

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
    testability().seedNotes([{ topicConstructor: noteTopic }])
    questionGenerationService().resetAndStubAskingMCQ(question)
    return basicActions.jumpToNotePage(noteTopic).chatAboutNote().testMe()
  },
}
