import { addQuestionPage } from './addQuestionPage'
import { editQuestionPage } from './editQuestionPage'

export const questionListPage = () => {
  return {
    addQuestionPage,
    editQuestionPage,
    deleteQuestion(questionNo: number) {
      cy.findAllByRole('button', { name: 'Delete' })
        .eq(questionNo - 1)
        .click()
    },
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
