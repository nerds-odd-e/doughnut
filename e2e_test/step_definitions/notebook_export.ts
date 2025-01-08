/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />

import { Given, When } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@cucumber/cucumber'
import start from '../start'

// First step already exists in user.ts:
// Given('I am logged in as an existing user', () => {
//   start.loginAs('old_learner')
// })

Given('I have a notebook titled {string}', (notebookTitle: string) => {
  // Following the pattern from note.ts where notes are injected
  start.testability().injectNotes([{ Title: notebookTitle }])
})

Given('the notebook contains the following notes', (notesTable: DataTable) => {
  const notes = notesTable.hashes()
  // Using the injectNotes pattern from note.ts
  start.testability().injectNotes(notes)
})

// Additional steps needed for the feature
When('I select the {string} notebook', (notebookTitle: string) => {
  start.jumpToNotePage(notebookTitle)
})

When(
  'I click on the export for Obsidian option on notebook {string}',
  (notebookTitle: string) => {
    // Wait and ensure element is fully loaded
    cy.findByText(notebookTitle, { selector: '.notebook-card *' })
      .should('be.visible')
      .parents('.daisy-card')
      .within(() => {
        // Click the export button with specific title
        cy.get('button[title="Export notebook for Obsidian"]')
          .should('be.visible')
          .click()
      })
  }
)

Given('I have an empty notebook titled {string}', (notebookTitle: string) => {
  start.testability().injectNotes([{ Title: notebookTitle }])
})

When('I go to Notebook page', () => {
  // Using the same navigation function but without parameters
  start.routerToNotebooksPage()
})
