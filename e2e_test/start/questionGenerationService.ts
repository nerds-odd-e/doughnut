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
const stubQuestionDesignerMcqOutputTexts = async (...outputTexts: string[]) => {
  await mock_services
    .openAi()
    .responses()
    .requestMessageMatches({
      role: 'developer',
      content: 'Question Designer|Memory Assistant',
    })
    .requestDoesNotMessageMatch({
      role: 'user',
      content: 'Previously generated non-feasible question',
    })
    .stubOutputTextSequence(...outputTexts)
}

type QuestionEvaluation = {
  feasibleQuestion: boolean
  correctChoices: number[]
  improvementAdvices: string
}

const acceptedQuestionEvaluation: QuestionEvaluation = {
  feasibleQuestion: true,
  correctChoices: [0],
  improvementAdvices: 'Yes, this is a good question!',
}

const rejectedQuestionEvaluation: QuestionEvaluation = {
  feasibleQuestion: false,
  correctChoices: [0],
  improvementAdvices:
    'This question is not feasible and needs to be regenerated completely.',
}

const evaluationDeveloperMessage = {
  role: 'developer' as const,
  content: 'evaluating a memory recall question',
}

const stubEvaluationOutput = (record: QuestionEvaluation) => {
  cy.then(async () => {
    await mock_services
      .openAi()
      .responses()
      .requestMessageMatches(evaluationDeveloperMessage)
      .stubOutputText(JSON.stringify(record))
  })
}

export const questionGenerationService = () => {
  const stubAskingMCQSequence = (records: Record<string, string>[]) => {
    if (records.length < 1) {
      throw new Error('stubAskingMCQSequence requires at least one MCQ row')
    }
    cy.then(async () => {
      await stubQuestionDesignerMcqOutputTexts(...records.map(mcqReplyJson))
    })
  }

  return {
    resetAndStubAskingMCQByResponses: (record: Record<string, string>) => {
      stubAskingMCQSequence([record])
    },

    stubAskingMCQSequence,

    stubRegeneratedQuestion: (record: Record<string, string>) => {
      cy.then(async () => {
        const reply = mcqReplyJson(record)
        await mock_services
          .openAi()
          .responses()
          .requestMessageMatches({
            role: 'user',
            content: 'Previously generated non-feasible question',
          })
          .stubOutputText(reply)
      })
    },

    stubAcceptedEvaluation: () => {
      stubEvaluationOutput(acceptedQuestionEvaluation)
    },

    stubRejectedEvaluation: () => {
      stubEvaluationOutput(rejectedQuestionEvaluation)
    },

    /** First evaluation accepts (generation keeps MCQ); later evaluations uphold contest. */
    stubAcceptThenUpholdContestEvaluations: () => {
      cy.then(async () => {
        await mock_services
          .openAi()
          .responses()
          .requestMessageMatches(evaluationDeveloperMessage)
          .stubOutputTextSequence(
            JSON.stringify(acceptedQuestionEvaluation),
            JSON.stringify(rejectedQuestionEvaluation)
          )
      })
    },
  }
}
