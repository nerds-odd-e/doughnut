import type { TextMessageToMatch } from './mock_services/MessageToMatch'
import { focusContextRecallStubUserContent } from './mock_services/focusContextRecallPromptShapes'
import mock_services from './mock_services'

/** Shape of JSON returned by OpenAI tool calls for MCQ (not all fields are in OpenAPI). */
type GeneratedMcq = {
  questionStem: string
  responseChoices: string[]
  correctAnswerIndex: number
  choicesMayBeShuffled: boolean
  testedFocus?: string
  validationRationale?: string
}

const createGeneratedMcq = (
  stem: string,
  correctChoice: string,
  incorrectChoice1: string,
  incorrectChoice2: string
): GeneratedMcq => ({
  correctAnswerIndex: 0,
  choicesMayBeShuffled: false,
  questionStem: stem,
  responseChoices: [correctChoice, incorrectChoice1, incorrectChoice2],
})

const mcqReplyJson = (record: Record<string, string>) =>
  JSON.stringify(
    createGeneratedMcq(
      record['Question Stem']!,
      record['Correct Choice']!,
      record['Incorrect Choice 1']!,
      record['Incorrect Choice 2']!
    )
  )

/** Adds a JSON-schema MCQ stub without restarting the OpenAI imposter. */
const addJsonSchemaMcqStubForUserMessage = async (
  userMessageMatch: TextMessageToMatch,
  record: Record<string, string>
) => {
  const reply = mcqReplyJson(record)
  await mock_services
    .openAi()
    .responses()
    .requestMessageMatches(userMessageMatch)
    .stubOutputText(reply)
}

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

/** Stubs registered most-specific-first so Mountebank matches the right prompt per recall. */
const addFocusContextShapeMcqStubs = async (
  depthTwoRow: Record<string, string>,
  folderSiblingsRow: Record<string, string>,
  wikiLinkedBahamasRow: Record<string, string>
) => {
  await addJsonSchemaMcqStubForUserMessage(
    {
      role: 'user',
      content: focusContextRecallStubUserContent.depthTwoWiki,
    },
    depthTwoRow
  )
  await addJsonSchemaMcqStubForUserMessage(
    {
      role: 'user',
      content: focusContextRecallStubUserContent.folderSiblings,
    },
    folderSiblingsRow
  )
  await addJsonSchemaMcqStubForUserMessage(
    {
      role: 'user',
      content: focusContextRecallStubUserContent.wikiLinkedBahamas,
    },
    wikiLinkedBahamasRow
  )
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

    /**
     * Three predicates on the shared OpenAI imposter (depth-two wiki path, folder siblings, Bahamas wiki link).
     * Table rows must be in this order: depth-two question, folder-sibling question, wiki-linked question.
     */
    stubMcqForFocusContextRetrievalCases: (rows: Record<string, string>[]) => {
      if (rows.length !== 3) {
        throw new Error(
          `Expected exactly 3 MCQ rows (depth-two, folder siblings, wiki-linked), got ${rows.length}`
        )
      }
      const depthTwo = rows[0]!
      const folderSiblings = rows[1]!
      const wikiLinked = rows[2]!
      cy.then(async () => {
        await addFocusContextShapeMcqStubs(depthTwo, folderSiblings, wikiLinked)
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
