/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />

import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@cucumber/cucumber'
import start from '../start'
import { notebookCard } from '../start/pageObjects/notebookCard'

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

When('I click on the download for Obsidian option on notebook {string}', (notebookTitle: string) => {
  notebookCard(notebookTitle).downloadForObsidian()
})

Given('I have an empty notebook titled {string}', (notebookTitle: string) => {
  start.testability().injectNotes([{ Title: notebookTitle }])
})

When('I go to Notebook page', () => {
  // Using the same navigation function but without parameters
  start.routerToNotebooksPage()
}) 