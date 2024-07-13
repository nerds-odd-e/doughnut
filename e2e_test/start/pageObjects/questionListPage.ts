import { addQuestionPage } from './addQuestionPage'
import { editQuestionPage } from './editQuestionPage'

export const questionListPage = () => {
  return {
    addQuestionPage,
    editQuestionPage,
    expectQuestion(expectedQuestions: Record<string, string>[]) {
      expectedQuestions.forEach((row) => {
        cy.findByText(row.Question!)
        cy.findByText(row['Correct Choice']!).then(($el) => {
          cy.wrap($el).should('have.class', 'correct-choice')
        })
      })
    },
  }
}
