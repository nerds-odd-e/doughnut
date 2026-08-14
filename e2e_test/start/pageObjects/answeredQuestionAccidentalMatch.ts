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

export function expectAccidentalMatchRevealForNotes(
  answer: string,
  reviewedNoteTitle: string,
  matchedNoteTitles: string[]
) {
  expectAccidentalMatchAlert(answer)
  cy.findByText(`Your answer \`${answer}\` is incorrect.`).should('not.exist')
  cy.findByTestId('resolve-accidental-match')
    .scrollIntoView()
    .should('be.visible')
    .and('contain.text', 'Resolve accidental match')
  cy.findByText('Note under question').should('be.visible')
  cy.get('[data-test="note-title"]')
    .filter(`:contains("${reviewedNoteTitle}")`)
    .should('have.length.at.least', 1)
  cy.findByTestId('matched-notes-section').should('not.exist')

  cy.findByTestId('resolve-accidental-match').click()
  waitUntilAppIsNotBusy()
  cy.findByTestId('accidental-match-resolve-dialog')
    .should('be.visible')
    .and('contain.text', 'English practice')
    .and('contain.text', 'largely overlaps with the current note')
    .within(() => {
      for (const matchedNoteTitle of matchedNoteTitles) {
        cy.contains('a', matchedNoteTitle).should('be.visible')
      }
      cy.findByTestId('resolve-overlap-explanation').should('be.visible')
    })
  cy.get('.close-button').filter(':visible').first().click()
  cy.findByTestId('accidental-match-resolve-dialog').should('not.exist')

  expectAccidentalMatchAlert(answer)
  cy.get('[data-test="note-title"]')
    .filter(`:contains("${reviewedNoteTitle}")`)
    .should('have.length.at.least', 1)
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
