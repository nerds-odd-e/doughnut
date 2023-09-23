/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, DataTable } from "@badeball/cypress-cucumber-preprocessor"
import "../support/string.extensions"
import { mock_services } from "page_objects"
import { MessageToMatch } from "page_objects/mock_services/MessageToMatch"

Given("open AI service always think the system token is invalid", () => {
  mock_services.openAi().alwaysResponseAsUnauthorized()
})

Then("I should be prompted with an error message saying {string}", (errorMessage: string) => {
  cy.expectFieldErrorMessage("Prompt", errorMessage)
})

Given("OpenAI by default returns text completion {string}", (details: string) => {
  cy.then(async () => {
    await mock_services.openAi().restartImposter()
    mock_services.openAi().stubChatCompletion(details, "stop")
  })
})

Given(
  "OpenAI completes with {string} for context containing {string}",
  (returnMessage: string, context: string) => {
    mock_services.openAi().mockChatCompletionWithContext(returnMessage, context)
  },
)

Given(
  "OpenAI completes with {string} for messages containing:",
  (returnMessage: string, data: DataTable) => {
    const messages: MessageToMatch[] = data.hashes().map((row) => {
      return {
        role: row["role"],
        content: row["content"],
      } as MessageToMatch
    })
    mock_services.openAi().mockChatCompletionWithMessages(returnMessage, messages)
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
  (details: string, assistantMessage: string) => {
    mock_services
      .openAi()
      .mockChatCompletionWithIncompleteAssistantMessage(assistantMessage, details, "length")
  },
)

Given("An OpenAI response is unavailable", () => {
  mock_services.openAi().stubOpenAiCompletionWithErrorResponse()
})

Given("OpenAI by default returns this question:", (questionTable: DataTable) => {
  const record = questionTable.hashes()[0]
  const reply = JSON.stringify({
    stem: record["Question Stem"],
    correctChoiceIndex: 0,
    choices: [record["Correct Choice"], record["Incorrect Choice 1"], record["Incorrect Choice 2"]],
    confidence: 10,
  })
  cy.then(async () => {
    await mock_services.openAi().restartImposter()
    await mock_services
      .openAi()
      .stubAnyChatCompletionFunctionCall("ask_single_answer_multiple_choice_question", reply)
  })
})

Then("I complain the question doesn't make sense", () => {
  cy.findByRole("button", { name: "Doesn't make sense?" }).click()
})
