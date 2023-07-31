/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, DataTable } from "@badeball/cypress-cucumber-preprocessor"
import "../support/string.extensions"
import { mock_services } from "page_objects"

Given("open AI service always think the system token is invalid", () => {
  mock_services.openAi().alwaysResponseAsUnauthorized()
})

Then("I should be prompted with an error message saying {string}", (errorMessage: string) => {
  cy.expectFieldErrorMessage("Prompt", errorMessage)
})

Given("OpenAI by default returns text completion {string}", (description: string) => {
  mock_services.openAi().stubChatCompletion(description, "stop")
})

Given(
  "OpenAI completes with {string} for context containing {string}",
  (returnMessage: string, context: string) => {
    mock_services.openAi().mockChatCompletionWithContext(returnMessage, context)
  },
)

Given(
  "OpenAI completes with {string} for assistant message {string}",
  (returnMessage: string, incompleteAssistantMessage: string) => {
    mock_services
      .openAi()
      .mockChatCompletionWithIncompleteAssistantMessage(
        incompleteAssistantMessage,
        returnMessage,
        "stop",
      )
  },
)

Given("OpenAI always return image of a moon", () => {
  mock_services.openAi().stubCreateImage()
})

Given(
  "OpenAI returns an incomplete text completion {string} for assistant message {string}",
  (description: string, assistantMessage: string) => {
    mock_services
      .openAi()
      .mockChatCompletionWithIncompleteAssistantMessage(assistantMessage, description, "length")
  },
)

Given("An OpenAI response is unavailable", () => {
  mock_services.openAi().stubOpenAiCompletionWithErrorResponse()
})

Given("OpenAI by default returns this question from now:", (questionTable: DataTable) => {
  const record = questionTable.hashes()[0]
  const reply = JSON.stringify({
    stem: record.question,
    correctChoiceIndex: 0,
    choices: [record.correct_choice, record.incorrect_choice_1, record.incorrect_choice_2],
  })
  cy.then(() => {
    mock_services
      .openAi()
      .restartImposter()
      .then(() =>
        Cypress.Promise.all([
          mock_services
            .openAi()
            .stubAnyChatCompletionFunctionCall("ask_single_answer_multiple_choice_question", reply),
        ]),
      )
  })
})

Then("I complain the question doesn't make sense", () => {
  cy.findByRole("button", { name: "Doesn't make sense?" }).click()
})
