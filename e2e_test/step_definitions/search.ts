/// <reference types="cypress" />
import { Given } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import notebookSettingsPopup from '../start/pageObjects/notebookSettingsPopup'

Given('I reindex the notebook {string}', (notebookTitle: string) => {
  start
    .routerToNotebooksPage()
    .notebookCard(notebookTitle)
    .editNotebookSettings()
    .reindexNotebook()
})
