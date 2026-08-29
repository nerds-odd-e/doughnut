/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import folderPage from '../start/pageObjects/folderPage'
import { assumeFolderOrganizeForm } from '../start/pageObjects/folderOrganizeForm'
import noteCreationForm from '../start/pageObjects/forms/noteCreationForm'
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
    start.jumpToFolderPage(folderLabel, notebookName)
  }
)

When('I type and save the folder readme with text {string}', (text: string) => {
  folderPage().typeFolderReadmeDraftAndSave(text)
})

When('I create a new note from the sidebar with the default title', () => {
  start.noteSidebar().addingNewNoteFromToolbar()
  noteCreationForm.submit()
  waitUntilAppIsNotBusy()
})

Then("I should see a note titled with today's date", () => {
  const expectedTitle = new Date().toISOString().slice(0, 10)
  start.assumeNotePage().expectNoteTitleDisplayed(expectedTitle)
})

Then('the folder readme should contain {string}', (fragment: string) => {
  folderPage().expectFolderReadmeBodyContains(fragment)
})

When('I reload the folder page', () => {
  folderPage().reload()
})

When('I rename the folder heading to {string}', (name: string) => {
  folderPage().renameHeading(name)
})

Then('the folder page heading should be {string}', (name: string) => {
  folderPage().expectHeading(name)
})

When('I move folder {string} to notebook root', (folderLabel: string) => {
  start
    .noteSidebar()
    .openFolderPageForOrganize(folderLabel)
    .selectNotebookRootAsDestination()
    .confirmMove()
})

When(
  'I move folder {string} to notebook {string} root',
  (folderLabel: string, notebookName: string) => {
    start
      .noteSidebar()
      .openFolderPageForOrganize(folderLabel)
      .selectDestinationNotebook(notebookName)
      .confirmMove()
  }
)

When(
  'I move folder {string} under {string} to notebook {string} root',
  (childLabel: string, parentLabel: string, notebookName: string) => {
    start
      .noteSidebar()
      .openFolderPageForOrganizeUnderParent(parentLabel, childLabel)
      .selectDestinationNotebook(notebookName)
      .confirmMove()
  }
)

When(
  'I move folder {string} under {string} to notebook {string} folder {string}',
  (
    childLabel: string,
    parentLabel: string,
    notebookName: string,
    destFolder: string
  ) => {
    start
      .noteSidebar()
      .openFolderPageForOrganizeUnderParent(parentLabel, childLabel)
      .selectDestinationNotebook(notebookName)
      .openFolderSearch()
      .searchFolderDestination(destFolder)
      .selectFolderSearchResultByName(destFolder)
      .confirmMove()
  }
)

When(
  'I move the current folder to notebook {string} folder {string} and confirm merge',
  (notebookName: string, destFolder: string) => {
    folderPage()
      .openOrganizeForm()
      .selectDestinationNotebook(notebookName)
      .openFolderSearch()
      .searchFolderDestination(destFolder)
      .selectFolderSearchResultByName(destFolder)
      .confirmMerge()
  }
)

When(
  'I attempt to move folder {string} under {string} to notebook root',
  (childLabel: string, parentLabel: string) => {
    start
      .noteSidebar()
      .openFolderPageForOrganizeUnderParent(parentLabel, childLabel)
      .selectNotebookRootAsDestination()
      .tryConfirmMove()
  }
)

When(
  'I move folder {string} under {string} to notebook root and confirm merge',
  (childLabel: string, parentLabel: string) => {
    start
      .noteSidebar()
      .openFolderPageForOrganizeUnderParent(parentLabel, childLabel)
      .selectNotebookRootAsDestination()
      .confirmMerge()
  }
)

When(
  'I move folder {string} under {string} to folder {string} using folder search',
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
  'I dissolve folder {string} under {string}',
  (childLabel: string, parentLabel: string) => {
    start
      .noteSidebar()
      .openFolderPageForOrganizeUnderParent(parentLabel, childLabel)
      .dissolveFolder()
  }
)

When(
  'I dissolve folder {string} under {string} and confirm merge',
  (childLabel: string, parentLabel: string) => {
    start
      .noteSidebar()
      .openFolderPageForOrganizeUnderParent(parentLabel, childLabel)
      .dissolveFolderWithMerge()
  }
)

Then('the folder page shows error {string}', (text: string) => {
  assumeFolderOrganizeForm().expectErrorText(text)
})
