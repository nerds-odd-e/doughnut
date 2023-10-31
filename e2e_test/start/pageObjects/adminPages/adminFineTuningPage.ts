import { assumeDownloadedJSONL } from "./questionGenerationTrainingData"

export function adminFineTuningPage() {
  return {
    downloadAIQuestionTrainingData() {
      cy.findByRole("button", {
        name: "Download Positive Feedback Question Generation Training Data",
      }).click()

      return assumeDownloadedJSONL("fineTuningData.jsonl")
    },

    uploadFineTuningTrainingData() {
      cy.findByRole("button", {
        name: "Upload Fine Tuning Training Data",
      }).click()
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
      cy.get("li input").first().clear().type(newQuestion["Choice A"])
      cy.findByRole("button", { name: "Save" }).click()
      cy.pageIsNotLoading()
      cy.findByText(newQuestion["Question Stem"])
    },

    duplicateNegativeQuestion(questionStem: string) {
      cy.findByText(questionStem).parent().findByRole("button", { name: "Duplicate" }).click()
    },

    expectString(numOfOccurrence: number, expectedString: string) {
      cy.findAllByText(expectedString).should("have.length", numOfOccurrence)
    },
  }
}
