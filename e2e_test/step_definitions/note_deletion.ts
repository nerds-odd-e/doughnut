/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When(
  'I trash note {string} at {int}:00',
  (noteTopology: string, hour: number) => {
    start.testability().backendTimeTravelTo(0, hour)
    start.jumpToNotePage(noteTopology).trashNote()
  }
)

When('I trash note {string}', (noteTopology: string) => {
  start.jumpToNotePage(noteTopology).trashNote()
})

When(
  'I trash note {string} and leave references as dead wiki links',
  (noteTopology: string) => {
    start
      .jumpToNotePage(noteTopology)
      .trashNoteAndLeaveReferencesAsDeadWikiLinks()
  }
)

When(
  'I trash note {string} and remove it from properties of references',
  (noteTopology: string) => {
    start
      .jumpToNotePage(noteTopology)
      .trashNoteAndRemoveFromReferenceProperties()
  }
)

Then('I should see the note {string} is in trash', (noteTopology: string) => {
  start.jumpToNotePage(noteTopology)
  cy.findByText('This note is in trash')
})

Then(
  'I should see the note {string} is marked as deleted',
  (noteTopology: string) => {
    start.jumpToNotePage(noteTopology)
    cy.findByText('This note has been deleted')
  }
)

When('I undo {string}', (undoType: string) => {
  start.assumeNotePage().undo(undoType)
  start.waitUntilAppIsNotBusy()
})

When('I undo trash note to recover note {string}', (noteTitle: string) => {
  start.assumeNotePage().undo('trash note')
  start.assumeNotePage(noteTitle)
})

Then('there should be nothing left to undo', () => {
  cy.get('.daisy-btn[title^="undo"]').should('not.exist')
})
