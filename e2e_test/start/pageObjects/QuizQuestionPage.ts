import { SuggestQuestionForFineTuningPage } from "./SuggestQuestionForFineTuningPage"

const assumeQuestionPage = (stem?: string) => {
  if (stem) {
    cy.findByText(stem)
  }
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

    suggestingThisQuestionForFineTuning() {
      question()
        .findByRole("button", {
          name: "send this question for fine tuning the question generation model",
        })
        .click()

      return SuggestQuestionForFineTuningPage()
    },
    expectFeedbackRequiredMessage: () => {
      cy.get(".feedback-required-message")
    },
  }
}

export { assumeQuestionPage }
