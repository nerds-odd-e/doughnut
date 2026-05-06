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
import { assumeSidebarFolderOrganizeDialog } from '../start/pageObjects/sidebarFolderOrganizeDialog'

Then('I am on a window {int} * {int}', (width: number, height: number) => {
  cy.viewport(width, height)
})

function expandSideBar() {
  start.noteSidebar()
}

When('I expand the side bar', expandSideBar)

When('I open the note {string} from the sidebar', (noteTopology: string) => {
  start.noteSidebar().navigateToNote(noteTopology)
})

When('I activate folder {string} in the sidebar', (folderLabel: string) => {
  start.noteSidebar().activateFolderByLabel(folderLabel)
})

When(
  'I activate folder {string} under folder {string} in the sidebar',
  (childLabel: string, parentLabel: string) => {
    start.noteSidebar().activateFolderUnderParent(parentLabel, childLabel)
  }
)

When('I view note {string}', (noteTitle: string) => {
  start.jumpToNotePage(noteTitle)
})

When(
  'I move the active folder to notebook root using the sidebar folder dialog',
  () => {
    start
      .noteSidebar()
      .openFolderOrganizeDialog()
      .selectNotebookRootAsDestination()
      .confirmMove()
  }
)

When(
  'I attempt to move the active folder to notebook root using the sidebar folder dialog',
  () => {
    start
      .noteSidebar()
      .openFolderOrganizeDialog()
      .selectNotebookRootAsDestination()
      .tryConfirmMove()
  }
)

When('I dissolve the active folder using the sidebar folder dialog', () => {
  start.noteSidebar().openFolderOrganizeDialog().dissolveFolder()
})

Then('the sidebar folder dialog shows error {string}', (text: string) => {
  assumeSidebarFolderOrganizeDialog().expectErrorText(text)
})

Then('I should see the note tree in the sidebar', (data: DataTable) => {
  start.noteSidebar().expectOrderedNotes(data.hashes())
})
