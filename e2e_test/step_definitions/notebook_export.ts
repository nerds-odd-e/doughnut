/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import { downloadedNotebookZip } from '../start/pageObjects/notebookExportZip'

When('I export notebook {string} from the catalog', (notebookName: string) => {
  start.navigateToNotebooksPage().notebookCard(notebookName).exportNotebook()
})

Then(
  'a zip file for notebook {string} should be downloaded',
  (notebookName: string) => {
    downloadedNotebookZip(notebookName).expectDownloaded()
  }
)

Then(
  'the downloaded zip for notebook {string} contains {string}',
  (notebookName: string, entryPath: string) => {
    downloadedNotebookZip(notebookName).expectContains(entryPath)
  }
)

Then(
  'the downloaded zip for notebook {string} does not contain {string}',
  (notebookName: string, entryPath: string) => {
    downloadedNotebookZip(notebookName).expectDoesNotContain(entryPath)
  }
)

Then(
  'the downloaded zip entry {string} of notebook {string} includes {string}',
  (entryPath: string, notebookName: string, fragment: string) => {
    downloadedNotebookZip(notebookName).expectEntryIncludes(entryPath, fragment)
  }
)

Then(
  'the downloaded zip entry {string} of notebook {string} does not include {string}',
  (entryPath: string, notebookName: string, fragment: string) => {
    downloadedNotebookZip(notebookName).expectEntryDoesNotInclude(
      entryPath,
      fragment
    )
  }
)

Then(
  'the collision zip entry for title {string} of notebook {string} includes {string}',
  (title: string, notebookName: string, fragment: string) => {
    downloadedNotebookZip(notebookName).expectCollisionEntryIncludes(
      title,
      fragment
    )
  }
)
