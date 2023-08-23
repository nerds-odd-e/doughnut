/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { When, Then } from "@badeball/cypress-cucumber-preprocessor"

When("I ask to OpenAI {string}", (askStatement: string) => {
  cy.get("#ask-input").should("be.visible").type(askStatement)
  cy.get("#ask-button").click()
})

Then("I can confirm the answer {}", (answer: string) => {
  cy.get("#hoge").should("be.visible").contains(answer)
})
