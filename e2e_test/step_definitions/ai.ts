/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { DataTable, Given, Then } from '@badeball/cypress-cucumber-preprocessor'
import '../support/string_util'
import start, { mock_services } from '../start'

Given('the OpenAI service is unavailable due to invalid system token', () => {
  mock_services.openAi().alwaysResponseAsUnauthorized()
})

Then(
  'I should be prompted with an error message saying {string}',
  (errorMessage: string) => {
    cy.expectFieldErrorMessage('Prompt', errorMessage)
  }
)

Given('OpenAI has models {string} available', (modelNames: string) => {
  cy.then(async () => {
    mock_services.openAi().stubGetModels(modelNames)
  })
})

Given('OpenAI always return image of a moon', () => {
  mock_services.openAi().stubCreateImage()
})

Given('An OpenAI response is unavailable', () => {
  mock_services.openAi().stubOpenAiCompletionWithErrorResponse()
})

Given('OpenAI now generates this question:', (questionTable: DataTable) => {
  const hashes = questionTable.hashes()
  if (hashes.length !== 1 || !hashes[0]) {
    throw new Error(
      `Expected exactly one row in the data table, but got ${hashes.length}`
    )
  }
  start.questionGenerationService().resetAndStubAskingMCQ(hashes[0])
})

Given('OpenAI evaluates the question as legitamate', () => {
  start
    .questionGenerationService()
    .stubEvaluationQuestion({ feasibleQuestion: true, correctChoices: [0] })
})

Given('OpenAI evaluates the question as not legitamate', () => {
  start.questionGenerationService().stubEvaluationQuestion({
    feasibleQuestion: false,
    correctChoices: [0],
    comment: "AI plainly doesn't like it.",
  })
})

Then('I contest the question', () => {
  cy.findByRole('button', { name: "Doesn't make sense?" }).click()
})

Given(
  'OpenAI assistant will reply below for user messages:',
  (data: DataTable) => {
    mock_services
      .openAi()
      .stubCreateThread('thread-123')
      .createThreadAndStubMessages('thread-123', data.hashes())
  }
)

Given('OpenAI assistant can accept tool call results submission', () => {
  mock_services.openAi().stubToolCallSubmission()
})

Given(
  'OpenAI assistant {string} will reply below for user messages:',
  (assistantId: string, data: DataTable) => {
    mock_services
      .openAi()
      .stubCreateThread('thread-123')
      .createThreadAndStubMessages('thread-123', data.hashes(), assistantId)
  }
)

Given(
  'I create an assistant for my notebook {string} with additional instruction {string}',
  (notebook: string, instruction: string) => {
    start
      .routerToNotebooksPage()
      .notebookCard(notebook)
      .notebookAssistant()
      .create(instruction)
  }
)

Given('OpenAI accepts the vector file upload requests', () => {
  mock_services.openAi().stubOpenAiUploadResponse(true)
  mock_services.openAi().stubOpenAiVectorFileUpload()
})

Given(
  'the OpenAI transcription service will return the following srt transcript:',
  (transcript: string) => {
    mock_services.openAi().stubTranscription(transcript)
  }
)

Given(
  'the OpenAI completion service will return the following response for the transcription to text request:',
  (data: DataTable) => {
    mock_services
      .openAi()
      .chatCompletion()
      .requestMessageMatches({
        role: 'user',
        content: `.*${data.hashes()[0]!['request contains']}.*`,
      })
      .stubAudioTranscriptToText(data.hashes()[0]!.response!)
  }
)
