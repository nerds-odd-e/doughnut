/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, DataTable } from "@badeball/cypress-cucumber-preprocessor"
import "../support/string.extensions"
import { services } from "page_objects"

Given("open AI service always think the system token is invalid", () => {
  services.openAiService().alwaysResponseAsUnauthorized()
})

Then("I should be prompted with an error message saying {string}", (errorMessage: string) => {
  cy.expectFieldErrorMessage("Prompt", errorMessage)
})

Given("OpenAI by default returns text completion {string}", (description: string) => {
  services.openAiService().stubChatCompletion(description, "stop")
})

Given(
  "OpenAI completes with {string} for context containing {string}",
  (returnMessage: string, context: string) => {
    services.openAiService().mockChatCompletionWithContext(returnMessage, context)
  },
)

Given(
  "OpenAI completes with {string} for assistant message {string}",
  (returnMessage: string, incompleteAssistantMessage: string) => {
    services
      .openAiService()
      .mockChatCompletionWithIncompleteAssistantMessage(
        incompleteAssistantMessage,
        returnMessage,
        "stop",
      )
  },
)

Given("OpenAI always return image of a moon", () => {
  services.openAiService().stubCreateImage()
})

Given(
  "OpenAI returns an incomplete text completion {string} for assistant message {string}",
  (description: string, assistantMessage: string) => {
    services
      .openAiService()
      .mockChatCompletionWithIncompleteAssistantMessage(assistantMessage, description, "length")
  },
)

Given("An OpenAI response is unavailable", () => {
  services.openAiService().stubOpenAiCompletionWithErrorResponse()
})

Given("OpenAI by default returns this question from now:", (questionTable: DataTable) => {
  const record = questionTable.hashes()[0]
  const reply = JSON.stringify({
    stem: record.question,
    correctChoiceIndex: 0,
    choices: [record.correct_choice, record.incorrect_choice_1, record.incorrect_choice_2],
  })
  services
    .openAiService()
    .restartImposter()
    .then(() =>
      services
        .openAiService()
        .stubAnyChatCompletionFunctionCall("ask_single_answer_multiple_choice_question", reply),
    )
})

Then("I complain the question doesn't make sense", () => {
  cy.findByRole("button", { name: "Doesn't make sense?" }).click()
})
