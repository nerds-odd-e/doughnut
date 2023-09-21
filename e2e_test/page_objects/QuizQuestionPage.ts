const currentQuestion = (stem?: string) => {
  const question = () => (stem ? cy.findByText(stem).parent() : cy)
  const getChoice = (choice: string) => question().findByText(choice)
  return {
    isDisabled() {
      question().find("ol button").should("be.disabled")
    },

    expectChoiceToBe(choice: string, correctness: "correct" | "incorrect") {
      getChoice(choice).click()
      getChoice(choice).parent().invoke("attr", "class").should("contain", `is-${correctness}`)
    },

    markQuestion() {
      question()
        .findByRole("button", {
          name: "send this question for fine tuning the question generation model",
        })
        .click()
      cy.findByRole("button", { name: "OK" }).click()
      cy.pageIsNotLoading()
    },

    enterComment(comment: string) {
      question()
        .findByRole("button", {
          name: "send this question for fine tuning the question generation model",
        })
        .click()
      cy.findByPlaceholderText("Add a comment about the question").type(comment)
      cy.findByRole("button", { name: "OK" }).click()
      cy.pageIsNotLoading()
    },

    suggestedQuestion(option: string, suggestion: string) {
      question()
        .findByRole("button", {
          name: "send this question for fine tuning the question generation model",
        })
        .click()
      cy.get(`textarea[name='suggested${option}'`).type(suggestion)
      cy.findByRole("button", { name: "OK" }).click()
      cy.pageIsNotLoading()
    },
  }
}

const findQuestionWithStem = (stem: string) => {
  cy.findByText(stem)
  return currentQuestion(stem)
}

export { findQuestionWithStem, currentQuestion }
