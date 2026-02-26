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

function stubExtractPointResponse(
  contentPattern: string,
  newNoteTitle: string,
  newNoteDetails: string,
  updatedParentDetails: string
) {
  const reply = JSON.stringify({
    newNoteTitle,
    newNoteDetails,
    updatedParentDetails,
  })
  cy.then(async () => {
    await mock_services
      .openAi()
      .chatCompletion()
      .requestMessageMatches({
        role: 'system',
        content: contentPattern,
      })
      .stubJsonSchemaResponse(reply)
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
    start.form.expectFieldError('Prompt', errorMessage)
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
  start
    .questionGenerationService()
    .resetAndStubAskingMCQByChatCompletion(
      parseSingleRowQuestion(questionTable)
    )
})

Given(
  'OpenAI generates this as first question:',
  (questionTable: DataTable) => {
    start
      .questionGenerationService()
      .resetAndStubAskingMCQByChatCompletion(
        parseSingleRowQuestion(questionTable)
      )
  }
)

Given(
  'OpenAI generates this as second question:',
  (questionTable: DataTable) => {
    const question = parseSingleRowQuestion(questionTable)
    cy.then(async () => {
      Cypress.env('secondQuestion', question)
    })
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
      .stubJsonSchemaResponse(
        JSON.stringify({
          completion: data.hashes()[0]!.response!,
          deleteFromEnd: 0,
        })
      )
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
        .stubJsonSchemaResponse(reply)
    })
  }
)

Given(
  'OpenAI returns the following details when requested to delete points:',
  (data: DataTable) => {
    const details = data.raw().flat()[0]
    const reply = JSON.stringify({ details })
    cy.then(async () => {
      await mock_services
        .openAi()
        .chatCompletion()
        .requestMessageMatches({
          role: 'system',
          content: '.*remove.*points.*',
        })
        .stubJsonSchemaResponse(reply)
    })
  }
)

Given(
  'OpenAI will extract point {string} to child note with title {string} and details {string} and updated parent details {string}',
  (
    _point: string,
    newNoteTitle: string,
    newNoteDetails: string,
    updatedParentDetails: string
  ) => {
    stubExtractPointResponse(
      '.*extract.*point.*child.*',
      newNoteTitle,
      newNoteDetails,
      updatedParentDetails
    )
  }
)

Given(
  'OpenAI will extract point {string} to sibling note with title {string} and details {string} and updated parent details {string}',
  (
    _point: string,
    newNoteTitle: string,
    newNoteDetails: string,
    updatedParentDetails: string
  ) => {
    stubExtractPointResponse(
      '.*extract.*point.*sibling.*',
      newNoteTitle,
      newNoteDetails,
      updatedParentDetails
    )
  }
)
