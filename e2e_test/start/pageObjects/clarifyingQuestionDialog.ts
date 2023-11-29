export const assumeClarifyingQuestionDialog = (question: string) => {
  cy.findByText(question)
  return {
    answer: (answer: string) => {
      cy.findByLabelText("Answer To Ai").type(answer)
      cy.findByText("Send").click()
    },
    close: () => {
      cy.get("button.close-button").click()
    },
  }
}
