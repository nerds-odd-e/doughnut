import basicActions from './basicActions'
import { questionGenerationService } from './questionGenerationService'
import testability from './testability'

export const higherOrderActions = {
  stubOpenAIQuestionGenerationAndSeeTheQuestion: (
    noteTopic: string,
    question: Record<string, string>
  ) => {
    testability().injectNotes([{ Topic: noteTopic }])
    questionGenerationService().resetAndStubAskingMCQ(question)
    return basicActions.jumpToNotePage(noteTopic).testMe()
  },
}
