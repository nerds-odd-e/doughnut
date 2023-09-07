import { findQuestionWithStem } from "./QuizQuestionPage"

export function chatAboutNotePage() {
  return {
    setCustomModel(customModel: string) {
      cy.get(".custom-model-input input").type(customModel)
    },
    testMe() {
      cy.findByRole("button", { name: "Test me" }).click()
    },
    sendMessage(msg: string) {
      cy.get("#chat-input").clear().type(msg)
      cy.get("#chat-button").click()
    },
    expectResponse(msg: string) {
      cy.findByText(msg)
    },
    findQuestionWithStem,
  }
}
