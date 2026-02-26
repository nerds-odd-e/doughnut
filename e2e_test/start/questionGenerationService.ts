import type { McqWithAnswer } from '@generated/backend/types.gen'
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
        concept: 'Special Instruction for Concept Note',
        source: 'Special Instruction for Source Note',
        person: 'Special Instruction for Person Note',
        experience: 'Special Instruction for Experience Note',
        initiative: 'Special Instruction for Initiative Note',
        quest: 'Special Instruction for Quest Note',
      }

      for (const [noteType, question] of Object.entries(noteTypeToQuestion)) {
        const instructionText = noteTypeInstructionMap[noteType.toLowerCase()]
        if (!instructionText) {
          throw new Error(`Unknown note type: ${noteType}`)
        }

        const mcqWithAnswer = createMcqWithAnswer(
          question,
          'Correct Answer',
          'Incorrect Choice 1',
          'Incorrect Choice 2'
        )
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
          .stubJsonSchemaResponse(reply)
      }
    })
  },
})
