/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, DataTable } from "@badeball/cypress-cucumber-preprocessor"
import "../support/string_util"
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

Given("OpenAI always return image of a moon", () => {
  mock_services.openAi().stubCreateImage()
})

Given("An OpenAI response is unavailable", () => {
  mock_services.openAi().stubOpenAiCompletionWithErrorResponse()
})

Given("OpenAI now generates this question:", (questionTable: DataTable) => {
  const hashes = questionTable.hashes()
  if (hashes.length !== 1 || !hashes[0]) {
    throw new Error("Expected exactly one row in the data table, but got " + hashes.length)
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
    mock_services
      .openAi()
      .stubCreateThread(threadId)
      .then((thread) =>
        thread
          .stubCreateMessageAndCreateRun({ role: "user", content: "Please complete" })
          .then((run) =>
            run
              .stubRetrieveRunsThatRequireAction(data.hashes())
              .then((run) => run.stubSubmitToolOutputs()),
          ),
      )
  },
)

Given(
  "OpenAI assistant will reply {string} for user message {string}",
  (returnMessage: string, message: string) => {
    const userMessage: MessageToMatch = {
      role: "user",
      content: message,
    } as MessageToMatch
    const threadId = "thread-abc123"
    mock_services
      .openAi()
      .stubCreateThread(threadId)
      .then((thread) =>
        thread
          .stubCreateMessageAndCreateRun(userMessage)
          .then((run) =>
            run
              .stubRetrieveRunsThatCompleted()
              .then((run) =>
                run.stubListMessages([{ role: "assistant", content: returnMessage }, userMessage]),
              ),
          ),
      )
  },
)
