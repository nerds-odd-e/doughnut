/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import folderPage from '../start/pageObjects/folderPage'
import { pageIsNotLoading } from '../start/pageBase'

When(
  'I open the folder page for {string} from the sidebar',
  (folderLabel: string) => {
    start.noteSidebar().openFolderPageByLabel(folderLabel)
  }
)

When('I type and save the folder index with text {string}', (text: string) => {
  folderPage().typeFolderIndexDraftAndSave(text)
})

Then('the folder index should contain {string}', (fragment: string) => {
  folderPage().expectFolderIndexBodyContains(fragment)
})

When('I reload the folder page', () => {
  cy.reload()
  pageIsNotLoading()
})
