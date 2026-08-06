import { waitUntilAppIsNotBusy } from '../pageBase'

export function expectAccidentalMatchAlert(answer: string) {
  cy.findByTestId('accidental-match-alert')
    .scrollIntoView()
    .should('be.visible')
    .and(
      'contain.text',
      `Your answer \`${answer}\` names another note — not correct for this review.`
    )
}

export function expectOverlapTryAgainAlert() {
  cy.findByTestId('overlap-try-again-alert')
    .scrollIntoView()
    .should('be.visible')
    .and(
      'contain.text',
      "Correct, but we're looking for another answer — try again."
    )
  cy.findByTestId('overlap-try-again').scrollIntoView().should('be.visible')
}

export function expectNoMatchedNotesOrAccidentalMatch() {
  cy.findByTestId('matched-notes-section').should('not.exist')
  cy.findByTestId('accidental-match-alert').should('not.exist')
  cy.findByTestId('resolve-accidental-match').should('not.exist')
}

export function openResolveAndClickMatchedNoteCta(
  matchedNoteTitle: string,
  testIdPrefix: string,
  buttonLabel: string
) {
  cy.findByTestId('resolve-accidental-match')
    .scrollIntoView()
    .should('be.visible')
    .click()
  waitUntilAppIsNotBusy()
  cy.findByTestId('accidental-match-resolve-dialog')
    .should('be.visible')
    .and('contain.text', matchedNoteTitle)
    .within(() => {
      cy.get(`[data-testid^="${testIdPrefix}"]`)
        .should('be.visible')
        .and('contain.text', buttonLabel)
        .click()
    })
}
