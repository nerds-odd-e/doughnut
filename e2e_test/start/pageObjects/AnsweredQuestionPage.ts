import { assumeNotePage } from './notePage'

const assumeAnsweredQuestionPage = () => {
  return {
    expectLastAnswerToBeCorrect() {
      // checking the css name isn't the best solution
      // but the text changes
      cy.get('.alert-success').should('exist')
    },
    showMemoryTracker(noteTopic?: string) {
      cy.findByText('Note under question:').click()
      return assumeNotePage(noteTopic).memoryTracker()
    },
    goToLastResult: () => {
      cy.findByRole('button', { name: 'view last result' }).click()
      return assumeAnsweredQuestionPage()
    },
  }
}

export { assumeAnsweredQuestionPage }
