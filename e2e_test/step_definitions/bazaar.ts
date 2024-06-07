/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from "@badeball/cypress-cucumber-preprocessor"
import start from "../start"

Given("I choose to share my notebook {string}", (noteTopic: string) => {
  start.routerToNotebooksPage()
  cy.findNoteCardButton(noteTopic, "Share notebook to bazaar").click()
  cy.findByRole("button", { name: "OK" }).click()
})

Then("I should see {string} shared in the Bazaar", (notebooks: string) => {
  start.bazaar().sharedNotebooks(notebooks)
})

Then("notebook {string} is shared to the Bazaar", (noteTopic: string) => {
  start.testability().shareToBazaar(noteTopic)
})

Then("there shouldn't be any note edit button", () => {
  start.assumeNotePage().editNoteImage().shouldNotExist()
})

When("I open the notebook {string} in the Bazaar", (noteTopic: string) => {
  cy.findCardTitle(noteTopic).click()
})
When("I generate assessment questions on notebook {string}", (notebookTitle: string) => {
  cy.findNoteCardButton(notebookTitle, "Generate assessment questions").click()
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
  (btnTitle: string, noteTopic: string) => {
    cy.findNoteCardButton(noteTopic, btnTitle).should("not.exist")
  },
)

Then("I should see {string} button", (btnTitle: string) => {
  cy.findByRole("button", { name: btnTitle }).should("exist")
})

Then(
  "I should see the {string} button on notebook {string}",
  (btnTitle: string, noteTopic: string) => {
    cy.findNoteCardButton(noteTopic, btnTitle).should("exist")
  },
)

Then("I should see readonly notebook {string} in my notes", (noteTopic: string) => {
  start.routerToNotebooksPage()
  cy.findByText(noteTopic).click()
  cy.pageIsNotLoading()
  start.assumeNotePage().editNoteImage().shouldNotExist()
})

Then("I should see I've subscribed to {string}", (noteTopic: string) => {
  cy.findCardTitle(noteTopic)
})

Then("I should see message that says {string}", (message: string) => {
  cy.findByText(message).should("exist")
})

Then("I should see I've not subscribed to {string}", (noteTopic: string) => {
  cy.findByText("Subscribed Notes").should("exist")
  cy.findCardTitle(noteTopic).should("not.exist")
})

Then("I should be able to edit the subscription to notebook {string}", (noteTopic: string) => {
  start.routerToNotebooksPage()
  cy.findNoteCardButton(noteTopic, "Edit subscription").click()
  cy.findByRole("button", { name: "Update" }).click()
})

When("I change notebook {string} to skip review", (noteTopic: string) => {
  start.routerToNotebooksPage()
  cy.findNoteCardButton(noteTopic, "Edit notebook settings").click()
  cy.formField("Skip Review Entirely").check()
  cy.findByRole("button", { name: "Update" }).click()
})

Then("I should see it has link to {string}", (noteTopic: string) => {
  start.assumeNotePage().navigateToReference(noteTopic)
})

Then("I unsubscribe from notebook {string}", (noteTopic: string) => {
  cy.unsubscribeFromNotebook(noteTopic)
  cy.findByRole("button", { name: "OK" }).click()
})

Given("There is a notebook with topic {string}", (notebookTopic: string) => {
  start.routerToNotebooksPage().creatingNotebook(notebookTopic)
})

Given("There are {int} notes belonging to {string}", (noteCount: number, notebookTopic: string) => {
  for (let i = 0; i < noteCount; i++) {
    const noteTopic = "Note " + i
    start.jumpToNotePage(notebookTopic).addingChildNote().createNote(noteTopic)
  }
})

Then("I should see {int} questions", (questionCount: number) => {
  for (let i = 1; i <= questionCount; i++) {
    const questionTitle = "Question " + i
    cy.findByText(questionTitle).should("exist")
  }

  const outOfBoundIndex = questionCount + 1
  cy.findByText("Question " + outOfBoundIndex).should("not.exist")
})