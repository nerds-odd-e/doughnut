import { assumeNotePage } from './notePage'

const assumeAnsweredQuestionPage = () => {
  return {
    expectLastAnswerToBeCorrect() {
      // checking the css name isn't the best solution
      // but the text changes
      cy.get('.alert-success').should('exist')
    },
    showReviewPoint(noteTopic?: string) {
      cy.findByText('Note reviewed:').click()
      return assumeNotePage(noteTopic).reviewPoint()
    },
    goToLastResult: () => {
      cy.findByRole('button', { name: 'view last result' }).click()
      return assumeAnsweredQuestionPage()
    },
  }
}

export { assumeAnsweredQuestionPage }
