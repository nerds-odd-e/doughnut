// ***********************************************
// custom commands and overwrite existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add("login", (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add("drag", { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add("dismiss", { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite("visit", (originalFn, url, options) => { ... })

/// <reference types="cypress" />
// @ts-check
import { DefaultPredicate, FlexiPredicate, Operator } from "@anev/ts-mountebank"
import "@testing-library/cypress/add-commands"
import "cypress-file-upload"
import "./string.extensions"
import ServiceMocker from "./ServiceMocker"
import { HttpMethod, Predicate } from "@anev/ts-mountebank"

function restartImposterAndMockTextCompletion(
  predicate: Predicate,
  serviceMocker: ServiceMocker,
  reply: string,
  finishReason: "length" | "stop",
) {
  serviceMocker.install()

  serviceMocker.mockWithPredicate(predicate, {
    id: "cmpl-uqkvlQyYK7bGYrRHQ0eXlWi7",
    object: "text_completion",
    created: 1589478378,
    model: "text-davinci-003",
    choices: [
      {
        text: reply,
        index: 0,
        logprobs: null,
        finish_reason: finishReason,
      },
    ],
    usage: {
      prompt_tokens: 5,
      completion_tokens: 7,
      total_tokens: 12,
    },
  })
}

function restartImposterAndMockChatCompletion(
  predicate: Predicate,
  serviceMocker: ServiceMocker,
  reply: string,
  finishReason: "length" | "stop",
) {
  serviceMocker.install()

  serviceMocker.mockWithPredicate(predicate, {
    id: "cmpl-uqkvlQyYK7bGYrRHQ0eXlWi7",
    object: "chat.completion",
    created: 1589478378,
    model: "gpt-3.5-turbo",
    choices: [
      {
        message: {
          role: "assistant",
          content: reply,
        },
        index: 0,
        finish_reason: finishReason,
      },
    ],
    usage: {
      prompt_tokens: 5,
      completion_tokens: 7,
      total_tokens: 12,
    },
  })
}

Cypress.Commands.add(
  "restartImposterAndMockTextCompletion",
  { prevSubject: true },
  (serviceMocker: ServiceMocker, prompt: string, reply: string) => {
    const predicate = new FlexiPredicate()
      .withOperator(Operator.matches)
      .withPath(`/v1/completions`)
      .withMethod(HttpMethod.POST)
      .withBody({ prompt })
    restartImposterAndMockTextCompletion(predicate, serviceMocker, reply, "stop")
  },
)

Cypress.Commands.add(
  "restartImposterAndStubTextCompletion",
  { prevSubject: true },
  (serviceMocker: ServiceMocker, reply: string, finishReason: "length" | "stop") => {
    const predicate = new DefaultPredicate(`/v1/completions`, HttpMethod.POST)
    restartImposterAndMockTextCompletion(predicate, serviceMocker, reply, finishReason)
  },
)

Cypress.Commands.add(
  "restartImposterAndMockChatCompletion",
  { prevSubject: true },
  (serviceMocker: ServiceMocker, prompt: string, reply: string) => {
    const body = { messages: [{ role: "user", content: prompt }] }
    const predicate = new FlexiPredicate()
      .withOperator(Operator.matches)
      .withPath(`/v1/chat/completions`)
      .withMethod(HttpMethod.POST)
      .withBody(body)
    restartImposterAndMockChatCompletion(predicate, serviceMocker, reply, "stop")
  },
)

Cypress.Commands.add(
  "restartImposterAndMockChatCompletionWithContext",
  { prevSubject: true },
  (serviceMocker: ServiceMocker, prompt: string, reply: string, context: string) => {
    const body = { messages: [{ role: "user", content: prompt }] }
    const predicate = new FlexiPredicate()
      .withOperator(Operator.matches)
      .withPath(`/v1/chat/completions`)
      .withMethod(HttpMethod.POST)
      .withBody(body)
    restartImposterAndMockChatCompletion(predicate, serviceMocker, reply, "stop")
  },
)

Cypress.Commands.add(
  "restartImposterAndStubChatCompletion",
  { prevSubject: true },
  (serviceMocker: ServiceMocker, reply: string, finishReason: "length" | "stop") => {
    const predicate = new DefaultPredicate(`/v1/chat/completions`, HttpMethod.POST)
    restartImposterAndMockChatCompletion(predicate, serviceMocker, reply, finishReason)
  },
)

Cypress.Commands.add("stubCreateImage", { prevSubject: true }, (serviceMocker: ServiceMocker) => {
  serviceMocker.stubPoster(`/v1/images/generations`, {
    id: "cmpl-uqkvlQyYK7bGYrRHQ0eXlWi7",
    created: 1589478378,
    data: [
      {
        url: "https://moon",
        b64_json:
          "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
      },
    ],
  })
})

Cypress.Commands.add(
  "stubOpenAiCompletionWithErrorResponse",
  { prevSubject: true },
  (serviceMocker: ServiceMocker) => {
    serviceMocker.stubGetterWithError500Response(`/*`, {})
  },
)

Cypress.Commands.add(
  "alwaysResponseAsUnauthorized",
  { prevSubject: true },
  (serviceMocker: ServiceMocker) => {
    serviceMocker.install()
    serviceMocker.stubPosterUnauthorized(`/*`, {
      status: "BAD_REQUEST",
      message: "nah nah nah, you need a valid token",
      errors: {
        "OpenAi Error": "BAD_REQUEST",
      },
    })
  },
)
