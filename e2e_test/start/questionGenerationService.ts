import type { McqWithAnswer } from '@generated/doughnut-backend-api/types.gen'
import type { TextMessageToMatch } from './mock_services/MessageToMatch'
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

const mcqReplyJson = (record: Record<string, string>) =>
  JSON.stringify(
    createMcqWithAnswer(
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
    .chatCompletion()
    .requestMessageMatches(userMessageMatch)
    .stubJsonSchemaResponse(reply)
}

const restartOpenAiAndStubMcqForUserMessage = async (
  userMessageMatch: TextMessageToMatch,
  record: Record<string, string>,
  responseKind: 'jsonSchema' | 'questionGeneration'
) => {
  await mock_services.openAi().restartImposter()
  if (responseKind === 'jsonSchema') {
    await addJsonSchemaMcqStubForUserMessage(userMessageMatch, record)
  } else {
    const reply = mcqReplyJson(record)
    await mock_services
      .openAi()
      .chatCompletion()
      .requestMessageMatches(userMessageMatch)
      .stubQuestionGeneration(reply)
  }
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

export const questionGenerationService = () => ({
  resetAndStubAskingMCQByChatCompletion: (record: Record<string, string>) => {
    cy.then(async () => {
      await restartOpenAiAndStubMcqForUserMessage(
        { role: 'user', content: 'Memory Assistant' },
        record,
        'questionGeneration'
      )
    })
  },

  /**
   * One imposter with three predicates (depth-two wiki path, folder siblings, Bahamas wiki link).
   * Table rows must be in this order: depth-two question, folder-sibling question, wiki-linked question.
   */
  resetAndStubMcqForFocusContextRetrievalCases: (
    rows: Record<string, string>[]
  ) => {
    if (rows.length !== 3) {
      throw new Error(
        `Expected exactly 3 MCQ rows (depth-two, folder siblings, wiki-linked), got ${rows.length}`
      )
    }
    const depthTwo = rows[0]!
    const folderSiblings = rows[1]!
    const wikiLinked = rows[2]!
    cy.then(async () => {
      await mock_services.openAi().restartImposter()
      await addFocusContextShapeMcqStubs(depthTwo, folderSiblings, wikiLinked)
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
