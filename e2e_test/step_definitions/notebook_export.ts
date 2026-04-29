/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />

import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@cucumber/cucumber'
import start from '../start'

// First step already exists in user.ts:
// Given('I am logged in as an existing user', () => {
//   start.loginAs('old_learner')
// })

Given('I have a notebook titled {string}', (notebookTitle: string) => {
  // Following the pattern from note.ts where notes are injected
  cy.get<string>('@currentLoginUser').then((username) => {
    start
      .testability()
      .injectNotes([{ Title: notebookTitle }], username, notebookTitle)
  })
})

Given('the notebook contains the following notes', (notesTable: DataTable) => {
  const hashes = notesTable.hashes()
  cy.get<string>('@currentLoginUser').then((username) => {
    const notebookName = (hashes[0]?.['Parent Title'] ?? '').trim()
    if (!notebookName) {
      throw new Error(
        'the notebook contains: first row must have Parent Title (head note title)'
      )
    }
    start.testability().injectNotes(hashes, username, notebookName)
  })
})

// Additional steps needed for the feature
When('I select the {string} notebook', (notebookTitle: string) => {
  start.jumpToNotePage(notebookTitle)
})

When(
  'I export notebook {string} to Obsidian markdown zip file',
  (notebook: string) => {
    start.navigateToNotebookPage(notebook).exportForObsidian()
  }
)

Then('I should receive a zip file containing', (table: DataTable) => {
  const expectedFiles = table.hashes().map((file) => ({
    Filename: file.Filename || '',
    Format: file.Format || '',
    Content: file.Content || '',
    validateMetadata: true,
  }))
  start.checkDownloadFiles().hasZipFileWith(expectedFiles)
})
