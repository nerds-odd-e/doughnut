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
    submit() {
      cy.findByRole("button", { name: "OK" }).click()
    },
    suggestingPositiveFeedbackForFineTuning() {
      cy.findByRole("button", { name: "👍 Good" }).click()
      this.submit()
    },

    suggestingNegativeFeedbackFineTuningExclusion() {
      cy.findByRole("button", { name: "👎 Bad" }).click()
      this.submit()
    },

    submittingNoFeedback() {
      this.submit()
    },

    inputComment(comment: string) {
      cy.get("#feedback-comment").type(comment)
    },
  }
}

const findQuestionWithStem = (stem: string) => {
  cy.findByText(stem)
  return currentQuestion(stem)
}

const expectSuccessMessageToBeShown = () => {
  cy.get(".suggestion-sent-successfully-message")
}

const expectFeedbackRequiredMessage = () => {
  cy.get(".feedback-required-message")
}

const expectFeedbackAlreadyExistMessage = () => {
  cy.get(".feedback-exist-message")
}

export {
  findQuestionWithStem,
  currentQuestion,
  expectFeedbackRequiredMessage,
  expectSuccessMessageToBeShown,
  expectFeedbackAlreadyExistMessage,
}
