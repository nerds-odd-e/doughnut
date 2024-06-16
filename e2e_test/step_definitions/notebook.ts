/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from "@badeball/cypress-cucumber-preprocessor"
import start from "../start"

Given("I choose to share my notebook {string}", (noteTopic: string) => {
  start.routerToNotebooksPage().shareNotebookToBazaar(noteTopic)
})

Then("I should see readonly notebook {string} in my notes", (noteTopic: string) => {
  start.routerToNotebooksPage()
  cy.findByText(noteTopic).click()
  cy.pageIsNotLoading()
  start.assumeNotePage().editNoteImage().shouldNotExist()
})

Then("I should be able to edit the subscription to notebook {string}", (noteTopic: string) => {
  start.routerToNotebooksPage().updateSubscription(noteTopic)
})

When("I change notebook {string} to skip review", (noteTopic: string) => {
  start.routerToNotebooksPage().skipReview(noteTopic)
})

Then("I unsubscribe from notebook {string}", (noteTopic: string) => {
  start.routerToNotebooksPage().unsubscribe(noteTopic)
})

Given(
  "I set the number of questions per assessment of the notebook {string} to {int}",
  (notebook: string, numberOfQuestion: number) => {
    start.routerToNotebooksPage().updateAssessmentSettings(notebook, numberOfQuestion)
  },
)
