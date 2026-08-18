/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import {
  type DataTable,
  Then,
  When,
} from '@badeball/cypress-cucumber-preprocessor'
import type NotePath from '../support/NotePath'
import start from '../start'
import folderPage from '../start/pageObjects/folderPage'

Then(
  'I should see folder {notepath} containing these notes:',
  (notePath: NotePath, data: DataTable) => {
    start
      .waitUntilAppIsNotBusy()
      .navigateToNotebooksPage()
      .expandFolderInSidebar(notePath)
      .expectChildrenUnderSidebarFolder(data.hashes())
  }
)

Then(
  'I should see sidebar folder {string} containing these notes:',
  (folderLabel: string, data: DataTable) => {
    start
      .noteSidebar()
      .expand(folderLabel)
      .expectChildrenUnderFolder(folderLabel, data.hashes())
  }
)

When(
  'I create a folder named {string} while viewing note {string}',
  (folderName: string, noteTitle: string) => {
    start.jumpToNotePage(noteTitle)
    start
      .noteSidebar()
      .addingNewFolderFromToolbar()
      .createFolderWithName(folderName)
  }
)

Then('I should see sidebar folder {string}', (folderLabel: string) => {
  start.noteSidebar().expectSidebarFolderVisible(folderLabel)
})

Then(
  'I should not see a warning that the portable tree may be OKF-incompatible',
  () => {
    folderPage().expectNoOkfIncompatibleTitleWarning()
  }
)

Then('I should not see sidebar folder {string}', (folderLabel: string) => {
  start.noteSidebar().expectSidebarFolderAbsent(folderLabel)
})

Then(
  'I should see sidebar folder {string} under open folder {string}',
  (childFolderLabel: string, parentFolderLabel: string) => {
    start
      .noteSidebar()
      .expectSidebarFolderUnderOpenParent(parentFolderLabel, childFolderLabel)
  }
)

Then(
  'I should see sidebar folder {string} under collapsed folder {string}',
  (childFolderLabel: string, parentFolderLabel: string) => {
    start
      .noteSidebar()
      .expand(parentFolderLabel)
      .expectSidebarFolderUnderOpenParent(parentFolderLabel, childFolderLabel)
  }
)

Then(
  'I should see note {string} under open folder {string}',
  (noteTitle: string, folderLabel: string) => {
    start.noteSidebar().expectSidebarNoteUnderOpenFolder(folderLabel, noteTitle)
  }
)
