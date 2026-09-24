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

Then('I should see these rows at the sidebar root:', (data: DataTable) => {
  start.noteSidebar().expectRootRows(data.raw().flat())
})

Then(
  'I should see these rows in sidebar folder {string}:',
  (folderLabel: string, data: DataTable) => {
    start.noteSidebar().expectRowsUnderFolder(folderLabel, data.raw().flat())
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
