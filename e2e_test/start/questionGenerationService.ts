import mock_services from './mock_services'

/** Shape of JSON returned by OpenAI tool calls for MCQ (not all fields are in OpenAPI). */
type GeneratedMcq = {
  questionStem: string
  correctAnswer: string
  distractors: string[]
  testedFocus?: string
  validationRationale?: string
}

const createGeneratedMcq = (
  stem: string,
  correctChoice: string,
  incorrectChoice1: string,
  incorrectChoice2: string,
  incorrectChoice3: string
): GeneratedMcq => ({
  questionStem: stem,
  correctAnswer: correctChoice,
  distractors: [incorrectChoice1, incorrectChoice2, incorrectChoice3],
})

const mcqReplyJson = (record: Record<string, string>) =>
  JSON.stringify(
    createGeneratedMcq(
      record['Question Stem']!,
      record['Correct Choice']!,
      record['Incorrect Choice 1']!,
      record['Incorrect Choice 2']!,
      record['Incorrect Choice 3']!
    )
  )

/** Question Designer / Memory Assistant MCQ stubs (excludes contest regeneration). */
const stubQuestionDesignerMcqOutput = async (outputText: string) => {
  await mock_services
    .openAi()
    .responses()
    .requestMessageMatches({
      role: 'developer',
      content: 'Question Designer|Memory Assistant',
    })
    .requestDoesNotMessageMatch({
      role: 'user',
      content: 'Previously generated question, contested by the learner:',
    })
    .stubOutputText(outputText)
}

/** The contest check: which choices the note supports. */
const stubSupportedChoices = (supportedChoices: number[]) => {
  cy.then(async () => {
    await mock_services
      .openAi()
      .responses()
      .requestMessageMatches({
        role: 'developer',
        content: 'Your only responsibility is answer support',
      })
      .stubOutputText(JSON.stringify({ supportedChoices }))
  })
}

export const questionGenerationService = () => {
  return {
    resetAndStubAskingMCQByResponses: (record: Record<string, string>) => {
      cy.then(async () => {
        await stubQuestionDesignerMcqOutput(mcqReplyJson(record))
      })
    },

    stubRegeneratedQuestion: (record: Record<string, string>) => {
      cy.then(async () => {
        const reply = mcqReplyJson(record)
        await mock_services
          .openAi()
          .responses()
          .requestMessageMatches({
            role: 'user',
            content: 'Previously generated question, contested by the learner:',
          })
          .stubOutputText(reply)
      })
    },

    stubNoteSupportsNoChoice: () => {
      stubSupportedChoices([])
    },
  }
}
