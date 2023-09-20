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
      cy.get("textarea[placeholder='Add a comment about the question']").type(comment)
      cy.findByRole("button", { name: "OK" }).click()
      cy.pageIsNotLoading()
    },

    suggestedQuestion(option: string, suggestedQuestion: string) {
      question()
        .findByRole("button", {
          name: "send this question for fine tuning the question generation model",
        })
        .click()
      //cy.findByRole("textbox", { name: "suggestedquestion" }).type(suggestedQuestion)
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
