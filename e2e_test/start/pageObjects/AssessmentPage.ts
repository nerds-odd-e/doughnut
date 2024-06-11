export const assumeAssessmentPage = (notebook?: string) => {
  if (notebook) {
    cy.findByRole("heading", { name: `Assessment For ${notebook}` })
  }

  return {
    expectQuestion(stem: string) {
      cy.findByText(stem)
      return {
        answer(answer: string) {
          cy.findByText(answer).click()
        },
      }
    },
    expectEndOfAssessment(expectedScore: string) {
      cy.findByText(expectedScore)
    },
    goToAddQuestion() {
      cy.findByRole("button", { name: "more options" }).click()
      cy.findByRole("button", { name: "Questions for the note" }).click()
      cy.findByRole("button", { name: "Add Question" }).click()
    },
  }
}
