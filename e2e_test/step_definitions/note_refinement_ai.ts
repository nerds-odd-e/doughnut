/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import {
  type DataTable,
  Given,
  Then,
  When,
} from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import { mock_services } from '../start'
import { REFINEMENT_LAYOUT_INSTRUCTION_PATTERN } from '../start/mock_services/createOpenAiResponsesMock'

const EXTRACT_NOTE_INSTRUCTION_PATTERN =
  '.*extract selected refinement layout items from a note to create one new note.*'

const REMOVE_REFINEMENT_LAYOUT_ITEMS_INSTRUCTION_PATTERN =
  '.*remove selected refinement layout items from the note content.*'

let refinementLayoutInitialOutput: string | null = null

type RefinementLayoutItem = {
  id: string
  text: string
  alreadyExtracted: boolean
  ledToQuestion: boolean
  children: RefinementLayoutItem[]
}

function parseLayoutFlag(value: string | undefined, ...aliases: string[]) {
  return ['true', 'yes', ...aliases].includes(value?.trim().toLowerCase() ?? '')
}

function refinementLayoutFromTable(data: DataTable) {
  const rows = data.hashes()
  const itemsById = new Map<string, RefinementLayoutItem>()
  rows.forEach((row) => {
    itemsById.set(row.id, {
      id: row.id,
      text: row.text,
      alreadyExtracted: parseLayoutFlag(
        row.alreadyExtracted,
        'already extracted'
      ),
      ledToQuestion: parseLayoutFlag(row.ledToQuestion),
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
  updatedOriginalNoteContent: string
) {
  await stubExtractNoteResponseSequence([
    { newNoteTitle, newNoteContent, updatedOriginalNoteContent },
  ])
}

async function stubExtractNoteResponseSequence(
  results: {
    newNoteTitle: string
    newNoteContent: string
    updatedOriginalNoteContent: string
  }[]
) {
  await mock_services
    .openAi()
    .responses()
    .requestMessageMatches({
      role: 'developer',
      content: EXTRACT_NOTE_INSTRUCTION_PATTERN,
    })
    .stubOutputTextSequence(
      ...results.map((result) =>
        JSON.stringify({
          newNoteTitle: result.newNoteTitle,
          newNoteContent: result.newNoteContent,
          updatedOriginalNoteContent: result.updatedOriginalNoteContent,
        })
      )
    )
}

Given('OpenAI generates refinement layout:', (data: DataTable) => {
  cy.then(async () => {
    await mock_services.openAi().restartImposter()
    const items = refinementLayoutFromTable(data)
    refinementLayoutInitialOutput = JSON.stringify({ items })
    await mock_services
      .openAi()
      .responses()
      .requestMessageMatches({
        role: 'developer',
        content: REFINEMENT_LAYOUT_INSTRUCTION_PATTERN,
      })
      .stubOutputText(refinementLayoutInitialOutput)
  })
})

Given('OpenAI reloads refinement layout after removal:', (data: DataTable) => {
  cy.then(async () => {
    if (!refinementLayoutInitialOutput) {
      throw new Error(
        'OpenAI reloads refinement layout after removal requires OpenAI generates refinement layout in the Background'
      )
    }
    const reloadOutput = JSON.stringify({
      items: refinementLayoutFromTable(data),
    })
    await mock_services
      .openAi()
      .replaceRefinementLayoutStubWithSequence(
        refinementLayoutInitialOutput,
        reloadOutput,
        reloadOutput
      )
  })
})

Given(
  'OpenAI returns the following content when requested to remove refinement layout items:',
  (data: DataTable) => {
    const content = data.raw().flat()[0]
    const reply = JSON.stringify({ content })
    cy.then(async () => {
      await mock_services
        .openAi()
        .responses()
        .requestMessageMatches({
          role: 'developer',
          content: REMOVE_REFINEMENT_LAYOUT_ITEMS_INSTRUCTION_PATTERN,
        })
        .stubOutputText(reply)
    })
  }
)

Given(
  'OpenAI will extract refinement layout items {string} to a new note with title {string} and content {string} and updated parent content {string}',
  (
    _layoutItems: string,
    newNoteTitle: string,
    newNoteContent: string,
    updatedOriginalNoteContent: string
  ) => {
    cy.then(async () => {
      await stubExtractNoteResponse(
        newNoteTitle,
        newNoteContent,
        updatedOriginalNoteContent
      )
    })
  }
)

Given(
  'OpenAI will extract refinement layout items {string} with retry producing title {string} and content {string} and updated parent content {string}',
  (
    _layoutItems: string,
    retryNoteTitle: string,
    retryNoteContent: string,
    retryUpdatedParentContent: string
  ) => {
    cy.then(async () => {
      await stubExtractNoteResponseSequence([
        {
          newNoteTitle: 'First attempt title',
          newNoteContent: 'First attempt content',
          updatedOriginalNoteContent: 'A. C. E. first',
        },
        {
          newNoteTitle: retryNoteTitle,
          newNoteContent: retryNoteContent,
          updatedOriginalNoteContent: retryUpdatedParentContent,
        },
      ])
    })
  }
)

When('I open Refine note from the answered question', () => {
  start.assumeAnsweredQuestionPage().openRefineNoteModal()
})

Then(
  'refinement layout items {string} should be selected',
  (layoutItemText: string) => {
    start
      .assumeAnsweredQuestionPage()
      .expectRefinementLayoutItemsSelected(layoutItemText)
  }
)

Then(
  'refinement layout items {string} and {string} should not be selected',
  (firstItem: string, secondItem: string) => {
    start
      .assumeAnsweredQuestionPage()
      .expectRefinementLayoutItemsNotSelected(firstItem, secondItem)
  }
)
