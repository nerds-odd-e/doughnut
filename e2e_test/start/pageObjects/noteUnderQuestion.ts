import { waitUntilAppIsNotBusy } from '../pageBase'
import { assumeNotePage } from './notePage'
import { notePropertyItemHref } from '@/routes/noteShowLocation'
import testability from '../testability'
import { assumeAnsweredQuestionPage } from './AnsweredQuestionPage'

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

export function followReviewedValue(label: string) {
  cy.findByTestId('reviewed-property-value')
    .contains('a.donut-wiki-link', label)
    .should('be.visible')
    .click()
  waitUntilAppIsNotBusy()
  return assumeNotePage(label)
}

export function returnToRecallAnswer() {
  cy.go('back')
  waitUntilAppIsNotBusy()
  return assumeAnsweredQuestionPage().goToLastAnsweredQuestion()
}
