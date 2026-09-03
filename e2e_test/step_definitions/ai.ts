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
  'OpenAI will return these questions in order:',
  (questionTable: DataTable) => {
    start
      .questionGenerationService()
      .stubAskingMCQSequence(questionTable.hashes())
  }
)

const defaultReplacementMcq = {
  'Question Stem': 'Second question',
  'Correct Choice': 'Rescue Diver',
  'Incorrect Choice 1': 'Divemaster',
  'Incorrect Choice 2': 'Open Water Diver',
  'Incorrect Choice 3': 'Advanced Open Water Diver',
}

Given('OpenAI evaluates the question as legitimate', () => {
  start.questionGenerationService().stubAcceptedEvaluation()
})

Given('OpenAI evaluates the question as not legitimate', () => {
  start.questionGenerationService().stubRejectedEvaluation()
  start
    .questionGenerationService()
    .stubRegeneratedQuestion(defaultReplacementMcq)
})

Given('OpenAI will accept the generated question then uphold a contest', () => {
  start.questionGenerationService().stubAcceptThenUpholdContestEvaluations()
  start
    .questionGenerationService()
    .stubRegeneratedQuestion(defaultReplacementMcq)
})

When('I contest the MCQ', () => {
  start.assumeQuestionPage().contestQuestion()
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

Then('I should see the suggested completion', () => {
  start.assumeConversationAboutNotePage().shouldShowCompletion()
})

When('I accept the suggested completion', () => {
  start.assumeConversationAboutNotePage().acceptCompletion()
})
