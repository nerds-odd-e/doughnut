/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from "@badeball/cypress-cucumber-preprocessor"
import start from "../start"

When("I create a new circle {string} and copy the invitation code", (circleName: string) => {
  cy.openSidebar()
  cy.findByRole("button", { name: "Create a new circle" }).click()
  cy.formField("Name").type(circleName)
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

When("I should see the circle {string} and it has two members in it", (circleName: string) => {
  cy.navigateToCircle(circleName)
  cy.findByText(circleName)
  cy.get("body").find(".circle-member").should("have.length", 2)
})

Given("There is a circle {string} with {string} members", (circleName: string, members: string) => {
  start.testability().seedCircle({ circleName: circleName, members: members })
})

When("I create a notebook {string} in circle {string}", (noteTopic, circleName) => {
  cy.navigateToCircle(circleName)
  cy.findByText("Add New Notebook In This Circle").click()
  cy.submitNoteCreationFormSuccessfully({ Topic: noteTopic })
})

When("I should see the notebook {string} in circle {string}", (noteTopic: string, circleName) => {
  cy.navigateToCircle(circleName)
  cy.findCardTitle(noteTopic)
})

When("I add a note {string} under {string}", (noteTopic: string, parentNoteTopic: string) => {
  cy.findByText(parentNoteTopic).click()
  cy.clickAddChildNoteButton()
  cy.submitNoteCreationFormSuccessfully({ Topic: noteTopic })
})

When(
  "I subscribe to notebook {string} in the circle {string}, with target of learning {int} notes per day",
  (notebookTitle: string, circleName: string, count: string) => {
    cy.navigateToCircle(circleName)
    cy.subscribeToNotebook(notebookTitle, count)
  },
)

When("I am on {string} circle page", (circleName) => {
  cy.navigateToCircle(circleName)
})

When("There is a notebook {string} in circle {string}", (topic, circleName) => {
  start.testability().seedNotes([{ topic }], "", circleName)
})

When("someone of my circle deletes the {string} notebook", (noteTopic: string) => {
  cy.noteByTitle(noteTopic).deleteNoteViaAPI()
})

Then(
  "I should see {string} in the circle page within {int} seconds",
  (noteTopic: string, seconds: number) => {
    cy.tick(seconds * 1000)
    cy.findCardTitle(noteTopic)
  },
)

Then(
  "I should not see {string} in the circle page within {int} seconds",
  (noteTopic: string, seconds: number) => {
    cy.tick(seconds * 1000)
    cy.findCardTitle(noteTopic).should("not.exist")
  },
)
