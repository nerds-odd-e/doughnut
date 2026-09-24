/// <reference types="cypress" />
// @ts-check

import {
  type DataTable,
  Given,
  Then,
} from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

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
