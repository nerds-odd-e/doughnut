const goToLastResult = () => {
  cy.findByRole("button", { name: "view last result" }).click()

  return {
    expectLastAnswerToBeCorrect() {
      // checking the css name isn't the best solution
      // but the text changes
      cy.get(".alert-success").should("exist")
    },
  }
}

export { goToLastResult }
