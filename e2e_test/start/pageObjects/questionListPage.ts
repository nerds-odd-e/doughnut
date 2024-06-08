export const questionListPage = () => {
  return {
    addQuestion(row: Record<string, string>) {
      cy.findByRole("button", { name: "Add Question" }).click()
      cy.findByLabelText("Stem").type(row["Question"]!)
      cy.findByLabelText("Choice 0").type(row["Incorrect Choice 1"]!)
      cy.findByLabelText("Choice 1").type(row["Incorrect Choice 2"]!)
      cy.findByRole("button", { name: "+" }).click()
      cy.findByLabelText("Choice 2").type(row["Correct Choice"]!)
      cy.findByLabelText("Correct Choice Index").clear().type("2")
      cy.findByRole("button", { name: "Submit" }).click()
    },
    expectQuestion(row: Record<string, string>) {
      cy.findByText(row["Question"]!)
      cy.findByText(row["Correct Choice"]!).then(($el) => {
        cy.wrap($el).should("have.class", "correct-choice")
      })
    },
  }
}
