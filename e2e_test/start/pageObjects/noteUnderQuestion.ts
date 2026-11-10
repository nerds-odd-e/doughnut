import { waitUntilAppIsNotBusy } from '../pageBase'
import { assumeNotePage } from './notePage'
import { notePropertyItemHref } from '@/routes/noteShowLocation'
import testability from '../testability'

export function followNoteUnderQuestion(noteTitle: string) {
  cy.findByText('Note under question').should('be.visible')
  cy.contains('.note-under-question .breadcrumb-wrapper a', noteTitle)
    .should('be.visible')
    .click()
  waitUntilAppIsNotBusy()
  return assumeNotePage(noteTitle)
}

export function followReviewedProperty(propertyKey: string) {
  cy.findByTestId('reviewed-property-link')
    .should('have.text', propertyKey)
    .click()
  waitUntilAppIsNotBusy()
  return assumeNotePage()
}

export function expectReviewedPropertyItem(
  noteTitle: string,
  propertyKey: string,
  propertyValue: string
) {
  testability()
    .getInjectedNoteIdByTitle(noteTitle)
    .then((noteId: number) => {
      cy.findByTestId('reviewed-property-link').should(
        'have.attr',
        'href',
        notePropertyItemHref(noteId, propertyKey, {
          value: propertyValue,
          occurrence: 1,
        })
      )
    })
}
