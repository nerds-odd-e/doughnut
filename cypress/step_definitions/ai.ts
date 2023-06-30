/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, DataTable } from "@badeball/cypress-cucumber-preprocessor"
import "../support/string.extensions"

Given("open AI service always think the system token is invalid", () => {
  cy.openAiService().alwaysResponseAsUnauthorized()
})

Then("I should be prompted with an error message saying {string}", (errorMessage: string) => {
  cy.expectFieldErrorMessage("Prompt", errorMessage)
})

Given("OpenAI by default returns text completion {string}", (description: string) => {
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

Given("OpenAI by default returns this question from now:", (questionTable: DataTable) => {
  const record = questionTable.hashes()[0]
  const reply = JSON.stringify({
    question: record.question,
    correctOption: record.correct_option,
    wrongOptions: [record.wrong_option_1, record.wrong_option_2],
  })
  cy.openAiService().restartImposter()
  cy.openAiService().stubChatCompletionFunctionCall(
    "ask_single_answer_multiple_choice_question",
    reply,
  )
})

Then("it should consider the context {string}", (path: string) => {
  cy.openAiService().thePreviousRequestShouldHaveIncludedPathInfo(path)
})

Then("I regenerate the question", () => {
  cy.findByRole("button", { name: "Ask again" }).click()
})
