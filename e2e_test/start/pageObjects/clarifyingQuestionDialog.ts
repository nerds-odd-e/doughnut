export const assumeClarifyingQuestionDialog = (question: string) => {
  cy.findByText(question)
  return {
    answer: (answer: string) => {
      cy.findByLabelText("Answer To Ai").type(answer)
      cy.findByText("Send").click()
    },
    oldAnswer: (answer: string) => {
      cy.findByText(answer)
    },
    close: () => {
      cy.get("button.close-button").click()
    },
  }
}
