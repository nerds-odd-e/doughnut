/// <reference types="cypress" />
// @ts-check

import { Given, Then, When } from "cypress-cucumber-preprocessor/steps"

Then("I should see an input box for comment", () => {
  cy.get("#comment-input").should("be.visible")
})

Given("there is a note and some comments of current user", (comment) => {
  cy.seedNotes([
    {
      "title": "A",
    },
  ])
  cy.get("@seededNoteIdMap").then((seededNoteIdMap) =>{
      cy.seedComments(seededNoteIdMap["A"],comment.hashes())
    })
})

When("I click \"Delete\" button on comment \"Hello\"", (comment) => {
  cy.deleteComment(comment)
})


When(
  "I add a comment {string}",
  (description) => {
    cy.get("#comment-input").click({ force: true })
    cy.replaceFocusedText(description)
  },
)

Then("I should see comment posted time", () => {
  cy.get("body").should("contain", "yyyy-MM-dd")
})
