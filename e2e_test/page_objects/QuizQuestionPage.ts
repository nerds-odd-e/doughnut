const currentQuestion = () => {
  const question = () => cy.get("@currentQuestion")

  return {
    isDisabled() {
      question().find("ol button").should("be.disabled")
    },

    expectChoiceToBe(choice: string, correctness: "correct" | "incorrect") {
      question()
        .findByText(choice)
        .click()
        .parent()
        .invoke("attr", "class")
        .should("contain", `is-${correctness}`)
    },
  }
}

const findQuestionWithStem = (stem: string) => {
  cy.findByText(stem).parent().as("currentQuestion")
  return currentQuestion()
}

export { findQuestionWithStem, currentQuestion }
