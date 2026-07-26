import { addQuestionPage, deleteQuestion } from './addQuestionPage'

export const questionListPage = () => {
  return {
    addQuestionPage: () => {
      cy.findByRole('button', { name: 'Add Question' }).click()
      return addQuestionPage()
    },
    expectQuestion(expectedQuestions: Record<string, string>[]) {
      expectedQuestions.forEach((row) => {
        cy.findByText(row.Question!)
        cy.findByText(row['Correct Choice']!).then(($el) => {
          cy.wrap($el).should('have.class', 'correct-choice')
        })
      })
    },
    deleteQuestion(questionStem: string) {
      deleteQuestion(questionStem)
    },
    expectQuestionNotInList(questionStem: string) {
      cy.get('body').then(($body) => {
        if ($body.find('.question-table').length > 0) {
          cy.get('.question-table').should('not.contain.text', questionStem)
        }
      })
    },
  }
}
