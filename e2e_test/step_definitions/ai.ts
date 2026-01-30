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
  cy.then(async () => {
    await mock_services.openAi().alwaysResponseAsUnauthorized()
  })
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

Given('OpenAI returns embeddings successfully', () => {
  mock_services.openAi().stubCreateEmbeddings()
})

Given('An OpenAI response is unavailable', () => {
  mock_services.openAi().stubOpenAiCompletionWithErrorResponse()
})

Given('OpenAI generates this question:', (questionTable: DataTable) => {
  const hashes = questionTable.hashes()
  if (hashes.length !== 1 || !hashes[0]) {
    throw new Error(
      `Expected exactly one row in the data table, but got ${hashes.length}`
    )
  }
  start
    .questionGenerationService()
    .resetAndStubAskingMCQByChatCompletion(hashes[0])
})

Given(
  'OpenAI generates this as first question:',
  (questionTable: DataTable) => {
    const hashes = questionTable.hashes()
    if (hashes.length !== 1 || !hashes[0]) {
      throw new Error(
        `Expected exactly one row in the data table, but got ${hashes.length}`
      )
    }
    // Store the first question as the default
    start
      .questionGenerationService()
      .resetAndStubAskingMCQByChatCompletion(hashes[0])
  }
)

Given(
  'OpenAI generates this as second question:',
  (questionTable: DataTable) => {
    const hashes = questionTable.hashes()
    if (hashes.length !== 1 || !hashes[0]) {
      throw new Error(
        `Expected exactly one row in the data table, but got ${hashes.length}`
      )
    }
    // Register the second question to be used after contest
    cy.then(async () => {
      // Just store the data, it will be used by the "not legitamate" step
      Cypress.env('secondQuestion', hashes[0])
    })
  }
)

Given('OpenAI evaluates the question as legitamate', () => {
  start.questionGenerationService().stubEvaluationQuestion({
    feasibleQuestion: true,
    correctChoices: [0],
    improvementAdvices: 'Yes, this is a good question!',
  })
})

Given('OpenAI evaluates the question as not legitamate', () => {
  start.questionGenerationService().stubEvaluationQuestion({
    feasibleQuestion: false,
    correctChoices: [0],
    improvementAdvices:
      'This question is not feasible and needs to be regenerated completely.',
  })

  // Use the stored second question data if available
  const secondQuestion = Cypress.env('secondQuestion') || {
    'Question Stem': 'Second question',
    'Correct Choice': 'Rescue Diver',
    'Incorrect Choice 1': 'Divemaster',
    'Incorrect Choice 2': 'Open Water Diver',
  }

  start
    .questionGenerationService()
    .resetAndStubAskingMCQByChatCompletion(secondQuestion)
})

Then('I contest the question', () => {
  cy.findByRole('button', { name: "Doesn't make sense?" }).click()
})

Given('OpenAI will reply below for user messages:', (data: DataTable) => {
  // Use chat completion streaming
  mock_services.openAi().stubChatCompletionStream(data.hashes())
})

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
  'OpenAI will reply below for user messages with notebook-specific instructions:',
  (data: DataTable) => {
    // Conversations use Chat Completion API
    // Notebook-specific instructions are included in system messages
    mock_services.openAi().stubChatCompletionStream(data.hashes())
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
        .chatCompletion()
        .requestMessageMatches({
          role: 'system',
          content: '.*Please generate an understanding checklist.*',
        })
        .stubUnderstandingChecklist(reply)
    })
  }
)

Given(
  'OpenAI will delete related content and return new details:',
  (data: DataTable) => {
    const newDetails = data.raw().flat()[0]
    const reply = JSON.stringify({ newDetails })
    cy.then(async () => {
      await mock_services
        .openAi()
        .chatCompletion()
        .requestMessageMatches({
          role: 'system',
          content: '.*delete.*understanding.*points.*',
        })
        .stubResponse(reply)
    })
  }
)

Given(
  'OpenAI will extract point {string} to child note with title {string} and details {string} and updated parent details {string}',
  (
    point: string,
    newNoteTitle: string,
    newNoteDetails: string,
    updatedParentDetails: string
  ) => {
    const result = {
      newNoteTitle,
      newNoteDetails,
      updatedParentDetails,
    }
    const reply = JSON.stringify(result)
    cy.then(async () => {
      await mock_services
        .openAi()
        .chatCompletion()
        .requestMessageMatches({
          role: 'system',
          content: '.*extract.*point.*child.*',
        })
        .stubExtractPointToChild(reply)
    })
  }
)

Given(
  'OpenAI will extract point {string} to sibling note with title {string} and details {string} and updated parent details {string}',
  (
    point: string,
    newNoteTitle: string,
    newNoteDetails: string,
    updatedParentDetails: string
  ) => {
    const result = {
      newNoteTitle,
      newNoteDetails,
      updatedParentDetails,
    }
    const reply = JSON.stringify(result)
    cy.then(async () => {
      await mock_services
        .openAi()
        .chatCompletion()
        .requestMessageMatches({
          role: 'system',
          content: '.*extract.*point.*sibling.*',
        })
        .stubExtractPointToSibling(reply)
    })
  }
)
