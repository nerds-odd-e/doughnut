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

    markAsGood() {
      question().find(".good-question .thumb-up-hollow").click()
    },

    unmarkAsGood() {
      question().find(".good-question .thumb-up-filled").click()
    },

    isMarkedAsGood() {
      question().find(".good-question .thumb-up-filled").should("be.visible")
    },

    isNotMarkedAsGood() {
      question().find(".good-question .thumb-up-hollow").should("be.visible")
    },

    markAsBad() {
      question().find(".bad-question .thumb-down-hollow").click()
    },
  }
}

const findQuestionWithStem = (stem: string) => {
  cy.findByText(stem)
  return currentQuestion(stem)
}

export { findQuestionWithStem, currentQuestion }
