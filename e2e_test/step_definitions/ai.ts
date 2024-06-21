/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { DataTable, Given, Then } from "@badeball/cypress-cucumber-preprocessor"
import "../support/string_util"
import start, { mock_services } from "../start"
import { MessageToMatch } from "../start/mock_services/MessageToMatch"

Given("the OpenAI service is unavailable due to invalid system token", () => {
  mock_services.openAi().alwaysResponseAsUnauthorized()
})

Then(
  "I should be prompted with an error message saying {string}",
  (errorMessage: string) => {
    cy.expectFieldErrorMessage("Prompt", errorMessage)
  },
)

Given("OpenAI has models {string} available", (modelNames: string) => {
  cy.then(async () => {
    mock_services.openAi().stubGetModels(modelNames)
  })
})

Given("OpenAI always return image of a moon", () => {
  mock_services.openAi().stubCreateImage()
})

Given("An OpenAI response is unavailable", () => {
  mock_services.openAi().stubOpenAiCompletionWithErrorResponse()
})

Given("OpenAI now generates this question:", (questionTable: DataTable) => {
  const hashes = questionTable.hashes()
  if (hashes.length !== 1 || !hashes[0]) {
    throw new Error(
      "Expected exactly one row in the data table, but got " + hashes.length,
    )
  }
  start.questionGenerationService().resetAndStubAskingMCQ(hashes[0])
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

Given(
  "the OpenAI assistant will create a thread and request for the following actions:",
  (data: DataTable) => {
    const threadId = "thread-abc123"
    mock_services.openAi().aThread("thread-abc123").stubMultipleCreateRuns(["run-run-id"])
    mock_services
      .openAi()
      .stubCreateThread(threadId)
      .then((thread) =>
        thread
          .stubCreateMessage({
            role: "user",
            content: "Please complete",
          },
          "run-run-id",
        )
          .then((run) =>
            run
              .stubRetrieveRunsThatRequireAction(data.hashes())
              .then((run) => run.stubSubmitToolOutputs()),
          ),
      )
  },
)

Given(
  "OpenAI assistant will reply below for user messages:",
  (data: DataTable) => {
    const threadId = "thread-abc123"
    mock_services
      .openAi()
      .stubCreateThread(threadId)
      mock_services
        .openAi()
        .aThread(threadId)
        .stubMultipleCreateRuns(data.hashes().map((row) => row["run id"]!))
    data.hashes().forEach((row) => {
      const userMessage: MessageToMatch = {
        role: "user",
        content: row["user message"],
      } as MessageToMatch
      mock_services
        .openAi()
        .aThread(threadId)
        .stubCreateMessage(userMessage, row["run id"]!)
        .then((run) => {
          run.stubRetrieveRunsThatCompleted()
          run.stubListMessages([
                userMessage,
                { role: "assistant", content: row["assistant reply"]! },
              ])
        })
    })
})
