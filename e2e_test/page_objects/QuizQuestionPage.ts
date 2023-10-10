import { SuggestQuestionForFineTuningPage } from "./SuggestQuestionForFineTuningPage"

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

    suggestingThisQuestionForFineTuning() {
      question()
        .findByRole("button", {
          name: "send this question for fine tuning the question generation model",
        })
        .click()

      return SuggestQuestionForFineTuningPage()
    },
  }
}

const findQuestionWithStem = (stem: string) => {
  cy.findByText(stem)
  return currentQuestion(stem)
}

export { findQuestionWithStem, currentQuestion }
