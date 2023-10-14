import { quesionGenerationTrainingData } from "./questionGenerationTrainingData"

export function adminFineTuningPage() {
  return {
    downloadAIQuestionTrainingData() {
      cy.findByRole("button", {
        name: "Download Positive Feedback training data for question generation ",
      }).click()

      return quesionGenerationTrainingData()
    },
    downloadFeedbackForEvaluationModel() {
      cy.findByRole("button", {
        name: "Download Evaluation Training Data",
      }).click()
      const downloadFilename = `${Cypress.config("downloadsFolder")}/evaluationData.jsonl`

      return {
        expectNumberOfRecords(count: number) {
          cy.readFile(downloadFilename)
            .then((content) => (content.match(/id/g) || []).length)
            .should("eq", count)
          return this
        },
      }
    },

    updateQuestionSuggestionAndChoice(
      originalQuestionStem: string,
      newQuestion: Record<string, string>,
    ) {
      cy.findByText(originalQuestionStem).parent().dblclick()
      cy.formField("Stem").clear().type(newQuestion["Question Stem"])
      cy.get("#choice-0").clear().type(newQuestion["Choice A"])
      cy.findByRole("button", { name: "Save" }).click()
      cy.pageIsNotLoading()
      cy.findByText(newQuestion["Question Stem"])
    },

    duplicateNegativeQuestion() {
      cy.get("#duplicate-0").click()
    },

    expectString(numOfOccurrence: number, expectedString: string) {
      cy.findAllByText(expectedString).should("have.length", numOfOccurrence)
    },

    expectUnableToDuplicate(_originalQuestionStem: string) {
      cy.findByText(_originalQuestionStem)
        .parent("tr")
        .contains("button", "Duplicate")
        .should("not.exist")
    },

    identifyDuplicatedRecord() {
      cy.get("#is-duplicated-0").contains("No")
      cy.get("#is-duplicated-1").contains("Yes")
    },
  }
}
