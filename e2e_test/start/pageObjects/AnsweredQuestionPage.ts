import { waitUntilAppIsNotBusy } from '../pageBase'
import { form } from '../forms'
import { assumeMemoryTrackerPage } from './memoryTrackerPage'

function expectAccidentalMatchAlert(answer: string) {
  cy.findByTestId('accidental-match-alert')
    .scrollIntoView()
    .should('be.visible')
    .and(
      'contain.text',
      `Your answer \`${answer}\` names another note — not correct for this review.`
    )
}

function expectOverlapTryAgainAlert() {
  cy.findByTestId('overlap-try-again-alert')
    .scrollIntoView()
    .should('be.visible')
    .and(
      'contain.text',
      "Correct, but we're looking for another answer — try again."
    )
  cy.findByTestId('overlap-try-again').should('be.visible')
}

function expectNoMatchedNotesOrAccidentalMatchOnOverlap() {
  cy.findByTestId('matched-notes-section').should('not.exist')
  cy.findByTestId('accidental-match-alert').should('not.exist')
}

function expectMatchedNoteInSection(matchedNoteTitle: string) {
  cy.findByTestId('matched-notes-section')
    .scrollIntoView()
    .should('be.visible')
    .within(() => {
      cy.findByText('Matched note(s)').should('be.visible')
      cy.get('[data-test="note-title"]')
        .filter(`:contains("${matchedNoteTitle}")`)
        .should('have.length.at.least', 1)
    })
}

const assumeAnsweredQuestionPage = () => {
  cy.get('body').should('be.visible')

  const self = {
    expectMCQAnswerToBeCorrect() {
      cy.get('[data-test="question-section"]').within(() => {
        cy.get('.is-correct.is-selected').should('exist')
      })
    },
    expectSpellingAnswerToBeCorrect() {
      cy.findByText('Correct!').should('exist')
      return self
    },
    expectMCQAnswerToBeIncorrect(answer: string) {
      cy.contains('button', answer).should('have.class', 'is-selected')
      cy.contains('button', answer).should('not.have.class', 'is-correct')
      cy.get('.is-correct').should('exist')
    },
    expectSpellingAnswerToBeIncorrect(answer: string) {
      cy.findByText(`Your answer \`${answer}\` is incorrect.`).should('exist')
    },
    expectAccidentalMatchReveal(
      answer: string,
      reviewedNoteTitle: string,
      matchedNoteTitle: string
    ) {
      expectAccidentalMatchAlert(answer)
      cy.findByText(`Your answer \`${answer}\` is incorrect.`).should(
        'not.exist'
      )
      cy.findByText('Note under question').should('be.visible')
      cy.get('[data-test="note-title"]')
        .filter(`:contains("${reviewedNoteTitle}")`)
        .should('have.length.at.least', 1)
      expectMatchedNoteInSection(matchedNoteTitle)
      return self
    },
    openLinkToMatchedNote(matchedNoteTitle: string) {
      expectMatchedNoteInSection(matchedNoteTitle)
      cy.findByTestId('matched-notes-section')
        .find('[data-testid^="link-to-matched-note-"]')
        .should('be.visible')
        .and('contain.text', 'Link to this note')
        .click()
      cy.contains('Link to:')
        .should('be.visible')
        .parent()
        .should('contain.text', matchedNoteTitle)
      cy.findByPlaceholderText('Search').should('not.exist')
      cy.findByRole('button', { name: 'Insert as a wiki link' }).should(
        'not.exist'
      )
      return self
    },
    linkMatchedNoteAsProperty(matchedNoteTitle: string) {
      self.openLinkToMatchedNote(matchedNoteTitle)
      cy.findByRole('button', {
        name: 'Add wiki link as a new property',
      }).click()
      waitUntilAppIsNotBusy()
      return self
    },
    linkMatchedNoteAsRelationship(
      matchedNoteTitle: string,
      relationType: string
    ) {
      self.openLinkToMatchedNote(matchedNoteTitle)
      cy.findByRole('button', {
        name: 'Add a new relationship note',
      }).click()
      form.getField('Relation Type').clickOption(relationType)
      waitUntilAppIsNotBusy()
      return self
    },
    expectStillOnAccidentalMatchResult(
      answer: string,
      matchedNoteTitle: string
    ) {
      cy.url().should('include', '/recall')
      expectAccidentalMatchAlert(answer)
      expectMatchedNoteInSection(matchedNoteTitle)
      return self
    },
    expectOverlapTryAgainForSpelling() {
      expectOverlapTryAgainAlert()
      return self
    },
    expectNoMatchedNotesOrAccidentalMatchOnOverlap() {
      expectNoMatchedNotesOrAccidentalMatchOnOverlap()
      return self
    },
    clickOverlapTryAgain() {
      cy.findByTestId('overlap-try-again').scrollIntoView().click()
      waitUntilAppIsNotBusy()
      return self
    },
    viewMemoryTracker() {
      waitUntilAppIsNotBusy()
      cy.findByRole('button', { name: 'View Memory Tracker' })
        .should('be.visible')
        .click()
      waitUntilAppIsNotBusy()
      cy.findByRole('heading', { name: 'Memory Tracker' }).should('be.visible')
      return assumeMemoryTrackerPage()
    },
    goToLastAnsweredQuestion: () => {
      waitUntilAppIsNotBusy()
      cy.get('body').then(($body) => {
        if ($body.text().includes('Note under question')) {
          return
        }
        cy.get(
          'button[title="view last answered question"], button[title="view previous answered question"]'
        )
          .filter(':visible')
          .first()
          .should('not.be.disabled')
          .click()
      })
      waitUntilAppIsNotBusy()
      cy.findByText('Note under question').should('be.visible')
      return assumeAnsweredQuestionPage()
    },
    confirmReAssimilation() {
      cy.contains('re-assimilate').should('be.visible')
      cy.findByRole('button', { name: 'OK' }).click()
    },
  }
  return self
}

export { assumeAnsweredQuestionPage }
