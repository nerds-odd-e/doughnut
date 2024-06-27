import { addQuestionPage } from "./addQuestionPage"

const lineSection = (chainable: Cypress.Chainable<any>) => {
  return {
    deleteButton: () => {
      return chainable.findByRole("button").contains("Delete")
    },
    deleteQuestion() {
      this.deleteButton().click()
      cy.get('[data-test="confirm-delete-button"]').click()
      cy.get('[data-test="confirm-delete-button"]').should('not.be.visible');
    }
  }
}

export const questionListPage = () => {
  return {
    addQuestionPage,
    questionLine: (question: string) => {
      return lineSection(cy.findByText(question).parent("tr"))
    },
    expectQuestion(expectedQuestions: Record<string, string>[]) {
      expectedQuestions.forEach((row) => {
        cy.findByText(row["Question"]!)
        cy.findByText(row["Correct Choice"]!).then(($el) => {
          cy.wrap($el).should("have.class", "correct-choice")
        })
      })
    },
  }
}
