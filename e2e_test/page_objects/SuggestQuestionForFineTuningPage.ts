export const SuggestQuestionForFineTuningPage = () => {
  cy.contains("will make this note and question visible to admin. Are you sure?")
  return {
    confirm() {
      cy.findByRole("button", { name: "OK" }).click()
      cy.pageIsNotLoading()
    },
    comment(comment: string) {
      cy.findByPlaceholderText("Add a comment about the question").type(comment)
      return this
    },
    changeQuestion(newQuestion: Record<string, string>) {
      cy.get(`textarea[name='suggestedQuestionText'`).type(newQuestion["Question Stem"])
      return this
    },
  }
}
