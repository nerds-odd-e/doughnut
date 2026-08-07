/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import {
  type DataTable,
  Then,
  When,
} from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Then('I am on a window {int} * {int}', (width: number, height: number) => {
  cy.viewport(width, height)
})

function openNotebookSidebar() {
  start.noteSidebar()
}

When('I open the notebook sidebar', openNotebookSidebar)

When('I open the note {string} from the sidebar', (noteTopology: string) => {
  start.noteSidebar().navigateToNote(noteTopology)
})

When('I activate folder {string} in the sidebar', (folderLabel: string) => {
  start.noteSidebar().activateFolderByLabel(folderLabel)
})

When(
  'I activate folder {string} under the open folder {string} in the sidebar',
  (childLabel: string, parentLabel: string) => {
    start.noteSidebar().activateFolderUnderOpenParent(parentLabel, childLabel)
  }
)

When('I view note {string}', (noteTitle: string) => {
  start.jumpToNotePage(noteTitle)
})

Then('I should see the note tree in the sidebar', (data: DataTable) => {
  start.noteSidebar().expectOrderedNotes(data.hashes())
})
