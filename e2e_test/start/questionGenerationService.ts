import type { MCQWithAnswer } from '../../frontend/src/generated/backend/models/MCQWithAnswer'
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

  stubAskingMCQ: (threadId: string, record: Record<string, string>) => {
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

    cy.then(async () => {
      await mock_services
        .openAi()
        .stubCreateRuns(threadId, ['run-123'])
        .aRun('run-123')
        .stubRetrieveRunsThatRequireAction([
          {
            response: 'ask_single_answer_multiple_choice_question',
            arguments: JSON.stringify(mcqWithAnswer),
          },
        ])
      mock_services.openAi().stubRunCancellation(threadId, 'run-123')
    })
  },

  stubEvaluationQuestion: (
    threadId: string,
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
