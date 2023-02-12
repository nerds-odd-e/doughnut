/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from "@badeball/cypress-cucumber-preprocessor"

Given("I choose to share my notebook {string}", (noteTitle: string) => {
  cy.routerToNotebooks()
  cy.findNoteCardButton(noteTitle, "Share notebook to bazaar").click()
  cy.findByRole("button", { name: "OK" }).click()
})

Then("I should see {string} is shared in the Bazaar", (noteTitle: string) => {
  cy.visit("/bazaar")
  cy.findCardTitle(noteTitle)
})

Then("notebook {string} is shared to the Bazaar", (noteTitle: string) => {
  cy.testability().shareToBazaar(noteTitle)
})

Then("there shouldn't be any note edit button for {string}", (noteTitle: string) => {
  cy.findNoteCardEditButton(noteTitle).should("not.exist")
})

When("I open the notebook {string} in the Bazaar", (noteTitle: string) => {
  cy.findCardTitle(noteTitle).click()
})

When("I go to the bazaar", () => {
  cy.visit("/bazaar")
})

When(
  "I subscribe to notebook {string} in the bazaar, with target of learning {int} notes per day",
  (notebookTitle: string, count: string) => {
    cy.visit("/bazaar")
    cy.subscribeToNotebook(notebookTitle, count)
  },
)

Then(
  "I should not see the {string} button on notebook {string}",
  (btnTitle: string, noteTitle: string) => {
    cy.findNoteCardButton(noteTitle, btnTitle).should("not.exist")
  },
)

Then("I should see readonly notebook {string} in my notes", (noteTitle: string) => {
  cy.routerToNotebooks()
  cy.findNoteCardButton(noteTitle, "edit note").should("not.exist")
})

Then("I should see I've subscribed to {string}", (noteTitle: string) => {
  cy.findCardTitle(noteTitle)
})

Then("I should see I've not subscribed to {string}", (noteTitle: string) => {
  cy.findByText("Subscribed Notes").should("exist")
  cy.findCardTitle(noteTitle).should("not.exist")
})

Then("I should be able to edit the subscription to notebook {string}", (noteTitle: string) => {
  cy.routerToNotebooks()
  cy.findNoteCardButton(noteTitle, "Edit subscription").click()
  cy.findByRole("button", { name: "Update" }).click()
})

When("I change notebook {string} to skip review", (noteTitle: string) => {
  cy.routerToNotebooks()
  cy.findNoteCardButton(noteTitle, "Edit notebook settings").click()
  cy.formField("Skip Review Entirely").check()
  cy.findByRole("button", { name: "Update" }).click()
})

Then("I should see it has link to {string}", (noteTitle: string) => {
  cy.findByText(noteTitle, { selector: ".link-title" }).click()
  cy.findNoteTitle(noteTitle)
})

Then("I unsubscribe from notebook {string}", (noteTitle: string) => {
  cy.unsubscribeFromNotebook(noteTitle)
  cy.findByRole("button", { name: "OK" }).click()
})
