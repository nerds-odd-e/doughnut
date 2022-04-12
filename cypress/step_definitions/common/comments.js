/// <reference types="cypress" />
// @ts-check

import { And, Before, Given, Then, When } from "cypress-cucumber-preprocessor/steps"
const path = require("path")

Then("I should see an input box for comment", (noteTitle) => {
  cy.get("#comment-input").should('be.visible');
})
