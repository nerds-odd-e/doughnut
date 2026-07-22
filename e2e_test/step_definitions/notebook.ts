/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import {
  Given,
  Then,
  When,
  type DataTable,
} from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import notebookPage from '../start/pageObjects/notebookPage'
import { pageIsNotLoading } from '../start/pageBase'

Given('I choose to share my notebook {string}', (noteTopology: string) => {
  start.navigateToNotebookPage(noteTopology).shareNotebookToBazaar()
})

Then(
  'I should see readonly notebook {string} in my notes',
  (noteTopology: string) => {
    start
      .navigateToNotebooksPage()
      .subscribedNotebooks()
      .openNotebook(noteTopology)
    start
      .pageIsNotLoading()
      .assumeNotePage()
      .addingChildNoteButton()
      .shouldNotExist()
  }
)

Then(
  'I should be able to edit the subscription to notebook {string}',
  (noteTopology: string) => {
    start
      .navigateToNotebooksPage()
      .notebookCard(noteTopology)
      .updateSubscription()
  }
)

When('I change notebook {string} to skip recall', (noteTopology: string) => {
  start.navigateToNotebookPage(noteTopology).skipMemoryTracking()
})

Then('I unsubscribe from notebook {string}', (noteTopology: string) => {
  start.navigateToNotebooksPage().notebookCard(noteTopology).unsubscribe()
})

When(
  'I add questions to the following notes in the notebook {string}',
  (_notebook: string, data: DataTable) => {
    data.rows().forEach((row) => {
      start.jumpToNotePage(row[0] as string).addQuestion({
        Stem: row[1] as string,
        'Choice 0': 'yes',
        'Choice 1': 'no',
        'Choice 2': 'maybe',
        'Correct Choice Index': '0',
      })
    })
  }
)

When(
  'I open the notebook {string} from my notebooks catalog',
  (notebookName: string) => {
    start.navigateToNotebookPage(notebookName)
  }
)

When(
  'I type notebook index body {string} on the notebook page and save',
  (body: string) => {
    notebookPage().typeNotebookIndexDraftAndSave(body)
  }
)

When('I reload the notebook page', () => {
  cy.reload()
  pageIsNotLoading()
})

Then('the notebook index body includes {string}', (fragment: string) => {
  notebookPage().expectNotebookIndexBodyContains(fragment)
})

Then('I should see popup {string}', (message: string) => {
  // Wait for and verify alert message
  cy.on('window:alert', (text) => {
    expect(text).to.equal(message)
  })
})

Then('the notebook page summary shows name {string}', (name: string) => {
  cy.get('[data-testid="notebook-page-summary"]')
    .find('h1')
    .should('contain.text', name)
})

Then(
  'the notebook workspace home shows name {string} and index',
  (name: string) => {
    notebookPage().expectHomeLandmarks(name)
  }
)

Then('notebook admin settings sections are not visible', () => {
  notebookPage().expectAdminSettingsAbsent()
})

When('I open the notebook workspace Settings tab', () => {
  notebookPage().openSettingsTab()
})

Then('notebook admin settings sections are visible', () => {
  notebookPage().expectSettingsSectionsVisible()
})

When(
  'I rename the notebook from the notebook page summary to {string}',
  (newName: string) => {
    cy.get('[data-testid="notebook-page-name-edit"]').click()
    cy.get('[data-test="notebook-page-name-input"]').click()
    cy.clearFocusedText().type(newName)
    cy.get('[data-testid="notebook-page-name-update"]').click()
    cy.findByRole('button', { name: 'OK' }).click()
    pageIsNotLoading()
  }
)
