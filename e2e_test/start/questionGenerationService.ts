import type { TextMessageToMatch } from './mock_services/MessageToMatch'
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

const restartOpenAiAndStubMcqForUserMessage = async (
  userMessageMatch: TextMessageToMatch,
  record: Record<string, string>,
  responseKind: 'jsonSchema' | 'questionGeneration'
) => {
  const reply = mcqReplyJson(record)
  const stub = mock_services
    .openAi()
    .responses()
    .requestMessageMatches(userMessageMatch)
  if (responseKind === 'questionGeneration') {
    await stub
      .requestDoesNotMessageMatch({
        role: 'user',
        content: 'Previously generated non-feasible question',
      })
      .stubOutputText(reply)
  } else {
    await stub.stubOutputText(reply)
  }
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
      content:
        '[\\s\\S]*Title: FarDepthTwo[\\s\\S]*Path:[\\s\\S]*->[\\s\\S]*->[\\s\\S]*Reached by: OutgoingWikiLink[\\s\\S]*',
    },
    depthTwoRow
  )
  await addJsonSchemaMcqStubForUserMessage(
    {
      role: 'user',
      content:
        '[\\s\\S]*Reached by: FolderSibling[\\s\\S]*Reached by: FolderSibling[\\s\\S]*',
    },
    folderSiblingsRow
  )
  await addJsonSchemaMcqStubForUserMessage(
    {
      role: 'user',
      content:
        '[\\s\\S]*Title: WikiRecall[\\s\\S]*Title: Bahamas[\\s\\S]*Reached by: OutgoingWikiLink[\\s\\S]*',
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

export const questionGenerationService = () => ({
  resetAndStubAskingMCQByResponses: (record: Record<string, string>) => {
    cy.then(async () => {
      await restartOpenAiAndStubMcqForUserMessage(
        { role: 'developer', content: 'Question Designer|Memory Assistant' },
        record,
        'questionGeneration'
      )
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
})
