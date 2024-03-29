/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, DataTable } from "@badeball/cypress-cucumber-preprocessor"
import "../support/string.extensions"
import start, { mock_services } from "../start"
import { MessageToMatch } from "../start/mock_services/MessageToMatch"

Given("the OpenAI service is unavailable due to invalid system token", () => {
  mock_services.openAi().alwaysResponseAsUnauthorized()
})

Then("I should be prompted with an error message saying {string}", (errorMessage: string) => {
  cy.expectFieldErrorMessage("Prompt", errorMessage)
})

Given("OpenAI by default reply text completion assistant message {string}", (details: string) => {
  cy.then(async () => {
    await mock_services.openAi().restartImposter()
    mock_services
      .openAi()
      .chatCompletion()
      .requestMessagesMatch([])
      .stubNonfunctionCallResponse(details, "stop")
  })
})

Given("OpenAI has models {string} available", (modelNames: string) => {
  cy.then(async () => {
    mock_services.openAi().stubGetModels(modelNames)
  })
})

Given(
  "OpenAI assistant will reply {string} for messages containing:",
  (returnMessage: string, data: DataTable) => {
    const messages: MessageToMatch[] = data.hashes().map((row) => {
      return {
        role: row["role"],
        content: row["content"],
      } as MessageToMatch
    })
    mock_services
      .openAi()
      .chatCompletion()
      .requestMessagesMatch(messages)
      .stubNonfunctionCallResponse(returnMessage)
  },
)

Given("OpenAI always return image of a moon", () => {
  mock_services.openAi().stubCreateImage()
})

Given("An OpenAI response is unavailable", () => {
  mock_services.openAi().stubOpenAiCompletionWithErrorResponse()
})

Given("OpenAI now generates this question:", (questionTable: DataTable) => {
  start.questionGenerationService().resetAndStubAskingMCQ(questionTable.hashes()[0])
})

Given("OpenAI evaluates the question as legitamate", () => {
  start
    .questionGenerationService()
    .stubEvaluationQuestion({ feasibleQuestion: true, correctChoices: [0] })
})

Given("OpenAI evaluates the question as not legitamate", () => {
  start.questionGenerationService().stubEvaluationQuestion({
    feasibleQuestion: false,
    correctChoices: [0],
    comment: "AI plainly doesn't like it.",
  })
})

Then("I contest the question", () => {
  cy.findByRole("button", { name: "Doesn't make sense?" }).click()
})

Given("the OpenAI assistant in thread {string} is set to:", (threadId: string, data: DataTable) => {
  mock_services.openAi().thread(threadId).stubRetrieveRuns(data.hashes())
})

Given(
  "OpenAI service can create thread and run with id {string} when requested",
  (threadId: string) => {
    mock_services.openAi().thread(threadId).stubCreateThreadRunAndSubmitOutput()
  },
)
