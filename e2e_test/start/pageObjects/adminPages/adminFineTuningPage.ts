export function adminFineTuningPage() {
  return {
    triggerFineTuning() {
      cy.findByRole("button", {
        name: "Trigger Fine Tuning",
      }).click()
    },

    expectFineTuningExamplesCount(count: number) {
      cy.pageIsNotLoading()
      cy.get("tbody").contains("tr td", "Positive").should("have.length", count)
    },

    updateQuestionSuggestionAndChoice(
      originalQuestionStem: string,
      newQuestion: Record<string, string>,
    ) {
      cy.findByText(originalQuestionStem).parent().dblclick()
      cy.formField("Stem").clear().type(newQuestion["Question Stem"])
      cy.get("li input").first().clear().type(newQuestion["Choice A"])
      cy.findByRole("button", { name: "Save" }).click()
      cy.pageIsNotLoading()
      cy.findByText(newQuestion["Question Stem"])
    },

    expectExampleQuestions(questions: Record<string, string>[]) {
      this.expectFineTuningExamplesCount(questions.length)
      questions.forEach((expectation) => {
        cy.findByText(expectation["Question Stem"], { selector: "td" }).dblclick()
        if (expectation["Choices"]) {
          expectation["Choices"].split(", ").forEach((choice: string, index: number) => {
            cy.findByLabelText(`Choice ${index}`).invoke("val").should("eq", choice)
          })
        }
      })
    },
    duplicateNegativeQuestion(questionStem: string) {
      cy.findByText(questionStem).parent().findByRole("button", { name: "Duplicate" }).click()
    },

    expectString(numOfOccurrence: number, expectedString: string) {
      cy.findAllByText(expectedString).should("have.length", numOfOccurrence)
    },
  }
}
