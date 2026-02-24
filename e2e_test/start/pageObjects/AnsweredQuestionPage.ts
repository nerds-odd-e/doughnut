import { assumeNotePage } from './notePage'

const assumeAnsweredQuestionPage = () => {
  // Note under question might not always be present immediately
  // Wait for the page to load and check if it exists
  cy.get('body').should('be.visible')

  return {
    expectMCQAnswerToBeCorrect() {
      // Multiple choice question - check that the selected answer is correct
      cy.get('[data-test="question-section"]').within(() => {
        cy.get('.is-correct.is-selected').should('exist')
      })
    },
    expectSpellingAnswerToBeCorrect() {
      // Spelling question - check for the success message
      cy.findByText('Correct!').should('exist')
    },
    expectMCQAnswerToBeIncorrect(answer: string) {
      // Multiple choice question - check visual indicators
      cy.contains('button', answer).should('have.class', 'is-selected')
      cy.contains('button', answer).should('not.have.class', 'is-correct')
      // Check that the correct answer is visually marked (green background)
      cy.get('.is-correct').should('exist')
    },
    expectSpellingAnswerToBeIncorrect(answer: string) {
      // Spelling question - check for the text message
      cy.findByText(`Your answer \`${answer}\` is incorrect.`).should('exist')
    },
    showMemoryTracker(noteTopology?: string) {
      cy.findByText('Note under question')
        .parent()
        .within(() => {
          cy.get('a').last().click()
        })
      return assumeNotePage(noteTopology).moreOptions()
    },
    goToLastAnsweredQuestion: () => {
      cy.findByRole('button', { name: 'view last answered question' }).click()
      return assumeAnsweredQuestionPage()
    },
    confirmReAssimilation() {
      cy.contains(
        'You have answered this note incorrectly too many times'
      ).should('be.visible')
      cy.findByRole('button', { name: 'OK' }).click()
    },
  }
}

export { assumeAnsweredQuestionPage }
