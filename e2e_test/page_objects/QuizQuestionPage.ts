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

    suggestingGoodQuestionForFineTuning() {
      cy.get(".positive-feedback-btn").click()
      cy.get(".suggest-fine-tuning-ok-btn").click()
    },

    suggestingBadQuestionForFineTuning() {
      cy.get(".negative-feedback-btn").click()
      cy.get(".suggest-fine-tuning-ok-btn").click()
    },
  }
}

const findQuestionWithStem = (stem: string) => {
  cy.findByText(stem)
  return currentQuestion(stem)
}

export { findQuestionWithStem, currentQuestion }
