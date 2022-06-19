/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { And, Given, Then, When } from "@badeball/cypress-cucumber-preprocessor"

When("I create a new circle {string} and copy the invitation code", (circleName) => {
  cy.visit("/circles")
  cy.findByRole("button", { name: "Create a new circle" }).click()
  cy.getFormControl("Name").type(circleName)
  cy.get('input[value="Submit"]').click()

  cy.get("#invitation-code")
    .invoke("val")
    .then((text) => {
      cy.wrap(text).as("savedInvitationCode")
    })
})

When("I visit the invitation link", () => {
  cy.get("@savedInvitationCode").then((invitationCode) => cy.visit(invitationCode))
})

When("I join the circle", () => {
  cy.get('input[value="Join"]').click()
})

When("I should see the circle {string} and it has two members in it", (circleName) => {
  cy.navigateToCircle(circleName)
  cy.get("body").find(".circle-member").should("have.length", 2)
})

Given("There is a circle {string} with {string} members", (circleName, members) => {
  cy.testability().seedCircle({ circleName: circleName, members: members })
})

When("I create a notebook {string} in circle {string}", (noteTitle, circleName) => {
  cy.navigateToCircle(circleName)
  cy.findByText("Add New Notebook In This Circle").click()
  cy.submitNoteCreationFormsWith([{ Title: noteTitle }])
})

When("I should see the notebook {string} in circle {string}", (noteTitle, circleName) => {
  cy.navigateToCircle(circleName)
  cy.findByText(noteTitle).should("be.visible")
})

When("I add a note {string} under {string}", (noteTitle, parentNoteTitle) => {
  cy.findByText(parentNoteTitle).click()
  cy.clickAddChildNoteButton()
  cy.submitNoteCreationFormsWith([{ Title: noteTitle }])
  cy.findByText(noteTitle).should("be.visible")
})

When(
  "I subscribe to notebook {string} in the circle {string}, with target of learning {int} notes per day",
  (notebookTitle, circleName, count) => {
    cy.navigateToCircle(circleName)
    cy.subscribeToNotebook(notebookTitle, count)
  },
)

When("I am on {string} circle page", (circleName) => {
  cy.navigateToCircle(circleName)
})

When("There is a notebook {string} in circle {string}", (title, circleName) => {
  cy.testability().seedNotes([{ title }], "", circleName)
})

And("someone of my circle deletes the {string} notebook", (noteTitle) => {
  cy.noteByTitle(noteTitle).deleteNoteViaAPI()
})

Then("I should see {string} in the circle page within {int} seconds", (noteTitle, seconds) => {
  cy.tick(seconds * 1000)
  cy.findByText(noteTitle)
})

Then("I should not see {string} in the circle page within {int} seconds", (noteTitle, seconds) => {
  cy.tick(seconds * 1000)
  cy.findByText(noteTitle).should("not.exist")
})
