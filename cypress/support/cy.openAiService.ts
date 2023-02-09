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
import "@testing-library/cypress/add-commands"
import "cypress-file-upload"
import "./string.extensions"
import ServiceMocker from "./ServiceMocker"

Cypress.Commands.add(
  "stubOpenAiCompletion",
  { prevSubject: true },
  (serviceMocker: ServiceMocker, title: string, reply: string) => {
    serviceMocker.stubPoster(`/v1/completions`, {
      id: "cmpl-uqkvlQyYK7bGYrRHQ0eXlWi7",
      object: "text_completion",
      created: 1589478378,
      model: "text-davinci-003",
      choices: [
        {
          text: reply,
          index: 0,
          logprobs: null,
          finish_reason: "length",
        },
      ],
      usage: {
        prompt_tokens: 5,
        completion_tokens: 7,
        total_tokens: 12,
      },
    })
  },
)

Cypress.Commands.add(
  "stubOpenAiCompletionWithErrorResponse",
  { prevSubject: true },
  (serviceMocker: ServiceMocker) => {
    serviceMocker.stubGetterWithError500Response(`/v1/completions`, {})
  },
)

Cypress.Commands.add(
  "responseAsIfTheTokenIs",
  { prevSubject: true },
  (serviceMocker: ServiceMocker, tokenValidity) => {
    if (tokenValidity === "invalid") {
      serviceMocker.stubPosterUnauthorized(`/v1/completions`)
    }
  },
)
