/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then } from "@badeball/cypress-cucumber-preprocessor"
import "../support/string.extensions"

Given("open AI service always think the system token is invalid", () => {
  cy.openAiService().alwaysResponseAsUnauthorized()
})

Then("I should be prompted with an error message saying {string}", (errorMessage: string) => {
  cy.expectFieldErrorMessage("Engaging Story", errorMessage)
})

Given("OpenAI always returns text completion {string}", (description: string) => {
  cy.openAiService().restartImposterAndStubTextCompletion(description, "stop")
})

Given(
  "OpenAI returns text completion {string} for prompt {string}",
  (returnMessage: string, requestMessage: string) => {
    cy.openAiService().restartImposterAndMockTextCompletion(`---\n${requestMessage}`, returnMessage)
  },
)

Given("OpenAI always return image of a moon", () => {
  cy.openAiService().stubCreateImage()
})

Given("OpenAI returns an incomplete text completion {string}", (description: string) => {
  cy.openAiService().restartImposterAndStubTextCompletion(description, "length")
})

Given("An OpenAI response is unavailable", () => {
  cy.openAiService().stubOpenAiCompletionWithErrorResponse()
})

Given("AI會返回{string}", (returnMessage: string) => {
  cy.openAiService().restartImposterAndStubTextCompletion(returnMessage, "stop")
})

Given("OpenAI returns a question", (question: DataTable) => {
  const questionAsJsonReply = question.hashes().map((questionHash) => {
    return JSON.stringify({
      question: questionHash.question,
      options: [
        {
          option: questionHash.option_a,
          correct: true,
          explanation: "",
        },
        {
          option: questionHash.option_b,
          correct: false,
          explanation: "",
        },
        {
          option: questionHash.option_c,
          correct: false,
          explanation: "",
        },
      ],
    })
  })
  cy.openAiService().restartImposterAndMockChatCompletion(questionAsJsonReply, "stop")
})
