/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, DataTable } from "@badeball/cypress-cucumber-preprocessor"
import "../support/string.extensions"

Given("open AI service always think the system token is invalid", () => {
  cy.openAiService().alwaysResponseAsUnauthorized()
})

Then("I should be prompted with an error message saying {string}", (errorMessage: string) => {
  cy.expectFieldErrorMessage("Engaging Story", errorMessage)
})

Given("OpenAI always returns text completion {string}", (description: string) => {
  cy.openAiService().restartImposterAndStubChatCompletion(description, "stop")
})

Given(
  "OpenAI returns text completion {string} for prompt containing {string}",
  (returnMessage: string, requestMessage: string) => {
    cy.openAiService().restartImposterAndMockChatCompletion(`---\n${requestMessage}`, returnMessage)
  },
)

Given(
  "OpenAI returns text completion {string} for prompt containing {string} and context containing {string}",
  (returnMessage: string, requestMessage: string) => {
    cy.openAiService().restartImposterAndMockChatCompletion(`---\n${requestMessage}`, returnMessage)
  },
)

Given("OpenAI always return image of a moon", () => {
  cy.openAiService().stubCreateImage()
})

Given("OpenAI returns an incomplete text completion {string}", (description: string) => {
  cy.openAiService().restartImposterAndStubChatCompletion(description, "length")
})

Given("An OpenAI response is unavailable", () => {
  cy.openAiService().stubOpenAiCompletionWithErrorResponse()
})

Given("AI會返回{string}", (returnMessage: string) => {
  cy.openAiService().restartImposterAndStubChatCompletion(returnMessage, "stop")
})

Given("OpenAI returns a question", (questionTable: DataTable) => {
  const record = questionTable.hashes()[0]
  const reply = JSON.stringify({
    question: record.question,
    options: [
      {
        option: record.option_a,
        correct: true,
        explanation: "",
      },
      {
        option: record.option_b,
        correct: false,
        explanation: "",
      },
      {
        option: record.option_c,
        correct: false,
        explanation: "",
      },
    ],
  })
  cy.openAiService().restartImposterAndMockChatCompletion(reply, "stop")
})
