/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import {
  DataTable,
  Given,
  Then,
} from "@badeball/cypress-cucumber-preprocessor"
import "../support/string_util"
import start, { mock_services } from "../start"

Given("the OpenAI service is unavailable due to invalid system token", () => {
  mock_services.openAi().alwaysResponseAsUnauthorized()
})

Then(
  "I should be prompted with an error message saying {string}",
  (errorMessage: string) => {
    cy.expectFieldErrorMessage("Prompt", errorMessage)
  }
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
      "Expected exactly one row in the data table, but got " + hashes.length
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
    mock_services
      .openAi()
      .stubCreateThread("thread-abc123")
      .stubCreateRuns("thread-abc123", ["run-run-id"])
      .stubCreateMessage({
        role: "user",
        content: "Please complete",
      })
      .aRun("run-run-id")
      .stubRetrieveRunsThatRequireAction(data.hashes())
      .stubSubmitToolOutputs()
  }
)

Given(
  "OpenAI assistant will reply below for user messages:",
  (data: DataTable) => {
    mock_services.openAi().stubAIChat(data.hashes())
})

Given(
  "OpenAI assistant {string} will reply below for user messages:",
  (assistantId: string, data: DataTable) => {
    mock_services.openAi().stubAIChat(data.hashes(), assistantId)
  }
)

Given(
  "I create an assistant for my notebook {string}",
  (notebook: string) => {
    start.routerToNotebooksPage().notebookAssistant(notebook).create()
  })

Given(
  "OpenAI accepts the vector file upload requests",
  () => {
    mock_services.openAi().stubOpenAiUploadResponse(true)
    mock_services.openAi().stubOpenAiVectorFileUpload()
  },
)

