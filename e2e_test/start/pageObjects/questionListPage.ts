export const questionListPage = () => {
  return {
    addQuestion(row: Record<string, string>) {
      cy.findByRole("button", { name: "Add Question" }).click()
      cy.get("label").contains("Question:").next().as("questionTextarea")
      cy.get("@questionTextarea").type(row?.["Question"] as string)
      cy.get("label").contains("Option 1 (Correct Answer)").next().as("questionTextarea")
      cy.get("@questionTextarea").type(row?.["Correct Choice"] as string)
      cy.get("label").contains("Option 2").next().as("questionTextarea")
      cy.get("@questionTextarea").type(row?.["Incorrect Choice 1"] as string)
      cy.get("button").contains("+").click()
      cy.get("label").contains("Option 3").next().as("questionTextarea")
      cy.get("@questionTextarea").type(row?.["Incorrect Choice 2"] as string)
      cy.get("button").contains("Submit").click()
    },
    expectQuestion(_row: Record<string, string>) {
      cy.get(".question-table").should("exist")
    },
  }
}
