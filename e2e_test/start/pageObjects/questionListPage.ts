import { addQuestionPage } from './addQuestionPage'

export const questionListPage = () => {
  return {
    addQuestionPage,
    removeQuestion(stem: string) {
      cy.findByText(stem).parent('tr').findByText('Remove').click()
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
