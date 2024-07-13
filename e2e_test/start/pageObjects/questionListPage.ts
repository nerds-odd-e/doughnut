import { addQuestionPage } from "./addQuestionPage"

export const questionListPage = () => {
  return {
    addQuestionPage,
    expectQuestion(expectedQuestions: Record<string, string>[]) {
      expectedQuestions.forEach((row) => {
        cy.findByText(row["Question"]!)
        cy.findByText(row["Correct Choice"]!).then(($el) => {
          cy.wrap($el).should("have.class", "correct-choice")
        })
      })
    },
    expectNoQuestion(question: string) {
      cy.findByText(question).should("not.exist")
    }
  }
}
