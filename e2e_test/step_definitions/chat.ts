/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { When, Then } from "@badeball/cypress-cucumber-preprocessor"

When("I send the message {string} to AI", (question: string) => {
  cy.get("#chat-input").should("be.visible").clear()
  cy.get("#chat-input").type(question)
  cy.get("#chat-button").click()
})

Then("I should receive the response {string}", (answer: string) => {
  cy.findByText(answer)
})
