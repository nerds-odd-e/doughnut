/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import {
  type DataTable,
  Given,
  Then,
  When,
} from '@badeball/cypress-cucumber-preprocessor'
import '../support/string_util'
import start, { mock_services } from '../start'

function parseSingleRowQuestion(questionTable: DataTable) {
  const hashes = questionTable.hashes()
  if (hashes.length !== 1 || !hashes[0]) {
    throw new Error(
      `Expected exactly one row in the data table, but got ${hashes.length}`
    )
  }
  return hashes[0]
}

function stubOpenAiMcqFromSingleRowTable(questionTable: DataTable) {
  start
    .questionGenerationService()
    .resetAndStubAskingMCQByResponses(parseSingleRowQuestion(questionTable))
}

function stubExtractPointResponse(
  contentPattern: string,
  newNoteTitle: string,
  newNoteContent: string,
  updatedParentContent: string
) {
  const reply = JSON.stringify({
    newNoteTitle,
    newNoteContent,
    updatedParentContent,
  })
  cy.then(async () => {
    await mock_services
      .openAi()
      .responses()
      .requestMessageMatches({
        role: 'developer',
        content: contentPattern,
      })
      .stubOutputText(reply)
  })
}

Given('the OpenAI service is unavailable due to invalid system token', () => {
  cy.then(async () => {
    await mock_services.openAi().alwaysResponseAsUnauthorized()
  })
})

Then(
  'I should be prompted with an error message saying {string}',
  (errorMessage: string) => {
    start.form.getField('Prompt').expectError(errorMessage)
  }
)

Given('OpenAI has models {string} available', (modelNames: string) => {
  cy.then(async () => {
    await mock_services.openAi().stubGetModels(modelNames)
  })
})

Given('OpenAI returns embeddings successfully', () => {
  mock_services.openAi().stubCreateEmbeddings()
})

Given('An OpenAI response is unavailable', () => {
  mock_services.openAi().stubOpenAiWithErrorResponse()
})

Given('OpenAI generates this question:', stubOpenAiMcqFromSingleRowTable)

Given(
  'OpenAI now refines the question to become:',
  stubOpenAiMcqFromSingleRowTable
)

Given(
  'OpenAI generates these MCQs when focus context matches depth-two wiki path, folder siblings, and wiki-linked Bahamas note:',
  (questionTable: DataTable) => {
    const rows = questionTable.hashes()
    start
      .questionGenerationService()
      .resetAndStubMcqForFocusContextRetrievalCases(rows)
  }
)

Then(
  'OpenAI Responses POST bodies include wiki-linked, depth-two wiki path, and folder-sibling focus context prompts',
  () => {
    mock_services
      .openAi()
      .expectResponsesPostBodiesIncludeFocusContextRetrievalPromptShapes()
  }
)

Given(
  'OpenAI generates this as first question:',
  stubOpenAiMcqFromSingleRowTable
)

Given(
  'OpenAI generates this as second question:',
  (questionTable: DataTable) => {
    const question = parseSingleRowQuestion(questionTable)
    cy.task('setTestState', { key: 'secondQuestion', value: question })
  }
)

Given('OpenAI evaluates the question as legitimate', () => {
  start.questionGenerationService().stubEvaluationQuestion({
    feasibleQuestion: true,
    correctChoices: [0],
    improvementAdvices: 'Yes, this is a good question!',
  })
})

Given('OpenAI evaluates the question as not legitimate', () => {
  start.questionGenerationService().stubEvaluationQuestion({
    feasibleQuestion: false,
    correctChoices: [0],
    improvementAdvices:
      'This question is not feasible and needs to be regenerated completely.',
  })

  const defaultSecondQuestion = {
    'Question Stem': 'Second question',
    'Correct Choice': 'Rescue Diver',
    'Incorrect Choice 1': 'Divemaster',
    'Incorrect Choice 2': 'Open Water Diver',
  }
  cy.task('getTestState', 'secondQuestion').then((stored) => {
    const secondQuestion =
      (stored as Record<string, string> | undefined) ?? defaultSecondQuestion
    start.questionGenerationService().stubRegeneratedQuestion(secondQuestion)
  })
})

Then('I contest the question', () => {
  cy.findByRole('button', { name: "Doesn't make sense?" }).click()
})

Given('OpenAI will reply below for user messages:', (data: DataTable) => {
  mock_services.openAi().stubConversationAiReplyStream(data.hashes())
})

Given(
  'the OpenAI completion service will return the following response for the transcription to text request:',
  (data: DataTable) => {
    const row = data.hashes()[0]!
    const reply = JSON.stringify({ content: row.response! })
    mock_services
      .openAi()
      .responses()
      .requestMessageMatches({
        role: 'developer',
        content: `.*${row['request contains']}.*`,
      })
      .stubOutputText(reply)
  }
)

Given(
  'the OpenAI transcription service will return the following srt transcript:',
  (transcript: string) => {
    mock_services.openAi().stubTranscription(transcript)
  }
)

Then('I should see the suggested completion in the chat dialog', () => {
  start.assumeConversationAboutNotePage().shouldShowCompletion()
})

When('I accept the suggested completion', () => {
  start.assumeConversationAboutNotePage().acceptCompletion()
})

When('I reject the suggested completion', () => {
  start.assumeConversationAboutNotePage().cancelCompletion()
})

Given(
  'OpenAI generates understanding checklist with points:',
  (data: DataTable) => {
    const points = data
      .raw()
      .flat()
      .filter((point) => point.trim().length > 0)
    const understandingChecklist = { points }
    const reply = JSON.stringify(understandingChecklist)
    cy.then(async () => {
      await mock_services.openAi().restartImposter()
      await mock_services
        .openAi()
        .responses()
        .requestMessageMatches({
          role: 'developer',
          content: '.*Please generate an understanding checklist.*',
        })
        .stubOutputText(reply)
    })
  }
)

Given(
  'OpenAI returns the following content when requested to delete points:',
  (data: DataTable) => {
    const content = data.raw().flat()[0]
    const reply = JSON.stringify({ content })
    cy.then(async () => {
      await mock_services
        .openAi()
        .responses()
        .requestMessageMatches({
          role: 'developer',
          content: '.*remove.*points.*',
        })
        .stubOutputText(reply)
    })
  }
)

Given(
  'OpenAI will extract point {string} to sibling note with title {string} and content {string} and updated parent content {string}',
  (
    _point: string,
    newNoteTitle: string,
    newNoteContent: string,
    updatedParentContent: string
  ) => {
    stubExtractPointResponse(
      '.*extract.*point.*sibling.*',
      newNoteTitle,
      newNoteContent,
      updatedParentContent
    )
  }
)
