import { pageIsNotLoading } from '../pageBase'
import { assumeMemoryTrackerPage } from './memoryTrackerPage'

const assumeAnsweredQuestionPage = () => {
  cy.get('body').should('be.visible')

  return {
    expectMCQAnswerToBeCorrect() {
      cy.get('[data-test="question-section"]').within(() => {
        cy.get('.is-correct.is-selected').should('exist')
      })
    },
    expectSpellingAnswerToBeCorrect() {
      cy.findByText('Correct!').should('exist')
      return this
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
      cy.findByTestId('accidental-match-alert')
        .should('be.visible')
        .and(
          'contain.text',
          `Your answer \`${answer}\` names another note — not correct for this review.`
        )
      cy.findByText(`Your answer \`${answer}\` is incorrect.`).should(
        'not.exist'
      )
      cy.findByText('Note under question').should('be.visible')
      cy.get('[data-test="note-title"]')
        .filter(`:contains("${reviewedNoteTitle}")`)
        .should('have.length.at.least', 1)
      cy.findByTestId('matched-notes-section')
        .scrollIntoView()
        .should('be.visible')
        .within(() => {
          cy.findByText('Matched note(s)').should('be.visible')
          cy.get('[data-test="note-title"]')
            .filter(`:contains("${matchedNoteTitle}")`)
            .should('have.length.at.least', 1)
        })
      return this
    },
    viewMemoryTracker() {
      pageIsNotLoading()
      cy.findByRole('button', { name: 'View Memory Tracker' })
        .should('be.visible')
        .click()
      pageIsNotLoading()
      cy.findByRole('heading', { name: 'Memory Tracker' }).should('be.visible')
      return assumeMemoryTrackerPage()
    },
    goToLastAnsweredQuestion: () => {
      pageIsNotLoading()
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
      pageIsNotLoading()
      cy.findByText('Note under question').should('be.visible')
      return assumeAnsweredQuestionPage()
    },
    confirmReAssimilation() {
      cy.contains('re-assimilate').should('be.visible')
      cy.findByRole('button', { name: 'OK' }).click()
    },
  }
}

export { assumeAnsweredQuestionPage }
