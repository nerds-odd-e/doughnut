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

const EXTRACT_NOTE_INSTRUCTION_PATTERN =
  '.*extract selected layout points from a note to create one new note.*'

const REFINEMENT_SUGGESTIONS_INSTRUCTION_PATTERN =
  '.*Return one current-content layout for the note content.*'

async function stubRefinementSuggestions(suggestions: string[]) {
  const items = suggestions.map((text, index) => ({
    id: `p${index + 1}`,
    text,
    alreadyExtracted: false,
    children: [],
  }))
  await mock_services
    .openAi()
    .responses()
    .requestMessageMatches({
      role: 'developer',
      content: REFINEMENT_SUGGESTIONS_INSTRUCTION_PATTERN,
    })
    .stubOutputText(JSON.stringify({ items }))
}

type RefinementLayoutItem = {
  id: string
  text: string
  alreadyExtracted: boolean
  children: RefinementLayoutItem[]
}

function parseAlreadyExtracted(value?: string) {
  return ['true', 'yes', 'already extracted'].includes(
    value?.trim().toLowerCase() ?? ''
  )
}

function refinementLayoutFromTable(data: DataTable) {
  const rows = data.hashes()
  const itemsById = new Map<string, RefinementLayoutItem>()
  rows.forEach((row) => {
    itemsById.set(row.id, {
      id: row.id,
      text: row.text,
      alreadyExtracted: parseAlreadyExtracted(row.alreadyExtracted),
      children: [],
    })
  })

  const rootItems: RefinementLayoutItem[] = []
  rows.forEach((row) => {
    const item = itemsById.get(row.id)
    if (!item) {
      throw new Error(`Missing refinement layout item ${row.id}`)
    }

    const parentId = row.parent?.trim()
    if (parentId) {
      const parent = itemsById.get(parentId)
      if (!parent) {
        throw new Error(`Missing refinement layout parent ${parentId}`)
      }
      parent.children.push(item)
    } else {
      rootItems.push(item)
    }
  })
  return rootItems
}

async function stubExtractNoteResponse(
  newNoteTitle: string,
  newNoteContent: string,
  updatedParentContent: string
) {
  await mock_services
    .openAi()
    .responses()
    .requestMessageMatches({
      role: 'developer',
      content: EXTRACT_NOTE_INSTRUCTION_PATTERN,
    })
    .stubOutputText(
      JSON.stringify({
        newNoteTitle,
        newNoteContent,
        updatedParentContent,
      })
    )
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

Then(
  'OpenAI Responses POST bodies include property focus for {string} with value {string}',
  (propertyKey: string, propertyValue: string) => {
    mock_services
      .openAi()
      .expectResponsesPostBodiesIncludePropertyFocusInFocusContext(
        propertyKey,
        propertyValue
      )
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

Given('OpenAI generates refinement suggestions:', (data: DataTable) => {
  const suggestions = data
    .raw()
    .flat()
    .filter((suggestion) => suggestion.trim().length > 0)
  cy.then(async () => {
    await mock_services.openAi().restartImposter()
    await stubRefinementSuggestions(suggestions)
  })
})

Given('OpenAI generates refinement layout:', (data: DataTable) => {
  cy.then(async () => {
    await mock_services.openAi().restartImposter()
    await mock_services
      .openAi()
      .responses()
      .requestMessageMatches({
        role: 'developer',
        content: REFINEMENT_SUGGESTIONS_INSTRUCTION_PATTERN,
      })
      .stubOutputText(
        JSON.stringify({ items: refinementLayoutFromTable(data) })
      )
  })
})

Given(
  'OpenAI returns the following content when requested to remove suggestions:',
  (data: DataTable) => {
    const content = data.raw().flat()[0]
    const reply = JSON.stringify({ content })
    cy.then(async () => {
      await mock_services
        .openAi()
        .responses()
        .requestMessageMatches({
          role: 'developer',
          content: '.*remove.*refinement suggestions.*',
        })
        .stubOutputText(reply)
    })
  }
)

Given(
  'OpenAI will extract suggestion {string} to a new note with title {string} and content {string} and updated parent content {string}',
  (
    _suggestion: string,
    newNoteTitle: string,
    newNoteContent: string,
    updatedParentContent: string
  ) => {
    cy.then(async () => {
      await stubExtractNoteResponse(
        newNoteTitle,
        newNoteContent,
        updatedParentContent
      )
    })
  }
)
