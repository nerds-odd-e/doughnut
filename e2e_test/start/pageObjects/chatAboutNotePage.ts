import { assumeQuestionPage } from "./QuizQuestionPage"

export function assumeChatAboutNotePage() {
  return {
    testMe() {
      cy.findByRole("button", { name: "Test me" }).click()
      cy.pageIsNotLoading() // wait for the response
      return assumeQuestionPage()
    },
    sendMessage(msg: string) {
      cy.get("#chat-input").clear().type(msg)
      cy.get("#chat-button").click()
    },
    expectResponse(msg: string) {
      cy.findByText(msg)
    },
  }
}
