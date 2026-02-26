/// <reference types="cypress" />
import { Given } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Given('I reindex the notebook {string}', (notebookTitle: string) => {
  start
    .navigateToNotebooksPage()
    .notebookCard(notebookTitle)
    .editNotebookSettings()
    .reindexNotebook()
})
