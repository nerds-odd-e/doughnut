/// <reference types="cypress" />
// @ts-check

import { And, Before, Given, Then, When } from "cypress-cucumber-preprocessor/steps"
const path = require("path")

Then("I should see an input box for comment", (noteTitle) => {
  cy.get("#comment-input").should('be.visible');
})

Given("there is a note and some comments of current user", (comment) => {
  cy.seedNotes([
    {
      'title': 'A'
    }
  ])
  cy.seedComments(comment.hashes())
})

