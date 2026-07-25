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
    deleteQuestion(questionStem: string) {
      cy.findByText(questionStem)
        .parents('tr')
        .within(() => {
          cy.findByRole('button', { name: 'Delete question' }).click()
        })
    },
    expectNoQuestions() {
      cy.findByText('No questions').should('be.visible')
    },
  }
}
