import type { McqWithAnswer } from '@generated/backend/types.gen'
import mock_services from './mock_services'

export const questionGenerationService = () => ({
  resetAndStubAskingMCQByChatCompletion: (record: Record<string, string>) => {
    const mcqWithAnswer: McqWithAnswer = {
      f1__correctChoiceIndex: 0,
      f2__strictChoiceOrder: true,
      f0__multipleChoicesQuestion: {
        f0__stem: record['Question Stem']!,
        f1__choices: [
          record['Correct Choice']!,
          record['Incorrect Choice 1']!,
          record['Incorrect Choice 2']!,
        ],
      },
    }
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
