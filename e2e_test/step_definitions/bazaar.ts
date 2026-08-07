/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
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
  'I subscribe to notebook {string} in the bazaar, with target of learning {int} notes per day',
  (notebookName: string, count: string) => {
    start.navigateToBazaar().subscribe(notebookName, count)
  }
)

Then(
  'I should not be able to add notebook {string} to my learning from the Bazaar',
  (noteTopology: string) => {
    start.navigateToBazaar().expectCannotAddToMyLearning(noteTopology)
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
