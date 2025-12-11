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

  stubQuestionByNoteType: (noteTypeToQuestion: Record<string, string>) => {
    cy.then(async () => {
      await mock_services.openAi().restartImposter()

      const noteTypeInstructionMap: Record<string, string> = {
        vocab: 'Special Instruction for Vocab Note',
        category: 'Special Instruction for Category Note',
        concept: 'Special Instruction for Concept Note',
        journal: 'Special Instruction for Journal Note',
      }

      const evaluationResponse = {
        feasibleQuestion: true,
        correctChoices: [0],
        improvementAdvices: '',
      }
      await mock_services
        .openAi()
        .chatCompletion()
        .requestMatches({
          messages: [
            {
              role: 'system',
              content: '.*evaluating a memory recall question.*',
            },
          ],
        })
        .stubQuestionGenerationWithBodyMatch(JSON.stringify(evaluationResponse))

      for (const [noteType, question] of Object.entries(noteTypeToQuestion)) {
        const instructionText = noteTypeInstructionMap[noteType.toLowerCase()]
        if (!instructionText) {
          throw new Error(`Unknown note type: ${noteType}`)
        }

        const mcqWithAnswer: McqWithAnswer = {
          f1__correctChoiceIndex: 0,
          f2__strictChoiceOrder: true,
          f0__multipleChoicesQuestion: {
            f0__stem: question,
            f1__choices: [
              'Correct Answer',
              'Incorrect Choice 1',
              'Incorrect Choice 2',
            ],
          },
        }
        const reply = JSON.stringify(mcqWithAnswer)

        await mock_services
          .openAi()
          .chatCompletion()
          .requestMatches({
            messages: [
              {
                role: 'system',
                content: `.*${instructionText}.*`,
              },
            ],
          })
          .stubQuestionGenerationWithBodyMatch(reply)
      }
    })
  },
})
