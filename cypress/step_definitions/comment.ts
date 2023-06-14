/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { When, Then } from "@badeball/cypress-cucumber-preprocessor"
import "../support/string.extensions"

When("I add a comment with {string}", (commentText: string) => {
  cy.get("#add-comment-textbox").should("exist")
  cy.get("#add-comment-button").should("exist")
  cy.addCommentToCurrentNote(commentText)
})

Then("I should see a comment added with {string}", (commentText: string) => {
  cy.get("#comments").should("exist")
})
