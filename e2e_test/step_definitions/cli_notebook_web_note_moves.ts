/**
 * Note identity across CLI journeys: capture the original note id before a move, trash, or
 * rename, then check that the same Donut note holds the published result.
 */
import { Given, Then } from '@badeball/cypress-cucumber-preprocessor'
import type NotePath from '../support/NotePath'
import start from '../start'

Given('I capture the note id of {string}', (noteTitle: string) => {
  start
    .testability()
    .getInjectedNoteIdByTitle(noteTitle)
    .then((noteId: number) => {
      cy.wrap(noteId).as('capturedOriginalNoteId')
    })
})

Then(
  'the original note in Donut should be {notepath} with content:',
  (notePath: NotePath, expectedContent: string) => {
    cy.get<number>('@capturedOriginalNoteId').then((noteId) => {
      start.donutNotebookContent.expectNoteByIdAt(
        noteId,
        notePath.path,
        expectedContent.trim()
      )
    })
  }
)
