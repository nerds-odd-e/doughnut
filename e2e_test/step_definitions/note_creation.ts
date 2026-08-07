/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import mock_services from '../start/mock_services'

When(
  'I create a note titled {string} from note {string} with relationship {string}',
  (title: string, fromNote: string, relationship: string) => {
    start
      .jumpToNotePage(fromNote)
      .addingNewNoteFromToolbar()
      .createNoteWithTitleAndParentRelationship(title, relationship)
    start.assumeNotePage(title)
    start.testability().rememberUiCreatedNote(title)
  }
)

When(
  'I create a note with title {string} under the folder {string} in the notebook {string}',
  (title: string, folder: string, notebook: string) => {
    start.navigateToNotebooksPage().navigateToNotebook(notebook)
    start
      .noteSidebar()
      .activateFolderByLabel(folder)
      .addingNewNoteFromToolbar()
      .createNoteWithTitle(title)
    start.assumeNotePage(title)
    if (title !== '') {
      start.testability().rememberUiCreatedNote(title)
    }
  }
)

When(
  'I create a note with title {string} and wikidata id {string} in the notebook {string}',
  (title: string, wikidataId: string, notebook: string) => {
    mock_services.wikidata().stubWikidataSearchResult(title, wikidataId)
    start
      .jumpToNotebookPage(notebook)
      .addingNewNoteFromToolbar()
      .createNoteWithTitleAndWikidataId(title, wikidataId)
    start.assumeNotePage(title, { timeout: 30000 })
    start.testability().rememberUiCreatedNote(title)
  }
)

When(
  'I attempt to create a note with title {string} and wikidata id {string} in the notebook {string}',
  (title: string, wikidataId: string, notebook: string) => {
    mock_services.wikidata().stubWikidataSearchResult(title, wikidataId)
    start
      .jumpToNotebookPage(notebook)
      .addingNewNoteFromToolbar()
      .createNoteWithTitleAndWikidataId(title, wikidataId)
  }
)

When('I am creating a note in the notebook {string}', (notebook: string) => {
  start.jumpToNotebookPage(notebook).addingNewNoteFromToolbar()
})

When('I should see that the note creation is not successful', () => {
  start.form.getField('Title').expectError('must not be blank')
  cy.get('body').then(($body) => {
    const close = $body.find('.Vue-Toastification__close-button').get(0)
    if (close) {
      close.click()
    }
  })
})

When(
  'I activate folder {string} in the sidebar and create a new note with title {string}',
  (folderLabel: string, title: string) => {
    start.jumpToNotePage('team')
    start
      .noteSidebar()
      .activateFolderByLabel(folderLabel)
      .addingNewNoteFromToolbar()
      .createNoteWithTitle(title)
    start.assumeNotePage(title)
    start.testability().rememberUiCreatedNote(title)
  }
)
