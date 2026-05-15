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
import { assumeSidebarFolderOrganizeForm } from '../start/pageObjects/sidebarFolderOrganizeForm'

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
  'I activate folder {string} under the open folder {string} in the sidebar',
  (childLabel: string, parentLabel: string) => {
    start.noteSidebar().activateFolderUnderOpenParent(parentLabel, childLabel)
  }
)

When('I view note {string}', (noteTitle: string) => {
  start.jumpToNotePage(noteTitle)
})

When(
  'I move folder {string} to notebook root using the folder page',
  (folderLabel: string) => {
    start
      .noteSidebar()
      .openFolderPageForOrganize(folderLabel)
      .selectNotebookRootAsDestination()
      .confirmMove()
  }
)

When(
  'I attempt to move folder {string} under {string} to notebook root using the folder page',
  (childLabel: string, parentLabel: string) => {
    start
      .noteSidebar()
      .openFolderPageForOrganizeUnderParent(parentLabel, childLabel)
      .selectNotebookRootAsDestination()
      .tryConfirmMove()
  }
)

When(
  'I move folder {string} under {string} to folder {string} using folder search on the folder page',
  (childLabel: string, parentLabel: string, destFolder: string) => {
    start
      .noteSidebar()
      .openFolderPageForOrganizeUnderParent(parentLabel, childLabel)
      .openFolderSearch()
      .searchFolderDestination(destFolder)
      .selectFolderSearchResultByName(destFolder)
      .confirmMove()
  }
)

When(
  'I dissolve folder {string} under {string} using the folder page',
  (childLabel: string, parentLabel: string) => {
    start
      .noteSidebar()
      .openFolderPageForOrganizeUnderParent(parentLabel, childLabel)
      .dissolveFolder()
  }
)

Then('the folder page shows error {string}', (text: string) => {
  assumeSidebarFolderOrganizeForm().expectErrorText(text)
})

Then('I should see the note tree in the sidebar', (data: DataTable) => {
  start.noteSidebar().expectOrderedNotes(data.hashes())
})
