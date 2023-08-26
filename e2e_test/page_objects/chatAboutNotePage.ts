import { findQuestionWithStem } from "./QuizQuestionPage"

export function chatAboutNotePage() {
  return {
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
