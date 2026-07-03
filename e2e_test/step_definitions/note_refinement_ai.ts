/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { type DataTable, Given } from '@badeball/cypress-cucumber-preprocessor'
import { mock_services } from '../start'
import { REFINEMENT_LAYOUT_INSTRUCTION_PATTERN } from '../start/mock_services/createOpenAiResponsesMock'

const EXTRACT_NOTE_INSTRUCTION_PATTERN =
  '.*extract selected layout points from a note to create one new note.*'

const REMOVE_LAYOUT_POINTS_INSTRUCTION_PATTERN =
  '.*remove selected layout points from the note content.*'

let refinementLayoutInitialOutput: string | null = null

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
  updatedOriginalNoteContent: string
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
        updatedOriginalNoteContent,
      })
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
  'OpenAI returns the following content when requested to remove layout points:',
  (data: DataTable) => {
    const content = data.raw().flat()[0]
    const reply = JSON.stringify({ content })
    cy.then(async () => {
      await mock_services
        .openAi()
        .responses()
        .requestMessageMatches({
          role: 'developer',
          content: REMOVE_LAYOUT_POINTS_INSTRUCTION_PATTERN,
        })
        .stubOutputText(reply)
    })
  }
)

Given(
  'OpenAI will extract layout points {string} to a new note with title {string} and content {string} and updated parent content {string}',
  (
    _layoutPoints: string,
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
