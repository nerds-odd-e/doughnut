export const SuggestQuestionForFineTuningPage = () => {
  cy.contains("will make this note and question visible to admin. Are you sure?")
  return {
    confirm() {
      cy.findByRole("button", { name: "OK" }).click()
      cy.pageIsNotLoading()
    },
    comment(comment: string) {
      cy.formField("Comment").type(comment)
      return this
    },
    suggestingPositiveFeedbackForFineTuning() {
      cy.findByRole("button", { name: "👍 Good" }).click()
      this.confirm()
    },
    suggestingNegativeFeedbackFineTuningExclusion() {
      cy.findByRole("button", { name: "👎 Bad" }).click()
      this.confirm()
    },
    submittingNoFeedback() {
      this.confirm()
    },
  }
}
