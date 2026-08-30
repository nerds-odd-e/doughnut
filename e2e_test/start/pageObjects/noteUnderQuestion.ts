import { waitUntilAppIsNotBusy } from '../pageBase'
import { assumeNotePage } from './notePage'

export function followNoteUnderQuestion(noteTitle: string) {
  cy.findByText('Note under question').should('be.visible')
  cy.contains('.note-under-question a', noteTitle).should('be.visible').click()
  waitUntilAppIsNotBusy()
  return assumeNotePage(noteTitle)
}
