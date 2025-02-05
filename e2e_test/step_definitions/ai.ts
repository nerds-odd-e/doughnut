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

Given(
  'OpenAI generates this question for assistant thread {string}:',
  (threadId: string, questionTable: DataTable) => {
    const hashes = questionTable.hashes()
    if (hashes.length !== 1 || !hashes[0]) {
      throw new Error(
        `Expected exactly one row in the data table, but got ${hashes.length}`
      )
    }
    start.questionGenerationService().stubAskingMCQ(threadId, hashes[0])
  }
)

Given(
  'OpenAI assistant will create these thread ids in sequence: {string}',
  (threadIds: string) => {
    const threadIdsArray = threadIds.split(',').map((id) => id.trim())
    mock_services.openAi().stubCreateThreads(threadIdsArray)
  }
)

When('I test myself for the note {string}', (noteTitle: string) => {
  start.jumpToNotePage(noteTitle).testMe()
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
  'OpenAI assistant will reply below for user messages in a stream run:',
  (data: DataTable) => {
    mock_services
      .openAi()
      .stubCreateThread('thread-123')
      .createThreadWithRunStreamAndStubMessages('thread-123', data.hashes())
  }
)

Given(
  'OpenAI assistant will reply below for user messages in a non-stream run {string}:',
  (runId: string, data: DataTable) => {
    mock_services
      .openAi()
      .stubCreateThread('thread-123')
      .stubCreateRuns('thread-123', [runId])
      .aRun(runId)
      .stubRetrieveRunsThatRequireAction(data.hashes())
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

Given(
  'OpenAI assistant can accept tool call results submission and run cancellation for run {string}',
  (runId: string) => {
    mock_services
      .openAi()
      .stubToolCallSubmission('thread-123', runId)
      .stubRunCancellation('thread-123', runId)
  }
)

Given(
  'OpenAI assistant {string} will reply below for user messages:',
  (assistantId: string, data: DataTable) => {
    mock_services
      .openAi()
      .stubCreateThread('thread-123')
      .createThreadWithRunStreamAndStubMessages(
        'thread-123',
        data.hashes(),
        assistantId
      )
  }
)

Given(
  'I set my notebook {string} to use additional AI instruction {string}',
  (notebook: string, instruction: string) => {
    start
      .routerToNotebooksPage()
      .notebookCard(notebook)
      .editNotebookSettings()
      .updateAiAssistantInstructions(instruction)
  }
)

Given(
  'I create a customized assistant for my notebook {string}',
  (notebook: string) => {
    start
      .routerToNotebooksPage()
      .notebookCard(notebook)
      .editNotebookSettings()
      .createCustomizedAssistant()
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

Then(
  'I should see the suggested completion {string} in the chat dialog',
  (completion: string) => {
    start.assumeConversationAboutNotePage().shouldShowCompletion(completion)
  }
)

When('I accept the suggested completion', () => {
  start.assumeConversationAboutNotePage().acceptCompletion()
})

When('I reject the suggested completion', () => {
  start.assumeConversationAboutNotePage().cancelCompletion()
})
