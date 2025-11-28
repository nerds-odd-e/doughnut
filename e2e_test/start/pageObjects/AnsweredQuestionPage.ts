import { assumeNotePage } from './notePage'

const assumeAnsweredQuestionPage = () => {
  return {
    expectLastAnswerToBeCorrect() {
      // checking the css name isn't the best solution
      // but the text changes
      cy.get('.daisy-alert-success').should('exist')
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
