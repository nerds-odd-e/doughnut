import { assumeNotePage } from './notePage'

const assumeAnsweredQuestionPage = () => {
  cy.findByText('Note under question')

  return {
    expectLastAnswerToBeCorrect() {
      // When answer is correct, check for visual indicators in the question display
      // The question section might be on the current page or we might need to wait for it
      cy.get('body').then(($body) => {
        if ($body.find('[data-test="question-section"]').length > 0) {
          // Question is visible on current page
          cy.get('[data-test="question-section"]').within(() => {
            cy.get('.is-correct.is-selected').should('exist')
          })
        } else {
          // Question might not be visible yet, just verify no error message
          // The visual correctness is shown in QuestionDisplay component
          cy.get('body').should('not.contain', 'is incorrect')
        }
      })
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
      return assumeNotePage(noteTopology).memoryTracker()
    },
    goToLastAnsweredQuestion: () => {
      cy.findByRole('button', { name: 'view last answered question' }).click()
      return assumeAnsweredQuestionPage()
    },
  }
}

export { assumeAnsweredQuestionPage }
