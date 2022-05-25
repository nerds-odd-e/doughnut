/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from "@badeball/cypress-cucumber-preprocessor"

When("I comment with {string} on note {string}", (comment, noteTitle) => {
  cy.jumpToNotePage(noteTitle)
  cy.clickNotePageMoreOptionsButton(noteTitle, "Add comment")
  cy.replaceFocusedText(comment)
  cy.findByText("Submit").click()
})

Then("I should see note {string} has a comment {string} from {string}", (noteTitle, comment) => {
  cy.findByText(comment)
})
