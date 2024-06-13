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
    expectQuestion(expectedQuestions: Record<string, string>[]) {
      expectedQuestions.forEach((row) => {
        cy.findByText(row["Question"]!)
        cy.findByText(row["Correct Choice"]!).then(($el) => {
          cy.wrap($el).should("have.class", "correct-choice")
        })
      })
    },
    expectApprovedQuestion(expectedQuestions: Record<string, boolean>[]) {
      expectedQuestions.forEach((row) => {
        const dynamicId = "checkbox-" + row["Question"]!
        const checkbox = document.getElementById(`#${dynamicId}`) as HTMLInputElement
        if (!checkbox) {
        }
      })
    },
    fillQuestion(row: Record<string, string>) {
      cy.findByRole("button", { name: "Add Question" }).click()
      cy.findByLabelText("Stem").type(row["Question"]!)
      cy.findByLabelText("Choice 0").type(row["Incorrect Choice 1"]!)
      cy.findByLabelText("Choice 1").type(row["Incorrect Choice 2"]!)
      cy.findByRole("button", { name: "+" }).click()
      cy.findByLabelText("Choice 2").type(row["Correct Choice"]!)
      cy.findByLabelText("Correct Choice Index").clear().type("2")
    },
  }
}
