import { addQuestionPage } from './addQuestionPage'

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
    deleteQuestion(stem: string) {
      cy.intercept('DELETE', '**/api/predefined-questions/**').as(
        'deleteQuestion'
      )
      cy.contains('.question-table tr', stem)
        .findByRole('button', { name: 'Delete question' })
        .click()
      cy.findByRole('button', { name: 'OK' }).click()
      cy.wait('@deleteQuestion').then(({ response }) => {
        expect(response?.statusCode, 'delete question').to.equal(200)
      })
    },
    expectQuestionNotInList(stem: string) {
      cy.get('body').then(($body) => {
        if ($body.find('.question-table').length > 0) {
          cy.get('.question-table').should('not.contain.text', stem)
        }
      })
    },
  }
}
