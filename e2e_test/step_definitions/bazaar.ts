/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When('I visit the Bazaar', () => {
  start.navigateToBazaar()
})

Then('I should see {string} shared in the Bazaar', (notebooks: string) => {
  start.navigateToBazaar().expectNotebooks(notebooks)
})

Then('notebook {string} is shared to the Bazaar', (notebookName: string) => {
  return start.testability().shareToBazaar(notebookName)
})

Then('I should not be able to edit the notes', () => {
  start.assumeNotePage().expectCannotEditNotes()
})

When('I open the notebook {string} in the Bazaar', (noteTopology: string) => {
  start.assumeBazaarPage().navigateToNotebook(noteTopology)
})

When(
  'I subscribe to notebook {string} in the bazaar, with daily assimilation target of {int} notes per day',
  (notebookName: string, count: string) => {
    start.navigateToBazaar().subscribe(notebookName, count)
  }
)

Given(
  'I have subscribed to notebook {string} in the bazaar with daily assimilation target of {int}',
  (notebookName: string, dailyTargetOfNewNotes: number) => {
    start
      .testability()
      .subscribeToBazaarNotebook(notebookName, dailyTargetOfNewNotes)
  }
)

Then(
  'I should not be able to subscribe to notebook {string} from the Bazaar',
  (noteTopology: string) => {
    start.navigateToBazaar().expectCannotSubscribe(noteTopology)
  }
)

Then("I should see I've subscribed to {string}", (noteTopology: string) => {
  start
    .navigateToNotebooksPage()
    .subscribedNotebooks()
    .expectNotebook(noteTopology)
})

Then('I should see message that says {string}', (message: string) => {
  cy.findByText(message)
})

Then("I should see I've not subscribed to {string}", (noteTopology: string) => {
  start
    .navigateToNotebooksPage()
    .subscribedNotebooks()
    .expectNotebookNotPresent(noteTopology)
})

Then('I should see it has relationship to {string}', (noteTopology: string) => {
  start.assumeNotePage().navigateToReference(noteTopology)
})
