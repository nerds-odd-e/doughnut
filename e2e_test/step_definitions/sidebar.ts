/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import {
  type DataTable,
  Then,
  When,
} from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Then('I am on a window {int} * {int}', (width: number, height: number) => {
  cy.viewport(width, height)
})

function expandSideBar() {
  start.noteSidebar()
}

When('I expand the side bar', expandSideBar)

When('I open the note {string} from the sidebar', (noteTopology: string) => {
  start.noteSidebar().navigateToNote(noteTopology)
})

When('I activate folder {string} in the sidebar', (folderLabel: string) => {
  start.noteSidebar().activateFolderByLabel(folderLabel)
})

When(
  'I move the active folder to notebook root using the sidebar folder dialog',
  () => {
    start.pageIsNotLoading()
    cy.get('aside').findByRole('button', { name: 'Folder…' }).click()
    cy.get('[data-testid="folder-move-parent-select"]').select('__root__')
    cy.get('[data-testid="folder-move-submit"]', { timeout: 20000 })
      .should('not.be.disabled')
      .click()
    start.pageIsNotLoading()
  }
)

Then('the folder organize control should not be visible in the sidebar', () => {
  cy.get('aside').findByRole('button', { name: 'Folder…' }).should('not.exist')
})

Then('I should see the note tree in the sidebar', (data: DataTable) => {
  start.noteSidebar().expectOrderedNotes(data.hashes())
})
