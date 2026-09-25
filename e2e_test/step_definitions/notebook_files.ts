/// <reference types="cypress" />
// @ts-check

import {
  type DataTable,
  Given,
  Then,
  When,
} from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import { attachmentPage } from '../start/pageObjects/attachmentPage'

Given(
  'the notebook {string} has files:',
  (notebookName: string, data: DataTable) => {
    const files = data.hashes() as { Path: string; Content: string }[]
    for (const { Path, Content } of files) {
      start
        .testability()
        .putNotebookFileForTestability(notebookName, Path, Content)
    }
  }
)

Given(
  'the notebook {string} has the picture {string} from fixture {string}',
  (notebookName: string, path: string, fixture: string) => {
    cy.fixture(fixture, 'base64').then((contentBase64: string) =>
      start
        .testability()
        .putNotebookFileBytesForTestability(notebookName, path, contentBase64)
    )
  }
)

Given(
  'the note {string} in the notebook {string} has a legacy uploaded picture from fixture {string}',
  (noteTitle: string, notebookName: string, fixture: string) => {
    cy.fixture(fixture, 'base64').then((contentBase64: string) =>
      start
        .testability()
        .seedLegacyNotePictureForTestability(
          notebookName,
          noteTitle,
          fixture.split('/').pop()!,
          contentBase64
        )
    )
  }
)

When(
  'the legacy uploaded pictures of the notebook {string} are moved into the notebook',
  (notebookName: string) => {
    start.testability().moveLegacyNotePicturesForTestability(notebookName)
  }
)

When(
  'I open the file {string} in sidebar folder path {string}',
  (filename: string, folderPath: string) => {
    start.noteSidebar().openFile(folderPath.split('/'), filename)
  }
)

Then(
  'I should see the file page for {string} of {int} bytes',
  (filename: string, size: number) => {
    attachmentPage().expectFileAndSize(filename, size)
  }
)

Then(
  'after reloading, the sidebar is open at folder {string} showing {string}',
  (folderLabel: string, filename: string) => {
    attachmentPage()
      .reload()
      .expectSidebarOpenAtFolderShowing(folderLabel, filename)
  }
)

Then(
  'downloading the file gives {string} named {string}',
  (content: string, filename: string) => {
    attachmentPage().expectDownload(filename, content)
  }
)
