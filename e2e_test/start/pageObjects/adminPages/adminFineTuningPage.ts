import { assumeDownloadedJSONL } from "./questionGenerationTrainingData"

export function adminFineTuningPage() {
  return {
    downloadAIQuestionTrainingData() {
      cy.findByRole("button", {
        name: "Download Positive Feedback Question Generation Training Data",
      }).click()

      return assumeDownloadedJSONL("fineTuningData.jsonl")
    },

    downloadFeedbackForEvaluationModel() {
      cy.findByRole("button", {
        name: "Download Evaluation Training Data",
      }).click()
      return assumeDownloadedJSONL("evaluationData.jsonl")
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
