import { waitUntilAppIsNotBusy } from '../pageBase'

function expectAccidentalMatchAlert(answer: string) {
  cy.findByTestId('accidental-match-alert')
    .scrollIntoView()
    .should('be.visible')
    .and(
      'contain.text',
      `Your answer \`${answer}\` names another note — not correct for this review.`
    )
}

export function expectAccidentalMatchRevealForNotes(
  answer: string,
  reviewedNoteTitle: string,
  matchedNoteTitle: string
) {
  expectAccidentalMatchAlert(answer)
  cy.findByTestId('accidental-match-answer-link')
    .should('be.visible')
    .and('have.text', answer)
    .and('have.attr', 'href')
    .and('match', /\/n\d+/)
  cy.findByText(`Your answer \`${answer}\` is incorrect.`).should('not.exist')
  cy.findByTestId('resolve-accidental-match')
    .scrollIntoView()
    .should('be.visible')
    .and('contain.text', 'Resolve accidental match')
  cy.get('.note-under-question')
    .should('be.visible')
    .and('contain.text', 'Note under question')
    .and('contain.text', reviewedNoteTitle)
  cy.findByTestId('matched-notes-section').should('not.exist')

  cy.findByTestId('resolve-accidental-match').click()
  waitUntilAppIsNotBusy()
  cy.findByTestId('accidental-match-resolve-dialog')
    .should('be.visible')
    .and('contain.text', 'English practice')
    .and('contain.text', 'largely overlaps with the current note')
    .within(() => {
      cy.contains('a', matchedNoteTitle).should('be.visible')
      cy.findByTestId('resolve-overlap-explanation').should('be.visible')
    })
  cy.get('.close-button').filter(':visible').first().click()
  cy.findByTestId('accidental-match-resolve-dialog').should('not.exist')

  expectAccidentalMatchAlert(answer)
  cy.get('.note-under-question')
    .should('be.visible')
    .and('contain.text', reviewedNoteTitle)
}
