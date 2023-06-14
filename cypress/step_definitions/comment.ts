/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import {
  When,
  Then
} from "@badeball/cypress-cucumber-preprocessor"
import "../support/string.extensions"

When(
  "I add a comment with {string}",
  (commentText: string) => {
    cy.get("#comment-button").should("exist")
  },
)

Then("I should see a comment added with {string}",
  (commentText: string) => {
    // ...
  },
)
