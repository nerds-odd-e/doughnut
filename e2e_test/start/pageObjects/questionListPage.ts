import { addQuestionPage } from "./addQuestionPage"

export const questionListPage = () => {
  return {
    addQuestionPage,
    expectQuestion(expectedQuestions: Record<string, string>[]) {
      cy.get('.question-table tbody tr').should('have.length', expectedQuestions.length)

      expectedQuestions.forEach((row) => {
        cy.findByText(row["Question"]!)
        cy.findByText(row["Correct Choice"]!).then(($el) => {
          cy.wrap($el).should("have.class", "correct-choice")
        })
      })
    },
    deleteQuestion(question: string) {
      cy.contains('tr', question).within(() => {
      cy.get('input[type="checkbox"]').click()
        })

      cy.get('button[title="Delete Question"]').click()
    }
  }
}
