import { findQuestionWithStem } from "./QuizQuestionPage"

export function chatAboutNotePage() {
  return {
    setCustomModel(customModel: string) {
      cy.get(".custom-model-input input").type(customModel)
    },
    setTemperature(temperature: number) {
      cy.get("input[type=range]").invoke("val", temperature).trigger("change")
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
