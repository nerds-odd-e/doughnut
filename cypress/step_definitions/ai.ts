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

Given("OpenAI by default returns text completion {string}", (description: string) => {
  cy.openAiService().stubChatCompletion(description, "stop")
})

Given("OpenAI by default returns text completion {string} from now", (description: string) => {
  cy.openAiService().restartImposter()
  cy.openAiService().stubChatCompletion(description, "stop")
})

Given(
  "OpenAI completes with {string} for context containing {string}",
  (returnMessage: string, context: string) => {
    cy.openAiService().mockChatCompletionWithContext(returnMessage, context)
  },
)

Given(
  "OpenAI completes with {string} for incomplete assistant message {string}",
  (returnMessage: string, incompleteAssistantMessage: string) => {
    cy.openAiService().mockChatCompletionWithIncompleteAssistantMessage(
      incompleteAssistantMessage,
      returnMessage,
    )
  },
)

Given("OpenAI always return image of a moon", () => {
  cy.openAiService().stubCreateImage()
})

Given("OpenAI returns an incomplete text completion {string}", (description: string) => {
  cy.openAiService().stubChatCompletion(description, "length")
})

Given("An OpenAI response is unavailable", () => {
  cy.openAiService().stubOpenAiCompletionWithErrorResponse()
})

Given("AI會返回{string}", (returnMessage: string) => {
  cy.openAiService().stubChatCompletion(returnMessage, "stop")
})

Given("OpenAI by default returns this question from now:", (questionTable: DataTable) => {
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
  cy.openAiService().restartImposter()
  cy.openAiService().stubChatCompletion(reply, "stop")
})
