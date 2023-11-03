export const assumeAdminEvaluateQuestionModelPage = () => {
  return {
    triggerEvaluate() {
      cy.findAllByText("Trigger").first().click();
    },
    newScoreWillBe(score: string) {
      cy.findByText("66%")
    }
  }
}
