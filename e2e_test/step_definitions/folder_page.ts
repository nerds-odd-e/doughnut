/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import folderPage from '../start/pageObjects/folderPage'
import noteCreationForm from '../start/pageObjects/forms/noteCreationForm'
import workspaceSurfaceLandmarks from '../start/pageObjects/workspaceSurfaceLandmarks'
import { waitUntilAppIsNotBusy } from '../start/pageBase'

When(
  'I open the folder page for {string} from the sidebar',
  (folderLabel: string) => {
    start.noteSidebar().openFolderPageByLabel(folderLabel)
  }
)

When(
  'I open the folder page for {string} in notebook {string}',
  (folderLabel: string, notebookName: string) => {
    start
      .testability()
      .getNotebookIdByName(notebookName)
      .then((notebookId) =>
        start
          .testability()
          .getFolderIdInNotebook(notebookId, folderLabel)
          .then((folderId) => {
            start.routerPush(
              `/notebooks/${notebookId}/folders/${folderId}`,
              'folderPage',
              { notebookId, folderId }
            )
            start.waitUntilAppIsNotBusy()
          })
      )
  }
)

When('I type and save the folder readme with text {string}', (text: string) => {
  folderPage().typeFolderReadmeDraftAndSave(text)
})

When(
  'I create a new note from the sidebar submitting the default title',
  () => {
    const expectedTitle = new Date().toISOString().slice(0, 10)
    start.noteSidebar().addingNewNoteFromToolbar()
    noteCreationForm.submit()
    waitUntilAppIsNotBusy()
    start.assumeNotePage().expectNoteTitleDisplayed(expectedTitle)
  }
)

Then('the folder readme should contain {string}', (fragment: string) => {
  folderPage().expectFolderReadmeBodyContains(fragment)
})

Then('the folder admin controls are present', () => {
  workspaceSurfaceLandmarks().expectFolderAdminControlsPresent()
})

Then('the folder admin controls are not present', () => {
  workspaceSurfaceLandmarks().expectFolderAdminControlsAbsent()
})

Then('folder Readme and Settings tabs are present', () => {
  workspaceSurfaceLandmarks().expectFolderWorkspaceTabsPresent()
})

Then('folder Readme and Settings tabs are not present', () => {
  workspaceSurfaceLandmarks().expectFolderWorkspaceTabsAbsent()
})

When('I reload the folder page', () => {
  cy.reload()
  waitUntilAppIsNotBusy()
})
