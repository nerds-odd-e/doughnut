import type { MCQWithAnswer } from '@generated/backend/models/MCQWithAnswer'
import mock_services from './mock_services'

export const questionGenerationService = () => ({
  resetAndStubAskingMCQByChatCompletion: (record: Record<string, string>) => {
    const mcqWithAnswer: MCQWithAnswer = {
      correctChoiceIndex: 0,
      strictChoiceOrder: true,
      multipleChoicesQuestion: {
        stem: record['Question Stem']!,
        choices: [
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
