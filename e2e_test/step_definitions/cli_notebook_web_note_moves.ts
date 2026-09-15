/**
 * CLI notebook web note move journey: capture the original note identity before
 * the web move and reopen the same note route after CLI publication. Steps stay
 * one-line glue to `e2e_test/start` (testability + named router helpers, ADR 0005).
 */
import { Given, When } from '@badeball/cypress-cucumber-preprocessor'
import router from '../start/router'
import start from '../start'

Given('I capture the note id of {string}', (noteTitle: string) => {
  start
    .testability()
    .getInjectedNoteIdByTitle(noteTitle)
    .then((noteId: number) => {
      cy.wrap(noteId).as('capturedOriginalNoteId')
    })
})

When('I open the original note route', () => {
  cy.get<number>('@capturedOriginalNoteId').then((noteId) => {
    router().visitNamed('noteShow', { noteId })
  })
  start.waitUntilAppIsNotBusy()
})
