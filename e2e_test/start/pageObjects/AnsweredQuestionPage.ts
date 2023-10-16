const assumeAnsweredQuestionPage = () => {
  return {
    expectLastAnswerToBeCorrect() {
      // checking the css name isn't the best solution
      // but the text changes
      cy.get(".alert-success").should("exist")
    },
    showReviewPoint(noteTopic?: string) {
      cy.findByText("Review Point:").click()
      if (noteTopic) {
        cy.findNoteTopic(noteTopic)
      }
      return {
        expectReviewPointInfo(attrs: { [key: string]: string }) {
          for (const k in attrs) {
            cy.contains(k).findByText(attrs[k]).should("be.visible")
          }
        },
        removeReviewPointFromReview() {
          cy.findByRole("button", { name: "remove this note from review" }).click()
          cy.findByRole("button", { name: "OK" }).click()
          cy.findByText("This review point has been removed from reviewing.")
        },
      }
    },
    goToLastResult: () => {
      cy.findByRole("button", { name: "view last result" }).click()
      return assumeAnsweredQuestionPage()
    },
  }
}

export { assumeAnsweredQuestionPage }
