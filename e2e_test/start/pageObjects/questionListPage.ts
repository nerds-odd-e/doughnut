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
      cy.wait('@deleteQuestion').then(({ response }) => {
        expect(response?.statusCode, 'delete question').to.equal(200)
      })
    },
    editQuestion(stem: string, row: Record<string, string>) {
      cy.intercept('PATCH', '**/api/predefined-questions/**').as(
        'updateQuestion'
      )
      cy.contains('.question-table tr', stem)
        .findByRole('button', { name: 'Edit question' })
        .click()
      for (const [key, value] of Object.entries(row)) {
        cy.findByLabelText(key).clear().invoke('val', value).trigger('input')
      }
      cy.findByRole('button', { name: 'Submit' }).click()
      cy.wait('@updateQuestion').then(({ response }) => {
        expect(response?.statusCode, 'update question').to.equal(200)
      })
    },
    expectNoQuestions() {
      cy.findByText('No questions')
    },
  }
}
