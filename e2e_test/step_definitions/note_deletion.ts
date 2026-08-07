/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When(
  'I delete note {string} at {int}:00',
  (noteTopology: string, hour: number) => {
    start.testability().backendTimeTravelTo(0, hour)
    start.jumpToNotePage(noteTopology).deleteNote()
  }
)

When('I delete note {string}', (noteTopology: string) => {
  start.jumpToNotePage(noteTopology).deleteNote()
})

When(
  'I delete note {string} and leave references as dead wiki links',
  (noteTopology: string) => {
    start
      .jumpToNotePage(noteTopology)
      .deleteNoteAndLeaveReferencesAsDeadWikiLinks()
  }
)

When(
  'I delete note {string} and remove it from properties of references',
  (noteTopology: string) => {
    start
      .jumpToNotePage(noteTopology)
      .deleteNoteAndRemoveFromReferenceProperties()
  }
)

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

When('I undo {string} again', (undoType: string) => {
  start.assumeNotePage().undo(undoType)
})

When('I undo delete note to recover note {string}', (noteTitle: string) => {
  start.assumeNotePage().undo('delete note')
  start.assumeNotePage(noteTitle)
})

When(
  'I undo delete note to recover note {string} again',
  (noteTitle: string) => {
    start.assumeNotePage().undo('delete note')
    start.assumeNotePage(noteTitle)
  }
)

Then('there should be no more undo to do', () => {
  cy.get('.daisy-btn[title^="undo"]').should('not.exist')
})
