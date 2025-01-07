/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />

import { Given } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@cucumber/cucumber'
import start from '../start'

// First step already exists in user.ts:
// Given('I am logged in as an existing user', () => {
//   start.loginAs('old_learner')
// })

Given('I have a notebook titled {string}', (notebookTitle: string) => {
  start.testability().createNotebook(notebookTitle)
})

Given('the notebook contains the following notes', (notesTable: DataTable) => {
  const notes = notesTable.hashes()
  notes.forEach(note => {
    start.testability().createNote({
      title: note.Title,
      content: note.Content,
      notebookId: Cypress.env('currentNotebookId')
    })
  })
}) 