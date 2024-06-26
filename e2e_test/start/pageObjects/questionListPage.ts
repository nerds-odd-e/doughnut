import { addQuestionPage } from "./addQuestionPage"

const lineSection = (chainable: Cypress.Chainable<any>) => {
  return {
    editButton: () => {
      return chainable.find("button").contains("Edit")
    },
    deleteButton: () => {
      return chainable.find("button").contains("Delete")
    },
    deleteQuestion() {
      this.deleteButton().click()
      cy.findByRole("button", { name: "OK" }).click()
    }
  }
};

export const questionListPage = () => {
  return {
    addQuestionPage,
    questionLine: (question: string) => {
      return lineSection(cy.findByText(question).parent("tr"));
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
