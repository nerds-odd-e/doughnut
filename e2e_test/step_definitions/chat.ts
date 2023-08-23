/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { When } from "@badeball/cypress-cucumber-preprocessor"

When("I ask to OpenAI {string}", (askStatement: string) => {
  cy.get("#undefined-undefined", { timeout: 10000 }).should("be.visible").type(askStatement)
  cy.get("#askBtn").click()
})
