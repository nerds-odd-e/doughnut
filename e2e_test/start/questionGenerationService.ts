import { MCQWithAnswer } from '../../frontend/src/generated/backend/models/MCQWithAnswer'
import mock_services from './mock_services'

export const questionGenerationService = () => ({
  resetAndStubAskingMCQ: (record: Record<string, string>) => {
    const mcqWithAnswer: MCQWithAnswer = {
      correctChoiceIndex: 0,
      multipleChoicesQuestion: {
        stem: record['Question Stem'],
        choices: [
          record['Correct Choice']!,
          record['Incorrect Choice 1']!,
          record['Incorrect Choice 2']!,
        ],
      },
    }

    cy.then(async () => {
      await mock_services.openAi().restartImposter()
      await mock_services
        .openAi()
        .stubCreateThread('thread-123')
        .stubCreateRuns('thread-123', ['run-123'])
        .aRun('run-123')
        .stubRetrieveRunsThatRequireAction([
          {
            response: 'ask_single_answer_multiple_choice_question',
            arguments: JSON.stringify(mcqWithAnswer),
          },
        ])
      mock_services.openAi().stubRunCancellation('thread-123')
    })
  },
  stubEvaluationQuestion: (
    record: Record<string, boolean | string | number[]>
  ) => {
    cy.then(async () => {
      await mock_services
        .openAi()
        .chatCompletion()
        .requestMessageMatches({
          role: 'user',
          content: '.*critically check.*',
        })
        .stubQuestionEvaluation(JSON.stringify(record))
    })
  },
})
