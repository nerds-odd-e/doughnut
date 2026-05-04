import type { McqWithAnswer } from '@generated/doughnut-backend-api/types.gen'
import mock_services from './mock_services'

const createMcqWithAnswer = (
  stem: string,
  correctChoice: string,
  incorrectChoice1: string,
  incorrectChoice2: string
): McqWithAnswer => ({
  f1__correctChoiceIndex: 0,
  f2__strictChoiceOrder: true,
  f0__multipleChoicesQuestion: {
    f0__stem: stem,
    f1__choices: [correctChoice, incorrectChoice1, incorrectChoice2],
  },
})

export const questionGenerationService = () => ({
  resetAndStubAskingMCQByChatCompletion: (record: Record<string, string>) => {
    const mcqWithAnswer = createMcqWithAnswer(
      record['Question Stem']!,
      record['Correct Choice']!,
      record['Incorrect Choice 1']!,
      record['Incorrect Choice 2']!
    )
    const reply = JSON.stringify(mcqWithAnswer)
    cy.then(async () => {
      await mock_services.openAi().restartImposter()
      await mock_services
        .openAi()
        .chatCompletion()
        .requestMessageMatches({ role: 'user', content: 'Memory Assistant' })
        .stubQuestionGeneration(reply)
    })
  },

  resetAndStubAskingMCQWhenPromptContainsDepthTwoFarNote: (
    record: Record<string, string>
  ) => {
    const mcqWithAnswer = createMcqWithAnswer(
      record['Question Stem']!,
      record['Correct Choice']!,
      record['Incorrect Choice 1']!,
      record['Incorrect Choice 2']!
    )
    const reply = JSON.stringify(mcqWithAnswer)
    cy.then(async () => {
      await mock_services.openAi().restartImposter()
      await mock_services
        .openAi()
        .chatCompletion()
        .requestMessageMatches({
          role: 'user',
          content:
            '[\\s\\S]*Title: FarDepthTwo[\\s\\S]*Path:[\\s\\S]*->[\\s\\S]*->[\\s\\S]*Reached by: OutgoingWikiLink[\\s\\S]*',
        })
        .stubJsonSchemaResponse(reply)
    })
  },

  resetAndStubAskingMCQWhenPromptContainsRetrievedNote: (
    record: Record<string, string>
  ) => {
    const mcqWithAnswer = createMcqWithAnswer(
      record['Question Stem']!,
      record['Correct Choice']!,
      record['Incorrect Choice 1']!,
      record['Incorrect Choice 2']!
    )
    const reply = JSON.stringify(mcqWithAnswer)
    cy.then(async () => {
      await mock_services.openAi().restartImposter()
      await mock_services
        .openAi()
        .chatCompletion()
        .requestMessageMatches({
          role: 'user',
          content: '.*Reached by: OutgoingWikiLink.*',
        })
        .stubJsonSchemaResponse(reply)
    })
  },

  stubEvaluationQuestion: (
    record: Record<string, boolean | string | number[]>
  ) => {
    cy.then(async () => {
      await mock_services
        .openAi()
        .chatCompletion()
        .requestMessageMatches({ role: 'user', content: 'Memory Assistant' })
        .stubQuestionEvaluation(JSON.stringify(record))
    })
  },
})
