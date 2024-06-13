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

    isNotDisabled() {
      question().find("ol button").should("not.be.disabled")
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
    expectQuestionGeneratedByAI(expectedQuestions: Record<string, string>) {
      cy.findByLabelText("Stem").should("have.value", expectedQuestions["Question"]!)
      cy.findByLabelText("Choice 0").should("have.value", expectedQuestions["Choice 0"]!)
      cy.findByLabelText("Choice 1").should("have.value", expectedQuestions["Choice 1"]!)
      cy.findByLabelText("Choice 2").should("have.value", expectedQuestions["Choice 2"]!)
      cy.findByLabelText("Correct Choice Index").should(
        "have.value",
        expectedQuestions["Correct Choice"]!,
      )
    },
  }
}

export { assumeQuestionPage }
