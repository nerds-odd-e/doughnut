import { waitUntilAppIsNotBusy } from '../pageBase'
import { assumeNotePage } from './notePage'

export function followNoteUnderQuestion(noteTitle: string) {
  cy.findByText('Note under question').should('be.visible')
  cy.contains('.note-under-question a', noteTitle).should('be.visible').click()
  waitUntilAppIsNotBusy()
  return assumeNotePage(noteTitle)
}

export function expectNoteUnderQuestionFocusedProperty(
  propertyKey: string,
  propertyValue: string
) {
  const expected = `Focused property: ${propertyKey}: ${propertyValue}`
  cy.get('.note-under-question [data-testid="focused-property-indicator"]')
    .invoke('text')
    .should((text) => {
      const actual = text.replace(/\s+/g, ' ').trim()
      expect(
        actual,
        `Expected the note under question to show "${expected}", but found "${actual}"`
      ).to.equal(expected)
    })
}
