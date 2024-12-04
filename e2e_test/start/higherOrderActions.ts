import basicActions from './basicActions'
import { questionGenerationService } from './questionGenerationService'
import testability from './testability'

export const higherOrderActions = {
  stubOpenAIQuestionGenerationAndSeeTheQuestion: (
    noteTitle: string,
    question: Record<string, string>
  ) => {
    testability().injectNotes([{ Title: noteTitle }])
    questionGenerationService().resetAndStubAskingMCQ(question)
    return basicActions.jumpToNotePage(noteTitle).testMe()
  },
}
